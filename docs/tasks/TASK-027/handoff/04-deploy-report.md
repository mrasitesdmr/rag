TASK: TASK-027
# Deploy raporu

## Değişen dosyalar
- `deployment/docker-compose.yml`: retrieval-api'den `INGESTION_BOOTSTRAP_FILE` ve `../docs/rehber/pdf:/app/rehber:ro` bağlantısı kaldırıldı.
- `web/nginx.conf`: `/api/workspaces/` location'ına `client_max_body_size 21m`.
- `deployment/CLAUDE.md`: açılış içe alması maddeleri kalktı; "Gerçek yapı (TASK-027)": boş açılış, `curl -F` yükleme (8080 ve 3000, `version` zorunlu), yeniden dene komutu, nginx sınırı, eski rehber verisinin temizliği (`down -v`, tam sistem ve dev).
- `README.md`, `ollama.md` (§8): otomatik içe alma anlatımı yerine yükleme ve yeniden dene.
- Dockerfile'lar değişmedi.

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|--------|---------------|------|--------------------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| assistant-api | rag-assistant-api (temurin 26 jre) | 8090 | Up (healthy) |
| retrieval-api | rag-retrieval-api (temurin 26 jre) | 8080 | Up (healthy) |
| web | nginx-unprivileged 1.29.8 | 3000 | Up (healthy) |

## Smoke test
Temiz açılış: `rag_pgdata` volume'unu kullanıcı kendi terminalinde `down -v` ile sildi (onaylı); ardından `up --build -d` ile hepsi healthy.
- **M1 temiz açılış:** `GET :8080/api/workspaces/42/sources` → 200 `content: []`, `totalElements: 0`. `GET …/ingestion-jobs/1` → 404 `INGESTION_JOB_NOT_FOUND`. `curl -sf localhost:3000` → 200.
- **M1 yükleme:** Rehber 2026.10, 8080'den → 202 + `Location: /api/workspaces/42/ingestion-jobs/1`, iş `DONE`, 154 parça, kaynak 10001. Başka yer imli PDF olarak `docs/rehber/pdf/…2026.11.pdf` web adresi (3000, nginx) üzerinden → 202, iş 2 `DONE`, 153 parça, kaynak 10002. (Elde bağımsız üçüncü bir yer imli PDF yoktu; 2026.11 rehberi kullanıldı.)
- **Yer imsiz PDF (elle üretilen tek sayfalık, 3000 üzerinden):** 422 `PDF_NO_BOOKMARKS`, listede yeni kayıt yok (`totalElements` 2 kaldı).
- **413:** 3000 üzerinden 20 MiB + 1 baytlık `.pdf` → 413 `application/problem+json`, `FILE_TOO_LARGE` (nginx HTML'i değil).
- **M2 yeniden başlatma:** `restart retrieval-api` öncesi/sonrası: 2 sources, 2 jobs, 307 chunks; yeni iş yok.
- **M3 Ollama erişilemez:** override ile `OLLAMA_BASE_URL=http://127.0.0.1:1`, 3000'den yükleme → iş 4 `FAILED`, `failedStage: EMBEDDING`, `errorCode: EMBEDDING_UNAVAILABLE`. Override kaldırılıp servis yeniden açıldı; `POST …/sources/10004/ingestion-jobs` → 202, iş 5 `DONE`, 153 parça; kaynak tek (10004).
- **İz araması:** `grep -rniE 'bootstrap|GuideBootstrap|INGESTION_BOOTSTRAP|/app/rehber' deployment retrieval-api/Dockerfile web/nginx.conf` (CLAUDE.md hariç) → 0 satır. `deployment/CLAUDE.md`'de sadece "kalktı" notu geçer.
- Not: assistant-api'ye ve SSE/WebSocket'e değişiklik yok; onlar için ayrı komut çalıştırılmadı (`/api/assistant/actuator/health` 404, beklenen: actuator yayınlanmaz).
- Not: hatalı bir kabuk değişkeni yüzünden override ilk denemede uygulanmadı; o deneme gerçek Ollama ile iş 3 `DONE` (kaynak 10003) oldu. Bu bir test artığıdır.

## Nasıl çalıştırılır
```
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
docker compose -f deployment/docker-compose.yml logs --tail=100 retrieval-api
docker compose -f deployment/docker-compose.yml down        # veriyi korur
```
Ollama açık olmalı (`bge-m3`). Yükleme: `curl -F file=@rehber.pdf -F name="Ürün ve Hizmet Rehberi" -F version=2026.10 localhost:3000/api/workspaces/42/sources`. Yeniden dene: `curl -X POST localhost:8080/api/workspaces/42/sources/<id>/ingestion-jobs`.

## Sistem durumu
Sistem **açık bırakıldı** (localhost:3000). Veritabanında test kayıtları var: kaynaklar 10001-10004 (5 iş, 613 parça). qa temiz başlangıç isterse `down -v` gerekir (kullanıcı onayıyla).

STATUS: DONE
