TASK: TASK-011
# Deploy raporu

## Değişen dosyalar
- `deployment/docker-compose.yml`: assistant-api ortamına `GENERATION_MODE: ${GENERATION_MODE:-rag}`.
- `deployment/CLAUDE.md`, `ollama.md` (değişken tablosu, mod değiştirme komutu, reranker `/health` 15 sn notu), `README.md` (Sadece LLM modu komutu).
- Dockerfile ve nginx değişmedi; yeni model/dependency yok. Host'taki Ollama ve llama-server'a dokunulmadı.

## Servisler
| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|---|---|---|---|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| assistant-api | build ../assistant-api | 8090 | Up (healthy) |
| retrieval-api | build ../retrieval-api | 8080 | Up (healthy) |
| web | build ../web (nginx) | 3000 | Up (healthy) |

## Smoke test
- `docker compose ... up --build -d`: tüm servisler healthy.
- `curl localhost:3000` → 200.
- `GET localhost:3000/api/assistant/live-config` (nginx üzerinden) → 200; varsayılan: `generationMode=RAG`, `retrievalStrategy=RERANK`, `rerankerStatus=UP`, `chatModel=qwen3.5:9b`, `embeddingModel=bge-m3`/1024, `queryRewriteEnabled=true`, `knowledgeBase.status=READY` (3 kaynak).
- `GET /api/workspaces/42/sources` → 200 (retrieval-api proxy).
- `GENERATION_MODE=llm ... up -d assistant-api` → healthy, live-config `generationMode=LLM_ONLY`, `rerankerStatus=NOT_USED`. Değişkensiz aynı komutla geri dönüldü → `RAG`, `rerankerStatus=UP`.
- SSE: nginx `/api/assistant/` ayarı değişmedi (TASK-008'de doğrulanmıştı); bu turda tekrar koşulmadı, qa E1'de koşacak.

## Nasıl çalıştırılır
- Başlat: `docker compose -f deployment/docker-compose.yml up --build -d`
- Sadece LLM: `GENERATION_MODE=llm docker compose -f deployment/docker-compose.yml up -d assistant-api` (geri: değişkensiz aynı komut). Mod ortam değişkenidir; servis yeniden başlar (o sırada web 502 verebilir).
- Durdur: `docker compose -f deployment/docker-compose.yml down`; log: `... logs -f assistant-api`.

Sistem localhost:3000'de **açık bırakıldı**, mod `rag` (varsayılan).

STATUS: DONE
