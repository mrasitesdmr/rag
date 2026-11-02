import { ErrorBoundary } from 'react-error-boundary'
import { Outlet, useLocation } from 'react-router'
import { FALLBACK_PAGE_MESSAGE } from '../shared/api/errorMessages.ts'
import { ToastProvider } from '../shared/toast/ToastProvider.tsx'
import styles from './AdminLayout.module.css'
import { AppHeader } from './AppHeader.tsx'
import { MainNav } from './MainNav.tsx'

export function AdminLayout() {
  const location = useLocation()
  return (
    <div className={styles.adminLayout}>
      <AppHeader />
      <MainNav />
      <main className={styles.page}>
        <ToastProvider>
          <ErrorBoundary
            fallback={
              <p role="alert" className={styles.pageError}>
                {FALLBACK_PAGE_MESSAGE}
              </p>
            }
            resetKeys={[location.pathname]}
            onError={(error) => console.error(error)}
          >
            <Outlet />
          </ErrorBoundary>
        </ToastProvider>
      </main>
    </div>
  )
}
