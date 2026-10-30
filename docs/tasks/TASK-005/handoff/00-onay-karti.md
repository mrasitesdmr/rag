TASK: TASK-005
# Onay kartı: Rehberin ilk içe alınması (bilgi bankası)

## Ne yapılacak
- Bilgi bankasının temeli kurulur: tek çalışma alanı (42, "Raşit Bankası · Dijital Asistan"), bilgi kaynağı, kaynağın sürümleri, parçalar ve içe alma işleri. Çalışma alanının embedding modeli `bge-m3`, 1024 boyuttur.
- Rehberin 2026.10 PDF'i gerçek içe alma hattından geçer:
  - PDF okunur.
  - Başlıklarına göre kısımlara ayrılır.
  - Parçalara bölünür.
  - Her parça `bge-m3` ile sayıya (vektöre) çevrilip veritabanına yazılır.
  - Hiçbir parça elle ya da hazır veriyle yazılmaz.
- İçe alma arka planda yürür. İşin hangi aşamada olduğu (sıraya alındı, okunuyor, parçalanıyor, embed ediliyor, yazılıyor, bitti ya da başarısız), ilerlemesi, parça sayısı ve zamanları bir adresten sorgulanır.
- Servis kapanıp açıldığında ikinci bir kaynak ya da tekrar eden parça oluşmaz. Rehber kaynağının numarası 10001'dir.
- Yüklenen PDF ve özellikleri saklanır: dosya adı, boyutu, sayfa sayısı ve bölüm sayısı. Bilgi kaynakları ekranındaki "PDF'i aç" (TASK-007) bunu kullanacak.
- Ekran yok. Sonuç veritabanında, servis kayıtlarında ve durum adresinde görünür.

## Bittiğini nasıl anlayacaksın
- Docker'daki sistemi veriler silinmiş hâlde ilk kez açarsın (Ollama açık). Birkaç saniye içinde `curl localhost:8080/api/workspaces/42/ingestion-jobs/1` iş için "DONE" der. Raporda şunları görürsün:
  - parça sayısı,
  - ortalama token,
  - işin süresi,
  - kaynağın 10001 numarası ve 2026.10 sürümü,
  - her parçanın 1024 boyutlu vektörü olduğu.
- Sistemi yeniden başlatırsın. Kaynak sayısı 1, parça sayısı aynı kalır, yeni iş açılmaz.
- Ollama kapalıyken ilk açılışta iş "embed ediliyor" aşamasında başarısız olur ve "embedding modeline ulaşılamadı" kodunu taşır. Hiç parça yazılmamıştır. Ollama'yı açıp servisi yeniden başlatınca yeni bir iş rehberi içe alır; kaynak yine 10001 olarak tek kalır.
- qa rastgele 5 parça seçer, sayfa numarasını ve başlığını PDF'te açıp elle karşılaştırır.
- Servis kayıtlarında parça metni geçmez. Sadece iş numarası, aşama, parça sayısı ve süre geçer.
- Otomatik testler geçer: parçalama kuralları, aşama geçişleri ve hata durumları.

## Yapılmayacaklar
- Vektör arama (TASK-006).
- Bilgi kaynakları ekranı; kaynak, sürüm ve parça okuma adresleri (TASK-007).
- Yeni sürüm yükleme ve eski parçaların yenisiyle değişmesi (TASK-015). 2026.11 bu task'ta girmez.
- Rehber dışında bir belgenin içe alınması. Okuyucu bu rehberin biçimine göre yazılır (bkz. Riskler).
- Ürün dokümanındaki ve ekip rehberlerindeki eski lab bilgilerinin ("31 sayfa, 61 parça") güncellenmesi.

## Benim verdiğim kararlar
- **Kapak ve içindekiler parçalanmaz.** İçerik 1. bölümün başladığı 5. sayfadan başlar. İçindekiler satırları aramada gereksiz sonuç olarak çıkmasın diye.
- **Başlıklar, PDF'in yan menüsündeki yer imlerinden tanınır.** Bunlar içindekilerle birebir aynıdır. Böylece "1 Kasım 2026 tarihinden…" gibi sayıyla başlayan bir cümle başlık sanılmaz.
- **Parça başlıkları:**
  - Alt başlığın kendisi, ör. "2.16 Kayıp ya da çalıntı bildirimi".
  - "Sık sorulanlar" kısımları için "2 Kartlar · Sık sorulanlar".
  - Alt başlığı olmayan sözlük bölümü için "11 Terimler sözlüğü".
- **Her sayfanın üst ve alt bilgisi parçalara girmez.** Üstteki "Sürüm 2026.10" ve alttaki "Sayfa 7 / 70" satırları atılır.
- **Parça satır ortasında kesilmez.** Bir tablo satırı her zaman tek parça içinde, bütün olarak kalır.
- **Sayfa numarası:** Bir parça iki sayfaya yayılırsa sayfa numarası, parçanın kendi metninin başladığı sayfadır.
- **Bölüm sayısı rehberin 11 ana bölümüdür.** Lab'daki gibi sayfa sayısı değildir.
- **Parçaların sıra numarası 0'dan başlar**, lab'daki gibi.
- **Sürüm ve kaynak adı servisin ayarından gelir:** "2026.10" ve "Raşit Bankası Ürün ve Hizmet Rehberi".
- **Rehber dosyası bulunamazsa** hiçbir şey yazılmaz. Servis açık kalır ve kayıtlara hata düşer. Sonraki açılışta yeniden bakılır.
- **Ollama kapalıyken açılırsa** iş başarısız olur, servis açık kalır. Rehber, Ollama açıldıktan sonra servis yeniden başlatılınca girer. Yeni kaynak oluşmaz, aynı kaynağa yeni bir iş açılır.
- **Başarısız işte durduğu aşama görünür**, ör. "embed ediliyordu". Lab'da bu bilgi yoktu.
- **PDF veritabanında saklanır.** Ayrı bir klasör ya da disk alanı gerekmez.
- **Embedding modeline istekler:**
  - Parçalar 96'şar gruplar hâlinde gider.
  - Modele aynı anda en fazla 2 istek gider.
  - Her isteğin 60 saniye zaman aşımı vardır.
  - Hata olursa 3 kez denenir, sonra iş başarısız olur.
