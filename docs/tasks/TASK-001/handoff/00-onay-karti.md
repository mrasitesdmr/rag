TASK: TASK-001
# Onay kartı: Ekip altyapısını (agent, skill, CLAUDE.md) RAG projesine özel hale getirme

> Revizyon 1: üç sorunun cevabına göre yeniden yazıldı. En büyük değişiklik, repo artık lab'daki servis yapısını izliyor ve bilgi bankası bu projede yeniden yazılıyor.

## Kullanıcı kararları
1. **Modül yapısı ve bilgi bankası:**
   - Repo lab'daki servis yapısını izleyecek: `web/` (yönetim paneli), `assistant-api/` (asistan servisi), `retrieval-api/` (bilgi bankası servisi) ve `deployment/`. `backend/` ve `frontend/` kalkacak.
   - Kaynaklar, parçalar, içe alma, embedding ve vektör arama bu repoda, `retrieval-api` içinde, bu projeye göre yeniden yazılacak.
   - `../embedding-pgvector` çalışırken kullanılmayacak; sadece okunup örnek alınacak.
   - Bu task'ta modüllere sadece notları (`CLAUDE.md`) konacak; kod sonraki task'larda yazılacak.
2. **Yeni skill'ler:** RAG hattı, cevap akışı ve değerlendirme skill'lerinin üçü de eklenecek.
3. **Canlı yapılandırma:** Üretim modu, top-5/rerank, yeniden yazma ve reranker ayarları sunucu ayarı olacak; ekranda düğme olmayacak. Başlıktaki "Canlı yapılandırma" rozeti ve Değerlendirme düğmesindeki yöntem adı lab'daki gibi kalacak.

## Ne yapılacak
- Ekibin okuduğu bütün yönerge dosyalarında (rol tanımları, 39 skill, proje notları, izinler, kılavuz, görev şablonu) eski klasör adları yeni modüllere çevrilecek. Rol tablosu da buna göre değişecek: arka uç geliştiricisi `assistant-api/` ve `retrieval-api/`'ye, ön yüz geliştiricisi `web/`'e yazacak.
- Kaynak, parça ve içe alma örnekleri artık bu projenin bilgi bankası servisini anlatacak. Önceki projeye özgü kurallar ve metinler çıkacak: kaynak sayısı sınırı, kaynak adı tekliği, arama kayıtları, sık arananlar, çoklu çalışma alanı, "Dalga Fiber", "Yıllık izin kaç gün?". Konuşma, tur, alıntı ve değerlendirme örnekleri asistan servisini anlatacak. Örnek sorular Raşit Bankası rehberinden alınacak.
- Ürün hedefi her yere aynı cümlelerle yazılacak: ürün lab sayfasının sol yarısının aynısı olacak; sağ yarı (sistem konsolu) ekrana gelmeyecek, oradaki akışı arka plan gerçekten yürütecek.
- Üç yeni skill yazılacak. Bilgi bankasına belge alma ve embedding, RAG hattı skill'inin ayrı bir bölümünde anlatılacak.
- Rol tanımlarındaki üç çelişki düzeltilecek.

## Bittiğini nasıl anlayacaksın
- Repoda `web/`, `assistant-api/`, `retrieval-api/`, `deployment/` klasörleri olacak ve içlerinde sadece `CLAUDE.md` bulunacak. `backend/` ve `frontend/` kalkmış olacak.
- Hiçbir dosyada `backend/`, `frontend/` yolu ya da `cd backend` gibi komut kalmamış olacak.
- `grep -rn "embedding-pgvector" .claude CLAUDE.md */CLAUDE.md` çıktısında sadece "okunur örnek" olarak gerekçesi yazılı satırlar kalacak.
- Skill'lerde "arama kaydı", "Dalga Fiber", "Fatura SSS", `/w/42`, "kaynak sınırı" gibi ifadeler kalmamış olacak.
- `rag.md`, `CLAUDE.md`, `web/CLAUDE.md` ve ön yüz skill'lerinde "sol yarısının aynısı" ve "ekrana gelmez" cümleleri yazılı olacak.
- `deployment/CLAUDE.md`'deki servis ve port tablosu ile diğer dosyalardaki portlar aynı olacak.
- `KULLANIM-KILAVUZU.md`'yi izleyerek TASK-002'yi açabileceksin; kılavuzda adı geçen dosyalar gerçekten var olacak.
- qa, planda yazılı 20 kontrolü kendisi çalıştırıp "DONE" yazmış olacak.

## Dosya envanteri (özet)
Dosya dosya tam tablo planda: `handoff/01-plan.md` → "Dosya envanteri".

