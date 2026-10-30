#import "../ortak.typ": *

= Dijital bankacılık <bolum-dijital>

== Raşit Mobil ve İnternet Şubesi <mobil>
Raşit Mobil, akıllı telefonlar için mobil bankacılık uygulamamızdır. İnternet Şubesi'ne www.rasitbank.example adresinden bilgisayarınızla girebilirsiniz. İki kanal da ücretsizdir ve 7 gün 24 saat kullanılabilir.

#ara[Neler yapabilirsiniz]
- Hesap açma ve kapatma, vadeli ve Kumbara hesabı işlemleri,
- Havale, EFT, FAST, SWIFT ve düzenli transfer talimatları,
- Kredi kartı ve banka kartı işlemleri: limit değişikliği, sanal kart, geçici kapatma, harcama itirazı,
- Kredi ve Ek Hesap başvurusu, erken kapama,
- Fatura, vergi ve kurum ödemeleri,
- Yatırım hesabı açma; fon, hisse senedi, döviz ve altın işlemleri,
- Şube randevusu, şikâyet ve talep oluşturma.

#ara[Desteklenen cihazlar]
- Raşit Mobil, Android 10 ve üzeri ile iOS 16 ve üzeri sürümlerde çalışır.
- Güvenlik nedeniyle root ya da jailbreak yapılmış cihazlarda Raşit Mobil açılmaz.
- Raşit Mobil'i yalnızca resmî uygulama mağazalarından indirin. Raşit Bankası uygulamayı SMS ya da e-posta ile gönderilen bağlantılar üzerinden dağıtmaz.
- İnternet Şubesi, güncel sürümdeki yaygın internet tarayıcılarıyla çalışır.

#ara[Oturum süresi]
İşlem yapılmadığında Raşit Mobil oturumu 5 dakika, İnternet Şubesi oturumu 10 dakika sonra kendiliğinden kapanır. Aynı anda yalnızca bir oturum açık olabilir; başka bir cihazdan giriş yapıldığında önceki oturum kapanır.

#ara[Uygulama güncellemeleri ve dil]
- Raşit Mobil'in yeni sürümleri uygulama mağazalarında yayımlanır. Güvenlik güncellemesi içeren sürümler zorunludur; zorunlu güncelleme yapılmadan uygulamaya giriş yapılamaz.
- Raşit Mobil ve İnternet Şubesi Türkçe ve İngilizce kullanılabilir. Dili Ayarlar > Dil ekranından değiştirebilirsiniz; sözleşmeler ve yasal bildirimler Türkçe gönderilir.
- Erişim sorunu yaşarsanız uygulamayı güncelleyin, telefonunuzun tarih ve saat ayarının otomatik olduğunu kontrol edin ve internet bağlantınızı değiştirerek yeniden deneyin. Sorun sürerse Müşteri Hizmetleri'nde dijital bankacılık menüsünü (4) seçin.

== İlk giriş ve şifre alma <ilk-giris>
Raşit Mobil'e ve İnternet Şubesi'ne T.C. kimlik numaranız ya da müşteri numaranız ve 6 haneli dijital bankacılık şifrenizle giriş yaparsınız.

#ara[Şifrenizi ilk kez almak]
+ Raşit Mobil'de giriş ekranındaki "Şifre al" adımını seçin.
+ Banka kartınızın ya da kredi kartınızın numarasını ve kart şifrenizi girin.
+ Telefonunuza SMS ile gelen doğrulama kodunu girin.
+ 6 haneli dijital bankacılık şifrenizi belirleyin.

Kartınız yoksa şifrenizi kimliğinizle şubeden alabilirsiniz. Raşit Mobil'den müşteri olduysanız şifrenizi müşteri olma adımlarının sonunda belirlersiniz (#bkz(<musteri-olma>)).

