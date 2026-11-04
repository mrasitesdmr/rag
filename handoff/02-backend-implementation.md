TASK: TASK-032
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline`, `sse-streaming`, `rag-evaluation`.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|------------------------|-----------|-----|
| K1 kelime listesi kalkar (Silinecekler, Karar 1) | `generation/SmallTalkDetector.java` silindi; `resources/small-talk/{greeting,thanks,farewell,address}.txt` ve klasör silindi; `AnswerService` (`answerSmallTalkByPattern`, `PATTERN_ROUTE` kalktı) | Silmeler `git rm` ile yapıldığı için index'e (staged) girdi |
| K3 kısa mesaj tanımı (kart S1 = B, Karar 2) | `generation/ShortMessageDetector.java` (yeni, `@Component`) | `isShortMessage`: normalize boş değil; `?` yoksa ≤ 10, varsa ≤ 5 kelime (`SHORT_MESSAGE_MAX_WORDS`, `SHORT_QUESTION_MAX_WORDS`) |
| K2 her kısa mesaj aramadan önce etiketlenir (Karar 3, 10) | `conversation/service/AnswerService.java` | `!history.isEmpty()` koşulu kalktı; `preRewriteLabel` → `preSearchLabel`, `PRE_SEARCH_LABEL_ROUTE = "PRE_SEARCH_LABEL"`; log `Message labeled before search …`. Selamlaşmaysa `answerSmallTalkWithoutSearch` (değişmedi); diğer etiket saklanır, eşik altında yeniden sorulmaz, model yolunda draft'a yazılır |
| K4 uzun mesajda eşik altı etiket | `AnswerService.answerBelowThreshold` | Değişmedi |
| `foldTurkishLetters` taşınması | `generation/MessageLabeler.java` | `private static`; sınıf yorumu güncel; `parseLabel` aynı |
| K9/K10 yeniden yazma (kart S2 = A, Karar 5, 6) | `retrieval/service/QueryRewriter.java`, `prompts/query-rewrite.st` | Geçmiş = her tur `- <soru>`; `HistoryTurn.answer()` kullanılmıyor; şablon plandaki Türkçe metin; `STANDALONE_QUERY_LABEL` = `^(?i)(standalone query|arama sorusu):` |
| K6/K7/K8 sistem talimatı (Karar 7) | `prompts/rag-system.st` | Plandaki tam metin |
| S2/K8 isim mesajı (kart S4 = A, Karar 8) | `prompts/message-label.st` | Plandaki tam metin (21 örnek) |
| K5 eşik (kart S3 = A) → D1 | `application.properties` | Kullanıcı -4,0 seçti; varsayılan zaten `${RETRIEVAL_MIN_RERANK_SCORE:-4.0}`, değişmedi |
| Düzeltme turu 1 D2 kısaltılmış selam örnekleri | `prompts/message-label.st` | Mrhb, Meraba, Slmlr, Nbr kanka, Gnydn → SELAM; Tşkkrler → TESEKKUR; Grşrz → VEDA (planlanan yerlere; toplam 28 örnek) |
| Düzeltme turu 1 D3 harf/rakamsız mesaj | `ShortMessageDetector.hasNoWords`, `AnswerService.answerWithoutWords` (adım 0, 409 ve geçmişten sonra, etiketten önce), `AnswerSource.NO_WORDS`, `AnswerStreamer.isFixedNotFoundAnswer` | Etiket, yeniden yazma, arama, rerank, model yok; draft `searchQuery` null, `DISABLED`, context/label null; log `Message without words answered conversationId={} questionLength={}`. `AnswerStreamer`'da `NO_WORDS` sabit bulamadım sayılır (`not_found_answer` true; V9 CHECK bunu ister). LLM_ONLY değişmedi |
| Düzeltme turu 1 V9 | `db/migration/V9__add_no_words_answer_source.sql` | `ck_conversation_turns_answer_source` + `NO_WORDS`; `ck_conversation_turns_none_reason` (`SMALL_TALK`, `NO_WORDS`); yeni `ck_conversation_turns_no_words`; geri doldurma yok |
| Ölçüm listesi (Backend planı) | `scripts/negative-messages.txt` | Başlık yorumu güncel; eklenen kategoriler: `small-talk-question` 5, `small-talk-short` 3, `name` 3, `personal-info-with-info` 1, `follow-up-independent` 3; ek satırlar: off-topic, not-in-guide, gibberish, follow-up-question. Toplam 99 mesaj (planın kabul sayılarıyla aynı) |
| Ölçüm betiği | `scripts/measure_answers.py` | Satır: `searchQuery`, `queryRewriteStatus`, `shortMessage`, `wrongOpener`, `citedHeadings`; `route` `NO_WORDS`. Özet: golden `fallen` (+ metinler), `wrongOpener`; kategori başına `smallTalk`, `noWords`, `wrongOpener`, `withCitations`, `notFound`; `shortMessageLatencyMs`. `--threshold-candidates=` → `summary.threshold` (en düşük/yüksek 3, medyan, hedefli puanlar, kural A/B/C değerleri, aday tablosu, `noScore`) |
| `assistant-api/CLAUDE.md` | `assistant-api/CLAUDE.md` | Durum, Ollama satırı, eşik satırı (ölçümle seçildi), etiket zaman aşımı notu (soğuk model), V9, `ShortMessageDetector`, akış sırası, prompt açıklamaları, ölçüm komutu (yeni alanlar, `--threshold-candidates=` eşittir işaretiyle) |

## Değişen dosyalar
- Yeni: `src/main/java/com/rasitbank/assistant/generation/ShortMessageDetector.java`, `src/main/resources/db/migration/V9__add_no_words_answer_source.sql`
- Değişen: `conversation/service/AnswerService.java`, `conversation/service/AnswerStreamer.java`, `conversation/domain/entity/AnswerSource.java`, `generation/MessageLabeler.java`, `retrieval/service/QueryRewriter.java`, `prompts/rag-system.st`, `prompts/query-rewrite.st`, `prompts/message-label.st`, `scripts/measure_answers.py`, `scripts/negative-messages.txt`, `assistant-api/CLAUDE.md`
- Silinen: `generation/SmallTalkDetector.java`, `resources/small-talk/greeting.txt`, `thanks.txt`, `farewell.txt`, `address.txt`
- Ölçüm: `handoff/measure/m1-no-threshold.json`, `handoff/measure/m2-final.json`
- Dependency: yok. `application.properties`: değişmedi.

## Plandan sapmalar
- **Ölçüm 8091 portunda yapıldı (lead onaylı).** 8090'daki assistant-api (kullanıcının IntelliJ'den başlattığı süreç) durdurma izni sınıflandırıcı tarafından engellendi. Yeni kod depo dışındaki bir kopyada paketlendi (`-Dmaven.test.skip=true`) ve `java -jar` ile `SERVER_PORT=8091` olarak açıldı. Aynı `assistant` veritabanı, retrieval-api, Ollama ve reranker kullanıldı. 8090'a dokunulmadı; hâlâ eski kodla çalışıyor.
- **Golden set yüklendi (lead onaylı).** Önceden yüklü değildi (`GOLDEN_SET_NOT_LOADED`). `PUT /api/assistant/golden-set` ile `rehber-golden-v1` (100 soru) eklendi. Başka veri silinmedi; mevcut turlar duruyor.
- **Liste sayısı:** M1 mesajında ve ara raporda listeyi "102 mesaj" yazmıştım; doğrusu 99 (toplam 199). Kategori sayıları planın kabul sayılarıyla birebir aynı.
- **Planın üstüne:** `AnswerStreamer.isFixedNotFoundAnswer` `NO_WORDS`'ü de kapsıyor. Plan "`not_found_answer` true" diyor; bu satır olmadan V9 CHECK'i kaydı reddederdi.

## M1 (yeni kod, eşik kapalı) — `handoff/measure/m1-no-threshold.json`
Komut: `python3 assistant-api/scripts/measure_answers.py --base-url http://localhost:8091 --threshold-candidates=-3,-2,-1,-0.5,0 --out handoff/measure/m1-no-threshold.json` (`RETRIEVAL_MIN_RERANK_SCORE=` ile açılmış 8091, 199 mesaj). Bilgi bankası: tek kaynak "Test" V1 = `Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf`, 70 sayfa, 154 parça, DONE. Fazladan belge yok.

