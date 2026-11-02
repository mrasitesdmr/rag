TASK: TASK-010
# Onay kartı: Takip sorusunu yeniden yazma

## Ne yapılacak
- Konuşmanın ikinci sorusundan itibaren asistan, "Yenisi kaç günde gelir?" gibi kısa bir soruyu önceki konuşmaya bakarak tek başına anlaşılır bir arama cümlesine çevirir ("Kaybolan kartın yenisi kaç günde gelir?"). Rehberde bu cümleyle arar.
- Cevabı yazan model müşterinin kendi sorusunu ve önceki soru-cevapları görür.
- Çevirme başarısız olursa asistan müşterinin yazdığı soruyla arar; cevap normal gelir.
- Telefonun yanındaki kartın alt satırında arama cümlesi görünür (`· arama: "…"`); süre çubuklarına "yeniden yazma" satırı eklenir.
- Her soruda arama cümlesi, çevirmenin sonucu ve süresi kaydedilir. Özellik sunucu ayarıyla kapatılabilir; varsayılan açık.

## Bittiğini nasıl anlayacaksın
1. "Kartımı kaybettim, ne yapmalıyım?", ardından "Yenisi kaç günde gelir?" yazarsın. Yan kartta arama cümlesi kayıp kartın yenisini sorar; cevap teslim süresi parçasına dayanır.
2. Yeni konuşmanın ilk sorusunda yan kartta arama cümlesi ve "yeniden yazma" satırı görünmez.
3. Özellik kapalıyken aynı iki soruyu sorarsın: arama "Yenisi kaç günde gelir?" ile yapılır. Doğru parçanın gelmemesi beklenen sonuçtur.
4. Raporda gerçek modelle en az 5 takip sorusu olur: açıkken ve kapalıyken arama cümlesi ve modele giden parçalar yan yana.
5. Otomatik testler ve derlemeler geçer. Testler ilk soruda çevirme yapılmadığını, hatada özgün soruya dönüldüğünü ve geçmiş sınırını doğrular.

## Yapılmayacaklar
- "Bu konuşmada devam" öneri düğmeleri (TASK-012).
- Konuşmalar ekranında arama cümlesinin gösterilmesi (TASK-014). Bilgi kayda yazılır ama bu task'ta sadece yan kartta görünür.
- Ekranda açma/kapama düğmesi. Özellik sunucu ayarıyla açılıp kapanır.
- Çevirme için harcanan kelime (token) sayısının kaydı. Sadece süresi kaydedilir.

## Benim verdiğim kararlar
1. **Cevabı yazan model geçmişi görür.** Önceki sorular ve cevaplar ona da gider. Lab ve proje kuralı böyle; TASK-008'de bu task'a bırakılmıştı. Çevirme kapalıyken de gider.
2. **Geçmişe sadece tamamlanmış cevaplar girer.** Hata alan ya da yarıda kesilen cevaplar girmez. Önceki soruların hepsi hatalıysa yeni soru ilk soru sayılır.
3. **Geçmişteki [1] gibi numaralar silinir.** Modele giderken silinir; yeni cevabın numaralarıyla karışmasın.
4. **Yeni kütüphane yok.** Spring AI'ın hazır çeviricisi ek bir parça istiyor ve model boş döndüğünde bunu sessizce gizliyor. Aynı İngilizce talimatı kendi küçük kodumuzla çalıştırıyoruz.
5. **Model cümlesi temizlenir.** Baştaki "Standalone query:" etiketi, tırnaklar ve satır sonları silinir. Cümle 1000 karakterden uzunsa kullanılmaz; arama servisi daha uzununu kabul etmiyor.
6. **Arama cümlesi yan kartta sadece çevirme yapıldıysa görünür.** 40 karakterde kısaltılır; lab'da da böyle.
7. **"Yeniden yazma" süre satırı sadece çevirme denendiyse görünür.** İlk soruda ve özellik kapalıyken görünmez.
8. **Bu özellikten önce kaydedilmiş sorular** kayıtta "çevirme kapalı" olarak işaretlenir.

## Çalışacak roller
1. planner
2. backend-coder: asistan servisi (geçmiş, çevirme, arama, prompt, kayıt). Gerçek modelle bir kez dener.
3. frontend-coder: Önizleme yan kartı
4. tester: otomatik testler
5. devops: Docker'da özelliği kapatıp açmak için tek ayar ve kurulum notu
6. qa: gerçek modelle açık/kapalı karşılaştırma

Atlanan rol yok. Arama servisi (bilgi bankası) değişmiyor.

## Kararını bekleyen sorular
1. Çevirmeye ve cevaba önceki kaç soru-cevap girsin?
   Neden önemli: Az olursa üç soru önceki konuya dönen soru anlaşılmaz. Çok olursa eski konular aramaya karışır ve cevap gecikir.
   Seçenekler: A) Son 1 (lab böyle). B) Son 3. C) Son 5.
   Önerim: B, çünkü "Kartımı kaybettim → Yenisi ne zaman gelir? → Ücreti var mı?" gibi zincirleri kapsar. Gecikme de küçük kalır (yaklaşık yarım saniye).

2. Model İngilizce talimata İngilizce cümleyle cevap verirse ne yapalım?
   Neden önemli: TASK talimatın lab'daki İngilizce metin olmasını, cümlenin Türkçe çıkmasını istiyor. Lab kaydında model Türkçe yazmış ama gerçek denemede farklı çıkabilir.
   Seçenekler: A) Lab metni olduğu gibi kalsın. Denemede tek bir İngilizce cümle bile çıkarsa talimata "takip sorusuyla aynı dilde yaz" satırı eklensin; bunu şimdiden onaylamış olursun. B) Lab metni kalsın; İngilizce çıkarsa iş durup sana sorulsun. C) Satır baştan eklensin.
   Önerim: A, çünkü lab metniyle başlar, sorun çıkarsa iş durmadan düzelir.

3. Çevirme başarısız olursa yan kartta bir iz görünsün mü?
   Neden önemli: Lab'da bu durum yok. İz olmazsa arama neden kısa soruyla yapıldı, anlaşılmaz.
   Seçenekler: A) Görünmesin; alt satır ilk sorudaki gibi kalsın. B) Alt satırda "· yeniden yazma başarısız" yazsın.
   Önerim: B, çünkü sıralama modeli kapalıyken çıkan "reranker DOWN" yazısıyla aynı mantık.

4. Çevirme için modelin cevabı en fazla kaç saniye beklensin?
   Neden önemli: Süre dolunca asistan kısa soruyla arar. Uzun süre cevabı geciktirir; kısa süre, model yavaşken çevirmeyi boşa düşürür.
   Seçenekler: A) 10 sn. B) 20 sn. C) 30 sn.
   Önerim: B, çünkü model sıcakken çevirme 1-2 sn sürer. Uzun aradan sonra modelin yeniden yüklenmesi 10 sn'yi bulabiliyor.

## Riskler
- Takip sorularında cevaptan önce bir model çağrısı daha yapılır. Cevap yaklaşık 1 saniye geç başlar.
- Lab'daki "doğru parça gelir" örnekleri lab kaydından geliyor. Gerçek modelle farklı çıkabilir; rapor gerçek sonucu yazar.

## Kullanıcının cevapları (2026-11-01)
1. B: son 3 soru-cevap.
2. A: lab metni kalır; denemede İngilizce sorgu çıkarsa "takip sorusuyla aynı dilde yaz" satırı eklenir (önceden onaylı).
3. B: alt satırda "· yeniden yazma başarısız" görünür.
4. B: 20 sn.

ONAY: VERİLDİ
