import { describe, expect, it } from 'vitest'
import { FALLBACK_MESSAGE, FALLBACK_PAGE_MESSAGE, messageFor } from './errorMessages.ts'

describe('messageFor', () => {
  it.each([
    ['VALIDATION_FAILED', 'Girdiğin bilgileri kontrol et.'],
    ['NETWORK_ERROR', 'Sunucuya ulaşılamıyor. Bağlantını kontrol et.'],
    ['WORKSPACE_NOT_FOUND', 'Çalışma alanı bulunamadı.'],
    ['SOURCE_NOT_FOUND', 'Kaynak bulunamadı.'],
    ['INGESTION_JOB_NOT_FOUND', 'İçe alma işi bulunamadı. Listeyi yenile.'],
    ['CHUNK_NOT_FOUND', 'Parça bulunamadı.'],
    ['SOURCE_NAME_TAKEN', 'Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz.'],
    ['FILE_TOO_LARGE', "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç."],
    ['UNSUPPORTED_FILE_TYPE', 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.'],
    [
      'PDF_UNREADABLE',
      "PDF açılamadı; dosya bozuk ya da şifreli olabilir. Şifresini kaldırıp yeniden kaydet ya da PDF'i yeniden oluştur.",
    ],
    [
      'PDF_NO_TEXT',
      "PDF'te okunabilir metin yok; taranmış bir görüntü olabilir. Metni seçilebilen bir PDF yükle (ör. Word'de \"Farklı kaydet → PDF\").",
    ],
    [
      'PDF_NO_BOOKMARKS',
      "PDF'te yer imi (başlık) yok. Word'de başlıkları \"Başlık 1\", \"Başlık 2\" stilleriyle yaz; \"Farklı kaydet → PDF → Seçenekler\"de \"Şunu kullanarak yer işaretleri oluştur: Başlıklar\"ı işaretleyip yeniden kaydet.",
    ],
    [
      'EMBEDDING_MODEL_MISMATCH',
      'Sistemin embedding modeli çalışma alanınınkiyle aynı değil; belge yüklenemiyor. Sistem ayarını kontrol et.',
    ],
    ['SOURCE_ALREADY_INGESTED', 'Bu belge zaten işlenmiş. Listeyi yenile.'],
    ['INGESTION_IN_PROGRESS', 'Bu belgenin işi zaten sürüyor.'],
    ['EMBEDDING_UNAVAILABLE', 'Embedding modeline ulaşılamadı. Model sunucusu açılınca yeniden dene.'],
    ['EMBEDDING_DIMENSION_MISMATCH', 'Embedding modeli beklenmeyen boyutta vektör döndü. Model ayarını kontrol et.'],
    ['INGESTION_INTERRUPTED', 'Servis iş sürerken kapandı. Yeniden dene.'],
    ['CONVERSATION_NOT_FOUND', 'Konuşma bulunamadı. Yeni konuşma başlat.'],
    ['TURN_IN_PROGRESS', 'Bu konuşmada bir cevap hâlâ yazılıyor. Bitince yeniden sor.'],
    ['RETRIEVAL_UNAVAILABLE', 'Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.'],
    ['CHAT_MODEL_UNAVAILABLE', 'Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.'],
    ['STREAM_INTERRUPTED', 'Cevap yarıda kesildi. Soruyu yeniden gönder.'],
  ])('givenErrorCode_whenMessageFor_thenReturnsCardText %s', (errorCode, expected) => {
    expect(messageFor(errorCode)).toBe(expected)
  })

  it.each(['INTERNAL_ERROR', 'UNEXPECTED_ERROR', 'BAD_REQUEST', 'NOT_FOUND', 'UNSUPPORTED_MEDIA_TYPE', '', 'ŞU_AN_YOK'])(
    'givenUnknownErrorCode_whenMessageFor_thenReturnsFallback %s',
    (errorCode) => {
      expect(messageFor(errorCode)).toBe('Beklenmeyen bir hata oluştu. Tekrar dene.')
    },
  )

  it('givenPageCrash_whenFallbackPageMessage_thenAsksForRefresh', () => {
    expect(FALLBACK_PAGE_MESSAGE).toBe('Beklenmeyen bir hata oluştu. Sayfayı yenile.')
    expect(FALLBACK_MESSAGE).toBe('Beklenmeyen bir hata oluştu. Tekrar dene.')
  })
})
