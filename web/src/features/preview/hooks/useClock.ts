import { useEffect, useState } from 'react'
import { formatTime } from '../../../shared/format/formatDateTime.ts'

const CLOCK_REFRESH_MS = 30_000

export function useClock(): string {
  const [time, setTime] = useState(() => formatTime(new Date()))
  useEffect(() => {
    const intervalId = window.setInterval(() => setTime(formatTime(new Date())), CLOCK_REFRESH_MS)
    return () => window.clearInterval(intervalId)
  }, [])
  return time
}