#ara[Şifre kuralları]
- Dijital bankacılık şifresi 6 rakamdan oluşur.
- Doğum tarihiniz, telefon numaranızın bir bölümü, art arda gelen (123456) ya da tekrar eden (111111) rakamlar şifre olarak kabul edilmez.
- Son 3 şifrenizi yeniden kullanamazsınız.
- Şifrenizi 3 kez hatalı girerseniz dijital bankacılık şifreniz bloke olur (#bkz(<sifre-sifirlama>)).

== Şifre sıfırlama <sifre-sifirlama>
Şifrenizi unuttuysanız ya da şifreniz bloke olduysa aşağıdaki yollardan biriyle yeni şifre belirleyebilirsiniz:

#tablo(("Yol", "Ne gerekir"),
  ("Raşit Mobil'de Şifremi unuttum", "Kart numarası, kart şifresi ve SMS doğrulama kodu"),
  ("Raşit Mobil'de kimlikle sıfırlama", "Çipli kimlik kartı (NFC) ve yüz doğrulaması"),
  ("Raşit Bankası ATM'si", "Kartınız ve kart şifreniz; Dijital şifre işlemleri menüsü"),
  ("Şube", "Kimlik belgeniz"),
)
- Yeni şifre hemen geçerli olur ve blokeniz kalkar.
- Müşteri Hizmetleri güvenlik nedeniyle telefonda yeni şifre veremez ve şifrenizi sormaz; blokenin nasıl kaldırılacağı konusunda size yol gösterir.
- Şifreniz sıfırlandıktan sonraki 24 saat boyunca başka kişilere yapılan transferler günlük toplam 10.000 TL ile sınırlıdır (#bkz(<cihaz>)).

Şifrenizi Raşit Mobil'de Ayarlar > Güvenlik > Şifre değiştir ekranından istediğiniz zaman değiştirebilirsiniz. Şifrenizi başkasıyla paylaştığınızı düşünüyorsanız hemen değiştirin.

== Cihaz tanımlama <cihaz>
Raşit Mobil'i yeni bir telefona kurduğunuzda telefonunuz hesabınızla eşlenir. Eşleme için SMS doğrulamasının yanında çipli T.C. kimlik kartınızın çipi NFC ile okunur. Kimlik kartınız çipli değilse eşleme şubeden yapılır.
- Aynı anda en fazla 2 cihaz eşli olabilir. Üçüncü bir cihaz eşlendiğinde en eski eşleme kendiliğinden kaldırılır.
- Eşli cihazlarınızı Raşit Mobil'de ya da İnternet Şubesi'nde Ayarlar > Eşli cihazlar ekranında görebilir ve kaldırabilirsiniz.
- 180 gün boyunca Raşit Mobil'e giriş yapılmayan cihazların eşlemesi güvenlik nedeniyle kendiliğinden kaldırılır.
- Telefonunuzu fabrika ayarlarına döndürdüğünüzde ya da uygulamayı silip yeniden kurduğunuzda eşleme adımlarını yeniden yapmanız gerekir.

#ara[Yeni cihazda ilk 24 saat]
Yeni bir cihaz eşlendikten, telefon numaranız değiştikten ya da şifreniz sıfırlandıktan sonraki 24 saat boyunca, güvenlik nedeniyle başka kişilere yapılan havale, EFT ve FAST işlemlerinin günlük toplamı 10.000 TL ile sınırlanır.
- Bu sınır limit artırma talebiyle kaldırılamaz; 24 saat dolduğunda kendiliğinden kalkar.
- Kendi hesaplarınız arasındaki transferler ve fatura ödemeleri bu sınırdan etkilenmez.
- Daha yüksek bir tutarı hemen göndermeniz gerekiyorsa işlemi kimliğinizle şubeden yapabilirsiniz.

== Yüz tanıma ve parmak izi ile giriş <biyometrik>
Eşli cihazınızda dijital bankacılık şifreniz yerine parmak izi ya da yüz tanıma ile giriş yapabilirsiniz.
- Biyometrik girişi Raşit Mobil'de Ayarlar > Güvenlik ekranından açarsınız; açarken bir kez şifreniz istenir.
- Biyometrik doğrulama, 50.000 TL'ye kadar transferlerin onayında da kullanılabilir. Bu tutarın üzerindeki transferlerde şifreniz istenir.
- Telefonunuza yeni bir parmak izi ya da yüz kaydı eklendiğinde biyometrik giriş güvenlik nedeniyle kapanır. Bir sonraki girişte şifrenizi girip özelliği yeniden açabilirsiniz.
- Raşit Bankası biyometrik verinizi saklamaz; doğrulama telefonunuzun kendi güvenlik sistemiyle yapılır.

== Dijital işlem limitleri <dijital-limit>
Raşit Mobil ve İnternet Şubesi'nden yapılan transfer ve ödemelerin günlük limitleri vardır. Limitler hesabınızı izinsiz erişime karşı korur ve sizin tarafınızdan değiştirilebilir.

#tablo(("İşlem", "Varsayılan günlük limit", "Raşit Mobil'den artırılabilecek üst limit"),
  ("Başka kişilere havale, EFT ve FAST (toplam)", "250.000 TL", "1.000.000 TL"),
  ("Kendi hesaplarınız arasında transfer", "Sınırsız", "Sınırsız"),
  ("Yurt dışına SWIFT", "25.000 ABD doları karşılığı", "100.000 ABD doları karşılığı"),
  ("Fatura ve kurum ödemeleri", "50.000 TL", "200.000 TL"),
  ("Döviz ve altın alımı", "1.000.000 TL", "5.000.000 TL"),
)
- Limitler her gün saat 00.00'da yenilenir ve aynı gün Raşit Mobil ile İnternet Şubesi'nden yapılan işlemlerin toplamına uygulanır.
- Bir işlem limitinizi aşarsa "İşlem limitinizi aşıyor" uyarısı ve o gün kullanabileceğiniz kalan limit gösterilir.

