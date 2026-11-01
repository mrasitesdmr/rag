---
name: frontend-conventions
description: RAG asistan projesinin frontend'i için bağlayıcı proje kuralları - kod dili (İngilizce) ve UI metni (Türkçe), yorum politikası, feature-first klasör yapısı, component/hook/dosya/type isimlendirmesi, domain sözlüğü (conversation, turn, citation, source, chunk, evaluation, handoff), TypeScript kuralları, API katmanı ve errorCode → Türkçe mesaj eşlemesi, cevabın SSE ile token token akıtılması, içe alma ve değerlendirme ilerlemesinin polling ile izlenmesi (WebSocket gelirse STOMP), tarayıcıda saklanan veri, form validation, stil (CSS Modules + design token) ve test isimlendirmesi (given_when_then, AAA). web/ altında herhangi bir TypeScript/React kodu veya test yazarken, isimlendirmeye ya da dosyanın nereye konacağına karar verirken, backend'e çağrı ya da akış eklerken veya kod review yaparken kullan. Diğer skill'lerdeki örneklerle çelişirse bu skill geçerlidir.
---

# Frontend Proje Kuralları

Bu kurallar diğer tüm frontend skill'lerinin (`react-patterns`, `react-testing`, `react-best-practices` vb.) örneklerinden önce gelir. Backend tarafındaki karşılığı `backend-conventions`'tır; ortak kavramlar (domain sözlüğü, `errorCode`, pagination, test isimlendirmesi) iki tarafta aynıdır.

> **Durum:** Web iskeleti ve kabuğu TASK-002'de Vite `react-ts` şablonuyla kuruldu (üst bar, menü, dört ekranın başlığı); ekran içerikleri henüz yok. Bu skill hedef kuralları yazar; kesinleşen yapı `web/CLAUDE.md`'dedir.
> **Referans:** Ekranların içeriği ve görsel dili `kaynaklar/rag-lab.html`'in sol yarısından (asistan yönetim paneli: Önizleme, Konuşmalar, Bilgi kaynakları, Değerlendirme), anlatım `kaynaklar/rag.html`'den gelir. Bilgi kaynakları ekranlarının çalışan bir örneği `../embedding-pgvector/frontend`'dir (okunur kalıp referansı); `source` örneklerinin çalışan hali oradadır.
>
> **Ürün hedefi:** Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. "Aynısı" şu demektir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder.

## 1. Teknoloji yığını

