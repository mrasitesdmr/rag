TASK: TASK-004
# Plan

## Özet
**Mevcut durum:**
- Repoda rehber yok. Lab'daki iki PDF (`Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf` / `-2026.11.pdf`, 31 sayfa, 61 parça) sadece statik demo içindi; kullanılmaz (TASK "Ne isteniyor").
- `kaynaklar/` değişmez (Kural 8).
- Servis kodu, `docs/api/**`, Docker etkilenmez. **Değişen servis yok** (assistant-api ve retrieval-api aynı kalır).

**Yeni durum:**
- `docs/rehber/` altında (soru 1 = A ise) tek bir Typst kaynağı, iki PDF, değişiklik özeti, "rehberde olmayan konular" notu, üretim ve doğrulama betikleri ve bir README durur.
- İki PDF aynı kaynaktan, sürüm parametresiyle tek komutla üretilir (Kural 6). Sürüme bağlı her değer tek dosyada (`surum.typ`) tutulur; 2026.11 farkları sadece o dosyadan gelir.
- Doğrulama betiği PDF'leri Apache PDFBox 3.0.6 ile okur (TASK-005'in büyük olasılıkla kullanacağı kütüphane; referans projede 3.0.6). Türkçe karakter, tablo satırı, üst/alt bilgi, içindekiler, çapraz atıf, değişiklik özeti, sayfa düzeni, yasak konu ve tekrar üretim kontrollerini yapar (D1-D12).

**Uygulama sırası:**
1. Kullanıcı onayı (kart).
2. **lead**: kaynak, PDF'ler, notlar, betikler; doğrulama ve negatif deneme; rapor `handoff/02-lead-implementation.md`.
3. **Kullanıcı içerik onayı** (Kural 9; görev sahibi lead): lead iki PDF'i ve değişiklik özetini kullanıcıya sunar. Değişiklik istenirse lead düzeltir, D1-D12'yi yeniden çalıştırır, raporu günceller.
4. **qa**: bağımsız yeniden üretim ve doğrulama, örneklem okuma.

## Çalışacak roller
- **planner:** bu plan ve onay kartı.
- **lead (ana oturum):** `docs/rehber/**` sahipsiz bir yoldur (kök `CLAUDE.md` rol tablosunda kimsenin değil). TASK.md içeriği lead'in hazırlamasını kullanıcı kararı olarak yazıyor (TASK-001'deki gibi). Lead L-1…L-8 maddelerini uygular, `handoff/02-lead-implementation.md`'yi yazar ve kullanıcı içerik onayını alır.
- **qa:** `05-qa-report.md`. Yazar ile doğrulayanın aynı kişi olmaması için gerekli. Doğrulama betiğini kendisi çalıştırır, sayfaları örneklemle PDF'e karşı okur, kabul kriterlerini işaretler (Q-1…Q-6). Kök `CLAUDE.md` madde 2 gereği qa doğrudan lead'in adımına (ve kullanıcı içerik onayına) bağlanır.
- **Atlananlar:**
  - backend-coder: servis kodu değişmez. PDF okuma kodu TASK-005'tedir.
  - frontend-coder: ekran yok.
  - tester: uygulama testi yok. Doğrulama betiği `docs/rehber/` altındadır, tester'ın test yollarında değildir; lead yazar ve çalıştırır, qa bağımsız çalıştırır.
  - devops: Docker, model ve `README.md` (kök) değişmez. Kök `README.md`'ye rehber bağlantısı gerekmez; rehberin kendi `docs/rehber/README.md`'si vardır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi kontrolle doğrulanır |
