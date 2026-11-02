TASK: TASK-010
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline`, `sse-streaming`. Transaction ve eşzamanlılık için plandaki kurallar uygulandı: dış çağrı transaction dışında, `getHistory` kısa readOnly tx, kilit yeniden yazmadan önce alınıyor. Bunun için ek skill yüklemedim.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Migration V2 (K6, Kararlar #11) | `db/migration/V2__add_query_rewrite_to_turns.sql` | `query_rewrite_status` önce nullable eklendi, eski turlar `DISABLED` yapıldı, sonra NOT NULL ve CHECK (`ck_conversation_turns_query_rewrite_status`) eklendi. `query_rewrite_latency_ms int` null olabilir. `history_turns int NOT NULL DEFAULT 0 CHECK (>= 0)`. Lokal dev veritabanında uygulandı: 7 eski tur `DISABLED` oldu |
| K1, K4 ayarları | `config/AssistantProperties` (`QueryRewrite(enabled, @NotNull timeout, @Min(1) @Max(1000) maxLength)`, `History(@Min(1) @Max(10) maxTurns)`, ikisi de `@Valid @NotNull`), `application.properties` | `assistant.query-rewrite.enabled=${QUERY_REWRITE_ENABLED:true}`, `timeout=20s` (kart soru 4 = B), `max-length=1000`, `assistant.history.max-turns=3` (kart soru 1 = B). Dosya ASCII (`grep` → 0) |
| `QueryRewriteStatus` (Kararlar #8) | `conversation/domain/entity/QueryRewriteStatus` | `DISABLED, NO_HISTORY, REWRITTEN, FAILED, EMPTY` |
| Tur alanları (K6) | `Turn` (3 kolon + getter), `TurnValues` (alanlar `searchQuery`'den hemen sonra), `TurnService.recordTurn`, `TurnDraft` | `TurnDraft(conversationId, question, searchQuery, queryRewriteStatus, queryRewriteLatencyMs, historyTurns, chatModel, receivedAt, startNanos, context)` |
| Geçmiş (K4, G1, Kararlar #2, #3) | `TurnRepository.findAnsweredTurns` (JPQL, `errorCode is null and answer is not null order by turnNo desc`, `Pageable`), `ConversationService.getHistory` (`@Transactional(readOnly = true)`, `PageRequest.of(0, maxTurns)`, `reversed()` ile eskiden yeniye), `generation/HistoryTurn`, `CitationParser.removeCitations` (`\s*\[\d+]` → "", sonra strip) | `ConversationService` constructor'ına `TurnRepository`, `CitationParser`, `AssistantProperties` eklendi |
| Yeniden yazma (K1, K2, K3a, K5, G3, G4, Kararlar #4-7) | `retrieval/service/QueryRewriter`, `QueryRewriteResult`, `prompts/query-rewrite.st` | Önce `enabled` kontrol edilir (`DISABLED`), sonra boş geçmiş (`NO_HISTORY`); bu iki durumda model çağrılmaz, süre `null` olur. Model `chatModel.stream(prompt)` ile çağrılır: `map(text)` → `reduce` → `Mono.timeout(timeout)` → `block()`. Zaman aşımında abonelik iptal edilir. Prompt tek `UserMessage`, sistem mesajı yok. `{history}` her tur için `USER: q\nASSISTANT: a` biçiminde, turlar `\n` ile birleşir. Normalizasyon Kararlar #7'deki sırayla yapılır. Boş sonuç `EMPTY`, `maxLength` üstü sonuç `FAILED`; her `RuntimeException` (bağlantı, zaman aşımı, senkron hata) da `FAILED`. Model çağrıldıysa süre her durumda dolu. WARN logunda sadece `status`, `reason` (exception sınıf adı `Exceptions.unwrap` ile, ya da `TooLong`/`Empty`) ve `durationMs` var |
| Şablon (K3a) | `prompts/query-rewrite.st` | Plandaki lab metninin birebir aynısı, sonda satır sonu yok. **Ek dil satırı eklenmedi**: M0'da 8 sorgunun 8'i de Türkçe çıktı (aşağıda) |
| Akış sırası (Kararlar #9, G5, B19) | `AnswerService.sendMessage` | Sıra: `getConversation` → `tryStart` → `getHistory` → `rewriteQuery` → `retrieveContext(rewrite.searchQuery())` → emitter. Retrieval hatasında kaydedilen draft yeni alanları taşır (B21). INFO log: `Query rewrite conversationId={} status={} historyTurns={} questionLength={} queryLength={} durationMs={}` (sorgu metni yok, K8) |
| Rerank sorgusu (Kararlar #12) | `ContextRetriever` değişmedi | `retrieveContext(searchQuery)` zaten tek sorguyla hem aramayı hem rerank'i yapıyor |
| Cevap prompt'u (Kararlar #1) | `PromptBuilder.buildPrompt(question, history, contextChunks)` | Sıra: `SystemMessage`, her geçmiş tur için `UserMessage(q)` + `AssistantMessage(a)`, son `UserMessage` (`rag-user.st`, değişmedi; içinde müşterinin sorusu var). Geçmiş boşsa çıktı TASK-008'dekiyle aynı. Geçmiş, yeniden yazma kapalıyken de girer |
| SSE (Kararlar #10) | `SourcesEvent` (`searchQuery`, `queryRewriteStatus` `topK`'dan sonra), `DoneEvent` (`queryRewriteLatencyMs` `completionTokens`'dan sonra), `AnswerStreamer` | Alanlar `sse.md`'deki adlarla birebir. `searchQuery` stripped soru ya da modelin sorgusu |
| `assistant-api/CLAUDE.md` | Durum, Teknoloji (ChatModel satırı, ayar listesi, V2, "henüz olmayanlar"dan sorgu yeniden yazma çıktı), Paket yapısı, Komutlar (takip sorusu duman testi, `QUERY_REWRITE_ENABLED=false`, `ASSISTANT_QUERYREWRITE_TIMEOUT=50ms`) | Plan "Açık sorular" 4: kesin anahtar `assistant.query-rewrite.enabled` olarak yazıldı |

### M0: gerçek modelle deneme (qwen3.5:9b, Ollama, `temperature 0`, `think false`; reranker açık, strateji `rerank`)
Her çift yeni bir konuşmada soruldu. İlk turların hepsi `NO_HISTORY`, ikinci turların hepsi `REWRITTEN` çıktı ve hiçbir akışta `error` olmadı.

| # | Tur 1 → Tur 2 | `searchQuery` (Türkçe mi?) | Yeniden yazma ms | Prompt'taki 5 parça (sırayla) | Alıntı |
|---|---|---|---|---|---|
| 1 | Kartımı kaybettim, ne yapmalıyım? → Yenisi kaç günde gelir? | "Kartımı kaybettiysenim, yeni kartım ne zaman (kaç günde) elime ulaşır?" (evet; "kaybettiysenim" dil bilgisi hatası) | 1344 | 2.16 Kayıp ya da çalıntı bildirimi, 2 Kartlar · SSS, **2.18 Kartın yenilenmesi ve teslim süresi**, 2.17 Harcama itirazı, 2.5 Kredi kartı başvurusu | [1], [3] (3 iş günü / 5 iş günü) |
| 2 | Gold kartın yıllık aidatı ne kadar? → Peki Platinum'da? | "Gold ve Platinum kartların yıllık üyelik ücretleri ne kadar?" (evet) | 814 | 2 Kartlar · SSS, **10.1 Kart ücretleri**, 2.2 Kredi kartı türleri, 2 Kartlar · SSS, 2.3 Ek kart | [2] (2.950 TL) |
| 3 | FAST ile en fazla ne kadar gönderebilirim? → Daha fazlasını göndermem gerekirse? | "Tek seferde 100.000 TL'yi aştığım durumlarda, daha fazla para göndermek için ne yapmalıyım?" (evet) | 1748 | **5.3 FAST ve Kolay Adres**, 5 Para transferi · SSS, 6.6 Dijital işlem limitleri, 5 Para transferi · SSS, 5.7 Transfer limitleri ve ücretleri | [1] (üstü EFT) |
| 4 | İhtiyaç kredisi faizi ne kadar? → Erken kapatırsam ücret öder miyim? | "İhtiyaç kredisini erken kapatırken herhangi bir ücret veya ceza ödemem gerekir mi?" (evet) | 1263 | 4 Krediler · SSS, **4.10 Erken ödeme ve erken kapama tazminatı**, 4.3 İhtiyaç kredisi, 10.3 Kredi ücretleri, 4.9 Cayma hakkı | [2] |
| 5 | Konut kredisi faizi ne kadar? → Ekspertiz ücreti ne kadar? | "Sabit faizli konut kredisi alırken ekspertiz ücreti ne kadar?" (evet) | 1155 | **4.5 Konut kredisi**, 4 Krediler · SSS, 10.3 Kredi ücretleri, 4.7 Faiz/KKDF/BSMV, 4 Krediler · SSS | [1], [2] (9.850 TL) |
| 6 | Kumbara hesabının faizi ne kadar? → Ayda kaç kez para çekebilirim? | "Kumbara hesabında ayda kaç kez para çekilebilir?" (evet) | 805 | **3.3 Kumbara hesabı**, 3 Hesaplar · SSS, 3.6 Ortak hesap, 3 Hesaplar · SSS, 3.2 Vadeli mevduat | [1] (ayda 4) |
| 7 | Mobil şifremi unuttum, ne yapmalıyım? → Kartım yoksa ne yapacağım? | "Mobil şifremi unuttum ve elimde kart bulunmuyor, bu durumda ne yapmalıyım?" (evet) | 1974 | 6 Dijital bankacılık · SSS, 2.15 Kart şifresi, **6.3 Şifre sıfırlama**, 8.7 Müşteri hizmetleri, 6.2 İlk giriş | [1] |
| 8 | Almanya'daki oğluma para göndermek istiyorum. → Ücreti ne kadar? | "Almanya'daki oğluma para gönderirken (SWIFT transferi veya Euro gönderimi) işlem ücretleri ve masraf paylaşım seçenekleri (SHA/OUR) ne kadar?" (evet; uzun, 141 karakter) | 3145 | **5.4 SWIFT ile yurt dışına transfer**, 5.7 Transfer limitleri ve ücretleri, 5 Para transferi · SSS, 5.5 Yurt dışından gelen para, 10.4 Transfer ücretleri | [1] |

- **K3b / kart soru 2:** 8 sorgunun 8'i Türkçe çıktı. Önceden onaylı "Write the standalone query in the same language as the follow-up query." satırı **eklenmedi**; şablon lab metniyle aynı. B3'ün beklenen metni de plandaki literal'le aynı kalır.
- Kalın yazılanlar konuyu doğrudan cevaplayan parçalardır (benim değerlendirmem; kesin karşılaştırma qa'nın M1'inde).
- **Kapalı mod (`QUERY_REWRITE_ENABLED=false`) çift 1:** iki tur da `DISABLED`, `searchQuery` = "Yenisi kaç günde gelir?", `queryRewriteLatencyMs` null. Parçalar: 2.18 (1. sırada), 2.16, 2.17, 5 Para transferi · SSS, 5.5. Cevap yine teslim süresini [2] ile verdi. TASK S2 "doğru parçanın gelmemesi beklenen" diyor ama bu çiftte "yenisi … gün" kelimeleri 2.18'i tek başına buldu. Ayrıca geçmiş cevap prompt'unda olduğu için model bağlamı biliyordu. Diğer çiftlerde fark daha belirgin olabilir. Bu karşılaştırma qa'nın (M1) işi; not olarak bırakıyorum.
- **Başarısızlık yolu (gerçek sistem):** `ASSISTANT_QUERYREWRITE_TIMEOUT=50ms` ile çift 1 denendi. Sonuç: `FAILED`, `searchQuery` = soru, `queryRewriteLatencyMs` 60, akış `error` olmadan bitti. WARN logu `reason=TimeoutException durationMs=60` yazdı.
- **Tur kaydı** (dev veritabanı): ikinci turlarda `query_rewrite_status=REWRITTEN`, `query_rewrite_latency_ms` dolu, `history_turns=1`, `search_query` modelin sorgusu. İlk turlarda `NO_HISTORY`, süre null, `history_turns=0`.
- **Log (K8):** `Query rewrite conversationId=9 status=REWRITTEN historyTurns=1 questionLength=16 queryLength=141 durationMs=3145`. Sorgu metni logda yok.
- Denemeden sonra assistant-api ve retrieval-api'yi durdurdum. Ollama, reranker ve dev postgres zaten açıktı, öyle bıraktım.

## Değişen dosyalar
**Yeni:**
- `assistant-api/src/main/resources/db/migration/V2__add_query_rewrite_to_turns.sql`
- `assistant-api/src/main/resources/prompts/query-rewrite.st`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/domain/entity/QueryRewriteStatus.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/retrieval/service/QueryRewriter.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/retrieval/service/QueryRewriteResult.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/generation/HistoryTurn.java`

