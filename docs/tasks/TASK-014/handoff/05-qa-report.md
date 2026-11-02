TASK: TASK-014
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Ek olarak sistem Docker ile kurulup Playwright ile ekran görüntüleri alındı (webapp-testing yaklaşımı).

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|------------------------|--------------------|-------|
| 1 | Örnek/seed konuşma yok; boş durum Türkçe | V6 migration seed içermiyor; liste gerçek turlardan (canlı: `GET /conversations` sadece gerçek 20 kayıt/sayfa). Test: `ConversationListPage.test#givenNoConversations…EmptyState…`, `ConversationReadControllerIntegrationTest#givenNoConversations…` | ✅ |
| 2 | Liste: sütunlar, 60 karakter kısaltma, en yeni üstte, `PageResponse`, menü sayacı | Canlı ekran görüntüsü (cnv_114 üstte, sütunlar lab'la aynı); `givenConversations…LabCells`, `givenThreeListedConversations_whenPageSizeTwo…`, `MainNav.test#givenThreeConversations…`. Canlı sayaç `totalElements` ile aynı (109) ve soru bitince arttı (101→102) | ✅ |
| 3 | Detay: breadcrumb, başlık `cnv_N`, "Kanal · saat · N tur", sıra no+soru, arama sorgusu satırı, alıntılı cevap, çip, üretim, rehber, token, süre, parça tablosu, Sadece LLM uyarısı, satıra tıklayınca pencere, geri bildirim/aktarma | Canlı cnv_107 (2 tur), cnv_104 (aktarma satırı + BİLMİYOR), cnv_109 (Sadece LLM uyarısı). Testler `ConversationDetailPage.test` F7–F12, F17 | ✅ |
| 4 | Süren tur "işleniyor" | Canlı: cevap sürerken liste `lastTurnStatus: IN_PROGRESS`, detay `inProgressTurn` dolu; bitince `DONT_KNOW`, `inProgressTurn: null`. Test: `givenInProgressConversation_whenPolled…`, `givenRecordedTurnAndActiveTurn…` | ✅ |
| 5 | Tur durumu kuralları (kart 1A 2A 3A 4A 5A), belirlenemeyen gösterilmez | `TurnStatusResolver` + `TurnStatusResolverTest` (15 satır, kural sırası), `givenOneTurnPerStatusRule…`, PIT %100. Canlı: bulamadım → BİLMİYOR, alıntılı → DAYANAKLI, "Hayır" → WRONG (liste dahil, "Evet"e dönünce eski çipe döndü), `GENERATION_MODE=llm` → DAYANAKSIZ, hata → TAMAMLANMADI (conv 93). KAÇIRDI/ÜRETİM HATASI üretilmiyor (kart gereği TASK-016) | ✅ |
| 6 | Bulunamayan konuşma/tur Türkçe mesaj | Canlı 404 `CONVERSATION_NOT_FOUND`; e2e `givenUnknownConversation_whenOpened_thenNotFoundMessage` geçti; bozuk id 400 `BAD_REQUEST`, `size=0` 400 `VALIDATION_FAILED` | ✅ |
| AC-1 | Kurallar kullanıcı onayıyla ve testle | Onay kartı `ONAY: VERİLDİ`, kararlar 1A 2A 3A 4A 5A; test eşlemesi 03-test-report'ta | ✅ |
| AC-3 | Görünüm lab'ın sol yarısıyla aynı; build'ler geçer | Ekran görüntüsü karşılaştırması aşağıda; komutlar geçti | ✅ |

## Sözleşme uyumu
- `openapi.yaml` (`ConversationSummaryResponse`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`) ↔ canlı cevapların alan adları ↔ `web/src/features/conversation/model/types.ts`: alan adları, null'lanabilirlik ve enum değerleri (`IN_PROGRESS`, `DONT_KNOW`, `WRONG`, `INCOMPLETE`, `UNGROUNDED`, `GROUNDED`) aynı. Eksik/fazla alan yok.
- Status/errorCode: 200 / 400 `VALIDATION_FAILED` / 400 `BAD_REQUEST` / 404 `CONVERSATION_NOT_FOUND`, RFC 9457 gövdesi.
- retrieval-api sözleşmesi ve SSE değişmedi (SSE olayları `token`, `sources`, `done` canlı gözlendi).

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|--------------------|
| `docker compose -f deployment/docker-compose.yml up --build -d` | 4 servis healthy | ✅ |
| `cd assistant-api && ./mvnw verify` | BUILD SUCCESS, 354 test, 0 hata/başarısız | ✅ (354) |
| `cd web && npm run build` | başarılı | ✅ |
| `cd web && npm run lint` | 0 uyarı | ✅ |
| `cd web && npm test` | 35 dosya, 587 test geçti | ✅ |
| `cd web && npm run test:e2e` (sistem açık) | 18 geçti, 1 atlandı (`givenEmptyKnowledgeBase…`, bilgi bankası dolu olduğu için), 0 kaldı; `conversations.spec.ts` 2/2 geçti | rapor "çalıştırılamadı" demişti, şimdi çalıştı |
| `retrieval-api verify` | çalıştırılmadı: bu task'ta değişmedi | – |

## Mutation testi
- PIT (assistant-api; `TurnStatusResolver`, `ConversationReadService`, `ActiveTurnRegistry`, `pagination.*`): 41 mutant üretildi, 41 öldürüldü (%100), hayatta kalan yok. Eşik karşılanıyor.
- Stryker: kurulu değil.

## Uçtan uca
Gerçek model (qwen3.5:9b, bge-m3, reranker :8081) ve yüklü rehberle, API üzerinden ve tarayıcıyla:
- **M1:** liste/detay 200/400/404 sözleşmeyle uyumlu; boş konuşma (soru yok) listede görünmedi (conv 107 açıldığında sayaç değişmedi, sorular bitince 101→102); seed yok. Boş veritabanı durumu birim/bileşen testinde (F2), canlı sistemde veri dolu olduğu için denenemedi.
- **M2 (cnv_107):** "Kartımı kaybettim, ne yapmalıyım?" → "Yenisi kaç günde gelir?": listede en üstte 2 tur, Önizleme, sayaç arttı. Cevap 1: "Kartınızı vakit kaybetmeden … kapatın [1] … 3-5 iş günü içinde … [1]"; cevap 2: "Yeni kartınız il merkezlerinde 3 iş günü, diğer yerlerde 5 iş günü içinde adresinize gönderilir [1]." Alıntı 1. parçayı (s. 16, "Kayıp ya da çalıntı bildirimi") gösteriyor, cevaba uygun.
- **M3:** 2. turun "Arama sorgusu: Kartımı kaybettim, yeni kartım ne zaman (kaç gün içinde) elime ulaşır?" satırı görünüyor. Detay tablosu `turn_chunks` ile birebir aynı: tur 141 prompt [1..5] → vector 10, 9, 8, 12, 7; tur 140 → 3, 5, 2, 4, 6 (psql sorgusuyla doğrulandı).
- **M4:** "Kripto para hesabı açabilir miyim?" → "Bu konuda rehberimizde bilgi bulamadım." → BİLMİYOR (alıntı yok, parçalar modele gitti, tabloda Atıf boş). "Hayır" geri bildirimi → tur WRONG, liste son durumu WRONG; "Evet" → GROUNDED'a döndü. `GENERATION_MODE=llm` → "Kart aidatı ne kadar?" modelden uydurma cevap, DAYANAKSIZ + "Bu turda modele hiçbir parça gitmedi (Sadece LLM)." uyarısı (mod sonra eski haline döndürüldü). Cevap sürerken liste/detay IN_PROGRESS, bitince kendiliğinden BİLMİYOR.
- **M5 (görsel):** Lab'ın liste ve detay ekranı ile yan yana karşılaştırıldı (ekran görüntüleri: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/7b71dd87-da1a-4d07-adcc-b18246848131/scratchpad/lab-list.png`, `app-list.png`, `lab-detail.png`, `app-detail.png`, `app-detail-dk.png`, `app-detail-llm.png`). Renkler, sütun başlıkları, çip tonları, breadcrumb, başlık/alt başlık, bilgi satırı, alıntı bloğu, parça tablosu ve sarı uyarı lab'la aynı. Uygulama lab'daki cihaz çerçevesi yerine tam genişlikte (sağ yarı/çerçeve ürüne girmiyor, beklenen).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional | `web/src/app/MainNav.tsx` (sayaç rozeti) | Tester'ın gözlemi doğrulandı: bağlantının metni `Konuşmalar109` (boşluksuz). Rozet ayrı öğe olduğu için görsel olarak doğru; ekran okuyucuda bitişik okunabilir. Çözüm: rozetten önce görünmez boşluk ya da rozete `aria-label`. Durdurmaz | frontend-coder |
| FYI | `web/.../ConversationDetailPage` parça tablosu | Lab'da Parça sütunu başlığı da gösterir ("s. 20 · Döviz ve altın alım satım · #38"); uygulamada TASK.md kuralı gereği "sayfa · sıra no" (`s. 16 · #28`). TASK kuralına uygun, sapma değil | – |
| FYI | e2e `sources.spec` boş bilgi bankası testi | Veri dolu olduğu için atlanıyor; bu task'la ilgisiz | – |

Critical/Required bulgu yok.

## Düzeltme
Gerekmiyor.

STATUS: DONE
