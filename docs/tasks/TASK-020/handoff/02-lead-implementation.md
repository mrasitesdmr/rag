TASK: TASK-020
# Lead uygulama raporu: Rehber olgu tutarlılığı taraması

## Özet
- `dogrulama/RehberDogrulama.java`'ya iki kontrol eklendi:
  - **D13 (tarife ↔ bölüm metni):** Kural 1.
  - **D14 (SSS değeri ↔ atıf yapılan başlık):** Kural 2.
- `dogrulama/kontroller.txt`'ye `[TARIFE]` bölümü eklendi: 73 girdi, her sürümde 72 tarife satırı.
- Değerler her sürümün kendi PDF'inden okunur; katalogda ve Java'da olgu değeri yok.
- İlk çalıştırmada D14 tam olarak R1-R5'i buldu, D13 geçti. Plan dışı bulgu çıkmadı, bulgu kapısında kullanıcıya soru gerekmedi.
- Kart sorusu 1 = A uygulandı: 5 SSS cevabına ikinci atıf eklendi. Olgu değişmedi.
- İki PDF yeniden üretildi; `dogrula.sh` 13/13 GEÇTİ.
- Negatif denemeler D-N3…D-N7'nin hepsi KALDI verdi.
- Kullanıcı değişen 5 cevabı onayladı.

## L-1 Baz
- `git status` temizdi (yalnız `handoff/00-onay-karti.md` ve `handoff/01-plan.md` izlenmiyordu).
- Önceki PDF'lerin SHA-256'ları TASK-023 raporundakiyle aynı:
  - 2026.10: `7585c2abac6fceecb6af795cab2fa1f83159c29dd899fd03d5d2d6110caa827c`
  - 2026.11: `4db9c9e892a01c2f0f9c4513d687cf9fb18eadb3e84a2d3f9b9f01215584e20b`
- `dogrula.sh` 11/11 GEÇTİ. D9 haritası `{1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}`, plan Kararlar #12 ile aynı.
- Araçlar: Typst 0.15.1, Java 26.0.1, PDFBox 3.0.6.

## L-2 Kontroller (nasıl çalışıyor)
- **Satır yapısı:** Yeni bir `PDFTextStripper` alt sınıfı (`SatirToplayici`) her satırı ilk karakterinin yazı tipi boyutu, ağırlığı ve konumuyla toplar. Mevcut D1-D12'nin kullandığı sayfa metni değişmedi.
- **Satır türleri:**

  | Satır | Yazı tipi |
  |-------|-----------|
  | Bölüm başlığı | 20 pt Bold |
  | Numaralı başlık ve "Sık sorulanlar" | 13 pt Bold |
  | Ara başlık | 10 pt Bold |
  | Tablo başlık satırı | 9 pt Semibold |
  | Tablo satırı | 9 pt Regular |
  | Gövde | 11 pt Regular |
  | SSS sorusu | 11 pt Semibold |
- **Paragraf ve cümle:** Aynı sayfada iki gövde satırı arasındaki mesafe 17 pt'yi geçerse yeni paragraf başlar (satır aralığı ~15 pt, paragraf aralığı ~19 pt). "• ", "N. " ve "Örnek:" ile başlayan satır her zaman yeni paragraf başlatır. Paragraflar cümlelere bölünür (`. ! ?` + büyük harf). "Örnek:" ile başlayan paragraf D13'e girmez.
- **Değer belirteçleri (D13 ve D14 ortak):**
  - Türler: TL, ABD doları, euro, yüzde, binde, on binde, puan, süre ve saat (`ss.dd`).
  - Süre birimleri: dakika, saniye, saat, gün, iş günü, hafta, ay ve yıl.
  - Aralığın iki ucu ayrı belirteçtir. Sayılar normalleştirilir: `15,00 TL` = `15 TL`.
  - "bkz. X.Y" atıfları belirteç taramasından önce atılır. Adetler ve sorudaki sayılar karşılaştırılmaz.
