TASK: TASK-018
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `frontend-conventions` (web testi), `e2e-testing` (acceptance), `sse-streaming` (eventStream testleri).

## Senaryo → test eşlemesi
Hepsi `web/e2e/acceptance.spec.ts` içinde, tek sıralı dosya (`E2E_ACCEPTANCE=1` yoksa dosya atlanır). Gerçek modellerle (Ollama `qwen3.5:9b`, `bge-m3`, host reranker :8081) geçici `-p rag-e2e` projesinde, boş sistemde koşuldu.

| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| A0 | Ön koşul: bayrak yoksa skip, dolu bilgi bankasında `acceptance needs an empty knowledge base` | `acceptance.spec.ts` `beforeAll` + dosya düzeyi `test.skip` | Bayraksız koşuda 11 skipped doğrulandı; dolu sistem hatası kodla var, ayrıca koşulmadı |
| A1 | Boş açılış (1.1) | `givenEmptySystem_whenScreensOpened_thenEmptyStatesShown` | ✅ |
| A2 | Rehber yükleme + yükleme sırasında yenileme (1.1, 1.7) | `givenGuidePdf_whenUploadedViaDialogAndPageReloaded_thenProgressContinuesAndSourceReady` | ✅ (yenilemeden sonra satır `QUEUED…WRITING` aşamasında, sonra "Hazır", Parça 154) |
| A3 | Yer imli ikinci PDF (`page.pdf({outline:true,tagged:true})`) (1.1) | `givenBookmarkedPdf_whenUploadedViaDialog_thenSourceReady` | ✅ (yer imi üretildi; listede 2 kaynak) |
| A4 | Yer imsiz PDF reddi (1.1) | `givenPdfWithoutBookmarks_whenUploaded_thenRejectedAndNoNewRow` | ✅ |
| A5 | Akan cevap, alıntı penceresi (1.2) | `givenGuideReady_whenQuestionAsked_thenAnswerStreamsAndCitationOpensChunkDialog` | ✅ (akış kanıtı: "Soru" kutusu disabled iken cevap metni büyüdü) |
| A6 | "Bulamadım" + temsilciye aktarma (1.2) | `givenOffTopicQuestion_whenAsked_thenNotFoundSentenceAndHandoffQueued` | ✅ |
| A7 | Takip sorusu, yeniden yazılmış arama sorgusu (1.3) | `givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsRewrittenSearchQuery` | ✅ |
| A8 | Geri bildirim (1.4) | `givenAnsweredTurn_whenYesClicked_thenFeedbackSavedAndPressed` | ✅ |
| A9 | Konuşmalar (1.5) | `givenThreeConversations_whenListAndDetailOpened_thenTurnsStatusChunksAndHandoffShown` | ✅ |
| A10 | Parça filtresi ve sayfalama (1.6) | `givenGuideReady_whenChunksFilteredAndPaged_thenMatchingRowsAndNextPage` | ✅ |
| A11 | Değerlendirme çalıştırması (1.8) | `givenGoldenSetLoaded_whenEvalRunStarted_thenProgressThenKpisAndRunRow` | ✅ (ölçüm ~2 dk) |

Plandan sapmalar (testin gerçek arayüze uyması için):
- A5 alıntı düğmesi `[n]` değil, arayüzdeki gerçek adıyla aranır: `… · s. N` (kaynak düğmesi). Parça penceresinin başlığı sayfa içermez; sayfa bilgisi düğmede doğrulanır, pencerede parça metni ve `chunk_no` doğrulanır.
- A9: en üst satır A7'nin konuşması (2 tur), bu yüzden tur sayısı `2` beklenir; planda yazılmayan bu ayrıntı A7'nin iki turundan gelir.
- A10: filtreli her satırın "asgari ödeme" içerdiği, satır metninde yoksa parça penceresi açılarak (başlık + içerik) doğrulanır.
- Bayrak yokken `acceptance.spec.ts` atlanır (11 skipped); mevcut `e2e/*.spec.ts` dosyalarına dokunulmadı.

