#import "../ortak.typ": *

= Ücret tarifesi <bolum-tarife>
Bu bölümde bireysel müşterilerden alınan ücretler ürün gruplarına göre toplanmıştır. Ücretlere BSMV dahildir. Ücretlerin koşulları ilgili bölümlerde anlatılır; tablolardaki atıflar bu bölümleri gösterir. Faiz oranları ücret değildir; kredi kartı faizleri #bkz(<asgari-odeme>), kredi faizleri #bkz(<bolum-krediler>), mevduat faizleri #bkz(<vadeli>) içinde yer alır.

== Kart ücretleri <tarife-kart>
#tablo(("Hizmet", "Ücret"),
  ("Banka kartı yıllık ücreti", "Alınmaz"),
  ("Raşit Classic yıllık üyelik ücreti", "650 TL"),
  ("Raşit Gold yıllık üyelik ücreti", S.gold-yillik-ucret),
  ("Raşit Platinum yıllık üyelik ücreti", "2.950 TL"),
  if S.genc-kart { ("Raşit Genç yıllık üyelik ücreti", "Alınmaz") },
  ("Raşit Classic ve Raşit Gold ek kart ücreti", "Alınmaz"),
  ("Raşit Platinum ek kart yıllık ücreti", "750 TL"),
  ("Kayıp ya da çalıntı kartın yenilenmesi, yılın ilk bildirimi", "Ücretsiz"),
  ("Kayıp ya da çalıntı kartın yenilenmesi, aynı yıldaki sonraki bildirimler", "100 TL"),
  ("Süresi dolan, yıpranan ya da ATM'de kalan kartın yenilenmesi", "Ücretsiz"),
  ("Sanal kart", "Ücretsiz"),
  ("Dijital ekstre", "Ücretsiz"),
  ("Kâğıt ekstre (gönderim başına)", "4,90 TL"),
  ("Nakit avans işlem ücreti", "Tutarın %1,25'i, en az 15,00 TL"),
  ("Başka banka ATM'sinden nakit avans (ek ücret)", "21,75 TL"),
  ("Yurt dışı kart işlemi", "Tutarın %1,99'u"),
  ("Yurt dışı ATM'den nakit çekme (ek ücret)", "Tutarın %1,5'i, en az 75 TL"),
)
Koşullar: yıllık üyelik ücreti ve muafiyet #bkz(<yillik-ucret>); kayıp ve çalıntı #bkz(<kayip-calinti>); nakit avans #bkz(<nakit-avans>); yurt dışı kullanım #bkz(<yurt-disi-kart>).

== Hesap ücretleri <tarife-hesap>
#tablo(("Hizmet", "Ücret"),
  ("Vadesiz hesap işletim ücreti", "Alınmaz"),
  ("Hesap açma ve kapatma (bütün hesap türleri)", "Ücretsiz"),
  ("Dijital hesap belgesi ve IBAN belgesi", "Ücretsiz"),
  ("Şubeden kâğıt hesap belgesi (belge başına)", "25 TL"),
  ("Kâğıt hesap cüzdanı", "25 TL"),
  ("Efektif döviz yatırma ve çekme", "Ücretsiz"),
  ("Müşteri ilişkisini sonlandırma", "Ücretsiz"),
)
Koşullar: #bkz(<isletim-ucreti>) ve #bkz(<hesap-kapatma>).

== Kredi ücretleri <tarife-kredi>
#tablo(("Hizmet", "Ücret"),
  ("Kredi tahsis ücreti (ihtiyaç, taşıt, konut)", "Kredi tutarının binde 5'i"),
  ("Konut kredisi ekspertiz ücreti", "9.850 TL"),
  ("Taşıt rehin tesis ücreti", "Noter ve tescil maliyeti kadar"),
  ("Erken ödeme tazminatı, ihtiyaç ve taşıt kredisi", "Kalan vade 12 ay ve altı %0,5, üstü %1"),
  ("Erken ödeme tazminatı, sabit faizli konut kredisi", "Kalan vade 36 ay ve altı %1, üstü %2"),
  ("Erken ödeme tazminatı, değişken faizli krediler ve Ek Hesap", "Alınmaz"),
  ("Ek Hesap kullandırım ücreti", "Alınmaz"),
  ("Yapılandırma", "Ücretsiz"),
  ("Borcu yoktur yazısı (dijital ya da ıslak imzalı)", "Ücretsiz"),
  ("Rehin ve ipotek kaldırma (banka ücreti)", "Alınmaz"),
)
Koşullar: #bkz(<kredi-faiz-vergi>), #bkz(<erken-odeme>) ve #bkz(<kredi-kapanis>). Tapu müdürlüğüne ödenen harçlar banka ücreti değildir.

