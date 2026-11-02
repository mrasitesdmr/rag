TASK: TASK-013
# Onay kartı: Geri bildirim ve temsilciye aktarma

**Senden 3 karar bekliyorum (aşağıda, "Kararını bekleyen sorular"):** aktarılan konuşmada soru sorulabilir mi, aktarma nerede ve hangi kuyruk adıyla tutulur, Konuşmalar ekranında nasıl görünür.

## Ne yapılacak
- Önizleme'de her tamamlanmış cevabın altında lab'daki "Faydalı mı? Evet / Hayır" satırı ve cevabın saati çıkar. Seçilen düğme vurgulanır. Fikrini değiştirebilirsin; son seçimin kaydedilir. Kayıttan sonra lab'daki kısa bildirim görünür.
- Asistan "Bu konuda rehberimizde bilgi bulamadım." dediğinde cevabın altında "Temsilciye bağlan" düğmesi çıkar. Basınca konuşma temsilci kuyruğuna alınır ve düğmenin yerinde "Temsilci kuyruğundasın" yazar.
- Bir konuşma sadece bir kez aktarılır. İkinci istek de başarılı olur ama yeni kayıt açmaz.
- Gerçek bir çağrı merkezi ve sahte temsilci cevabı yoktur; aktarma sadece kayıt olarak tutulur.

## Bittiğini nasıl anlayacaksın
1. "Kartımı kaybettim, ne yapmalıyım?" diye sor. Cevabın altında "Faydalı mı?" satırı ve saat görünür. "Evet"e bas, sonra "Hayır"a bas: "Hayır" vurgulanır ve kayıtta "Hayır" durur.
2. "Bitcoin alıp satabilir miyim?" diye sor. "Bulamadım" cevabının altında "Temsilciye bağlan" çıkar. Basınca "Temsilci kuyruğundasın" yazar ve konuşma `bireysel` kuyruğunda kayıtlı olur.
3. Aynı konuşma için aktarma iki kez gönderildiğinde tek kayıt oluşur.
4. Cevabı olan sorularda "Temsilciye bağlan" çıkmaz. Yarıda kalan ya da hatayla biten cevapta iki satır da çıkmaz.
5. Görünüm lab'la yan yana karşılaştırılır; testler ve derlemeler geçer.

## Yapılmayacaklar
- Temsilci ekranı ya da gerçek çağrı merkezi bağlantısı.
- Geri bildirime yorum yazma (lab'da yok).
- Konuşmalar ekranının kendisi (TASK-014). Bu task sadece verileri kaydeder; ekrandaki işareti soru 3'teki seçimine göre TASK-014 gösterir.
- Bilgi bankası servisinde, cevap akışında ve Docker kurulumunda değişiklik.

## Benim verdiğim kararlar
1. **"Tamamlanmış cevap" ne demek:** Sonuna kadar yazılmış cevap. Hatayla biten ya da yarıda kesilen cevaba geri bildirim verilemez. Bu durumda sistem sabit bir hata kodu döner.
2. **Aynı tura aynı anda iki geri bildirim gelirse** sonra işlenen geçerli olur. Turun başka hiçbir bilgisi değişmez.
3. **Aynı düğmeye yeniden basmak** bir şey yapmaz: istek ve bildirim gitmez.
4. **Seçim, kayıt başarılı olunca vurgulanır.** Kayıt sürerken iki düğme kısa süre basılamaz. Kayıt başarısız olursa satırın altında Türkçe hata mesajı çıkar ve eski seçim kalır.
5. **Cevabın saati,** cevabın bittiği andır.
6. **Bildirim metinleri lab'daki gibidir.** "Evet" için "Geri bildirim kaydedildi", "Hayır" için "Geri bildirim kaydedildi; bu tur incelemeye düştü". Ayrı bir inceleme listesi yoktur; "Hayır" denen turlar TASK-014'te Konuşmalar'da işaretli görünür. Aktarmada bildirim çıkmaz (lab'daki gibi).
7. **Aktarma konuşma düzeyindedir.** Aktarmadan sonra aynı konuşmadaki diğer "bulamadım" cevaplarında da "Temsilci kuyruğundasın" yazar. Ekrandaki önceki konuşmanın cevapları kendi konuşmalarına göre davranır.
8. **"Temsilciye bağlan" istek sürerken basılamaz.** Böylece çift tıklama ikinci bir istek göndermez.

