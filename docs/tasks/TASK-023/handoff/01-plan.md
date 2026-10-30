TASK: TASK-023
# Plan

## Özet
**Mevcut durum:** Rehberin kaynağında (`docs/rehber/kaynak/bolumler/02-kartlar.typ`) gerçek kart şeması adı "Mastercard" 4 cümlede geçiyor. Bu cümleler sürüme bağlı değil (`surum.typ`'den gelmiyor), bu yüzden iki PDF'te de var:

| Yer | Satır | Mevcut cümle | PDF sayfası (TASK-004) |
|-----|-------|--------------|------------------------|
| M1 · 2.1 | 6 | `Kart Mastercard altyapısıyla çalışır, temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir.` | 7 |
| M2 · 2.2 | 38 | `Raşit Bankası'nın bireysel kredi kartlarının hepsi Mastercard altyapısıyla çalışır, temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (bkz. 2.11).` | 7 |
| M3 · 2.14 | 298 | `Kartlarınız yurt dışında Mastercard logosu bulunan her yerde kullanılabilir.` | 2.14'ün sayfası (lead raporuna yazar) |
| M4 · 2.17 | 369 | `Raşit Bankası, işlemi yapan işyerinin bankasından Mastercard kuralları çerçevesinde belge ister.` | 2.17'nin sayfası (lead raporuna yazar) |

Planlama sırasında kaynakta Visa, American Express, Amex, Troy, Maestro, UnionPay, JCB, Diners aranmış, hiçbiri bulunmamıştır. `dogrulama/kontroller.txt` `[GERCEK_BANKA]` listesinde 24 banka adı var, kart şeması yok.

**Yeni durum:**
- 4 cümle, kullanıcının onay kartında seçtiği ifadeyle yazılır (kart soruları 1-4). Başka hiçbir metin değişmez.
- `[GERCEK_BANKA]` listesine kart şeması adları eklenir (Kararlar #3). D12 bunları iki PDF'te arar.
- İki PDF `uret.sh` ile yeniden üretilir. `dogrula.sh`'deki 11 kontrolün hepsi GEÇTİ der. Bölüm başlangıç sayfaları ve 2. bölümün sayfa sayısı (13) değişmez.
- **Değişen servis yok.** assistant-api, retrieval-api, web, `docs/api/**` ve Docker etkilenmez. `RehberDogrulama.java` değişmez (Kararlar #4).

**Ön koşul (Kural 6):** Rehber henüz bilgi bankasına alınmadı. TASK-005 açık: `docs/tasks/TASK-005/handoff/` yok, son commit TASK-004'e ait. Lead uygulamaya başlamadan bunu yeniden kontrol eder (L-1).

**Uygulama sırası:**
1. Kullanıcı onayı (kart, 4 ifade seçimi).
2. **lead:** L-1…L-7, rapor `handoff/02-lead-implementation.md`.
3. **Kullanıcı içerik onayı** (Kural 5; görevin sahibi lead): lead değişen 4 cümleyi PDF'ten çıkarılmış hâliyle ve sayfa numaralarıyla gösterir, onayı rapora yazar.
4. **qa:** `handoff/05-qa-report.md`.

## Çalışacak roller
- **planner:** bu plan ve onay kartı.
- **lead (ana oturum):** `docs/rehber/**` rol tablosunda sahipsiz bir yoldur; TASK.md "lead uygular" diyor (TASK-004'teki düzen). L-1…L-7'yi uygular, `handoff/02-lead-implementation.md`'yi yazar, kullanıcı içerik onayını alır.
- **qa:** PDF'leri kendi başına yeniden üretir, kontrolleri çalıştırır, diff'i inceler, kabul kriterlerini işaretler (Q-1…Q-4). Kök `CLAUDE.md` madde 2 gereği qa, lead'in adımına ve kullanıcı içerik onayına bağlanır.
- **Atlananlar:**
  - backend-coder: servis kodu değişmez.
  - frontend-coder: ekran yok.
  - tester: uygulama kodu ve otomatik uygulama testi yok. Kontroller `docs/rehber/` altındadır, tester'ın test yollarında değildir. Lead çalıştırır, qa bağımsız yeniden çalıştırır.
  - devops: Docker, model, ortam değişkeni ve kök `README.md` değişmez.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi kontrolle doğrulanır |
|---|-----------------|------------------|----------------------------|
| K1 | Gerçek kart şeması markaları (Mastercard, Visa, American Express, Troy) iki PDF'te geçmez; `dogrula.sh` D12 listesine eklenir | L-3 (`kontroller.txt` `[GERCEK_BANKA]`); Kararlar #3, #4, #5 | D12, D-N2, K-3 |
| K2 | Yerine gelecek ifade onay kartında önerilir, kullanıcı seçer | Kart soruları 1-4; L-2 seçilen ifadeyi birebir yazar | K-2 (diff'teki 4 satır kartta seçilen ifadeyle aynı), Q-2 |
| K3 | Olgular değişmez: yurt dışında kullanılabilme, ücretler, itiraz süreleri aynı | L-2: sadece M1-M4 cümleleri değişir; Kararlar #1 | K-1 (2. bölüm gövde metni eskisinden yalnız 4 cümleyle ayrılır), K-2, D4, D8 |
| K4 | 2. bölüm iki sürümde aynı sayfa sayısını korur; bölüm başlangıçları ve D9 sayfaları değişmez; değişiklik özetindeki sayfalar kayarsa güncellenir | L-4, L-5; Kararlar #6 | D1, D9, K-1 (TASK-004 baz değerleriyle karşılaştırma), D8 |
| K5 | PDF'ler `uret.sh` ile üretilir, `dogrula.sh` bütün kontrollerden geçer, değişen metin kullanıcıya gösterilir | L-4, L-5, L-7; uygulama sırası 3 | D1-D12 (11/11 GEÇTİ), D11, Q-3 |
| K6 | TASK-005'ten önce yapılır | L-1 | L-1 sonucu raporda, Q-1 |
| S1 | "Yurt dışında banka kartımı kullanabilir miyim?" → 2.14 marka adı olmadan aynı olguyu anlatır: yurt dışında kullanılabilir, önce açılması gerekir, %1,99 ücret | M3 (2.14 ilk paragraf); 2.14'ün geri kalanı değişmez | K-1, K-4 |
| AC1 | `dogrula.sh` bütün kontrollerden geçiyor, yeni marka kontrolü dahil | L-3, L-5 | D1-D12, D-N2 |
| AC2 | İki PDF'in metninde "Mastercard" geçmiyor | L-2 | D12, K-3 |
| AC3 | Kullanıcı değişen metni onaylamış | Uygulama sırası 3 | Q-3 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan kontrol |
|---|-------|-------------------|--------------------|
| G1 | Büyük harfli yazım: D12 metni ve listeyi `toLowerCase(tr)` ile küçültür. "VISA" → `vısa`, "AMERICAN EXPRESS" → `amerıcan express` olur, listedeki "Visa" ise `visa` olur. Eşleşmez, kontrol kaçırır | Listeye büyük harfli biçimler de girer: `VISA`, `AMERICAN EXPRESS` (Kararlar #3). Mastercard, Troy, Amex gibi "I" içermeyen adlarda sorun yok | D-N2 (`VISA` ile) |
| G2 | Türkçe ek: "Mastercard'ın", "Visa'yla". D12 tam kelime arar (önü ve arkası harf ya da rakam olmamalı) | Kesme işareti (`'` ya da `’`) harf değildir, kelime sınırı sayılır ve yakalanır. Kesmesiz yazım ("Mastercardla") D12'den kaçar; bunu lead'in tek seferlik alt dize araması yakalar | D-N2 (`Mastercard'ın` ile), K-3 |
| G3 | Yanlış alarm: "Troy" başka anlamda ya da başka bir sözcüğün parçası ("Troya", "troyka"), "Maestro" müzik anlamında | Tam kelime araması "Troya"/"troyka"yı eşlemez. Rehberde bu sözcükler geçmiyor (planlamada kaynakta arandı). D12 yeni ad yüzünden KALDI derse lead adı listeden çıkarmaz; geçişi inceler ve raporda yazar | D12 |
| G4 | Cümle uzunluğu değişince sayfa akışı kayar. 2.2 tablosu s. 7'nin altına sığmadığı için s. 8'de başlıyor (TASK-004 qa Nit). M1 kısalır ya da uzarsa tablo s. 7'ye geçebilir, 2. bölüm SSS'i (F1.2, s. 18) kayabilir | Bölüm başlangıçları ve 2. bölümün 13 sayfası korunur. Değişiklik özetindeki sayfa kayarsa "Ayrıntı" tablosu ve "Kısa özet" parantezleri gerçek çıktıdan güncellenir | D8, D9, K-1 |
| G5 | Değişiklik özetine yanlışlıkla "fark" olarak girmesi. Bu düzeltme iki sürüme birden uygulanır, sürümler arası fark değildir | Özete satır eklenmez, sadece kayan sayfa numaraları güncellenir (Kararlar #2) | D8, Q-2 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | Değişiklik yalnız M1-M4 cümlelerindeki marka ifadesini değiştirir. Cümlenin geri kalanı, liste maddeleri, tablolar ve 2.14/2.17'nin diğer paragrafları aynı kalır | Kural 3. Her cümlede değişen kısım kartta gösterilir, kullanıcı tam olarak neyi onayladığını görür | 2.14'ü yeniden yazmak: S1'in dayandığı olgular (önce açma, %1,99) gereksiz yere riske girer |
| 2 | Değişiklik iki sürüme birden uygulanır, `surum.typ`'ye girmez. `degisiklik-ozeti-2026.11.md`'ye fark satırı eklenmez | 2026.10 ile 2026.11 arasında fark değil, iki sürümün ortak metninin düzeltmesi. Özete girseydi D8 eski metni 2026.11'de, yeni metni 2026.10'da yok sanıp kalırdı | 2026.10'da Mastercard bırakmak: Kural 1 iki PDF'i de kapsıyor |
| 3 | `[GERCEK_BANKA]`'ya eklenecek 11 satır: `Mastercard`, `Visa`, `VISA`, `American Express`, `AMERICAN EXPRESS`, `Amex`, `Troy`, `Maestro`, `UnionPay`, `JCB`, `Diners Club`. Bölüm yorumu "gerçek banka ve kart şeması adları" olur. Liste 24 → 35 | TASK'taki 4 marka zorunlu. Büyük harfli iki biçim G1 için. Amex, Maestro (Mastercard'ın banka kartı markası), UnionPay, JCB ve Diners Club, Türkiye'de kabul gören diğer kart şemaları. Hiçbiri rehberde geçmiyor, yanlış alarm riski yok | Sadece 4 marka: Amex kısaltması ve Maestro, banka kartı metninde en olası gerçek adlar, kaçar. "Discover" eklenmez: İngilizce sıradan sözcük, eklemek kural kazandırmaz. Türk bankalarının kart program adları (Bonus, World, Maximum, Axess, Paraf, CardFinans, Bankkart) eklenmez: kart şeması değiller, TASK kapsamında değiller. Bonus/maximum/world sıradan sözcük olduğu için yanlış alarm riski de var (bkz. Açık sorular 2) |
| 4 | `RehberDogrulama.java` değişmez. D12'nin eşleşme kuralı (Türkçe küçük harf + tam kelime) ve çıktı metni ("N gerçek banka adı geçmiyor") aynı kalır; N 35 olur | Kural 1 "listeye eklenir" diyor. G1 kod değişmeden, listeye biçim eklenerek çözülüyor. Program değişmeyince TASK-021'in (betik sağlamlaştırma) alanına girilmez | D12'yi "ı→i" eşlemesiyle değiştirmek: 24 banka adının eşleşme davranışı da değişir, TASK-021 ile çakışır. Ayrı `[KART_SEMASI]` bölümü: Java değişikliği gerektirir |
| 5 | K-3: lead, L-4'ten sonra kaynakta (`docs/rehber/kaynak/`, `*.md`; `dogrulama/kontroller.txt` hariç) ve iki PDF'in çıkarılmış metninde büyük/küçük harf duyarsız alt dize araması yapar: `mastercard`, `visa`, `american express`, `amex`, `troy`, `maestro`, `unionpay`, `jcb`, `diners`. Beklenen 0. Çıkan her geçiş raporda tek tek incelenir | G2'deki kesmesiz ekleri ve D12'nin tam kelime sınırının kaçırdığı biçimleri tek seferde yakalar | Bunu D12'ye kalıcı kontrol olarak eklemek: "visa" alt dizesi ileride gelecek bir sözcükte yanlış alarm verebilir, Java değişikliği gerektirir |
| 6 | Sayfa düzeninin baz değerleri TASK-004 lead raporundandır: 70 sayfa; bölüm başlangıçları `{1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}`. D9 yalnız iki sürümü birbiriyle karşılaştırır, eski PDF'le karşılaştırmaz. Bu yüzden K-1'de lead, D9 çıktısındaki başlangıç haritasını bu değerle karşılaştırır, sayfaları da önceki PDF'lerle tek tek karşılaştırır | Kural 4 "değişmez" der, yani önceki hâle göre. D9 tek başına iki sürümün birlikte kaymasını yakalamaz | D9'u baz değerle karşılaştıracak şekilde değiştirmek: kalıcı bir kontrol olarak her içerik değişikliğinde (TASK-020) güncellenmesi gerekir; tek seferlik karşılaştırma yeterli |
| 7 | Önceki PDF'ler, L-1'de değişiklikten önce `git show HEAD:docs/rehber/pdf/<dosya>` ile scratch'e alınır. Karşılaştırmada metin, `dogrula.sh`'nin indirdiği PDFBox 3.0.6 jar'ının `export:text` komutuyla (ya da scratch'te tek seferlik eşdeğer bir programla) sayfa sayfa çıkarılır. Eski ve yeni dosya aynı araçla okunur | K-1 için eski çıktı gerekir. Repoya yeni betik girmez | Repoya kalıcı karşılaştırma betiği eklemek: kapsam dışı |
| 8 | README güncellenir: D12 satırı "gerçek banka ve kart şeması adı geçmiyor" olur. "Yazım kuralları"na bir madde eklenir: gerçek kart şeması markası yazılmaz, yerine kartta seçilen ifade kullanılır | Sonraki içerik task'ları (TASK-020) kuralı görür | — |

## Sözleşme değişiklikleri
Yok. `docs/api/openapi.yaml`, `sse.md` ve `websocket.md` değişmez.

## Backend planı
Yok.

## Frontend planı
Yok.

## Lead uygulama maddeleri
- **L-1 Ön koşul ve baz:**
  - TASK-005'in kapanmadığını doğrula: `docs/tasks/TASK-005/handoff/` yok ve `git log` içinde `TASK-005` commit'i yok. Varsa dur ve kullanıcıya sor (Kural 6).
  - Önceki iki PDF'i scratch'e al (Kararlar #7). SHA-256'larını rapora yaz; TASK-004 raporundakiyle aynı olmalı (`752abfa3…cb03`, `3688ad34…5937`).
  - `grep -n Mastercard docs/rehber/kaynak/bolumler/02-kartlar.typ` ile tam 4 satır (6, 38, 298, 369) çıktığını gör.
- **L-2 Kaynak:** M1-M4'ü onay kartında seçilen ifadeyle değiştir. Seçilen ifade birebir yazılır; Typst biçimlemesi (`#bkz(...)`) korunur. Başka satıra dokunulmaz. C (kurgusal ad) seçildiyse ad 4 yerde aynı yazılır; lead adın gerçek bir markaya ya da kuruma ait olmadığını web aramasıyla kontrol eder ve raporda yazar (TASK-004 Kararlar #14 düzeni).
- **L-3 `dogrulama/kontroller.txt`:** `[GERCEK_BANKA]`'ya Kararlar #3'teki 11 satır eklenir, bölüm yorumu güncellenir. Başka bölüm değişmez.
- **L-4 Üretim:** `./docs/rehber/uret.sh`. Çıktıda iki PDF de 70 sayfa olmalı. D11: ikinci üretim scratch'e (`uret.sh <scratch>/ikinci`) yapılır ve `cmp` ile karşılaştırılır.
- **L-5 Doğrulama ve sayfa düzeni:**
  - `./docs/rehber/dogrula.sh` çalıştırılır. D8 sayfa yüzünden kalırsa `degisiklik-ozeti-2026.11.md`'deki "Ayrıntı" tablosunun sayfa sütunları ve "Kısa özet"teki parantezler gerçek çıktıdan güncellenir, kontroller yeniden çalıştırılır. Metin hücreleri değişmez.
  - K-1, K-3 ve D-N2 çalıştırılır.
  - Bölüm başlangıçları baz değerden saparsa ya da 2. bölüm 13 sayfa kalmazsa lead seçilen ifadeyi değiştirmez. Bölüm sonundaki boşluk payı yetmiyorsa `NEEDS_FIX` yazar (sorumlu planner) ve kullanıcıya sorulur (TASK-004 Açık sorular 6 düzeni).
- **L-6 README:** Kararlar #8.
- **L-7 Rapor `handoff/02-lead-implementation.md`** (ilk satır `TASK: TASK-023`, son satır `STATUS: DONE`/`NEEDS_FIX`). İçerik:
  - L-1 sonuçları;
  - M1-M4 için eski ve yeni cümle, PDF'ten çıkarılmış hâliyle ve iki sürümdeki sayfa numarasıyla;
  - `git diff --stat` ve `02-kartlar.typ` diff'i;
  - `uret.sh` ve `dogrula.sh` tam çıktıları;
  - D11 `cmp` sonucu, yeni SHA-256'lar;
  - K-1 tablosu (sayfa → aynı/farklı), K-3 ve D-N2 çıktıları;
  - değişiklik özetinde güncellenen sayfalar (varsa);
  - kullanıcı içerik onayı satırı (tarih).
- **Sınırlar:** `kaynaklar/**`, servis kodu, `docs/api/**`, `RehberDogrulama.java`, `uret.sh`, `dogrula.sh`, `surum.typ`, `rehberde-olmayan-konular.md`, kök `CLAUDE.md` ve `rag.md` değişmez. Değişebilecek dosyalar: `kaynak/bolumler/02-kartlar.typ`, `dogrulama/kontroller.txt`, `README.md`, iki PDF ve sayfa kayarsa `degisiklik-ozeti-2026.11.md`.

## Test senaryoları
Otomatik uygulama testi yok. D1-D12 `docs/rehber/dogrula.sh` kontrolleridir; mevcut test adları TASK-004 planındaki gibidir. D11, D-N2 ve K-1…K-4 lead'in elle çalıştırdığı komutlardır. Q-1…Q-4 qa'nın kontrolleridir. "İki PDF" = 2026.10 ve 2026.11.

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| D1-D10 | Mevcut kontroller (TASK-004 planı) | doğrulama | (değişmedi) | Hepsi GEÇTİ. D1: `2026.10=70, 2026.11=70`. D4: 29 satır kontrolü. D6: 110 / 109 numaralı başlık. D7: iki sürümde 312 atıf. D9: başlangıç haritası Kararlar #6'daki değerle aynı, 37 sayfa birebir. D10: 54 yasak kelime |
| D12 | Kurgu sınırı + kart şemaları (K1, AC1, AC2) | doğrulama | `givenCoverAndBody_whenFictionRulesChecked_thenDisclaimerPresentAndNoRealBankNames` | GEÇTİ; çıktıda "35 gerçek banka adı geçmiyor"; adresler yalnızca `rasitbank.example` |
| D11 | Tekrar üretim (K5) | doğrulama | `givenCleanCheckout_whenUretRunTwice_thenOutputsIdentical` | İki üretim `cmp` ile bayt bayt aynı. Yeni SHA-256'lar eskisinden farklı (metin değişti) |
| D-N2 | Yeni marka kontrolü gerçekten yakalıyor (K1, G1, G2) | doğrulama | `givenCardSchemeNamesInSource_whenDogrulaRun_thenD12FailsListingEachName` | `docs/rehber` scratch'e kopyalanır. Kopyada M3 cümlesi `Kartlarınız yurt dışında VISA, Troy ve Mastercard'ın logosu bulunan her yerde kullanılabilir.` yapılır, PDF'ler scratch'e üretilir. Repodaki `dogrula.sh <scratch-pdf>`: D12 KALDI, ayrıntıda iki sürüm için `Mastercard`, `VISA`, `Troy`. Çıkış kodu 1. Repo dosyaları değişmez |
| K-1 | Sayfa düzeni ve olgular önceki hâle göre (K3, K4, S1) | doğrulama | `givenPreviousPdfs_whenPagesCompared_thenOnlyChapterTwoSentencesDiffer` | Her sürüm için önceki ve yeni PDF sayfa sayfa karşılaştırılır. (a) Sayfa sayısı 70, bölüm başlangıçları baz değerle aynı. (b) Sayfa 1, 5-6 ve 20-70'in metni, üst ve alt bilgi dahil, birebir aynı. (c) Sayfa 2-4 (İçindekiler) aynı; fark varsa yalnızca bir 2.x satırının sayfa numarasındadır ve raporda listelenir. (d) Sayfa 7-19'un gövde metni (üst/alt bilgi çıkarılmış, sayfalar birleştirilmiş, boşluklar tek boşluğa indirilmiş) eskisinden yalnız M1-M4 değişimleriyle ayrılır: eski metinde 4 eski ifade yeni ifadeyle değiştirilince yeni metin elde edilir |
| K-2 | Kaynakta yalnız 4 satır değişti (K2, K3) | doğrulama | `givenSourceDiff_whenInspected_thenOnlyFourSelectedSentencesChanged` | `git diff --numstat docs/rehber/kaynak`: yalnız `02-kartlar.typ`, `4 4`. Diff'teki yeni satırlar kartta seçilen ifadelerle birebir aynı |
| K-3 | Kesmesiz ek ve diğer biçimler (G2, AC2) | doğrulama | `givenSourceAndExtractedText_whenCaseInsensitiveSearched_thenNoCardSchemeName` | Kararlar #5'teki 9 dize kaynakta (`kontroller.txt` hariç) ve iki PDF'in çıkarılmış metninde 0 kez geçer |
| K-4 | Örnek senaryo S1 | doğrulama | `givenNewPdfs_whenSectionTwoFourteenRead_thenForeignUseFactsUnchanged` | Her iki PDF'in 2.14 metninde şunlar var: M3'ün yeni hâli, `seyahatten önce Raşit Mobil'de kart ayarlarından "Yurt dışı kullanım" seçeneğini açmanız gerekir`, `işlem tutarının %1,99'u kadar yurt dışı işlem ücreti alınır`. 2.17'de `en geç 120 gün içinde` ve `Süreç ortalama 45 gün, yurt dışı işlemlerde 90 güne kadar sürebilir` aynen var |
| Q-1 | Ön koşul ve sınırlar (K6) | qa elle | — | TASK-005 kapanmamış. `git status` yalnız "Sınırlar"daki dosyaları ve `handoff/`'u gösteriyor. `kaynaklar/`, servisler, `docs/api/` ve `RehberDogrulama.java` diff'i yok |
| Q-2 | İfade ve kapsam (K2, K3, G5) | qa elle | — | Diff'teki 4 cümle kartta seçilen ifadeyle aynı. Değişiklik özetine fark satırı eklenmemiş, değişen yalnız sayfa numaraları (varsa). `kontroller.txt`'de 11 yeni satır var |
| Q-3 | Bağımsız üretim ve içerik onayı (K5, AC1, AC3) | qa elle | — | qa `uret.sh` ve `dogrula.sh`'yi kendi çalıştırır: 11/11 GEÇTİ, SHA-256'lar lead raporundakiyle aynı. Lead raporunda kullanıcı içerik onayı satırı var. Onaydan sonra kaynak değiştiyse kontroller yeniden çalıştırılmış |
| Q-4 | Görsel kontrol (K4) | qa elle | — | Değişen 4 cümlenin bulunduğu sayfalar ve s. 7-8 PDF görüntüleyicide açılır: tablo bölünmemiş, bölüm 3 s. 20'de başlıyor. Metin K-3'teki adlar için PDF görüntüleyicide aranır: sonuç yok |

## Deployment planı
Yok.

## Açık sorular ve riskler
1. **Sayfa kayması (risk):** M1/M2'nin uzunluğu s. 7'nin akışını değiştirebilir. Değişiklik özetindeki sayfalar güncellenir (L-5). Bölüm başlangıcı kayarsa `NEEDS_FIX`'e gider (L-5).
2. **Kapsam dışı not:** Türk bankalarının kart program adları (Bonus, World, Maximum, Axess, Paraf, CardFinans, Bankkart) D12'de yok. Bugün rehberde de geçmiyorlar (planlamada arandı). Sıradan sözcük oldukları için tam kelime listesine eklemek yanlış alarm üretebilir. Gerekirse ayrı bir takip task'ı olur.
3. **Kapsam dışı not:** D12 eşleşmesinin Türkçe "I/ı" duyarlılığı genel bir betik sorunudur: listedeki "I" içeren mevcut banka adları (ör. `ING`) metindeki karışık harfli yazımda kaçabilir. Bu task'ta yalnız yeni adlar için listeye biçim eklenerek çözülür (Kararlar #3, #4). Genel çözüm TASK-021'in (betik sağlamlaştırma) kapsamına eklenebilir; kullanıcı karar verir.
4. **Lab dosyaları:** `kaynaklar/**`'daki eski lab PDF'leri ya da HTML'de marka adı olabilir; TASK kapsam dışı sayıyor, bakılmadı.

STATUS: DONE
