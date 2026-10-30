TASK: TASK-004
# Lead uygulama raporu: Raşit Bankası Ürün ve Hizmet Rehberi

## Özet
- Rehber `docs/rehber/` altında, tek Typst kaynağından iki sürüm olarak üretildi: 2026.10 ve 2026.11. İki sürüm de 70 sayfa ve 11 bölüm.
- Ayrıca yazıldı: değişiklik özeti, "rehberde olmayan konular" notu, `uret.sh`, `dogrula.sh` ve README.
- `dogrula.sh`: 11 kontrolün 11'i GEÇTİ, çıkış kodu 0.
- Negatif denemede (D-N) D8 ve D4 KALDI verdi, çıkış kodu 1 oldu.
- `uret.sh` iki kez çalıştırıldı; PDF'ler bayt bayt aynı (`cmp`).
- Kullanıcının tam metin onayı bekleniyor (aşağıda "Kullanıcı içerik onayı").

## Araç sürümleri
| Araç | Sürüm | Not |
|------|-------|-----|
| Typst | `typst 0.15.1 (unknown commit)` | `brew install typst` ile kuruldu |
| Java | `java 26.0.1 2026-04-21` | `dogrula.sh` tek dosyalık programı bununla çalıştırır |
| Apache PDFBox | `pdfbox-app-3.0.6.jar` | Maven Central'dan indirildi. SHA-1 `e99940234dbd689c45274fadc0bec350272cf3ed` (Maven Central'ın `.sha1` dosyasıyla aynı). SHA-256 `28948291a7d6addb91a158292f2e9348d2143720e25a9c87c91bbdd4b088475f`, `dogrula.sh` içine sabitlendi |
| Source Sans 3 | 3.052R (adobe-fonts/source-sans GitHub sürümü) | Regular, Semibold ve Bold TTF dosyaları ile `OFL.txt` (sürümün LICENSE.md dosyası) |

## Dosyalar
| Yol | İçerik |
|-----|--------|
| `docs/rehber/README.md` | Kurulum, komutlar, kontroller, TASK-005 için üst ve alt bilgi kalıpları, yazım kuralları |
| `docs/rehber/kaynak/rehber.typ` | Ana dosya: sayfa düzeni, üst ve alt bilgi, kapak, içindekiler, bölümlerin sırası |
| `docs/rehber/kaynak/surum.typ` | Sürüm sözlüğü: `etiket`, `yururluk`, `tarih`, `gold-yillik-ucret`, `banka-karti-atm-gunluk`, `genc-kart`, `sifre-cihazi`. Bilinmeyen ya da verilmemiş sürüm derlemeyi durdurur |
| `docs/rehber/kaynak/ortak.typ` | Yardımcılar: `tablo` (hücreleri ölçer, satır sığmazsa derlemeyi durdurur), `ara`, `bkz`, `sss`, `soru`, `bilgi` |
| `docs/rehber/kaynak/bolumler/01-…11-*.typ` | 11 bölüm |
| `docs/rehber/kaynak/fontlar/` | 3 TTF ve `OFL.txt` |
| `docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf` | 70 sayfa, 1.117.324 bayt |
| `docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf` | 70 sayfa, 1.117.555 bayt |
| `docs/rehber/degisiklik-ozeti-2026.11.md` | Kısa özet (yükleme penceresi biçiminde), 15 satırlık "Ayrıntı" tablosu, asistanda beklenen etki |
| `docs/rehber/rehberde-olmayan-konular.md` | 9 konu: tanım, örnek sorular, yasak kelimeler (toplam 54), beklenen cevap |
| `docs/rehber/uret.sh` | İki PDF'i üretir |
| `docs/rehber/dogrula.sh`, `dogrulama/RehberDogrulama.java`, `dogrulama/kontroller.txt` | Kontroller |

`kaynaklar/`, servis kodu, `docs/api/`, kök `CLAUDE.md` ve `rag.md` değişmedi. `git status` yalnızca `docs/rehber/` ve `handoff/` dosyalarını gösteriyor.

## Komut çıktıları
### `./docs/rehber/uret.sh`
```
pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf: 70 sayfa
pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf: 70 sayfa
```

