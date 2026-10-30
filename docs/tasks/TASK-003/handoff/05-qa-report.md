TASK: TASK-003
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality` (severity etiketleri). Uygulama kodu, DTO, SSE ve test değişmediği için `java-code-review`, `springboot-verification`, `rag-pipeline`, `frontend-*` yüklenmedi.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|------------------------|--------------------|-------|
| 1 | Üç model: `qwen3.5:9b`, `bge-m3`, reranker `bge-reranker-v2-m3` (Q8_0, kullanıcı kararı A) | `ollama list`: `qwen3.5:9b d63f830f28d5`, `bge-m3:latest 790764642607`. HF önbelleğinde `bge-reranker-v2-m3-Q8_0.gguf`. `ollama.md` "Modeller ve sürümler" tablosu bunlarla birebir aynı | ✅ |
| 2 | Ollama 11434, llama-server 8081; adres ve model adı ortam değişkeninden, kodda sabit adres yok | `compose config`: assistant-api `OLLAMA_BASE_URL`, `CHAT_MODEL`, `RERANKER_URL`, `RERANKER_MODEL`; retrieval-api `OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS=1024`. `grep -rn "11434\|8081" assistant-api/src retrieval-api/src` boş. `lsof`: iki sunucu da yalnızca `127.0.0.1` dinliyor | ✅ |
| 3 | Çalışma yeri (host, GPU), keep-alive, ölçülen bellek | `ollama.md` §1 ve §7. Kendi ölçümüm: `ollama ps` qwen 7,8 GB 100% GPU ctx 8192, bge-m3 673 MB 100% GPU. Keep-alive 5 dk (`UNTIL 4 minutes from now`). Swap 9725 MB (belgedeki 9228→9699 ile aynı mertebe, ek artış küçük) | ✅ |
| 4 | `temperature 0`, `num_ctx 8192` | Duman testi komutunda var. `ollama ps` CONTEXT 8192. İki çalıştırma aynı metni verdi | ✅ |
| 5 | Üç duman testi | Aşağıdaki "Komutlar" tablosu | ✅ |
| 6 | `ollama.md`: kurulum, indirme, başlat/durdur, duman testleri, bellek, sorun giderme | §2, §3, §4, §5, §7, §9 var. Komutlar bu makinede çalıştırıldı (aşağıda) | ✅ |
| 7 | Uygulama kodu modellere bağlanmaz | `git diff --name-only`: `README.md`, `deployment/CLAUDE.md`, `deployment/docker-compose.yml`; yeni dosya `ollama.md`. `assistant-api/`, `retrieval-api/`, `web/`, `pom.xml` değişmedi | ✅ |
| AC1 | Duman testi çıktıları raporda | devops raporunda var, benim çalıştırmam da aynı | ✅ |
| AC2 | Üç modelle ölçülen bellek raporda | `04-deploy-report.md` ve `ollama.md` §7 aynı sayıları taşıyor | ✅ |
| S1 | `ollama.md` izlenir, üç test beklenen çıktıyı verir | Birebir aynı çıktılar | ✅ |
| S2 | Reranker 8081'de, Ollama 11434'te cevap verir; container'lar ulaşır | `assistant-api` ve `retrieval-api` içinden `host.docker.internal:11434/api/version` → `{"version":"0.40.2"}`; `assistant-api` içinden `:8081/health` → `{"status":"ok"}` | ✅ |
| S3 | Reranker durdurulunca bağlantı hatası, servisler sağlıklı | Aşağıda | ✅ |

## Sözleşme uyumu
Uygulanamaz: `docs/api/` değişmedi, endpoint ve DTO yok. Tek ilgili sözleşme env değişkenleridir ve `ollama.md` §8 tablosu ile `compose config` aynı.

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `ollama --version`, `llama-server --version` | 0.40.2; version 0.8.0 (build 11960, commit 8642541b2) | Evet |
| `ollama list` | ID'ler tabloyla aynı | Evet |
| Duman 1 embedding | `1024` | Evet |
| Duman 2 dil modeli (iki kez) | `Türkiye'nin başkenti Ankara'dır. stop ''` ve aynısı | Evet, deterministik |
| Duman 3 reranker | `0 5.359559535980225`, `1 -11.0427885055542`, `OK` | Evet, skorlar aynı |
| Uzun belge (1450 token) rerank | HTTP 200, skor 4,71 / -10,35 | Evet (`input is too large` yok) |
| Container erişimi (3 komut) | Belgedeki çıktılarla aynı | Evet |
| Reranker `kill -INT 82278` sonrası rerank ve `/health` | curl çıkış kodu 7; süreç yok. Boru hattında Python `JSONDecodeError` görünüyor | Evet; belge bunu zaten yazıyor |
| Aynı anda `docker compose ps` | Dört servis `healthy`; `actuator/health` iki serviste `UP`, web 200 | Evet |
| `brew services stop ollama` sonrası `/api/version` | çıkış kodu 7; dört servis hâlâ `healthy` | Evet |
| `./mvnw verify`, `npm run build/lint/test` | Uygulanamaz: kod değişmedi | — |

## Mutation testi
Uygulanamaz: kod ve test değişmedi.

## Uçtan uca
Playwright çalıştırılmadı: web ve servis kodu değişmedi. Dil modeli içeren task'ın RAG soruları da yok, çünkü servisler modele bağlı değil (TASK-005/008/009). Elle senaryolar:
- Senaryo 1 (belgeyi izle, üç test): ✅
- Senaryo 2 (Compose açık, reranker 8081, Ollama 11434, container erişimi): ✅
- Senaryo 3 (reranker durur → bağlantı hatası): ✅

## Son durum
Üç parça da açık bırakıldı. Reranker nohup ile yeniden başlatıldı: **yeni pid 82879** (eski 82278 durduruldu), `127.0.0.1:8081`, `/health` ok. Ollama brew service açık (`0.40.2`). Compose dört servis healthy. Yeniden başlatma sonrası embedding `1024`, rerank skorları aynı. Reranker logu: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/3983d5d5-5faa-43f0-9970-1fc22443acae/scratchpad/reranker.log`. Kapanış: `kill -INT 82879`.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional | `ollama.md` §5 reranker kapalı notu | Kapalıyken komut `curl: (7)` yanında Python `JSONDecodeError` izi de basıyor. Belge bunu zaten söylüyor. İstenirse `curl -f` ile ya da `set -o pipefail` ile temizlenir | devops |
| FYI | `ollama ps` | Dil modeli `mlx` runner'ında, bge-m3 `llamacpp`'de çalışıyor; bge-m3 bağlamı 4096. Bellek rakamları belgeyle uyumlu | — |
| FYI | Bellek | Swap ölçümden önce de ~9,7 GB dolu (başka uygulamalar). Üç model GPU'da kalıyor ama bellek baskısı gerçek; kartın riski duruyor. Reranker'ın kalıcı servis olması ayrı task önerisi | lead |
| FYI | `qwen3-vl:8b` kopyaları | Dokunulmadı | — |
| Nit | `04-deploy-report.md` | "Bellek ölçümü" yeniden üretilmedi; sadece `ollama ps` ve swap'ı kontrol ettim, diğer RSS değerleri devops ölçümüne dayanıyor | — |

Critical ya da Required bulgu yok.

## Düzeltme
Yok.

STATUS: DONE
