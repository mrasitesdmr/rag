# TASK-011: Canlı yapılandırma ve Sadece LLM modu

## Ne isteniyor
assistant-api'nin etkin yapılandırması ekrana gerçek değerleriyle gelir. Önizleme'deki yapılandırma şeridinde Model, Embedding, Retrieval, Soru yeniden yazma ve bilgi bankası değerleri gösterilir; üst bardaki "Canlı yapılandırma" rozeti lab'daki gibi kalır. Ayrıca üretim modu "Sadece LLM" eklenir: retrieval yapılmaz, soru parçasız sistem talimatıyla doğrudan modele gider. Bu, lab'ın sağ yarısındaki "üretim: Sadece LLM / RAG" düğmesinin backend karşılığıdır; ekranda düğme yoktur, sunucu ayarıdır.

## Kurallar
1. Üretim modu `assistant.*` yapılandırmasıdır: `rag` ya da `llm`. Varsayılan `rag`'dir.
2. Sadece LLM modunda sistem talimatı lab'daki kısa metindir: "Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın." Parça gönderilmez, alıntı ayrıştırılmaz, "Rehber · s. N" düğmeleri gösterilmez.
3. assistant-api etkin yapılandırmayı bir uçtan verir:
   - dil modeli adı, embedding modeli ve boyutu,
   - üretim modu, retrieval stratejisi, yeniden yazmanın açık olup olmadığı,
   - bilgi bankasının durumu (retrieval-api'den alınır). Lab'da tek rehber olduğu için şeritte "Rehber 2026.10" yazıyordu. Kullanıcı birden fazla belge yükleyebildiği için ne gösterileceği (ör. "Bilgi kaynakları 3 belge"; tek belge varsa adı ve sürümü; hiç yoksa "belge yok" uyarısı) onay kartında belirlenir.
4. Şeritteki metinler lab'daki gibidir: "Retrieval kapalı (Sadece LLM)", "top-5", "rerank 20→5", "reranker DOWN"; "Soru yeniden yazma açık/kapalı". Lab'daki renk vurguları (kapalı → kırmızı, uyarı → sarı) korunur.
5. "Reranker DOWN" durumu ölçüme dayanır: reranker'ın erişilebilir olup olmadığı periyodik sağlık kontrolüyle ya da son çağrının sonucuyla belirlenir. Hangisi olacağı onay kartında belirlenir.
6. Yan karttaki "Asistan nasıl cevaplıyor?" metni etkin moda göre değişir (lab'daki üç varyant).
7. Sadece LLM modunda tur, retrieval stratejisi olmadan ve "parça yok" bilgisiyle kaydedilir.
8. Yapılandırma ekrandan değiştirilemez; değişiklik ortam değişkeniyle ve servisin yeniden başlatılmasıyla olur.
9. Rozet üzerinde lab'daki ipucu metni yer alır: "Önizleme, canlı asistanla aynı yapılandırmayı kullanır".

## Örnek senaryolar
- Varsayılan ayarlarla, sadece rehber 2026.10 yüklüyken şerit "Model qwen3.5:9b · Embedding bge-m3 · Retrieval rerank 20→5 · Soru yeniden yazma açık" ve bilgi bankası değerini (Kural 3'teki karara göre) gösterir.
- Hiç belge yüklenmemişken şerit bunu gösterir.
- `assistant.generation.mode=llm` ile servis yeniden başlatılır. Şerit "Retrieval kapalı (Sadece LLM)" gösterir. "Kartımı kaybettim" sorusunun cevabında alıntı yoktur ve rehbere dayanmaz.
- Reranker durdurulur; şerit "reranker DOWN" gösterir.

## Kabul kriterleri
- İki mod gerçek modelle denenmiş; aynı soruya RAG ve Sadece LLM cevapları raporda karşılaştırılmış.
- Şerit değerleri yapılandırmadan geliyor; web'de sabit model adı yok (kodda arama ile doğrulanır).
- Testler ve ilgili build komutları geçer.

## Kapsam dışı
- Yapılandırmayı ekrandan değiştirmek.
- Değerlendirme ekranındaki "Ölçümü çalıştır · …" metni (TASK-016; aynı yapılandırma ucunu kullanır).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [x] Docker (ortam değişkenleri)
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
