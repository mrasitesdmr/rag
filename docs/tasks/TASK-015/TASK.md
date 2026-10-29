# TASK-015: Yeni sürüm yükleme (rehber 2026.11)

## Ne isteniyor
Kaynak detay ekranındaki "Yeni sürüm yükle" düğmesi çalışır. Kullanıcı rehberin yeni sürümünü (TASK-004'teki 2026.11 PDF'i) yükler. retrieval-api yeni bir içe alma işi başlatır; ekran aşamaları ve ilerlemeyi canlı gösterir. İş bitince kaynağın parçaları tek seferde yeni sürümle değişir. İş sürerken asistan eski sürümden cevap vermeye devam eder. Lab'daki `POST /api/workspaces/42/sources/10001/versions` → 202 + `Location: /ingestion-jobs/{id}` akışının gerçek hâlidir.

## Kurallar
1. **Yükleme penceresi** ("Yeni sürüm yükle · <kaynak adı>"):
   - Lab'da dosya önceden seçili gelir ("belge yönetim sisteminden geliyor"). Gerçek üründe kullanıcı dosyayı kendisi seçer.
   - Sürüm etiketi girilir.
   - Değişiklik özeti girilir ya da gösterilir; TASK-004'teki değişiklik özetinin pencerede nasıl yer alacağı onay kartında belirlenir.
   - Seçilen dosyanın adı, boyutu ve sayfa sayısı ile önceki sürümün dosya adı gösterilir.
   - Düğmeler: "Vazgeç" ve "Yükle ve işle".
2. **Doğrulama:** Sadece PDF kabul edilir; dosya boş olamaz; en fazla 20 MB. Sürüm etiketi zorunludur ve aynı kaynakta tekrarlanamaz. Her ihlal sabit bir `errorCode` döner; ekran Türkçe mesaj gösterir.
3. **Eşzamanlı yükleme:** Kaynakta işlenen bir iş varken ikinci yükleme 409 ile reddedilir. Ekranda düğme iş bitene kadar kapalıdır.
4. **Embedding modeli:** Çalışma alanının modelinden farklı modelle yükleme 409 ile reddedilir (lab'daki bilgi kutusu).
5. **Asenkron iş:** Yükleme isteği 202 ve iş adresiyle döner. İş TASK-005'teki aşamalardan geçer.
6. **İlerleme kartı** ("İçe alma işi #N · AŞAMA"):
   - aşama şeridi (QUEUED … DONE), ilerleme çubuğu,
   - EMBEDDING'de "x/y parça embed edildi", WRITING'de "eski parçalar silinip yenileri yazılıyor (tek transaction)",
   - "bu sırada asistan <eski sürüm>'dan cevap veriyor" notu.
   Ekran durumu polling ile izler. Sayfa yenilense ya da başka ekrana gidilip dönülse bile süren iş bulunur ve izlenmeye devam eder.
7. **Atomik değişim:** WRITING aşamasında eski sürümün parçaları ve yenileri tek transaction'da değişir. Arama hiçbir an yarım sürüm görmez: ya tamamen eski ya tamamen yeni.
8. **Hata:** İş `FAILED` olursa eski sürüm aynen kalır; işler tablosunda hata durumu ve Türkçe sebep görünür.
9. **Düğme davranışı:** Lab'da 2026.11 yüklendikten sonra düğme "Güncel sürüm yüklü" olup kapanıyordu, çünkü demoda sadece iki sürüm vardı. Gerçek üründe iş sürmüyorsa düğme her zaman açıktır; sonraki sürümler de yüklenebilir.
10. **Sürümün izi:** Önceki turlar kendi rehber sürümünü (2026.10) korur. Yeni turlar 2026.11 ile kaydedilir. Yapılandırma şeridindeki "Rehber" değeri güncellenir.
11. Eski sürümün PDF'i ve iş geçmişi saklanır. Eski sürüm parçalarının saklanıp saklanmayacağı (sadece güncel sürüm aranır) onay kartında belirlenir; değerlendirme geçmişi ve konuşma detayı bundan etkilenir.

## Örnek senaryolar
- Kullanıcı 2026.11 PDF'ini seçer, "2026.11" etiketini girer, "Yükle ve işle"ye basar. Kart aşamaları gösterir. EMBEDDING sırasında Önizleme'de sorulan "Raşit Gold yıllık üyelik ücreti ne kadar?" sorusu 1.450 TL cevabını alır. İş `DONE` olduktan sonra aynı soru 1.650 TL cevabını alır.
- İş sürerken ikinci bir yükleme denenir: 409, Türkçe mesaj.
- Yükleme sırasında sayfa yenilenir; ilerleme kartı kaldığı aşamadan devam eder.
- Kullanıcı `.docx` seçer: Türkçe "Sadece PDF yüklenebilir" mesajı görünür.

## Kabul kriterleri
- Gerçek 2026.11 PDF'iyle uçtan uca denenmiş: iş süresi, parça sayısı ve önce/sonra cevap karşılaştırması raporda.
- Atomik değişim, 409 kuralları, doğrulama ve FAILED'da eski sürümün korunması testlerle doğrulanmış.
- Sayfa yenilenince devam etme E2E testiyle doğrulanmış; ilgili build komutları geçer.
- Görünüm lab'ın sol yarısıyla aynı.

## Kapsam dışı
- Sürümler arası parça farkı (diff) ekranı.
- Eski sürüme geri dönme.

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme) (rehber sürümü turda)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [x] Dil modeli / prompt / reranker (lokal model gerekiyor mu?) (bge-m3)
- [x] Veritabanı (migration; hangi servis?) (retrieval)
- [ ] WebSocket (ilerleme polling ile; STOMP gerekirse plan kararı)
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
