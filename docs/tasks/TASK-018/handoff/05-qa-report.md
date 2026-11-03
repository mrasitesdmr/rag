TASK: TASK-018
# QA raporu

Scratchpad (`$S`): `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/3f94d08a-d707-4cc3-9213-5c952cf755f2/scratchpad`
Görüntüler: `$S/gorsel/<no>-lab.png` ve `$S/gorsel/<no>-web.png`. Betikler: `$S/lab1.mjs`, `lab2.mjs`, `lab3.mjs`, `lab4.mjs`, `web1.mjs`, `web2.mjs`.

## Durum özeti
**Düzeltme turu 1 sonrası: DONE (ilk tur NEEDS_FIX, frontend-coder 3 madde, düzeltildi).** Görsel karşılaştırmada lab'dan bilinçsiz 3 fark bulundu (parça penceresi başlığı, Konuşmalar parça tablosu, Önizleme "Yeni konuşma" ayracı). Mock taraması, log denetimi, `npm audit`, README ve L1 temiz. Mutation ve tam E2E regresyonu (C7, C8, C9, C15) frontend düzeltmesinden sonra, düzeltme turunda yapılır: Stryker ve E2E web kodunu ölçer; düzeltmeden önce koşmak boşa gider. Backend tarafı (`verify` ve PIT) düzeltmeden etkilenmediği için arka planda koşuyor; sonuçları aşağıdaki "Komutlar" ve "Mutation testi" bölümlerine koşu bitince işlenecek (bkz. "Bekleyenler").

## Kullanılan skill'ler
`code-review-and-quality`, `frontend-conventions`, `web-design-guidelines` (görsel), `webapp-testing` (Playwright betikleri), `rag-evaluation`, `springboot-verification` (komutlar), `writing-good-tests` (mutation değerlendirmesi), `frontend-security` (`npm audit`).

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|-----------------------|--------------------|-------|
| 1 | Senaryolar (E2E) A1-A11 gerçek modellerle | `web/e2e/acceptance.spec.ts`; devops 11/11 iki kez (boş sistemde, `04-deploy-report.md`); qa'nın kendi koşusu düzeltme turunda (C15 yalnız acceptance dışı dosyaları koşar) | ⏳ düzeltme turunda yeniden |
| 2 | Görsel karşılaştırma, bilinçsiz fark yok | Aşağıdaki tablo | ❌ 3 bilinçsiz fark |
| 3 | Mock taraması | Aşağıda; izin listesi dışı isabet 0 | ✅ |
| 4a | PIT iki serviste, eşik %80 | `pom.xml` `mutationThreshold` 80; tester %88 / %86; qa koşusu bekliyor | ⏳ |
| 4b | Stryker web, eşik %80 | `stryker.config.mjs` `break: 80`; tester %83,14; qa koşusu düzeltme turunda | ⏳ |
| 4c | `npm audit` | Aşağıda: `--omit=dev` 0 açık | ✅ |
| 4d | OWASP | Yapılmaz (kullanıcı kararı 4) | — |
| 5 | Performans ve bellek, README'de "bu makinede ölçülen" | README "Ölçümler" ↔ `04-deploy-report.md` tabloları aynı (C13) | ✅ |
| 6 | Log denetimi | Aşağıda; ihlal 0 | ✅ |
| 7 | README (kurulum, çalıştırma, ekranlar, mimari, ölçümler) | `README.md` başlıkları: Gereksinimler, Kurulum, İlk kullanım, Ekranlar, Mimari, Ayarlar, Ölçümler, Testler, Sorun giderme, Agent ekibi, tmux; devops komut tablosu ✅ | ✅ |
| 8 | Eskimiş bilgiler (L1) | Aşağıda C14 | ✅ |

## Görsel karşılaştırma (C5)
Yöntem (K5): lab `file://…/kaynaklar/rag-lab.html` 1480×900'de açıldı, `.sbody` kutusu **737,5 × 829** ölçüldü; lab'ın adım adım akışı "Devam" tıklamalarıyla sonuna kadar oynatıldı. Web 738 genişlikte açıldı. **Yükseklik notu:** lab'ın telefon yüksekliği `clamp(440px, 100vh − 356px, 600px)` olduğundan `100vh` lab penceresinin 900'ü; web de 738×**900**'de açıldı (829'da telefon 63 px kısa çıkıyordu; bu bir fark değil, ölçüm farkıydı). Öneri çipleri için sistemde liste yoktu; `PUT` yerine tarayıcıda `GET /api/assistant/suggestions` yanıtı Playwright `route` ile verildi (sistem verisi değişmedi). Boş durum ayrıca V1-bos-oneri.

