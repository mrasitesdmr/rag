import { messageFor } from '../api/errorMessages.ts'
import styles from './ErrorAlert.module.css'

type ErrorAlertProps = { errorCode: string }

export function ErrorAlert({ errorCode }: ErrorAlertProps) {
  return (
    <p role="alert" className={styles.errorAlert}>
      {messageFor(errorCode)}
    </p>
  )
}
