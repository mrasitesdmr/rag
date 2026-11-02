TASK: TASK-010
# QA raporu

## Kullanılan skill'ler
code-review-and-quality, java-code-review (assistant-api değişti), rag-pipeline (yeniden yazma + dil modeli), frontend-conventions (web yan kartı), writing-good-tests (PIT hayatta kalanları), e2e-testing.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Yeniden yazma `assistant.*` ile açılır/kapanır, varsayılan açık | `application.properties`: `assistant.query-rewrite.enabled=${QUERY_REWRITE_ENABLED:true}`; compose `QUERY_REWRITE_ENABLED`. Test: `AssistantPropertiesTest.givenDefaults_whenBound_thenRewriteOnTwentySecondsThreeTurns`, `givenEnabledFalse_...`. Canlı: kapatınca tüm turlar `DISABLED`, açınca `REWRITTEN` | ✅ |
| 2 | İlk turda yeniden yazma yok | `QueryRewriterTest.givenFirstTurn_...NoHistory...`; canlı: yan kart tur 1'de `· arama` ve "yeniden yazma" satırı yok | ✅ |
| 3 | Lab şablonu (İngilizce, "Standalone query:"), Türkçe sorgu | `prompts/query-rewrite.st` lab şablonu; `givenOneHistoryTurn_...SendsLabTemplate...`. Gerçek modelle 6/6 sorgu Türkçe çıktı, "aynı dilde yaz" satırı gerekmedi (kart soru 2 koşulu tetiklenmedi) | ✅ |
| 4 | Geçmiş sınırı kartta: son 3 | `assistant.history.max-turns=3`; `givenTwoHistoryTurns_...HistoryLinesOldestFirst`, `PromptBuilderTest.givenTwoHistoryTurns_...`, `AssistantPropertiesTest.givenMaxTurnsOutOfRange_...` | ✅ |
| 5 | Hata/boş/uzun/zaman aşımı → özgün soru, tur kaydeder, cevap akar | `givenModelFails_`, `givenModelThrowsSynchronously_`, `givenModelSilentLongerThanTimeout_`, `givenBlankOrLabelOnlyOutput_`, `givenOutputLongerThanMaxLength_`; `SendMessageIntegrationTest.givenRewriteFailed_whenSendMessage_thenSearchesWithQuestionAndStreams`; 20 sn = `query-rewrite.timeout` (kart soru 4) | ✅ |
| 6 | Tur kaydına arama sorgusu + süre | `V2__add_query_rewrite_to_turns.sql` (status, latency, history_turns; eski turlar `DISABLED`); `TurnServiceIntegrationTest`, `ConversationPersistenceIntegrationTest`, `givenRetrievalUnavailableAfterRewrite_...TurnKeepsSearchQueryAndStatus` | ✅ |
| 7 | Yan kart `· arama: "…"` + "yeniden yazma" süre satırı | `strategyLabels.ts` / `latencyRows.ts` / `ContextChunksCard.tsx`; `previewModel.test.ts`, `PreviewPage.test.tsx`; tarayıcıda doğrulandı (aşağıda); FAILED/EMPTY → "yeniden yazma başarısız" (kart soru 3) | ✅ |
| 8 | Sorgu loglanmaz, sadece uzunluk/süre | Log: `Query rewrite conversationId=7 status=NO_HISTORY historyTurns=0 questionLength=4 queryLength=4 durationMs=null` (içerik yok); diff'te sorgu/cevap loglayan satır yok | ✅ |
| K1 | Gerçek modelle ≥5 takip sorusu, açık/kapalı karşılaştırma | 6 senaryo, aşağıda | ✅ |
| K2 | İlk tur atlama, hatada geri dönüş, geçmiş sınırı testle doğrulanmış; build'ler geçer | Yukarıdaki testler; komutlar aşağıda | ✅ |
| Kart | Cevabı yazan model geçmişi görür; yalnızca tamamlanmış cevaplar; `[n]` silinir | `PromptBuilderTest`, `CitationParserTest` (+10 satır), `TurnRepository` sorgusu; `givenTurnInProgress_...NoHistoryReadAndNoRewrite` | ✅ |

