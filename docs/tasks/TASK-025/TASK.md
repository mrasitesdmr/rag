# TASK-025: Test fontunun lisans notu

## Ne isteniyor
TASK-005'te tester, Türkçe karakterli (ş, ı, İ) test PDF'leri üretmek için `retrieval-api/src/test/resources/fonts/LiberationSans-Regular.ttf` dosyasını ekledi (SIL Open Font License, 410 KB). Kullanıcı dosyanın kalmasını onayladı. Lisans yeniden dağıtıma izin veriyor ama lisans metninin fontla birlikte dağıtılmasını istiyor; repoda lisans metni ya da atıf notu yok. Kaynak: `docs/tasks/TASK-005/handoff/05-qa-report.md` (Kod review bulguları, Optional).

## Kurallar
1. Fontun yanına OFL lisans metni (`OFL.txt` ya da eşdeğeri) ve fontun adı, sürümü ve kaynağını belirten kısa bir atıf notu eklenir.
2. Font dosyası değişmez; testler aynı fontu kullanmaya devam eder.
3. Lisans metni fontun gerçek dağıtımındaki metinle aynıdır; elle yazılmaz ya da kısaltılmaz. Nereden alındığı raporda yazılır.
4. Repoya başka üçüncü taraf dosya girdiyse (test kaynakları, fixture'lar) ve lisans notu eksikse raporda listelenir.

## Örnek senaryolar
- `retrieval-api/src/test/resources/fonts/` klasörü açılınca font, lisans metni ve atıf notu birlikte görülür.

## Kabul kriterleri
- Lisans metni ve atıf notu eklenmiş; kaynağı raporda.
- `cd retrieval-api && ./mvnw verify` geçer (testler etkilenmez).

## Kapsam dışı
- Fontun değiştirilmesi ya da test PDF'lerinin başka yolla üretilmesi.
- Uygulama kodu.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama) (sadece test kaynakları)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
