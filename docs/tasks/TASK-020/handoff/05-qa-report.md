TASK: TASK-020
# QA raporu

Servis, web, Docker ve `docs/api/` değişmedi. mvn, npm, Playwright, PIT ve Stryker uygulanmaz. Plandaki Q-1…Q-4 yapıldı. Lead raporuna güvenilmedi, her iddia yeniden çalıştırıldı.

## Kullanılan skill'ler
- `code-review-and-quality`. Diğer skill'ler kapsam dışı: Java servisi, web ve dil modeli yok.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç ✅/❌ |
|---|-----------------------|--------------------|-------------|
| K1 | Tarife satırı değeri, bölümlerdeki her geçişle aynı | D13 (`RehberDogrulama.java`, `kontroller.txt` `[TARIFE]` 73 girdi). Kendi mutasyonum M2: 10.4 FAST ATM `9,90 TL` → `9,95 TL` D13'ü KALDI yaptı, 5.7 s.41 ve 10.4 s.66 gösterildi. Mutasyon M5: 3.8 "Şu hizmetler de ücretsizdir" → "10 TL'dir" D13 KALDI. | ✅ |
| K2 | SSS değeri, atıf yapılan başlıkla aynı | D14. Mutasyon M4: kayıp kart SSS "5 iş günü" → "8 iş günü" D14 KALDI, 2.16'daki değerler listelendi. | ✅ |
| K3 | İki sürümde çalışır; değerler `surum.typ`'den; özet doğru | D13/D14 iki sürümde 72 satır, 105 soru, GEÇTİ. D8 GEÇTİ. `surum.typ` ve `degisiklik-ozeti-2026.11.md` diff'te yok. Katalog kalıplarında tarife değeri yok (aşağıda Nit-1). | ✅ |
| K4 | `dogrula.sh`'nin parçası, GEÇTİ/KALDI; bilerek tutarsızlıkla KALDI | `dogrula.sh` 13/13 GEÇTİ. S2 (D-N3) bende de D13 ve D14 KALDI verdi, iki yer yazıldı. Sayfa ve alıntı var. | ✅ |
| K5 | Bulunan tutarsızlık düzeltilir, PDF'ler yeniden üretilir, kontroller yeniden koşar | 5 SSS atfı düzeltildi (diff 5 satır, 4 dosya). PDF'ler benim üretimimle bayt bayt aynı. 13/13 GEÇTİ. | ✅ |
| K6 | Olgu değişikliği kullanıcı onayıyla | Kart sorusu 1 = A, `ONAY: VERİLDİ`. Lead raporu L-8: 5 cevap gösterildi, "Onaylıyorum" 2026-10-30. Diff'te yalnız `#bkz(...)` eklendi, tutar/oran/süre yok. | ✅ |
| S1 | Platinum ek kart 750 TL: tarife ve 2.3 aynı → GEÇTİ | Katalog girdisi `Raşit Platinum ek kart yıllık ücreti` var. Gerçek metinde GEÇTİ. | ✅ |
| S2 | SSS'e "%1,5" → KALDI, iki yer | D-N3 (aşağıda) | ✅ |
| AC1-AC3 | 13/13 GEÇTİ; negatif deneme raporda; bulgular listeli ve onaylı | Lead raporu L-3/L-7/L-8 ve benim tekrarlarım | ✅ |

### Q-1 Sınırlar
`git status` / `git diff --stat`: değişenler `README.md`, `dogrula.sh` (1 satır; 2. satır yorumu), `RehberDogrulama.java`, `kontroller.txt`, `kaynak/bolumler/02, 05, 06, 08-*.typ`, iki PDF. `docs/rehber` dışında yalnızca `handoff/00-02` izlenmeyen dosyalar. Servis, `docs/api/`, `kaynaklar/`, `surum.typ`, `ortak.typ`, `rehber.typ`, `uret.sh`, `degisiklik-ozeti-2026.11.md` ve `rehberde-olmayan-konular.md` değişmedi. ✅

