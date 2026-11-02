TASK: TASK-010
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `react-testing`. (Plan `rag-pipeline`, `sse-streaming`, `spring-testing-fundamentals`, `frontend-conventions` da sayıyor; bunları ayrıca yüklemedim, mevcut testlerin kalıbını izledim.)

## Önce yapılan uyarlamalar (mevcut testler yeni imzalara)
- assistant-api: `TestProperties` (yeni `QueryRewrite`/`History` bileşenleri + `withQueryRewrite(enabled, timeout, maxLength)` yardımcısı), `AnswerServiceTest` (`QueryRewriter` mock'u), `AnswerStreamerTest`, `ConversationPersistenceIntegrationTest`, `TurnServiceIntegrationTest` (yeni `TurnDraft` imzası), `PromptBuilderTest` (geçmiş parametresi), `AssistantPropertiesTest` (yeni anahtarlar), `AssistantApplicationIntegrationTest` (artık 2 migration).
- web: `src/test/previewFixtures.ts` yeni zorunlu alanları taşıyor (`searchQuery`, `queryRewriteStatus: 'NO_HISTORY'`, `queryRewriteLatencyMs: null`).

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|---|---|---|
| B1 (K1, S2) | Kapalı → model çağrılmaz | `QueryRewriterTest#givenRewriteDisabled_whenRewriteQuery_thenDisabledWithoutModelCall` | geçti |
| B2 (K2, G1) | İlk tur → NO_HISTORY, model çağrılmaz | `QueryRewriterTest#givenFirstTurn_whenRewriteQuery_thenNoHistoryAndSearchesWithQuestion` | geçti |
| B3 (K3a) | Lab şablonu, tek UserMessage, tam literal | `QueryRewriterTest#givenOneHistoryTurn_whenRewriteQuery_thenSendsLabTemplateAsSingleUserMessage` (+ iki tur sırası testi) | geçti |
| B4 (K4, G1) | Geçmiş sınırı: son 3 cevaplı tur, eskiden yeniye, `[n]` silinmiş, hatalı tur dışarıda | `ConversationServiceIntegrationTest#givenFiveAnsweredTurnsAndOneFailed_whenGetHistory_thenLastThreeAnsweredOldestFirst` (+ yarım cevaplı `STREAM_CANCELLED`, sınırdan az tur, başka konuşma) | geçti |
| B5 (K5, G2) | Model hatası → FAILED, özgün soru | `QueryRewriterTest#givenModelFails_...`, `#givenModelThrowsSynchronously_...` | geçti |
| B6 (K5) | Zaman aşımı → FAILED, iptal edilir, 5 sn dolmadan biter | `QueryRewriterTest#givenModelSilentLongerThanTimeout_whenRewriteQuery_thenFailedWithinTimeout` | geçti |
| B7 (K5, G3) | Boş / sadece etiket → EMPTY | `QueryRewriterTest#givenBlankOrLabelOnlyOutput_...` (parametrik) | geçti |
| B8 (G3) | Etiket, tırnak, satır sonu temizliği | `QueryRewriterTest#givenDecoratedOutput_...` (parametrik, 4 biçim) + iç tırnak korunur + parçalı çıktı birleşir | geçti |
| B9 (G4) | 1001 → FAILED, 1000 → REWRITTEN | `QueryRewriterTest#givenOutputLongerThanMaxLength_...`, `#givenOutputOfExactlyMaxLength_...` | geçti |
| B10 (S1) | Arama standalone, prompt müşterinin sorusu, mesaj sırası | `AnswerServiceTest#givenFollowUp_whenSendMessage_thenSearchesWithStandaloneAndPromptsCustomerQuestion` | geçti |
| B11 (K5) | Hata → özgün soruyla arama, akış sürer | `AnswerServiceTest#givenRewriteFailed_whenSendMessage_thenSearchesWithQuestionAndStreams`; uçtan uca `SendMessageIntegrationTest#givenRewriteModelFailsOnFollowUp_whenSent_thenStreamContinuesWithOriginalQuestion` | geçti |
| B12 | Prompt geçmişi / geçmişsiz TASK-008 ile aynı | `PromptBuilderTest#givenTwoHistoryTurns_...`, `#givenNoHistory_whenBuildPrompt_thenSameAsTask008` | geçti |
| B13 | Alıntı silme | `CitationParserTest#givenAnswerWithCitations_whenRemoveCitations_thenPlainText` | geçti |
| B14 (K6) | Tur kaydı: search_query, status, latency 812, history_turns 1 | `TurnServiceIntegrationTest#givenRewrittenDraft_...` (+ NO_HISTORY/null latency) | geçti |
| B15 | Migration kısıtları (`'FOO'`, `-1`), NOT NULL | `ConversationServiceIntegrationTest#givenMigrations_whenApplied_thenRewriteColumnsHaveConstraints`, `#givenTurnWithoutStatus_...`; `AssistantApplicationIntegrationTest` (2 migration) | geçti |
| B16 | SSE uçtan uca: 1. tur NO_HISTORY/null, 2. tur REWRITTEN; retrieval ve rerank standalone sorguyla; tur kaydı | `SendMessageIntegrationTest#givenTwoQuestions_whenSecondSent_thenSourcesHaveStandaloneQueryAndDoneHasRewriteLatency` | geçti |
| B17 (K8) | Logda uzunluk var, sorgu metni yok | `SendMessageIntegrationTest#givenRewrittenTurn_whenLogsCaptured_thenLengthsButNoQueryText` | geçti |
| B18 (K1, K4) | Ayarlar: varsayılanlar, `enabled=false`, max-turns 0/11, max-length 0/1001, bölüm eksik | `AssistantPropertiesTest` (5 yeni test) | geçti |
| B19 (G5) | Kilit önce: 409'da geçmiş okunmaz, yeniden yazma çağrılmaz | `AnswerServiceTest#givenTurnInProgress_whenSendMessage_thenNoHistoryReadAndNoRewrite` | geçti |
| B20 | Model çağrısı transaction dışında | `QueryRewriterTest#givenRewrite_whenModelCalled_thenNoTransactionActive` | geçti |
| B21 (G2) | Retrieval hatasında tur arama sorgusu/durum/süre/geçmişi taşır | `AnswerServiceTest#givenRetrievalUnavailableAfterRewrite_whenSendMessage_thenTurnKeepsSearchQueryAndStatus` | geçti |
| F1 | Olay doğrulama (searchQuery yok/sayı, durum yok/FOO, latency yok/string → null; null geçerli) | `answerStreamEvents.test.ts` (6 yeni satır + geçerli olay testi) | geçti |
| F2 | Süre satırı: 812 ilk satır; null → yok; 0 → görünür | `previewModel.test.ts#latencyRows` (3 yeni) | geçti |
| F3 | Alt başlık `· arama: "…"` | `PreviewPage.test.tsx#givenRewrittenSecondTurn_whenDone_thenSubtitleShowsSearchQuery` (+ FAILED: "yeniden yazma başarısız", arama etiketi yok; NO_HISTORY: ek yok) | geçti |
| F4 | Etiket durumları, 45 karakter → 39 + `…`, tam 40 kısaltılmaz | `previewModel.test.ts#queryRewriteLabel` | geçti |
| F5 | "yeniden yazma 812 ms" satırı "retrieval-api araması"ndan önce | `PreviewPage.test.tsx#givenRewrittenDone_whenRendered_thenRewriteLatencyRowBeforeRetrievalRow` | geçti |
| E1 | Gerçek sistemde takip sorusu | `web/e2e/preview.spec.ts#givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsSearchQueryAndRewriteRow` | yazıldı, çalıştırılmadı |

Kabul kriteri 2: ilk turda atlama (B2, B16), hatada özgün soruya dönüş (B5-B7, B9, B11) ve geçmiş sınırı (B4, B18) testli.
Planda olup eksik kalan kural yok. Eklenen ek vakalar: tek `"` karakteri, sınırdan az tur, başka konuşmanın turları, `STREAM_CANCELLED` yarım cevap, `0 ms` yeniden yazma.

## Mutation
- assistant-api: PIT (`QueryRewriter`, `CitationParser`, `PromptBuilder`, `ConversationService`, `AnswerService`): 59 mutant, 57 öldü (%97). Hayatta kalan 2:
  - `QueryRewriter.removeSurroundingQuotes` `length >= 2` sınırı → öldürmek için `givenSingleQuoteCharacterOutput_...` testi eklendi (yeniden PIT koşturmadım; test tek başına geçiyor).
  - `PromptBuilder` `ArrayList` kapasitesi `2 + history.size() * 2` çarpma→bölme: davranışa etkisi yok (eşdeğer mutant), bırakıldı.
- web: Stryker kurulu değil (`web/CLAUDE.md` "Henüz olmayanlar"). Yapılmadı.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw clean verify` → BUILD SUCCESS, 170 test, 0 hata, JaCoCo eşiği geçti.
- `cd web && npm run build` → geçti. `npm run lint` → geçti (uyarı yok). `npm test` → 21 dosya, 371 test geçti. `npm run test:coverage` → satır %96,44 (eşik %80).
- PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` (yukarıda).

## Çalıştırılamayanlar
- E1 (Playwright): ayakta Docker sistemi, Ollama, reranker ve içe alınmış rehber gerekiyor; qa çalıştırır (`E2E_BASE_URL=... npm run test:e2e`).
- Gerçek modelle M1/M3/M4 qa'nındır.

## Düzeltme
Yok. Uygulama hatası bulunmadı.

STATUS: DONE