### Tekrar üretim (D11)
`./docs/rehber/uret.sh <scratch>/ikinci` ile ikinci bir üretim yapıldı ve repodaki PDF'lerle `cmp` ile karşılaştırıldı:
```
cmp 2026.10: aynı
cmp 2026.11: aynı
752abfa3315ab8bd4904ec20076874b2c60095ac841c2457189fc63fa295cb03  docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf
3688ad3419dc8cf723fec3e13a12777c5a841e0a8b39508242e8ee8d0f755937  docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf
```
Plan Kararlar #18: bayt eşitliği sağlandı. Typst 0.15.1 `--creation-timestamp` seçeneğini destekliyor; zaman damgası sürümün yürürlük tarihine sabitlendi (1790812800 ve 1793491200).

### `./docs/rehber/dogrula.sh` (çıkış kodu 0)
```
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
D12 GEÇTİ · Kurgu sınırı · kapak notları var; 24 gerçek banka adı geçmiyor; adresler [www.rasitbank.example, musteri@rasitbank.example, kep@rasitbank.example]

Toplam: 11 GEÇTİ, 0 KALDI
```

### Negatif deneme (D-N)
1. `docs/rehber` repo dışındaki bir kopyaya (`<scratch>/negatif/rehber`) alındı. Kopyadaki `surum.typ`'de 2026.11'in `gold-yillik-ucret` değeri `1.450 TL` yapıldı.
2. PDF'ler `<scratch>/negatif/pdf-bozuk` klasörüne üretildi.
3. Repodaki `./docs/rehber/dogrula.sh <scratch>/negatif/pdf-bozuk` çalıştırıldı. Sonuç, çıkış kodu 1:
```
D4 KALDI · Tablo satırları · 2026.11 tek satırda yok: Raşit Gold | 1.650 TL | Yıllık 150.000 TL harcama | Harcamanın binde 5'i; 2026.11 tek satırda yok: Raşit Gold yıllık üyelik ücreti | 1.650 TL
D8 KALDI · Değişiklik özeti · F1.1 eski metin 2026.11 içinde hâlâ var: Raşit Gold 1.450 TL; F1.1 2026.11 s.8 üzerinde yok: Raşit Gold 1.650 TL; … 2026.11 '1.450 TL' 3 kez geçiyor, beklenen 0; 2026.11 '1.650 TL' 0 kez geçiyor, beklenen 3
Toplam: 9 GEÇTİ, 2 KALDI
```
Repodaki `surum.typ` değişmedi: 2026.11 `gold-yillik-ucret: "1.650 TL"`.

## Sayfa düzeni
Kapak s. 1, İçindekiler s. 2-4. İki sürümde aynıdır:

| Bölüm | Başlangıç | Sayfa sayısı | Kartta tahmin |
|-------|-----------|--------------|---------------|
| 1 Bu rehber hakkında | 5 | 2 | 2 |
| 2 Kartlar | 7 | 13 | 14 |
| 3 Hesaplar | 20 | 9 | 9 |
| 4 Krediler | 29 | 9 | 10 |
| 5 Para transferi ve ödemeler | 38 | 7 | 8 |
| 6 Dijital bankacılık | 45 | 6 | 7 |
| 7 Yatırım | 51 | 6 | 6 |
| 8 Şube, ATM ve müşteri hizmetleri | 57 | 5 | 5 |
| 9 Şikâyet, itiraz ve kişisel veriler | 62 | 3 | 3 |
| 10 Ücret tarifesi | 65 | 3 | 4 |
| 11 Terimler sözlüğü | 68 | 3 | 3 |
| **Toplam** | | **70** | ~74 (hedef 72, kabul 68-76) |

