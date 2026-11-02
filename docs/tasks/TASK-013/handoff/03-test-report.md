TASK: TASK-013
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions` ve `spring-testing-fundamentals` (adlandırma, AAA, MockitoBean), `spring-mvc-testing` (MockMvcTester), `spring-jpa-testing` / Testcontainers kalıbı (mevcut `PostgresIntegrationTest`), `react-testing`, `frontend-conventions`, `sse-streaming` (MSW sahte akış), `e2e-testing`. Bu turda skill'ler `Skill` aracıyla yalnızca `writing-good-tests` için yüklendi; diğerlerinin kalıpları repodaki mevcut testlerden alındı.

## Önce yapılan onarımlar (build kırıktı)
- `support/TestProperties`: 5 yerde `Handoff("bireysel")`; yeni `withHandoffQueue(queue)`.
- `AssistantPropertiesTest.BASE`'e `assistant.handoff.queue`; eklenen testler: gönderilen varsayılan `bireysel`, ortamla geçersiz kılma, boş kuyruk adı ve 40 harften uzun ad açılışı düşürür.
- `AssistantApplicationIntegrationTest`: 5 migration (V5 dahil).
- web `src/test/suggestionFixtures.ts` `turn()`: `conversationId`, `answeredAt`.
- Yeni test yardımcıları: `support/FixedClockTestConfiguration` (`2026-11-02T10:31:05Z`), `support/TurnFixtures` (SQL ile tur: cevaplı, "cevap yok", hatalı, Sadece LLM), `web/src/test/feedbackHandlers.ts`.

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|---|---|---|
| B1 | Geri bildirim kaydı (K1a) | `FeedbackControllerIntegrationTest#givenAnsweredTurn_whenSubmitFeedback_thenStoresHelpfulAndTime` | geçti |
| B2 | Fikir değiştirme (S1, K1b) | `FeedbackControllerIntegrationTest#givenHelpfulTrue_whenSubmitFalse_thenFalseStored` | geçti |
| B3 | Aynı değer tekrar (K1b) | `...#givenHelpfulTrue_whenSubmitTrueAgain_thenStillTrue` | geçti |
| B4 | Tamamlanmamış tur (K1c, G4) | `...#givenFailedTurn_whenSubmitFeedback_thenTurnNotCompleted` (3 errorCode) | geçti |
| B5 | Olmayan tur | `FeedbackControllerTest#givenUnknownTurnId_whenSubmitFeedback_thenTurnNotFound`; `FeedbackControllerIntegrationTest#givenUnknownTurn_...` | geçti |
| B6 | Gövde/uç hataları | `FeedbackControllerTest#givenMissingHelpful_...`, `givenNullHelpful_...`, `givenMalformedJson_...`, `givenUnconvertibleHelpful_...` (`"yes"`), `givenNonNumericTurnId_...`, `givenTextPlain_...` | geçti |
| B7 | Eşzamanlı geri bildirim (K1d) | `FeedbackServiceIntegrationTest#givenConcurrentOppositeFeedbacks_whenBothFinish_thenOneValueAndOtherColumnsUnchanged` (20 tekrar) | geçti |
| B8 | Aktarma 202, kuyruk, sebep (S2, K2c) | `HandoffControllerIntegrationTest#givenNotFoundTurn_whenRequestHandoff_thenAcceptedWithQueueAndReason` (+ `HandoffControllerTest`: alan sırası, `Location` yok; `givenHandoff_whenRequested_thenNoTurnIsAdded`: temsilci mesajı yok, K3) | geçti |
| B9 | İdempotent (S3, K2b) | `...#givenHandedOffConversation_whenRequestAgain_thenSameRecordAndOneRow` | geçti |
| B10 | Cevap yok koşulu (K2a, G4) | `...#givenTurnWithoutNotFoundAnswer_whenRequestHandoff_thenHandoffNotAllowed` (cevaplı, 2 hatalı, Sadece LLM) | geçti |
| B11 | Bulunamayanlar | `...#givenUnknownConversation_...`, `givenTurnOfAnotherConversation_...`, `givenUnknownTurn_...`, `givenHandedOffConversation_whenRetryWithWrongTurn_thenStillValidated` | geçti |
| B12 | Gövde hataları | `HandoffControllerTest#givenMissingTurnId_...`, `givenMissingReason_...`, `givenEmptyBody_...`, `givenUnknownReason_...` (`OTHER`), `givenMalformedJson_...`, `givenNonNumericConversationId_...`, `givenTextPlain_...` | geçti |
| B13 | Eşzamanlı aktarma (G2) | `HandoffServiceIntegrationTest#givenTwoConcurrentRequests_whenBothFinish_thenOneRowAndSameResponse` (20 tekrar) | geçti |
| B14 | Kuyruk ayarı | `HandoffServiceTest#givenConfiguredQueue_whenRequestHandoff_thenInsertsThatQueue` | geçti |
| B15 | İkinci "cevap yok" turu (G3) | `HandoffControllerIntegrationTest#givenHandedOffConversation_whenRequestWithAnotherNotFoundTurn_thenFirstRecordReturned` (+ `givenTwoConversations_whenEachHandedOff_thenTwoRows`) | geçti |
| B16 | Aktarma sonrası soru | `HandoffThenSendMessageIntegrationTest#givenHandedOffConversation_whenSendMessage_thenAnswerStreams` (gerçek akışla oluşan "cevap yok" turu → aktarma → yeni soru cevaplanır; 2 tur, 1 aktarma) | geçti |
| B17 | Migration kısıtları | `FeedbackControllerIntegrationTest#givenFailedTurn_whenHelpfulWrittenDirectly_thenCheckViolation`, `givenHelpfulWithoutTime_whenWrittenDirectly_thenPairCheckViolation` | geçti |
| F1 | Satır + cevabın saati (K1e) | `PreviewPage.feedback.test.tsx#givenDoneTurn_whenRendered_thenFeedbackRowWithAnswerTime` (`13:31`) | geçti |
| F2 | Evet | `...#givenDoneTurn_whenYesClicked_thenRequestSentAndYesPressed` | geçti |
| F3 | Fikir değiştirme | `...#givenYesSelected_whenNoClicked_thenNoPressedAndReviewToast` | geçti |
| F4 | Aynı düğme | `...#givenYesSelected_whenYesClickedAgain_thenNoRequest` | geçti |
| F5 | Hata (G1) | `...#givenFeedbackFails_when{TURN_NOT_COMPLETED,TURN_NOT_FOUND}OnYesClick_...` (it.each), `givenNetworkError_whenYesClicked_...`, `givenFailedFeedback_whenRetriedAndSucceeds_...` | geçti |
| F6 | Sadece tamamlanmış tur (G4) | `...#givenStreamingTurn_whenRendered_thenNoFeedbackOrHandoffRow`, `givenFailedTurn_whenRendered_thenNoFeedbackOrHandoffRow` | geçti |
| F7 | Düğme sadece "cevap yok" | `...#givenAnsweredTurn_whenRendered_thenNoHandoffButton`, `givenNotFoundTurn_whenRendered_thenHandoffButtonAndFeedbackRow` | geçti |
| F8 | Aktarma (S2) | `...#givenNotFoundTurn_whenHandoffClicked_thenQueuedText` | geçti |
| F9 | Konuşma düzeyi, önceki konuşma (G3, G5) | `...#givenHandedOffConversation_whenSecondNotFoundTurnAndNewConversation_thenQueuedOnlyInThatConversation` | geçti |
| F10 | Aktarma hatası | `...#givenHandoffNotAllowed_whenHandoffClicked_thenAlertAndButtonStays` | geçti |
| F11 | Çift tıklama (G2) | `...#givenPendingHandoff_whenClickedTwice_thenOneRequest`, `givenPendingFeedback_whenOtherButtonClicked_thenOneRequestAndBothDisabled` | geçti |
| F12 | Erişilebilirlik | `...#givenFeedbackAndHandoffRows_whenRendered_thenNoAxeViolations` | geçti |
| F13 | Toast | `ToastProvider.test.tsx#givenShownToast_whenDurationPasses_thenRemoved` (+ iki toast, provider dışı hata) | geçti |
| F14 | Aktarma sonrası soru | `PreviewPage.feedback.test.tsx#givenHandedOffConversation_whenQuestionSent_thenAnswerStreams` | geçti |
| F15 | İki tur arasında durum | `...#givenTwoDoneTurns_whenFeedbackOnFirst_thenSecondUnaffected`, `givenFeedbackBeforeNewConversation_...` | geçti |
| E1 | Gerçek sistem | `web/e2e/preview.spec.ts#givenRunningSystem_whenAnswerDoneAndYesClicked_thenYesPressed` | yazıldı, çalıştırılmadı |