#ara[Limit artırma ve düşürme]
- Limitinizi Raşit Mobil'de Ayarlar > İşlem limitleri ekranından artırabilirsiniz. Artırma talebi SMS doğrulama koduyla onaylanır ve güvenlik nedeniyle 24 saat sonra geçerli olur. Limitinizi artırdığınız halde işlem yapamıyorsanız bu 24 saatin dolmasını bekleyin.
- Üst limitin üzerindeki tutarlar için işlemi şubeden yapabilirsiniz.
- Limit düşürme hemen geçerli olur ve doğrulama gerektirmez. Kullanmadığınız bir işlem türünün limitini 0 yaparak o işlemi dijital kanallarda tamamen kapatabilirsiniz.

Örnek: Varsayılan limitle aynı gün içinde Raşit Mobil'den 180.000 TL FAST ve EFT yaptıysanız, aynı gün İnternet Şubesi'nden başka kişilere en fazla 70.000 TL daha gönderebilirsiniz. Kendi hesaplarınız arasındaki aktarımlar bu toplama girmez.

#ara[İlk kez para gönderilen alıcılar]
Daha önce para göndermediğiniz bir alıcıya yapılan ilk transfer 50.000 TL'yi aşıyorsa, işlemi onaylamanız için telefonunuza ayrıca bir arama ya da uygulama bildirimi gönderilir. Onay vermezseniz transfer gerçekleşmez. Bu kontrol, dolandırıcılıkla yönlendirilen transferleri durdurmak için yapılır.

== Bildirimler ve SMS <bildirim>
- *İşlem bildirimleri:* Her kart harcamasında, hesabınıza para girişinde ve çıkışında Raşit Mobil'den anlık bildirim alabilirsiniz. Bildirimleri Ayarlar > Bildirimler ekranından açıp kapatabilirsiniz. Uygulama bildirimleri ücretsizdir.
- *Güvenlik SMS'leri:* Doğrulama kodları, şifre değişikliği, yeni cihaz eşleme ve kart kapatma gibi güvenlik bildirimleri her zaman SMS ile de gönderilir ve kapatılamaz. Güvenlik SMS'leri ücretsizdir.
- *SMS başlığı:* Raşit Bankası SMS'leri RASITBANK başlığıyla gelir. Bu başlıkla gelmeyen ve Raşit Bankası adına gönderildiğini söyleyen mesajlara güvenmeyin.
- *SMS ile işlem bildirimi:* Uygulama bildirimi yerine kart harcamalarınız için SMS almak isterseniz SMS bildirim hizmetini açabilirsiniz. Hizmet ücretsizdir.
- *E-posta:* Ekstreler, sözleşmeler ve dekontlar kayıtlı e-posta adresinize gönderilebilir. Raşit Bankası e-postaları yalnızca rasitbank.example alan adından gelir.

