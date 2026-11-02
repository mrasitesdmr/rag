import type { LiveConfigResponse } from '../../../shared/liveConfig/types.ts'

const SOURCE_NOTE = 'Cevaptaki numaralar rehberin parçalarını gösterir.'

export function howItWorksText(config: LiveConfigResponse): string {
  const search = `Soru önce rehberde aranır (${embeddingText(config.embeddingModel)})`
  if (config.generationMode === 'LLM_ONLY') {
    return `${search}, bu modda arama kapalı; soru doğrudan modele gider.`
  }
  if (config.retrievalStrategy === 'RERANK' && config.rerankerStatus !== 'DOWN') {
    return `${search}, ${config.candidates} aday reranker ile puanlanıp en iyi ${config.topK} parça ${config.chatModel} modeline gider. ${SOURCE_NOTE}`
  }
  return `${search}, en yakın ${config.topK} parça ${config.chatModel} modeline gider. ${SOURCE_NOTE}`
}

function embeddingText(embeddingModel: string | null): string {
  return embeddingModel === null ? 'pgvector' : `${embeddingModel} + pgvector`
}
