import { QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { createQueryClient } from './queryClient.ts'

type AppProvidersProps = { children: ReactNode }

export function AppProviders({ children }: AppProvidersProps) {
  const [queryClient] = useState(createQueryClient)
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}
