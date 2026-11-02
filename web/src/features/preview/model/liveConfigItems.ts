import { shortenText } from '../../../shared/format/formatText.ts'
import type { KnowledgeBaseResponse, LiveConfigResponse } from '../../../shared/liveConfig/types.ts'
import { RETRIEVAL_OFF_LABEL } from './strategyLabels.ts'

export const LIVE_CONFIG_TONES = ['normal', 'warning', 'danger'] as const
export type LiveConfigTone = (typeof LIVE_CONFIG_TONES)[number]

export type LiveConfigItem = { label: string; value: string; tone: LiveConfigTone }

export const KNOWLEDGE_BASE_NAME_SHORT_LENGTH = 40
export const MISSING_VALUE = '—'
export const RERANKER_DOWN_STRIP_LABEL = 'reranker DOWN'
export const KNOWLEDGE_BASE_LABEL = 'Bilgi kaynakları'

export function liveConfigItems(config: LiveConfigResponse): LiveConfigItem[] {
  return [
    { label: 'Model', value: config.chatModel, tone: 'normal' },
    { label: 'Embedding', value: config.embeddingModel ?? MISSING_VALUE, tone: 'normal' },
    retrievalItem(config),
    {
      label: 'Soru yeniden yazma',
      value: config.queryRewriteEnabled ? 'açık' : 'kapalı',
      tone: config.queryRewriteEnabled ? 'normal' : 'warning',
    },
    knowledgeBaseItem(config.knowledgeBase),
  ]
}

function retrievalItem(config: LiveConfigResponse): LiveConfigItem {
  const label = 'Retrieval'
  if (config.generationMode === 'LLM_ONLY') {
    return { label, value: RETRIEVAL_OFF_LABEL, tone: 'danger' }
  }
  if (config.retrievalStrategy === 'TOP_K') {
    return { label, value: `top-${config.topK}`, tone: 'normal' }
  }
  if (config.rerankerStatus === 'DOWN') {
    return { label, value: RERANKER_DOWN_STRIP_LABEL, tone: 'warning' }
  }
  return { label, value: `rerank ${config.candidates}→${config.topK}`, tone: 'normal' }
}

function knowledgeBaseItem(knowledgeBase: KnowledgeBaseResponse): LiveConfigItem {
  const label = KNOWLEDGE_BASE_LABEL
  switch (knowledgeBase.status) {
    case 'UNAVAILABLE':
      return { label, value: 'ulaşılamıyor', tone: 'warning' }
    case 'EMPTY':
      return { label, value: 'belge yok', tone: 'warning' }
    case 'READY': {
      const [onlySource] = knowledgeBase.sources
      if (knowledgeBase.sources.length === 1 && onlySource !== undefined) {
        return {
          label,
          value: `${shortenText(onlySource.name, KNOWLEDGE_BASE_NAME_SHORT_LENGTH)} · ${onlySource.version}`,
          tone: 'normal',
        }
      }
      return { label, value: `${knowledgeBase.searchableSourceCount ?? knowledgeBase.sources.length} belge`, tone: 'normal' }
    }
  }
}
