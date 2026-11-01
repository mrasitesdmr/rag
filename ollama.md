# Lokal modeller (Ollama + llama-server)

Bu rehberdeki her komut, Apple M5 / 16 GB makinede çalıştırılıp çıktısı alınmıştır (TASK-003). Komutlar repo kökünden çalışır.

## 1. Ne nerede çalışır

```
Mac (Metal GPU)                                   Docker Desktop
├─ Ollama      127.0.0.1:11434  qwen3.5:9b (dil modeli) + bge-m3 (embedding)
├─ llama-server 127.0.0.1:8081  bge-reranker-v2-m3 (reranker)
└─────────────── host.docker.internal ◄──────────── assistant-api, retrieval-api
```

- Üç model de Mac'te ve GPU'da çalışır. Docker Desktop for Mac container'lara GPU vermez; 9B model CPU'da çok yavaş kalır ve Docker'a ayrılan ~7,75 GB ona yetmez. Bu yüzden modeller Compose'da değildir.
- İki sunucu da sadece `127.0.0.1`'i dinler (yerel ağa açık değil). Container'lar ikisine `host.docker.internal` ile ulaşır (`extra_hosts: host.docker.internal:host-gateway`).
- Reranker Compose'da değildir; aşağıdaki komutla ayrı terminalde açılır.

## 2. Kurulum

```bash
brew install ollama llama.cpp
ollama --version        # Warning satırı: sunucu kapalıysa görünür; "client version is 0.40.2"
llama-server --version  # version: 0.8.0 (build 11960, commit 8642541b2)
```

## 3. Modelleri indirme

```bash
ollama pull qwen3.5:9b
ollama pull bge-m3
ollama list
```

```
NAME             ID              SIZE      MODIFIED
qwen3.5:9b       d63f830f28d5    7.6 GB    ...
bge-m3:latest    790764642607    1.2 GB    ...
```

Reranker ayrıca indirilmez: aşağıdaki başlatma komutunun ilk çalıştırmasında (~0,6 GB) Hugging Face önbelleğine iner: `~/.cache/huggingface/hub/models--gpustack--bge-reranker-v2-m3-GGUF/`. Sonraki açılışlar internetsizdir.

### Modeller ve sürümler
| Model | Nerede | Etiket / dosya | ID | Disk | Not |
|-------|--------|----------------|----|------|-----|
| Dil modeli | Ollama | `qwen3.5:9b` | `d63f830f28d5` | 7,6 GB | düşünme modeli; `think:false` ile kullanılır |
| Embedding | Ollama | `bge-m3` (`bge-m3:latest`) | `790764642607` | 1,2 GB | 1024 boyut |
| Reranker | llama-server | `gpustack/bge-reranker-v2-m3-GGUF`, `bge-reranker-v2-m3-Q8_0.gguf` (Q8_0) | — | 608 MB | `--reranking` |

## 4. Başlatma ve durdurma

Sıra: Ollama → reranker → Compose.

**Ollama** (girişte otomatik başlar):
```bash
brew services start ollama
brew services stop ollama
ollama serve              # alternatif: ön planda, terminal kapanınca durur
ollama ps                 # bellekteki modeller
ollama stop qwen3.5:9b    # modeli bellekten çıkar
```

**Reranker** (ayrı terminalde, ön planda; Ctrl+C ile durur):
```bash
llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99
```
Logda `server is listening on http://127.0.0.1:8081` görününce hazırdır. Terminal kapanınca ya da bilgisayar yeniden başlayınca durur; komutla yeniden başlatılır. `--n-gpu-layers 99` tüm katmanları Metal'e aktarır (logda `offloading 23 repeating layers to GPU`, `using device MTL0 (Apple M5)`). `--batch-size/--ubatch-size 2048` uzun parçalarda `input is too large` hatasını önler.

**Compose:** `docker compose -f deployment/docker-compose.yml up --build -d`

## 5. Duman testleri

**1. Embedding** (beklenen: `1024`):
```bash
curl -s localhost:11434/api/embed -d '{"model":"bge-m3","input":"Gold kartın yıllık aidatı ne kadar?"}' | python3 -c 'import sys,json; print(len(json.load(sys.stdin)["embeddings"][0]))'
```
```
1024
```

