TASK: TASK-014
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-mvc-testing`, `react-testing`.

## Önce yapılan düzeltmeler (lead kararı: test dosyalarını sadece tester değiştirir)
| # | Düzeltme | Dosya |
|---|----------|-------|
| 1 | `answerFormat.test.ts` yeni yerine `git mv` | `web/src/shared/turn/answerFormat.test.ts` |
| 2 | MSW varsayılan handler'ları: `listConversations` (boş sayfa, `size` query'sine uyar), `getConversationDetail` (fixture). `AdminLayout.test` `QueryClientProvider` ile sarıldı; `App.test` bilinmeyen adres örneği `/konusmalar/12` → `/kaynaklarim` (`/konusmalar/*` artık gerçek rota ve menüde seçili kalır); `SourceDetailPage.test` "istek yok" kontrolü sadece `/api/workspaces` isteklerine bakar (sayaç isteği yan menüden gelir) | `web/src/test/server.ts`, `app/AdminLayout.test.tsx`, `app/App.test.tsx`, `SourceDetailPage.test.tsx` |
| 3 | `tryStart(long, String, Instant)` yeni imzası (9 çağrı) | `AnswerServiceTest` |
| 4 | `@MockitoBean ConversationReadService` | `ConversationControllerTest` |
| 5 | Migration sayısı 5 → 6 (adı `...AllSixMigrations`, V6 sürüm assertion'ı eklendi) | `AssistantApplicationIntegrationTest` |

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| B1 | Durum kuralları, kural sırası (15 satır: INCOMPLETE ×4 hata türüyle, LLM_ONLY → DAYANAKSIZ, bulamadım → BİLMİYOR, Hayır+alıntı → HATALI, alıntı → DAYANAKLI, alıntısız → DAYANAKSIZ, öncelik çakışmaları) | `TurnStatusResolverTest#givenTurnFields_whenResolve_thenStatusFollowsRuleOrder` | geçti |
| B2 | Liste içeriği/sırası, tursuz konuşma yok, aktarılmış satır (S1, K1a, K2b) | `ConversationReadControllerIntegrationTest#givenConversationsWithAndWithoutTurns_whenListConversations_thenNewestFirstWithoutEmptyOnes` (+ `givenNoConversations_…EmptyPage`, `givenSameCreatedAt_…HigherIdFirst`) | geçti |
| B3 | Sayfalama | `…#givenThreeListedConversations_whenPageSizeTwo_thenTwoPages` | geçti |
| B4 | Parametre hataları | `ConversationControllerTest#givenInvalidPageOrSize_whenListConversations_thenValidationFailed` (page=-1, size=0, size=101), `#givenNonNumericSize_…BadRequest`, `#givenNoParameters_…DefaultsPageZeroSizeTwenty…` | geçti |
| B5 | Süren tur + kayıtlı tur | `…#givenRecordedTurnAndActiveTurn_whenRead_thenInProgressShown` | geçti |
| B6 | Sadece süren tur | `…#givenOnlyActiveTurn_whenListConversations_thenListedWithActiveQuestionUntilFinished` | geçti |
| B7 | Çift görünme yok (G1, mikro saniye kesme) | `…#givenTurnRecordedButRegistryNotFinished_whenRead_thenTurnShownOnce` | geçti |
| B8 | Detay alanları, prompt sırası (S2): vector 7,1,3,2,9, distance 0.3124, rerank 5.36, 5 öğe | `…#givenRerankFollowUpTurn_whenGetConversationDetail_thenPromptChunksInPromptOrder` | geçti |
| B9 | 404 / bozuk id | `ConversationControllerTest#givenUnknownConversation_…ConversationNotFound`, `#givenNonNumericId_…BadRequest`; gerçek DB: `…#givenUnknownConversation_whenGetConversationDetail_thenNotFoundProblem` | geçti |
| B10 | Aktarma kaydı | `…#givenHandedOffConversation_whenGetConversationDetail_thenHandoffIncluded` | geçti |
| B11 | Durum geri bildirimi izler (GROUNDED → WRONG → GROUNDED, listede de) | `…#givenCitedTurn_whenFeedbackChanges_thenStatusChanges` | geçti |
| B11b | Her durum kuralı gerçek DB'den (GROUNDED, DAYANAKSIZ, BİLMİYOR, TAMAMLANMADI, Sadece LLM, HATALI) | `…#givenOneTurnPerStatusRule_whenGetConversationDetail_thenEachStatusReported` | geçti |
| B12 | Sadece LLM turu | `…#givenLlmOnlyTurn_whenGetConversationDetail_thenNoChunksAndUngrounded` | geçti |
| B13 | Tursuz konuşma detayı (G2) | `…#givenConversationWithoutTurns_whenGetConversationDetail_thenEmptyTurns` | geçti |
| B14 | Registry | `ActiveTurnRegistryTest` (3 test) | geçti |
| B15 | Akış soruyu strip edip kaydeder | `AnswerServiceTest#givenQuestionWithSpaces_whenSendMessage_thenRegistersStrippedQuestionAndReceivedAt` | geçti |
| F1 | Liste satırı (60 karakter kısaltma, saat bugün/başka gün) | `ConversationListPage.test.tsx#givenConversations_whenRendered_thenRowShowsLabCells`, `#givenConversationFromAnotherDay_…`, `#givenShortQuestion_…` | geçti |
| F2 | Boş durum | `…#givenNoConversations_whenRendered_thenEmptyStateWithPreviewLink` | geçti |
| F3 | Satır tıklama / Enter | `…#givenRow_whenOpened_thenNavigatesToDetail` (click, enter) | geçti |
| F4 | İşleniyor + polling durur | `…#givenInProgressConversation_whenPolled_thenChipUpdatesAndPollingStops`; detay: `ConversationDetailPage.test.tsx#givenInProgressDetail_whenPolled_…` | geçti |
| F5 | Temsilcide çipi | `…#givenHandedOffConversation_whenRendered_thenTemsilcideChipOnlyOnThatRow` | geçti |
| F6 | Sayaç | `MainNav.test.tsx#givenThreeConversations_whenNavRendered_thenCountShown`, `#givenNoConversations_…CountZeroShown`, `#givenCountRequestFails_…NoCountBadge`, `#givenConversationDetailOpen_…StaysSelected` | geçti |
| F7 | Detay başlığı | `ConversationDetailPage.test.tsx#givenConversation_whenRendered_thenBreadcrumbTitleAndSubtitle` | geçti |
| F8 | Arama sorgusu satırı | `…#givenFollowUpTurn_whenRendered_thenSearchQueryLineOnlyWhenDifferent` | geçti |
| F9 | Parça tablosu | `…#givenRerankTurn_whenRendered_thenChunkTableColumnsAndValues`, `#givenTopKTurn_…NoRerankColumn`, `#givenRerankFallbackTurn_…` | geçti |
| F10 | Sadece LLM | `…#givenLlmOnlyTurn_whenRendered_thenWarningInsteadOfTable` | geçti |
| F11 | Parça penceresi, silinmiş parça | `…#givenChunkRowOrCitation_whenClicked_thenChunkDialogOpens` (satır, alıntı), `#givenDeletedChunk_whenOpened_thenNotFoundMessage`, `#givenCitationOutsideContext_…NotAButton` | geçti |
| F12 | Bilgi satırı | `…#givenAnsweredTurn_whenRendered_thenNumbersLine`, `#givenHelpfulTrueAndNull_…` | geçti |
| F13 | Her tur durumu etiketi/tonu (altı kural ayrı ayrı) | `turnStatus.test.ts` (etiket ×6, ton ×6), liste ve detayda çip testleri (×6 + ×6) | geçti |
| F14 | Hatalı tur | `…#givenFailedTurn_…`, `#givenRetrievalFailedTurn_…` | geçti |
| F15 | Süren tur kartı | `…#givenInProgressTurn_whenRendered_thenProcessingCardAndCountIncludesIt` | geçti |
| F16 | Bulunamadı / bozuk id / tursuz | `…#givenUnknownConversation_…`, `#givenNonNumericId_…WithoutRequest` (4 id), `#givenConversationWithoutTurns_…` | geçti |
| F17 | Aktarma satırı | `…#givenHandedOffConversation_whenRendered_thenHandoffLine` | geçti |
| F18 | a11y | liste ve detayda `vitest-axe` testleri | geçti |
| F19 | Liste/detay hatası | `…#givenServerError_whenRendered_thenErrorAlert` (ikisinde) | geçti |
| F20 | Akış bitince sayaç yenilenir | `MainNav.test.tsx#givenAnswerDone_whenStreamEnds_thenConversationCountRefetched` (planda `AdminLayout.test`; `App` gerekli olduğu için MainNav testinde) | geçti |
| F21 | Taşınan `answerFormat` testi + mevcut `AnswerMessage` testleri | `shared/turn/answerFormat.test.ts` | geçti |
| — | Etiketler ve `formatListTime` (planda yoktu, kural 3/Karar 15-16 için eklendi) | `turnLabels.test.ts`, `format.test.ts#formatListTime` | geçti |
| E1 | Gerçek sistem | `web/e2e/conversations.spec.ts` (+ bulunamadı sayfası) | çalıştırılamadı |

## Mutation
- **PIT** (assistant-api; `TurnStatusResolver`, `ConversationReadService`, `ActiveTurnRegistry`, yeni DTO'lar, `pagination.*`): 46 mutant, 46 öldürüldü (%100), satır coverage %100, test gücü %100. Hayatta kalan yok.
- **Stryker** (web): kurulu değil, çalıştırılamadı. Yerine web için v8 coverage: satır %97,28.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 354 test, 0 hata/başarısız, JaCoCo eşiği geçti.
- `cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -DtargetClasses=…` → yukarıdaki sonuç.
- `cd web && npm test` → 35 dosya, 587 test geçti.
- `cd web && npm run build` → başarılı. `npm run lint` → 0 uyarı. `npm run test:coverage` → satır %97,28, eşik geçti.

## Çalıştırılamayanlar
- `web/e2e/conversations.spec.ts`: çalışan sistem (Docker, modeller, yüklenmiş rehber) gerekiyor; localhost:3000 kapalı. Dosya tip kontrolünden geçti, qa çalıştırır.

## Gözlem (NEEDS_FIX değil)
- Yan menü sayacının erişilebilir adı `Konuşmalar3` (araya boşluk yok; frontend raporu "Konuşmalar 3" diyordu). Test `Konuşmalar\s*3` kabul ettiği için geçiyor; ekran okuyucuda rozet bitişik okunabilir. Opsiyonel düzeltme, frontend-coder.

## Düzeltme
Yok.

STATUS: DONE
