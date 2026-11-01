export const MESSAGE_MAX_LENGTH = 1000

export const MESSAGE_RULE_MESSAGE = `Soru 1-${MESSAGE_MAX_LENGTH} karakter olmalı.`

export function normalizeMessage(input: string): string {
  return input.trim()
}

export function canSendMessage(input: string): boolean {
  const normalized = normalizeMessage(input)
  return normalized.length > 0 && normalized.length <= MESSAGE_MAX_LENGTH
}
