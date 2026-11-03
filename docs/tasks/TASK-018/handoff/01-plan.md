TASK: TASK-018
# Plan

## Özet
Bugün dört ekranın hepsi gerçek veriyle çalışıyor (TASK-007…016, TASK-027). Ancak:
- Ürün hiç boş bir kurulumdan baştan sona denenmedi.
- Ekranların bütünü lab'la karşılaştırılmadı.
- Web'de mutation aracı yok. Mutation eşiği hiçbir modülde yazılı değil.
- README eski: "Durum" satırı TASK-008 öncesini anlatıyor.
- Bazı dokümanlarda lab rehberinin sayıları duruyor (`rag.md`:30 "31 sayfa, 61 parça", `.claude/skills/rag-pipeline/references/indexing.md`:15 "31 sayfa").

Kullanıcı kararları:
- **1B:** Bugünkü veriler silinir, denemeler ana `rag` projesinde yapılır, kapanışta sistem deneme verileriyle kalır.
- **2A:** Stryker eklenir.
- **3A:** Üç modülde %80 mutation eşiği.
- **4:** OWASP bağımlılık taraması yapılmaz; sadece `npm audit`.

Bu task yeni özellik eklemez. Yapılacaklar:
- **Doğrulama:** 8 E2E senaryosu tek bir sıralı Playwright dosyasında (`web/e2e/acceptance.spec.ts`) gerçek modellerle koşar.
- **Temiz kurulum:** Ana sistemin volume'ları ve proje imajları silinir, sistem README adımlarıyla yeniden kurulur, açılıştan ilk cevaba kadar geçen süre ölçülür.
- **Lab'la görsel karşılaştırma.**
- **Taramalar:** mock, log, mutation (PIT ×2, Stryker), `npm audit`.
- **Ölçümler:** performans ve bellek.
- **Dokümanlar:** README baştan yazılır, eskimiş bilgiler düzeltilir (lead).

Değişen yerler (sadece araç ayarı):
- assistant-api ve retrieval-api: `pom.xml`'de PIT eşiği.
- web: Stryker devDependency'leri, `stryker.config.mjs`, `test:mutation` script'i.

Uygulama kodu ancak bir bulgu `NEEDS_FIX` ile dönerse değişir. Sözleşme (`docs/api/**`) değişmez. Yeniden adlandırma yok.

Uygulama sırası: kart onayı → L1 (lead) ile paralel backend-coder ve frontend-coder → tester → devops → qa → L2 (lead, kapanış). Bulunan hatalar "Hata → sorumlu rol" tablosuna göre döner.

## Çalışacak roller
| Sıra | Rol | İş | Bağlı olduğu adım | Çıktı |
|------|-----|----|-------------------|-------|
| 1 | planner | Bu plan ve kart | — | `00`, `01` |
| 2 | KULLANICI | Kart onayı | 1 | `ONAY: VERİLDİ` |
| 3 | lead (L1) | Kural 8 doküman düzeltmeleri, aşağıdaki metinle ("Lead adımları") | 2 | Değişen dosyalar |
| 3a | backend-coder | İki `pom.xml`'e PIT eşiği, modül `CLAUDE.md` komut satırı | 2 | `02-backend-implementation.md` |
| 3b | frontend-coder | Stryker kurulumu, eşik, `web/CLAUDE.md` | 2 | `02-frontend-implementation.md` |
| 4 | tester | `acceptance.spec.ts` yazar ve geçici `rag-e2e` projesinde çalıştırır. Tüm birim ve entegrasyon testlerini koşar. PIT ×2 ve Stryker'ı koşar; %80'e ve kurala ait mutant'lara göre test ekler | 3a, 3b | `03-test-report.md` |
| 5 | devops | **İlk adım: ana sistemin verilerini siler.** Temiz kurulum ve 30 dk ölçümü. Acceptance spec'in son koşusu ve logların saklanması. README komut denemesi. Performans ve bellek ölçümü. README ve `deployment/CLAUDE.md`. Doküman önerileri. Sistemi açık bırakır | 4 | `04-deploy-report.md` |
| 6 | qa | Görsel karşılaştırma. Mock ve log taraması. Mutation (bağımsız). `npm audit`. L1 kontrolü. Tam E2E regresyonu (en son). Kabul kriterleri | 5 | `05-qa-report.md` |
| 7 | lead (L2) | rag.md ve KULLANIM-KILAVUZU önerilerini kullanıcıya sunar, onayla uygular. Sistem deneme verileriyle açık kalır | 6 + kullanıcı kabulü | — |

- Atlanan rol yok. Coder'lar ayrıca `NEEDS_FIX` ile çağrılır.
- **Model ve reranker durdurma:** Hiçbir kontrol çalışan Ollama'yı ya da llama-server'ı (8081) durdurmayı gerektirmez. Agent'lar bunları kapatamaz. Kapatma gerekirse kullanıcıya PID ve yeniden başlatma komutu verilir; kill'i kullanıcı kendisi çalıştırır.
- **Reranker komutunun denemesi:** devops'un 8082'de başlattığı ikinci kopyayla yapılır. Kopyayı kapatma izni engellenirse kullanıcıdan istenir.

### Hata → sorumlu rol (`NEEDS_FIX` yönlendirmesi)
| Bulgu | Sorumlu rol | Zincir yeniden nereden yürür |
|-------|-------------|------------------------------|
| E2E senaryosu uygulama hatası yüzünden kırılıyor (servis: API, akış, kayıt) | backend-coder | backend-coder → tester → devops → qa |
| E2E senaryosu ekran hatası yüzünden kırılıyor; görsel karşılaştırmada bilinçsiz fark | frontend-coder | frontend-coder → tester → devops → qa |
| E2E testinin kendisi hatalı; eksik ya da zayıf test; mutation skoru %80'in altında; kurala ait hayatta kalan mutant | tester | tester → devops → qa |
| Hayatta kalan mutant ölü kodu gösteriyor | ilgili coder | coder → tester → devops → qa |
| Ürün kodunda sabit veri ya da açılışta veri oluşturan kod | ilgili coder (Java → backend, web → frontend) | coder → … |
| `deployment/` altında veri tohumlayan dosya | devops | devops → qa |
| Servis logunda soru, cevap, parça, prompt ya da kişisel veri | backend-coder | backend-coder → … |
| nginx ya da postgres log ayarından gelen sızıntı | devops | devops → qa |
| `npm audit --omit=dev`'de high/critical bulgu, aynı ana sürümde düzeltmesi var | frontend-coder (`package.json`, `package-lock.json`) | frontend-coder → tester → devops → qa |
| README komutu çalışmıyor; temiz kurulum başarısız ya da 30 dk'yı aşıyor | devops (kod kaynaklıysa ilgili coder) | devops → qa |
| Sözleşme eksik ya da hatalı | planner | planner → … |
| L1 metninde hata; rag.md ve kılavuz önerileri | lead (kullanıcı onayıyla) | — |

