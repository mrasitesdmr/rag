TASK: TASK-005
# Deploy raporu

## Değişen dosyalar
- `deployment/docker-compose.yml`: retrieval-api'ye `../docs/rehber/pdf:/app/rehber:ro` bağlama ve `INGESTION_BOOTSTRAP_FILE`.
- `deployment/CLAUDE.md`, `ollama.md` (§8 "okunuyor" + ilk içe alma notu), `README.md`.
- Dockerfile'lar, nginx, `docker-compose.dev.yml` değişmedi.

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|--------|---------------|------|--------------------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| assistant-api | temurin 26 jre-noble | 8090 | Up (healthy) |
| retrieval-api | temurin 26 jre-noble | 8080 | Up (healthy), uid `app`(999) `/app/rehber` içindeki PDF'i okur (644) |
| web | nginx-unprivileged 1.29.8-alpine | 3000 | Up (healthy) |

## Silinen veri (temiz başlangıç)
- 5432 portunu `rag-dev-postgres-1` (backend-coder'ın lokal dev postgres'i) tutuyordu; tam sistem açılamayacağı için `docker-compose.dev.yml stop` ile **durdurdum, silmedim** (`rag-dev_pgdata-dev` volume'u ve içindeki içe alınmış `retrieval` verisi duruyor). Geri açmak için: `docker compose -f deployment/docker-compose.dev.yml up -d` (önce tam sistemi `down` et).
- Tam sistemin `rag_pgdata` volume'unu `down -v` ile sildim (önceki test verisi). Senaryolar arasında da aynısını yaptım.

## Smoke test / senaryolar
**(1) Temiz ilk açılış (Ollama gerçek, bge-m3):** iş 1 `DONE`, progress 1.0, `embeddedChunks=totalChunks=154`. Süre 8,96 sn (ingestion_jobs finished-started; log `durationMs=8964`). Ortalama token 285 (DB `avg(token_count)`, log `avgTokens=285`). Kaynak 10001 "Raşit Bankası Ürün ve Hizmet Rehberi", sürüm 2026.10, 70 sayfa, 11 bölüm, `chunk_count=154`. `vector_dims` min=max=1024, tek `embedding_model`. Servis ilk açılışta `healthy`, içe alma arka planda.
**(2) Yeniden başlatma** (`restart retrieval-api`): log `Guide bootstrap result=ALREADY_INGESTED`; sources=1, ingestion_jobs=1, chunks=154, source_versions=1 (değişmedi).
**(3) Ollama ulaşılamaz:** Ollama'yı durdurmadım; geçici override ile retrieval-api'ye `OLLAMA_BASE_URL=http://host.docker.internal:9` verdim (override dosyası scratchpad'de, repoya girmedi). Temiz DB'de açılış: servis `healthy`; iş 1 `FAILED`, `failedStage=EMBEDDING`, `errorCode=EMBEDDING_UNAVAILABLE` (3 deneme, ConnectException, ~3,4 sn); sources=1 (10001), chunks=0, source_versions=0. Gerçek URL ile `up -d retrieval-api` sonrası: iş 2 `DONE`, 154 parça, sources hâlâ tek (10001); iş 1 FAILED olarak geçmişte kaldı (beklenen, kart kararı).
**(3b) INGESTION_INTERRUPTED:** `ingestion_jobs`'a EMBEDDING aşamasında yarım bir satır ekleyip (SQL ile benzetim; gerçek kesinti değil) servisi yeniden başlattım: log `Interrupted ingestion jobs marked failed count=1`; iş 3 `FAILED`, `failedStage=EMBEDDING`, `errorCode=INGESTION_INTERRUPTED`; rehber zaten girili olduğundan yeni iş yok (`ALREADY_INGESTED`).
**(4) Loglar:** retrieval-api ve assistant-api loglarında, DB'den alınan 3 parçanın 40 karakterlik metinleri aranınca 0 eşleşme; "kredi kart/parmak/Hesab" araması 0. Loglarda yalnız jobId, sourceId, aşama, parça sayısı, token ortalaması, süre var.
**Proxy / diğer:** `curl localhost:3000` 200; `localhost:3000/api/workspaces/42/ingestion-jobs/2` iş JSON'u; `/ingestion-jobs/999` 404 `INGESTION_JOB_NOT_FOUND`; `/api/assistant/nothing` 404 problem+json `NOT_FOUND` (assistant-api'de henüz uç yok); `localhost:8090/actuator/health` UP. SSE/WebSocket bu task'ta yok.

## Nasıl çalıştırılır
```bash
brew services start ollama          # Compose'dan ÖNCE (bge-m3 inmiş olmalı: curl -s localhost:11434/api/tags)
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
curl localhost:8080/api/workspaces/42/ingestion-jobs/1     # stage: DONE
docker compose -f deployment/docker-compose.yml logs --tail=100 retrieval-api
docker compose -f deployment/docker-compose.yml down       # (-v: veriyi sıfırla)
```
Ollama kapalıyken açıldıysa: Ollama'yı aç, `docker compose -f deployment/docker-compose.yml restart retrieval-api`.

## Sistem durumu (qa için)
Sistem **açık**, localhost:3000. Temiz yeniden başlatıldı: iş 1 DONE, kaynak 10001, 154 parça. Dev postgres (`rag-dev`) durdurulmuş durumda (5432 tam sisteme ait).

## Notlar
- Reranker bu task'ta gerekmiyor (llama-server kontrol edilmedi).
- INGESTION_INTERRUPTED gerçek süreç kesintisiyle değil SQL ile benzetilerek denendi.

## Düzeltme
Yok.

STATUS: DONE
