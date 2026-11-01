import { useQuery } from '@tanstack/react-query'
import { getChunk } from './chunkApi.ts'

export const chunkKeys = {
  all: ['chunks'] as const,
  detail: (chunkId: number) => [...chunkKeys.all, 'detail', chunkId] as const,
}

export function useChunkQuery(chunkId: number | null) {
  return useQuery({
    queryKey: chunkKeys.detail(chunkId ?? -1),
    queryFn: () => getChunk(chunkId ?? -1),
    enabled: chunkId !== null,
  })
}