**Düzeltme turunda devops'un yapacağı:**
1. Ana sistemi yeniden sıfırlar: `docker compose -f deployment/docker-compose.yml down -v --rmi local`, ardından `build --no-cache` ve `up -d`.
2. Rehberi yükler ve acceptance spec'i yeniden koşar; logları yeniden saklar.
3. README değiştiyse sadece değişen komutları yeniden dener.
4. Performans ölçümünü sadece servis kodu değiştiyse tekrarlar.

Bu sıfırlama deneme verisini siler; kullanıcı verisi zaten ilk adımda silinmiştir (karar 1B). qa düzeltme turunda görsel karşılaştırmayı sadece etkilenen ekranlar için yeniden yapar.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle / kontrolle doğrulanır |
|---|-----------------|------------------|-------------------------------------|
| 1.1 | Boş bilgi bankası; "Belge ekle" ile rehber ve yer imli başka PDF; yer imsiz PDF reddi | `acceptance.spec.ts` | A1, A2, A3, A4 |
| 1.2 | Önizleme: serbest soru, akan cevap, alıntı penceresi, "bulamadım", temsilciye aktarma | `acceptance.spec.ts` | A5, A6 |
| 1.3 | Takip sorusu ve yeniden yazılmış arama sorgusu | `acceptance.spec.ts` | A7 |
| 1.4 | Geri bildirim | `acceptance.spec.ts` | A8 |
| 1.5 | Konuşmalar: konuşma ve tur ayrıntıları | `acceptance.spec.ts` | A9 |
| 1.6 | Bilgi kaynakları: parça filtresi ve sayfalama | `acceptance.spec.ts` | A10 |
| 1.7 | Yükleme sırasında sayfa yenilenince ilerleme sürer | `acceptance.spec.ts` | A2 |
| 1.8 | Değerlendirme çalıştırması | `acceptance.spec.ts` | A11 |
| 2 | Her ekran ve pencere lab'la aynı boyutta görüntüyle karşılaştırılır; bilinçli farklar gerekçeli, bilinçsizler düzeltilir | qa, "Görsel karşılaştırma" | C5 |
| 3 | Mock taraması: sabit veri yok, açılışta veri üreten kod yok, test dublörleri sadece testte | qa, "Mock taraması" | C6 |
| 4a | PIT iki serviste, eşik modül `CLAUDE.md`'de (%80) | backend-coder (eşik), tester (test), qa (bağımsız koşu) | C7, C8 |
| 4b | Stryker web'de (%80) | frontend-coder (kurulum), tester, qa | C9 |
| 4c | `npm audit` | qa | C10 |
| 4d | OWASP bağımlılık taraması | **Yapılmaz** (kullanıcı kararı 4); kart "Yapılmayacaklar" | — |
| 5 | 16 GB'ta uçtan uca ve adım süreleri, bellek; README'de "bu makinede ölçülen" | devops, "Performans ve bellek ölçümü" | C3, C4 |
| 6 | Senaryo koşusunun logları: soru, cevap, parça, prompt, kişisel veri yok | devops logları saklar, qa tarar ("Log denetimi") | C12 |
| 7a | README (kurulum, çalıştırma, ekranlar, mimari, ölçümler) devops tarafından | devops, "README içeriği" | C2, C13 |
| 7b | `rag.md` ve `KULLANIM-KILAVUZU.md` için öneriler raporda; lead kullanıcı onayıyla günceller | devops ve qa raporlarında "Doküman önerileri"; L2 | L2 (kullanıcı) |
| 8 | Lab sayılarının kaldırılması; kök `CLAUDE.md` "Durum" ve klasör tablosu; açılışta yükleme anlatan yer kalmaz; `grep "31 sayfa"` boş | L1 (lead, aşağıdaki metin) | C14 (qa) |
| S1 | README'yi izleyen kullanıcı 30 dk içinde (model indirme hariç) ilk soruyu cevaplatır | devops temiz kurulum | C1 |
| S2 | Görsel farkı frontend-coder bu task içinde düzeltir | `NEEDS_FIX` → frontend-coder | C5 |
| AC1 | Bütün E2E senaryoları gerçek modellerle geçmiş; sonuçlar raporda | tester (geliştirme), devops (temiz kurulumda son koşu), qa (regresyon) | A1-A11, C15 |
| AC2 | Görsel karşılaştırma tablosu raporda; bilinçsiz fark yok | qa | C5 |
| AC3 | Mutation, güvenlik (`npm audit`) ve log denetimi raporda | tester, qa | C7-C10, C12 |
| AC4 | README güncel, her komut çalışıyor | devops | C2 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Acceptance spec dolu bir bilgi bankasında çalıştırılır (ör. tam regresyon koşusunda) | `E2E_ACCEPTANCE` yoksa bütün dosya atlanır (skip, gerekçeli). Varsa ve kaynak listesi boş değilse `beforeAll` "acceptance needs an empty knowledge base" mesajıyla başarısız olur | A0 |
| G2 | README komut denemesinde `up -d assistant-api` servisi yeniden oluşturur, senaryo koşusunun logları kaybolur | devops, acceptance koşusunun hemen ardından ve başka bir komuttan önce logları dosyaya yazar | C12 (log dosyası var mı) |
| G3 | Ana sistem (`rag`), tester'ın geçici projesi (`rag-e2e`) ve dev postgres (`rag-dev`) aynı portları ister | tester başlamadan ana sistemi `down` (`-v` olmadan) ve dev postgres'i `stop` eder. İşi bitince `rag-e2e`'yi `down -v` ile siler. Ana sistemin verisini sadece devops siler | A0, C1 |
| G4 | Rehber, sayfa yenilenmeden önce işlenip biter (yarış) | A2, yenilemeden sonra satırın "Hazır" olmadığını doğrular; olmazsa açık bir mesajla başarısız olur. Bu sessiz geçiş değil, görünen başarısızlıktır | A2, kart riski |
| G5 | Tam E2E regresyonu her koşuda "E2E Rehber …" kaynağı bırakır; görsel karşılaştırma ve ölçümler bozulur | qa görsel karşılaştırmayı ve taramaları önce yapar, tam regresyonu en son çalıştırır. devops ölçümleri acceptance koşusundan sonraki veriyle yapar | C5, C15 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| K1 | Denemeler ana `rag` projesinde yapılır (karar 1B). Veri silme devops'un ilk adımıdır: `docker compose -f deployment/docker-compose.yml down -v --rmi local`. Tester bundan önce ayrı bir `-p rag-e2e` projesi kullanır | Kullanıcı kararı. Lead'in talimatı: silme devops'un başında. tester'ın boş sisteme ihtiyacı, kullanıcı verisini erkenden silmeden geçici projeyle karşılanır | tester'ın ana veriyi silmesi: silme adımı iki role dağılırdı |
| K2 | "Temiz makine" yaklaşımı: aynı çalışma ağacında volume'lar ve proje imajları silinir (`down -v --rmi local`), imajlar `build --no-cache` ile derlenir, ardından README adımları izlenir. Taban imajlar (maven, temurin, node, nginx, pgvector) ve modeller önbellekte kalır; raporda yazılır | Docker build'i `.dockerignore` sayesinde `target/` ve `node_modules/`'u kullanmaz. Sistem kapanışta repodaki compose dosyasından çalışır durumda kalır | Scratchpad'e kopya: kapanışta sistem geçici bir klasörden çalışmış olurdu. `git clone`: commit edilmemiş düzeltmeleri içermez |
| K3 | 8 senaryo tek dosyada (`web/e2e/acceptance.spec.ts`), `test.describe.configure({ mode: 'serial' })`. `E2E_ACCEPTANCE=1` ile çalışır, yoksa atlanır | Senaryolar birbirinin verisine dayanır (boş → yükle → sor → Konuşmalar → ölç). Bayrak, dolu sistemde regresyon koşusunu bozmaz (G1) | Senaryo başına dosya: sıra Playwright'ın dosya sırasına kalırdı. Bayraksız: tam regresyon her seferinde kırılırdı |
| K4 | Yer imli ikinci PDF test sırasında üretilir: Chromium `page.pdf({ outline: true, tagged: true })` ile `kaynaklar/rag.html`'den (`file://`) `testInfo.outputPath('rag-anlatim.pdf')`'e. Yüklemede ad `RAG anlatımı`, sürüm `1`. Yer imsiz PDF: mevcut `web/e2e/fixtures/yer-imi-yok.pdf` | TASK-027 kararı (RAG anlatım sayfası). Repoya yeni ikili dosya girmez | `docs/rehber/pdf/…2026.11.pdf`: rehberin ikinci sürümü, cevaplarda iki rehberden alıntı karışır (TASK-027 qa FYI-3). Repoya PDF fixture: yeni ikili dosya |
| K5 | Görsel karşılaştırma boyutu: lab `file://…/kaynaklar/rag-lab.html` 1480×900'de açılır, `.sbody`'nin `boundingBox()` ölçülür. Web aynı genişlik × yükseklikte viewport'la açılır. Lab'da `.sbody` görüntüsü, web'de tam sayfa görüntüsü alınır | TASK "aynı pencere boyutunda". Lab'ın ürün alanı container query ile kendi genişliğine göre dizilir; aynı genişlik iki tarafı aynı kırılım noktasında karşılaştırır. 1480×900 TASK-002 karşılaştırmasıyla aynı | Web'i 1480×900 açmak: web tam genişlikte, lab ~740 px'te dizilir. Her düzen farkı "bilinçli" sayılmak zorunda kalırdı |
| K6 | Bilinçli fark listesi önceden sabit: gerçek veriden gelen sayı ve metin; model cevabı; lab'da olmayan boş ve "sürüyor" durumları; lab'ın sahte tarayıcı çerçevesi; kullanıcı alanı yok (TASK-002); "Yeni sürüm yükle" yok (TASK-015 iptal). Başka her fark `NEEDS_FIX` frontend-coder | TASK kural 2; qa'nın keyfi "bilinçli" demesini engeller | Fark başına lead'e sormak: zinciri durdururdu |
| K7 | Tipik soru ölçümü: 5 golden soru + 1 takip sorusu, her biri 3 kez yeni konuşmada; ortanca ve en kötü. Ayrıntı "Performans ve bellek ölçümü"nde | Tek ölçüm gürültülü; `done` olayı adım sürelerini zaten taşır | Tarayıcıdan elle ölçüm: tekrarlanamaz |
| K8 | `npm audit` politikası: `npm audit --omit=dev`'de high/critical ve aynı ana sürümde düzeltmesi var → `NEEDS_FIX` frontend-coder (`npm audit fix`, `--force` yok). Ana sürüm atlaması gerekiyorsa ya da bulgu sadece dev aracındaysa (tam `npm audit`) raporda gerekçeyle kalır. OWASP yok (karar 4) | Kart "Benim verdiğim kararlar" | Her bulguyu düzeltmek: ana sürüm atlamaları yeni özellik kadar risklidir |
| K9 | Mutation eşiği (karar 3A): PIT `<mutationThreshold>80</mutationThreshold>`; Stryker `thresholds: { high: 90, low: 80, break: 80 }` | Eşik araç ayarında ve `CLAUDE.md`'de tek yerde; araç kendisi "başarısız" der | Sadece rapora yazmak: TASK "eşikler modül CLAUDE.md'lerindeki gibi" diyor |
| K10 | Log denetimi: dört servisin logu ve host model logları. Kalıplar ve izin listesi "Log denetimi"nde. nginx erişim logundaki `?query=` (parça filtresi) FYI'dır | Kural 6; kart kararı | Sadece Türkçe karakter aramak: İngilizce prompt parçalarını (`query-rewrite.st`) kaçırırdı |
| K11 | Kural 8 metinleri bu planda sabit ("Lead adımları"). Lead onaydan hemen sonra uygular (L1), qa doğrular. `grep "31 sayfa"` kapsamı `kaynaklar/` (lab, değiştirilmez) ve `docs/tasks/` (geçmiş kayıt) hariçtir | Kart onayı metnin onayıdır; qa'dan önce uygulanınca doğrulanabilir | Kapanışta uygulamak: qa doğrulayamazdı |
| K12 | README'de ekran görüntüsü yok; ekranlar metinle anlatılır | Repoya ikili dosya girmez, görüntüler eskir | PNG'ler: her ekran değişikliğinde güncellenmesi gerekirdi |

