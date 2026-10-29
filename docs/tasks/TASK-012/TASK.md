# TASK-012: Önizleme soru önerileri (öneri çipleri, soru listesi, otomatik tamamlama)

## Ne isteniyor
Lab'ın Önizleme ekranındaki yardımcı öğeler gerçek bir veri kaynağıyla çalışır:
- giriş alanının üstündeki öneri çipleri,
- "Kayıtlı sorular" penceresi (kategorilere göre soru listesi),
- yazarken açılan otomatik tamamlama listesi ("Bu konuşmada devam" / "Yeni konuşmada").

Lab'da bu öğeler kaydedilmiş 149 soru üzerinden çalışıyordu, çünkü statik sayfa modeli çalıştıramıyordu. Gerçek üründe kullanıcı her zaman serbest metin yazar (TASK-008); bu öğeler sadece öneridir, soru sormayı kısıtlamaz.

## Kurallar
1. **Önerilerin kaynağı:** Mock ya da koda gömülü soru listesi yoktur. Önerilerin nereden geleceği onay kartında kullanıcıya sorulur. Seçenekler, örneğin:
   - ürün ekibinin yönettiği öneri soruları (veritabanında, kategoriyle),
   - golden set'ten türetilen sorular (TASK-016'ya bağımlı),
   - geçmiş konuşmalardaki sık sorular.
   Gerçek bankacılık asistanlarındaki uygulama araştırılıp öneri yapılır.
2. **Kategoriler** lab'daki gibidir: Kartlar, Hesaplar, Krediler, Transfer, Dijital, Yatırım, Diğer.
3. **Öneri çipleri:** En fazla 2 çip gösterilir. Konuşmada önceki tur varsa ilk çip bir takip sorusudur ("Bu konuşmada devam"). Takip önerilerinin nasıl üretileceği (önceki cevaba göre dil modeliyle, ya da öneri listesindeki bağlantılarla) onay kartında belirlenir. Sorulmuş sorular yeniden önerilmez. Cevap akarken çipler kapalıdır.
4. **Takip mi, yeni konuşma mı:** Lab'da takip çipi aynı konuşmada sorulur, diğer çipler "Yeni konuşmada sorulur" ipucuyla yeni konuşma açar. Bu davranış korunur.
5. **Otomatik tamamlama:**
   - Yazdıkça en fazla 7 eşleşen öneri listelenir. Eşleşme Türkçe eklere dayanıklıdır (lab'da kök + IDF ağırlığı vardı; gerçek yöntem onay kartında).
   - Liste klavyeyle kullanılabilir: ↑ ↓ ile gezinme, Enter ile seçme, Esc ile kapatma.
   - Eşleşme yoksa liste kapanır ve kullanıcı yazdığını gönderebilir. Lab'daki "listeden seç" uyarısı yoktur.
6. **Kayıtlı sorular penceresi:** Başlık lab'daki gibidir: "Kayıtlı sorular · N soru, M takip". Önce "Bu konuşmada devam", sonra kategoriler gelir. Bir soruya tıklayınca soru sorulur ve pencere kapanır. Gerçek kaynağa göre başlık metni değişecekse onay kartında belirtilir.
7. Erişilebilirlik: liste `listbox`/`option` rolleriyle, pencere odak yönetimiyle çalışır.

## Örnek senaryolar
- Boş konuşmada iki başlangıç önerisi görünür. Kullanıcı birine tıklar; soru sorulur.
- "Kartımı kaybettim" cevabından sonra ilk çip "Yenisi kaç günde gelir?" gibi bir takip önerisidir ve aynı konuşmada sorulur.
- Kullanıcı "asgari" yazar; asgari ödemeyle ilgili öneriler listelenir. ↓ ve Enter ile birini seçer.
- Kullanıcı listede olmayan bir soru yazıp Enter'a basar; soru normal şekilde sorulur.

## Kabul kriterleri
- Önerilerin kaynağı kullanıcı onayıyla seçilmiş; kodda sabit soru listesi yok.
- Klavye etkileşimi ve erişilebilirlik testleri yazılmış; ilgili build komutları geçer.
- Görünüm lab'ın sol yarısıyla aynı.

## Kapsam dışı
- Önerileri ekrandan yönetmek (lab'da yönetim ekranı yok; gerekiyorsa ayrı task).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (takip önerisi modelle üretilirse evet)
- [x] Veritabanı (migration; hangi servis?) (assistant, öneriler veritabanındaysa)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
