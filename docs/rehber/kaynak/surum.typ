// Sürüme bağlı bütün değerler bu dosyadadır. Bölüm dosyalarında sürüm adı geçmez;
// değişen her olgu buradaki bir anahtardan okunur. Değişiklik özeti:
// ../degisiklik-ozeti-2026.11.md

#let surumler = (
  "2026.10": (
    etiket: "2026.10",
    yururluk: "1 Ekim 2026",
    tarih: datetime(year: 2026, month: 10, day: 1),
    // F1: Raşit Gold yıllık üyelik ücreti
    gold-yillik-ucret: "1.450 TL",
    // F2: banka kartıyla Raşit Bankası ATM'lerinden günlük nakit çekme limiti
    banka-karti-atm-gunluk: "25.000 TL",
    // F3: Raşit Genç kredi kartı
    genc-kart: false,
    // F4: şifre cihazı maddesi (6.11) ve ücret satırı (10.5)
    sifre-cihazi: true,
  ),
  "2026.11": (
    etiket: "2026.11",
    yururluk: "1 Kasım 2026",
    tarih: datetime(year: 2026, month: 11, day: 1),
    gold-yillik-ucret: "1.650 TL",
    banka-karti-atm-gunluk: "30.000 TL",
    genc-kart: true,
    sifre-cihazi: false,
  ),
)

#let surum-adi = sys.inputs.at("surum", default: none)
#if surum-adi == none {
  panic("Sürüm verilmedi. Örnek: typst compile --input surum=2026.10 ...")
}
#if surum-adi not in surumler {
  panic("Bilinmeyen sürüm: " + surum-adi + ". Geçerli sürümler: " + surumler.keys().join(", "))
}

#let S = surumler.at(surum-adi)
