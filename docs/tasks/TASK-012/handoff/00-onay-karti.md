TASK: TASK-012
# Onay kartı: Önizleme soru önerileri (öneri çipleri, soru listesi, otomatik tamamlama)

## Ne yapılacak
- Önizleme telefonunda, giriş alanının üstünde en fazla 2 öneri çipi çıkar. Boş konuşmada iki başlangıç sorusu görünür; bir cevaptan sonra ilk çip o soruya bağlı bir takip sorusudur. Takip aynı konuşmada, diğer çipler yeni konuşmada sorulur ("Yeni konuşmada sorulur" ipucu lab'daki gibi).
- Giriş alanının solunda "Soru listesi" düğmesi olur. Düğme "Kayıtlı sorular · N soru, M takip" penceresini açar: önce "Bu konuşmada devam", sonra kategoriler. Bir soruya tıklayınca soru sorulur ve pencere kapanır.
- Yazarken en fazla 7 öneri listelenir. Türkçe eklere dayanıklıdır ("asgari" yazınca asgari ödemeyle ilgili sorular çıkar). Liste ↑ ↓, Enter ve Esc ile kullanılır. Eşleşme yoksa liste kapanır ve yazdığın soru normal gönderilir.
- Öneriler, ürün ekibinin hazırladığı listeden gelir (soru 1'in önerisi). Listede mock ya da koda yazılmış soru yoktur. Liste yüklenene kadar çipler ve pencere boş durum metnini gösterir.

## Bittiğini nasıl anlayacaksın
1. Hiç liste yüklenmemişken çiplerin yerinde "Henüz soru önerisi yok." yazar. Pencerenin başlığında "0 soru, 0 takip" ve boş durum metni görünür.
2. Örnek bir liste yüklenince boş konuşmada iki başlangıç çipi görünür; çipe tıklayınca soru sorulur.
3. "Kartımı kaybettim, ne yapmalıyım?" cevabından sonra ilk çip takip sorusudur (ör. "Yenisi kaç günde gelir?"). Tıklayınca aynı konuşmada sorulur. Diğer çip yeni bir konuşma açar.
4. "asgari" yazınca asgari önerileri listelenir; ↓ ve Enter ile seçilen soru sorulur. Esc listeyi kapatır, yazdığın kalır.
5. Listede olmayan bir soru yazıp Enter'a basınca soru normal şekilde sorulur.
6. Pencerede soruya tıklayınca soru sorulur ve pencere kapanır. Esc pencereyi kapatır.
7. Görünüm lab'la yan yana karşılaştırılır. Klavye ve erişilebilirlik testleri ile derlemeler geçer. Kodda sabit soru listesi olmadığı aramayla doğrulanır.

## Yapılmayacaklar
- Önerileri ekrandan yönetmek (TASK kapsam dışı). Liste ekransız, tek komutla yüklenir.
- Raşit rehberi için gerçek öneri listesini hazırlamak. Listeyi sen (ürün ekibi) yazarsın. Ekip denemeler için küçük bir deneme listesi yükler ve iş bitince listeyi eski hâline getirir.
- Bilgi bankası servisinde ve cevap akışında değişiklik.

## Benim verdiğim kararlar
1. **Öneri seçilince hemen sorulur** (lab). Takip önerisi aynı konuşmada sorulur. Listedeki diğer sorular, konuşmada soru varsa yeni konuşmada sorulur: çip, pencere ve otomatik tamamlamadaki "Yeni konuşmada" bölümü için geçerli. Kendi yazdığın soru bugünkü gibi aynı konuşmada sorulur.
2. **Takip önerisi ne zaman çıkar:** Son soru listedeki bir soruyla aynıysa çıkar; büyük/küçük harf ve noktalama farkı önemsenmez. Cevap hatasız bitmiş olmalıdır. Takip sorusundan sonra yeni takip çıkmaz (lab böyle).
3. **"Sorulmuş sorular yeniden önerilmez" kuralı sadece çipler ve bu konuşma için geçerli.** Pencere ve otomatik tamamlama bütün soruları gösterir.
4. **Başlangıç çipleri:** Önce ürün ekibinin işaretlediği sorular gelir; yetmezse listedeki sıradaki sorular. Uzun öneri iki satıra iner. Lab 34 harften uzun soruları çipte hiç göstermiyordu.
5. **Otomatik tamamlama toplam en fazla 7 öneri gösterir;** önce takip soruları gelir. Lab'daki "listeden seç" ve "Eşleşen soru yok" metinleri yoktur. Giriş alanından çıkınca liste kapanır.
6. **Cevap yazılırken** çipler ve penceredeki sorular kapalıdır. Pencere yine açılabilir (lab).
7. **Öneriler okunamazsa** (servis kapalı) çip satırı boş kalır, pencere hata mesajı gösterir. Soru yazıp göndermek çalışmaya devam eder.
8. **Liste sınırları:** En fazla 50 kategori ve toplam 500 soru olur; her soruya en fazla 5 takip eklenir. Soru en fazla 200, kategori adı en fazla 40 harftir. Aynı soru iki kez yazılamaz. Kurala uymayan bir dosya hiç yüklenmez ve eski liste olduğu gibi kalır.
9. **Yeni yüklenen liste** açık Önizleme sayfasında hemen görünmez; sayfa yenilenince ya da sekmeye dönünce görünür.
10. **Kategorilerin ve soruların sırası** dosyadaki sırayla aynıdır.

## Çalışacak roller
1. planner
2. backend-coder: asistan servisi (öneri listesinin saklanması, okuma ve yükleme)
3. frontend-coder: çipler, soru listesi penceresi, otomatik tamamlama
4. tester: otomatik testler (klavye ve erişilebilirlik dahil)
5. devops: sistemi yeni kodla yeniden kurar, büyük liste dosyası için sınırı ayarlar, yükleme komutunu kılavuza yazar
6. qa: deneme listesiyle dört senaryo, lab ile görünüm karşılaştırması

Atlanan rol yok. Bilgi bankası servisi değişmiyor.

## Kararını bekleyen sorular
1. Öneriler nereden gelsin?
   Neden önemli: Çiplerin, pencerenin ve otomatik tamamlamanın hepsi bu kaynağa dayanır. Seçim işin büyüklüğünü ve ne zaman yapılabileceğini belirler.
   Araştırma özeti: Bu bilgi kamuya açık ürünlerden bildiklerime dayanıyor; bu oturumda internette doğrulayamadım. Bankaların asistanlarında (ör. Bank of America Erica, Garanti BBVA Ugi, İşCep Maxi) öneri düğmeleri ve hazır cevap seçenekleri genelde bankanın ekibi tarafından yazılır ve onaylanır. Takip önerileri konuya bağlı olarak önceden tanımlanır. Yapay zekâ platformları önerileri otomatik üretebiliyor; bankalar ise müşteriye görünen metni çoğunlukla onaydan geçirerek yayınlıyor.
   Seçenekler:
   A) Ürün ekibinin hazırladığı liste. Kategoriler, sorular, başlangıç işareti ve takip soruları tek bir dosyada durur; dosya tek komutla yüklenir ve listenin tamamını değiştirir. Listeyi sen yazarsın; yüklenene kadar boş durum görünür. Pencere başlığı lab'daki gibi kalır.
   B) Golden set'ten türetilen sorular. Bu seçenek TASK-016'ya bağlıdır: golden set'in yapısı orada belirleneceği için bu task o zamana kadar bekler.
   C) Geçmiş konuşmalardaki sık sorular. Liste kendiliğinden oluşur, ama doğal bir kategori yoktur (kategori belge adı olur). Başta liste boştur. Müşterilerin yazdığı, kişisel bilgi ya da yazım hatası içerebilen sorular başkalarına önerilir.
   D) Dil modelinin yüklenen belgelerden ürettiği sorular. Her belge için kendiliğinden oluşur; kategoriler belgenin bölümlerinden gelir. Ancak bir belge için üretim dakikalar sürer ve o sırada Önizleme cevapları yavaşlar (tek model var). Soruların kalitesi garanti değildir ve iş belirgin büyür.
   Önerim: A. Bankalardaki yaygın uygulamaya uyar, hemen yapılabilir, sonuç öngörülebilir ve lab'daki "N soru, M takip" yapısını aynen korur. A dışında bir seçenek seçersen planı yeniden yazarım ve kart yeniden onayına gelir.

