import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { pageOf, workspace } from './fixtures.ts'

type TestViolation = { field: string; message: string }

export function problem(status: number, errorCode: string, violations?: TestViolation[]) {
  return HttpResponse.json(
    { type: 'about:blank', title: errorCode, status, errorCode, ...(violations ? { violations } : {}) },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

export const defaultHandlers = [
  http.get('/api/workspaces/42', () => HttpResponse.json(workspace())),
  http.get('/api/workspaces/42/sources', () => HttpResponse.json(pageOf([]))),
]

export const server = setupServer(...defaultHandlers)
