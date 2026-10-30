# TASK-021: Rehber kontrol betiklerinin sağlamlaştırılması

## Ne isteniyor
TASK-004'ün QA raporundaki iki Optional ve bir Nit bulgu ile TASK-023'ün QA raporundaki bir Optional bulgu kapatılır. Rehber kontrol betikleri (`docs/rehber/uret.sh`, `dogrula.sh`, `dogrulama/`) hatalı girdide sessizce "GEÇTİ" demez ve verilen klasör yolunu beklendiği gibi çözer. Rehberin içeriği ve PDF'ler değişmez. Kaynak: `docs/tasks/TASK-004/handoff/05-qa-report.md` (Bulgular tablosu) ve `docs/tasks/TASK-023/handoff/05-qa-report.md` (Kod review bulguları, Optional).

## Kurallar
1. `kontroller.txt`'de bir kontrolün beklediği bölüm (`[CUMLE]`, `[SATIR]`, `[KAPAK]`, `[GERCEK_BANKA]`, `[DEGISMEYEN_BOLUMLER]`, `[SAYFA_ARALIGI]`) yoksa ya da boşsa o kontrol KALDI der ve nedenini yazar. Tanınmayan bölüm adı da (ör. `[SATIRR]`) hata sayılır.
2. `uret.sh <klasör>` ve `dogrula.sh <klasör>` komutlarında göreli yol, komutun çalıştırıldığı dizine göre çözülür. Mutlak yol olduğu gibi kullanılır.
3. D4 tablo satırı eşleşmesi hücre sınırlarına bakar. Örneğin `1.450 TL` aranırken `11.450 TL` eşleşmez.
4. Mevcut 11 kontrolün sonucu değişmez: repodaki PDF'lerle hepsi GEÇTİ der.
5. Her yeni koruma için bir negatif deneme yapılır ve KALDI dediği raporda gösterilir.
6. D12, gerçek kart şeması adlarını (`[GERCEK_BANKA]` listesindeki Mastercard, Visa, Amex, Troy vb.) kesme işaretsiz Türkçe ek almış hâlde de yakalar (ör. `Mastercardla`, `Visayla`). Bu genişletme yanlış alarm üretmez: repodaki PDF'lerle D12 GEÇTİ der; kart şeması adlarını içeren sıradan Türkçe sözcükler (varsa) planda değerlendirilir.
7. Yeni dependency eklenmez.

## Örnek senaryolar
- `kontroller.txt`'de `[SATIR]` yanlışlıkla `[SATIRR]` yazılır. Bugün D4 "0 satır kontrolü" ile GEÇTİ diyor; bu task'tan sonra KALDI der.
- Repo kökünden `./docs/rehber/dogrula.sh tmp/pdf` çalıştırılır. Komut repo kökündeki `tmp/pdf` klasörünü kontrol eder.
- Rehbere yanlışlıkla "Mastercardla ödeme yapabilirsiniz" yazılır. Bugün D12 bunu kaçırıyor (tam kelime araması); bu task'tan sonra KALDI der ve adı gösterir.

## Kabul kriterleri
- `./docs/rehber/dogrula.sh` repodaki PDF'lerle 11/11 GEÇTİ.
- Kural 1, 2, 3 ve 6 için negatif denemelerin çıktısı raporda.
- `docs/rehber/README.md` yeni davranışı anlatıyor.

## Kapsam dışı
- Olgu tutarlılığı kontrolü (TASK-020).
- Rehber içeriği ve PDF'ler.

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
- [x] Diğer: rehber kontrol betikleri (`docs/rehber/`, lead uygular)
