TASK: TASK-002
# Onay kartı: Proje iskeleti: iki servis, web kabuğu ve veritabanı

## Ne yapılacak
- Asistan servisi ve bilgi bankası servisi ilk kez kurulacak. İkisi birbirinden bağımsız çalışacak, ayrı ayrı derlenecek ve kod paylaşmayacak. Henüz hiçbir iş yapmayacaklar; çalıştıklarını gösteren bir sağlık ucu olacak. Olmayan bir adrese gelen isteğe ikisi de "bulunamadı" hatasını sabit bir kodla dönecek.
- Tek veritabanı sunucusunda iki ayrı veritabanı açılacak: biri asistan servisinin, biri bilgi bankası servisinin. Her servisin kendi kullanıcısı olacak ve kimse diğerinin veritabanına giremeyecek. Tablo henüz yok.
- Yönetim panelinin iskeleti kurulacak. Lab'ın sol yarısındaki gibi görünecek: üstte Raşit Bankası logosu ve "Canlı yapılandırma" rozeti, altında Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme menüsü. Her ekranda sadece ekranın yolu (Dijital Asistan › …), başlığı ve lab'daki açıklama metni olacak.
- Ekran adresleri `/onizleme`, `/konusmalar`, `/kaynaklar` ve `/degerlendirme` olacak. Ana adres Önizleme'yi açacak. Adres doğrudan açıldığında ya da sayfa yenilendiğinde aynı ekran gelecek.
- Hepsi tek komutla birlikte ayağa kalkacak; site http://localhost:3000'de açılacak.

## Bittiğini nasıl anlayacaksın
- `docker compose -f deployment/docker-compose.yml up --build -d` çalıştırınca dört parça (veritabanı, iki servis, site) birkaç dakika içinde "sağlıklı" görünür.
- http://localhost:3000 açılınca adres `/onizleme` olur; "Asistan önizleme" başlığı ve lab'daki açıklama görünür.
- Menüden Bilgi kaynakları'na tıklayınca adres `/kaynaklar` olur, başlık "Bilgi kaynakları" ve açıklama görünür, menüde o öğe seçili olur. Sayfayı yenilersin, aynı ekranda kalırsın.
- Tarayıcıda http://localhost:3000/api/assistant/yok adresini açınca "bulunamadı" hatası ve `NOT_FOUND` kodu görünür.
- Üst barda isim ya da baş harf, menüde Konuşmalar'ın yanında sayı görünmez.
- Asistan servisinin kullanıcısıyla bilgi bankasının veritabanına bağlanmaya çalışınca "izin yok" hatası alınır, tersi de aynı (devops raporunda komutuyla yazar).
- İki servisin derleme ve testleri, sitenin derleme ve kod denetimi hatasız geçer.
- Lab'ın sol yarısı ile site yan yana konunca aynı görünür (qa ekran görüntüleriyle karşılaştırır).

## Yapılmayacaklar
- Tablolar, gerçek uçlar, lokal modeller (TASK-003 ve sonrası).
- Ekranların içerikleri: sohbet, listeler, tablolar, düğmeler.
- Konuşma ve kaynak detay ekranları (`/konusmalar/…`, `/kaynaklar/…`). Bu task'ta bu adresler "Sayfa bulunamadı" gösterir.
- Kimlik doğrulama ve üst bardaki kullanıcı bilgisi (TASK-017). Konuşmalar sayacı (TASK-014).
- Veritabanını tarayıcıdan görmek için arayüz (pgAdmin). Gerektiğinde ayrı bir task'ta eklenir.
- Cevap akışı için sunucu ayarları (TASK-008) ve dosya yükleme sınırı (TASK-015).

