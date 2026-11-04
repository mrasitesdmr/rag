import { describe, expect, it } from 'vitest'
import { TURN_STATUS_LABELS, turnStatusTone } from './turnStatus.ts'
import { TURN_STATUSES } from './types.ts'

describe('turnStatus', () => {
  it.each([
    ['IN_PROGRESS', 'işleniyor'],
    ['INCOMPLETE', 'TAMAMLANMADI'],
    ['UNGROUNDED', 'DAYANAKSIZ'],
    ['DONT_KNOW', 'BİLMİYOR'],
    ['WRONG', 'HATALI'],
    ['GROUNDED', 'DAYANAKLI'],
    ['SMALL_TALK', 'SOHBET'],
  ] as const)('givenStatus_whenLabelled_thenLabText %s', (status, label) => {
    // Act
    const text = TURN_STATUS_LABELS[status]

    // Assert
    expect(text).toBe(label)
  })

  it.each([
    ['GROUNDED', 'ok'],
    ['DONT_KNOW', 'ok'],
    ['UNGROUNDED', 'bad'],
    ['WRONG', 'bad'],
    ['IN_PROGRESS', 'run'],
    ['INCOMPLETE', 'neutral'],
    ['SMALL_TALK', 'neutral'],
  ] as const)('givenStatus_whenToneRequested_thenLabTone %s', (status, tone) => {
    expect(turnStatusTone(status)).toBe(tone)
  })

  it('givenAllStatuses_whenLabelled_thenEveryStatusHasDistinctLabel', () => {
    const labels = TURN_STATUSES.map((status) => TURN_STATUS_LABELS[status])

    expect(new Set(labels).size).toBe(TURN_STATUSES.length)
  })
})
