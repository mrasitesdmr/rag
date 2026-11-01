import type { DoneEvent } from './types.ts'

export type LatencyRow = { label: string; ms: number; isModel: boolean }

export function modelShortName(chatModel: string): string {
  const colonIndex = chatModel.indexOf(':')
  return colonIndex === -1 ? chatModel : chatModel.slice(0, colonIndex)
}

export function latencyRows(done: DoneEvent): LatencyRow[] {
  const rows: LatencyRow[] = [{ label: 'retrieval-api araması', ms: done.retrievalLatencyMs, isModel: false }]
  if (done.rerankLatencyMs !== null) {
    rows.push({ label: 'rerank', ms: done.rerankLatencyMs, isModel: false })
  }
  if (done.generationLatencyMs !== null) {
    rows.push({ label: `${modelShortName(done.chatModel)} cevabı`, ms: done.generationLatencyMs, isModel: true })
  }
  return rows
}
