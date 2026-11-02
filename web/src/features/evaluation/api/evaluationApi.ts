import { request, withQuery } from '../../../shared/api/httpClient.ts'
import type { PageParams, PageResponse } from '../../../shared/api/pagination.ts'
import type { EvalOverviewResponse, EvalRunResponse } from '../model/types.ts'

const EVALS_PATH = '/api/assistant/evals'

export function getEvalOverview(): Promise<EvalOverviewResponse> {
  return request<EvalOverviewResponse>(`${EVALS_PATH}/overview`)
}

export function startEvalRun(): Promise<EvalRunResponse> {
  return request<EvalRunResponse>(EVALS_PATH, { method: 'POST' })
}

export function listEvalRuns(params: PageParams): Promise<PageResponse<EvalRunResponse>> {
  return request<PageResponse<EvalRunResponse>>(withQuery(EVALS_PATH, params))
}