### Mutation skorunu yükseltmek için eklenen birim testleri (hepsi iş kuralına bağlı)
| Kural / davranış | Test dosyası |
|---|---|
| Öneri eşleştirme: durak sözcükler, 3 harf alt sınırı, boş metin | `features/preview/model/suggestionMatcher.stopWords.test.ts` |
| Öneri çipleri: en fazla 2, başlangıç soruları önce; otomatik tamamlama sırası ve seçenek kimliği | `features/preview/model/suggestions.edge.test.ts` |
| Cevap biçimi: madde işareti, iki haneli alıntı, boş satır, kalın yazı, bağlamsız alıntı | `shared/turn/answerFormat.edge.test.ts` |
| SSE ayrıştırma: olay adı sızmaması, yorum satırı, CRLF, parçalara bölünmüş CR, çok satırlı data | `shared/api/eventStream.edge.test.ts` |
| SSE olay doğrulama: bozuk `sources`/`done`/parça alanları reddedilir, null'lanabilir alanlar kabul edilir | `features/preview/model/answerStreamEvents.validation.test.ts` |
| Strateji etiketleri (sayı eksikken) | `features/preview/model/strategyLabels.edge.test.ts`, `features/conversation/model/turnLabels.edge.test.ts` |
| Ölçüm alt yazıları ve hata metni | `features/evaluation/model/evaluationLabels.edge.test.ts` |
| Ölçüm başladı / tamamlandı / tamamlanamadı duyurusu | `features/evaluation/hooks/useEvalRunAnnouncement.test.ts` |
| Bilgi/uyarı kutusu simgesi | `shared/components/Callout.test.tsx` |

## Mutation
| Araç | Modül | Skor | Eşik | Sonuç |
|---|---|---|---|---|
| PIT 1.30.1 | assistant-api | **%88** (802 mutant, 702 öldü; 53 kapsamsız; test gücü %94; satır kapsamı %95) | 80 | BUILD SUCCESS |
| PIT 1.30.1 | retrieval-api | **%86** (629 mutant, 541 öldü; 29 kapsamsız; test gücü %90; satır kapsamı %97) | 80 | BUILD SUCCESS |
| Stryker 10.1.1 | web | **%83,14** (önce %79,36) | 80 (`break`) | Komut 0 döndü, 42 dk 14 sn |

- Web: frontend-coder koşusunda 610 survived + 87 no coverage idi; yukarıdaki testlerden sonra eşik aşıldı. Kalan survivor'ların bir kısmı **eşdeğer mutant** olarak bırakıldı, zorlama test yazılmadı:
  - `answerStreamEvents.ts` içinde `typeof x === 'number' && Number.isInteger(x)` ve `typeof x === 'string' && LIST.includes(x)` kalıplarındaki `typeof` mutantları (sağ taraf zaten aynı sonucu verir).
  - `suggestionMatcher.ts` `stemOf` içindeki `join('')` ayracı (iki taraf da aynı biçimde kök alır), `stemsOf` boş metin dalı.
  - `answerFormat.ts` `bullet[1] ?? ''` ve `match[1] ?? ''` yedekleri (regex grubu hep dolu).
  - `eventStream.ts`: `response.body === null` dalı (fetch yanıtında jsdom/MSW ile üretilemiyor), `reader.cancel().catch` temizliği.
  - `MessageList.tsx` kaydırma hesabı (`offsetTop`, `scrollHeight`, `clientHeight`): jsdom yerleşim hesaplamaz; Playwright ile doğrulanır (A5 akışı).
  - `formatDateTime.ts` / `formatNumber.ts` modül düzeyindeki `Intl` seçenek mutantları: `RuntimeError`, skora girmez.
- Ölü kod gösteren mutant bulunmadı, `NEEDS_FIX` yok. İsteğe bağlı sadeleştirme önerisi (zorunlu değil): yukarıdaki gereksiz `typeof` kontrolleri.
- Hâlâ düşük skorlu ama eşiği etkilemeyen kapsam boşlukları (frontend-coder listesiyle aynı): `MessageList.tsx`, `AddSourceDialog.tsx`, `useAnswerStream.ts`, `Dialog.tsx`, `EvaluationPage.tsx`, `PreviewPage.tsx`. İş kuralı olarak açık bir hata gösteren survivor görülmedi.
- Backend PIT survivor'ları incelenmedi (eşik aşıldı, kural bazlı hata sinyali yok).

## Komutlar ve sonuçları
| Komut | Sonuç |
|---|---|
| `cd assistant-api && ./mvnw verify` | Geçti: 441 test, 0 başarısız; JaCoCo satır kapsamı %95,5 |
| `cd retrieval-api && ./mvnw verify` | Geçti: 344 test, 0 başarısız; JaCoCo satır kapsamı %96,4 |
| `cd assistant-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS, %88 |
| `cd retrieval-api && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS, %86 (19 dk 28 sn) |
| `cd web && npm run build` | Geçti |
| `cd web && npm run lint` | Geçti, uyarı yok |
| `cd web && npm run test:coverage` | Geçti: 46 dosya, 728 test (önce 620). Satır %97,54, dal %92,65, ifade %97,51 |
| `cd web && npm run test:mutation` | Exit 0, skor %83,14 |
| `cd web && E2E_ACCEPTANCE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test e2e/acceptance.spec.ts` | 11 passed, iki ayrı koşuda (3,1 dk + 3,1 dk), `-p rag-e2e` sisteminde, gerçek modellerle |
| `cd web && E2E_BASE_URL=… npx playwright test e2e/acceptance.spec.ts` (bayraksız) | 11 skipped |

