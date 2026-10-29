# İndeksleme zamanı (retrieval-api)

> Bilgi bankası bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılır. Çalışan bir örnek `../embedding-pgvector/backend`'dedir (okunur kalıp referansı: içe alma hattı, parçalama, HNSW sorguları). Kod toplu kopyalanmaz, oraya yazılmaz; çalışma zamanında ona bağlanılmaz.
> SQL ve index ayrıntısı `postgres-patterns`, transaction ve kilit `spring-transactions`, arka plan executor'ı `java-concurrency`'dedir.

## 1. İş ve aşamaları

- Yükleme `202 Accepted` + `Location` (`/api/workspaces/42/ingestion-jobs/{jobId}`) + iş DTO'su döner; istemci işi polling ile izler (`frontend-conventions` bölüm 8).
- Aşamalar (lab): `QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE`, hata olursa `FAILED` (`errorCode` ile). `progress` 0-1 arası, `embeddedChunks` / `totalChunks` ilerlemeyi taşır.
- İş, oluşturan transaction commit edildikten sonra arka plan executor'ına verilir (`TransactionSynchronization.afterCommit`). Her aşama kendi kısa transaction'ında yazılır; böylece polling eden ekran ilerlemeyi görür.
- Bir kaynağın aynı anda tek aktif işi olur: yeni sürüm yüklenirken kaynak satırı kilitlenir, aktif iş varsa 409 `INGESTION_IN_PROGRESS`; `ingestion_jobs` üzerindeki partial unique index son güvencedir.

## 2. Okuma (PARSING)

- Kaynak rehber bir PDF'tir (lab: "Raşit Bankası Ürün ve Hizmet Rehberi", sürüm 2026.10, 31 sayfa). Sayfa numarası ve bölüm başlığı parçaya taşınır; alıntıda "s. 7" gösterilir.
- PDF kütüphanesi plan kararıdır (referans uygulamada Apache PDFBox).
- Dosya türü uzantıdan değil içerikten de doğrulanır; desteklenmeyen tür 415 `UNSUPPORTED_FILE_TYPE`, büyük dosya 413 `FILE_TOO_LARGE` (örnek kodlar).

## 3. Parçalama (CHUNKING)

Lab ayarları (retrieval-api `application.properties`):

```properties
ingestion.target-chunk-chars=1600
ingestion.max-chunk-chars=2000
ingestion.overlap-chars=200
ingestion.chars-per-token=4
ingestion.embedding-batch-size=96
```

- Parça bölüm sınırlarında kesilir; hedef 1600, üst sınır 2000 karakter, komşu parçalar 200 karakter örtüşür.
- Parçanın başlığı (`heading`) ve sayfası saklanır; embed edilen metin başlık + içeriktir.
- `chars-per-token` sadece yaklaşık token sayısını göstermek içindir; model tokenizer'ının yerine geçmez.

## 4. Embedding (EMBEDDING)

- `EmbeddingClient` Ollama'nın `/api/embed` ucuna toplu (`ingestion.embedding-batch-size`) istek atar; model `bge-m3`, boyut 1024 (`embedding.model`, `embedding.dimensions`).
- Model etiketi `bge-m3@1024` çalışma alanı düzeyinde tektir; farklı modelle yükleme 409 `EMBEDDING_MODEL_MISMATCH`. Kolon boyutu (`vector(1024)`) yapılandırmadaki boyutla aynı olmalıdır.
- Embedding çağrısı transaction dışında yapılır; ağ hatasında (5xx) backoff ile retry `EmbeddingClient` seviyesindedir, kalıcı hatada iş `FAILED` + `EMBEDDING_UNAVAILABLE`.
- Aynı anda giden istek sayısı `Semaphore` ile sınırlanır; lokal model birden fazla paralel isteği yavaşlatır (`java-concurrency`).

## 5. Yazma (WRITING)

- Yeni sürümde eski parçalar silinip yenileri **tek transaction'da** yazılır; iş sürerken arama eski sürümü döndürür, commit anında yeni sürüme geçer. Arada parçasız bir an yoktur (lab: yükleme sürerken asistan eski rehberden cevap vermeye devam eder).
- `Chunk` id'si `SEQUENCE` (`allocationSize = 50`) ile üretilir; `hibernate.jdbc.batch_size=100` ile toplu insert yapılır (`IDENTITY` batch insert yapmaz).
- Kaynağın `version`, `chunkCount`, `syncedAt` alanları iş bitince güncellenir.

## 6. Arama

- `POST /api/workspaces/{workspaceId}/search` `{query, mode, limit}`; lab sadece `VECTOR` kullanır (`limit` 5 ya da 20).
- Sorgu embed edilir, cosine mesafe (`<=>`) ile HNSW index (`vector_cosine_ops`) üzerinde en yakın `limit` parça döner. `search.default-ef-search=40`, `search.max-scan-tuples=20000` (lab).
- Sadece aktif modelin satırları aranır; isabet parça kimliği, başlık, sayfa, içerik, sıra ve mesafeyi taşır. Vektörün kendisi yanıtta yoktur.
- SQL ve `EXPLAIN` kontrolü: `postgres-patterns`, `sql-optimization-patterns`.
- Log (lab): `Search completed workspaceId={} mode={} hitCount={} durationMs={}`; sorgu metni loglanmaz.

## 7. Test

- Gerçek embedding modeline gidilmez: sahte `EmbeddingClient` girdiden deterministik vektör üretir (aynı metin → aynı vektör); elle hesaplanmış küçük vektörlerle (`[1,0,0]`, `[0.8,0.6,0]`) sıralama doğrulanır.
- Repository ve arama sorguları Testcontainers ile gerçek pgvector imajına karşı test edilir (`spring-jpa-testing`); imaj deployment'takiyle aynıdır.
- Örnek test adları: `givenSourceWithActiveJob_whenUploadSourceVersion_thenThrowsIngestionInProgress`, `givenThreeChunks_whenFindNearest_thenReturnsChunksOrderedByCosineDistance`, `givenNewVersionWritten_whenSearchDuringIngestion_thenReturnsOldVersionChunks`.
- Ollama kapalıyken `./mvnw verify` geçer (ör. `EMBEDDING_BASE_URL=http://127.0.0.1:9 ./mvnw verify` kalıbı servisin `CLAUDE.md`'sinde).