| Önceki projede | Bu projede | Durum |
|----------------|------------|-------|
| 39 skill klasörü (alt dosyalarıyla) | Aynı adlarla var | 3'ü değişmiyor; 36'sında yol ya da örnek değişecek |
| 6 rol tanımı, izin ayarları | Aynı adlarla var | Yeni klasör adlarına ve kararlara göre değişecek |
| `backend/CLAUDE.md` | `assistant-api/CLAUDE.md` ve `retrieval-api/CLAUDE.md` | Taşınacak; eski dosya silinecek |
| `frontend/CLAUDE.md` | `web/CLAUDE.md` | Taşınacak; eski dosya silinecek |
| `deployment/CLAUDE.md`, kök `CLAUDE.md`, kılavuz, README, ürün dokümanı (`rag.md`), görev şablonu, `.gitignore` | Var | Değişecek |
| `handoff/` ve `docs/tasks/` klasörleri | Var | Aynı |
| `ollama.md` | Yok | Bilerek alınmadı: bu task'ın kapsamı dışında; ilk model task'ında yazılacak |
| `.nojekyll` | Yok | Bilerek alınmadı: sayfaları GitHub Pages'te yayınlamak için kullanılan boş bir dosyaydı; bu projede yayın yok |
| `scratch/` | "git'e girmesin" satırı olarak var | Aynı; klasöre gerek yok |
| Docker dosyaları, önceki projenin kodu, örnek PDF, ekran kayıtları | Yok | Bilerek alınmadı: kapsam dışı ya da önceki projenin içeriği. Bilgi bankası kodu ileride bu projede yeniden yazılacak |
| Önceki projenin görev geçmişi | Yok | Bilerek alınmadı (TASK kuralı 2) |
| — | 3 yeni skill | Yeni |

