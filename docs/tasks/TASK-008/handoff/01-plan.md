TASK: TASK-008
# Plan

## Özet
**Mevcut durum:** assistant-api iskelet (TASK-002): `AssistantApplication`, `GlobalExceptionHandler`, sağlık ucu, Flyway (migration yok). Domain kodu, Spring AI, `RestClient`, `AssistantProperties`, kategori exception'ları yok. web'de `/onizleme` sadece başlık gösteriyor. retrieval-api'de arama (`POST .../search`, TASK-006) ve tek parça ucu (`GET .../chunks/{chunkId}`, TASK-007) hazır. Lokal modeller Mac'te çalışıyor (TASK-003, `ollama.md`); Compose assistant-api'ye `OLLAMA_BASE_URL`, `CHAT_MODEL`, `RERANKER_URL`, `RERANKER_MODEL` veriyor ama servis bunları okumuyor.

**Yeni durum:**
- **assistant-api (değişen servis):** konuşma açma (`POST /api/assistant/conversations`) ve soru gönderme (`POST /api/assistant/conversations/{conversationId}/messages`, SSE). Hat: retrieval-api araması → (strateji `rerank`) reranker ile 20→5 / (`top5`) ilk 5 → numaralı parçalarla prompt → `qwen3.5:9b` stream → `token` olayları → alıntı ayrıştırma + bulamadım tespiti → tur kaydı (commit) → `sources` → `done`. İlk migration (`conversations`, `conversation_turns`, `turn_chunks`).
- **web:** Önizleme ekranı lab'daki gibi: telefon görünümü (karşılama, sorular, akan cevap, alıntı düğmeleri, "kaynak · s. N" düğmeleri, "Yeni konuşma"), yan kart ("Asistan nasıl cevaplıyor?" → "Son cevapta modele giden parçalar" + süre çubukları). İlk SSE kodu: `shared/api/eventStream.ts`. Alıntılar TASK-007'nin `shared/chunk/ChunkDialog`'unu açar.
- **retrieval-api:** değişmez (arama sözleşmesi yeterli: `vectorRank` = sonuç listesindeki sıra).
- **Docker:** Compose'a `RETRIEVAL_API_URL`, `RETRIEVAL_STRATEGY`, `ASSISTANT_HOST_LABEL`; nginx `/api/assistant/` bloğunda SSE tamponu kapalı. `ollama.md` güncellenir (dosya TASK-003'te oluşturuldu; bu task'ta assistant-api'nin değişkenleri "okunuyor" olur, cevap akışı duman testi ve reranker'ı kapatma denemesi eklenir).

**Lab'dan bilerek alınmayanlar (sonraki task'lar):** yapılandırma şeridi (`.cfg`, TASK-011), öneri çipleri ve "Kayıtlı sorular" düğmesi (TASK-012), "Faydalı mı?" ve "Temsilciye bağlan" satırları (TASK-013), sarı "cevabın geçtiği parça" işareti (TASK-016), konuşma geçmişinin prompt'a girmesi ve yeniden yazma (TASK-010).

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ikisi de sadece `openapi.yaml` ve `sse.md`'ye bakar.

