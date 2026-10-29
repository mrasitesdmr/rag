---
name: problem-details-rfc9457
description: >
  Spring Boot 4'te exception mapping veya RFC 9457 error response'ları uygularken kullan.
  REST hata response'ları, domain exception'ları, validation hataları ve bunların HTTP status/errorCode eşlemesi için tek doğru kaynak.
---

# Problem Details - RFC 9457

## Error contract'ı

Bu projenin tüm REST hataları RFC 9457 `ProblemDetail` (`application/problem+json`) formatındadır. API başına tek bir error policy vardır; success DTO'ları ayrı tutulur (RFC 9457 sadece error'ları tanımlar). Mevcut bir legacy API'ye eklenirken migration istenmedikçe eski contract korunur; bu projede legacy contract yoktur.

```properties
spring.mvc.problemdetails.enabled=true
```

Boot 4.1'de bu property'nin varsayılanı `false`'tur. Property, framework exception'ları için Boot'un kendi `ResponseEntityExceptionHandler`'ını açar; projede kendi `GlobalExceptionHandler`'ımız (yine `ResponseEntityExceptionHandler`'ı extend eder) bulunduğunda Boot'unki geri çekilir ve tüm eşleme bizim sınıfımızdan yapılır. Property yine de açık tutulur ki advice yüklenmeyen durumlarda da format aynı kalsın.

## Domain error'ları

Hata başına exception sınıfı açılmaz. Hatalar HTTP kategorisine göre ortak sınıflarla fırlatılır; hangi hata olduğunu `errorCode` söyler. Bütün sınıflar her servisin kendi kök paketindeki `exception` paketindedir (`com.rasitbank.assistant.exception`, `com.rasitbank.retrieval.exception`); iki servis bu sınıfları paylaşmaz. Şablonlar assistant-api paketiyle yazılıdır:

- [BaseException](templates/BaseException.java): abstract taban; `errorCode` (String) ve HTTP status taşır.
- Kategori sınıfları, her biri sabit status'uyla: [NotFoundException](templates/NotFoundException.java) (404), [ConflictException](templates/ConflictException.java) (409), [UnauthorizedException](templates/UnauthorizedException.java) (401), [ForbiddenException](templates/ForbiddenException.java) (403), [ServiceUnavailableException](templates/ServiceUnavailableException.java) (503; dil modeli, embedding modeli, reranker ya da diğer servis kullanılamadığında). Constructor `(String errorCode, String message)` alır.
- [GlobalExceptionHandler](templates/GlobalExceptionHandler.java): tek `@RestControllerAdvice`. `BaseException`'ı taşıdığı status ve `errorCode` ile map'ler; yeni hata için handler'a metot eklenmez.

`errorCode` fırlatıldığı yerde string olarak verilir; enum ya da sabit sınıfı yoktur:

```java
throw new ConflictException("INGESTION_IN_PROGRESS", "Source has an active ingestion job: " + sourceId);
```

Örnek eşleme (kesin liste `docs/api/openapi.yaml`):