## Benim verdiğim kararlar
- **Lab'daki sahte tarayıcı çerçevesi çizilmeyecek.** Lab, iki yarıyı yan yana gösterebilmek için üç noktalı bir pencere ve "asistan.rasitbank.intra" yazan bir adres çubuğu çiziyor. Site zaten gerçek bir tarayıcıda açıldığı için bu çerçeve gelmeyecek; site pencerenin tamamını kullanacak. Karşılaştırmada lab'ın çerçeve içindeki kısmı ölçü alınacak.
- **Menü lab'daki gibi üst barın altında yatay duracak.** Görev metni "yan menü" diyor ama lab'da menü yatay bir şerit. Seçili öğenin altında bordo çizgi olacak.
- **Üst bar ve menü sabit kalacak, sadece sayfa içeriği kayacak** (lab'daki gibi).
- **Dar ekranda** (telefon genişliği) menü yana kayacak. Ekran 640 pikselden darsa "Canlı yapılandırma" rozeti gizlenecek (lab'daki gibi).
- **Tarayıcı sekmesinin başlığı ekrana göre değişecek**, ör. "Bilgi kaynakları · Raşit Bankası Dijital Asistan Yönetimi". Sekme simgesi Raşit Bankası logosu olacak.
- **Olmayan bir ekran adresi** açılırsa menü ve üst bar yerinde kalacak. Sayfada "Sayfa bulunamadı" başlığı ve "Adresi kontrol et ya da menüden bir ekran seç." yazısı görünecek. Menüde hiçbir öğe seçili olmayacak.
- **Servis adreslerine benzeyen ama iki servisten hiçbirine ait olmayan adresler** (ör. `/api/yok`) boş bir sayfa açmayacak; aynı "bulunamadı" hatasını dönecek.
- **Sağlık ucu sadece servislerin kendi portundan görülecek;** siteden dışarı açılmayacak.
- **Veritabanı şifreleri** sadece bu bilgisayarda kullanılmak üzere çalıştırma dosyasına düz yazılacak. Servislerin ayar dosyasına şifre yazılmayacak.
- **Hata kodları:** Olmayan adres, desteklenmeyen istek türü, bozuk istek, yanlış girilmiş alan ve beklenmeyen hata için ortak kodların hepsi şimdiden çalışacak. Liste numarası gibi adres parametrelerindeki hatalar da "girdiğin bilgileri kontrol et" koduna düşecek.

## Çalışacak roller
1. planner.
2. backend-coder (iki servis) ve frontend-coder (site) paralel çalışır.
3. tester.
4. devops: sistemi ayağa kaldırır, kontrolleri ve uçtan uca testleri çalıştırır.
5. qa.

Atlanan rol yok.

## Kararını bekleyen sorular
1. Bilgi kaynakları ve Değerlendirme ekranlarının açıklamalarındaki değişen değerleri bu task'ta nasıl gösterelim?
   Neden önemli: Lab'da Bilgi kaynakları açıklaması embedding modelinin adını ("her parça bge-m3 ile embed edilip…"), Değerlendirme açıklaması soru sayısını ("Rehberde cevabı olan 149 soru…") içeriyor. Bu değerler sonraki task'larda sistemden gelecek; 149 da eski rehbere ait. Kural "sabit veri yok" diyor.
   Seçenekler:
   A) Açıklamalar değişen değer olmadan yazılır: "…her parça embedding modeliyle embed edilip pgvector'e yazılır." ve "Rehberde cevabı olan sorular; her birinde…". Gerçek değer, ekranın kendi task'ında (Bilgi kaynakları TASK-007, Değerlendirme TASK-016) eklenir.
   B) Bu iki ekranda açıklama, değer gelene kadar hiç gösterilmez; sadece başlık görünür.
   C) Lab'daki metin olduğu gibi yazılır ("bge-m3", "149").
   Önerim: A. Açıklama ekranda kalır ve lab'a en yakın görünüm olur. Hiçbir sayı ya da model adı uydurulmaz. C kuralı çiğner, ayrıca 149 yanlış bir sayı.

2. İskelet için aşağıdaki hazır kütüphaneleri ve araçları kuralım mı?
   Neden önemli: Projede henüz hiçbir kütüphane yok; listede olmayan bir şeyi ekip kendi başına ekleyemez.
   - **Servisler (ikisinde ayrı ayrı):**
     - Spring Boot 4.1: web istekleri, girdi doğrulama, sağlık ucu ve veritabanı erişimi.
     - Flyway: veritabanı değişikliklerini sırayla ve kayıtlı uygulayan araç.
     - PostgreSQL sürücüsü.
     - JSpecify: kodda "boş olabilir" işaretleri.
   - **Servis testleri:**
     - Spring Boot test araçları.
     - Testcontainers: testlerde gerçek bir PostgreSQL'i geçici olarak açar.
     - JaCoCo: testlerin kodun ne kadarını çalıştırdığını ölçer ve %80'in altında derlemeyi durdurur.
     - PIT: kodu bilerek bozup testlerin bunu yakalayıp yakalamadığını dener.
   - **Site:**
     - React 19 ve Vite.
     - React Router: ekran adresleri.
     - Source Sans 3: lab'ın yazı tipi, internetten değil sitenin kendisinden yüklenir.
   - **Site testleri ve denetim:**
     - Vitest ve Testing Library: ekran testleri.
     - vitest-axe: erişilebilirlik kontrolü.
     - Playwright: gerçek tarayıcıda uçtan uca test.
     - oxlint: kod denetimi.
   - **Çalıştırma:** Docker imajları (pgvector'lü PostgreSQL 18, Java 26, Node 26, nginx).

   Seçenekler:
   A) Hepsini kur.
   B) PIT ve Playwright'ı çıkar; ilk gerektikleri task'ta eklenir.

   Önerim: A. İkisi de projenin kurallarında hedef olarak yazılı. Playwright bu task'ta "sayfa yenilenince aynı ekran" ve "bulunamadı hatası siteden de dönüyor" kontrollerini gerçek tarayıcıda yapacak.

## Riskler
- Veritabanı kurulumu sadece ilk açılışta çalışır. Kurulum sonradan değişirse veritabanı verisi silinip yeniden açılmalıdır (henüz veri olmadığı için kayıp yok). README'de yazılacak.
- Önceki proje (`embedding-pgvector`) açıksa aynı portları kullandığı için bu sistem açılmaz; önce o kapatılmalıdır.

## Kullanıcı kararları (2026-10-29)
1. Açıklamalardaki değişen değerler: **A**, değer olmadan yazılır (gerçek değer TASK-007 / TASK-016'da).
2. Yeni kütüphane ve araçlar: **A**, hepsi kurulur (PIT ve Playwright dahil).

ONAY: VERİLDİ
