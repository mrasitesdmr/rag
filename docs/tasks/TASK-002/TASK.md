# TASK-002: Proje iskeleti: iki servis, web kabuğu ve veritabanı

## Ne isteniyor
Üç modülün çalışan iskeleti kurulur ve Docker Compose ile birlikte ayağa kalkar. Web açıldığında lab'ın sol yarısındaki kabuk görünür: üst bar (Raşit Bankası · Dijital Asistan Yönetimi logosu, "Canlı yapılandırma" rozeti), yan menü (Önizleme, Konuşmalar, Bilgi kaynakları, Değerlendirme) ve her ekranın başlığı ile açıklama metni. Ekranların içeriği sonraki task'larda gelir. Sağ yarıdaki akışın hiçbir adımı bu task'ta yok.

## Kurallar
1. `assistant-api` ve `retrieval-api`: Spring Boot 4, Java 26, Maven wrapper (`./mvnw`). `web`: React 19, TypeScript, Vite. Servisler birbirinden bağımsız derlenir ve kod paylaşmaz.
2. Portlar lab'daki gibidir: web 3000, assistant-api 8090, retrieval-api 8080, PostgreSQL 5432.
3. Tek PostgreSQL sunucusu (pgvector'lü imaj), iki veritabanı: `assistant` ve `retrieval`. Her servis sadece kendi veritabanına kendi kullanıcısıyla bağlanır; diğer veritabanına erişim yetkisi yoktur.
4. Her serviste Flyway kuruludur; bu task'ta domain tablosu oluşturulmaz.
5. Her servisin sağlık ucu vardır; Compose sağlık kontrolü bu uçları kullanır.
6. Bilinmeyen bir uca istek atıldığında servis RFC 9457 Problem Details ve `errorCode: NOT_FOUND` döner. Ortak hata kodları `docs/api/openapi.yaml`'daki iskelettekilerdir.
7. Web, istekleri aynı adres üzerinden gönderir: `/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api (dev'de Vite proxy, Docker'da nginx).
8. Ekran adresleri: `/onizleme`, `/konusmalar`, `/kaynaklar`, `/degerlendirme`. Kök adres Önizleme'ye gider. Adres doğrudan açıldığında ya da sayfa yenilendiğinde aynı ekran gelir.
9. Görünüm lab'ın aynısıdır: renkler, font, üst bar, yan menü, seçili menü öğesi ve sayfa başlığı stili lab'dan alınır (design token).
10. Mock ya da sabit veri yoktur. Menüdeki Konuşmalar sayacı ve üst bardaki kullanıcı alanı gerçek veri gelene kadar gösterilmez. Sayaç TASK-014'te, kullanıcı alanı TASK-017'de gelir; sabit bir isim ya da sayı yazılmaz.

## Örnek senaryolar
- Kullanıcı `docker compose up` çalıştırır. postgres, assistant-api, retrieval-api ve web konteynerleri sağlıklı olur. `http://localhost:3000` Önizleme ekranının başlığını gösterir.
- Kullanıcı yan menüden Bilgi kaynakları'na tıklar. Adres `/kaynaklar` olur, başlık "Bilgi kaynakları" ve lab'daki açıklama metni görünür. Sayfayı yeniler, aynı ekranda kalır.
- `GET /api/assistant/yok` isteği 404 ve `errorCode: NOT_FOUND` döner.

## Kabul kriterleri
- `cd assistant-api && ./mvnw verify` ve `cd retrieval-api && ./mvnw verify` geçer.
- `cd web && npm run build` ve `npm run lint` geçer.
- `docker compose -f deployment/docker-compose.yml up` ile tüm servisler sağlıklı olur.
- assistant-api kullanıcısı `retrieval` veritabanına bağlanamaz (ve tersi).
- Kabuk, lab'ın sol yarısıyla yan yana konduğunda aynı görünür (ekran görüntüsüyle karşılaştırma).

## Kapsam dışı
- Domain tabloları, endpoint'ler, lokal modeller.
- Ekranların içerikleri.
- Kimlik doğrulama (TASK-017).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [x] Veritabanı (migration; hangi servis?) (iki veritabanı, boş Flyway)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
