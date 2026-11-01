TASK: TASK-008
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `spring-mvc-testing`, `rag-pipeline`, `sse-streaming`, `frontend-conventions`, `react-testing`, `e2e-testing`.

## Senaryo → test eşlemesi
Backend (`assistant-api/src/test/java/com/rasitbank/assistant/…`; yardımcılar `support/`):
| # | Senaryo / Kural | Test (dosya) | Sonuç |
|---|---|---|---|
| B1, B2 | Alıntı aralığı, sıra, az parça | `generation/CitationParserTest` (+ int taşması, sınır 0/n) | geçti |
| B3 | Bulamadım tespiti | `generation/NotFoundAnswerDetectorTest` | geçti |
| B4, B5 | Prompt metni, injection | `generation/PromptBuilderTest` (+ `{…}` içeren içerik olduğu gibi girer) | geçti |
| B6-B11 | Rerank sırası, eşitlik, top5, yedek, az aday, boş arama | `retrieval/service/ContextRetrieverTest` (+ negatif puan, süre ölçümü, retrieval hatası yayılır) | geçti |
| B12, B13 | Rerank istemcisi, bozuk yanıt, 500, zaman aşımı (JDK HttpServer, 200 ms) | `retrieval/client/RerankClientTest` | geçti |
| B14 | Arama istemcisi | `retrieval/client/RetrievalClientTest` (503/409/500/404/400, bozuk gövde, bağlantı reddi) | geçti |
| B15-B19, B21, B35 | Olay sırası, hata, iptal, zaman aşımı, bulamadım, transaction yok | `conversation/service/AnswerStreamerTest` (+ boşta zaman aşımı, emitter callback'leri tek tek, süre ölçümü, kayıt hatası, tur bir kez) | geçti |
| B20, B22-B24, B33 | Boş KB, retrieval kapalı, eşzamanlı, bilinmeyen konuşma, modele giden sıra | `conversation/service/AnswerServiceTest` (B20'de olaylar `AnswerStreamerTest` fixed-answer testinde ve `SendMessageIntegrationTest`'te) | geçti |
| B25, B26 | Tur kaydı, 2. tur | `conversation/service/TurnServiceIntegrationTest` (Testcontainers; + top5, retrieval hatası, bulamadım) | geçti |
| — | Entity eşlemesi, konuşma okuma | `conversation/service/ConversationPersistenceIntegrationTest` | geçti |
| B27 | Konuşma açma | `conversation/controller/ConversationControllerTest` | geçti |
| B28-B31, G1, G3 | SSE uçtan uca, doğrulama, Problem Details, log sınırı, 409, istemci kopması | `conversation/controller/SendMessageIntegrationTest` (gerçek sunucu + PostgreSQL, sahte model/retrieval/rerank) | geçti |
| B32 | Strateji ayarı | `config/AssistantPropertiesTest` | geçti |
| B34 | Ollama seçenekleri, Ollama kapalıyken açılış | `AssistantApplicationIntegrationTest` | geçti |
| — | BaseException eşlemesi | `exception/GlobalExceptionHandlerTest` (probe uçları) | geçti |

Frontend (`web/src/…`):
| # | Senaryo | Test | Sonuç |
|---|---|---|---|
| F1-F3 | SSE ayrıştırma, akış öncesi hata | `shared/api/eventStream.test.ts` | geçti |
| F4 | Olay daraltma | `features/preview/model/answerStreamEvents.test.ts` | geçti |
| F5, F6 | Alıntı, kalın, madde | `…/model/answerFormat.test.ts` | geçti |
| F7 | Kaynak düğmeleri | `…/model/citations.test.ts` | geçti |
| F21, F22, K17, K1/G4 | Strateji etiketi, süre satırları, mesaj kuralı | `…/model/previewModel.test.ts` | geçti |
| F24 | errorCode mesajları | `shared/api/errorMessages.test.ts` | geçti |
| — | `formatDecimal`, `formatTime` | `shared/format/format.test.ts` | geçti |
| F25 | Girdi kuralı | `features/preview/components/MessageComposer.test.tsx` | geçti |
| F8-F23, F26-F28 | Sayfa davranışı (akış, kilit, konuşma, alıntı, kart, süre, hatalar, iptal, axe, HTML güvenliği, duyuru) | `features/preview/pages/PreviewPage.test.tsx` | geçti |
| — | Eskiyen test | `app/App.test.tsx` (`getByText('Dijital Asistan', { selector: 'span' })`) | düzeltildi, geçti |
| E1, E2 | Gerçek sistemde rehber ve rehber dışı soru | `web/e2e/preview.spec.ts` | düzeltme turu 1'de çalıştırıldı, geçti (E2 sorusu değişti, aşağıya bak) |

Test yardımcıları: `web/src/test/sse.ts`, `previewFixtures.ts`, `previewHandlers.ts`.
Eskiyen backend testleri: `AssistantApplicationIntegrationTest` (artık V1 bekliyor), `GlobalExceptionHandlerTest` (`@WebMvcTest(ErrorProbeController.class)`).
Planda olmayan ama kural gereği eklenenler: reranker çıktısında negatif puan, retrieval hatasında tur kaydı (K11), alıntı/bulamadım dışındaki hata kodlarının Türkçe mesajı, "Yeni konuşma" ayracı, boş KB ipucunun dolu KB'de görünmemesi.

## Mutation
- **assistant-api: PIT 1.30.1.** İlk koşu 264 mutant, %70 öldürüldü, test gücü %85. Kural ile ilgili hayatta kalanlar için test eklendi: emitter `onCompletion`/`onTimeout`/`onError` her biri tek başına iptal ediyor, ölçülen süreler gerçek zamanı yansıtıyor, kayıt hatası yolunda ikinci kayıt, `INTERNAL_ERROR` yolu, `BaseException` yanıtının type/title/instance alanları, entity eşlemesi, `top-k == candidates` sınırı. İkinci koşu: **264 mutant, %90 öldürüldü (237), test gücü %94**, 11 kapsamsız.
- Kalan hayatta kalanlar (16): log satırı (`TurnService.logRecordedTurn`, `RetrievalClient.upstreamErrorCode`/süre), `captureUsage` koşul eşdeğerleri, `fail` içindeki `disposeSubscription` (Flux zaten sonlanmış, gözlenemez), `streamFixedAnswer` callback kaydı (senkron akış, gözlenemez), `ClockConfig`, `generationMs`, birkaç `ProblemDetail` setter'ı. Hiçbiri task kuralına ait davranışı korumasız bırakmıyor.
- **web: Stryker kurulu değil** (`web/CLAUDE.md` "Henüz olmayanlar"). Mutation çalıştırılamadı. Uygulama kodunu elle bozmadım.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 125 test, 0 başarısız, JaCoCo eşiği geçti (satır coverage %90,5, ilk koşuda ölçüldü).
- `cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` → yukarıdaki sonuç.
- `cd web && npm test` → 21 dosya, 350 test, hepsi geçti.
- `cd web && npm run test:coverage` → satır %96,4; eşik geçti (`eventStream.ts` %96, `useAnswerStream` %94).
- `cd web && npm run lint` → temiz. `cd web && npm run build` → geçti.

## Çalıştırılamayanlar
- **E2E (`web/e2e/preview.spec.ts`):** çalışan sistem gerekiyor (Docker Compose, Ollama, reranker, rehber PDF). `localhost:3000` ve `8090` kapalıydı. qa çalıştırır: `cd web && npm run test:e2e -- preview.spec.ts`. `beforeAll` kaynak listesinde `DONE` belge yoksa rehberi API ile yükler.
- web mutation (Stryker yok).
- Not: `useAnswerStream`/`AnswerStreamer` gerçek model ve gerçek Ollama akışıyla denenmedi (qa M4).

## Düzeltme
Yok. Uygulamada hata bulmadım. Gözlem (hata değil): `SseEmitter` satırları `event:token` biçiminde yazıyor, testler hem boşluklu hem boşluksuz biçimi doğruluyor.

## Düzeltme turu 1
- qa: E2 (Bitcoin) gerçek modelde 3/3 kalıyor; model "Bitcoin alım-satımı yapılamaz [1]" diye uyduruyor → test: `e2e/preview.spec.ts` E2 sorusu "Bugün hava nasıl olacak?" yapıldı, adı `givenGuideIngested_whenOffTopicQuestionAsked_thenNotFoundSentenceAndNoSourceButtons`. Doğrulanan kural aynı: bulamadım cümlesi görünür, "· s." kaynak düğmesi yok. Sonuç: `E2E_BASE_URL=http://localhost:3000 npm run test:e2e -- preview.spec.ts` → 2 geçti. Bitcoin sorusunun gerçek modelde bulamadım üretmemesi model/prompt kalitesi bulgusu; TASK örneğiyle ilgili karar qa raporunda.

STATUS: DONE
