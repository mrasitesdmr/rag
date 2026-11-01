TASK: TASK-007
# Plan

## Özet
**Mevcut durum (TASK-027 + TASK-006):** retrieval-api'de belge ekleme (`POST .../sources`), sade kaynak listesi (`GET .../sources`: `sourceId, workspaceId, name, kind, createdAt`), yeniden dene (`POST .../sources/{sourceId}/ingestion-jobs`), iş durumu (`GET .../ingestion-jobs/{jobId}`) ve arama var. Tek kaynak, PDF, işler listesi, parça ve çalışma alanı okuma uçları yok. web'de sadece kabuk var: `/kaynaklar` başlık ve açıklama gösteriyor; sunucu çağrısı, TanStack Query, MSW, `shared/api/**` yok.

**Yeni durum:**
- **retrieval-api (değişen tek servis):** yeni okuma uçları: `GET /api/workspaces/{workspaceId}` (çalışma alanı + parçalama ve index ayarları), `GET .../sources/{sourceId}`, `GET .../sources/{sourceId}/file` (saklanan PDF), `GET .../sources/{sourceId}/ingestion-jobs` (en yeni üstte), `GET .../sources/{sourceId}/chunks` (sunucuda filtre + sayfa), `GET .../chunks/{chunkId}` (tam metin + embedding'in ilk 8 değeri). `SourceResponse` ekran alanlarıyla genişler (dosya, sürüm, sayılar, son senkron, `latestJob`). Migration yok.
- **web:** `/kaynaklar` (tablo, bilgi kutusu, boş durum, "Belge ekle" penceresi, "Yeniden dene") ve `/kaynaklar/:sourceId` (başlık, "PDF'i aç", meta kutuları, içe alma işleri, parçalar, parça penceresi). Süren iş polling ile izlenir. İlk sunucu çağrısı yapan task: `shared/api/**`, `AppProviders`, TanStack Query, MSW kurulur (kart sorusu 1).
- **assistant-api:** değişmez.

**Değişen mevcut parçalar:**
- `SourceResponse.from(Source)` → `SourceResponse.of(Source, SourceVersion?, Double?, IngestionJob)` (alanlar eklendi; eski alanların anlamı aynı).
- `PageHeader` props'u genişler (`parent`, `actions`, `description: ReactNode`); mevcut 4 sayfa değişmeden çalışır.
- `MainNav`: `/kaynaklar` öğesinden `end` kalkar (detayda menü seçili kalır; `web/CLAUDE.md`'deki not).
- `openapi.yaml` retry ucundaki satır içi `sourceId` parametresi ortak `#/components/parameters/SourceId`'ye taşındı (şekil aynı).

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ikisi de sadece sözleşmeye bakar.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** `retrieval-api/src/main/**`, `retrieval-api/CLAUDE.md`. Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/package.json` + lock (kart sorusu 1 onaylanırsa), `web/CLAUDE.md`. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `retrieval-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**`, `web/e2e/**` (E2E fixture'ı dahil). Rapor `handoff/03-test-report.md`.
- **devops:** Docker'ı yeniden kurar, E2E'yi Docker'daki sitede çalıştırır, PDF'in nginx üzerinden açıldığını ve Ollama kapalı senaryosunu dener; `deployment/CLAUDE.md`'ye kısa not. Dockerfile, Compose ve `nginx.conf` değişmez (PDF ucu mevcut `/api/workspaces/` bloğundan geçer). Rapor `handoff/04-deploy-report.md`.
- **qa:** lab ile yan yana karşılaştırma, kabul kriterleri, sözleşme uyumu. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** yok. assistant-api değişmediği için o servise kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1a | Liste sütunları: Kaynak (ad + dosya), Tür ("PDF · N sayfa"), Sürüm, Parça, Durum, Son senkron | `SourceTable`, `SourceResponse` | F2, B2, B3 |
| K1b | Durum: iş sürüyorsa aşama, değilse "Hazır" | `model/ingestionRules.ts` `sourceStatus`, `SourceTable` | F2, F3, F4 |
| K1c | Bilgi kutusu, değerler backend'den | `WorkspaceCallout`, `GET /api/workspaces/{id}` | F1, B1 |
| K2a | Başlıkta "Belge ekle" | `SourceListPage` (`PageHeader.actions`) | F1, F7 |
| K2b | Pencerede dosya ve ad; ad boşsa dosya adı (uzantısız); tür ve sınırlar TASK-027 | `AddSourceDialog`, `sourceUploadRules.ts`, backend (değişmez) | F8, F9, F13 |
| K2c | "Vazgeç" / "Yükle ve işle"; yüklenince pencere kapanır, yeni satır aşamasıyla görünür | `AddSourceDialog`, `useCreateSourceMutation` | F13, E1 |
| K2d | Sürüm etiketi zorunlu | `AddSourceDialog`, `sourceUploadRules.ts` | F8, F12 |
| K2e | Doğrulama hataları (boş, 20 MB üstü, PDF değil, aynı ad, eksik sürüm) pencerede Türkçe | `AddSourceDialog`, `errorMessages.ts` | F8, F9, F11, F12, E3 |
| K2f | Yer imsiz / metinsiz / açılamayan PDF reddi, düzeltme anlatan mesaj (metinler kartta) | `errorMessages.ts` (`PDF_*`) | F10, F25, E2 |
| K2g | Odak: açılınca ilk alan, Esc kapatır, odak düğmeye döner | `Dialog`, `AddSourceDialog` (`initialFocusRef`) | F7, F14 |
| K2h | Görünüm lab'ın "Yeni sürüm yükle" penceresine benzer | `Dialog` + lab `.dlg/.file/.fld/.drow` stilleri | M3 |
| K3 | Başarısız ve tamamlanmış sürümü olmayan belgede "Yeniden dene"; satır yeniden aşama gösterir | `SourceTable`, `canRetry`, `useRetryIngestionMutation` | F4, F5, M2 |
| K4 | Hiç belge yoksa boş durum + "Belge ekle" | `SourceEmptyState` | F1, E0 |
| K5a | Breadcrumb, başlık; alt başlık: dosya, bayt, sürüm, son senkron | `SourceDetailPage`, `PageHeader.parent` | F16 |
| K5b | "PDF'i aç" saklanan PDF'i yeni sekmede açar | `GET .../file`, `<a target="_blank">` | B8, B9, F16, E4, M1 |
| K5c | Meta kutuları: Sayfa / bölüm, Parça · ort. token, Parça boyutu, Örtüşme, Embedding, Index; backend'den | `SourceMetaGrid`, `SourceResponse`, `WorkspaceResponse` | F17, B1, B2 |
| K6 | İşler tablosu: İş, Dosya, Sürüm, Parça, Süre, Durum; en yeni üstte | `IngestionJobTable`, `GET .../ingestion-jobs` | F18, B10 |
| K7a | Parça tablosu: #, Sayfa · başlık, İlk satır, Token | `ChunkTable` | F19 |
| K7b | "Parçalarda ara (başlık ya da metin)" sunucuda | `ChunkService`, `ChunkRepository.findActiveVersionChunks`, `ChunkTable` | B12, B13, F20, E1 |
| K7c | Sayfa başına 12, Önceki / Sonraki, "N / M", "Eşleşen parça yok." | `Pagination`, `ChunkTable`, `CHUNK_PAGE_SIZE` | F19, F20, B11 |
| K7d | `PageResponse` standardı | `ChunkController`, `SourceController` | B11, B10 |
| K8a | Parçaya tıklayınca pencere; başlık parçanın başlığı | `ChunkDialog` | F21 |
| K8b | Bilgi şeridi: chunk_id, chunk_no, token, model, kaynak (ad ve sürüm) | `ChunkDialog`, `ChunkResponse` | F21, B17 |
| K8c | Parça metni + embedding'in ilk değerleri ("… N değer daha") | `ChunkDialog`, `embeddingPreview` | F21, B17 |
| K8d | "Kapat" ve Esc; odak açan öğeye döner | `Dialog` | F21 |
| K8e | Yeniden kullanılabilir (TASK-008 alıntıdan açacak) | `shared/chunk/ChunkDialog` (`chunkId` prop'u), `GET .../chunks/{chunkId}` | F21 (sayfadan bağımsız render) |
| K9 | Süren iş polling ile izlenir; bitince liste ve detay yenilenir; ekrandan çıkınca durur | `useIngestionJobsPolling` | F3, F24, F28 |
| K10 | `errorCode` → Türkçe; kaynak yoksa Türkçe mesaj | `shared/api/errorMessages.ts`, `SourceDetailPage` | F23, F25, F26 |
| K11 | Parça metni ve PDF içeriği HTML olarak yorumlanmaz | React metin render'ı (`dangerouslySetInnerHTML` yok); PDF `application/pdf` + `nosniff` | F22, B8 |
| S1 | İlk açılış boş durum | K4 | F1, E0, M1 |
| S2 | Rehber 2026.10 yükle → aşamalar → "PDF · 70 sayfa · 2026.10 · Hazır" | K2c, K9 | E1, M1 |
| S3 | Yer imli başka PDF → iki kaynak | K2 | M1 |
| S4 | Yer imsiz PDF → pencerede düzeltme mesajı, kayıt yok | K2f | F10, E2 |
| S5 | Ollama kapalı → "Başarısız" + "Yeniden dene" → "Hazır" | K3 | F4, M2 |
| S6 | Detayda iş `DONE` | K6 | F18, E1 |
| S7 | Aynı ad → Türkçe hata | K2e | F11, E3 |
| S8 | Filtre "asgari ödeme" | K7b | B12, F20, E1 |
| S9 | Parça penceresi, Esc | K8 | F21, E1 |
| AC1 | Lab ile yan yana aynı (Belge ekle, Yeniden dene, boş durum hariç) | stiller lab'dan | M3 |
| AC2 | "Belge ekle" gerçek PDF'le uçtan uca (E2E dahil) | — | E1, E2, E3, M1 |
| AC3 | Filtre ve sayfalama sunucuda; 2. sayfa doğru 12 parça | K7b, K7c | B11, F19, E1 |
| AC4 | Testler, `./mvnw verify`, `npm run build`, `npm run lint` | tester, coder'lar | Rapor |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | İş sürerken sayfa yenilenir ya da adres yeni sekmede açılır | Durum backend'deki `latestJob`'dan gelir; polling kaldığı yerden sürer | F3, F24 |
| G2 | Bilinmeyen (`/kaynaklar/99999`) ya da sayı olmayan (`/kaynaklar/abc`) kaynak adresi | "Kaynak bulunamadı." + "Bilgi kaynaklarına dön"; sayı olmayan adreste istek atılmaz | F23, B7 |
| G3 | Filtrede büyük harf, Türkçe İ/ı, fazla boşluk, satır sonu ("  ASGARİ   ÖDEME ") | "asgari ödeme" ile aynı sonuç; kutu en fazla 200 karakter alır | B12, B13, B14 |
| G4 | "Yeniden dene"ye art arda basılır ya da iş başka sekmede başlamıştır | İstek sürerken düğme pasif; 409 gelirse mesaj ve liste yenilenir | F4, F5 |
| G5 | Kaynak adında ya da parça metninde HTML (`<img src=x onerror=…>`) | Düz metin olarak görünür, öğe oluşmaz | F22 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Ayrı çalışma alanı ucu** `GET /api/workspaces/{workspaceId}` → `WorkspaceResponse` (ad, `embeddingModel`, `embeddingDimensions`, `targetChunkChars`, `maxChunkChars`, `overlapChars`, `vectorIndexType` `HNSW`, `distanceMetric` `COSINE`) | Bilgi kutusu ve meta kutularının kaynağa bağlı olmayan değerleri tek yerden gelir (kural 1, 5) | Her `SourceResponse`'a gömmek: aynı değer her satırda tekrar eder |
| 2 | **Liste ve detay aynı `SourceResponse`**; dosya alanları (`fileName`, `fileSizeBytes`, `version`) güncel sürümden, yoksa son işten; sayılar (`pageCount`, `chapterCount`, `chunkCount`, `averageTokenCount`, `syncedAt`) güncel sürüm yoksa `null` | `backend-conventions`: aynı kavram aynı DTO. yeniden deneme işlenirken son işin dosya bilgisi görünür | Ayrı `SourceDetailResponse`: iki DTO aynı alanları taşır |
| 3 | **`latestJob` (`IngestionJobResponse`) `SourceResponse`'ta**; web süren işi `GET .../ingestion-jobs/{jobId}` ile izler, iş bitince `sourceKeys.all`'ı invalidate eder | `frontend-conventions` §8: iş kendi key'inde polling; sayfa yenilense de durum backend'den gelir (G1). Referans `embedding-pgvector` `useIngestionJobsPolling` | Listeyi polling: her saniye bütün sayfa + sayılar sorgulanır |
| 4 | **Filtre SQL'de:** `strpos(regexp_replace(lower(translate(heading \|\| ' ' \|\| content, 'Iİ', 'ıi')), '\s+', ' ', 'g'), :query) > 0`; sorgu Java'da `strip()` → boşsa filtre yok → `toLowerCase(Locale.forLanguageTag("tr"))` → `\s+` → tek boşluk. Noktalama korunur | Sunucuda çalışır (kural 7). `translate` Türkçe İ/ı'yı veritabanı diline bağlı olmadan çevirir; `strpos` `%`/`_` kaçışı gerektirmez. Lab'daki noktalama yok sayma SQL'de veritabanının karakter sınıflarına bağlı kalırdı | `ILIKE`: I/ı ve `%` sorunu. Java'da filtre: her istekte bütün parçalar okunur |
| 5 | **Tek parça ucu çalışma alanı düzeyinde** `GET .../chunks/{chunkId}`; eski sürümün parçası da döner | TASK-008 alıntıda sadece `chunkId` bilir (kural 8 "yeniden kullanılabilir") | `.../sources/{sourceId}/chunks/{chunkId}`: alıntıdan kaynak id'si de gerekirdi |
| 6 | **`ChunkSummaryResponse.content` tam metindir**; "İlk satır" web'de üretilir (boşluklar teke, ilk 88 karakter, `…`), başlık 42 karakter (lab `short`) | Lab davranışı birebir; 12 × 2.000 karakter küçük | Backend'de kısaltma: lab kuralı iki yerde yaşar |
| 7 | **PDF ucu** `GET .../sources/{sourceId}/file`: güncel sürümün işinin dosyası, yoksa son işin dosyası; `application/pdf`, `inline` + dosya adı, `nosniff`, `no-store`. Web `<a href target="_blank" rel="noopener noreferrer">` (düğme görünümlü) | Tarayıcının PDF görüntüleyicisi açar; içerik HTML yorumlanmaz (kural 11) | Blob + `URL.createObjectURL` (lab): 20 MB belleğe alınır, sekme kapanınca URL sızar. CSP `sandbox` başlığı: Chrome'un PDF görüntüleyicisini engeller |
| 8 | **`Dialog` kendi bileşenimiz** (portal + `role="dialog"` + `aria-modal` + Tab tuzağı + Esc + dış tık + odak geri dönüşü) | Lab'ın `#modal` davranışı; jsdom'da ek taklit gerekmez | Native `<dialog>`: jsdom `showModal` desteklemez (`web/CLAUDE.md` Tuzaklar) |
| 9 | **Parça penceresi `shared/chunk/`'ta** (`ChunkDialog`, `chunkApi`, `chunkQueries`, `types`) | TASK-008'de `preview` feature'ı da kullanacak; feature'lar birbirini import etmez | `features/source`'ta: TASK-008'de taşıma gerekir |
| 10 | **Tarih ve saat Türkiye saatiyle** (`Europe/Istanbul`, `dd.MM.yyyy HH:mm`, lab "29.10.2026 16:15") | Banka uygulaması; testler makinenin saat diliminden bağımsız | Tarayıcı saati: testler makineye göre değişir |
| 11 | **`averageTokenCount` sorguyla hesaplanır** (`avg(token_count)`, yarım yukarı yuvarlama), saklanmaz | Migration gerekmez; değer parçalardan türer | `source_versions`'a kolon: migration + eski satırlar boş |
| 12 | **İşler tablosu ilk sayfayı alır (`size=20`), sayfa düğmesi yok**; kaynak listesi `size=20`, sayfa sayısı > 1 ise altta aynı `Pagination` | Lab'da ikisinde de düğme yok; bu task'ta bir kaynağın işi sadece yeniden denemeyle artar. Kaynak listesi sınırsız büyüyebilir (`backend-conventions` §6) | Kaynak listesini 100'le sınırlamak: 101. belge görünmez |
| 13 | **Yükleme sürerken pencere kapanmaz** ("Yükle ve işle" → "Yükleniyor…", "Vazgeç", ×, Esc ve dış tık pasif) | İstek birkaç saniye sürer (PDF istekte okunur); kapanırsa sonucu ve hatayı gösterecek yer kalmaz | Kapatıp arka planda sürdürmek: hata mesajı kaybolur |
| 14 | **Hatada pencere açık kalır; seçilen dosya ve yazılanlar silinmez.** Dosyaya ait kodlar (`FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `PDF_*`, `violations[].field = file`) dosya alanının altında; `SOURCE_NAME_TAKEN` ve `name` ihlali ad alanının altında; `version` ihlali sürüm alanının altında; diğerleri pencerenin üstünde `role="alert"` | `frontend-conventions` §7, §10 (dosyalı formda `onSubmit` + mutation) | `useActionState`: React form alanlarını sıfırlar |
| 15 | **İstemci tarafı ön kontrol** (dosya yok / boş / 20 MB üstü / `.pdf` değil / dosya adı > 255; sürüm boş ya da > 40; ad doluysa 2-80) | Gereksiz istek ve bekleme olmaz; son söz backend'de | Sadece backend: 20 MB'lık dosya yüklendikten sonra reddedilir |
| 16 | **TanStack Query varsayılanı:** 4xx `ApiError`'da yeniden deneme yok, diğer hatalarda 1 kez; `refetchOnWindowFocus` varsayılan | 404 gibi kalıcı hatada bekleme olmaz | Varsayılan 3 deneme: "Kaynak bulunamadı" geç görünür |
| 17 | **springdoc bu task'ta da eklenmez**; sözleşme uyumunu qa elle karşılaştırır | Yeni dependency yok (TASK-027 Kararlar #16'nın devamı) | Eklemek: ayrı karar, ayrı task |
| 18 | **Embedding önizlemesi 8 değer** (`ChunkService.EMBEDDING_PREVIEW_SIZE`), web `toFixed(5)` ile (lab) | Lab'daki sayı ve biçim | Tam vektör: sözleşme vektörü dönmez der |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu: TASK-007 durumu ve yeni uçlar.
- `tags`: `Workspaces`, `Chunks` eklendi.
- Yeni: `GET /api/workspaces/{workspaceId}` (`getWorkspace`): 200 `WorkspaceResponse`; 400 `BAD_REQUEST`; 404 `WORKSPACE_NOT_FOUND`; 500.
- `GET .../sources` (`listSources`): açıklama güncellendi; şema `SourceResponse` genişledi.
- Yeni: `GET .../sources/{sourceId}` (`getSource`): 200 `SourceResponse`; 400; 404 `WORKSPACE_NOT_FOUND`, `SOURCE_NOT_FOUND`; 500.
- Yeni: `GET .../sources/{sourceId}/file` (`getSourceFile`): 200 `application/pdf` + başlıklar; 400; 404 aynı; 500.
- Yeni: `GET .../sources/{sourceId}/chunks` (`listChunks`): `query` (≤ 200), `page`, `size`; 200 `ChunkSummaryResponsePage`; 400 `VALIDATION_FAILED` (`query`, `page`, `size`), `BAD_REQUEST`; 404 aynı; 500.
- Yeni: `GET .../chunks/{chunkId}` (`getChunk`): 200 `ChunkResponse`; 400; 404 `WORKSPACE_NOT_FOUND`, `CHUNK_NOT_FOUND`; 500.
- Yeni: `GET .../sources/{sourceId}/ingestion-jobs` (`listIngestionJobs`): 200 `IngestionJobResponsePage` (`id DESC`); 400; 404; 500. Aynı path'teki `POST` (`retryIngestion`) değişmedi.
- `components.parameters.SourceId` eklendi (retry ucu da bunu kullanıyor).
- `components.schemas`: yeni `WorkspaceResponse`, `IngestionJobResponsePage`, `ChunkSummaryResponse`, `ChunkSummaryResponsePage`, `ChunkResponse`; `SourceResponse`'a `fileName`, `fileSizeBytes`, `version`, `pageCount`, `chapterCount`, `chunkCount`, `averageTokenCount`, `syncedAt`, `latestJob` (hepsi `required`, sayılar ve `syncedAt` nullable).
- `ProblemDetail.errorCode` enum'u: `CHUNK_NOT_FOUND`.
- `sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `springboot-patterns`, `jpa-patterns`, `postgres-patterns`, `sql-optimization-patterns`, `spring-transactions`, `springboot-security` (PDF yanıt başlıkları).

**Dependency:** yok. **Migration:** yok.

### Sınıflar (kök `com.rasitbank.retrieval`; yeni alt paketlerde `package-info.java` `@NullMarked`: `workspace/controller`, `workspace/domain/dto`, `chunk/controller`, `chunk/service`, `chunk/domain/dto`)

**workspace/**
- `domain/dto/WorkspaceResponse(long workspaceId, String name, String embeddingModel, int embeddingDimensions, int targetChunkChars, int maxChunkChars, int overlapChars, String vectorIndexType, String distanceMetric)` + `static WorkspaceResponse of(Workspace workspace, IngestionProperties ingestion)`; sabitler `VECTOR_INDEX_TYPE = "HNSW"`, `DISTANCE_METRIC = "COSINE"`.
- `service/WorkspaceService`: constructor'a `IngestionProperties` eklenir; `WorkspaceResponse getWorkspaceResponse(long workspaceId)` (404 `WORKSPACE_NOT_FOUND`). Mevcut `getWorkspace` (entity) değişmez.
- `controller/WorkspaceController` (`@RequestMapping("/api/workspaces/{workspaceId}")`): `@GetMapping WorkspaceResponse getWorkspaceResponse(@PathVariable long workspaceId)`.

**source/**
- `domain/dto/SourceResponse(long sourceId, long workspaceId, String name, SourceKind kind, Instant createdAt, String fileName, long fileSizeBytes, String version, @Nullable Integer pageCount, @Nullable Integer chapterCount, @Nullable Integer chunkCount, @Nullable Integer averageTokenCount, @Nullable Instant syncedAt, IngestionJobResponse latestJob)` + `static SourceResponse of(Source source, @Nullable SourceVersion activeVersion, @Nullable Double averageTokenCount, IngestionJob latestJob)`. `averageTokenCount` → `(int) Math.round(value)`. `from(Source)` silinir.
- `domain/dto/SourceFile(String fileName, byte[] content)`.
- `service/SourceService` (constructor'a `SourceVersionRepository`, `IngestionJobRepository`, `IngestionJobFileRepository`, `ChunkRepository` eklenir):
  - `listSources(long workspaceId, int page, int size)`: kaynak sayfası + üç toplu sorgu (güncel sürümler `findAllById`, son işler `findLatestBySourceIds`, ortalamalar `averageTokenCountsByVersionIds`) → `SourceResponse.of`. Satır başına sorgu yok.
  - `@Transactional(readOnly = true) SourceResponse getSource(long workspaceId, long sourceId)`.
  - `@Transactional(readOnly = true) SourceFile getSourceFile(long workspaceId, long sourceId)`: güncel sürüm varsa onun `ingestionJobId`'sinin dosyası, yoksa `findFirstBySourceIdOrderByIdDesc`'in dosyası.
  - Kaynak bulma (üçünde): `workspaceService.ensureWorkspaceExists` → `sourceRepository.findByIdAndWorkspaceId` yoksa `NotFoundException("SOURCE_NOT_FOUND", ...)`.
- `controller/SourceController`:
  - `@GetMapping("/{sourceId}") SourceResponse getSource(@PathVariable long workspaceId, @PathVariable long sourceId)`.
  - `@GetMapping("/{sourceId}/file") ResponseEntity<byte[]> getSourceFile(...)`: `contentType(MediaType.APPLICATION_PDF)`, `Content-Disposition` = `ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8)`, `X-Content-Type-Options: nosniff`, `CacheControl.noStore()`.
  - `@GetMapping("/{sourceId}/ingestion-jobs") PageResponse<IngestionJobResponse> listIngestionJobs(@PathVariable long workspaceId, @PathVariable long sourceId, page, size)` → `ingestionService.listIngestionJobs(...)` (`backend-conventions` §6 kalıbı).

**ingestion/**
- `service/IngestionService`: `@Transactional(readOnly = true) Page<IngestionJobResponse> listIngestionJobs(long workspaceId, long sourceId, int page, int size)` (404'ler önce).
- `repository/IngestionJobRepository`: `Page<IngestionJob> findBySourceIdOrderByIdDesc(long sourceId, Pageable pageable)`; `@Query("select j from IngestionJob j where j.sourceId in :sourceIds and j.id = (select max(j2.id) from IngestionJob j2 where j2.sourceId = j.sourceId)") List<IngestionJob> findLatestBySourceIds(@Param("sourceIds") Collection<Long> sourceIds)`.

**chunk/**
- `repository/ChunkRepository`:
  - `Optional<Chunk> findByIdAndWorkspaceId(long id, long workspaceId)`.
  - `@Query("select c.sourceVersionId as sourceVersionId, avg(c.tokenCount) as averageTokenCount from Chunk c where c.sourceVersionId in :versionIds group by c.sourceVersionId") List<VersionTokenAverage> averageTokenCountsByVersionIds(@Param("versionIds") Collection<Long> versionIds)`; projeksiyon `repository/VersionTokenAverage` (`long getSourceVersionId()`, `double getAverageTokenCount()`).
  - `@Query(nativeQuery = true, value = ..., countQuery = ...) Page<Chunk> findActiveVersionChunks(@Param("sourceVersionId") long sourceVersionId, @Param("query") @Nullable String normalizedQuery, Pageable pageable)`. Koşul: `source_version_id = :sourceVersionId AND (CAST(:query AS text) IS NULL OR strpos(<normalize>, CAST(:query AS text)) > 0)`, `<normalize>` = Kararlar #4; `ORDER BY chunk_no` (count sorgusunda yok).
- `service/ChunkService`: sabitler `EMBEDDING_PREVIEW_SIZE = 8`, `QUERY_MAX_LENGTH = 200`.
  - `@Transactional(readOnly = true) Page<ChunkSummaryResponse> listChunks(long workspaceId, long sourceId, @Nullable String query, int page, int size)`: 404'ler; kaynağın `activeVersionId` yoksa `Page.empty(PageRequest.of(page, size))`; değilse repository.
  - `@Transactional(readOnly = true) ChunkResponse getChunk(long workspaceId, long chunkId)`: `ensureWorkspaceExists` → `findByIdAndWorkspaceId` yoksa `NotFoundException("CHUNK_NOT_FOUND", ...)`; kaynak ve sürüm id ile okunur.
  - `static @Nullable String normalizeQuery(@Nullable String query)`.
- `domain/dto/ChunkSummaryResponse(long chunkId, int chunkNo, int pageNumber, String heading, String content, int tokenCount)` + `from(Chunk)`.
- `domain/dto/ChunkResponse(long chunkId, int chunkNo, int pageNumber, String heading, String content, int tokenCount, String embeddingModel, int embeddingDimensions, float[] embeddingPreview, long sourceId, String sourceName, String version)` + `static of(Chunk, Source, SourceVersion)`; önizleme `Arrays.copyOf(embedding, min(8, length))`, `embeddingDimensions` = `embedding.length`.
- `controller/ChunkController` (`@RequestMapping("/api/workspaces/{workspaceId}")`, `@Validated`):
  - `@GetMapping("/sources/{sourceId}/chunks") PageResponse<ChunkSummaryResponse> listChunks(@PathVariable long workspaceId, @PathVariable long sourceId, @RequestParam(name = "query", required = false) @Size(max = 200, message = "size must be at most 200 characters") @Nullable String query, page, size)`.
  - `@GetMapping("/chunks/{chunkId}") ChunkResponse getChunk(@PathVariable long workspaceId, @PathVariable long chunkId)`.

### Loglar
- `INFO Source file served sourceId={} jobId={} fileSizeBytes={}`. Dosya adı, kaynak adı, filtre metni ve parça metni loglanmaz. Liste ve okuma uçları ayrıca log yazmaz.

### `retrieval-api/CLAUDE.md` (backend-coder)
- Durum yorumu: TASK-007 okuma uçları. "Kapsam"a yeni uçlar ve filtre kuralı (tek cümle). "Paket yapısı"na `workspace/controller`, `chunk/controller`, `chunk/service`, `ChunkService`, `WorkspaceController`. "Henüz olmayanlar"dan "tek kaynak/sürüm/parça okuma uçları" çıkar. "Komutlar"a örnek: `curl -s 'localhost:8080/api/workspaces/42/sources/10001/chunks?query=asgari%20%C3%B6deme&size=12'`.

## Frontend planı
Ek skill'ler: `react-state-management`, `react-patterns`, `frontend-a11y`, `frontend-security`, `frontend-design` (lab'dan token aktarma).

### Yeni paketler (kart sorusu 1)
- Runtime: `@tanstack/react-query`, `react-error-boundary`, `@fontsource/jetbrains-mono` (400; embedding değerleri lab'da bu yazı tipiyle).
- Test: `msw`.
Sürümleri frontend-coder seçer (React 19 uyumlu son kararlı sürüm), `web/CLAUDE.md` "Teknoloji"ye yazar.

### Kabuk
- `main.tsx`: `@fontsource/jetbrains-mono/400.css`; `<AppProviders>` (`BrowserRouter`'ın içinde).
- `app/AppProviders.tsx`: `QueryClientProvider` (Kararlar #16).
- `app/AdminLayout.tsx`: `<Outlet>` `react-error-boundary` `ErrorBoundary` ile sarılır; fallback `role="alert"` "Beklenmeyen bir hata oluştu. Sayfayı yenile." (`errorMessages.ts`'te `FALLBACK_PAGE_MESSAGE`).
- `app/App.tsx`: `<Route path="kaynaklar/:sourceId" element={<SourceDetailPage />} />`.
- `app/MainNav.tsx`: `/kaynaklar` öğesinde `end` yok (diğerlerinde kalır).
- `index.css`: lab'ın `--s-*` değerlerinden eksik token'lar (tablo, rozet, kutu, pencere, pager); ham hex component CSS'ine yazılmaz.

### shared/
- `api/ApiError.ts`, `api/httpClient.ts` (`request<T>`; `FormData`'da `Content-Type` yazılmaz), `api/errorMessages.ts` (`messageFor`), `api/pagination.ts` (`PageResponse<T>`, `PageParams`) — `frontend-conventions` §7 kalıbı.
- `api/workspace.ts`: `WORKSPACE_ID = 42`, `WORKSPACE_API_PATH = '/api/workspaces/42'`.
- `format/formatNumber.ts` `formatInteger(value)` (tr-TR: `412.873`); `format/formatDateTime.ts` `formatDateTime(iso)` (`Europe/Istanbul`, `29.10.2026 16:15`); `format/formatDuration.ts` `formatDuration(ms)` (lab `msTxt`: < 1000 → `850 ms`; < 10000 → `8,44 sn`; ≥ 10000 → `12,3 sn`); `format/formatText.ts` `collapseWhitespace(text)`, `shortenText(text, maxLength)` (lab `short`: uzunsa ilk `maxLength - 1` karakter + `…`).
- `components/Dialog.tsx` (+ css): props `{ title: string; onClose: () => void; canClose?: boolean; initialFocusRef?: RefObject<HTMLElement | null>; children: ReactNode }`. Kararlar #8; açılınca `initialFocusRef` yoksa × ("Kapat") odaklanır (lab); kapanınca açılış anındaki `document.activeElement`'e döner (DOM'da hâlâ varsa). Başlık `<h3>`, `aria-labelledby` ile bağlı.
- `components/Pagination.tsx`: `{ label: string; page: number; totalPages: number; onPageChange: (page: number) => void }` → `<nav aria-label={label}>`, metin `${page + 1} / ${Math.max(1, totalPages)}`, "Önceki" (ilk sayfada pasif), "Sonraki" (son sayfada pasif).
- `components/StatusBadge.tsx`: `{ tone: 'ok' | 'run' | 'bad' | 'neutral'; children }` (lab `.st.ok/.run/.bad/.gr`).
- `components/ErrorAlert.tsx`: `{ errorCode: string }` → `role="alert"` + `messageFor`.
- `components/Callout.tsx`: lab `.callout` (ikon + içerik).
- `components/PageHeader.tsx`: props `{ section: string; title: string; description?: ReactNode; parent?: { label: string; to: string }; actions?: ReactNode }`. `parent` varsa breadcrumb `<Link to={parent.to}>{parent.label}</Link> › <b>{section}</b>`, yoksa bugünkü gibi `Dijital Asistan › <b>{section}</b>`.
- `chunk/types.ts` (`ChunkResponse`), `chunk/chunkApi.ts` (`getChunk(chunkId)`), `chunk/chunkQueries.ts` (`chunkKeys.detail(chunkId)`, `useChunkQuery(chunkId: number | null)`), `chunk/ChunkDialog.tsx` (+ css): props `{ chunkId: number; onClose: () => void }`. İçerik: başlık = `heading`; şerit (lab `.cfg`): `chunk_id`, `chunk_no`, `token`, `model` (`embeddingModel`), `kaynak` (`${sourceName} · ${version}`); metin (lab `.ctext`, `white-space: pre-wrap`, `tabIndex=0`); `embedding [v1, …, v8, … ${embeddingDimensions - embeddingPreview.length} değer daha]` (`toFixed(5)`, `--font-mono`); "Kapat" düğmesi. Yüklenirken "Yükleniyor…", hatada `ErrorAlert`.

### features/source/
- `model/types.ts`: `WorkspaceResponse`, `SourceResponse`, `INGESTION_STAGES` + `IngestionStage`, `IngestionJobResponse`, `ChunkSummaryResponse`, `CreateSourceInput = { file: File | null; name: string; version: string }`, `ListChunksParams = { query: string; page: number; size: number }` — alan adları sözleşmeyle birebir.
- `model/ingestionRules.ts`: `INGESTION_POLL_INTERVAL_MS = 1000`, `isJobActive(job)` (`DONE`/`FAILED` dışı), `sourceStatus(source, job): 'RUNNING' | 'READY' | 'FAILED'` (iş aktif → RUNNING; `chunkCount === null` ve iş `FAILED` → FAILED; diğer → READY), `canRetry(source, job)` (= FAILED).
- `model/sourceUploadRules.ts`: `SOURCE_NAME_MIN_LENGTH = 2`, `SOURCE_NAME_MAX_LENGTH = 80`, `VERSION_MAX_LENGTH = 40`, `FILE_MAX_BYTES = 20 * 1024 * 1024`, `FILE_NAME_MAX_LENGTH = 255`; mesajlar (kart "Ekrandaki metinler"): `FILE_REQUIRED_MESSAGE`, `FILE_EMPTY_MESSAGE`, `FILE_NAME_TOO_LONG_MESSAGE`, `FILE_RULE_MESSAGE` (backend `file` ihlali), `SOURCE_NAME_RULE_MESSAGE`, `VERSION_RULE_MESSAGE`; `validateSourceUpload(input): Partial<Record<'file' | 'name' | 'version', string>>` (boyut ve tür için `messageFor('FILE_TOO_LARGE')` / `messageFor('UNSUPPORTED_FILE_TYPE')`). Ad ve sürüm kontrolden ve gönderimden önce `trim()`; boş ad gönderilmez (backend dosya adını kullanır).
- `model/chunkRules.ts`: `CHUNK_PAGE_SIZE = 12`, `CHUNK_FILTER_DEBOUNCE_MS = 300`, `CHUNK_FILTER_MAX_LENGTH = 200`, `CHUNK_PREVIEW_LENGTH = 88`, `CHUNK_HEADING_PREVIEW_LENGTH = 42`, `SOURCE_PAGE_SIZE = 20`, `JOB_PAGE_SIZE = 20`.
- `api/sourceApi.ts`: `getWorkspace()`, `listSources(params: PageParams)`, `getSource(sourceId)`, `createSource(input)` (`FormData`: `file`, dolu ise `name`, `version`), `retryIngestion(sourceId)`, `listIngestionJobs(sourceId, params)`, `getIngestionJob(jobId)`, `listChunks(sourceId, params)` (boş `query` gönderilmez), `sourceFileUrl(sourceId): string`.
- `api/sourceQueries.ts`: `workspaceKeys.detail()`; `sourceKeys.all = ['sources']`, `.list(params)`, `.detail(sourceId)`, `.jobs(sourceId)`, `.job(jobId)`, `.chunks(sourceId, params)`. Hook'lar: `useWorkspaceQuery()`, `useSourcesQuery(params)`, `useSourceQuery(sourceId)`, `useIngestionJobsQuery(sourceId)`, `useChunksQuery(sourceId, params)` (`placeholderData: keepPreviousData`), `useCreateSourceMutation()` (başarıda `sourceKeys.all` invalidate + dönen iş `sourceKeys.job` cache'ine), `useRetryIngestionMutation()` (aynısı).
- `hooks/useIngestionJobsPolling.ts`: `useIngestionJobsPolling(jobs: IngestionJobResponse[]): Map<number, IngestionJobResponse>`; aktif her iş için `useQueries` (`initialData` = gelen iş, `refetchInterval` iş aktifken `INGESTION_POLL_INTERVAL_MS`, 4xx'te ya da iş bitince `false`); bir iş aktiften `DONE`/`FAILED`'a geçince `sourceKeys.all` invalidate. Sayfa unmount olunca sorgular durur.
- `pages/SourceListPage.tsx`: `PageHeader` (açıklama: "Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça {embeddingModel} ile embed edilip pgvector'e yazılır." — model gelene kadar "embedding modeli"), `actions` = "Belge ekle". Yükleniyor → "Yükleniyor…"; hata → `ErrorAlert`; boş → `SourceEmptyState`; dolu → `SourceTable` (+ `Pagination` "Kaynak sayfaları", sadece `totalPages > 1`); altında `WorkspaceCallout`; retry hatası tablonun üstünde `ErrorAlert`.
- `components/SourceTable.tsx`: `{ sources, jobsById, onRetry, retryingSourceId }`. Satır (lab `.tbl tr.click`): Kaynak hücresi `<Link to={/kaynaklar/${sourceId}}>` (ad, kalın) + dosya adı (`small`); Tür "PDF · 70 sayfa" (`pageCount` null → "PDF"); Sürüm; Parça (`chunkCount` ya da "–"); Durum: RUNNING → `StatusBadge run` aşama adı; READY → `ok` "Hazır"; FAILED → `bad` "Başarısız" + `small` `messageFor(job.errorCode)` + `btn out` "Yeniden dene" (istek sürerken pasif; tık satıra yayılmaz); Son senkron (`formatDateTime` ya da "–"). Satırın boş yerine tık da detaya gider.
- `components/SourceEmptyState.tsx`: `{ onAdd }`; başlık, metin, "Belge ekle" (kart metinleri).
- `components/WorkspaceCallout.tsx`: `{ workspace }`; lab metni: "Çalışma alanı **{name}** (ws {workspaceId}) · embedding modeli **{embeddingModel}@{embeddingDimensions}**. Modeli değiştirmek bütün parçaların yeniden embed edilmesi demek; retrieval-api farklı modelle yüklemeyi 409 ile reddeder."
- `components/AddSourceDialog.tsx`: `{ onClose }` (açık/kapalı durumu sayfada). `Dialog title="Belge ekle"`, `initialFocusRef` = "Dosya seç" düğmesi, `canClose = !isPending`. İçerik (lab "Yeni sürüm yükle" düzeni): açıklama `p`; dosya kutusu (lab `.file` + PDF ikonu): seçilmediyse "Dosya seçilmedi" + "PDF · en fazla 20 MB", seçildiyse dosya adı + "{formatInteger(size)} bayt"; "Dosya seç" `btn out` (gizli `<input type="file" accept="application/pdf,.pdf" aria-label="PDF dosyası">`'i açar); "Kaynak adı" alanı + ipucu; "Sürüm etiketi" alanı (placeholder "ör. 2026.10"); alan hataları altta (`aria-describedby`, `aria-invalid`); `drow`: "Vazgeç" (`btn out`), "Yükle ve işle" (gönderilirken "Yükleniyor…", pasif). `onSubmit` → `validateSourceUpload` → hata yoksa mutation; 202'de `onClose()` (odak "Belge ekle"ye döner). Kararlar #13-#15.
- `pages/SourceDetailPage.tsx`: `sourceId` `/^\d+$/` değilse istek atmadan bulunamadı görünümü. `SOURCE_NOT_FOUND` / bulunamadı → `PageHeader` yok; "Kaynak bulunamadı." + `<Link to="/kaynaklar">Bilgi kaynaklarına dön</Link>`. Normal: `PageHeader` (`parent = { label: 'Bilgi kaynakları', to: '/kaynaklar' }`, `section`/`title` = ad, açıklama `"{fileName} · {formatInteger(fileSizeBytes)} bayt · sürüm {version} · son senkron {syncedAt ? formatDateTime : '–'}"`, `actions` = `<a className="btn out" href={sourceFileUrl(id)} target="_blank" rel="noopener noreferrer">PDF'i aç</a>`); `SourceMetaGrid`; `sect` "İçe alma işleri" + `IngestionJobTable`; `sect` "Parçalar" + sağda `"{totalElements} / {chunkCount ?? 0}"`; filtre kutusu (lab `.fbox`, `aria-label="Parçalarda ara"`, placeholder "Parçalarda ara (başlık ya da metin)", `maxLength=200`); `ChunkTable`; `Pagination` ("Parça sayfaları"); seçili `chunkId` varsa `ChunkDialog`. `latestJob` aktifse `useIngestionJobsPolling([latestJob])`.
- `components/SourceMetaGrid.tsx`: `{ source, workspace }`; lab `.meta` kutuları: "Sayfa / bölüm" `70 / 11`; "Parça" `154 · ort. 285 token`; "Parça boyutu" `1.600-2.000 karakter`; "Örtüşme" `200 karakter`; "Embedding" `bge-m3 · 1024`; "Index" `HNSW · cosine` (`distanceMetric.toLowerCase()`). Güncel sürüm yoksa ilk iki kutu "–".
- `components/IngestionJobTable.tsx`: `{ jobs }`; sütunlar İş (`#{jobId}`), Dosya, Sürüm, Parça (`totalChunks ?? '–'`), Süre (`startedAt` ve `finishedAt` varsa `formatDuration(fark)`, yoksa "–"), Durum (`DONE` → ok, `FAILED` → bad, diğer → run; metin aşama adı).
- `components/ChunkTable.tsx`: `{ chunks, onOpen }`; sütunlar #, Sayfa · başlık (`<button>`: `<b>s. {pageNumber}</b>` + `small` kısaltılmış başlık; odak burada kalır ve pencere kapanınca buraya döner), İlk satır, Token; boş → tek satır "Eşleşen parça yok.". Satırın boş yerine tık da açar.
- Filtre: girilen metin `CHUNK_FILTER_DEBOUNCE_MS` sonra sorguya girer; filtre değişince sayfa 0. Filtre ve sayfa component state'inde (adreste değil).

### errorCode → Türkçe (`shared/api/errorMessages.ts`)
Metinler onay kartındaki "Ekrandaki metinler" bölümüyle birebir. Kodlar: `VALIDATION_FAILED`, `NETWORK_ERROR`, `WORKSPACE_NOT_FOUND`, `SOURCE_NOT_FOUND`, `INGESTION_JOB_NOT_FOUND`, `CHUNK_NOT_FOUND`, `SOURCE_NAME_TAKEN`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `PDF_UNREADABLE`, `PDF_NO_TEXT`, `PDF_NO_BOOKMARKS`, `EMBEDDING_MODEL_MISMATCH`, `SOURCE_ALREADY_INGESTED`, `INGESTION_IN_PROGRESS`, `EMBEDDING_UNAVAILABLE`, `EMBEDDING_DIMENSION_MISMATCH`, `INGESTION_INTERRUPTED`. Diğerleri (`INTERNAL_ERROR`, `UNEXPECTED_ERROR`, `BAD_REQUEST`, `NOT_FOUND`, `UNSUPPORTED_MEDIA_TYPE` …) → `FALLBACK_MESSAGE` "Beklenmeyen bir hata oluştu. Tekrar dene."

### `web/CLAUDE.md` (frontend-coder)
Durum yorumu; "Teknoloji"de kurulu paketler; "Henüz olmayanlar"dan kurulanlar çıkar; adreslerde `/kaynaklar/:sourceId` gerçek.

## Test senaryoları
Backend: fixture'lar `SourceFixtures`, `ChunkFixtures` (parçalar doğrudan repository ile, deterministik vektör); gerçek rehberle olan testler TASK-027'deki gibi sahte embedding ile içe alınır. Frontend: MSW handler'ları sözleşmeye uyar, hatalar `problem(status, errorCode)`; `renderWithProviders` `QueryClient` (retry kapalı) + `MemoryRouter` ile.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Çalışma alanı | controller `WorkspaceControllerTest` | `givenWorkspace42_whenGetWorkspace_thenReturnsModelChunkingAndIndex` / `givenWorkspace43_whenGetWorkspace_thenReturns404WorkspaceNotFound` | `name` "Raşit Bankası · Dijital Asistan", `embeddingModel` `bge-m3`, `embeddingDimensions` 1024, 1600, 2000, 200, `HNSW`, `COSINE`; 404 `WORKSPACE_NOT_FOUND` |
| B2 | Hazır kaynak alanları | integration `SourceReadIntegrationTest` (Testcontainers, rehber sahte embedding ile içe alınır) | `givenGuideIngested_whenListSources_thenItemCarriesVersionCountsAndLatestJob` | `fileName` rehber dosyası, `version` `2026.10`, `pageCount` 70, `chapterCount` 11, `chunkCount` 154, `averageTokenCount` 285, `syncedAt` = işin `finishedAt`'ı, `latestJob.stage` `DONE` |
| B3 | Başarısız ilk iş | unit `SourceServiceTest` | `givenSourceWhoseOnlyJobFailed_whenListSources_thenFileFieldsFromJobAndCountsNull` | `fileName`, `fileSizeBytes`, `version` işten; `pageCount`, `chapterCount`, `chunkCount`, `averageTokenCount`, `syncedAt` null; `latestJob.stage` `FAILED` |
| B4 | Son iş seçimi | integration | `givenSourceWithFailedAndRetriedJobs_whenListSources_thenLatestJobIsHighestId` | iş 1 FAILED, iş 2 DONE → `latestJob.jobId` 2 |
| B5 | Yuvarlama | unit `SourceResponseTest` | `givenAverageTokenCount10Point5_whenOf_thenRoundsTo11` | 10.5 → 11; 10.49 → 10 |
| B6 | Tek kaynak | controller `SourceControllerTest` | `givenSource10001_whenGetSource_thenReturns200` / `givenUnknownSource_whenGetSource_thenReturns404SourceNotFound` | 200 `sourceId` 10001; 404 `SOURCE_NOT_FOUND` |
| B7 | Yanlış çalışma alanı | controller | `givenWorkspace43_whenGetSource_thenReturns404WorkspaceNotFound` | 404 `WORKSPACE_NOT_FOUND` |
| B8 | PDF | integration `SourceFileIntegrationTest` (`RANDOM_PORT`) | `givenDoneSourceWithTurkishFileName_whenGetSourceFile_thenReturnsSameBytesAsPdfInline` | 200; gövde yüklenen baytlarla aynı; `Content-Type` `application/pdf`; `ContentDisposition.parse(...)` `isInline` ve `getFilename()` = `Şirket El Kitabı.pdf`; `X-Content-Type-Options` `nosniff`; `Cache-Control` `no-store` |
| B9 | Sürümsüz kaynağın PDF'i | integration | `givenSourceWhoseFirstJobFailed_whenGetSourceFile_thenReturnsLatestJobFile` | Gövde son işin dosyasıyla aynı |
| B10 | İşler | integration | `givenFailedThenRetriedSource_whenListIngestionJobs_thenNewestFirst` / `givenUnknownSource_whenListIngestionJobs_thenReturns404` | `content[0].jobId` 2, `content[1].jobId` 1, `totalElements` 2; 404 `SOURCE_NOT_FOUND` |
| B11 | Parça sayfası | integration `ChunkListIntegrationTest` (rehber) | `givenGuide154Chunks_whenListChunksPage1Size12_thenChunkNo12To23` | `chunkNo` 12..23 sırayla, `totalElements` 154, `totalPages` 13, `page` 1, `size` 12 |
| B12 | Filtre | integration (`ChunkFixtures`, tek sürüm) | `givenFourChunks_whenListChunksWithSpacedUpperCaseQuery_thenMatchesHeadingAndTextAcrossLineBreak` | Parçalar: A içerik `Asgari\nödeme tutarı`, B başlık `ASGARİ ÖDEME`, C `asgari tutar`, D `Asgari-ödeme`. `query` `"  ASGARİ   ödeme "` → [A, B], `totalElements` 2 (D noktalama nedeniyle yok) |
| B13 | Türkçe I | integration | `givenChunkKisaVadeli_whenListChunksWithTurkishLowerCase_thenMatchesOnlyDottedlessI` | İçerik `KISA VADELİ`: `kısa vadeli` → 1; `kisa` → 0 |
| B14 | Sorgu sınırı | controller `ChunkControllerTest` | `givenQueryOf201Chars_whenListChunks_thenReturns400OnQuery` / `givenQueryOf200Chars_whenListChunks_thenReturns200` | 400 `VALIDATION_FAILED`, `violations[0].field` `query`; 200 |
| B15 | Sürümsüz kaynak | unit `ChunkServiceTest` | `givenSourceWithoutActiveVersion_whenListChunks_thenEmptyPageWithoutQuery` | `totalElements` 0; repository'nin `findActiveVersionChunks`'ı çağrılmadı |
| B16 | Eski sürüm | integration | `givenTwoVersionsActiveIsSecond_whenListChunks_thenOnlySecondVersionChunks` | Bütün `chunkId`'ler ikinci sürümün |
| B17 | Tek parça | integration | `givenChunk_whenGetChunk_thenReturnsTextFirstEightValuesAndSource` / `givenUnknownChunk_whenGetChunk_thenReturns404ChunkNotFound` | `embeddingPreview` = saklanan vektörün ilk 8 değeri, `embeddingDimensions` 1024, `embeddingModel` `bge-m3@1024`, `sourceName`, `version` `2026.10`; 404 `CHUNK_NOT_FOUND` |
| B18 | Normalizasyon | unit `ChunkServiceTest` | `givenQueries_whenNormalizeQuery_thenTurkishLowerCaseSingleSpacesOrNull` | `"  ASGARİ\n ÖDEME "` → `asgari ödeme`; `"KISA"` → `kısa`; `"   "` ve `null` → `null` |
| B19 | Log | integration (`OutputCaptureExtension`) | `givenSourceFileServed_whenLogsCaptured_thenIdsLoggedButNoFileName` | `Source file served sourceId=` var; dosya adı ve kaynak adı yok |
| F1 | Boş durum + bilgi kutusu | component `SourceListPage.test.tsx` | `givenNoSources_whenPageLoads_thenShowsEmptyStateAndWorkspaceCallout` | "Henüz belge yok" görünür; 2 "Belge ekle" düğmesi; kutuda "Raşit Bankası · Dijital Asistan", "ws 42", "bge-m3@1024" (MSW'den) |
| F2 | Hazır satır | component | `givenReadySource_whenPageLoads_thenRowShowsLabColumns` | "Raşit Bankası Ürün ve Hizmet Rehberi", dosya adı, "PDF · 70 sayfa", "2026.10", "154", "Hazır", "29.10.2026 16:15" (`syncedAt` `2026-10-29T13:15:44Z`) |
| F3 | Polling | component | `givenEmbeddingJob_whenJobAdvancesToDone_thenRowShowsHazirAndPollingStops` | Satır "EMBEDDING" → (iş ucu EMBEDDING, WRITING, DONE döner; liste 2. çağrıda hazır kaynak) → "Hazır"; iş ucuna tam 3 istek |
| F4 | Yeniden dene | component | `givenFailedSource_whenRetryClicked_thenRowShowsQueued` | "Başarısız", "Embedding modeline ulaşılamadı. Model sunucusu açılınca yeniden dene.", "Yeniden dene"; tık → `POST .../sources/10002/ingestion-jobs` 1 kez, satır "QUEUED", düğme yok |
| F5 | Yeniden dene 409 | component | `givenRetryConflict_whenRetryClicked_thenShowsInProgressMessage` | `role="alert"` "Bu belgenin işi zaten sürüyor."; liste yeniden istendi |
| F6 | Detaya gitme | component (App) | `givenSourceRow_whenNameLinkClicked_thenNavigatesToDetail` | Adres `/kaynaklar/10001`, h1 kaynak adı |
| F7 | Pencere odağı | component `AddSourceDialog.test.tsx` (sayfa içinden) | `givenListPage_whenAddClickedAndEscPressed_thenFocusesFileButtonThenReturnsFocus` | Açılınca odak "Dosya seç"; Esc → `dialog` yok, odak "Belge ekle" |
| F8 | İstemci kontrolü | component | `givenNoFileAndBlankVersion_whenSubmit_thenFieldMessagesAndNoRequest` | "Bir PDF dosyası seç." ve "Sürüm etiketi zorunlu; en fazla 40 karakter." alanların altında, `aria-invalid`; `POST` yok |
| F9 | Boyut ve tür | component | `givenFileOver20MiB_whenSubmit_thenFileTooLargeMessage` / `givenTxtFile_whenSubmit_thenUnsupportedTypeMessage` | 20 MiB + 1 bayt → "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç."; `notlar.txt` → "Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil."; istek yok |
| F10 | Yer imi yok | component | `givenServerRejectsNoBookmarks_whenSubmit_thenFixInstructionUnderFileAndValuesKept` | 422 `PDF_NO_BOOKMARKS` → kart metni dosya alanının altında; pencere açık; dosya adı, "Kaynak adı" ve "Sürüm etiketi" değerleri duruyor |
| F11 | Aynı ad | component | `givenServerRejectsNameTaken_whenSubmit_thenMessageUnderName` | 409 → "Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz." ad alanının altında |
| F12 | Sunucu doğrulaması | component | `givenServerViolationOnVersion_whenSubmit_thenVersionRuleMessage` | 400 `violations[{field: version}]` → sürüm kural mesajı; backend `message` ekranda yok |
| F13 | Başarılı yükleme | component | `givenValidUpload_whenSubmit_thenDialogClosesAndNewRowQueued` | İstek gövdesi: `file`, `name` `Atlaskap El Kitabı` (baştaki/sondaki boşluk silinmiş), `version` `1.0`; ad boşken `name` parçası yok (ayrı `it`); pencere kapanır; yeni satır "QUEUED"; odak "Belge ekle" |
| F14 | Gönderim sürerken | component | `givenPendingUpload_whenEscPressed_thenDialogStaysAndButtonsDisabled` | "Yükleniyor…" pasif; "Vazgeç" pasif; Esc sonrası `dialog` duruyor |
| F15 | Erişilebilirlik | component | `givenListPage_whenRendered_thenHasNoAxeViolations` (+ pencere, detay, parça penceresi için birer `it`) | `toHaveNoViolations` |
| F16 | Detay başlığı | component `SourceDetailPage.test.tsx` | `givenReadySource_whenDetailLoads_thenHeaderAndPdfLink` | Breadcrumb linki "Bilgi kaynakları" → `/kaynaklar`; h1 ad; "Rasit-…-2026.10.pdf · 412.873 bayt · sürüm 2026.10 · son senkron 29.10.2026 16:15"; "PDF'i aç" `href` `/api/workspaces/42/sources/10001/file`, `target` `_blank`, `rel` `noopener noreferrer` |
| F17 | Meta kutuları | component | `givenReadySource_whenDetailLoads_thenMetaBoxesFromBackend` | "70 / 11", "154 · ort. 285 token", "1.600-2.000 karakter", "200 karakter", "bge-m3 · 1024", "HNSW · cosine" |
| F18 | İşler tablosu | component | `givenFailedAndDoneJobs_whenDetailLoads_thenNewestFirstWithDuration` | 1. satır "#2", "154", "8,44 sn" (`startedAt`→`finishedAt` 8.440 ms), "DONE"; 2. satır "#1", "–", "–", "FAILED" |
| F19 | Parça sayfaları | component | `givenThirteenPages_whenNextClicked_thenRequestsPage1Size12` | İlk görünüm 12 satır, "1 / 13", "154 / 154", "Önceki" pasif; "Sonraki" → istek `page=1&size=12`, "2 / 13" |
| F20 | Filtre | component | `givenFilterTyped_whenDebounced_thenRequestsQueryFromPage0` / `givenNoMatch_whenFiltered_thenShowsEmptyRowAndOnePage` | 2. sayfadayken "asgari ödeme" → istek `query=asgari ödeme&page=0` (tek istek), "3 / 154"; boş sonuç → "Eşleşen parça yok.", "1 / 1", iki düğme pasif |
| F21 | Parça penceresi | component `ChunkDialog.test.tsx` + sayfa | `givenChunkRow_whenClickedAndEscPressed_thenDialogShowsChunkAndFocusReturns` / `givenChunkId_whenRenderedAlone_thenLoadsChunk` | Başlık `2.16 Kayıp ya da çalıntı bildirimi`; şerit `chunk_id 151`, `chunk_no 31`, `token 297`, `model bge-m3@1024`, `kaynak Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10`; "embedding [0.01086, -0.00193, -0.06578, 0.01364, -0.01739, -0.03014, 0.01209, -0.04669, … 1016 değer daha]"; Esc → pencere yok, odak satırın düğmesinde. Tek başına render (`chunkId` prop'u) da çalışır |
| F22 | HTML güvenliği | component | `givenHtmlInSourceNameAndChunkText_whenRendered_thenShownAsText` | Metin `<img src=x onerror=alert(1)>` olduğu gibi görünür; `img` öğesi yok |
| F23 | Bulunamayan kaynak | component | `givenUnknownSource_whenDetailLoads_thenNotFoundMessage` / `givenNonNumericId_whenDetailLoads_thenNotFoundWithoutRequest` | "Kaynak bulunamadı." + "Bilgi kaynaklarına dön" linki; `abc`'de hiç istek yok |
| F24 | Detayda polling | component | `givenRunningLatestJob_whenJobDone_thenDetailJobsAndChunksRefetched` | İş DONE olunca kaynak, işler ve parça uçlarına birer yeni istek; meta "70 / 11" |
| F25 | Mesaj eşlemesi | unit `errorMessages.test.ts` | `givenEachTask007ErrorCode_whenMessageFor_thenReturnsCardText` | Plandaki her kod kart metnini döner; bilinmeyen kod → "Beklenmeyen bir hata oluştu. Tekrar dene." |
| F26 | Ağ hatası | component | `givenNetworkError_whenListLoads_thenShowsNetworkMessage` | "Sunucuya ulaşılamıyor. Bağlantını kontrol et." |
| F27 | Menü | component `App.test.tsx` | `givenDetailRoute_whenRendered_thenSourcesNavItemIsCurrent` | "Bilgi kaynakları" linki `aria-current="page"` |
| F28 | Polling durur | component | `givenRunningJob_whenNavigatedAway_thenNoMoreJobRequests` | `/onizleme`'ye geçince iş ucuna yeni istek yok |
| F29 | Biçimleyiciler | unit `format*.test.ts` | `givenMilliseconds_whenFormatDuration_thenLabFormat` / `givenIso_whenFormatDateTime_thenIstanbulTime` / `givenLongText_whenShortenText_thenEllipsis` | 850 → "850 ms", 8440 → "8,44 sn", 12345 → "12,3 sn"; `2026-10-29T13:15:44Z` → "29.10.2026 16:15"; 90 karakter, 88 → 87 karakter + "…" |
| E0 | Boş durum | e2e `web/e2e/sources.spec.ts` | `givenEmptyKnowledgeBase_whenSourcesOpened_thenShowsEmptyState` | Liste boşsa "Henüz belge yok"; liste boş değilse `test.skip` |
| E1 | Uçtan uca yükleme | e2e (gerçek sistem, Ollama açık) | `givenGuidePdf_whenUploadedViaDialog_thenReadyAndDetailFilterPagingAndChunkDialogWork` | Ad `E2E Rehber <zaman damgası>`, sürüm `2026.10`; satır bir aşama gösterir, en fazla 120 sn içinde "Hazır"; detayda iş "DONE", "154 / 154"; "Sonraki" → ilk satır `#12`; filtre "asgari ödeme" → her satırın metni ya da başlığı ifadeyi içerir (harf farksız); parça penceresi açılır, Esc ile kapanır |
| E2 | Yer imsiz PDF | e2e | `givenPdfWithoutBookmarks_whenUploaded_thenFixMessageAndNoNewRow` | Fixture `web/e2e/fixtures/yer-imi-yok.pdf` (tester üretir) → `PDF_NO_BOOKMARKS` metni; satır sayısı değişmez |
| E3 | Aynı ad | e2e | `givenExistingName_whenUploadedAgain_thenNameTakenMessage` | E1'deki adla → ad alanının altında kart metni |
| E4 | PDF'i aç | e2e | `givenReadySource_whenPdfOpened_thenNewTabReceivesPdf` | Yeni sekmenin yanıtı 200, `content-type` `application/pdf` |

**Elle çalıştırılanlar:**
- **M1 (devops):** `docker compose -f deployment/docker-compose.yml down -v` + `up --build -d`. `npm run test:e2e` (3000) geçer. `curl -sI localhost:3000/api/workspaces/42/sources/<id>/file` → `application/pdf`, `inline`, `nosniff`. Rehber gerçek adıyla ("Raşit Bankası Ürün ve Hizmet Rehberi", 2026.10) ve yer imli başka bir PDF tarayıcıdan yüklenir; listede iki kaynak, rehber satırı "PDF · 70 sayfa · 2026.10 · 154 · Hazır".
- **M2 (devops):** Ollama'ya ulaşılamayan override'la (`deployment/CLAUDE.md`, TASK-005) bir PDF yüklenir → satır "Başarısız" + sebep + "Yeniden dene". Override kaldırılıp servis yeniden açılınca düğmeye basılır → "Hazır", kaynak tek.
- **M3 (qa):** Lab (`kaynaklar/rag-lab.html`, sol yarı) ile aynı genişlikte yan yana: liste, bilgi kutusu, detay başlığı, meta kutuları, iş tablosu, parça tablosu, filtre, pager, parça penceresi, "Belge ekle" penceresi (lab'ın "Yeni sürüm yükle" penceresiyle). Farklar karttaki kararlarla sınırlı.
- **M4 (qa):** İş sürerken sayfayı yenileme (G1), `/kaynaklar/abc`, filtrede "ASGARİ ÖDEME", 360 px genişlikte tabloların yatay kaydırılması.

## Deployment planı
- Docker dosyaları, Compose ve `web/nginx.conf` değişmez. PDF ucu `location /api/workspaces/` üzerinden gider (`nosniff` nginx'te de var).
- `deployment/CLAUDE.md` (devops): "Gerçek yapı (TASK-007)" — Bilgi kaynakları ekranı 3000'de; E2E'nin gerçek sistem ve Ollama istediği; E2E'nin `E2E Rehber …` adlı kaynaklar bıraktığı ve bunların ancak `down -v` ile silindiği.
- Lokal model: yeni model yok (`bge-m3`).

## Açık sorular ve riskler
1. **Kart sorusu 1 (dependency):** cevap gelene kadar frontend-coder paket kurmaz.
2. **Türkçe harf eşleşmesi (risk):** `lower()` `ş, ğ, ö, ü, ç`'yi veritabanının dil ayarına göre çevirir; pgvector imajının varsayılanı (`en_US.utf8`) bunu yapar, C diliyle kurulan bir veritabanında yapmaz. B12/B13 aynı imajla doğrular.
3. **Parça boyutu ve örtüşme (risk):** meta kutuları bugünkü ayarı gösterir; ayar değişir ve belge yeniden yüklenmezse eski parçalar için yanlış olur (sürüm başına ayar saklanmıyor).
4. **E2E verisi (risk):** kaynak silme olmadığı için E2E her koşuda bir `E2E Rehber …` kaynağı bırakır; Ollama ile içe alma ~10 sn sürer.
5. **Kapsam dışı not:** `rag.md` "Lab'da görünenler" rehberi 31 sayfa / 61 parça diye anlatıyor; gerçek rehber 70 sayfa / 154 parça (TASK-027). Dosya kullanıcının.

STATUS: DONE