| No | Ekran | Lab görüntüsü | Web görüntüsü | Fark | Bilinçli mi / NEEDS_FIX |
|----|-------|---------------|---------------|------|-------------------------|
| V1 | Önizleme, boş | `$S/gorsel/V1-lab.png` | `V1-web.png`, boş öneri: `V1-bos-oneri-web.png` | Sağ üstte kullanıcı alanı yok; şerit "Rehber 2026.10" yerine "Bilgi kaynakları 2 belge"; "Asistan nasıl cevaplıyor?" metninde "5'i … modeline" yerine "en iyi 5 parça … modeline gider" (model adı yapılandırmadan geldiği için Türkçe ek verilemez); telefon saati; çipler gerçek öneri listesinden. Bileşen sırası, boşluk, renk, yazı tipi, telefon çerçevesi aynı | Bilinçli: kullanıcı alanı yok (K6/TASK-002); sayı ve metin gerçek veriden (K6) |
| V1b | Önizleme, öneri listesi boş ("Henüz soru önerisi yok.") | yok | `V1-bos-oneri-web.png`, `V3-web.png` | Lab'da yok | Bilinçli: lab'da olmayan boş durum (K6) |
| V2 | Önizleme, alıntılı cevap | `V2-lab.png` | `V2-web.png` | Cevap metni, parça listesi, süre çubukları farklı (gerçek model). Web'de ayrıca cevap altında kaynak düğmeleri ("Raşit Bankası Ürün ve H… · s. 16"), "Faydalı mı?" satırı aynı. Sarı "cevabın geçtiği parça" satırı ve altyazısı webde yok (turun golden karşılığı yok) | Bilinçli: gerçek veri / golden set yok (K6). Kaynak düğmeleri TASK-008 kararı |
| V3 | "Bulamadım" + temsilci | `V3-lab.png` | `V3-web.png` | Aynı sıra, aynı "Temsilci kuyruğundasın". **Ayraç: lab "Yeni konuşma · cnv_48212", web "Yeni konuşma · 6"** (kimlik `cnv_` önekisiz) | ❌ **NEEDS_FIX #3** frontend-coder |
| V3t | Toast ("Temsilci kuyruğuna alındı") | `V3-toast-lab.png` | `V3-toast-web.png` | Görüntü anında iki tarafta da toast görünmedi (zamanlama); karşılaştırılamadı. Toast metni ve konumu `preview.spec.ts`/`acceptance.spec.ts` ile doğrulanıyor; düzeltme turunda görüntü yeniden alınacak | ⏳ |
| V4 | Parça / alıntı penceresi | `V4-lab.png` | `V4-web.png` | **Başlık: lab "[1] s. 11 · Kart kaybı, …", web sadece "2.16 Kayıp ya da çalıntı bildirimi"** (ne `[n]` ne sayfa). Meta kutularında "rehber 2026.10" yerine "kaynak <ad> · <sürüm>" (iki belge olabilir). Sarı vurgu yok (golden yok). Metin kutusu `max-height 300px` iki tarafta aynı | ❌ başlık: **NEEDS_FIX #1**. Meta kutusu: bilinçli (çoklu belge, gerçek veri). Vurgu: bilinçli |
| V5 | "Kayıtlı sorular" | `V5-lab.png` | `V5-web.png` | Açıklama paragrafı: lab "Her soru rehber üzerinde gerçekten çalıştırıldı…", web "Ürün ekibinin hazırladığı öneri sorular; sorunu her zaman kendin de yazabilirsin…". Başlık, kategori başlıkları, satırlar aynı | Bilinçli: TASK-012 kart soru 4 (kayıttan oynatma yok) |
| V6 | Konuşmalar listesi | `V6-lab.png` | `V6-web.png` | Durum sütununda "Temsilcide" çipi (lab'da yok); kimlikler `cnv_<n>`, saatler gerçek | Bilinçli: TASK-013 soru 3 = A, TASK-014 K3h (kullanıcı onaylı, lab'da karşılığı olmayan durum) |
| V7 | Konuşma detayı | `V7-lab.png` | `V7-web.png` | Üstte "Temsilciye aktarıldı" satırı (lab'da yok, TASK-014 K3h). **Parça tablosu "PARÇA" hücresi: lab "s. 4 · Vadeli mevduat ve faiz oranları · #6", web "s. 52 · #115" (başlık yok; sadece `title` ipucunda)** | Aktarma satırı: bilinçli. Başlık: ❌ **NEEDS_FIX #2** |
| V8 | Bilgi kaynakları | `V8-lab.png` | `V8-web.png` | "Belge ekle" düğmesi, iki satır, sayfa/parça sayıları gerçek. Bilgi kutusu aynı | Bilinçli: lab'ın "Yeni sürüm yükle"si yerine "Belge ekle" (TASK-015 iptal, TASK-027) |
| V9 | "Belge ekle" penceresi | lab'da yok (lab'ın karşılığı "Yeni sürüm yükle") | `V9-web.png` (boş), `V9-hata-web.png` (`PDF_NO_BOOKMARKS` mesajı) | Lab'da karşılığı yok; pencere lab'ın `.dlg` stilinde (başlık, kapat, alanlar, düğmeler hizalı) | Bilinçli (K6) |
| V10 | Kaynak detayı | `V10-lab.png` | `V10-web.png` | Meta kutuları, işler tablosu, filtre, parça tablosu aynı sıra ve hizada; "Yeni sürüm yükle" düğmesi yok; sayılar gerçek | Bilinçli (K6) |
| V11 | Değerlendirme (ölçüm sonrası) | `V11-lab.png` | `V11-web.png` | Kutular, "Çalıştırmalar", "Kaçanlar ve sırası değişenler" aynı sıra ve hizada. Sayılar gerçek (recall 0,98, Kaçan 2 kırmızı) | Bilinçli: gerçek veri (K6). Hizada fark yok |
| V11b | Değerlendirme boş / "Ölçülüyor" | lab'da boş hâl var (ilk lab koşusu: "Henüz ölçüm yok…", dosyası üzerine yazıldı), "Golden set yüklenmemiş." ve "Ölçülüyor" yok | görüntü alınmadı; `A1`/`A11` E2E senaryoları metinleri doğrular | "Golden set yüklenmemiş." ve "Ölçülüyor · n/100" lab'da yok | Bilinçli: lab'da olmayan durum (K6) |

