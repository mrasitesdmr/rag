import { describe, expect, it } from 'vitest'
import { formatDateTime, formatListTime, formatTime } from './formatDateTime.ts'
import { formatDuration } from './formatDuration.ts'
import { formatDecimal, formatInteger } from './formatNumber.ts'
import { collapseWhitespace, shortenText } from './formatText.ts'

describe('formatDuration', () => {
  it.each([
    [0, '0 ms'],
    [850, '850 ms'],
    [999, '999 ms'],
    [1000, '1,00 sn'],
    [8440, '8,44 sn'],
    [9994, '9,99 sn'],
    [10000, '10,0 sn'],
    [12345, '12,3 sn'],
  ])('givenMilliseconds_whenFormatDuration_thenLabFormat %i', (milliseconds, expected) => {
    expect(formatDuration(milliseconds)).toBe(expected)
  })
})

describe('formatDateTime', () => {
  it.each([
    ['2026-10-29T13:15:44Z', '29.10.2026 16:15'],
    ['2026-10-31T21:05:00Z', '01.11.2026 00:05'],
    ['2026-12-31T20:59:59Z', '31.12.2026 23:59'],
  ])('givenIso_whenFormatDateTime_thenIstanbulTime %s', (iso, expected) => {
    expect(formatDateTime(iso)).toBe(expected)
  })
})

describe('formatInteger', () => {
  it.each([
    [0, '0'],
    [999, '999'],
    [412873, '412.873'],
    [1120236, '1.120.236'],
    [282.6, '283'],
  ])('givenNumber_whenFormatInteger_thenTurkishGrouping %d', (value, expected) => {
    expect(formatInteger(value)).toBe(expected)
  })
})

describe('shortenText', () => {
  it('givenTextOfExactlyMaxLength_whenShortenText_thenUnchanged', () => {
    expect(shortenText('a'.repeat(88), 88)).toBe('a'.repeat(88))
  })

  it('givenLongText_whenShortenText_thenFirstMaxMinusOneCharsAndEllipsis', () => {
    expect(shortenText('a'.repeat(90), 88)).toBe(`${'a'.repeat(87)}…`)
  })
})

describe('collapseWhitespace', () => {
  it('givenLineBreaksAndRuns_whenCollapseWhitespace_thenSingleSpacesTrimmed', () => {
    expect(collapseWhitespace('  Asgari\n\n ödeme \t tutarı ')).toBe('Asgari ödeme tutarı')
  })
})

describe('formatDecimal', () => {
  it.each([
    [0.3124, 3, '0,312'],
    [5.359, 2, '5,36'],
    [-11.04, 2, '-11,04'],
    [2, 2, '2,00'],
    [1234.5, 1, '1234,5'],
  ])('givenValue_whenFormatDecimal_thenFixedDigitsWithCommaAndAsciiMinus %f', (value, digits, expected) => {
    expect(formatDecimal(value, digits)).toBe(expected)
  })
})

describe('formatTime', () => {
  it.each([
    ['2026-11-01T10:05:00Z', '13:05'],
    ['2026-10-31T21:05:00Z', '00:05'],
    ['2026-12-31T20:59:59Z', '23:59'],
  ])('givenIso_whenFormatTime_thenIstanbulHourMinute %s', (iso, expected) => {
    expect(formatTime(iso)).toBe(expected)
    expect(formatTime(new Date(iso))).toBe(expected)
  })
})

describe('formatListTime', () => {
  const now = new Date('2026-11-02T10:31:05Z')

  it.each([
    ['2026-11-02T10:31:05Z', '13:31'],
    ['2026-11-01T10:31:05Z', '01.11.2026 13:31'],
    ['2026-11-02T21:30:00Z', '03.11.2026 00:30'],
    ['2026-11-01T20:59:00Z', '01.11.2026 23:59'],
  ])('givenInstant_whenListTimeFormatted_thenTimeOnlyForSameIstanbulDay %s', (iso, expected) => {
    expect(formatListTime(iso, now)).toBe(expected)
  })
})
