TASK: TASK-024
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`, `java-code-review`, `springboot-verification`, `rag-pipeline` (+ indexing). Web, SSE ve model yok; `e2e-testing` uygulanamaz.

## Kural kontrolü
Kart cevapları: 1) sayfa = parçanın ilk satırının sayfası; 2) SSS'de örtüşme yok; 3) soru = tamamen kalın satır, art arda kalın satırlar tek soru; 4) başlıklar sabit liste, harf farksız.

| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| K1 | Örtüşme soru-cevap çiftini bölmez (SSS'de örtüşme yok, çift bütün) | `TextChunker.questionAnswerBlocks/pairBlocks`, `startSection(!hasQuestions)`. Testler `TextChunkerTest` T3-T7. Canlı (aşağıda): 105 çiftin 105'i soru + cevap başı bitişik, tek parçada | ✅ |
| K2 | Sayfa = ilk satırın sayfası | `ChunkAssembler.emit` → `currentLines.getFirst().pageNumber()`. Testler T1, T2, T8, T17. Canlı: 154 parçanın 154'ünün ilk satırı, `pdftotext` ile bağımsız okunan gösterilen sayfada (0 aykırı) | ✅ |
| K3 | Diğer parçalama kuralları değişmez | Diff: bölüm sınırı, tablo satırı, `PageMarginFilter` algoritması aynı. Mevcut testler geçiyor (211). Canlı: SSS dışı 138 parçanın içeriği TASK-005 ile aynı (backend raporu; kendim DB'de SSS dışı parçaları taramadım, ama sayfa/başlık kontrolü 154/154 temiz) | ✅ |
| K3b | Kural her belge için, soru-cevap yapısı tanınınca | `PdfDocumentParser.SectionCollector.currentIsFaq`, `FAQ_TITLES`; T9-T13. Kalın satır yoksa eski davranış (T12) | ✅ |
| K4 | Yeniden yükleme belge ekleme ucuyla; açılışta yükleme yok; yenileme yolu kartta; elle parça yok | `retrieval-api/CLAUDE.md` Komutlar (`down -v` + `up -d` + `curl -F`). Dev DB'de kaynak 10001 + 154 parça `POST .../sources` ile yazılmış (02 raporu M0, iş DONE; `embeddedChunks=totalChunks=154`) | ✅ |
| AC-a | ≥ 5 örtüşmeli parça + chunk 122 PDF ile elle karşılaştırıldı | Aşağıda "Elle karşılaştırma" | ✅ |
| AC-b | 105 sorunun her biri tek parçada ve cevabıyla | Aşağıda | ✅ |
| AC-c | Parça sayısı / ortalama token farkı raporda doğru | DB: 154 parça, `avg(token_count)` = 283,38 (yuvarlak 283). TASK-005 QA raporu: 154, 285,4. Fark −2 (−2,0). 02 raporundaki tablo doğru. SSS parçaları 16, değişmedi | ✅ |
| AC-d | Testler geçer, `./mvnw verify` | Aşağıda | ✅ |

## Elle karşılaştırma (gerçek 2026.10 PDF, `pdftotext` ile sayfa sayfa)
**Chunk 122 karşılığı:** "Döviz alırken komisyon alıyor musunuz?" sorusu DB'de id 123 (chunk_no 122), `7 Yatırım · Sık sorulanlar`, s. 56. Parça sorusuyla başlıyor (ilk satır soru, ardından "Hayır. Döviz alım satımında komisyon…"). `p56.txt` sayfa başında bu soru var. Eski sorunlu cevap ("Raşit Mobil'den limit emri verebilirsiniz…" / "Dolar belli bir kura gelince…") soruyla birlikte id 122 (s. 55) içinde bütün. "limit emri verebilirsiniz" cevap metni sadece s. 54-55'te, id 123'te yok.

**Önceki parçanın son satırlarıyla başlayan parçalar:** DB'de ilk satırı önceki parçanın son 3 satırı arasında olan 37 parça var (SSS dışı bölümler; SSS'de örtüşme yok). Hepsinin ilk satırının ilk 45 karakteri `pdftotext` ile tek bir sayfada bulundu ve gösterilen sayfayla aynıydı (belirsiz tekrarlar 63, 65, 152 hariç: o satırlar birçok sayfada geçen kısa tablo/liste satırları, sayfa gösterilen sayfaları da içeriyor). Önceki parçanın sayfasından farklı olanlardan örnekler, hepsi gösterilen sayfada:

| Parça id | Başlık | İlk satır (başı) | Gösterilen s. | PDF'te bulunan s. | Önceki parçanın s. |
|---|---|---|---|---|---|
| 17 | 2.8 Asgari ödeme… | (örtüşme satırı) | 12 | 12 | 11 |
| 19 | 2.9 Taksitli alışveriş | "tamamı hemen limitinizden düşer…" | 13 | 13 | 12 |
| 22 | 2.11 RaşitPuan | "kampanyalara bu ekrandaki "Katıl" düğmesiyle…" | 14 | 14 | 13 |
| 32 | 2.17 Harcama itirazı | "• İtiraz lehinize sonuçlanırsa tutar…" | 17 | 17 | 16 |
| 41 | 3.2 Vadeli mevduat | "denk gelirse hesap izleyen ilk iş günü…" | 21 | 21 | 20 |
| 60 | 4.2 Başvuru belgeleri… | "Başvurunun reddi ve yeniden başvuru" | 30 | 30 | 29 |
| 83 | 5.4 SWIFT | "ücreti, sonuç olumsuz olsa da iade edilmez." | 40 | 40 | 39 |
| 107 | 6.9 Dolandırıcılık uyarıları | "İkinci el satışlarda paranın hesabınıza…" | 49 | 49 | 48 |
| 132 | 8.7 Müşteri hizmetleri… | "Müşteri Hizmetleri görüşmeleri hizmet kalitesi…" | 60 | 60 | 59 |

(Eski kuralda 13 ve 113 numaralı parçaların sayfası da sayfa ilk satırına çekilmiş: 02 raporuyla tutarlı.)

**105 soru:** `docs/rehber/kaynak/bolumler/*.typ` içinde 105 `#soru[…][…]` çifti. DB parçalarında (boşluklar normalize) her sorunun "soru + cevabın ilk 25 karakteri" bitişik dizisi **tam 1 parçada** geçiyor: 105/105, 0 eksik, 0 çoklu. SSS parçalarının 16'sının tamamı bir soruyla başlıyor.

## Sözleşme uyumu
`docs/api/` değişmedi; migration, DTO, endpoint, property yok. Sayfa tek alan olarak aynı kaldı. assistant-api ve web etkilenmiyor. ✅

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` | Tests run: 211, Failures 0, Errors 0, Skipped 0; "All coverage checks have been met"; BUILD SUCCESS | Evet, 03 raporu 210 diyor, bir test sonradan eklenmiş (fark tek test, sonuç aynı) |
| `npm run build/lint/test`, `test:e2e` | Uygulanamaz (web değişmedi) | — |
| DB sorguları (dev postgres): `count/avg`, SSS başlıkları, tüm parçalar | 154 parça, 283,38 ort. token | Evet |

## Mutation testi
PIT 1.30.1, hedef `ingestion.chunking.*` + `ingestion.parsing.*`: 256 mutant, 219 öldürüldü (%86), test gücü %89, satır coverage %97 (387/400). Eşik task'ta tanımlı değil. Hayatta kalanlar (önemli olanlar):
- `TextChunker:81` `maxChunkChars - prefixLength` → `+`: backend-coder'ın plana eklediği "uzun çiftte cevap kesme sınırı soru uzunluğu kadar düşürüldü" kuralının sınırı. Hiçbir test bu sınırı kesin doğrulamıyor (cevap tek satırı 2.000'e yakınken soru + parça ≤ 2.000'i). Kuralın kendisi kartla uyumlu (aşağıda); testi eksik. → Optional-1.
- `TextChunker:72` `prefixLength >= targetChunkChars` sınır mutantı (tester bunu `…ExactlyTargetMinusOneChars…` ile kapattığını yazmış; `>=` ↔ `>` kalıyor), `:56`, `:92`, `:107`, `:149`, `:193`, `:202`, `:212`: sınır/sayaç mutantları, kural taşımıyor.
- `PdfDocumentParser:355` `hasRegular |=` → `&=`: kopya üzerinde mutantı uygulayıp `PdfDocumentParserTest` çalıştırdım, 22/22 geçti. Sebep: PDFBox satırı tek `writeString` çağrısıyla verdiği için kısmen kalın satır `isBold(textPositions)` ile zaten düz işaretleniyor; `hasRegular` pratikte gereksiz (02 raporu "PDFBox kelime başına çağırır" diyor; gözlemim tam tersi). Davranış doğru (T11 geçiyor, kısmen kalın satır soru değil). → Optional-2.
- `isBoldFont` 600 eşiği/ad işareti sınırları, `PDDocument.close`, `splitLines` iç indeksleri: kenar durum.

## Uçtan uca
Uygulanamaz: frontend ve model çağrısı yok; Docker'daki sisteme dokunulmadı. Gerçek PDF + gerçek `bge-m3` ile belge ekleme ucundan yükleme backend-coder tarafından dev ortamında yapılmış (iş DONE, 154/154 embedded); ben DB'deki sonucu doğruladım, yüklemeyi yeniden yapmadım.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Optional | `TextChunker.java:81` (+ `pairBlocks`) | Plana ek: uzun çiftte cevap satırı sınırı `max − (L(Q)+1)`. Kart kararı (çift > 2.000 bölünür, devam parçası soruyla başlar, 2.000 korunur) ile uyumlu; plan metni `max` diyordu, ama o halde parça 2.000'i aşabilirdi. Sapma raporda açıkça yazılı. Bu sınırı kesin yoklayan test yok (mutant yaşıyor) | tester |
| Optional | `PdfDocumentParser.java:348-356` | `hasRegular` bayrağı gereksiz, `writeString` düzeyinde karar zaten veriliyor; 02 raporundaki "kelime başına" açıklaması gerçekle çelişiyor | backend-coder |
| FYI | `TextChunker.questionAnswerBlocks` | Soru satırı olan kısımlarda ilk soruya kadar giriş satırları `sizedBlocks` ile; test T7 var | — |
| FYI | log | `Document parsed … questionCount=…`: sadece sayı, metin yok; CLAUDE.md log kuralına uygun | — |

Dead code, Lombok, `System.out`, yeni dependency, loglanan içerik: yok.

## Tester raporundaki zayıflıklar: yeterli mi? (lead sorusu)
1. **154 / 283 literal'i M0'dan alındı.** Test bağımsız oracle değil, regresyon kilidi. Kabul için yeterli: sayıyı ben ayrıca doğruladım (DB: 154 parça, 283,38 ort. token; TASK-005 QA raporu: 154 / 285,4). Doğruluk kanıtı T14 (105 soru `.typ` oracle'ı), T17 (sayfa, `PDFTextStripper`) ve benim `pdftotext` kontrolüm.
2. **T15/T16 oracle'ı "soru + cevabın ilk 25 karakteri".** Rehberde cevap başları tekrar ettiği için kabul edilebilir. Eksik kalan "cevabın sonu da aynı parçada mı" kısmını ben ayrıca kontrol ettim: her soru için cevabın işaretsiz düz metninin son 25 karakteri, sorunun parçasında soruyu izleyen kısımda geçiyor: **105/105** (tek seferlik doğrulamam, testte yok). Kabul kriteri (b) için yeterli; kalıcı test zorunlu değil.
3. **PIT "son testlerden önceki koşu".** Tester sonradan bir test ekledi (211 test). Benim PIT koşum güncel testlerle yapıldı: 219/256 (%86), test gücü %89. Yeni test (`givenAnswerLineOf1990CharsAndQuestionOf100…`, soru + cevap satırı 2.091) Optional-1'deki sınırı davranış olarak örnekliyor; `TextChunker:81` mutantı yine de hayatta kaldığı için sınır kesin doğrulanmış değil. Optional-1 geçerli.

## Düzeltme
Yok. Optional bulgular işi durdurmaz (kullanıcı kuralı: Optional bulgular ayrı task'a bölünür).

STATUS: DONE
