# RAG Asistan

RAG'i (Retrieval-Augmented Generation) anlatan ve Raşit Bankası dijital asistanı örneğiyle çalışan bir demoda gösteren eğitim projesi. Başlangıç içeriği: `kaynaklar/rag.html` (anlatım) ve `kaynaklar/rag-lab.html` (lab). Önceki projenin (embedding ve pgvector) devamıdır; bilgi bankası bu repoda yeniden yazılır.

> Durum: proje iskeleti (TASK-002) ve lokal modeller (TASK-003) hazır. Servis kodu modellere henüz bağlı değil (TASK-005/008/009).

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
