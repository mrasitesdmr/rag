TASK: TASK-001
# Plan

## Özet
**Revizyon 1 (kullanıcı kararları, lead aracılığıyla):**
- (1) Repo lab'daki servis yapısını izler. Modüller `web/`, `assistant-api/`, `retrieval-api/` ve `deployment/` olur.
- Bilgi bankası (kaynak, sürüm, parça, içe alma, embedding, vektör arama) bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılır. `../embedding-pgvector` çalışma zamanı bağımlılığı değildir; en fazla okunur kalıp referansıdır.
- (2) Üç yeni skill eklenir: `rag-pipeline`, `sse-streaming`, `rag-evaluation`.
- (3) Canlı yapılandırma sunucu ayarıdır; ekranda düğme olmaz.
- Önceki plan (retrieval-api ayrı servis, `backend/` + `frontend/`) bu dosyada tamamen değiştirildi.

**Mevcut durum:** Ekip altyapısı dosya listesi olarak referansla aynı (39 skill, 6 agent, `settings.json`). Ama:
- Modüller `backend/` ve `frontend/` adını taşıyor.
- Skill'lerin çoğunda referans projenin işine özgü kurallar ve metinler duruyor: kaynak limiti, kaynak adı tekliği, arama kayıtları, sık aranan sorgular, embedding deneme ucu, çok çalışma alanı, `/w/42/...` adresleri, "Dalga Fiber", "Fatura SSS", "Yıllık izin kaç gün?".
- Ürün hedefi hiçbir yerde yazılı değil.
- Agent tanımlarında kök `CLAUDE.md` ile üç çelişki var.

**Yeni durum:**
- Kök `CLAUDE.md` rol tablosu, agent tanımları, izinler, skill'ler, kılavuz ve şablon yeni modül adlarıyla yazılı.
- Yeni modüllerde sadece `CLAUDE.md` var: `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md`, `web/CLAUDE.md`. `backend/` ve `frontend/` kalktı.
- Kaynak, parça ve içe alma örnekleri artık bu projenin `retrieval-api`'sini anlatıyor; referansa özgü kurallar ve metinler çıkarıldı.
- Konuşma, tur, alıntı ve değerlendirme örnekleri `assistant-api`'yi anlatıyor.
- Ürün hedefi ve modül yapısı her dosyada aynı metinle geçiyor.

**Uygulama sırası:**
1. Kullanıcı onayı.
2. **lead**: sahipsiz dosyalar (L-01…L-15). Rol tablosu ve agent tanımları önce değişir; coder'lar yeni sahiplikle çalışır.
3. **backend-coder** (B-1 `assistant-api/CLAUDE.md`, B-2 `retrieval-api/CLAUDE.md`) ve **frontend-coder** (F-1 `web/CLAUDE.md`) paralel çalışır. Yeni dosyaları oluştururlar, eski dosyalara dokunmazlar.
4. **lead** L-16 (eski `backend/`, `frontend/` klasörlerini kaldırır) ve **devops** (D-1 `deployment/CLAUDE.md`, D-2 `README.md`) paralel çalışır. İkisi de coder raporlarının `DONE` olmasını bekler.
5. **qa**: K1-K20.

## Çalışacak roller
- planner: bu plan + `docs/api/openapi.yaml` iskeleti.
- lead: sahipsiz dosyalar (`.claude/**`, kök `CLAUDE.md`, kök `.gitignore`, `rag.md`, `KULLANIM-KILAVUZU.md`, `docs/tasks/_TEMPLATE.md`) ve eski klasörlerin kaldırılması (L-16). Kural 10 gereği.
- backend-coder: B-1, B-2.
- frontend-coder: F-1.
- devops: D-1, D-2. Docker dosyası yazmaz, Docker komutu çalıştırmaz.
- qa: K1-K20.
- **Atlanan: tester.** Kural 11 gereği bu task'ta test yazılmaz. Kök `CLAUDE.md` madde 2 gereği devops coder'lara (3a, 3b) bağlanır.

