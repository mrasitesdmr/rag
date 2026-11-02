# RAG Asistan

RAG'i (Retrieval-Augmented Generation) anlatan ve Raşit Bankası dijital asistanı örneğiyle çalışan bir demoda gösteren eğitim projesi. Başlangıç içeriği: `kaynaklar/rag.html` (anlatım) ve `kaynaklar/rag-lab.html` (lab). Önceki projenin (embedding ve pgvector) devamıdır; bilgi bankası bu repoda yeniden yazılır.

> Durum: proje iskeleti (TASK-002), lokal modeller (TASK-003) ve belge yükleme ve içe alma (TASK-005, TASK-027) hazır. assistant-api henüz modellere bağlı değil (TASK-008/009).

**Gereksinimler:** Docker Desktop; local geliştirme için Java 26 ve Node 26; lokal modeller için Homebrew `ollama` ve `llama.cpp`.

## Lokal modeller

Dil modeli, embedding ve reranker Mac'te (Metal GPU) çalışır, Docker'da değil. Tam sistemi açmadan önce Ollama ve reranker açılır; kurulum, `ollama pull` komutları, reranker başlatma komutu, duman testleri ve bellek notları: [`ollama.md`](ollama.md).

```bash
brew services start ollama
llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99   # ayrı terminalde
```

## Çalıştırma

Repo kökünden:

```bash
docker compose -f deployment/docker-compose.yml up --build -d   # başlat
docker compose -f deployment/docker-compose.yml ps              # dördü de healthy olmalı
docker compose -f deployment/docker-compose.yml logs --tail=100 assistant-api
docker compose -f deployment/docker-compose.yml down            # durdur
docker compose -f deployment/docker-compose.yml down -v         # durdur + veriyi sıfırla
```

Site: http://localhost:3000 (assistant-api 8090, retrieval-api 8080, PostgreSQL 127.0.0.1:5432).

- **Bilgi bankası ilk açılışta boştur.** Belgeyi Bilgi kaynakları ekranından ya da `curl -F file=@rehber.pdf -F name="Ürün ve Hizmet Rehberi" -F version=2026.10 localhost:3000/api/workspaces/42/sources` ile yükle (PDF'te yer imi gerekir; `bge-m3` için Ollama açık olmalı). Durum: `curl localhost:8080/api/workspaces/42/ingestion-jobs/<id>`. Başarısız işi yeniden dene: `curl -X POST localhost:8080/api/workspaces/42/sources/<id>/ingestion-jobs`. Eski sürümden kalan rehber verisini silmek için `docker compose -f deployment/docker-compose.yml down -v` (veritabanını kalıcı siler).
- **Önizleme'nin çalışması için sıra:** Ollama (`qwen3.5:9b`, `bge-m3`) → reranker (llama-server) → Compose → rehberi yükle → `localhost:3000/onizleme`'de soru sor. Reranker kapalıysa cevap yine gelir (vector sırasının ilk 5'i, "reranker DOWN"); Ollama kapalıysa "Dil modeline şu an ulaşılamıyor" görünür. Stratejiyi değiştirmek: `RETRIEVAL_STRATEGY=top5 docker compose -f deployment/docker-compose.yml up -d assistant-api`; süre kartına makine etiketi: `ASSISTANT_HOST_LABEL="Apple M5 · 16 GB"` (aynı komut). Takip sorusunu yeniden yazmayı kapatmak: `QUERY_REWRITE_ENABLED=false` (aynı komut; varsayılan açık). Sadece LLM modu (retrieval/rerank yok): `GENERATION_MODE=llm docker compose -f deployment/docker-compose.yml up -d assistant-api`; geri dönmek için değişkensiz aynı komut (mod ortam değişkenidir, servis yeniden başlar).
- **Önerileri yükleme (Önizleme çipleri, "Soru listesi", otomatik tamamlama):** liste ilk açılışta boştur; ürün ekibi bir JSON dosyasıyla yükler, her yükleme bütün listeyi değiştirir: `curl -X PUT http://localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' --data-binary @suggestions.json`. Okumak: `curl localhost:3000/api/assistant/suggestions`. Temizlemek: `curl -X PUT http://localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' -d '{"categories":[]}'`. Dosya biçimi (`starter` ve `followUps` isteğe bağlı; en fazla 50 kategori, toplam 500 soru, soru başına 5 takip):

```json
{"categories":[{"name":"Kartlar","questions":[
  {"text":"Kartımı kaybettim, ne yapmalıyım?","starter":true,"followUps":["Yenisi kaç günde gelir?"]},
  {"text":"Kart aidatı ne kadar?"}]}]}
```

  Hatalı dosya 400 `VALIDATION_FAILED` döner ve kayıtlı liste değişmez. Gövde sınırı 2 MB (nginx).
- Tam sistem ve `docker-compose.dev.yml` ikisi de 5432'yi kullanır; tam sistemden önce `docker compose -f deployment/docker-compose.dev.yml stop` (veri silmez).
- PostgreSQL init script'i (iki veritabanı ve kullanıcı) sadece boş volume'da çalışır; script değişirse `down -v` gerekir.
- `../embedding-pgvector` aynı portları (5432, 8080, 3000) kullanır; iki proje aynı anda açılmaz, önce diğerini `down` et.
- Geliştirme (servisler bilgisayarda): `docker compose -f deployment/docker-compose.dev.yml up -d`, sonra modül `CLAUDE.md`'lerindeki komutlar.

## Ne geliştiriliyor

Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. Modüller: `web/`, `assistant-api/`, `retrieval-api/`, `deployment/`.

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
