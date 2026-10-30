# Değişiklik özeti: 2026.10 → 2026.11

Raşit Bankası Ürün ve Hizmet Rehberi'nin 2026.11 sürümü, 2026.10 sürümünden aşağıdaki dört farkla ayrılır. Bütün farkların yürürlük tarihi **1 Kasım 2026**'dır.

- Kaynak: `kaynak/surum.typ`. Farkların hepsi bu dosyadaki tek sözlükten gelir; bölüm dosyalarında sürüm adı geçmez.
- İki PDF'in sayfa sayısı (70) ve bölümlerin başladığı sayfalar aynıdır.
- 2. bölüm (Kartlar), 6. bölüm (Dijital bankacılık), 8. bölüm (Şube, ATM ve müşteri hizmetleri) ve 10. bölüm (Ücret tarifesi) farkları taşır. İçindekiler (6.11 satırı), kapak ve 1.1 (sürüm etiketi ve yürürlük tarihi) de değişir. Diğer bölümlerin (3, 4, 5, 7, 9, 11) sayfaları iki sürümde kelimesi kelimesine aynıdır.
- Sayfa numaraları üretilmiş PDF'lerden alınmıştır. `dogrula.sh` (D8) aşağıdaki "Ayrıntı" tablosunu okur. Her satırın metni, yazılan sayfada aranır; eski metnin yeni sürümde, yeni metnin de eski sürümde hiç geçmediği ayrıca kontrol edilir.
- 2026.11 rehberi eski değerleri anmaz. Rehberde "önceki ücret" ya da "bu sürümde değişenler" gibi bir bölüm yoktur.

## Kısa özet
Yeni sürüm yükleme penceresinde (TASK-015) gösterilecek biçimdedir: önce eski satır, sonra yeni satır. Parantezdeki sayfalar 2026.11 PDF'indeki sayfalardır.

1. Raşit Gold yıllık üyelik ücreti 1.450 TL
   → Raşit Gold yıllık üyelik ücreti 1.650 TL · yürürlük 1 Kasım 2026 (s. 8, 18, 65)
2. Banka kartıyla Raşit Bankası ATM'lerinden günlük nakit çekme limiti 25.000 TL
   → Banka kartıyla Raşit Bankası ATM'lerinden günlük nakit çekme limiti 30.000 TL · yürürlük 1 Kasım 2026 (s. 7, 58)
3. Raşit Genç kredi kartı yok
   → Raşit Genç kredi kartı: 18-26 yaş üniversite öğrencileri, yıllık üyelik ücreti yok, limit 5.000-20.000 TL · yürürlük 1 Kasım 2026 (s. 8, 9, 65)
