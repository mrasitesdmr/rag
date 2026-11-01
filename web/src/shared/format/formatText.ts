export function collapseWhitespace(text: string): string {
  return text.replace(/\s+/g, ' ').trim()
}

export function shortenText(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.slice(0, maxLength - 1)}…` : text
}
