import { HttpResponse } from 'msw'

export type SseEvent = { event: string; data: unknown }

const SSE_HEADERS = { 'Content-Type': 'text/event-stream' }

export function encodeSse(events: SseEvent[]): string {
  return events.map(({ event, data }) => `event:${event}\ndata:${JSON.stringify(data)}\n\n`).join('')
}

export function sseResponse(events: SseEvent[]) {
  return new HttpResponse(encodeSse(events), { status: 200, headers: SSE_HEADERS })
}

export function controllableSseStream() {
  const encoder = new TextEncoder()
  let streamController: ReadableStreamDefaultController<Uint8Array> | null = null
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      streamController = controller
    },
  })
  return {
    response: () => new HttpResponse(body, { status: 200, headers: SSE_HEADERS }),
    push(...events: SseEvent[]) {
      streamController?.enqueue(encoder.encode(encodeSse(events)))
    },
    close() {
      streamController?.close()
    },
  }
}
