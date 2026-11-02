TASK: TASK-016
# Plan

## Özet
Bugün Değerlendirme ekranında sadece başlık var. Golden set, ölçüm ve metrik yok. Konuşmalar'da "bulamadım" turları hep BİLMİYOR görünüyor. Önizleme ve Konuşmalar'da cevabın geçtiği parçaya dair bir işaret yok.

Yeni davranış (onay kartı ve kullanıcının cevapları: 1-C, 2-A, 3-A, 4-A, 5-A):
- **Golden set:** Bir dosya olarak durur (`docs/rehber/golden/rehber-golden-v1.json`, biçimi aşağıda) ve `PUT /api/assistant/golden-set` ile yüklenir. assistant veritabanında saklanır. Doğru parça, parça kimliğiyle değil `answerText` ile işaretlenir; `answerText`, rehberden birebir kopyalanmış cümledir.
- **Doğru parçayı bulma:** Bu cümleyi içeren güncel parçaları retrieval-api'nin yeni `POST /api/workspaces/{workspaceId}/chunk-matches` ucu bulur. Normalize edilmiş metinde arar; büyük/küçük harf ve boşluk farkı yok sayılır. Eşleştirme kuralı tek yerde, retrieval-api'de durur.
- **Ölçüm:**
  - `POST /api/assistant/evals` → 202 döner, ölçüm arka planda tek thread'de yürür.
  - Ölçüm sadece retrieval (ve rerank) çalıştırır; dil modeli çağrılmaz.
  - Kayıtlar `eval_runs` ve `eval_run_questions` tablolarında durur.
  - Ekran `GET /api/assistant/evals/overview` ile okur ve ölçüm sürerken bu ucu yoklar. "Çalıştırmalar" tablosu `GET /api/assistant/evals` ile sayfalı gelir.
- **Önizleme ve Konuşmalar:** Soru golden set'teyse işaret görünür. Eşleşme, soru önerilerindeki normalizasyonla tam metindir.
  - Konuşmalar parça tablosunda "cevap burada" etiketi çıkar.
  - Önizleme parça kartında doğru parça sarı görünür.
  - Eşleşen "bulamadım" turu `MISSED` (KAÇIRDI) ya da `GENERATION_ERROR` (ÜRETİM HATASI) olur.
  - Durum saklanmaz, her okumada hesaplanır.

Değişen servisler:
- **assistant-api:** yeni `evaluation/` paketi ve V7 migration. `ContextRetriever`'a bir metot eklenir. `RetrievalClient`'a `findChunkMatches` eklenir. `TurnStatus`, `TurnStatusResolver`, `ConversationReadService`, `AnswerService` / `AnswerStreamer`, `TurnChunkResponse` ve `ContextChunk` değişir.
- **retrieval-api:** sadece `chunk/` paketine `findChunkMatches` eklenir; migration yok.
- **web:**
  - `features/evaluation` baştan yazılır.
  - `preview`'a `ContextChunksCard` işareti ve Değerlendirme'den gelen soruyu sorma eklenir.
  - `conversation`'a `TurnChunkTable` etiketi ve iki yeni durum eklenir.

Sözleşme dışı bir ek: TASK "Cevap akışı (SSE)" katmanını işaretlememişti, ama `sse.md`'deki `ContextChunk`'a `containsAnswer` alanı eklendi (Karar K7).

Uygulama sırası: retrieval-api `findChunkMatches` → assistant-api istemci + `GoldenAnswerLocator` → ölçüm → Konuşmalar/Önizleme işaretleri. Backend ve frontend sözleşmeye göre paralel çalışır.

Yeniden adlandırma yok. Mevcut adlar (`TurnStatusResolver.resolve`, `TurnResponse.from`) imza değiştirir (aşağıda).

## Çalışacak roller
1. planner.
2. lead (ana oturum): golden set dosyasını yazar (bölüm "Golden set dosyası"), kullanıcıya sunar ve onayını rapora kaydeder.
3. backend-coder: retrieval-api `findChunkMatches`; assistant-api tarafında `evaluation/` paketi, V7 migration ve Konuşmalar/Önizleme işaretleri. Gerçek sistemde iki stratejiyle birer ölçüm yapıp sonuçları rapora yazar.
4. frontend-coder: Değerlendirme ekranı, Önizleme sarı işaret ve Değerlendirme'den gelen soruyu sorma, Konuşmalar'daki "cevap burada" etiketi ve iki yeni durum çipi.
5. tester: aşağıdaki test senaryoları (iki servis + web).
6. qa: temiz kurulumda rehber 2026.10 + golden set yükleme, iki stratejiyle ölçüm, sıfırlayıp yeniden yükleme senaryosu, lab'la görsel karşılaştırma.

