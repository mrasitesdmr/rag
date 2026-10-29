# TASK-003: Lokal modeller (dil modeli, embedding, reranker)

## Ne isteniyor
Projenin kullanacağı üç lokal model kurulur, çalışır hâle gelir ve servislerin erişebileceği adreslerde durur: dil modeli ve embedding Ollama'da, reranker llama-server'da. Kurulum ve kullanım `ollama.md`'de anlatılır. Lab'ın sağ yarısındaki mimari haritada görünen model kutularının (dil modeli, embedding modeli, reranker) gerçek karşılığı budur.

## Kurallar
1. Lab'daki modeller başlangıç önerisidir: dil modeli `qwen3.5:9b`, embedding `bge-m3` (1024 boyut), reranker `bge-reranker-v2-m3`. Kesin modeller ve sürümleri onay kartında kullanıcıya sorulur; indirilecek her model bir plan kararıdır.
2. Lab'ın adresleri: Ollama 11434, llama-server 8081. Servisler model adresini ve model adını ortam değişkeninden alır; kodda sabit adres yoktur.
3. Makine Apple Silicon, 16 GB. Lab'ın kayıt notunda üç model aynı anda yüklenince swap'e düşüldüğü yazıyor. Onay kartında modellerin nerede çalışacağı (host ya da Docker; Docker'ın Mac'te GPU kullanamaması), aynı anda bellekte kalıp kalmayacakları (keep-alive) ve ölçülen bellek kullanımı yer alır.
4. Dil modeli ayarları lab'daki gibidir: `temperature 0`, `num_ctx 8192`.
5. Her model için tek komutluk bir duman testi (smoke test) vardır:
   - embedding testi 1024 boyutlu vektör döner,
   - dil modeli testi Türkçe kısa bir cevap döner,
   - reranker testi verilen sorgu ve iki metin için iki skor döner; ilgili metin daha yüksek skor alır.
6. `ollama.md`: kurulum, model indirme, başlatma/durdurma, duman testleri, bellek notları ve sorun giderme.
7. Bu task'ta uygulama kodu modellere bağlanmaz; bağlantılar ilgili task'larda kurulur.

## Örnek senaryolar
- Kullanıcı `ollama.md`'deki adımları izler; üç duman testi de beklenen çıktıyı verir.
- Kullanıcı Compose'u başlatır; reranker 8081'de, Ollama 11434'te cevap verir.
- Reranker durdurulur; reranker duman testi bağlantı hatası verir. Bu, TASK-009'daki yedek davranışın sınanacağı durumdur.

## Kabul kriterleri
- Üç duman testi çalıştırılmış, çıktıları raporda.
- Üç model birlikte yüklüyken ölçülen bellek kullanımı raporda.
- `ollama.md` yazılmış; içindeki her komut bu makinede gerçekten çalışıyor.

## Kapsam dışı
- Spring AI ile model çağrısı (TASK-005, TASK-008, TASK-009).
- Model ince ayarı, quantization karşılaştırması.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (evet, üç model)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
