TASK: TASK-002
# Plan

## Özet
**Mevcut durum:** `assistant-api/`, `retrieval-api/`, `web/`, `deployment/` klasörlerinde sadece `CLAUDE.md` var. Kod, `pom.xml`, `package.json`, Docker dosyası yok. `docs/api/openapi.yaml` iskelet (`paths: {}`).

**Yeni durum:**
- **assistant-api** (yeni, 8090) ve **retrieval-api** (yeni, 8080): iki bağımsız Maven projesi (Spring Boot 4.1.1, Java 26, `./mvnw`). Her birinde:
  - uygulama sınıfı,
  - kendi `GlobalExceptionHandler`'ı (iskeletteki ortak hata kodlarının hepsi; bilinmeyen path → 404 `NOT_FOUND`),
  - Actuator sağlık ucu,
  - Flyway,
  - sadece kendi veritabanına bağlantı.
- retrieval-api'nin tek migration'ı `V1__enable_vector_extension.sql`. Domain tablosu yok.
- **web** (yeni): Vite react-ts iskeleti. Lab'ın sol yarısındaki kabuk: üst bar (logo, "Canlı yapılandırma" rozeti), menü, dört ekran (sadece breadcrumb + başlık + açıklama) ve "Sayfa bulunamadı" ekranı. Dev'de Vite proxy var.
- **deployment** (yeni):
  - `docker-compose.yml`: postgres, assistant-api, retrieval-api, web.
  - `docker-compose.dev.yml`: sadece postgres.
  - postgres init script: iki veritabanı, iki kullanıcı, çapraz erişim kapalı.
  - Dockerfile'lar, `web/nginx.conf`.

**Değişen servisler:** assistant-api ve retrieval-api ikisi de (iskelet). Silinen ya da yeniden adlandırılan mevcut parça yok.

**Uygulama sırası:**
1. Kullanıcı onayı.
2. backend-coder (iki servis) ve frontend-coder (web) paralel çalışır.
3. tester.
4. devops: compose ile ayağa kaldırır, smoke kontrolleri ve E2E koşusu.
5. qa (ekran görüntüsü karşılaştırması dahil).

## Çalışacak roller
- planner: bu plan ve `docs/api/openapi.yaml` iskeletine ekleme.
- backend-coder: assistant-api ve retrieval-api iskeleti. Ayrıca `assistant-api/CLAUDE.md` ve `retrieval-api/CLAUDE.md`'deki "Hedef" satırlarını gerçek değerlerle günceller.
- frontend-coder: web iskeleti ve kabuk. `web/CLAUDE.md`'deki "Hedef" satırlarını günceller.
- tester: backend unit, controller ve integration testleri (Testcontainers); web component testleri (Vitest + RTL); Playwright E2E testleri. E2E sistem ayakta değilken çalıştırılamaz; tester yazar, çalıştırma devops ve qa'dadır (bkz. Deployment planı).
- devops: compose dosyaları, init script, Dockerfile'lar, `.dockerignore`'lar, `web/nginx.conf`, `deployment/CLAUDE.md` ve `README.md`. Sistemi ayağa kaldırır, smoke kontrollerini ve `npm run test:e2e`'yi çalıştırır.
- qa: tüm kabul kriterleri. Görünüm karşılaştırmasını ekran görüntüsüyle yapar.
- Atlanan rol yok.

