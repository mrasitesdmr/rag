TASK: TASK-027
# Onay kartı: Açılışta otomatik yüklemenin kaldırılması, belge ekleme ve genel PDF okuyucu

## Ne yapılacak
- Sistem açılınca bilgi bankası boş olur. Rehber artık kendiliğinden yüklenmez. Bunu yapan kod, ayarlar ve Docker'daki rehber klasörü bağlantısı kalkar.
- Belgeler sadece senin yüklemenle girer. "Belge ekle" adresine dosyayı, adını ve sürümünü gönderirsin. Belge uygunsa hemen bir iş numarası alırsın; belge arka planda bugünkü hattan geçer.
- Belge yüklenirken bir kez okunur. Açılamayan, metni olmayan (taranmış) ya da yer imi (başlık) olmayan PDF o anda reddedilir. Hiçbir şey kaydedilmez, ad boş kalır.
- Başarısız ilk içe almayı yeniden başlatan bir "yeniden dene" adresi gelir. Dosyayı yeniden göndermen gerekmez.
- Yüklenen belgeleri gösteren sade bir liste adresi gelir.
- Ekran yok. Pencere ve "Yeniden dene" düğmesi TASK-007'de gelir.

## Bittiğini nasıl anlayacaksın
- Docker'ı verileri silerek açarsın. Liste boştur, hiç iş yoktur.
- Rehberin 2026.10 PDF'ini sürümüyle yüklersin. Birkaç saniyede iş "bitti" olur. Parça sayısı TASK-005'teki gibi 154'tür, başlıklar aynıdır.
- Yer imleri olan başka bir PDF yüklersin. İş "bitti" olur; parçalar kendi başlıklarını ve sayfa numaralarını taşır. Sayfalardaki üst ve alt bilgi parçalara girmez.
- Yer imi olmayan bir PDF yüklersin: hemen reddedilir, listede yeni kayıt yoktur. Aynı adla düzeltilmiş belgeyi yükleyebilirsin.
- Sürüm vermeden, aynı adla ya da taranmış bir PDF'le yüklersin: hemen reddedilir.
- Model sunucusu (Ollama) kapalıyken yüklenen belgenin işi başarısız olur. Sunucu açılınca "yeniden dene" çalıştırılır; iş "bitti" olur, belge tek kalır.
- Sistemi yeniden başlatırsın: belge ve parça sayısı değişmez, yeni iş açılmaz.
- Raporda kabul edilen iki belge için parça sayısı, ortalama uzunluk ve süre; rehber için TASK-005 ile fark yazar.
- qa iki belgeden rastgele parçaları PDF'le elle karşılaştırır. Otomatik yüklemenin izi kalmadığı aramayla gösterilir.

## Yapılmayacaklar
- Bilgi kaynakları ekranı, "Belge ekle" penceresi, "Yeniden dene" düğmesi, hataların Türkçe metinleri (TASK-007).
- PDF dışındaki dosya türleri. Yer imi olmayan PDF'ten başlık tahmin etmek.
- Yeni sürüm yükleme (TASK-015). Kaynak silme: deneme yüklemeleri ancak verileri sıfırlayarak silinir.
- Vektör arama (TASK-006). Parça örtüşmesi ve sayfa numarası düzeltmesi (TASK-024).
- Eski rehber verisinin kendiliğinden silinmesi. Nasıl temizleneceği kurulum notunda yazar.

## Senin verdiğin kararlar
- Yer imi olmayan PDF reddedilir; sistem başlık tahmin etmez. Taranmış ve açılamayan PDF de yükleme anında reddedilir.
- Ad her durumda tekildir; aynı adla ikinci yükleme hep reddedilir. Başarısız ilk içe alma "yeniden dene" ile tekrarlanır.
- Sürüm etiketi zorunludur, 1-40 karakter.
- Sadece PDF kabul edilir.
- Çalışma alanı bugünkü gibi kurulumla gelir (42, "Raşit Bankası · Dijital Asistan", `bge-m3`).

