import { request } from '../api/httpClient.ts'
import { WORKSPACE_API_PATH } from '../api/workspace.ts'
import type { ChunkResponse } from './types.ts'

export function getChunk(chunkId: number): Promise<ChunkResponse> {
  return request<ChunkResponse>(`${WORKSPACE_API_PATH}/chunks/${chunkId}`)
}
