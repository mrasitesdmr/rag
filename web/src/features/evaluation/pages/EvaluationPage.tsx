import { PageHeader } from '../../../shared/components/PageHeader.tsx'

export function EvaluationPage() {
  return (
    <PageHeader
      section="Değerlendirme"
      title="Değerlendirme"
      description="Rehberde cevabı olan sorular; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar."
    />
  )
}
