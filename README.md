# RAG Asistan

RAG'i (Retrieval-Augmented Generation: dil modeline cevabı kendi verinle, kaynak göstererek yazdırmak) anlatan ve çalışan bir demoyla gösteren eğitim projesi. Raşit Bankası dijital asistanı, müşteri sorularını "Ürün ve Hizmet Rehberi" PDF'ine dayanarak, kaynak numaralı olarak cevaplar. Ürün `kaynaklar/rag-lab.html`'in sol yarısıdır: Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme ekranları, gerçek veri ve lokal modellerle çalışır. Başlangıç içeriği `kaynaklar/rag.html` (anlatım) ve `kaynaklar/rag-lab.html` (lab); `../embedding-pgvector` projesinin devamıdır.

## Gereksinimler
- Docker Desktop
- Homebrew ile `ollama` ve `llama.cpp` (lokal modeller)
- Geliştirme için ayrıca Java 26 ve Node 26
- Disk: modeller ~9,5 GB (qwen3.5:9b 7,6 GB, bge-m3 1,2 GB, reranker 0,6 GB)

## Kurulum
Repo kökünden, sırayla.

**1. Modeller** (ayrıntı, duman testleri ve bellek notları: [`ollama.md`](ollama.md))
```bash
brew install ollama llama.cpp          # bir kez
brew services start ollama
ollama pull qwen3.5:9b
ollama pull bge-m3
```

**2. Reranker** (ayrı terminalde, ön planda; Ctrl+C ile durur)
```bash
llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99
```
`curl -s localhost:8081/health` → `{"status":"ok"}` olunca hazırdır.

**3. Sistem**
```bash
docker compose -f deployment/docker-compose.dev.yml stop   # varsa geliştirme postgres'i (5432 çakışır; veri silmez)
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps         # dördü de healthy olmalı
```
Site: http://localhost:3000. Diğer portlar: assistant-api 8090, retrieval-api 8080, PostgreSQL 127.0.0.1:5432.

Durdurma ve temizlik:
```bash
docker compose -f deployment/docker-compose.yml logs --tail=100 assistant-api
docker compose -f deployment/docker-compose.yml down        # durdur, veri kalır
docker compose -f deployment/docker-compose.yml down -v     # durdur + bütün veriyi sil
```

## İlk kullanım
Bilgi bankası, golden set ve öneri listesi boş açılır; hiçbiri kendiliğinden yüklenmez.

1. **Rehber:** http://localhost:3000/kaynaklar → "Belge ekle" → `docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf`, ad "Raşit Bankası Ürün ve Hizmet Rehberi", sürüm `2026.10` → "Yükle ve işle". Durum "Hazır" olunca 154 parça görünür. PDF'te yer imi (bookmark) gerekir; yer imsiz PDF reddedilir. Komutla: `curl -F file=@rehber.pdf -F name="Ürün ve Hizmet Rehberi" -F version=2026.10 localhost:3000/api/workspaces/42/sources`.
2. **Golden set** (Değerlendirme ekranı için, cevabı hangi parçada olan 100 soru):
   ```bash
   curl -X PUT localhost:3000/api/assistant/golden-set -H 'Content-Type: application/json' --data-binary @docs/rehber/golden/rehber-golden-v1.json
   ```
3. **Soru önerileri** (Önizleme çipleri, "Soru listesi", otomatik tamamlama; isteğe bağlı). Her yükleme bütün listeyi değiştirir:
   ```bash
   curl -X PUT localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' --data-binary @suggestions.json
   curl -X PUT localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' -d '{"categories":[]}'   # temizle
   ```
   Biçim (`starter` ve `followUps` isteğe bağlı; en fazla 50 kategori, toplam 500 soru, soru başına 5 takip; gövde sınırı 2 MB):
   ```json
   {"categories":[{"name":"Kartlar","questions":[
     {"text":"Kartımı kaybettim, ne yapmalıyım?","starter":true,"followUps":["Yenisi kaç günde gelir?"]},
     {"text":"Kart aidatı ne kadar?"}]}]}
   ```
   Hatalı dosya 400 `VALIDATION_FAILED` döner, kayıtlı liste değişmez.
4. **İlk soru:** http://localhost:3000/onizleme → "Kartımı kaybettim, ne yapmalıyım?". İlk cevap modeller bellekte değilse ~10 sn sürer.

