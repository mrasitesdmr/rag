TASK: TASK-002
# Deploy raporu

## Değişen dosyalar
- `assistant-api/Dockerfile`, `retrieval-api/Dockerfile`, `web/Dockerfile`
- `assistant-api/.dockerignore`, `retrieval-api/.dockerignore`, `web/.dockerignore`
- `web/nginx.conf`
- `deployment/docker-compose.yml`, `deployment/docker-compose.dev.yml`, `deployment/postgres/initdb/01-init-databases.sh`
- `deployment/CLAUDE.md`, `README.md`
- Uygulama koduna dokunulmadı. Lokal model gerekmediği için `ollama.md` yazılmadı.

## Servisler
Komut: `docker compose -f deployment/docker-compose.yml down -v && … up --build -d` (boş volume, G1), sonra `ps`.

| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|--------|---------------|------|--------------------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| assistant-api | rag-assistant-api (temurin 26 jre-noble, uid app) | 8090 | Up (healthy) |
| retrieval-api | rag-retrieval-api (temurin 26 jre-noble, uid app) | 8080 | Up (healthy) |
| web | rag-web (nginx-unprivileged 1.29.8-alpine, uid nginx) | 3000→8080 | Up (healthy) |

## Smoke test
- D-S1: dört konteyner `healthy` (boş volume ile).
- `curl -si localhost:3000/onizleme` → 200 `text/html`; `/kaynaklar` → 200 `text/html` (SPA fallback).
- D-S4: `/api/assistant/yok` → 404 `application/problem+json`, `errorCode":"NOT_FOUND"` (assistant-api'den, `instance":"/api/assistant/yok"`); `/api/workspaces/42/yok` → 404 problem+json `NOT_FOUND` (retrieval-api'den, `instance` doğru); `/api/yok` → 404 problem+json `NOT_FOUND` (nginx).
- D-S5: `localhost:8090/actuator/health` ve `localhost:8080/actuator/health` → `{"groups":["liveness","readiness"],"status":"UP"}`; `localhost:3000/actuator/health` → 200 `text/html` (SPA HTML'i, UP gövdesi değil).
- D-S2/D-S3 (`exec -e PGPASSWORD=<k>-local postgres psql -h 127.0.0.1 -U <k> -d <db> -c 'select 1'`):
  - assistant → retrieval: `FATAL: permission denied for database "retrieval" / User does not have CONNECT privilege.`
  - assistant → assistant: `select 1` başarılı.
  - retrieval → assistant: `FATAL: permission denied for database "assistant"`.
  - retrieval → retrieval: başarılı.
- `retrieval` DB: `vector` eklentisi kurulu; `flyway_schema_history`: `1 | enable vector extension`. `assistant` DB: `flyway_schema_history` 0 kayıt (migration yok).
- D-S6: `cd web && npx playwright install chromium && npm run test:e2e` → 6 passed (E-1…E-6), http://localhost:3000.
- SSE/WebSocket: bu task'ta yok, denenmedi.
- Not: `docker ps`'te Testcontainers'tan kalan iki konteyner (portlar 50542, 50544) var; port çakışması yok. `../embedding-pgvector` sistemi açık değildi.

## Nasıl çalıştırılır
```
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
docker compose -f deployment/docker-compose.yml logs --tail=100 assistant-api
docker compose -f deployment/docker-compose.yml down        # veriyi sıfırlamak için down -v
```
Site: http://localhost:3000. Model gerekmez.

## Düzeltme
Yok.

STATUS: DONE
