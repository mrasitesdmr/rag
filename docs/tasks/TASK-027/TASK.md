# TASK-027: Açılışta otomatik yüklemenin kaldırılması, belge ekleme ucu ve genel PDF okuyucu

## Ne isteniyor
Bilgi bankasına hiçbir belge kendiliğinden girmez. TASK-005'te servis açılırken repodaki rehber PDF'i otomatik içe alınıyordu; bu kaldırılır. Sistem ilk kez açıldığında bilgi bankası boştur. Belgeler sadece kullanıcının yüklemesiyle girer: retrieval-api'ye `../embedding-pgvector`'deki gibi bir "belge ekle" ucu (`POST /api/workspaces/{workspaceId}/sources`, dosya + ad, 202 + iş adresi) eklenir ve yüklenen dosya TASK-005'teki gerçek içe alma hattından geçer. PDF okuyucu sadece bizim rehberin biçimine bağlı kalmaz; başlıkları (yer imleri) olan her PDF'i okur. Başlığı olmayan belge tahminle işlenmez, yükleme anında açıklayıcı bir hatayla reddedilir. Ekrandaki "Belge ekle" düğmesi ve penceresi TASK-007'de gelir; bu task'ta ekran yok.

## Kurallar
1. **Otomatik yükleme yok:** Açılışta rehberi içe alan kod, ayarları (`ingestion.bootstrap.*`) ve Docker'daki rehber klasörü bağlantısı kaldırılır. Servis açılırken hiçbir kaynak, sürüm, parça ya da iş oluşturmaz. Açılışta sadece yarıda kalmış işlerin "servis kapandı" ile kapatılması (TASK-005, 2. soru A) kalır.
2. **Belge ekleme ucu:** İstek dosya ve kaynak adını taşır. Cevap 202, `Location` başlığında iş adresi. İş TASK-005'teki aşamalardan geçer (sıraya alındı, okunuyor, parçalanıyor, embed ediliyor, yazılıyor, bitti ya da başarısız). Sözleşme `../embedding-pgvector`'deki `createSource` ucu örnek alınarak `docs/api/openapi.yaml`'a yazılır.
3. **Doğrulama:** Dosya boş olamaz, en fazla 20 MB. Kaynak adı 2-80 karakter, çalışma alanında büyük/küçük harf farkı gözetmeden tekildir; aynı adla ikinci yükleme, önceki belgenin durumu ne olursa olsun reddedilir. Ad boş bırakılırsa dosya adı (uzantısız) kullanılır. Sürüm etiketi zorunludur, 1-40 karakter (kullanıcı kararı, 2026-10-31). Her ihlal sabit bir `errorCode` döner.
4. **Dosya türleri:** Sadece PDF kabul edilir (kullanıcı kararı, 2026-10-31). `.txt` ve `.csv`'de başlık olmadığı için Kural 5'e zaten uymaz; `.md` gerekirse ayrı task olur.
5. **Genel PDF okuyucu:**
   - Başlıklar PDF'in yer imlerinden (yan menüdeki başlıklar) alınır. **Yer imi olmayan PDF reddedilir** (kullanıcı kararı, 2026-10-31): sistem başlığı tahmin etmez; belgeyi yükleyen bilerek, başlıklı belge yükler. Ekranda kalın/büyük görünen ama yer imi olmayan başlıklar da başlık sayılmaz.
   - Rehbere özgü ayarlar kalkar: üst/alt bilgi kalıbı (`page-header-pattern`, `page-footer-pattern`) ve "içerik 5. sayfadan başlar" kuralı. Sayfalarda tekrarlanan üst/alt bilginin genel olarak nasıl ayıklanacağı (ör. sayfaların çoğunda aynı olan ilk/son satır) onay kartında yazılır.
   - **Dosyadan kaynaklanan hatalar yükleme anında yakalanır:** yer imi yok, metin yok (taranmış görüntü), PDF açılamıyor. Bu durumda istek hemen sabit bir `errorCode` ile reddedilir; kaynak kaydedilmez, iş açılmaz, ad kilitlenmez. TASK-007'de bu hata, belgenin nasıl düzeltileceğini anlatan Türkçe bir mesajla gösterilir (ör. Word'de başlık stilleri + PDF'e kaydederken "yer imi oluştur").
   - Bizim rehber (`docs/rehber/pdf/…2026.10.pdf`) bu okuyucuyla yüklendiğinde TASK-005'teki kalite korunur: başlıklar doğru, üst/alt bilgi parçalara girmez, tablo satırı bölünmez.