| Servis | Kategori | Status | `errorCode` | Kural |
|--------|----------|--------|-------------|-------|
| assistant-api | `NotFoundException` | 404 | `CONVERSATION_NOT_FOUND` | Var olmayan konuşma |
| assistant-api | `ConflictException` | 409 | `TURN_IN_PROGRESS` | Konuşmada cevabı hâlâ akan bir tur varken yeni soru |
| assistant-api | `ConflictException` | 409 | `FEEDBACK_ALREADY_SUBMITTED` | Aynı tura ikinci geri bildirim |
| assistant-api | `ServiceUnavailableException` | 503 | `CHAT_MODEL_UNAVAILABLE` | Dil modeli yanıt vermiyor (akış başlamadan) |
| assistant-api | `ServiceUnavailableException` | 503 | `RETRIEVAL_UNAVAILABLE` | retrieval-api'ye ulaşılamıyor |
| assistant-api | `ServiceUnavailableException` | 503 | `RERANKER_UNAVAILABLE` | Reranker kapalı ve TASK yedek yol yerine hata istiyor |
| retrieval-api | `NotFoundException` | 404 | `WORKSPACE_NOT_FOUND` | Var olmayan çalışma alanı (sadece 42 vardır) |
| retrieval-api | `NotFoundException` | 404 | `SOURCE_NOT_FOUND` | Var olmayan kaynak |
| retrieval-api | `ConflictException` | 409 | `INGESTION_IN_PROGRESS` | Kaynağın aktif içe alma işi varken yeni sürüm yükleme |
| retrieval-api | `ConflictException` | 409 | `EMBEDDING_MODEL_MISMATCH` | Yüklemenin ya da sorgu vektörünün modeli çalışma alanının modeliyle (`bge-m3@1024`) uyuşmuyor |
| retrieval-api | `ServiceUnavailableException` | 503 | `EMBEDDING_UNAVAILABLE` | Embedding modeli yanıt vermiyor ya da 5xx dönüyor (retry'lar tükendikten sonra) |
| İkisi | Bean Validation (`@Valid` gövde ve `@RequestParam`/`@PathVariable` kısıtları; `MethodArgumentNotValidException` ve `HandlerMethodValidationException`) | 400 | `VALIDATION_FAILED` + `violations` (`field` = gövde alanı ya da parametre adı) | Soru metni 2-1000 karakter (assistant-api); arama metni boş olamaz, `limit` 1-50 (retrieval-api) |
| İkisi | Framework hatası (bozuk JSON, desteklenmeyen method/media type, olmayan path) | Spring'in verdiği status (400, 404, 405, 406, 415...) | HTTP status adı: `BAD_REQUEST`, `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE` | — |
| İkisi | Beklenmeyen hata | 500 | `INTERNAL_ERROR` | — |

- Her public class kendi dosyasındadır. Feature paketlerinde (`source`, `search` vb.) exception sınıfı yoktur.
- Yeni bir kategori (ör. 422 için `UnprocessableContentException`) sadece ihtiyaç duyan task'ta, `BaseException`'dan türeyen bir sınıf olarak eklenir.
- 503 cevabına `Retry-After` header'ı eklenebilir; model sunucusunun hata gövdesi (iç detay, model adı, adres) `detail`'e kopyalanmaz.
- Cevap akışı başladıktan sonraki hata Problem Details değil, SSE `error` olayıdır; aynı `errorCode` kullanılır (`sse-streaming`).
- Bunlar HTTP status metadata'sı taşıyan, API'ye dönük exception'lardır. Rastgele persistence veya infrastructure exception mesajlarını dışarı açma (`DataIntegrityViolationException`'ı service'te domain hatasına çevir, bkz. `spring-transactions`).
- Durum çakışması (kaynak limiti dolu, kaynak adı alınmış, kaynak işleniyor) 409, sözdizimsel olarak geçerli ama iş kuralına aykırı istek 422'dir. Spring Framework 7'de 422'nin sabiti `HttpStatus.UNPROCESSABLE_CONTENT`'tir (`UNPROCESSABLE_ENTITY` deprecated).

Advice, Spring'in framework exception handling'ini korumak için `ResponseEntityExceptionHandler`'ı extend eder. Framework exception'larının `ProblemDetail` body'si `createResponseEntity` override'ında `type` ve status adından türetilen `errorCode` ile tamamlanır; böylece istisnasız her hata response'u `errorCode` taşır ve frontend bilmediği kodlar için genel mesajı gösterir. Validation alan bazlı `violations` ile 400 döner; beklenmeyen hatalar generic bir 500 döner ve exception'ın tamamı sadece server log'unda kalır (log sadece burada atılır, bkz. `observability-tracing` bölüm 5). Nullable validation mesajlarını güvenli tut.

Spring Security eklenirse filter seviyesindeki authentication hataları için `AuthenticationEntryPoint`, authorization hataları için `AccessDeniedHandler` da `ProblemDetail` yazmalıdır; controller advice security filter chain'i kapsamaz.

## Response alanları

```json
{
  "type": "https://rag.example/errors/source-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Source not found: 999",
  "instance": "/api/workspaces/42/sources/999",
  "errorCode": "SOURCE_NOT_FOUND"
}
```

```json
{
  "type": "https://rag.example/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/workspaces/42/search",
  "errorCode": "VALIDATION_FAILED",
  "violations": [
    { "field": "query", "message": "size must be between 2 and 500" }
  ]
}
```

- `type` projeye ait sabit bir base URI + `errorCode`'un kebab-case hali. Base URI tek sabittedir (`ERROR_TYPE_BASE_URI`); gerçek domain belirlenince sadece orası değişir. Açık bir `type` RFC'de isteğe bağlıdır (varsayılan `about:blank`, o durumda `title` HTTP status phrase'iyle aynı olmalı); bu projede her hata `type` ve `errorCode` taşır.
- HTTP status ile body'deki `status` aynı olmalıdır.
- `detail` İngilizcedir ve insan içindir; istemci kararını **`errorCode`** üzerinden verir. Kullanıcıya gösterilen Türkçe metni ("Kaynak bulunamadı", "Dil modeline şu an ulaşılamıyor", "Kaynak işleniyor") frontend `errorCode`'a göre üretir (`backend-conventions` bölüm 1).
- `errorCode` değerleri `UPPER_SNAKE_CASE`, API sözleşmesinin (`docs/api/openapi.yaml`) parçasıdır; değiştirmek breaking change'tir.
- WebSocket hataları aynı `errorCode` değerlerini kullanır (`spring-websocket`).

## Doğrulama

Gerçek HTTP response'larını test et: domain 404/409/422/503, validation 400, beklenmeyen 500 ve malformed JSON, desteklenmeyen method gibi framework error'ları. Content type'ı (`application/problem+json`), status'u, sabit `errorCode`'ları ve stack trace ya da internal mesaj olmadığını assert et. Test kalıbı: `spring-mvc-testing`.

## Resmi kaynaklar

- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Spring MVC error response'ları](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

## Gotcha'lar

- Agent, domain'in fırlattığından farklı bir kategori sınıfını ya da `errorCode`'u bekliyor — testte hem kategori sınıfını hem `errorCode`'u doğrula.
- Agent her hata için handler'a yeni `@ExceptionHandler` metodu ekliyor — uygun kategori sınıfını yeni bir `errorCode` ile fırlat.
- Agent hata başına yeni exception sınıfı açıyor (hata adıyla biten `<Hata>Exception` sınıfı) — kategori sınıfı + `errorCode` kullanılır: `new ConflictException("INGESTION_IN_PROGRESS", ...)`.
- Agent ikinci bir `@RestControllerAdvice` açıyor — projede tek advice vardır.
- Agent `spring.mvc.problemdetails.enabled`'ın Boot 4'te varsayılan açık olduğunu sanıyor — varsayılanı `false`.
- Agent framework hatalarını (bozuk JSON, 405, 415) `errorCode`'suz bırakıyor — şablondaki `createResponseEntity` override'ı kaldırılmaz.
- Agent 422 için deprecated `HttpStatus.UNPROCESSABLE_ENTITY` kullanıyor — Spring 7'de `HttpStatus.UNPROCESSABLE_CONTENT`.
- Agent kullanıcıya gösterilecek Türkçe metni `detail`'e yazıyor — `detail` İngilizce, Türkçe metin frontend'de `errorCode`'dan üretilir.
- Agent success envelope'ları ile Problem Details'ı uyumsuz sanıyor — sadece error policy tutarlı olmalı.
- Agent 500 response'larında internal exception mesajlarını dönüyor — private olarak logla, generic detail dön.
- Agent exception'ı hem service'te loglayıp hem fırlatıyor — log sadece handler'da (500) atılır.

Örnekler: [examples/good-exception-handler.java](examples/good-exception-handler.java), [examples/bad-exception-handler.java](examples/bad-exception-handler.java).
