TASK: TASK-002
# Test raporu

## Kullanılan skill'ler
writing-good-tests, frontend-conventions, react-testing, e2e-testing, backend-conventions, spring-testing-fundamentals, spring-mvc-testing.

## Senaryo → test eşlemesi
| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| T-B1/T-R1 | K6 bilinmeyen path → 404 NOT_FOUND | `{assistant,retrieval}/exception/GlobalExceptionHandlerTest#givenUnknownPath_whenGet_thenReturns404ProblemWithNotFoundErrorCode` | geçti |
| T-B2/T-R2 | 405 | `…#givenGetOnlyEndpoint_whenDelete_thenReturns405MethodNotAllowed` | geçti |
| T-B3/T-R3 | bozuk JSON → 400 BAD_REQUEST | `…#givenMalformedJson_whenPost_thenReturns400BadRequest` | geçti |
| T-B4/T-R4 | 415 | `…#givenTextPlainBody_whenPostJsonEndpoint_thenReturns415UnsupportedMediaType` | geçti |
| T-B5/T-R5 | 406 | `…#givenJsonOnlyEndpoint_whenAcceptTextCsv_thenReturns406NotAcceptable` | geçti (sadece status doğrulandı, aşağıya bak) |
| T-B6/T-R6 | gövde doğrulaması | `…#givenBlankRequiredField_whenPost_thenReturns400ValidationFailedWithFieldViolation` | geçti |
| T-B7/T-R7 | query doğrulaması | `…#givenNegativePageParam_whenGet_thenReturns400ValidationFailedWithPageViolation` | geçti |
| ek | `@RequestParam(name=…)` ile ihlal alanı adı | `…#givenNamedRequestParamBelowMinimum_whenGet_thenViolationFieldUsesDeclaredParamName` | geçti (mutant kapatmak için eklendi) |
| T-B8/T-R8 | 500 iç ayrıntı sızdırmaz | `…#givenHandlerThrowsUnexpectedException_whenGet_thenReturns500InternalErrorWithoutInternalDetails` | geçti |
| ek | geçerli istek problem üretmez | `…#givenValidRequest_whenGet_thenHandlerRunsWithoutProblemResponse` | geçti |
| T-B10 | Flyway kurulu, migration yok | `AssistantApplicationIntegrationTest#givenEmptyDatabase_whenContextStarts_thenFlywayRunsWithNoMigrations` | geçti |
| T-B11 / T-R12 | Sağlık ucu (`"status":"UP"` içerir, `components` yok) | `…IntegrationTest#givenRunningApplication_whenGetActuatorHealth_thenReturnsUp` | geçti |
| T-B12 | gerçek sunucuda 404 | `AssistantApplicationIntegrationTest#givenRunningApplication_whenGetUnknownAssistantPath_thenReturns404NotFoundProblem` | geçti |
| T-R10 | Flyway V1 | `RetrievalApplicationIntegrationTest#givenFreshDatabase_whenContextStarts_thenFlywayAppliedVersion1` | geçti |
| T-R11 | vector eklentisi | `RetrievalApplicationIntegrationTest#givenFreshDatabase_whenContextStarts_thenVectorExtensionIsInstalled` | geçti |
| T-R13 | gerçek sunucuda 404 | `RetrievalApplicationIntegrationTest#givenRunningApplication_whenGetUnknownWorkspacesPath_thenReturns404NotFoundProblem` | geçti |
| T-F1 | kök → Önizleme | `web/src/app/App.test.tsx#givenRootPath_whenRendered_thenRedirectsToPreviewScreen` | geçti |
| T-F2 | 4 ekran metni | `App.test.tsx#givenScreenPath_whenRendered_thenShowsLabBreadcrumbTitleAndDescription` (it.each ×4) | geçti |
| T-F3 | menüden gezinme | `App.test.tsx#givenPreviewScreen_whenSourcesMenuItemClicked_…` | geçti |
| T-F4 | üst bar | `App.test.tsx#givenAnyScreen_whenRendered_thenShowsLogoTextAndLiveConfigBadge` | geçti |
| ek | menü 4 öğe, lab sırası | `App.test.tsx#givenAnyScreen_whenRendered_thenShowsAllFourMenuItemsInLabOrder` | geçti |
| T-F5 | sayaç/kullanıcı alanı yok | `App.test.tsx#givenAnyScreen_whenRendered_thenShowsNoConversationCounterAndNoUserArea` | geçti |
| T-F6 | bilinmeyen adres (`/yok`, `/konusmalar/12`) | `App.test.tsx#givenUnknownPath_whenRendered_thenShowsNotFoundScreenWithNoMenuItemSelected` (×2) | geçti |
| T-F7 | belge başlığı | `App.test.tsx#givenSourcesPath_whenRendered_thenSetsDocumentTitle` | geçti |
| T-F8 | vitest-axe | `App.test.tsx#givenShell_whenRendered_thenHasNoAxeViolations` (×2) | geçti |
| E-1…E-6 | Playwright | `web/e2e/shell.spec.ts` (6 test, adlar plandaki gibi) | **çalıştırılmadı** (sistem ayakta değil); `npx playwright test --list` ile 6 test listelendi, derleniyor |

