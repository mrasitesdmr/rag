import type { StatusTone } from '../../../shared/components/StatusBadge.tsx'
import type { TurnStatus } from './types.ts'

export const TURN_STATUS_LABELS: Record<TurnStatus, string> = {
  IN_PROGRESS: 'işleniyor',
  INCOMPLETE: 'TAMAMLANMADI',
  UNGROUNDED: 'DAYANAKSIZ',
  SMALL_TALK: 'SOHBET',
  DONT_KNOW: 'BİLMİYOR',
  MISSED: 'KAÇIRDI',
  GENERATION_ERROR: 'ÜRETİM HATASI',
  WRONG: 'HATALI',
  GROUNDED: 'DAYANAKLI',
}

const TURN_STATUS_TONES: Record<TurnStatus, StatusTone> = {
  IN_PROGRESS: 'run',
  INCOMPLETE: 'neutral',
  UNGROUNDED: 'bad',
  SMALL_TALK: 'neutral',
  DONT_KNOW: 'ok',
  MISSED: 'bad',
  GENERATION_ERROR: 'bad',
  WRONG: 'bad',
  GROUNDED: 'ok',
}

export function turnStatusTone(status: TurnStatus): StatusTone {
  return TURN_STATUS_TONES[status]
}
