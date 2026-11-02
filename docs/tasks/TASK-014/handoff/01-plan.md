TASK: TASK-014
# Plan

## Özet
**Mevcut durum (TASK-013):** `/konusmalar` sadece başlık ve açıklama gösteriyor (`ConversationListPage`); `/konusmalar/:conversationId` rotası yok; yan menüde sayaç yok. assistant-api'de konuşma okuma ucu yok (sadece `createConversation`, `sendMessage`, `submitFeedback`, `requestHandoff`). Tur, akış bitince (`done`/`error`) tek seferde kaydedilir (`TurnService.recordTurn`); akışı süren turun satırı yoktur, sadece `ActiveTurnRegistry` bellekte konuşma kimliğini tutar. Turlar modele giden parçaları `turn_chunks`'ta saklar (bütün adaylar; prompt'a girenlerde `prompt_position` dolu); parça metni saklanmaz.

**Yeni durum (kart soruları önerilen seçeneklerle: 1A, 2A, 3A, 4A, 5A):**
- **assistant-api (değişen servis):**
  - `GET /api/assistant/conversations` (`listConversations`, sayfalı) ve `GET /api/assistant/conversations/{conversationId}` (`getConversationDetail`).
  - Tur durumu (`TurnStatus`) okunurken kayıtlı alanlardan hesaplanır (`TurnStatusResolver`), saklanmaz; geri bildirim değişince durum da değişir.
  - `ActiveTurnRegistry` akışı süren turun sorusunu ve başlama anını da tutar (`ActiveTurn`); liste ve detay onu "işleniyor" olarak gösterir.
  - V6 migration: sadece liste sıralaması için index. Yeni `errorCode` yok.
- **web:** `features/conversation` gerçek liste ve detay; yan menüde Konuşmalar sayacı; `features/preview/model/answerFormat.ts` ve `AnswerMessage` içindeki `Inlines` `shared/turn/`'e taşınır (iki ekran da alıntılı cevap çizer).
- **retrieval-api, SSE (`sse.md`), dil modeli, Docker:** değişmez. Parça penceresi mevcut `getChunk` ucunu ve `shared/chunk/ChunkDialog`'u kullanır.

**Yeniden adlandırma / taşıma (eski → yeni):**
- `web/src/features/preview/model/answerFormat.ts` → `web/src/shared/turn/answerFormat.ts` (içerik aynı; `answerFormat.test.ts` da taşınır, tester).
- `AnswerMessage.tsx` içindeki yerel `Inlines` → `web/src/shared/turn/AnswerText.tsx` (`AnswerText`); `AnswerMessage` onu kullanır, görünüm değişmez.
- `ActiveTurnRegistry.tryStart(long)` → `tryStart(long conversationId, String question, Instant startedAt)`.

**Kart cevapları başka seçenek olursa:**
- Soru 1 = B (bulamadım turunda çip yok): `DONT_KNOW` enum'dan çıkar, `TurnResponse.status` ve `lastTurnStatus` `null` alabilir (sözleşme `[TurnStatus, 'null']`); web çipi çizmez. Soru 1 = C: değerlendirici model gerekir → Soru 4 = B ile aynı ek iş (aşağıda).
- Soru 2 = B (lab gibi): kural 7 kalkar; `RAG` ve bulamadım değilse `GROUNDED` (`cited` boş olsa da). B1'deki `cited []` satırının beklentisi `GROUNDED` olur.
- Soru 3 = B (HATALI kullanılmaz): kural 5 kalkar, `WRONG` enum'dan çıkar; "Hayır" sadece bilgi satırında görünür. Soru 3 = C (yönetici işaretler): yeni uç + kolon + düğme gerekir; plan yeniden yazılır, kart yeniden onaya gider.
- Soru 4 = B (değerlendirici model): yeni prompt şablonu, arka planda tur başına ikinci model çağrısı, sonuç kolonu (migration), "değerlendiriliyor" durumu; plan yeniden yazılır, kart yeniden onaya gider.
- Soru 5 = B (çip yok): `INCOMPLETE` enum'dan çıkar, durum `null`; bilgi satırındaki sebep kalır. Soru 5 = C: `INCOMPLETE` yerine `FAILED` ("HATA", kırmızı) ve `CANCELLED` ("İPTAL", gri; `STREAM_CANCELLED`).
- Bu değişiklikler (C/3C/4B hariç) kartı yeniden onaya göndermez; coder'lar başlamadan plan ve sözleşme güncellenir.

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ortak kaynak `openapi.yaml` (`listConversations`, `getConversationDetail`, `TurnStatus`, `RetrievalStrategy`, `TurnErrorCode`, `ConversationSummaryResponse(Page)`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`, `TurnChunkResponse`; `HandoffResponse` yeniden kullanılır).

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/CLAUDE.md` (paket listesi, V6, `pagination/`, yeni uçların duman testi; "Henüz olmayanlar"dan `PageResponse` çıkar). Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md` (conversation feature, `shared/turn/`, `shared/conversation/`, rota `/konusmalar/:conversationId`, `NavLink` `end`). Yeni paket yok. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**` (MSW varsayılan handler'ları: `listConversations` boş sayfa, `getConversationDetail` fixture), `web/e2e/**`. `answerFormat.test.ts` yeni yerine taşınır. Rapor `handoff/03-test-report.md`.
- **qa:** kabul kriterleri, M1-M5, sözleşme uyumu, lab ile görsel karşılaştırma. Sistemi mevcut komutla yeni kodla kurar: `docker compose -f deployment/docker-compose.yml up --build -d` (dosya değiştirmez). qa tester'a bağlıdır. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** devops. Dockerfile, compose, nginx, ortam değişkeni ve lokal model değişmiyor (yeni uçlar mevcut `/api/assistant/` yönlendirmesinden geçer). retrieval-api koduna kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1a | Örnek (SEED) konuşma yok; liste sadece gerçek konuşmalar | `ConversationReadService.listConversations` (veritabanı + bellekteki süren tur); seed migration yok | B2, B6, F2, M1 |
| K1b | Boş durum için Türkçe metin | `ConversationEmptyState` | F2 |
| K2a | Sütunlar: Konuşma, İlk soru (60), Tur, Durum (son tur), Kanal, Saat | `ConversationTable`; `FIRST_QUESTION_MAX_LENGTH = 60` | F1 |
| K2b | En yeni üstte; `PageResponse` ile sayfalı | `ConversationRepository.findListed*` (`created_at DESC, id DESC`), `pagination/PageResponse` | B2, B3, B4 |
| K2c | Yan menü sayacı gerçek konuşma sayısı | `useConversationCountQuery` (`totalElements`), `MainNav` | F6, F20 |
| K3a | Detay: breadcrumb, başlıkta kimlik, alt başlıkta "Kanal · saat · N tur" | `ConversationDetailPage` (`PageHeader` `parent`) | F7 |
| K3b | Tur: sıra no + soru; farklıysa "Arama sorgusu: …" | `TurnCard` | F8, B8 |
| K3c | Alıntılı cevap | `TurnCard` → `shared/turn/AnswerText` | F11 |
| K3d | Durum çipi, üretim etiketi, rehber sürümü, prompt/cevap token, süre | `TurnCard`, `model/turnLabels.ts` | F12, F13, F10 |
| K3e | Parça tablosu: Prompt [n], Parça (sayfa · sıra no), Vector #, Mesafe, Rerank (sadece rerank), Atıf ✓ | `TurnChunkTable`; backend `contextChunks` (prompt'a girenler, `n` sırasıyla) | F9, B8 |
| K3f | Sadece LLM turunda tablo yerine uyarı | `TurnCard` (`Callout` uyarı) | F10 |
| K3g | Satıra tıklayınca parça penceresi | `TurnChunkTable` → `shared/chunk/ChunkDialog` | F11 |
| K3h | Geri bildirim ve aktarma bilgisi turda görünür (TASK-013 soru 3 = A) | Liste "Temsilcide" çipi; detay `HandoffLine`; tur bilgi satırında "geri bildirim: Evet/Hayır" | F5, F12, F17, B10 |
| K4 | Akışı süren tur "işleniyor" çipiyle | `ActiveTurnRegistry` + `ConversationReadService` (`IN_PROGRESS`, `inProgressTurn`); web polling | B5, B6, B7, F4, F15 |
| K5 | Tur durumu kartta onaylanan kurala göre; belirlenemeyen durum yok | `TurnStatusResolver` (kural sırası `openapi.yaml` `TurnStatus`), `turnStatus.ts` etiketleri | B1, B11, F13 |
| K6 | Konuşma ya da tur bulunamazsa Türkçe mesaj | `ConversationNotFound` (404 ve sayı olmayan id); parça yoksa `ChunkDialog` (`CHUNK_NOT_FOUND`) | B9, F16, F11 |
| S1 | İki turluk konuşma listede en üstte "2 tur · Önizleme" | K2a, K2b | B2, F1, M2 |
| S2 | 2. turda "Arama sorgusu" yeniden yazılmış sorgu; tabloda vector 7, prompt [1] | K3b, K3e | B8, F8, F9, M3 |
| S3 | Rehberde olmayan soruda tur onaylanan kurala göre (BİLMİYOR) | K5 | B1, F13, M4 |
| AC1 | Durum kuralları onaylı ve testli | Kart soru 1-5, `TurnStatusResolver` | B1, B11 |
| AC2 | Liste ve detay gerçek turlardan; mock/seed yok | K1a | B2, M1 |
| AC3 | Görünüm lab'ın sol yarısıyla aynı; build'ler geçer | Lab `.tbl`, `.st.*`, `.turn`, `.turn .q/.rq/.a/.nums`, `.used`, `.callout.w`, `.crumbs`, `.ph`, `.side .cnt` | M5, raporlar |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Akış tam bitmek üzereyken (tur yazıldı, bellekteki kayıt henüz silinmedi) okunan detay/liste | Tur bir kez görünür: kayıtlı tur; süren tur gösterilmez, tur sayısı artmaz | B7 |
| G2 | Konuşma açıldı ama tek sorusu 400/409 aldı ya da hiç soru gelmedi | Listede görünmez, sayaca girmez; adresi elle açılırsa başlık + "Bu konuşmada henüz tur yok." | B2, F16b |
| G3 | Tur hatayla bitti (dil modeli kapalı, bilgi bankası kapalı, müşteri ayrıldı) | Kart soru 5; yarım cevap olduğu gibi, token yoksa token öğeleri yok; RAG turunda parça yoksa "Bu turda modele hiçbir parça gitmedi." | F14, B1 |
| G4 | Parçanın kaynağı sonradan silindi / yeni sürüm yüklendi | Tablo turda saklanan değerlerle çizilir; pencere `CHUNK_NOT_FOUND` alırsa "Parça bulunamadı." | F11 |
| G5 | Sayfa açıkken başka sekmede tur sürüyor / bitiyor | Süren tur varken liste ve detay `CONVERSATION_POLL_INTERVAL_MS` (2000) ile yenilenir, bitince durur | F4 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Durum okunurken hesaplanır** (`TurnStatusResolver.resolve(Turn)`), kolon yok | Durum geri bildirime bağlı ve değişebilir; tek kaynak kayıtlı alanlar | Kolona yazmak: geri bildirimde de güncellemek gerekir, çift kaynak |
| 2 | **Süren tur bellekte kalır:** `ActiveTurnRegistry` `Map<Long, ActiveTurn>`; `ActiveTurn(String question, Instant startedAt)` | Tur akış bitince yazılıyor (TASK-008 tasarımı); başlangıçta satır açmak `TurnService`'i ve kısıtları değiştirir, çöken serviste "sonsuza dek işleniyor" satırları bırakır | Başta `IN_PROGRESS` satırı: kalıcı yetim satır riski, CHECK kısıtlarının hepsi değişir |
| 3 | **Çift görünme önlenir:** konuşmanın kayıtlı bir turunun `createdAt`'i `startedAt.truncatedTo(MICROS)`'tan önce değilse süren tur yok sayılır | `Turn.createdAt` = `draft.receivedAt` = `startedAt`; PostgreSQL mikro saniyeye keser; kayıt `finish`'ten önce commit olur | `turnNo` tutmak: tryStart anında ek sorgu gerekir |
| 4 | **Liste kapsamı:** en az bir kayıtlı turu olan ya da şu an süren turu olan konuşmalar; sayaç = aynı listenin `totalElements` | Tursuz konuşma (400/409 alan ilk soru) "konuşma" değildir; lab'da da tursuz konuşma yok | Bütün konuşmalar: boş satırlar, "İlk soru" boş kalır |
| 5 | **Tur sayısı = `max(turn_no)`** (+1 süren tur) | `turn_no` 1'den ardışık ve konuşmada tekil (V1); ayrı `count` sorgusu gerekmez | `count(*)`: aynı sonuç, ek gruplama |
| 6 | **Detayda turlar sayfasız** | Bir konuşmanın turları elle tek tek sorulur; lab hepsini gösterir | Sayfalı turlar: detay ekranında anlamsız bölünme |
| 7 | **`contextChunks` sadece prompt'a girenler**, `n` sırasıyla; adayların geri kalanı dönmez | Tablo lab'da sadece modele giden 5 parçayı gösterir | 20 adayın hepsi: tabloda olmayan veri |
| 8 | **Okuma servisi ayrı sınıf:** `conversation/service/ConversationReadService` (`listConversations`, `getConversationDetail`) | `ConversationService` akışta kullanılıyor; okuma tarafı `HandoffRepository` ve registry'ye de bağlı, ayrı test edilir | `ConversationService`'e eklemek: akış testlerinin kurulumunu büyütür |
| 9 | **`pagination/PageResponse` + `PageParameters` assistant-api'de ilk kez açılır** (retrieval-api'deki ile aynı mesajlar) | `backend-conventions` 6; servisler kod paylaşmaz | `Page<T>` dönmek: kural dışı |
| 10 | **V6: `ix_conversations_created_at_id ON conversations (created_at DESC, id DESC)`** | Liste sıralaması; tablo zamanla büyür | Index'siz: küçük veride fark yok ama sıralama her istekte tam tarama |
| 11 | **web: `conversationKeys` `shared/conversation/conversationKeys.ts`'te** | Önizleme akış bitince sayaç ve listeyi yeniler; feature'lar birbirini import edemez | Önizleme'nin `features/conversation`'ı import etmesi: kural dışı |
| 12 | **Sayaç sorgusu:** `GET /api/assistant/conversations?page=0&size=1` → `totalElements`; yüklenene kadar ya da hatada rozet yok | Ayrı sayım ucu açmadan aynı kural | Ayrı `/count` ucu: sözleşme büyür |
| 13 | **Polling:** liste herhangi bir satır `IN_PROGRESS` iken, detay `inProgressTurn` doluyken `refetchInterval` 2000 ms; aksi `false` | Süren tur bitince ekran kendiliğinden güncellenir (`frontend-conventions` 8) | WebSocket: kapsam dışı |
| 14 | **Konuşma kimliği lab biçiminde `cnv_<id>`** (adres `/konusmalar/<id>`) | Lab görünümü; değer gerçek kimlik | Çıplak sayı: lab'dan ayrışır |
| 15 | **Saat:** bugünse `HH:mm`, değilse `dd.MM.yyyy HH:mm` (Europe/Istanbul) | Lab tek gün gösteriyordu; gerçek veri günler boyunca birikir | Sadece saat: farklı günler karışır |
| 16 | **Üretim etiketi kayıtlı değerlerden:** `Sadece LLM` / `RAG · top-{topK}` / `RAG · rerank {candidates}→{topK}` / `RAG · reranker DOWN` | Varsayılanla lab metniyle aynı; ayar değişirse gerçeği gösterir | Sabit "20→5": ayar değişince yanlış |
| 17 | **Rehber sürümü = turun parçalarının sürümleri** (tekrarsız, virgülle); parça yoksa öğe gizli | Sürüm turda saklı (`turn_chunks.version`); Sadece LLM turunda rehber kullanılmadı | Bugünkü sürüm: tur başka sürümle cevaplanmış olabilir |
| 18 | **Mesafe 4, rerank 3 ondalık** (`formatDecimal`), token binlik ayraçlı (`formatInteger`), süre `formatDuration` | Lab `dec(...,4)`, `dec(...,3)`, `nTr`, `msTxt` | — |
| 19 | **"Tur bulunamazsa":** ayrı tur adresi yok; turlar konuşmayla gelir. Konuşma yoksa sayfa mesajı, turun parçası yoksa pencere mesajı | Lab'da tur adresi yok | Yeni tur ucu/adresi: kapsam dışı |
| 20 | **`answerFormat` ve alıntı çizimi `shared/turn/`'e taşınır** | İki feature aynı alıntılı cevabı çizer (`frontend-conventions` 4) | Kopya: iki yerde bakım |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumuna TASK-014; `Conversations` tag açıklaması.
- Yeni `GET /api/assistant/conversations` (`listConversations`): `page`, `size`; 200 `ConversationSummaryResponsePage`; 400 `VALIDATION_FAILED` (`page`/`size`, retrieval-api ile aynı mesajlar) / `BAD_REQUEST`; 500.
- Yeni `GET /api/assistant/conversations/{conversationId}` (`getConversationDetail`): 200 `ConversationDetailResponse`; 400 `BAD_REQUEST`; 404 `CONVERSATION_NOT_FOUND`; 500.
- `createConversation` açıklaması: tursuz konuşma listede görünmez.
- Yeni şemalar: `TurnStatus` (`IN_PROGRESS`, `INCOMPLETE`, `UNGROUNDED`, `DONT_KNOW`, `WRONG`, `GROUNDED`; kural sırası açıklamada), `RetrievalStrategy`, `TurnErrorCode`, `ConversationSummaryResponse`, `ConversationSummaryResponsePage`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`, `TurnChunkResponse`. `HandoffResponse` detayda yeniden kullanılır.
- Yeni `errorCode` yok. `sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `jpa-patterns`, `spring-jpa-testing`, `flyway-migrations`, `postgres-patterns`, `spring-boot-openapi-documentation` (sadece sözleşme uyumu; springdoc eklenmez), `java-concurrency` (registry), `spring-mvc-testing` (tester).

**Yeni dependency yok.**

### Migration `V6__add_conversation_list_index.sql`
- `CREATE INDEX ix_conversations_created_at_id ON conversations (created_at DESC, id DESC);`

### `pagination/` (yeni, `package-info.java`)
- `PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages)` + `static from(Page<T>)` (`backend-conventions` 6).
- `PageParameters`: `PAGE_MESSAGE = "must be greater than or equal to 0"`, `SIZE_MESSAGE = "must be between 1 and 100"`, `MAX_SIZE = 100`.

### Süren tur
- `conversation/service/ActiveTurn` (record): `String question`, `Instant startedAt`.
- `ActiveTurnRegistry`: `ConcurrentHashMap<Long, ActiveTurn>`; `boolean tryStart(long conversationId, String question, Instant startedAt)` (`putIfAbsent`), `void finish(long conversationId)`, `Optional<ActiveTurn> findActiveTurn(long conversationId)`, `Map<Long, ActiveTurn> activeTurns()` (kopya).
- `AnswerService.sendMessage`: `tryStart(conversationId, question, receivedAt)` (strip edilmiş soru; başka davranış değişmez).

### Durum
- `conversation/domain/entity/TurnStatus` (enum, `openapi.yaml` sırasıyla).
- `conversation/service/TurnStatusResolver` (`@Component`, durumsuz): `TurnStatus resolve(Turn turn)`; kurallar `openapi.yaml` `TurnStatus` açıklamasındaki sırayla (2-7). `IN_PROGRESS` sadece okuma servisinde verilir.

### Okuma
- `conversation/domain/dto/`: `ConversationSummaryResponse`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`, `TurnChunkResponse` (alan adları ve sırası `openapi.yaml` ile birebir; `TurnResponse.errorCode` `String`, `retrievalStrategy`/`generationMode` mevcut enum'lar). `HandoffResponse` `handoff/domain/dto`'dan.
- `ConversationRepository`:
  - `@Query("select c from Conversation c where exists (select t.id from Turn t where t.conversation = c) order by c.createdAt desc, c.id desc") Page<Conversation> findWithTurns(Pageable)` (+ `countQuery`).
  - `findWithTurnsOrIdIn(Collection<Long> conversationIds, Pageable)`: aynısı `or c.id in :conversationIds`. Süren tur yoksa servis ilkini çağırır (boş `IN` listesi gönderilmez).
- `TurnRepository`:
  - `@Query("select t from Turn t where t.conversation.id in :conversationIds and (t.turnNo = 1 or t.turnNo = (select max(t2.turnNo) from Turn t2 where t2.conversation = t.conversation)))") List<Turn> findFirstAndLastTurns(Collection<Long> conversationIds)`.
  - `@Query("select distinct t from Turn t left join fetch t.chunks where t.conversation.id = :conversationId order by t.turnNo") List<Turn> findAllWithChunks(long conversationId)`.
- `HandoffRepository`: `@Query("select h.conversationId from Handoff h where h.conversationId in :conversationIds") Set<Long> findHandedOffConversationIds(Collection<Long>)`; mevcut `findByConversationId`.
- `conversation/service/ConversationReadService` (`ConversationRepository`, `TurnRepository`, `HandoffRepository`, `ActiveTurnRegistry`, `TurnStatusResolver`), `@Transactional(readOnly = true)`:
  - `Page<ConversationSummaryResponse> listConversations(int page, int size)`: `PageRequest.of(page, size)` → konuşma sayfası → `findFirstAndLastTurns` → `findHandedOffConversationIds` → registry. Satır: `turnCount` = son tur `turnNo` (+1 geçerli süren tur), `firstQuestion` = 1. tur sorusu ya da süren turun sorusu, `lastTurnStatus` = geçerli süren tur varsa `IN_PROGRESS`, yoksa son turun durumu. "Geçerli süren tur" = Kararlar #3.
  - `ConversationDetailResponse getConversationDetail(long conversationId)`: konuşma yoksa `NotFoundException("CONVERSATION_NOT_FOUND", …)`; `findAllWithChunks`; her tur `TurnResponse` (`status` resolver'dan; `contextChunks` = `promptPosition != null` olanlar, `promptPosition` artan, `n` = `promptPosition`); `inProgressTurn` = geçerli süren tur → `InProgressTurnResponse(turnNo = kayıtlı tur sayısı + 1, question, startedAt)`; `handoff` = `findByConversationId` → `HandoffResponse.from` ya da `null`.
  - Log: `INFO`'ya gerek yok; `DEBUG Conversations listed page={} size={} returned={}`. Soru metni loglanmaz.
- `ConversationController`: `@GetMapping PageResponse<ConversationSummaryResponse> listConversations(@RequestParam page (default 0, @Min 0), @RequestParam size (default 20, @Min 1, @Max 100))` (`backend-conventions` 6 örneği); `@GetMapping("/{conversationId}") ConversationDetailResponse getConversationDetail(@PathVariable long conversationId)`. Sınıfa `@Validated` eklenmez: Spring MVC'nin yerleşik metot doğrulaması `HandlerMethodValidationException` fırlatır ve mevcut handler bunu `VALIDATION_FAILED`'a çevirir (`@Validated` `ConstraintViolationException`'a döndürür, handler'da karşılığı yok).

### `assistant-api/CLAUDE.md`
Paket yapısına `pagination/`, `ConversationReadService`, `TurnStatusResolver`, `TurnStatus`, `ActiveTurn`; V6; duman testi: `curl -s 'localhost:8090/api/assistant/conversations?page=0&size=20'`, `curl -s localhost:8090/api/assistant/conversations/<id>`. "Henüz olmayanlar"dan `PageResponse` çıkar.

## Frontend planı
Ek skill'ler: `frontend-design` (lab `.tbl`, `.st`, `.turn`, `.callout.w`, `.side .cnt`, boş durum), `frontend-a11y` (tıklanabilir satır, tablo başlıkları), `react-state-management` (polling), `react-testing` (tester).

**Yeni paket yok.**

### shared/
- `shared/turn/answerFormat.ts` (taşındı, içerik aynı). `shared/turn/AnswerText.tsx` (+ `.module.css`, `AnswerMessage`'daki alıntı düğmesi stili): props `{ text: string; contextSize: number | null; onCitation: (n: number) => void }`; paragraf/liste/kalın ve `aria-label="Kaynak {n}"` alıntı düğmeleri. `AnswerMessage` bunu kullanır.
- `shared/conversation/conversationKeys.ts`: `conversationKeys = { all: ['conversations'] as const, list: (params: PageParams) => [...all, 'list', params], count: [...all, 'count'], detail: (conversationId: number) => [...all, 'detail', conversationId] }`.
- `shared/format/formatDateTime.ts`: `formatListTime(iso: string, now: Date): string` (Kararlar #15).
- `features/preview/hooks/useAnswerStream.ts`: `liveConfigKeys.all`'ın invalidate edildiği yerde `conversationKeys.all` da invalidate edilir; ayrıca akış `error` ile bittiğinde.

### features/conversation/
- `model/types.ts` (`openapi.yaml` ile birebir): `TURN_STATUSES`, `TurnStatus`, `RetrievalStrategy`, `TurnErrorCode`, `ConversationSummaryResponse`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`, `TurnChunkResponse`; `HandoffResponse` (aynı alanlar, bu feature'da yeniden tanımlanır; preview'dan import edilmez).
- `model/conversationRules.ts`: `CONVERSATION_PAGE_SIZE = 20`, `CONVERSATION_POLL_INTERVAL_MS = 2000`, `FIRST_QUESTION_MAX_LENGTH = 60`, `DISTANCE_FRACTION_DIGITS = 4`, `RERANK_FRACTION_DIGITS = 3`.
- `model/turnStatus.ts`: `TURN_STATUS_LABELS` (`IN_PROGRESS` → `işleniyor`, `INCOMPLETE` → `TAMAMLANMADI`, `UNGROUNDED` → `DAYANAKSIZ`, `DONT_KNOW` → `BİLMİYOR`, `WRONG` → `HATALI`, `GROUNDED` → `DAYANAKLI`), `turnStatusTone(status): StatusTone` (`GROUNDED`, `DONT_KNOW` → `ok`; `UNGROUNDED`, `WRONG` → `bad`; `IN_PROGRESS` → `run`; `INCOMPLETE` → `neutral`).
- `model/turnLabels.ts`: `conversationLabel(id) → 'cnv_<id>'`; `channelLabel('PREVIEW') → 'Önizleme'`; `generationLabel(turn)` (Kararlar #16); `guideVersionLabel(turn): string | null`; `failureReasonLabel(errorCode)`: `CHAT_MODEL_UNAVAILABLE` → `dil modeline ulaşılamadı`, `RETRIEVAL_UNAVAILABLE` → `bilgi bankasına ulaşılamadı`, `STREAM_CANCELLED` → `cevap yarıda kesildi`, `INTERNAL_ERROR` → `beklenmeyen hata`; `hasDistinctSearchQuery(turn)`.
- `model/conversationTexts.ts`: `EMPTY_TITLE = 'Henüz konuşma yok'`, `EMPTY_TEXT = "Önizleme'de asistana bir soru sorduğunda konuşma burada görünür; her tur modele giden parçalarla birlikte saklanır."`, `EMPTY_ACTION = "Önizleme'ye git"`, `NOT_FOUND_TITLE = 'Konuşma bulunamadı.'`, `BACK_TO_LIST = "Konuşmalar'a dön"`, `LLM_ONLY_NO_CHUNKS = 'Bu turda modele hiçbir parça gitmedi (Sadece LLM).'`, `NO_CHUNKS = 'Bu turda modele hiçbir parça gitmedi.'`, `NO_TURNS = 'Bu konuşmada henüz tur yok.'`, `NO_ANSWER = 'Cevap üretilmedi.'`, `HANDED_OFF_CHIP = 'Temsilcide'`.
- `api/conversationApi.ts`: `listConversations(params: PageParams): Promise<PageResponse<ConversationSummaryResponse>>`, `getConversationDetail(conversationId: number): Promise<ConversationDetailResponse>`.
- `api/conversationQueries.ts`: `useConversationsQuery(params)` (Kararlar #13), `useConversationCountQuery()` (`select: page => page.totalElements`), `useConversationDetailQuery(conversationId)` (Kararlar #13).
- `components/`:
  - `ConversationTable.tsx` (lab `.tbl` + `tr.click`): sütunlar Konuşma (`cnv_<id>`, `td.num`), İlk soru (`shortenText(firstQuestion, 60)`, `td.tx`), Tur, Durum (`StatusBadge` + `handedOff` ise yanında `StatusBadge tone="neutral"` "Temsilcide"), Kanal, Saat (`formatListTime`). Satır tıklama ve Enter ile `/konusmalar/<id>`; satırın içinde konuşma kimliği `Link` (klavye ve ekran okuyucu için).
  - `ConversationEmptyState.tsx`: lab'da karşılığı yok; `Callout` biçiminde başlık + metin + `/onizleme` bağlantısı (`frontend-design`).
  - `TurnCard.tsx` (lab `.turn`): `<h2>` yerine `.q` `{turnNo}. {question}`; `hasDistinctSearchQuery` ise `.rq` "Arama sorgusu: `<code>`"; `.a` `AnswerText` (`contextSize` = `contextChunks.length`, Sadece LLM'de `null`; `answer` null → `NO_ANSWER`); `.nums`: durum çipi, `üretim <b>`, `rehber <b>` (varsa), `prompt <b>N</b> token`, `cevap <b>N</b> token` (null değilse), `süre <b>`, `geri bildirim <b>Evet/Hayır</b>` (varsa), `INCOMPLETE` ise `sebep <b>{failureReasonLabel}</b>`; altında `TurnChunkTable` ya da `Callout` uyarı (`LLM_ONLY` → `LLM_ONLY_NO_CHUNKS`; RAG ve parça yok → `NO_CHUNKS`).
  - `InProgressTurnCard.tsx`: `.q` + `.rq` yok + `.a` "…" + `işleniyor` çipi.
  - `TurnChunkTable.tsx` (lab tablo): başlıklar Prompt, Parça, Vector #, Mesafe, [Rerank], Atıf; satır `[n]`, `s. {pageNumber} · #{chunkNo}`, `vectorRank`, mesafe, rerank (`retrievalStrategy === 'RERANK'` ise sütun), `cited.includes(n)` ise `✓` (`.used`). Satır tıklama/Enter → `onChunkOpen(chunkId)`.
  - `HandoffLine.tsx`: "Temsilciye aktarıldı · {queue} kuyruğu · {formatTime(createdAt)} · sebep: cevap yok".
- `pages/ConversationListPage.tsx`: `PageHeader` (mevcut metin) + tablo / boş durum / `ErrorAlert` / "Yükleniyor…"; `totalPages > 1` ise `Pagination`.
- `pages/ConversationDetailPage.tsx`: id `^\d+$` değilse `ConversationNotFound`; `PageHeader` `section`=`cnv_<id>`, `title`=`cnv_<id>`, `parent` `{label: 'Konuşmalar', to: '/konusmalar'}`, `description` `{channelLabel} · {formatListTime(createdAt)} · {turnCount} tur` (kayıtlı + süren); `HandoffLine`; turlar; `inProgressTurn`; tur yoksa `NO_TURNS`; `CONVERSATION_NOT_FOUND` → `ConversationNotFound` (`NOT_FOUND_TITLE` + `/konusmalar` bağlantısı, `SourceNotFound` kalıbı); diğer hata `ErrorAlert`. Tek `ChunkDialog` durumu (`chunkId | null`), alıntı ve tablo satırı açar.
- `app/App.tsx`: `konusmalar/:conversationId` rotası. `app/MainNav.tsx`: `/konusmalar` `end: false`; `NavItem`'a `count?: number`; Konuşmalar öğesinde `useConversationCountQuery` verisi varsa `<span className={styles.count}>` (lab `.side .cnt`).

### errorMessages.ts
Değişiklik yok (yeni `errorCode` yok; `CONVERSATION_NOT_FOUND` sayfasında `NOT_FOUND_TITLE` kullanılır, `messageFor` metni Önizleme içindir).

## Test senaryoları
Backend: Testcontainers (mevcut kurulum), tablolar her testten önce temizlenir; turlar `TurnFixtures`/`TurnRepository` ile doğrudan yazılır; saat `FixedClockTestConfiguration` (`2026-11-02T10:31:05Z`). Süren tur `ActiveTurnRegistry.tryStart` ile verilir. Web: MSW; fixture konuşma `12`, turlar `31` (cevaplı, `RERANK`, prompt chunk'ları vector 7,1,3,2,9; `cited [1]`), `32` (takip, `searchQuery` farklı).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Durum kuralları | unit `TurnStatusResolverTest` (`@ParameterizedTest`) | `givenTurnFields_whenResolve_thenStatusFollowsRuleOrder` | `error_code CHAT_MODEL_UNAVAILABLE` → `INCOMPLETE`; `LLM_ONLY` + `helpful false` → `UNGROUNDED`; `RAG` bulamadım + `helpful false` → `DONT_KNOW`; `RAG` `cited [1]` + `helpful false` → `WRONG`; `RAG` `cited [1,3]` + `helpful null` → `GROUNDED`; `cited [1]` + `helpful true` → `GROUNDED`; `cited []` bulamadım değil → `UNGROUNDED`; bulamadım + `STREAM_CANCELLED` → `INCOMPLETE` |
| B2 | Liste içeriği ve sırası (S1) | integration `ConversationReadControllerIntegrationTest` | `givenConversationsWithAndWithoutTurns_whenListConversations_thenNewestFirstWithoutEmptyOnes` | konuşmalar 10:00 (2 tur), 10:05 (tursuz), 10:10 (1 tur, aktarılmış) → `content` sırası 10:10, 10:00; `totalElements` 2; 10:00 satırı `turnCount 2`, `firstQuestion` 1. turun sorusu, `lastTurnStatus` 2. turun durumu, `handedOff false`; 10:10 `handedOff true` |
| B3 | Sayfalama | integration | `givenThreeListedConversations_whenPageSizeTwo_thenTwoPages` | `size=2` → `page 0`: 2 öğe, `totalElements 3`, `totalPages 2`; `page=1`: 1 öğe |
| B4 | Parametre hataları | controller `ConversationControllerTest` | `givenInvalidPageOrSize_whenListConversations_thenValidationFailed` (`page=-1`, `size=0`, `size=101`) / `givenNonNumericSize_whenListConversations_thenBadRequest` | 400 `VALIDATION_FAILED`, `violations[0].field` `page`/`size`, mesajlar `must be greater than or equal to 0` / `must be between 1 and 100`; `size=abc` → 400 `BAD_REQUEST` |
| B5 | Süren tur, kayıtlı tur var | integration | `givenRecordedTurnAndActiveTurn_whenRead_thenInProgressShown` | listede `turnCount 2`, `lastTurnStatus IN_PROGRESS`; detayda `inProgressTurn {turnNo 2, question "Yenisi kaç günde gelir?", startedAt}` |
| B6 | Süren tur, kayıtlı tur yok | integration | `givenOnlyActiveTurn_whenListConversations_thenListedWithActiveQuestion` | tursuz konuşma listede: `turnCount 1`, `firstQuestion` süren soru, `IN_PROGRESS`; `finish` sonrası listede yok |
| B7 | Çift görünme yok (G1) | integration | `givenTurnRecordedButRegistryNotFinished_whenRead_thenTurnShownOnce` | kayıtlı turun `createdAt` = `startedAt` (nano kısmı farklı) → `inProgressTurn null`, `turnCount 1`, `lastTurnStatus` kayıtlı turun durumu |
| B8 | Detay alanları (S2) | integration | `givenRerankFollowUpTurn_whenGetConversationDetail_thenPromptChunksInPromptOrder` | 20 aday, prompt'a girenler vector 7,1,3,2,9 → `contextChunks[0]` `{n 1, vectorRank 7, distance 0.3124, rerankScore 5.36}`; 5 öğe; `searchQuery` yeniden yazılmış sorgu; `cited [1]`; `turns` `turnNo` sırasında |
| B9 | Bulunamayan / bozuk id | controller | `givenUnknownConversation_whenGetConversationDetail_thenConversationNotFound` / `givenNonNumericId_whenGetConversationDetail_thenBadRequest` | 404 `CONVERSATION_NOT_FOUND`; 400 `BAD_REQUEST`; `application/problem+json` |
| B10 | Aktarma kaydı | integration | `givenHandedOffConversation_whenGetConversationDetail_thenHandoffIncluded` | `handoff {queue "bireysel", reason "NO_ANSWER", turnId, createdAt}`; aktarılmamışta `handoff null` |
| B11 | Durum geri bildirimi izler | integration | `givenCitedTurn_whenFeedbackChanges_thenStatusChanges` | `submitFeedback false` → detayda `WRONG`; `true` → `GROUNDED` |
| B12 | Sadece LLM turu | integration | `givenLlmOnlyTurn_whenGetConversationDetail_thenNoChunksAndUngrounded` | `generationMode LLM_ONLY`, `retrievalStrategy NONE`, `searchQuery null`, `contextChunks []`, `status UNGROUNDED` |
| B13 | Tursuz konuşmanın detayı (G2) | integration | `givenConversationWithoutTurns_whenGetConversationDetail_thenEmptyTurns` | 200, `turns []`, `inProgressTurn null`, `handoff null` |
| B14 | Registry | unit `ActiveTurnRegistryTest` | `givenActiveTurn_whenTryStartAgain_thenRejectedAndSnapshotKept` / `givenFinishedTurn_whenFindActiveTurn_thenEmpty` | ikinci `tryStart` false, ilk soru kalır; `finish` sonrası boş |
| B15 | Akış soruyu kaydeder | unit `AnswerServiceTest` | `givenQuestionWithSpaces_whenSendMessage_thenRegistersStrippedQuestionAndReceivedAt` | `findActiveTurn` → `question` "Kartımı kaybettim, ne yapmalıyım?" (boşluksuz), `startedAt` sabit saat |
| F1 | Liste satırı (S1) | component `ConversationListPage.test.tsx` | `givenConversations_whenRendered_thenRowShowsLabCells` | satır: `cnv_12`, ilk soru 70 karakterlik sorunun ilk 59 karakteri + `…`, `2`, `DAYANAKLI`, `Önizleme`, saat (sistem saati `vi.setSystemTime` aynı gün → `13:31`; başka gün → `01.11.2026 13:31`) |
| F2 | Boş durum (K1) | component | `givenNoConversations_whenRendered_thenEmptyStateWithPreviewLink` | `Henüz konuşma yok`, bağlantı `Önizleme'ye git` → `/onizleme`; tablo yok |
| F3 | Satır tıklama | component | `givenRow_whenClickedOrEnter_thenNavigatesToDetail` | adres `/konusmalar/12` |
| F4 | İşleniyor ve polling (K4, G5) | component (MSW sırayla `IN_PROGRESS`, sonra `GROUNDED`) | `givenInProgressConversation_whenPolled_thenChipUpdatesAndPollingStops` | önce `işleniyor`, sonra `DAYANAKLI`; sonrasında yeni istek gelmez (istek sayısı 2) |
| F5 | Temsilcide çipi | component | `givenHandedOffConversation_whenRendered_thenTemsilcideChip` | satırda `Temsilcide` |
| F6 | Sayaç (K2c) | component `MainNav.test.tsx` | `givenThreeConversations_whenNavRendered_thenCountShown` | Konuşmalar bağlantısında `3`; hata cevabında sayı yok |
| F7 | Detay başlığı | component `ConversationDetailPage.test.tsx` | `givenConversation_whenRendered_thenBreadcrumbTitleAndSubtitle` | breadcrumb `Konuşmalar` bağlantısı + `cnv_12`; `h1` `cnv_12`; `Önizleme · 13:31 · 2 tur` |
| F8 | Arama sorgusu (S2) | component | `givenFollowUpTurn_whenRendered_thenSearchQueryLineOnlyWhenDifferent` | 2. turda `Arama sorgusu: Kaybolan kartın yenisi kaç günde gelir?`; 1. turda yok |
| F9 | Parça tablosu (S2) | component | `givenRerankTurn_whenRendered_thenChunkTableColumnsAndValues` | ilk satır `[1]`, `s. 19 · #31`, `7`, `0,3124`, `5,360`, `✓`; atıfsız satırda ✓ yok; `TOP_K` turunda `Rerank` başlığı yok |
| F10 | Sadece LLM | component | `givenLlmOnlyTurn_whenRendered_thenWarningInsteadOfTable` | `Bu turda modele hiçbir parça gitmedi (Sadece LLM).`; tablo yok; `üretim Sadece LLM`; `DAYANAKSIZ` |
| F11 | Parça penceresi (K3g, G4) | component | `givenChunkRowOrCitation_whenClicked_thenChunkDialogOpens` / `givenDeletedChunk_whenOpened_thenNotFoundMessage` | satıra ve cevaptaki `Kaynak 1`'e tıklayınca `GET /api/workspaces/42/chunks/151` → pencere başlığı `2.16 Kayıp ya da çalıntı bildirimi`; 404 `CHUNK_NOT_FOUND` → `Parça bulunamadı.` |
| F12 | Bilgi satırı | component | `givenAnsweredTurn_whenRendered_thenNumbersLine` | `üretim RAG · rerank 20→5`, `rehber 2026.10`, `prompt 1.234 token`, `cevap 87 token`, `süre 3,42 sn`, `geri bildirim Hayır` |
| F13 | Durum etiketleri (K5, S3) | unit `turnStatus.test.ts` (`it.each`) | `givenStatus_whenLabelled_thenLabText` | 6 enum → `işleniyor`, `TAMAMLANMADI`, `DAYANAKSIZ`, `BİLMİYOR`, `HATALI`, `DAYANAKLI` |
| F14 | Hatalı tur (G3) | component | `givenFailedTurn_whenRendered_thenIncompleteChipReasonAndPartialAnswer` | `TAMAMLANMADI`, `sebep dil modeline ulaşılamadı`, yarım cevap metni görünür, `token` öğeleri yok; `RETRIEVAL_UNAVAILABLE` turunda `Cevap üretilmedi.` ve `Bu turda modele hiçbir parça gitmedi.` |
| F15 | Süren tur (K4) | component | `givenInProgressTurn_whenRendered_thenProcessingCard` | `3. Peki ek kart?`, `işleniyor`, `…` |
| F16 | Bulunamadı (K6) | component | `givenUnknownConversation_whenRendered_thenNotFoundPage` / `givenNonNumericId_whenRendered_thenNotFoundWithoutRequest` (F16b: `givenConversationWithoutTurns_whenRendered_thenNoTurnsText`) | `Konuşma bulunamadı.` + `Konuşmalar'a dön`; `/konusmalar/abc` için istek yok; tursuz → `Bu konuşmada henüz tur yok.` |
| F17 | Aktarma satırı | component | `givenHandedOffConversation_whenRendered_thenHandoffLine` | `Temsilciye aktarıldı · bireysel kuyruğu · 13:31 · sebep: cevap yok` |
| F18 | Erişilebilirlik | component (`vitest-axe`) | `givenListAndDetail_whenRendered_thenNoAxeViolations` | ihlal yok |
| F19 | Liste hatası | component | `givenServerError_whenRendered_thenErrorAlert` | 500 → `Beklenmeyen bir hata oluştu. Tekrar dene.` (`role="alert"`) |
| F20 | Önizleme sayacı yeniler | component `AdminLayout.test.tsx` | `givenAnswerDone_whenStreamEnds_thenConversationCountRefetched` | sayaç `0` → akış `done` → `1` |
| F21 | Taşınan alıntı çizimi | unit `shared/turn/answerFormat.test.ts` (taşınan test) + mevcut `AnswerMessage` testleri | (mevcut adlar) | değişmeden geçer |
| E1 | Gerçek sistem | e2e `web/e2e/conversations.spec.ts` | `givenAnsweredQuestion_whenConversationsOpened_thenTopRowAndDetailTable` | Önizleme'de bir soru → `done` → `/konusmalar` ilk satır `1` tur, ilk soru metni; satıra tıkla → parça tablosunda en az 1 satır, durum çipi var. Cevap metnine bakmaz |

**Elle çalıştırılanlar (qa):**
- **M1 (sözleşme, K1a):** iki ucun 200/400/404 cevapları `openapi.yaml` ile; boş veritabanında liste `totalElements 0` ve ekranda boş durum; seed yok.
- **M2 (S1):** Önizleme'de "Kartımı kaybettim, ne yapmalıyım?" → "Yenisi kaç günde gelir?" → Konuşmalar'da en üstte `2` tur, `Önizleme`, sayaç arttı.
- **M3 (S2):** detayda 2. turun `Arama sorgusu` satırı; `psql` ile `select vector_rank, prompt_position from turn_chunks where turn_id = …` sonuçlarının tabloyla aynı olduğu.
- **M4 (S3, K5):** "Kripto para hesabı açabilir miyim?" → `BİLMİYOR`; cevaplı tura Önizleme'de "Hayır" → `HATALI`; `GENERATION_MODE=llm` ile bir tur → `DAYANAKSIZ` ve uyarı; uzun bir cevap sürerken ikinci sekmede Konuşmalar → `işleniyor`, bitince kendiliğinden değişir.
- **M5 (AC3):** lab ile yan yana ekran görüntüsü: liste, detay, çipler, tablo, uyarı, sayaç.

## Deployment planı
Yok. Yeni servis, port, ortam değişkeni ya da lokal model yok. qa sistemi `docker compose -f deployment/docker-compose.yml up --build -d` ile yeni kodla kurar.

## Açık sorular ve riskler
1. **Kart soruları 1-5:** cevaplar farklı olursa yapılacaklar Özet'te "Kart cevapları başka seçenek olursa".
2. **Risk – BİLMİYOR geniş:** golden set (TASK-016) gelene kadar rehberde cevabı olduğu hâlde bulunamayan sorular da `BİLMİYOR` görünür; ayrım parça tablosundan yapılır. KAÇIRDI ve ÜRETİM HATASI'nın golden set'le eklenmesi TASK-016 Kural 8'in kapsamında kullanıcıya sorulur.
3. **Risk – süren tur bellekte:** assistant-api yeniden başlarsa süren turlar kaybolur (o anki akış kesilir ve tur hiç kaydedilmez; bugünkü davranış); kayıtlı turlar etkilenmez. Birden fazla assistant-api örneği çalışırsa bir örnek diğerinin süren turunu görmez (bugün tek örnek var).
4. **Kapsam dışı not:** kimlik doğrulama olmadığı için konuşma içerikleri sisteme erişen herkese açık (mevcut uçlarla aynı durum).

STATUS: DONE
