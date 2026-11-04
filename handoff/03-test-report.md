TASK: TASK-032
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `rag-pipeline`, `sse-streaming`, `frontend-conventions`, `react-testing` (proje kuralları ve plan üzerinden uygulandı).

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| T1 | Kısa mesaj tanımı | `ShortMessageDetectorTest#givenMessages_whenIsShortMessage_thenTenWordsWithoutQuestionMarkOrFiveWithIt` (plandaki 13 örnek + `?` ile başlayan mesaj) | geçti |
| T2 | İlk mesaj selam | `AnswerServiceTest#givenFirstShortMessageLabeledGreeting_whenSendMessage_thenFixedReplyWithoutRewriteOrRetrieval` | geçti |
| T3 | Takip teşekkürü | `AnswerServiceTest#givenHistoryAndShortThanks_whenSendMessage_thenFixedThanksWithoutRewriteOrRetrieval` (+ `givenShortThanksAndFarewell_…` her etiket kendi metni) | geçti |
| T4 | Kısa mesaj, etiket diğer | `AnswerServiceTest#givenShortMessageLabeledOther_whenSendMessage_thenNormalPathAndLabelAskedOnce` | geçti |
| T5 | Diğer + eşik altı | `AnswerServiceTest#givenShortMessageLabeledOtherBelowThreshold_whenSendMessage_thenNotFoundWithoutSecondLabel` | geçti |
| T6 | retrieval-api kapalı + teşekkür | `AnswerServiceTest#givenRetrievalDownAndShortThanks_whenSendMessage_thenThanksReplyNot503` | geçti |
| T7 | Uzun mesaj | `AnswerServiceTest#givenLongMessage_whenSendMessage_thenNoLabelBeforeSearch` + `…givenLongMessageBelowThreshold_…LabelAskedAfterRetrievalOnce` (InOrder) + `givenLongMessageBelowThresholdAndLabelGreeting_…` | geçti |
| T8 | Etiket hatası | `AnswerServiceTest#givenShortMessageAndLabelFailure_whenSendMessage_thenNormalPathWithoutError` | geçti |
| T9 | Akışta ilk mesaj selam | `SendMessageGroundedAnswerIntegrationTest#givenFirstMessageGreeting_whenSendMessage_thenLabelThenFixedReplyWithoutSearch` (SSE + DB + RetrievalClient çağrılmadı + model 1 kez). Plan `SendMessageIntegrationTest` demişti; etiketi ayıran sahte model bu sınıfta olduğu için oraya yazıldı | geçti |
| T10 | Sadece LLM | `AnswerServiceTest#givenLlmOnlyModeAndShortGreeting_whenSendMessage_thenModelCalledAndLabelerNotCalled` | geçti |
| T11 | Kısa hesap sorusu, giriş + bilgi | `SendMessageGroundedAnswerIntegrationTest#givenShortPersonalQuestionLabeledOther_whenSendMessage_thenOpenerAndCitedInfo` | geçti |
| T12 | parseLabel | `MessageLabelerTest#givenModelOutputs_whenParseLabel_thenMapsExactLabelsElseOther` (değişmedi) | geçti |
| T13 | Cevap talimatı | `PromptBuilderTest#givenRagPrompt_whenBuild_thenOpenersLimitedToTheirTopicsAndUncitedSentencesAllowed` + yeni tam metin sabiti (`LAB_SYSTEM_TEXT`, planın metninden elle yazıldı) | geçti |
| T14 | İsim etiket talimatında | `MessageLabelerTest#givenMessage_whenLabelMessage_thenSystemMessageTreatsSelfIntroductionAsGreeting` | geçti |
| T15 | Yeniden yazmaya cevap gitmez | `QueryRewriterTest#givenHistoryWithAssistantGreeting_whenRewriteQuery_thenPromptHasOnlyPreviousQuestions` (+ `givenTwoHistoryTurns_…QuestionLinesOldestFirst`, tam şablon eşitliği `…SendsTurkishTemplateAsSingleUserMessage`) | geçti |
| T16 | Türkçe şablon | `QueryRewriterTest#givenFollowUp_whenRewriteQuery_thenTurkishInstructionKeepsIndependentQuestion` | geçti |
| T17 | Etiket satırı silinir | `QueryRewriterTest#givenDecoratedOutput_whenRewriteQuery_thenCleanQuery` ("Arama sorusu:", "ARAMA SORUSU:" ve eski "Standalone query:" varyantları) + `givenBlankOrLabelOnlyOutput_…` ("Arama sorusu:" tek başına → `EMPTY`) | geçti |
| T18 | Ölçüme sızma yok | `MessageLabelerTest#givenLabelPromptMeasurementListAndGoldenSet_whenCompared_thenNoMessageOverlapsAnExample` (28 örnek × 99 liste mesajı + 100 golden soru; eşitlik ve 3+ kelimede kelime sınırı içermesi; Nber, Ahmet, Ben Ayşe, Adım Mehmet, Nasılsın?, Merhaba, iyi misin? listede mevcut diye doğrulanır; anahtar test tarafında kuruldu) | geçti (çakışma yok) |
| T19 | Log sınırı | `SendMessageGroundedAnswerIntegrationTest#givenPreSearchLabelTurn_whenLogsCaptured_thenRouteLoggedWithoutMessageText` | geçti |
| T20 | Harf/rakam yok tanımı | `ShortMessageDetectorTest#givenMessages_whenHasNoWords_thenTrueOnlyWithoutLettersOrDigits` | geçti |
| T21 | Akış öncesi yol | `AnswerServiceTest#givenMessageWithoutWords_whenSendMessage_thenNotFoundWithoutLabelRewriteRetrievalOrModel` | geçti |
| T22 | retrieval-api kapalı | `AnswerServiceTest#givenRetrievalDownAndMessageWithoutWords_whenSendMessage_thenNotFoundNot503` | geçti |
| T23 | Akış ve kayıt | `SendMessageGroundedAnswerIntegrationTest#givenMessageWithoutWords_whenSendMessage_thenSingleNotFoundTokenAndNoWordsTurn` | geçti |
| T24 | Migration V9 | `TurnsV9MigrationIntegrationTest#givenV9_whenInsertNoWordsTurns_thenOnlyNotFoundNoneWithoutLabelAccepted` (+ `givenV9_whenInsertingOlderValidCombinations_thenStillAccepted`); `AssistantApplicationIntegrationTest` 9 migration | geçti |
| T25 | Durum | `TurnStatusResolverTest#givenNoWordsTurn_whenResolve_thenDontKnow` | geçti |
| T26 | Sadece LLM, harfsiz mesaj | `AnswerServiceTest#givenLlmOnlyModeAndMessageWithoutWords_whenSendMessage_thenModelCalled` | geçti |
| T27 | Kısaltma örnekleri | `MessageLabelerTest#givenMessage_whenLabelMessage_thenSystemMessageContainsAbbreviatedGreetingExamples` ("Nber → " satırı yok) | geçti |
| W1 | Olay daraltma | `answerStreamEvents.validation.test.ts#givenDoneWithNoWordsSource_whenParsed_thenAccepted` (+ mevcut `it.each` listesine `NO_WORDS`; `OTHER` hâlâ atlanır) | geçti |
| W2 | Önizleme | `PreviewPage.test.tsx#givenNoWordsDone_whenFinished_thenNotFoundSentenceWithoutContextCard` | geçti |
| E1 | Selam uçtan uca | `web/e2e/acceptance.spec.ts` değişmedi, çalıştırılmadı | çalıştırılmadı (aşağıda) |

