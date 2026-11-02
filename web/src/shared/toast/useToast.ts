import { use } from 'react'
import { ToastContext } from './toastContext.ts'
import type { ToastControls } from './toastContext.ts'

export function useToast(): ToastControls {
  const controls = use(ToastContext)
  if (controls === null) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return controls
}
