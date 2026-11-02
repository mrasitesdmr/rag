import { useQuery } from '@tanstack/react-query'
import { getSuggestions } from './previewApi.ts'

export const previewKeys = {
  suggestions: ['preview', 'suggestions'] as const,
}

export function useSuggestionsQuery() {
  return useQuery({
    queryKey: previewKeys.suggestions,
    queryFn: getSuggestions,
  })
}
