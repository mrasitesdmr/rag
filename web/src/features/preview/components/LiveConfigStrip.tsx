import type { LiveConfigResponse } from '../../../shared/liveConfig/types.ts'
import { liveConfigItems } from '../model/liveConfigItems.ts'
import styles from './LiveConfigStrip.module.css'

type LiveConfigStripProps = { config: LiveConfigResponse }

export function LiveConfigStrip({ config }: LiveConfigStripProps) {
  return (
    <div role="group" aria-label="Canlı yapılandırma" className={styles.strip}>
      {liveConfigItems(config).map((item) => (
        <span key={item.label} className={styles.item} data-tone={item.tone}>
          {item.label} <b>{item.value}</b>
        </span>
      ))}
    </div>
  )
}
