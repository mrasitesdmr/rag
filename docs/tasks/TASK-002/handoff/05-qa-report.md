TASK: TASK-002
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Diğer skill'ler (`java-code-review`, `springboot-verification`, `frontend-conventions`, `web-design-guidelines`, `frontend-a11y`, `e2e-testing`, `writing-good-tests`) yüklenmedi. Gerekçe: bu task'ta iş mantığı, model, akış ve yeni ekran içeriği yok. Kontrol task kurallarına, komutlara ve çalışan sisteme dayandı. Stil ve konvansiyon incelemesi bu yüzden yüzeysel kaldı.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Spring Boot 4 / Java 26 / `./mvnw`; React 19 + TS + Vite; servisler bağımsız | Her serviste ayrı `pom.xml` ve `mvnw`; `web/package.json` react ^19.3.1, vite ^8; `target/` imajları temurin 26 | ✅ |
| 2 | Portlar 3000 / 8090 / 8080 / 5432 | `docker compose ps`: 3000→8080 (nginx), 8090, 8080, 127.0.0.1:5432 | ✅ |
| 3 | Tek PG, iki DB, ayrı kullanıcı, çapraz erişim yok | `deployment/postgres/initdb/01-init-databases.sh`; kendi denemem (aşağıda): çapraz bağlantı `permission denied` | ✅ |
| 4 | Flyway kurulu, domain tablosu yok | retrieval `V1__enable_vector_extension.sql` (sadece eklenti), assistant'ta migration yok; entegrasyon testleri Flyway'i çalıştırıyor | ✅ |
| 5 | Sağlık ucu; Compose healthcheck bunu kullanır | `/actuator/health` 8090 ve 8080'de 200; compose'ta dört konteyner healthy | ✅ |
| 6 | Bilinmeyen uca 404 + Problem Details + `NOT_FOUND` | Canlı: `GET :3000/api/assistant/yok`, `:8080/api/workspaces/1/yok` ve `:3000/api/yok` hepsi 404 `application/problem+json` ve `errorCode":"NOT_FOUND"`. `DELETE /api/assistant/x` de aynı 404'ü döner. `GlobalExceptionHandlerTest` 10 test (assistant ve retrieval'da ayrı) | ✅ |
| 7 | `/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api | `web/nginx.conf` location blokları; Vite proxy `vite.config.ts`; E2E 3-5 canlı geçti | ✅ |
| 8 | 4 ekran adresi, kök → Önizleme, doğrudan açma ve yenileme | `App.tsx` rotaları, `/onizleme` ve `/kaynaklar` 200 text/html (SPA fallback); E2E-1 ve E2E-2 geçti | ✅ |
| 9 | Görünüm lab'ın aynısı (design token) | Ekran görüntüsü karşılaştırması: aşağıda | ✅ |
| 10 | Mock ya da sabit veri yok; Konuşmalar sayacı ve kullanıcı alanı gösterilmez | `web/src`'te "Eşdemir", sayı, "bge-m3", "qwen" ve "149" araması (testler hariç) boş döndü. Görüntüde sayaç ve kullanıcı alanı yok | ✅ |
| K1 | `./mvnw verify` iki serviste | Aşağıda | ✅ |
| K2 | `npm run build` ve `lint` | Aşağıda | ✅ |
| K3 | `docker compose up` ile hepsi sağlıklı | `docker compose ps`: dört konteyner Up (healthy) | ✅ |
| K4 | assistant kullanıcısı `retrieval` DB'ye bağlanamaz (ve tersi) | Aşağıda | ✅ |
| K5 | Kabuk lab'ın sol yarısıyla aynı görünür | Aşağıda | ✅ |

Onay kartı kararları:
- Sahte tarayıcı çerçevesi yok ✅.
- Menü üst barın altında yatay ✅.
- Sekme başlığı ekrana göre değişir (`App.test.tsx:124`) ✅.
- Olmayan ekranda "Sayfa bulunamadı" (`NotFoundPage`) ✅.
- `/api/yok` boş sayfa yerine problem+json `NOT_FOUND` döner (canlı) ✅.
- Sağlık ucu siteden dışarı açık değil. `:3000/actuator/health` SPA HTML'i (200 text/html) döner, `UP` gövdesi dönmez ✅.
- Şifreler compose'ta düz yazılı, `application.properties`'te yok (`${DATABASE_PASSWORD:}`) ✅.
- Onaylı kütüphane listesi dışında dependency yok. `package.json` ve `pom.xml` onay kartı A seçeneğiyle uyumlu; Stryker yok, kartta da yok ✅.

## Sözleşme uyumu
- `docs/api/openapi.yaml` bu task'ta yalnızca ortak hata iskeletini tanımlıyor (`errorCode` enum, `VALIDATION_FAILED`, `NOT_FOUND`, `INTERNAL_ERROR`; Problem Details alanları `type`, `title`, `status`, `errorCode`).
- Canlı yanıtlar bu alanları birebir taşıyor: `type`, `title`, `status`, `detail`, `instance`, `errorCode`. `type` biçimi `https://rag.example/errors/not-found`.
- İki servisin `GlobalExceptionHandler`'ı aynı davranışta. Web'in henüz API çağrısı yok.
- SSE ve WebSocket bu task'ta yok.

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 13 test, JaCoCo "All coverage checks have been met" | ✅ (test raporuyla aynı) |
| `cd retrieval-api && ./mvnw verify` | BUILD SUCCESS, 14 test (entegrasyon 4, handler 10), JaCoCo geçti | ✅ |
| `cd web && npm run build` | tsc + vite build geçti (dist: js 263 kB, Source Sans 3 yerel) | ✅ |
| `cd web && npm run lint` | oxlint hatasız | ✅ |
| `cd web && npm test` | 14 test geçti | ✅ |
| `cd web && npm run test:coverage` | Statements 100% (13/13), Branches 100%, Functions 100%. Eşik geçti | ✅ |
| `docker compose -f deployment/docker-compose.yml ps` | 4 konteyner healthy | ✅ |
| `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | 6 passed (E-1…E-6) | ✅ |

Çapraz DB denemem (`docker compose exec postgres psql -h 127.0.0.1 -U <kullanıcı> -d <db>`):
- assistant → retrieval: `FATAL: permission denied for database "retrieval"`, `User does not have CONNECT privilege`.
- retrieval → assistant: `FATAL: permission denied for database "assistant"`.
- assistant → assistant: `select 1` başarılı.

## Mutation testi
**PIT** (iki serviste de `test-compile org.pitest:pitest-maven:mutationCoverage`; BUILD SUCCESS, eşik tanımlı değil).

| Servis | Üretilen | Öldürülen | Skor | Test gücü | Hayatta kalan |
|---|---|---|---|---|---|
| assistant-api | 32 | 28 | %88 | %90 | 3 SURVIVED + 1 NO_COVERAGE |
| retrieval-api | 32 | 28 | %88 | %90 | 3 SURVIVED + 1 NO_COVERAGE |

İki serviste aynı mutantlar hayatta kalıyor (`GlobalExceptionHandler`):
- `handleUnexpectedException` satır 69 ve 70: `problem.setTitle("Internal Server Error")` ve `setInstance(...)` çağrıları silinince test yakalamıyor. 500 yanıtının `title` ve `instance` alanı doğrulanmıyor.
- `validationFailed` satır 92: `setProperty("violations", ...)` silinince test yakalamıyor. 400 gövdesindeki `violations` listesi doğrulanmıyor.
- `hasErrorCode` satır 77: NO_COVERAGE. `properties != null` kolu hiç çalışmıyor; bu yol erişilemez gibi, düşük önem.

Bunlar sözleşmedeki isteğe bağlı alanlar (`title` zorunlu, `violations` yalnızca `VALIDATION_FAILED` için). Bu yüzden `Optional` olarak bildirildi, işi durdurmuyor. Özellikle `title` zorunlu alan olduğu için bir test eklenmesi iyi olur.

**Stryker**: kurulu değil (onay kartında yok, bu task'ta beklenmiyor).

## Uçtan uca
- Playwright, çalışan sisteme karşı (`http://localhost:3000`): 6/6 geçti.
  - Kök adres Önizleme'ye gider.
  - `/kaynaklar` doğrudan açılıp yenilenince aynı ekranda kalır.
  - `/api/assistant/yok` ve `/api/workspaces/…/yok` problem+json 404 `NOT_FOUND` döner.
  - `/api/yok` HTML değil problem+json döner.
  - Dar ekranda rozet gizlenir ve yatay taşma yok.