Yardımcı dosyalar: `web/src/test/setup.ts` (jest-dom, vitest-axe matcher, cleanup), `web/src/test/renderWithProviders.tsx`, `web/src/test/vitest-axe.d.ts`; backend'de test-only `ErrorProbeController` (`src/test`, production'a girmez). Retrieval probe path'i `/api/workspaces/probe-ws/probe/**`.

## Mutation
- Web: Stryker kurulu değil (plan dışı); çalıştırılmadı.
- Backend: PIT 1.30.1 (`./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`), iki serviste aynı: 32 mutant, 28 öldü (%88), test strength %90. Not: `pitest:mutationCoverage`'ı `test-compile` olmadan çalıştırmak MINION_DIED veriyor (surefire argLine'daki `${org.mockito:mockito-core:jar}` çözülmüyor); CLAUDE.md'deki komut doğru.
- İlk koşuda hayatta kalan type/validation/named-param mutantları için assertion eklendi (500 ve VALIDATION_FAILED `type`, `@RequestParam(name)` alan adı) ve öldürüldü.
- Kalan 4 mutant (iki serviste aynı), task kuralı değil: `handleUnexpectedException` içinde `setTitle` ve `setInstance` kaldırılması (Spring `instance`'ı zaten kendisi dolduruyor, eşdeğer mutant; `title` openapi/plan kuralı değil), `validationFailed` içinde `setTitle`, `hasErrorCode` içinde negated conditional (çerçeve yolundan ulaşılamayan dal: gövde zaten errorCode taşıyorsa; yalnızca ileride handler'ın kendi errorCode'lu ProblemDetail'i bu yoldan dönerse test edilebilir).

## Komutlar ve sonuçları
- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 13 test (10 controller + 3 integration), 0 hata; JaCoCo %80 eşiği dahil "All coverage checks have been met"; satır coverage 43/45 (%96).
- `cd retrieval-api && ./mvnw verify` → BUILD SUCCESS, 14 test, 0 hata; JaCoCo geçti; 43/45 (%96).
- `cd web && npm test` → 1 dosya, 14 test geçti. `npm run test:coverage` → satır/statement/function %100 (13/13), eşik %80 geçti.
- `cd web && npm run build` → geçti. `npm run lint` → hatasız.
- Integration testleri Testcontainers `pgvector/pgvector:0.8.1-pg18` ile gerçek PostgreSQL'e karşı çalıştı (Docker erişilebilir).

## Gözlemler
- T-B5/T-R5 (406): sadece status doğrulandı; plandaki risk #2 gereği gövde doğrulanmadı.
- Integration testinde `TestRestTemplate` kullanılamadı (pom'da `spring-boot-restclient` yok); JDK `HttpClient` + `@LocalServerPort` kullanıldı, dependency eklenmedi.
- vitest-axe jsdom'da `HTMLCanvasElement.getContext` "Not implemented" uyarısı yazıyor (renk kontrastı kuralı); zararsız, test sonucu etkilenmiyor. Kontrast gerçek tarayıcıda qa'da (Q-1) kontrol edilmeli.
- Sağlık ucu gövdesi birebir değil, `"status":"UP"` içeriği doğrulandı.

## Çalıştırılamayanlar
- Playwright E2E (E-1…E-6) ve nginx/Compose yolu: çalışan Docker sistemi gerekiyor. Sadece yazıldı ve `--list` ile listelendi; `cd web && npx playwright install chromium && npm run test:e2e` devops (D-S6) ve qa tarafından çalıştırılacak. E-3/E-4/E-5 nginx yönlendirmesine bağlı.
- D-S2/D-S3 (çapraz veritabanı erişimi) ve D-S4/D-S5 devops smoke işidir; backend testleri çapraz erişimi kapsamaz.

## Düzeltme
Yok.

## Düzeltme turu 1
- Madde: Testcontainers imajı plandan sapıyordu (`pgvector/pgvector:pg17`; plan `0.8.1-pg18`). Bilinçli sebep yoktu.
- Test: `AssistantApplicationIntegrationTest` ve `RetrievalApplicationIntegrationTest` imajı `pgvector/pgvector:0.8.1-pg18` olarak değiştirildi.
- Sonuç: `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, 13 test, JaCoCo geçti. `cd retrieval-api && ./mvnw verify` → BUILD SUCCESS, 14 test, JaCoCo geçti. T-R10 (Flyway V1) ve T-R11 (vector eklentisi) pg18 üzerinde de geçiyor.

STATUS: DONE