- **D13:**
  - 10.1-10.7'deki her tablo satırı katalogdaki en uzun etiketle eşlenir; etiketi olmayan satır KALDI verir. Kalan kısım değerdir. Değer türü ücretsiz, tutar ya da metin olarak belirlenir.
  - Her kalıp eşleşmesinde pencere, eşleşmenin bittiği yerden cümlenin ya da tablo satırının sonuna kadar uzanır. Plan Kararlar #4(d)'deki kural uygulanır.
  - Aynı cümleye birden çok kalıp uyarsa cümle bir kez sayılır.
  - `yalniz-tarife` olmayan her girdi, satırının bulunduğu her sürümde en az bir doğrulanmış geçiş bulmalıdır.
  - İki sürümün tarifesinde de satırı olmayan katalog girdisi de KALDI verir.
- **D14:**
  - 2-9. bölümlerin "Sık sorulanlar" kısmında soru = yarı kalın satırlar, cevap = sonraki düz satırlar.
  - Cevaptaki her belirteç, atıf yapılan başlıkların (birleşim) belirteçleri içinde olmalıdır. Başlık metni tablolar ve örnekler dahildir.
  - Cevapsız soru ya da atıfsız cevap KALDI verir. PDF'teki soru sayısı kaynaktaki `#soru[` sayısıyla (105) aynı olmalıdır.
- **README:** "Kontroller" tablosuna D13 ve D14 satırları eklendi. D13/D14'ün değerleri nereden okuduğunu ve `[TARIFE]`'yi anlatan paragraf ile "Yazım kuralları"na iki madde eklendi.
- **dogrula.sh:** Yalnız 2. satırdaki yorum değişti: "(D1-D10, D12-D14)".

