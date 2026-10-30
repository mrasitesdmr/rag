#import "../ortak.typ": *

= Kartlar <bolum-kartlar>

== Raşit Banka Kartı <banka-karti>
Raşit Banka Kartı, vadesiz Türk lirası hesabınıza bağlı çalışan banka kartıdır. Alışveriş ve nakit çekme tutarları doğrudan hesabınızdaki paradan düşer; kartın borcu ya da ekstresi olmaz. Kart uluslararası kart altyapısıyla çalışır, temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir.

- Banka kartı için yıllık ücret alınmaz.
- Müşteri olduğunuzda banka kartınız kendiliğinden basılır ve kayıtlı adresinize kuryeyle gönderilir. İl merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde teslim edilir.
- Kart gelene kadar Raşit Mobil'de sanal banka kartı oluşturarak internetten alışveriş yapabilirsiniz (#bkz(<sanal-kart>)).
- Bir vadesiz hesaba en fazla 1 banka kartı bağlanır. Farklı hesaplarınız için ayrı banka kartı alabilirsiniz.
- 15 yaşını dolduran çocuklar için velinin onayıyla banka kartı verilebilir (#bkz(<ortak-cocuk>)).

#ara[Banka kartı limitleri]
Banka kartınızın günlük limitleri aşağıdadır. Limitler her gün saat 00.00'da yenilenir.

#tablo(("İşlem", "Günlük varsayılan limit", "Raşit Mobil'den artırılabilecek üst limit"),
  ("Raşit Bankası ATM'lerinden nakit çekme", S.banka-karti-atm-gunluk, "50.000 TL"),
  ("Başka banka ATM'lerinden nakit çekme", "10.000 TL", "20.000 TL"),
  ("Mağazada ve internette alışveriş", "100.000 TL", "250.000 TL"),
  ("Yurt dışında nakit çekme", "7.500 TL karşılığı", "15.000 TL karşılığı"),
  ("Yurt dışında alışveriş", "50.000 TL karşılığı", "100.000 TL karşılığı"),
)

- Limitlerinizi Raşit Mobil'de Kartlarım > kartı seçin > Kart limitleri ekranından değiştirebilirsiniz.
- Limit düşürme hemen geçerli olur. Limit artırma, güvenlik nedeniyle talepten 2 saat sonra geçerli olur.
- Üst limitin üzerinde nakit ihtiyacınız varsa şubeden kimliğinizle para çekebilirsiniz. Şubeden para çekmede limit uygulanmaz; 250.000 TL'nin üzerindeki çekimler için 1 iş günü önceden şubenize haber vermeniz gerekir.

Banka kartıyla ATM'de para yatırma ve ATM ücretleri için #bkz(<atm-limit>) ve #bkz(<baska-atm>).

#ara[Banka kartıyla alışveriş ve iade]
- Banka kartıyla yaptığınız alışverişin tutarı işlem anında hesabınızdan düşer. Hesabınızda yeterli para yoksa ve Ek Hesap limitiniz varsa eksik kısım Ek Hesap'tan karşılanır (#bkz(<ek-hesap>)); Ek Hesap'ınız yoksa işlem gerçekleşmez.
- Banka kartıyla taksitli alışveriş yapılamaz ve banka kartı harcamaları RaşitPuan kazandırmaz.
- İade edilen bir alışverişin tutarı, işyerinin iade işlemini yaptığı günden itibaren en geç 7 iş günü içinde hesabınıza geri yatar. İade süresi işyerinin bankasına bağlıdır; süre dolduğu hâlde iade gelmediyse harcama itirazı başlatabilirsiniz (#bkz(<harcama-itirazi>)).
- Otel ve araç kiralama gibi işyerleri ödeme öncesinde kartınızdaki tutarı geçici olarak bloke edebilir. Bloke tutar işlem tamamlandığında ya da en geç 30 gün içinde kalkar.

== Kredi kartı türleri <kart-turleri>
Raşit Bankası'nın bireysel kredi kartlarının hepsi uluslararası kart altyapısıyla çalışır, temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (#bkz(<rasitpuan>)). Kartlar yıllık üyelik ücretine, ücretsiz olma koşuluna ve sağladığı ayrıcalıklara göre ayrılır.

#tablo(("Kart", "Yıllık üyelik ücreti", "Ücretsiz olma koşulu", "RaşitPuan"),
  ("Raşit Classic", "650 TL", "Yıllık 60.000 TL harcama", "Harcamanın binde 3'ü"),
  ("Raşit Gold", S.gold-yillik-ucret, "Yıllık 150.000 TL harcama", "Harcamanın binde 5'i"),
  ("Raşit Platinum", "2.950 TL", "Yıllık 400.000 TL harcama", "Harcamanın binde 8'i"),
  if S.genc-kart { ("Raşit Genç", "Alınmaz", "Koşul yok", "Harcamanın binde 3'ü") },
)

#ara[Raşit Classic]
Günlük harcamalar için temel kredi kartıdır. İlk limit gelirinize göre 5.000 TL ile 150.000 TL arasında belirlenir. Raşit Classic sahipleri, Raşit Bankası'ndan maaş alıyorsa kartın ilk yılında yıllık üyelik ücreti ödemez.

#ara[Raşit Gold]
Sık seyahat edenler ve harcamalarının bir kısmını yurt dışında yapanlar için tasarlanmıştır.
- Yurt dışında yapılan alışverişlerde RaşitPuan oranı harcamanın binde 10'udur.
- Anlaşmalı havalimanı otoparklarında %20 indirim sağlanır.
- İlk limit en az 25.000 TL'dir. Başvuru için aylık net gelirin en az 40.000 TL olması gerekir.

#ara[Raşit Platinum]
Yüksek limit ve seyahat ayrıcalıkları sunan karttır.
- Yılda 6 kez yurt içi ve yurt dışı havalimanı yolcu salonlarına ücretsiz giriş hakkı verir. Hak, kart hesap yılının başında yenilenir; kullanılmayan haklar sonraki yıla devretmez.
- Yurt dışı seyahatlerinizde kartla ödediğiniz uçak biletine bağlı seyahat asistansı hizmeti ücretsizdir.
- İlk limit en az 100.000 TL'dir. Başvuru için aylık net gelirin en az 120.000 TL olması gerekir.

#if S.genc-kart [
  #ara[Raşit Genç]
  Raşit Genç, 18-26 yaş arasındaki üniversite öğrencileri için çıkarılan kredi kartıdır. Yıllık üyelik ücreti alınmaz ve ek kartı çıkarılmaz.
  - Kart limiti 5.000-20.000 TL arasında belirlenir. Düzenli geliri olmayan öğrencilerde limit 5.000 TL'dir.
  - Başvuruda e-Devlet'ten alınan öğrenci belgesi istenir. Öğrenciliğiniz her yıl ekim ayında e-Devlet üzerinden yeniden doğrulanır.
  - 26 yaşını doldurduğunuzda ya da öğrenciliğiniz sona erdiğinde kartınız, limitiniz korunarak Raşit Classic'e dönüştürülür; dönüşümden 30 gün önce size SMS ile bilgi verilir.
]

