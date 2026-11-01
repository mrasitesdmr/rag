import { describe, expect, it } from 'vitest'
import { GUIDE_NAME } from '../../../test/fixtures.ts'
import { contextChunk, sourcesEvent } from '../../../test/previewFixtures.ts'
import { citedChunks, sourceButtonLabel, sourceButtonTitle } from './citations.ts'

describe('citations', () => {
  it('givenCitationsOnTwoChunksOfSamePage_whenCitedChunks_thenTwoButtonsInCitationOrder', () => {
    const chunks = [
      contextChunk(1, { pageNumber: 19 }),
      contextChunk(2, { pageNumber: 22 }),
      contextChunk(3, { pageNumber: 19 }),
    ]

    const cited = citedChunks(sourcesEvent({ contextChunks: chunks, citations: [3, 1] }))

    expect(cited.map((chunk) => chunk.n)).toEqual([3, 1])
    expect(cited.map(sourceButtonLabel)).toEqual([
      'Raşit Bankası Ürün ve H… · s. 19',
      'Raşit Bankası Ürün ve H… · s. 19',
    ])
  })

  it('givenCitationOutsideContext_whenCitedChunks_thenIgnored', () => {
    const cited = citedChunks(sourcesEvent({ citations: [9, 2] }))

    expect(cited.map((chunk) => chunk.n)).toEqual([2])
  })

  it('givenTwoCitationsPointingToSameChunkId_whenCitedChunks_thenOneButton', () => {
    const chunks = [contextChunk(1, { chunkId: 500 }), contextChunk(2, { chunkId: 500 })]

    const cited = citedChunks(sourcesEvent({ contextChunks: chunks, citations: [1, 2] }))

    expect(cited.map((chunk) => chunk.n)).toEqual([1])
  })

  it('givenShortSourceName_whenSourceButtonLabel_thenNotShortened', () => {
    expect(sourceButtonLabel(contextChunk(1, { sourceName: 'Rehber', pageNumber: 3 }))).toBe('Rehber · s. 3')
  })

  it('givenLongSourceName_whenSourceButtonTitle_thenFullNameAndPage', () => {
    expect(sourceButtonTitle(contextChunk(1, { pageNumber: 19 }))).toBe(`${GUIDE_NAME} · sayfa 19`)
  })
})
