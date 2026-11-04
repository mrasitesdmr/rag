TASK: TASK-031
# Plan

## Özet
**Kullanıcı kararları (kart soruları, 2026-11-03):** S1 = D (reranker puan eşiği + talimat + cevap denetimi). S2 = A
(top-5 ve reranker kapalıyken eşik yok; talimat + denetim çalışır). S3 = hazır metin; tanıma **kalıp listesi + eşik
altında dil modeline sadece etiket sorusu** (SELAM / TESEKKUR / VEDA / DIGER; model cevap yazmaz, parça verilmez).
Top-5 / reranker kapalıyken sadece kalıp listesi. Etiket çağrısı hata / zaman aşımı / tanımsız çıktıda DIGER. S4 = B
(yeni durum `SMALL_TALK`, "SOHBET", gri). S5 = B (en fazla 2 rehber sorusu "bulamadım"a düşebilir).

**Mevcut durum:** RAG modunda soru her zaman yeniden yazma → retrieval → rerank → dil modeli yolundan geçer (boş bilgi
bankası hariç). Sistem talimatı (`prompts/rag-system.st`) "Sen … asistanısın" diye başlar ve "cevap yoksa sadece
bulamadım yaz" der. Model "Selam"a "Merhaba, … asistanısınız … [1]", Bitcoin'e "… bulamadım. [5]" yazıyor (TASK-018 qa).
`AnswerStreamer` token'ları olduğu gibi akıtır; `NotFoundAnswerDetector` "içeriyor mu" bakar ama metni değiştirmez;
`CitationParser` aralıktaki her `[n]`'i alıntı sayar, "bulamadım" cevabında da. Web akan metni gösterir; `[5]` aralıkta
olduğu için tıklanabilir alıntı olur, Önizleme kartında 5. parçada ✓ çıkar.

**Yeni durum (sadece RAG modu; Sadece LLM değişmez):**
1. **Kalıp listesi:** mesajın tamamı selam / teşekkür / veda kalıplarından oluşuyorsa (harf büyüklüğü, noktalama, emoji,
   Türkçe karakter yerine ASCII, harf uzatma "selaaam" fark etmez) yeniden yazma, retrieval, rerank ve dil modeli
   çağrılmaz; hazır metin akar. Tur `answer_source = SMALL_TALK`, strateji `NONE`.
2. **Reranker puan eşiği:** strateji `RERANK` iken ilk parçanın `rerankScore`'u `assistant.retrieval.min-rerank-score`'dan
   küçükse cevap modeli çağrılmaz. Önce dil modeline müşterinin mesajı tek başına verilip **sadece etiket** sorulur:
   SELAM / TESEKKUR / VEDA → hazır metin (`SMALL_TALK`); DIGER, hata, zaman aşımı ya da tanımsız çıktı → sabit
   "bulamadım" (`BELOW_RERANK_THRESHOLD`). Prompt'a parça girmez; adaylar puanlarıyla kaydedilir. `TOP_K` ve
   `RERANK_FALLBACK`'te eşik ve etiket yok.
3. **Talimat:** birinci tekil şahıs; parçaya dayanmayan cümle yazma; bulamadım cümlesine numara ya da başka bir şey ekleme.
4. **Denetim:** akışta "bulamadım" cümlesinin başı tutulur (cümle tamamlanınca tek parça gider, sonrası gitmez); cevap
   bitince "bulamadım" cümlesini içeren cevap tam o cümleye indirilir ve alıntısız kaydedilir; aralık dışı `[n]` kayıttan
   silinir. `done` olayı son metni (`answer`) ve yolu (`answerSource`) taşır; web ekrandaki metni onunla değiştirir.
5. Selamlaşma turları (iki yoldan da) Konuşmalar'da `SMALL_TALK` (SOHBET) görünür ve sonraki turların geçmişine girmez.

**Değişen servisler:** assistant-api (akış, prompt, etiket çağrısı, tur kaydı, V8 migration) ve web (Önizleme,
Konuşmalar). retrieval-api değişmez. Değerlendirme ölçümü (`EvalRunRunner`) değişmez: eşik `AnswerService`'te uygulanır,
`ContextRetriever`'da değil.

**Uygulama sırası (backend-coder):** M0 ölçüm betiği + bugünkü kodla ölçüm → M1 eşik seçimi ve selam puanlarının
kontrolü → kod → M2 yeni kodla ölçüm.

**Yeniden adlandırma:** yok. Yeni adlar: `AnswerSource`, `MessageLabel`, `SmallTalkDetector`, `MessageLabeler`,
`MessageLabelResult`, `RerankScoreGate`, `NotFoundAnswerStreamFilter`, `AnswerFinalizer`, `FinalAnswer`,
`TurnStatus.SMALL_TALK`.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/sse.md`, `docs/api/openapi.yaml`.
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/scripts/**`, `assistant-api/CLAUDE.md`; M0-M2 gerçek
  modellerle. Rapor `handoff/02-backend-implementation.md` (+ ölçüm dosyaları `handoff/measure/`).
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md`. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**`, `web/e2e/**`. Rapor `handoff/03-test-report.md`.
- **qa:** M3 bağımsız ölçüm, kabul kriterleri, sözleşme uyumu, görünüm karşılaştırması. Sistemi
  `docker compose -f deployment/docker-compose.yml up -d --build` ile açar. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** devops. Docker, nginx ve kurulum değişmez; yeni ayarlar `application.properties` varsayılanıyla gelir,
  yeni model yok (etiket çağrısı mevcut dil modeliyle). qa, tester'a bağlanır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Alıntı sadece dayanılan parçaya; karşılığı olmayan mesajda `[n]`, "Rehber · s. N" ve ✓ yok | `SmallTalkDetector`, `RerankScoreGate`, `MessageLabeler`, `NotFoundAnswerStreamFilter`, `AnswerFinalizer`, web `done.answer` | T6-T11, T15-T17, T29, W1-W3, M2, M3 |
