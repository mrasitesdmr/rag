import { PageHeader } from '../../../shared/components/PageHeader.tsx'

export function SourceListPage() {
  return (
    <PageHeader
      section="Bilgi kaynakları"
      title="Bilgi kaynakları"
      description="Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça embedding modeliyle embed edilip pgvector'e yazılır."
    />
  )
}