## Sözleşme uyumu
- `sse.md` ↔ backend: `sources` olayında `searchQuery`, `queryRewriteStatus` (DISABLED/NO_HISTORY/REWRITTEN/FAILED/EMPTY); `done` olayında `queryRewriteLatencyMs`. Canlı akışta alan adları ve değerler birebir geldi (`off.json`: `DISABLED`, süre `null`; `on.json`: `REWRITTEN`, süre dolu).
- Backend ↔ web: `QUERY_REWRITE_STATUSES` ve `SourcesEvent`/`DoneEvent` tipleri aynı ad ve değer kümesini kullanıyor; `answerStreamEvents.test.ts` ayrıştırmayı doğruluyor.
- `openapi.yaml` `sendMessage` açıklaması (kontrol sırası: 400 → 404 → 409 → yeniden yazma → 503) kodla uyumlu; yeni HTTP status/errorCode yok (yeniden yazma hata döndürmez).
- Fark yok.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 170 test, 0 hata; JaCoCo eşiği geçti | Evet |
| `cd web && npm run build` | Geçti | Evet |
| `cd web && npm run lint` | Hata yok | Evet |
| `cd web && npm test` | 371 test geçti | Evet |
| `cd web && npm run test:coverage` | Satır %96,57 (eşik %80) | Evet |
| `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | 2 koşu: 1. koşu 10 geçti / 1 kaldı (`sources.spec.ts:35`, yükleme diyaloğu `toHaveCount(0)` zaman aşımı) / 1 atlandı / 2 çalışmadı; 2. koşu 13 geçti / 1 atlandı / 0 kaldı. E1 (`givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsSearchQueryAndRewriteRow`) iki koşuda da geçti | Kısmen: tester E1'i çalıştıramamıştı, ben çalıştırdım. TASK-007'nin yükleme testi bir kez kaldı (aşağıda FYI) |
| retrieval-api / compose | Değişmedi; çalıştırılmadı | — |

## Mutation testi
- PIT (assistant-api, `com.rasitbank.assistant.*`): 304 mutant, 271 öldü (%89), test gücü %93, satır coverage %96 (mutate edilen sınıflar), 14 kapsamsız. Maven pom'unda eşik tanımlı değil, build BUILD SUCCESS.
- Bu task'ın dosyalarında hayatta kalanlar (hepsi düşük önem):
  - `QueryRewriter.java:113` sınır koşulu (`length() >= 2` → `> 2`): 2 karakterlik `""` çıktısının tırnaklarının silinmesi test edilmemiş.
  - `PromptBuilder.java:44` `history.size() * 2` → `/ 2`: sadece ArrayList başlangıç kapasitesi; davranış etkisi yok.
  - `TurnService.java:117` log dalı (`STREAM_CANCELLED`); davranış etkisi yok.
  - `Turn.java:179/183/187/215`: yeni alanların getter'ları (`getQueryRewriteStatus`, `getQueryRewriteLatencyMs`, `getHistoryTurns`, `isNotFoundAnswer`) hiçbir testte okunmuyor; kayıt yazma tarafı entegrasyon testleriyle doğrulanıyor ama okuma tarafı (Konuşmalar ekranı, TASK-014) bu task'ta yok.
- Stryker: Kurulu değil (`web/CLAUDE.md`).

## Uçtan uca
**Yan kart (Playwright + ekran görüntüsü, canlı sistem, yeniden yazma açık):**
- Tur 1 ("Kartımı kaybettim, ne yapmalıyım?"): alt başlık `"Kartımı kaybettim, ne yapmalıyım?" · rerank 20→5`; `· arama` yok; süre satırları: retrieval-api araması, rerank, qwen3.5 cevabı ("yeniden yazma" yok). ✅
- Tur 2 ("Yenisi kaç günde gelir?"): alt başlık `"Yenisi kaç günde gelir?" · rerank 20→5 · arama: "Kartımı kaybettiğimde yeni kartım ne za…"` (40 karakterde kısaltılmış); süre satırları: **yeniden yazma 1,96 sn**, retrieval-api araması, rerank, qwen3.5 cevabı. ✅
- Ekran görüntüleri: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/c75618dc-2d66-491c-b9ac-1ceb2dd9702a/scratchpad/tur1.png`, `.../scratchpad/tur2.png`.

**Gerçek modelle açık/kapalı karşılaştırma** (qwen3.5:9b, bge-m3, bge-reranker; strateji RERANK, top 5; her çift yeni konuşmada; kapalı = `QUERY_REWRITE_ENABLED=false`, durum `DISABLED`). Bilgi bankasında tek kaynak vardı (karşılaştırma e2e koşusundan önce yapıldı). Ham veri: `.../scratchpad/on.json`, `.../scratchpad/off.json`.

