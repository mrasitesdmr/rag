import styles from './Pagination.module.css'

type PaginationProps = {
  label: string
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ label, page, totalPages, onPageChange }: PaginationProps) {
  const pageCount = Math.max(1, totalPages)
  return (
    <nav className={styles.pagination} aria-label={label}>
      <span>{`${page + 1} / ${pageCount}`}</span>
      <button type="button" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        Önceki
      </button>
      <button type="button" disabled={page >= pageCount - 1} onClick={() => onPageChange(page + 1)}>
        Sonraki
      </button>
    </nav>
  )
}