**Tester raporundaki iddia doğrulandı:** parça penceresinin başlığında sayfa yok; lab'ın penceresinde var. Bilinçsiz fark (#1). Veri sözleşmede mevcut: `ChunkResponse.pageNumber` ve `heading` (`web/src/shared/chunk/types.ts`).

## Sözleşme uyumu
- Web tip ve çağrıları: `shared/chunk/types.ts`, `features/*/model/types.ts` `openapi.yaml` alanlarıyla eşleşiyor; canlı yanıtlar (`/api/workspaces/42/chunks/115`, `/api/assistant/conversations`, `/api/assistant/suggestions`) alan adlarıyla uyuştu. Sözleşme değişmedi (plan).
- SSE: `docs/api/sse.md` olayları canlı akışta (Playwright ve devops `curl -sN`) token, done sırasıyla geliyor; frontend'in ayrıştırması A5/A7 ve kendi akışlarımda çalıştı.
- Servis tarafı kodu bu task'ta değişmedi (yalnız `pom.xml` PIT eşiği; `git diff`).

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `cd web && npm run build && npm run lint && npm test` | build ve lint geçti; **728/728** test, 46 dosya | ✅ (tester: 728) |
| `cd web && npm audit --omit=dev` | 0 vulnerabilities | ✅ (devops: 0) |
| `cd web && npm audit` | 5 moderate (yalnız dev araç zinciri) | bkz. C10 |
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, **441 test**, 0 hata (20 sn) | ✅ (tester/devops: 441) |
| `cd retrieval-api && ./mvnw verify` | BUILD SUCCESS, **344 test**, 0 hata (24 sn) | ✅ (344) |
| `npm run test:coverage`, E2E, Stryker | ⏳ düzeltme turu | |

