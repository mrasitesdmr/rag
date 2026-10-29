TASK: TASK-001
# Frontend uygulama raporu

## Kullanılan skill'ler
- `frontend-conventions` (Durum/Ürün hedefi notu, bölüm 4 ağaç, bölüm 5 adresler, bölüm 13 komutlar: `web/CLAUDE.md` bunlarla tutarlı tutuldu)
- `react-patterns` (Önizleme formu örneği: `aria-label="Soru"`, "Sorunu yaz…", "Gönder"; bu task'ta kod yok, sadece tutarlılık için okundu)
- Doküman task'ı olduğu için iş → skill tablosundan ek skill yüklenmedi (plan "Frontend planı": "Ek skill yok"). `sse-streaming`, `rag-evaluation`, `rag-pipeline` klasörlerinin varlığı `ls .claude/skills` ile doğrulandı.

## Yapılanlar
F-1: `web/CLAUDE.md` yeni dosya; eski `frontend/CLAUDE.md`'nin içeriği taşındı ve plana göre tamamlandı. Eski dosyaya dokunulmadı (L-16 lead'in).

| Eski `frontend/CLAUDE.md` bölümü | Yeni `web/CLAUDE.md`'de | Plan maddesi | Not |
|---|---|---|---|
| Yorum bloğu (AMAÇ, İçerik, kurallar skill'de, Durum) | Yorum bloğu | F-1.1 | `frontend/` → `web/`; Durum: "iskelet ilk web task'ında Vite react-ts şablonuyla kurulur"; İçerik satırına "ürün hedefi" eklendi |
| Başlık `# Frontend (React)` | `# web (React yönetim paneli)` | F-1.1 | |
| — (yoktu) | `## Ürün hedefi` | F-1.2 | M1 + M4 kelimesi kelimesine (plan satır 109 ve 128 ile `grep -xF` ile birebir doğrulandı) |
| Teknoloji: sürüm hedefi | Teknoloji, 1. madde | F-1.3 | Aynı |
| Teknoloji: runtime ve test paketleri | Teknoloji, 2. madde | F-1.3 | Aynı |
| Teknoloji: SSE native fetch | Teknoloji, 3. madde | F-1.3 | Aynı; parantezine `sse-streaming` eklendi |
| Teknoloji: Referans uygulama | Teknoloji, 4. madde | F-1.3 | Plandaki metin: "`../embedding-pgvector/frontend`: Bilgi kaynakları, yükleme ve içe alma ekranlarının çalışan örneği; okunur kalıp referansı, değiştirilmez" (M5'teki tek izinli atıf) |
| Teknoloji: Henüz olmayanlar | Teknoloji, 5. madde | F-1.3 | Aynı |
| Teknoloji: Vite proxy `/api` → backend | Teknoloji, 6. madde | F-1.3 | Plandaki metin (8090/8080, `vite-patterns`, `sse-streaming`); sona "Docker'da aynı yönlendirmeyi nginx yapar (`deployment/CLAUDE.md`)" eklendi (M2) |
| Teknoloji: `kaynaklar/*.html` başlangıç içeriği | Teknoloji, 7. madde | F-1.3 | Plandaki metin: sol yarı ürünün kendisi, `rag.html` anlatım, değiştirilmez |
| Klasör yapısı: ağaç | Klasör yapısı: ağaç | F-1.4 | Kök `web/` |
| Klasör yapısı: feature adları = backend paketleri | Klasör yapısı, 1. madde | F-1.4 (Ç-1) | "servis paketleriyle aynı: `conversation`, `evaluation` → assistant-api; `source` → retrieval-api"; `preview` sadece web'de, assistant-api'nin `conversation` API'si |
| Klasör yapısı: feature import yasağı | Klasör yapısı, 2. madde | F-1.4 | Aynı |
| Klasör yapısı: hedef adresler | Klasör yapısı, 3. madde | F-1.4 | Adresler aynı (`frontend-conventions` bölüm 5 ile aynı parametre adları); plandaki ek cümle: "Adreste çalışma alanı yoktur (M6). Bilgi kaynakları çağrıları `/api/workspaces/42/...`." |
| Klasör yapısı: test dosyasının yeri | Klasör yapısı, 4. madde | F-1.4 | Aynı |
| Komutlar (8 madde + giriş) | Komutlar | F-1.5 | Giriş "Hepsi `web/` içinde çalıştırılır."; dev server maddesinde "Backend açık olmalı" → "assistant-api (8090) ve retrieval-api (8080) açık olmalı" (Ç-1); E2E maddesi aynı (3000 / 5173) |
| Dosya sahipliği (tablo + 2 madde) | Dosya sahipliği | F-1.6 | Aynı |
| Kurallar: `frontend-conventions` bağlayıcı | Kurallar, 1. madde | F-1.7 (Ç-1) | "frontend/ altında" → "web/ altında" |
| Kurallar: API type'ları sözleşmeyle aynı | Kurallar, 2. madde | F-1.7 | "`docs/api/openapi.yaml` ile (iki servis, path önekleri: `/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api)" |
| Kurallar: formatter yok, biçim | Kurallar, 3. madde | F-1.7 | Aynı |
| Tuzaklar (giriş + 9 madde) | Tuzaklar | F-1.8 | Aynı |
| Hangi iş → hangi skill (13 satır) | Hangi iş → hangi skill (14 satır) | F-1.9 | "Server state, cache, akan cevap, canlı veri" satırına `sse-streaming`; yeni satır "Değerlendirme ekranı → `rag-evaluation`"; "Yeni ekran ya da görsel tasarım" → "Lab'dan ekran ve token aktarma, lab'da karşılığı olmayan durum → `frontend-design`, `web-design-guidelines`"; diğer satırlar aynı |

Eski dosyanın her bölümünün yeni dosyada karşılığı var; düşen içerik yok.

## Değişen dosyalar
- Yeni: `web/CLAUDE.md`
- Yeni: `handoff/02-frontend-implementation.md` (bu rapor)
- Değişen / silinen: yok (`frontend/CLAUDE.md`'ye dokunulmadı; L-16 lead'in)
- Eklenen paket: yok

## Ekran metinleri
Bu task'ta ekran yok. `web/CLAUDE.md`'de geçen lab metinleri: "Canlı yapılandırma" rozeti, "Ölçümü çalıştır · rerank 20→5" (M4'ten). Lead'in bildirdiği Önizleme metinleri (`aria-label="Soru"`, "Sorunu yaz…", "Gönder") `web/CLAUDE.md`'de geçmiyor; `react-patterns` örneğindedir.

## Plandan sapmalar
Yok. Plana göre küçük uyarlamalar (sapma değil, Ç-1 / M2 kapsamında):
- Teknoloji proxy maddesinin sonuna Docker'daki nginx yönlendirmesine bir cümle eklendi (M2: "dev'de Vite proxy, Docker'da nginx").
- Komutlar'da "Backend açık olmalı" iki servisin adı ve portuyla yazıldı.
- Klasör yapısında "backend paketleri" iki servisin paketlerine göre açıldı.

Not (tutarsızlık, benim dosyam değil): `frontend-conventions` bölüm 2 "UI metni yazımı" ve bölüm 11'deki test örneği hâlâ "Soruyu gönder" diyor; lead'in kararıyla `react-patterns` örneği lab'daki "Gönder"i kullanıyor. Skill'ler lead'in (sahipsiz); lead'e mesajla iletildi.

## Doğrulama
İskelet yok, komut çalıştırılmadı; kontroller qa'da. (`npm` ve `docker` çalıştırılmadı.)

Kendi ön kontrolüm (repo kökünden, sadece `web/CLAUDE.md` üzerinde; qa'nın yerine geçmez):
- K3 `grep -n "embedding-pgvector"` → 1 satır, "Referans uygulama" satırı (M5'e uygun).
- K4 regex'leri → 0 satır.
- K5 `grep -niE "workspace|çalışma alan"` → 3 satır: proxy maddesi (`/api/workspaces` adres öneki), hedef adresler maddesi (M6 cümlesi + `/api/workspaces/42/...`), Kurallar maddesi (`/api/workspaces/**` adres öneki). Üçü de plandaki F-1.3, F-1.4, F-1.7 metinleri; hepsi adres ya da M6 cümlesi.
- K6 `grep -cF "sol yarısının aynısı"` → 1; `grep -c "ekrana gelmez"` → 1.
- K12 portlar → 8090 assistant-api, 8080 retrieval-api, 5173 Vite, 3000 site (M3 anlamında).
- K13 `sse-streaming`/`rag-evaluation` → 4 satırda geçiyor.
- K19 eski yol regex'i → 0 satır.
- M1 ve M4: plan satır 109 ve 128 (`> ` öneki atılarak) `grep -qxF` ile dosyada tam satır olarak bulundu.

## Tester için notlar
Bu task'ta tester atlandı (plan "Çalışacak roller"). Test edilecek kod yok.

STATUS: DONE
