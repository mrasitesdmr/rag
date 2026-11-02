TASK: TASK-013
# Plan

## Özet
**Mevcut durum (TASK-012):** Önizleme'de cevap balonunun altında sadece "Rehber · s. N" düğmeleri (ve boş bilgi bankası notu) var. "Faydalı mı?" satırı, cevabın saati, "Temsilciye bağlan" düğmesi yok. assistant-api'de geri bildirim ve aktarma ucu, tablosu, kolonu yok. Tur, cevap akışı bitince (`done` ya da `error`) tek seferde kaydedilir (`TurnService.recordTurn`); akışı süren turun veritabanında satırı ve kimliği yoktur. web'de `shared/toast/` yok.

**Yeni durum (kart soru 1-3'te önerilen seçeneklerle: 1A, 2A, 3A):**
- **assistant-api (değişen servis):**
  - `POST /api/assistant/turns/{turnId}/feedback` (`submitFeedback`, 204): `conversation_turns.helpful` + `feedback_at` tek `UPDATE` ile yazılır; son yazan kazanır. Sadece `error_code IS NULL` turlar (akışı `done` ile bitmiş) kabul edilir, aksi `409 TURN_NOT_COMPLETED`.
  - `POST /api/assistant/conversations/{conversationId}/handoff` (`requestHandoff`, 202): yeni `handoffs` tablosuna konuşma başına tek satır (`UNIQUE (conversation_id)`, `INSERT … ON CONFLICT DO NOTHING`). Kuyruk adı `assistant.handoff.queue=bireysel`, sebep `NO_ANSWER`. Tur "cevap yok" (`not_found_answer` true, `error_code` null) değilse `409 HANDOFF_NOT_ALLOWED`. Tekrar istek mevcut kaydı döner.
  - V5 migration. Yeni paket `handoff/`; geri bildirim `conversation/` içinde.
  - Aktarılan konuşmada soru sorulmaya devam eder (kart soru 1 = A); `sendMessage` değişmez.
- **web:** cevap balonunun altında lab'ın iki satırı: "cevap yok" turunda `HandoffAction` ("Temsilciye bağlan" → "Temsilci kuyruğundasın"), her tamamlanmış turda `TurnFeedback` ("Faydalı mı?" Evet/Hayır + cevabın saati). Başarılı geri bildirimde lab'ın toast'ı (`shared/toast/` ilk kez kurulur).
- **Konuşmalar ekranı:** bu task'ta değişmez. Karttaki işaret (soru 3) TASK-014'te uygulanır; bu task veriyi (`helpful`, `feedback_at`, `handoffs`) hazırlar.
- **retrieval-api, SSE (`sse.md`), dil modeli, Docker:** değişmez.

**Değişen mevcut parçalar:** `Turn` entity'sine iki alan; `TurnRepository`'ye bir `@Modifying` sorgu; `AssistantProperties`'e `handoff` bölümü (testlerde `AssistantProperties`'i elle kuran yerler güncellenir, tester). web'de `PreviewTurn`'e `conversationId` ve `answeredAt`; `AnswerMessage` ve `MessageList` props'u genişler; `AdminLayout` `ToastProvider` ile sarılır. İsim değişikliği yok.