| K2 | Bulamadım cevabı tektir; numara görünmez, numarasız kaydedilir; tur BİLMİYOR | `NotFoundAnswerStreamFilter`, `AnswerFinalizer`, `TurnService` | T6, T9, T10, T16, T20, W1 |
| K3 | Selam/teşekkür/vedaya kısa nazik karşılık, rehberde ne sorulabileceği, alıntı yok; hazır metin; Konuşmalar'da SOHBET | `SmallTalkDetector`, `MessageLabeler`, `assistant.small-talk.*`, `TurnStatusResolver`, web `turnStatus.ts` | T1-T3, T13, T14, T18, T20, T22, T26-T30, W2, W6, M2 |
| K4 | Karşılığı olmadığı nasıl anlaşılır (karar D); reranker kapalı ve top-5'te eşik yok (karar A) | `RerankScoreGate` + talimat + denetim | T4, T5, T15, T30, M1 |
| K5 | Birinci tekil şahıs; "asistanısınız" çıkmaz | `prompts/rag-system.st`, hazır metinler | T12, M2, M3 (`secondPersonSelfReference` = 0) |
| K6 | Golden set soruları bozulmaz; bulamadım'a düşen ≤ 2 | M0/M2 karşılaştırması | M0, M2, M3 |
| K7 | Talimatlar Türkçe, cevap Türkçe | `prompts/rag-system.st`, `prompts/message-label.st`, hazır metinler | T12, T28 |
| S1 | "Selam" → kısa selam, `[n]` ve düğme yok | Kalıp listesi | T1, T13, W2, E1 |
| S2 | "Bitcoin alıp satabilir miyim?" → sadece cümle, numara yok, BİLMİYOR | Eşik + etiket ya da filtre + finalizer | T15, T16, W1, E2, M2 |
| S3 | "Bugün hava nasıl?", "Bana bir şiir yaz", "asdfgh" → alıntısız bulamadım | Eşik + etiket (DIGER), filtre, finalizer | T15, T16, M2 |
| S4 | "Teşekkürler" → kısa karşılık, alıntı yok | Kalıp listesi | T2, T13 |
| S5 | "Kartımı kaybettim" → bugünkü gibi cevap + alıntı | Değişmeyen yol | T7, T17, M2 |
| S6 | Gold kart → Teşekkürler: ikinci cevap alıntısız; yeniden yazma teşekkürü çevirmez | Kalıp listesi yeniden yazmadan önce | T18, T19, M2 |
| AC1 | Kural 4 kararı kullanıcıdan | Verildi (D, top-5/fallback A) | Kart |
| AC2 | Gerçek modellerle negatif listede alıntılı cevap 0 | M2, M3 | M2, M3 |
| AC3 | Golden set cevap/alıntı sonuçları raporda, ≤ 2 düşüş | M0, M2, M3 | M2, M3 |
| AC4 | Alıntının atılması ve bulamadım tespiti deterministik testli; build'ler geçer | tester | T6-T11, T16, T26, T27; `./mvnw verify`, `npm run build/lint/test` |
| AC5 | Önizleme ve Konuşmalar görünümü lab'la aynı | Sadece iki yeni metin (Kararlar #10, SOHBET) | W2-W4, W6, qa |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Selamla başlayan gerçek soru ("Merhaba, kartımı kaybettim") | Kalıp listesi tutmaz; puan eşiği geçer → normal RAG cevabı, alıntılı | T3, M2 |
| G2 | Yazım çeşitleri ("SELAM!!", "selaaam", "tesekkurler", "İyi günler 🙂") | Kalıp listesi tutar | T1, T2 |
| G3 | Model cümleden önce metin yazar ("Maalesef, bu konuda rehberimizde bilgi bulamadım. [2]") | Kaydedilen ve `done.answer` = sadece cümle, `citations` []; web akan metni cümleyle değiştirir | T10, W1 |
| G4 | Gerçek cevap "Bu" ile başlar ("Bu kartın yıllık aidatı … [1].") | Tutulan metin ayrışınca gönderilir, cevap ve alıntı aynen kalır | T7, T17 |
| G5 | Eşik altındaki mesajda dil modeli kapalı / yavaş | Etiket çağrısı hata ya da `label-timeout` → DIGER → "bulamadım"; hata dönmez | T27, T30 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | Eşik `AnswerService`'te, retrieval'dan sonra; `ContextRetriever` değişmez | Değerlendirme ölçümü (recall@5, MRR) eşikten etkilenmez | `ContextRetriever`'da: eval sonuçları değişir |
| 2 | Eşik ilk prompt parçasının `rerankScore`'una (= adayların en yüksek puanı) bakar; `score < min` → eşik altı, eşitlik geçer | Tek sayı, ölçülebilir | Parçaları tek tek elemek: daha az parça cevabı bozabilir, ölçümü zorlaşır |
| 3 | Eşik değeri M1'de: golden set'in en düşük ilk parça puanının 1,0 altı, 0,5'in katına aşağı yuvarlanır. `assistant.retrieval.min-rerank-score=${RETRIEVAL_MIN_RERANK_SCORE:<M1>}`; boş → eşik kapalı (`@Nullable Double`) | Eşik tek başına hiçbir golden soruyu kesmez; 2'lik sınır model oynaması için kalır | Golden'dan 2'sini kesen en yüksek eşik: rehber sorusunu bilerek feda eder |
| 4 | Kalıp anahtarı: `SuggestionTextNormalizer.normalize` → Türkçe harfler ASCII'ye (ç→c, ğ→g, ı→i, ö→o, ş→s, ü→u) → art arda aynı harfler teke. Listeler yüklenirken her ifade aynı anahtara çevrilir. Mesajın kelime dizisi listelerdeki ifadelerin (en fazla 4 kelime) art arda dizilişiyle tamamen kaplanıyorsa selamlaşmadır (soldan sağa dinamik programlama). Tür önceliği: teşekkür > veda > selam; "dostum", "kanka", "asistan" gibi hitap sözcükleri sadece başka bir ifadeyle birlikte sayılır | "selaaam", "tesekkurler", "Naber dostum, ne var ne yok" tanınır; soru içeren mesaj tanınmaz. Aynı anahtar iki tarafta, "teşekkür"deki çift k sorun olmaz | Noktalamaya göre bölmek: "selam nasılsın" kaçar. Sadece dil modeli: her selamda gecikme |
| 5 | İfade listeleri `src/main/resources/small-talk/{greeting,thanks,farewell,address}.txt` (UTF-8, satır başına bir ifade, `#` yorum); hazır metinler `assistant.small-talk.greeting-answer`, `thanks-answer`, `farewell-answer` (`\u` kaçışlı) | Liste uzun ve Türkçe; properties ASCII. Metinler `not-found-answer` gibi yapılandırmada | Java sabiti: içerik koda gömülür |
| 6 | Kalıp kontrolü 409'dan sonra, yeniden yazmadan önce; arama yapılmaz, `RETRIEVAL_UNAVAILABLE` oluşmaz | Teşekkürün yeniden yazılması (S6) yapısal olarak imkânsız; anında cevap | Arama yapıp metni sabitlemek: anlamsız 5 parça kaydedilir |
| 7 | **Etiket çağrısı** (`MessageLabeler`): sadece eşik altında; girdi müşterinin kendi mesajı (yeniden yazılmış sorgu değil), geçmiş ve parça yok. Mesajlar: `SystemMessage` = `prompts/message-label.st` (Türkçe), `UserMessage` = mesaj (talimattan ayrı). Aynı dil modeli ve ayarlar (`temperature 0`, `think false`). Toplam süre `assistant.small-talk.label-timeout=10s`; dolunca abonelik iptal | Kullanıcı kararı; mesaj veri olarak ayrı (prompt injection); modele cevap yazdırılmaz | Cevap modeline selam talimatı eklemek: model yine numara yazabilir |
| 8 | Etiket ayrıştırma: çıktı `strip()`, baştaki/sondaki tırnak ve noktalama atılır, Türkçe harfler ASCII'ye, büyük harfe (`Locale.ROOT`) çevrilir; tam olarak `SELAM`, `TESEKKUR`, `VEDA` ya da `DIGER` ise o etiket; değilse (boş, iki kelime, başka metin) `OTHER`. Exception ya da zaman aşımı → `OTHER` (`failed` true) | "TEŞEKKÜR", "Diğer." de kabul; belirsiz çıktı güvenli tarafa (bulamadım) düşer | İlk kelimeyi almak: "SELAM ama…" yanlış sınıflanır |
| 9 | Etiket sonucu tura yazılır: `message_label` (`GREETING`/`THANKS`/`FAREWELL`/`OTHER`; kalıp listesinde de dolu) ve `message_label_latency_ms` (sadece model sorulduysa). SSE'ye eklenmez; `done.latencyMs` süreyi kapsar | "Her adımın sonucu tura kaydedilir"; Önizleme görünümü değişmez | `done`'a yeni süre alanı: ekranda karşılığı yok |
| 10 | Eşik altındaki "bulamadım" turunda `sources.contextChunks` `[]`; Önizleme kartında liste yerine tek satır: "Bu turda modele hiçbir parça gitmedi: en iyi parçanın rerank puanı eşiğin altında kaldı." Konuşmalar'da mevcut "Bu turda modele hiçbir parça gitmedi." | Kayıt doğru kalır; golden sorusuysa tur `MISSED` (KAÇIRDI) | Seçilen 5 parçayı göstermek: "modele giden parçalar" başlığı yanlış olur |
| 11 | Selamlaşma turunda (iki yol) Önizleme'nin "Son cevapta modele giden parçalar" kartı gösterilmez | Cevap parçaya dayanmıyor | Boş kart: anlamsız |
| 12 | Akış filtresi: model metni normalize edilince (tr küçük harf, boşluklar teke, baştaki boşluk atılmış) cümlenin başı olduğu sürece tutulur; cümleyi tamamlayınca cümle yapılandırmadaki yazımıyla tek token gider, abonelik sürer ama sonrası gönderilmez; ayrışırsa tutulan metin gider ve geçiş moduna girilir | "Kullanıcı numarayı görmez" (K2) bir an bile; token sayıları turda kalır | Aboneliği kesmek: `completionTokens` null olur. Sadece sonda düzeltmek: `[5]` bir an görünür |
| 13 | `done.answer` + `done.answerSource`; web `done` gelince akan metni `answer` ile değiştirir; boş bilgi bankası notu sadece `EMPTY_KNOWLEDGE_BASE`'te | Cümleden önce metin yazıldığında (G3) ekranın kayıtla aynı olmasının tek yolu | Web'in kendi silmesi: cümleyi bilmez, kural iki yerde |
| 14 | Kaydedilen `RAG` model cevabından aralık dışı `[n]` (önündeki boşlukla) silinir, sonra `strip()` | K1: işaret parçaya dayanmalı; `[6]`'nın karşılığı yok | Bırakmak: ekranda düz metin `[6]` kalır |
| 15 | V8 migration; eski "bulamadım" turlarının `[5]`'leri geriye dönük silinmez | Geçmiş kayıt değiştirilmez | Geri doldurma: kayıt sonradan değişir |
| 16 | Akış ortasında hata/iptalde tura modelin o ana kadarki ham metni yazılır (tutulan metin dahil) | sse.md "o ana kadar üretilen metin" | Sadece gönderilen metin: tutulan kısım kaybolur |
| 17 | Selamlaşma turları `findAnsweredTurns`'a (geçmiş) girmez | Geçmiş 3 turluk; selam gerçek turu dışarı iter | Girmesi: yeniden yazma gereksiz çağrılır |
| 18 | Ölçüm betiği Python 3 standart kütüphanesiyle (`assistant-api/scripts/measure_answers.py`), yeni dependency yok | qa aynı betikle tekrar koşar | Elle curl: tekrarlanamaz |

## Sözleşme değişiklikleri
`docs/api/sse.md`:
- `done`: yeni `answer` (string) ve `answerSource` (`AnswerSource`); `notFoundAnswer` açıklaması.
- Yeni `AnswerSource`: `MODEL`, `EMPTY_KNOWLEDGE_BASE`, `BELOW_RERANK_THRESHOLD`, `SMALL_TALK` (kalıp listesi ya da eşik altı + etiket).
- "Sıra": eşik + etiket yolu, kalıp listesi yolu, "bulamadım" cümlesinin tutulması.
- `sources`: kalıp listesi turunda `retrievalStrategy` `NONE`, `candidates`/`topK`/`searchQuery` `null`, `queryRewriteStatus` `DISABLED`; etiketli selamlaşma ve eşik altı turunda strateji ve arama alanları dolu, `contextChunks` []; `citations` "bulamadım"da her zaman [].
- Örnekler: mevcut `done` örneklerine iki alan; yeni örnekler: numara eklenen bulamadım, eşik altı, kalıp listesi selamı, eşik altı + etiketli selam. "İsimler" tablosuna `AnswerSource`.

`docs/api/openapi.yaml`:
- Başlık yorumuna TASK-031.
- `sendMessage` açıklaması: kalıp listesi, eşik + etiket, "bulamadım" cevabının kaydı, kontrol sırası (409 → kalıp listesi → yeniden yazma → 503 → eşik/etiket → akış), yeniden yazmada selamlaşma turları sayılmaz.
- `TurnStatus`: kural 3a `SMALL_TALK` (SOHBET), kural 4'e eşik notu; enum'a `SMALL_TALK`.
- `TurnResponse`: `searchQuery`, `answer`, `candidates`, `topK`, `cited`, `contextChunks` açıklamaları. `RetrievalStrategy.NONE` açıklaması.
- Yeni endpoint ya da `errorCode` yok.

## Backend planı
Ek skill'ler: `rag-pipeline`, `sse-streaming`, `java-concurrency` (filtre durumu akış thread'inde, etiket çağrısı zaman aşımı), `spring-transactions`, `flyway-migrations`, `postgres-patterns`.

### Dependency
Yok.

### Migration `V8__add_answer_source_and_message_label_to_turns.sql`
- `answer_source text NOT NULL DEFAULT 'MODEL'` + `ck_conversation_turns_answer_source CHECK (answer_source IN ('MODEL','EMPTY_KNOWLEDGE_BASE','BELOW_RERANK_THRESHOLD','SMALL_TALK'))`.
- `message_label text NULL` + `ck_conversation_turns_message_label CHECK (message_label IN ('GREETING','THANKS','FAREWELL','OTHER'))`; `message_label_latency_ms integer NULL` + `CHECK (message_label_latency_ms >= 0)`; `ck_conversation_turns_label_latency_has_label CHECK (message_label_latency_ms IS NULL OR message_label IS NOT NULL)`.
- Geri doldurma: `generation_mode = 'RAG' AND error_code IS NULL AND not_found_answer AND NOT EXISTS (turn_chunks of the turn)` → `EMPTY_KNOWLEDGE_BASE`.
- `DROP CONSTRAINT ck_conversation_turns_mode_strategy`; yerine `ck_conversation_turns_llm_only_none CHECK (generation_mode <> 'LLM_ONLY' OR retrieval_strategy = 'NONE')` ve `ck_conversation_turns_none_reason CHECK (retrieval_strategy <> 'NONE' OR generation_mode = 'LLM_ONLY' OR answer_source = 'SMALL_TALK')`.
- `ck_conversation_turns_small_talk CHECK (answer_source <> 'SMALL_TALK' OR (generation_mode = 'RAG' AND NOT not_found_answer AND cardinality(cited) = 0 AND message_label IN ('GREETING','THANKS','FAREWELL')))`.
- `ck_conversation_turns_below_threshold CHECK (answer_source <> 'BELOW_RERANK_THRESHOLD' OR (retrieval_strategy = 'RERANK' AND not_found_answer))`.
- `ck_conversation_turns_llm_only_model CHECK (generation_mode <> 'LLM_ONLY' OR answer_source = 'MODEL')`.
- `ck_conversation_turns_none_has_no_retrieval` ve `…_retrieval_has_values` değişmez.

### Properties (`application.properties`, saf ASCII)
```
assistant.retrieval.min-rerank-score=${RETRIEVAL_MIN_RERANK_SCORE:<M1 değeri>}
assistant.small-talk.greeting-answer=<aşağıdaki metin, \u kaçışlı>
assistant.small-talk.thanks-answer=<…>
assistant.small-talk.farewell-answer=<…>
assistant.small-talk.label-timeout=10s
```
`AssistantProperties.Retrieval`'a `@Nullable Double minRerankScore`; yeni `@Valid @NotNull SmallTalk smallTalk` record'u `(@NotBlank String greetingAnswer, @NotBlank String thanksAnswer, @NotBlank String farewellAnswer, @NotNull Duration labelTimeout)` + `String answerFor(MessageLabel label)` (`OTHER` → `IllegalArgumentException`). Boş ortam değişkeninin `null`'a bağlandığını backend-coder doğrular (T5).

### Hazır metinler (kullanıcı onayladı)
- Selam: `Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılıkla ilgili sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplayabilirim. Ne sormak istersin?`
- Teşekkür: `Rica ederim. Kart, hesap, kredi, transfer ya da dijital bankacılıkla ilgili başka bir sorun olursa yazabilirsin.`
- Veda: `Görüşmek üzere. Kart, hesap, kredi, transfer ya da dijital bankacılıkla ilgili bir sorun olursa buradayım.`