- Golden en düşük 3: -2,74 ("Haksız alınan ücret iade edilirken faiz de ödeniyor mu?"), -1,45 ("Dolar cinsinden belirlenen bir ücreti hangi kurla ödüyorum?"), -0,73 ("Mirası reddetmek için ne kadar sürem var?"); medyan 3,41.
- Konu dışı en yüksek 3: 1,23 ("???"), -0,46 ("İşletme kredisi faiz oranınız ne kadar?"), -3,26 ("Bankanın sahibi kim?").
- "Kredi limitim ne kadar": 1,08.
- Kural A: `floor_0.5(-2,74 − 1)` = **-4,0** (B -1,0; C -3,5). Kullanıcı -4,0'ı seçti.

| Aday | Kesilen rehber (/100) | Kesilen konu dışı (/27) | Kesilen hedefli (/16) |
|------|----------------------|-------------------------|-----------------------|
| -4,0 (A) | 0 | 23 | 2 |
| -3,5 (C) | 0 | 24 | 2 |
| -3 | 0 | 25 | 3 |
| -2 | 1 | 25 | 7 |
| -1 (B) | 2 | 25 | 9 |
| -0,5 | 4 | 25 | 9 |
| 0 | 6 | 26 | 10 |

M1 bulguları düzeltme turu 1'i doğurdu: "Nber" etiket DIGER → yanlış "Başka bankaların…" giriş cümlesi; "???" etiketlenmedi, 1,23 puanla uydurma "[1]".

