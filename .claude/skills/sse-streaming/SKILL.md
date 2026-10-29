---
name: sse-streaming
description: Cevabın Server-Sent Events ile token token akıtılması - assistant-api'de akış endpoint'i, olay adları ve sırası, akış öncesi ve akış içi hata, istemci kopunca model çağrısının iptali, zaman aşımı, proxy tamponlaması; web'de fetch + ReadableStream ile ayrıştırma, AbortController ve MSW ile sahte akış. Akış endpoint'i, eventStream.ts, useAnswerStream ya da bunların testlerini yazarken, akış yarıda kopuyor ya da token'lar toplu geliyorsa kullan.
---

# Cevap Akışı (SSE)

> **Proje kuralı:** `backend-conventions` bölüm 7 ve `frontend-conventions` bölüm 8 bu skill'den önce gelir. Olay adları, sırası ve payload'ları `docs/api/sse.md`'dedir (ilk akış task'ında planner oluşturur); bu dosya sözleşmeyi değil kalıbı anlatır. Spring ve Spring AI API ayrıntıları kurulan sürümün dokümanıyla doğrulanır.

## 1. Sözleşme

- Endpoint (lab): `POST /api/assistant/conversations/{conversationId}/messages`, gövde `{ "text": "...", "channel": "WEB" }`, yanıt `200` + `Content-Type: text/event-stream` + `Cache-Control: no-cache`.
- Olay sırası (lab): `token`* → `sources` → `done`; hata olursa `error` ve akış kapanır.
- `data` her zaman tek satır JSON, alanlar camelCase. Lab'daki şekiller:
  - `token`: akan metin parçası
  - `sources`: alıntılanan parçalar (`n`, `chunkId`, `heading` …)
  - `done`: `turnId`, `state`, `promptTokens`, `completionTokens`, `latencyMs`
  - `error`: `errorCode`
- Kesin alan adları `docs/api/sse.md`'dedir; backend ve web ondan okur.

## 2. Backend (assistant-api)

