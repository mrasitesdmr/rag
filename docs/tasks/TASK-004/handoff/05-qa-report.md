TASK: TASK-004
# QA raporu

Bu task'ta backend-coder, frontend-coder, tester ve devops çalışmadı; içeriği ve betikleri lead yazdı. QA, yazar ile doğrulayanın aynı kişi olmaması için bağımsız ikinci gözdür. Servis kodu, web, Docker ve API sözleşmesi bu task'ta değişmediği için bunlara ait komutlar (`./mvnw verify`, `npm run build/lint/test`, Playwright, PIT, Stryker) **uygulanamaz**. Kontrol, plandaki D1-D12, D-N ve Q-1…Q-6 ile yapıldı.

## Kullanılan skill'ler
- `code-review-and-quality` (uret.sh, dogrula.sh, RehberDogrulama.java incelemesi ve severity etiketleri).
- Diğer skill'ler (java-code-review, springboot-verification, rag-pipeline, sse-streaming, frontend-*, e2e-testing) uygulanmadı: ilgili katman değişmedi.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|------------------------|--------------------|-------|
| K1 | Kurgu sınırı: gerçek banka adı/logosu yok; kapakta "bilgilendirme amaçlıdır" | D12 GEÇTİ (24 banka adı yok, kapakta 2 not, adresler yalnız `rasitbank.example`). Elle: kapak PNG'sinde iki not var, logo yok. Metinde `garanti/papara/ziraat/vakıf/halk/akbank/yapı kredi` araması boş. Not: `Mastercard` kart şeması olarak geçiyor, bkz. bulgu O-3 | ✅ |
| K2 | Kapsam: Kural 2'deki her alt konu bir başlıkta | Lead tablosundan 16 başlık seçildi (2.1, 2.4, 2.9, 2.12, 2.16, 2.17, 3.11, 3.12, 4.6, 4.10, 4.12, 5.8, 6.4, 7.5, 8.4, 9.3). Her biri için içindekilerdeki sayfa numarasında başlık satırı gerçekten var (16/16 OK). Başlıklar 2026.11 PDF metninden çıkarıldı | ✅ |
| K3 | Rehberde olmayan konular notu | `rehberde-olmayan-konular.md`: 9 konu, her birinde 3 örnek soru, yasak kelimeler (54), sabit cevap. Bkz. Q-3. D10 GEÇTİ | ✅ |
| K4a | İçindekiler, numaralı bölüm, sayfa numarası, üst/alt bilgi | D5, D6 GEÇTİ. 70 sayfa, 11 bölüm | ✅ |
| K4b | Ücret/limit tabloları, listeler, her bölüm sonunda SSS (2-9) | D4 GEÇTİ. SSS soru sayıları kendi sayımım: 2→22, 3→19, 4→14, 5→15, 6→12, 7→11, 8→6, 9→4 (9. bölümde 1 soru `?` ile bitmiyor, elle sayıldı). Hepsi ≥ 4 | ✅ |
| K4c | Çapraz atıflar | D7 GEÇTİ (312 atıf). Ek olarak 30 atıfı rastgele seçip hedef başlıkla anlamca karşılaştırdım: 30/30 mantıklı | ✅ |
| K4d | Sayfa sayısı 60-100, kartta 68-76 | D1: 70 / 70 | ✅ |
| K5 | Metin seçilebilir, Türkçe karakter, tablo satırı tek satır | D2, D3, D4 GEÇTİ. PDFBox metninde `Ğ` yok (bkz. sapma 1) | ✅ |
| K6 | Kaynak ve üretim komutu repoda, tekrarlanabilir | `uret.sh` çalıştı; D11 bayt eşit (aşağıda) | ✅ |
| K7 | 2026.11 farkları, özet sayfalarla | `degisiklik-ozeti-2026.11.md` 15 satır; D8 GEÇTİ; D9 GEÇTİ. Gold 1.450→1.650 TL 3 yerde, ATM 25.000→30.000 TL 2 yerde, Raşit Genç eklendi, şifre cihazı kaldırıldı. Elle: `25.000/30.000` metinde başka yerde ATM limiti olarak geçmiyor | ✅ |
| K8 | Konum `docs/rehber/` | Q-1: dosyalar plandaki yollarda | ✅ |
| K9 / AC5 | Kullanıcı içerik onayı | Q-6: lead raporunda "VERİLDİ (30 Ekim 2026)" satırı var. Onaydan sonra kaynak değişmedi; PDF SHA-256 değerleri benim üretimimle aynı | ✅ (lead beyanı; onayın kendisini QA doğrulayamaz) |
| S1 | "Kartımı kaybettim" | 2.16 (s. 16) onay kartındaki metinle kelimesi kelimesine aynı: 4 kanal tablosu, kalıcı/geçici kapatma, il merkezi 3 / diğer 5 iş günü, 250 TL, 100 TL yenileme | ✅ |
| S2 | Ev kredisi erken kapama | 4.10 (s. 35) kart metniyle aynı: tazminat tablosu (6 satır), örnek 400.000 × %2 = 8.000 TL, değişken faizli ve Ek Hesap'ta alınmaz. SSS'nin ilk sorusu aynı oranları veriyor. Tarife 10.3 ile tutarlı | ✅ |
| S3 | "Bitcoin" rehberde yok | D10; `bitcoin/kripto` iki PDF'te 0 | ✅ |
| S4 | Gold 1.450 → 1.650 TL | D8 + elle: 2026.10'da 3 yerde 1.450, 2026.11'de 3 yerde 1.650 (s. 8, 18, 65) | ✅ |