**Değişen:** `AssistantProperties`, `application.properties`, `Turn`, `TurnValues`, `SourcesEvent`, `DoneEvent`, `TurnRepository`, `ConversationService`, `AnswerService`, `AnswerStreamer`, `TurnDraft`, `TurnService`, `CitationParser`, `PromptBuilder`, `assistant-api/CLAUDE.md`.

**Silinen:** yok. **Yeni dependency:** yok. retrieval-api'ye dokunulmadı.

## Plandan sapmalar
Yok. İki küçük uygulama ayrıntısı planla uyumlu:
- Plan "`Flux.timeout` + `reduce` + `block`" diyor. `Flux.timeout(Duration)` öğeler arası süredir, toplam süre değildir. Kart soru 4'teki 20 sn'nin toplam süre olması için zaman aşımı `reduce`'dan sonra `Mono.timeout` olarak kondu. İptal davranışı aynı: zaman aşımında upstream abonelik iptal edilir.
- `enabled=false` iken konuşmanın ilk turu da `DISABLED` döner (önce `enabled` kontrol ediliyor, B1'deki sıra). `sse.md`'nin `DISABLED` tanımıyla uyumlu: "assistant.query-rewrite.enabled=false; model çağrılmadı".

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı, ana kod temiz derleniyor.
- `cd assistant-api && ./mvnw clean verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE, test derleme hatası (testCompile).** Bu yüzden testler çalışmadı, test sayısı yok. Kırılan yerlerin hepsi `src/test`'te (tester'ın) ve planın değiştirdiği imzalardan kaynaklanıyor; ana kodda hata yok:
  - `support/TestProperties.java:34,43,53`: `AssistantProperties` artık 8 bileşen alıyor (`queryRewrite`, `history` eklendi).
  - `conversation/service/AnswerServiceTest.java:53`: `AnswerService` constructor'ına 3. parametre olarak `QueryRewriter` eklendi.
  - `conversation/service/AnswerStreamerTest.java:60,280`, `ConversationPersistenceIntegrationTest.java:69`, `TurnServiceIntegrationTest.java:41`: `TurnDraft` 10 bileşen alıyor.
  - `generation/PromptBuilderTest.java:31,45,55`: `buildPrompt(question, history, contextChunks)`.
- Not: `clean`'siz `./mvnw verify` artımlı derleme yüzünden eski test sınıflarıyla koşup yanıltıcı `NoSuchMethodError`'lar verdi. Doğru sonuç için tester'ın da `clean verify` kullanması gerekiyor.
- M0: yukarıda (gerçek model, 8 çift + kapalı mod + zaman aşımı yolu).

## Tester için notlar
- **Önce derlemeyi düzelt:** `TestProperties.create/withReranker/withRetrievalApi` yeni iki bileşeni almalı: `new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000)`, `new AssistantProperties.History(3)`. B1 ve B6 için `enabled=false` ve kısa timeout veren yardımcılar gerekecek. `TurnDraft` için yeni sıra: `(…, question, searchQuery, queryRewriteStatus, queryRewriteLatencyMs, historyTurns, chatModel, …)`.
- **Eskiyebilecek diğer testler** (derleme düzelince bakılmalı):
  - `AssistantApplicationIntegrationTest.givenEmptyDatabase_whenContextStarts_thenFlywayAppliesConversationsMigration` artık 2 migration görmeli.
  - `AssistantPropertiesTest` yeni anahtarlar olmadan bağlanıyorsa `@NotNull` yüzünden açılış düşer.
  - `SendMessageIntegrationTest` aynı konuşmaya ikinci soru gönderiyorsa sahte `ChatModel.stream` iki kez çağrılır: önce yeniden yazma, sonra cevap.
- **Seam'ler:**
  - `QueryRewriter(ChatModel, Resource, AssistantProperties)` Spring'siz kurulabilir. Şablon `new ClassPathResource("prompts/query-rewrite.st")` ile yüklenir. Sahte `ChatModel.stream` `Flux.just(ChatResponses…)` / `Flux.error` / `Flux.never().doOnCancel(...)` dönebilir (B5, B6). B6'da `block()` çağıran thread `Mono.timeout` dolunca döner; iptal bayrağı `doOnCancel` ile yakalanır.
  - `ChatModel.stream` senkron exception fırlatırsa da sonuç `FAILED` olur.
  - Normalizasyon `private`, `rewriteQuery` üzerinden test edilir (B7, B8, B9). Etiket kalıbı `^(?i)standalone query:` ve sadece baştaki etiketi siler. Tırnak silme sadece baş ve son eşleşen `"…"` ya da `“…”` çiftinde yapılır.
  - `AnswerService`: `conversationService` mock'u `getHistory` için varsayılan olarak boş liste döner, yani mevcut testler `NO_HISTORY` yolundan gider. `queryRewriter` mock'u stub'lanmazsa `null` döner ve NPE olur; mevcut `AnswerServiceTest` kurulumuna `given(queryRewriter.rewriteQuery(any(), any())).willReturn(new QueryRewriteResult(NO_HISTORY, soru, null))` gerekir.
  - B19: `tryStart` false dönünce `getHistory` ve `rewriteQuery` çağrılmaz.
  - B20: `QueryRewriter`'da `@Transactional` yok. `AnswerService`'te de yok. `getHistory` tx'i model çağrısından önce kapanır.
- **Log yakalama (B17):** INFO satırı `AnswerService`'te, WARN satırı `QueryRewriter`'da. İkisinde de sorgu ve soru metni yok.
- **B4 geçmiş:** `findAnsweredTurns` `turnNo desc` + `PageRequest.of(0, maxTurns)` döner, `getHistory` listeyi `reversed()` ile eskiden yeniye çevirir. `answer` null olan tur (ör. `RETRIEVAL_UNAVAILABLE`) da sorguya girmez.
- **B15 migration:** CHECK adı `ck_conversation_turns_query_rewrite_status`. `history_turns` CHECK'i isimsiz (Postgres `conversation_turns_history_turns_check` adını verir).
- Süreler `System.nanoTime()` ile ölçülüyor; `Clock` iş kuralına girmiyor.

## Düzeltme
—

STATUS: DONE
