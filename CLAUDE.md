# RAG Asistan

## Proje özeti
RAG'i (Retrieval-Augmented Generation: dil modeline cevabı kendi verinle, kaynak göstererek yazdırmak) anlatan ve çalışan bir demoyla gösteren eğitim projesi. `kaynaklar/` altındaki iki HTML sayfası projenin başlangıç içeriğidir: `rag.html` (RAG anlatımı) ve `rag-lab.html` (Raşit Bankası dijital asistan yönetimi demosu: Önizleme, Konuşmalar, Bilgi kaynakları, Değerlendirme ve sistem konsolu). Hedef, lab'ı gerçek bir backend + frontend + veritabanı + lokal modeller üzerinde çalışan bir projeye dönüştürmektir.
Bu proje `../embedding-pgvector` projesinin devamıdır; bilgi bankası (retrieval-api) bu repoda yeniden yazılır (Mimari).

Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. "Aynısı" şu demektir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder.

Ürün dokümanı: `rag.md`. İş kuralı belirsizse önce onu ve ilgili `TASK.md`'yi oku.

## Mimari
- assistant-api: Spring Boot 4, Java 26, Maven, Spring AI → `assistant-api/`
- retrieval-api: Spring Boot 4, Java 26, Maven, pgvector → `retrieval-api/`
- Web: React 19, TypeScript, Vite → `web/`
- İletişim: REST (sözleşme: `docs/api/openapi.yaml`). Cevap akışı Server-Sent Events (sözleşme: `docs/api/sse.md`). Başka canlı güncelleme gerekirse STOMP over WebSocket (sözleşme: `docs/api/websocket.md`).
- Bilgi bankası **bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılır**. `../embedding-pgvector` çalışma zamanı bağımlılığı değildir; sadece okunur kalıp referansıdır: kodu toplu kopyalanmaz ve oraya yazılmaz. `assistant-api`, `retrieval-api`'ye REST ile gider (`POST /api/workspaces/{workspaceId}/search`). `web`'in Bilgi kaynakları ekranı `retrieval-api`'nin kaynak, sürüm, parça ve içe alma uçlarını aynı adres üzerinden (dev'de Vite proxy, Docker'da nginx) doğrudan çağırır (lab'daki gibi). İki servisin sözleşmesi `docs/api/openapi.yaml`'dadır; veritabanları ayrıdır.
- Veritabanı: Tek PostgreSQL sunucusu (pgvector'lü imaj), iki ayrı veritabanı: `assistant` (assistant-api: konuşmalar, turlar, değerlendirme çalıştırmaları) ve `retrieval` (retrieval-api: çalışma alanı, kaynaklar, parçalar ve embedding'leri, içe alma işleri). Flyway her serviste ayrıdır.
- Modeller: lokal, ücretsiz. Lab'da dil modeli ve embedding Ollama'da (`qwen3.5:9b`, `bge-m3`), reranker llama-server'da (`bge-reranker-v2-m3`). Kesin modeller kullanıcı onayıyla seçilir.
- Çalıştırma: Docker Compose → `deployment/`
- Durum: ürün çalışıyor (TASK-002…016, TASK-027): dört ekran gerçek veriyle, lokal modellerle (Ollama `qwen3.5:9b` ve `bge-m3`, llama-server `bge-reranker-v2-m3`) ve Docker Compose ile. Bilgi bankası boş açılır; belgeler "Belge ekle" ile yüklenir, golden set ve öneri listesi dosyayla yüklenir. TASK-018: uçtan uca doğrulama ve dokümanlar. Modüllerin güncel yapısı kendi `CLAUDE.md` dosyalarındadır.

