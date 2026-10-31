# TASK-018: Uçtan uca doğrulama, lab ile karşılaştırma ve dokümanlar

## Ne isteniyor
Ürün bütün olarak doğrulanır. Temiz bir makinede `docker compose up` ve `ollama.md` adımlarıyla sistem kurulur. Lab'ın sol yarısındaki bütün senaryolar gerçek sistemde baştan sona çalıştırılır, ekranlar lab ile yan yana karşılaştırılır. Kullanıcıya yönelik dokümanlar (`README.md`, `KULLANIM-KILAVUZU.md` notu, `rag.md`) son hâline getirilir. Bu task yeni özellik eklemez; bulunan eksikler ayrı task olarak açılır.

## Kurallar
1. **Senaryolar (E2E, Playwright):**
   1. giriş yapma; sistem boş bilgi bankasıyla açılır, belgeler "Belge ekle" ile yüklenir (rehber ve yer imli başka bir PDF; yer imi olmayan PDF'in reddedilmesi de denenir);
   2. Önizleme'de serbest metin soru, akan cevap, alıntı penceresi, "bulamadım" ve temsilciye aktarma;
   3. takip sorusu ve yeniden yazılmış arama sorgusu;
   4. geri bildirim;
   5. Konuşmalar'da konuşmanın ve tur ayrıntılarının görünmesi;
   6. Bilgi kaynakları'nda parça filtresi ve sayfalama;
   7. yeni sürüm yükleme, sayfa yenilenince devam ve önce/sonra cevap farkı;
   8. Değerlendirme çalıştırması.
2. **Görsel karşılaştırma:** Her ekran ve pencere lab'ın sol yarısıyla aynı pencere boyutunda ekran görüntüsüyle karşılaştırılır. Farklar listelenir. Bilinçli farklar gerekçesiyle yazılır (ör. gerçek veriden gelen sayılar, serbest metin, lab'da olmayan boş durumlar). Bilinçsiz farklar düzeltilmek üzere ayrı task'a dönüşür.
3. **Mock taraması:** Ürün kodunda (test dışında) sabit soru, cevap, parça, konuşma, kullanıcı, metrik ya da model adı olmadığı taranır. Açılışta kendiliğinden belge, kaynak ya da parça oluşturan kod olmadığı da taranır. Test dublörleri (MSW, Mockito) sadece testlerde bulunur.
4. **Kalite:** İki serviste mutation testi (PIT), web'de mutation testi (Stryker) çalıştırılır; eşikler modül `CLAUDE.md`'lerindeki gibidir. `npm audit` ve OWASP bağımlılık taraması yapılır.
5. **Performans ve kaynak:** 16 GB makinede tipik bir soru için uçtan uca süre ve adım süreleri ölçülür (yeniden yazma, arama, rerank, cevap). Bellek kullanımı ölçülür. Sonuçlar `README.md`'de "bu makinede ölçülen" notuyla yazılır.
6. **Loglama denetimi:** Bir senaryo koşusunun logları taranır; soru, cevap, parça metni, prompt ya da kişisel veri bulunmadığı doğrulanır.
7. **Dokümanlar:** `README.md` (kurulum, çalıştırma, ekranlar, mimari, ölçümler) devops tarafından yazılır. `rag.md` ve `KULLANIM-KILAVUZU.md` için güncelleme önerileri rapora yazılır; bu dosyaları kullanıcı onayıyla lead günceller.

## Örnek senaryolar
- Yeni klonlanmış repoda README adımları izlenir; kullanıcı rehberi ekrandan yükler; 30 dakika içinde (model indirme hariç) ilk soru cevaplanır.
- Görsel karşılaştırmada Değerlendirme kutularının hizası lab'dan farklı çıkar; düzeltme task'ı açılır.

## Kabul kriterleri
- Bütün E2E senaryoları gerçek modellerle geçmiş; sonuçlar raporda.
- Görsel karşılaştırma tablosu raporda; bilinçsiz fark kalmamış ya da her biri için task açılmış.
- Mutation, güvenlik ve log denetimi sonuçları raporda.
- README güncel ve içindeki her komut çalışıyor.

## Kapsam dışı
- Yeni özellik.
- Üretim ortamına dağıtım (bu proje lokal çalışır).

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [x] Docker
- [x] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz) (dokümanlar)
