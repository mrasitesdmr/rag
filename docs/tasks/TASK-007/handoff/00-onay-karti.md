TASK: TASK-007
# Onay kartı: Bilgi kaynakları ekranı

## Ne yapılacak
- Bilgi kaynakları listesi gerçek veriyle çalışır. Her belge için ad, dosya, sayfa, sürüm, parça sayısı, durum ve son senkron görünür. Altında çalışma alanı ve embedding modeli bilgisi durur; bu değerler sistemden gelir.
- "Belge ekle" düğmesi ve penceresi gelir. PDF'i seçip ad ve sürüm yazarsın, "Yükle ve işle"ye basarsın. Pencere kapanır, belge listede işlenme aşamasıyla görünür, iş bitince "Hazır" olur.
- İlk işlenmesi başarısız olan belgenin satırında "Yeniden dene" düğmesi çıkar. Hiç belge yoksa boş durum görünür.
- Belgeye tıklayınca detay ekranı açılır: dosya bilgileri, "PDF'i aç", özet kutuları, içe alma işleri ve parçalar. Parçalarda arama yapılır, parçalar 12'şer gösterilir. Bir parçaya tıklayınca tam metni ve embedding'in ilk değerleri bir pencerede açılır.
- Süren bir iş ekranda kendiliğinden takip edilir. Sayfayı yenilesen de takip sürer.

## Bittiğini nasıl anlayacaksın
1. Verileri silip sistemi açarsın: boş durum görünür.
2. Rehberin 2026.10 PDF'ini yüklersin. Satır önce aşamaları gösterir, sonra "PDF · 70 sayfa · 2026.10 · 154 · Hazır" olur.
3. Yer imli başka bir PDF yüklersin: listede iki belge olur.
4. Yer imi olmayan bir PDF seçersin: pencerede belgeyi nasıl düzelteceğin yazar, listeye satır eklenmez. Aynı adla ikinci belgeyi yüklemeye çalışırsın: pencerede Türkçe hata görürsün.
5. Ollama kapalıyken yüklediğin belge "Başarısız" ve "Yeniden dene" gösterir. Ollama'yı açıp düğmeye basarsın, belge "Hazır" olur.
6. Detayda içe alma işi "DONE" görünür. Filtreye "asgari ödeme" yazarsın, sadece eşleşen parçalar gelir. 2. sayfada #12-#23 numaralı parçalar görünür.
7. Bir parçaya tıklarsın: metin ve embedding değerleri görünür. Esc ile kapatırsın, odak tıkladığın satıra döner.
8. Ekranlar lab'ın sol yarısıyla yan yana konduğunda aynı görünür. Aşağıdaki kararlardaki farklar bunun dışındadır.
9. Otomatik testler, uçtan uca test ve derleme geçer.

## Yapılmayacaklar
- Yeni sürüm yükleme, "Yeni sürüm yükle" düğmesi ve ilerleme kartı (bu projede yok). Kaynak silme.
- Parça penceresinin cevaptaki alıntılardan açılması (TASK-008). Pencere buna hazır yazılır.
- Filtrenin ve sayfa numarasının adres çubuğunda tutulması. Sayfa yenilenince filtre sıfırlanır.