#ara[Kart değiştirmek]
Kartınızı başka bir Raşit Bankası kredi kartıyla değiştirmek için Raşit Mobil'de Kartlarım > Kart değiştir adımını kullanabilirsiniz. Yeni kartın limiti ve koşulları yeniden değerlendirilir. RaşitPuan'larınız ve taksitli borçlarınız yeni karta aktarılır. Ödenmiş yıllık üyelik ücretinin kalan tam aylara düşen kısmı iade edilir.

== Ek kart <ek-kart>
Eşiniz, anneniz, babanız, çocuklarınız ve kardeşleriniz için kredi kartınıza bağlı ek kart çıkarabilirsiniz.
- Ek kart sahibinin en az 15 yaşında olması gerekir.
- Bir ana karta en fazla 4 ek kart bağlanabilir.
- Raşit Classic ve Raşit Gold'a bağlı ek kartlar ücretsizdir. Raşit Platinum'a bağlı her ek kart için yıllık 750 TL alınır.
- Ek kartın harcamaları ana kartın limitinden kullanılır ve ana kartın ekstresinde ayrı bir başlık altında gösterilir.
- Ek kart için ana kart limitinin altında ayrı bir aylık harcama sınırı belirleyebilir, ek kartı istediğiniz an geçici olarak kapatabilirsiniz.
- Ek kartla yapılan bütün harcamaların ödemesinden ana kart sahibi sorumludur. Ana kart kapatıldığında ona bağlı ek kartlar da kapanır.

Ek kart başvurusu Raşit Mobil'de Kartlarım > Ek kart başvurusu ekranından ya da şubeden yapılır. Ek kart sahibinin kimlik bilgileri ve telefon numarası istenir; ek kart, ek kart sahibinin adresine gönderilir.

#ara[Ek kartın kapatılması]
Ek kartı ana kart sahibi istediği an Raşit Mobil'den kapatabilir. Ek kart sahibi de kendi kartını Müşteri Hizmetleri'nden kapattırabilir. Ek kart kapatıldığında ek kartla yapılmış harcamaların ödemesi ana kartın ekstresinde devam eder. 18 yaşından küçük ek kart sahiplerinin kartları, ana kart sahibinin onayı olmadan yeniden açılmaz.

== Yıllık üyelik ücreti, muafiyet ve iade <yillik-ucret>
Kredi kartlarının yıllık üyelik ücreti, kartın kullanıma açıldığı ayın her yıl dönümünde o ayın ekstresine yansıtılır. Ücretler #bkz(<kart-turleri>) tablosundadır. Ücret yansıtılmadan en az 30 gün önce size SMS ile bilgi verilir; bildirimde ücretin tutarı, yansıtılacağı ekstre ve ücretsiz olma koşulunu sağlamak için kalan harcama tutarı yazar.

#ara[Ücretsiz olma koşulu]
Ücretin tahsil edileceği tarihten önceki 12 ay içinde kartla yaptığınız alışverişlerin toplamı kartınızın ücretsiz olma koşulundaki tutara ulaşırsa o yıl için yıllık üyelik ücreti alınmaz.
- Toplama ek kartların alışverişleri de dahildir.
- Nakit avans, kart borcu ödemesi, kumar ve bahis işlemleri, iade edilen alışverişler ve RaşitPuan ile ödenen kısım toplama dahil edilmez.
- Harcama toplamınızı Raşit Mobil'de Kartlarım > kartı seçin > Yıllık ücret durumu ekranında görebilirsiniz.

#ara[Ücretin iadesi]
- Yıllık üyelik ücretinin yansıtıldığı tarihten sonraki 30 gün içinde kartınızı kapatırsanız ücretin tamamı iade edilir.
- 30 günden sonra kartınızı kapatırsanız ücretin kalan tam aylara düşen kısmı iade edilir. Örneğin ücret yansıdıktan 4 ay 10 gün sonra kapatılan kartta 7 aylık kısım iade edilir.
- İade, kart hesabınıza alacak olarak geçer. Kartta borç yoksa tutar vadesiz hesabınıza aktarılır.

