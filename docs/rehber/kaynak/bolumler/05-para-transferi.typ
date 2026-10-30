#import "../ortak.typ": *

= Para transferi ve ödemeler <bolum-transfer>

== Havale <havale>
Havale, Raşit Bankası içindeki hesaplar arasında yapılan para transferidir.
- 7 gün 24 saat, anında gerçekleşir.
- Raşit Mobil, İnternet Şubesi, ATM ve şubeden yapılan havaleler ücretsizdir.
- Alıcının adı soyadı ve IBAN'ı ya da Raşit Bankası hesap numarası yeterlidir.
- Tutar sınırı yoktur; dijital kanallarda günlük işlem limitiniz geçerlidir (#bkz(<dijital-limit>)).
- Kendi hesaplarınız arasındaki aktarımlar da havale sayılır ve limitten etkilenmez.

#ara[Alıcı kontrolü]
Havale onay ekranında, girdiğiniz IBAN'ın sahibinin adı ve soyadı kısmen gizlenerek gösterilir (örneğin "Ay\*\*\* Yıl\*\*\*"). Gösterilen ad, göndermek istediğiniz kişiyle uyuşmuyorsa işlemi onaylamayın. Havale anında gerçekleştiği için onaylandıktan sonra iptal edilemez (#bkz(<yanlis-gonderim>)).

#ara[Kayıtlı alıcılar]
Sık para gönderdiğiniz kişileri Raşit Mobil'de Transferler > Kayıtlı alıcılar ekranına ekleyebilirsiniz. Kayıtlı alıcılara yapılan transferlerde alıcı bilgilerini yeniden yazmanız gerekmez. Güvenli alıcı listesi ayarı açıksa yalnızca kayıtlı alıcılara transfer yapılabilir (#bkz(<guvenlik>)).

== EFT <eft>
EFT, başka bir bankadaki hesaba Türkiye Cumhuriyet Merkez Bankası'nın Elektronik Fon Transferi sistemiyle yapılan transferdir. FAST'ın tek işlem sınırını (100.000 TL) aşan transferler EFT ile gönderilir.
- EFT, iş günlerinde 09.00-17.00 saatleri arasında gerçekleşir. Bu saatlerde gönderilen EFT genellikle birkaç dakika içinde alıcının hesabına geçer.
- Saat 17.00'den sonra, hafta sonu ya da resmî tatilde verdiğiniz EFT talimatı ileri tarihli olarak kaydedilir ve izleyen ilk iş günü saat 09.00'dan itibaren gönderilir. Örneğin cuma akşamı saat 20.00'de verilen 250.000 TL'lik EFT, pazartesi saat 09.00'dan sonra gönderilir.
- Arife günlerinde EFT saat 12.30'a kadar yapılabilir.
- Ücretler için #bkz(<transfer-limit-ucret>).

#ara[EFT'nin iptali ve sorgulanması]
- İleri tarihli olarak kaydedilmiş bir EFT talimatını, gönderilene kadar Raşit Mobil'deki "İleri tarihli işlemler" ekranından ücretsiz iptal edebilirsiniz.
- Gönderilmiş bir EFT iptal edilemez. Alıcıya ulaşmadığını düşündüğünüz EFT için Raşit Mobil'de işlem detayındaki "EFT sorgulama numarası"nı alıcıyla paylaşabilirsiniz; alıcı bu numarayla kendi bankasına başvurabilir.
- Alıcı banka EFT'yi aynı gün kabul etmezse tutar en geç izleyen iş günü hesabınıza iade edilir.

== FAST ve Kolay Adres <fast>
FAST, başka bir bankadaki hesaba 7 gün 24 saat, saniyeler içinde yapılan transferdir. Hafta sonu ve resmî tatillerde de çalışır.
- Tek seferde en fazla 100.000 TL gönderilebilir. Bu tutarın üzerindeki transferler EFT ile gönderilir (#bkz(<eft>)).
- Alıcı adı ile IBAN sahibinin adı uyuşmazsa transfer alıcı banka tarafından reddedilebilir; reddedilen tutar ve ücreti aynı gün hesabınıza iade edilir.

#ara[Kolay Adres]
Kolay Adres, IBAN yerine telefon numarası, e-posta adresi ya da T.C. kimlik numarasıyla FAST transferi yapmanızı sağlar.
- IBAN bilmeden para göndermek için alıcının bankasına tanımladığı Kolay Adres'i yazmanız yeterlidir. Transferi onaylamadan önce alıcının adı soyadının baş harfleri gösterilir.
- Kendi telefon numaranızı, e-posta adresinizi ya da T.C. kimlik numaranızı Raşit Mobil'de Transferler > Kolay Adres ekranından bir hesabınıza tanımlayabilirsiniz.
- Bir Kolay Adres aynı anda yalnızca bir bankadaki bir hesaba tanımlanabilir. Başka bir bankada tanımlı Kolay Adres'i Raşit Bankası'na taşıyabilirsiniz; taşıma için SMS ile onay istenir.

== SWIFT ile yurt dışına transfer <swift>
Yurt dışındaki bir hesaba para göndermek için SWIFT transferi kullanılır. Transfer Raşit Mobil'den, İnternet Şubesi'nden ya da şubeden yapılabilir.
- Gönderim ABD doları, euro, İngiliz sterlini ve İsviçre frangı cinsinden yapılır. Türk lirası hesabınızdan gönderirseniz tutar işlem anındaki döviz satış kuruyla çevrilir.
- Dijital kanallardan günlük SWIFT limiti için #bkz(<dijital-limit>).

#ara[Gereken bilgiler]
- Alıcının adı soyadı ya da unvanı ve açık adresi,
- Alıcının IBAN'ı ya da hesap numarası,
- Alıcı bankanın SWIFT (BIC) kodu,
- Gönderim amacı (eğitim, sağlık, aile desteği, mal bedeli gibi).

#ara[Masraf seçenekleri: SHA ve OUR]
#tablo(("Seçenek", "Gönderim ücretini kim öder", "Aradaki bankaların masraflarını kim öder"),
  ("SHA (paylaşımlı)", "Gönderen", "Alıcı; masraflar gönderilen tutardan düşülebilir"),
  ("OUR (gönderen öder)", "Gönderen", "Gönderen; alıcı tutarın tamamını alır"),
)
OUR seçeneği için gönderim ücretine ek ücret alınır. SWIFT ücretleri #bkz(<transfer-limit-ucret>) tablosundadır.

Örnek: Almanya'daki bir hesaba Raşit Mobil'den 1.000 euro gönderiyorsunuz. SHA seçeneğinde 25 ABD doları karşılığı gönderim ücreti ödersiniz; aradaki bankaların masrafları gönderilen tutardan düşülebileceği için alıcıya 1.000 euro'dan biraz azı ulaşabilir. OUR seçeneğinde toplam 45 ABD doları karşılığı ücret ödersiniz ve alıcı 1.000 euro'nun tamamını alır. Euro bölgesindeki ülkelere euro gönderirken alıcının IBAN'ı yeterlidir; alıcı bankanın SWIFT kodu IBAN'dan otomatik bulunur.

#ara[Süreler]
- İş günlerinde saat 16.00'ya kadar verilen SWIFT talimatları aynı gün gönderilir; 16.00'dan sonra verilenler izleyen iş günü gönderilir.
- Paranın alıcı hesaba geçmesi, aradaki bankalara ve alıcı ülkeye bağlı olarak genellikle 1 ile 3 iş günü sürer.
- Hafta sonu ve Türkiye'deki ya da alıcı ülkedeki resmî tatillerde SWIFT gönderimi yapılmaz.

#ara[SWIFT işleminin takibi]
Gönderdiğiniz SWIFT işleminin hangi aşamada olduğunu Raşit Mobil'de işlem detayındaki "Transfer takibi" ekranından görebilirsiniz. Ekranda işlemin aradaki bankalardan geçişi ve kesilen masraflar yer alır. İşlemin uluslararası takip numarası (UETR) da bu ekrandadır; alıcı bu numarayla kendi bankasından sorgulama yapabilir.

#ara[İade, iptal ve değişiklik]
- Gönderilmemiş bir SWIFT talimatını, gönderim saatinden önce Raşit Mobil'den ücretsiz iptal edebilirsiniz.
- Gönderilmiş bir SWIFT işleminin iadesi ya da alıcı bilgilerinin düzeltilmesi için talep oluşturabilirsiniz. Talep aradaki bankalara iletilir; sonuç alıcı bankanın onayına bağlıdır ve talep ücreti, sonuç olumsuz olsa da iade edilmez.
- İade edilen tutar, aradaki bankaların masrafları düşülerek gönderdiğiniz para biriminde hesabınıza yatırılır.

#ara[Belge talepleri ve kısıtlamalar]
Mevzuat gereği yüksek tutarlı ya da sık yapılan yurt dışı transferlerde gönderim amacını gösteren belge (fatura, okul kayıt belgesi, kira sözleşmesi gibi) istenebilir. Belge istenen transfer, belge yüklenene kadar bekletilir. Uluslararası yaptırım listelerinde bulunan kişi, kurum ya da ülkelerle ilgili transferler yapılamaz. Raşit Bankası, hesap numarası olmadan isimle para gönderilen hızlı para transferi hizmetlerine aracılık etmez.

== Yurt dışından gelen para <gelen-swift>
Size yurt dışından para gönderecek kişiye şu bilgileri vermeniz yeterlidir:
- IBAN'ınız,
- Adınız ve soyadınız,
- Raşit Bankası'nın SWIFT kodu: RSBKTRIS.

Gelen tutar, gönderilen para birimindeki vadesiz döviz hesabınıza geçer; o para biriminde hesabınız yoksa hesap kendiliğinden açılır. Paranın Türk lirası hesabınıza geçmesini isterseniz Raşit Mobil'den döviz satışı yapabilirsiniz (#bkz(<doviz-alim>)).

- Gelen SWIFT için Raşit Bankası ücret almaz. Aradaki muhabir bankalar gönderilen tutardan kesinti yapabilir.
- Gönderen banka parayı gönderdikten sonra tutar genellikle 1 ile 3 iş günü içinde hesabınıza geçer. Raşit Bankası'na iş günü saat 16.00'ya kadar ulaşan tutarlar aynı gün hesabınıza aktarılır.
- Yüksek tutarlı gelen transferlerde gönderim amacını açıklamanız istenebilir.
- Size gönderilen tutar, gönderenin seçtiği masraf seçeneğine (SHA ya da OUR) göre aradaki bankaların kesintileri düşülerek ulaşabilir (#bkz(<swift>)).
- Gelen bir tutarın hesabınıza geçmediğini düşünüyorsanız gönderenden işlemin takip numarasını (UETR) isteyip Müşteri Hizmetleri'ne iletebilirsiniz.

== İşlem saatleri, hafta sonu ve resmî tatiller <transfer-saatleri>
#tablo(("İşlem", "Hafta içi", "Hafta sonu ve resmî tatil"),
  ("Havale", "7/24", "7/24"),
  ("FAST (100.000 TL'ye kadar)", "7/24", "7/24"),
  ("EFT", "09.00-17.00", "Yapılmaz; ilk iş günü gönderilir"),
  ("SWIFT gönderimi", "09.00-16.00", "Yapılmaz; ilk iş günü gönderilir"),
  ("Fatura ve vergi ödemeleri", "7/24", "7/24"),
  ("Kredi kartı borcu ödemesi", "7/24", "7/24"),
)
- Arife günlerinde EFT saat 12.30'a, SWIFT gönderimi saat 12.00'ye kadar yapılabilir. FAST ve havale arife günlerinden etkilenmez.
- Hafta sonu ya da resmî tatilde verilen EFT ve SWIFT talimatlarını gönderilmeden önce Raşit Mobil'deki "İleri tarihli işlemler" ekranından iptal edebilirsiniz.
- Döviz ve altın alım satım saatleri için #bkz(<doviz-alim>) ve #bkz(<altin-alim>).

#ara[Resmî tatiller]
EFT ve SWIFT'in çalışmadığı resmî tatiller şunlardır:
- Yılbaşı (1 Ocak), Ulusal Egemenlik ve Çocuk Bayramı (23 Nisan), Emek ve Dayanışma Günü (1 Mayıs), Atatürk'ü Anma, Gençlik ve Spor Bayramı (19 Mayıs), Demokrasi ve Millî Birlik Günü (15 Temmuz), Zafer Bayramı (30 Ağustos) ve Cumhuriyet Bayramı (29 Ekim).
- Ramazan Bayramı ve Kurban Bayramı. Bu bayramların tarihleri her yıl değişir.
- 28 Ekim ile Ramazan ve Kurban bayramlarının arife günleri yarım gündür; bu günlerde EFT ve SWIFT öğleden önce çalışır.

Resmî tatile denk gelen bir düzenli EFT talimatı izleyen ilk iş günü gönderilir. Tatil boyunca biriken ileri tarihli EFT talimatları, ilk iş günü veriliş sırasıyla gönderilir.

== Transfer limitleri ve ücretleri <transfer-limit-ucret>
#ara[Yurt içi transfer ücretleri]
#tablo(("İşlem", "Raşit Mobil ve İnternet Şubesi", "Raşit Bankası ATM'si", "Şube"),
  ("Havale", "Ücretsiz", "Ücretsiz", "Ücretsiz"),
  ("FAST, 6.000 TL'ye kadar", "Ücretsiz", "9,90 TL", "74,50 TL"),
  ("FAST, 6.000,01-100.000 TL", "6,49 TL", "9,90 TL", "74,50 TL"),
  ("EFT", "11,95 TL", "Yapılmaz", "74,50 TL"),
  ("İleri tarihli ya da düzenli transfer", "Kanalın ücreti kadar", "Yapılmaz", "Kanalın ücreti kadar"),
)
- Ücretlere BSMV dahildir ve ücret, transferle aynı anda gönderen hesaptan tahsil edilir.
- Raşit Bankası'ndan maaş ya da emekli maaşı alan müşterilerin Raşit Mobil ve İnternet Şubesi'nden yaptığı FAST ve EFT işlemleri tutar sınırı olmadan ücretsizdir.
- Alıcı banka tarafından reddedildiği ya da alıcı bilgisi hatalı olduğu için geri dönen transferlerde ücret iade edilir. Alıcının onayıyla yapılan geri gönderimlerde ücret iade edilmez (#bkz(<yanlis-gonderim>)).

Örnek: Raşit Mobil'den başka bir bankaya 4.000 TL'yi FAST ile gönderirseniz ücret alınmaz; 50.000 TL gönderirseniz ücret 6,49 TL, 150.000 TL'yi EFT ile gönderirseniz ücret 11,95 TL'dir. Aynı işlemleri şubeden yaptırırsanız her biri için 74,50 TL ödersiniz.

#ara[Yurt dışı transfer ücretleri]
#tablo(("İşlem", "Raşit Mobil ve İnternet Şubesi", "Şube"),
  ("SWIFT gönderim ücreti", "25 ABD doları karşılığı", "45 ABD doları karşılığı"),
  ("OUR seçeneği için ek ücret", "20 ABD doları karşılığı", "20 ABD doları karşılığı"),
  ("Gelen SWIFT", "Ücretsiz", "Ücretsiz"),
  ("SWIFT iade, iptal ya da değişiklik talebi", "35 ABD doları karşılığı", "35 ABD doları karşılığı"),
)

