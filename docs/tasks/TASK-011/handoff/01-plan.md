TASK: TASK-011
# Plan

## Özet
**Mevcut durum (TASK-010):** Önizleme'nin yan sütununda yapılandırma şeridi yok; `HowItWorksCard` lab'ın rerank metnini sabit yazıyor ("bge-m3", "20", "5", "qwen3.5:9b" kodda). Üst bardaki "Canlı yapılandırma" rozeti ve ipucu metni zaten var (TASK-002, `AppHeader.tsx`). assistant-api'de üretim modu yok: `GenerationMode` sadece `RAG`, her tur retrieval yapar. Reranker'ın durumu sadece turun `RERANK_FALLBACK` stratejisinde görünür; ölçülüp saklanmaz.

**Yeni durum:**
- **assistant-api (değişen servis):**
  1. `assistant.generation.mode=${GENERATION_MODE:rag}` (`rag` | `llm`; başka değer açılışı düşürür) → `GenerationMode.RAG` / `LLM_ONLY`.
  2. `LLM_ONLY`'de `AnswerService` yeniden yazma, retrieval ve rerank yapmaz; prompt = kısa sistem talimatı (`prompts/llm-only-system.st`) + geçmiş + müşterinin sorusu. Alıntı ayrıştırılmaz, "bulamadım" tespiti yapılmaz. Tur `generation_mode = LLM_ONLY`, `retrieval_strategy = NONE`, arama sorgusu/aday/top-k boş, parçasız kaydedilir (V3 migration).
  3. SSE `sources`'a `generationMode`; `RetrievalStrategy`'ye `NONE`; `candidates`, `topK`, `searchQuery`, `done.retrievalLatencyMs` nullable (`sse.md`).
  4. Yeni uç `GET /api/assistant/live-config` (`liveconfig` paketi): yapılandırma + reranker durumu + retrieval-api'den embedding modeli ve bilgi bankası durumu.
  5. Reranker durumu ölçümü: `RerankerStatusTracker` (son ölçüm) + `RerankerHealthChecker` (15 sn'de bir `GET /health`) + `ContextRetriever` rerank sonucunu bildirir (kart soru 2, öneri C; A/B'de ne değiştiği Backend planında).
- **web:** Önizleme yan sütununun başına lab'ın yapılandırma şeridi (`LiveConfigStrip`); `HowItWorksCard` metni yapılandırmadan ve moda göre üç varyant; Sadece LLM cevabında parça kartı yerine "Asistan nasıl cevaplıyor?" kalır, alıntı ve "Rehber · s. N" düğmesi çıkmaz, "henüz belge yok" notu çıkmaz. Rozet değişmez.
- **retrieval-api:** değişmez (assistant-api mevcut `getWorkspace` ve `listSources` uçlarını çağırır).
- **Docker:** assistant-api ortamına `GENERATION_MODE`.

**Silinen/değişen mevcut parçalar:** `HowItWorksCard()` → `HowItWorksCard({ config })`. `TurnDraft`, `TurnValues`, `Turn`, `SourcesEvent`, `DoneEvent` alan alır ya da nullable olur (aşağıda). `AnswerMessage.isEmptyKnowledgeBase` `generationMode === 'RAG'` koşulunu alır. İsim değişikliği yok.

