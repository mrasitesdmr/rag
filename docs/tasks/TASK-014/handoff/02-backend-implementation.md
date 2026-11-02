TASK: TASK-014
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457` (zorunlu); plana göre `jpa-patterns`, `flyway-migrations`, `java-concurrency` (registry). `spring-boot-openapi-documentation` yüklenmedi; sözleşme uyumu `openapi.yaml` ile alan alan elle karşılaştırıldı (springdoc yok).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Migration V6 (Kararlar #10) | `db/migration/V6__add_conversation_list_index.sql` | `ix_conversations_created_at_id ON conversations (created_at DESC, id DESC)` |
| `pagination/` (Kararlar #9) | `pagination/PageResponse.java`, `PageParameters.java`, `package-info.java` | retrieval-api'dekiyle aynı mesajlar ve sınırlar; kod paylaşılmadı |
| Süren tur (Kararlar #2, K4) | `conversation/service/ActiveTurn.java` (yeni), `ActiveTurnRegistry.java` | `ConcurrentHashMap<Long, ActiveTurn>`; `tryStart(conversationId, question, startedAt)` (`putIfAbsent`), `finish`, `findActiveTurn`, `activeTurns()` (`Map.copyOf`) |
| `AnswerService.sendMessage` | `conversation/service/AnswerService.java` | `tryStart(conversationId, question, receivedAt)`: strip edilmiş soru ve `Instant.now(clock)`. Başka davranış değişmedi |
| Durum (K5, AC1; Kararlar #1) | `conversation/domain/entity/TurnStatus.java`, `conversation/service/TurnStatusResolver.java` | Kural sırası `openapi.yaml` `TurnStatus` ile aynı: `errorCode` → `INCOMPLETE`; `LLM_ONLY` → `UNGROUNDED`; `notFoundAnswer` → `DONT_KNOW`; `helpful == false` → `WRONG`; `cited` dolu → `GROUNDED`; diğer → `UNGROUNDED`. `IN_PROGRESS` sadece okuma servisinde verilir |
| Okuma DTO'ları | `conversation/domain/dto/ConversationSummaryResponse`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`, `TurnChunkResponse` | Alan adları ve sırası `openapi.yaml` ile birebir. `TurnResponse.from(turn, status)`: `contextChunks` = `promptPosition != null` olanlar, `n` artan sırada. `TurnResponse.errorCode` `String`. Detaydaki `handoff` için mevcut `HandoffResponse` kullanıldı |
| Liste sorguları (K2b, Kararlar #4) | `ConversationRepository.findWithTurns`, `findWithTurnsOrIdIn` (+ `countQuery`) | `exists` alt sorgusu, `created_at desc, id desc`. Süren tur yoksa ilki çağrılır, boş `IN` listesi gönderilmez |
| İlk/son tur, detay (Kararlar #5, #6) | `TurnRepository.findFirstAndLastTurns`, `findAllWithChunks` | JPQL plandaki gibi; detay `left join fetch t.chunks`, `turnNo` sırası |
| Aktarma (K3h) | `HandoffRepository.findHandedOffConversationIds` | `Set<Long>`; detayda mevcut `findByConversationId` |
| Okuma servisi (Kararlar #3, #8; K1a, K4, G1, G2) | `conversation/service/ConversationReadService.java` | `@Transactional(readOnly = true)`. Önce registry'nin anlık görüntüsü alınır, sonra veritabanı okunur. Süren tur, son kayıtlı turun `createdAt`'i `startedAt.truncatedTo(MICROS)`'tan önceyse geçerlidir; değilse yok sayılır (G1). `turnCount` = son `turnNo` (+1 geçerli süren tur). `firstQuestion` = 1. tur sorusu, yoksa süren turun sorusu. Detayda `inProgressTurn.turnNo` = kayıtlı tur sayısı + 1. Olmayan konuşma → `NotFoundException("CONVERSATION_NOT_FOUND")`. Log: sadece `DEBUG Conversations listed page= size= returned=`; soru loglanmaz |
| Controller (K2b, B4, B9) | `conversation/controller/ConversationController.java` | `GET /api/assistant/conversations` (`page` varsayılan 0, `@Min 0`; `size` varsayılan 20, `@Min 1`/`@Max 100`; mesajlar `PageParameters`'tan) → `PageResponse.from(...)`. `GET /api/assistant/conversations/{conversationId}` → `ConversationDetailResponse`. Sınıfta `@Validated` yok (plan) |
| `assistant-api/CLAUDE.md` | `assistant-api/CLAUDE.md` | Durum yorumu, V6 (Flyway listesi ve veritabanı satırı), paket yapısı (`ConversationReadService`, `TurnStatusResolver`, `TurnStatus`, `ActiveTurn`, yeni DTO'lar, `pagination/`), "Komutlar"a Konuşmalar duman testi; "Henüz olmayanlar"dan `PageResponse` çıkarıldı |

## Değişen dosyalar
Yeni:
- `assistant-api/src/main/java/com/rasitbank/assistant/pagination/{package-info,PageResponse,PageParameters}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/service/{ActiveTurn,TurnStatusResolver,ConversationReadService}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/domain/entity/TurnStatus.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/conversation/domain/dto/{ConversationSummaryResponse,ConversationDetailResponse,InProgressTurnResponse,TurnResponse,TurnChunkResponse}.java`
- `assistant-api/src/main/resources/db/migration/V6__add_conversation_list_index.sql`

Değişen:
- `conversation/service/ActiveTurnRegistry.java`, `conversation/service/AnswerService.java` (tek satır)
- `conversation/controller/ConversationController.java`
- `conversation/repository/ConversationRepository.java`, `conversation/repository/TurnRepository.java`, `handoff/repository/HandoffRepository.java`
- `assistant-api/CLAUDE.md`

Silinen: yok. Yeni dependency: yok. `application.properties` değişmedi. retrieval-api'ye dokunulmadı.

## Plandan sapmalar
Kodda sapma yok. Plan bilerek uygulandığı için **üç mevcut test kırılıyor**. Hepsi `src/test/**` altında, yani tester'ın işi. Lead'e mesajla bildirildi:
1. `AnswerServiceTest`: 9 yerde `registry.tryStart(CONVERSATION_ID)` çağrılıyor. Plan imzayı `tryStart(long, String, Instant)` yaptı, test **derlenmiyor**.
2. `ConversationControllerTest` (`@WebMvcTest(ConversationController.class)`): controller artık `ConversationReadService` de alıyor. Testin `@MockitoBean ConversationReadService` eklemesi gerekiyor, yoksa context kurulmaz.
3. `AssistantApplicationIntegrationTest.givenEmptyDatabase_whenContextStarts_thenFlywayAppliesAllFiveMigrations`: `hasSize(5)` artık 6 (V6). Testin adı ve beklentisi güncellenmeli.

Bu yüzden repo'da `./mvnw verify` tester bu üç testi güncelleyene kadar test-compile adımında kalır. Doğrulama aşağıdaki gibi ayrı bir kopyada yapıldı.

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı. `LC_ALL=C grep -c '[^ -~]' src/main/resources/application.properties` → `0` (dosya değişmedi).
- `cd assistant-api && ./mvnw clean test-compile` (repo) → **COMPILATION ERROR**, sadece `AnswerServiceTest` (`tryStart` imzası, madde 1). Ardından `./mvnw -q compile` ile `target/classes` yeniden üretildi.
- Geçici kopyada tam doğrulama: `assistant-api/` scratchpad'e kopyalandı (`/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/abd112da-8ff0-401e-a44b-75a7f6247f5a/scratchpad/assistant-api-verify`). `src/main` repo ile aynı. Testlerde sadece şu en küçük uyarlamalar yapıldı: `tryStart(X)` → `tryStart(X, "q", Instant.EPOCH)`, `ConversationControllerTest`'e `@MockitoBean ConversationReadService`, migration sayısı 5 → 6. Komut `./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD SUCCESS, Tests run: 314, Failures: 0, Errors: 0, Skipped: 0**, "All coverage checks have been met" (satır coverage 0.963; yeni kodun testleri olmadan). Repo'daki testlere dokunulmadı.
- Aynı kopyada, sonra silinen geçici bir duman testiyle (Testcontainers, gerçek PostgreSQL) şunlar görüldü:
  - Liste en yeni üstte, tursuz konuşma yok, `totalElements` 2. Sadece süren turu olan konuşma `turnCount 1`, `IN_PROGRESS`, `firstQuestion` = süren soru.
  - Detayda `contextChunks` sadece prompt'a girenler, `n` sırasıyla (`[1]` = vector 7). `inProgressTurn`/`handoff` alan olarak `null` yazılıyor. Tursuz konuşma 200 + `turns []`.
  - G1: kayıtlı turun `createdAt`'i `…05.000001Z`, süren turun `startedAt`'i `…05.000001789Z` iken `inProgressTurn null` ve `turnCount 1`.
  - Hatalar: olmayan id → 404 `CONVERSATION_NOT_FOUND`; `/abc` → 400 `BAD_REQUEST`; `size=101` → 400 `VALIDATION_FAILED` `{field: size, message: "must be between 1 and 100"}`; `page=-1` → `{field: page, message: "must be greater than or equal to 0"}`; `size=abc` → 400 `BAD_REQUEST`.

## Tester için notlar
- **Önce kırılan üç test** (bkz. "Plandan sapmalar"). `AnswerServiceTest`'teki eski `tryStart(id)` çağrıları test kurulumunda süren tur açmak için kullanılıyor; yeni imzada soru ve saat verilir. B15 (`findActiveTurn` → strip edilmiş soru + sabit saat) için `AnswerServiceTest`'teki gerçek `ActiveTurnRegistry` örneği kullanılabilir.
- **`TurnStatusResolver`** durumsuz bir `@Component`'tir; unit testte `new TurnStatusResolver()` yeterli. `Turn`'ün public kurucusu yok. Test turu `Turn.record(conversation, turnNo, TurnValues, chunks)` ile kurulabilir. `helpful` sadece veritabanı güncellemesiyle (`updateFeedback`) değişir, entity'de setter yok; `WRONG`/`GROUNDED` geçişi integration testte `submitFeedback` ya da SQL ile kurulur. Unit testte `helpful` gerekiyorsa `Turn` mock'lanabilir (`getErrorCode`, `getGenerationMode`, `isNotFoundAnswer`, `getHelpful`, `getCited`).
- **Süren tur:** `ActiveTurnRegistry` gerçek bean'dir (`@Component`). Integration testte `@Autowired` ile alınıp `tryStart(id, question, startedAt)` çağrılır. Testler arasında `finish` ile temizlenmeli; aksi hâlde sonraki testin listesine girer (`@AfterEach` içinde `registry.activeTurns().keySet().forEach(registry::finish)`).
- **G1 (B7):** Karşılaştırma `!lastTurn.createdAt.isBefore(startedAt.truncatedTo(MICROS))` ise süren turu yok sayar. Kayıtlı turun `created_at`'ini SQL ile `startedAt`'in mikro saniyesine eşitleyip `startedAt`'e nano ekleyerek kurulur. Sadece son tur (en büyük `turnNo`) karşılaştırılır.
- **`TurnFixtures`** turları `now()` ile yazar ve `cited`'ı doldurmaz (varsayılan `{}`). B2/B3 sırası için `conversations.created_at` SQL ile ayarlanmalı. `cited [1]` için `update conversation_turns set cited = '{1}'`, prompt parçaları için `turn_chunks` satırları (`prompt_position` dolu/boş) SQL ile eklenir.
- **Sıra:** konuşmalar `created_at desc, id desc`. Aynı `created_at`'te büyük id üstte.
- **Liste ve detay aynı registry anlık görüntüsünü kullanmaz**, her istek kendi görüntüsünü alır. Eşzamanlılık noktası sadece G1'dir.
- **Sahteleme:** okuma uçları dil modeline, retrieval-api'ye ya da reranker'a gitmez. Mevcut `PostgresIntegrationTest` (reranker sağlık kontrolü kapalı) ve `FixedClockTestConfiguration` yeterli.
- Controller testi (`@WebMvcTest`): `ConversationReadService` `@MockitoBean`; `listConversations(page, size)` `Page<ConversationSummaryResponse>` döner (`new PageImpl<>(list, PageRequest.of(p, s), total)`).

STATUS: DONE