### Q-2 Onay izi
Kaynak diff'inde 5 satır var ve her biri kartın A seçeneğindeki (bkz. 2.2 ve bkz. 2.4 / 5.2 ve 5.3 / 6.4 ve 6.6 / 8.3 ve 8.4) atıflar. Cevap metni ve değerleri aynı. Atıf etiketleri `<kart-turleri>`, `<eft>`, `<cihaz>`, `<atm-limit>` hedeflerinin başlık numaraları R1-R5 tablosundaki değerin yazdığı başlıklara denk geliyor; D7 317 atıfın hepsini var olan başlığa çözdü. ✅

## Sözleşme uyumu
Uygulanamaz. REST/SSE/WebSocket ve kod değişikliği yok.

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `./docs/rehber/uret.sh <scratch>/uret` | 2 PDF, 70 sayfa. SHA-256: 2026.10 `f5651f12…70199d`, 2026.11 `9dbd765f…0f32`. | Evet, lead ile aynı. `cmp` ile repodaki iki PDF'e bayt bayt eşit. |
| `./docs/rehber/dogrula.sh` | 13 GEÇTİ, 0 KALDI, çıkış 0. D7=317 atıf. D9 haritası `{1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}`. D13: 72 satır, 67 doğrulandı (100 yer), 5 yalnız tarifede. D14: 105 soru, 126 değer. | Evet |
| mvn / npm / Playwright | Uygulanamaz | — |

Negatif denemeler: her biri `docs/rehber`'in ayrı scratch kopyasında yapıldı, kopyada `uret.sh` ve `dogrula.sh` çalıştı. Repo dosyalarına dokunulmadı; çalışma bittikten sonra repo diff'i aynı 10 dosya.

| Deneme | Değişiklik (kopyada) | Sonuç |
|--------|----------------------|-------|
| **D-N3** (lead'in S2'si) | 2. bölüm SSS "Nakit avans faizi ne kadar?": `Ayrıca çekilen tutarın %1,25'i, en az 15,00 TL işlem ücreti alınır` → `Ayrıca nakit avans işlem ücreti %1,5'tir` | 11 GEÇTİ, 2 KALDI. D13: `'Nakit avans işlem ücreti' 10.1 s.65 … ↔ 2. bölüm Sık sorulanlar s.19 '… %1,5'tir' (bölümde %1,5, tarifede %1,25)`, iki sürümde. D14: `'%1,5' atıf yapılan başlıkta (bkz. 2.10) yok; oradaki aynı türden değerler: %50, %1,25, %4,25`. Lead ile aynı. ✅ |
| **M2** (benim) | 10.4 `FAST (Raşit Bankası ATM'si)` `9,90 TL` → `9,95 TL` (yalnız tarife) | D13 KALDI, 5.7 s.41'deki iki tablo satırı (9,90 TL) ve 10.4 s.66 (9,95 TL) yazıldı. ✅ |
| **M4** (benim) | Kayıp kart SSS: `diğer yerlerde 5 iş günü` → `8 iş günü` | D14 KALDI: `'8 iş günü' atıf yapılan başlıkta (bkz. 2.16) yok; oradaki … 3 iş günü, 5 iş günü`. D13 değişmedi (tarifede yok). ✅ |
| **M5** (benim) | 3.8 `Şu hizmetler de ücretsizdir` → `Şu hizmetler de 10 TL'dir` | D13 KALDI: `Dijital hesap belgesi ve IBAN belgesi` 10.2 ↔ 3.8 s.24, "tarifede ücretsiz, bölümde 10 TL". Lead'in `^şu hizmetler de` kalıbı gerçekten çalışıyor. ✅ |
| M1/M3 (benim, geçersiz) | Aranan dize birden çok yerde geçtiği için betik değişikliği uygulamadı (13 GEÇTİ). Kontrol sonucu sayılmaz; M4 ile değiştirildi. | — |

## Mutation testi
PIT ve Stryker kurulu değil / bu task'ta uygulanmaz. Yerine kontrol betiğine karşı yukarıdaki dört geçerli mutasyon (D-N3, M2, M4, M5) kendi scratch kopyamda yapıldı; dördü de beklenen KALDI'yı verdi, hayatta kalan mutant yok. Lead'in D-N7 (sessiz kalıp) denemesini ben tekrarlamadım; bu kontrolün 'geçiş bulunamadı' davranışını M2'de dolaylı olarak gördüm.

