# TASK-008: İlk uçtan uca cevap: Önizleme, cevap akışı ve tur kaydı

## Ne isteniyor
Kullanıcı Önizleme ekranındaki telefon görünümünde serbest metinle soru sorar. assistant-api gerçek RAG hattını yürütür:
1. retrieval-api'den 5 parça ister (top-5),
2. numaralı parçalarla prompt kurar,
3. `qwen3.5:9b` ile cevap üretir,
4. cevabı token token akıtır (SSE),
5. cevaptaki `[n]` alıntılarını ayrıştırır,
6. turu kaydeder.

Bu task lab'ın sağ yarısındaki "soru → retrieval → prompt → cevap → alıntı" akışının ilk gerçek hâlidir. Rerank (TASK-009) ve sorguyu yeniden yazma (TASK-010) yoktur.

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

## Örnek senaryolar
- Rehberin 2026.10 PDF'i Bilgi kaynakları'ndan yüklenmiştir (TASK-007). Kullanıcı "Kartımı kaybettim, ne yapmalıyım?" yazar. Cevap kelime kelime gelir, rehberin kayıp kart bölümündeki parçaya dayanır ve `[1]` gibi alıntılar taşır. `[1]`'e tıklayınca o parça açılır.
- Kullanıcı "Bitcoin alıp satabilir miyim?" yazar. Cevap "Bu konuda rehberimizde bilgi bulamadım." olur.
- Cevap akarken kullanıcı sayfayı kapatır; assistant-api model çağrısını iptal eder.
- Dil modeli kapalıyken soru sorulur; Türkçe hata mesajı görünür.
- Bilgi bankasında hiç belge yokken soru sorulur; parça gelmez ve cevap "Bu konuda rehberimizde bilgi bulamadım." olur. Bu durumda ekranda belge yüklenmesi gerektiğini anlatan bir ipucu gösterilip gösterilmeyeceği onay kartında sorulur.

## Kabul kriterleri
- Gerçek modellerle en az 5 rehber sorusu ve 2 rehber dışı soru denenmiş; cevaplar, alıntılar ve süreler raporda.
- `docs/api/sse.md` yazılmış; backend ve frontend buna uyuyor.
- Alıntı ayrıştırma, bulamadım tespiti, prompt kurma ve akış iptali testlerle doğrulanmış.
- Önizleme ekranı lab'ın sol yarısıyla aynı görünür.
- İki servisin `./mvnw verify`'ı ve web'in `npm run build`, `npm run lint` komutları geçer.

## Kapsam dışı
- Rerank (TASK-009), yeniden yazma (TASK-010), yapılandırma şeridi ve Sadece LLM modu (TASK-011).
- Öneri çipleri, kayıtlı sorular penceresi, otomatik tamamlama (TASK-012).
- Geri bildirim ve temsilciye aktarma (TASK-013).
- Konuşmalar ekranı ve tur durumu (TASK-014).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [x] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (qwen3.5:9b)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
