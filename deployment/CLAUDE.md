<!--
AMAÇ: Claude (ve ekip üyeleri) deployment/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
Durum: TASK-002 ile compose dosyaları var (4 servis). pgAdmin, Ollama ve reranker bağlantısı sonraki task'larda eklenir.
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
| Ollama (Mac'te, henüz bağlı değil) | Dil modeli `qwen3.5:9b` (assistant-api), embedding `bge-m3` 1024 boyut (retrieval-api) (lab; kesin modeller kullanıcı onayıyla) | `11434` |
| Reranker, llama-server (Mac'te, henüz bağlı değil) | `POST /v1/rerank`, `bge-reranker-v2-m3` (lab) | `8081` |

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
- nginx: `/api/assistant/` → assistant-api:8090, `/api/workspaces/` → retrieval-api:8080, diğer `/api/` → 404 problem+json `NOT_FOUND`, `/` → SPA fallback. `/actuator` web'den yayınlanmaz (SPA HTML'i döner). SSE tampon ayarları ve yükleme sınırı ilgili task'larda eklenecek.
- E2E: sistem açıkken `cd web && npx playwright install chromium && npm run test:e2e` (`E2E_BASE_URL` varsayılan http://localhost:3000).

## Ayarlar
- Değerler compose dosyalarında düz yazılır; şifreler sadece local kullanım içindir (dosya başında "local use only" yorumu). Dockerfile'a ya da imaja gömülmez. Gerçek gizli bilgi bu yapıya girmez.
- Model adları ve adresleri servislere ortam değişkeniyle verilir (`assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md`).

## Komutlar
Hepsi repo kökünden çalıştırılır. - Önce kontrol: `docker info`. Hata verirse Docker Desktop kapalıdır; kendin düzeltmeye çalışma, lead'e bildir.
- Lokal modeller: `curl -s localhost:11434/api/tags` (Ollama açık mı, modeller indirilmiş mi). Reranker: `curl -s localhost:8081/health`. Model indirme komutları `ollama.md`'dedir (ilk model task'ında oluşturulur).
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