## Çalışacak roller
1. planner
2. backend-coder: asistan servisi (kayıtlar ve iki yeni uç)
3. frontend-coder: Önizleme'deki iki satır ve bildirim
4. tester: otomatik testler (aynı anda gelen istekler ve tek kayıt kuralı dahil)
5. qa: sistemi yeni kodla mevcut komutla kurar; üç örnek senaryoyu dener; lab ile görünümü karşılaştırır.

Atlanan: devops. Docker, ayar dosyaları ve kurulum değişmiyor.

## Kararını bekleyen sorular
1. Temsilciye aktarılan konuşmada müşteri yeni soru sorabilsin mi?
   Neden önemli: Aktarmadan sonra ekranın ve asistanın nasıl davranacağını belirler.
   Seçenekler:
   A) Evet. Asistan cevaplamaya devam eder, "Temsilci kuyruğundasın" yazısı yerinde kalır (lab böyle).
   B) Hayır. Konuşma kilitlenir: giriş alanı kapanır, "Temsilci kuyruğundasın. Yeni soru için Yeni konuşma'ya bas." yazar; sunucu da o konuşmaya gelen soruyu reddeder. Gerçek bankalarda yaygın olan budur. Ancak burada temsilci hiç gelmeyeceği için konuşma sonsuza kadar kilitli kalır.
   Önerim: A. Lab böyle çalışıyor, gerçek temsilci olmadığı için kilit sadece kullanıcıyı yeni konuşma açmaya zorlar, aktarma kaydı yine durur.

2. Aktarma nerede tutulsun ve kuyruğun adı ne olsun?
   Neden önemli: Konuşmalar ekranı (TASK-014) bu kaydı okuyacak; tek kayıt kuralı da buna dayanır.
   Seçenekler:
   A) Asistan veritabanında ayrı bir aktarma kaydı. Kayıtta konuşma, hangi cevaptan sonra aktarıldığı, sebep ("cevap yok"), kuyruk ve saat durur; konuşma başına tek kayıt olur. Kuyruk adı ayardan gelir ve her zaman `bireysel` olur. Tek kanal Önizleme ve müşteri bireysel müşteridir.
   B) Aynı bilgiler konuşma kaydına eklenir, ayrı kayıt açılmaz. Daha az parça gerekir ama aktarma bilgisi konuşma bilgileriyle karışır ve ileride genişletmek zorlaşır.
   C) A gibi, ama kuyruk sorunun konusuna göre seçilir (kart, kredi…). Bunun için konu belirleyen ek bir kural ya da model gerekir; TASK bunu istemiyor.
   Önerim: A. Tek kayıt kuralını veritabanı garanti eder, kayıt sade kalır ve lab'daki `bireysel` aynen korunur.

3. Konuşmalar ekranında aktarma ve geri bildirim nasıl görünsün? (TASK-014'te yapılır.)
   Neden önemli: Kuyruktaki konuşmaları görmenin tek yolu bu ekran; TASK bu işaretin burada belirlenmesini istiyor.
   Seçenekler:
   A) Listede son turun durum çipinin yanına "Temsilcide" çipi. Detayda başlığın altına "Temsilciye aktarıldı · bireysel kuyruğu · 14:32 · sebep: cevap yok" satırı. Her turun bilgi satırına "geri bildirim: Evet/Hayır".
   B) Listeye ayrı bir "Aktarma" sütunu eklenir (lab tablosunda yok); detay A'daki gibi olur.
   C) Sadece detayda görünür, listede işaret olmaz.
   Önerim: A. Lab tablosunun sütunları değişmez ve aktarılan konuşmalar listede tek bakışta seçilir.

## Riskler
- Önizleme sayfası yenilenince ekrandaki konuşma kaybolur (bugünkü davranış). Geri bildirim ve aktarma kayıtları silinmez; TASK-014'te Konuşmalar'da görünür.
- Sistemde kimlik doğrulama olmadığı için bu uçları sisteme erişen herkes çağırabilir. Bugünkü uçlarda da durum aynı.

ONAY: VERİLDİ
