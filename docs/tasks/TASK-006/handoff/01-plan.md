TASK: TASK-006
# Plan

## Özet
**Mevcut durum (TASK-027 + TASK-024):** retrieval-api belgeleri içe alıyor (kaynak → içe alma işi → sürüm → parçalar + `vector(1024)` embedding, HNSW `chunks_embedding_hnsw_idx`). İş `DONE` olunca `sources.active_version_id` yeni sürüme bağlanıyor (`IngestionJobLifecycleService.complete`, parçalarla aynı transaction'da). Arama ucu yok; `search/` paketi yok; `search.*` ayarı yok.

**Yeni durum:**
- `POST /api/workspaces/{workspaceId}/search`, gövde `{ "query": "...", "topK": 5 }` → 200 `{ "hits": [ ... ] }`. Sorgu kırpılır, `EmbeddingClient` ile embed edilir, her kaynağın sadece güncel sürümünün (`active_version_id`) parçalarında HNSW ile en yakın `topK` parça döner; sıra mesafe artan, eşitlikte `chunkId` artan.
- Hiçbir şey yazılmaz (arama kaydı yok). Migration yok, dependency yok.
- **Değişen servis:** sadece retrieval-api. assistant-api (çağıran taraf, TASK-008), web ve Docker değişmez.

**Yeniden adlandırma / sapma:**
- Skill'ler ve `retrieval-api/CLAUDE.md` arama gövdesini lab'daki gibi `{query, mode, limit}` diye anıyor. TASK.md `topK` diyor; sözleşme `{query, topK}`'dır, `mode` alanı yoktur (Kararlar #1). `retrieval-api/CLAUDE.md`'deki anma backend-coder tarafından düzeltilir; skill'ler için "Açık sorular ve riskler" 4.
- `retrieval-api/CLAUDE.md`'deki hedef anahtar `search.default-ef-search` → `search.ef-search` (istek başına ef_search değişmediği için "default" yok).

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** retrieval-api `src/main/**`, `retrieval-api/CLAUDE.md`. M0 (gerçek `bge-m3` ile ölçüm ve `EXPLAIN`). Rapor `handoff/02-backend-implementation.md`.
- **tester:** `retrieval-api/src/test/**`. Rapor `handoff/03-test-report.md`.
- **qa:** M1, kabul kriterleri, sözleşme uyumu (elle; springdoc yok), log kontrolü. Rapor `handoff/05-qa-report.md`. qa, tester'a bağlıdır.
- **Atlanan:** frontend-coder (TASK: "Ekran yok"); devops (Docker, Compose, ortam değişkeni ve lokal model değişmiyor; yeni ayarların varsayılanı `application.properties`'te).

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | İstek: sorgu metni + `topK` | `SearchChunksRequest(query, topK)`, `SearchController.searchChunks` | T8, T6 |
| K2 | `topK` 1-50; boş sorgu, 1000 karakterden uzun sorgu (kart sorusu 1), sınır dışı `topK` → `VALIDATION_FAILED` | Bean Validation (`SearchChunksRequest`), `GlobalExceptionHandler.handleMethodArgumentNotValid` (mevcut) | T9-T14 |
| K3a | Her kaynağın sadece güncel (`active_version_id`) sürümü aranır | `ChunkSearchRepository.NEAREST_CHUNKS_SQL` (`source_version_id = ANY(ARRAY(...active_version_id...))`) | T20, T21 |
| K3b | `DONE` sürümü olmayan kaynak aramaya girmez | aynı filtre (`active_version_id IS NOT NULL`) | T21 |
| K3c | İçe alma sürerken eski sürüm döner | aynı filtre; `active_version_id` sadece WRITING commit'inde değişir (mevcut) | T20 |
| K4a | Sonuç alanları: parça kimliği, sıra no, sayfa, başlık, metin, kaynak kimliği, kaynak adı, sürüm etiketi, cosine mesafesi | `SearchHitResponse`, SQL'deki join (`sources`, `source_versions`) | T8, T22, T25 |
| K4b | Mesafe artan; eşitse parça kimliği artan | SQL dış `ORDER BY distance, id` | T17, T18 |
| K5 | HNSW `ef_search` 40; sorgu index'i kullanır | `SearchProperties.efSearch`, `set_config` (Kararlar #5), SQL biçimi (Kararlar #4) | T24, M0, M1 |
| K6 | Embedding modeline erişilemezse 503 + sabit `errorCode` | `EmbeddingClient` (mevcut) → `ServiceUnavailableException("EMBEDDING_UNAVAILABLE")` → `GlobalExceptionHandler` | T5, T16c, T25c, M0 |
| K7 | Arama kaydı tutulmaz | Yazan kod ve migration yok | T25a (satır sayıları), qa diff |
| K8 | Sorgu loglanmaz; uzunluk, `topK`, sonuç sayısı, süre loglanır | `SearchService` log satırı (Backend planı "Loglar") | T7, M1 |
| S1 | Rehber 2026.10 yüklü; "Kartımı kaybettim, ne yapmalıyım?" `topK` 5 → ilk sonuçlar kayıp/çalıntı kart bölümünden | gerçek model | M0 (soru 1), M1 |
| S2 | `topK: 0` → 400, `VALIDATION_FAILED`, `violations[0].field = topK` | K2 | T9 |
| S3 | Bilgi bankası boşken arama → boş liste | `SearchService` (model çağrılmaz, Kararlar #8) | T2, T25b |
| S4 | İki belge yüklüyken sonuçlar ikisinden gelebilir; her sonuç belgesini taşır | SQL (kaynak filtresi yok), `sourceId`/`sourceName`/`version` | T22, M0 |
| AC1 | ≥ 10 rehber sorusu için doğru parça ilk 5'te mi, gerçek modelle ölçülüp raporda | backend-coder M0 ("Ölçüm soruları") | M0, M1 |
| AC2 | `EXPLAIN` HNSW index'ini gösterir | SQL + ayarlar | T24, M0, M1 |
| AC3 | Sıralama, filtreleme (güncel sürüm, birden fazla kaynak), doğrulama testleri; `./mvnw verify` geçer | tester | T9-T24 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Cevabı hiçbir belgede olmayan soru | Eşik yok; yine en yakın `topK` parça döner (lab: "gelen parçalar sadece en az uzak olanlar"). "Bulamadım" kararı dil modelinindir (TASK-008) | T19a |
| G2 | `topK` aranabilir parça sayısından büyük | Olan kadar döner (5 parça, `topK` 10 → 5) | T19b |
| G3 | Bilgi bankası boşken model sunucusu kapalı | 200 `{"hits": []}`; model çağrılmaz | T2, T25b |
| G4 | Baştaki/sondaki boşluklu, Türkçe karakterli ya da sadece boşluktan oluşan sorgu | Kırpılıp olduğu gibi (harf büyüklüğü ve Türkçe harfler korunur) embed edilir; sadece boşluk → 400 `query` | T1, T12 |
| G5 | Ayardaki embedding modeli çalışma alanınınkinden farklı | 409 `EMBEDDING_MODEL_MISMATCH`; model çağrılmaz (yükleme ucuyla aynı) | T3, T16b |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Gövde `{query, topK}`; `mode` alanı yok.** Bilinmeyen alanlar yok sayılır (Jackson varsayılanı) | TASK `topK` diyor; hibrit kapsam dışı, tek mod var | Lab'ın `{query, mode, limit}`'i: TASK'la ad çakışır, tek değerli `mode` boşuna doğrulama getirir |
| 2 | **Yanıt `SearchResponse { hits: List<SearchHitResponse> }`**; `rank` alanı yok (liste sırası sıradır) | Nesne sarmalayıcı ileride alan (ör. süreler) eklemeyi kırıcı olmaktan çıkarır | Düz dizi: alan eklenemez. `rank`: listedeki konumla aynı bilgi |
| 3 | **Sürüm etiketi alanı `version`** | `IngestionJobResponse.version` ile aynı kavram, aynı ad | `versionLabel`: aynı şeye iki ad |
| 4 | **Tek SQL cümlesi** (`ChunkSearchRepository.NEAREST_CHUNKS_SQL`): `WITH nearest AS MATERIALIZED (SELECT id, embedding <=> CAST(:embedding AS vector) AS distance FROM chunks WHERE workspace_id = :workspaceId AND embedding_model = :embeddingModel AND source_version_id = ANY (ARRAY(SELECT active_version_id FROM sources WHERE workspace_id = :workspaceId AND active_version_id IS NOT NULL)) ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :topK)`; dışta `chunks`, `sources`, `source_versions` join'i, `ORDER BY nearest.distance, nearest.id` | `ARRAY(subquery)` bir kez hesaplanan InitPlan olur; filtre index taramasında kalır ve sıra bozulmaz. Tek cümle tek snapshot: WRITING commit'i aramanın ortasına düşse de sonuç tutarlı. Dış sıralama `relaxed_order`'ın gevşek sırasını ve eşitlikteki kimlik sırasını kesinleştirir (lab'ın `semantic AS MATERIALIZED` + `ORDER BY distance, id` kalıbı) | `IN (subquery)`/join: planlayıcı semi-join seçip HNSW sırasını kaybedebilir. Önce sürüm kimliklerini ayrı sorguyla okumak: iki snapshot, arada yeni sürüm yazılırsa eski sürümün parçaları silinmiş olabilir (TASK-015) |
| 5 | **Oturum ayarları aynı transaction'da `set_config(..., true)`** ile (lab): `hnsw.ef_search` = `search.ef-search` (40), `hnsw.iterative_scan` = `relaxed_order`, `hnsw.max_scan_tuples` = `search.max-scan-tuples` (20000), `enable_sort` = `off` | `true` = sadece bu transaction; havuza dönen bağlantıda kalmaz. Filtre satır elediğinde iterative scan `topK`'yı doldurur. `enable_sort off` küçük tabloda da planlayıcıyı "tam tarama + sırala" yerine HNSW'ye yöneltir (154 satırda aksi olası); dış `ORDER BY` yine çalışır (tek seçenek o) | Oturum düzeyinde `SET`: havuzdaki bağlantıda kalır. `strict_order`: daha yavaş, dış sıralama zaten var. `enable_seqscan off`: planlayıcı `chunks_source_id_idx` + sıralamaya kayabilir |
| 6 | **`ChunkSearchRepository` (`@Repository` sınıf) `JdbcClient` ile**, `@Transactional(readOnly = true)` metot: önce `set_config` sorgusu, sonra arama; satırlar doğrudan `SearchHitResponse`'a eşlenir | `set_config` ile aramanın aynı transaction'da olması gerekiyor; entity gerekmiyor. `JdbcClient` `spring-boot-starter-data-jpa` içindeki `spring-jdbc`'den gelir; `JpaTransactionManager` aynı bağlantıyı JDBC'ye açar | Spring Data `@Query(nativeQuery)`: iki cümleyi tek transaction'da tutmak için yine servis düzeyi transaction ve projeksiyon arayüzü gerekir; vektör bağlama daha dolaylı |
| 7 | **Sorgu vektörü metin olarak bağlanır:** `[v1,v2,...]` (`Float.toString`, `,` ayraçlı, boşluksuz) + `CAST(:embedding AS vector)` | pgvector'ün metin girdisi; ek kütüphane yok, yerel ayardan bağımsız | `com.pgvector:pgvector` kütüphanesi: yeni dependency |
| 8 | **Akış:** (1) doğrulama (controller); (2) `workspaceService.ensureWorkspaceExists` (404); (3) `workspaceService.ensureEmbeddingModelMatches(workspaceId, embeddingClient.modelLabel())` (409); (4) `sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(workspaceId)` false ise boş yanıt, model çağrılmaz; (5) `embeddingClient.embed(List.of(strippedQuery))` transaction dışında; (6) `chunkSearchRepository.findNearest(...)`. `SearchService.searchChunks` kendisi `@Transactional` değildir | Dış çağrı transaction açıkken yapılmaz (`backend-conventions` §7). Boş bilgi bankasında model kapalı olsa da senaryo 3 çalışır | (4) olmadan: boş bilgi bankası + kapalı model → 503; senaryo 3 Ollama'ya bağımlı olur |
| 9 | **`EmbeddingClient` olduğu gibi kullanılır** (3 deneme, 2 sn bağlantı / 60 sn okuma zaman aşımı, ortak semaphore 2) | TASK yeni zaman aşımı istemiyor; model sunucusu kapalıyken 503 yaklaşık 3 sn'de döner (1 + 2 sn bekleme) | Aramaya ayrı ayar: yeni property ve ikinci kod yolu; ihtiyaç TASK-008'de ölçülerek doğarsa ayrı task (Açık sorular 2) |
| 10 | **Doğrulama Bean Validation ile, ham değer üzerinde:** `query` `@NotBlank` + `@Size(max = 1000)` (Java `length()`, kırpmadan önce); `topK` `Integer` + `@NotNull` + `@Min(1)` + `@Max(50)`. Sorgu servis içinde `strip()` edilir. Mesajlar sözleşmedeki gibi | Gövde JSON olduğu için mevcut `handleMethodArgumentNotValid` `violations`'ı zaten üretir; yeni exception gerekmez | Kırpılmış uzunluğu saymak: özel doğrulayıcı gerekir; fark sadece 1000 karaktere boşlukla dayanan sorguda |
| 11 | **Sınır değerleri sabit:** `SearchChunksRequest.QUERY_MAX_LENGTH = 1000`, `TOP_K_MIN = 1`, `TOP_K_MAX = 50` (anotasyonlar derleme zamanı sabiti ister) | Sözleşmenin parçası; ortamla değişmez | Property: anotasyonda kullanılamaz |
| 12 | **Ayarlar `SearchProperties` (`search.*`):** `search.ef-search=40`, `search.max-scan-tuples=20000` | Lab değerleri; `backend-conventions` "magic number yok" | Kodda sabit |
| 13 | **springdoc bu task'ta da eklenmez**; sözleşme uyumunu qa elle kontrol eder | Yeni dependency yok (TASK-005/027 kararı) | — |
| 14 | **Ölçüm (AC1):** 12 soru ("Ölçüm soruları" bölümü). Her soru için "doğru parça(lar)" arama çalıştırılmadan önce belirlenir: beklenen alt başlığın PDF'teki cevap cümlesi seçilir, bu cümleyi içeren bütün parçalar (`content LIKE`) doğru sayılır (SSS ve ücret tarifesindeki tekrarlar dahil). Başarı: doğru parçalardan biri ilk 5'te | Beklenen değer aramadan bağımsız türetilir; cevabın rehberde birden fazla yerde geçmesi adil sayılır | Sadece alt başlık adına bakmak: SSS'deki doğru cevap kaçırılmış sayılır |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu: TASK-006 durumu.
- `tags`: `Search`.
- `POST /api/workspaces/{workspaceId}/search` (`searchChunks`, yeni): gövde `SearchChunksRequest`; 200 `SearchResponse`; 400 `VALIDATION_FAILED` (`query`, `topK`), `BAD_REQUEST`; 404 `WORKSPACE_NOT_FOUND`; 409 `EMBEDDING_MODEL_MISMATCH`; 415 `UNSUPPORTED_MEDIA_TYPE`; 503 `EMBEDDING_UNAVAILABLE`, `EMBEDDING_DIMENSION_MISMATCH`; 500. Açıklama: güncel sürüm kuralı, sıralama, eşik olmaması, boş bilgi bankası, kontrol sırası.
- `components.schemas`: yeni `SearchChunksRequest`, `SearchResponse`, `SearchHitResponse`.
- `ProblemDetail.errorCode` enum'u: `EMBEDDING_UNAVAILABLE`, `EMBEDDING_DIMENSION_MISMATCH` eklendi (ikisi de iş gövdesinde zaten vardı; artık HTTP'de de dönüyor).
- `sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `rag-pipeline/references/indexing.md` (§6), `postgres-patterns`, `sql-optimization-patterns`, `spring-transactions`, `springboot-patterns`.

### Dependency, migration
Yok. `JdbcClient` mevcut `spring-jdbc`'den (data-jpa ile geliyor).

### Properties (`application.properties`, saf ASCII)
```
search.ef-search=40
search.max-scan-tuples=20000
```

### Sınıflar (kök `com.rasitbank.retrieval`; yeni paketlerde `package-info.java` `@NullMarked`)
**config/**
- `SearchProperties` (`@ConfigurationProperties("search")` record): `int efSearch`, `int maxScanTuples`. Mevcut properties sınıflarıyla aynı kayıt biçimi.

**search/domain/dto/**
- `SearchChunksRequest(@Nullable String query, @Nullable Integer topK)` record; Kararlar #10-#11. Mesajlar: `must not be blank`, `size must be at most 1000 characters`, `must not be null`, `must be between 1 and 50` (`@Min` ve `@Max` aynı mesaj).
- `SearchResponse(List<SearchHitResponse> hits)` + `static SearchResponse empty()`.
- `SearchHitResponse(long chunkId, int chunkNo, int pageNumber, String heading, String content, long sourceId, String sourceName, String version, double distance)`.

**search/controller/**
- `SearchController` (`@RestController`, `@RequestMapping("/api/workspaces/{workspaceId}/search")`): `@PostMapping(consumes = APPLICATION_JSON_VALUE) SearchResponse searchChunks(@PathVariable long workspaceId, @Valid @RequestBody SearchChunksRequest request)` → `searchService.searchChunks(workspaceId, request.query(), request.topK())` (doğrulamadan geçtiği için `Objects.requireNonNull`).

**search/service/**
- `SearchService` (`@Service`; constructor `WorkspaceService`, `SourceRepository`, `EmbeddingClient`, `ChunkSearchRepository`): `SearchResponse searchChunks(long workspaceId, String query, int topK)`; akış Kararlar #8. Süreler `System.nanoTime()` ile (`embeddingMs`, `sqlMs`, `durationMs`).

**search/repository/**
- `ChunkSearchRepository` (`@Repository`; constructor `JdbcClient`, `SearchProperties`): `@Transactional(readOnly = true) List<SearchHitResponse> findNearest(long workspaceId, float[] queryEmbedding, String embeddingModel, int topK)`. `static final String SEARCH_SETTINGS_SQL` (Kararlar #5, dört `set_config`, parametreli) ve `static final String NEAREST_CHUNKS_SQL` (Kararlar #4, adlı parametreler `:embedding`, `:workspaceId`, `:embeddingModel`, `:topK`); ikisi de paket görünürlüğünde, tester `EXPLAIN` testinde aynı metni kullanır. `static String toVectorLiteral(float[] embedding)` (Kararlar #7).

**source/repository/**
- `SourceRepository`: `boolean existsByWorkspaceIdAndActiveVersionIdIsNotNull(long workspaceId)` eklenir.

**Değişmeyenler:** `EmbeddingClient`, `WorkspaceService`, `GlobalExceptionHandler` (gövde doğrulaması ve `BaseException` eşlemesi hazır), içe alma hattı.

### Loglar (İngilizce, `key=value`; sorgu metni ve parça içeriği loglanmaz)
- `INFO Search completed workspaceId={} queryLength={} topK={} hitCount={} embeddingMs={} sqlMs={} durationMs={}` — her başarılı aramada bir kez. Bilgi bankası boşsa aynı satır `hitCount=0 embeddingMs=0 sqlMs=0` ile. `queryLength` kırpılmış sorgunun `length()`'i.
- Hata yolunda yeni log yok: `EmbeddingClient`'ın mevcut `WARN` satırları kalır; 503 handler'dan döner.

### `retrieval-api/CLAUDE.md` (backend-coder)
- Durum yorumu: TASK-006, arama ucu.
- "Teknoloji": `Hedef: search.default-ef-search…` satırı gerçek ayarlarla (`search.ef-search=40`, `search.max-scan-tuples=20000`, `set_config` dörtlüsü, tek SQL + `ARRAY(...)` filtresi) değişir; "Henüz olmayanlar"dan "arama ucu ve `search.*` ayarları" çıkar.
- "Kapsam": vektör arama maddesi `{query, topK}` (1-50), sorgu 1-1000 karakter, `mode` yok; güncel sürüm kuralı; boş bilgi bankasında model çağrılmaz.
- "Paket yapısı": `search/` (`SearchController`, `SearchService`, `ChunkSearchRepository`, DTO'lar), `config/SearchProperties`.
- "Komutlar": arama örneği `curl -s -H 'Content-Type: application/json' -d '{"query":"Kartımı kaybettim, ne yapmalıyım?","topK":5}' localhost:8080/api/workspaces/42/search`.
- "Kurallar"daki log örneği yeni satıra göre.

## Frontend planı
Yok (frontend-coder atlandı; TASK'ta ekran yok).

## Test senaryoları
Fixture'lar: `SearchFixtures` (SQL ile çalışma alanı 42'ye kaynak, iş, sürüm ve elle verilmiş vektörlü parçalar yazar; `source_versions.ingestion_job_id` zorunlu olduğu için her sürüme bir `DONE` iş; parça kimlikleri açıkça verilir). Vektörler 1024 boyutlu, birim eksenlerden kurulur: `e0`, `e1`, `0.8·e0 + 0.6·e1`. Sahte embedding `@MockitoBean EmbeddingClient` (`modelLabel()` → `bge-m3@1024`). Beklenen mesafeler elle: cos(e0, e0) = 1 → 0; cos(e0, 0.8e0+0.6e1) = 0.8 → 0.2; cos(e0, e1) = 0 → 1.0 (`within(1e-6)`).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T1 | Kırpma | unit `SearchServiceTest` | `givenQueryWithSurroundingSpaces_whenSearchChunks_thenEmbedsStrippedQueryKeepingCaseAndTurkishLetters` | `"  Kartımı KAYBETTİM  "` → `embed(["Kartımı KAYBETTİM"])` |
| T2 | Boş bilgi bankası | unit | `givenNoSourceWithActiveVersion_whenSearchChunks_thenReturnsNoHitsWithoutCallingEmbeddingModel` | `hits` []; `embeddingClient.embed` ve `chunkSearchRepository` hiç çağrılmadı |
| T3 | Model farkı | unit | `givenConfiguredModelDiffersFromWorkspace_whenSearchChunks_thenThrowsEmbeddingModelMismatchWithoutEmbedding` | `ConflictException`, `EMBEDDING_MODEL_MISMATCH`; `embed` çağrılmadı |
| T4 | Çalışma alanı yok | unit | `givenUnknownWorkspace_whenSearchChunks_thenThrowsWorkspaceNotFound` | `NotFoundException`, `WORKSPACE_NOT_FOUND`; `embed` çağrılmadı |
| T5 | Model kapalı | unit | `givenEmbeddingUnavailable_whenSearchChunks_thenThrowsEmbeddingUnavailableAndDoesNotQuery` | `ServiceUnavailableException`, `EMBEDDING_UNAVAILABLE`; `chunkSearchRepository` çağrılmadı |
| T6 | Repository'ye giden değerler | unit | `givenTopK5_whenSearchChunks_thenQueriesWithQueryVectorModelLabelAndTopK` | `findNearest(42, <embed'in döndüğü dizi>, "bge-m3@1024", 5)`; dönen liste `hits` olarak aynı sırayla |
| T7 | Log | unit (`OutputCaptureExtension`) | `givenThreeHits_whenSearchChunks_thenLogsLengthTopKAndHitCountButNotQuery` | `Search completed workspaceId=42 queryLength=33 topK=5 hitCount=3` var (`"Kartımı kaybettim, ne yapmalıyım?"` = 33 karakter); log `Kartımı` içermez |
| T8 | Başarılı yanıt | controller `SearchControllerTest` (`@WebMvcTest`, `@MockitoBean SearchService`) | `givenTwoHits_whenSearchChunks_thenReturns200WithAllHitFieldsInOrder` | 200, `application/json`; `hits[0]` 9 alanın hepsi fixture değerleriyle (`chunkId`, `chunkNo`, `pageNumber`, `heading`, `content`, `sourceId`, `sourceName`, `version`, `distance`); `hits[1]` ikinci sırada; `embedding` alanı yok |
| T9 | `topK: 0` (S2) | controller | `givenTopKZero_whenSearchChunks_thenReturns400ValidationFailedOnTopK` | 400, `application/problem+json`, `VALIDATION_FAILED`, `violations[0].field` = `topK`, mesaj `must be between 1 and 50`; servis çağrılmadı |
| T10 | `topK` sınırları | controller (parametreli) | `givenTopK_whenSearchChunks_thenOnly1To50Accepted` | 0, 51, -1 → 400 `topK`; 1 ve 50 → 200 |
| T11 | `topK` yok | controller | `givenMissingTopK_whenSearchChunks_thenReturns400MustNotBeNull` | 400, `topK` / `must not be null` |
| T12 | Boş sorgu | controller (parametreli) | `givenBlankQuery_whenSearchChunks_thenReturns400OnQuery` | `query` yok, `""`, `"   "` → 400, `query` / `must not be blank` |
| T13 | Sorgu uzunluğu | controller | `givenQueryOf1001Chars_whenSearchChunks_thenReturns400OnQuery` / `givenQueryOf1000TurkishChars_whenSearchChunks_thenReturns200` | 1001 × `a` → 400 `query` / `size must be at most 1000 characters`; 1000 × `ş` → 200 |
| T14 | İki ihlal | controller | `givenBlankQueryAndTopK51_whenSearchChunks_thenReturnsBothViolations` | `violations` alanları kümesi = {`query`, `topK`} (sıra serbest) |
| T15 | Biçim hataları | controller | `givenTextPlainBody_whenSearchChunks_thenReturns415` / `givenMalformedJson_whenSearchChunks_thenReturns400BadRequest` / `givenTopKAsText_whenSearchChunks_thenReturns400BadRequest` | 415 `UNSUPPORTED_MEDIA_TYPE`; 400 `BAD_REQUEST`; `{"query":"a","topK":"beş"}` → 400 `BAD_REQUEST` |
| T16 | Servis hataları | controller | `givenWorkspaceNotFound_whenSearchChunks_thenReturns404` (a) / `givenModelMismatch_whenSearchChunks_thenReturns409` (b) / `givenEmbeddingUnavailable_whenSearchChunks_thenReturns503` (c) | 404 `WORKSPACE_NOT_FOUND`; 409 `EMBEDDING_MODEL_MISMATCH`; 503 `application/problem+json` `EMBEDDING_UNAVAILABLE` |
| T17 | Mesafe sırası | integration `ChunkSearchRepositoryIntegrationTest` (Testcontainers, `SearchFixtures`) | `givenThreeChunks_whenFindNearestToE0_thenOrderedByCosineDistance` | Parçalar (yazılma sırası) `e1`, `0.8e0+0.6e1`, `e0`; sorgu `e0` → sıra `e0` (0.0), karışık (0.2), `e1` (1.0) |
| T18 | Eşit mesafe | integration | `givenTwoChunksWithSameEmbedding_whenFindNearest_thenLowerChunkIdFirst` | Kimlik 7002 önce, 7001 sonra yazılır, ikisi de `e0`; sonuç [7001, 7002] |
| T19 | `topK` | integration | `givenFiveChunks_whenFindNearestWithTopK2_thenTwoNearest` (a) / `givenFiveChunks_whenFindNearestWithTopK10_thenAllFive` (b) | (a) 2 sonuç, en yakın ikisi; (b) 5 sonuç (G1: sorgu hiçbir parçaya yakın olmasa da 2 ve 5 döner) |
| T20 | Güncel sürüm (K3a, K3c) | integration | `givenOldActiveVersionAndNewerUnactivatedVersion_whenFindNearest_thenOnlyActiveVersionChunksReturned` | Kaynak 10001: sürüm A (aktif, `2026.10`, parça `0.8e0+0.6e1`), sürüm B (`DONE` işi var ama `active_version_id` A'yı gösteriyor, parça `e0`; içe alma sürerken yazılmış yeni sürümün yerine geçer). Sorgu `e0`, `topK` 5 → tek sonuç, sürüm A'nın parçası, `version` `2026.10` |
| T21 | Güncel sürümü olmayan kaynak (K3b) | integration | `givenSourceWithoutActiveVersion_whenFindNearest_thenItsChunksAreNotReturned` | Kaynak X aktif (parça `e1`); kaynak Y `active_version_id` NULL ama sürümü ve parçası (`e0`) var. Sorgu `e0` → sadece X'in parçası |
| T22 | İki kaynak (S4) | integration | `givenTwoActiveSources_whenFindNearest_thenHitsComeFromBothWithTheirNamesAndVersions` | Kaynak 10001 "Rehber" `2026.10` (parça `e0`), kaynak 10002 "El Kitabı" `v1` (parça `0.8e0+0.6e1`); sorgu `e0` → [10001/"Rehber"/`2026.10`, 10002/"El Kitabı"/`v1`]; `heading`, `content`, `pageNumber`, `chunkNo` fixture'daki gibi |
| T23 | Model satırı | integration | `givenChunkOfAnotherEmbeddingModel_whenFindNearest_thenItIsNotReturned` | `embedding_model = 'other@1024'` olan aktif sürüm parçası (`e0`) dönmez |
| T24 | `EXPLAIN` (AC2) | integration | `givenSearchSettings_whenExplainNearestChunksQuery_thenPlanUsesHnswIndex` | Aynı transaction'da `SEARCH_SETTINGS_SQL` (40, `relaxed_order`, 20000, `off`) + `EXPLAIN NEAREST_CHUNKS_SQL`; plan metni `Index Scan using chunks_embedding_hnsw_idx` içerir |
| T25 | HTTP uçtan uca | integration `SearchHttpIntegrationTest` (`RANDOM_PORT`, Testcontainers, `@MockitoBean EmbeddingClient`) | `givenSeededChunks_whenPostSearch_thenReturnsHitsAndWritesNothing` (a) / `givenEmptyKnowledgeBase_whenPostSearch_thenReturnsNoHitsAndEmbeddingNotCalled` (b) / `givenEmbeddingUnavailable_whenPostSearch_thenReturns503ProblemDetail` (c) | (a) 200, `hits` fixture sırasıyla; `sources`, `source_versions`, `chunks`, `ingestion_jobs` satır sayıları aramadan önceki ve sonraki aynı (K7). (b) 200 `{"hits":[]}`, `embed` hiç çağrılmadı (S3). (c) 503, `application/problem+json`, `EMBEDDING_UNAVAILABLE` |
## Ölçüm soruları (AC1, M0)
Rehber 2026.10, `topK` 5. "Beklenen alt başlık" doğru parçayı bulmak için başlangıç noktasıdır; doğru parçalar Kararlar #14'e göre aramadan önce belirlenir.

| # | Soru | Beklenen alt başlık (rehber) |
|---|------|------------------------------|
| 1 | Kartımı kaybettim, ne yapmalıyım? | Kayıp ya da çalıntı bildirimi |
| 2 | Kredi kartımın yıllık üyelik ücreti iade edilir mi? | Yıllık üyelik ücreti, muafiyet ve iade |
| 3 | Asgari ödeme tutarını ödemezsem ne olur? | Asgari ödeme ve gecikme |
| 4 | Vadeli hesabımı vadesinden önce bozarsam faiz alır mıyım? | Vadeli mevduat |
| 5 | Kredimi erken kapatırsam tazminat öder miyim? | Erken ödeme ve erken kapama tazminatı |
| 6 | Hafta sonu EFT yapabilir miyim? | İşlem saatleri, hafta sonu ve resmî tatiller |
| 7 | Yanlış hesaba para gönderdim, geri alabilir miyim? | Yanlış gönderim ve iade |
| 8 | Mobil uygulama şifremi unuttum, nasıl yenilerim? | Şifre sıfırlama |
| 9 | Kartım ATM'de kaldı, ne yapmalıyım? | ATM'de yaşanan sorunlar |
| 10 | Şikâyetime ne kadar sürede cevap verilir? | Şikâyet kanalları ve cevap süresi |
| 11 | Hafta sonu döviz alırsam hangi kur uygulanır? | Döviz alım satımı ve hafta sonu kuru |
| 12 | Kiralık kasa ücreti ne kadar? | Şubede yapılan işlemler ve kiralık kasa |

- **M0 (backend-coder, dev ortamı):** dev postgres sıfırdan (`down -v` + `up -d`), Ollama'da `bge-m3`, servis açık. Rehber 2026.10 `curl -F` ile yüklenir (`retrieval-api/CLAUDE.md` komutu), iş `DONE`. Raporda:
  1. Her soru için: seçilen cevap cümlesi, doğru parça kimlikleri (aramadan önce), 5 sonucun `chunkId` / `heading` / `distance`'ı, ilk doğru parçanın sırası ya da "yok". Toplam: kaç soruda doğru parça ilk 5'te. Soru 1'de ilk sonucun başlığı (S1).
  2. Aynı `SEARCH_SETTINGS_SQL` değerleriyle bir transaction'da `EXPLAIN (ANALYZE, BUFFERS)` + `NEAREST_CHUNKS_SQL` (soru 1'in vektörüyle) çıktısı; `Index Scan using chunks_embedding_hnsw_idx` satırı görünür.
  3. Ollama durdurulmuşken (ya da `OLLAMA_BASE_URL=http://127.0.0.1:9`) arama → 503 `EMBEDDING_UNAVAILABLE`, `curl -w '%{time_total}'` süresi.
  4. İkinci belge: TASK-027'deki yer imli Belge B (TASK-027 planı "Deneme belgeleri" adımıyla üretilir) yüklenir; RAG konulu bir sorgu (`"Embedding nedir?"`, `topK` 5) ve soru 1 tekrar çalıştırılır; her sonucun `sourceName`'i raporda (S4).
  5. `Search completed` log satırlarından iki örnek; logda soru metni olmadığı (`grep -c 'Kartımı'` → 0).
- **M1 (qa):** M0 ortamında ya da kendi kurduğu dev ortamında soru 1, 6 ve 11'i tekrarlar ve M0'ın sıralarıyla karşılaştırır; `EXPLAIN`'i tekrar alır; boş bilgi bankasında arama → `{"hits":[]}`; `topK: 0` → 400; logda sorgu metni yok. Sözleşmedeki alan adları ve status kodları gerçek yanıtlarla karşılaştırılır. Kodda arama kaydı yazan bir yer ve yeni migration olmadığı diff'ten doğrulanır.

## Deployment planı
Yok. Yeni ayarların varsayılanı `application.properties`'te; Compose, ortam değişkeni ve lokal model değişmez (`bge-m3` TASK-003'ten).

## Açık sorular ve riskler
1. **Kart sorusu 1 (sorgu üst sınırı):** sözleşme ve plan önerilen 1000 karakterle yazıldı. Kullanıcı başka bir değer seçerse `SearchChunksRequest.QUERY_MAX_LENGTH`, sözleşmedeki `maxLength`/mesaj ve T13 o değere göre değişir.
2. **Askıda kalan model sunucusu (risk):** Sunucu kapalı değil de cevap vermiyorsa `EmbeddingClient` 60 sn okuma zaman aşımı × 3 deneme yüzünden 503 3 dakikaya kadar gecikebilir. TASK-008'de asistan bu süre boyunca bekler.
3. **İçe alma sürerken gecikme (risk):** Arama ve içe alma aynı model sunucusunu ve aynı istek sınırını (2) paylaşır; içe alma sürerken arama birkaç saniye bekleyebilir. İlk aramada model belleğe yükleniyorsa da birkaç saniye sürer.
4. **Kapsam dışı not:** `.claude/skills/rag-pipeline/SKILL.md` §3, `rag-pipeline/references/indexing.md` §6 ve `problem-details-rfc9457` tablosu arama gövdesini `{query, mode, limit}` ve "`limit` 1-50" diye anıyor. Sözleşme `{query, topK}` oldu; bu dosyalar kullanıcının (`.claude/**`), onayla güncellenebilir.
5. **HNSW yaklaşık arama (risk):** İndex en yakın parçalardan birini nadiren kaçırabilir; 154 parçada ve `ef_search` 40'ta bu pratikte görülmez. Sıralama testi (T17-T19) küçük veriyle bunu ölçmez; M0 ölçümü gerçek veride gösterir.

STATUS: DONE
