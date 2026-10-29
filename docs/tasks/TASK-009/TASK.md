# TASK-009: Rerank 20→5 ve reranker kapalıyken yedek davranış

## Ne isteniyor
Retrieval adımına rerank eklenir: assistant-api retrieval-api'den 20 aday ister, `bge-reranker-v2-m3` (llama-server) ile puanlatır ve en iyi 5 parçayı prompt'a koyar. Reranker erişilemezse asistan durmaz; vector sırasının ilk 5'iyle devam eder ve bunu tura yazar. Lab'ın sağ yarısındaki "retrieval: top-5 / rerank 20→5" ve "reranker: UP/DOWN" düğmelerinin backend karşılığıdır. Ekranda bu düğmeler yoktur; strateji sunucu ayarıdır.

## Kurallar
1. Strateji `assistant.*` yapılandırmasıyla seçilir: `top5` ya da `rerank`. Varsayılan, lab'daki gibi `rerank`'tir. Aday sayısı (20) ve prompt'a giren parça sayısı (5) da yapılandırmadır.
2. Rerank'te 20 adayın hepsi puanlanır. Prompt'a puanı en yüksek 5 parça girer; puan eşitse vector sırası önce gelir.
3. Reranker çağrısı zaman aşımına uğrarsa ya da hata dönerse vector sırasının ilk 5'i kullanılır. Tur, stratejiyi "reranker DOWN · vector sırası" olarak kaydeder; cevap normal akar. Zaman aşımı süresi onay kartında belirlenir.
4. Tur kaydına 20 adayın vector sırası, mesafesi ve rerank puanı eklenir. Prompt'a girenlerin hem vector sırası hem prompt'taki yeri saklanır.
5. Önizleme yan kartında ("Son cevapta modele giden parçalar") rerank stratejisinde her parçanın rerank puanı da görünür ("rerank 0,97 · d 0,312"). Kart alt başlığı stratejiyi lab'daki metinle gösterir: "top-5", "rerank 20→5" ya da "reranker DOWN · vector sırası".
6. Süre çubuklarına "rerank" satırı eklenir.
7. Reranker'a giden metin loglanmaz; aday sayısı ve süre loglanır.

## Örnek senaryolar
- Strateji `rerank`. "Ev kredisini erken kapatırsam ceza öder miyim?" sorusunda vector sırasında 7. olan erken ödeme parçası rerank sonrası prompt'ta 1. sıradadır. Yan kart bunu puanlarla gösterir.
- Reranker konteyneri durdurulur. Aynı soru cevaplanır; tur "reranker DOWN · vector sırası" olarak kaydedilir; prompt'a vector sırasının ilk 5'i girer.
- Strateji `top5` yapılır; rerank çağrılmaz, yan kartta rerank puanı görünmez.

## Kabul kriterleri
- Gerçek reranker ile en az 5 soruda top-5 ve rerank sıraları karşılaştırılmış, sonuç raporda.
- Reranker kapalıyken yedek davranış gerçek sistemde denenmiş.
- Sıralama, eşitlik, zaman aşımı ve yedek davranış testlerle doğrulanmış; ilgili build komutları geçer.

## Kapsam dışı
- Stratejiyi ekrandan değiştirmek (lab kararı: sunucu ayarı).
- Yapılandırma şeridi (TASK-011).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [x] Cevap akışı (SSE) (sources olayı rerank puanı taşıyorsa)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (bge-reranker-v2-m3)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