### İfade listeleri (`small-talk/*.txt`; yazım serbest, yüklenirken anahtara çevrilir)
```
greeting.txt: selam, selamlar, merhaba, merhabalar, mrb, mrhb, slm, sa, selamün aleyküm, selamünaleyküm,
  selamın aleyküm, aleyküm selam, aleykümselam, hey, hi, hello, alo, günaydın, tünaydın, iyi günler,
  iyi akşamlar, hayırlı günler, hayırlı sabahlar, hayırlı akşamlar, nasılsın, nasılsınız, naber, nbr,
  ne haber, ne var ne yok, napıyorsun, naptın, iyi misin, iyi misiniz, nasıl gidiyor, kolay gelsin, hoş bulduk
thanks.txt: teşekkürler, teşekkür, teşekkür ederim, teşekkür ederiz, çok teşekkürler, çok teşekkür ederim,
  tşk, tşkler, tşkrler, sağ ol, sağol, saol, sağ olun, sağolun, sağ olasın, sağolasın, çok sağ ol,
  eyvallah, eyv, eyw, mersi, merci, eline sağlık, elinize sağlık, thanks, thank you, thx
farewell.txt: görüşürüz, görüşmek üzere, hoşça kal, hoşçakal, hoşça kalın, güle güle, bay bay, bye, bb,
  iyi geceler, iyi çalışmalar, kendine iyi bak, kendinize iyi bakın, allah'a emanet
address.txt (tek başına sayılmaz): dostum, kanka, asistan, efendim, hocam, abi, abla, arkadaşım
```
"Selamlar herkese", "Keyifler nasıl?", "Çok makbule geçti", "Görüşmek dileğiyle", "Merhabalar efendim, hayırlı
işler" bilerek listede yoktur (bu ifadeler listeye eklenmez); etiket yolunu ölçmek için kullanılır (Ölçüm). M1'de bu mesajların puanı eşiğin üstündeyse backend-coder raporlar
(listeye eklenmez; liste değişikliği kullanıcı kararıdır).

### Sistem talimatı `prompts/rag-system.st` (yeni metin; kural 4 düzeltme turu 1'de eklendi, aşağıda)
```
Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın. Kendinden birinci tekil şahısla söz edersin, örneğin "asistanıyım", "yardımcı olabilirim".
1. Sadece <parcalar> içindeki bilgiyle cevap ver; kendi bilgini ekleme.
2. Her cümlenin sonuna dayandığı parçanın numarasını yaz, örneğin [1]. Hiçbir parçaya dayanmayan cümle yazma.
3. Cevap parçalarda yoksa sadece "{notFoundAnswer}" yaz; bu cümleye numara ya da başka bir şey ekleme.
```
`rag-user.st`, `llm-only-system.st`, `query-rewrite.st` değişmez.

### Etiket talimatı `prompts/message-label.st` (yeni, sistem mesajı; müşteri mesajı ayrı kullanıcı mesajıdır; örnekler düzeltme turu 1'de eklendi, aşağıda)
```
Bir bankanın dijital asistanına gelen müşteri mesajını sınıflandırıyorsun. Mesaja cevap yazma.
SELAM: selamlaşma ya da hal hatır sorma.
TESEKKUR: teşekkür etme.
VEDA: vedalaşma.
DIGER: soru, istek, şikâyet, anlamsız metin ve diğer her şey.
Sadece tek kelime yaz: SELAM, TESEKKUR, VEDA ya da DIGER.
```