Lead için not (sahipsiz dosyalar, bu task'ın kapanışında): kök `CLAUDE.md`'deki "Durum: henüz kod yok…" satırı ve `backend-conventions` / `frontend-conventions` skill'lerindeki "Durum: henüz kod yok" notları güncellenmeli. Bkz. Açık sorular ve riskler #4.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Boot 4 + Java 26 + `./mvnw`; React 19 + TS + Vite; bağımsız derleme, kod paylaşımı yok | Backend planı B-1, B-2 (ayrı `pom.xml`, ayrı paketler); Frontend planı F-1 | AC1, AC2 komutları; qa: iki servisin `pom.xml`'inde birbirine bağımlılık yok, ortak modül yok |
| K2 | Portlar: web 3000, assistant-api 8090, retrieval-api 8080, PostgreSQL 5432 | `server.port` (B-1, B-2); compose (D-1) | D-S1, D-S4, D-S5 |
| K3 | Tek PostgreSQL (pgvector imajı), iki veritabanı, her servis kendi kullanıcısıyla; çapraz erişim yok | D-2 init script; B-1/B-2 datasource | D-S2, D-S3; T-B10, T-R10 (servis kendi DB'sine bağlanıyor) |
| K4 | Her serviste Flyway; domain tablosu yok | B-1 (migration yok), B-2 (`V1__enable_vector_extension.sql`) | T-B10, T-R10, T-R11 |
| K5 | Sağlık ucu; Compose healthcheck bunu kullanır | Actuator `/actuator/health`; D-1 healthcheck | T-B11, T-R12, D-S1 |
| K6 | Bilinmeyen uç → RFC 9457 + `NOT_FOUND`; ortak kodlar iskelettekiler | `GlobalExceptionHandler` (B-3); nginx `/api/` fallback (D-3) | T-B1…T-B8, T-R1…T-R8, T-B12, T-R13, E-3, E-4, E-5 |
| K7 | Aynı adres: `/api/assistant/**` → 8090, `/api/workspaces/**` → 8080 (Vite proxy / nginx) | F-1 `vite.config.ts`; D-3 `nginx.conf` | E-3, E-4; D-S4 |
| K8 | Ekran adresleri; kök → Önizleme; doğrudan açma ve yenileme aynı ekranı açar | F-2 route'lar; D-3 SPA fallback | T-F1, T-F2, E-1, E-2 |
| K9 | Görünüm lab'ın aynısı (renk, font, üst bar, menü, seçili öğe, başlık stili) | F-3 token'lar, F-4 bileşenler | T-F4, T-F8; qa Q-1 (ekran görüntüsü) |
| K10 | Mock/sabit veri yok; Konuşmalar sayacı ve kullanıcı alanı gösterilmez | F-4 (`.cnt`, `.who`, `.me` render edilmez); açıklama metinleri soru 1'e göre | T-F5 |
| S1 | `docker compose up` → 4 konteyner sağlıklı; 3000 Önizleme başlığı | D-1 | D-S1, E-1 |
| S2 | Menüden Bilgi kaynakları → `/kaynaklar`, başlık ve açıklama; yenileyince aynı ekran | F-2, F-4, D-3 | T-F3, E-2 |
| S3 | `GET /api/assistant/yok` → 404 `NOT_FOUND` | B-3 | T-B1, T-B12, E-3 |
| AC1 | İki serviste `./mvnw verify` geçer | B-1, B-2 + tester | tester ve qa komut çıktısı |
| AC2 | `npm run build` ve `npm run lint` geçer | F-1 | frontend-coder ve qa komut çıktısı |
| AC3 | `docker compose -f deployment/docker-compose.yml up` ile hepsi sağlıklı | D-1 | D-S1 |
| AC4 | assistant kullanıcısı `retrieval`'a bağlanamaz (ve tersi) | D-2 | D-S2, D-S3 |
| AC5 | Kabuk lab'ın sol yarısıyla yan yana aynı görünür | F-3, F-4 | qa Q-1 |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Postgres ilk açılışta init script'i çalıştırırken (geçici sunucu sadece unix socket'ten dinler) healthcheck "hazır" der; servisler bağlanamayıp çöker | postgres healthcheck TCP üzerinden yapılır (`pg_isready -h 127.0.0.1 …`). Init bitmeden `healthy` olmaz; servisler `service_healthy` ile bekler | D-S1 (boş volume ile, `down -v` sonrası) |
| G2 | `/api/` ile başlayıp iki önekten hiçbirine uymayan istek (ör. `/api/yok`) nginx'te SPA fallback'e düşer, 200 + HTML döner | nginx 404 + `application/problem+json` + `errorCode: NOT_FOUND` döner | E-5, D-S4 |
| G3 | Bilinmeyen ekran adresi (`/yok`, henüz olmayan `/konusmalar/12`) | Kabuk içinde "Sayfa bulunamadı" ekranı; hiçbir menü öğesi seçili değil | T-F6 |
| G4 | Derin adres doğrudan açılır ya da yenilenir (Docker'da nginx) | Aynı ekran gelir (SPA fallback); 404 değil | E-2 |
| G5 | Dar ekran (360 px) | Menü yatay kayar, sayfa yatay taşmaz; 640 px altında "Canlı yapılandırma" rozeti gizlenir (lab'daki container kuralı) | E-6; qa Q-2 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | Lab'daki sahte tarayıcı çerçevesi (`.chrome`: üç nokta + `asistan.rasitbank.intra` adres çubuğu) ile gri dış zemin ve gölgeli pencere (`.site`, `.win`) uygulanmaz. Uygulama gerçek tarayıcı penceresini doldurur: üst bar → menü → sayfa. Sayfa zemini `--s-bg` | Lab, iki ürünü yan yana gösterebilmek için tarayıcıyı taklit ediyor. Ürün gerçek tarayıcıda açılıyor ve adres çubuğunu tarayıcı gösteriyor | Çerçeveyi de çizmek: tarayıcı içinde ikinci, sahte bir adres çubuğu olur ve adresi yanlış gösterir |
| 2 | Üst bar ve menü sabit kalır, sadece sayfa alanı kayar (`100dvh` grid: üst bar, menü, `main` `overflow:auto`) | Lab'daki etkileşim böyle: `.page` kendi içinde kayıyor | Tüm belgenin kayması: menü yukarıda kaybolur, lab'dan farklı olur |
| 3 | Lab'daki "yan menü" (`nav.side`) görünüşte üst barın altında yatay bir sekme şerididir; öyle uygulanır. Öğeler `<button>` değil, `NavLink` (`<a>`) olur; seçili öğe `aria-current="page"` taşır | "Aynısı" kuralı. Link, adres tabanlı gezinmede doğru öğe: yeni sekmede açılabilir, ekran okuyucu "mevcut sayfa" der | Dikey yan menü: lab'da yok. `<button>` + `navigate`: adres semantiği kaybolur |
| 4 | Logo Önizleme'ye giden bir link olur (`aria-label="Önizleme"`, lab'daki gibi) | Lab'da logo `go-home` | — |
| 5 | Kullanıcı alanı (`.who` ad/unvan ve `.me` baş harf rozeti) ve Konuşmalar sayacı (`.cnt`) hiç render edilmez; yer tutucu da konmaz | TASK K10; sayaç TASK-014'te, kullanıcı TASK-017'de gelir | Boş kutu ya da "0": sabit veri olur |
| 6 | Belge başlığı (`<title>`) her ekranda `"<menü adı> · Raşit Bankası Dijital Asistan Yönetimi"` olur (ör. "Bilgi kaynakları · Raşit Bankası Dijital Asistan Yönetimi"). `index.html`'deki varsayılan "Raşit Bankası · Dijital Asistan Yönetimi" | Lab'ın başlığı ("Raşit Bankası Asistan · RAG Lab") lab'a özel. Sekmeler ekrana göre ayırt edilebilmeli | Tek sabit başlık: sekmeler ayırt edilmez |
| 7 | "Sayfa bulunamadı" ekranı kabuğun içinde açılır. Breadcrumb "Dijital Asistan › Sayfa bulunamadı", başlık "Sayfa bulunamadı", açıklama "Adresi kontrol et ya da menüden bir ekran seç.", hiçbir menü öğesi seçili değil | `frontend-conventions` bölüm 5: bilinmeyen adres → "Sayfa bulunamadı". Lab'da karşılığı yok, metin lab'ın tonunda | Kök adrese yönlendirmek: yanlış adres sessizce yutulur |
| 8 | Favicon lab'daki logo işaretidir (30×30 SVG, `public/favicon.svg`). Şablondaki `vite.svg`, `react.svg`, `hero.png`, `App.css` silinir | Ürün kimliği. `frontend-conventions` bölüm 12 | — |
| 9 | Sadece Source Sans 3 kurulur (`@fontsource/source-sans-3`; 400, 500, 600, 700, 800). `--font-mono` token'ı lab'daki yığınla tanımlanır, JetBrains Mono paketi ilk monospace metni gösteren task'ta eklenir | Kabukta monospace metin yok | İki fontu şimdi kurmak: kullanılmayan dosya |
| 10 | Bu task'ta TanStack Query, `react-error-boundary`, MSW ve springdoc **kurulmaz**. İlk sunucu çağrısını yapan ya da ilk endpoint'i yazan task kurar | Kabuk hiçbir istek atmıyor, endpoint yok | Şimdi kurmak: kullanılmayan bağımlılık, coverage dışı kod |
| 11 | `AppProviders.tsx` bu task'ta açılmaz. `main.tsx` `<StrictMode><BrowserRouter><App/></BrowserRouter></StrictMode>` render eder. `App` sadece `<Routes>` tanımıdır; testler `MemoryRouter` ile sarar | Provider gerektiren paket yok; `App`'in router'dan bağımsız olması testte başlangıç adresi vermeyi kolaylaştırır | `App` içinde `BrowserRouter`: testte adres verilemez |
| 12 | Ekran başlığı ortak bir `PageHeader` bileşeniyle çizilir (`shared/components/`) | Dört feature aynı başlığı kullanıyor; feature'lar birbirini import edemez | Her feature'da kopya: stil ayrışır |
| 13 | Exception: bu task'ta sadece `GlobalExceptionHandler` açılır. `BaseException` ve kategori sınıfları ilk domain hatasını getiren task'ta eklenir. Handler iskeletteki 7 kodun hepsini üretir. Şablona ek olarak `handleHandlerMethodValidationException` override edilir: `@RequestParam` doğrulaması (ör. `page=-1`) `BAD_REQUEST` değil `VALIDATION_FAILED` + `violations[].field = "<parametre adı>"` döner | TASK K6 "ortak kodlar iskelettekiler"; `backend-conventions` bölüm 6: geçersiz `page`/`size` `VALIDATION_FAILED` döner. Spring 7'de bu hata `HandlerMethodValidationException`'dır, şablon bunu `BAD_REQUEST`'e çeviriyor | Kategori sınıflarını şimdi açmak: hiçbir yerde fırlatılmayan, test edilemeyen kod |
| 14 | Veritabanı yetkileri devops'un init script'indedir (`deployment/postgres/initdb/01-init-databases.sh`, sadece boş volume'da çalışır): roller `assistant`, `retrieval` (`LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE`); `CREATE DATABASE assistant OWNER assistant`, `CREATE DATABASE retrieval OWNER retrieval`; `assistant`, `retrieval` ve `postgres` veritabanlarında `REVOKE CONNECT … FROM PUBLIC`; `retrieval` içinde süper kullanıcıyla `CREATE EXTENSION IF NOT EXISTS vector`. Şifreler script'e değil compose ortam değişkenlerine yazılır | Ayrılık yetkiyle sağlanır; sahip kendi veritabanına bağlanabilir, PUBLIC'in hakkı kalkınca diğeri bağlanamaz. Eklentiyi süper kullanıcının kurması, pgvector'ün "trusted" olup olmamasından bağımsızdır | Ayrılığı sadece `pg_hba.conf` ile yapmak: imajın varsayılan dosyasını ezmek gerekir |
| 15 | retrieval-api'nin `V1__enable_vector_extension.sql`'i `CREATE EXTENSION IF NOT EXISTS vector;` içerir. Docker'da init script'ten sonra etkisizdir, Testcontainers'ta eklentiyi kurar. assistant-api'de migration yok; Flyway yine çalışır ("no migrations") | `retrieval-api/CLAUDE.md`: "`CREATE EXTENSION IF NOT EXISTS vector` ilk migration'dadır". Eklenti domain tablosu değil (K4) | assistant-api'ye boş ya da yorum-only migration: anlamsız dosya |
| 16 | Datasource: `spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/<db>}`, `spring.datasource.username=${DATABASE_USER:<db>}`, `spring.datasource.password=${DATABASE_PASSWORD:}`. Şifrenin varsayılanı yok | `backend-conventions` bölüm 4: gizli bilgi properties'e yazılmaz | Referanstaki gibi varsayılan şifre yazmak: kuralı çiğner |
| 17 | `spring-boot-starter-data-jpa` şimdi eklenir (`ddl-auto=validate`, `open-in-view=false`); entity yok | Sonraki her task'ın ilk ihtiyacı. Ayarlar modül `CLAUDE.md`'lerinde hedef olarak yazılı | Sadece JDBC: bir sonraki task `pom.xml` ve ayarları yeniden açar |
| 18 | pgAdmin bu task'ta yok | TASK senaryosu dört konteyner sayıyor; veritabanı arayüzü kabul kriterinde yok | Şimdi eklemek: beşinci konteyner, iki kullanıcıyla sunucu tanımı |
| 19 | nginx: `/api/assistant/` → `assistant-api:8090`, `/api/workspaces/` → `retrieval-api:8080`, `/api/` (geri kalan) → 404 problem+json `NOT_FOUND`, `/` → SPA fallback. Akış (SSE) için tampon ayarları TASK-008'de, yükleme boyutu TASK-015'te eklenir | Bu task'ta akış ve yükleme yok | — |
| 20 | Görünüm karşılaştırması: lab 1480×900'de açılır ve `.sbody` öğesinin ekran görüntüsü alınır. Web aynı genişlik ve yükseklikteki bir pencerede açılır ve görüntüsü alınır. İkisi yan yana konur (qa Q-1). Otomatik görüntü-fark testi yok | Lab'ın sol yarısı çerçeve içinde; sadece `.sbody` ürünün karşılığı. `frontend-conventions`: snapshot testi yok | Playwright `toHaveScreenshot` ile lab'a karşı karşılaştırma: çerçeve ve font render farkı yüzünden sürekli kırılır |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu (`Durum` satırı ve altı):
  - Bilinmeyen path iki serviste de 404 + `application/problem+json` + `NOT_FOUND` döner.
  - nginx, iki önek dışındaki `/api/**` isteklerine aynı biçimde 404 `NOT_FOUND` döner.
  - `/actuator/health` işletim ucudur, sözleşmenin parçası değildir ve web üzerinden yayınlanmaz.
- `components.responses.NotFound` eklendi (`NOT_FOUND`, `ProblemDetail` şeması). Henüz hiçbir path onu kullanmıyor; ilk endpoint task'ı kullanır.
- `paths: {}` aynı kaldı. `errorCode` enum'u değişmedi (7 kod).

`docs/api/sse.md` ve `docs/api/websocket.md`: yok (kapsam dışı).

## Backend planı
Yükleyeceği skill'ler: `backend-conventions`, `problem-details-rfc9457`, `flyway-migrations`, `springboot-patterns`, `spring-boot-engineer`.

### B-1 assistant-api (`assistant-api/`)
- **Proje bilgisi:** `groupId` `com.rasitbank`, `artifactId` `assistant-api`, `version` `0.0.1-SNAPSHOT`, parent `spring-boot-starter-parent` 4.1.1, `java.version` 26.
- **Maven wrapper:** `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` (Maven 3.9.x; Initializr ya da `mvn -N wrapper:wrapper` ile).
- **Dependencies:**
  - `spring-boot-starter-webmvc`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-actuator`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-flyway`
  - `org.flywaydb:flyway-database-postgresql`
  - `org.postgresql:postgresql` (runtime)
  - `org.jspecify:jspecify` 1.0.1
- **Test dependencies:**
  - `spring-boot-starter-test`
  - `spring-boot-starter-webmvc-test`
  - `spring-boot-testcontainers`
  - `org.testcontainers:testcontainers-junit-jupiter`
  - `org.testcontainers:testcontainers-postgresql`
- **Plugin'ler** (referans `../embedding-pgvector/backend/pom.xml` kalıbıyla):
  - `spring-boot-maven-plugin`
  - `maven-dependency-plugin` `properties` goal'u (Mockito agent yolu)
  - `maven-surefire-plugin` `argLine` = `@{argLine} -javaagent:${org.mockito:mockito-core:jar}`
  - `jacoco-maven-plugin` 0.8.15: prepare-agent, report, check (LINE COVEREDRATIO ≥ `0.80`)
  - `pitest-maven` 1.30.1 + `pitest-junit5-plugin` 1.2.3: `targetClasses` ve `targetTests` = `com.rasitbank.assistant.*`
- **Sınıflar:**
  - `com.rasitbank.assistant.AssistantApplication`
  - `com.rasitbank.assistant.exception.GlobalExceptionHandler` (B-3)
  - Her pakette `package-info.java` (`@NullMarked`)
- **`application.properties`:**
  - `spring.application.name=assistant-api`
  - `server.port=8090`
  - `spring.mvc.problemdetails.enabled=true`
  - datasource (Kararlar #16; varsayılan veritabanı `assistant`, kullanıcı `assistant`)
  - `spring.flyway.locations=classpath:db/migration`
  - `spring.flyway.validate-on-migrate=true`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.jpa.open-in-view=false`
  - `management.endpoints.web.exposure.include=health`
  - `management.endpoint.health.probes.enabled=true`
- **Migration:** yok.
- **Bu task'ta olmayanlar:** Spring AI, `ClockConfig`, `OpenApiConfig`, `PageResponse`.
- `.gitignore`: `target/`.

### B-2 retrieval-api (`retrieval-api/`)
B-1'in aynısı, şu farklarla:
- `artifactId` `retrieval-api`.
- Paket `com.rasitbank.retrieval`, sınıflar `RetrievalApplication` ve `com.rasitbank.retrieval.exception.GlobalExceptionHandler`.
- PIT hedefi `com.rasitbank.retrieval.*`.
- `spring.application.name=retrieval-api`, `server.port=8080`, varsayılan veritabanı ve kullanıcı `retrieval`.
- Migration: `src/main/resources/db/migration/V1__enable_vector_extension.sql` (`CREATE EXTENSION IF NOT EXISTS vector;`).
- `hibernate-vector` bu task'ta yok (vektör kolonu yok).

### B-3 `GlobalExceptionHandler` (iki serviste aynı davranış, ayrı kopya)
- `@RestControllerAdvice`, `ResponseEntityExceptionHandler`'ı extend eder.
- Sabit `ERROR_TYPE_BASE_URI = "https://rag.example/errors/"`. `type` = base + `errorCode`'un kebab-case hali.
- Gövdesi şablondan (`problem-details-rfc9457/templates/GlobalExceptionHandler.java`) alınır, `handleBaseException` hariç. Metotlar:
  - `handleMethodArgumentNotValid` → 400 `VALIDATION_FAILED`, `violations[{field, message}]`.
  - `handleHandlerMethodValidationException` (yeni) → 400 `VALIDATION_FAILED`, `violations[].field` = parametre adı, `message` = varsayılan mesaj.
  - `createResponseEntity` → `errorCode` taşımayan framework `ProblemDetail`'lerine HTTP status adından `errorCode` ve `type` ekler: 400 `BAD_REQUEST`, 404 `NOT_FOUND`, 405 `METHOD_NOT_ALLOWED`, 406 `NOT_ACCEPTABLE`, 415 `UNSUPPORTED_MEDIA_TYPE`.
  - `handleUnexpectedException(Exception)` → 500 `INTERNAL_ERROR`, `detail` "An unexpected error occurred". Exception sadece burada `log.error` ile loglanır.
- Bilinmeyen path (`NoResourceFoundException` / `NoHandlerFoundException`) şablondaki yolla 404 `NOT_FOUND` olur; ek kod gerekmez.
- `instance` istek path'idir.

### B-4 Modül notları
backend-coder, iki servisin `CLAUDE.md`'sindeki "Hedef" satırlarını gerçek değerlerle değiştirir: sürümler, paketler, gerçek komutlar ve ortam değişkenleri (`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`). Lokal çalıştırma örneği: `docker compose -f deployment/docker-compose.dev.yml up -d` + `DATABASE_PASSWORD=assistant-local ./mvnw spring-boot:run`.

## Frontend planı
Yükleyeceği skill'ler: `frontend-conventions`, `vite-patterns`, `frontend-design`, `frontend-a11y`, `react-patterns`.

### F-1 İskelet ve araçlar (`web/`)
- **Kurulum:** Vite `react-ts` şablonu. `package.json`'da `"engines": {"node": ">=26 <27"}`.
- **Script'ler:**
  - `dev`: `vite`
  - `build`: `tsc -b && vite build`
  - `lint`: `oxlint`
  - `test`: `vitest run`
  - `test:watch`: `vitest`
  - `test:coverage`: `vitest run --coverage`
  - `test:e2e`: `playwright test`
- **Runtime paketleri:** `react`, `react-dom`, `react-router` (7.x), `@fontsource/source-sans-3`.
- **Dev paketleri:**
  - Derleme ve lint: `typescript` (~6), `vite` (8), `@vitejs/plugin-react`, `@types/react`, `@types/react-dom`, `@types/node`, `oxlint`.
  - Test: `vitest`, `jsdom`, `@vitest/coverage-v8`, `@testing-library/react`, `@testing-library/dom`, `@testing-library/user-event`, `@testing-library/jest-dom`, `vitest-axe`, `@playwright/test`.
  - Sürümler referans `../embedding-pgvector/frontend/package.json`'daki seviyede.
- **`vite.config.ts`** (`vitest/config`'ten `defineConfig`):
  - `server.proxy`: `'/api/assistant'` → `http://localhost:8090`, `'/api/workspaces'` → `http://localhost:8080`.
  - `test.environment`: `'jsdom'`.
  - `test.setupFiles`: `['./src/test/setup.ts']`. Bu dosyayı tester yazar.
  - `test.exclude`: `e2e/**`.
  - Coverage: v8, `src/**/*.{ts,tsx}`; `src/test/**`, `src/main.tsx` ve test dosyaları hariç; `thresholds.lines: 80`.
- **`playwright.config.ts`:** `testDir: 'e2e'`, `use.baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000'`, sadece chromium.
- **Diğer dosyalar:**
  - `.oxlintrc.json`: referanstaki kalıp.
  - `.gitignore`: `node_modules`, `dist`, `coverage`, `test-results`, `playwright-report`.
  - `index.html`: `lang="tr"`, `<title>Raşit Bankası · Dijital Asistan Yönetimi</title>`, `<link rel="icon" href="/favicon.svg" type="image/svg+xml">`.
- **Silinecekler:** şablondaki `App.css`, `src/assets/`, `public/vite.svg` (Kararlar #8).

### F-2 Route'lar (`src/app/App.tsx`, `export function App`)
| Adres | Bileşen |
|-------|---------|
| (layout) | `AdminLayout` (`src/app/AdminLayout.tsx`); altındaki route'lar `<Outlet/>` ile |
| `/` (index) | `<Navigate to="/onizleme" replace />` |
| `/onizleme` | `PreviewPage` (`src/features/preview/pages/PreviewPage.tsx`) |
| `/konusmalar` | `ConversationListPage` (`src/features/conversation/pages/ConversationListPage.tsx`) |
| `/kaynaklar` | `SourceListPage` (`src/features/source/pages/SourceListPage.tsx`) |
| `/degerlendirme` | `EvaluationPage` (`src/features/evaluation/pages/EvaluationPage.tsx`) |
| `*` | `NotFoundPage` (`src/app/NotFoundPage.tsx`) |

`/konusmalar/:conversationId` ve `/kaynaklar/:sourceId` bu task'ta yok (TASK-014, TASK-007); şimdilik "Sayfa bulunamadı" açılır.

`src/main.tsx` font CSS'lerini import eder (`@fontsource/source-sans-3/{400,500,600,700,800}.css`), sonra `index.css`'i ve Kararlar #11'deki render'ı.

### F-3 Design token'ları (`src/index.css`)
`:root` içinde, değerler lab'dan birebir:
- **Renk** (lab satır 15-17):
  - `--color-bg` #f5f2f3, `--color-card` #ffffff, `--color-ink` #21181c, `--color-muted` #6e6268, `--color-line` #e6dee1
  - `--color-brand` #7b2346, `--color-brand-dark` #5a1832, `--color-brand-soft` #f5e8ed
  - `--color-accent` #b8892b, `--color-accent-soft` #f8f0dc
  - `--color-red` #b8352a, `--color-red-soft` #fbebe9, `--color-green` #1d7a4f, `--color-green-soft` #e6f4ec
- **Kabukta ham hex olarak geçen renkler token olur** (lab satır 45, 52, 55):
  - `--color-nav-ink` #4a3f44, `--color-nav-icon` #8a7d83
  - `--color-badge-ink` #7c5a12, `--color-badge-line` #ecd9a8
  - `--color-focus` #5aa2ff (lab satır 27'deki focus-visible kuralı; değer konsol paletinden geliyor ama sol yarının focus rengi bu)
- **Font** (lab satır 20-21): `--font-site`: `"Source Sans 3", "Segoe UI", system-ui, -apple-system, sans-serif`. `--font-mono`: `"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace`.
- **Boyut ve boşluk:** lab satır 40-66'daki değerler token'a çevrilir (`--text-*`, `--space-*`, `--radius-*`):
  - üst bar: yükseklik 52 px, yatay iç boşluk 14 px, aralık 12 px
  - logo: 16/800 ve 11.5/600
  - rozet: 11/700, radius 5, iç boşluk 2 px × 7 px
  - menü: iç boşluk 0 12 px, öğe 10/10/9 px, 13/600, alt çizgi 2 px
  - sayfa: iç boşluk 16 18 28 px, aralık 12 px
  - breadcrumb: 12.5 px
  - h1: 700 20 px / 1.25, letter-spacing -.01em
  - açıklama: 13.5 px, `max-width: 64ch`, üst boşluk 3 px
  - Token adlarını coder seçer; component CSS'inde ham hex ya da rastgele px kalmaz.
- **Global:** `body` `font-family: var(--font-site)`, 14 px, `color: var(--color-ink)`, `background: var(--color-bg)`. `*{box-sizing:border-box}`. `button`, `a` `:focus-visible` → `outline: 2px solid var(--color-focus); outline-offset: 2px`. `prefers-reduced-motion` kuralı lab satır 351'deki gibi.

### F-4 Bileşenler
- **`AdminLayout`** (`src/app/AdminLayout.tsx` + `.module.css`): `100dvh` grid, satırlar `auto auto minmax(0,1fr)` (Kararlar #2). Sırasıyla `AppHeader`, `MainNav`, `<main>` (`overflow:auto`) içinde `<Outlet/>`.
- **`AppHeader`** (`src/app/AppHeader.tsx` + `.module.css`): `<header>`, beyaz zemin, alt çizgi `--color-line`.
  - Logo: `<Link to="/onizleme" aria-label="Önizleme">` içinde lab satır 366'daki SVG (`aria-hidden="true"`) ve iki satır metin "Raşit Bankası" / "Dijital Asistan Yönetimi".
  - Rozet: `<span title="Önizleme, canlı asistanla aynı yapılandırmayı kullanır">Canlı yapılandırma</span>`. `@media (max-width: 640px)` altında gizlenir.
  - Kullanıcı alanı yok (Kararlar #5).
- **`MainNav`** (`src/app/MainNav.tsx` + `.module.css`): `<nav aria-label="Menü">`, yatay, `overflow-x:auto`. Dosya içi sabit `NAV_ITEMS`: `{ path, label, icon }`.
  - Sıra: `/onizleme` "Önizleme", `/konusmalar` "Konuşmalar", `/kaynaklar` "Bilgi kaynakları", `/degerlendirme` "Değerlendirme".
  - İkonlar lab satır 374-377'deki SVG'ler (`aria-hidden="true"`). Ayrı dosya açılmaz.
  - Her öğe `NavLink`. Aktif stil lab'daki `.on`: alt çizgi `--color-brand`, metin `--color-brand-dark`, ikon `--color-brand`.
  - Sayaç yok.
- **`PageHeader`** (`src/shared/components/PageHeader.tsx` + `.module.css`):
  - Props: `type PageHeaderProps = { section: string; title: string; description: string }`.
  - Breadcrumb `<div>`: "Dijital Asistan", "›", `<b>{section}</b>` (lab satır 1067).
  - `<h1>{title}</h1>`, `<p>{description}</p>`.
  - `<title>{`${section} · Raşit Bankası Dijital Asistan Yönetimi`}</title>` (React 19, tek string).
- **Sayfalar:** her biri sadece `PageHeader` render eder. Metinler aşağıda.

### F-5 Ekran metinleri (lab satır 1067-1108)
| Sayfa | `section` | `title` | `description` |
|-------|-----------|---------|---------------|
| `PreviewPage` | Önizleme | Asistan önizleme | Raşit Mobil'deki asistanı müşterinin gördüğü hâliyle dene; önizleme konuşmaları da Konuşmalar'a düşer. |
| `ConversationListPage` | Konuşmalar | Konuşmalar | Her tur, modele giden parçalarla birlikte saklanır. Yanlış bir cevapta önce doğru parçanın prompt'a girip girmediğine bakılır. |
| `SourceListPage` | Bilgi kaynakları | Bilgi kaynakları | Soru 1'in cevabına göre. Önerilen (A): "Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça embedding modeliyle embed edilip pgvector'e yazılır." |
| `EvaluationPage` | Değerlendirme | Değerlendirme | Soru 1'in cevabına göre. Önerilen (A): "Rehberde cevabı olan sorular; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar." |
| `NotFoundPage` | Sayfa bulunamadı | Sayfa bulunamadı | Adresi kontrol et ya da menüden bir ekran seç. |

Lab'daki değişken değerler: Bilgi kaynakları'nda `bge-m3` (çalışma alanının embedding modeli), Değerlendirme'de soru sayısı (golden set). Bunlar TASK-007 ve TASK-016'da backend'den gelir ve metne o task'larda eklenir. Bu task'ta sabit yazılmaz.

- Hata mesajı ve `errorMessages.ts` bu task'ta yok (sunucu çağrısı yok).

## Test senaryoları
Önek: T-B assistant-api, T-R retrieval-api, T-F web component, E Playwright, D-S devops smoke, Q qa elle.

Backend controller testleri (`GlobalExceptionHandlerTest`, `@WebMvcTest` + `MockMvcTester`, iki serviste ayrı). Test-only controller `src/test/**` altında durur (ör. `ErrorProbeController`, path `/api/assistant/probe/**` ya da `/api/workspaces/probe/**`). Production'a test ucu girmez.

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| T-B1 / T-R1 | Bilinmeyen path | controller | `givenUnknownPath_whenGet_thenReturns404ProblemWithNotFoundErrorCode` | `GET /api/assistant/yok` (retrieval: `/api/workspaces/42/yok`) → 404, `Content-Type: application/problem+json`, `status` 404, `errorCode` "NOT_FOUND", `type` "https://rag.example/errors/not-found", `instance` = istek path'i |
| T-B2 / T-R2 | Desteklenmeyen method | controller | `givenGetOnlyEndpoint_whenDelete_thenReturns405MethodNotAllowed` | 405, `errorCode` "METHOD_NOT_ALLOWED", `type` ".../method-not-allowed" |
| T-B3 / T-R3 | Bozuk JSON | controller | `givenMalformedJson_whenPost_thenReturns400BadRequest` | gövde `{"text":` → 400, `errorCode` "BAD_REQUEST", `violations` alanı yok |
| T-B4 / T-R4 | Desteklenmeyen içerik türü | controller | `givenTextPlainBody_whenPostJsonEndpoint_thenReturns415UnsupportedMediaType` | 415, `errorCode` "UNSUPPORTED_MEDIA_TYPE" |
| T-B5 / T-R5 | Kabul edilemeyen Accept | controller | `givenJsonOnlyEndpoint_whenAcceptTextCsv_thenReturns406NotAcceptable` | 406. Gövde yazılabiliyorsa `errorCode` "NOT_ACCEPTABLE". Spring gövdeyi yazamıyorsa tester sadece status'u doğrular ve raporda belirtir (risk #2) |
| T-B6 / T-R6 | Gövde doğrulaması | controller | `givenBlankRequiredField_whenPost_thenReturns400ValidationFailedWithFieldViolation` | `{"text":" "}` (`@NotBlank`) → 400, `errorCode` "VALIDATION_FAILED", `violations` = `[{field:"text", message:"must not be blank"}]` |
| T-B7 / T-R7 | Query parametresi doğrulaması | controller | `givenNegativePageParam_whenGet_thenReturns400ValidationFailedWithPageViolation` | `?page=-1` (`@Min(0)`) → 400, `errorCode` "VALIDATION_FAILED" (BAD_REQUEST değil), `violations[0].field` "page" |
| T-B8 / T-R8 | Beklenmeyen hata | controller | `givenHandlerThrowsUnexpectedException_whenGet_thenReturns500InternalErrorWithoutInternalDetails` | 500, `errorCode` "INTERNAL_ERROR", `detail` "An unexpected error occurred". Gövdede exception mesajı (ör. "boom-secret") ve `trace` yok |

Backend integration testleri (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, Testcontainers `pgvector/pgvector:0.8.1-pg18` + `@ServiceConnection`):

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| T-B10 | Flyway kurulu, migration yok | integration | `givenEmptyDatabase_whenContextStarts_thenFlywayRunsWithNoMigrations` (`AssistantApplicationIntegrationTest`) | `Flyway` bean'i var; `flyway.info().applied()` boş, `pending()` boş |
| T-B11 | Sağlık ucu | integration | `givenRunningApplication_whenGetActuatorHealth_thenReturnsUp` | `GET /actuator/health` → 200, `{"status":"UP"}`; gövdede `components` ya da bağlantı bilgisi yok |
| T-B12 | Gerçek sunucuda bilinmeyen path | integration | `givenRunningApplication_whenGetUnknownAssistantPath_thenReturns404NotFoundProblem` | `GET /api/assistant/yok` → 404, problem+json, `errorCode` "NOT_FOUND" |
| T-R10 | Flyway V1 uygulandı | integration | `givenFreshDatabase_whenContextStarts_thenFlywayAppliedVersion1` (`RetrievalApplicationIntegrationTest`) | `applied()` tek kayıt, version "1", description "enable vector extension", state SUCCESS |
| T-R11 | `vector` eklentisi kurulu | integration | `givenFreshDatabase_whenContextStarts_thenVectorExtensionIsInstalled` | `select count(*) from pg_extension where extname = 'vector'` → 1 |
| T-R12 | Sağlık ucu | integration | `givenRunningApplication_whenGetActuatorHealth_thenReturnsUp` | T-B11 ile aynı |
| T-R13 | Gerçek sunucuda bilinmeyen path | integration | `givenRunningApplication_whenGetUnknownWorkspacesPath_thenReturns404NotFoundProblem` | `GET /api/workspaces/42/yok` → 404, `errorCode` "NOT_FOUND" |

Web component testleri (`src/app/App.test.tsx`; `MemoryRouter` ile, `src/test/renderWithProviders.tsx` tester'ın):

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| T-F1 | Kök adres | component | `givenRootPath_whenRendered_thenRedirectsToPreviewScreen` | h1 "Asistan önizleme"; "Menü" navigasyonundaki "Önizleme" linki `aria-current="page"` |
| T-F2 | Her ekranın metni (`it.each`, 4 satır) | component | `givenScreenPath_whenRendered_thenShowsLabBreadcrumbTitleAndDescription` | F-5 tablosundaki `title` ve `description` literal olarak; breadcrumb "Dijital Asistan" + "›" + `section`; menüde sadece o ekranın linki `aria-current="page"` |
| T-F3 | Menüden gezinme | component | `givenPreviewScreen_whenSourcesMenuItemClicked_thenShowsSourcesScreenAndMarksItSelected` | Tıklamadan sonra h1 "Bilgi kaynakları"; "Bilgi kaynakları" linki `aria-current="page"`, "Önizleme" linkinde `aria-current` yok |
| T-F4 | Üst bar | component | `givenAnyScreen_whenRendered_thenShowsLogoTextAndLiveConfigBadge` | "Raşit Bankası", "Dijital Asistan Yönetimi", "Canlı yapılandırma" görünür; logo linki (`name: 'Önizleme'`, header içinde) `href="/onizleme"` |
| T-F5 | Sabit veri yok | component | `givenAnyScreen_whenRendered_thenShowsNoConversationCounterAndNoUserArea` | "Konuşmalar" linkinin erişilebilir adı tam "Konuşmalar" (sayı yok); header'da "Raşit Eşdemir", "RE", "Ürün sahibi" yok |
| T-F6 | Bilinmeyen adres | component | `givenUnknownPath_whenRendered_thenShowsNotFoundScreenWithNoMenuItemSelected` | `/yok` ve `/konusmalar/12` → h1 "Sayfa bulunamadı"; menüde hiçbir linkte `aria-current` yok |
| T-F7 | Belge başlığı | component | `givenSourcesPath_whenRendered_thenSetsDocumentTitle` | `document.title` "Bilgi kaynakları · Raşit Bankası Dijital Asistan Yönetimi" |
| T-F8 | Erişilebilirlik | component | `givenShell_whenRendered_thenHasNoAxeViolations` | `vitest-axe` ihlal yok (Önizleme ve Bilgi kaynakları) |

Playwright (`web/e2e/shell.spec.ts`, Docker sistemi 3000'de):

| # | Senaryo | Katman | Test adı | Beklenen sonuç |
|---|---------|--------|----------|----------------|
| E-1 | Açılış | e2e | `givenRunningSystem_whenRootOpened_thenShowsPreviewScreen` | URL `/onizleme`, h1 "Asistan önizleme" |
| E-2 | Derin adres + yenileme | e2e | `givenSourcesUrl_whenOpenedDirectlyAndReloaded_thenStaysOnSourcesScreen` | `/kaynaklar` açılır → h1 "Bilgi kaynakları"; `reload()` sonrası aynı h1 ve URL; yanıt 200 |
| E-3 | assistant-api 404 web üzerinden | e2e | `givenUnknownAssistantPath_whenRequestedThroughWeb_thenReturns404NotFoundProblem` | `request.get('/api/assistant/yok')` → 404, `content-type` problem+json, `errorCode` "NOT_FOUND" |
| E-4 | retrieval-api 404 web üzerinden | e2e | `givenUnknownWorkspacesPath_whenRequestedThroughWeb_thenReturns404NotFoundProblem` | `/api/workspaces/42/yok` → 404, `errorCode` "NOT_FOUND" (yanıt retrieval-api'den gelir: `instance` "/api/workspaces/42/yok") |
| E-5 | Önekler dışındaki `/api` | e2e | `givenApiPathOutsideBothPrefixes_whenRequested_thenReturns404NotFoundProblemNotHtml` | `/api/yok` → 404, problem+json, `errorCode` "NOT_FOUND"; gövde HTML değil |
| E-6 | Dar ekran | e2e | `givenNarrowViewport_whenShellRendered_thenBadgeHiddenAndNoHorizontalPageOverflow` | 360×740: "Canlı yapılandırma" görünmez; `document.documentElement.scrollWidth` ≤ 360; menü linklerinin dördü de DOM'da |

Devops smoke (`handoff/04-deploy-report.md`'ye komut ve çıktı):

| # | Kontrol | Beklenen |
|---|---------|----------|
| D-S1 | `docker compose -f deployment/docker-compose.yml down -v && … up --build -d` + `… ps` | postgres, assistant-api, retrieval-api, web dördü de `healthy` (boş volume ile; G1) |
| D-S2 | `docker compose … exec -e PGPASSWORD=assistant-local postgres psql -h 127.0.0.1 -U assistant -d retrieval -c 'select 1'` | Hata: `permission denied for database "retrieval"`; aynı komut `-d assistant` ile başarılı |
| D-S3 | Tersi: `-U retrieval -d assistant` | `permission denied for database "assistant"`; `-d retrieval` başarılı |
| D-S4 | `curl -si localhost:3000/api/assistant/yok`, `/api/workspaces/42/yok`, `/api/yok` | Üçü de 404 + `application/problem+json` + `"errorCode":"NOT_FOUND"` |
| D-S5 | `curl -s localhost:8090/actuator/health`, `localhost:8080/actuator/health`; `curl -s -o /dev/null -w '%{http_code}' localhost:3000/actuator/health` | İlk ikisi `{"status":"UP"}`; web üzerinden actuator yayınlanmaz (SPA HTML'i ya da 404, `UP` gövdesi değil) |
| D-S6 | `cd web && npm run test:e2e` | E-1…E-6 geçer |

qa elle:
- Q-1: Kararlar #20'deki yöntemle dört ekranın ekran görüntüsü lab'la yan yana konur. Renk, font, üst bar, menü, seçili öğe, breadcrumb, başlık ve açıklama aynı. Bilinen farklar: sahte tarayıcı çerçevesi, kullanıcı alanı, sayaç, açıklamalardaki değişken değerler.
- Q-2: 360 px ve 1280 px'te kabuğu gezer, klavyeyle Tab sırasını (logo → menü öğeleri) ve görünür focus'u kontrol eder.

## Deployment planı
- **D-1 `deployment/docker-compose.yml`** (`name: rag`; dosya başında "Local use only" yorumu):
  - **`postgres`:**
    - imaj `pgvector/pgvector:0.8.1-pg18`
    - `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres-local`, `ASSISTANT_DB_PASSWORD=assistant-local`, `RETRIEVAL_DB_PASSWORD=retrieval-local`
    - volume `pgdata:/var/lib/postgresql`
    - `./postgres/initdb:/docker-entrypoint-initdb.d:ro`
    - port `127.0.0.1:5432:5432`
    - healthcheck `pg_isready -h 127.0.0.1 -U postgres` (TCP; G1)
  - **`assistant-api`:**
    - build `../assistant-api`
    - `DATABASE_URL=jdbc:postgresql://postgres:5432/assistant`, `DATABASE_USER=assistant`, `DATABASE_PASSWORD=assistant-local`
    - port `8090:8090`
    - healthcheck `curl -sf http://localhost:8090/actuator/health`
    - `depends_on: postgres: service_healthy`
  - **`retrieval-api`:** aynı kalıp; veritabanı `retrieval`, kullanıcı `retrieval`, şifre `retrieval-local`, port `8080:8080`.
  - **`web`:**
    - build `../web`
    - port `3000:8080` (nginx-unprivileged)
    - healthcheck `wget -q -O /dev/null http://127.0.0.1:8080/`
    - `depends_on`: iki servis `service_healthy`
  - Bu task'ta `extra_hosts` ve model ortam değişkenleri yok (TASK-003).
- **D-2:**
  - `deployment/postgres/initdb/01-init-databases.sh` (Kararlar #14; `set -euo pipefail`, `psql -v ON_ERROR_STOP=1`, şifreler `$ASSISTANT_DB_PASSWORD` / `$RETRIEVAL_DB_PASSWORD`'dan).
  - `deployment/docker-compose.dev.yml`: sadece `postgres` (aynı imaj, init script, port ve healthcheck; ayrı volume `pgdata-dev`).
- **D-3 Dockerfile'lar ve nginx:**
  - `assistant-api/Dockerfile`, `retrieval-api/Dockerfile`: referans kalıbı (`maven:3.9-eclipse-temurin-26` build, `eclipse-temurin:26-jre-noble` + `curl`, root olmayan kullanıcı; `EXPOSE` 8090 / 8080).
  - `web/Dockerfile`: `node:26-alpine` build, `nginxinc/nginx-unprivileged:1.29.8-alpine`.
  - `*/.dockerignore` (`target/`, `node_modules/`, `dist/`, `e2e` çıktıları).
  - `web/nginx.conf`: Kararlar #19. `/api/` geri kalan için `default_type application/problem+json; return 404 '{"type":"https://rag.example/errors/not-found","title":"Not Found","status":404,"detail":"No endpoint at this path","errorCode":"NOT_FOUND"}'`. Güvenlik başlıkları ve CSP (`font-src 'self'`) referanstaki gibi, her location'da. `/assets/` uzun cache.
- **D-4 dokümanlar:** `deployment/CLAUDE.md` (gerçek servisler, komutlar; pgAdmin hedefi "sonraki task") ve `README.md` (gereksinimler: Docker Desktop, Java 26, Node 26; çalıştırma; `../embedding-pgvector` ile port çakışması).
- Lokal model gerekmiyor; indirilecek model yok.

## Açık sorular ve riskler
1. Kart soru 1 (açıklamalardaki değişken değerler) ve soru 2 (yeni bağımlılıklar) cevaplanmadan F-5'in iki satırı ve F-1/B-1 kesinleşmez. Plan önerilen seçeneklere göre yazıldı.
2. T-B5/T-R5: Spring, `Accept` istemcinin istemediği bir türdeyse 406 gövdesini yazamayabilir. Tester gözlenen davranışı raporlar; gövde yoksa sadece status doğrulanır. Bu durum sözleşmeyi değiştirmez.
3. Postgres init script sadece boş volume'da çalışır. Script değişirse `down -v` gerekir; README'de yazılır.
4. Kapsam dışı not (lead): kök `CLAUDE.md`'deki "Durum: henüz kod yok; modüllerde sadece `CLAUDE.md` var (TASK-001)" satırı ve `backend-conventions` / `frontend-conventions`'daki "Durum" notları bu task'tan sonra eskir. Sahipsiz dosyalar; kapanışta lead günceller.
5. Kapsam dışı not: `problem-details-rfc9457` şablonu `HandlerMethodValidationException`'ı `BAD_REQUEST`'e çeviriyor, oysa `backend-conventions` bölüm 6 `VALIDATION_FAILED` istiyor. Bu planda handler'da düzeltiliyor (Kararlar #13). Şablonun kendisi `.claude/**` altında; lead'in güncellemesi önerilir.
6. `../embedding-pgvector` sistemi açıksa 5432, 8080 ve 3000 portları çakışır; önce o `down` edilir.

STATUS: DONE
