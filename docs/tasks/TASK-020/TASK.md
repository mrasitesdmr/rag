# TASK-020: Rehber olgu tutarlılığı taraması

## Ne isteniyor
TASK-004'te yazılan Ürün ve Hizmet Rehberi'nde (`docs/rehber/`) aynı olgu birden fazla yerde geçiyor: bölüm metni, "Sık sorulanlar" ve 10. bölümdeki ücret tarifesi. Bu task'ta bu yerlerin birbiriyle tutarlı olduğu otomatik olarak kontrol edilir. Mevcut kontrol (`dogrula.sh`) yalnızca listelenen dizeleri arıyor. Kaynak: TASK-004 plan Riskler 1 ve QA raporu Optional 1 (`docs/tasks/TASK-004/handoff/05-qa-report.md`).

## Kurallar
1. Ücret tarifesindeki (10.1-10.7) her satırın değeri, aynı ücretin bölüm metninde ya da bölüm tablosunda geçtiği her yerde aynıdır. Örnek: kâğıt ekstre 4,90 TL; 2.7, 2. bölümün SSS'i ve 10.1'de aynı.
2. SSS cevaplarındaki tutar, oran, süre ve limitler, atıf yaptıkları başlıktaki (`bkz. X.Y`) değerlerle aynıdır.
3. Kontrol iki sürümde (2026.10 ve 2026.11) de çalışır. Sürüme göre değişen değerler `kaynak/surum.typ`'den gelir; değişiklik özeti doğru kalır.
4. Kontrol `dogrula.sh`'nin parçası olur ve diğer kontroller gibi GEÇTİ ya da KALDI yazar. Bir kez bilerek tutarsızlık eklenerek KALDI dediği gösterilir.
5. Tarama bir tutarsızlık bulursa düzeltilir. Değişen PDF'ler yeniden üretilir ve bütün kontroller yeniden çalıştırılır. Değişen olgu kullanıcıya gösterilir.
6. Olgularda yapılacak her değişiklik (örneğin bir ücretin farklı yazıldığı ortaya çıkarsa hangi değerin doğru olduğu) kullanıcı onayıyla yapılır.

## Örnek senaryolar
- Tarifede Raşit Platinum ek kart ücreti 750 TL, 2.3'te de 750 TL. Kontrol GEÇTİ der.
- Biri 2. bölümün SSS'ine "nakit avans işlem ücreti %1,5" yazar; 2.10 ve 10.1'de %1,25 yazıyor. Kontrol KALDI der ve iki yeri gösterir.

## Kabul kriterleri
- `./docs/rehber/dogrula.sh` yeni kontrolle birlikte bütün kontrollerden geçiyor.
- Negatif deneme raporda: bilerek eklenen tutarsızlık KALDI verdi.
- Bulunan ve düzeltilen tutarsızlıklar (varsa) raporda listeli ve kullanıcı onaylamış.

## Kapsam dışı
- Yeni içerik ya da yeni bölüm eklemek.
- Kontrol betiklerinin genel sağlamlaştırılması (TASK-021).
- Mastercard adının değiştirilmesi (TASK-023).

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
- [x] Diğer: rehber kontrolleri ve gerekirse rehber içeriği (`docs/rehber/`, lead uygular)
