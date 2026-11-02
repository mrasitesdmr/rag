TASK: TASK-011
# QA raporu

## Kullanılan skill'ler
code-review-and-quality, rag-pipeline, sse-streaming, e2e-testing, writing-good-tests (mutation yorumu), springboot-verification.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| K1 | Mod `assistant.generation.mode` rag/llm, varsayılan rag | `AssistantProperties.Generation.configuredMode()`; `AssistantPropertiesTest` (B1). Canlı: varsayılan `generationMode=RAG`, `GENERATION_MODE=llm` → `LLM_ONLY` | ✅ |
| K2 | LLM'de lab'daki kısa talimat, parça yok, alıntı yok, "Rehber · s. N" yok | `prompts/llm-only-system.st`, `PromptBuilder.buildLlmOnlyPrompt`. Canlı: `promptTokens` 56-63, `contextChunks 0`, `citations []`; tarayıcıda "Rehber · s." ve "Kaynak" düğmesi sayısı 0 | ✅ |
| K3 | Uç: model, embedding+boyut, mod, strateji, yeniden yazma, bilgi bankası | `GET /api/assistant/live-config` canlı JSON, `openapi.yaml` `LiveConfigResponse` ile alan alan aynı | ✅ |
| K4 | Şerit metinleri/renkleri | Tarayıcı (LLM modu): `Retrieval kapalı (Sadece LLM) [danger]`, `Soru yeniden yazma açık [normal]`; rag: `rerank 20→5` (E1 + `liveConfigItems.test.ts`) | ✅ |
| K5 | Reranker DOWN ölçüme dayanır (soru 2 = C) | `RerankerStatusTracker`, `RerankerHealthChecker` (15 sn), `ContextRetriever` bildirimi; birim testleri geçti. Canlı DOWN/UP denemesi (M3, aşağıda) | ✅ |
| K6 | "Asistan nasıl cevaplıyor?" moda göre | `howItWorksText`; tarayıcıda LLM varyantı görüldü, RAG varyantı birim testte | ✅ |
| K7 | LLM turu stratejisiz/parçasız kayıt | DB: turn 99/100/102/104/105 `LLM_ONLY`, `NONE`, `search_query`/`candidates`/`top_k`/`retrieval_latency_ms` NULL, `cited {}`, `turn_chunks` 0 satır, `DISABLED` | ✅ |
| K8 | Ekrandan değişmez | Uç sadece GET, web'de düğme yok; mod `GENERATION_MODE` + yeniden başlatma ile değişti | ✅ |
| K9 | Rozet ipucu | `AdminLayout.test.tsx` (F9), değişmedi | ✅ |
| Kart 1 = A | Bilgi bankası "Bilgi kaynakları", çoklu "N belge" | Tarayıcı: `Bilgi kaynakları 4 belge [normal]` | ✅ |
| Kart 3 | LLM'de yeniden yazma yok, geçmiş prompt'a girer | Canlı takip sorusu "Peki Platinum?" → `DISABLED`, `promptTokens` 60 → 98 (geçmiş girdi) | ✅ |
| AC1 | İki mod gerçek modelle, yan yana karşılaştırma | Aşağıdaki tablo | ✅ |
| AC2 | Web'de sabit model adı yok | `grep -rnE "qwen\|bge-\|llama\|nomic" web/src --include='*.ts' --include='*.tsx'` (test hariç) → boş | ✅ |
| AC3 | Testler/build geçer | Komutlar tablosu | ✅ |