### Plandan sapmalar (qa incelesin)
1. **Pencere eşleşmenin sonundan başlıyor** (plan #4(c): "eşleşmeden"). Bazı etiketlerin kendisinde sayı var ("FAST, 6.000 TL'ye kadar"); pencere eşleşmenin başından başlasaydı bu sayı ilk değer sayılırdı. Bu yüzden kalıp ücretin konusunu tarif ediyor, pencere konudan sonraki metin. S2 (D-N3) bu yöntemle yakalanıyor.
2. **"ücret almaz / ücreti almaz" da ücretsiz sözcüğü sayılıyor** (plan: "ücretsiz/alınmaz"). Örnekler: 4.13 "ipotek kaldırma için ücret almaz", 5.5 "Gelen SWIFT için Raşit Bankası ücret almaz", 7.2 "fon alım satımında ayrıca işlem ücreti almaz" ve 3.8. Bu ekleme olmasaydı bu ücretlerin hiçbir geçişi doğrulanamazdı.
3. **"Metin" türü** (Taşıt rehin tesis ücreti: "Noter ve tescil maliyeti kadar"). Pencerede belirteç yoksa geçiş doğrulanmış sayılıyor; belirteç varsa çelişki. Plan yalnız çelişki kuralını yazıyordu; bu ek olmasaydı satır hiçbir zaman doğrulanamazdı.
4. **Paragraf başı kuralı.** "N. " ile başlayan numaralı adımlar ve "Örnek:" de paragraf başıdır. İlk denemede iki sorun çıktı: "5. … 6." adımları tek cümleye yapışıyordu, s. 35 başındaki "Örnek:" paragrafı da önceki sayfanın maddesine ekleniyordu.

### Kalıp ayarları (plan #10)
- **Erken ödeme tazminatı:** İki girdinin kalıbı 4.10 tablosunun "12 ay ve altı" ve "36 ay ve altı" satırlarıyla sınırlı. Karşılaştırma her türün ilk değerine bakıyor; "12 aydan uzun %1" satırı tarifedeki ilk yüzdeyle (%0,5) karşılaştırılıp yanlış alarm verirdi. Sonuç: tarife hücresindeki "üstü %1" ve "üstü %2" değerleri D13'te karşılaştırılmıyor. D4 bu satırları zaten birebir arıyor.
- **Değerin konudan önce geldiği cümleler:** Üç cümle konuya önden bakan bir kalıpla yakalandı:
  - 8.2 "Ayrıca 2.000 TL anahtar teminatı alınır" → `ayrıca(?= \S+ tl anahtar teminatı)`
  - 4. bölüm SSS "Konut kredisinde ayrıca 9.850 TL ekspertiz ücreti vardır" → `konut kredisinde ayrıca(?= \S+ tl ekspertiz ücreti)`
  - 4. bölüm SSS "kredilerinde yalnızca kredi tutarının binde 5'i kadar kredi tahsis ücreti" → `kredilerinde yalnızca`

  Değerin kendisi kalıpta yok.
- **Dijital hesap belgesi ve IBAN belgesi:** Bölümlerde yalnız 3.8'deki madde listesinde geçiyor; değer listenin giriş cümlesinde ("Şu hizmetler de ücretsizdir:"). Girdi bu cümleyi kullanıyor (`^şu hizmetler de`); tarifede ücret yazılırsa D13 bu cümleyle çelişki verir. `yalniz-tarife` yapılmadı (plan #10).
- **SMS ile kart harcama bildirimi:** Geçiş 6.7'de iki cümlede: "… SMS bildirim hizmetini açabilirsiniz. Hizmet ücretsizdir." Değer ikinci cümlede, bu yüzden kalıp genel: `^hizmet(?= )`. Bu kalıp 8.2'deki "Hizmet 48 şubemizde verilir" cümlesine de uyuyor ama orada değer yok (değersiz sayılıyor).
- **Değersiz kalan geçişler:** D13 bunları ne hata ne doğrulama sayar. İki sürümde de 15 eşleşme değersiz kaldı. Bunların ikisi (4. bölüm SSS'teki tahsis ücreti ve ekspertiz cümleleri) aynı cümlede başka bir kalıpla doğrulanıyor, yani tamamen değersiz kalan 13 cümle var:
  - Değer içermeyen cümleler: "Kredi tahsis ücreti iade edilir", "Ekspertiz ücreti, kredi kullandırılmasa da iade edilmez".
  - 5.6'daki saat tablosu satırı "EFT 09.00-17.00 Yapılmaz". Tarifedeki EFT satırında saat türü yok.
  - 8.5 "Kredi kartıyla nakit avans 21,75 TL ve nakit avans işlem ücreti". "Nakit avans işlem ücreti" kalıbından sonra değer yok; aynı satır "Başka banka ATM'sinden nakit avans" girdisinde doğrulanıyor.

### `yalniz-tarife` girdileri (5; plan listesiyle aynı)
- **Şubeden kâğıt hesap belgesi (belge başına):** 3.8'de yalnız "ücreti olan hizmetler … bkz. 10.2" diye anılıyor, değer yok.
- **Kâğıt hesap cüzdanı:** Bir önceki maddeyle aynı cümlede.
- **Devlet tahvili ve hazine bonosu alım satımı:** 7.6'da işlem ücreti için 10.6'ya atıf var, değer yok.
- **Eurobond alım satımı:** Bir önceki maddeyle aynı.
- **Şubeden nakit çekme ve yatırma:** Bölümlerde ücretiyle anılmıyor; 2.1'de yalnız limit uygulanmadığı yazıyor.

## L-3 İlk çalıştırma (içerik değişmeden)
D1-D12 ve D13 GEÇTİ. D14 KALDI; bulgular tam olarak R1-R5:

| # | Sürüm | Yer | Atfın göstermediği değer |
|---|-------|-----|--------------------------|
| R1 | ikisi | 2. bölüm SSS s.18 "Raşit Gold kartın yıllık üyelik ücreti ne kadar?" (bkz. 2.4) | 1.450 TL / 1.650 TL, 150.000 TL |
| R2 | ikisi | 2. bölüm SSS s.18 "Kart aidatından muaf olmak için …" (bkz. 2.4) | 60.000, 150.000, 400.000 TL |
| R3 | ikisi | 5. bölüm SSS s.44 "FAST ile en fazla …" (bkz. 5.3) | 09.00, 17.00 |
| R4 | ikisi | 6. bölüm SSS s.50 "Transfer limitimi artırdım …" (bkz. 6.6) | 10.000 TL |
| R5 | ikisi | 8. bölüm SSS s.61 "Kartım yanımda değil …" (bkz. 8.3) | 5.000 TL |

D13 ilk çalıştırmada (katalog ayarlarından sonra) çelişki bulmadı. Plan dışı bulgu yok; bulgu kapısında kullanıcıya soru gerekmedi.

## L-5 İçerik (kart sorusu 1 = A)
`git diff docs/rehber/kaynak` (5 satır, 4 dosya; yalnız atıflar):
```
+++ b/docs/rehber/kaynak/bolumler/02-kartlar.typ
-#soru[Raşit Gold kartın yıllık üyelik ücreti ne kadar?][Raşit Gold'un yıllık üyelik ücreti #S.gold-yillik-ucret'dir. Önceki 12 ayda kartla toplam 150.000 TL alışveriş yaptıysanız ücret alınmaz (#bkz(<yillik-ucret>)).]
+#soru[Raşit Gold kartın yıllık üyelik ücreti ne kadar?][Raşit Gold'un yıllık üyelik ücreti #S.gold-yillik-ucret'dir. Önceki 12 ayda kartla toplam 150.000 TL alışveriş yaptıysanız ücret alınmaz (#bkz(<kart-turleri>) ve #bkz(<yillik-ucret>)).]
-#soru[Kart aidatından muaf olmak için ne kadar harcamam gerekiyor?][Önceki 12 aydaki alışveriş toplamınız Raşit Classic'te 60.000 TL, Raşit Gold'da 150.000 TL, Raşit Platinum'da 400.000 TL'ye ulaşırsa o yıl yıllık üyelik ücreti alınmaz (#bkz(<yillik-ucret>)).]
+#soru[Kart aidatından muaf olmak için ne kadar harcamam gerekiyor?][Önceki 12 aydaki alışveriş toplamınız Raşit Classic'te 60.000 TL, Raşit Gold'da 150.000 TL, Raşit Platinum'da 400.000 TL'ye ulaşırsa o yıl yıllık üyelik ücreti alınmaz (#bkz(<kart-turleri>) ve #bkz(<yillik-ucret>)).]
+++ b/docs/rehber/kaynak/bolumler/05-para-transferi.typ
-#soru[FAST ile en fazla ne kadar gönderebilirim?][Tek seferde en fazla 100.000 TL. Daha yüksek tutarlar iş günlerinde 09.00-17.00 arasında EFT ile gönderilir (#bkz(<fast>)).]
+#soru[FAST ile en fazla ne kadar gönderebilirim?][Tek seferde en fazla 100.000 TL. Daha yüksek tutarlar iş günlerinde 09.00-17.00 arasında EFT ile gönderilir (#bkz(<eft>) ve #bkz(<fast>)).]
+++ b/docs/rehber/kaynak/bolumler/06-dijital-bankacilik.typ
-#soru[Transfer limitimi artırdım ama hâlâ işlem yapamıyorum.][Limit artırma güvenlik nedeniyle 24 saat sonra geçerli olur. Yeni cihaz eşlediyseniz 24 saatlik 10.000 TL sınırı da uygulanıyor olabilir (#bkz(<dijital-limit>)).]
+#soru[Transfer limitimi artırdım ama hâlâ işlem yapamıyorum.][Limit artırma güvenlik nedeniyle 24 saat sonra geçerli olur. Yeni cihaz eşlediyseniz 24 saatlik 10.000 TL sınırı da uygulanıyor olabilir (#bkz(<cihaz>) ve #bkz(<dijital-limit>)).]
+++ b/docs/rehber/kaynak/bolumler/08-sube-atm-musteri-hizmetleri.typ
-#soru[Kartım yanımda değil. ATM'den para çekebilir miyim?][Evet. Raşit Mobil'den alacağınız 6 haneli kodla ya da ATM ekranındaki karekodu okutarak işlem başına en fazla 5.000 TL çekebilirsiniz (#bkz(<atm>)).]
+#soru[Kartım yanımda değil. ATM'den para çekebilir miyim?][Evet. Raşit Mobil'den alacağınız 6 haneli kodla ya da ATM ekranındaki karekodu okutarak işlem başına en fazla 5.000 TL çekebilirsiniz (#bkz(<atm>) ve #bkz(<atm-limit>)).]
```

## L-6 Üretim ve doğrulama
`./docs/rehber/uret.sh`:
```
pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf: 70 sayfa
pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf: 70 sayfa
```
`./docs/rehber/dogrula.sh` (çıkış kodu 0):
```
D1 GEÇTİ · Sayfa sayısı · 2026.10=70, 2026.11=70, onaylanan aralık 68-76
D2 GEÇTİ · Metin seçilebilir · 2026.10: kapak 353, en kısa iç sayfa 622 karakter, görüntü 0; 2026.11: kapak 354, en kısa iç sayfa 623 karakter, görüntü 0; 
D3 GEÇTİ · Türkçe karakterler · 12 harf (çğıİöşüÇÖŞÜâ) var, bozuk karakter 0, bölünmüş kelime 0, 12 kontrol cümlesi bulundu
D4 GEÇTİ · Tablo satırları · 29 satır kontrolü tek satırda ve sütun sırasıyla bulundu
D5 GEÇTİ · Üst ve alt bilgi · her iki PDF'te 2. sayfadan itibaren her sayfada doğru; kapakta yok
D6 GEÇTİ · İçindekiler ve yer imleri · 2026.10: 110 numaralı başlık (11 bölüm); 2026.11: 109 numaralı başlık (11 bölüm); sayfa numaraları ve yer imleri eşleşiyor
D7 GEÇTİ · Çapraz atıflar · 2026.10: 317 atıf, 95 farklı başlık; 2026.11: 317 atıf, 95 farklı başlık; hepsi var olan başlıkları gösteriyor
D8 GEÇTİ · Değişiklik özeti · 15 satır yazılan sayfada bulundu, eski değerler 2026.11'de yok; '1.450 TL' 3 kez, '1.650 TL' 3 kez (yalnızca Gold ücreti)
D9 GEÇTİ · Sayfa düzeni · bölüm başlangıçları aynı {1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}; değişmeyen bölümler [3, 4, 5, 7, 9, 11] (37 sayfa) birebir aynı
D10 GEÇTİ · Rehberde olmayan konular · 54 yasak kelime iki PDF'te de geçmiyor
D12 GEÇTİ · Kurgu sınırı · kapak notları var; 35 gerçek banka adı geçmiyor; adresler [www.rasitbank.example, musteri@rasitbank.example, kep@rasitbank.example]
D13 GEÇTİ · Tarife tutarlılığı · 2026.10: 72 tarife satırı, 67'i bölümlerde 100 yerde doğrulandı, 5'i yalnız tarifede; 2026.11: 72 tarife satırı, 67'i bölümlerde 100 yerde doğrulandı, 5'i yalnız tarifede; 
D14 GEÇTİ · Sık sorulanlar ve atıflar · 2026.10: 105 soru, 126 değer; 2026.11: 105 soru, 126 değer; her değer atıf yapılan başlıkta yazılı

Toplam: 13 GEÇTİ, 0 KALDI
```
- **D8:** Değişiklik özetinin sayfa numaraları değişmedi; `degisiklik-ozeti-2026.11.md`'ye dokunulmadı.
- **D7:** Her sürümde 317 atıf (312 + 5), plandaki beklentiyle aynı.
- **D11:** İkinci üretim `uret.sh <scratch>/ikinci` ile yapıldı. `cmp` iki dosyada da aynı sonucunu verdi.
- **Yeni SHA-256'lar:**
  - 2026.10: `f5651f12eec03ae28bd4470d400d62fa9c421229afcf7eb7d68ba8c04d70199d`
  - 2026.11: `9dbd765fa74ed151af6ad1579bec499b322121b920baf0f84410b89e377f0d32`
- **K-1:** Sayfa sayısı 70, bölüm başlangıçları baz değerle aynı. Önceki PDF'lerle `pdftotext -layout` üzerinden sayfa sayfa karşılaştırıldı. İki sürümde de yalnız s. 18, 44, 50 ve 61 farklı; bunlar değişen beş cevabın sayfaları. Diğer 66 sayfa aynı.
- **K-2:** Kaynak diff'i yalnız onaylı 5 satır.

## L-7 Negatif denemeler
Her deneme `docs/rehber`'in scratch kopyasında yapıldı (`cp -R`, değişiklik, kaynak değiştiyse kopyada `uret.sh`, sonra kopyada `dogrula.sh`). Repo dosyaları değişmedi. Aşağıda yalnız KALDI satırları var; diğer kontroller GEÇTİ.

**D-N3** (S2): 2. bölüm SSS "Nakit avans faizi ne kadar?" cevabında `Ayrıca çekilen tutarın %1,25'i, en az 15,00 TL işlem ücreti alınır` → `Ayrıca nakit avans işlem ücreti %1,5'tir`
```
D13 KALDI · Tarife tutarlılığı · 2026.10 çelişki: 'Nakit avans işlem ücreti' 10.1 s.65 'Tutarın %1,25'i, en az 15,00 TL' ↔ 2. bölüm Sık sorulanlar s.19 'Ayrıca nakit avans işlem ücreti %1,5'tir (bkz. 2.10).' (bölümde %1,5, tarifede %1,25); 2026.11 çelişki: 'Nakit avans işlem ücreti' 10.1 s.65 'Tutarın %1,25'i, en az 15,00 TL' ↔ 2. bölüm Sık sorulanlar s.19 'Ayrıca nakit avans işlem ücreti %1,5'tir (bkz. 2.10).' (bölümde %1,5, tarifede %1,25)
D14 KALDI · Sık sorulanlar ve atıflar · 2026.10 2. bölüm Sık sorulanlar s.19 'Nakit avans faizi ne kadar?': '%1,5' atıf yapılan başlıkta (bkz. 2.10) yok; oradaki aynı türden değerler: %50, %1,25, %4,25; 2026.11 2. bölüm Sık sorulanlar s.19 'Nakit avans faizi ne kadar?': '%1,5' atıf yapılan başlıkta (bkz. 2.10) yok; oradaki aynı türden değerler: %50, %1,25, %4,25
çıkış kodu: 1
```
**D-N4** (sürüm farkı): Gold SSS cevabında `#S.gold-yillik-ucret` → `1.450 TL`. 2026.10'da D13/D14 bulgusu yok; yalnız 2026.11 kalıyor.
```
D8 KALDI · Değişiklik özeti · F1.2 eski metin 2026.11 içinde hâlâ var: Raşit Gold'un yıllık üyelik ücreti 1.450 TL'dir; F1.2 2026.11 s.18 üzerinde yok: Raşit Gold'un yıllık üyelik ücreti 1.650 TL'dir; 2026.11 '1.450 TL' 1 kez geçiyor, beklenen 0; 2026.11 '1.650 TL' 2 kez geçiyor, beklenen 3
D13 KALDI · Tarife tutarlılığı · 2026.11 çelişki: 'Raşit Gold yıllık üyelik ücreti' 10.1 s.65 '1.650 TL' ↔ 2. bölüm Sık sorulanlar s.18 'Raşit Gold'un yıllık üyelik ücreti 1.450 TL'dir.' (bölümde 1.450 TL, tarifede 1.650 TL)
D14 KALDI · Sık sorulanlar ve atıflar · 2026.11 2. bölüm Sık sorulanlar s.18 'Raşit Gold kartın yıllık üyelik ücreti ne kadar?': '1.450 TL' atıf yapılan başlıkta (bkz. 2.2, 2.4) yok; oradaki aynı türden değerler: 650 TL, 60.000 TL, 1.650 TL, 150.000 TL, 2.950 TL, 400.000 TL, 5.000 TL, 25.000 TL, 40.000 TL, 100.000 TL, 120.000 TL, 20.000 TL
çıkış kodu: 1
```
**D-N5** (girdisi olmayan tarife satırı): 10.1'e `("Kart aidatı gecikme ücreti", "50 TL")` eklendi.
```
D13 KALDI · Tarife tutarlılığı · 2026.10 kontroller.txt [TARIFE] içinde girdisi olmayan tarife satırı: 10.1 s.65 'Kart aidatı gecikme ücreti 50 TL'; 2026.11 kontroller.txt [TARIFE] içinde girdisi olmayan tarife satırı: 10.1 s.65 'Kart aidatı gecikme ücreti 50 TL'
çıkış kodu: 1
```
**D-N6** (tarife değeri tek yerde değişti): 10.1 `Kâğıt ekstre (gönderim başına)` → `5,90 TL`
```
D4 KALDI · Tablo satırları · 2026.10 tek satırda yok: Kâğıt ekstre (gönderim başına) | 4,90 TL; 2026.11 tek satırda yok: Kâğıt ekstre (gönderim başına) | 4,90 TL
D13 KALDI · Tarife tutarlılığı · 2026.10 çelişki: 'Kâğıt ekstre (gönderim başına)' 10.1 s.65 '5,90 TL' ↔ 2.7 s.10 '• Kâğıt ekstre isteğe bağlıdır ve her gönderim için 4,90 TL posta ücreti alınır.' (bölümde 4,90 TL, tarifede 5,90 TL); 2026.10 çelişki: 'Kâğıt ekstre (gönderim başına)' 10.1 s.65 '5,90 TL' ↔ 2. bölüm Sık sorulanlar s.19 'Her gönderim için 4,90 TL posta ücreti alınır.' (bölümde 4,90 TL, tarifede 5,90 TL); 2026.10 bölümlerde doğrulanan geçiş bulunamadı: 'Kâğıt ekstre (gönderim başına)' (10.1 s.65); 2026.11 çelişki: 'Kâğıt ekstre (gönderim başına)' 10.1 s.65 '5,90 TL' ↔ 2.7 s.11 '• Kâğıt ekstre isteğe bağlıdır ve her gönderim için 4,90 TL posta ücreti alınır.' (bölümde 4,90 TL, tarifede 5,90 TL); 2026.11 çelişki: 'Kâğıt ekstre (gönderim başına)' 10.1 s.65 '5,90 TL' ↔ 2. bölüm Sık sorulanlar s.19 'Her gönderim için 4,90 TL posta ücreti alınır.' (bölümde 4,90 TL, tarifede 5,90 TL); 2026.11 bölümlerde doğrulanan geçiş bulunamadı: 'Kâğıt ekstre (gönderim başına)' (10.1 s.65)
çıkış kodu: 1
```
**D-N7** (hiçbir yeri bulmayan kalıp): kopyadaki `kontroller.txt`'de kâğıt ekstre girdisi `kâğıt ekstre ücreti bu cümlede yok` yapıldı.
```
D13 KALDI · Tarife tutarlılığı · 2026.10 bölümlerde doğrulanan geçiş bulunamadı: 'Kâğıt ekstre (gönderim başına)' (10.1 s.65); 2026.11 bölümlerde doğrulanan geçiş bulunamadı: 'Kâğıt ekstre (gönderim başına)' (10.1 s.65)
çıkış kodu: 1
```

## L-8 Kullanıcı içerik onayı
- Değişen 5 cevap PDF'ten çıkarılmış hâliyle ve sayfa numaralarıyla (iki sürümde s. 18, 18, 44, 50, 61) kullanıcıya gösterildi.
- Kullanıcı 2026-10-30'da "Onaylıyorum" dedi.
- Kart sorusu 1 kararı (A) da kullanıcının (kartın "Kullanıcı kararı" bölümü).

## Değişen dosyalar
- `docs/rehber/dogrulama/RehberDogrulama.java` (D13, D14, satır toplayıcı, belirteçler)
- `docs/rehber/dogrulama/kontroller.txt` (`[TARIFE]`, 73 girdi)
- `docs/rehber/README.md`, `docs/rehber/dogrula.sh` (2. satır)
- `docs/rehber/kaynak/bolumler/02-kartlar.typ`, `05-para-transferi.typ`, `06-dijital-bankacilik.typ`, `08-sube-atm-musteri-hizmetleri.typ` (yalnız onaylı SSS satırları)
- `docs/rehber/pdf/*.pdf` (iki PDF)

Değişmeyenler: `surum.typ`, `ortak.typ`, `rehber.typ`, `uret.sh`, `degisiklik-ozeti-2026.11.md`, `rehberde-olmayan-konular.md`, `kaynaklar/**`, servisler, `docs/api/**`.

## Bilinen sınırlar (TASK-021'e not)
- **Değerin konudan önce geldiği cümleler:** Kalıp önden bakacak biçimde yazılmadıkça karşılaştırılmaz (yukarıdaki "değersiz" geçişler).
- **Aynı türden birden çok değer:** Tarife hücresinde aynı türden birden çok değer varsa yalnız ilki karşılaştırılır ("üstü %1").
- **Listeler:** Değeri giriş cümlesinde olan madde listelerinde yalnız giriş cümlesi kullanılabilir.

STATUS: DONE