6. **Yeniden deneme:** Kaydedilmiş bir belgenin içe alma işi sadece geçici bir sebeple başarısız olabilir (embedding modeline ulaşılamadı, servis iş sürerken kapandı). Bunun için bir "yeniden dene" ucu eklenir: hiç tamamlanmış sürümü olmayan ve işi sürmeyen belge için, saklanan PDF'ten yeni bir iş başlatır; dosyanın yeniden seçilmesi gerekmez. Ekrandaki "Yeniden dene" düğmesi TASK-007'de gelir (kullanıcı kararı, 2026-10-31).
7. **Çalışma alanı:** Çalışma alanı (42, `bge-m3@1024`) bugünkü gibi migration'la oluşur ve öyle kalır (kullanıcı kararı, 2026-10-31). Kaynak ya da parça içermez, kurulumun parçasıdır; açılışta veritabanına yazılmaz.
8. **Mevcut veri:** Geliştirme veritabanında açılıştan kalmış rehber kaynağı (10001) ve parçaları varsa nasıl temizleneceği (ör. `docker compose down -v`) `deployment/CLAUDE.md`'de yazar. Kod içinde sabit kaynak numarası (10001) kalmaz.
9. **Loglama:** Dosya içeriği ve parça metni loglanmaz; iş numarası, dosya boyutu, sayfa sayısı, parça sayısı ve süre loglanır.
10. Repodaki rehber PDF'leri (`docs/rehber/pdf/`) kalır; kullanıcının elle yükleyebileceği örnek belgedir, servis onlara bakmaz.

## Örnek senaryolar
- Veriler silinmiş sistem ilk kez açılır. `GET /api/workspaces/42/sources` boş liste döner; hiç iş yoktur.
- Kullanıcı rehberin 2026.10 PDF'ini `curl -F file=@… -F name=… -F version=2026.10` ile yükler. 202 ve iş adresi gelir; iş birkaç saniye içinde `DONE` olur.
- Kullanıcı yer imleri olan başka bir PDF yükler (ör. Word'den başlık stilleriyle kaydedilmiş bir el kitabı). İş `DONE` olur; parçalar kendi başlıkları ve sayfa numaralarıyla yazılır.
- Kullanıcı yer imi olmayan bir PDF yükler: istek hemen sabit `errorCode` ile reddedilir; kaynak listesinde yeni kayıt yoktur, aynı adla düzeltilmiş belge yüklenebilir.
- Sürüm etiketi olmadan yükleme: 400, `VALIDATION_FAILED`.
- Kullanıcı aynı adla ikinci bir belge yükler: 409, sabit `errorCode`.
- Kullanıcı metni olmayan, taranmış bir PDF yükler: istek hemen reddedilir, kaynak ve iş oluşmaz.
- Ollama kapalıyken yüklenen belgenin işi `FAILED` olur. Ollama açılınca "yeniden dene" ucu aynı belgede yeni bir iş başlatır; iş `DONE` olur, kaynak tek kalır.
- Servis yeniden başlatılır: kaynak ve parça sayısı değişmez, yeni iş açılmaz.

## Kabul kriterleri
- Temiz bir Docker açılışında bilgi bankası boş; kodda, ayarlarda ve Compose'da otomatik yükleme izi yok (aramayla doğrulanır).
- Gerçek `bge-m3` ile iki belge yüklenmiş: bizim rehber (2026.10) ve yer imleri olan başka bir PDF. Yer imi olmayan bir PDF'in reddedildiği de gerçek dosyayla denenmiş. Parça sayısı, ortalama token ve süre raporda; rehber için TASK-005 sonucuyla fark raporda.
- qa her iki belgeden rastgele parçaları PDF'le elle karşılaştırmış.
- Doğrulama, ad çakışması, yer imi/metin olmayan ve açılamayan dosyanın yükleme anında reddi, yeniden deneme ve yeniden başlatma testlerle doğrulanmış; `cd retrieval-api && ./mvnw verify` geçer.

## Kapsam dışı
- Bilgi kaynakları ekranı, "Belge ekle" düğmesi ve penceresi, "Yeniden dene" düğmesi, hata mesajlarının Türkçe metinleri (TASK-007).
- PDF dışındaki dosya türleri.
- Yer imi olmayan PDF'ten başlık tahmin etmek.
- Yeni sürüm yükleme (TASK-015).
- Kaynak silme.
- Vektör arama (TASK-006).
- Parça örtüşmesi ve sayfa etiketi düzeltmesi (TASK-024; bu task'tan sonra yapılır).

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (bge-m3)
- [x] Veritabanı (migration; hangi servis?) (retrieval, gerekirse)
- [ ] WebSocket
- [x] Docker (rehber klasörü bağlantısının kaldırılması)
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
