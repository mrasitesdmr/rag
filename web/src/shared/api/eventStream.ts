import { ApiError } from './ApiError.ts'
import { toApiError } from './httpClient.ts'

export type ServerSentEvent = { event: string; data: string }

const DEFAULT_EVENT = 'message'
const NETWORK_ERROR = 'NETWORK_ERROR'
const UNEXPECTED_ERROR = 'UNEXPECTED_ERROR'
export const STREAM_INTERRUPTED = 'STREAM_INTERRUPTED'

export async function* postEventStream(
  path: string,
  body: unknown,
  signal: AbortSignal,
): AsyncGenerator<ServerSentEvent> {
  let response: Response
  try {
    response = await fetch(path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream, application/problem+json',
      },
      body: JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (isAbortError(error)) {
      throw error
    }
    throw new ApiError(0, NETWORK_ERROR)
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.body === null) {
    throw new ApiError(response.status, UNEXPECTED_ERROR)
  }
  yield* parseEventStream(response.body)
}

export async function* parseEventStream(body: ReadableStream<Uint8Array>): AsyncGenerator<ServerSentEvent> {
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let eventName = ''
  let dataLines: string[] = []
  try {
    while (true) {
      const { done, value } = await readChunk(reader)
      buffer += done ? decoder.decode() : decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')
      let newlineIndex = buffer.indexOf('\n')
      while (newlineIndex !== -1) {
        const line = buffer.slice(0, newlineIndex).replace(/\r$/, '')
        buffer = buffer.slice(newlineIndex + 1)
        if (line === '') {
          if (dataLines.length > 0) {
            yield { event: eventName === '' ? DEFAULT_EVENT : eventName, data: dataLines.join('\n') }
          }
          eventName = ''
          dataLines = []
        } else if (!line.startsWith(':')) {
          const [field, fieldValue] = splitField(line)
          if (field === 'event') {
            eventName = fieldValue
          } else if (field === 'data') {
            dataLines.push(fieldValue)
          }
        }
        newlineIndex = buffer.indexOf('\n')
      }
      if (done) {
        return
      }
    }
  } finally {
    reader.cancel().catch(() => undefined)
  }
}

async function readChunk(reader: ReadableStreamDefaultReader<Uint8Array>): Promise<ReadableStreamReadResult<Uint8Array>> {
  try {
    return await reader.read()
  } catch (error) {
    if (isAbortError(error)) {
      throw error
    }
    throw new ApiError(0, STREAM_INTERRUPTED)
  }
}

function splitField(line: string): [string, string] {
  const colonIndex = line.indexOf(':')
  if (colonIndex === -1) {
    return [line, '']
  }
  const value = line.slice(colonIndex + 1)
  return [line.slice(0, colonIndex), value.startsWith(' ') ? value.slice(1) : value]
}

export function isAbortError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError'
}
