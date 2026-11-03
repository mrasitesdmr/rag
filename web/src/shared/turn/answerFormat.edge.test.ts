import { describe, expect, it } from 'vitest'
import { formatAnswer } from './answerFormat.ts'

describe('formatAnswer edge cases', () => {
  it('givenBulletWithExtraSpacesAfterMarker_whenFormatted_thenTextHasNoLeadingSpace', () => {
    expect(formatAnswer('-   Kartı blokla', null)).toEqual([
      { kind: 'list', items: [[{ kind: 'text', text: 'Kartı blokla' }]] },
    ])
  })

  it('givenBulletMarkerWithoutSpace_whenFormatted_thenParagraph', () => {
    expect(formatAnswer('-Kartı blokla', null)).toEqual([
      { kind: 'paragraph', inlines: [{ kind: 'text', text: '-Kartı blokla' }] },
    ])
  })

  it('givenTwoDigitCitationWithinContext_whenFormatted_thenCitation12', () => {
    expect(formatAnswer('Bkz. [12]', 12)).toEqual([
      {
        kind: 'paragraph',
        inlines: [
          { kind: 'text', text: 'Bkz. ' },
          { kind: 'citation', n: 12 },
        ],
      },
    ])
  })

  it('givenWhitespaceOnlyLine_whenFormatted_thenNoBlock', () => {
    expect(formatAnswer('Birinci\n   \nİkinci', null)).toEqual([
      { kind: 'paragraph', inlines: [{ kind: 'text', text: 'Birinci' }] },
      { kind: 'paragraph', inlines: [{ kind: 'text', text: 'İkinci' }] },
    ])
  })

  it('givenBoldAtLineStartWithoutContext_whenFormatted_thenNoEmptyTextBeforeBold', () => {
    expect(formatAnswer('**Önemli** not', null)).toEqual([
      {
        kind: 'paragraph',
        inlines: [
          { kind: 'bold', children: [{ kind: 'text', text: 'Önemli' }] },
          { kind: 'text', text: ' not' },
        ],
      },
    ])
  })

  it('givenCitationWithoutContext_whenFormatted_thenKeptAsPlainText', () => {
    expect(formatAnswer('Bkz. [1]', null)).toEqual([
      { kind: 'paragraph', inlines: [{ kind: 'text', text: 'Bkz. [1]' }] },
    ])
  })
})