#ara[Bildirim ya da SMS gelmiyorsa]
- Telefonunuzun ayarlarında Raşit Mobil bildirimlerine izin verildiğini ve pil tasarrufu modunun uygulamayı kısıtlamadığını kontrol edin.
- Doğrulama SMS'leri, bankamızda kayıtlı telefon numaranıza gönderilir. Numaranız değiştiyse önce numaranızı güncelleyin (#bkz(<iletisim-guncelleme>)).
- Mobil operatörünüzün mesaj engelleme hizmeti açıksa RASITBANK başlığına izin verin.
- Doğrulama kodu 3 dakika içinde gelmezse yeni kod isteyebilirsiniz; aynı işlem için en fazla 3 kez kod gönderilir.

Kampanya ve tanıtım amaçlı mesajlar için izin tercihleriniz #bkz(<aydinlatma>) içinde anlatılır.

== Güvenlik önerileri <guvenlik>
- Raşit Mobil'i ve telefonunuzun işletim sistemini güncel tutun.
- Telefonunuzda ekran kilidi kullanın. Dijital bankacılık şifrenizi telefonunuzda not olarak saklamayın.
- Ekran paylaşımı ya da uzaktan erişim uygulamalarını, sizi arayan birinin yönlendirmesiyle kurmayın.
- Ortak kullanılan bilgisayarlarda İnternet Şubesi'nden çıkış yapmayı unutmayın; tarayıcıya şifre kaydettirmeyin.
- Kart ve hesap bilgilerinizi sosyal medyada paylaşmayın, kartınızın fotoğrafını kimseye göndermeyin.

#ara[Güvenlik ayarları]
- *Gece işlem kilidi:* Dijital kanallardan başka kişilere yapılan transferleri her gece 00.00 ile 07.00 arasında kapatabilirsiniz.
- *Güvenli alıcı listesi:* Transferleri yalnızca kayıtlı alıcılarınıza izin verecek şekilde sınırlandırabilirsiniz. Listeye eklenen yeni alıcı 12 saat sonra kullanılabilir.
- *Yurt dışı ve internet kullanımı:* Kartlarınızın yurt dışı kullanımını ve internet alışverişini ihtiyacınız olmadığında kapalı tutun (#bkz(<sanal-kart>)).

Bu ayarların hepsi Raşit Mobil'de Ayarlar > Güvenlik ekranındadır ve hemen geçerli olur.

== Dolandırıcılık uyarıları ve dolandırıldığınızda yapılacaklar <dolandiricilik>
#ara[Raşit Bankası sizden asla istemez]
Raşit Bankası çalışanları telefon, SMS, e-posta ya da sosyal medya üzerinden hiçbir zaman şunları istemez:
- Kart şifreniz, dijital bankacılık şifreniz ya da SMS ile gelen doğrulama kodu,
- Kartınızın tam numarası, son kullanma tarihi ve güvenlik kodu (CVV),
- "Hesabınızı korumaya alacağız" gerekçesiyle başka bir hesaba para göndermeniz,
- Telefonunuza uzaktan erişim uygulaması kurmanız.

Bu bilgileri isteyen arama ve mesajlar dolandırıcılık girişimidir. Kendini bankacı, polis, savcı ya da kamu görevlisi olarak tanıtan biri size para transferi yaptırmaya çalışıyorsa görüşmeyi kapatın ve Müşteri Hizmetleri'ni arayın. Raşit Bankası SMS'lerinde sizi şifre girmeye yönlendiren bağlantı bulunmaz.

#ara[Sık karşılaşılan dolandırıcılık yöntemleri]
#tablo(("Yöntem", "Nasıl tanırsınız"),
  ("Sahte kargo, ceza ya da aidat iadesi mesajı", "Kart bilgisi ya da şifre isteyen bir bağlantı içerir"),
  ("Kendini kamu görevlisi olarak tanıtan arama", "Paranızı korumak için başka hesaba göndermenizi ister"),
  ("Sahte müşteri hizmetleri hesabı", "Sosyal medyadan size ulaşır, doğrulama kodu ister"),
  ("İkinci el alışverişte sahte dekont", "Ödemenin yapıldığını gösteren bir ekran görüntüsü gönderir"),
  ("Yüksek kazanç vaadi", "Kısa sürede kesin ve yüksek kazanç vaat eder, önce para ister"),
  ("Uzaktan erişim tuzağı", "Bir sorunu çözmek için telefonunuza uygulama kurdurur"),
)
İkinci el satışlarda paranın hesabınıza geçtiğini ekran görüntüsüyle değil, Raşit Mobil'deki işlem bildirimiyle kontrol edin.

