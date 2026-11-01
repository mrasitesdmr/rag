import { useId, useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import button from '../../../shared/components/button.module.css'
import { Dialog } from '../../../shared/components/Dialog.tsx'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import visuallyHidden from '../../../shared/components/visuallyHidden.module.css'
import { formatInteger } from '../../../shared/format/formatNumber.ts'
import { useCreateSourceMutation } from '../api/sourceQueries.ts'
import { normalizeUploadInput, toUploadErrors, validateSourceUpload } from '../model/sourceUploadRules.ts'
import type { UploadField, UploadFieldErrors } from '../model/sourceUploadRules.ts'
import styles from './AddSourceDialog.module.css'
import { PdfIcon } from './PdfIcon.tsx'

type AddSourceDialogProps = { onClose: () => void }

export function AddSourceDialog({ onClose }: AddSourceDialogProps) {
  const createSource = useCreateSourceMutation()
  const fileButtonRef = useRef<HTMLButtonElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const nameInputRef = useRef<HTMLInputElement>(null)
  const versionInputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [name, setName] = useState('')
  const [version, setVersion] = useState('')
  const [fieldErrors, setFieldErrors] = useState<UploadFieldErrors>({})
  const [formErrorCode, setFormErrorCode] = useState<string | null>(null)
  const baseId = useId()
  const ids = {
    fileInfo: `${baseId}-file-info`,
    fileError: `${baseId}-file-error`,
    name: `${baseId}-name`,
    nameHint: `${baseId}-name-hint`,
    nameError: `${baseId}-name-error`,
    version: `${baseId}-version`,
    versionError: `${baseId}-version-error`,
  }
  const isPending = createSource.isPending

  function clearFieldError(field: UploadField) {
    setFieldErrors((errors) => ({ ...errors, [field]: undefined }))
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.item(0) ?? null
    if (selected !== null) {
      setFile(selected)
      clearFieldError('file')
    }
    event.target.value = ''
  }

  function focusFirstInvalid(errors: UploadFieldErrors) {
    if (errors.file !== undefined) {
      fileButtonRef.current?.focus()
    } else if (errors.name !== undefined) {
      nameInputRef.current?.focus()
    } else if (errors.version !== undefined) {
      versionInputRef.current?.focus()
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isPending) {
      return
    }
    const input = normalizeUploadInput({ file, name, version })
    const errors = validateSourceUpload(input)
    setFormErrorCode(null)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      focusFirstInvalid(errors)
      return
    }
    createSource.mutate(input, {
      onSuccess: onClose,
      onError: (error) => {
        const uploadErrors = toUploadErrors(error)
        setFieldErrors(uploadErrors.fieldErrors)
        setFormErrorCode(uploadErrors.formErrorCode)
        focusFirstInvalid(uploadErrors.fieldErrors)
      },
    })
  }

  const fileDescribedBy = fieldErrors.file === undefined ? ids.fileInfo : `${ids.fileInfo} ${ids.fileError}`
  const nameDescribedBy = fieldErrors.name === undefined ? ids.nameHint : `${ids.nameHint} ${ids.nameError}`

  return (
    <Dialog title="Belge ekle" onClose={onClose} canClose={!isPending} initialFocusRef={fileButtonRef}>
      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <p className={styles.intro}>
          Asistanın cevap verirken arayacağı PDF'i seç. Belgenin yer imleri (başlıkları) olmalı; parçalar bu başlıklarla
          adlandırılır. En fazla 20 MB.
        </p>
        {formErrorCode === null ? null : <ErrorAlert errorCode={formErrorCode} />}
        <div className={styles.fileBox}>
          <PdfIcon className={styles.fileIcon} />
          <div id={ids.fileInfo} className={styles.fileInfo}>
            {file === null ? (
              <>
                <b>Dosya seçilmedi</b>
                <small>PDF · en fazla 20 MB</small>
              </>
            ) : (
              <>
                <b>{file.name}</b>
                <small>{`${formatInteger(file.size)} bayt`}</small>
              </>
            )}
          </div>
          <button
            ref={fileButtonRef}
            type="button"
            className={`${button.button} ${button.outline}`}
            aria-describedby={fileDescribedBy}
            disabled={isPending}
            onClick={() => fileInputRef.current?.click()}
          >
            Dosya seç
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/pdf,.pdf"
            aria-label="PDF dosyası"
            aria-invalid={fieldErrors.file !== undefined}
            aria-describedby={fileDescribedBy}
            className={visuallyHidden.visuallyHidden}
            tabIndex={-1}
            disabled={isPending}
            onChange={handleFileChange}
          />
        </div>
        {fieldErrors.file === undefined ? null : (
          <p id={ids.fileError} className={styles.fieldError}>
            {fieldErrors.file}
          </p>
        )}
        <div className={styles.field}>
          <label htmlFor={ids.name}>Kaynak adı</label>
          <input
            ref={nameInputRef}
            id={ids.name}
            value={name}
            autoComplete="off"
            aria-invalid={fieldErrors.name !== undefined}
            aria-describedby={nameDescribedBy}
            disabled={isPending}
            onChange={(event) => {
              setName(event.target.value)
              clearFieldError('name')
            }}
          />
          <span id={ids.nameHint} className={styles.hint}>
            Boş bırakırsan dosya adı kullanılır.
          </span>
          {fieldErrors.name === undefined ? null : (
            <p id={ids.nameError} className={styles.fieldError}>
              {fieldErrors.name}
            </p>
          )}
        </div>
        <div className={styles.field}>
          <label htmlFor={ids.version}>Sürüm etiketi</label>
          <input
            ref={versionInputRef}
            id={ids.version}
            value={version}
            placeholder="ör. 2026.10"
            autoComplete="off"
            required
            aria-invalid={fieldErrors.version !== undefined}
            aria-describedby={fieldErrors.version === undefined ? undefined : ids.versionError}
            disabled={isPending}
            onChange={(event) => {
              setVersion(event.target.value)
              clearFieldError('version')
            }}
          />
          {fieldErrors.version === undefined ? null : (
            <p id={ids.versionError} className={styles.fieldError}>
              {fieldErrors.version}
            </p>
          )}
        </div>
        <div className={styles.actions}>
          <button type="button" className={`${button.button} ${button.outline}`} disabled={isPending} onClick={onClose}>
            Vazgeç
          </button>
          <button type="submit" className={button.button} disabled={isPending}>
            {isPending ? 'Yükleniyor…' : 'Yükle ve işle'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
