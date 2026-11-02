import { Fragment } from 'react'
import { formatAnswer } from './answerFormat.ts'
import type { AnswerInline } from './answerFormat.ts'
import styles from './AnswerText.module.css'

type AnswerTextProps = {
  text: string
  contextSize: number | null
  onCitation: (n: number) => void
}

export function AnswerText({ text, contextSize, onCitation }: AnswerTextProps) {
  return formatAnswer(text, contextSize).map((block, blockIndex) =>
    block.kind === 'paragraph' ? (
      <p key={blockIndex}>
        <Inlines inlines={block.inlines} onCitation={onCitation} />
      </p>
    ) : (
      <ul key={blockIndex}>
        {block.items.map((item, itemIndex) => (
          <li key={itemIndex}>
            <Inlines inlines={item} onCitation={onCitation} />
          </li>
        ))}
      </ul>
    ),
  )
}

function Inlines({ inlines, onCitation }: { inlines: AnswerInline[]; onCitation: (n: number) => void }) {
  return inlines.map((inline, index) => {
    switch (inline.kind) {
      case 'text':
        return <Fragment key={index}>{inline.text}</Fragment>
      case 'bold':
        return (
          <strong key={index}>
            <Inlines inlines={inline.children} onCitation={onCitation} />
          </strong>
        )
      case 'citation':
        return (
          <button
            key={index}
            type="button"
            className={styles.cite}
            aria-label={`Kaynak ${inline.n}`}
            onClick={() => onCitation(inline.n)}
          >
            {inline.n}
          </button>
        )
    }
  })
}