#ara[Hesabı olmayan birine para göndermek: Cepten Cebe]
Raşit Bankası'nda hesabı olmayan birine "Cepten Cebe" hizmetiyle para gönderebilirsiniz. Alıcı, telefonuna SMS ile gelen kod ve kendi T.C. kimlik numarasıyla parayı Raşit Bankası ATM'lerinden çeker.
- Tek seferde en fazla 10.000 TL gönderilebilir; işlem ücreti 14,90 TL'dir.
- Para 7 gün içinde çekilmezse gönderenin hesabına iade edilir; ücret iade edilmez.

#ara[Limitler]
Raşit Mobil ve İnternet Şubesi'nden yapılan transferlerin günlük limitleri ve limit değiştirme #bkz(<dijital-limit>) içinde anlatılır. Şubede transfer için limit uygulanmaz.

== Karekod ile ödeme <karekod>
Raşit Mobil'deki "Karekod" menüsüyle Türkiye Cumhuriyet Merkez Bankası'nın TR Karekod standardındaki karekodları okutarak işlem yapabilirsiniz.
- *İşyerinde ödeme:* Kasada gösterilen karekodu okutup tutarı onayladığınızda ödeme vadesiz hesabınızdan ya da seçtiğiniz kredi kartınızdan yapılır. Kredi kartıyla karekod ödemeleri alışveriş sayılır ve RaşitPuan kazandırır.
- *Para transferi:* Kendi hesabınız için karekod oluşturup paylaşabilirsiniz. Karekodu okutan kişi IBAN yazmadan size FAST ile para gönderir.
- *ATM'de kartsız işlem:* Raşit Bankası ATM'lerinde ekrandaki karekodu okutarak kartsız para çekebilir ve yatırabilirsiniz (#bkz(<atm>)).
- Karekodla ödeme ve transfer için ayrıca ücret alınmaz; FAST ile yapılan transferlerde FAST ücretleri geçerlidir.
- İşyerinde ödeme için gösterilen karekod 3 dakika, sizin oluşturduğunuz transfer karekodu 24 saat geçerlidir.
- Karekodu okuttuktan sonra onay ekranında işyerinin ya da alıcının adı ve tutar gösterilir. Bu bilgiler beklediğinizle uyuşmuyorsa işlemi onaylamayın; kamusal alanlara yapıştırılmış karekodların sahte olabileceğini unutmayın.
- Karekodla yapılan işyeri ödemelerinin iadesi, işyerinin iade işlemiyle aynı hesaba ya da karta yapılır.

== Düzenli ödeme talimatları <duzenli-odeme>
#ara[Düzenli ve ileri tarihli transfer]
Havale, EFT ve FAST için ileri bir tarihte bir kez gerçekleşecek ya da her ay aynı gün tekrarlanacak transfer talimatı verebilirsiniz. Kira ve site aidatı gibi düzenli ödemeler için düzenli transfer talimatı kullanabilirsiniz.
- Talimatı Raşit Mobil'de Transferler > Düzenli transferler ekranından verir, değiştirir ya da iptal edersiniz.
- Talimat gününde hesabınızda yeterli para yoksa transfer gün içinde 3 kez denenir. Yine gerçekleşmezse SMS ile bilgilendirilirsiniz ve o ayın transferi yapılmaz.
- Açıklama alanına ödemenin konusunu yazmanızı öneririz (örneğin "Ekim 2026 kirası"). Açıklama en fazla 140 karakterdir.

#ara[Otomatik fatura ödeme talimatı]
Otomatik ödeme talimatı verdiğiniz faturalar, son ödeme tarihinde vadesiz hesabınızdan ya da kredi kartınızdan kendiliğinden ödenir. Talimatı Raşit Mobil'de Ödemeler > Otomatik ödemeler ekranından kurum ve abone numarasını seçerek verirsiniz.
- Talimat, verildiği tarihten sonra kesilen ilk faturadan itibaren geçerli olur. O sırada ödenmemiş bir faturanız varsa onu ayrıca ödemeniz gerekir.
- Son ödeme tarihinde hesabınızda yeterli para yoksa ödeme 3 gün boyunca her gün yeniden denenir. Bu sürede ödenemezse fatura ödenmemiş kalır ve SMS ile bilgilendirilirsiniz.
- Kredi kartından verilen talimatlarda fatura tutarı kart limitinizden düşer ve ekstrenize yansır.
- Her talimat için tek seferde ödenecek en yüksek tutarı belirleyebilirsiniz. Fatura bu tutarı aşarsa ödenmez ve size bildirim gönderilir.
- Talimatı istediğiniz an iptal edebilirsiniz. İptal, kurumun bir sonraki fatura döneminden itibaren geçerli olur.

Kredi kartı borcunuz için otomatik ödeme talimatı #bkz(<asgari-odeme>) içinde anlatılır.

== Fatura, vergi ve kurum ödemeleri <fatura>
#ara[Faturalar]
Elektrik, su, doğalgaz, telefon, internet ve televizyon faturalarınızı Raşit Mobil'de Ödemeler > Fatura öde ekranından, İnternet Şubesi'nden, Raşit Bankası ATM'lerinden ve şubelerden ödeyebilirsiniz.
- Fatura sorgulamak için abone ya da tesisat numaranız yeterlidir.
- Fatura ödemeleri dijital kanallarda ve ATM'lerde ücretsizdir; şubede fatura başına 9,90 TL alınır.
- Son ödeme tarihi geçmiş faturalar, kurum izin veriyorsa gecikme bedeliyle birlikte ödenebilir.

#ara[Vergi, harç ve SGK ödemeleri]
Gelir vergisi, motorlu taşıtlar vergisi, emlak vergisi, trafik cezaları, pasaport ve sürücü belgesi harçları ile SGK prim borçlarını Raşit Mobil'de Ödemeler > Vergi ve kamu ödemeleri ekranından ödeyebilirsiniz.
- Vergi ödemeleri vadesiz hesaptan ücretsizdir.
- Motorlu taşıtlar vergisini ve emlak vergisini kredi kartıyla 3 taksitle ödeyebilirsiniz. Vergi ödemeleri RaşitPuan kazandırmaz.

#ara[Ödemenin kuruma ulaşması]
- Dijital kanallardan ve ATM'den yapılan fatura ödemeleri kuruma anında bildirilir. Kurumun kendi sisteminde görünmesi kuruma bağlı olarak 1 iş gününü bulabilir.
- Ödediğiniz faturanın dekontunu Raşit Mobil'den PDF olarak indirebilirsiniz. Kurum ödemeyi görmediğini söylerse bu dekontu kuruma iletin.
- Yanlış abone numarasına yapılan fatura ödemesi Raşit Bankası tarafından geri alınamaz; iade için faturayı kesen kuruma başvurmanız gerekir.

#ara[Eğitim, aidat ve mobil hat ödemeleri]
- Anlaşmalı okul, üniversite ve site yönetimlerine ödemeleri Ödemeler > Kurum ödemeleri ekranından yapabilirsiniz. Anlaşmalı olmayan kurumlar için düzenli transfer talimatı kullanılır (#bkz(<duzenli-odeme>)).
- Faturalı ve faturasız mobil hatlarınıza TL yükleyebilirsiniz. Bu işlemler kredi kartından yapıldığında nakit avans sayılmaz, alışveriş olarak işlem görür.

== Yanlış gönderim ve iade <yanlis-gonderim>
Parayı yanlış IBAN'a ya da yanlış kişiye gönderdiyseniz vakit kaybetmeden Müşteri Hizmetleri'ni arayın ya da Raşit Mobil'de işlemi seçip "İade talebi oluştur" adımını kullanın.
+ Raşit Bankası, gerçekleşmiş bir transferi alıcının onayı olmadan geri alamaz.
+ Raşit Bankası, talebinizi aynı gün alıcının bankasına iletir; alıcının bankası müşterisinden iade onayı ister.
+ Alıcı iadeyi kabul ederse tutar hesabınıza geri gönderilir. Bu süreç genellikle 3 ile 10 iş günü sürer.
+ Alıcı iadeyi kabul etmezse ya da ulaşılamazsa tutarı ancak yasal yollarla geri isteyebilirsiniz. Talebinizin sonucu size yazılı olarak bildirilir.

#ara[Geri dönen transferler]
Alıcı adı ile IBAN sahibi uyuşmadığı, IBAN geçersiz olduğu ya da alıcı hesap kapalı olduğu için alıcı banka tarafından reddedilen FAST ve EFT işlemleri, tutar ve ücretiyle birlikte aynı gün hesabınıza iade edilir. Geri dönen SWIFT işlemlerinde aradaki bankaların kestiği masraflar iade edilmez.

Dolandırıcılık sonucu yapılmış transferler için #bkz(<dolandiricilik>).

#sss()
#soru[FAST ile en fazla ne kadar gönderebilirim?][Tek seferde en fazla 100.000 TL. Daha yüksek tutarlar iş günlerinde 09.00-17.00 arasında EFT ile gönderilir (#bkz(<fast>)).]
#soru[Akşam saat 20.00'de 250.000 TL EFT yaparsam ne zaman gider?][Talimatınız ileri tarihli olarak kaydedilir ve izleyen ilk iş günü saat 09.00'dan itibaren gönderilir (#bkz(<eft>)).]
#soru[Mobilden EFT ücreti ne kadar?][Raşit Mobil ve İnternet Şubesi'nden EFT ücreti 11,95 TL'dir. Raşit Bankası'ndan maaş alıyorsanız ücretsizdir (#bkz(<transfer-limit-ucret>)).]
#soru[Bankanızın SWIFT kodu nedir?][Raşit Bankası'nın SWIFT kodu RSBKTRIS'tir (#bkz(<gelen-swift>)).]
#soru[Yanlış kişiye para gönderdim. Geri alabilir miyim?][Banka, alıcının onayı olmadan transferi geri alamaz. Hemen iade talebi oluşturun; talebiniz alıcının bankasına iletilir ve alıcı kabul ederse para size döner (#bkz(<yanlis-gonderim>)).]
#soru[Otomatik ödemede hesabımda para yoksa ne olur?][Ödeme 3 gün boyunca her gün yeniden denenir. Bu sürede ödenemezse fatura ödenmemiş kalır ve SMS ile bilgilendirilirsiniz (#bkz(<duzenli-odeme>)).]
#soru[Arife günü EFT yapılabiliyor mu?][Evet, saat 12.30'a kadar. Bu saatten sonra verilen EFT talimatları izleyen ilk iş günü gönderilir; FAST ve havale arife gününden etkilenmez (#bkz(<transfer-saatleri>)).]
#soru[IBAN bilmeden para gönderebilir miyim?][Evet. Alıcının bankasına tanımladığı Kolay Adres'i (telefon numarası, e-posta adresi ya da T.C. kimlik numarası) kullanarak FAST ile gönderebilirsiniz (#bkz(<fast>)).]
#soru[Hesabı olmayan birine nasıl para gönderirim?][Cepten Cebe hizmetiyle tek seferde en fazla 10.000 TL gönderebilirsiniz. Alıcı parayı SMS kodu ve T.C. kimlik numarasıyla 7 gün içinde Raşit Bankası ATM'lerinden çeker. Ücret 14,90 TL'dir (#bkz(<transfer-limit-ucret>)).]
#soru[Şubeden FAST yaptırmak kaç lira?][Şubeden yapılan FAST ve EFT işlemlerinin ücreti 74,50 TL'dir (#bkz(<transfer-limit-ucret>)).]
#soru[Transfer geri döndü. Ücreti iade ediliyor mu?][Alıcı banka reddettiği ya da alıcı bilgisi hatalı olduğu için geri dönen FAST ve EFT işlemlerinde ücret iade edilir (#bkz(<yanlis-gonderim>)).]
#soru[OUR ve SHA arasındaki fark nedir?][SHA'da aradaki bankaların masraflarını alıcı öder ve masraflar gönderilen tutardan düşülebilir. OUR'da bütün masrafları gönderen öder ve alıcı tutarın tamamını alır (#bkz(<swift>)).]
#soru[Yurt dışından gelen para kaç günde hesabıma geçer?][Gönderen banka parayı gönderdikten sonra genellikle 1 ile 3 iş günü içinde hesabınıza geçer (#bkz(<gelen-swift>)).]
#soru[Her ay kira ödemesi için talimat verebilir miyim?][Evet. Raşit Mobil'de Transferler > Düzenli transferler ekranından her ay aynı gün gerçekleşecek transfer talimatı verebilirsiniz (#bkz(<duzenli-odeme>)).]
#soru[Motorlu taşıtlar vergisini taksitle ödeyebilir miyim?][Evet. Motorlu taşıtlar vergisini kredi kartıyla 3 taksitle ödeyebilirsiniz (#bkz(<fatura>)).]
