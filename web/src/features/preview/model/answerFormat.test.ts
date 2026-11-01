import { describe, expect, it } from 'vitest'
import { formatAnswer } from './answerFormat.ts'

describe('formatAnswer', () => {
  it('givenInAndOutOfRangeNumbers_whenFormatAnswerAfterDone_thenOnlyInRangeAreCitations', () => {
    const blocks = formatAnswer('Kartını kapat [1]. Bak [7].', 5)

    expect(blocks).toEqual([
      {
        kind: 'paragraph',
        inlines: [
          { kind: 'text', text: 'Kartını kapat ' },
          { kind: 'citation', n: 1 },
          { kind: 'text', text: '. Bak [7].' },
        ],
      },
    ])
  })

  it('givenContextSizeNull_whenFormatAnswer_thenNoCitationWhileStreaming', () => {
    const blocks = formatAnswer('Kartını kapat [1].', null)

    expect(blocks).toEqual([{ kind: 'paragraph', inlines: [{ kind: 'text', text: 'Kartını kapat [1].' }] }])
  })

  it.each([[0], [6]])('givenBoundaryNumberOutsideRange_whenFormatAnswer_thenStaysText %i', (n) => {
    const blocks = formatAnswer(`a [${n}]`, 5)

    expect(blocks).toEqual([{ kind: 'paragraph', inlines: [{ kind: 'text', text: `a [${n}]` }] }])
  })

  it('givenNumberEqualToContextSize_whenFormatAnswer_thenCitation', () => {
    const blocks = formatAnswer('[5]', 5)

    expect(blocks).toEqual([{ kind: 'paragraph', inlines: [{ kind: 'citation', n: 5 }] }])
  })

  it('givenBoldAndBulletLines_whenFormatAnswer_thenBoldInlineAndListBlock', () => {
    const blocks = formatAnswer('**Önemli:** kapat [1]\n- bir [2]\n- iki', 5)

    expect(blocks).toEqual([
      {
        kind: 'paragraph',
        inlines: [
          { kind: 'bold', children: [{ kind: 'text', text: 'Önemli:' }] },
          { kind: 'text', text: ' kapat ' },
          { kind: 'citation', n: 1 },
        ],
      },
      {
        kind: 'list',
        items: [
          [
            { kind: 'text', text: 'bir ' },
            { kind: 'citation', n: 2 },
          ],
          [{ kind: 'text', text: 'iki' }],
        ],
      },
    ])
  })

  it('givenAsteriskBulletsAndBlankLines_whenFormatAnswer_thenSeparateListsAndNoEmptyParagraphs', () => {
    const blocks = formatAnswer('* a\n* b\n\nson\n- c', 5)

    expect(blocks).toEqual([
      { kind: 'list', items: [[{ kind: 'text', text: 'a' }], [{ kind: 'text', text: 'b' }]] },
      { kind: 'paragraph', inlines: [{ kind: 'text', text: 'son' }] },
      { kind: 'list', items: [[{ kind: 'text', text: 'c' }]] },
    ])
  })

  it('givenHtmlInAnswer_whenFormatAnswer_thenKeptAsPlainText', () => {
    const blocks = formatAnswer('<img src=x onerror=alert(1)>', 5)

    expect(blocks).toEqual([{ kind: 'paragraph', inlines: [{ kind: 'text', text: '<img src=x onerror=alert(1)>' }] }])
  })

  it('givenEmptyAnswer_whenFormatAnswer_thenNoBlocks', () => {
    expect(formatAnswer('', 5)).toEqual([])
  })
})
