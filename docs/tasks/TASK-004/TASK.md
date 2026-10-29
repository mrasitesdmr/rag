# TASK-004: Raşit Bankası Ürün ve Hizmet Rehberi (PDF, 2026.10 ve 2026.11)

## Ne isteniyor
Asistanın cevaplarını dayandıracağı belge, gerçek bir bankanın müşteri rehberi kapsamında ve ayrıntısında yazılır: "Raşit Bankası Ürün ve Hizmet Rehberi". İki sürümü olur: 2026.10 (ilk içe alınan) ve 2026.11 (Bilgi kaynakları ekranındaki "Yeni sürüm yükle" ile işlenen). Lab'daki PDF'ler kullanılmaz; o PDF'ler sadece statik demo için hazırlanmıştı. Belge, gerçek bankacılık süreçlerini yansıtacak kadar kapsamlıdır.

Kullanıcı kararı: içeriği lead (Claude) hazırlar. Bu dosyalar hiçbir rolün yazma alanında değildir; onay kartı onaylanınca lead uygular (TASK-001'deki gibi).

## Kurallar
1. **Kurgu sınırı:** Banka kurgusaldır. Gerçek banka adı, logosu ya da kampanyası kullanılmaz. Ücret, faiz, limit ve süreler gerçekçidir ama kurgusaldır. Kapakta "bilgilendirme amaçlıdır" notu bulunur.
2. **Kapsam:** Bölümler lab'daki soru kategorilerini karşılar ve gerçek bir bankanın rehberindeki konuları içerir:
   - **Kartlar:** banka kartı; kredi kartı türleri (Raşit Gold dahil); yıllık üyelik ücreti ve iadesi; limit artırma ve düşürme; hesap kesim tarihi, ekstre, asgari ödeme; taksit; nakit avans; sanal kart; temassız ödeme; yurt dışı kullanım ve kur; kayıp ya da çalıntı bildirimi; itiraz (chargeback); kartın yenilenmesi ve teslim süresi.
   - **Hesaplar:** vadesiz, vadeli, döviz ve altın hesapları; hesap açma belgeleri; hesap işletim ücreti; hesap kapatma; mevduat sigortası (TMSF); stopaj.
   - **Krediler:** ihtiyaç, taşıt ve konut kredisi; kredili mevduat hesabı; başvuru şartları ve belgeleri; faiz, KKDF, BSMV, masraflar; erken ödeme ve erken kapama tazminatı; gecikme faizi; yapılandırma.
   - **Transfer:** havale, EFT, FAST, SWIFT; saatler, limitler, ücretler; karekod ile ödeme; düzenli ödeme talimatları; fatura ödemeleri.
   - **Dijital bankacılık:** Raşit Mobil ve internet şubesi; ilk giriş, şifre alma ve sıfırlama; cihaz tanımlama; işlem limitleri; bildirimler; güvenlik ve dolandırıcılık uyarıları.
   - **Yatırım:** yatırım fonları, hisse senedi, döviz ve altın alım satımı; işlem saatleri ve hafta sonu kuru; vergilendirme.
   - **Diğer:** şube ve ATM hizmetleri, ATM nakit çekme limitleri; müşteri hizmetleri ve kanallar; şikâyet ve itiraz süreci; KVKK ve aydınlatma metni; ücret tarifesi tablosu; terimler sözlüğü.
3. **Bilerek dışarıda bırakılanlar:** Rehberde cevabı olmayan, gerçekçi müşteri soruları için konular yazılmaz: kişisel bakiye, hesap hareketi, kripto para, başka bankaların ürünleri. Bu liste ayrı bir notta tutulur. Asistanın "Bu konuda rehberimizde bilgi bulamadım." demesi gereken durumlar bunlardır.
4. **Biçim:**
   - içindekiler, numaralı bölüm ve alt başlıklar, sayfa numarası, üst ve alt bilgi (rehber adı, sürüm, yürürlük tarihi),
   - ücret ve limit tabloları, madde listeleri, her bölüm sonunda "Sık sorulanlar",
   - çapraz atıflar ("bkz. 4.2").
   Sayfa sayısı onay kartında kesinleşir; öneri 60-100 sayfa (lab'daki rehber 31 sayfaydı).
5. **Makine tarafından okunabilir:** PDF'te metin seçilebilir olmalı, taranmış görüntü kullanılmaz. Türkçe karakterler (ç, ğ, ı, İ, ö, ş, ü) PDFBox ile çıkarılan metinde bozulmadan görünür. Tablolar metin olarak çıkarılabilir.
6. **Tekrarlanabilir üretim:** Rehberin kaynağı (ör. Markdown ya da HTML) ve PDF üretme komutu repoda durur; iki PDF aynı kaynaktan yeniden üretilebilir. Üretim aracı bir plan kararıdır (dependency); onay kartında önerilir.
7. **2026.11 sürümü:** 2026.10'dan listelenmiş, gerçekçi farkları vardır. Örnek: ücret artışı (lab'da Raşit Gold yıllık üyelik ücreti 1.450 TL → 1.650 TL, yürürlük 1 Kasım 2026), değişen bir limit, eklenen bir ürün ya da madde, kaldırılan bir madde. Farklar "değişiklik özeti" olarak ayrı yazılır, hangi sayfalarda olduklarıyla birlikte. Bu özet TASK-015'te yükleme sırasında kullanılır.
8. **Konum:** `kaynaklar/` değişmez. Dosyaların nerede duracağını (ör. `docs/rehber/`) planner önerir, kullanıcı onaylar.
9. **İçerik onayı:** Kullanıcı içindekileri ve örnek bölümleri onay kartı aşamasında görür. Tam metin, PDF üretildikten sonra kullanıcıya sunulur.

## Örnek senaryolar
- "Kartımı kaybettim, ne yapmalıyım?" sorusunun cevabı rehberde adım adım vardır: bildirim kanalları, kartın kapatılması, yeni kartın kaç iş gününde geleceği.
- "Ev kredisini erken kapatırsam ceza öder miyim?" sorusu için rehberde erken ödeme tazminatı oranı ve hangi kredilerde uygulandığı yazar.
- "Bitcoin alıp satabilir miyim?" sorusunun cevabı rehberde yoktur.
- 2026.11'de Raşit Gold ücreti değişmiştir. Aynı soru 2026.10 içe alınmışken 1.450 TL, 2026.11 işlendikten sonra 1.650 TL cevabını almalıdır.

## Kabul kriterleri
- İki PDF ve kaynakları onaylanan konumda; üretme komutu çalışıyor.
- Her iki PDF'ten PDFBox ile metin çıkarılmış, Türkçe karakter ve tablo kontrolü raporda.
- Değişiklik özetindeki her fark 2026.11 PDF'inde belirtilen sayfada var.
- "Rehberde olmayan konular" notu yazılmış.
- Kullanıcı içeriği onaylamış.

## Kapsam dışı
- Golden set soruları (TASK-016). İçerik, soru üretmeye uygun net olgular (sayı, süre, koşul) içerecek biçimde yazılır.
- İçe alma kodu (TASK-005).

## Etkilenen katmanlar
- [ ] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [ ] retrieval-api (kaynak, içe alma, embedding, arama)
- [ ] Web
- [ ] API sözleşmesi (REST)
- [ ] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [ ] Veritabanı (migration; hangi servis?)
- [ ] WebSocket
- [ ] Docker
- [x] Diğer: rehber içeriği ve PDF üretimi (lead uygular)