#ara[Ücret ödemek istemiyorsanız]
Yıllık üyelik ücreti ödemek istemiyorsanız üç seçeneğiniz vardır: ücretsiz olma koşulundaki harcama tutarına ulaşmak, kartınızı daha düşük ücretli bir kartla değiştirmek (#bkz(<kart-turleri>)) ya da kartınızı kapatmak (#bkz(<kart-kapatma>)). Kart kapatıldıktan sonra kalan borcunuz ödeme planına göre ödenmeye devam eder.

== Kredi kartı başvurusu <kart-basvuru>
Kredi kartı başvurusunu Raşit Mobil'den, İnternet Şubesi'nden ya da şubeden yapabilirsiniz.

#ara[Başvuru şartları]
- 18 yaşını doldurmuş olmak,
- Düzenli gelir sahibi olmak#if S.genc-kart [ (Raşit Genç dışındaki kartlar için)],
- Kredi Kayıt Bürosu (KKB) kayıtlarında karşılıksız çek, kanuni takip ya da son 12 ayda 90 günü aşan gecikme bulunmamak.

#ara[Belgeler]
Başvuruda kimlik bilgileriniz ve gelir bilginiz istenir. Raşit Bankası'ndan maaş ya da emekli maaşı alıyorsanız gelir belgesi istenmez. Diğer başvurularda gelir belgesi gerekebilir; kabul edilen gelir belgeleri #bkz(<kredi-belgeler>) içinde listelenmiştir.

#ara[Sonuç ve teslim süresi]
- Başvuruların çoğu birkaç dakika içinde sonuçlanır. Ek belge istenen başvurular belgenin yüklendiği günden itibaren en geç 3 iş günü içinde sonuçlandırılır.
- Başvurunuz onaylanınca kartınız basılır ve kayıtlı adresinize kuryeyle gönderilir. İl merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde teslim edilir.
- Sanal kartınız onaydan hemen sonra Raşit Mobil'de kullanıma açılır.
- Başvuru olumsuz sonuçlanırsa size SMS ile bilgi verilir. Olumsuz sonuçlanan başvurudan sonra 3 ay içinde yapılan yeni başvurular, gelir bilginiz değişmediyse aynı şekilde değerlendirilir.

== Kart limiti: belirleme, artırma, düşürme, geçici limit <kart-limiti>
#ara[Limit nasıl belirlenir]
Kart limitiniz başvuru sırasında gelirinize, diğer bankalardaki kart ve kredi borçlarınıza ve KKB kayıtlarınıza göre belirlenir. Mevzuat gereği bir kişiye bütün bankalarda verilen kredi kartı limitlerinin toplamı, ilk yıl aylık gelirin 2 katını, sonraki yıllarda 4 katını geçemez. Bu nedenle başka bankalardaki kart limitleriniz yüksekse Raşit Bankası'ndaki limitiniz beklediğinizden düşük olabilir.

#ara[Limit artırma]
- Limit artırma talebini Raşit Mobil'de Kartlarım > Limit işlemleri ekranından verebilirsiniz.
- Talepler genellikle anında sonuçlanır. Güncel gelir bilginiz gerekiyorsa e-Devlet'ten SGK hizmet dökümü ya da son maaş bordronuzu yüklemeniz istenir.
- Son limit artırımından sonraki 6 ay içinde yeni artırım talebi değerlendirilmez.
- Raşit Bankası, ödemelerini düzenli yapan kart sahiplerine limit artırım teklifi gönderebilir. Teklif siz onaylamadan uygulanmaz.

#ara[Limit düşürme]
Limit düşürme talebi her zaman verilebilir ve hemen geçerli olur. Yeni limit, o anki kullanılmış tutarın altında olamaz.

#ara[Limitin banka tarafından değiştirilmesi]
Raşit Bankası; gelirinizin azaldığını, başka bankalardaki borçlarınızın arttığını ya da ödemelerinizde gecikme olduğunu tespit ederse kart limitinizi düşürebilir. Limit düşürülmeden en az 30 gün önce size SMS ile bildirilir. Düşürülen limit, o anki kullanılmış tutarın altına inmez.

#ara[Limit aşımı]
Kartınızla limitinizi aşan harcama yapılamaz; limit yetersizse işlem reddedilir. Faiz, ücret ve vergilerin yansıtılması nedeniyle limit aşılırsa aşan tutar, bir sonraki ekstrede asgari ödeme tutarına eklenir. Limit kullanımınız %80'e ulaştığında Raşit Mobil'den bildirim gönderilir.

#ara[Geçici limit artışı]
Tatil, düğün ya da sağlık harcaması gibi tek seferlik ihtiyaçlar için geçici limit artışı alabilirsiniz.
- Geçici artış en fazla mevcut limitin %30'u kadardır ve en fazla 30 gün geçerlidir.
- Bir takvim yılında en fazla 2 kez geçici limit artışı alınabilir.
- Süre sonunda limit eski hâline döner. Bu sürede limiti aşan harcamanız varsa aşan tutar bir sonraki ekstrede tek seferde ödenir.

== Hesap kesim tarihi, ekstre ve son ödeme tarihi <ekstre>
#ara[Hesap kesim tarihi]
Hesap kesim tarihi, kartınızın bir dönemdeki harcamalarının toplanıp ekstre olarak düzenlendiği gündür. Kesim tarihinizi başvuru sırasında ya da sonrasında Raşit Mobil'de ayın 1'i ile 28'i arasından seçebilirsiniz. Kesim tarihi bir takvim yılında 1 kez değiştirilebilir; değişiklik bir sonraki dönemden itibaren geçerli olur.

#ara[Son ödeme tarihi]
Son ödeme tarihi, hesap kesim tarihinden 10 gün sonradır. Son ödeme tarihi hafta sonuna ya da resmî tatile denk gelirse izleyen ilk iş gününe kayar ve bu nedenle faiz ya da gecikme ücreti uygulanmaz.

#ara[Ekstre]
- Ekstreniz kesim tarihinde Raşit Mobil'de ve İnternet Şubesi'nde yayımlanır; isterseniz e-posta ile de gönderilir. Dijital ekstre ücretsizdir.
- Kâğıt ekstre isteğe bağlıdır ve her gönderim için 4,90 TL posta ücreti alınır. Kâğıt ekstre tercihinizi Raşit Mobil'de Kartlarım > Ekstre tercihleri ekranından değiştirebilirsiniz.
- Geçmiş 24 aya ait ekstrelerinizi Raşit Mobil'den PDF olarak indirebilirsiniz.

#ara[Ekstrede neler yazar]
#tablo(("Ekstredeki bilgi", "Ne anlama gelir"),
  ("Dönem borcu", "Bu dönem ödemeniz gereken toplam tutar"),
  ("Asgari ödeme tutarı", "Gecikmeye düşmemek için ödenmesi gereken en düşük tutar"),
  ("Son ödeme tarihi", "Faiz işlememesi için dönem borcunun ödenmesi gereken son gün"),
  ("Gelecek dönem taksitleri", "Sonraki ekstrelere yansıyacak taksit tutarları"),
  ("Uygulanan faiz oranları", "Bu dönemde geçerli akdi faiz ve gecikme faizi oranları"),
  ("Kazanılan RaşitPuan", "Bu dönem kazandığınız ve silinecek olan puanlar"),
)
Ekstrenize itiraz etmek istediğiniz bir işlem varsa #bkz(<harcama-itirazi>) adımlarını izleyin. Ekstreniz kesim tarihinden sonraki 2 gün içinde yayımlanmadıysa Müşteri Hizmetleri'ni arayın.

== Asgari ödeme ve gecikme <asgari-odeme>
#ara[Asgari ödeme tutarı]
Asgari ödeme tutarı, dönem borcunun mevzuatta kart limitine göre belirlenen yüzdesidir ve ekstrede ayrıca yazar.

#tablo(("Kart limiti", "Asgari ödeme oranı"),
  ("50.000 TL'ye kadar", "Dönem borcunun %20'si"),
  ("50.000 TL'nin üzerinde", "Dönem borcunun %40'ı"),
  ("Kartın ilk yılında (limitten bağımsız)", "Dönem borcunun %40'ı"),
)

#ara[Faiz oranları]
#tablo(("Faiz türü", "Türk lirası işlemler (aylık)", "Yabancı para işlemler (aylık)"),
  ("Akdi faiz (alışveriş)", "%3,75", "%3,40"),
  ("Akdi faiz (nakit avans)", "%4,25", "%3,90"),
  ("Gecikme faizi", "%4,05", "%3,70"),
)
Oranlar Türkiye Cumhuriyet Merkez Bankası'nın belirlediği azami oranları aşmaz. Azami oranlar değiştiğinde Raşit Bankası'nın oranları en geç bir sonraki hesap kesim tarihinde güncellenir. Faize ayrıca KKDF ve BSMV eklenir.

#ara[Faiz nasıl işler]
- Dönem borcunun tamamını son ödeme tarihine kadar öderseniz alışverişlere faiz işlemez.
- Asgari tutarı ya da daha fazlasını ödeyip borcun tamamını ödemezseniz kalan borca, son ödeme tarihinden bir sonraki kesim tarihine kadar akdi faiz işler.
- Asgari tutarı ödemezseniz ödenmeyen asgari kısma gecikme faizi, borcun geri kalanına akdi faiz işler.