#ara[Dolandırıldığınızı düşünüyorsanız]
Şifrenizi ya da doğrulama kodunuzu biriyle paylaştıysanız veya bilginiz dışında bir işlem gördüyseniz:
+ Hemen Müşteri Hizmetleri'ni arayıp menüde 9'u tuşlayın (#bkz(<musteri-hizmetleri>)). Dolandırıcılık hattı 7 gün 24 saat açıktır. Kartlarınız ve dijital bankacılık erişiminiz geçici olarak kapatılır.
+ Size verilen başvuru numarasını not alın.
+ En yakın polis merkezine ya da Cumhuriyet başsavcılığına şikâyette bulunun. Alıcı bankada tutarın bloke edilebilmesi için resmî şikâyet kaydı gerekir.
+ Kartınızla yapılan işlemler için harcama itirazı başlatın (#bkz(<harcama-itirazi>)).

Dolandırıcılık bildirimi alındığında Raşit Bankası, paranın gönderildiği bankaya hemen bildirim yapar. Tutarın geri alınabilmesi, paranın alıcı hesapta hâlâ durmasına bağlıdır; bu yüzden bildirimin ilk saatler içinde yapılması çok önemlidir.

== Telefonunuz kaybolursa ya da çalınırsa <telefon-kaybi>
+ Müşteri Hizmetleri'ni arayarak dijital bankacılık erişiminizi hemen kapattırın (#bkz(<musteri-hizmetleri>)).
+ Başka bir cihazdan İnternet Şubesi'ne girip Ayarlar > Eşli cihazlar ekranından kaybolan telefonun eşlemesini kaldırın.
+ Telefon hattınızı (SIM kartınızı) mobil operatörünüze kapattırın; doğrulama kodları bu hatta gelir.
+ Telefonunuza tanımlı kartla telefonla ödeme yapılıyorsa Raşit Mobil'den ya da Müşteri Hizmetleri'nden kartlarınızı geçici olarak kapatın.

Yeni telefonunuzda Raşit Mobil'i kurduğunuzda cihaz eşleme adımlarını yeniden yaparsınız (#bkz(<cihaz>)). Yeni hatla numaranız değiştiyse numara güncellemesi için #bkz(<iletisim-guncelleme>).

#if S.sifre-cihazi [
  == Şifre cihazı <sifre-cihazi>
  Akıllı telefon kullanmayan ve İnternet Şubesi'ne girişte SMS doğrulaması almak istemeyen müşterilerimiz, tek kullanımlık giriş kodunu şifre cihazıyla üretebilir.
  - Şifre cihazı şubelerden kimlikle başvurularak alınır ve 150 TL'ye verilir.
  - Cihaz yalnızca adına tanımlandığı müşteri için kod üretir. Ürettiği kod 60 saniye geçerlidir.
  - Cihaz kaybolursa ya da çalınırsa Müşteri Hizmetleri'ni arayarak cihazı kapattırın. Kaybolan ya da arızalanan cihazın yerine verilen yenisi de 150 TL'dir.
  - Şifre cihazı Raşit Mobil'de kullanılmaz; Raşit Mobil'de doğrulama eşli cihazla yapılır.
]

#sss()
#soru[Mobil şifremi unuttum. Ne yapmalıyım?][Raşit Mobil'de "Şifremi unuttum" adımında kart bilgilerinizi ve kart şifrenizi girip SMS kodunu onaylayarak yeni şifre belirleyebilirsiniz. Kartınız yoksa çipli kimlik kartınızla ya da şubeden sıfırlayabilirsiniz (#bkz(<sifre-sifirlama>)).]
#soru[Müşteri Hizmetleri bana yeni şifre verebilir mi?][Hayır. Güvenlik nedeniyle telefonda yeni şifre verilmez; temsilci size şifrenizi nasıl sıfırlayacağınızı anlatır (#bkz(<sifre-sifirlama>)).]
#soru[Yeni telefona geçtim. Neden fazla para gönderemiyorum?][Yeni cihaz eşlendikten sonraki 24 saat boyunca başka kişilere yapılan transferler günlük toplam 10.000 TL ile sınırlıdır. Sınır 24 saat sonra kendiliğinden kalkar (#bkz(<cihaz>)).]
#soru[Kaç telefonda aynı anda Raşit Mobil kullanabilirim?][En fazla 2 cihaz eşli olabilir. Üçüncü cihaz eşlendiğinde en eski eşleme kaldırılır (#bkz(<cihaz>)).]
#soru[Banka beni arayıp SMS kodunu istedi. Vermeli miyim?][Hayır. Raşit Bankası doğrulama kodunuzu hiçbir zaman sormaz. Görüşmeyi kapatın ve Müşteri Hizmetleri'ni arayıp menüde 9'u tuşlayın (#bkz(<dolandiricilik>)).]
#soru[Gece transferleri kapatabilir miyim?][Evet. Gece işlem kilidini açarak başka kişilere yapılan dijital transferleri her gece 00.00 ile 07.00 arasında kapatabilirsiniz (#bkz(<guvenlik>)).]
#soru[Yüz tanıma ile giriş neden kapandı?][Telefonunuza yeni bir yüz ya da parmak izi kaydı eklendiğinde biyometrik giriş güvenlik nedeniyle kapanır. Şifrenizle girip yeniden açabilirsiniz (#bkz(<biyometrik>)).]
#soru[Telefonum çalındı. Hesaplarım güvende mi?][Hemen Müşteri Hizmetleri'ni arayıp dijital bankacılık erişiminizi kapattırın ve hattınızı operatörünüze kapattırın. Eşli cihazı İnternet Şubesi'nden kaldırabilirsiniz (#bkz(<telefon-kaybi>)).]
#soru[Mobilden günde ne kadar transfer yapabilirim?][Başka kişilere havale, EFT ve FAST için varsayılan günlük limit toplam 250.000 TL'dir; Raşit Mobil'den 1.000.000 TL'ye kadar artırabilirsiniz (#bkz(<dijital-limit>)).]
#soru[Transfer limitimi artırdım ama hâlâ işlem yapamıyorum.][Limit artırma güvenlik nedeniyle 24 saat sonra geçerli olur. Yeni cihaz eşlediyseniz 24 saatlik 10.000 TL sınırı da uygulanıyor olabilir (#bkz(<cihaz>) ve #bkz(<dijital-limit>)).]
#soru[İlk kez para gönderdiğim kişi için neden onay istendi?][Daha önce para göndermediğiniz bir alıcıya 50.000 TL'yi aşan ilk transferde ayrıca onay istenir. Bu kontrol dolandırıcılığa karşı yapılır (#bkz(<dijital-limit>)).]
#soru[Raşit Bankası SMS'leri hangi başlıkla gelir?][RASITBANK başlığıyla. Bu başlıkla gelmeyen mesajlara güvenmeyin (#bkz(<bildirim>)).]
#soru[Root yapılmış telefonda uygulama çalışır mı?][Hayır. Güvenlik nedeniyle Raşit Mobil root ya da jailbreak yapılmış cihazlarda açılmaz (#bkz(<mobil>)).]
