TASK: TASK-018
# Deploy raporu

Scratchpad: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/9e2628af-0899-45bf-b3a3-4af016d3f6d6/scratchpad` (aşağıda `$S`).

## Değişen dosyalar
- `README.md` (baştan yazıldı; "Agent ekibi" ve "tmux" korundu; "Durum" satırı kalktı; Ölçümler dolu)
- `deployment/CLAUDE.md` ("Gerçek yapı (TASK-018)" bölümü eklendi)
- `ollama.md` §7 (sadece bellek değerleri, bkz. Ek). Compose, Dockerfile, nginx: değişmedi.

## Servisler
Sistem **açık**, boş değil: deneme verisiyle (karar 1B): rehber 2026.10 (154 parça), "RAG anlatımı" (62 parça), golden set (100 soru), 3 konuşma, 1 ölçüm çalıştırması.

| Servis | İmaj / etiket | Port | Durum (ps) |
|--------|---------------|------|------------|
| web | `rag-web` (node:26-alpine → nginx-unprivileged:1.29.8-alpine) | 3000 | healthy |
| assistant-api | `rag-assistant-api` (temurin 26) | 8090 | healthy |
| retrieval-api | `rag-retrieval-api` (temurin 26) | 8080 | healthy |
| postgres | `pgvector/pgvector:0.8.1-pg18` | 127.0.0.1:5432 | healthy |
| Ollama (host) | 0.40.2, `qwen3.5:9b`, `bge-m3` | 11434 | açık (dokunulmadı) |
| llama-server (host) | reranker, 8081 | 8081 | açık (dokunulmadı) |

## Smoke test
- `curl -sf localhost:3000` → 200. `/api/assistant/live-config` → 200. `/api/workspaces/42/sources` → 200.
- SSE proxy üzerinden (`curl -sN`): `token` olayları ~0,14 sn aralıkla parça parça geldi (tek seferde değil).
- WebSocket yok (kullanılmıyor).

## Veri silme ve temiz kurulum (C1)
- İlk deneme `down -v --rmi local` Bash izin sistemince reddedildi; kullanıcı iki silme komutunu kendisi çalıştırdı (`rag` ve `rag-e2e`). Sonrasında `docker volume ls` ve `docker images`'ta `rag*` kalmadı. Dev postgres `stop` edildi (verisi silinmedi).
- Taban imajlar (maven, temurin, node, nginx, pgvector) ve modeller önbellekte, indirme yok.
- Saat 09:18:10 → `build --no-cache` 49 sn → `up -d` → 4 servis healthy 09:19:31 → rehber UI'dan yüklendi (Playwright, "Belge ekle" penceresi; ad alanı betikte bulunamadı, ad dosya adından geldi) hazır 09:20:24 → Önizleme'de ilk soru → cevap bitti 09:21:19.
- **C1: 3 dk 9 sn (≤ 30 dk) ✅.**

## Acceptance (G2, AC1)
`cd web && E2E_ACCEPTANCE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test e2e/acceptance.spec.ts --reporter=list`
- 1. koşu (boş sistem): **11/11 geçti, 2,9 dk** (`$S/acceptance.out`). A11: recall@5 0,98, MRR 0,9125, 100 soru, 103 sn.
- Loglar koşunun hemen ardından, başka komuttan önce toplandı (G2), `$S/loglar/`:
  - `acceptance-compose.log` (949 satır), `assistant-api.log`, `retrieval-api.log`, `web.log`, `postgres.log`
  - `ollama-host.log`: `/opt/homebrew/var/log/ollama.log` tam kopyası (koşu saatleri 09:22-09:25 içinde; dosya tamdır, dilimlenmedi)
  - `reranker-host.log`: `/tmp/llama-reranker.log` tam kopyası (reranker 8081, 10:36'dan beri açık olduğu için eski satırlar da var)
- README komut denemesi sırasında curl ile rehber ikinci kez yüklendi (kaynak 10003, kopya parçalar; silme ucu yok). Bu yüzden `down -v` + `up -d` ve acceptance **ikinci kez** koşuldu: **11/11 geçti, 2,9 dk**. Son durum: 2 kaynak (10001: 154, 10002: 62 parça), golden set ve ölçüm yüklü. İkinci koşunun logu: `$S/loglar2/acceptance2-compose.log` (qa her iki seti tarayabilir; ana set 1. koşu).
- Not: 1. koşudan sonra (loglar kaydedildikten sonra) README denemesi servisi yeniden oluşturdu; `$S/loglar/` etkilenmedi.

## README komut denemesi (C2)
| Komut | Sonuç | |
|-------|-------|--|
| `brew install ollama llama.cpp` | zaten kurulu (ollama 0.40.2, llama.cpp 0.8.0); `brew list --versions` | ✅ (yeniden kurulum denenmedi) |
| `brew services start ollama` | `ollama started` | ✅ |
| `ollama pull qwen3.5:9b`, `ollama pull bge-m3` | success (önbellekte) | ✅ |
| reranker komutu `--port 8082` | `listening on http://127.0.0.1:8082`, `/health` ok, `/v1/rerank` skorlar 2,72 / -11,03; PID 68555 `kill` ile kapatıldı, kapalı doğrulandı | ✅ |
| `curl localhost:8081/health` | `{"status":"ok"}` | ✅ |
| `docker compose … stop` (dev), `build --no-cache`, `up -d`, `ps`, `logs --tail`, `down`, `down -v` | çalıştı, dördü healthy | ✅ |
| `RETRIEVAL_STRATEGY=top5 ASSISTANT_HOST_LABEL=… QUERY_REWRITE_ENABLED=false … up -d assistant-api` | live-config: `TOP_K`, `queryRewriteEnabled:false`, `rerankerStatus:NOT_USED` | ✅ |
| `GENERATION_MODE=llm … up -d assistant-api`, sonra değişkensiz | `LLM_ONLY` → `RAG`, `RERANK`, `UP` | ✅ |
| golden set `curl -X PUT` | 200 | ✅ |
| suggestions `PUT` / `GET` / temizle | 200 / 2 soru / `categories:[]` 200 | ✅ |
| kaynak yükleme `curl -F …` | 202, iş tamamlandı | ✅ |
| `tmux new -d -s rag-check && tmux kill-session -t rag-check` | tmux-ok | ✅ |
| `claude --version` | 2.1.317 (Claude Code) | ✅ |
| `cd web && npm run build`, `lint`, `npm test`, `test:coverage` | build ok; lint temiz; **728/728**; coverage All files satır 97,51 / dal 92,65 | ✅ |
| `npx playwright install chromium` | çalıştı | ✅ |
| `./mvnw verify` assistant-api | exit 0, 441 test, 0 hata | ✅ |
| `./mvnw verify` retrieval-api | exit 0, 344 test, 0 hata | ✅ |
| `npm audit --omit=dev` | 0 vulnerabilities | ✅ |
| PIT ×2, `npm run test:mutation` | **denenmedi** (30-60 dk'lık koşular; tester `03-test-report.md`'de koştu, qa bağımsız koşacak) | — |
| `E2E_ACCEPTANCE=1 …acceptance.spec.ts` | 11/11 (yukarıda) | ✅ |
| tam `npm run test:e2e` | qa koşacak (C15), burada koşulmadı | — |

