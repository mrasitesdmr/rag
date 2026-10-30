TASK: TASK-003
# Onay kartı: Lokal modeller (dil modeli, embedding, reranker)

## Ne yapılacak
- Asistanın kullanacağı üç model bu bilgisayarda çalışır hâle gelecek:
  - cevabı yazan dil modeli,
  - metni sayıya çeviren embedding modeli,
  - bulunan parçaları soruya yakınlığına göre yeniden sıralayan reranker.
- Dil modeli ve embedding modeli Ollama'da çalışacak (adres 11434). Reranker bilgisayarın kendisinde llama-server ile çalışacak, ekran kartını kullanacak ve 8081'de cevap verecek.
- Her model için tek komutluk bir duman testi olacak:
  - embedding testi 1024 sayılık bir vektör döndürecek,
  - dil modeli testi kısa bir Türkçe cevap verecek,
  - reranker testi iki metne iki skor verecek ve soruyla ilgili metne daha yüksek skor verecek.
- Üç model birlikte açıkken ne kadar bellek kullandığı ölçülecek ve yazılacak.
- Kurulum, model indirme, başlatma ve durdurma, testler, bellek notları ve sorun giderme `ollama.md`'de anlatılacak.
- Servislerin modellere ulaşacağı adres ve model adları şimdiden ayarlara yazılacak. Servisler modelleri henüz kullanmayacak.

## Bittiğini nasıl anlayacaksın
- `ollama.md`'yi baştan sona izlersin; her komut çalışır ve belgede yazan sonucu verir.
- Embedding testi `1024` yazar.
- Dil modeli testi "Türkiye’nin başkenti hangi şehirdir?" sorusuna "Ankara" içeren tek cümlelik Türkçe bir cevap verir. Aynı komutu tekrar çalıştırınca aynı cevap gelir.
- Reranker testi iki skor ve `OK` yazar (ilgili metin daha yüksek skoru aldı).
- Sistemi tek komutla başlatırsın, reranker'ı da `ollama.md`'deki komutla ayrı bir terminalde açarsın. Reranker 8081'de, Ollama 11434'te cevap verir. Docker'daki servisler ikisine de ulaşabilir.
- Reranker'ı terminalde durdurunca (Ctrl+C) reranker testi "bağlanamadı" hatası verir. Docker'daki servisler sağlıklı kalır.
- Raporda üç modelin birlikte açıkken kullandığı bellek, takas alanının (swap) ne kadar arttığı ve her modelin ilk ve sonraki cevap süreleri yazar.

## Yapılmayacaklar
- Servislerin modelleri gerçekten kullanması:
  - bilgi bankası embedding'i TASK-005'te,
  - asistan dil modelini TASK-008'de,
  - reranker'ı TASK-009'da kullanmaya başlar.
- Modellerin ince ayarı ve farklı sıkıştırma seviyelerinin karşılaştırılması.
- Ekranda değişiklik.
- Diskte duran, bu projede kullanılmayan eski model kopyalarının silinmesi (bkz. Riskler).
- Reranker'ın bilgisayar açılınca kendiliğinden başlaması (arka plan servisi). Bu task'ta reranker elle başlatılır; kalıcı servis ayrı bir iş olarak önerilir.

## Benim verdiğim kararlar
- **Ollama bilgisayarın kendisinde çalışacak, Docker'da değil.** Docker bu Mac'te ekran kartını kullanamıyor. Dil modeli Docker'da çok yavaşlar, ayrıca Docker'a ayrılan ~7,75 GB bellek ona yetmez.
- **Ollama bilgisayar açılınca kendiliğinden başlayacak** (Homebrew servisi olarak). Model yüklü değilken çok az bellek kullanır. Durdurmak tek komut.
- **Dil modeli testi lab'daki ayarlarla yapılacak:**
  - rastgelelik kapalı (temperature 0), bu yüzden aynı soruya hep aynı cevap gelir,
  - 8192 token'lık bağlam,
  - "düşünme" çıktısı kapalı; böylece cevabın başına modelin iç düşünme metni karışmaz.
