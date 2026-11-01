TASK: TASK-008
# Onay kartı: İlk uçtan uca cevap: Önizleme, cevap akışı ve tur kaydı

## Ne yapılacak
- Önizleme'deki telefona istediğin soruyu yazarsın. Asistan yüklü belgelerde arar, en uygun 20 parçayı sıralama modeline puanlatır, en iyi 5'ini dil modeline verir.
- Cevap ekranda kelime kelime yazılarak gelir. Cevaptaki [1] gibi numaralara tıklayınca o parça Bilgi kaynakları'ndaki pencereyle açılır. Cevabın altında dayandığı belgelerin sayfa düğmeleri durur.
- Rehberde cevap yoksa asistan "Bu konuda rehberimizde bilgi bulamadım." yazar; sayfa düğmeleri çıkmaz.
- Telefonun yanındaki kart, cevaptan sonra modele giden 5 parçayı (sayfa, başlık, sıra, puan, mesafe) ve adımların gerçek sürelerini gösterir.
- Her soru-cevap, modele giden parçalar ve sürelerle birlikte kaydedilir. "Yeni konuşma" düğmesi yeni bir konuşma başlatır.
- Sıralama modeli kapalıysa asistan arama sırasındaki ilk 5 parçayla cevaplar; müşteri hata görmez.

## Bittiğini nasıl anlayacaksın
1. Hiç belge yokken soru sorarsın: "bulamadım" cevabı ve belge eklemeni söyleyen not görünür.
2. Rehberi yükleyip "Kartımı kaybettim, ne yapmalıyım?" yazarsın. Cevap yazılarak gelir, [1] taşır; [1]'e tıklayınca parça açılır.
3. "Bitcoin alıp satabilir miyim?" yazarsın: "bulamadım" cümlesi gelir.
4. Cevap akarken sayfayı kapatırsın: dil modeli o soruyu üretmeyi bırakır.
5. Dil modelini ya da arama servisini kapatıp soru sorarsın: Türkçe hata mesajı görürsün.
6. Sıralama modelini kapatıp aynı soruyu sorarsın: cevap yine gelir, yan kartta "reranker DOWN · vector sırası" yazar.
7. Raporda gerçek modellerle 5 rehber ve 2 rehber dışı soru, alıntılar, süreler ve 5 soruda sıralamadan önce/sonra parça sırası olur.
8. Önizleme lab'ın sol yarısıyla yan yana aynı görünür. Otomatik testler ve derlemeler geçer.

## Yapılmayacaklar
- Takip sorusunu yeniden yazma (TASK-010). Bu task'ta model önceki soruları görmez; "Peki yenisi?" gibi sorular bağlamsız cevaplanır.
- Yapılandırma şeridi ve "Sadece LLM" modu (TASK-011). Öneri çipleri ve kayıtlı sorular (TASK-012). "Faydalı mı?" ve "Temsilciye bağlan" (TASK-013). Konuşmalar ekranı (TASK-014). Sarı "cevabın geçtiği parça" işareti (TASK-016).

## Benim verdiğim kararlar
1. **Soru uzunluğu:** 1-1000 karakter. Baştaki ve sondaki boşluk silinir. Sadece boşluk yazılırsa "Gönder" pasif kalır. Arama servisinin sınırı da 1000.
2. **Boş bilgi bankası:** Dil modeli hiç çağrılmaz; "bulamadım" cümlesi hemen yazılır.
3. **"Bulamadım" tespiti:** Cevap bu cümleyi içeriyorsa (sonunda [1] olsa da) tur "cevap yok" sayılır.
4. **Hata yeri:** Arama servisi kapalıysa hata cevap başlamadan gelir. Dil modeli hatası cevap balonunda görünür; yarıda kesilirse yazılan kısım kalır, altında hata yazar.
5. **Sayfadan çıkınca** ekrandaki konuşma kapanır; dönünce yeni konuşma başlar. Eski konuşma kayıtta kalır (Konuşmalar ekranında görünecek). "Yeni konuşma"ya basınca önceki konuşma lab'daki gibi ayracın üstünde kalır.
6. **Aynı konuşmaya aynı anda iki soru** gelirse ikincisi reddedilir.
7. **Kalın yazı ve madde listeleri** lab'daki gibi gösterilir, ek kütüphane olmadan.
8. **Puan:** Yan karttaki puan sıralama modelinin ham puanıdır (ör. 5,36; eksi olabilir). Lab'daki 0,97 gibi 0-1 arası değildir.
9. **"Asistan nasıl cevaplıyor?" metni** lab'daki gibi sabit kalır; ayar değişirse TASK-011'de güncellenir. Cevaptan sonraki kart ise gerçek ayarı gösterir.
10. **Bekleme süreleri:** Dil modelinin ilk kelimesi için 60 sn, kelimeler arası 30 sn. Arama servisi için 15 sn.

