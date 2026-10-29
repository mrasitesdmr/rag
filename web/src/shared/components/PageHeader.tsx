import styles from './PageHeader.module.css'

type PageHeaderProps = { section: string; title: string; description: string }

export function PageHeader({ section, title, description }: PageHeaderProps) {
  return (
    <>
      <title>{`${section} · Raşit Bankası Dijital Asistan Yönetimi`}</title>
      <div className={styles.crumbs}>
        <span>Dijital Asistan</span>
        <span>›</span>
        <b>{section}</b>
      </div>
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.description}>{description}</p>
        </div>
      </div>
    </>
  )
}
