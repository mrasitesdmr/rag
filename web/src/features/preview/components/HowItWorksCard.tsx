import { useId } from 'react'
import card from './sideCard.module.css'

export function HowItWorksCard() {
  const titleId = useId()
  return (
    <section className={card.card} aria-labelledby={titleId}>
      <h2 id={titleId} className={card.title}>
        Asistan nasıl cevaplıyor?
      </h2>
      <div className={card.sub}>
        Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5'i qwen3.5:9b'ye gider.
        Cevaptaki numaralar rehberin parçalarını gösterir.
      </div>
      <div className={card.sub}>Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek.</div>
    </section>
  )
}
