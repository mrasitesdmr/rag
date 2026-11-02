TASK: TASK-016
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions` (test adı, AAA), `spring-testing-fundamentals`, `spring-mvc-testing`, `rag-evaluation`, `rag-pipeline`, `sse-streaming`, `frontend-conventions`, `react-testing`.

## Önce onarılanlar
| Kırık | Onarım |
|---|---|
| web: `previewFixtures.contextChunk`, `conversationFixtures.turnChunk` zorunlu `containsAnswer` eksikti (tsc hatası, 30 Önizleme testi) | Varsayılan `containsAnswer: false` eklendi; build ve eski 587 test yeşil |
| assistant-api: `AnswerServiceTest`, `AnswerStreamerTest`, `TurnStatusResolverTest` derlenmiyordu | Yeni imzalar (`GoldenAnswerLocator` mock'u, `streamModelAnswer(..., answerChunkIds)`, `resolve(turn, Set)`) |
| `AssistantApplicationIntegrationTest` 6 migration | 7 migration (adı `AllSevenMigrations`, V7 assertion eklendi) |

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|---|---|---|
| B1-B3 | Metrik (recall@5, MRR), elle türetilmiş | `RetrievalMetricsTest` (B1: 0.8/0.55; B2: 0/0; B3: 1/0.333333) + 100 soruluk (0.98 / 0.915) + boş liste ve 0. sıra hata | geçti |
| B4-B6 | Vector sırası / prompt yeri | `AnswerChunkRankerTest` (vector7→prompt2; iki parça: 3/1; adaylarda yok: null/null; adayda var prompt'ta yok) | geçti |
| B7-B12 | Başlatma kontrolleri | `EvalRunServiceTest` (LLM_ONLY, set yok, süren, hiç bulunamadı, retrieval kapalı, 3/4 eşleşme, sürüm birleştirme, index ihlali→409, başka ihlal yükselir, TOP_K) | geçti |
| B13, B14 | Fallback sayısı, arama hatası | `EvaluationIntegrationTest#givenRerankFallbackOnTwoQuestions…`, `#givenSearchFailsOnSecondQuestion…`, `#givenUnexpectedFailure…` (gerçek DB; dil modeli etkileşimsiz) | geçti |
| B15 | Top-5'te 20 aday | `ContextRetrieverTest#givenTopKStrategy_whenRetrieveCandidatesAndContext…` (+ rerank, fallback) | geçti |
| B16 | Bitmiş kayıt, süre | `EvaluationIntegrationTest#givenFinishedRun_whenGetEvalRun…` (ilerletilebilir saat, 3000 ms) | geçti |
| B17 | Açılışta yarım ölçüm | `#givenRunningRunAtStartup…` + `#givenRunningRun_whenFailInterruptedRuns…` | geçti |
| B18 | Eşzamanlı başlatma (G1) | `#givenTwoConcurrentStarts…` (×3) | geçti |
| B19, B20, B20b | Golden set yükleme | `#givenSameVersionDifferentContent…`, `#givenRunningRun_whenReplaceGoldenSet…`, `#givenSameVersionSameContent…`, `#givenNewVersion…`, eşzamanlı yükleme (×3) | geçti |
| B21 | Doğrulama | `GoldenSetControllerTest` (üç ihlal alan yoluyla; 20/300/301 sınırları; version/questions) | geçti |
| B22-B25 | Tur durumu | `TurnStatusResolverTest` (GENERATION_ERROR, MISSED, DONT_KNOW, INCOMPLETE, golden olsa da GROUNDED) | geçti |
| B26-B28 | Locator (G5) | `GoldenAnswerLocatorTest` (harf/noktalama, tek kelime fazla → çağrı yok, retrieval kapalı → boş, + 4 ek) | geçti |
| B29-B31 | Konuşmalar işaretleri (G3) | `ConversationGoldenMarksIntegrationTest` (MISSED + GENERATION_ERROR/satır işareti, golden değil, retrieval kapalı detay ve liste, liste son tur) | geçti |
| B32, B33 | SSE işareti | `SendMessageIntegrationTest#givenGoldenQuestion…`, `#givenAnswerLookupFails…`, `#givenQuestionNotInGoldenSet…`; `AnswerStreamerTest#givenAnswerChunkIds…`; `AnswerServiceTest` (locator sadece RAG + dolu bağlam) | geçti |
| B34 | Uçlar | `EvalRunControllerTest` (202 + Location, 409, abc→400 BAD_REQUEST, 999→404, size=101, sayfa, overview) ; `GoldenSetControllerTest#givenNoGoldenSet…` | geçti |
| B35 | Overview, 24 sınırı | `EvaluationIntegrationTest#givenRunningAndDoneRuns_whenGetEvalOverview…` (+ liste sırası, bilinmeyen id) | geçti |
| — | `RetrievalClient.findChunkMatches` | `RetrievalClientTest` (istek/yanıt, eksik eleman → RETRIEVAL_UNAVAILABLE, hata kodları) | geçti |
| R1-R7 | retrieval-api | `ChunkServiceIntegrationTest` (R1, R2 (+aynı kaynakta eski sürüm), R3, R4, R6, R7, I/İ), `ChunkControllerTest` (R5, 501 metin, WORKSPACE_NOT_FOUND, gövde) | geçti |
| W1-W4 | Set yok, LLM_ONLY, düğme metni, ölçüm yok | `EvaluationPage.test.tsx` | geçti |
| W5, W6 | Kutular ve eşik (0,947→kırmızı "0,95"; 0,95→yeşil), alt yazılar | `EvaluationPage.test.tsx` | geçti |
| W7, W8 | Kaçanlar tablosu, satırdan Önizleme (StrictMode, 1 konuşma + 1 mesaj) | `EvaluationPage.test.tsx` | geçti |
| W9, W10 | Yoklama ve durma, yenileme | `EvaluationPage.test.tsx` (gerçek 1 sn zamanlayıcı) | geçti |
| W11, W12 | Başlatma hataları (5 kod), başarısız son ölçüm (3 neden) | `EvaluationPage.test.tsx` | geçti |
| W13 | 21 çalıştırma, sayfa 2 | `EvaluationPage.test.tsx` | geçti |
| W14 | Önizleme sarı + açıklama | `PreviewPage.test.tsx` (+ işaret yoksa "sarı" yok); `answerStreamEvents.test.ts` (boolean olmayan `containsAnswer` geçersiz) | geçti |
| W15 | Konuşmalar etiket/çipler | `ConversationDetailPage.test.tsx` (KAÇIRDI, ÜRETİM HATASI, "cevap burada" tek satırda) | geçti |
| W16 | axe | `EvaluationPage.test.tsx` | geçti |
| K4 | Metrik elle türetilmiş, build'ler geçer | yukarıdakiler + komutlar | geçti |

