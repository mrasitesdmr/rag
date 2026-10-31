# TASK-006: Vektör arama ucu (retrieval-api)

## Ne isteniyor
retrieval-api'ye vektör arama ucu eklenir: `POST /api/workspaces/42/search`. Bir sorgu metni alır, `bge-m3` ile embed eder ve çalışma alanına yüklenmiş bütün belgelerin güncel sürümlerindeki en yakın parçaları döndürür. Belgeler sadece kullanıcının yüklemesiyle girer (TASK-027). assistant-api, retrieval adımında bu ucu çağırır (TASK-008). Ekran yok.

## Kurallar
1. İstek: sorgu metni ve kaç parça istendiği (`topK`). Lab'da top-5 için 5, rerank için 20 aday istenir.
2. `topK` 1 ile 50 arasındadır. Boş sorgu, çok uzun sorgu (sınır onay kartında) ya da sınır dışı `topK` için `VALIDATION_FAILED` döner.
3. Arama çalışma alanındaki her kaynağın sadece güncel (son `DONE` olan) sürümünün parçalarında yapılır. Hiç `DONE` sürümü olmayan kaynak aramaya girmez. İçe alma sürerken arama, yeni sürüm bitene kadar eski sürümden sonuç döndürür.
4. Her sonuçta şunlar bulunur:
   - parça kimliği, sıra no, sayfa no, başlık ve metin,
   - kaynak kimliği, kaynak adı ve sürüm etiketi,
   - cosine mesafesi.
   Sonuçlar mesafeye göre artan sıradadır; mesafe eşitse parça kimliğine göre sıralanır (deterministik).
5. HNSW `ef_search` 40'tır (lab). Sorgu index'i kullanır.
6. Embedding modeline erişilemezse sabit bir `errorCode` ile 503 döner.
7. Arama kaydı tutulmaz. "Hangi soruda hangi parça geldi" bilgisi assistant-api'nin tur kaydındadır.
8. Sorgu metni loglanmaz; sorgu uzunluğu, `topK`, sonuç sayısı ve süre loglanır.

## Örnek senaryolar
- Rehberin 2026.10 PDF'i belge ekleme ucuyla yüklenmiştir. "Kartımı kaybettim, ne yapmalıyım?" ile `topK: 5` istenir. İlk sonuçlar rehberin kayıp/çalıntı kart bölümündendir.
- `topK: 0` → 400, `VALIDATION_FAILED`, `violations[0].field = topK`.
- Bilgi bankası boşken arama → boş liste.
- Rehber ve başka bir belge yüklüyken sonuçlar iki belgeden de gelebilir; her sonuç hangi belgeden geldiğini taşır.

## Kabul kriterleri
- Rehberden seçilmiş en az 10 soru için doğru parçanın ilk 5'te olup olmadığı gerçek modelle ölçülmüş, sonuç raporda (bu ölçüm golden set değildir; TASK-016'nın yerini tutmaz).
- `EXPLAIN` çıktısı HNSW index'inin kullanıldığını gösterir.
- Sıralama, filtreleme (her kaynağın güncel sürümü, birden fazla kaynak) ve doğrulama testleri yazılmış; `./mvnw verify` geçer.

## Kapsam dışı
- Rerank (TASK-009), sorguyu yeniden yazma (TASK-010).
- Hibrit (anahtar kelime + vektör) arama.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (bge-m3)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
