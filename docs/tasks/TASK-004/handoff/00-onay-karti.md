TASK: TASK-004
# Onay kartı: Raşit Bankası Ürün ve Hizmet Rehberi (PDF, 2026.10 ve 2026.11)

## Ne yapılacak
- Asistanın cevaplarını dayandıracağı rehber, gerçek bir bankanın müşteri rehberi ayrıntısında yazılacak. Rehberde 11 bölüm olacak: kartlar, hesaplar, krediler, para transferi, dijital bankacılık, yatırım, şube ve ATM, şikâyet ve kişisel veriler, ücret tarifesi, terimler sözlüğü. Taslak aşağıda.
- Rehberin iki sürümü aynı kaynak metinden üretilecek:
  - 2026.10 (yürürlük 1 Ekim 2026), ilk içe alınacak olan,
  - 2026.11 (yürürlük 1 Kasım 2026), "Yeni sürüm yükle" ile işlenecek olan.
- İki sürümün farkları ayrı bir "değişiklik özeti"nde, hangi sayfada olduklarıyla birlikte yazılacak. Öneri: dört fark (aşağıda).
- Asistanın "Bu konuda rehberimizde bilgi bulamadım." demesi gereken konular ayrı bir notta toplanacak. Bu konular rehberde hiç geçmeyecek.
- PDF'ler tek komutla yeniden üretilebilecek. İkinci bir komut, PDF'lerden metni bilgi bankasının okuyacağı biçimde çıkarıp kontrol edecek.
- İçeriği ana oturum (lead) yazacak. Sonra qa, PDF'leri kendi başına yeniden üretip kontrol edecek.

## Bittiğini nasıl anlayacaksın
- Önerilen konumda (`docs/rehber/`) şunlar duruyor: iki PDF, rehberin kaynağı, değişiklik özeti ve "rehberde olmayan konular" notu.
- Üretme komutunu çalıştırınca iki PDF yeniden oluşuyor. Komutu ikinci kez çalıştırınca birebir aynı dosyalar çıkıyor.
- Kontrol komutu PDF'lerden metni çıkarıyor ve her kontrol için "GEÇTİ" yazıyor:
  - Türkçe harfler (ç, ğ, ı, İ, ö, ş, ü, â) bozulmadan çıkıyor; satır sonunda bölünmüş kelime yok.
  - Ücret ve limit tablolarının her satırı metinde tek satır olarak, sütun sırasıyla çıkıyor (ör. "Raşit Gold 1.450 TL").
  - Her sayfanın üstünde rehber adı ve sürüm, altında yürürlük tarihi ve sayfa numarası var. Sayfa numarası, PDF'teki gerçek sayfa sırasıyla aynı.
  - İçindekiler'deki sayfa numaraları doğru. Her "bkz. 4.10" atfı var olan bir başlığı gösteriyor.
  - Değişiklik özetindeki her fark 2026.11 PDF'inde yazılan sayfada var. Eski değer (ör. 1.450 TL) 2026.11'de hiç geçmiyor.
  - Değişmeyen bölümlerin sayfaları iki sürümde kelimesi kelimesine aynı.
  - "Rehberde olmayan konular"ın kelimeleri (ör. kripto, Bitcoin) iki PDF'te de hiç geçmiyor.
  - Kapakta "Bu rehber bilgilendirme amaçlıdır." notu var; gerçek bir bankanın adı geçmiyor.
- Kontrol komutunun gerçekten hata yakaladığı bir kez denenmiş: 2026.11'deki Gold ücreti bilerek eski değerde bırakılınca kontrol "KALDI" diyor.
- İki PDF'i açıp kendin okursun:
  - "Kartımı kaybettim" için 2.16'da adım adım yol var.
  - "Ev kredisini erken kapatırsam ceza öder miyim?" için 4.10'da oranlar var.
  - "Bitcoin" hiçbir yerde geçmiyor.
  - Gold ücreti 2026.10'da 1.450 TL, 2026.11'de 1.650 TL.
  Tam metni onaylarsın. Değişiklik istersen lead düzeltir ve kontrolleri yeniden çalıştırır.

