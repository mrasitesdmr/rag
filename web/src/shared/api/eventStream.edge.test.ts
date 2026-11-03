import { describe, expect, it } from 'vitest'
import { parseEventStream } from './eventStream.ts'
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

async function collect(chunks: string[]): Promise<ServerSentEvent[]> {
  const events: ServerSentEvent[] = []
  for await (const event of parseEventStream(streamOf(...chunks))) {
    events.push(event)
  }
  return events
}

describe('parseEventStream edge cases', () => {
  it('givenEventNameThenBlankLineThenNameless_whenParsed_thenNameDoesNotLeakIntoNextEvent', async () => {
    const events = await collect(['event: token\ndata: a\n\ndata: b\n\n'])

    expect(events).toEqual([
      { event: 'token', data: 'a' },
      { event: 'message', data: 'b' },
    ])
  })

  it('givenBlankLinesWithoutData_whenParsed_thenNoEvent', async () => {
    expect(await collect(['event: token\n\n\n\n'])).toEqual([])
  })

  it('givenCommentLine_whenParsed_thenIgnoredEvenWithDataLookingText', async () => {
    expect(await collect([': data: hidden\ndata: shown\n\n'])).toEqual([{ event: 'message', data: 'shown' }])
  })

  it('givenLineEndingWithColon_whenParsed_thenNotTreatedAsComment', async () => {
    expect(await collect(['data:\n\n'])).toEqual([{ event: 'message', data: '' }])
  })

  it('givenFieldWithoutColon_whenParsed_thenUnknownFieldIgnored', async () => {
    expect(await collect(['retry\ndata: x\n\n'])).toEqual([{ event: 'message', data: 'x' }])
  })

  it('givenDataFieldNameOnly_whenParsed_thenEmptyData', async () => {
    expect(await collect(['data\n\n'])).toEqual([{ event: 'message', data: '' }])
  })

  it('givenCarriageReturnLineEndings_whenParsed_thenCleanValues', async () => {
    expect(await collect(['event: done\r\ndata: {"a":1}\r\n\r\n'])).toEqual([{ event: 'done', data: '{"a":1}' }])
  })

  it('givenCarriageReturnSplitAcrossChunks_whenParsed_thenCleanValues', async () => {
    expect(await collect(['event: done\r', '\ndata: 1\r', '\n\r', '\n'])).toEqual([{ event: 'done', data: '1' }])
  })

  it('givenMultipleDataLines_whenParsed_thenJoinedWithNewline', async () => {
    expect(await collect(['data: a\ndata: b\n\n'])).toEqual([{ event: 'message', data: 'a\nb' }])
  })

  it('givenValueWithoutLeadingSpace_whenParsed_thenKeptAsIs', async () => {
    expect(await collect(['data:  two\n\n'])).toEqual([{ event: 'message', data: ' two' }])
  })
})