Ayrıca `ollama.md` komutlarından duman testleri (embedding, chat, rerank) tekrar çalıştırılmadı; sistem bu komutların aynı uçlarını kullanarak çalışıyor.

## Performans ve bellek (C3, C4)
Makine: Apple M5, 17179869184 bayt (16 GB), macOS 27.0, Ollama 0.40.2. Varsayılan ayarlar, rehber 2026.10 yüklü (+ "RAG anlatımı" kaynağı). Betik: `$S/perf.py`, ham veri `$S/perf.json`, `$S/perf.out`, bellek örnekleri `$S/mem.log` (2 sn'de bir, 56 örnek; acceptance'tan hemen sonra başladığı için modeller zaten bellekteydi).

Ortanca / en kötü (ms), 15 tek soru (5 kategori × 3) ve 3 takip:
| Adım | Tek soru | Takip |
|------|----------|-------|
| yeniden yazma | — | 1199 / 1269 |
| arama (embedding ~37 + SQL ~7) | 56 / 188 | 111 / 139 |
| rerank | 956 / 1127 | 1075 / 1102 |
| ilk token | 245 / 4598 | 261 / 844 |
| cevap üretimi | 915 / 5209 | 2350 / 2805 |
| sunucu toplamı | 1986 / 6558 | 4670 / 5343 |
| istemci toplamı | 2006 / 6591 | 4726 / 5383 |
| prompt / completion token | 2450 / 22 | 2460 / 62 |

- Isınma sorusu (modeller soğuk): sunucu 20 090 ms, ilk token 16 772 ms.
- **Dikkat:** her sorunun ilk sorulması (5 ölçüm) ilk token 3516-4598 ms, toplam 5245-6558 ms; aynı soru tekrar sorulunca 152-262 ms / 1720-2279 ms (Ollama prompt önbelleği). Ortanca bu yüzden tekrarlara kayıyor; README'de not edildi.
- İçe alma: rehber 154 parça 8,98 sn; RAG anlatımı 62 parça 3,2 sn. A11 ölçümü: 103 125 ms (recall@5 0,98, MRR 0,9125).
- Bellek: Docker web 9 MiB, assistant-api 1135-1143 MiB, retrieval-api 1116-1118 MiB, postgres 94-96 MiB. Ollama `ollama ps`: qwen3.5:9b 10 GB (8192 ctx), bge-m3 673 MB; ollama runner RSS 595-599 MiB; reranker RSS 130-181 MiB. Swap 12 963 → tepe 13 506 MB (+543; toplam 14 336 MB). `memory_pressure` serbest %30-31.
- Not: `ollama.md` §7'deki eski Docker değerleri (assistant-api 435 MiB, retrieval-api 550 MiB) ve qwen 7,8 GB artık güncel değil (ölçüm: 1,1 GB / 1,1 GB / 10 GB). Sonradan güncellendi (bkz. Ek).