## Kapsam eşlemesi: TASK alt konusu → başlık (K2)
| TASK Kural 2 alt konusu | Başlık |
|-------------------------|--------|
| Banka kartı | 2.1 |
| Kredi kartı türleri (Raşit Gold dahil) | 2.2 |
| Yıllık üyelik ücreti ve iadesi | 2.4 |
| Limit artırma ve düşürme | 2.6 |
| Hesap kesim tarihi, ekstre, asgari ödeme | 2.7, 2.8 |
| Taksit | 2.9 |
| Nakit avans | 2.10 |
| Sanal kart | 2.12 |
| Temassız ödeme | 2.13 |
| Yurt dışı kullanım ve kur | 2.14 |
| Kayıp ya da çalıntı bildirimi | 2.16 |
| İtiraz (chargeback) | 2.17 |
| Kartın yenilenmesi ve teslim süresi | 2.18 |
| Vadesiz, vadeli, döviz ve altın hesapları | 3.1, 3.2, 3.4, 3.5 |
| Hesap açma belgeleri | 3.7 |
| Hesap işletim ücreti | 3.8 |
| Hesap kapatma | 3.10 |
| Mevduat sigortası (TMSF) | 3.11 |
| Stopaj | 3.12 |
| İhtiyaç, taşıt ve konut kredisi | 4.3, 4.4, 4.5 |
| Kredili mevduat hesabı | 4.6 |
| Başvuru şartları ve belgeleri | 4.1, 4.2 |
| Faiz, KKDF, BSMV, masraflar | 4.7 |
| Erken ödeme ve erken kapama tazminatı | 4.10 |
| Gecikme faizi | 4.11 |
| Yapılandırma | 4.12 |
| Havale, EFT, FAST, SWIFT | 5.1, 5.2, 5.3, 5.4 |
| Saatler, limitler, ücretler | 5.6, 5.7 |
| Karekod ile ödeme | 5.8 |
| Düzenli ödeme talimatları | 5.9 |
| Fatura ödemeleri | 5.10 |
| Raşit Mobil ve internet şubesi | 6.1 |
| İlk giriş, şifre alma ve sıfırlama | 6.2, 6.3 |
| Cihaz tanımlama | 6.4 |
| İşlem limitleri | 6.6 |
| Bildirimler | 6.7 |
| Güvenlik ve dolandırıcılık uyarıları | 6.8, 6.9 |
| Yatırım fonları, hisse senedi | 7.2, 7.3 |
| Döviz ve altın alım satımı | 7.4, 7.5 |
| İşlem saatleri ve hafta sonu kuru | 7.4 (döviz), 7.5 (altın), 7.2 (fon), 7.3 (hisse) |
| Vergilendirme | 7.8 |
| Şube ve ATM hizmetleri | 8.1, 8.2, 8.3 |
| ATM nakit çekme limitleri | 8.4, 8.5 |
| Müşteri hizmetleri ve kanallar | 8.7 |
| Şikâyet ve itiraz süreci | 9.1, 9.2 |
| KVKK ve aydınlatma metni | 9.3, 9.4 |
| Ücret tarifesi tablosu | 10 (10.1-10.7) |
| Terimler sözlüğü | 11 (69 terim) |

Sık sorulanlar 2-9. bölümlerin sonunda yer alıyor. Soru sayıları: 2. bölüm 22, 3. bölüm 19, 4. bölüm 14, 5. bölüm 15, 6. bölüm 13, 7. bölüm 12, 8. bölüm 6, 9. bölüm 4. Her bölümde en az 4 soru var. Soruların çoğu lab'daki soru kategorilerinden alındı.

