import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { problem, server } from '../../test/server.ts'
import { ApiError } from './ApiError.ts'
import { parseEventStream, postEventStream } from './eventStream.ts'
import type { ServerSentEvent } from './eventStream.ts'

function streamOf(...chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder()
  return new ReadableStream({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk))
      }
      controller.close()
    },
  })
}

async function collect(source: AsyncGenerator<ServerSentEvent>): Promise<ServerSentEvent[]> {
  const events: ServerSentEvent[] = []
  for await (const event of source) {
    events.push(event)
  }
  return events
}

describe('parseEventStream', () => {
  it('givenEventSplitAcrossChunks_whenParsed_thenEmitsSingleEvent', async () => {
    const events = await collect(parseEventStream(streamOf('event: tok', 'en\ndata: {"text":"a"}\n\n')))

    expect(events).toEqual([{ event: 'token', data: '{"text":"a"}' }])
  })

  it('givenTwoEventsInOneChunkWithCrlf_whenParsed_thenEmitsBothInOrder', async () => {
    const events = await collect(
      parseEventStream(
        streamOf(': keep-alive\r\nevent: token\r\ndata: {"text":"a"}\r\n\r\nevent: done\r\ndata: {"turnId":1}\r\n\r\n'),
      ),
    )

    expect(events).toEqual([
      { event: 'token', data: '{"text":"a"}' },
      { event: 'done', data: '{"turnId":1}' },
    ])
  })

  it('givenSpringStyleLinesWithoutSpace_whenParsed_thenSameAsWithSpace', async () => {
    const events = await collect(parseEventStream(streamOf('event:token\ndata:{"text":"Kartını "}\n\n')))

    expect(events).toEqual([{ event: 'token', data: '{"text":"Kartını "}' }])
  })

  it('givenMultibyteCharacterSplitAcrossChunks_whenParsed_thenDecodedWhole', async () => {
    const bytes = new TextEncoder().encode('event: token\ndata: {"text":"ş"}\n\n')
    const splitAt = bytes.indexOf(0xc5) + 1
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(bytes.slice(0, splitAt))
        controller.enqueue(bytes.slice(splitAt))
        controller.close()
      },
    })

    const events = await collect(parseEventStream(stream))

    expect(events).toEqual([{ event: 'token', data: '{"text":"ş"}' }])
  })

  it('givenMultipleDataLinesAndNoEventName_whenParsed_thenJoinedWithNewlineAndMessageEvent', async () => {
    const events = await collect(parseEventStream(streamOf('data: a\ndata: b\n\n')))

    expect(events).toEqual([{ event: 'message', data: 'a\nb' }])
  })

  it('givenStreamEndingWithoutBlankLine_whenParsed_thenIncompleteEventDropped', async () => {
    const events = await collect(parseEventStream(streamOf('event: token\ndata: {"text":"a"}\n\nevent: done\ndata: {')))

    expect(events).toEqual([{ event: 'token', data: '{"text":"a"}' }])
  })

  it('givenConnectionDropsMidStream_whenParsed_thenThrowsStreamInterrupted', async () => {
    const encoder = new TextEncoder()
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(encoder.encode('event: token\ndata: {"text":"a"}\n\n'))
      },
      pull(controller) {
        controller.error(new TypeError('terminated'))
      },
    })
    const received: ServerSentEvent[] = []

    const failure = (async () => {
      for await (const event of parseEventStream(stream)) {
        received.push(event)
      }
    })()

    await expect(failure).rejects.toMatchObject({ errorCode: 'STREAM_INTERRUPTED' })
    expect(received).toHaveLength(1)
  })
})

describe('postEventStream', () => {
  it('givenSseResponse_whenPostEventStream_thenSendsJsonBodyAndAcceptHeadersAndYieldsEvents', async () => {
    let seenAccept: string | null = null
    let seenContentType: string | null = null
    let seenBody: unknown = null
    server.use(
      http.post('/api/assistant/conversations/1/messages', async ({ request }) => {
        seenAccept = request.headers.get('Accept')
        seenContentType = request.headers.get('Content-Type')
        seenBody = await request.json()
        return new HttpResponse('event:token\ndata:{"text":"a"}\n\n', { headers: { 'Content-Type': 'text/event-stream' } })
      }),
    )

    const events = await collect(
      postEventStream('/api/assistant/conversations/1/messages', { text: 'Merhaba' }, new AbortController().signal),
    )

    expect(seenAccept).toBe('text/event-stream, application/problem+json')
    expect(seenContentType).toBe('application/json')
    expect(seenBody).toEqual({ text: 'Merhaba' })
    expect(events).toEqual([{ event: 'token', data: '{"text":"a"}' }])
  })

  it('givenProblem503_whenPostEventStream_thenThrowsApiErrorWithCode', async () => {
    server.use(http.post('/x', () => problem(503, 'RETRIEVAL_UNAVAILABLE')))

    const failure = collect(postEventStream('/x', {}, new AbortController().signal))

    await expect(failure).rejects.toBeInstanceOf(ApiError)
    await expect(failure).rejects.toMatchObject({ status: 503, errorCode: 'RETRIEVAL_UNAVAILABLE' })
  })

  it('givenFetchRejects_whenPostEventStream_thenNetworkError', async () => {
    server.use(http.post('/x', () => HttpResponse.error()))

    const failure = collect(postEventStream('/x', {}, new AbortController().signal))

    await expect(failure).rejects.toMatchObject({ status: 0, errorCode: 'NETWORK_ERROR' })
  })

  it('givenAbortedSignal_whenPostEventStream_thenAbortErrorNotNetworkError', async () => {
    server.use(http.post('/x', () => new HttpResponse('', { headers: { 'Content-Type': 'text/event-stream' } })))
    const controller = new AbortController()
    controller.abort()

    const failure = collect(postEventStream('/x', {}, controller.signal))

    await expect(failure).rejects.toMatchObject({ name: 'AbortError' })
  })
})
