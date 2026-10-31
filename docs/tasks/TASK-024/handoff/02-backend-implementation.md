TASK: TASK-024
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline` (+ `references/indexing.md`).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Kararlar #1 (soru bilgisi satırda) | `parsing/PageLine.java` | `record PageLine(int pageNumber, String text, boolean isQuestion)` + iki argümanlı constructor (`isQuestion = false`). Javadoc güncellendi |
| Kararlar #2 (kalın satır) | `parsing/ExtractedLine.java` (yeni), `parsing/PdfDocumentParser.java` | `StyledLineStripper` (private static nested, `PDFTextStripper` alt sınıfı): `writeString(String, List<TextPosition>)` her kelimeye kalın/düz işareti koyar, kendi `Writer`'ı çıktıyla aynı uzunlukta bir stil maskesi tutar. Satırlar bugünkü gibi `\n`/`\r`/`\r\n`'de bölünür, `strip()` edilir, boşlar atılır; metin sonucu düz okumayla aynı. Kalın yazı tipi: `getFontWeight() >= 600` ya da `isForceBold()` ya da `+` önekinden sonraki ad (harf farksız) `bold/semibold/demi/black/heavy` içerir; sonuç yazı tipi başına `IdentityHashMap`'te tutulur. Satır kalın = boşluk olmayan bütün karakterleri kalın (en az bir tane) |
| Kararlar #3 (soru-cevap kısmı) | `PdfDocumentParser.SectionCollector` | `currentIsFaq`: `startFaq()` → true; `startHeading` → `isFaqTitle(title)` (`FAQ_TITLES`, `equalsIgnoreCase`). `addLine(pageNumber, ExtractedLine)` → `isQuestion = currentIsFaq && isBold`. Başlık adlandırması ve "Sık sorulanlar" satır kuralı değişmedi |
| `PageMarginFilter` generic overload | `parsing/PageMarginFilter.java` | `<T> removeRepeatedMargins(List<List<T>>, Function<T,String>)`; eski `List<List<String>>` metodu `Function.identity()` ile devreder. Algoritma ve `marginKey` aynı |
| Kararlar #4 (soru-cevap bloğu) | `chunking/TextChunker.java` | Kısımda `isQuestion` satırı varsa `questionAnswerBlocks`: ilk sorudan önceki satırlar `sizedBlocks` ile; sonra her çift = art arda soru satırları + sonraki soruya kadarki satırlar; ≤ 2.000 ise tek blok |
| Kararlar #5 (uzun çift) | `TextChunker.pairBlocks` | `prefix = L(Q) + 1`. `prefix ≥ target` ise çift bugünkü uzun blok kuralıyla (önek yok). Değilse cevap satırları `splitLongLines(…, max − prefix)` + `splitLongBlock(…, target − prefix)`; her parça = Q + parça. `splitLongLines`/`splitLongBlock` sınırı parametre aldı (diğer kısımlarda yine `max`/`target`); `splitLongLines` `isQuestion`'ı korur |
| Kararlar #6 (örtüşme) | `TextChunker.ChunkAssembler.startSection(boolean)` | Soru satırı taşıyan kısımda örtüşme boş; diğer kısımlarda bugünkü gibi |
| Kararlar #7 / K2 (sayfa) | `TextChunker.ChunkAssembler.emit` | Sayfa = `currentLines.getFirst().pageNumber()` (örtüşme kırpıldıysa kalan ilk satır). `newLineCount` sadece "yeni satır var mı" için kaldı |
| Kararlar #8 (soru sayısı) | `parsing/ParsedDocument.java`, `service/IngestionJobRunner.java` | `questionCount()`: kısım içinde önceki satırı soru olmayan soru satırları. Log: `Document parsed jobId={} pageCount={} chapterCount={} sectionCount={} questionCount={} removedMarginLines={}` (metin loglanmaz) |
| Kararlar #9 (sabitler kodda) | `PdfDocumentParser` | `FAQ_TITLES`, `MIN_BOLD_FONT_WEIGHT = 600`, `BOLD_FONT_NAME_MARKERS`; yeni property yok |
| Kararlar #10 / K4 (yenileme yolu) | `retrieval-api/CLAUDE.md` | "Teknoloji" PDF okuma paragrafı (kalın satır, soru-cevap kısmı), yeni "Parçalama" maddesi, "Komutlar"da yenileme yolu (`down -v` + `up -d` + `curl -F`; TASK-015'ten sonra yeni sürüm) |
| M0 / AC1 | dev ortamı | Aşağıda "Doğrulama" |

## Değişen dosyalar
- Yeni: `retrieval-api/src/main/java/com/rasitbank/retrieval/ingestion/parsing/ExtractedLine.java`
- Değişen: `ingestion/parsing/PageLine.java`, `ingestion/parsing/PageMarginFilter.java`, `ingestion/parsing/ParsedDocument.java`, `ingestion/parsing/PdfDocumentParser.java`, `ingestion/chunking/TextChunker.java`, `ingestion/service/IngestionJobRunner.java`, `retrieval-api/CLAUDE.md`
- Silinen: yok. Dependency / migration / property: yok.

## Plandan sapmalar
Yok. Tek açıklama: Kararlar #5 "cevap satırları önce `splitLongLines`" diyor; sınırı `max − L(Q) − 1` olarak verdim (plandaki gibi `max` olsaydı 2.000'e yakın tek bir cevap satırı + soru 2.000'i aşardı; G2 "her parça ≤ 2.000" bunu istiyor). Rehberde bu yol hiç çalışmıyor (en uzun çift < 2.000).

## Doğrulama
**`cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify -Djacoco.haltOnFailure=false`** → Tests run: 185, Failures: 2, Errors: 0, Skipped: 0 (BUILD FAILURE). Kırılan iki test de planın beklentisini değiştirdiği testler; tester günceller:
- `TextChunkerTest.givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstNewLine` → beklenen 8, gelen **7** (T1: yeni ad `…thenPageNumberIsPageOfFirstLineIncludingOverlap`, beklenti 7).
- `PdfDocumentParserGuideTest.givenGuide202610_whenParseAndChunk_then154ChunksWithAverage285Tokens` → beklenen 285, gelen **283** (T14 bunun yerine geçer; T18'in literal'leri aşağıda).
Diğer 183 test (`PdfDocumentParserTest` 14, `PageMarginFilterTest` 7, `TextChunkerTest`'in diğer 18'i, `SourceUploadIntegrationTest` 10 dahil) geçti. Coverage eşiği kapalıydı; eşiği tester ve qa tam `./mvnw verify` ile doğrular.

**M0 (gerçek PDF + gerçek `bge-m3`, belge ekleme ucu):** Docker'da tam sistem çalışmıyordu; sadece dev ortamı kullanıldı (`rag-dev` projesi, ayrı volume). `docker compose -f deployment/docker-compose.dev.yml down -v` + `up -d --wait`, `DATABASE_PASSWORD=retrieval-local ./mvnw spring-boot:run`, Ollama'da `bge-m3:latest`. Bilgi bankası boş (`content: []`). `curl -F file=@../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf -F name='Raşit Bankası Ürün ve Hizmet Rehberi' -F version=2026.10 localhost:8080/api/workspaces/42/sources` → 202, `Location: /api/workspaces/42/ingestion-jobs/1`, kaynak 10001; iş **DONE**, `embeddedChunks = totalChunks = 154`, iş süresi 9.005 ms (yükleme + polling 11 sn; embedding 2 parti: 6.195 + 2.569 ms).

| Ölçü | TASK-024 (M0) | TASK-005 | Fark |
|---|---|---|---|
| Parça sayısı | **154** | 154 | 0 |
| `round(avg(token_count))` | **283** (283,38) | 285 (285,42) | −2 |
| Soru-cevap kısımlarındaki parça (`heading like '% · Sık sorulanlar'`) | 16, ort. 308,9 token | 16, ort. 328,5 | 0 parça, −19,6 token (örtüşme kalktı) |
| Diğer kısımlardaki parça | 138, ort. 280,4 token | 138, ort. 280,4 | içerik 138/138 aynı |
| `questionCount` (log) | 105 | — | `.typ`'teki 105 çiftle aynı; rehberde iki satıra kayan soru yok |
| Sayfa aralığı | 5-70, en uzun parça 1.599 karakter | — | — |

- Log: `Document parsed jobId=1 pageCount=70 chapterCount=11 sectionCount=109 questionCount=105 removedMarginLines=138`, `Document chunked jobId=1 chunkCount=154 avgTokens=283`. Logda soru/cevap/parça metni yok (`grep` "Döviz|limit emri|komisyon|aidat" → 0).
- TASK-005'e göre değişen parçalar (eski `TextChunker`'ı scratchpad'de aynı PDF'e çalıştırıp karşılaştırdım): içeriği değişenler sadece soru-cevap kısımları (34, 35, 36, 55, 56, 75, 76, 93, 109, 110, 121, 122). Diğer kısımlarda sadece 2 parçanın sayfası değişti: **13** (`2.7 Hesap kesim tarihi…`, s. 11 → **10**) ve **113** (`7.2 Yatırım fonları`, s. 52 → **51**); ikisi de ilk satırı (örtüşme) önceki sayfada olan parçalar.
- S1: `content like '%Döviz alırken komisyon%'` → chunk 122, s. 56, `7 Yatırım · Sık sorulanlar`, ilk satırı `Döviz alırken komisyon alıyor musunuz?`. "Dolar belli bir kura…" çifti soru + cevap bütün olarak chunk 121'de (s. 55). "limit emri verebilirsiniz" geçen ama "Dolar belli bir kura" geçmeyen tek parça 118 (`7.7 Emir türleri ve otomatik alım`; SSS değil, bölümün kendi metni).
- S2 ön kontrolü (scratchpad): 154 parçanın her birinin ilk satırı (boşluklar indirilmiş) `pageNumber` sayfasının `PDFTextStripper` metninde geçiyor: 0 aykırı; ilk parçanın sayfası 5.
- Veriler qa (M1) için dev veritabanında bırakıldı: `rag-dev-postgres-1` açık, `retrieval` veritabanında kaynak 10001 + 154 parça. retrieval-api durduruldu (port 8080 boş). qa için sorgu kalıbı: `docker compose -f deployment/docker-compose.dev.yml exec -T postgres psql -U postgres -d retrieval -c "…"`.

## Tester için notlar
- **T18 / T19 literal'leri (M0):** 154 parça, `round(avg(tokenCount))` = 283. `SourceUploadIntegrationTest` 154 beklentisiyle zaten geçiyor (sayı değişmedi); plan "M0 sayısı olur" diyor, değer aynı kaldı.
- **Kalın satır seam'i:** `PdfDocumentParser.isBoldFont(PDFont)` (private static) yazı tipi ağırlığı, `forceBold` ya da adla karar verir. Fixture'da `SourceSans3-Semibold.ttf` gömülünce PDFBox subset adı `ABCDEF+SourceSans3-Semibold` olur → ad işaretiyle kalın. Standart 14 yazı tipiyle (`Helvetica-Bold`) de ad işareti tutar. T11 (kısmen kalın satır): stil kelime düzeyinde değil `writeString` çağrısı düzeyinde işaretlenir; PDFBox bunu kelime başına çağırır, yani `Kur alarmı:` (kalın) + ` Piyasalar…` (düz) aynı satırda → `isBold = false`. Bir kelimenin içinde yazı tipi değişirse de o kelime düz sayılır (bütün pozisyonlar kalın olmalı).
- **Soru-cevap kısmı:** sadece `startFaq()` (satır kuralı ya da alt yer imi `Sık sorulanlar`) ve `FAQ_TITLES` başlıklı yer imi. Bölüm düzeyindeki (üst) bir yer iminin başlığı `SSS` ise o bölüm de soru-cevap kısmıdır. Başlık eşlemesi `equalsIgnoreCase` (T13: `SIKÇA SORULAN SORULAR` eşleşir).
- **`questionCount()`:** saf fonksiyon; `ParsedDocument` elle kurulup test edilebilir (T20). Kısım sınırında sayaç sıfırlanır (iki kısmın sınırındaki soru satırları ayrı sayılır).
- **Parçalayıcı:** `TextChunker` saf; `IngestionProperties(1600, 2000, 200, 4, 96, 3, 0.5, 2)` ile kurulur, satırlar `new PageLine(page, text, true/false)`. Uzun çift yolunda devam parçalarının her biri soru satırlarını tekrar taşır; cevap satırları bir kez geçer. `prefix ≥ target` (soru ≥ 1.599 karakter) yolu ayrı bir dal: önek yok, bugünkü uzun blok kuralı.
- **Soru satırı yoksa** (`isQuestion` hiç yok) parçalama bit bit TASK-005'teki gibi (örtüşme dahil); sadece sayfa kuralı değişti.
- Dış model yok: bu task'taki değişiklikler embedding'e dokunmuyor; integration testlerde sahte `EmbeddingClient` yeterli. Zaman (`Clock`) ve eşzamanlılık noktası yok.

STATUS: DONE
