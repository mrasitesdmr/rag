TASK: TASK-027
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `spring-mvc-testing`, `rag-pipeline` (indexing).

## Senaryo → test eşlemesi
Önceki oturumun yazdığı testler gözden geçirildi, baştan yazılmadı. T1…T58 (T41b, T23b, T23c dahil) plandaki katmanda ve adla mevcut:

| Senaryo | Dosya |
|---|---|
| T1-T10 | `source/service/SourceUploadValidatorTest` |
| T11-T19, T57, T58 | `source/controller/SourceControllerTest`, `SourceControllerValidationTest` |
| T20 | `ingestion/integration/SourceUploadHttpIntegrationTest` |
| T21-T24, T26, T28, T51 | `ingestion/integration/SourceUploadIntegrationTest` |
| T27, T29, T55, T56 | `ingestion/service/IngestionServiceTest` |
| T30 | `WorkspaceMigrationIntegrationTest`, `RetrievalApplicationIntegrationTest` (V1-V5) |
| T31, T32 | `ingestion/integration/StartupIntegrationTest` |
| T33 | `ingestion/service/IngestionStartupListenerTest` |
| T34 | `ingestion/integration/SourceNameIndexIntegrationTest` |
| T35-T40, T45, T45b, T47 | `ingestion/parsing/PdfDocumentParserTest` |
| T41-T44 | `ingestion/parsing/PageMarginFilterTest` |
| T46, T49, T50 | `ingestion/parsing/PdfDocumentParserGuideTest` |
| T52-T54 | `ingestion/integration/RetryIngestionIntegrationTest` |

Silinenler: `GuideBootstrapIntegrationTest`, `BootstrapPropertiesIntegrationTest`, `bootstrapFor`, `ingestion.bootstrap.enabled` satırı.

**Bu turda eklenenler/düzeltmeler:**
- `PdfFixtures.validPdf()`: PDFBox her kayıtta rastgele belge ID'si yazdığı için `T52` baytları karşılaştırırken kırılıyordu (test hatası). Tek örnek önbelleğe alındı.
- PIT'te hayatta kalan mutantlar için: `givenUnknownWorkspace_whenRetryIngestion_thenThrowsWorkspaceNotFoundAndWritesNothing` (IngestionServiceTest), `givenSignatureEndingExactlyAtByte1024_whenValidate_thenAcceptedAsPdf` (SourceUploadValidatorTest).

## Mutation
PIT 1.30.1, hedef: `PageMarginFilter`, `PdfDocumentParser`, `SourceUploadValidator`, `IngestionService` (rehber testi hariç, çünkü kırık). 139 mutant, 120 öldü (%86), test gücü %91. Hayatta kalanlar: çoğu eşdeğer ya da sınır (`setSortByPosition`, `shortenHeading` sınırı, `closeQuietly`). `IngestionService.retryIngestion` `ensureWorkspaceExists` kaldırma mutantı ve imza 1024 bayt sınırı için test eklendi (sonrası PIT yeniden koşulmadı). Kapatılmayan: `failInterruptedJobs` (integration testleriyle kapsanıyor, bu kısıtlı PIT çalışmasına dahil edilmedi).

## Komutlar ve sonuçları
- `cd retrieval-api && ./mvnw verify` → **Tests run: 185, Failures: 1**, BUILD FAILURE (tek kırık aşağıda).
- `./mvnw verify -Dmaven.test.failure.ignore=true` → JaCoCo "All coverage checks have been met"; talimat %93,6, dal %86,7, satır %92,9 (eşik %80).
- PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` (kısıtlı sınıf listesiyle).

## Çalıştırılamayanlar
Yok. Docker açık, Testcontainers testleri koştu. Gerçek Ollama gerektiren bir test yok.

## Düzeltme
**Sorumlu rol: backend-coder** (kapalı; lead'e iletildi).
- Kırılan test: `PdfDocumentParserGuideTest.givenGuide202610_whenParse_thenSeventyPagesElevenChaptersAndEveryTocSubsectionIsASection` (T49; T39 ile aynı kural).
- Beklenen (plan Kararlar #9 kural 6, TASK-005 sonucu): 8 başlık `<bölüm> · Sık sorulanlar` (ör. `2 Kartlar · Sık sorulanlar`), yinelenen başlık yok.
- Gerçekleşen: 0 başlık böyle. Rehber PDF'inde "Sık sorulanlar" 8 bölümün altında **yer imi** olarak da var (pypdf ile doğrulandı). `PdfDocumentParser.splitIntoSections` satırı önce `takeHeading` ile yer imi sayıyor, `FAQ_HEADING` dalına hiç düşmüyor. Sonuç: 8 kısmın başlığı yalın `Sık sorulanlar`, birbirinin aynısı ve hangi bölüme ait olduğu kaybolmuş. Parça sayısı (154) ve ort. token (285) aynı olduğu için M0 bunu yakalamadı; parça başlıkları TASK-005'e göre kötüleşti.
- Öneri: yer imi başlığı `Sık sorulanlar` ise ve üstte bölüm varsa başlık `<bölüm> · Sık sorulanlar` olsun (rehberin yer imi ağacı derinlik 1 çocuk). Testi zayıflatmadım.


## Düzeltme turu 1

| Madde | Test | Sonuç |
|---|---|---|
| SSS yer imi başlığı `<bölüm> · Sık sorulanlar` olmalı (backend `SectionCollector.startHeading`) | `PdfDocumentParserGuideTest.givenGuide202610_whenParse_thenSeventyPagesElevenChaptersAndEveryTocSubsectionIsASection` (T49), testte değişiklik yok | geçti |

- `cd retrieval-api && ./mvnw verify` (eşik açık) → **Tests run: 185, Failures: 0, Errors: 0**, BUILD SUCCESS, JaCoCo eşiği geçti.
- PIT (aynı dört sınıf, bu sefer integration testleri dahil, rehber testi dahil): 139 mutant, 125 öldü (%90), kapsanan satır %96. `failInterruptedJobs` için hayatta kalan ya da kapsanmayan mutant yok (önceki NO_COVERAGE, integration testleri dahil edilmediği içindi). Kalan 14 mutant: çoğu eşdeğer/sınır (`setSortByPosition`, `setLineSeparator`, `closeQuietly`, başlık kesme sınırı).

STATUS: DONE