## Yapılmayacaklar
- Golden set soruları (TASK-016). Rehber, soru üretmeye uygun net olgularla (sayı, süre, koşul) yazılır.
- Rehberin bilgi bankasına içe alınması (TASK-005) ve yeni sürüm yükleme (TASK-015). "Gold sorusu önce 1.450 TL, sonra 1.650 TL cevabını alıyor" senaryosu TASK-015'te uçtan uca denenir; bu task sadece rehberdeki değerleri kontrol eder.
- `kaynaklar/` klasörü ve lab'daki eski PDF'ler değişmez, kullanılmaz.
- Servislerde, ekranda ve Docker'da değişiklik.
- Ürün dokümanındaki (`rag.md`) "31 sayfa, 61 parça" gibi lab bilgilerinin yeni rehbere göre güncellenmesi (bkz. Riskler).

## Benim verdiğim kararlar
- **İki sürüm tek kaynaktan çıkar.** Sürüme göre değişen her değer kaynakta tek bir yerde durur. Böylece farklar gözden kaçmaz ve değişmeyen metin iki sürümde aynı kalır.
- **Her bölüm yeni sayfada başlar.** Değişen bölümler iki sürümde aynı sayfa sayısını korur. Sonuç:
  - iki PDF'in sayfa sayısı ve bölümlerin başladığı sayfalar aynı,
  - farkın olmadığı bölümlerin sayfaları kelimesi kelimesine aynı.
  Böylece yeni sürüm yüklenince değişmeyen sayfalardan gelen bilgiler de aynı kalır. Bu, ileride golden set'teki "cevap burada" işaretinin kaymamasına yardım eder.
- **Farklar başlık numaralarını kaydırmaz.** Kaldırılan madde 6. bölümün son alt başlığıdır. Eklenen kart, mevcut 2.2'nin içine girer. "bkz. 6.9" iki sürümde de aynı yeri gösterir.
- **2026.11 rehberi eski değerleri anmaz.** Rehberde "eskiden 1.450 TL idi" ya da "bu sürümde değişenler" sayfası olmaz. Olsaydı asistan eski değeri bulup söyleyebilirdi. Farklar sadece ayrı değişiklik özetinde durur.
- **Sayfa numarası kapaktan başlar.** Kapak 1. sayfadır (üstünde numara yazmaz). Böylece asistanın "s. 7" diye gösterdiği sayfa, PDF'te gerçekten 7. sayfadır.
- **Üst ve alt bilgi sabittir:**
  - üstte "Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm 2026.10",
  - altta "Yürürlük: 1 Ekim 2026 · Sayfa 7 / 72".
  Bölüm adı üst bilgiye girmez. Böylece her sayfada metne karışan tekrar kısa kalır ve içe almada kolayca ayıklanır.
- **Kelimeler satır sonunda tireyle bölünmez, harfler bitişik basılmaz.** Bölünmüş kelime ("kre-di") ve bitişik "fi" harfi, metin çıkarıldığında aramayı bozar.
- **Tablo hücreleri tek satırdır.** Uzun koşullar tablonun altına madde olarak yazılır. Bir tablo satırı sayfa sonunda ikiye bölünmez, başlık satırı yeni sayfada tekrar eder.
- **"Sık sorulanlar" 2-9. bölümlerin sonunda yer alır**, her birinde en az 4 soru. Giriş bölümünde, ücret tarifesinde ve sözlükte yer almaz.
- **Kişisel bilgi sorgulama rehberde anlatılmaz.** "Bakiyenizi şuradan görürsünüz" gibi bir adım yazılmaz; yoksa "bakiyemi söyler misin?" sorusu rehberden cevaplanırdı. "Bakiye" kelimesi sadece genel kavram olarak geçer (ör. eksi bakiye).
- **Kapakta iki not olur:** "Bu rehber bilgilendirme amaçlıdır." ve "Raşit Bankası kurgusal bir bankadır; bu rehber bir eğitim demosu için hazırlanmıştır."
- **İletişim bilgileri kurgusaldır.** Web ve e-posta adresleri gerçek olamayacak bir alan adındadır (`rasitbank.example`). Lead, telefon numarasının ve SWIFT kodunun gerçek bir kuruma ait olmadığını arayarak kontrol eder. Resmî kurumlar adıyla anılabilir: Merkez Bankası, BDDK, TMSF, KVKK Kurumu, Tüketici Hakem Heyeti.
- **Sayılar tek biçimde yazılır:** "1.450 TL", "%3,25", "1 Kasım 2026", "3 iş günü", "09.00-17.00".
- **PDF'ler de repoya girer**, adları lab'dakiyle aynıdır (`Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf`). Bilgi bankası ve yükleme ekranı onları yeniden üretmeden kullanır.
- **Rapor:** Lead yaptıklarını ve kontrol çıktılarını bir rapora yazar. Tam metni sana sunar ve onayını rapora kaydeder.

