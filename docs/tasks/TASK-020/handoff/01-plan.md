TASK: TASK-020
# Plan

## Özet
**Mevcut durum:** `docs/rehber/dogrula.sh` 11 kontrol çalıştırıyor (D1-D10, D12). D4 ve D8 yalnız `kontroller.txt`'de ve değişiklik özetinde listelenen dizeleri arıyor. Tarife (10.1-10.7) ile bölüm metni arasında ve SSS cevabı ile atıf yaptığı başlık arasında genel bir karşılaştırma yok (TASK-004 plan Riskler 1, qa Optional 1).

**Yeni durum:**
- `dogrulama/RehberDogrulama.java`'ya iki kontrol eklenir: **D13** (tarife ↔ bölüm metni, Kural 1) ve **D14** (SSS değeri ↔ atıf yapılan başlık, Kural 2). İkisi de diğerleri gibi `GEÇTİ`/`KALDI` yazar; toplam 13 kontrol.
- `dogrulama/kontroller.txt`'ye `[TARIFE]` bölümü eklenir (tarife satırı → konu kalıpları). Değer yazılmaz; değerler her sürümün PDF'inden okunur (Kural 3).
- Rehber içeriği yalnız kullanıcının onayladığı bulgular için değişir (kart sorusu 1). Olgu (tutar, oran, süre) değişmez.
- İki PDF `uret.sh` ile yeniden üretilir, 13 kontrol çalıştırılır.
- **Değişen servis yok.** assistant-api, retrieval-api, web, `docs/api/**`, Docker etkilenmez.

**Planlama taraması (kaynak elle okundu, iki sürüm):**
- Tarife ↔ bölüm: 10.1-10.7'deki 73 satırın (her sürümde 72) bölüm metnindeki karşılıkları tek tek karşılaştırıldı. **Çelişen değer bulunmadı.** Bölümlerde hiç geçmeyen satırlar (aday): şubeden kâğıt hesap belgesi, kâğıt hesap cüzdanı, devlet tahvili ve hazine bonosu alım satımı, eurobond alım satımı, şubeden nakit çekme ve yatırma.
- SSS ↔ atıf: 105 soru-cevap (2→22, 3→19, 4→14, 5→15, 6→13, 7→12, 8→6, 9→4). Değerlerin hepsi doğru; ancak 5 cevapta değer, atıf yapılan başlıkta değil başka başlıkta yazıyor (D14 bunları KALDI sayar):

| # | Soru (bölüm SSS) | Mevcut atıf | Değer | Değerin yazdığı başlık |
|---|------------------|-------------|-------|------------------------|
| R1 | "Raşit Gold kartın yıllık üyelik ücreti ne kadar?" (2) | 2.4 | 1.450/1.650 TL, 150.000 TL | 2.2 |
| R2 | "Kart aidatından muaf olmak için ne kadar harcamam gerekiyor?" (2) | 2.4 | 60.000, 150.000, 400.000 TL | 2.2 |
| R3 | "FAST ile en fazla ne kadar gönderebilirim?" (5) | 5.3 | 09.00-17.00 | 5.2 |
| R4 | "Transfer limitimi artırdım ama hâlâ işlem yapamıyorum." (6) | 6.6 | 10.000 TL | 6.4 |
| R5 | "Kartım yanımda değil. ATM'den para çekebilir miyim?" (8) | 8.3 | 5.000 TL | 8.4 |

**Uygulama sırası:**
1. Kullanıcı onayı (kart; soru 1).
2. **lead** L-1…L-3: D13/D14'ü yazar, içerik değişmeden çalıştırır.
3. **Bulgu kapısı (Kural 5-6):** D13/D14 bulguları yukarıdaki tabloyla aynıysa kartta seçilen seçenek uygulanır. Fazladan gerçek bulgu çıkarsa lead düzeltmeden önce kullanıcıya iki yeri ve önerisini gösterir, kararı rapora yazar (görev sahibi lead).
4. **lead** L-4…L-8: düzeltme, üretim, 13 kontrol, negatif denemeler, kullanıcı içerik onayı, rapor `handoff/02-lead-implementation.md`.
5. **qa** `handoff/05-qa-report.md`.

