import { request } from '../api/httpClient.ts'
import type { LiveConfigResponse } from './types.ts'

export function getLiveConfig(): Promise<LiveConfigResponse> {
  return request<LiveConfigResponse>('/api/assistant/live-config')
}