4. Şifre cihazı 150 TL'ye verilir, kaybında yenisi 150 TL (6.11)
   → Şifre cihazı maddesi ve ücret satırı kaldırıldı · yürürlük 1 Kasım 2026 (2026.10'da s. 3, 49, 66)

## Ayrıntı
Bir fark birden fazla yerde geçiyorsa her geçiş ayrı satırdır. "— " eklenen farkta 2026.10 metninin, kaldırılan farkta 2026.11 metninin olmadığını gösterir. Metin hücreleri PDF'ten çıkarılan metinde birebir aranan dizelerdir (boşluklar tek boşluğa indirilir).

| # | Tür | Konu | 2026.10 metni | 2026.11 metni | Bölüm | Sayfa (2026.10) | Sayfa (2026.11) | Yürürlük |
|---|-----|------|---------------|---------------|-------|-----------------|-----------------|----------|
| F1.1 | Ücret | Raşit Gold yıllık üyelik ücreti (kart türleri tablosu) | `Raşit Gold 1.450 TL` | `Raşit Gold 1.650 TL` | 2.2 | 8 | 8 | 1 Kasım 2026 |
| F1.2 | Ücret | Raşit Gold yıllık üyelik ücreti (Kartlar sık sorulanlar) | `Raşit Gold'un yıllık üyelik ücreti 1.450 TL'dir` | `Raşit Gold'un yıllık üyelik ücreti 1.650 TL'dir` | 2 SSS | 18 | 18 | 1 Kasım 2026 |
| F1.3 | Ücret | Raşit Gold yıllık üyelik ücreti (ücret tarifesi) | `Raşit Gold yıllık üyelik ücreti 1.450 TL` | `Raşit Gold yıllık üyelik ücreti 1.650 TL` | 10.1 | 65 | 65 | 1 Kasım 2026 |
| F2.1 | Limit | Banka kartı günlük nakit çekme limiti, Raşit Bankası ATM'leri (banka kartı limitleri tablosu) | `Raşit Bankası ATM'lerinden nakit çekme 25.000 TL` | `Raşit Bankası ATM'lerinden nakit çekme 30.000 TL` | 2.1 | 7 | 7 | 1 Kasım 2026 |
| F2.2 | Limit | Banka kartı günlük nakit çekme limiti, Raşit Bankası ATM'leri (ATM limitleri tablosu) | `Banka kartıyla Raşit Bankası ATM'lerinden nakit çekme Günlük 25.000 TL` | `Banka kartıyla Raşit Bankası ATM'lerinden nakit çekme Günlük 30.000 TL` | 8.4 | 58 | 58 | 1 Kasım 2026 |
| F3.1 | Eklenen | Raşit Genç (kart türleri tablosu) | — | `Raşit Genç Alınmaz Koşul yok` | 2.2 | — | 8 | 1 Kasım 2026 |
| F3.2 | Eklenen | Raşit Genç (kart tanımı) | — | `Raşit Genç, 18-26 yaş arasındaki üniversite öğrencileri için çıkarılan kredi kartıdır.` | 2.2 | — | 8 | 1 Kasım 2026 |
| F3.3 | Eklenen | Raşit Genç kart limiti | — | `Kart limiti 5.000-20.000 TL arasında belirlenir.` | 2.2 | — | 8 | 1 Kasım 2026 |
| F3.4 | Eklenen | Kredi kartı başvuru şartında Raşit Genç istisnası | — | `Düzenli gelir sahibi olmak (Raşit Genç dışındaki kartlar için)` | 2.5 | — | 9 | 1 Kasım 2026 |
| F3.5 | Eklenen | Raşit Genç (ücret tarifesi) | — | `Raşit Genç yıllık üyelik ücreti Alınmaz` | 10.1 | — | 65 | 1 Kasım 2026 |
| F4.1 | Kaldırılan | Şifre cihazı (içindekiler satırı) | `6.11 Şifre cihazı 49` | — | İçindekiler | 3 | — | 1 Kasım 2026 |
| F4.2 | Kaldırılan | Şifre cihazı (başlık) | `6.11 Şifre cihazı` | — | 6.11 | 49 | — | 1 Kasım 2026 |
| F4.3 | Kaldırılan | Şifre cihazının ücreti | `Şifre cihazı şubelerden kimlikle başvurularak alınır ve 150 TL'ye verilir.` | — | 6.11 | 49 | — | 1 Kasım 2026 |
| F4.4 | Kaldırılan | Kaybolan şifre cihazının yenisi | `Kaybolan ya da arızalanan cihazın yerine verilen yenisi de 150 TL'dir.` | — | 6.11 | 49 | — | 1 Kasım 2026 |
| F4.5 | Kaldırılan | Şifre cihazı (ücret tarifesi) | `Şifre cihazı (ilk cihaz ya da yenisi) 150 TL` | — | 10.5 | 66 | — | 1 Kasım 2026 |

## Asistanda beklenen etki
Bu sorular TASK-015'te yeni sürüm yüklendikten sonra, TASK-016'da golden set'te kullanılır.

| Soru | 2026.10 içe alınmışken | 2026.11 işlendikten sonra |
|------|------------------------|---------------------------|
| Raşit Gold yıllık üyelik ücreti ne kadar? | 1.450 TL | 1.650 TL |
| Banka kartıyla günde ne kadar para çekebilirim? | Raşit Bankası ATM'lerinden 25.000 TL | Raşit Bankası ATM'lerinden 30.000 TL |
| Üniversite öğrencisiyim, ücretsiz kredi kartınız var mı? | Bu konuda rehberimizde bilgi bulamadım. | Evet, Raşit Genç; yıllık üyelik ücreti yok |
| Şifre cihazı alabilir miyim? | Evet, şubeden 150 TL'ye | Bu konuda rehberimizde bilgi bulamadım. |
