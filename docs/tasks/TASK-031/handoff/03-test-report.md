TASK: TASK-031
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `frontend-conventions`, `react-testing`.

## Senaryo → test eşlemesi
Beklenen değerler plandan/sözleşmeden elle türetildi.

| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|---|---|---|
| T1-T3 | Kalıp listesi (yazım çeşitleri, tür önceliği, soru/hitap/bilinmeyen) + ölçüm listesi 13 listede / 5 listede yok | `generation/SmallTalkDetectorTest` (45 test, parametreli) | geçti |
| T4, T5 | Eşik sınırı; TOP_K, RERANK_FALLBACK, boş bağlam, eşik null | `retrieval/service/RerankScoreGateTest`; bağlama: `config/AssistantPropertiesTest#givenEmptyMinRerankScore_*`, `#givenShippedProperties_*` | geçti |
| T6-T9 | Akış filtresi | `generation/NotFoundAnswerStreamFilterTest` | geçti |
| T10, T11, T31, T37, T38 | Finalizer: bulamadım+numara, başka kelimelerle, aralık dışı, giriş cümlesi alıntıları | `generation/AnswerFinalizerTest`, `CitationParserTest#*OutOfRange*`, `NotFoundAnswerDetectorTest` (T32 dahil) | geçti |
| T12, T34 | Cevap talimatı, üç giriş cümlesi | `generation/PromptBuilderTest` | geçti |
| T13, T14, T24, T30 | Kalıp yolu, retrieval kapalı+teşekkür, Sadece LLM değişmez, etiket sadece eşik altında | `conversation/service/AnswerServiceTest` | geçti |
| T15, T29 | Eşik altı DIGER / SELAM | `conversation/controller/SendMessageGroundedAnswerIntegrationTest` | geçti |
| T16, T17, T33, T39 | Model numara ekler, normal cevap, başka kelimelerle bulamadım, giriş cümlesi | aynı sınıf | geçti |
| T18, T19, T23, T25 | Gold→Teşekkürler, geçmişe girmeme, boş bilgi bankası, log sınırı | aynı sınıf | geçti |
| T20 | Durum SMALL_TALK / eşik altı MISSED | `TurnStatusResolverTest` | geçti |
| T21 | V8 migration (geri doldurma + kısıtlar) | `db/TurnsV8MigrationIntegrationTest` (ayrı veritabanında V7→V8) | geçti |
| T22 | Konuşmalar detay SMALL_TALK | `ConversationReadControllerIntegrationTest#givenSmallTalkTurn_*` (+ liste, eşik altı) | geçti |
| T26-T28, T35, T36 | Etiket ayrıştırma, hata/zaman aşımı, prompt, örnekler, ölçüme sızma yok | `generation/MessageLabelerTest` | geçti |
| — | `streamFixedAnswer`, filtreli akış, `done.answer/answerSource`, kesinti | `AnswerStreamerTest` (yeni 8 test; 2 eski test yeni davranışa uyarlandı: akış kesintisinde ham metin kaydı, bulamadım cevabında alıntı []) | geçti |
| — | Tur kaydı (etiket, kaynak, aday parçalar) | `TurnServiceIntegrationTest` (+2) | geçti |
| W1 | Bulamadım + numara | `PreviewPage.test#givenNotFoundDoneWithCleanAnswer_*` | geçti |
| W2 | Selamlaşma (kalıp ve etiket yolu) | `PreviewPage.test#givenPatternSmallTalkDone_*`, `#givenLabelSmallTalkDone_*` | geçti |
| W3 | Eşik altı notu (+axe) | `PreviewPage.test#givenBelowThresholdDone_*` (ContextChunksCard ayrı test dosyası yok, sayfa üzerinden) | geçti |
| W4 | Boş bilgi bankası, LLM_ONLY'de not yok | `PreviewPage.test#givenEmptyKnowledgeBaseDone_*`, `#givenLlmOnlyEmptyContextDone_*` | geçti |
| W5 | Olay daraltma | `answerStreamEvents.validation.test` (answer eksik/tip, answerSource bilinmeyen, dört geçerli kaynak) | geçti |
| W6 | SOHBET | `turnStatus.test`, `ConversationListPage.test`, `ConversationDetailPage.test` (etiket ve `neutral` ton) | geçti |
| G3 | Ekran `done.answer` ile değişir | `PreviewPage.test#givenAnswerDifferentFromStreamedTokens_*`, aralık dışı iki test | geçti |
| E1, E2 | Selam / Bitcoin uçtan uca | `e2e/acceptance.spec.ts` (E1 yeni, E2 sıkılaştırıldı) | **çalıştırılamadı** |

