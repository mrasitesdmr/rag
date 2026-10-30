TASK: TASK-003
# Deploy raporu

## Değişen dosyalar
- `deployment/docker-compose.yml`: assistant-api'ye `OLLAMA_BASE_URL`, `CHAT_MODEL`, `RERANKER_URL`, `RERANKER_MODEL`; retrieval-api'ye `OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS`; ikisine `extra_hosts`. Reranker servisi eklenmedi.
- `ollama.md` (yeni), `README.md` ("Lokal modeller" bölümü), `deployment/CLAUDE.md` (TASK-003 gerçek yapısı).
- Servis kodu, `pom.xml`, `web/` değişmedi. `grep -rn "11434\|8081" assistant-api/src retrieval-api/src` boş.

## Servisler
| Servis | İmaj / etiket | Port | Durum |
|--------|---------------|------|-------|
| postgres | pgvector/pgvector:0.8.1-pg18 | 127.0.0.1:5432 | Up (healthy) |
| assistant-api | yerel build | 8090 | Up (healthy) |
| retrieval-api | yerel build | 8080 | Up (healthy) |
| web | yerel build (nginx) | 3000 | Up (healthy) |
| Ollama (Mac, brew service) 0.40.2 | `qwen3.5:9b` (`d63f830f28d5`), `bge-m3` (`790764642607`) | 127.0.0.1:11434 | çalışıyor |
| llama-server (Mac) build 11960 | `bge-reranker-v2-m3-Q8_0.gguf` | 127.0.0.1:8081 | çalışıyor (pid 82278) |

## Smoke test
- D-S5 `docker compose -f deployment/docker-compose.yml config`: iki serviste D-2 değişkenleri doğru değerlerle, iki serviste `host.docker.internal=host-gateway`, `reranker` servisi yok. ✔
- D-S1 embedding → `1024`. ✔
- D-S2 dil modeli (`think:false`, temperature 0, num_ctx 8192), iki çalıştırma: `"Türkiye'nin başkenti Ankara'dır."` `done_reason=stop`, `thinking=''`; metinler aynı. ✔
- D-S3 reranker → `{"results":[{"index":0,"relevance_score":5.359559535980225},{"index":1,"relevance_score":-11.0427885055542}]}` → `OK`. ✔
- D-S4 container erişimi (Kararlar #11, `--host 127.0.0.1` ile): `exec assistant-api curl http://host.docker.internal:11434/api/version` → `{"version":"0.40.2"}`; aynısı `retrieval-api` ile aynı çıktı; `exec assistant-api curl http://host.docker.internal:8081/health` → `{"status":"ok"}`. ✔ (0.0.0.0'a geçmeye gerek kalmadı)
- D-S6: `up --build -d` sonrası dört servis healthy; reranker logunda `listening on http://127.0.0.1:8081`; `localhost:8081/health` → `{"status":"ok"}`, `localhost:11434/api/version` → `{"version":"0.40.2"}`. ✔
- D-S7: reranker SIGINT ile durdurulunca D-S3 komutu ve `/health` çıkış kodu 7 (bağlantı reddedildi); `brew services stop ollama` sonrası `/api/version` çıkış kodu 7; her iki durumda `ps` dört servis healthy. Sonra ikisi yeniden başlatıldı, D-S3 yine aynı skorlar/`OK`. ✔
- D-S8: ~6000 karakter (1345 token) belge → HTTP 200, `relevance_score` 6.23, hata yok. ✔
- G1/GPU: reranker `-lv 4` ile ayrı denemede `using device MTL0 (Apple M5)`, `offloading output layer to GPU`, `offloading 23 repeating layers to GPU`. `ollama ps`: ikisi de `100% GPU`. ✔
- Ana sayfa `curl localhost:3000` → 200. `/api/workspaces/42/sources` → 404 (endpoint henüz yok; bu task kapsamı dışı).
- Not: sistem iskelet aşamasında; SSE/WebSocket ucu yok, test edilmedi.

## Bellek ve süre (D-M1, D-M2)
Ölçüm: Docker sistemi açık; önce `ollama stop` ile iki model boşaltıldı, sonra D-S1, D-S2, reranker 20 belge çalıştırıldı.

| Bileşen | Ölçüm |
|---------|-------|
| `qwen3.5:9b` | 7,8 GB, 100% GPU, context 8192 |
| `bge-m3` | 673 MB, 100% GPU |
| Reranker | RSS 387 MB; `footprint`: phys_footprint 217 MB; Metal arabelleği 388 MiB (ağırlık 308 + KV 80). Metal belleği RSS'te tam görünmediği için log kırılımı da verildi |
| Docker (web/assistant/retrieval/postgres) | 15,6 MiB / 435 MiB / 550 MiB / 102 MiB |
| Swap | başlangıç used 9228 MB → son 9699 MB (+~470 MB); 9 GB ölçümden önce de doluydu |
| Serbest bellek | %68 |

| Çağrı | Soğuk | Sıcak |
|-------|-------|-------|
| Embedding | 1,10 sn | 0,019 sn |
| Dil modeli | 9,74 sn (load 4,91 sn, eval_count 12) | 0,52 sn |
| Reranker, 20 belge ~1000 karakter | — | 0,65 sn (HTTP 200) |

Not: İki model birlikte GPU'da kaldı, swap'e belirgin düşme olmadı; ancak makinede ölçümden önce de ~9 GB swap dolu (başka uygulamalar).

## Nasıl çalıştırılır
Ayrıntı `ollama.md`'de.
```bash
brew services start ollama
llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99   # ayrı terminal; Ctrl+C durdurur
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
docker compose -f deployment/docker-compose.yml down
```

## Planla farklar
- Plan Kararlar #7: model önbelleği `~/Library/Caches/llama.cpp/` değil, `~/.cache/huggingface/hub/models--gpustack--bge-reranker-v2-m3-GGUF/snapshots/*/bge-reranker-v2-m3-Q8_0.gguf` (608 MB). `ollama.md`'ye gerçek yol yazıldı.
- Bayraklar build 11960'ta olduğu gibi çalıştı (`--reranking`, `-hf`, `--n-gpu-layers`); değişiklik yok.
- Reranker bu oturumda arka planda (`nohup`) başlatıldı; belgedeki komut aynı, ön planda çalışır.

## Açık bırakılanlar (qa aynı komutları yeniden çalıştıracak)
- Docker Compose: dört servis açık, healthy (localhost:3000).
- Ollama: brew service açık (modeller 5 dk keep-alive sonrası bellekten çıkar).
- llama-server reranker: arka planda açık, `127.0.0.1:8081` (pid 82278).
- Kapanışta: `kill -INT 82278` (ya da `pkill -f 'llama-server.*--port 8081'`), compose `down`, istenirse `brew services stop ollama`.

## Düzeltme
Yok.

STATUS: DONE