- Dönüş tipi (`SseEmitter` ya da `Flux<ServerSentEvent<…>>`) ilk akış task'ının planında seçilir; ikisi de servlet stack'te (Spring MVC) çalışır. Spring AI `ChatClient.stream()` bir `Flux` döner.
- Akış başlamadan yapılacak kontroller (doğrulama, konuşma var mı, süren tur var mı, retrieval) akış açılmadan önce yapılır; bunların hatası normal HTTP status + Problem Details'tir.
- Akış başladıktan sonraki hata `event: error` + `{"errorCode": "CHAT_MODEL_UNAVAILABLE"}` ile bildirilir, emitter tamamlanır; tur `INTERRUPTED` olarak kaydedilir.
- `done` olayı tur kaydı commit edildikten sonra gönderilir; istemci `done`'daki `turnId` ile Konuşmalar'a gidebilir.
- İptal: istemci bağlantıyı kapatırsa `onCompletion` / `onError` / `onTimeout` callback'lerinde model aboneliği `dispose` edilir; boşa token üretilmez.
- Zaman aşımı: servlet async timeout (`spring.mvc.async.request-timeout` ya da emitter'a verilen süre) lokal modelin en uzun cevabından uzun olmalıdır; aksi halde akış ortasında kesilir.
- Header'lar: `Cache-Control: no-cache`; nginx arkasında `X-Accel-Buffering: no`.
- Akış thread'i ile istek thread'i farklıdır; MDC/trace context taşınmalıdır (`observability-tracing` bölüm 6), paylaşılan state thread-safe olmalıdır (`java-concurrency`).

## 3. Backend testi

- `MockMvcTester` ile istek başlatılır, async dispatch ile gövde okunur (`spring-mvc-testing`); olay adları, sırası ve `data` alanları doğrulanır.
- Dil modeli sahte `ChatModel`'dir: sabit token dizisi döner; hata testi için akış ortasında hata fırlatır.
- Test adları: `givenFakeChatModel_whenSendMessage_thenStreamsTokensSourcesAndDone`, `givenModelFailsMidStream_whenSendMessage_thenSendsErrorEventAndMarksTurnInterrupted`, `givenClientDisconnects_whenStreaming_thenCancelsModelSubscription`.

## 4. Web

- `EventSource` sadece `GET` destekler; akış `POST` olduğu için `fetch` + `response.body.getReader()` + `TextDecoder` kullanılır. Ayrıştırıcı `shared/api/eventStream.ts`'tedir.
- Ayrıştırıcı satır tamponu tutar: bir ağ parçası olayın ortasında bitebilir; olay ancak boş satır (`\n\n`) gelince işlenir. `event:` ve `data:` satırları okunur, `data` JSON parse edilir ve type guard ile doğrulanır (`frontend-security`).
- `response.ok` değilse gövde Problem Details'tir: `errorCode` okunur, mesaj `errorMessages.ts`'ten gelir.
- `useAnswerStream` hook'u: akışı event handler'dan başlatır (effect'ten değil), token'ları updater ile biriktirir (`setAnswer((text) => text + event.text)`), `AbortController` ile iptal eder (yeni soru, sayfadan çıkış, unmount), `done` gelince konuşma sorgularını invalidate eder (`react-state-management/references/details.md`).
- Akış sürerken gönderme butonu kapalıdır; durum değişiklikleri `aria-live="polite"` bölgesinde duyurulur, her token duyurulmaz (`frontend-a11y`).
- Cevap HTML olarak işlenmez; alıntı `[n]` sadece aralıktaysa düğmeye çevrilir (`frontend-security`).

## 5. Web testi

- MSW handler'ı `ReadableStream` ile SSE gövdesi döner; yardımcı `src/test/sse.ts` (`sseResponse([{ event, data }, …])`):

```ts
export function sseResponse(events: { event: string; data: unknown }[]): Response {
  const encoder = new TextEncoder()
  const stream = new ReadableStream({
    start(controller) {
      for (const { event, data } of events) {
        controller.enqueue(encoder.encode(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`))
      }
      controller.close()
    },
  })
  return new Response(stream, { headers: { 'Content-Type': 'text/event-stream' } })
}
```

- Ayrıştırıcı testinde olay iki ağ parçasına bölünür (satır ortasından); doğru birleştiği doğrulanır.
- Test adları: `givenTokensThenDone_whenStreamFinishes_thenShowsFullAnswerAndEnablesSend`, `givenEventSplitAcrossChunks_whenParsed_thenEmitsSingleEvent`, `givenErrorEventMidStream_whenStreaming_thenShowsChatModelUnavailableMessage`, `givenStreamInProgress_whenNewQuestionSent_thenAbortsPreviousStream`.

## 6. Proxy ve smoke test

- Vite dev proxy akışı tamponlamaz; araya sıkıştırma middleware'i eklenmez (`vite-patterns`).
- nginx (`web/nginx.conf`, devops): akış yolunda `proxy_buffering off`, `proxy_http_version 1.1`, `proxy_read_timeout` lokal modelin en uzun cevabından uzun.
- Smoke: `curl -sN -X POST localhost:3000/api/assistant/conversations/<id>/messages -H 'Content-Type: application/json' -d '{"text":"Gold kartın yıllık aidatı ne kadar?","channel":"WEB"}'`; token'lar parça parça gelmelidir. Hepsi tek seferde geliyorsa bir yerde tampon vardır.

## 7. Tuzaklar

- `EventSource` ile `POST` yapılamaz; `fetch` kullan.
- Ayrıştırıcıda `split('\n')` ile her ağ parçasını ayrı işlemek, bölünmüş olayları bozar.
- `done` commit'ten önce gönderilirse istemci henüz görünmeyen turu ister.
- Emitter timeout'u varsayılanda bırakmak uzun cevaplarda akışı keser.
- Akış testinde gerçek modele gitmek testi yavaş ve kararsız yapar.
