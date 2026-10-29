# TASK-005: Rehberin ilk içe alınması (retrieval-api)

## Ne isteniyor
retrieval-api'de bilgi bankasının temeli kurulur: çalışma alanı, kaynak, sürüm, parça ve içe alma işi. Rehberin 2026.10 sürümü (TASK-004) gerçek içe alma hattından geçerek bilgi bankasına girer: PDF okunur, sayfalara ve parçalara ayrılır, her parça `bge-m3` ile embed edilip pgvector'e yazılır. Bu, lab'ın sağ yarısındaki "içe alma" akışının backend'deki karşılığıdır. Ekran bu task'ta yok.

## Kurallar
1. **Çalışma alanı:** Tek çalışma alanı vardır: 42, "Raşit Bankası · Dijital Asistan". Migration ile gelir. Embedding modeli çalışma alanı düzeyinde tektir: `bge-m3@1024`.
2. **İş durumları:** İçe alma işi şu sırayla ilerler: `QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE`. Hata olursa iş `FAILED` olur ve sabit bir `errorCode` taşır. İşin durumu `GET /api/workspaces/42/ingestion-jobs/{id}` ile sorgulanır. Cevapta aşama, ilerleme oranı (EMBEDDING'de embed edilen ve toplam parça sayısı), parça sayısı ve zaman damgaları (oluşma, başlama, bitiş) bulunur.
3. **İş arka planda yürür:** İşi başlatan istek hemen döner. İş, servis yeniden başlarsa yarıda kalmış hâlde unutulmaz; nasıl ele alınacağı (yeniden kuyruğa alma ya da `FAILED`) onay kartında yazılır.
4. **Parçalama (lab'daki değerler):** hedef 1.600, en fazla 2.000 karakter, 200 karakter örtüşme. Parça bölüm başlığını ve sayfa numarasını taşır; bir parça mümkünse bölüm sınırında biter. Her parçada sıra no, sayfa no, başlık, metin ve token sayısı bulunur. Tablolar parçalanırken satırlar bölünmez.
5. **Embedding:** Parçalar 96'lık gruplar hâlinde embed edilir; modele aynı anda giden istek sayısı sınırlıdır. Vektörler 1024 boyuttadır; farklı boyut gelirse iş `FAILED` olur.
6. **Index:** pgvector, HNSW, cosine.
7. **İlk giriş:** Lab'ın sol yarısında "kaynak ekle" ekranı yoktur, sadece "Yeni sürüm yükle" vardır. Rehberin bilgi bankasına ilk kez nasıl gireceği onay kartında kullanıcıya sorulur. Seçenekler, örneğin: servis açılışında yapılandırılmış dosyadan otomatik içe alma ya da ekransız bir yönetim ucu. Her durumda rehber gerçek içe alma hattından geçer; tabloya elle ya da seed ile parça yazılmaz.
8. **Tekrar açılış:** Servis yeniden başladığında ikinci bir kaynak ya da mükerrer parça oluşmaz. Kaynak kimliği sequence'ten gelir (ilk kaynak 10001).
9. **Dosya saklama:** Yüklenen PDF ve özellikleri (dosya adı, bayt, sayfa sayısı, bölüm sayısı) saklanır; Bilgi kaynakları ekranındaki "PDF'i aç" bunu kullanır (TASK-007).
10. **Sürüm:** Her içe alma bir sürüm etiketiyle (ör. `2026.10`) kaydedilir. "Son senkron" zamanı işin bitiş zamanıdır.
11. **Loglama:** Parça içeriği loglanmaz; iş kimliği, aşama, parça sayısı ve süre loglanır.

## Örnek senaryolar
- Sistem ilk kez açılır. Rehber 2026.10 için bir içe alma işi oluşur ve aşamaları geçip `DONE` olur. Bilgi bankasında 10001 numaralı kaynak, `2026.10` sürümü ve her biri 1024 boyutlu embedding'e sahip parçalar vardır.
- Embedding modeli kapalıyken iş başlar. İş EMBEDDING aşamasında `FAILED` olur ve `errorCode` (ör. `EMBEDDING_UNAVAILABLE`) taşır; yarım parça yazılmaz.
- Sistem yeniden başlatılır; kaynak sayısı 1 kalır.

## Kabul kriterleri
- Uçtan uca içe alma gerçek PDF ve gerçek `bge-m3` ile çalıştırılmış; parça sayısı, ortalama token ve süre raporda.
- Rastgele seçilmiş parçaların sayfa numarası ve başlığı PDF ile elle karşılaştırılmış.
- Parçalama kuralları, durum geçişleri ve hata durumları testlerle doğrulanmış.
- `./mvnw verify` geçer.

## Kapsam dışı
- Vektör arama (TASK-006).
- Bilgi kaynakları ekranı ve okuma uçları (TASK-007).
- Yeni sürüm yükleme ve eski parçaların değişmesi (TASK-015).

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (bge-m3)
- [x] Veritabanı (migration; hangi servis?) (retrieval)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
