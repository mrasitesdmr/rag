import { describe, expect, it } from 'vitest'
import { doneEvent, sourcesEvent } from '../../../test/previewFixtures.ts'
import { toAnswerStreamEvent } from './answerStreamEvents.ts'

describe('toAnswerStreamEvent', () => {
  it.each([
    ['unknown event name', { event: 'ping', data: '{}' }],
    ['token with numeric text', { event: 'token', data: '{"text":5}' }],
    ['token without text', { event: 'token', data: '{}' }],
    ['malformed JSON', { event: 'token', data: '{"text":' }],
    ['JSON array', { event: 'token', data: '[]' }],
    ['error without errorCode', { event: 'error', data: '{}' }],
    ['error with empty errorCode', { event: 'error', data: '{"errorCode":""}' }],
    ['sources with unknown strategy', { event: 'sources', data: JSON.stringify({ ...sourcesEvent(), retrievalStrategy: 'MAGIC' }) }],
    ['sources with non-array chunks', { event: 'sources', data: JSON.stringify({ ...sourcesEvent(), contextChunks: 'x' }) }],
    [
      'sources chunk missing rerankScore',
      {
        event: 'sources',
        data: JSON.stringify({
          ...sourcesEvent(),
          contextChunks: [{ ...sourcesEvent().contextChunks[0], rerankScore: undefined }],
        }),
      },
    ],
    ['sources without searchQuery', { event: 'sources', data: JSON.stringify({ ...sourcesEvent(), searchQuery: undefined }) }],
    ['sources with numeric searchQuery', { event: 'sources', data: JSON.stringify({ ...sourcesEvent(), searchQuery: 5 }) }],
    ['sources without queryRewriteStatus', { event: 'sources', data: JSON.stringify({ ...sourcesEvent(), queryRewriteStatus: undefined }) }],
    ['sources with unknown queryRewriteStatus', { event: 'sources', data: JSON.stringify({ ...sourcesEvent(), queryRewriteStatus: 'FOO' }) }],
    ['done without queryRewriteLatencyMs', { event: 'done', data: JSON.stringify({ ...doneEvent(), queryRewriteLatencyMs: undefined }) }],
    ['done with string queryRewriteLatencyMs', { event: 'done', data: JSON.stringify({ ...doneEvent(), queryRewriteLatencyMs: '812' }) }],
    ['done without hostLabel', { event: 'done', data: JSON.stringify({ ...doneEvent(), hostLabel: undefined }) }],
    ['done with string latency', { event: 'done', data: JSON.stringify({ ...doneEvent(), latencyMs: '4730' }) }],
  ])('givenUnknownOrMalformedEvent_whenToAnswerStreamEvent_thenNull %s', (_label, event) => {
    expect(toAnswerStreamEvent(event)).toBeNull()
  })

  it('givenValidEvents_whenToAnswerStreamEvent_thenTypedEvents', () => {
    expect(toAnswerStreamEvent({ event: 'token', data: '{"text":"Kartını "}' })).toEqual({
      type: 'token',
      data: { text: 'Kartını ' },
    })
    expect(toAnswerStreamEvent({ event: 'sources', data: JSON.stringify(sourcesEvent()) })).toEqual({
      type: 'sources',
      data: sourcesEvent(),
    })
    expect(toAnswerStreamEvent({ event: 'done', data: JSON.stringify(doneEvent()) })).toEqual({
      type: 'done',
      data: doneEvent(),
    })
    expect(toAnswerStreamEvent({ event: 'error', data: '{"errorCode":"CHAT_MODEL_UNAVAILABLE"}' })).toEqual({
      type: 'error',
      data: { errorCode: 'CHAT_MODEL_UNAVAILABLE' },
    })
  })

  it('givenNullableFieldsNull_whenToAnswerStreamEvent_thenAccepted', () => {
    const nullable = doneEvent({
      promptTokens: null,
      completionTokens: null,
      rerankLatencyMs: null,
      generationLatencyMs: null,
      timeToFirstTokenMs: null,
      hostLabel: null,
    })

    expect(toAnswerStreamEvent({ event: 'done', data: JSON.stringify(nullable) })).toEqual({
      type: 'done',
      data: nullable,
    })
  })

  it('givenRewrittenSourcesAndDoneWithLatency_whenToAnswerStreamEvent_thenTypedWithNewFields', () => {
    const rewrittenSources = sourcesEvent({
      searchQuery: 'Kaybolan kartın yenisi kaç günde gelir?',
      queryRewriteStatus: 'REWRITTEN',
    })
    const rewrittenDone = doneEvent({ queryRewriteLatencyMs: 812 })

    expect(toAnswerStreamEvent({ event: 'sources', data: JSON.stringify(rewrittenSources) })).toEqual({
      type: 'sources',
      data: rewrittenSources,
    })
    expect(toAnswerStreamEvent({ event: 'done', data: JSON.stringify(rewrittenDone) })).toEqual({
      type: 'done',
      data: rewrittenDone,
    })
  })
})
