TASK: TASK-012
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `react-testing`, `spring-mvc-testing`.

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|---|---|---|
| B1 | Yükle + oku | `SuggestionControllerIntegrationTest#givenValidSet_whenReplaceThenGet_thenSameOrderAndCounts` | geçti |
| B2 | Tamamen değiştirir | `SuggestionControllerIntegrationTest#givenExistingSet_whenReplaceWithAnother_thenOnlyNewSetRemains` | geçti |
| B3 | Alan kuralları | `SuggestionControllerTest#givenInvalidField_whenReplace_thenValidationFailedWithPath` (7 parametre) + `#givenFiftyOneCategories_...` | geçti |
| B4 | Benzersizlik, takip kuralları | `SuggestionSetValidatorTest` (soru, kategori adı, takip tekrarı/=soru, Unicode boşluk, ASCII boşluk tek ihlal, null alanlar) | geçti |
| B5 | Toplam 500 sınırı | `SuggestionSetValidatorTest#givenFiveHundredOneQuestions_...` / `#givenExactlyFiveHundredQuestions_...` | geçti |
| B6 | Hatalı yükleme listeyi bozmaz | `SuggestionControllerIntegrationTest#givenExistingSet_whenInvalidReplace_thenSetUnchanged` | geçti |
| B7 | Temizleme | `SuggestionControllerIntegrationTest#givenExistingSet_whenReplaceWithEmptyCategories_thenEmptySet` | geçti |
| B8 | Seed yok | `SuggestionControllerIntegrationTest#givenFreshDatabase_whenGetSuggestions_thenEmptySet` | geçti |
| B9 | 415 / 400 / 405 | `SuggestionControllerTest#givenTextPlain_...`, `#givenMalformedJson_...`, `#givenNonBooleanStarter_...`, `#givenPost_whenSuggestions_thenMethodNotAllowed` | geçti |
| B10 | Eşzamanlı yükleme | `SuggestionServiceIntegrationTest#givenTwoConcurrentReplaces_whenBothFinish_thenSetIsExactlyOneOfThem` (3 tekrar) | geçti |
| B11 | Kırpma ve varsayılanlar | `SuggestionControllerIntegrationTest#givenPaddedTextsAndOmittedFields_...` | geçti |
| B12 | Normalizasyon | `SuggestionTextNormalizerTest` (5 parametre) | geçti |
| Mevcut | V4 migration sayısı | `AssistantApplicationIntegrationTest#givenEmptyDatabase_whenContextStarts_thenFlywayAppliesAllFourMigrations` (3→4) | geçti |
| F1 | Eşleştirme (kök, IDF, önek, durak kelime, 3 harf) | `suggestionMatcher.test.ts` | geçti |
| F2 | Çip mantığı (G5 dahil) | `suggestions.test.ts#suggestionChips` (7 durum + hepsi sorulmuş + starter yok) | geçti |
| F3 | Otomatik tamamlama (7 sınırı, takip önce, sıra) | `suggestions.test.ts#autocompleteItems` | geçti |
| F3b | Başlangıç çipi | `PreviewPage.suggestions.test.tsx#givenEmptyConversation_whenStarterChipClicked_thenQuestionAsked` | geçti |
| F4 | Takip çipi aynı konuşmada | `PreviewPage.suggestions.test.tsx#givenAnsweredListQuestion_whenFollowUpChipClicked_thenSameConversation` | geçti |
| F5 | Diğer çip yeni konuşmada | `PreviewPage.suggestions.test.tsx#givenConversationWithTurn_whenQuestionChipClicked_thenNewConversation` | geçti |
| F6 | Akış sürerken çipler kapalı | `PreviewPage.suggestions.test.tsx#givenStreamingAnswer_whenRendered_thenChipsDisabled` | geçti |
| F7 | Klavye ↓ Enter / Esc | `MessageComposer.test.tsx#givenTypedAsgari_whenArrowDownAndEnter_...`, `#givenOpenList_whenEscape_...`; sayfa: `PreviewPage.suggestions.test.tsx#...TypingAsgariArrowDownEnter...` | geçti |
| F8 | Eşleşmeyen metin | `MessageComposer.test.tsx#givenUnlistedText_whenEnter_thenTypedTextSent`; sayfa: `#givenUnlistedQuestion_whenEnter_thenAskedAsTyped` | geçti |
| F9 | Gruplar | `PreviewPage.suggestions.test.tsx#givenConversationWithTurn_whenTyping_thenFollowUpAndNewConversationGroups` | geçti |
| F10 | Pencere (başlık, sıra, tıklama, odak, Esc) | `PreviewPage.suggestions.test.tsx#givenSuggestions_whenQuestionListOpenedAndQuestionClicked_...`, `#...NoFollowUpHeadingAndEscapeClosesWithFocusBack`, `#givenStreamingAnswer_whenQuestionListOpened_...` | geçti |
| F11 | Boş durum | `PreviewPage.suggestions.test.tsx#givenEmptySuggestionSet_whenPageOpens_thenEmptyStates` | geçti |
| F12 | Okunamayan öneriler | `PreviewPage.suggestions.test.tsx#givenSuggestionsFail_whenPageOpens_thenNoChipsDialogAlertAndChatWorks` | geçti |
| F13 | Erişilebilirlik (axe) | `PreviewPage.suggestions.test.tsx#givenChips_...`, `#givenQuestionListDialog_...`; `MessageComposer.test.tsx#givenOpenAutocomplete_whenRendered_thenNoAxeViolations`; ayrıca `listbox`/`option`/`aria-activedescendant` assertion'ları | geçti |
| F14 | Döngüsel gezinme | `MessageComposer.test.tsx#givenThreeItems_whenArrowUpFromStart_thenWrapsToLast`, `#givenLastItemActive_whenArrowDown_thenWrapsToFirst` | geçti |
| F15 | Normalizasyon | `suggestionText.test.ts` | geçti |
| E1 | Gerçek sistem | `web/e2e/preview.spec.ts#givenRunningSystem_whenPreviewOpens_thenChipsMatchSuggestionSet` | yazıldı, çalıştırılmadı (aşağıda) |