## Ekranlar
- **Önizleme:** Asistanla konuşulan ekran. Cevap token token akar, `[n]` alıntısına tıklanınca ilgili parça ve sayfa açılır. Yan kartta modele giden parçalar, adım süreleri ve (takip sorusunda) yeniden yazılmış arama sorgusu görünür. Rehberde olmayan soruda "Bu konuda rehberimizde bilgi bulamadım." çıkar ve "Temsilciye bağlan" belirir; "Faydalı mı?" ile geri bildirim verilir. Üstteki şerit canlı yapılandırmayı (strateji, üretim modu, reranker durumu) gösterir.
- **Konuşmalar:** Önizleme'de yapılan konuşmaların listesi ve ayrıntısı: turlar, durum çipi (Dayanaklı, Bilmiyor, Yanlış…), modele giden parçalar ve temsilciye aktarma kaydı.
- **Bilgi kaynakları:** Belge listesi, "Belge ekle" penceresi, kaynak ayrıntısı (içe alma işleri, parçalar, filtre ve sayfalama). Yükleme sırasında sayfa yenilense de ilerleme sürer.
- **Değerlendirme:** Golden set üzerinde recall@5 ve MRR ölçümü. Ölçüm arka planda sürer, ilerleme sayacı görünür; bitince çalıştırmalar tablosu ile kaçan ve sırası değişen sorular listelenir.

