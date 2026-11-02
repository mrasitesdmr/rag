import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ToastProvider } from './ToastProvider.tsx'
import { useToast } from './useToast.ts'

function Trigger({ message }: { message: string }) {
  const { showToast } = useToast()
  return (
    <button type="button" onClick={() => showToast(message)}>
      göster
    </button>
  )
}

describe('ToastProvider', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('givenShownToast_whenDurationPasses_thenRemoved', () => {
    const { container } = render(
      <ToastProvider>
        <Trigger message="Geri bildirim kaydedildi" />
      </ToastProvider>,
    )

    act(() => screen.getByRole('button', { name: 'göster' }).click())

    const region = container.querySelector('[aria-live="polite"]')
    expect(region).toHaveTextContent('Geri bildirim kaydedildi')
    act(() => vi.advanceTimersByTime(2599))
    expect(region).toHaveTextContent('Geri bildirim kaydedildi')
    act(() => vi.advanceTimersByTime(1))
    expect(region).not.toHaveTextContent('Geri bildirim kaydedildi')
  })

  it('givenTwoToasts_whenFirstExpires_thenSecondStaysUntilItsOwnDuration', () => {
    render(
      <ToastProvider>
        <Trigger message="Birinci" />
      </ToastProvider>,
    )
    act(() => screen.getByRole('button', { name: 'göster' }).click())
    act(() => vi.advanceTimersByTime(1500))
    act(() => screen.getByRole('button', { name: 'göster' }).click())

    expect(screen.getAllByText('Birinci')).toHaveLength(2)
    act(() => vi.advanceTimersByTime(1100))
    expect(screen.getAllByText('Birinci')).toHaveLength(1)
    act(() => vi.advanceTimersByTime(1500))
    expect(screen.queryByText('Birinci')).not.toBeInTheDocument()
  })

  it('givenNoProvider_whenUseToast_thenThrowsClearError', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)

    expect(() => render(<Trigger message="x" />)).toThrow('useToast must be used within ToastProvider')

    consoleError.mockRestore()
  })
})
