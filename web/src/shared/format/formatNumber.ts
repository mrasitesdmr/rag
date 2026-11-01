const INTEGER_FORMAT = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 0 })

export function formatInteger(value: number): string {
  return INTEGER_FORMAT.format(Math.round(value))
}