**Kart cevapları başka seçenek olursa:**
- Soru 1 = B (aktarılan konuşmada yeni soru yok): `sendMessage`'a 404'ten sonra `409 CONVERSATION_HANDED_OFF` eklenir (sözleşme + `sse.md` "akış öncesi hatalar" + `errorMessages.ts`); web'de o konuşma aktarılınca giriş alanı, "Gönder", çipler ve soru listesi kapanır ve giriş alanının yerinde "Temsilci kuyruğundasın. Yeni soru için Yeni konuşma'ya bas." yazar. Kart yeniden onaya gitmez (cevap onaydır); coder'lar başlamadan plan ve sözleşme güncellenir.
- Soru 2 = B (konuşma satırına alanlar): `handoffs` tablosu yerine `conversations`'a `handoff_turn_id`, `handoff_reason`, `handoff_queue`, `handoff_at` (hepsi null, birlikte dolu CHECK); idempotentlik `UPDATE … WHERE handoff_at IS NULL`. Uç ve cevap aynı kalır. Soru 2 = C: kuyruk seçimi kuralı gerekir; plan yeniden yazılır, kart yeniden onaya gider.
- Soru 3: sadece TASK-014'ün planını etkiler; bu task'ın kodu değişmez.

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ortak kaynak `openapi.yaml` (`submitFeedback`, `requestHandoff`, `SubmitFeedbackRequest`, `RequestHandoffRequest`, `HandoffReason`, `HandoffResponse`, üç yeni `errorCode`).

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/CLAUDE.md` (paket yapısı, V5, yeni hata kodları, `assistant.handoff.queue`, duman testi komutları; "Henüz olmayanlar"dan geri bildirim ve aktarma çıkar). Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md` (preview dosya listesi, `shared/toast/`; "Henüz olmayanlar"dan `shared/toast/` çıkar). Yeni paket yok. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**` (`renderWithProviders`'a `ToastProvider`; MSW varsayılan handler'ları: `submitFeedback` → 204, `requestHandoff` → 202 fixture), `web/e2e/**`. Rapor `handoff/03-test-report.md`.
- **qa:** kabul kriterleri, M1-M5, sözleşme uyumu, lab ile görsel karşılaştırma. Çalışan sistemi mevcut komutla yeni kodla kurar: `docker compose -f deployment/docker-compose.yml up --build -d` (dosya değiştirmez). Rapor `handoff/05-qa-report.md`; qa tester'a bağlıdır.
- **Atlanan:** devops. Dockerfile, compose, nginx ve ortam değişkeni değişmiyor (kuyruk adı `application.properties`'te; gövdeler küçük, mevcut 2 MB sınırı yeter). Kılavuza yeni komut gerekmiyor. retrieval-api koduna kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1a | "Evet"/"Hayır" seçilir; seçili düğme vurgulanır | `TurnFeedback` (`aria-pressed`, `.selected` = lab `.fb button.on`) | F2, F3, M4 |
| K1b | Fikir değiştirilebilir, son seçim geçerli; aynı düğmeye yeniden basmak seçimi değiştirmez | `FeedbackService.submitFeedback` (koşulsuz `UPDATE`); `useTurnFeedback` (kayıtlı değerle aynı düğmeye basınca istek gönderilmez, seçim kalır) | B2, B3, F3, F4 |
| K1c | Sadece tamamlanmış tura; aksi sabit `errorCode` | `FeedbackService` → `409 TURN_NOT_COMPLETED`; web satırı sadece `phase === 'done'` turda çizer | B4, F5, F6 |
| K1d | Eşzamanlı iki geri bildirim tutarlı; son yazan kazanır, kayıp güncelleme yok | `TurnRepository.updateFeedback` (tek satırlık atomik `UPDATE`, sadece iki kolon) | B7 |
| K1e | Cevabın saati geri bildirim satırında (lab) | `TurnFeedback` (`formatTime(answeredAt)`), `PreviewTurn.answeredAt` | F1 |
| K2a | Düğme sadece "cevap yok" turunda; sebep `NO_ANSWER` | `AnswerMessage` (`turn.done.notFoundAnswer`), `HandoffService` (`HANDOFF_NOT_ALLOWED`), `HandoffReason` | B10, F7, F8 |
| K2b | Konuşma düzeyinde, bir kez; tekrar istek başarılı ama yeni kayıt yok | `handoffs.uq_handoffs_conversation`, `HandoffRepository.insertIfAbsent` | B9, B13, B15, F9 |
| K2c | 202; cevapta kuyruk adı ve sebep | `HandoffController` (202 + `HandoffResponse`), `assistant.handoff.queue` | B8, B14 |
| K2d | Aktarılan konuşmada yeni soru: kartta | Kart soru 1 (öneri A: sorulabilir); `sendMessage` değişmez | B16, F14 |
| K3 | Kayıt yeri ve kuyruk adı, kuyruğun görünüşü kartta; sahte temsilci cevabı yok | Kart soru 2 (`handoffs` tablosu, `bireysel`), soru 3 (TASK-014 işareti); hiçbir yerde temsilci mesajı üretilmez | B8, F8, M5 |
| K4 | Aktarma ve geri bildirim Konuşmalar'da görünür (TASK-014) | Veri: `conversation_turns.helpful`/`feedback_at`, `handoffs`; ekran TASK-014 | B1, B8 (veri DB'de) |
| K5 | Hata mesajları `errorCode` → Türkçe | `errorMessages.ts` (`TURN_NOT_FOUND`, `TURN_NOT_COMPLETED`, `HANDOFF_NOT_ALLOWED`) | F5, F10, F15 |
| S1 | "Evet" sonra "Hayır" → turda "Hayır" | K1b | B2, F3, M2 |
| S2 | "Bitcoin…" → bulamadım → "Temsilciye bağlan" → "Temsilci kuyruğundasın"; konuşma `bireysel` kuyruğunda | K2a, K2c | B8, F8, M3 |
| S3 | Aynı konuşma için iki aktarma → tek kayıt | K2b | B9, B13, M3 |
| AC1 | Uçlar `openapi.yaml`'da; backend ve web uyuyor | Sözleşme değişiklikleri | B6, B12, M1 |
| AC2 | İdempotentlik, eşzamanlılık, bitmemiş tur, cevap yok koşulu testli; build'ler geçer | tester, tüm roller | B4, B7, B9, B10, B13, raporlar |
| AC3 | Görünüm lab'ın sol yarısıyla aynı | Lab `.fb`, `.fb button`, `.fb button.on`, `.fb .ho`, `.msgs .m .tm`, `.toasts`, `.toast` | M4 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Geri bildirim ya da aktarma sırasında assistant-api'ye ulaşılamıyor (ağ hatası, 500) | Satırın altında `role="alert"` ile `messageFor` metni; seçim / düğme önceki hâlinde kalır; yeniden basılabilir | F5, F10 |
| G2 | "Temsilciye bağlan"a çift tıklama ya da iki sekmeden aynı anda istek | İstek sürerken düğme `disabled` (tek istek); sunucuda iki istek gelse de tek kayıt, iki cevap aynı | F11, B13 |
| G3 | Aynı konuşmada ikinci bir "cevap yok" turu (aktarmadan sonra) | O turda da düğme yerine "Temsilci kuyruğundasın"; sunucuya gitse de ilk kayıt döner | F9, B15 |
| G4 | Cevabı hatayla bitmiş ya da yarıda iptal edilmiş tur | Ekranda geri bildirim ve aktarma satırı yok; uca gelirse `409 TURN_NOT_COMPLETED` / `409 HANDOFF_NOT_ALLOWED` | B4, B10, F6 |
| G5 | Önceki konuşmanın ("Yeni konuşma" ayracının üstündeki) turları | Kendi konuşmalarının kimliğiyle çalışır: geri bildirim o tura, aktarma o konuşmaya gider; yeni konuşmanın durumu etkilenmez | F9 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **"Tamamlanmış tur" = kaydı `error_code IS NULL` olan tur** (akışı `done` ile bitti). Akışı süren turun satırı yoktur → `TURN_NOT_FOUND`; `error`, `STREAM_CANCELLED`, `RETRIEVAL_UNAVAILABLE` turları → `TURN_NOT_COMPLETED` | Tur akış bitince yazılıyor (`TurnService`); `TurnRepository.findAnsweredTurns` de aynı tanımı kullanıyor | Yarım cevaplı turlara da geri bildirim: TASK "sadece tamamlanmış" diyor |
| 2 | **Geri bildirim tek atomik `UPDATE conversation_turns SET helpful, feedback_at WHERE id`** (`@Modifying` JPQL), entity kaydetme yok | Satır kilidiyle sıralanır, son commit eden kazanır; diğer kolonlara dokunmaz (kayıp güncelleme yok); kontrol edilen `error_code` kayıttan sonra hiç değişmediği için oku-sonra-yaz yarışı yok | Entity'yi okuyup `save`: Hibernate bütün kolonları yazar; `@Version`: iki istekten biri gereksiz yere hata alır, "son yazan kazanır"la çelişir |
| 3 | **`submitFeedback` 204, gövdesiz** | Lab 204; web gönderdiği değeri zaten biliyor | Tur DTO'su dönmek: henüz tur okuma DTO'su yok (TASK-014) |
| 4 | **Aktarma `INSERT … ON CONFLICT (conversation_id) DO NOTHING` + aynı transaction'da okuma** | Eşzamanlı iki istekte PostgreSQL ikinciyi birincinin commit'ine kadar bekletir, sonra hiçbir şey yapmaz; READ COMMITTED'de sonraki `SELECT` kaydı görür; exception yakalamaya gerek yok | Önce `exists`, sonra `save`: yarışta `DataIntegrityViolationException` çevirmek gerekir; tablo kilidi: gereksiz geniş |
| 5 | **Tekrar istek de 202 ve ilk kaydı döner** (ilk `turnId`, ilk `createdAt`), farklı "cevap yok" turuyla gelse bile | TASK: "tekrar istek başarılı, yeni aktarma yok"; tek kayıt, tek cevap şekli | 200 ile ayırmak: TASK "uç 202 döner" diyor |
| 6 | **Doğrulama önce, idempotent dönüş sonra:** 400 → konuşma 404 → tur 404 → 409 → 202 | Yanlış tur kimliğiyle gelen tekrar istek de hatayı görür; kurallar her istekte aynı | Kayıt varsa her isteğe 202: hatalı istemciyi gizler |
| 7 | **202'de `Location` yok** | Okunacak bir aktarma ucu yok; iş arka planda sürmüyor | Uydurma bir `GET` ucu: kapsam dışı |
| 8 | **`reason` gövdede zorunlu, tek değerli enum `HandoffReason`** (lab gövdesi `{reason, turnId}`) | Lab sözleşmesi; ileride başka sebep eklenirse şema genişler | Sebepsiz gövde: lab'dan ayrışır |
| 9 | **Kuyruk adı `assistant.handoff.queue=bireysel`** (`@NotBlank`, en fazla 40 harf), kayda yazılır | Kayıt, aktarıldığı andaki kuyruğu saklar; ayar değişse eski kayıtlar değişmez | Sabit string: magic value (`backend-conventions` 3) |
| 10 | **Geri bildirim `conversation/`, aktarma `handoff/` paketinde**; `HandoffService` turu `TurnRepository` ile okur | `backend-conventions` paket ağacı; tur verisi `conversation`'ındır | Aktarmayı `conversation/` içine koymak: sözlükteki `handoff/` paketiyle çelişir |
| 11 | **web: onaylanan geri bildirim ve aktarma durumu `PreviewPage` seviyesinde iki hook'ta** (`useTurnFeedback`, `useHandoffRequests`), `turnId` / `conversationId` anahtarlı | "Yeni konuşma"da turlar `previousTurns` dizisine geçince bileşenler yeniden kurulur; durum kaybolmamalı | Bileşen içi `useState`: yeniden kurulunca kaybolur; `useAnswerStream`'e eklemek: akış hook'u şişer |
| 12 | **Seçim cevaptan sonra görünür (iyimser değil); istek sürerken o turun iki düğmesi `disabled`** | Ekrandaki seçim her zaman sunucudakiyle aynı; ard arda iki tıklamanın cevapları ters sırada gelip yanlış seçimi göstermesi engellenir | İyimser güncelleme + geri alma: ters sırada gelen cevaplarda tutarsız |
| 13 | **Aynı değere yeniden basınca istek gönderilmez** | Seçim değişmez (K1b); gereksiz istek ve toast yok. Sunucu yine de idempotent (B3) | Her basışta istek: aynı toast tekrar tekrar çıkar |
| 14 | **Cevabın saati = `done` olayının istemciye geldiği an** (`PreviewTurn.answeredAt`, `formatTime`) | "Cevabın saati"; `done` olayında sunucu saati yok, sözleşme değişmez | Sorunun saati: lab'da ikisi aynıydı ama TASK "cevabın saati" diyor |
| 15 | **Aktarma durumu konuşma düzeyinde gösterilir:** aynı konuşmanın bütün "cevap yok" turlarında "Temsilci kuyruğundasın" | Aktarma konuşma düzeyinde (K2b) | Sadece basılan turda: diğer turda düğme yeniden basılabilir görünür |
| 16 | **Toast'lar lab metinleriyle:** Evet → `Geri bildirim kaydedildi`, Hayır → `Geri bildirim kaydedildi; bu tur incelemeye düştü`; aktarmada toast yok (lab) | Ürün hedefi lab'ın sol yarısı; "Hayır" turları TASK-014'te Konuşmalar'da işaretli görünür | Metni değiştirmek: lab'dan ayrışır |
| 17 | **`shared/toast/` bu task'ta kurulur:** `ToastProvider` `AdminLayout`'ta, `useToast().showToast(message)`, süre `TOAST_DURATION_MS = 2600` (lab), bölge `aria-live="polite"` | `frontend-conventions` 4'te planlı yer; lab'ın toast'ı sol yarıda | Satır içi "kaydedildi" metni: lab'dan ayrışır |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu (TASK-013); `Turns` ve `Handoff` tag'leri.
- Yeni `POST /api/assistant/turns/{turnId}/feedback` (`submitFeedback`): gövde `SubmitFeedbackRequest {helpful}`; 204; 400 `VALIDATION_FAILED` (`helpful` `must not be null`) / `BAD_REQUEST`; 404 `TURN_NOT_FOUND`; 409 `TURN_NOT_COMPLETED`; 415; 500. Kontrol sırası açıklamada.
- Yeni `POST /api/assistant/conversations/{conversationId}/handoff` (`requestHandoff`): gövde `RequestHandoffRequest {turnId, reason}`; 202 `HandoffResponse {conversationId, turnId, reason, queue, createdAt}` (`Location` yok); 400 `VALIDATION_FAILED` (`turnId`/`reason` `must not be null`) / `BAD_REQUEST` (bilinmeyen `reason` dahil); 404 `CONVERSATION_NOT_FOUND` / `TURN_NOT_FOUND`; 409 `HANDOFF_NOT_ALLOWED`; 415; 500.
- `sendMessage` açıklamasına: aktarılan konuşma soru almaya devam eder (kart soru 1 = A).
- Yeni şemalar: `SubmitFeedbackRequest`, `HandoffReason` (`[NO_ANSWER]`), `RequestHandoffRequest`, `HandoffResponse`.
- `ProblemDetail.errorCode` enum'una: `TURN_NOT_FOUND`, `TURN_NOT_COMPLETED`, `HANDOFF_NOT_ALLOWED`.

`sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `spring-transactions` (atomik güncelleme, `ON CONFLICT`), `jpa-patterns`, `flyway-migrations`, `postgres-patterns`, `spring-mvc-testing` ve `spring-jpa-testing` (tester), `problem-details-rfc9457`.

**Yeni dependency yok.**

### Migration `V5__add_feedback_and_handoffs.sql`
- `conversation_turns`: `ADD COLUMN helpful boolean`, `ADD COLUMN feedback_at timestamptz`; `CONSTRAINT ck_conversation_turns_feedback_pair CHECK ((helpful IS NULL) = (feedback_at IS NULL))`; `CONSTRAINT ck_conversation_turns_feedback_completed CHECK (helpful IS NULL OR error_code IS NULL)`. Eski turlar `NULL`.
- `handoffs`: `id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, `conversation_id bigint NOT NULL REFERENCES conversations (id)`, `turn_id bigint NOT NULL REFERENCES conversation_turns (id)`, `reason text NOT NULL CHECK (reason IN ('NO_ANSWER'))`, `queue text NOT NULL CHECK (char_length(queue) BETWEEN 1 AND 40)`, `created_at timestamptz NOT NULL`, `CONSTRAINT uq_handoffs_conversation UNIQUE (conversation_id)`.

### Config
- `AssistantProperties`'e `@Valid @NotNull Handoff handoff`; `public record Handoff(@NotBlank @Size(max = 40) String queue)`.
- `application.properties`: `assistant.handoff.queue=bireysel` (ASCII; ortam değişkeniyle `ASSISTANT_HANDOFF_QUEUE`).

### Geri bildirim (`conversation/`)
- `domain/entity/Turn`: `@Column(name = "helpful") private @Nullable Boolean helpful;`, `@Column(name = "feedback_at") private @Nullable Instant feedbackAt;` + getter'lar (`getHelpful()`, `getFeedbackAt()`). `Turn.record` ikisini `null` bırakır. Değiştirici metot yok (güncelleme sorguyla).
- `domain/dto/SubmitFeedbackRequest(@NotNull(message = "must not be null") @Nullable Boolean helpful)`.
- `repository/TurnRepository`: `@Modifying @Query("update Turn t set t.helpful = :helpful, t.feedbackAt = :feedbackAt where t.id = :turnId") int updateFeedback(@Param("turnId") long turnId, @Param("helpful") boolean helpful, @Param("feedbackAt") Instant feedbackAt)`.
- `service/FeedbackService` (`TurnRepository`, `Clock`): `@Transactional void submitFeedback(long turnId, SubmitFeedbackRequest request)` → `findById` yoksa `NotFoundException("TURN_NOT_FOUND", …)`; `getErrorCode() != null` ise `ConflictException("TURN_NOT_COMPLETED", …)`; sonra `updateFeedback(turnId, helpful, Instant.now(clock))`. Log: `INFO Feedback submitted turnId={} helpful={}`.
- `controller/FeedbackController` (`@RequestMapping("/api/assistant/turns")`): `@PostMapping(path = "/{turnId}/feedback", consumes = APPLICATION_JSON_VALUE) ResponseEntity<Void> submitFeedback(@PathVariable long turnId, @Valid @RequestBody SubmitFeedbackRequest request)` → `204`.

### Aktarma (`handoff/`, her alt pakete `package-info.java`)
- `domain/entity/HandoffReason` (enum `NO_ANSWER`); `domain/entity/Handoff` (`id`, `conversationId long` (düz kolon, ilişki yok), `turnId long`, `reason` `@Enumerated(STRING)`, `queue`, `createdAt`; `protected` no-arg constructor, getter'lar; kayıt native sorguyla eklendiği için oluşturucu factory yok).
- `domain/dto/RequestHandoffRequest(@NotNull(message = "must not be null") @Nullable Long turnId, @NotNull(message = "must not be null") @Nullable HandoffReason reason)`; `HandoffResponse(long conversationId, long turnId, HandoffReason reason, String queue, Instant createdAt)` + `static HandoffResponse from(Handoff handoff)`. JSON alan sırası `openapi.yaml` ile aynı.
- `repository/HandoffRepository extends JpaRepository<Handoff, Long>`: `Optional<Handoff> findByConversationId(long conversationId)`; `@Modifying @Query(nativeQuery = true, value = "insert into handoffs (conversation_id, turn_id, reason, queue, created_at) values (:conversationId, :turnId, :reason, :queue, :createdAt) on conflict (conversation_id) do nothing") int insertIfAbsent(...)` (`reason` `String` olarak `name()`).
- `service/HandoffService` (`ConversationRepository`, `TurnRepository`, `HandoffRepository`, `AssistantProperties`, `Clock`): `@Transactional HandoffResponse requestHandoff(long conversationId, RequestHandoffRequest request)`:
  1. konuşma yoksa `NotFoundException("CONVERSATION_NOT_FOUND", …)`;
  2. tur yoksa ya da `turn.getConversation().getId() != conversationId` → `NotFoundException("TURN_NOT_FOUND", …)` (sorgu: `TurnRepository`'ye `@Query("select t from Turn t where t.id = :turnId and t.conversation.id = :conversationId") Optional<Turn> findInConversation(...)`);
  3. `errorCode != null` ya da `!isNotFoundAnswer()` → `ConflictException("HANDOFF_NOT_ALLOWED", …)`;
  4. `insertIfAbsent(conversationId, turnId, "NO_ANSWER", queue, Instant.now(clock))`; `findByConversationId` → `HandoffResponse.from`. Log: `INFO Conversation handed off conversationId={} turnId={} queue={} reason={} created={}` (`created` = insert sonucu 1 mi).
- `controller/HandoffController` (`@RequestMapping("/api/assistant/conversations")`): `@PostMapping(path = "/{conversationId}/handoff", consumes = APPLICATION_JSON_VALUE) ResponseEntity<HandoffResponse> requestHandoff(@PathVariable long conversationId, @Valid @RequestBody RequestHandoffRequest request)` → `ResponseEntity.accepted().body(...)`.

### `assistant-api/CLAUDE.md`
Paket yapısına `handoff/` ve `FeedbackController`/`FeedbackService`; V5; hata kodları listesine `TURN_NOT_FOUND`, `TURN_NOT_COMPLETED`, `HANDOFF_NOT_ALLOWED`; `assistant.handoff.queue`. Duman testi: `curl -s -o /dev/null -w '%{http_code}' -X POST localhost:8090/api/assistant/turns/<turnId>/feedback -H 'Content-Type: application/json' -d '{"helpful":false}'` → `204`; `curl -s -X POST localhost:8090/api/assistant/conversations/<id>/handoff -H 'Content-Type: application/json' -d '{"reason":"NO_ANSWER","turnId":<turnId>}'` → 202 + kayıt.

## Frontend planı
Ek skill'ler: `frontend-a11y` (toggle düğmesi `aria-pressed`, `aria-live`), `frontend-design` (lab `.fb`, `.toast`), `react-state-management` (mutation callback'leri).

**Yeni paket yok.**

### model/
- `types.ts` (`openapi.yaml` ile birebir): `SubmitFeedbackRequest = { helpful: boolean }`; `HANDOFF_REASONS = ['NO_ANSWER'] as const`, `HandoffReason`; `RequestHandoffRequest = { turnId: number; reason: HandoffReason }`; `HandoffResponse = { conversationId: number; turnId: number; reason: HandoffReason; queue: string; createdAt: string }`.
- `PreviewTurn`'e `conversationId: number | null` (konuşma kimliği bilinince yazılır) ve `answeredAt: string | null` (`done` gelince ISO zaman).
- `feedbackMessages.ts`: `FEEDBACK_HELPFUL_TOAST = 'Geri bildirim kaydedildi'`, `FEEDBACK_NOT_HELPFUL_TOAST = 'Geri bildirim kaydedildi; bu tur incelemeye düştü'`.

### api/
- `previewApi.ts`: `submitFeedback(turnId: number, body: SubmitFeedbackRequest): Promise<void>` → `request<void>('/api/assistant/turns/${turnId}/feedback', { method: 'POST', body })`; `requestHandoff(conversationId: number, body: RequestHandoffRequest): Promise<HandoffResponse>` → `request('/api/assistant/conversations/${conversationId}/handoff', { method: 'POST', body })`.
- `previewQueries.ts`: `useSubmitFeedbackMutation(options)` ve `useRequestHandoffMutation(options)`. Sonuç hook seviyesindeki `onSuccess(data, variables)` / `onError(error, variables)` ile işlenir; `mutate(vars, { onSuccess })` kullanılmaz (TanStack'te ard arda çağrıda sadece sonuncusunun çağrı içi callback'i çalışır, iki farklı turun istekleri karışır).

### hooks/
- `useAnswerStream.ts`: `newTurn` `conversationId: conversationIdRef.current`, `answeredAt: null` ile başlar; konuşma açılınca (`createConversation` sonrası) tura `conversationId` yazılır; `applyEvent` `done`'da `answeredAt = new Date().toISOString()`. Başka davranış değişmez.
- `useTurnFeedback.ts` → `FeedbackControls = { feedbackFor: (turnId: number) => TurnFeedbackState; submitFeedback: (turnId: number, helpful: boolean) => void }`, `TurnFeedbackState = { helpful: boolean | null; isSaving: boolean; errorCode: string | null }`. State `Record<number, TurnFeedbackState>`. `submitFeedback`: kayıtlı `helpful` aynıysa ya da `isSaving` ise hiçbir şey yapmaz (Kararlar #12-13); değilse `isSaving: true, errorCode: null` + mutation. Başarı → `helpful` = gönderilen, `isSaving: false`, toast (`useToast`, Kararlar #16). Hata → `isSaving: false`, `helpful` aynı kalır, `errorCode` = `ApiError.errorCode` ya da `UNEXPECTED_ERROR`.
- `useHandoffRequests.ts` → `HandoffControls = { handoffFor: (conversationId: number) => HandoffState; requestHandoff: (conversationId: number, turnId: number) => void }`, `HandoffState = { isQueued: boolean; isRequesting: boolean; errorCode: string | null }`. State `Record<number, …>` (konuşma anahtarlı). İstek gövdesi `{ reason: 'NO_ANSWER', turnId }`. Başarı → `isQueued: true`; hata → `errorCode`, `isQueued: false`. `isQueued` ya da `isRequesting` iken yeni istek yok.

### shared/toast/
- `ToastProvider.tsx`: context + `<div className={styles.toasts} aria-live="polite">` (lab `.toasts`: sayfa alanının altında ortalı, `pointer-events: none`); her toast `<div className={styles.toast}>` (lab `.toast`: koyu zemin, beyaz 600 13px, gölge, `tin` girişi; `prefers-reduced-motion`'da animasyonsuz), `TOAST_DURATION_MS = 2600` sonra kalkar; unmount'ta zamanlayıcılar temizlenir.
- `useToast.ts`: `useToast(): { showToast: (message: string) => void }`; provider dışında `Error('useToast must be used within ToastProvider')`.
- `app/AdminLayout.tsx`: `<main>` içeriği `ToastProvider` ile sarılır (bölge sayfa alanında konumlanır).

### components/
- `TurnFeedback.tsx` (+ `.module.css`, lab `.fb`, `.fb button`, `.fb button.on`, `.msgs .m .tm`): props `{ state: TurnFeedbackState; answeredAt: string; onSubmit: (helpful: boolean) => void }`. `<div className={styles.feedback} role="group" aria-label="Geri bildirim">`: `<span>Faydalı mı?</span>`, `<button type="button" aria-pressed={helpful === true}>Evet</button>`, `<button type="button" aria-pressed={helpful === false}>Hayır</button>` (seçili olan `.selected`; `isSaving` iken ikisi `disabled`), `<span className={styles.time}>{formatTime(answeredAt)}</span>`. `errorCode` doluysa altında `<p role="alert" className={styles.error}>{messageFor(errorCode)}</p>`.
- `HandoffAction.tsx` (+ `.module.css`, lab `.fb`, `.fb .ho`): props `{ state: HandoffState; onRequest: () => void }`. `isQueued` → `<span>Temsilci kuyruğundasın</span>`; değilse `<button type="button" className={styles.handoffButton} disabled={isRequesting}>Temsilciye bağlan</button>`. `errorCode` → `TurnFeedback` ile aynı alert.
- `AnswerMessage.tsx`: props `{ turn; feedback: FeedbackControls; handoff: HandoffControls; onCitationOpen }`. `phase === 'done'` ve `done`/`answeredAt` varken, kaynak düğmelerinden ve boş bilgi bankası notundan sonra: `done.notFoundAnswer` ise `HandoffAction` (`handoff.handoffFor(turn.conversationId)`; `onRequest` → `requestHandoff(conversationId, done.turnId)`), ardından her zaman `TurnFeedback` (`feedback.feedbackFor(done.turnId)`). Lab sırası: kaynaklar → aktarma satırı → geri bildirim satırı. `waiting`/`streaming`/`failed` turlarda ikisi de yok.
- `MessageList.tsx`: `feedback` ve `handoff` props'unu `TurnMessages` → `AnswerMessage`'a geçirir (önceki konuşmanın turları dahil).
- `pages/PreviewPage.tsx`: `const feedback = useTurnFeedback()`, `const handoff = useHandoffRequests()`; `MessageList`'e verir.

### errorMessages.ts
- `TURN_NOT_FOUND: 'Bu cevap bulunamadı. Sayfayı yenile.'`
- `TURN_NOT_COMPLETED: 'Tamamlanmamış bir cevaba geri bildirim verilemez.'`
- `HANDOFF_NOT_ALLOWED: 'Temsilciye sadece asistanın cevap bulamadığı sorularda bağlanabilirsin.'`

### `web/CLAUDE.md`
preview listesine `useTurnFeedback`, `useHandoffRequests`, `TurnFeedback`, `HandoffAction`, `feedbackMessages`; shared listesine `toast/`; "Henüz olmayanlar"dan `shared/toast/` çıkar.

## Test senaryoları
Backend: Testcontainers (mevcut kurulum), tablolar her testten önce temizlenir; turlar `TurnRepository`/fixture ile doğrudan yazılır (`TurnFixtures`: `answeredTurn`, `notFoundTurn`, `failedTurn(errorCode)`, `llmOnlyTurn`). Saat `FixedClockTestConfiguration` (`2026-11-02T10:31:05Z`). Web: MSW; fixture `done` olayı `turnId: 12`, konuşma `1`; "cevap yok" akışı `notFoundAnswer: true`.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Geri bildirim kaydı | integration `FeedbackControllerIntegrationTest` | `givenAnsweredTurn_whenSubmitFeedback_thenStoresHelpfulAndTime` | `{"helpful":true}` → 204, gövde yok; satırda `helpful` true, `feedback_at` `2026-11-02T10:31:05Z` |
| B2 | Fikir değiştirme (S1) | integration | `givenHelpfulTrue_whenSubmitFalse_thenFalseStored` | önce true, sonra false → `helpful` false |
| B3 | Aynı değer tekrar | integration | `givenHelpfulTrue_whenSubmitTrueAgain_thenStillTrue` | ikinci istek 204, `helpful` true |
| B4 | Tamamlanmamış tur | integration (`@ParameterizedTest`: `CHAT_MODEL_UNAVAILABLE`, `STREAM_CANCELLED`, `RETRIEVAL_UNAVAILABLE`) | `givenFailedTurn_whenSubmitFeedback_thenTurnNotCompleted` | 409 `TURN_NOT_COMPLETED`, `application/problem+json`; `helpful` null |
| B5 | Olmayan tur | controller `FeedbackControllerTest` | `givenUnknownTurnId_whenSubmitFeedback_thenTurnNotFound` | 404 `TURN_NOT_FOUND` |
| B6 | Gövde ve uç hataları | controller | `givenMissingHelpful_whenSubmitFeedback_thenValidationFailed` / `givenMalformedJson_whenSubmitFeedback_thenBadRequest` / `givenNonNumericTurnId_whenSubmitFeedback_thenBadRequest` / `givenTextPlain_whenSubmitFeedback_thenUnsupportedMediaType` | 400 `VALIDATION_FAILED` + `violations[0]` `{field:"helpful", message:"must not be null"}`; 400 `BAD_REQUEST`; 400 `BAD_REQUEST`; 415 `UNSUPPORTED_MEDIA_TYPE` |
| B7 | Eşzamanlılık, kayıp güncelleme yok | integration `FeedbackServiceIntegrationTest` (iki thread, `CountDownLatch`, 20 tekrar) | `givenConcurrentOppositeFeedbacks_whenBothFinish_thenOneValueAndOtherColumnsUnchanged` | her turda `helpful` ∈ {true, false} ve `feedback_at` dolu; `answer`, `cited`, `not_found_answer`, `latency_ms` istekten önceki değerlerle aynı |
| B8 | Aktarma (S2) | integration `HandoffControllerIntegrationTest` | `givenNotFoundTurn_whenRequestHandoff_thenAcceptedWithQueueAndReason` | 202 `{"conversationId":1,"turnId":<id>,"reason":"NO_ANSWER","queue":"bireysel","createdAt":"2026-11-02T10:31:05Z"}`; `Location` yok; `handoffs` 1 satır |
| B9 | İdempotent (S3) | integration | `givenHandedOffConversation_whenRequestAgain_thenSameRecordAndOneRow` | ikinci istek 202, gövde birinciyle aynı; `handoffs` 1 satır |
| B10 | Cevap yok koşulu | integration (`@ParameterizedTest`: cevaplı tur, `CHAT_MODEL_UNAVAILABLE` turu, Sadece LLM turu) | `givenTurnWithoutNotFoundAnswer_whenRequestHandoff_thenHandoffNotAllowed` | 409 `HANDOFF_NOT_ALLOWED`; `handoffs` 0 satır |
| B11 | Bulunamayanlar | integration | `givenUnknownConversation_whenRequestHandoff_thenConversationNotFound` / `givenTurnOfAnotherConversation_whenRequestHandoff_thenTurnNotFound` / `givenUnknownTurn_whenRequestHandoff_thenTurnNotFound` | 404 `CONVERSATION_NOT_FOUND`; 404 `TURN_NOT_FOUND`; 404 `TURN_NOT_FOUND`; `handoffs` 0 satır |
| B12 | Gövde hataları | controller `HandoffControllerTest` | `givenMissingTurnId_whenRequestHandoff_thenValidationFailed` / `givenMissingReason_whenRequestHandoff_thenValidationFailed` / `givenUnknownReason_whenRequestHandoff_thenBadRequest` / `givenTextPlain_whenRequestHandoff_thenUnsupportedMediaType` | `violations` `turnId` / `reason` `must not be null`; `"reason":"OTHER"` → 400 `BAD_REQUEST`; 415 |
| B13 | Eşzamanlı aktarma | integration `HandoffServiceIntegrationTest` (iki thread, `CountDownLatch`) | `givenTwoConcurrentRequests_whenBothFinish_thenOneRowAndSameResponse` | iki çağrı da hatasız; iki `HandoffResponse` eşit; `handoffs` 1 satır |
| B14 | Kuyruk ayarı | unit `HandoffServiceTest` | `givenConfiguredQueue_whenRequestHandoff_thenInsertsThatQueue` | `assistant.handoff.queue` = `kurumsal` → `insertIfAbsent`'e `kurumsal` gider, cevapta `kurumsal` |
| B15 | İkinci "cevap yok" turu | integration | `givenHandedOffConversation_whenRequestWithAnotherNotFoundTurn_thenFirstRecordReturned` | 202, `turnId` ilk turunki; 1 satır |
| B16 | Aktarılan konuşmada soru (soru 1 = A) | integration (sahte `ChatModel`, `RetrievalClient`) | `givenHandedOffConversation_whenSendMessage_thenAnswerStreams` | `token`… `sources` `done`; yeni tur kaydı; `handoffs` değişmez |
| B17 | Migration kısıtları | integration | `givenFailedTurn_whenHelpfulWrittenDirectly_thenCheckViolation` | SQL ile `error_code` dolu tura `helpful` yazmak `ck_conversation_turns_feedback_completed` ihlali |
| F1 | Satır görünümü | component `PreviewPage.feedback.test.tsx` | `givenDoneTurn_whenRendered_thenFeedbackRowWithAnswerTime` | `group` "Geri bildirim": `Faydalı mı?`, `Evet`/`Hayır` `aria-pressed="false"`; saat `done` anının `formatTime`'ı (sistem saati `vi.setSystemTime` ile sabit, beklenen `13:31` elle yazılır) |
| F2 | Evet | component | `givenDoneTurn_whenYesClicked_thenRequestSentAndYesPressed` | `POST /api/assistant/turns/12/feedback` gövdesi `{"helpful":true}`; `Evet` `aria-pressed="true"`; toast `Geri bildirim kaydedildi` |
| F3 | Fikir değiştirme (S1) | component | `givenYesSelected_whenNoClicked_thenNoPressedAndReviewToast` | ikinci gövde `{"helpful":false}`; `Hayır` pressed, `Evet` değil; toast `Geri bildirim kaydedildi; bu tur incelemeye düştü` |
| F4 | Aynı düğme | component | `givenYesSelected_whenYesClickedAgain_thenNoRequest` | toplam istek 1; `Evet` pressed |
| F5 | Hata | component (`it.each`: 409 `TURN_NOT_COMPLETED`, 404 `TURN_NOT_FOUND`, ağ hatası) | `givenFeedbackFails_whenYesClicked_thenAlertAndSelectionUnchanged` | `alert`: `Tamamlanmamış bir cevaba geri bildirim verilemez.` / `Bu cevap bulunamadı. Sayfayı yenile.` / `Sunucuya ulaşılamıyor. Bağlantını kontrol et.`; `Evet` `aria-pressed="false"` |
| F6 | Satır sadece tamamlanmış turda | component | `givenStreamingOrFailedTurn_whenRendered_thenNoFeedbackOrHandoffRow` | akış sürerken ve `error` olayından sonra `Faydalı mı?` ve `Temsilciye bağlan` yok |
| F7 | Düğme sadece "cevap yok" turunda | component | `givenAnsweredTurn_whenRendered_thenNoHandoffButton` | `notFoundAnswer: false` → düğme yok; `true` → `Temsilciye bağlan` var |
| F8 | Aktarma (S2) | component | `givenNotFoundTurn_whenHandoffClicked_thenQueuedText` | `POST /api/assistant/conversations/1/handoff` gövdesi `{"reason":"NO_ANSWER","turnId":12}`; düğme yok, `Temsilci kuyruğundasın` görünür |
| F9 | Konuşma düzeyi ve önceki konuşma | component | `givenHandedOffConversation_whenSecondNotFoundTurnAndNewConversation_thenQueuedOnlyInThatConversation` | aynı konuşmanın ikinci "cevap yok" turunda `Temsilci kuyruğundasın`; "Yeni konuşma" sonrası yeni konuşmanın "cevap yok" turunda `Temsilciye bağlan`; önceki konuşmanın turu `Temsilci kuyruğundasın` kalır |
| F10 | Aktarma hatası | component | `givenHandoffNotAllowed_whenHandoffClicked_thenAlertAndButtonStays` | 409 → `alert` `Temsilciye sadece asistanın cevap bulamadığı sorularda bağlanabilirsin.`; düğme duruyor |
| F11 | Çift tıklama | component (MSW cevabı geciktirilir) | `givenPendingHandoff_whenClickedTwice_thenOneRequest` | istek sürerken düğme `disabled`; istek sayısı 1 |
| F12 | Erişilebilirlik | component (`vitest-axe`) | `givenFeedbackAndHandoffRows_whenRendered_thenNoAxeViolations` | ihlal yok |
| F13 | Toast | component `ToastProvider.test.tsx` (fake timers) | `givenShownToast_whenDurationPasses_thenRemoved` | `showToast('Geri bildirim kaydedildi')` → metin `aria-live="polite"` bölgede; 2600 ms sonra yok |
| F14 | Aktarma sonrası soru (soru 1 = A) | component | `givenHandedOffConversation_whenQuestionSent_thenAnswerStreams` | giriş alanı etkin; `messages` isteği aynı `conversationId`'ye gider |
| F15 | Satır durumu iki tur arasında | component | `givenTwoDoneTurns_whenFeedbackOnFirst_thenSecondUnaffected` | 1. tur `Evet` pressed; 2. turun düğmeleri `aria-pressed="false"` |
| E1 | Gerçek sistem | e2e `web/e2e/preview.spec.ts` | `givenRunningSystem_whenAnswerDoneAndYesClicked_thenYesPressed` | bir soru → `Faydalı mı?` görünür → `Evet` → `aria-pressed="true"` ve toast. Cevap metnine bakmaz |

**Elle çalıştırılanlar (qa):**
- **M1 (sözleşme):** `curl` ile iki ucun 204/202/400/404/409/415 cevapları `openapi.yaml` ile; `HandoffResponse` alanları ve sırası.
- **M2 (S1):** Önizleme'de "Kartımı kaybettim, ne yapmalıyım?" → `Evet` → `Hayır`; `psql` ile `select helpful, feedback_at from conversation_turns where id = …` → `false`.
- **M3 (S2, S3, gerçek model):** "Bitcoin alıp satabilir miyim?" → "Bu konuda rehberimizde bilgi bulamadım." → `Temsilciye bağlan` → `Temsilci kuyruğundasın`; `select * from handoffs` → 1 satır, `queue` `bireysel`, `reason` `NO_ANSWER`; aynı isteği `curl` ile iki kez → hâlâ 1 satır. Aynı konuşmada yeni soru sorulabiliyor.
- **M4 (AC3):** lab ile yan yana ekran görüntüsü: iki satır, seçili düğme rengi, "Temsilciye bağlan" düğme rengi, saat, toast.
- **M5 (K3):** aktarmadan sonra ekranda ya da veritabanında temsilci mesajı yok (`conversation_turns` satır sayısı aktarmayla değişmez).

## Deployment planı
Yok. Yeni servis, ortam değişkeni, port ya da lokal model yok. qa sistemi `docker compose -f deployment/docker-compose.yml up --build -d` ile yeni kodla kurar.

## Açık sorular ve riskler
1. **Kart soruları:** 1 → `sendMessage` davranışı (B'de yeni 409 ve giriş alanının kapanması); 2 → kaydın yeri (B'de `conversations` kolonları, C'de plan yeniden yazılır); 3 → sadece TASK-014.
2. **Risk – Önizleme durumu bellekte:** Önizleme sayfası yenilenince ekrandaki konuşma ve satırların durumu kaybolur (bugünkü davranış); kayıtlar veritabanında kalır ve TASK-014'te Konuşmalar'da görünür.
3. **Risk – "incelemeye düştü" metni:** ayrı bir inceleme listesi yok; "Hayır" turları TASK-014'te Konuşmalar'da işaretli görünür.
4. **Kapsam dışı not:** kimlik doğrulama olmadığı için uçları sisteme erişen herkes çağırabilir (mevcut uçlarla aynı durum).

## Düzeltme turu 1
Kaynak: backend-coder'ın sözleşme bildirimi; lead kararı seçenek A (kod değişmez, kart değişmez, yeniden onay yok).
- **Madde:** `openapi.yaml` 400 açıklamaları "`helpful` JSON boolean değilse / `turnId` integer değilse `BAD_REQUEST`" diyordu; JSON okuyucunun varsayılan dönüşümü (`"true"`, `1` → `true`; `"12"`, `12.5` → `12`) ve mevcut uçların davranışı bununla çelişiyordu.
- **Değişiklik:** `submitFeedback` ve `requestHandoff` 400 açıklamaları yumuşatıldı: eksik/null alan → `VALIDATION_FAILED` (değişmedi); dönüştürülemeyen değer (`"yes"`, `"abc"`, nesne, dizi), bozuk JSON, bilinmeyen `reason` ve sayı olmayan path id'si → `BAD_REQUEST`; varsayılan dönüşümle okunabilen değerler hata değildir. İstemci JSON boolean / integer gönderir.
- **Testler:** B6 ve B12 değişmez (eksik alan, bozuk JSON, sayı olmayan path id'si, bilinmeyen `reason`). Dönüştürülebilen değerin 400 döndüğünü bekleyen test yazılmaz.

STATUS: DONE