Uyarlama: web fixture `previewFixtures.ts` `doneEvent()` (+`answer`, `answerSource`); 4 eski PreviewPage testi yeni davranışa uyarlandı. Backend: `TestProperties`, `ContextFixtures`, `TurnFixtures`, `AnswerServiceTest`, `AnswerStreamerTest`, üç persistence/integration testi imza değişikliklerine uyarlandı; `AssistantApplicationIntegrationTest` 8 migration bekler.

## Mutation
- PIT (tüm paket) Testcontainers'lı testlerle zaman aşımına düştü, tamamlanmadı (yarıda kesildi). Yeni saf sınıflar için hedefli koşu (`generation.*`, `RerankScoreGate`; birim testlerle): 54 mutant, 50 öldü, 4 hayatta: `SmallTalkDetector` satır 135 (boş anahtar filtresi, davranışsal olarak eşdeğer), `NotFoundAnswerStreamFilter` satır 70 (`held.setLength(0)`, sonra hiç okunmuyor, eşdeğer), `PromptBuilder` satır 66 (liste kapasitesi, eşdeğer), `MessageLabeler` satır 58 (süre) → `givenSlowModel_whenLabelMessage_thenLatencyCoversModelCall` ile kapatıldı. Kalan 3 mutant bir task kuralına ait değil.
- Stryker (web) çalıştırılmadı.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 597 test, 0 hata; satır coverage %95,7 (eşik %80).
- `cd web && npm run build` → geçti. `npm run lint` → uyarısız. `npm test` → 46 dosya, 748 test geçti. `npm run test:coverage` → satır %97,55.

## Çalıştırılamayanlar
- e2e (`acceptance.spec.ts`): web sitesi (localhost:3000) açık değil; qa çalıştırır. Gerçek model gerektiren M3 ölçümü qa'nındır.
- Tam paket PIT ve Stryker: yukarıda.

## Düzeltme
Yok. Uygulama kodunda hata bulunmadı.

## Düzeltme turu 2
Plan: "Düzeltme turu 2" + "Ek: M5 sonrası" (önce etiket, kısalık ≤ 10 kelime ve `?` yok). Beklenen değerler plandan elle türetildi.

| Madde | Test | Sonuç |
|---|---|---|
| T35 (değişti) | `MessageLabelerTest#givenMessage_whenLabelMessage_thenSystemMessageContainsLabelExamples` ("Bu bilgi çok yardımcı oldu → TESEKKUR", "Ya bireysel kredide → DIGER"); tur 1'deki tek kırmızı buydu | geçti |
| T36 (sıkılaştı) | `MessageLabelerTest#givenLabelPromptAndMeasurementList_whenCompared_thenNoMeasurementMessageOverlapsAnExample` (iki turlu satırların iki mesajı da; eşitlik + 3+ kelimede kelime sınırında içerme) | geçti |
| T40 | `SmallTalkDetectorTest#givenMessages_whenIsShortMessage_thenUpToTenWordsWithoutQuestionMark` (10 kelime true, 11 false, `?` false, "🙂 !!" false, "?evet" false) | geçti |
| T41-T45 | `AnswerServiceTest` (takip teşekkürü, DIGER + toplam 1 etiket, ilk turda etiket yok, 11 kelime / `?` yok sayılır, etiket hatası normal yol); ek: 10 kelime sınırı etiketlenir, eşik üstü DIGER etiketi tura yazılır, Sadece LLM'de etiket yok | geçti |
| T46 | `SendMessageGroundedAnswerIntegrationTest#givenAnsweredTurnThenUnlistedThanks_whenSendMessage_thenSmallTalkWithoutSearchOrCitation` (+ kısa DIGER mesajda etiket 1 kez, cevap `MODEL`) | geçti |

- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 614 test, 0 hata (sonradan "?evet" satırı eklendi; `SmallTalkDetectorTest` 53 test geçti).
- `cd web && npm test` → 46 dosya, 748 test geçti (web etkilenmedi).
- Hedefli PIT (`SmallTalkDetector`, `AnswerService`; birim testlerle): 37 mutant, 35 öldü; `?` sınırı mutantı için "?evet" satırı eklendi, kalan `AnswerService` log çağrısı eşdeğer.
- e2e koşulmadı (talimat: bilgi bankasına belge ekliyor; qa ölçümden sonra koşar). Uygulama kodunda hata yok.

STATUS: DONE
