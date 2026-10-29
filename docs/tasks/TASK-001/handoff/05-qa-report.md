TASK: TASK-001
# QA raporu

Bu bir altyapı/doküman task'ı. Proje iskeleti, uygulama kodu, test ve Docker dosyası yok. Kural kontrolünü `01-plan.md` "Test senaryoları" bölümündeki K1-K20 komutlarıyla repo kökünden kendim çalıştırdım. Çıktılar rapordakilerle değil, kendi çalıştırmalarımla karşılaştırıldı.

## Kullanılan skill'ler
`code-review-and-quality`. Diğer skill'ler uygulanamaz: kod, DB, akış, model ve yeni ekran yok. Skill dosyalarının içeriğini doküman olarak inceledim.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|-----------------------|--------------------|-------|
| 1 | Kapsamdaki dosyalar | K1 (`.claude` envanteri), K2 (klasör yapısı: `handoff/.gitkeep`, `docs/tasks/.gitkeep`, `_TEMPLATE.md`, `docs/api/openapi.yaml`, dört modülde sadece `CLAUDE.md`) | ✅ |
| 2 | Eksiksizlik | K1: referansla `diff`'te `<` satırı yok. `>` satırları tam olarak `rag-evaluation/SKILL.md`, `rag-pipeline/SKILL.md`, `rag-pipeline/references/indexing.md`, `sse-streaming/SKILL.md`. Referansın `docs/tasks/TASK-00X` geçmişi taşınmamış | ✅ |
| 3 | Projeye özgülük | K4 (referans işi grep'i, iki komut): 0 satır. K5: 0 satır. K5b: `workspace` geçen her satır `/api/workspaces/42`, "adreste çalışma alanı yoktur" ya da `vite-patterns` "workspace kökü" (genel anlam). K15 (skill açıklamaları): 0 satır. K20: 0 satır | ✅ |
| 4 | `../embedding-pgvector` atıfları | K3: 20 satırın tamamı M5 tablosundaki dosya ve atıf türüne uyuyor (kök `CLAUDE.md`, `rag.md`, üç modül `CLAUDE.md`'si, `deployment/CLAUDE.md`, üç agent, `backend-conventions`, `frontend-conventions`, `e2e-testing`, `react-state-management/references/details.md`, `indexing.md`). `README.md`, `KULLANIM-KILAVUZU.md`, `_TEMPLATE.md`, `docs/api`: 0. Hepsi "okunur kalıp referansı" ve yazma yasağını belirtiyor | ✅ |
| 5 | Retrieval kararı verilmiş ve işlenmiş | Onay kartında `ONAY: VERİLDİ`. K7: eski karar ifadeleri 0; "bu projeye göre yeniden yazılır" 3 dosyada (kök, `rag.md`, `backend-conventions`); "tek bir çalışma alanı vardır" `rag.md` ve `retrieval-api/CLAUDE.md`'de | ✅ |
| 6 | Ürün hedefi yazılı | K6: "sol yarısının aynısı" 6/6 dosyada (`rag.md`, kök, `web/CLAUDE.md`, `frontend-design`, `frontend-conventions`, `e2e-testing`), "ekrana gelmez" hepsinde ≥ 1. M1'in "Sağ yarı … ekrana gelmez." cümlesi birebir 7 dosyada (ayrıca `backend-conventions`) | ✅ |
| 7 | Orkestrasyon aynı, SSE/model/reranker tutarlı | Kök tablo ve 1-11 numaralı iş akışı maddeleri duruyor. K13: `rag-pipeline` 7, `sse-streaming` 9, `rag-evaluation` 8 dosyada bağlı. Ortak metinler M1-M7 ilgili dosyalarda aynı | ✅ |
| 8 | Tutarlılık | K10: handoff adları tam 7 (`00` … `05`). K11: agent "yazarsın" satırları kök tabloyla aynı (planner, backend-coder, frontend-coder, tester, devops, qa). devops'ta `ollama.md`, `README.md` var; `03-test-report` "plan tester'ı çalıştırıyorsa" koşullu. K12: portlar M3 anlamında (8090 assistant-api, 8080 retrieval-api, 8081 reranker, 11434 Ollama, 3000 site, 5173 Vite, 5432, 5050). K19: eski `backend/`, `frontend/` yolları 0 satır | ✅ |
| 9 | Eksik skill | `rag-pipeline` (+`references/indexing.md`), `sse-streaming`, `rag-evaluation` var ve bağlı (K13) | ✅ |
| 10 | Uygulayan | K17: backend raporu sadece `assistant-api/CLAUDE.md` + `retrieval-api/CLAUDE.md`; frontend raporu sadece `web/CLAUDE.md`; devops raporu sadece `deployment/CLAUDE.md` + `README.md`. Sahipsiz dosyalar lead'in | ✅ |
| 11 | Kod/Docker/test yok, model indirilmez | K18: dört modülde `CLAUDE.md` dışında dosya yok (`find` boş); `backend/` ve `frontend/` yok (`OK`) | ✅ |
| 12 | Dil kuralı | K16 örneklem: yeni 3 skill + `indexing.md`'de Türkçe identifier yok; `react-patterns`, `frontend-a11y`, `react-state-management` hook örnekleri İngilizce. Genel tarama (`const/function/class/record` + Türkçe harf): 0 satır. UI metinleri Türkçe | ✅ |
| 13 | Frontmatter ve `settings.json` | K8: çıktı yok (tüm skill ve agent'larda `name`/`description`/`tools` geçerli). K9: `json.tool` OK, yeni yol/izin satırı sayısı 6, eski yol 0 | ✅ |
| 14 | Kılavuz komut ve yolları var | K14: tek "YOK" `ollama.md` (kılavuzda "ilk model task'ında oluşturulur" diye anılıyor, beklenen) ve `TASK.md` (kalıp ad, `docs/tasks/TASK-XXX/TASK.md`) | ✅ |
| 15 | Onay kartında karşılaştırmalı envanter | `00-onay-karti.md` envanter özeti, tam tablo `01-plan.md` "Dosya envanteri" | ✅ |
| 16 | `docs/api/openapi.yaml` sadece iskelet | `paths: {}`, ortak `Page`/`Size`/`ProblemDetail`; endpoint yok | ✅ |

Lead'in bilerek yaptığı sapmalar (kurala karşı değerlendirildi):
1. "Gönder" / `aria-label="Soru"`: `react-patterns`, `frontend-a11y`, `frontend-conventions` (satır 46) ve `react-testing` örneklerinde tutarlı. "Soruyu gönder" metni hiçbir yerde kalmamış. Ürün hedefiyle (lab metni) uyumlu. ✅
2. Ayar anahtarları lab adlarıyla ve "kesin adlar ilk assistant-api task'ında" notuyla yazılmış (`02-backend-implementation.md` sapma 1). ✅
3. `Source.ingesting(...)`: `spring-boot-engineer` ve `jpa-patterns` gibi dosyalarda tutarlı kullanılıyor; `Source.create` kalıntısı yok. ✅
4. Yeniden yazılan cümleler anlamı koruyor; K4 temiz. ✅

## Sözleşme uyumu
Uygulanamaz (kod yok). `docs/api/openapi.yaml` iskelet ve geçerli yapıda. `docs/api/sse.md` ve `websocket.md` bilerek yok (kapsam dışı, ilk akış/WebSocket task'ında oluşturulur); dokümanlar bu dosyalara "oluşturulacak" diye atıf yapıyor. Path önekleri (`/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api) kök `CLAUDE.md`, `web/CLAUDE.md`, `deployment/CLAUDE.md`, `vite-patterns`, `openapi.yaml` başlığında aynı. SSE olay örneklerinin kendi içinde tutarlılığı: `useAnswerStream` `{ answer, status, errorCode, send }` döndürüyor; `react-patterns`, `frontend-a11y` ve `react-state-management` örnekleri bu alanları aynı adlarla kullanıyor.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `./mvnw verify`, `npm run build`, `npm run lint`, `npm test`, coverage | Uygulanamaz (iskelet yok) | Evet (coder raporları da çalıştırmadığını yazıyor) |
| K1-K20 (plan kontrol komutları) | Yukarıdaki tabloda; hepsi beklenen sonuçta | Evet |
| `python3 -I -m json.tool .claude/settings.json` | OK | Evet |

## Mutation testi
Kurulu değil (uygulanamaz).

## Uçtan uca
Uygulanamaz: Playwright ve çalışan sistem yok; devops sistemi açık bırakmadı, model ve gerçek cevap denemesi yok.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional (tur 1'de giderildi) | `.claude/skills/spring-boot-openapi-documentation/references/{configuration,troubleshooting,build-integration}.md` | Örneklerde `localhost:8080` (retrieval-api portu) geçiyor; SKILL.md iki servisi ayırıyor (8090/8080) ama referans dosyalar genel Spring örneği olarak 8080'de kalmış. Yanıltıcı değil, genel teknik içerik | lead |
| FYI | `README.md`, `KULLANIM-KILAVUZU.md`, `deployment/CLAUDE.md`, `*/CLAUDE.md` | `ollama.md`, `docs/api/sse.md`, `websocket.md` henüz yok; "oluşturulur" diye anılıyor, çelişki yok | — |
| FYI | `deployment/CLAUDE.md:25` | `../embedding-pgvector` ile port çakışması uyarısı doğru ve gerekçeli | — |
| FYI | tüm bölümler | Cevap/model/akış davranışı bu task'ta doğrulanamaz; ilk model ve ilk akış task'larında gerçek modelle denenmeli | — |

Critical veya Required bulgu yok.

| Required (tur 2'de giderildi) | `.claude/skills/spring-boot-openapi-documentation/references/advanced-configuration.md:125`, `model-documentation.md:72`, `controller-documentation.md:35`; `sql-optimization-patterns/SKILL.md:87`, `references/details.md:71,81,88,209-211,225`; `flyway-migrations/templates/V1__create_table_template.sql:21`; `spring-websocket/SKILL.md:22-23`; `frontend-conventions/SKILL.md:311` | Kaynak kimliği `301`/`302` referans projenin (Atlaskap) örnek verisi. Plan Ç-2 "Kimlikler: kaynak 301 → 10001 (lab)" diyor ve TASK Kural 3 örnek isimlerinin çevrilmesini ister; K4 kalıbı bunu yakalamıyordu. Skill'lerin başka yerlerinde (`e2e-testing`, `react-testing`, `react-best-practices`) zaten `10001` kullanılıyor, yani dosyalar birbiriyle tutarsız | lead |

## Düzeltme
**lead:**
1. Yukarıdaki dosyalarda kaynak kimliği `301` → `10001`, `302` → `10002` (ör. `/api/workspaces/42/sources/10001`, `/topic/sources/10001/ingestion`, `WHERE source_id = 10001`, `VALUES (10001, 742), (10002, 12)`, `flyway` şablon yorumu "Seed kaynaklarının id'leri sabittir (10001, 10002 ...)" ile sequence çakışmasına dikkat: sequence 10001'den başlıyorsa seed id'si sequence'ten ayrı bir aralıkta ya da yorum "tek seed kaynak 10001, sequence 10002'den başlar" olarak düzeltilmeli). Doğrulama: `grep -rnE "\b(301|302)\b" .claude CLAUDE.md rag.md README.md KULLANIM-KILAVUZU.md assistant-api retrieval-api web deployment` → 0 satır.

## Düzeltme turu 1
Lead'in 8 maddesi ve K1-K20 baştan çalıştırıldı.
| Madde | Kanıt | Sonuç |
|-------|-------|-------|
| 1. `ck_sources_kind` sadece `'PDF'` | `flyway-migrations/SKILL.md:79`, `templates/V1__create_table_template.sql:38` | ✅ |
| 2. Dosya parametresi açıklaması | `spring-boot-openapi-documentation/SKILL.md:90`, `references/complete-examples.md:32`: "PDF file, not empty, at most 20 MB"; `.markdown` kalıntısı yok | ✅ |
| 3. `spring-websocket` proje modeli | `SKILL.md:14,17`: STOMP henüz yok, SSE + polling, kalıp "yeni sürüm yükle" | ✅ |
| 4. `spring-transactions` proxy örneği | `SKILL.md:32-40` `saveTurn`; `reingest` yok | ✅ |
| 5. Dead code örneği | `code-review-and-quality/SKILL.md:255` `features/conversation/OldTurnCard.tsx` | ✅ |
| 6. `EvaluationPage` / `degerlendirme` | `frontend-performance/references/optimization-patterns.md:181-189`; `SearchPage` yok | ✅ |
| 7. `frontend-security`, `java-code-review`, `springboot-patterns` | `frontend-security/SKILL.md:160` "işlenen kaynağa yeni sürüm yükleyemez" | ✅ |
| 8. Port notu | `configuration.md:22`, `troubleshooting.md:71`, `build-integration.md:75` (8080/8090) | ✅ |
| Ek tarama kalıbı | `grep -rnE "TICKET_EXPORT|WEB_CRAWL|\.csv|SearchPage|features/search|reingest|Kaynağı sil|Lab'da dene" …` → 0 satır | ✅ |
| K1-K20 tekrar | K1 `<` 0, `>` aynı 4 dosya; K3 atıflar M5 listesinde; K4/K5/K7(ilk)/K15/K18/K19/K20 = 0; K6 6/6; K7 3 ve 2; K8 çıktı yok; K9 OK/6/0; K10 7 ad; K13 7/9/8; K14 sadece `ollama.md` ve `TASK.md` (beklenen) | ✅ |
| Kendi ek taramam (referansa özgü örnek veri) | Kaynak kimliği 301/302 kalıntısı bulundu (bulgu tablosunda Required). `SearchResultResponse`, `searchChunks` retrieval-api'nin arama ucudur, kalır | ❌ |

## Düzeltme turu 2
Lead 301/302 kalıntılarını düzeltti; K1-K20 ve ek kalıplar baştan çalıştırıldı.
| Madde | Kanıt | Sonuç |
|-------|-------|-------|
| Kaynak kimliği `301`/`302` → `10001`/`10002` | `grep -rnE "\b(301|302)\b|Şifre sıfırla|TICKET_EXPORT|WEB_CRAWL|\.csv|SearchPage|features/search|reingest|Kaynağı sil|Lab'da dene|markdownWithChunks" .claude CLAUDE.md rag.md README.md KULLANIM-KILAVUZU.md assistant-api retrieval-api web deployment` → 0 satır | ✅ |
| Flyway şablonu çakışması | `V1__create_table_template.sql:20-23`: "Kaynaklar seed ile gelmez; ilk kaynak sequence'ten 10001 alır, sonrakiler 10002 …", `START WITH 10001`. Lab'daki davranışla uyumlu, çakışma yok (önerimden farklı ama doğru çözüm) | ✅ |
| Ek düzeltmeler (parça metinleri, heading örneği, `pdfWithChunks`) | `pdfWithChunks` `websocket-stomp-testing-patterns.md:102,111`'de tutarlı; `markdownWithChunks`, "Şifre sıfırla" kalıntısı yok | ✅ |
| K1-K20 | K1 `<` 0 ve `>` aynı 4 dosya; K3 20 satır hepsi M5'te; K4 0/0; K5 0; K6 6/6; K7 0/3/2; K8 çıktı yok; K9 OK/6/0; K10 7 ad; K13 7/9/8; K14 sadece `ollama.md`, `TASK.md` (beklenen); K15 0; K18 0 dosya ve `OK`; K19 0; K20 0 | ✅ |
| Kendi ek taramam (Atlas, Dalga, ornek-dokuman, .docx, .txt, text/plain) | `.claude` içinde 0 satır | ✅ |

Critical veya Required bulgu kalmadı.

STATUS: DONE
