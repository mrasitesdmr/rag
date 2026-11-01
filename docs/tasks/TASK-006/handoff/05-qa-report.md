TASK: TASK-006
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`, `java-code-review`, `springboot-verification`, `rag-pipeline` (indexing §6), `writing-good-tests`.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç ✅/❌ |
|---|-----------------------|--------------------|-----------|
| K1 | İstek: sorgu + `topK` | `SearchChunksRequest`, `SearchController`; `SearchControllerTest#givenTwoHits...`; canlı 200 | ✅ |
| K2 | `topK` 1-50; boş / >1000 karakter sorgu → `VALIDATION_FAILED` | Bean Validation (`SearchChunksRequest`); controller testleri (0, -1, 51, boş, 1001, iki ihlal); canlı: `topK:0` → 400 `violations[0].field=topK`, `{" ",51}` → iki ihlal, `topK` yok → 400 | ✅ |
| K3a | Her kaynağın sadece güncel sürümü | `NEAREST_CHUNKS_SQL` (`source_version_id = ANY(ARRAY(... active_version_id ...))`); `ChunkSearchRepositoryIntegrationTest#givenOldActiveVersionAndNewerUnactivatedVersion...` | ✅ |
| K3b | `DONE` sürümü olmayan kaynak girmez | aynı filtre (`active_version_id IS NOT NULL`); `#givenSourceWithoutActiveVersion...` | ✅ |
| K3c | İçe alma sürerken eski sürüm döner | `active_version_id` sadece WRITING commit'inde değişir (mevcut kod); tek SQL = tek snapshot; K3a testi | ✅ |
| K4a | Sonuç alanları (9 alan) | `SearchHitResponse`; `SearchControllerTest#...AllHitFieldsInOrder`; canlı yanıtta alanlar sözleşmeyle aynı | ✅ |
| K4b | Mesafe artan, eşitte `chunkId` | dış `ORDER BY nearest.distance, nearest.id`; `#givenThreeChunks...OrderedByCosineDistance`, `#givenTwoChunksWithSameEmbedding...LowerChunkIdFirst`; canlı 12 sorguda mesafe sıralı (kendi betiğimle assert) | ✅ |
| K5 | `ef_search` 40, index kullanılır | `SearchProperties`/`application.properties` (40, 20000); `#givenFindNearestInsideTransaction...HnswSettingsAreApplied`; kendi EXPLAIN'im (aşağıda) | ✅ |
| K6 | Model erişilemez → 503 sabit kod | `EmbeddingClient` → `ServiceUnavailableException`; `SearchServiceTest`, `SearchControllerTest`, `SearchHttpIntegrationTest#...503ProblemDetail`; canlı: `OLLAMA_BASE_URL=http://127.0.0.1:9` → 503 `EMBEDDING_UNAVAILABLE`, 3,17 sn | ✅ |
| K7 | Arama kaydı yok | `search/` altında INSERT/save/update yok (grep boş); yeni migration yok (V1-V5 aynı, `git status`); `SearchHttpIntegrationTest#...WritesNothing` (satır sayıları) | ✅ |
| K8 | Sorgu loglanmaz; uzunluk, topK, sonuç sayısı, süre loglanır | `SearchService.logCompleted`; `SearchServiceTest#...LogsLengthTopKAndHitCountButNotQuery`; canlı log: `Search completed workspaceId=42 queryLength=45 topK=5 hitCount=5 embeddingMs=19 sqlMs=4 durationMs=29`; `grep -c 'Kartımı\|Hafta sonu'` app.log → 0 | ✅ |
| S1 | Kayıp kart sorusu → kayıp/çalıntı bölümü | canlı: ilk iki sonuç 29, 30 "2.16 Kayıp ya da çalıntı bildirimi"; 29 içeriği "Kartınızı kaybettiyseniz ... bildirin" | ✅ |
| S2 | `topK:0` → 400 `topK` | canlı + `#givenTopKZero...` | ✅ |
| S3 | Boş bilgi bankası → boş liste | `SearchServiceTest#givenNoSourceWithActiveVersion...`, `SearchHttpIntegrationTest#givenEmptyKnowledgeBase...` (200 `{"hits":[]}`, embed çağrılmadı). Canlı denenmedi (veri silmek gerekirdi; lead yasakladı) | ✅ (test kapsamı) |
| S4 | İki belge, her sonuç belgesini taşır | `#givenTwoActiveSources...`; canlı: "Embedding nedir?" → 5 sonuç 10002 (RAG anlatımı); rehber soruları → 10001; backend'in karışık sorgusu iki kaynağı birlikte döndürdü (M0) | ✅ |
| AC1 | ≥10 soru gerçek modelle ölçülmüş, raporda | Backend raporunda 12 soru; ben aynı 12 soruyu canlı yeniden çalıştırdım: **12/12 ilk 5'te**, 11'i 1. sırada, soru 6 2. sırada; sıralar M0 ile birebir aynı (aşağıda) | ✅ |
| AC2 | EXPLAIN HNSW'yi gösterir | kendi EXPLAIN'im: `Index Scan using chunks_embedding_hnsw_idx on chunks chunks_1`, `Order By: (embedding <=> ...)`, filtre index taramasında; `#givenSearchSettings_whenExplain...` | ✅ |
| AC3 | Sıralama, filtreleme, doğrulama testleri; `verify` geçer | 4 test sınıfı (46 yeni test); `verify` benim koşumda geçti | ✅ |

