TASK: TASK-012
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `flyway-migrations`, `postgres-patterns`, `jpa-patterns`, `spring-transactions`.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Migration V4 (K1, K2a; seed yok) | `src/main/resources/db/migration/V4__create_suggestions.sql` | Üç tablo plandaki gibi: identity PK, `position >= 0`, uzunluk CHECK'leri, `uq_*` benzersizlikleri (FK kolonları bu unique'lerin ilk kolonu olduğu için ayrıca index'li), `ON DELETE CASCADE`. Satır eklenmez. |
| domain/entity | `suggestion/domain/entity/SuggestionCategory`, `SuggestedQuestion`, `SuggestedFollowUp` | `@ManyToOne(LAZY)`, `@OneToMany` yok, `protected` no-arg + tüm alanları alan constructor, değişiklik metodu yok. |
| domain/dto: istek | `ReplaceSuggestionsRequest` (`@ValidSuggestionSet`), `SuggestionCategoryRequest`, `SuggestedQuestionRequest` (`isStarter()`, `followUpsOrEmpty()`) | Mesajlar ve sınırlar sözleşmeyle birebir aynı (50 kategori, 40/200 karakter, en az 1 soru, en fazla 5 takip). |
| domain/dto: yanıt | `SuggestionSetResponse`, `SuggestionCategoryResponse`, `SuggestedQuestionResponse` | Alan sırası `openapi.yaml` ile aynı; listeler `List.copyOf`. |
| Kararlar #6, normalizasyon | `suggestion/domain/dto/SuggestionTextNormalizer` | tr küçük harf → `[^\p{L}\p{N}\s]` → boşluk → boşluklar teke → `strip()`. |
| Kararlar #4, G3, çapraz kurallar | `suggestion/domain/validation/ValidSuggestionSet`, `SuggestionSetValidator` | Toplam > 500 → `categories`; ad tekrarı → `categories[i].name`; soru tekrarı (bütün liste) → `categories[i].questions[j].text`; takip tekrarı → `…followUps[k]` `must be unique`; takip = soru → `…followUps[k]` `must differ from the question`. Null ya da boş öğeler atlanır, onları alan kısıtları raporlar. İhlaller mevcut `GlobalExceptionHandler` ile `VALIDATION_FAILED` + `violations` olarak döner. |
| repository | `SuggestionCategoryRepository` (`findAllByOrderByPositionAsc`, `lockForReplace`), `SuggestedQuestionRepository`, `SuggestedFollowUpRepository` (`findAllInDisplayOrder`) | Sorgular plandaki gibi. |
| service, Kararlar #1, #3, G4 | `suggestion/service/SuggestionService` | `getSuggestions`: üç sorgu, kimliğe göre gruplama. `replaceSuggestions` tek transaction'da: `LOCK TABLE … IN EXCLUSIVE MODE`, `deleteAllInBatch` (DB cascade), sırayla ekleme (metinler `strip()`, `position` gönderilen sıra). Yanıt kaydedilen entity'lerden aynı dönüştürücüyle kurulur. Log: `Suggestions replaced categories={} questions={} followUps={}` (metin loglanmaz). |
| controller | `suggestion/controller/SuggestionController` | `GET`; `PUT` (`consumes = application/json`). Başka method yok, POST'a framework 405 döner. |
| Paket kuralı | `package-info.java` (6 alt paket, `@NullMarked`) | |
| `assistant-api/CLAUDE.md` | Durum yorumu, Flyway listesi + V4 açıklaması, paket yapısı (`suggestion/`), Komutlar: "Önerileri yükleme" duman testi ve dosya biçimi | "Henüz olmayanlar" listesi değişmedi. |

## Değişen dosyalar
Yeni:
- `assistant-api/src/main/resources/db/migration/V4__create_suggestions.sql`
- `assistant-api/src/main/java/com/rasitbank/assistant/suggestion/controller/{SuggestionController,package-info}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/suggestion/service/{SuggestionService,package-info}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/suggestion/repository/{SuggestionCategoryRepository,SuggestedQuestionRepository,SuggestedFollowUpRepository,package-info}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/suggestion/domain/entity/{SuggestionCategory,SuggestedQuestion,SuggestedFollowUp,package-info}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/suggestion/domain/dto/{ReplaceSuggestionsRequest,SuggestionCategoryRequest,SuggestedQuestionRequest,SuggestionSetResponse,SuggestionCategoryResponse,SuggestedQuestionResponse,SuggestionTextNormalizer,package-info}.java`
- `assistant-api/src/main/java/com/rasitbank/assistant/suggestion/domain/validation/{ValidSuggestionSet,SuggestionSetValidator,package-info}.java`

Değişen: `assistant-api/CLAUDE.md`.
Dependency eklenmedi. retrieval-api'ye dokunulmadı.

