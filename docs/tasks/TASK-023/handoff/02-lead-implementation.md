TASK: TASK-023
# Lead uygulama raporu: rehberden gerçek kart şeması adının kaldırılması

## L-1 Ön koşul ve baz
- TASK-005 kapanmamış: `docs/tasks/TASK-005/` altında yalnız `TASK.md` var, `handoff/` yok; `git log` içinde TASK-005 commit'i 0.
- Önceki PDF'ler `git show HEAD:docs/rehber/pdf/<dosya>` ile scratch'e alındı. SHA-256'lar TASK-004 raporundakiyle aynı:
  - 2026.10: `752abfa3315ab8bd4904ec20076874b2c60095ac841c2457189fc63fa295cb03`
  - 2026.11: `3688ad3419dc8cf723fec3e13a12777c5a841e0a8b39508242e8ee8d0f755937`
- `grep -n Mastercard 02-kartlar.typ`: tam 4 satır (6, 38, 298, 369).

## Plandan sapma: 2.17 ifadesi
- Onaylanan A ifadesiyle ("…bankasından uluslararası kart kuralları çerçevesinde belge ister.") ilk üretimde 2026.11 sürümü **71 sayfa** çıktı. `dogrula.sh`'de D1, D8 ve D9 KALDI; 3. bölümden itibaren bütün bölümler bir sayfa kaydı.
- **Neden:**
  - 2.17 cümlesi bir satır uzadı.
  - 2026.10'da s. 19'da boşluk vardı (39 satır), uzayan satır oraya sığdı.
  - 2026.11'de s. 19 doluydu (45 satır), 2. bölümün son 3 satırı s. 20'ye taştı.
- **Yapılan:** L-5 gereği lead seçilen ifadeyi kendisi değiştirmedi. Scratch kopyalarda varyantlar denendi:

| Varyant | 2026.10 | 2026.11 | dogrula.sh |
|---------|---------|---------|------------|
| 2.14'ü kısaltmak ("uluslararası kart logosu bulunan") | 70 | 71 | — |
| 2.17 "uluslararası kurallar çerçevesinde" | 70 | 70 | 11/11 GEÇTİ |
| 2.17 "kart kuralları çerçevesinde" | 70 | 70 | 11/11 GEÇTİ |
| 2.17 B ("bankasından belge ister") | 70 | 70 | 11/11 GEÇTİ |

- **Karar:** Kullanıcı 2.17 için "uluslararası kurallar çerçevesinde" ifadesini seçti (2026-10-30). Karar onay kartının "Kullanıcı kararı" bölümüne yazıldı. 2.1, 2.2 ve 2.14 A olarak kaldı.
- **Yan etki:** README yazım kuralındaki ifade listesi buna göre yazıldı.

## M1-M4: eski ve yeni cümle (PDF'ten çıkarılmış, PDFBox 3.0.6)
| Yer | Sayfa (2026.10 / 2026.11) | Eski | Yeni |
|-----|---------------------------|------|------|
| M1 · 2.1 | 7 / 7 | Kart Mastercard altyapısıyla çalışır, temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir. | Kart uluslararası kart altyapısıyla çalışır, temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir. |
| M2 · 2.2 | 7 / 7 | Raşit Bankası'nın bireysel kredi kartlarının hepsi Mastercard altyapısıyla çalışır, temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (bkz. 2.11). | Raşit Bankası'nın bireysel kredi kartlarının hepsi uluslararası kart altyapısıyla çalışır, temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (bkz. 2.11). |
| M3 · 2.14 | 15 / 15 | Kartlarınız yurt dışında Mastercard logosu bulunan her yerde kullanılabilir. | Kartlarınız yurt dışında, kartınızın üzerindeki uluslararası kart logosunun bulunduğu her yerde kullanılabilir. |
| M4 · 2.17 | 17 / 17 | Raşit Bankası, işlemi yapan işyerinin bankasından Mastercard kuralları çerçevesinde belge ister. | Raşit Bankası, işlemi yapan işyerinin bankasından uluslararası kurallar çerçevesinde belge ister. |

