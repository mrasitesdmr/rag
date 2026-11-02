import type { EvalRunErrorCode } from './types.ts'

export const PAGE_DESCRIPTION_WITHOUT_SET =
  "Rehberde cevabı olan sorular; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar."

export function pageDescription(questionCount: number): string {
  return `Rehberde cevabı olan ${questionCount} soru; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar.`
}

export const GOLDEN_SET_NOT_LOADED_NOTICE = 'Golden set yüklenmemiş.'
export const LLM_ONLY_WARNING = "Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al."
export const NO_MEASUREMENT_TEXT =
  'Henüz ölçüm yok. Mevcut retrieval ayarıyla çalıştır; sonra ayarı değiştirip tekrar çalıştırarak iki satırı karşılaştır.'

export const RUN_BUTTON_IDLE = 'Ölçümü çalıştır'
export const RUN_BUTTON_RUNNING = 'Ölçülüyor'

export const KPI_RECALL_LABEL = 'recall@5'
export const KPI_MRR_LABEL = 'MRR'
export const KPI_MRR_SUBTITLE = 'doğru parçanın sırası'
export const KPI_MISSED_LABEL = 'Kaçan'
export const KPI_MISSED_SUBTITLE = "doğru parça prompt'ta yok"
export const KPI_RETRIEVAL_LABEL = 'Retrieval'

export const RUNS_SECTION_TITLE = 'Çalıştırmalar'
export const MISSED_SECTION_TITLE = 'Kaçanlar ve sırası değişenler'
export const MISSED_SECTION_NOTE = 'son çalıştırma'
export const MISSED_EMPTY_TEXT = 'Bütün sorularda doğru parça vector sırasıyla aynı yerde.'
export const ASK_IN_PREVIEW_TITLE = 'Önizlemede sor'

export const RUN_ROW_RUNNING = 'ölçülüyor…'
export const RUN_ROW_FAILED = 'başarısız'
export const RUN_ROW_EMPTY_METRIC = '–'

export const RUN_FAILURE_PREFIX = 'Son ölçüm tamamlanamadı: '

export const RUN_FAILURE_REASONS: Record<EvalRunErrorCode, string> = {
  RETRIEVAL_UNAVAILABLE: 'bilgi bankasına ulaşılamadı.',
  EVAL_RUN_INTERRUPTED: 'servis ölçüm sırasında yeniden başladı.',
  INTERNAL_ERROR: 'beklenmeyen bir hata oluştu.',
}

export const RUN_STARTED_ANNOUNCEMENT = 'Ölçüm başladı.'
export const RUN_FINISHED_ANNOUNCEMENT = 'Ölçüm tamamlandı.'
export const RUN_FAILED_ANNOUNCEMENT = 'Ölçüm tamamlanamadı.'
export const LOADING_TEXT = 'Yükleniyor…'