## Plandan sapmalar
Sözleşme değişmedi. İki uygulama ayrıntısı plana eklendi; lead'e mesajla bildirildi:
1. **`getSuggestions` `isolation = REPEATABLE_READ`** (`readOnly`): üç okuma sorgusu tek anlık görüntüden yapılır. READ_COMMITTED'da sorgular arasında bir `PUT` commit edilirse kategoriler eski listeden, sorular yeni listeden okunabilirdi. Bu durumda listeler karışır, sorular gruplanamazdı. Böylece G4 okuma tarafında da korunur. Yazma tarafı plandaki gibidir (tablo kilidi).
2. **Sadece Unicode boşluğundan oluşan metin** (ör. `" "`, ad/soru/takip): `@NotBlank` sadece `trim()` ile bakar ve bu metni geçirir. Ama kayıttan önceki `strip()` metni boşaltır, DB CHECK'i de 500 döndürürdü. Validator bu durumu aynı yolda `must not be blank` diye raporlar; mesaj ve kural sözleşmeyle aynıdır.

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı.
- `cd assistant-api && ./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE, Tests run: 227, Failures: 1, Errors: 0**. Coverage eşiği kapalıydı, çünkü yeni kodun testleri henüz yok. Tek kırılan test `AssistantApplicationIntegrationTest.givenEmptyDatabase_whenContextStarts_thenFlywayAppliesAllThreeMigrations` (`src/test`, tester'ın dosyası). Test uygulanan migration sayısını `hasSize(3)` diye sabitliyor, planlanan V4 ile sayı 4 oldu. Kod hatası değil. Tester testi "dört migration, sonuncusu `4`" olarak güncellemeli. Diğer 226 test geçiyor. V4, Testcontainers'ta `ddl-auto=validate` ile birlikte sorunsuz uygulandı (bağlam açılan bütün integration testleri geçti).
- Elle duman testi: dev postgres (`docker compose -f deployment/docker-compose.dev.yml up -d`) + `./mvnw spring-boot:run`. Bitince uygulama ve postgres durduruldu, liste `{"categories":[]}` ile boş bırakıldı.
  - İlk `GET` → `{"questionCount":0,"followUpCount":0,"categories":[]}` (B8).
  - `openapi` örneği (+ bilinmeyen alan `extra`, baş/son boşluklu metin) ile `PUT` → 200, metin kırpılmış, `starter` false, `followUps` `[]`; `GET` aynı (B1, B11).
  - Hatalı liste → 400 `VALIDATION_FAILED`, 10 ihlal birlikte: `categories[2]` `must not be null`, `categories[3].questions` `must not be empty`, `categories[1].name` / `categories[1].questions[0].text` / `…followUps[2]` `must be unique`, `…followUps[0]` `must differ from the question`, `…followUps[3]` ve `…text` `must not be blank`. Ardından `GET` önceki set (B3, B4, B6).
  - `{}` → `categories` `must not be null`; 11×50 soru → `categories` `total questions must be at most 500`; 500 soru + 2500 takip → 200 (~0,7 sn).
  - `text/plain` → 415 `UNSUPPORTED_MEDIA_TYPE`; `{` → 400 `BAD_REQUEST`; `"starter":"evet"` → 400 `BAD_REQUEST`; `POST` → 405 `METHOD_NOT_ALLOWED`. Hepsi `application/problem+json` (B9).
  - Eşzamanlı iki `PUT` (500'lük set ve 3'lük set) + `GET`, 3 tur: her `GET` ve son durum tam olarak tek bir set (karışım yok) (B10).

## Tester için notlar
- **Kırılan mevcut test:** `AssistantApplicationIntegrationTest` migration sayısı 3 → 4 (yukarıda).
- **Temizlik:** integration testlerinde `@BeforeEach` içinde `suggestionCategoryRepository.deleteAllInBatch()` yeterli, DB cascade soruları ve takipleri siler. B2'de `suggested_questions` / `suggested_follow_ups` satır sayısını doğrudan `SuggestedQuestionRepository.count()` / `SuggestedFollowUpRepository.count()` ile ölçebilirsin.
- **Validator unit testi (B4, B5):** `Validation.buildDefaultValidatorFactory().getValidator().validate(request)` ile Spring'siz test edilebilir. Yollar Hibernate `ConstraintViolation.getPropertyPath().toString()` biçimindedir. Takip yolunun son düğümü bean düğümü olduğu için HV string'i `categories[0].questions[0].followUps[1]` biçimindedir; `violations[].field` biçimi (Spring `FieldError`) aynıdır. Bunu en güvenli şekilde controller ya da integration testinde `violations[].field` üzerinden doğrula.
- **İhlal sırası** belirli değildir (Bean Validation `Set` döner). Testlerde `violations` içeriği sırasız karşılaştırılmalı (`containsExactlyInAnyOrder`).
- **Normalizasyon (B12):** `SuggestionTextNormalizer.normalize` statik ve saf, doğrudan test edilir.
- **Unicode boşluğu:** `" "` tek başına `@NotBlank`'i geçer, validator `must not be blank` döner (sapma 2). İstersen bir parametre olarak eklenebilir.
- **Eşzamanlılık (B10):** `SuggestionService.replaceSuggestions`'ı iki thread'den `CountDownLatch` ile aynı anda çağır. Kilit `LOCK TABLE suggestion_categories IN EXCLUSIVE MODE` olduğu için ikinci çağrı ilkinin commit'ini bekler. Sonuç sette kategori sayısı 1 ve soru sayısı tam 3 ya da 2 olmalı. Test sınıfı `@Transactional` olmamalı.
- **`getSuggestions` REPEATABLE_READ:** okuma sırasında araya giren bir `PUT`'ı deterministik test etmek zor; B10'un okuma tarafı yeterli.
- Dış model, retrieval ya da Clock kullanılmıyor; bu özellik için sahte bean gerekmez. `@SpringBootTest` için mevcut `PostgresIntegrationTest` tabanı ve reranker sağlık kontrolünü kapatan özellik (`assistant.reranker.health-check-enabled=false`) yeterli.

STATUS: DONE