## Değişen dosyalar
```
 M docs/rehber/README.md                                    (D12 satırı + yazım kuralı maddesi)
 M docs/rehber/dogrulama/kontroller.txt                     ([GERCEK_BANKA] yorumu + 11 satır, 24 → 35)
 M docs/rehber/kaynak/bolumler/02-kartlar.typ               (4 satır: 4 ekleme, 4 silme)
 M docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf
 M docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf
```
- `degisiklik-ozeti-2026.11.md` değişmedi: D8 sayfa kaymadan GEÇTİ.
- Değişmeyenler: `RehberDogrulama.java`, `uret.sh`, `dogrula.sh`, `surum.typ`, `rehberde-olmayan-konular.md`, `kaynaklar/**`, servisler, `docs/api/**`.
- `git diff --numstat docs/rehber/kaynak` → `4 4 docs/rehber/kaynak/bolumler/02-kartlar.typ` (K-2).

`kontroller.txt`'ye eklenen satırlar: `Mastercard`, `Visa`, `VISA`, `American Express`, `AMERICAN EXPRESS`, `Amex`, `Troy`, `Maestro`, `UnionPay`, `JCB`, `Diners Club`.

## L-4 Üretim
```
$ ./docs/rehber/uret.sh
pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf: 70 sayfa
pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf: 70 sayfa
```
**D11:** İkinci üretim scratch'e yapıldı (`uret.sh <scratch>/ikinci`). `cmp` iki dosyada da aynı sonucunu verdi. Yeni SHA-256'lar:
- 2026.10: `7585c2abac6fceecb6af795cab2fa1f83159c29dd899fd03d5d2d6110caa827c`
- 2026.11: `4db9c9e892a01c2f0f9c4513d687cf9fb18eadb3e84a2d3f9b9f01215584e20b`

## L-5 Doğrulama
```
$ ./docs/rehber/dogrula.sh
D1 GEÇTİ · Sayfa sayısı · 2026.10=70, 2026.11=70, onaylanan aralık 68-76
D2 GEÇTİ · Metin seçilebilir · 2026.10: kapak 353, en kısa iç sayfa 622 karakter, görüntü 0; 2026.11: kapak 354, en kısa iç sayfa 623 karakter, görüntü 0;
D3 GEÇTİ · Türkçe karakterler · 12 harf (çğıİöşüÇÖŞÜâ) var, bozuk karakter 0, bölünmüş kelime 0, 12 kontrol cümlesi bulundu
D4 GEÇTİ · Tablo satırları · 29 satır kontrolü tek satırda ve sütun sırasıyla bulundu
D5 GEÇTİ · Üst ve alt bilgi · her iki PDF'te 2. sayfadan itibaren her sayfada doğru; kapakta yok
D6 GEÇTİ · İçindekiler ve yer imleri · 2026.10: 110 numaralı başlık (11 bölüm); 2026.11: 109 numaralı başlık (11 bölüm); sayfa numaraları ve yer imleri eşleşiyor
D7 GEÇTİ · Çapraz atıflar · 2026.10: 312 atıf, 95 farklı başlık; 2026.11: 312 atıf, 95 farklı başlık; hepsi var olan başlıkları gösteriyor
D8 GEÇTİ · Değişiklik özeti · 15 satır yazılan sayfada bulundu, eski değerler 2026.11'de yok; '1.450 TL' 3 kez, '1.650 TL' 3 kez (yalnızca Gold ücreti)
D9 GEÇTİ · Sayfa düzeni · bölüm başlangıçları aynı {1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}; değişmeyen bölümler [3, 4, 5, 7, 9, 11] (37 sayfa) birebir aynı
D10 GEÇTİ · Rehberde olmayan konular · 54 yasak kelime iki PDF'te de geçmiyor
D12 GEÇTİ · Kurgu sınırı · kapak notları var; 35 gerçek banka adı geçmiyor; adresler [www.rasitbank.example, musteri@rasitbank.example, kep@rasitbank.example]

Toplam: 11 GEÇTİ, 0 KALDI
```
D9'daki başlangıç haritası, plan Kararlar #6'daki TASK-004 baz değeriyle aynı.