**Kart cevapları (2026-11-01, kullanıcı):** 1 A (Spring AI kurulur), 2 A (24 karakter kısaltılmış ad, `title`'da tam ad), 3 B (`assistant.host-label`, boşsa gizli), 4 A (boş bilgi bankası notu görünür), 5 B (reranker okuma zaman aşımı 5 sn). Hepsi planın önerdiği seçenek; plan değişmedi.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`, `docs/api/sse.md` (yeni).
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/pom.xml`, `assistant-api/CLAUDE.md`. M0'ı lokal gerçek modellerle çalıştırır. Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md`. Yeni paket yok. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**` (`sse.ts` yardımcısı dahil), `web/e2e/**`. Rapor `handoff/03-test-report.md`.
- **devops:** `deployment/docker-compose.yml`, `deployment/CLAUDE.md`, `web/nginx.conf`, `ollama.md`, kök `README.md`. M1-M3. Rapor `handoff/04-deploy-report.md`.
- **qa:** M4-M7, kabul kriterleri, iki sözleşmeyle uyum. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** yok. retrieval-api değişmediği için o servise kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Serbest metin; boş soru gönderilemez; uzunluk sınırı kartta (karar: 1-1000) | `SendMessageRequest` (`@NotBlank`, `@Size(max = 1000)`), `messageRules.ts`, `MessageComposer` (`maxLength`) | B29, F25 |
| K2 | İlk soru konuşma açar, sonrakiler aynı konuşmaya tur; "Yeni konuşma"; kanal Önizleme; kimlik backend'de | `createConversation`, `Channel.PREVIEW`, `useAnswerStream`, `PhoneFrame` | B26, B27, F10, F11 |
| K3 | Karşılama mesajı lab metni | `MessageList` (`WELCOME_MESSAGE`) | F8 |
| K4a | Sistem talimatı lab metninin aynısı (Türkçe) | `prompts/rag-system.st`, `PromptBuilder` | B4 |
| K4b | Kullanıcı mesajı `<parcalar>` içinde `[n] başlık` + metin, ardından `Soru: …` | `prompts/rag-user.st`, `PromptBuilder` | B4, B5, B33 |
| K4c | `temperature 0`, `num_ctx 8192`, cevap dili Türkçe | `spring.ai.ollama.*` (+ `think=false`) | B34, M0, M4 |
| K5a | Token token akış; en az `token`, `sources`, `done`, `error`; ayrıntı `sse.md` | `AnswerStreamer`, `eventStream.ts`, `useAnswerStream` | B15, B28, F1, F2, F8 |
| K5b | Akış öncesi hata Problem Details, sonrası `error` olayı aynı `errorCode` | `AnswerService` (öncesi), `AnswerStreamer` (sonrası), `GlobalExceptionHandler` | B16, B17, B22, B30, F15, F16 |
| K5c | İstemci bağlantıyı keserse model çağrısı iptal | `AnswerStreamer` (send hatası / `onError` / `onTimeout` → `dispose`), `useAnswerStream` (`AbortController`) | B18, F18, M1 |
| K5d | Akış sürerken giriş alanı ve gönder kapalı | `MessageComposer`, `PhoneFrame` ("Yeni konuşma" da) | F9 |
| K6a | `[n]` sadece 1..parça sayısı arasındaysa alıntı ve düğme; düğme TASK-007 parça penceresini açar | `CitationParser`, `answerFormat.ts`, `AnswerMessage`, `ChunkDialog` | B1, B2, F5, F12 |
| K6b | Atıf yapılan parçaların sayfaları "‹belge› · s. N" düğmeleri; biçim kartta | `citations.ts` (`citedChunks`, `sourceButtonLabel`), `AnswerMessage` | F7, F13 |
| K7 | Model sabit cümleyi yazarsa tur "cevap yok"; kaynak düğmeleri gösterilmez | `NotFoundAnswerDetector`, `Turn.notFoundAnswer`, `DoneEvent.notFoundAnswer`, `AnswerMessage` | B3, B21, F13 |
| K8a | Cevaptan önce "Asistan nasıl cevaplıyor?" kartı | `HowItWorksCard` | F20 |
| K8b | Cevaptan sonra "Son cevapta modele giden parçalar": `[n]`, sayfa, başlık, sıra no, mesafe; atıflıda ✓ | `ContextChunksCard`, `SourcesEvent` | F20, F23 |
| K8c | Süre çubukları (retrieval-api araması, qwen3.5 cevabı, toplam); gerçek ölçüm | `DoneEvent` süreleri, `LatencyBars`, `latencyRows.ts` | B15, F22 |
| K8d | Sarı işaret yok | `ContextChunksCard` (açıklama sadece "✓ atıf yapılan") | F20 |
| K8e | Makine etiketi kartta sorulur; sabit yazılmaz | `assistant.host-label` → `DoneEvent.hostLabel` (kart soru 3) | F22 |
| K9a | Tur kaydı: soru, arama sorgusu (= soru), üretim modu, strateji | `conversation_turns`, `TurnService.recordTurn` | B25 |
| K9b | Prompt'a giren her parçanın kaynağı ve sürümü; kimlik, prompt sırası, vector sırası, mesafe; atıf numaraları | `turn_chunks` (`source_id`, `version`, `chunk_id`, `prompt_position`, `vector_rank`, `distance`), `conversation_turns.cited` | B25 |
| K9c | Cevap, prompt/cevap token'ı, adım süreleri, hata varsa `errorCode` | `conversation_turns` kolonları | B16, B22, B25 |
| K9d | Tur akış bitince yazılır; model çağrısı sırasında transaction açık değil | `TurnService.recordTurn` (`@Transactional`, sadece kayıt); `AnswerService`/`AnswerStreamer` transaction'sız | B15 (sıra), B35 |
| K10 | Soru, cevap, parça içeriği, prompt loglanmaz | Backend planı "Loglar" | B31 |
| K11 | retrieval-api ya da dil modeli erişilemezse Türkçe mesaj; tur hata koduyla kaydedilir | `RETRIEVAL_UNAVAILABLE` (503), `CHAT_MODEL_UNAVAILABLE` (`error`), `errorMessages.ts` | B16, B17, B22, F15, F16, F24, M3 |
| K12 | Strateji `assistant.*` ile `top5`/`rerank`, varsayılan `rerank`; 20 ve 5 yapılandırma; ekranda düğme yok | `AssistantProperties.Retrieval` | B32, B6, B8 |
| K13 | Rerank'te 20 aday puanlanır; en yüksek 5; eşitlikte vector sırası; `top5`'te reranker çağrılmaz | `ContextRetriever` | B6, B7, B8, B10 |
| K14 | Reranker zaman aşımı/hata → vector sırasının ilk 5'i; strateji "reranker DOWN · vector sırası"; cevap normal akar; süre kartta | `ContextRetriever` (`RERANK_FALLBACK`), `RerankClient` zaman aşımı (kart soru 5) | B9, B13, M2 |
| K15 | 20 adayın vector sırası, mesafesi, rerank puanı kayıtta; prompt'takilerin vector sırası ve prompt yeri | `turn_chunks` (her aday bir satır) | B25 |
| K16 | Yan kartta rerank puanı ("rerank 0,97 · d 0,312"); alt başlık "top-5" / "rerank 20→5" / "reranker DOWN · vector sırası" | `ContextChunksCard`, `strategyLabels.ts` | F20, F21 |
| K17 | Süre çubuklarına "rerank" satırı | `latencyRows.ts` | F22 |
| K18 | Reranker'a giden metin loglanmaz; aday sayısı ve süre loglanır | `RerankClient` logu | B31 |
| S1 | "Kartımı kaybettim, ne yapmalıyım?" → kelime kelime, kayıp kart parçasına dayalı, `[1]` alıntı; tıklayınca parça açılır | K5a, K6a | E1, M4 |
| S2 | "Bitcoin alıp satabilir miyim?" → bulamadım cümlesi | K7 | E2, M4 |
| S3 | Cevap akarken sayfa kapanır → model çağrısı iptal | K5c | B18, F18, M1 |
| S4 | Dil modeli kapalı → Türkçe hata | K11 | B17, F16, M3 |
| S5 | `rerank`, "Ev kredisini erken kapatırsam ceza öder miyim?" → vector 7. parça prompt'ta 1.; yan kart puanlarla | K13, K16 | B6, M5 |
| S6 | Reranker durdurulur → "reranker DOWN · vector sırası", ilk 5 vector | K14 | B9, M2 |
| S7 | Hiç belge yok → parça yok, bulamadım cümlesi; ipucu kartta sorulur | Kararlar #7 (model çağrılmaz), `AnswerMessage` ipucu (kart soru 4) | B20, F14, M1 |
| AC1 | Gerçek modellerle ≥5 rehber + 2 rehber dışı soru; cevaplar, alıntılar, süreler raporda | qa | M4 |
| AC2 | `sse.md` yazılmış; backend ve frontend uyuyor | planner; qa karşılaştırır | B28, F1-F4, M7 |
| AC3 | Gerçek reranker ile ≥5 soruda top-5 ve rerank sıraları karşılaştırılmış; reranker kapalı yedek gerçek sistemde | qa, devops | M5, M2 |
| AC4 | Alıntı ayrıştırma, bulamadım, prompt, iptal, rerank sırası, eşitlik, zaman aşımı, yedek testli | tester | B1-B3, B4, B18, B6, B7, B13, B9 |
| AC5 | Önizleme lab'ın sol yarısıyla aynı görünür | frontend-coder, qa | M6 |
| AC6 | İki servisin `./mvnw verify`'ı, `npm run build`, `npm run lint` | coder'lar, tester | Raporlar |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Akış sürerken menüden başka sayfaya geçilir ya da sayfa yenilenir | İstek iptal edilir (sunucuda model durur, tur `STREAM_CANCELLED`). Önizleme'ye dönünce ekran yeni konuşmayla açılır (eski konuşma veritabanında kalır, TASK-014'te görünür) | B18, F18 |
| G2 | Bilgi bankasında 5'ten (ya da 20'den) az parça var | Ne varsa o kadar aday puanlanır; prompt'a en fazla 5 girer; alıntı aralığı gerçek parça sayısıdır (3 parçada `[4]` alıntı değil) | B10, B2 |
| G3 | Aynı konuşmaya aynı anda ikinci soru (başka sekme ya da doğrudan API) | İkincisi 409 `TURN_IN_PROGRESS`; tur yazılmaz; ilki etkilenmez | B23 |
| G4 | Sadece boşluktan oluşan, 1000 karakterden uzun ya da Türkçe karakterli soru | Boşluk: "Gönder" pasif, istek yok; uzun: kutu 1000 karakterden fazlasını almaz, sunucu 1001'i 400 ile reddeder; Türkçe karakter olduğu gibi gider, baştaki/sondaki boşluk silinir | B29, F25 |
| G5 | Sayfa açıkken veritabanı sıfırlanır (konuşma yok olur) | "Konuşma bulunamadı. Yeni konuşma başlat." mesajı; bir sonraki soru kendiliğinden yeni konuşma açar | B24, F19 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Konuşma ayrı uçla açılır** (`POST /api/assistant/conversations` → 201 `ConversationResponse`). web ilk soruda (ve "Yeni konuşma"dan sonraki ilk soruda) önce bunu, sonra `.../messages`'ı çağırır | Kural 2: kimlik backend'de; lab adresi `.../conversations/{id}/messages` olduğu gibi kalır | İlk soruyu kimliksiz bir uca göndermek: akış başlamadan kimliği istemciye iletmek için ek olay gerekir. "Yeni konuşma"ya basınca açmak: hiç sorusu olmayan konuşmalar birikir |
| 2 | **Akış retrieval ve rerank bittikten sonra açılır.** retrieval hatası 503 Problem Details; dil modeli hatası (ilk token'dan önce de) `error` olayı | `sse-streaming`: retrieval akış öncesi kontroldür. Model hatasını HTTP status'a çevirmek için ilk token'ı bekleyip ancak sonra yanıt başlatmak gerekir; prefill birkaç saniye sürer | İlk token'ı bekleyip 503 dönmek: istek thread'i bekler, kod karmaşıklaşır; web iki yolu zaten aynı mesajla gösterir |
| 3 | **`SseEmitter`** (servlet), dil modeli Spring AI `ChatModel.stream(Prompt)` (`Flux<ChatResponse>`); abonelik `Disposable` olarak tutulur, `emitter.send` `IOException`'ında, `onError`, `onTimeout` ve `onCompletion`'da `dispose` | İptal ve "tur bir kez kaydedilir" kuralı açıkça kodda; test kaydeden sahte emitter ile yazılır | `Flux<ServerSentEvent>` dönmek: iptal framework'te gizli; kopmada tur kaydının garanti edilmesi zor |
| 4 | **Strateji değerleri:** `RetrievalStrategy { TOP_K, RERANK, RERANK_FALLBACK }`. Yapılandırma `assistant.retrieval.strategy` = `top5` → `TOP_K`, `rerank` → `RERANK`; başka değer açılışta bağlama hatası. Reranker başarısız olunca tur `RERANK_FALLBACK` | Kural 12 değerleri TASK'tan; kural 14 "reranker DOWN" turu ayrı görünmeli (TASK-014 "RAG · reranker DOWN") | `RERANK` + ayrı boolean: TASK-014 iki alana bakmak zorunda kalır |
| 5 | **Aday kaydı ayrı tablo** `turn_chunks` (her aday bir satır: vector sırası, mesafe, rerank puanı, prompt yeri ya da `null`, kaynak, sürüm, sayfa, başlık, sıra no). `top5`'te 5, `rerank`'te 20 satır | Kural 9 ve 15 tek yapıda; TASK-014 tabloyu doğrudan okur. Başlık ve sayfa saklanır: Konuşmalar her satır için retrieval-api'ye gitmez | Turda paralel diziler (`chunk_ids`, `distances`, `scores`): okunması ve doğrulanması zor |
| 6 | **Aynı anda tek tur:** bellekte `ActiveTurnRegistry` (`ConcurrentHashMap` kümesi); istek başında `tryStart`, tur kaydedilince (her yolda) `finish` | Tur akış bitince yazıldığı için veritabanında "süren tur" satırı yoktur; tek servis kopyası | Turu başta yazıp sonra güncellemek: kural 9 "akış bitince yazılır" ile çelişir |
| 7 | **Boş bilgi bankası** (arama 0 sonuç): dil modeli çağrılmaz; `notFoundAnswer` tek `token` olayıyla gider, tur `not_found_answer = true`, token sayıları `null` | Senaryo "parça gelmez ve cevap … olur" deterministik sağlanır; boş `<parcalar>` ile modelin ne yazacağı garanti değil | Modeli boş parçalarla çağırmak: cevap kayabilir, 2-4 sn boşa gider |
| 8 | **Bulamadım tespiti:** cevap (Türkçe küçük harf, boşluklar teke) sabit cümleyi (aynı normalizasyonla) **içeriyorsa** `notFoundAnswer = true` | Model cümlenin sonuna `[1]` ekleyebilir ya da başına kısa bir giriş yazabilir (lab da "içerir" kontrolü yapıyordu) | Tam eşitlik: "Bu konuda rehberimizde bilgi bulamadım. [2]" kaçar |
| 9 | **Konuşma geçmişi prompt'a girmez;** her tur sistem + tek kullanıcı mesajıdır | Kural 4 prompt'u tam tarif ediyor; geçmiş ve yeniden yazma TASK-010 | Lab gibi geçmişi eklemek: TASK-010'un kapsamına girer, prompt testleri iki kez değişir |
| 10 | **Prompt şablonları:** `prompts/rag-system.st` (lab metni; 3. kuraldaki cümle `{notFoundAnswer}` ile), `prompts/rag-user.st` (`<parcalar>\n{chunks}\n</parcalar>\n\nSoru: {question}`). Parça bloğu `[n] {heading}\n{content}`, bloklar arası boş satır (lab `userMessage`) | `backend-conventions` §7; tek cümle iki yerde yaşamaz | `[n] başlık (s. sayfa)` (rag-pipeline örneği): TASK "lab'daki gibi" diyor |
| 11 | **Alıntı sırası:** `citations` cevapta ilk geçiş sırasıyla, tekrarsız. "kaynak · s. N" düğmeleri bu sırayla, aynı `chunkId` bir kez (lab) | Lab `cites` + `pages` | Numara sırası: cevabın okunuş sırasıyla ayrışır |
| 12 | **Rerank isteği** `POST {base-url}/v1/rerank` `{model, query, documents}` (`top_n` yok, 20'sinin de puanı gelir); belge = `heading + "\n\n" + content`, vector sırasıyla. Yanıtta `results` sayısı ≠ belge sayısı, tekrarlı ya da aralık dışı `index` → hata (yedek yol) | `rag-pipeline` §4; eksik puanla sıralama yapılamaz | `top_n: 5`: kural 15 20 puanı istiyor |
| 13 | **Sıralama:** puan azalan, eşitlikte `vectorRank` artan; ilk `topK`. `Double.compare` ile | Kural 13 | — |
| 14 | **retrieval-api hatası tek kod:** bağlantı, zaman aşımı, her non-2xx (kendi `EMBEDDING_UNAVAILABLE`'ı dahil), okunamayan gövde → 503 `RETRIEVAL_UNAVAILABLE`; logda upstream status ve `errorCode` | Kullanıcı için hepsi "bilgi bankasına ulaşılamıyor"; sözleşme yüzeyi küçük | Her upstream kodu yansıtmak: web'de ikinci bir sözlük |
| 15 | **Zaman aşımları:** retrieval-api bağlantı 2 sn, okuma 15 sn; reranker bağlantı 1 sn, okuma 5 sn (kart soru 5); dil modeli ilk token 60 sn, token arası 30 sn; emitter 300 sn | ollama.md ölçümleri: rerank sıcak 0,65 sn; ilk soğuk yanıt ~10 sn; arama soğuk embedding ~1,1 sn + retrieval-api'nin 3 denemesi | Tek genel süre: yavaş prefill ile takılmış bağlantı ayırt edilmez |
| 16 | **Önizleme durumu sayfada tutulur** (`useAnswerStream`); sayfadan çıkınca akış iptal edilir, dönünce yeni konuşma | `frontend-conventions` §8: unmount'ta `abort`; konuşmanın kalıcı kaydı backend'de (TASK-014 gösterir) | Durumu `AdminLayout`'a taşımak: akış başka sayfadayken sürer, iptal kuralıyla çelişir |
| 17 | **Cevap biçimi kütüphanesiz:** lab `fmtAnswer`'ın karşılığı saf fonksiyon (`answerFormat.ts`): satırlara böl, `- ` / `* ` ile başlayan satır madde, `**x**` kalın, `[n]` (akış bitince, aralıktaysa) alıntı düğmesi; React öğeleri, `dangerouslySetInnerHTML` yok | Lab görünümü; güvenli render (`frontend-security`); yeni dependency yok | Markdown kütüphanesi: dependency + HTML üretir |
| 18 | **Yan kart son turu gösterir:** mevcut konuşmanın son turu `done` ise "Son cevapta …", değilse (bekliyor, akıyor, hata) "Asistan nasıl cevaplıyor?" (lab `sys.last`) | Lab davranışı | — |
| 19 | **"Asistan nasıl cevaplıyor?" metni lab'ın rerank hâliyle sabit**; strateji ve model adına göre değişmesi TASK-011 kural 6. Alt başlık ve süre etiketi ise turun gerçek verisinden (`strategyLabel`, `chatModel`) | Kapsam: etkin yapılandırma ucu TASK-011'de | Bu task'ta yapılandırma ucu açmak: TASK-011'in kapsamına girer |
| 20 | **Model süre etiketi** `chatModel`'in `:`'dan önceki kısmı + " cevabı" (`qwen3.5:9b` → "qwen3.5 cevabı"); rerank satırı `rerankLatencyMs` doluysa (yedek yolda başarısız denemenin süresi) | Lab metni, sabit model adı olmadan; yedek yolda harcanan süre görünür | — |
| 21 | **Spring AI yapılandırması açılışta Ollama'ya gitmez** (`pull-model-strategy=never`, embedding otomatik yapılandırması kapalı); model kapalıyken servis `healthy` | `deployment/CLAUDE.md`: model kapalıyken servisler sağlıklı kalır | — |
| 22 | **springdoc bu task'ta da eklenmez;** sözleşme uyumunu qa elle kontrol eder | Yeni dependency sadece Spring AI (kart soru 1) | — |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu: assistant-api'nin TASK-008 uçları.
- `tags`: `Conversations`.
- Yeni: `POST /api/assistant/conversations` (`createConversation`): `CreateConversationRequest`; 201 + `Location` + `ConversationResponse`; 400 `VALIDATION_FAILED` (`channel`), `BAD_REQUEST`; 415; 500.
- Yeni: `POST /api/assistant/conversations/{conversationId}/messages` (`sendMessage`): `SendMessageRequest`; 200 `text/event-stream`; 400 `VALIDATION_FAILED` (`text`), `BAD_REQUEST`; 404 `CONVERSATION_NOT_FOUND`; 409 `TURN_IN_PROGRESS`; 415; 500; 503 `RETRIEVAL_UNAVAILABLE`. `Accept: text/event-stream, application/problem+json` zorunlu notu.
- `components.schemas`: `Channel`, `CreateConversationRequest`, `ConversationResponse`, `SendMessageRequest`.
- `ProblemDetail.errorCode` enum'u: `CONVERSATION_NOT_FOUND`, `TURN_IN_PROGRESS`, `RETRIEVAL_UNAVAILABLE`, `CHAT_MODEL_UNAVAILABLE` (sonuncusu sadece SSE `error` olayında).

`docs/api/sse.md` (yeni): endpoint ve başlıklar, biçim, sıra (başarılı / hatalı / boş bilgi bankası / `STREAM_INTERRUPTED`), olaylar `token` (`TokenEvent`), `sources` (`SourcesEvent`, `ContextChunk`, `RetrievalStrategy`), `done` (`DoneEvent`), `error` (`StreamErrorEvent`: `CHAT_MODEL_UNAVAILABLE`, `INTERNAL_ERROR`), isim tablosu.

`websocket.md`: yok. retrieval-api kısmı değişmedi.

**Uygulama sırasında (backend-coder bildirimi):** `sse.md` "Biçim"e not: iki noktadan sonraki boşluk isteğe bağlı; sunucu `SseEmitter` ile boşluksuz yazar (`event:token`), istemci iki biçimi de kabul eder. Olay adları ve alanlar değişmedi. Etkilenen test: F1/F2 (ayrıştırıcı boşluklu ve boşluksuz satırı aynı okur), B28 (gövde boşluksuz biçimle karşılaştırılır).

## Backend planı
Ek skill'ler: `rag-pipeline`, `sse-streaming`, `java-concurrency`, `spring-transactions`, `springboot-patterns`, `jpa-patterns`, `flyway-migrations`, `postgres-patterns`, `springboot-security` (prompt injection), `spring-mvc-testing`.

### Dependency (kart soru 1 onaylanırsa)
- `org.springframework.ai:spring-ai-bom` (`dependencyManagement`, `import`) + `org.springframework.ai:spring-ai-starter-model-ollama`. Sürüm: Spring Boot 4.1.1 ile uyumlu son kararlı Spring AI (2.x); backend-coder sürümü `pom.xml`'e ve `assistant-api/CLAUDE.md` "Teknoloji"ye yazar, Ollama seçeneklerinin (model, `temperature`, `num-ctx`, `think`) property anahtarlarını o sürümün dokümanından doğrular. `think=false` gönderilemiyorsa planner'a ve lead'e yazar (NEEDS_FIX).
- `spring-boot-starter-restclient` (Boot 4'te `RestClient.Builder` otomatik yapılandırması; Spring AI starter getirmiyorsa). Sürüm Boot BOM'undan.
- Test için yeni dependency yok (`MockRestServiceServer` spring-test'te; zaman aşımı testi JDK `HttpServer` ile).

### Migration: `V1__create_conversations_and_turns.sql`
- `conversations`: `id bigint generated always as identity primary key`, `channel text not null check (channel in ('PREVIEW'))`, `created_at timestamptz not null`.
- `conversation_turns`: `id` identity PK; `conversation_id bigint not null references conversations(id)`; `turn_no int not null check (turn_no >= 1)`, `unique (conversation_id, turn_no)`; `question text not null check (char_length(question) between 1 and 1000)`; `search_query text not null`; `generation_mode text not null check (generation_mode in ('RAG'))`; `retrieval_strategy text not null check (... in ('TOP_K','RERANK','RERANK_FALLBACK'))`; `candidates int not null`, `top_k int not null`; `chat_model text not null`; `answer text` (null: model hiç cevap üretmeden hata); `not_found_answer boolean not null default false`; `cited int[] not null default '{}'`; `prompt_tokens int`, `completion_tokens int`; `retrieval_latency_ms int`, `rerank_latency_ms int`, `generation_latency_ms int`, `time_to_first_token_ms int`, `latency_ms int not null`; `error_code text check (error_code in ('RETRIEVAL_UNAVAILABLE','CHAT_MODEL_UNAVAILABLE','STREAM_CANCELLED','INTERNAL_ERROR'))`; `created_at timestamptz not null` (soru geldi), `completed_at timestamptz not null` (kaydedildi). Index `(conversation_id, turn_no)` unique'ten gelir.
- `turn_chunks`: `turn_id bigint not null references conversation_turns(id) on delete cascade`, `vector_rank int not null check (vector_rank >= 1)`, `chunk_id bigint not null`, `source_id bigint not null`, `version text not null`, `chunk_no int not null`, `page_number int not null`, `heading text not null`, `distance double precision not null`, `rerank_score double precision`, `prompt_position int check (prompt_position >= 1)`; `primary key (turn_id, vector_rank)`, `unique (turn_id, prompt_position)`. `chunk_id` retrieval veritabanına FK değildir (ayrı veritabanı).

### Properties (`application.properties`, dosya ASCII kalır)
```
assistant.workspace-id=42
assistant.host-label=${ASSISTANT_HOST_LABEL:}
assistant.retrieval-api.base-url=${RETRIEVAL_API_URL:http://localhost:8080}
assistant.retrieval-api.connect-timeout=2s
assistant.retrieval-api.read-timeout=15s
assistant.retrieval.strategy=${RETRIEVAL_STRATEGY:rerank}
assistant.retrieval.candidates=20
assistant.retrieval.top-k=5
assistant.reranker.base-url=${RERANKER_URL:http://localhost:8081}
assistant.reranker.model=${RERANKER_MODEL:bge-reranker-v2-m3}
assistant.reranker.connect-timeout=1s
assistant.reranker.read-timeout=5s
assistant.generation.not-found-answer=Bu konuda rehberimizde bilgi bulamadım.
assistant.generation.first-token-timeout=60s
assistant.generation.token-idle-timeout=30s
assistant.generation.stream-timeout=300s
spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
spring.ai.ollama.init.pull-model-strategy=never
# chat: model ${CHAT_MODEL:qwen3.5:9b}, temperature 0, num-ctx 8192, think false (anahtar adları Spring AI sürümüne göre)
# embedding otomatik yapılandırması kapalı (assistant-api embedding üretmez)
```
`assistant-api/CLAUDE.md`'deki "Hedef" ayar satırı bu anahtarlarla değiştirilir.

### Sınıflar (kök `com.rasitbank.assistant`; yeni her pakette `package-info.java` `@NullMarked`)
**config/**
- `AssistantProperties` (`@ConfigurationProperties("assistant")`, `@Validated` record): `long workspaceId`, `@Nullable String hostLabel` (boş → `null`), `RetrievalApi retrievalApi(String baseUrl, Duration connectTimeout, Duration readTimeout)`, `Retrieval retrieval(@Pattern("top5|rerank") String strategy, @Min(1) @Max(50) int candidates, @Min(1) int topK)` + `RetrievalStrategy configuredStrategy()`, `Reranker reranker(String baseUrl, String model, Duration connectTimeout, Duration readTimeout)`, `Generation generation(String notFoundAnswer, Duration firstTokenTimeout, Duration tokenIdleTimeout, Duration streamTimeout)`. `topK > candidates` açılışta hata.
- `ClockConfig` (`Clock.systemUTC()`).
- `RestClientConfig`: `@Bean RestClient retrievalRestClient(...)`, `@Bean RestClient rerankRestClient(...)`; her biri kendi base URL'i ve bağlantı/okuma zaman aşımıyla (`JdkClientHttpRequestFactory` ya da `SimpleClientHttpRequestFactory`).

**exception/**
- `BaseException` (abstract; `errorCode`, `HttpStatus`), `NotFoundException` (404), `ConflictException` (409), `ServiceUnavailableException` (503) — `problem-details-rfc9457` şablonları. `GlobalExceptionHandler`'a `@ExceptionHandler(BaseException.class)` (status + `errorCode` + `type`).

**conversation/**
- `domain/entity/Channel { PREVIEW }`, `GenerationMode { RAG }`, `RetrievalStrategy { TOP_K, RERANK, RERANK_FALLBACK }`.
- `domain/entity/Conversation` (`id`, `channel`, `createdAt`; `static Conversation open(Channel, Instant)`).
- `domain/entity/Turn` (kolonlar migration'daki gibi; `cited` `int[]` / `List<Integer>` `@JdbcTypeCode(SqlTypes.ARRAY)`; `@OneToMany(cascade = ALL) List<TurnChunk> chunks`; `static Turn record(...)` fabrika metodu). `TurnChunk` entity (`@IdClass` ya da `@EmbeddedId` `(turnId, vectorRank)`).
- `domain/dto/CreateConversationRequest(@NotNull(message = "must not be null") @Nullable Channel channel)`, `ConversationResponse(long conversationId, Channel channel, Instant createdAt)` + `from(Conversation)`, `SendMessageRequest(@NotBlank(message = "must not be blank") @Size(max = 1000, message = "size must be at most 1000 characters") @Nullable String text)`.
- `domain/event/TokenEvent(String text)`, `SourcesEvent(RetrievalStrategy retrievalStrategy, int candidates, int topK, List<ContextChunk> contextChunks, List<Integer> citations)`, `ContextChunk(int n, long chunkId, int chunkNo, int pageNumber, String heading, long sourceId, String sourceName, String version, int vectorRank, double distance, @Nullable Double rerankScore)`, `DoneEvent(long turnId, boolean notFoundAnswer, String chatModel, @Nullable Integer promptTokens, @Nullable Integer completionTokens, long retrievalLatencyMs, @Nullable Long rerankLatencyMs, @Nullable Long generationLatencyMs, @Nullable Long timeToFirstTokenMs, long latencyMs, @Nullable String hostLabel)`, `StreamErrorEvent(String errorCode)` — alan adları `sse.md` ile birebir.
- `repository/ConversationRepository`, `TurnRepository` (`@Query("select coalesce(max(t.turnNo), 0) from Turn t where t.conversation.id = :conversationId") int findMaxTurnNo(@Param("conversationId") long conversationId)`).
- `service/ConversationService`: `@Transactional ConversationResponse createConversation(CreateConversationRequest request)`; `@Transactional(readOnly = true) Conversation getConversation(long conversationId)` → yoksa `NotFoundException("CONVERSATION_NOT_FOUND", ...)`.
- `service/ActiveTurnRegistry` (`@Component`): `boolean tryStart(long conversationId)`, `void finish(long conversationId)`.
- `service/TurnDraft` (record): `conversationId`, `question` (stripped), `searchQuery` (= question), `receivedAt` (`Instant`), `startNanos`, `RetrievedContext context` (`null` olabilir: retrieval hatası).
- `service/TurnOutcome` (record): `@Nullable String answer`, `boolean notFoundAnswer`, `List<Integer> citations`, `@Nullable Integer promptTokens`, `@Nullable Integer completionTokens`, `@Nullable Long generationLatencyMs`, `@Nullable Long timeToFirstTokenMs`, `long latencyMs`, `@Nullable String errorCode`.
- `service/TurnService`: `@Transactional long recordTurn(TurnDraft draft, TurnOutcome outcome)` → `turn_no = findMaxTurnNo + 1`, tur ve `turn_chunks` satırları (adaylar; prompt'takilere `prompt_position`), `completedAt = Instant.now(clock)`; dönüş `turnId`. Tek transaction, içinde dış çağrı yok.
- `service/AnswerService`: `SseEmitter sendMessage(long conversationId, SendMessageRequest request)`. Sıra: (1) `conversationService.getConversation` (404); (2) `registry.tryStart` yoksa `ConflictException("TURN_IN_PROGRESS", ...)`; (3) `contextRetriever.retrieveContext(question)`; `RETRIEVAL_UNAVAILABLE` gelirse `turnService.recordTurn(... errorCode)` + `registry.finish` + yeniden fırlat (503); (4) `contextChunks` boşsa `answerStreamer.streamFixedAnswer(draft, notFoundAnswer, emitter)`, değilse `promptBuilder.buildPrompt(question, contextChunks)` → `answerStreamer.streamModelAnswer(draft, prompt, emitter)`; (5) emitter'ı döner. Beklenmeyen hata 2-4 arasında olursa `registry.finish`.
- `service/AnswerStreamer` (`@Component`; `ChatModel`, `CitationParser`, `NotFoundAnswerDetector`, `TurnService`, `ActiveTurnRegistry`, `AssistantProperties`, `Clock`):
  - `void streamModelAnswer(TurnDraft draft, Prompt prompt, SseEmitter emitter)`: `chatModel.stream(prompt)` → ilk öğe için `firstTokenTimeout`, sonrakiler için `tokenIdleTimeout` (`Flux.timeout(Publisher, Function)`); boş olmayan her metin parçası `token`; ilk metinde `timeToFirstTokenMs`; son `ChatResponse`'un `Usage`'ından `promptTokens`/`completionTokens` (gelmezse `null`). Tamamlanınca: alıntı + bulamadım → `recordTurn` → `sources` → `done` → `emitter.complete()`. Model hatası/zaman aşımı → `recordTurn(errorCode = CHAT_MODEL_UNAVAILABLE, answer = o ana kadarki metin ya da `null`)` → `error` → `complete()`. Başka hata → `INTERNAL_ERROR` aynı yol. `emitter.send` `IOException` ya da emitter `onError`/`onTimeout` → `dispose`, `recordTurn(errorCode = STREAM_CANCELLED)`, başka olay gönderilmez. Kayıt ve `registry.finish` her turda tam bir kez (`AtomicBoolean`).
  - `void streamFixedAnswer(TurnDraft draft, String answer, SseEmitter emitter)`: tek `token` → `recordTurn(notFoundAnswer = true, token ve üretim süreleri null)` → `sources` (boş) → `done`.
  - Emitter `new SseEmitter(streamTimeout.toMillis())`. Olay adları sabit: `token`, `sources`, `done`, `error`; `data` JSON (`MediaType.APPLICATION_JSON`).
- `controller/ConversationController` (`@RequestMapping("/api/assistant/conversations")`):
  - `@PostMapping ResponseEntity<ConversationResponse> createConversation(@Valid @RequestBody CreateConversationRequest request)` → 201 + `Location` `/api/assistant/conversations/{id}`.
  - `@PostMapping(path = "/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE) ResponseEntity<SseEmitter> sendMessage(@PathVariable long conversationId, @Valid @RequestBody SendMessageRequest request)` → `ResponseEntity.ok().header("Cache-Control", "no-cache").header("X-Accel-Buffering", "no").body(answerService.sendMessage(...))`.

**retrieval/**
- `client/SearchChunksRequest(String query, int topK)`, `client/SearchResponse(List<SearchHit> hits)`, `client/SearchHit(long chunkId, int chunkNo, int pageNumber, String heading, String content, long sourceId, String sourceName, String version, double distance)` — retrieval-api sözleşmesiyle aynı.
- `client/RetrievalClient` (`@Component`, `retrievalRestClient`): `List<SearchHit> search(String query, int topK)` → `POST /api/workspaces/{workspaceId}/search`; her hata → `ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", ...)`.
- `client/RerankClient` (`@Component`, `rerankRestClient`): `List<Double> rerankChunks(String query, List<String> documents)` → `documents` sırasıyla hizalı puanlar; Kararlar #12'deki her bozukluk ve her HTTP/IO hatası → `ServiceUnavailableException("RERANKER_UNAVAILABLE", ...)` (sadece içeride yakalanır, HTTP'ye çıkmaz). İstek/yanıt record'ları `RerankRequest(String model, String query, List<String> documents)`, `RerankResponse(List<RerankResult> results)`, `RerankResult(int index, @JsonProperty("relevance_score") double relevanceScore)`.
- `service/RetrievedChunk(SearchHit hit, int vectorRank, @Nullable Double rerankScore)`; `service/RetrievedContext(RetrievalStrategy strategy, int candidates, int topK, List<RetrievedChunk> candidateChunks, List<RetrievedChunk> contextChunks, long retrievalLatencyMs, @Nullable Long rerankLatencyMs)`.
- `service/ContextRetriever`: `RetrievedContext retrieveContext(String query)`. `TOP_K` → `search(query, topK)`, aday = bağlam. `RERANK` → `search(query, candidates)`; sonuç boşsa reranker çağrılmaz; değilse `rerankChunks` → Kararlar #13 sıralaması → ilk `topK`; `RERANKER_UNAVAILABLE` → `RERANK_FALLBACK`, bağlam = ilk `topK` vector sırası, puanlar `null`. Süreler `System.nanoTime()`.

**generation/**
- `PromptBuilder`: `Prompt buildPrompt(String question, List<RetrievedChunk> contextChunks)` → `SystemMessage` (rag-system.st) + `UserMessage` (rag-user.st); seçenekler `ChatModel`'in varsayılanından (properties). Şablonlar `src/main/resources/prompts/`.
- `CitationParser`: `List<Integer> parseCitations(String answer, int contextSize)` (`\[(\d+)\]`, 1..contextSize, tekrarsız, ilk geçiş sırası).
- `NotFoundAnswerDetector`: `boolean isNotFoundAnswer(String answer)` (Kararlar #8; `Locale.forLanguageTag("tr")`).

**prompts/rag-system.st** (lab metni, birebir):
```
Sen Raşit Bankası'nın dijital asistanısın. Müşterilerin sorularını Türkçe, kısa ve net cevaplarsın.
1. Sadece <parcalar> içindeki bilgiyle cevap ver; kendi bilgini ekleme.
2. Her cümlenin sonuna dayandığı parçanın numarasını yaz, örneğin [1].
3. Cevap parçalarda yoksa sadece "{notFoundAnswer}" yaz.
```
**prompts/rag-user.st**: `<parcalar>`, `{chunks}`, `</parcalar>`, boş satır, `Soru: {question}` (Kararlar #10). Şablondaki `<`/`>` karakterleri Spring AI şablon motorunun ayraçlarıyla çakışıyorsa backend-coder şablonu metin olarak okuyup yer tutucuları kendisi doldurur; çıktı B4'teki metinle aynı olmalı.

### Loglar (İngilizce, `key=value`; soru, cevap, parça içeriği, prompt, reranker belgeleri loglanmaz)
- `INFO Conversation created conversationId={} channel={}`
- `INFO Retrieval search finished topK={} hits={} durationMs={}`; hata: `WARN Retrieval unavailable status={} errorCode={} reason={}` (reason = exception sınıf adı)
- `INFO Reranked candidates={} kept={} durationMs={}`; `WARN Reranker unavailable, falling back to vector order candidates={} topK={} reason={}`
- `INFO Turn completed conversationId={} turnId={} strategy={} questionLength={} answerLength={} promptTokens={} completionTokens={} citedCount={} notFoundAnswer={} latencyMs={}`
- `WARN Turn failed conversationId={} turnId={} errorCode={} answerLength={}`; `INFO Turn cancelled conversationId={} turnId={} answerLength={}`

### `assistant-api/CLAUDE.md` (backend-coder)
Durum yorumu (TASK-008); "Teknoloji"de Spring AI sürümü, restclient, gerçek `assistant.*` anahtarları (Hedef satırları gerçek değerlerle); "Henüz olmayanlar"dan kurulanlar çıkar (springdoc, `PageResponse`, `OpenApiConfig` kalır); paket yapısında gerçek sınıflar; "Komutlar"a duman testi:
`curl -s -X POST localhost:8090/api/assistant/conversations -H 'Content-Type: application/json' -d '{"channel":"PREVIEW"}'` ve `curl -sN -X POST localhost:8090/api/assistant/conversations/<id>/messages -H 'Content-Type: application/json' -H 'Accept: text/event-stream, application/problem+json' -d '{"text":"Kartımı kaybettim, ne yapmalıyım?"}'`.

## Frontend planı
Ek skill'ler: `sse-streaming`, `react-state-management`, `react-patterns`, `frontend-a11y`, `frontend-security`, `frontend-design` (lab'dan token aktarma), `frontend-performance` (hızlı token'lar).

**Yeni paket yok.**

### shared/
- `api/eventStream.ts`: `type ServerSentEvent = { event: string; data: string }`; `parseEventStream(body: ReadableStream<Uint8Array>): AsyncGenerator<ServerSentEvent>` (TextDecoder `stream: true`, satır tamponu, `\r\n` → `\n`, boş satırda olay; `event:` yoksa `message`; `data:` satırları `\n` ile birleşir; `:` yorum satırı atlanır); `postEventStream(path: string, body: unknown, signal: AbortSignal): AsyncGenerator<ServerSentEvent>` (`fetch` POST, `Content-Type: application/json`, `Accept: text/event-stream, application/problem+json`; `fetch` reddi → `ApiError(0, 'NETWORK_ERROR')` (abort hariç: `AbortError` olduğu gibi fırlar); `!response.ok` → `toApiError`; gövde yoksa `ApiError(status, 'UNEXPECTED_ERROR')`).
- `api/errorMessages.ts` ekler: `CONVERSATION_NOT_FOUND` "Konuşma bulunamadı. Yeni konuşma başlat.", `TURN_IN_PROGRESS` "Bu konuşmada bir cevap hâlâ yazılıyor. Bitince yeniden sor.", `RETRIEVAL_UNAVAILABLE` "Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.", `CHAT_MODEL_UNAVAILABLE` "Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.", `STREAM_INTERRUPTED` "Cevap yarıda kesildi. Soruyu yeniden gönder.". `INTERNAL_ERROR` → fallback.
- `format/formatNumber.ts`: `formatDecimal(value: number, fractionDigits: number): string` (tr-TR, sabit hane: `0.3124, 3` → "0,312"; `5.359, 2` → "5,36"; `-11.04, 2` → "-11,04").
- `format/formatDateTime.ts`: `formatTime(value: string | Date): string` (`Europe/Istanbul`, "HH:mm").

### features/preview/
- `model/types.ts`: `CHANNELS = ['PREVIEW'] as const` + `Channel`; `ConversationResponse`, `CreateConversationRequest`, `SendMessageRequest`; `RETRIEVAL_STRATEGIES = ['TOP_K', 'RERANK', 'RERANK_FALLBACK'] as const` + `RetrievalStrategy`; `ContextChunk`, `TokenEvent`, `SourcesEvent`, `DoneEvent`, `StreamErrorEvent` (alanlar `sse.md` ile birebir); `AnswerStreamEvent = { type: 'token'; data: TokenEvent } | { type: 'sources'; data: SourcesEvent } | { type: 'done'; data: DoneEvent } | { type: 'error'; data: StreamErrorEvent }`; `TURN_PHASES = ['waiting', 'streaming', 'done', 'failed'] as const`; `PreviewTurn = { localId: string; question: string; askedAt: string; answer: string; phase: TurnPhase; sources: SourcesEvent | null; done: DoneEvent | null; errorCode: string | null }`.
- `model/answerStreamEvents.ts`: `toAnswerStreamEvent(event: ServerSentEvent): AnswerStreamEvent | null` (JSON parse `try`; alan tipleri kontrol; bilinmeyen olay ya da bozuk şekil → `null`).
- `model/messageRules.ts`: `MESSAGE_MAX_LENGTH = 1000`, `normalizeMessage(input)` (= `trim()`), `canSendMessage(input)`, `MESSAGE_RULE_MESSAGE = 'Soru 1-1000 karakter olmalı.'` (`VALIDATION_FAILED` gelirse).
- `model/answerFormat.ts`: `AnswerInline = { kind: 'text'; text: string } | { kind: 'citation'; n: number } | { kind: 'bold'; children: AnswerInline[] }`; `AnswerBlock = { kind: 'paragraph'; inlines: AnswerInline[] } | { kind: 'list'; items: AnswerInline[][] }`; `formatAnswer(text: string, contextSize: number | null): AnswerBlock[]` (Kararlar #17; `contextSize` `null` iken akış sürüyor, alıntı yok; aralık dışı `[n]` metin kalır).
- `model/citations.ts`: `SOURCE_NAME_SHORT_LENGTH = 24` (kart soru 2); `citedChunks(sources: SourcesEvent): ContextChunk[]` (Kararlar #11); `sourceButtonLabel(chunk: ContextChunk): string` → `${shortenText(chunk.sourceName, SOURCE_NAME_SHORT_LENGTH)} · s. ${chunk.pageNumber}`.
- `model/strategyLabels.ts`: `strategyLabel(sources: SourcesEvent): string` → `TOP_K` `top-${topK}`, `RERANK` `rerank ${candidates}→${topK}`, `RERANK_FALLBACK` "reranker DOWN · vector sırası".
- `model/latencyRows.ts`: `type LatencyRow = { label: string; ms: number; isModel: boolean }`; `latencyRows(done: DoneEvent): LatencyRow[]` → "retrieval-api araması"; `rerankLatencyMs !== null` ise "rerank"; `generationLatencyMs !== null` ise `${modelShortName(done.chatModel)} cevabı` (`isModel: true`). `modelShortName` `:`'dan öncesi.
- `api/previewApi.ts`: `createConversation(): Promise<ConversationResponse>` (`{ channel: 'PREVIEW' }`); `sendMessage(conversationId: number, request: SendMessageRequest, signal: AbortSignal): AsyncGenerator<AnswerStreamEvent>` (`postEventStream` + `toAnswerStreamEvent`, `null`'lar atlanır).
- `hooks/useAnswerStream.ts`: `useAnswerStream(): { conversationId: number | null; turns: PreviewTurn[]; previousTurns: PreviewTurn[]; isStreaming: boolean; sendQuestion: (text: string) => void; startNewConversation: () => void }`.
  - `sendQuestion`: `normalizeMessage`; geçersizse ya da akış sürüyorsa hiçbir şey yapmaz. Yeni tur `waiting` olarak eklenir. `conversationId` `null` ise önce `createConversation`. Olaylar: `token` → `answer` updater ile eklenir, `phase` `streaming`; `sources` → `sources`; `done` → `done`, `phase` `done`; `error` → `errorCode`, `phase` `failed`. Akış `done`/`error` olmadan biterse `STREAM_INTERRUPTED`. `ApiError` → `errorCode`; `CONVERSATION_NOT_FOUND`'da `conversationId` `null` olur. `AbortError` sessiz.
  - Akış istek başına bir `AbortController`; unmount'ta `abort()`. Akış handler'dan başlar (effect'ten değil). Token'lar Query cache'ine yazılmaz.
  - `startNewConversation`: akış sürerken çalışmaz; `previousTurns` = mevcut `turns` (sadece son önceki konuşma, lab), `turns` = [], `conversationId` = `null`.
- `hooks/useClock.ts`: `useClock(): string` (`formatTime`, 30 sn'de bir güncellenir; telefonun durum çubuğu).
- `pages/PreviewPage.tsx`: `PageHeader` (bugünkü metinler) + `.pv` ızgarası (lab: 322 px + kalan): solda `PhoneFrame` (içinde `MessageList` + `MessageComposer`), sağda `.side2` (`HowItWorksCard` ya da `ContextChunksCard`, Kararlar #18). Açık `chunkId` sayfa state'inde; varsa `ChunkDialog`. Durum duyurusu `aria-live="polite"` görünmez bölge: akış başlayınca "Asistan yazıyor…", `done` → "Cevap tamamlandı.". Hata bu bölgeye yazılmaz: `AnswerMessage`'taki `role="alert"` zaten okur, iki kez okunmaz (lead onayıyla, frontend-coder önerisi).
- `components/PhoneFrame.tsx` (+ css; lab `.phone/.scr/.sbar/.abar`): props `{ isStreaming: boolean; onNewConversation: () => void; children: ReactNode }`; durum çubuğu `useClock()` + "5G"; avatar SVG (lab), "Dijital Asistan", "Raşit Mobil · 7/24", "Yeni konuşma" (`disabled={isStreaming}`).
- `components/MessageList.tsx` (+ css; lab `.msgs`): props `{ previousTurns: PreviewTurn[]; turns: PreviewTurn[]; conversationId: number | null; onCitationOpen: (chunkId: number) => void }`. Karşılama balonu `WELCOME_MESSAGE` = "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılık sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplarım. Bakiyen gibi sana özel bilgileri göremem."; `previousTurns` varsa onlar + ayraç (`.sysm`) "Yeni konuşma" (kimlik geldikten sonra "Yeni konuşma · {conversationId}"); her tur `UserMessage` + `AnswerMessage`. Yeni tur ve her token'da son soru görünür kalacak şekilde kaydırır (lab `scrollMsgs`).
- `components/UserMessage.tsx`: soru balonu + `formatTime(askedAt)` (`.tm`).
- `components/AnswerMessage.tsx` (+ css; lab `.bub`, `.cite`, `.srcs`, `.typing`, `.m.a.bad`): props `{ turn: PreviewTurn; onCitationOpen }`. `waiting` ve metin yok → üç noktalı "yazıyor" göstergesi (`prefers-reduced-motion`'da animasyonsuz). Metin `formatAnswer(answer, turn.done ? contextChunks.length : null)`; alıntı `<button className="cite" aria-label="Kaynak {n}">{n}</button>` → `onCitationOpen(contextChunks[n-1].chunkId)`. `done` ve `!notFoundAnswer` → `.srcs` düğmeleri `sourceButtonLabel` (`aria-label` yok: erişilebilir ad görünen metindir, WCAG 2.5.3; `title` = `${sourceName} · sayfa ${pageNumber}` (kart soru 2, "üstüne gelince tam ad"); tık → `onCitationOpen(chunkId)`) (lead onayıyla, frontend-coder önerisi). `done` ve `contextChunks.length === 0` → ipucu (kart soru 4): "Bilgi bankasında henüz belge yok." + `<Link to="/kaynaklar">Bilgi kaynakları'ndan belge ekle.</Link>`. `failed` → yarım metin (varsa) + altında `role="alert"` `messageFor(errorCode)` (`VALIDATION_FAILED`'da `MESSAGE_RULE_MESSAGE`); balon `.bad` kenarlı.
- `components/MessageComposer.tsx` (+ css; lab `.comp`): props `{ isStreaming: boolean; onSend: (text: string) => void }`; `<form>` + `<input aria-label="Soru" placeholder="Sorunu yaz…" maxLength={MESSAGE_MAX_LENGTH} disabled={isStreaming}>` + `<button type="submit" aria-label="Gönder" disabled={isStreaming || !canSendMessage(value)}>` (lab ok SVG'si). Gönderince alan boşalır. Lab'daki "Kayıtlı sorular" düğmesi yok (TASK-012).
- `components/HowItWorksCard.tsx`: `.card` "Asistan nasıl cevaplıyor?" + iki `.sub`: "Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5'i qwen3.5:9b'ye gider. Cevaptaki numaralar rehberin parçalarını gösterir." ve "Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek." (Kararlar #19).
- `components/ContextChunksCard.tsx` (+ css; lab `.ctxl`): props `{ turn: PreviewTurn (done); onChunkOpen }`. Başlık "Son cevapta modele giden parçalar"; `.sub` `"${shortenText(question, 60)}" · ${strategyLabel(sources)}`; her parça `<button>`: `.n` `[n]` (+ " ✓" `citations.includes(n)` ise), `.t` `s. {pageNumber} · {heading} · #{chunkNo}`, `small` `RERANK`'te `rerank {formatDecimal(rerankScore, 2)} · d {formatDecimal(distance, 3)}`, diğerlerinde `d {…}`; tık → `onChunkOpen(chunkId)`. Açıklama "✓ atıf yapılan". Sonra `LatencyBars`.
- `components/LatencyBars.tsx` (lab `.lat`): props `{ done: DoneEvent }`; başlık `Süre · {formatDuration(latencyMs)}` + `hostLabel` varsa ` · {hostLabel}`; `latencyRows` her satır: etiket, çubuk (genişlik `max(1, ms / latencyMs × 100)%`, inline style; model satırı `--s-accent`), `formatDuration(ms)`.
- `index.css`: lab `--s-*` değerlerinden eksik token'lar (telefon çerçevesi, ekran zemini, balon kenarları, `bad` kenarı, `brand-soft`, `brand-d`, `accent`, `green`, `muted`, `line`); component CSS'inde ham hex yok.

### `web/CLAUDE.md` (frontend-coder)
Durum yorumu (TASK-008 Önizleme); `shared/api/eventStream.ts` "Henüz olmayanlar"dan çıkar; klasör yapısında `features/preview` içeriği.

## Test senaryoları
Backend: sahte `ChatModel` (`@MockitoBean` ya da elle yazılmış; `stream` sabit `ChatResponse` dizisi döner, hata için `Flux.error`, iptal için `doOnCancel` bayrağı), sahte `RetrievalClient`/`RerankClient` (fixture'lar `SearchHitFixtures.hits(20)`: `chunkId` 101..120, `distance` 0.30 + 0.01 × sıra), kaydeden sahte emitter (`RecordingSseEmitter`: gönderilen olay adı + JSON'u listeler; isteğe bağlı n. gönderimde `IOException`). Gerçek modele ve retrieval-api'ye gidilmez. Frontend: MSW; SSE gövdesi `src/test/sse.ts` (`sseResponse(events)` + parça parça yazılabilen akış `controllableSseStream()`); hatalar `problem(status, errorCode)`.

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Alıntı aralığı ve sırası | unit `CitationParserTest` | `givenAnswerWithRepeatedAndOutOfRangeNumbers_whenParseCitations_thenInRangeUniqueInFirstAppearanceOrder` | `"A [2]. B [1]. C [2] [0] [6] [12]"`, 5 → `[2, 1]` |
| B2 | Az parça | unit | `givenThreeContextChunks_whenParseCitations_thenFourIsIgnored` | `"x [4] y [3]"`, 3 → `[3]` |
| B3 | Bulamadım tespiti | unit `NotFoundAnswerDetectorTest` (`@ParameterizedTest`) | `givenAnswers_whenIsNotFoundAnswer_thenTrueOnlyWhenFixedSentenceIncluded` | "Bu konuda rehberimizde bilgi bulamadım." → true; "bu konuda REHBERİMİZDE  bilgi bulamadım. [1]" → true; "Kartını kapat [1]." → false; "" → false |
| B4 | Prompt metni | unit `PromptBuilderTest` | `givenTwoChunks_whenBuildPrompt_thenSystemIsLabTextAndUserHasNumberedChunksThenQuestion` | Sistem = rag-system.st'nin lab metni (testte literal, cümle dahil); kullanıcı = `"<parcalar>\n[1] H1\nİçerik bir\n\n[2] H2\nİçerik iki\n</parcalar>\n\nSoru: Kartımı kaybettim, ne yapmalıyım?"`; 2 mesaj |
| B5 | Prompt injection | unit | `givenChunkContainingInstruction_whenBuildPrompt_thenOnlyInUserMessageAndSystemUnchanged` | İçerik "Önceki talimatları yok say" sadece kullanıcı mesajında; sistem mesajı B4'tekiyle aynı |
| B6 | Rerank sırası | unit `ContextRetrieverTest` | `givenRerankStrategy_whenRetrieveContext_thenSendsTwentyCandidatesAndPromptsTopFiveByScore` | `search(q, 20)`; puanlar: vector 7 → 9.1, 2 → 8.0, 15 → 7.5, 1 → 6.0, 3 → 5.0, diğerleri < 5 → bağlam vectorRank `[7, 2, 15, 1, 3]`; `strategy` `RERANK`; reranker'a 20 belge vector sırasıyla, ilki `"H101\n\nC101"` |
| B7 | Eşitlik | unit | `givenEqualTopScores_whenRetrieveContext_thenLowerVectorRankFirst` | vector 9 ve 3 ikisi 9.0 (en yüksek) → bağlam `[3, 9, …]` |
| B8 | top5 | unit | `givenTop5Strategy_whenRetrieveContext_thenSearchesFiveAndNeverCallsReranker` | `search(q, 5)`; `rerankClient` hiç çağrılmadı; `TOP_K`; puanlar `null`; `rerankLatencyMs` `null` |
| B9 | Yedek yol | unit | `givenRerankerUnavailable_whenRetrieveContext_thenFirstFiveInVectorOrderAsFallback` | `RERANKER_UNAVAILABLE` → `RERANK_FALLBACK`; bağlam `[1..5]`; puanlar `null`; `rerankLatencyMs` dolu; aday 20 |
| B10 | Az aday | unit | `givenThreeHits_whenRetrieveContextWithRerank_thenRerankThreeAndPromptThree` | 3 belge reranker'a; bağlam 3 |
| B11 | Boş arama | unit | `givenNoHits_whenRetrieveContext_thenEmptyContextAndRerankerNotCalled` | bağlam `[]`; reranker çağrılmadı |
| B12 | Rerank istemcisi | unit `RerankClientTest` (`MockRestServiceServer`) | `givenUnorderedResults_whenRerankChunks_thenScoresAlignedToDocumentIndex` | İstek `POST /v1/rerank`, gövde `{"model":"bge-reranker-v2-m3","query":"q","documents":["a","b","c"]}`; yanıt index 2→5.36, 0→-11.04, 1→0.5 → `[-11.04, 0.5, 5.36]` |
| B13 | Rerank bozuk/hata/zaman aşımı | unit (+ JDK `HttpServer`) | `givenMissingResult_whenRerankChunks_thenRerankerUnavailable` / `givenDuplicateIndex_…` / `givenHttp500_…` / `givenRerankerSlowerThanReadTimeout_whenRerankChunks_thenThrowsRerankerUnavailable` | Hepsi `ServiceUnavailableException`, `RERANKER_UNAVAILABLE`; zaman aşımı testinde okuma süresi 200 ms, sunucu 2 sn bekler; test 2 sn dolmadan biter |
| B14 | Arama istemcisi | unit `RetrievalClientTest` | `givenHits_whenSearch_thenPostsQueryAndTopKToWorkspace42` / `givenRetrievalReturns503Or409OrRefuses_whenSearch_thenRetrievalUnavailable` | `POST /api/workspaces/42/search` `{"query":"q","topK":20}`; 9 alan okunur; hatalar `RETRIEVAL_UNAVAILABLE` |
| B15 | Olay sırası | unit `AnswerStreamerTest` | `givenModelStreamsThreeChunks_whenStreamModelAnswer_thenTokensThenSourcesThenDoneAfterTurnRecorded` | Olaylar: `token` "Kartını ", `token` "kapat [1]", `token` "."; `sources` (`citations` [1], 5 parça, `n` 1..5); `done` (`turnId` 12, `promptTokens` 2154, `completionTokens` 61, `notFoundAnswer` false). `InOrder`: `recordTurn` → `sources` gönderimi |
| B16 | Akış ortasında hata | unit | `givenModelFailsAfterFirstChunk_whenStreamModelAnswer_thenErrorEventAndTurnRecordedWithPartialAnswer` | `token` "Kartını " → `error` `CHAT_MODEL_UNAVAILABLE`; `sources`/`done` yok; kayıt `answer` "Kartını ", `errorCode` `CHAT_MODEL_UNAVAILABLE` |
| B17 | İlk token'dan önce hata | unit | `givenModelRefusesConnection_whenStreamModelAnswer_thenOnlyErrorEvent` | Tek olay `error` `CHAT_MODEL_UNAVAILABLE`; kayıt `answer` `null`, `promptTokens` `null` |
| B18 | İptal | unit | `givenClientDisconnectsOnSecondToken_whenStreamModelAnswer_thenModelSubscriptionCancelledAndTurnCancelled` | 2. `send` `IOException` → model akışında iptal bayrağı true; başka gönderim yok; kayıt `STREAM_CANCELLED`, `answer` "Kartını "; `registry.finish` 1 kez |
| B19 | İlk token zaman aşımı | unit | `givenModelSilentLongerThanFirstTokenTimeout_whenStreamModelAnswer_thenChatModelUnavailable` | `first-token-timeout` 100 ms, model hiç yayınlamıyor → `error` `CHAT_MODEL_UNAVAILABLE` (emitter tamamlanmasını en fazla 5 sn bekler) |
| B20 | Boş bilgi bankası | unit `AnswerServiceTest` | `givenNoSearchableChunks_whenSendMessage_thenFixedNotFoundAnswerWithoutModelCall` | `chatModel` hiç çağrılmadı; olaylar `token` "Bu konuda rehberimizde bilgi bulamadım." → `sources` (`contextChunks` [], `citations` []) → `done` (`notFoundAnswer` true, `promptTokens` null, `generationLatencyMs` null) |
| B21 | Modelden bulamadım | unit `AnswerStreamerTest` | `givenModelWritesNotFoundSentence_whenStreamModelAnswer_thenTurnAndDoneMarkedNotFound` | `done.notFoundAnswer` true; kayıt `notFoundAnswer` true |
| B22 | retrieval kapalı | unit `AnswerServiceTest` | `givenRetrievalUnavailable_whenSendMessage_thenThrows503AndRecordsTurnWithErrorCode` | `ServiceUnavailableException` `RETRIEVAL_UNAVAILABLE`; `recordTurn` 1 kez (`errorCode` aynı, `answer` null); model çağrılmadı; aynı konuşmaya sonraki istek 409 almaz |
| B23 | Eşzamanlı soru | unit | `givenTurnInProgress_whenSendMessage_thenThrowsTurnInProgressAndDoesNotSearch` | `ConflictException` `TURN_IN_PROGRESS`; `retrievalClient` etkileşimsiz; `recordTurn` yok |
| B24 | Bilinmeyen konuşma | unit | `givenUnknownConversation_whenSendMessage_thenThrowsConversationNotFound` | `NotFoundException` `CONVERSATION_NOT_FOUND`; registry, retrieval, model etkileşimsiz |
| B25 | Tur kaydı | integration `TurnServiceIntegrationTest` (Testcontainers) | `givenRerankContext_whenRecordTurn_thenTurnRowAndTwentyCandidateRows` | `conversation_turns`: `turn_no` 1, `question` = `search_query`, `generation_mode` RAG, `retrieval_strategy` RERANK, `candidates` 20, `top_k` 5, `chat_model` qwen3.5:9b, `cited` {1,3}, token ve süreler verilen değerler, `error_code` null. `turn_chunks` 20 satır, `vector_rank` 1..20, `prompt_position` sadece 5 satırda 1..5 (vector 7'nin satırında 1), `rerank_score` 20'sinde dolu, `source_id` 10001, `version` 2026.10 |
| B26 | İkinci tur | integration | `givenConversationWithOneTurn_whenRecordTurn_thenTurnNoIsTwo` | `turn_no` 2 |
| B27 | Konuşma açma | controller `ConversationControllerTest` (`@WebMvcTest`) | `givenPreviewChannel_whenCreateConversation_thenReturns201WithLocation` / `givenMissingChannel_whenCreateConversation_thenReturns400OnChannel` / `givenUnknownChannelWeb_whenCreateConversation_thenReturns400BadRequest` | 201, `Location` `/api/assistant/conversations/1`, `channel` PREVIEW; 400 `VALIDATION_FAILED` `violations[0].field` `channel`; 400 `BAD_REQUEST` |
| B28 | SSE uçtan uca (sahte model) | integration `SendMessageIntegrationTest` (`@SpringBootTest`, Testcontainers, `MockMvcTester` async, sahte `ChatModel`/`RetrievalClient`/`RerankClient`) | `givenFakeModel_whenSendMessage_thenEventStreamWithTokenSourcesDone` | 200, `Content-Type` `text/event-stream`, `Cache-Control` `no-cache`, `X-Accel-Buffering` `no`; gövdede olay adları sırasıyla `token, token, sources, done`; `done.turnId` veritabanındaki turun id'si |
| B29 | Soru doğrulama | integration (aynı sınıf) | `givenBlankText_whenSendMessage_thenReturns400` / `given1001Chars_…_thenReturns400` / `given1000Chars_…_thenStreams` | `"   "` → `text` `must not be blank`; 1001 → `size must be at most 1000 characters`; 1000 → 200; ilk ikisinde tur yok |
| B30 | Akış ucunda Problem Details | integration | `givenUnknownConversationAndSseAccept_whenSendMessage_thenProblemJson404` / `givenRetrievalDown_whenSendMessage_thenProblemJson503` | `Accept: text/event-stream, application/problem+json` → 404 `application/problem+json` `CONVERSATION_NOT_FOUND`; 503 `RETRIEVAL_UNAVAILABLE` + tur kaydı |
| B31 | Log sınırı | integration (`OutputCaptureExtension`) | `givenAnsweredTurn_whenLogsCaptured_thenIdsAndCountsButNoQuestionAnswerOrChunkText` | Logda `Turn completed conversationId=`, `Reranked candidates=20`; soru metni, cevap metni, parça içeriği ("C101") ve sistem talimatından cümle yok |
| B32 | Strateji ayarı | unit `AssistantPropertiesTest` (`ApplicationContextRunner`) | `givenDefaults_whenBound_thenRerank20To5` / `givenStrategyFoo_whenBound_thenContextFails` / `givenTopKGreaterThanCandidates_whenBound_thenContextFails` | varsayılan `RERANK`, 20, 5; `foo` ve `top-k=21` açılışı düşürür |
| B33 | Modele giden prompt | unit `AnswerServiceTest` (`ArgumentCaptor<Prompt>`) | `givenRerankedContext_whenSendMessage_thenModelReceivesChunksInRerankOrder` | Kullanıcı mesajında `[1] H107` (vector 7), `[2] H102`; soru stripped (`"  Kartımı kaybettim  "` → `Soru: Kartımı kaybettim`) |
| B34 | Model seçenekleri | integration (`@SpringBootTest`) | `givenDefaultProperties_whenContextStarts_thenOllamaOptionsMatchLab` | Varsayılan seçenekler: model `qwen3.5:9b`, `temperature` 0.0, `numCtx` 8192, `think` false. Ollama kapalıyken context açılır (`/actuator/health` UP) |
| B35 | Transaction dışında model | unit `AnswerStreamerTest` | `givenStreaming_whenModelCalled_thenNoTransactionActive` | Sahte model `stream` çağrısında `TransactionSynchronizationManager.isActualTransactionActive()` false |
| F1 | Bölünmüş olay | unit `eventStream.test.ts` | `givenEventSplitAcrossChunks_whenParsed_thenEmitsSingleEvent` | `"event: tok"` + `"en\ndata: {\"text\":\"a\"}\n\n"` → 1 olay `token`, data `{"text":"a"}` |
| F2 | Çoklu olay, CRLF, yorum | unit | `givenTwoEventsInOneChunkWithCrlf_whenParsed_thenEmitsBothInOrder` | `token` sonra `done`; `:` satırı atlandı |
| F3 | Akış öncesi hata | unit | `givenProblem503_whenPostEventStream_thenThrowsApiErrorWithCode` / `givenFetchRejects_whenPostEventStream_thenNetworkError` | `ApiError` `RETRIEVAL_UNAVAILABLE` (status 503); `NETWORK_ERROR`; istek başlığı `Accept` `text/event-stream, application/problem+json` |
| F4 | Olay daraltma | unit `answerStreamEvents.test.ts` | `givenUnknownOrMalformedEvent_whenToAnswerStreamEvent_thenNull` | `event: ping` → null; `token` `{"text":5}` → null; bozuk JSON → null; geçerli `sources` → tipli olay |
| F5 | Alıntı biçimi | unit `answerFormat.test.ts` | `givenInAndOutOfRangeNumbers_whenFormatAnswerAfterDone_thenOnlyInRangeAreCitations` | "Kartını kapat [1]. Bak [7]." , 5 → `citation` 1; "[7]" metin. `contextSize` null → hiç `citation` |
| F6 | Kalın ve madde | unit | `givenBoldAndBulletLines_whenFormatAnswer_thenBoldInlineAndListBlock` | "**Önemli:** kapat [1]\n- bir [2]\n- iki" → paragraf (`bold` "Önemli:" + metin + `citation` 1), liste 2 madde (ilkinde `citation` 2) |
| F7 | Kaynak düğmeleri | unit `citations.test.ts` | `givenCitationsOnTwoChunksOfSamePage_whenCitedChunks_thenTwoButtonsInCitationOrder` | `citations` [3, 1] → [chunk3, chunk1]; etiket "Raşit Bankası Ürün ve H… · s. 19" (24 karakter: 23 + "…") |
| F8 | Akan cevap | component `PreviewPage.test.tsx` | `givenStreamedTokens_whenQuestionSent_thenAnswerGrowsBeforeDone` | Karşılama metni görünür; "Kartını " görünür, `done` gelmeden "Gönder" pasif; `done` sonrası tam metin, "Gönder" (metin yazılınca) etkin |
| F9 | Akışta kilit | component | `givenStreamInProgress_whenRendered_thenInputSendAndNewConversationDisabled` | `Soru` kutusu, "Gönder", "Yeni konuşma" `disabled` |
| F10 | Konuşma bir kez açılır | component | `givenTwoQuestions_whenSent_thenOneConversationCreatedAndBothMessagesUseIt` | `POST /api/assistant/conversations` 1 kez; iki `POST …/conversations/1/messages`; ilk istekte gövde `{"channel":"PREVIEW"}`, mesaj gövdesi `{"text":"Kartımı kaybettim, ne yapmalıyım?"}` (baştaki/sondaki boşluk silinmiş) |
| F11 | Yeni konuşma | component | `givenAnsweredTurn_whenNewConversationThenQuestion_thenNewConversationAndSeparator` | 2. `POST /conversations` → id 2; eski soru ayracın üstünde; ayraç "Yeni konuşma · 2" |
| F12 | Alıntı → pencere | component | `givenDoneAnswer_whenCitationClicked_thenChunkDialogOpensAndFocusReturns` | "Kaynak 1" düğmesi → `GET /api/workspaces/42/chunks/151` → pencere başlığı "2.16 Kayıp ya da çalıntı bildirimi"; Esc → odak "Kaynak 1"de |
| F13 | Kaynak düğmeleri ve bulamadım | component | `givenCitedAnswer_whenDone_thenSourceButtonsShown` / `givenNotFoundAnswer_whenDone_thenNoSourceButtons` | "Raşit Bankası Ürün ve H… · s. 19" düğmesi; `notFoundAnswer` true → kaynak düğmesi yok |
| F14 | Boş bilgi bankası ipucu | component | `givenEmptyContext_whenDone_thenShowsAddDocumentHint` | "Bilgi bankasında henüz belge yok." + `/kaynaklar` bağlantısı |
| F15 | Akış öncesi 503 | component | `givenRetrievalUnavailable_whenQuestionSent_thenAlertAndInputEnabled` | `role="alert"` "Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene."; kutu etkin |
| F16 | Akış içi hata | component | `givenErrorEventAfterToken_whenStreaming_thenKeepsPartialAnswerAndShowsModelMessage` | "Kartını " duruyor; "Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene."; kaynak düğmesi yok |
| F17 | done'suz bitiş | component | `givenStreamEndsWithoutDone_whenStreaming_thenInterruptedMessage` | "Cevap yarıda kesildi. Soruyu yeniden gönder." |
| F18 | Ayrılınca iptal | component | `givenStreamInProgress_whenPageUnmounted_thenRequestAborted` | MSW handler'ındaki `request.signal.aborted` true |
| F19 | Kaybolan konuşma | component | `givenConversationNotFound_whenQuestionSent_thenMessageAndNextQuestionOpensNewConversation` | "Konuşma bulunamadı. Yeni konuşma başlat."; sonraki soruda yeni `POST /conversations` |
| F20 | Yan kart | component | `givenNoAnswerYet_whenRendered_thenHowItWorksCard` / `givenRerankDone_whenRendered_thenContextCardWithScoresAndChecks` | Önce "Asistan nasıl cevaplıyor?"; sonra "Son cevapta modele giden parçalar", alt başlık `"Kartımı kaybettim, ne yapmalıyım?" · rerank 20→5`, 5 satır, `[1] ✓`, "s. 19 · 2.16 Kayıp ya da çalıntı bildirimi · #31", "rerank 5,36 · d 0,312"; "✓ atıf yapılan"; "sarı" metni yok |
| F21 | Strateji etiketleri | component | `givenTopKDone_whenRendered_thenTop5AndDistanceOnly` / `givenFallbackDone_whenRendered_thenRerankerDownLabel` | "· top-5", satırda sadece "d 0,312"; "· reranker DOWN · vector sırası" |
| F22 | Süre çubukları | component | `givenRerankDone_whenRendered_thenLatencyRowsAndHostLabel` / `givenTopKDoneWithoutHostLabel_whenRendered_thenNoRerankRowNoHost` | "Süre · 4,73 sn · Apple M5 · 16 GB"; satırlar "retrieval-api araması 84 ms", "rerank 712 ms", "qwen3.5 cevabı 3,92 sn"; ikinci durumda "rerank" satırı ve "Apple" yok |
| F23 | Kart satırı → pencere | component | `givenContextRow_whenClicked_thenChunkDialogOpens` | 3. satır → `GET …/chunks/{3. chunkId}` |
| F24 | Mesajlar | unit `errorMessages.test.ts` | `givenTask008ErrorCodes_whenMessageFor_thenCardText` | 5 yeni kod plandaki metni döner; `INTERNAL_ERROR` → fallback |
| F25 | Girdi kuralı | component `MessageComposer.test.tsx` | `givenWhitespaceOnly_whenTyped_thenSendDisabled` / `givenPasted1005Chars_whenPasted_thenInputKeeps1000` | "   " → "Gönder" pasif, istek yok; değer uzunluğu 1000 |
| F26 | Erişilebilirlik | component | `givenAnsweredPreview_whenRendered_thenHasNoAxeViolations` | `toHaveNoViolations` |
| F27 | HTML güvenliği | component | `givenHtmlInAnswerToken_whenRendered_thenShownAsText` | `<img src=x onerror=alert(1)>` metin olarak; `img` yok |
| F28 | Duyuru | component | `givenDoneEvent_whenStreamFinishes_thenLiveRegionAnnouncesCompletion` | `aria-live="polite"` bölgede "Cevap tamamlandı."; `error` olayında bu bölge hata metnini içermez (metin sadece `role="alert"`'te, bir kez) |
| E1 | Rehber sorusu (gerçek sistem) | e2e `web/e2e/preview.spec.ts` | `givenGuideIngested_whenLostCardQuestionAsked_thenStreamedAnswerWithCitationOpensChunk` | `beforeAll`: kaynak listesi boşsa rehber API ile yüklenir ve `DONE` beklenir. "Kartımı kaybettim, ne yapmalıyım?" → 120 sn içinde "Gönder" yeniden etkin; ≥1 "Kaynak n" düğmesi; ≥1 "· s." düğmesi; yan kartta 5 satır; "Kaynak 1" → pencere açılır, Esc kapanır |
| E2 | Rehber dışı | e2e | `givenGuideIngested_whenBitcoinQuestionAsked_thenNotFoundSentenceAndNoSourceButtons` | Balonda "Bu konuda rehberimizde bilgi bulamadım."; "· s." düğmesi yok |

**Elle çalıştırılanlar:**
- **M0 (backend-coder, lokal, gerçek modeller):** `assistant-api/CLAUDE.md` "Komutlar"daki iki `curl` ile "Kartımı kaybettim, ne yapmalıyım?": token'lar parça parça gelir, `done`'da token sayıları dolu, cevapta düşünme metni yok (`think=false`). Sonuç raporda.
- **M1 (devops):** Lead aracılığıyla kullanıcı onayı alınırsa `down -v` + `up --build -d` (onay yoksa boş bilgi bankası denemesi geliştirme veritabanıyla yapılır). Boş bilgi bankasında soru → bulamadım + ipucu. Rehber 2026.10 yüklenir. `curl -sN` ile `localhost:3000` üzerinden akış: olaylar zaman damgalarıyla parça parça (tek seferde değil). Akış sürerken `curl` Ctrl+C → assistant-api logunda `Turn cancelled`, Ollama logunda istek erken kapanır; veritabanında `error_code = STREAM_CANCELLED`.
- **M2 (devops):** Reranker terminali Ctrl+C → aynı soru → `sources.retrievalStrategy` `RERANK_FALLBACK`, cevap akar, `WARN Reranker unavailable`; `conversation_turns.retrieval_strategy` `RERANK_FALLBACK`. Reranker yeniden açılır.
- **M3 (devops):** Ollama'ya ulaşılamayan override → ekranda "Dil modeline şu an ulaşılamıyor…"; `docker compose stop retrieval-api` → "Bilgi bankasına şu an ulaşılamıyor…"; iki tur da hata koduyla kayıtlı (`psql`).
- **M4 (qa):** Ekrandan 5 rehber sorusu (en az "Kartımı kaybettim, ne yapmalıyım?" ve "Ev kredisini erken kapatırsam ceza öder miyim?") + 2 rehber dışı ("Bitcoin alıp satabilir miyim?" + bir tane daha): cevap, alıntılar, kaynak düğmeleri, süreler tabloda.
- **M5 (qa):** 5 soruda `turn_chunks`'tan vector sırası ile rerank sonrası prompt sırası tablosu (top-5 = vector 1-5; rerank = `prompt_position`); "erken ödeme" parçası vector 7 → prompt 1 tutuyor mu, gerçek değerle. Bir soru `RETRIEVAL_STRATEGY=top5` ile de sorulur; reranker çağrılmadığı logdan görülür.
- **M6 (qa):** Lab'ın sol yarısıyla yan yana: telefon, balonlar, alıntı ve kaynak düğmeleri, yazıyor göstergesi, yan kartlar, süre çubukları; 360 px genişlik. Farklar karttaki kararlarla sınırlı.
- **M7 (qa):** `sse.md` ile gerçek akışın (olay adları, alanlar, sıra) ve `openapi.yaml` ile hata yanıtlarının elle karşılaştırması.

## Deployment planı
- `deployment/docker-compose.yml` (assistant-api): `RETRIEVAL_API_URL: http://retrieval-api:8080`, `RETRIEVAL_STRATEGY: ${RETRIEVAL_STRATEGY:-rerank}`, `ASSISTANT_HOST_LABEL: ${ASSISTANT_HOST_LABEL:-}` (kart soru 3; değer kullanıcının kabuğundan ya da `deployment/.env`'den, dosya repoya girmez). Mevcut `OLLAMA_BASE_URL`, `CHAT_MODEL`, `RERANKER_URL`, `RERANKER_MODEL` artık okunur.
- `web/nginx.conf` `location /api/assistant/`: `proxy_http_version 1.1`, `proxy_set_header Connection ''`, `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 300s` (emitter süresiyle aynı). Güvenlik başlıkları kalır.
- `ollama.md`: §8 tablosunda assistant-api değişkenleri "TASK-008 (okunuyor)" + `RETRIEVAL_API_URL`, `RETRIEVAL_STRATEGY`, `ASSISTANT_HOST_LABEL`; yeni bölüm "Cevap akışı duman testi" (iki `curl`, beklenen olay sırası); reranker'ı kapatıp yedek yolu görme; reranker okuma zaman aşımı notu.
- `deployment/CLAUDE.md`: "Gerçek yapı (TASK-008)": SSE nginx ayarları, yeni değişkenler, stratejiyi değiştirme (`RETRIEVAL_STRATEGY=top5 docker compose … up -d assistant-api`), M2/M3 denemeleri.
- Kök `README.md`: Önizleme'nin çalışması için sıra (Ollama → reranker → Compose → belge yükle).
- Lokal model: yeni model yok (`qwen3.5:9b`, `bge-reranker-v2-m3` TASK-003'te indirildi).

## Açık sorular ve riskler
1. **Kart soru 1 (Spring AI):** cevap gelene kadar backend-coder dependency eklemez. "Hayır" gelirse dil modeli istemcisi retrieval-api'deki gibi `RestClient` ile Ollama `/api/chat` NDJSON akışını okuyan bir `ChatModelClient` olur; plan yeniden yazılır (B15-B19 ve B34 değişir).
2. **Kart soru 2-5'in etkisi:** soru 2 → `SOURCE_NAME_SHORT_LENGTH` ve `sourceButtonLabel` (F7, F13); soru 3 → `assistant.host-label` + `DoneEvent.hostLabel` (A seçilirse alan sözleşmeden çıkar, F22 değişir); soru 4 → `AnswerMessage` ipucu (F14); soru 5 → `assistant.reranker.read-timeout` (B13 değişmez).
3. **Risk – bellek ve süre:** 16 GB makinede üç model birlikte; soğuk ilk cevap ~10 sn, ilk token zaman aşımı 60 sn.
4. **Risk – tek kopya:** `ActiveTurnRegistry` bellektedir; assistant-api'nin iki kopyası çalışırsa aynı konuşmaya eşzamanlı soru engellenmez (bu projede tek kopya).
5. **Risk – "erken ödeme" senaryosu:** TASK'taki "vector 7 → prompt 1" lab kaydından gelir; gerçek rehber (70 sayfa, 154 parça) ve gerçek reranker'la farklı çıkabilir. qa gerçek değeri raporlar, test bunu sabitlemez.
6. **Kapsam dışı not:** `assistant-api/CLAUDE.md`'deki lab anahtarları (`assistant.generation` = `rag`/`llm-only`) TASK-011'in `rag`/`llm` değeriyle ayrışıyor; TASK-011'de kesinleşir.

STATUS: DONE
