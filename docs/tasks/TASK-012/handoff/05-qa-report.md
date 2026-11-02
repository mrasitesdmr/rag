TASK: TASK-012
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. (Diğer skill'ler ayrıca yüklenmedi; kural kontrolü kod, test ve çalışan sistem üzerinden yapıldı.)

## Kural kontrolü
Kaynak: onay kartı "Bittiğini nasıl anlayacaksın" 1-7 (kabul kriteri) + TASK.md. Deneme listesi: 3 kategori, 8 soru, 2 takip (kartı kaybettim → "Yenisi kaç günde gelir?", "Kartımı geçici kapatabilir miyim?"; 2 başlangıç işaretli). Tarayıcı: Playwright/chromium, `localhost:3000`, gerçek model (qwen3.5:9b).

| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Liste boşken çipte "Henüz soru önerisi yok."; pencere "0 soru, 0 takip" + boş durum metni | Canlı: çip `Henüz soru önerisi yok.`; pencere `Kayıtlı sorular · 0 soru, 0 takip` + açıklama + `Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür.` (kartın metinleri birebir). Esc pencereyi kapattı. E2E `...ChipsMatchSuggestionSet` | ✅ |
| 2 | Liste yüklenince boş konuşmada 2 başlangıç çipi; tıklayınca sorulur | Canlı: yükleme sonrası sayfa yenilenince `Kartımı kaybettim, ne yapmalıyım? \| Kart aidatı ne kadar?` (en fazla 2); çipe tıklayınca soru soruldu, gerçek model cevabı geldi (alıntılı, "kart kalıcı kapanır, yeni kartınız 3-5 iş günü içinde…"). Cevap akarken çipler `disabled`, input kilitli | ✅ |
| 3 | Cevaptan sonra ilk çip takip; aynı konuşmada sorulur; diğer çip yeni konuşma | Canlı: cevap bitince çipler `Yenisi kaç günde gelir? \| Kart aidatı ne kadar?`. Takip çipi: yeni "Yeni konuşma ·" ayracı 0 (aynı konuşma). Takipten sonra yeni takip çıkmadı (karar 2, lab gibi). Diğer çip: 1 yeni ayraç (yeni konuşma). Soru metni sorulmuş soru tekrar önerilmedi | ✅ |
| 4 | "asgari" → asgari önerileri; ↓/Enter/Esc | Canlı: `listbox` + 3 `option` (`Asgari ödemeyi yaparsam ne olur?`, `Asgari tutarın altında…`, `Kredi kartı asgari ödeme tutarı…`; başlangıçla eşleşen önce). ↓ ilk seçeneği `aria-selected` + `aria-activedescendant` yaptı; Esc listeyi kapattı, `asgari` yazısı kaldı; ↓+Enter soruyu gönderdi, input boşaldı | ✅ |
| 5 | Listede olmayan soru + Enter normal sorulur | Canlı: "Havale limiti nedir?" yazıldı (kök "limi" nedeniyle liste açıldı, hiçbir seçenek seçilmediği için) Enter → yazılan soru aynen soruldu, "listeden seç" uyarısı yok | ✅ |
| 6 | Pencerede soruya tıklayınca sorulur + kapanır; Esc kapatır | Canlı: pencere başlığı `Kayıtlı sorular · 8 soru, 2 takip`, kategoriler dosya sırasıyla (Kartlar, Krediler, Dijital — veriden), odak `dialog` içinde, tıklayınca pencere kapandı ve soru soruldu; Esc (boş durum penceresinde) kapattı. "Bu konuşmada devam" bölümü: `PreviewPage.suggestions.test.tsx:115,164` (canlıda ayrıca denemedim) | ✅ |
| 7a | Lab ile görünüm | `kaynaklar/rag-lab.html` sol yarısı ile yan yana ekran görüntüsü: çipler (2 hap, telefonun altında), giriş solunda liste düğmesi (☰ yuvarlak), pencere (başlık + ×, açıklama, büyük harf soluk kategori başlığı, girintili sorular, beyaz yuvarlak köşeli kart, koyu arka perde) aynı düzen ve renkler. Fark: sadece içerik (ürün ekibi listesi, açıklama metni kartta onaylı), lab'ın 149 soruluk gömülü listesi yok | ✅ |
| 7b | Klavye + a11y testleri, derlemeler | Aşağıdaki komutlar; `role=listbox/option/combobox/dialog` canlıda doğrulandı, vitest-axe testleri `PreviewPage.suggestions.test.tsx`, `MessageComposer.test.tsx` | ✅ |
| 7c | Kodda sabit soru listesi yok | `grep -rn -i -E "kartımı kaybettim\|asgari\|Yenisi kaç\|Hesaplar\|Yatırım" web/src assistant-api/src/main` (testler/e2e hariç) → eşleşme yok. Kategori başlıkları `QuestionListDialog`'ta veriden. V4 migration seed içermiyor | ✅ |
| T1 | Kaynak kullanıcı onayıyla (kart soru 1 = A) | Kart "Kullanıcı kararları (2026-11-02)" 1-4 A; implementasyon `suggestion/` (PUT/GET, 3 tablo) | ✅ |
| T2 | Hata kuralları | Çift soru PUT → 400 `VALIDATION_FAILED` (violations alanıyla), liste değişmedi; devops: 3 MB gövde → 413 | ✅ |

## Sözleşme uyumu
- `openapi.yaml` `getSuggestions` / `replaceSuggestions` / `SuggestionSetResponse` ↔ canlı GET gövdesi (`questionCount`, `followUpCount`, `categories[].name`, `questions[].text/starter/followUps`) birebir; web yalnız `GET /api/assistant/suggestions` çağırıyor (`previewApi.ts:32`), PUT yalnız ürün ekibi `curl`.
- Hata: RFC 9457 + `errorCode: VALIDATION_FAILED` sözleşmeyle uyumlu.
- SSE ve retrieval-api değişmedi; takip/yeni konuşma sorusu mevcut akıştan gitti, cevaplar canlı geldi.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd web && npm run lint` | temiz | ✅ |
| `cd web && npm run build` | geçti | ✅ |
| `cd web && npm test` | 28 dosya, 481 test geçti | ✅ |
| `cd web && npm run test:coverage` | satır %97,04 (eşik %80), 481 test | ✅ |
| `cd assistant-api && ./mvnw verify` | 262 test, 0 hata, JaCoCo eşiği karşılandı, BUILD SUCCESS | ✅ |
| `E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | 15 geçti, 1 atlandı (`sources.spec.ts:25` boş bilgi bankası; sistemde kaynak var, TASK-012 dışı). E1 (`ChipsMatchSuggestionSet`) dahil | tester çalıştırmamıştı; şimdi yeşil |
| retrieval-api | değişmedi, çalıştırılmadı | — |

## Mutation testi
- PIT (assistant-api, sadece `suggestion.*`): 89 mutasyon, 84 öldü (%94), test strength %99; 4 mutasyon kapsamsız (`NO_COVERAGE`), hayatta kalan önemli mutant yok.
- Stryker: Kurulu değil (web/CLAUDE.md).

## Uçtan uca
Playwright paket testleri yukarıda. Manuel senaryolar (gerçek model): kartı kaybettim akışı (cevap parçalara dayalı: "Raşit Mobil, İnternet Şubesi, Müşteri Hizmetleri veya şubeye…; yeni kart 3-5 iş günü", alıntı "Rehber · s. 16"), takip çipi aynı konuşmada, diğer çip yeni konuşmada, "asgari" otomatik tamamlama, "asgari" seçimi için model "rehberde bilgi bulamadım" dedi (rehberde asgari ödeme konusu yok; uydurma yok), "Havale limiti nedir?" normal soruldu. Cevap metni deterministik değil; yukarıdaki alıntılar örnektir. Deneme listesi iş bitince boşaltıldı: `GET` → `{"questionCount":0,"followUpCount":0,"categories":[]}`. Host llama-server'a dokunulmadı. Kullandığım betikler scratchpad'de; repoya dosya eklenmedi.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| FYI | `suggestionMatcher.ts` | "Havale limiti nedir?" gibi serbest soru 4 harflik kök yüzünden ("limi") liste açar; Enter seçili öğe yoksa yazılanı gönderiyor, davranış kart karar 3'e (lab yöntemi) uygun | — |
| FYI | web | Soru listesi penceresi cevap akarken açılır ama sorular pasif (karar 6, lab gibi) | — |
| Nit | — | Takip önerisi yalnızca listedeki soru birebir sorulunca çıkar; kart riskinde belirtilmiş | — |

Critical/Required bulgu yok.

## Düzeltme
Yok.

STATUS: DONE
