# TASK-026: Rehberin ilk girişinde biçim ve dosya yolu güvenceleri

> **İPTAL (2026-10-31):** Bu task'ın iki riski TASK-027 ile ortadan kalkıyor. Rehber artık açılışta repodaki klasörden otomatik yüklenmiyor (Docker'daki klasör bağlantısı kalkıyor) ve PDF okuyucu rehberin yer imi ile üst/alt bilgi kalıbına bağlı olmayan genel okuyucuya dönüşüyor. Yapılmayacak; aşağıdaki metin geçmiş kayıt olarak duruyor.

## Ne isteniyor
TASK-005'in onay kartındaki iki risk kapatılır. (1) İçe alma, başlıkları PDF'in yer imlerinden ve sayfa üst/alt bilgisini rehberin kendi kalıbından tanıyor; rehber üretimi yer imlerini kaybederse iş `PDF_NO_SECTIONS` ile durur, başka bir kalıpta üst/alt bilgi sessizce parçalara karışabilir. (2) Docker'da rehber PDF'i repodaki `docs/rehber/pdf` klasöründen salt okunur bağlanıyor; Compose repo dışından ya da klasör taşınarak çalıştırılırsa dosya bulunamaz, rehber girmez ve bu sadece loglarda görünür. Bugün ikisi de sorun çıkarmıyor; amaç bozulduklarında sessiz kalmamaları. Kaynak: `docs/tasks/TASK-005/handoff/00-onay-karti.md` (Riskler) ve `01-plan.md` (Açık sorular ve riskler).

## Kurallar
1. Rehber üretilirken (TASK-004 `uret.sh` / `dogrula.sh`) içe almanın dayandığı biçim kontrol edilir: numaralı başlıkların yer imi olarak bulunması ve sayfa üst/alt bilgisinin içe almanın beklediği kalıba uyması. Uymayan PDF için kontrol KALDI der.
2. İçe alma sırasında sayfaların üst/alt bilgisi kalıba uymuyorsa bu sessiz geçmez: ya iş sabit bir `errorCode` ile `FAILED` olur ya da sayısı loglanır. Hangisi olacağı onay kartında sorulur.
3. Docker'da rehber dosyası bulunamazsa durum, logların dışında da görülebilir (ör. sağlık ucunda ya da bir iş kaydında). Nasıl görüneceği ve servisin açık kalıp kalmayacağı onay kartında sorulur.
4. Compose'un hangi dizinden çalıştırılması gerektiği ve rehber yolunun nasıl değiştirileceği `deployment/CLAUDE.md` ve `README.md`'de açıkça yazar.
5. Mevcut davranış bozulmaz: repodaki rehberle ilk açılış, yeniden başlatma ve TASK-005'in kabul senaryoları aynen geçer.

## Örnek senaryolar
- Yer imleri olmadan üretilmiş bir rehber `dogrula.sh`'den KALDI ile döner.
- Üst bilgisi farklı kalıpta olan bir PDF içe alınmaya çalışılır; sonuç kayıtlarda ya da işin durumunda görünür, parçalara sessizce karışmaz.
- Compose, rehber klasörü bağlanamayacak şekilde çalıştırılır; rehberin girmediği loglara bakmadan görülebilir.

## Kabul kriterleri
- Her yeni koruma için bir negatif deneme yapılmış ve sonucu raporda.
- Repodaki iki PDF'le `dogrula.sh` bütün kontrollerden geçer.
- `cd retrieval-api && ./mvnw verify` geçer; Docker'da temiz ilk açılış ve yeniden başlatma denenmiş.

## Kapsam dışı
- Rehber dışındaki biçimde PDF'leri okuyabilen genel bir okuyucu.
- Rehberin içeriğinin değişmesi.
- Yeni sürüm yükleme (TASK-015).

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [x] API sözleşmesi (REST) (sağlık ucu ya da yeni errorCode gerekirse)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
