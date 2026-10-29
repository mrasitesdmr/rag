# TASK-019: Hata yanıtlarının sözleşme alanlarını testle güvenceye alma

## Ne isteniyor
TASK-002'nin qa raporundaki iki Optional bulgu kapatılır. İki servisin hata yanıtlarında sözleşmenin istediği alanlar (`title`, `instance`, `violations`) testlerle doğrulanır; hata yakalayıcıda hiç çalışmayan kod dalı ya test edilir ya kaldırılır. Ekranda ve davranışta değişiklik yoktur. Kaynak: `docs/tasks/TASK-002/handoff/05-qa-report.md` (Bulgular tablosu).

## Kurallar
1. assistant-api ve retrieval-api için aynı kurallar geçerlidir.
2. Beklenmeyen hata (500, `INTERNAL_ERROR`) yanıtında `title` ve `instance` alanlarının değeri testte doğrulanır. `title` sözleşmede zorunludur.
3. Doğrulama hatası (400, `VALIDATION_FAILED`) yanıtında `violations` listesi testte doğrulanır: hem istek gövdesi alanı hem `@RequestParam` kısıtı için `field` ve `message`.
4. `GlobalExceptionHandler.hasErrorCode` içindeki hiç çalışmayan `properties != null` dalı ya bir testle çalıştırılır ya da gereksizse kaldırılır. Karar planda gerekçesiyle yazılır.
5. Hata yanıtlarının biçimi ve `errorCode` değerleri değişmez; `docs/api/openapi.yaml` değişmez.
6. Yeni dependency eklenmez.

## Örnek senaryolar
- Bir uç beklenmeyen bir hata fırlatır. Yanıt 500, `errorCode: INTERNAL_ERROR`, `title: "Internal Server Error"` ve `instance` istek adresidir. Bu alanlardan biri kaldırılırsa test kırılır.
- Geçersiz bir sorgu parametresiyle istek atılır. Yanıt 400, `errorCode: VALIDATION_FAILED` ve `violations` içinde parametrenin adı ile mesajı vardır. Liste boş dönerse test kırılır.

## Kabul kriterleri
- `cd assistant-api && ./mvnw verify` ve `cd retrieval-api && ./mvnw verify` geçer.
- PIT iki serviste çalıştırılır. TASK-002'de hayatta kalan `setTitle` / `setInstance` mutantları öldürülür. Hayatta kalan mutant varsa gerekçesi raporda yazılır (ör. eşdeğer mutant).
- `hasErrorCode` dalı için verilen karar uygulanır; NO_COVERAGE kalmaz.

## Kapsam dışı
- Yeni hata kodu, domain exception'ı (`BaseException` ve kategori sınıfları).
- Web tarafı.
- Sözleşme değişikliği.

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme) (sadece hata yakalayıcı ve testleri)
- [x] retrieval-api (kaynak, içe alma, embedding, arama) (sadece hata yakalayıcı ve testleri)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
