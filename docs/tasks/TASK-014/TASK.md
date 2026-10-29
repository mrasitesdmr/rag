# TASK-014: Konuşmalar ekranı ve tur durumu

## Ne isteniyor
Lab'ın Konuşmalar ekranı ve konuşma detay ekranı gerçek konuşmalarla çalışır. Önizleme'de açılan her konuşma burada görünür. Detayda her tur için şunlar görülür:
- soru ve arama sorgusu,
- cevap ve alıntılar,
- durum, üretim modu, rehber sürümü, token sayıları ve süre,
- modele giden parçaların tablosu.

Ekranın amacı lab'daki açıklamadır: "Yanlış bir cevapta önce doğru parçanın prompt'a girip girmediğine bakılır."

## Kurallar
1. **Örnek konuşma yok.** Lab'daki SEED konuşmalar (cnv_48188 vb.) gelmez; liste sadece gerçek konuşmalardan oluşur. Boş durum için Türkçe bir metin gösterilir (lab'da karşılığı yok; `frontend-design` ile tasarlanır).
2. **Liste (`/konusmalar`):**
   - Tablo sütunları: Konuşma, İlk soru (60 karaktere kısaltılmış), Tur, Durum (son turun durumu), Kanal, Saat.
   - En yeni konuşma üstte; `PageResponse` ile sayfalı.
   - Yan menüdeki Konuşmalar sayacı gerçek konuşma sayısını gösterir.
3. **Detay (`/konusmalar/{id}`):**
   - Breadcrumb; başlıkta konuşma kimliği; alt başlıkta "Kanal · saat · N tur".
   - Her tur için:
     - sıra no ve soru; arama sorgusu soru ile farklıysa "Arama sorgusu: …",
     - alıntılı cevap,
     - durum çipi, üretim ("Sadece LLM", "RAG · top-5", "RAG · rerank 20→5", "RAG · reranker DOWN"), rehber sürümü, prompt ve cevap token sayısı, süre,
     - parça tablosu: Prompt [n], Parça (sayfa · sıra no), Vector #, Mesafe, Rerank (sadece rerank turlarında), Atıf ✓.
   - Sadece LLM turunda tablo yerine "Bu turda modele hiçbir parça gitmedi (Sadece LLM)." uyarısı gösterilir.
   - Satıra tıklayınca parça penceresi açılır.
   - Geri bildirim ve temsilciye aktarma bilgisi turda görünür (TASK-013).
4. **Akışı süren tur** "işleniyor" çipiyle gösterilir.
5. **Tur durumu, açık soru (onay kartında kullanıcıya sorulur):** Lab'da altı durum var: DAYANAKLI, BİLMİYOR, KAÇIRDI, ÜRETİM HATASI, HATALI, DAYANAKSIZ.
   - Lab bunları elle işaretlenmiş verilerle hesaplıyordu: hangi sorunun cevabı rehberde var, doğru parça hangisi, hangi cevap insan tarafından "hatalı" bulundu.
   - Serbest metin sorularda bu bilgi her zaman yoktur.
   - Gerçek üründe her durumun nasıl belirleneceği planner'ın araştırıp önerdiği seçeneklerle kullanıcıya sorulur. Seçenekler, örneğin:
     - modelin "bulamadım" cevabı ve alıntı varlığından hesaplananlar,
     - golden set'teki sorular için doğru parçayla karşılaştırma (TASK-016'ya bağlı),
     - "Hayır" geri bildirimi ya da bir değerlendirici modelin (LLM-as-judge) kararı.
   - Belirlenemeyen durum gösterilmez; sahte durum üretilmez.
6. Konuşma ya da tur bulunamazsa Türkçe mesaj gösterilir.

## Örnek senaryolar
- Kullanıcı Önizleme'de iki turluk bir konuşma yapar. Konuşmalar'da en üstte bu konuşma "2 tur · Önizleme" olarak görünür.
- Detayda 2. turun "Arama sorgusu" satırı yeniden yazılmış sorguyu gösterir. Parça tablosunda doğru parçanın vector sırası 7, prompt'taki yeri [1]'dir.
- Rehberde olmayan bir soruda tur, onaylanan kurala göre ("BİLMİYOR" ya da karşılığı) işaretlenir.

## Kabul kriterleri
- Tur durumu kuralları kullanıcı onayıyla belirlenmiş ve testlerle doğrulanmış.
- Liste ve detay gerçek turlardan oluşuyor; mock ya da seed konuşma yok.
- Görünüm lab'ın sol yarısıyla aynı; ilgili build komutları geçer.

## Kapsam dışı
- Konuşma silme, dışa aktarma, filtreleme (lab'da yok).
- Raşit Mobil gibi gerçek bir müşteri kanalı (bu projede tek kanal Önizleme'dir).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (durum için değerlendirici model seçilirse evet)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