## Kim neyi değiştirecek
| Dosya | Uygulayan | Ne değişecek |
|-------|-----------|--------------|
| `.claude/` (rol tanımları, skill'ler, izin ayarları) | lead | Yollar, örnekler, açıklamalar, 3 yeni skill, izinler, rol tanımlarındaki çelişkiler |
| Kök `CLAUDE.md` | lead | Modüller, rol tablosu, komutlar, ürün hedefi |
| `rag.md` | lead | Ürün hedefi, modüller ve bilgi bankası, çalışma alanı, veritabanları |
| `KULLANIM-KILAVUZU.md` | lead | Task'ı lab'a göre yazma, hangi servisin etkilendiği, model notu |
| `docs/tasks/_TEMPLATE.md` | lead | Lab'ın sol yarısı sorusu; "Backend" yerine iki servis, "Frontend" yerine web; "Ekip altyapısı" kutusu |
| Kök `.gitignore` | lead | Eski klasör yolları yerine yenileri |
| `assistant-api/CLAUDE.md`, `retrieval-api/CLAUDE.md` (yeni) | backend-coder | Eski `backend/CLAUDE.md`'nin içeriği iki servise göre ayrılır ve tamamlanır |
| `web/CLAUDE.md` (yeni) | frontend-coder | Eski `frontend/CLAUDE.md`'nin içeriği + ürün hedefi |
| Eski `backend/`, `frontend/` klasörlerinin silinmesi | lead | Yeni tabloda bu yolların sahibi kalmıyor; coder'lar yeni dosyaları yazdıktan sonra lead siler |
| `deployment/CLAUDE.md`, `README.md` | devops | Servis/port tablosu, veritabanları, gereksinimler |
| `docs/api/openapi.yaml` | planner (yapıldı) | İki servisi anlatan başlık, ortak hata biçimi; yeni uç yok |

## Yapılmayacaklar
- Uygulama kodu, proje iskeleti, test, Docker dosyası; model indirme.
- `ollama.md`, cevap akışı ve canlı güncelleme sözleşmelerinin içeriği.
- `../embedding-pgvector`'a ve `kaynaklar/` altındaki sayfalara herhangi bir değişiklik.
- Önceki projenin kodunu ya da görev geçmişini taşımak.

## Benim verdiğim kararlar
- **Çalışma alanı korunuyor, ama tek ve sabit.** Bilgi bankasında tek bir çalışma alanı olacak: 42, "Raşit Bankası · Dijital Asistan". Gerekçe: lab'ın sol yarısındaki Bilgi kaynakları ekranında "Çalışma alanı Raşit Bankası · Dijital Asistan (ws 42) · embedding modeli bge-m3@1024" yazan bir bilgi kutusu var; "aynısı" kuralı gereği bu kutu kalmalı. Lab'ın istekleri de çalışma alanı numarasını içeriyor. Önceki projedeki çoklu çalışma alanı (oluşturma, seçme, ekran adreslerinde çalışma alanı) lab'da olmadığı için gelmiyor.
- **İki servis, iki ayrı veritabanı.** Tek bir veritabanı sunucusunda iki ayrı veritabanı olacak: asistan servisi için konuşmalar ve turlar, bilgi bankası servisi için kaynaklar, parçalar ve vektörler. Lab da iki ayrı veritabanı gösteriyor. Servisler birbirinin verisine doğrudan bakmaz, birbirine istek atar. Tek sunucu 16 GB makinede daha az bellek demek.
- **Portlar lab'daki gibi:** site 3000, asistan servisi 8090, bilgi bankası servisi 8080, reranker 8081, Ollama 11434, veritabanı 5432. Önceki projenin sistemi de aynı portları kullandığı için iki proje aynı anda açılamaz.
- **Ekran ile servisler arasındaki trafik:** Bilgi kaynakları ekranı lab'daki gibi doğrudan bilgi bankası servisine gider; Önizleme, Konuşmalar ve Değerlendirme asistan servisine gider. İki servisin sözleşmesi tek dosyada (`docs/api/openapi.yaml`) kalır.
- **Arama kayıtları bilgi bankasında tutulmayacak.** Lab'ın sağ yarısındaki veritabanı haritasında bu tablo görünüyor, ama sol yarıda ekranı yok ve TASK onu önceki projenin işi sayıyor. "Hangi soruda hangi parça geldi" bilgisi zaten asistanın tur kaydında duruyor.
- **Önceki projenin kurallarının yerine lab'dan iki kural örnek olacak:**
  - Bir belgeye yeni sürüm işlenirken ikinci yükleme reddedilir; lab'da yükleme sürerken düğme kapalı.
  - Farklı embedding modeliyle yükleme reddedilir; lab'ın bilgi kutusu bunu söylüyor.
- **Belge alma ve embedding, ayrı bir dördüncü skill değil.** RAG hattı skill'inin "indeksleme" bölümünde anlatılacak; RAG anlatım sayfası da hattı "indeksleme ve sorgu zamanı" diye ikiye ayırıyor. Böylece yeni bir onay gerekmiyor.
- **İki servis ayrı projeler olarak kurulacak ve kod paylaşmayacak.** Küçük ortak parçalar (sayfa yanıtı, hata sınıfları) her serviste ayrı durur; servisler birbirinden bağımsız derlenir.
- **Eski notların taşınması:** Eski `backend/` ve `frontend/` notlarının içeriğini yeni dosyalara ilgili geliştirici taşıyacak. Yeni rol tablosunda eski klasörlerin sahibi kalmadığı için onları lead silecek.
- **Rol tanımlarındaki üç çelişki düzeltiliyor:**
  - Altyapı rolü `ollama.md` ve `README.md`'yi yazabilecek; kök tabloda vardı ama rol tanımında yoktu.
  - Test rolü çalışmayan task'ta altyapı rolü test raporu beklemeyecek.
  - Proje iskeleti yokken roller derleme komutu çalıştırmaya kalkmayacak.
- **İzinler:** yeni klasörler için düzenleme izinleri, cevap akışını denemek için bir komut, `ollama.md` ve `README.md` düzenleme izni.
- **Test rolü bu task'ta çalışmayacak**, çünkü test yazılmıyor; kontrolleri qa yapacak.

## Çalışacak roller
Sıra şöyle olacak:
1. planner.
2. lead: sahibi olmayan dosyaları onaydan hemen sonra, diğer rollerden önce değiştirir.
3. backend-coder (sadece `assistant-api/CLAUDE.md` ve `retrieval-api/CLAUDE.md`) ve frontend-coder (sadece `web/CLAUDE.md`) paralel çalışır.
4. lead eski klasörleri siler; aynı sırada devops yalnızca `deployment/CLAUDE.md` ve `README.md`'yi yazar, Docker çalıştırmaz.
5. qa.

Atlanan: tester, çünkü bu task'ta test yazılmıyor.

## Kararını bekleyen sorular
Yok.

## Riskler
- Değişiklik 60'tan fazla dosyaya dağılıyor ve klasör adı değişikliği neredeyse her dosyaya dokunuyor. Gözden kaçan bir eski yol ya da örnek kalabilir; qa'nın 20 kontrolü bunları yakalamak için yazıldı.
- Lab'ın sol yarısında "kaynak ekle" ekranı yok, sadece "Yeni sürüm yükle" var. Rehberin bilgi bankasına ilk kez nasıl gireceği, bilgi bankası servisinin ilk task'ında karara bağlanmalı. Bu task'ı etkilemiyor.
- Yeni skill'ler henüz kod yokken yazılıyor; dil modeli kütüphanesinin ayrıntıları ilk kod task'ında kurulan sürüme göre doğrulanmalı.

ONAY: VERİLDİ
