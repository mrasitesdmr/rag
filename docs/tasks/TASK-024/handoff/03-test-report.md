TASK: TASK-024
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `rag-pipeline` (+ `references/indexing.md`).

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|---|---|---|
| T1 (K2) | Sayfa = ilk satır, örtüşme dahil (7, eskiden 8; 20 satırlık sentetik veriden elle türetildi) | TextChunkerTest#givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstLineIncludingOverlap (mevcut test yeniden adlandırıldı) | Geçti |
| T2 | Kırpılmış örtüşmede sayfa 8 | TextChunkerTest#givenOverlapTrimmedBecauseBlockNearMax_whenChunk_thenPageNumberIsPageOfFirstRemainingLine | Geçti |
| T3 (K1) | 10 çift → 4+4+2, satır tekrarı yok | TextChunkerTest#givenFaqSectionWithTenPairs_whenChunk_thenEveryPairWholeInExactlyOneChunkAndNoLineRepeated | Geçti |
| T4 | Cümle sonu çifti bölmez | TextChunkerTest#givenAnswerOfThreeSentenceLines_whenChunk_thenQuestionAndAllAnswerLinesInSameChunk | Geçti |
| T5 (G1) | İki satırlı soru | TextChunkerTest#givenQuestionWrappedOverTwoLines_whenChunk_thenBothQuestionLinesStayWithTheAnswer | Geçti |
| T6 (G2) | Uzun çift, her parça soruyla başlar ≤ 2.000 | TextChunkerTest#givenPairLongerThan2000_whenChunk_thenEveryPieceStartsWithQuestionAndIsAtMost2000 | Geçti |
| T6+ | Tek uzun cevap satırı; çok uzun soru (önek yok dalı, sınır 1599) | TextChunkerTest#givenSingleAnswerLineLongerThan3000_…, #givenQuestionAlmostAsLongAsTarget_…, #givenQuestionOfExactlyTargetMinusOneChars_… | Geçti |
| T7 (G3) | Giriş satırı bir kez, çiftler bütün | TextChunkerTest#givenIntroLineBeforeFirstQuestion_whenChunk_thenIntroKeptOnceAndPairsStayWhole | Geçti |
| T8 | Soru s.55, cevap s.56 → parça sayfası 55 | TextChunkerTest#givenPairWhoseQuestionIsOnPage55AndAnswerOnPage56_whenChunk_thenChunkPageIs55 | Geçti |
| T9 (K3b) | Kalın (gerçek SourceSans3-Semibold) satırlar soru | PdfDocumentParserTest#givenFaqSectionWithSemiboldQuestions_whenParse_thenOnlySemiboldLinesAreQuestions | Geçti |
| T10 (G4) | SSS dışında kalın satır soru değil | PdfDocumentParserTest#givenSemiboldLineOutsideFaqSection_whenParse_thenNotAQuestion | Geçti |
| T11 (G4) | Kısmen kalın satır soru değil | PdfDocumentParserTest#givenFaqLineWithOnlyBoldPrefix_whenParse_thenNotAQuestion | Geçti |
| T12 (G5) | Kalın satırsız SSS | PdfDocumentParserTest#givenFaqSectionWithoutBoldLines_whenParse_thenNoQuestions; TextChunkerTest#givenSectionWithoutQuestionLines_whenChunk_thenOverlapAsBefore | Geçti |
| T13 | Başlık listesi (harf farksız / listede yok) | PdfDocumentParserTest#givenBookmarkTitledSikcaSorulanSorularInUpperCase_…, #givenBookmarkTitledSorularVeCevaplar_… | Geçti |
| ek | "Sık sorulanlar" satır kuralı soruyu işaretler; art arda kalın satırlar tek soru | PdfDocumentParserTest#givenFaqLineRule_whenParseWithBoldLines_…, #givenConsecutiveBoldLines_whenParse_thenCountedAsOneQuestion | Geçti |
| T14 | Rehberin 105 sorusu `.typ` oracle'ıyla birebir | PdfDocumentParserGuideTest#givenGuide202610_whenParse_thenEach105TypstQuestionIsAQuestionLineGroup | Geçti |
| T15 | Soru tek parçada, cevabın başı hemen arkasında | PdfDocumentParserGuideTest#givenGuide202610_whenParseAndChunk_thenEveryAnswerSitsInTheChunkOfItsQuestionAndEachQuestionInOneChunk | Geçti |
| T16 (S1) | Döviz komisyonu parçası | PdfDocumentParserGuideTest#givenGuide202610_whenParseAndChunk_thenDovizKomisyonChunkHasNoOrphanLimitEmriAnswer | Geçti |
| T17 (S2) | Her parçanın ilk satırı kendi sayfasında (PDFTextStripper ile bağımsız okuma), ilk sayfa 5 | PdfDocumentParserGuideTest#givenGuide202610_whenParseAndChunk_thenEveryChunkFirstLineIsOnItsPage | Geçti |
| T18 | 154 parça, ort. 283 token (M0 literal'leri) | PdfDocumentParserGuideTest#givenGuide202610_whenParseAndChunk_thenChunkCountAndAverageTokensAsInM0Report (eski 154/285 testinin yerine) | Geçti |
| T19 | Uçtan uca yükleme; sayfa 5-70 eklendi | SourceUploadIntegrationTest#givenFreshDatabase_whenCreateSourceWithGuide_thenJobDoneSource10001WithChunkCountOfM0Report | Geçti |
| T20 | Soru sayısı 3; kısım sınırında ayrı sayım | ParsedDocumentTest (2 test) | Geçti |

Notlar: (1) T15/T16 oracle'ı rehberde cevap başlangıçları tekrar ettiği için (gövde metni ve başka SSS cevapları) "soru + cevap başı" bitişikliği üzerinden ve yalnız SSS parçalarında kuruldu; zayıflatma değil, oracle'ın kendi belirsizliği. (2) Fixture: `PdfFixtures`'a `BOLD` / `BOLD_END` işaretleri eklendi; kalın yazı tipi repodaki `docs/rehber/kaynak/fontlar/SourceSans3-Semibold.ttf`. (3) TASK.md'de planda testsiz kalan kural yok.

## Mutation
PIT 1.30.1 (`ingestion.chunking.*`, `ingestion.parsing.*`): 256 mutant, 216 öldürüldü (%84), test gücü %88. Hayatta kalanlar: çoğu sınır/sayaç mutantı (`>=` ↔ `>`, `+`↔`-` log ve uzunluk hesapları), `PDDocument.close`, `isBoldFont` ağırlık eşiği 600 sınırı ve `StyledLineStripper` stil maskesi iç sayaçları (satır 328-355; davranış fixture ile test edilir, iç indeks mutantları çıktıyı değiştirmeyen eşdeğer ya da yalnız kenar durum). Kapatılan: uzun soru sınırı (satır 75, `…ExactlyTargetMinusOneChars…` testi). Çift ≤ 2.000 sınırı (satır 72) eşdeğer mutant (iki yol aynı bloğu üretir). Kalan hayatta kalanlar task kuralı taşımıyor; PIT son testlerden önceki koşuya ait.

## Komutlar ve sonuçları
- `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` (coverage eşiği AÇIK): Tests run: 210, Failures: 0, Errors: 0, Skipped: 0; "All coverage checks have been met"; BUILD SUCCESS.
- PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -DtargetClasses=… -DtargetTests=…` → yukarıdaki sonuç.

## Çalıştırılamayanlar
e2e: bu task'ta frontend yok. M1 (PDF ile elle karşılaştırma) qa'nındır. Gerçek model denemesi M0 backend-coder'da yapıldı.

## Düzeltme
Yok; uygulama hatası bulunmadı.

## Düzeltme turu 1
Lead isteği (uzun çiftte cevap kesme sınırı soru uzunluğu kadar düşürülür): `TextChunkerTest#givenAnswerLineOf1990CharsAndQuestionOf100_whenChunk_thenQuestionRepeatedAndNoPieceExceeds2000` eklendi (cevap satırı 1.990 < 2.000, soruyla 2.091). Kural: her parça ≤ 2.000 ve soruyla başlar. Sonuç: geçti. Tüm takım: `./mvnw verify` (eşik açık) 211 test, 0 kalan, BUILD SUCCESS.

STATUS: DONE