**Kart cevapları (2026-11-02, kullanıcı):** 1 A ("Bilgi kaynakları"; tek belgede ad · sürüm, çoklu "N belge", yoksa sarı "belge yok"), 2 C (15 sn sağlık kontrolü + rerank çağrısı sonucu). İkisi de planın önerdiği seçenek; plan değişmedi, B/C ve A/B dallarındaki alternatif notlar uygulanmaz.

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ortak kaynak `openapi.yaml` (`getLiveConfig`, `LiveConfigResponse`) ve `sse.md`. Kart soru 1'in cevabı sadece `liveConfigItems` (web) bilgi bankası değerini; soru 2'nin cevabı sadece `RerankerHealthChecker`'ın varlığını, `ContextRetriever` bildirimini ve web'in yoklama aralığını değiştirir. Sözleşme alanları iki cevapta da aynıdır.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`, `docs/api/sse.md`.
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/CLAUDE.md`. M0'ı gerçek modelle çalıştırır. Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md`. Yeni paket yok. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**` (MSW'ye `live-config` varsayılan handler'ı, SSE fixture'larına `generationMode`), `web/e2e/**`. Rapor `handoff/03-test-report.md`.
- **devops:** `deployment/docker-compose.yml` (`GENERATION_MODE`), `deployment/CLAUDE.md`, `ollama.md` (değişken tablosu, mod değiştirme). Rapor `handoff/04-deploy-report.md`.
- **qa:** M1-M6, kabul kriterleri, sözleşme uyumu. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** yok. retrieval-api koduna kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Üretim modu `assistant.*`: `rag`/`llm`, varsayılan `rag` | `assistant.generation.mode`, `AssistantProperties.Generation.configuredMode()` | B1 |
| K2a | Sadece LLM'de sistem talimatı lab'ın kısa metni; parça gönderilmez | `prompts/llm-only-system.st`, `PromptBuilder.buildLlmOnlyPrompt` | B2, B3 |
| K2b | Alıntı ayrıştırılmaz, "Rehber · s. N" düğmesi yok | `AnswerStreamer` (`citations` `[]`), web `AnswerMessage` (0 parça → `[n]` düz metin) | B4, F5 |
| K3a | Uç: dil modeli, embedding modeli ve boyutu | `LiveConfigService` (`chatModel`; `RetrievalClient.getWorkspace`) | B9, B14 |
| K3b | Uç: üretim modu, retrieval stratejisi, yeniden yazma | `LiveConfigResponse` | B9, B14 |
| K3c | Uç: bilgi bankası durumu (retrieval-api); gösterim kartta | `LiveConfigService` (`listSources` sayfaları), `liveConfigItems` (kart soru 1) | B10-B13, F1, M4 |
| K4 | Şerit metinleri ve renkleri lab'daki gibi | `liveConfigItems`, `LiveConfigStrip` (`data-tone`) | F1, F3, F4 |
| K5 | "reranker DOWN" ölçüme dayanır; yöntem kartta | `RerankerStatusTracker`, `RerankerHealthChecker`, `ContextRetriever` (kart soru 2) | B15-B17, F6, M3 |
| K6 | "Asistan nasıl cevaplıyor?" üç varyant | `howItWorksText`, `HowItWorksCard` | F2, F4 |
| K7 | Sadece LLM turu stratejisiz, parçasız kaydedilir | `TurnService.recordTurn`, V3 | B5, B6 |
| K8 | Ekrandan değiştirilemez; ortam değişkeni + yeniden başlatma | Uç sadece `GET`; web'de düğme yok; `GENERATION_MODE` | B14b, M2 |
| K9 | Rozette ipucu metni | `AppHeader` (mevcut, değişmez) | F9 |
| S1 | Varsayılan + tek rehber: şerit değerleri | K3, K4 | F3, M4 |
| S2 | Hiç belge yokken şerit bunu gösterir | `KnowledgeBaseStatus.EMPTY` | B11, F1, M4 |
| S3 | `mode=llm`: "Retrieval kapalı (Sadece LLM)", "Kartımı kaybettim" cevabında alıntı yok | K1, K2, K4 | B2, B4, F4, F5, M1, M2 |
| S4 | Reranker durdurulunca "reranker DOWN" | K5 | B15-B17, M3 |
| AC1 | İki mod gerçek modelle denenmiş, aynı soru karşılaştırılmış | backend-coder M0, qa M1 | M0, M1 |
| AC2 | Şerit değerleri yapılandırmadan; web'de sabit model adı yok | `liveConfigItems`, `howItWorksText` | F1, F2, M5 |
| AC3 | Testler ve build'ler geçer | tüm roller | raporlar |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | retrieval-api kapalıyken Önizleme açılır | Uç 200 döner; `knowledgeBase.status` `UNAVAILABLE`, embedding `null`. Şerit: "Embedding —", sarı "Bilgi kaynakları ulaşılamıyor". Diğer değerler görünür | B12, F1 |
| G2 | Sadece LLM'de takip sorusu | Yeniden yazma yapılmaz (`DISABLED`, model bir kez çağrılır); geçmiş prompt'a girer (lab `promptMessages`) | B19 |
| G3 | Sadece LLM cevabında `[1]` ya da "bulamadım" cümlesi geçer | `[1]` düz metin kalır, tıklanmaz, `citations` `[]`; `notFoundAnswer` `false` | B4, B7, F5 |
| G4 | Sadece LLM'de bilgi bankası boş | Model yine çağrılır; "Bilgi bankasında henüz belge yok" notu çıkmaz | B2, F5 |
| G5 | Yapılandırma ucu okunamaz (assistant-api kapalı/500) | Şerit yerine hata mesajı (`ErrorAlert`, `messageFor`); "Asistan nasıl cevaplıyor?" sadece ikinci satırla; soru gönderme etkilenmez | F7 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Uç `GET /api/assistant/live-config`**, paket `liveconfig`, adlar `LiveConfigController`/`LiveConfigService`/`LiveConfigResponse`, `operationId` `getLiveConfig` | Rozetin adı "Canlı yapılandırma"; `config` paketi Spring yapılandırmasıyla karışmasın | `/api/assistant/config`: `config/` paketiyle ad çakışması |
| 2 | **Uç retrieval-api hatasında da 200** döner; bilgi bankası `UNAVAILABLE`, embedding alanları `null` | Şerit tek bir servis kapalı diye kaybolmamalı (G1); Önizleme zaten akış sırasında `RETRIEVAL_UNAVAILABLE` gösterir | 503 dönmek: model ve mod bilgisi de kaybolur |
| 3 | **Aranabilir belge = `chunkCount != null`** (güncel sürümü olan); bütün sayfalar `size=100` ile okunur | retrieval-api araması sadece güncel sürümleri arar (`searchChunks`); işlenen/başarısız belge cevaba girmez | `totalElements`: işlenmekte olanları da sayar |
| 4 | **Uç sunucuda önbelleklenmez** | 15 sn yoklama + 2 hafif GET; yeni yüklenen belge hemen görünür | Önbellek: yükleme sonrası eski değer |
| 5 | **`LiveConfigResponse.retrievalStrategy` yapılandırılan değer** (`TOP_K`/`RERANK`), `LLM_ONLY`'de de | TASK-016 aynı ucu kullanacak; "kapalı" bilgisini `generationMode` taşır | `NONE` dönmek: yapılandırılan strateji kaybolur |
| 6 | **Sadece LLM'de yeniden yazma yapılmaz**, `queryRewriteStatus = DISABLED`; geçmiş prompt'a girer | Arama yok, yeniden yazmanın tek kullanıcısı arama; lab `promptMessages` (`t.hist`) ve lab akışı (`rw:false`) | Yeni durum değeri (`NOT_NEEDED`): CHECK + web enum genişler, anlamı `DISABLED` ile aynı |
| 7 | **Sadece LLM turunda `search_query`, `candidates`, `top_k`, `retrieval_latency_ms` NULL**; SSE'de de `null` | K7 "retrieval stratejisi olmadan"; lab `search_query NULL`, `retrieval 'NONE'` | Yapılandırma değerlerini yazmak: kayıt yapılmamış aramayı anlatır |
| 8 | **Sadece LLM'de `notFoundAnswer` hep `false`** | K2 alıntı ayrıştırmayı kapatıyor; talimatta "bulamadım" cümlesi yok, TASK-013 aktarma önerisi rehbere dayanır | Tespiti açık bırakmak: rehbere dayanmayan cevap "rehberde yok" sayılır |
| 9 | **`sources` olayı Sadece LLM'de de gönderilir** (parçasız, `generationMode` ile) | Olay sırası tek kalır (`token`* → `sources` → `done`); web turun modunu bilir | `sources` göndermemek: sıra kuralı ikiye ayrılır |
| 10 | **"Asistan nasıl cevaplıyor?" metni yapılandırmadan, ek'siz biçimde**: "…en iyi {topK} parça {chatModel} modeline gider." Sadece LLM varyantı lab metninin aynısı | AC2 sabit model adı yasak; lab'daki "5'i … qwen3.5:9b'ye" ekleri sayıya ve ada göre değişir, genel çözülemez | Lab metnini birebir koymak: sayı/model değişince Türkçe bozulur |
| 11 | **Sadece LLM cevabından sonra yan kartta "Asistan nasıl cevaplıyor?" (Sadece LLM varyantı) kalır** | Lab `last=t.mode==='rag'?t:null` | Parça kartını boş göstermek: lab'dan ayrışır |
| 12 | **Reranker DOWN iken "nasıl cevaplıyor" metni top-k varyantı** | Lab `rk==='down'` → "en yakın 5 parça" | — |
| 13 | **Reranker durumu bellekte, son ölçüm kazanır**; durum değişiminde tek log | Kart soru 2 (C): periyodik kontrol + gerçek çağrı; servis yeniden başlayınca `UNKNOWN` | Veritabanı: geçici durum, kalıcılık gereksiz |
| 14 | **`RerankerStatus { UP, DOWN, UNKNOWN, NOT_USED }`**; `LLM_ONLY` ya da `TOP_K`'da `NOT_USED`, ölçüm yapılmaz | Kullanılmayan servis için uyarı anlamsız; şerit zaten "top-5"/"kapalı" gösterir | Her zaman ölçmek: gereksiz istek ve yanlış sarı |
| 15 | **Web şerit verisi TanStack Query'de** (`useLiveConfigQuery`), `refetchInterval` 15 sn, her akış bitince (`done`/`error`/akış öncesi hata) invalidate | Reranker düşünce şerit soruyla hemen (C/A), sorusuz ~30 sn'de (C/B) güncellenir; yeni belge sayfaya dönünce görünür | Sadece sayfa açılışında okumak: senaryo 4 sayfa yenilenmeden görünmez |
| 16 | **`shared/liveConfig/`** (API, query, type'lar); şerit ve metin fonksiyonları `features/preview` | TASK-016 Değerlendirme aynı ucu okuyacak; feature'lar birbirini import etmez | `features/preview/api`: TASK-016'da taşıma gerekir |
| 17 | **Şeritte ton `data-tone` niteliğiyle** (`normal`/`warning`/`danger`), CSS bu nitelikten renk alır | Test ton'u CSS Module adına bağlı kalmadan doğrular | Sınıf adına bakmak: hash'li ad, kırılgan test |
| 18 | **`assistant.reranker.health-check-enabled`** (varsayılan `true`) | İşletimde kapatılabilir; `RerankClient`'ı sahtelemeyen `@SpringBootTest`'ler lokal 8081'e istek atmaz | Koşulsuz zamanlayıcı: testlerde gerçek adrese istek |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu (TASK-011), `Live config` tag'i.
- Yeni `GET /api/assistant/live-config` (`getLiveConfig`): 200 `LiveConfigResponse`, 500 `INTERNAL_ERROR`. Yeni `errorCode` yok.
- Yeni şemalar: `GenerationMode` (`RAG`, `LLM_ONLY`), `RerankerStatus` (`UP`, `DOWN`, `UNKNOWN`, `NOT_USED`), `KnowledgeBaseStatus` (`READY`, `EMPTY`, `UNAVAILABLE`), `KnowledgeBaseSource`, `KnowledgeBaseResponse`, `LiveConfigResponse`.
- `sendMessage` açıklaması: Sadece LLM modu ve kontrol sırası (`llm`: 400 → 404 → 409 → 200).
- `getWorkspace` ve `listSources` açıklaması: assistant-api de çağırır; aranabilir = `chunkCount != null`. Şema değişmedi.

`docs/api/sse.md`:
- Durum satırı; "Sıra": bilgi bankası boş kuralı sadece `RAG`; Sadece LLM maddesi.
- `sources`: yeni `generationMode` (ilk alan); `candidates`, `topK` integer | null; `searchQuery` string | null; `queryRewriteStatus` `LLM_ONLY`'de `DISABLED`; `contextChunks`/`citations` `LLM_ONLY`'de `[]`. `RetrievalStrategy` yeni `NONE`. `QueryRewriteStatus.DISABLED` tanımı genişledi.
- `done`: `retrievalLatencyMs` integer | null; `rerankLatencyMs` `LLM_ONLY`'de null; `notFoundAnswer` `LLM_ONLY`'de false.
- Örnekler `generationMode` aldı; Sadece LLM örneği; isim tablosuna `GenerationMode`, `RetrievalStrategy`.

`websocket.md`: yok.

## Backend planı
Ek skill'ler: `rag-pipeline`, `sse-streaming`, `java-concurrency` (tracker, zamanlayıcı), `spring-transactions`, `jpa-patterns`, `flyway-migrations`, `spring-boot-openapi-documentation` (sözleşme uyumu; springdoc yok), `springboot-security` (sistem talimatı veri ayrımı).

**Yeni dependency yok.** `@EnableScheduling` Spring'in kendisinde.

### Migration `V3__add_llm_only_generation_mode.sql`
`conversation_turns`:
- `generation_mode` CHECK → `IN ('RAG', 'LLM_ONLY')`; `retrieval_strategy` CHECK → `IN ('TOP_K', 'RERANK', 'RERANK_FALLBACK', 'NONE')` (V1'in satır içi CHECK'leri; Postgres adları `conversation_turns_generation_mode_check`, `conversation_turns_retrieval_strategy_check`, coder `\d` ile doğrular, drop + yeniden ekler).
- `search_query`, `candidates`, `top_k` → `DROP NOT NULL`.
- `ck_conversation_turns_mode_strategy CHECK ((generation_mode = 'LLM_ONLY') = (retrieval_strategy = 'NONE'))`.
- `ck_conversation_turns_none_has_no_retrieval CHECK (retrieval_strategy <> 'NONE' OR (search_query IS NULL AND candidates IS NULL AND top_k IS NULL AND retrieval_latency_ms IS NULL AND rerank_latency_ms IS NULL))`.
- `ck_conversation_turns_retrieval_has_values CHECK (retrieval_strategy = 'NONE' OR (search_query IS NOT NULL AND candidates IS NOT NULL AND top_k IS NOT NULL))`.
Eski satırlar hepsi `RAG` ve dolu; veri güncellemesi yok.

### Properties (ASCII)
```
assistant.generation.mode=${GENERATION_MODE:rag}
assistant.reranker.health-check-interval=15s
assistant.reranker.health-check-enabled=true
```
`AssistantProperties.Generation`'a `@NotNull @Pattern(regexp = "rag|llm", message = "must be one of rag, llm") String mode` + `GenerationMode configuredMode()` (`llm` → `LLM_ONLY`). `Reranker`'a `@NotNull Duration healthCheckInterval`, `boolean healthCheckEnabled`. Javadoc'lar güncellenir. `assistant-api/CLAUDE.md`: ayar listesi, paket yapısı (`liveconfig/`), "Üretim modu seçimi henüz yok" satırı çıkar, duman testi (`GENERATION_MODE=llm`, `curl localhost:8090/api/assistant/live-config`).

### Prompt `prompts/llm-only-system.st` (lab metni, birebir; değişken yok)
```
Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın.
```

### Sınıflar
**conversation/domain/entity/**
- `GenerationMode { RAG, LLM_ONLY }` (Javadoc güncellenir). `RetrievalStrategy`'ye `NONE`.
- `Turn`: `searchQuery` `@Nullable String`, `candidates` ve `topK` `@Nullable Integer` (getter'lar da). `TurnValues`: aynı üç alan nullable; alan sırası değişmez.

**conversation/domain/event/**
- `SourcesEvent(GenerationMode generationMode, RetrievalStrategy retrievalStrategy, @Nullable Integer candidates, @Nullable Integer topK, @Nullable String searchQuery, QueryRewriteStatus queryRewriteStatus, List<ContextChunk> contextChunks, List<Integer> citations)`.
- `DoneEvent`: `retrievalLatencyMs` → `@Nullable Long`; diğer alanlar ve sıra aynı.

**conversation/service/**
- `TurnDraft(long conversationId, GenerationMode generationMode, String question, @Nullable String searchQuery, QueryRewriteStatus queryRewriteStatus, @Nullable Long queryRewriteLatencyMs, int historyTurns, String chatModel, Instant receivedAt, long startNanos, @Nullable RetrievedContext context)`.
- `AnswerService.sendMessage`: kilit ve geçmiş okuma aynı sırada (TASK-010 Kararlar #9). Sonra `properties.generation().configuredMode() == LLM_ONLY` ise: `queryRewriter` ve `contextRetriever` çağrılmaz; `draft(LLM_ONLY, searchQuery null, DISABLED, latency null, context null)`; `answerStreamer.streamModelAnswer(draft, promptBuilder.buildLlmOnlyPrompt(question, history), emitter)`. Log `INFO Generation without retrieval conversationId={} mode=LLM_ONLY historyTurns={} questionLength={}`. `RAG` yolu değişmez (draft `RAG` alır).
- `AnswerStreamer.StreamingTurn`: `context` `@Nullable`; `RAG` turunda boş olamaz (mevcut `requireNonNull` sadece `RAG` için). `LLM_ONLY`'de: `citations` `List.of()` (parser çağrılmaz), `notFoundAnswer` `false` (detector çağrılmaz); `sourcesEvent` → `(LLM_ONLY, NONE, null, null, null, DISABLED, [], [])`; `doneEvent` → `retrievalLatencyMs` ve `rerankLatencyMs` `null`. `RAG`'de `sourcesEvent` `generationMode` `RAG` ile başlar.
- `TurnService.recordTurn`: `generationMode` draft'tan; `LLM_ONLY` → `retrievalStrategy NONE`, `candidates`/`topK`/`retrievalLatencyMs`/`rerankLatencyMs` `null`, parça satırı yok. `logRecordedTurn`: `context` `null` ise strateji `NONE`; mesaj `Turn completed conversationId={} turnId={} mode={} strategy={} …` (`requireNonNull(context)` kaldırılır; yoksa Sadece LLM turu loglanırken düşer).

**generation/**
- `PromptBuilder`: constructor `@Value("classpath:prompts/llm-only-system.st") Resource llmOnlySystemTemplate` alır. Yeni `Prompt buildLlmOnlyPrompt(String question, List<HistoryTurn> history)` → `SystemMessage(llmOnlySystemText)`, her geçmiş tur `UserMessage(question)` + `AssistantMessage(answer)`, son `UserMessage(question)` (şablon yok, soru olduğu gibi). `buildPrompt` değişmez.

**retrieval/client/**
- `RetrievalClient`: yeni `WorkspaceResponse getWorkspace()` (`GET /api/workspaces/{workspaceId}`) ve `SourcePageResponse listSources(int page, int size)` (`GET /api/workspaces/{workspaceId}/sources?page=&size=`). Hata davranışı `search` ile aynı: her hata `ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", …)`, aynı `WARN Retrieval unavailable …` logu.
- Yeni record'lar (retrieval-api şemasının ihtiyaç duyulan alanları): `WorkspaceResponse(long workspaceId, String name, String embeddingModel, int embeddingDimensions)`, `SourcePageResponse(List<SourceResponse> content, int page, int size, long totalElements, int totalPages)`, `SourceResponse(long sourceId, String name, String version, @Nullable Integer chunkCount)`.
- `RerankClient`: yeni `boolean isHealthy()` → `GET /health`; 2xx → `true`; her `RestClientException` (bağlantı, zaman aşımı, non-2xx) → `false`. Exception fırlatmaz.

**retrieval/service/**
- `RerankerStatus { UP, DOWN, UNKNOWN, NOT_USED }`.
- `RerankerStatusTracker` (`@Component`; `AssistantProperties`): `boolean isRerankerUsed()` (`RAG` ve `RERANK`), `RerankerStatus currentStatus()` (kullanılmıyorsa `NOT_USED`, ölçüm yoksa `UNKNOWN`), `void recordAvailable()`, `void recordUnavailable()` (kullanılmıyorsa yok sayılır). `AtomicReference`; durum değişince `INFO Reranker status changed from={} to={}` (her ölçümde değil).
- `RerankerHealthChecker` (`@Component`, `@ConditionalOnProperty(name = "assistant.reranker.health-check-enabled", havingValue = "true")`): `@Scheduled(fixedDelayString = "${assistant.reranker.health-check-interval}")` `void checkRerankerHealth()` → `isRerankerUsed()` değilse hiçbir şey yapmaz; yoksa `rerankClient.isHealthy()` → `recordAvailable`/`recordUnavailable`. İlk çalışma açılışta (`initialDelay` yok).
- `config/SchedulingConfig` (`@Configuration`, `@EnableScheduling`).
- `ContextRetriever.rerank`: başarıda `tracker.recordAvailable()`, `ServiceUnavailableException`'da `tracker.recordUnavailable()`.
- **Kart soru 2 cevabına göre:** A → `RerankerHealthChecker`, `SchedulingConfig`, `isHealthy`, iki `health-check-*` anahtarı açılmaz; B → `ContextRetriever` tracker'a bildirmez; C → yukarıdaki gibi. `openapi.yaml` `RerankerStatus` açıklaması planner tarafından cevaba göre güncellenir.

**liveconfig/** (yeni; her pakete `package-info.java`)
- `domain/dto/KnowledgeBaseStatus { READY, EMPTY, UNAVAILABLE }`, `KnowledgeBaseSource(long sourceId, String name, String version)`, `KnowledgeBaseResponse(KnowledgeBaseStatus status, @Nullable Integer searchableSourceCount, List<KnowledgeBaseSource> sources)`, `LiveConfigResponse(String chatModel, @Nullable String embeddingModel, @Nullable Integer embeddingDimensions, GenerationMode generationMode, RetrievalStrategy retrievalStrategy, int candidates, int topK, boolean queryRewriteEnabled, RerankerStatus rerankerStatus, KnowledgeBaseResponse knowledgeBase)`. JSON alan sırası `openapi.yaml` ile aynı.
- `service/LiveConfigService` (`AssistantProperties`, `@Value("${spring.ai.ollama.chat.model}") String chatModelName`, `RetrievalClient`, `RerankerStatusTracker`): `LiveConfigResponse getLiveConfig()`. `getWorkspace()` + sayfa 0'dan `listSources(page, 100)` (`page + 1 < totalPages` oldukça sonraki sayfa); `chunkCount != null` olanlar `sourceId` artan sırada. `ServiceUnavailableException` yakalanır → `UNAVAILABLE`, `null` sayı, `[]`, embedding `null`. Transaction yok.
- `controller/LiveConfigController`: `@GetMapping("/api/assistant/live-config") LiveConfigResponse getLiveConfig()`. Başka method yok (POST/PUT → framework 405 `METHOD_NOT_ALLOWED`).

### M0 (backend-coder, gerçek model)
`GENERATION_MODE=llm` ile başlat: `curl …/live-config` → `generationMode` `LLM_ONLY`, `rerankerStatus` `NOT_USED`; "Kartımı kaybettim, ne yapmalıyım?" akışı → `sources.retrievalStrategy` `NONE`, `citations` `[]`; retrieval-api kapalıyken de cevap gelir. Varsayılanla aynı soru → `RERANK`, alıntılı. Reranker kapat → en geç 15 sn'de `rerankerStatus` `DOWN`. Sonuçlar raporda.

## Frontend planı
Ek skill'ler: `react-state-management` (yoklama, invalidate), `frontend-a11y`, `frontend-design` (lab `.cfg` stilleri).

**Yeni paket yok.**

**shared/liveConfig/**
- `types.ts`: `GENERATION_MODES = ['RAG', 'LLM_ONLY'] as const` + `GenerationMode`; `RERANKER_STATUSES = ['UP', 'DOWN', 'UNKNOWN', 'NOT_USED'] as const` + `RerankerStatus`; `KNOWLEDGE_BASE_STATUSES = ['READY', 'EMPTY', 'UNAVAILABLE'] as const` + `KnowledgeBaseStatus`; `KnowledgeBaseSource`, `KnowledgeBaseResponse`, `LiveConfigResponse` (`retrievalStrategy: 'TOP_K' | 'RERANK'`) — `openapi.yaml` ile birebir.
- `liveConfigApi.ts`: `getLiveConfig(): Promise<LiveConfigResponse>` → `request('/api/assistant/live-config')`.
- `liveConfigQueries.ts`: `liveConfigKeys = { all: ['liveConfig'] as const }`, `useLiveConfigQuery()` (`refetchInterval: LIVE_CONFIG_POLL_INTERVAL_MS`).
- `liveConfigRules.ts`: `LIVE_CONFIG_POLL_INTERVAL_MS = 15_000` (kart soru 2 = A ise `refetchInterval` verilmez).

**features/preview/**
- `model/types.ts`: `RETRIEVAL_STRATEGIES`'e `'NONE'`; `SourcesEvent`'e ilk alan `generationMode: GenerationMode` (shared'dan import), `candidates: number | null`, `topK: number | null`, `searchQuery: string | null`; `DoneEvent.retrievalLatencyMs: number | null`.
- `model/answerStreamEvents.ts`: `isSourcesEvent` `generationMode`'u bilinen değer, `candidates`/`topK` nullable integer, `searchQuery` nullable string kontrol eder; `isDoneEvent` `retrievalLatencyMs` nullable.
- `model/strategyLabels.ts`: `strategyLabel` `'NONE'` → `RETRIEVAL_OFF_LABEL = 'kapalı (Sadece LLM)'`; `TOP_K`/`RERANK` `topK`/`candidates` null değilse sayılı (null olmaz ama tip daraltılır); `queryRewriteLabel` `REWRITTEN` + `searchQuery === null` → `null`.
- `model/latencyRows.ts`: `retrievalLatencyMs === null` → "retrieval-api araması" satırı yok.
- `model/liveConfigItems.ts`: `type LiveConfigTone = 'normal' | 'warning' | 'danger'`, `type LiveConfigItem = { label: string; value: string; tone: LiveConfigTone }`, `KNOWLEDGE_BASE_NAME_SHORT_LENGTH = 40`, `liveConfigItems(config: LiveConfigResponse): LiveConfigItem[]` → beş öğe, sırayla:
  1. `Model` / `chatModel` / normal
  2. `Embedding` / `embeddingModel ?? '—'` / normal
  3. `Retrieval` / `LLM_ONLY` → `kapalı (Sadece LLM)` danger; `RERANK` + `DOWN` → `reranker DOWN` warning; `RERANK` → `rerank {candidates}→{topK}` normal; `TOP_K` → `top-{topK}` normal
  4. `Soru yeniden yazma` / `açık` normal | `kapalı` warning
  5. Bilgi bankası (kart soru 1; öneri A): etiket `Bilgi kaynakları`; `READY` + 1 → `{shortenText(name, 40)} · {version}` normal; `READY` + ≥2 → `{n} belge` normal; `EMPTY` → `belge yok` warning; `UNAVAILABLE` → `ulaşılamıyor` warning. (B seçilirse etiket `Rehber`, tek belgede sadece `{version}`; C seçilirse tek belgede de `1 belge`.)
- `model/howItWorks.ts`: `howItWorksText(config: LiveConfigResponse): string` (Kararlar #10-12). `emb` = `embeddingModel === null ? 'pgvector' : '{embeddingModel} + pgvector'`:
  - `LLM_ONLY`: `Soru önce rehberde aranır ({emb}), bu modda arama kapalı; soru doğrudan modele gider.`
  - `RERANK` ve `rerankerStatus !== 'DOWN'`: `Soru önce rehberde aranır ({emb}), {candidates} aday reranker ile puanlanıp en iyi {topK} parça {chatModel} modeline gider. Cevaptaki numaralar rehberin parçalarını gösterir.`
  - `TOP_K` ya da `RERANK` + `DOWN`: `Soru önce rehberde aranır ({emb}), en yakın {topK} parça {chatModel} modeline gider. Cevaptaki numaralar rehberin parçalarını gösterir.`
- `components/LiveConfigStrip.tsx` (+ `.module.css`, lab `.cfg`, `.side2>.cfg span`, `.w` → `[data-tone='warning']`, `.r` → `[data-tone='danger']`; token'lar `--color-line`, `--color-accent-soft`, `--color-red-soft`): props `{ config: LiveConfigResponse }`; `<div role="group" aria-label="Canlı yapılandırma">`, her öğe `<span data-tone={tone}>{label} <b>{value}</b></span>`.
- `components/HowItWorksCard.tsx`: props `{ config: LiveConfigResponse | undefined }`; ilk alt satır `config` varsa `howItWorksText(config)`, yoksa yok; ikinci satır aynı.
- `pages/PreviewPage.tsx`: `useLiveConfigQuery()` bir kez; yan sütunun başında yüklenince `LiveConfigStrip`, hata olursa `ErrorAlert errorCode={…}` (ApiError değilse `UNEXPECTED_ERROR`), yüklenirken hiçbir şey. Kart seçimi: son tur `done` ve `sources.generationMode === 'RAG'` → `ContextChunksCard`; diğer her durumda `HowItWorksCard config={data}`.
- `components/AnswerMessage.tsx`: `isEmptyKnowledgeBase` → `sources.generationMode === 'RAG' && sources.contextChunks.length === 0`.
- `hooks/useAnswerStream.ts`: akış bitince (`done`, `error` olayı ya da akış öncesi `ApiError`; abort hariç) `queryClient.invalidateQueries({ queryKey: liveConfigKeys.all })`.
- `AppHeader`: değişmez.
- `errorMessages.ts`: yeni kod yok.

## Test senaryoları
Backend sahteleri TASK-008/010'daki gibi (sahte `ChatModel`, `RetrievalClient`, `RerankClient`, `RecordingSseEmitter`); `RerankClient`'ı sahtelemeyen `@SpringBootTest`'ler `assistant.reranker.health-check-enabled=false` alır. Web: `src/test/handlers.ts`'e `GET /api/assistant/live-config` varsayılan handler'ı (`LiveConfigResponse` örneğiyle, tam şema); bütün SSE `sources` fixture'ları `generationMode: 'RAG'` alır (eksik alan olayı düşürür).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Mod ayarı | unit `AssistantPropertiesTest` | `givenDefaults_whenBound_thenRagMode` / `givenModeLlm_whenBound_thenLlmOnly` / `givenModeFoo_whenBound_thenContextFails` | varsayılan `RAG`; `llm` → `LLM_ONLY`; `foo` açılışı düşürür; `health-check-interval` 15s |
| B2 | LLM: retrieval yok | unit `AnswerServiceTest` | `givenLlmOnlyMode_whenSendMessage_thenNoRewriteNoRetrieval` | `queryRewriter`, `contextRetriever` etkileşimsiz; `streamModelAnswer` 1 kez, draft `LLM_ONLY`, `searchQuery` null, `DISABLED` |
| B3 | LLM prompt | unit `PromptBuilderTest` | `givenHistory_whenBuildLlmOnlyPrompt_thenShortSystemHistoryAndQuestion` | 4 mesaj: System = `Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın.` (literal), User q1, Assistant a1, User `Kartımı kaybettim, ne yapmalıyım?`; hiçbir mesajda `<parcalar>` yok |
| B4 | LLM SSE | integration `SendMessageIntegrationTest` (`mode=llm`) | `givenLlmOnlyMode_whenSendMessage_thenSourcesWithoutChunksAndCitations` | sahte model `Kartını kapat [1].` → `sources` = `{"generationMode":"LLM_ONLY","retrievalStrategy":"NONE","candidates":null,"topK":null,"searchQuery":null,"queryRewriteStatus":"DISABLED","contextChunks":[],"citations":[]}`; `done.retrievalLatencyMs` null, `rerankLatencyMs` null; sahte `RetrievalClient` etkileşimsiz |
| B5 | LLM tur kaydı | integration `TurnServiceIntegrationTest` | `givenLlmOnlyDraft_whenRecordTurn_thenNoneStrategyAndNoChunks` | `generation_mode` LLM_ONLY, `retrieval_strategy` NONE, `search_query`/`candidates`/`top_k`/`retrieval_latency_ms` null, `cited` `{}`, `turn_chunks` 0 satır, `query_rewrite_status` DISABLED |
| B6 | V3 kısıtları | integration (Flyway) | `givenMigrations_whenInconsistentModeAndStrategy_thenCheckViolation` | `LLM_ONLY`+`RERANK`, `RAG`+`NONE`, `NONE`+`candidates 20`, `RAG`+`search_query null` insert'leri ihlal; `LLM_ONLY`+`NONE`+null'lar geçer |
| B7 | LLM "bulamadım" | unit `AnswerStreamerTest` | `givenLlmOnlyAnswerWithNotFoundSentence_whenCompleted_thenNotFoundFalse` | cevap `Bu konuda rehberimizde bilgi bulamadım.` → `done.notFoundAnswer` false |
| B8 | RAG değişmedi | integration `SendMessageIntegrationTest` | `givenRagMode_whenSendMessage_thenSourcesStartWithRagMode` | `sources.generationMode` RAG, `retrievalStrategy` RERANK, `candidates` 20, `topK` 5 |
| B9 | Uç değerleri | unit `LiveConfigServiceTest` | `givenDefaultsAndOneSearchableSource_whenGetLiveConfig_thenConfiguredValues` | `chatModel` qwen3.5:9b, `embeddingModel` bge-m3, `embeddingDimensions` 1024, RAG, RERANK, 20, 5, `queryRewriteEnabled` true, KB `READY`, 1, `[{10001, "Raşit Bankası Ürün ve Hizmet Rehberi", "2026.10"}]` |
| B10 | Sadece aranabilir | unit | `givenSourceWithoutCurrentVersion_whenGetLiveConfig_thenNotCounted` | 3 kaynak (10001, 10002 `chunkCount` null, 10003) → sayı 2, kaynaklar 10001, 10003 |
| B11 | Belge yok | unit | `givenNoSources_whenGetLiveConfig_thenEmpty` | `EMPTY`, 0, `[]`; embedding bge-m3 |
| B12 | retrieval-api kapalı | unit | `givenRetrievalUnavailable_whenGetLiveConfig_thenUnavailableWithoutFailing` | `getWorkspace` 503 → `UNAVAILABLE`, sayı null, `[]`, embedding null/null; `chatModel` dolu |
| B13 | Sayfalama | unit | `givenTwoPages_whenGetLiveConfig_thenReadsBothWithSize100` | `totalPages` 2 → `listSources(0,100)` ve `(1,100)`; sayı iki sayfanın toplamı |
| B14 | Uç | controller `LiveConfigControllerTest` | `givenLiveConfig_whenGet_thenJsonFields` / `givenLiveConfig_whenPost_thenMethodNotAllowed` | 200, B9 değerleri `openapi` alan adlarıyla (`$.knowledgeBase.sources[0].version` = `2026.10`); POST → 405 `METHOD_NOT_ALLOWED` |
| B15 | Tracker | unit `RerankerStatusTrackerTest` (`@ParameterizedTest`) | `givenMeasurements_whenCurrentStatus_thenLatestWins` / `givenTopKOrLlmOnly_whenCurrentStatus_thenNotUsed` | başta UNKNOWN; unavailable → DOWN; available → UP; TOP_K ya da LLM_ONLY'de kayıt sonrası da NOT_USED |
| B16 | Rerank çağrısı bildirir | unit `ContextRetrieverTest` | `givenRerankerFails_whenRetrieveContext_thenTrackerDown` / `givenRerankSucceeds_whenRetrieveContext_thenTrackerUp` | `RERANK_FALLBACK` + `currentStatus()` DOWN; başarıda UP |
| B17 | Sağlık kontrolü | unit `RerankerHealthCheckerTest` + `RerankClientTest` (`MockRestServiceServer`) | `givenHealthy_whenCheck_thenUp` / `givenUnhealthy_whenCheck_thenDown` / `givenTopK_whenCheck_thenNoRequest` / `givenHealth503OrConnectionError_whenIsHealthy_thenFalse` | `GET /health` 200 → UP; 503 → DOWN; TOP_K → `isHealthy` çağrılmaz |
| B18 | Transaction dışında | unit `LiveConfigServiceTest` | `givenGetLiveConfig_whenRetrievalCalled_thenNoTransactionActive` | sahte istemcide `isActualTransactionActive()` false |
| B19 | LLM takip sorusu | unit `AnswerServiceTest` (`ArgumentCaptor`) | `givenLlmOnlyFollowUp_whenSendMessage_thenHistoryInPromptAndNoRewrite` | 1 geçmiş tur; prompt System, User q1, Assistant a1, User q2; `queryRewriter` etkileşimsiz |
| F1 | Şerit öğeleri | unit `liveConfigItems.test.ts` (`it.each`) | `givenConfigs_whenLiveConfigItems_thenLabLabelsAndTones` | varsayılan → `Model qwen3.5:9b`, `Embedding bge-m3`, `Retrieval rerank 20→5`, `Soru yeniden yazma açık`, `Bilgi kaynakları Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10` hepsi normal; LLM_ONLY → `kapalı (Sadece LLM)` danger; RERANK+DOWN → `reranker DOWN` warning; TOP_K → `top-5`; rewrite false → `kapalı` warning; 3 belge → `3 belge`; EMPTY → `belge yok` warning; UNAVAILABLE → `ulaşılamıyor` warning + `Embedding —`; 45 karakterlik ad → 39 karakter + `…` |
| F2 | Nasıl cevaplıyor | unit `howItWorks.test.ts` | `givenModes_whenHowItWorksText_thenThreeVariantsFromConfig` | RERANK → `Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5 parça qwen3.5:9b modeline gider. Cevaptaki numaralar rehberin parçalarını gösterir.`; TOP_K ve RERANK+DOWN → `…, en yakın 5 parça qwen3.5:9b modeline gider. …`; LLM_ONLY → `Soru önce rehberde aranır (bge-m3 + pgvector), bu modda arama kapalı; soru doğrudan modele gider.`; `candidates 30, topK 3, chatModel 'llama3:8b'` → `30 aday … en iyi 3 parça llama3:8b modeline` |
| F3 | Şerit sayfada | component `PreviewPage.test.tsx` | `givenLiveConfig_whenPageOpens_thenStripShowsConfiguredValues` | `group` "Canlı yapılandırma" içinde F1 varsayılan beş metin |
| F4 | LLM ekranı | component | `givenLlmOnlyConfig_whenPageOpens_thenRetrievalOffDangerAndLlmHowItWorks` | "Retrieval kapalı (Sadece LLM)" `data-tone="danger"`; kartta LLM varyantı |
| F5 | LLM cevabı | component | `givenLlmOnlyAnswerWithCitationText_whenDone_thenPlainTextAndHowItWorksStays` | cevap `Kartını kapat [1].` metni görünür; "Kaynak 1" düğmesi yok, "Rehber · s." düğmesi yok, "Bilgi bankasında henüz belge yok." yok; yan kart başlığı "Asistan nasıl cevaplıyor?" |
| F6 | Cevaptan sonra yenileme | component | `givenAnswerDone_whenDone_thenLiveConfigRefetched` | MSW live-config istek sayısı açılışta 1, `done` sonrası 2; ikinci yanıt `DOWN` → "Retrieval reranker DOWN" |
| F7 | Uç hatası | component | `givenLiveConfigFails_whenPageOpens_thenAlertAndChatWorks` | 500 `INTERNAL_ERROR` → `alert` "Beklenmeyen bir hata oluştu. Tekrar dene."; kartta sadece "Bir soru sor; …"; soru gönderilir, cevap akar |
| F8 | Olay doğrulama | unit `answerStreamEvents.test.ts` | `givenLlmOnlySources_whenToAnswerStreamEvent_thenAccepted` / `givenUnknownGenerationMode_whenToAnswerStreamEvent_thenNull` | sse.md LLM örneği geçer; `generationMode: "FOO"` → null; `done.retrievalLatencyMs: null` geçer |
| F9 | Rozet | component `AdminLayout.test.tsx` | `givenAnyPage_whenRendered_thenLiveConfigBadgeHasHint` | "Canlı yapılandırma" `title` = `Önizleme, canlı asistanla aynı yapılandırmayı kullanır` |
| F10 | Erişilebilirlik | component `LiveConfigStrip.test.tsx` | `givenStrip_whenRendered_thenNoAxeViolations` | `axe` ihlal yok |
| F11 | Süre satırı | unit `previewModel.test.ts` | `givenNullRetrievalLatency_whenLatencyRows_thenNoSearchRow` | `retrievalLatencyMs` null → "retrieval-api araması" satırı yok |
| E1 | Gerçek şerit | e2e `web/e2e/preview.spec.ts` | `givenRunningSystem_whenPreviewOpens_thenStripMatchesLiveConfig` | `request.get('/api/assistant/live-config')` cevabındaki `chatModel` şeritte `Model {chatModel}` olarak görünür |

**Elle çalıştırılanlar:**
- **M0 (backend-coder):** bkz. Backend planı.
- **M1 (qa, AC1):** aynı sorular iki modda (yeni konuşma, `GENERATION_MODE=llm` ve varsayılan): "Kartımı kaybettim, ne yapmalıyım?", "Gold kartın yıllık aidatı ne kadar?", "FAST ile en fazla ne kadar gönderebilirim?". Tablo: cevap, alıntı sayısı, rehberle tutarlı mı, süre, `conversation_turns` satırı (mode, strategy, search_query).
- **M2 (devops/qa):** `GENERATION_MODE=llm docker compose -f deployment/docker-compose.yml up -d assistant-api` → şerit kırmızı "Retrieval kapalı (Sadece LLM)", kart LLM varyantı; değişkensiz aynı komutla geri.
- **M3 (qa, S4):** reranker'ı durdur, soru sorma → şerit en geç ~30 sn'de sarı "Retrieval reranker DOWN"; yeniden aç → "rerank 20→5". Soru sorulunca anında değişimi de dene.
- **M4 (qa, S1-S2):** belge yokken sarı "belge yok"; rehber yüklenince kart soru 1'deki biçim; ikinci belgede "2 belge".
- **M5 (qa, AC2):** `grep -rnE "qwen|bge-|llama|nomic" web/src --include=*.ts --include=*.tsx | grep -v -E "\.test\.|src/test/"` → boş.
- **M6 (qa):** `curl localhost:8090/api/assistant/live-config` alanları `openapi.yaml` `LiveConfigResponse` ile; Sadece LLM akışı `sse.md` örneğiyle karşılaştırılır.

## Deployment planı
- `deployment/docker-compose.yml` assistant-api ortamına `GENERATION_MODE: ${GENERATION_MODE:-rag}`.
- `deployment/CLAUDE.md` ve `ollama.md` değişken tablosu: `GENERATION_MODE` ve M2 komutu; reranker sağlık kontrolünün 15 sn'de bir `/health`'e gittiği notu.
- nginx değişmez (`/api/assistant/` zaten yönlendiriliyor). Yeni lokal model yok.

## Açık sorular ve riskler
1. **Kart soruları:** 1 → `liveConfigItems` 5. öğe (F1, M4); 2 → `RerankerHealthChecker`, `ContextRetriever` bildirimi, `LIVE_CONFIG_POLL_INTERVAL_MS` ve `RerankerStatus` açıklaması (B15-B17, F6, M3).
2. **Risk – lab metni sapması:** "Asistan nasıl cevaplıyor?" rerank ve top-5 varyantlarında lab'ın "5'i … qwen3.5:9b'ye" ekleri "5 parça … qwen3.5:9b modeline" olur (Kararlar #10; kartta yazılı).
3. **Risk – yavaş retrieval-api:** retrieval-api asılı kalırsa uç 15 sn'ye kadar bekler (okuma zaman aşımı); şerit o sürede görünmez. Bağlantı reddi anında `UNAVAILABLE` döner.
4. **Kapsam dışı not:** Sadece LLM varyantının lab metni ("Soru önce rehberde aranır …, bu modda arama kapalı") kendi içinde çelişkili; ürün hedefi gereği aynen alındı. Kullanıcı isterse ayrı bir düzeltme olarak değiştirilebilir.

STATUS: DONE
