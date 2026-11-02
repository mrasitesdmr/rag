export type AnswerInline =
  | { kind: 'text'; text: string }
  | { kind: 'citation'; n: number }
  | { kind: 'bold'; children: AnswerInline[] }

export type AnswerBlock = { kind: 'paragraph'; inlines: AnswerInline[] } | { kind: 'list'; items: AnswerInline[][] }

const BULLET_LINE = /^\s*[*-]\s+(.*)$/
const BOLD = /\*\*(.+?)\*\*/g
const CITATION = /\[(\d+)\]/g

export function formatAnswer(text: string, contextSize: number | null): AnswerBlock[] {
  const blocks: AnswerBlock[] = []
  let list: AnswerInline[][] | null = null
  for (const line of text.split('\n')) {
    const bullet = BULLET_LINE.exec(line)
    if (bullet !== null) {
      if (list === null) {
        list = []
        blocks.push({ kind: 'list', items: list })
      }
      list.push(parseInlines(bullet[1] ?? '', contextSize))
      continue
    }
    list = null
    if (line.trim() !== '') {
      blocks.push({ kind: 'paragraph', inlines: parseInlines(line, contextSize) })
    }
  }
  return blocks
}

function parseInlines(line: string, contextSize: number | null): AnswerInline[] {
  const inlines: AnswerInline[] = []
  let cursor = 0
  for (const match of line.matchAll(BOLD)) {
    inlines.push(...parseCitations(line.slice(cursor, match.index), contextSize))
    inlines.push({ kind: 'bold', children: parseCitations(match[1] ?? '', contextSize) })
    cursor = match.index + match[0].length
  }
  inlines.push(...parseCitations(line.slice(cursor), contextSize))
  return inlines
}

function parseCitations(text: string, contextSize: number | null): AnswerInline[] {
  if (text === '') {
    return []
  }
  if (contextSize === null) {
    return [{ kind: 'text', text }]
  }
  const inlines: AnswerInline[] = []
  let pending = ''
  let cursor = 0
  for (const match of text.matchAll(CITATION)) {
    const n = Number(match[1])
    if (n < 1 || n > contextSize) {
      continue
    }
    pending += text.slice(cursor, match.index)
    if (pending !== '') {
      inlines.push({ kind: 'text', text: pending })
      pending = ''
    }
    inlines.push({ kind: 'citation', n })
    cursor = match.index + match[0].length
  }
  pending += text.slice(cursor)
  if (pending !== '') {
    inlines.push({ kind: 'text', text: pending })
  }
  return inlines
}
