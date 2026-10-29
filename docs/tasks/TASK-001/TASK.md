# TASK-001: Ekip altyapısını (agent, skill, CLAUDE.md) RAG projesine özel hale getirme

## Ne isteniyor
Kod yazmaya başlamadan önce ekip altyapısı eksiksiz ve bu projeye özel hale gelecek. `../embedding-pgvector` projesindeki agent orkestrasyonu (roller, görev listesi, handoff, onay, NEEDS_FIX/STALE, kapanış), skill yapısı ve `CLAUDE.md` düzeni bu projeye bire bir ve eksiksiz taşınmış olacak. İçerik ise o projenin değil, RAG asistan projesinin işine göre yazılmış olacak.

Ürün hedefi de bu task'ta altyapıya işlenir: Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır** (Raşit Bankası dijital asistan yönetimi: Önizleme, Konuşmalar, Bilgi kaynakları, Değerlendirme). Sağ yarıdaki adım adım akış ve sistem konsolu ekrana gelmez. O akış (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) arka planda backend'in gerçekten yaptığı iştir.

## Kurallar
1. **Kapsamdaki dosyalar:** `.claude/agents/*.md`, `.claude/skills/**` (alt klasörlerdeki `references/`, `rules/`, `examples/`, `templates/`, `scripts/` dahil), `.claude/settings.json`, kök `CLAUDE.md`, `backend/CLAUDE.md`, `frontend/CLAUDE.md`, `deployment/CLAUDE.md`, `docs/tasks/_TEMPLATE.md`, `docs/api/openapi.yaml` (sadece iskelet), `README.md`, `KULLANIM-KILAVUZU.md`, `rag.md`, `handoff/` ve `docs/tasks/` klasör yapısı.
2. **Eksiksizlik:** `../embedding-pgvector`'daki her agent, skill (alt dosyalarıyla), ayar, şablon, kılavuz ve klasör yapısı (ör. `.gitkeep` dosyaları) bu projede karşılığına sahip olur. Bilerek alınmayan bir şey varsa onay kartında gerekçesiyle yazılır. Referans projenin task geçmişi (`docs/tasks/TASK-00X/` ve handoff dosyaları) taşınmaz.
3. **Projeye özgülük:** Kapsamdaki her dosya baştan sona okunur. Referans projenin işine dayanan süreç, örnek, isim, ekran, port, yol ve dosya adları bu projenin işine göre güncellenir. Referans projenin işi: çalışma alanı (workspace), embedding sınaması, arama ve arama kayıtları, `lab.html` / "RAG Retrieval Lab". Bu projenin işi: konuşma ve tur, retrieval, rerank, prompt, cevap üretimi ve akışı, alıntı, geri bildirim, temsilciye aktarma, değerlendirme (golden set, recall@5, MRR), bilgi kaynakları ve sürümleri. Projeden bağımsız genel teknik içerik (ör. Spring, React, test, SQL kalıpları) korunur; sadece örnekleri bu projenin alanına çevrilir.
4. **`../embedding-pgvector` atıfları:** Kalan her atıf ya kaldırılır ya da "sadece okunur referans" olarak gerekçesiyle bırakılır. Bir dosya, işin kendisini referans projeye göre tarif etmez.
5. **Retrieval kararı:** Bilgi kaynakları, parçalar, içe alma ve vektör aramanın bu repoda mı yazılacağı yoksa `../embedding-pgvector`'un ayrı servis (retrieval-api) olarak mı çalıştırılacağı onay kartında kullanıcıya sade dille, artı/eksileriyle sorulur. Planner bir öneri yapar. Dokümanlar (`CLAUDE.md`'ler, `deployment/CLAUDE.md`'deki servis ve port tablosu, ilgili skill'ler, `rag.md`) bu karara göre yazılır.
6. **Ürün hedefi altyapıya işlenir:** "Sol yarının aynısı; sağ yarı ekrana gelmez, backend'de çalışır" kararı `rag.md`'ye, kök `CLAUDE.md`'ye, `frontend/CLAUDE.md`'ye ve ilgili skill'lere yazılır. İlgili skill'ler: `frontend-design`, `frontend-conventions`, `e2e-testing` ve gerekiyorsa diğerleri. "Aynısı" şu anlama gelir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir.
7. **Orkestrasyon mantığı aynı kalır:** Rol tablosu, sıralama, handoff dosya formatı, onay, NEEDS_FIX, STALE, kapanış ve devam etme kuralları referans projeyle aynı mantıkta çalışır. Bu projeye özgü eklemeler (SSE sözleşmesi `docs/api/sse.md`, lokal modeller ve `ollama.md`, reranker) tüm rol tanımlarında ve `CLAUDE.md`'lerde tutarlıdır.
8. **Tutarlılık:** Kök `CLAUDE.md`, modül `CLAUDE.md`'leri, agent tanımları, skill'ler ve `KULLANIM-KILAVUZU.md` birbiriyle çelişmez. Yol, komut, port, dosya adı ve dosya sahipliği her yerde aynıdır. Agent tanımlarındaki okuma/yazma yolları kök `CLAUDE.md`'deki tabloyla aynıdır.
9. **Eksik skill:** Planner, bu projenin referans projede olmayan ihtiyaçlarını değerlendirir: Spring AI ile dil modeli çağrısı ve prompt, SSE ile cevap akışı, reranker, RAG değerlendirmesi. Yeni ya da genişletilmiş skill gerekiyorsa onay kartında önerir. Yeni skill de dependency gibi bir plan kararıdır, kullanıcı onayıyla eklenir.
10. **Uygulayan:** Bu task'ın değiştireceği dosyaların çoğu hiçbir rolün değildir (`.claude/**`, kök `CLAUDE.md`, `rag.md`, `KULLANIM-KILAVUZU.md`, `docs/tasks/_TEMPLATE.md`). Kullanıcı bu task ile bunların değiştirilmesini talep eder: onay kartı onaylanınca bu dosyaları lead uygular. Bir rolün sahip olduğu dosyaları (modül `CLAUDE.md`'leri, `README.md`) kök `CLAUDE.md`'deki tabloya göre o rol uygular. Planner, onay kartında dosya bazında kimin neyi değiştireceğini yazar.
11. Bu task'ta uygulama kodu, Docker dosyası ve test yazılmaz; model indirilmez.
12. Dil kuralları kök `CLAUDE.md`'deki gibidir: doküman, skill ve agent metinleri Türkçe; kod örnekleri, identifier'lar ve log metinleri İngilizce.

