TASK: TASK-003
# Plan

## Özet
**Mevcut durum:**
- Modeller hiçbir yerde bağlı değil. `deployment/docker-compose.yml`'de postgres, assistant-api, retrieval-api ve web var.
- `ollama.md` yok.
- Makine (lead'in tespiti): Apple M5, 16 GB RAM, 10 çekirdek. Docker Desktop'a ~7,75 GB bellek ve 10 CPU ayrılmış.
- Ollama 0.40.2 Homebrew ile kurulu ama çalışmıyor. `qwen3.5:9b` ve `bge-m3:latest` indirilmiş.
- Lead'in "llamacpp" diye gördüğü iki manifest reranker değil. Planner manifestleri okudu: ikisi de `qwen3-vl:8b` / `qwen3-vl:8b-instruct` türevi (her biri ~5,0 GB model + ~1,16 GB projector). Reranker GGUF'u indirilmemiş.
- llama-server (Homebrew, build 11960) kurulu, 8081 boş.

**Yeni durum (kullanıcı kararları: soru 1 = A, soru 2 = B, soru 3 = A):**
- **Ollama** Mac'te Homebrew servisi olarak çalışır (Metal GPU), `127.0.0.1:11434`'te. Dil modeli `qwen3.5:9b` ve embedding modeli `bge-m3` (1024 boyut) buradadır. Keep-alive Ollama varsayılanıdır (5 dk).
- **Reranker** `bge-reranker-v2-m3` (GGUF, Q8_0) Mac'te Homebrew llama-server ile çalışır (Metal GPU), `127.0.0.1:8081`'de. Ayrı bir terminalde `ollama.md`'deki tek komutla başlatılır, Ctrl+C ile durur. Compose'a reranker servisi **eklenmez**.
- **Senaryo 2'deki değişiklik (kullanıcının kararı, soru 2 = B):** TASK.md'deki "Kullanıcı Compose'u başlatır; reranker 8081'de, Ollama 11434'te cevap verir" senaryosu şu hâle gelir: reranker Compose ile değil, `ollama.md`'deki komutla açılır. Compose açıkken container'lar reranker'a da Ollama'ya da `host.docker.internal` üzerinden ulaşır.
- Model ortam değişkenlerinin adları bu task'ta sabitlenir. Değerleri compose'da assistant-api ve retrieval-api'ye verilir, `extra_hosts` eklenir. Servis kodu bunları henüz okumaz (kural 7); okuma TASK-005, TASK-008 ve TASK-009'da başlar.
- `ollama.md` yazılır: kurulum, indirme, başlat/durdur, üç duman testi, bellek notları ve ölçümler, ortam değişkenleri, sorun giderme.
- Uygulama kodu, test kodu, `docs/api/**` değişmez. **Değişen servis yok** (assistant-api ve retrieval-api'nin kodu da `pom.xml`'i de aynı kalır; compose'da sadece ortam değişkenleri ve `extra_hosts` eklenir).

**Uygulama sırası:** devops → qa.

## Çalışacak roller
- **planner:** bu plan ve onay kartı.
- **devops:** compose değişikliği (iki servise ortam değişkenleri ve `extra_hosts`), `ollama.md`, `README.md` ve `deployment/CLAUDE.md`. Modelleri indirir, Ollama'yı ve reranker'ı başlatır, duman testlerini ve bellek ölçümünü çalıştırır, çıktıları `handoff/04-deploy-report.md`'ye yazar.
- **qa:** `ollama.md`'yi baştan sona izler, her komutun bu makinede çalıştığını ve çıktıların raporla tutarlı olduğunu kontrol eder. Üç senaryoyu (senaryo 2'nin yeni hâliyle) ve kabul kriterlerini doğrular.
- **Atlananlar:**
  - backend-coder: kural 7, uygulama kodu modellere bağlanmaz. Servislerin kodu ve `pom.xml`'i değişmez.
  - frontend-coder: web etkilenmiyor.
  - tester: yazılacak kod yok, dolayısıyla unit, controller ya da e2e testi de yok. Duman testleri tek komutluk terminal komutlarıdır, devops çalıştırır. qa, kök `CLAUDE.md` madde 2 gereği doğrudan devops'a bağlanır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Lab modelleri başlangıç önerisi. Kesin model ve sürüm kartta sorulur, her indirme plan kararı | Kullanıcı kararı 1 = A. Kararlar #1, #2. `ollama.md` "Modeller ve sürümler" tablosu (Ollama etiketi + `ollama list` ID'si; reranker için HF deposu + dosya adı) | D-S1, D-S2, D-S3 (doğru model adı cevap veriyor); qa Q-1 (tablo ile `ollama list` çıktısı aynı) |