- **Reranker'ın skoru 0 ile 1 arasında değil.** Ham bir değer; sadece sıralamak için kullanılır (lab'daki not). Test bu yüzden sadece hangi metnin önde olduğuna bakar.
- **Reranker uzun parçaları da işleyebilecek şekilde başlatılacak** (yaklaşık 1.500 token'lık bir metinle denenecek). Varsayılan ayarla uzun parçada hata veriyor.
- **Model sürümleri kaydedilecek.** Ollama'daki modellerin kimlik numarası ve reranker dosyasının adı `ollama.md`'ye yazılacak. Böylece ileride hangi modelle ölçüldüğü bilinir.
- **Reranker sadece bu bilgisayardan erişilebilir olacak.** Ağdaki başka cihazlar ona bağlanamaz. Docker'daki servisler yine de ulaşabilir; devops bunu deneyerek doğrulayacak.
- **Bellek bu adımda ölçülemiyor;** devops ölçecek ve bu bir kabul kriteri. Tahminim, üç model birlikteyken 9-11 GB. Bu 16 GB makinede, Docker'daki sistemle birlikte takas alanına düşmeye yakın.

## Çalışacak roller
1. planner.
2. devops: modelleri indirir ve başlatır, servislerin model adreslerini sistem ayarlarına yazar, `ollama.md`'yi yazar, testleri ve bellek ölçümünü çalıştırır.
3. qa: `ollama.md`'yi baştan izleyip her komutu dener ve kabul kriterlerini kontrol eder.

Atlananlar:
- backend-coder ve frontend-coder: bu task'ta servislerin ve sitenin kodu değişmiyor.
- tester: yazılacak kod ve otomatik test yok. Testler tek komutluk terminal komutları; devops çalıştırır.

## Kararını bekleyen sorular
1. Lab'daki üç modelle mi devam edelim?
   Neden önemli: Her model ayrı indirme ve bellek demek. Seçim sonraki bütün task'ları (cevap kalitesi, hız, bellek) etkiler.
   - Dil modeli `qwen3.5:9b` ve embedding modeli `bge-m3` zaten indirilmiş.
   - Reranker `bge-reranker-v2-m3` indirilmemiş.
   Seçenekler:
   A) Lab'daki üç model. Reranker'ın 8 bit sıkıştırılmış hâli indirilir (~0,6 GB).
   B) Lab'daki üç model. Reranker'ın sıkıştırılmamış hâli indirilir (~1,1 GB). Sıralama biraz daha hassas olabilir, bellek iki kat.
   C) Başka bir model istiyorsan adını yaz.
   Önerim: A. Sıralama kalitesi sıkıştırılmamış hâline çok yakın, bellek yarısı. 16 GB makinede her yarım GB önemli.

2. Reranker nerede çalışsın?
   Neden önemli: Bu seçim hızı ve sistemi nasıl açıp kapadığını belirler. Görevdeki senaryo, reranker'ın sistemle birlikte tek komutla açılmasını bekliyor.
   Seçenekler:
   A) Docker'da, sistemin bir parçası olarak. llama.cpp'nin resmi Docker imajı kullanılır; bu, reranker'ı çalıştıran hazır sunucu programıdır. Sistemle birlikte açılır ve kapanır, tek komutla durdurulur (lab'daki "reranker konteynerini durdur" gibi). Model ilk açılışta kendini indirir. Ekran kartını kullanamaz, işlemcide çalışır; 20 parçayı sıralamak birkaç saniye sürebilir.
   B) Bilgisayarın kendisinde. Zaten kurulu olan llama-server ekran kartını kullanır, sıralama hızlı olur. Ama sistemle birlikte açılmaz; ayrı bir terminalde komutla başlatılıp açık tutulur.
   Önerim: A. Hem senaryo hem lab böyle. Model küçük, işlemcide de çalışır. devops sıralama süresini ölçecek; yavaş çıkarsa B'ye geçmek ayrı bir iş olarak önerilir.

3. Ollama'daki modeller son kullanımdan sonra bellekte ne kadar kalsın?
   Neden önemli: Bellekte kalırsa sorular hemen cevaplanır ama bilgisayarın geri kalanı için bellek azalır. Lab'ın notunda bu makinede üç model birlikteyken takas alanına düşüldüğü yazıyor. Bellekten çıkmışsa ilk soru birkaç saniye gecikir.
   Seçenekler:
   A) 5 dakika (Ollama'nın varsayılanı, ek ayar yok). Kullanırken üçü birlikte bellekte durur, boşta bellek geri verilir.
   B) 30 dakika.
   C) Ollama kapatılana kadar sürekli.
   Önerim: A. Demo sırasında modeller zaten açık kalır, boşta 16 GB makine rahatlar. Reranker hangi seçenek olursa olsun açık kaldıkça bellekte durur (yaklaşık 1 GB).

## Riskler
- Bu 16 GB makinede, Docker'daki sistem açıkken üç model birlikte belleği doldurabilir. O zaman bilgisayar yavaşlar ya da Ollama modellerden birini bellekten çıkarıp her soruda yeniden yükler. Ölçüm bunu gösterecek; çözümü gerekirse ayrı bir task olur.
- Reranker ayrı bir terminalde açık kalmalı. Terminal kapanırsa ya da bilgisayar yeniden başlarsa reranker durur ve yeniden başlatılması gerekir. Kalıcı servis ayrı bir iş olarak önerilir.
- Diskte bu projede kullanılmayan iki eski model kopyası var (`qwen3-vl:8b`, toplam ~12 GB). Bu task onlara dokunmaz; silmek istersen ayrı bir iş.

## Kullanıcı kararları (2026-10-29)
1. Modeller: **A**. Lab'daki üç model: `qwen3.5:9b`, `bge-m3` ve reranker `bge-reranker-v2-m3`'ün 8 bit sıkıştırılmış hâli.
2. Reranker'ın yeri: **B**. Bilgisayarın kendisinde, kurulu llama-server ile, ekran kartını kullanarak.
   - Bunun sonucu olarak görevdeki "Compose'u başlatınca reranker 8081'de" senaryosu değişiyor: reranker Compose ile değil, `ollama.md`'deki komutla açılır.
3. Bellekte kalma süresi: **A**. 5 dakika (Ollama'nın varsayılanı).

ONAY: VERİLDİ
