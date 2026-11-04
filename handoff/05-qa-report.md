TASK: TASK-032
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`, `java-code-review`/`springboot-verification` (komutlar ve diff), `rag-pipeline`, `rag-evaluation`, `writing-good-tests` (mutation yorumu), `e2e-testing`/`webapp-testing` (ekran denemesi).

Önkoşul: 8090'ın yeni kodla çalıştığı doğrulandı ("???" → `answerSource` `NO_WORDS`, 24 ms; "Selam" → `SMALL_TALK`). Ölçümler 8090'da, ısınmış modelle, başka yük yokken alındı (`handoff/measure/m3-qa.json`, 199 mesaj, hata 0).

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Kelime listesi ve eşleştirme kalktı | `small-talk/*.txt` ve `SmallTalkDetector(+Test)` silinmiş (`git status` staged D); `AnswerService` artık `ShortMessageDetector` kullanıyor; `./mvnw verify` 600 test geçti | ✅ |
| 2 | Kısa mesaj her zaman, ilk mesaj dahil, aramadan önce etiketlenir; selam/teşekkür/veda → sabit metin, arama ve alıntı yok | `AnswerService.answerWithRetrieval` (`!history.isEmpty()` koşulu kalkmış); M3: 40 selam/teşekkür/veda/isim mesajının 40'ı `SMALL_TALK`, route `NO_SEARCH`, alıntı 0; ekranda Selam/Günaydın/Ahmet denendi; Nber, Nasılsın?, Teşekkürler, Görüşürüz API'den denendi | ✅ |
| 3 | Kısa mesaj tanımı (≤10 kelime, `?` varsa ≤5) | `ShortMessageDetector`, `ShortMessageDetectorTest`; M3 `small-talk-question` 5/5 yakalandı | ✅ |
| 4 | Eşik altı uzun mesaj etiketlenir; selam → sabit metin, diğer → bulamadım | `AnswerService.answerBelowThreshold`; M3: not-in-guide/off-topic/gibberish 23 mesaj `BELOW_RERANK_THRESHOLD` ya da modelin "bulamadım"ı | ✅ |
| 5 | Eşik ölçülerek seçilir, kullanıcı onaylı | Kart karar 6: -4,0; M1 tablosu `m1-no-threshold.json`; M3'te golden en düşük puan -2,74, konu dışı en yüksek -0,46 (model yoluna geçen "İşletme kredisi…" bile bulamadım verdi) | ✅ |
| 6 | Giriş cümleleri sadece kendi konusunda | `rag-system.st` kural 3-4; M3 `wrongOpener` 0, golden'da giriş cümlesiyle başlayan 0, hava/şiir/Bitcoin/jjkbjknlk'da yok. **Ama** aşağıdaki B1: "Gold kart aidatı ne kadar? → ya kredi kartında" cevabının ortasında kişisel-bilgi cümlesi çıkıyor (5/5 tekrar) | ❌ |
| 7 | Hesap bilgisi sorusunda genel bilgi alıntısıyla eklenir | "Kredi limitim ne kadar" 3/3 denemede cümle + 2.6'ya dayanan `[2]` (parça başlığı "2.6 Kart limiti…" elle doğrulandı). "Kredi kartı limitim ne kadar?" 2.6 en üst parça olduğu halde 3/3 sadece giriş cümlesi (bkz. O1) | ✅ (TASK örneği) |
| 8 | Parçaya dayanmayan cümleye numara yok | Giriş cümlelerinde `openerHasCitation` 0; selam/isim cevaplarında `[n]` 0; 59 alıntılı cevabın alıntıları `citedHeadings` ile elle karşılaştırıldı (örnek: "Ek kart ücretli mi" → ek kart bölümü; "Platinum" → Platinum ücreti) | ✅ |
| 9 | Yeniden yazma asistan cevabını almaz; bağımsız soru aynen kalır | `QueryRewriter.historyText` sadece `turn.question()`; `query-rewrite.st` Türkçe; M3 `follow-up-independent` 3/3: "Selam >> Hava durumu", "Ek kart… >> Hava durumu", "…>> Bana bir şiir yaz" → bulamadım, giriş cümlesi yok; "Ek kart nasıl alabilirim >> Peki ücretli mi" → doğru cevap `[2]` | ✅ |
| 10 | Talimatlar Türkçe, yeni model yok | `query-rewrite.st`, `message-label.st`, `rag-system.st` Türkçe; dependency/model değişmedi | ✅ |
| S | Senaryolar: Selam, Merhaba, Günaydın, Nber, Nasılsın?, Teşekkürler, Görüşürüz | M3 37 selam/teşekkür/veda 37/37 sabit metin, alıntı 0; Nber ve Günaydın M3'te dahil | ✅ |
| S | "Ahmet" | M3: Ahmet, Ben Ayşe, Adım Mehmet → selam karşılığı, alıntı yok (kart karar 4); ekranda Ahmet denendi | ✅ |
| S | "Bitcoin alabilir miyim?", "jjkbjknlk", "???" | bulamadım, alıntı yok; "???" `NO_WORDS`, ekranda "Bu konuda rehberimizde bilgi bulamadım.", modele giden parçalar kartı görünmedi, Konuşmalar'da "BİLMİYOR" (`DONT_KNOW`) | ✅ |
| AC2 | Rehberde olmayan konular listesinde (13 mesaj) yanlış giriş cümlesi ve alıntılı cevap 0 | M3 `not-in-guide` 13/13 bulamadım, alıntı 0, giriş cümlesi 0; `off-topic` 6/6, `gibberish` 5/5 aynı | ✅ |
| AC3 | Golden recall@5 ve bulamadım'a düşen sayısı | `fallen` 0/100 (sınır 2), 100/100 alıntılı, `citedContainsAnswer` 92. Değerlendirme ekranı ölçümü backend raporunda (recall@5 0,99, MRR 0,9125, kaçan 1); ben tekrar koşmadım (değerlendirme eşikten etkilenmiyor, kodu değişmedi) | ✅ |
| AC4 | Kelime listesi kodu kalktı, testler güncel, build geçer | Aşağıda komutlar | ✅ |
| AC5 | Önizleme ve Konuşmalar görünümü lab'la aynı | Web diff'i sadece `types.ts` (`NO_WORDS`) ve `PreviewPage.tsx`'te tek koşul; stil ve metin değişmedi. Ekran görüntüleri (Önizleme açılış, Selam/??? /Ahmet turları, Konuşmalar listesi, SOHBET/BİLMİYOR rozetleri) lab'ın sol yarısıyla aynı düzen ve renkte | ✅ |

## Sözleşme uyumu
- `openapi.yaml` ↔ controller: alan, status ve `errorCode` değişmedi. `AnswerSource` enum'ında `NO_WORDS` sözleşmede, backend enum'ında (`AnswerSource.java`), V9 migration CHECK'inde ve web `ANSWER_SOURCES`'ında aynı. `TurnStatus` `DONT_KNOW` canlı API'den doğrulandı.
- `sse.md` ↔ backend olayları: `NO_WORDS` turunda `token` (tek, sabit cümle) → `done` (`answerSource` `NO_WORDS`, `notFoundAnswer` true, süreler null); `sources` olayı yok. Web ayrıştırıcısı `NO_WORDS`'ü kabul ediyor (`answerStreamEvents.validation.test.ts`), ekranda bu turda parça kartı gizleniyor.
- Not: `docs/api/` bu görevde `git diff`'te güncel; planner dışında kimse değiştirmemiş görünüyor.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 600 test, 0 hata; JaCoCo eşiği geçti | Evet |
| `cd web && npm run lint` | temiz | Evet |
| `cd web && npm run build` | geçti | Evet |
| `cd web && npm test` | 751/751 geçti | Evet |
| PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS; 919 mutant, 808 öldü (%88), test gücü %93; eşik 80 | Rapordaki hedef sınıf skoru %96 ile uyumlu (benimki tüm proje) |
| `E2E_BASE_URL=http://localhost:5173 npm run test:e2e` | 16 geçti, 13 atlandı, 2 kaldı (aşağıda) | Test raporunda E1 koşulmamıştı |
| `npm run test:mutation` (Stryker) | skor %83,23, eşik 80 geçti | Evet |

## Mutation testi
- PIT 1.30.1: %88 (808/919), eşik %80 geçti. Hayatta kalan mutant'lar tüm projedeki 111; bu görevin sınıfları için test raporundaki %96 (ShortMessageDetector, QueryRewriter, AnswerService) ve kapatılan 2 mutant raporla tutarlı. Önemli yeni hayatta kalan bulunmadı.
- Stryker (web): skor %83,23, eşik %80 geçti (kapsanan %85,08). Web değişikliği küçük (bir koşul + bir enum değeri), önemli hayatta kalan mutant yok.

## Uçtan uca
- **E2E (dev, 5173):** 2 başarısız:
  1. `shell.spec.ts` `/api/yok` → 404 Problem beklerken Vite dev server HTML dönüyor. Bu test nginx'e (Docker, 3000) özgü; dev modunda çalışması beklenmez, task ile ilgisiz. Docker kapalı olduğu için doğrulanamadı.
  2. `preview.spec.ts:88` ("Takip sorusu… yan kart") ilk koşuda PIT ile aynı anda çalışırken zaman aşımına düştü; tek başına yeniden koşuda geçti (2,3 dk). Yük kaynaklı, görev ile ilgisiz.
- **E1 (`acceptance.spec.ts` Selam adımı):** `E2E_ACCEPTANCE=1` ve boş bilgi bankası gerektirir; yüklü rehberi/golden seti silmemek için koşmadım. Yerine aynı Selam adımını Playwright ile Önizleme'de denedim (aşağıda). Bu bir kısıt, bulgu değil.
- **Ekran denemesi (Playwright, 5173):** "Selam" ve "Günaydın" → sabit selam, numara ve Kaynak düğmesi yok; "Ahmet" → sabit selam metni, alıntı yok, "Faydalı mı?" görünür; "???" → "Bu konuda rehberimizde bilgi bulamadım.", parça kartı yerine bugünkü bilgilendirme kartı. Konuşmalar: bu konuşma "SOHBET", önceki konu dışı konuşma "BİLMİYOR" rozetiyle listelenir. Detayda "???" turu `DONT_KNOW`.
- **Gözlem (FYI):** Ben PIT çalışırken denediğimde ekrandaki ilk "Selam" turu `GROUNDED` ve modelin kendi yazdığı "Merhaba, Raşit Bankası'nın dijital asistanıyım…" oldu: etiket çağrısı yük altında 10 sn'yi aştı, kart karar 3 gereği "diğer" sayıldı. Yük yokken Selam, Günaydın, Nber, Nasılsın?, Teşekkürler, Görüşürüz 6/6 `SMALL_TALK` aldı. Planda ve riskte yazılı davranış; tek kullanıcılı yerelde sorun değil.

### Gerçek model cevap örnekleri (M3)
| Mesaj | Cevap | Alıntı |
|---|---|---|
| Selam / Günaydın / Nber / Nasılsın? | "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. … Ne sormak istersin?" | yok |
| Teşekkürler / Görüşürüz | "Rica ederim. …" / "Görüşmek üzere. …" | yok |
| Ahmet | selam metni | yok |
| Selam >> Hava durumunu verir misin? | "Bu konuda rehberimizde bilgi bulamadım." | yok |
| Kredi limitim ne kadar | "Hesabına özel bilgileri (…) göremiyorum. Kart limitiniz gelirinize, başka bankalardaki borçlarınıza ve KKB kayıtlarınıza göre belirlenir [2]." | [2] = 2.6 Kart limiti |
| Limitimi nasıl artırabilirim? | "…Raşit Mobil uygulamasında Kartlarım > Limit işlemleri ekranından talepte bulunabilirsiniz […] [1]" | [1] = 2.6 |
| Ek kart nasıl alabilirim >> Peki ücretli mi | "Evet, Raşit Classic ve Raşit Gold'a bağlı ek kartlar ücretsizdir [2]. … Platinum … 750 TL [2]." | [2] |
| Bitcoin alabilir miyim? / jjkbjknlk / ??? | "Bu konuda rehberimizde bilgi bulamadım." | yok |
| Gold kart aidatı ne kadar? >> ya kredi kartında | "Kredi kartı hakkında genel bilgiler için rehberde detaylar bulunmaktadır [4]. Ancak hesabınıza özel bilgileri (…) göremiyorum." | [4] (içeriksiz) |

Takip soruları (16): hiçbiri hazır karşılık almadı; 15'i alıntılı model cevabı, 1'i ("Hangi kartlarda yok") bulamadım.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Required | `assistant-api/src/main/resources/prompts/rag-system.st` kural 4 | TASK kural 6 ("Konu dışı başka her soruda bu cümleler çıkmaz") ihlali, tekrarlanabilir (5/5): konuşma "Gold kart aidatı ne kadar?" → "ya kredi kartında" cevabı içeriksiz bir cümle + `[4]` ve ardından "Ancak hesabınıza özel bilgileri (…) göremiyorum." Kullanıcı kendi hesabını sormuyor. Ölçüm betiği bunu yakalamıyor: `wrongOpener` yalnızca cevabın başlangıcına bakıyor, bu cümle cevabın ortasında; M2/M3'te `wrongOpener` 0 çıktığı için gözden kaçmış. Ölçüm "başka konunun giriş cümlesi hiç yok" kriterini (kart) bu haliyle kanıtlamıyor | backend-coder |
| Optional | `rag-system.st` kural 4 | "Kredi kartı limitim ne kadar?" 2.6 en üst parçadayken (3,35) 3/3 sadece giriş cümlesi; kural 7 "genel bilgi varsa eklenir" bu varyantta uygulanmıyor. TASK örneği ("Kredi limitim ne kadar") geçiyor; model kararsızlığı | backend-coder |
| Nit | `AnswerService.java` sınıf Javadoc'u | Satır kırılması düzensiz (üçüncü satır çok uzun) | backend-coder |
| FYI | `web/e2e/shell.spec.ts:42` | Test dev server'da çalışmıyor (nginx'e özgü); Docker'da çalıştığı varsayımı | — |
| FYI | `measure_answers.py` `ANSWER_LISTED_CATEGORIES` | `follow-up-question` kategorisi giriş cümlesi denetlemesine girmiyor; B1 ile aynı kök | backend-coder |

## Düzeltme
**backend-coder:**
1. B1: "Gold kart aidatı ne kadar? >> ya kredi kartında" (ve benzeri, konu dışı ya da hesap bilgisi sormayan takip soruları) cevabında "Hesabına özel bilgileri…", "Başka bankaların…", "Yatırım tavsiyesi…" cümlelerinden biri çıkmayacak şekilde `rag-system.st` düzeltilsin (ör. giriş cümlelerinin sadece müşterinin son mesajı doğrudan o konuyu soruyorsa kullanılacağı; cevabın ortasında da yazılmayacağı açıkça belirtilsin). Gerçek modelle 5 tekrarda cümle çıkmamalı.
2. Ölçüm betiği: giriş cümlelerinden birinin cevabın herhangi bir yerinde ve o kategoriye ait olmayan mesajda geçmesini (`wrongOpener` ya da yeni bir `openerAnywhere`) say; takip soruları (`follow-up-*`) dahil. M3'ü bu betikle yeniden koş; sayı 0 olmalı.
3. Backend raporu (`02-backend-implementation.md`) `## Düzeltme turu 1` ile güncellensin; tester gerekirse `PromptBuilderTest`'e kuralın yeni metnini kontrol eden test eklesin.

STATUS: STALE
