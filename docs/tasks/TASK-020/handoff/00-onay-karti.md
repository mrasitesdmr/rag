TASK: TASK-020
# Onay kartı: Rehber olgu tutarlılığı taraması

## Ne yapılacak
- Rehberin kontrol komutuna iki yeni kontrol eklenir:
  - **Ücret tarifesi kontrolü:** 10. bölümdeki tarifenin (10.1-10.7) her satırı, aynı ücretin bölümlerde geçtiği her yerle karşılaştırılır. Örneğin kâğıt ekstre 2.7'de de 10.1'de de 4,90 TL olmalıdır.
  - **Sık sorulanlar kontrolü:** "Sık sorulanlar" cevaplarındaki her tutar, oran, süre ve saat, cevabın sonundaki "bkz." ile gösterilen başlıkta da yazılı olmalıdır.
- İki kontrol de iki sürümde (2026.10 ve 2026.11) çalışır. Diğer kontroller gibi "GEÇTİ" ya da "KALDI" yazar. KALDI derse çelişen iki yeri sayfa numarasıyla gösterir.
- Planlarken rehberi elle taradım:
  - Ücret tarifesiyle bölümler arasında çelişen değer bulmadım.
  - Sık sorulanlarda 5 cevabın değeri doğru, ama değer atıf yapılan başlıkta değil başka bir başlıkta yazıyor. Bunlar aşağıdaki 1. soruda listeli.
- Senin seçtiğin düzeltme uygulanır, iki PDF yeniden üretilir ve bütün kontroller yeniden çalıştırılır. Ücret, oran ya da süre değerlerinden hiçbiri değişmez.
- İşi ana oturum (lead) yapar, qa bağımsız olarak kontrol eder.

## Bittiğini nasıl anlayacaksın
- Kontrol komutu 13 kontrolün hepsinde "GEÇTİ" diyor (eski 11 kontrol ve 2 yeni kontrol).
- Ücret tarifesi kontrolünün satırı her sürüm için kaç tarife satırının bölümlerde doğrulandığını ve kaç satırın yalnız tarifede geçtiğini yazıyor. Her sürümde 72 tarife satırı var.
- Sık sorulanlar kontrolünün satırı her sürümde 105 sorunun hepsini kontrol ettiğini yazıyor.
- Kontrolün çelişkileri gerçekten yakaladığı geçici bir kopyada denenmiş:
  - 2. bölümün sık sorulanlarına "nakit avans işlem ücreti %1,5" yazılınca kontrol "KALDI" diyor. Hem bu cevabı hem de %1,25 yazan 2.10 ve 10.1'i gösteriyor.
  - Tarifede kâğıt ekstre 5,90 TL yapılınca kontrol "KALDI" diyor ve 4,90 TL yazan 2.7'yi gösteriyor.
  - Gold ücreti sık sorulanlarda yalnız bir sürümde eski değerle yazılınca sadece o sürüm "KALDI" alıyor.
- Rehber hâlâ 70 sayfa. Bölümler eskisiyle aynı sayfada başlıyor.
- Değişen cevaplar sana PDF'teki hâliyle ve sayfa numarasıyla gösteriliyor, sen onaylıyorsun.