## Sözleşme uyumu
`openapi.yaml` `searchChunks` ↔ kod, elle karşılaştırıldı (springdoc yok):
- Path `POST /api/workspaces/{workspaceId}/search`, `consumes=application/json` → aynı. Gövde `{query, topK}`; `mode` yok; bilinmeyen alan yok sayılır (test var).
- Yanıt `{hits:[...]}`; `SearchHitResponse` alan adları (`chunkId, chunkNo, pageNumber, heading, content, sourceId, sourceName, version, distance`) sözleşmeyle birebir; `embedding` dönmüyor. Canlı yanıtlarda doğrulandı.
- Status / `errorCode`: 400 `VALIDATION_FAILED` (mesajlar `must not be blank`, `size must be at most 1000 characters`, `must not be null`, `must be between 1 and 50` → sözleşmeyle aynı), 404 `WORKSPACE_NOT_FOUND`, 409 `EMBEDDING_MODEL_MISMATCH` (unit + controller testi), 415 `UNSUPPORTED_MEDIA_TYPE`, 503 `EMBEDDING_UNAVAILABLE` canlı doğrulandı. `EMBEDDING_DIMENSION_MISMATCH` (503) mevcut `EmbeddingClient` yolundan; `ProblemDetail.errorCode` enum'unda var.
- Kontrol sırası 400 → 404 → 409 → boş bilgi bankası → 503: `SearchService` akışı aynı.
- Sınırlar (1000, 1-50) kodda ve sözleşmede aynı.
- Tüketici (assistant-api istemcisi) bu task'ta yok (TASK-008); web yok.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` | BUILD SUCCESS, Tests run: 257, Failures 0, Errors 0, Skipped 0; "All coverage checks have been met." | Evet (03-test-report: 257) |
| `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS; modül: 560 mutant, 457 öldü (%82), test strength %89 | Evet (03: 432+... modül geneli aynı büyüklükte; `search.*` aynı) |
| `npm run build/lint/test` | Uygulanamaz (web değişmedi) | — |
| assistant-api `verify` | Uygulanamaz (değişmedi) | — |
| `git status` | Sadece retrieval-api, `docs/api/openapi.yaml` ve handoff/dokümanlar değişti; dependency (`pom.xml`) ve migration değişikliği yok | Evet |

## Mutation testi
PIT 1.30.1, tüm modül. `search.*` kapsamında **20 mutant, 18 öldü (%90), 2 hayatta**:
- `ChunkSearchRepository.java:80` StringBuilder kapasite çarpımı → bölme: davranışa etkisi yok (eşdeğer mutant).
- `SearchService.java:67` `elapsedMs` dönüşü 0: yalnız log süresi; zamana bağlı, kural değil.
İkisi de kabul edilebilir; kural kapsayan mutant hayatta kalmadı. Modül geneli (önceki task'ların kodu dahil) %82; bu task'ın kapsamı dışı.

## Uçtan uca
Playwright/web bu task'ta yok. Gerçek `bge-m3` ile canlı deneme (retrieval-api 8090'da, dev postgres'te rehber 2026.10 + RAG anlatımı, Ollama `bge-m3`):

| Soru | İlk doğru parçanın sırası | Sonuç chunkId'leri (mesafe artan) |
|------|---------------------------|-----------------------------------|
| 1 Kartımı kaybettim | 1 | 29, 30, 108, 28, 130 |
| 2 Yıllık üyelik ücreti | 1 | 9, 36, 154, 35, 142 |
| 3 Asgari ödeme | 1 | 15, 16, 17, 149, 76 |
| 4 Vadeli hesap bozma | 1 | 55, 40, 41, 53, 76 |
| 5 Erken kapama tazminatı | 1 | 72, 76, 71, 36, 16 |
| 6 Hafta sonu EFT | 2 | 79, 85, 94, 116, 80 |
| 7 Yanlış hesaba para | 1 | 93, 94, 130, 32, 83 |
| 8 Mobil şifre | 1 | 110, 99, 28, 98, 108 |
| 9 ATM'de kart | 1 | 130, 127, 135, 29, 129 |
| 10 Şikâyet süresi | 1 | 140, 136, 132, 31, 131 |
| 11 Hafta sonu döviz kuru | 1 | 116, 27, 85, 151, 122 |
| 12 Kiralık kasa | 1 | 125, 148, 126, 135, 42 |

Toplam 12/12. "Doğru parça" kümeleri backend-coder'ın M0 tablosundan (arama öncesi belirlenmiş, Kararlar #14) alındı; ben sadece soru 1'in parçasını (29) içerik olarak kontrol ettim, diğer kümeleri bağımsız yeniden türetmedim. Bu ölçüm golden set değildir. "Embedding nedir?" → 224, 213, 225, 214, 220, hepsi 10002 "RAG anlatımı".
- 503: `OLLAMA_BASE_URL=http://127.0.0.1:9` ile ikinci instance → 503 `EMBEDDING_UNAVAILABLE`, `application/problem+json`, 3,17 sn.
- Hata yanıtları: 400 (üç biçim), 404 (çalışma alanı 7), 415 canlı denendi.
- Boş bilgi bankası canlı denenmedi (veri silme yasağı); Testcontainers'lı HTTP testi (`SearchHttpIntegrationTest`) `verify` içinde geçti.
- Dil modeli cevabı yok (bu task retrieval); cevap kalitesi/uydurma TASK-008 sonrası.
- Kullandığım instance'lar durduruldu; dev postgres verisine dokunulmadı (EXPLAIN `BEGIN READ ONLY` + `ROLLBACK`).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional | `SearchService.java:67` | `elapsedMs` mutantı hayatta (log süresi). Zamanı `Clock`/yerine test edilebilir kılmak mümkün ama değer düşük | tester |
| Nit | `ChunkSearchRepository.java:80` | StringBuilder kapasite sabiti (`* 12`) anlamsız bir sayı gibi okunuyor; kaldırılabilir | backend-coder |
| FYI | `.claude/skills/rag-pipeline`, `indexing.md` §6, `problem-details-rfc9457` | Hâlâ `{query, mode, limit}` diyor; sözleşme `{query, topK}` (plan risk 4). Kullanıcı onayıyla güncellenir | lead |
| FYI | `EmbeddingClient` | Model askıdayken 503 3 dakikaya kadar gecikebilir (plan risk 2); TASK-008'de ölçülecek | — |
| FYI | `SearchService` | `modelLabel()` doğrulamalardan önce çağrılıyor ama dış çağrı değil; sorun yok | — |

Critical/Required bulgu yok. Gözlenen iyi uygulamalar: tek SQL = tek snapshot, `set_config(..., true)` ile transaction'a bağlı ayar, dış çağrı transaction dışında, beklenen değerler elle türetilmiş vektörlerden.

## Düzeltme
—

STATUS: DONE