## M2 (son kod, eşik -4,0) — `handoff/measure/m2-final.json`
Komut: `python3 assistant-api/scripts/measure_answers.py --base-url http://localhost:8091 --threshold-candidates=-4 --out handoff/measure/m2-final.json` (8091 varsayılan eşikle açık; model önce bir mesajla ısıtıldı). Hata 0.

| Kabul (plan "Kabul") | Sonuç |
|----------------------|-------|
| 1. Selamlaşma 37 | 37/37 `SMALL_TALK`, alıntı 0 (listed 13, unlisted 5, question 5, short 3 dahil "Nber", follow-up 1, follow-up-small-talk 10) |
| 2. İsim 3 (S4 = A) | 3/3 `SMALL_TALK`, alıntı 0 |
| 3. Konu dışı 27 | not-in-guide 13/13, off-topic 6/6, gibberish 5/5, follow-up-independent 3/3 bulamadım; alıntı 0; `wrongOpener` 0. "???" → `NO_WORDS`, `topRerankScore` null |
| 4. Hedefli 15 | `targetedOk` 15/15, `wrongOpener` 0. 13'ü kendi giriş cümlesiyle başladı ve alıntısız; 2'si ("Son 5 işlemimi gösterir misin?", "Dolar yarın yükselir mi?") eşik altında kalıp bulamadım aldı. Elle parça karşılaştırması gereken alıntılı cümle yok |
| 5. "Kredi limitim ne kadar" | "Hesabına özel bilgileri (…) göremiyorum. Kart limitiniz gelirinize, başka bankalardaki borçlarınıza ve KKB kayıtlarınıza göre belirlenir [2]." Giriş cümlesinde numara yok; alıntı 1; `citedHeadings` "2.6 Kart limiti: belirleme, artırma, düşürme, geçici limit". qa elle karşılaştıracak |
| 6. Takip soruları 16 | `SMALL_TALK` 0. "Ek kart nasıl alabilirim >> Peki ücretli mi": bulamadım değil, alıntı 2 kez [2] (2.3 Ek kart). "Gold kart aidatı ne kadar? >> Hangi kartlarda yok" bulamadım aldı (M1'de de; yeniden yazma "Gold kart aidatı hangi kartlarda yok?") |
| 7. Golden 100 | `fallen` 0 (sınır 2); `wrongOpener` 0 (sınır 2); `paraphrasedNotFound` 0; `secondPersonSelfReference` 0; 100/100 alıntılı |
| 8. `secondPersonSelfReference` | 0 |

Kısa mesaj gecikmesi (bilgi, uçtan uca `latencyMs`): hazır metin medyanı 624 ms; kısa ama selam olmayan (etiket + arama + model) medyanı 10,8 sn.

**Değerlendirme ekranı ölçümü** (8091, `POST /api/assistant/evals` → 202 `/api/assistant/evals/1`, DONE): RERANK 20→5, `sourceVersions` V1, 100/100 ölçüldü, **recall@5 0,99**, **MRR 0,9125**, kaçan 1, rerank yedek yolu 0, süre 132 sn. Veritabanındaki ilk çalıştırma olduğu için karşılaştırılacak önceki çalıştırma yok. Eşik ve etiket bu ölçüme girmez.

## Doğrulama
- `./mvnw -q compile` → geçti (main kod derleniyor).
- **Temiz `./mvnw verify -Djacoco.haltOnFailure=false`** (depo dışındaki kopyada, `target/` yok) → **BUILD FAILURE, test derleme hatası**. Plan gereği silinen `SmallTalkDetector`'a bağlı 3 test dosyası derlenmiyor: `generation/SmallTalkDetectorTest` (silinecek, yerine `ShortMessageDetectorTest`), `conversation/service/AnswerServiceTest` (sahte `SmallTalkDetector`), `generation/MessageLabelerTest` (`SmallTalkDetector.phraseKey`, T36/T18).
- Aynı kopyada bu 3 dosya çıkarılarak `./mvnw verify -Djacoco.haltOnFailure=false` → **Tests run: 506, Failures: 20, Errors: 0 → BUILD FAILURE**. Hepsi planlanan davranış değişikliğinden geliyor ve tester'ın güncelleyeceği testler:
  - `AssistantApplicationIntegrationTest.givenEmptyDatabase_whenContextStarts_thenFlywayAppliesAllEightMigrations` (artık 9 migration var)
  - `QueryRewriterTest` 2 test (İngilizce şablon ve `USER:`/`ASSISTANT:` geçmişi; T15-T17)
  - `PromptBuilderTest` 4 test (eski sistem talimatı metni; T13)
  - `SendMessageGroundedAnswerIntegrationTest` 7 ve `SendMessageIntegrationTest` 6 test. İlk turdaki kısa sorular (ör. "Kartımı kaybettim, ne yapmalıyım?" 3 kelime + `?`, "Gold kart aidatı ne kadar?") artık önce etiket çağrısı yapıyor. Sahte model bu çağrıya ayrı cevap vermediği için çağrı sayısı, cevap ve `retrievalStrategy` beklentileri tutmuyor; "Teşekkürler" de artık kalıp listesiyle değil, etiketle yakalanıyor. `givenAnswerInProgress…` sahte akışın ilk değerini etiket çağrısı tükettiği için 10 sn bekliyor.
- Depoda (`assistant-api/`) doğrudan `./mvnw verify` eski `target/test-classes` yüzünden yanıltıcı sonuç verdi (526 test, 20 başarısız + 3 hata; silinen sınıf çalışma anında `NoClassDefFoundError`). Bu yüzden temiz kopyanın sonucu esas alındı. tester `./mvnw clean verify` kullanmalı.
- `application.properties` saf ASCII kontrolü gerekmedi (dosya değişmedi).

## Çalışan sistem ve 8090
- **8091:** yeni kodla açık bırakıldı (depo dışındaki kopyadan `SERVER_PORT=8091 DATABASE_PASSWORD=assistant-local java -jar assistant-m2.jar`, eşik varsayılanı -4,0). qa M3'ü aynı sistemde koşabilir: `--base-url http://localhost:8091`. Gerekmiyorsa lead kapatabilir. Bu süreç ajanın oturumundan başlatıldı.
- **8090:** kullanıcının IntelliJ süreci, **eski kodla** çalışıyor. Veritabanı artık V9'da; eski kod V9'u bilmiyor ama Flyway gelecekteki migration'ları varsayılan olarak yok saydığı için açık kalabilir. Yeniden başlatma: IntelliJ'de `AssistantApplication`'ı yeniden çalıştır ya da `cd assistant-api && DATABASE_PASSWORD=assistant-local ./mvnw spring-boot:run -Dmaven.test.skip=true`. Testler tester güncelleyene kadar derlenmediği için `-Dmaven.test.skip=true` gerekli. Yeniden başlatınca ilk etiket çağrısı soğuk modelde 10 sn'yi aşabilir (aşağıda).

## Tester için notlar
- **Etiket çağrısının ayırt edilmesi:** `MessageLabeler` `ChatModel.stream` çağrısında sistem mesajı olarak `prompts/message-label.st`'yi gönderir. Sahte model sistem mesajında "Bir bankanın dijital asistanına gelen müşteri mesajını sınıflandırıyorsun" geçen çağrıya "DIGER"/"SELAM" dönmeli. İlk turdaki kısa sorular da artık bu çağrıyı yapar: `?` ile ≤ 5 kelime ("Kartımı kaybettim, ne yapmalıyım?", "Gold kart aidatı ne kadar?") ve `?` olmadan ≤ 10 kelime. Yeniden yazma çağrısı tek kullanıcı mesajıdır ve "Arama sorusu:" ile biter.
- **Uzun soru fixture'ı:** etiket çağrısını tetiklememek için `?` ile 6+ kelime ya da `?` olmadan 11+ kelime kullan (ör. "Ücret değişiklikleri bana ne kadar önceden bildiriliyor?").
- **`NO_WORDS`:** `ShortMessageDetector.hasNoWords` = `SuggestionTextNormalizer.normalize(...).isEmpty()`; emoji ve noktalama silinir. Yol `ChatModel`, `MessageLabeler`, `QueryRewriter` ve `ContextRetriever`'a hiç dokunmaz; retrieval-api kapalıyken de 503 olmaz (T22). Tur `RAG`/`NONE`, `not_found_answer` true, `message_label` NULL, `history_turns` 0.
- **V9 CHECK testleri (T24):** `ck_conversation_turns_no_words`, `ck_conversation_turns_none_reason`, `ck_conversation_turns_answer_source`.
- **T18 (sızma):** silinen `phraseKey`'in eşdeğeri: `SuggestionTextNormalizer.normalize` → Türkçe harf ASCII (ç ğ ı ö ş ü â î û) → tekrarlanan harf teke. Elle yaptığım kontrol: 28 örnekle 99 liste mesajı (iki turlu satırların ikisi de) ve 100 golden soru arasında eşitlik ya da 3+ kelimede kelime sınırında içerme yok.
- **Loglar (T19):** `Message labeled before search conversationId= label= durationMs= failed=`, `Small talk answered … route=PRE_SEARCH_LABEL` / `route=LABEL`, `Message without words answered conversationId= questionLength=`. `route=PATTERN` artık yok.
- **Kısa mesaj geçmişi:** selamlaşma turları geçmişe girmez (`TurnRepository.findAnsweredTurns` `SMALL_TALK`'ı dışlar; değişmedi). `NO_WORDS` turları geçmişe girer (planda dışlama yok).
- **Gerçek model notu (test değil, bilgi):** Ollama modeli soğukken (servis ya da Ollama yeni açılmışsa) ilk etiket çağrısı `small-talk.label-timeout=10s`'yi aşabilir. Bu durumda mesaj `OTHER` sayılır ve normal yoldan gider. Duman testinde "Selam" bu yüzden model yoluna düştü ve "[1]" aldı. Isınmış modelde etiket 0,2-1 sn sürüyor. qa M3'ten önce bir mesajla modeli ısıtmalı.

## Düzeltme turu 2
Kaynak: `handoff/05-qa-report.md` "## Düzeltme" (qa NEEDS_FIX), lead mesajı (kullanıcı kararları: (b) onaylı, "Kredi kartı limitim" varyantları dahil, E2E kaynağı 10002 lead tarafından silindi), planner "Plan notu 2".

| Madde | Yapılan değişiklik |
|-------|--------------------|
| qa 1 / lead 1 — B1: giriş cümlesi cevabın ortasında ("Gold kart aidatı ne kadar? >> ya kredi kartında") | (a) Talimat: kural 4 başlığının 7 varyantı gerçek modelle denendi. B1'i kapatan tek başlık ("Sadece müşterinin son mesajı şu üç konudan birini soruyorsa …") "Kredi limitim ne kadar"daki genel bilgiyi ve alıntıyı kaybettirdi (kabul 5). Sadece talimatla ikisi birlikte sağlanamadı. (b) Plan notu 2 (planner + kullanıcı onaylı): `AnswerFinalizer.removeMisplacedOpeners`. D5 (2)'den sonra, (3) aralık dışı `[n]`'den önce çalışır; bulamadım tutarsa çalışmaz. İlk cümle hiç silinmez. Diğer cümlelerin `[n]`'leri çıkarılır ve `SuggestionTextNormalizer.normalize` ile normalize edilir; bu hâl bir giriş cümlesinin özünü içeriyorsa cümle, arkasındaki `[n]`'ler ve önündeki boşlukla birlikte silinir. Öz, properties'teki giriş cümlesinin aynı normalizasyonla ilk kelimesi atılmış hâlidir; kodda metin yok. Cümle sonu `[.!?]+` ve arkasındaki `[n]`'ler. `FinalAnswer.misplacedOpenersRemoved`; `AnswerStreamer` log `INFO Misplaced opener removed conversationId={} removedCount={}`. Plan notu 2'deki T28/T29 örneklerinin hepsi ve gerçek B1 cevabı depo dışında geçici bir testle doğrulandı (test depoya yazılmadı). Gerçek modelle B1 5/5 temiz |
| lead 2 — "Kredi kartı limitim ne kadar?" genel bilgi almıyor (TASK kural 7) | Kural 4'ün başlığı ve maddeleri **plandaki metinle aynı kaldı**. Sadece son cümle değişti; 3 varyant denendi (aşağıda). Liste: `personal-info-with-info`'ya "Kredi kartı limitim ne kadar?", "Kart limitim kaç?", "Ek hesap limitim ne kadar?" eklendi (etiket örnekleriyle çakışma yok) |
| qa 2 / lead 3 — ölçüm betiği | `openerAnywhere`: üç giriş cümlesinin özü (ör. "özel bilgileri bakiye limit borç başvuru durumu hesap hareketleri göremiyorum") `done.answer`'ın herhangi bir yerinde geçiyorsa ve satırın kategorisine ait değilse sayılır. Bütün kategoriler, golden ve takip soruları dahil; "Hesabınıza" gibi farklar yakalanır. Özet golden ve listede sayı + metin, kategori başına sayı verir. Geçmiş M2 ve M3 koşularında tam olarak B1 satırını (1) buldu. Ek alan `openerWithCitedInfo`: kendi giriş cümlesi + alıntı var |
| qa Nit / lead 4 | `AnswerService` sınıf Javadoc'u yeniden kırıldı |
| qa 3 / lead 5 | Bu bölüm |
| `assistant-api/CLAUDE.md` | `AnswerFinalizer` adımı, kural 4 notu, `openerAnywhere`/`openerWithCitedInfo`, 202 mesaj, ölçüm öncesi tek kaynak kontrolü |

### Son `prompts/rag-system.st` (kural 4'ün son satırı dışında plandaki metin)
Değişen tek satır (kural 4'ün son satırı):
- Plan: "Bu ilk cümleye numara yazma. <parcalar> içinde konuyla ilgili genel bir bilgi varsa (bu bilginin nereden görüleceği, nasıl değiştirileceği, hangi kanaldan yapılacağı) ardından onu kural 2'ye göre numarasıyla ekle; yoksa başka bir şey yazma."
- Son: "Bu ilk cümleye numara yazma. <parcalar> içinde konuyla ilgili genel bir bilgi varsa (bu bilginin nasıl belirlendiği, nereden görüleceği, nasıl değiştirileceği, hangi kanaldan yapılacağı) cevabı bu ilk cümleyle bitirme, ardından o bilgiyi kural 2'ye göre numarasıyla ekle; yoksa başka bir şey yazma."

Kural 4 başlığı ("4. Sadece şu üç konuda kural 3 yerine bu kural uygulanır; başka hiçbir mesajda bu üç cümleyi kullanma:") **değişmedi**. Planner'ın Plan notu 1'deki başlık önerisi ("cevabın başında, ortasında ya da sonunda") kabul 5'i bozduğu için kullanılmadı; B1'i silme adımı kapatıyor.

Kişisel bilgi varyantlarının deneme sonuçları (tek kaynaklı bilgi bankası, "giriş cümlesi + alıntılı bilgi" / deneme):
| Son satır | Kredi limitim | Kredi kartı limitim | Kart limitim kaç? | Ek hesap limitim |
|-----------|---------------|---------------------|-------------------|------------------|
| Plan metni | 2/2 | 0/2 | 0/1 | 0/1 (bilgi var, numarasız) |
| Maddeye "ardından … ekle" eki | 2/2 | 0/2 | 0/1 | 0/1 |
| Biçim örneği eki | 2/2 | 1/2 | 0/1 | 0/1 |
| **"cevabı bu ilk cümleyle bitirme" (seçilen)** | 2/2 | 2/2 | 0/1 | 0/1 (bilgi var, numarasız) |

Not: aynı istek temp 0'da bile iki koşuda farklı cevap verebildi; oranlar küçük örneklemdir.

### Ortam
- Fazladan kaynak 10002 "E2E Rehber 1793799557640" (13:39'da eklenmiş, rehber 2026.10'un ikinci kopyası) parçaları çift getiriyordu; bunu ben buldum ve lead'e bildirdim. Lead kullanıcı onayıyla sildi. Son ölçüm tek kaynakla (10001, 154 parça) koşuldu.
- 8090'a dokunulmadı (kullanıcı yeni kodla yeniden başlatmış). Ölçüm 8091'de: depo dışındaki kopyadan `SERVER_PORT=8091 DATABASE_PASSWORD=assistant-local java -jar assistant-current.jar`. Süreç açık; 8090 artık bu turun kodunu içermiyor. Kullanıcı 8090'ı yeniden başlatmalı: IntelliJ'de `AssistantApplication`'ı yeniden çalıştır ya da `cd assistant-api && DATABASE_PASSWORD=assistant-local ./mvnw spring-boot:run`.

### Son ölçüm — `handoff/measure/m2-final-r2.json`
Komut: `python3 assistant-api/scripts/measure_answers.py --base-url http://localhost:8091 --threshold-candidates=-4 --out handoff/measure/m2-final-r2.json`. 202 mesaj (golden 100 + liste 102), eşik -4,0, hata 0.

| Ölçüt | Sonuç |
|-------|-------|
| Golden 100 | `fallen` **0** (sınır 2); giriş cümlesiyle başlayan (`goldenStartsWithOpener`/`wrongOpener`) **0** (sınır 2); `openerAnywhere` **0**; 100/100 alıntılı; `paraphrasedNotFound` 0 |
| Yanlış yerde giriş cümlesi (`openerAnywhere`, bütün kategoriler, takip soruları dahil) | `done.answer`'da **0**; akan metinde (`streamedText`) de 0. Bu koşuda silme adımı hiç devreye girmedi, B1 talimatla da temiz |
| B1 "Gold kart aidatı ne kadar? >> ya kredi kartında" | Giriş cümlesi yok; 3 alıntılı cevap (2 Kartlar · Sık sorulanlar, 2.2, 2.3) |
| Selamlaşma 37 + isim 3 | 37/37 ve 3/3 `SMALL_TALK`, alıntı 0 |
| Konu dışı 27 | 27/27 bulamadım, alıntı 0, `wrongOpener` 0, `openerAnywhere` 0; "???" `NO_WORDS` |
| Hedefli (personal-info 9, other-banks 3, investment-advice 3, personal-info-with-info 4) | `targetedOk` 19/19, `wrongOpener` 0. "Son 5 işlemimi gösterir misin?" ve "Dolar yarın yükselir mi?" eşik altında kaldı, bulamadım aldı. "Kumbara hesabımda kaç lira birikti?" giriş cümlesi + 3.3 alıntısı aldı |
| Kabul 5 "Kredi limitim ne kadar" | Giriş cümlesi + "Kart limitiniz geliriniz, diğer bankalardaki borçlarınız ve KKB kayıtlarınıza göre belirlenir [2]." (2.6) ✓ |
| Kişisel bilgi varyantları (`openerWithCitedInfo`) | "Kredi kartı limitim ne kadar?" **2/2** (personal-info ve personal-info-with-info satırları, 2.6). "Kart limitim kaç?" **0/1** (sadece giriş cümlesi). "Ek hesap limitim ne kadar?" **0/1**: bilgi var ("Ek Hesap limiti … 1.000 TL ile 250.000 TL arasında belirlenir.") ama model numarayı giriş cümlesinin arkasına koydu; mevcut D5 (2) adımı onu sildi ve bilgi cümlesi numarasız kaldı. Toplam: limit varyantlarında 3/5 tam, 1/5 numarasız bilgi, 1/5 sadece giriş cümlesi |
| Takip soruları 16 | `SMALL_TALK` 0, `openerAnywhere` 0. "Ek kart nasıl alabilirim >> Peki ücretli mi" alıntılı (2.3). "Gold kart aidatı ne kadar? >> Hangi kartlarda yok" yine bulamadım aldı |
| `secondPersonSelfReference` | 0 |

### Doğrulama
- `./mvnw -q compile` → geçti.
- Depo dışındaki kopyada (tester'ın güncel testleriyle) **`./mvnw clean verify`** → **Tests run: 600, Failures: 4, Errors: 1 → BUILD FAILURE**. Coverage eşiği test hatası yüzünden değerlendirilmedi.
  - `MessageLabelerTest.givenLabelPromptMeasurementListAndGoldenSet_…` (Error): sadece kopyada `../docs/rehber/golden/…` yolu olmadığı için. Golden dosyası kopyanın yanına konunca `./mvnw test -Dtest=MessageLabelerTest` geçti; yeni liste satırları ve etiket örnekleri çakışmıyor. Depoda bu sorun yok.
  - `AnswerFinalizerTest.givenOpenerNotAtStart_whenFinalize_thenCitationsUnchanged` ve `…givenOpenerCitationOutsideRangeAndOpenerNotFirstSentence_whenFinalize_thenOnlyOutOfRangeRemoved`: ikisi de ilk cümle dışındaki giriş cümlesinin **kalmasını** bekliyor. Plan notu 2 ile bu cümle artık siliniyor (T28 bu davranışı test ediyor). Tester'ın güncellemesi gerekir.
  - `PromptBuilderTest.givenTwoChunks_whenBuildPrompt_thenSystemIsLabTextAndUserHasNumberedChunksThenQuestion` ve `…givenChunkContainingInstruction_…SystemUnchanged`: sistem talimatını birebir karşılaştırıyor; kural 4'ün son satırı değişti (yukarıda).

### Tester için notlar (bu tur)
- **T13 / PromptBuilderTest:** kural 4 başlığı değişmedi; "başka hiçbir mesajda bu üç cümleyi kullanma" ifadesi aynı kaldı (planner'ın önerdiği yeni beklenti gerekmiyor). Değişen tek ifade: "ardından onu kural 2'ye göre numarasıyla ekle" → "cevabı bu ilk cümleyle bitirme, ardından o bilgiyi kural 2'ye göre numarasıyla ekle"; parantez içine "bu bilginin nasıl belirlendiği," eklendi. Birebir sistem metni bekleyen iki test bu satıra göre güncellenmeli.
- **T28-T30 (Plan notu 2):** `AnswerFinalizer.finalizeRagAnswer`; sayaç `FinalAnswer.misplacedOpenersRemoved()` (yeni record bileşeni; `new FinalAnswer(...)` artık 5 argüman alıyor). Log `Misplaced opener removed conversationId= removedCount=`.
- Cümle bölme `[.!?]+` ile yapılıyor; "1.450 TL" gibi sayılar da cümleyi böler. Silme sadece özü içeren parçayı etkiler; ilk parça hiç silinmez.

STATUS: DONE
