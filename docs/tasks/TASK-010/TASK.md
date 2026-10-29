# TASK-010: Takip sorusunu yeniden yazma

## Ne isteniyor
Konuşmada önceki turlar varken gelen takip sorusu ("Yenisi kaç günde gelir?") retrieval'dan önce, sohbet geçmişiyle birlikte tek başına anlaşılır bir arama sorgusuna çevrilir ("Kaybolan kartın yenisi kaç günde gelir?"). Arama bu sorguyla yapılır; modele giden soru ise müşterinin yazdığı sorudur. Lab'ın sağ yarısındaki "yeniden yazma" düğmesinin ve adımının backend karşılığıdır; ekranda düğme yoktur, sunucu ayarıdır.

## Kurallar
1. Yeniden yazma `assistant.*` yapılandırmasıyla açılır ya da kapanır. Varsayılan, lab'daki gibi açıktır.
2. Konuşmanın ilk turunda yeniden yazma yapılmaz; arama sorgusu soruyla aynıdır.
3. Yeniden yazma talimatı lab'daki şablondur (İngilizce, Spring AI `CompressionQueryTransformer` kalıbı): "Given the following conversation history and a follow-up query, … Standalone query:". Üretilen sorgunun dili Türkçedir; şablonun Türkçe sorgu üretip üretmediği gerçek modelle doğrulanır, gerekirse onay kartında değişiklik önerilir.
4. Geçmişe kaç önceki turun gireceği onay kartında belirlenir.
5. Yeniden yazma hata verirse ya da boş dönerse arama özgün soruyla yapılır; tur bunu kaydeder; cevap akmaya devam eder.
6. Tur kaydına arama sorgusu ve yeniden yazmanın süresi eklenir.
7. Önizleme yan kartının alt başlığı arama sorgusunu lab'daki gibi gösterir: `· arama: "…"`. Süre çubuklarına "yeniden yazma" satırı eklenir.
8. Yeniden yazılmış sorgu loglanmaz; sadece uzunluğu ve süresi loglanır.

## Örnek senaryolar
- Tur 1: "Kartımı kaybettim, ne yapmalıyım?". Tur 2: "Yenisi kaç günde gelir?". Arama sorgusu kaybolan kartın yenisinin teslim süresini sorar; cevap rehberdeki teslim süresi parçasına dayanır.
- Yeniden yazma kapalıyken aynı tur 2, "Yenisi kaç günde gelir?" diye aranır. Doğru parçanın gelmemesi beklenen bir sonuçtur; bu fark raporda gösterilir.

## Kabul kriterleri
- Gerçek modelle en az 5 takip sorusu denenmiş; arama sorgusu ve getirilen parçalar, yeniden yazma açıkken ve kapalıyken karşılaştırılmış.
- İlk turda atlama, hata durumunda özgün soruya dönüş ve geçmiş sınırı testlerle doğrulanmış; ilgili build komutları geçer.

## Kapsam dışı
- "Bu konuşmada devam" öneri çipleri (TASK-012).
- Konuşmalar ekranında arama sorgusunun gösterilmesi (TASK-014).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [x] Cevap akışı (SSE) (arama sorgusu akışta bildiriliyorsa)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (qwen3.5:9b)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