Örnek: Kart limitiniz 40.000 TL ve dönem borcunuz 18.000 TL olsun. Asgari ödeme oranı %20 olduğu için asgari ödeme tutarı 3.600 TL'dir. Son ödeme tarihinde 3.600 TL öderseniz kalan 14.400 TL'ye bir sonraki kesim tarihine kadar akdi faiz işler. Bu süre 30 gün ise faiz 14.400 TL × %3,75 = 540 TL, faize eklenen KKDF ve BSMV toplam 162 TL olur ve bir sonraki ekstreye yansır.

#ara[Dönem borcunu taksitlendirmek]
Dönem borcunuzu tek seferde ödeyemeyecekseniz son ödeme tarihinden önce Raşit Mobil'de Kartlarım > Ekstre taksitlendir ekranından borcun tamamını ya da bir kısmını 3 ile 12 ay arasında taksitlendirebilirsiniz.
- Ekstre taksitlendirmede aylık %3,75 faiz uygulanır ve faize KKDF ve BSMV eklenir. Taksit planı onayınızdan önce gösterilir.
- Taksitlendirilen tutar asgari ödeme hesabından çıkarılır; ilk taksit bir sonraki ekstreye yansır.
- Bir takvim yılında en fazla 3 kez ekstre taksitlendirme yapılabilir. Gecikmedeki borçlar taksitlendirilemez; bu durumda #bkz(<yapilandirma>).

#ara[Asgari tutarı ödemezseniz]
- Gecikme, Kredi Kayıt Bürosu'na bildirilir ve kredi notunuzu düşürür.
- Bir takvim yılında asgari tutarı art arda 3 kez ödemezseniz kartınız nakit avans kullanımına ve limit artırımına kapatılır. Alışverişe devam edebilirsiniz; dönem borcunun tamamını ödediğinizde kısıtlama kalkar.
- Son ödeme tarihinden itibaren 90 gün içinde asgari tutar ödenmezse kart kullanıma kapatılır ve borcun tamamı için yasal takip süreci başlatılabilir (#bkz(<gecikme>)).

#ara[Kart borcunu ödeme yolları]
Kart borcunuzu Raşit Mobil'den, İnternet Şubesi'nden, Raşit Bankası ATM'lerinden (kartla ya da kartsız), şubelerden ve başka bankalardan kart numaranıza EFT ya da FAST ile ödeyebilirsiniz. Vadesiz hesabınızdan otomatik ödeme talimatı da verebilirsiniz: talimatla son ödeme tarihinde, seçiminize göre dönem borcunun tamamı ya da asgari tutar hesabınızdan alınır. Son ödeme tarihinden sonra yapılan ödemeler önce gecikme faizine ve vergilerine, sonra akdi faize, en son anaparaya sayılır.

== Taksitli alışveriş <taksit>
Taksit yapan üye işyerlerinde alışverişinizi kredi kartınızla taksitlendirebilirsiniz. Taksitli alışverişin toplam tutarı işlem anında limitinizden düşer; her ay ödediğiniz taksit kadar limitiniz yeniden açılır.

#ara[Ürün gruplarına göre en fazla taksit sayısı]
Taksit sayısı, mevzuatın ürün grubuna göre belirlediği üst sınırı aşamaz.

#tablo(("Ürün ya da hizmet grubu", "En fazla taksit"),
  ("Beyaz eşya, mobilya, eğitim ve sağlık harcamaları", "12 taksit"),
  ("Bilgisayar, televizyon ve diğer elektronik ürünler", "6 taksit"),
  ("Cep telefonu", "3 taksit"),
  ("Havayolu, otel ve tur harcamaları", "6 taksit"),
  ("Giyim, ayakkabı ve kozmetik", "4 taksit"),
  ("Kuyum, altın, akaryakıt, gıda ve yemek", "Taksit yapılmaz"),
  ("Yurt dışı ve yurt dışı internet alışverişleri", "Taksit yapılmaz"),
)

#ara[Alışverişi sonradan taksitlendirmek]
Peşin yaptığınız 1.500 TL ve üzerindeki alışverişleri, hesap kesim tarihinden önce Raşit Mobil'de Kartlarım > Harcamalar ekranından 3, 6 ya da 9 taksite bölebilirsiniz.
- Sonradan taksitlendirmede aylık %3,49 faiz uygulanır; taksit planı onayınızdan önce ekranda gösterilir.
- Taksit yapılamayan ürün gruplarındaki alışverişler sonradan da taksitlendirilemez. Taksit sayısı, ürün grubunun üst sınırını aşamaz.
- Taksitlendirilmiş bir alışverişin kalan taksitlerini istediğiniz an tek seferde kapatabilirsiniz; kalan taksitlerin faizi alınmaz.

#ara[Taksitli alışverişte limit ve iade]
- Örneğin limiti 30.000 TL olan kartla 12.000 TL'lik bir buzdolabını 12 taksitle aldığınızda 12.000 TL'nin tamamı hemen limitinizden düşer. Her ay ödediğiniz 1.000 TL'lik taksitle limitinizin 1.000 TL'si yeniden kullanıma açılır.
- Taksitli alışverişin iadesinde iade tutarı kalan taksitlerden düşülür; ödenmiş taksitler kart hesabınıza alacak olarak geçer.
- Taksitli alışverişlerde taksitler faizsizdir; vade farkı uygulanıp uygulanmayacağını işyeri belirler ve satış sırasında size bildirir.

== Nakit avans <nakit-avans>
Kredi kartınızla Raşit Bankası ATM'lerinden, başka banka ATM'lerinden ya da Raşit Mobil'den vadesiz hesabınıza nakit avans çekebilirsiniz.