| Konu | Seçim | Not |
|------|-------|-----|
| Build | Vite 8 | `vite-patterns` |
| UI | React 19 | Function component + hook. Class component yok (Error Boundary için `react-error-boundary`) |
| Dil | TypeScript 6, `strict` (TS 6'da varsayılan) | `strict` veya alt bayrakları kapatılmaz |
| Lint | oxlint (`npm run lint`) | Uyarı bırakılmaz |
| Server state | TanStack Query | `useEffect` + `fetch` yok |
| Client state | `useState` / `useReducer` / Context | Global store (Zustand) sadece gerçek ihtiyaçta ve planda onaylanmışsa |
| Routing | React Router (library/declarative mode) | |
| HTTP | Native `fetch`, `shared/api/httpClient.ts` üzerinden | axios yok |
| Cevap akışı (SSE) | Native `fetch` + `ReadableStream`, `shared/api/eventStream.ts` üzerinden | `EventSource` kullanılmaz (sadece GET yapar, gövde ve `Authorization` gönderemez); SSE kütüphanesi eklemek plan kararıdır |
| WebSocket | `@stomp/stompjs`, native WebSocket | SockJS yok; sadece STOMP gereken task'ta kurulur |
| Stil | CSS Modules + `src/index.css` içinde CSS custom property token'ları | UI kütüphanesi (MUI vb.) ve Tailwind yok |
| Test | Vitest (jsdom) + React Testing Library + user-event + `@testing-library/jest-dom` + MSW + vitest-axe | `react-testing` |
| E2E | Playwright | `e2e-testing` |

Listede olmayan bir runtime dependency (ör. markdown render, grafik kütüphanesi) eklemek plan kararıdır; coder kendi başına eklemez. Henüz kurulmamış bir kütüphane, onu gerektiren ilk task'ta kurulur.

## 2. Dil

- Tüm kod İngilizce: dosya, component, hook, değişken, type, sabit, CSS class adı, test adı, `data-testid`, console mesajı.
- Kullanıcıya gösterilen her metin Türkçe: label, buton, placeholder, hata mesajı, boş durum metni, `aria-label`, sayfa başlığı. Yerleşik teknik terimler (RAG, LLM, retrieval, rerank, prompt, token, embedding, chunk, recall, MRR) anlatım sayfalarındaki gibi İngilizce bırakılabilir; aynı terim her yerde aynı yazılır.
- Domain sözlüğü backend ile aynıdır (`backend-conventions` bölüm 1): konuşma → `conversation`, tur → `turn`, mesaj → `message`, kanal → `channel`, üretim modu → `generationMode`, cevap → `answer`, alıntı → `citation`, retrieval stratejisi → `retrievalStrategy`, bağımsız soru → `standaloneQuery`, geri bildirim → `feedback`, temsilciye aktarma → `handoff`, değerlendirme çalıştırması → `evalRun`, isabet oranı → `recallAtK`, `mrr`; çalışma alanı → `workspace` (tek ve sabit; sadece retrieval-api adreslerinde ve Bilgi kaynakları bilgi kutusunda görünür), bilgi kaynağı → `source`, parça → `chunk`, içe alma işi → `ingestionJob`; konu anlatım sayfası → `lesson`.
- `handoff` kodda "temsilciye aktarma"dır; ekip iş akışındaki `handoff/` dosyalarıyla ilgisi yoktur.
- Backend'in `detail` alanı (İngilizce) kullanıcıya **asla** gösterilmez. Türkçe metin her zaman `errorCode`'dan üretilir (bölüm 7).

### UI metni yazımı

- Sentence case: "Soru sor", "Değerlendirmeyi başlat" (her kelime büyük harfle değil).
- Buton ve etiket metinleri lab'ın sol yarısındakiyle birebir aynıdır (Önizleme: soru kutusu `aria-label="Soru"`, buton "Gönder"; Bilgi kaynakları: "Belge ekle", "Yükle ve işle", "Vazgeç"; Değerlendirme: "Ölçümü çalıştır · rerank 20→5"). Lab'da karşılığı olmayan butonda ad ne olacağını tam söyler ("Aktar" değil "Temsilciye aktar"). Aynı eylem akış boyunca aynı adı taşır.
- Hata mesajı ne olduğunu ve gerekiyorsa ne yapılacağını söyler, özür dilemez: "Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene."
- Yönetim paneli metinlerinde hitap "sen" ile. Asistanın müşteriye cevabı dil modelinden gelir; onun dili ve hitabı sistem talimatındadır, frontend metni değildir.

## 3. Yorum politikası

- Component, hook ve function üstüne yorum **yazılmaz**: JSDoc (`/** ... */`) yok, `//` ile açıklama yok.
- Kod kendini isimlerle anlatır. Açıklama gerekiyorsa önce yeniden adlandır ya da küçük bir function/component'e ayır.
- Gövde içinde yorum sadece koddan çıkarılamayan bir **neden** için ve tek satır (ör. bir tarayıcı workaround'unun sebebi).
- Testlerdeki `// Arrange`, `// Act`, `// Assert` işaretleri istisnadır (bölüm 11).
- Yorum satırına alınmış kod, `console.log` ve `debugger` bırakılmaz.

## 4. Klasör yapısı (feature-first)

```
web/
├── e2e/                         Playwright testleri (e2e-testing)
├── public/
└── src/
    ├── main.tsx                 Giriş noktası; sadece render + provider'lar
    ├── index.css                Design token'ları (:root) ve global reset
    ├── app/
    │   ├── App.tsx              Router tanımı
    │   ├── AppProviders.tsx     QueryClientProvider vb.
    │   └── AdminLayout.tsx      Başlık, yan menü, ToastProvider; panel sayfalarının iskeleti
    ├── features/
    │   ├── preview/             Önizleme: asistanla sohbet, akan cevap, alıntılar, geri bildirim, aktarma
    │   │   ├── api/
    │   │   │   ├── previewApi.ts        REST ve akış çağrıları (saf async function'lar)
    │   │   │   └── previewQueries.ts    previewKeys + useXQuery / useXMutation hook'ları
    │   │   ├── components/
    │   │   │   ├── MessageComposer.tsx
    │   │   │   ├── MessageComposer.module.css
    │   │   │   └── MessageComposer.test.tsx
    │   │   ├── hooks/           ör. useAnswerStream.ts
    │   │   ├── pages/
    │   │   │   └── PreviewPage.tsx
    │   │   └── model/
    │   │       ├── types.ts             Bu feature'ın DTO type'ları
    │   │       ├── messageRules.ts      Validation sabitleri ve function'ları
    │   │       └── citations.ts         Cevaptaki [n] alıntılarını parçalara eşleyen saf function'lar
    │   ├── conversation/        Konuşmalar listesi ve konuşma detayı (turlar, modele giden parçalar)
    │   ├── source/              Bilgi kaynakları, kaynak detayı, yükleme ve içe alma ilerlemesi
    │   └── evaluation/          Değerlendirme çalıştırmaları ve sonuçları
    ├── shared/
    │   ├── api/                 httpClient.ts, eventStream.ts, ApiError.ts, errorMessages.ts, pagination.ts
    │   ├── components/          Feature'dan bağımsız UI parçaları (Dialog, ErrorAlert, StatusBadge, Pagination)
    │   ├── format/              Sayı, süre ve tarih biçimleme (tr-TR)
    │   ├── toast/               ToastProvider.tsx, useToast.ts
    │   └── websocket/           (ilk WebSocket task'ında açılır) stompClient.ts, useStompSubscription.ts
    └── test/                    setup.ts, server.ts (MSW), handlers.ts, problem.ts, sse.ts, renderWithProviders.tsx
```

- Feature klasörleri lab'ın yan menüsünü izler: `preview`, `conversation`, `source`, `evaluation`. Tur ve alıntı tipleri iki feature'da (`preview`, `conversation`) gerekiyorsa `shared/turn/`'e taşınır; feature'lar birbirini import etmez. Konu anlatım sayfaları taşınırsa sadece frontend'de olan `lesson` feature'ı açılır.
- Feature'ın iç yapısı her zaman `api/`, `components/`, `hooks/`, `pages/`, `model/`'dir. Feature kökünde dosya durmaz. `model/` backend'deki `domain` paketinin karşılığıdır: type'lar, iş kuralları ve UI'dan bağımsız saf function'lar (ör. alıntı eşleme, oranı yüzdeye çeviren formatter). İhtiyacı olmayan klasör boş olarak açılmaz.
- `shared/` birden fazla feature'ın ya da `app/`'in kullandığı kodu tutar. `errorCode` → Türkçe mesaj `shared/api/errorMessages.ts`'tedir (backend `exception`). SSE ayrıştırma `shared/api/eventStream.ts`'tedir.
- Bir feature başka bir feature'ın içinden import etmez. Ortak ihtiyaç `shared/`'a taşınır. `shared/` hiçbir feature'ı import etmez.
- Barrel dosyası (`index.ts` ile re-export) açılmaz; doğrudan dosyadan import et (`react-best-practices` / `bundle-barrel-imports`).
- Import'lar göreli path ile ve dosya uzantısıyla yapılır (`./MessageComposer.tsx`, şablondaki `allowImportingTsExtensions` ile uyumlu).
- Test dosyası test ettiği dosyanın yanında durur (`X.test.tsx`). `__tests__` klasörü yok.

## 5. İsimlendirme

### Dosyalar ve export'lar

| Rol | Dosya adı | Export | Örnek |
|-----|-----------|--------|-------|
| Component | `PascalCase.tsx` | Aynı adlı named export | `MessageComposer.tsx` → `export function MessageComposer` |
| Sayfa | `<Name>Page.tsx` | named | `PreviewPage`, `ConversationDetailPage` |
| Hook | `use<Name>.ts` | named | `useAnswerStream.ts` |
| CSS Module | `<Component>.module.css` | - | `MessageComposer.module.css` |
| REST modülü | `<feature>Api.ts` | function'lar | `previewApi.ts` |
| Query modülü | `<feature>Queries.ts` | `<feature>Keys` + hook'lar | `conversationQueries.ts` |
| Type'lar | `model/types.ts` | named type'lar | |
| Kural/sabit | `model/<topic>Rules.ts` | named | `messageRules.ts` |
| Test | `<Dosya>.test.ts(x)` | - | `citations.test.ts` |

- **Default export yok** (named export'lar yeniden adlandırmayı ve arama yapmayı güvenli kılar). `React.lazy` gerektiğinde `lazy(() => import('./EvaluationPage.tsx').then(m => ({ default: m.EvaluationPage })))`. Tek istisna, aracın default export beklediği config dosyalarıdır (`vite.config.ts`, `playwright.config.ts`): `export default defineConfig(...)`.
- Dosya başına bir export edilen component. Sadece o dosyada kullanılan küçük yardımcı component'ler aynı dosyada, top-level'da tanımlanabilir (component içinde component tanımlanmaz). İstisna: compound component parçaları (`Tabs`, `TabsList`, `TabsPanel`) tek dosyada birlikte named export edilir (`react-composition-patterns`).

### Kod içi isimler

| Anlam | Kalıp | Örnek |
|-------|-------|-------|
| Component | PascalCase isim | `TurnBubble`, `CitationList`, `EvalRunTable` |
| Props type'ı | `<Component>Props` | `TurnBubbleProps` |
| Callback prop'u | `on<Event>` | `onSent`, `onCitationOpen` |
| Handler function'ı | `handle<Event>` | `handleSubmit`, `handleFeedback` |
| Boolean | `is`, `has`, `can`, `should` | `isStreaming`, `hasCitations`, `canHandoff` |
| REST / akış function'ı | Backend service metodu ile aynı fiil | `sendMessage`, `submitFeedback`, `requestHandoff`, `startEvalRun`, `listConversations` |
| Query hook'u | `use<Noun>Query` | `useConversationQuery(conversationId)` |
| Mutation hook'u | `use<Verb><Noun>Mutation` | `useSubmitFeedbackMutation()`, `useStartEvalRunMutation()` |
| Akış hook'u | `use<Noun>Stream` | `useAnswerStream()` |
| Query key factory | `<feature>Keys` | `conversationKeys.detail(conversationId)` |
| Sabit | `UPPER_SNAKE_CASE` | `MESSAGE_MAX_LENGTH`, `EVAL_POLL_INTERVAL_MS` |
| CSS Module class'ı | camelCase | `styles.turnBubble`, `styles.isStreaming` |
| `data-testid` | kebab-case, son çare | `turn-latency` |

- Kısaltma yok: `conversation` (`conv` değil), `message` (`msg` değil), `event` (`e` sadece tek satırlık inline handler'da kabul).
- Koleksiyonlar çoğul (`turns`, `citations`), id'ler `<noun>Id` (`conversationId`, `turnId`, `chunkId`).
- Magic number yok; kural değerleri `<topic>Rules.ts`'te sabit olur.
- Route path'leri lab'daki adresler gibi Türkçe ve kebab-case'tir; kullanıcının adres çubuğunda gördüğü metin olduğu için bölüm 2'deki "UI metni Türkçe" kuralına girer. Lab'daki adresler referanstır:
  - `/onizleme`, `/konusmalar`, `/konusmalar/:conversationId`, `/kaynaklar`, `/kaynaklar/:sourceId`, `/degerlendirme`
  - `/` → `/onizleme` yönlendirmesi. Bilinmeyen adres → "Sayfa bulunamadı". Adreste çalışma alanı yoktur. Anlatım sayfalarının adresleri, gelirlerse, planda belirlenir.
- Route parametreleri ve kod içi isimler yine İngilizcedir (`:conversationId`, `:sourceId`); sadece sabit path segmentleri Türkçedir.

## 6. TypeScript

- `any` yok. Bilinmeyen veri (`response.json()`, SSE olayının `data`'sı, STOMP `message.body`) `unknown` olarak alınır ve tipine daraltılır.
- Non-null assertion (`!`) sadece `main.tsx`'teki `getElementById('root')!` gibi garanti edilen yerlerde.
- `enum` ve constructor parameter property kullanılmaz (`erasableSyntaxOnly` açık). Yerine string literal union:

```ts
export const GENERATION_MODES = ['LLM_ONLY', 'RAG'] as const
export type GenerationMode = (typeof GENERATION_MODES)[number]
```

- Enum değerleri backend'deki gibi `UPPER_SNAKE_CASE` string'lerdir.
- Object şekilleri ve props için `type` kullanılır (`interface` yok); tutarlılık için tek biçim. Diğer skill'lerdeki `interface` örnekleri projede `type` olarak yazılır.
- Biçim Vite şablonuyla aynıdır: 2 boşluk girinti, tek tırnak, noktalı virgül yok, çok satırlı listelerde sondaki virgül. Diğer skill'lerdeki çift tırnaklı / noktalı virgüllü örnekler bu biçime çevrilerek yazılır.
- Type-only import'lar `import type { TurnResponse } from '../model/types.ts'` (`verbatimModuleSyntax` açık).
- API type'ları backend DTO adlarıyla **birebir aynıdır** ve `docs/api/openapi.yaml`'a uyar (`ConversationResponse`, `TurnResponse`, `SendMessageRequest`, `EvalRunResponse` …). SSE olay type'ları `docs/api/sse.md`'deki adlarla aynıdır (`TokenEvent`, `SourcesEvent`, `DoneEvent`, `StreamErrorEvent`) ve tek bir ayrık birleşim (`AnswerStreamEvent`) olarak tanımlanır. Multipart istekler (dosya yükleme) için frontend'e özel girdi type'ı `<Verb><Noun>Input` adını taşır ve API function'ında `FormData`'ya çevrilir. Alan adları camelCase, tarih alanları ISO-8601 `string`.

## 7. API katmanı ve hata gösterimi

### Katmanlar

```
Component  →  use<X>Query / use<X>Mutation / use<X>Stream  →  <feature>Api.ts  →  shared/api/httpClient.ts | eventStream.ts  →  fetch
```

- Component `fetch`'i ya da `<feature>Api`'yi doğrudan çağırmaz; query/mutation/akış hook'u kullanır.
- `<feature>Api.ts` saf async function'lardır, React bilmez; böylece MSW ile ve tek başına test edilebilir.
- Path'ler göreli: `/api/assistant/...` (assistant-api; ör. `/api/assistant/conversations/${conversationId}/messages`) ve `/api/workspaces/42/...` (retrieval-api, Bilgi kaynakları); ikisi de `docs/api/openapi.yaml`'da. Dev'de Vite proxy, production'da reverse proxy yönlendirir (`vite-patterns`). Base URL kodda sabitlenmez.

### httpClient ve ApiError

```ts
// shared/api/ApiError.ts
export type Violation = { field: string; message: string }

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string
  readonly violations: Violation[]

  constructor(status: number, errorCode: string, violations: Violation[] = []) {
    super(errorCode)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
    this.violations = violations
  }
}
```

```ts
// shared/api/httpClient.ts
const UNEXPECTED_ERROR = 'UNEXPECTED_ERROR'
const NETWORK_ERROR = 'NETWORK_ERROR'

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  const body = init?.body
  if (body !== undefined && body !== null && !(body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, NETWORK_ERROR)
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError(response.status, UNEXPECTED_ERROR)
  }
}
```

- Gövde `FormData` ise `Content-Type` yazılmaz; tarayıcı `multipart/form-data; boundary=…` başlığını kendisi koyar. Dosya yüklemesi için ayrı bir istek function'ı açılmaz, hata eşlemesi tek yerde kalır.
- `toApiError`, `application/problem+json` body'sinden `errorCode` ve `violations`'ı okur; body yoksa, parse edilemezse ya da `errorCode` içermiyorsa `errorCode: 'UNEXPECTED_ERROR'` kullanır. `eventStream.ts` akış başlamadan gelen hata cevabında aynı `toApiError`'ı kullanır.
- Backend hata formatı RFC 9457 Problem Details'tir: `type`, `title`, `status`, `detail`, `instance`, `errorCode`, validation'da `violations[]` (bkz. `problem-details-rfc9457`).

### errorCode → Türkçe mesaj

Tüm eşleme tek dosyadadır: `shared/api/errorMessages.ts`. Component içinde hata metni yazılmaz.

```ts
const ERROR_MESSAGES: Record<string, string> = {
  CONVERSATION_NOT_FOUND: 'Konuşma bulunamadı. Listeyi yenile.',
  CHAT_MODEL_UNAVAILABLE: 'Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.',
  RETRIEVAL_UNAVAILABLE: 'Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.',
  STREAM_INTERRUPTED: 'Cevap yarıda kesildi. Soruyu yeniden gönder.',
  VALIDATION_FAILED: 'Girdiğin bilgileri kontrol et.',
  NETWORK_ERROR: 'Sunucuya ulaşılamıyor. Bağlantını kontrol et.',
}

const FALLBACK_MESSAGE = 'Beklenmeyen bir hata oluştu. Tekrar dene.'

export function messageFor(errorCode: string): string {
  return ERROR_MESSAGES[errorCode] ?? FALLBACK_MESSAGE
}
```

- Örnekteki kodlar kalıbı gösterir; gerçek liste sözleşmeden gelir. Backend'e yeni bir `errorCode` eklendiğinde aynı task içinde buraya da Türkçe karşılığı eklenir; MSW handler'ı ve testi de.
- Bir alana ait hatalar o input'un altında gösterilir ve input'a `aria-describedby` + `aria-invalid` ile bağlanır (`frontend-a11y`): `VALIDATION_FAILED`'da `violations[].field`'a göre, metin alanın kural mesajından (`MESSAGE_RULE_MESSAGE` vb., bölüm 10) gelir; backend `message`'ı gösterilmez.
- Input'a ait olmayan hatalar (model kapalı, ağ hatası, fallback) form üstünde `role="alert"` ile gösterilir. Akış sırasında gelen `error` olayı, yarım kalan cevabın altında aynı `messageFor(errorCode)` ile gösterilir; yarım metin silinmez ama tamamlanmış gibi gösterilmez.
- Component içinde Türkçe hata metni literal olarak yazılmaz: `errorCode` metinleri `errorMessages.ts`'ten, alan kuralı metinleri `<topic>Rules.ts`'ten gelir.
- Beklenmeyen hatalar `console.error` ile loglanır (soru ve cevap metni olmadan), kullanıcıya fallback mesajı gösterilir. Stack trace veya `detail` ekrana basılmaz.

### Pagination

Backend `PageResponse<T>` döndürür (`backend-conventions` bölüm 6). Frontend karşılığı `shared/api/pagination.ts`'tedir:

```ts
export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type PageParams = { page?: number; size?: number }
```

- `page` 0'dan başlar; ekranda gösterirken `page + 1`. `size` en fazla 100.
- Query key'e sayfa parametreleri dahil edilir: `conversationKeys.list({ page, size })`.

## 8. Cevap akışı, ilerleme ve gerçek zamanlı iletişim

Ayrıntı ve test kalıbı: `sse-streaming`.

### Cevap akışı (SSE)

Müşteri sorusu `POST .../messages` ile gönderilir, cevap `text/event-stream` olarak akar (olaylar `docs/api/sse.md`'de; lab: `token`, `sources`, `done`, hata için `error`).

- Akışı sadece `shared/api/eventStream.ts` ayrıştırır: `fetch` + `response.body` okuyucusu, `TextDecoder`, satır tamponu; tamamlanmamış satır bir sonraki parçaya taşınır. Her olay `unknown` olarak parse edilir, şekli doğrulanır ve `AnswerStreamEvent`'e daraltılır; bilinmeyen olay atlanır.
- Akış tek bir hook'ta yönetilir (`useAnswerStream`): istek başına bir `AbortController`; component unmount olunca ya da yeni soru gönderilince önceki akış `abort()` edilir. StrictMode'da effect iki kez çalışır; akış bir effect'ten değil, gönderme handler'ından başlatılır.
- Akan metin turun yerel state'inde birikir ve her zaman updater biçimiyle eklenir (`setAnswer((text) => text + event.text)`). Token'lar TanStack Query cache'ine yazılmaz; akış `done` ile bitince ilgili konuşma sorguları invalidate edilir ve kalıcı tur verisi oradan okunur.
- Akış sürerken gönderme butonu `disabled` olur, "yazıyor" göstergesi görünür; ilk token gelene kadar geçen süre beklemedir, hata değildir. Durum değişiklikleri (cevap tamamlandı, hata) `aria-live="polite"` bölgesinde duyurulur; her token duyurulmaz (`frontend-a11y`).
- `sources` olayındaki parçalar alıntı numaralarıyla eşlenir (`model/citations.ts`); cevaptaki `[n]` ancak eşleşen bir parça varsa tıklanabilir olur.
- Cevap metni düz metin olarak render edilir; `dangerouslySetInnerHTML` kullanılmaz (dil modeli çıktısı ve parça içeriği güvenilmeyen veridir, `frontend-security`). Markdown gösterimi gerekirse plan kararıdır.

### İçe alma ve değerlendirme ilerlemesi: polling

- Uzun süren işler (kaynak içe alma, değerlendirme çalıştırması) `202` + `Location` ile başlar ve iş durumu sorgulanarak izlenir. İş, kendi key'inde `refetchInterval` ile sorgulanır; iş aktifken `<TOPIC>_POLL_INTERVAL_MS` (`model/<topic>Rules.ts`), bitince ya da 4xx hatasında `false` döner. Başlatma mutation'ının döndüğü iş `initialData` olarak verilir, ilk istek beklenmez.
- Süren işin bilgisi backend'den gelir (ör. kaynağın `latestJob` alanı); sayfa yenilense ya da adres yeni sekmede açılsa da ilerleme kaldığı yerden görünür. Referans uygulama: `../embedding-pgvector/frontend/src/features/source` (`IngestionWatcherProvider`, `useIngestionJobsPolling`).
- İş bitince ilgili liste ve detay sorguları invalidate edilir. Aynı veri ayrıca `useState`'te ya da ikinci bir store'da tutulmaz.

### WebSocket gelirse (STOMP)

Bu bölüm, STOMP'u getiren ilk task için hedeftir; planı o task'ın planner'ı yazar ve `docs/api/websocket.md`'yi oluşturur. Cevap akışı WebSocket'e taşınmaz, SSE'de kalır. Sözleşme backend ile ortaktır (`spring-websocket`):

| Yön | Destination |
|-----|-------------|
| İstemci → sunucu | `/app/<resource>/{id}/<action>` |
| Sunucu → izleyenler | `/topic/<resource>/{id}/<event>` (ör. `/topic/sources/10001/ingestion`) |
| Sunucu → tek istemci | `/user/queue/<event>` (ör. `/user/queue/errors`) |

- Bağlantı adresi göreli: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`. Dev'de Vite proxy `/ws`'i `ws: true` ile backend'e yönlendirir.
- Heartbeat `10000/10000` (backend ile aynı), `reconnectDelay` 2000 ms.
- Uygulamada tek bir STOMP client vardır (`shared/websocket/stompClient.ts`); component'ler client oluşturmaz.
- Subscription'lar hook içinde kurulur ve cleanup'ta `unsubscribe()` edilir. StrictMode'a dayanıklıdır (`react-best-practices` / `advanced-init-once`); mesaj callback'leri `useEffectEvent` ile sarılır (`advanced-effect-event-deps`).
- Gelen mesaj `unknown` olarak parse edilir, şekli doğrulanır, sonra polling'in kullandığı **aynı** cache girdisine updater biçimiyle yazılır; ilerleme için yeni bir query key açılmaz. WebSocket gelince polling kaldırılır ya da sadece bağlantı koptuğunda yedek olarak çalışır; hangisi olacağına planner karar verir.
- Bağlantı durumu (bağlanıyor / bağlı / koptu) kullanıcıya `aria-live="polite"` bir mesajla gösterilir.

## 9. Tarayıcıda saklanan veri

- Hesap ve oturum yoktur (lab'daki `Authorization` başlığı bankanın kimlik sisteminin temsilidir; kimlik doğrulama eklemek plan kararıdır).
- Lab'da üretim modu (Sadece LLM / RAG), retrieval (top-5 / rerank 20→5), takip sorusunu yeniden yazma ve reranker durumu sağ yarıdaki düğmelerle değişir. Sağ yarı gelmediği için bu ayarlar ekrandan değiştirilmez; `assistant-api` yapılandırmasıdır (`assistant.*`, ortam değişkeniyle). Başlıktaki "Canlı yapılandırma" rozeti lab'daki gibi kalır; Değerlendirme'deki ölçüm düğmesi etkin stratejiyi lab'daki gibi gösterir ("Ölçümü çalıştır · rerank 20→5"), değeri backend verir. Bu ayarlar tarayıcıda tutulmaz.
- `localStorage` sadece gizli olmayan, kişiye özel görünüm tercihleri için kullanılır (ör. açık sekme). Erişim tek bir `model/<topic>Settings.ts` modülünden yapılır; key versiyonludur (`rag:<topic>:v1`), okunan değer şekil ve aralık kontrolünden geçer, bozuk alan varsayılana döner, okuma/yazma `try/catch` içindedir (`client-localstorage-schema`).
- Müşteri soruları, cevaplar ve konuşma geçmişi `localStorage`'a yazılmaz; kaynağı backend'dir.
- Gizli bilgi (model API anahtarı, token) frontend'e hiç gelmez: ne `VITE_` env değişkenine, ne `localStorage`'a, ne de bir response'a (`frontend-security`).
- Kullanıcının yazdığı soru ve gelen cevap log'lanmaz ve analitik/hata raporlarına konmaz.

## 10. Formlar ve validation

- Basit formlar React 19 `useActionState` + native `<form action>` ile yazılır (`react-patterns`). Dosya alanı olan formlarda `useActionState` kullanılmaz (React form action'ı bitince alanları sıfırlar, backend hatasında seçilen dosya kaybolur); `onSubmit` + mutation kullanılır. Çok adımlı/dinamik alanlı bir form çıkarsa form kütüphanesi plan kararıdır.
- İstemci tarafı validation sadece kullanıcı deneyimi içindir; doğruluk kaynağı backend'dir. Kurallar backend ile aynı değerleri kullanır ve `<topic>Rules.ts`'te tek yerde durur:

```ts
export const MESSAGE_MIN_LENGTH = 2
export const MESSAGE_MAX_LENGTH = 1000

export function normalizeMessage(input: string): string {
  return input.trim().replace(/\s+/g, ' ')
}

export const MESSAGE_RULE_MESSAGE = `Soru ${MESSAGE_MIN_LENGTH}-${MESSAGE_MAX_LENGTH} karakter olmalı.`

export function isValidMessage(message: string): boolean {
  const normalized = normalizeMessage(message)
  return normalized.length >= MESSAGE_MIN_LENGTH && normalized.length <= MESSAGE_MAX_LENGTH
}
```

- Örnekteki sınır değerler kalıbı gösterir; gerçek değerler TASK'tan ve sözleşmeden gelir.
- Alan kuralının Türkçe mesajı kuralın yanında, aynı dosyadadır (`<FIELD>_RULE_MESSAGE`); hem istemci tarafı kontrol hem backend'in `VALIDATION_FAILED` / `violations[].field` cevabı bu mesajı gösterir.
- Kontrolden önce `normalize…` / `trim()` uygulanır; sadece boşluktan oluşan değer geçersizdir. Türkçe karakterler (ı, İ, ş, ğ) olduğu gibi gönderilir; büyük/küçük harf dönüşümü gerekiyorsa `toLocaleLowerCase('tr')` kullanılır.
- Submit sırasında buton `disabled` olur ve metni eylemi anlatır ("Gönderiliyor…").
- Her input'un `<label htmlFor>`'u vardır; placeholder label yerine geçmez (`frontend-a11y`).

## 11. Testler

### İsimlendirme

Backend ile aynı biçim: `given<Precondition>_when<Action>_then<ExpectedResult>`

```ts
describe('PreviewPage', () => {
  it('givenStreamedTokens_whenQuestionSent_thenShowsAnswerAsTokensArrive', async () => { ... })
  it('givenChatModelDown_whenQuestionSent_thenShowsChatModelUnavailableMessage', async () => { ... })
  it('givenErrorEventMidStream_whenStreaming_thenKeepsPartialAnswerAndShowsInterruptedMessage', async () => { ... })
})
```

- `describe` adı test edilen component/hook/function adıdır.
- `it` kullanılır (`test` değil); tek biçim.

### Gövde yapısı: AAA

```tsx
it('givenChatModelDown_whenQuestionSent_thenShowsChatModelUnavailableMessage', async () => {
  // Arrange
  server.use(http.post('/api/assistant/conversations/:conversationId/messages', () => problem(503, 'CHAT_MODEL_UNAVAILABLE')))
  const user = userEvent.setup()
  renderWithProviders(<PreviewPage />)

  // Act
  await user.type(screen.getByLabelText('Soru'), 'Gold kartın yıllık aidatı ne kadar?')
  await user.click(screen.getByRole('button', { name: 'Gönder' }))

  // Assert
  expect(await screen.findByRole('alert')).toHaveTextContent('Dil modeline şu an ulaşılamıyor.')
})
```

- Bölümler `// Arrange`, `// Act`, `// Assert` ile ayrılır; Arrange gerekmiyorsa atlanabilir.
- Query önceliği: `getByRole` > `getByLabelText` > `getByText` > `getByTestId` (`react-testing`). Türkçe UI metinleriyle sorgulanır; bu, metnin de test edilmesini sağlar.
- Network her zaman MSW ile mock'lanır (msw 3: `server.listen({ onUnhandledFrame: 'error' })`); `fetch` veya `<feature>Api` `vi.mock` ile mock'lanmaz. Handler'lar `docs/api/openapi.yaml`'a uyar; hata response'ları `src/test/problem.ts`'teki `problem(status, errorCode)` ile üretilir.
- SSE akışı MSW handler'ında `ReadableStream` gövdeli bir `HttpResponse` (`Content-Type: text/event-stream`) ile sahtelenir; olaylar `src/test/sse.ts`'teki yardımcıyla `docs/api/sse.md`'deki biçimde yazılır. Parçalı gelen satır (olayın iki chunk'a bölünmesi), akış ortasında `error` ve kullanıcının yeni soru göndermesiyle iptal ayrı ayrı test edilir.
- Her `errorCode` için en az bir test, kullanıcıya doğru Türkçe mesajın gösterildiğini doğrular.
- Oran ve süre gösterimi testlerinde beklenen değer fixture'daki sabit değerlerden elle türetilir (ör. `recallAtK: 0.8` → "0,80"); formatter'ın kendisi beklenen değeri üretmek için kullanılmaz.
- Her etkileşimli component için bir `axe` testi (`vitest-axe`).
- Snapshot testi yok. `setTimeout`/sabit bekleme yok; `findBy*` ve `waitFor` kullan.
- Polling içeren ekranlar MSW handler'ının art arda farklı aşamalar dönmesiyle test edilir; beklenen istek sayısı ve iş bitince sorgunun durduğu doğrulanır.
- Playwright E2E testleri (`web/e2e/`) aynı isim biçimini kullanır; Playwright API'si gereği `it` yerine `test` / `test.describe` yazılır. Gerçek dil modeliyle cevap metni deterministik değildir: E2E cevabın tam metnine değil, akışın bittiğine, alıntı sayısına, gösterilen kaynaklara ve "bulamadım" gibi sabit cümlelere bakar (`e2e-testing`). AAA yorumları E2E'de de kullanılır, sabit bekleme (`waitForTimeout`) yoktur.
- Coverage hedefi: satır bazında en az %80 (`react-testing` / coverage thresholds).

## 12. Stil

- Her component kendi `X.module.css`'ini kullanır. Global CSS sadece `src/index.css`'te: reset, font ve design token'ları.
- Renk, boşluk, radius, font boyutu gibi değerler CSS custom property token'larından gelir (`var(--color-surface)`, `var(--space-3)`); component CSS'inde ham hex değer ya da rastgele px boşluk yazılmaz. Görsel dil lab'ın sol yarısıyla aynıdır: token değerleri lab'ın `--s-*` (site) ve `--f-*` (font) değişkenlerinden birebir alınır; `--o-*` (konsol) kullanılmaz. `frontend-design`'ın özgün tasarım önerileri sadece lab'da karşılığı olmayan durumlar içindir.
- Yazı tipleri kendi sunucumuzdan gelir (`@fontsource/*`); CDN kullanılmaz (CSP `font-src 'self'`).
- Inline `style` sadece runtime'da hesaplanan değerler için (ör. ilerleme yüzdesi, skor çubuğunun genişliği).
- Mobile-first; hedef en dar ekran 360 px. Kod blokları ve prompt örnekleri dar ekranda yatay kaydırılır, sayfa taşmaz. Görünür focus stili kaldırılmaz (`outline: none` tek başına yasak). `prefers-reduced-motion` desteklenir ("yazıyor" animasyonu dahil).
- Şablondan gelen `App.css`, `assets/hero.png`, `react.svg`, `vite.svg` ilk gerçek ekran yazılırken silinir.

## 13. Komutlar

Hepsi `web/` içinde çalıştırılır.

```bash
npm run dev        # Vite dev server (5173); /api/assistant → 8090, /api/workspaces → 8080 proxy'lenir
npm run build      # tsc -b && vite build (type-check dahil)
npm run lint       # oxlint
npm test           # vitest run
npm run test:e2e   # Playwright, sistem açıkken (ayrıntı web/CLAUDE.md)
```

Bir iş bitmeden önce `build`, `lint` ve testler hatasız geçmelidir.

## Kontrol listesi

```
- [ ] Tüm identifier'lar İngilizce, kullanıcıya görünen tüm metinler Türkçe
- [ ] Component/function üstünde yorum yok, commented-out kod, console.log, debugger yok
- [ ] Dosya feature-first yapıda doğru klasörde; feature'lar birbirini import etmiyor; barrel yok
- [ ] Default export yok; isimler bölüm 5'teki kalıplara ve domain sözlüğüne uyuyor
- [ ] any, enum, gereksiz `!` yok; API ve SSE olay type'ları sözleşmedeki adlarla aynı
- [ ] Backend çağrısı query/mutation/akış hook'u üzerinden; component'te fetch yok; SSE sadece eventStream.ts'te ayrıştırılıyor
- [ ] Akış AbortController ile iptal ediliyor; token'lar updater biçimiyle ekleniyor; `done` sonrası sorgular invalidate ediliyor
- [ ] Cevap ve parça içeriği düz metin olarak render ediliyor (dangerouslySetInnerHTML yok)
- [ ] Hata metni errorMessages.ts'ten; backend `detail`'i ekranda değil
- [ ] localStorage sadece görünüm tercihleri için ve tek modülden; frontend'de gizli anahtar yok, soru/cevap loglanmıyor
- [ ] Polling iş bitince duruyor; (WebSocket varsa) STOMP subscription'ları cleanup'ta kapanıyor
- [ ] Testler given_when_then isimli, AAA yapılı, MSW ile; her errorCode için mesaj testi var; akış parçalı satır, hata ve iptal durumlarıyla test ediliyor
- [ ] Stil CSS Module + token ile; focus görünür
- [ ] build, lint ve testler geçiyor
```
