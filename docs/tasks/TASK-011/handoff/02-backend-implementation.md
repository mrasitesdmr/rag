TASK: TASK-011
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline`, `java-concurrency` (tracker, zamanlayıcı), `flyway-migrations` (V3). Kart cevapları: soru 1 = A (sadece web'i etkiler), soru 2 = C (15 sn sağlık kontrolü + rerank çağrısı sonucu) → planın C dalı uygulandı.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| K1 üretim modu | `config/AssistantProperties` (`Generation.mode` `@Pattern rag\|llm`, `configuredMode()`), `application.properties` (`assistant.generation.mode=${GENERATION_MODE:rag}`) | `foo` → bağlama doğrulaması açılışı düşürür |
| K2a Sadece LLM prompt'u | `prompts/llm-only-system.st` (lab metni birebir), `generation/PromptBuilder` (`buildLlmOnlyPrompt`; yeni constructor parametresi `llm-only-system.st`) | Dosya okunup `strip()` edilir; System + geçmiş (User/Assistant) + soru (şablonsuz). `buildPrompt` davranışı aynı (geçmiş ekleme ortak `addHistory`'ye taşındı) |
| K2b alıntı yok, bulamadım yok | `conversation/service/AnswerStreamer` | `context == null` (yalnız `LLM_ONLY`) → `citations` `[]`, `notFoundAnswer` `false`; parser/detector çağrılmaz. `RAG` turunda `context` hâlâ zorunlu (`requireNonNull`) |
| K7 tur kaydı | `conversation/service/TurnService`, `TurnDraft` (+`generationMode`, `searchQuery` nullable), `domain/entity/Turn`, `TurnValues` (`searchQuery`, `candidates`, `topK` nullable), `GenerationMode` (+`LLM_ONLY`), `RetrievalStrategy` (+`NONE`), `db/migration/V3__add_llm_only_generation_mode.sql` | `LLM_ONLY` → `NONE`, arama alanları/süreler `NULL`, parça satırı yok. Log: `Turn completed … mode={} strategy={} …` (`requireNonNull(context)` kaldırıldı) |
| Kararlar #6 akış | `conversation/service/AnswerService` | Kilit + geçmiş okuma aynı sırada; sonra `LLM_ONLY` → `answerWithoutRetrieval` (yeniden yazma, retrieval, rerank yok; draft `DISABLED`, `searchQuery` null), `RAG` → `answerWithRetrieval` (eski yol, draft `RAG`). Log `Generation without retrieval conversationId={} mode=LLM_ONLY historyTurns={} questionLength={}` |
| SSE sözleşmesi | `domain/event/SourcesEvent` (ilk alan `generationMode`; `candidates`, `topK`, `searchQuery` nullable), `DoneEvent` (`retrievalLatencyMs` `@Nullable Long`) | `LLM_ONLY` → `(LLM_ONLY, NONE, null, null, null, DISABLED, [], [])`; `done.retrievalLatencyMs`/`rerankLatencyMs` null |
| K3 uç | `liveconfig/controller/LiveConfigController` (`GET /api/assistant/live-config`), `liveconfig/service/LiveConfigService`, `liveconfig/domain/dto/LiveConfigResponse`, `KnowledgeBaseResponse` (`of`, `unavailable`), `KnowledgeBaseSource`, `KnowledgeBaseStatus` | Alan sırası `openapi.yaml` ile aynı. Transaction yok, önbellek yok. `getWorkspace()` + `listSources(page, 100)` (sayfa < `totalPages` oldukça); `chunkCount != null` → aranabilir, `sourceId` artan. Herhangi bir `ServiceUnavailableException` → `UNAVAILABLE`, sayı null, `[]`, embedding null/null (çalışma alanı okunup kaynak listesi düşerse de embedding null) |
| K3 istemci | `retrieval/client/RetrievalClient` (`getWorkspace`, `listSources`; ortak `call(...)` hata eşlemesi), yeni record'lar `WorkspaceResponse`, `SourcePageResponse`, `SourceResponse` | Hata davranışı `search` ile aynı: her hata `RETRIEVAL_UNAVAILABLE` + aynı `WARN Retrieval unavailable …` logu; boş gövde de `EmptyBody` |
| K5 reranker ölçümü (kart C) | `retrieval/client/RerankClient.isHealthy()` (`GET /health`, her `RestClientException` → `false`), `retrieval/service/RerankerStatus`, `RerankerStatusTracker` (`AtomicReference`, durum değişince tek `INFO` log), `RerankerHealthChecker` (`@ConditionalOnProperty health-check-enabled=true`, `@Scheduled(fixedDelayString=…interval)`), `config/SchedulingConfig` (`@EnableScheduling`), `retrieval/service/ContextRetriever` (rerank başarı → `recordAvailable`, yedek yol → `recordUnavailable`) | `isRerankerUsed()` = `RAG` ve `RERANK`; değilse `NOT_USED`, kayıtlar yok sayılır. `"15s"` biçimi Spring 7 `@Scheduled`'da `DurationFormatterUtils.detectAndParse` ile çözülüyor (doğrulandı) |
| Kararlar #18 | `application.properties` (`assistant.reranker.health-check-interval=15s`, `health-check-enabled=true`) | ASCII kontrolü: `LC_ALL=C grep -c '[^ -~]'` → 0 |
| K8 salt okunur | `LiveConfigController` sadece `GET` | `POST` → 405 `METHOD_NOT_ALLOWED` (canlı denendi) |
| CLAUDE.md | `assistant-api/CLAUDE.md` | Durum satırı, ayar listesi (`generation.mode`, `health-check-*`, tracker), V3, paket yapısı (`liveconfig/`), "Üretim modu henüz yok" satırı çıktı, duman testi (`GENERATION_MODE=llm`, live-config, reranker kapalı denemesi), `spring-boot:run` + `-Dmaven.test.skip=true` notu |

## Değişen dosyalar
Tümü `assistant-api/` altında.
- **Yeni:** `src/main/java/com/rasitbank/assistant/config/SchedulingConfig.java`; `liveconfig/controller/{LiveConfigController,package-info}.java`; `liveconfig/service/{LiveConfigService,package-info}.java`; `liveconfig/domain/dto/{LiveConfigResponse,KnowledgeBaseResponse,KnowledgeBaseSource,KnowledgeBaseStatus,package-info}.java`; `retrieval/client/{WorkspaceResponse,SourcePageResponse,SourceResponse}.java`; `retrieval/service/{RerankerStatus,RerankerStatusTracker,RerankerHealthChecker}.java`; `src/main/resources/db/migration/V3__add_llm_only_generation_mode.sql`; `src/main/resources/prompts/llm-only-system.st`.
- **Değişen:** `config/AssistantProperties.java`; `conversation/domain/entity/{GenerationMode,RetrievalStrategy,Turn,TurnValues}.java`; `conversation/domain/event/{SourcesEvent,DoneEvent}.java`; `conversation/service/{AnswerService,AnswerStreamer,TurnDraft,TurnService}.java`; `generation/PromptBuilder.java`; `retrieval/client/{RetrievalClient,RerankClient}.java`; `retrieval/service/ContextRetriever.java`; `src/main/resources/application.properties`; `CLAUDE.md`.
- **Silinen:** yok. **Yeni dependency:** yok (`@EnableScheduling` Spring'in kendisinde). `pom.xml` değişmedi.

## Plandan sapmalar
Yok. Küçük uygulama ayrıntıları (plan bunları açık bırakıyordu):
- V3'te silinen satır içi CHECK'ler (`conversation_turns_generation_mode_check`, `conversation_turns_retrieval_strategy_check`; adlar dev veritabanında `\d` ile doğrulandı) `ck_conversation_turns_generation_mode` ve `ck_conversation_turns_retrieval_strategy` adıyla yeniden eklendi.
- `liveconfig` ve `liveconfig.domain` ara paketlerine `package-info.java` konmadı (sınıf yok; mevcut `conversation`/`conversation.domain` düzeniyle aynı). Sınıflı her pakette var.
- M0'da kullanıcının çalışan reranker'ını (llama-server, 8081) durdurmak izin sınıflandırıcısınca engellendi; ona dokunulmadı. "Reranker kapalı" durumu, ikinci bir assistant-api örneğini boş bir adrese (`RERANKER_URL=http://localhost:8089`) yönlendirerek denendi (aşağıda). Gerçek reranker'ın durdurulup yeniden açılması (M3) qa'ya kalıyor.

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı, ana kod temiz derleniyor.
- `cd assistant-api && ./mvnw clean verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE, test derleme hatası (testCompile).** Testler çalışmadı, test sayısı yok. Kırılanların hepsi `src/test`'te (tester'ın) ve planın değiştirdiği imzalardan geliyor; ana kodda hata yok (TASK-010'daki durumla aynı):
  - `support/TestProperties.java:38,50,60,73`: `AssistantProperties.Reranker` 6 bileşen (`healthCheckInterval`, `healthCheckEnabled`); `:81`: `Generation` ilk bileşen `mode`.
  - `conversation/service/AnswerStreamerTest.java:61,281`, `ConversationPersistenceIntegrationTest.java:70`, `ConversationServiceIntegrationTest.java:48`, `TurnServiceIntegrationTest.java:42,175`: `TurnDraft` 2. bileşen `GenerationMode`.
  - `generation/PromptBuilderTest.java:28`, `conversation/service/AnswerServiceTest.java:59`: `PromptBuilder` 3. parametre `llm-only-system.st` kaynağı.
  - `retrieval/service/ContextRetrieverTest.java:45`: `ContextRetriever` 3. parametre `RerankerStatusTracker`.
  - Derleme düzelince bakılacak: `SourcesEvent`/`DoneEvent` JSON'unda `generationMode` ilk alan ve `retrievalLatencyMs` nullable; mevcut SSE beklentileri (`SendMessageIntegrationTest`) `generationMode` alanını görecek.
- Not: `spring-boot:run` test derlemesini de yaptığı için M0'da `-Dmaven.test.skip=true` ile çalıştırıldı. Tester `clean verify` kullanmalı (artımlı derleme eski test sınıflarıyla yanıltıcı sonuç verebilir).
- **V3 gerçek veritabanında:** dev postgres (`pgvector/pgvector:0.8.1-pg18`) üzerinde açılışta uygulandı (`flyway_schema_history` 1, 2, 3); `\d conversation_turns` beş yeni CHECK'i ve nullable `search_query`/`candidates`/`top_k`'yı gösteriyor; eski RAG turları kısıtları geçti.

### M0 (gerçek model: Ollama `qwen3.5:9b`, llama-server `bge-reranker-v2-m3`, retrieval-api + bilgi bankasında 2 belge)
**`GENERATION_MODE=llm`:**
- `GET /live-config` → `generationMode` `LLM_ONLY`, `retrievalStrategy` `RERANK` (yapılandırılan), `rerankerStatus` `NOT_USED`, `embeddingModel` `bge-m3`/1024, `knowledgeBase` `READY` 2 (`10001` "Raşit Bankası Ürün ve Hizmet Rehberi" `2026.10`, `10002` "RAG anlatımı" `v1`).
- "Kartımı kaybettim, ne yapmalıyım?" → `sources` = `{"generationMode":"LLM_ONLY","retrievalStrategy":"NONE","candidates":null,"topK":null,"searchQuery":null,"queryRewriteStatus":"DISABLED","contextChunks":[],"citations":[]}`; `done` `retrievalLatencyMs`/`rerankLatencyMs` null, `notFoundAnswer` false, `promptTokens` 63.
- Takip "Yenisi kaç günde gelir?" → yeniden yazma yok (`DISABLED`, `queryRewriteLatencyMs` null), log `historyTurns=1`, `promptTokens` 108 (geçmiş prompt'a girdi).
- retrieval-api durdurulunca: `live-config` 200, `knowledgeBase` `{"status":"UNAVAILABLE","searchableSourceCount":null,"sources":[]}`, `embeddingModel`/`embeddingDimensions` null; "Gold kartın yıllık aidatı ne kadar?" yine cevaplandı.
- `conversation_turns` (turId 28-30): `LLM_ONLY`, `NONE`, `search_query`/`candidates`/`top_k`/`retrieval_latency_ms`/`rerank_latency_ms` NULL, `DISABLED`, `cited` `{}`, `turn_chunks` 0 satır.

**Varsayılan (`rag`), aynı sorular:**
- `live-config` → `RAG`, `RERANK`, `rerankerStatus` `UP` (açılıştaki sağlık kontrolü).
- Karşılaştırma (AC1):

| Soru | Sadece LLM | RAG |
|---|---|---|
| Kartımı kaybettim, ne yapmalıyım? | "Hemen 720 10 00 numaralı çağrı merkezimizi arayarak kartınızı bloke ettirin." — alıntı yok, numara uydurma, rehbere dayanmıyor (7,5 sn) | "…aşağıdaki kanallardan biriyle kapatın [1]. Bildirimden sonra … sorumlu olmazsınız [1]. Yeni kartınız … kuryeyle gönderilir [3]." — `RERANK`, `citations` [1, 3] (23 sn, ilk istek; model ısınması) |
| Yenisi kaç günde gelir? (takip) | "Yeni kartınız en geç 5 iş günü içinde adresinize gönderilir." — kısmen doğru, kaynaksız (0,9 sn) | "…il merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde kuryeyle teslim edilir [1]…" — `REWRITTEN`, `citations` [1, 3] (9 sn) |
| Gold kartın yıllık aidatı ne kadar? | "Raşit Bankası Gold Kart'ın yıllık aidatı **1.500 TL**'dir." — yanlış (uydurma), alıntı yok (1,3 sn) | "Raşit Gold kartın yıllık üyelik ücreti 1.450 TL'dir [1]." — rehberle tutarlı, `citations` [1] (6 sn) |

**Reranker kapalı (ikinci örnek, port 8091, `RERANKER_URL=http://localhost:8089`, 8089'da dinleyen yok):**
- Sağlık kontrolü açık: açılıştaki ilk kontrolle `rerankerStatus` `DOWN` (log `Reranker status changed from=UNKNOWN to=DOWN`, thread `scheduling-1`); soru → `RERANK_FALLBACK`, alıntılı cevap.
- `ASSISTANT_RERANKER_HEALTHCHECKENABLED=false`: soru öncesi `UNKNOWN`, sorudaki rerank hatasından hemen sonra `DOWN` (thread `nio-8091-exec-6`, `ContextRetriever` uyarısıyla aynı anda) → kart C'nin iki ölçüm yolu da çalışıyor.
- Ana örnekte (8090, gerçek reranker) durum boyunca `UP`, durum değişimi logu sadece 1 kez (açılışta `UNKNOWN → UP`).
- `POST /api/assistant/live-config` → 405 `application/problem+json`, `errorCode` `METHOD_NOT_ALLOWED`.
- Loglarda soru/cevap metni yok (`grep` "kartımı|gold kart|1.450|aidat" → 0 satır).
- Başlattığım servisler (8080, 8090, 8091) durduruldu; dev postgres `stop` ile eski (durmuş) haline getirildi. Ollama ve llama-server'a dokunulmadı.

