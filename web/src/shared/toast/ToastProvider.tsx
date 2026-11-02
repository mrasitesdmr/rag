import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ToastContext } from './toastContext.ts'
import type { ToastControls } from './toastContext.ts'
import styles from './ToastProvider.module.css'
import { TOAST_DURATION_MS } from './toastRules.ts'

type Toast = { id: number; message: string }

type ToastProviderProps = { children: ReactNode }

export function ToastProvider({ children }: ToastProviderProps) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextIdRef = useRef(0)
  const timersRef = useRef(new Map<number, number>())

  useEffect(() => {
    const timers = timersRef.current
    return () => {
      timers.forEach((timer) => window.clearTimeout(timer))
      timers.clear()
    }
  }, [])

  const showToast = useCallback((message: string) => {
    nextIdRef.current += 1
    const id = nextIdRef.current
    setToasts((current) => [...current, { id, message }])
    const timer = window.setTimeout(() => {
      timersRef.current.delete(id)
      setToasts((current) => current.filter((toast) => toast.id !== id))
    }, TOAST_DURATION_MS)
    timersRef.current.set(id, timer)
  }, [])

  const controls = useMemo<ToastControls>(() => ({ showToast }), [showToast])

  return (
    <ToastContext value={controls}>
      {children}
      <div className={styles.toasts} aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={styles.toast}>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext>
  )
}
