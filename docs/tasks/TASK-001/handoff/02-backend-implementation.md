TASK: TASK-001
# Backend uygulama raporu

## Kullanılan skill'ler
- `backend-conventions`, `springboot-patterns`, `problem-details-rfc9457` (zorunlu üçlü)
- Okunan (yüklenmeden, "Hangi iş → hangi skill" tablolarını ve hedef değerleri tutarlı yazmak için): `rag-pipeline` (bölüm 2-10), `rag-pipeline/references/indexing.md`, `sse-streaming` (bölüm 1-3)

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|------------------------|------------|-----|
| B-1 (K-5, K-8) | `assistant-api/CLAUDE.md` (yeni) | Bölüm yapısı eski dosyayla aynı: yorum bloğu, Teknoloji, Paket yapısı, Komutlar, Dosya sahipliği, Kurallar, Spring Boot 4 tuzakları, Hangi iş → hangi skill |
| B-1.1 | `assistant-api/CLAUDE.md` başlık + yorum | `# assistant-api (Spring Boot + Spring AI)`; "Durum: henüz kod yok; iskelet ilk assistant-api task'ında kurulur." |
| B-1.2 | `assistant-api/CLAUDE.md` Teknoloji | Eski maddeler korundu. "Vektörler bu repoda tutulursa …" → veritabanı `assistant`, vektör yok, M7 içeriği. Lokal modellerden embedding çıkarıldı (retrieval-api'nin). `RetrievalClient` maddesi lab anahtarlarıyla yazıldı (bkz. "Plandan sapmalar" 1). Referans satırı M5'e göre |
| B-1.3 | `assistant-api/CLAUDE.md` Paket yapısı | `backend-conventions` bölüm 3 ağacı; `retrieval/client/` (`RetrievalClient`, `RerankClient`), `retrieval/service/` |
| B-1.4 | `assistant-api/CLAUDE.md` Komutlar | "Hepsi `assistant-api/` içinde"; port 8090 (`server.port`); testler lokal modeller ve retrieval-api kapalıyken geçer; Testcontainers imajı `pgvector/pgvector:0.8.1-pg18` (Kararlar #4) |
| B-1.5 | `assistant-api/CLAUDE.md` Dosya sahipliği | Aynı tablo; migration'lar `src/main/**` içinde açıkça yazıldı; `docs/api/**` planner notu eklendi |
| B-1.6 | `assistant-api/CLAUDE.md` Kurallar | Sözleşme `/api/assistant/**` kısmı; `RetrievalClient` → `POST /api/workspaces/{workspaceId}/search` kısmı; SSE `docs/api/sse.md`; akış içi hata `error` olayı; tur kaydı maddesi |
| B-1.7 | `assistant-api/CLAUDE.md` Tuzaklar | Eski liste aynen (Spring AI maddesi dahil); YAML maddesine "lab'daki YAML örnekleri" eklendi |
| B-1.8 | `assistant-api/CLAUDE.md` Skill tablosu | Eski tablo + `rag-pipeline`, `sse-streaming` + `java-concurrency`, `rag-evaluation`; "JPA, PostgreSQL, pgvector" → "JPA, PostgreSQL, migration" |
| B-2 (K-5, K-8) | `retrieval-api/CLAUDE.md` (yeni) | B-1 yapısı + "Kapsam (lab)" bölümü (B-2.3) |
| B-2.1 | `retrieval-api/CLAUDE.md` başlık + yorum | `# retrieval-api (Spring Boot + pgvector)`; "Durum: henüz kod yok; iskelet ilk retrieval-api task'ında kurulur." |
| B-2.2 | `retrieval-api/CLAUDE.md` Teknoloji | Hepsi "Hedef:": Java 26/Boot 4.1/`./mvnw`; `com.rasitbank.retrieval`, `RetrievalApplication`; PostgreSQL 18 + pgvector 0.8.1 imajı, `hibernate-vector`, Flyway, `validate`, `open-in-view=false`, veritabanı `retrieval` (M7); embedding `embedding.base-url`/`model`/`dimensions` (Ollama, `bge-m3`, 1024); arama/içe alma ayarları lab değerleriyle (`indexing.md`); PDF kütüphanesi plan kararı; referans satırı M5; henüz olmayanlar: hepsi |
| B-2.3 (Kararlar #2, #7, #8, #11) | `retrieval-api/CLAUDE.md` Kapsam (lab) | M6 kelimesi kelimesine; kaynak 10001 ve sürümleri, içe alma aşamaları, `INGESTION_IN_PROGRESS`, `EMBEDDING_MODEL_MISMATCH`, parçalar, `VECTOR` arama; aranan sorgular saklanmaz ve embedding deneme ucu yok (bkz. "Plandan sapmalar" 3) |
| B-2.4 | `retrieval-api/CLAUDE.md` Paket yapısı | `backend-conventions` bölüm 3 retrieval-api paketleri (`workspace/` … `exception/`) ağaç olarak, özellik içi yapı notu |
| B-2.5 | `retrieval-api/CLAUDE.md` Komutlar | "Hepsi `retrieval-api/` içinde"; port 8080; `EMBEDDING_BASE_URL=http://127.0.0.1:9 ./mvnw verify` kalıbı; Docker açık olmalı (Testcontainers, gerçek pgvector imajı) |
| B-2.6 | `retrieval-api/CLAUDE.md` Dosya sahipliği | B-1 ile aynı |
| B-2.7 | `retrieval-api/CLAUDE.md` Kurallar | Sözleşme `/api/workspaces/**` kısmı; arama ucunun assistant-api'yi de etkilediği notu; 202 + `Location`; exception paketi `com.rasitbank.retrieval.exception`; lab'daki arama logu |
| B-2.8 | `retrieval-api/CLAUDE.md` Tuzaklar | B-1 listesi (Spring AI maddesi hariç) + `CREATE EXTENSION IF NOT EXISTS vector`, `Chunk` `SEQUENCE` (IDENTITY maddesine eklendi), vektör kolon boyutu = `embedding.dimensions` |
| B-2.9 | `retrieval-api/CLAUDE.md` Skill tablosu | `rag-pipeline/references/indexing.md` + `postgres-patterns` + `sql-optimization-patterns`; `java-concurrency` + `spring-transactions`; B-1'in ortak satırları (değerlendirme ve akış hariç) |

### Eski `backend/CLAUDE.md` → yeni dosyalar (L-16 için)
| Eski bölüm | assistant-api/CLAUDE.md | retrieval-api/CLAUDE.md |
|------------|-------------------------|-------------------------|
| Yorum bloğu (AMAÇ, içerik, "tekrar etme", Durum, güncelleme notu) | Yorum bloğu (servise göre; `rag-pipeline` atfı eklendi) | Yorum bloğu (servise göre; `indexing.md` atfı eklendi) |
| Başlık `# Backend (Spring Boot + Spring AI)` | `# assistant-api (Spring Boot + Spring AI)` | `# retrieval-api (Spring Boot + pgvector)` |
| Teknoloji: Java 26, Boot 4.1, `./mvnw` | Teknoloji madde 1 | Teknoloji madde 1 |
| Teknoloji: kök paket `com.rasitbank.assistant`, `AssistantApplication`, groupId/artifactId | Teknoloji madde 2 | Teknoloji madde 2 (`com.rasitbank.retrieval`, `RetrievalApplication`) |
| Teknoloji: Spring AI | Teknoloji madde 3 | — (assistant-api'nin; "Henüz olmayanlar"da Spring AI yok notu) |
| Teknoloji: PostgreSQL + Flyway, `validate`, `open-in-view`; "vektörler bu repoda tutulursa pgvector + `hibernate-vector`" | Teknoloji madde 4 (veritabanı `assistant`, vektör yok) | Teknoloji madde 3 (pgvector imajı, `hibernate-vector`, veritabanı `retrieval`) |
| Teknoloji: lokal modeller (Ollama `qwen3.5:9b` + `bge-m3`, reranker 8081), properties + ortam değişkeni | Teknoloji madde 5 (dil modeli + reranker) | Teknoloji madde 4 (embedding `bge-m3`) |
| Teknoloji: referans uygulama | Teknoloji "Referans uygulama" (M5) | Teknoloji "Referans uygulama" (M5) |
| Teknoloji: henüz olmayanlar | Teknoloji "Henüz olmayanlar" | Teknoloji "Henüz olmayanlar" |
| Paket yapısı: genel ağaç + katman sırası + prompts + testler | Paket yapısı (ayrıntılı assistant ağacı, prompts, testler) | Paket yapısı (retrieval ağacı, katman sırası, testler); prompts yok |
| Komutlar: derle, verify + coverage, Testcontainers/Docker | Komutlar | Komutlar |
| Komutlar: `-Djacoco.haltOnFailure=false` | Komutlar | Komutlar |
| Komutlar: PIT | Komutlar | Komutlar |
| Komutlar: tek test sınıfı/metot | Komutlar | Komutlar |
| Komutlar: `spring-boot:run` + health, portlar (8090 / 8080) | Komutlar (8090) | Komutlar (8080) |
| Komutlar: testler lokal modeller kapalıyken geçer | Komutlar (+ retrieval-api kapalı) | Komutlar (`EMBEDDING_BASE_URL` kalıbı) |
| Komutlar: tek test çalıştır, DONE öncesi verify | Komutlar son madde | Komutlar son madde |
| Dosya sahipliği tablosu + iki madde | Dosya sahipliği (+ `docs/api/**` notu) | Dosya sahipliği (+ `docs/api/**` notu) |
| Kurallar: `backend-conventions` bağlayıcı | Kurallar madde 1 | Kurallar madde 1 |
| Kurallar: sözleşme `openapi.yaml` + `sse.md` | Kurallar madde 2-3 (`/api/assistant/**`, `RetrievalClient`, SSE) | Kurallar madde 2-3 (`/api/workspaces/**`) |
| Kurallar: RFC 9457 + `errorCode` | Kurallar madde 4 | Kurallar madde 5 |
| Kurallar: Initializr tab → 4 boşluk | Kurallar son madde | Kurallar son madde |
| Spring Boot 4 tuzakları (10 madde) | Tuzaklar (10 madde, aynen) | Tuzaklar (Spring AI hariç 9 madde + 2 pgvector maddesi; IDENTITY maddesine `Chunk` `SEQUENCE` eklendi) |
| Hangi iş → hangi skill (14 satır) | Skill tablosu (eski satırlar + 3 yeni; SSE satırı `sse-streaming` ile güncellendi; JPA satırı "migration") | Skill tablosu (ortak satırlar + içe alma ve arka plan iş satırları; akış ve değerlendirme yok) |

Eski dosyanın karşılığı olmayan bölümü yok. Eski dosyaya dokunulmadı; silme L-16'da lead'in.

## Değişen dosyalar
- Yeni: `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md`, `handoff/02-backend-implementation.md`
- Değişen / silinen: yok (`backend/CLAUDE.md`'ye dokunulmadı)
- Eklenen dependency: yok

## Plandan sapmalar
1. **Ayar anahtarları (lead'in sapması, lead'in başlatma mesajıyla):** B-1.2'deki `assistant.retrieval.base-url` / `assistant.retrieval.workspace-id` yerine `rag-pipeline` bölüm 2'deki lab anahtarları "Hedef (lab)" diye yazıldı: `assistant.generation`, `assistant.retrieval` (`top-k`, `candidates`, `rerank`), `assistant.query-rewrite`, `assistant.retrieval-api.url` (hedef `http://localhost:8080`), `assistant.reranker.url` (hedef `http://localhost:8081`). Çalışma alanı 42'nin yapılandırmadan geldiği yazıldı, anahtar adı verilmedi; kesin adlar ilk assistant-api task'ının planında.
2. **`Source.ingesting(...)`:** CLAUDE.md'lerde factory örneği verilmedi; sapma gerekmedi.
3. **B-2.3 ifadesi:** Plan "Arama kaydı ve embedding deneme ucu yoktur" diyor; bu metin qa'nın K4 grep'ine (`arama kayd`) takılır. Anlam aynı kalarak "aranan sorgular retrieval-api'de saklanmaz ('hangi sorguyla hangi parça geldi' assistant-api'nin tur kaydındadır); embedding deneme ucu yoktur" diye yazıldı. Sadece ifade değişikliği; lead'e mesajla bildirildi.
4. **Küçük eklemeler (plan maddelerinin içinde):** iki dosyaya "Sözleşme dosyaları (`docs/api/**`) planner'ındır" notu; assistant-api'ye M4 özeti (ayarlar ekranda değil, sunucu yapılandırmasında) ve tur kaydı kuralı; retrieval-api'ye web'in Bilgi kaynakları ekranının uçları doğrudan çağırdığı (M2) ve lab arama logu. Hepsi kök `CLAUDE.md`, `backend-conventions` ve `rag-pipeline`'daki metinlerin kısa atfı; yeni karar yok.

## Doğrulama
- İskelet yok, komut çalıştırılmadı (`./mvnw`, `npm`, `docker` yok); kontroller qa'da.
- Ön kontrol olarak qa'nın grep'leri sadece iki yeni dosyada çalıştırıldı:
  - K3: `embedding-pgvector` sadece iki "Referans uygulama" satırında (M5'e uygun).
  - K4, K5, K19: 0 satır.
  - K7: `tek bir çalışma alanı vardır` → `retrieval-api/CLAUDE.md`'de var; yasaklı ifadeler 0.
  - K12: 11434 Ollama, 8081 reranker, 8080 retrieval-api, 8090 assistant-api (M3 anlamında).
  - K13: `rag-pipeline` 2 dosya, `sse-streaming` ve `rag-evaluation` 1 dosya (assistant-api; diğer dosyalarla birlikte qa sayar).

## Tester için notlar
Bu task'ta tester yok (Kural 11). İleride kullanılacak seam'ler dosyalara yazıldı: assistant-api'de sahte `ChatModel`, `RetrievalClient`, `RerankClient`; retrieval-api'de sahte `EmbeddingClient` ve `EMBEDDING_BASE_URL=http://127.0.0.1:9` kalıbı; iki serviste Testcontainers `pgvector/pgvector:0.8.1-pg18`.

STATUS: DONE
