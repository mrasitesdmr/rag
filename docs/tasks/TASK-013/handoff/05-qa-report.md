TASK: TASK-013
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Bulgular `java-code-review`, `springboot-verification` ve `frontend-conventions` ölçütlerine göre değerlendirildi; bu skill'ler ayrıca yüklenmedi.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| 1 | Evet/Hayır seçilir, seçili düğme vurgulanır | UI: Hayır `aria-pressed=true`, Evet `false` (Playwright ile gerçek sistemde); `PreviewPage.feedback.test.tsx` F2/F3; e2e E1 | ✅ |
| 2 | Son seçim geçerli, aynı düğme seçimi değiştirmez | `FeedbackService` + `TurnRepository.updateFeedback`; gerçek sistem: `true` sonra `false` → DB `helpful=f`; `FeedbackControllerIntegrationTest` B2/B3; web F4 (aynı düğmede istek yok) | ✅ |
| 3 | Sadece tamamlanmış tura geri bildirim, sabit `errorCode` | `FeedbackService` (`TURN_NOT_COMPLETED` 409, `TURN_NOT_FOUND` 404); V5 `ck_conversation_turns_feedback_completed`; B4/B17; gerçek sistem: olmayan tur 404 `TURN_NOT_FOUND` | ✅ |
| 4 | Eşzamanlı iki geri bildirim tutarlı | Tek atomik `UPDATE` (sadece 2 kolon); `FeedbackServiceIntegrationTest` (20 tekrar); gerçek sistem: aynı tura 15 paralel istek → hepsi 204, DB'de tek değer | ✅ |
| 5 | Cevabın saati geri bildirim satırında | Ekran görüntüsünde "Faydalı mı? Evet Hayır 14:46"; test F1 | ✅ |
| 6 | Düğme sadece "cevap yok" turunda, sebep `NO_ANSWER` | `HandoffService` (`HANDOFF_NOT_ALLOWED` 409); gerçek sistem: cevaplı turda 409, "cevap yok" turunda düğme 1 adet, cevaplıda yok; B10, F7 | ✅ |
| 7 | Konuşma başına tek aktarma, tekrar isteği 202 ve aynı kayıt | `uq_handoffs_conversation` + `insertIfAbsent`; gerçek sistem: 2 ardışık istek → aynı `createdAt`, 1 satır; 15 paralel istek → hepsi 202, 1 satır | ✅ |
| 8 | 202, kuyruk adı ve sebep | Gerçek yanıt: `{"conversationId":101,"turnId":132,"reason":"NO_ANSWER","queue":"bireysel","createdAt":...} 202`; DB `handoffs` satırı `bireysel` | ✅ |
| 9 | Aktarılan konuşmada soru sorulabilir (onay kartı: 1A) | Gerçek sistem: aktarmadan sonra yeni soru 71 token ile cevaplandı; `HandoffThenSendMessageIntegrationTest` | ✅ |
| 10 | Kuyruk assistant DB'de kayıt, sahte temsilci cevabı yok | V5 `handoffs` tablosu; `HandoffControllerTest#givenHandoff_whenRequested_thenNoTurnIsAdded`; ekranda sadece "Temsilci kuyruğundasın" metni | ✅ |
| 11 | Konuşmalar ekranı işareti (TASK-014) | Bu task'ın dışı; veri kaydediliyor (`helpful`, `feedback_at`, `handoffs`) | ✅ (kapsam dışı) |
| 12 | `errorCode` → Türkçe eşleme | `web/src/shared/api/errorMessages.ts` (3 yeni kod); testler F5, F10 | ✅ |
| 13 | Örnek senaryo 1 (Kartımı kaybettim: Evet → Hayır) | Gerçek modelle API ve tarayıcıda denendi; turda `helpful=f` | ✅ |
| 14 | Örnek senaryo 2 (aktarma → "Temsilci kuyruğundasın", `bireysel`) | Tarayıcıda "Kripto para hesabı açabilir miyim?" → "Bu konuda rehberimizde bilgi bulamadım." → düğme → "Temsilci kuyruğundasın", düğme kalktı; DB `bireysel`. Bkz. FYI-1 (Bitcoin sorusu) | ✅ |
| 15 | Örnek senaryo 3 (çift aktarma → tek kayıt) | Yukarıda #7 | ✅ |
| 16 | Yarıda kalan/hatalı cevapta iki satır da çıkmaz | Web testleri F6 (`givenStreamingTurn…`, `givenFailedTurn…`) | ✅ |
| 17 | Görünüm lab'ın sol yarısıyla aynı | Lab kaynağıyla karşılaştırıldı: satır sırası (önce aktarma, sonra "Faydalı mı?" + saat), metinler ("Temsilciye bağlan", "Temsilci kuyruğundasın", toast'lar "Geri bildirim kaydedildi" / "…; bu tur incelemeye düştü") birebir; ekran görüntüsünde düğme ve satır biçimi lab'la uyumlu | ✅ |