2. Takip önerileri nasıl üretilsin?
   Neden önemli: Bir cevaptan sonra çıkan ilk çipi belirler.
   Seçenekler:
   A) Listedeki bağlantılarla. Her soruya listede en fazla 5 takip sorusu yazılır. O soru sorulup cevaplanınca sorulmamış ilk takip çip olur. Ek model çağrısı yoktur. Ancak listede olmayan bir soru yazıldığında takip önerisi çıkmaz.
   B) Dil modeliyle. Her cevaptan sonra model, cevaba ve rehber parçalarına bakarak bir takip sorusu yazar. Her soruda çalışır, ama her cevaptan sonra birkaç saniyelik ek model çağrısı yapılır. Önerilen soru rehberde cevabı olmayan bir soru olabilir. Başlıktaki "M takip" sayısı da anlamını yitirir.
   C) İkisi birden. Listede bağlantı varsa o kullanılır, yoksa model yazar.
   Önerim: A, çünkü lab böyle çalışıyor, modele ek yük binmez ve sonuç her seferinde aynıdır.

3. Otomatik tamamlamada eşleşme nasıl yapılsın?
   Neden önemli: "asgari" ya da "kartımı" yazınca hangi önerilerin hangi sırayla çıkacağını belirler.
   Seçenekler:
   A) Lab'daki yöntem. Kelimelerin ilk 4 harfi kök sayılır; böylece "kartımı" ile "kartın" eşleşir. Nadir geçen kelimeler daha ağır basar: "şifre" eşleşmesi "kart" eşleşmesinden değerlidir. Yazılanla başlayan soru öne çıkar. Hesap tarayıcıda yapıldığı için anında çalışır. Sınırı şudur: 4 harften kısa yazılan kelime, sadece onunla başlayan soruyu bulur.
   B) Veritabanının Türkçe kelime araması. Her tuşta sunucuya istek gider. Bazı eklerde daha iyi, bazılarında daha kötü sonuç verir.
   C) Anlam benzerliği. "Kartım çalındı" yazınca "kaybettim" sorusunu da bulur. Ancak her tuşta embedding modeline istek gider ve bilgi bankası servisinin değişmesi gerekir.
   Önerim: A, çünkü lab'da denenmiş, anında çalışıyor ve kısa bir listede Türkçe eklere yeterince dayanıklı.

