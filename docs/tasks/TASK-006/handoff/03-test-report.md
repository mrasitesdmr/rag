TASK: TASK-006
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-mvc-testing`.

## Senaryo → test eşlemesi
Dosyalar `retrieval-api/src/test/java/com/rasitbank/retrieval/` altında. U = `search/service/SearchServiceTest`, C = `search/controller/SearchControllerTest`, R = `search/repository/ChunkSearchRepositoryIntegrationTest`, H = `search/repository/SearchHttpIntegrationTest`. Fixture: `fixture/SearchFixtures`, taban sınıf `search/repository/AbstractSearchIntegrationTest`.

| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| T1 / G4 | Kırpma, harf ve Türkçe karakter korunur | U#givenQueryWithSurroundingSpaces_whenSearchChunks_thenEmbedsStrippedQueryKeepingCaseAndTurkishLetters | geçti |
| T2 / S3 / G3 | Boş bilgi bankası, model çağrılmaz | U#givenNoSourceWithActiveVersion_...ReturnsNoHitsWithoutCallingEmbeddingModel; H#givenEmptyKnowledgeBase_whenPostSearch_thenReturnsNoHitsAndEmbeddingNotCalled (T25b) | geçti |
| T3 / G5 | Model farkı 409, embed yok | U#givenConfiguredModelDiffersFromWorkspace_...ThrowsEmbeddingModelMismatchWithoutEmbedding | geçti |
| T4 | Çalışma alanı yok 404 | U#givenUnknownWorkspace_whenSearchChunks_thenThrowsWorkspaceNotFound | geçti |
| T5 / K6 | Model kapalı, repository çağrılmaz | U#givenEmbeddingUnavailable_...DoesNotQuery | geçti |
| T6 / K1 | Repository'ye giden değerler ve sıra | U#givenTopK5_whenSearchChunks_thenQueriesWithQueryVectorModelLabelAndTopK | geçti |
| T7 / K8 | Log: uzunluk, topK, hitCount; sorgu yok | U#givenThreeHits_...LogsLengthTopKAndHitCountButNotQuery (ek: durationMs sayısal ve makul) ; U#givenEmptyKnowledgeBase_...LogsZeroHitsAndZeroModelTime (eklendi) | geçti |
| T8 / K4a | 9 alan, sıra, `embedding` alanı yok | C#givenTwoHits_whenSearchChunks_thenReturns200WithAllHitFieldsInOrder | geçti |
| T9 / S2 | `topK: 0` → 400 `topK` | C#givenTopKZero_whenSearchChunks_thenReturns400ValidationFailedOnTopK; H#givenTopKZero_whenPostSearch_thenReturns400AndEmbeddingNotCalled | geçti |
| T10 / K2 | topK sınırları (0, -1, 51, 1000 → 400; 1, 50 → 200) | C#givenTopKOutsideRange_...; C#givenTopKAtBoundary_whenSearchChunks_thenReturns200 | geçti |
| T11 | topK yok | C#givenMissingTopK_whenSearchChunks_thenReturns400MustNotBeNull | geçti |
| T12 | Boş sorgu (yok, "", "   ") | C#givenBlankQuery_whenSearchChunks_thenReturns400OnQuery | geçti |
| T13 | 1001 → 400, 1000 `ş` → 200 | C#givenQueryOf1001Chars_...; C#givenQueryOf1000TurkishChars_... | geçti |
| T14 | İki ihlal | C#givenBlankQueryAndTopK51_whenSearchChunks_thenReturnsBothViolations | geçti |
| T15 | 415, bozuk JSON, `"topK":"beş"` | C#givenTextPlainBody_...Returns415; C#givenMalformedJson_...; C#givenTopKAsText_... | geçti |
| T15+ (Karar #1) | Bilinmeyen `mode` alanı yok sayılır | C#givenUnknownExtraField_whenSearchChunks_thenIgnoresItAndReturns200 (eklendi) | geçti |
| T16 | 404 / 409 / 503 eşlemesi | C#givenWorkspaceNotFound_...Returns404; C#givenModelMismatch_...Returns409; C#givenEmbeddingUnavailable_...Returns503 | geçti |
| T17 / K4b | Mesafe sırası, değerler 0 / 0.2 / 1.0 (`within(1e-6)`) | R#givenThreeChunks_whenFindNearestToE0_thenOrderedByCosineDistance | geçti |
| T18 / K4b | Eşit mesafede küçük kimlik önce | R#givenTwoChunksWithSameEmbedding_whenFindNearest_thenLowerChunkIdFirst | geçti |
| T19 / G1 / G2 | topK 2 → 2; topK 10 → 5 (uzak sorguda da) | R#givenFiveChunks_whenFindNearestWithTopK2_thenTwoNearest; R#givenFiveChunks_whenFindNearestWithTopK10_thenAllFive | geçti |
| T20 / K3a / K3c | Sadece aktif sürüm | R#givenOldActiveVersionAndNewerUnactivatedVersion_...OnlyActiveVersionChunksReturned | geçti |
| T21 / K3b | Aktif sürümü olmayan kaynak girmez | R#givenSourceWithoutActiveVersion_...ItsChunksAreNotReturned | geçti |
| T22 / S4 | İki kaynak, ad ve sürüm | R#givenTwoActiveSources_...HitsComeFromBothWithTheirNamesAndVersions | geçti |
| T23 | Başka embedding modeli satırı dönmez | R#givenChunkOfAnotherEmbeddingModel_whenFindNearest_thenItIsNotReturned | geçti |
| T24 / K5 / AC2 | `EXPLAIN` HNSW index'ini gösterir | R#givenSearchSettings_whenExplainNearestChunksQuery_thenPlanUsesHnswIndex | geçti |
| K5 (ek) | `findNearest` ayarları (40, relaxed_order, 20000, off) gerçekten uygular | R#givenFindNearestInsideTransaction_whenReadingSessionSettings_thenHnswSettingsAreApplied (PIT'in gösterdiği boşluk) | geçti |
| ek | Başka çalışma alanı kimliği sonuç vermez | R#givenOtherWorkspaceId_whenFindNearest_thenNothingReturned | geçti |
| ek | Vektör literal biçimi | R#givenEmbedding_whenToVectorLiteral_thenCommaSeparatedWithoutSpaces | geçti |
| T25a / K7 | 200, fixture sırası ve mesafeler; tablo satır sayıları aynı | H#givenSeededChunks_whenPostSearch_thenReturnsHitsAndWritesNothing | geçti |
| T25c / K6 | 503 `application/problem+json` `EMBEDDING_UNAVAILABLE` | H#givenEmbeddingUnavailable_whenPostSearch_thenReturns503ProblemDetail | geçti |
| AC1, S1, M0/M1 | Gerçek modelle ölçüm | tester kapsamı dışı (backend M0, qa M1) | — |

**Regresyon kilidi:** yok. Beklenen değerlerin hepsi plandaki elle türetilmiş vektörlerden (e0, e1, 0.8e0+0.6e1 → 0, 1, 0.2), literal kimlik ve metinlerden gelir; kodun çıktısı beklenen değer yapılmadı. İstisna sayılabilecek tek yer `distance` değerlerinin HTTP testinde (0.0 ve 0.2) aynı elle türetilmiş cosine değeri olması.

Uygulama kodunda hata bulunmadı.

## Mutation
PIT 1.30.1, tüm modül (`./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`). Kapsamda `search.*` mutantları:
- İlk koşu: 20 mutant, 16 öldü, 4 hayatta.
- Eklenen testlerle son koşu: **20 mutant, 18 öldü (%90), 2 hayatta.**
- Kapatılanlar: `applySearchSettings` çağrısının silinmesi (yeni ayar testi), `elapsedMs` çıkarma → toplama (log süresi aralık testi).
- Hayatta kalan 2 mutant task kuralı değildir:
  - `ChunkSearchRepository.toVectorLiteral` satır 80: `StringBuilder` başlangıç kapasitesi çarpımı, davranışa etkisi yok (eşdeğer mutant).
  - `SearchService.elapsedMs` "dönüşü 0 yap": sadece log süresi; süre değeri için bağımsız beklenen değer türetilemez (zamana bağlı), kural değil.
- Modül geneli (bu task dışı kod dahil): 432 öldü, 55 hayatta, 48 kapsamsız, 24 zaman aşımı, 1 bellek hatası; bunlar önceki task'ların kodu, bu task'ta ele alınmadı.

## Komutlar ve sonuçları
- `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` (coverage eşiği açık) → **BUILD SUCCESS**, Tests run: 257, Failures: 0, Errors: 0, Skipped: 0; "All coverage checks have been met." Önceki 211 test + 46 yeni (SearchServiceTest 8, SearchControllerTest 22, ChunkSearchRepositoryIntegrationTest 13, SearchHttpIntegrationTest 4; parametreli çalıştırmalar dahil).
- PIT: yukarıdaki gibi.

## Çalıştırılamayanlar
Yok (Docker açıkken Testcontainers dahil hepsi çalıştı). Gerçek `bge-m3` ile ölçüm (AC1, S1) testlerin değil backend M0 ve qa M1'in işidir. Frontend ve e2e bu task'ta yok.

## Düzeltme
—

STATUS: DONE
