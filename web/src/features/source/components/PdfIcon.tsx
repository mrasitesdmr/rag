type PdfIconProps = { className?: string }

export function PdfIcon({ className }: PdfIconProps) {
  return (
    <svg className={className} width="28" height="34" viewBox="0 0 28 34" aria-hidden="true">
      <path
        d="M3 1h15l7 7v24a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1z"
        fill="var(--color-card)"
        stroke="var(--color-file-icon-line)"
      />
      <rect x="5" y="20" width="18" height="8" rx="1.5" fill="var(--color-brand)" />
      <text
        x="14"
        y="26.2"
        textAnchor="middle"
        fontSize="6"
        fontWeight="700"
        fill="var(--color-on-brand)"
        fontFamily="sans-serif"
      >
        PDF
      </text>
    </svg>
  )
}
