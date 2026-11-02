import { useQuery } from '@tanstack/react-query'
import { getLiveConfig } from './liveConfigApi.ts'
import { LIVE_CONFIG_POLL_INTERVAL_MS } from './liveConfigRules.ts'

export const liveConfigKeys = {
  all: ['liveConfig'] as const,
}

export function useLiveConfigQuery() {
  return useQuery({
    queryKey: liveConfigKeys.all,
    queryFn: getLiveConfig,
    refetchInterval: LIVE_CONFIG_POLL_INTERVAL_MS,
  })
}
