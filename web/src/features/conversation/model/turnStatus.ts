import type { StatusTone } from '../../../shared/components/StatusBadge.tsx'
import type { TurnStatus } from './types.ts'

export const TURN_STATUS_LABELS: Record<TurnStatus, string> = {
  IN_PROGRESS: 'işleniyor',
  INCOMPLETE: 'TAMAMLANMADI',
  UNGROUNDED: 'DAYANAKSIZ',
  DONT_KNOW: 'BİLMİYOR',
  WRONG: 'HATALI',
  GROUNDED: 'DAYANAKLI',
}

const TURN_STATUS_TONES: Record<TurnStatus, StatusTone> = {
  IN_PROGRESS: 'run',
  INCOMPLETE: 'neutral',
  UNGROUNDED: 'bad',
  DONT_KNOW: 'ok',
  WRONG: 'bad',
  GROUNDED: 'ok',
}

export function turnStatusTone(status: TurnStatus): StatusTone {
  return TURN_STATUS_TONES[status]
}