| # | Konuşma | AÇIK: arama sorgusu | AÇIK: parçalar (başlık, s.) | KAPALI: arama sorgusu | KAPALI: parçalar |
|---|---|---|---|---|---|
| 1 | "Kartımı kaybettim, ne yapmalıyım?" → "Yenisi kaç günde gelir?" | "Kartımı kaybettim, yeni kartım ne zaman ve hangi süre içinde adresime gönderilir?" (1,2 sn) | 2.16 Kayıp bildirimi (s.16), **2.18 Kartın yenilenmesi ve teslim süresi (s.17)**, SSS Kartlar, 2.14, 2.17 | "Yenisi kaç günde gelir?" | **2.18 teslim süresi (s.17) ilk sırada**, 2.16, 2.17, 5 Para transferi SSS, 5.5 |
| 2 | "Kredi kartı limitimi artırmak istiyorum." → "Bunun için hangi belgeler gerekiyor?" | "Raşit Mobil uygulaması üzerinden kredi kartı limit artırımı talebinde bulunmak için neler yüklenmesi veya sunulması gerekiyor?" (1,7 sn) | 2.6 Kart limiti, 2.5 Kredi kartı başvurusu, 6.1, 6.6, 2.1 | "Bunun için hangi belgeler gerekiyor?" | 5.4 SWIFT, 3.7 Hesap açma belgeleri, 3.13 Vefat, 4.2, 2.17 (konuyla ilgisiz) |
| 3 | "Mobil bankacılığa nasıl giriş yaparım?" → "Şifremi unuttum, ne yapacağım?" | "Mobil Raşit'te şifremi unuttuğumda nasıl işlem yaparım?" (1,3 sn) | 6 SSS, 2.15, **6.3 Şifre sıfırlama**, 8.7, 6.2 | "Şifremi unuttum, ne yapacağım?" | 6 SSS, **6.3 Şifre sıfırlama**, 2.15, 8.7, 6.2 (aynı küme, sıra farklı) |
| 4 | "Havale ile EFT arasındaki fark nedir?" → "Hangisinin ücreti daha az?" | "Havale ve EFT arasındaki ücret farkı nedir? Hangisinin işlem ücreti daha düşüktür?" (1,4 sn) | **10.4 Transfer ücretleri**, 5.7, 5 SSS ×2, 5.2 EFT | "Hangisinin ücreti daha az?" | 10.1 Kart ücretleri, 10.3 Kredi ücretleri, 5.7, 10.6, 10.4 |
| 5 | "Hesap işletim ücreti var mı?" → "Peki bunu ödemek zorunda mıyım?" | "Raşit Bankası'nda bireysel müşteriler için vadesiz Türk lirası ve döviz hesaplarında hesap işletim ücreti ödenmesi zorunlu mu?" (2,0 sn) | **3.8 Hesap işletim ücreti**, 3 SSS, 3.1, 3.4, 2.1 | "Peki bunu ödemek zorunda mıyım?" | 5.10 Fatura ödemeleri, 2 SSS, 4 SSS, **3 SSS (4. sırada)**, 5.4 |
| 6 | Tur 1 + "Yenisi kaç günde gelir?" → "Ücreti var mı?" (3. tur) | "Kayıp banka kartı için yeni kartın gönderim ücreti ne kadar?" (3,4 sn) | 2.16, **10.1 Kart ücretleri**, 2 SSS ×2, 2.14 | "Ücreti var mı?" | 3.8 Hesap işletim ücreti, 10.1 Kart ücretleri, 5.7, 2 SSS ×2 |