Atlanan: devops. Docker, ortam değişkeni ve model değişmiyor; strateji bugünkü `RETRIEVAL_STRATEGY` ile değişir. Bu yüzden qa, tester'a bağlıdır.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| 1a | Golden set gerçek veri; belge yüklü değilken ekran; kendi belgeler için set | `PUT/GET /api/assistant/golden-set` (`GoldenSetService`), başlatma kontrolleri (`EvalRunService.startEvalRun`), ekran (`EvaluationPage`) | B19-B21, B20b, B8, B10, B12, W1, W11 |
| 1b | Rehber soruları TASK-004 rehberine göre; kategori ve cevabın geçtiği yer işaretli; kim, nasıl, kaç soru, sürüm | Lead'in yazdığı `docs/rehber/golden/rehber-golden-v1.json` (biçim aşağıda), `ReplaceGoldenSetRequest` doğrulaması | B21; qa: 2026.10 yüklüyken `unmatchedCount` 0 |
| 2 | Doğru parça neyle işaretlenir (karar: sayfa + cümle) | `answerText` + retrieval-api `findChunkMatches` | R1-R7, B12 |
| 3 | recall@5, MRR; ≥ 0,95 yeşil; deterministik test | `RetrievalMetrics.evaluate`, `AnswerChunkRanker.rank`; web `EvalKpis` | B1-B6, W5 |
| 4a | Etkin yapılandırmayla ölçüm; düğme metni backend'den | `ContextRetriever.retrieveCandidatesAndContext`; web düğmesi `useLiveConfigQuery` | B15, W3 |
| 4b | Sadece LLM modunda düğme kapalı + uyarı | `EvalRunService` 409 `EVAL_NOT_AVAILABLE_IN_LLM_ONLY`; web uyarısı | B7, W2 |
| 4c | Reranker kapalıyken sonuç bunu kaydeder | `rerankFallbackCount`, `eval_run_questions.is_rerank_fallback` | B13, W6, W13 |
| 5a | 202 + polling | `EvalRunController.startEvalRun` (`Location`), `useEvalOverviewQuery` | B34, W9 |
| 5b | Sürerken düğme kapalı; ikinci çalıştırma 409 | Kısmi unique index `ux_eval_runs_single_running`, 409 `EVAL_RUN_IN_PROGRESS` | B9, B18, W9, W11 |
| 5c | Sayfa yenilense de süren çalıştırma bulunur | `EvalOverviewResponse.runningRun` | B35, W10 |
| 5d | Kayıt: golden set sürümü, strateji, rehber sürümü, soru sayısı, soru başına vector sırası ve prompt'taki yer, metrikler, süre | `eval_runs`, `eval_run_questions` | B16, B18 |
| 6 | Sadece retrieval ölçülür, dil modeli yok | `EvalRunRunner` (sadece `ContextRetriever`) | B15 (dil modeli mock'una etkileşim yok) |
| 7a | Kutular: recall@5 (x/n), MRR, Kaçan, Retrieval | `EvalKpis` | W5, W6 |
| 7b | Kaçanlar tablosu; "20+"; "N. sıra"/"yok"; en fazla 24 | `EvalRunQuestionRepository.findMissedAndMoved`, `MissedQuestionTable` | B35, W7 |
| 7c | Satıra tıklayınca soru Önizleme'de sorulur | `MissedQuestionTable` → `navigate('/onizleme', { state })`, `PreviewPage` | W8 |
| 7d | Hiç ölçüm yoksa "Henüz ölçüm yok. …" | `EvaluationPage` | W4 |
| 7e | Önceki çalıştırmalar saklanır; ekranda lab gibi sayfalı | `GET /api/assistant/evals`, `EvalRunTable` | B34, W13 |
| 8 | Önizleme sarı ve Konuşmalar "cevap burada"; tam metin; KAÇIRDI / ÜRETİM HATASI | `GoldenAnswerLocator`, `TurnStatusResolver`, `ConversationReadService`, `AnswerService`, web `ContextChunksCard`, `TurnChunkTable`, `turnStatus.ts` | B22-B33, W14, W15 |
| S1 | Örnek: 2026.10 + rerank ölçümü gerçek değerler | Gerçek sistem | backend-coder raporu + qa |
| S2 | Örnek: `top5` ile tekrar; iki çalıştırma karşılaştırılır | Gerçek sistem, "Çalıştırmalar" | qa |
| S3 | Örnek: sil + yeniden yükle → işaret geçerli | `answerText` eşleşmesi kimliğe bağlı değil | R1, qa (verileri sıfırla → rehber + set yükle → aynı recall) |
| K1 | Kural 2 kararı verilmiş | Onay kartı | — |
| K2 | Golden set hazır, sürümlü, kullanıcıya gösterilmiş | Lead | qa |
| K3 | İki stratejiyle gerçek ölçüm raporda | backend-coder, qa | qa raporu |
| K4 | Metrik hesabı bağımsız türetilmiş değerlerle test; build'ler geçer | tester | B1-B6, `./mvnw verify` ×2, `npm run build/lint/test` |
| K5 | Görünüm lab'ın sol yarısıyla aynı | frontend-coder | qa görsel karşılaştırma, W16 (axe) |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | İki sekmeden aynı anda "Ölçümü çalıştır" | Biri 202, diğeri 409 `EVAL_RUN_IN_PROGRESS`; tabloda tek `RUNNING` | B18 |
| G2 | Ölçüm sürerken golden set yeniden yüklenir | Süren ölçüm, başlarken kopyaladığı sorularla biter; yeni set sonraki ölçümde geçerli | B20 |
| G3 | Konuşmalar açılırken retrieval-api kapalı | Sayfa 200 ile açılır; işaret yok, "bulamadım" turları BİLMİYOR | B30 |
| G4 | Cevap cümlesi örtüşme yüzünden iki parçada | İkisi de doğru sayılır; Vector # ve prompt yeri, ilk geçen parçanınkidir | B5, R3 |
| G5 | Önizleme'de golden sorusu farklı harf/noktalama ile yazılır ("KARTIMI kaybettim ne yapmalıyım") | Eşleşir; tek kelime farkı eşleşmez | B26, B27 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| K1 | Cümlenin hangi parçalarda geçtiğini retrieval-api bulur (`findChunkMatches`); assistant-api metin eşleştirmez | Normalizasyon tek yerde durur ve parça metni assistant-api'ye taşınmaz. Konuşmalar'da eski turların parça metni zaten assistant'ta yok | assistant-api'nin arama sonucundaki `content` ile kendisinin eşleştirmesi: iki serviste iki normalizasyon kopyası olurdu ve Konuşmalar için parça metni yine gerekirdi |
| K2 | Golden set assistant veritabanında saklanır (`golden_sets`, `golden_questions`); tek set, PUT bütünüyle değiştirir | Kart 2-A; soru önerileriyle (TASK-012) aynı yöntem; Konuşmalar'da hızlı arama için `normalized_text` index'i | Dosyayı çalışırken okumak: "kendiliğinden yüklenmez" kuralına ve 2-A'ya aykırı |
| K3 | Başlatmada sorular ve doğru parça kimlikleri `eval_run_questions`'a kopyalanır | Ölçüm, golden set değişse de kendi sorularıyla biter (G2). Kaçanlar tablosu eski çalıştırmada da okunabilir | Ölçümün golden set tablosunu canlı okuması: yeniden yükleme yarım ölçümü bozardı |
| K4 | Tek `RUNNING` kuralı kısmi unique index'le sağlanır, uygulamada önce okunur | Yarış durumunda da tek kayıt (G1) | Sadece bellekte kilit: iki örnek ya da yeniden başlatmada tutmaz |
| K5 | Ekranın okuması tek uçtan yapılır (`getEvalOverview`); "Çalıştırmalar" ayrı sayfalı uçtan | Süren ölçüm, son biten ve son başarılı ölçüm tek istekle gelir; ölçüm sürerken sadece bu uç yoklanır | Liste + detay + golden set uçlarını ayrı ayrı yoklamak: üç ayrı yoklama ve "son başarılı ölçüm ilk sayfada mı" sorunu |
| K6 | Tur durumu ve `containsAnswer` her okumada hesaplanır. DB okuması `TransactionTemplate` (readOnly) içinde yapılır ve ihtiyaç duyulan her şey bu sırada yüklenir; `GoldenAnswerLocator` transaction kapandıktan sonra çağrılır | TASK-014 kararı (saklanmaz). `backend-conventions`: transaction açıkken dış çağrı yapılmaz | Tur kaydına `answer_chunk_ids` yazmak: golden set değişince eski turlar güncellenmezdi (kart soru 5) |
| K7 | SSE `ContextChunk`'a `containsAnswer` eklenir; Önizleme kartı bu alanı okur | Önizleme kartı zaten `sources` olayından çiziliyor. Hesap retrieval'dan sonra, akış açılmadan tek bir çağrıyla yapılır | Akış bitince web'in konuşma detayını ayrıca çekmesi: kartın veri kaynağı ikiye bölünürdü |
| K8 | Ölçümde iki stratejide de `candidates` (20) parça aranır; `TOP_K`'da prompt bunların ilk `topK`'sıdır | Kartın "Benim verdiğim kararlar" 6. maddesi; "Vector #" için 20'ye kadar sıra gerekir | Top-5'te 5 parça aramak: kaçan sorularda "Vector #" hep "20+" olurdu |
| K9 | Değerlendirme → Önizleme geçişi router `state`'iyle (`{ question }`) yapılır. `PreviewPage`, effect içinde `setTimeout(…, 0)` ile `askInNewConversation`'ı çağırır, temizlikte `clearTimeout` yapar, ardından `navigate(…, { replace: true, state: null })` ile state'i siler | StrictMode'da effect iki kez çalışınca soru yine bir kez gider. Sayfa yenilenince state tarayıcı geçmişinde kalır, silinmezse soru yeniden giderdi | Adres parametresi (`?soru=`): soru adres çubuğunda ve geçmişte kalır, yenilemede tekrar gider. Modül düzeyinde "bekleyen soru" değişkeni: test ve StrictMode için kırılgan |
| K10 | Ölçüm tek thread'li `evalExecutor`'da yürür; her soru sonucu kendi kısa transaction'ında yazılır | İlerleme sayacı anında görünür; dış çağrı transaction dışında kalır | Bütün ölçümü tek transaction'da yazmak: ilerleme görünmezdi, transaction dakikalarca açık kalırdı |
| K11 | Soru normalizasyonu mevcut `suggestion.domain.dto.SuggestionTextNormalizer.normalize` ile yapılır (evaluation paketi import eder) | Kart soru 5-A: "soru önerilerindeki kuralın aynısı"; tek kopya | Yeni bir normalizer: iki kural zamanla ayrışabilir |
| K12 | Çalıştırma ekranda `ev_<evalRunId>` diye görünür (lab `ev_7`); kimlik 1'den başlar | Konuşmalardaki `cnv_12` ile aynı biçim | — |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık notuna TASK-016 satırları eklendi. Yeni `Evaluation` tag'i eklendi.
- **Yeni (assistant-api):**
  - `GET /api/assistant/golden-set` (`getGoldenSet`, 404 `GOLDEN_SET_NOT_LOADED`)
  - `PUT /api/assistant/golden-set` (`replaceGoldenSet`, 400 `VALIDATION_FAILED`, 409 `GOLDEN_SET_VERSION_CONFLICT`)
  - `POST /api/assistant/evals` (`startEvalRun`; 202 + `Location`; 409 `EVAL_NOT_AVAILABLE_IN_LLM_ONLY` / `GOLDEN_SET_NOT_LOADED` / `EVAL_RUN_IN_PROGRESS` / `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE`; 503 `RETRIEVAL_UNAVAILABLE`)
  - `GET /api/assistant/evals` (`listEvalRuns`, sayfalı)
  - `GET /api/assistant/evals/overview` (`getEvalOverview`)
  - `GET /api/assistant/evals/{evalRunId}` (`getEvalRun`, 404 `EVAL_RUN_NOT_FOUND`)
- **Yeni şemalar (assistant-api):** `ReplaceGoldenSetRequest`, `GoldenQuestionRequest`, `GoldenSetSummaryResponse`, `EvalRunStatus`, `EvalRunErrorCode`, `EvalRunResponse`, `EvalRunResponsePage`, `EvalQuestionResultResponse`, `EvalRunDetailResponse`, `EvalOverviewResponse`.
- **Yeni (retrieval-api):** `POST /api/workspaces/{workspaceId}/chunk-matches` (`findChunkMatches`).
- **Yeni şemalar (retrieval-api):** `FindChunkMatchesRequest`, `ChunkMatchesResponse`, `ChunkTextMatchResponse`, `MatchedChunkResponse`.
- **Değişen:**
  - `TurnStatus`'a `MISSED` ve `GENERATION_ERROR` eklendi; "answer chunks" tanımı ve 4. kural yazıldı.
  - `TurnChunkResponse`'a zorunlu `containsAnswer` (boolean) eklendi.

`docs/api/sse.md`:
- `ContextChunk`'a `containsAnswer` (boolean) eklendi, örnek güncellendi, durum notu eklendi.

## Golden set dosyası (lead yazar)
- **Yol:** `docs/rehber/golden/rehber-golden-v1.json`, UTF-8. İçeriği `PUT /api/assistant/golden-set` isteğinin gövdesidir (`ReplaceGoldenSetRequest`); başka alan yazılmaz.
- **Biçim:**
  ```json
  {
    "version": "rehber-golden-v1",
    "questions": [
      {
        "category": "Kartlar",
        "text": "Kartımı kaybettim, ne yapmalıyım?",
        "pageNumber": 19,
        "answerText": "Kartınızı kaybettiyseniz ya da kartınız çalındıysa vakit kaybetmeden bildirin."
      }
    ]
  }
  ```
- **Alan kuralları** (sunucu da doğrular):
  - `version`: en fazla 40 karakter.
  - `questions`: 1-500 soru.
  - `category`: en fazla 40 karakter.
  - `text`: en fazla 200 karakter; büyük/küçük harf ve noktalama farkı yok sayıldığında da tekil olmalı.
  - `pageNumber`: 1 ve üzeri; PDF'teki gerçek sayfa.
  - `answerText`: kırpıldıktan sonra 20-300 karakter.
- **Soru sayısı ve dağılım:** 100 soru, rehber 2026.10'un bölümlerine sayfa sayısına göre dağıtılır. Kategori bölümün adıdır, aynen şöyle yazılır:

  | Kategori | Soru |
  |----------|------|
  | Bu rehber hakkında | 3 |
  | Kartlar | 20 |
  | Hesaplar | 13 |
  | Krediler | 14 |
  | Para transferi ve ödemeler | 11 |
  | Dijital bankacılık | 10 |
  | Yatırım | 8 |
  | Şube, ATM ve müşteri hizmetleri | 7 |
  | Şikâyet, itiraz ve kişisel veriler | 4 |
  | Ücret tarifesi | 6 |
  | Terimler sözlüğü | 4 |
- **`answerText` nasıl seçilir:**
  - Rehber 2026.10 sisteme yüklendikten sonra Bilgi kaynakları → parça penceresinde görünen metinden birebir kopyalanır. Bu metin PDF'ten çıkarılmış hâlidir, eşleşme bunun üzerinden yapılır.
  - Cevabı taşıyan tek bir cümle ya da tablo satırı seçilir.
  - Çok kısa ve başka yerlerde de geçen ifadeler seçilmez ("1.450 TL", "3 iş günü").
  - Satır sonu boşluk sayılır. Büyük/küçük harf farkı sorun olmaz, ama noktalama ve rakamlar aynen kopyalanır.
- **Soru metni:** Müşterinin soracağı gibi yazılır; `answerText`'in kelimelerini aynen tekrar etmez.
- **Kapsam:** Rehberde olmayan konular (TASK-004 notu) golden set'e girmez.
- **Kontrol:**
  1. Lead, sürüm adı `rehber-golden-v1-taslak` olan bir kopyayla çalışır. Rehber 2026.10 ve taslak yüklenir, bir ölçüm çalıştırılır. `unmatchedCount` 0 olmalı; değilse ilgili `answerText`'ler düzeltilir. Aynı adla farklı içerik 409 `GOLDEN_SET_VERSION_CONFLICT` aldığı için her denemede taslak adına bir sıra numarası eklenir (`-taslak2` …).
  2. `unmatchedCount` 0 olunca dosya `version: "rehber-golden-v1"` ile kaydedilir ve yüklenir.
  3. İçerik bundan sonra değişirse yeni ad `rehber-golden-v2` olur.
  4. Lead tam listeyi kullanıcıya sunar ve onayını rapora yazar.
- **Yükleme komutu:** `curl -s -X PUT localhost:8090/api/assistant/golden-set -H 'Content-Type: application/json' --data-binary @docs/rehber/golden/rehber-golden-v1.json`

## Backend planı
Coder'ın yükleyeceği ek skill'ler: `rag-evaluation`, `rag-pipeline`, `spring-transactions`, `java-concurrency`, `flyway-migrations`, `postgres-patterns`, `problem-details-rfc9457`.

### retrieval-api
- `chunk/controller/ChunkController.findChunkMatches(long workspaceId, @Valid FindChunkMatchesRequest request)` → `POST /api/workspaces/{workspaceId}/chunk-matches`, 200 `ChunkMatchesResponse`.
- `chunk/service/ChunkService.findChunkMatches(long workspaceId, List<String> texts)`:
  - Çalışma alanı yoksa `NotFoundException("WORKSPACE_NOT_FOUND", …)`.
  - Her metin mevcut `normalizeQuery` ile normalize edilir.
  - Tek bir native sorgu çalışır, sonuç istek sırasıyla `ChunkMatchesResponse`'a çevrilir. Yanıttaki `text` gönderilen metindir.
- `chunk/repository/ChunkRepository.findChunksContainingTexts(long workspaceId, String[] normalizedTexts)`:
  - Native sorgu: `unnest(CAST(:texts AS text[])) WITH ORDINALITY`.
  - `chunks` ⨝ `sources` (sadece `chunks.source_version_id = sources.active_version_id` ve `sources.workspace_id = :workspaceId`) ⨝ `source_versions`.
  - Koşul: `strpos(regexp_replace(lower(translate(c.content, 'Iİ', 'ıi')), '\s+', ' ', 'g'), t.text) > 0`.
  - Sıra: `ORDER BY ordinality, c.id`.
  - Satır projeksiyonu: `ordinality, chunkId, sourceId, sourceName, version, pageNumber`. `JdbcClient` ya da interface projeksiyonu kullanılabilir; coder seçer.
- DTO'lar (`chunk/domain/dto`):
  - `FindChunkMatchesRequest(@NotNull @Size(min = 1, max = 500) List<@NotBlank @Size(max = 1000) String> texts)`
  - `ChunkMatchesResponse(List<ChunkTextMatchResponse> matches)`
  - `ChunkTextMatchResponse(String text, List<MatchedChunkResponse> chunks)`
  - `MatchedChunkResponse(long chunkId, long sourceId, String sourceName, String version, int pageNumber)`
- Doğrulama mesajları sözleşmedeki gibidir. Eleman ihlalinde `violations[].field` = `texts[i]` olur; Hibernate Validator'ın `texts[i].<list element>` yolu bu biçime çevrilir.
- Log: `Chunk matches found texts={} matchedTexts={} durationMs={}`. Metinler loglanmaz.
- `retrieval-api/CLAUDE.md` "Durum" ve "Okuma uçları" satırlarına ekleme yapılır.

### assistant-api
**Migration** `V7__create_golden_set_and_eval_runs.sql`:
- **`golden_sets`:**
  - `id bigint identity PK`
  - `version text NOT NULL CHECK (char_length BETWEEN 1 AND 40)`
  - `content_hash text NOT NULL`
  - `loaded_at timestamptz NOT NULL`
- **`golden_questions`:**
  - `id bigint identity PK`
  - `golden_set_id bigint NOT NULL REFERENCES golden_sets ON DELETE CASCADE`
  - `position int NOT NULL CHECK (≥ 1)`
  - `category text` (1-40)
  - `text text` (1-200)
  - `normalized_text text NOT NULL`
  - `page_number int CHECK (≥ 1)`
  - `answer_text text` (20-300)
  - `UNIQUE (golden_set_id, position)`, `UNIQUE (golden_set_id, normalized_text)`
  - `ix_golden_questions_normalized_text (normalized_text)`
- **`eval_runs`:**
  - `id bigint identity PK`
  - `status text CHECK IN ('RUNNING','DONE','FAILED')`
  - `golden_set_version text NOT NULL`
  - `retrieval_strategy text CHECK IN ('TOP_K','RERANK')`
  - `candidates int`, `top_k int`
  - `source_versions text NOT NULL`
  - `question_count int ≥ 1`, `measured_count int ≥ 1`, `processed_count int NOT NULL DEFAULT 0 ≥ 0`
  - `hit_count int NULL`, `recall_at_k double precision NULL`, `mrr double precision NULL`
  - `rerank_fallback_count int NOT NULL DEFAULT 0`
  - `error_code text NULL CHECK IN ('RETRIEVAL_UNAVAILABLE','EVAL_RUN_INTERRUPTED','INTERNAL_ERROR')`
  - `started_at timestamptz NOT NULL`, `finished_at timestamptz NULL`, `duration_ms bigint NULL`
  - CHECK'ler:
    - `ck_eval_runs_done_has_metrics`: `status = 'DONE'` ⇔ `hit_count`, `recall_at_k`, `mrr` dolu
    - `ck_eval_runs_failed_has_error`: `status = 'FAILED'` ⇔ `error_code` dolu
    - `ck_eval_runs_finished`: `status = 'RUNNING'` ⇔ `finished_at IS NULL`
  - `CREATE UNIQUE INDEX ux_eval_runs_single_running ON eval_runs (status) WHERE status = 'RUNNING'`
  - `ix_eval_runs_started_at_id ON eval_runs (started_at DESC, id DESC)`
- **`eval_run_questions`:**
  - `eval_run_id bigint REFERENCES eval_runs ON DELETE CASCADE`
  - `position int`, `category text`, `text text`, `page_number int`, `answer_text text`
  - `answer_chunk_ids bigint[] NOT NULL`
  - `is_measured boolean NOT NULL` (`answer_chunk_ids` boş değil)
  - `is_processed boolean NOT NULL DEFAULT false`
  - `vector_rank int NULL`, `prompt_position int NULL`
  - `is_rerank_fallback boolean NOT NULL DEFAULT false`
  - `PRIMARY KEY (eval_run_id, position)`

**Paket `evaluation/`** (`backend-conventions` bölüm 3; her pakette `package-info.java`):
- **`controller/GoldenSetController`:**
  - `getGoldenSet()` → `GET /api/assistant/golden-set`
  - `replaceGoldenSet(@Valid ReplaceGoldenSetRequest)` → `PUT`, 200
- **`controller/EvalRunController`:**
  - `startEvalRun()` → `POST /api/assistant/evals`: 202, `Location: /api/assistant/evals/{id}`, gövde `EvalRunResponse`
  - `listEvalRuns(page, size)` → `GET`; `PageParameters` + `@Min`/`@Max`, `PageResponse`
  - `getEvalOverview()` → `GET /overview`
  - `getEvalRun(long evalRunId)` → `GET /{evalRunId}`
- **`service/GoldenSetService`:**
  - `getGoldenSet()` → `GoldenSetSummaryResponse`; yoksa `NotFoundException("GOLDEN_SET_NOT_LOADED")`.
  - `replaceGoldenSet(ReplaceGoldenSetRequest)`, tek transaction:
    1. `GoldenSetRepository.lockForReplace()` (`LOCK TABLE golden_sets IN EXCLUSIVE MODE`).
    2. Mevcut set aynı sürümde ve `content_hash` farklıysa `ConflictException("GOLDEN_SET_VERSION_CONFLICT")`.
    3. `deleteAllInBatch` (cascade).
    4. Yeni set ve sorular sırayla eklenir. Metinler `strip()`, `normalized_text` = `SuggestionTextNormalizer.normalize(text)`.
  - `contentHash`: SHA-256 hex. Girdi, kırpılmış `category`, `text`, `pageNumber`, `answerText` alanlarının sırayla `\u001F` ile ve sorular arası `\u001E` ile birleştirilmesidir.
- **`domain/validation/ValidGoldenSet` + `GoldenSetValidator`** (sınıf düzeyi): `answerText`'in kırpılmış uzunluğu 20-300 ve normalize `text` tekilliği. İhlaller `questions[i].answerText` / `questions[i].text` yoluyla `VALIDATION_FAILED`'a düşer; `SuggestionSetValidator`'daki kalıp kullanılır.
- **`service/EvalRunService`:**
  - **`startEvalRun()`:** kontrol sırası sözleşmedeki gibidir.
    1. `AssistantProperties.generation().configuredMode() == LLM_ONLY` → 409.
    2. Set yok → 409 `GOLDEN_SET_NOT_LOADED`.
    3. `EvalRunRepository.existsByStatus(RUNNING)` → 409 `EVAL_RUN_IN_PROGRESS`.
    4. Transaction dışında `RetrievalClient.findChunkMatches(answerTexts)`; hata yukarı fırlar (503).
    5. Ölçülebilir soru yoksa 409 `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE`.
    6. `EvalRunLifecycleService.createRun(...)`. Unique index ihlali (`DataIntegrityViolationException`) → 409 `EVAL_RUN_IN_PROGRESS`.
    7. `evalExecutor.execute(() -> evalRunRunner.runEvalRun(id))`.
    8. Dönen `EvalRunResponse`.
  - `sourceVersions`: ölçülen soruların eşleşen parçalarındaki `version` etiketleri; tekil, artan sıralı, `", "` ile birleşik.
  - **Okuma metotları:** `listEvalRuns(page, size)` → `Page<EvalRunResponse>`, `getEvalRun(id)` → `EvalRunDetailResponse` (yoksa 404 `EVAL_RUN_NOT_FOUND`), `getEvalOverview()` → `EvalOverviewResponse`.
  - `missedAndMovedQuestions`: sadece `DONE`'da; `MISSED_AND_MOVED_LIMIT = 24`.
- **`service/EvalRunRunner.runEvalRun(long evalRunId)`:**
  - Ölçülen soruları `position` sırasıyla okur.
  - Her soru için `ContextRetriever.retrieveCandidatesAndContext(text)` → `AnswerChunkRanker.rank(context, answerChunkIds)` → `EvalRunLifecycleService.recordQuestionResult(evalRunId, position, rank, isRerankFallback)`. Bu çağrı soru satırını yazar ve `processed_count` + 1, `rerank_fallback_count` + (fallback ? 1 : 0) yapar; tek transaction.
  - Bitince `RetrievalMetrics.evaluate(promptPositions)` → `completeRun` (DONE, metrikler, `finished_at` = `Instant.now(clock)`, `duration_ms`).
  - `ServiceUnavailableException` (`RETRIEVAL_UNAVAILABLE`) → `failRun(id, "RETRIEVAL_UNAVAILABLE")`. Diğer `RuntimeException` → `failRun(id, "INTERNAL_ERROR")` + `log.error`.
- **`service/EvalRunLifecycleService`:** her metot kendi transaction'ında çalışır: `createRun`, `recordQuestionResult`, `completeRun`, `failRun`, `failInterruptedRuns`. Sonuncusu `RUNNING` → `FAILED` + `EVAL_RUN_INTERRUPTED` yapar ve sayıyı döner.
- **`service/EvalRunStartupListener`:** `ApplicationReadyEvent` → `failInterruptedRuns()`. Log: `Interrupted eval runs failed count={}`.
- **`service/AnswerChunkRanker`** (durumsuz `@Component`): `QuestionRank rank(RetrievedContext context, Set<Long> answerChunkIds)`.
  - `vectorRank`: `candidateChunks` içinde kimliği kümede olan ilk parçanın `vectorRank`'ı; yoksa `null`.
  - `promptPosition`: `contextChunks` içinde kümede olan ilk parçanın 1 tabanlı sırası; yoksa `null`.
  - `QuestionRank` record: `@Nullable Integer vectorRank, @Nullable Integer promptPosition`.
- **`service/RetrievalMetrics`:** saf statik `evaluate(List<@Nullable Integer> promptPositions)` → `RetrievalMetricsResult(int hitCount, int missedCount, double recallAtK, double mrr)`. Boş liste `IllegalArgumentException` fırlatır; başlatmada en az 1 soru zaten garanti.
- **`service/GoldenAnswerLocator`:**
  - `Map<String, Set<Long>> locateAnswerChunks(Collection<String> questions)`. Anahtar normalize sorudur; değer, cevap parçası kimlikleridir (boş küme yazılmaz).
  - Akış: sorular normalize edilir → `GoldenQuestionRepository.findByNormalizedTextIn` (kendi readOnly transaction'ı) → eşleşme yoksa retrieval-api çağrılmadan boş map → eşleşenlerin `answerText`'leri tek `findChunkMatches` çağrısıyla.
  - `ServiceUnavailableException` boş map döner ve uyarı loglar: `Golden answer lookup skipped reason={}`.
  - Çağıranın transaction'ı olmamalıdır.
- **Repository'ler:**
  - `GoldenSetRepository` (`lockForReplace`, `findFirstByOrderByIdDesc`)
  - `GoldenQuestionRepository` (`findByGoldenSetIdOrderByPosition`, `findByNormalizedTextIn`; ikincisi tek set olduğu için set kimliği istemez)
  - `EvalRunRepository`:
    - `existsByStatus`
    - `findFirstByStatus(RUNNING)`
    - en son `DONE` ya da `FAILED` ve en son `DONE` (sıra `started_at DESC, id DESC`)
    - `findAllByOrderByStartedAtDescIdDesc(Pageable)`
  - `EvalRunQuestionRepository`:
    - `findMeasuredByEvalRunIdOrderByPosition`
    - `findMissedAndMoved(evalRunId, Pageable)`: `is_measured AND (prompt_position IS NULL OR prompt_position IS DISTINCT FROM vector_rank)`, `ORDER BY position`
- **Entity'ler:** `GoldenSet`, `GoldenQuestion`, `EvalRun`, `EvalRunQuestion`, `EvalRunQuestionId`, `EvalRunStatus` (enum). `EvalRun` üzerinde domain metotları: `recordProgress(boolean rerankFallback)`, `complete(RetrievalMetricsResult, Instant)`, `fail(String errorCode, Instant)`.
- **DTO'lar** (sözleşmedeki adlarla): `ReplaceGoldenSetRequest`, `GoldenQuestionRequest`, `GoldenSetSummaryResponse`, `EvalRunResponse` (`from(EvalRun)`; `unmatchedCount` = `question_count − measured_count`, `missedCount` = `measured − hit`, DONE değilse `null`), `EvalQuestionResultResponse`, `EvalRunDetailResponse` (alanlar düz yazılır: `EvalRunResponse` alanları + `missedAndMovedQuestions`), `EvalOverviewResponse`.
- **`config/EvalExecutorConfig`:** bean `evalExecutor`, `ThreadPoolTaskExecutor` core = max = 1, kuyruk 1, thread adı öneki `eval-`.

**Değişen mevcut sınıflar:**
- **`retrieval/service/ContextRetriever.retrieveCandidatesAndContext(String query)`:** her zaman `candidates` parça arar. `TOP_K` → `contextChunks` = ilk `topK`, strateji `TOP_K`. `RERANK` → mevcut `rerank(...)` (yedek yol dahil; `RerankerStatusTracker` aynı biçimde bilgilendirilir). `retrieveContext` davranışı değişmez; ortak kısım özel metoda alınır.
- **`retrieval/client/RetrievalClient.findChunkMatches(List<String> texts)`** → `ChunkMatchesResponse`. Hata davranışı mevcut `call(...)` ile aynıdır (`RETRIEVAL_UNAVAILABLE`). İstemci record'ları `retrieval/client/FindChunkMatchesRequest`, `ChunkMatchesResponse`, `ChunkTextMatch`, `MatchedChunk`.
- **`conversation/domain/entity/TurnStatus`:** `MISSED`, `GENERATION_ERROR` eklenir (sözleşme sırası).
- **`conversation/service/TurnStatusResolver.resolve(Turn turn, Set<Long> answerChunkIds)`:** sözleşmedeki kural sırası uygulanır. "Prompt'a giden" = `prompt_position` dolu `TurnChunk`'lar. Boş küme = cevap parçası yok.
- **`conversation/domain/dto/TurnResponse.from(Turn turn, TurnStatus status, Set<Long> answerChunkIds)`** ve `TurnChunkResponse.from(TurnChunk chunk, boolean containsAnswer)`.
- **`conversation/service/ConversationReadService`:** sınıf düzeyindeki `@Transactional(readOnly = true)` kalkar (K6).
  - `getConversationDetail`: turlar ve parçaları `TransactionTemplate` içinde yüklenir → `GoldenAnswerLocator.locateAnswerChunks(sorular)` → eşleme.
  - `listConversations`: sadece son turu `notFoundAnswer` olan, `RAG`, hatasız konuşmaların sorularıyla tek locator çağrısı. Son turun prompt parça kimlikleri okuma transaction'ında yüklenir.
- **`conversation/domain/event/ContextChunk`:** `boolean containsAnswer` son alan olarak eklenir.
- **`conversation/service/AnswerService`:** `RAG` ve `contextChunks` boş değilse, retrieval'dan sonra ve akış açılmadan `goldenAnswerLocator.locateAnswerChunks(List.of(strippedQuestion))` çağrılır. Sonuç küme olarak `AnswerStreamer`'a geçer, `sourcesEvent` her parçaya `containsAnswer` yazar. `LLM_ONLY` ya da boş bağlamda çağrı yapılmaz.

**Hata kodları** (yeni; hepsi kategori sınıflarıyla, yeni handler metodu yok):
- `GOLDEN_SET_NOT_LOADED` (404 GET / 409 POST evals)
- `GOLDEN_SET_VERSION_CONFLICT` (409)
- `EVAL_NOT_AVAILABLE_IN_LLM_ONLY` (409)
- `EVAL_RUN_IN_PROGRESS` (409)
- `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE` (409)
- `EVAL_RUN_NOT_FOUND` (404)
- Çalıştırma kaydında: `EVAL_RUN_INTERRUPTED`.

**Log** (soru, cevap cümlesi, parça metni loglanmaz):
- `Golden set replaced version={} questionCount={}`
- `Eval run started evalRunId={} goldenSetVersion={} strategy={} questionCount={} measuredCount={}`
- `Eval run finished evalRunId={} status={} recallAtK={} mrr={} rerankFallbackCount={} durationMs={}`
- `Eval run failed evalRunId={} errorCode={} processedCount={}`

**Ayarlar:** Yeni property yok. Duman testleri `assistant-api/CLAUDE.md`'ye eklenir (golden set yükleme, ölçüm başlatma, overview), "Durum" ve "Paket yapısı" satırları güncellenir.

## Frontend planı
Ek skill'ler: `rag-evaluation`, `react-state-management`, `frontend-a11y`, `frontend-design` (lab token'ları).

**`features/evaluation/`:**
- **`model/types.ts`:** `GoldenSetSummaryResponse`, `EVAL_RUN_STATUSES` / `EvalRunStatus`, `EvalRunErrorCode`, `EvalRunResponse`, `EvalQuestionResultResponse`, `EvalRunDetailResponse`, `EvalOverviewResponse` (sözleşmeyle birebir).
- **`model/evaluationRules.ts`:**
  - `EVAL_POLL_INTERVAL_MS = 1000`
  - `RECALL_PASS_THRESHOLD = 0.95` (ham değerle karşılaştırılır, yuvarlanmışla değil)
  - `EVAL_RUNS_PAGE_SIZE = 20`
  - `KPI_FRACTION_DIGITS = 2`
  - `RUN_TABLE_FRACTION_DIGITS = 3`
- **`model/evaluationLabels.ts`** (saf fonksiyonlar):
  - `evalRunLabel(evalRunId)` → `ev_12`
  - `strategyText(strategy, candidates, topK)` → `rerank 20→5` / `top-5`
  - `retrievalText(run)` → strateji + `rerankFallbackCount > 0` ise ` · reranker DOWN (n)`
  - `vectorRankText(vectorRank, candidates)` → `9` / `20+`
  - `promptPositionText(promptPosition)` → `2. sıra` / `yok`
  - `runButtonText(liveConfig, runningRun)` → `Ölçümü çalıştır · rerank 20→5`, ölçüm sürerken `Ölçülüyor · 37/100` (`processedCount/measuredCount`)
  - `recallSubtitle(run)` → `95/100 soru`, `unmatchedCount > 0` ise `95/99 soru · 1 soru ölçülemedi`
  - `retrievalSubtitle(run)` → `rehber 2026.10`, fallback varsa `rehber 2026.10 · reranker DOWN: 12 soru`
  - `runFailureText(errorCode)`
- **`model/evaluationTexts.ts`** (lab metinleri birebir):
  - Başlık açıklaması: `Rehberde cevabı olan ${n} soru; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar.` Set yokken bugünkü metin ("Rehberde cevabı olan sorular; …").
  - `Golden set yüklenmemiş.`
  - `Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al.`
  - `Henüz ölçüm yok. Mevcut retrieval ayarıyla çalıştır; sonra ayarı değiştirip tekrar çalıştırarak iki satırı karşılaştır.`
  - Kutu adları ve alt yazıları: `recall@5`, `MRR` / `doğru parçanın sırası`, `Kaçan` / `doğru parça prompt'ta yok`, `Retrieval`.
  - Bölüm başlıkları: `Çalıştırmalar`, `Kaçanlar ve sırası değişenler` + sağda `son çalıştırma`.
  - Kaçanlar boşken: `Bütün sorularda doğru parça vector sırasıyla aynı yerde.`
  - Tablo hücreleri: süren satırda `ölçülüyor…`, başarısız satırda `başarısız`.
  - Başarısızlık notu: `Son ölçüm tamamlanamadı: ` + `bilgi bankasına ulaşılamadı.` (RETRIEVAL_UNAVAILABLE) / `servis ölçüm sırasında yeniden başladı.` (EVAL_RUN_INTERRUPTED) / `beklenmeyen bir hata oluştu.` (INTERNAL_ERROR).
- **`api/evaluationApi.ts`:** `getEvalOverview()`, `startEvalRun()` (POST, gövdesiz), `listEvalRuns({ page, size })`.
- **`api/evaluationQueries.ts`:**
  - `evaluationKeys = { all, overview, runs(params) }`
  - `useEvalOverviewQuery()`: `refetchInterval` = `runningRun` doluysa `EVAL_POLL_INTERVAL_MS`, değilse `false`.
  - `useEvalRunsQuery(params, isRunning)`: aynı aralık kuralı.
  - `useStartEvalRunMutation()`: başarıda dönen çalıştırma `setQueryData(evaluationKeys.overview, …)` ile `runningRun`'a yazılır, ilk yoklama beklenmez; `runs` invalidate edilir.
  - `runningRun` doludan boşa dönünce `evaluationKeys.runs` invalidate edilir.
- **`components/EvalKpis.tsx`** (lab `.kpis` / `.kpi`): dört kutu.
  - recall@5: `formatDecimal(recallAtK, 2)`; ham değer ≥ 0,95 ise yeşil, değilse kırmızı.
  - MRR: 2 hane.
  - Kaçan: `missedCount`; > 0 ise kırmızı, 0 ise yeşil.
  - Retrieval: `strategyText` (16 px) + `retrievalSubtitle`.
  - 620 px altında 2 sütun.
- **`components/EvalRunTable.tsx`** (lab "Çalıştırmalar"):
  - Sütunlar: Çalıştırma, Retrieval, Rehber, recall@5, MRR, Saat.
  - recall ve MRR 3 hane; Saat = `formatListTime(startedAt)`.
  - `RUNNING` satırı: recall `ölçülüyor…`, MRR `–`. `FAILED` satırı: recall `başarısız`, MRR `–`.
  - Altında `shared/components/Pagination`.
- **`components/MissedQuestionTable.tsx`** (lab tablosu):
  - Sütunlar: Soru, Vector #, Prompt'taki yeri.
  - Prompt yeri: `StatusBadge tone="ok"` `N. sıra` / `tone="bad"` `yok`.
  - Satır tıklanabilir, `title="Önizlemede sor"`; satır içinde soruyu taşıyan bir düğme klavye yoludur (TurnChunkTable kalıbı).
  - Tıklayınca `navigate('/onizleme', { state: { question: text } })`.
- **`pages/EvaluationPage.tsx`:**
  - `PageHeader` `actions` = düğme. `disabled` koşulları: live-config yüklenmedi, `generationMode === 'LLM_ONLY'`, `goldenSet === null`, `runningRun !== null`, mutation sürüyor.
  - Sırasıyla gösterilenler:
    1. Sadece LLM uyarısı (`Callout tone="warning"`)
    2. Set yok bilgisi (`Callout`)
    3. Başlatma hatası (`ErrorAlert`, `messageFor(errorCode)`)
    4. Son biten ölçüm `FAILED` ise başarısızlık notu (`Callout tone="warning"`)
    5. `lastDoneRun` varsa kutular + Çalıştırmalar + Kaçanlar; yoksa "Henüz ölçüm yok." kutusu
  - Çalıştırmalar tablosu `listEvalRuns` boş değilse `lastDoneRun` olmasa da görünür.
  - Durum değişimleri (ölçüm başladı / bitti) `aria-live="polite"` ile duyurulur.

**`shared/api/errorMessages.ts`'e eklenecekler:**
- `GOLDEN_SET_NOT_LOADED`: `Golden set yüklenmemiş. Önce golden set'i yükle.`
- `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE`: `Golden set'in belgesi bilgi bankasında yok. Önce belgeyi yükle.`
- `EVAL_RUN_IN_PROGRESS`: `Bir ölçüm zaten sürüyor. Bitmesini bekle.` (409'da overview de yeniden çekilir)
- `EVAL_NOT_AVAILABLE_IN_LLM_ONLY`: `Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al.`
- `RETRIEVAL_UNAVAILABLE` zaten var.

**`features/preview/`:**
- `model/types.ts`: `ContextChunk.containsAnswer: boolean`. `answerStreamEvents.ts` alanı doğrular (boolean değilse olay geçersiz sayılır).
- `components/ContextChunksCard.tsx`:
  - `containsAnswer` parçaya lab'ın `.ctxl button.gold` görünümü verilir (kenar `#e8cf91`, zemin `#fffaf0`; `index.css`'e token olarak).
  - Alt satır: `✓ atıf yapılan`, en az bir parça `containsAnswer` ise ` · sarı: cevabın geçtiği parça` eklenir.
- `pages/PreviewPage.tsx`: `useLocation().state` `{ question: string }` ise K9'daki yolla `answerStream.askInNewConversation(question)` bir kez çağrılır. State tipi `PreviewNavigationState` `model/types.ts`'te tanımlanır ve `unknown`'dan daraltılır.

**`features/conversation/`:**
- `model/types.ts`: `TURN_STATUSES`'a `MISSED`, `GENERATION_ERROR`; `TurnChunkResponse.containsAnswer: boolean`.
- `model/turnStatus.ts`: `MISSED: 'KAÇIRDI'` (`bad`), `GENERATION_ERROR: 'ÜRETİM HATASI'` (`bad`).
- `components/TurnChunkTable.tsx`: Parça hücresinde düğmeden sonra `containsAnswer` ise `<StatusBadge tone="run">cevap burada</StatusBadge>`.

`web/CLAUDE.md` "Durum" ve klasör satırları güncellenir. Yeni paket yok.

## Test senaryoları
Beklenen değerler elle hesaplanmıştır; oranlar `isCloseTo(x, within(1e-6))` ile karşılaştırılır.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç |
|---|---------|--------|-----------------------------|----------------|
| B1 | Bilinen sıralarla metrik | unit (`RetrievalMetricsTest`) | givenPromptPositionsOneTwoOneFourAndNone_whenEvaluate_thenRecallIsPointEightAndMrrIsPointFiftyFive | hit 4, missed 1, recall 0.8, mrr 0.55 |
| B2 | Hepsi kaçtı | unit | givenNoPromptPositions_whenEvaluate_thenRecallAndMrrAreZero | `[null,null,null]` → hit 0, missed 3, 0.0, 0.0 |
| B3 | Tek soru 3. sırada | unit | givenSinglePromptPositionThree_whenEvaluate_thenMrrIsOneThird | recall 1.0, mrr 0.333333 |
| B4 | Rerank sırayı değiştirir | unit (`AnswerChunkRankerTest`) | givenAnswerChunkAtVectorSevenRerankedToSecond_whenRank_thenVectorSevenPromptTwo | vectorRank 7, promptPosition 2 |
| B5 | Cevap iki parçada (G4) | unit | givenAnswerInTwoChunks_whenRank_thenUsesFirstInEachOrder | A vector 9 / prompt 1, B vector 3 / prompt 4 → vectorRank 3, promptPosition 1 |
| B6 | Adaylarda yok | unit | givenAnswerChunkNotInCandidates_whenRank_thenBothNull | `null`, `null` |
| B7 | Sadece LLM | unit (`EvalRunServiceTest`) | givenLlmOnlyMode_whenStartEvalRun_thenThrowsEvalNotAvailableInLlmOnly | `ConflictException` + `EVAL_NOT_AVAILABLE_IN_LLM_ONLY`; `RetrievalClient` etkileşimsiz |
| B8 | Set yok | unit | givenNoGoldenSet_whenStartEvalRun_thenThrowsGoldenSetNotLoaded | 409 `GOLDEN_SET_NOT_LOADED` |
| B9 | Süren ölçüm var | unit | givenRunningRun_whenStartEvalRun_thenThrowsEvalRunInProgress | 409; `findChunkMatches` çağrılmaz |
| B10 | Hiç cümle bulunamadı | unit | givenNoAnswerTextFound_whenStartEvalRun_thenThrowsGoldenSetNotInKnowledgeBase | 409; kayıt oluşmaz |
| B11 | retrieval-api kapalı | unit | givenChunkMatchFails_whenStartEvalRun_thenThrowsRetrievalUnavailable | `ServiceUnavailableException` + `RETRIEVAL_UNAVAILABLE`; kayıt oluşmaz |
| B12 | Kısmi eşleşme | unit | givenThreeOfFourAnswerTextsFound_whenStartEvalRun_thenMeasuresThreeAndOneUnmatched | `questionCount` 4, `measuredCount` 3, `unmatchedCount` 1, `sourceVersions` "2026.10" |
| B13 | Reranker iki soruda düştü | unit (`EvalRunRunnerTest`) | givenRerankFallbackOnTwoQuestions_whenRunEvalRun_thenCountsTwoFallbacks | `rerankFallbackCount` 2, durum `DONE` |
| B14 | İkinci soruda arama hatası | unit | givenSearchFailsOnSecondQuestion_whenRunEvalRun_thenRunFailsWithRetrievalUnavailable | `FAILED`, `RETRIEVAL_UNAVAILABLE`, metrikler `null`, `processedCount` 1 |
| B15 | Top-5'te 20 aday, dil modeli yok | unit (`ContextRetrieverTest` + runner) | givenTopKStrategy_whenRetrieveCandidatesAndContext_thenSearchesTwentyAndKeepsFirstFive | `search(q, 20)`; prompt = vector 1-5; rerank ve `ChatModel` etkileşimsiz |
| B16 | Bitmiş kayıt | integration (Testcontainers) | givenFinishedRun_whenGetEvalRun_thenReturnsMetricsVersionsAndDuration | `status` DONE, `goldenSetVersion`, `sourceVersions`, `durationMs` = bitiş − başlangıç (sabit saat) |
| B17 | Açılışta yarım ölçüm | integration | givenRunningRunAtStartup_whenApplicationReady_thenRunFailsInterrupted | `FAILED`, `EVAL_RUN_INTERRUPTED`, `finishedAt` dolu |
| B18 | Eşzamanlı başlatma (G1) | integration | givenTwoConcurrentStarts_whenStartEvalRun_thenOneAcceptedOneConflict | bir 202, bir 409 `EVAL_RUN_IN_PROGRESS`; tabloda tek `RUNNING` |
| B19 | Aynı sürüm, farklı içerik | integration (`GoldenSetServiceIntegrationTest`) | givenSameVersionDifferentContent_whenReplaceGoldenSet_thenConflictAndStoredSetUnchanged | 409 `GOLDEN_SET_VERSION_CONFLICT`; eski sorular duruyor |
| B20 | Ölçüm sürerken yükleme (G2) | integration | givenRunningRun_whenReplaceGoldenSet_thenRunKeepsCopiedQuestions | 200; çalıştırmanın soru satırları değişmez |
| B20b | Aynı sürüm, aynı içerik (baştaki/sondaki boşluk farkıyla) | integration | givenSameVersionSameContent_whenReplaceGoldenSet_thenAcceptedAndLoadedAtRenewed | 200; soru sayısı aynı, `loadedAt` yeni sabit saat |
| B21 | Doğrulama | controller (`GoldenSetControllerTest`) | givenInvalidGoldenSet_whenReplaceGoldenSet_thenReportsAllViolations | 400; `questions[1].text` "must be unique" ("Kartımı kaybettim?" ile "kartımı KAYBETTİM"), `questions[0].answerText` 19 karakter → "size must be between 20 and 300 characters", `questions[2].pageNumber` 0 |
| B22 | Golden + prompt'ta → üretim hatası | unit (`TurnStatusResolverTest`) | givenNotFoundTurnWithAnswerChunkInPrompt_whenResolve_thenGenerationError | `GENERATION_ERROR` |
| B23 | Golden + prompt'ta yok → kaçırdı | unit | givenNotFoundTurnWithAnswerChunkOutsidePrompt_whenResolve_thenMissed | `MISSED` |
| B24 | Golden değil | unit | givenNotFoundTurnWithoutAnswerChunks_whenResolve_thenDontKnow | `DONT_KNOW` |
| B25 | Hata önce gelir | unit | givenFailedTurnWithAnswerChunks_whenResolve_thenIncomplete | `INCOMPLETE` |
| B26 | Harf/noktalama farkı (G5) | unit (`GoldenAnswerLocatorTest`) | givenQuestionDifferingInCaseAndPunctuation_whenLocateAnswerChunks_thenMatchesGoldenQuestion | "KARTIMI kaybettim ne yapmalıyım" → `{151}` |
| B27 | Tek kelime fark | unit | givenQuestionWithExtraWord_whenLocateAnswerChunks_thenNoMatchAndNoRetrievalCall | boş map; `RetrievalClient` etkileşimsiz |
| B28 | Locator, retrieval kapalı | unit | givenChunkMatchFails_whenLocateAnswerChunks_thenReturnsEmpty | boş map, exception yok |
| B29 | Konuşma detayı işaretleri | unit (`ConversationReadServiceTest`) | givenGoldenNotFoundTurn_whenGetConversationDetail_thenMissedAndNoChunkContainsAnswer | tur `MISSED`; prompt parçalarının hepsi `containsAnswer` false; başka turda cevap parçası prompt'taysa o satır true |
| B30 | Detay, retrieval kapalı (G3) | unit | givenRetrievalDown_whenGetConversationDetail_thenDontKnowWithoutMarks | `DONT_KNOW`, bütün `containsAnswer` false |
| B31 | Liste son tur durumu | unit | givenLastTurnMissed_whenListConversations_thenLastTurnStatusMissed | `lastTurnStatus` `MISSED` |
| B32 | SSE işareti | sse (`AnswerStreamer`/controller testi) | givenGoldenQuestion_whenStreamAnswer_thenSourcesMarksAnswerChunk | `sources.contextChunks[1].containsAnswer` true, diğerleri false |
| B33 | SSE, locator hatası | sse | givenAnswerLookupFails_whenStreamAnswer_thenStreamsWithoutMarks | `token`→`sources`→`done`; hepsi false |
| B34 | Uçlar | controller (`EvalRunControllerTest`) | givenAcceptedRun_whenStartEvalRun_thenAcceptedWithLocation ve diğerleri | 202 + `Location: /api/assistant/evals/7`; `GET /evals/abc` 400 `BAD_REQUEST`; `GET /evals/999` 404 `EVAL_RUN_NOT_FOUND`; `GET /evals?size=101` 400 `VALIDATION_FAILED` field `size`; `GET /golden-set` set yokken 404 `GOLDEN_SET_NOT_LOADED` |
| B35 | Overview ve Kaçanlar sınırı | integration | givenRunningAndDoneRuns_whenGetEvalOverview_thenReturnsRunningRunAndFirstTwentyFourMissedOrMoved | `runningRun` dolu; `lastDoneRun` 30 uygun sorudan `position` sırasıyla ilk 24; vector = prompt olan ve ölçülemeyen sorular yok |
| R1 | Harf ve satır sonu farkı | integration (`ChunkServiceIntegrationTest`) | givenTextDifferingInCaseAndLineBreaks_whenFindChunkMatches_thenReturnsChunk | içerik "Kartınızı\nKAYBETTİYSENİZ ya da" ↔ metin "kartınızı kaybettiyseniz ya da" → parça döner |
| R2 | Sadece güncel sürüm | integration | givenTextOnlyInSourceWithoutCurrentVersion_whenFindChunkMatches_thenNoChunks | `chunks` [] |
| R3 | İki parçada | integration | givenTextInTwoChunks_whenFindChunkMatches_thenReturnsBothByChunkId | iki parça, `chunkId` artan |
| R4 | Boş bilgi bankası | integration | givenNoSearchableSource_whenFindChunkMatches_thenEveryMatchIsEmpty | `matches` istek kadar, hepsi [] |
| R5 | Doğrulama | controller | givenInvalidTexts_whenFindChunkMatches_thenValidationFailed | `texts` [] → `size must be between 1 and 500`; `["ok", "  "]` → field `texts[1]` `must not be blank`; olmayan çalışma alanı → 404 `WORKSPACE_NOT_FOUND` |
| R6 | Sıra ve tekrar | integration | givenDuplicateTexts_whenFindChunkMatches_thenKeepsRequestOrder | 3 metin (A, B, A) → 3 eleman, aynı sıra, `text` gönderildiği gibi |
| R7 | Noktalama korunur | integration | givenDifferentDigitPunctuation_whenFindChunkMatches_thenNoMatch | "1650 TL" ↔ içerik "1.650 TL" → [] |
| W1 | Set yok | component (`EvaluationPage.test.tsx`) | givenNoGoldenSet_whenPageOpens_thenRunButtonDisabledAndNoticeShown | düğme disabled; "Golden set yüklenmemiş." |
| W2 | Sadece LLM | component | givenLlmOnlyMode_whenPageOpens_thenRunButtonDisabledAndWarningShown | uyarı metni lab'daki gibi |
| W3 | Düğme metni | component | givenRerankConfig_whenPageOpens_thenButtonShowsRerankTwentyToFive | "Ölçümü çalıştır · rerank 20→5"; `TOP_K` → "Ölçümü çalıştır · top-5" |
| W4 | Ölçüm yok | component | givenNoDoneRun_whenPageOpens_thenShowsNoMeasurementText | "Henüz ölçüm yok. Mevcut retrieval ayarıyla çalıştır; …" |
| W5 | Kutular ve eşik | component | givenRecallPointNineFourSeven_whenPageOpens_thenShowsZeroNinetyFiveInRed | "0,95" kırmızı sınıfta (0,947 < 0,95); `recallAtK` 0.95 → yeşil; `mrr` 0.871 → "0,87" |
| W6 | Alt yazılar | component | givenUnmatchedAndFallback_whenPageOpens_thenSubtitlesShowCounts | "94/99 soru · 1 soru ölçülemedi"; Kaçan "5"; "rehber 2026.10 · reranker DOWN: 12 soru" |
| W7 | Kaçanlar tablosu | component | givenMissedAndMovedRows_whenPageOpens_thenShowsTwentyPlusAndPositions | `vectorRank` null → "20+", `promptPosition` null → "yok", 2 → "2. sıra"; boş liste → "Bütün sorularda doğru parça vector sırasıyla aynı yerde." |
| W8 | Satırdan Önizleme | component (router + StrictMode) | givenMissedRow_whenClicked_thenPreviewAsksQuestionOnceInNewConversation | `/onizleme`'ye geçer; `POST /conversations` 1 kez, `POST …/messages` 1 kez, gövde `{ text: soru }` |
| W9 | Başlat ve yokla | component (MSW art arda) | givenStartedRun_whenPolling_thenShowsProgressAndStopsWhenDone | "Ölçülüyor · 0/3" → "Ölçülüyor · 2/3" → kutular; DONE'dan sonra overview isteği artmaz |
| W10 | Yenileme | component | givenRunningRunInOverview_whenPageOpens_thenButtonShowsProgressAndIsDisabled | "Ölçülüyor · 37/100", disabled |
| W11 | Başlatma hataları | component (`it.each`) | givenStartError_whenRunClicked_thenShowsMessage | her `errorCode` için `errorMessages.ts`'teki metin `role="alert"`'te |
| W12 | Başarısız son ölçüm | component | givenLastFinishedRunInterrupted_whenPageOpens_thenShowsFailureNote | "Son ölçüm tamamlanamadı: servis ölçüm sırasında yeniden başladı." |
| W13 | Çalıştırmalar | component | givenTwentyOneRuns_whenPageOpens_thenFirstPageAndRowTexts | 20 satır + sayfa 2; `ev_12`, "rerank 20→5 · reranker DOWN (2)", "0,947", süren satır "ölçülüyor…", başarısız satır "başarısız" |
| W14 | Önizleme sarı | component (`PreviewPage.test.tsx`) | givenAnswerChunkInSources_whenAnswerDone_thenCardMarksChunkAndShowsLegend | ilgili düğme gold sınıfında; "✓ atıf yapılan · sarı: cevabın geçtiği parça"; hiçbiri değilse "sarı" yok |
| W15 | Konuşmalar | component (`ConversationDetailPage.test.tsx`) | givenMissedTurnWithAnswerChunk_whenDetailOpens_thenShowsChipAndLabel | "KAÇIRDI" çipi; `GENERATION_ERROR` → "ÜRETİM HATASI"; `containsAnswer` satırında "cevap burada" |
| W16 | Erişilebilirlik | component | givenLoadedEvaluationPage_whenAxeRuns_thenNoViolations | axe ihlali yok |

## Deployment planı
Yok. Yeni servis, port, ortam değişkeni ya da model yok. Golden set curl ile yüklenir (komut yukarıda). Strateji karşılaştırması mevcut `RETRIEVAL_STRATEGY=top5|rerank` ile yeniden başlatarak yapılır.

## Açık sorular ve riskler
- **recall@5 kutusu:** Ham değer 0,947 iken kutu yuvarlanmış "0,95" gösterir ama kırmızı olur. Lab da böyle; W5 bunu belgeler.
- **`answerText` seçimi:** Lead cümleyi PDF'ten değil parça penceresinden kopyalamazsa noktalama farkı yüzünden eşleşme bozulur. Bu durum "ölçülemedi" olarak görünür, sessizce kaçan sayılmaz.
- **Durum ve retrieval-api bağımlılığı:** Konuşmalar'daki KAÇIRDI / ÜRETİM HATASI hesabı retrieval-api'ye bağlıdır. retrieval-api kapalıyken bu turlar BİLMİYOR görünür (G3, kabul edilmiş davranış).
- **Ölçüm yükü:** Ölçüm sürerken Önizleme yavaşlar (embedding ve reranker paylaşılır).

STATUS: DONE