TASK.md'de planda testi unutulmuş kural bulunmadı.

## Mutation
- **web (Stryker):** Kurulu değil.
- **assistant-api (PIT, `evaluation.service.*`):** 94 mutant, 83 öldü (%88), test gücü %91. İlk koşudan sonra kapatılanlar: `failInterruptedRuns` sayısı, `completeRun`/`failRun` dönüşleri, `lockForReplace` (eşzamanlı yükleme testi). Kalan: `contentHash` içindeki `index > 0` sınırı (eşdeğer; hash yine deterministik), `GoldenAnswerLocator` döngü sınırı (eşdeğer), `lambda$…getRun$0` gibi orElseThrow lambda'ları (kapsama yok, sadece olmayan kayıt), `runEvalRun` çağrısının silinmesi TIMED_OUT (öldü sayılır).
- **retrieval-api:** Yeni kod için PIT çalıştırılmadı (sadece sorgu+servis; davranış gerçek PostgreSQL'e karşı 8 senaryoyla sınandı).

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw verify` (OLLAMA_BASE_URL kapalı adres): **441 test, 0 başarısız**, coverage eşiği geçti, BUILD SUCCESS.
- `cd retrieval-api && ./mvnw verify`: **344 test, 0 başarısız**, coverage eşiği geçti, BUILD SUCCESS.
- `cd web && npm run build`: geçti. `npm run lint`: geçti. `npm test`: **36 dosya, 620 test, hepsi geçti**. `npm run test:coverage`: satır %97,32 (eşik %80).

## Çalıştırılamayanlar
- Uçtan uca (Playwright): çalışan sistem gerektirir; bu turda e2e dosyası yazılmadı/çalıştırılmadı, qa gerçek sistemde bakar.
- Gerçek model ile ölçüm (recall 0,98 / 0,89): backend-coder raporunda; sahte modelle yeniden üretilmedi.

## Bulgular (uygulama hatası değil, bilgi)
- `GoldenAnswerLocator`, `RetrievalClient` null döndürürse (yalnızca sahte) NPE verir; gerçek istemci hiç null döndürmez, düzeltme istenmedi.

STATUS: DONE
