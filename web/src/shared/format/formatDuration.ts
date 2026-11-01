const MILLISECONDS_PER_SECOND = 1000
const ONE_DECIMAL_FROM_MS = 10_000

export function formatDuration(milliseconds: number): string {
  if (milliseconds < MILLISECONDS_PER_SECOND) {
    return `${Math.round(milliseconds)} ms`
  }
  const digits = milliseconds >= ONE_DECIMAL_FROM_MS ? 1 : 2
  return `${(milliseconds / MILLISECONDS_PER_SECOND).toFixed(digits).replace('.', ',')} sn`
}
