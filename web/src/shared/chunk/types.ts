export type ChunkCitation = { chunkId: number; n: number }

export type ChunkResponse = {
  chunkId: number
  chunkNo: number
  pageNumber: number
  heading: string
  content: string
  tokenCount: number
  embeddingModel: string
  embeddingDimensions: number
  embeddingPreview: number[]
  sourceId: number
  sourceName: string
  version: string
}
