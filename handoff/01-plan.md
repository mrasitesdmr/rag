TASK: TASK-032
# Plan

## Özet
**Mevcut durum (TASK-031 sonrası, sadece `RAG`):** `AnswerService.answerWithRetrieval` sırası: (1) `SmallTalkDetector.detect`
kalıp listesi (`small-talk/*.txt`) tutarsa hazır metin, arama yok; (2) konuşmada cevaplı önceki tur varsa ve mesaj kısaysa
(`isShortMessage`: ≤ 10 kelime, `?` yok) arama öncesi etiket; (3) yeniden yazma (`query-rewrite.st`, İngilizce, geçmişte
`USER:` + `ASSISTANT:` satırları); (4) retrieval + rerank; (5) eşik (`min-rerank-score=-4.0`) altıysa etiket → hazır metin
ya da bulamadım; (6) cevap modeli. Sistem talimatı kural 2 "her cümleye numara", kural 4 giriş cümleleri "kural 3'ten önce
gelir". Sonuç (TASK.md): "Ahmet" → "Merhaba Ahmet … [1]"; "Hava durumu" → yatırım cümlesi; "Nber" → başka banka cümlesi;
"Kredi limitim ne kadar" → sadece giriş cümlesi.

**Yeni durum (sadece `RAG`; Sadece LLM değişmez):**
1. Kalıp listesi ve eşleştirme kaldırılır (dosyalar ve `SmallTalkDetector.detect`).
2. **Arama öncesi etiket her kısa mesajda:** konuşmanın ilk mesajı da olsa, eşikten bağımsız. Selam / teşekkür / veda → hazır
   metin (bugünkü metinler), `SMALL_TALK`, strateji `NONE`, arama ve yeniden yazma yok. Diğer / hata / zaman aşımı → normal yol;
   eşik altında etiket yeniden sorulmaz → bulamadım.
3. **Kısa mesaj** (kart S1, önerilen B): normalize metin ≥ 1 kelime ve `?` yoksa ≤ 10 kelime, `?` varsa ≤ 5 kelime.
4. **Uzun mesajda eşik altı:** bugünkü gibi etiket (selam → hazır metin, diğer → bulamadım).
5. **Eşik ölçülerek seçilir** (kart S3): M1 ölçümü (eşik kapalı) → aday tablosu → kullanıcı değeri onaylar → properties
   varsayılanı → M2.
6. **Sistem talimatı:** giriş cümleleri sadece kendi konusunda; diğer bütün rehber dışı konular bulamadım kuralına; hesap
   bilgisinde giriş cümlesinden sonra genel bilgi alıntıyla; parçaya dayanmayan cümleye numara yazılmaz.
7. **Yeniden yazma** (kart S2, önerilen A): sadece önceki müşteri soruları verilir, asistan cevapları verilmez; şablon
   Türkçe; bağımsız yeni soru aynen kalır.
8. **İsim mesajı** (kart S4, önerilen A): etiket talimatında kendini tanıtma / sadece isim = SELAM → selam karşılığı.

