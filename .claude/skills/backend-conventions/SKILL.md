---
name: backend-conventions
description: RAG asistan projesinin iki backend servisi (assistant-api, retrieval-api) için bağlayıcı proje kuralları - kod dili (İngilizce), yorum politikası (her sınıfın üstünde kısa Türkçe amaç yorumu zorunlu, metot üstüne yorum yok), sınıf/metot/değişken/paket/REST isimlendirmesi, domain sözlüğü (conversation, turn, retrieval, rerank, generation, citation, evaluation, handoff; retrieval-api: workspace, source, chunk, ingestion, embedding, search), dış modeller (dil modeli, embedding, reranker) ve prompt kuralları, cevap akışı (SSE) kuralları, loglama sınırları, test isimlendirmesi (given_when_then) ve AAA test yapısı, pagination standardı (PageResponse). assistant-api/ ya da retrieval-api/ altında herhangi bir Java kodu veya test yazarken, isimlendirmeye karar verirken, model/prompt/akış kodu yazarken, pagination'lı endpoint eklerken ya da kod review yaparken kullan. Diğer skill'lerdeki örneklerle çelişirse bu skill geçerlidir.
---

# Backend Proje Kuralları

Bu kurallar diğer tüm skill'lerin örneklerinden önce gelir.

> **Durum:** İki servisin iskeleti TASK-002'de kuruldu (uygulama sınıfı, `GlobalExceptionHandler`, sağlık ucu, Flyway); domain kodu henüz yok. Bu skill hedef kuralları yazar; servisin güncel yapısı ve gerçek sürümleri kendi `CLAUDE.md`'sindedir. Kesinleşen her yapısal karar (paket, sınıf adı, property anahtarı) ilk uygulandığı task'ta ilgili servisin `CLAUDE.md`'sine işlenir; burada çelişen bir şey kalırsa planner raporunda belirtir, kullanıcı onayıyla güncellenir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder.
> **Modüller:** Bilgi bankası **bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılır**. `../embedding-pgvector` çalışma zamanı bağımlılığı değildir; sadece okunur kalıp referansıdır: kodu toplu kopyalanmaz ve oraya yazılmaz. `assistant-api`, `retrieval-api`'ye REST ile gider (`POST /api/workspaces/{workspaceId}/search`). Diğer skill'lerdeki `Source`/`Chunk`/`IngestionJob` örnekleri retrieval-api'yi, `Conversation`/`Turn` örnekleri assistant-api'yi anlatır.

## 1. Dil

