# TASK-XXX: <Kısa başlık>

> AMAÇ: Yeni bir geliştirme talebinin şablonu. Kopyala: `docs/tasks/TASK-XXX/TASK.md`.
> Bu dosyayı SEN yazarsın. Planner buradan başlar. Teknik detay yazma; iş kuralını yaz.

## Ne isteniyor
<!-- 1-3 cümle. Kullanıcı açısından ne değişecek? Lab'ın sol yarısındaki (kaynaklar/rag-lab.html) hangi ekrana karşılık geliyor? Sağ yarıdaki akışın hangi adımını backend'e ekliyor? -->

## Kurallar
<!-- Numaralı, test edilebilir kurallar. Sayı ve sınır değerleri net yaz. -->
1.
2.

## Örnek senaryolar
<!-- Somut örnekler: kim, ne sordu / ne yaptı, ne oldu. Dil modeli içeren senaryoda cevabın tam metnini değil, neye dayanması gerektiğini yaz (ör. "cevap rehberin 7. sayfasındaki parçaya dayanır ve [1] alıntısı taşır"). -->
-

## Kabul kriterleri
<!-- Bitti saymak için ne gerekli? -->
-

## Kapsam dışı
<!-- Bu task'ta YAPILMAYACAKLAR. -->
-

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
