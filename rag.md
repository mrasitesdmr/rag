# RAG Asistan – Ürün dokümanı

> Bu dosyayı kullanıcı yazar ve günceller. Planner iş kuralı belirsiz olduğunda önce `TASK.md`'ye, sonra buraya bakar.
> Durum: ürün çalışıyor (TASK-002…016, TASK-027). Aşağıdaki bilgiler `kaynaklar/rag-lab.html`'den özetlendi; kesinleşen kurallar task'larla buraya eklenmiştir.

## Amaç
Dil modeline cevabı kendi verinle ve kaynak göstererek yazdırmayı (RAG) anlatmak ve bunu çalışan bir demoyla göstermek. Önceki proje (`../embedding-pgvector`) parçaları anlamıyla bulmayı öğretti; bu proje bulunan parçalarla dil modeline doğru, güncel ve kaynağı gösterilebilen bir cevap yazdırmayı öğretir.

## Ürün hedefi
Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. "Aynısı" şu demektir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder.

Lab'da üretim modu (Sadece LLM / RAG), retrieval (top-5 / rerank 20→5), takip sorusunu yeniden yazma ve reranker durumu sağ yarıdaki düğmelerle değişir. Sağ yarı gelmediği için bu ayarlar ekrandan değiştirilmez; `assistant-api` yapılandırmasıdır (`assistant.*`, ortam değişkeniyle). Başlıktaki "Canlı yapılandırma" rozeti lab'daki gibi kalır; Değerlendirme'deki ölçüm düğmesi etkin stratejiyi lab'daki gibi gösterir ("Ölçümü çalıştır · rerank 20→5"), değeri backend verir.

## Modüller ve bilgi bankası
Repo lab'daki servis yapısını izler: `web/` (React yönetim paneli), `assistant-api/` (Spring Boot + Spring AI: konuşma, sorgu yeniden yazma, retrieval çağrısı, rerank, prompt, cevap akışı, geri bildirim, temsilciye aktarma, değerlendirme), `retrieval-api/` (Spring Boot + pgvector: bilgi kaynakları, sürümler, içe alma, parçalama, embedding, vektör arama) ve `deployment/`. Bilgi bankası **bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılır**. `../embedding-pgvector` çalışma zamanı bağımlılığı değildir; sadece okunur kalıp referansıdır: kodu toplu kopyalanmaz ve oraya yazılmaz. `assistant-api`, `retrieval-api`'ye REST ile gider (`POST /api/workspaces/{workspaceId}/search`). `web`'in Bilgi kaynakları ekranı `retrieval-api`'nin kaynak, sürüm, parça ve içe alma uçlarını aynı adres üzerinden (dev'de Vite proxy, Docker'da nginx) doğrudan çağırır (lab'daki gibi). İki servisin sözleşmesi `docs/api/openapi.yaml`'dadır; veritabanları ayrıdır.

retrieval-api'de tek bir çalışma alanı vardır: 42, "Raşit Bankası · Dijital Asistan". Migration ile gelir; oluşturma, listeleme ya da seçme ekranı ve ucu yoktur. Adreslerde (`/api/workspaces/42/...`) ve Bilgi kaynakları ekranındaki bilgi kutusunda adıyla görünür (lab); ekran adreslerinde (`/kaynaklar/...`) yoktur. Kaynaklar, parçalar ve içe alma işleri çalışma alanına bağlıdır; embedding modeli çalışma alanı düzeyinde tektir (`bge-m3@1024`).

Tek PostgreSQL sunucusu (pgvector'lü imaj), iki ayrı veritabanı: `assistant` (assistant-api: konuşmalar, turlar, değerlendirme çalıştırmaları) ve `retrieval` (retrieval-api: çalışma alanı, kaynaklar, parçalar ve embedding'leri, içe alma işleri). Her servis kendi kullanıcısıyla sadece kendi veritabanına bağlanır ve kendi Flyway migration'larını taşır; servisler birbirinin tablosunu okumaz, REST ile konuşur. Vektörler sadece `retrieval` veritabanındadır.

## Başlangıç içeriği
| Dosya | Ne anlatıyor |
|-------|--------------|
| `kaynaklar/rag.html` | RAG: dil modelinin neyi bilemeyeceği, hallucination, RAG hattı (indexing ve sorgu zamanı), retrieval kalitesi, prompt ve kaynaklı cevap, ölçme, production'da RAG |
| `kaynaklar/rag-lab.html` | Raşit Bankası dijital asistan yönetimi demosu: sol yarıda ürün (yönetim paneli), sağ yarıda adım adım akış ve sistem konsolu |

Bu sayfalar şu an tek başına çalışan statik HTML dosyalarıdır. Hedef, lab'ı backend, frontend, PostgreSQL ve lokal modeller üzerinde gerçekten çalışan bir projeye dönüştürmektir.

## Lab'da görünenler (referans)
- **Senaryo:** Raşit Bankası'nın dijital asistanı, müşteri sorularını "Ürün ve Hizmet Rehberi" PDF'ine dayanarak Türkçe, kısa ve kaynak numaralı cevaplar. Rehber kurgusaldır ve repodadır (`docs/rehber/`): 70 sayfa, 11 bölüm, iki sürüm (2026.10 ana sürüm, 2026.11 isteğe bağlı ikinci belge). Bilgi bankası boş açılır; belgeler Bilgi kaynakları'ndaki "Belge ekle" ile girer. Rehberde olmayan soruda sabit cümle: "Bu konuda rehberimizde bilgi bulamadım."
- **Ekranlar (sol yarı):** Önizleme (asistanla sohbet), Konuşmalar (geçmiş konuşmalar ve turlar), Bilgi kaynakları (rehber, sürümleri, parçaları, içe alma), Değerlendirme (golden set ile recall@5 ve MRR).
- **Canlı yapılandırma (sağ yarıda; ekrana gelmez):** üretim modu (Sadece LLM / RAG), retrieval (top-5 / rerank 20→5), takip sorusunu yeniden yazma (açık/kapalı), reranker durumu.
- **Cevap:** token token akar (SSE: `token`, `sources`, `done`); cevaptaki `[n]` parçayı açar; tura geri bildirim verilir; cevap yoksa temsilciye aktarma önerilir.
- **Sistem konsolu (sağ yarı):** Network, Veritabanı, Sıralama, Prompt, Loglar, Kod; "adım adım" modu. Ekrana gelmez (Ürün hedefi).
- **Servisler:** web → assistant-api (8090, Spring AI) → retrieval-api (8080, bu repoda) + Ollama (11434: `qwen3.5:9b`, `bge-m3`) + llama-server reranker (8081: `bge-reranker-v2-m3`); assistant ve retrieval için ayrı PostgreSQL veritabanları.

## Genel kurallar
- Hedef kitle: RAG'i öğrenmek isteyen geliştiriciler. Modeller: lokal ve ücretsiz (Ollama `qwen3.5:9b`, `bge-m3`; llama-server `bge-reranker-v2-m3`). Veri kaynağı: `docs/rehber/` altındaki kurgusal Raşit Bankası rehberi (PDF), "Belge ekle" ile yüklenir.
- Ekranlar ve görsel dil: Ürün hedefi.
- Bilgi bankası: retrieval-api, bu repoda.
