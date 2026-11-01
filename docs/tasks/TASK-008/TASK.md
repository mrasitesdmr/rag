# TASK-008: İlk uçtan uca cevap: Önizleme, cevap akışı ve tur kaydı

## Ne isteniyor
Kullanıcı Önizleme ekranındaki telefon görünümünde serbest metinle soru sorar. assistant-api gerçek RAG hattını yürütür:
1. retrieval-api'den 20 aday parça ister,
2. adayları `bge-reranker-v2-m3` (llama-server) ile puanlatıp en iyi 5'ini seçer (rerank 20→5),
3. numaralı parçalarla prompt kurar,
4. `qwen3.5:9b` ile cevap üretir,
5. cevabı token token akıtır (SSE),
6. cevaptaki `[n]` alıntılarını ayrıştırır,
7. turu kaydeder.

Bu task lab'ın sağ yarısındaki "soru → retrieval → rerank → prompt → cevap → alıntı" akışının ilk gerçek hâlidir. Sorguyu yeniden yazma (TASK-010) yoktur. Rerank kuralları eski TASK-009'dan bu task'a katıldı (Kural 12–18).

## Kurallar
1. **Serbest metin:** Kullanıcı istediği soruyu yazar. Lab'daki "Bu soru önizleme kaydında yok" davranışı ve kayıtlı soruya zorlama yoktur. Boş soru gönderilemez; uzunluk sınırı onay kartında belirlenir.
2. **Konuşma:** Önizleme'deki ilk soru yeni bir konuşma açar; sonraki sorular aynı konuşmaya tur olarak eklenir. "Yeni konuşma" düğmesi yeni bir konuşma başlatır. Önizleme'den açılan konuşmanın kanalı "Önizleme"dir. Konuşma kimliği backend'de üretilir.
3. **Karşılama mesajı** lab'daki metindir: "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. …".
4. **Prompt (lab'daki gibi):**
   - Sistem talimatı Türkçedir ve lab'daki metnin aynısıdır: "Sen Raşit Bankası'nın dijital asistanısın. … 1. Sadece <parcalar> içindeki bilgiyle cevap ver … 2. Her cümlenin sonuna … [1] … 3. Cevap parçalarda yoksa sadece \"Bu konuda rehberimizde bilgi bulamadım.\" yaz."
   - Kullanıcı mesajı: `<parcalar>` içinde `[n] başlık` + metin olarak numaralı 5 parça, ardından `Soru: …`.
   - Model ayarları: `temperature 0`, `num_ctx 8192`. Modelin cevap dili Türkçedir.
5. **Akış (SSE):**
   - Cevap token token akar. Olaylar en az `token`, `sources`, `done` ve `error`'dur; ayrıntı ve sıra `docs/api/sse.md`'de tanımlanır (planner yazar).
   - Akış başlamadan oluşan hata Problem Details ile, akış başladıktan sonraki hata `error` olayında aynı `errorCode` ile bildirilir.
   - İstemci bağlantıyı keserse model çağrısı iptal edilir.
   - Akış sürerken giriş alanı ve gönder düğmesi kapalıdır.
6. **Alıntı:**
   - Cevaptaki `[n]` sadece 1 ile prompt'taki parça sayısı arasındaysa alıntıdır ve tıklanabilir düğme olur. Düğme, TASK-007'deki parça penceresini o parçayla açar.
   - Cevabın altında atıf yapılan parçaların sayfaları "Rehber · s. N" düğmeleri olarak gösterilir. Birden fazla belge yüklü olabildiği için "Rehber" yerine parçanın geldiği belgenin adı (ya da kısa adı) yazar; biçimi onay kartında belirlenir.
7. **Bulamadım:** Model sabit cümleyi ("Bu konuda rehberimizde bilgi bulamadım.") yazarsa tur "cevap yok" olarak işaretlenir. Bu durumda "Rehber · s. N" düğmeleri gösterilmez.
8. **Yan kart:**
   - Cevap gelmeden önce lab'daki "Asistan nasıl cevaplıyor?" kartı görünür.
   - Cevaptan sonra "Son cevapta modele giden parçalar" kartı görünür: her parça için `[n]`, sayfa, başlık, sıra no ve mesafe; atıf yapılanlarda ✓.
   - Kartta süre çubukları da vardır (retrieval-api araması, qwen3.5 cevabı, toplam). Süreler gerçek ölçümdür.
   - "Cevabın geçtiği parça" (sarı) işareti bu task'ta yok; TASK-016'da golden set kararıyla gelir.
   - Lab'daki makine etiketinin ("Apple M5 · 16 GB") gösterilip gösterilmeyeceği ve nereden geleceği onay kartında sorulur; sabit yazılmaz.
9. **Tur kaydı** (assistant veritabanı):
   - soru, arama sorgusu (bu task'ta soruyla aynı), üretim modu ve retrieval stratejisi,
   - prompt'a giren her parçanın kaynağı ve sürümü,
   - prompt'a giren parçalar (kimlik, prompt'taki sıra, vector sırası, mesafe), atıf yapılan numaralar,
   - cevap metni, prompt ve cevap token sayısı, adım süreleri, hata varsa `errorCode`.
   Tur, akış bitince yazılır; model çağrısı sırasında veritabanı transaction'ı açık tutulmaz.
10. **Loglama:** Soru, cevap, parça içeriği ve prompt loglanmaz; kimlik, uzunluk, token sayısı ve süre loglanır.
11. retrieval-api ya da dil modeli erişilemezse kullanıcı Türkçe bir hata mesajı görür; tur hata koduyla kaydedilir.
12. **Rerank stratejisi:** Strateji `assistant.*` yapılandırmasıyla seçilir: `top5` ya da `rerank`. Varsayılan, lab'daki gibi `rerank`'tir. Aday sayısı (20) ve prompt'a giren parça sayısı (5) da yapılandırmadır. Ekranda strateji düğmesi yoktur (lab kararı: sunucu ayarı).
13. Rerank'te 20 adayın hepsi puanlanır. Prompt'a puanı en yüksek 5 parça girer; puan eşitse vector sırası önce gelir. `top5` stratejisinde reranker çağrılmaz.
14. **Reranker kapalıyken:** Reranker çağrısı zaman aşımına uğrarsa ya da hata dönerse vector sırasının ilk 5'i kullanılır. Tur, stratejiyi "reranker DOWN · vector sırası" olarak kaydeder; cevap normal akar. Zaman aşımı süresi onay kartında belirlenir.
15. Tur kaydına 20 adayın vector sırası, mesafesi ve rerank puanı eklenir. Prompt'a girenlerin hem vector sırası hem prompt'taki yeri saklanır.
16. Yan kartta ("Son cevapta modele giden parçalar") rerank stratejisinde her parçanın rerank puanı da görünür ("rerank 0,97 · d 0,312"). Kart alt başlığı stratejiyi lab'daki metinle gösterir: "top-5", "rerank 20→5" ya da "reranker DOWN · vector sırası".
17. Süre çubuklarına "rerank" satırı eklenir.
18. Reranker'a giden metin loglanmaz; aday sayısı ve süre loglanır.

## Örnek senaryolar
- Rehberin 2026.10 PDF'i Bilgi kaynakları'ndan yüklenmiştir (TASK-007). Kullanıcı "Kartımı kaybettim, ne yapmalıyım?" yazar. Cevap kelime kelime gelir, rehberin kayıp kart bölümündeki parçaya dayanır ve `[1]` gibi alıntılar taşır. `[1]`'e tıklayınca o parça açılır.
- Kullanıcı "Bitcoin alıp satabilir miyim?" yazar. Cevap "Bu konuda rehberimizde bilgi bulamadım." olur.
- Cevap akarken kullanıcı sayfayı kapatır; assistant-api model çağrısını iptal eder.
- Dil modeli kapalıyken soru sorulur; Türkçe hata mesajı görünür.
- Strateji `rerank`. "Ev kredisini erken kapatırsam ceza öder miyim?" sorusunda vector sırasında 7. olan erken ödeme parçası rerank sonrası prompt'ta 1. sıradadır. Yan kart bunu puanlarla gösterir.
- Reranker durdurulur. Aynı soru cevaplanır; tur "reranker DOWN · vector sırası" olarak kaydedilir; prompt'a vector sırasının ilk 5'i girer.
- Bilgi bankasında hiç belge yokken soru sorulur; parça gelmez ve cevap "Bu konuda rehberimizde bilgi bulamadım." olur. Bu durumda ekranda belge yüklenmesi gerektiğini anlatan bir ipucu gösterilip gösterilmeyeceği onay kartında sorulur.

## Kabul kriterleri
- Gerçek modellerle en az 5 rehber sorusu ve 2 rehber dışı soru denenmiş; cevaplar, alıntılar ve süreler raporda.
- `docs/api/sse.md` yazılmış; backend ve frontend buna uyuyor.
- Gerçek reranker ile en az 5 soruda top-5 ve rerank sıraları karşılaştırılmış, sonuç raporda. Reranker kapalıyken yedek davranış gerçek sistemde denenmiş.
- Alıntı ayrıştırma, bulamadım tespiti, prompt kurma, akış iptali, rerank sıralaması, eşitlik, zaman aşımı ve yedek davranış testlerle doğrulanmış.
- Önizleme ekranı lab'ın sol yarısıyla aynı görünür.
- İki servisin `./mvnw verify`'ı ve web'in `npm run build`, `npm run lint` komutları geçer.

## Kapsam dışı
- Yeniden yazma (TASK-010), yapılandırma şeridi ve Sadece LLM modu (TASK-011).
- Öneri çipleri, kayıtlı sorular penceresi, otomatik tamamlama (TASK-012).
- Geri bildirim ve temsilciye aktarma (TASK-013).
- Konuşmalar ekranı ve tur durumu (TASK-014).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [x] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (qwen3.5:9b, bge-reranker-v2-m3)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
