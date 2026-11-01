TASK: TASK-008
# Deploy raporu

## Değişen dosyalar
- `deployment/docker-compose.yml`: assistant-api'ye `RETRIEVAL_API_URL`, `RETRIEVAL_STRATEGY` (`${…:-rerank}`), `ASSISTANT_HOST_LABEL` (`${…:-}`).
- `web/nginx.conf`: `/api/assistant/` bloğu `proxy_http_version 1.1`, `Connection ''`, `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 300s`.
- `deployment/CLAUDE.md` ("Gerçek yapı (TASK-008)"), `ollama.md` (§8 değişkenler "okunuyor", duman testi, reranker kapatma), `README.md` (Önizleme sırası, strateji/etiket, dev/tam sistem port notu).
- Dockerfile'lar değişmedi; imajlar yeniden build edildi.

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|--------|---------------|------|--------------------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| assistant-api | rag-assistant-api (build) | 8090 | Up (healthy) |
| retrieval-api | rag-retrieval-api (build) | 8080 | Up (healthy) |
| web | rag-web (nginx 1.29.8) | 3000 | Up (healthy) |
Ollama (`qwen3.5:9b`, `bge-m3`) açıktı. Reranker kapalıydı; kendi başlattım (`ollama.md` §4 komutu + `--offline`, indirme yok), şu an açık (`/health` ok).

## Smoke test
- `curl localhost:3000` → 200. `GET /api/workspaces/42/sources` (proxy) → 200, mevcut kaynaklar (10001 "Ürün ve Hizmet Rehberi" 154 parça, 10003 "Rehber Üç", ...).
- `POST /api/assistant/conversations` (proxy) → 201.
- **SSE tamponlama (nginx, 3000):** "Kartımı kaybettim, ne yapmalıyım?" → `200 text/event-stream`; 72 `token` olayı, ilki 7,52 sn, sonuncusu 10,06 sn'de (tek seferde değil); sonra `sources` (`RERANK`, 20→5) ve `done` (promptTokens 2857, completionTokens 73, latencyMs 10067, hostLabel null).
- **M1 iptal:** istemci kopunca log `Turn cancelled conversationId=3 turnId=3 answerLength=1`; DB `error_code = STREAM_CANCELLED` (tur 1 ve 3). Kopma sunucuya bir sonraki token gönderiminde fark ediliyor (prefill sürerken birkaç sn gecikme). Ollama tarafında isteğin erken kapandığını ayrıca gözlemlemedim.
- **M1 boş bilgi bankası** (geçici proje `rag-empty`, kendi volume'u, web 3100): `sources` listesi boş; soru → `token` "Bu konuda rehberimizde bilgi bulamadım." tek olay, `sources` boş, `done.notFoundAnswer=true`, token sayıları null, model çağrılmadı (0,31 sn); DB `not_found_answer=t`. Sonra sadece `rag-empty` projesine `down -v` (yalnız `rag-empty_pgdata` silindi; `rag_pgdata` ve `rag-dev_pgdata-dev` duruyor). Ekrandaki ipucu metni frontend testinde (F14), tarayıcıda qa bakar.
- **M2 reranker kapalı:** llama-server durduruldu → aynı soru 200; `retrievalStrategy=RERANK_FALLBACK`, cevap akıyor (72 token), log `WARN Reranker unavailable, falling back to vector order candidates=20 topK=5 reason=…ResourceAccessException`; DB `retrieval_strategy=RERANK_FALLBACK`, `rerank_latency_ms=74`. Servisler healthy kaldı. Reranker yeniden açıldı.
- **M3 Ollama erişilemez** (geçici override, `OLLAMA_BASE_URL=…:11999`): 200 akış, 2,18 sn'de `error {"errorCode":"CHAT_MODEL_UNAVAILABLE"}`; DB `error_code=CHAT_MODEL_UNAVAILABLE`, `answer null`. Override kaldırıldı. **retrieval-api durduruldu:** `503 application/problem+json` `RETRIEVAL_UNAVAILABLE`; DB `error_code=RETRIEVAL_UNAVAILABLE`. retrieval-api yeniden başlatıldı.
- **Strateji `top5`:** `RETRIEVAL_STRATEGY=top5` ile `retrievalStrategy=TOP_K`, logda `Reranked` yok. Normal ayara (`rerank`) geri döndürüldü.
- Gözlem: `RERANK` ve `RERANK_FALLBACK` sonuçlarında ilk parça farklı kaynaklardan (10001 / 10003) geliyor; veritabanında aynı rehberin birden çok kaynağı var (kaynak silme olmadığından). Hata değil, qa M5 karşılaştırmasında kaynak adına dikkat etmeli.
- Gözlem: assistant-api yeniden oluşturulurken `ps` healthy olana kadar web 502 verir (geçici, kalıcı sorun yok).
- Gözlem: `rag-dev` projesinin postgres'i 5432'yi tuttuğu için tam sistem açılmadı; `docker compose -f deployment/docker-compose.dev.yml stop` ile durdurdum (veri silinmedi, `start` ile döner). Servisleri bilgisayarda geliştirecek biri onu yeniden başlatmalı, tam sistem kapalıyken.
- Veri silen işlem yapılmadı (yalnız geçici `rag-empty` projesi).

## Nasıl çalıştırılır
```bash
brew services start ollama
llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99   # ayrı terminal
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
docker compose -f deployment/docker-compose.yml logs --tail=100 assistant-api
docker compose -f deployment/docker-compose.yml down     # durdur (veri kalır)
```
Strateji/etiket: `RETRIEVAL_STRATEGY=top5` ve `ASSISTANT_HOST_LABEL="Apple M5 · 16 GB"` ile `up -d assistant-api`. Yeni model gerekmiyor.

**Sistem qa için açık: localhost:3000** (4 servis healthy, Ollama ve reranker açık).

## Düzeltme
Yok.

## Düzeltme turu 1
qa madde 2: retrieval veritabanındaki kopya kaynaklar (10002-10016) kullanıcının açık isteğiyle silindi. Silmeyi lead tek transaction'da çalıştırdı (benim denemelerim izin denetiminde reddedildi, ben silmedim); ben doğruladım.
- Önce: 15 kaynak, 15 sürüm, 2215 parça, 18 iş. Sonra: 1 kaynak (10001), 1 sürüm, 154 parça, 1 iş, 1 iş dosyası (`psql` sayımı).
- `GET localhost:3000/api/workspaces/42/sources` → `totalElements` 1: 10001 "Ürün ve Hizmet Rehberi", 154 parça.
- `POST localhost:3000/api/workspaces/42/search` ("Kartımı kaybettim, ne yapmalıyım?", topK 20) → 20 aday, hepsi `sourceId` 10001.
- assistant veritabanına ve volume'lara dokunulmadı. Dört servis healthy, reranker `/health` ok. Sistem qa için açık: localhost:3000.

STATUS: DONE