Mevcut testlerde yeni davranışa göre yapılanlar: silinen `SmallTalkDetectorTest` yerine `ShortMessageDetectorTest`; `AnswerServiceTest`'te kalıp listesi testleri etiket yoluna çevrildi, sınıf başında varsayılan sahte etiket `OTHER`; `SendMessageIntegrationTest` ve `SendMessageGroundedAnswerIntegrationTest` kısa ilk sorular için etiket çağrısını sayacak biçimde güncellendi (yeni test yardımcısı `support/ChatModelStubs`: etiket çağrısına sabit etiket, diğerlerine sırayla akış).

TASK.md'de olup planda testi unutulmuş kural: yok.

## Mutation
PIT 1.30.1, hedef `ShortMessageDetector`, `QueryRewriter`, `AnswerService` (sadece bu üç sınıfın birim testleriyle): 74 mutant, 71 öldü (%96), test gücü %96. Hayatta kalan 3'ten ikisi kapatıldı:
- `ShortMessageDetector` `indexOf('?') >= 0` sınırı → `?` ile başlayan mesaj testi eklendi.
- `QueryRewriter.removeSurroundingQuotes` `length >= 2` sınırı → yalnız `""` çıktısı testi eklendi.
- Kalan: `AnswerService` `logQueryRewrite` çağrısının silinmesi (sadece log; log içeriği `SendMessageIntegrationTest#givenRewrittenTurn_whenLogsCaptured…` ile entegrasyonda doğrulanıyor, PIT'in birim kümesinde yok). Eklemeler sonrası PIT yeniden çalıştırılmadı. Web için Stryker çalıştırılmadı.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw clean verify` → BUILD SUCCESS; 600 test, 0 başarısız, 0 hata; JaCoCo eşiği geçti (satır coverage ≈ %95,6).
- `cd web && npm test` → 46 dosya, 751 test geçti (748 + 3 yeni).
- `cd web && npm run build` → geçti. `npm run lint` → geçti, uyarı yok.

## Çalıştırılamayanlar
- E1 (`web/e2e/acceptance.spec.ts`) ve gerçek modelle ölçümler: çalışan tam sistem gerekir; qa çalıştırır. Testler 8090/8091/8080/5173'e dokunmadı (Testcontainers kendi veritabanını kullandı).

## Düzeltme
Yok. Uygulama hatası bulunmadı.

## Düzeltme turu 1
Kaynak: qa B1, Plan notu 1-2, backend raporu "Düzeltme turu 2".

| Madde | Test | Sonuç |
|-------|------|-------|
| T28 ortadaki giriş cümlesi silinir | `AnswerFinalizerTest#givenOpenerAfterFirstSentence_whenFinalize_thenOpenerSentenceRemovedAndCitationsRecomputed` (plandaki 4 örnek + eski "[9]" aralık dışı örneği; `misplacedOpenersRemoved` = 1) | geçti |
| T29 ilk cümledeki ve "bakiye" geçen cümle kalır | `AnswerFinalizerTest#givenOpenerAsFirstSentenceOrSentenceMentioningBalance_whenFinalize_thenKept` | geçti |
| T30 akışta | `SendMessageIntegrationTest#givenModelWritesOpenerMidAnswer_whenSendMessage_thenDoneAnswerAndRecordWithoutOpener` (token'lar olduğu gibi, citations [1], done ve DB `answer` temiz, `cited` `{1}`) | geçti |
| Eski "ortadaki giriş cümlesi kalır" bekleyen 2 `AnswerFinalizerTest` | silindi; yerine T28 (eski davranışı tersine çeviren örnekler T28'e taşındı) | geçti |
| 2 birebir sistem metni testi (`PromptBuilderTest`) | `LAB_SYSTEM_TEXT` kural 4 son satırı yeni metne göre güncellendi | geçti |
| T13 | Kural 4 başlığı backend'de değişmediği için (gerçek `rag-system.st`: "başka hiçbir mesajda bu üç cümleyi kullanma") mevcut ifadeler korundu; plandaki "ortasında ya da sonunda" ifadesi dosyada yok, o yüzden aranmadı. Eklenen: "cevabı bu ilk cümleyle bitirme" | geçti |
| Mutation'dan ek test | `AnswerFinalizerTest#givenSingleWordOpenerInConfiguration_whenFinalize_thenNoSentenceRemoved` (tek kelimelik giriş cümlesi tüm cümleleri silmez) | geçti |

Mutation (PIT, `AnswerFinalizer`, `ShortMessageDetector`, `QueryRewriter`, `AnswerService`, `MessageLabeler` + ilgili birim testleri): 118 mutant, 115 öldü (%97), kapsamasız 0. Hayatta kalan: `AnswerService` log çağrısı (entegrasyonda log testiyle kapalı), `AnswerFinalizer` satır 89 `firstSpace < 0` sınırı (boşlukla başlayan giriş cümlesi normalize sonrası olamaz: eşdeğer mutant), `AnswerFinalizer` satır 40 filtre (yukarıdaki test sonrası eklendi, yeniden koşulmadı).

Komutlar: `cd assistant-api && ./mvnw clean verify` → BUILD SUCCESS, 607 test, 0 başarısız, coverage eşiği tamam. `cd web && npm test` → 46 dosya, 751 test geçti (web değişmedi). Uygulama hatası yok; çalışan süreçlere dokunulmadı.

STATUS: DONE
