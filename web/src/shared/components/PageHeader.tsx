import type { ReactNode } from 'react'
import { Link } from 'react-router'
import styles from './PageHeader.module.css'

type PageHeaderProps = {
  section: string
  title: string
  description?: ReactNode
  parent?: { label: string; to: string }
  actions?: ReactNode
}

export function PageHeader({ section, title, description, parent, actions }: PageHeaderProps) {
  return (
    <>
      <title>{`${section} · Raşit Bankası Dijital Asistan Yönetimi`}</title>
      <div className={styles.crumbs}>
        {parent ? (
          <Link to={parent.to} className={styles.parentLink}>
            {parent.label}
          </Link>
        ) : (
          <span>Dijital Asistan</span>
        )}
        <span>›</span>
        <b>{section}</b>
      </div>
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.title}>{title}</h1>
          {description === undefined ? null : <p className={styles.description}>{description}</p>}
        </div>
        {actions === undefined ? null : <div className={styles.actions}>{actions}</div>}
      </div>
    </>
  )
}
