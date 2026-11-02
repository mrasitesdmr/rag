TASK: TASK-010
# Deploy raporu

## Değişen dosyalar
- `deployment/docker-compose.yml`: assistant-api ortamına `QUERY_REWRITE_ENABLED: ${QUERY_REWRITE_ENABLED:-true}`.
- `deployment/CLAUDE.md`: değişken ve açma/kapama komutu (M2).
- `ollama.md`: §8 değişken tablosuna `QUERY_REWRITE_ENABLED` satırı.
- `README.md`: Önizleme paragrafına kapatma notu.
- Dockerfile'lara dokunulmadı; yeni model yok (`qwen3.5:9b` yeterli).

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|---|---|---|---|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| retrieval-api | rag-retrieval-api (build) | 8080 | Up (healthy) |
| assistant-api | rag-assistant-api (build) | 8090 | Up (healthy) |
| web | rag-web (build, nginx) | 3000 | Up (healthy) |

Ön koşul: Ollama açık (`qwen3.5:9b` yüklü), llama-server `localhost:8081/health` → `{"status":"ok"}`.

## Smoke test
- `docker compose -f deployment/docker-compose.yml up --build -d` → 4 servis `healthy`.
- Flyway (assistant DB): `select version,success from flyway_schema_history` → V1 ve V2 `t`.
- `curl localhost:3000` → 200.
- `curl localhost:3000/api/workspaces/42/sources` → 200, `{"content":[{"sourceId":10001,...` (retrieval-api).
- `POST localhost:3000/api/assistant/conversations` `{"channel":"PREVIEW"}` → konuşma açıldı (assistant-api).
- SSE proxy üzerinden: `curl -sN` ile `token` olayları 0,1-0,2 sn aralıkla parça parça geldi (tamponlama yok).
- Ayar ulaşıyor mu (konteyner env + gerçek model, çift: "Kartımı kaybettim, ne yapmalıyım?" → "Yenisi kaç günde gelir?"):
  - Varsayılan/açık (`env`: `QUERY_REWRITE_ENABLED=true`): 1. tur `NO_HISTORY`; 2. tur `REWRITTEN`, `searchQuery` = "Kartımı kaybettim, yeni kartım ne zaman ve hangi süre içinde adresime gönderilir?".
  - Kapalı (`QUERY_REWRITE_ENABLED=false`): 1. ve 2. tur `DISABLED`, `searchQuery` = sorunun kendisi ("Yenisi kaç günde gelir?").
  - Tekrar açık (değişkensiz komut): yine `NO_HISTORY` → `REWRITTEN`.

## Nasıl çalıştırılır
```
docker compose -f deployment/docker-compose.yml up --build -d     # başlat (varsayılan: yeniden yazma AÇIK)
docker compose -f deployment/docker-compose.yml ps                 # sağlık
docker compose -f deployment/docker-compose.yml logs -f assistant-api
docker compose -f deployment/docker-compose.yml down              # durdur
```
Yeniden yazmayı kapat: `QUERY_REWRITE_ENABLED=false docker compose -f deployment/docker-compose.yml up -d assistant-api`
Geri aç: aynı komut, değişkensiz. Servis yeniden oluşurken `healthy` olana kadar (~15 sn) web 502 verebilir.

**Bırakılan durum: sistem localhost:3000'de AÇIK, yeniden yazma AÇIK (`true`).** Tam yığın (4 servis) çalışıyor.

## Notlar
- `docker-compose.dev.yml` ile çalışan `rag-dev-postgres-1` 5432 portunu tutuyordu ve tam yığının postgres'i kalkmıyordu. Bu konteyneri `stop` ettim (veri/volume silinmedi). Coder'ın dev veritabanı gerekirse: `docker compose -f deployment/docker-compose.dev.yml start` (önce tam yığını durdurmak gerekir, port çakışır).
- Uygulama koduna dokunulmadı.

STATUS: DONE
