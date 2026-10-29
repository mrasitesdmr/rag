import { Link } from 'react-router'
import styles from './AppHeader.module.css'

export function AppHeader() {
  return (
    <header className={styles.appHeader}>
      <Link to="/onizleme" className={styles.logo} aria-label="Önizleme">
        <svg width="30" height="30" viewBox="0 0 30 30" aria-hidden="true" className={styles.logoMark}>
          <rect width="30" height="30" rx="8" fill="var(--color-brand)" />
          <circle cx="15" cy="15" r="8.4" fill="var(--color-card)" />
          <rect x="13.1" y="9.2" width="3.8" height="11.6" rx="1.2" fill="var(--color-brand)" />
          <rect x="3" y="25.5" width="24" height="1.6" rx=".8" fill="var(--color-accent)" />
        </svg>
        <span className={styles.wordmark}>
          <b>Raşit Bankası</b>
          <small>Dijital Asistan Yönetimi</small>
        </span>
      </Link>
      <span className={styles.liveConfigBadge} title="Önizleme, canlı asistanla aynı yapılandırmayı kullanır">
        Canlı yapılandırma
      </span>
    </header>
  )
}
