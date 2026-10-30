# Raşit Bankası Ürün ve Hizmet Rehberi

Asistanın cevaplarını dayandırdığı belge. Banka kurgusaldır; ücret, faiz, limit ve süreler gerçekçi ama kurgusaldır. İki sürümü vardır ve ikisi de aynı kaynaktan üretilir:

| Sürüm | Yürürlük | Kullanım | PDF |
|-------|----------|----------|-----|
| 2026.10 | 1 Ekim 2026 | Bilgi bankasına ilk içe alınan sürüm (TASK-005) | `pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf` |
| 2026.11 | 1 Kasım 2026 | Bilgi kaynakları ekranında "Yeni sürüm yükle" ile işlenen sürüm (TASK-015) | `pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf` |

İki sürüm arasındaki farklar: `degisiklik-ozeti-2026.11.md`. Rehberde bilerek bulunmayan konular ("Bu konuda rehberimizde bilgi bulamadım." cevabı beklenenler): `rehberde-olmayan-konular.md`.

## Klasör yapısı
| Yol | İçerik |
|-----|--------|
| `kaynak/rehber.typ` | Ana dosya: sayfa düzeni, üst ve alt bilgi, kapak, içindekiler; bölümleri sırayla içe alır |
| `kaynak/surum.typ` | Sürüme bağlı bütün değerler (tek sözlük). Bölüm dosyalarında sürüm adı geçmez |
| `kaynak/ortak.typ` | Bölümlerin ortak yardımcıları: tablo, ara başlık, atıf (`bkz`), sık sorulanlar |
| `kaynak/bolumler/*.typ` | 11 bölüm, bölüm başına bir dosya |
| `kaynak/fontlar/` | Source Sans 3 (3.052R; Regular, Semibold, Bold) ve lisansı `OFL.txt` (SIL Open Font License 1.1) |
| `pdf/` | Üretilmiş iki PDF (repoya girer; içe alma ve yükleme bunları yeniden üretmeden kullanır) |
| `uret.sh` | İki PDF'i üretir |
| `dogrula.sh`, `dogrulama/` | PDF'leri PDFBox ile okuyup kontrol eder |

## Kurulum
- **Typst 0.15.1:** `brew install typst`. Başka bir sürümle de üretilebilir; ancak `uret.sh` uyarı verir ve PDF'ler repodakilerle bayt bayt aynı çıkmayabilir.
- **JDK 21 ya da üstü:** yalnızca `dogrula.sh` için (tek dosyalık Java programı, derleme adımı yok). Geliştirmede JDK 26 kullanıldı.
- **Apache PDFBox 3.0.6:** `dogrula.sh` ilk çalıştırmada `pdfbox-app-3.0.6.jar` dosyasını Maven Central'dan `~/.cache/rasit-rehber/` altına indirir ve SHA-256 değerini doğrular. Jar repoya girmez.

Üretim internet gerektirmez. Yazı tipleri repodan okunur; sistem yazı tipleri ve Typst'in gömülü yazı tipleri kullanılmaz (`--ignore-system-fonts --ignore-embedded-fonts`).

## Komutlar
Repo kökünden ya da bu klasörden çalışır.

```bash
./docs/rehber/uret.sh      # iki PDF'i pdf/ altına üretir ve sayfa sayılarını yazar
./docs/rehber/dogrula.sh   # kontrolleri çalıştırır; her kontrol için GEÇTİ ya da KALDI, kalan varsa çıkış kodu 1
```

- `uret.sh <klasör>` PDF'leri başka bir klasöre üretir. `dogrula.sh <klasör>` o klasördeki PDF'leri kontrol eder; örneğin kaynağı bilerek bozulmuş bir kopyayla negatif deneme yapmak için.
- PDF'in oluşturma tarihi sürümün yürürlük tarihine sabitlenir (`--creation-timestamp`). Böylece aynı kaynaktan iki kez üretilen PDF'ler bayt bayt aynıdır.

### Kontroller (`dogrula.sh`)
| Kod | Kontrol |
|-----|---------|
| D1 | İki PDF'in sayfa sayısı eşit ve onaylanan aralıkta (68-76) |
| D2 | Her sayfada seçilebilir metin var (kapak en az 80, diğerleri en az 300 karakter); görüntü nesnesi yok |
| D3 | Türkçe harfler bozulmadan çıkıyor; bitişik harf ve bölünmüş kelime yok; kontrol cümleleri birebir bulunuyor |
| D4 | Tablo satırları tek metin satırı olarak ve sütun sırasıyla çıkıyor |
| D5 | Kapak dışındaki her sayfada üst ve alt bilgi doğru; sayfa numarası PDF'teki sırayla aynı |
| D6 | İçindekiler'deki sayfa numaraları ve PDF yer imleri başlıkların gerçek sayfalarıyla aynı |
| D7 | Her "bkz. X.Y" atfı var olan bir başlığı gösteriyor |
| D8 | Değişiklik özetindeki her fark yazılan sayfada; eski değerler yeni sürümde yok |
| D9 | Bölüm başlangıç sayfaları iki sürümde aynı; değişmeyen bölümlerin sayfaları birebir aynı |
| D10 | "Rehberde olmayan konular"ın yasak kelimeleri iki PDF'te de geçmiyor |
| D12 | Kapak notları var; gerçek banka ve kart şeması adı geçmiyor; adresler yalnızca `rasitbank.example` alan adında |
| D13 | Ücret tarifesindeki (10.1-10.7) her satırın değeri, 1-9. bölümlerde o ücretin anıldığı her cümle ve tablo satırında aynı; her satırın `kontroller.txt` `[TARIFE]` girdisi var ve "yalnız tarifede" işaretli değilse bölümlerde en az bir yerde doğrulanıyor |
| D14 | Her "Sık sorulanlar" cevabındaki tutar, oran, süre ve saat, cevabın "bkz." ile gösterdiği başlıklardan birinde yazılı; PDF'teki soru sayısı kaynaktaki `#soru` sayısıyla aynı |

