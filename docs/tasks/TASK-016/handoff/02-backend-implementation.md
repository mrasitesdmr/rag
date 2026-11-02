TASK: TASK-016
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-evaluation`, `rag-pipeline`, `spring-transactions`, `java-concurrency`, `flyway-migrations`, `postgres-patterns`.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| retrieval-api `findChunkMatches` (Kural 2, K1, R1-R7) | `chunk/controller/ChunkController`, `chunk/service/ChunkService.findChunkMatches`, `chunk/repository/ChunkRepository.findChunksContainingTexts` + `ChunkTextMatchRow`, `chunk/domain/dto/FindChunkMatchesRequest`, `ChunkMatchesResponse`, `ChunkTextMatchResponse`, `MatchedChunkResponse` | Tek native sorgu (`unnest … WITH ORDINALITY` ⨝ `chunks` ⨝ `sources.active_version_id` ⨝ `source_versions`, `ORDER BY ordinality, c.id`). Projeksiyon: interface + tırnaklı alias. Metin `normalizeQuery` ile normalize edilir. Yanıttaki `text` gönderilen metindir. `texts[i]` alan yolu Spring'in `determineField`'ından zaten bu biçimde gelir, ek çeviri gerekmedi. Log: `Chunk matches found texts={} matchedTexts={} durationMs={}` |
| V7 migration (5d, K3, K4) | `db/migration/V7__create_golden_set_and_eval_runs.sql` | Plandaki tablolar, CHECK'ler, `ux_eval_runs_single_running`, `ix_eval_runs_started_at_id`, `ix_golden_questions_normalized_text`. Küçük ekler "Plandan sapmalar"da |
| Golden set yükleme/okuma (1a, K2) | `evaluation/controller/GoldenSetController`, `service/GoldenSetService`, `repository/GoldenSetRepository`, `GoldenQuestionRepository`, `domain/entity/GoldenSet`, `GoldenQuestion`, `domain/dto/ReplaceGoldenSetRequest`, `GoldenQuestionRequest`, `GoldenSetSummaryResponse`, `domain/validation/ValidGoldenSet`, `GoldenSetValidator` | Tek transaction: `LOCK TABLE` → sürüm çakışması kontrolü (SHA-256, `\u001F`/`\u001E`) → `deleteAllInBatch` (cascade) → sırayla ekleme. Metinler `strip()`, `normalized_text` = `SuggestionTextNormalizer.normalize` (K11) |
| Ölçüm başlatma (4a, 4b, 5a, 5b, G1, K4) | `evaluation/service/EvalRunService.startEvalRun`, `controller/EvalRunController` | Kontrol sırası sözleşmedeki gibi: LLM_ONLY → set yok → RUNNING var → `findChunkMatches` (transaction dışında) → ölçülebilir soru yok → `createRun`. Kısmi unique index ihlali → 409 `EVAL_RUN_IN_PROGRESS`. Ardından `evalExecutor`'a gönderilir. 202 + `Location: /api/assistant/evals/{id}` |
| Arka plan ölçüm (6, 4c, K8, K10) | `service/EvalRunRunner`, `EvalRunLifecycleService`, `AnswerChunkRanker`, `QuestionRank`, `RetrievalMetrics`, `RetrievalMetricsResult`, `config/EvalExecutorConfig` | Sadece `ContextRetriever.retrieveCandidatesAndContext`; dil modeli çağrılmaz. Her soru sonucu kendi transaction'ında yazılır. `RERANK_FALLBACK` → `is_rerank_fallback` + `rerank_fallback_count`. Hata eşlemesi: `RETRIEVAL_UNAVAILABLE` → `FAILED`/`RETRIEVAL_UNAVAILABLE`, diğer hata → `INTERNAL_ERROR` + `log.error` |
| Açılışta yarım ölçüm (5c) | `service/EvalRunStartupListener` | `ApplicationReadyEvent` → `failInterruptedRuns()` → `EVAL_RUN_INTERRUPTED`. Log: `Interrupted eval runs failed count={}` |
| Okuma uçları (5c, 7b, 7e, K5) | `EvalRunService.listEvalRuns`, `getEvalRun`, `getEvalOverview`; `repository/EvalRunRepository`, `EvalRunQuestionRepository.findMissedAndMoved`; DTO'lar `EvalRunResponse`, `EvalRunDetailResponse`, `EvalQuestionResultResponse`, `EvalOverviewResponse` | Sıra `started_at DESC, id DESC`. `missedAndMovedQuestions` sadece `DONE`'da dolar, en fazla 24. `hitCount`/`missedCount`/`recallAtK`/`mrr` `DONE` değilse `null` |
| `ContextRetriever.retrieveCandidatesAndContext` (B15, K8) | `retrieval/service/ContextRetriever` | Her zaman `candidates` (20) parça arar. `TOP_K`'da prompt ilk `topK` parçadır. `retrieveContext`'in davranışı değişmedi; ortak kısım `retrieve(...)` özel metoduna alındı |
| `RetrievalClient.findChunkMatches` | `retrieval/client/RetrievalClient`, `FindChunkMatchesRequest`, `ChunkMatchesResponse`, `ChunkTextMatch`, `MatchedChunk` | Hata davranışı `call(...)` ile aynı. Yanıtta `matches` yoksa ya da metin sayısıyla uyuşmuyorsa `RETRIEVAL_UNAVAILABLE` |
| Golden işaret (Kural 8, K6, K7, G3, G5) | `evaluation/service/GoldenAnswerLocator`; `conversation/domain/entity/TurnStatus` (+`MISSED`, `GENERATION_ERROR`), `service/TurnStatusResolver.resolve(turn, answerChunkIds)`, `domain/dto/TurnResponse.from(turn, status, answerChunkIds)`, `TurnChunkResponse.from(chunk, containsAnswer)`, `domain/event/ContextChunk` (+`containsAnswer`), `service/ConversationReadService`, `AnswerService`, `AnswerStreamer`, `repository/TurnRepository.findAllWithChunksByIdIn` | `ConversationReadService` sınıf düzeyindeki transaction'ı bıraktı. Veritabanı `TransactionTemplate` (readOnly) içinde okunur; liste için son "bulamadım" turlarının parçaları aynı transaction'da fetch join ile yüklenir. Locator transaction kapandıktan sonra çağrılır. `AnswerService` locator'ı sadece RAG'de ve bağlam boş değilse, akış açılmadan çağırır. Sonuç `AnswerStreamer.streamModelAnswer(..., answerChunkIds)`'e geçer |
| Hata kodları | kategori sınıfları | `GOLDEN_SET_NOT_LOADED` (404 GET / 409 POST), `GOLDEN_SET_VERSION_CONFLICT`, `EVAL_NOT_AVAILABLE_IN_LLM_ONLY`, `EVAL_RUN_IN_PROGRESS`, `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE`, `EVAL_RUN_NOT_FOUND`. Yeni handler metodu yok |
| CLAUDE.md'ler | `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md` | Durum, paket yapısı, V7, `evalExecutor`, duman testi komutları. assistant-api'de "Henüz olmayanlar"dan "değerlendirme" çıkarıldı |

## Değişen dosyalar
**retrieval-api** (`src/main/java/com/rasitbank/retrieval/chunk/`)
- Yeni:
  - `domain/dto/FindChunkMatchesRequest.java`
  - `domain/dto/ChunkMatchesResponse.java`
  - `domain/dto/ChunkTextMatchResponse.java`
  - `domain/dto/MatchedChunkResponse.java`
  - `repository/ChunkTextMatchRow.java`
- Değişen:
  - `controller/ChunkController.java`
  - `service/ChunkService.java`
  - `repository/ChunkRepository.java`
  - `retrieval-api/CLAUDE.md`

**assistant-api** (`src/main/…/assistant/`)
- Yeni:
  - `evaluation/**`: controller 2, service 13, repository 4, entity 6, dto 7, validation 2, ayrıca her alt pakette `package-info.java`
  - `config/EvalExecutorConfig.java`
  - `retrieval/client/FindChunkMatchesRequest.java`
  - `retrieval/client/ChunkMatchesResponse.java`
  - `retrieval/client/ChunkTextMatch.java`
  - `retrieval/client/MatchedChunk.java`
  - `resources/db/migration/V7__create_golden_set_and_eval_runs.sql`
- Değişen:
  - `conversation/domain/entity/TurnStatus.java`
  - `conversation/domain/dto/TurnResponse.java`
  - `conversation/domain/dto/TurnChunkResponse.java`
  - `conversation/domain/event/ContextChunk.java`
  - `conversation/repository/TurnRepository.java`
  - `conversation/service/TurnStatusResolver.java`
  - `conversation/service/ConversationReadService.java`
  - `conversation/service/AnswerService.java`
  - `conversation/service/AnswerStreamer.java`
  - `retrieval/client/RetrievalClient.java`
  - `retrieval/service/ContextRetriever.java`
  - `assistant-api/CLAUDE.md`

Yeni dependency yok. Yeni property yok.

## Plandan sapmalar
Davranış ya da sözleşme farkı yok. Planda imzası yazmayan yardımcı yerler ve küçük ekler şunlar; hiçbiri sözleşmeyi değiştirmiyor, bu yüzden planner'a mesaj atılmadı:
- **V7'de plana ek CHECK'ler:**
  - `ck_eval_runs_counts` (`measured ≤ question`, `processed ≤ measured`)
  - `ck_eval_run_questions_measured` (`is_measured` ⇔ dizi boş değil)
  - `ck_eval_run_questions_processed` (işlenmemiş soruda sonuç alanları boş)
  - `candidates`/`top_k` `NOT NULL ≥ 1`
  - `golden_questions`'ın bütün kolonları `NOT NULL`
  - `ck_eval_runs_finished` `duration_ms`'i de kapsıyor
- **Yardımcı tipler ve metotlar:**
  - `GoldenSetService.findGoldenSet()` (overview için) ve `findGoldenSetSnapshot()` + `GoldenSetSnapshot`
  - `EvalRunQuestionDraft`
  - `GoldenQuestionRepository.countByGoldenSetId`
  - `EvalRunRepository.findAllByStatus` (`failInterruptedRuns` için)
  - `TurnRepository.findAllWithChunksByIdIn`. Plandaki "son turun prompt parça kimlikleri okuma transaction'ında yüklenir" maddesini karşılar.
- **İmzalar:**
  - `EvalRunLifecycleService.createRun(version, strategy, candidates, topK, sourceVersions, drafts)`
  - `completeRun(evalRunId, metrics)`
  - `failRun(evalRunId, errorCode)`; log için `EvalRunResponse` döner
  - `recordQuestionResult(evalRunId, position, QuestionRank, rerankFallback)`
  - `AnswerStreamer.streamModelAnswer(draft, prompt, emitter, answerChunkIds)` (4. parametre). `streamFixedAnswer` boş küme kullanır.
- **`GoldenAnswerLocator`:** normalize edilince boş kalan soruları ("???") eşleştirmeye almaz.
- **Kısmi unique index ihlali:** sadece mesajında `ux_eval_runs_single_running` geçen `DataIntegrityViolationException` 409'a çevrilir. Başka bir constraint ihlali olduğu gibi yükselir (500).
- **Executor reddi:** `evalExecutor` görevi reddederse (`TaskRejectedException`) kayıt `FAILED`/`INTERNAL_ERROR` yapılır ve hata yükselir. Tek `RUNNING` kuralı yüzünden pratikte oluşmaz.

## Doğrulama
- **retrieval-api:** `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD SUCCESS, Tests run: 332, Failures: 0, Errors: 0, Skipped: 0**, "All coverage checks have been met". Eşik yeni kodun testleri olmadan da tutuyor.
- **assistant-api ana kod:** `./mvnw -q compile` → başarılı.
- **assistant-api, gerçek repo:** `./mvnw clean test-compile` → **COMPILATION ERROR** (beklenen). Plan gereği imzası değişen yerleri kullanan testler derlenmiyor; `src/test/**`'e yazamadığım için dokunmadım:
  - `AnswerServiceTest`: kurucuya `GoldenAnswerLocator` geldi; `streamModelAnswer` 4 parametre aldı.
  - `AnswerStreamerTest`: `streamModelAnswer(..., Set.of())`.
  - `TurnStatusResolverTest`: `resolve(turn, Set.of())`.
- **assistant-api, scratch kopya** (`/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/39760b99-b80b-4d41-b8d4-df7a0bb45f02/scratchpad/assistant-api-verify`):
  - `src/main` repo ile aynı. Testlerde sadece şu en küçük uyarlamalar yapıldı:
    - Yukarıdaki üç sınıf: `GoldenAnswerLocator` için `mock(...)`, 4. parametre `any()` / `Set.of()`, `resolve(turn, Set.of())`.
    - `AssistantApplicationIntegrationTest`'te `hasSize(6)` → `hasSize(7)`. Testin adı `…AllSixMigrations` da güncellenmeli.
  - Komut: `OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD SUCCESS, Tests run: 354, Failures: 0, Errors: 0, Skipped: 0**.
  - Satır coverage 0,74, eşik 0,80. Yeni kodun testleri olmadığı için eşik kapalıydı; tester ve qa tam `./mvnw verify` ile doğrular. Repo'daki testlere dokunulmadı.
- **Gerçek sistem duman testi (K3, S1, S2):**
  - Ortam: dev postgres, retrieval-api 8080, assistant-api 8090, Ollama, reranker.
  - Bilgi bankası: "Raşit Bankası Ürün ve Hizmet Rehberi" 2026.10 (154 parça) ve "RAG anlatımı v1" (63 parça).
  - Golden set: `docs/rehber/golden/rehber-golden-v1.json` (100 soru) PUT ile yüklendi.

  | Çalıştırma | Strateji | Ölçülen / ölçülemeyen | recall@5 | MRR | Kaçan | Reranker DOWN | Süre |
  |---|---|---|---|---|---|---|---|
  | ev_1 | RERANK (rerank 20→5) | 100 / 0, rehber 2026.10 | **0,98** (98/100) | **0,9125** | 2 (sıra 70 ve 87; ikisi de ilk 20 adayda yok, Vector # "20+") | 0 | 102,3 sn |
  | ev_2 | TOP_K (`RETRIEVAL_STRATEGY=top5`) | 100 / 0, rehber 2026.10 | **0,89** (89/100) | **0,7223** | 11 (Kaçanlar tablosunda 11 satır, hepsi "yok") | 0 | 5,4 sn |

  - ev_1'de doğru parça 85 soruda prompt'un 1. sırasında. 36 soru "kaçan ya da sırası değişen"; ekrana ilk 24'ü geliyor. `unmatchedCount` 0, yani golden set'in bütün `answerText`'leri rehber 2026.10'da bulundu.
- **Uçların gerçek yanıtları:**
  - `chunk-matches`:
    - Büyük harf ve fazla boşluklu metin eşleşti (chunk 29).
    - Tekrar eden metin istek sırasıyla geldi, bulunmayan metin `[]`.
    - `texts: []` → `size must be between 1 and 500`; `["ok","  ",null]` → `texts[1]`, `texts[2]` `must not be blank`; `{}` → `texts` `must not be null`.
    - Çalışma alanı 7 → 404 `WORKSPACE_NOT_FOUND`; `text/plain` → 415.
  - Golden set:
    - Set yokken `GET` → 404 `GOLDEN_SET_NOT_LOADED`; `POST /evals` → 409 `GOLDEN_SET_NOT_LOADED`.
    - B21 gövdesi → 400 ve tam üç ihlal (`questions[1].text` "must be unique", `questions[0].answerText` "size must be between 20 and 300 characters", `questions[2].pageNumber`).
    - Aynı sürüm farklı içerik → 409 `GOLDEN_SET_VERSION_CONFLICT`. Aynı sürüm aynı içerik → 200, `loadedAt` yenilendi.
  - Ölçüm:
    - `POST /evals` → 202 + `Location: /api/assistant/evals/1`; ikinci istek → 409 `EVAL_RUN_IN_PROGRESS`.
    - `GENERATION_MODE=llm` → 409 `EVAL_NOT_AVAILABLE_IN_LLM_ONLY`.
    - Ölçüm sürerken servis durduruldu (9/100 işlenmişti). Açılışta log `Interrupted eval runs failed count=1`, ev_3 `FAILED`/`EVAL_RUN_INTERRUPTED` ve `finishedAt` dolu. Overview'da `lastFinishedRun` ev_3, `lastDoneRun` ev_2.
    - `?size=101` → 400 `VALIDATION_FAILED` (`size`); `/evals/abc` → 400 `BAD_REQUEST`; `/evals/999` → 404 `EVAL_RUN_NOT_FOUND`.
  - SSE (G5): "ÜCRET değişiklikleri bana ne kadar önceden bildiriliyor" (harf ve noktalama farklı) → `sources.contextChunks` içinde yalnız chunk 1'de `containsAnswer: true`.
  - Konuşmalar, geçici golden setlerle; ardından gerçek set geri yüklendi:
    - "Bitcoin alıp satabilir miyim?" "bulamadım" turu, cevap parçası prompt dışında → detayda ve listede `MISSED`.
    - Cevap parçası prompt'taki chunk 116 → `GENERATION_ERROR`, yalnız o satırda `containsAnswer: true`.
    - Gerçek set geri yüklenince → `DONT_KNOW`.
  - G3: retrieval-api kapalıyken konuşma detayı 200 döndü, bütün `containsAnswer` false.
  - Bitince assistant-api, retrieval-api ve dev postgres durduruldu (`docker compose -f deployment/docker-compose.dev.yml stop`). Dev veritabanında golden set `rehber-golden-v1` ve ev_1-ev_3 kayıtlı kaldı.

## Tester için notlar
- **Kırılan mevcut testler:** Uyarlama listesi "Doğrulama"daki scratch maddeleriyle aynı.
  - `AnswerServiceTest`
  - `AnswerStreamerTest`
  - `TurnStatusResolverTest`
  - `AssistantApplicationIntegrationTest` (7 migration)
  - `ContextChunk`/`TurnChunkResponse` fixture'larında yeni `containsAnswer` alanı da tester'ın işi.
- **Dış çağrı sahteleme noktaları:**
  - `RetrievalClient.findChunkMatches(List<String>)` → `ChunkMatchesResponse(List<ChunkTextMatch>)`. Metin başına bir eleman, aynı sırayla.
  - `ContextRetriever.retrieveCandidatesAndContext(String)` → `RetrievedContext`. Fallback = `strategy() == RERANK_FALLBACK`.
  - `GoldenAnswerLocator.locateAnswerChunks(Collection<String>)` → `Map<normalize(soru), Set<chunkId>>`. `AnswerService` ve `ConversationReadService` için mock'lanabilir. Mockito'nun varsayılanı boş `Map` döner, bu da "işaret yok" demektir.
- **`ConversationReadService`:** kurucu artık `GoldenAnswerLocator` ve `PlatformTransactionManager` alıyor. Unit testte mock `PlatformTransactionManager` verilebilir; `TransactionTemplate.execute` mock'la çalışır.
- **`TurnStatusResolver.resolve(turn, ids)`:** küme boşsa `turn.getChunks()`'a hiç dokunmaz. Boş değilse `prompt_position` dolu parçalara bakar (B22-B25).
- **Ölçümün yürütülmesi:**
  - Bean adı `evalExecutor` (`ThreadPoolTaskExecutor`, 1 thread, kuyruk 1). Integration testte senkron çalıştırmak için aynı adla bir `SyncTaskExecutor` sağlanabilir. Ya da sonuç `GET /evals/{id}` yoklanarak beklenir (Awaitility; `Thread.sleep` değil).
  - `EvalRunService` executor'ı `@Qualifier("evalExecutor") TaskExecutor` olarak alır.
- **Zaman:**
  - `EvalRunLifecycleService` ve `GoldenSetService` `Clock` kullanır. `durationMs` = `finishedAt − startedAt`.
  - B16 için iki farklı an gerekir. Sabit saatle süre 0 çıkar; ilerletilebilir bir `Clock` kullanılmalı.
- **Açılış dinleyicisi:** `EvalRunStartupListener` her `@SpringBootTest` açılışında çalışır ve `RUNNING` satırları `FAILED` yapar. B17'de satır context açıldıktan sonra eklenmeli ve `failInterruptedRuns()` ya da `ApplicationReadyEvent` yeniden tetiklenmeli.
- **B18 (eşzamanlı başlatma):**
  - `existsByStatus` kontrolünü iki istek birlikte geçebilir. Son güvence `ux_eval_runs_single_running`; ihlal `saveAndFlush`'ta yakalanıp 409'a çevrilir.
  - `RetrievalClient` mock'u iki isteği de ölçülebilir döndürmeli.
- **Saf fonksiyonlar:**
  - `RetrievalMetrics.evaluate(List<@Nullable Integer>)`: boş liste `IllegalArgumentException`, 1'den küçük sıra da `IllegalArgumentException`.
  - `AnswerChunkRanker.rank(context, ids)`.
- **retrieval-api R1-R7:** `ChunkService.findChunkMatches` + `ChunkRepository.findChunksContainingTexts`; gerçek pgvector Testcontainers ile. Satır sonu, harf (`I/İ`) ve boşluk normalize edilir; noktalama korunur. Sadece `sources.active_version_id` sürümü aranır.

STATUS: DONE
