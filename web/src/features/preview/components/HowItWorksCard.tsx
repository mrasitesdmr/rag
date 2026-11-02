import { useId } from 'react'
import type { LiveConfigResponse } from '../../../shared/liveConfig/types.ts'
import { howItWorksText } from '../model/howItWorks.ts'
import card from './sideCard.module.css'

type HowItWorksCardProps = { config: LiveConfigResponse | undefined }

export function HowItWorksCard({ config }: HowItWorksCardProps) {
  const titleId = useId()
  return (
    <section className={card.card} aria-labelledby={titleId}>
      <h2 id={titleId} className={card.title}>
        Asistan nasıl cevaplıyor?
      </h2>
      {config === undefined ? null : <div className={card.sub}>{howItWorksText(config)}</div>}
      <div className={card.sub}>Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek.</div>
    </section>
  )
}
