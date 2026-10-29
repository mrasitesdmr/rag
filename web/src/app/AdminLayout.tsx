import { Outlet } from 'react-router'
import styles from './AdminLayout.module.css'
import { AppHeader } from './AppHeader.tsx'
import { MainNav } from './MainNav.tsx'

export function AdminLayout() {
  return (
    <div className={styles.adminLayout}>
      <AppHeader />
      <MainNav />
      <main className={styles.page}>
        <Outlet />
      </main>
    </div>
  )
}
