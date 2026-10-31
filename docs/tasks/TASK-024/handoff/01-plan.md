TASK: TASK-024
# Plan

## Özet
**Mevcut durum (TASK-005 / TASK-027):** `TextChunker` her kısmı bloklara ayırır; yeni parça, öncekinin son tam satırlarıyla (≤ 200 karakter) başlar. Parçanın sayfası "ilk yeni satırın" (örtüşmeyle gelmeyen) sayfasıdır (TASK-005 Kararlar #4). "Sık sorulanlar" kısımlarında soru ve cevap ayrı bloklardır (soru `?`/`.` ile bitince blok kapanır); örtüşme bir cevabı sorusuz taşıyabiliyor (2026.10 chunk 122). `PdfDocumentParser` satırları düz metin olarak okur; kalın yazı bilgisi yoktur.

**Yeni durum (kart önerilerine göre: soru 1 = A, 2 = A, 3 = A, 4 = B):**
- **Sayfa:** parçanın sayfası, içeriğinin ilk satırının (örtüşme satırı dahil) sayfasıdır. Tek sayfa; veritabanı ve sözleşme değişmez.
- **Soru satırı:** `PdfDocumentParser` her satırın tamamen kalın olup olmadığını okur. Soru-cevap kısmındaki (Kararlar #3) tamamen kalın satır `PageLine.isQuestion = true` olur. Başka hiçbir yerde `isQuestion` true olmaz.
- **Soru-cevap çifti:** `TextChunker`, soru satırı taşıyan kısımda art arda soru satırları + sonraki soru satırına kadarki satırları tek blok yapar. Bu kısımda örtüşme yoktur. Çift 2.000'i aşarsa bölünür, her devam parçası sorunun satırlarıyla başlar.
- **Değişmeyenler (TASK kural 3):** bölüm/kısım sınırı, tablo satırının bölünmemesi, `PageMarginFilter`, yer imi kuralları, "Sık sorulanlar" satır kuralı ve başlık adları, 1.600 / 2.000 / 200, soru satırı olmayan kısımlarda örtüşme.
- **Değişen servis:** sadece retrieval-api (`ingestion/parsing`, `ingestion/chunking`, `IngestionJobRunner` logu). assistant-api, web, Docker değişmez. Migration yok.
- **Yeniden yükleme (kural 4):** var olan parçalar kendiliğinden değişmez; tabloya elle parça yazılmaz. Bugün yenileme = verileri silmek (`down -v`) + belge ekleme ucuyla yeniden yükleme. TASK-015'ten sonra yeni sürüm yükleme. Bu, `retrieval-api/CLAUDE.md`'ye yazılır (backend-coder).

**Yeniden adlandırma:** yok. `PageLine` yeni alan alır, eski iki argümanlı constructor kalır. Mevcut test `givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstNewLine` → `givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstLineIncludingOverlap` (beklenti değişir, T1). `givenGuide202610_whenParseAndChunk_then154ChunksWithAverage285Tokens` → T14.

**Kullanıcı başka seçenek seçerse:** "Kart cevabına göre değişecekler" bölümü.

## Çalışacak roller
- **planner:** bu plan, onay kartı. Sözleşme değişmez.
- **backend-coder:** `retrieval-api/src/main/**`, `retrieval-api/CLAUDE.md`. M0'ı lokal çalıştırır. Rapor `handoff/02-backend-implementation.md`.
- **tester:** `retrieval-api/src/test/**`. Rapor `handoff/03-test-report.md`.
- **qa:** M1, kabul kriterleri, kod review. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** frontend-coder (TASK'ta Web işaretli değil; alıntı gösterimi kapsam dışı). devops (Docker, Compose, nginx, model değişmiyor; yeniden yükleme geliştirme ortamında yapılır). qa, tester'a bağlıdır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Örtüşme soru-cevap çiftini bölmez; boyut sınırlarıyla uzlaşma kartta | `TextChunker` (soru-cevap blokları, bu kısımda örtüşme yok, uzun çiftin bölünmesi) | T3, T4, T5, T6, T7, T15, T16, M1 |
| K2 | Gösterilen sayfa, müşterinin parçanın metnini bulacağı sayfayla tutarlı; seçenekler kartta | `TextChunker.ChunkAssembler.emit` (sayfa = ilk satırın sayfası) | T1, T2, T8, T17, M1 |
| K3a | Genel okuyucunun diğer kuralları değişmez | `PdfDocumentParser`, `PageMarginFilter`, `TextChunker` blok kuralları | Mevcut `PdfDocumentParserTest`, `PageMarginFilterTest`, `TextChunkerTest` (T1 hariç) değişmeden geçer; T18 |
| K3b | Kurallar her belge için; SSS kuralı soru-cevap yapısı tanınınca | Parser: soru-cevap kısmı (Kararlar #3) + kalın satır (Kararlar #2) | T9, T10, T11, T12, T13 |
| K4 | Rehber belge ekleme ucuyla yeniden yüklenir; açılışta yükleme yok; yenileme yolu kartta; elle parça yazılmaz | M0 (`curl -F` ile), `retrieval-api/CLAUDE.md` | M0, M1, T19 |
| S1 | 2026.10 yeniden içe alınır; "Döviz alırken komisyon…" parçası ya cevabın sorusunu da taşır ya cevabı hiç taşımaz | K1 | T16, M0, M1 |
| S2 | Rastgele bir parçanın sayfası PDF'te açılır; ilk metni o sayfada | K2 | T17, M1 |
| AC1 | Gerçek PDF + gerçek `bge-m3` ile belge ekleme ucundan yükleme; parça sayısı, ortalama token, TASK-005 farkı raporda | backend-coder | M0 |
| AC2 | Örtüşmeyle başlayan en az 5 parça (chunk 122'nin karşılığı dahil) PDF ile elle karşılaştırılır | qa | M1 |
| AC3 | Örtüşme ve sayfa kuralları testli; `./mvnw verify` geçer | tester | Test senaryoları tablosu |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Soru iki satıra kayıyor | Art arda kalın satırlar tek soru; ikisi de cevapla aynı parçada | T5 |
| G2 | Tek soru-cevap çifti 2.000 karakterden uzun | Bölünür; her parça ≤ 2.000 ve sorunun satırlarıyla başlar; cevap satırları bir kez geçer | T6 |
| G3 | Soru-cevap kısmında ilk sorudan önce açıklama satırı | Açıklama bugünkü blok kuralıyla kalır, kaybolmaz; çiftler bütün kalır | T7 |
| G4 | Satırın sadece başı kalın ("Kur alarmı: …") ya da soru-cevap kısmı dışında tamamen kalın satır (tablo başlığı, ara başlık) | Soru sayılmaz | T10, T11 |
| G5 | Soru-cevap kısmında hiç kalın satır yok (kalın yazı okunamayan PDF) | Kısım bugünkü gibi örtüşmeli bölünür | T12, karttaki risk 1 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Soru bilgisi satırda taşınır:** `PageLine(int pageNumber, String text, boolean isQuestion)` + `PageLine(int pageNumber, String text)` (isQuestion false). Kalın yazıyı parser bilir, parçalayıcı sadece `isQuestion`'a bakar | PDF'e özgü bilgi parser'da kalır; mevcut testler ve çağrılar değişmez | `ParsedSection`'a soru listesi: satır-soru eşlemesi iki yerde tutulur. Parçalayıcıya font bilgisi: katman karışır |
| 2 | **Kalın satır:** `PdfDocumentParser` içinde `PDFTextStripper` alt sınıfı (private static nested `StyledLineStripper`) her sayfa için `List<ExtractedLine>` üretir; satır metni bugünkü gibi `strip()` edilir, boş satır atılır (metin sonucu bugünküyle aynı). Bir yazı tipi kalındır: `PDFontDescriptor.getFontWeight() >= 600` ya da `isForceBold()` ya da `+` önekinden sonraki yazı tipi adı (harf farksız) `bold`, `semibold`, `demi`, `black`, `heavy` içerir. Satır, boşluk olmayan bütün karakterleri kalın yazı tipindeyse kalındır | Rehberin soruları `SourceSans3-Semibold` ile basılır, cevaplar `-Regular`. Kart soru 3 = A | Yazı tipi boyutu ya da rengi: daha belirsiz. `?` ile biten satır: rehberde 3 soru kaçar (kart soru 3) |
| 3 | **Soru-cevap kısmı:** (a) "Sık sorulanlar" satır kuralı ya da `Sık sorulanlar` başlıklı alt yer imiyle açılan `<bölüm> · Sık sorulanlar` kısmı (bugünkü `startFaq`); (b) başlığı (yer imi metni, iç boşluklar tek boşluğa indirilmiş) `FAQ_TITLES` listesinden biriyle `String.equalsIgnoreCase` eşit kısım: `Sık sorulanlar`, `Sık sorulan sorular`, `Sıkça sorulan sorular`, `SSS`, `FAQ`, `Frequently asked questions`. Başlık adlandırması ve satır kuralı değişmez; sadece kısmın satırlarına `isQuestion` yazılır | Kart soru 4 = B. `equalsIgnoreCase` karakter karakter hem büyük hem küçük harf karşılaştırdığı için `SIK` / `Sık` ve `İ` / `i` eşleşir | `toLowerCase(Locale)`: `FAQ` / `I` dönüşümlerinde dile göre ayrışır. Başlıkta "içerir" araması: "Sık sorulanlar hakkında not" gibi başlıklar yanlışlıkla girer |
| 4 | **Soru-cevap bloğu:** soru satırı taşıyan kısımda bloklar: ilk sorudan önceki satırlar bugünkü `blocks()` kuralıyla; sonra her çift = art arda `isQuestion` satırları + sonraki `isQuestion` satırına ya da kısım sonuna kadarki satırlar. Çift ≤ `max-chunk-chars` ise tek bloktur (cümle sonu ve `•` bloğu kapatmaz) | Kart "Benim verdiğim kararlar" 1; çift parça sınırında da bölünmez | Sadece örtüşmeyi değiştirmek: parça sınırı soru ile cevabın arasına düşebilir |
| 5 | **Uzun çift:** soru satırları Q, uzunluk L(Q). Cevap satırları önce `splitLongLines`, sonra hedef `target-chunk-chars − L(Q) − 1` ile `splitLongBlock` kuralıyla bölünür; her parça = Q + parça. L(Q) + 1 ≥ `target-chunk-chars` ise çift bugünkü uzun blok kuralıyla bölünür (önek yok) | K1: devam parçası cevabı sorusuz taşımaz; her parça ≤ 2.000 | Soruyu sadece ilk parçaya koymak: chunk 122 sorunu geri gelir |
| 6 | **Örtüşme:** soru satırı taşıyan kısımda `overlap(...)` boş döner (ilk sorudan önceki satırlar dahil bütün kısım). Diğer kısımlarda bugünkü gibi | Kart soru 2 = A; sorular bağımsız | Örtüşme son çifti bütün taşır (kart soru 2 B): aynı çift iki parçada, aramada çift isabet |
| 7 | **Sayfa:** `emit`, `currentLines.getFirst().pageNumber()` kullanır (örtüşme kırpıldıysa kalan ilk satır). `newLineCount` sadece "parçada yeni satır var mı" için kalır | Kart soru 1 = A; lab parça listesinde "s. N" ile "İlk satır"ı yan yana gösterir | Sayfa aralığı (kart soru 1 B): migration + alan, lab görünümünden ayrılır |
| 8 | **Soru sayısı:** `ParsedDocument.questionCount()` türetilmiş metot: her kısımda, önceki satırı soru olmayan (ya da kısmın ilk satırı olan) `isQuestion` satır sayısı. Constructor değişmez. `Document parsed` loguna `questionCount={}` eklenir | M0'da rehberin 105 sorusunun tanındığı logdan görülür | Ayrı sayaç alanı: constructor'ı ve testleri değiştirir |
| 9 | **Sabitler kodda:** `FAQ_TITLES`, kalın yazı tipi işaretleri ve 600 ağırlık eşiği `PdfDocumentParser`'da `private static final`. Yeni property yok | Bugünkü `FAQ_HEADING` gibi okuyucu kuralıdır, ortama göre değişmez | `ingestion.*` property: ayar yüzeyi büyür, değer değişince eski parçalar tutarsızlaşır |
| 10 | **Eski parçalar kendiliğinden değişmez,** migration ya da açılışta yeniden işleme yok. Yenileme yolu `retrieval-api/CLAUDE.md` "Komutlar"da | Kural 4; elle parça yazılmaz, açılışta yükleme yok (TASK-027) | Açılışta eski parçaları yeniden bölmek: TASK-027'nin "açılışta hiçbir şey" kuralını bozar |

## Sözleşme değişiklikleri
Yok. `docs/api/openapi.yaml`'da parçanın sayfasını taşıyan bir alan henüz yok (arama TASK-006, parça listesi TASK-007); `IngestionErrorCode` ve diğer şemalar değişmez. `sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `rag-pipeline/references/indexing.md`, `writing-good-tests` (fixture'larla birlikte okunması için), `java-coding-standards`.

### Dependency
Yok. PDFBox 3.0.6 (`PDFTextStripper`, `TextPosition`, `PDFontDescriptor`) zaten var.

### Migration / properties
Yok.

### Sınıflar (`com.rasitbank.retrieval.ingestion`)
**parsing/**
- `PageLine`: `record PageLine(int pageNumber, String text, boolean isQuestion)`; ek constructor `PageLine(int pageNumber, String text)` → `this(pageNumber, text, false)`. Javadoc: soru satırı bilgisi de taşıdığı yazılır.
- `ExtractedLine` (yeni, public record): `(String text, boolean isBold)`. Sayfadan okunmuş, kalınlık bilgili satır.
- `PageMarginFilter`: yeni generic overload `public <T> List<List<T>> removeRepeatedMargins(List<List<T>> pageLines, Function<T, String> textOf)`; mevcut `List<List<String>> removeRepeatedMargins(List<List<String>>)` buna `Function.identity()` ile devreder. Algoritma ve `marginKey` değişmez.
- `PdfDocumentParser`:
  - `pageLines(...)` → `List<List<ExtractedLine>>`, `StyledLineStripper` ile (Kararlar #2). `removeRepeatedMargins(..., ExtractedLine::text)`. `PDF_NO_TEXT`, yer imi ve başlık eşleme bugünkü gibi metin üzerinden.
  - `SectionCollector`: `currentIsFaq` tutar. `startFaq()` → true; `startHeading(bookmark)` → `isFaqTitle(shortened)` (Kararlar #3). `addLine(int pageNumber, ExtractedLine line)` → `new PageLine(pageNumber, line.text(), currentIsFaq && line.isBold())`.
  - `private static boolean isFaqTitle(String title)`, sabit `FAQ_TITLES`.
- `ParsedDocument`: `public int questionCount()` (Kararlar #8).

**chunking/**
- `TextChunker.chunk(List<ParsedSection>)` imzası değişmez.
  - Kısımda `isQuestion` satırı varsa bloklar Kararlar #4-#5'e göre, yoksa bugünkü `sizedBlocks`.
  - `ChunkAssembler`: kısım başında örtüşme açık/kapalı (Kararlar #6). `emit` sayfayı Kararlar #7'ye göre verir. Diğer kurallar (hedef aşılınca kapanma, örtüşme + blok > max ise örtüşmenin baştan kırpılması, `chunkNo`, `tokenCount`) değişmez.
  - Sınıf Javadoc'u soru-cevap kuralını da anar.
- `ChunkDraft`: değişmez.

**service/**
- `IngestionJobRunner`: `Document parsed jobId={} pageCount={} chapterCount={} sectionCount={} questionCount={} removedMarginLines={}`. Soru ve cevap metni loglanmaz.

### `retrieval-api/CLAUDE.md` (backend-coder)
- "PDF okuma" paragrafı: kalın satır ve soru-cevap kısmı (Kararlar #2, #3).
- Parçalama paragrafı (yeni): sayfa = ilk satırın sayfası; soru-cevap çifti tek blok, bu kısımda örtüşme yok, uzun çiftin devam parçaları soruyla başlar.
- "Komutlar": kurallar değişince var olan parçalar kendiliğinden değişmez; yenileme: `docker compose -f deployment/docker-compose.dev.yml down -v` + `up -d`, servis açılır, belge `curl -F` ile yeniden yüklenir (tam sistemde `deployment/docker-compose.yml down -v`). TASK-015'ten sonra yeni sürüm yükleme. Parçalar elle yazılmaz.

## Frontend planı
Yok (frontend-coder atlandı).

## Test senaryoları
Fixture'lar: `PdfFixtures`'a kalın satırlı sayfa desteği eklenir (ör. `PageSpec` satırında kalın işareti ya da ayrı `faqPdf(...)`); yazı tipleri `../docs/rehber/kaynak/fontlar/SourceSans3-Regular.ttf` ve `SourceSans3-Semibold.ttf` (OFL, repoda var; rehberle aynı yazı tipi, yeni test kaynağı eklenmez). Parçalayıcı testlerinde satırlar `new PageLine(page, text, true/false)` ile kurulur. Rehber oracle'ı: `../docs/rehber/kaynak/bolumler/*.typ` içindeki `#soru[<soru>][<cevap>]` çiftleri (105 çift; sorularda ve cevapların ilk 25 karakterinde Typst işareti yok; rehberde akıllı tırnak ve heceleme kapalı). Karşılaştırmada boşluklar tek boşluğa indirilir. Testler gerçek modele gitmez.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T1 | Sayfa = ilk satır (örtüşme dahil) | unit `TextChunkerTest` (mevcut T5 güncellenir) | `givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstLineIncludingOverlap` | 20 × 100 karakterlik satır, ilk 15'i s. 7. İkinci parçanın ilk satırı 15. satır (s. 7) → sayfa **7** (eskiden 8) |
| T2 | Kırpılmış örtüşmede sayfa | unit | `givenOverlapTrimmedBecauseBlockNearMax_whenChunk_thenPageNumberIsPageOfFirstRemainingLine` | Örtüşme satırı s. 7, sonraki blok s. 8'de ve örtüşme + blok > 2.000 → örtüşme atılır, sayfa **8** |
| T3 | Çiftler bütün, örtüşme yok | unit | `givenFaqSectionWithTenPairs_whenChunk_thenEveryPairWholeInExactlyOneChunkAndNoLineRepeated` | 10 çift: soru 60 karakter `?` ile biter, cevap 2 × 150 karakterlik `.` ile biten satır (çift 362). Hedef 1.600 → 4 + 4 + 2 çift = 3 parça; her parçanın ilk satırı bir soru; hiçbir satır iki parçada yok |
| T4 | Soru-cevap kısmında cümle sonu bloğu kapatmaz | unit | `givenAnswerOfThreeSentenceLines_whenChunk_thenQuestionAndAllAnswerLinesInSameChunk` | Önceki çiftlerle 1.500 karakter dolu; yeni çift 300 → yeni parçaya bütün geçer, soru eski parçada kalmaz |
| T5 | İki satırlı soru (G1) | unit | `givenQuestionWrappedOverTwoLines_whenChunk_thenBothQuestionLinesStayWithTheAnswer` | Art arda iki `isQuestion` satırı + cevap; ikisi ve cevap aynı parçada; parça sınırı ilk soru satırından önce |
| T6 | Uzun çift (G2) | unit | `givenPairLongerThan2000_whenChunk_thenEveryPieceStartsWithQuestionAndIsAtMost2000` | Soru 50 karakter, cevap 30 × 100 karakterlik satır (≈ 3.080). ≥ 2 parça; her biri ≤ 2.000 ve ilk satırı soru; her cevap satırı tam bir kez |
| T7 | Giriş metni (G3) | unit | `givenIntroLineBeforeFirstQuestion_whenChunk_thenIntroKeptOnceAndPairsStayWhole` | Giriş satırı bir kez görünür; her çift bütün |
| T8 | SSS parçasının sayfası | unit | `givenPairWhoseQuestionIsOnPage55AndAnswerOnPage56_whenChunk_thenChunkPageIs55` | Parça bu soruyla başlıyor → sayfa **55** |
| T9 | Kalın soru işaretlenir | unit `PdfDocumentParserTest` (fixture, gerçek yazı tipleri) | `givenFaqSectionWithSemiboldQuestions_whenParse_thenOnlySemiboldLinesAreQuestions` | Yer imi `2 Kartlar` > `Sık sorulanlar`; satırlar: kalın `Kart aidatı ne kadar?`, düz `Yıllık 1.650 TL.`, kalın `Ek kart ücretli mi?`, düz `Hayır.` → `isQuestion` [true, false, true, false]; başlık `2 Kartlar · Sık sorulanlar` (değişmedi) |
| T10 | SSS dışında kalın satır (G4) | unit | `givenSemiboldLineOutsideFaqSection_whenParse_thenNotAQuestion` | `2.1 Kart çeşitleri` kısmında tamamen kalın `Kart Yıllık ücret` satırı → `isQuestion` false |
| T11 | Kısmen kalın satır (G4) | unit | `givenFaqLineWithOnlyBoldPrefix_whenParse_thenNotAQuestion` | SSS kısmında `Kur alarmı:` kalın + ` Piyasalar ekranından…` düz, tek satır → false |
| T12 | Kalın satırsız SSS (G5) | unit `TextChunkerTest` + `PdfDocumentParserTest` | `givenFaqSectionWithoutBoldLines_whenParse_thenNoQuestions` / `givenSectionWithoutQuestionLines_whenChunk_thenOverlapAsBefore` | Hiç `isQuestion` yok; parçalama mevcut T2'deki örtüşmeyi verir |
| T13 | Liste başlıkları | unit `PdfDocumentParserTest` | `givenBookmarkTitledSikcaSorulanSorularInUpperCase_whenParse_thenBoldLinesAreQuestions` / `givenBookmarkTitledSorularVeCevaplar_whenParse_thenNoQuestions` | `SIKÇA SORULAN SORULAR` → kalın satırlar soru; `Sorular ve cevaplar` (listede yok) → soru yok |
| T14 | Rehberin 105 sorusu tanınır | unit `PdfDocumentParserGuideTest` (mevcut 154/285 testinin yerine) | `givenGuide202610_whenParse_thenEach105TypstQuestionIsAQuestionLineGroup` | `questionCount()` = 105; art arda soru satırlarının birleşimi kümesi = `.typ`'teki 105 soru kümesi |
| T15 | Rehberde cevap sorusuyla | unit | `givenGuide202610_whenParseAndChunk_thenEveryAnswerSitsInTheChunkOfItsQuestionAndEachQuestionInOneChunk` | Her çift için: cevabın ilk 25 karakterini içeren her parça soruyu da içerir; soruyu içeren parça tam 1 |
| T16 | S1 senaryosu | unit | `givenGuide202610_whenParseAndChunk_thenDovizKomisyonChunkHasNoOrphanLimitEmriAnswer` | `Döviz alırken komisyon alıyor musunuz?` içeren parça `Raşit Mobil'den limit emri verebilirsiniz.` içeriyorsa `Dolar belli bir kura gelince otomatik almak istiyorum.` da içerir; parçanın ilk satırı bir soru |
| T17 | S2: ilk satır gösterilen sayfada | unit | `givenGuide202610_whenParseAndChunk_thenEveryChunkFirstLineIsOnItsPage` | Test her sayfanın metnini ayrıca `PDFTextStripper` ile okur; her parçanın ilk içerik satırı (boşluklar indirilmiş) `pageNumber` sayfasının metninde geçer; ilk parçanın sayfası 5 |
| T18 | Rehber sayıları (gerileme kilidi) | unit | `givenGuide202610_whenParseAndChunk_thenChunkCountAndAverageTokensAsInM0Report` | Parça sayısı ve `round(avg(tokenCount))` backend raporundaki M0 değerleri (literal); her parça ≤ 2.000 ve "Raşit Gold" satırı testi değişmeden kalır |
| T19 | Uçtan uca yükleme | integration `SourceUploadIntegrationTest` (mevcut, 154 beklentileri güncellenir) | `givenFreshDatabase_whenCreateSourceWithGuide_thenJobDoneSource10001WithChunkCountOfM0Report` | iş `DONE`; `chunk_count` = `count(chunks)` = `total_chunks` = `embedded_chunks` = M0 sayısı; `page_number` 5-70; diğer beklentiler (10001, 70 sayfa, 11 bölüm) aynı. Aynı dosyadaki 216. satırdaki `154` de M0 sayısı olur |
| T20 | Soru sayısı | unit `ParsedDocument` (`ResponseAndDraftTest` ya da yeni `ParsedDocumentTest`) | `givenSectionsWithTwoSingleAndOneTwoLineQuestions_whenQuestionCount_thenThree` | 3 |

**Elle çalıştırılanlar (gerçek PDF + gerçek `bge-m3`):**
- **M0 (backend-coder, lokal):** dev postgres `down -v` + `up -d`, Ollama açık, servis açık. Rehber 2026.10 `curl -F file=@… -F name='Raşit Bankası Ürün ve Hizmet Rehberi' -F version=2026.10 localhost:8080/api/workspaces/42/sources` ile yüklenir; iş `DONE`. Raporda: parça sayısı, `round(avg(token_count))`, süre, `questionCount` (log, 105 beklenir); TASK-005 ile fark (154 parça, ort. 285 token); soru-cevap kısımlarındaki ve diğer kısımlardaki parça sayısı ayrı ayrı (`heading like '% · Sık sorulanlar'`). Veriler qa için bırakılır.
- **M1 (qa):** (1) `select chunk_no, page_number, heading, left(content, 120) from chunks where content like '%Döviz alırken komisyon%'` → PDF'te: ilk metin gösterilen sayfada; "limit emri verebilirsiniz" varsa "Dolar belli bir kura" da var. (2) Önceki parçayla aynı başlığı taşıyan (örtüşmeyle başlayan), soru-cevap kısmında olmayan parçalardan `order by random() limit 5`: ilk metin gösterilen sayfada, ilk satırları önceki parçanın son satırları. (3) Bir soru-cevap kısmının bütün parçalarında her parçanın bir soruyla başladığı ve hiçbir cevabın sorusundan ayrı olmadığı. (4) Veritabanındaki sayı = M0 raporundaki sayı. (5) Logda soru/cevap metni yok.

## Kart cevabına göre değişecekler
- **Soru 1 = B (sayfa aralığı):** `V6__add_chunk_last_page.sql` (`chunks.last_page_number int NOT NULL`, CHECK `>= page_number`); `ChunkDraft.lastPageNumber`; `Chunk` alanı. Sözleşme yine değişmez (alanı taşıyan uç yok); TASK-006/007 alanı ekler. T1/T2/T8 aralığı da doğrular.
- **Soru 1 = C:** `TextChunker` sayfa değişiminde parçayı kapatır; T18 sayısı belirgin artar.
- **Soru 2 = B/C:** Kararlar #6 değişir: B'de örtüşme = son bütün çift (≤ max), T3 "hiçbir satır iki parçada yok" yerine "tekrar eden satırlar bir bütün çifttir". C'de örtüşme = 200'e sığan bütün çiftler.
- **Soru 3 = B/C:** Kararlar #2: `StyledLineStripper` gerekmez (B) ya da kalın veya `?` (C); T9-T11 ve T14 beklentileri (B'de 102 soru) değişir.
- **Soru 4 = A:** `FAQ_TITLES` sadece `Sık sorulanlar`; T13'ün ilk testi "soru yok" olur.

## Deployment planı
Yok. Docker, Compose, nginx ve modeller değişmez (`bge-m3` TASK-003'ten). Açık bırakılmış Docker sistemindeki eski parçalar bu task'ta yenilenmez; yol `retrieval-api/CLAUDE.md`'de.

## Açık sorular ve riskler
1. **Kart soruları (4):** sayfa gösterimi, SSS'de örtüşme, soru satırının tanınması, soru-cevap kısmı başlıkları. Plan önerilere göre yazıldı.
2. **Kalın yazı tanıma (risk):** Kalınlığı yazı tipi adı ya da ağırlığıyla değil, kontur kalınlığıyla çizen PDF'lerde sorular tanınmaz (G5). Rehberde sorun yok; T14 bunu gerçek PDF'le doğrular. T14 kalırsa backend-coder `StyledLineStripper`'ın satır eşlemesini raporuna yazar; beklenti sessizce değiştirilmez.
3. **Rehberin parça sayısı:** T18/T19'un sayısı M0'dan gelir; tester raporda değeri M0 ile karşılaştırır, qa veritabanıyla. TASK-005'e göre fark beklenir (soru-cevap kısımları yeniden bölünür, diğer kısımların sınırları aynı kalır, sadece sayfaları değişebilir).
4. **Kapsam dışı not:** TASK-026 (rehber üretiminde biçim kontrolü) soruların kalın basıldığını da kontrol edebilir; bu task'ta yapılmaz.
5. **Kapsam dışı not:** `.claude/skills/rag-pipeline/references/indexing.md` §3 sayfa ve örtüşme kuralını anlatmıyor; bu dosya kullanıcının.

STATUS: DONE
