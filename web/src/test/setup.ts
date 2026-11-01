import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, expect } from 'vitest'
import * as axeMatchers from 'vitest-axe/matchers'
import { server } from './server.ts'

expect.extend(axeMatchers)

// jsdom's FormData and File are not understood by Node's fetch (the body is sent broken), so tests use Node's own.
// Node does not export them as typed modules here, so FormData comes from a parsed body and File from node:buffer.
const nodeBuffer: { File: typeof File } = await import(/* @vite-ignore */ String('node:buffer'))
globalThis.FormData = (
  await new Response('a=1', { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }).formData()
).constructor as typeof FormData
globalThis.File = nodeBuffer.File

// Node's fetch rejects relative URLs; the browser resolves them against the page origin.
const nativeFetch = globalThis.fetch.bind(globalThis)
globalThis.fetch = (input: RequestInfo | URL, init?: RequestInit) =>
  nativeFetch(typeof input === 'string' && input.startsWith('/') ? new URL(input, window.location.origin) : input, init)

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
})

afterAll(() => server.close())
