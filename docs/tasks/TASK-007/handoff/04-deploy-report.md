TASK: TASK-007
# Deploy raporu

## Değişen dosyalar
- `deployment/CLAUDE.md` ("Gerçek yapı (TASK-007)" bölümü eklendi). Dockerfile, Compose, `nginx.conf` değişmedi (plana uygun).

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps) |
|--------|---------------|------|------------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up, healthy |
| retrieval-api | rag-retrieval-api (yeniden derlendi) | 8080 | Up, healthy |
| assistant-api | rag-assistant-api (yeniden derlendi) | 8090 | Up, healthy |
| web (nginx) | rag-web (yeniden derlendi) | 3000 | Up, healthy |

Ön koşullar: `docker info` tamam; Ollama'da `qwen3.5:9b` ve `bge-m3:latest` var. Reranker (8081) bu task'ta gerekmiyor, kontrol edilmedi (curl yanıt vermedi).

## Kurulum notları
- `up --build -d` ilk denemede `Bind for 127.0.0.1:5432 failed: port is already allocated` verdi: `rag-dev-postgres-1` (dev compose) 5432'yi tutuyordu. `docker compose -f deployment/docker-compose.dev.yml stop` ile durdurdum (veri silinmedi, `-v` yok); dev postgres'i şu an KAPALI.
- Yarım kalan ilk başlatma ağ DNS'ini bozdu (`UnknownHostException: postgres`); `down` (volume'suz) + `up -d` ile düzeldi.
- Veritabanı volume'u korundu; önceki koşulardan kalan kaynaklar duruyor (10001 rehber, 10002-10005 elle/QA, E2E kaynakları).

## Smoke test
- `curl localhost:3000/` → 200 text/html; `/konusmalar/1` (SPA fallback) → 200 text/html
- `/api/workspaces/42` → 200 JSON (bge-m3, 1024, 1600/2000/200, HNSW, COSINE)
- `/api/workspaces/42/sources` → 200 JSON (10001 "Ürün ve Hizmet Rehberi", 70 sayfa, 154 parça)
- `/api/workspaces/42/sources/99999` → 404 application/problem+json
- `/api/assistant/actuator/health` → 404 problem+json (assistant-api bu task'ta uç sunmuyor; proxy assistant-api'ye ulaşıyor)
- PDF: `curl -I localhost:3000/api/workspaces/42/sources/10001/file` → 200, `application/pdf`, `Content-Length: 1120236`, `Content-Disposition: inline`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`; gövde 1120236 bayt. Not (küçük): `X-Content-Type-Options` iki kez geliyor (backend + nginx); zararsız.
- SSE/WebSocket: bu task'ta yok.

## E2E (`cd web && npm run test:e2e`, Docker'daki site, gerçek Ollama)
Sonuç: **8 geçti, 2 kaldı, 1 atlandı** (1.3 dk; ilk koşu ve tekrar aynı sonuç).
- Kalan 1: `sources.spec.ts:86 givenReadySource_whenPdfOpened_thenNewTabReceivesPdf` → `popup.waitForLoadState()` 60 sn zaman aşımı ("networkidle" tetiklendi, "load" gelmedi). Headless chromium'da PDF görüntüleyici sekmesi `load` üretmiyor. Ürün tarafı çalışıyor: aynı PDF curl ile nginx'ten 200/`application/pdf` (yukarıda); testin kendi `page.request.get` assertion'larına hiç gelinmedi. Test hatası.
- Kalan 2: `sources.spec.ts:102 givenPdfWithoutBookmarks_whenUploaded_thenFixMessageAndNoNewRow` → `PDF'te yer imi (başlık) yok` mesajı göründü (doğrulama geçti, `PDF_NO_BOOKMARKS` entegrasyonda doğrulandı); son `toHaveCount(before)` kaldı: `before=0` (başlık görününce tablo satırları henüz yüklenmemiş), sonra 7 satır. Yarış durumu, test hatası.
- Atlanan 1: E0 (boş durum), bilgi bankası boş olmadığı için `test.skip` (testin kendi koşulu).
- Geçen 8: shell.spec + E1 (yükleme, aşama, Hazır), tekrar adı (E3) vb.

## Elle denemeler
- Ollama kapalı (`brew services stop ollama`): 4 servis `healthy` kaldı. `POST /api/workspaces/42/sources` (rehber PDF) → 202; iş 9 birkaç sn içinde `FAILED`, `failedStage: EMBEDDING`, `errorCode: EMBEDDING_UNAVAILABLE`, `totalChunks: 153`. Yeniden dene (`POST .../sources/10008/ingestion-jobs`) → 202 yeni iş (10), Ollama hâlâ kapalıyken yine `FAILED`.
- Ollama `brew services start ollama` ile yeniden açıldı (`bge-m3` görünüyor); retry bir kez daha atıldı (202), kaynak 10008 (`Ollama Kapali …`) şu an EMBEDDING aşamasında, Hazır'a dönecek. Bu kaynak qa verisine eklenmiş olur (silme yok).

## Nasıl çalıştırılır
```
brew services start ollama   # bge-m3 gerekli (ollama.md)
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
docker compose -f deployment/docker-compose.yml logs --tail=100 retrieval-api
docker compose -f deployment/docker-compose.yml down        # veri kalır; -v veri siler
```
Sistem açık: site `http://localhost:3000` (Bilgi kaynakları `/kaynaklar`), retrieval-api `:8080`, assistant-api `:8090`.

## Düzeltme
Sorumlu rol: **tester** (`web/e2e/sources.spec.ts`; uygulama kodunda hata yok).
1. `:86` PDF testi: `popup.waitForLoadState()` kaldırılmalı/`'commit'` ya da popup URL'sinin `/file` ile bittiğini doğrulamaya çevrilmeli (headless PDF görüntüleyici `load` vermiyor).
2. `:102` yer imsiz test: `before` satır sayısı liste yüklenmeden alınıyor; önce "Ürün ve Hizmet Rehberi" gibi bir satırın (ya da `Henüz belge yok`) görünmesi beklenmeli, sonra sayılmalı.


## Düzeltme turu 1
- tester `sources.spec.ts`'i düzeltti. Yeniden koşu: `cd web && npm run test:e2e` (Docker'daki site, gerçek Ollama) → **10 geçti, 1 atlandı (E0, bilgi bankası boş değil), 0 kaldı** (11.8 sn).
- `docker compose ps`: 4 servis healthy. Sistem açık: `http://localhost:3000`.
- Not (küçük, kapsam dışı): `X-Content-Type-Options` PDF cevabında iki kez geliyor (backend + nginx); yanıtta "nosniff, nosniff" görünüyor; `nginx.conf` değiştirilmedi, değerlendirmesi qa'da.

STATUS: DONE
