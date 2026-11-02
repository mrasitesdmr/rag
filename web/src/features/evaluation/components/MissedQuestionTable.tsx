import type { MouseEvent } from 'react'
import { useNavigate } from 'react-router'
import { StatusBadge } from '../../../shared/components/StatusBadge.tsx'
import table from '../../../shared/components/table.module.css'
import { promptPositionText, vectorRankText } from '../model/evaluationLabels.ts'
import { ASK_IN_PREVIEW_TITLE, MISSED_EMPTY_TEXT } from '../model/evaluationTexts.ts'
import type { EvalQuestionResultResponse } from '../model/types.ts'
import styles from './MissedQuestionTable.module.css'

const PREVIEW_PATH = '/onizleme'

type MissedQuestionTableProps = { questions: EvalQuestionResultResponse[]; candidates: number }

export function MissedQuestionTable({ questions, candidates }: MissedQuestionTableProps) {
  const navigate = useNavigate()

  function askInPreview(question: string) {
    void navigate(PREVIEW_PATH, { state: { question } })
  }

  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>Soru</th>
            <th>Vector #</th>
            <th>{"Prompt'taki yeri"}</th>
          </tr>
        </thead>
        <tbody>
          {questions.length === 0 ? (
            <tr>
              <td colSpan={3} className={table.text}>
                {MISSED_EMPTY_TEXT}
              </td>
            </tr>
          ) : (
            questions.map((question) => (
              <MissedQuestionRow
                key={question.position}
                question={question}
                candidates={candidates}
                onAsk={askInPreview}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

type MissedQuestionRowProps = {
  question: EvalQuestionResultResponse
  candidates: number
  onAsk: (question: string) => void
}

function MissedQuestionRow({ question, candidates, onAsk }: MissedQuestionRowProps) {
  function handleRowClick(event: MouseEvent<HTMLTableRowElement>) {
    event.currentTarget.querySelector('button')?.focus()
    onAsk(question.text)
  }

  return (
    // The question button is the keyboard path; a click on the rest of the row asks the question like the lab.
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <tr className={table.clickable} title={ASK_IN_PREVIEW_TITLE} onClick={handleRowClick}>
      <td className={table.text}>
        <button type="button" className={styles.askButton}>
          {question.text}
        </button>
      </td>
      <td className={table.num}>{vectorRankText(question.vectorRank, candidates)}</td>
      <td>
        <StatusBadge tone={question.promptPosition === null ? 'bad' : 'ok'}>
          {promptPositionText(question.promptPosition)}
        </StatusBadge>
      </td>
    </tr>
  )
}
