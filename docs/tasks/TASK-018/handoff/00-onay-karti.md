TASK: TASK-018
# Onay kartı: Uçtan uca doğrulama, lab ile karşılaştırma ve dokümanlar

**Dört soruyu cevapladın. Cevapların aşağıda "Verilen kararlar" bölümünde. Kart bu cevaplara göre güncellendi.**

## Ne yapılacak
- Ürün bütün olarak denenir. Yeni özellik eklenmez. Bulunan hatalar bu task içinde ilgili kişiye geri döner ve düzeltilir.
- Bugünkü sistemin verileri silinir. Sistem README adımlarıyla boş olarak yeniden kurulur.
- TASK'taki 8 senaryo gerçek modellerle, otomatik bir denemeyle baştan sona çalıştırılır:
  - belge ekleme ve yer imsiz PDF'in reddi,
  - soru, akan cevap, alıntı penceresi, "bulamadım" ve temsilciye aktarma,
  - takip sorusu ve geri bildirim,
  - Konuşmalar, parça filtresi ve sayfalama,
  - yükleme sırasında sayfa yenileme,
  - ölçüm.
- Her ekran ve pencere lab'ın sol yarısıyla aynı boyutta ekran görüntüsüyle karşılaştırılır. Farklar tabloya yazılır.
- Kod ve kayıtlar taranır:
  - Kodda sabit ya da sahte veri olmamalı.
  - Açılışta kendiliğinden veri oluşturan kod olmamalı.
  - Kayıtlarda (log) soru, cevap, parça, prompt ya da kişisel veri olmamalı.
- Mutation testleri üç modülde %80 eşikle çalıştırılır. Web için Stryker aracı eklenir. Web'in kütüphaneleri `npm audit` ile güvenlik açısından taranır.
- README baştan yazılır: kurulum, çalıştırma, ekranlar, mimari ve bu makinede ölçülen süreler ve bellek. İçindeki her komut denenir.
- Eskimiş bilgiler dokümanlardan kalkar (lab rehberinin "31 sayfa, 61 parça" sayısı gibi). Kartı onaylarsan lead bunu plandaki metinle hemen yapar. Diğer doküman önerileri (rag.md, kullanım kılavuzu) sana sonunda ayrıca gösterilir.

## Bittiğini nasıl anlayacaksın
1. Raporda 8 senaryonun listesi var ve hepsi gerçek modellerle geçmiş.
2. Temiz kurulum süresi raporda yazar: README adımlarıyla başlayıp ilk cevaba kadar geçen süre. Model indirme hariç 30 dakikanın altındadır.
3. Görsel karşılaştırma tablosu raporda. Her ekran ve pencere için lab görüntüsü, uygulama görüntüsü ve farklar var. Bilinçsiz fark kalmamış.
4. Sahte veri, kayıt, mutation ve `npm audit` sonuçları raporda. Üç modülün mutation skoru %80 ya da üstünde.
5. README'de "bu makinede ölçülen" tablosu var. Tipik bir soru için yeniden yazma, arama, rerank, cevap ve toplam süreyi, ayrıca bellek kullanımını gösterir.
6. Kendin dene:
   - README'yi baştan izle, rehberi Bilgi kaynakları'ndan yükle ve bir soru sor.
   - Repoda "31 sayfa" ara: sonuç boş çıkar (lab dosyası ve eski task kayıtları hariç).
7. İş bitince sistem açık kalır. İçinde denemelerin verileri vardır: yüklenen belgeler, konuşmalar ve ölçümler.

## Yapılmayacaklar
- Yeni özellik. Üretim ortamına dağıtım.
- OWASP bağımlılık taraması (senin kararın). Servislerin Java kütüphaneleri taranmaz; sadece web'in kütüphaneleri `npm audit` ile taranır.
- Bugünkü verileri saklamak. Silinirler; geri getirilmezler (senin kararın).
- Performans iyileştirmesi. Süreler sadece ölçülür ve yazılır, eşik yok.
- Model ya da reranker kapalıyken yapılan denemeler. Bunlar önceki task'larda denendi, TASK bu sefer istemiyor.
- README'ye ekran görüntüsü koymak. Ekranlar kısa metinle anlatılır.

## Benim verdiğim kararlar
- **Karşılaştırma boyutu:** Lab 1480×900 pencerede açılır. Uygulama, lab'daki ürün alanının boyutunda açılır (yaklaşık 740×835). Böylece iki görüntüde aynı büyüklükte alan karşılaştırılır.
- **Önceden "bilinçli" sayılan farklar:**
  - gerçek veriden gelen sayılar ve metinler, modelin yazdığı cevaplar,
  - lab'da olmayan boş durumlar ve "sürüyor" durumları,
  - lab'ın sahte tarayıcı çerçevesi,
  - üstteki kullanıcı alanı ("Raşit Eşdemir"), çünkü TASK-002'de kaldırıldı,
  - "Yeni sürüm yükle", çünkü projeden çıkarıldı.
  Bunların dışındaki her fark hata sayılır ve düzeltilir.
