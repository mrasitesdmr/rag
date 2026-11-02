TASK: TASK-013
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `frontend-a11y` (toggle düğmesi `aria-pressed`, `role="group"`, `aria-live`), `frontend-design` (lab `.fb`, `.fb button`, `.fb button.on`, `.fb .ho`, `.msgs .m .tm`, `.toasts`, `.toast` token aktarımı), `react-state-management` (hook seviyesindeki mutation callback'leri).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| model/types (AC1) | `features/preview/model/types.ts` | `SubmitFeedbackRequest`, `HANDOFF_REASONS`/`HandoffReason`, `RequestHandoffRequest`, `HandoffResponse` (`openapi.yaml` ile birebir). `PreviewTurn`'e `conversationId: number \| null`, `answeredAt: string \| null` |
| Toast metinleri (Kararlar #16) | `features/preview/model/feedbackMessages.ts` | `FEEDBACK_HELPFUL_TOAST`, `FEEDBACK_NOT_HELPFUL_TOAST` (lab metinleri) |
| api/ (AC1) | `features/preview/api/previewApi.ts`, `previewQueries.ts` | `submitFeedback(turnId, body)` → `POST /api/assistant/turns/{turnId}/feedback` (204 → `undefined`); `requestHandoff(conversationId, body)` → `POST /api/assistant/conversations/{conversationId}/handoff`. `useSubmitFeedbackMutation({ onSuccess, onError })`, `useRequestHandoffMutation({ onSuccess, onError })`: sonuç hook seviyesindeki callback'lerle (variables ile) işlenir, `mutate(vars, { onSuccess })` kullanılmaz |
| hooks/useAnswerStream (K1e, G5) | `features/preview/hooks/useAnswerStream.ts` | Yeni tur `conversationId: conversationIdRef.current`, `answeredAt: null` ile başlar; `createConversation` sonrası tura konuşma kimliği yazılır; `done` olayında `answeredAt = new Date().toISOString()`. Başka davranış değişmedi |
| K1a, K1b, K1c, Kararlar #11-13 | `features/preview/hooks/useTurnFeedback.ts` | `Record<turnId, TurnFeedbackState>`; seçim cevaptan sonra yazılır (iyimser değil); istek sürerken (`isSaving`) ya da kayıtlı değerle aynı düğmeye basınca istek gönderilmez. Başarı → `helpful` + toast; hata → `helpful` aynı kalır, `errorCode` (`ApiError` değilse `UNEXPECTED_ERROR`). Ard arda çift tıklamaya karşı kontrol ref üzerinden yapılır |
| K2a, K2b, Kararlar #11, #15, G2, G3 | `features/preview/hooks/useHandoffRequests.ts` | `Record<conversationId, HandoffState>`; gövde `{ reason: 'NO_ANSWER', turnId }`; `isQueued` ya da `isRequesting` iken yeni istek yok. Başarı → `isQueued: true`; hata → `errorCode`, `isQueued: false` |
| Kararlar #17 | `shared/toast/ToastProvider.tsx`, `ToastProvider.module.css`, `useToast.ts`, `toastContext.ts`, `toastRules.ts` | `useToast().showToast(message)`; provider dışında `Error('useToast must be used within ToastProvider')`. Bölge `aria-live="polite"`, lab `.toasts` (alt ortada, 22 px, `pointer-events: none`, `position: fixed`: lab'da sol yarının tamamına göre konumlanır, bizde sol yarı bütün ekran). Toast `TOAST_DURATION_MS = 2600` ms sonra kalkar; unmount'ta zamanlayıcılar temizlenir; `prefers-reduced-motion`'da animasyon yok |
| AdminLayout | `app/AdminLayout.tsx` | `<main>` içeriği (`ErrorBoundary` + `Outlet`) `ToastProvider` ile sarıldı |
| K1a, K1e, K5 | `features/preview/components/TurnFeedback.tsx` | `role="group" aria-label="Geri bildirim"`: "Faydalı mı?", "Evet"/"Hayır" (`aria-pressed`, seçili olan `.selected` = lab `.fb button.on`, `isSaving` iken ikisi `disabled`), saat `formatTime(answeredAt)` (lab `.tm`: `message.time`). Hata: satırın altında `<p role="alert">{messageFor(errorCode)}</p>` |
| K2a, K2c, K5 | `features/preview/components/HandoffAction.tsx` | `isQueued` → "Temsilci kuyruğundasın"; değilse "Temsilciye bağlan" düğmesi (lab `.fb .ho`, `isRequesting` iken `disabled`). Hata alert'i aynı biçimde |
| Lab `.fb` görünümü (AC3) | `features/preview/components/feedbackRow.module.css`, `src/index.css` | İki satırın ortak stili. Yeni token'lar: `--color-feedback-ink` #8c7f86, `--color-feedback-line` #e3d9de, `--color-feedback-button-ink` #4f4248, `--color-toast-bg` #2a1f24, `--text-feedback` 11px, `--text-toast` 13px, `--radius-toast` 8px, `--radius-feedback-button` 10px, `--shadow-toast`, `--offset-toast-bottom` 22px, `--offset-toast-enter` 6px, `--duration-toast-in` 0.2s, `--z-toast` 30 |
| K1c, K2a, G4 | `features/preview/components/AnswerMessage.tsx` | Sadece `phase === 'done'` turda: kaynak düğmeleri ve boş bilgi bankası notundan sonra, `done.notFoundAnswer` ise `HandoffAction` (`handoffFor(turn.conversationId)`, `requestHandoff(conversationId, done.turnId)`), ardından her zaman `TurnFeedback` (`feedbackFor(done.turnId)`). `waiting`/`streaming`/`failed` turlarda ikisi de yok |
| G5 | `features/preview/components/MessageList.tsx` | `feedback` ve `handoff` props'u önceki konuşmanın turları dahil `TurnMessages` → `AnswerMessage`'a geçer; her tur kendi `conversationId`'siyle çalışır |
| Kararlar #11 | `features/preview/pages/PreviewPage.tsx` | `useTurnFeedback()`, `useHandoffRequests()` sayfa seviyesinde; `MessageList`'e verilir |
| K5 | `shared/api/errorMessages.ts` | `TURN_NOT_FOUND`, `TURN_NOT_COMPLETED`, `HANDOFF_NOT_ALLOWED` (plandaki metinler) |
| `web/CLAUDE.md` | `web/CLAUDE.md` | Durum yorumu (TASK-013), preview dosya listesi, `shared/toast/` eklendi; "Henüz olmayanlar"dan `shared/toast/` çıkarıldı |
| K2d (soru 1 = A) | — | `sendMessage` ve giriş alanı değişmedi; aktarılan konuşmada soru sorulmaya devam eder |
| K3 | — | Hiçbir yerde temsilci mesajı üretilmez; aktarmada toast yok (lab) |

## Değişen dosyalar
Yeni:
- `web/src/features/preview/model/feedbackMessages.ts`
- `web/src/features/preview/hooks/useTurnFeedback.ts`
- `web/src/features/preview/hooks/useHandoffRequests.ts`
- `web/src/features/preview/components/TurnFeedback.tsx`
- `web/src/features/preview/components/HandoffAction.tsx`
- `web/src/features/preview/components/feedbackRow.module.css`
- `web/src/shared/toast/ToastProvider.tsx`, `ToastProvider.module.css`, `useToast.ts`, `toastContext.ts`, `toastRules.ts`

Değişen:
- `web/src/features/preview/model/types.ts`
- `web/src/features/preview/api/previewApi.ts`, `previewQueries.ts`
- `web/src/features/preview/hooks/useAnswerStream.ts`
- `web/src/features/preview/components/AnswerMessage.tsx`, `MessageList.tsx`
- `web/src/features/preview/pages/PreviewPage.tsx`
- `web/src/app/AdminLayout.tsx`
- `web/src/shared/api/errorMessages.ts`
- `web/src/index.css`
- `web/CLAUDE.md`

Silinen: yok. Eklenen paket: yok.

## Ekran metinleri
- Geri bildirim satırı: grup `aria-label="Geri bildirim"`; metin `Faydalı mı?`; düğmeler `Evet`, `Hayır` (`aria-pressed="true|false"`); saat `HH:MM` (Europe/Istanbul, `formatTime(answeredAt)`).
- Aktarma satırı: düğme `Temsilciye bağlan`; aktarıldıktan sonra metin `Temsilci kuyruğundasın`.
- Toast (bölge `aria-live="polite"`): `Geri bildirim kaydedildi` (Evet), `Geri bildirim kaydedildi; bu tur incelemeye düştü` (Hayır). Aktarmada toast yok.
- Hata metinleri (`role="alert"`, satırın hemen altında):
  - `TURN_NOT_FOUND` → `Bu cevap bulunamadı. Sayfayı yenile.`
  - `TURN_NOT_COMPLETED` → `Tamamlanmamış bir cevaba geri bildirim verilemez.`
  - `HANDOFF_NOT_ALLOWED` → `Temsilciye sadece asistanın cevap bulamadığı sorularda bağlanabilirsin.`
  - `CONVERSATION_NOT_FOUND` (aktarmada) → `Konuşma bulunamadı. Yeni konuşma başlat.` (mevcut)
  - `NETWORK_ERROR` → `Sunucuya ulaşılamıyor. Bağlantını kontrol et.`; bilinmeyen/500 → `Beklenmeyen bir hata oluştu. Tekrar dene.`

## Plandan sapmalar
Davranış sapması yok. Dosya düzeyinde üç fark (lead'e mesajla bildirildi):
1. Plan `TurnFeedback.module.css` ve `HandoffAction.module.css` diyordu; iki bileşen lab'da aynı `.fb` satırını ve `.fb button` tabanını paylaştığı için tek `components/feedbackRow.module.css` kullanıldı (mevcut `message.module.css` / `sideCard.module.css` kalıbı).
2. `TOAST_DURATION_MS` `ToastProvider.tsx`'te değil `shared/toast/toastRules.ts`'te; context `shared/toast/toastContext.ts`'te. Sebep: oxlint `react/only-export-components` component dosyasından sabit/context export'unu uyarı olarak veriyor, projede uyarı bırakılmıyor. Değer 2600 ms.
3. `TurnFeedback` props'u planla aynı; görünüm için dosya içinde küçük bir `FeedbackButton` yardımcı bileşeni var (export edilmiyor).

## Doğrulama
- `cd web && npm run build` → **kaldı**, tek hata tester'a ait dosyada: `src/test/suggestionFixtures.ts(39,3): error TS2739: ... missing the following properties from type 'PreviewTurn': conversationId, answeredAt`. Plandaki `PreviewTurn` değişikliğinin (iki yeni alan) beklenen sonucu; fixture'ı tester güncelleyecek (plan "Değişen mevcut parçalar").
- Uygulama kodu tek başına: geçici `tsconfig` (`tsconfig.app.json`'u extend eder, `src/test/**` ve `*.test.ts(x)` hariç) ile `npx tsc -p …` → **geçti** (0 hata); geçici dosya silindi. `npx vite build` → **geçti** (`✓ built`).
- `npm run lint` (oxlint) → **geçti** (çıkış 0, uyarı yok).
- `npm test` → **geçti**: 28 test dosyası, 481 test (vitest tip kontrolü yapmadığı için fixture hatası testleri etkilemiyor; testler `App` → `AdminLayout` üzerinden render ettiği için `ToastProvider` mevcut).
- Tarayıcıda elle deneme yapılmadı (backend uçları paralel yazılıyor); lab ile görsel karşılaştırma qa'da (M4).

## Tester için notlar
- **Build için önce:** `src/test/suggestionFixtures.ts` `turn()` fixture'ına `conversationId` (ör. `1`) ve `answeredAt` (`phase === 'done'` için bir ISO zaman, aksi `null`) eklenmeli.
- **MSW varsayılan handler'ları (plan):** `http.post('/api/assistant/turns/:turnId/feedback', () => new HttpResponse(null, { status: 204 }))`; `http.post('/api/assistant/conversations/:conversationId/handoff', () => HttpResponse.json({ conversationId: 1, turnId: 12, reason: 'NO_ANSWER', queue: 'bireysel', createdAt: '2026-11-02T10:31:05Z' }, { status: 202 }))`. Hatalar `problem(409, 'TURN_NOT_COMPLETED')`, `problem(404, 'TURN_NOT_FOUND')`, `problem(409, 'HANDOFF_NOT_ALLOWED')`, ağ hatası `HttpResponse.error()`.
- **Satırların görünmesi için** akışın `done` olayıyla bitmesi gerekir (`turnId: 12`); "Temsilciye bağlan" için `done.notFoundAnswer: true`. `createConversation` cevabındaki `conversationId` tura yazılır; aktarma isteği o kimliğe gider.
- **Saat (F1):** `answeredAt`, `done` olayının işlendiği an `new Date()`; `vi.setSystemTime` ile sabitlenebilir. Biçim Europe/Istanbul `HH:MM` (UTC `10:31` → `13:31`).
- **Seçim iyimser değil:** `aria-pressed="true"` istek 204 döndükten sonra görünür; `findBy*`/`waitFor` kullan. İstek sürerken iki düğme `disabled`.
- **Aynı düğme (F4):** kayıtlı değerle aynı düğmeye basınca istek gitmez, toast çıkmaz.
- **Çift tıklama (F11):** MSW cevabını `await delay()` ile geciktir; istek sürerken "Temsilciye bağlan" `disabled`, istek sayısı 1.
- **Konuşma düzeyi (F9):** aktarma durumu `conversationId` anahtarlı; aynı konuşmanın ikinci "cevap yok" turunda da "Temsilci kuyruğundasın". "Yeni konuşma" sonrası ikinci `createConversation` farklı bir `conversationId` dönmeli (ör. `2`) ki yeni konuşmada düğme yeniden görünsün.
- **Toast (F13):** `ToastProvider` + `useToast` doğrudan render edilebilir; süre `shared/toast/toastRules.ts` → `TOAST_DURATION_MS` (2600). Fake timers ile `vi.advanceTimersByTime(2600)`. Toast bölgesi `ToastProvider`'ın içinde render edilir (portal değil). `PreviewPage`'i `App`/`AdminLayout` dışında render eden bir test olursa `ToastProvider` ile sarılmalı (plan: `renderWithProviders`'a eklemek).
- **Hata alert'leri** satırın altında `role="alert"`; aynı ekranda başka alert'ler (akış hatası) olabileceği için metinle sorgulamak daha güvenli.

## Düzeltme
Yok.

STATUS: DONE
