TASK: TASK-018
# Backend uygulama raporu

## Kullanılan skill'ler
- `backend-conventions`, `springboot-patterns`, `problem-details-rfc9457` (zorunlu set)
- `springboot-verification` (planın "Backend planı" ek skill'i)

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Backend planı md. 1 / 4a, K9 | `assistant-api/pom.xml`, `retrieval-api/pom.xml` | `pitest-maven` `<configuration>`'a `<mutationThreshold>80</mutationThreshold>` eklendi. Başka ayar değişmedi. Eşik `verify`'a bağlı değil; sadece `mutationCoverage` goal'ünde çalışır |
| Backend planı md. 2 ("Komutlar") | `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md` | Mutation satırının sonuna `eşik %80 (mutationThreshold); skor altındaysa komut BUILD FAILURE verir` eklendi |
| Backend planı md. 2 ("Teknoloji") | `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md` | PIT tanımı `(hedef com.rasitbank.<servis>.*, eşik %80)` oldu |
| Backend planı md. 3 | — | Uygulama kodu (`src/main/**`) değişmedi |

## Değişen dosyalar
- Değişen: `assistant-api/pom.xml`, `retrieval-api/pom.xml`, `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md`
- Yeni / silinen: yok
- Eklenen dependency / plugin: yok (PIT zaten vardı; sadece ayar eklendi). "Henüz olmayanlar" listeleri bu yüzden değişmedi.

## Plandan sapmalar
Yok.

## Doğrulama
JaCoCo eşik kontrolü **açık** (kapatılmadı) olarak tam `verify` koşuldu:
- `cd assistant-api && ./mvnw verify` → BUILD SUCCESS, Tests run: 441, Failures: 0, Errors: 0, Skipped: 0; "All coverage checks have been met."
- `cd retrieval-api && ./mvnw verify` → BUILD SUCCESS, Tests run: 344, Failures: 0, Errors: 0, Skipped: 0; "All coverage checks have been met."

PIT (`mutationCoverage`) bu adımda koşulmadı: plana göre C7/C8 tester'ın (qa tekrarlar) işidir.

## Tester için notlar
- C7/C8 komutları: `cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` ve `cd retrieval-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`. Skor %80'in altındaysa PIT kendisi `BUILD FAILURE` verir ("Mutation score of X is below threshold of 80"); rapor `target/pit-reports/index.html` ve `mutations.xml`.
- `mutationThreshold` öldürülen mutant oranına bakar (timeout'lar öldürülmüş sayılır; testi olmayan mutantlar "NO_COVERAGE" olarak skoru düşürür). Bu task'ta `coverageThreshold` / `testStrengthThreshold` eklenmedi; plan sadece mutation eşiği istiyor.
- Uygulama kodu değişmediği için yeni seam, fixture ya da sahte model noktası yok; mevcut sahte `ChatModel`, `RetrievalClient`, `RerankClient`, `EmbeddingClient` aynen geçerli.

## Düzeltme

STATUS: DONE
