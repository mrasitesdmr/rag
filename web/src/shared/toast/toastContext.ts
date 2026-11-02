import { createContext } from 'react'

export type ToastControls = { showToast: (message: string) => void }

export const ToastContext = createContext<ToastControls | null>(null)
