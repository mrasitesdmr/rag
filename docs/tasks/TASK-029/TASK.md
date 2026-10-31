# TASK-029: retrieval-api'de hayatta kalan mutant'ların kapatılması

## Ne isteniyor
TASK-027'nin QA raporundaki Optional bulgu kapatılır. PIT'te `SourceVersion` (12 mutant'ın 2'si öldü) ve `Workspace` (6'nın 1'i) sınıflarında getter mutant'ları hayatta kalıyor; bu değerlerin yanlış dönmesini hiçbir test yakalamıyor. TASK-027'nin yeni sınıflarında (`PdfDocumentParser`, `SourceUploadValidator`, `GlobalExceptionHandler`, `PageMarginFilter`) kalan mutant'lar da tek tek incelenir. Uygulama davranışı değişmez. Kaynak: `docs/tasks/TASK-027/handoff/05-qa-report.md` (Mutation testi ve Bulgular: Optional `SourceVersion`, `Workspace`) ve `03-test-report.md`.

## Kurallar
1. `SourceVersion` ve `Workspace` alanlarının kullanıldığı davranış (ör. kaynak listesi ve iş yanıtındaki sürüm, sayfa, bölüm sayısı; çalışma alanının modeli ve boyutu) testle doğrulanır. Getter'ı doğrudan çağıran anlamsız test yazılmaz; beklenen değerler test edilen koddan bağımsız türetilir.
2. TASK-027 sınıflarında kalan her mutant için: test eklenir ya da neden eşdeğer olduğu raporda bir satırla yazılır.
3. Uygulama koduna dokunulmaz. Test edilemeyen ya da ölü kod bulunursa raporda `NEEDS_FIX` ile backend-coder'a yazılır.

## Örnek senaryolar
- `SourceVersion.getPageCount()` her zaman 0 dönecek şekilde bozulursa kaynak listesi testi kırılır.

## Kabul kriterleri
- PIT önce/sonra sayıları raporda; `SourceVersion` ve `Workspace` için hayatta kalan mutant ya kalmamış ya da her biri gerekçeli.
- `cd retrieval-api && ./mvnw verify` geçer.

## Kapsam dışı
- assistant-api ve web mutation testleri.
- Uygulama davranışının değişmesi.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama) (sadece testler)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
