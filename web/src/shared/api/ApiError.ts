export type Violation = { field: string; message: string }

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string
  readonly violations: Violation[]

  constructor(status: number, errorCode: string, violations: Violation[] = []) {
    super(errorCode)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
    this.violations = violations
  }
}

export function isClientError(error: unknown): boolean {
  return error instanceof ApiError && error.status >= 400 && error.status < 500
}

export function isApiErrorWithCode(error: unknown, errorCode: string): boolean {
  return error instanceof ApiError && error.errorCode === errorCode
}

export function errorCodeOf(error: unknown): string {
  return error instanceof ApiError ? error.errorCode : 'UNEXPECTED_ERROR'
}