## İçindekiler taslağı (72 sayfalık hedefe göre tahmini sayfa)
- **Kapak** (1 sayfa): rehber adı, sürüm, yürürlük tarihi, iki not.
- **İçindekiler** (2 sayfa).
- **1 Bu rehber hakkında** (2 sayfa)
  - 1.1 Kapsam ve yürürlük
  - 1.2 Rehberi nasıl kullanırsınız
  - 1.3 Ücretler, vergiler ve sayı biçimi
- **2 Kartlar** (14 sayfa)
  - 2.1 Raşit Banka Kartı
  - 2.2 Kredi kartı türleri (Raşit Classic, Gold, Platinum; 2026.11'de Raşit Genç)
  - 2.3 Ek kart
  - 2.4 Yıllık üyelik ücreti, muafiyet ve iade
  - 2.5 Kredi kartı başvurusu
  - 2.6 Kart limiti: belirleme, artırma, düşürme, geçici limit
  - 2.7 Hesap kesim tarihi, ekstre ve son ödeme tarihi
  - 2.8 Asgari ödeme ve gecikme
  - 2.9 Taksitli alışveriş
  - 2.10 Nakit avans
  - 2.11 RaşitPuan
  - 2.12 Sanal kart ve internetten alışveriş
  - 2.13 Temassız ödeme
  - 2.14 Yurt dışında kullanım ve kur
  - 2.15 Kart şifresi ve güvenli ödeme (3D Secure)
  - 2.16 Kayıp ya da çalıntı bildirimi
  - 2.17 Harcama itirazı
  - 2.18 Kartın yenilenmesi ve teslim süresi
  - 2.19 Kartın kapatılması
  - Sık sorulanlar
- **3 Hesaplar** (9 sayfa)
  - 3.1 Vadesiz hesap
  - 3.2 Vadeli mevduat
  - 3.3 Kumbara hesabı
  - 3.4 Döviz hesapları
  - 3.5 Altın hesabı
  - 3.6 Ortak hesap ve çocuklar için hesap
  - 3.7 Müşteri olma ve hesap açma belgeleri
  - 3.8 Hesap işletim ücreti
  - 3.9 Hareketsiz hesaplar ve zamanaşımı
  - 3.10 Hesap kapatma
  - 3.11 Mevduat sigortası (TMSF)
  - 3.12 Stopaj
  - 3.13 Vefat hâlinde hesaplar
  - Sık sorulanlar
- **4 Krediler** (10 sayfa)
  - 4.1 Başvuru şartları
  - 4.2 Başvuru belgeleri ve değerlendirme süresi
  - 4.3 İhtiyaç kredisi
  - 4.4 Taşıt kredisi
  - 4.5 Konut kredisi
  - 4.6 Kredili mevduat hesabı (Ek Hesap)
  - 4.7 Faiz, KKDF, BSMV ve masraflar
  - 4.8 Kredi sigortaları
  - 4.9 Cayma hakkı
  - 4.10 Erken ödeme ve erken kapama tazminatı
  - 4.11 Gecikme faizi ve takip süreci
  - 4.12 Yapılandırma
  - 4.13 Kredi kapandıktan sonra: borcu yoktur yazısı, rehin ve ipotek kaldırma
  - Sık sorulanlar
- **5 Para transferi ve ödemeler** (8 sayfa)
  - 5.1 Havale
  - 5.2 EFT
  - 5.3 FAST ve Kolay Adres
  - 5.4 SWIFT ile yurt dışına transfer
  - 5.5 Yurt dışından gelen para
  - 5.6 İşlem saatleri, hafta sonu ve resmî tatiller
  - 5.7 Transfer limitleri ve ücretleri
  - 5.8 Karekod ile ödeme
  - 5.9 Düzenli ödeme talimatları
  - 5.10 Fatura, vergi ve kurum ödemeleri
  - 5.11 Yanlış gönderim ve iade
  - Sık sorulanlar
- **6 Dijital bankacılık** (7 sayfa)
  - 6.1 Raşit Mobil ve İnternet Şubesi
  - 6.2 İlk giriş ve şifre alma
  - 6.3 Şifre sıfırlama
  - 6.4 Cihaz tanımlama
  - 6.5 Yüz tanıma ve parmak izi ile giriş
  - 6.6 Dijital işlem limitleri
  - 6.7 Bildirimler ve SMS
  - 6.8 Güvenlik önerileri
  - 6.9 Dolandırıcılık uyarıları ve dolandırıldığınızda yapılacaklar
  - 6.10 Telefonunuz kaybolursa ya da çalınırsa
  - 6.11 Şifre cihazı (sadece 2026.10'da)
  - Sık sorulanlar
- **7 Yatırım** (6 sayfa)
  - 7.1 Yatırım hesabı ve uygunluk testi
  - 7.2 Yatırım fonları
  - 7.3 Hisse senedi
  - 7.4 Döviz alım satımı ve hafta sonu kuru
  - 7.5 Altın alım satımı ve fiziki altın
  - 7.6 Tahvil, bono ve eurobond
  - 7.7 Emir türleri ve otomatik alım
  - 7.8 Vergilendirme
  - 7.9 Yatırım ürünleri ve mevduat sigortası
  - Sık sorulanlar
- **8 Şube, ATM ve müşteri hizmetleri** (5 sayfa)
  - 8.1 Şubeler ve çalışma saatleri
  - 8.2 Şubede yapılan işlemler ve kiralık kasa
  - 8.3 ATM hizmetleri ve kartsız işlem
  - 8.4 ATM nakit çekme ve yatırma limitleri
  - 8.5 Başka banka ATM'leri
  - 8.6 ATM'de yaşanan sorunlar
  - 8.7 Müşteri hizmetleri ve iletişim kanalları
  - 8.8 Engelli müşterilerimiz için hizmetler
  - 8.9 İletişim bilgilerinin güncellenmesi
  - Sık sorulanlar
- **9 Şikâyet, itiraz ve kişisel veriler** (3 sayfa)
  - 9.1 Şikâyet kanalları ve cevap süresi
  - 9.2 Sonuçtan memnun kalmazsanız
  - 9.3 Kişisel verilerin korunması: aydınlatma metni
  - 9.4 Kişisel verilerinizle ilgili haklarınız ve başvuru
  - Sık sorulanlar
- **10 Ücret tarifesi** (4 sayfa)
  - 10.1 Kart ücretleri
  - 10.2 Hesap ücretleri
  - 10.3 Kredi ücretleri
  - 10.4 Transfer ücretleri
  - 10.5 Dijital bankacılık ücretleri
  - 10.6 Yatırım işlem ücretleri
  - 10.7 Şube ve ATM ücretleri
- **11 Terimler sözlüğü** (3 sayfa): alfabetik, en az 40 terim.

Toplam yaklaşık 74 sayfa. Bölüm sonlarındaki boşluklarla 68-76 arasında kalır.

## Örnek bölümler
Bu iki bölüm rehbere bu olgularla girer. Kart onaylanınca lead metnin dilini düzeltebilir, olguları değiştirmez.

> **2.16 Kayıp ya da çalıntı bildirimi**
>
> Kartınızı kaybettiyseniz ya da kartınız çalındıysa vakit kaybetmeden bildirin. Bildirimden sonra kartınızla yapılan işlemlerden sorumlu olmazsınız. Bildirimden önceki 24 saat içinde onayınız olmadan yapılan işlemlerde sorumluluğunuz 250 TL ile sınırlıdır.
>
> 1. Kartınızı aşağıdaki kanallardan biriyle kapatın:
>
> | Kanal | Ne zaman | Nasıl |
> |-------|----------|-------|
> | Raşit Mobil | 7/24 | Kartlarım > kartı seçin > Kayıp/çalıntı bildir |
> | İnternet Şubesi | 7/24 | Kartlar > Kart işlemleri > Kayıp/çalıntı bildir |
> | Müşteri Hizmetleri | 7/24 | Kartlar menüsü > Kayıp/çalıntı (bkz. 8.7) |
> | Şube | Şube çalışma saatlerinde | Kimliğinizle başvurun |
>
> 2. Kayıp ya da çalıntı bildirimiyle kartınız kalıcı olarak kapatılır; bu işlem geri alınamaz. Kartınızı yalnızca nereye koyduğunuzu bilmiyorsanız Raşit Mobil'deki "Geçici kapat" seçeneğini kullanın. Kart siz yeniden açana kadar kapalı kalır. Bulamazsanız kayıp bildirimi yapın.
> 3. Yeni kartınız kendiliğinden basılır ve kayıtlı adresinize kuryeyle gönderilir. İl merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde teslim edilir. Teslimde kimliğinizi göstermeniz gerekir.
> 4. Yeni kartın numarası, son kullanma tarihi ve güvenlik kodu değişir. Bankamızdaki düzenli ödeme talimatları yeni karta kendiliğinden aktarılır. İnternet sitelerinde kayıtlı kart ve abonelik bilgilerini siz güncellersiniz.
> 5. Bir takvim yılındaki ilk kayıp ya da çalıntı bildiriminde yeni kart ücretsizdir. Aynı yıldaki sonraki bildirimlerde kart yenileme ücreti 100 TL'dir (bkz. 10.1).
> 6. Bildirimden önce tanımadığınız bir harcama varsa harcama itirazı başlatın (bkz. 2.17). Kartınız çalındıysa emniyete de bildirmenizi öneririz.

> **4.10 Erken ödeme ve erken kapama tazminatı**
>
> Kredinizin bir kısmını ya da tamamını vadesinden önce ödeyebilirsiniz. Erken ödediğiniz tutar için kalan vadenin faizini, KKDF'sini ve BSMV'sini ödemezsiniz. Sabit faizli kredilerde bankamız, erken ödenen anapara üzerinden aşağıdaki oranlarda erken ödeme tazminatı alır:
>
> | Kredi türü | Kalan vade | Tazminat oranı |
> |------------|------------|----------------|
> | İhtiyaç ve taşıt kredisi | 12 ay ve altı | %0,5 |
> | İhtiyaç ve taşıt kredisi | 12 aydan uzun | %1 |
> | Konut kredisi (sabit faizli) | 36 ay ve altı | %1 |
> | Konut kredisi (sabit faizli) | 36 aydan uzun | %2 |
> | Değişken faizli krediler | Tümü | Alınmaz |
> | Ek Hesap (kredili mevduat hesabı) | — | Alınmaz |
>
> Örnek: Kalan vadesi 60 ay olan sabit faizli konut kredinizin 400.000 TL'sini erken öderseniz tazminat 400.000 TL × %2 = 8.000 TL olur.
>
> - Erken kapama tutarını Raşit Mobil'de Kredilerim > kredi > Erken kapama tutarı ekranında görürsünüz. Tutar o günün sonuna kadar geçerlidir.
> - Kısmi ödemede taksit tutarının mı yoksa vadenin mi azalacağını siz seçersiniz.
> - Kredi kapandıktan sonra borcu yoktur yazısı 3 iş günü içinde hazırlanır. Konut kredisinde ipoteğin kaldırılması ayrı bir işlemdir (bkz. 4.13).

## 2026.11 farkları (öneri, soru 4)
Hepsinin yürürlüğü 1 Kasım 2026. Sayfa numaraları PDF üretilince kesinleşir ve değişiklik özetine yazılır.

| # | Tür | 2026.10 | 2026.11 | Rehberde geçtiği yerler |
|---|-----|---------|---------|-------------------------|
| 1 | Ücret artışı | Raşit Gold yıllık üyelik ücreti 1.450 TL | 1.650 TL | 2.2 kart türleri tablosu, geçtiği diğer yerler (ör. sık sorulanlar), 10.1 ücret tarifesi |
| 2 | Değişen limit | Banka kartıyla ATM'den günlük nakit çekme limiti 25.000 TL | 30.000 TL | 2.1, 8.4 limit tablosu |
| 3 | Eklenen ürün | Yok | Raşit Genç kredi kartı: 18-26 yaş üniversite öğrencileri için, yıllık üyelik ücreti yok, limit 5.000-20.000 TL | 2.2 (paragraf ve tablo satırı), 10.1 |
| 4 | Kaldırılan madde | Şifre cihazı: 150 TL'ye verilir, kaybında yenisi 150 TL | Madde ve ücret satırı kaldırılır | 6.11, 10.5 |

Bu farklar asistanda şöyle görünür:
- "Raşit Gold yıllık üyelik ücreti ne kadar?" 2026.10'da 1.450 TL, 2026.11'de 1.650 TL cevabını alır.
- "Üniversite öğrencisiyim, ücretsiz kredi kartınız var mı?" 2026.10'da "bulamadım", 2026.11'de "evet, Raşit Genç" cevabını alır.
- "Şifre cihazı alabilir miyim?" 2026.10'da 150 TL cevabını alır, 2026.11'de "bulamadım".

## Rehberde olmayan konular (öneri, soru 5)
Bu konular rehbere hiç yazılmaz; asistan bunlarda "Bu konuda rehberimizde bilgi bulamadım." der. Liste ayrı bir notta, her konu için örnek sorularla tutulur.

TASK'taki dört konu:
1. **Kişisel bakiye.** Örnek: "Hesabımdaki bakiyeyi söyler misin?"
2. **Hesap hareketleri.** Örnek: "Dün hesabıma gelen para ne kadardı?"
3. **Kripto para.** Örnek: "Bitcoin alıp satabilir miyim?"
4. **Başka bankaların ürünleri.** Örnek: "X bankasının konut kredisi faizi kaç?" Raşit Bankası'ndan başka bankaya EFT ya da başka bankanın ATM'sini kullanma gibi konular rehberde vardır; bu madde bunları kapsamaz.

Lab'daki "bulamadım" sorularından eklemeyi önerdiğim beş konu:
5. **Kişiye özel diğer bilgiler.** Örnekler: "Kredi kartı limitim ne kadar?", "Başvurum ne durumda?"
6. **Ticari ve kurumsal bankacılık.** Örnek: "İşletme kredisi faiz oranınız ne kadar?"
7. **Bankanın kurumsal bilgileri.** Örnek: "Raşit Bankası'nın genel müdürü kim?"
8. **Piyasa tahmini ve yatırım tavsiyesi.** Örnek: "Dolar yarın yükselir mi?"
9. **Bireysel emeklilik (BES) ve sigorta ürünlerinin ayrıntıları.** Örnek: "BES'te devlet katkısı yüzde kaç?" Kredilere bağlı zorunlu sigortalar (DASK, kredi hayat sigortası) 4.8'de anlatılır; bu madde onları kapsamaz.

## Çalışacak roller
1. planner.
2. lead (ana oturum): rehberi, iki PDF'i, değişiklik özetini, notu ve iki komutu hazırlar. Kontrolleri çalıştırır, raporu yazar. Tam metni sana sunar ve onayını alır.
3. qa: PDF'leri kendi başına yeniden üretir, kontrolleri çalıştırır, sayfaları PDF'te açıp örneklemle okur ve kabul kriterlerini işaretler.

Atlananlar:
- backend-coder ve frontend-coder: servislerde ve ekranda değişiklik yok.
- tester: uygulama kodu ve otomatik testi yok. Kontrol komutunu lead yazar, qa bağımsız çalıştırır.
- devops: Docker ve modeller değişmiyor.

## Kararını bekleyen sorular
1. Rehberin dosyaları `docs/rehber/` klasöründe dursun mu?
   Neden önemli: Bilgi bankası (TASK-005) ve yükleme ekranı (TASK-015) PDF'leri bu klasörden alacak; sonradan taşımak birkaç task'ı etkiler.
   Seçenekler:
   A) `docs/rehber/`: kaynak, iki PDF, değişiklik özeti, not ve komutlar bir arada.
   B) Kök dizinde `rehber/`.
   C) Başka bir yer (yolunu yaz).
   Önerim: A. Doküman klasöründe, `kaynaklar/`'dan ayrı ve hiçbir servisin kod klasörüne karışmıyor.

