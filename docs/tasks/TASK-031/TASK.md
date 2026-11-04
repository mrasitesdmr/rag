# TASK-031: Rehberde karşılığı olmayan mesajlarda uydurma alıntının önlenmesi

## Ne isteniyor
Önizleme'de kullanıcı rehberde karşılığı olmayan bir şey yazdığında (selam, sohbet, konu dışı soru, anlamsız metin) asistan bugün yine parça numarası ekliyor. Örnek: "Selam" → "Merhaba, Raşit Bankası'nın dijital asistanısınız. Size nasıl yardımcı olabilirim? [1]". "Bitcoin alıp satabilir miyim?" → "Bu konuda rehberimizde bilgi bulamadım. [5]" (TASK-018 qa raporu). Cevap, dayanmadığı bir parçaya alıntı vermemelidir. Rehberde cevabı olan sorularda bugünkü davranış (cevap + doğru alıntı) bozulmamalıdır.

Lab'ın sol yarısında Önizleme ekranına karşılık gelir. Sağ yarıdaki akışta retrieval, rerank, prompt kurma, cevap üretme ve alıntı adımlarını etkiler.

## Kurallar
1. **Alıntı sadece dayanılan parçaya verilir:** Cevaptaki bir `[n]` işareti, o cümlenin gerçekten n. parçadaki bilgiye dayandığını gösterir. Rehberde karşılığı olmayan mesajın cevabında hiç alıntı işareti, "Rehber · s. N" düğmesi ve parça işareti (✓) olmaz.
2. **Bulamadım cevabı tektir:** Rehberde karşılığı olmayan soruya cevap sadece "Bu konuda rehberimizde bilgi bulamadım." olur; arkasına numara ya da başka metin eklenmez. Model numara eklese bile kullanıcı numarayı görmez ve tura numarasız kaydedilir. Tur "cevap yok" (BİLMİYOR) olarak işaretlenir (TASK-008 kural 7).
3. **Selamlaşma, teşekkür, vedalaşma:** Asistan kısa ve nazik karşılık verir, rehberle ilgili ne sorulabileceğini söyler; alıntı yoktur. Bu karşılığın sabit metin mi yoksa model cevabı mı olacağı ve turun Konuşmalar'da nasıl görüneceği (BİLMİYOR mı, ayrı bir durum mu) onay kartında sorulur.
4. **AÇIK KARAR, onay kartında kullanıcıya sorulacak: rehberde karşılığı olmadığı nasıl anlaşılır?** Planner seçenekleri sade dille, artı ve eksileriyle ve gerçek RAG projelerindeki yaygın uygulamayla sunar. Seçenekler, örneğin:
   - prompt'a kural eklemek (sadece modelin uymasına güvenir),
   - reranker puanına eşik koymak: en iyi parçanın puanı eşiğin altındaysa dil modeline gidilmeden "bulamadım" cevabı verilir (eşik golden set ve `docs/rehber/rehberde-olmayan-konular.md` sorularıyla ölçülerek seçilir),
   - cevaptaki alıntıların backend'de denetlenmesi (sabit bulamadım cümlesine eklenen numaranın atılması gibi),
   - bunların birleşimi.
   Karar verilmeden plan yazılmaz. Reranker kapalıyken (yedek yol) ve `top5` stratejisinde ne olacağı da kartta yazılır.
5. **Asistan kendinden birinci tekil şahısla söz eder:** "asistanıyım", "yardımcı olabilirim". "Asistanısınız" gibi kullanıcıya dönük yanlış çekim çıkmaz.
6. **Rehberdeki sorular bozulmaz:** Golden set'teki (`rehber-golden-v1`, 100 soru) sorularda cevap gelmeye ve doğru alıntıyı taşımaya devam eder. Değişiklik yüzünden "bulamadım"a düşen rehber sorusu sayısı raporda yazılır ve kabul edilebilir sınır onay kartında belirlenir.
7. Dil modeline giden talimatlar Türkçe kalır; cevap Türkçedir.

## Örnek senaryolar
- Kullanıcı "Selam" yazar. Asistan kısa bir selamla karşılık verir ve rehberle ilgili soru sorabileceğini söyler. Cevapta `[n]` yok, kaynak düğmesi yok.
- Kullanıcı "Bitcoin alıp satabilir miyim?" yazar. Cevap sadece "Bu konuda rehberimizde bilgi bulamadım." olur; numara yok. Tur BİLMİYOR.
- Kullanıcı "Bugün hava nasıl?", "Bana bir şiir yaz", "asdfgh" yazar. Her birinde cevap alıntısız "bulamadım" olur.
- Kullanıcı "Teşekkürler" yazar. Kısa nazik karşılık, alıntı yok.
- Kullanıcı "Kartımı kaybettim, ne yapmalıyım?" yazar. Cevap rehberdeki kart kaybı parçasına dayanır ve o parçanın alıntısını taşır (bugünkü gibi).
- Konuşmada önce "Gold kart aidatı ne kadar?" (cevaplı, alıntılı), sonra "Teşekkürler" yazılır. İkinci cevap alıntısızdır; takip sorusu yeniden yazma (TASK-010) teşekkürü rehber sorusuna çevirmez.

## Kabul kriterleri
- Kural 4'teki karar kullanıcı tarafından verilmiş.
- Gerçek modellerle (rehber 2026.10 yüklü) ölçüm raporda: `rehberde-olmayan-konular.md`'deki örnek sorular + selamlaşma/teşekkür/konu dışı/anlamsız mesajlardan oluşan bir liste için alıntılı cevap sayısı 0.
- Golden set sorularında cevap ve alıntı sonuçları raporda; kural 6'daki sınır sağlanmış.
- Alıntının atılması ve bulamadım tespiti deterministik testlerle doğrulanmış; ilgili build komutları geçer.
- Önizleme ve Konuşmalar'ın görünümü lab'ın sol yarısıyla aynı kalır.

## Kapsam dışı
- Sadece LLM modu (alıntı zaten yok).
- Cevap kalitesinin (faithfulness, doğruluk) model ile ölçülmesi.
- Dil modelinin değiştirilmesi.

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web (kararına göre: selamlaşma turunun Konuşmalar'daki durumu)
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE) (kararına göre: alıntısız cevapta `sources` olayı)
- [x] Dil modeli / prompt / reranker (yeni model yok)
- [ ] Veritabanı (migration; hangi servis?) (kararına göre: yeni tur durumu)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