## Çalışacak roller
- **planner:** bu plan ve onay kartı.
- **lead (ana oturum):** `docs/rehber/**` rol tablosunda sahipsizdir, TASK "lead uygular" der (TASK-004/023 düzeni).
- **qa:** bağımsız üretim ve doğrulama, katalog kalıplarının incelemesi. qa, lead adımına ve kullanıcı içerik onayına bağlanır.
- **Atlananlar:** backend-coder, frontend-coder (servis ve ekran değişmez); tester (kontroller `docs/rehber/` altında, tester yollarında değil; lead çalıştırır, qa tekrarlar); devops (Docker, model, kök README değişmez).

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Doğrulama |
|---|-----------------|------------------|-----------|
| K1 | Tarife satırının değeri bölüm metni/tablosundaki her geçişte aynı | D13; `[TARIFE]`; Kararlar #4, #5, #9 | D13, D-N3, D-N6, D-N5 |
| K2 | SSS değeri atıf yapılan başlıktaki değerle aynı | D14; Kararlar #6, #7, #8 | D14, D-N3 |
| K3 | İki sürümde çalışır; sürüm değerleri `surum.typ`'den; değişiklik özeti doğru kalır | Kararlar #3; L-6 (özet sayfaları) | D13/D14 iki sürümde, D-N4, D8 |
| K4 | `dogrula.sh`'nin parçası, GEÇTİ/KALDI; bilerek tutarsızlıkla KALDI gösterilir | Kararlar #1; L-7 | D-N3…D-N7 |
| K5 | Bulunan tutarsızlık düzeltilir, PDF'ler yeniden üretilir, bütün kontroller yeniden çalışır, değişen olgu kullanıcıya gösterilir | L-5, L-6, L-8 | D1-D14, D11, Q-3 |
| K6 | Olgu değişikliği kullanıcı onayıyla | Kart sorusu 1; bulgu kapısı (uygulama sırası 3) | K-2, Q-2 |
| S1 | Platinum ek kart 750 TL: tarife ve 2.3 aynı → GEÇTİ | D13 (`Raşit Platinum ek kart yıllık ücreti` girdisi) | D13 ayrıntısında bu satır "doğrulandı" |
| S2 | SSS'e "nakit avans işlem ücreti %1,5" → KALDI, iki yer | D13 + D14 | D-N3 |
| AC1 | `dogrula.sh` yeni kontrollerle bütün kontrollerden geçiyor | L-6 | 13/13 GEÇTİ |
| AC2 | Negatif deneme raporda | L-7 | D-N3…D-N7 |
| AC3 | Bulunan/düzeltilen tutarsızlıklar raporda, kullanıcı onaylı | L-3, L-8 | Q-2, Q-3 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test |
|---|-------|-------------------|-----------------|
| G1 | Sürüme özel tarife satırları (Raşit Genç yalnız 2026.11, şifre cihazı yalnız 2026.10) | Katalog girdisi en az bir sürümde satır bulur; "bölümde geçiş" şartı yalnız satırın olduğu sürümde aranır | D13 (iki sürüm), D-N4 |
| G2 | Türkçe ekler ve harf: "%1,25'i", "1.650 TL'dir", "12 ayda", "Kâğıt/kâğıt" | Değer belirteçlerinden ek atılır; konu kalıpları Türkçe küçük harfe (`Locale tr`) çevrilmiş metinde aranır | D13, D14 gerçek metinde GEÇTİ |
| G3 | Başlık ya da SSS iki sayfaya yayılıyor (2. bölüm SSS s. 18-19) | Bölüm metni sayfalar birleştirilip üst/alt bilgi atılarak kurulur | D14 (2. bölümün 22 sorusu sayılır) |
| G4 | `[TARIFE]` bölümü boş ya da yanlış yazılmış | Tarife satırları karşılıksız kalır → D13 KALDI | D-N5 |
| G5 | Konu kalıbı hiçbir yeri bulmuyor (sessiz geçiş) | `yalniz-tarife` işaretli değilse D13 KALDI ("bölümlerde geçiş bulunamadı") | D-N7 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif |
|---|-------|---------|-------------------|
| 1 | D13 ve D14 mevcut tek dosyalık `RehberDogrulama.java`'ya eklenir; `dogrula.sh`'de yalnız 2. satırdaki yorum "(D1-D10, D12-D14)" olur | TASK-004 Kararlar #15 ile aynı araç; JDK 21 şartı korunur; yeni bağımlılık yok | Ayrı Python betiği: yeni çalışma zamanı. Çok dosyalı Java kaynak programı: JDK 22+ ister. Typst `query`: kontrol anında Typst ve kaynakta metadata gerekir |
| 2 | Olgular PDF metninden (PDFBox, mevcut `PDFTextStripper` ayarı) okunur | Asistanın içe alacağı metin kontrol edilir; sürüm değerleri derlenmiş hâlde gelir | Typst kaynağını ayrıştırmak: `#if`, `S.*`, etiket→numara mini bir Typst yorumlayıcısı ister, PDF'te görünenle ayrışabilir |
| 3 | Katalogda ve Java'da hiçbir olgu değeri yazılmaz; tarife değeri her sürümün 10. bölümünden, karşılaştırılan değerler aynı PDF'ten okunur | Kural 3: sürüm değerleri yalnız `surum.typ`'de kalır | Değerleri `kontroller.txt`'ye yazmak (D4 gibi): sürüm değeri iki yerde tutulur |
| 4 | **D13 algoritması.** (a) Tarife satırı: 10.1-10.7 içinde tablo yazı boyutundaki (9,5 pt, `ortak.typ` `tablo-yazi`) kalın olmayan satır; başlık satırı ("Hizmet Ücret") kalındır. Satır, katalogdaki en uzun eşleşen etiketle başlamalı; kalan kısım değer hücresidir. (b) Birimler: 1-9. bölümlerde düzyazıda cümle, tablolarda satır (SSS dahil). "Örnek:" ile başlayan paragraf hariç. (c) Her konu kalıbı eşleşmesinde pencere = eşleşmeden birimin sonuna kadar. (d) Değer türüne göre kural: *tutarlı* değer (hücrede belirteç var): penceredeki ilk değer öğesi "ücretsiz/alınmaz" ise çelişki; aksi hâlde tarifedeki her belirteç türü için penceredeki o türün ilk belirteci tarifedekine eşit olmalı. *Ücretsiz* değer (Ücretsiz, Alınmaz, Komisyon alınmaz): penceredeki ilk değer öğesi belirteçse çelişki. *Metin* değer (ör. "Noter ve tescil maliyeti kadar"): pencerede belirteç varsa çelişki. Pencerede değer öğesi yoksa geçiş "değersiz" sayılır (ne hata ne doğrulama). (e) Kapsam: her tarife satırı tam bir katalog girdisine eşlenir; `yalniz-tarife` olmayan her girdi, satırının bulunduğu her sürümde en az bir doğrulanmış geçiş bulur | Kural 1'deki "her satır" ve "her yer" kendini zorlar: yeni tarife satırı kalıp yazılmadan geçmez; değerler listeden değil PDF'ten gelir | Değer-öncelikli arama (tarife değerini metinde aramak): yalnız varlığı kanıtlar, S2'deki "%1,5" gibi çelişkiyi yakalamaz |
| 5 | **Değer belirteçleri** (D13 ve D14 ortak): TL tutarı (`1.450 TL`), döviz (`25 ABD doları`, `100 euro`), yüzde (`%1,25`), `on binde N`/`binde N` (önce uzun), `N puan` ("RaşitPuan" değil), süre `N dakika/saniye/saat/iş günü/gün/ay/yıl` (önce "iş günü"; ek atılır: "12 ayda"→"12 ay"), saat `HH.MM`. Aralıkta iki uç ayrı belirteçtir ("1.000-99.999 TL" → `1.000 TL`, `99.999 TL`); kesme işaretinden sonrası atılır. Adet/sayı (kez, adet, taksit, yaş, cihaz, hane, karakter), yıl numarası, telefon, kod ve başlık numarası belirteç değildir | Kural 2'nin "tutar, oran, süre ve limit"i. Sayılar aynı olguyu farklı kelimeyle anlatıyor ("4 kez"/"4 adede kadar", 3.3) ve yanlış alarm verir | Sayıları da katmak: 3. bölüm SSS'te doğru bir cevap KALDI olur |
| 6 | **D14 SSS ayrıştırma:** 2-9. bölümlerde "Sık sorulanlar" satırından bölüm sonuna kadar; soru = Semibold yazı tipindeki metin (`ortak.typ` `soru`), cevap = sonraki normal metin. Cevaptaki bütün `bkz. X.Y` hedeftir. Sorudaki değerler kontrol edilmez (müşterinin örneği: "16 yaşındaki", "100.000 TL kredi") | 6. bölümde "?" ile bitmeyen soru var; yazı tipi kesin ayırır | "?" ile bölmek: o soru cevaba karışır |
| 7 | **D14 kuralı:** cevaptaki her belirteç, hedef başlıkların (birleşim) belirteçleri içinde olmalı. Başlık metni = başlık satırından sonraki numaralı başlığa ya da "Sık sorulanlar"a kadar, örnekler dahil. Yapı hatası (sorusuz cevap, atıfsız cevap, soru sayısı ≠ kaynaktaki `#soru[` sayısı 105) KALDI. Kart sorusu 1 = B ise hedef başlıkların kendi atıfları da (yalnız bir adım) birleşime girer | Kural 2'nin sözü; tek adım B seçeneğinin tanımıdır | Yinelemeli atıf takibi: birleşim bütün bölüme yayılır, kontrol anlamsızlaşır |
| 8 | Çıktı: `D13 GEÇTİ · Tarife tutarlılığı · 2026.10: 72 tarife satırı, N'i bölümlerde M yerde doğrulandı, K'i yalnız tarifede; 2026.11: …`. `D14 GEÇTİ · Sık sorulanlar ve atıflar · 2026.10: 105 soru, V değer; 2026.11: …`. KALDI'da her bulgu için iki yer: sürüm, başlık (ya da "N. bölüm Sık sorulanlar"), sayfa ve ≤100 karakterlik alıntı; D14'te hedef başlığın aynı türdeki değerleri de yazılır | TASK: "iki yeri gösterir"; qa ve kullanıcı bulguyu PDF'te bulabilir | — |
| 9 | Katalog `kontroller.txt` `[TARIFE]` bölümünde; satır biçimi `<tarife etiketi, birebir> => <kalıp> || <kalıp>` ya da `<etiket> => yalniz-tarife`. Kalıp, Türkçe küçük harfli ve boşlukları teke indirilmiş birim metninde aranan Java düzenli ifadesidir. 73 girdi. Bölüm başındaki yorum biçimi ve kuralı anlatır | Beklentiler tek dosyada kalır (TASK-004 düzeni) | Ayrı `olgular.txt`: ikinci veri dosyası |
| 10 | Lead, yanlış alarmı yalnız kalıbı daraltarak ya da `yalniz-tarife` işaretiyle giderebilir; her böyle değişikliği gerekçesiyle rapora yazar. Rehber metnini kart ya da bulgu kapısı onayı olmadan değiştiremez. Bölümde geçen bir satırı `yalniz-tarife` yapamaz | Kontrolün sessizce gevşetilmesini önler; qa kalıpları inceler (Q-4) | — |
| 11 | Kart sorusu 1 = A ise atıf biçimi `(bkz. X ve bkz. Y)`, numaralar artan sırada: R1, R2 `(bkz. 2.2 ve bkz. 2.4)`; R3 `(bkz. 5.2 ve bkz. 5.3)`; R4 `(bkz. 6.4 ve bkz. 6.6)`; R5 `(bkz. 8.3 ve bkz. 8.4)`. Cevap metni başka değişmez. C ise atıf yalnız değer başlığına çevrilir | Mevcut çoklu atıf biçimi (`#bkz(..) ve #bkz(..)`, 10. bölüm); D7 her atfı sayar | — |
| 12 | Sayfa düzeni bazı (TASK-023 sonrası): 70 sayfa; başlangıçlar `{1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}`. Değişmeli; değişirse lead ifadeyi kendi başına kısaltmaz, kullanıcıya sorar | Bölüm başlangıçları TASK-005/015'in sayfa çapası | — |
| 13 | 11. bölüm (Terimler sözlüğü) ve 10. bölümün düzyazısı D13/D14 kapsamında değildir. "Bölümde olup tarifede olmayan ücret" yönü taranmaz | TASK kapsamı Kural 1 ve 2. Sözlükteki değerler planlamada elle karşılaştırıldı, tutarlı | — |

