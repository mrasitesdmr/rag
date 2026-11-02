import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { isApiErrorWithCode, isClientError } from '../../../shared/api/ApiError.ts'
import type { PageParams } from '../../../shared/api/pagination.ts'
import { EVAL_POLL_INTERVAL_MS } from '../model/evaluationRules.ts'
import type { EvalOverviewResponse, EvalRunResponse } from '../model/types.ts'
import { getEvalOverview, listEvalRuns, startEvalRun } from './evaluationApi.ts'

const EVALUATION_KEY = ['evaluation'] as const

export const evaluationKeys = {
  all: EVALUATION_KEY,
  overview: [...EVALUATION_KEY, 'overview'] as const,
  runsAll: [...EVALUATION_KEY, 'runs'] as const,
  runs: (params: PageParams) => [...EVALUATION_KEY, 'runs', params] as const,
}

export function useEvalOverviewQuery() {
  const queryClient = useQueryClient()

  async function fetchOverview(): Promise<EvalOverviewResponse> {
    const previous = queryClient.getQueryData<EvalOverviewResponse>(evaluationKeys.overview)
    const overview = await getEvalOverview()
    if (hasRunningRun(previous) && overview.runningRun === null) {
      void queryClient.invalidateQueries({ queryKey: evaluationKeys.runsAll })
    }
    return overview
  }

  return useQuery({
    queryKey: evaluationKeys.overview,
    queryFn: fetchOverview,
    refetchInterval: (query) => {
      if (isClientError(query.state.error)) {
        return false
      }
      return hasRunningRun(query.state.data) ? EVAL_POLL_INTERVAL_MS : false
    },
  })
}

export function useEvalRunsQuery(params: PageParams, isRunning: boolean) {
  return useQuery({
    queryKey: evaluationKeys.runs(params),
    queryFn: () => listEvalRuns(params),
    placeholderData: keepPreviousData,
    refetchInterval: (query) => (isRunning && !isClientError(query.state.error) ? EVAL_POLL_INTERVAL_MS : false),
  })
}

export function useStartEvalRunMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: startEvalRun,
    onSuccess: (run: EvalRunResponse) => {
      queryClient.setQueryData<EvalOverviewResponse>(evaluationKeys.overview, (overview) =>
        overview === undefined ? overview : { ...overview, runningRun: run },
      )
      void queryClient.invalidateQueries({ queryKey: evaluationKeys.runsAll })
    },
    onError: (error) => {
      if (isApiErrorWithCode(error, 'EVAL_RUN_IN_PROGRESS')) {
        void queryClient.invalidateQueries({ queryKey: evaluationKeys.overview })
      }
    },
  })
}

function hasRunningRun(overview: EvalOverviewResponse | undefined): boolean {
  return overview !== undefined && overview.runningRun !== null
}
