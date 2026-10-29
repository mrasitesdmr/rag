# TASK-013: Geri bildirim ve temsilciye aktarma

## Ne isteniyor
Önizleme'de her cevabın altında "Faydalı mı? Evet / Hayır" geri bildirimi verilir ve tura kaydedilir. Asistan cevap bulamadığında ("Bu konuda rehberimizde bilgi bulamadım.") cevabın altında "Temsilciye bağlan" düğmesi çıkar. Düğmeye basılınca konuşma temsilci kuyruğuna alınır ve düğmenin yerinde "Temsilci kuyruğundasın" yazar. Lab'daki `POST /api/assistant/turns/{turnId}/feedback` ve `POST /api/assistant/conversations/{id}/handoff` uçlarının gerçek hâlidir.

## Kurallar
1. **Geri bildirim:**
   - "Evet" ya da "Hayır" seçilir; seçili düğme vurgulanır.
   - Kullanıcı fikrini değiştirebilir: son seçim geçerlidir, aynı düğmeye yeniden basmak seçimi değiştirmez.
   - Geri bildirim sadece tamamlanmış (akışı bitmiş) bir tura verilebilir; aksi hâlde sabit bir `errorCode` döner.
   - Aynı tura eşzamanlı iki geri bildirim tutarlı sonuçlanır (son yazan kazanır, kayıp güncelleme olmaz).
   - Cevabın saati lab'daki gibi geri bildirim satırında gösterilir.
2. **Temsilciye aktarma:**
   - Düğme sadece "cevap yok" olarak işaretlenen turlarda görünür. Aktarma sebebi `NO_ANSWER`'dır.
   - Aktarma konuşma düzeyindedir: bir konuşma bir kez aktarılır. Tekrar istek başarılı döner ama yeni aktarma oluşturmaz (idempotent).
   - Uç 202 döner; cevapta kuyruk adı (lab'da `bireysel`) ve sebep bulunur.
   - Aktarılan konuşmada yeni soru sorulabilir mi, onay kartında belirlenir.
3. **Kuyruk:** Bu projede gerçek bir çağrı merkezi sistemi yoktur. Aktarmanın nereye kaydedileceği (assistant veritabanında aktarma kaydı ve kuyruk adı) ve kuyruğun nasıl görüleceği (Konuşmalar ekranında işaret) onay kartında yazılır. Sahte bir temsilci cevabı üretilmez.
4. Aktarma ve geri bildirim Konuşmalar ekranında görünür (TASK-014).
5. Hata mesajları `errorCode` → Türkçe eşlemeyle gösterilir.

## Örnek senaryolar
- Kullanıcı "Kartımı kaybettim" cevabına "Evet" der; sonra fikrini değiştirip "Hayır" der. Turda "Hayır" kayıtlıdır.
- Kullanıcı "Bitcoin alıp satabilir miyim?" sorar, "bulamadım" cevabını alır, "Temsilciye bağlan"a basar. Düğmenin yerinde "Temsilci kuyruğundasın" görünür; konuşma `bireysel` kuyruğunda kayıtlıdır.
- Aynı konuşma için aktarma iki kez gönderilir; tek aktarma kaydı vardır.

## Kabul kriterleri
- Uçlar `openapi.yaml`'da; backend ve web buna uyuyor.
- İdempotentlik, eşzamanlılık, akışı bitmemiş tur ve cevap yok koşulu testlerle doğrulanmış; ilgili build komutları geçer.
- Görünüm lab'ın sol yarısıyla aynı.

## Kapsam dışı
- Temsilci ekranı ya da gerçek çağrı merkezi entegrasyonu.
- Geri bildirim için serbest metin yorum (lab'da yok).

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [x] Veritabanı (migration; hangi servis?) (assistant)
- [ ] WebSocket
- [ ] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