|---|-----------------|------------------|----------------------------|
| K1 | Kurgu sınırı: gerçek banka adı/logosu/kampanyası yok; ücret, faiz, limit, süre gerçekçi ama kurgusal; kapakta "bilgilendirme amaçlıdır" | L-2 kapak metni; Kararlar #14 (kurgusal kimlikler) | D12, Q-4 |
| K2 | Kapsam: 7 kategori ve TASK'taki her alt konu | "İçindekiler" bölümü (kapsam eşlemesi sütunu); L-2 | Q-2 (lead raporundaki "TASK alt konusu → başlık" tablosu, her satırda başlık var), D6 |
| K3 | Bilerek dışarıda bırakılanlar yazılmaz; ayrı notta tutulur | L-4 `rehberde-olmayan-konular.md`; Kararlar #19, #20 | D10, Q-3 |
| K4a | İçindekiler, numaralı bölüm ve alt başlık, sayfa numarası, üst/alt bilgi (rehber adı, sürüm, yürürlük) | L-2; Kararlar #6, #7 | D5, D6 |
| K4b | Ücret ve limit tabloları, madde listeleri, her bölüm sonunda "Sık sorulanlar" | L-2; Kararlar #11, #12 | D4, Q-2 |
| K4c | Çapraz atıflar ("bkz. 4.2") | L-2 (Typst etiket + referans) | D7 |
| K4d | Sayfa sayısı kartta kesinleşir (öneri 60-100) | Kart sorusu 3 | D1 |
| K5 | Metin seçilebilir, taranmış görüntü yok, Türkçe karakterler PDFBox çıktısında bozulmadan, tablolar metin olarak çıkar | Kararlar #4, #5, #12 | D2, D3, D4 |
| K6 | Kaynak ve üretme komutu repoda; iki PDF aynı kaynaktan yeniden üretilir; araç plan kararı (kartta) | L-1, L-5; Kararlar #2, #3; kart sorusu 2 | D11, Q-1 |
| K7 | 2026.11 listelenmiş gerçekçi farklar (ücret: Gold 1.450 → 1.650 TL, 1 Kasım 2026; değişen limit; eklenen madde; kaldırılan madde); ayrı değişiklik özeti, sayfalarıyla | "2026.11 farkları" bölümü; L-3; Kararlar #8, #9, #10; kart sorusu 4 | D8, D9 |
| K8 | `kaynaklar/` değişmez; konum planner önerir, kullanıcı onaylar | Kart sorusu 1; Kararlar #1 | Q-1 (`git status kaynaklar/` boş) |
| K9 | İçindekiler ve örnek bölümler kartta; tam metin PDF'ten sonra kullanıcıya | Kart "İçindekiler taslağı" ve "Örnek bölümler"; uygulama sırası 3 | Q-6 (raporda kullanıcı içerik onayı satırı) |
| S1 | "Kartımı kaybettim" → bildirim kanalları, kartın kapatılması, yeni kartın kaç iş gününde geleceği adım adım | 2.16 (kartta örnek metin) | D4 (kanal tablosu satırı), Q-5 |
| S2 | "Ev kredisini erken kapatırsam ceza öder miyim?" → oran ve hangi kredilerde | 4.10 (kartta örnek metin, tazminat tablosu) | D4 (tazminat tablosu satırları), Q-5 |
| S3 | "Bitcoin alıp satabilir miyim?" → rehberde yok | L-4; Kararlar #20 | D10 |
| S4 | Gold: 2026.10'da 1.450 TL, 2026.11'de 1.650 TL | `surum.typ` `gold-yillik-ucret`; F1 | D8. Cevap davranışı (içe alma + asistan) TASK-015'te doğrulanır |
| AC1 | İki PDF ve kaynakları onaylanan konumda; üretme komutu çalışıyor | L-1, L-5 | D11, Q-1 |
| AC2 | PDFBox ile metin çıkarılmış; Türkçe karakter ve tablo kontrolü raporda | L-6, L-7 | D3, D4; rapor `02-lead-implementation.md`, Q-1 |
| AC3 | Değişiklik özetindeki her fark 2026.11 PDF'inde belirtilen sayfada | L-3 | D8 |
| AC4 | "Rehberde olmayan konular" notu yazılmış | L-4 | Q-3, D10 |
| AC5 | Kullanıcı içeriği onaylamış | Uygulama sırası 3 | Q-6 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan kontrol |
|---|-------|-------------------|--------------------|
| G1 | Üst/alt bilgi ve sayfa numarası her sayfada çıkarılan metne karışır; parçalama (TASK-005) bunları içerik sanar | Üst ve alt bilgi sabit, tek satırlık kalıplardır (Kararlar #7) ve `docs/rehber/README.md`'de birebir yazılıdır; TASK-005 bunları bu kalıpla ayıklayabilir | D5 |
| G2 | PDF'in fiziksel sayfa sırası ile basılı sayfa numarası farklıdır (kapak numarasız, içindekiler romen rakamlı); alıntıdaki "s. 7" yanlış sayfayı gösterir | Tek numaralama, kapak = 1. Basılı numara = PDFBox sayfa sırası (Kararlar #6) | D5, D6 |
| G3 | Çok satırlı tablo hücresi ya da sayfa sonunda bölünen satır, çıkarılan metinde hücreleri karıştırır; parçalayıcı satırı bölemez kuralını (TASK-005 K4) uygulayamaz | Hücreler tek satır; uzun açıklama tablonun altında madde olarak; satır sayfada bölünmez, başlık satırı yeni sayfada tekrarlanır (Kararlar #12) | D4, Q-4 |
| G4 | Dışarıda bırakılan konu rehberde dolaylı geçer ("kripto para hizmeti sunmuyoruz", "bakiyenizi Raşit Mobil'den görürsünüz"); asistan "bulamadım" yerine bu cümleyle cevap verir | Nottaki yasak kelimeler iki PDF'te de geçmez; kişisel veri sorgulama adımları rehberde yoktur (Kararlar #19, #20) | D10 |
| G5 | Türkçe metin çıkarma bozulur: otomatik büyük harfte "i → I", heceleme tiresinin kelimeyi bölmesi ("kre-\ndi"), "fi" bitişik harfi (U+FB01), "İ"nin "I + U+0307" olarak çıkması | Dil `tr`; heceleme kapalı; bitişik harf kapalı; otomatik büyük harf dönüşümü yok (Kararlar #5) | D3 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | Konum `docs/rehber/` (kart sorusu 1, öneri A). Yapı: "Dosya yapısı" bölümü | Sahipsiz doküman alanı, `kaynaklar/` dışında; TASK-005 (dosyadan içe alma) ve TASK-015 (kullanıcının yüklediği dosya) aynı yolu kullanır | Kökte `rehber/`: kökü kalabalıklaştırır. `retrieval-api/src/main/resources/`: backend-coder'ın yolu, kaynak dosyaları uygulama paketine girer |
| 2 | Üretim aracı **Typst** (tek ikili, `brew install typst`; 0.13 ya da üstü; kesin sürüm `docs/rehber/README.md`'ye yazılır). Typst Universe paketleri (`@preview/...`) kullanılmaz | İçindekiler sayfa numarasıyla, numaralı başlık, üst/alt bilgi, tablo (başlık tekrarı), sayfa numaralı çapraz atıf, PDF yer imleri, yazı tipi gömme yerleşik. Derleme ağsız ve deterministik. Kaynak düz metin, diff'i okunur | WeasyPrint (HTML+CSS): iyi sayfa düzeni, ama Python + Pango kurulumu ve sistem yazı tiplerine bağlılık. Pandoc + LaTeX: kurulum GB'larca, bitişik harf ve heceleme ayarı daha zor. Playwright/Chrome yazdırma: içindekilere sayfa numarası ve sürekli üst bilgi için iki geçişli kendi betiğimiz gerekir; Playwright `web/`'in (frontend-coder) paketidir |
| 3 | Tek kaynak, sürüm parametresi: `typst compile --input surum=2026.10 …`. Sürüme bağlı bütün değerler ve açılıp kapanan maddeler `kaynak/surum.typ`'deki tek sözlükte durur; bölüm dosyalarında sürüm adı geçmez (`"2026.11"` aranınca sadece `surum.typ` ve `rehber.typ`'deki parametre okuması çıkar) | Kural 6 "aynı kaynaktan". Farkların tamamı tek dosyada görünür; değişiklik özeti bu dosyayla birebir karşılaştırılabilir | İki ayrı kaynak kopyası: değişmeyen sayfalar sessizce ayrışır |
| 4 | Yazı tipi: Source Sans 3 (panelin yazı tipi, SIL OFL), TTF/OTF dosyaları lisansıyla `kaynak/fontlar/`'da; derleme `--font-path kaynak/fontlar --ignore-system-fonts` ile. Monospace gerekmez (kod yok) | Her makinede aynı çıktı (sistem yazı tipi farkı yok); Türkçe glifler tam; panelle aynı görsel dil. Typst WOFF2 okumaz, `web`'deki `@fontsource` paketi kullanılamaz | Typst'in gömülü Libertinus Serif'i: ek dosya yok ama serif, panelle uyumsuz. Sistem yazı tipi: makineye bağlı |
| 5 | `set text(lang: "tr", hyphenate: false, ligatures: false)`; paragraflar sola dayalı; `upper()`/`smallcaps` gibi otomatik harf dönüşümleri kullanılmaz, büyük harf gereken yer elle yazılır | G5: çıkarılan metin kelime kelime aynı kalır; "İ/ı" doğru; arama ve alıntı eşleşmesi (TASK-016 metin çapası) bozulmaz | Heceleme açık: "kre-\ndi" gibi bölünmüş kelimeler parçalara girer |
| 6 | Tek sayfa numaralaması: kapak 1. sayfadır (numara kapakta görünmez ama sayılır), sonraki her sayfada "Sayfa N / T" | G2: PDFBox'ın sayfa sırası, basılı numara ve alıntıdaki "s. N" aynı | Kapak/içindekiler ayrı numaralı: basılı ve fiziksel numara ayrışır |
| 7 | Üst bilgi (kapak hariç her sayfa, tek satır): `Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm 2026.10`. Alt bilgi: `Yürürlük: 1 Ekim 2026 · Sayfa 7 / 72`. 2026.11'de `Sürüm 2026.11`, `Yürürlük: 1 Kasım 2026`. Bölüm adı üst bilgiye girmez | Kural 4'ün istediği üç bilgi; sabit kalıp TASK-005'te ayıklanabilir (G1) | Üst bilgide bölüm adı: her sayfada parçaya ek gürültü |
| 8 | Her ana bölüm (1-11) ve İçindekiler yeni sayfada başlar. Değişen bölümler (1: 1.1 sürüm ve yürürlüğü anar; 2, 6, 8, 10: farklar) iki sürümde aynı sayfa sayısını korur (bölüm sonunda boşluk payı). Sonuç: toplam sayfa ve bölüm başlangıç sayfaları iki sürümde aynı; değişmeyen bölümlerin (3, 4, 5, 7, 9, 11) sayfa metni üst/alt bilgi hariç birebir aynı | TASK-015/016: değişmeyen sayfaların parçaları aynı kalır, sayfa + metin çapası işaretleri kaymaz (lab notu: "değişmeyen sayfaların metni aynı olduğu için vektörleri de aynı") | Serbest akış: tek bir eklenen paragraf sonraki bütün sayfaları kaydırır |
| 9 | Farklar başlık numaralarını değiştirmez: kaldırılan madde bölümünün son alt başlığıdır (6.11), eklenen ürün mevcut 2.2'nin içine paragraf ve tablo satırı olarak girer | Çapraz atıflar ("bkz. 6.9") iki sürümde aynı başlığı gösterir | Yeni alt başlık numarası (2.3 araya girer): sonraki bütün numaralar ve atıflar kayar |
| 10 | 2026.11 PDF'i eski değerleri anmaz ("eskiden 1.450 TL idi" yok; "Bu sürümde değişenler" sayfası yok). Farklar sadece `degisiklik-ozeti-2026.11.md`'dedir | Rehberde eski değer kalırsa retrieval onu getirir, asistan 1.450 TL diyebilir (S4) | Rehber içinde sürüm notları sayfası: eski değerler aranabilir kalır |
| 11 | "Sık sorulanlar" içerik bölümlerinin (2-9) sonunda, en az 4 soru-cevap. 1 (Bu rehber hakkında), 10 (Ücret tarifesi) ve 11 (Terimler sözlüğü) SSS taşımaz | Tarife ve sözlükte SSS anlamsız tekrar | Her bölümde: tarife/sözlükte yapay soru |
| 12 | Tablolar: hücre metni tek satır (uzun koşul tablonun altında madde ya da dipnot); satır sayfa sonunda bölünmez; başlık satırı yeni sayfada tekrar eder; para birimi sütunda değil hücrede ("1.450 TL") | G3: PDFBox her satırı tek metin satırı olarak çıkarır; parçalayıcı satırı bölmez | Çok satırlı hücre: çıkarılan metinde hücreler iç içe geçer |
| 13 | Biçim: tutarlar `1.450 TL` (binlik nokta, kuruş gerekmedikçe yok, "TL" sonda), oranlar `%3,25`, tarihler `1 Kasım 2026`, süreler "3 iş günü", saatler `09.00-17.00` | Golden set (TASK-016) ve cevaplarda olgular tek biçimde aranır | Karışık biçim ("1450 TL", "TL 1.450"): aynı olgu farklı yazılır |
| 14 | Kurgusal kimlikler: web ve e-posta `rasitbank.example` (RFC 2606 ayrılmış alan adı); çağrı merkezi, SWIFT kodu ve şube adresleri kurgusal; lead her birinin gerçek bir kuruma ait olmadığını arayarak kontrol eder ve raporda yazar. Kamu kurumları (TCMB, BDDK, TMSF, KVKK Kurumu, Tüketici Hakem Heyeti) adıyla anılabilir; banka değillerdir | Kural 1 | Gerçek görünen numara ve kodları kontrolsüz kullanmak |
| 15 | Doğrulama Apache PDFBox **3.0.6** ile (referans projedeki sürüm). `pdfbox-app-3.0.6.jar` betik tarafından Maven Central'dan `~/.cache/rasit-rehber/`'e indirilir, SHA-256'sı betikte sabittir; repoya girmez. Kontroller tek dosyalık Java programıdır (`java -cp <jar> dogrulama/RehberDogrulama.java`; JDK 26 makinede kurulu) | TASK-005 PDF'i büyük olasılıkla aynı kütüphaneyle okuyacak; doğrulama onun göreceği metni görür. Tek dosya Java: sayfa sayısı, sayfa sayfa metin, yer imleri tek API'den. Kök `.gitignore` değişmez | Sadece `export:text` komut satırı: sayfa sayısı ve yer imi kontrolü zor. `pdftotext` (poppler): TASK-005'in kütüphanesi değil |
| 16 | Lead'in uygulama raporu `handoff/02-lead-implementation.md` (ilk satır `TASK: TASK-004`, son satır `STATUS: DONE`/`NEEDS_FIX`) | qa ve kapanış için kalıcı kayıt; kök `CLAUDE.md` madde 4 biçimi | Raporu sadece mesajla vermek: oturumla kaybolur |
| 17 | İki PDF git'e girer (`docs/rehber/pdf/`). Dosya adları lab'dakiyle aynı | TASK-005 dosyadan içe alır, TASK-015'te kullanıcı yükler; yeniden üretmeden kullanılabilir. Typst çıktısı küçük (tahmin < 1 MB) | Sadece kaynak: her kullanıcı Typst kurmak zorunda kalır |
| 18 | Tekrarlanabilirlik: `SOURCE_DATE_EPOCH` (ya da `--creation-timestamp`) sürümün yürürlük tarihine sabitlenir (2026.10 → 2026-10-01T00:00:00Z, 2026.11 → 2026-11-01T00:00:00Z). Hedef: `uret.sh` iki kez çalışınca PDF'ler bayt bayt aynı. Typst sürümü bunu sağlamazsa kabul edilen alt sınır: sayfa sayfa çıkarılan metin aynı; lead hangisinin sağlandığını raporda yazar | Kural 6 "yeniden üretilebilir"in ölçülebilir hâli | Zaman damgası sabitlenmeden: her üretim farklı bayt |
| 19 | Rehber kişisel veri sorgulamayı anlatmaz: "bakiyenizi/hesap hareketlerinizi şuradan görürsünüz" adımı yoktur. "Bakiye" kelimesi sadece genel kavram olarak (eksi bakiye, asgari bakiye) geçer | K3, G4: kişisel bakiye sorusunda asistan "bulamadım" demeli (TASK Kural 3) | Bakiye sorgulama adımını yazmak: soru rehberden cevaplanır, TASK'taki "bulamadım" durumu kaybolur |
| 20 | `rehberde-olmayan-konular.md` her konu için yasak kelime listesi taşır (ör. "kripto", "Bitcoin", "işletme kredisi"); D10 bunları iki PDF'in metninde Türkçe küçük harfe çevirerek arar. "Başka bankaya EFT", "başka banka ATM'si" gibi Raşit hizmetleri yasak kapsamına girmez | G4 | Yasak kelime kontrolü olmadan: dolaylı cevap fark edilmez |
| 21 | Kapakta iki not: "Bu rehber bilgilendirme amaçlıdır." ve "Raşit Bankası kurgusal bir bankadır; bu rehber bir eğitim demosu için hazırlanmıştır." | Kural 1; PDF dışarıda dolaşırsa gerçek banka sanılmaz | Sadece "bilgilendirme amaçlıdır": kurgu açık değil |
| 22 | Bölüm hacmi: alt başlık başına en az bir net olgu (sayı, süre, koşul) | TASK kapsam dışı notu: içerik golden set'e uygun olgular taşır | — |

## Sözleşme değişiklikleri
Yok. `docs/api/openapi.yaml`, `sse.md`, `websocket.md` değişmez.

## Backend planı
Yok.

## Frontend planı
Yok.

## Dosya yapısı (`docs/rehber/`, lead)
| Yol | İçerik |
|-----|--------|
| `README.md` | Ne olduğu; Typst ve JDK kurulumu; `./uret.sh` ve `./dogrula.sh` komutları; kullanılan Typst sürümü ve PDFBox sürümü; üst/alt bilgi kalıpları (Kararlar #7, TASK-005 için); sayı/tarih biçimi (Kararlar #13); yazı tipi lisansı notu |
| `kaynak/rehber.typ` | Ana dosya: `sys.inputs.surum` okur (yoksa ya da tanınmıyorsa derleme hata verir), belge üst verisi (başlık "Raşit Bankası Ürün ve Hizmet Rehberi", yazar "Raşit Bankası A.Ş.", dil `tr`), sayfa düzeni, üst/alt bilgi, başlık numaralama, kapak, içindekiler, bölümleri sırayla içe alır |
| `kaynak/surum.typ` | Sürüm sözlüğü (aşağıdaki "2026.11 farkları" tablosundaki anahtarlar + `etiket`, `yururluk`) |
| `kaynak/bolumler/01-bu-rehber-hakkinda.typ` … `11-terimler-sozlugu.typ` | Bölüm başına bir dosya; adları İçindekiler'deki sırayla |
| `kaynak/fontlar/` | Source Sans 3 TTF/OTF (gereken kalınlıklar) + `OFL.txt` |
| `pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf`, `pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.11.pdf` | Üretilmiş PDF'ler (Kararlar #17) |
| `degisiklik-ozeti-2026.11.md` | Değişiklik özeti (L-3) |
| `rehberde-olmayan-konular.md` | Not (L-4) |
| `uret.sh` | İki PDF'i üretir (L-5) |
| `dogrula.sh`, `dogrulama/RehberDogrulama.java`, `dogrulama/kontroller.txt` | Doğrulama (L-6) |

## Lead uygulama maddeleri
- **L-1 Kurulum:** `brew install typst` (yoksa). `typst --version` ve `java --version` çıktıları rapora. Source Sans 3 dosyaları resmî deposundan (adobe-fonts/source-sans) indirilir; sürümü ve lisans dosyası `kaynak/fontlar/`'a.
- **L-2 Kaynak:** İçindekiler'deki bütün başlıklar, onaylanan sayfa hedefi (soru 3), Kararlar #5-#13, #19, #21, #22. Kartta onaylanan örnek metinler (2.16, 4.10) karttaki hâliyle girer (dil düzeltmesi serbest, olgu değişmez). Çapraz atıflar Typst etiketleriyle yazılır ve metinde "bkz. 4.10" biçiminde görünür (gerekirse "(s. 34)" eklenir).
- **L-3 `degisiklik-ozeti-2026.11.md`:**
  - Bölüm "Kısa özet": fark başına tek satır, lab'daki pencere biçiminde: eski satır / yeni satır + "(s. 9, 66)". TASK-015 yükleme penceresinde bunu kullanır.
  - Bölüm "Ayrıntı" tablosu, sütunlar: `#`, `Tür` (Ücret / Limit / Eklenen / Kaldırılan), `Konu`, `2026.10 metni`, `2026.11 metni`, `Bölüm`, `Sayfa (2026.10)`, `Sayfa (2026.11)`, `Yürürlük`. `… metni` hücreleri PDF'te birebir aranacak dizedir (kaldırılan için 2026.11 hücresi `—`, eklenen için 2026.10 hücresi `—`). Aynı fark birden fazla yerde geçiyorsa (bölüm metni, SSS, tarife) her geçiş ayrı satırdır.
  - Sayfa numaraları PDF üretildikten sonra gerçek çıktıdan yazılır; D8 doğrular.
- **L-4 `rehberde-olmayan-konular.md`:** Kullanıcının onayladığı konu listesi (soru 5). Konu başına: kısa tanım, 2-3 örnek müşteri sorusu, yasak kelimeler, beklenen cevap: "Bu konuda rehberimizde bilgi bulamadım." Not, bu listenin TASK-016'da "cevabı olmayan soru" örnekleri için kaynak olduğunu söyler.
- **L-5 `uret.sh`:** `bash`, `set -euo pipefail`, repo kökünden ya da `docs/rehber/`'den çalışır. Her sürüm için tek `typst compile` (Kararlar #2, #3, #4, #18). Bilinmeyen sürüm parametresi derlemeyi durdurur. Çıktı: iki PDF yolu ve sayfa sayıları.
- **L-6 `dogrula.sh` + `RehberDogrulama.java`:** PDFBox jar'ını indirir ve SHA-256'sını doğrular (Kararlar #15); D1-D10 ve D12'yi çalıştırır; her kontrol için `GEÇTİ`/`KALDI` ve kısa ayrıntı basar; bir kontrol kalırsa çıkış kodu 1. `kontroller.txt` beklenen dizeleri tutar: kontrol cümleleri (D3), tablo satırları (D4), kurgu kontrolü için gerçek banka adları (D12). D8 beklentilerini `degisiklik-ozeti-2026.11.md`'nin "Ayrıntı" tablosundan, D10 yasak kelimelerini `rehberde-olmayan-konular.md`'den okur (beklenen dizeler tek yerde). Metin çıkarma `PDFTextStripper` varsayılanlarıyla yapılır; tablo satırı için `setSortByPosition(true)` gerekiyorsa bu ayar raporda ve README'de yazılır (TASK-005 aynı ayarı kullanır).
- **L-7 Rapor:** `handoff/02-lead-implementation.md`: araç sürümleri; `uret.sh` ve `dogrula.sh` çıktıları (tam); sayfa sayıları ve bölüm başlangıç sayfaları tablosu; "TASK alt konusu → başlık" eşleme tablosu (K2); kurgusal kimlik kontrolü (Kararlar #14); negatif deneme (D-N) sonucu; bayt eşitliği mi metin eşitliği mi sağlandı (Kararlar #18); kullanıcı içerik onayı satırı (tarih ve varsa istenen değişiklikler).
- **L-8 Sınırlar:** `kaynaklar/**`, servis kodu, `docs/api/**`, kök `CLAUDE.md`, `rag.md` değişmez. Rehberin varlığını `rag.md`'ye ya da `retrieval-api/CLAUDE.md`'ye işlemek bu task'ta yok ("Açık sorular" 3).

## İçindekiler (onay kartındakiyle aynı; sayfa tahmini 72 sayfalık hedefe göre)
| Bölüm | Alt başlıklar | Tahmini sayfa | TASK kapsamı |
|-------|---------------|---------------|--------------|
| Kapak | Ad, sürüm, yürürlük, iki not (Kararlar #21) | 1 | K1, K4a |
| İçindekiler | — | 2 | K4a |
| 1 Bu rehber hakkında | 1.1 Kapsam ve yürürlük · 1.2 Rehberi nasıl kullanırsınız · 1.3 Ücretler, vergiler ve sayı biçimi | 2 | — |
| 2 Kartlar | 2.1 Raşit Banka Kartı · 2.2 Kredi kartı türleri · 2.3 Ek kart · 2.4 Yıllık üyelik ücreti, muafiyet ve iade · 2.5 Kredi kartı başvurusu · 2.6 Kart limiti: belirleme, artırma, düşürme, geçici limit · 2.7 Hesap kesim tarihi, ekstre ve son ödeme tarihi · 2.8 Asgari ödeme ve gecikme · 2.9 Taksitli alışveriş · 2.10 Nakit avans · 2.11 RaşitPuan · 2.12 Sanal kart ve internetten alışveriş · 2.13 Temassız ödeme · 2.14 Yurt dışında kullanım ve kur · 2.15 Kart şifresi ve güvenli ödeme (3D Secure) · 2.16 Kayıp ya da çalıntı bildirimi · 2.17 Harcama itirazı · 2.18 Kartın yenilenmesi ve teslim süresi · 2.19 Kartın kapatılması · Sık sorulanlar | 14 | Kartlar: hepsi |
| 3 Hesaplar | 3.1 Vadesiz hesap · 3.2 Vadeli mevduat · 3.3 Kumbara hesabı · 3.4 Döviz hesapları · 3.5 Altın hesabı · 3.6 Ortak hesap ve çocuklar için hesap · 3.7 Müşteri olma ve hesap açma belgeleri · 3.8 Hesap işletim ücreti · 3.9 Hareketsiz hesaplar ve zamanaşımı · 3.10 Hesap kapatma · 3.11 Mevduat sigortası (TMSF) · 3.12 Stopaj · 3.13 Vefat hâlinde hesaplar · Sık sorulanlar | 9 | Hesaplar: hepsi |
| 4 Krediler | 4.1 Başvuru şartları · 4.2 Başvuru belgeleri ve değerlendirme süresi · 4.3 İhtiyaç kredisi · 4.4 Taşıt kredisi · 4.5 Konut kredisi · 4.6 Kredili mevduat hesabı (Ek Hesap) · 4.7 Faiz, KKDF, BSMV ve masraflar · 4.8 Kredi sigortaları · 4.9 Cayma hakkı · 4.10 Erken ödeme ve erken kapama tazminatı · 4.11 Gecikme faizi ve takip süreci · 4.12 Yapılandırma · 4.13 Kredi kapandıktan sonra: borcu yoktur yazısı, rehin ve ipotek kaldırma · Sık sorulanlar | 10 | Krediler: hepsi |
| 5 Para transferi ve ödemeler | 5.1 Havale · 5.2 EFT · 5.3 FAST ve Kolay Adres · 5.4 SWIFT ile yurt dışına transfer · 5.5 Yurt dışından gelen para · 5.6 İşlem saatleri, hafta sonu ve resmî tatiller · 5.7 Transfer limitleri ve ücretleri · 5.8 Karekod ile ödeme · 5.9 Düzenli ödeme talimatları · 5.10 Fatura, vergi ve kurum ödemeleri · 5.11 Yanlış gönderim ve iade · Sık sorulanlar | 8 | Transfer: hepsi |
| 6 Dijital bankacılık | 6.1 Raşit Mobil ve İnternet Şubesi · 6.2 İlk giriş ve şifre alma · 6.3 Şifre sıfırlama · 6.4 Cihaz tanımlama · 6.5 Yüz tanıma ve parmak izi ile giriş · 6.6 Dijital işlem limitleri · 6.7 Bildirimler ve SMS · 6.8 Güvenlik önerileri · 6.9 Dolandırıcılık uyarıları ve dolandırıldığınızda yapılacaklar · 6.10 Telefonunuz kaybolursa ya da çalınırsa · 6.11 Şifre cihazı (sadece 2026.10) · Sık sorulanlar | 7 | Dijital: hepsi |
| 7 Yatırım | 7.1 Yatırım hesabı ve uygunluk testi · 7.2 Yatırım fonları · 7.3 Hisse senedi · 7.4 Döviz alım satımı ve hafta sonu kuru · 7.5 Altın alım satımı ve fiziki altın · 7.6 Tahvil, bono ve eurobond · 7.7 Emir türleri ve otomatik alım · 7.8 Vergilendirme · 7.9 Yatırım ürünleri ve mevduat sigortası · Sık sorulanlar | 6 | Yatırım: hepsi |
| 8 Şube, ATM ve müşteri hizmetleri | 8.1 Şubeler ve çalışma saatleri · 8.2 Şubede yapılan işlemler ve kiralık kasa · 8.3 ATM hizmetleri ve kartsız işlem · 8.4 ATM nakit çekme ve yatırma limitleri · 8.5 Başka banka ATM'leri · 8.6 ATM'de yaşanan sorunlar · 8.7 Müşteri hizmetleri ve iletişim kanalları · 8.8 Engelli müşterilerimiz için hizmetler · 8.9 İletişim bilgilerinin güncellenmesi · Sık sorulanlar | 5 | Diğer: şube, ATM, limitler, müşteri hizmetleri |
| 9 Şikâyet, itiraz ve kişisel veriler | 9.1 Şikâyet kanalları ve cevap süresi · 9.2 Sonuçtan memnun kalmazsanız · 9.3 Kişisel verilerin korunması: aydınlatma metni · 9.4 Kişisel verilerinizle ilgili haklarınız ve başvuru · Sık sorulanlar | 3 | Diğer: şikâyet, KVKK |
| 10 Ücret tarifesi | 10.1 Kart ücretleri · 10.2 Hesap ücretleri · 10.3 Kredi ücretleri · 10.4 Transfer ücretleri · 10.5 Dijital bankacılık ücretleri · 10.6 Yatırım işlem ücretleri · 10.7 Şube ve ATM ücretleri | 4 | Diğer: ücret tarifesi tablosu |
| 11 Terimler sözlüğü | Alfabetik, en az 40 terim | 3 | Diğer: sözlük |
| **Toplam** | | **~74 (hedef 72; bölüm sonu boşluklarıyla 68-76)** | |

"6.11 Şifre cihazı" başlığı PDF'te "(sadece 2026.10)" eki olmadan yazılır; ek sadece bu planda ve kartta açıklama içindir.

## 2026.11 farkları (öneri; kart sorusu 4)
`surum.typ` anahtarları ve değerleri. Bütün farkların yürürlüğü 1 Kasım 2026'dır.

| # | Tür | `surum.typ` anahtarı | 2026.10 | 2026.11 | Geçtiği yerler (sayfalar PDF'ten) |
|---|-----|----------------------|---------|---------|-------------------------------|
| F1 | Ücret | `gold-yillik-ucret` | `1.450 TL` | `1.650 TL` | 2.2 kart türleri tablosu, 2.4 (muafiyet metni varsa), 2. bölüm SSS (varsa), 10.1 |
| F2 | Limit | `banka-karti-atm-gunluk` | `25.000 TL` | `30.000 TL` | 2.1, 8.4 limit tablosu, 10.7 (varsa) |
| F3 | Eklenen ürün | `genc-kart` | `false` | `true` | 2.2'ye paragraf + tablo satırı ("Raşit Genç": 18-26 yaş üniversite öğrencileri, yıllık üyelik ücreti yok, limit 5.000-20.000 TL), 10.1 tablo satırı |
| F4 | Kaldırılan madde | `sifre-cihazi` | `true` | `false` | 6.11 alt başlığı (2026.10: şifre cihazı 150 TL'ye verilir, kaybında yenisi 150 TL), 10.5 tablo satırı. Şifre cihazı başka hiçbir yerde anılmaz |

- F2'deki değer 2.1 ile 8.4'te aynı dizedir. Kredi kartının ATM limiti ayrı bir olgudur ve değişmez.
- Etkilenen bölümler: kapak ve 1.1 (sürüm etiketi ve yürürlük tarihi), 2, 6, 8, 10 ve İçindekiler (6.11 satırı). D9 bunların dışındaki bölümlerin (3, 4, 5, 7, 9, 11) sayfalarını birebir karşılaştırır.
- Sürüm etiketi ve yürürlük tarihi her sürümde zaten farklıdır; değişiklik özetine fark olarak girmez.

## Test senaryoları
Otomatik uygulama testi yok. D1-D12 `docs/rehber/dogrula.sh` kontrolleridir (D11 ve D-N lead'in elle çalıştırdığı komutlardır); lead çalıştırır ve çıktıyı rapora yazar, qa bağımsız yeniden çalıştırır. Q-1…Q-6 qa'nın elle kontrolleridir. "her iki PDF" = 2026.10 ve 2026.11.

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| D1 | Sayfa sayısı (K4d) | doğrulama | `givenBothPdfs_whenPageCountRead_thenEqualAndWithinApprovedRange` | İki PDF'in sayfa sayısı eşit ve onaylanan aralıkta (öneri A: 68-76) |
| D2 | Metin seçilebilir (K5) | doğrulama | `givenEachPage_whenTextExtracted_thenPageHasSelectableText` | Kapak ≥ 80 karakter, diğer her sayfa ≥ 300 karakter metin. Hiçbir sayfa tam sayfa görüntü değil |
| D3 | Türkçe karakter (K5, G5) | doğrulama | `givenExtractedText_whenTurkishCharactersChecked_thenAllPresentAndUncorrupted` | Metinde `ç ğ ı İ ö ş ü Ç Ğ Ö Ş Ü â` geçer. `U+FFFD`, `U+FB00`-`U+FB06` (bitişik harf), `U+0307` (birleşik nokta) ve satır sonunda tireyle bölünmüş kelime (`[a-zçğıöşü]-\n[a-zçğıöşü]`) sıfır. `kontroller.txt`'deki en az 10 kontrol cümlesi birebir bulunur; örnek: `Kayıp ya da çalıntı bildirimi`, `Şikâyet, itiraz ve kişisel veriler`, `Kâğıt ekstre`, `İhtiyaç kredisi`, `Yurt dışında kullanım ve kur` |
| D4 | Tablo satırları (K4b, K5, G3, S1, S2) | doğrulama | `givenFeeAndLimitTables_whenTextExtracted_thenEachRowOnOneLineInColumnOrder` | `kontroller.txt`'deki her satır kontrolü için hücre metinleri aynı çıkarılan satırda ve soldan sağa sırayla bulunur. En az: 2.2 `Raşit Gold` + F1 değeri (sürüme göre); 4.10 `Konut kredisi (sabit faizli)` + `36 aydan uzun` + `%2`; 4.10 `İhtiyaç ve taşıt kredisi` + `12 ay ve altı` + `%0,5`; 2.16 kanal tablosunun bir satırı; 8.4 banka kartı ATM satırı + F2 değeri; 5.7 FAST satırı; 10.1'den bir satır |
| D5 | Üst/alt bilgi (K4a, G1, G2) | doğrulama | `givenEveryPageAfterCover_whenHeaderFooterRead_thenMatchesVersionAndPageIndex` | Sayfa 2…T'nin ilk satırı `Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm <etiket>`, son satırı `Yürürlük: <tarih> · Sayfa <i> / <T>`; `i` PDFBox sayfa sırası. Kapakta bu satırlar yok |
| D6 | İçindekiler ve yer imleri (K4a, K2) | doğrulama | `givenOutline_whenComparedWithTocAndHeadings_thenPageNumbersMatch` | PDF yer imlerinde her numaralı başlık (1…11 ve alt başlıklar) var. İçindekiler'deki her satırın sayfa numarası, o başlığın metinde göründüğü sayfa. Ana bölümler sayfanın ilk başlığıdır (Kararlar #8) |
| D7 | Çapraz atıf (K4c) | doğrulama | `givenCrossReferences_whenResolved_thenEachPointsToExistingHeading` | Metindeki her `bkz. X.Y` için `X.Y` numaralı başlık var. Her iki PDF'te en az 10 atıf |
| D8 | Değişiklik özeti (K7, AC3, S4) | doğrulama | `givenChangeSummary_whenPagesSearched_thenEachChangeOnListedPageAndOldValuesGone` | "Ayrıntı" tablosunun her satırı için: `2026.11 metni` 2026.11'in listelenen sayfasında, `2026.10 metni` 2026.10'un listelenen sayfasında bulunur; ayrıca `2026.10 metni` 2026.11'in hiçbir sayfasında, `2026.11 metni` 2026.10'un hiçbir sayfasında yoktur (bu yüzden hücre dizeleri bağlamıyla seçilir: `Raşit Gold 1.450 TL` gibi, tek başına `25.000 TL` değil). Ek olarak `1.450 TL` ve `şifre cihazı` (Türkçe küçük harfe çevrilerek) 2026.11'de sıfır; `1.650 TL` ve `Raşit Genç` 2026.10'da sıfır. `1.450 TL` ve `1.650 TL` tutarları rehberde Gold ücreti dışında kullanılmaz |
| D9 | Sayfa düzeni (Kararlar #8) | doğrulama | `givenTwoVersions_whenUnchangedChaptersCompared_thenPageTextIdentical` | Bölüm başlangıç sayfaları iki sürümde aynı. Bölüm 3, 4, 5, 7, 9, 11'in her sayfasının metni, üst ve alt bilgi satırları çıkarılınca iki sürümde birebir aynı |
| D10 | Rehberde olmayan konular (K3, S3, G4) | doğrulama | `givenExcludedTopicKeywords_whenSearchedInBothPdfs_thenNoMatch` | Nottaki her yasak kelime iki PDF'te sıfır (Türkçe küçük harfe çevrilerek). En az: `kripto`, `bitcoin`, `işletme kredisi` (soru 5 = A ise), `genel müdür` (soru 5 = A ise) |
| D11 | Tekrar üretim (K6, AC1) | doğrulama | `givenCleanCheckout_whenUretRunTwice_thenOutputsIdentical` | `./docs/rehber/uret.sh` iki kez: PDF'ler `cmp` ile aynı (Kararlar #18). Sağlanamazsa sayfa sayfa çıkarılan metin aynı ve raporda gerekçe. `git status` sadece beklenen dosyaları gösterir |
| D12 | Kurgu sınırı (K1) | doğrulama | `givenCoverAndBody_whenFictionRulesChecked_thenDisclaimerPresentAndNoRealBankNames` | Kapakta `Bu rehber bilgilendirme amaçlıdır.` ve kurgu notu birebir. `kontroller.txt`'deki gerçek banka adlarının (ör. `Ziraat`, `İş Bankası`, `Garanti BBVA`, `Akbank`, `Yapı Kredi`, `Halkbank`, `VakıfBank`, `QNB`, `DenizBank`, `TEB`, `ING`, `Enpara`) hiçbiri metinde yok. Web/e-posta adresleri sadece `rasitbank.example` alanında |
| D-N | Negatif deneme (doğrulamanın kendisi) | doğrulama | `givenMutatedSource_whenDogrulaRun_thenFailsWithNonZeroExit` | Lead geçici bir kopyada (scratch) `surum.typ`'de 2026.11'in `gold-yillik-ucret`'ini `1.450 TL` yapar, PDF üretir ve `dogrula.sh`'yi o çıktıya karşı çalıştırır: D8 `KALDI`, çıkış kodu 1. Repo dosyası değişmez. Sonuç raporda |
| Q-1 | Konum, üretim, rapor (AC1, AC2, K8) | qa elle | — | Dosyalar "Dosya yapısı"ndaki yollarda. `uret.sh` ve `dogrula.sh` qa'nın makinesinde baştan çalışır, tüm kontroller `GEÇTİ`, çıktı lead raporundakiyle aynı. `git status kaynaklar/` boş. Servis ve `docs/api/` diff'i yok |
| Q-2 | Kapsam ve biçim (K2, K4b) | qa elle | — | Lead raporundaki "TASK alt konusu → başlık" tablosunda Kural 2'deki her alt konu bir başlığa eşlenmiş ve qa en az 10 tanesini PDF'te açıp doğrular. Bölüm 2-9'un sonunda en az 4 soruluk SSS var |
| Q-3 | Not (AC4, K3) | qa elle | — | `rehberde-olmayan-konular.md` onaylanan her konuyu, örnek soruları, yasak kelimeleri ve sabit cevabı taşır |
| Q-4 | Görsel kontrol (K1, G3) | qa elle | — | Rastgele 8 sayfa PDF görüntüleyicide açılır: tablolarda satır sayfada bölünmemiş, başlık satırı tekrar ediyor, kapakta iki not var, gerçek banka adı/logosu yok |
| Q-5 | Örnek senaryolar (S1, S2) | qa elle | — | 2.16'da kanal, kalıcı/geçici kapatma ve teslim süresi (iş günü) yazıyor; 4.10'da tazminat oranları ve hangi kredilerde alınmadığı yazıyor; kartta onaylanan olgularla aynı |
| Q-6 | Kullanıcı içerik onayı (AC5, K9) | qa elle | — | Lead raporunda kullanıcının tam metni onayladığı satır (tarih) var; onaydan sonra kaynakta değişiklik olduysa D1-D12 yeniden çalıştırılmış |

## Deployment planı
Yok. Docker, model ve ortam değişkeni değişmez.

## Açık sorular ve riskler
Kullanıcı tercihi gereği riskler bu task'a katılmaz; takip task'ı adayı olarak listelenir.
1. **Yazım hacmi ve tutarlılık (risk):** ~72 sayfa Türkçe metni lead tek turda yazar. Aynı olgunun bölüm metni, SSS ve tarifede farklı yazılması (ör. bir ücretin iki yerde iki değer) en olası hata. D4/D8 sadece listelenen dizeleri yakalar. Takip task'ı adayı: "Rehber olgu tutarlılığı taraması" (tarife tablosundaki her değerin bölüm metniyle karşılaştırılması).
2. **TASK-005 için not (bu task'ta iş yok):** Üst/alt bilgi kalıpları (Kararlar #7) parçalamada ayıklanmalı; PDF'in retrieval-api container'ına nasıl gireceği (bağlama ya da kopyalama) TASK-005'in kararı; metin çıkarma ayarı (`setSortByPosition`) L-6'da kayda geçer, TASK-005 aynısını kullanmalı.
3. **Kapsam dışı not:** Rehber gelince `rag.md` "Lab'da görünenler" (31 sayfa, 61 parça) ve `rag-pipeline/references/indexing.md` §2'deki "31 sayfa" lab'ı anlatmaya devam eder; gerçek rehberin sayfa sayısı oralara işlenmez. Kök `CLAUDE.md` "Durum" satırı TASK-003'ten beri eski ("lokal modeller henüz yok"). Sahipsiz dosyalar; kapanışta lead kullanıcıya sorar.
4. **Typst tekrarlanabilirlik (risk):** Kurulu Typst sürümü sabit zaman damgasını desteklemezse bayt eşitliği sağlanmaz; Kararlar #18'deki alt sınır (metin eşitliği) uygulanır ve raporda yazılır.
5. **Kurgusal kimlik çakışması (risk):** Seçilen çağrı merkezi numarası ya da SWIFT kodu gerçek bir kuruma ait çıkabilir; lead arayarak kontrol eder (Kararlar #14). Kesin güvence yok.
6. **Sayfa düzeni kısıtı (risk):** Değişen bölümlerin aynı sayfa sayısını koruması (Kararlar #8) bölüm sonunda boşluk bırakmayı gerektirir; tutmazsa D9 kalır. Lead içerik kısaltmadan bölüm sonu payıyla çözer; çözülemezse `NEEDS_FIX` (sorumlu planner) ve kural kullanıcıya sorulur.

STATUS: DONE