- Tüm kod İngilizce: sınıf, metot, değişken, paket, enum, sabit, log mesajı, exception mesajı, test adı, commit edilen config anahtarları ve kod içi yorumlar (bölüm 2'nin izin verdiği tek satırlık "neden" yorumu dahil). Tek istisna bölüm 2'deki sınıf yorumudur; o Türkçe yazılır. Skill'lerdeki örneklerde görülen Türkçe yorumlar sadece açıklama içindir; projeye yazılmaz.
- Türkçe karakter veya Türkçe kelime identifier'larda yok (`konusma`, `cevap`, `parca` yanlış → `conversation`, `answer`, `chunk`).
- Domain sözlüğü (asistan):
  - konuşma → `conversation`, konuşma turu (bir soru ve cevabı) → `turn`, müşterinin mesajı → `message` (metni `text`), kanal → `channel` (lab: `PREVIEW`)
  - üretim modu → `generationMode` (`LLM_ONLY`, `RAG`), cevap → `answer`, dil modeli → `chatModel`, sistem talimatı → `systemPrompt`, prompt → `prompt`, modele giden parçalar → `context`, alıntı / kaynak numarası (`[1]`) → `citation`, "rehberde bulamadım" cevabı → `notFoundAnswer`
  - retrieval stratejisi → `retrievalStrategy` (`TOP_K`, `RERANK`; `LLM_ONLY` turunda `NONE`), getirilecek parça sayısı → `topK`, rerank öncesi aday sayısı → `candidates`, yeniden sıralama → `rerank`, reranker modeli → `rerankModel`, reranker skoru → `rerankScore`, takip sorusunun bağımsız hali → `standaloneQuery`, sorgu yeniden yazma → `queryRewrite`, konuşma geçmişi → `history`
  - token sayıları → `promptTokens`, `completionTokens`; toplam süre → `latencyMs`; ilk token süresi → `timeToFirstTokenMs`
  - geri bildirim → `feedback` (`helpful`), temsilciye aktarma → `handoff` (`reason`, ör. `NO_ANSWER`; `queue`)
  - değerlendirme çalıştırması → `evalRun`, soru seti → `dataset` (golden set; cevabın geçtiği parça önceden işaretli soru → `goldenQuestion`), isabet oranı → `recallAtK`, ortalama ters sıra → `mrr`
- Domain sözlüğü (retrieval-api): çalışma alanı → `workspace` (tek ve sabit: 42, "Raşit Bankası · Dijital Asistan"), bilgi kaynağı → `source`, kaynak türü → `sourceKind`, kaynak sürümü → `version`, parça → `chunk`, parçalama → `chunking`, embedding (vektör) → `embedding`, embedding modeli → `embeddingModel`, vektör boyutu → `dimensions`, içe alma işi → `ingestionJob`, içe alma aşaması → `ingestionStage` (`QUEUED`, `PARSING`, `CHUNKING`, `EMBEDDING`, `WRITING`, `DONE`, `FAILED`), arama sorgusu → `query`, arama modu → `searchMode` (`KEYWORD`, `VECTOR`, `HYBRID`; lab sadece `VECTOR` kullanır, diğerlerinin olup olmayacağına ilgili task karar verir), arama sonucu → `searchHit`, benzerlik skoru → `score`, mesafe → `distance`.
- `handoff` kelimesi kodda her zaman "temsilciye aktarma"dır. Ekip iş akışındaki `handoff/` dosyalarıyla ilgisi yoktur; kodda, logda ve sözleşmede o anlamda kullanılmaz.
- Kullanıcıya gösterilen metinler bu kuralın dışındadır; dili kök `CLAUDE.md`'deki genel kurallar belirler. Orada aksi yazmadıkça: backend hata response'unda `detail` İngilizce kalır, istemci kararını sabit `errorCode` üzerinden verir ve kullanıcıya gösterilecek Türkçe metni frontend `errorCode`'a göre üretir (bkz. `problem-details-rfc9457`). Dil modelinin ürettiği cevap ve prompt metinleri kod değil içeriktir; dilleri bölüm 7'dedir.

## 2. Yorum politikası

- Her üst seviye tipin (class, interface, record, enum, annotation; main ve test) üstünde **Türkçe**, 1-2 satırlık bir Javadoc (`/** ... */`) bulunur. Sınıfın ne işe yaradığını ve ne için kullanıldığını söyler; alanları, metotları veya uygulama adımlarını saymaz. `@author`, `@since` gibi tag yok.
  ```java
  /**
   * Müşterinin sorusunu rehberdeki parçalarla cevaplar ve cevabı
   * token token akıtır.
   */
  @Service
  public class AnswerService {
  ```
- Metot üstüne yorum **yazılmaz**: Javadoc (`/** ... */`) yok, `//` veya `/* */` ile metot açıklaması yok. İç içe (nested) tiplere yorum yazılmaz.
- Kod kendini isimlerle anlatır. Açıklama gerekiyorsa önce metodu/değişkeni yeniden adlandır veya küçük bir metoda ayır.
- Metot gövdesinde yorum sadece koddan çıkarılamayan bir **neden** için ve tek satır (ör. bir workaround'un sebebi). Ne yaptığını anlatan yorum yok.
- Testlerdeki `// Arrange`, `// Act`, `// Assert` işaretleri bu kuralın istisnasıdır (bölüm 5).
- Yorum satırına alınmış (commented-out) kod bırakılmaz.

## 3. İsimlendirme

### Sınıflar (PascalCase, isim)

| Rol | Kalıp | Örnek |
|-----|-------|-------|
| Entity / domain | Tekil isim, ek yok | `Conversation`, `Turn` (konuşmanın turu; `ConversationTurn` değil), `EvalRun` |
| REST controller | `<Feature>Controller` | `ConversationController`, `HandoffController`, `EvalRunController` |
| Service | `<Feature>Service` ya da işi anlatan isim | `ConversationService`, `AnswerService`, `RetrievalEvaluator` |
| Repository | `<Entity>Repository` | `ConversationRepository`, `TurnRepository` |
| Request DTO | `<Verb><Noun>Request` (JSON gövde) | `SendMessageRequest`, `SubmitFeedbackRequest`, `StartEvalRunRequest` |
| Response DTO | `<Noun>Response` (fiil yok: `CreateConversationResponse` değil) | `ConversationResponse`, `TurnResponse`, `EvalRunResponse` |
| Akış olayı (SSE `data`) | `<Noun>Event` | `TokenEvent`, `SourcesEvent`, `DoneEvent`, `StreamErrorEvent` |
| WebSocket payload | `<Noun>Message` (WebSocket gelirse) | `IngestionProgressMessage` |
| Domain event | `<Noun><PastVerb>Event` (Spring event; akış olayıyla karışmasın diye sadece geçmiş zaman) | `TurnCompletedEvent` |
| Exception | Sadece HTTP kategorisi: `<Category>Exception`. Hata başına sınıf açılmaz; hatayı `errorCode` ayırt eder | `NotFoundException`, `ConflictException`, `ServiceUnavailableException` |
| Config / properties | `<Topic>Config`, `<Topic>Properties` | `AssistantProperties`, `ChatModelConfig` |
| Dış servis istemcisi | `<Topic>Client` | `RetrievalClient`, `RerankClient` (assistant-api), `EmbeddingClient` (retrieval-api) |
| Prompt kurucu / dönüştürücü | `<Noun>Builder`, `<Noun>Rewriter` | `PromptBuilder`, `QueryRewriter` |
| Mapper / listener / interceptor | `<Noun>Mapper`, `<Noun>Listener`, `<Noun>Interceptor` | `TurnMapper` |

- Spring AI'ın kendi tipleri (`ChatClient`, `ChatModel`, `Prompt`, `Message`) adlarıyla kullanılır; aynı adla proje sınıfı açılmaz (`ChatClient` adında bir proje interface'i yok).
- `*Impl` sadece aynı interface'in birden fazla implementasyonu varsa (ör. `RerankClient` için gerçek reranker istemcisi ve testte deterministik sahte istemci). Tek implementasyon için interface açma.
- `Manager`, `Helper`, `Util`, `Processor`, `Data`, `Info` gibi belirsiz isimler yok.

### Metotlar (camelCase, fiil ile başlar)

| Anlam | Kalıp | Örnek |
|-------|-------|-------|
| Oluştur | `create<Noun>` | `createConversation` |
| Tekil getir, yoksa exception | `get<Noun>` | `getConversation(conversationId)` |
| Tekil getir, yoksa boş | `find<Noun>` → `Optional` | `findTurn(turnId)` |
| Liste / sayfa | `list<Nouns>` | `listConversations(page, size)`, `listEvalRuns(page, size)` |
| Güncelle | `update<Noun>` veya domain fiili | `submitFeedback`, `requestHandoff`, `completeTurn` |
| Sil | `delete<Noun>` | `deleteConversation` |
| Cevap / akış | domain fiili | `sendMessage`, `answer`, `streamAnswer` |
| Retrieval | `retrieve<Noun>`, `rerank<Nouns>`, `rewrite<Noun>` | `retrieveContext`, `rerankChunks`, `rewriteQuery` |
| Değerlendirme | `start<Noun>`, `evaluate<Noun>` | `startEvalRun`, `evaluateQuestion` |
| Boolean | `is`, `has`, `can`, `exists` | `isFinished()`, `hasCitations()`, `canHandoff()` |
| Dönüşüm | `to<Type>`, statik `from(...)`, `of(...)` | `TurnResponse.from(turn)` |

- Controller metot adı, çağırdığı service metodu ile aynı.
- Spring Data repository metotları derived query isimlendirmesine uyar (`TurnRepository.findByConversationIdOrderByCreatedAtAsc`); property path entity alanlarıyla birebir eşleşmelidir. Türetilmiş getter'ı olan alanlarda (`Turn.getConversationId()` aslında `conversation.id`) Spring Data yanlış özelliği bulur; bu sorgular açık JPQL `@Query` ile yazılır. Derived query ile yazılamayan sorgular (vektör benzerliği, FTS, `array` kolonları) `@Query(nativeQuery = true)` ile yazılır (bölüm 3 "Veritabanı").
- Kısaltma yok: `conversation` (`conv` değil), `request` (`req` değil), `embedding` (`emb`/`vec` değil), `rerank` (`rr` değil), `exception` (`e`/`ex` sadece tek satırlık lambda'da kabul).
- Koleksiyonlar çoğul (`turns`, `chunks`, `citations`), boolean'lar `is/has/can` ile (`isHelpful`).
- Sabitler `UPPER_SNAKE_CASE`. Magic number yok: `topK`, `candidates`, `temperature`, `numCtx`, zaman aşımları ve model adları `AssistantProperties`'ten (`assistant.*`) gelir (bölüm 7).

### Paketler

Paket adları küçük harf ve tekil. Önce özellik, özelliğin içinde katman.

**assistant-api** (`com.rasitbank.assistant`, lab logları `c.r.assistant.*`; uygulama sınıfı `AssistantApplication`):

```
com.rasitbank.assistant
├── conversation/            Konuşma, tur, mesaj gönderme, geri bildirim
│   ├── controller/          ConversationController
│   ├── service/             ConversationService, AnswerService
│   ├── repository/          ConversationRepository, TurnRepository
│   └── domain/
│       ├── entity/          Conversation, Turn, GenerationMode, RetrievalStrategy
│       ├── dto/             Request ve Response record'ları
│       └── event/           Akış olayları (TokenEvent, SourcesEvent, DoneEvent) ve domain event'leri
├── retrieval/               client/ (RetrievalClient, RerankClient), service/ (ContextRetriever, QueryRewriter)
├── generation/              PromptBuilder, sistem talimatları, dil modeli çağrısı (Spring AI ChatClient)
├── handoff/                 Temsilciye aktarma (HandoffController, HandoffService)
├── evaluation/              EvalRun, RetrievalEvaluator, golden set yükleme
├── pagination/              PageResponse
├── config/                  AssistantProperties, ClockConfig, OpenApiConfig
└── exception/               GlobalExceptionHandler, BaseException ve kategori exception'ları
```

**retrieval-api** (`com.rasitbank.retrieval`; uygulama sınıfı `RetrievalApplication`): `workspace/`, `source/`, `chunk/`, `ingestion/`, `search/`, `embedding/`, `pagination/`, `config/`, `exception/`; özellik içi yapı (`controller/`, `service/`, `repository/`, `domain/…`) aynıdır.

- İki servis kod paylaşmaz; `PageResponse`, `PageParameters`, exception kategori sınıfları her serviste kendi kök paketindedir. Ortak Maven modülü ya da paylaşılan kütüphane yoktur.
- Özellik paketinde exception paketi yoktur; hatalar kökteki `exception` paketindeki kategori sınıflarıyla fırlatılır (yukarıdaki isimlendirme tablosu, `problem-details-rfc9457`).
- Kökte katman-first paket (`controllers`, `services`, lab'daki `api/`) ve `common` paketi açılmaz. Birden fazla özelliğin kullandığı DTO (ör. `PageResponse`) ilk ihtiyaç duyan task'ta kökte kendi paketinde açılır (`pagination/`).
- İhtiyacı olmayan alt paket boş olarak açılmaz.
- Her paketin `package-info.java` dosyası `@NullMarked` taşır.
- Testler aynı paket yapısıyla `src/test/java` altındadır; test, test ettiği sınıfla aynı pakettedir.

### REST ve JSON

- Path: `/api/<çoğul-kebab-case>`; assistant-api `/api/assistant/**`, retrieval-api `/api/workspaces/{workspaceId}/**` (lab). Versiyon öneki (`/api/v1`) yok. Fiil path'te değil HTTP method'unda. Lab'daki adresler referanstır: `POST /api/assistant/conversations/{conversationId}/messages` (cevap SSE ile akar), `POST /api/assistant/turns/{turnId}/feedback`, `POST /api/assistant/conversations/{conversationId}/handoff`, `POST /api/assistant/evals` + `GET /api/assistant/evals/{evalRunId}`. Kesin adresler `docs/api/openapi.yaml`'dadır.
- Path variable ve query param isimleri camelCase (`{conversationId}`, `{turnId}`). Kimlik her yerde (REST path, SSE olayı, `@PathVariable` parametresi, DTO alanı) `<entity>Id` adını taşır (`turnId`); entity alanı `Turn.id` olarak kalır. Pagination için sadece standart `page`, `size` kullanılır (bölüm 6).
- JSON alanları camelCase. Tarih/saat ISO-8601 (`Instant`). Enum değerleri `UPPER_SNAKE_CASE`.
- Uzun süren işler (içe alma, değerlendirme çalıştırması) `202 Accepted` + `Location` döner; durum ayrı bir `GET` ile sorgulanır.
- Response şekillerinin tek doğru kaynağı `docs/api/openapi.yaml`'dır; SSE olaylarının adları ve payload'ları `docs/api/sse.md`'dedir. DTO record'ları bunlara birebir uyar ve aynı kavram her endpoint'te aynı DTO ile döner.
- Vektörün kendisi response'a konmaz; gerekiyorsa sadece boyutu ve model etiketi döner (`embeddingModel`, `dimensions`).

### Veritabanı

Tek PostgreSQL sunucusu, iki ayrı veritabanı: `assistant` (assistant-api) ve `retrieval` (retrieval-api); her servis kendi kullanıcısıyla sadece kendi veritabanına bağlanır, kendi Flyway migration'larını taşır, diğerinin tablosunu okumaz. Tablolar (lab): assistant `conversations`, `conversation_turns`, `eval_runs`; retrieval `workspaces`, `sources`, `chunks`, `ingestion_jobs`; "hangi sorguyla hangi parça geldi" assistant-api'nin tur kaydındadır. snake_case, çoğul tablo adları. PK `bigint` identity (dışarıya tahmin edilemez kimlik gerekiyorsa plan kararıyla `uuid`), zaman `timestamptz` (Java'da `Instant`), metin `text` + `CHECK`. Ayrıntı: `postgres-patterns`; migration: `flyway-migrations`.

- Her tur, cevabın neye dayandığını sonradan inceleyebilmek için modele giden parçaların kimliklerini (`chunk_ids`), cevapta alıntılanan numaraları (`cited`), arama sorgusunu (`search_query`, yeniden yazıldıysa), retrieval stratejisini, token sayılarını ve süreyi saklar. "Doğru parça modele gitti mi?" sorusu bu satırdan cevaplanır.
- retrieval-api'de (vektörler sadece `retrieval` veritabanındadır): `CREATE EXTENSION IF NOT EXISTS vector;` ilk migration'da; embedding kolonu boyutuyla (`vector(1024)`, `bge-m3`) ve üreten model `embedding_model` kolonunda; cosine mesafe `<=>` + HNSW `vector_cosine_ops`; arama sadece aktif modelin satırlarında (`postgres-patterns`).
- Entity'ler mutable'dır ve `protected` no-arg constructor taşır. Değişiklik domain metoduyla yapılır: `conversation.addTurn(...)` → eklenen `Turn`, `turn.complete(...)`, `turn.markHelpful(...)`.
- "Kontrol et + değiştir" atomikliği gereken yerde (aynı tura iki geri bildirim, aynı konuşma için iki aktarma) transaction + kilit ya da unique index kullanılır (`spring-transactions`).

## 4. Kod stili

- Constructor injection; `@Autowired` field injection yok (istisna: test sınıflarında `@Autowired` / `@MockitoBean` field'ları kullanılır). Lombok kullanılmaz.
- DTO'lar `record`. Entity'ler controller'dan dışarı çıkmaz.
- `Optional` sadece dönüş tipi olarak; parametre ve field olarak yok.
- Logger: `private static final Logger log = LoggerFactory.getLogger(X.class);`. Mesaj formatı: İngilizce cümle + `key=value` alanları (`log.info("Turn completed conversationId={} turnId={} strategy={} promptTokens={} latencyMs={}", ...)`). Ayrıntı `observability-tracing`.
- **Loglanmayanlar:** müşterinin sorusu, yeniden yazılmış sorgu, cevap metni, parça içeriği, prompt'un kendisi ve `Authorization` başlığı. Bunlar kullanıcı ve banka verisidir; sadece kimlikler, uzunluklar, token sayıları, süreler ve strateji loglanır. Prompt'u görmek gerekiyorsa tura kaydedilen alanlardan bakılır.
- Yapılandırma `application.properties` (+ profile'a özel `application-<profile>.properties`) ile yapılır; YAML kullanılmaz. Skill'lerdeki YAML örnekleri aynı anahtarların properties karşılığı olarak okunur. Gizli bilgi properties'e yazılmaz, ortam değişkeninden gelir. İstisna: compose dosyalarında da düz yazılı olan local veritabanı şifresi (`retrieval-local`, `assistant-local`) `${DATABASE_PASSWORD:<db>-local}` varsayılanı olarak yazılır; servis IDE'den ortam değişkeni vermeden açılır.
- Girinti 4 boşluk. Null anotasyonları JSpecify (`@NullMarked`, `@Nullable`).
- Zaman `Clock` bean'inden okunur: `config` paketindeki `ClockConfig`'te `@Bean Clock clock() { return Clock.systemUTC(); }` tanımlıdır, kod `Instant.now(clock)` kullanır (`Instant.now()` değil). Süre ölçümü (latency, ilk token süresi) `System.nanoTime()` ile yapılabilir; iş kuralına giren zaman her zaman `Clock`'tan okunur. Testte `FixedClockTestConfiguration` ile sabitlenir (`spring-boot-engineer/references/testing.md`).
- `@Component` yerine uygun stereotype: iş mantığı `@Service`, veri `@Repository` (Spring Data interface'lerinde gerekmez), HTTP `@RestController`. Listener, interceptor, mapper gibi altyapı sınıfları `@Component` olabilir.
- JSON için Jackson 3 (`tools.jackson.databind.json.JsonMapper`); `com.fasterxml.jackson.databind.ObjectMapper` kullanılmaz (anotasyonlar `com.fasterxml.jackson.annotation` paketinde kalır).
- Hata response'ları: `problem-details-rfc9457` (`spring.mvc.problemdetails.enabled=true`; varsayılanı `false`).

## 5. Testler

### İsimlendirme

Format: `given<Precondition>_when<Action>_then<ExpectedResult>`

```
givenUnknownConversationId_whenSendMessage_thenThrowsConversationNotFound
givenLlmOnlyMode_whenAnswer_thenDoesNotCallRetrieval
givenFollowUpQuestion_whenRetrieveContext_thenSearchesWithStandaloneQuery
givenRerankStrategy_whenRetrieveContext_thenSendsTopFiveOfTwentyCandidatesInRerankOrder
givenFinishedTurn_whenStreamAnswer_thenEmitsTokenSourcesAndDoneEventsInOrder
```

- Her parça PascalCase kelimelerle, parçalar arasında tek `_`.
- `when` kısmı test edilen metodu/aksiyonu, `then` kısmı gözlemlenen sonucu söyler.
- `@DisplayName` kullanılmaz; metot adı yeterli.
- Test sınıfları: unit → `<ClassUnderTest>Test`, integration → `<ClassUnderTest>IntegrationTest` (ikisi de surefire tarafından çalıştırılır).

### Gövde yapısı: AAA

```java
@Test
void givenUnknownConversationId_whenSendMessage_thenThrowsConversationNotFound() {
    // Arrange
    given(conversationRepository.findById(7L)).willReturn(Optional.empty());
    SendMessageRequest request = new SendMessageRequest("Gold kartın yıllık aidatı ne kadar?", Channel.PREVIEW);

    // Act
    Throwable thrown = catchThrowable(() -> conversationService.sendMessage(7L, request));

    // Assert
    assertThat(thrown).isInstanceOf(NotFoundException.class)
            .extracting("errorCode").isEqualTo("CONVERSATION_NOT_FOUND");
    then(retrievalClient).shouldHaveNoInteractions();
    then(chatModel).shouldHaveNoInteractions();
}
```

- Üç bölüm her zaman `// Arrange`, `// Act`, `// Assert` yorumlarıyla ayrılır; Arrange gerekmiyorsa atlanabilir.
- Hata beklenen testte hem kategori sınıfı hem `errorCode` doğrulanır; sadece sınıfa bakmak iki farklı 404'ü ayırt etmez.
- Act bölümü tek bir aksiyon içerir. Exception beklenen durumda `catchThrowable` ile yakala, assert'i Assert bölümünde yap (`assertThatThrownBy` Act ile Assert'i birleştirdiği için kullanılmaz).
- Bir test = bir davranış. Birden fazla assertion aynı davranışı doğruluyorsa serbest.
- Mockito BDD stili: `given(...).willReturn(...)`, `then(mock).should(...)`. `when(...)`/`verify(...)` yok.
- Assertion'lar AssertJ (`assertThat`). JUnit `assertEquals` yok. Skor ve oran karşılaştırmalarında (`rerankScore`, `recallAtK`, `mrr`) `isCloseTo(expected, within(1e-6))`; `double` eşitliği `isEqualTo` ile doğrulanmaz. Beklenen oran testte fixture'dan elle hesaplanır (ör. 5 sorudan 4'ünde altın parça ilk 5'te → `0.8`).
- Test verisi için `<Entity>Fixtures` sınıflarında statik factory metotları (`ConversationFixtures.conversationWithTurns(...)`, `ChunkFixtures.candidates(20)`). Builder sınıfı veya `TestDataFactory` açılmaz.
- `Thread.sleep()` yok (bkz. `java-concurrency`); akış testlerinde olaylar toplanıp sırasıyla doğrulanır (Reactor kullanılıyorsa `StepVerifier`, `SseEmitter` kullanılıyorsa olayları kaydeden sahte emitter).

### Dış modeller testte

- Unit ve integration testleri gerçek dil modeline, embedding modeline, reranker'a ya da retrieval servisine gitmez; lokal Ollama kapalıyken de `./mvnw verify` geçer.
- Her dış model için deterministik sahte verilir: dil modeli için sabit token dizisi dönen sahte `ChatModel` (Spring AI), reranker için girdiden hesaplanabilir sıralama dönen sahte `RerankClient`, retrieval için fixture parçaları dönen sahte `RetrievalClient`.
- retrieval-api testleri gerçek embedding modeline gitmez; sahte `EmbeddingClient` sabit (girdiden deterministik) vektör döner.
- Prompt testleri modele giden mesajları yakalayıp doğrular (sistem talimatı, geçmiş, numaralı parçalar, soru); cevabın kalitesi değil, modele doğru bağlamın gittiği test edilir. Cevabın kalitesi değerlendirme (eval) ve qa'nın gerçek modelle denemesinin konusudur.

### Kapsam

- Unit test: `@ExtendWith(MockitoExtension.class)`, Spring context yok; her iş kuralı için en az bir test (görev dosyasındaki her kural ve örnek senaryo).
- `@SpringBootTest` integration testlerine `@Transactional` eklenmez; veri her testten **önce** temizlenir (`@BeforeEach`). İstisna: `@DataJpaTest` kendi test-başına rollback'ini getirir ve izole sorgu testi için olduğu gibi kullanılır (flush + clear zorunlu); ayrıntı `spring-jpa-testing`.
- Coverage hedefi: satır bazında en az %80 (JaCoCo `check` ile zorunlu, bkz. `springboot-verification`).
- Controller testi: `@WebMvcTest` + `MockMvcTester` (`spring-mvc-testing`). SSE endpoint'inde async dispatch'i bekleyip `text/event-stream` gövdesindeki olay sırası doğrulanır.
- Repository / integration: Testcontainers 2.x + `@ServiceConnection`, production'la aynı PostgreSQL imajı; H2 kullanılmaz (`spring-jpa-testing`).
- WebSocket gelirse: `spring-websocket-testing`.
- Spring Boot 4: `@MockitoBean` (`@MockBean` yok).

## 6. Pagination standardı

- Query param'lar: `page` (0'dan başlar, en az 0) ve `size` (1-100; varsayılan 20). `sort` alınmaz: her listenin sırası sabittir ve repository sorgusunda yazılıdır (ör. konuşmalar ve değerlendirme çalıştırmaları `created_at DESC, id DESC`, bir konuşmanın turları `created_at, id`).
- Geçersiz `page`/`size` sessizce düzeltilmez, `VALIDATION_FAILED` + `violations[].field = "page"`/`"size"` döner. Bu yüzden Spring'in `Pageable` çözümleyicisi (`@PageableDefault`, `max-page-size`; aşan değeri sessizce kırpar) kullanılmaz; parametreler `@RequestParam` + Bean Validation ile alınır, mesaj ve sınırlar `pagination/PageParameters`'tadır.
- Katman sorumluluğu: service `Page<XResponse>` döndürür (`PageRequest.of(page, size)` + repository'nin sabit sıralı sorgusu; entity → DTO dönüşümü service'te), controller `PageResponse.from(...)` ile sarar:

```java
@GetMapping
public PageResponse<ConversationResponse> listConversations(
        @RequestParam(name = "page", defaultValue = "0")
        @Min(value = 0, message = PageParameters.PAGE_MESSAGE) int page,
        @RequestParam(name = "size", defaultValue = "20")
        @Min(value = 1, message = PageParameters.SIZE_MESSAGE)
        @Max(value = PageParameters.MAX_SIZE, message = PageParameters.SIZE_MESSAGE) int size) {
    return PageResponse.from(conversationService.listConversations(page, size));
}
```

- Zamanla sınırsız büyüyebilen koleksiyonları dönen liste endpoint'leri (konuşmalar, değerlendirme çalıştırmaları, kaynaklar, parçalar) her zaman sayfalıdır. Retrieval sonucu sayfalı değildir: `topK` / `candidates` ile sınırlı düz `List` döner.
- `Page<T>` doğrudan döndürülmez; proje response tipi:

```java
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
```

- Toplam sayı gerekmiyorsa (sonsuz kaydırma) repository `Slice<T>` döner (count sorgusu yok), controller `SliceResponse<T>(content, page, size, hasNext)` ile sarar (`spring-boot-openapi-documentation/references/pagination-support.md`).
- İleride bir liste sıralanabilir olursa alanları whitelist'le; istemcinin rastgele kolonla sıralamasına izin verme. Bu bir sözleşme değişikliğidir (planner).

## 7. Dış modeller, prompt ve cevap akışı

Ayrıntı: `rag-pipeline` (sorgu zamanı; içe alma ve embedding `references/indexing.md`), `sse-streaming`, `rag-evaluation`.

### Dış modeller

- Embedding modeli retrieval-api'nindir; dil modeli ve reranker assistant-api'nin.
- Dil modeli, embedding modeli ve reranker dış servistir ve lokal çalışır (lab: Ollama `qwen3.5:9b` + `bge-m3`, reranker `bge-reranker-v2-m3` llama-server üzerinde). Ücretli dış API'ye istek atılmaz; model seçimi kullanıcı onayıyla kesinleşir.
- Model adları, adresler, `temperature`, `num_ctx`, `topK`, `candidates`, üretim modu, retrieval stratejisi, sorgu yeniden yazmanın açık/kapalı olması ve zaman aşımları kodda değil `AssistantProperties`'te (`assistant.*`, lab: `assistant.generation`, `assistant.retrieval`, `assistant.query-rewrite`) ya da Spring AI'ın kendi anahtarlarında (`spring.ai.*`) durur; ortam değişkeniyle değiştirilebilir.
- Her dış çağrının bağlantı ve okuma zaman aşımı vardır. Servise ulaşılamazsa backend çökmez; sabit bir `errorCode` döner (`ServiceUnavailableException`, 503; ör. `CHAT_MODEL_UNAVAILABLE`, `RETRIEVAL_UNAVAILABLE`, `RERANKER_UNAVAILABLE`). Bir servis kapalıyken hangi davranışın seçileceği (hata mı, yedek yol mu; lab: reranker kapalıyken vektör sırasının ilk 5'i) iş kuralıdır ve TASK'ta / planda yazar.
- Dış çağrı hiçbir veritabanı transaction'ı açıkken yapılmaz (`spring-transactions`).

### Prompt

- Sistem talimatları ve prompt şablonları kod içinde dağınık string olarak durmaz; `src/main/resources/prompts/` altında dosya olarak (`<name>.st`, Spring AI şablon biçimi) tutulur ve `generation` paketinden yüklenir. Metinlerin dilini TASK belirler (lab: sistem talimatı Türkçe, sorgu yeniden yazma şablonu İngilizce).
- Modele giden parçalar `[1]`…`[n]` diye numaralanır; numara ile parça kimliği eşlemesi tura kaydedilir. Cevaptaki alıntı numaraları bu eşlemeyle parçaya çevrilir; aralık dışı numara alıntı sayılmaz.
- Takip sorusunda arama `standaloneQuery` ile yapılır; modele giden prompt'ta müşterinin kendi sorusu ve geçmiş kalır.
- Kullanıcı girdisi ve parça içeriği talimat değil veridir: şablonda ayrı bir bölümde (ör. `<parcalar>` etiketi içinde) durur, sistem talimatına eklenmez (`springboot-security`, prompt injection).

### Cevap akışı (SSE)

- Cevap Server-Sent Events ile akar (`text/event-stream`). Olay adları küçük harf (lab: `token`, `sources`, `done`; hata için `error`), `data` alanı JSON ve camelCase'tir; sözleşme `docs/api/sse.md`'dedir.
- Akış başlamadan oluşan hata (doğrulama, bulunamayan konuşma, model kapalı) normal HTTP status + Problem Details ile döner. Akış başladıktan sonra oluşan hata `event: error` + `{"errorCode": ...}` ile bildirilir ve akış kapanır; yarım kalan tur kaydı bu durumu taşır.
- İstemci bağlantıyı kapatırsa dil modeli çağrısı iptal edilir; boşa token üretilmez.
- Tur kaydı (`done` olayındaki `turnId`, token sayıları, süre) akış bittiğinde yazılır; `done` olayı kayıt commit edildikten sonra gönderilir.

## Kontrol listesi

```
- [ ] Tüm identifier ve log mesajları İngilizce; logda soru, cevap, parça içeriği, prompt ve Authorization yok
- [ ] Her üst seviye tipin üstünde 1-2 satırlık Türkçe amaç Javadoc'u var; metot üstünde yorum veya Javadoc yok, commented-out kod yok
- [ ] Sınıf ve metot isimleri bölüm 3'teki kalıplara ve domain sözlüğüne uyuyor; kodda `handoff` sadece temsilciye aktarma anlamında
- [ ] Sınıf doğru pakette (özellik → controller/service/repository/domain; ortak kod kökte config/exception/pagination); hata başına exception sınıfı yok
- [ ] Model adı, eşik, topK/candidates, temperature, zaman aşımı kodda değil properties'te; dış çağrılarda zaman aşımı var, transaction açıkken dış çağrı yok
- [ ] Prompt şablonları resources/prompts altında; kullanıcı girdisi ve parçalar talimattan ayrı bölümde
- [ ] SSE olay adları ve payload'ları docs/api/sse.md ile aynı; akış öncesi hata Problem Details, akış içi hata `error` olayı
- [ ] Her tur modele giden parça kimliklerini, alıntıları, stratejiyi, token ve süreyi saklıyor
- [ ] Testler given_when_then isimli ve AAA yapısında; gerçek modele / retrieval servisine giden test yok
- [ ] Sınırsız büyüyebilen liste endpoint'leri PageResponse döndürüyor (bölüm 6)
- [ ] Lombok, `@Autowired` field injection, `@MockBean`, `@DisplayName`, Jackson 2 `ObjectMapper` yok
```
