import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from './Pagination.tsx'

describe('Pagination', () => {
  it.each([
    { page: 0, totalPages: 13, text: '1 / 13', previous: false, next: true },
    { page: 5, totalPages: 13, text: '6 / 13', previous: true, next: true },
    { page: 12, totalPages: 13, text: '13 / 13', previous: true, next: false },
    { page: 0, totalPages: 0, text: '1 / 1', previous: false, next: false },
    { page: 0, totalPages: 1, text: '1 / 1', previous: false, next: false },
  ])('givenPage$page/$totalPages_whenRendered_thenTextAndButtonStates', ({ page, totalPages, text, previous, next }) => {
    render(<Pagination label="Parça sayfaları" page={page} totalPages={totalPages} onPageChange={() => undefined} />)

    expect(screen.getByRole('navigation', { name: 'Parça sayfaları' })).toHaveTextContent(text)
    expect(screen.getByRole('button', { name: 'Önceki' }).hasAttribute('disabled')).toBe(!previous)
    expect(screen.getByRole('button', { name: 'Sonraki' }).hasAttribute('disabled')).toBe(!next)
  })

  it('givenMiddlePage_whenButtonsClicked_thenReportsAdjacentPages', async () => {
    const onPageChange = vi.fn()
    const actor = userEvent.setup()
    render(<Pagination label="x" page={3} totalPages={10} onPageChange={onPageChange} />)

    await actor.click(screen.getByRole('button', { name: 'Önceki' }))
    await actor.click(screen.getByRole('button', { name: 'Sonraki' }))

    expect(onPageChange.mock.calls).toEqual([[2], [4]])
  })
})
