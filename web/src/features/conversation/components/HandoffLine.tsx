import { Callout } from '../../../shared/components/Callout.tsx'
import { formatTime } from '../../../shared/format/formatDateTime.ts'
import type { HandoffReason, HandoffResponse } from '../model/types.ts'

const HANDOFF_REASON_LABELS: Record<HandoffReason, string> = {
  NO_ANSWER: 'cevap yok',
}

type HandoffLineProps = { handoff: HandoffResponse }

export function HandoffLine({ handoff }: HandoffLineProps) {
  return (
    <Callout>
      {`Temsilciye aktarıldı · ${handoff.queue} kuyruğu · ${formatTime(handoff.createdAt)} · sebep: ${HANDOFF_REASON_LABELS[handoff.reason]}`}
    </Callout>
  )
}
