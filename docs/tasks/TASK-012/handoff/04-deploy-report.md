TASK: TASK-012
# Deploy raporu

## Değişen dosyalar
- `web/nginx.conf`: `/api/assistant/` location'ına `client_max_body_size 2m`.
- `README.md`: "Önerileri yükleme" maddesi (komut, dosya biçimi, temizleme).
- `deployment/CLAUDE.md`: "Gerçek yapı (TASK-012)" bölümü.
- Yeni env değişkeni, servis, model yok; imajlar `up --build` ile yeniden derlendi.

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|--------|---------------|------|--------------------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| retrieval-api | yerel build (temurin 26 jre) | 8080 | Up (healthy) |
| assistant-api | yerel build (temurin 26 jre) | 8090 | Up (healthy) |
| web | nginx-unprivileged 1.29.8-alpine | 3000 | Up (healthy) |

Ön koşul: Ollama açık (`qwen3.5:9b` listede), llama-server `localhost:8081/health` → `{"status":"ok"}`. Host süreçlerine dokunulmadı.

## Smoke test
- `curl -s -o /dev/null -w %{http_code} localhost:3000` → 200
- `GET localhost:3000/api/assistant/suggestions` → 200 `{"questionCount":0,"followUpCount":0,"categories":[]}`
- `GET localhost:3000/api/workspaces/42/sources` → 200 (`content` dolu, kaynak 10001 mevcut)
- Büyük liste PUT (nginx üzerinden): 500 soru × 5 takip, Türkçe harfli, ~1,13 MB → 200 `questionCount 500, followUpCount 2500`
- Temizleme `{"categories":[]}` → 200; sonra GET → boş liste (deneme listesi kalıcı bırakılmadı)
- 3 MB gövdeli PUT → 413 (sınır çalışıyor)
- SSE/WebSocket: bu task'ta akış/WebSocket değişmedi; çalıştırılmadı (SSE qa'nın gerçek model testinde doğrulanır).

## Nasıl çalıştırılır
```bash
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
docker compose -f deployment/docker-compose.yml logs --tail=100 assistant-api
docker compose -f deployment/docker-compose.yml down
```
Önerileri yükleme: `curl -X PUT localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' --data-binary @suggestions.json` (biçim README.md'de). Sistem localhost:3000'de açık bırakıldı.

## Düzeltme
Yok.

STATUS: DONE
