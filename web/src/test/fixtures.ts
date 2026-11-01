import type { PageResponse } from '../shared/api/pagination.ts'
import type { ChunkResponse } from '../shared/chunk/types.ts'
import type {
  ChunkSummaryResponse,
  IngestionJobResponse,
  SourceResponse,
  WorkspaceResponse,
} from '../features/source/model/types.ts'

export const GUIDE_NAME = 'Raşit Bankası Ürün ve Hizmet Rehberi'
export const GUIDE_FILE = 'Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf'

export function workspace(overrides: Partial<WorkspaceResponse> = {}): WorkspaceResponse {
  return {
    workspaceId: 42,
    name: 'Raşit Bankası · Dijital Asistan',
    embeddingModel: 'bge-m3',
    embeddingDimensions: 1024,
    targetChunkChars: 1600,
    maxChunkChars: 2000,
    overlapChars: 200,
    vectorIndexType: 'HNSW',
    distanceMetric: 'COSINE',
    ...overrides,
  }
}

export function job(overrides: Partial<IngestionJobResponse> = {}): IngestionJobResponse {
  const jobId = overrides.jobId ?? 1
  return {
    jobId,
    workspaceId: 42,
    sourceId: 10001,
    version: '2026.10',
    fileName: GUIDE_FILE,
    stage: 'DONE',
    failedStage: null,
    progress: 1,
    embeddedChunks: 154,
    totalChunks: 154,
    errorCode: null,
    createdAt: '2026-10-29T13:15:30Z',
    startedAt: '2026-10-29T13:15:35Z',
    finishedAt: '2026-10-29T13:15:44Z',
    statusUrl: `/api/workspaces/42/ingestion-jobs/${jobId}`,
    ...overrides,
  }
}

export function source(overrides: Partial<SourceResponse> = {}): SourceResponse {
  return {
    sourceId: 10001,
    workspaceId: 42,
    name: GUIDE_NAME,
    kind: 'PDF',
    createdAt: '2026-10-29T13:15:30Z',
    fileName: GUIDE_FILE,
    fileSizeBytes: 412873,
    version: '2026.10',
    pageCount: 70,
    chapterCount: 11,
    chunkCount: 154,
    averageTokenCount: 285,
    syncedAt: '2026-10-29T13:15:44Z',
    latestJob: job(),
    ...overrides,
  }
}

export function failedSource(overrides: Partial<SourceResponse> = {}): SourceResponse {
  return source({
    sourceId: 10002,
    name: 'Atlaskap El Kitabı',
    fileName: 'atlaskap.pdf',
    fileSizeBytes: 1000,
    version: '1.0',
    pageCount: null,
    chapterCount: null,
    chunkCount: null,
    averageTokenCount: null,
    syncedAt: null,
    latestJob: job({
      jobId: 5,
      sourceId: 10002,
      fileName: 'atlaskap.pdf',
      version: '1.0',
      stage: 'FAILED',
      failedStage: 'EMBEDDING',
      errorCode: 'EMBEDDING_UNAVAILABLE',
      totalChunks: null,
      embeddedChunks: 0,
      finishedAt: '2026-10-29T13:20:00Z',
    }),
    ...overrides,
  })
}

export function pageOf<T>(
  content: T[],
  options: { page?: number; size?: number; totalElements?: number } = {},
): PageResponse<T> {
  const size = options.size ?? 20
  const totalElements = options.totalElements ?? content.length
  return {
    content,
    page: options.page ?? 0,
    size,
    totalElements,
    totalPages: Math.ceil(totalElements / size),
  }
}

export function chunkSummary(chunkNo: number, overrides: Partial<ChunkSummaryResponse> = {}): ChunkSummaryResponse {
  return {
    chunkId: 100 + chunkNo,
    chunkNo,
    pageNumber: chunkNo + 3,
    heading: `Başlık ${chunkNo}`,
    content: `İçerik ${chunkNo}`,
    tokenCount: 200 + chunkNo,
    ...overrides,
  }
}

export function chunkDetail(overrides: Partial<ChunkResponse> = {}): ChunkResponse {
  return {
    chunkId: 151,
    chunkNo: 31,
    pageNumber: 19,
    heading: '2.16 Kayıp ya da çalıntı bildirimi',
    content: 'Kartınızı kaybederseniz\nhemen 7/24 hattını arayın.',
    tokenCount: 297,
    embeddingModel: 'bge-m3@1024',
    embeddingDimensions: 1024,
    embeddingPreview: [0.010856, -0.001934, -0.065783, 0.013642, -0.017388, -0.030141, 0.012092, -0.046688],
    sourceId: 10001,
    sourceName: GUIDE_NAME,
    version: '2026.10',
    ...overrides,
  }
}