**Cevaplar (örnek, deterministik değil):**
1. AÇIK: "Kaybolan kartınızın yerine gelen yeni kart, il merkezlerinde 3 iş günü, diğer yerlerde 5 iş gün içinde adresinize kuryeyle gönderilir [3]." (alıntı [3] teslim süresini içeren SSS parçasını gösteriyor). KAPALI: aynı bilgi + süresi dolan kartların yenilenmesi eklenmiş, alıntı [2] ve [1].
2. AÇIK: gelir/kimlik belgesi cevabı, alıntı [2] = 2.5 Kredi kartı başvurusu (limit artırımı değil, başvuru belgesi; parça 2.6 de geldi). KAPALI: "genellikle ek belge istenmez… SGK dökümü / maaş bordrosu" cevabı, alıntı [1] = 5.4 SWIFT parçası (alakasız parçaya dayanıyor).
3. İkisinde de aynı cevap (kart bilgisi + SMS ile şifre sıfırlama), alıntı [1].
4. AÇIK: "Havale ücretsizdir, bu nedenle EFT'den daha azdır [1]" (10.4 parçası). KAPALI: "Havale ücretsiz, EFT 11,95 TL [3]" (5.7 parçası).
5. AÇIK: "Hayır, … hesap işletim ücreti almaz [1]" (3.8). KAPALI: aynı sonuç, ama alıntı [4] = 3 Hesaplar SSS (doğru bilgi, farklı parça).
6. AÇIK: "Yeni kartın ücreti yılın ilk bildirimi için ücretsiz; aynı yıldaki sonraki bildirimlerde 100 TL [2]" (10.1 parçası, kayıp kart konusunda). KAPALI: Gold/Platinum üyelik ücretlerini sıralıyor, kayıp kartla ilgisiz.

**Yorum:**
- Fark çıkan sorular: 2, 4, 5, 6. Kapalıyken doğru parça ya gelmedi (2, 4, 6) ya da 4. sıraya düştü (5). Üçünde (2, 4, 6) cevap yanlış konuya kaydı; 5'te doğru bilgi tesadüfen aynı kaldı.
- Fark çıkmayan: 3 (aynı parça kümesi, "Şifremi unuttum" zaten konu içeriyor) ve **1**. Senaryo 1'de kapalıyken de 2.18 parçası ilk sırada geldi; yani onay kartının "doğru parça gelmeyebilir" beklentisi bu çiftte gerçekleşmedi (backend-coder'ın gözlemiyle uyumlu). Yeniden yazma 1'de farkı parça sırasında yaratıyor (2.16, 2.18 → 2.18, 2.16) ve cevabı tek parçadan veriyor; kazanç küçük.
- Dil: 6/6 yeniden yazılmış sorgu Türkçe; İngilizce çıkmadığı için "aynı dilde yaz" satırı eklenmedi, gerekmedi.
- Gözlem (Optional): Senaryo 2'de model sorguya geçmişte olmayan "Raşit Mobil uygulaması" ifadesini eklemiş (uydurma bağlam), senaryo 5'te "bireysel müşteriler… vadesiz TL ve döviz" ifadesini eklemiş; sorgular geçmişte olmayan ayrıntı taşıyabiliyor. Yine de parçalar daha ilgili geldi. Lab şablonunun doğal sonucu.
- Gecikme: yeniden yazma 1,2-3,4 sn; tur 3'te geçmiş uzadıkça süre artıyor (3,4 sn). Onay kartındaki "yaklaşık 1 sn" tahmininin üstünde ama 20 sn zaman aşımı içinde.

**Not (ortam):** e2e testleri kaynağı silme ucu olmadığı için bilgi bankasında iki "E2E Rehber …" kaynağı bıraktı (10017, 10018); bu yüzden yan kart denemesinde aynı parça tekrar görünüyor (#28 üç kez). Karşılaştırma tablosu bundan önce, tek kaynakla alındı. Gerekirse lead sistemi temiz kurulumla yeniden başlatmalı. Yeniden yazma şu an **açık** (`QUERY_REWRITE_ENABLED=true`).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Optional | `QueryRewriter.java:113` | `length() >= 2` sınırı testsiz (PIT sınır mutantı hayatta); `""` çıktısı için bir test eklenebilir | tester |
| Optional | `Turn.java:179-215` | Yeni alanların getter'ları hiçbir testte okunmuyor (PIT NO_COVERAGE/SURVIVED) | tester |
| FYI | `web/e2e/sources.spec.ts:41` | TASK-007 yükleme testi bir koşuda diyalog zaman aşımıyla kaldı, ikinci koşuda geçti (flaky); bu task'ın kodu ile ilgisi yok | — |
| FYI | e2e kaynakları | E2E yüklemeleri bilgi bankasında kalıcı kopya bırakıyor, silme ucu yok | — |
| FYI | `query-rewrite.st` | Model sorguya geçmişte olmayan ayrıntı ekleyebiliyor (senaryo 2, 5) | — |

Critical/Required bulgu yok. Kod konvansiyonları (üst seviye tip Javadoc'u, İngilizce identifier, log sınırları) örneklem olarak uyuyor; yeni bağımlılık eklenmemiş; sözleşme dosyalarını planner güncellemiş.

## Düzeltme
Gerekmiyor.

STATUS: DONE