| K2 | Ollama 11434, llama-server 8081; servisler adres ve model adını ortam değişkeninden alır, kodda sabit adres yok | Ollama `127.0.0.1:11434`, reranker `--host 127.0.0.1 --port 8081` (Kararlar #11). Ortam değişkenleri tablosu (D-2). Servis kodu değişmez | D-S4 (container'lardan erişim), D-S5 (`compose config`'te değişkenler), qa Q-3 (servis kodunda `11434`/`8081` geçmiyor: `grep -rn "11434\|8081" assistant-api/src retrieval-api/src` boş) |
| K3 | Kartta nerede çalışacakları (host/Docker, GPU), keep-alive ve ölçülen bellek | Kullanıcı kararları 2 = B, 3 = A; kartta "Benim verdiğim kararlar". Kararlar #3, #4, #5. Bellek ölçümü D-M1 | D-M1 (ölçüm raporda), qa Q-2 |
| K4 | Dil modeli `temperature 0`, `num_ctx 8192` | D-S2 isteğinde `options: {temperature: 0, num_ctx: 8192}` ve `think: false`. `ollama.md`'de "Dil modeli ayarları" (servis bağlantısı TASK-008'de bu değerleri kullanır) | D-S2 |
| K5a | Embedding duman testi 1024 boyut döner | `ollama.md` duman testi 1 | D-S1 |
| K5b | Dil modeli duman testi kısa Türkçe cevap döner | `ollama.md` duman testi 2 | D-S2 |
| K5c | Reranker duman testi iki skor döner, ilgili metin daha yüksek | `ollama.md` duman testi 3 | D-S3 |
| K6 | `ollama.md`: kurulum, indirme, başlat/durdur, duman testleri, bellek notları, sorun giderme | D-3 (bölüm listesi) | qa Q-1 (her bölüm var, her komut çalıştırıldı) |
| K7 | Uygulama kodu modellere bağlanmaz | Rol kapsamı: backend-coder yok. Compose'a sadece ortam değişkeni ve `extra_hosts` eklenir | qa Q-3 (`git diff --stat` içinde `assistant-api/src`, `retrieval-api/src`, `pom.xml` yok) |
| S1 | `ollama.md` adımları izlenir, üç duman testi beklenen çıktıyı verir | D-3 | qa Q-1 |
| S2 | (Kullanıcı kararıyla değişti) Compose başlatılır, reranker `ollama.md`'deki komutla açılır; reranker 8081'de, Ollama 11434'te cevap verir | Ollama brew servisi; reranker komutu (D-1); compose'da ortam değişkenleri (D-2) | D-S6, D-S4 |
| S3 | Reranker durdurulunca reranker duman testi bağlantı hatası verir | `ollama.md` "Reranker'ı durdurma" (Ctrl+C) | D-S7 |
| AC1 | Üç duman testinin çıktıları raporda | `04-deploy-report.md` | D-S1, D-S2, D-S3 |
| AC2 | Üç model birlikte yüklüyken ölçülen bellek raporda | `04-deploy-report.md` + `ollama.md` "Bellek" tablosu | D-M1 |
| AC3 | `ollama.md` yazılmış, her komut bu makinede çalışıyor | D-3 | qa Q-1 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Üç model birlikte Metal'in kullanılabilir belleğini doldurur; Ollama modellerden birini bellekten atar, her soruda model yeniden yüklenir | Üç duman testinden sonra `ollama ps` iki modeli birlikte gösterir ve ikisinde de `PROCESSOR` "100% GPU" yazar. Reranker de GPU'da kalır (llama-server açılış logunda Metal'e katman aktarımı). Böyle olmazsa devops raporda belirtir (risk) | D-M1 |
| G2 | Uzun bir parça reranker'ın tek seferde işleyebileceği sınırı (varsayılan ubatch 512 token) aşar, llama-server 500 döner | Reranker `--batch-size 2048 --ubatch-size 2048 --ctx-size 8192` ile başlar. ~1.500 token'lık belge 200 ve skor döner | D-S8 |
| G3 | Container'dan Mac'teki Ollama'ya ya da reranker'a erişilemez: iki sunucu da sadece `127.0.0.1`'i dinliyor, ya da adres yanlış | assistant-api ve retrieval-api container'larından `host.docker.internal:11434`'e, assistant-api'den `host.docker.internal:8081`'e istek başarılı olur (Kararlar #11) | D-S4 |
| G4 | Ollama ya da reranker kapalıyken Docker'daki servisler sağlıksız görünür ya da açılmaz | Healthcheck'ler modele bakmaz. Reranker'ı durdurunca (Ctrl+C) da, `brew services stop ollama` sonrasında da postgres, assistant-api, retrieval-api ve web `healthy` kalır | D-S7 |
| G5 | `qwen3.5:9b` düşünme modeli; cevabın başına düşünme metni karışır | İstekte `think: false`. Yanıtta `message.thinking` boştur ya da yoktur, `content` `<think>` içermez | D-S2 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | Model sürümü: Ollama modelleri etiket + `ollama list` ID'si (12 karakter) ile, reranker HF deposu `gpustack/bge-reranker-v2-m3-GGUF` + dosya `bge-reranker-v2-m3-Q8_0.gguf` ile kaydedilir. Kesin dosya adını devops HF sayfasından doğrular; farklıysa raporda yazar | Ollama etiketleri değişebilir; ID hangi ağırlıkların ölçüldüğünü sabitler | Sadece etiket: `ollama pull` ileride farklı ağırlık getirebilir, ölçüm tekrarlanamaz |
| 2 | Reranker nicemleme: Q8_0 (kullanıcı kararı 1 = A) | ~0,6 GB; sıralama kalitesi F16'ya çok yakın, bellek yarısı. Nicemleme karşılaştırması kapsam dışı | F16: bellek ~2 kat. Q4: sıralama kalitesi düşebilir |
| 3 | Ollama Mac'te çalışır (Homebrew servisi, Metal GPU) | Docker Mac'te GPU kullanamaz. 9B model CPU'da çok yavaşlar, Docker'a ayrılan 7,75 GB ona yetmez | Ollama'yı Compose'a koymak: CPU, bellek yetmez |
| 4 | Reranker Mac'te Homebrew llama-server ile çalışır (Metal GPU), Compose'da değil (kullanıcı kararı 2 = B). Ayrı terminalde ön planda çalışır | Kullanıcı kararı. GPU'da 20 parçayı sıralamak hızlı | Compose servisi (CPU): kullanıcı seçmedi |
| 5 | Keep-alive: Ollama varsayılanı (5 dk), ek ayar yok (kullanıcı kararı 3 = A). Reranker açık kaldıkça bellekte kalır | Kullanımda üç model birlikte bellekte. Boşta 16 GB makinede bellek geri verilir | Sürekli bellekte ya da 30 dk: kullanıcı seçmedi |
| 6 | Ollama `brew services start ollama` ile başlar (girişte otomatik), `brew services stop ollama` ile durur. `ollama serve` alternatif olarak yazılır | Kurulum zaten Homebrew. Model yüklü değilken Ollama sunucusu az bellek kullanır | Sadece `ollama serve`: terminal kapanınca durur |
| 7 | Reranker modeli ilk başlatmada `-hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0` ile indirilir, llama.cpp'nin önbelleğinde (`~/Library/Caches/llama.cpp/`) durur. Sonraki başlatmalar önbellekten açılır. Gerçek yolu devops doğrular ve `ollama.md`'ye yazar | Tek komut, elle dosya indirme ya da taşıma yok | GGUF'u `curl` ile bir klasöre indirip `-m <yol>` ile açmak: ek elle adım, yol makineye bağlı |
| 8 | Ortam değişkeni adları şimdi sabitlenir, compose'da iki servise verilir (D-2). Sonraki task'lar properties'te `${OLLAMA_BASE_URL:http://localhost:11434}` biçiminde okur | Kural 2. Önceki projeyle aynı adlar (`OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS`). Container'dan erişim bu task'ta doğrulanabilir (G3) | Adları bağlantı task'larına bırakmak: üç task ayrı ad seçebilir |
| 9 | Duman testleri `ollama.md`'de tek satırlık `curl … \| python3 -c …` komutlarıdır; ayrı script dosyası yok | Kural 5 "tek komut". Önceki projede bu makinede aynı kalıp çalıştı | Script: bakımı gereken ek dosya |
| 10 | Reranker skoru ham logit'tir (0-1 arası değil). Duman testi sadece sırayı karşılaştırır | Lab notu: "sigmoid uygulanmadığı için 0-1 arasında değil, sadece sıralama için" | Eşik değeriyle test: skor ölçeği modele bağlı |
| 11 | Reranker `--host 127.0.0.1` ile dinler; Ollama da varsayılan `127.0.0.1:11434`'te kalır (`OLLAMA_HOST` değiştirilmez). Container'lar ikisine `host.docker.internal` ile ulaşır. devops D-S4 ile doğrular. Doğrulama başarısız olursa `0.0.0.0`'a kendi başına geçmez: raporda `NEEDS_FIX` (sorumlu planner) yazar ve lead'e bildirir | Docker Desktop for Mac'te `host.docker.internal`'a giden bağlantıyı Mac'teki Docker Desktop süreci açar, bu yüzden Mac'in loopback'ine ulaşır. Önceki projede Ollama varsayılan `127.0.0.1` ile dinlerken container'lar `host.docker.internal:11434` üzerinden bağlanıyordu (`../embedding-pgvector/ollama.md`). `127.0.0.1` reranker'ı yerel ağa açmaz; reranker'da kimlik doğrulama yok | `0.0.0.0`: aynı Wi-Fi'deki herkes reranker'a istek atabilir; gerekmiyor |
| 12 | Reranker komutu `--n-gpu-layers 99` içerir; Metal'e tüm katmanlar açıkça aktarılır | Varsayılan aktarım sürüme göre değişebilir; G1 için GPU'da çalıştığı kesin olmalı | Varsayılana güvenmek: sürüm değişince sessizce CPU'ya düşebilir |
| 13 | Reranker'ı arka plan servisi yapmak (launchd ya da `brew services`) bu task'ta **yok**. Ayrı task olarak önerilir | Kullanıcı tercihi: kapsam dar, risk ve açık maddeler ayrı task olur | Şimdi launchd plist'i yazmak: kapsam dışı, `ollama.md`'deki tek komutu karmaşıklaştırır |

## Sözleşme değişiklikleri
Yok. `docs/api/openapi.yaml`, `sse.md` ve `websocket.md` değişmez (REST ucu ya da akış yok).

## Backend planı
Yok (kural 7). Servis kodu, `pom.xml` ve `application.properties` değişmez. Ortam değişkenlerini okuyan property'ler TASK-005 (embedding, retrieval-api), TASK-008 (dil modeli) ve TASK-009'da (reranker) eklenir. Adlar D-2'dedir.

## Frontend planı
Yok.

## Test senaryoları
Otomatik test yok. Kontrolleri devops çalıştırır (D-S, D-M) ve komutunu, çıktısını `04-deploy-report.md`'ye yazar; qa elle doğrular (Q). Komutlar repo kökünden çalışır.

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| D-S1 | Embedding duman testi | smoke | `givenBgeM3Pulled_whenEmbedTurkishSentence_thenReturns1024Dimensions` | `curl -s localhost:11434/api/embed -d '{"model":"bge-m3","input":"Gold kartın yıllık aidatı ne kadar?"}' \| python3 -c 'import sys,json; print(len(json.load(sys.stdin)["embeddings"][0]))'` → `1024` |
| D-S2 | Dil modeli duman testi | smoke | `givenQwenLoaded_whenAskTurkishQuestion_thenReturnsShortTurkishAnswer` | `/api/chat`, `stream:false`, `think:false`, `options:{temperature:0,num_ctx:8192}`. Mesaj "Türkiye’nin başkenti hangi şehirdir? Tek cümleyle cevap ver." (tek tırnaklı shell gövdesini bozmamak için tipografik kesme işareti `’`). Python `message.content`'i ve `done_reason`'ı basar. Cevap "Ankara" içerir, Türkçedir, en fazla 2 cümledir. `done_reason` "stop". `message.thinking` yok ya da boş (G5). Aynı komut iki kez çalışınca aynı metni döner (temperature 0) |
| D-S3 | Reranker duman testi | smoke | `givenRerankerRunning_whenRerankRelevantAndIrrelevantText_thenRelevantScoresHigher` | `curl -s localhost:8081/v1/rerank -H 'Content-Type: application/json' -d '{"model":"bge-reranker-v2-m3","query":"Gold kartın yıllık aidatı ne kadar?","documents":["Gold kartın yıllık aidatı kart başvurusunda belirtilir ve her yıl hesaptan tahsil edilir.","Şubelerimiz hafta içi sabah dokuzdan akşam beşe kadar açıktır."]}'`. Python `results`'ı `index`'e göre sıralayıp iki skor basar, ardından `OK` (index 0 > index 1) ya da `FAIL` yazar. Beklenen: iki satır skor + `OK` |
| D-S4 | Container'lardan erişim (G3, Kararlar #11) | smoke | `givenComposeUp_whenServiceContainersCallHostModels_thenReachable` | `docker compose -f deployment/docker-compose.yml exec assistant-api curl -s http://host.docker.internal:11434/api/version` → `{"version":"0.40.2"}`. Aynısı `retrieval-api` ile. `exec assistant-api curl -s http://host.docker.internal:8081/health` → `{"status":"ok"}` |
| D-S5 | Ortam değişkenleri compose'da | smoke | `givenComposeFile_whenConfigRendered_thenModelEnvVarsPresent` | `docker compose -f deployment/docker-compose.yml config` çıktısında D-2 tablosundaki değişkenler değerleriyle doğru serviste; iki serviste `extra_hosts` `host.docker.internal=host-gateway`; `reranker` adında servis yok |
| D-S6 | Senaryo 2 (yeni hâli) | smoke | `givenComposeUpAndRerankerStarted_whenHealthChecked_thenRerankerAndOllamaRespond` | `up --build -d` sonrası `ps`'te dört servis `healthy`. Ayrı terminalde D-1 reranker komutu çalışır; logda `server is listening on http://127.0.0.1:8081` görünür. `curl -s localhost:8081/health` → `{"status":"ok"}`, `curl -s localhost:11434/api/version` → sürüm |
| D-S7 | Senaryo 3 + G4 | smoke | `givenRerankerStopped_whenRerankSmokeTest_thenConnectionRefused` | Reranker durdurulur. `ollama.md`'de Ctrl+C; devops arka planda başlattıysa `kill <pid>`. Sonra D-S3 komutu `curl: (7) Failed to connect to localhost port 8081` verir, çıkış kodu 7. `ps`'te dört servis `healthy` kalır. Ayrıca `brew services stop ollama` sonrası da dört servis `healthy`. Ardından reranker yeniden başlatılır, `brew services start ollama` çalışır, D-S3 yine `OK` verir |
| D-S8 | Uzun belge (G2) | smoke | `givenLongDocument_whenRerank_thenReturnsScoreWithoutBatchError` | ~6.000 karakterlik (~1.500 token) Türkçe belgeyle `/v1/rerank` → HTTP 200 ve bir skor; `input is too large` hatası yok. Komut `ollama.md`'ye girmez, sadece raporda |
| D-M1 | Üç model birlikte bellek (AC2, G1) | ölçüm | `givenAllThreeModelsLoaded_whenMemoryMeasured_thenReportedPerModelAndSwap` | Compose ve reranker açık. Önce `ollama stop qwen3.5:9b; ollama stop bge-m3` çalışır ve `sysctl vm.swapusage` alınır (başlangıç). Sonra D-S1, D-S3 ve D-S2 art arda çalışır. Ölçülenler: `ollama ps` (iki model, `SIZE`, `PROCESSOR` "100% GPU"); reranker için `ps -o rss= -p $(pgrep -f 'llama-server.*--port 8081')` (KB → MB) ve varsa `footprint -p <pid>` (Metal belleği RSS'te tam görünmeyebilir; hangisinin kullanıldığı raporda yazılır); `docker stats --no-stream --format '{{.Name}} {{.MemUsage}}'` (Docker'daki dört servis); `sysctl vm.swapusage` (son); `memory_pressure \| tail -1`. Raporda tablo: model başına bellek, Docker toplamı, swap farkı, serbest bellek yüzdesi |
| D-M2 | Süreler | ölçüm | `givenModelsCold_whenFirstAndSecondCall_thenLatenciesReported` | D-S1 ve D-S2 için soğuk (ilk yükleme) ve sıcak süre (`total_duration`, `load_duration`, `eval_count`). Reranker için 20 belge (her biri ~1.000 karakter) tek istekte `time` ile ölçülür. Raporda tablo |
| Q-1 | AC3, S1, K6 | qa elle | — | `ollama.md`'yi baştan izler. Her komut çalışır ve belgedeki örnek çıktıyla aynı biçimde sonuç verir. K6'daki bölümlerin hepsi var. Model ve sürüm tablosu `ollama list` ile ve reranker dosya adıyla aynı |
| Q-2 | K3, AC2 | qa elle | — | Raporda bellek tablosu var. `ollama.md` "Bellek" bölümü aynı sayıları ve keep-alive kararını taşıyor |
| Q-3 | K2, K7 | qa elle | — | `git diff --stat`'ta `assistant-api/src`, `retrieval-api/src`, `*/pom.xml`, `web/` yok. Servis kodunda `11434` ya da `8081` geçmiyor |

## Deployment planı
Yükleyeceği skill: yok (`deployment/CLAUDE.md` yeterli). Önceki projenin `../embedding-pgvector/ollama.md`'si biçim referansıdır, kopyalanmaz.

### D-1 Reranker'ı başlatma (Mac, Homebrew llama-server; Compose'da servis yok)
- Komut (tek satır, ayrı terminalde, ön planda):
  `llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99`
- İlk çalıştırmada model (~0,6 GB) llama.cpp önbelleğine iner (Kararlar #7). Sonraki çalıştırmalar internetsiz açılır.
- Durdurma Ctrl+C ile yapılır. 8081 doluysa `lsof -i :8081`.
- Bayraklar `llama-server --help` ile doğrulanır (build 11960). Adı değişen bayrak varsa (ör. `--reranking` / `--rerank`) gerçek ad kullanılır ve raporda yazılır.
- `docker-compose.yml` ve `docker-compose.dev.yml`'e reranker servisi eklenmez.

### D-2 Ortam değişkenleri (adlar sabit; servis kodu henüz okumaz)
| Servis | Değişken | Docker değeri | Lokal varsayılan (sonraki task'ların properties'i) | Okuyacak task |
|--------|----------|---------------|---------------------------------------------------|---------------|
| retrieval-api | `OLLAMA_BASE_URL` | `http://host.docker.internal:11434` | `http://localhost:11434` | TASK-005 |
| retrieval-api | `EMBEDDING_MODEL` | `bge-m3` | `bge-m3` | TASK-005 |
| retrieval-api | `EMBEDDING_DIMENSIONS` | `1024` | `1024` | TASK-005 |
| assistant-api | `OLLAMA_BASE_URL` | `http://host.docker.internal:11434` | `http://localhost:11434` | TASK-008 |
| assistant-api | `CHAT_MODEL` | `qwen3.5:9b` | `qwen3.5:9b` | TASK-008 |
| assistant-api | `RERANKER_URL` | `http://host.docker.internal:8081` | `http://localhost:8081` | TASK-009 |
| assistant-api | `RERANKER_MODEL` | `bge-reranker-v2-m3` | `bge-reranker-v2-m3` | TASK-009 |

- Sadece `deployment/docker-compose.yml`'deki iki servise eklenir. İki servise `extra_hosts: ["host.docker.internal:host-gateway"]` eklenir.
- `temperature` ve `num_ctx` ortam değişkeni değildir; TASK-008'de properties varsayılanı olur (0 ve 8192).

### D-3 `ollama.md` (repo kökü, devops)
Bölümler:
1. **Ne nerede çalışır:** üç model de Mac'te ve GPU'da. Ollama `127.0.0.1:11434` (dil modeli + embedding), llama-server `127.0.0.1:8081` (reranker). Docker'daki servisler ikisine `host.docker.internal` ile ulaşır. Kısa şema ve "neden Docker'da değil" notu.
2. **Kurulum:** `brew install ollama llama.cpp` (ikisi de kurulu; doğrulama `ollama --version`, `llama-server --version`).
3. **Modelleri indirme:** `ollama pull qwen3.5:9b`, `ollama pull bge-m3`, `ollama list`. Reranker D-1 komutunun ilk çalıştırmasında iner; önbellek yolu. "Modeller ve sürümler" tablosu: model, nerede, etiket ya da dosya, ID, disk boyutu, boyut (bge-m3 1024).
4. **Başlatma ve durdurma:**
   - Ollama: `brew services start|stop ollama`, alternatif `ollama serve`; bellekten çıkarmak için `ollama ps`, `ollama stop <model>`.
   - Reranker: D-1 komutu ayrı terminalde; Ctrl+C ile durur. Terminal kapanınca ya da bilgisayar yeniden başlayınca durur, yeniden başlatılır.
   - Tam sistemle birlikte kullanım sırası: Ollama → reranker → `docker compose … up -d`.
5. **Duman testleri:** D-S1, D-S2, D-S3 komutları ve gerçek örnek çıktıları. Reranker kapalıyken beklenen hata (D-S7).
6. **Dil modeli ayarları:** `temperature 0`, `num_ctx 8192`, `think: false` ve nedenleri.
7. **Bellek notları:** D-M1 ve D-M2 tabloları, keep-alive (5 dk, Kararlar #5; reranker açık kaldıkça bellekte), lab'ın swap notu, üç model birlikteyken yapılmaması gerekenler (ör. aynı anda başka büyük model açmak).
8. **Servislerin kullanacağı ortam değişkenleri:** D-2 tablosu.
9. **Sorun giderme:**
   - 11434/8081 bağlantı hatası (`curl: (7)`): sunucu kapalı.
   - `model "…" not found`.
   - `address already in use` (`lsof -i :11434`, `lsof -i :8081`).
   - Reranker ilk açılışta uzun süre bekliyor: model iniyor.
   - `input is too large`: ubatch.
   - Container'dan `host.docker.internal:8081`'e erişilemiyor (Kararlar #11).
   - Swap ve yavaşlama.

Belgedeki her komut devops tarafından bu makinede çalıştırılmış olmalıdır. Örnek çıktılar gerçek koşudan alınır.

### D-4 Diğer dosyalar
- `README.md`: "Lokal modeller" bölümü, `ollama.md`'ye bağlantı ve tam sistemden önce Ollama'nın ve reranker'ın açılması gerektiği notu.
- `deployment/CLAUDE.md`:
  - Ollama ve reranker satırlarında "henüz bağlı değil" yerine "Mac'te çalışıyor (TASK-003); servisler TASK-005/008/009'da bağlanır". Reranker satırına başlatma komutunun `ollama.md`'de olduğu yazılır.
  - "Gerçek yapı (TASK-003)" maddesi: ortam değişkenleri, `extra_hosts`, reranker'ın Compose'da olmadığı.

### İndirilecek modeller
- `bge-reranker-v2-m3` Q8_0 GGUF (~0,6 GB), D-1 komutuyla.
- `qwen3.5:9b` ve `bge-m3` zaten indirilmiş; sadece `ollama list` ile doğrulanır.

## Açık sorular ve riskler
1. `qwen3.5:9b`, `bge-m3` ve reranker birlikte Metal'in kullanılabilir belleğine sığmayabilir (G1). D-M1 gösterir. Sığmazsa bu task içinde çözülmez; raporda yazılır, ayrı task önerilir.
2. Kararlar #11 (`127.0.0.1` + `host.docker.internal`) D-S4 ile doğrulanır. Başarısız olursa devops `NEEDS_FIX` (sorumlu planner) yazar; `0.0.0.0` kararı kullanıcıya sorulur.
3. Ayrı task önerisi: reranker'ı arka plan servisi yapmak (launchd ya da `brew services`), böylece bilgisayar açılınca kendiliğinden başlar. Bu task'a katılmaz (Kararlar #13).
4. Kapsam dışı not: `~/.ollama/models`'te bu projede kullanılmayan iki `qwen3-vl:8b` kopyası var (`llamacpp` manifestleri, ~12 GB disk). Silinip silinmeyeceği kullanıcının kararı; bu task dokunmaz.
5. Kapsam dışı not (lead): servislerin `CLAUDE.md`'lerindeki ortam değişkeni bölümleri, bağlantı task'larında (TASK-005/008/009) ilgili coder tarafından D-2 adlarıyla güncellenmeli. Kök `CLAUDE.md`'deki "Durum" satırı bu task'tan sonra eskir (lokal modeller artık var); sahipsiz dosya, kapanışta lead günceller.
6. llama-server build 11960'ta `-hf` biçimi ya da bayrak adları beklenenden farklı olabilir. devops doğrular; komut değişirse raporda ve `ollama.md`'de gerçek komut yazılır.

STATUS: DONE