## Örnek senaryoların karşılığı
| Senaryo | Rehberdeki yeri |
|---------|-----------------|
| S1 "Kartımı kaybettim" | 2.16 (s. 16): kanal tablosu, kalıcı ve geçici kapatma, il merkezlerinde 3, diğer yerlerde 5 iş günü teslim. Metin kartta onaylanan olgularla aynı |
| S2 "Ev kredisini erken kapatırsam ceza öder miyim?" | 4.10 (s. 35): tazminat tablosu (konut kredisinde %1 ya da %2; değişken faizli kredilerde ve Ek Hesap'ta alınmaz) ve 4. bölüm SSS'nin ilk sorusu. Metin kartta onaylanan olgularla aynı |
| S3 "Bitcoin" | İki PDF'te de geçmiyor (D10) |
| S4 Gold ücreti | 2026.10'da 1.450 TL, 2026.11'de 1.650 TL; her sürümde 3 yerde (D8) |

## Kurgusal kimlikler (Kararlar #14)
| Bilgi | Değer | Kontrol |
|-------|-------|---------|
| Web ve e-posta | `www.rasitbank.example`, `musteri@rasitbank.example`, `kep@rasitbank.example` | RFC 2606 ile ayrılmış `.example` alan adı; D12 başka adres olmadığını kontrol ediyor |
| Müşteri Hizmetleri | 0850 724 72 74; yurt dışından +90 216 724 72 74 | Web aramasında (`"0850 724 72 74"`) bu numaraya ait hiçbir sonuç çıkmadı |
| SWIFT kodu | RSBKTRIS | Web aramasında (`"RSBKTRIS" SWIFT BIC`) kayıt çıkmadı; sonuçlarda yalnızca gerçek Türk bankalarının başka kodları vardı (ISBKTRIS, AKBKTRIS gibi) |
| Diğer | Raşit Yatırım Menkul Değerler A.Ş., Raşit Portföy | Kurgusal |

Kesin güvence yok (plan Riskler 5): arama, numaranın ve kodun kullanılmadığını kanıtlamaz, yalnızca kayıt bulunamadığını gösterir. Kamu kurumları adıyla anılıyor: TCMB, BDDK, TMSF, KVKK Kurumu, Tüketici Hakem Heyeti, Ticaret Bakanlığı, KKB, Merkezi Kayıt Kuruluşu, Borsa İstanbul.

## Plandan sapmalar ve kararlar
1. **D3'te büyük "Ğ" aranmıyor.** Türkçede Ğ ile başlayan sözcük yok; büyük Ğ yalnızca tamamı büyük harfle yazılmış metinde çıkar ve rehberde öyle metin yok. Diğer 12 harf (büyük İ dahil) aranıyor. Sapmanın gerekçesi kodda yorum olarak da yazılı.
2. **Gerçek banka adları (D12) `kontroller.txt`'de, 4. konunun yasak ifadeleri `rehberde-olmayan-konular.md`'de.** Plandaki ayrım korundu. Not dosyasında 4. konu için "rakip banka", "diğer bankaların faiz" gibi ifadeler var ve D12'ye atıf yapılıyor.
3. **"Garanti" yerine "Garanti BBVA".** Banka adları tam kelime olarak aranıyor; "garanti" sıradan bir Türkçe sözcük olduğu için banka adı iki kelimeyle tutuldu.
4. **İçindekiler 3 sayfa** (kartta 2 tahmin edilmişti). 110 başlık ve 8 "Sık sorulanlar" satırı 2 sayfaya sığmadı. Toplam sayfa sayısı yine aralıkta.
5. **Toplam 70 sayfa** (hedef 72, kabul 68-76). Bölümlerin sayfa dağılımı yukarıdaki tabloda.
6. **`--ignore-embedded-fonts` eklendi.** Typst'in kendi gömülü yazı tipleri de kullanılmıyor; bütün glifler Source Sans 3'ten geliyor. İtalik kullanılmadığı için italik TTF repoya girmedi; çıkarıldığında PDF'ler bayt bayt aynı kaldı.
7. **Tireli sözcükler bölünmüyor.** Typst, kelime bölme kapalıyken de tirede satır kırabiliyordu ("e-" / "posta"). `rehber.typ`'ye tireli sözcükleri ve aralıkları tek parça tutan bir kural eklendi. D3 bunu kontrol ediyor.
8. **Sayfa sayısı `typst eval` ile okunuyor.** `uret.sh`, belgedeki görünmeyen `<sayfa-sayisi>` etiketini okuyor. Typst 0.15'te `typst query` komutu eskidiği için `eval` kullanıldı.
9. **Metin çıkarma ayarı:** `PDFTextStripper` varsayılan ayarlarıyla (`setSortByPosition(false)`) kullanıldı; konum sıralamasına gerek kalmadı. TASK-005 aynı ayarı kullanmalı (README "İçe alma için notlar").

## Kullanıcıya bilgi (karar gerektirmeyebilir)
Kartta onaylanan iki olgu gerçek mevzuattan farklı olabilir. Rehber kurgusal olduğu için kartta onaylandığı gibi yazıldı:
- 4.10: ihtiyaç ve taşıt kredisinde erken ödeme tazminatı (%0,5 / %1). Gerçek tüketici kredisi mevzuatında bu kredilerde tazminat alınmıyor olabilir.
- 2.16: bildirimden önceki 24 saatteki sorumluluk sınırı 250 TL. Gerçek mevzuattaki tutar farklı olabilir.

## Kullanıcı içerik onayı
- Durum: VERİLDİ (30 Ekim 2026). Kullanıcı iki PDF'in ve değişiklik özetinin tam metnini değişiklik istemeden onayladı.
- Onaydan sonra kaynakta değişiklik yapılmadı. Onaylanan PDF'lerin SHA-256 değerleri yukarıdaki "Tekrar üretim (D11)" bölümünde.
- Bilgi notundaki iki olgu (4.10 ihtiyaç/taşıt tazminatı, 2.16 250 TL sınırı) kartta onaylandığı hâliyle kaldı.

STATUS: DONE