## Örnek senaryolar
- Yeni bir task'ta planner, onay kartını `rag.md`, `rag-lab.html` ve bu projenin domain sözlüğüne göre yazar. Referans projenin "çalışma alanı" ya da "arama kaydı" kavramlarına başvurmaz.
- frontend-coder `frontend-conventions` skill'ini açar. Klasör yapısı örneklerinde lab'ın sol menüsündeki ekranları (Önizleme, Konuşmalar, Bilgi kaynakları, Değerlendirme) görür, sistem konsolunun ekrana gelmediğini okur.
- backend-coder `backend-conventions`'ı açar. Domain sözlüğünde conversation, turn, citation, feedback ve evaluation tanımlarını, SSE olay kurallarını ve loglama sınırını (soru, cevap, parça, prompt loglanmaz) bulur.
- Ekipten biri `grep -rn "embedding-pgvector" .claude CLAUDE.md */CLAUDE.md` çalıştırır. Sadece onay kartında gerekçesi yazılmış "okunur referans" atıflarını görür.
- Kullanıcı `KULLANIM-KILAVUZU.md`'yi okuyarak ilk geliştirme task'ını (TASK-002) açabilir. Kılavuzdaki komutlar ve dosya yolları bu projede gerçekten vardır.

## Kabul kriterleri
- Onay kartında referans projeyle karşılaştırmalı bir dosya envanteri var: referans dosya → bu projedeki karşılığı → durum (aynı / uyarlandı / eksikti, eklendi / bilerek alınmadı + gerekçe).
- Kural 5'teki retrieval kararı kullanıcı tarafından verilmiş ve dokümanlara işlenmiş.
- Kapsamdaki tüm dosyalarda referans projenin işine ait kavram, ekran, port ve yol kalmamış. Kalan `../embedding-pgvector` atıflarının her biri gerekçeli.
- Ürün hedefi (Kural 6) `rag.md`, kök `CLAUDE.md`, `frontend/CLAUDE.md` ve ilgili skill'lerde yazılı.
- Agent ve skill dosyalarının frontmatter'ı geçerli (`name`, `description`; agent'larda `tools`). Skill açıklamaları bu projenin işine göre tetiklenir. `.claude/settings.json` geçerli JSON.
- Agent tanımlarındaki okuma/yazma yolları ve handoff dosya adları kök `CLAUDE.md` tablosuyla birebir aynı.
- qa, kapsamdaki dosyaları bağımsız olarak kontrol edip `DONE` vermiş.

## Kapsam dışı
- Backend, frontend ve veritabanı kodu; Maven/npm proje iskeletinin oluşturulması (sonraki task'lar).
- `docs/api/openapi.yaml`'a endpoint eklenmesi, `docs/api/sse.md` ve `docs/api/websocket.md`'nin içeriği.
- Dockerfile, docker compose, `ollama.md` ve model indirme.
- `../embedding-pgvector`'a herhangi bir yazma.
- `kaynaklar/**` dosyalarının değiştirilmesi.

## Etkilenen katmanlar
- [ ] Backend
- [ ] Frontend
- [ ] API sözleşmesi (REST) (sadece iskelet tutarlılığı)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration)
- [x] Retrieval (`../embedding-pgvector` ile ilişki: karar onay kartında)
- [ ] WebSocket
- [ ] Docker
- [x] Ekip altyapısı: `.claude/**`, `CLAUDE.md`'ler, `KULLANIM-KILAVUZU.md`, `rag.md`, şablon