### Sınıflar (kök `com.rasitbank.assistant`)
**generation/**
- `MessageLabel` (enum): `GREETING`, `THANKS`, `FAREWELL`, `OTHER`; `boolean isSmallTalk()` (`OTHER` dışı).
- `SmallTalkDetector` (`@Component`; listeleri constructor'da `classpath:small-talk/*.txt`'ten okur): `Optional<MessageLabel> detect(String message)` (Kararlar #4; hiçbir zaman `OTHER` dönmez). Boş anahtar → boş. `static String phraseKey(String text)` (normalize + ASCII + tekrar harf).
- `MessageLabelResult` (record): `(MessageLabel label, long latencyMs, boolean failed)`.
- `MessageLabeler` (`@Service`; `ChatModel`, `classpath:prompts/message-label.st`, `AssistantProperties`): `MessageLabelResult labelMessage(String message)` (Kararlar #7, #8; `QueryRewriter.callModel` kalıbı: `stream` → birleştir → `timeout(labelTimeout)` → `block`). `static MessageLabel parseLabel(String output)`.
- `NotFoundAnswerStreamFilter` (bean değil, tur başına; `NotFoundAnswerDetector.newStreamFilter()` üretir): `String accept(String token)` (o an gönderilecek metin, `""` olabilir), `String finish()` (tutulan metin), `boolean isNotFoundAnswer()`. Durumlar `HOLDING` → `PASS` | `MATCHED` (Kararlar #12). Thread-safe olması gerekmez; `StreamingTurn` senkronize çağırır.
- `NotFoundAnswerDetector`: `isNotFoundAnswer` değişmez; `newStreamFilter()` ve `String notFoundAnswer()` eklenir.
- `CitationParser`: `String removeOutOfRangeCitations(String answer, int contextSize)` eklenir (`\s*\[(\d{1,9})]`, aralık dışıysa silinir).
- `FinalAnswer` (record): `(String answer, boolean notFoundAnswer, List<Integer> citations)`.
- `AnswerFinalizer` (`@Component`): `FinalAnswer finalizeRagAnswer(String modelText, int contextSize)` → detector "içeriyor" derse `(notFoundAnswer, true, [])`; değilse `(removeOutOfRangeCitations(modelText, contextSize).strip(), false, parseCitations(modelText, contextSize))`.
- `PromptBuilder`: değişmez (yeni şablon metnini okur).

**retrieval/service/**
- `RerankScoreGate` (`@Component`, `AssistantProperties`): `boolean isBelowThreshold(RetrievedContext context)` → `strategy == RERANK && minRerankScore != null && !contextChunks.isEmpty() && contextChunks.get(0).rerankScore() < minRerankScore`.
- `RetrievedContext`: `RetrievedContext withoutContextChunks()` (adaylar ve süreler aynı, `contextChunks` boş).

**conversation/**
- `domain/entity/AnswerSource` (enum, `@Enumerated(STRING)`); `Turn` alanları `answerSource`, `messageLabel` (`@Nullable MessageLabel`, `@Enumerated(STRING)`), `messageLabelLatencyMs` (`@Nullable Integer`) + getter'lar; `TurnValues`'a aynı üç alan.
- `service/TurnDraft`: `@Nullable MessageLabel messageLabel`, `@Nullable Long messageLabelLatencyMs` (son iki alan).
- `domain/event/DoneEvent`: `notFoundAnswer`'dan sonra `String answer`, `AnswerSource answerSource` (sse.md alan sırası).
- `service/TurnOutcome`: `AnswerSource answerSource` (son alan); `failed(...)` → `MODEL`.
- `service/AnswerService.answerWithRetrieval` sırası:
  1. `smallTalkDetector.detect(question)` doluysa: draft (`searchQuery` null, `DISABLED`, context null, `messageLabel` = sonuç, süre null) → `answerStreamer.streamFixedAnswer(draft, smallTalk.answerFor(label), AnswerSource.SMALL_TALK, emitter)`.
  2. Yeniden yazma + retrieval (mevcut; 503 burada).
  3. `contextChunks` boş → `EMPTY_KNOWLEDGE_BASE` (mevcut sabit cevap).
  4. `rerankScoreGate.isBelowThreshold(context)` → `messageLabeler.labelMessage(question)`; draft `context.withoutContextChunks()` + etiket + süre ile. Etiket selamlaşmaysa `streamFixedAnswer(…, answerFor(label), SMALL_TALK, …)`, değilse `streamFixedAnswer(…, notFoundAnswer, BELOW_RERANK_THRESHOLD, …)`.
  5. Aksi halde mevcut model yolu.
  `answerWithoutRetrieval` (LLM_ONLY) değişmez; orada kalıp listesi ve etiket yok.
- `service/AnswerStreamer`: `streamFixedAnswer(TurnDraft, String answer, AnswerSource, SseEmitter)`. `StreamingTurn` RAG'de context'i `null` kabul eder (kalıp listesi yolu). RAG model yolunda filtre: ham metin `rawAnswer`'a, `filter.accept` çıktısı boş değilse `token`; `onModelCompleted`'da `filter.finish()` gönderilir, `answerFinalizer.finalizeRagAnswer(rawAnswer, contextSize)` → `TurnOutcome` (`MODEL`). `sourcesEvent` context null + RAG → `RAG, NONE, null, null, null, DISABLED, [], []`. `doneEvent` `outcome.answer()` (null ise `""`) ve `outcome.answerSource()`. `interruptedOutcome` ham metni yazar (Kararlar #16).
- `service/TurnService`: retrieval alanları `rag && draft.searchQuery() != null` ise yazılır; kalıp listesi turunda strateji `NONE`, `candidates`/`topK` `null`. `answerSource`, `messageLabel`, `messageLabelLatencyMs` yazılır. Log satırına `answerSource={} messageLabel={}`.
- `service/TurnStatusResolver`: `LLM_ONLY` kontrolünden sonra `answerSource == SMALL_TALK` → `TurnStatus.SMALL_TALK`. Eşik turu mevcut "bulamadım" kuralına düşer (prompt'ta parça yok → cevap parçası varsa `MISSED`).
- `domain/entity/TurnStatus`: `SMALL_TALK` (`UNGROUNDED`'dan sonra, openapi sırası).
- `repository/TurnRepository.findAnsweredTurns`: `and t.answerSource <> SMALL_TALK` (parametre ya da enum literal).

### Loglar (soru, cevap, sorgu, etiket çıktısının metni yok)
- `INFO Small talk answered conversationId={} label={} route={}` (`route` `PATTERN` ya da `LABEL`)
- `INFO Answer stopped by rerank threshold conversationId={} topRerankScore={} minRerankScore={}`
- `INFO Message labeled conversationId={} label={} durationMs={} failed={}`; hata/zaman aşımında `WARN Message label failed, treating as other reason={} durationMs={}`
- `INFO Not found answer normalized conversationId={} removedLength={}` (finalizer metni kısalttıysa)
- `Turn completed …` satırına `answerSource={} messageLabel={}`.

### Ölçüm betiği `assistant-api/scripts/measure_answers.py`
- Python 3, sadece standart kütüphane. Argümanlar: `--base-url` (varsayılan `http://localhost:8090`), `--golden` (`docs/rehber/golden/rehber-golden-v1.json`), `--negatives` (`assistant-api/scripts/negative-messages.txt`), `--out` (JSON).
- Her mesaj için yeni konuşma (`POST /api/assistant/conversations` `{"channel":"PREVIEW"}`), mesaj, SSE ayrıştırma. Takip satırı (`A >> B`) aynı konuşmada iki mesajdır; sadece B değerlendirilir.
- Satır başına: `set` (`golden`/`negative`), `category`, `text`, `answerSource` (eski kodda yoksa `null`), `route` (`sources.retrievalStrategy` `NONE` + `SMALL_TALK` → `PATTERN`; strateji dolu + `SMALL_TALK` → `LABEL`; diğer → `null`), `notFoundAnswer`, `citations` sayısı, `citedContainsAnswer`, `topRerankScore` (`contextChunks` en büyük `rerankScore`; boşsa `null`), `answer` (`done.answer`, yoksa birleşik token'lar), `secondPersonSelfReference` (`asistanı(sınız|sın)\b`, tr küçük harf).
- Özet: golden için cevaplı / bulamadım / alıntılı / doğru alıntılı; `topRerankScore` en küçük ve medyan. Negatif için kategori başına alıntılı, bulamadım, `SMALL_TALK` (route'a göre) sayıları ve `topRerankScore` en büyük. `secondPersonSelfReference` toplamı.
- Etiket yolunun puan bilgisi M2'de görünmez (eşik altında `contextChunks` boş); puanlar M0'dan okunur, `turn_chunks.rerank_score`'ta da kalır.
- Önkoşul: rehber 2026.10 `DONE`, golden set `PUT` edilmiş, `RETRIEVAL_STRATEGY=rerank`, `QUERY_REWRITE_ENABLED=true`.

### Negatif mesaj listesi `assistant-api/scripts/negative-messages.txt` (`kategori | metin`; 55 mesaj)
- **Rehberde olmayan (27):** `docs/rehber/rehberde-olmayan-konular.md`'deki 9 konunun 3'er örnek sorusu.
- **Selamlaşma, kalıp listesinde (13):** Selam · Merhaba · Günaydın · Teşekkürler · Sağ ol · Görüşürüz · İyi günler · selamün aleyküm · eyv · Naber dostum, ne var ne yok · selaaam · tesekkurler · Teşekkürler, iyi günler
- **Selamlaşma, listede yok (5):** Selamlar herkese · Keyifler nasıl? · Çok makbule geçti · Görüşmek dileğiyle · Merhabalar efendim, hayırlı işler
- **Konu dışı (5):** Bugün hava nasıl? · Bana bir şiir yaz · En sevdiğin film ne? · 2 artı 2 kaç eder? · İstanbul'un nüfusu kaç?
- **Anlamsız (4):** asdfgh · qwerty 123 · ??? · lorem ipsum dolor
- **Takip (1):** `Gold kart aidatı ne kadar? >> Teşekkürler`

### Ölçüm adımları
- **M0 (bugünkü kod, değişiklikten önce):** betik + liste yazılır, bugünkü `main` ile koşulur → `handoff/measure/m0-baseline.json`.
- **M1:** eşik = golden `topRerankScore` en küçüğü − 1,0, 0,5'in katına aşağı yuvarlanır → properties varsayılanı. Raporda: golden en küçük/medyan, negatif kategorilerinin en büyükleri, eşiğin altında kalan negatif sayısı; "Selamlaşma, listede yok" mesajlarının her birinin puanı ve eşiğin altında olup olmadığı.
- **M2 (yeni kod):** aynı betik → `handoff/measure/m2-after.json`; ayrıca `RETRIEVAL_MIN_RERANK_SCORE=` (eşik kapalı) ile sadece golden → `m2-no-threshold.json` (talimat etkisini ayırmak için).
- **Kabul (M2 ve M3; düzeltme turu 1'de M4 ve M3 için değişti, aşağıda):**
  - Bütün negatif listede `citations > 0` olan cevap 0.
  - 18 selamlaşma mesajının hepsi `SMALL_TALK` (13'ü `PATTERN`; "listede yok" 5'i `LABEL` beklenir; puanı eşiği geçtiği için `LABEL`'a ulaşamayan varsa raporda ayrıca yazılır ve kabul dışı sayılır).
  - "Bitcoin alıp satabilir miyim?", "Bugün hava nasıl?", "Bana bir şiir yaz", "asdfgh" → `notFoundAnswer` true. Diğer negatiflerde bulamadım sayısı raporda.
  - Bulamadım'a düşen golden (M0'da `notFoundAnswer` false, şimdi true) ≤ 2. Golden'da doğru alıntılı sayısının M0'a göre değişimi raporda.
  - `secondPersonSelfReference` 0.
- **M3 (qa):** Docker'da aynı betik bağımsız koşu → `handoff/measure/m3-qa.json`.

### `assistant-api/CLAUDE.md` (backend-coder)
Durum yorumuna TASK-031; `assistant.retrieval.min-rerank-score`, `assistant.small-talk.*`; V8; yeni sınıflar ve `prompts/message-label.st`; ölçüm betiği komutu ("Komutlar").

## Frontend planı
Ek skill'ler: `sse-streaming`, `react-testing`, `frontend-a11y`.
- `features/preview/model/types.ts`: `ANSWER_SOURCES = ['MODEL', 'EMPTY_KNOWLEDGE_BASE', 'BELOW_RERANK_THRESHOLD', 'SMALL_TALK'] as const`, `AnswerSource`; `DoneEvent`'e `answer: string`, `answerSource: AnswerSource`.
- `features/preview/model/answerStreamEvents.ts`: `done` daraltması `answer` (string) ve `answerSource` (listede) ister; yoksa olay atlanır.
- `features/preview/hooks/useAnswerStream.ts` `applyEvent` `done`: `answer: event.data.answer`.
- `features/preview/components/AnswerMessage.tsx`: boş bilgi bankası notu `done.answerSource === 'EMPTY_KNOWLEDGE_BASE'` iken.
- `features/preview/pages/PreviewPage.tsx`: `ContextChunksCard` koşuluna `lastTurn.done.answerSource !== 'SMALL_TALK'`.
- `features/preview/components/ContextChunksCard.tsx`: `done.answerSource === 'BELOW_RERANK_THRESHOLD'` ise liste ve açıklama satırı yerine `card.sub` stilinde sabit `BELOW_THRESHOLD_NOTE = 'Bu turda modele hiçbir parça gitmedi: en iyi parçanın rerank puanı eşiğin altında kaldı.'`; `LatencyBars` kalır.
- `features/conversation/model/types.ts`: `TURN_STATUSES`'a `'SMALL_TALK'` (`UNGROUNDED`'dan sonra); `turnStatus.ts`: etiket `SOHBET`, ton `neutral`.
- Yeni paket yok. Yeni `errorCode` yok. `web/CLAUDE.md` güncellenir (done alanları, SOHBET).

## Test senaryoları
Sahte `ChatModel` sabit token dizisi (cevap ve etiket çağrısı ayrı sahte yanıtlar); sahte `RetrievalClient`/`RerankClient` fixture parçaları ve puanları. Gerçek modele gidilmez. Etiket çağrısı deterministik testlerde sadece ayrıştırma, eşleme ve hata yoluyla doğrulanır; modelin gerçekten doğru etiketi verip vermediği M2/M3 ölçümünün konusudur.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T1 | Selam kalıpları | unit `SmallTalkDetectorTest` | `givenGreetingSpellingsCasePunctuationAndEmoji_whenDetect_thenGreeting` | "Selam", "SELAM!!", "selaaam", "Merhaba, nasılsın?", "İyi günler 🙂", "selamün aleyküm", "Naber dostum, ne var ne yok", "slm" → `GREETING` |
| T2 | Tür önceliği ve ASCII | unit | `givenThanksAndFarewellPhrases_whenDetect_thenThanksBeatsFarewellBeatsGreeting` | "Teşekkürler, görüşürüz" → `THANKS`; "Merhaba, görüşürüz" → `FAREWELL`; "tesekkurler", "saol", "eyv", "Teşekkür ederim." → `THANKS` |
| T3 | Soru içeren ya da tek hitap | unit | `givenQuestionUnknownTextOrAddressWordAlone_whenDetect_thenEmpty` | "Merhaba, kartımı kaybettim", "Selam kart aidatı ne kadar", "Bitcoin alıp satabilir miyim?", "asdfgh", "dostum", "   " → boş |
| T4 | Eşik sınırı | unit `RerankScoreGateTest` | `givenTopRerankScoreBelowEqualOrAboveThreshold_whenIsBelowThreshold_thenOnlyBelowIsTrue` | min −2,0: ilk puan −2,5 → `true`; −2,0 → `false`; −1,9 → `false` |
| T5 | Eşik uygulanmayan durumlar | unit + binding | `givenTopKFallbackEmptyContextOrNoThreshold_whenIsBelowThreshold_thenFalse` | `TOP_K`, `RERANK_FALLBACK`, `contextChunks` boş, `minRerankScore` null → `false`; `RETRIEVAL_MIN_RERANK_SCORE=` ile bağlanan değer `null` |
| T6 | Cümle + numara | unit `NotFoundAnswerStreamFilterTest` | `givenSentenceThenCitationTokens_whenAccept_thenEmitsSentenceOnceAndSuppressesRest` | ["Bu konuda ", "rehberimizde bilgi ", "bulamadım.", " [5]"] → "", "", "Bu konuda rehberimizde bilgi bulamadım.", ""; `isNotFoundAnswer` true |
| T7 | "Bu" ile başlayan cevap | unit | `givenAnswerStartingWithBu_whenAccept_thenFlushesHeldTextOnDivergence` | ["Bu ", "kartın yıllık ", "aidatı 750 TL'dir [1]."] → "", "Bu kartın yıllık ", "aidatı 750 TL'dir [1]."; false |
| T8 | Yarım önek | unit | `givenModelStopsMidPrefix_whenFinish_thenReturnsHeldText` | ["Bu konuda"] → accept "", finish "Bu konuda"; false |
| T9 | Büyük harf, fazla boşluk | unit | `givenUpperCaseAndExtraSpaces_whenAccept_thenEmitsConfiguredSentence` | [" BU  konuda rehberimizde bilgi bulamadım. [2]"] → "Bu konuda rehberimizde bilgi bulamadım." |
| T10 | Cümleden önce metin | unit `AnswerFinalizerTest` | `givenTextBeforeNotFoundSentence_whenFinalize_thenOnlySentenceWithoutCitations` | "Maalesef, bu konuda rehberimizde bilgi bulamadım. [2]", 5 parça → cümle, `notFoundAnswer` true, citations [] |
| T11 | Aralık dışı işaret | unit | `givenOutOfRangeMarker_whenFinalize_thenRemovedAndValidCitationKept` | "Kartını kapat [1]. Yenisi gelir [6]." → "Kartını kapat [1]. Yenisi gelir.", citations [1] |
| T12 | Cevap talimatı | unit `PromptBuilderTest` | `givenRagPrompt_whenBuild_thenSystemMessageAsksFirstPersonAndNoNumberAfterNotFound` | Sistem mesajı "Kendinden birinci tekil şahısla söz edersin", "Hiçbir parçaya dayanmayan cümle yazma", "bu cümleye numara ya da başka bir şey ekleme" ve yapılandırılmış cümleyi içerir |
| T13 | Kalıp listesi yolu | unit `AnswerServiceTest` | `givenGreeting_whenSendMessage_thenFixedReplyWithoutRewriteRetrievalLabelOrModel` | `QueryRewriter`, `ContextRetriever`, `MessageLabeler`, `ChatModel` etkileşimsiz; `streamFixedAnswer` selam metni + `SMALL_TALK`; draft `messageLabel` GREETING |
| T14 | retrieval-api kapalı + teşekkür | unit | `givenRetrievalDownAndThanks_whenSendMessage_thenStreamsThanksReplyNot503` | Exception yok; teşekkür metni |
| T15 | Eşik altı + DIGER | integration `SendMessageSseIntegrationTest` (MockMvcTester, Testcontainers) | `givenTopRerankScoreBelowThresholdAndLabelOther_whenSendMessage_thenNotFoundWithoutAnswerModelCall` | Sahte etiket "DIGER". Olaylar: token cümle; sources `RERANK`, `contextChunks` [], `citations` []; done `notFoundAnswer` true, `answerSource` `BELOW_RERANK_THRESHOLD`, `generationLatencyMs` null; cevap modeli çağrılmadı (model 1 kez, etiket için); `turn_chunks` 20 satır, `prompt_position` hepsi NULL; `message_label` OTHER, `message_label_latency_ms` dolu |
| T16 | Model numara ekler | integration | `givenModelWritesNotFoundWithCitation_whenSendMessage_thenSingleSentenceTokenAndNoCitation` | Puan eşiğin üstünde; model "Bu konuda ", "rehberimizde bilgi bulamadım.", " [5]" → tek token tam cümle, `[5]` gitmez; `citations` []; `done.answer` cümle; DB `answer` cümle, `cited` `{}`, `not_found_answer` true, `completion_tokens` dolu; `MessageLabeler` çağrılmadı |
| T17 | Normal cevap bozulmaz | integration | `givenGroundedModelAnswer_whenSendMessage_thenTokensCitationAndAnswerUnchanged` | "Kartını hemen ", "kapat [1]." → aynı iki token; `citations` [1]; `done.answer` "Kartını hemen kapat [1]."; `answerSource` `MODEL` |
| T18 | Gold → Teşekkürler | integration | `givenAnsweredTurn_whenThanksSent_thenSmallTalkTurnWithoutRewriteOrCitation` | İkinci tur `answer_source` SMALL_TALK, `retrieval_strategy` NONE, `search_query` NULL, `cited` `{}`; sahte model sadece ilk tur için 1 kez çağrıldı |
| T19 | Geçmiş | integration | `givenSmallTalkBetweenTurns_whenNextQuestion_thenHistoryExcludesSmallTalk` | Gold (cevaplı), Teşekkürler, "Peki ek kart?" → yeniden yazma prompt'unun geçmişinde tek tur (Gold); `history_turns` 1 |
| T20 | Durum | unit `TurnStatusResolverTest` | `givenSmallTalkTurn_whenResolve_thenSmallTalk` / `givenBelowThresholdTurnWithAnswerChunks_whenResolve_thenMissed` | Kalıp ve etiket yolundaki iki selam turu → `SMALL_TALK`; cevap parçası var, prompt'ta parça yok → `MISSED` |
| T21 | Migration | integration (Flyway, Testcontainers) | `givenV7Turns_whenV8Migrated_thenAnswerSourceBackfilledAndConstraintsHold` | Parçasız RAG bulamadım → `EMPTY_KNOWLEDGE_BASE`, diğerleri `MODEL`; `RAG`+`NONE`+`MODEL` insert reddedilir; `RAG`+`NONE`+`SMALL_TALK`+`GREETING` kabul; `RAG`+`RERANK`+`SMALL_TALK`+`THANKS` kabul; `SMALL_TALK`+`OTHER` reddedilir; `BELOW_RERANK_THRESHOLD`+`TOP_K` reddedilir; `message_label_latency_ms` dolu + `message_label` NULL reddedilir |
| T22 | Konuşmalar detay | controller | `givenSmallTalkTurn_whenGetConversationDetail_thenStatusSmallTalkAndNoChunks` | `status` SMALL_TALK, `searchQuery` null, `contextChunks` [], `retrievalStrategy` NONE |
| T23 | Boş bilgi bankası | integration | `givenEmptyKnowledgeBase_whenSendMessage_thenAnswerSourceEmptyKnowledgeBase` | `done.answerSource` `EMPTY_KNOWLEDGE_BASE`, `notFoundAnswer` true; `MessageLabeler` çağrılmadı |
| T24 | Sadece LLM değişmez | unit | `givenLlmOnlyModeAndGreeting_whenSendMessage_thenModelCalledAndNoSmallTalkRoute` | Model çağrıldı; `SmallTalkDetector` ve `MessageLabeler` etkileşimsiz; `answerSource` `MODEL` |
| T25 | Log sınırı | integration (`OutputCaptureExtension`) | `givenSmallTalkAndThresholdTurns_whenLogsCaptured_thenNoMessageOrAnswerText` | `Small talk answered … label=GREETING route=PATTERN`, `Answer stopped by rerank threshold`, `Message labeled … label=OTHER`; "Selam", "şiir", hazır metin ve etiket çıktısı logda yok |
| T26 | Etiket ayrıştırma | unit `MessageLabelerTest` (parametreli) | `givenModelOutputs_whenParseLabel_thenMapsExactLabelsElseOther` | "SELAM" → GREETING; " tesekkur." → THANKS; "TEŞEKKÜR" → THANKS; "\"Veda\"" → FAREWELL; "DİĞER" → OTHER; "Merhaba!" → OTHER; "SELAM VEDA" → OTHER; "" → OTHER |
| T27 | Etiket hatası | unit | `givenModelErrorOrTimeout_whenLabelMessage_thenOtherAndFailed` | Sahte model exception → `OTHER`, `failed` true; 10 ms zaman aşımı ayarı + hiç bitmeyen `Flux` → `OTHER`, `failed` true, abonelik iptal edildi |
| T28 | Etiket prompt'u | unit | `givenMessage_whenLabelMessage_thenSystemIsLabelInstructionAndUserIsMessageOnly` | Yakalanan prompt: 2 mesaj; sistem `message-label.st` metni (Türkçe, "Mesaja cevap yazma"); kullanıcı mesajı tam olarak müşteri mesajı; `<parcalar>` ve geçmiş yok |
| T29 | Eşik altı + selam etiketi | integration | `givenBelowThresholdAndLabelGreeting_whenSendMessage_thenGreetingReplyWithRetrievalRecorded` | Sahte etiket "SELAM": token selam metni; sources `RERANK`, `searchQuery` dolu, `contextChunks` []; done `answerSource` `SMALL_TALK`, `notFoundAnswer` false; DB `message_label` GREETING, `retrieval_strategy` RERANK, `turn_chunks` 20, prompt_position NULL |
| T30 | Etiket sadece eşik altında ve RERANK'te | unit `AnswerServiceTest` | `givenTopKFallbackOrAboveThreshold_whenSendMessage_thenLabelerNotCalled` | `TOP_K`, `RERANK_FALLBACK`, eşik üstü → `MessageLabeler` etkileşimsiz; eşik altı + model kapalı (etiket `OTHER`, failed) → bulamadım, hata yok |
| W1 | Bulamadım + numara | component `PreviewPage.test.tsx` (MSW SSE) | `givenNotFoundDoneWithCleanAnswer_whenStreamFinishes_thenShowsSentenceWithoutCitationButtons` | Token "Maalesef, bu konuda rehberimizde bilgi bulamadım. [2]", done `answer` cümle → ekranda sadece cümle; `[2]` düğmesi ve "Rehber · s." yok; "Temsilciye bağlan" var |
| W2 | Selamlaşma | component | `givenSmallTalkDone_whenFinished_thenNoSourceButtonsEmptyKbNoteOrContextCard` | "Bilgi bankasında henüz belge yok." yok; "Son cevapta modele giden parçalar" yok; "Temsilciye bağlan" yok (kalıp ve etiket yolu fixture'larıyla) |
| W3 | Eşik altı | component `ContextChunksCard.test.tsx` | `givenBelowThresholdDone_whenRendered_thenShowsThresholdNoteInsteadOfChunks` | Not metni görünür, parça düğmesi yok; axe ihlali yok |
| W4 | Boş bilgi bankası | component | `givenEmptyKnowledgeBaseDone_whenFinished_thenShowsEmptyKbNote` | Not ve "Bilgi kaynakları'ndan belge ekle." bağlantısı var |
| W5 | Olay daraltma | unit `answerStreamEvents.test.ts` | `givenDoneWithoutAnswerOrUnknownSource_whenParsed_thenSkipped` | `answer` eksik ya da `answerSource` `OTHER` → atlanır; geçerli olay daraltılır |
| W6 | SOHBET | component `TurnCard.test.tsx`, `ConversationTable.test.tsx` | `givenSmallTalkTurn_whenRendered_thenStatusChipSohbet` | "SOHBET" çipi, nötr ton |
| E1 | Selam uçtan uca | e2e `acceptance.spec.ts` (gerçek sistem) | `givenGreeting_whenSent_thenFixedReplyWithoutCitation` | Cevap selam metniyle aynı, `[n]` ve kaynak düğmesi yok |
| E2 | Bitcoin uçtan uca | e2e | `givenBitcoinQuestion_whenSent_thenOnlyNotFoundSentence` | Balon metni tam olarak cümle; alıntı düğmesi yok |
| M0-M3 | Gerçek modellerle ölçüm | backend-coder, qa | Backend planı "Ölçüm adımları" | "Kabul" maddeleri |

## Deployment planı
Yeni servis, port, model yok. Yeni isteğe bağlı ortam değişkenleri: `RETRIEVAL_MIN_RERANK_SCORE` (boş → eşik kapalı), `ASSISTANT_SMALLTALK_LABELTIMEOUT`. Compose dosyası değişmez.

## Açık sorular ve riskler
- Eşik golden set'e göre seçildiği için eşiği geçen konu dışı mesajlarda talimata ve denetime güvenilir; model "bulamadım" demeden uydurma alıntılı cevap yazarsa yakalanmaz. M2'de çıkarsa backend-coder raporlar; eşiği yükseltmek kullanıcı kararıdır.
- Listede olmayan bir selam varyantının puanı eşiği geçerse etiket sorulmaz, cevap modeline gider. M1 bunu ölçer; çıkarsa rapor edilir, kullanıcıya sorulur (listeye ekleme ya da eşik).
- Listede olmayan bir teşekkür takip sorusu olarak gelirse ("Gold kart …" → "Çok makbule geçti") yeniden yazılan sorgu rehbere benzeyip eşiği geçebilir. Ölçüm listesinde bu kombinasyon yok; qa elle bir kez dener ve raporlar.
- Eşik altındaki her mesaj etiket için bir model çağrısı yapar (~1-2 sn ek süre).
- Cevap "bulamadım" cümlesiyle başlıyorsa cümle tamamlanana kadar (~1 sn) ekrana bir şey yazılmaz.

## Düzeltme turu 1
**Sebep:** M2'de (eşik −4,0) golden 100/100 cevaplı (düşüş 0), negatif listede alıntılı cevap 7 (M0'da 32), selam 17/18.
Alıntılı 7 negatif cevabın 2'si "bulamadım"ın başka kelimelerle yazılıp numara eklenmesi (a türü), 5'i rehberde
olmayan soruda yakın rehber bilgisinin doğru alıntıyla verilmesi (b türü). "Çok makbule geçti" etiket yolunda DIGER
aldı. Kullanıcı kararları lead aracılığıyla (2026-11-03): a türüne denetim genişletmesi, b türüne hedefli talimat,
etiket talimatına örnekler. Etkilenen roller: backend-coder, tester, qa. **frontend-coder etkilenmez** (SSE alanları
ve ekran değişmez).

| # | Madde | Plandaki ya da sözleşmedeki değişiklik |
|---|-------|----------------------------------------|
| D1 | a türü: cevapta "rehberimizde … bulamadım" geçerse sabit cümleye çevrilir, alıntılar atılır | `NotFoundAnswerDetector.isNotFoundAnswer(String)` genişler: normalize metin (tr küçük harf, boşluklar teke) sabit cümleyi içeriyorsa **ya da** `PARAPHRASED_NOT_FOUND = rehberimizde[^.!?\n]*?bulamad[ıi]m` kalıbı bulunuyorsa `true`. `AnswerFinalizer` değişmeden bu sonucu kullanır (cevap = sabit cümle, `citations` []). `NotFoundAnswerStreamFilter` değişmez (sadece sabit cümlenin başını tutar); bu yüzden a türünde metin akarken numara görünür, `done` gelince web sabit cümleyi gösterir. sse.md `done.notFoundAnswer` ve "Sıra", openapi `sendMessage` güncellendi. **Golden kontrolü (M2 kayıtları):** `m2-after.json`'daki 100 golden cevabın hiçbirinde "rehberimizde" ya da "bulamad" geçmiyor (planner, `grep` ile; ilk eşleşme negatif kayıtlarda). M4'te betik aynı kontrolü yapar (`goldenParaphrasedNotFound` = 0) |
| D2 | b türü: kişisel hesap bilgisi, başka bankalar, yatırım tavsiyesi sorularında önce "göremiyorum / veremiyorum", sonra varsa alıntılı genel bilgi | `prompts/rag-system.st`'ye kural 4 (metin aşağıda). Üç sabit giriş cümlesi talimatta birebir yazılır; backend bunları denetlemez, sadece ölçüm betiği kontrol eder |
| D3 | Etiket talimatına farklı selam/teşekkür/veda örnekleri; "Çok makbule geçti" ve ölçümdeki listede olmayan 5 selam örnek olamaz | `prompts/message-label.st`'ye örnek satırları (metin aşağıda); yeni test T36 |
| D4 | Yeni ölçülebilir kabul kriteri (b grupları) | "Yeni kabul (M4 ve M3)" aşağıda; negatif listede 15 mesaj üç yeni kategoriye ayrılır |
| D4b | Hedefli talimatın golden'a etkisine sınır (kullanıcı kararı) | "Yeni kabul": `goldenStartsWithOpener` ≤ 2 (betik otomatik); kartta kabul kriteri ve risk güncellendi |
| D5 | Giriş cümlesine eklenen numaranın silinmesi (kullanıcı kararı, kartı değiştirmez; backend-coder duman testinde "… göremiyorum. [1]" buldu) | Aşağıda "D5: giriş cümlesi alıntılarının silinmesi"; sse.md `done.answer`, openapi `sendMessage`; testler T37-T39; ölçüm M4b |
| D6 | Yeniden ölçüm: golden 100 + 55 negatif | M4 = M2'nin tekrarı, aynı eşik (−4,0), D5'ten önceki kodla → `handoff/measure/m4-after-fix.json`; D5'ten sonra üç gruptaki 15 mesaj yeniden → `m4b-targeted.json` (M4b); qa'nın M3'ü D5'li kodla, bütün listeyle |

### `prompts/rag-system.st` (düzeltme turu 1 sonrası tam metin; D5 ile üç giriş cümlesi şablonda `{personalInfoOpener}`, `{otherBanksOpener}`, `{investmentAdviceOpener}` olur, değerleri aşağıdaki cümlelerdir)
```
Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın. Kendinden birinci tekil şahısla söz edersin, örneğin "asistanıyım", "yardımcı olabilirim".
1. Sadece <parcalar> içindeki bilgiyle cevap ver; kendi bilgini ekleme.
2. Her cümlenin sonuna dayandığı parçanın numarasını yaz, örneğin [1]. Hiçbir parçaya dayanmayan cümle yazma.
3. Cevap parçalarda yoksa sadece "{notFoundAnswer}" yaz; bu cümleye numara ya da başka bir şey ekleme.
4. Müşteri kendi hesabına özel bir bilgi (bakiye, limit, borç, başvuru durumu, hesap hareketleri) sorarsa cevaba tam olarak "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum." cümlesiyle başla. Başka bir bankanın ürününü ya da koşulunu sorarsa "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum." cümlesiyle, yatırım tavsiyesi ya da piyasa tahmini isterse "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum." cümlesiyle başla. Bu ilk cümleye numara yazma. <parcalar> içinde konuyla ilgili genel bilgi varsa ardından kural 2'ye göre numarasıyla ekle; yoksa başka bir şey yazma. Bu kural kural 3'ten önce gelir.
```

### `prompts/message-label.st` (düzeltme turu 1 sonrası tam metin)
```
Bir bankanın dijital asistanına gelen müşteri mesajını sınıflandırıyorsun. Mesaja cevap yazma.
SELAM: selamlaşma ya da hal hatır sorma.
TESEKKUR: teşekkür etme, memnuniyetini ya da minnettarlığını belirtme.
VEDA: vedalaşma, konuşmayı bitirme.
DIGER: soru, istek, şikâyet, anlamsız metin ve diğer her şey. Selam ya da teşekkürle birlikte soru da soran mesaj DIGER'dir.
Sadece tek kelime yaz: SELAM, TESEKKUR, VEDA ya da DIGER.

Örnekler:
Hey, orada mısın? → SELAM
Hayırlı sabahlar, nasılsınız? → SELAM
Merhaba asistan, iyi misin? → SELAM
Çok işime yaradı → TESEKKUR
Allah razı olsun → TESEKKUR
Minnettarım, sağ olasın → TESEKKUR
Bu bilgi çok yardımcı oldu → TESEKKUR
Şimdilik bu kadar, hoşça kal → VEDA
Hadi bana müsaade → VEDA
İyi geceler, sonra yazarım → VEDA
Selam, kartımı kaybettim → DIGER
Teşekkürler ama kartım hâlâ gelmedi → DIGER
Yarın yağmur yağar mı? → DIGER
qwe asd → DIGER
```
Örneklerde ölçüm listesindeki hiçbir mesaj geçmez (T36). `MessageLabeler.parseLabel` ve zaman aşımı değişmez.

### D5: giriş cümlesi alıntılarının silinmesi
- **Tek kaynak:** üç giriş cümlesi `application.properties`'e taşınır (`\u` kaçışlı): `assistant.generation.personal-info-opener`, `assistant.generation.other-banks-opener`, `assistant.generation.investment-advice-opener`. `AssistantProperties.Generation`'a üç `@NotBlank String` + `List<String> targetedOpeners()`. `prompts/rag-system.st` kural 4'te cümleler yerine `{personalInfoOpener}`, `{otherBanksOpener}`, `{investmentAdviceOpener}`; `PromptBuilder` bunları `notFoundAnswer` ile birlikte render eder. Modele giden metin değişmez.
- **Kural (`AnswerFinalizer.finalizeRagAnswer`, sıra):** (1) "bulamadım" tespiti (D1) tutarsa mevcut davranış. (2) Cevabın ilk cümlesi (baştan ilk `.`/`!`/`?`'ye kadar, o işaret dahil) içindeki `[n]` işaretleri çıkarılıp normalize edilince (tr küçük harf, boşluklar teke, `strip`) bir giriş cümlesinin normalize hâline eşitse: o cümlenin içindeki bütün `[n]` işaretleri önlerindeki boşlukla birlikte, ve bitiş işaretinden hemen sonra gelen, aralarında sadece boşluk olan bir ya da daha fazla `[n]` (boşluklu ya da boşluksuz: `. [1]`, `.[4][2]`, `. [5] [1]`) silinir. Silinen yerde giriş cümlesinin bitiş işaretinden sonra metin devam ediyorsa tek boşluk kalır. Geri kalan metin değişmez. (3) Aralık dışı `[n]` silinir (mevcut). (4) `citations` bu temizlenmiş metinden `parseCitations` ile hesaplanır (artık ham model metninden değil). (5) `strip()`.
- Giriş cümlesi cevabın başında değilse (ikinci cümle) hiçbir şey silinmez.
- `NotFoundAnswerStreamFilter` değişmez: numara akış sırasında görünebilir; `done.answer` temizlenmiş metindir (D1'deki sınırla aynı).
- Log: `INFO Opener citations removed conversationId={} removedCount={}`.
- Sözleşme: sse.md `done.answer` tanımı, openapi `sendMessage` "Not found answer" paragrafı güncellendi.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T37 | Giriş cümlesi alıntıları | unit `AnswerFinalizerTest` (parametreli, 5 parça) | `givenAnswerStartingWithOpenerAndCitedOpener_whenFinalize_thenOpenerCitationsRemovedAndRestKept` | (a) "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. [1]" → giriş cümlesi tek başına, citations []. (b) "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.[4][2] Raşit Bankası'nda konut kredisi aylık %2,79'dan başlar [1]." → "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum. Raşit Bankası'nda konut kredisi aylık %2,79'dan başlar [1].", citations [1]. (c) "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum [5]. Altın hesabı Raşit Mobil'den açılır [1]." → "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum. Altın hesabı Raşit Mobil'den açılır [1].", citations [1]. (d) "YATIRIM TAVSİYESİ YA DA PİYASA TAHMİNİ VEREMİYORUM. [3] [2]" → "YATIRIM TAVSİYESİ YA DA PİYASA TAHMİNİ VEREMİYORUM.", citations [] |
| T38 | Giriş cümlesi başta değil / hiç yok | unit | `givenOpenerNotAtStartOrAbsent_whenFinalize_thenCitationsUnchanged` | "Kart limitin gelirine göre belirlenir [1]. Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. [2]" → aynı metin, citations [1, 2]; "Kartını hemen kapat [1]. Yenisi gelir [2]." → aynı, citations [1, 2] |
| T39 | Akışta giriş cümlesi | integration `SendMessageSseIntegrationTest` | `givenModelCitesOpener_whenSendMessage_thenDoneAnswerAndCitationsExcludeOpenerCitation` | Sahte model "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum. [1] ", "Kumbara hesabını Raşit Mobil'den açabilirsin [2]." → token'lar olduğu gibi; sources `citations` [2]; done `answer` "…göremiyorum. Kumbara hesabını Raşit Mobil'den açabilirsin [2].", `notFoundAnswer` false; DB `answer` aynı, `cited` `{2}` |
| T34 (değişti) | Talimat | unit `PromptBuilderTest` | `givenRagPrompt_whenBuild_thenSystemMessageHasTargetedOpenersBeforeNotFoundRule` | Sistem mesajı yapılandırmadaki üç giriş cümlesini birebir içerir (test kendi değerlerini properties'le verir) |

### Ölçüm listesi ve betik değişiklikleri
- `assistant-api/scripts/negative-messages.txt`: "rehberde olmayan" 27 sorudan 15'i yeni kategoriye alınır, metinler değişmez:
  - `personal-info` (9): konu 1 Kişisel bakiye, konu 2 Hesap hareketleri, konu 5 Kişiye özel diğer bilgiler (3'er soru).
  - `other-banks` (3): konu 4 Başka bankaların ürünleri.
  - `investment-advice` (3): konu 8 Piyasa tahmini ve yatırım tavsiyesi.
  - Kalan 12 (`not-in-guide`): konu 3, 6, 7, 9.
- `measure_answers.py` ekleri:
  - Kategori → giriş cümlesi: `personal-info` → "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum.", `other-banks` → "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.", `investment-advice` → "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum."
  - Satır başına: `startsWithOpener` (normalize cevap — tr küçük harf, boşluklar teke — normalize giriş cümlesiyle başlıyor mu), `openerHasCitation` (D5 ile yeniden tanımlandı: cevabın ilk cümlesinde, ilk `.`/`!`/`?`'ye kadar, **ya da** bitiş işaretinden hemen sonra, arada sadece boşluk olan bir ya da daha fazla `[n]` var mı; ör. "… göremiyorum. [1]"), `openerTrailingCitation` (sadece bitiş işaretinden sonraki kısım; bilgi amaçlı, backend-coder ekledi), `streamedOpenerHasCitation` (aynı kontrol `streamedText` üzerinde; modelin kaç kez numara eklediğini gösterir, kabul dışı), `citedChunks` (`[{n, chunkId, pageNumber, heading}]`, `sources.contextChunks`'tan; elle okuma için), `paraphrasedNotFound` (D1 kalıbı ham cevapta).
  - Özet: üç kategori için `startsWithOpener`, `notFound`, `withCitations`, `openerHasCitation` sayıları; golden için `paraphrasedNotFound` sayısı ve herhangi bir giriş cümlesiyle başlayan golden cevap sayısı (`goldenStartsWithOpener`).

### Yeni kabul (M4 ve M3)
- **Hedefli gruplar (15 mesaj: `personal-info`, `other-banks`, `investment-advice`):** her cevap ya sabit "bulamadım" cümlesidir (alıntı yok) ya da kendi kategorisinin giriş cümlesiyle başlar (`startsWithOpener` true) ve giriş cümlesine numara bağlanmamıştır: `openerHasCitation` false (tanım bitiş noktasından sonra gelen "[1]"i de kapsar). Kontrol `done.answer` üzerinde yapılır; bu 15 mesaj için kabul M4b'ye (D5'li kod) göre değerlendirilir, M4 sonucu bilgi olarak raporlanır. Bu kısım **deterministik** (betik). Cevapta alıntı varsa her alıntılı cümle alıntıladığı parçaya dayanmalıdır: bu kısım **elle okuma**; qa her alıntıyı `GET /api/workspaces/42/chunks/{chunkId}` metniyle karşılaştırır, sonuçları rapora tablo olarak yazar (mesaj, cümle, parça, dayanıyor mu). Dayanmayan alıntı 0.
- **Diğer negatifler (40 mesaj):** `citations > 0` olan cevap 0.
- **Selamlaşma:** 18/18 `SMALL_TALK` (13 `PATTERN`, 5 `LABEL`).
- "Bitcoin alıp satabilir miyim?", "Bugün hava nasıl?", "Bana bir şiir yaz", "asdfgh" → `notFoundAnswer` true.
- **Golden:** bulamadım'a düşen (M0'da cevaplı, M4'te bulamadım) ≤ 2; `paraphrasedNotFound` 0; `goldenStartsWithOpener` (üç giriş cümlesinden herhangi biriyle başlayan golden cevap, normalize önek eşleşmesi, betik otomatik) ≤ 2 (kullanıcı kararı, 2026-11-03); bu cevaplar raporda soru metniyle listelenir.
- `secondPersonSelfReference` 0.

### Test değişiklikleri (tester)
| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T31 | Başka kelimelerle bulamadım | unit `AnswerFinalizerTest` | `givenParaphrasedNotFoundWithCitation_whenFinalize_thenFixedSentenceWithoutCitations` | "Diğer bankaların EFT ücretlerini rehberimizde bulamadım. [3]" ve "Başka bankanın kartının avantajlarını rehberimizde bulamadım; Raşit Bankası'nın kendi kart özellikleri hakkında bilgi verebilirim. [1]" (5 parça) → answer sabit cümle, `notFoundAnswer` true, citations []; "REHBERİMİZDE ARADIM AMA BULAMADIM." → aynı |
| T32 | Kalıp farklı cümlelerde ya da hiç yok | unit `NotFoundAnswerDetectorTest` | `givenRehberimizdeAndBulamadimInDifferentSentencesOrAbsent_whenIsNotFoundAnswer_thenFalse` | "Aidat rehberimizde 750 TL olarak geçiyor [1]. Başka bir ücret bulamadım." → false; "Bu bilgi rehberimizde yer alıyor [1]." → false; "Kartı bulamadım diyorsan hemen kapat [1]." → false |
| T33 | Akışta a türü | integration `SendMessageSseIntegrationTest` | `givenModelWritesParaphrasedNotFound_whenSendMessage_thenDoneAnswerIsFixedSentenceAndNoCitations` | Sahte model "Diğer bankaların EFT ücretlerini ", "rehberimizde bulamadım. [3]" → token'lar olduğu gibi; sources `citations` []; done `notFoundAnswer` true, `answer` sabit cümle; DB `answer` sabit cümle, `cited` `{}` |
| T34 | Cevap talimatı kural 4 | unit `PromptBuilderTest` (T12'ye ek) | `givenRagPrompt_whenBuild_thenSystemMessageHasTargetedOpenersBeforeNotFoundRule` | Sistem mesajı üç giriş cümlesini birebir içerir ve "Bu ilk cümleye numara yazma" der |
| T35 | Etiket örnekleri prompt'ta | unit `MessageLabelerTest` (T28'e ek) | `givenMessage_whenLabelMessage_thenSystemMessageContainsLabelExamples` | Sistem mesajı "Çok işime yaradı → TESEKKUR" ve "Selam, kartımı kaybettim → DIGER" satırlarını içerir; kullanıcı mesajı yine sadece müşteri mesajı |
| T36 | Ölçüme sızma yok | unit `MessageLabelerTest` | `givenLabelPromptAndMeasurementList_whenCompared_thenNoMeasurementMessageIsAnExample` | `prompts/message-label.st` ile `scripts/negative-messages.txt`'teki her mesaj `SmallTalkDetector.phraseKey` ile karşılaştırılır; özellikle "Çok makbule geçti", "Selamlar herkese", "Keyifler nasıl?", "Görüşmek dileğiyle", "Merhabalar efendim, hayırlı işler" örneklerde yok |

T26-T28, T6-T11 değişmeden kalır. Web testleri değişmez.

### Kart
Kabul kriterleri ve kapsam (hedefli talimat) değiştiği için kart güncellendi, son satır `ONAY: BEKLİYOR`.

## Düzeltme turu 2
**Sebep (qa, `handoff/05-qa-report.md` "Düzeltme" 1):** cevaplı bir sorudan sonra kalıp listesinde olmayan teşekkür
("Çok işime yaradı sağ olun", "Allah razı olsun", "Harikasınız, eyvallah", "Yardımların için minnettarım", "Çok makbule
geçti, sağ olun") yeniden yazmayla önceki soruya çevriliyor, puan eşiği geçiliyor, etiket sorulmuyor; model 6 denemenin
5'inde "… memnuniyetle yardımcı oldum [1]" gibi uydurma alıntı yazıyor. **Kullanıcı kararı (lead aracılığıyla, 2026-11-03):
"önce etiket".** Etkilenen roller: backend-coder, tester, qa. **frontend-coder etkilenmez:** bu yolla tanınan selamlaşma
turu, kalıp listesi turuyla aynı SSE alanlarını taşır (`answerSource` `SMALL_TALK`, `retrievalStrategy` `NONE`); ekran ve
alanlar değişmez.

| # | Madde | Plandaki ya da sözleşmedeki değişiklik |
|---|-------|----------------------------------------|
| E1 | Önce etiket: önceki (selamlaşma olmayan) tur varsa ve mesaj kısaysa yeniden yazmadan önce ham mesaj için etiket | Aşağıda "Davranış"; `AnswerService` sırası değişir |
| E2 | Kısalık ölçütü | `SmallTalkDetector.isShortMessage(String)`: normalize kelime sayısı ≤ 10 **ve** ham metinde `?` yok (ilk tanım 6'ydı; M5 sonrası kullanıcı kararıyla 10, aşağıda "Ek: M5 sonrası") |
| E3 | Etiket talimatı örnekleri ölçüm listesiyle çakışmasın | `prompts/message-label.st` örnekleri değişir (aşağıda); T36 sıkılaşır |
| E4 | Ölçüm: qa'nın 6 teşekkürü + yeni takip selamları + kısa gerçek takip soruları | Negatif listeye 18 iki turlu senaryo; M5 tam koşu |
| E5 | Kabul | Aşağıda "Kabul (M5 ve qa M3)" |
| E6 | Sözleşme | sse.md `SMALL_TALK` ve "Sıra"; openapi `sendMessage` ve `TurnStatus` 3a güncellendi; alan yok |

### Davranış (`AnswerService.answerWithRetrieval`, yeni sıra; sadece `RAG`)
1. Kalıp listesi (değişmez): tutarsa hazır metin, `SMALL_TALK`, arama yok.
2. **Önce etiket (yeni):** `history` boş değilse (cevabı tamamlanmış, `error_code`'suz, selamlaşma olmayan önceki tur var; `findAnsweredTurns` bunu zaten sağlar) **ve** `smallTalkDetector.isShortMessage(question)` ise `messageLabeler.labelMessage(question)` (ham mesaj; geçmiş ve parça yok; aynı talimat ve `label-timeout`). Etiket `GREETING`/`THANKS`/`FAREWELL` → `streamFixedAnswer(…, answerFor(label), SMALL_TALK, …)`; draft `searchQuery` null, `queryRewriteStatus` `DISABLED`, context null, `messageLabel` + `messageLabelLatencyMs` dolu → strateji `NONE`, arama yok, yeniden yazma yok. Etiket `OTHER`, hata, zaman aşımı ya da tanımsız çıktı → sonuç saklanır, adım 3'e geçilir (hata dönmez).
3. Yeniden yazma + retrieval (değişmez; 503 burada).
4. Boş bilgi bankası (değişmez).
5. Eşik altı: adım 2'de etiket alındıysa **yeniden sorulmaz**, saklı `OTHER` sonucu kullanılır → "bulamadım" (`BELOW_RERANK_THRESHOLD`, `messageLabel` OTHER ve adım 2'nin süresi). Alınmadıysa bugünkü gibi sorulur.
6. Model yolu (değişmez).

İlk tur (geçmiş yok) ve golden soruları bu adımdan geçmez. `TOP_K` / `RERANK_FALLBACK`'te de adım 2 çalışır (arama öncesidir, reranker puanına bağlı değildir); S2 kararı (eşik yok) değişmez. Sadece LLM değişmez.

- `SmallTalkDetector.isShortMessage(String message)`: `SuggestionTextNormalizer.normalize(message)` sonucu boşlukla bölünür; 1 ≤ kelime sayısı ≤ `SHORT_MESSAGE_MAX_WORDS = 10` **ve** `message.indexOf('?') < 0` → `true`. Sabit kodda (iş kuralı değil, tanım); değiştirmek plan kararıdır.
- Log: `INFO Message labeled before rewrite conversationId={} label={} durationMs={} failed={}`; selamlaşma ise `Small talk answered … route=PRE_REWRITE_LABEL`.
- Ölçüm betiğinde `route`: SSE'de kalıp listesi ile arama öncesi etiket ayrılamaz (ikisi de `NONE` + `SMALL_TALK`); betik ikisini `NO_SEARCH` diye sayar, `RERANK` + `SMALL_TALK` → `LABEL`. Ayrım gerekirse qa `conversation_turns.message_label_latency_ms`'e bakar (kalıp listesinde NULL, etiketle dolu).

### `prompts/message-label.st` (düzeltme turu 2 sonrası tam metin)
```
Bir bankanın dijital asistanına gelen müşteri mesajını sınıflandırıyorsun. Mesaja cevap yazma.
SELAM: selamlaşma ya da hal hatır sorma.
TESEKKUR: teşekkür etme, memnuniyetini ya da minnettarlığını belirtme.
VEDA: vedalaşma, konuşmayı bitirme.
DIGER: soru, istek, şikâyet, anlamsız metin ve diğer her şey. Selam ya da teşekkürle birlikte soru da soran mesaj DIGER'dir. Önceki konuşmaya dayanan kısa bir soru ya da istek de DIGER'dir.
Sadece tek kelime yaz: SELAM, TESEKKUR, VEDA ya da DIGER.

Örnekler:
Hey, orada mısın? → SELAM
Hayırlı sabahlar, nasılsınız? → SELAM
Merhaba asistan, iyi misin? → SELAM
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
```
Turu 1'deki "Çok işime yaradı", "Allah razı olsun", "Minnettarım, sağ olasın" örnekleri çıkarıldı (qa'nın ölçülecek mesajlarıyla çakışıyordu).

### Ölçüm listesi eki (`assistant-api/scripts/negative-messages.txt`; mevcut 55 satır değişmez, 18 iki turlu satır eklenir → 73)
- `follow-up-small-talk` (10; ikinci mesaj değerlendirilir):
  - qa'nın 6'sı: `Gold kart aidatı ne kadar? >> Çok makbule geçti` · `Kartımı kaybettim, ne yapmalıyım? >> Çok makbule geçti, sağ olun` · `Gold kart aidatı ne kadar? >> Çok işime yaradı sağ olun` · `Gold kart aidatı ne kadar? >> Allah razı olsun` · `Gold kart aidatı ne kadar? >> Yardımların için minnettarım` · `Gold kart aidatı ne kadar? >> Harikasınız, eyvallah`
  - Yeni 4: `Kartımı kaybettim, ne yapmalıyım? >> Merhaba tekrar` · `Gold kart aidatı ne kadar? >> Her şey için sağ olun, iyi akşamlar` · `Kartımı kaybettim, ne yapmalıyım? >> Tamamdır, kolay gelsin` · `Gold kart aidatı ne kadar? >> Size de iyi çalışmalar`
- `follow-up-question` (8; kısa gerçek takip, ikinci mesaj değerlendirilir): `Gold kart aidatı ne kadar? >> Peki Platinum?` · `Gold kart aidatı ne kadar? >> ya kredi kartında` · `Kartımı kaybettim, ne yapmalıyım? >> Ya yurt dışında` · `Kartımı kaybettim, ne yapmalıyım? >> Yenisi kaç günde gelir` · `Kartımı kaybettim, ne yapmalıyım? >> Peki ücreti var mı` · `Kartımı kaybettim, ne yapmalıyım? >> Mobil uygulamadan da olur` · `Gold kart aidatı ne kadar? >> Ek kart için de aynı mı` · `Gold kart aidatı ne kadar? >> Hangi kartlarda yok`
- Betik özeti iki yeni kategori için: `SMALL_TALK` (rotaya göre), alıntılı, bulamadım sayıları ve her satırın `answer`'ı.

### Kabul (M5 ve qa M3)
- **M5 öncesi sistem sıfırlama (backend-coder, kullanıcı kararı):** bilgi bankasında e2e testlerinin eklediği 2 fazladan "E2E Rehber" belgesi var. Ölçümden önce: `docker compose -f deployment/docker-compose.yml down -v` → `docker compose -f deployment/docker-compose.yml up -d --build` → rehber 2026.10'u "Belge ekle" ile (`POST /api/workspaces/42/sources`) yükle; "E2E Rehber" belgeleri böylece temizlenmiş olur (iş `DONE`, 154 parça) → golden set'i `PUT /api/assistant/golden-set` ile yükle → `GET /api/workspaces/42/sources` ile tek kaynak olduğunu raporla. Eşik −4,0 aynı kalır (properties varsayılanı).
- **M5** = D5 sonrası koddan sonra, düzeltme turu 2'li kodla tam koşu: golden 100 + 73 negatif → `handoff/measure/m5-after-fix2.json`.
- **e2e sırası:** e2e testleri bilgi bankasına belge ekler. qa önce M3 ölçümünü, sonra e2e'yi koşar. Ölçüm, e2e'den sonra tekrarlanacaksa önce aynı sıfırlama yapılır.
- `follow-up-small-talk` 10/10: `answerSource` `SMALL_TALK` ve alıntı 0.
- `follow-up-question` 8/8: `answerSource` `SMALL_TALK` değil (selam etiketi 0). Cevapların alıntı ve bulamadım sayıları raporda (bilgi; rehber sorusu oldukları için cevaplı ve alıntılı olmaları beklenir).
- Önceki kabul maddelerinin hepsi aynı (turu 1 "Yeni kabul": hedefli 15, diğer 40 negatif alıntı 0, selam 18/18, Bitcoin/hava/şiir/asdfgh, golden bulamadım ≤ 2, `paraphrasedNotFound` 0, `goldenStartsWithOpener` ≤ 2, `secondPersonSelfReference` 0).

### Ek: M5 sonrası (kullanıcı kararı, 2026-11-03)
- **Bulgu:** `Gold kart aidatı ne kadar? >> Her şey için sağ olun, iyi akşamlar` 7 kelime olduğu için önce etiket adımı çalışmadı; cevap "İyi akşamlar! [5]" oldu.
- **Karar:** `SHORT_MESSAGE_MAX_WORDS` 6 → **10**; "`?` yok" koşulu aynı. Kod değişikliği sadece bu sabit (+ T40, T44 beklentileri).
- **Ölçüm listesine yeni kategori `follow-up-question-long`** (7 iki turlu senaryo; ikinci mesaj 7-10 kelime, `?` yok, gerçek takip sorusu; hiçbiri etiket örneklerinde yok, T36 kapsar):
  - `Gold kart aidatı ne kadar? >> Platinum kartın aidatı bundan daha mı yüksek` (7)
  - `Gold kart aidatı ne kadar? >> Peki bunu mobil uygulamadan da ödeyebilir miyim acaba` (8)
  - `Gold kart aidatı ne kadar? >> Aidatı ilk yıl için kaldırmanın bir yolu var mı` (9)
  - `Gold kart aidatı ne kadar? >> Ek kart alırsam onun için de ayrıca ücret ödüyor muyum` (10)
  - `Kartımı kaybettim, ne yapmalıyım? >> Kartı kapattıktan sonra bulursam tekrar açabilir miyim acaba` (8)
  - `Kartımı kaybettim, ne yapmalıyım? >> Kayıp bildirimini yurt dışındayken nasıl yapacağımı da söyler misin` (9)
  - `Kartımı kaybettim, ne yapmalıyım? >> Yeni kart adresime mi gelir yoksa şubeden mi almam gerekir` (10)
- **Yeniden ölçüm M5b:** sadece iki turlu senaryolar: `follow-up` (1), `follow-up-small-talk` (10), `follow-up-question` (8), `follow-up-question-long` (7) = 26 → `handoff/measure/m5b-followups.json`. Betik `--categories` ile süzer (yoksa backend-coder ekler).
- **Golden ve tek turlu negatifler için M5 geçerli kalır. Gerekçe:** değişen tek şey önce etiket adımının kelime sınırı. Bu adım sadece konuşmada cevaplanmış önceki bir tur varken çalışır. Golden soruları ve tek turlu negatifler her zaman yeni konuşmanın ilk turudur (geçmiş boş), bu yüzden bu adıma hiç girmezler. Etiket talimatı, eşik, cevap talimatı ve denetim bu ekte değişmedi.
- **Sıfırlama:** M5'ten sonra e2e koşulduysa M5b'den önce aynı sıfırlama yapılır (yukarıda); koşulmadıysa gerekmez.
- **Kabul (M5b):** `follow-up-small-talk` 10/10 `SMALL_TALK` ve alıntı 0; `follow-up-question` 8 + `follow-up-question-long` 7 = 15 mesajda `SMALL_TALK` 0; `follow-up` (Teşekkürler) `SMALL_TALK`. Diğer bütün maddeler M5 sonucuyla değerlendirilir.

### Testler (tester)
| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T40 | Kısalık | unit `SmallTalkDetectorTest` (parametreli) | `givenMessages_whenIsShortMessage_thenUpToTenWordsWithoutQuestionMark` | "Çok işime yaradı sağ olun" → true; "Her şey için sağ olun, iyi akşamlar" (7) → true; "Bir iki üç dört beş altı yedi sekiz dokuz on" → true; "Bir iki üç dört beş altı yedi sekiz dokuz on onbir" → false; "Peki Platinum?" → false; "ya kredi kartında" → true; "🙂 !!" → false (0 kelime) |
| T41 | Takip teşekkürü | unit `AnswerServiceTest` | `givenHistoryAndShortThanks_whenSendMessage_thenFixedThanksWithoutRewriteOrRetrieval` | Geçmiş 1 tur; etiket THANKS → `streamFixedAnswer` teşekkür metni + `SMALL_TALK`; `QueryRewriter` ve `ContextRetriever` etkileşimsiz; draft `messageLabel` THANKS, süre dolu, `searchQuery` null |
| T42 | Takip, etiket DIGER | unit | `givenHistoryAndShortOtherMessage_whenSendMessage_thenNormalPathAndLabelAskedOnce` | Etiket OTHER → yeniden yazma ve retrieval çağrıldı; eşik altı olsa da `labelMessage` toplam 1 kez; `BELOW_RERANK_THRESHOLD`, `messageLabel` OTHER |
| T43 | İlk tur | unit | `givenNoHistoryAndShortMessage_whenSendMessage_thenNoLabelBeforeRetrieval` | Geçmiş boş, "Allah razı olsun" → `labelMessage` retrieval'dan önce çağrılmadı (eşik üstü fixture'da hiç çağrılmadı) |
| T44 | Uzun ya da soru işaretli | unit | `givenHistoryAndLongOrQuestionMarkMessage_whenSendMessage_thenNoLabelBeforeRewrite` | "Peki Platinum?" ve 11 kelimelik mesaj → `labelMessage` yeniden yazmadan önce çağrılmadı |
| T45 | Etiket hatası | unit | `givenHistoryAndLabelFailure_whenSendMessage_thenNormalPathWithoutError` | `labelMessage` → `OTHER`, `failed` true → yeniden yazma ve retrieval yapıldı, exception yok |
| T46 | Akışta takip teşekkürü | integration `SendMessageSseIntegrationTest` | `givenAnsweredTurnThenUnlistedThanks_whenSendMessage_thenSmallTalkWithoutSearchOrCitation` | Gold (cevaplı), sonra "Allah razı olsun", sahte etiket "TESEKKUR" → token teşekkür metni; sources `NONE`, `contextChunks` [], `citations` []; done `SMALL_TALK`; DB `retrieval_strategy` NONE, `search_query` NULL, `message_label` THANKS, `message_label_latency_ms` dolu, `cited` `{}`; yeniden yazma çağrılmadı |
| T36 (sıkılaştı) | Ölçüme sızma yok | unit `MessageLabelerTest` | `givenLabelPromptAndMeasurementList_whenCompared_thenNoMeasurementMessageOverlapsAnExample` | `negative-messages.txt`'teki her mesajın (iki turlu satırların iki mesajı da) `phraseKey`'i ile her örneğin `phraseKey`'i karşılaştırılır: hiçbiri eşit değil; ayrıca 3 ve daha fazla kelimeli mesajlarda biri ötekini (kelime sınırında) içermiyor. 1-2 kelimeli mesajlar ("Teşekkürler", "Selam") sadece eşitlikle kontrol edilir, çünkü örneklerde kelime olarak geçmeleri doğaldır |
| T35 (değişti) | Örnekler prompt'ta | unit | `givenMessage_whenLabelMessage_thenSystemMessageContainsLabelExamples` | "Bu bilgi çok yardımcı oldu → TESEKKUR" ve "Ya bireysel kredide → DIGER" satırları |

STATUS: DONE
