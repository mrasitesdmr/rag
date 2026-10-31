# TASK-027: Açılışta otomatik yüklemenin kaldırılması, belge ekleme ucu ve genel PDF okuyucu

## Ne isteniyor
Bilgi bankasına hiçbir belge kendiliğinden girmez. TASK-005'te servis açılırken repodaki rehber PDF'i otomatik içe alınıyordu; bu kaldırılır. Sistem ilk kez açıldığında bilgi bankası boştur. Belgeler sadece kullanıcının yüklemesiyle girer: retrieval-api'ye `../embedding-pgvector`'deki gibi bir "belge ekle" ucu (`POST /api/workspaces/{workspaceId}/sources`, dosya + ad, 202 + iş adresi) eklenir ve yüklenen dosya TASK-005'teki gerçek içe alma hattından geçer. PDF okuyucu sadece bizim rehberin biçimine bağlı kalmaz; kullanıcının yüklediği sıradan bir PDF'i de okur. Ekrandaki "Belge ekle" düğmesi ve penceresi TASK-007'de gelir; bu task'ta ekran yok.

## Kurallar
1. **Otomatik yükleme yok:** Açılışta rehberi içe alan kod, ayarları (`ingestion.bootstrap.*`) ve Docker'daki rehber klasörü bağlantısı kaldırılır. Servis açılırken hiçbir kaynak, sürüm, parça ya da iş oluşturmaz. Açılışta sadece yarıda kalmış işlerin "servis kapandı" ile kapatılması (TASK-005, 2. soru A) kalır.
2. **Belge ekleme ucu:** İstek dosya ve kaynak adını taşır. Cevap 202, `Location` başlığında iş adresi. İş TASK-005'teki aşamalardan geçer (sıraya alındı, okunuyor, parçalanıyor, embed ediliyor, yazılıyor, bitti ya da başarısız). Sözleşme `../embedding-pgvector`'deki `createSource` ucu örnek alınarak `docs/api/openapi.yaml`'a yazılır.
3. **Doğrulama:** Dosya boş olamaz, en fazla 20 MB. Kaynak adı 2-80 karakter, çalışma alanında büyük/küçük harf farkı gözetmeden tekildir. Ad boş bırakılırsa dosya adı (uzantısız) kullanılır. Her ihlal sabit bir `errorCode` döner. İlk sürüm etiketi zorunlu mu, isteğe bağlı mı: onay kartında sorulur.
4. **Dosya türleri:** PDF kabul edilir. `../embedding-pgvector` ayrıca `.txt`, `.md` ve `.csv` kabul ediyordu; bunların da eklenip eklenmeyeceği onay kartında sorulur.
5. **Genel PDF okuyucu:**
   - Yer imi (PDF'in yan menüsündeki başlıklar) varsa başlıklar oradan alınır. Yoksa belge yine okunur; parça başlığı ve bölüm bilgisi olmadan ya da başka bir yöntemle (onay kartında) parçalanır. `PDF_NO_SECTIONS` ile durmak yoktur.
   - Rehbere özgü ayarlar kalkar: üst/alt bilgi kalıbı (`page-header-pattern`, `page-footer-pattern`) ve "içerik 5. sayfadan başlar" kuralı. Sayfalarda tekrarlanan üst/alt bilginin genel olarak nasıl ayıklanacağı (ör. sayfaların çoğunda aynı olan ilk/son satır) onay kartında yazılır.
   - Metni çıkmayan PDF (taranmış görüntü) ya da açılamayan dosya için iş sabit bir `errorCode` ile `FAILED` olur; boş parça yazılmaz.
   - Bizim rehber (`docs/rehber/pdf/…2026.10.pdf`) bu okuyucuyla yüklendiğinde TASK-005'teki kalite korunur: başlıklar doğru, üst/alt bilgi parçalara girmez, tablo satırı bölünmez.
6. **Çalışma alanı:** Çalışma alanı (42, `bge-m3@1024`) kaynak ya da parça içermez, uygulamanın yapılandırmasıdır. Bugün nasıl oluştuğu (migration, ayar) ve böyle kalıp kalmayacağı onay kartında kullanıcıya gösterilir.
7. **Mevcut veri:** Geliştirme veritabanında açılıştan kalmış rehber kaynağı (10001) ve parçaları varsa nasıl temizleneceği (ör. `docker compose down -v`) `deployment/CLAUDE.md`'de yazar. Kod içinde sabit kaynak numarası (10001) kalmaz.
8. **Loglama:** Dosya içeriği ve parça metni loglanmaz; iş numarası, dosya boyutu, sayfa sayısı, parça sayısı ve süre loglanır.
9. Repodaki rehber PDF'leri (`docs/rehber/pdf/`) kalır; kullanıcının elle yükleyebileceği örnek belgedir, servis onlara bakmaz.

## Örnek senaryolar
- Veriler silinmiş sistem ilk kez açılır. `GET /api/workspaces/42/sources` boş liste döner; hiç iş yoktur.
- Kullanıcı rehberin 2026.10 PDF'ini `curl -F file=@… -F name=…` ile yükler. 202 ve iş adresi gelir; iş birkaç saniye içinde `DONE` olur.
- Kullanıcı yer imi olmayan sıradan bir PDF yükler (ör. kendi şirketinin el kitabı). İş `DONE` olur; parçalar sayfa numaralarıyla yazılır.
- Kullanıcı aynı adla ikinci bir belge yükler: 409, sabit `errorCode`.
- Kullanıcı metni olmayan, taranmış bir PDF yükler: iş `FAILED`, sabit `errorCode`, parça yok.
- Servis yeniden başlatılır: kaynak ve parça sayısı değişmez, yeni iş açılmaz.

## Kabul kriterleri
- Temiz bir Docker açılışında bilgi bankası boş; kodda, ayarlarda ve Compose'da otomatik yükleme izi yok (aramayla doğrulanır).
- Gerçek `bge-m3` ile iki belge yüklenmiş: bizim rehber (2026.10) ve yer imi olmayan bir PDF. Parça sayısı, ortalama token ve süre raporda; rehber için TASK-005 sonucuyla fark raporda.
- qa her iki belgeden rastgele parçaları PDF'le elle karşılaştırmış.
- Doğrulama, ad çakışması, okunamayan dosya ve yeniden başlatma testlerle doğrulanmış; `cd retrieval-api && ./mvnw verify` geçer.

## Kapsam dışı
- Bilgi kaynakları ekranı, "Belge ekle" düğmesi ve penceresi (TASK-007).
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