## Sözleşme uyumu
- `openapi.yaml` ↔ controller: `POST /api/assistant/turns/{turnId}/feedback` (204; 400/404/409/415), `POST /api/assistant/conversations/{id}/handoff` (202; 400/404/409/415). Gerçek yanıtlar sözleşmeyle eşleşti: 204, 202 gövde alanları (`conversationId, turnId, reason, queue, createdAt`), 404 `TURN_NOT_FOUND`, 409 `HANDOFF_NOT_ALLOWED`, 400 `VALIDATION_FAILED` (`helpful` / `reason` → `must not be null`). Hata gövdeleri `application/problem+json` ve `errorCode` var.
- web `previewApi.ts` ve type'lar aynı path ve alanları kullanıyor; tarayıcıda uçtan uca çalıştı (nginx üzerinden).
- Kontrol sırası 400 → 404 → 409 gerçek sistemde doğrulandı. SSE değişmedi; `sse.md` etkilenmiyor. retrieval-api etkilenmiyor.
- Sözleşme uyumsuzluğu bulunmadı.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 314 test, 0 hata; JaCoCo eşiği geçti | Hayır, sayı farklı: `03-test-report.md`'deki 311 eski. Tester sonradan `HandoffControllerTest`'e çevrilemeyen `turnId` testini (`"abc"`, `{}`, `[1]` → 400 `BAD_REQUEST`) ekledi ve raporu güncellemeden kapandı. Yeni testle birlikte geçerli sayı 314 (testi dahil ettiğini tekrar koşuda doğruladım: `HandoffControllerTest` 12 test geçti). Hepsi geçti, NEEDS_FIX sebebi değil |
| `cd web && npm run build` | geçti | Evet |
| `cd web && npm run lint` | geçti, uyarı yok | Evet |
| `cd web && npm test` | 30 dosya, 505 test geçti | Evet |
| `cd web && npm run test:coverage` | satır %97,2, ifade %97,15 (eşik %80) | Evet |
| `docker compose -f deployment/docker-compose.yml up --build -d` | 4 servis healthy (V5 migration uygulandı) | — |
| `cd web && E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | 16 geçti, 1 atlandı (`givenEmptyKnowledgeBase…` boş bilgi bankası gerektirir; bu koşulda beklenen), 0 kaldı; E1 `givenRunningSystem_whenAnswerDoneAndYesClicked_thenYesPressed` geçti | Evet (tester E1'i çalıştırmamıştı) |
| PIT (yeni sınıflar: `handoff.*`, `FeedbackService`, `FeedbackController`) | Hayatta kalan: 1 mutant (`HandoffService` log çağrısı/`created` bayrağı) + 3 kapsamsız (erişilemez `IllegalStateException`, `Handoff.getId`) | Evet |

## Mutation testi
- Backend: PIT 1.30.1, yeni sınıflar üzerinde çalıştırıldı. Tester'ın bildirdiği sonuçla uyumlu: ölenler %84 (16/19), hayatta kalanlar kurala ait değil (log içeriği; erişilemez dal; `getId`). Önemli mutant yok.
- Web: Stryker kurulu değil (`web/CLAUDE.md` "Henüz olmayanlar"); çalıştırılamadı.

## Uçtan uca
- Playwright e2e: yukarıda (E1 dahil geçti).
- Gerçek modelle (qwen3.5:9b, bge-m3, reranker) denendi:
  - "Kartımı kaybettim, ne yapmalıyım?" → cevap 72 token, alıntı [1] kayıp bildirimi parçasını gösteriyor; geri bildirim satırı ve saat var; Evet → Hayır sonrası Hayır vurgulu, iki toast görüldü; DB `helpful=false`. Cevap örneği: "Kartınızı vakit kaybetmeden aşağıdaki kanallardan biriyle kapatın: Raşit Mobil, İnternet Şubesi, Müşteri Hizmetleri veya şubeye gidin [1]. Kayıp bildirimiyle kart kalıcı olarak kapanır ve yeni kartınız 3-5 iş günü içinde adresinize gönderilir [1]."
  - "Kripto para hesabı açabilir miyim?" → "Bu konuda rehberimizde bilgi bulamadım." (sabit cümle, uydurma yok); "Temsilciye bağlan" çıktı, basınca "Temsilci kuyruğundasın" yazdı ve düğme kalktı; DB `handoffs` `bireysel`/`NO_ANSWER`.
  - Cevaplı soruda aktarma 409, düğme çıkmıyor; aktarmadan sonra yeni soru cevaplandı.
  - Modele giden parçalar tur kaydında/yan kartta görünüyor ve alıntı doğru parçayı gösteriyor.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| FYI | TASK.md örnek 2 | Rehber "Bitcoin alım-satımı yapılamaz" diyor; bu yüzden model "Bitcoin alıp satabilir miyim?" sorusuna dayanarak cevap veriyor (`notFoundAnswer=false`, bu cevap doğru). Bulamadım cevabı çıkmadığı için aktarma düğmesi görünmüyor. Davranış TASK-013 hatası değil; rehber içeriğinden kaynaklanıyor. Aynı akış "Kripto para hesabı açabilir miyim?" ile doğrulandı. | — |
| FYI | `HandoffService` `requestHandoff` | `insertIfAbsent` + ardından `findByConversationId`: eşzamanlılıkta 15 paralel istekte tek kayıt, hepsi 202. Doğru. | — |
| Nit | PIT | Log içeriğini test eden mutant hayatta; `backend-conventions` loglama kuralı gereği gerekmiyor. | — |

Critical ya da Required bulgu yok. Log'larda soru/cevap metni yok (sadece id, `helpful`, kuyruk, sebep).

## Düzeltme
Yok.

STATUS: DONE
