import { describe, expect, it } from 'vitest'
import { contextChunk, doneEvent, sourcesEvent } from '../../../test/previewFixtures.ts'
import { toAnswerStreamEvent } from './answerStreamEvents.ts'

const sources = (data: unknown) => toAnswerStreamEvent({ event: 'sources', data: JSON.stringify(data) })
const done = (data: unknown) => toAnswerStreamEvent({ event: 'done', data: JSON.stringify(data) })

describe('toAnswerStreamEvent payload rules', () => {
  it.each(['null', '"text"', '5', 'true'])('givenNonObjectJson_whenConverted_thenNull(%s)', (data) => {
    expect(toAnswerStreamEvent({ event: 'token', data })).toBeNull()
  })

  it.each([
    ['chunkId as string', { chunkId: '151' }],
    ['chunkNo fractional', { chunkNo: 1.5 }],
    ['pageNumber missing', { pageNumber: undefined }],
    ['heading as number', { heading: 5 }],
    ['sourceId missing', { sourceId: undefined }],
    ['sourceName as number', { sourceName: 5 }],
    ['version as number', { version: 2026 }],
    ['vectorRank fractional', { vectorRank: 0.5 }],
    ['distance as string', { distance: '0.3' }],
    ['distance infinite-like null', { distance: null }],
    ['rerankScore as string', { rerankScore: '5.3' }],
    ['n as string', { n: '1' }],
  ])('givenChunkWith_%s_whenConverted_thenSourcesRejected', (_name, patch) => {
    const chunk = { ...contextChunk(1), ...patch }

    expect(sources(sourcesEvent({ contextChunks: [chunk as never] }))).toBeNull()
  })

  it('givenChunkWithNullRerankScore_whenConverted_thenAccepted', () => {
    expect(sources(sourcesEvent({ contextChunks: [contextChunk(1, { rerankScore: null })] }))?.type).toBe('sources')
  })

  it('givenChunkThatIsNotAnObject_whenConverted_thenSourcesRejected', () => {
    expect(sources({ ...sourcesEvent(), contextChunks: [null] })).toBeNull()
    expect(sources({ ...sourcesEvent(), contextChunks: [[1, 2]] })).toBeNull()
  })

  it.each([
    ['citations not array', { citations: 'x' }],
    ['citation fractional', { citations: [1.5] }],
    ['searchQuery as number', { searchQuery: 5 }],
    ['queryRewriteStatus unknown', { queryRewriteStatus: 'MAYBE' }],
    ['queryRewriteStatus missing', { queryRewriteStatus: undefined }],
    ['candidates fractional', { candidates: 2.5 }],
  ])('givenSourcesWith_%s_whenConverted_thenRejected', (_name, patch) => {
    expect(sources({ ...sourcesEvent(), ...patch })).toBeNull()
  })

  it('givenSourcesWithNullableFieldsNull_whenConverted_thenAccepted', () => {
    const event = sources(sourcesEvent({ searchQuery: null, candidates: null, topK: null }))

    expect(event?.type).toBe('sources')
  })

  it.each([
    ['turnId as string', { turnId: '5' }],
    ['turnId fractional', { turnId: 5.5 }],
    ['notFoundAnswer missing', { notFoundAnswer: undefined }],
    ['notFoundAnswer as string', { notFoundAnswer: 'false' }],
    ['chatModel missing', { chatModel: undefined }],
    ['chatModel as number', { chatModel: 9 }],
    ['promptTokens fractional', { promptTokens: 1.5 }],
    ['completionTokens as string', { completionTokens: '5' }],
    ['queryRewriteLatencyMs as string', { queryRewriteLatencyMs: '5' }],
    ['rerankLatencyMs fractional', { rerankLatencyMs: 0.5 }],
    ['generationLatencyMs as string', { generationLatencyMs: '5' }],
    ['timeToFirstTokenMs as string', { timeToFirstTokenMs: '5' }],
    ['latencyMs null', { latencyMs: null }],
    ['latencyMs fractional', { latencyMs: 1.5 }],
    ['hostLabel as number', { hostLabel: 5 }],
  ])('givenDoneWith_%s_whenConverted_thenRejected', (_name, patch) => {
    expect(done({ ...doneEvent(), ...patch })).toBeNull()
  })

  it('givenDoneWithAllNullableFieldsNull_whenConverted_thenAccepted', () => {
    const event = done(
      doneEvent({
        promptTokens: null,
        completionTokens: null,
        queryRewriteLatencyMs: null,
        retrievalLatencyMs: null,
        rerankLatencyMs: null,
        generationLatencyMs: null,
        timeToFirstTokenMs: null,
        hostLabel: null,
      }),
    )

    expect(event?.type).toBe('done')
  })
})
