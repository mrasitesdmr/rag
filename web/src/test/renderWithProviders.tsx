import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { App } from '../app/App.tsx'

export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } },
  })
}

export function renderWithProviders(ui: ReactElement, path = '/') {
  const queryClient = createTestQueryClient()
  const result = render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  )
  return { ...result, queryClient }
}

export function renderAppAt(path: string) {
  return renderWithProviders(<App />, path)
}