### K-1 Önceki PDF'lerle sayfa sayfa karşılaştırma
Metin, PDFBox 3.0.6 `PDFTextStripper` (varsayılan ayar) ile sayfa sayfa çıkarıldı. Bunun için scratch'te tek seferlik bir Java programı ve karşılaştırma betiği kullanıldı; ikisi de repoya girmedi.

| Sürüm | Sayfa (eski/yeni) | Birebir aynı | Farklı sayfalar | s. 1-6 ve 20-70'te fark | s. 7-19 gövdesi |
|-------|-------------------|--------------|-----------------|-------------------------|------------------|
| 2026.10 | 70 / 70 | 67 | 7, 15, 17 (üst ve alt bilgi aynı) | yok | Eski metne 4 değişim uygulanınca yeni metne eşit: **True** |
| 2026.11 | 70 / 70 | 67 | 7, 15, 17 (üst ve alt bilgi aynı) | yok | Eski metne 4 değişim uygulanınca yeni metne eşit: **True** |

İçindekiler (s. 2-4) birebir aynı; hiçbir 2.x başlığı sayfa değiştirmedi.

### K-3 Büyük/küçük harf duyarsız alt dize taraması
Aranan dizeler: `mastercard`, `visa`, `american express`, `amex`, `troy`, `maestro`, `unionpay`, `jcb`, `diners`. Tarama iki yerde yapıldı:
- **Typst kaynağı (`docs/rehber/kaynak/`):** 0 eşleşme.
- **İki PDF'in çıkarılmış metni:** 0 eşleşme.
- **`docs/rehber/*.md` (`kontroller.txt` hariç):** yalnız `README.md`'de eşleşme var. Bu bilerek eklenen yazım kuralı maddesi ("Gerçek kart şeması markası … yazılmaz"); README PDF'e girmez.

### K-4 Örnek senaryo S1
İki PDF'in metninde şunların hepsi var:
- M3'ün yeni hâli;
- `seyahatten önce Raşit Mobil'de kart ayarlarından "Yurt dışı kullanım" seçeneğini açmanız gerekir`;
- `işlem tutarının %1,99'u kadar yurt dışı işlem ücreti alınır`;
- `en geç 120 gün içinde`;
- `Süreç ortalama 45 gün, yurt dışı işlemlerde 90 güne kadar sürebilir`.

### D-N2 Negatif deneme
- **Hazırlık:** `docs/rehber` scratch'e kopyalandı. Kopyada M3 şu hâle getirildi: `Kartlarınız yurt dışında VISA, Troy ve Mastercard'ın logosu bulunan her yerde kullanılabilir.` PDF'ler kopyada üretildi.
- **Komut:** Repodaki `dogrula.sh <scratch>/pdf`.
- **Sonuç:** Çıkış kodu 1.
  ```
  D12 KALDI · Kurgu sınırı · 2026.10 gerçek banka adı geçiyor: Mastercard; 2026.10 gerçek banka adı geçiyor: VISA; 2026.10 gerçek banka adı geçiyor: Troy; 2026.11 gerçek banka adı geçiyor: Mastercard; 2026.11 gerçek banka adı geçiyor: VISA; 2026.11 gerçek banka adı geçiyor: Troy
  Toplam: 10 GEÇTİ, 1 KALDI
  ```
- **Etki:** Repo dosyaları değişmedi.

## L-6 README
- D12 satırı: "gerçek banka ve kart şeması adı geçmiyor".
- "Yazım kuralları"na madde eklendi: gerçek kart şeması markası yazılmaz. Yerine "uluslararası kart altyapısı", "uluslararası kart logosu" ve "uluslararası kurallar" ifadeleri kullanılır.

## Kullanıcı içerik onayı
- 2026-10-30: Kullanıcı, iki PDF'te değişen 4 cümleyi (yukarıdaki M1-M4 tablosu, sayfa numaralarıyla) onayladı. Onaydan sonra kaynak değişmedi.

STATUS: DONE
