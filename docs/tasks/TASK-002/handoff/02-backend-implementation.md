TASK: TASK-002
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457` (şablon `templates/GlobalExceptionHandler.java`). Okunur kalıp referansı: `../embedding-pgvector/backend/pom.xml` ve `GlobalExceptionHandler` (kopyalanmadı; plan B-3'e göre yazıldı, `BaseException`/multipart kısımları yok).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| B-1 proje bilgisi, K1 | `assistant-api/pom.xml` | `com.rasitbank:assistant-api:0.0.1-SNAPSHOT`, parent `spring-boot-starter-parent` 4.1.1, `java.version` 26 |
| B-1 Maven wrapper, K1 | `assistant-api/mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | `maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.16 -Dtype=only-script` ile üretildi (Maven 3.9.16) |
| B-1 dependency'ler | `assistant-api/pom.xml` | Plandaki 8 main + 5 test dependency, fazlası yok |
| B-1 plugin'ler | `assistant-api/pom.xml` | spring-boot-maven-plugin; dependency `properties` + surefire `@{argLine} -javaagent:${org.mockito:mockito-core:jar}`; JaCoCo 0.8.15 (prepare-agent, report, check LINE ≥ 0.80); PIT 1.30.1 + junit5-plugin 1.2.3, hedef `com.rasitbank.assistant.*` |
| B-1 sınıflar | `AssistantApplication.java`, `package-info.java` (kök ve `exception`) | Türkçe amaç Javadoc'u; `@NullMarked` |
| B-1 properties, K2, Kararlar #16, #17 | `assistant-api/src/main/resources/application.properties` | port 8090, problemdetails açık, `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD` (şifrenin varsayılanı yok), Flyway, `ddl-auto=validate`, `open-in-view=false`, actuator sadece `health` + probes |
| B-1 migration yok, K4, Kararlar #15 | — | `db/migration` klasörü açılmadı; Flyway "No migrations found" uyarısıyla çalışıyor (smoke'ta görüldü) |
| B-1 `.gitignore` | `assistant-api/.gitignore` | `target/`, `.mvn/wrapper/maven-wrapper.jar` |
| B-2 retrieval-api | `retrieval-api/pom.xml`, wrapper, `.gitignore`, `RetrievalApplication.java`, `package-info.java` ×2, `application.properties` | B-1'in aynısı; farklar: artifactId, paket `com.rasitbank.retrieval`, PIT hedefi, port 8080, varsayılan DB/kullanıcı `retrieval`. `hibernate-vector` yok |
| B-2 migration, K4, Kararlar #15 | `retrieval-api/src/main/resources/db/migration/V1__enable_vector_extension.sql` | `CREATE EXTENSION IF NOT EXISTS vector;` |
| B-3, K6, Kararlar #13, S3 | `assistant-api/.../exception/GlobalExceptionHandler.java`, `retrieval-api/.../exception/GlobalExceptionHandler.java` | İki ayrı kopya (sadece `package` satırı farklı). `handleMethodArgumentNotValid` → `VALIDATION_FAILED` + `violations[{field,message}]`; `handleHandlerMethodValidationException` → `VALIDATION_FAILED`, `field` = `@RequestParam` adı (yoksa parametre adı); `createResponseEntity` → `errorCode`'u olmayan framework ProblemDetail'ine status adından `errorCode` + `type`; `handleUnexpectedException` → 500 `INTERNAL_ERROR`, `detail` "An unexpected error occurred", tek `log.error` burada. `handleBaseException` yok (BaseException bu task'ta yok) |
| K5 | `application.properties` (iki servis) | `/actuator/health` |
| B-4 modül notları | `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md` | Durum yorumu, "Teknoloji" (gerçek sürümler, dependency/plugin listesi, ortam değişkenleri, port, sağlık ucu, "Henüz olmayanlar" listesi), "Paket yapısı" kök paket satırı, "Komutlar" (gerçek komutlar, `docker-compose.dev.yml` + `DATABASE_PASSWORD=<db>-local ./mvnw spring-boot:run`). Sonraki task'lara ait "Hedef" satırları (Spring AI, modeller, embedding, arama ayarları, PDF) bırakıldı |

## Değişen dosyalar
Yeni (assistant-api):
- `pom.xml`, `.gitignore`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- `src/main/java/com/rasitbank/assistant/AssistantApplication.java`
- `src/main/java/com/rasitbank/assistant/package-info.java`
- `src/main/java/com/rasitbank/assistant/exception/package-info.java`
- `src/main/java/com/rasitbank/assistant/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.properties`