Lead'in başlatma mesajına ekleyeceği not: coder'lar ve devops kod, test ya da Docker dosyası yazmaz; `./mvnw`, `npm` ve `docker` komutu çalıştırmaz (iskelet yok). Raporun "Doğrulama" bölümüne "iskelet yok, komut çalıştırılmadı; kontroller qa'da" yazarlar. Ortak metinler (M1-M7) kelimesi kelimesine kullanılır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi kontrolle doğrulanır |
|---|-----------------|------------------|----------------------------|
| K-1 | Kapsamdaki dosyalar | "Dosya envanteri" + L/B/F/D maddeleri | K1, K2 |
| K-2 | Eksiksizlik; bilerek alınmayanlar gerekçeli; task geçmişi taşınmaz | "Dosya envanteri", kart "Benim verdiğim kararlar" | K1, K2 |
| K-3 | Projeye özgülük (referans işi kalmaz, genel teknik içerik korunur) | Ç-1, Ç-2 + L-12 | K4, K5, K15, K19, K20 |
| K-4 | `../embedding-pgvector` atıfları kaldırılır ya da gerekçeli kalır | M5 | K3 |
| K-5 | Retrieval kararı kartta sorulur, dokümanlara işlenir | Kullanıcı kararı 1 → M2, M3, M6, M7 → L-01, L-02, L-12.1, B-1, B-2, F-1, D-1 | K7, K12 |
| K-6 | Ürün hedefi `rag.md`, kök `CLAUDE.md`, `web/CLAUDE.md` (eski `frontend/CLAUDE.md`) ve ilgili skill'lerde | M1 (+M4) → L-01, L-02, L-12.2-L-12.4, L-08, F-1 | K6 |
| K-7 | Orkestrasyon aynı; SSE, lokal modeller, `ollama.md`, reranker tutarlı | L-01 (sadece yol sütunları değişir), L-06…L-11 | K10, K11, K13 |
| K-8 | Tutarlılık: yol, komut, port, dosya adı, sahiplik | M3, Ç-1, L-01 tablosu | K10, K11, K12, K14, K19 |
| K-9 | Eksik skill önerilir | Kullanıcı kararı 2 → L-13…L-15 | K1, K8, K13 |
| K-10 | Uygulayan: sahipsiz dosyalar lead, sahipli dosyalar sahibi rol | "Çalışacak roller", madde kodları; L-16 gerekçesi (Kararlar #5) | K17 |
| K-11 | Kod, Docker, test yok; model indirilmez | Rollere not; Deployment planı "Yok" | K18 |
| K-12 | Dil kuralı | L-12, L-13…L-15 | K16 |
| S-1 | Planner kartı `rag.md`/lab/sözlüğe göre yazar | L-06, Ç-2 | K4, K5 |
| S-2 | frontend-coder sol menü ekranlarını ve konsolun gelmediğini görür | L-12.2 (M1) | K6 |
| S-3 | backend-coder sözlükte conversation, turn, citation, feedback, evaluation, SSE ve loglama sınırını bulur | `backend-conventions` bölüm 1, 4, 7 (mevcut) + L-12.1 | K16 (manuel) |
| S-4 | `grep -rn "embedding-pgvector" .claude CLAUDE.md */CLAUDE.md` sadece gerekçeli atıfları gösterir | M5 | K3 |
| S-5 | Kullanıcı kılavuzla TASK-002'yi açabilir | L-03 | K14 |
| AC-1 | Karşılaştırmalı envanter | Kartta özet; tam tablo bu plandaki "Dosya envanteri" | qa kartı okur |
| AC-2 | Retrieval kararı verilmiş ve işlenmiş | Kullanıcı kararı 1 | K7 |
| AC-3 | Referans kavram/ekran/port/yol kalmamış | K-3, K-4 | K3, K4, K5, K12, K19, K20 |
| AC-4 | Ürün hedefi yazılı | K-6 | K6 |
| AC-5 | Frontmatter, açıklamalar, `settings.json` | L-05, L-12, L-13…L-15 | K8, K9, K15 |
| AC-6 | Agent yolları ve handoff adları kök tabloyla aynı | L-01, L-06…L-11 | K10, K11 |
| AC-7 | qa bağımsız kontrol edip `DONE` verir | qa adımı | K1-K20 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan kontrol |
|---|-------|-------------------|--------------------|
| G1 | Eski yol adları (`backend/`, `frontend/`, `cd backend`) bir skill'de, agent'ta ya da izinde kalır; rol yanlış klasöre yazar | Ç-1 her dosyada uygulanır; izinler yeni yollara çevrilir | K9, K19 |
| G2 | Eski `backend/CLAUDE.md` silinmeden yeni dosyalar oluşur ve iki kopya çelişir | L-16: coder raporları `DONE` olunca lead eskileri kaldırır | K18 |
| G3 | `../embedding-pgvector` ile bu projenin sistemi aynı portlarla aynı anda açılır | `deployment/CLAUDE.md` ve `README.md` "aynı anda açılmaz" der | K12 |
| G4 | Coder/devops iskelet yokken build ya da docker komutu çalıştırır; tester atlanınca devops test raporu bekler | Agent notları (L-07, L-08, L-10) | K11 |
| G5 | `lab.html` aramasının `rag-lab.html`'i yakalaması; genel anlamdaki "workspace" (monorepo kökü, debugging örneği) | K4 regex'i `(^|[^-])lab\.html`; K5 sadece referansa özgü workspace kalıplarını arar | K4, K5 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | Modüller: `web/` (React), `assistant-api/` (Spring Boot + Spring AI), `retrieval-api/` (Spring Boot + pgvector), `deployment/`. İki servis ayrı, bağımsız Maven projesidir (her birinde `mvnw`); ortak Maven modülü ya da paylaşılan kütüphane yoktur, ortak küçük sınıflar (`PageResponse`, exception kategori sınıfları) her serviste kendi paketindedir | Kullanıcı kararı; lab'daki iki ayrı servis; bağımsız build ve deploy | Maven multi-module (parent POM): iki servis birbirine bağlanır, rol tablosuna yeni bir ortak yol ve sahip gerekir |
| 2 | **Çalışma alanı retrieval-api'de korunur** ama tek ve sabittir (M6): 42, "Raşit Bankası · Dijital Asistan", migration ile gelir; oluşturma, listeleme ve seçme yoktur; ekran adreslerinde yoktur | Lab'ın sol yarısı Bilgi kaynakları ekranında "Çalışma alanı **Raşit Bankası · Dijital Asistan** (ws 42) · embedding modeli bge-m3@1024" bilgi kutusunu gösteriyor ("aynısı" kuralı); lab'ın istekleri `/api/workspaces/42/...` | Kavramı tamamen kaldırmak (`/api/sources/...`): lab'daki bilgi kutusu ve adresler karşılıksız kalır. Referanstaki gibi çok çalışma alanı: lab'da yok, referansın işi (TASK Kural 3) |
| 3 | Sözleşme tek dosyada kalır: `docs/api/openapi.yaml`, path önekiyle iki servis (`/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api) | `web` ikisini aynı origin'den çağırıyor; assistant-api'nin retrieval-api çağrısı da aynı sözleşmeye dayanıyor; ~40 dosyadaki `docs/api/openapi.yaml` adı değişmiyor | İki dosya (`assistant-api.yaml`, `retrieval-api.yaml`): her rolün ve skill'in sözleşme yolu değişir, iki servis arası çağrı iki dosyaya bölünür |
| 4 | Veritabanı: tek PostgreSQL sunucusu (pgvector'lü imaj), iki ayrı veritabanı (`assistant`, `retrieval`), her biri ayrı kullanıcı ve ayrı Flyway geçmişiyle; servisler birbirinin veritabanına bağlanmaz. Testcontainers'ta iki servis de aynı pgvector imajını kullanır | Lab iki ayrı veritabanı gösteriyor (`postgres` ve `assistant-db`); tek sunucu 16 GB makinede bellek ve tek healthcheck demek; ayrı kullanıcı ayrılığı korur | İki PostgreSQL container'ı (5432 + 5433): fazladan bellek ve port, ayrılığa bir şey katmaz |
| 5 | Eski `backend/CLAUDE.md` ve `frontend/CLAUDE.md`'nin içeriğini yeni dosyalara coder'lar taşır (B-1, B-2, F-1); eski klasörleri **lead** kaldırır (L-16) | L-01 sonrası yeni rol tablosunda `backend/` ve `frontend/` kimsenin değil (sahipsiz → lead). Coder'lar yeni dosyaları yazar, eskiye dokunmaz; böylece hiçbir rol tablosu dışında bir yola yazmaz | Coder'ın eski dosyayı silmesi: yeni tabloda o yol coder'ın değil |
| 6 | Kaynak, parça, içe alma ve arama örnekleri skill'lerde **kalır** ve `retrieval-api`'yi anlatır; sadece referansa özgü kurallar ve metinler (Ç-2) değişir | Bu artık bu projenin işi (TASK Kural 3: "bilgi kaynakları ve sürümleri"); kalıplar doğru, gereksiz yeniden yazım hata riski | Bütün örnekleri konuşma/tur'a çevirmek: retrieval-api'nin kalıpları skill'lerden kaybolur |
| 7 | Referans kurallarının yerine lab'dan iki kural örnek olur: "bir kaynağın aynı anda tek aktif içe alma işi → 409 `INGESTION_IN_PROGRESS`" (lab: yükleme sürerken "Yeni sürüm yükle" kapalı) ve "farklı embedding modeliyle yükleme → 409 `EMBEDDING_MODEL_MISMATCH`" (lab bilgi kutusu) | Lab'da görünen davranış; referanstaki kaynak limiti ve ad tekliği lab'da yok | — |
| 8 | Arama kaydı (`search_events`) retrieval-api'de yok | Lab'ın veritabanı haritasında tablo görünüyor ama sağ yarıda; sol yarıda arama kaydı ekranı yok ve TASK Kural 3 bunu referansın işi sayıyor; "hangi sorguyla hangi parça geldi" assistant-api'nin tur kaydında | Tabloyu tutmak: kullanılmayan veri, TASK'la çelişki |
| 9 | İçe alma ve embedding `rag-pipeline` skill'inin `references/indexing.md` dosyasında anlatılır (indeksleme zamanı, retrieval-api); `SKILL.md` sorgu zamanını (assistant-api) anlatır | `kaynaklar/rag.html` RAG hattını "indexing ve sorgu zamanı" diye iki parça anlatıyor; dördüncü bir skill kullanıcı onayı gerektirir; pgvector SQL'i zaten `postgres-patterns`'ta | Dördüncü skill (`rag-ingestion`): yeni onay sorusu; içerik `postgres-patterns` ve `java-concurrency` ile çakışır |
| 10 | Kök paketler: `com.rasitbank.assistant` (`AssistantApplication`), `com.rasitbank.retrieval` (`RetrievalApplication`) | Lab logları `c.r.assistant.*`; retrieval yeniden yazıldığı için referansın `com.embedding.pgvector`'u kullanılmaz | — |
| 11 | Arama modu: lab sadece `VECTOR` kullanır; `KEYWORD`/`HYBRID` kalıpları skill'lerde genel teknik içerik olarak kalır, retrieval-api'de olup olmayacağına ilgili task karar verir | Lab: `{query, mode: 'VECTOR', limit}` | — |
| 12 | `devops.md` yazma listesine `ollama.md` ve kök `README.md` eklenir; tester atlanınca devops coder raporlarına bağlanır; coder/devops iskelet yokken komut çalıştırmaz | Kök tablo ve madde 2 ile agent'lar arasındaki çelişkiler (önceki planda da vardı) | — |
| 13 | `settings.json`: `Edit(/backend/**)`, `Edit(/frontend/**)` → `Edit(/assistant-api/**)`, `Edit(/retrieval-api/**)`, `Edit(/web/**)`; eklenir: `Bash(curl -sN localhost:*)`, `Edit(/ollama.md)`, `Edit(/README.md)` | Yol bazlı izinler yeni modüllere uymalı; devops'un SSE smoke testi ve doküman düzenlemesi | — |
| 14 | `.nojekyll` alınmaz; `scratch/` `.gitignore` satırı olarak kalır; `ollama.md` ilk model task'ında devops yazar; kök `.gitignore`'daki modül yolları yeni adlara çevrilir (lead) | `.nojekyll` GitHub Pages içindi, bu projede yayın yok; `.gitignore` sahipsizdir ve eski yollar artık yanlış | — |
| 15 | `docs/api/openapi.yaml` iskeletine ortak hata şeması ve `Page`/`Size` eklendi, başlık yorumu iki servisi anlatır; `paths: {}` | İki servisin de kullanacağı ortak parça; TASK "sadece iskelet" | — |

## Sözleşme değişiklikleri
- `docs/api/openapi.yaml`:
  - Başlık yorumu: dosya iki servisin sözleşmesidir (`/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api); uyum kontrolü her servisin kendi önekiyle yapılır.
  - `components` eklendi: `parameters.Page`, `parameters.Size`, `responses.ValidationFailed`, `responses.InternalError`, `schemas.ProblemDetail` (framework `errorCode`'ları: `VALIDATION_FAILED`, `BAD_REQUEST`, `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`) ve `schemas.Violation`.
  - `paths: {}`.
- `docs/api/sse.md` ve `docs/api/websocket.md` oluşturulmadı (kapsam dışı).

## Ortak metinler (kelimesi kelimesine kullanılır)

### M1 – Ürün hedefi
> Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. "Aynısı" şu demektir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder.

### M2 – Modül yapısı ve bilgi bankası
> Repo lab'daki servis yapısını izler: `web/` (React yönetim paneli), `assistant-api/` (Spring Boot + Spring AI: konuşma, sorgu yeniden yazma, retrieval çağrısı, rerank, prompt, cevap akışı, geri bildirim, temsilciye aktarma, değerlendirme), `retrieval-api/` (Spring Boot + pgvector: bilgi kaynakları, sürümler, içe alma, parçalama, embedding, vektör arama) ve `deployment/`. Bilgi bankası **bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılır**. `../embedding-pgvector` çalışma zamanı bağımlılığı değildir; sadece okunur kalıp referansıdır: kodu toplu kopyalanmaz ve oraya yazılmaz. `assistant-api`, `retrieval-api`'ye REST ile gider (`POST /api/workspaces/{workspaceId}/search`). `web`'in Bilgi kaynakları ekranı `retrieval-api`'nin kaynak, sürüm, parça ve içe alma uçlarını aynı adres üzerinden (dev'de Vite proxy, Docker'da nginx) doğrudan çağırır (lab'daki gibi). İki servisin sözleşmesi `docs/api/openapi.yaml`'dadır; veritabanları ayrıdır.

### M3 – Servisler ve portlar (hedef; ilk Docker task'ında devops kesinleştirir)
| Servis | Görevi | Host adresi |
|--------|--------|-------------|
| `web` | nginx: SPA; `/api/assistant/` → `assistant-api:8090`, `/api/workspaces/` → `retrieval-api:8080` | `3000` |
| `assistant-api` | Konuşma, sorgu yeniden yazma, retrieval çağrısı, rerank, prompt, cevap akışı (SSE), geri bildirim, aktarma, değerlendirme | `8090` |
| `retrieval-api` | Kaynak, sürüm, içe alma, parçalama, embedding, vektör arama | `8080` |
| `postgres` | pgvector'lü PostgreSQL (lab: `pgvector/pgvector:0.8.1-pg18`); iki veritabanı: `assistant` (assistant-api) ve `retrieval` (retrieval-api), ayrı kullanıcılarla | `127.0.0.1:5432` |
| `pgadmin` | Veritabanı arayüzü | `127.0.0.1:5050` |
| Ollama (Mac'te) | Dil modeli `qwen3.5:9b` (assistant-api), embedding `bge-m3` 1024 boyut (retrieval-api) (lab; kesin modeller kullanıcı onayıyla) | `11434` |
| Reranker, llama-server (Mac'te) | `POST /v1/rerank`, `bge-reranker-v2-m3` (lab) | `8081` |

Geliştirme: `cd assistant-api && ./mvnw spring-boot:run` → 8090, `cd retrieval-api && ./mvnw spring-boot:run` → 8080, `cd web && npm run dev` → 5173 (Vite proxy: `/api/assistant` → `http://localhost:8090`, `/api/workspaces` → `http://localhost:8080`). `../embedding-pgvector`'un compose'u aynı portları (5432, 5050, 8080, 3000) kullanır; iki proje aynı anda açılmaz.

### M4 – Canlı yapılandırma (kullanıcı kararı 3)
> Lab'da üretim modu (Sadece LLM / RAG), retrieval (top-5 / rerank 20→5), takip sorusunu yeniden yazma ve reranker durumu sağ yarıdaki düğmelerle değişir. Sağ yarı gelmediği için bu ayarlar ekrandan değiştirilmez; `assistant-api` yapılandırmasıdır (`assistant.*`, ortam değişkeniyle). Başlıktaki "Canlı yapılandırma" rozeti lab'daki gibi kalır; Değerlendirme'deki ölçüm düğmesi etkin stratejiyi lab'daki gibi gösterir ("Ölçümü çalıştır · rerank 20→5"), değeri backend verir.

### M5 – İzinli `../embedding-pgvector` atıfları (bunların dışındaki her atıf kaldırılır)
| Dosya | Atıf | Gerekçe |
|-------|------|---------|
| kök `CLAUDE.md` | Proje özeti ("devamıdır"), M2, Klasör yapısı satırı, sahipsiz dosyalar listesi | Kökeni ve okunur kalıp referansı; yazma yasağı |
| `rag.md` | Amaç, M2 | Aynı |
| `retrieval-api/CLAUDE.md` | "Referans uygulama" satırı | İçe alma, parçalama, pgvector kalıpları için okunur; toplu kopya yok |
| `assistant-api/CLAUDE.md` | "Referans uygulama" satırı | Aynı yığının sürümleri ve test/kalite araçları |
| `web/CLAUDE.md` | "Referans uygulama" satırı | Bilgi kaynakları, yükleme ve içe alma ekranlarının çalışan örneği |
| `deployment/CLAUDE.md` | Port çakışması; "devops yazmaz" | İki projenin aynı makinede çalışması |
| `KULLANIM-KILAVUZU.md`, `README.md` | Yok | Çalışma zamanı bağımlılığı değil |
| `docs/tasks/_TEMPLATE.md`, `docs/api/**` | Yok | — |
| `.claude/agents/planner.md`, `backend-coder.md`, `frontend-coder.md` | "Gerekirse referans" satırı (okunur, değiştirilmez) | Kalıp referansı |
| `backend-conventions` | Durum notu (M2 cümlesi) | Kalıp referansı sınırı |
| `frontend-conventions` | Referans notu; bölüm 8 polling referansı | Aynı ekranın çalışan kodu |
| `e2e-testing` | Yükleme/ilerleme örneklerinin kaynağı | Aynı akışın çalışan testi |
| `react-state-management/references/details.md` | `sourceQueries.ts` kalıbı | Polling'in çalışan örneği |
| `rag-pipeline/references/indexing.md` (yeni) | İçe alma hattının çalışan örneği | Okunur kalıp |

### M6 – Çalışma alanı
> retrieval-api'de tek bir çalışma alanı vardır: 42, "Raşit Bankası · Dijital Asistan". Migration ile gelir; oluşturma, listeleme ya da seçme ekranı ve ucu yoktur. Adreslerde (`/api/workspaces/42/...`) ve Bilgi kaynakları ekranındaki bilgi kutusunda adıyla görünür (lab); ekran adreslerinde (`/kaynaklar/...`) yoktur. Kaynaklar, parçalar ve içe alma işleri çalışma alanına bağlıdır; embedding modeli çalışma alanı düzeyinde tektir (`bge-m3@1024`).

### M7 – Veritabanları
> Tek PostgreSQL sunucusu (pgvector'lü imaj), iki ayrı veritabanı: `assistant` (assistant-api: konuşmalar, turlar, değerlendirme çalıştırmaları) ve `retrieval` (retrieval-api: çalışma alanı, kaynaklar, parçalar ve embedding'leri, içe alma işleri). Her servis kendi kullanıcısıyla sadece kendi veritabanına bağlanır ve kendi Flyway migration'larını taşır; servisler birbirinin tablosunu okumaz, REST ile konuşur. Vektörler sadece `retrieval` veritabanındadır.

## Çeviri sözlüğü

### Ç-1 Yollar, adlar, portlar (her dosyada)
| Eski | Yeni |
|------|------|
| `backend/` (genel: "backend/ altında Java kodu") | "`assistant-api/` ya da `retrieval-api/` altında" |
| `backend/` (konuşma, model, akış, değerlendirme bağlamı) | `assistant-api/` |
| `backend/` (kaynak, parça, içe alma, embedding, arama bağlamı) | `retrieval-api/` |
| `frontend/` (`frontend/e2e`, `frontend/src`, `frontend/nginx.conf` …) | `web/` |
| `cd backend && ./mvnw verify` | `cd assistant-api && ./mvnw verify`, `cd retrieval-api && ./mvnw verify` (değişen servis için) |
| `cd frontend && npm …` | `cd web && npm …` |
| `backend/CLAUDE.md`, `frontend/CLAUDE.md` | ilgili servisin `CLAUDE.md`'si, `web/CLAUDE.md` |
| compose servisleri `backend`, `frontend` | `assistant-api`, `retrieval-api`, `web` |
| Paket `com.rasitbank.assistant.{workspace,source,chunk,ingestion,search,embedding}` | `com.rasitbank.retrieval.…` |
| `com.rasitbank.assistant.exception` (şablonlar) | Kalır; not: "retrieval-api'de `com.rasitbank.retrieval.exception`" |
| Port 8080 "backend" | 8080 retrieval-api; 8090 assistant-api |

Rol adları (`backend-coder`, `frontend-coder`) ve skill adları (`backend-conventions`, `frontend-conventions`) değişmez (Kural 7).

### Ç-2 Referansa özgü iş → bu proje
| Referans projede | Bu projede |
|------------------|-----------|
| Çok çalışma alanı: oluşturma/listeleme ucu, `WorkspaceSelect`, `WorkspaceLayout`, `workspaceKeys`, `/w/:workspaceId/...` ekran adresleri, `WorkspaceHomeRedirect` | Kalkar; tek çalışma alanı (M6). Web'de `workspaceId` parametresi ve prop'u kalkar, adres `/api/workspaces/42/...` |
| Kaynak limiti (`MAX_SOURCES_PER_WORKSPACE`, `SOURCE_LIMIT_REACHED`, "11. kaynak") ve kaynak adı tekliği (`SOURCE_NAME_TAKEN`, `uk_sources_workspace_id_name`, `name_key`, `toNameKey`), `Workspace.addSource` kuralı, çalışma alanı satırının kilitlenmesi | Kararlar #7: `source.startIngestion(version, now)` → `IngestionJob`; aktif iş varsa 409 `INGESTION_IN_PROGRESS`; kilit `SourceRepository.findByWorkspaceIdAndIdForUpdate`; farklı modelle yükleme 409 `EMBEDDING_MODEL_MISMATCH`. Kaynak oluşturma (`createSource`) retrieval-api yeteneği olarak kalır, ama limit ve ad kontrolü olmadan |
| `SearchEvent`, `search_events`, arama kayıtları, göstergeler (stats), sık aranan (popular queries) | Kalkar (Kararlar #8); "hangi sorguyla hangi parça" → assistant-api'nin tur kaydı |
| Embedding deneme ucu `/api/embeddings`, `EmbedTextRequest`, `EmbeddingResponse` | Kalkar; embedding sadece içe almada ve aramada (`EmbeddingClient` → Ollama `/api/embed`) |
| "embedding API anahtarı", "ücretli embedding API" | "lokal embedding modeli (Ollama `bge-m3`), anahtar yok"; gizli bilgi yok, lab'daki `Authorization` başlığı bankanın kimlik sisteminin temsilidir |
| Metinler: "Dalga Fiber", "Belgelerim" | Çalışma alanı "Raşit Bankası · Dijital Asistan" |
| "Fatura SSS", "Yardım merkezi", Atlaskap PDF, `ornek-dokuman/` | Kaynak 10001 "Raşit Bankası Ürün ve Hizmet Rehberi" (PDF; sürüm 2026.10, yeni sürüm 2026.11) |
| "Yıllık izin kaç gün?", "Şifremi unuttum" | "Gold kartın yıllık aidatı ne kadar?", "Eşime ek kart çıkarmak ücretli mi?"; bulunamayan cevapta sabit cümle "Bu konuda rehberimizde bilgi bulamadım." |
| Kimlikler: kaynak 301 | 10001 (lab) |
| Web: `/w/42/arama`, `/w/:workspaceId/arama-kayitlari`, `/w/42/kaynaklar/301`, `belge-yukle` | `/onizleme`, `/konusmalar`, `/kaynaklar/10001`, kaynak detayındaki "Yeni sürüm yükle" diyaloğu |
| Web: `SearchForm`, `useSearchChunksMutation`, `CreateSourceForm` | `MessageComposer`, `useAnswerStream`, `UploadSourceVersionDialog` |
| Web query key'leri `sourceKeys.*(workspaceId, …)` | `sourceKeys.list()`, `sourceKeys.detail(sourceId)`, `sourceKeys.ingestionJob(jobId)` |
| Güvenlik: "URL'deki workspaceId'yi değiştirip başka kiracının kaynakları", "içe alınan web sayfasında `<img onerror>`" | "URL'deki `conversationId`/`sourceId`'yi değiştirmek", "rehber parçasında ya da model cevabında `<img onerror>` / talimat gibi görünen metin (prompt injection)" |
| Eşzamanlılık: "11 kaynağın 10 limitli çalışma alanına eklenmesi" | "aynı kaynağa aynı anda iki yeni sürüm (`INGESTION_IN_PROGRESS`)", "aynı konuşmaya aynı anda iki soru (`TURN_IN_PROGRESS`)", "aynı tura iki geri bildirim (`FEEDBACK_ALREADY_SUBMITTED`)" |
| Konuşma tarafı örneği gereken yerde | assistant-api: `Conversation` (`addTurn`), `Turn` (`status` `STREAMING`/`COMPLETED`/`INTERRUPTED`, `generationMode`, `retrievalStrategy`, `searchQuery`, `chunkIds`, `cited`, `promptTokens`, `completionTokens`, `latencyMs`, `helpful`), `EvalRun` (`POST /api/assistant/evals` → 202 + `Location`); kodlar `CONVERSATION_NOT_FOUND`, `TURN_NOT_FOUND`, `EVAL_RUN_NOT_FOUND`, `TURN_IN_PROGRESS`, `FEEDBACK_ALREADY_SUBMITTED`, `HANDOFF_ALREADY_REQUESTED`, `CHAT_MODEL_UNAVAILABLE`, `RETRIEVAL_UNAVAILABLE`, `RERANKER_UNAVAILABLE` (hepsi örnek; kesin liste sözleşmede) |

Değişmeyen ve geçerli kalanlar (retrieval-api): `Workspace` (tek), `Source`, `SourceKind`, sürüm, `Chunk` + `vector(1024)` + HNSW, `IngestionJob` ve aşamaları, `searchChunks`, `POST /api/workspaces/{workspaceId}/search`, log `Search completed workspaceId={} mode={} …` (lab'daki retrieval logu), 202 + `Location` + polling, kodlar `WORKSPACE_NOT_FOUND`, `SOURCE_NOT_FOUND`, `CHUNK_NOT_FOUND`, `INGESTION_JOB_NOT_FOUND`, `INGESTION_IN_PROGRESS`, `EMBEDDING_UNAVAILABLE`, `EMBEDDING_MODEL_MISMATCH`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE` (örnek).

## Dosya envanteri (referans → bu proje → durum)
Durum değerleri: **aynı** · **uyarlandı** (değişiklik yok) · **değişecek** (madde) · **taşınacak** · **yeni** · **bilerek alınmadı** (gerekçe).

### Kök ve modüller
| Referans | Bu proje | Durum |
|----------|----------|-------|
| `CLAUDE.md` | `CLAUDE.md` | değişecek → L-01 |
| `embedding-pgvector.md` | `rag.md` | değişecek → L-02 |
| `KULLANIM-KILAVUZU.md` | aynı ad | değişecek → L-03 |
| `docs/tasks/_TEMPLATE.md` | aynı | değişecek → L-04 |
| `.gitignore` | aynı | değişecek → L-04b (modül yolları) |
| `README.md` | aynı | değişecek → D-2 |
| `backend/CLAUDE.md` | `backend/CLAUDE.md` → `assistant-api/CLAUDE.md` + `retrieval-api/CLAUDE.md` | taşınacak → B-1, B-2; eski dosya L-16 |
| `frontend/CLAUDE.md` | `frontend/CLAUDE.md` → `web/CLAUDE.md` | taşınacak → F-1; eski dosya L-16 |
| `deployment/CLAUDE.md` | aynı | değişecek → D-1 |
| `docs/api/openapi.yaml` | aynı | uyarlandı (bu planla iskelet ve iki servis başlığı) |
| `handoff/.gitkeep`, `docs/tasks/.gitkeep` | var | aynı |
| `scratch/` | `.gitignore` satırı | aynı (klasör gerekmez) |
| `ollama.md` | — | bilerek alınmadı: kapsam dışı; ilk model task'ında devops yazar |
| `.nojekyll` | — | bilerek alınmadı: Kararlar #14 |
| Compose dosyaları, `deployment/pgadmin/servers.json`, Dockerfile'lar, `nginx.conf`, `.dockerignore`'lar | — | bilerek alınmadı: Kural 11 |
| Referansın kodu, `ornek-dokuman/`, README'deki ekran kayıtları | — | bilerek alınmadı: referansın içeriği (kod retrieval-api'de yeniden yazılacak) |
| `docs/tasks/TASK-001…/**` | — | bilerek alınmadı: Kural 2 |
| `.claude/settings.json` | aynı | değişecek → L-05 |

### Agent'lar
`planner.md` → L-06, `backend-coder.md` → L-07, `frontend-coder.md` → L-08, `tester.md` → L-09, `devops.md` → L-10, `qa.md` → L-11 (hepsi değişecek).

### Skill'ler (39 klasör; dosya adları referansla birebir aynı)
| Skill (dosya sayısı) | Durum | Madde |
|----------------------|-------|-------|
| `backend-conventions` (1) | değişecek | L-12.1 |
| `frontend-conventions` (1) | değişecek | L-12.2 |
| `frontend-design` (1) | değişecek | L-12.3 |
| `e2e-testing` (1) | değişecek | L-12.4 |
| `problem-details-rfc9457` (SKILL + 2 examples + 7 templates = 10) | SKILL, `examples/bad-exception-handler.java` değişecek; templates uyarlandı (paket notu SKILL'de) | L-12.5 |
| `springboot-patterns` (1) | değişecek | L-12.6 |
| `spring-boot-engineer` (SKILL + 4 references = 5) | 5'i değişecek | L-12.7 |
| `springboot-tdd` (1) | değişecek | L-12.8 |
| `java-coding-standards` (1) | değişecek | L-12.9 |
| `java-code-review` (1) | değişecek | L-12.10 |
| `java-concurrency` (SKILL + 1 reference = 2) | SKILL değişecek; reference aynı | L-12.11 |
| `springboot-security` (1) | değişecek | L-12.12 |
| `spring-transactions` (1) | değişecek | L-12.13 |
| `jpa-patterns` (1) | değişecek | L-12.14 |
| `postgres-patterns` (1) | değişecek | L-12.15 |
| `sql-optimization-patterns` (SKILL + 1 reference = 2) | ikisi de değişecek | L-12.16 |
| `flyway-migrations` (SKILL + 2 examples + 2 templates = 5) | SKILL, `bad-migration.sql`, iki template değişecek; `good-migration.sql` uyarlandı | L-12.17 |
| `spring-boot-openapi-documentation` (SKILL + 12 references = 13) | SKILL ve `complete-examples`, `controller-documentation`, `exception-handling`, `model-documentation`, `pagination-support`, `security-configuration` değişecek; diğer 6 reference aynı | L-12.18 |
| `spring-mvc-testing` (SKILL + 1 reference = 2) | reference değişecek; SKILL aynı | L-12.19 |
| `spring-testing-fundamentals` (SKILL + 2 references = 3) | SKILL ve `assertj-mockito-idioms` değişecek; `cross-cutting-testing-patterns` uyarlandı | L-12.20 |
| `spring-jpa-testing` (SKILL + 4 references = 5) | SKILL, `lazy-loading-tests`, `transactional-tests` değişecek; `testcontainers`, `hibernate-6-migration` aynı | L-12.21 |
| `spring-websocket` (1) | değişecek (bir not) | L-12.22 |
| `spring-websocket-testing` (SKILL + 1 reference = 2) | uyarlandı (retrieval-api ingestion örnekleri geçerli) | — |
| `observability-tracing` (SKILL + 1 reference = 2) | SKILL değişecek; reference uyarlandı | L-12.24 |
| `springboot-verification` (1) | değişecek (yol) | L-12.0 |
| `react-patterns` (1) | değişecek | L-12.25 |
| `react-state-management` (SKILL + 1 reference = 2) | ikisi de değişecek | L-12.26 |
| `react-testing` (1) | değişecek | L-12.27 |
| `react-best-practices` (SKILL + 57 rules = 58) | `client-swr-dedup`, `rerender-functional-setstate` değişecek; `client-localstorage-schema` uyarlandı; `advanced-use-latest` (`onSearchEvent` genel ad) ve diğer 54 kural aynı | L-12.28 |
| `react-composition-patterns` (SKILL + 8 rules = 9) | aynı | — |
| `frontend-a11y` (1) | değişecek | L-12.29 |
| `frontend-security` (SKILL + 1 reference = 2) | ikisi de değişecek | L-12.30 |
| `frontend-performance` (SKILL + 1 reference = 2) | ikisi de değişecek | L-12.31 |
| `vite-patterns` (1) | değişecek | L-12.32 |
| `webapp-testing` (SKILL + 3 examples + 1 script = 5) | SKILL ve `scripts/with_server.py` değişecek (yol); examples uyarlandı | L-12.0 |
| `web-design-guidelines` (1) | aynı | — |
| `writing-good-tests` (1) | değişecek | L-12.33 |
| `code-review-and-quality` (1) | değişecek (bir satır) | L-12.35 |
| `systematic-debugging` (SKILL + 3 md + 1 script = 5) | `condition-based-waiting`, `scripts/find-polluter.sh` (yol) değişecek; SKILL uyarlandı; `root-cause-tracing`, `defense-in-depth` aynı (genel "workspace") | L-12.0, L-12.34 |
| — | `rag-pipeline` (SKILL + `references/indexing.md`), `sse-streaming`, `rag-evaluation` | yeni → L-13…L-15 |

## Değişiklik maddeleri
Biçim: **Bölüm** → kalkan → gelen. Kod örnekleri çevrilirken genel teknik içerik (anotasyon, test kalıbı, SQL tekniği) korunur.

### lead (sahipsiz dosyalar)

**L-01 kök `CLAUDE.md`**
1. **Proje özeti:** ikinci paragrafın sonuna M1 eklenir. ": lab'daki retrieval-api (kaynak, parça, içe alma, vektör arama) o projenin backend'idir." → "; bilgi bankası (retrieval-api) bu repoda yeniden yazılır (Mimari)."
2. **Mimari:**
   - "Backend: … → `backend/`" satırı kalkar. Yerine iki satır gelir: "assistant-api: Spring Boot 4, Java 26, Maven, Spring AI → `assistant-api/`" ve "retrieval-api: Spring Boot 4, Java 26, Maven, pgvector → `retrieval-api/`".
   - "Frontend: … → `frontend/`" → "Web: React 19, TypeScript, Vite → `web/`".
   - "İletişim" maddesinden sonra M2'nin 2.-6. cümleleri gelir.
   - "Veritabanı" → M7'nin ilk cümlesi.
   - "Durum" → "henüz kod yok; modüllerde sadece `CLAUDE.md` var (TASK-001)." Retrieval cümlesi kalkar.
3. **Klasör yapısı:**
   - `backend/`, `frontend/`, `deployment/` satırı → "`assistant-api/`, `retrieval-api/`, `web/`, `deployment/`".
   - `../embedding-pgvector` satırı → "Önceki proje; okunur kalıp referansı (içe alma, parçalama, pgvector, aynı yığın). Çalışma zamanı bağımlılığı değil. Hiçbir rol oraya yazmaz".
   - `ollama.md` satırına "(henüz yok)" eklenir.
4. **Komutlar:**
   - Eski satır: "Backend derle + test: `cd backend && ./mvnw verify`". Yeni satırlar: "assistant-api derle + test: `cd assistant-api && ./mvnw verify`", "retrieval-api derle + test: `cd retrieval-api && ./mvnw verify`".
   - Frontend satırları → `cd web && npm run build`, `cd web && npm run lint`.
5. **Genel kurallar:** "Hiçbir rolün olmayan dosyalar" listesinde değişiklik yok. Yeni madde: "İki servis birbirinin veritabanına bağlanmaz; REST ile konuşur (M7)."
6. **Rol tablosu (sadece yol hücreleri; sıra, bağımlılık ve handoff adları aynı):**
   - 3a backend-coder Yazar: "`assistant-api/`, `retrieval-api/` (test ve Docker dosyaları hariç), `handoff/02-backend-implementation.md`".
   - 3b frontend-coder Yazar: "`web/` (test ve Docker dosyaları hariç), `handoff/02-frontend-implementation.md`".
   - 4 tester Yazar: "Test dosyaları (`assistant-api/src/test/**`, `retrieval-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**`, `web/e2e/**`), `handoff/03-test-report.md`".
   - 5 devops Yazar: "Dockerfile'lar (`assistant-api/`, `retrieval-api/`, `web/`), `web/nginx.conf`, `deployment/`, `ollama.md`, `README.md`, `handoff/04-deploy-report.md`".
7. **Madde 9.1:** compose komutu aynı kalır.

**L-02 `rag.md`**
1. "Amaç"tan sonra `## Ürün hedefi`: M1 + M4.
2. `## Modüller ve bilgi bankası`: M2 + M6 + M7.
3. "Lab'da görünenler":
   - "Canlı yapılandırma" maddesine "(sağ yarıda; ekrana gelmez)" eklenir.
   - "Sistem konsolu" maddesinin son cümlesi → "Ekrana gelmez (Ürün hedefi)."
   - "Servisler" maddesi: "retrieval-api (8080, `../embedding-pgvector`)" → "retrieval-api (8080, bu repoda)".
4. "Genel kurallar" yorumundan cevaplanan konular (ekranlar, sistem konsolu, retrieval-api) çıkar. Yerine iki madde gelir: "- Ekranlar ve görsel dil: Ürün hedefi." ve "- Bilgi bankası: retrieval-api, bu repoda." Kalan alanlar kullanıcıya kalır.

**L-03 `KULLANIM-KILAVUZU.md`**
1. 1. Task'ı hazırla: "Ürün `kaynaklar/rag-lab.html`'in sol yarısıdır; TASK'ta hangi ekrana ya da adıma karşılık geldiğini yaz. Etkilenen katmanlarda hangi servisi (assistant-api, retrieval-api, web) etkilediğini işaretle." eklenir.
2. Lokal modeller paragrafı → "Dil modeli ve embedding Ollama'da, reranker llama-server'da, Mac'te çalışır. … Kurulum ve indirme komutları `ollama.md`'dedir (ilk model task'ında oluşturulur)."
3. Aşama tablosu → "Kodlama | + backend-coder (assistant-api, retrieval-api) + frontend-coder (web) (paralel)".
4. `../embedding-pgvector` anılmaz (M5).

**L-04 `docs/tasks/_TEMPLATE.md`**
1. "Ne isteniyor" yorumu → "Lab'ın sol yarısındaki (kaynaklar/rag-lab.html) hangi ekrana karşılık geliyor? Sağ yarıdaki akışın hangi adımını backend'e ekliyor?"
2. Etkilenen katmanlar:
   - "Backend" → "assistant-api (konuşma, model, prompt, akış, değerlendirme)" ve "retrieval-api (kaynak, içe alma, embedding, arama)".
   - "Frontend" → "Web".
   - "Retrieval (`../embedding-pgvector` ile ilişki)" satırı kalkar.
   - "Veritabanı (migration)" → "Veritabanı (migration; hangi servis?)".
   - Sona "- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)" eklenir.

**L-04b kök `.gitignore`**
`backend/target/`, `backend/build/` → `assistant-api/target/`, `retrieval-api/target/`. `frontend/node_modules/`, `frontend/dist/` → `web/node_modules/`, `web/dist/`. Diğer satırlar aynı kalır.

**L-05 `.claude/settings.json`**
`permissions.allow` dizisinde değişiklikler:
- `"Edit(/backend/**)"` → `"Edit(/assistant-api/**)"` ve `"Edit(/retrieval-api/**)"`.
- `"Edit(/frontend/**)"` → `"Edit(/web/**)"`.
- Eklenir: `"Bash(curl -sN localhost:*)"`, `"Edit(/ollama.md)"`, `"Edit(/README.md)"`.

Dosya geçerli JSON kalır.

**L-06 `agents/planner.md`**
1. Başlamadan önce 2: "Dil modeli, prompt, sorgu yeniden yazma, retrieval ya da rerank içeriyorsa `rag-pipeline`; içe alma, parçalama ya da embedding içeriyorsa `rag-pipeline/references/indexing.md`; cevap akışı içeriyorsa `sse-streaming`; değerlendirme içeriyorsa `rag-evaluation` yükle." eklenir.
2. Okuduğu dosyalar:
   - Lab satırındaki retrieval-api cümlesi → "retrieval-api bu repodadır (`retrieval-api/`). `../embedding-pgvector` sadece okunur kalıp referansıdır; oraya yazma."
   - "Mevcut kod" satırı → "`assistant-api/src`, `retrieval-api/src`, `web/src`, servislerin `pom.xml`'leri, `web/package.json`".
3. Adım 5: "Lab'da görünen davranış güçlü bir ipucudur ama …" → "Lab'ın sol yarısındaki ekran, metin, renk ve etkileşimler ürün hedefidir (`rag.md` 'Ürün hedefi'); TASK aksini söylemedikçe uygulanır ve soru yapılmaz. Sağ yarıdaki akış (istekler, SQL, prompt, loglar) backend davranışı için güçlü ipucudur ama TASK.md'de yazmıyorsa kesin kural değildir;"
4. Adım 3'e: "Hangi servisin (assistant-api, retrieval-api) değiştiğini planın Özet'ine yaz."

**L-07 `agents/backend-coder.md`**
1. description: "Onaylanmış planın backend kısmını (assistant-api ve retrieval-api) Spring Boot ile uygular; … Sadece assistant-api/ ve retrieval-api/ altına yazar, test yazmaz."
2. Rol:
   - "Sadece `backend/src/main/**` …" → "Sadece `assistant-api/` ve `retrieval-api/` altındaki `src/main/**`, `pom.xml`, `.gitignore`, Maven wrapper dosyaları (`mvnw*`, `.mvn/**`) ve o servisin `CLAUDE.md`'sine yazarsın."
   - "`backend/src/test/**`, Dockerfile …" → "`*/src/test/**`, Dockerfile, `.dockerignore` ve `docs/api/**` senin değil."
3. Başlamadan önce 4: "`backend/CLAUDE.md`'deki tablo" → "değişen servisin `CLAUDE.md`'sindeki tablo". Örnekler: "dil modeli, prompt, retrieval/rerank istemcisi → `rag-pipeline`; içe alma, parçalama, embedding → `rag-pipeline/references/indexing.md`, `postgres-patterns`; akış → `sse-streaming`, `java-concurrency`; değerlendirme → `rag-evaluation`".
4. Okuduğu dosyalar → referans satırı: "`../embedding-pgvector/backend` (okunur kalıp referansı: içe alma hattı, pgvector sorguları; kod toplu kopyalanmaz, değiştirilmez)".
5. Adımlar 4-5:
   - `cd backend && ./mvnw verify` → "değişen her servis için `cd <servis> && ./mvnw verify`".
   - "`backend/CLAUDE.md`" → "ilgili servisin `CLAUDE.md`'si".
   - Sona: "Servisin `pom.xml`'i henüz yoksa komut çalıştırılamaz; raporda belirt."
6. Rapor şablonu "Doğrulama" örneği → "`cd assistant-api && ./mvnw verify` → BUILD SUCCESS …".

**L-08 `agents/frontend-coder.md`**
1. description: "frontend/" → "web/".
2. Rol:
   - Yollar: `frontend/` → `web/`, `frontend/CLAUDE.md` → `web/CLAUDE.md`, `frontend/.gitignore` → `web/.gitignore`.
   - Yeni madde: "Ekranlar `kaynaklar/rag-lab.html`'in sol yarısının aynısıdır (ekran, yan menü, düzen, Türkçe metin, renk, etkileşim); sağ yarı (sistem konsolu) uygulanmaz."
3. Başlamadan önce 4: tablo yolu `web/CLAUDE.md`. Örneklere "akan cevap → `sse-streaming`; değerlendirme ekranı → `rag-evaluation`" eklenir.
4. Okuduğu dosyalar → referans satırı: "`../embedding-pgvector/frontend` (okunur kalıp)". `docs/api/openapi.yaml` notu: "`/api/assistant/**` ve `/api/workspaces/**`, ikisi de".
5. Adımlar:
   - Adım 4: `cd frontend && …` → `cd web && npm run build && npm run lint`. Sona: "`web/package.json` henüz yoksa komut çalıştırılamaz; raporda belirt."
   - Adım 5: `frontend/CLAUDE.md` → `web/CLAUDE.md`.

**L-09 `agents/tester.md`**
1. description: değişiklik gerekmez (JUnit, Vitest, Playwright).
2. Rol: "`backend/src/test/**`, `frontend/**/*.test.ts(x)`, `frontend/src/test/**`, `frontend/e2e/**`" → "`assistant-api/src/test/**`, `retrieval-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**`, `web/e2e/**`".
3. Gerçek servis cümlesi: "gerçek dil modeline, reranker'a ya da retrieval servisine gitmez" → "gerçek dil modeline, embedding modeline, reranker'a ya da diğer servise gitmez".
4. Başlamadan önce 3:
   - Backend satırına "sahte model/retrieval/rerank ve prompt testi için `rag-pipeline`, içe alma testi için `rag-pipeline/references/indexing.md`, akış için `sse-streaming`, metrik için `rag-evaluation`" eklenir.
   - Frontend satırına "akış testi için `sse-streaming`" eklenir.
5. Adım 4: `cd backend && ./mvnw verify`, `cd frontend && npm test` → "değişen her servis için `cd <servis> && ./mvnw verify`, `cd web && npm test`".

**L-10 `agents/devops.md`**
1. Rol:
   - Yazma listesi → "Sadece `deployment/**`, `assistant-api/Dockerfile`, `retrieval-api/Dockerfile`, `web/Dockerfile`, `*/.dockerignore`, `web/nginx.conf`, `ollama.md` ve kök `README.md`'ye yazarsın."
   - "(`src/`, `pom.xml`, `package.json`)" aynı kalır.
2. Başlamadan önce 2 → "Plan tester'ı çalıştırıyorsa `handoff/03-test-report.md`, çalıştırmıyorsa planda çalışan coder'ların `02-*.md` raporları aktif task'a ait ve `STATUS: DONE`."
3. Başlamadan önce 3:
   - Başına: "Plan Docker dosyası değiştirmiyorsa (sadece doküman) bu maddeyi ve Adımlar 2-5'i atla; raporda 'Docker değişmedi, komut çalıştırılmadı' yaz."
   - Model kontrolüne "reranker: `curl -s localhost:8081/health`" eklenir.
4. Okuduğu dosyalar: "`backend/pom.xml`, `frontend/package.json`, `frontend/vite.config.ts`" → "`assistant-api/pom.xml`, `retrieval-api/pom.xml`, `web/package.json`, `web/vite.config.ts`".
5. Adım 4 smoke: "proxy üzerinden bir backend endpoint'i" → "proxy üzerinden her iki servisten birer endpoint (`/api/assistant/…`, `/api/workspaces/42/sources`)".
6. Adım 6 → "`deployment/CLAUDE.md`'yi gerçek duruma göre güncelle; kullanıcıya dönük kurulum değiştiyse `README.md`'yi, model gerektiren ilk task'ta `ollama.md`'yi (modeller, `ollama pull` komutları, llama-server reranker başlatma) yaz."

**L-11 `agents/qa.md`**
1. Başlamadan önce 2:
   - "Backend değiştiyse" → "assistant-api ya da retrieval-api değiştiyse".
   - "Frontend değiştiyse" → "web değiştiyse".
   - Eklenir: "dil modeli/içe alma içeren task'ta `rag-pipeline`, değerlendirmede `rag-evaluation`, akışta `sse-streaming`".
2. Adım 2 (Sözleşme uyumu): "`openapi.yaml` ↔ backend controller" → "`openapi.yaml` ↔ her servisin controller ve DTO'ları (kendi path önekiyle) ↔ web type'ları ↔ assistant-api'nin retrieval-api istemcisi".
3. Adım 3:
   - `./mvnw verify` → "değişen her servis için `cd <servis> && ./mvnw verify`".
   - Sona: "Proje iskeleti yoksa (doküman task'ı) komutları 'uygulanamaz' yaz; kural kontrolünü plandaki kontrol komutlarıyla yap."
4. Adım 5: "`npm run test:e2e`" → "`cd web && …`".

**L-12 skill'ler**

**L-12.0 Bütün skill dosyalarında Ç-1.** Özellikle:
- `description` ve gövdelerdeki "backend/ altında" ve "frontend/ altında".
- `springboot-verification` (`backend/` yolu).
- `webapp-testing/SKILL.md` ve `scripts/with_server.py` (`cd backend`, `cd frontend` örnekleri → `cd retrieval-api`/`cd assistant-api`/`cd web`; portlar 8080/8090/5173).
- `systematic-debugging/scripts/find-polluter.sh` (yol).
- `spring-jpa-testing/references/{transactional,lazy-loading}-tests.md`, `spring-boot-engineer/references/data.md`, `spring-boot-openapi-documentation/references/complete-examples.md`, `react-state-management/references/details.md`, `react-testing`, `react-patterns`, `vite-patterns`, `frontend-design`, `frontend-security` (+reference) ve `e2e-testing`'deki `backend/`, `frontend/` geçen satırlar.
- Paket adları (`com.rasitbank.assistant.source…` → `com.rasitbank.retrieval.source…`; `spring-boot-engineer/SKILL.md`, `references/data.md`, `sql-optimization-patterns/references/details.md`, `flyway-migrations/templates/DevDataSeeder.java`).

**L-12.1 `backend-conventions`**
1. description: "RAG asistan projesinin backend'i" → "RAG asistan projesinin iki backend servisi (assistant-api, retrieval-api)". "backend/ altında" → "assistant-api/ ya da retrieval-api/ altında". Sözlük parantezi "(…; workspace, source, chunk, embedding, search)" → "(…; retrieval-api: workspace, source, chunk, ingestion, embedding, search)".
2. Durum notu: "Projede henüz backend kodu yok. … `backend/CLAUDE.md`'ye işlenir" → "… ilgili servisin `CLAUDE.md`'sine işlenir". Sonuna M1'in son iki cümlesi eklenir.
3. "Referans uygulama" notu → M2'nin 2.-4. cümleleri. Ardından: "Diğer skill'lerdeki `Source`/`Chunk`/`IngestionJob` örnekleri retrieval-api'yi, `Conversation`/`Turn` örnekleri assistant-api'yi anlatır." "… ilk task'ta planner'ın kararıdır (kart sorusu)" kalkar.
4. Bölüm 1:
   - "Domain sözlüğü (retrieval, referans uygulamayla aynı)" → "Domain sözlüğü (retrieval-api)".
   - "çalışma alanı (kiracı) → `workspace`" → "çalışma alanı → `workspace` (tek ve sabit, M6)".
   - `searchMode` sonuna "(lab sadece `VECTOR` kullanır; Kararlar #11)".
5. Bölüm 3 isim tablosu, "Dış servis istemcisi" satırı → "`RetrievalClient`, `RerankClient` (assistant-api), `EmbeddingClient` (retrieval-api)".
6. Bölüm 3 Paketler:
   - Mevcut ağacın başlığı "assistant-api (`com.rasitbank.assistant`)" olur.
   - Altına retrieval-api ağacı eklenir: "`com.rasitbank.retrieval` (`RetrievalApplication`): `workspace/`, `source/`, `chunk/`, `ingestion/`, `search/`, `embedding/`, `pagination/`, `config/`, `exception/`; özellik içi yapı aynı".
   - "Retrieval kodu bu backend'e taşınırsa …" maddesi → "İki servis kod paylaşmaz; `PageResponse`, `PageParameters`, exception kategori sınıfları her serviste kendi kök paketindedir (Kararlar #1)."
7. Bölüm 3 REST: "Path: `/api/<çoğul-kebab-case>`" maddesine "assistant-api `/api/assistant/**`, retrieval-api `/api/workspaces/{workspaceId}/**` (lab)" eklenir.
8. Bölüm 3 Veritabanı:
   - İlk satır → "M7. Tablolar (lab): assistant `conversations`, `conversation_turns`, `eval_runs`; retrieval `workspaces`, `sources`, `chunks`, `ingestion_jobs` (arama kaydı yok, Kararlar #8)."
   - "Vektör tutuluyorsa (retrieval bu backend'deyse) …" → "retrieval-api'de: …" (maddenin geri kalanı aynı).
9. Bölüm 5 "Dış modeller testte": "retrieval-api testleri gerçek embedding modeline gitmez; sahte `EmbeddingClient` sabit vektör döner." eklenir.
10. Bölüm 7 başına: "Ayrıntı: `rag-pipeline` (sorgu zamanı; içe alma ve embedding `references/indexing.md`), `sse-streaming`, `rag-evaluation`." Dış modeller maddesine "embedding modeli retrieval-api'nindir; dil modeli ve reranker assistant-api'nin" eklenir.

**L-12.2 `frontend-conventions`**
1. description: "frontend/ altında" → "web/ altında".
2. Durum notu: `frontend/CLAUDE.md` → `web/CLAUDE.md`. "Referans" notunun son cümlesi ("Lab'ın sağ yarısının … TASK karar verir.") → M1. "`source`/`search` örnekleri oradan gelir" → "`source` örneklerinin çalışan hali oradadır".
3. Bölüm 2 sözlük: "çalışma alanı → `workspace`" → "çalışma alanı → `workspace` (tek ve sabit; sadece retrieval-api adreslerinde ve Bilgi kaynakları bilgi kutusunda, M6)".
4. Bölüm 4: ağaç kökü `frontend/` → `web/`.
5. Bölüm 5 route maddesi: "Çalışma alanı id'sinin adreste olup olmayacağı (`/w/:workspaceId/...`) ve …" → "Adreste çalışma alanı yoktur (M6). Anlatım sayfalarının adresleri, gelirlerse, planda belirlenir."
6. Bölüm 7 "Path'ler göreli" → "`/api/assistant/...` (assistant-api) ve `/api/workspaces/42/...` (retrieval-api, Bilgi kaynakları); ikisi de `docs/api/openapi.yaml`'da."
7. Bölüm 8 başına "Ayrıntı ve test kalıbı: `sse-streaming`." eklenir. Polling bölümündeki referans cümlesi kalır (M5).
8. Bölüm 9: "asistan ayarları sunucu yapılandırmasıdır (lab: …)" cümlesi → M4.
9. Bölüm 12: "`kaynaklar/rag-lab.html`'in görsel dili (font, renk) başlangıç referansıdır." → "Görsel dil lab'ın sol yarısıyla aynıdır: token değerleri lab'ın `--s-*` (site) ve `--f-*` (font) değişkenlerinden birebir alınır; `--o-*` (konsol) kullanılmaz. `frontend-design`'ın özgün tasarım önerileri sadece lab'da karşılığı olmayan durumlar içindir."
10. Bölüm 13 komutlarının başına "Hepsi `web/` içinde çalıştırılır." eklenir.

**L-12.3 `frontend-design`**
1. description sonuna: "Bu projede ekranlar kaynaklar/rag-lab.html'in sol yarısından birebir alınır; lab'dan token çıkarırken ve lab'da karşılığı olmayan ekran ya da durum tasarlarken kullan."
2. "Proje kuralı" notunun son cümlesi ("Görsel dilin başlangıç referansı …") → M1 + "Bu projede görsel dil yeniden tasarlanmaz. Token'lar lab'ın `--s-*`/`--f-*` değerlerinden alınır. Aşağıdaki özgünlük ve 'estetik risk' yönergeleri sadece lab'da karşılığı olmayan ekran ya da durumlar (boş durum, hata, 'Sayfa bulunamadı') için, lab'ın diliyle uyumlu biçimde uygulanır."
3. `frontend/` yolu varsa `web/` olur.

**L-12.4 `e2e-testing`**
1. Girişe M1 + "Sistem konsolu olmadığı için akışın adımları (yeniden yazılan sorgu, modele giden parçalar, strateji) ekranda değil tur kaydında (Konuşmalar ekranı) doğrulanır."
2. Yollar `frontend/e2e` → `web/e2e`, komutlar `cd web`.
3. Page Object `goto(workspaceId)` → `/w/${workspaceId}/arama` → `PreviewPage.goto()` → `/onizleme`.
4. Test `givenDalgaWorkspace_whenHybridSuggestionAndOpenHit_thenSourceDetailHighlightsChunk` → `givenGuideIngested_whenQuestionAnswered_thenCitationOpensChunk`:
   - Soru "Gold kartın yıllık aidatı ne kadar?".
   - Cevabın tam metnine bakılmaz; akışın bittiği, en az bir `[n]` alıntısı ve alıntının parçayı açtığı doğrulanır.
   - Satır 122'deki metin lab'ın parça açma metni olur.
5. `/w/42/kaynaklar` → `/kaynaklar`; `/w/42/kaynaklar/301` → `/kaynaklar/10001`.
   - `DELETE …/sources/301` örneği → "Yeni sürüm yükle" (`POST /api/workspaces/42/sources/10001/versions` → 202) + ilerlemenin sayfa yenilense de sürmesi.
   - `/api/workspaces/42/...` adresleri kalır.
6. "çalışma alanındaki boş yere göre davran" → "veriyi kalıcı değiştiren testler (yeni sürüm) sona konur; temiz başlangıç `down -v` + `up`".
7. `givenWorkspaceWithTenSources_whenEleventhSourceAdded_thenShowsSourceLimitMessage` → `givenIngestionRunning_whenUploadOpened_thenUploadButtonIsDisabled` (lab: yükleme sürerken düğme kapalı).
8. `waitForResponse(… '/api/workspaces/42/search' …)` → `response.url().includes('/api/assistant/conversations/') && response.url().endsWith('/messages')` + `content-type` `text/event-stream`.
9. Satır 338 "ücretli embedding API'siyle toplu yeniden içe alma" → "gerçek modellerle uzun değerlendirme çalıştırması".

**L-12.5 `problem-details-rfc9457`**
1. "Bütün sınıflar kökteki `com.rasitbank.assistant.exception` paketindedir" → "… her servisin kendi kök paketindeki `exception` paketindedir (`com.rasitbank.assistant.exception`, `com.rasitbank.retrieval.exception`); şablonlar assistant-api paketiyle yazılıdır."
2. `ServiceUnavailableException` "(embedding servisi gibi …)" → "(dil modeli, embedding modeli, reranker ya da diğer servis kullanılamadığında)".
3. Örnek `throw new ConflictException("SOURCE_LIMIT_REACHED", …)` → `throw new ConflictException("INGESTION_IN_PROGRESS", "Source has an active ingestion job: " + sourceId);`.
4. Tablo:
   - Başlık "Örnek eşleme (kesin liste `docs/api/openapi.yaml`)" olur; yeni "Servis" sütunu eklenir.
   - `SOURCE_LIMIT_REACHED` ve `SOURCE_NAME_TAKEN` satırları kalkar.
   - `WORKSPACE_NOT_FOUND` "Var olmayan çalışma alanı (sadece 42 vardır)" olur.
   - `INGESTION_IN_PROGRESS`, `EMBEDDING_MODEL_MISMATCH`, `EMBEDDING_UNAVAILABLE` retrieval satırları olarak kalır.
   - assistant-api satırları eklenir: `CONVERSATION_NOT_FOUND`, `TURN_IN_PROGRESS`, `FEEDBACK_ALREADY_SUBMITTED`, `CHAT_MODEL_UNAVAILABLE`, `RETRIEVAL_UNAVAILABLE`, `RERANKER_UNAVAILABLE`.
   - Validation satırı → "soru metni 2-1000 karakter (assistant-api); arama metni boş olamaz, `limit` 1-50 (retrieval-api)".
5. `detail` maddesindeki Türkçe örnek "Bu ad çalışma alanında başka bir kaynakta kullanılıyor" → "Dil modeline şu an ulaşılamıyor".
6. Yeni madde: "Cevap akışı başladıktan sonraki hata Problem Details değil, SSE `error` olayıdır; aynı `errorCode` kullanılır (`sse-streaming`)."
7. `examples/bad-exception-handler.java`: `"Kaynak sınırı doldu"` → `"Kaynak işleniyor"` (kötü örnek olarak kalır).

**L-12.6 `springboot-patterns`**
1. "Aşağıdaki `Source`/`Ingestion` örnekleri referans uygulamadan … gelir; retrieval bu backend'e taşınırsa …" cümlesi → "Aşağıdaki `Source`/`Ingestion` örnekleri retrieval-api'yi anlatır."
2. 202 açıklamasına "Cevap akışı (assistant-api `POST /api/assistant/conversations/{conversationId}/messages`) `200` + `text/event-stream` döner (`sse-streaming`)." eklenir.
3. Yeni kısa bölüm "Dış servis istemcileri (assistant-api)":
   - `RetrievalClient` (retrieval-api) ve `RerankClient` `RestClient` ile yazılır; bağlantı/okuma zaman aşımı `assistant.*`'tan gelir.
   - Hata 503 `RETRIEVAL_UNAVAILABLE` / `RERANKER_UNAVAILABLE` olur.
   - Ayrıntı `rag-pipeline`.
4. Satır 155 "Arama sonucu sayfalı değildir; `limit` ile sınırlıdır." kalır.
5. Cache bölümü "Embedding API'si ücretli ve yavaş olduğu için …" → "Embedding modeli yavaş olduğu için …" (lokal, ücretsiz). Yeni not: "Cevap cache'lenmez (geçmişe ve rehber sürümüne bağlı)."
6. "Embedding API'sine paralel istek sayısı" → "embedding modeline paralel istek sayısı". "(embedding API'si dahil)" → "(dil modeli, embedding, reranker ve diğer servis dahil)".

**L-12.7 `spring-boot-engineer`**
1. SKILL.md:
   - Adım 4 "tek gizli bilgi embedding API anahtarıdır" → "gizli bilgi yok (lokal modeller; adresler ortam değişkeninden)".
   - Service örneğinden `embeddingModelGuard.ensureWorkspaceCompatible` kalır. `SOURCE_NAME_TAKEN` `try/catch` bloğu kalkar. `workspace.addSource(...)` → `Source.create(workspace, sourceRepository.nextSourceId(), name.strip(), kind, now)`.
   - Satır 165 "Kaynak limiti ve tekil ad kuralı `Workspace.addSource` içindedir; çalışma alanı satırı kilitlenerek okunur" → "Yeni sürüm yüklemede kaynak satırı kilitlenerek okunur; aktif iş varsa `INGESTION_IN_PROGRESS` (`spring-transactions`)".
   - Paket `com.rasitbank.assistant.source` → `com.rasitbank.retrieval.source`.
2. `references/data.md`:
   - Girişteki not: "… ve arama kayıtları" kalkar.
   - `Workspace` entity'sinden `MAX_SOURCES_PER_WORKSPACE` ve `addSource` limit/ad kontrolleri kalkar; `Workspace.seeded` kalır (M6).
   - "`(workspace_id, lower(name))` unique index'i …" maddesi kalkar.
   - "`@Version` … `sources` koleksiyonuna ekleme" maddesi → "kaynak satırındaki pessimistic lock ile tek aktif iş".
   - Satır 249 → "Yeni sürüm yüklemede kaynak `findByWorkspaceIdAndIdForUpdate` ile kilitlenir".
   - Vektör bölümü ve `findNearest` aynı kalır (retrieval-api).
3. `references/security.md`: "Proje durumu" → "hesap ve Spring Security yok; gizli bilgi yok (lokal modeller)". `WorkspaceAdminService` örneği → `SourceAdminService` + `@sourceAccess.canManage(#sourceId, authentication)`.
4. `references/testing.md`:
   - `givenWorkspaceWithFreeSlot_whenCreateSource_thenSavesIngestingSourceWithRequestedName` → `givenSeededWorkspace_whenCreateSource_thenSavesIngestingSourceWithRequestedName`.
   - Limit testi → `givenSourceWithActiveJob_whenUploadSourceVersion_thenThrowsIngestionInProgress`.
   - `WorkspaceFixtures.workspaceWithSources(…)` → `WorkspaceFixtures.seededWorkspace()`; `Workspace.create("Dalga Fiber", …)` → `Workspace.seeded(42L, "Raşit Bankası · Dijital Asistan", …)`; `addSource("Kaynak " + index …)` döngüsü kalkar.
   - Yeni küçük örnek: assistant-api'den `givenUnknownConversationId_whenSendMessage_thenThrowsConversationNotFound` (`backend-conventions` bölüm 5'teki örneğe gönderme).
5. `references/web.md`:
   - "bu kaynak adı çalışma alanında alınmış mı" → "bu tura geri bildirim verilmiş mi".
   - `SearchController` örneği retrieval-api olarak kalır.
   - Yeni küçük örnek: `ConversationController#sendMessage` imzası (`produces = MediaType.TEXT_EVENT_STREAM_VALUE`, gövde `sse-streaming`'e gönderme).

**L-12.8 `springboot-tdd`**
1. `MockMultipartFile("file", "fatura-sss.md", "text/markdown", "# Fatura SSS"…)` → `("file", "urun-ve-hizmet-rehberi.pdf", "application/pdf", PDF_BYTES)`.
2. `givenWorkspaceBelowSourceLimit_whenCreateSource_thenSavesIngestingSource` → `givenSeededWorkspace_whenCreateSource_thenSavesIngestingSource`.
3. `givenWorkspaceWithTenSources_whenCreateSource_thenThrowsSourceLimitReached` → `givenSourceWithActiveJob_whenUploadSourceVersion_thenThrowsIngestionInProgress` (`SourceFixtures.sourceWithActiveJob(10001L)`).
4. "Fatura SSS" → "Raşit Bankası Ürün ve Hizmet Rehberi".
5. Satır 107 → "Tek aktif iş kuralı ayrıca doğrudan `Source.startIngestion` üzerinde unit test edilir; iş aşamaları `IngestionJobTest`'te."
6. Controller testleri `/api/workspaces/42/sources` kalır; gövde `{"name":"Fatura SSS","kind":"MARKDOWN"}` → `{"name":"Raşit Bankası Ürün ve Hizmet Rehberi","kind":"PDF"}`.
7. `WorkspaceFixtures.workspaceWithSources` → `WorkspaceFixtures.seededWorkspace()` (L-12.7/4 ile aynı).

**L-12.9 `java-coding-standards`**
1. `private static final int MAX_SOURCES_PER_WORKSPACE = 10;` → `private static final int MESSAGE_MAX_LENGTH = 1000;`.
2. Paket ağacındaki satır → "`source/` (workspace, chunk, search, ingestion aynı iç yapıyla) — retrieval-api; assistant-api'de `conversation/`, `retrieval/`, `generation/`, `handoff/`, `evaluation/`".
3. Satır 197 → "İş kuralı sınırları (soru 2-1000 karakter, kaynağın tek aktif içe alma işi) domain'dedir; ortama göre ayarlanan arama parametreleri (`ef_search`), parçalama boyutları, `topK`, `candidates`, model adları config'tedir."
4. Test kurulumu aynı kalır (retrieval-api).

**L-12.10 `java-code-review`**
1. `Source.pending(workspace, …)` kalır.
2. Sınır değer satırı → "kaynak adı 1/2/80/81 karakter, arama `limit` 50/51, soru 1000/1001 karakter, aralık dışı alıntı numarası (`[0]`, `[6]`)".
3. Satır 210 "(ingestion ilerlemesi) … embedding istekleri" → "(içe alma ilerlemesi, akış iptal bayrağı) … embedding ve dil modeli istekleri".

**L-12.11 `java-concurrency`**
1. description: "embedding API'sine paralel ve sınırlı istek atan içe alma akışı" → "embedding modeline paralel ve sınırlı istek atan içe alma akışı (retrieval-api)"; "SSE cevap akışı ve iptali (assistant-api)" eklenir.
2. Satır 18 "embedding API" → "embedding modeli".
3. Satır 123 → "aynı kaynak için iki içe alma isteğinin aynı anda gelmesi; aynı konuşmaya aynı anda iki soru".

**L-12.12 `springboot-security`**
1. "Proje durumu" → "hesap ve Spring Security yok; gizli bilgi yok (lokal modeller); lab'daki `Authorization` başlığı bankanın kimlik sisteminin temsilidir. Kullanıcı girdisi ve parça içeriği talimat değil veridir (prompt injection, `backend-conventions` bölüm 7)."
2. `canEditWorkspace(#workspaceId)` → `canManageSource(#sourceId)`.
3. KÖTÜ/İYİ `SearchController` örneği kalır (retrieval-api).
4. Yeni kısa madde "Prompt injection (assistant-api)": sistem talimatına kullanıcı metni eklenmez, parçalar `<parcalar>` bölümünde, model çıktısı HTML olarak işlenmez.

**L-12.13 `spring-transactions`**
1. description örnekleri → "aynı kaynağa iki içe alma işi, aynı tura iki geri bildirim, aynı konuşmaya iki soru, tur kaydının akış bitince yazılması, dış model çağrısında transaction açık tutmama".
2. Satır 55 → "(kaynak zaten işleniyor mu? tur zaten geri bildirim aldı mı? konuşmada süren tur var mı?)".
3. Unique index örneği → `CREATE UNIQUE INDEX uk_ingestion_jobs_source_id_active ON ingestion_jobs (source_id) WHERE stage NOT IN ('DONE', 'FAILED')`.
4. Pessimistic lock örneği → `SourceRepository.findByWorkspaceIdAndIdForUpdate`.
5. Optimistic lock uyarısı "çalışma alanına kaynak ekle" → "konuşmaya tur ekle".
6. Satır 68 "Referans uygulama (…), kaynak ekleme" paragrafı kalkar.
7. Satır 70 "yeni sürüm yükleme" paragrafı retrieval-api kuralı olarak kalır.
8. Yeni paragraf "assistant-api": "yeni soru konuşmayı kilitleyerek okur, `STREAMING` tur varsa `TURN_IN_PROGRESS`; tur `done` olayından önce commit edilir; model çağrısı transaction dışındadır."
9. Satır 104 "Embedding API çağrısının retry'ı" → "Embedding modeli çağrısının retry'ı"; dil modeli akışı retry edilmez.
10. Satır 162 → "aynı kaynağa eşzamanlı iki yükleme, aynı konuşmaya eşzamanlı iki soru".

**L-12.14 `jpa-patterns`**
1. Satır 91 "Kaynak `Workspace.addSource(...)` üzerinden oluşturulur (kural sahibi aggregate)" → "Kaynak `Source.create(...)` factory'siyle oluşturulur; tek aktif iş kuralının sahibi `Source`'tur (`startIngestion`)".
2. `@OneToMany(mappedBy = "workspace" …)` örneği kalır.

**L-12.15 `postgres-patterns`**
1. Satır 59-61 tekillik örneği "aynı çalışma alanında aynı kaynak adı yok" → partial unique index "kaynağın tek aktif içe alma işi" (L-12.13/3).
2. Başa not: "pgvector bölümleri retrieval-api'nin `retrieval` veritabanı içindir; assistant veritabanında vektör yok (M7)."
3. Satır 111 notu → "bu projede iş, oluşturan transaction commit edilince doğrudan executor'a verilir; kuyruk tablosu taranmaz".

**L-12.16 `sql-optimization-patterns`**
1. SKILL satır 93 `WHERE lower(name) = 'fatura sss'` → `'raşit bankası ürün ve hizmet rehberi'` (örnek amacı aynı).
2. `references/details.md`'de `search_events` örnekleri varsa → `conversation_turns` (assistant). Paket adı Ç-1.

**L-12.17 `flyway-migrations`**
1. SKILL:
   - `uk_sources_workspace_id_name` satırı → L-12.13/3 index.
   - `V7__add_result_limit_to_search_events.sql` → `V4__add_time_to_first_token_ms_to_conversation_turns.sql` (assistant).
   - Satır 182 "Lab demosunun sabit çalışma alanı, kaynak türü kodları" → "tek çalışma alanı (42, M6), kaynak türü kodları" (`V…__seed_workspace.sql`).
   - Satır 183 "içe alma akışıyla (embedding API'si)" → "içe alma akışıyla (embedding modeli)".
   - `DevDataSeeder` örneği: "Dalga Fiber" + `addSource` → "dev'de örnek kaynak kaydı yerine rehberin `Yeni sürüm yükle` ile yüklenmesi önerilir; seeder örneği `Source.create(… "Raşit Bankası Ürün ve Hizmet Rehberi", SourceKind.PDF …)`".
   - Yeni not: "Her servis kendi `src/main/resources/db/migration` klasörüne ve kendi veritabanına sahiptir (M7)."
2. `templates/V1__create_table_template.sql`:
   - "Çalışma alanları seed verisiyle gelir (id'ler sabit: 42, 19 ...)" → "Tek çalışma alanı seed migration'ıyla gelir (42)".
   - `uk_sources_workspace_id_name` → kalkar (yerine partial unique index örneği).
3. `templates/DevDataSeeder.java`:
   - Paket `com.rasitbank.retrieval.config`, import'lar `com.rasitbank.retrieval.…`.
   - "Dalga Fiber"/"Yardım merkezi"/"Fatura SSS" → L-12.17/1 seeder.
   - Log `Dev data seeded sources={}`.
4. `examples/bad-migration.sql`: `INSERT INTO sources (workspace_id, name, kind) VALUES (42, 'Deneme kaynağı', 'MARKDOWN');` kalır (kötü örnek; içerik referansa özgü değil).

**L-12.18 `spring-boot-openapi-documentation`**
1. SKILL:
   - "Proje kuralı" maddesine "`docs/api/openapi.yaml` iki servisi path önekiyle tanımlar; her servis kendi önekini uygular ve uyum kontrolünü kendi önekiyle yapar (Kararlar #3)" eklenir.
   - Path örnekleri: `{conversationId}`, `{turnId}` eklenir.
   - Yeni madde: "SSE endpoint'i `text/event-stream` ile belgelenir; olaylar `docs/api/sse.md`'de".
   - `SourceController#createSource` örneğinden 409 açıklamasındaki `SOURCE_NAME_TAKEN, SOURCE_LIMIT_REACHED` kalkar → `INGESTION_IN_PROGRESS, EMBEDDING_MODEL_MISMATCH`.
   - `name` örneği "Fatura SSS" → "Raşit Bankası Ürün ve Hizmet Rehberi"; "unique in the workspace ignoring case" kalkar.
   - DTO örneği `SearchChunksRequest.query` example "Şifremi unuttum …" → "Gold kartın yıllık aidatı ne kadar?".
   - Doğrulama satırı: "`curl localhost:8090/v3/api-docs.yaml` (assistant-api) ya da `localhost:8080` (retrieval-api)".
2. References (`complete-examples`, `controller-documentation`, `exception-handling`, `model-documentation`, `pagination-support`, `security-configuration`): `SOURCE_LIMIT_REACHED`/`SOURCE_NAME_TAKEN`/"Fatura SSS"/`search-events`/"workspace oluşturma" örnekleri Ç-2'ye göre çevrilir; Ç-1 yolları uygulanır.

**L-12.19 `spring-mvc-testing/references/mvc-rest-testing-patterns.md`**
1. 409 `SOURCE_LIMIT_REACHED`/`SOURCE_NAME_TAKEN` testleri → 409 `INGESTION_IN_PROGRESS` (`POST /api/workspaces/42/sources/10001/versions`).
2. "Fatura SSS" → rehber adı.
3. Yeni kısa örnek: assistant-api SSE async dispatch (`sse-streaming`'e gönderme).

**L-12.20 `spring-testing-fundamentals`**
SKILL ve `assertj-mockito-idioms.md`'deki limit/ad testleri, `WorkspaceFixtures.workspaceWithSources` ve "Dalga Fiber"/"Fatura SSS" metinleri L-12.8'e göre çevrilir. `ArgumentCaptor` örneğine assistant-api'den "modele giden prompt'u yakalayıp numaralı parçaları doğrulama" eklenir (`rag-pipeline`).

**L-12.21 `spring-jpa-testing`**
SKILL, `lazy-loading-tests.md`, `transactional-tests.md`: "Referans uygulama: `SourceRepositoryTest` (çalışma alanı sınırı, `uk_sources_workspace_id_name` ihlali) …" paragrafları → "retrieval-api: `SourceRepositoryTest` (aktif iş index'i ihlali), `ChunkRepositoryTest` (vektör sorguları, toplu silme), `IngestionJobRepositoryTest`; assistant-api: `TurnRepositoryTest` (konuşmanın turları sırası, `chunk_ids` dizisi)". Referans atfı kalkar; Ç-1 yolları uygulanır.

**L-12.22 `spring-websocket`**
1. description'a "(bu projede STOMP henüz planlı değil; cevap akışı SSE'dedir, içe alma ilerlemesi polling ile izlenir)" eklenir.
2. Satır 117 "çalışma alanı üyeliği kontrol edilir" → "kaynağa erişim kontrol edilir".

**L-12.24 `observability-tracing`**
1. Satır 75 log örneği kalır (retrieval-api, lab logu).
2. Yeni örnek: `Turn completed conversationId={} turnId={} strategy={} promptTokens={} latencyMs={}` (assistant-api).
3. Satır 79 "secret (embedding API anahtarı)" → "secret (`Authorization` başlığı)"; "kullanıcının yazdığı sorgu metni" → "müşterinin sorusu, yeniden yazılmış sorgu, cevap, parça içeriği, prompt".
4. Span örneği `searchChunks` kalır. Yeni not: "assistant-api'de `AnswerService.answer` altında `retrieval`, `rerank`, `generation` span'leri; iki servis arasında trace context `RestClient` ile taşınır".

**L-12.25 `react-patterns`**
`CreateSourceForm({ workspaceId })` örneği → `MessageComposer` (`useActionState`; label "Soru", buton "Soruyu gönder"; akış handler'dan başlar). Dosya alanlı form notu `UploadSourceVersionDialog` ile (lab: "Yeni sürüm yükle", "Yükle ve işle", "Vazgeç"). Ç-1 yolları uygulanır.

**L-12.26 `react-state-management`**
1. description "(ör. ingestion ilerlemesi)" kalır; "akan cevabın (SSE) state'e bağlanması" eklenir.
2. SKILL satır 37 ve `references/details.md`:
   - `sourceKeys.*(workspaceId, …)` → `sourceKeys.ingestionJob(jobId)` vb.; `workspaceKeys` invalidation kalkar.
   - `useSearchChunksMutation` örneği → `useAnswerStream` (yerel state + `AbortController`, kısa).
   - Referans kalıp atfı kalır (M5).

**L-12.27 `react-testing`**
1. MSW `/api/workspaces/:workspaceId/sources/:sourceId` → `/api/workspaces/42/sources/:sourceId`.
2. `/api/workspaces/:workspaceId/search` handler'ı → `POST /api/assistant/conversations/:conversationId/messages` (SSE gövdeli, `src/test/sse.ts`).
3. `route: '/workspaces/42/sources/301'` → `/kaynaklar/10001`.
4. `SearchForm workspaceId={42}` axe testi → `MessageComposer`.
5. `givenWorkspaceAtSourceLimit_whenCreateSubmitted_thenShowsSourceLimitMessage` → `givenChatModelDown_whenQuestionSent_thenShowsChatModelUnavailableMessage` ("Dil modeline şu an ulaşılamıyor.").
6. `SourceDetails workspaceId={42} sourceId={301}` → `SourceDetails sourceId={10001}`.
7. Satır 353 "(ör. kaynak ekleyip … ardından lab'da arama)" → "(ör. yeni rehber sürümü yükleyip ilerlemeyi izleme, ardından Önizleme'de soru sorup alıntıdan parçayı açma)".

**L-12.28 `react-best-practices/rules`**
1. `client-swr-dedup.md`:
   - `SourceStatusBadge({ workspaceId, sourceId })` → `({ sourceId })`, `fetch('/api/workspaces/42/sources/${sourceId}')`, `useSourceQuery(sourceId)`, `sourceKeys.detail(sourceId)`.
   - `ReingestSourceButton` → `UploadSourceVersionButton({ sourceId })`.
2. `rerender-functional-setstate.md` satır 76 örneği → akan token birikimi `setAnswer((text) => text + event.text)`; referans atfı kalkar.

**L-12.29 `frontend-a11y`**
1. Satır 76 "(ör. `SOURCE_LIMIT_REACHED`, `EMBEDDING_UNAVAILABLE`)" → "(ör. `CHAT_MODEL_UNAVAILABLE`, `EMBEDDING_UNAVAILABLE`)".
2. `SearchForm({ workspaceId })` → `MessageComposer()`.
3. Satır 215 "arama bitince sonuç sayısı ('10 sonuç bulundu')" → "cevap bitince ('Cevap tamamlandı')"; ingestion kısmı kalır.
4. Alıntı `[n]` düğmesinin erişilebilir adı örneği eklenir ("1 numaralı kaynağı aç").

**L-12.30 `frontend-security`**
1. description: "embedding API anahtarı gibi secret'ların" → "model sunucusu adresleri gibi bilgilerin"; "arama sonucu vurgulama" → "model cevabı ve parça içeriğini gösterme".
2. SKILL:
   - Route örneği `/workspaces/:workspaceId/sources/:sourceId` → `/kaynaklar/:sourceId`, `/konusmalar/:conversationId`.
   - Varlıklar "embedding API anahtarı ve kotası … çalışma alanlarının birbirinin içeriğini görmemesi" → "müşteri soruları ve konuşma geçmişi, banka rehberinin içeriği".
   - STRIDE Spoofing satırı → "Biri başka bir konuşmayı açabilir mi? — erişim backend'de kontrol edilir (hesap yok: demo)".
   - Kötüye kullanım örnekleri → Ç-2 güvenlik satırı + "soru kutusuna 10 000 karakter".
   - Satır 25 ve 93'teki "Arama sonucu" → "Model cevabı ve parça içeriği".
   - Satır 117'deki ingestion örneğinden `workspaceId` kalkar.
3. `references/hardening-patterns.md`:
   - Route parametresi örneği `workspaceId` → `sourceId`, `conversationId`.
   - `/w/42/arama?q=` → `/onizleme` (soru URL'ye konmaz).
   - `request('/api/workspaces/${workspaceId}/search'…)` → `sendMessage(conversationId, …)`.
   - `sourceKeys.ingestionJob(workspaceId, jobId)` → `(jobId)`.
   - Satır 89'daki referans atfı kalkar; key öneki `rag:`.

**L-12.31 `frontend-performance`**
1. description "canlı ingestion ilerlemesi/STOMP güncellemeleri ya da uzun arama sonucu listesi" → "hızlı gelen token'lar, uzun konuşma ve tur listesi, parça listesi, içe alma/değerlendirme polling'i".
2. SKILL satır 37, 146 "Arama sonucu" → "Akan cevap / parça listesi"; ingestion satırları kalır.
3. Reference:
   - Context örneği "çalışma alanı bilgisi" → "konuşma bilgisi".
   - `sourceKeys.list(workspaceId)` → `sourceKeys.list()`; `getIngestionJob(workspaceId, …)` → `getIngestionJob(jobId)`.
   - `ef_search` slider örneği → "her token'da render" örneği.

**L-12.32 `vite-patterns`**
1. Proxy örnekleri → `'/api/assistant': { target: 'http://localhost:8090', changeOrigin: true }`, `'/api/workspaces': { target: 'http://localhost:8080', changeOrigin: true }`.
2. `/ws` örneği "(WebSocket gelirse) → 8090".
3. env örneği `ASSISTANT_API_URL ?? 'http://localhost:8090'`, `RETRIEVAL_API_URL ?? 'http://localhost:8080'`.
4. Docker notu `http://backend:8080` → `http://assistant-api:8090`, `http://retrieval-api:8080`.
5. Yeni not: "SSE cevabı proxy'de tamponlanmamalı (`sse-streaming`)." `frontend/` yolları → `web/`. Satır 390 "workspace kökü" kalır.

**L-12.33 `writing-good-tests`**
1. Change detector örneği → "`expect(MESSAGE_MAX_LENGTH).toBe(1000)` değil, '1001 karakterlik soru gönderilemez ve kural mesajı gösterilir'".
2. MSW örneği 409 `SOURCE_LIMIT_REACHED` → `POST /api/workspaces/42/sources/:sourceId/versions` 409 `INGESTION_IN_PROGRESS`.

**L-12.34 `systematic-debugging/condition-based-waiting.md`**
Playwright satırı `/api/workspaces/42/search` → `r.url().endsWith('/messages') && r.ok()`.

**L-12.35 `code-review-and-quality`**
Satır 77 "embedding API anahtarı ortam değişkeninden geliyor …; sorgu metni log'lanmıyor" → "model adresleri ortam değişkeninden geliyor; müşterinin sorusu, cevap, parça içeriği ve prompt log'lanmıyor".

### Yeni skill'ler (kullanıcı kararı 2; lead yazar)
Ortak kurallar:
- Dosya `.claude/skills/<ad>/SKILL.md`; frontmatter `name` = klasör adı, `description` Türkçe ve tetikleyici.
- Metin Türkçe; kod örnekleri İngilizce ve `backend-conventions`/`frontend-conventions`'a uygun.
- Başta: "Proje kuralı: `backend-conventions` bölüm 7 / `frontend-conventions` bölüm 8 önce gelir."
- Spring AI'a özgü API ayrıntıları "iskelet task'ında `pom.xml`'deki sürümün dokümanıyla doğrulanır" notuyla yazılır; sürüm numarası sabitlenmez.
- Kaynaklar: `kaynaklar/rag-lab.html` ("Kod" sekmesi: `AnswerService`, `assistant.*` ve retrieval-api ayarları), `kaynaklar/rag.html` (RAG hattı).

**L-13 `rag-pipeline`** (SKILL + `references/indexing.md`)
description: "RAG hattı. Sorgu zamanı (assistant-api): Spring AI ChatClient ile lokal dil modeli (Ollama), prompt şablonları ve numaralı parçalar, takip sorusunu yeniden yazma, retrieval-api istemcisi, reranker (llama-server /v1/rerank) ve yedek davranış, alıntı ayrıştırma, 'bulamadım' cevabı, tur kaydı. İndeksleme zamanı (retrieval-api, references/indexing.md): PDF okuma, parçalama, embedding (bge-m3), pgvector'e yazma, vektör arama. Dil modeli, prompt, retrieval, rerank, içe alma ya da embedding kodu yazarken, test ederken veya review ederken kullan."

SKILL.md bölümleri:
1. İki zaman ve iki servis (`rag.html`).
2. Sorgu hattı: soru → (takip sorusuysa) `QueryRewriter.rewriteQuery` → `RetrievalClient.search(query, limit)` (`limit` = `topK` 5 ya da `candidates` 20) → (`RERANK` ise) `RerankClient.rerankChunks` → ilk `topK` → `PromptBuilder` → `ChatClient` stream → alıntı ayrıştırma → tur kaydı. `LLM_ONLY`'de retrieval çağrılmaz.
3. retrieval-api isteği: `POST /api/workspaces/{workspaceId}/search` `{query, mode: "VECTOR", limit}` (şema `docs/api/openapi.yaml`).
4. Reranker: `POST /v1/rerank` `{model, query, documents}` → `results[].index`, `results[].relevance_score`. Kapalıysa lab davranışı (vektör sırasının ilk 5'i + WARN log) ya da 503; hangisi olacağı TASK kararıdır.
5. Prompt: `src/main/resources/prompts/*.st`; sistem talimatı Türkçe, yeniden yazma şablonu İngilizce (lab); parçalar `[1]…[n]` ve `<parcalar>` bölümünde; geçmiş + soru; `notFoundAnswer`.
6. Model ayarları: `temperature`, `num_ctx`, `think: false` (lab); `assistant.*` / `spring.ai.*` properties.
7. Alıntı: `\[(\d+)\]`; aralık dışı numara yok sayılır; `cited`.
8. Hata ve zaman aşımı (örnek kodlar); transaction açıkken dış çağrı yok; loglama sınırı.
9. Test: sahte `ChatModel`, `RetrievalClient`, `RerankClient`; prompt yakalama. Örnek adlar: `givenLlmOnlyMode_whenAnswer_thenDoesNotCallRetrieval`, `givenRerankStrategy_whenRetrieveContext_thenSendsTopFiveOfTwentyCandidatesInRerankOrder`, `givenOutOfRangeCitation_whenParseCitations_thenIgnoresIt`.
10. Gotcha'lar (Boot 3 için yazılmış Spring AI örnekleri, YAML → properties, modeller kapalıyken testler).

`references/indexing.md` bölümleri:
1. İçe alma aşamaları `QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE | FAILED` (lab); iş 202 + `Location`, arka plan executor'ında, commit sonrası.
2. Okuma: PDF sayfa ve bölümleri (kütüphane plan kararı; referans `pdfbox`).
3. Parçalama: `ingestion.target-chunk-chars`, `max-chunk-chars`, `overlap-chars`, `chars-per-token` (lab ayarları); başlık + sayfa etiketi.
4. Embedding: `EmbeddingClient` → Ollama `/api/embed`, toplu (`ingestion.embedding-batch-size`), model etiketi `bge-m3@1024`, boyut kolonla aynı, farklı model → `EMBEDDING_MODEL_MISMATCH`.
5. Yazma: yeni sürümde eski parçalar tek transaction'da silinip yenileri yazılır; yazma sürerken eski sürümden cevap verilmeye devam edilir (lab).
6. Arama: cosine `<=>`, HNSW, `ef_search` (`search.default-ef-search`), `search.max-scan-tuples`; SQL ayrıntısı `postgres-patterns`.
7. Test: sahte `EmbeddingClient` (deterministik vektör), Testcontainers pgvector imajı.
8. Referans: `../embedding-pgvector/backend` okunur kalıp (M5).

**L-14 `sse-streaming`**
description: "Cevabın Server-Sent Events ile token token akıtılması: assistant-api'de akış endpoint'i, olay adları ve sırası, akış öncesi ve akış içi hata, istemci kopunca model çağrısının iptali, zaman aşımı, proxy tamponlaması; web'de fetch + ReadableStream ile ayrıştırma, AbortController ve MSW ile sahte akış. Akış endpoint'i, eventStream.ts, useAnswerStream ya da bunların testlerini yazarken kullan."

Bölümler:
1. Sözleşme `docs/api/sse.md`; olaylar `token`* → `sources` → `done` | `error` (lab); `data` JSON camelCase.
2. Backend: `SseEmitter` ile `Flux` arasındaki seçim ilk akış task'ının planında yapılır. Tuzaklar: servlet async timeout; `onCompletion`/`onError`/`onTimeout`'ta `dispose`; `done` commit sonrası; `Cache-Control: no-cache`; `X-Accel-Buffering: no`.
3. Akış öncesi hata Problem Details ile döner; akış içi hata `event: error` + `StreamErrorEvent{errorCode}` ile bildirilir; yarım tur `INTERRUPTED` olur.
4. Backend test: `MockMvcTester` + async dispatch; olay sırası ve `data` doğrulanır; iptal testi.
5. Web: `shared/api/eventStream.ts` (satır tamponu, bölünmüş chunk); `useAnswerStream` (`AbortController`, handler'dan başlatma, updater, `done`'da invalidate).
6. Web test: MSW `ReadableStream` (`src/test/sse.ts`), parçalı satır, akış ortasında `error`, iptal.
7. Proxy: Vite ve nginx (`proxy_buffering off`, `proxy_read_timeout`, HTTP/1.1); smoke `curl -sN`.

**L-15 `rag-evaluation`**
description: "RAG değerlendirmesi: golden set (cevabın geçtiği parçası işaretli sorular), recall@k ve MRR hesabı, top-k ile rerank'in karşılaştırılması, değerlendirme çalıştırmasının 202 + polling ile yürütülmesi, deterministik metrik testleri ve gerçek modelle ölçümün yorumlanması. Değerlendirme ekranı, EvalRun, metrik hesabı ya da golden set üzerinde çalışırken kullan."

Bölümler:
1. Ne ölçülür: lab'daki gibi retrieval (doğru parça modele giden 5 parçaya girdi mi). Cevap kalitesi qa'nın gerçek model denemesidir.
2. Golden set: soru + altın parça kimlik(ler)i. Parça kimlikleri retrieval-api'den gelir; yeni sürümde değişir, bu yüzden altın parça sürüme bağlıdır (lab: "rehber 2026.10"). Rehberde cevabı olmayan sorular ayrı işaretlenir.
3. Metrikler, elle hesaplanmış örnekle: 5 soru, altın parçanın sıraları 1, 2, 1, 4, yok → recall@5 = 0,8; MRR = (1 + 0,5 + 1 + 0,25 + 0) / 5 = 0,55.
4. Stratejiler: `TOP_K`, `RERANK`. `LLM_ONLY`'de ölçüm yapılmaz (lab: düğme pasif, "Sadece LLM modunda arama yok" uyarısı). Strateji M4 gereği sunucu ayarıdır.
5. Çalıştırma: `POST /api/assistant/evals` → 202 + `Location`; `EvalRun` durumları; web polling (`EVAL_POLL_INTERVAL_MS`, `evaluationKeys.run(evalRunId)`); lab ekranı (recall@5, MRR, Kaçan, Retrieval kartları; Çalıştırmalar; "Kaçanlar ve sırası değişenler").
6. Test: sahte retrieval sıralamaları; `isCloseTo(…, within(1e-6))`; beklenen değer elle hesaplanır.
7. Yorum: recall düşükse parçalama/retrieval, recall yüksek ama cevap yanlışsa prompt/model; sürüm değişince yeniden ölç.

**L-16 eski klasörler (lead; B-1, B-2, F-1 raporları `DONE` olunca)**
`backend/CLAUDE.md` ve `frontend/CLAUDE.md` silinir (`git rm`), `backend/` ve `frontend/` klasörleri kalkar. Gerekçe: Kararlar #5. Kaldırmadan önce lead, eski dosyalardaki her bölümün yeni dosyalarda karşılığı olduğunu coder raporlarındaki "Yapılanlar" tablosundan kontrol eder.

### backend-coder
**B-1 `assistant-api/CLAUDE.md` (yeni; eski `backend/CLAUDE.md`'den taşınır)**
Bölüm yapısı eski dosyayla aynıdır: yorum bloğu, Teknoloji, Paket yapısı, Komutlar, Dosya sahipliği, Kurallar, Spring Boot 4 tuzakları, Hangi iş → hangi skill.
1. Başlık `# assistant-api (Spring Boot + Spring AI)`. Yorum: "assistant-api/ altında bir dosya okununca yüklenir … Durum: henüz kod yok; iskelet ilk assistant-api task'ında kurulur."
2. Teknoloji:
   - Eski maddeler korunur (Java 26, Boot 4.1, `./mvnw`, kök paket `com.rasitbank.assistant`, `AssistantApplication`, Spring AI, lokal modeller).
   - "Vektörler bu repoda tutulursa …" cümlesi → "Veritabanı `assistant` (M7); vektör yok."
   - Yeni madde: "retrieval-api'ye `RetrievalClient` ile gider: `assistant.retrieval.base-url` (hedef `http://localhost:8080`), `assistant.retrieval.workspace-id` (42, M6)."
   - Referans satırı → "`../embedding-pgvector/backend`: aynı yığının sürümleri ve test/kalite araçları için okunur kalıp referansı; değiştirilmez".
3. Paket yapısı: `backend-conventions` bölüm 3 assistant-api ağacı; `retrieval/client/` (`RetrievalClient`, `RerankClient`).
4. Komutlar: "Hepsi `assistant-api/` içinde"; port 8090 (`server.port`); "Testler lokal modeller ve retrieval-api kapalıyken geçer"; Testcontainers pgvector imajı (Kararlar #4).
5. Dosya sahipliği: aynı tablo (yollar servise göre).
6. Kurallar: sözleşme `docs/api/openapi.yaml`'ın `/api/assistant/**` kısmı; `RetrievalClient` `/api/workspaces/{workspaceId}/search` kısmına uyar; SSE `docs/api/sse.md`.
7. Spring Boot 4 tuzakları: aynı (Spring AI maddesi dahil).
8. Hangi iş → hangi skill: eski tablo + "Dil modeli, prompt, sorgu yeniden yazma, retrieval/rerank istemcisi → `rag-pipeline`", "Cevap akışı (SSE), iptal → `sse-streaming`, `java-concurrency`", "Değerlendirme → `rag-evaluation`". "JPA, PostgreSQL, pgvector" satırı → "JPA, PostgreSQL, migration".

**B-2 `retrieval-api/CLAUDE.md` (yeni)**
Bölüm yapısı B-1 ile aynıdır.
1. Başlık `# retrieval-api (Spring Boot + pgvector)`. Yorum: "retrieval-api/ altında … Durum: henüz kod yok; iskelet ilk retrieval-api task'ında kurulur."
2. Teknoloji (hepsi "Hedef:"):
   - Java 26, Boot 4.1, `./mvnw`.
   - Kök paket `com.rasitbank.retrieval`, `RetrievalApplication`.
   - PostgreSQL 18 + pgvector 0.8.1 (imaj `pgvector/pgvector:0.8.1-pg18`, lab; testlerde de aynısı), `hibernate-vector`, Flyway (`src/main/resources/db/migration`), `ddl-auto=validate`, `open-in-view=false`; veritabanı `retrieval` (M7).
   - Embedding: Ollama `http://localhost:11434`, `bge-m3`, 1024 boyut (`embedding.base-url`, `embedding.model`, `embedding.dimensions`).
   - Arama ve içe alma ayarları (lab): `search.default-ef-search`, `search.max-scan-tuples`, `ingestion.embedding-batch-size`, `ingestion.target-chunk-chars`, `ingestion.max-chunk-chars`, `ingestion.overlap-chars`, `ingestion.chars-per-token`.
   - PDF okuma kütüphanesi plan kararıdır (referansta `pdfbox`).
   - Referans: `../embedding-pgvector/backend` okunur kalıp referansıdır (içe alma hattı, parçalama, HNSW sorguları); kod toplu kopyalanmaz, bu projenin ihtiyacına göre yeniden yazılır; çalışma zamanında ona bağlanılmaz.
   - Henüz olmayanlar: hepsi.
3. Kapsam (lab): M6; kaynaklar ve sürümleri, içe alma işleri (aşamalar `QUEUED…DONE`/`FAILED`), parçalar, vektör arama (`mode: VECTOR`, `limit`). Arama kaydı ve embedding deneme ucu yoktur (Kararlar #8, Ç-2).
4. Paket yapısı: `backend-conventions` bölüm 3 retrieval-api ağacı.
5. Komutlar: "Hepsi `retrieval-api/` içinde"; port 8080; "testler Ollama kapalıyken geçer (`EMBEDDING_BASE_URL=http://127.0.0.1:9 ./mvnw verify` kalıbı)"; Docker açık olmalı (Testcontainers).
6. Dosya sahipliği: B-1 ile aynı.
7. Kurallar: sözleşme `docs/api/openapi.yaml`'ın `/api/workspaces/**` kısmı; diğer maddeler B-1 ile aynı.
8. Tuzaklar: B-1'in Boot 4 listesi (Spring AI maddesi hariç) + "`CREATE EXTENSION IF NOT EXISTS vector` ilk migration'da", "`IDENTITY` id'de batch insert yok; toplu yazılan `Chunk` `SEQUENCE`", "vektör kolon boyutu `embedding.dimensions` ile aynı".
9. Hangi iş → hangi skill: "İçe alma, parçalama, embedding, arama → `rag-pipeline/references/indexing.md`, `postgres-patterns`, `sql-optimization-patterns`", "Arka plan içe alma işi, executor → `java-concurrency`, `spring-transactions`", + B-1'deki ortak satırlar (değerlendirme ve akış hariç).

### frontend-coder
**F-1 `web/CLAUDE.md` (yeni; eski `frontend/CLAUDE.md`'den taşınır)**
1. Başlık `# web (React yönetim paneli)`. Yorum: "web/ altında … Durum: henüz kod yok; iskelet ilk web task'ında Vite react-ts şablonuyla kurulur."
2. Başlıktan sonra `## Ürün hedefi`: M1 + M4.
3. Teknoloji:
   - Eski maddeler korunur.
   - Proxy maddesi → "Hedef: Vite dev server `/api/assistant`'ı `http://localhost:8090`'a (assistant-api), `/api/workspaces`'ı `http://localhost:8080`'e (retrieval-api) proxy'ler (M3); SSE için proxy tamponlamamalıdır (`vite-patterns`, `sse-streaming`)."
   - Referans satırı → "`../embedding-pgvector/frontend`: Bilgi kaynakları, yükleme ve içe alma ekranlarının çalışan örneği; okunur kalıp referansı, değiştirilmez".
   - `kaynaklar/rag-lab.html` maddesi → "`kaynaklar/rag-lab.html`'in sol yarısı ürünün kendisidir (Ürün hedefi); `kaynaklar/rag.html` anlatımdır. Değiştirilmez."
4. Klasör yapısı: ağaç kökü `web/`. "Hedef adresler (lab)" maddesine "Adreste çalışma alanı yoktur (M6). Bilgi kaynakları çağrıları `/api/workspaces/42/...`." eklenir.
5. Komutlar: "Hepsi `web/` içinde". E2E maddesi aynı (3000 / 5173).
6. Dosya sahipliği: aynı tablo.
7. Kurallar: "API type'ları ve çağrıları `docs/api/openapi.yaml` ile (iki servis, path önekleri)".
8. Tuzaklar: aynı.
9. Hangi iş → hangi skill:
   - "Server state, cache, akan cevap" satırına `sse-streaming` eklenir.
   - "Değerlendirme ekranı → `rag-evaluation`" eklenir.
   - "Yeni ekran ya da görsel tasarım" → "Lab'dan ekran ve token aktarma, lab'da karşılığı olmayan durum → `frontend-design`, `web-design-guidelines`".

### devops
**D-1 `deployment/CLAUDE.md`**
1. Yorum → "Durum: henüz compose dosyası yok. Aşağıdaki hedef yapı TASK-001 kararlarına göredir (M2, M3, M7); devops ilk Docker task'ında gerçek komut, port ve imajlarla değiştirir."
2. "Hedef servisler" tablosu → M3 tablosu; altına geliştirme paragrafı (M3) ve M7.
3. "retrieval-api'nin bu compose'a nasıl gireceği … ilk task'ta planner'ın kararıdır." → "`build.context`: `../assistant-api`, `../retrieval-api`, `../web`."
4. "Hedef ortamlar …" → "tam sistem (`docker-compose.yml`, site http://localhost:3000) ve geliştirme (`docker-compose.dev.yml`: postgres ve pgAdmin; servisler ve web bilgisayarda)". Referans atfı kalkar.
5. Port maddesi → "`../embedding-pgvector`'un compose'u aynı portları kullanır; iki proje aynı anda açılmaz, önce diğeri `down` edilir."
6. Dosya sahipliği tablosu: "`backend/Dockerfile`, `frontend/Dockerfile`, `*/.dockerignore`, `frontend/nginx.conf`" → "`assistant-api/Dockerfile`, `retrieval-api/Dockerfile`, `web/Dockerfile`, `*/.dockerignore`, `web/nginx.conf`". "devops `../embedding-pgvector`'a yazmaz" maddesi kalır.
7. Tuzaklar:
   - "Düz `postgres` imajında `vector` eklentisi yoktur …" → "retrieval-api `vector` eklentisi ister; ortak postgres servisi pgvector'lü imajla çalışır."
   - Yeni: "İki veritabanı ve kullanıcı, postgres container'ının ilk açılışında `/docker-entrypoint-initdb.d` script'iyle oluşturulur (sadece boş volume'da çalışır; değişince `down -v`)."
   - Yeni: "nginx'te iki `location`: `/api/assistant/` → assistant-api (akış yolunda tampon kapalı), `/api/workspaces/` → retrieval-api (dosya yükleme sınırı `client_max_body_size` retrieval-api'ninkiyle uyumlu)."
   - "Ollama ve reranker Mac'te" maddesi: dil modeli assistant-api'den, embedding retrieval-api'den kullanılır; iki servis de `extra_hosts` ister.
   - SPA fallback örneği `/konusmalar/12` kalır.
8. Komutlar: "Lokal modeller" maddesine "reranker: `curl -s localhost:8081/health`"; "`ollama.md`'dedir" → "… (ilk model task'ında oluşturulur)"; log örneği `… logs --tail=100 assistant-api`.

**D-2 kök `README.md`**
1. Giriş: "`../embedding-pgvector` projesinin devamıdır." → "Önceki projenin (embedding ve pgvector) devamıdır; bilgi bankası bu repoda yeniden yazılır." (M5: README'de yol atfı yok.)
2. Durum notunun altına: "**Gereksinimler (hedef):** Docker Desktop, Ollama, llama-server (reranker); local geliştirme için Java 26 ve Node 26."
3. `## Ne geliştiriliyor`: M1'in ilk iki cümlesi + "Modüller: `web/`, `assistant-api/`, `retrieval-api/`, `deployment/`."
4. Agent ekibi tablosu:
   - `backend-coder`: "Planın backend kısmını `assistant-api` (Spring Boot + Spring AI) ve `retrieval-api` (Spring Boot + pgvector) içinde uygular. Sadece bu iki klasöre yazar, test yazmaz."
   - `frontend-coder`: "… Sadece `web/` altına yazar …".
   - `devops`: "Dockerfile ve docker-compose dosyalarını, `ollama.md`'yi ve bu README'yi yazar; …".
5. "Ayrıntılar" satırı: `KULLANIM-KILAVUZU.md`; `ollama.md` henüz yok, adı geçmez.

## Backend planı
Kod yok. B-1, B-2 (iki `CLAUDE.md`). Ek skill yok.

## Frontend planı
Kod yok. F-1 (`web/CLAUDE.md`). Ek skill yok.

## Test senaryoları
Test kodu yazılmaz (Kural 11). Aşağıdakiler qa'nın repo kökünden çalıştıracağı kontrollerdir. `$DOCS` = `.claude CLAUDE.md assistant-api/CLAUDE.md retrieval-api/CLAUDE.md web/CLAUDE.md deployment/CLAUDE.md rag.md README.md KULLANIM-KILAVUZU.md docs/tasks/_TEMPLATE.md`.

| # | Senaryo | Kontrol | Beklenen sonuç |
|---|---------|---------|----------------|
| K1 | Envanter eksiksiz | `diff <(cd ../embedding-pgvector/.claude && find . -type f ! -name .DS_Store \| sort) <(cd .claude && find . -type f ! -name .DS_Store \| sort)` | `<` satırı yok; `>` satırları tam olarak `./skills/rag-evaluation/SKILL.md`, `./skills/rag-pipeline/SKILL.md`, `./skills/rag-pipeline/references/indexing.md`, `./skills/sse-streaming/SKILL.md` |
| K2 | Klasör yapısı | `ls -a handoff docs/tasks; ls assistant-api retrieval-api web deployment` | `.gitkeep`'ler var; dört modülde sadece `CLAUDE.md` |
| K3 | `../embedding-pgvector` atıfları gerekçeli | `grep -rn "embedding-pgvector" $DOCS docs/api` | Her satır M5'teki bir dosya ve atıf türüne uyar; `README.md`, `KULLANIM-KILAVUZU.md`, `_TEMPLATE.md`, `docs/api` için 0 |
| K4 | Referans işi kalmadı | `grep -rniE "search[-_]events?\|arama kayd\|sık aranan\|popular[- ]?quer\|Atlaskap\|Belgelerim\|Dalga Fiber\|Fatura SSS\|Yardım merkezi\|Yıllık izin\|Şifremi unuttum\|com\.embedding\|EmbeddingPgvector\|embedding_pgvector\|(^\|[^-])lab\.html\|Retrieval Lab\|/w/[0-9:\$]\|arama-kayitlari\|belge-yukle\|workspaceWithSources\|MAX_SOURCES_PER_WORKSPACE\|SOURCE_LIMIT_REACHED\|SOURCE_NAME_TAKEN\|uk_sources_workspace_id_name\|name_key\|/api/embeddings\|embedding API\|CreateSourceForm" $DOCS` ve `grep -rnE "\bSearchEvent" $DOCS` | İkisi de 0 |
| K5 | Çok çalışma alanı kalmadı | `grep -rniE "WorkspaceSelect\|WorkspaceLayout\|WorkspaceHomeRedirect\|workspaceKeys\|çalışma alanı (seç\|oluştur\|başına)\|başka kiracı\|kiracının" $DOCS` ; ayrıca `grep -rniE "workspace\|çalışma alan" web/CLAUDE.md .claude/skills/{frontend-*,react-*,e2e-testing,vite-patterns}` | İlki 0; ikincideki her satır `/api/workspaces/42` adresi, M6 metni ya da sözlük satırı (`vite-patterns` "workspace kökü" genel anlam) |
| K6 | Ürün hedefi yazılı | `grep -lF "sol yarısının aynısı" rag.md CLAUDE.md web/CLAUDE.md .claude/skills/frontend-design/SKILL.md .claude/skills/frontend-conventions/SKILL.md .claude/skills/e2e-testing/SKILL.md`; `grep -c "ekrana gelmez"` aynı dosyalarda | 6 dosya; her birinde ≥ 1 |
| K7 | Modül/bilgi bankası kararı işlendi | `grep -rnE "ilk task'ta planner\|planner'ın kararıdır\|kart sorusu\|karara bağlanır" $DOCS`; `grep -lF 'bu projeye göre yeniden yazılır' CLAUDE.md rag.md .claude/skills/backend-conventions/SKILL.md`; `grep -lF 'tek bir çalışma alanı vardır' rag.md retrieval-api/CLAUDE.md` | İlki 0; ikincisi 3 dosya; üçüncüsü 2 dosya |
| K8 | Frontmatter geçerli | `for f in .claude/skills/*/SKILL.md; do d=$(basename "$(dirname "$f")"); [ "$(head -1 "$f")" = "---" ] \|\| echo "FM $f"; grep -qE "^name: $d$" "$f" \|\| echo "NAME $f"; grep -qE "^description:" "$f" \|\| echo "DESC $f"; done; for f in .claude/agents/*.md; do n=$(basename "$f" .md); grep -qE "^name: $n$" "$f" && grep -qE "^description:" "$f" && grep -qE "^tools:" "$f" \|\| echo "AGENT $f"; done` | Çıktı yok |
| K9 | `settings.json` geçerli ve yeni yollar | `python3 -I -m json.tool .claude/settings.json >/dev/null && echo OK; grep -cE 'Edit\(/(assistant-api\|retrieval-api\|web)/\*\*\)\|curl -sN localhost\|Edit\(/ollama.md\)\|Edit\(/README.md\)' .claude/settings.json; grep -cE '/(backend\|frontend)/' .claude/settings.json` | `OK`; `6`; `0` |
| K10 | Handoff adları | `grep -rhoE "handoff/0[0-5]-[a-z-]+\.md" CLAUDE.md .claude/agents KULLANIM-KILAVUZU.md \| sort -u` | Tam 7 ad (önceki planla aynı) |
| K11 | Agent yolları = kök tablo | Her agent'ın "Sadece … yazarsın" satırı kök tablonun "Yazar" sütunuyla karşılaştırılır; `grep -n "ollama.md\|README.md" .claude/agents/devops.md`; `grep -n "03-test-report" .claude/agents/devops.md` | Altısı aynı; devops'ta `ollama.md`, `README.md` var; 03 koşulu "tester plandaysa" |
| K12 | Portlar tutarlı | `grep -rnoE "localhost:[0-9]+\|:(8080\|8081\|8090\|11434\|3000\|5173\|5432\|5050)\b" $DOCS` | Her port M3'teki anlamında: 8090 assistant-api, 8080 retrieval-api, 8081 reranker, 11434 Ollama, 3000 site, 5173 Vite, 5432 postgres, 5050 pgAdmin |
| K13 | Yeni skill'ler bağlı | `for s in rag-pipeline sse-streaming rag-evaluation; do echo "$s: $(grep -rl "$s" .claude/agents assistant-api/CLAUDE.md retrieval-api/CLAUDE.md web/CLAUDE.md .claude/skills/backend-conventions .claude/skills/frontend-conventions \| wc -l)"; done` | Her biri ≥ 2 |
| K14 | Kılavuzdaki yollar var | ``grep -oE '`[^`]+\.(md\|json)`' KULLANIM-KILAVUZU.md README.md \| tr -d '`' \| grep -v XXX \| sort -u \| while read p; do [ -e "$p" ] \|\| echo "YOK: $p"; done`` | Çıktı yok (ya da sadece "(ilk model task'ında oluşturulur)" diye anılan `ollama.md`) |
| K15 | Açıklamalar bu projeye göre | `for f in .claude/skills/*/SKILL.md; do awk 'NR>1 && /^---$/{exit} {print FILENAME": "$0}' "$f"; done \| grep -niE "embedding API\|kaynak limit\|çalışma alan\|arama sonucu vurgu\|(backend\|frontend)/"` | 0 satır |
| K16 | Dil kuralı | Değişen kod örneklerinden örneklem (L-12.7, L-12.17, L-13…L-15) | Türkçe identifier yok (`<parcalar>` ve UI metinleri hariç) |
| K17 | Uygulayan doğru | `02-backend-implementation.md`, `02-frontend-implementation.md`, `04-deploy-report.md` "Değişen dosyalar" | Sırasıyla: `assistant-api/CLAUDE.md` + `retrieval-api/CLAUDE.md`; `web/CLAUDE.md`; `deployment/CLAUDE.md` + `README.md` |
| K18 | Kod/Docker/test yok, eski klasörler kalktı | `git status --porcelain -- assistant-api retrieval-api web deployment \| grep -v CLAUDE.md`; `test ! -e backend && test ! -e frontend && echo OK` | İlki 0 satır; `OK` |
| K19 | Eski yollar kalmadı | `grep -rnE "(^\|[^a-z/-])(backend\|frontend)/\|cd (backend\|frontend)\b" $DOCS .gitignore` | 0 satır (`../embedding-pgvector/backend` gibi M5 atıfları `/` ile başladığı için eşleşmez) |
| K20 | Paket adları servise göre | `grep -rnE "com\.rasitbank\.assistant\.(workspace\|source\|chunk\|ingestion\|search\|embedding)" .claude` | 0 satır |

## Deployment planı
Yok. devops sadece D-1 ve D-2'yi yazar; Docker komutu, smoke test ve model indirme yok.

## Açık sorular ve riskler
- **Hacim:** ~60 dosyada değişiklik var ve yol değişikliği (Ç-1) neredeyse her dosyaya dokunuyor. qa ilk turda kalıntı bulursa sorumlu "lead" olarak `NEEDS_FIX` yazar. K4, K5, K15, K19 ve K20 bunun için var.
- **Kaynak oluşturma ekranı yok:** lab'ın sol yarısında "kaynak ekle" ekranı yok, sadece "Yeni sürüm yükle" var. Rehberin ilk kez nasıl yükleneceği (seed migration, komut ya da gizli bir uç) ilk retrieval-api task'ının sorusu. Bu task'ı etkilemez.
- **Spring AI ayrıntıları:** yeni skill'ler kod yokken yazılıyor; Spring AI ayrıntıları iskelet task'ında doğrulanmalı.
- **Lead görev listesi:** #4-#7 bu plana göre yeniden adlandırılmalı (B-1/B-2, F-1, D-1/D-2, K1-K20). L-16 için yeni bir görev açılmalı; bu görev coder görevlerine bağlıdır.

STATUS: DONE
