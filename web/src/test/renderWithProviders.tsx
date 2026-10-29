import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { App } from '../app/App.tsx'

export function renderAppAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}