## Uçtan uca
Uygulanamaz (UI, servis, dil modeli yok).

## Q-4 Katalog incelemesi
**Yöntem:** 2026.11 PDF'inin 5-64. sayfaları `pdftotext -layout` ile çıkarıldı, Türkçe küçük harfe çevrildi. 68 `yalniz-tarife` olmayan girdinin kalıbı bağımsız bir Python betiğiyle aranıp eşleşmenin sonrası yazdırıldı. Satır bölünmesi nedeniyle iki girdi bende 0 buldu (`aynı yıldaki sonraki bildirimlerde…` ve `hesap ve ürün kapatma`, cümle iki satıra yayılıyor), `şifre cihazı şubelerden` yalnız 2026.10'da var. D13 bunları iki sürümde de doğruladığı için (72 satırdan 67'si) sorun görmüyorum.

Bağımsız olarak doğru yeri bulan en az 15 girdi, eşleşme sonrası metin:

| Girdi | Bulunan yer |
|-------|-------------|
| Raşit Gold yıllık üyelik ücreti | 1.650 TL (2.2 tablosu ve cümle) |
| Raşit Platinum ek kart yıllık ücreti | "için yıllık … 750" |
| Kâğıt ekstre (gönderim başına) | 2.7 "her gönderim için 4,90 TL posta ücreti" |
| Nakit avans işlem ücreti | "çekilen tutarın %1,25'i, en az 15,00 TL'dir" |
| Yurt dışı kart işlemi | "%1,99'u kadar yurt dışı işlem ücreti" |
| Yurt dışı ATM'den nakit çekme (ek ücret) | "%1,5'i, en az 75 TL" |
| FAST (Raşit Bankası ATM'si) | 5.7 tablosu "9,90 TL 74,50 TL" |
| EFT (Raşit Mobil ve İnternet Şubesi) | "11,95 TL" |
| Cepten Cebe gönderim | "14,90 TL'dir" |
| SWIFT gönderim (şube) | "45 ABD doları karşılığı" |
| Fatura ödeme (şube) | "9,90 TL alınır" |
| Hisse senedi alım satım komisyonu | "on binde 15'i, en az 2,50 TL" |
| Fiziki altını hesaba yatırma | "gram başına 15 TL hizmet bedeli" |
| Kiralık kasa, küçük/orta/büyük/çok büyük | 2.400 / 3.900 / 6.750 / 11.200 TL |
| Kasa kilidinin değiştirilmesi | "3.250 TL'dir" |
| Konut kredisi ekspertiz ücreti | "9.850 TL" |
| Kredi tahsis ücreti | "kredi tutarının binde 5'i" |

Hepsi doğru konunun doğru değerini gösteriyor.

**`yalniz-tarife` (5 satır):** `Şubeden kâğıt hesap belgesi`, `Kâğıt hesap cüzdanı`, `Devlet tahvili ve hazine bonosu alım satımı`, `Eurobond alım satımı`, `Şubeden nakit çekme ve yatırma`. Gövde metninde (1-9. bölümler) aradım: hesap belgesi/cüzdanı yalnız 3.8'de "ücreti olan hesap hizmetleri … bkz. 10.2" olarak geçiyor, değer yok. Tahvil/bono/eurobond 7.6'da yalnızca en düşük alım tutarı (1.000 TL / 1.000 ABD doları ya da euro, ücret değil), "saklama ücreti alınmaz" ve "işlem ücretleri bkz. 10.6" var, işlem ücreti değeri yok. Şubeden nakit çekmede (2.1) "limit uygulanmaz" yazıyor, ücret yok. Beş satırın hiçbiri bölümlerde ücret değeriyle geçmiyor. ✅

**Lead'in sapmalarının değerlendirmesi:**
1. *Pencere eşleşmenin sonundan başlıyor.* Gevşetme değil, doğru ve gerekli: etiketin kendisinde sayı olan satırlarda (`FAST, 6.000 TL'ye kadar`) aksi hâlde etiketteki sayı "ilk değer" olurdu. S2 (D-N3) ve M2 bu yöntemle yakalandı. Kabul.
2. *"ücret almaz" da ücretsiz sayılıyor.* Gerçek metinde (4.13, 5.5, 7.2, 3.7) kullanılan biçim. Gevşetme sayılmaz; tersine, bu olmadan satırlar doğrulanamazdı. Bir riski var: "ücret almaz" geçen cümleye 10 TL gibi bir değer eklenirse, ilk değer olan "almaz" yüzünden değer gözden kaçabilir (aşağıda Optional-1). Kabul.
3. *"Metin" türü* (Taşıt rehin tesis ücreti). Pencerede belirteç yoksa doğrulama, varsa çelişki. Kuralı daha sıkı değil ama yalnız bir satır için. Kabul.
4. *Paragraf başı kuralı.* "N. ", "• " ve "Örnek:" yeni paragraf. Kontrolün gücünü azaltmaz, cümle sınırlarını netleştirir. Kabul.
5. *`^şu hizmetler de`.* M5 ile doğrulandı: tarifeye ücret yazılırsa D13 çelişki veriyor. `yalniz-tarife` yapılmamış, plan #10'a uygun. Kabul.
6. *`^hizmet(?= )`.* Çok genel bir kalıp (8.2 "Hizmet 48 şubemizde verilir" cümlesine de uyuyor). Orada değer olmadığı için "değersiz" sayılıyor, yanlış doğrulama üretmiyor. Optional-2.
7. *Erken ödeme tazminatı kalıpları iki alt satırla sınırlı; "12 aydan uzun %1" ve "36 aydan uzun %2" D13'te karşılaştırılmıyor.* Bu bilinçli bir gevşetme; lead raporda açıkça yazmış ve D4 bu iki değeri zaten birebir arıyor. Kabul, Optional-3.
8. *"Değersiz" geçişler (15 eşleşme, 13 cümle).* Kontrol bunları hata ya da doğrulama saymıyor. Hiçbir girdi yalnız değersiz geçişle GEÇTİ olmuyor (en az bir doğrulanmış geçiş şartı, lead'in D-N7'si ve benim M2 çıktımla tutarlı: 'bölümlerde doğrulanan geçiş bulunamadı' KALDI veriyor). Sessiz gevşeme yok.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional-1 | `dogrulama/RehberDogrulama.java` (D13 ilk değer kuralı) | "ücret almaz/alınmaz" geçen cümlede ücretsiz sözcüğü ilk değer olarak sayılıyor. Aynı cümleye sonradan eklenen bir tutar karşılaştırılmaz. Mevcut metinde yanlış alarm ya da kaçırma yok, TASK-021'e sınır olarak not edilebilir. | lead |
| Optional-2 | `kontroller.txt` (`SMS ile kart harcama bildirimi => ^hizmet(?= )`) | Çok genel kalıp. Bugün zararsız. Kalıp ilgili cümleyi (6.7 "Hizmet ücretsizdir") daha belirgin yakalarsa gelecekte yanlış alarm riski azalır. | lead |
| Optional-3 | `kontroller.txt` (erken ödeme tazminatı satırları) | Tarife hücresindeki "üstü %1" ve "üstü %2" yalnız D4 ile korunuyor, D13 karşılaştırmıyor. Lead'in raporunda açık. | lead (TASK-021) |
| Nit-1 | `kontroller.txt` (`[TARIFE]` FAST satırları, erken ödeme satırları) | Kalıplar satır etiketlerindeki "6.000 TL'ye kadar", "12 ay ve altı", "36 ay ve altı" ifadelerini içeriyor. Bunlar değer değil kademe adı, ama bir sürümde 6.000 limiti değişirse kalıp yeniden yazılmalı. Bu durumda D13 sessiz geçmez, "geçiş bulunamadı" KALDI verir. | lead |
| FYI | `degisiklik-ozeti-2026.11.md` | Dokunulmadı; D8 GEÇTİ. Atıf düzeltmesi iki sürümde aynı olduğundan özete fark eklenmesi gerekmiyor. | — |
| FYI | plan Riskler 3-4 | 2.8 "Ödeme … anında yansır" ve gecikme faizi sözlük notları kapsam dışı, kontrol yakalamaz. Ayrı task adayı. | planner |

Critical ya da Required bulgu yok.

## Düzeltme
Gerekmiyor.

STATUS: DONE
