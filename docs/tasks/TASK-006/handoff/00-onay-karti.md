TASK: TASK-006
# Onay kartı: Vektör arama ucu

## Ne yapılacak
- Bilgi bankasına bir arama adresi gelir. Bir soru metni ve kaç parça istendiği gönderilir; anlamca en yakın parçalar döner.
- Arama, yüklenen her belgenin sadece son tamamlanan sürümünde yapılır. Hiç tamamlanmamış belge aramaya girmez. Belgenin yeni sürümü işlenirken eski sürümünden sonuç gelmeye devam eder.
- Her sonuç şunları taşır: parçanın numarası ve belgedeki sırası, sayfası, başlığı, metni, belgenin numarası, adı ve sürümü, soruya uzaklığı.
- Sonuçlar en yakından uzağa sıralanır; uzaklık eşitse küçük numaralı parça önce gelir.
- Aramalar kaydedilmez. Kayıtlara soru metni yazılmaz; sadece uzunluğu, istenen parça sayısı, sonuç sayısı ve süre yazılır.
- Ekran yok. Asistan bu adresi TASK-008'de kullanacak.

## Bittiğini nasıl anlayacaksın
- Rehber 2026.10 yüklüyken "Kartımı kaybettim, ne yapmalıyım?" diye 5 parça istenir; ilk sonuçlar rehberin kayıp/çalıntı kart bölümündendir.
- Raporda rehberden 12 soru vardır. Her soru için doğru parçanın ilk 5'te olup olmadığı ve kaçıncı sırada geldiği gerçek modelle ölçülmüştür. (Bu, Değerlendirme ekranındaki ölçümün yerini tutmaz.)
- Parça sayısı 0 istenirse istek reddedilir ve hatanın parça sayısında olduğu söylenir.
- Bilgi bankası boşken arama boş liste döner.
- Rehberle birlikte başka bir belge yüklüyken sonuçlar iki belgeden de gelebilir; her sonuç hangi belgeden geldiğini gösterir.
- Raporda veritabanının hızlı arama yöntemini (index) kullandığı gösterilir.
- Otomatik testler geçer.

## Yapılmayacaklar
- Yeniden sıralama (TASK-009), takip sorusunu yeniden yazma (TASK-010).
- Anahtar kelime ve karma arama. İstekte arama türü seçilmez.
- Uzaklık sınırı: çok uzak parçalar elenmez.
- Arama ekranı, aramaların kaydı.

## Benim verdiğim kararlar
- **Cevabı olmayan soru:** Rehberde cevabı olmayan bir soruda da en yakın parçalar döner; eleme yapılmaz. "Bulamadım" kararını asistan verir (TASK-008). Lab da böyle çalışıyor.
- **Az parça:** Bilgi bankasında istenenden az parça varsa olanlar döner.
- **Parça sayısı zorunlu:** 1 ile 50 arasında olmalı; gönderilmezse istek reddedilir.
- **Sorgu metni:** Baştaki ve sondaki boşluklar silinir. Harf büyüklüğü ve Türkçe harfler olduğu gibi kalır. Sadece boşluktan oluşan sorgu reddedilir.
- **Model sunucusu kapalıysa:** Arama yaklaşık 3 saniye içinde (üç denemeden sonra) hata döner.
- **Bilgi bankası boşsa:** Model sunucusuna hiç gidilmez; sunucu kapalı olsa da boş liste döner.
- **Model uyuşmazlığı:** Ayardaki embedding modeli çalışma alanınınkiyle aynı değilse arama reddedilir (yüklemedeki gibi).
- **Arama ayarları lab'daki gibi:** Küçük bilgi bankasında da hızlı arama yöntemi kullanılır.
- **Ölçüm soruları:** Rehberin farklı bölümlerinden 12 soru (kayıp kart, yıllık ücret iadesi, asgari ödeme, vadeli hesabı bozma, kredi erken kapama, hafta sonu EFT, yanlış gönderim, mobil şifre, ATM'de kalan kart, şikâyet süresi, hafta sonu kuru, kiralık kasa ücreti). Doğru parça, arama yapılmadan önce rehberdeki cevap cümlesine bakılarak belirlenir. Cevap rehberde birden fazla yerde geçiyorsa (ör. sık sorulanlar kısmında) o parçalar da doğru sayılır.

## Çalışacak roller
1. planner.
2. backend-coder: arama adresi. Rehberi gerçek modelle yükleyip 12 soruyu ölçer, veritabanının index kullandığını gösterir.
3. tester: otomatik testler.
4. qa: ölçümden birkaç soruyu tekrarlar, kabul kriterlerini dener.

Atlanan: frontend-coder (ekran yok), devops (Docker ve kurulum değişmiyor).

## Kararını bekleyen sorular
1. Arama sorgusu en fazla kaç karakter olsun?
   Neden önemli: Daha uzun sorgu reddedilir; asistanın kabul ettiği bir müşteri sorusu aramada reddedilmemeli.
   Seçenekler: A) 1000 karakter. B) 500 karakter. C) 2000 karakter.
   Önerim: A. Proje kurallarındaki örnekte müşteri sorusunun sınırı 1000 karakter (kesin sınır TASK-008'de belirlenecek); takip sorusundan üretilen arama metni bundan kısadır. Model bu uzunluğu rahat işler. B uzun bir soruyu reddedebilir; C'nin pratikte bir faydası yok. TASK-008 farklı bir sınır seçerse bu değer ona göre güncellenir.

## Riskler
- **Model sunucusu askıda kalırsa** (kapalı değil ama cevap vermiyorsa) hata 3 dakikaya kadar gecikebilir; asistan bu sürede bekler.
- **Belge yüklenirken** arama birkaç saniye gecikebilir, çünkü ikisi aynı model sunucusunu kullanır.
- **Ölçüm küçüktür:** 12 soru genel kaliteyi kesin göstermez; asıl ölçüm Değerlendirme ekranında (TASK-016).

ONAY: VERİLDİ
