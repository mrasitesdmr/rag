import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { chunkDetail } from '../../test/fixtures.ts'
import { renderWithProviders } from '../../test/renderWithProviders.tsx'
import { problem, server } from '../../test/server.ts'
import { ChunkDialog } from './ChunkDialog.tsx'

describe('ChunkDialog', () => {
  it('givenChunkId_whenRenderedAlone_thenLoadsChunk', async () => {
    server.use(http.get('/api/workspaces/42/chunks/151', () => HttpResponse.json(chunkDetail())))

    renderWithProviders(<ChunkDialog chunkId={151} onClose={() => undefined} />)

    const dialog = await screen.findByRole('dialog', { name: '2.16 Kayıp ya da çalıntı bildirimi' })
    expect(within(dialog).getByText(/^chunk_id/)).toHaveTextContent('chunk_id 151')
    expect(within(dialog).getByText(/^chunk_no/)).toHaveTextContent('chunk_no 31')
    expect(within(dialog).getByText(/^token/)).toHaveTextContent('token 297')
    expect(within(dialog).getByText(/^model/)).toHaveTextContent('model bge-m3@1024')
    expect(within(dialog).getByText(/^kaynak/)).toHaveTextContent(
      'kaynak Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10',
    )
    expect(within(dialog).getByRole('region', { name: 'Parça metni' })).toHaveTextContent(
      'Kartınızı kaybederseniz hemen 7/24 hattını arayın.',
    )
    expect(
      within(dialog).getByText(
        'embedding [0.01086, -0.00193, -0.06578, 0.01364, -0.01739, -0.03014, 0.01209, -0.04669, … 1016 değer daha]',
      ),
    ).toBeInTheDocument()
  })

  it('givenPreviewCoversWholeVector_whenRendered_thenNoMoreValuesSuffix', async () => {
    server.use(
      http.get('/api/workspaces/42/chunks/151', () =>
        HttpResponse.json(chunkDetail({ embeddingDimensions: 3, embeddingPreview: [0.5, 0.25, -1] })),
      ),
    )

    renderWithProviders(<ChunkDialog chunkId={151} onClose={() => undefined} />)

    expect(await screen.findByText('embedding [0.50000, 0.25000, -1.00000]')).toBeInTheDocument()
  })

  it('givenLoadingChunk_whenRendered_thenGenericTitleAndLoadingText', async () => {
    server.use(http.get('/api/workspaces/42/chunks/151', () => HttpResponse.json(chunkDetail())))

    renderWithProviders(<ChunkDialog chunkId={151} onClose={() => undefined} />)

    expect(screen.getByRole('dialog', { name: 'Parça' })).toBeInTheDocument()
    expect(screen.getByText('Yükleniyor…')).toBeInTheDocument()
    await screen.findByRole('dialog', { name: '2.16 Kayıp ya da çalıntı bildirimi' })
  })

  it.each([
    ['Esc key', async (actor: ReturnType<typeof userEvent.setup>) => actor.keyboard('{Escape}')],
    ['Kapat text button', async (actor: ReturnType<typeof userEvent.setup>) => actor.click(screen.getAllByRole('button', { name: 'Kapat' })[1] as HTMLElement)],
    ['× button', async (actor: ReturnType<typeof userEvent.setup>) => actor.click(screen.getAllByRole('button', { name: 'Kapat' })[0] as HTMLElement)],
  ])('givenOpenChunk_whenClosedWith_thenOnCloseCalledOnce %s', async (_label, close) => {
    server.use(http.get('/api/workspaces/42/chunks/151', () => HttpResponse.json(chunkDetail())))
    const onClose = vi.fn()
    const actor = userEvent.setup()
    renderWithProviders(<ChunkDialog chunkId={151} onClose={onClose} />)
    await screen.findByRole('dialog', { name: '2.16 Kayıp ya da çalıntı bildirimi' })

    await close(actor)

    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('givenUnknownChunk_whenRendered_thenChunkNotFoundMessage', async () => {
    server.use(http.get('/api/workspaces/42/chunks/9', () => problem(404, 'CHUNK_NOT_FOUND')))

    renderWithProviders(<ChunkDialog chunkId={9} onClose={() => undefined} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Parça bulunamadı.')
  })

  it('givenOtherChunkId_whenRendered_thenRequestsThatChunkOfTheWorkspace', async () => {
    let requested = ''
    server.use(
      http.get('/api/workspaces/42/chunks/:chunkId', ({ params }) => {
        requested = String(params.chunkId)
        return HttpResponse.json(chunkDetail({ chunkId: 500, heading: 'Başka parça' }))
      }),
    )

    renderWithProviders(<ChunkDialog chunkId={500} onClose={() => undefined} />)

    expect(await screen.findByRole('dialog', { name: 'Başka parça' })).toBeInTheDocument()
    expect(requested).toBe('500')
  })
})