Planda olmayıp eklenenler: `AssistantPropertiesTest` handoff ayarı testleri (K2c kuyruk ayarı), `HandoffControllerIntegrationTest#givenTwoConversations_...`, `givenHelpfulWithoutTime_...` (feedback_pair CHECK).

## Mutation
PIT 1.30.1, sadece yeni sınıflar (`handoff.*`, `FeedbackService`, `FeedbackController`): 19 mutant, 16 öldü (%84), test gücü %94. Hayatta kalan/kapsamsız 3 mutant:
- `HandoffService` satır 62 `IllegalStateException` lambda'sı: kapsamsız (insert sonrası kayıt hep var; erişilemez dal).
- `Handoff.getId` boş dönüş: kapsamsız (hiçbir kural `id`'yi okumaz).
- `HandoffService` içinde 1 mutant hayatta; yeni bir task kuralına ait değil (log çağrısı / `created` bayrağı; log içeriği test edilmez, `backend-conventions` loglama kuralı gereği log metni sözleşme değil). Test eklenmedi.
Stryker kurulu değil (web/CLAUDE.md "Henüz olmayanlar"); web için mutation çalıştırılamadı.

## Komutlar ve sonuçlarını
- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 311 test, 0 başarısız, JaCoCo eşiği geçti ("All coverage checks have been met").
- `cd web && npm run build` → geçti. `npm run lint` → geçti (uyarı yok).
- `cd web && npm run test:coverage` → 30 dosya, 505 test geçti; toplam satır coverage %97,15 (eşik %80).
- PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -DtargetClasses=... -DtargetTests=...` (yukarıdaki sonuç).

## Çalıştırılamayanlar
E1 (e2e): çalışan sistem gerekiyor (`localhost:3000` ve `:8090` kapalı, gerçek model ister). qa çalıştırır: `cd web && npm run test:e2e`.

## Düzeltme
Yok. Uygulama kodunda hata bulunmadı; planın "Düzeltme turu 1" kararına uygun olarak dönüştürülebilir değer (`"true"`, `1`) için 400 bekleyen test yazılmadı, sadece dönüştürülemeyen (`"yes"`) test edildi.

STATUS: DONE
