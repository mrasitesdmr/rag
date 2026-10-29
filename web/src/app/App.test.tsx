import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { renderAppAt } from '../test/renderWithProviders.tsx'

const MENU_LABELS = ['Önizleme', 'Konuşmalar', 'Bilgi kaynakları', 'Değerlendirme']

function selectedMenuLabels(): string[] {
  const menu = screen.getByRole('navigation', { name: 'Menü' })
  return within(menu)
    .getAllByRole('link')
    .filter((link) => link.getAttribute('aria-current') === 'page')
    .map((link) => link.textContent ?? '')
}

describe('App shell', () => {
  it('givenRootPath_whenRendered_thenRedirectsToPreviewScreen', () => {
    renderAppAt('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Asistan önizleme' })).toBeInTheDocument()
    expect(selectedMenuLabels()).toEqual(['Önizleme'])
  })

  it.each([
    {
      path: '/onizleme',
      section: 'Önizleme',
      title: 'Asistan önizleme',
      description:
        "Raşit Mobil'deki asistanı müşterinin gördüğü hâliyle dene; önizleme konuşmaları da Konuşmalar'a düşer.",
    },
    {
      path: '/konusmalar',
      section: 'Konuşmalar',
      title: 'Konuşmalar',
      description:
        "Her tur, modele giden parçalarla birlikte saklanır. Yanlış bir cevapta önce doğru parçanın prompt'a girip girmediğine bakılır.",
    },
    {
      path: '/kaynaklar',
      section: 'Bilgi kaynakları',
      title: 'Bilgi kaynakları',
      description:
        "Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça embedding modeliyle embed edilip pgvector'e yazılır.",
    },
    {
      path: '/degerlendirme',
      section: 'Değerlendirme',
      title: 'Değerlendirme',
      description:
        "Rehberde cevabı olan sorular; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar.",
    },
  ])(
    'givenScreenPath_whenRendered_thenShowsLabBreadcrumbTitleAndDescription $path',
    ({ path, section, title, description }) => {
      renderAppAt(path)

      expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument()
      expect(screen.getByText(description)).toBeInTheDocument()
      expect(screen.getByText('Dijital Asistan')).toBeInTheDocument()
      expect(screen.getByText('›')).toBeInTheDocument()
      expect(screen.getByText(section, { selector: 'b' })).toBeInTheDocument()
      expect(selectedMenuLabels()).toEqual([section])
    },
  )

  it('givenPreviewScreen_whenSourcesMenuItemClicked_thenShowsSourcesScreenAndMarksItSelected', async () => {
    renderAppAt('/onizleme')
    const menu = screen.getByRole('navigation', { name: 'Menü' })

    await userEvent.click(within(menu).getByRole('link', { name: 'Bilgi kaynakları' }))

    expect(screen.getByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeInTheDocument()
    expect(within(menu).getByRole('link', { name: 'Bilgi kaynakları' })).toHaveAttribute('aria-current', 'page')
    expect(within(menu).getByRole('link', { name: 'Önizleme' })).not.toHaveAttribute('aria-current')
  })

  it('givenAnyScreen_whenRendered_thenShowsLogoTextAndLiveConfigBadge', () => {
    renderAppAt('/degerlendirme')
    const header = screen.getByRole('banner')

    expect(within(header).getByText('Raşit Bankası')).toBeVisible()
    expect(within(header).getByText('Dijital Asistan Yönetimi')).toBeVisible()
    expect(within(header).getByText('Canlı yapılandırma')).toBeVisible()
    expect(within(header).getByRole('link', { name: 'Önizleme' })).toHaveAttribute('href', '/onizleme')
  })

  it('givenAnyScreen_whenRendered_thenShowsAllFourMenuItemsInLabOrder', () => {
    renderAppAt('/onizleme')
    const menu = screen.getByRole('navigation', { name: 'Menü' })

    const labels = within(menu)
      .getAllByRole('link')
      .map((link) => link.textContent)

    expect(labels).toEqual(MENU_LABELS)
  })

  it('givenAnyScreen_whenRendered_thenShowsNoConversationCounterAndNoUserArea', () => {
    renderAppAt('/onizleme')
    const header = screen.getByRole('banner')

    expect(screen.getByRole('link', { name: /Konuşmalar/ }).textContent).toBe('Konuşmalar')
    expect(screen.getByRole('link', { name: 'Konuşmalar' })).toBeInTheDocument()
    expect(within(header).queryByText('Raşit Eşdemir')).not.toBeInTheDocument()
    expect(within(header).queryByText('RE')).not.toBeInTheDocument()
    expect(within(header).queryByText('Ürün sahibi')).not.toBeInTheDocument()
  })

  it.each(['/yok', '/konusmalar/12'])(
    'givenUnknownPath_whenRendered_thenShowsNotFoundScreenWithNoMenuItemSelected %s',
    (path) => {
      renderAppAt(path)

      expect(screen.getByRole('heading', { level: 1, name: 'Sayfa bulunamadı' })).toBeInTheDocument()
      expect(selectedMenuLabels()).toEqual([])
    },
  )

  it('givenSourcesPath_whenRendered_thenSetsDocumentTitle', () => {
    renderAppAt('/kaynaklar')

    expect(document.title).toBe('Bilgi kaynakları · Raşit Bankası Dijital Asistan Yönetimi')
  })

  it.each(['/onizleme', '/kaynaklar'])('givenShell_whenRendered_thenHasNoAxeViolations %s', async (path) => {
    const { container } = renderAppAt(path)

    expect(await axe(container)).toHaveNoViolations()
  })
})
