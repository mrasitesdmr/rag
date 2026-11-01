export const INGESTION_STAGES = ['QUEUED', 'PARSING', 'CHUNKING', 'EMBEDDING', 'WRITING', 'DONE', 'FAILED'] as const
export type IngestionStage = (typeof INGESTION_STAGES)[number]

export type SourceKind = 'PDF'

export type WorkspaceResponse = {
  workspaceId: number
  name: string
  embeddingModel: string
  embeddingDimensions: number
  targetChunkChars: number
  maxChunkChars: number
  overlapChars: number
  vectorIndexType: string
  distanceMetric: string
}

export type IngestionJobResponse = {
  jobId: number
  workspaceId: number
  sourceId: number
  version: string
  fileName: string
  stage: IngestionStage
  failedStage: IngestionStage | null
  progress: number
  embeddedChunks: number
  totalChunks: number | null
  errorCode: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  statusUrl: string
}

export type SourceResponse = {
  sourceId: number
  workspaceId: number
  name: string
  kind: SourceKind
  createdAt: string
  fileName: string
  fileSizeBytes: number
  version: string
  pageCount: number | null
  chapterCount: number | null
  chunkCount: number | null
  averageTokenCount: number | null
  syncedAt: string | null
  latestJob: IngestionJobResponse
}

export type ChunkSummaryResponse = {
  chunkId: number
  chunkNo: number
  pageNumber: number
  heading: string
  content: string
  tokenCount: number
}

export type CreateSourceInput = { file: File | null; name: string; version: string }

export type ListChunksParams = { query: string; page: number; size: number }
