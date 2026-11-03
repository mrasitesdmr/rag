import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Callout } from './Callout.tsx'

describe('Callout', () => {
  it('givenDefaultTone_whenRendered_thenInfoIconAndText', () => {
    const { container } = render(<Callout>Golden set yüklenmemiş.</Callout>)

    expect(screen.getByText('Golden set yüklenmemiş.')).toBeInTheDocument()
    expect(container.querySelector('svg')).not.toBeNull()
  })

  it('givenNoIcon_whenRendered_thenTextWithoutIcon', () => {
    const { container } = render(<Callout hasIcon={false}>Henüz ölçüm yok.</Callout>)

    expect(screen.getByText('Henüz ölçüm yok.')).toBeInTheDocument()
    expect(container.querySelector('svg')).toBeNull()
  })

  it('givenWarningTone_whenRendered_thenTextWithoutIconEvenIfIconRequested', () => {
    const { container } = render(<Callout tone="warning">Dikkat</Callout>)

    expect(screen.getByText('Dikkat')).toBeInTheDocument()
    expect(container.querySelector('svg')).toBeNull()
    expect(container.firstElementChild?.className).not.toBe(render(<Callout>x</Callout>).container.firstElementChild?.className)
  })
})
