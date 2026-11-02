import { useState } from 'react'
import { errorCodeOf } from '../../../shared/api/ApiError.ts'
import button from '../../../shared/components/button.module.css'
import { Callout } from '../../../shared/components/Callout.tsx'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import { Pagination } from '../../../shared/components/Pagination.tsx'
import hidden from '../../../shared/components/visuallyHidden.module.css'
import { useLiveConfigQuery } from '../../../shared/liveConfig/liveConfigQueries.ts'
import { useEvalOverviewQuery, useEvalRunsQuery, useStartEvalRunMutation } from '../api/evaluationQueries.ts'
import { EvalKpis } from '../components/EvalKpis.tsx'
import { EvalRunTable } from '../components/EvalRunTable.tsx'
import { MissedQuestionTable } from '../components/MissedQuestionTable.tsx'
import { useEvalRunAnnouncement } from '../hooks/useEvalRunAnnouncement.ts'
import { runButtonText, runFailureText } from '../model/evaluationLabels.ts'
import { EVAL_RUNS_PAGE_SIZE } from '../model/evaluationRules.ts'
import {
  GOLDEN_SET_NOT_LOADED_NOTICE,
  LLM_ONLY_WARNING,
  LOADING_TEXT,
  MISSED_SECTION_NOTE,
  MISSED_SECTION_TITLE,
  NO_MEASUREMENT_TEXT,
  PAGE_DESCRIPTION_WITHOUT_SET,
  RUNS_SECTION_TITLE,
  pageDescription,
} from '../model/evaluationTexts.ts'
import styles from './EvaluationPage.module.css'

export function EvaluationPage() {
  const overviewQuery = useEvalOverviewQuery()
  const liveConfigQuery = useLiveConfigQuery()
  const startEvalRun = useStartEvalRunMutation()
  const [page, setPage] = useState(0)
  const [now] = useState(() => new Date())
  const overview = overviewQuery.data
  const liveConfig = liveConfigQuery.data
  const runningRun = overview?.runningRun ?? null
  const runsQuery = useEvalRunsQuery({ page, size: EVAL_RUNS_PAGE_SIZE }, runningRun !== null)
  const runPage = runsQuery.data
  const announcement = useEvalRunAnnouncement(overview)
  const goldenSet = overview?.goldenSet ?? null
  const isLlmOnly = liveConfig?.generationMode === 'LLM_ONLY'
  const isRunDisabled =
    liveConfig === undefined ||
    isLlmOnly ||
    overview === undefined ||
    goldenSet === null ||
    runningRun !== null ||
    startEvalRun.isPending
  const lastFinishedRun = overview?.lastFinishedRun ?? null
  const lastDoneRun = overview?.lastDoneRun ?? null
  const hasRuns = runPage !== undefined && runPage.totalElements > 0

  return (
    <>
      <PageHeader
        section="Değerlendirme"
        title="Değerlendirme"
        description={goldenSet === null ? PAGE_DESCRIPTION_WITHOUT_SET : pageDescription(goldenSet.questionCount)}
        actions={
          <button type="button" className={button.button} disabled={isRunDisabled} onClick={() => startEvalRun.mutate()}>
            {runButtonText(liveConfig, runningRun)}
          </button>
        }
      />
      {isLlmOnly ? <Callout tone="warning">{LLM_ONLY_WARNING}</Callout> : null}
      {liveConfig === undefined && liveConfigQuery.isError ? (
        <ErrorAlert errorCode={errorCodeOf(liveConfigQuery.error)} />
      ) : null}
      {overview === undefined ? (
        overviewQuery.isError ? (
          <ErrorAlert errorCode={errorCodeOf(overviewQuery.error)} />
        ) : (
          <p className={styles.status}>{LOADING_TEXT}</p>
        )
      ) : (
        <>
          {goldenSet === null ? <Callout>{GOLDEN_SET_NOT_LOADED_NOTICE}</Callout> : null}
          {startEvalRun.isError ? <ErrorAlert errorCode={errorCodeOf(startEvalRun.error)} /> : null}
          {lastFinishedRun !== null && lastFinishedRun.status === 'FAILED' ? (
            <Callout tone="warning">{runFailureText(lastFinishedRun.errorCode)}</Callout>
          ) : null}
          {lastDoneRun === null ? (
            <Callout hasIcon={false}>{NO_MEASUREMENT_TEXT}</Callout>
          ) : (
            <EvalKpis run={lastDoneRun} />
          )}
          {hasRuns || lastDoneRun !== null ? (
            <>
              <div className={styles.section}>
                <h2>{RUNS_SECTION_TITLE}</h2>
              </div>
              {runPage === undefined ? (
                runsQuery.isError ? (
                  <ErrorAlert errorCode={errorCodeOf(runsQuery.error)} />
                ) : (
                  <p className={styles.status}>{LOADING_TEXT}</p>
                )
              ) : (
                <>
                  <EvalRunTable runs={runPage.content} now={now} />
                  {runPage.totalPages > 1 ? (
                    <Pagination
                      label="Çalıştırma sayfaları"
                      page={page}
                      totalPages={runPage.totalPages}
                      onPageChange={setPage}
                    />
                  ) : null}
                </>
              )}
            </>
          ) : null}
          {lastDoneRun === null ? null : (
            <>
              <div className={styles.section}>
                <h2>{MISSED_SECTION_TITLE}</h2>
                <span>{MISSED_SECTION_NOTE}</span>
              </div>
              <MissedQuestionTable questions={lastDoneRun.missedAndMovedQuestions} candidates={lastDoneRun.candidates} />
            </>
          )}
        </>
      )}
      <div className={hidden.visuallyHidden} aria-live="polite">
        {announcement}
      </div>
    </>
  )
}