## Mock taraması (C6)
| # | Sonuç |
|---|-------|
| M1 sabit soru | Java: 2 isabet, ikisi de sınıf Javadoc'u (`"Faydalı mı?"`); `web/src` ürün kodunda 0. Sabit sorular yalnızca `web/src/test/*Fixtures.ts` (test) |
| M2 Java'da Türkçe içerik | Kod string'i olarak 3 satır: `PdfDocumentParser.java:33-35` (`"Sık sorulanlar"`, `"Sık sorulan sorular"`, `"Sıkça sorulan sorular"`, `SSS`, `FAQ`): PDF'te "sık sorulanlar" bölümünü tanıyan ayrıştırma sözlüğü, veri değil. **Optional/FYI** (kural uyarınca ayrıştırma sezgisi; sahte cevap/parça/konuşma değil). Gerisi Javadoc |
| M3 model adı | Ürün kodunda 0 (adlar `application.properties` ve `@Value`'dan gelir) |
| M4 kullanıcı/kişi | 0 |
| M5 sayı sabiti | Üst düzey sabitler API sözleşmesindeki enum değerleri (`TURN_STATUSES`, `RETRIEVAL_STRATEGIES`, …) ve `*Rules.ts` iş kuralları; metrik/veri listesi yok |
| M6 açılışta çalışan kod | `IngestionStartupListener` (yarıda kalan işleri `FAILED` yapar), `EvalRunStartupListener` (ölçümler için aynısı), `RerankerHealthChecker` (`@Scheduled`, sadece sağlık). Gövdeler okundu; veri oluşturan yok |
| M7 migration tohumu | Tek `INSERT`: `retrieval-api` V2 çalışma alanı satırı (izin listesinde) |
| M8 üründe test dublörü | `msw`, `vi.mock`, `vi.fn`, Mockito: üründe 0 |
| M9 deployment tohumu | `deployment/postgres/initdb/01-init-databases.sh` yalnız rol, veritabanı, `vector` eklentisi; `web/public` yalnız `favicon.svg` |

**Sonuç: izin listesi dışında isabet 0 (M2 notu FYI).**

## Log denetimi (C12)
Taranan dosyalar (`04-deploy-report.md`): `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/9e2628af-0899-45bf-b3a3-4af016d3f6d6/scratchpad/loglar/` (acceptance 1. koşu: `acceptance-compose.log` 949 satır, `assistant-api.log`, `retrieval-api.log`, `web.log`, `postgres.log`, `ollama-host.log`, `reranker-host.log`) ve `.../loglar2/acceptance2-compose.log`. Duyarlılık kontrolü: dosyalar gerçek içerik taşıyor (`Search completed` 104 satır, nginx erişim satırları).

| Kalıp | İsabet (8 dosya toplamı) |
|-------|--------------------------|
| L1 soru/cevap (`Kartımı kaybettim`, `Yenisi kaç günde`, `Bitcoin`, `rehberimizde bilgi bulamadım`; URL kodlu `Kart%C4%B1m%C4%B1`) | 0 |
| L2 golden `answerText` ilk 30 karakter (ilk 20 kayıt) | 0 |
| L3 prompt (`<parcalar>`, `Given the following conversation`, `Sen Raşit Bankası`, `Standalone query`) | 0 |
| L4 Türkçe karakter (servis logları) | 0 |
| L5 e-posta, TCKN, IBAN, telefon | 0 |
| L6 `Authorization` | 0 |

FYI: nginx erişim logunda parça filtresi `GET …/chunks?query=asgari+%C3%B6deme` (kullanıcının yazdığı filtre; plan K10'a göre FYI). **İhlal 0.**

## `npm audit` (C10)
- `npm audit --omit=dev`: **0 vulnerabilities** → K8 gereği düzeltme yok.
- `npm audit` (dev dahil): 5 moderate, yalnız dev bağımlılıkları (`npm audit fix` önerisi, `--force` yok); runtime'a girmez. FYI.
- OWASP: yapılmadı (kullanıcı kararı 4).

## README (C13)
Başlıklar plandaki sırayla var (Proje girişi, Gereksinimler, Kurulum, İlk kullanım, Ekranlar, Mimari, Ayarlar, Ölçümler, Testler, Sorun giderme, Agent ekibi, tmux). "Durum" satırı yok. Ölçüm tablosu (`README.md` 99-130) ile `04-deploy-report.md` aynı sayılar (1199/1269, 56/188, 956/1127, 245/4598, 1986/6558, bellek 1135→1143 MiB vb.). Komut denemesi tablosu devops raporunda ✅ (PIT ve tam E2E hariç; onlar qa'da).

## L1 doğrulaması (C14)
- `grep -rn --exclude-dir={kaynaklar,tasks,node_modules,target,.git,handoff,reports,.stryker-tmp} -e "31 sayfa" -e "61 parça" .` → **boş** ✅ (handoff dosyaları, bu rapor dahil, plandaki arama kapsamına girmez ve geçmiş kayıttır).
- Plandaki L1 metinleri (`rag.md` Senaryo satırı, `indexing.md`, kök `CLAUDE.md` Durum ve klasör tablosu) yerinde: grep boş ve kök `CLAUDE.md` Durum maddesi plandaki metinle birebir ✅.

## Mutation testi
| Araç | Tester sonucu | qa koşusu |
|------|---------------|-----------|
| PIT assistant-api | %88 (802 mutant, 702 öldü) | ⏳ arka planda |
| PIT retrieval-api | %86 (629 mutant, 541 öldü) | ⏳ arka planda |
| Stryker web | %83,14 (42 dk 14 sn) | düzeltme turunda (web kodu değişecek) |

Hayatta kalan mutant ve eşik yorumu koşu bitince eklenecek.

## Uçtan uca (C15)
Düzeltme turunda (web değişecek): `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e`. Bugüne kadar: devops acceptance 11/11 (iki kez, boş sistemde, gerçek modellerle; A11 recall@5 0,98). qa'nın kendi canlı denemeleri (Playwright ile gerçek modelle): "Kartımı kaybettim" alıntılı cevap + kaynak düğmeleri + Evet/Hayır; "Bitcoin" soruda sabit cümle + "Temsilciye bağlan" → "Temsilci kuyruğundasın"; Konuşmalar listesi ve detay; yer imsiz PDF reddi mesajı. Örnek cevap (gerçek model): "Kartınızı kaybettiyseniz vakit kaybetmeden aşağıdaki kanallardan biriyle kapatın [1]. Bildirimden sonra kartınızla yapılan işlemlerden sorumlu olmazsınız [1]. Yeni kartınız kendiliğinden basılır ve kayıtlı adresinize kuryeyle gönderilir [3]." (alıntı `s. 16 · 2.16 Kayıp ya da çalıntı bildirimi`, doğru parça). "Bitcoin" cevabı: "Bu konuda rehberimizde bilgi bulamadım. [5]".

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Required | `web/src/shared/chunk/ChunkDialog.tsx:19` | Pencere başlığı yalnızca `chunk.heading`. Lab: `[n] s. {sayfa} · {başlık}` (`n` alıntıdan açılınca, Bilgi kaynakları'ndan açılınca yok). `pageNumber` sözleşmede ve type'ta var | frontend-coder |
| Required | `web/src/features/conversation/components/TurnChunkTable.tsx:63-68` | "Parça" hücresi `s. N · #n`; lab `s. N · {başlık} · #n`. Başlık yalnız `title`'da (dokunmatik/klavyede görünmez) | frontend-coder |
| Required | `web/src/features/preview/components/MessageList.tsx:60` | Ayraç `Yeni konuşma · ${conversationId}` ham sayı; lab `cnv_<id>` (`turnLabels` içindeki kimlik biçimi Konuşmalar'da zaten kullanılıyor) | frontend-coder |
| Optional | `PdfDocumentParser.java:33-35` | "Sık sorulanlar" ayrıştırma sözlüğü kodda Türkçe sabit; veri değil, yapı sezgisi | FYI |
| FYI | tur kaydı `cnv_6` | Model "bulamadım" cevabına `[5]` alıntı numarası ekledi (gerçek model, `completionTokens 14`); alıntı düğmesi 5. parçaya bağlı (✓). Prompt "sadece cümleyi yaz" der; model bazen uymuyor. A6 kabul testi bu örnekte düğmenin olmadığını bekler; nondeterministik, bu koşuda kırılmadı | — |
| FYI | nginx `?query=` | Parça filtresi erişim logunda | — |

## Doküman önerileri
`rag.md` ve `KULLANIM-KILAVUZU.md` için devops'un `04-deploy-report.md` "Doküman önerileri" bölümündeki üç değişiklik (rag.md 4. satır "Durum", rag.md 38. satır boş yorum, kılavuz 76. satır) qa tarafından da doğrulandı: metinler yerinde değişiklik bekliyor; `rag.md` 4. satırda hâlâ "Durum: taslak" duruyor (grep). Ek öneri yok.

## Bekleyenler (düzeltme turunda yapılacak)
Backend `verify` ×2 bitti (yukarıda); PIT ×2 koşuları arka planda sürüyor (backend kodu düzeltmeden etkilenmez); bitince bu raporun "Komutlar" ve "Mutation testi" bölümleri doldurulacak. Düzeltmeden sonra: `npm run build/lint/test/test:coverage`, `npm run test:mutation` (C9), `npm run test:e2e` (C15), etkilenen ekranlar için V2/V3/V4/V7 görüntüleri yeniden.

## Düzeltme
**frontend-coder:**
1. `ChunkDialog` başlığı lab gibi: alıntıdan açılınca `[n] s. {pageNumber} · {heading}`, Sadece Bilgi kaynakları'ndan açılınca `s. {pageNumber} · {heading}` (düzeltme: lab'da Konuşmalar tablosu ve cevap alıntıları da `data-cite` ile `[n]` gösterir; frontend-coder doğruladı, qa teyit etti). (`n`'i çağıran taraf geçirir; Önizleme alıntı düğmesi ve parça kartı.) Mevcut testler başlığı `heading` ile arıyorsa tester güncellemesi gerekir.
2. `TurnChunkTable` "Parça" hücresi `s. {pageNumber} · {heading} · #{chunkNo}` olsun (lab `.tbl`). Başlık uzunsa lab gibi satır içinde görünür.
3. Önizleme ayracı `Yeni konuşma · cnv_{id}` (Konuşmalar'daki `turnLabels` biçimi).
Zincir: frontend-coder → tester → devops → qa. (Tester: ilgili testleri güncelle ve Stryker eşiğini koru; qa düzeltme turunda C9, C15 ve etkilenen ekran görüntülerini yeniden koşar.)

## Düzeltme turu 1
Görüntüler: `$S/gorsel1/`. Düzeltilen kod: `ChunkDialog.tsx`, `TurnChunkTable.tsx`, `turnLabels.ts`/`conversationLabel`, `MessageList.tsx` (`git diff`, frontend `Düzeltme turu 1`).

| Madde | Kanıt | Sonuç |
|-------|-------|-------|
| 1. Parça penceresi başlığı | `gorsel1/V4-web.png`: `[1] s. 16 · 2.16 Kayıp ya da çalıntı bildirimi`; lab `V4-lab.png`: `[1] s. 11 · Kart kaybı, …` (aynı biçim). Konuşmalar'dan da `[n]` (lab `data-cite`, doğrulandı); Bilgi kaynakları'nda `[n]` yok (lab `data-chunk`) | ✅ |
| 2. Konuşmalar parça tablosu | `gorsel1/V7-web.png`: `s. 52 · 7.4 Döviz alım satımı ve hafta sonu kuru · #115` (lab biçimi `s. N · başlık · #n`) | ✅ |
| 3. Önizleme ayracı | `gorsel1/V3-toast-web.png`: `Yeni konuşma · cnv_5` (lab `cnv_48212`) | ✅ |

Etkilenen ekranlar yeniden alındı (V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11; web `gorsel1/*-web.png`, lab `V2/V3/V4-lab.png`): başka fark çıkmadı; ilk turdaki bilinçli farklar aynı. **Toast:** 150 ms sonra alınan görüntüde iki tarafta da toast görünmedi (headless zamanlaması); toast metni E2E'de (`preview.spec.ts`, A6/A8) doğrulanıyor, görsel karşılaştırma yapılamadı: FYI, bilinçsiz fark kanıtı yok.

### Yeniden koşulan kontroller
| Komut / kontrol | Sonuç | Raporla tutarlı mı |
|-----------------|-------|--------------------|
| `cd web && npm run lint`, `npm test` | lint temiz; 46 dosya, **728/728** | ✅ (tester 728) |
| `cd web && npm run test:mutation` (C9, bağımsız, 12:29-13:17) | **%83,26** (eşik 80, `break`), exit 0, 48 dk 9 sn. (11:12'deki koşu düzeltme öncesiydi, kullanılmadı.) | ✅ (tester %83,23) |
| PIT assistant-api (C7, bu oturumda 09:51-10:06) | BUILD SUCCESS, **%88** (802 mutant, 702 öldü; 53 kapsamsız; test gücü %94; satır kapsamı %95) | ✅ |
| PIT retrieval-api (C8, 10:06-10:23) | BUILD SUCCESS, **%86** (629 mutant, 541 öldü; 29 kapsamsız; test gücü %90; satır kapsamı %97) | ✅ |
| `./mvnw verify` ×2 | 441 / 344 test, geçti | ✅ |
| C15 `E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | **18 geçti, 12 atlandı**, 0 kalan, 1,9 dk. Atlananlar: acceptance dosyası (11, `E2E_ACCEPTANCE` yok, gerekçeli) ve `sources.spec.ts` "boş bilgi bankası" (1, sistem dolu) | ✅ |
| Log denetimi, yeni set (`.../loglar-t1/`: acceptance-compose 1019 satır, assistant-api, retrieval-api, web, postgres, ollama-host, reranker-host) | L1 soru/cevap, L2 parça metni, L3 prompt, L4 Türkçe karakter, L5 kişisel veri, L6 Authorization: hepsinde **0 isabet** | ✅ |

PIT için not: backend kodu bu turda değişmedi (`git diff` yalnız `web/src`, `web/CLAUDE.md` ve önceki dokümanlar); PIT sonuçları geçerli. Hayatta kalan mutant incelemesi: tester kural bazlı hata sinyali bulmadı; qa eşik üstü skorlarda ek bulgu çıkarmadı.

### Mutation değerlendirmesi (`writing-good-tests`)
Skorlar eşiğin üstünde, ama Stryker'da düşük kalan dosyalar tester raporunda sayılı (`MessageList.tsx`, `AddSourceDialog.tsx`, `useAnswerStream.ts`, `Dialog.tsx`, `EvaluationPage.tsx`, `PreviewPage.tsx`; stryker çıktısında `AnswerText.tsx` %76 de var). Kabul eşiği (%80, toplam) sağlandığı için Optional.

### Kabul kriterleri
| Kriter | Sonuç |
|--------|-------|
| Bütün E2E senaryoları gerçek modellerle geçmiş | ✅ acceptance 11/11 (tester rag-e2e, devops ana sistem, iki turda) + qa regresyonu 18/18 çalışan |
| Görsel karşılaştırma tablosu, bilinçsiz fark kalmamış | ✅ (3 fark düzeltildi, doğrulandı) |
| Mutation, güvenlik, log denetimi raporda | ✅ PIT %88/%86, Stryker %83,26, `npm audit --omit=dev` 0, log 0 ihlal |
| README güncel, komutlar çalışıyor | ✅ (devops tablosu; qa C13) |

### Kod review (tur sonu)
| Severity | Bulgu | Rol |
|----------|-------|-----|
| Optional | Gerçek model, TASK'ın kendi örneği "Bitcoin alıp satabilir miyim?" sorusuna her seferinde (iki koşuda aynı, temperature 0) `Bu konuda rehberimizde bilgi bulamadım. [5]` yazıyor; `[5]` işareti ve 5. parçada ✓ kalıyor. "Rehber · s. N" düğmesi gösterilmiyor (TASK-008 kural 7 sağlandı), tur "BİLMİYOR". Prompt lab'la aynı; model uyumu. İsterseniz alıntı işaretini bulamadım cevabında çizmeme kuralı yeni task olabilir | — |
| Optional | `PdfDocumentParser.java:33-35` Türkçe "Sık sorulanlar" ayrıştırma sözlüğü | — |
| FYI | nginx erişim logunda `?query=`; `npm audit` dev 5 moderate | — |

### Doküman önerileri (rag.md, KULLANIM-KILAVUZU.md)
Devops'un önerileri (rag.md 4. satır "Durum: taslak" → "ürün çalışıyor (TASK-002…016, TASK-027)", rag.md 38. satır boş yorum, kılavuz 76. satır `(ilk model task'ında oluşturulur)` silinmesi) geçerli; `rag.md` 4. satırda hâlâ "Durum: taslak" duruyor. Ek öneri yok.

STATUS: DONE