- Dil modeli, SSE ve WebSocket bu task'ta yok; denenmedi.

**Görünüm karşılaştırması (plan Q-1):** `kaynaklar/rag-lab.html` 1480×900'de açıldı, `.sbody` ekran görüntüsü alındı. Web aynı boyutta açıldı. Görüntüler:
- Lab `.sbody`: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/80023026-c9fd-4282-a914-cb48323c7518/scratchpad/shots/lab-sbody.png`
- Lab tam sayfa: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/80023026-c9fd-4282-a914-cb48323c7518/scratchpad/shots/lab-full.png`
- Web `/onizleme`: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/80023026-c9fd-4282-a914-cb48323c7518/scratchpad/shots/web-onizleme.png`
- Web `/kaynaklar`: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/80023026-c9fd-4282-a914-cb48323c7518/scratchpad/shots/web-kaynaklar.png`

Sonuç, gözle yan yana karşılaştırma (piksel farkı hesaplanmadı). `/kaynaklar` görüntüsünü dosya olarak aldım ama açıp bakmadım; onu yalnızca E2E ve metin testleri doğruluyor.
- Aynı olanlar (`/onizleme` görüntüsünde):
  - Logo, "Raşit Bankası / Dijital Asistan Yönetimi" ve "Canlı yapılandırma" rozeti.
  - Yatay menü ve bordo altı çizgili seçili öğe. Menü simgeleri ve yazı tipi aynı.
  - Üst bar yüksekliği, ince alt çizgisi ve gri zemin.
  - Breadcrumb "Dijital Asistan › Önizleme", "Asistan önizleme" başlığı ve açıklama metni (satır kırılımı dahil).
