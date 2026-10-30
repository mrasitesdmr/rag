# TASK-022: Rehber sonrası doküman güncellemesi

## Ne isteniyor
Proje dokümanlarında hâlâ lab'daki 31 sayfalık statik rehber anlatılıyor. Bu task'ta bunlar, TASK-004'te yazılan gerçek rehbere göre güncellenir. Kök `CLAUDE.md`'deki "Durum" satırı da TASK-003'ten beri eski. Kaynak: TASK-004 plan "Açık sorular ve riskler" 3.

## Kurallar
1. `rag.md` içindeki lab rehberine ait sayılar ("31 sayfa, 61 parça" gibi) kaldırılır ya da gerçek rehbere göre yazılır. Gerçek rehber: 70 sayfa, 11 bölüm, iki sürüm (2026.10 ve 2026.11), `docs/rehber/`. Parça sayısı içe alma (TASK-005) yapılmadan bilinmez, bu yüzden yazılmaz.
2. `.claude/skills/rag-pipeline/references/indexing.md` içindeki "31 sayfa" bilgisi aynı şekilde güncellenir.
3. Kök `CLAUDE.md`'nin "Durum" satırı güncel duruma getirilir: iskelet (TASK-002), lokal modeller ve `ollama.md` (TASK-003), rehber PDF'leri (TASK-004). Klasör yapısı tablosuna `docs/rehber/` eklenir.
4. Ürün kuralları değişmez. Yalnızca eskimiş bilgiler düzeltilir.
5. Bu dosyalar hiçbir rolün yazma alanında değil. Değişiklikler kullanıcı onayıyla lead tarafından yapılır.
6. Bunların dışında eski lab bilgisi taşıyan başka bir dosya bulunursa, onu değiştirmeden önce kullanıcıya gösterilir.

## Örnek senaryolar
- Yeni bir ekip üyesi kök `CLAUDE.md`'yi okuyor ve lokal modellerin kurulu olduğunu, rehberin `docs/rehber/` altında durduğunu görüyor.
- TASK-005'i planlayan planner, `rag.md`'de eski "31 sayfa" bilgisini görmüyor.

## Kabul kriterleri
- `rag.md`, `indexing.md` ve kök `CLAUDE.md`'de lab rehberine ait eski sayı kalmamış (`grep "31 sayfa"` boş).
- Değişikliklerin diff'i kullanıcıya gösterilmiş ve onaylanmış.

## Kapsam dışı
- Rehber içeriği.
- `kaynaklar/**` (değişmez).

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
- [x] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