4. Boş durum ve pencere açıklama metinleri şunlar olsun mu?
   Neden önemli: Liste yüklenene kadar kullanıcı bu metinleri görür. Lab'daki açıklama cümlesi ("Her soru rehber üzerinde gerçekten çalıştırıldı…") gerçek üründe doğru değil.
   Önerdiğim metinler:
   - Çip satırı (liste boşken): "Henüz soru önerisi yok."
   - Pencere (liste boşken): "Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür."
   - Pencere açıklaması (her zaman): "Ürün ekibinin hazırladığı öneri sorular; sorunu her zaman kendin de yazabilirsin. Takip soruları ilk sorudan sonra "devam" önerisi olarak görünür."
   Seçenekler: A) Bu metinler. B) Liste boşken çip satırı tamamen gizlensin, metin sadece pencerede görünsün. C) Kendi metnini yaz.
   Önerim: A, çünkü TASK boş durumun hem çiplerde hem pencerede görünmesini istiyor ve metinler ne yapılacağını söylüyor.

## Riskler
- Liste yüklenene kadar ekranda öneri görünmez; ilk açılışta boş durum metinleri görünür.
- Sistemde hiç kimlik doğrulama olmadığı için listeyi sisteme erişen herkes değiştirebilir. Bugün belge yükleme de böyle çalışıyor.
- Takip önerisi sadece listedeki bir soru sorulduğunda çıkar (soru 2'de A seçilirse). Kendi cümleni yazarsan çiplerde başlangıç önerileri görünür.

## Kullanıcı kararları (2026-11-02)
1. A: ürün ekibinin hazırladığı liste
2. A: listedeki bağlantılar
3. A: lab yöntemi (4 harflik kök + IDF, tarayıcıda)
4. A: önerilen metinler
Kartın geri kalanı olduğu gibi onaylandı.

ONAY: VERİLDİ