2. PDF'i Typst ile üretip PDFBox ile kontrol edelim mi?
   Neden önemli: Bu iki araç projeye yeni girer. PDF'lerin her makinede aynı çıkması ve bilgi bankasının metni doğru okuması bu seçime bağlı.
   - **Typst:** metinden düzgün sayfalı PDF üreten, tek parça hâlinde kurulan bir program. İçindekiler, sayfa numarası, üst/alt bilgi, tablo ve "bkz." atıflarını kendisi yapar. İnternet gerektirmez.
   - **Yazı tipi:** paneldeki Source Sans 3 yazı tipinin dosyaları lisansıyla birlikte repoya girer. Böylece PDF her bilgisayarda aynı görünür.
   - **PDFBox:** PDF'ten metin çıkaran Java kütüphanesi (3.0.6). Bilgi bankası rehberi büyük olasılıkla bununla okuyacak; kontrol de aynı gözle bakmış olur. Kontrol komutu onu kendisi indirir, repoya eklenmez.
   Seçenekler:
   A) Typst + Source Sans 3 + PDFBox.
   B) WeasyPrint (HTML'den PDF üreten program) + PDFBox. Kurulumu Python ve ek kütüphaneler ister; yazı tipleri bilgisayara göre değişebilir.
   C) Pandoc + LaTeX + PDFBox. Kurulumu birkaç GB. Türkçe heceleme ve bitişik harf ayarları daha zahmetli.
   Önerim: A. Tek program, aynı girdiden hep aynı PDF. Türkçe harf ve tablo kontrolü için gereken ayarlar basit.