## Benim verdiğim kararlar
- **Üst/alt bilgi:** Sayfaların en az yarısında ilk satır aynıysa üst bilgi sayılır ve atılır; son satır için de aynısı. Sayılar fark etmez ("Sayfa 7 / 70" ile "Sayfa 8 / 70" aynı sayılır). 3 sayfadan kısa belgede ayıklama yapılmaz.
- **Değişti (deneme sırasında bulundu):** Bu kontrol sayfanın başında ve sonunda ikişer kez yapılır, yani her kenarda en fazla iki satır atılır. Sebep: tarayıcıdan basılan PDF her sayfanın sonuna iki satır yazıyor (tarih + sayfa başlığı, adres + sayfa numarası). Tek satır kuralında tarih + başlık satırı parçaların çoğunda kalıyordu. Rehber bundan etkilenmiyor: yine 154 parça.
- **Yer imleri:** Her yer imi başlık olur, numaralı olsun olmasın. İlk yer iminden önceki sayfalar (kapak, içindekiler) atlanır. Rehberdeki "Sık sorulanlar" kısımları bugünkü gibi ayrı başlık alır.
- **Ret sırası:** Önce dosyanın açılıp açılmadığına, sonra metnine, sonra yer imlerine bakılır. Taranmış ve yer imsiz bir dosyada "metin yok" hatası döner.
- **Ad:** 2-80 karakter, baştaki ve sondaki boşluk silinir. Ad boşsa dosya adı uzantısız kullanılır; 80 karakterden uzunsa ilk 80 karakteri alınır. "Rehber" ile " REHBER " aynı ad sayılır; Türkçe "İ/i" ve "I/ı" eşleşmesi garanti değildir.
- **Dosya:** Boş olamaz, en fazla 20 MB'tır. Adı ".pdf" ile bitmeli ve içi gerçekten PDF olmalı.
- **Model uyuşmazlığı:** Ayardaki model çalışma alanınınkiyle uyuşmazsa yükleme ve yeniden dene hemen reddedilir.
- **Yeniden dene:** Belgenin son denemesindeki dosyayı, dosya adını ve sürümü kullanır. Belge tamamlanmışsa ya da işi sürüyorsa reddedilir.
- **Numaralar:** İlk belge yine 10001 numarasını alır, lab'daki gibi. Kodda bu numaraya bağlı bir şey kalmaz.
- **Kayıtlar:** Sadece iş numarası, dosya boyutu, sayfa ve parça sayısı ve süre görünür. Belge adı, dosya adı, sürüm ve parça metni görünmez.
- **Web üzerinden yükleme** de 20 MB'a kadar çalışır.
- **Deneme belgeleri:** RAG anlatım sayfasının tarayıcıdan basılmış iki PDF'i kullanılır: biri yer imli (kabul), biri yer imsiz (ret). Tarayıcı bunu yapamazsa senden yer imli bir PDF istenir.

## Çalışacak roller
1. planner.
2. backend-coder: belge ekleme, yeniden dene ve liste adresleri, genel PDF okuyucu, otomatik yüklemenin kaldırılması. Gerçek `bge-m3` ile belgeleri bir kez yükleyip sayıları raporlar.
3. tester: otomatik testler.
4. devops: Docker'dan rehber klasörü bağlantısının kaldırılması, web üzerinden yükleme sınırı, kurulum notları. Temiz açılış, yükleme, yeniden dene ve yeniden başlatma denemeleri.
5. qa: rastgele parçaların PDF'le karşılaştırılması, iz araması, kabul kriterleri.

Atlanan: frontend-coder (bu task'ta ekran yok).

## Riskler
- **Üst/alt bilgi tahmini yanılabilir.** Her sayfada tekrarlanan gerçek bir satır (ör. tablonun başlık satırı) atılabilir. Üç ve daha fazla satırlı üst/alt bilginin fazlası parçalarda kalır.
- **İlk yer iminden önceki sayfalarda** gerçek içerik varsa parçalara girmez.
- **Büyük belgede yükleme cevabı** birkaç saniye gecikebilir, çünkü belge ret kontrolü için o anda okunur.

ONAY: VERİLDİ