## Sözleşme uyumu
Uygulanamaz. `docs/api/openapi.yaml`, `sse.md`, servis kodu ve web değişmedi (Q-1: `git status` yalnız `docs/rehber/` ve `handoff/`).

## Komutlar (kendi çalıştırdıklarım)
Çalışma dizini repo kökü. Scratch: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag-docs-rehber-kaynak-bolumler/92a7e6a0-9a94-42fe-9a27-13aa46a71de4/scratchpad`.

| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `./docs/rehber/uret.sh` | Çıkış 0. 2026.10: 70 sayfa, 2026.11: 70 sayfa. Üretimden sonra repodaki PDF SHA-256: `752abfa3…cb03` (2026.10), `3688ad34…7937` (2026.11), üretimden önceki değerlerle aynı | Evet |
| `./docs/rehber/dogrula.sh` | Çıkış 0. D1-D10 ve D12: 11 GEÇTİ, 0 KALDI. Çıktı satırları lead raporundakiyle birebir aynı (sayılar dahil: 312 atıf, 54 yasak kelime, 29 satır) | Evet |
| D11: `./docs/rehber/uret.sh <scratch>/ikinci` + `cmp` | `cmp` 2026.10 aynı, 2026.11 aynı (bayt eşitliği). Repo PDF'leri değişmedi | Evet |
| D-N (1): kopyada 2026.11 `gold-yillik-ucret` → `1.450 TL`, `uret.sh`, `dogrula.sh <o klasör>` | Çıkış **1**. D4 KALDI (Gold satırı), D8 KALDI (F1.1, F1.2, F1.3 ve sayım). 9 GEÇTİ, 2 KALDI | Evet |
| D-N (2), kendi eklediğim farklı mutasyon: kopyada Gold'u düzeltip 2026.11 `banka-karti-atm-gunluk` → `25.000 TL` | Çıkış **1**. D4 KALDI (8.4 ve 2.1 satırları), D8 KALDI (F2.1, F2.2). 9 GEÇTİ, 2 KALDI | Yeni kanıt |
| `./docs/rehber/dogrula.sh yok-klasor` ve repo köküne göre göreli `docs/rehber/pdf` | Çıkış 1 (hata verir, sessiz geçmez; bkz. O-2) | — |
| `git status --short` (başta ve sonda) | Yalnız `?? docs/rehber/`, `?? handoff/00…02`. `kaynaklar/`, servisler, `docs/api/`, kök `CLAUDE.md`, `rag.md` için çıktı boş | Evet |
| `./mvnw verify`, `npm run build/lint/test`, `test:e2e`, PIT, Stryker | Uygulanamaz (kod yok) | — |

Negatif denemelerin ikisi de repo dışındaki kopyada yapıldı; repodaki `surum.typ` değişmedi.

## Mutation testi
Kurulu değil (kod task'ı değil). Onun yerine D-N iki farklı mutasyonla (ücret, limit) tekrarlandı ve ikisini de yakaladı. Yakalanmayacak mutasyon sınıfı: rehberde listelenmeyen bir yerde olgunun değişmesi (bkz. O-1).

## Uçtan uca
Uygulanamaz (Playwright, çalışan sistem ve dil modeli yok; TASK-005 sonrası konu). Bunun yerine Q-1…Q-6:

| # | Sonuç |
|---|-------|
| Q-1 | ✅ Dosyalar yerinde, `uret.sh` ve `dogrula.sh` baştan çalıştı, 11/11 GEÇTİ, çıktı lead raporuyla aynı, `git status kaynaklar/` ve servis/`docs/api/` diff'i boş |
| Q-2 | ✅ 16 başlık PDF'te doğrulandı (K2). SSS'ler ≥ 4 soru (K4b) |
| Q-3 | ✅ 9 konu (kişisel bakiye, hesap hareketleri, kripto para, başka bankaların ürünleri, kişiye özel diğer bilgiler, ticari/kurumsal, kurumsal bilgiler, piyasa tahmini/tavsiye, BES ve sigorta). Her birinde 3 örnek soru, yasak kelimeler ve sabit cümle "Bu konuda rehberimizde bilgi bulamadım." var. Planda istenen `kripto`, `bitcoin`, `işletme kredisi`, `genel müdür` listede |
| Q-4 | ✅ 9 sayfa görsel kontrol (PNG, 2026.11): s. 1, 7, 8, 16, 35, 42, 58, 65, 69. Bu 9 sayfada tablo satırı bölünmemiş, başlık satırı gri zeminle var, kapakta iki not var, gerçek banka adı/logosu yok. Not: 2.2 tablosu s. 7'ye sığmayıp s. 8'in başına geçmiş (başlık ve giriş s. 7'de). Kozmetik, bkz. N-1. Tablolar sayfa geçişinde bölünmüyor (`tablo` yardımcısı satırı ölçüyor); tarife tabloları ayrı sayfalara tam sığıyor |
| Q-5 | ✅ 2.16 ve 4.10 PDF metni onay kartıyla karşılaştırıldı: kanal, kalıcı/geçici kapatma, 3/5 iş günü, 250 TL, tazminat tablosu ve örnek aynı |
| Q-6 | ✅ Lead raporunda "Durum: VERİLDİ (30 Ekim 2026)" var; onaydan sonra kaynak değişmediği beyan edilmiş. QA tarafında D1-D12 yeniden çalıştırıldı ve geçti |

### Rehber içeriğinde iç tutarlılık örneklemi (elle)
Aynı olgunun bölüm metni, SSS ve 10. bölüm tarifesindeki değerleri karşılaştırıldı (2026.11 metni). Hepsi tutarlı:
- Ücretler: Classic 650, Gold 1.650, Platinum 2.950, ek kart 750, kâğıt ekstre 4,90, yenileme 100, nakit avans %1,25 / min 15,00, başka banka ATM 21,75, yurt dışı %1,99 ve %1,5 / min 75, FAST 6,49 / 9,90, EFT 11,95, şube 74,50, Cepten Cebe 14,90, SWIFT 25 / 45 / 20 / 35 ABD doları, ekspertiz 9.850, tahsis binde 5, kiralık kasa 4 boyut + 2.000 teminat + 3.250 kilit, hisse on binde 15 / 2,50 TL, fiziki altın 15 TL.
- Oranlar ve süreler: aylık %3,49 (2.9, 4.3, SSS, 4.9, 4.11), nakit avans %4,25 (2.10, SSS, 4.6), mevduat %38,50 (3.2, 3.12, SSS), kumbara %34,00 / %35,50, TMSF 1.100.000 TL (3.11, SSS), teslim 3/5 iş günü (2.1, 2.16, 2.18, SSS), cayma 14 gün, itiraz 120 gün, süreç 45/90 gün, EFT 09.00-17.00, FAST limitleri (5.7, 6.6, SSS), 50.000 TL artırma (2.1, 8.4).
- Hesaplar elle yeniden hesaplandı: 50.000 × %3,49 × 10/30 = 581,67 TL; gecikme %3,49 × 1,3 = %4,54; 100.000 × %38,50 × 32/365 = 3.375,34 TL, stopaj %17,5 = 590,68 TL; kumbara 2.794,52 TL ve 489,04 TL; 100.000 TL / 24 ay / %3,49 taksit 6.220,74 TL (KKDF+BSMV ile ~6.924 TL). Hepsi doğru.
- Farklı stopaj oranları (mevduat %17,5 / %15 / %10, döviz %20, fon örneği %15) farklı ürünlere ait; çelişki yok.

## Kod review bulguları
`uret.sh`, `dogrula.sh`, `RehberDogrulama.java` (619 satır), `kontroller.txt`, `ortak.typ` incelendi.

| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional | `dogrulama/RehberDogrulama.java` (D8 l.290-325; D4 l.170-197) | D8 ve D4 yalnızca listelenen dizeleri arar. Bir olgu listede olmayan bir yerde yanlış yazılırsa (ör. tarifede Gold 1.650, ama yeni bir SSS'de 1.450 gibi) yalnızca `1.450 TL` toplam sayımı ve `kontroller.txt` yakalar. Genel bir "tarifedeki her değer bölüm metniyle aynı mı" kontrolü yok. Plan Riskler 1 bu riski zaten takip task'ına bırakmıştı; QA elle örneklemi yaptı ve tutarsızlık bulmadı (yukarıda) | lead (takip task'ı adayı) |
| Optional | `uret.sh:9-17`, `dogrula.sh:11,19` | Script `cd "$(dirname "$0")"` yaptığı için `<klasör>` argümanı göreli verilirse çağıranın dizinine değil `docs/rehber/`'e göre çözülür. README "repo kökünden çalışır" diyor. Hatalı yolda betik hata verip çıkış 1 döner (sessiz geçmez), bu yüzden engel değil; README'ye "mutlak yol verin" notu yeterli | lead |
| Optional | `kontroller.txt`, `RehberDogrulama.java:597-616` | `kontroller.txt` bölümü yanlış yazılırsa (ör. `[SATIR]` → `[SATIRR]`) D4/D3/D12 boş listeyle `GEÇTİ` döner; yalnızca D8/D10/D1 için boş-liste koruması var. Şu an dosya doğru ve D4 "29 satır" yazıyor | lead |
| Optional | `kontroller.txt:[GERCEK_BANKA]`, `rehber` metni | `Mastercard` kart şeması olarak 4 yerde geçiyor (2.1, 2.2, 2.14, 2.17). TASK Kural 1 "gerçek banka adı, logosu ya da kampanyası" diyor; Mastercard banka değil ve onay kartı/plan kapsamında bir karar yok. Kullanıcı tam metni onayladığı için engel değil; ileride istenirse "kart şeması" olarak genelleştirilir | lead / kullanıcı |
| Nit | `RehberDogrulama.java:182` | D4 hücre eşleşmesi alt dizi eşleşmesidir (`1.450 TL` ile `11.450 TL` da eşleşir). Mevcut kontrol dizeleri bağlam taşıdığı için pratik risk yok | lead |
| Nit | `rehber.typ`/2.2 | 2.2 tablosu sayfaya sığmayınca s. 8'in başına geçiyor, s. 7 altında boşluk kalıyor. Okunabilirliği bozmuyor | lead |
| FYI | `dogrula.sh:17-33` | PDFBox jar'ı ilk çalıştırmada indirilir ve SHA-256 ile doğrulanır; iyi. İnternetsiz ilk çalıştırma hata verir (README'de yazıyor) | — |
| FYI | `RehberDogrulama.java:137` | D3'te `Ğ` aranmıyor (plan satırında vardı). Gerekçe doğru: PDF metninde `Ğ` yok (0 eşleşme), Türkçede Ğ ile başlayan sözcük yok. Kontrol gücü pratikte azalmıyor | — |

Critical ve Required bulgu yok.

### Lead raporundaki "Plandan sapmalar" değerlendirmesi
1. **D3'te `Ğ` aranmıyor:** Kabul. Gerekçe kodda yazılı, kabul kriterini (Türkçe karakterler bozulmadan çıkar) zayıflatmıyor: diğer 12 harf (büyük İ dahil) aranıyor ve `ğ` küçük hâli var. Plan metninden bilinçli sapma olduğu için burada kayıt altında.
2. **Banka adları `kontroller.txt`'de, 4. konunun yasak ifadeleri nottaki listede:** Kabul. Plandaki ayrım korunmuş, D12 ve D10 çalışıyor.
3. **"Garanti" yerine "Garanti BBVA":** Kabul. Tek kelime "garanti" sıradan sözcük; yanlış alarmı önlüyor. QA metinde tam kelime `garanti` aradı: sonuç boş.
4. **İçindekiler 3 sayfa (kartta 2):** Kabul; toplam 70, aralık içinde, kart "~74, kabul 68-76" demişti.
5. **Toplam 70 (hedef 72):** Kabul, kabul aralığında.
6. **`--ignore-embedded-fonts`:** Kabul, tekrarlanabilirliği artırıyor; D11 bayt eşit.
7. **Tireli sözcükler bölünmüyor:** Kabul, D3 bunu doğruluyor (bölünmüş kelime 0).
8. **Sayfa sayısı `typst eval` ile:** Kabul; Typst 0.15.1 ile çalışıyor (QA da çalıştırdı).
9. **Metin çıkarma ayarı:** Kabul, README TASK-005 için not düşmüş.

Hiçbir sapma kabul kriterini zayıflatmıyor.

## Düzeltme
Gerekmiyor. (Optional bulgular önerilerdir; takip task'ı için bırakıldı: "Rehber olgu tutarlılığı taraması" ve `kontroller.txt` boş-liste koruması.)

STATUS: DONE
