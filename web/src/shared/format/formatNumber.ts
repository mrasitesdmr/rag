const INTEGER_FORMAT = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 0 })

export function formatInteger(value: number): string {
  return INTEGER_FORMAT.format(Math.round(value))
}

const DECIMAL_FORMATS = new Map<number, Intl.NumberFormat>()

export function formatDecimal(value: number, fractionDigits: number): string {
  let format = DECIMAL_FORMATS.get(fractionDigits)
  if (format === undefined) {
    format = new Intl.NumberFormat('tr-TR', {
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
      useGrouping: false,
    })
    DECIMAL_FORMATS.set(fractionDigits, format)
  }
  return format.format(value)
}
