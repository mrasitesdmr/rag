TASK: TASK-013
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `spring-transactions`, `jpa-patterns`, `flyway-migrations`, `postgres-patterns`.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Migration V5 (K1d, K2b, K4, B17) | `src/main/resources/db/migration/V5__add_feedback_and_handoffs.sql` | `conversation_turns.helpful` + `feedback_at` (null), `ck_conversation_turns_feedback_pair`, `ck_conversation_turns_feedback_completed`; `handoffs` tablosu planla birebir, `uq_handoffs_conversation UNIQUE (conversation_id)` |
| Config (Kararlar #9, K2c) | `config/AssistantProperties.java`, `application.properties` | `@Valid @NotNull Handoff handoff`, `record Handoff(@NotBlank @Size(max = 40) String queue)`; `assistant.handoff.queue=bireysel` (ortam değişkeni `ASSISTANT_HANDOFF_QUEUE`, relaxed binding). Dosya hâlâ saf ASCII (`grep` → 0) |
| Turn alanları | `conversation/domain/entity/Turn.java` | `@Nullable Boolean helpful`, `@Nullable Instant feedbackAt` + `getHelpful()`, `getFeedbackAt()`; `Turn.record` ikisini null bırakır, değiştirici metot yok |
| Geri bildirim DTO | `conversation/domain/dto/SubmitFeedbackRequest.java` | `@NotNull(message = "must not be null") @Nullable Boolean helpful` |
| K1b, K1d (Kararlar #2) | `conversation/repository/TurnRepository.java` | `@Modifying` JPQL `updateFeedback(turnId, helpful, feedbackAt)`: sadece iki kolon, tek atomik `UPDATE` |
| K1c (Kararlar #1) | `conversation/service/FeedbackService.java` | `@Transactional submitFeedback`: yoksa `NotFoundException("TURN_NOT_FOUND")`, `errorCode != null` ise `ConflictException("TURN_NOT_COMPLETED")`, sonra `updateFeedback(..., Instant.now(clock))`. Log `Feedback submitted turnId={} helpful={}` |
| Kararlar #3 | `conversation/controller/FeedbackController.java` | `POST /api/assistant/turns/{turnId}/feedback`, `consumes` JSON, 204 gövdesiz |
| Aktarma entity/enum | `handoff/domain/entity/Handoff.java`, `HandoffReason.java` | `conversationId`/`turnId` düz `long` kolon, ilişki yok; `reason` `@Enumerated(STRING)`; `protected` no-arg constructor + getter'lar, factory yok |
| Aktarma DTO'ları | `handoff/domain/dto/RequestHandoffRequest.java`, `HandoffResponse.java` | `RequestHandoffRequest(@NotNull Long turnId, @NotNull HandoffReason reason)`; `HandoffResponse(conversationId, turnId, reason, queue, createdAt)` + `from(Handoff)`; JSON alan sırası openapi ile aynı |
| K2b (Kararlar #4) | `handoff/repository/HandoffRepository.java` | `findByConversationId`; native `insertIfAbsent` (`INSERT … ON CONFLICT (conversation_id) DO NOTHING`) |
| K2a, K2b, K2c (Kararlar #5, #6) | `handoff/service/HandoffService.java`; `TurnRepository.findInConversation` | Tek transaction: `CONVERSATION_NOT_FOUND` → `TURN_NOT_FOUND` (JPQL `t.id = :turnId and t.conversation.id = :conversationId`) → `errorCode != null || !isNotFoundAnswer()` → `HANDOFF_NOT_ALLOWED` → `insertIfAbsent` → `findByConversationId` → `HandoffResponse.from`. Log `Conversation handed off conversationId={} turnId={} queue={} reason={} created={}` (`turnId` = kaydın turu) |
| K2c (Kararlar #7) | `handoff/controller/HandoffController.java` | `POST /api/assistant/conversations/{conversationId}/handoff`, `consumes` JSON, `ResponseEntity.accepted().body(...)`, `Location` yok |
| K2d (soru 1 = A) | — | `sendMessage` değişmedi |
| Paket yapısı | `handoff/{controller,service,repository,domain/entity,domain/dto}/package-info.java` | Hepsi `@NullMarked` |
| `assistant-api/CLAUDE.md` | `assistant-api/CLAUDE.md` | Durum satırı; hata kodları (`TURN_NOT_FOUND`, `TURN_NOT_COMPLETED`, `HANDOFF_NOT_ALLOWED`); V5; `handoff.queue`; paket yapısı (`FeedbackController`, `FeedbackService`, `SubmitFeedbackRequest`, `handoff/`); "Komutlar"a geri bildirim + aktarma duman testi; "Henüz olmayanlar"dan geri bildirim ve temsilciye aktarma çıkarıldı |

## Değişen dosyalar
Yeni:
- `assistant-api/src/main/resources/db/migration/V5__add_feedback_and_handoffs.sql`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/controller/FeedbackController.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/service/FeedbackService.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/domain/dto/SubmitFeedbackRequest.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/handoff/controller/HandoffController.java` + `package-info.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/handoff/service/HandoffService.java` + `package-info.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/handoff/repository/HandoffRepository.java` + `package-info.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/handoff/domain/entity/Handoff.java`, `HandoffReason.java` + `package-info.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/handoff/domain/dto/RequestHandoffRequest.java`, `HandoffResponse.java` + `package-info.java`

Değişen:
- `assistant-api/src/main/java/com/rasitbank/assistant/config/AssistantProperties.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/domain/entity/Turn.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/repository/TurnRepository.java`
- `assistant-api/src/main/resources/application.properties`
- `assistant-api/CLAUDE.md`

Silinen: yok. Eklenen dependency: yok.

## Plandan sapmalar
Yok. Kod plana birebir uyuyor.

**Sözleşme sorusu (çözüldü):** openapi 400 açıklamaları "`helpful` JSON boolean değilse / `turnId` tam sayı değilse → `BAD_REQUEST`" diyordu. Jackson 3 / Boot 4 varsayılanıyla ölçülen davranış farklıydı: `{"helpful":"true"}` ve `{"helpful":1}` → true, 204; `{"turnId":"12"}` ve `{"turnId":12.5}` → 12 olarak kabul. Alana `@JsonFormat(lenient = OptBoolean.FALSE)` eklemek bunu değiştirmedi; deneme koda girmedi. Lead'e iletildi (planner kapalıydı). Lead'in kararı **(A)**: kod aynı kalır, sözleşme metni yumuşatılır. planner-3 `openapi.yaml`'ı ölçülen davranışa göre güncelledi (`01-plan.md` "## Düzeltme turu 1"):
- Eksik ya da null alan → `VALIDATION_FAILED`.
- Dönüştürülemeyen değer, bozuk JSON, bilinmeyen `reason` ve sayı olmayan path id'si → `BAD_REQUEST`.
- `"true"`/`1` → true ve `"12"`/`12.5` → 12 hata değildir.

Backend kodunda değişiklik yok.

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı.
- **Gerçek repoda `./mvnw verify` test derlemesinde kırılıyor (beklenen, planda tester'a verilmiş):** `./mvnw clean test-compile` → `support/TestProperties.java:[34,16] constructor AssistantProperties … cannot be applied to given types` (5 yer). Yeni `handoff` bileşeni yüzünden `TestProperties` güncellenmeli. Plan "testlerde `AssistantProperties`'i elle kuran yerler güncellenir, tester" diyor; `src/test/**`'e yazamadığım için orayı değiştirmedim.
- Ana kodun mevcut testleri geçtiğini kanıtlamak için `assistant-api`'nin scratch kopyasında (`src/main` gerçek repoyla birebir aynı, `diff -r` ile doğrulandı) sadece şu üç test uyarlaması yapıldı. Bunlar tester'ın yapması gereken değişikliklerin aynısıdır:
  1. `support/TestProperties`: 5 yerde `new AssistantProperties.Handoff("bireysel")` eklendi.
  2. `config/AssistantPropertiesTest.BASE`'e `"assistant.handoff.queue=bireysel"` eklendi. Eklenmezse bu sınıfta 1 failure + 6 error çıkıyor: zorunlu ayar eksik olduğu için context açılmıyor.
  3. `AssistantApplicationIntegrationTest.givenEmptyDatabase_whenContextStarts_thenFlywayAppliesAllFourMigrations`'ta `hasSize(4)` → `hasSize(5)` yapıldı (V5 eklendi; testin adı da güncellenmeli).

  Sonuç: `./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD SUCCESS, Tests run: 262, Failures: 0, Errors: 0, Skipped: 0**. Satır coverage %92 (toplam). `handoff.service` %16'da kalıyor, çünkü yeni kodun testleri yok. Eşik kontrolü bu yüzden kapalıydı; tester ve qa tam `./mvnw verify` ile doğrular.
- Duman testi aynı scratch kopyada geçici bir `@SpringBootTest` ile yapıldı (gerçek HTTP + Testcontainers PostgreSQL; repoya girmedi, silindi):
  - Geri bildirim:
    - `true` → 204, ardından `false` → 204; satırda `helpful=false`, `feedback_at` dolu.
    - `error_code` dolu tur → 409 `TURN_NOT_COMPLETED`; olmayan tur → 404 `TURN_NOT_FOUND`.
    - `{}` ya da `{"helpful":null}` → 400 `VALIDATION_FAILED` + `{field:"helpful", message:"must not be null"}`.
    - Bozuk JSON → 400 `BAD_REQUEST`; `/turns/abc/feedback` → 400 `BAD_REQUEST`; `text/plain` → 415 `UNSUPPORTED_MEDIA_TYPE`.
  - Aktarma:
    - Cevaplı tur ya da `CHAT_MODEL_UNAVAILABLE` turu → 409 `HANDOFF_NOT_ALLOWED`.
    - Başka konuşmanın turu → 404 `TURN_NOT_FOUND`; olmayan konuşma → 404 `CONVERSATION_NOT_FOUND`.
    - `"reason":"OTHER"` → 400 `BAD_REQUEST`; `{}` → 400 `VALIDATION_FAILED` (`turnId` ve `reason`).
    - "Cevap yok" turu → 202 `{"conversationId":1,"turnId":2,"reason":"NO_ANSWER","queue":"bireysel","createdAt":…}`, `Location` başlığı yok.
    - Aynı konuşmanın ikinci "cevap yok" turuyla tekrar istek → 202, aynı gövde (ilk `turnId`, ilk `createdAt`).
    - Başka bir konuşmaya aynı anda 4 istek → 4 × 202, gövdeler aynı; `handoffs`'ta konuşma başına 1 satır.
  - Migration kısıtı: SQL ile `error_code` dolu tura `helpful` yazınca `ck_conversation_turns_feedback_completed` ihlali alınıyor.

## Tester için notlar
- **Önce derleme:** `support/TestProperties` (5 yer, `new AssistantProperties.Handoff("bireysel")`), `AssistantPropertiesTest.BASE` (`assistant.handoff.queue=bireysel`) ve `AssistantApplicationIntegrationTest` (migration sayısı 5, test adı "AllFiveMigrations") güncellenmeli; yoksa `verify` kırık. B14 için `TestProperties`'e kuyruk parametreli bir factory (ör. `withHandoffQueue("kurumsal")`) işini görür.
- **Tur fixture'ı:** `Turn.record(...)` + `TurnValues` ile ya da doğrudan SQL ile yazılabilir. RAG turunda `search_query`, `candidates`, `top_k` dolu olmalı; `LLM_ONLY`/`NONE` turunda bunların hepsi null olmalı (V3 CHECK'leri). Benim kullandığım minimal SQL: `insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status, generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer, latency_ms, error_code, created_at, completed_at) values (?, ?, 'q', 'q', 'DISABLED', 'RAG', 'RERANK', 20, 5, 'm', 'a', ?, 10, ?, now(), now())`.
- **Temizlik:** `truncate conversations, conversation_turns, turn_chunks, handoffs restart identity cascade` (`handoffs` iki tabloya FK'li). Var olan temizlik kodu `handoffs`'u bilmiyorsa `conversations` silinirken FK hatası alınır.
- **Saat:** `FeedbackService` ve `HandoffService` `Clock` bean'ini kullanıyor; `FixedClockTestConfiguration` (`2026-11-02T10:31:05Z`) ile `feedback_at` ve `createdAt` kesin değerle doğrulanabilir.
- **Eşzamanlılık (B7, B13):**
  - Geri bildirimde koşulsuz tek `UPDATE` var, satır kilidiyle sıralanıyor; `answer`, `cited`, `not_found_answer`, `latency_ms` güncellemede hiç yazılmıyor.
  - Aktarmada `INSERT … ON CONFLICT DO NOTHING` var; ikinci transaction birincinin commit'ini bekliyor, sonra READ COMMITTED'de `findByConversationId` kaydı görüyor. Exception yakalanmıyor.
  - Bu testler `@Transactional` olmamalı (commit davranışı test ediliyor).
- **`created` log alanı:** `insertIfAbsent` dönüşü `1` ise `true`. Unit testte (B14) `insertIfAbsent` ve `findByConversationId` mock'lanırsa `findByConversationId` boş dönerse `IllegalStateException` (500) fırlar; mock bir `Handoff` dönmeli. `Handoff`'un public constructor'ı yok (JPA `protected`). Unit testte `mock(Handoff.class)` ya da reflection gerekir; alternatif olarak B14 integration testi olarak yazılabilir (`@SpringBootTest(properties = "assistant.handoff.queue=kurumsal")`).
- **Gevşek JSON (güncel sözleşme):** `{"helpful":"true"}` / `{"helpful":1}` → 204 ve `{"turnId":"12"}` → kabul edilir; bunlar hata değildir. `BAD_REQUEST` testi için çevrilemeyen bir değer kullanılmalı (`"maybe"`, `"abc"`, nesne, dizi).
- Dış model yok: iki uç dil modeline, retrieval'a ya da reranker'a gitmiyor. B16 için mevcut `SendMessageIntegrationTest` kalıbı (sahte `ChatModel`, `RetrievalClient`, `RerankClient`) kullanılabilir.

STATUS: DONE