## Sözleşme değişiklikleri
Yok.

## Backend planı
Yok.

## Frontend planı
Yok.

## Lead uygulama maddeleri
- **L-1 Baz:** `git status` temiz. Mevcut iki PDF'i scratch'e al; SHA-256'lar TASK-023 raporundakiyle aynı olmalı (`7585c2ab…827c`, `4db9c9e8…e20b`). `dogrula.sh` 11/11 GEÇTİ ve D9 haritası Kararlar #12 ile aynı.
- **L-2 Kontroller:** D13, D14 (Kararlar #1-#9); `[TARIFE]` 73 girdi; README "Kontroller" tablosuna D13, D14 satırı, `kontroller.txt` açıklamasına `[TARIFE]`, "Yazım kuralları"na madde: "Sık sorulanlar cevabındaki her tutar, oran, süre ve saat, cevabın atıf yaptığı başlıklardan birinde yazılıdır; yeni tarife satırı için `[TARIFE]`'ye kalıp eklenir." `dogrula.sh` yalnız 2. satır yorumu.
- **L-3 İlk çalıştırma (içerik değişmeden):** D1-D12 GEÇTİ. Beklenen: D13 GEÇTİ; D14 KALDI ve bulgular tam R1-R5 (soru 1 = B ise yalnız R4). D13 satır sayısı 72/72, kaynaktaki tarife satırlarıyla karşılaştırılır. Fazladan bulgu → uygulama sırası 3.
- **L-4 Kalıp ayarı:** Kararlar #10 sınırında; değişiklikler rapora.
- **L-5 İçerik:** kart sorusu 1'in seçeneği (Kararlar #11) ve kapıda onaylanan ek düzeltmeler. Yalnız onaylanan satırlar değişir.
- **L-6 Üretim ve doğrulama:** `uret.sh`; `dogrula.sh` 13/13 GEÇTİ. D11: ikinci üretim scratch'e + `cmp`. D8 sayfa yüzünden kalırsa yalnız `degisiklik-ozeti-2026.11.md`'deki sayfa numaraları güncellenir. K-1.
- **L-7 Negatif denemeler:** D-N3…D-N7, her biri `docs/rehber`'in scratch kopyasında; repo dosyaları değişmez.
- **L-8 Kullanıcı içerik onayı:** değişen cevaplar PDF'ten çıkarılmış hâliyle, iki sürümdeki sayfasıyla gösterilir; onay tarihi rapora.
- **L-9 Rapor** `handoff/02-lead-implementation.md` (ilk satır `TASK: TASK-020`, son satır `STATUS: DONE`/`NEEDS_FIX`): L-1 sonuçları; L-3 çıktısı ve bulgu listesi; kalıp ayarları; kaynak diff; `uret.sh`/`dogrula.sh` tam çıktısı; yeni SHA-256'lar; K-1, K-2; D-N3…D-N7 çıktıları; `yalniz-tarife` listesi; kullanıcı onay satırları.
- **Sınırlar:** değişebilir: `dogrulama/RehberDogrulama.java`, `dogrulama/kontroller.txt`, `README.md`, `dogrula.sh` (2. satır), `kaynak/bolumler/02,05,06,08-*.typ` (yalnız onaylı SSS satırları), iki PDF, `degisiklik-ozeti-2026.11.md` (yalnız sayfa). Değişmez: `surum.typ`, `ortak.typ`, `rehber.typ`, `uret.sh`, `rehberde-olmayan-konular.md`, `kaynaklar/**`, servisler, `docs/api/**`.

## Test senaryoları
Otomatik uygulama testi yok. D1-D14 `dogrula.sh` kontrolleri; D11, D-N ve K maddeleri lead'in elle çalıştırdığı komutlar; Q maddeleri qa'nın.

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| D1-D12 | Mevcut kontroller | doğrulama | (değişmedi) | GEÇTİ. D7: soru 1 = A ise iki sürümde 317 atıf (312 + 5) |
| D13 | Tarife ↔ bölüm (K1, S1, G1) | doğrulama | `givenTariffAndChapters_whenFactsCompared_thenEveryTariffValueMatchesChapterMentions` | GEÇTİ; iki sürümde 72 satır; `Raşit Platinum ek kart yıllık ücreti` ve `Kâğıt ekstre (gönderim başına)` doğrulananlar arasında; `yalniz-tarife` listesi raporda |
| D14 | SSS ↔ atıf (K2, G3) | doğrulama | `givenFaqAnswers_whenValuesComparedWithCitedSections_thenEachValueFound` | Düzeltmeden önce KALDI, bulgular R1-R5; düzeltmeden sonra GEÇTİ, iki sürümde 105 soru |
| D-N3 | S2 (K4) | doğrulama | `givenFaqWithConflictingCashAdvanceFee_whenDogrulaRun_thenD13AndD14FailShowingBothPlaces` | Kopyada 2. bölüm SSS "Nakit avans faizi ne kadar?" cevabındaki `Ayrıca çekilen tutarın %1,25'i, en az 15,00 TL işlem ücreti alınır` → `Ayrıca nakit avans işlem ücreti %1,5'tir`. Çıkış 1. D13 KALDI: SSS yeri (%1,5) ve 10.1 (%1,25); D14 KALDI: %1,5 2.10'da yok, 2.10'daki yüzdeler listede (%1,25 dahil). İki sürümde |
| D-N4 | Sürüm farkı (K3, G1) | doğrulama | `givenHardcodedOldGoldFeeInFaq_whenDogrulaRun_thenOnlyNewVersionFails` | Kopyada Gold SSS cevabında `#S.gold-yillik-ucret` → `1.450 TL`. 2026.10 için D13/D14 bulgusu yok; 2026.11 için D13 (SSS 1.450 vs 10.1 1.650) ve D14 KALDI; D8 de KALDI. Çıkış 1 |
| D-N5 | Kataloğu olmayan tarife satırı (K1, G4) | doğrulama | `givenTariffRowWithoutCatalogEntry_whenDogrulaRun_thenD13FailsNamingRow` | Kopyada 10.1'e `("Kart aidatı gecikme ücreti", "50 TL")` satırı. D13 KALDI, satırın etiketi yazılı. Çıkış 1 |
| D-N6 | Tarife değeri tek yerde değişti (K1) | doğrulama | `givenTariffValueChangedOnlyInChapterTen_whenDogrulaRun_thenD13FailsShowingChapterMention` | Kopyada 10.1 `Kâğıt ekstre (gönderim başına)` → `5,90 TL`. D13 KALDI: 2.7 (4,90 TL) ve 10.1 (5,90 TL). D4 de KALDI. Çıkış 1 |
| D-N7 | Sessiz kalıp (G5) | doğrulama | `givenCatalogPatternMatchingNothing_whenDogrulaRun_thenD13FailsNoMentionFound` | Kopyadaki `kontroller.txt`'de kâğıt ekstre kalıbı hiçbir yere uymayan bir ifade yapılır, kopyadaki `dogrula.sh` çalışır. D13 KALDI "bölümlerde geçiş bulunamadı". Çıkış 1 |
| K-1 | Sayfa düzeni (Kararlar #12) | doğrulama | `givenNewPdfs_whenLayoutCompared_thenPageCountAndChapterStartsUnchanged` | 70 sayfa; başlangıç haritası baz değerle aynı; değişmeyen sayfalar önceki PDF'lerle aynı, farklı sayfalar yalnız onaylı cevapların sayfaları |
| K-2 | Kaynak diff (K6) | doğrulama | `givenSourceDiff_whenInspected_thenOnlyApprovedFaqLinesChanged` | `git diff docs/rehber/kaynak` yalnız onaylı satırlar (A: 5 satır, 4 dosya) |
| Q-1 | Sınırlar | qa elle | — | Diff yalnız "Sınırlar"daki dosyalarda; servis, `docs/api/`, `kaynaklar/` yok |
| Q-2 | Onay izi (K6, AC3) | qa elle | — | Her içerik değişikliğinin kartta ya da raporda kullanıcı onayı var; olgu değeri değişmemiş |
| Q-3 | Bağımsız tekrar (K4, K5, AC1, AC2) | qa elle | — | qa `uret.sh` + `dogrula.sh`: 13/13 GEÇTİ, SHA-256 lead'inkiyle aynı; D-N3'ü kendi tekrarlar ve kendinden bir mutasyon daha ekler |
| Q-4 | Katalog incelemesi (Kararlar #10) | qa elle | — | 73 girdiden en az 15'inin kalıbı PDF'te doğru yerleri buluyor; `yalniz-tarife` satırları gerçekten bölümlerde geçmiyor |

## Deployment planı
Yok.

## Açık sorular ve riskler
1. **Kalıp hassasiyeti (risk):** D13 elle yazılmış konu kalıplarına dayanır; çok dar kalıp bir geçişi kaçırabilir. Kapsam şartı (Kararlar #4e) ve Q-4 bunu azaltır, sıfırlamaz.
2. **Sayfa kayması (risk):** atıf eklemek satır akışını değiştirebilir (Kararlar #12).
3. **Kapsam dışı not:** 2. bölüm SSS "Kartımın borcunu başka bir bankadan ödeyebilir miyim?" cevabı "Ödeme, gönderildiği anda kartınıza yansır" der; 2.8'de bu süre yok ve EFT yalnız iş saatlerinde gider (5.2). Sayısal değer olmadığı için kontrol yakalamaz; değişiklik önerilmiyor.
4. **Kapsam dışı not:** Kredi kartı gecikme faizi (2.8, %4,05) 4.11'deki "akdi faizin %30 fazlası" kuralına uymaz (o kural kredi taksitleri içindir); çelişki değil ama sözlükteki "Gecikme faizi" yalnız 4.11'i gösteriyor.
5. **Kapsam dışı:** "Örnek:" hesaplarının aritmetiği ve bölümde olup tarifede olmayan ücretler taranmaz. Betiklerin genel sağlamlaştırılması TASK-021'dedir.

STATUS: DONE
