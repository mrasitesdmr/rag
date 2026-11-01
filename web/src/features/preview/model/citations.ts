import { shortenText } from '../../../shared/format/formatText.ts'
import type { ContextChunk, SourcesEvent } from './types.ts'

export const SOURCE_NAME_SHORT_LENGTH = 24

export function contextChunkOf(sources: SourcesEvent, n: number): ContextChunk | undefined {
  return sources.contextChunks.find((chunk) => chunk.n === n)
}

export function citedChunks(sources: SourcesEvent): ContextChunk[] {
  const seen = new Set<number>()
  const chunks: ContextChunk[] = []
  for (const n of sources.citations) {
    const chunk = contextChunkOf(sources, n)
    if (chunk !== undefined && !seen.has(chunk.chunkId)) {
      seen.add(chunk.chunkId)
      chunks.push(chunk)
    }
  }
  return chunks
}

export function sourceButtonLabel(chunk: ContextChunk): string {
  return `${shortenText(chunk.sourceName, SOURCE_NAME_SHORT_LENGTH)} · s. ${chunk.pageNumber}`
}

export function sourceButtonTitle(chunk: ContextChunk): string {
  return `${chunk.sourceName} · sayfa ${chunk.pageNumber}`
}
