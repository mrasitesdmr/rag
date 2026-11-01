TASK: TASK-008
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Diğer skill'ler (java-code-review, springboot-verification, rag-pipeline, sse-streaming, frontend-conventions, e2e-testing) ayrıca yüklenmedi. Bu turda kod satır satır okunmadı; denetim komut, canlı sistem ve sözleşmeye karşı davranış üzerinden yapıldı (bkz. "Kod review bulguları").

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Serbest metin, boş soru yok, uzunluk sınırı | DB `CHECK char_length 1..1000`; web testleri (350 geçti); canlı: "Merhaba", "Bugün hava…" serbest metin kabul edildi | ✅ |
| 2 | Konuşma: ilk soru yeni konuşma, kanal PREVIEW, id backend'de | `POST /conversations {"channel":"PREVIEW"}` → `conversationId`; `conversations.channel CHECK ('PREVIEW')` | ✅ |
| 3 | Karşılama mesajı lab'daki gibi | Ekran görüntüsü: "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. …" lab ile aynı metin | ✅ |
| 4 | Prompt: sistem talimatı lab'la aynı, 5 numaralı parça + `Soru:`, temp 0, num_ctx 8192 | `prompts/rag-system.st` metni `rag-lab.html` `sysRag` ile karakter karakter aynı (`{notFoundAnswer}` yer tutucusu "Bu konuda rehberimizde bilgi bulamadım."); `rag-user.st`: `<parcalar>…</parcalar>` + `Soru:` | ✅ |
| 5 | SSE akışı; istemci kopunca model iptal | Canlı: token → sources → done, `sse.md` ile uyumlu. İptal: aşağıda "Kabul 4 iptal doğrulaması" | ✅ |
| 6 | Alıntı `[n]` düğme, "Rehber · s. N" yerine belge adı | Ekran görüntüsü: `[1]` rozeti + "Rehber Üç · s. 16"; `citations:[1]` | ✅ |
| 7 | Bulamadım → `notFoundAnswer`, sayfa düğmesi yok | "Bugün hava nasıl olacak?" → tur 15 `not_found_answer=t`, `cited={}`; boş KB mock'lu tarayıcı denemesinde düğme yok | ✅ |
| 8 | Yan kart: ilk "Asistan nasıl cevaplıyor?", sonra parçalar, süre çubukları (retrieval, rerank, qwen), puan ve mesafe | Ekran görüntüsü (app-answer): "[1] ✓ s. 16 · 2.16 … #28 · rerank 2,90 · d 0,298", çubuklar 469 ms / 2,10 sn / 24,6 sn | ✅ |
| 8b | Makine etiketi ayardan, sabit yazılmaz | `hostLabel: null` (ayar boş) → ekranda gösterilmiyor | ✅ |
| 9 | Tur kaydı alanları, tur akış bitince yazılır | `conversation_turns`, `turn_chunks` (20 aday, vector_rank, distance, rerank_score, prompt_position) DB'de dolu; tur 9–17 incelendi | ✅ |
| 10 | Loglamada soru/cevap/parça/prompt yok | `log.*` çağrıları tarandı: sadece id, uzunluk, sayı, süre, errorCode | ✅ |
| 11 | retrieval-api/dil modeli kapalıyken hata kodu ile tur | DB: tur 6 `CHAT_MODEL_UNAVAILABLE`, tur 7 `RETRIEVAL_UNAVAILABLE`; Türkçe eşleme `errorMessages.ts` (kendim kapatıp denemedim, devops/tester kaydına ve DB'ye dayanıyor) | ✅ |
| 12 | Strateji `top5`/`rerank` ayardan, varsayılan rerank | `assistant.retrieval.strategy=${RETRIEVAL_STRATEGY:rerank}`; DB'de tur 8 `TOP_K` | ✅ |
| 13 | 20 aday puanlanır, ilk 5, eşitlikte vector sırası | Canlı: 20 satır `rerank_score` dolu; prompt_position yalnızca 5'inde. Eşitlik birim testlerde (PIT/test raporu) | ✅ |
| 14 | Reranker kapalı → vector sırası ilk 5, `RERANK_FALLBACK` | Canlı: reranker sürecine `kill -STOP` (5 sn read-timeout), soru cevaplandı; `strategy RERANK_FALLBACK`, `rr=None`, vr=1..5 prompt'ta; rerank 5098 ms; sonra `-CONT` ile geri açıldı. DB'de tur 4 de aynı | ✅ |
| 15 | Kayıt: 20 adayın vector sırası, mesafe, puan | `turn_chunks` 20 satır/tur | ✅ |
| 16 | Kart alt başlığı "rerank 20→5" ve parçada "rerank 2,90 · d 0,298" | Ekran görüntüsü; `RERANK_FALLBACK` metni birim testle (tarayıcıda görmedim) | ✅ |
| 17 | Süre çubuğunda rerank satırı | Ekran görüntüsü | ✅ |
| 18 | Reranker metni loglanmaz | `ContextRetriever` logu: sadece sayı ve süre | ✅ |
| K1 | 5 rehber + 2 rehber dışı soru gerçek modelle | "Gerçek modelle denemeler"; rehber dışı: hava ✅, Bitcoin ❌ (aşağıda) | ❌ |
| K2 | sse.md yazılmış, iki taraf uyuyor | Canlı akış `sse.md` alanlarıyla birebir; web ayrıştırıcı testleri geçti | ✅ |
| K3 | 5 soruda top-5 ↔ rerank sıraları gerçek reranker ile karşılaştırılmış; yedek davranış gerçek sistemde | Karşılaştırma yapıldı ama **anlamsız**: aşağıda "Rerank karşılaştırması". Yedek davranış ✅ | ❌ |
| K4 | Testler: alıntı, bulamadım, prompt, iptal, rerank sırası, eşitlik, zaman aşımı, yedek | assistant-api 125 test geçti; PIT sonrası ek testler var | ✅ |
| K5 | Önizleme lab'ın sol yarısıyla aynı | Yan yana ekran görüntüsü: üst bar, menü, telefon, karşılama, kart aynı. Lab'daki çipler, yapılandırma şeridi ve öneri soruları bu task'ın kapsamı dışı (TASK-011/012). Lab'da telefon sağdaki konsolla yan yana dar sütunda; uygulamada tam genişlik, kart telefonun sağında geniş (düzen farkı küçük, not) | ✅ |
| K6 | `./mvnw verify`, `npm run build`, `npm run lint` | Aşağıda | ✅ (retrieval-api değişmedi, çalıştırılmadı) |

## Gerçek modelle denemeler (Ollama qwen3.5:9b, bge-m3, llama-server bge-reranker-v2-m3; strateji RERANK, temperature 0)
Sistem localhost:3000 üzerinden (nginx). İlk soruda model yüklenme gecikmesi (ilk token 31 sn). `hostLabel` boş olduğu için makine etiketi gösterilmedi.

| # | Soru | Cevap (özet) | Alıntı | Parça sayfası/başlık | Süre (toplam / retr / rerank / ilk token) | Sonuç |
|---|---|---|---|---|---|---|
| 1 | Kartımı kaybettim, ne yapmalıyım? | Raşit Mobil, İnternet Şubesi, Müşteri Hizmetleri ya da şubeden kapat; yeni kart 3-5 iş günü [1] | [1] | s. 16 · 2.16 Kayıp ya da çalıntı bildirimi | 42,5 sn / 2825 / 2168 / 31489 ms | ✅ |
| 2 | Ev kredisini erken kapatırsam ceza öder miyim? | Sabit faizlide %1 (≤36 ay) / %2; değişken faizde ceza yok [1] | [1] | s. 36 · 4 Krediler · Sık sorulanlar | 37,2 sn / 1071 / 2828 / 28191 | ✅ |
| 3 | Kredi kartı limitimi nasıl artırabilirim? | Raşit Mobil > Kartlarım > Limit işlemleri [1] | [1] | s. 10 · 2.6 Kart limiti | 20,4 sn / 546 / 1745 / 15156 | ✅ |
| 4 | Havale ve EFT saatleri nedir? | Havale 7/24; EFT hafta içi 09.00–17.00 [1] | [1] | s. 40 · 5.6 İşlem saatleri | 24,9 sn / 463 / 2028 / 16464 | ✅ |
| 5 | Şifremi unuttum, mobil uygulamaya nasıl girerim? | "Şifremi unuttum" → kart bilgisi + SMS ile yeni şifre [1] | [1] | s. 49 · 6 Dijital bankacılık SSS | 23,7 sn / 872 / 2159 / 16405 | ✅ |
| 6 | Bugün hava nasıl olacak? (rehber dışı) | "Bu konuda rehberimizde bilgi bulamadım." | yok | — | 20,7 sn | ✅ `notFoundAnswer=true`, sayfa düğmesi yok |
| 7 | Bitcoin alıp satabilir miyim? (rehber dışı) | "Raşit Bankası'nda sadece ABD doları, euro, … alıp satılabilir; **Bitcoin alım-satımı yapılamaz.** [1]" | [1] | s. 52 · 7.4 Döviz (Bitcoin geçmiyor) | 23,2 sn | ❌ uydurma |

Bitcoin sorusu 3 kez denendi (Playwright E2E dahil), 3'ünde aynı cevap (temperature 0). Rehberde "bitcoin/kripto" geçen parça yok (retrieval DB: 0 satır). Model sabit cümleyi yazmadı, döviz parçasından "yapılamaz" çıkarımını uydurdu ve `[1]` ile alıntıladı; tur `notFoundAnswer=false`. Prompt lab'la birebir aynı. `rag-lab.html` içindeki kayıtlı cevapta da aynı "Bitcoin alım-satımı yapılamaz" cümlesi geçiyor; yani bu modelin lab'daki davranışıyla aynı, kod hatası değil. Ama TASK örnek senaryosu ve onay kartı 3. madde ("bulamadım cümlesi gelir") bu modelle sağlanmıyor.

## Rerank karşılaştırması (5 soru, gerçek reranker)
Veritabanında Rehber'in 16 kopyası var (retrieval `sources` 10001–10016: "Ürün ve Hizmet Rehberi", "Rehber Yeni/Üç/Dört", "E2E Rehber …", "Ollama Kapali …" gibi önceki task'ların ve E2E testlerin yüklediği kopyalar). Her soruda 20 adayın 15'i aynı parçanın kopyası. Tur 10 (ev kredisi): adaylarda **yalnız 2 farklı parça** (#75 sayfa 36 ×15, #71 sayfa 35 ×5); tur 12: #84 ×15, #78 ×5. Sonuç:
- Rerank puanları aynı parçanın kopyaları için bile 3. basamakta oynuyor (5,19 / 5,18): prompt'taki 5 parça aynı içeriğin 5 kopyası, sıra değişikliği sadece sayısal gürültü (ör. tur 10: vector 8. ve 9. kopya prompt'ta 2. ve 3. sırada).
- Bu yüzden "vector sırasında 7. olan erken ödeme parçasının rerank'te 1. sıraya çıkması" (TASK örneği) bu veriyle gösterilemiyor; top-5 ↔ rerank karşılaştırması anlamlı bir sonuç vermiyor. Çok parçalı bağlam (ör. tur 13: #108 ve #109) kopyalar yüzünden yine bastırılıyor.
- Mantık yönü doğru: tüm 20 aday puanlanıyor, en yüksek 5 prompt'a giriyor, `prompt_position` ve `vector_rank` ikisi de kayıtlı; alakasız soruda puanlar çok düşük (hava: −9,4), alakalıda yüksek (ev kredisi: +5,2).
- Kabul kriterini karşılamak için kopya kaynakların kaldırılması gerekir; bu veri silme olduğu için ben yapmadım (kullanıcı onayı gerekir).

## Sözleşme uyumu
- `openapi.yaml`: `createConversation` (400 `VALIDATION_FAILED`, `channel` alanı) ve `sendMessage` yolları canlı çalıştı; hatalı istek RFC 9457 + `errorCode` döndü.
- `sse.md`: `token` → `sources` → `done` sırası; `sources` alanları (`retrievalStrategy`, `candidates`, `topK`, `contextChunks[].n/chunkId/chunkNo/pageNumber/heading/sourceId/sourceName/version/vectorRank/distance/rerankScore`, `citations`) ve `done` alanları (`turnId`, `notFoundAnswer`, `chatModel`, token sayıları, süreler, `hostLabel`) kendi yazdığım ayrıştırıcıyla birebir okundu. `RERANK_FALLBACK`'te `rerankScore=null`, `rerankLatencyMs` dolu (5098). Web ekranı bu alanlarla render etti.
- Web `errorMessages.ts` `CHAT_MODEL_UNAVAILABLE` vb. için Türkçe eşleme içeriyor.
- Fark yok.

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw -q verify` | exit 0; 15 sınıf, 125 test, 0 başarısız/hata; JaCoCo eşiği geçti | Evet (125) |
| `cd web && npm run build` | başarılı (348 kB js) | Evet |
| `cd web && npm run lint` | oxlint, uyarı/hata çıktısı yok | Evet |
| `cd web && npm test` | 350 test geçti | Evet |
| `cd web && npm run test:coverage` | 350 geçti; satır %96,5 (eşik %80) | Evet (%96,4) |
| `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e -- preview.spec.ts` | 1 geçti, **1 kaldı**: `givenGuideIngested_whenBitcoinQuestionAsked_thenNotFoundSentenceAndNoSourceButtons` (gerçek model uydurdu, yukarıya bak) | Hayır: tester çalıştıramamıştı |
| retrieval-api `./mvnw verify` | Çalıştırılmadı (bu task'ta değişmedi) | — |

## Mutation testi
- assistant-api, PIT 1.30.1: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` → 264 mutant, 236 öldürüldü (**%89**), 11 kapsamsız, test gücü **%93**, satır kapsamı (mutasyonlu sınıflar) %96. Test raporundaki ilk koşu %70'ti, ek testlerle yükselmiş; raporla tutarlı. PIT'te bağlayıcı eşik tanımlı değil. Sınıf bazlı dağılım: 71–100%; en düşük %71'lik sınıf 7 mutantlık küçük sınıf. Önemli hayatta kalan mutant incelemesi yapmadım (HTML raporu açılmadı).
- web: Stryker kurulu değil (`web/CLAUDE.md`).

## Kabul 4 iptal doğrulaması (lead'in 1. noktası)
Yöntem: uzun cevap isteyen soruyu gönderdim, 6. token'dan sonra istemci bağlantısını kapattım (tarayıcı sekmesi kapanmasına eşdeğer, `http.client` close).
- assistant-api: tur 16 `STREAM_CANCELLED`, o ana kadarki metin (`Kartlar bölümünün detay…`) kayıtlı, `completion_tokens` boş.
- **Ollama tarafı, gözlemlendi:** `/opt/homebrew/var/log/ollama.log` aynı anda (22:46:09): `[GIN] 200 | 16.7s | POST /api/chat`, hemen ardından `speculative decode stats iterations=10` (üretilen ~10 adım; uzun cevap istenmişti) ve `runner.go:300 msg="Request terminated" error="context canceled"`. Yani Ollama isteği iptal olarak gördü ve üretimi erken bıraktı. Log'da toplam 4 "Request terminated" satırı var (önceki task denemeleri dahil).
- Runner süreç CPU'su iptalden sonra 24 sn boyunca %0,0 (GPU/Metal üretimi olduğu için CPU tek başına kanıt değil; asıl kanıt log satırları).
Sonuç: ✅ doğrulandı.

## Uçtan uca
- Playwright `preview.spec.ts`: 2 senaryodan 1 geçti (Önizleme rehber sorusu), 1 kaldı (Bitcoin; model davranışı).
- Tarayıcıda elle (Playwright ekran görüntüsü): boş ekran → soru → akış → yan kart (alıntı rozeti, "Rehber Üç · s. 16", puanlar, süre çubukları).
- Lead'in 4. noktası (boş bilgi bankası ipucu): bilgi bankasını boşaltamayacağım için `page.route` ile SSE cevabı **taklit edildi** (`contextChunks: []`, `notFoundAnswer: true`). Ekranda "Bilgi bankasında henüz belge yok. Bilgi kaynakları'ndan belge ekle." ipucu, "Bu konuda rehberimizde bilgi bulamadım." ve yan kartta "Merhaba · rerank 20→5" göründü, sayfa düğmesi yok. Bu, ekranın render'ını doğrular; backend'in gerçek boş KB davranışı devops'un API denemesine dayanıyor (ben görmedim).
- Dil modeli/retrieval-api kapalıyken Türkçe hata mesajı: tarayıcıda denemedim (DB kayıtları ve `errorMessages.ts` testi var).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Required | `web/e2e/preview.spec.ts:77-86` | Bitcoin senaryosu gerçek modelle deterministik olarak kalıyor (3/3); model rehberde olmayan konuda uydurma cevap veriyor. Test, model davranışına bağlı olduğu için güvenilir değil. Rehber dışı soru olarak gerçekten bulamadım üreten bir soru ("Bugün hava nasıl olacak?" bu denemede ürettiyi) kullanılmalı | tester |
| Required | retrieval `sources` 10002–10016 (veri) | Önceki task/E2E çalıştırmalarından 15 kopya kaynak; 20 adayı dolduruyor, rerank karşılaştırmasını anlamsız kılıyor (kabul K3). Silme işlemi kullanıcı onayı ister | lead (kullanıcı onayı) / devops |
| Optional | `assistant-api/CLAUDE.md:17` | "henüz migration yok, Flyway 'No migrations found' uyarısıyla çalışır" yazıyor; `V1__*.sql` var (conversations, conversation_turns, turn_chunks). Satır 5'teki durum satırıyla çelişiyor | backend-coder |
| FYI | `prompts/rag-system.st` + model | Bitcoin uydurması: prompt lab'la birebir; yaklaşım değişikliği (ör. rerank puanı eşiği, ek talimat) task kapsamı/kararı dışında. Onay kartı 3. madde ("bulamadım gelir") bu modelle tutmuyor; kullanıcıya bilgi verilmeli | lead |
| FYI | `done` / yan kart | Rerank puanında aynı içerik 3. basamakta farklı; tie kuralı ancak tam eşitlikte devreye giriyor. Kopya kaynak yokken sorun olmaz | — |
| FYI | Önizleme düzeni | Lab'da telefon ve kart dar sütunda, uygulamada tam genişlik; hizalama ve kart başlıkları aynı | — |

## Düzeltme
- **tester:** `web/e2e/preview.spec.ts` Bitcoin senaryosunu gerçek modelde kararlı çalışan bir rehber dışı soruyla değiştir (ya da senaryoyu bu modelle doğru olmayan örnek olarak çıkar); `npm run test:e2e -- preview.spec.ts` iki senaryo yeşil olsun, çalıştırıp rapora yaz.
- **lead (kullanıcı onayıyla) / devops:** retrieval DB'deki kopya kaynakların (10002–10016: "Rehber Yeni/Üç/Dört", "E2E Rehber …", "Ollama Kapali …") kaldırılmasına kullanıcıdan onay al, kaldır; ardından en az 5 soruda top-5 ↔ rerank sıralarını yeniden çıkar (TASK örneği: ev kredisi erken ödeme parçasının vector sırası ↔ rerank sonrası). Karşılaştırmayı bu turda ben yeniden yaparım.
- **lead:** Bitcoin sorusunun gerçek modelde "bulamadım" yerine uydurma cevap verdiğini (lab'daki kayıtta da aynı) kullanıcıya bildir; TASK örnek senaryosu/onay kartı 3. madde bu modelle sağlanamıyor.
- **backend-coder (küçük):** `assistant-api/CLAUDE.md:17` Flyway satırını V1'e göre güncelle.

## Düzeltme turu 1
| Madde | Kanıt | Sonuç |
|---|---|---|
| tester: Bitcoin E2E senaryosu | `E2E_BASE_URL=http://localhost:3000 npm run test:e2e -- preview.spec.ts` → 2 senaryo geçti (rehber sorusu 36,9 sn; "Bugün hava nasıl olacak?" bulamadım 13,6 sn) | ✅ |
| backend-coder: Flyway satırı | `assistant-api/CLAUDE.md:17` artık `V1__create_conversations_and_turns.sql` → 3 tablo | ✅ |
| Rerank karşılaştırması | Kopya kaynaklar (10002–10016) hâlâ duruyor, silinmedi (silme yapılmadı). Karşılaştırmayı kopyalara rağmen anlamlı hâle getirdim: 5 yeni soru için retrieval-api'den 20 aday alıp gerçek reranker'a (llama-server :8081) doğrudan puanlattım, parçaları (chunkNo, sayfa) ile ayrıştırdım. Aşağıda | ✅ (kısıtla) |
| Bitcoin uydurması | Model sorunu, kod değişikliği yok; lead'e bildirildi, TASK örneği "Bitcoin" yerine rehber dışı hava sorusuyla doğrulandı | FYI |

Rerank sonuçları (20 aday; her soruda yalnız 2 ayrı parça, geri kalanı kopya; vector top-5 ↔ rerank top-5, parça = chunkNo/sayfa):
| Soru | Vector top-5 | Rerank top-5 | Sıra değişti mi |
|---|---|---|---|
| Ev kredisini erken kapatırsam ceza öder miyim? | #75/s.36 ×5 | #75/s.36 ×5 (3,08) | Hayır, aynı parça |
| Yurt dışında kartımla nakit çekersem komisyon var mı? | #25/s.15 ×5 | #25/s.15 ×5 (3,11) | Hayır |
| Vadeli hesapta faiz ne zaman yatar? | #39/s.20 ×5 | #39/s.20 ×5 (2,81) | Hayır |
| Hesabım bloke oldu ne yapmalıyım? | #98/s.46 ×5 | **#106/s.49 ×5** (−0,56; vector sırasında 16.–20. kopyalar) | **Evet**, farklı parça |
| Kredi başvurusu için hangi belgeler gerekir? | #58/s.29 ×5 | **#9/s.9 ×4 (3,31) + #58/s.29 ×1 (3,24)** (vector 16.–19.) | **Evet**, 2. sıradaki parça öne çıktı |
Reranker alt sıradaki parçayı üste taşıyor (5 sorunun 2'sinde), bu yüzden 20→5 etkisi gerçek; diğer 3'ünde zaten doğru parça önde. Dört sorunun kopya doluluğu aynı parçayı 5 kez prompt'a koyuyor; bu veri artefaktı (kaynak kopyaları), kod hatası değil. TASK'taki "vector 7. → rerank 1." örneği bu veriyle birebir üretilemedi; yerine yukarıdaki 16.–20. → 1. örnekleri var. Önceki turdaki canlı kayıtlar (tur 9–15) da aynı kopya tabloyu gösteriyor.

Not: puanlar assistant-api'nin kendi çağrısından değil, aynı model ve uca doğrudan istekten (assistant-api `RERANK` turlarında ev kredisi 5,19 çıkarken burada 3,08: farklı belge metni/ön işleme, assistant-api'nin gönderdiği metin biçimi ile benim `content` doğrudan göndermem farklı). Sıralama yönü tutarlı, mutlak puanlar karşılaştırılmamalı.

Kalan Optional: kopya kaynakların temizlenmesi önerilir (kullanıcı onayıyla); işi durdurmaz. Önceki Required "kopyaları kaldır" maddesi yerine karşılaştırma kopyalara rağmen yapılabildiği için Optional'a indirildi.

## Düzeltme turu 2
Temiz veri: `sources` tablosunda yalnız 10001 "Ürün ve Hizmet Rehberi" (kopyalar silindi, lead). Karşılaştırma assistant-api'nin gerçek turları üzerinden: localhost:3000, strateji `RERANK`, 6 soru → tur 31–36, kaynak `turn_chunks` (20 aday, `vector_rank`, `prompt_position`, `rerank_score`). Parça = chunkNo/sayfa. Kopyalar gitti: 20 adayın hepsi farklı parça.

| Tur | Soru | Vector top-5 (vector sırası) | Rerank sonrası prompt (1→5), parantezde vector sırası ve puan | Değişti mi |
|---|---|---|---|---|
| 31 | Ev kredisini erken kapatırsam ceza öder miyim? | #75, #71, #70, #66, #76 | #75 (1; 5,09), **#71 4.10 Erken ödeme (2; 1,46)**, #70 (3; −4,79), #63 (9; −4,98), #72 (16; −5,11) | Evet: 4.–5. sıra vector 9. ve 16. parçalarla değişti; ilk üç aynı |
| 32 | Yurt dışında kartımla nakit çekersem komisyon var mı? | #25, #26, #36, #35, #128 | #25 (1; 2,81), #3 (8; 0,89), #35 (4; 0,57), #128 (5; 0,56), #141 (7; −0,07) | Evet: #26 ve #36 düştü, vector 8. ve 7. girdi, 8. sırada olan #3 2. sıraya çıktı |
| 33 | Vadeli hesapta faiz ne zaman yatar? | #39, #54, #52, #40, #42 | #39 (1; 2,08), #40 (4; 1,71), #52 (3; 0,35), #37 (10; −0,32), #54 (2; −0,57) | Evet: #40 vector 4.'den 2.'ye, #54 2.'den 5.'e, #37 vector 10. girdi |
| 34 | Hesabım bloke oldu ne yapmalıyım? | #98, #106, #66, #109, #28 | #98 (1; −1,32), #106 (2; −1,62), #109 (4; −2,94), #50 (9; −3,42), #28 (5; −4,35) | Evet: #66 (kredili mevduat, alakasız) çıktı, vector 9. girdi |
| 35 | Kredi başvurusu için hangi belgeler gerekir? | #58, #9, #57, #64, #59 | **#9 (2; 2,82)**, #58 (1; 2,76), #59 (5; 0,71), #57 (3; 0,68), #47 (7; 0,02) | Evet: 1.–2. yer değiştirdi (puan farkı küçük), #64 çıktı |
| 36 | Kartımı kaybettim, ne yapmalıyım? | #28, #29, #107, #27, #129 | #28 (1; 2,90), **#109 (9; 0,94)**, #29 (2; −0,19), #107 (3; −1,39), #36 (14; −2,22) | Evet: vector 9. 2. sıraya çıktı, 14. girdi |

Bulgular:
- Her 6 soruda rerank, vector top-5'ten farklı bir prompt kümesi/sırası üretti; 20 adayın hepsi puanlanmış (turn_chunks'ta 20 satırda da `rerank_score` dolu), yalnız 5'inde `prompt_position` var, puan sırası = prompt sırası (tur 31–36 elle kontrol edildi, ör. tur 33: 2,08 > 1,71 > 0,35 > −0,32 > −0,57).
- TASK örneği ("vector 7. → prompt 1."): gerçek veride erken ödeme parçası (#71, "4.10 Erken ödeme ve erken kapama") **vector 2.**'dir ve rerank'te **2.** kalır; vector 7. olan parça #143 (10.3 Kredi ücretleri) elendi. Örneğin sayısı bu veriyle birebir tutmuyor; davranış (rerank'in alakasız/alakalı parçaları yeniden sıralaması) doğru. Erken ödeme parçası zaten ilk iki sırada olduğu için örnek senaryo gösterilemedi; nedenini araştırmadım, kod hatası belirtisi görmedim.
- Yön doğru: alt sıradaki alakalı parçalar öne geldi (tur 32 #3 8.→2., tur 36 #109 9.→2.), alakasız vector üst parçalar elendi (tur 34 #66 kredili mevduat). Tur 35'te 1.–2. yer değişimi puan farkı 0,06 ile sınırda.
- `top5` karşılığı (strateji değiştirmek için yeniden başlatma gerekeceğinden tur kaydından okundu): her turun "Vector top-5" sütunu = `TOP_K` stratejisinde prompt'a girecek parçalar. Önceki turdaki gerçek `TOP_K` turu (tur 8, kayıt) sadece yapı olarak doğrulanmıştı; bu turda tekrar koşmadım.
- Cevaplar (gerçek model): 31 "sabit faizlide %1/%2, değişkende ceza yok [1]", 32 "yurt dışı ATM %1,5, en az 75 TL [1]", 33 "faiz vade sonunda [1][2]", 34 şifre/bloke yanıtı [1] (bloke sorusunda rehber alakasız parçalarla zayıf; rerank puanları negatif, model şifre sıfırlamaya yöneldi), 35 kimlik + gelir belgeleri [1][2], 36 kayıp kart kanalları + sorumluluk [1]. Alıntılar ilgili parçaları gösteriyor.
- Süreler (retrieval / rerank): 160–741 ms / 1198–2117 ms (önceki kopyalı veriyle 463–2825 / 1706–2828; arama kısa). Not: tur 31'de üretim 43 sn sürdü (nedenini ölçmedim).

Önceki "kopya kaynaklar" Optional maddesi kapandı. Yeni Critical/Required yok.

STATUS: DONE