3. Rehber kaç sayfa olsun?
   Neden önemli: Sayfa sayısı rehberin ayrıntısını, yazım süresini ve bilgi bankasındaki parça sayısını belirler. 72 sayfa yaklaşık 120-150 parça eder; lab'daki rehber 31 sayfa, 61 parçaydı.
   Seçenekler:
   A) 72 sayfa hedef, 68-76 arası kabul. Yukarıdaki içindekiler buna göre.
   B) 60 sayfa (56-64). Alt başlıklar daha kısa olur.
   C) 90 sayfa (86-94). Her alt başlıkta daha fazla örnek ve koşul olur, yazım süresi uzar.
   Önerim: A. Bütün konular ayrıntılı ve net olgularla yazılabilir; yazım ve kontrol yönetilebilir kalır.

4. 2026.11 için yukarıdaki dört fark uygun mu?
   Neden önemli: Bu farklar yeni sürüm yüklemesinde (TASK-015) ve değerlendirmede (TASK-016) "cevap değişti mi?" sorularının kaynağı olacak.
   Seçenekler:
   A) Dört fark yukarıdaki gibi.
   B) Dört fark + bir faiz oranı değişikliği (ör. 32 günlük vadeli mevduat faizi). Fark sayısı beşe çıkar, bir bölüm daha değişir.
   C) Kendi listen (değerleriyle yaz).
   Önerim: A. TASK'ın istediği dört türü (ücret, limit, eklenen, kaldırılan) birer kez karşılıyor. Her biri asistanda farklı bir cevap değişimi gösteriyor.