## Tester için notlar
- **Derleme düzeltmeleri** yukarıda (Doğrulama); `TestProperties`'e `mode` (`"rag"`), `healthCheckInterval` (`Duration.ofSeconds(15)`), `healthCheckEnabled` eklenmeli. `RerankClient`'ı sahtelemeyen `@SpringBootTest`'ler `assistant.reranker.health-check-enabled=false` almalı (yoksa açılışta `localhost:8081/health`'e istek atılır; test kırılmaz ama gerçek adrese gider).
- **Seam'ler:** `RerankerStatusTracker` constructor'ı sadece `AssistantProperties` alır (mode + strategy) → B15 için `TestProperties` ile kurulabilir. `RerankerHealthChecker` iki bağımlılık (`RerankClient`, tracker) → B17 unit. `LiveConfigService` constructor'ı (`AssistantProperties`, `chatModelName`, `RetrievalClient`, tracker) → B9-B13, B18 sahte `RetrievalClient` ile; sayfa boyutu sabiti `LiveConfigService.SOURCE_PAGE_SIZE` (100, package-private). `RetrievalClient.getWorkspace/listSources` ve `RerankClient.isHealthy` `MockRestServiceServer` ile (`/api/workspaces/42`, `/api/workspaces/42/sources?page=0&size=100`, `/health`).
- **LLM_ONLY'de `AnswerStreamer`:** `draft.generationMode() == LLM_ONLY` ve `context == null` olmalı; `RAG` + `context == null` → `NullPointerException` (bilinçli; RAG turunun context'i zorunlu). `streamFixedAnswer` sadece RAG yolunda çağrılır.
- **`TurnService`:** `LLM_ONLY` draft'ında `context` dolu gelse bile yok sayılır (`rag ? draft.context() : null`); `searchQuery` de `null` yazılır. RAG + retrieval hatası (`context` null) eskisi gibi yapılandırma değerleriyle (`candidates`/`topK`/strateji) yazılır.
- **B6 (V3 kısıtları):** CHECK adları `ck_conversation_turns_mode_strategy`, `ck_conversation_turns_none_has_no_retrieval`, `ck_conversation_turns_retrieval_has_values`, `ck_conversation_turns_generation_mode`, `ck_conversation_turns_retrieval_strategy`.
- **Zaman/eşzamanlılık:** tracker `AtomicReference.getAndSet`; zamanlayıcı testte açılmasın diye `health-check-enabled=false`; zaman `Clock`'tan okunmuyor (sadece durum, zaman damgası yok).
- **B3 literal:** `llm-only-system.st` dosyası sonda satır sonu taşır; `PromptBuilder` `strip()` ettiği için System mesajı satır sonu olmadan lab metnidir.

STATUS: DONE