Yeni (retrieval-api):
- `pom.xml`, `.gitignore`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- `src/main/java/com/rasitbank/retrieval/RetrievalApplication.java`
- `src/main/java/com/rasitbank/retrieval/package-info.java`
- `src/main/java/com/rasitbank/retrieval/exception/package-info.java`
- `src/main/java/com/rasitbank/retrieval/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.properties`
- `src/main/resources/db/migration/V1__enable_vector_extension.sql`

Değişen: `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md`.

Dependency'ler (iki serviste aynı, hepsi planda): `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `flyway-database-postgresql`, `postgresql` (runtime), `jspecify` 1.0.1; test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`. Plugin'ler: JaCoCo 0.8.15, PIT 1.30.1 + `pitest-junit5-plugin` 1.2.3.

## Plandan sapmalar
Yok. (Plan "Maven 3.9.x" diyor; 3.9.16 seçildi. Referans projenin wrapper'ı 3.10.0'dı, plana uyularak 3.9.x kullanıldı.)

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı.
- `cd retrieval-api && ./mvnw -q compile` → başarılı.
- `cd assistant-api && ./mvnw verify -Djacoco.haltOnFailure=false` → BUILD SUCCESS. Test yok (Tests run: 0); JaCoCo report/check "Skipping JaCoCo execution due to missing execution data file" ile atladı. Eşik kontrolü tester'ın testlerinden sonra tam `./mvnw verify` ile doğrulanır.
- `cd retrieval-api && ./mvnw verify -Djacoco.haltOnFailure=false` → BUILD SUCCESS, aynı not.
- Elle smoke (geçici `pgvector/pgvector:0.8.1-pg18` konteyneri, 127.0.0.1:55432, iki DB + iki kullanıcı; iş bitince durduruldu):
  - `curl localhost:8090/actuator/health` → `{"groups":["liveness","readiness"],"status":"UP"}`; 8080 aynı.
  - `GET localhost:8090/api/assistant/yok` → 404, `Content-Type: application/problem+json`, `{"detail":"No static resource api/assistant/yok.","instance":"/api/assistant/yok","status":404,"title":"Not Found","type":"https://rag.example/errors/not-found","errorCode":"NOT_FOUND"}`.
  - `GET localhost:8080/api/workspaces/42/yok` → aynı biçimde 404 `NOT_FOUND`, `instance` "/api/workspaces/42/yok".
  - `DELETE localhost:8090/actuator/health` → 405, `errorCode` "METHOD_NOT_ALLOWED", `type` ".../method-not-allowed".
  - retrieval-api Flyway: "Successfully applied 1 migration … now at version v1" ("1 - enable vector extension"). assistant-api Flyway: "No migrations found" (WARN), uygulama ayağa kalktı.

## Tester için notlar
- **Sağlık ucu gövdesi:** `management.endpoint.health.probes.enabled=true` (plan) yüzünden gövde `{"groups":["liveness","readiness"],"status":"UP"}`. T-B11/T-R12 ve D-S5'teki "`{"status":"UP"}`" tam eşitlik olarak değil, `status` = "UP" ve `components` yok şeklinde doğrulanmalı. `components`/bağlantı bilgisi gövdede yok (show-details varsayılanı `never`).
- **404 `detail`:** Spring'in metni "No static resource <path>." şeklinde; sözleşme `detail`'i sabitlemiyor, assert edilmemeli.
- **Test-only controller:** Handler'da `BaseException` yok; T-B2…T-B8 için `src/test/**` altında bir probe controller gerekli (plan: `/api/assistant/probe/**`, `/api/workspaces/probe/**`). `@WebMvcTest` diliminde `GlobalExceptionHandler` `@RestControllerAdvice` olduğu için otomatik yüklenir.
- **T-B7/T-R7 (`?page=-1`):** `@RequestParam(name = "page") @Min(0) int page` kullanılırsa Spring 7 method validation devreye girer → `HandlerMethodValidationException` → handler `VALIDATION_FAILED`, `violations[0].field` "page". `field`, `@RequestParam` `name`/`value`'su; yoksa derleyici parametre adı (`-parameters` Boot parent'ta açık).
- **T-B8:** `handleUnexpectedException` tek `log.error` atar (`path=` ile); gövdede exception mesajı ve `trace` yok.
- **T-B5/T-R5 (406):** Gözlemlenmedi; plan riski #2 geçerli.
- **Integration testleri:** Flyway assistant-api'de `db/migration` klasörü olmadan çalışıyor (`validate-on-migrate=true`, eksik location hata değil). `@ServiceConnection` datasource'u override ettiği için `DATABASE_PASSWORD` boş varsayılanı testleri etkilemez. retrieval-api testinde imajın süper kullanıcısı migration'ı çalıştırdığı için `CREATE EXTENSION` başarılı olur.
- Dış model, zaman (`Clock`), akış ya da eşzamanlılık noktası bu task'ta yok.

STATUS: DONE
