import { useEffect, useEffectEvent, useId, useRef } from 'react'
import type { MouseEvent, ReactNode, RefObject } from 'react'
import { createPortal } from 'react-dom'
import styles from './Dialog.module.css'

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), input:not([disabled]):not([tabindex="-1"]), textarea:not([disabled]), select:not([disabled]), [tabindex="0"]'

type DialogProps = {
  title: string
  onClose: () => void
  canClose?: boolean
  initialFocusRef?: RefObject<HTMLElement | null>
  children: ReactNode
}

export function Dialog({ title, onClose, canClose = true, initialFocusRef, children }: DialogProps) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  const requestClose = useEffectEvent(() => {
    if (canClose) {
      onClose()
    }
  })

  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const focusTarget = initialFocusRef?.current ?? closeButtonRef.current
    focusTarget?.focus()
    return () => {
      if (opener?.isConnected) {
        opener.focus()
      }
    }
  }, [initialFocusRef])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault()
        requestClose()
        return
      }
      if (event.key === 'Tab') {
        trapFocus(event, dialogRef.current)
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  function handleBackdropClick(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget && canClose) {
      onClose()
    }
  }

  return createPortal(
    // Backdrop clicks close the dialog like the lab; the keyboard equivalent is Esc.
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <div className={styles.backdrop} onClick={handleBackdropClick}>
      <div ref={dialogRef} className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <div className={styles.header}>
          <h3 id={titleId} className={styles.title}>
            {title}
          </h3>
          <button
            ref={closeButtonRef}
            type="button"
            className={styles.closeButton}
            aria-label="Kapat"
            disabled={!canClose}
            onClick={onClose}
          >
            ×
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}

function trapFocus(event: KeyboardEvent, dialog: HTMLElement | null) {
  if (dialog === null) {
    return
  }
  const focusables = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR))
  const first = focusables.at(0)
  const last = focusables.at(-1)
  if (first === undefined || last === undefined) {
    event.preventDefault()
    return
  }
  const active = document.activeElement
  const isInside = active instanceof Node && dialog.contains(active)
  if (event.shiftKey && (active === first || !isInside)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && (active === last || !isInside)) {
    event.preventDefault()
    first.focus()
  }
}
