# TASK-007: Bilgi kaynakları ekranı

## Ne isteniyor
Lab'ın sol yarısındaki Bilgi kaynakları ekranı ve kaynak detay ekranı gerçek veriyle çalışır. Veriyi retrieval-api'nin kaynak, sürüm, parça ve içe alma uçları verir; web bu uçları doğrudan çağırır (lab'daki gibi). Ekranda `../embedding-pgvector`'deki gibi bir "Belge ekle" düğmesi ve penceresi vardır; kullanıcı kendi belgesini buradan yükler ve belge gerçek içe alma hattından geçer (uç: TASK-027). Lab'da bu düğme yoktu, çünkü demoda rehber hazır geliyordu; görünümü lab'ın tasarım diline uygun yapılır. "Yeni sürüm yükle" bu task'ta yok (TASK-015).

## Kurallar
1. **Liste ekranı (`/kaynaklar`):**
   - Tablo sütunları: Kaynak (ad ve dosya adı), Tür ("PDF · N sayfa"), Sürüm, Parça, Durum, Son senkron.
   - Durum: iş sürüyorsa aşama adı, değilse "Hazır".
   - Altında lab'daki bilgi kutusu yer alır: "Çalışma alanı Raşit Bankası · Dijital Asistan (ws 42) · embedding modeli bge-m3@1024. Modeli değiştirmek …". Değerler sabit yazılmaz, backend'den gelir.
2. **Belge ekle:**
   - Liste ekranının başlığında "Belge ekle" düğmesi vardır.
   - Pencerede dosya seçimi ve kaynak adı alanı vardır; ad boşsa dosya adı (uzantısız) kullanılır. Kabul edilen türler ve sınırlar TASK-027'deki kurallardır.
   - "Vazgeç" ve "Yükle ve işle" düğmeleri vardır. Yükleme başlayınca pencere kapanır, yeni kaynak listede iş aşamasıyla görünür.
   - Doğrulama hataları (boş dosya, 20 MB üstü, desteklenmeyen tür, aynı ad) Türkçe mesajla pencerede gösterilir.
   - Pencere odak yönetimiyle çalışır: açılınca ilk alana odaklanır, Esc ile kapanır, odak düğmeye döner.
   - Görünüm ve metinler onay kartında lab'ın "Yeni sürüm yükle" penceresine benzetilerek gösterilir.
3. **Boş durum:** Hiç belge yoksa liste yerine bir boş durum ve "Belge ekle" çağrısı gösterilir. Metni onay kartında belirlenir.
4. **Detay ekranı (`/kaynaklar/{id}`):**
   - Breadcrumb ve başlık; alt başlıkta dosya adı, bayt, sürüm ve son senkron.
   - "PDF'i aç" düğmesi saklanan PDF'i yeni sekmede açar.
   - Meta kutuları: Sayfa / bölüm, Parça · ortalama token, Parça boyutu, Örtüşme, Embedding, Index. Değerler backend'den gelir.
5. **İçe alma işleri:** Tabloda İş, Dosya, Sürüm, Parça, Süre ve Durum sütunları vardır; en yeni iş üstte.
6. **Parçalar:**
   - Tabloda #, Sayfa · başlık, İlk satır ve Token sütunları vardır.
   - "Parçalarda ara (başlık ya da metin)" filtresi sunucuda çalışır.
   - Sayfa başına 12 parça gösterilir; "Önceki / Sonraki" düğmeleri ve "N / M" bilgisi vardır. "Eşleşen parça yok." boş durumu gösterilir.
   - Liste uçları projenin `PageResponse` standardını kullanır.
7. **Parça penceresi:**
   - Bir parçaya tıklanınca açılır; başlık parçanın başlığıdır.
   - Bilgi şeridinde chunk_id, chunk_no, token, model ve kaynak (ad ve sürüm) bilgisi yer alır.
   - Parça metni ve embedding'in ilk değerleri gösterilir ("… N değer daha").
   - "Kapat" düğmesi ve Esc ile kapanır; odak, pencereyi açan öğeye döner.
   - Bu pencere TASK-008'de alıntılardan da açılacak; yeniden kullanılabilir yazılır.
8. **İş sürerken izleme:** Ekran açıkken bir içe alma işi sürüyorsa iş durumu polling ile izlenir; iş bitince liste ve detay yenilenir. Polling ekrandan çıkınca durur.
9. Hata mesajları `errorCode` → Türkçe eşlemeyle gösterilir. Kaynak bulunamazsa ekranda Türkçe bir mesaj gösterilir.
10. Parça metni ve PDF içeriği güvenli gösterilir: HTML olarak yorumlanmaz.

## Örnek senaryolar
- Sistem ilk kez açılmıştır; Bilgi kaynakları boş durumu gösterir.
- Kullanıcı "Belge ekle"ye basar, rehberin 2026.10 PDF'ini seçer, "Yükle ve işle"ye basar. Satır aşamaları gösterir, sonra "Raşit Bankası Ürün ve Hizmet Rehberi · PDF · N sayfa · 2026.10 · Hazır" olur.
- Kullanıcı kendi şirketinin bir PDF'ini de yükler; listede iki kaynak görünür.
- Satıra tıklar, detayda içe alma işini `DONE` durumunda görür.
- Aynı adla ikinci belge yüklemeye çalışır; pencerede Türkçe hata mesajı görür.
- Filtreye "asgari ödeme" yazar; sadece başlığında ya da metninde bu ifade geçen parçalar listelenir.
- Bir parçaya tıklar; pencerede tam metni ve embedding'in ilk değerlerini görür, Esc ile kapatır.

## Kabul kriterleri
- Ekranlar lab'ın sol yarısıyla yan yana konduğunda aynı görünür (lab'da olmayan "Belge ekle" ve boş durum hariç).
- "Belge ekle" gerçek bir PDF'le uçtan uca denenmiş (E2E testi dahil).
- Filtre ve sayfalama sunucuda çalışır; 2. sayfaya geçildiğinde doğru 12 parça gelir.
- Backend ve frontend testleri ile `./mvnw verify`, `npm run build` ve `npm run lint` geçer.

## Kapsam dışı
- Yeni sürüm yükleme, "Yeni sürüm yükle" düğmesi ve ilerleme kartı (TASK-015).
- Kaynak silme.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
