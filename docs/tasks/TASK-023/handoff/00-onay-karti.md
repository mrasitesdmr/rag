TASK: TASK-023
# Onay kartı: Rehberden gerçek kart şeması adının kaldırılması

## Ne yapılacak
- Rehberde "Mastercard" adı 4 cümlede geçiyor: 2.1, 2.2, 2.14 ve 2.17. Bu 4 cümle, aşağıdaki sorularda senin seçeceğin ifadeyle yeniden yazılır. Rehberde başka hiçbir şey değişmez.
- Değişiklik iki sürüme (2026.10 ve 2026.11) birden uygulanır.
- Kontrol komutu, rehberde gerçek kart şeması adı (Mastercard, Visa, American Express, Troy ve birkaç benzeri) geçmediğini de kontrol eder hâle gelir.
- İki PDF yeniden üretilir. Bütün kontroller yeniden çalıştırılır.
- Değişen 4 cümle sana PDF'teki hâliyle ve sayfa numarasıyla gösterilir.
- İşi ana oturum (lead) yapar, qa bağımsız olarak kontrol eder.

## Bittiğini nasıl anlayacaksın
- Kontrol komutu bütün kontrollerde "GEÇTİ" diyor. Gerçek ad kontrolü artık kart şemalarını da sayıyor ("35 gerçek banka adı geçmiyor").
- İki PDF'te "Mastercard" aradığında sonuç çıkmıyor. Visa, American Express ve Troy için de sonuç çıkmıyor.
- 2.14'ü açınca şunlar duruyor, marka adı olmadan: kartlar yurt dışında kullanılabilir, yurt dışı kullanım seyahatten önce açılmalıdır, her işlemde %1,99 ücret alınır. 2.17'deki itiraz süreleri (120 gün, ortalama 45 gün, yurt dışında 90 güne kadar) aynı.
- Rehberin sayfa sayısı 70'te kalıyor. Bölümler eskisiyle aynı sayfada başlıyor, örneğin Hesaplar 20. sayfada. Kartlar bölümünün dışındaki bütün sayfalar eskisiyle kelimesi kelimesine aynı. Tek istisna içindekiler olabilir: bir Kartlar başlığı sayfa değiştirirse sadece onun sayfa numarası değişir.
- Kontrolün gerçekten yakaladığı bir kez denenmiş: 2.14'e geçici bir kopyada "VISA, Troy ve Mastercard'ın" yazılınca kontrol "KALDI" diyor ve üç adı da gösteriyor.
- Değişen 4 cümleyi görüp onaylıyorsun.

## Yapılmayacaklar
- Rehberdeki diğer içerik değişiklikleri. Olgu tutarlılığı taraması TASK-020'de yapılacak.
- Kontrol programının kendisinin sağlamlaştırılması (TASK-021).
- Lab dosyaları (`kaynaklar/`).
- Değişiklik özetine yeni bir fark eklenmesi. Bu düzeltme iki sürümün ortak metnindedir, sürümler arası fark değildir (bkz. kararlar).
- Türk bankalarının kart program adları (Bonus, World, Maximum, Axess gibi) kontrol listesine eklenmez (bkz. kararlar).
- Servislerde, ekranda ve Docker'da değişiklik.