## Klasör yapısı
| Yol | İçerik |
|-----|--------|
| `assistant-api/`, `retrieval-api/`, `web/`, `deployment/` | Modüller; her birinin kendi `CLAUDE.md`'si var |
| `kaynaklar/*.html` | Başlangıç içeriği: RAG anlatım sayfası ve lab demosu. Referanstır; task'lar bunları kaynak olarak kullanır |
| `ollama.md` | Lokal modellerin kurulum ve kullanım rehberi (TASK-003) |
| `docs/tasks/TASK-XXX/TASK.md` | Kullanıcının yazdığı iş talebi (şablon: `docs/tasks/_TEMPLATE.md`) |
| `docs/rehber/` | Raşit Bankası Ürün ve Hizmet Rehberi: Typst kaynağı, iki sürümün PDF'i (2026.10, 2026.11; 70 sayfa, 11 bölüm), golden set (`golden/rehber-golden-v1.json`), rehberde olmayan konular ve doğrulama betikleri. Bilgi bankasına "Belge ekle" ile yüklenir; açılışta kendiliğinden yüklenmez |
| `docs/api/openapi.yaml` | REST sözleşmesi, tek doğru kaynak (iki servis, path önekiyle: `/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api) |
| `docs/api/sse.md` | Cevap akışı sözleşmesi: endpoint, olay adları, sırası ve payload'lar |
| `docs/api/websocket.md` | STOMP sözleşmesi: destination'lar ve payload'lar (ilk WebSocket task'ında oluşturulur) |
| `handoff/` | Aktif task'ın rol çıktıları. Aynı anda tek task yürür. |
| `.claude/agents/` | Rol tanımları |
| `../embedding-pgvector` | Önceki proje; okunur kalıp referansı (içe alma, parçalama, pgvector, aynı yığın). Çalışma zamanı bağımlılığı değil. Hiçbir rol oraya yazmaz |

## Komutlar
- assistant-api derle + test: `cd assistant-api && ./mvnw verify`
- retrieval-api derle + test: `cd retrieval-api && ./mvnw verify`
- Web build (tip kontrolü dahil): `cd web && npm run build`
- Web lint: `cd web && npm run lint`
- Test, coverage, mutation ve uçtan uca test komutları modüllerin `CLAUDE.md` dosyalarındaki "Komutlar" bölümündedir; Docker komutları `deployment/CLAUDE.md`'de. Yeni komut kök dosyaya değil, ilgili modülün `CLAUDE.md`'sine yazılır.

## Genel kurallar
- Dil: kod, identifier, log ve commit mesajı İngilizce. Kullanıcıya görünen metin (UI, hata mesajı) Türkçe. Task, handoff ve doküman dosyaları Türkçe. Dil modeline giden talimatların ve modelin cevabının dili TASK'ta belirlenir.
- Hata mesajları: backend RFC 9457 Problem Details ve sabit bir `errorCode` döner, Türkçe metin üretmez. Türkçe mesaj frontend'de `errorCode` eşlemesiyle gösterilir. Akış başladıktan sonraki hata, akışın `error` olayında aynı `errorCode` ile bildirilir.
- Müşteri sorusu, cevap metni, parça içeriği ve prompt loglanmaz; sadece kimlik, uzunluk, token sayısı ve süre loglanır.
- İki servis birbirinin veritabanına bağlanmaz; REST ile konuşur.
- `docs/api/` altındaki sözleşmeleri (`openapi.yaml`, `sse.md`, `websocket.md`) sadece planner değiştirir. Sözleşmede eksik ya da hata gören rol kendisi düzeltmez, raporunda `NEEDS_FIX` ile bildirir.
- Her rol sadece aşağıdaki tabloda kendine ait yollara yazar. Başka bir yerde değişiklik gerekiyorsa raporda belirtir.
- Hiçbir rolün olmayan dosyalar: kök `CLAUDE.md`, kök `.gitignore`, `.claude/**`, `rag.md`, `kaynaklar/**`, `KULLANIM-KILAVUZU.md`, `docs/tasks/**/TASK.md` ve `../embedding-pgvector/**`. Bunları kullanıcı ya da kullanıcının onayıyla lead değiştirir.
- Planda olmayan dependency eklenmez. Gerekiyorsa raporda gerekçesiyle önerilir. Yeni lokal model (indirilecek model) de dependency gibi plan kararıdır.
- İşi bitti saymadan önce ilgili komutu çalıştır; komutu ve sonucunu (geçti/kaldı, test sayısı) rapora yaz. Çalıştırılmamış test "geçti" sayılmaz.
- Subagent'lar commit ve push yapmaz. Commit, kullanıcı onayıyla ana oturumda atılır. Format: `<type>(TASK-XXX): <summary>` (type: feat, fix, test, refactor, docs, chore).

## Ekip iş akışı (agent team + handoff)
Ana oturum **team lead**'dir. Task akışında kod yazmaz. Ekip üyelerini `.claude/agents/` tanımlarıyla (agent type) başlatır, ortak görev listesini kurar ve kullanıcı onayını yönetir. Ekip üyeleri lead'in konuşmasını görmez; başlatma prompt'una task yolunu (ör. `docs/tasks/TASK-001/TASK.md`) yaz.

- **Koordinasyon:** ortak görev listesi (sıra ve bağımlılık) ve doğrudan mesaj (soru, "dosya hazır" bildirimi).
- **Kalıcı kayıt:** `handoff/` dosyaları. Mesajlar oturumla kaybolur, dosyalar kalır. Bir rolün çıktısı handoff dosyasına yazılmadan geçerli sayılmaz.
- **İsim çakışması:** kodda `handoff` "temsilciye aktarma" özelliğidir (lab'daki `/handoff` endpoint'i). Bu bölümdeki handoff ise ekip içi dosya devridir; ikisi karıştırılmaz.

| Sıra | Rol | Okur | Yazar | Bağımlı olduğu adım |
|------|-----|------|-------|---------------------|
| 1 | planner | `TASK.md`, `docs/api/`, mevcut kod | `handoff/00-onay-karti.md`, `handoff/01-plan.md`, `docs/api/**` | — |
| 2 | **KULLANICI** (lead yönetir) | `00-onay-karti.md` | Onay ya da değişiklik isteği | 1 |
| 3a | backend-coder | `TASK.md`, `01-plan.md`, `docs/api/` | `assistant-api/`, `retrieval-api/` (test ve Docker dosyaları hariç), `handoff/02-backend-implementation.md` | 2 |
| 3b | frontend-coder | `TASK.md`, `01-plan.md`, `docs/api/` | `web/` (test ve Docker dosyaları hariç), `handoff/02-frontend-implementation.md` | 2 |
| 4 | tester | `TASK.md`, `01-plan.md`, `02-*.md` | Test dosyaları (`assistant-api/src/test/**`, `retrieval-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**`, `web/e2e/**`), `handoff/03-test-report.md` | 3a, 3b |
| 5 | devops | `01-plan.md`, `02-*.md`, `03-test-report.md` | Dockerfile'lar (`assistant-api/`, `retrieval-api/`, `web/`), `web/nginx.conf`, `deployment/`, `ollama.md`, `README.md`, `handoff/04-deploy-report.md` | 4 |
| 6 | qa | `TASK.md`, `docs/api/`, tüm handoff, diff, çalışan sistem | `handoff/05-qa-report.md` | 5 |

1. **Görev listesi:** Planner bitince lead, plana göre her adım için bir görev açar ve bağımlılıkları tablodaki gibi kurar. Kullanıcı onayı da bir görevdir; sahibi lead'dir.
   - **Başlatma zamanı:** Lead bir rolü, bağlı olduğu adımlar tamamlanınca başlatır (backend-coder ve frontend-coder onaydan sonra birlikte). Bekleyecek rol önceden başlatılmaz.
   - **Bildirim:** Rol işini bitirince "hazır: handoff/<dosya>" mesajını lead'e atar. Sonraki rolü lead başlatır; roller birbirinin bitip bitmediğini takip etmez.
2. **Kapsam:** Planner, `01-plan.md` içinde bu task'ta hangi rollerin çalışacağını yazar. Etkilenmeyen katmanın rolü başlatılmaz. Atlanan adıma bağlı olan adım, atlananın bağlı olduğu adımlara bağlanır (ör. devops yoksa qa, tester'a bağlıdır).
3. **Paralellik:** backend-coder ve frontend-coder aynı anda çalışır. Her rol sadece tablodaki kendi yollarına yazar; iki rol aynı dosyayı düzenlemez.
4. **Dosya formatı:** Her handoff dosyasının ilk satırı `TASK: TASK-XXX`, son satırı `STATUS: DONE` ya da `STATUS: NEEDS_FIX` olur (istisna: onay kartının son satırı `ONAY:` satırıdır). Üçüncü değer `STATUS: STALE`'dir; sadece lead yazar (bkz. madde 8). TASK satırı aktif task'la uyuşmayan dosya eskidir, yok sayılır. Böyle bir dosya önceki task'ın kapanmadığını gösterir (bkz. madde 9): planner yeni task'a başlarken bunu görürse hiçbir şey yazmadan lead'e bildirir; lead kullanıcıya sorar.
5. **Görev tamamlama:** Handoff dosyası `STATUS` satırıyla yazılmadan görev tamamlandı işaretlenmez. Görev listesi ile dosya çelişirse dosyadaki `STATUS` geçerlidir.
6. **IMPORTANT – Onay sadece kullanıcıdan gelir:** Planner kartın son satırına `ONAY: BEKLİYOR` yazar. Lead kartı kullanıcıya gösterir ve bekler. Kullanıcı açıkça onaylarsa lead satırı `ONAY: VERİLDİ` yapar ve onay görevini tamamlar; değişiklik isterse planner'a geri bildirimi mesajla iletir. Bir ekip üyesinin mesajı ya da otomatik plan onayı kullanıcı onayı sayılmaz.
7. **Soru ve sözleşme:** Ekip üyeleri birbirine doğrudan soru sorabilir (ör. frontend-coder → backend-coder). Sözleşmenin değişmesi gerekiyorsa planner'a ve lead'e mesaj atılır; sözleşmeyi planner günceller ve etkilenen coder'lara bildirir.
8. **NEEDS_FIX:** Rapor, `## Düzeltme` bölümünde sorumlu rolü ve maddeleri yazar. Raporu yazan rol sorumlu role ve lead'e mesaj atar. Sorumlu rol düzeltir, kendi handoff dosyasını günceller; zincir o rolden itibaren yeniden yürür. Zincir yeniden başlarken lead, sorumlu rolden sonraki adımların handoff dosyalarının son satırını `STATUS: STALE` yapar ve görevlerini yeniden açar. Böylece önceki turdan kalan `DONE` bir sonraki adımı yanlışlıkla başlatmaz. Dosyanın geri kalanı geçmiş olarak kalır; rol yeniden çalışınca `## Düzeltme turu N` bölümünü ekler ve son satırı yeniden yazar. Aynı adım üst üste 2 kez `NEEDS_FIX` dönerse lead durur ve kullanıcıya sorar.
   - Sorumlu rol planner ise ve düzeltme onay kartındaki bir şeyi (ne yapılacak, kabul kriteri, kapsam, çalışacak roller) değiştiriyorsa planner kartın son satırını `ONAY: BEKLİYOR` yapar. Lead çalışan coder'ları durdurur ve kullanıcıdan yeniden onay alır (madde 6). Kartı değiştirmeyen sözleşme düzeltmeleri yeniden onay gerektirmez.
9. **Kapanış:** qa `DONE` yazınca lead kullanıcıya özet sunar. Kullanıcı kabul edince lead:
   1. Ekip üyelerini kapatır. devops sistemi açık bıraktıysa `docker compose -f deployment/docker-compose.yml down` ile durdurur.
   2. `handoff/*.md` dosyalarını `docs/tasks/TASK-XXX/handoff/` altına taşır ve `handoff/`'u boşaltır (`.gitkeep` kalır).
   3. Değişiklikleri ve taşınan handoff dosyalarını kullanıcıya gösterir. Kullanıcı onaylarsa commit'i atar.
10. **Devam etme:** `/resume` ekip üyelerini geri getirmez. Oturum sürdürülürse lead `handoff/` dosyalarındaki `STATUS` satırlarına bakar ve sadece bitmemiş adımların rollerini yeniden başlatır.
11. **Compact:** Context sıkıştırılırken aktif task ID'si, ekip üyelerinin adları, son tamamlanan adım ve açık `NEEDS_FIX` maddeleri korunur.