## Mimari
| Bileşen | Teknoloji | Port |
|---------|-----------|------|
| `web` | React 19, TypeScript, Vite; nginx ile sunulur ve `/api/…` isteklerini yönlendirir | 3000 |
| `assistant-api` | Spring Boot 4, Java 26, Spring AI: konuşma, cevap akışı (SSE), geri bildirim, aktarma, değerlendirme | 8090 |
| `retrieval-api` | Spring Boot 4, Java 26, pgvector: kaynak, içe alma, parçalama, embedding, vektör arama | 8080 |
| `postgres` | `pgvector/pgvector:0.8.1-pg18`; iki veritabanı: `assistant` ve `retrieval` | 127.0.0.1:5432 |
| Ollama (Mac'te) | `qwen3.5:9b` (dil modeli), `bge-m3` (embedding, 1024 boyut) | 11434 |
| llama-server (Mac'te) | `bge-reranker-v2-m3` (reranker) | 8081 |

- nginx: `/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api. Modeller Docker'da değil Mac'te (Metal GPU) çalışır; container'lar `host.docker.internal` ile ulaşır.
- İki servis birbirinin veritabanına bağlanmaz, REST ile konuşur. Sözleşmeler: `docs/api/openapi.yaml`, `docs/api/sse.md`.
- **Bir soru böyle cevaplanır:** (1) takip sorusuysa geçmişle birlikte yeniden yazılır → (2) retrieval-api'de `bge-m3` embedding'i ile pgvector araması (20 aday) → (3) reranker adayları sıralar, ilk 5 seçilir → (4) parçalar numaralı olarak prompt'a konur → (5) `qwen3.5:9b` cevabı akıtır → (6) `[n]` alıntıları ayrıştırılır; her adımın sonucu tura kaydedilir. Cevap parçalarında bilgi yoksa sabit "bulamadım" cümlesi döner.

## Ayarlar
Ortam değişkenidir; servis yeniden oluşturulunca geçerli olur (`deployment/.env` de olur, repoya girmez). Örnek: `RETRIEVAL_STRATEGY=top5 docker compose -f deployment/docker-compose.yml up -d assistant-api` (geri dönmek için değişkensiz aynı komut). Tek servis yeniden oluşurken `ps` `healthy` olana kadar web 502 verebilir.

| Değişken | Varsayılan | Anlamı |
|----------|------------|--------|
| `RETRIEVAL_STRATEGY` | `rerank` | `rerank`: 20 aday → reranker → 5; `top5`: reranker yok, vektör sırasının ilk 5'i |
| `GENERATION_MODE` | `rag` | `llm`: Sadece LLM, retrieval ve rerank yok (değerlendirme bu modda çalışmaz) |
| `QUERY_REWRITE_ENABLED` | `true` | `false`: takip sorusu yeniden yazılmaz |
| `ASSISTANT_HOST_LABEL` | boş | Süre kartındaki makine etiketi, ör. `Apple M5 · 16 GB` |
| `CHAT_MODEL`, `RERANKER_MODEL` | `qwen3.5:9b`, `bge-reranker-v2-m3` | assistant-api modelleri |
| `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS` | `bge-m3`, `1024` | retrieval-api embedding'i (değişirse veri yeniden içe alınır) |

Compose değerleri düz yazılıdır; şifreler yalnızca lokal kullanım içindir.

## Ölçümler
### Bu makinede ölçülen: Apple M5, 16 GB, macOS 27.0, Ollama 0.40.2, llama.cpp 0.8.0, 2026-11-03

Varsayılan ayarlar (`rerank`, yeniden yazma açık), rehber 2026.10 yüklü (ayrıca "RAG anlatımı" kaynağı var). Her soru yeni konuşmada 3 kez soruldu; değerler **ortanca / en kötü**, milisaniye. Takip sorusu `Kartımı kaybettim, ne yapmalıyım?` sorusundan sonra sorulan `Yenisi kaç günde gelir?` sorusudur.

| Adım | Tek soru (15 ölçüm: 5 kategoriden birer soru × 3) | Takip sorusu (3 ölçüm) |
|------|-----------|-------------|
| Yeniden yazma (`queryRewriteLatencyMs`) | — (takip değil) | 1199 / 1269 |
| Arama (`retrievalLatencyMs`; embedding ~37 + SQL ~7 ortanca) | 56 / 188 | 111 / 139 |
| Rerank (`rerankLatencyMs`, 20 aday) | 956 / 1127 | 1075 / 1102 |
| İlk token (`timeToFirstTokenMs`) | 245 / 4598 | 261 / 844 |
| Cevap üretimi (`generationLatencyMs`) | 915 / 5209 | 2350 / 2805 |
| Sunucu toplamı (`latencyMs`) | 1986 / 6558 | 4670 / 5343 |
| İstemci toplamı (`curl time_total`) | 2006 / 6591 | 4726 / 5383 |
| `promptTokens` / `completionTokens` (ortanca) | 2450 / 22 | 2460 / 62 |

- **İlk token farkı:** Bir sorunun ilk sorulduğu 5 ölçümde ilk token 3516-4598 ms, toplam 5245-6558 ms; aynı soru tekrar sorulduğunda 152-262 ms ve 1720-2279 ms (Ollama aynı prompt'un işlenmiş halini önbellekten kullanıyor). Gerçek kullanımda farklı sorular geldiği için ilk değerler tipiktir.
- **Isınma:** Modeller bellekte değilken ilk cevap 20,1 sn sürdü (ilk token 16,8 sn). Ollama modelleri 5 dk kullanılmayınca bellekten çıkarır.
- **Temiz kurulum:** `build --no-cache` 49 sn; README "Kurulum" komutlarının başlangıcından Önizleme'de ilk cevabın bitmesine **3 dk 9 sn** (modeller indirilmiş, taban imajlar önbellekte).
- **İçe alma:** rehber 2026.10 (70 sayfa, 154 parça) 9,0 sn; "RAG anlatımı" (62 parça) 3,2 sn.
- **Değerlendirme:** 100 soru, rerank 20→5, 103 sn; recall@5 0,98, MRR 0,9125.

| Bellek | Ölçüm başı* | Tepe |
|--------|-------------|------|
| Docker `web` | 9 MiB | 9 MiB |
| Docker `assistant-api` | 1135 MiB | 1143 MiB |
| Docker `retrieval-api` | 1116 MiB | 1118 MiB |
| Docker `postgres` | 94 MiB | 96 MiB |
| Ollama `qwen3.5:9b` (`ollama ps`, num_ctx 8192) | 10 GB | 10 GB |
| Ollama `bge-m3` (`ollama ps`) | 673 MB | 673 MB |
| Ollama runner süreci (RSS) | 595 MiB | 599 MiB |
| Reranker llama-server (RSS) | 130 MiB | 181 MiB |
| Swap (`vm.swapusage`, toplam 14336 MB) | 12963 MB | 13506 MB (+543 MB) |
| Serbest bellek (`memory_pressure`) | %30-31 | — |

\* Örnekleme (2 sn'de bir, 56 örnek) acceptance koşusundan hemen sonra başladığı için modeller bellekte yüklüydü; "ölçüm başı" soğuk boşta değildir. Metal belleğindeki model ağırlıkları süreç RSS'ine yansımaz, `ollama ps` değeri esastır.

Yorum: Tek sorular 1,7-6,6 sn'de (ortanca 2,0 sn), takip soruları 4,7-5,4 sn'de bitti; bu makinede ölçüm sırasında swap 13 GB'ın üstündeydi ve 14,3 GB'lık swap alanının yaklaşık %94'üne ulaştı.


## Testler
```bash
cd assistant-api && ./mvnw verify                 # birim + entegrasyon (Testcontainers; Docker açık olmalı)
cd retrieval-api && ./mvnw verify
cd web && npm run build && npm run lint && npm test && npm run test:coverage

# Uçtan uca (sistem açık, modeller açık, rehber yüklü olmalı)
cd web && npx playwright install chromium        # bir kez
cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e

# Kabul senaryosu: 11 senaryo, gerçek modellerle; BOŞ bilgi bankası ister (dolu ise başlamaz)
cd web && E2E_ACCEPTANCE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test e2e/acceptance.spec.ts

# Mutation (eşik %80)
cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
cd retrieval-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
cd web && npm run test:mutation

# Bağımlılık güvenliği
cd web && npm audit --omit=dev
```
Tam E2E koşusu her seferinde "E2E Rehber …" adlı bir kaynak bırakır; temiz kurulum için `down -v`.

## Sorun giderme
- **Port çakışması (5432/8080/3000):** geliştirme postgres'i (`docker-compose.dev.yml`) ya da `../embedding-pgvector` açıktır. İlkini `docker compose -f deployment/docker-compose.dev.yml stop` (veri silmez), ikincisini kendi klasöründe `down` ile kapat.
- **Veriyi sıfırlama:** `docker compose -f deployment/docker-compose.yml down -v` (bütün kaynaklar, konuşmalar, golden set silinir). Tam temiz kurulum: `down -v --rmi local`, sonra `build --no-cache` ve `up -d`. PostgreSQL init script'i (iki veritabanı ve kullanıcı) yalnız boş volume'da çalışır.
- **Ollama kapalı:** Önizleme'de "Dil modeline şu an ulaşılamıyor"; belge yüklemesinde iş `FAILED` + `EMBEDDING_UNAVAILABLE`. Ollama'yı açıp kaynak ayrıntısından "Yeniden dene" (ya da `curl -X POST localhost:8080/api/workspaces/42/sources/<id>/ingestion-jobs`). Servisler yine `healthy` görünür (healthcheck modele bağlı değildir).
- **Reranker kapalı:** Cevap yine gelir; sıra vektör sırasının ilk 5'idir (canlı yapılandırma şeridinde "reranker DOWN"). Komutu yeniden çalıştır.
- **Yavaşlık/swap:** `ollama ps` ve `sysctl vm.swapusage`; kullanılmayan modeli `ollama stop <model>` ile çıkar. Ayrıntı: `ollama.md` §7 ve §9.

## Agent ekibi

| Sıra | Agent | Model | Görev |
|------|-------|-------|-------|
| 1 | `planner` | opus | Task'ı analiz eder; onay kartını ve teknik planı yazar, gerekirse `docs/api/` sözleşmelerini (REST, SSE) günceller. Kod yazmaz. |
| 2 | `backend-coder` | opus | Planın backend kısmını `assistant-api` (Spring Boot + Spring AI) ve `retrieval-api` (Spring Boot + pgvector) içinde uygular. Sadece bu iki klasöre yazar, test yazmaz. |
| 2 | `frontend-coder` | opus | Planın frontend kısmını React ile uygular. Sadece `web/` altına yazar, test yazmaz. |
| 3 | `tester` | sonnet | Backend (JUnit) ve frontend (Vitest, Playwright) testlerini yazar ve çalıştırır. |
| 4 | `devops` | sonnet | Dockerfile ve docker-compose dosyalarını, lokal model kurulum rehberini ve bu README'yi yazar; sistemi ayağa kaldırıp smoke kontrolü yapar. |
| 5 | `qa` | sonnet | Bağımsız son kontrol: değişiklikleri task'a ve sözleşmeye karşı denetler, mutation ve uçtan uca testleri çalıştırır, gerçek modelle cevapları dener. |

Tanımlar `.claude/agents/` altındadır. `backend-coder` ve `frontend-coder` paralel çalışır.

## tmux ile agent ekibi

Task'lar Claude Code agent ekibiyle yürür; her agent kendi tmux panelinde açılır. macOS Terminal ya da iTerm2 kullan (VS Code terminali ve Ghostty'de paneller açılmaz).

```bash
brew install tmux                    # bir kez
cd ~/Documents/rag
tmux new -s rag                      # oturumu aç
claude                               # lead oturumu
```

Lead'e yaz: `docs/tasks/TASK-XXX/TASK.md için akışı başlat`

| Komut | Ne yapar |
|-------|----------|
| `Ctrl+b` → ok tuşları | Paneller arasında geçiş |
| `Ctrl+b` → `z` | Paneli tam ekran yap / geri al |
| `Ctrl+b` → `d` | Oturumdan ayrıl (ekip çalışmaya devam eder) |
| `tmux attach -t rag` | Oturuma geri dön |
| `claude --resume` | Önceki Claude oturumuna devam et (tmux içinde) |

Ayrıntılar: `KULLANIM-KILAVUZU.md` (agent ekibi).