**Değişen servis:** sadece assistant-api (akış sırası, prompt'lar, etiket talimatı, yeniden yazma, properties varsayılanı,
ölçüm betiği ve listesi). retrieval-api ve web değişmez. Veritabanı değişmez (migration yok, Kararlar #4). Değerlendirme ekranı
(recall@5, MRR) değişmez; eşik orada uygulanmaz.

**Yeniden adlandırma:** `SmallTalkDetector` → `ShortMessageDetector` (sadece `isShortMessage` kalır).
`SmallTalkDetector.foldTurkishLetters` → `MessageLabeler` içinde `private static`. `SmallTalkDetector.phraseKey` production'dan
kalkar (tek kullanıcısı T36 testi; tester test tarafında aynı anahtarı kurar). Log rotası `PRE_REWRITE_LABEL` →
`PRE_SEARCH_LABEL`; `PATTERN` kalkar.

**Uygulama sırası (backend-coder):** kod (eşik varsayılanı geçici olarak -4.0 kalır) → ölçüm betiği ve liste → M1 (eşik kapalı)
→ aday tablosu lead'e → kullanıcı kararı → varsayılanı yaz → M2.

Kart sorularının cevabı önerilenden farklı olursa planner bu planı ve `docs/api/` metinlerini (kısa mesaj tanımı, yeniden
yazma cümlesi) günceller.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/sse.md`, `docs/api/openapi.yaml` (sadece açıklama metinleri).
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/scripts/**`, `assistant-api/CLAUDE.md`; M1 ve M2 gerçek
  modellerle (lokal dev sistemi: assistant-api 8090, retrieval-api 8080, Ollama 11434, reranker 8081, rehber 2026.10 yüklü,
  golden set yüklü). Rapor `handoff/02-backend-implementation.md`, ölçüm dosyaları `handoff/measure/`.
- **tester:** `assistant-api/src/test/**` (ve gerekirse `web/e2e/**`). Rapor `handoff/03-test-report.md`.
- **qa:** M3 bağımsız ölçüm, kabul kriterleri, sözleşme uyumu, Önizleme ve Konuşmalar görünüm kontrolü. Rapor `handoff/05-qa-report.md`.
- **Atlanan: frontend-coder.** SSE olay adları, alanları ve `AnswerSource`/`TurnStatus` değerleri değişmez; selamlaşma turu
  ekranda bugünkü kalıp listesi turuyla aynı görünür (`NONE` + `SMALL_TALK`, SOHBET). Web'de yazılacak kod yok.
- **Atlanan: devops.** Docker, compose, nginx ve modeller değişmez; eşik `application.properties` varsayılanıyla gelir
  (compose `RETRIEVAL_MIN_RERANK_SCORE` vermez). qa, tester'a bağlanır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Kelime listesi ve eşleştirme kalkar | `small-talk/*.txt` silinir; `SmallTalkDetector` → `ShortMessageDetector`; `AnswerService` | T1, T2, T9, `./mvnw verify` |
| K2 | Kısa mesaj her zaman (ilk mesaj dahil, eşikten bağımsız) aramadan önce etiketlenir; selam/teşekkür/veda → hazır metin, arama yok, alıntı yok; diğer → normal | `AnswerService.answerWithRetrieval` adım 1 | T2-T6, T9, T10, M2 |
| K3 | Kısa mesaj tanımı; soru işaretli selamlaşmalar yakalanır (kart S1) | `ShortMessageDetector.isShortMessage` | T1, M2 (`small-talk-question`) |
| K4 | Uzun mesajda eşik altı yine etiketlenir | `AnswerService.answerBelowThreshold` (değişmez) | T7, M2 |
| K5 | Eşik ölçülerek seçilir, kullanıcı onaylar (kart S3) | M1 → kullanıcı → `application.properties` | M1 aday tablosu, M2 |
| K6 | Giriş cümleleri sadece kendi konusunda | `prompts/rag-system.st` kural 3-4 | T13, M2 (`wrongOpener` = 0) |
| K7 | Hesap bilgisi sorusunda genel bilgi alıntısıyla eklenir | `prompts/rag-system.st` kural 4 | T13, T11, M2 (`personal-info-with-info`) |
| K8 | Parçaya dayanmayan cümleye numara yok; selam/isim cevabında `[n]` yok | `rag-system.st` kural 2; etiket yolu (S4) | T13, T14, M2 |
| K9 | Yeniden yazma niyeti korur; asistan cevabı sorguya girmez; bağımsız soru aynen kalır (kart S2) | `QueryRewriter.historyText`, `prompts/query-rewrite.st` | T15-T17, M2 (`follow-up-independent`) |
| K10 | Talimatlar Türkçe, cevap Türkçe, yeni model yok | `query-rewrite.st` Türkçe; diğerleri zaten Türkçe | T16, Deployment planı |
| S1 | "Selam", "Merhaba", "Günaydın", "Nber", "Nasılsın?", "Teşekkürler", "Görüşürüz" → hazır metin, arama yok | Adım 1 | T3, T9, M2 |
| S2 | "Ahmet" → alıntısız (kart S4) | `message-label.st` | T14, M2 (`name`) |
| S3 | "Selam" → "Hava durumunu verir misin?" → bulamadım, yatırım cümlesi yok | Adım 1 + eşik + talimat | M2 (`follow-up-independent`) |
| S4 | "Kredi limitim ne kadar" → giriş cümlesi + 2.6 genel bilgi ve alıntı | Talimat kural 4 | T11, M2 |
| S5 | "Ek kart nasıl alabilirim" → "Peki ücretli mi" → doğru cevap, alıntı | Yeniden yazma | T15, M2 (`follow-up-question`) |
| S6 | "Bitcoin alabilir miyim?", "jjkbjknlk" → bulamadım, alıntı yok | Adım 1 (diğer) + eşik | T5, M2 |
| AC1 | Eşik ve kısa mesaj tanımı kullanıcı onaylı | Kart S1, S3 + M1 sonrası değer | Kart, rapor |
| AC2 | Gerçek modellerle: yanlış giriş cümlesi ve alıntılı cevap 0; selamlaşmaların hepsi hazır metin | M2, M3 | "Kabul" (aşağıda) |
| AC3 | Golden recall@5 ve bulamadım'a düşen sayısı raporda; sınır kartta (S5) | M2 + Değerlendirme çalıştırması | "Kabul" |
| AC4 | Kelime listesi ve kodu kalkmış, testler güncel, build geçer | backend-coder, tester | `./mvnw verify` (web değişmediği için web komutları gerekmez) |
| AC5 | Önizleme ve Konuşmalar görünümü lab'la aynı | Web değişmez | qa görsel kontrol |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | İlk mesaj kısa gerçek rehber sorusu ("Kredi limitim ne kadar", "Ek kart ücretli mi?") | Etiket diğer → normal yol, alıntılı cevap; ~1 sn ek süre | T4, T11, M2 (golden, `personal-info-with-info`) |
| G2 | retrieval-api kapalıyken "Selam" | Etiket selam → hazır metin; arama yapılmadığı için 503 yok | T6 |
| G3 | Dil modeli kapalı / yavaşken "Selam" | Etiket çağrısı hata ya da 10 sn zaman aşımı → diğer → normal yol; cevap modeli de kapalıysa akışta `CHAT_MODEL_UNAVAILABLE` (bugünkü hata mesajı). Önceden kalıp listesi anında cevap veriyordu | T8, kart Riskler |
| G4 | Sadece noktalama / emoji ("???", "🙂") | Kısa değil (0 kelime) → etiket yok → normal yol → eşik altıysa bulamadım | T1, M2 (`gibberish` "???") |
| G5 | Cevaplı bir sorudan sonra bağımsız konu dışı soru ("Ek kart nasıl alabilirim" → "Hava durumunu verir misin?") | Arama sorgusu soruyla aynı (ya da konuyu değiştirmeyen hali) kalır; bulamadım, giriş cümlesi yok | T16, M2 (`follow-up-independent`) |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | `SmallTalkDetector` silinir, `generation/ShortMessageDetector` (`@Component`) açılır: sadece `boolean isShortMessage(String message)` | Sınıfın adı yaptığı işi söylemeli; algılama artık etiket çağrısında | Adı korumak: "detector" artık selam algılamıyor, yanıltıcı |
| 2 | Tanım sabitleri kodda: `SHORT_MESSAGE_MAX_WORDS = 10`, `SHORT_QUESTION_MAX_WORDS = 5`; `?` ham metinde aranır, kelime sayısı `SuggestionTextNormalizer.normalize` sonucunun boşlukla bölünmesi | TASK-031'deki tanım da kodda sabitti; tanım iş kuralı, kullanıcı kartta onaylar | Properties: ortamdan değişmesi gereken bir ayar değil |
| 3 | Arama öncesi etiket, mevcut "önce etiket" yolunun koşulundan `!history.isEmpty()` çıkarılarak yapılır; `answerSmallTalkWithoutSearch` aynen kullanılır | Tek yol; SSE alanları ve tur kaydı bugünkü arama öncesi etiket turuyla aynı | Yeni yol yazmak: aynı davranışın iki kopyası |
| 4 | Migration yok. Yeni selamlaşma turu: `answer_source` `SMALL_TALK`, `retrieval_strategy` `NONE`, `message_label` GREETING/THANKS/FAREWELL, `message_label_latency_ms` dolu; V8 CHECK'leri bunu zaten kabul eder. Eski kalıp listesi turları (`message_label_latency_ms` NULL) olduğu gibi kalır | Kayıt şeması değişmiyor; geçmiş kayıt değiştirilmez | Eski turları geri doldurmak: geçmiş değişir, faydası yok |
| 5 | Yeniden yazma geçmişi sadece müşteri soruları: `QueryRewriter.historyText` her tur için `- <soru>` satırı yazar, `HistoryTurn.answer()` kullanılmaz. Cevap prompt'undaki geçmiş (`PromptBuilder`) değişmez | Kart S2 önerisi; asistan metni sorguya yapısal olarak giremez | Cevabı kısaltıp vermek: selamlama ilk cümlede olduğu için yine girer |
| 6 | `prompts/query-rewrite.st` Türkçe yazılır (metin aşağıda); `QueryRewriter.STANDALONE_QUERY_LABEL` `^(?i)(standalone query|arama sorusu):` olur | Kural 10 "talimatlar Türkçe"; model etiketi tekrar ederse silinir | İngilizce bırakmak: kural 10'la çelişir |
| 7 | Sistem talimatı kural sırası: bulamadım kuralı (3) giriş cümlesi kuralından (4) önce, rehber dışı konular açıkça sayılır; kural 4 sadece üç konu için "kural 3 yerine" der; "Bu kural kural 3'ten önce gelir" cümlesi kalkar (metin aşağıda) | Kök neden (TASK.md): giriş cümlesi genel ret olarak kullanılıyor | Giriş cümlelerini kodda denetlemek: konuyu ancak model anlar |
| 8 | `prompts/message-label.st`: SELAM tanımına "kendini tanıtma ya da sadece adını yazma"; kısa gerçek soru ve isim örnekleri eklenir (metin aşağıda) | Kart S4 önerisi; ilk mesajdaki kısa sorular da artık etikete gelir | Ayrı "isim" etiketi: yeni enum değeri + migration gerekir |
| 9 | Eşik ölçümü (M1) yeni kodla, `RETRIEVAL_MIN_RERANK_SCORE=` (boş → eşik kapalı) ile koşulur | Eşik kapalıyken her turun `contextChunks` puanları SSE'de görünür; yeniden yazma değişikliği takip sorularının puanını etkilediği için eski kodla ölçülmez | Retrieval + reranker'ı betikten doğrudan çağırmak: `RerankClient`'ın belge biçimini kopyalamak gerekir, ayrışma riski |
| 10 | Etiketin sonucu diğer olan kısa mesaj turunda `message_label` OTHER ve süre kaydedilir (bugünkü arama öncesi etiket davranışı) | "Her adımın sonucu tura kaydedilir" | Kaydetmemek: 1 sn'lik adım kayıtta görünmez |
| 11 | Ölçüm betiği ve listesi aynı dosyalarda genişler (`measure_answers.py`, `negative-messages.txt`); mevcut kategori adları (`small-talk-listed`, `small-talk-unlisted`) TASK-031 ölçümleriyle karşılaştırılabilsin diye değişmez, artık ikisi de etiket yolundan geçer | qa aynı betikle tekrar koşar | Yeni betik: aynı ayrıştırma iki kez yazılır |
| 12 | Golden'da "düşen" = cevabı bulamadım **ya da** hazır selamlaşma metni olan rehber sorusu | İlk mesajdaki kısa rehber soruları artık etikete gidiyor; yanlış selam etiketi de bir kayıptır | Sadece bulamadım saymak: yanlış etiket gizlenir |

## Sözleşme değişiklikleri
Alan, olay, endpoint, `errorCode` ve enum değişmez; sadece açıklama metinleri.

`docs/api/sse.md`:
- Durum notuna TASK-032.
- "Sıra": kalıp listesi maddesi ve "arama öncesi etiket (düzeltme turu 2)" maddesi tek maddede birleşti: "Selamlaşma, arama
  öncesi etiket (TASK-032)": her kısa mesaj, ilk mesaj dahil; kısa mesaj tanımı (≤ 10 kelime `?` yok, ≤ 5 kelime `?` var);
  olaylar bugünkü `NONE` + `SMALL_TALK` yoluyla aynı; `latencyMs` etiket süresini kapsar.
- "Reranker puan eşiği": kısa mesajda etiket zaten sorulmuştur; uzun mesajda etiket eşik altında sorulur.
- `sources` alan açıklamalarında "(kalıp listesi ya da arama öncesi etiket)" → "(arama öncesi etiket)".
- `AnswerSource.SMALL_TALK` açıklaması; eski kayıtlarda kalıp listesi turu notu.
- Örnekler: eşik altı selam örneği uzun mesajla (11 kelime); "Selam" örneği arama öncesi etiket yoluyla, `latencyMs` 840.

`docs/api/openapi.yaml`:
- Başlık yorumuna TASK-032.
- `sendMessage`: "Query rewrite" paragrafı (sadece müşteri soruları, bağımsız soru aynen); "Small talk by phrase list" ve
  "Small talk before rewrite" paragrafları yerine "Small talk before search (TASK-032)"; eşik paragrafında uzun mesaj notu ve
  değerin ölçümle seçildiği; "Check order" güncellendi.
- `TurnStatus` 3a ve `RetrievalStrategy.NONE` açıklamaları.

## Backend planı
Ek skill'ler: `rag-pipeline`, `sse-streaming`, `rag-evaluation` (ölçüm yorumu), `writing-good-tests` (betik değişikliklerinde).

### Dependency
Yok.

### Silinecekler
- `src/main/resources/small-talk/greeting.txt`, `thanks.txt`, `farewell.txt`, `address.txt` (klasör de).
- `generation/SmallTalkDetector.java`.
- `AnswerService`: `answerSmallTalkByPattern`, `PATTERN_ROUTE`, `SmallTalkDetector` bağımlılığı.

### Sınıflar (kök `com.rasitbank.assistant`)
**generation/**
- `ShortMessageDetector` (`@Component`, yeni; Türkçe sınıf yorumu: mesajın aramadan önce etiket sorulacak kadar kısa olup
  olmadığına karar verir): `boolean isShortMessage(String message)` → `SuggestionTextNormalizer.normalize(message)` boşsa
  `false`; kelime sayısı `n`; ham mesajda `?` varsa `n <= SHORT_QUESTION_MAX_WORDS (5)`, yoksa `n <= SHORT_MESSAGE_MAX_WORDS (10)`.
- `MessageLabeler`: sınıf yorumu güncellenir (kısa mesajda aramadan önce, uzun mesajda eşik altında); `foldTurkishLetters`
  buraya `private static` taşınır, `parseLabel` aynı kalır. `labelMessage` değişmez.
- `PromptBuilder`: değişmez (yeni `rag-system.st` metnini okur; yer tutucular aynı).

**retrieval/service/**
- `QueryRewriter`: `historyText(List<HistoryTurn>)` → her tur `"- " + turn.question()`, satırlar `\n` ile; `STANDALONE_QUERY_LABEL`
  = `^(?i)(standalone query|arama sorusu):`. Şablon değişkenleri aynı (`history`, `query`). Sınıf yorumu: "takip sorusunu
  müşterinin önceki sorularıyla …".

**conversation/service/**
- `AnswerService.answerWithRetrieval` yeni sıra:
  1. `shortMessageDetector.isShortMessage(question)` → `messageLabeler.labelMessage(question)`; log
     `Message labeled before search conversationId={} label={} durationMs={} failed={}`. Selamlaşmaysa
     `answerSmallTalkWithoutSearch` (log `route=PRE_SEARCH_LABEL`). Değilse sonuç `preSearchLabel` olarak saklanır.
  2. Yeniden yazma + retrieval (503 burada) — değişmez.
  3. Boş bilgi bankası — değişmez.
  4. `rerankScoreGate.isBelowThreshold(context)` → `answerBelowThreshold` (saklı etiket varsa yeniden sorulmaz) — değişmez.
  5. Model yolu — değişmez (draft'a saklı etiket ve süresi yazılır).
  Değişken `preRewriteLabel` → `preSearchLabel`, sabit `PRE_REWRITE_LABEL_ROUTE` → `PRE_SEARCH_LABEL_ROUTE = "PRE_SEARCH_LABEL"`.
  Sınıf yorumu güncellenir. `answerWithoutRetrieval` (LLM_ONLY) değişmez: orada etiket yok.
- `AnswerStreamer`, `TurnService`, `TurnStatusResolver`, `AnswerFinalizer`, `RerankScoreGate`: değişmez.

### Sistem talimatı `prompts/rag-system.st` (tam metin)
```
Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın. Kendinden birinci tekil şahısla söz edersin, örneğin "asistanıyım", "yardımcı olabilirim".
1. Sadece <parcalar> içindeki bilgiyle cevap ver; kendi bilgini ekleme.
2. <parcalar> içindeki bir bilgiye dayanan her cümlenin sonuna o parçanın numarasını yaz, örneğin [1]. Hiçbir parçaya dayanmayan cümleye numara yazma; selamlaşma, teşekkür ya da müşterinin kendini tanıtması gibi mesajlara yazdığın cümlelere de numara yazma.
3. Sorunun cevabı <parcalar> içinde yoksa sadece "{notFoundAnswer}" yaz; bu cümleye numara ya da başka bir şey ekleme. Hava durumu, şiir, sohbet, genel kültür, kripto para gibi rehberde olmayan her konu ve anlamsız mesajlar bu kurala girer.
4. Sadece şu üç konuda kural 3 yerine bu kural uygulanır; başka hiçbir mesajda bu üç cümleyi kullanma:
- Müşteri kendi hesabına özel bir bilgiyi (kendi bakiyesi, limiti, borcu, başvurusunun durumu, hesap hareketleri) soruyorsa cevaba tam olarak "{personalInfoOpener}" cümlesiyle başla.
- Müşteri başka bir bankanın ürününü ya da koşulunu soruyorsa "{otherBanksOpener}" cümlesiyle başla.
- Müşteri yatırım tavsiyesi ya da piyasa tahmini istiyorsa "{investmentAdviceOpener}" cümlesiyle başla.
Bu ilk cümleye numara yazma. <parcalar> içinde konuyla ilgili genel bir bilgi varsa (bu bilginin nereden görüleceği, nasıl değiştirileceği, hangi kanaldan yapılacağı) ardından onu kural 2'ye göre numarasıyla ekle; yoksa başka bir şey yazma.
```
`rag-user.st`, `llm-only-system.st` değişmez.

### Yeniden yazma şablonu `prompts/query-rewrite.st` (tam metin)
```
Bir bankanın dijital asistanına müşterinin yazdığı son mesajı, rehberde arama yapmak için tek başına anlaşılır bir soruya çevir.
Son mesaj müşterinin önceki sorularından birine dayanıyorsa (ör. "Peki ücretli mi", "Ya kredi kartında") eksik konuyu önceki sorulardan tamamla.
Son mesaj önceki sorulardan bağımsız yeni bir konuysa son mesajı hiç değiştirmeden aynen yaz.
Müşterinin niyetini koru; yeni bilgi, selamlama ya da açıklama ekleme. Sadece soruyu yaz.

Müşterinin önceki soruları:
{history}

Son mesaj:
{query}

Arama sorusu:
```

### Etiket talimatı `prompts/message-label.st` (tam metin)
```
Bir bankanın dijital asistanına gelen müşteri mesajını sınıflandırıyorsun. Mesaja cevap yazma.
SELAM: selamlaşma, hal hatır sorma, kendini tanıtma ya da sadece adını yazma.
TESEKKUR: teşekkür etme, memnuniyetini ya da minnettarlığını belirtme.
VEDA: vedalaşma, konuşmayı bitirme.
DIGER: soru, istek, şikâyet, anlamsız metin ve diğer her şey. Selam ya da teşekkürle birlikte soru da soran mesaj DIGER'dir. Önceki konuşmaya dayanan kısa bir soru ya da istek de DIGER'dir. Bankacılıkla ilgili kısa bir soru da DIGER'dir.
Sadece tek kelime yaz: SELAM, TESEKKUR, VEDA ya da DIGER.

Örnekler:
Hey, orada mısın? → SELAM
Hayırlı sabahlar, nasılsınız? → SELAM
Merhaba asistan, iyi misin? → SELAM
Ben Zeynep → SELAM
Adım Burak, merhaba → SELAM
Bu bilgi çok yardımcı oldu → TESEKKUR
Ellerinize sağlık, çok iyi açıkladınız → TESEKKUR
Çok naziksiniz, var olun → TESEKKUR
Süpersiniz, teşekkürler → TESEKKUR
Şimdilik bu kadar, hoşça kal → VEDA
Hadi bana müsaade → VEDA
İyi geceler, sonra yazarım → VEDA
Selam, kartımı kaybettim → DIGER
Teşekkürler ama kartım hâlâ gelmedi → DIGER
Yarın yağmur yağar mı? → DIGER
qwe asd → DIGER
Ya bireysel kredide → DIGER
Peki taksitli olursa → DIGER
O zaman diğer kart → DIGER
Şifremi unuttum → DIGER
Nakit avans faizi kaç? → DIGER
```
Kart S4'te B seçilirse SELAM tanımındaki "kendini tanıtma ya da sadece adını yazma" ve iki isim örneği DIGER'e taşınır.
T36 bir örneğin ölçüm listesi ya da golden set ile çakıştığını bulursa backend-coder örneği değiştirir ve raporlar.

### Properties
- `assistant.retrieval.min-rerank-score=${RETRIEVAL_MIN_RERANK_SCORE:<M1 sonrası kullanıcının onayladığı değer>}`. Değer
  gelene kadar -4.0 kalır; M2 onaylanan değerle koşulur.
- Başka anahtar değişmez. `assistant.small-talk.*` kalır (hazır metinler, `label-timeout`).

### Loglar (soru, sorgu, cevap, etiket çıktısı metni yok)
- `INFO Message labeled before search conversationId={} label={} durationMs={} failed={}`
- `INFO Small talk answered conversationId={} label={} route={}` (`route`: `PRE_SEARCH_LABEL` ya da `LABEL`)
- Diğer loglar değişmez.

### Ölçüm listesi `assistant-api/scripts/negative-messages.txt` (eklenenler; mevcut satırlar değişmez)
Başlık yorumu TASK-032'ye göre güncellenir (kalıp listesi yok; `small-talk-listed` / `small-talk-unlisted` adları karşılaştırma için).
- `small-talk-question` (soru işaretli selamlaşma): `Nasılsın?` · `Merhaba, iyi misin?` · `Selam, nasıl gidiyor?` ·
  `Naber, iyi misin?` · `İyi akşamlar, nasılsınız?`
- `small-talk-short` (TASK senaryolarından listede olmayan ve benzerleri): `Nber` · `Slm` · `Teşekkür ederim`
- `name`: `Ahmet` · `Ben Ayşe` · `Adım Mehmet`
- `off-topic` (ek): `Hava durumunu verir misin?`
- `not-in-guide` (ek): `Bitcoin alabilir miyim?`
- `gibberish` (ek): `jjkbjknlk`
- `personal-info-with-info`: `Kredi limitim ne kadar`
- `follow-up-independent`: `Selam >> Hava durumunu verir misin?` · `Ek kart nasıl alabilirim >> Hava durumunu verir misin?` ·
  `Kartımı kaybettim, ne yapmalıyım? >> Bana bir şiir yaz`
- `follow-up-question` (ek): `Ek kart nasıl alabilirim >> Peki ücretli mi`

### Ölçüm betiği `assistant-api/scripts/measure_answers.py` (ekler)
- Satır başına: `searchQuery`, `queryRewriteStatus` (`sources`'tan); `wrongOpener` (cevap normalize hâliyle üç giriş
  cümlesinden biriyle başlıyor **ve** bu satırın kategorisinin kendi giriş cümlesi değil; giriş cümlesi olmayan kategorilerde
  herhangi biri, golden dahil); `citedHeadings` (`citedChunks[].heading` listesi).
- `--threshold-candidates` (virgüllü, ör. `-3,-2,-1,-0.5,0`): özete `thresholdTable` ekler; her aday `c` için:
  `goldenBelow` (golden'da `topRerankScore < c` sayısı ve soru metinleri), `offTopicBelow` / `offTopicTotal`
  (`not-in-guide`, `off-topic`, `gibberish` ve `follow-up-independent` kategorileri), `targetedBelow` (`personal-info`,
  `other-banks`, `investment-advice`, `personal-info-with-info`; metinleriyle). `topRerankScore` null olan satırlar
  (arama yapılmamış) sayılmaz, ayrıca sayılır (`noScore`).
- Özet: golden `fallen` (bulamadım ya da `answerSource` `SMALL_TALK`, metinleriyle), `wrongOpener` toplamı; kategori başına
  `smallTalk`, `wrongOpener`, `withCitations`, `notFound`; kısa mesajlarda `latencyMs` medyanı (bilgi).

### Ölçüm adımları
- **Önkoşul:** rehber 2026.10 `DONE` (tek kaynak), golden set yüklü, `RETRIEVAL_STRATEGY=rerank`, `QUERY_REWRITE_ENABLED=true`.
  Bilgi bankasında fazladan belge varsa (ör. e2e'nin eklediği) ölçümden önce raporlanır; lead'e sorulmadan silinmez.
- **M1 (yeni kod, eşik kapalı):** assistant-api `RETRIEVAL_MIN_RERANK_SCORE=` ile yeniden açılır; golden 100 + bütün liste,
  `--threshold-candidates -3,-2,-1,-0.5,0` → `handoff/measure/m1-no-threshold.json`.
- **Aday tablosu:** backend-coder raporuna ve lead'e mesajla: golden `topRerankScore` en küçük 3 değer (sorularıyla) ve
  medyan; konu dışı kategorilerin en büyük 3 değeri (metinleriyle); `thresholdTable`; önerilen kural değeri
  `floor_0.5(goldenMin − 1.0)` (golden en düşüğünün 1 puan altı, 0,5'in katına aşağı yuvarlanmış) ve bu değerin tablodaki
  karşılığı; "Kredi limitim ne kadar"ın puanı. Kart S3'te B seçildiyse kural `(goldenMin + offTopicMax) / 2` (0,5'e aşağı),
  C seçildiyse `floor_0.5(goldenMin − 0.5)` olur; tabloya o değer de eklenir. Lead kullanıcıdan değeri alır, backend-coder'a iletir. Backend-coder bu
  sırada beklerken kod dışı işleri (CLAUDE.md) yapabilir; değeri kendisi seçmez.
- **M2 (yeni kod, onaylanan eşik):** golden 100 + bütün liste → `handoff/measure/m2-final.json`. Ayrıca Değerlendirme ekranı
  ölçümü bir kez (`POST /api/assistant/evals`, bitene kadar `GET`): recall@5, MRR ve kaçan sayısı rapora.
- **M3 (qa):** aynı betik, aynı sistem, bağımsız koşu → `handoff/measure/m3-qa.json`; qa e2e'den önce koşar.

### Kabul (M2 ve M3; betik otomatik sayar, aksi yazmadıkça)
1. **Selamlaşma:** `small-talk-listed` (13), `small-talk-unlisted` (5), `small-talk-question` (5), `small-talk-short` (3),
   `follow-up` (1), `follow-up-small-talk` (10) = 37 mesajın 37'si `answerSource` `SMALL_TALK`, alıntı 0.
2. **İsim (`name`, 3):** kart S4 = A ise 3/3 `SMALL_TALK`; B ise 3/3 bulamadım. İki durumda alıntı 0.
3. **Konu dışı (`not-in-guide` 13, `off-topic` 6, `gibberish` 5, `follow-up-independent` 3):** hepsi `notFoundAnswer` true,
   alıntı 0, `wrongOpener` false.
4. **Hedefli (`personal-info` 9, `other-banks` 3, `investment-advice` 3):** `targetedOk` true (TASK-031 ölçütü: bulamadım ya da
   kendi giriş cümlesi + giriş cümlesinde numara yok) ve `wrongOpener` false. Alıntılı cümleler qa tarafından parça metniyle
   elle karşılaştırılır (`GET /api/workspaces/42/chunks/{chunkId}`); dayanmayan alıntı 0.
5. **"Kredi limitim ne kadar":** kişisel bilgi giriş cümlesiyle başlar, giriş cümlesinde numara yok, alıntı ≥ 1 ve
   `citedHeadings`'ten biri "2.6" ile başlar. qa alıntıyı parçayla elle karşılaştırır.
6. **Takip soruları (`follow-up-question` 9, `follow-up-question-long` 7):** `SMALL_TALK` 0. "Ek kart nasıl alabilirim >> Peki
   ücretli mi": `notFoundAnswer` false, alıntı ≥ 1.
7. **Golden 100:** `fallen` ≤ kart S5 sınırı (önerilen 2); `wrongOpener` ≤ 2 (TASK-031'deki `goldenStartsWithOpener` sınırı);
   `paraphrasedNotFound` 0; `secondPersonSelfReference` 0. recall@5 ve MRR raporda (eşik ve etiket bu ölçüme girmez; önceki
   çalıştırmayla aynı olması beklenir).
8. Bütün listede `secondPersonSelfReference` 0.

### `assistant-api/CLAUDE.md` (backend-coder)
Durum yorumuna TASK-032; kalıp listesi satırlarının silinmesi; `ShortMessageDetector` ve kısa mesaj tanımı; yeniden yazmanın
sadece soruları kullandığı; yeni eşik varsayılanı; ölçüm betiğinin yeni argümanı.

## Frontend planı
Yok (frontend-coder çalışmaz). Web'de değişiklik yok; yeni `errorCode` yok. qa, Önizleme ve Konuşmalar'ın görünümünü kontrol eder.

## Test senaryoları
Sahte `ChatModel` gerçek modele gitmez. **Tester için önemli:** ilk turdaki kısa sorular (`?` ile ≤ 5 kelime, `?` olmadan ≤ 10
kelime; ör. "Gold kart aidatı ne kadar?") artık önce etiket çağrısı yapar. Mevcut integration testlerindeki sahte model,
sistem mesajı etiket talimatı olan çağrıya ayrı yanıt (ör. "DIGER") vermeli; çağrı sayısı beklentileri buna göre güncellenir.
Silinen: `SmallTalkDetectorTest` (yerine `ShortMessageDetectorTest`); TASK-031 T1-T3 (kalıp listesi), T13-T14 ve T18'in kalıp
listesi varsayımı (etiket yoluna çevrilir: T3, T6, T9).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T1 | Kısa mesaj tanımı | unit `ShortMessageDetectorTest` (parametreli) | `givenMessages_whenIsShortMessage_thenTenWordsWithoutQuestionMarkOrFiveWithIt` | "Selam" → true; "Nasılsın?" → true; "Merhaba, iyi misin?" → true; "Bir iki üç dört beş?" → true; "Bir iki üç dört beş altı?" → false; "Bir iki üç dört beş altı yedi sekiz dokuz on" → true; aynı + "onbir" → false; "Kredi limitim ne kadar" → true; "Gold kart aidatı ne kadar?" → true (5, `?`); "Ücret değişiklikleri bana ne kadar önceden bildiriliyor?" → false (6, `?`); "???" → false; "🙂 !!" → false; "   " → false |
| T2 | İlk mesaj selam | unit `AnswerServiceTest` | `givenFirstShortMessageLabeledGreeting_whenSendMessage_thenFixedReplyWithoutRewriteOrRetrieval` | Geçmiş boş, "Selam", etiket GREETING → `streamFixedAnswer` selam metni + `SMALL_TALK`; `QueryRewriter`, `ContextRetriever` etkileşimsiz; draft `messageLabel` GREETING, `messageLabelLatencyMs` dolu, `searchQuery` null, `QueryRewriteStatus.DISABLED` |
| T3 | Takip teşekkürü | unit | `givenHistoryAndShortThanks_whenSendMessage_thenFixedThanksWithoutRewriteOrRetrieval` | Geçmiş 1 tur, "Teşekkürler", etiket THANKS → teşekkür metni; yeniden yazma ve retrieval yok |
| T4 | Kısa mesaj, etiket diğer | unit | `givenShortMessageLabeledOther_whenSendMessage_thenNormalPathAndLabelAskedOnce` | Geçmiş boş, "Kredi limitim ne kadar", etiket OTHER, eşik üstü → model yolu; draft `messageLabel` OTHER; `labelMessage` 1 kez |
| T5 | Kısa mesaj, diğer + eşik altı | unit | `givenShortMessageLabeledOtherBelowThreshold_whenSendMessage_thenNotFoundWithoutSecondLabel` | "jjkbjknlk", etiket OTHER, ilk puan eşikten küçük → `BELOW_RERANK_THRESHOLD` bulamadım; `labelMessage` toplam 1 kez |
| T6 | retrieval-api kapalı + kısa teşekkür | unit | `givenRetrievalDownAndShortThanks_whenSendMessage_thenThanksReplyNot503` | `ContextRetriever` 503 fırlatacak şekilde kurulu, etiket THANKS → exception yok, teşekkür metni; `ContextRetriever` çağrılmadı |
| T7 | Uzun mesaj | unit | `givenLongMessage_whenSendMessage_thenNoLabelBeforeSearch` | 11 kelime `?` yok ve 6 kelime `?` var, eşik üstü → `labelMessage` hiç çağrılmadı; eşik altı fixture'ında yeniden yazma ve retrieval'dan **sonra** 1 kez çağrıldı |
| T8 | Etiket hatası | unit | `givenShortMessageAndLabelFailure_whenSendMessage_thenNormalPathWithoutError` | Geçmiş boş, `labelMessage` → OTHER, `failed` true → retrieval yapıldı, exception yok |
| T9 | Akışta ilk mesaj selam | integration `SendMessageIntegrationTest` (MockMvcTester, Testcontainers) | `givenFirstMessageGreeting_whenSendMessage_thenLabelThenFixedReplyWithoutSearch` | Sahte model etiket çağrısına "SELAM" → olaylar: tek token selam metni; sources `retrievalStrategy` NONE, `searchQuery` null, `queryRewriteStatus` DISABLED, `contextChunks` [], `citations` []; done `answerSource` SMALL_TALK, `notFoundAnswer` false; DB `message_label` GREETING, `message_label_latency_ms` dolu, `retrieval_strategy` NONE; sahte `RetrievalClient` çağrılmadı; model 1 kez çağrıldı |
| T10 | Sadece LLM | unit | `givenLlmOnlyModeAndShortGreeting_whenSendMessage_thenModelCalledAndLabelerNotCalled` | `MessageLabeler` ve `ShortMessageDetector` etkileşimsiz; model yolu `MODEL` |
| T11 | Kısa hesap sorusu, giriş + bilgi | integration | `givenShortPersonalQuestionLabeledOther_whenSendMessage_thenOpenerAndCitedInfo` | "Kredi limitim ne kadar", etiket "DIGER", eşik üstü; sahte cevap "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. ", "Limitini Raşit Mobil'de Kartlarım > Limit işlemleri'nden artırabilirsin [1]." → `citations` [1]; done `answer` iki cümle, `answerSource` MODEL; DB `message_label` OTHER, `cited` `{1}` |
| T12 | Etiket ayrıştırma (taşınan fonksiyon) | unit `MessageLabelerTest` (mevcut T26) | `givenModelOutputs_whenParseLabel_thenMapsExactLabelsElseOther` | Beklentiler aynı ("TEŞEKKÜR" → THANKS, "DİĞER" → OTHER, "SELAM VEDA" → OTHER) |
| T13 | Cevap talimatı | unit `PromptBuilderTest` (T12/T34 yerine) | `givenRagPrompt_whenBuild_thenOpenersLimitedToTheirTopicsAndUncitedSentencesAllowed` | Sistem mesajı: "Hiçbir parçaya dayanmayan cümleye numara yazma", "başka hiçbir mesajda bu üç cümleyi kullanma", "Hava durumu, şiir, sohbet", yapılandırmadaki üç giriş cümlesi ve bulamadım cümlesi; "Bu kural kural 3'ten önce gelir" ve "Hiçbir parçaya dayanmayan cümle yazma" **yok** |
| T14 | İsim etiket talimatında | unit `MessageLabelerTest` | `givenMessage_whenLabelMessage_thenSystemMessageTreatsSelfIntroductionAsGreeting` | Sistem mesajı "kendini tanıtma ya da sadece adını yazma" ve "Ben Zeynep → SELAM" satırını içerir; kullanıcı mesajı sadece müşteri mesajı (kart S4 = B ise DIGER satırları) |
| T15 | Yeniden yazmaya cevap gitmez | unit `QueryRewriterTest` | `givenHistoryWithAssistantGreeting_whenRewriteQuery_thenPromptHasOnlyPreviousQuestions` | Geçmiş `("Ek kart nasıl alabilirim", "Merhaba Ahmet, ben Raşit Bankası'nın dijital asistanıyım. Ek kartı Raşit Mobil'den alabilirsin [1].")`, soru "Peki ücretli mi" → yakalanan prompt "- Ek kart nasıl alabilirim" ve "Peki ücretli mi" içerir; "Merhaba Ahmet", "asistanıyım", "[1]", "ASSISTANT" içermez |
| T16 | Türkçe şablon, bağımsız soru talimatı | unit `QueryRewriterTest` | `givenFollowUp_whenRewriteQuery_thenTurkishInstructionKeepsIndependentQuestion` | Prompt "bağımsız yeni bir konuysa son mesajı hiç değiştirmeden aynen yaz" ve "Arama sorusu:" içerir; "Standalone query" içermez |
| T17 | Etiket satırı silinir | unit `QueryRewriterTest` (parametreli) | `givenModelOutputWithLabel_whenRewriteQuery_thenLabelStripped` | "Arama sorusu: Ek kart ücretli mi?" → "Ek kart ücretli mi?"; "ARAMA SORUSU: Ek kart ücretli mi?" → aynı; "Standalone query: Ek kart ücretli mi?" → aynı |
| T18 | Ölçüme sızma yok (T36 genişler) | unit `MessageLabelerTest` | `givenLabelPromptMeasurementListAndGoldenSet_whenCompared_thenNoMessageOverlapsAnExample` | `negative-messages.txt`'teki her mesaj (iki turlu satırların ikisi de) ve `docs/rehber/golden/rehber-golden-v1.json`'daki her soru, her örnekle test tarafındaki anahtarla (normalize + Türkçe harf ASCII + tekrar harf teke; silinen `phraseKey` ile aynı) karşılaştırılır: eşit değil; 3+ kelimelilerde kelime sınırında içerme yok. Özellikle "Ahmet", "Ben Ayşe", "Adım Mehmet", "Nasılsın?", "Merhaba, iyi misin?" örneklerde yok |
| T19 | Log sınırı | integration (`OutputCaptureExtension`) | `givenPreSearchLabelTurn_whenLogsCaptured_thenRouteLoggedWithoutMessageText` | `Message labeled before search … label=GREETING`, `Small talk answered … route=PRE_SEARCH_LABEL`; "Selam" ve hazır metin logda yok; `route=PATTERN` hiç yok |
| E1 | Selam uçtan uca | e2e `acceptance.spec.ts` (mevcut, gerçek sistem) | `givenGreeting_whenSent_thenFixedReplyWithoutCitation` | Değişmeden geçer (artık etiket yolundan) |
| M1-M3 | Gerçek modellerle ölçüm | backend-coder, qa | Backend planı "Ölçüm adımları" | "Kabul" maddeleri |

## Deployment planı
Yeni servis, port, model, ortam değişkeni yok. `RETRIEVAL_MIN_RERANK_SCORE` varsayılanı M1 sonrası onaylanan değer olur
(boş → eşik kapalı, değişmez). Compose dosyası değişmez.

## Açık sorular ve riskler
- Eşiği seçmek için ölçüm gerekiyor: M1 bitmeden backend-coder'ın işi tamamlanamaz; lead M1 sonrası kullanıcıya ikinci kez
  sorar (kart S3'ün ikinci adımı).
- İlk mesajdaki kısa gerçek sorular (golden'ın büyük kısmı 6+ kelime ve `?` taşıdığı için etikete girmez; kısa olanlar girer)
  ~1 sn ek süre alır; yanlış selam etiketi riski golden `fallen` ile ölçülür.
- Dil modeli kapalıyken selamlaşma artık anında karşılık almaz (G3).
- Talimat değişikliği modelin davranışına dayanır; "giriş cümlesi sadece kendi konusunda" deterministik denetlenmez, ölçümle
  kontrol edilir (`wrongOpener`). Ölçümde çıkarsa raporlanır.
- Kapsam dışı not: `docs/rehber/rehberde-olmayan-konular.md` "Kişiye özel diğer bilgiler"de beklenen cevabı sadece bulamadım
  yazıyor; TASK-031 ve bu task kişisel bilgi sorularında giriş cümlesi + genel bilgiyi kabul ediyor. Doküman bu task'ta
  değişmez (kullanıcı dosyası değil ama kapsam dışı); ileride güncellenmesi gerekebilir.

## Düzeltme turu 1
**Sebep (M1, `handoff/02-backend-implementation.md` "M1", `handoff/measure/m1-no-threshold.json`):** golden en düşük
-2,74 → kural A = -4,0. "Nber" etiket adımında DIGER aldı, arandı (-3,03), model "Başka bankaların …" yazdı (yanlış giriş
cümlesi; kabul 1 ve 3 tutmaz). "???" 0 kelime olduğu için etiketlenmedi, reranker 1,23 verdi, model "… asistanıyım … [1]"
yazdı (uydurma alıntı; hiçbir aday eşik kesmez). **Kullanıcı kararları (kart 6, 7; lead aracılığıyla, 2026-11-04):** eşik
-4,0; etiket talimatına kısaltılmış / yazım hatalı selam örnekleri; harf ya da rakam içermeyen mesaj modele (ve aramaya)
gitmeden bulamadım alır.

| # | Madde | Plandaki ya da sözleşmedeki değişiklik |
|---|-------|----------------------------------------|
| D1 | Eşik -4,0 | `assistant.retrieval.min-rerank-score=${RETRIEVAL_MIN_RERANK_SCORE:-4.0}` (bugünkü değer aynen kalır; kod değişmez). M2 bu değerle |
| D2 | Kısaltılmış selam örnekleri | `prompts/message-label.st` "Örnekler"e SELAM satırlarından sonra (aşağıda); T18 kapsar |
| D3 | Harf/rakamsız mesaj → bulamadım, model ve arama yok | Yeni `AnswerSource.NO_WORDS`, V9 migration, `AnswerService` adım 0 (aşağıda) |
| D4 | Web yeni değeri tanımalı | Bugünkü web `done` olayını bilinmeyen `answerSource`'la atlar (TASK-031 W5) → akış "yarıda kesildi" görünür. `ANSWER_SOURCES`'a ekleme + Önizleme sağ kartı (aşağıda). **frontend-coder bu turda çalışır** |
| D5 | Sözleşme | sse.md: `AnswerSource` `NO_WORDS`, "Sıra" maddesi, `sources` alan açıklamaları, "İsimler"; openapi: `sendMessage` paragrafı ve "Check order", `RetrievalStrategy.NONE`, `TurnStatus` kural 4 |

### D2: `prompts/message-label.st` eklenen örnekler
"Merhaba asistan, iyi misin? → SELAM" satırından sonra, "Ben Zeynep → SELAM" satırından önce:
```
Mrhb → SELAM
Meraba → SELAM
Slmlr → SELAM
Nbr kanka → SELAM
Gnydn → SELAM
```
"Bu bilgi çok yardımcı oldu → TESEKKUR" satırından önce `Tşkkrler → TESEKKUR`; "Şimdilik bu kadar, hoşça kal → VEDA"
satırından önce `Grşrz → VEDA`. Ölçülen "Nber", "Slm", "Naber, iyi misin?" ve diğer mesajlar örnek değildir (T18: eşitlik ve
kelime sınırında içerme yok; "nbr" ≠ "nber", "slmlr" ≠ "slm"). T18 çakışma bulursa backend-coder örneği değiştirir ve raporlar.

### D3: harf ya da rakam içermeyen mesaj
- **Tanım:** `SuggestionTextNormalizer.normalize(message)` boş (harf ve rakam dışındaki her karakter silinince bir şey kalmıyor):
  "???", "?!", "🙂", "... !!". `ShortMessageDetector`'a `boolean hasNoWords(String message)`; `isShortMessage` bu mesajlarda
  `false` döner (değişmez).
- **Sıra (`AnswerService.answerWithRetrieval`, sadece `RAG`):** 409 kontrolünden ve geçmiş okunduktan sonra, **adım 0**:
  `shortMessageDetector.hasNoWords(question)` → `answerWithoutWords(...)`: etiket, yeniden yazma, retrieval, rerank ve dil
  modeli çağrılmaz; `TurnDraft` `searchQuery` null, `QueryRewriteStatus.DISABLED`, context null, `messageLabel` null, süre
  null; `answerStreamer.streamFixedAnswer(draft, properties.generation().notFoundAnswer(), AnswerSource.NO_WORDS, emitter)`.
  Log: `INFO Message without words answered conversationId={} questionLength={}`. `LLM_ONLY` değişmez (orada adım 0 yok).
- **Tur kaydı:** `generation_mode` RAG, `retrieval_strategy` NONE, `search_query`/`candidates`/`top_k` NULL,
  `query_rewrite_status` DISABLED, `not_found_answer` true, `answer` sabit cümle, `cited` `{}`, `message_label` NULL,
  `answer_source` `NO_WORDS`, parça yok. `TurnService` NONE'u `searchQuery == null`'dan türetir (bugünkü selamlaşma yolu gibi).
- **Durum (`TurnStatusResolver`, değişmez):** kural 4 bulamadım → cevap parçası yok → `DONT_KNOW` (BİLMİYOR).
- **Migration `V9__add_no_words_answer_source.sql`:**
  - `ck_conversation_turns_answer_source` düşürülüp `IN ('MODEL','EMPTY_KNOWLEDGE_BASE','BELOW_RERANK_THRESHOLD','SMALL_TALK','NO_WORDS')` ile yeniden.
  - `ck_conversation_turns_none_reason` düşürülüp `CHECK (retrieval_strategy <> 'NONE' OR generation_mode = 'LLM_ONLY' OR answer_source IN ('SMALL_TALK','NO_WORDS'))` ile yeniden.
  - Yeni `ck_conversation_turns_no_words CHECK (answer_source <> 'NO_WORDS' OR (generation_mode = 'RAG' AND retrieval_strategy = 'NONE' AND not_found_answer AND cardinality(cited) = 0 AND message_label IS NULL))`.
  - Geri doldurma yok (eski "???" turları `MODEL` kalır).
- **Neden yeni değer:** mevcut değerlerin hiçbiri bu turu doğru anlatmıyor: `EMPTY_KNOWLEDGE_BASE`'te web "Bilgi bankasında
  henüz belge yok." yazar; `BELOW_RERANK_THRESHOLD` `RERANK` stratejisi ister ve web "rerank puanı eşiğin altında kaldı" yazar;
  `SMALL_TALK` bulamadım'a izin vermez; `MODEL` + `NONE` CHECK'e takılır. Arama yapıp eşik altı gibi kaydetmek puan 1,23
  olduğu için yanlış kayıt olur.

### D4: frontend planı (bu tur)
Ek skill'ler: `sse-streaming`, `react-testing`.
- `features/preview/model/types.ts`: `ANSWER_SOURCES = ['MODEL', 'EMPTY_KNOWLEDGE_BASE', 'BELOW_RERANK_THRESHOLD', 'SMALL_TALK', 'NO_WORDS'] as const`.
- `features/preview/pages/PreviewPage.tsx`: `ContextChunksCard` koşulunda `answerSource` `SMALL_TALK` **ya da** `NO_WORDS`
  ise kart gösterilmez (yerine `HowItWorksCard`, bugünkü selamlaşma davranışı). Arama yapılmadığı için "Son cevapta modele
  giden parçalar" kartının anlamı yok.
- `AnswerMessage`: değişmez (bulamadım cevabı olduğu için "Temsilciye bağlan" önerisi bugünkü kurala göre görünür; boş bilgi
  bankası notu sadece `EMPTY_KNOWLEDGE_BASE`'te).
- Konuşmalar: değişmez (`NONE` → "RAG", parça yok → "Bu turda modele hiçbir parça gitmedi.", durum BİLMİYOR).
- Yeni metin, paket, `errorCode` yok. `web/CLAUDE.md`'de `done.answerSource` değerlerine `NO_WORDS`.
- Rapor: `handoff/02-frontend-implementation.md`.

### Testler (eklenen; tester)
| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T20 | Harf/rakam yok tanımı | unit `ShortMessageDetectorTest` (parametreli) | `givenMessages_whenHasNoWords_thenTrueOnlyWithoutLettersOrDigits` | "???" → true; "?!" → true; "🙂" → true; "... !!" → true; "a?" → false; "2?" → false; "Selam" → false |
| T21 | Akış öncesi yol | unit `AnswerServiceTest` | `givenMessageWithoutWords_whenSendMessage_thenNotFoundWithoutLabelRewriteRetrievalOrModel` | "???" → `streamFixedAnswer` bulamadım + `NO_WORDS`; `MessageLabeler`, `QueryRewriter`, `ContextRetriever`, `ChatModel` etkileşimsiz; draft `searchQuery` null, `messageLabel` null |
| T22 | retrieval-api kapalı | unit | `givenRetrievalDownAndMessageWithoutWords_whenSendMessage_thenNotFoundNot503` | Exception yok, bulamadım |
| T23 | Akış ve kayıt | integration `SendMessageIntegrationTest` | `givenMessageWithoutWords_whenSendMessage_thenSingleNotFoundTokenAndNoWordsTurn` | Olaylar: tek token sabit cümle; sources `retrievalStrategy` NONE, `searchQuery` null, `queryRewriteStatus` DISABLED, `contextChunks` [], `citations` []; done `notFoundAnswer` true, `answerSource` NO_WORDS, model süreleri null; DB `answer_source` NO_WORDS, `retrieval_strategy` NONE, `not_found_answer` true, `cited` `{}`, `message_label` NULL; sahte model ve `RetrievalClient` çağrılmadı |
| T24 | Migration | integration (Flyway, Testcontainers) | `givenV9_whenInsertNoWordsTurns_thenOnlyNotFoundNoneWithoutLabelAccepted` | RAG + NONE + NO_WORDS + not_found + cited {} + label NULL → kabul; NO_WORDS + RERANK → red; NO_WORDS + not_found false → red; NO_WORDS + message_label OTHER → red; RAG + NONE + MODEL → hâlâ red |
| T25 | Durum | unit `TurnStatusResolverTest` | `givenNoWordsTurn_whenResolve_thenDontKnow` | `DONT_KNOW` |
| T26 | Sadece LLM | unit | `givenLlmOnlyModeAndMessageWithoutWords_whenSendMessage_thenModelCalled` | Model çağrıldı, `answerSource` MODEL |
| T27 | Kısaltma örnekleri prompt'ta | unit `MessageLabelerTest` | `givenMessage_whenLabelMessage_thenSystemMessageContainsAbbreviatedGreetingExamples` | Sistem mesajı "Mrhb → SELAM" ve "Nbr kanka → SELAM" satırlarını içerir; "Nber → " satırı yok |
| W1 | Olay daraltma | unit `answerStreamEvents.test.ts` | `givenDoneWithNoWordsSource_whenParsed_thenAccepted` | `answerSource` `NO_WORDS` olan `done` daraltılır (atlanmaz); `OTHER` yine atlanır |
| W2 | Önizleme | component `PreviewPage.test.tsx` (MSW SSE) | `givenNoWordsDone_whenFinished_thenNotFoundSentenceWithoutContextCard` | Balonda sadece bulamadım cümlesi; "Son cevapta modele giden parçalar" yok; "Bilgi bankasında henüz belge yok." yok; alıntı düğmesi yok; "Temsilciye bağlan" var |

T18 (sızma) yeni örnekleri de kapsar; değişmez.

### Kabul değişikliği (M2, M3)
- Kabul 3'te `gibberish` "???": `answerSource` `NO_WORDS`, `notFoundAnswer` true, alıntı 0, `topRerankScore` null.
- Kabul 1 "Nber" dahil 37/37 `SMALL_TALK` aynen geçerli.
- Betik: `route_of`/özet `NO_WORDS`'ü ayrı sayar (kategori başına `noWords`).

### Gözden kaçabilecek durumlar (güncelleme)
G4 değişti: sadece noktalama / emoji ("???", "🙂") → `NO_WORDS`, bulamadım, arama ve model yok (T20-T23).

### Roller ve kart
frontend-coder bu turda çalışır (D4); kartın "Çalışacak roller" bölümü değişti, kart güncellendi, son satır `ONAY: BEKLİYOR`.
backend-coder: D1-D3, V9, betik, `assistant-api/CLAUDE.md`. tester: T20-T27, W1-W2. qa: değişmez, kabul değişikliğiyle.

## Plan notu: qa düzeltmesi B1 (backend-coder önerisi, planner kabul etti)
`prompts/rag-system.st` kural 4'ün giriş satırı değişir; maddeler ve son cümle aynı kalır:
- Eski: `4. Sadece şu üç konuda kural 3 yerine bu kural uygulanır; başka hiçbir mesajda bu üç cümleyi kullanma:`
- Yeni: `4. Bu kural sadece müşterinin son mesajı doğrudan şu üç konudan birini soruyorsa kural 3 yerine uygulanır. Başka hiçbir mesajda (önceki konuya dayanan takip soruları, ürün ve ücret soruları dahil) bu üç cümleyi cevabın başında, ortasında ya da sonunda kullanma:`
- Gerçek modelle denendikten sonra kesin metin `02-backend-implementation.md`'dedir; o metin geçerlidir.
- **T13 beklentisi değişir:** sistem mesajı "Başka hiçbir mesajda" ve "bu üç cümleyi cevabın başında, ortasında ya da sonunda
  kullanma" ifadelerini içerir (eski "başka hiçbir mesajda bu üç cümleyi kullanma" ifadesi aranmaz). Diğer T13 beklentileri aynı.
- Sözleşme ve kart değişmez.

## Plan notu 2: qa düzeltmesi B1, yerinde olmayan giriş cümlesinin silinmesi (planner kararı)
**Neden:** backend-coder 6 kural 4 varyantı denedi. B1'i kapatan tek başlık "Kredi limitim ne kadar"daki genel bilgi ve
alıntıyı kaybettiriyor (kabul 5). Sadece talimatla ikisi birlikte sağlanamıyor. Kural 6'ya göre giriş cümlesi cevabın ilk
cümlesi değilse hiçbir mesajda doğru değildir. Bu yüzden deterministik olarak silinir (backend-coder'ın (b) seçeneği).
Kabul 5 gevşetilmez; (c) gerekmez.

- **Talimat:** backend-coder, kabul 5'i ("Kredi limitim ne kadar" → giriş cümlesi + 2.6 alıntısı) sağlayan kural 4 varyantını
  seçer (Plan notu 1'deki metin ya da denediği başka bir varyant). B1'i talimatın tek başına kapatması gerekmez. Kesin metin
  `02-backend-implementation.md`'dedir; T13 beklentisi o metne göre güncellenir.
- **Kural (`AnswerFinalizer.finalizeRagAnswer`):** yeni adım, TASK-031 D5'in (2) "ilk cümledeki giriş cümlesi alıntıları"
  adımından sonra, (3) "aralık dışı `[n]`" adımından önce gelir. "Bulamadım" tespiti (1) tutarsa bu adıma gelinmez.
  - Cümleler: bitiş işareti (`.`, `!`, `?`) ve hemen arkasından, arada sadece boşluk olan `[n]` işaretleriyle biter.
  - İlk cümle hiç silinmez.
  - Diğer her cümlenin `[n]` işaretleri çıkarılıp `SuggestionTextNormalizer.normalize` ile normalize edilmiş hâli üç giriş
    cümlesinden birinin **özünü** içeriyorsa cümle silinir. Silme, cümlenin arkasındaki `[n]` işaretlerini ve önündeki
    boşluğu da kapsar.
  - Öz: yapılandırmadaki giriş cümlesi (`assistant.generation.*-opener`) aynı normalizasyondan geçer, ilk kelimesi atılır.
    Örnek: "özel bilgileri bakiye limit borç başvuru durumu hesap hareketleri göremiyorum", "bankaların ürünleri ve koşulları
    hakkında bilgi veremiyorum", "tavsiyesi ya da piyasa tahmini veremiyorum" (Türkçe harfler korunur).
    Özler properties'ten türetilir, kodda metin olarak yazılmaz.
  - Sonra (3) ve (4) aynen çalışır: `citations` temizlenmiş metinden hesaplanır, silinen cümlenin numaraları alıntı sayılmaz.
- **Akış:** silinen cümle `token` olarak akarken görünmüş olabilir. `done.answer` temizlenmiş metindir ve web ekrandaki metni
  onunla değiştirir. Bu, TASK-031'deki "başka kelimelerle bulamadım" ile aynı sınırdır. sse.md `done.answer` tanımı bu sınırı
  zaten "silinen numaralar … görünmüş olabilir" diye anlatıyor. Planner tanıma "yerinde olmayan giriş cümlesi" cümlesini ekler.
- **Log:** `INFO Misplaced opener removed conversationId={} removedCount={}`.
- **Ölçüm:** qa düzeltme maddesi 2'deki `openerAnywhere` sayacı (giriş cümlesinin özü, satırın kategorisine ait değilse
  cevabın herhangi bir yerinde; `follow-up-*` dahil) `done.answer` üzerinde 0 olmalı. Ayrıca `streamedText` üzerinde bilgi
  amaçlı sayılır. Kabul 5 aynen geçerli.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T28 | Ortadaki giriş cümlesi silinir | unit `AnswerFinalizerTest` (parametreli, 5 parça) | `givenOpenerAfterFirstSentence_whenFinalize_thenOpenerSentenceRemovedAndCitationsRecomputed` | (a) "Ek kartın yıllık aidatı yoktur [1]. Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. Kredi kartında aidat 750 TL'dir [2]." → "Ek kartın yıllık aidatı yoktur [1]. Kredi kartında aidat 750 TL'dir [2].", citations [1, 2]. (b) "Aidat 750 TL'dir [1]. Hesabınıza özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum [3]." → "Aidat 750 TL'dir [1].", citations [1]. (c) "Aidat 750 TL'dir [1]. Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum." → "Aidat 750 TL'dir [1].". (d) "Altın hesabı Raşit Mobil'den açılır [1]! Yatırım tavsiyesi ya da piyasa tahmini veremiyorum. [2]" → "Altın hesabı Raşit Mobil'den açılır [1]!", citations [1] |
| T29 | İlk cümledeki ve ilgisiz cümleler kalır | unit | `givenOpenerAsFirstSentenceOrSentenceMentioningBalance_whenFinalize_thenKept` | "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. Limitini Raşit Mobil'de Kartlarım > Limit işlemleri'nden artırabilirsin [1]." → aynı, citations [1]; "Kartını kapat [1]. Bakiyeni Raşit Mobil'de görebilirsin [2]." → aynı, citations [1, 2] |
| T30 | Akışta | integration `SendMessageIntegrationTest` | `givenModelWritesOpenerMidAnswer_whenSendMessage_thenDoneAnswerAndRecordWithoutOpener` | Sahte model "Kredi kartının yıllık aidatı 750 TL'dir [1]. ", "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. [2]" → token'lar olduğu gibi; sources `citations` [1]; done `answer` "Kredi kartının yıllık aidatı 750 TL'dir [1]."; DB `answer` aynı, `cited` `{1}` |

T37-T38 (TASK-031) ve T13 dışındaki testler değişmez. Kart değişmez: kural 6 ("giriş cümleleri sadece kendi konusunda")
aynı, uygulanış biçimi değişti.

STATUS: DONE