## Benim verdiğim kararlar
- **Sadece marka ifadesi değişir.** 4 cümlede yalnız "Mastercard" ifadesinin olduğu kısım değişir. Cümlenin geri kalanı, ücretler, süreler ve tablolar olduğu gibi kalır.
- **Değişiklik özetine girmez.** Değişiklik özeti 2026.10 ile 2026.11 arasındaki farkları anlatır; bu düzeltme iki sürümde de aynıdır. Metin uzayıp kısalınca özette yazan bir sayfa numarası kayarsa (ör. Raşit Gold'un 8. sayfadaki tablosu 7. sayfaya geçerse) sadece o numara güncellenir.
- **Kontrol listesine 11 ad eklenir:** Mastercard, Visa, American Express, Troy; bunların yanında Amex, Maestro, UnionPay, JCB ve Diners Club. Bu son beşi de gerçek kart şemalarıdır; rehberde hiçbiri geçmiyor.
- **Büyük harfle yazılan adlar da yakalanır.** Kontrol, Türkçe kurala göre küçük harfe çevirerek arar. Bu kurala göre "VISA" "vısa" olur ve "visa" aramasına takılmaz. Bu yüzden "VISA" ve "AMERICAN EXPRESS" listeye ayrıca yazılır.
- **Ekler ve yanlış alarm.**
  - Kontrol tam kelime arar. "Mastercard'ın" gibi kesme işaretiyle ek almış hâl yakalanır.
  - "Troy" tam kelime arandığı için "Troya" ya da "troyka" gibi sözcükler yanlış alarm vermez.
  - Kesme işaretsiz yazım ("Mastercardla") kontrolden kaçabilir. Bunun için lead bir kez, harf büyüklüğüne ve eklere bakmadan ek bir tarama yapar ve sonucunu sana raporlar.
- **Türk bankalarının kart program adları eklenmez.** Bonus, World, Maximum, Axess gibi adlar kart şeması değil, bankaların kendi program adlarıdır. Bunlar TASK kapsamında değil. "Bonus" ve "maximum" sıradan sözcük de olduğu için yanlış alarm verebilirler. Bugün rehberde geçmiyorlar.
- **Kontrol programı değişmez.** Sadece kontrol listesi ve rehberin açıklama dosyası güncellenir. Açıklama dosyasındaki yazım kurallarına "gerçek kart şeması adı yazılmaz" maddesi eklenir.
- **Sayfa düzeni önceki hâlle karşılaştırılır.** Bölüm başlangıçları ve sayfalar eski PDF'lerle tek tek karşılaştırılır. Mevcut kontrol yalnız iki sürümü birbiriyle karşılaştırıyor; iki sürüm birlikte kaysa fark etmezdi.

## Çalışacak roller
1. planner.
2. lead (ana oturum): 4 cümleyi değiştirir, kontrol listesini günceller, PDF'leri üretir, kontrolleri ve önceki hâlle karşılaştırmayı çalıştırır, raporu yazar, değişen metni sana gösterir ve onayını alır.
3. qa: PDF'leri kendi başına yeniden üretir, kontrolleri çalıştırır, değişiklikleri inceler.

Atlananlar:
- backend-coder ve frontend-coder: servislerde ve ekranda değişiklik yok.
- tester: uygulama kodu yok; kontrolleri lead çalıştırır, qa bağımsız tekrarlar.
- devops: Docker ve modeller değişmiyor.

## Kararını bekleyen sorular
Dört soruda harfler aynı yaklaşımı gösterir:
- A: markanın yerine genel bir ifade,
- B: markayı anan kısmı çıkarmak,
- C: kurgusal bir ad.

Dördüne de aynı harfi seçebilirsin ("hepsi A" demen yeterli). Her soruda önerdiğim seçenek "(önerilen)" diye işaretli.

1. 2.1 (Raşit Banka Kartı) cümlesi nasıl yazılsın?
   Mevcut: "Kart Mastercard altyapısıyla çalışır, temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir."
   Neden önemli: Asistan "Banka kartım yurt dışında geçer mi?" gibi sorularda bu cümleye dayanır.
   Seçenekler:
   A) (önerilen) "Kart uluslararası kart altyapısıyla çalışır, temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir."
   B) "Kart temassız ödeme özelliğine sahiptir ve Türkiye'de ve yurt dışında kullanılabilir." Altyapı hiç anılmaz.
   C) "Kart <kurgusal ad> altyapısıyla çalışır, …". Adı sen verirsin; lead adın gerçek bir markaya ait olmadığını arayarak kontrol eder.
   Önerim: A. Cümle aynı kalır, sadece marka genel bir ifadeye döner. Kurgusal ad, rehberin cevaplamadığı yeni bir soru doğurur ("<ad> nedir?") ve gerçek bir markayla çakışabilir.