Ortam: ana sistem (`rag`) `down` (`-v` yok) ile kapalı, `rag_pgdata` volume'una dokunulmadı; dev postgres zaten kapalıydı. Geçici `rag-e2e` projesi `down -v` ile silindi. Ollama ve llama-server'a dokunulmadı.

## Çalıştırılamayanlar
- Mevcut `web/e2e/*.spec.ts` (shell, sources, preview, conversations) bu turda koşulmadı; tam regresyon qa'nın C15 adımıdır. Acceptance dosyası bayraksız atlandığı için o koşuyu bozmaz.
- A0'ın "dolu bilgi bankasında hata verir" dalı ayrıca koşulmadı (kodda var; ana sistem verisi açılmadı).

## Düzeltme turu 1
Kaynak: qa `NEEDS_FIX` (3 görsel fark), frontend-coder düzeltmesi (`02-frontend-implementation.md` "Düzeltme turu 1").

| Madde | Test / değişiklik | Sonuç |
|---|---|---|
| Parça penceresi başlığı Bilgi kaynakları: `s. {sayfa} · {başlık}` ([n] yok) | `shared/chunk/ChunkDialog.test.tsx` (prop'suz render, tam ad eşleşmesi: `s. 19 · 2.16 Kayıp ya da çalıntı bildirimi`, `s. 19 · Başka parça`), `source/pages/SourceDetailPage.test.tsx` (2 test) | ✅ |
| Parça penceresi başlığı Önizleme: `[n] s. {sayfa} · {başlık}` | `preview/pages/PreviewPage.test.tsx` (`[1] s. 19 · …` alıntıdan, `[3] s. 19 · Parça 153 başlığı` kart satırından) | ✅ |
| Parça penceresi başlığı Konuşmalar: `[n] s. {sayfa} · {başlık}` | `conversation/pages/ConversationDetailPage.test.tsx` (satır ve alıntı tıklaması; başlık `heading` rolüyle tam ad) | ✅ |
| Konuşmalar parça hücresi `s. {sayfa} · {başlık} · #{no}` | `ConversationDetailPage.test.tsx` (`s. 19 · 2.16 Kayıp ya da çalıntı bildirimi · #31`, `s. 20 · Başlık 2 · #32`; silinmiş parça testinde de) | ✅ |
| Önizleme ayracı `Yeni konuşma · cnv_{id}` | `PreviewPage.test.tsx`, `PreviewPage.suggestions.test.tsx` (3 test); kimlik yokken "Yeni konuşma" testi değişmedi | ✅ |
| Acceptance A5 | Parça penceresi `h3` başlığı artık `/^\[\d\] s\. \d+ · .+/` ile doğrulanır (kaynak düğmesinden açılış) | ✅ |

Güncellenen eski-metin testleri: 17 (ChunkDialog 6, SourceDetailPage 2, PreviewPage 3, PreviewPage.suggestions 2, ConversationDetailPage 4). Beklenen değerler lab biçiminden ve frontend raporundaki ekran metinlerinden türetildi, koddan kopyalanmadı.

### Yeniden koşulanlar
| Komut | Sonuç |
|---|---|
| `cd web && npm run build` / `npm run lint` | Geçti, uyarı yok |
| `cd web && npm run test:coverage` | 46 dosya, 728/728 geçti; satır %97,55, dal %92,66, ifade %97,51 |
| `cd web && npm run test:mutation` (C9) | Exit 0, skor **%83,23** (eşik 80), 46 dk 22 sn. Koşu benim ve güncel testlerle (qa'nın 11:12 koşusu değil) |
| Acceptance `-p rag-e2e` (`E2E_ACCEPTANCE=1`) | 11 passed (3,8 dk), gerçek modellerle, boş sistemde |
| PIT ×2 | Backend değişmediği için yeniden koşulmadı (önceki tur: %88 / %86) |

Ortam: ana sistem `down` (`-v` yok) ile durduruldu, iş bitince `rag-e2e` `down -v` ile silindi ve ana sistem `up -d` ile geri açıldı (4 servis healthy, `rag_pgdata` korundu). Ollama ve llama-server'a dokunulmadı.

STATUS: DONE
