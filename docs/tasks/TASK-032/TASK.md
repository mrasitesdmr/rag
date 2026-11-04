# TASK-032: Konu dışı ve kısa mesajlarda yanlış cevabın düzeltilmesi

## Ne isteniyor
Önizleme'de rehberle ilgili sorular doğru cevaplanıyor ama konu dışı, kısa ya da selamlaşma mesajlarında asistan yanlış cevap veriyor. Lokal sistemdeki gerçek turlardan örnekler (2026-11-04):
- "Ahmet" → "Merhaba Ahmet, … [1]" (uydurma alıntı)
- "Hava durumunu verir misin?" → "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum."
- "Nber" → "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum."
- "Kredi limitim ne kadar" → sadece "Hesabına özel bilgileri … göremiyorum."; rehberdeki genel bilgi (limit artırma Raşit Mobil'de Kartlarım > Limit işlemleri) eklenmedi. Kullanıcı 👎 verdi.

Kök nedenler: reranker eşiği (-4,0) çok gevşek; "hesap bilgisi / başka banka / yatırım" giriş cümlesi kuralı "bulamadım" kuralından önce geldiği için model bu cümleleri genel ret olarak kullanıyor; "her cümleye numara" kuralı selamlaşmaya alıntı ekletiyor; takip sorusunu yeniden yazma, asistanın önceki cevabını arama sorgusuna karıştırıyor.

Selamlaşma, teşekkür ve vedalaşma için kelime listesi (TASK-031) kaldırılır. Bu mesajları, dil modeline sadece etiket soran sınıflandırma adımı yakalar. Eşik tek başına yetmez: 2026-11-04 ölçümünde "Merhaba" reranker'da 1,18, "Günaydın" -0,75 aldı; gerçek bir rehber sorusu ("Ek kart ücretli mi", yeniden yazılmış hâli) 0,65 aldı.

Lab'ın sol yarısında Önizleme ve Konuşmalar ekranına karşılık gelir. Sağ yarıdaki akışta sorguyu yeniden yazma, rerank, prompt kurma ve cevap üretme adımlarını etkiler.

## Kurallar
1. **Kelime listesi kalkar:** `small-talk/` altındaki selamlaşma, teşekkür, vedalaşma ve hitap listeleri ve bunlarla yapılan eşleştirme kaldırılır. Selamlaşmayı kural 2 ve 4'teki etiketleme yakalar. "Selam" gibi bir mesajın cevabı bugün anında geliyor, artık yaklaşık 1 sn sürebilir; bu kabul edilir.
2. **Kısa mesaj her zaman etiketlenir:** Kısa mesaj, eşikten ve konuşmanın ilk mesajı olup olmamasından bağımsız olarak, aramadan önce dil modeline sadece etiket olarak sorulur (selam / teşekkür / veda / diğer). Bugün bu adım sadece konuşmanın devamındaki kısa mesajlarda çalışıyor; ilk mesaja da genişler. Selam, teşekkür ya da veda çıkarsa sabit karşılık (bugünkü metinler) verilir, arama yapılmaz, alıntı olmaz. Diğer çıkarsa akış normal devam eder. Kısa mesajlara yaklaşık 1 sn eklenmesi kabul edilir.
3. **Kısa mesajın tanımı:** Bugünkü tanım "en fazla 10 kelime ve soru işareti yok". Soru işaretli selamlaşmalar ("Nasılsın?", "Merhaba, iyi misin?") da yakalanmalıdır; tanımın nasıl değişeceği onay kartında sorulur.
4. **Eşiğin altı yine etiketlenir:** Uzun mesajda en iyi parçanın reranker puanı eşiğin altındaysa bugünkü gibi etiket sorulur. Selam, teşekkür, veda ise sabit karşılık, diğer ise "Bu konuda rehberimizde bilgi bulamadım." verilir; alıntı yoktur.
5. **Eşik ölçülerek seçilir:** Yeni eşik değeri golden set (`rehber-golden-v1`, 100 soru) ve aşağıdaki konu dışı listeyle ölçülerek seçilir. Planner ölçüm yöntemini ve aday değerleri onay kartında sunar; kesin değer ölçüm sonucuyla kullanıcıya onaylatılır. 2026-11-04 ölçümü yol gösterir: konu dışı sorular -8,28 ("Hava durumunu verir misin?"), -8,24 ("Bana bir şiir yaz"), -7,12 ("Bitcoin alabilir miyim?"); rehber soruları 0,65 ile 4,89 arası.
6. **Giriş cümleleri sadece kendi konusunda kullanılır:** "Hesabına özel bilgileri …", "Başka bankaların …", "Yatırım tavsiyesi …" cümleleri sadece mesaj gerçekten o konudaysa kullanılır. Konu dışı başka her soruda (hava durumu, şiir, sohbet) bu cümleler çıkmaz.
7. **Hesap bilgisi sorusunda genel bilgi eklenir:** Kullanıcı kendi limitini, bakiyesini ya da borcunu sorduğunda, giriş cümlesinden sonra rehberde ilgili genel bilgi varsa (nereden görülür, nasıl değiştirilir) alıntısıyla eklenir.
8. **Alıntısız cümle kuralı:** Parçaya dayanmayan cümleye numara eklenmez. Selamlaşma ya da isim söyleme gibi mesajların cevabında `[n]` olmaz.
9. **Yeniden yazma kullanıcının niyetini korur:** Takip sorusu yeniden yazılırken asistanın önceki cevabındaki metin (selamlama, kendini tanıtma) arama sorgusuna girmez. Önceki konudan bağımsız yeni bir soru (ör. selamlaşmadan sonra "Hava durumunu verir misin?") olduğu gibi kalır. Önceki cevabın yeniden yazmaya hiç verilip verilmeyeceği onay kartında sorulur.
10. Dil modeline giden talimatlar Türkçe kalır; cevap Türkçedir. Yeni model indirilmez.

## Örnek senaryolar
- "Selam", "Merhaba", "Günaydın", "Nber", "Nasılsın?", "Teşekkürler", "Görüşürüz" → kısa mesaj etiketlenir, sabit karşılık gelir, arama yapılmaz, alıntı yok.
- "Ahmet" → alıntısız cevap (selamlaşma karşılığı ya da bulamadım; hangisi olacağı onay kartında sorulur).
- "Selam" → "Hava durumunu verir misin?" → "Bu konuda rehberimizde bilgi bulamadım."; yatırım cümlesi çıkmaz.
- "Kredi limitim ne kadar" → "Hesabına özel bilgileri … göremiyorum." + rehberin 2.6 bölümüne dayanan genel bilgi ve alıntısı.
- "Ek kart nasıl alabilirim" → "Peki ücretli mi" → bugünkü gibi doğru cevap ve alıntı.
- "Bitcoin alabilir miyim?", "jjkbjknlk" → bulamadım, alıntı yok.

## Kabul kriterleri
- Eşik değeri ve kısa mesaj tanımı kullanıcı tarafından onaylanmış.
- Gerçek modellerle (rehber 2026.10 yüklü) ölçüm raporda: yukarıdaki senaryolar + `docs/rehber/rehberde-olmayan-konular.md` sorularından oluşan listede yanlış giriş cümlesi ve alıntılı cevap sayısı 0; selamlaşma/teşekkür/veda mesajlarının hepsi sabit karşılık alır.
- Golden set'te recall@5 ve "bulamadım"a düşen rehber sorusu sayısı raporda; kabul edilebilir sınır onay kartında belirlenir.
- Kelime listesi dosyaları ve eşleştirme kodu kaldırılmış; ilgili testler güncel; build komutları geçer.
- Önizleme ve Konuşmalar'ın görünümü lab'ın sol yarısıyla aynı kalır.

## Kapsam dışı
- Dil modelinin ya da reranker'ın değiştirilmesi.
- Sadece LLM modu.
- Cevap kalitesinin (faithfulness, doğruluk) model ile ölçülmesi.

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (yeni model yok)
- [ ] Veritabanı (migration; hangi servis?) (planner karar verir: tur kaydında kelime listesi yolunun izi varsa)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
