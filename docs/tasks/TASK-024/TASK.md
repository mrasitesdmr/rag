# TASK-024: Parça örtüşmesi ve sayfa etiketinin düzeltilmesi

## Ne isteniyor
TASK-005'in onay kartındaki "sayfa numarası yaklaşıktır" riski ve QA raporundaki Optional-1 kapatılır. Bugün bir parça önceki parçanın son satırlarıyla başlıyor (örtüşme). Bu satırlar bir "Sık sorulanlar" cevabını alıp sorusunu dışarıda bırakabiliyor. Parçanın sayfası da ilk yeni satırın sayfası olduğu için parçanın görünen ilk metni başka sayfada kalabiliyor. Örnek: 2026.10'da chunk 122 (`7 Yatırım · Sık sorulanlar`) "s. 56" gösteriyor, ama ilk metni ("Raşit Mobil'den limit emri verebilirsiniz…") s. 55'te ve bu cevabın sorusu ("Dolar belli bir kura gelince…") parçada yok. Alıntılar (TASK-006/007 ve sonrası) "s. N" gösterdiğinde müşteri bilgiyi o sayfada bulabilmelidir. Kaynak: `docs/tasks/TASK-005/handoff/05-qa-report.md` (Kod review bulguları, Optional) ve `00-onay-karti.md` (Riskler).

## Kurallar
1. Örtüşme bir soru-cevap çiftini bölmez: örtüşmeye bir SSS cevabının satırı giriyorsa o cevabın sorusu da girer. Bunun parça boyutu sınırlarıyla (hedef 1.600, en fazla 2.000, örtüşme 200 karakter) nasıl uzlaşacağı onay kartında yazılır.
2. Parçanın gösterilen sayfası, müşterinin parçanın metnini bulacağı sayfayla tutarlıdır. Seçenekler (ör. sayfa = parçanın ilk satırının sayfası; ya da sayfa aralığı "s. 55-56") ve sözleşmeye etkisi onay kartında kullanıcıya sorulur.
3. TASK-005'teki diğer parçalama kuralları değişmez: bölüm sınırı, tablo satırının bölünmemesi, üst/alt bilginin atılması, kapak ve içindekilerin atlanması.
4. Rehber değişen kurallarla yeniden içe alınır. Mevcut bir bilgi bankasının bu değişiklikten sonra nasıl yenileneceği (yeni sürüm yükleme TASK-015'te) onay kartında yazılır; tabloya elle parça yazılmaz.

## Örnek senaryolar
- 2026.10 yeniden içe alınır. "Döviz alırken komisyon…" sorusunu içeren parça, örtüşmeyle gelen cevabın sorusunu da taşır ya da o cevabı hiç taşımaz.
- Rastgele seçilen bir parçanın gösterilen sayfası PDF'te açılır; parçanın ilk metni o sayfada (ya da gösterilen aralıkta) bulunur.

## Kabul kriterleri
- Gerçek 2026.10 PDF'i ve gerçek `bge-m3` ile yeniden içe alınmış; parça sayısı, ortalama token ve TASK-005'e göre fark raporda.
- Örtüşmeyle başlayan en az 5 parça (chunk 122'nin karşılığı dahil) PDF ile elle karşılaştırılmış.
- Örtüşme ve sayfa kuralları testlerle doğrulanmış; `cd retrieval-api && ./mvnw verify` geçer.

## Kapsam dışı
- Vektör arama (TASK-006) ve alıntıların ekranda gösterimi.
- Yeni sürüm yükleme (TASK-015).

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [x] API sözleşmesi (REST) (sayfa aralığı seçilirse)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (bge-m3, yeniden içe alma)
- [x] Veritabanı (migration; hangi servis?) (retrieval, sayfa aralığı seçilirse)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