5. "Rehberde olmayan konular" listesi, TASK'taki dört konuya ek olarak lab'dan önerdiğim beş konuyu (5-9) da içersin mi?
   Neden önemli: Listedeki konular rehbere hiç yazılmaz ve kontrol komutu bunların kelimelerinin geçmediğine bakar. Bu liste ileride "cevabı olmayan soru" örneklerinin de kaynağıdır.
   Seçenekler:
   A) Dokuz konunun hepsi.
   B) Sadece TASK'taki dört konu. 5-9 rehberde kısaca anlatılabilir ya da hiç anılmaz.
   C) Dört konu + 5-9'dan seçtiklerin.
   Önerim: A. Hepsi lab'da "bulamadım" cevabı alan gerçekçi müşteri soruları. Kapsam dışı olduğu baştan yazılırsa rehbere dolaylı olarak da girmezler.

## Riskler
Kural gereği bu task'a katılmazlar; gerekirse ayrı task olurlar.
- **Aynı bilginin iki yerde farklı yazılması.** Örneğin bir ücretin bölüm metninde ve ücret tarifesinde farklı yazılması. Kontrol komutu sadece listelenen değerleri yakalar. Takip task'ı adayı: "Rehber olgu tutarlılığı taraması" (tarifedeki her değerin bölüm metniyle karşılaştırılması).
- **Değişen bölümlerin sayfa sayısını koruması tutmayabilir.** Tutmazsa lead içeriği kısaltmadan bölüm sonundaki boşlukla çözer. Çözemezse bu kuralı sana yeniden sorarız.
- **Ürün dokümanındaki eski lab bilgileri.** `rag.md` ve bazı ekip rehberleri lab'daki 31 sayfalık rehberi anlatmaya devam eder. Takip task'ı adayı: rehber sonrası doküman güncellemesi. Bu dosyalar senin onayınla değişir.

ONAY: VERİLDİ