## Benim verdiğim kararlar
Lab'dan farklı olan ya da TASK'ın söylemediği noktalar:
1. **Durum sütunu:** İş sürüyorsa aşama adı lab'daki gibi İngilizce ve sarı görünür (QUEUED, PARSING, EMBEDDING …). Bittiyse yeşil "Hazır" yazar. İlk işlenme başarısızsa kırmızı "Başarısız" yazar; altında kısa sebep ve "Yeniden dene" düğmesi olur. Lab'da başarısız durum yoktu.
2. **Henüz işlenmemiş belge:** Dosya adı ve sürüm son yüklemeden gelir. Sayfa, parça ve son senkron "–" görünür. Detayda ilk iki kutu "–" olur, parça tablosunda "Eşleşen parça yok." yazar.
3. **"Sayfa / bölüm" kutusu:** Bölüm, en üst düzey başlık (yer imi) sayısıdır. Rehberde "70 / 11" görünür.
4. **"Parça boyutu" ve "Örtüşme" kutuları** sistemin bugünkü ayarını gösterir: 1.600-2.000 ve 200 karakter.
5. **Filtre:** Büyük/küçük harf (Türkçe İ/ı dahil) ve fazla boşluk fark etmez. Noktalama fark eder: "asgari-ödeme" yazısı "asgari ödeme" aramasında çıkmaz. Lab noktalamayı yok sayıyordu. Arama, yazmayı bıraktıktan 0,3 saniye sonra yapılır; filtre değişince 1. sayfaya dönülür.
6. **Belge ekle penceresi:** Görünümü lab'ın "Yeni sürüm yükle" penceresi gibidir: dosya kutusu, "Kaynak adı", "Sürüm etiketi", "Vazgeç" ve "Yükle ve işle". Açılınca "Dosya seç" düğmesine odaklanır. Esc, ×, "Vazgeç" ya da pencerenin dışına tıklamak pencereyi kapatır; odak "Belge ekle"ye döner. Yükleme sürerken (birkaç saniye) pencere kapanmaz. Hata olursa pencere açık kalır; seçtiğin dosya ve yazdıkların silinmez.
7. **Kurallar göndermeden önce de kontrol edilir:** dosya var mı, boş mu, 20 MB'ı geçiyor mu, PDF mi, sürüm yazılmış mı. Son söz sistemindir.
8. **"PDF'i aç":** Belgenin işlenmiş sürümünün PDF'ini tarayıcının PDF görüntüleyicisinde yeni sekmede açar. Belge henüz işlenmediyse son yüklenen dosyayı açar.
9. **Listeler:** İşler tablosu en yeni 20 işi gösterir. Belge listesi 20'yi geçerse altında "Önceki / Sonraki" çıkar; lab'da bu düğme yoktu.
10. **Takip:** Süren iş saniyede bir sorulur. Ekrandan çıkınca sorma durur.
11. **Parça penceresi:** Embedding'in ilk 8 değeri gösterilir (lab'daki gibi). Bilgi şeridinde lab'daki "rehber 2026.10" yerine TASK'taki gibi "kaynak: belge adı · sürüm" yazar.
12. **Tarih ve saat** Türkiye saatiyle gösterilir ("29.10.2026 16:15").

## Ekrandaki metinler (önerim)
- **Boş durum:** "Henüz belge yok" başlığı, altında "Asistan, cevap verirken buraya eklediğin belgelerde arar. İlk belgeni ekleyerek başla." ve "Belge ekle" düğmesi.
- **Pencere açıklaması:** "Asistanın cevap verirken arayacağı PDF'i seç. Belgenin yer imleri (başlıkları) olmalı; parçalar bu başlıklarla adlandırılır. En fazla 20 MB."
- **Pencere dosya kutusu:** "Dosya seçilmedi" · "PDF · en fazla 20 MB" · düğme "Dosya seç". Yükleme sürerken düğme "Yükleniyor…" olur.
- **Ad ipucu:** "Boş bırakırsan dosya adı kullanılır."
- **Yer imi yok:** "PDF'te yer imi (başlık) yok. Word'de başlıkları "Başlık 1", "Başlık 2" stilleriyle yaz; "Farklı kaydet → PDF → Seçenekler"de "Şunu kullanarak yer işaretleri oluştur: Başlıklar"ı işaretleyip yeniden kaydet."
- **Metin yok:** "PDF'te okunabilir metin yok; taranmış bir görüntü olabilir. Metni seçilebilen bir PDF yükle (ör. Word'de "Farklı kaydet → PDF")."
- **Açılamıyor:** "PDF açılamadı; dosya bozuk ya da şifreli olabilir. Şifresini kaldırıp yeniden kaydet ya da PDF'i yeniden oluştur."
- **Aynı ad:** "Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz."
- **Dosya sorunları:** "Bir PDF dosyası seç." · "Seçtiğin dosya boş. Başka bir PDF seç." · "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç." · "Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil." · "Dosya adı en fazla 255 karakter olabilir." · (sistem dosyayı boş ya da adını uzun bulursa) "Seçtiğin dosya boş ya da adı 255 karakterden uzun. Başka bir PDF seç."
- **Ad ve sürüm:** "Kaynak adı 2-80 karakter olmalı." · "Sürüm etiketi zorunlu; en fazla 40 karakter."
- **Başarısız sebebi:** "Embedding modeline ulaşılamadı. Model sunucusu açılınca yeniden dene." · "Servis iş sürerken kapandı. Yeniden dene."
- **Yeniden dene sorunları:** "Bu belgenin işi zaten sürüyor." · "Bu belge zaten işlenmiş. Listeyi yenile."
- **Model uyuşmazlığı:** "Sistemin embedding modeli çalışma alanınınkiyle aynı değil; belge yüklenemiyor. Sistem ayarını kontrol et."
- **Embedding boyutu:** "Embedding modeli beklenmeyen boyutta vektör döndü. Model ayarını kontrol et."
- **Bulunamadı:** "Kaynak bulunamadı." ve "Bilgi kaynaklarına dön" bağlantısı · "Parça bulunamadı." · "İçe alma işi bulunamadı. Listeyi yenile." · "Çalışma alanı bulunamadı."
- **Genel:** "Girdiğin bilgileri kontrol et." · "Sunucuya ulaşılamıyor. Bağlantını kontrol et." · "Beklenmeyen bir hata oluştu. Tekrar dene." · (ekran çökerse) "Beklenmeyen bir hata oluştu. Sayfayı yenile."
- **Yüklenirken:** "Yükleniyor…"

