TASK: TASK-031
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`, `java-code-review`, `springboot-verification`, `rag-pipeline`, `sse-streaming`, `frontend-conventions`, `writing-good-tests`, `e2e-testing`.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Alıntı sadece dayanılan parçaya; karşılığı olmayan mesajda alıntı/düğme/✓ yok | `AnswerFinalizer`, `RerankScoreGate`, etiket yolu; M3'te 55 negatifin hedefli olmayan 40'ında alıntı 0; UI'da Selam/Bitcoin/Teşekkürler düğmesiz | ⚠️ Kısmen: bir soru sonrası **listede olmayan teşekkür** alıntı alıyor (aşağıda, Required) |
| 2 | Bulamadım tek cümle, numara ekranda yok, tur BİLMİYOR | `NotFoundAnswerDetector`, `AnswerFinalizer`, `NotFoundAnswerStreamFilter`; M3: Bitcoin/hava/şiir/asdfgh vb. hepsi sadece cümle, alıntı 0; Konuşmalar'da BİLMİYOR | ✅ |
| 3 | Selamlaşma: kısa nazik karşılık, alıntı yok; tur SOHBET | `SmallTalkDetector`, `MessageLabeler`; M3: 18/18 SMALL_TALK (13 PATTERN, 5 LABEL, "Çok makbule geçti" dahil), tek başına | ✅ (takip sorusu sonrası hâli: kural 1 satırı) |
| 4 | Karar kullanıcıdan (eşik + talimat + denetim) | Kart "Kullanıcının verdiği kararlar" 1-10, ONAY: VERİLDİ | ✅ |
| 5 | Birinci tekil şahıs | `rag-system.st`; M3 `secondPersonSelfReference` 0 (155 cevap) | ✅ |
| 6 | Golden bozulmaz, ≤ 2 bulamadım | M3: 100/100 cevaplı, bulamadım 0, alıntılı 100, doğru parçayı alıntılayan 92 (M0 90), `paraphrasedNotFound` 0, giriş cümlesiyle başlayan 1 (≤ 2) | ✅ |
| 7 | Talimatlar Türkçe | `prompts/*.st` | ✅ |
| K1 | Hedefli 15 mesaj: sadece bulamadım ya da giriş cümlesi + numarasız | M3 `targetedOk` 15/15, `openerHasCitation` 0 | ✅ |
| K2 | Hedefli alıntılı cevaplarda alıntı parçaya dayanır (elle) | Aşağıda 3 cevap, parçalar retrieval-api'den okundu | ✅ dayanmayan 0 |
| K3 | Hedefli olmayan 40 negatifte alıntılı 0 | M3 `untargetedWithCitations` 0 | ✅ |
| K4 | Deterministik test + build | `./mvnw verify` 597 test geçti | ✅ |
| K5 | Önizleme/Konuşmalar lab'la aynı | Ekran görüntüleri: fark sadece SOHBET etiketi ve eşik satırı | ✅ |

### Elle okuma (3 alıntılı hedefli cevap; chunk metni `GET /api/workspaces/42/chunks/{id}`)
- "Başka bankanın kartı sizinkinden daha mı avantajlı?": giriş cümlesi + "banka kartı yıllık ücret almaz, temassız… [2]" → parça 4 (2.1) "yıllık ücret alınmaz… temassız… yurt dışında kullanılabilir" ✓; "maaş müşterileri başka banka ATM'lerinden ayda 5 kez ücretsiz nakit çekebilir [3]" → parça 38 "Başka banka ATM'lerinden ayda 5 adet ücretsiz nakit çekme" ✓.
- "Şimdi altın almalı mıyım?": "gram cinsinden birikim… fiziki altın teslimi yapılmaz; satışta karşılığı TL… [1]" → parça 44 ✓.
- "Hangi fonu almamı önerirsiniz?": "600'den fazla fon… fon türleri… [1]" → parça 113 ✓.
Dayanmayan alıntı: **0**. (Soruya yanıt olarak ilgisi zayıf ama parçaya dayanıyor.)

## Sözleşme uyumu
- `openapi.yaml` / `sse.md` ↔ backend: `done.answer`, `done.answerSource` (MODEL, EMPTY_KNOWLEDGE_BASE, BELOW_RERANK_THRESHOLD, SMALL_TALK), `TurnStatus.SMALL_TALK` birebir (Java `AnswerSource` enum, `DoneEvent`). Canlı akışta alanlar geldi (M3 betiği ayrıştırdı).
- Web: `ANSWER_SOURCES`, `TURN_STATUSES`, `isDoneEvent` doğrulaması sözleşmeyle aynı; ekran `done.answer`'a geçiyor (UI'da numarasız son metin görüldü). Uyumsuzluk yok.
- Not: akış sırasında giriş cümlesi arkasındaki numara bir an görünür (aşağıda).

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 597 test, 0 hata, JaCoCo eşiği geçti | Evet |
| `cd web && npm run build` | geçti | Evet |
| `cd web && npm run lint` | uyarısız | Evet |
| `cd web && npm test` | 46 dosya, 748 test geçti | Evet |
| `E2E_BASE_URL=http://localhost:5173 npm run test:e2e` (Vite dev + Docker backend; :3000 açık değildi) | 14 geçti, 2 kaldı, 13 skip, 2 çalışmadı. Kalanlar: `shell.spec` 404 testi (Vite dev server nginx gibi davranmıyor) ve `sources.spec` yükleme (dialog kapanmadı; TASK-031 dışı dosyalar, nedeni incelenmedi) | Raporda "çalıştırılamadı" idi |
| `acceptance.spec.ts` | Çalıştırılmadı: `E2E_ACCEPTANCE=1` ve boş bilgi bankası ister (`down -v`, kullanıcı onayı gerekir). Karşılığı olan senaryolar elle/betikle denendi (aşağıda) | — |

## Mutation testi
- PIT (tam paket, `test-compile org.pitest:pitest-maven:mutationCoverage`): 927 mutant, 812 öldü, **%88** (eşik %80), BUILD SUCCESS; test gücü %93. TASK-031 sınıflarında hayatta kalan: `SmallTalkDetector` 55/60/134 (sınır/aritmetik/boş filtre), `NotFoundAnswerStreamFilter` 46/62/70 (`setLength`, sonrası okunmuyor, eşdeğer). `AnswerFinalizer`, `NotFoundAnswerDetector`, `MessageLabeler`, `RerankScoreGate`, `CitationParser`'da hayatta kalan yok. Önemli mutant yok.
- Stryker (web): skor **%83,32** (eşik %80), geçti.

## Uçtan uca
Ölçüm (gerçek modeller, rehber 2026.10, eşik −4,0, `handoff/measure/qa-m3.json`; 155 mesaj, son kod: imaj kaynak dosyalarından sonra değişmedi):
- Golden 100: cevaplı 100, bulamadım 0, alıntılı 100, doğru parçalı 92, hata 0.
- Negatif 55: hedefli 15/15 `targetedOk`; hedefli olmayan 40'ta alıntı 0; selam 18/18; konu dışı 5/5, anlamsız 4/4 bulamadım ("???" modelden, eşik üstü, yine sadece cümle); takip senaryosu "Gold…" → "Teşekkürler" alıntısız (PATTERN).
- Giriş cümlesi arkası numara akışta görünüyor: hedefli 15'te 13 (M3 `streamedOpenerHasCitation`); `done` ile silinip ekran temiz metne dönüyor. Kartta kabul edilmiş risk; UI'da son metin temiz.
- "İşletme kredisi faiz oranınız ne kadar?": yine yanlış giriş cümlesi ("Başka bankaların…") + ardından "Rehberde sadece tüketici kredileri… işletme kredisi ile ilgili genel bir bilgi bulunmamaktadır." Alıntı yok, kural 1 sağlanıyor; model parçasız bir "bulunmamaktadır" iddiası yazıyor (bulamadım kalıbı "rehberimizde" içermediği için yakalanmıyor). Optional.
- UI (Playwright, Vite dev): "Selam" → hazır karşılık, düğme yok; "Bitcoin…" → sadece bulamadım + "Temsilciye bağlan", yan kartta "Bu turda modele hiçbir parça gitmedi…"; "Gold kart aidatı" → alıntılı; "Teşekkürler" → hazır karşılık, alıntısız, yan kart gizli. Konuşmalar: gri SOHBET, BİLMİYOR, DAYANAKLI görünüyor.
- **Kartın risk maddesi (soru sonrası listede olmayan teşekkür), gerçek akış, elle:**

| Önceki soru | Mesaj | Sonuç |
|---|---|---|
| Gold kart aidatı ne kadar? | Çok makbule geçti | "Bu konuda rehberimizde bilgi bulamadım." (alıntısız ama teşekkür karşılığı değil) |
| Kartımı kaybettim, ne yapmalıyım? | Çok makbule geçti, sağ olun | "Rica ederim, memnuniyetle yardımcı oldum **[1]**." |
| Gold kart aidatı ne kadar? | Çok işime yaradı sağ olun | "Rica ederim, memnuniyetle yardımcı oldum! **[1]**" |
| aynı | Allah razı olsun | "Rahatsız etmediniz, size yardımcı olmaktan mutluluk duydum. **[1]**" |
| aynı | Yardımların için minnettarım | "Rica ederim, yardımcı olabildiğime sevindim! **[1]**" |
| aynı | Harikasınız, eyvallah | "Teşekkür ederim, memnuniyetle yardımcı oldum! **[1]**" |
Neden: takip sorusu yeniden yazma teşekkürü önceki soruya çevirmiş (`searchQuery`: "Gold Kart'ın yıllık üyelik ücreti ne kadar…"), puan eşiğin üstünde kalmış, model teşekkür cevabına uydurma `[1]` eklemiş. 6 denemenin 5'inde alıntı çıktı.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| **Required** | `AnswerService` (takip yeniden yazma) + `SmallTalkDetector`/`MessageLabeler` | Kalıp listesinde olmayan teşekkür/selam, bir cevaplı turdan sonra gelirse yeniden yazma onu önceki soruya çevirir, eşik geçilir, etiket sorulmaz ve cevap "memnuniyetle yardımcı oldum [1]" gibi uydurma alıntı taşır. Kural 1 ve TASK.md'nin "Teşekkürler" senaryosunun varyantı ihlal ediliyor; kartın "Bittiğini nasıl anlayacaksın"ı yalnız listedeki "Teşekkürler" ile test ediyor. Kart bunu risk olarak yazmıştı ve qa'nın denemesini istemişti; sonuç 5/6 ihlal. Çözüm yönü planner'ın kararı (ör. yeniden yazmadan önce etiket sorusu ya da kısa mesajda eşiksiz etiket; ya da cevapta modelin alıntıyı dayanaksız saymak) | planner → backend-coder |
| Optional | `AnswerFinalizer` / giriş cümlesi | Giriş cümlesi arkasındaki numara akışta ~13/15 turda bir an görünüyor (kartta kabul) | — |
| Optional | `rag-system.st` kural 4 | "İşletme kredisi…" yanlış giriş cümlesi seçiyor, parçasız "bulunmamaktadır" iddiası; alıntı yok | — |
| Optional | testler | PIT hayatta kalan 6 mutant eşdeğer/önemsiz | — |
| FYI | ortam | e2e `sources.spec` testi çalıştığı için bilgi bankasında fazladan "E2E Rehber …" kaynağı (2 belge) kaldı; tekrar eden parçalar sonraki ölçümleri etkileyebilir. Silme ucu yok; temizlik `down -v` ile (kullanıcı onayı) | lead |

## E2E'de kalan iki test: neden
- **`shell.spec` 404 testi: ortam.** Vite dev server `/api/yok` için SPA HTML'i ve 200 döndürüyor (`curl localhost:5173/api/zzz` → `200 text/html`); test nginx'in 404 problem+json davranışını bekliyor. Docker'daki web (:3000) açıkken geçmesi beklenir; :3000 kapalı olduğu için doğrulanamadı. TASK-031 ile ilgisi yok.
- **`sources.spec` yükleme testi: uygulama hatası bulunmadı, neden kesin belirlenemedi.**
  - Tam koşuda diyalog 5 sn içinde kapanmadı. Ama retrieval-api log'u isteğin hemen kabul edildiğini gösteriyor (kaynak 10002 oluştu, iş 0,03 sn sonra kuyruğa girdi, 16 sn'de bitti). Diyalog `createSource.onSuccess`'te kapanıyor (`AddSourceDialog.tsx:75`).
  - Testi tek başına yeniden çalıştırdım: `sources.spec.ts` 4 test geçti, 1 skip (boş bilgi bankası değil). Yükleme testi 12,2 sn'de geçti.
  - Bilgi bankasında zaten belge olması engel değil: test farklı bir ad kullanıyor ve ad çakışması testi geçiyor. Sürüm etiketi 2026.10 başka kaynakta olsa da sorun çıkarmıyor.
  - Sonuç: kararlı bir hata yok; tam koşudaki tek seferlik kapanma gecikmesi büyük ihtimalle zamanlama ya da yük (Vite proxy üzerinden 1 MB multipart, Ollama meşgulken) kaynaklı. Kanıt: tam koşunun izi (trace) elimde yok, ikinci koşuda oluşan hata da yok. Bu task'ın dosyalarını (web/src/features/source) etkilemiyor.
  - Yan etki: bu yeniden koşu bilgi bankasına bir "E2E Rehber" kaynağı daha ekledi (toplam 3 belge: 1 gerçek rehber + 2 e2e). Temizlik `down -v` ile, kullanıcı onayıyla.

## Düzeltme
1. **planner → backend-coder (Required):** Kalıp dışı teşekkür/selam, cevaplı bir turdan sonra gelince alıntı almamalı (yukarıdaki 6 mesaj). Çözüm kararı planner'da: kullanıcı kartı etkileniyorsa onay gerekir. Doğrulama: yukarıdaki 6 takip mesajında alıntı 0 ve teşekkür karşılığı; golden ve 55 negatif yeniden ölçülür (golden ≤ 2 bulamadım korunur). tester: yeni tur için "soru → kalıp dışı teşekkür" testi.

## Düzeltme turu 2
Adım 1-7 baştan uygulandı. Ortam: sistem `down -v` ile sıfırlanmış, bilgi bankasında tek kaynak (rehber 2026.10, 154 parça) + golden set; imaj turu 2 kodundan sonra derlenmiş (sonrasında `src/main` değişmedi). Ölçüm `handoff/measure/qa-m6.json`.

| Madde | Kanıt | Sonuç |
|---|---|---|
| Required: cevaplı sorudan sonra kalıp dışı teşekkür uydurma alıntı alıyor | Önceki 6 mesajın 6'sı `SMALL_TALK` `NO_SEARCH`, hazır metin, alıntı 0 (M6). Bende hiç görülmemiş 6 yeni mesaj: "Eline sağlık, çok yardımcı oldunuz", "Bu kadar yeter, sağ olun", "Mükemmel, bayıldım", "İyi günler dilerim", "Hayırlı olsun", "Teşekkür ederim sizlere" → 6/6 `SMALL_TALK`, `citations` [] | ✅ |
| Kısa gerçek takip sorusunda yanlış selam yok | 8 kısa + 7 uzun (7-10 kelime) takip sorusu: 15/15 `MODEL`, hepsi cevaplı ve alıntılı, selam 0 | ✅ |
| Backend sapması: arama öncesi etiket `OTHER` çıkınca etiketin her cevap yolunda tura yazılması | `AnswerService`: saklı `preRewriteLabel` boş-bilgi-bankası ve model yolunda `draft(..., labelOf, latencyOf)` ile yazılıyor; eşik altında yeniden sorulmuyor (tek çağrı). Kısıt `ck_conversation_turns_small_talk` sadece `SMALL_TALK`'ı bağlıyor. "Her adımın sonucu tura kaydedilir" ilkesiyle uyumlu, sözleşmede (SSE/REST alanları) değişiklik yok; sapma kabul | ✅ |

### Ölçüm (M6; golden 100 + 80 negatif, hepsi yeniden koşuldu, örneklem değil)
- Golden: 99 cevaplı, bulamadım 0, alıntılı 99, doğru parçalı 91, `paraphrasedNotFound` 0, giriş cümlesiyle başlayan 1 (≤ 2). **1 hata** (`RETRIEVAL_UNAVAILABLE`, "Ekspertiz raporu ne kadar süre geçerli?", 29 sn, log: `ResourceAccessException`): geçici altyapı zaman aşımı; aynı soru yeniden sorulunca 9,7 sn'de "3 ay geçerlidir [1]" ile doğru cevap geldi. Bulamadım'a düşen yok, sınır sağlanıyor.
- Hedefli 15: `targetedOk` 15/15, `openerHasCitation` 0.
- Hedefli olmayan tek turlu 40 negatif: alıntı 0. (Toplam "alıntılı hedefli olmayan 15" sayısı yalnız meşru takip soruları: `follow-up-question` 8 + `-long` 7.)
- Selam 18/18; konu dışı 5/5, anlamsız 4/4 bulamadım; takip "Teşekkürler" alıntısız; `secondPersonSelfReference` 0.
- Takip senaryoları 26: `follow-up-small-talk` 10/10 `SMALL_TALK` alıntı 0 ("Her şey için sağ olun, iyi akşamlar" ve "Tamamdır, kolay gelsin" veda metni); `follow-up-question` 8/8 ve `-long` 7/7 cevaplı.

### Komutlar
| Komut | Sonuç | Raporla tutarlı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 615 test, 0 hata | Evet |
| `cd web && npm run build` / `npm run lint` / `npm test` | geçti / çıkış 0 / 46 dosya 748 test | Evet (web turu 2'de değişmedi) |
| PIT tam paket | 945 mutant, 830 öldü, **%88**, test gücü %93, BUILD SUCCESS. `AnswerService`, `MessageLabeler` ve yeni `isShortMessage`'da hayatta kalan yok; kalan 3 `SmallTalkDetector` mutantı (satır 56, 61, 143) tur 1'dekiyle aynı, eşdeğer/önemsiz | — |
| Stryker | Yeniden çalıştırılmadı: `web/src` ve `web/e2e` turu 2'de değişmedi (tur 1'de %83,32) | — |
| `E2E_BASE_URL=http://localhost:3000 npm run test:e2e` (ölçümden sonra, Docker'daki web) | **18 geçti, 0 kaldı**, 13 skip (acceptance: `E2E_ACCEPTANCE=1` ve boş bilgi bankası ister). Tur 1'de kalan `shell` 404 testi ve `sources` yükleme testi bu koşuda geçti: ilki gerçekten Vite dev ortamı sorunuydu, ikincisi tek seferlik zamanlama | — |

### Görünüm
Turda web değişmedi; arama öncesi etiketle gelen selamlaşma ekranda kalıp listesindekiyle aynı bileşenden geçiyor (`answerSource` `SMALL_TALK`). Tur 1'de lab'la karşılaştırma yapılmıştı (fark sadece SOHBET ve eşik satırı); e2e görünümü doğruladı.

### Kod review (tur 2)
| Severity | Dosya | Bulgu | Rol |
|---|---|---|---|
| Optional | `message-label.st` | "İyi günler dilerim" selam metni ("Merhaba, ben…") aldı; veda olması daha doğru. Alıntı yok, kural bozulmuyor | — |
| Optional | `AnswerService` | Cevaplı turdan sonra gelen, ≤ 10 kelimelik, `?` içermeyen her mesaj bir ek model çağrısı (etiket) yapıyor; gerçek bir kısa takip sorusu yanlış etiketlenirse hazır teşekkür alır (ölçümde 15 takipte 0) | — |
| FYI | ortam | Bilgi bankası şu an 2 kaynak içeriyor (rehber + e2e'nin eklediği "E2E Rehber"); temizlik kapanışta `down -v` ile kullanıcı kararı | lead |

**Kalan risk:** Kart her girdi için garanti vermiyor; model etiketi ya da cevabı beklenmedik yazarsa alıntı doğrulaması (numaranın gerçekten o parçaya dayanması) otomatik değil, kapsam dışı.

STATUS: DONE