## Yapılmayacaklar
- Yeni içerik ya da yeni bölüm eklemek.
- Kontrol betiklerinin genel sağlamlaştırılması (TASK-021).
- Mastercard adı (TASK-023'te yapıldı).
- Terimler sözlüğünün (11. bölüm) taranması. Elle baktım, değerleri tutarlı.
- "Örnek:" ile başlayan hesap örneklerinin aritmetiği. Bunlar TASK-004'te elle doğrulanmıştı.
- Ters yönde tarama: bölümde geçip tarifede olmayan ücretler aranmaz (ör. kiralık kasa gözetim ücreti).
- Servislerde, ekranda ve Docker'da değişiklik.

## Benim verdiğim kararlar
- **Değerler PDF'in kendisinden okunur.** Kontrole hiçbir ücret ya da oran ayrıca yazılmaz. Gold ücreti gibi sürüme göre değişen değerler (2026.10'da 1.450 TL, 2026.11'de 1.650 TL) her sürümün kendi PDF'iyle karşılaştırılır. Değer yine yalnız sürüm dosyasında durur.
- **Tarifedeki her satır için bir arama ifadesi yazılır.** Bu ifade o ücretin bölümlerde nasıl anıldığını tarif eder, örneğin "kâğıt ekstre". Tarifeye yeni bir satır eklenir de ifadesi yazılmazsa kontrol "KALDI" der. Böylece hiçbir satır gözden kaçmaz.
- **Bazı tarife satırları yalnız tarifede geçer.** Örnekler: şubeden kâğıt hesap belgesi 25 TL, eurobond alım satımı. Bunlar hata sayılmaz. Kontrol bunları "yalnız tarifede" diye sayar, liste raporda yer alır.
- **Adetler karşılaştırılmaz.** Sık sorulanlarda tutar, oran, süre ve saat karşılaştırılır; "ayda 4 kez", "en fazla 2 cihaz" gibi adetler karşılaştırılmaz. Sebebi: aynı bilgi farklı kelimeyle yazılabiliyor ("4 kez" ve "4 adede kadar") ve bu yanlış alarm verir.
- **Sorudaki sayılara bakılmaz.** "16 yaşındaki çocuğuma", "100.000 TL kredi çeksem" gibi sayılar müşterinin örneğidir. Yalnız cevaptaki değerler kontrol edilir.
- **Kontrol mevcut kontrol programına eklenir.** Yeni bir araç ya da program kurulmaz.
- **Planda öngörmediğim bir bulgu çıkarsa düzeltmeden önce sana gösterilir.** Lead kontrolü yazıp ilk kez çalıştırdığında öyle bir bulgu çıkarsa iki yeri ve önerisini sana gösterir. Hangi değerin doğru olduğuna sen karar verirsin.
- **Kontrolü gevşetme sınırı.** Lead bir arama ifadesini yanlış alarmı gidermek için daraltabilir. Her daraltmayı gerekçesiyle raporlar, qa da inceler. Rehber metnini senin onayın olmadan değiştiremez.
- **Değişiklik özetine yeni fark eklenmez.** Atıf düzeltmesi iki sürümde de aynıdır. Sayfa kayarsa yalnız sayfa numaraları güncellenir.

## Çalışacak roller
1. planner.
2. lead (ana oturum): iki kontrolü yazar ve çalıştırır. Bulguları sana gösterir, onayladığın düzeltmeyi yapar. PDF'leri üretir, bütün kontrolleri ve denemeleri çalıştırır, raporu yazar.
3. qa: PDF'leri kendi başına üretir, kontrolleri ve bir denemeyi tekrarlar. Arama ifadelerini inceler.

Atlananlar:
- backend-coder ve frontend-coder: servislerde ve ekranda değişiklik yok.
- tester: uygulama kodu yok.
- devops: Docker ve modeller değişmiyor.

## Kararını bekleyen sorular
1. Sık sorulanlarda değeri atıf yapılan başlıkta değil başka bir başlıkta yazan 5 cevap nasıl düzeltilsin?
   Durumlar (değerlerin hepsi doğru, sadece atıf eksik):
   - Kartlar: "Raşit Gold kartın yıllık üyelik ücreti ne kadar?" Cevap 2.4'ü gösteriyor; ücret ve 150.000 TL koşulu 2.2'deki tabloda.
   - Kartlar: "Kart aidatından muaf olmak için ne kadar harcamam gerekiyor?" Cevap 2.4'ü gösteriyor; 60.000, 150.000 ve 400.000 TL 2.2'de.
   - Para transferi: "FAST ile en fazla ne kadar gönderebilirim?" Cevap 5.3'ü gösteriyor; EFT saatleri (09.00-17.00) 5.2'de.
   - Dijital bankacılık: "Transfer limitimi artırdım ama hâlâ işlem yapamıyorum." Cevap 6.6'yı gösteriyor; 10.000 TL sınırı 6.4'te.
   - Şube ve ATM: "Kartım yanımda değil. ATM'den para çekebilir miyim?" Cevap 8.3'ü gösteriyor; işlem başına 5.000 TL 8.4'te.

   Neden önemli: Asistan ya da müşteri atfı izleyince değeri o başlıkta bulamıyor. Kontrol bu 5 cevapta "KALDI" der.

   Seçenekler:
   - A) (önerilen) Atfa ikinci başlık eklenir. Örnek: "(bkz. 2.4)" yerine "(bkz. 2.2 ve bkz. 2.4)"; diğerleri "5.2 ve 5.3", "6.4 ve 6.6", "8.3 ve 8.4". Cevabın metni ve değerleri değişmez.
   - B) Rehber değişmez, kontrol gevşetilir: atıf yapılan başlığın kendi gösterdiği başlıklar da kabul edilir. 4 durum böyle geçer. Dijital bankacılıktaki durumda 6.6 başka başlığı göstermediği için yine atıf eklenir.
   - C) Atıf, yalnız değerin yazdığı başlığı gösterecek şekilde değiştirilir. Örnek: Gold sorusunda 2.4 yerine 2.2.

   Önerim: A, çünkü atfı izleyen değeri hemen bulur ve kontrol katı kalır. B kontrolü gevşetir; ileride yazılacak yanlış atıfları da kaçırır. C ise ikinci başlıktaki bilgiyi gizler; örneğin muafiyet ve iade koşulları 2.4'te.

## Riskler
- **Arama ifadeleri elle yazılır.** Çok dar yazılmış bir ifade bir geçişi kaçırabilir. Kontrol her tarife satırının bölümlerde en az bir yerde bulunmasını şart koşar ve qa ifadeleri inceler; bu riski azaltır ama tamamen gidermez.
- **Sayfa kayması.** Atıf eklemek birkaç kelime ekler, sayfa akışı kayabilir. Bölüm başlangıçları kayarsa lead ifadeyi kendi başına değiştirmez, sana sorar.

## Kullanıcı kararı
- Soru 1: **A** seçildi (atfa değerin yazdığı başlık eklenir).

ONAY: VERİLDİ
