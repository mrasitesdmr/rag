import { formatInteger } from '../../../shared/format/formatNumber.ts'
import type { SourceResponse, WorkspaceResponse } from '../model/types.ts'
import styles from './SourceMetaGrid.module.css'

const MISSING = '–'

type SourceMetaGridProps = { source: SourceResponse; workspace: WorkspaceResponse | undefined }

export function SourceMetaGrid({ source, workspace }: SourceMetaGridProps) {
  const items = [
    { label: 'Sayfa / bölüm', value: pagesValue(source) },
    { label: 'Parça', value: chunksValue(source) },
    {
      label: 'Parça boyutu',
      value: workspace
        ? `${formatInteger(workspace.targetChunkChars)}-${formatInteger(workspace.maxChunkChars)} karakter`
        : MISSING,
    },
    { label: 'Örtüşme', value: workspace ? `${formatInteger(workspace.overlapChars)} karakter` : MISSING },
    { label: 'Embedding', value: workspace ? `${workspace.embeddingModel} · ${workspace.embeddingDimensions}` : MISSING },
    {
      label: 'Index',
      value: workspace ? `${workspace.vectorIndexType} · ${workspace.distanceMetric.toLowerCase()}` : MISSING,
    },
  ]
  return (
    <dl className={styles.metaGrid}>
      {items.map((item) => (
        <div key={item.label}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}

function pagesValue(source: SourceResponse): string {
  if (source.pageCount === null) {
    return MISSING
  }
  return `${source.pageCount} / ${source.chapterCount ?? MISSING}`
}

function chunksValue(source: SourceResponse): string {
  if (source.chunkCount === null) {
    return MISSING
  }
  return `${formatInteger(source.chunkCount)} · ort. ${source.averageTokenCount ?? MISSING} token`
}