## Çalışacak roller
1. planner
2. backend-coder: asistan servisi (konuşma, arama, sıralama, prompt, cevap akışı, kayıt). Lokal modellerle bir kez dener.
3. frontend-coder: Önizleme ekranı
4. tester: otomatik testler ve uçtan uca test
5. devops: Docker ayarları, cevap akışının web üzerinden kesilmeden gelmesi, model kapatma denemeleri, kurulum notları
6. qa: gerçek modellerle sorular, sıralama karşılaştırması, lab'la yan yana karşılaştırma

Atlanan rol yok. Arama servisi (bilgi bankası) değişmiyor.

## Kararını bekleyen sorular
1. Dil modeliyle konuşmak için Spring AI kütüphanesini kuralım mı?
   Neden önemli: Onayın olmadan yeni kütüphane eklenmiyor; asistan servisinin dil modeline bağlanma yolu buna göre yazılır.
   Seçenekler: A) Kurulsun (Ollama bağlantısı ve cevabı parça parça okuma hazır gelir). B) Kurulmasın; bağlantıyı kendimiz yazalım (bilgi bankası servisinde yaptığımız gibi).
   Önerim: A, çünkü proje mimarisi Spring AI diyor ve TASK-010'daki soru yeniden yazma onun hazır şablonunu kullanıyor.

2. Cevabın altındaki sayfa düğmelerinde belge adı nasıl yazılsın?
   Neden önemli: Birden fazla belge olabilir; telefon ekranı dar.
   Seçenekler: A) Kısaltılmış ad: "Raşit Bankası Ürün ve H… · s. 19" (24 karakter; üstüne gelince tam ad). B) Tam ad: "Raşit Bankası Ürün ve Hizmet Rehberi · s. 19". C) Tek belge varken "Rehber · s. 19", birden fazlaysa kısaltılmış ad.
   Önerim: A, çünkü düğmeler telefona sığar ve her durumda aynı görünür.

3. Süre kartında lab'daki makine etiketi ("Apple M5 · 16 GB") gösterilsin mi?
   Neden önemli: Süreler makineye bağlı; ama sistem Docker içinden makinenin adını doğru öğrenemez.
   Seçenekler: A) Gösterilmesin. B) Ayardan gelsin: kurulumda bir kez yazarsın, boşsa görünmez.
   Önerim: B, çünkü süreleri okurken hangi makinede ölçüldüğü bilinir ve kodda sabit değer olmaz.

4. Hiç belge yokken sorulan sorunun cevabının altında "Bilgi bankasında henüz belge yok. Bilgi kaynakları'ndan belge ekle." notu (bağlantılı) görünsün mü?
   Neden önemli: Not olmazsa "bulamadım" cevabının sebebi anlaşılmaz.
   Seçenekler: A) Görünsün. B) Görünmesin; sadece "bulamadım" cümlesi kalsın.
   Önerim: A, çünkü yönetim panelini kullanan kişiye ne yapacağını söyler.

5. Sıralama modeli cevap vermezse kaç saniye beklensin?
   Neden önemli: Bu süre dolunca asistan arama sırasıyla devam eder; uzun süre cevabı geciktirir, kısa süre yavaş anlarda gereksiz yedek yola düşürür.
   Seçenekler: A) 3 sn. B) 5 sn. C) 10 sn.
   Önerim: B, çünkü bu makinede 20 parçanın puanlanması 1 saniyenin altında ölçüldü; 5 sn yeterli pay bırakır. Model tamamen kapalıysa beklemeden hemen devam eder.

## Riskler
- Üç model aynı anda 16 GB belleği zorlar. Sistem yeni açıldığında ilk cevap 10 saniyeyi bulabilir.
- TASK'taki "erken ödeme parçası 7. sıradan 1. sıraya çıkar" örneği lab kaydından geliyor; gerçek rehber ve gerçek modelle farklı çıkabilir. Rapor gerçek sonucu yazar.

## Kullanıcının cevapları (2026-11-01)
1. A: Spring AI kurulsun. 2. A: kısaltılmış ad (24 karakter, üstüne gelince tam ad). 3. B: makine etiketi ayardan, boşsa gizli. 4. A: boş bilgi bankası notu görünsün. 5. B: reranker bekleme süresi 5 sn.
Kartın geri kalanı (planner kararları dahil) olduğu gibi onaylandı.

ONAY: VERİLDİ