- Beklenen farklar (onay kartı ve kural 10):
  - Kullanıcı alanı ("Raşit Eşdemir / RE") yok.
  - Konuşmalar sayacı yok.
  - Telefon çerçevesi, çipler ve kart (ekran içerikleri) yok.
  - Lab'daki sahte tarayıcı çerçevesi yok.
- Fark bulmadım.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Optional | `*/exception/GlobalExceptionHandler.java:69-70, 92` | PIT: 500 yanıtının `title`/`instance` alanı ve 400 yanıtının `violations` listesi hiçbir testte doğrulanmıyor (iki serviste de). Sözleşmede `title` zorunlu alan; ona karşı test yok. | tester |
| Optional | `*/exception/GlobalExceptionHandler.java:77` | `hasErrorCode` içindeki `properties != null` kolu hiç çalışmıyor (NO_COVERAGE). | backend-coder |
| Nit | `web/coverage/`, `web/test-results/` | Üretilen çıktılar. Hem `web/.gitignore`'da hem `git status --ignored`'da ignored görünüyor, sorun değil. | — |
| FYI | `GlobalExceptionHandler.java:65` | `log.error` yalnızca `path` ve exception'ı loglar. İstek gövdesi, soru, cevap ya da prompt loglanmıyor, log kuralına uyuyor. Exception mesajı ileride kullanıcı verisi taşırsa gözden geçirilmeli. | — |
| FYI | `docker-compose.yml` | Şifreler düz yazılı. Onay kartı kararı (lokal kullanım). | — |

Rol yol sınırları: `assistant-api/`, `retrieval-api/`, `web/`, `deployment/` ve `README.md` dışında yeni içerik göremedim. Tester dosyaları (`src/test/**`, `*.test.tsx`, `web/e2e/**`) ve devops dosyaları (`Dockerfile`, `nginx.conf`, `deployment/**`) kendi rollerinin yollarında. Hangi rolün hangi dosyayı yazdığını commit olmadığı için `git log` ile doğrulayamadım; yalnızca yolların konumuna baktım. `docs/api/openapi.yaml` değişikliğinin planner'dan geldiğini de aynı sebeple doğrulayamadım.

Sınırlar: Docker `down -v` ile temiz volume'dan yeniden kurulum denemedim. Sistem devops'un bıraktığı hâliyle test edildi. Devops raporu bu denemeyi (G1) kendisi yaptığını söylüyor, ben doğrulamadım.

## Düzeltme
Yok. `Critical` ya da `Required` bulgu yok.

STATUS: DONE