- Nakit avans limiti, kart limitinizin %50'sidir.
- Nakit avans işlem ücreti çekilen tutarın %1,25'i, en az 15,00 TL'dir. Başka banka ATM'lerinden çekimde ayrıca 21,75 TL alınır.
- Nakit avansa çekim gününden itibaren nakit avans akdi faizi işler (#bkz(<asgari-odeme>)). Dönem borcunun tamamı son ödeme tarihinde ödense bile bu faiz alınır.
- Kredi kartından vadesiz hesaba yapılan para aktarımı ve kart üzerinden yapılan kumar, bahis ve döviz alımı nakit avans sayılır.

Örnek: Kredi kartınızdan 10.000 TL nakit avans çekip 30 gün sonra ödediğinizde 125 TL işlem ücreti, 10.000 TL × %4,25 = 425 TL faiz ve faize eklenen 127,50 TL KKDF ve BSMV ödersiniz.

#ara[Taksitli nakit avans]
Raşit Mobil'den 3 ile 12 ay arasında vadeli taksitli nakit avans kullanabilirsiniz. Taksitli nakit avansın aylık faizi %4,25'tir ve ödeme planı kullanımdan önce gösterilir. Taksitli nakit avansı vadesinden önce kapatırsanız kalan dönemlerin faizi alınmaz ve erken kapama ücreti uygulanmaz.

== RaşitPuan <rasitpuan>
Kredi kartınızla yaptığınız her alışverişte RaşitPuan kazanırsınız. 1 RaşitPuan 1 TL değerindedir. Kazanma oranları kart türüne göre #bkz(<kart-turleri>) tablosunda yer alır.

- *Puan kazandırmayan işlemler:* nakit avans, kart borcu ödemesi, kumar ve bahis, akaryakıt alışverişleri, vergi ve kamu ödemeleri.
- *Kullanma:* Puanlarınızı üye işyerlerinde ödeme sırasında ya da Raşit Mobil'den kart borcunuza aktararak kullanabilirsiniz. En az kullanım 10 RaşitPuan'dır.
- *Geçerlilik:* Kazanılan puanlar, kazanıldığı yılı izleyen 2. takvim yılının sonuna kadar geçerlidir. Örneğin 2026'da kazanılan puanlar 31 Aralık 2028'de silinir.
- *Kart kapatılınca:* Kullanılmayan puanlar silinir. Kartınızı kapatmadan önce puanlarınızı borcunuza aktarmanızı öneririz.
- Puanlar başka bir kişinin kartına aktarılamaz. Ek kartların kazandığı puanlar ana karta yazılır.
- Kampanyalarda kazanılan ek puanların geçerlilik süresi kampanya koşullarında ayrıca yazar.

Örnek: Raşit Gold ile Türkiye'de 20.000 TL ve yurt dışında 5.000 TL alışveriş yaptığınız bir dönemde 20.000 TL × binde 5 = 100 RaşitPuan ve 5.000 TL × binde 10 = 50 RaşitPuan, toplam 150 RaşitPuan kazanırsınız. Puanlar, alışverişin ekstrenize yansıdığı gün hesabınıza yazılır; iade edilen alışverişlerin puanları geri alınır.

#ara[Kampanyalar]
- Kart kampanyaları Raşit Mobil'de Kampanyalar ekranında yayımlanır. Katılım gerektiren kampanyalara bu ekrandaki "Katıl" düğmesiyle katılırsınız; katılımdan önce yapılan harcamalar kampanyaya dahil edilmez.
- Kampanyada kazanılan RaşitPuan'lar, kampanya koşullarında yazan tarihte, genellikle kampanya bitiminden sonraki 30 gün içinde hesabınıza yazılır.
- Kampanya koşullarını sağlayan harcamalardan biri iade edilirse o harcamaya bağlı kampanya puanı geri alınır.
- Ek kartların harcamaları, kampanya koşullarında aksi yazmadıkça ana kartın kampanya harcamasına dahil edilir.

#ara[Puanı borca aktarmak]
Raşit Mobil'de Kartlarım > RaşitPuan > Borca aktar ekranından puanlarınızı dönem borcunuza ya da güncel borcunuza aktarabilirsiniz. Aktarılan puan, aktarım günü borcunuzdan düşer. Asgari ödeme tutarını puanla ödeyebilirsiniz; ancak puanla yapılan ödeme, otomatik ödeme talimatının yerine geçmez.

== Sanal kart ve internetten alışveriş <sanal-kart>
Sanal kart, internet alışverişleri için kullanılan ve ana kartınızdan farklı numarası olan karttır.
- Raşit Mobil'de Kartlarım > Sanal kart ekranından ücretsiz ve anında oluşturulur. Hem kredi kartınıza hem banka kartınıza bağlı sanal kart oluşturabilirsiniz.
- Sanal kartın limitini, ana kart limitinizi aşmayacak şekilde siz belirlersiniz ve istediğiniz an değiştirebilirsiniz.
- Sanal kartın güvenlik kodu (CVV) Raşit Mobil'de kart her görüntülendiğinde yeniden üretilir.
- Sanal kart mağazada fiziki ödemede ve ATM'de kullanılamaz.
- Sanal kartı istediğiniz an silip yeni numarayla yeniden oluşturabilirsiniz. Bir ay içinde en fazla 5 kez yeni sanal kart oluşturulabilir.
- Sanal kart harcamaları ana kartın ekstresinde ayrı bir başlık altında gösterilir ve ana kartın sağladığı RaşitPuan oranıyla puan kazandırır.

#ara[Kartın internet alışverişine açılması]
Yeni kartlar internet alışverişine ve yurt dışı kullanıma kapalı olarak gönderilir. Yeni kartınızla internetten alışveriş yapamıyorsanız Raşit Mobil'de kart ayarlarından "İnternet alışverişi" seçeneğini açmanız gerekir. Seçenek açıldığı anda geçerli olur. İnternet alışverişlerinde güvenli ödeme doğrulaması yapılır (#bkz(<kart-sifresi>)).

#ara[İnternet alışverişinde güvenlik]
- Kart bilgilerinizi yalnızca adres çubuğunda doğru alan adı görünen sitelere girin; e-posta ya da SMS ile gelen bağlantılardan ödeme sayfasına gitmeyin.
- Güvenli ödeme bildiriminde işyeri adını ve tutarı kontrol etmeden onay vermeyin.
- Sık alışveriş yapmadığınız sitelerde ana kartınız yerine düşük limitli bir sanal kart kullanın.
- İnternet alışverişi kullanmadığınızda kart ayarlarından kapalı tutabilirsiniz; açma ve kapatma hemen geçerli olur.

#ara[Abonelikler ve kayıtlı kartlar]
İnternet sitelerine kaydettiğiniz kartlarla yapılan düzenli abonelik ödemelerini Raşit Mobil'de kart detayındaki "Abonelikler" listesinde görebilirsiniz. Aboneliği iptal etmek için önce hizmeti aldığınız siteye başvurmanız gerekir; iptal edilmiş bir aboneliğe rağmen ödeme alınırsa harcama itirazı başlatabilirsiniz (#bkz(<harcama-itirazi>)).

== Temassız ödeme <temassiz>
Raşit Bankası kartlarının hepsi temassız ödeme özelliğine sahiptir.
- Banka kartı ve kredi kartıyla 1.500 TL'ye kadar olan temassız ödemelerde şifre sorulmaz. 1.500 TL'nin üzerindeki temassız ödemelerde kart şifresi girilir.
- Güvenlik nedeniyle aynı gün içinde art arda yapılan şifresiz temassız ödemelerin toplamı 5.000 TL'yi aşarsa bir sonraki işlemde şifre istenir.
- Temassız özelliğini ve şifresiz işlem tutarını Raşit Mobil'de kart ayarlarından kapatabilir ya da daha düşük bir tutara indirebilirsiniz.
- Kartınızı Raşit Mobil'e tanımlayarak telefonunuzla da temassız ödeme yapabilirsiniz. Telefonla ödemede tutardan bağımsız olarak telefonunuzun ekran kilidi doğrulaması istenir.

== Yurt dışında kullanım ve kur <yurt-disi-kart>
Kartlarınız yurt dışında, kartınızın üzerindeki uluslararası kart logosunun bulunduğu her yerde kullanılabilir. Yurt dışı kullanım yeni kartlarda kapalıdır; seyahatten önce Raşit Mobil'de kart ayarlarından "Yurt dışı kullanım" seçeneğini açmanız gerekir.

#ara[Kur ve ücretler]
- *Banka kartı:* Yurt dışı harcama ve nakit çekim tutarı, işlemin hesabınıza yansıdığı günün Raşit Bankası döviz satış kuruyla Türk lirasına çevrilir.
- *Kredi kartı:* Yurt dışı harcamalar ekstrenizde işlemin yapıldığı para biriminin karşılığı olarak ABD doları ya da euro cinsinden gösterilir. Bu borcu Türk lirasıyla öderseniz ödeme günündeki Raşit Bankası döviz satış kuru uygulanır.
- Yurt dışında yapılan her kart işleminde işlem tutarının %1,99'u kadar yurt dışı işlem ücreti alınır.
- Yurt dışındaki ATM'lerden nakit çekimde ayrıca işlem tutarının %1,5'i, en az 75 TL ücret alınır. ATM'nin bulunduğu banka ayrıca ücret alabilir.

Örnek: Banka kartınızla yurt dışında 100 euro'luk bir alışveriş yaptığınızda, işlemin hesabınıza yansıdığı gün euro satış kuru 40,00 TL ise hesabınızdan 4.000 TL ve 79,60 TL yurt dışı işlem ücreti düşer.

#ara[Seyahatten önce]
- Kartınızın yurt dışı kullanımını açın ve son kullanma tarihini kontrol edin.
- Banka kartınızın yurt dışı günlük limitlerini ihtiyacınıza göre ayarlayın (#bkz(<banka-karti>)).
- Kartınız yurt dışında kaybolursa ya da çalınırsa Raşit Mobil'den kapatın ya da Müşteri Hizmetleri'nin yurt dışı numarasını arayın (#bkz(<musteri-hizmetleri>)). Yurt dışına yeni kart gönderilmez; yeni kartınız Türkiye'deki kayıtlı adresinize gönderilir.

#bilgi[*Öneri:* Yurt dışında ödeme sırasında işlemi Türk lirasıyla yapmak isteyip istemediğiniz sorulursa yerel para birimini seçin. Türk lirası seçildiğinde dönüşümü yabancı banka kendi kuruyla yapar ve bu kur genellikle daha yüksektir.]

== Kart şifresi ve güvenli ödeme (3D Secure) <kart-sifresi>
#ara[Kart şifresi]
- Kartınızın 4 haneli şifresini kart teslim edildikten sonra Raşit Mobil'den, Raşit Bankası ATM'lerinden ya da Müşteri Hizmetleri'nden belirleyebilirsiniz.
- Doğum yılınız, art arda gelen (1234) ya da tekrar eden (1111) rakamlar şifre olarak kabul edilmez.
- Şifrenizi 3 kez hatalı girerseniz kartınız şifreli işlemlere kapanır. Kart, temassız şifresiz ödemelere ve internet alışverişine açık kalır.
- Şifrenizi unuttuysanız ya da kartınız şifreli işlemlere kapandıysa Raşit Mobil'de Kartlarım > Kart şifremi unuttum adımıyla yeni şifre belirleyebilirsiniz; kart hemen şifreli işlemlere açılır.

#ara[Kart şifresini değiştirmek]
Kart şifrenizi Raşit Mobil'de Kartlarım > kartı seçin > Kart şifresi ekranından ya da Raşit Bankası ATM'lerinden istediğiniz zaman değiştirebilirsiniz. Yeni şifre hemen geçerli olur. Banka kartınızın ve kredi kartınızın şifreleri birbirinden ayrıdır; birini değiştirmeniz diğerini etkilemez.

#ara[Güvenli ödeme (3D Secure)]
İnternet alışverişlerinde güvenli ödeme doğrulaması yapılır. Doğrulama Raşit Mobil bildirimiyle ya da SMS ile gelen tek kullanımlık kodla yapılır.
- Doğrulama kodu 3 dakika geçerlidir ve yalnızca kodu istediğiniz işlem için kullanılabilir.
- Bildirimde işyerinin adı ve tutar yazar. Size ait olmayan bir işlemin bildirimini onaylamayın ve hemen kartınızı geçici olarak kapatın (#bkz(<kayip-calinti>)).
- Raşit Bankası çalışanları kart şifrenizi ya da doğrulama kodunu hiçbir zaman sormaz (#bkz(<dolandiricilik>)).

== Kayıp ya da çalıntı bildirimi <kayip-calinti>
Kartınızı kaybettiyseniz ya da kartınız çalındıysa vakit kaybetmeden bildirin. Bildirimden sonra kartınızla yapılan işlemlerden sorumlu olmazsınız. Bildirimden önceki 24 saat içinde onayınız olmadan yapılan işlemlerde sorumluluğunuz 250 TL ile sınırlıdır.

+ Kartınızı aşağıdaki kanallardan biriyle kapatın:
  #tablo(("Kanal", "Ne zaman", "Nasıl"),
    ("Raşit Mobil", "7/24", "Kartlarım > kartı seçin > Kayıp/çalıntı bildir"),
    ("İnternet Şubesi", "7/24", "Kartlar > Kart işlemleri > Kayıp/çalıntı bildir"),
    ("Müşteri Hizmetleri", "7/24", [Kartlar menüsü > Kayıp/çalıntı (#bkz(<musteri-hizmetleri>))]),
    ("Şube", "Şube çalışma saatlerinde", "Kimliğinizle başvurun"),
  )
+ Kayıp ya da çalıntı bildirimiyle kartınız kalıcı olarak kapatılır; bu işlem geri alınamaz. Kartınızı yalnızca nereye koyduğunuzu bilmiyorsanız Raşit Mobil'deki "Geçici kapat" seçeneğini kullanın. Kart siz yeniden açana kadar kapalı kalır. Bulamazsanız kayıp bildirimi yapın.
+ Yeni kartınız kendiliğinden basılır ve kayıtlı adresinize kuryeyle gönderilir. İl merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde teslim edilir. Teslimde kimliğinizi göstermeniz gerekir.
+ Yeni kartın numarası, son kullanma tarihi ve güvenlik kodu değişir. Bankamızdaki düzenli ödeme talimatları yeni karta kendiliğinden aktarılır. İnternet sitelerinde kayıtlı kart ve abonelik bilgilerini siz güncellersiniz.
+ Bir takvim yılındaki ilk kayıp ya da çalıntı bildiriminde yeni kart ücretsizdir. Aynı yıldaki sonraki bildirimlerde kart yenileme ücreti 100 TL'dir (#bkz(<tarife-kart>)).
+ Bildirimden önce tanımadığınız bir harcama varsa harcama itirazı başlatın (#bkz(<harcama-itirazi>)). Kartınız çalındıysa emniyete de bildirmenizi öneririz.

Yeni kartınız gelene kadar Raşit Mobil'de sanal kart oluşturarak internet alışverişlerinize devam edebilirsiniz (#bkz(<sanal-kart>)). Yurt dışındaysanız Müşteri Hizmetleri'nin yurt dışı numarasını arayabilirsiniz (#bkz(<musteri-hizmetleri>)).

== Harcama itirazı <harcama-itirazi>
Ekstrenizde tanımadığınız, tutarı hatalı ya da ürünü veya hizmeti size ulaşmamış bir işlem görürseniz itiraz edebilirsiniz.

#ara[İtiraz nasıl yapılır]
- Raşit Mobil'de işlemi seçip "Bu işleme itiraz et" adımını kullanın ya da Müşteri Hizmetleri'ni arayın.
- İtiraz, işlemin ekstrenize yansıdığı tarihten itibaren en geç 120 gün içinde yapılmalıdır.
- İtiraz nedeninize göre fatura, iade belgesi ya da işyeriyle yazışmalarınız gibi belgeler istenebilir.
- Tanımadığınız işlemlerde, kartınız başkasının elinde olabileceği için itirazla birlikte kartınız kapatılır ve yeni kart gönderilir.

#ara[İtiraz nedenleri ve istenen belgeler]
#tablo(("Durum", "İstenebilecek belge"),
  ("Tanımadığınız işlem", "Belge gerekmez; kart kapatılır ve yeni kart gönderilir"),
  ("Aynı işlemin iki kez çekilmesi", "Satış fişi ya da sipariş onayı"),
  ("Tutarı hatalı işlem", "Satış fişi ya da fatura"),
  ("Teslim edilmeyen ürün ya da hizmet", "Sipariş belgesi ve işyeriyle yazışmalar"),
  ("İade edildiği hâlde yansımayan tutar", "İade belgesi ya da kargo teslim kaydı"),
  ("İptal edildiği hâlde ödenen abonelik", "İptal onayı ya da iptal tarihini gösteren belge"),
)

#ara[İtiraz süreci]
- Raşit Bankası, işlemi yapan işyerinin bankasından uluslararası kurallar çerçevesinde belge ister.
- Süreç ortalama 45 gün, yurt dışı işlemlerde 90 güne kadar sürebilir. Sonuç size SMS ve Raşit Mobil bildirimiyle iletilir.
- İtiraz incelenirken itiraz edilen tutar için ödeme yapmanız ve bu tutara faiz ödemeniz gerekmez.
- İtiraz lehinize sonuçlanırsa tutar kartınıza alacak olarak geçer. Aleyhinize sonuçlanırsa tutar ve işlemiş faizi bir sonraki ekstrenize yansıtılır.

#ara[Şüpheli işlem kontrolü]
Raşit Bankası, olağan dışı görünen kart işlemlerini otomatik olarak inceler ve işlemi geçici olarak durdurabilir. Bu durumda size SMS ya da arama ile ulaşılır ve işlemi sizin yapıp yapmadığınız sorulur. İşlemi onaylarsanız aynı işlemi yeniden deneyebilirsiniz; onaylamazsanız kartınız kapatılır ve yeni kart gönderilir. Bu aramalarda kart şifreniz, kartın tam numarası, güvenlik kodu ya da doğrulama kodu hiçbir zaman istenmez.

Abonelik iptali, ürün iadesi ve fiyat anlaşmazlığı gibi konularda itirazdan önce işyeriyle görüşmenizi öneririz. Kartınız ya da bilgileriniz dolandırıcılıkla ele geçirildiyse #bkz(<dolandiricilik>) adımlarını da izleyin.

== Kartın yenilenmesi ve teslim süresi <kart-yenileme>
- *Süresi dolan kartlar:* Kartınız son kullanma tarihinden önceki ayın ilk haftasında kendiliğinden yenilenir ve kayıtlı adresinize gönderilir. Yenileme ücretsizdir. Kart numaranız aynı kalabilir; son kullanma tarihi ve güvenlik kodu değişir.
- *Yıpranan ya da çalışmayan kartlar:* Raşit Mobil'de Kartlarım > Yeni kart talebi ekranından ücretsiz yeni kart isteyebilirsiniz.
- *Teslim süresi:* Bütün kartlar kuryeyle, il merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde teslim edilir. Kart yalnızca kart sahibine kimlik gösterilerek teslim edilir.
- *Teslim edilemeyen kartlar:* Kurye 3 kez denemesine rağmen teslim edemezse kart size en yakın şubeye gönderilir ve SMS ile bilgi verilir. Şubeden 30 gün içinde alınmayan kart güvenlik nedeniyle imha edilir.
- *Adres değişikliği:* Kartın gönderileceği adresi, kart basılmadan önce Raşit Mobil'den güncelleyebilirsiniz (#bkz(<iletisim-guncelleme>)).

Yeni kartınızı kullanmadan önce Raşit Mobil'de kart ekranındaki "Kartı kullanıma aç" adımını tamamlayın. Eski kartınızı çip bölümünden keserek kullanılamaz hâle getirin.

== Kartın kapatılması <kart-kapatma>
Kredi kartınızı Raşit Mobil'de Kartlarım > kartı seçin > Kartı kapat adımıyla, Müşteri Hizmetleri'nden ya da şubeden kapatabilirsiniz. Kart kapatma ücretsizdir.

- Kart hemen kullanıma kapanır; ona bağlı ek kartlar ve sanal kartlar da kapanır.
- Kalan borcunuz, taksitli alışverişleriniz dahil, ödeme planına göre ödenmeye devam eder. Kart hesabınız, bütün borç ödendikten sonra kapanır.
- Kullanılmayan RaşitPuan'lar silinir.
- Kartınıza bağlı otomatik fatura ödeme talimatları iptal edilir; faturalarınızı başka bir yolla ödemeniz gerekir (#bkz(<duzenli-odeme>)).
- Kapatılan kart KKB kayıtlarında kapalı olarak görünür.

#ara[Banka kartının kapatılması]
Banka kartınızı Raşit Mobil'den ya da şubeden kapatabilirsiniz; bağlı olduğu vadesiz hesap açık kalır. Bağlı olduğu vadesiz hesap kapatıldığında banka kartı da kendiliğinden kapanır. Kapatılan banka kartına bağlı sanal banka kartı ve "Küsurat biriktir" özelliği de kapanır (#bkz(<kumbara>)).

#sss()
#soru[Raşit Gold kartın yıllık üyelik ücreti ne kadar?][Raşit Gold'un yıllık üyelik ücreti #S.gold-yillik-ucret'dir. Önceki 12 ayda kartla toplam 150.000 TL alışveriş yaptıysanız ücret alınmaz (#bkz(<yillik-ucret>)).]
#soru[Son ödeme tarihim hafta sonuna denk gelirse ne olur?][Son ödeme tarihi izleyen ilk iş gününe kayar. O güne kadar yaptığınız ödemeler için gecikme faizi uygulanmaz (#bkz(<ekstre>)).]
#soru[Kartımı nereye koyduğumu bilmiyorum. İptal etmeden kapatabilir miyim?][Evet. Raşit Mobil'deki "Geçici kapat" seçeneğiyle kartınızı kapatabilir, bulduğunuzda aynı ekrandan yeniden açabilirsiniz. Kart numaranız değişmez (#bkz(<kayip-calinti>)).]
#soru[Kartımın borcunu başka bir bankadan ödeyebilir miyim?][Evet. Başka bir bankadan kart numaranıza EFT ya da FAST ile ödeme yapabilirsiniz. Ödeme, gönderildiği anda kartınıza yansır (#bkz(<asgari-odeme>)).]
#soru[Kart şifremi 3 kez yanlış girdim. Kartım kilitlendi mi?][Kartınız şifreli işlemlere kapanır. Raşit Mobil'de "Kart şifremi unuttum" adımıyla yeni şifre belirlediğinizde kart hemen yeniden açılır (#bkz(<kart-sifresi>)).]
#soru[Harcama itirazı ne kadar sürüyor?][Süreç ortalama 45 gün, yurt dışı işlemlerde 90 güne kadar sürebilir. Bu sürede itiraz edilen tutarı ödemeniz gerekmez (#bkz(<harcama-itirazi>)).]
#soru[Kart aidatından muaf olmak için ne kadar harcamam gerekiyor?][Önceki 12 aydaki alışveriş toplamınız Raşit Classic'te 60.000 TL, Raşit Gold'da 150.000 TL, Raşit Platinum'da 400.000 TL'ye ulaşırsa o yıl yıllık üyelik ücreti alınmaz (#bkz(<yillik-ucret>)).]
#soru[Aidat ödendikten sonra kartı kapatırsam param geri gelir mi?][Ücretin yansıdığı tarihten sonraki 30 gün içinde kapatırsanız tamamı, daha sonra kapatırsanız kalan tam aylara düşen kısmı iade edilir (#bkz(<yillik-ucret>)).]
#soru[Tatile gidiyorum. Limitimi geçici olarak yükseltebilir miyim?][Evet. Limitinizin %30'una kadar, en fazla 30 gün geçerli geçici limit artışı alabilirsiniz. Bir yılda en fazla 2 kez kullanılabilir (#bkz(<kart-limiti>)).]
#soru[Kart limitim neden düşük belirlendi?][Limit gelirinize, başka bankalardaki borçlarınıza ve KKB kayıtlarınıza göre belirlenir. Bütün bankalardaki kart limitlerinizin toplamı ilk yıl aylık gelirinizin 2 katını geçemez (#bkz(<kart-limiti>)).]
#soru[Kredi kartı hesap kesim tarihimi değiştirebilir miyim?][Evet. Raşit Mobil'den ayın 1'i ile 28'i arasında yeni bir tarih seçebilirsiniz; değişiklik yılda 1 kez yapılabilir (#bkz(<ekstre>)).]
#soru[Asgari tutarı üst üste ödemezsem ne olur?][Bir takvim yılında art arda 3 kez ödemezseniz kartınız nakit avans kullanımına ve limit artırımına kapatılır. Gecikme ayrıca KKB'ye bildirilir (#bkz(<asgari-odeme>)).]
#soru[Kâğıt ekstre ücretli mi?][Evet. Her gönderim için 4,90 TL posta ücreti alınır. Dijital ekstre ücretsizdir (#bkz(<ekstre>)).]
#soru[Nakit avans faizi ne kadar?][Türk lirası nakit avansta aylık %4,25'tir ve çekim gününden itibaren işler. Ayrıca çekilen tutarın %1,25'i, en az 15,00 TL işlem ücreti alınır (#bkz(<nakit-avans>)).]
#soru[Yurt dışında banka kartımı kullanabilir miyim?][Evet. Önce Raşit Mobil'de kart ayarlarından yurt dışı kullanımı açmanız gerekir. İşlemlerde %1,99 yurt dışı işlem ücreti alınır (#bkz(<yurt-disi-kart>)).]
#soru[Eşime ek kart çıkarmak ücretli mi?][Raşit Classic ve Raşit Gold'a bağlı ek kartlar ücretsizdir. Raşit Platinum'a bağlı her ek kart için yıllık 750 TL alınır (#bkz(<ek-kart>)).]
#soru[Temassız ödemede kaç liraya kadar şifre sorulmuyor?][1.500 TL'ye kadar olan temassız ödemelerde şifre sorulmaz. Aynı gün art arda yapılan şifresiz ödemeler 5.000 TL'yi aşarsa bir sonraki işlemde şifre istenir (#bkz(<temassiz>)).]
#soru[Peşin aldığım ürünü sonradan taksitlendirebilir miyim?][1.500 TL ve üzerindeki peşin alışverişleri, hesap kesim tarihinden önce Raşit Mobil'den 3, 6 ya da 9 taksite bölebilirsiniz. Aylık %3,49 faiz uygulanır (#bkz(<taksit>)).]
#soru[RaşitPuan'larım ne zaman siliniyor?][Puanlar, kazanıldığı yılı izleyen 2. takvim yılının sonunda silinir. Kart kapatılınca kullanılmayan puanlar hemen silinir (#bkz(<rasitpuan>)).]
#soru[Akaryakıt alırken puan kazanır mıyım?][Hayır. Akaryakıt alışverişleri RaşitPuan kazandırmaz (#bkz(<rasitpuan>)).]
#soru[Kaybolan kartımın yerine yenisi kaç günde gelir?][Yeni kart kendiliğinden basılır ve il merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde kuryeyle teslim edilir (#bkz(<kayip-calinti>)).]
#soru[Yeni gelen kartımla internetten alışveriş yapamıyorum. Neden?][Yeni kartlar internet alışverişine kapalı gönderilir. Raşit Mobil'de kart ayarlarından "İnternet alışverişi" seçeneğini açmanız gerekir (#bkz(<sanal-kart>)).]
