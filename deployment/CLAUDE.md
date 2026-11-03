<!--
AMAÇ: Claude (ve ekip üyeleri) deployment/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
Durum: TASK-002 ile compose dosyaları var (4 servis); TASK-003 ile lokal modeller Mac'te çalışıyor; TASK-027 ile açılışta otomatik içe alma kalktı, belgeler yüklenir. pgAdmin sonraki task'ta eklenir.
-->

# Deployment (Docker)

## Servisler
| Servis | Görevi | Host adresi |
|--------|--------|-------------|
| `web` | nginx: SPA; `/api/assistant/` → `assistant-api:8090`, `/api/workspaces/` → `retrieval-api:8080` | `3000` |
| `assistant-api` | Konuşma, sorgu yeniden yazma, retrieval çağrısı, rerank, prompt, cevap akışı (SSE), geri bildirim, aktarma, değerlendirme | `8090` |
| `retrieval-api` | Kaynak, sürüm, içe alma, parçalama, embedding, vektör arama | `8080` |
| `postgres` | pgvector'lü PostgreSQL (lab: `pgvector/pgvector:0.8.1-pg18`); iki veritabanı: `assistant` (assistant-api) ve `retrieval` (retrieval-api), ayrı kullanıcılarla | `127.0.0.1:5432` |
| `pgadmin` | (henüz yok) Veritabanı arayüzü | `127.0.0.1:5050` |
| Ollama (Mac'te çalışıyor, TASK-003; retrieval-api TASK-005'ten, assistant-api TASK-008'den beri bağlı) | Dil modeli `qwen3.5:9b` (assistant-api), embedding `bge-m3` 1024 boyut (retrieval-api) (lab; kesin modeller kullanıcı onayıyla) | `11434` |
| Reranker, llama-server (Mac'te çalışıyor, TASK-003; assistant-api TASK-008'den beri kullanır; başlatma komutu `ollama.md`'de) | `POST /v1/rerank`, `bge-reranker-v2-m3` Q8_0 | `8081` |

Geliştirme: `cd assistant-api && ./mvnw spring-boot:run` → 8090, `cd retrieval-api && ./mvnw spring-boot:run` → 8080, `cd web && npm run dev` → 5173 (Vite proxy: `/api/assistant` → `http://localhost:8090`, `/api/workspaces` → `http://localhost:8080`).

Tek PostgreSQL sunucusu (pgvector'lü imaj), iki ayrı veritabanı: `assistant` (assistant-api: konuşmalar, turlar, değerlendirme çalıştırmaları) ve `retrieval` (retrieval-api: çalışma alanı, kaynaklar, parçalar ve embedding'leri, içe alma işleri). Her servis kendi kullanıcısıyla sadece kendi veritabanına bağlanır ve kendi Flyway migration'larını taşır; servisler birbirinin tablosunu okumaz, REST ile konuşur. Vektörler sadece `retrieval` veritabanındadır.

- `build.context`: `../assistant-api`, `../retrieval-api`, `../web`.
- Ortamlar: tam sistem (`docker-compose.yml`, site http://localhost:3000) ve geliştirme (`docker-compose.dev.yml`: postgres ve pgAdmin; servisler ve web bilgisayarda).
- `../embedding-pgvector`'un compose'u aynı portları (5432, 5050, 8080, 3000) kullanır; iki proje aynı anda açılmaz, önce diğeri `down` edilir.

## Gerçek yapı (TASK-002)
- Dosyalar: `docker-compose.yml` (proje adı `rag`), `docker-compose.dev.yml` (sadece postgres, volume `pgdata-dev`), `postgres/initdb/01-init-databases.sh`.
- İmajlar: `pgvector/pgvector:0.8.1-pg18`; servisler `maven:3.9-eclipse-temurin-26` ile derlenip `eclipse-temurin:26-jre-noble` üzerinde (uid `app`) çalışır; web `node:26-alpine` ile derlenip `nginxinc/nginx-unprivileged:1.29.8-alpine` (uid nginx, konteyner portu 8080 → host 3000).
- Şifreler (local): `postgres-local`, `assistant-local`, `retrieval-local`; servislere `DATABASE_URL/USER/PASSWORD` ortam değişkeniyle gider.
- Veritabanı ayrımı: `assistant` ve `retrieval` rolleri kendi veritabanının sahibidir; `assistant`, `retrieval` ve `postgres` veritabanlarında `CONNECT` PUBLIC'ten alındı. Çapraz bağlantı `permission denied for database` verir. `vector` eklentisini init script süper kullanıcıyla `retrieval` içine kurar.
- Healthcheck'ler: postgres `pg_isready -h 127.0.0.1` (TCP; init bitmeden healthy olmaz), servisler `curl -sf /actuator/health`, web `wget`. Health gövdesi `{"groups":["liveness","readiness"],"status":"UP"}`.
- nginx: `/api/assistant/` → assistant-api:8090, `/api/workspaces/` → retrieval-api:8080, diğer `/api/` → 404 problem+json `NOT_FOUND`, `/` → SPA fallback. `/actuator` web'den yayınlanmaz (SPA HTML'i döner). Yükleme sınırı TASK-027'de eklendi; SSE tampon ayarları ilgili task'ta eklenecek.
- E2E: sistem açıkken `cd web && npx playwright install chromium && npm run test:e2e` (`E2E_BASE_URL` varsayılan http://localhost:3000).

## Gerçek yapı (TASK-003)
- `docker-compose.yml`: assistant-api'ye `OLLAMA_BASE_URL`, `CHAT_MODEL`, `RERANKER_URL`, `RERANKER_MODEL`; retrieval-api'ye `OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS`; ikisine de `extra_hosts: host.docker.internal:host-gateway`. Adresler `http://host.docker.internal:11434` ve `:8081`. Servis kodu bunları henüz okumaz.
- Reranker Compose'da değildir: Mac'te Homebrew llama-server (`ollama.md`), ön planda ayrı terminal, `--host 127.0.0.1` (container erişimi bu şekilde doğrulandı). Ollama `brew services start ollama` ile `127.0.0.1:11434`.
- llama-server `-hf` modeli `~/.cache/huggingface/hub/`'a indirir (llama.cpp önbelleği değil).
- Model kapalıyken dört servis `healthy` kalır (doğrulandı).

## Gerçek yapı (TASK-005, TASK-027 ile güncel)
- Açılışta otomatik içe alma yoktur (TASK-027); rehber klasörü bağlantısı ve `INGESTION_BOOTSTRAP_*` ayarları kalktı. Açılışta servis sadece yarıda kalan işleri `FAILED` + `INGESTION_INTERRUPTED` yapar.
- Ollama Compose'dan önce açık olmalı (yükleme sırasında embedding gerekir). Kapalıyken yüklenen belgenin işi `FAILED` + `EMBEDDING_UNAVAILABLE` olur; Ollama açılınca yeniden dene (aşağıda).
- Ollama'ya ulaşılamama denemesi: OLLAMA_BASE_URL'i geçersiz veren geçici override dosyasıyla (`-f docker-compose.yml -f override.yml`); Ollama'yı durdurmaya gerek yok.
- Port 5432: dev postgres (`rag-dev`) açıkken tam sistem açılamaz; önce `docker compose -f deployment/docker-compose.dev.yml stop`.

## Gerçek yapı (TASK-027)
- Açılışta bilgi bankası **boştur**: `curl localhost:8080/api/workspaces/42/sources` → `content: []`.
- Belge yükleme (`version` zorunlu), doğrudan retrieval-api ya da web adresi (nginx) üzerinden:
  - `curl -F file=@rehber.pdf -F name="Ürün ve Hizmet Rehberi" -F version=2026.10 localhost:8080/api/workspaces/42/sources`
  - `curl -F file=@rehber.pdf -F name="Ürün ve Hizmet Rehberi" -F version=2026.10 localhost:3000/api/workspaces/42/sources`
  - Cevap 202 + `Location` (iş adresi); durum: `curl localhost:8080/api/workspaces/42/ingestion-jobs/<id>`.
- Yeniden dene (tamamlanmış sürümü olmayan, işi sürmeyen kaynak): `curl -X POST localhost:8080/api/workspaces/42/sources/<id>/ingestion-jobs`.
- nginx `/api/workspaces/` location'ında `client_max_body_size 21m`; retrieval-api sınırı 20 MB (aşılırsa 413 `FILE_TOO_LARGE`, problem+json).
- **Eski rehber verisinin temizliği (TASK-005'ten kalan kaynak 10001 ve parçaları):** tam sistem için `docker compose -f deployment/docker-compose.yml down -v`; geliştirme postgres'i için `docker compose -f deployment/docker-compose.dev.yml down -v`. Bu komutlar veritabanını kalıcı siler; sadece kullanıcı onayıyla çalıştırılır.

## Gerçek yapı (TASK-012)
- Yeni servis, ortam değişkeni ya da model yok; imajlar yeniden derlendi. nginx `/api/assistant/` location'ında `client_max_body_size 2m` (en büyük geçerli öneri listesi ~1,2 MB; 3 MB gövde 413 döner).
- Öneri listesi açılışta **boştur**. Yükleme/okuma/temizleme (web adresi üzerinden): `curl -X PUT localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' --data-binary @suggestions.json`; `curl localhost:3000/api/assistant/suggestions`; temizleme gövdesi `{"categories":[]}`. Dosya biçimi ve kurallar `README.md` "Önerileri yükleme" ve `docs/api/openapi.yaml` `ReplaceSuggestionsRequest`.

## Ayarlar
- Değerler compose dosyalarında düz yazılır; şifreler sadece local kullanım içindir (dosya başında "local use only" yorumu). Dockerfile'a ya da imaja gömülmez. Gerçek gizli bilgi bu yapıya girmez.
- Model adları ve adresleri servislere ortam değişkeniyle verilir (`assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md`).

## Komutlar
Hepsi repo kökünden çalıştırılır. - Önce kontrol: `docker info`. Hata verirse Docker Desktop kapalıdır; kendin düzeltmeye çalışma, lead'e bildir.
- Lokal modeller: `curl -s localhost:11434/api/tags` (Ollama açık mı, modeller indirilmiş mi). Reranker: `curl -s localhost:8081/health`. Model indirme ve reranker başlatma komutları `ollama.md`'dedir.
- Tam sistem: `docker compose -f deployment/docker-compose.yml up --build -d`; durum `… ps` (tüm servisler `healthy`); log `… logs --tail=100 assistant-api`; durdur `… down`; veriyi sıfırla `… down -v`.

## Dosya sahipliği
| Yol | Sahibi |
|-----|--------|
| `deployment/**`, `ollama.md`, kök `README.md` | devops |
| `assistant-api/Dockerfile`, `retrieval-api/Dockerfile`, `web/Dockerfile`, `*/.dockerignore`, `web/nginx.conf` | devops |

- devops uygulama koduna (`src/`, `pom.xml`, `package.json`) dokunmaz. Container'da çalışmak için kod değişikliği gerekiyorsa ilgili coder'a ve lead'e mesaj atar.
- devops `../embedding-pgvector`'a yazmaz; oradaki bir dosyanın değişmesi gerekiyorsa lead'e bildirir.

## Tuzaklar
- Compose dosyasındaki `build.context` yolları compose dosyasına göre çözülür: `../assistant-api`, `../retrieval-api`, `../web`.
- Her servisin `healthcheck`'i vardır; bağımlı servis `depends_on: condition: service_healthy` ile bekler. Healthcheck komutu imajda gerçekten bulunan bir araçla yazılır; JRE imajında `curl` olmayabilir, önce kontrol et. postgres için `pg_isready`.
- PostgreSQL 18 imajlarında veri dizini `/var/lib/postgresql/18/docker`'dır; volume `/var/lib/postgresql`'e bağlanır (`/var/lib/postgresql/data`'ya bağlamak 18'de hata verir).
- Düz `postgres` imajında `vector` eklentisi yoktur; retrieval-api `vector` eklentisi ister, ortak postgres servisi pgvector'lü imajla çalışır.
- İki veritabanı ve kullanıcı, postgres container'ının ilk açılışında `/docker-entrypoint-initdb.d` script'iyle oluşturulur (sadece boş volume'da çalışır; değişince `down -v`).
- Ollama ve reranker Mac'te çalışır (Docker Mac'te GPU kullanamaz, model çok yavaşlar); dil modeli assistant-api'den, embedding retrieval-api'den kullanılır, iki servis de `extra_hosts` ister. Container'dan erişim `host.docker.internal` + `extra_hosts: host.docker.internal:host-gateway` ile olur. Model kapalıyken de servisler `healthy` olmalıdır (healthcheck modele bağlı değil).
- 16 GB makinede dil modeli, embedding modeli ve reranker birlikte belleği doldurabilir (lab notu: "üç model birlikte swap'e düşüyordu"). Smoke test ve uçtan uca test sürelerini buna göre değerlendir.
- nginx'te iki `location` vardır: `/api/assistant/` → assistant-api (akış yolunda tampon kapalı), `/api/workspaces/` → retrieval-api (dosya yükleme sınırı `client_max_body_size` retrieval-api'ninkiyle uyumlu).
- nginx SSE'yi varsayılan olarak tamponlar; cevap tek seferde gelir. Akış endpoint'inde `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout` uzun, HTTP/1.1 (`proxy_http_version 1.1`, `Connection ''`) ayarlanır; backend ayrıca `X-Accel-Buffering: no` gönderebilir.
- nginx'te `add_header` bir `location` içinde tanımlıysa server düzeyindekileri ezer; güvenlik başlıkları bu yüzden her location'da tekrarlanır.
- nginx'te `/ws` kullanılırsa `Upgrade` ve `Connection` header'ları iletilmezse WebSocket bağlanmaz.
- nginx'te SPA fallback (`try_files $uri /index.html`) yoksa `/konusmalar/12` gibi sayfalar yenilenince 404 verir.
- `.dockerignore` olmadan `node_modules/` ve `target/` build context'e kopyalanır; build yavaşlar ve gereksiz dosyalar imaja girer.
- İmaj etiketleri sabit sürümdür (`latest` değil). Container'lar root olmayan kullanıcıyla çalışır.

## Gerçek yapı (TASK-007)
- Bilgi kaynakları ekranı `localhost:3000/kaynaklar`'da; PDF `/api/workspaces/42/sources/{id}/file` ile mevcut nginx bloğundan geçer (Compose, Dockerfile ve `nginx.conf` değişmedi).
- E2E (`cd web && npm run test:e2e`) gerçek sistemi ve Ollama'yı (`bge-m3`) ister; yükleme testi her koşuda bir `E2E Rehber <zaman>` kaynağı bırakır. Kaynak silme olmadığı için bunlar ancak `down -v` ile (veri siler, sadece kullanıcı onayıyla) kaybolur.
- Ollama kapalıyken servisler `healthy` kalır; yükleme 202 döner, iş `FAILED` + `EMBEDDING_UNAVAILABLE` olur (`failedStage: EMBEDDING`), "Yeniden dene" ucu yeni iş açar.
- Tam sistem ve `docker-compose.dev.yml` ikisi de `127.0.0.1:5432`'yi kullanır; birlikte çalışmaz. Tam sistemden önce `docker compose -f deployment/docker-compose.dev.yml stop` (veri silmez).

## Gerçek yapı (TASK-008)
- assistant-api ortamı: `RETRIEVAL_API_URL=http://retrieval-api:8080`, `RETRIEVAL_STRATEGY` (`rerank` varsayılan, `top5`), `QUERY_REWRITE_ENABLED` (`true` varsayılan; `false` ise takip sorusu yeniden yazılmaz, `sources.queryRewriteStatus=DISABLED`), `GENERATION_MODE` (`rag` varsayılan, `llm` = Sadece LLM; `live-config.generationMode` `RAG`/`LLM_ONLY`), `ASSISTANT_HOST_LABEL` (boşsa süre kartında makine etiketi gizli) + `OLLAMA_BASE_URL`, `CHAT_MODEL`, `RERANKER_URL`, `RERANKER_MODEL`. Etiket ve strateji kabuktan ya da `deployment/.env`'den (repoya girmez) gelir.
- `web/nginx.conf` `/api/assistant/`: `proxy_http_version 1.1`, `Connection ''`, `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 300s`. Doğrulandı: `localhost:3000` üzerinden 72 token ~2,5 sn'ye yayılarak gelir (tek seferde değil).
- Üretim modu (TASK-011): `GENERATION_MODE=llm docker compose -f deployment/docker-compose.yml up -d assistant-api`; geri: değişkensiz aynı komut. Reranker sağlığı 15 sn'de bir `/health`'e yoklanır.
- Strateji değiştirme: `RETRIEVAL_STRATEGY=top5 docker compose -f deployment/docker-compose.yml up -d assistant-api` (eski haline: değişkensiz aynı komut). Yeniden yazmayı kapatma (TASK-010 M2): `QUERY_REWRITE_ENABLED=false docker compose -f deployment/docker-compose.yml up -d assistant-api`; geri açma: değişkensiz aynı komut. Tek servis yeniden oluşturulurken `ps` `healthy` olana kadar bekle; bu sürede web 502 verir.
- **Reranker kapatma (yedek yol):** llama-server'ı Ctrl+C ile durdur → aynı soru → `sources.retrievalStrategy = RERANK_FALLBACK`, cevap akar, assistant-api logunda `WARN Reranker unavailable, falling back to vector order`. Servisler `healthy` kalır.
- **Ollama kapalı denemesi:** `OLLAMA_BASE_URL` geçersiz bir adrese çeviren geçici override dosyasıyla `up -d assistant-api` → akışta `error` olayı `CHAT_MODEL_UNAVAILABLE`, tur `error_code` ile kaydedilir. **retrieval-api kapalı:** `docker compose … stop retrieval-api` → `503 RETRIEVAL_UNAVAILABLE` (Problem Details), tur kaydedilir.
- **Boş bilgi bankası denemesi** veri silmeden: ayrı proje adı (`rag-empty`), kendi volume'u ve farklı web portuyla geçici Compose; iş bitince sadece o projeye `down -v`. Ana `rag_pgdata`'ya dokunulmaz.
- Akış kesilince (istemci kopar) tur `STREAM_CANCELLED` kaydedilir; sunucu kopmayı bir sonraki token gönderiminde fark eder (prefill sürerken birkaç sn gecikebilir).

## Gerçek yapı (TASK-018)
- **Temiz kurulum (ana sistem, `rag` projesi):** `docker compose -f deployment/docker-compose.yml down -v --rmi local` (volume ve proje imajlarını siler; Bash izin sistemi bunu otomatik reddedebilir, kullanıcı çalıştırır) → `… build --no-cache` (~49 sn) → `… up -d` → `… ps` (dördü `healthy`). Taban imajlar (maven, temurin, node, nginx, pgvector) ve modeller önbellekte kalır. README "Kurulum"un başından Önizleme'de ilk cevaba: 3 dk 9 sn (2026-11-03).
- **Veriyi boşaltıp yeniden başlama:** `… down -v` sonra `… up -d`. Acceptance spec dolu bilgi bankasında başlamaz (A0); ayrıca README komut denemesinde aynı rehberin ikinci kez yüklenmesi de kopya kaynak bırakır (silme ucu yok), bu yüzden deneme sonrası `down -v` ve yeniden acceptance gerekir.
- **Acceptance (11 senaryo, gerçek modeller, boş bilgi bankası):** `cd web && E2E_ACCEPTANCE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test e2e/acceptance.spec.ts --reporter=list` (~3 dk; 2026-11-03: 11/11 geçti, 2,9 dk).
- **Log toplama:** koşunun hemen ardından, başka bir komuttan önce (`up -d <servis>` servisi yeniden oluşturur ve logu siler): `docker compose -f deployment/docker-compose.yml logs --no-color --timestamps [servis] > dosya`. Ollama logu `/opt/homebrew/var/log/ollama.log`, reranker logu kullanıcının başlattığı yere göre (`/tmp/llama-reranker.log`).
- **Tester'ın geçici projesi:** `docker compose -p rag-e2e -f deployment/docker-compose.yml up --build -d` (aynı portlar: ana sistem ve dev postgres önce kapatılır); işi bitince `-p rag-e2e … down -v --rmi local`.
- **Ölçüm:** `done` olayındaki süre alanları (`curl -sN`) ve `docker stats --no-stream`, `ps -axo rss,comm`, `sysctl vm.swapusage`, `ollama ps`, `memory_pressure`. Sonuçlar README "Ölçümler"de.
- **Reranker komutunu denemek** ana kopyayı durdurmadan: aynı komut `--port 8082` ile; deneme sonrası PID ile kapatılır.
