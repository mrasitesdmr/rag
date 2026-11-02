export const GENERATION_MODES = ['RAG', 'LLM_ONLY'] as const
export type GenerationMode = (typeof GENERATION_MODES)[number]

export const RERANKER_STATUSES = ['UP', 'DOWN', 'UNKNOWN', 'NOT_USED'] as const
export type RerankerStatus = (typeof RERANKER_STATUSES)[number]

export const KNOWLEDGE_BASE_STATUSES = ['READY', 'EMPTY', 'UNAVAILABLE'] as const
export type KnowledgeBaseStatus = (typeof KNOWLEDGE_BASE_STATUSES)[number]

export type KnowledgeBaseSource = {
  sourceId: number
  name: string
  version: string
}

export type KnowledgeBaseResponse = {
  status: KnowledgeBaseStatus
  searchableSourceCount: number | null
  sources: KnowledgeBaseSource[]
}

export type LiveConfigResponse = {
  chatModel: string
  embeddingModel: string | null
  embeddingDimensions: number | null
  generationMode: GenerationMode
  retrievalStrategy: 'TOP_K' | 'RERANK'
  candidates: number
  topK: number
  queryRewriteEnabled: boolean
  rerankerStatus: RerankerStatus
  knowledgeBase: KnowledgeBaseResponse
}
