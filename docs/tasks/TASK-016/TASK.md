# TASK-016: Değerlendirme (golden set, recall@5, MRR)

## Ne isteniyor
Lab'ın Değerlendirme ekranı gerçek ölçümle çalışır. Golden set, rehberde cevabı olan ve her birinde cevabın geçtiği parçası işaretli sorulardan oluşur. "Ölçümü çalıştır" düğmesi etkin retrieval stratejisiyle her soru için retrieval'ı gerçekten çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar. Sonuçlar ekranda gösterilir:
- recall@5, MRR, Kaçan ve Retrieval kutuları,
- "Kaçanlar ve sırası değişenler" tablosu.

Lab'daki `POST /api/assistant/evals` (202 + polling) akışının gerçek hâlidir.

## Kurallar
1. **Golden set gerçek veridir:** Sorular TASK-004'teki rehbere göre yazılır; lab'daki 149 soru eski rehbere aittir ve kullanılmaz. Her soru için kategori ve cevabın geçtiği yer işaretlenir. Golden set'in kim tarafından ve nasıl hazırlanacağı, kaç soru olacağı ve sürümlenmesi (lab'da `rehber-golden-v1`) onay kartında yazılır.
2. **AÇIK KARAR, bu task'a gelindiğinde kullanıcıya yeniden sorulacak: doğru parça neyle işaretlenir?**
   - Lab doğru parçayı parça kimliğiyle işaretliyordu.
   - Gerçek sistemde parça kimlikleri yeni sürüm yüklenince (TASK-015) değişir; bu yüzden kimlikle işaretlenen golden set her yeni sürümde bozulabilir.
   - Kullanıcı "kimlik sabit olabilir" ihtimalini düşünüyor ama karar vermedi.
   - Planner bu task'ın onay kartında seçenekleri sade dille, artı ve eksileriyle sunar ve gerçek RAG projelerindeki yaygın uygulamayı belirtir. Seçenekler, örneğin:
     - parça kimliği, sürümler arasında içerik aynıysa kimlik korunarak,
     - sayfa + metin çapası (cevabın geçtiği cümle),
     - beklenen cevap olgusu (ör. "1.650 TL"),
     - bunların birleşimi.
   - Karar verilmeden plan yazılmaz.
3. **Metrikler:** recall@5 (doğru parçası modele giden 5 parçada olan soruların oranı) ve MRR (doğru parçanın prompt'taki sırasının tersinin ortalaması). Lab'daki gibi recall@5 0,95 ve üzeriyse yeşil, altıysa kırmızı gösterilir. Metrik hesabı deterministik testlerle doğrulanır.
4. **Strateji:**
   - Ölçüm etkin yapılandırmayla çalışır (TASK-011). Düğme metni stratejiyi gösterir: "Ölçümü çalıştır · rerank 20→5" ya da "· top-5"; değer backend'den gelir.
   - Sadece LLM modunda düğme kapalıdır ve lab'daki uyarı gösterilir: "Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al."
   - Reranker kapalıyken ölçüm yapılırsa sonuç bunu kaydeder.
5. **Çalıştırma:**
   - 202 ile başlar; ekran polling ile ilerlemeyi izler.
   - Sürerken düğme kapalıdır; aynı anda ikinci çalıştırma 409 ile reddedilir.
   - Sayfa yenilense de süren çalıştırma bulunur.
   - Çalıştırma kaydı şunları içerir: golden set sürümü, strateji, rehber sürümü, soru sayısı, her soru için vector sırası ve prompt'taki yer, metrikler, süre.
6. **Ölçüm sadece retrieval'ı ölçer:** Dil modeli cevap üretmez (lab'daki tanım).
7. **Ekran:**
   - Kutular: recall@5 (x/n soru), MRR ("doğru parçanın sırası"), Kaçan ("doğru parça prompt'ta yok"), Retrieval (strateji ve rehber sürümü).
   - "Kaçanlar ve sırası değişenler" tablosunda Soru, Vector # ("20+" dahil) ve Prompt'taki yeri ("N. sıra" ya da "yok") sütunları vardır; en fazla 24 satır gösterilir.
   - Bir satıra tıklayınca soru Önizleme'de sorulur.
   - Hiç ölçüm yoksa lab'daki metin gösterilir: "Henüz ölçüm yok. …".
   - Önceki çalıştırmalar karşılaştırma için saklanır; geçmişin ekranda nasıl görüneceği (lab'da sadece son çalıştırma var) onay kartında belirlenir.
8. **Golden set ile diğer ekranlar:** Önizleme'deki "sarı: cevabın geçtiği parça" işareti ve Konuşmalar'daki "cevap burada" etiketi, soru golden set'te varsa gösterilir. Eşleşmenin nasıl yapılacağı (tam metin ya da benzerlik) onay kartında belirlenir; TASK-014'teki tur durumu kararıyla tutarlı olur.

## Örnek senaryolar
- Varsayılan stratejiyle (rerank 20→5) ölçüm çalıştırılır. Kutular gerçek recall@5 ve MRR'yi gösterir. Kaçan sorular tabloda "yok" olarak listelenir.
- Strateji `top5` yapılıp ölçüm tekrarlanır; iki çalıştırma karşılaştırılabilir.
- 2026.11 yüklendikten sonra ölçüm çalıştırılır; doğru parça işareti (Kural 2'deki karara göre) hâlâ geçerlidir ve ölçüm bozulmaz.

## Kabul kriterleri
- Kural 2'deki karar kullanıcı tarafından verilmiş.
- Golden set hazırlanmış ve sürümlenmiş; içeriği kullanıcıya gösterilmiş.
- Gerçek sistemde iki stratejiyle ölçüm yapılmış, sonuçlar raporda.
- recall@5 ve MRR hesabı bağımsız türetilmiş beklenen değerlerle test edilmiş; ilgili build komutları geçer.
- Görünüm lab'ın sol yarısıyla aynı.

## Kapsam dışı
- Cevap kalitesinin (faithfulness, doğruluk) model ile ölçülmesi; gerekiyorsa ayrı task.
- Golden set'i ekrandan düzenlemek (lab'da yok).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama) (doğru parça işaretinin karşılığı)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (embedding ve reranker)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
