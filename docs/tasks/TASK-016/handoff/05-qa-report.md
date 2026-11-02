TASK: TASK-016
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Diğer skill'ler yüklenmedi; kurallar plan ve TASK.md'den kontrol edildi, tarayıcı denemeleri Playwright betikleriyle yapıldı.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test + kendi denemem) | Sonuç |
|---|---|---|---|
| K1 | Kural 2 kararı verilmiş | Onay kartı "Kullanıcının cevapları": 1-C (sayfa + cümle). Kod: `answerText` + `POST /chunk-matches` | ✅ |
| K2 | Golden set hazır, sürümlü, kullanıcıya gösterilmiş | `docs/rehber/golden/rehber-golden-v1.json`: 100 soru, planın kategori dağılımı birebir. Kartta "kullanıcıya tam liste olarak sunuldu, onaylandı" yazıyor. Yükleme `PUT /golden-set` ile yapıldı | ✅ |
| K3 | İki stratejiyle gerçek ölçüm | Aşağıdaki tablo (benim ölçümüm) | ✅ |
| K4 | recall@5 ve MRR bağımsız beklenen değerlerle testli; build'ler geçer | `RetrievalMetricsTest` (elle türetilmiş: 0.8/0.55, 0/0, 1/0.333); benim SQL kontrolüm ayrıca tutuyor (aşağıda); komutlar geçti | ✅ |
| K5 | Görünüm lab'ın sol yarısıyla aynı | Yapı, metinler ve CSS değerleri lab'la aynı (aşağıda) | ✅ |
| 3 | recall@5, MRR; ≥ 0,95 yeşil, altı kırmızı | Ekran: 0,98 yeşil (ev_6), 0,89 kırmızı (ev_5). `EvalKpis`, W5 | ✅ |
| 4a | Etkin yapılandırmayla ölçüm; düğme metni backend'den | Rerank'te "Ölçümü çalıştır · rerank 20→5"; `RETRIEVAL_STRATEGY=top5` ile ev_5 `TOP_K` ve ekranda "top-5" | ✅ |
| 4b | Sadece LLM'de düğme kapalı + uyarı | `GENERATION_MODE=llm`: düğme disabled, uyarı metni ekranda, `POST /evals` → 409 `EVAL_NOT_AVAILABLE_IN_LLM_ONLY` | ✅ |
| 4c | Reranker kapalıyken sonuç bunu kaydeder | Gerçek sistemde denenmedi (llama-server'ı durduramam, istenmedi). Kod: `rerankFallbackCount`, `is_rerank_fallback`; testler B13, W6, W13 geçti | ✅ (sadece testle) |
| 5a | 202 + polling | `POST /evals` → 202 + `Location: /api/assistant/evals/4`. Ekran sürerken "Ölçülüyor · 0/100 → 1/100 …" | ✅ |
| 5b | Sürerken düğme kapalı; ikinci çalıştırma 409 | Süren ölçümde düğme disabled. İkinci `POST` → 409. İkinci sekmede eski (etkin) düğmeye basınca "Bir ölçüm zaten sürüyor. Bitmesini bekle." | ✅ |
| 5c | Sayfa yenilense de süren ölçüm bulunur | Ölçüm sürerken `reload` → "Ölçülüyor · 1/100", düğme disabled | ✅ |
| 5d | Kayıt alanları | `GET /evals/{id}`: sürüm, strateji, `sourceVersions`, `questionCount`, soru başına `vectorRank`/`promptPosition`, metrikler, `durationMs`; `eval_run_questions` tablosu | ✅ |
| 6 | Sadece retrieval ölçülür | Ölçüm 5,2 sn (top-5) / 100 sn (rerank); `EvalRunRunner` sadece `ContextRetriever` çağırıyor; ölçümde yeni tur/konuşma oluşmadı | ✅ |
| 7a | Dört kutu ve alt yazılar | Ekran: "0,98 · 98/100 soru", "0,91 · doğru parçanın sırası", "2 · doğru parça prompt'ta yok", "rerank 20→5 · rehber 2026.10" | ✅ |
| 7b | Kaçanlar tablosu: "20+", "N. sıra"/"yok", en fazla 24 | Ekranda 24 satır (ev_6'da 36 uygun soru var), "1. sıra" yeşil, "yok" kırmızı. "20+" ve "yok" ev_6'da son satırlarda (sıra 70, 87) | ✅ |
| 7c | Satıra tıklayınca soru Önizleme'de sorulur | İlk satıra tıkladım → `/onizleme`; tam 1 `POST /conversations` + 1 `POST …/messages` (StrictMode'lu dev server); yenilemede tekrar gitmedi | ✅ |
| 7d | Ölçüm yokken "Henüz ölçüm yok…" | Gerçek sistemde denemedim (veritabanında ölçüm vardı). W4 testi geçti | ✅ (sadece testle) |
| 7e | Önceki çalıştırmalar saklanır | "Çalıştırmalar" tablosu ev_6 … ev_1; ev_3 "başarısız", süren satır "ölçülüyor…" | ✅ |
| 8 | Önizleme sarı + Konuşmalar "cevap burada" + KAÇIRDI / ÜRETİM HATASI | Aşağıda | ✅ |
| S3 | Sil + yeniden yükle → işaret geçerli | **Silme ucu yok** (`DELETE` openapi'de de yok; temiz kurulum `down -v` kullanıcı onayı ister). Yerine aynı PDF'i ikinci kaynak olarak ekrandan yükledim (154 yeni parça, yeni kimlikler) → ev_7: 100 ölçülen, 0 ölçülemeyen. Ayrıntı "Uçtan uca"da | ✅ (vekil senaryo) |

## Sözleşme uyumu
- `openapi.yaml` yolları ↔ controller'lar: `GET/PUT /api/assistant/golden-set`, `POST/GET /api/assistant/evals`, `/evals/overview`, `/evals/{evalRunId}`, `POST /api/workspaces/{id}/chunk-matches` birebir aynı. Gerçek çağrılarda status ve `errorCode`'lar sözleşmeyle aynı çıktı (202 + `Location`, 409 `EVAL_RUN_IN_PROGRESS`, 409 `EVAL_NOT_AVAILABLE_IN_LLM_ONLY`).
- Web `evaluationApi.ts` yolları ve tipleri sözleşmeyle aynı; ekran gerçek yanıtlarla sorunsuz çizildi.
- SSE: `sources.contextChunks[].containsAnswer` `sse.md` ile aynı; web ayrıştırıcı boolean olmayanı geçersiz sayıyor; gerçek akışta sarı işaret göründü.
- `TurnStatus` `MISSED` / `GENERATION_ERROR` ve `TurnChunkResponse.containsAnswer` gerçek yanıtlarda geldi.

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` | BUILD SUCCESS, 441 test, 0 hata, coverage eşiği geçti | Evet |
| `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` | BUILD SUCCESS, 344 test, 0 hata, coverage eşiği geçti | Evet |
| `cd web && npm run build` | Geçti | Evet |
| `cd web && npm run lint` | Geçti (0 uyarı) | Evet |
| `cd web && npm test` | 36 dosya, 620 test geçti | Evet |
| `cd web && npm run test:coverage` | Satır %97,32 (eşik %80) | Evet |
| `E2E_BASE_URL=http://localhost:5173 npx playwright test e2e/shell e2e/preview e2e/conversations` | 13 geçti, 1 kaldı: `shell.spec.ts` "`/api/yok` → 404 problem+json". Dev server (Vite) bilinmeyen yola `index.html` döner; bu test nginx'e (Docker sitesi, :3000) göre yazılmış. TASK-016'yla ilgisi yok | — |
| `sources.spec.ts` | **Çalıştırılmadı**: boş bilgi bankası ve yeni yükleme varsayıyor; dolu veritabanında anlamsız kalır | — |

## Mutation testi
- **PIT** (`assistant-api`, `evaluation.service.*`, `evaluation.*` testleriyle): 94 mutant, 87 öldü (%93), test gücü %96. Raporda tester'ın yazdığı %88/%91'den iyi (sonradan kapanan mutant'lar).
- Hayatta kalan 4 ve kapsamsız 3:
  - `GoldenSetService:105` (`contentHash` içindeki `index > 0` sınırı) ve `GoldenAnswerLocator:61` sınır mutant'ları: eşdeğer, davranışı değiştirmiyor.
  - Kapsamsız 3: `orElseThrow` lambda'ları (olmayan kayıt yolu). Doğruluğu etkilemez.
- **Stryker (web):** kurulu değil.
- **retrieval-api PIT:** yeni kod için çalıştırmadım (tester da çalıştırmadı); sorgu davranışı Testcontainers + gerçek PostgreSQL testleri ve gerçek sistem denemeleriyle sınandı.

## Uçtan uca

### Gerçek ölçümler (rehber 2026.10, 100 soru, golden set `rehber-golden-v1`)
Ortam: dev postgres, retrieval-api, assistant-api, Ollama (`qwen3.5:9b`, `bge-m3`), reranker `:8081` açık. Hepsini ben yeniden ölçtüm; backend-coder'ın sayılarıyla aynı çıktı.

| Çalıştırma | Strateji | Ölçülen / ölçülemeyen | recall@5 | MRR | Kaçan | Reranker DOWN | Süre |
|---|---|---|---|---|---|---|---|
| ev_4 ve ev_6 | rerank 20→5 | 100 / 0 | **0,98** (98/100) | **0,9125** | 2 | 0 | 103 sn |
| ev_5 | top-5 (`RETRIEVAL_STRATEGY=top5`) | 100 / 0 | **0,89** (89/100) | **0,7223** | 11 | 0 | 5,2 sn |

- Bağımsız kontrol (uygulama kodundan değil SQL'den): `eval_run_questions` üzerinde `avg(coalesce(1.0/prompt_position,0))` → ev_4: 98 isabet, MRR 0,9125; ev_5: 89 isabet, MRR 0,7223. Uygulamanın yazdığı değerlerle aynı.
- Rerank iki kaçanı: sıra 70 ve 87; ikisi de ilk 20 adayda yok ("20+"). Top-5'te 11 kaçan, hepsi "yok".
- Rerank kazancı net: recall 0,89 → 0,98, MRR 0,72 → 0,91. İki çalıştırma "Çalıştırmalar" tablosunda alt alta karşılaştırılabiliyor.
- ev_3 `FAILED` / `EVAL_RUN_INTERRUPTED` (backend-coder servisi ölçüm sırasında durdurdu); ekranda "başarısız".

### Sıfırla ve yeniden yükle (vekil senaryo)
Silme ucu olmadığı için aynı PDF'i "QA Rehber kopyası" adıyla ekrandan (Belge ekle penceresi) yükledim (kaynak 10003, 154 yeni parça). Golden set'e dokunmadan rerank ölçümü: ev_7 **DONE, 100 ölçülen, 0 ölçülemeyen**, recall 0,94, MRR 0,86. İşaretler yeni parça kimlikleriyle de bulundu; yani işaret kimliğe bağlı değil. Recall'un 0,98'den 0,94'e düşmesi beklenen bir yan etki: iki kopyanın aynı parçaları top-20/top-5'te yer kapıyor. Bu bir hata değil.
- **Not:** Bu kopya dev veritabanında kaldı (kaynak 10003). Kaldıracak uç yok; temizlik için `docker compose -f deployment/docker-compose.dev.yml down -v` gerekir (kullanıcı onayıyla). Ayrıca golden set doğru sürümde (`rehber-golden-v1`, 100 soru) yüklü.

### Rastgele 12 golden sorusunun PDF'teki yeri
`docs/rehber/pdf/…2026.10.pdf`'ten `pdftotext` ile her sorunun `pageNumber` sayfasını okudum ve normalize `answerText`'in o sayfada geçtiğini kontrol ettim: **12/12 doğru** (ör. s. 31 taşıt kredisi, s. 15 yurt dışı ATM, s. 38 havale iptali, s. 8 Gold gelir şartı, s. 52 kredili işlem).

### Önizleme / Konuşmalar işaretleri
- Kaçanlar'ın ilk satırına ("Ücret değişiklikleri bana ne kadar önceden bildiriliyor?") tıklandı → Önizleme'de yeni konuşma (cnv_26), cevap: "…yürürlük tarihinden en az 30 gün önce SMS ve e-posta ile size bildirilir [1]", alıntı s. 5. Parça kartında 1 sarı parça, alt satırda "✓ atıf yapılan · sarı: cevabın geçtiği parça". Konuşmalar'da DAYANAKLI.
- Geçici golden set `qa-tmp` (2 soru) yükleyip sonra gerçek set'i geri yükledim:
  - "Uzaylılar bana kredi verir mi?" (cevap parçası prompt'ta yok) → model "bulamadım" dedi → liste ve detayda **KAÇIRDI**, detayda "cevap burada" etiketi yok (doğru).
  - "Bitcoin alıp satabilir miyim?" (cevap parçası 3. sırada, prompt'ta) → **ÜRETİM HATASI**, parça tablosunda tek satırda "cevap burada". Sarı işaret SSE `sources`'ta yalnız o parçada `containsAnswer: true`.
  - Aynı sorunun golden set'ten önce sorulmuş eski turu (cnv_27) set yüklenince ÜRETİM HATASI'na döndü: durum her okumada hesaplanıyor (K6 doğru).
  - Gerçek set geri yüklenince bu turlar BİLMİYOR'a döner (backend-coder denemiş; kod yolu aynı).
- Cevap metni deterministik değil; ölçüm dil modeli çağırmıyor, bu yüzden metrikler modelden bağımsız.

### Lab'la görsel karşılaştırma
- Lab'ın Değerlendirme sayfası (ölçümsüz hâli) ile ekran görüntüsü karşılaştırıldı: breadcrumb, başlık, açıklama, "Ölçümü çalıştır · rerank 20→5" düğmesi, üst bar ve menü aynı.
- Ölçümlü hâl: lab `pEval` kaynağı ile karşılaştırıldı. Aynı yapı ve metinler: dört kutu, "Çalıştırmalar" tablosu (Çalıştırma/Retrieval/Rehber/recall@5/MRR/Saat), "Kaçanlar ve sırası değişenler" + "son çalıştırma", "Önizlemede sor". Kutu CSS değerleri (`repeat(4,1fr)`, gap 10, radius, 12/20 px yazı, yeşil/kırmızı) aynı.
- Lab'ın ölçümlü hâlini tarayıcıda çalıştıramadım (sağ yarı adım adım akışı bekliyor); bu yüzden ölçümlü hâl piksel piksel değil, kaynak kod ve CSS üzerinden karşılaştırıldı.
- Fark: lab'ın üst barındaki "Raşit Eşdemir · RE" kullanıcı alanı bizde yok. Bu önceki task'lardan geliyor, TASK-016 değişikliği değil.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| FYI | `GoldenAnswerLocator` | Test raporunda yazılan null durumunda NPE, sadece sahte istemciyle oluşur; gerçek istemci null dönmez | — |
| FYI | `EvaluationPage` / canlı ayar | LLM_ONLY'de düğme "rerank 20→5" yazıyor; lab da aynı (`ret` yalnızca rerank bayrağına bakıyor) | — |
| FYI | `web/e2e/` | Değerlendirme için Playwright senaryosu yazılmamış; ölçüm, yenileme, 409 ve satırdan Önizleme'ye geçiş component testleriyle ve bu raporda elle doğrulandı | — |
| FYI | Loglar | `evaluation/` ve `chunk/` log satırlarında soru, cevap cümlesi, parça metni ya da prompt yok (kontrol ettim) | — |
| FYI | Sistem | Dev veritabanında "QA Rehber kopyası" (10003) kaldı; silme ucu yok, temizlik `down -v` ile | lead / kullanıcı |

`Critical` ya da `Required` bulgu yok.

## Çalıştıramadıklarım
- Reranker kapalı senaryosu gerçek sistemde: llama-server'ı durdurmam yasak. Test düzeyinde (B13, W6, W13) kapsanıyor. İstenirse kullanıcı durdurur, ardından ölçüm tekrarlanır.
- Temiz kurulum (`down -v`): kullanıcı onayı gerekiyor; mevcut verilerle çalıştım.

## Sistem durumu
Denemeden sonra assistant-api (8090), retrieval-api (8080), Vite (5173) ve dev postgres durduruldu. Docker tam sistemi hiç açılmadı. Dev veritabanı verileri (rehber, 10003 kopyası, golden set, ev_1…ev_7) duruyor.

STATUS: DONE