- **Yer imli ikinci belge:** RAG anlatım sayfası deneme sırasında yer imli bir PDF'e çevrilip yüklenir. Repoya yeni PDF girmez. Yer imsiz PDF için repodaki deneme dosyası kullanılır.
- **Sayfa yenileme denemesi** rehber yüklenirken yapılır. Rehberin işlenmesi yaklaşık 10 saniye sürer.
- **Otomatik denemenin hazırlanması:** tester, denemeleri yazarken ayrı ve geçici bir kopya kullanır. Bunun için bugünkü sistem sadece durdurulur. Verilerin silinmesi devops'un temiz kurulumunun ilk adımıdır.
- **Tipik soru ölçümü:**
  - Golden set'ten 5 farklı konudan birer soru ve bir takip sorusu ("Kartımı kaybettim, ne yapmalıyım?" → "Yenisi kaç günde gelir?") sorulur.
  - Her biri 3 kez sorulur. Ortanca ve en kötü süre yazılır.
  - Isınma için sorulan ilk soru ayrıca yazılır.
  - Bellek: 4 Docker servisi, Ollama ve reranker ölçülür. Soru sırasındaki en yüksek değer ve swap yazılır.
- **Güvenlik bulguları (`npm audit`):** Ürünle birlikte çalışan bir web kütüphanesinde yüksek ya da kritik açık bulunabilir. Aynı ana sürümde düzeltmesi varsa bu task'ta yükseltilir. Diğerleri ve sadece geliştirmede kullanılan araçlardaki bulgular raporda kalır.
- **Kayıt denetimi:** Site sunucusunun erişim kayıtlarında, parça filtresine yazılan arama kelimesi adresin içinde görünür (ör. "asgari ödeme"). Bu müşteri sorusu ya da kişisel veri değildir; raporda not olarak geçer.
- **Komutların denenmesi:**
  - README'deki model ve reranker komutları, çalışan modeller durdurulmadan denenir. Reranker komutu ikinci bir kopyayla, başka bir portta denenir.
  - Hiçbir denemede senin bir süreci kapatman gerekmiyor. Ekip kendi açtığı kopyayı kapatamazsa senden isteriz.
  - tmux ve claude gibi etkileşimli komutlar ekranı açmadan denenir.
- **30 dakika ölçümü:** Docker'ın taban imajları bilgisayarda zaten inmiş olabilir. Raporda bu belirtilir.

## Çalışacak roller
1. planner.
2. lead:
   - Onaydan hemen sonra eskimiş bilgileri plandaki metinle düzeltir.
   - Kapanışta rag.md ve kullanım kılavuzu önerilerini sana sunar.
3. backend-coder: iki serviste mutation eşiğini %80'e ayarlar ve bulunan servis hatalarını düzeltir.
4. frontend-coder: web'e Stryker'ı %80 eşikle kurar ve bulunan ekran hatalarını düzeltir.
5. tester:
   - 8 senaryonun otomatik denemesini yazar ve çalıştırır.
   - Bütün testleri çalıştırır.
   - Mutation skorlarını %80'e çıkaracak testleri ekler.
6. devops:
   - Bugünkü verileri siler ve sistemi README'ye göre temiz kurar.
   - README'yi yazar ve içindeki komutları dener.
   - Süre ve bellek ölçümlerini yapar.
7. qa: görsel karşılaştırma, sahte veri ve kayıt taraması, mutation ve `npm audit`, son kontrol.

Bulunan hata kime döner:
- ekran farkı → frontend-coder
- servis hatası ya da kayıtta veri sızması → backend-coder
- eksik test ya da düşük mutation skoru → tester
- kurulum ya da README sorunu → devops

Atlanan rol yok.

## Verilen kararlar
1. **Deneme verisi: B.** Bugünkü veriler silinir ve denemeler bu sistemde yapılır. Silme, devops'un temiz kurulumunun ilk adımıdır. Kapanışta sistem deneme verileriyle kalır.
2. **Stryker: A.** Eklenir; sadece geliştirmede kullanılır. `npm run test:mutation` komutu gelir.
3. **Mutation eşiği: A.** Üç modülde %80. Eşik modül dosyalarına ve araç ayarına yazılır. Altında kalan modülde tester test ekler.
4. **NVD anahtarı ve OWASP:** Atlanır. OWASP bağımlılık taraması bu task'ta yapılmaz; `npm audit` yapılır.

## Riskler
- Mutation testleri uzun sürer, modül başına 30-60 dakika. Web'in ilk skoru %80'in altındaysa tester'ın ek test yazması task'ı uzatır.
- 16 GB'ta üç model birlikte belleği doldurabilir. Senaryolar yavaşlar; ölçülen süreler swap'e göre değişebilir.
- Rehber çok hızlı işlenirse sayfa yenileme denemesi ara sıra başarısız olabilir. Olursa bu raporda görünür.
- Servislerin Java kütüphanelerindeki bilinen açıklar bu task'ta taranmıyor.

ONAY: VERİLDİ
