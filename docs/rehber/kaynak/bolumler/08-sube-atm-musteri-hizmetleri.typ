#import "../ortak.typ": *

= Şube, ATM ve müşteri hizmetleri <bolum-sube>

== Şubeler ve çalışma saatleri <subeler>
#tablo(("Şube türü", "Çalışma saatleri", "Gişe işlemleri"),
  ("Cadde şubeleri", "Hafta içi 09.00-17.00", "16.30'a kadar"),
  ("Alışveriş merkezi şubeleri", "Hafta içi 10.00-19.00, cumartesi 10.00-17.00", "Kapanıştan 30 dakika öncesine kadar"),
)
- Şubeler pazar günleri ve resmî tatillerde kapalıdır. Arife günlerinde bütün şubeler saat 12.30'a kadar açıktır.
- Size en yakın şubeyi, çalışma saatlerini ve şubedeki yoğunluğu Raşit Mobil'de Şubeler ve ATM'ler ekranında görebilirsiniz.

#ara[Şube randevusu]
Şube ziyaretiniz için Raşit Mobil'den randevu alabilirsiniz. Randevulu müşteriler sıra beklemeden hizmet alır. Randevunuza 15 dakikadan fazla geç kalırsanız randevu iptal olur ve sıra numarası almanız gerekir. 65 yaş ve üzeri müşterilerimiz, hamile müşterilerimiz ve engelli müşterilerimiz randevusuz da sıra beklemeden hizmet alır.

