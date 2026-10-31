TASK: TASK-024
# Onay kartı: Parça örtüşmesi ve sayfa etiketinin düzeltilmesi

## Ne yapılacak
- "Sık sorulanlar" kısımlarında bir soru ile cevabı hep aynı parçada durur. Hiçbir parça, sorusu dışarıda kalmış bir cevapla başlamaz.
- Parçanın gösterdiği sayfa, parçanın ilk satırının bulunduğu sayfadır. Müşteri "s. N"yi açınca parçanın başını o sayfada bulur.
- Kurallar sadece rehber için değil, yüklenen her belge için çalışır. Belgede soru-cevap kısmı tanınamazsa parça bugünkü gibi bölünür.
- Parçalamanın geri kalanı değişmez: bölüm sınırı, tablo satırının bölünmemesi, üst/alt bilginin atılması, 1.600 / 2.000 / 200 boyutları.
- Rehber 2026.10 belge ekleme adresiyle yeniden yüklenir; sonuç TASK-005 ile karşılaştırılır.

## Bittiğini nasıl anlayacaksın
- Rapor, rehberin yeni parça sayısını ve ortalama uzunluğunu TASK-005 (154 parça, ortalama 285 token) ile yan yana verir.
- "Döviz alırken komisyon…" sorusunun parçasında "Dolar belli bir kura gelince…" cevabı ya hiç yoktur ya da sorusuyla birlikte vardır.
- Rehberdeki 105 sık sorulan sorunun her biri tek bir parçadadır ve cevabıyla birliktedir.
- qa, önceki parçanın son satırlarıyla başlayan en az 5 parçayı ve yukarıdaki SSS parçasını PDF'te açar. Parçanın ilk metni gösterilen sayfadadır.
- Otomatik testler geçer.

## Yapılmayacaklar
- Vektör arama (TASK-006) ve alıntıların ekranda gösterimi.
- Yeni sürüm yükleme (TASK-015) ve kaynak silme.
- Başlığı ya da soruyu metinden tahmin etmek. Soru-cevap kısmı sadece başlığından tanınır (soru 4).
- Var olan parçaların kendiliğinden yeniden bölünmesi.

## Benim verdiğim kararlar
- **Soru ile cevabı bölünmez:** Bir soru ve cevabı hep aynı parçada kalır; parça, sığdığı kadar bütün soru-cevap çifti taşır. Tek bir çift 2.000 karakteri aşarsa bölünür; devam eden parça yine sorunun kendisiyle başlar.
- **Soruya giriş metni:** Soru-cevap kısmında ilk sorudan önce gelen açıklama satırları bugünkü gibi bölünür.
- **Var olan bilgi bankası nasıl yenilenir:** Kurallar değişince eski parçalar kendiliğinden değişmez; tabloya hiçbir zaman elle parça yazılmaz. Bugün kaynak silme ve yeni sürüm yükleme olmadığı için tek yol verileri silip belgeleri belge ekleme adresiyle yeniden yüklemektir. Asistan tarafında henüz konuşma kaydı olmadığı için bu bir şey kaybettirmez. TASK-015'ten sonra aynı belge yeni sürüm olarak yüklenir ve yeni kurallarla bölünür.
- **Docker'daki sistem:** Açıkta bırakılan Docker sistemindeki eski parçalar bu task'ta değişmez. Deneme ve karşılaştırma geliştirme ortamında yapılır.

## Çalışacak roller
1. planner.
2. backend-coder: parçalama ve sayfa kuralı, PDF'teki kalın satırların okunması. Rehberi gerçek modelle bir kez yükleyip sayıları raporlar.
3. tester: otomatik testler.
4. qa: parçaların PDF'le elle karşılaştırılması, kabul kriterleri.

Atlanan: frontend-coder (ekran yok), devops (Docker ve kurulum değişmiyor).

## Kararını bekleyen sorular
1. Parçanın sayfası nasıl gösterilsin?
   Neden önemli: Alıntılar ileride bu sayfayı "s. N" diye gösterecek; müşteri bilgiyi orada aramalı.
   Seçenekler: A) Parçanın ilk satırının sayfası, tek sayfa ("s. 55"). B) Sayfa aralığı ("s. 55-56"): parçanın ilk ve son satırının sayfası. C) Parça sayfa sonunda kesilir, her parça tek sayfada kalır.
   Önerim: A. Lab da tek sayfa gösteriyor ve parça listesinde sayfanın yanında "İlk satır"ı veriyor; veritabanı ve adresler değişmez. B veritabanına ikinci bir sayfa alanı ekler ve ekran lab'dan ayrılır. C çok sayıda yarım parça üretir.

2. "Sık sorulanlar" kısımlarında örtüşme (önceki parçanın son satırlarının tekrarı) nasıl olsun?
   Neden önemli: Bugünkü 200 karakterlik örtüşme çoğu zaman bir cevabı alıp sorusunu dışarıda bırakıyor; rehberde çiftlerin çoğu 200 karakterden uzun.
   Seçenekler: A) Bu kısımlarda örtüşme olmaz; her parça bir soruyla başlar. B) Örtüşme son soru-cevap çiftini bütün olarak taşır, 200 karakteri aşsa da (2.000 sınırı korunur); aynı çift iki parçada görünür. C) Örtüşme 200 karakterde kalır; son çift sığmıyorsa örtüşme olmaz.
   Önerim: A. Sorular birbirinden bağımsız, örtüşmenin bağlam katkısı yok. B aynı cevabı aramada iki kez getirir. C pratikte A ile aynı sonucu daha karışık bir kuralla verir.

3. Soru-cevap kısmında bir satırın soru olduğu nasıl anlaşılsın?
   Neden önemli: Rehber dışındaki belgelerde kuralın çalışıp çalışmayacağını bu belirler.
   Seçenekler: A) Satırın tamamı kalın yazılmışsa soru sayılır; art arda kalın satırlar tek sorudur. B) "?" ile biten satır soru sayılır. C) Kalın ya da "?" ile biten satır soru sayılır.
   Önerim: A. Rehberdeki 105 sorunun hepsi kalın. B'de "…almak istiyorum." gibi üç soru kaçar ve önceki cevabın parçası sayılır. C cevabın içinde soru işaretiyle biten bir satırı yanlışlıkla soru yapabilir.

4. Hangi başlıklar soru-cevap kısmı sayılsın?
   Neden önemli: Bugünkü okuyucu sadece rehberdeki "Sık sorulanlar" adını tanıyor; başka bir belge aynı kısmı başka adla açabilir.
   Seçenekler: A) Sadece "Sık sorulanlar". B) Sabit bir liste, büyük/küçük harf farksız: "Sık sorulanlar", "Sık sorulan sorular", "Sıkça sorulan sorular", "SSS", "FAQ", "Frequently asked questions".
   Önerim: B. Kural her belge için geçerli olmalı; liste sabit olduğu için tahmin yoktur. Listede olmayan bir adla gelen kısım normal metin gibi bölünür.

## Riskler
- **Kalın yazı okunamayabilir.** Bazı PDF'ler kalın yazıyı yazı tipi adıyla değil başka yollarla çizer. O belgede sorular tanınmaz ve parçalar bugünkü gibi bölünür.
- **Parça sayfa sonuna taşabilir.** Tek sayfa gösteriminde parçanın sonu bir sonraki sayfada olabilir; müşteri okumaya orada devam eder.

ONAY: VERİLDİ