== Transfer ücretleri <tarife-transfer>
#tablo(("Hizmet", "Ücret"),
  ("Havale (bütün kanallar)", "Ücretsiz"),
  ("FAST, 6.000 TL'ye kadar (Raşit Mobil ve İnternet Şubesi)", "Ücretsiz"),
  ("FAST, 6.000,01-100.000 TL (Raşit Mobil ve İnternet Şubesi)", "6,49 TL"),
  ("FAST (Raşit Bankası ATM'si)", "9,90 TL"),
  ("EFT (Raşit Mobil ve İnternet Şubesi)", "11,95 TL"),
  ("FAST ve EFT (şube)", "74,50 TL"),
  ("Cepten Cebe gönderim", "14,90 TL"),
  ("SWIFT gönderim (Raşit Mobil ve İnternet Şubesi)", "25 ABD doları karşılığı"),
  ("SWIFT gönderim (şube)", "45 ABD doları karşılığı"),
  ("SWIFT OUR seçeneği (ek ücret)", "20 ABD doları karşılığı"),
  ("Gelen SWIFT", "Ücretsiz"),
  ("SWIFT iade, iptal ya da değişiklik talebi", "35 ABD doları karşılığı"),
  ("Fatura ödeme (dijital kanallar ve ATM)", "Ücretsiz"),
  ("Fatura ödeme (şube, fatura başına)", "9,90 TL"),
  ("Vergi ödemeleri (vadesiz hesaptan)", "Ücretsiz"),
  ("Karekod ile ödeme", "Ücretsiz"),
)
Raşit Bankası'ndan maaş ya da emekli maaşı alan müşterilerin dijital kanallardan yaptığı FAST ve EFT işlemleri ücretsizdir. Koşullar: #bkz(<transfer-limit-ucret>).

== Dijital bankacılık ücretleri <tarife-dijital>
#tablo(("Hizmet", "Ücret"),
  ("Raşit Mobil ve İnternet Şubesi kullanımı", "Ücretsiz"),
  ("Uygulama bildirimleri", "Ücretsiz"),
  ("Güvenlik SMS'leri", "Ücretsiz"),
  ("SMS ile kart harcama bildirimi", "Ücretsiz"),
  if S.sifre-cihazi { ("Şifre cihazı (ilk cihaz ya da yenisi)", "150 TL") },
)

== Yatırım işlem ücretleri <tarife-yatirim>
#tablo(("Hizmet", "Ücret"),
  ("Yatırım hesabı açma", "Ücretsiz"),
  ("Yatırım fonu alım satımı", "Alınmaz"),
  ("Hisse senedi alım satım komisyonu", "On binde 15, en az 2,50 TL"),
  ("Hisse senedi, tahvil ve bono saklama", "Alınmaz"),
  ("Devlet tahvili ve hazine bonosu alım satımı", "Alınmaz"),
  ("Eurobond alım satımı", "Tutarın binde 1'i, en az 10 ABD doları karşılığı"),
  ("Döviz ve altın alım satımı", "Komisyon alınmaz"),
  ("Fiziki altını hesaba yatırma", "Gram başına 15 TL"),
)
Döviz ve altın alımlarında ayrıca binde 2 Kambiyo Muameleleri Vergisi alınır; bu tutar ücret değil, vergidir (#bkz(<doviz-alim>)). Fonların yönetim ücreti fon fiyatına yansır (#bkz(<fonlar>)).

== Şube ve ATM ücretleri <tarife-sube-atm>
#tablo(("Hizmet", "Ücret"),
  ("Raşit Bankası ATM'lerindeki bütün işlemler", "Ücretsiz"),
  ("Başka banka ATM'sinden banka kartıyla nakit çekme", "21,75 TL"),
  ("Şubeden nakit çekme ve yatırma", "Ücretsiz"),
  ("Kiralık kasa, küçük (yıllık)", "2.400 TL"),
  ("Kiralık kasa, orta (yıllık)", "3.900 TL"),
  ("Kiralık kasa, büyük (yıllık)", "6.750 TL"),
  ("Kiralık kasa, çok büyük (yıllık)", "11.200 TL"),
  ("Kiralık kasa anahtar teminatı (iade edilir)", "2.000 TL"),
  ("Kasa kilidinin değiştirilmesi (anahtar kaybı)", "3.250 TL"),
  ("Braille alfabesiyle ya da büyük puntolu belge", "Ücretsiz"),
)
Raşit Bankası'ndan maaş ya da emekli maaşı alan müşterilerimiz başka banka ATM'lerinden ayda 5 kez ücretsiz nakit çekebilir. Koşullar: #bkz(<baska-atm>) ve #bkz(<sube-islemleri>).
