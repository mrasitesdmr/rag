TASK: TASK-007
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Ek skill yüklenmedi: kararlar komut çıktılarına, canlı sisteme ve diff'e dayanıyor.

## Kural kontrolü
Kod ve test eşlemesi `handoff/01-plan.md` "Kural eşlemesi" tablosundaki gibi. Aşağıda, kendi çalıştırdığım kanıt var.

| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç |
|---|---|---|---|
| K1 | Liste sütunları, durum, bilgi kutusu (backend'den) | Canlı `GET /api/workspaces/42` → `bge-m3`, 1024, 1600/2000/200, HNSW, COSINE. Ekran görüntüsünde tablo: Kaynak (ad + dosya), "PDF · 70 sayfa", Sürüm, Parça, "Hazır", Son senkron. Web testleri F1-F4 geçti | ✅ |
| K2 | Belge ekle: düğme, pencere, Vazgeç / Yükle ve işle, zorunlu sürüm, hata mesajları, odak | Ekran görüntüsü: "Belge ekle" pencerelerde dosya, Kaynak adı ("Boş bırakırsan dosya adı kullanılır."), Sürüm etiketi var. Esc sonrası odak "Belge ekle" düğmesinde (kendim denedim). E2E E1, E2, E3 geçti (yer imsiz PDF'te düzeltme mesajı ve yeni satır yok; aynı adda "ad alınmış" mesajı) | ✅ |
| K3 | Yeniden dene | Web testleri F4, F5. Listede "Ollama Kapalı …" kaynağı "Hazır": devops'un Ollama kapalı denemesi (`04-deploy-report.md`) sonuçlanmış | ✅ |
| K4 | Boş durum | Web testi F1 geçti. Canlı sistemde 15'ten fazla kaynak olduğundan E0 atlandı. Veri silmediğim için canlıda denemedim | ✅ (yalnız birim testle) |
| K5 | Detay: breadcrumb, başlık, alt başlık, "PDF'i aç", meta kutuları | Ekran görüntüsü: 6 kutu (Sayfa / bölüm 70 / 11, Parça 154 · ort. 285 token, 1.600-2.000 karakter, 200 karakter, bge-m3 · 1024, HNSW · cosine). `GET .../file` → 200, `application/pdf`, `inline`, `no-store`. E2E E4 geçti | ✅ |
| K6 | İşler tablosu | Ekran görüntüsü: İş, Dosya, Sürüm, Parça, Süre, Durum "DONE". Web testi F18; `id DESC` sorgusu backend testinde | ✅ |
| K7 | Parçalar: sütunlar, sunucuda filtre, 12'lik sayfa, N / M, boş durum | Canlı: `page=1&size=12` → ilk `chunkNo` 12, 12 kayıt. `query=asgari ödeme` → 8 sonuç, başlıklar eşleşiyor. Web testleri F19, F20; E2E E1 geçti | ✅ |
| K8 | Parça penceresi: başlık, bilgi şeridi, metin, embedding önizlemesi, Esc / odak | `ChunkResponse` alanları (chunkId, chunkNo, tokenCount, embeddingModel, sourceName, version, 8 değerlik `embeddingPreview`). Web testi F21; E2E E1 | ✅ |
| K9 | Polling | `useIngestionJobsPolling`; testler F3, F24, F28 geçti | ✅ |
| K10 | `errorCode` → Türkçe; kaynak yoksa mesaj | Canlı 404 `SOURCE_NOT_FOUND`. Web testleri F23, F25, F26. Sözleşmedeki bazı genel kodların (`BAD_REQUEST`, `INTERNAL_ERROR` …) özel Türkçe karşılığı yok, genel mesaja düşüyor | ✅ |
| K11 | HTML yorumlanmaz | `dangerouslySetInnerHTML` yok; web testi F22. PDF yanıtında `nosniff` var | ✅ |
| AC1 | Lab ile aynı görünüm | Lab (sol yarı) ile yan yana ekran görüntüsü: bilgi kutusu, tablo, Hazır rozeti, breadcrumb, başlık, meta kutuları, işler tablosu, parçalar, filtre kutusu aynı bileşenler ve renkler. Fark: lab telefon çerçevesinde dar, burada tam genişlik; kutular tek sırada. Lab'daki "Yeni sürüm yükle" yok (kapsam dışı). | ✅ |
| AC2 | "Belge ekle" gerçek PDF'le uçtan uca | E2E E1 (rehber PDF'i, Hazır, filtre, sayfalama, parça penceresi) gerçek Ollama ile geçti, 10 sn | ✅ |
| AC3 | Filtre ve sayfalama sunucuda | Canlı istekler yukarıda; B11-B13 | ✅ |
| AC4 | `verify`, `build`, `lint` | Komutlar bölümü | ✅ |

Örnek senaryolar: boş durum (birim testle), rehber yükleme (E1), yer imsiz PDF (E2), Ollama kapalı (devops raporu), aynı ad (E3), "asgari ödeme" filtresi (canlı), parça penceresi + Esc (E1).

## Sözleşme uyumu
- `openapi.yaml` ↔ DTO: `WorkspaceResponse`, `ChunkSummaryResponse`, `ChunkResponse` alan adları ve sırası birebir aynı; `SourceResponse` canlı çıktısı sözleşmedeki alanlarla aynı (`latestJob` dahil).
- Path ve status: `/api/workspaces/{id}`, `.../sources/{id}`, `.../file`, `.../ingestion-jobs`, `.../chunks`, `.../chunks/{chunkId}` canlıda dönüyor. 404 `SOURCE_NOT_FOUND` ve 400 `VALIDATION_FAILED` (`violations[].field = query`) RFC 9457 biçiminde.
- Web type'ları (`ChunkResponse` vb.) backend alanlarıyla aynı. Web, uçları nginx üzerinden aynı adresle çağırıyor.
- assistant-api değişmedi; SSE ve WebSocket yok.
- Sözleşmede bir hata görmedim.

## Komutlar (kendi çalıştırdıklarım)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd retrieval-api && ./mvnw verify` | Çıkış kodu 0, 332 test, 0 hata | ✅ (332) |
| `cd web && npm run build` | Geçti | ✅ |
| `cd web && npm run lint` | 0 uyarı, 0 hata | ✅ |
| `cd web && npm test` | 14 dosya, 238 test geçti | ✅ |
| `E2E_BASE_URL=http://localhost:3000 npm run test:e2e` | 10 geçti, 1 atlandı (E0, liste dolu) | ✅ |
| PIT (`chunk`, `source`, `workspace`) | Aşağıda | ✅ |
| assistant-api | Değişmedi, çalıştırılmadı | — |

## Mutation testi
- **Backend PIT:** `chunk.*`, `source.*`, `workspace.*`: 140 mutant, 124 öldü (%89), 11 kapsamsız, test gücü %96, 387 test. Tester'ın ilk turdaki açığı (`ensureWorkspaceExists` çağrısının silinmesi) kapatılmış. Kalanlar veri bütünlüğü savunması olan `IllegalStateException` dalları ve bir zaman aşımı; kural dalı yok.
- **Frontend Stryker:** Kurulu değil (`web/CLAUDE.md`), çalıştırılmadı.

## Uçtan uca
- Playwright, Docker'daki sisteme karşı: yukarıdaki sonuçlar. E4'te headless Chromium PDF'i çizmiyor; test yeni sekmenin açıldığını ve PDF'in ayrı istekle `200 application/pdf` geldiğini doğruluyor. PDF'in gözle görünmesini "Kendin dene" bölümü kapsar.
- Elle: `/kaynaklar` ve `/kaynaklar/10001` ekran görüntüsü alındı, lab ile karşılaştırıldı. "Belge ekle" penceresi açılıp Esc ile kapatıldı: odak düğmeye döndü. Başlık yapısı: h1, "İçe alma işleri" h2, "Parçalar" h2.
- Dil modeli içeren adım yok (bu task'ta soru-cevap yok).
- Not: canlı sistemde E2E koşularından kalan kaynaklar var ("E2E Rehber …", "Ollama Kapalı …"). Bunları kendin silemezsin (silme kapsam dışı); veritabanı sıfırlamak kullanıcı kararı.

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Optional | `Dialog` (pencere başlığı h3) | **Lead sorusu 1, karar: kabul edilebilir istisna.** Detay sayfasında h1 → h2 → pencere h3 sırası zaten doğru. Liste sayfasında "Belge ekle" penceresi h1'den sonra h3 atlıyor. Ekran okuyucu için moderate seviyede en iyi uygulama, kullanımı engellemiyor; lab ile aynı. Kural iki testte kapalı, diğer axe kuralları açık ve geçiyor. İsteyen `Dialog` başlığını h2 yapıp iki test istisnasını silebilir | frontend-coder |
| Optional | nginx + `SourceController` | **Lead sorusu 2.** `X-Content-Type-Options: nosniff, nosniff` canlıda her yanıtta çift. Zararsız (tarayıcı iki değeri de `nosniff` okur, PDF yanıtında da aynı). Tekilleştirmek istenirse nginx'te `proxy_hide_header` | devops |
| Optional | `AddSourceDialog` | **Lead sorusu 3.** Sunucu hatasından sonra odak ilk hatalı alana gitmiyor, çünkü alanlar o sırada pasif. Hata `role="alert"` ile okunuyor, mesaj alanın altında; kullanıcı yine tıklayabilir. TASK'taki odak kuralı (açılış, Esc, dönüş) sağlanıyor. Kural ihlali değil | frontend-coder |
| FYI | `ChunkController`, `ChunkResponse` | **Lead sorusu 4.** `@Validated` olmaması doğru karar: canlıda 201 karakterlik `query` → 400 `VALIDATION_FAILED`, `field=query`. `EMBEDDING_PREVIEW_SIZE` DTO'da, planda serviste; iki yerde aynı değer, davranış doğru | backend-coder |
| Nit | `ChunkController` doğrulama mesajı | `query` ihlalinin mesajı "size must be at most 200 characters" diyor (alan adı yanlış). Kullanıcıya gösterilmiyor (Türkçe mesaj `errorCode`'dan) | backend-coder |
| FYI | `04-deploy-report.md`, canlı veri | Canlıdaki `averageTokenCount` 285 (planın değeri), testteki fixture'da 283. Fark veriden, hata değil | — |
| FYI | Test | Frontend mutation (Stryker) yok; web'in %98 satır coverage'ı var | — |

Critical ve Required bulgu yok.

## Düzeltme
Gerekmiyor.

## Kendin dene
`http://localhost:3000` adresinde:
1. **Bilgi kaynakları**'na tıkla. Tabloda belgeleri, altında "Çalışma alanı Raşit Bankası · Dijital Asistan (ws 42) · embedding modeli bge-m3@1024 …" kutusunu gör.
2. Sağ üstteki **Belge ekle**'ye bas → **Dosya seç** ile `docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf` dosyasını seç → Sürüm etiketine `2026.10` yaz, adı istersen boş bırak → **Yükle ve işle**. Pencere kapanır, yeni satır aşama adlarını gösterir ve birkaç saniye içinde **Hazır** olur. Aynı adı tekrar yüklemeyi dene: pencerede Türkçe hata çıkar.
3. Yer imi olmayan bir PDF (ör. Word'den başlık stili kullanmadan kaydettiğin) seç: pencerede belgeyi nasıl düzelteceğini anlatan mesaj çıkar, listeye satır eklenmez. **Vazgeç** ya da **Esc** ile kapat; odak **Belge ekle** düğmesine döner.
4. Bir belgenin satırına tıkla. Üstte dosya adı, bayt ve sürümü, meta kutularını ve **İçe alma işleri** tablosunda `DONE` durumunu gör. **PDF'i aç** yeni sekmede PDF'i açar.
5. **Parçalarda ara** kutusuna `asgari ödeme` yaz: sadece başlığında ya da metninde geçen parçalar kalır (ör. "2.8 Asgari ödeme ve gecikme"). Kutuyu temizleyip **Sonraki**'ne bas: "2 / 13" gibi bir sayfa ve 12 yeni parça gelir. Anlamsız bir şey yaz: "Eşleşen parça yok."
6. Bir parçaya tıkla: pencerede başlık, chunk_id, chunk_no, token, model ve kaynak ile tam metin ve embedding'in ilk değerleri ("… N değer daha") görünür. **Kapat** ya da **Esc** ile kapat; odak tıkladığın satıra döner.

STATUS: DONE
