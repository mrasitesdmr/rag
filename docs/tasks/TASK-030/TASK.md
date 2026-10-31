# TASK-030: Belge adında Türkçe büyük/küçük harf eşleşmesi

## Ne isteniyor
TASK-027'de belge adı çalışma alanında büyük/küçük harf farkı gözetmeden tekil olmalı. Ama karşılaştırma veritabanının `lower()` fonksiyonuyla yapılıyor ve Türkçe `İ/i` ile `I/ı` eşleşmesini garanti etmiyor: "RAG ANLATIMI" ile "RAG Anlatımı", "İstanbul" ile "istanbul" farklı ad sayılabiliyor. Banka Türkçe çalıştığı için aynı belge iki kez, iki ayrı kaynak olarak yüklenebilir. Kaynak: `docs/tasks/TASK-027/handoff/00-onay-karti.md` (Ad kararı), `01-plan.md` (Kararlar #7), `05-qa-report.md` (FYI-2) ve `02-backend-implementation.md` (M0 notu).

## Kurallar
1. Ad karşılaştırması Türkçe kurallarla yapılır: `I` ↔ `ı`, `İ` ↔ `i`. "RAG ANLATIMI", "RAG Anlatımı" ve "rag anlatımı" aynı addır; "İstanbul" ile "istanbul" aynı addır.
2. Kural hem yüklemedeki ön kontrolde hem veritabanındaki tekillik index'inde aynıdır; aynı anda gelen iki yükleme de aynı sonucu verir (biri 409 `SOURCE_NAME_TAKEN`).
3. Kaydedilen ad kullanıcının yazdığı gibi kalır; sadece karşılaştırma değişir.
4. Yöntem (ör. PostgreSQL ICU collation, ayrı normalize edilmiş ad kolonu) ve mevcut kayıtların taşınması onay kartında seçenekleriyle yazılır. Mevcut veride bu kurala göre çakışan iki ad varsa migration'ın ne yapacağı da kartta yazılır.
5. Aynı kural TASK-015'teki sürüm etiketi tekilliği için de gerekiyorsa kartta belirtilir.

## Örnek senaryolar
- "RAG Anlatımı" yüklüyken "RAG ANLATIMI" adıyla yükleme → 409 `SOURCE_NAME_TAKEN`.
- "İstanbul Şube Rehberi" yüklüyken "istanbul şube rehberi" → 409.
- "Kart" yüklüyken "Kartlar" → kabul edilir (farklı ad).

## Kabul kriterleri
- Türkçe harf çiftleri için testler (ön kontrol ve index) yazılmış; `cd retrieval-api && ./mvnw verify` geçer.
- Migration temiz veritabanında ve mevcut veriyle denenmiş.

## Kapsam dışı
- Ad değiştirme ya da kaynak silme.
- Arama ve parça metninde Türkçe büyük/küçük harf davranışı.

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [x] Veritabanı (migration; hangi servis?) (retrieval)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