## Çalışacak roller
1. planner
2. backend-coder: yeni okuma adresleri (çalışma alanı, tek belge, PDF, işler, parçalar, tek parça) ve listenin ekran alanları
3. frontend-coder: iki ekran, pencereler, takip
4. tester: otomatik testler ve uçtan uca test
5. devops: Docker'ı yeniden kurar; uçtan uca testi, PDF açmayı ve Ollama kapalı senaryosunu dener
6. qa: lab'la yan yana karşılaştırma, kabul kriterleri

Atlanan rol yok. Asistan servisi değişmiyor.

## Kararını bekleyen sorular
1. Ekranın sunucuyla konuşması ve testleri için aşağıdaki hazır kütüphaneleri kuralım mı?
   - **TanStack Query:** Sunucudan gelen veriyi önbellekte tutar, süren işi belli aralıkla yeniden sorar.
   - **react-error-boundary:** Ekranda beklenmeyen bir hata olursa beyaz sayfa yerine hata mesajı gösterir.
   - **JetBrains Mono yazı tipi:** Lab embedding değerlerini bu yazı tipiyle gösteriyor. Yazı tipi kendi sunucumuzdan gelir.
   - **MSW (sadece testte):** Testlerde sunucuyu taklit eder.

   Neden önemli: Onayın olmadan yeni kütüphane eklenmiyor. Dördü de proje kurallarında "ilk sunucu çağrısını yapan task'ta kurulur" diye listeli; bu task o task.
   Seçenekler: A) Dördü de kurulsun. B) Hata ekranı kütüphanesi olmasın; beklenmeyen hata beyaz sayfa bırakabilir.
   Önerim: A, çünkü dördü de proje kurallarında öngörülmüş ve ekranın temel parçası.

## Riskler
- Filtrede Türkçe harflerin (ş, ğ, ö, ü, ç) eşleşmesi veritabanının dil ayarına bağlı. Bugünkü kurulum bunu destekliyor; testler aynı kurulumla doğrular.
- "Parça boyutu" ve "Örtüşme" bugünkü ayarı gösterir. Ayar değişir de belgeler yeniden yüklenmezse eski belgeler için yanlış görünür.
- Uçtan uca test her çalıştığında "E2E Rehber …" adlı bir deneme belgesi bırakır. Kaynak silme olmadığı için bu belgeler ancak verileri sıfırlayarak silinir.

ONAY: VERİLDİ