== Şubede yapılan işlemler ve kiralık kasa <sube-islemleri>
Dijital kanallarda yapılabilen bütün işlemlerin yanında şu işlemler için şubeye gelmeniz gerekir:
- Ortak hesap ve 18 yaşından küçükler için hesap açma (#bkz(<ortak-cocuk>)),
- Vekâletname ile işlem ve vefat işlemleri (#bkz(<vefat>)),
- Efektif döviz yatırma ve çekme (#bkz(<doviz-hesabi>)),
- 250.000 TL'nin üzerinde nakit çekme (1 iş günü önceden haber verilerek),
- Kiralık kasa işlemleri.

#ara[Kiralık kasa]
Kiralık kasa, değerli eşyalarınızı ve önemli belgelerinizi şubenin kasa dairesinde saklamanız için kiraladığınız kilitli bir bölmedir. Hizmet 48 şubemizde verilir; bu şubeler Raşit Mobil'deki şube listesinde "Kiralık kasa" filtresiyle görülür.

#tablo(("Kasa boyutu", "Ölçüler (yükseklik × genişlik × derinlik)", "Yıllık kira ücreti"),
  ("Küçük", "5 × 25 × 45 cm", "2.400 TL"),
  ("Orta", "10 × 25 × 45 cm", "3.900 TL"),
  ("Büyük", "20 × 25 × 45 cm", "6.750 TL"),
  ("Çok büyük", "30 × 50 × 45 cm", "11.200 TL"),
)
- Kira ücreti yıllık ve peşin alınır. Ayrıca 2.000 TL anahtar teminatı alınır; teminat, kasa boşaltılıp anahtarlar teslim edildiğinde iade edilir.
- Kasa bir kişi ya da en fazla üç kişi adına kiralanabilir. Kiracıya iki anahtar verilir ve anahtarların kopyası bankada bulunmaz. Kasa, kiracının anahtarı ve bankanın ana anahtarı birlikte kullanılarak açılır.
- Kasanıza hafta içi 09.00-16.30 saatleri arasında erişebilirsiniz. Her ziyarette kimlik kontrolü yapılır ve ziyaret kayıt altına alınır. Kasaya ne koyduğunuz banka tarafından bilinmez ve kayıt altına alınmaz.
- Kiralık kasanın içindekiler Raşit Bankası tarafından sigortalanmaz. Nakit para, kolay bozulan, yanıcı, patlayıcı ya da yasa dışı eşyalar kasada saklanamaz.
- *Anahtar kaybı:* Kasa anahtarınızı kaybederseniz hemen şubenize bildirin. Kasanın kilidi, kiracının ve iki banka görevlisinin hazır bulunduğu tutanakla açılır ve değiştirilir; kilit değişim ücreti 3.250 TL'dir.
- *Kiranın bitmesi:* Kira yenilenmez ve ücret ödenmezse, bankanın yazılı bildiriminden 60 gün sonra kasa noter huzurunda açılır ve içindekiler tutanakla bankanın gözetimine alınır. Gözetim süresince yıllık kira ücreti kadar saklama ücreti alınır.

== ATM hizmetleri ve kartsız işlem <atm>
Raşit Bankası ATM'leri 7 gün 24 saat hizmet verir. ATM'lerde şu işlemleri yapabilirsiniz:
- Banka kartıyla nakit çekme ve kredi kartıyla nakit avans,
- Para yatırma özellikli ATM'lerde Türk lirası yatırma,
- Havale, FAST, kredi kartı borcu ve fatura ödeme,
- Kart şifresi ve dijital bankacılık şifresi işlemleri.

#ara[Kartsız para çekme]
+ Raşit Mobil'de Kartsız işlemler > Para çek ekranında tutarı girin; 6 haneli bir kod alırsınız.
+ Kod 30 dakika geçerlidir. Raşit Bankası ATM'sinde "Kartsız işlemler" seçeneğinde telefon numaranızı ve kodu girin.
+ Kodu girmek yerine ATM ekranındaki karekodu Raşit Mobil'le okutarak da para çekebilirsiniz (#bkz(<karekod>)).

#ara[Kartsız para yatırma]
Para yatırma özellikli ATM'lerde kartınız olmadan, IBAN ya da Raşit Bankası hesap numarası girerek kendi hesabınıza ya da başkasının hesabına Türk lirası yatırabilirsiniz. ATM'ye yatırdığınız para hesaba anında geçer.

ATM'de çekme ve yatırma limitleri için #bkz(<atm-limit>).

== ATM nakit çekme ve yatırma limitleri <atm-limit>
#tablo(("İşlem", "Limit"),
  ("Banka kartıyla Raşit Bankası ATM'lerinden nakit çekme", [Günlük #S.banka-karti-atm-gunluk]),
  ("Kartsız nakit çekme (kodla ya da karekodla)", "İşlem başına 5.000 TL, günlük 10.000 TL"),
  ("Kredi kartıyla nakit avans", "Nakit avans limitine kadar"),
  ("Kartla para yatırma", "Tek seferde en fazla 200 banknot"),
  ("Kartsız para yatırma", "Günlük 50.000 TL"),
)
- Banka kartıyla nakit çekme limitini Raşit Mobil'den günlük 50.000 TL'ye kadar artırabilirsiniz (#bkz(<banka-karti>)). Kartsız çekme ve kartsız yatırma limitleri değiştirilemez.
- Kredi kartının nakit avans limiti, kart limitinin %50'sidir (#bkz(<nakit-avans>)).
- ATM'ler yalnızca 10, 20, 50, 100 ve 200 TL'lik banknotları kabul eder. ATM'nin tanımadığı banknotlar size geri verilir.
- Başka banka ATM'lerinden çekme limiti için #bkz(<baska-atm>).

== Başka banka ATM'leri <baska-atm>
Raşit Bankası kartlarınızla Türkiye'deki bütün bankaların ATM'lerinden nakit çekebilir ve kredi kartı borcunuzu ödeyebilirsiniz.

#tablo(("İşlem", "Ücret"),
  ("Banka kartıyla nakit çekme", "21,75 TL"),
  ("Kredi kartıyla nakit avans", "21,75 TL ve nakit avans işlem ücreti"),
  ("Kredi kartı borcu ödeme", "Ücretsiz"),
)
- Raşit Bankası'ndan maaş ya da emekli maaşı alan müşterilerimiz başka banka ATM'lerinden ayda 5 kez ücretsiz nakit çekebilir.
- Banka kartıyla başka banka ATM'lerinden günlük nakit çekme limiti 10.000 TL'dir.
- Başka banka ATM'lerine para yatırılamaz.
- ATM'nin sahibi olan banka, işlemden önce ekranda göstererek ayrıca ücret alabilir. Raşit Bankası'nın ücreti işlem anında hesabınızdan tahsil edilir.

Raşit Bankası ATM'lerinde yapılan işlemler ücretsizdir. Yurt dışı ATM'ler için #bkz(<yurt-disi-kart>).

== ATM'de yaşanan sorunlar <atm-sorun>
#ara[Para çıkmadı ama hesabımdan düştü]
ATM'den para çıkmadığı hâlde tutar hesabınızdan düştüyse ATM ekranındaki numarayı ya da Müşteri Hizmetleri'ni arayarak bildirin.
- Raşit Bankası ATM'lerinde ATM sayımı yapılır ve fazla çıkan tutar en geç 3 iş günü içinde hesabınıza iade edilir.
- Başka banka ATM'lerinde yaşanan sorunlarda süreç ilgili bankanın sayımına bağlıdır ve en geç 10 iş günü sürer.

#ara[Kartım ATM'de kaldı]
Kartınız ATM'de kaldıysa Müşteri Hizmetleri'ni arayın. ATM'de kalan kartlar güvenlik nedeniyle iade edilmez ve imha edilir; yeni kartınız ücretsiz olarak kayıtlı adresinize gönderilir (#bkz(<kart-yenileme>)).

#ara[Yatırdığım parayı ATM almadı ya da sahte sandı]
- ATM'nin tanımadığı banknotlar işlem sırasında size geri verilir.
- ATM'nin sahte olduğundan şüphelendiği banknotlar ATM'de tutulur ve hesabınıza yatırılmaz. İnceleme sonucu en geç 3 iş günü içinde SMS ile bildirilir. Banknotların gerçek olduğu anlaşılırsa tutar hesabınıza yatırılır; sahte olduğu anlaşılırsa banknotlar mevzuat gereği ilgili kuruma teslim edilir.

#ara[Şüpheli ATM]
ATM'nin kart girişinde ya da tuş takımında olağan dışı bir düzenek görürseniz işlem yapmayın ve Müşteri Hizmetleri'ni arayın. Şifrenizi girerken tuş takımını elinizle kapatın.

== Müşteri hizmetleri ve iletişim kanalları <musteri-hizmetleri>
#tablo(("Kanal", "Bilgi", "Saatler"),
  ("Müşteri Hizmetleri", "0850 724 72 74", "7 gün 24 saat"),
  ("Yurt dışından arama", "+90 216 724 72 74", "7 gün 24 saat"),
  ("Raşit Mobil'de dijital asistan", "Raşit Mobil > Destek", "7 gün 24 saat"),
  ("E-posta", [musteri\@rasitbank.example], "Cevap en geç 2 iş günü"),
  ("İnternet sitesi", "www.rasitbank.example", "7 gün 24 saat"),
)

#ara[Müşteri Hizmetleri menüsü]
#tablo(("Tuş", "Konu"),
  ("1", "Kartlar: kayıp ya da çalıntı bildirimi, kart kapatma"),
  ("2", "Hesaplar ve para transferleri"),
  ("3", "Krediler ve Ek Hesap"),
  ("4", "Dijital bankacılık: şifre ve cihaz işlemleri"),
  ("9", "Dolandırıcılık bildirimi"),
)
- Kart kapatma (1) ve dolandırıcılık bildirimi (9) her zaman önceliklidir ve kimlik doğrulamasından önce yapılabilir.
- Müşteri temsilcisine bağlanmadan önce kimliğiniz T.C. kimlik numaranız ve telefon bankacılığı şifreniz ya da Raşit Mobil'e gönderilen onay bildirimiyle doğrulanır.

#ara[Telefon bankacılığı şifresi]
Telefon bankacılığı şifresi, Müşteri Hizmetleri'nde kimliğinizi doğrulamak için kullanılan 4 haneli şifredir ve dijital bankacılık şifrenizden ayrıdır. Şifreyi Raşit Mobil'de Ayarlar > Güvenlik > Telefon bankacılığı şifresi ekranından ya da Raşit Bankası ATM'lerinden belirleyebilirsiniz. Şifrenizi unuttuysanız aynı ekrandan yenisini belirleyebilirsiniz; şifre 3 kez hatalı girilirse Raşit Mobil onay bildirimiyle doğrulama yapılır.

#ara[Görüşme kayıtları]
Müşteri Hizmetleri görüşmeleri hizmet kalitesi ve mevzuat gereği kaydedilir. Bir görüşmeyle ilgili şikâyetiniz varsa görüşmenin tarihini ve saatini belirterek başvurabilirsiniz (#bkz(<sikayet>)).

#ara[Dijital asistan]
Raşit Mobil'deki dijital asistan, soruları bu rehberdeki bilgilere dayanarak 7 gün 24 saat cevaplar ve cevabın dayandığı rehber bölümünü gösterir. Dijital asistan işlem yapmaz. Dijital asistanın cevaplayamadığı sorular için "Temsilciye bağlan" seçeneğiyle müşteri temsilcisine aktarılırsınız; temsilciye aktarma hafta içi 08.00-22.00, hafta sonu 09.00-18.00 saatleri arasında yapılır.

== Engelli müşterilerimiz için hizmetler <engelli>
- *Erişilebilir şubeler:* Şubelerimizin büyük çoğunluğu tekerlekli sandalyeyle erişime uygundur; erişilebilir şubeler Raşit Mobil'deki şube listesinde işaretlidir. Engelli müşterilerimiz şubelerde sıra beklemeden hizmet alır.
- *Görme engelli müşterilerimiz:* Sesli yönlendirmeli ATM'lerimizde kulaklık girişi bulunur. Raşit Mobil, telefonların ekran okuyucu özellikleriyle kullanılabilir.
- *İşitme engelli müşterilerimiz:* Raşit Mobil'deki "Görüntülü destek" seçeneğiyle hafta içi 09.00-18.00 saatleri arasında işaret dili bilen temsilcimizle görüşebilirsiniz. Ayrıca e-posta ve Raşit Mobil'deki yazılı destek kanalı 7 gün 24 saat açıktır.
- *Erişilebilir belgeler:* Kredi kartı ekstresi, sözleşmeler ve bu rehber talep üzerine Braille alfabesiyle ya da büyük puntolu olarak ücretsiz gönderilir.

Engelli müşterilerimiz için kredi ve hesap sözleşmeleri, isterlerse bir tanık ya da yasal temsilcilerinin huzurunda şubede sesli olarak okunur.

== İletişim bilgilerinin güncellenmesi <iletisim-guncelleme>
#ara[Telefon numarası]
Telefon numaranızı Raşit Mobil'de Profilim > Kişisel bilgilerim ekranından, çipli kimlik kartınızı NFC ile okutup yeni numaranıza gelen SMS kodunu onaylayarak değiştirebilirsiniz. Kimlik kartınız çipli değilse numara değişikliği şubeden yapılır. Güvenlik nedeniyle numara değişikliğinden sonraki 24 saat boyunca başka kişilere yapılan transferler günlük toplam 10.000 TL ile sınırlıdır (#bkz(<cihaz>)).

#ara[E-posta ve meslek bilgisi]
E-posta adresinizi ve meslek bilginizi Raşit Mobil'de Profilim > Kişisel bilgilerim ekranından istediğiniz an güncelleyebilirsiniz. E-posta değişikliği, yeni adrese gönderilen bağlantıyla onaylanır.

#ara[Adres]
Yerleşim yeri adresiniz e-Devlet'teki adres kayıt sisteminden alınır. Taşındığınızda yeni adresinizi nüfus müdürlüğüne ya da e-Devlet'e bildirmeniz yeterlidir; yeni adres en geç 1 iş günü içinde kayıtlarımıza yansır. Kartların ve yazışmaların gönderilmesi için Raşit Mobil'den ayrı bir teslimat adresi tanımlayabilirsiniz.

#sss()
#soru[Cumartesi açık şubeniz var mı?][Evet. Alışveriş merkezlerindeki şubelerimiz cumartesi 10.00-17.00 saatleri arasında açıktır (#bkz(<subeler>)).]
#soru[ATM paramı vermedi ama hesabımdan düştü. Ne olacak?][Durumu Müşteri Hizmetleri'ne bildirin. Raşit Bankası ATM'lerinde fazla çıkan tutar en geç 3 iş günü, başka banka ATM'lerinde en geç 10 iş günü içinde iade edilir (#bkz(<atm-sorun>)).]
#soru[Müşteri Hizmetleri'nin numarası nedir?][0850 724 72 74. Yurt dışından +90 216 724 72 74 numarasını arayabilirsiniz. İki hat da 7 gün 24 saat açıktır (#bkz(<musteri-hizmetleri>)).]
#soru[İşitme engelliyim. Sizinle nasıl iletişim kurabilirim?][Raşit Mobil'deki "Görüntülü destek" seçeneğiyle hafta içi 09.00-18.00 saatleri arasında işaret dili bilen temsilcimizle görüşebilirsiniz (#bkz(<engelli>)).]
#soru[Kiralık kasa ücreti ne kadar?][Kasa boyutuna göre yıllık 2.400 TL ile 11.200 TL arasındadır. Ayrıca 2.000 TL iade edilen anahtar teminatı alınır (#bkz(<sube-islemleri>)).]
#soru[Kartım yanımda değil. ATM'den para çekebilir miyim?][Evet. Raşit Mobil'den alacağınız 6 haneli kodla ya da ATM ekranındaki karekodu okutarak işlem başına en fazla 5.000 TL çekebilirsiniz (#bkz(<atm>) ve #bkz(<atm-limit>)).]