Planda olmayıp eklenenler: akış sırasında "Soru listesi" düğmesi etkin (`MessageComposer.test.tsx`), sorulmamış seçim sıfırlama (yazınca `aria-activedescendant` temizlenir), blur ile liste kapanır, tek-ok/Enter'da yazılan metnin gönderilmesi.

Kırık testlerin düzeltilmesi: migration testi 3→4; `MessageComposer.test.tsx` yeni props ve `combobox` rolüyle yeniden yazıldı; `PreviewPage.test.tsx` ve `e2e/preview.spec.ts` içinde `textbox` → `combobox`; `src/test/server.ts`'e varsayılan boş `GET /api/assistant/suggestions` handler'ı; yeni `src/test/suggestionFixtures.ts`.
Not: Plan F3b–F13'ü `PreviewPage.test.tsx` diye adlandırıyordu; dosya şişmesin diye `PreviewPage.suggestions.test.tsx` ayrı dosya olarak yazıldı.

## Mutation
- Backend PIT (`suggestion.*`): 89 mutant, 84 öldü (%94), test strength %98. Hayatta kalan/kapsamsız 5 mutant, hiçbiri task kuralı değil:
  - `SuggestionSetValidator#isValid` "true→false" (satır 30): ihlaller `buildConstraintViolation` ile eklendiği için dönüş değeri gözlemlenemez, eşdeğer mutant.
  - 4 × NO_COVERAGE: entity getter'ları (`getPosition`, `SuggestedFollowUp.getId`), kuralı olmayan düz erişimciler.
  - İlk koşuda hayatta kalan `isBlankOnlyAfterStrip` mutantı iki test eklenerek öldürüldü (ASCII boşluk tek ihlal, null ad/metin).
- Web Stryker: Kurulu değil (`web/CLAUDE.md`), çalıştırılmadı.

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw verify` (bayraksız, `-Djacoco.haltOnFailure=false` yok): 262 test, 0 hata, "All coverage checks have been met", BUILD SUCCESS.
- `cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` (suggestion paketi): yukarıda.
- `cd web && npm run build`: geçti (tsc -b dahil; önceki 4×TS2739 giderildi).
- `cd web && npm run lint`: 0 uyarı, 0 hata.
- `cd web && npm test`: 28 dosya, 481 test, 0 başarısız (önceki 50 başarısız giderildi).
- `cd web && npm run test:coverage`: satır %97,04, dal %91,19, fonksiyon %98,15 (eşik %80).
- `npx playwright test --list`: e2e dosyaları derleniyor, 16 test listelendi.

## Çalıştırılamayanlar
- E2E (E1 ve mevcut `preview.spec.ts`): çalışan sistem (Docker Compose, Ollama, reranker, retrieval-api) gerekiyor; qa çalıştırır. `preview.spec.ts` içinde soru alanı rolü `combobox`'a güncellendi.
- M1–M5 elle kontroller qa'nındır.

STATUS: DONE