## Nasıl çalıştırılır
README.md "Kurulum" ve "İlk kullanım" bölümleri. Özet:
```bash
brew services start ollama   # + ollama pull qwen3.5:9b, bge-m3
llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml logs --tail=100 assistant-api
docker compose -f deployment/docker-compose.yml down        # durdur (veri kalır)
```

## Doküman önerileri
**`rag.md` satır 4** (şimdi: "> Durum: taslak. Aşağıdaki bilgiler `kaynaklar/rag-lab.html`'den özetlendi; kesinleşen kurallar task'larla buraya eklenir.") yerine:
> Durum: ürün çalışıyor (TASK-002…016, TASK-027). Aşağıdaki bilgiler `kaynaklar/rag-lab.html`'den özetlendi; kesinleşen kurallar task'larla buraya eklenmiştir.

**`rag.md` satır 38** (`<!-- Kullanıcı dolduracak: hedef kitle, modeller, veri kaynağı vb. -->`) yerine (ya da silinsin):
> - Hedef kitle: RAG'i öğrenmek isteyen geliştiriciler. Modeller: lokal ve ücretsiz (Ollama `qwen3.5:9b`, `bge-m3`; llama-server `bge-reranker-v2-m3`). Veri kaynağı: `docs/rehber/` altındaki kurgusal Raşit Bankası rehberi (PDF), "Belge ekle" ile yüklenir.

**`KULLANIM-KILAVUZU.md` satır 76**, cümlenin sonu `(ilk model task'ında oluşturulur)` → silinsin; `ollama.md` artık var (TASK-003). Yeni cümle sonu:
> Kurulum ve indirme komutları `ollama.md`'dedir; sistemi baştan sona kurmak için kök `README.md`'deki "Kurulum" ve "İlk kullanım" bölümlerini izle.

(`KULLANIM-KILAVUZU.md` başka eskimiş docker/ollama komutu içermiyor; `grep` ile bakıldı.)

## Düzeltme
Yok.

## Ek
- `ollama.md` §7 (Bellek notları) lead isteğiyle bu task'ta ölçülen değerlerle güncellendi: tablo (qwen 10 GB, bge-m3 673 MB, runner RSS, reranker RSS 130-181 MiB, Docker 9 / ~1,1 GB / ~1,1 GB / ~95 MiB, swap 12963 → 13506 MB, serbest %30-31). Komutlar ve §7'nin "Süreler" tablosu ile sonrası değişmedi (soğuk/sıcak süreler TASK-003 ölçümü olarak kaldı).

## Düzeltme turu 1
Frontend görsel düzeltmeleri (tester onaylı) sonrası plandaki 1-4. adımlar:
1. `docker compose -f deployment/docker-compose.yml down -v --rmi local` (izin sistemi bu sefer reddetmedi; sadece bu sabahki deneme verisi silindi) → `build --no-cache` (exit 0) → `up -d`; 4 servis `healthy`, bilgi bankası boş (`content: []`). Ollama ve reranker'a dokunulmadı.
2. Acceptance: `E2E_ACCEPTANCE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test e2e/acceptance.spec.ts --reporter=list` → **11/11 geçti, 3,3 dk** (`/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/9e2628af-0899-45bf-b3a3-4af016d3f6d6/scratchpad/acceptance-t1.out`). Rehber testin A2 adımında "Belge ekle" ile yüklendi.
3. Loglar koşunun hemen ardından, başka komuttan önce toplandı: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/9e2628af-0899-45bf-b3a3-4af016d3f6d6/scratchpad/loglar-t1/` (`acceptance-compose.log` 1019 satır, `assistant-api.log`, `retrieval-api.log`, `web.log`, `postgres.log`, `ollama-host.log` ve `reranker-host.log` tam dosya kopyaları). Önceki turun logları `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/9e2628af-0899-45bf-b3a3-4af016d3f6d6/scratchpad/loglar/` ve `loglar2/` altında kaldı; qa güncel olarak `loglar-t1/`'i taramalı.
4. README değişmedi, komut denemesi tekrarlanmadı. Servis kodu değişmedi, performans ölçümü tekrarlanmadı (README "Ölçümler" önceki turdan).
5. Son durum (sistem qa için açık): 4 servis healthy; 2 kaynak (10001: 154, 10002: 62 parça), golden set (100 soru) ve bir ölçüm çalıştırması yüklü; 3 konuşma.

STATUS: DONE
