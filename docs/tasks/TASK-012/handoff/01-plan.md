TASK: TASK-012
# Plan

## Özet
**Mevcut durum (TASK-011):** Önizleme'de telefonun içinde mesaj listesi ve giriş alanı (`MessageComposer`: input + "Gönder") var. Öneri çipleri, "Soru listesi" düğmesi, "Kayıtlı sorular" penceresi ve otomatik tamamlama yok. assistant-api'de öneri verisi yok. Kullanıcı her zaman serbest metin yazar; soru mevcut konuşmada sorulur (`useAnswerStream.sendQuestion`), "Yeni konuşma" düğmesi önceki konuşmayı üstte bırakıp yenisini açar (`startNewConversation`, ayraç "Yeni konuşma · {id}").

**Yeni durum (kart soru 1-4'te önerilen seçeneklerle: 1A, 2A, 3A, 4A):**
- **assistant-api (değişen servis):** yeni `suggestion/` paketi. Ürün ekibinin hazırladığı öneri listesi (kategori → soru → takip soruları) assistant veritabanında üç tabloda durur (V4 migration, seed yok). `PUT /api/assistant/suggestions` bütün listeyi tek transaction'da değiştirir (ekranı yok; ürün ekibi `curl` ile dosya yükler), `GET /api/assistant/suggestions` bütün listeyi döner. Liste yüklenene kadar boştur.
- **web:** Önizleme telefonunda lab'ın öğeleri: mesajların altında en fazla 2 öneri çipi (`SuggestionChips`), giriş alanının solunda "Soru listesi" düğmesi ve "Kayıtlı sorular · N soru, M takip" penceresi (`QuestionListDialog`), yazarken açılan otomatik tamamlama listesi (`AutocompleteList`, combobox). Bütün eşleştirme tarayıcıda, lab'ın yöntemiyle (4 harflik kök + IDF + başlangıç bonusu) yapılır. Takip önerisi, son sorunun listedeki bir soruyla normalize edilmiş hâliyle aynı olmasından ve o sorunun takip bağlantılarından gelir. Takip önerisi aynı konuşmada, diğer liste soruları (konuşmada tur varsa) yeni konuşmada sorulur.
- **retrieval-api:** değişmez. **SSE:** değişmez. **Dil modeli:** kullanılmaz (soru 2 = A ise).
- **Docker:** yeni ortam değişkeni yok; nginx `/api/assistant/` bloğuna `client_max_body_size 2m` (en büyük geçerli liste ~1,2 MB).

**Silinen/değişen mevcut parçalar:** `MessageComposer` props'u genişler (`{ isStreaming, onSend }` → aşağıdaki imza) ve "Soru listesi" düğmesi ile combobox davranışını alır. `useAnswerStream`'e `askInNewConversation` eklenir; mevcut alanlar ve `startNewConversation` aynı kalır. `PreviewPage` telefonun içine `SuggestionChips` ve pencereyi ekler. İsim değişikliği yok.

**Kart cevapları başka seçenek olursa:**
- Soru 1 ≠ A: B (golden set) → task TASK-016'ya kadar bekler, plan ve sözleşme o task'ın veri yapısına göre yeniden yazılır; C (geçmiş konuşmalar) → `PUT` ve üç tablo yerine `GET` turlardan hesaplanır, kategori kaynak adıdır; D (belgelerden üretim) → arka plan üretim işi, prompt, retrieval-api parça okuma, durum alanı eklenir. Üçünde de planı ve sözleşmeyi yeniden yazarım, kart yeniden onaya gider (kapsam ve roller değişir).
- Soru 2 = B ya da C: assistant-api'ye `POST /api/assistant/turns/{turnId}/follow-up-suggestion` (dil modeliyle, cevabın parçalarına dayalı tek takip sorusu), `prompts/follow-up-suggestion.st` ve web'de `done` sonrası istek eklenir; coder'lar başlamadan sözleşme ve plan güncellenir (kart yeniden onaya gitmez, cevap onaydır).
- Soru 3 = B ya da C: eşleştirme sunucuya taşınır (`GET /api/assistant/suggestions/search?q=`); B'de PostgreSQL `turkish` tam metin araması, C'de retrieval-api'ye embedding ucu (retrieval-api değişir, kart yeniden onaya gider). A'da web'de kalır.
- Soru 4: sadece web'deki üç sabit metin değişir.

**Kart cevapları (2026-11-02, kullanıcı):** 1 A, 2 A, 3 A, 4 A. Dördü de planın önerdiği seçenek; plan değişmedi, yukarıdaki "başka seçenek olursa" dalları uygulanmaz.

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ortak kaynak `openapi.yaml` (`getSuggestions`, `replaceSuggestions`, `SuggestionSetResponse` ve istek şemaları). Web sadece `GET`'i çağırır.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/CLAUDE.md` (paket yapısı, V4, öneri yükleme komutu ve örnek dosya biçimi). Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md`. Yeni paket yok. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**` (MSW'ye `GET /api/assistant/suggestions` varsayılan handler'ı: boş set), `web/e2e/**`. Rapor `handoff/03-test-report.md`.
- **devops:** `web/nginx.conf` (`client_max_body_size 2m`), imajların yeniden derlenmesi, `README.md` ve `deployment/CLAUDE.md`'ye "Önerileri yükleme" (komut + örnek dosya). TASK'ta Docker işaretli değil ama qa yeni kodla çalışan sisteme ve kullanıcı yükleme komutuna ihtiyaç duyar. Rapor `handoff/04-deploy-report.md`.
- **qa:** kabul kriterleri, M1-M5, sözleşme uyumu, lab ile görsel karşılaştırma. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** yok. retrieval-api koduna kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Önerilerin kaynağı kartta seçilir; mock/koda gömülü soru listesi yok | Kart soru 1; V4 (seed yok), `SuggestionService`, `PUT`/`GET`; web sadece `GET` sonucunu gösterir | B8, M5 |
| K2a | Kategoriler koda gömülmez, kaynakla gelir | `suggestion_categories`, `SuggestionCategoryResponse`; web `QuestionListDialog` başlıkları veriden | B1, F10 |
| K2b | Hiç öneri yoksa çipler ve pencere boş durum gösterir; metin kartta | Kart soru 4; `SuggestionChips` (`isEmpty`), `QuestionListDialog` | B7, B8, F11 |
| K3a | En fazla 2 çip | `suggestionChips`, `CHIP_MAX_COUNT = 2` | F2 |
| K3b | Önceki tur varsa ilk çip takip; üretim yöntemi kartta | Kart soru 2; `followUpsFor`, `suggestionChips` | F2, F4 |
| K3c | Sorulmuş sorular yeniden önerilmez | `suggestionChips` (bu konuşmanın soruları normalize karşılaştırılır) | F2 |
| K3d | Cevap akarken çipler kapalı | `SuggestionChips` `isDisabled` | F6 |
| K4 | Takip çipi aynı konuşmada; diğerleri "Yeni konuşmada sorulur" ipucuyla yeni konuşma | `PreviewPage.handleSuggestionSelect`, `useAnswerStream.askInNewConversation`, `title` metinleri | F4, F5, F9 |
| K5a | Yazdıkça en fazla 7 eşleşme; Türkçe eklere dayanıklı; yöntem kartta | Kart soru 3; `suggestionMatcher`, `autocompleteItems`, `AUTOCOMPLETE_MAX_ITEMS = 7` | F1, F3 |
| K5b | ↑ ↓ gezinme, Enter seçme, Esc kapatma | `MessageComposer` klavye işleyicisi | F7, F14 |
| K5c | Eşleşme yoksa liste kapanır, yazılan gönderilir; "listeden seç" uyarısı yok | `MessageComposer` (`aria-expanded` false), `AutocompleteList` (uyarı metni yok) | F8 |
| K6 | Pencere başlığı "Kayıtlı sorular · N soru, M takip"; önce "Bu konuşmada devam", sonra kategoriler; tıklayınca sorulur ve kapanır | `QuestionListDialog` | F10 |
| K7 | `listbox`/`option` rolleri; pencere odak yönetimi | `AutocompleteList`, combobox ARIA, `shared/components/Dialog` | F7, F10, F13 |
| S1 | Boş konuşmada iki başlangıç önerisi; tıklayınca sorulur | K3a, `suggestionChips` | F2, F3b |
| S2 | "Kartımı kaybettim" cevabından sonra ilk çip takip, aynı konuşmada | K3b, K4 | F4, M2 |
| S3 | "asgari" yazınca asgari önerileri; ↓ + Enter ile seçilir | K5a, K5b | F1, F7, M3 |
| S4 | Listede olmayan soru + Enter normal sorulur | K5c | F8, M3 |
| AC1 | Kaynak kullanıcı onayıyla; kodda sabit soru listesi yok | Kart soru 1 | B8, M5 |
| AC2 | Klavye ve erişilebilirlik testleri; build'ler geçer | tester, tüm roller | F7, F13, F14, raporlar |
| AC3 | Görünüm lab'ın sol yarısıyla aynı | Lab `.chips`, `.chips button.fu`, `.comp .ib.lt`, `.ac`, `.qlist` stilleri | M4 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Öneriler okunamıyor (assistant-api kapalı, 500) | Çip satırı boş (metin yok); pencere açılır, başlık "Kayıtlı sorular", içinde `ErrorAlert` (`messageFor`); otomatik tamamlama açılmaz; soru yazıp göndermek çalışır | F12 |
| G2 | Büyük harf ve Türkçe harf: "ASGARİ", "KARTIMI" | `toLocaleLowerCase('tr')` ile "asgari", "kartımı"; aynı öneriler | F1 |
| G3 | Kurala uymayan liste yüklenir | 400 `VALIDATION_FAILED`, bütün ihlaller birlikte; kayıtlı liste değişmez | B3-B6 |
| G4 | Aynı anda iki liste yüklemesi | Sonuç gönderilen iki listeden biri, karışım yok | B10 |
| G5 | Konuşmanın son turu hatayla bitti (ör. `CHAT_MODEL_UNAVAILABLE`) | Takip önerisi çıkmaz; çiplerde başlangıç önerileri ("Yeni konuşmada sorulur") | F2 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Liste bütün olarak değiştirilir:** tek `PUT /api/assistant/suggestions` (tam liste), tek `GET` | Ürün ekibi listeyi bir dosyada tutar, düzenler, yeniden yükler; idempotent; ekran yok (kapsam dışı) | Kategori/soru başına CRUD uçları: ekran olmadan kullanışsız, kapsam büyür |
| 2 | **Eşleştirme tarayıcıda**, liste bir kez okunur (en fazla 500 soru) | Lab da tarayıcıda eşleştiriyordu; tuş başına istek yok, anında; test deterministik | Tuş başına sunucu araması (kart soru 3 B/C) |
| 3 | **İlişkisel üç tablo** (`suggestion_categories`, `suggested_questions`, `suggested_follow_ups`, `ON DELETE CASCADE`); değiştirme transaction'ının ilk komutu `LOCK TABLE suggestion_categories IN EXCLUSIVE MODE` | Eşzamanlı iki yükleme sıraya girer, karışık liste oluşmaz (G4); okuyucular eski listeyi görmeye devam eder | Tek satır `jsonb`: Hibernate 7 JSON eşlemesinin Jackson 3 ile davranışı belirsiz; kilitsiz sil-ekle: iki yükleme karışabilir |
| 4 | **Çapraz kurallar** (benzersizlik, toplam 500, takip ≠ soru) sınıf düzeyinde Bean Validation kısıtı `@ValidSuggestionSet` ile | İhlaller `MethodArgumentNotValidException` → mevcut handler → aynı `violations[].field` biçimi (`categories[1].questions[0].text`) | Service'te exception: `BaseException` `violations` taşımaz, handler'a metot eklemek gerekir |
| 5 | **`GET` sayfalı değil** | Liste yükleme kurallarıyla sınırlı (50 kategori, 500 soru, soru başına 5 takip); pencere ve eşleştirme bütün listeyi ister | `PageResponse`: istemci bütün sayfaları okumak zorunda kalır |
| 6 | **Tek normalizasyon** iki tarafta: tr küçük harf, harf/rakam/boşluk dışı → boşluk, boşluklar teke, kırpma (lab `norm`) | Benzersizlik (backend), takip eşleşmesi, sorulmuş soru ve otomatik tamamlama (web) aynı eşitliği kullanır | Sadece `trim`: "Kartımı kaybettim?" ile "kartımı kaybettim" iki ayrı soru sayılır |
| 7 | **Takip önerisi tam eşleşmeyle:** son sorunun normalize hâli listedeki bir sorununkiyle aynıysa o sorunun takipleri | Yanlış takip önermez; lab da kayıtlı soruya bağlıydı | Otomatik tamamlama skoruyla "en yakın" soruyu bulmak: ilgisiz takip önerebilir |
| 8 | **Kod `features/preview`'de** (`api/previewQueries.ts`, `model/suggestions.ts`, `model/suggestionMatcher.ts`, bileşenler) | Önerileri sadece Önizleme kullanır | `shared/`: başka kullanıcı yok |
| 9 | **Combobox: odak input'ta kalır**, seçili öğe `aria-activedescendant` ile; liste input odağı kaybedince kapanır; seçenek `onMouseDown` `preventDefault` | APG combobox kalıbı; yazmaya devam edilebilir | Odağı seçeneklere taşımak: yazma kesilir, lab davranışından ayrışır |
| 10 | **Öneriler için yoklama yok**; TanStack varsayılanı (sayfa açılışı, pencereye dönüş) | Liste nadiren değişir | `refetchInterval`: gereksiz istek |
| 11 | **`starter`/`followUps` isteğe bağlı** (yoksa `false`/`[]`); metinler kırpılarak saklanır, uzunluk kırpmadan önce sayılır (`SendMessageRequest` ile aynı) | Dosya yazmak kolay; mevcut doğrulama biçimiyle tutarlı | Zorunlu alanlar: her soruda gereksiz satır |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu (TASK-012), `Suggestions` tag'i.
- Yeni `GET /api/assistant/suggestions` (`getSuggestions`): 200 `SuggestionSetResponse` (boş liste dahil), 500 `INTERNAL_ERROR`.
- Yeni `PUT /api/assistant/suggestions` (`replaceSuggestions`): 200 `SuggestionSetResponse`; 400 `VALIDATION_FAILED` (kurallar ve mesajlar açıklamada) / `BAD_REQUEST`; 415 `UNSUPPORTED_MEDIA_TYPE`; 500. Yeni `errorCode` yok.
- Yeni şemalar: `ReplaceSuggestionsRequest`, `SuggestionCategoryRequest`, `SuggestedQuestionRequest`, `SuggestionSetResponse`, `SuggestionCategoryResponse`, `SuggestedQuestionResponse`.

`sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `jpa-patterns`, `spring-jpa-testing` (tester), `flyway-migrations`, `postgres-patterns`, `spring-transactions` (kilit), `spring-boot-openapi-documentation` (sözleşme uyumu; springdoc yok).

**Yeni dependency yok.**

### Migration `V4__create_suggestions.sql`
- `suggestion_categories`: `id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, `position int NOT NULL CHECK (position >= 0)`, `name text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 40)`, `CONSTRAINT uq_suggestion_categories_position UNIQUE (position)`.
- `suggested_questions`: `id` identity PK, `category_id bigint NOT NULL REFERENCES suggestion_categories (id) ON DELETE CASCADE`, `position int NOT NULL CHECK (position >= 0)`, `text text NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200)`, `starter boolean NOT NULL`, `CONSTRAINT uq_suggested_questions_category_position UNIQUE (category_id, position)`.
- `suggested_follow_ups`: `id` identity PK, `question_id bigint NOT NULL REFERENCES suggested_questions (id) ON DELETE CASCADE`, `position int NOT NULL CHECK (position >= 0)`, `text text NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200)`, `CONSTRAINT uq_suggested_follow_ups_question_position UNIQUE (question_id, position)`.
- Satır eklenmez (seed yok). `position` 0'dan başlar, gönderilen sıradır.

### Paket `suggestion/` (her alt pakete `package-info.java`)
**domain/entity/**
- `SuggestionCategory(id, position, name)`; `SuggestedQuestion(id, category @ManyToOne(fetch = LAZY), position, text, starter)`; `SuggestedFollowUp(id, question @ManyToOne(fetch = LAZY), position, text)`. Koleksiyon ilişkisi (`@OneToMany`) yok. `protected` no-arg constructor + tüm alanları alan constructor; değişiklik metodu yok (liste bütün olarak değişir).

**domain/dto/**
- İstek: `ReplaceSuggestionsRequest(@NotNull(message = "must not be null") @Size(max = 50, message = "size must be at most 50") @Nullable List<@NotNull(message = "must not be null") @Valid SuggestionCategoryRequest> categories)` + sınıf üstünde `@ValidSuggestionSet`.
- `SuggestionCategoryRequest(@NotBlank(message = "must not be blank") @Size(max = 40, message = "size must be at most 40 characters") @Nullable String name, @NotEmpty(message = "must not be empty") @Nullable List<@NotNull(message = "must not be null") @Valid SuggestedQuestionRequest> questions)`.
- `SuggestedQuestionRequest(@NotBlank(message = "must not be blank") @Size(max = 200, message = "size must be at most 200 characters") @Nullable String text, @Nullable Boolean starter, @Size(max = 5, message = "size must be at most 5") @Nullable List<@NotBlank(message = "must not be blank") @Size(max = 200, message = "size must be at most 200 characters") String> followUps)`. Yardımcılar: `boolean isStarter()` (`Boolean.TRUE.equals(starter)`), `List<String> followUpsOrEmpty()`.
- Yanıt: `SuggestionSetResponse(int questionCount, int followUpCount, List<SuggestionCategoryResponse> categories)`, `SuggestionCategoryResponse(String name, List<SuggestedQuestionResponse> questions)`, `SuggestedQuestionResponse(String text, boolean starter, List<String> followUps)`. JSON alan sırası `openapi.yaml` ile aynı.
- `SuggestionTextNormalizer` (final, private constructor): `static String normalize(String text)` → `text.toLowerCase(Locale.forLanguageTag("tr")).replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").strip()`.

**domain/validation/**
- `@ValidSuggestionSet` (TYPE, `message = "invalid suggestion set"`) + `SuggestionSetValidator implements ConstraintValidator<ValidSuggestionSet, ReplaceSuggestionsRequest>`. `categories` null ise ya da elemanlar null ise o kısmı atlar (alan kısıtları raporlar). Kontroller ve ihlal yolları (varsayılan ihlal kapatılır, `buildConstraintViolationWithTemplate(...).addPropertyNode(...)` ile):
  - toplam soru > 500 → `categories` / `total questions must be at most 500`
  - normalize kategori adı tekrarı → `categories[i].name` / `must be unique` (ikinci ve sonraki)
  - normalize soru metni tekrarı (bütün liste, kategori sonra soru sırasıyla) → `categories[i].questions[j].text` / `must be unique`
  - aynı sorudaki normalize takip tekrarı → `categories[i].questions[j].followUps[k]` / `must be unique`
  - takip = kendi sorusu (normalize) → `categories[i].questions[j].followUps[k]` / `must differ from the question`
  - Boş/null metinler normalize edilmez, atlanır (alan kısıtı zaten raporlar).

**repository/**
- `SuggestionCategoryRepository extends JpaRepository<SuggestionCategory, Long>`: `List<SuggestionCategory> findAllByOrderByPositionAsc()`; `@Modifying @Query(value = "LOCK TABLE suggestion_categories IN EXCLUSIVE MODE", nativeQuery = true) void lockForReplace()`.
- `SuggestedQuestionRepository`: `@Query("select q from SuggestedQuestion q join q.category c order by c.position, q.position") List<SuggestedQuestion> findAllInDisplayOrder()`.
- `SuggestedFollowUpRepository`: `@Query("select f from SuggestedFollowUp f join f.question q join q.category c order by c.position, q.position, f.position") List<SuggestedFollowUp> findAllInDisplayOrder()`.

**service/**
- `SuggestionService` (repository'ler): 
  - `@Transactional(readOnly = true) SuggestionSetResponse getSuggestions()` → üç sorgu, kimliklere göre gruplanır; `questionCount` = soru sayısı, `followUpCount` = takip sayısı.
  - `@Transactional SuggestionSetResponse replaceSuggestions(ReplaceSuggestionsRequest request)` → `lockForReplace()`, `categoryRepository.deleteAllInBatch()` (DB cascade soruları ve takipleri siler), sonra sırayla kategori, soru, takip kayıtları (metinler `strip()`), sonuç `getSuggestions()` ile aynı biçimde (aynı transaction'da yeniden okunur ya da kaydedilenlerden kurulur). Log: `INFO Suggestions replaced categories={} questions={} followUps={}` (metin loglanmaz).

**controller/**
- `SuggestionController` (`@RequestMapping("/api/assistant/suggestions")`): `@GetMapping SuggestionSetResponse getSuggestions()`; `@PutMapping(consumes = APPLICATION_JSON_VALUE) SuggestionSetResponse replaceSuggestions(@Valid @RequestBody ReplaceSuggestionsRequest request)`. Başka method yok (POST/DELETE → framework 405 `METHOD_NOT_ALLOWED`).

### `assistant-api/CLAUDE.md`
Paket yapısına `suggestion/`, V4, "Önerileri yükleme" duman testi: `curl -s -X PUT localhost:8090/api/assistant/suggestions -H 'Content-Type: application/json' --data-binary @suggestions.json` ve `curl -s localhost:8090/api/assistant/suggestions`; dosya biçimi `openapi.yaml` `ReplaceSuggestionsRequest` örneği. "Henüz olmayanlar" listesi değişmez.

## Frontend planı
Ek skill'ler: `frontend-a11y` (combobox, listbox, dialog), `frontend-design` (lab `.chips`, `.ac`, `.qlist`, `.comp .ib.lt`), `react-patterns`, `react-state-management`.

**Yeni paket yok.**

### model/
- `types.ts` (`openapi.yaml` ile birebir): `SuggestedQuestionResponse = { text: string; starter: boolean; followUps: string[] }`, `SuggestionCategoryResponse = { name: string; questions: SuggestedQuestionResponse[] }`, `SuggestionSetResponse = { questionCount: number; followUpCount: number; categories: SuggestionCategoryResponse[] }`. Ayrıca `SUGGESTION_KINDS = ['followUp', 'question'] as const`, `SuggestionKind`, `SuggestionItem = { text: string; kind: SuggestionKind }`.
- `suggestionText.ts`: `normalizeSuggestionText(text: string): string` → `text.toLocaleLowerCase('tr').replace(/[^\p{L}\p{N}\s]/gu, ' ').replace(/\s+/g, ' ').trim()` (lab `norm`; Kararlar #6).
- `suggestionMatcher.ts` (kart soru 3 = A; lab `acMatches`):
  - Sabitler: `STEM_LENGTH = 4`, `MIN_TOKEN_LENGTH = 3`, `PREFIX_BONUS = 3`, `STOP_WORDS` = lab listesi birebir: `ne, mi, mı, mu, mü, ve, ile, bir, için, gibi, kaç, nasıl, neden, var, yok, olur, ben, bana, benim, miyim, mıyım, muyum, müyüm, ama, daha, çok, şu, bu, o`.
  - `type SuggestionMatcher = { score: (input: string, text: string) => number }`; `createSuggestionMatcher(pool: string[]): SuggestionMatcher`.
  - Algoritma: kök = normalize kelimenin ilk 4 harfi. `df(kök)` = havuzda (liste sorularının metinleri; takipler hariç) o kökü içeren soru sayısı; `idf = ln((N + 1) / (df + 1))`, N = havuz büyüklüğü. Girdinin normalize kelimelerinden uzunluğu ≥ 3 ve durak kelimesi olmayanların benzersiz kökleri alınır; skor = metnin kelime köklerinde bulunan her girdi kökünün `idf` toplamı + (normalize metin normalize girdiyle başlıyorsa `PREFIX_BONUS`). Normalize girdi boşsa skor 0.
- `suggestions.ts`:
  - `CHIP_MAX_COUNT = 2`, `AUTOCOMPLETE_MAX_ITEMS = 7`.
  - `listQuestionTexts(set: SuggestionSetResponse): string[]` (kategori sonra soru sırası).
  - `followUpsFor(set: SuggestionSetResponse, turns: PreviewTurn[]): string[]` → son tur `phase === 'done'` değilse `[]`; değilse son turun `question`'ı ile normalize eşit liste sorusunun `followUps`'ı, yoksa `[]`.
  - `suggestionChips(set, turns): SuggestionItem[]` → `asked` = bu konuşmanın turlarının normalize soruları (hata ile bitenler dahil). Önce `followUpsFor`'dan sorulmamış ilk takip (`followUp`); sonra sorulmamış `starter` sorular liste sırasıyla, yetmezse sorulmamış diğer sorular liste sırasıyla (`question`); toplam `CHIP_MAX_COUNT`.
  - `autocompleteItems(set, turns, input, matcher): SuggestionItem[]` → normalize girdi boşsa `[]`; önce `followUpsFor` içinden skor > 0 olanlar kendi sıralarıyla (`followUp`), sonra skor > 0 liste soruları skora göre azalan (eşitlikte liste sırası), takiplerle normalize aynı olanlar hariç (`question`); toplam `AUTOCOMPLETE_MAX_ITEMS`. Sorulmuş sorular hariç tutulmaz (kart karar 3).

### api/
- `previewApi.ts`: `getSuggestions(): Promise<SuggestionSetResponse>` → `request('/api/assistant/suggestions')`.
- `previewQueries.ts` (yeni): `previewKeys = { suggestions: ['preview', 'suggestions'] as const }`, `useSuggestionsQuery()` (yoklama yok).

### hooks/
- `useAnswerStream.ts`: `AnswerStream`'e `askInNewConversation: (text: string) => void`. Akış sürüyorsa ya da metin gönderilemezse hiçbir şey yapmaz; konuşmada tur varsa `startNewConversation` ile aynı işi yapar (turlar `previousTurns`'e, konuşma kimliği `null`), sonra `sendQuestion(text)`. Tur yoksa doğrudan `sendQuestion(text)`.

### components/
- `SuggestionChips.tsx` (+ `.module.css`, lab `.chips`, `.chips button`, `.chips button.fu`, `.chips button[disabled]`): props `{ items: SuggestionItem[]; hasTurns: boolean; isEmpty: boolean; isDisabled: boolean; onSelect: (item: SuggestionItem) => void }`. Kapsayıcı `<div role="group" aria-label="Soru önerileri">`. `isEmpty` → tek `<p>` `Henüz soru önerisi yok.` (kart soru 4; stil lab `.ac p`: 12px, `--s-muted`). Değilse her öğe `<button type="button">`: `followUp` → `.followUp` sınıfı + `title="Bu konuşmada devam"`; `question` → `hasTurns` ise `title="Yeni konuşmada sorulur"`, değilse `title` yok. `disabled={isDisabled}`. Uzun metin satıra sarar (kart karar 4).
- `AutocompleteList.tsx` (+ `.module.css`, lab `.ac`, `.ac h6`, `.ac button`, `.ac button.sel`): props `{ id: string; items: SuggestionItem[]; activeIndex: number | null; hasTurns: boolean; onSelect: (item: SuggestionItem) => void }`. Export'lar: `AutocompleteList` ve `autocompleteOptionId(listId: string, index: number): string` (`${listId}-option-${index}`). `<div role="listbox" id={id} aria-label="Soru önerileri">`; takip öğeleri varsa `<div role="group" aria-labelledby=…>` + görünen başlık `Bu konuşmada devam`; soru öğeleri `hasTurns` ise aynı biçimde `Yeni konuşmada` başlıklı grupta, değilse grupsuz. Her seçenek `<div role="option" id={autocompleteOptionId(id, i)} aria-selected={i === activeIndex}>`; `onMouseDown` `preventDefault`, `onClick` → `onSelect`. Lab'daki alt paragraf ("listeden seç", "Eşleşen soru yok") yok.
- `MessageComposer.tsx`: props `{ isStreaming: boolean; hasTurns: boolean; autocompleteItemsFor: (input: string) => SuggestionItem[]; onSend: (text: string) => void; onSuggestionSelect: (item: SuggestionItem) => void; onQuestionListOpen: () => void }`.
  - Input'tan önce "Soru listesi" düğmesi: `<button type="button" aria-label="Soru listesi" title="Kayıtlı sorular">` + lab'ın liste SVG'si, stil lab `.comp .ib.lt`; akış sürerken de etkin (lab).
  - State: `value`, `isListOpen`, `activeIndex: number | null`. `items = isListOpen ? autocompleteItemsFor(value) : []`; liste görünür = `items.length > 0`.
  - Input combobox: `role="combobox"`, `aria-autocomplete="list"`, `aria-expanded={liste görünür}`, `aria-controls={listId}` (`useId`), `aria-activedescendant` = liste görünür ve `activeIndex !== null` ise `autocompleteOptionId(listId, activeIndex)`. Mevcut `aria-label="Soru"`, placeholder, `maxLength` aynı.
  - Yazma: `value` güncellenir, `isListOpen = normalize(value) !== ''`, `activeIndex = null`.
  - `ArrowDown`/`ArrowUp` (liste görünürken, `preventDefault`): `activeIndex` null ise 0; değilse sırasıyla `(i + 1) % n` / `(i - 1 + n) % n`.
  - `Enter` (form submit): liste görünür ve `activeIndex !== null` → `onSuggestionSelect(items[activeIndex])`; değilse mevcut davranış (`canSend` → `onSend(value)`). İkisinde de `value = ''`, liste kapanır.
  - `Escape` (liste görünürken): liste kapanır, `value` kalır, `preventDefault`.
  - Input `onBlur` → liste kapanır (Kararlar #9). Seçeneğe tıklama → `onSuggestionSelect`, `value = ''`, liste kapanır.
  - `<AutocompleteList>` formun içinde, input'un üstünde konumlanır (lab `.ac`: `position: absolute; bottom: 56px`).
- `QuestionListDialog.tsx` (+ `.module.css`, lab `.qlist`, `.qlist h6`, `.qlist button`, pencere içi paragraf stili): `shared/components/Dialog` ile. Props `{ suggestions: SuggestionSetResponse | undefined; errorCode: string | null; followUps: string[]; isStreaming: boolean; onSelect: (item: SuggestionItem) => void; onClose: () => void }`.
  - Başlık: `suggestions` varsa `Kayıtlı sorular · {questionCount} soru, {followUpCount} takip`, yoksa `Kayıtlı sorular`.
  - Açıklama paragrafı (kart soru 4): `Ürün ekibinin hazırladığı öneri sorular; sorunu her zaman kendin de yazabilirsin. Takip soruları ilk sorudan sonra "devam" önerisi olarak görünür.`
  - `errorCode` doluysa `ErrorAlert errorCode={errorCode}`. `questionCount === 0` ise `Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür.` (kart soru 4).
  - Liste: `followUps` boş değilse başlık `Bu konuşmada devam` + her takip `<button type="button">` (`followUp`); sonra her kategori başlığı (`<h4>`, görünüm lab `h6`) + soruları `<button type="button">` (`question`). Akış sürerken düğmeler `disabled` (kart karar 6). Tıklama → `onSelect(item)` ve `onClose()`.
- `pages/PreviewPage.tsx`: `useSuggestionsQuery()`; `matcher = useMemo(() => createSuggestionMatcher(listQuestionTexts(data)), [data])`; `hasTurns = answerStream.turns.length > 0`. Telefonun içinde `MessageList` → `SuggestionChips` → `MessageComposer`. `SuggestionChips`: veri yoksa (yükleniyor ya da hata) `items=[]`, `isEmpty=false`; varsa `items=suggestionChips(data, turns)`, `isEmpty = data.questionCount === 0`; `isDisabled = isStreaming`. `autocompleteItemsFor = (input) => data ? autocompleteItems(data, turns, input, matcher) : []`. `isQuestionListOpen` state; `QuestionListDialog` hata kodu `ApiError` ise `errorCode`, başka hata `UNEXPECTED_ERROR`. `handleSuggestionSelect(item)`: `followUp` → `answerStream.sendQuestion(item.text)`; `question` → `answerStream.askInNewConversation(item.text)`.
- `errorMessages.ts`: yeni kod yok (web `PUT` çağırmaz).
- `web/CLAUDE.md`: preview klasör listesine yeni dosyalar.

## Test senaryoları
Backend: Testcontainers (mevcut kurulum), tablolar her testten önce temizlenir. Web: MSW varsayılan `GET /api/assistant/suggestions` → `{questionCount:0, followUpCount:0, categories:[]}`; öneri testleri kendi handler'ını verir. Web fixture'ı (`SUGGESTIONS_FIXTURE`): Kartlar → `Kartımı kaybettim, ne yapmalıyım?` (starter, takipler `Yenisi kaç günde gelir?`, `Kartımı nasıl dondururum?`), `Kart şifremi unuttum` , `Kart aidatı ne kadar?`; Krediler → `Asgari ödeme tutarı nedir?` (starter). (`questionCount` 4, `followUpCount` 2.)

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Yükle + oku | integration `SuggestionControllerIntegrationTest` | `givenValidSet_whenReplaceThenGet_thenSameOrderAndCounts` | `openapi` istek örneği → 200; `GET` `questionCount` 3, `followUpCount` 2, kategoriler `Kartlar`, `Krediler`; ilk soru `starter` true, ikinci `starter` false, `Asgari…` `followUps` `[]` |
| B2 | Tamamen değiştirir | integration | `givenExistingSet_whenReplaceWithAnother_thenOnlyNewSetRemains` | ikinci yükleme tek kategori `Hesaplar` → `GET` sadece `Hesaplar`; `suggested_questions` ve `suggested_follow_ups` eski satırları 0 |
| B3 | Alan kuralları | controller `SuggestionControllerTest` (`@ParameterizedTest`) | `givenInvalidField_whenReplace_thenValidationFailedWithPath` | boş soru → `categories[0].questions[0].text` `must not be blank`; 201 harf → `size must be at most 200 characters`; 41 harflik ad → `categories[0].name` `size must be at most 40 characters`; `questions: []` → `categories[0].questions` `must not be empty`; 6 takip → `categories[0].questions[0].followUps` `size must be at most 5`; `categories` yok → `categories` `must not be null`; 51 kategori → `size must be at most 50` |
| B4 | Benzersizlik | unit `SuggestionSetValidatorTest` | `givenDuplicatesAfterNormalization_whenValidate_thenLaterOnesReported` | `Kartımı kaybettim?` ve (ikinci kategoride) `kartımı  KAYBETTİM` → tek ihlal `categories[1].questions[0].text` `must be unique`; `Kartlar`/`kartlar` → `categories[1].name` `must be unique`; aynı soruda takip tekrarı → `…followUps[1]` `must be unique`; takip = soru → `…followUps[0]` `must differ from the question` |
| B5 | Toplam sınır | unit `SuggestionSetValidatorTest` | `givenFiveHundredOneQuestions_whenValidate_thenTotalViolation` | 11 kategori × 50 = 550 soru → `categories` `total questions must be at most 500`; tam 500 → ihlal yok |
| B6 | Hatalı yükleme listeyi bozmaz | integration | `givenExistingSet_whenInvalidReplace_thenSetUnchanged` | önce B1 seti; boş metinli istek → 400 `VALIDATION_FAILED`; `GET` B1 setiyle aynı |
| B7 | Temizleme | integration | `givenExistingSet_whenReplaceWithEmptyCategories_thenEmptySet` | `{"categories":[]}` → 200 `{"questionCount":0,"followUpCount":0,"categories":[]}` |
| B8 | Seed yok | integration | `givenFreshDatabase_whenGetSuggestions_thenEmptySet` | Flyway sonrası `GET` → `questionCount` 0, `categories` `[]` |
| B9 | Uç | controller `SuggestionControllerTest` | `givenTextPlain_whenReplace_thenUnsupportedMediaType` / `givenMalformedJson_whenReplace_thenBadRequest` / `givenPost_whenSuggestions_thenMethodNotAllowed` | 415 `UNSUPPORTED_MEDIA_TYPE`; 400 `BAD_REQUEST`; 405 `METHOD_NOT_ALLOWED`; hepsi `application/problem+json` |
| B10 | Eşzamanlı yükleme | integration `SuggestionServiceIntegrationTest` (iki thread, `CountDownLatch`) | `givenTwoConcurrentReplaces_whenBothFinish_thenSetIsExactlyOneOfThem` | A seti (`Kartlar` 3 soru) ve B seti (`Hesaplar` 2 soru) aynı anda → `GET` tam olarak A ya da B; kategori sayısı 1, soru sayısı 3 ya da 2 |
| B11 | Kırpma ve varsayılanlar | integration | `givenPaddedTextsAndOmittedFields_whenReplace_thenStrippedAndDefaults` | `"  Kartımı kaybettim?  "` → `Kartımı kaybettim?`; `starter` yok → false; `followUps` yok → `[]` |
| B12 | Normalizasyon | unit `SuggestionTextNormalizerTest` | `givenTurkishAndPunctuation_whenNormalize_thenLabEquivalent` | `"  KARTIMI  kaybettim, ne yapmalıyım?"` → `kartımı kaybettim ne yapmalıyım`; `"İŞLEM"` → `işlem` |
| F1 | Eşleştirme | unit `suggestionMatcher.test.ts` | `givenPool_whenScore_thenStemIdfAndPrefix` | Fixture havuzu (4 soru): `şifre kart` → `Kart şifremi unuttum` ≈ 1,139 (ln(5/2)+ln(5/4)), `Kartımı kaybettim…` ≈ 0,223, `Kart aidatı…` ≈ 0,223, `Asgari…` 0 (`toBeCloseTo(…, 3)`); `asgari` ve `ASGARİ` → `Asgari…` > 0, diğerleri 0; `ne kaç` (durak) → hepsi 0; `kar` (3 harf) → `Kart…` ile başlayanlara sadece önek bonusu 3 |
| F2 | Çip mantığı | unit `suggestions.test.ts` | `givenConversationStates_whenSuggestionChips_thenFollowUpFirstAndNoRepeats` | tur yok → `Kartımı kaybettim…`, `Asgari…` (iki starter), `question`; son tur `Kartımı kaybettim, ne yapmalıyım?` `done` → `Yenisi kaç günde gelir?` (`followUp`) + `Asgari…`; son tur takip `Yenisi kaç günde gelir?` (listede soru değil) → takip yok, `Asgari…` + `Kart şifremi unuttum`; turlar `Kartımı…`, `Yenisi…`, yeniden `Kartımı…` (`done`) → `Kartımı nasıl dondururum?` (sorulmuş `Yenisi…` atlanır) + `Asgari…`; son tur `failed` → takip yok, `Asgari…` + `Kart şifremi unuttum`; son soru `kartımı KAYBETTİM ne yapmalıyım` (farklı yazım) → yine `Yenisi…` |
| F3 | Otomatik tamamlama öğeleri | unit `suggestions.test.ts` | `givenInput_whenAutocompleteItems_thenFollowUpsFirstAndMaxSeven` | ayrı 10 soruluk fixture, hepsi `kredi` geçer, girdi `kredi` → 7 öğe; son tur `Kartımı kaybettim…` + girdi `kart` → ilk öğe `Kartımı nasıl dondururum?` (`followUp`), sonra `question`'lar; boş girdi → `[]` |
| F3b | Başlangıç çipi | component `PreviewPage.test.tsx` | `givenEmptyConversation_whenStarterChipClicked_thenQuestionAsked` | iki çip `Kartımı kaybettim, ne yapmalıyım?`, `Asgari ödeme tutarı nedir?`; tıklama → `POST /conversations` 1, `POST …/messages` gövdesi `{"text":"Kartımı kaybettim, ne yapmalıyım?"}` |
| F4 | Takip çipi aynı konuşmada | component | `givenAnsweredListQuestion_whenFollowUpChipClicked_thenSameConversation` | ilk tur `done` sonrası ilk çip `Yenisi kaç günde gelir?` `title="Bu konuşmada devam"`; tıklama → ikinci `messages` aynı `conversationId`'ye, `POST /conversations` toplam 1 |
| F5 | Diğer çip yeni konuşmada | component | `givenConversationWithTurn_whenQuestionChipClicked_thenNewConversation` | çip `title="Yeni konuşmada sorulur"`; tıklama → `POST /conversations` toplam 2, ayraç `Yeni konuşma · 2`, önceki tur üstte görünür |
| F6 | Akış sürerken | component | `givenStreamingAnswer_whenRendered_thenChipsDisabled` | akış bitmeden iki çip `disabled`; `done` sonrası etkin |
| F7 | Klavye | component `MessageComposer.test.tsx` | `givenTypedAsgari_whenArrowDownAndEnter_thenSelectedSuggestionSent` | `asgari` → `listbox` görünür, input `aria-expanded="true"`; ↓ → ilk `option` `aria-selected="true"` ve input `aria-activedescendant` onun `id`'si; Enter → `onSuggestionSelect` `{text:'Asgari ödeme tutarı nedir?', kind:'question'}`, input boş; Esc senaryosu: `asgari` + Esc → `listbox` yok, input değeri `asgari` |
| F8 | Eşleşmeyen metin | component `MessageComposer.test.tsx` | `givenUnlistedText_whenEnter_thenTypedTextSent` | `Döviz kurları ne?` → `listbox` yok, `aria-expanded="false"`; Enter → `onSend('Döviz kurları ne?')`; "listeden seç" metni yok |
| F9 | Gruplar | component `PreviewPage.test.tsx` | `givenConversationWithTurn_whenTyping_thenFollowUpAndNewConversationGroups` | `kart` → `group` "Bu konuşmada devam" ve `group` "Yeni konuşmada"; "Yeni konuşmada" grubundan seçim → `POST /conversations` toplam 2 |
| F10 | Pencere | component `PreviewPage.test.tsx` | `givenSuggestions_whenQuestionListOpenedAndQuestionClicked_thenAskedAndClosed` | "Soru listesi" → `dialog` adı `Kayıtlı sorular · 4 soru, 2 takip`; tur sonrası ilk başlık `Bu konuşmada devam`, sonra `Kartlar`, `Krediler`; soruya tıklama → `messages` isteği, `dialog` kapanır, odak "Soru listesi" düğmesinde; Esc → kapanır, odak düğmede |
| F11 | Boş durum | component | `givenEmptySuggestionSet_whenPageOpens_thenEmptyStates` | çip satırında `Henüz soru önerisi yok.`; pencere adı `Kayıtlı sorular · 0 soru, 0 takip`, içinde `Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür.`; `kart` yazınca `listbox` yok |
| F12 | Okunamayan öneriler | component | `givenSuggestionsFail_whenPageOpens_thenNoChipsDialogAlertAndChatWorks` | 500 `INTERNAL_ERROR` → çip düğmesi yok, boş durum metni yok; pencere adı `Kayıtlı sorular`, `alert` `Beklenmeyen bir hata oluştu. Tekrar dene.`; yazılan soru gönderilir, cevap akar |
| F13 | Erişilebilirlik | component (`vitest-axe`) | `givenChips_whenRendered_thenNoAxeViolations` / `givenOpenAutocomplete_whenRendered_thenNoAxeViolations` / `givenQuestionListDialog_whenRendered_thenNoAxeViolations` | `axe` ihlal yok |
| F14 | Döngüsel gezinme | component `MessageComposer.test.tsx` | `givenThreeItems_whenArrowUpFromStart_thenWrapsToLast` | ilk ↑ → 0. öğe; ikinci ↑ → 2. öğe; ↓ → 0. öğe |
| F15 | Normalizasyon | unit `suggestionText.test.ts` | `givenTurkishText_whenNormalize_thenSameAsBackend` | B12 ile aynı iki örnek, aynı sonuç |
| E1 | Gerçek sistem | e2e `web/e2e/preview.spec.ts` | `givenRunningSystem_whenPreviewOpens_thenChipsMatchSuggestionSet` | `request.get('/api/assistant/suggestions')`; `questionCount` 0 ise `Henüz soru önerisi yok.` görünür; değilse en az bir çip görünür ve pencere başlığı `Kayıtlı sorular · {questionCount} soru, {followUpCount} takip`. Veriyi değiştirmez |

**Elle çalıştırılanlar (qa):**
- **M1 (sözleşme):** `curl` ile `GET`/`PUT` cevapları `openapi.yaml` şemalarıyla; hatalı dosya → `violations` yolları.
- **M2 (S1, S2, gerçek model):** önce mevcut liste `GET` ile saklanır. qa rehberden 3 kategori, ~8 soru ve takiplerden bir deneme listesi yükler (`Kartımı kaybettim, ne yapmalıyım?` → takip `Yenisi kaç günde gelir?` dahil). Boş konuşmada iki çip; çiple soru; cevap sonrası ilk çip takip, aynı konuşmada sorulur (Konuşmalar'da/`conversation_turns`'te aynı `conversation_id`); diğer çip yeni konuşma açar.
- **M3 (S3, S4):** `asgari` → asgari önerileri; ↓ + Enter; listede olmayan soru + Enter normal sorulur.
- **M4 (AC3):** lab ile yan yana ekran görüntüsü: çipler, takip çipinin rengi, liste düğmesi, otomatik tamamlama kutusu, pencere.
- **M5 (AC1):** `grep -rniE "kaybettim|asgari|aidat" web/src assistant-api/src/main --include=*.ts --include=*.tsx --include=*.java --include=*.sql | grep -vE "\.test\.|src/test/"` → boş. Bitince liste M2 öncesi hâline geri yüklenir.

## Deployment planı
- `web/nginx.conf` `location /api/assistant/` bloğuna `client_max_body_size 2m;` (en büyük geçerli liste: 500 soru × (200 + 5 × 200) karakter, Türkçe harflerle ~1,2 MB; nginx varsayılanı 1 MB).
- İmajlar yeniden derlenir; yeni ortam değişkeni, servis ya da lokal model yok.
- `README.md` ve `deployment/CLAUDE.md`: "Önerileri yükleme" — `curl -X PUT http://localhost:3000/api/assistant/suggestions -H 'Content-Type: application/json' --data-binary @suggestions.json`, dosya biçimi (`openapi.yaml` `ReplaceSuggestionsRequest` örneği), temizleme `{"categories":[]}`.

## Açık sorular ve riskler
1. **Kart soruları:** 1 → bütün backend ve kapsam (A dışı seçilirse plan yeniden yazılır, kart yeniden onaya gider); 2 → takip üretimi (B/C'de yeni uç ve prompt); 3 → eşleştirmenin yeri; 4 → üç web metni (F11, `QuestionListDialog` açıklaması).
2. **Risk – ilk açılışta öneri yok:** liste ürün ekibi yükleyene kadar boştur; ekranda boş durum metinleri görünür. Bu task'ta Raşit rehberi için liste hazırlanmaz.
3. **Risk – kimlik doğrulama yok:** projede hiçbir uçta kimlik doğrulama olmadığı için `PUT` ucuna da sisteme erişen herkes liste yükleyebilir (mevcut belge yükleme ucuyla aynı durum).
4. **Risk – serbest metinde takip yok:** takip önerisi sadece listedeki bir soru (yazım farkları hariç aynen) sorulduğunda çıkar; kullanıcı kendi cümlesini yazarsa çiplerde başlangıç önerileri görünür.
5. **Kapsam dışı not:** önerileri ekrandan yönetmek (TASK kapsam dışı).

STATUS: DONE
