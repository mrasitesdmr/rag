const ERROR_MESSAGES: Record<string, string> = {
  VALIDATION_FAILED: 'Girdiğin bilgileri kontrol et.',
  NETWORK_ERROR: 'Sunucuya ulaşılamıyor. Bağlantını kontrol et.',
  WORKSPACE_NOT_FOUND: 'Çalışma alanı bulunamadı.',
  SOURCE_NOT_FOUND: 'Kaynak bulunamadı.',
  INGESTION_JOB_NOT_FOUND: 'İçe alma işi bulunamadı. Listeyi yenile.',
  CHUNK_NOT_FOUND: 'Parça bulunamadı.',
  SOURCE_NAME_TAKEN: 'Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz.',
  FILE_TOO_LARGE: "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç.",
  UNSUPPORTED_FILE_TYPE: 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.',
  PDF_UNREADABLE:
    "PDF açılamadı; dosya bozuk ya da şifreli olabilir. Şifresini kaldırıp yeniden kaydet ya da PDF'i yeniden oluştur.",
  PDF_NO_TEXT:
    "PDF'te okunabilir metin yok; taranmış bir görüntü olabilir. Metni seçilebilen bir PDF yükle (ör. Word'de \"Farklı kaydet → PDF\").",
  PDF_NO_BOOKMARKS:
    "PDF'te yer imi (başlık) yok. Word'de başlıkları \"Başlık 1\", \"Başlık 2\" stilleriyle yaz; \"Farklı kaydet → PDF → Seçenekler\"de \"Şunu kullanarak yer işaretleri oluştur: Başlıklar\"ı işaretleyip yeniden kaydet.",
  EMBEDDING_MODEL_MISMATCH:
    'Sistemin embedding modeli çalışma alanınınkiyle aynı değil; belge yüklenemiyor. Sistem ayarını kontrol et.',
  SOURCE_ALREADY_INGESTED: 'Bu belge zaten işlenmiş. Listeyi yenile.',
  INGESTION_IN_PROGRESS: 'Bu belgenin işi zaten sürüyor.',
  EMBEDDING_UNAVAILABLE: 'Embedding modeline ulaşılamadı. Model sunucusu açılınca yeniden dene.',
  EMBEDDING_DIMENSION_MISMATCH: 'Embedding modeli beklenmeyen boyutta vektör döndü. Model ayarını kontrol et.',
  INGESTION_INTERRUPTED: 'Servis iş sürerken kapandı. Yeniden dene.',
  CONVERSATION_NOT_FOUND: 'Konuşma bulunamadı. Yeni konuşma başlat.',
  TURN_IN_PROGRESS: 'Bu konuşmada bir cevap hâlâ yazılıyor. Bitince yeniden sor.',
  RETRIEVAL_UNAVAILABLE: 'Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.',
  CHAT_MODEL_UNAVAILABLE: 'Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.',
  STREAM_INTERRUPTED: 'Cevap yarıda kesildi. Soruyu yeniden gönder.',
  TURN_NOT_FOUND: 'Bu cevap bulunamadı. Sayfayı yenile.',
  TURN_NOT_COMPLETED: 'Tamamlanmamış bir cevaba geri bildirim verilemez.',
  HANDOFF_NOT_ALLOWED: 'Temsilciye sadece asistanın cevap bulamadığı sorularda bağlanabilirsin.',
  GOLDEN_SET_NOT_LOADED: "Golden set yüklenmemiş. Önce golden set'i yükle.",
  GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE: "Golden set'in belgesi bilgi bankasında yok. Önce belgeyi yükle.",
  EVAL_RUN_IN_PROGRESS: 'Bir ölçüm zaten sürüyor. Bitmesini bekle.',
  EVAL_NOT_AVAILABLE_IN_LLM_ONLY: "Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al.",
}

export const FALLBACK_MESSAGE = 'Beklenmeyen bir hata oluştu. Tekrar dene.'

export const FALLBACK_PAGE_MESSAGE = 'Beklenmeyen bir hata oluştu. Sayfayı yenile.'

export function messageFor(errorCode: string): string {
  return ERROR_MESSAGES[errorCode] ?? FALLBACK_MESSAGE
}