- **Docker'da rehber klasörü servise salt okunur bağlanır.** PDF'in kopyası imaja girmez.

## Çalışacak roller
1. planner.
2. backend-coder: bilgi bankası, içe alma hattı, durum adresi. Kendi bilgisayarında gerçek PDF ve gerçek `bge-m3` ile bir kez çalıştırıp sayıları raporlar.
3. tester: otomatik testler.
4. devops: Docker'da rehber klasörünün bağlanması. İlk açılış, yeniden başlatma ve Ollama kapalıyken açılış denemeleri. Kurulum notları.
5. qa: rastgele parçaların PDF ile karşılaştırılması, kayıtlarda parça metni olmadığı, kabul kriterleri.

Atlanan: frontend-coder (bu task'ta ekran yok).

## Kararını bekleyen sorular
1. Rehber bilgi bankasına ilk kez nasıl girsin?
   Neden önemli: Lab'ın sol yarısında "kaynak ekle" ekranı yok. Rehberin ilk girişi ya kendiliğinden olacak ya da biri bir komut çalıştıracak.
   Seçenekler:
   A) Servis açılırken kendiliğinden. Bilgi bankasında rehberin tamamlanmış bir sürümü yoksa, ayarda yolu yazılı PDF'ten (2026.10) bir iş başlar. Varsa hiçbir şey yapılmaz.
   B) Ekransız bir yönetim adresi: lab'ın sağ yarısındaki "kaynak ekle" adresi. Sistem kurulunca bir kez komut satırından PDF gönderilerek çağrılır. Aynı adla ikinci çağrı reddedilir.
   C) İkisi birden.
   Önerim: A. Senaryodaki "sistem ilk kez açılır, rehber içe alınır" adımı elle bir iş gerektirmez. Ekranın kullanmayacağı bir yazma adresi de açık kalmaz. Yeniden açılışta tekrar girmemesi (kural 8) aynı kontrolle sağlanır.

2. Servis bir içe alma işi sürerken kapanırsa, açılışta o iş ne olsun?
   Neden önemli: İş arka planda yürüyor. Kapanmada yarıda kalan iş, ele alınmazsa sonsuza kadar "embed ediliyor" görünür. İki seçenekte de yarım parça yazılmaz, çünkü parçalar en sonda tek seferde yazılıyor.
   Seçenekler:
   A) İş "başarısız" işaretlenir: "servis kapandı" kodu ve kaldığı aşamayla. Rehberin ilk girişiyse açılışta yeni bir iş rehberi yeniden içe alır (1. soruda A seçilirse).
   B) İş aynı numarayla baştan yeniden başlar; saklanan PDF'ten tekrar okunur.
   Önerim: A. İşlerin geçmişi gerçeği gösterir: hangi iş yarıda kaldı, hangisi bitti. B'de servisi her açılışta çökerten bir dosya (ör. çok büyük bir PDF) her açılışta yeniden denenir ve sistem hiç düzelmez. Rehberin kendisi A'da da kendiliğinden yeniden girer.

3. Projeye şu iki hazır kütüphaneyi ekleyelim mi?
   - **Apache PDFBox 3.0.6:** PDF'ten metni, sayfa numaralarını ve yer imlerini okur. Rehberin kontrol komutu (TASK-004) aynı sürümle aynı ayarı kullanıyor. Bilgi bankası metni kontrolün gördüğü gibi okur.
   - **Hibernate'in vektör eklentisi:** Parçaların 1024 sayılık vektörünü veritabanına yazmak için. Veritabanı katmanının resmî eklentisidir, sürümünü Spring Boot belirler.
   Neden önemli: Onayın olmadan yeni kütüphane eklenmiyor. İkisi de bu task'ın temel parçası.
   Seçenekler:
   A) İkisi de eklensin.
   B) Sadece PDFBox. Vektörler elle yazılan veritabanı komutlarıyla kaydedilir. Daha fazla elle kod olur; TASK-006'daki arama da aynı yolu izlemek zorunda kalır.
   Önerim: A. PDFBox'ın yerine geçecek, Türkçe metni doğrulanmış başka bir okuyucu yok. Vektör eklentisi en az kodla doğru tipte yazar.

## Riskler
- **Okuyucu bu rehberin biçimine bağlı.** Başlıklar PDF'in yer imlerinden, üst ve alt bilgi de rehberin kendi kalıbından tanınıyor. Rehberin üretimi değişir de yer imleri kaybolursa içe alma "bölüm bulunamadı" ile durur. Başka biçimde bir PDF'te üst ve alt bilgi parçalara karışabilir. Bugün rehberin kontrol komutu yer imlerini koruyor.
- **Sayfa numarası yaklaşıktır.** İki sayfaya yayılan bir parçada gösterilen sayfa, parçanın başladığı sayfadır. Asistan ileride "s. 7" dediğinde bilginin bir kısmı 8. sayfada olabilir.
- **Docker'da rehber dosyası repodaki klasörden bağlanıyor.** Compose repo dışından ya da klasör taşınarak çalıştırılırsa dosya bulunamaz. Rehber girmez; servis açık kalır ve kayıtlarda hata görünür.

ONAY: VERİLDİ