### AC1: aynı sorular, iki mod (qwen3.5:9b, her soru yeni konuşma)
| Soru | RAG (rerank 20→5) | Sadece LLM |
|---|---|---|
| Kartımı kaybettim, ne yapmalıyım? | "Kartınızı vakit kaybetmeden … Raşit Mobil, İnternet Şubesi, Müşteri Hizmetleri veya şube [1]. … bildirime kadar 24 saat içindeki onaysız işlemlerde sorumluluk 250 TL'dir [1]. Yeni kartınız … kuryeyle gönderilir [1]." Alıntı [1] (s. 18, "Kartlar · Sık sorulanlar"), 5 parça, 6,8 sn | "Hemen **720 10 00** numaralı çağrı merkezimizi arayarak kartınızı bloke ettirin." Uydurma numara, alıntı yok, 0 parça, 9,2 sn (soğuk) |
| Gold kartın yıllık aidatı ne kadar? | "Raşit Gold'un yıllık üyelik ücreti **1.450 TL**'dir [1]. Önceki 12 ayda … 150.000 TL alışveriş yaptıysanız ücret alınmaz [1]." 7,9 sn | "Gold Kart'ın yıllık aidatı **1.500 TL**'dir." (yanlış rakam, alıntısız) 1,1 sn |
| FAST ile en fazla ne kadar gönderebilirim? | "FAST ile tek seferde en fazla 100.000 TL gönderilebilir [5]." [5] = s. 38 "5.3 FAST ve Kolay Adres" | "FAST ile **günlük** 100.000 TL'ye kadar …" (rakam tesadüfen aynı, kapsam farklı) 1,0 sn |
| Bitcoin alabilir miyim? (rehberde yok) | "Bu konuda rehberimizde bilgi bulamadım." `notFoundAnswer=true`, alıntı yok | "Hayır, Raşit Bankası kripto … desteklemez. Sadece TL ve Yabancı Para hesapları açabilirsiniz." Uydurma, `notFoundAnswer=false` |

Sonuç: RAG cevapları rehber parçalarına dayanıyor ve alıntı doğru sayfayı gösteriyor; rehberde olmayan soruda sabit cümle yazılıyor. Sadece LLM cevapları akıcı ama rehberle tutarsız/uydurma (aidat 1.500 vs rehber 1.450, uydurma telefon). Beklenen davranış bu; mod farkı ekranda ve kayıtta görünüyor.

