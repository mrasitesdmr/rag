# TASK-028: Belge yüklemede PDF'in tek okunması ve bellek sınırı

## Ne isteniyor
TASK-027'nin QA raporundaki iki Optional bulgu ve onay kartındaki "yükleme süresi" riski kapatılır. Bugün yüklenen PDF iki kez okunuyor: istek sırasında ret kontrolü için (yer imi, metin, açılma) ve iş içinde PARSING aşamasında yeniden. Büyük bir belgede "belge ekle" cevabı (202) birkaç saniye gecikiyor. Aynı anda gelen yüklemeler de PDF'i ayrı ayrı belleğe alıyor (en çok 20 MB × istek sayısı). Kullanıcının gördüğü davranış değişmez; amaç cevabın hızlı dönmesi ve belleğin sınırlı kalması. Kaynak: `docs/tasks/TASK-027/handoff/05-qa-report.md` (Bulgular: Optional `SourceUploadValidator.validate`, Optional `IngestionService.createSource`) ve `00-onay-karti.md` (Riskler).

## Kurallar
1. Dosyadan kaynaklanan hatalar (`PDF_UNREADABLE`, `PDF_NO_TEXT`, `PDF_NO_BOOKMARKS`) yine yükleme anında, kaynak ve iş oluşmadan döner (TASK-027 kullanıcı kararı). Bu kural gevşetilmez.
2. PDF bir yüklemede gereğinden fazla okunmaz. Okumanın nasıl tekrar kullanılacağı ya da ret kontrolünün nasıl hafifletileceği (ör. sadece yer imi ve metin varlığı) onay kartında seçenekleriyle yazılır.
3. Aynı anda gelen yüklemelerin bellekte tuttuğu PDF baytları sınırlıdır. Sınır ve sınır aşılınca ne olacağı (bekletme ya da sabit `errorCode` ile ret) onay kartında yazılır. TASK-015 Kural 12 (kuyruktaki iş baytları bellekte tutmaz) ile tutarlıdır.
4. Yeniden dene (TASK-027) ve iş aşamaları değişmez.
5. Önce ölçülür, sonra değiştirilir: rehber (70 sayfa) ve 20 MB'a yakın bir PDF için yükleme cevabının süresi değişiklikten önce ve sonra aynı makinede ölçülür.

## Örnek senaryolar
- Rehber yüklenir; 202 cevabı değişiklikten öncekinden belirgin biçimde hızlı döner; iş yine `DONE` olur, 154 parça.
- Yer imsiz PDF yüklenir; yine anında 422 `PDF_NO_BOOKMARKS`, kayıt oluşmaz.
- Aynı anda birkaç büyük yükleme yapılır; servisin belleği sınırın üstüne çıkmaz.

## Kabul kriterleri
- Önce/sonra süre ölçümü ve eşzamanlı yükleme bellek ölçümü raporda.
- TASK-027'nin kabul senaryoları aynen geçer; `cd retrieval-api && ./mvnw verify` geçer.

## Kapsam dışı
- Bilgi kaynakları ekranı (TASK-007).
- Ret kurallarının değişmesi.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST) (bellek sınırında yeni `errorCode` seçilirse)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
