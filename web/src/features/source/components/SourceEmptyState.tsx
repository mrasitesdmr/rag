import { useId } from 'react'
import button from '../../../shared/components/button.module.css'
import { PdfIcon } from './PdfIcon.tsx'
import styles from './SourceEmptyState.module.css'

type SourceEmptyStateProps = { onAdd: () => void }

export function SourceEmptyState({ onAdd }: SourceEmptyStateProps) {
  const titleId = useId()
  return (
    <section className={styles.emptyState} aria-labelledby={titleId}>
      <PdfIcon className={styles.icon} />
      <h2 id={titleId} className={styles.title}>
        Henüz belge yok
      </h2>
      <p className={styles.text}>
        Asistan, cevap verirken buraya eklediğin belgelerde arar. İlk belgeni ekleyerek başla.
      </p>
      <button type="button" className={button.button} onClick={onAdd}>
        Belge ekle
      </button>
    </section>
  )
}