## Sözleşme uyumu
- `openapi.yaml` `LiveConfigResponse` ↔ `LiveConfigResponse` record ↔ web `shared/liveConfig/types.ts`: alan adları, sıra ve enum değerleri aynı (canlı JSON ile doğrulandı).
- `sse.md` ↔ backend: RAG `sources` ilk alan `generationMode: RAG`; LLM modunda `sources` = `{generationMode:LLM_ONLY, retrievalStrategy:NONE, candidates:null, topK:null, searchQuery:null, queryRewriteStatus:DISABLED, contextChunks:[], citations:[]}`, `done.retrievalLatencyMs/rerankLatencyMs: null`, `notFoundAnswer:false`. Olay sırası `token`* → `sources` → `done`. Web `answerStreamEvents` aynı alanları ayrıştırıyor (423 web testi + E2E).
- assistant-api → retrieval-api: `getWorkspace`/`listSources` canlı çalışıyor (embedding bge-m3/1024, 4 kaynak).

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | exit 0, 227 test, 0 hata/başarısız, JaCoCo eşiği geçti | evet |
| `cd web && npm run lint` | exit 0 | evet |
| `cd web && npm run build` | exit 0 | evet |
| `cd web && npm test -- --run` | 24 dosya, 423 test geçti | evet |
| `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | 14 geçti, 1 atlandı (boş bilgi bankası senaryosu, bilinen koşullu atlama), E1 dahil geçti | evet |

## Mutation testi
PIT (assistant-api, TASK-011 sınıfları: `liveconfig.*`, `RerankerStatusTracker`, `RerankerHealthChecker`, `PromptBuilder`, `AnswerService`): 54 mutant, 50 öldü (%93), test gücü %93, satır kapsamı %99. Hayatta kalanlar: `PromptBuilder` satır 52 ve 60 (üç aritmetik mutant; liste kapasitesi ipucu, davranışı etkilemez, eşdeğer) ve `RerankerStatusTracker.record` satır 49 (koşul negasyonu; durum değişimi log koşulu, davranış testi dışında). Önemli kaçırılmış davranış yok. Web: Stryker kurulu değil.

## Uçtan uca
- E1 ve diğer Playwright testleri: yukarıda.
- M2: `GENERATION_MODE=llm docker compose … up -d assistant-api` → şerit `Retrieval kapalı (Sadece LLM)` (danger), kart LLM varyantı, cevapta "Rehber · s." / "Kaynak" düğmesi 0, parça kartı yok. Değişkensiz aynı komutla `rag`'e dönüldü (live-config `RAG`, `rerankerStatus UP`).
- M4: Bilgi bankasında şu an 4 kaynak olduğundan şerit `4 belge`. "Belge yok" ve tek belge adı dallarını canlıda denemedim (veri silmek kapsamım dışı); `liveConfigItems.test.ts` (F1) ve `LiveConfigService` testleri (B11) kapsıyor.
- M3 (reranker DOWN, S4): Reranker'ı kullanıcı durdurdu (qa'nın `kill` komutu izin sınıflandırıcısınca reddedilmişti, aşılmadı). Durdurulan komut (ps): `llama-server -hf gpustack/bge-reranker-v2-m3-GGUF:Q8_0 --reranking --host 127.0.0.1 --port 8081 --ctx-size 8192 --batch-size 2048 --ubatch-size 2048 --n-gpu-layers 99 --offline`; lead aynı komut ve cwd ile nohup ile yeniden başlattı (PID 4886).
  - Soru sormadan: assistant-api logu 10:33:52 `Reranker status changed from=UP to=DOWN` (15 sn'lik sağlık kontrolü). Sayfa açıldığında şerit `Retrieval reranker DOWN [warning]`, 64 sn izlemede değişmedi; kart top-k varyantı ("en yakın 5 parça qwen3.5:9b modeline gider").
  - Soru sırasında: "Gold kartın yıllık aidatı ne kadar?" → `retrievalStrategy RERANK_FALLBACK`, `rerankLatencyMs 2`, cevap normal aktı (1.450 TL [1]), şerit DOWN kaldı.
  - Geri dönüş: `/health` 200; log 10:36:52 `from=DOWN to=UP`; şerit `Retrieval rerank 20→5 [normal]`, kart rerank varyantı; yeni soru `RERANK`. Reranker çalışır halde. Ollama'ya dokunulmadı.
  - Not: "soru sormadan ≤30 sn" için durdurma anı kullanıcıdan bana ayrı bildirilmedi; log, sağlık kontrolünün durumu 15 sn aralıkla (DOWN 10:33:52, UP 10:36:52) ölçtüğünü gösteriyor. Aralık yapılandırması 15 sn, birim testleri (B15-B17) geçti.
- Sistem açık bırakıldı: localhost:3000, 4 servis healthy, mod `rag`, reranker UP.
- Yenileme hata verirse son değerlerin kalması (frontend raporu): TanStack Query varsayılanı; şerit kaybolmaz, bir sonraki başarılı yoklamada güncellenir. Kabul edilebilir (FYI): servis kapalıyken bile eski değer görünür ama 15 sn içinde düzelir; ilk yüklemede hata `ErrorAlert` gösterir (F7).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Optional | `web/e2e/sources.spec.ts` (yükleme akışı) | E2E koşusu her seferinde kalıcı "E2E Rehber …" kaynağı bırakıyor (10017, 10018, 10019). Bilgi bankası kirleniyor: aynı soruya RAG'de 5 parçadan 4'ü aynı metnin kopyası (kaynak 10017-10019), aday havuzu zayıflıyor, şerit "4 belge" | tester |
| FYI | şerit | LLM modunda "Soru yeniden yazma açık" görünüyor ama yeniden yazma yapılmıyor; kart/plan (`queryRewriteEnabled` yapılandırma değeri) gereği, lab ile uyumlu | — |
| FYI | `PromptBuilder` | Prompt token'ı LLM modunda 56-63; talimat lab metniyle aynı | — |

Critical/Required bulgu yok.

STATUS: DONE