**2. Dil modeli** (kısa Türkçe cevap, `done_reason` `stop`, düşünme metni boş; iki çalıştırmada aynı metin):
```bash
curl -s localhost:11434/api/chat -d '{"model":"qwen3.5:9b","stream":false,"think":false,"options":{"temperature":0,"num_ctx":8192},"messages":[{"role":"user","content":"Türkiye’nin başkenti hangi şehirdir? Tek cümleyle cevap ver."}]}' | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d["message"]["content"], d["done_reason"], repr(d["message"].get("thinking","")))'
```
```
Türkiye'nin başkenti Ankara'dır. stop ''
```

**3. Reranker** (iki skor, ilgili metin daha yüksek; skor ham logit'tir, 0-1 arası değildir, sadece sıra anlamlıdır):
```bash
curl -s localhost:8081/v1/rerank -H 'Content-Type: application/json' -d '{"model":"bge-reranker-v2-m3","query":"Gold kartın yıllık aidatı ne kadar?","documents":["Gold kartın yıllık aidatı kart başvurusunda belirtilir ve her yıl hesaptan tahsil edilir.","Şubelerimiz hafta içi sabah dokuzdan akşam beşe kadar açıktır."]}' | python3 -c 'import sys,json; r=sorted(json.load(sys.stdin)["results"],key=lambda x:x["index"]); [print(x["index"],x["relevance_score"]) for x in r]; print("OK" if r[0]["relevance_score"]>r[1]["relevance_score"] else "FAIL")'
```
```
0 5.359559535980225
1 -11.0427885055542
OK
```

**Reranker kapalıyken** (Ctrl+C sonrası) aynı komut `curl: (7) Failed to connect to localhost port 8081` verir (çıkış kodu 7; boru hattında Python'a boş girdi gittiği için ayrıca JSON hatası görünür). Docker'daki dört servis `healthy` kalır.

Container'dan erişim:
```bash
docker compose -f deployment/docker-compose.yml exec assistant-api curl -s http://host.docker.internal:11434/api/version   # {"version":"0.40.2"}
docker compose -f deployment/docker-compose.yml exec assistant-api curl -s http://host.docker.internal:8081/health         # {"status":"ok"}
```

## 6. Dil modeli ayarları
- `temperature 0`: aynı soruya aynı cevap; değerlendirme ve testler tekrarlanabilir.
- `num_ctx 8192`: prompt + numaralı parçalar + cevap sığar; daha büyüğü bellek ister.
- `think: false`: `qwen3.5:9b` düşünme modelidir; kapatılmazsa cevabın başına düşünme metni karışır ve süre uzar.
Bağlantı TASK-008'de bu değerleri kullanır (ortam değişkeni değil, properties varsayılanıdır).

## 7. Bellek notları
Makine: Apple M5, 16 GB, Metal'in kullanılabilir belleği ~12 GB. Üç model birlikte yüklüyken ölçüm (Docker'daki dört servis açık):

| Bileşen | Bellek |
|---------|--------|
| `qwen3.5:9b` (`ollama ps`, num_ctx 8192) | 7,8 GB, 100% GPU |
| `bge-m3` (`ollama ps`) | 673 MB, 100% GPU |
| Reranker llama-server | RSS 387 MB, `phys_footprint` 217 MB (+ Metal arabelleği ~388 MiB: ağırlık 308 + KV 80) |
| Docker: web / assistant-api / retrieval-api / postgres | 16 / 435 / 550 / 102 MiB (Docker VM'e ayrılan 7,75 GB içinde) |
| Swap (`sysctl vm.swapusage`) | başlangıç 9228 MB → ölçüm sonu 9699 MB (+~470 MB) |
| Serbest bellek (`memory_pressure`) | %68 |

- Swap'in büyük kısmı (9 GB) ölçümden önce de doluydu (başka uygulamalar); üç modelin eklediği artış küçüktür.
- Süreler:

| Çağrı | Soğuk (ilk yükleme) | Sıcak |
|-------|---------------------|-------|
| Embedding (`/api/embed`) | 1,10 sn | 0,019 sn |
| Dil modeli (12 token cevap) | 9,7 sn (yükleme 4,9 sn) | 0,52 sn |
| Reranker, 20 belge × ~1000 karakter | — | 0,65 sn |

- Keep-alive: Ollama varsayılanı, 5 dakika; sonra modeller bellekten çıkar. Reranker açık kaldıkça bellekte kalır.
- Üç model birlikteyken başka büyük model (ör. diskteki `qwen3-vl:8b`) açma; swap artar ve her şey yavaşlar.

## 8. Servislerin kullanacağı ortam değişkenleri
Compose'da ayarlıdır. retrieval-api değişkenlerini TASK-005'ten beri okur; assistant-api olanları sonraki task'larda okunur.

| Servis | Değişken | Docker değeri | Lokal varsayılan | Okuyacak task |
|--------|----------|---------------|------------------|---------------|
| retrieval-api | `OLLAMA_BASE_URL` | `http://host.docker.internal:11434` | `http://localhost:11434` | TASK-005 (okunuyor) |
| retrieval-api | `EMBEDDING_MODEL` | `bge-m3` | `bge-m3` | TASK-005 (okunuyor) |
| retrieval-api | `EMBEDDING_DIMENSIONS` | `1024` | `1024` | TASK-005 (okunuyor) |
| assistant-api | `OLLAMA_BASE_URL` | `http://host.docker.internal:11434` | `http://localhost:11434` | TASK-008 |
| assistant-api | `CHAT_MODEL` | `qwen3.5:9b` | `qwen3.5:9b` | TASK-008 |
| assistant-api | `RERANKER_URL` | `http://host.docker.internal:8081` | `http://localhost:8081` | TASK-008 |
| assistant-api | `RERANKER_MODEL` | `bge-reranker-v2-m3` | `bge-reranker-v2-m3` | TASK-008 |

### Belge yükleme (TASK-005, TASK-027)
retrieval-api açılışta belge içe almaz; PDF yüklenince `bge-m3` ile içe alınır (rehber için yaklaşık 154 parça, ~9 sn). **Sıra: Ollama açık → yükleme.** Ollama kapalıyken yüklenen belgenin işi `FAILED` + `EMBEDDING_UNAVAILABLE` olur (servis yine `healthy`); Ollama'yı açıp `curl -X POST localhost:8080/api/workspaces/42/sources/<id>/ingestion-jobs` ile yeniden dene. Durum: `curl localhost:8080/api/workspaces/42/ingestion-jobs/<id>`.

## 9. Sorun giderme
- `curl: (7) Failed to connect` (11434 / 8081): sunucu kapalı. `brew services start ollama` ya da reranker komutunu çalıştır.
- `model "…" not found`: `ollama pull <model>`; `ollama list` ile kontrol et.
- `address already in use`: `lsof -i :11434` / `lsof -i :8081` ile süreci bul. `ollama serve` ile brew servisini aynı anda çalıştırma.
- Reranker ilk açılışta uzun bekliyor: model (~0,6 GB) iniyor. İnternet gerekir, yalnızca bir kez.
- `input is too large`: `--ubatch-size` küçük kalmış; komuttaki 2048 değerlerini kullan (~1.350 token'lık belge 200 döner).
- Container'dan `host.docker.internal:8081`'e erişilemiyor: reranker çalışıyor mu (`curl localhost:8081/health`)? `docker compose … config` çıktısında `extra_hosts` var mı? Bu makinede `--host 127.0.0.1` ile erişim çalışıyor; çalışmazsa `0.0.0.0`'a geçmeden önce karar sorulur (reranker'da kimlik doğrulama yok).
- Swap ve yavaşlama: `sysctl vm.swapusage`, `ollama ps`; gereksiz modelleri `ollama stop <model>` ile çıkar.
- Reranker logunda `CORS allows all origins` uyarısı: sunucu sadece 127.0.0.1'de dinlediği için kabul edilmiştir.
