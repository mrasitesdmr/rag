TASK: TASK-011
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `react-testing`, `frontend-conventions`.

## Mevcut testlerin uyarlanması
- assistant-api: `TestProperties` (yeni `Reranker`/`Generation` bileşenleri, `withMode(mode, strategy)`), `TurnDraft` çağrıları (`GenerationMode.RAG`), `PromptBuilder` ve `ContextRetriever` yeni constructor'ları, `AssistantPropertiesTest` BASE'i. `PostgresIntegrationTest` `assistant.reranker.health-check-enabled=false` verir (`RerankClient`'ı sahtelemeyen `@SpringBootTest`'ler lokal 8081'e gitmez). `AssistantApplicationIntegrationTest` artık 3 migration bekler.
- web: `previewFixtures.ts` (`generationMode: 'RAG'`, Sadece LLM fixture'ı), `fixtures.ts#liveConfig`, `server.ts` varsayılan `GET /api/assistant/live-config` handler'ı, yeni `liveConfigHandlers.ts#serveLiveConfig` (istek sayar). Eski sabit "Asistan nasıl cevaplıyor?" testi yapılandırmadan gelen yeni metne çevrildi (beklenen metin plandan, kodtan değil).

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| B1 (K1) | Mod ayarı | `AssistantPropertiesTest#givenApplicationPropertiesDefaults_whenBound_thenRagModeAnd15SecondHealthCheck` (gerçek `application.properties`), `#givenModeLlm_whenBound_thenLlmOnly`, `#givenModeFoo_whenBound_thenContextFails` | Geçti |
| B2 (K2a, S3, G4) | LLM: yeniden yazma/retrieval yok, bilgi bankası boş olsa da model çağrılır | `AnswerServiceTest#givenLlmOnlyMode_whenSendMessage_thenNoRewriteNoRetrieval`, `#givenLlmOnlyMode_whenSendMessageWithNoDocuments_thenModelStillCalledNotFixedAnswer`, `#givenLlmOnlyAndTurnInProgress_...` , `#givenRagMode_whenSendMessage_thenDraftCarriesRagMode` | Geçti |
| B3 (K2a) | LLM prompt (lab metni literal, `<parcalar>` yok) | `PromptBuilderTest#givenHistory_whenBuildLlmOnlyPrompt_thenShortSystemHistoryAndQuestion`, `#givenQuestionWithBraces_whenBuildLlmOnlyPrompt_thenQuestionVerbatimWithoutTemplate` | Geçti |
| B4 (K2b, S3, G3) | LLM SSE: `sources` sözleşme literal'i, `done` null'lar | `SendMessageLlmOnlyIntegrationTest#givenLlmOnlyMode_whenSendMessage_thenSourcesWithoutChunksAndCitations` (gerçek sunucu + PostgreSQL), `AnswerStreamerTest#givenLlmOnlyAnswerWithCitationText_whenStreamModelAnswer_thenSourcesWithoutChunksAndNoCitations` | Geçti |
| B5 (K7) | LLM tur kaydı NONE ve boş alanlar | `TurnServiceIntegrationTest#givenLlmOnlyDraft_whenRecordTurn_thenNoneStrategyAndNoChunks`, `#givenLlmOnlyDraftWithUnexpectedContext_whenRecordTurn_thenContextIgnored`, `SendMessageLlmOnlyIntegrationTest#givenLlmOnlyMode_whenSendMessage_thenTurnRowHasNoneStrategyAndNoRetrievalColumns` | Geçti |
| B6 (K7) | V3 kısıtları | `TurnServiceIntegrationTest#givenMigrations_whenInconsistentModeAndStrategy_thenCheckViolation` (6 satır, `violates check constraint` mesajı aranır), `#givenMigrations_whenConsistentLlmOnlyRow_thenInsertAccepted`; `AssistantApplicationIntegrationTest#...AppliesAllThreeMigrations` | Geçti |
| B7 (G3) | LLM "bulamadım" | `AnswerStreamerTest#givenLlmOnlyAnswerWithNotFoundSentence_whenCompleted_thenNotFoundFalse` | Geçti |
| B8 | RAG değişmedi | `SendMessageIntegrationTest` happy path (`generationMode` ilk alan, `RAG`, `RERANK`, 20, 5), `AnswerStreamerTest` ilk test | Geçti |
| B9-B13 (K3, S2, G1) | Uç değerleri, sadece aranabilir, boş, retrieval-api kapalı, sayfalama | `LiveConfigServiceTest` (`...ConfiguredValues`, `...NotCounted`, `...Empty`, `...UnavailableWithoutFailing`, `...EmbeddingAlsoNull`, `...ReadsBothWithSize100`, LLM/TOP_K/DOWN varyantları) | Geçti |
| B14 (K3b, K8) | Uç JSON alanları, POST 405 | `LiveConfigControllerTest#givenLiveConfig_whenGet_thenJsonFields`, `#givenUnavailableKnowledgeBase_...`, `#givenLiveConfig_whenPost_thenMethodNotAllowed` | Geçti |
| B15 (K5) | Tracker | `RerankerStatusTrackerTest` (`...LatestWins`, `...NotUsedEvenAfterMeasurements` ×3, `...Unknown`) | Geçti |
| B16 (K5, S4) | Rerank çağrısı bildirir | `ContextRetrieverTest#givenRerankerFails_whenRetrieveContext_thenTrackerDown`, `#givenRerankSucceeds_...TrackerUp`, `#givenTopKStrategy_...TrackerStaysNotUsed` | Geçti |
| B17 (K5, S4) | Sağlık kontrolü | `RerankerHealthCheckerTest` (UP, DOWN, TOP_K ve LLM_ONLY'de istek yok), `RerankClientTest#givenHealthEndpointOk_...`, `#givenHealth503_...`, `#givenConnectionRefused_...` | Geçti |
| B18 | Transaction dışında | `LiveConfigServiceTest#givenGetLiveConfig_whenRetrievalCalled_thenNoTransactionActive` | Geçti |
| B19 (G2) | LLM takip sorusu | `AnswerServiceTest#givenLlmOnlyFollowUp_whenSendMessage_thenHistoryInPromptAndNoRewrite` | Geçti |
| (ek) | `RetrievalClient.getWorkspace/listSources` yolu, sorgu parametreleri, `chunkCount` null, hata → `RETRIEVAL_UNAVAILABLE` | `RetrievalClientTest` (4 yeni test) | Geçti |
| F1 (K4, S1, S2, G1) | Şerit öğeleri ve tonlar | `liveConfigItems.test.ts` (varsayılan, 8 retrieval varyantı, rewrite açık/kapalı, embedding `—`, 5 bilgi bankası durumu, 45/40 karakter ad) | Geçti |
| F2 (K6, AC2) | Üç varyant | `howItWorks.test.ts` (9 durum; `llama3:8b`, 30/3 dahil) | Geçti |
| F3 | Şerit sayfada | `PreviewPage.test.tsx#givenLiveConfig_whenPageOpens_thenStripShowsConfiguredValues`, `#givenOtherConfiguredModelAndNumbers_...` (AC2) | Geçti |
| F4 (S3) | LLM ekranı | `#givenLlmOnlyConfig_whenPageOpens_thenRetrievalOffDangerAndLlmHowItWorks`, `#givenEmptyKnowledgeBase_...`, `#givenRetrievalApiDown_...` | Geçti |
| F5 (K2b, G3, G4) | LLM cevabı | `#givenLlmOnlyAnswerWithCitationText_whenDone_thenPlainTextAndHowItWorksStays`, `#givenLlmOnlyAnswerWithEmptyKnowledgeBase_whenDone_thenNoAddDocumentHint` | Geçti |
| F6 (K5, S4) | Cevaptan sonra yenileme | `#givenAnswerDone_whenDone_thenLiveConfigRefetchedAndRerankerDownShown`, `#givenStreamErrorEvent_...Refetched`, `#givenRetrievalUnavailableBeforeStream_...Refetched` | Geçti |
| F7 (G5) | Uç hatası | `#givenLiveConfigFails_whenPageOpens_thenAlertAndChatWorks` | Geçti |
| F8 | Olay doğrulama | `answerStreamEvents.test.ts` (LLM örneği kabul, `FOO` mod/`candidates` string/`topK` kesirli/eksik alan → null, `retrievalLatencyMs` null) | Geçti |
| F9 (K9) | Rozet ipucu | `App.test.tsx#givenAnyScreen_whenRendered_thenShowsLogoTextAndLiveConfigBadge` (`title` eklendi) | Geçti |
| F10 | Axe | `LiveConfigStrip.test.tsx#givenStrip_whenRendered_thenNoAxeViolations` (+ şerit rol/ton testi) | Geçti |
| F11 | Süre satırı, strateji etiketi | `previewModel.test.ts` (`NONE` etiketi, `retrievalLatencyMs` null, `REWRITTEN` + `searchQuery` null) | Geçti |
| E1 | Gerçek şerit | `web/e2e/preview.spec.ts#givenRunningSystem_whenPreviewOpens_thenStripMatchesLiveConfig` | Yazıldı, çalıştırılamadı (aşağıda) |

TASK.md'de planda testi unutulmuş kural bulunmadı.

## Mutation
- **assistant-api: PIT 1.30.1** (`./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`): 378 mutant, 342 öldü (%90), test gücü %94. Task kurallarına ait hayatta kalan yok. Kalanlar: log-only dallar (`RerankerStatusTracker.record` durum-değişimi logu, `TurnService.logRecordedTurn`), `PromptBuilder` ArrayList kapasite aritmetiği (eşdeğer), TASK-008/010'dan kalan akış/yeniden yazma ayrıntıları (`AnswerStreamer`, `RetrievalClient.upstreamErrorCode` hata kodu string'i, süre). PIT koşusunda ilk turda `SendMessageLlmOnlyIntegrationTest` kırmıştı (global `turn_chunks` sayımı diğer testlerin satırlarını da sayıyordu); tur kimliğine daraltıldı, test düzeldi.
- **web: Kurulu değil** (Stryker `package.json`'da yok; eklemek plan kararı). Elle mutasyon kontrolü: ton eşlemesi, `RERANK`+`DOWN`, `LLM_ONLY` önceliği, 40 karakter sınırı, null embedding ve her `generationMode` dalı için ayrı test vakası var.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw -q clean verify` → BUILD SUCCESS, 227 test, 0 başarısız, JaCoCo satır %96,2 (eşik %80).
- `cd web && npm run build` → geçti (tsc -b + vite build). `npm run lint` → hata/uyarı yok.
- `cd web && npm test` → 24 dosya, 423 test, hepsi geçti. `npm run test:coverage` → 423 geçti, satır %96,65 / dal %89,96 (eşik %80/%70).
- `npx playwright test --list` → 15 test listelendi (E1 dahil).

## Çalıştırılamayanlar
- **e2e (`npm run test:e2e`)**: `localhost:3000` ve `localhost:8090` kapalı; çalışan sistem (Docker, retrieval-api, Ollama, reranker) gerekiyor. Kullanıcının çalışan reranker'ına/servislerine dokunulmadı. E1 ve mevcut e2e'leri qa çalıştırır.
- Reranker'ın gerçekten durdurulup şeridin ~30 sn'de sarıya dönmesi (M3), mod değişimi (M2) ve diğer elle maddeler (M1-M6) qa'nındır.

## Düzeltme
Yok: uygulama hatası bulunmadı.

STATUS: DONE