Beklenen dizeler `dogrulama/kontroller.txt` dosyasındadır. D8 beklentilerini `degisiklik-ozeti-2026.11.md`, D10 yasak kelimelerini `rehberde-olmayan-konular.md` dosyasından okur.

D13 ve D14 hiçbir ücret ya da oranı listeden okumaz; değerleri her sürümün kendi PDF'inden alır, böylece sürüme göre değişen değerler yalnızca `kaynak/surum.typ`'de kalır. Karşılaştırılan değerler: TL tutarı, ABD doları ve euro, yüzde, binde ve on binde, puan, süre (dakika, saat, gün, iş günü, hafta, ay, yıl) ve saat (`09.00`). Adetler ("ayda 4 kez", "en fazla 2 cihaz") ve sorudaki sayılar karşılaştırılmaz. D13'ün tarife satırlarını bölümlerde bulduğu kalıplar `kontroller.txt` dosyasının `[TARIFE]` bölümündedir; biçimi ve kuralları bölümün başında yazar. Tarifeye yeni satır eklenirse oraya da bir girdi eklenir, yoksa D13 KALDI der.

## İçe alma için notlar (TASK-005)
- **Metin çıkarma:** `PDFTextStripper` varsayılan ayarlarıyla (`setSortByPosition(false)`) kullanıldı. Bu ayarla her sayfanın ilk satırı üst bilgi, son satırı alt bilgi olur ve tablo satırları tek satır olarak gelir.
- **Üst bilgi** (kapak hariç her sayfanın ilk satırı): `Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm 2026.10`
- **Alt bilgi** (kapak hariç her sayfanın son satırı): `Yürürlük: 1 Ekim 2026 · Sayfa 7 / 70`. 2026.11'de `Sürüm 2026.11` ve `Yürürlük: 1 Kasım 2026` yazar.
- **Sayfa numarası:** Kapak 1. sayfadır. Alt bilgideki numara PDF'teki sayfa sırasıyla aynıdır.
- **Başlıklar:** Bölüm başlığı `2 Kartlar`, alt başlık `2.16 Kayıp ya da çalıntı bildirimi` biçiminde kendi satırındadır. Her bölüm yeni sayfada başlar. Her bölümün sonundaki "Sık sorulanlar" başlığı numarasızdır.

## Yazım kuralları
- Tutarlar `1.450 TL` (binlik nokta, para birimi sonda), oranlar `%3,25`, tarihler `1 Kasım 2026`, saatler `09.00-17.00`, süreler "3 iş günü" biçimindedir.
- Kesme işareti düz yazılır (`Raşit Mobil'de`); Typst'in akıllı tırnağı kapalıdır.
- Gerçek kart şeması markası (Mastercard, Visa, American Express, Troy vb.) yazılmaz; yerine "uluslararası kart altyapısı", "uluslararası kart logosu", "uluslararası kurallar" ifadeleri kullanılır. D12 bu adları arar.
- Kelime bölme ve bitişik harfler kapalıdır. Tireli sözcükler ve aralıklar (`e-posta`, `5.000-20.000 TL`) satır sonunda bölünmez.
- "Sık sorulanlar" cevabındaki her tutar, oran, süre ve saat, cevabın atıf yaptığı başlıklardan birinde yazılıdır. Değer başka bir başlıktaysa o başlık da atfa eklenir: `(bkz. 2.2 ve bkz. 2.4)`. D14 bunu kontrol eder.
- Bir ücret tarifede (10. bölüm) ve bölüm metninde aynı yazılır. Yeni tarife satırı için `dogrulama/kontroller.txt` `[TARIFE]` bölümüne bir girdi eklenir. D13 bunu kontrol eder.
- Tablo hücreleri tek satırdır. Bir satır sayfaya sığmazsa derleme hata verir (`kaynak/ortak.typ`, `tablo`).
- Sürüme göre değişen her değer yalnızca `kaynak/surum.typ` içinde durur. Yeni bir fark eklenirse değişiklik özetinin "Ayrıntı" tablosuna her geçişi için bir satır eklenir ve `dogrula.sh` çalıştırılır.
- Değişen bölümler (1, 2, 6, 8, 10) iki sürümde aynı sayfa sayısını korumalıdır; D9 bunu kontrol eder.
