import { describe, expect, it } from 'vitest'
import { ApiError } from '../../../shared/api/ApiError.ts'
import { normalizeUploadInput, toUploadErrors, validateSourceUpload } from './sourceUploadRules.ts'

const MIB = 1024 * 1024

function pdf(name = 'rehber.pdf', size = 1000): File {
  const file = new File(['x'], name, { type: 'application/pdf' })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

describe('validateSourceUpload', () => {
  it('givenValidInput_whenValidate_thenNoErrors', () => {
    expect(validateSourceUpload({ file: pdf(), name: '', version: '2026.10' })).toEqual({})
  })

  it('givenNoFileAndBlankVersion_whenValidate_thenBothFieldMessages', () => {
    expect(validateSourceUpload({ file: null, name: '', version: '   ' })).toEqual({
      file: 'Bir PDF dosyası seç.',
      version: 'Sürüm etiketi zorunlu; en fazla 40 karakter.',
    })
  })

  it.each([
    ['empty file', pdf('a.pdf', 0), 'Seçtiğin dosya boş. Başka bir PDF seç.'],
    ['one byte over 20 MiB', pdf('a.pdf', 20 * MIB + 1), "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç."],
    ['txt', pdf('notlar.txt'), 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.'],
    ['no extension', pdf('rehber'), 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.'],
    ['pdf only in the middle', pdf('a.pdf.exe'), 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.'],
    ['name of 256 chars', pdf(`${'a'.repeat(252)}.pdf`), 'Dosya adı en fazla 255 karakter olabilir.'],
  ])('givenBadFile_whenValidate_thenFileMessage %s', (_label, file, expected) => {
    expect(validateSourceUpload({ file, name: '', version: '1' }).file).toBe(expected)
  })

  it.each([
    ['exactly 20 MiB', pdf('a.pdf', 20 * MIB)],
    ['upper case extension', pdf('REHBER.PDF')],
    ['name of 255 chars', pdf(`${'a'.repeat(251)}.pdf`)],
  ])('givenAcceptedFile_whenValidate_thenNoFileMessage %s', (_label, file) => {
    expect(validateSourceUpload({ file, name: '', version: '1' }).file).toBeUndefined()
  })

  it.each([
    ['a', true],
    ['ab', false],
    ['x'.repeat(80), false],
    ['x'.repeat(81), true],
    ['', false],
    ['   ', false],
    [' a ', true],
  ])('givenName_whenValidate_thenRuleMessageOnlyOutside2To80 "%s"', (name, hasError) => {
    const errors = validateSourceUpload({ file: pdf(), name, version: '1' })

    expect(errors.name).toBe(hasError ? 'Kaynak adı 2-80 karakter olmalı.' : undefined)
  })

  it.each([
    ['1', false],
    ['v'.repeat(40), false],
    ['v'.repeat(41), true],
    ['', true],
    ['\t', true],
  ])('givenVersion_whenValidate_thenRuleMessageOnlyOutside1To40 "%s"', (version, hasError) => {
    const errors = validateSourceUpload({ file: pdf(), name: '', version })

    expect(errors.version).toBe(hasError ? 'Sürüm etiketi zorunlu; en fazla 40 karakter.' : undefined)
  })

  it('givenEmojiVersionOf40CodePoints_whenValidate_thenCountedByCharacterNotByUtf16Unit', () => {
    expect(validateSourceUpload({ file: pdf(), name: '', version: '😀'.repeat(40) }).version).toBeUndefined()
  })
})

describe('normalizeUploadInput', () => {
  it('givenPaddedNameAndVersion_whenNormalize_thenTrimmed', () => {
    const file = pdf()

    expect(normalizeUploadInput({ file, name: '  Atlaskap  ', version: ' 1.0\n' })).toEqual({
      file,
      name: 'Atlaskap',
      version: '1.0',
    })
  })
})

describe('toUploadErrors', () => {
  it.each([
    'FILE_TOO_LARGE',
    'UNSUPPORTED_FILE_TYPE',
    'PDF_UNREADABLE',
    'PDF_NO_TEXT',
    'PDF_NO_BOOKMARKS',
  ])('givenFileCode_whenToUploadErrors_thenUnderFileField %s', (code) => {
    const errors = toUploadErrors(new ApiError(422, code))

    expect(Object.keys(errors.fieldErrors)).toEqual(['file'])
    expect(errors.formErrorCode).toBeNull()
  })

  it('givenNameTaken_whenToUploadErrors_thenUnderNameField', () => {
    expect(toUploadErrors(new ApiError(409, 'SOURCE_NAME_TAKEN'))).toEqual({
      fieldErrors: { name: 'Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz.' },
      formErrorCode: null,
    })
  })

  it('givenViolationsOnAllFields_whenToUploadErrors_thenRuleMessagesNotBackendMessages', () => {
    const error = new ApiError(400, 'VALIDATION_FAILED', [
      { field: 'file', message: 'english a' },
      { field: 'name', message: 'english b' },
      { field: 'version', message: 'english c' },
    ])

    expect(toUploadErrors(error)).toEqual({
      fieldErrors: {
        file: 'Seçtiğin dosya boş ya da adı 255 karakterden uzun. Başka bir PDF seç.',
        name: 'Kaynak adı 2-80 karakter olmalı.',
        version: 'Sürüm etiketi zorunlu; en fazla 40 karakter.',
      },
      formErrorCode: null,
    })
  })

  it('givenViolationOnUnknownField_whenToUploadErrors_thenFormLevelValidationFailed', () => {
    const error = new ApiError(400, 'VALIDATION_FAILED', [{ field: 'other', message: 'x' }])

    expect(toUploadErrors(error)).toEqual({ fieldErrors: {}, formErrorCode: 'VALIDATION_FAILED' })
  })

  it.each(['EMBEDDING_MODEL_MISMATCH', 'NETWORK_ERROR', 'INTERNAL_ERROR'])(
    'givenOtherCode_whenToUploadErrors_thenFormLevel %s',
    (code) => {
      expect(toUploadErrors(new ApiError(500, code))).toEqual({ fieldErrors: {}, formErrorCode: code })
    },
  )

  it('givenNonApiError_whenToUploadErrors_thenFormLevelUnexpected', () => {
    expect(toUploadErrors(new TypeError('boom'))).toEqual({ fieldErrors: {}, formErrorCode: 'UNEXPECTED_ERROR' })
  })
})
