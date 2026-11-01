import type { ReactNode } from 'react'
import { NavLink } from 'react-router'
import styles from './MainNav.module.css'

type NavItem = { path: string; label: string; end: boolean; icon: ReactNode }

const NAV_ITEMS: NavItem[] = [
  {
    path: '/onizleme',
    end: true,
    label: 'Önizleme',
    icon: (
      <>
        <rect x="6" y="2.5" width="12" height="19" rx="2.5" />
        <path d="M10.5 18.5h3" />
      </>
    ),
  },
  {
    path: '/konusmalar',
    end: true,
    label: 'Konuşmalar',
    icon: <path d="M4 6h16M4 12h16M4 18h10" />,
  },
  {
    path: '/kaynaklar',
    end: false,
    label: 'Bilgi kaynakları',
    icon: (
      <>
        <path d="M6 3h9l4 4v14H6z" />
        <path d="M15 3v4h4M9 12h7M9 16h5" />
      </>
    ),
  },
  {
    path: '/degerlendirme',
    end: true,
    label: 'Değerlendirme',
    icon: <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />,
  },
]

function navLinkClassName({ isActive }: { isActive: boolean }): string {
  return isActive ? `${styles.navLink} ${styles.isActive}` : styles.navLink
}

export function MainNav() {
  return (
    <nav className={styles.mainNav} aria-label="Menü">
      {NAV_ITEMS.map((item) => (
        <NavLink key={item.path} to={item.path} end={item.end} className={navLinkClassName}>
          <svg
            className={styles.navIcon}
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden="true"
          >
            {item.icon}
          </svg>
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}
