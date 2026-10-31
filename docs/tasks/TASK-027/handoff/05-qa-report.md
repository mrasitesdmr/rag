TASK: TASK-027
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Değişiklik sadece retrieval-api ve Docker/doküman olduğu için web ve SSE skill'leri uygulanmadı. Java, Spring ve Flyway kontrolleri bu skill'in beş ekseniyle ve plandaki ek skill listesiyle yapıldı.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|------------------------|--------------------|-------|
| K1 | Otomatik yükleme yok | `IngestionStartupListener` sadece `failInterruptedJobs()` çağırır. `GuideBootstrap`, `GuideBootstrapResult`, `PathConversionConfig` silinmiş. İz araması aşağıda. `StartupIntegrationTest` ve `IngestionStartupListenerTest` `verify`'da geçti. Canlıda 4 kaynak devops denemelerinden kalma; açılışta yeni iş oluşmuyor (devops M2). | ✅ |
| K2 | Belge ekleme ucu: 202 + `Location` + iş | Canlı: Belge B `POST :3000/api/workspaces/42/sources` → 202, `Location: /api/workspaces/42/ingestion-jobs/6`, iş 3,7 sn'de `DONE`. `SourceController.createSource`; `SourceControllerTest`, `SourceUploadIntegrationTest`. | ✅ |
| K3 | Doğrulama: boş dosya, ≤ 20 MB, ad 2-80 ve harf farksız tekil, sürüm zorunlu 1-40 | Canlı: sürümsüz → 400 `VALIDATION_FAILED` (`version`). Boş dosya → 400 (`file`). 20 MiB+1 bayt, nginx (3000) üzerinden → 413 `FILE_TOO_LARGE` (`application/problem+json`). `"qa belge b "` → 409 `SOURCE_NAME_TAKEN`. Ad verilmeden yükleme → dosya adından ad. Kod: `SourceUploadValidator`, V5 `lower(name)` unique index'i. Testler: `SourceUploadValidatorTest`, `SourceNameIndexIntegrationTest`, `SourceUploadHttpIntegrationTest`. | ✅ |
| K4 | Sadece PDF | Canlı: `a.txt` → 415 `UNSUPPORTED_FILE_TYPE`. Uzantı `.pdf` ve ilk 1024 baytta `%PDF-` aranır. | ✅ |
| K5a | Başlıklar yer imlerinden, yer imsiz PDF reddedilir | Canlı: Belge C (`rag.html`, yer imsiz Chrome PDF'i) → 422 `PDF_NO_BOOKMARKS`; listede yeni kayıt yok. Belge B (yer imli) → `DONE`. `PdfDocumentParserTest`, `PdfDocumentParserGuideTest`. | ✅ |
| K5b | Rehbere özgü kalıp ve "5. sayfa" kuralı kalktı; üst/alt bilgi genel yöntemle ayıklanır | `ingestion.page-*-pattern` kalktı (iz aramasında yok). `PageMarginFilter` ve `PageMarginFilterTest`. Canlı Belge B: parçalarda `file:///` 0, `dd.MM.yyyy HH:mm` 0 (63 parça). | ✅ |
| K5c | Dosya hataları yüklemede reddedilir, kaynak ve iş oluşmaz | Canlı: bozuk PDF (`%PDF-` + çöp) → 422 `PDF_UNREADABLE`. Yer imsiz → 422. Toplam kaynak 5'te kaldı. Testler: T23, T23b, T23c. **Taranmış (görüntü) PDF'i gerçek dosyayla denemedim**, sadece testteki sentetik fixture ile (`PDF_NO_TEXT`). | ✅ (not) |
| K5d | Rehber kalitesi korunur | Rehber 2026.10: 70 sayfa, 11 bölüm, 154 parça (TASK-005 ile aynı). Elle karşılaştırma aşağıda. | ✅ |
| K6 | Yeniden dene ucu | Canlı: tamamlanmış kaynak → 409 `SOURCE_ALREADY_INGESTED`; bilinmeyen kaynak → 404 `SOURCE_NOT_FOUND`. Başarısız işin yeniden denenmesi (`RetryIngestionIntegrationTest`: T52, T53) `verify`'da geçti. Canlıda Ollama'yı kapatıp denemedim; devops M3 raporunda (kaynak 10004: iş 4 `FAILED`, iş 5 `DONE`, kaynak tek). Veritabanında bu görünüyor. | ✅ |
| K7 | Çalışma alanı migration'la | `V2` değişmedi; `GET /api/workspaces/43/sources` → 404 `WORKSPACE_NOT_FOUND`, 42 var. | ✅ |
| K8 | Eski veri temizliği dokümanda, kodda sabit 10001 yok | `deployment/CLAUDE.md:56` `down -v` komutlarını yazıyor. Kodda 10001 sadece V3 `START WITH 10001`. | ✅ |
| K9 | Loglama sınırı | `SourceUploadHttpIntegrationTest` ve log testleri (T51) geçti. Log satırları (kod): `Source created sourceId`, `Upload rejected errorCode fileSizeBytes`, `Ingestion job queued jobId sourceId fileSizeBytes`. Ad, dosya adı ve içerik logda yok. | ✅ |
| K10 | Rehber PDF'leri repoda kalır, servis bakmaz | `docs/rehber/pdf/` içinde 2 PDF duruyor; `src/main` ve Compose'da yol yok. | ✅ |
| AC1 | Temiz açılışta boş, iz yok | Aşağıdaki iz araması. Temiz Docker açılışını `down -v` gerektirdiği için kendim tekrarlamadım; `StartupIntegrationTest` boş veritabanında 0 kayıt doğruluyor, devops M1 de bunu raporladı. | ✅ |
| AC2 | Gerçek `bge-m3` ile iki belge + yer imsiz ret, sayılar | Aşağıdaki tablo; backend ve devops raporlarıyla tutarlı. | ✅ |
| AC3 | qa rastgele parçaları PDF'le karşılaştırır | 5 + 5 parça, aşağıda. | ✅ |
| AC4 | Testler, `verify` geçer | 185 test, 0 hata. | ✅ |

### Gerçek `bge-m3` sonuçları (canlı veritabanı)
| Belge | Sayfa | Bölüm | Parça | Ort. token | Süre |
|-------|-------|-------|-------|-----------|------|
| Rehber 2026.10 (kaynak 10001) | 70 | 11 | 154 | devops/backend raporunda 285 | 8,41 sn |
| Rehber 2026.11 (devops, 10002-10004) | 70 | 11 | 153 | | 6,97-7,27 sn |
| **Belge B**, yer imli `rag.html` Chrome PDF'i (ben ürettim, kaynak 10005) | 51 | 1 | 63 | 277 | 3,73 sn |

- TASK-005 sonucu: 154 parça, ort. 285 token, 8,96 sn. Rehber 2026.10 için parça sayısı aynı; süre 8,41 sn.
- Belge B'yi `kaynaklar/rag.html`'den yeniden ürettim (`--generate-pdf-document-outline`). Backend-coder'ın M0'ındaki 63 parça ile aynı.
- Belge B'de `chapter_count` 1: sayfadaki yer imleri tek bir üst düzey başlığın altında. Parçalar yine yer imi başlıklarını taşıyor (aşağıda).
- Belge C (yer imsiz): 422 `PDF_NO_BOOKMARKS`, hiçbir kayıt oluşmadı.

### Elle parça karşılaştırması (PDF metni `pdftotext` ile sayfa sayfa çıkarıldı)
Belge B (5 rastgele parça):
| Parça | Başlık | Kayıtlı sayfa | PDF'te |
|-------|--------|---------------|--------|
| 742 | Generation'ı ölçmek: faithfulness ve answer relevance | 30 | Başlık ve metin s.30'da ✅ |
| 761 | Kendini test et | 46 | s.46 ✅ |
| 759 | Aklında kalması gerekenler | 45 | s.45 ✅ |
| 731 | Takip sorularını yeniden yazmak | 22 | Parça, önceki bölümün son cümlesiyle (örtüşme) başlıyor; o cümle s.21'de. Başlık s.22'de. Bkz. FYI-1 |
| 760 | Aklında kalması gerekenler | 45 | s.45 ✅ |

Rehber 2026.10 (5 rastgele parça):
| Parça | Başlık | Kayıtlı sayfa | PDF'te |
|-------|--------|---------------|--------|
| 19 | 2.9 Taksitli alışveriş | 13 | Metin s.13'te ✅ |
| 129 | 8.5 Başka banka ATM'leri | 58 | Başlık ve metin s.58'de ✅ |
| 118 | 7.6 Tahvil, bono ve eurobond | 53 | s.53 ✅ |
| 29 | 2.16 Kayıp ya da çalıntı bildirimi | 16 | s.16 ✅ |
| 42 | 3.3 Kumbara hesabı | 21 | s.21 ✅ |

Üst/alt bilgi ya da tarih satırı örneklerde yok; tablo satırları bölünmemiş.

### İz araması (K1/K10, plan M5)
`grep -rniE 'bootstrap|GuideBootstrap|INGESTION_BOOTSTRAP|/app/rehber|docs/rehber/pdf|page-header-pattern|page-footer-pattern|PDF_NO_SECTIONS|10001' retrieval-api/src/main deployment retrieval-api/Dockerfile web/nginx.conf` sonucu 4 satır:
- `V3...sql:1` `START WITH 10001`: planın istisnası.
- `V3...sql:30` eski `PDF_NO_SECTIONS` CHECK satırı: lead'in istisnası (migration değişmez, V5 kuralı yeniden kurar).
- `deployment/CLAUDE.md:43` ve `:56`: otomatik yüklemenin kalktığını ve temizliği anlatan doküman; kod ya da ayar değil.

README, `ollama.md`, `retrieval-api/src/test` ve `docs/api` için ayrıca aradım (`bootstrap`, `INGESTION_BOOTSTRAP`, `/app/rehber`, `page-header-pattern`, `PDF_NO_SECTIONS`): sonuç yok. Compose'da `/app/rehber` bağlantısı yok.

## Sözleşme uyumu
`docs/api/openapi.yaml` ↔ `SourceController`, DTO'lar, `GlobalExceptionHandler`. Canlıda denendi:
| İstek | Beklenen | Gelen |
|-------|----------|-------|
| `POST .../sources` geçerli | 202 + `Location` + `IngestionJobResponse` | ✅ |
| sürümsüz | 400 `VALIDATION_FAILED`, `violations[].field` = `version` | ✅ |
| boş dosya | 400, `file` | ✅ |
| 20 MiB+1 (3000 üzerinden) | 413 `FILE_TOO_LARGE` | ✅ |
| JSON gövde | 415 `UNSUPPORTED_MEDIA_TYPE` | ✅ |
| `.txt` | 415 `UNSUPPORTED_FILE_TYPE` | ✅ |
| yer imsiz / bozuk | 422 `PDF_NO_BOOKMARKS` / `PDF_UNREADABLE` | ✅ |
| aynı ad (harf ve boşluk farklı) | 409 `SOURCE_NAME_TAKEN` | ✅ |
| çalışma alanı 43 | 404 `WORKSPACE_NOT_FOUND` | ✅ |
| `page=-1` | 400 `VALIDATION_FAILED`, `page` | ✅ |
| retry: bitmiş / bilinmeyen kaynak | 409 `SOURCE_ALREADY_INGESTED` / 404 `SOURCE_NOT_FOUND` | ✅ |

Hata gövdeleri `type`, `title`, `detail`, `instance`, `status`, `errorCode` alanlarını taşıyor. `PDF_NO_SECTIONS` sözleşmede kalmadı (grep). Sözleşmedeki `errorCode` enum'u ile kodun kullandığı kodlar eşleşiyor; `EMBEDDING_MODEL_MISMATCH` ve `INGESTION_IN_PROGRESS` yollarını canlıda denemedim, birim testleri geçti (T17, T29, T55, T56). assistant-api ve web bu uçları kullanmıyor (etkilenmedi).

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `cd retrieval-api && ./mvnw verify` | BUILD SUCCESS, 185 test, 0 hata/atlanan. JaCoCo eşiği geçti; satır coverage %92,9 (861/927) | Evet (03 raporu: 185 test, %92,9) |
| `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS (aşağıda) | Kısmen: tester dört sınıfla sınırlı koştu; ben tüm paketi çalıştırdım |
| Canlı sistem (`curl` senaryoları) | Yukarıdaki tablolar | Evet |
| assistant-api, web | Uygulanamaz: bu task'ta değişmedi | — |

## Mutation testi
PIT 1.30.1, bütün `com.rasitbank.retrieval.*`: 456 mutant, 371 öldü (%81), test gücü %89, 41 mutant kapsanmıyor, kapsanan satır %93. Projede eşik tanımlı değil.
Yeni ve değişen sınıflar: `PageMarginFilter` 18/19, `PageMarginFilter$PageBounds` 10/10, `SourceUploadValidator` 34/37, `PdfDocumentParser` 52/60, `IngestionService` 21/23, `SourceController` 4/4, `GlobalExceptionHandler` 38/48. Hayatta kalanlar çoğunlukla eşdeğer ya da sınır (PDFBox ayarları, `closeQuietly`, başlık kesme sınırı); tester raporundaki liste aynı. Düşük skor `SourceVersion` (2/12) ve `Workspace` (1/6) getter'larında; bunlar bu task'ın kodu değil (bkz. Optional-1).

## Uçtan uca
Playwright bu task'ta yok (ekran yok). Yerine canlı sistemde (3000/nginx ve 8080) yukarıdaki yükleme, ret, çakışma, 413 ve yeniden dene senaryolarını çalıştırdım. Dil modeli çağrısı bu task'ta yok. Taranmış PDF'i gerçek dosyayla ve "Ollama kapalı → yeniden dene" akışını canlıda ben tekrarlamadım (ikincisi devops M3'te; sebep: Ollama'yı durdurmak ve veritabanını temizlemek lead/kullanıcı onayı gerektirir).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional | `source/service/SourceUploadValidator.java` `validate` | PDF yüklemede iki kez okunuyor (istekte ret için, iş içinde PARSING'de); büyük belgede 202 gecikir. Plan Kararlar #10 ve kartta bilinen risk olarak yazılı. Okunmuş belgeyi taşımak ya da PARSING'de yeniden kullanmak izleyen bir task olabilir. | backend-coder |
| Optional | `IngestionService.createSource` | Aynı anda iki yükleme aynı PDF'i iki kez okuyup bellekte tutuyor (en çok 20 MB × istek sayısı). Tek kullanıcılı lab için sorun değil. | backend-coder |
| Optional | `SourceVersion`, `Workspace` | PIT'te getter mutant'ları kalıyor; davranışı test eden testle kapatılabilir. | tester |
| FYI-1 | parça 731 (Belge B) | Parçanın başı önceki bölümün son cümlesiyle (örtüşme) başlıyor; sayfa etiketi başlığın sayfası. TASK-024 kapsamı (parça örtüşmesi ve sayfa etiketi), bu task'ta değil. | — |
| FYI-2 | `lower()` | Türkçe `İ`/`I` için veritabanı kuralı kullanılıyor ("İstanbul" ile "istanbul" farklı sayılabilir). Plan riski #6, kullanıcı biliyor. | — |
| FYI-3 | `docs/rehber/pdf/…2026.11.pdf` | devops'un ikinci belgesi rehberin biçiminde olduğu için kabul kriterini tek başına karşılamıyordu; Belge B ile karşıladım. | — |
| FYI-4 | veritabanı | Canlıda devops denemelerinden kalan kaynaklar 10001-10005 var (benim eklediğim 10005 `QA Belge B` dahil). Temizlik `down -v` ile ve kullanıcı onayıyla. | lead |

`Critical` ve `Required` bulgu yok. Kod incelemesinde V5 migration (index + CHECK), validator sırası (400 → 415 → 422), 404/409 sırası, transaction sonrası `submit`, yarış durumunda unique index'in 409'a çevrilmesi ve `PageMarginFilter` algoritması plana uygun bulundu; log satırlarında içerik, ad ve parça metni yok.

## Düzeltme
Gerekmiyor.

STATUS: DONE
