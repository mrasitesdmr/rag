import { render, screen, within } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { liveConfig } from '../../../test/fixtures.ts'
import { LiveConfigStrip } from './LiveConfigStrip.tsx'

describe('LiveConfigStrip', () => {
  it('givenConfig_whenRendered_thenGroupNamedLiveConfigWithFiveItemsAndTones', () => {
    render(<LiveConfigStrip config={liveConfig({ generationMode: 'LLM_ONLY', queryRewriteEnabled: false })} />)

    const group = screen.getByRole('group', { name: 'Canlı yapılandırma' })
    const tones = Array.from(group.querySelectorAll('span')).map((span) => [span.textContent, span.dataset.tone])
    expect(tones).toEqual([
      ['Model qwen3.5:9b', 'normal'],
      ['Embedding bge-m3', 'normal'],
      ['Retrieval kapalı (Sadece LLM)', 'danger'],
      ['Soru yeniden yazma kapalı', 'warning'],
      ['Bilgi kaynakları Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10', 'normal'],
    ])
    expect(within(group).getByText('kapalı (Sadece LLM)', { selector: 'b' })).toBeInTheDocument()
  })

  it('givenStrip_whenRendered_thenNoAxeViolations', async () => {
    const { container } = render(<LiveConfigStrip config={liveConfig()} />)

    expect((await axe(container)).violations).toEqual([])
  })
})