## Sözleşme değişiklikleri
Yok.

## Backend planı
Ek skill: `springboot-verification`.
- İki serviste `pitest-maven` `<configuration>`'a `<mutationThreshold>80</mutationThreshold>` eklenir.
- `assistant-api/CLAUDE.md` ve `retrieval-api/CLAUDE.md`'de:
  - "Komutlar" bölümündeki mutation satırının sonuna şu eklenir: `eşik %80 (mutationThreshold); skor altındaysa komut BUILD FAILURE verir`.
  - "Teknoloji" satırındaki PIT tanımına "eşik %80" eklenir.
- Uygulama kodu değişmez. `./mvnw verify` iki serviste geçmelidir (eşik `verify`'a bağlı değildir).
- `NEEDS_FIX` gelirse maddeyi uygular, `02-backend-implementation.md`'ye `## Düzeltme turu N` ekler.

## Frontend planı
Ek skill: `writing-good-tests` (Stryker ayarı için).
- **devDependencies:** `@stryker-mutator/core`, `@stryker-mutator/vitest-runner`. İkisi aynı ana sürümde ve en güncel olur. Vitest 5 desteği kurulumda doğrulanır; desteklemiyorsa frontend-coder durur ve planner'a ve lead'e yazar.
- **`web/stryker.config.mjs`** (config dosyası olduğu için default export serbest):
  - `testRunner: 'vitest'`, `plugins: ['@stryker-mutator/vitest-runner']`, `vitest: { configFile: 'vite.config.ts' }`
  - `coverageAnalysis: 'perTest'`
  - `mutate`: `src/**/*.{ts,tsx}`; hariç: `src/**/*.test.{ts,tsx}`, `src/test/**`, `src/main.tsx`, `src/**/*.d.ts`
  - `reporters: ['html', 'clear-text', 'progress']`, `htmlReporter: { fileName: 'reports/mutation/index.html' }`
  - `thresholds`: K9
- **Script:** `package.json`'a `"test:mutation": "stryker run"`.
- **`web/.gitignore`:** `reports/`, `.stryker-tmp/`.
- **`web/CLAUDE.md`:**
  - "Teknoloji": "Henüz olmayanlar"dan Stryker çıkarılır, kurulu test paketlerine eklenir.
  - "Komutlar": `npm run test:mutation` → rapor `reports/mutation/index.html`, eşik %80 (`break`).
- **Doğrulama:** `npm run build`, `npm run lint`, `npm test` geçmelidir. Stryker bir kez koşulur, skor `02-frontend-implementation.md`'ye yazılır. Test eklenmez; testler tester'ındır.
- **`NEEDS_FIX` gelirse** (görsel fark, ekran hatası, `npm audit` runtime bulgusu): düzeltir, `## Düzeltme turu N` ekler. Görsel düzeltme lab'ın `--s-*` token'larıyla yapılır (`frontend-conventions` bölüm 12).

## Test senaryoları

### Acceptance E2E (tester yazar: `web/e2e/acceptance.spec.ts`)
Ön koşul: boş açılmış sistem; Ollama (`qwen3.5:9b`, `bge-m3`) ve reranker (8081) açık.

Koşu komutu: `cd web && E2E_ACCEPTANCE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test e2e/acceptance.spec.ts`.

Süreler:
- dosya düzeyinde `test.setTimeout(20 * 60_000)`
- `ANSWER_TIMEOUT_MS = 120_000`
- `INGESTION_TIMEOUT_MS = 240_000`
- `EVAL_TIMEOUT_MS = 15 * 60_000`
- `waitForTimeout` yok

Seçiciler rol ve Türkçe metinle yazılır. Beklenen metinler koddaki sabitlerden alınmaz, test içinde literal yazılır. Dosyanın başındaki sabitler:
- `LOST_CARD_QUESTION = 'Kartımı kaybettim, ne yapmalıyım?'`
- `FOLLOW_UP_QUESTION = 'Yenisi kaç günde gelir?'`
- `OFF_TOPIC_QUESTION = 'Bitcoin alıp satabilir miyim?'`
- `NOT_FOUND_SENTENCE = 'Bu konuda rehberimizde bilgi bulamadım.'`

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| A0 | Ön koşul (G1) | e2e `beforeAll` | — | `E2E_ACCEPTANCE !== '1'` → `test.skip`. `GET /api/workspaces/42/sources` `content` boş değilse hata: `acceptance needs an empty knowledge base` |
| A1 | Boş açılış (1.1) | e2e | `givenEmptySystem_whenScreensOpened_thenEmptyStatesShown` | `/kaynaklar`'da "Henüz belge yok", `/konusmalar`'da "Henüz konuşma yok" görünür. `/degerlendirme`'de "Golden set yüklenmemiş." görünür, ölçüm düğmesi `disabled` |
| A2 | Rehber "Belge ekle" ile; yükleme sırasında yenileme (1.1, 1.7) | e2e | `givenGuidePdf_whenUploadedViaDialogAndPageReloaded_thenProgressContinuesAndSourceReady` | Pencerede dosya `docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf`, ad `Raşit Bankası Ürün ve Hizmet Rehberi`, sürüm `2026.10` → "Yükle ve işle". Pencere kapanır kapanmaz `page.reload()`. Yenilemeden sonra satır görünür ve "Hazır" değildir; içe alma ilerlemesi gösterir (G4). `INGESTION_TIMEOUT_MS` içinde "Hazır" olur, Parça sütunu `154` |
| A3 | Yer imli ikinci PDF (1.1) | e2e | `givenBookmarkedPdf_whenUploadedViaDialog_thenSourceReady` | K4'teki PDF, ad `RAG anlatımı`, sürüm `1` → "Hazır", parça > 0. Listede 2 satır |
| A4 | Yer imsiz PDF reddi (1.1) | e2e | `givenPdfWithoutBookmarks_whenUploaded_thenRejectedAndNoNewRow` | `fixtures/yer-imi-yok.pdf`, ad `Yer imsiz deneme`, sürüm `1`. Pencerede `PDF_NO_BOOKMARKS` için gösterilen düzeltme mesajı görünür ve pencere açık kalır. Metin `shared/api/errorMessages.ts` ya da `source/model/sourceUploadRules.ts`'ten hangisi gösteriyorsa odur; testte literal yazılır. Vazgeç sonrası listede yine 2 satır |
| A5 | Serbest soru, akan cevap, alıntı penceresi (1.2) | e2e | `givenGuideReady_whenQuestionAsked_thenAnswerStreamsAndCitationOpensChunkDialog` | `/onizleme`'de `LOST_CARD_QUESTION` gönderilir. **Akış kanıtı:** "Soru" kutusu hâlâ `disabled` iken cevap balonunda boş olmayan metin görünür. Akış biter ("Son cevapta modele giden parçalar" başlığı). Cevapta en az bir `[n]` alıntı düğmesi var; tıklanınca parça penceresi açılır ve başlıkta "s. " ile sayfa görünür. Esc ile kapanır |
| A6 | "Bulamadım" ve temsilciye aktarma (1.2) | e2e | `givenOffTopicQuestion_whenAsked_thenNotFoundSentenceAndHandoffQueued` | Yeni konuşmada `OFF_TOPIC_QUESTION` → cevap `NOT_FOUND_SENTENCE` içerir, alıntı düğmesi yok. "Temsilciye bağlan" → "Temsilci kuyruğundasın" |
| A7 | Takip sorusu, yeniden yazılmış arama sorgusu (1.3) | e2e | `givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsRewrittenSearchQuery` | Yeni konuşmada önce `LOST_CARD_QUESTION`, sonra `FOLLOW_UP_QUESTION`. Yan kartta arama sorgusu satırı görünür ve metni `FOLLOW_UP_QUESTION`'dan farklıdır. Yeniden yazma süre satırı görünür (mevcut `preview.spec.ts` kalıbı) |
| A8 | Geri bildirim (1.4) | e2e | `givenAnsweredTurn_whenYesClicked_thenFeedbackSavedAndPressed` | A7'deki son cevapta "Faydalı mı?" → "Evet": `aria-pressed="true"`, toast "Geri bildirim kaydedildi" |
| A9 | Konuşmalar (1.5) | e2e | `givenThreeConversations_whenListAndDetailOpened_thenTurnsStatusChunksAndHandoffShown` | `/konusmalar`'da 3 satır (A5, A6, A7), en üstte A7'nin ilk sorusu; menü sayacı `3`. A6 detayında durum çipi "BİLMİYOR" ve aktarma satırı görünür. A5 detayında tur kartında soru metni, parça tablosunda 5 satır |
| A10 | Parça filtresi ve sayfalama (1.6) | e2e | `givenGuideReady_whenChunksFilteredAndPaged_thenMatchingRowsAndNextPage` | Rehber detayında filtreye `asgari ödeme` yazılır: en az 1 satır gelir ve her satır (başlık + içerik) tr küçük harfle `asgari ödeme` içerir. Filtre temizlenip sonraki sayfaya geçilir: sayfa göstergesi 2. sayfayı gösterir, ilk satırın parça numarası 1. sayfadakinden farklıdır |
| A11 | Değerlendirme çalıştırması (1.8) | e2e | `givenGoldenSetLoaded_whenEvalRunStarted_thenProgressThenKpisAndRunRow` | `request.put('/api/assistant/golden-set', { data: docs/rehber/golden/rehber-golden-v1.json })` → 200. `/degerlendirme`'de düğme `/^Ölçümü çalıştır · (rerank 20→5\|top-5)$/` tıklanır → `/^Ölçülüyor · \d+\/100$/` görünür → `EVAL_TIMEOUT_MS` içinde recall@5 kutusunda `0,dd` ve `\d+/100 soru` görünür. "Çalıştırmalar" tablosunda `ev_` ile başlayan 1 satır |

**Tester'ın çalışma şekli (K1, G3):**
1. Başlamadan önce:
   - `docker compose -f deployment/docker-compose.yml down` (`-v` yok, ana veriye dokunulmaz)
   - `docker compose -f deployment/docker-compose.dev.yml stop`
2. Her deneme `docker compose -p rag-e2e -f deployment/docker-compose.yml up --build -d` ile açılır. Yeniden denemeden önce `down -v` (sadece `rag-e2e`).
3. Bitince `rag-e2e` `down -v` ile silinir; ana sistem kapalı bırakılır, devops açar.
4. A1-A11 en az bir kez baştan sona geçmelidir. Mevcut `web/e2e/*.spec.ts` dosyaları değişmez.

### Kontroller (rol ve komut)
| # | Kontrol | Rol | Komut / yöntem | Beklenen sonuç |
|---|---------|-----|----------------|----------------|
| C0 | Tüm birim ve entegrasyon testleri | tester (qa tekrarlar) | `cd assistant-api && ./mvnw verify`, `cd retrieval-api && ./mvnw verify`, `cd web && npm run build && npm run lint && npm test && npm run test:coverage` | Hepsi geçer; test sayıları ve coverage raporda |
| C1 | Temiz kurulum süresi (S1) | devops | "Deployment planı" adım 1-4. Saat, README "Kurulum"un ilk komutunda başlar ve ilk cevabın `done` olayında durur (Önizleme'de, rehber ekrandan yüklenmiş) | ≤ 30 dk (model indirme hariç; taban imaj durumu yazılı). Aşarsa `NEEDS_FIX` devops |
| C2 | README ve `ollama.md` komutları (AC4) | devops | Her komut çalıştırılır. Reranker komutu `--port 8082` ile ikinci kopyada denenir. `tmux new -d -s rag-check && tmux kill-session -t rag-check`, `claude --version` | Rapor tablosu: komut → çıktı özeti → ✅/❌. ❌ kalmaz |
| C3 | Adım süreleri (kural 5) | devops | "Performans ve bellek ölçümü" | Tablo README'de ve raporda |
| C4 | Bellek (kural 5) | devops | Aynı bölüm | Tepe ve boşta değerler, swap |
| C5 | Görsel karşılaştırma (kural 2, S2, AC2) | qa | "Görsel karşılaştırma" | Tablo; bilinçsiz fark 0 |
| C6 | Mock taraması (kural 3) | qa | "Mock taraması" | İzin listesi dışında isabet 0 |
| C7 | PIT assistant-api (4a) | tester (qa tekrarlar) | `cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS (skor ≥ %80); kurala ait hayatta kalan mutant yok |
| C8 | PIT retrieval-api (4a) | tester (qa tekrarlar) | `cd retrieval-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | Aynı |
| C9 | Stryker web (4b) | tester (qa tekrarlar) | `cd web && npm run test:mutation` | Skor ≥ %80, komut 0 döner |
| C10 | `npm audit` (4c) | qa | `cd web && npm audit --omit=dev` ve `npm audit` | Runtime'da K8'e göre düzeltilmesi gereken bulgu kalmaz; diğerleri listede |
| C12 | Log denetimi (kural 6) | devops toplar, qa tarar | "Log denetimi" | İhlal 0; FYI'lar listede |
| C13 | README içeriği (7a) | qa | "README içeriği" bölümündeki başlıklar var mı, ölçüm tablosu C3/C4 ile aynı mı | Eksik yok |
| C14 | L1 doğrulaması (kural 8) | qa | `grep -rn --exclude-dir={kaynaklar,tasks,node_modules,target,.git} -e "31 sayfa" -e "61 parça" .` ve "Lead adımları"ndaki metinler yerinde mi | grep boş; metinler birebir |
| C15 | Tam E2E regresyonu (AC1) | qa, en son | `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e` (`E2E_ACCEPTANCE` yok → acceptance dosyası atlanır) | Hepsi geçer ya da sebebi gerekçeli skip |

(C11 OWASP, karar 4 ile kaldırıldı.)

## Görsel karşılaştırma (qa)
- **Araç:** Playwright (`webapp-testing`).
  - Lab: `file://<repo>/kaynaklar/rag-lab.html`, viewport 1480×900. Ekranlar lab'ın kendi yan menüsü ve düğmeleriyle açılır; `.sbody` öğesinin görüntüsü alınır.
  - Web: `http://localhost:3000`, viewport = lab `.sbody` kutusunun genişliği × yüksekliği (K5); tam sayfa görüntüsü alınır.
- **Dosyalar:** `<scratchpad>/gorsel/<no>-lab.png`, `<no>-web.png`. Tam yollar rapora yazılır.
- **Karşılaştırılacak ekran ve pencereler** (lab'da karşılığı olan her ekran, durum ve pencere):

  | No | Ekran / pencere | Web'de nasıl açılır |
  |----|-----------------|---------------------|
  | V1 | Önizleme, boş (karşılama, çipler, canlı yapılandırma şeridi, "Asistan nasıl cevaplıyor?") | `/onizleme`. Çipler için `<scratchpad>`'te küçük bir deneme öneri listesi `PUT` edilir (repoya girmez) |
  | V2 | Önizleme, alıntılı cevap (yan kartta parçalar ve süre çubukları, "Faydalı mı?") | `LOST_CARD_QUESTION` |
  | V3 | Önizleme, "bulamadım" + "Temsilciye bağlan" → toast + "Temsilci kuyruğundasın" | `OFF_TOPIC_QUESTION` |
  | V4 | Parça / alıntı penceresi | V2'de `[1]` |
  | V5 | "Kayıtlı sorular" penceresi | V1'de liste düğmesi |
  | V6 | Konuşmalar listesi | `/konusmalar` |
  | V7 | Konuşma detayı (tur kartları, durum çipi, parça tablosu, aktarma satırı) | V3'ün ve V2'nin konuşması |
  | V8 | Bilgi kaynakları listesi ve bilgi kutusu | `/kaynaklar` |
  | V9 | "Belge ekle" penceresi (boş ve hata mesajlı) | "Belge ekle" |
  | V10 | Kaynak detayı (meta kutuları, işler tablosu, parçalar, filtre, sayfalama) | rehberin detay sayfası |
  | V11 | Değerlendirme (ölçüm sonrası: kutular, "Çalıştırmalar", "Kaçanlar ve sırası değişenler") | `/degerlendirme` |

  Lab'da olmayan durumlar (boş durumlar, "Ölçülüyor", içe alma sürüyor) web'de ayrıca görüntülenir ve tabloya "lab'da yok, bilinçli" diye yazılır.
- **Rapor tablosu** (`05-qa-report.md`): `No | Ekran | Lab görüntüsü | Web görüntüsü | Fark | Bilinçli mi (K6 maddesi) / NEEDS_FIX`.
  - Karşılaştırılan nitelikler: bileşen sırası ve hizası, metinler (birebir), renkler, yazı tipi ve boyutu, boşluklar, kenarlık ve köşe, ikonlar, etkileşim (hover, odak, açılan pencere).
- Deneme öneri listesi iş bitince `{"categories":[]}` ile temizlenir.

## Mock taraması (qa)
Kapsam: `assistant-api/src/main`, `retrieval-api/src/main`, `web/src` (test dosyaları ve `src/test/**` hariç), `web/public`, `deployment/`. Her isabet tabloya yazılır: `dosya:satır | ne | izin listesi maddesi / NEEDS_FIX rol`.

| # | Ne aranır | Yöntem |
|---|-----------|--------|
| M1 | Sabit soru | Java ve TS string literal'lerinde `?` ile biten Türkçe metin. `rg -n '"[^"]*\?"' */src/main/java`, `rg -n "'[^']*\?'" web/src -g '!*.test.*' -g '!src/test/**'` |
| M2 | Java kodunda Türkçe içerik (cevap, parça, konuşma) | `rg -n '"[^"]*[çğıöşüÇĞİÖŞÜ][^"]*"' */src/main/java` → beklenen 0 |
| M3 | Kodda model adı | `rg -n 'qwen\|bge-\|reranker-v2\|llama' */src/main/java web/src -g '!*.test.*' -g '!src/test/**'` |
| M4 | Kullanıcı / kişi | `rg -n 'Eşdemir\|@[a-z]+\.(com\|net)' */src/main web/src -g '!*.test.*'` |
| M5 | Metrik / sayı sabiti | `web/src/**/model/*.ts` ve `*/src/main/java/**` içindeki üst düzey sabit dizi ve nesneler elle okunur. Sayısal metrik (`recallAtK`, `mrr`, süre) ya da veri listesi içeren her sabit sınıflandırılır |
| M6 | Açılışta veri üreten kod | `rg -n 'ApplicationReadyEvent\|CommandLineRunner\|ApplicationRunner\|@PostConstruct\|ContextRefreshedEvent\|SmartLifecycle\|@Scheduled' */src/main/java` → her isabetin gövdesi okunur |
| M7 | Migration tohum verisi | `rg -in 'insert into' */src/main/resources/db/migration` |
| M8 | Üründe test dublörü | `rg -ln "from 'msw'\|vi\.mock\|vi\.fn" web/src -g '!*.test.*' -g '!src/test/**'` ve `rg -ln 'mockito\|MockitoBean' */src/main` → ikisi de 0 |
| M9 | Deployment tohumu | `deployment/postgres/initdb/*` sadece veritabanı, kullanıcı ve eklenti oluşturur; `web/public`'te sadece favicon var |

**İzin listesi** (sahte veri değildir):
- `application.properties` varsayılanları: model adları, `not-found-answer`, adresler. Ortam değişkeniyle değişir (`backend-conventions` bölüm 7).
- `retrieval-api` V2 migration'ındaki çalışma alanı satırı (42, ad, `bge-m3`, 1024; `rag.md`).
- Lab'dan birebir alınan arayüz metinleri: karşılama mesajı, açıklamalar, placeholder, "Asistan nasıl cevaplıyor?" şablon metni, boş durum metinleri.
- `RECALL_PASS_THRESHOLD = 0.95` ve diğer `*Rules.ts` iş kuralı sabitleri.
- Prompt şablonları (`src/main/resources/prompts/*.st`).
- M6'da beklenen üç başlangıç kodu:
  - `IngestionStartupListener`: sadece yarıda kalan işleri `FAILED` yapar.
  - `EvalRunStartupListener`: aynısını ölçümler için yapar.
  - `RerankerHealthChecker`: sadece sağlık ölçer.

Bunlar dışında veri oluşturan her başlangıç kodu `NEEDS_FIX`'tir.

## Log denetimi
**devops — toplama:**
- Acceptance koşusu biter bitmez, başka hiçbir komuttan önce (G2): `docker compose -f deployment/docker-compose.yml logs --no-color --timestamps > <scratchpad>/loglar/acceptance-compose.log`.
- Ayrıca servis başına birer dosya: `assistant-api`, `retrieval-api`, `web`, `postgres`.
- Host logları:
  - `~/.ollama/logs/server.log`'un koşu saatlerine düşen kısmı.
  - Reranker log dosyası, varsa. Yeri kullanıcının reranker'ı başlattığı komuta göre değişir (ör. `/tmp/llama-reranker.log`).
- Dosya yolları rapora yazılır.

**qa — tarama:** devops'un dosyaları ve qa'nın kendi E2E koşusunun logları taranır. Her kalıp için isabet sayısı ve örnek satır (içerik kısaltılarak) tabloya yazılır.

| # | Kalıp | Ne yakalar |
|---|-------|------------|
| L1 | Acceptance soru metinleri (`Kartımı kaybettim`, `Yenisi kaç günde`, `Bitcoin`), `rehberimizde bilgi bulamadım` | Soru ve cevap |
| L2 | `docs/rehber/golden/rehber-golden-v1.json`'daki ilk 20 `answerText`'in ilk 30 karakteri (`jq`) | Parça metni |
| L3 | `prompts/rag-system.st`, `rag-user.st`, `query-rewrite.st` ve `llm-only-system.st`'in ilk satırları; `<parcalar>` ve şablondaki diğer etiketler | Prompt |
| L4 | `[çğıöşüÇĞİÖŞÜ]` (sadece `assistant-api` ve `retrieval-api` logları) | Her türlü Türkçe içerik; isabet tek tek açıklanır |
| L5 | E-posta `[\w.+-]+@[\w-]+\.[\w.]+`, TCKN `\b[1-9][0-9]{10}\b`, telefon `\b0?5\d{2}[ -]?\d{3}[ -]?\d{2}[ -]?\d{2}\b`, IBAN `\bTR\d{2}[ 0-9]{10,}` | Kişisel veri |
| L6 | `Authorization` | Başlık |

**Değerlendirme:**
- Servis loglarında L1-L3, L5 ya da L6 isabeti `Required`'dır → `NEEDS_FIX` backend-coder. nginx ya da postgres kaynaklıysa sorumlu devops'tur.
- nginx erişim logundaki `?query=` (parça filtresi) FYI'dır (K10).
- Host model logları ürün dışıdır; isabet varsa FYI olarak yazılır.

## Performans ve bellek ölçümü (devops)
- **Ortam:** Temiz kurulum, rehber 2026.10 yüklü, varsayılan ayarlar (`RETRIEVAL_STRATEGY=rerank`, yeniden yazma açık).
  - Makine bilgisi: `sysctl -n machdep.cpu.brand_string hw.memsize`, `sw_vers -productVersion`, `ollama --version`.
  - Makine 16 GB değilse gerçek değer yazılır.
- **Sorular:**
  - `rehber-golden-v1.json`'daki şu kategorilerin her birinden ilk soru: Kartlar, Hesaplar, Krediler, Para transferi ve ödemeler, Dijital bankacılık.
  - Takip çifti: `LOST_CARD_QUESTION` → `FOLLOW_UP_QUESTION`.
  - Önce bir ısınma sorusu sorulur ve ayrıca raporlanır.
  - Sonra her soru 3 kez, her seferinde yeni konuşmada sorulur. Takip sorusu da 3 kez, her seferinde `LOST_CARD_QUESTION`'dan sonra sorulur.
- **Yöntem:**
  - İstek, `assistant-api/CLAUDE.md` duman testindeki istektir; `curl -sN -w '%{time_total}'` ile atılır, `done` olayının JSON'u saklanır.
  - Arama alt adımları retrieval-api logundaki `Search completed … embeddingMs={} sqlMs={}` satırından alınır.
- **Tablo** (ortanca / en kötü, ms):
  - yeniden yazma (`queryRewriteLatencyMs`, sadece takip sorusunda)
  - arama (`retrievalLatencyMs`; içinde embedding ve SQL)
  - rerank (`rerankLatencyMs`)
  - ilk token (`timeToFirstTokenMs`)
  - cevap üretimi (`generationLatencyMs`)
  - sunucu toplamı (`latencyMs`)
  - istemci toplamı (`time_total`)
  - `promptTokens` ve `completionTokens` ortancası
  - ek olarak: rehberin içe alma süresi (iş kaydından) ve A11 ölçümünün süresi (`durationMs`)
- **Bellek:**
  - Ölçüm döngüsü boyunca 2 sn'de bir örneklenir: `docker stats --no-stream --format '{{.Name}} {{.MemUsage}}'`, `ps -axo rss,comm | grep -E 'ollama|llama-server'`, `sysctl vm.swapusage`.
  - Ayrıca bir kez `ollama ps` ve `memory_pressure | tail -1` çalıştırılır.
  - Boşta ve tepe değerler servis ve süreç başına yazılır; en yüksek swap da yazılır.
- **README:** "Ölçümler" bölümünün başlığında "bu makinede ölçülen: <çip>, <RAM>, <macOS>, <tarih>" yazar. Altında tablo ve bir cümlelik yorum olur; yorum sadece ölçüleni söyler, öneri vermez.

## README içeriği (devops yazar)
Mevcut "Agent ekibi" ve "tmux" bölümleri korunur. Eskimiş "Durum" satırı kaldırılır. Bölümler sırasıyla:
1. **Proje** (2-3 cümle).
2. **Gereksinimler:** Docker Desktop, Homebrew `ollama` ve `llama.cpp`; geliştirme için Java 26 ve Node 26.
3. **Kurulum:** modeller (`ollama.md` bağlantısı + özet komutlar), reranker, Compose.
4. **İlk kullanım:** rehberi Bilgi kaynakları → "Belge ekle" ile yükle; golden set'i yükle (komut); önerileri yükle (komut + biçim); ilk soru.
5. **Ekranlar:** dört ekran, her biri 2-3 cümle; ekran görüntüsü yok (K12).
6. **Mimari:** servis ve port tablosu, iki veritabanı, cevap akışının adımları (yeniden yazma → arama → rerank → prompt → cevap → alıntı), modeller.
7. **Ayarlar:** ortam değişkenleri (`RETRIEVAL_STRATEGY`, `GENERATION_MODE`, `QUERY_REWRITE_ENABLED`, `ASSISTANT_HOST_LABEL`, model adları).
8. **Ölçümler** (bu makinede ölçülen).
9. **Testler:** birim, E2E, acceptance (`E2E_ACCEPTANCE=1`), mutation ve `npm audit` komutları.
10. **Sorun giderme:** port çakışması, veriyi sıfırlama, model kapalıyken görülen belirtiler.
11. **Agent ekibi** ve **tmux**.

Ek işler:
- `deployment/CLAUDE.md`'ye "Gerçek yapı (TASK-018)" bölümü eklenir: temiz kurulum komutu (`down -v --rmi local` + `build --no-cache`), acceptance komutu, log toplama, tester'ın geçici `rag-e2e` projesi.
- `ollama.md` sadece denemede yanlış çıkan bir komut varsa değişir.
- **Doküman önerileri** (`04-deploy-report.md`, ayrı bölüm): `rag.md` ve `KULLANIM-KILAVUZU.md` için önerilen değişikliklerin birebir metni. Örnekler: `rag.md` "Genel kurallar"daki boş yorum, "Durum: taslak" satırı, kılavuzdaki eskimiş komutlar. Lead bunları L2'de kullanıcıya sunar.

## Deployment planı
- Yeni servis, port, ortam değişkeni ya da model yok.
- **devops temiz kurulum sırası (karar 1B):**
  1. **Kullanıcı verisinin silinmesi (devops'un ilk adımı):** `docker compose -f deployment/docker-compose.yml down -v --rmi local`. Kullanıcı kart kararıyla onayladı. `rag-e2e` kalmışsa o da `docker compose -p rag-e2e -f deployment/docker-compose.yml down -v --rmi local` ile silinir. `docker compose -f deployment/docker-compose.dev.yml stop` çalıştırılır (dev verisi silinmez).
  2. Saat başlatılır. README "Kurulum" adımları sırayla izlenir: modeller (`ollama.md`), reranker, `docker compose -f deployment/docker-compose.yml build --no-cache`, `up -d`, `ps` (dört servis `healthy`).
  3. Rehber ekrandan yüklenir, Önizleme'de ilk soru sorulur → saat durur (C1).
  4. Sistem `down -v` ile yeniden boşaltılır, açılır ve acceptance spec koşulur. Hemen ardından loglar toplanır (G2).
  5. Ölçümler (C3, C4) ve README komut denemesi (C2) yapılır.
- Sistem qa için açık bırakılır. Kapanışta da deneme verileriyle açık kalır.
- **Lokal modeller:** bugünkü `qwen3.5:9b`, `bge-m3` (Ollama 11434) ve `bge-reranker-v2-m3` (llama-server 8081). Hiçbiri durdurulmaz.

## Lead adımları
**L1. Onaydan hemen sonra (kural 8, kart onayıyla).** Metinler birebir uygulanır:
1. **`rag.md` 30. satır** yerine:
   > - **Senaryo:** Raşit Bankası'nın dijital asistanı, müşteri sorularını "Ürün ve Hizmet Rehberi" PDF'ine dayanarak Türkçe, kısa ve kaynak numaralı cevaplar. Rehber kurgusaldır ve repodadır (`docs/rehber/`): 70 sayfa, 11 bölüm, iki sürüm (2026.10 ana sürüm, 2026.11 isteğe bağlı ikinci belge). Bilgi bankası boş açılır; belgeler Bilgi kaynakları'ndaki "Belge ekle" ile girer. Rehberde olmayan soruda sabit cümle: "Bu konuda rehberimizde bilgi bulamadım."
2. **`.claude/skills/rag-pipeline/references/indexing.md` 15. satır** yerine:
   > - Kaynak belge yer imli bir PDF'tir (örnek: "Raşit Bankası Ürün ve Hizmet Rehberi", `docs/rehber/`, 70 sayfa, 11 bölüm, sürüm 2026.10 ve 2026.11). Belgeler "Belge ekle" ile yüklenir; açılışta kendiliğinden içe alınmaz. Sayfa numarası ve bölüm başlığı parçaya taşınır; alıntıda "s. 7" gösterilir.
3. **Kök `CLAUDE.md` "Mimari" altındaki "Durum" maddesi** yerine:
   > - Durum: ürün çalışıyor (TASK-002…016, TASK-027): dört ekran gerçek veriyle, lokal modellerle (Ollama `qwen3.5:9b` ve `bge-m3`, llama-server `bge-reranker-v2-m3`) ve Docker Compose ile. Bilgi bankası boş açılır; belgeler "Belge ekle" ile yüklenir, golden set ve öneri listesi dosyayla yüklenir. TASK-018: uçtan uca doğrulama ve dokümanlar. Modüllerin güncel yapısı kendi `CLAUDE.md` dosyalarındadır.
4. **Kök `CLAUDE.md` klasör tablosu:**
   - `ollama.md` satırının açıklaması şu olur: `Lokal modellerin kurulum ve kullanım rehberi (TASK-003)`.
   - `docs/api/sse.md` satırının açıklamasından `(ilk akış task'ında oluşturulur)` kalkar.
   - `docs/tasks/...` satırından sonra yeni satır eklenir:

     > | `docs/rehber/` | Raşit Bankası Ürün ve Hizmet Rehberi: Typst kaynağı, iki sürümün PDF'i (2026.10, 2026.11; 70 sayfa, 11 bölüm), golden set (`golden/rehber-golden-v1.json`), rehberde olmayan konular ve doğrulama betikleri. Bilgi bankasına "Belge ekle" ile yüklenir; açılışta kendiliğinden yüklenmez |

**L2. qa `DONE` ve kullanıcı kabulünden sonra:**
1. devops ve qa raporlarındaki "Doküman önerileri"ni (`rag.md`, `KULLANIM-KILAVUZU.md`) kullanıcıya sunar ve onaylananları uygular.
2. Kapanışı kök `CLAUDE.md` madde 9'a göre yapar. Sistem deneme verileriyle açık kalır (karar 1B). `down` sadece kullanıcı isterse yapılır.

## Açık sorular ve riskler
- Kart soruları cevaplandı: 1B, 2A, 3A, 4 (OWASP yok). Açık soru yok.
- Stryker'ın Vitest 5 desteği kurulumda doğrulanacak. Desteklemiyorsa frontend-coder durur; planner kararı gerekir.
- PIT, Testcontainers'lı entegrasyon testleri yüzünden modül başına uzun sürebilir (30-60 dk). Docker açık olmalı.
- `page.pdf({ outline: true })` yer imi üretmezse (A3) tester durur ve lead'e bildirir. Yedek: kullanıcıdan yer imli bir PDF istenir (TASK-027 kararı).
- Servislerin Java bağımlılıkları bu task'ta güvenlik taramasından geçmiyor (karar 4).

STATUS: DONE