2. 2.2 (Kredi kartı türleri) cümlesi nasıl yazılsın?
   Mevcut: "Raşit Bankası'nın bireysel kredi kartlarının hepsi Mastercard altyapısıyla çalışır, temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (bkz. 2.11)."
   Neden önemli: Bu cümle bütün kredi kartlarının ortak özelliklerini sayar. Asistan kart karşılaştırma sorularında buna dayanır.
   Seçenekler:
   A) (önerilen) "Raşit Bankası'nın bireysel kredi kartlarının hepsi uluslararası kart altyapısıyla çalışır, temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (bkz. 2.11)."
   B) "Raşit Bankası'nın bireysel kredi kartlarının hepsi temassız ödeme özelliğine sahiptir, taksitli alışverişe uygundur ve RaşitPuan kazandırır (bkz. 2.11)."
   C) 1. sorudaki kurgusal ad, aynı yapıda.
   Önerim: A. 2.1 ile aynı ifade olur, kartların yurt dışında geçerli olduğu bilgisi bu cümlede de kalır.

3. 2.14 (Yurt dışında kullanım ve kur) cümlesi nasıl yazılsın?
   Mevcut: "Kartlarınız yurt dışında Mastercard logosu bulunan her yerde kullanılabilir."
   Neden önemli: "Yurt dışında banka kartımı kullanabilir miyim?" sorusunun cevabı bu cümleyle başlar. Görevdeki örnek senaryo budur.
   Seçenekler:
   A) (önerilen) "Kartlarınız yurt dışında, kartınızın üzerindeki uluslararası kart logosunun bulunduğu her yerde kullanılabilir."
   B) "Kartlarınız yurt dışında kart kabul eden her yerde kullanılabilir." Kısa, ama "logosu bulunan yerde" koşulu düşer ve söz biraz genişler.
   C) "Kartlarınız yurt dışında <kurgusal ad> logosu bulunan her yerde kullanılabilir."
   Önerim: A. "Logo bulunan yerde geçerli" koşulu aynen kalır, sadece marka adı çıkar.

4. 2.17 (Harcama itirazı) cümlesi nasıl yazılsın?
   Mevcut: "Raşit Bankası, işlemi yapan işyerinin bankasından Mastercard kuralları çerçevesinde belge ister."
   Neden önemli: İtiraz sürecini anlatan maddelerden biridir. Hemen altındaki süreler (ortalama 45 gün, yurt dışında 90 güne kadar) değişmez.
   Seçenekler:
   A) (önerilen) "Raşit Bankası, işlemi yapan işyerinin bankasından uluslararası kart kuralları çerçevesinde belge ister."
   B) "Raşit Bankası, işlemi yapan işyerinin bankasından belge ister."
   C) "Raşit Bankası, işlemi yapan işyerinin bankasından <kurgusal ad> kuralları çerçevesinde belge ister."
   Önerim: A. İtirazın belli kurallara göre yürüdüğü bilgisi kalır, diğer üç cümleyle aynı ifade kullanılır.

## Riskler
- **Sayfa kayması.** Cümleler birkaç kelime uzayıp kısalacağı için Kartlar bölümünde bir tablo ya da "Sık sorulanlar" bir sayfa öne veya arkaya kayabilir. Değişiklik özetindeki sayfa numarası o zaman güncellenir. Bölümlerin başladığı sayfalar kayarsa lead seçtiğin ifadeyi kendi başına değiştirmez; sana sorar.
- **Kesme işaretsiz ekler.** "Mastercardla" gibi bir yazım kalıcı kontrolden kaçabilir. Bu task'ta lead'in ek taramasıyla kapatılır. Kalıcı kontrol için gerekirse ayrı bir takip task'ı açılır.

## Kullanıcı kararı
- 2026-10-30: Dört soruda da A seçildi (2.1, 2.2, 2.14, 2.17 → "uluslararası kart altyapısı / logosu / kuralları"). Kart bu hâliyle onaylandı.
- 2026-10-30 (uygulama sırasında): 2.17'nin A ifadesi ("uluslararası kart kuralları") 2026.11 PDF'ini 71 sayfaya çıkardı (D1, D8, D9 KALDI). Lead üç kısa varyantı scratch'te denedi, üçü de 11/11 geçti. Kullanıcı 2.17 için "uluslararası kurallar çerçevesinde" ifadesini seçti. 2.1, 2.2 ve 2.14 A olarak kaldı.

ONAY: VERİLDİ
