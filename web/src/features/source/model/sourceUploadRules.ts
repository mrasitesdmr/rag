import { ApiError } from '../../../shared/api/ApiError.ts'
import { messageFor } from '../../../shared/api/errorMessages.ts'
import type { CreateSourceInput } from './types.ts'

export const SOURCE_NAME_MIN_LENGTH = 2
export const SOURCE_NAME_MAX_LENGTH = 80
export const VERSION_MAX_LENGTH = 40
export const FILE_MAX_BYTES = 20 * 1024 * 1024
export const FILE_NAME_MAX_LENGTH = 255

export const FILE_REQUIRED_MESSAGE = 'Bir PDF dosyası seç.'
export const FILE_EMPTY_MESSAGE = 'Seçtiğin dosya boş. Başka bir PDF seç.'
export const FILE_NAME_TOO_LONG_MESSAGE = `Dosya adı en fazla ${FILE_NAME_MAX_LENGTH} karakter olabilir.`
export const FILE_RULE_MESSAGE = `Seçtiğin dosya boş ya da adı ${FILE_NAME_MAX_LENGTH} karakterden uzun. Başka bir PDF seç.`
export const SOURCE_NAME_RULE_MESSAGE = `Kaynak adı ${SOURCE_NAME_MIN_LENGTH}-${SOURCE_NAME_MAX_LENGTH} karakter olmalı.`
export const VERSION_RULE_MESSAGE = `Sürüm etiketi zorunlu; en fazla ${VERSION_MAX_LENGTH} karakter.`

const PDF_EXTENSION = '.pdf'

export type UploadField = 'file' | 'name' | 'version'

export type UploadFieldErrors = Partial<Record<UploadField, string>>

export type UploadErrors = { fieldErrors: UploadFieldErrors; formErrorCode: string | null }

const FILE_ERROR_CODES = new Set(['FILE_TOO_LARGE', 'UNSUPPORTED_FILE_TYPE', 'PDF_UNREADABLE', 'PDF_NO_TEXT', 'PDF_NO_BOOKMARKS'])

const VIOLATION_MESSAGES: Record<UploadField, string> = {
  file: FILE_RULE_MESSAGE,
  name: SOURCE_NAME_RULE_MESSAGE,
  version: VERSION_RULE_MESSAGE,
}

export function normalizeUploadInput(input: CreateSourceInput): CreateSourceInput {
  return { file: input.file, name: input.name.trim(), version: input.version.trim() }
}

export function validateSourceUpload(input: CreateSourceInput): UploadFieldErrors {
  const { file, name, version } = normalizeUploadInput(input)
  const errors: UploadFieldErrors = {}
  const fileError = validateFile(file)
  if (fileError !== null) {
    errors.file = fileError
  }
  const nameLength = [...name].length
  if (nameLength > 0 && (nameLength < SOURCE_NAME_MIN_LENGTH || nameLength > SOURCE_NAME_MAX_LENGTH)) {
    errors.name = SOURCE_NAME_RULE_MESSAGE
  }
  const versionLength = [...version].length
  if (versionLength === 0 || versionLength > VERSION_MAX_LENGTH) {
    errors.version = VERSION_RULE_MESSAGE
  }
  return errors
}

export function toUploadErrors(error: unknown): UploadErrors {
  if (!(error instanceof ApiError)) {
    return { fieldErrors: {}, formErrorCode: 'UNEXPECTED_ERROR' }
  }
  if (FILE_ERROR_CODES.has(error.errorCode)) {
    return { fieldErrors: { file: messageFor(error.errorCode) }, formErrorCode: null }
  }
  if (error.errorCode === 'SOURCE_NAME_TAKEN') {
    return { fieldErrors: { name: messageFor(error.errorCode) }, formErrorCode: null }
  }
  if (error.errorCode === 'VALIDATION_FAILED') {
    const fieldErrors: UploadFieldErrors = {}
    for (const violation of error.violations) {
      if (isUploadField(violation.field)) {
        fieldErrors[violation.field] = VIOLATION_MESSAGES[violation.field]
      }
    }
    if (Object.keys(fieldErrors).length > 0) {
      return { fieldErrors, formErrorCode: null }
    }
  }
  return { fieldErrors: {}, formErrorCode: error.errorCode }
}

function validateFile(file: File | null): string | null {
  if (file === null) {
    return FILE_REQUIRED_MESSAGE
  }
  if (file.size === 0) {
    return FILE_EMPTY_MESSAGE
  }
  if (!file.name.toLowerCase().endsWith(PDF_EXTENSION)) {
    return messageFor('UNSUPPORTED_FILE_TYPE')
  }
  if (file.size > FILE_MAX_BYTES) {
    return messageFor('FILE_TOO_LARGE')
  }
  if ([...file.name].length > FILE_NAME_MAX_LENGTH) {
    return FILE_NAME_TOO_LONG_MESSAGE
  }
  return null
}

function isUploadField(field: string): field is UploadField {
  return field === 'file' || field === 'name' || field === 'version'
}
