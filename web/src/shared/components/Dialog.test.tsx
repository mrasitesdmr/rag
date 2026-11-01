import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { Dialog } from './Dialog.tsx'

function Harness({ canClose = true }: { canClose?: boolean }) {
  const [isOpen, setOpen] = useState(false)
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Aç
      </button>
      {isOpen ? (
        <Dialog title="Deneme penceresi" onClose={() => setOpen(false)} canClose={canClose}>
          <button type="button">İlk</button>
          <button type="button">Son</button>
        </Dialog>
      ) : null}
    </>
  )
}

describe('Dialog', () => {
  it('givenOpenDialog_whenRendered_thenModalDialogNamedByItsTitleAndCloseButtonFocused', async () => {
    const actor = userEvent.setup()
    render(<Harness />)

    await actor.click(screen.getByRole('button', { name: 'Aç' }))

    const dialog = screen.getByRole('dialog', { name: 'Deneme penceresi' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByRole('heading', { level: 3, name: 'Deneme penceresi' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Kapat' })).toHaveFocus()
  })

  it('givenFocusOnLastElement_whenTab_thenWrapsToFirstAndShiftTabWrapsBack', async () => {
    const actor = userEvent.setup()
    render(<Harness />)
    await actor.click(screen.getByRole('button', { name: 'Aç' }))
    screen.getByRole('button', { name: 'Son' }).focus()

    await actor.tab()
    expect(screen.getByRole('button', { name: 'Kapat' })).toHaveFocus()

    await actor.tab({ shift: true })
    expect(screen.getByRole('button', { name: 'Son' })).toHaveFocus()
  })

  it('givenBackdropClick_whenCanClose_thenDialogClosesAndFocusReturnsToOpener', async () => {
    const actor = userEvent.setup()
    render(<Harness />)
    const opener = screen.getByRole('button', { name: 'Aç' })
    await actor.click(opener)

    await actor.click(screen.getByRole('dialog').parentElement as HTMLElement)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  it('givenClickInsideDialog_whenClicked_thenStaysOpen', async () => {
    const actor = userEvent.setup()
    render(<Harness />)
    await actor.click(screen.getByRole('button', { name: 'Aç' }))

    await actor.click(screen.getByRole('heading', { level: 3 }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('givenCannotClose_whenEscBackdropAndCloseButton_thenDialogStaysOpen', async () => {
    const actor = userEvent.setup()
    render(<Harness canClose={false} />)
    await actor.click(screen.getByRole('button', { name: 'Aç' }))

    await actor.keyboard('{Escape}')
    await actor.click(screen.getByRole('dialog').parentElement as HTMLElement)
    await actor.click(screen.getByRole('button', { name: 'Kapat' }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Kapat' })).toBeDisabled()
  })
})
