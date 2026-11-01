import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MessageComposer } from './MessageComposer.tsx'

describe('MessageComposer', () => {
  it('givenWhitespaceOnly_whenTyped_thenSendDisabled', async () => {
    const onSend = vi.fn()
    render(<MessageComposer isStreaming={false} onSend={onSend} />)

    await userEvent.type(screen.getByRole('textbox', { name: 'Soru' }), '   ')
    await userEvent.keyboard('{Enter}')

    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
    expect(onSend).not.toHaveBeenCalled()
  })

  it('givenPasted1005Chars_whenPasted_thenInputKeeps1000', async () => {
    render(<MessageComposer isStreaming={false} onSend={() => undefined} />)
    const input = screen.getByRole('textbox', { name: 'Soru' })

    await userEvent.click(input)
    await userEvent.paste('a'.repeat(1005))

    expect(input).toHaveValue('a'.repeat(1000))
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeEnabled()
  })

  it('givenTypedQuestion_whenEnterPressed_thenSendsAndClearsInput', async () => {
    const onSend = vi.fn()
    render(<MessageComposer isStreaming={false} onSend={onSend} />)
    const input = screen.getByRole('textbox', { name: 'Soru' })

    await userEvent.type(input, 'Kartımı kaybettim{Enter}')

    expect(onSend).toHaveBeenCalledExactlyOnceWith('Kartımı kaybettim')
    expect(input).toHaveValue('')
  })

  it('givenStreaming_whenRendered_thenInputAndSendDisabledEvenWithText', () => {
    render(<MessageComposer isStreaming onSend={() => undefined} />)

    expect(screen.getByRole('textbox', { name: 'Soru' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
  })
})
