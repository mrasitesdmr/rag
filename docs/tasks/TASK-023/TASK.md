# TASK-023: Rehberden gerçek kart şeması adının kaldırılması

## Ne isteniyor
Ürün ve Hizmet Rehberi'nde gerçek bir kart şeması markası olan "Mastercard" 4 yerde geçiyor: 2.1, 2.2, 2.14 ve 2.17. Banka kurgusal olduğu için bu ad kaldırılır; metin genel bir ifadeyle ("uluslararası kart altyapısı" gibi) ya da kurgusal bir adla yazılır. Kaynak: TASK-004 QA raporu Optional 4.

## Kurallar
1. Gerçek kart şeması markaları (Mastercard, Visa, American Express, Troy) iki PDF'te de geçmez. Bu, `dogrula.sh`'de kontrol edilir (D12'deki gerçek ad listesine eklenir).
2. Yerine gelecek ifade onay kartında önerilir ve kullanıcı seçer.
3. Olgular değişmez: kartların yurt dışında kullanılabilmesi, ücretler ve itiraz süreleri aynı kalır.
4. 2. bölüm iki sürümde aynı sayfa sayısını korur; bölüm başlangıç sayfaları ve değişmeyen bölümlerin sayfaları (D9) değişmez. Değişiklik özetindeki sayfa numaraları kayarsa güncellenir.
5. PDF'ler `uret.sh` ile yeniden üretilir, `dogrula.sh` bütün kontrollerden geçer, değişen metin kullanıcıya gösterilir.
6. Bu task, rehber bilgi bankasına içe alınmadan (TASK-005) önce yapılmalıdır. TASK-005'ten sonra yapılırsa rehberin yeniden içe alınması gerekir.

## Örnek senaryolar
- "Yurt dışında banka kartımı kullanabilir miyim?" sorusunun dayandığı 2.14 metni, marka adı olmadan aynı olguyu anlatır: yurt dışında kullanılabilir, önce açılması gerekir, %1,99 ücret alınır.

## Kabul kriterleri
- `./docs/rehber/dogrula.sh` bütün kontrollerden geçiyor; yeni gerçek marka kontrolü de dahil.
- İki PDF'in metninde "Mastercard" geçmiyor.
- Kullanıcı değişen metni onaylamış.

## Kapsam dışı
- Diğer içerik değişiklikleri (TASK-020).
- Lab dosyaları (`kaynaklar/**`).

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
- [x] Diğer: rehber içeriği ve PDF'ler (`docs/rehber/`, lead uygular)
