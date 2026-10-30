// Bölüm dosyalarının ortak yardımcıları: renkler, tablo, ara başlık, atıf, sık sorulanlar.
#import "surum.typ": S

#let renk = (
  marka: rgb("#7b2346"),
  marka-koyu: rgb("#5a1832"),
  marka-acik: rgb("#f5e8ed"),
  vurgu: rgb("#b8892b"),
  vurgu-acik: rgb("#f8f0dc"),
  murekkep: rgb("#21181c"),
  soluk: rgb("#6e6268"),
  cizgi: rgb("#e6dee1"),
)

#let tablo-yazi = 9.5pt
#let tablo-bosluk = 5pt

// Hücreler tek satırdır: her sütunun doğal genişliği ölçülür, satır sayfaya
// sığmıyorsa derleme durur. Böylece PDF'ten çıkarılan metinde her tablo satırı
// tek satır olarak ve sütun sırasıyla görünür.
#let tablo(baslik, ..satirlar) = layout(alan => {
  let n = baslik.len()
  let satirlar = satirlar.pos().filter(s => s != none)
  for s in satirlar {
    if s.len() != n { panic("Tablo satırında " + str(s.len()) + " hücre var, " + str(n) + " bekleniyordu: " + repr(s)) }
  }
  let ust = baslik.map(h => strong(h))
  let genislik = range(n).map(i => calc.max(
    measure(text(size: tablo-yazi, ust.at(i))).width,
    ..satirlar.map(s => measure(text(size: tablo-yazi, s.at(i))).width),
  ))
  let toplam = genislik.sum() + 2 * tablo-bosluk * n
  if toplam > alan.width {
    panic("Tablo sayfaya sığmıyor (" + repr(toplam) + " > " + repr(alan.width) + "): " + repr(baslik))
  }
  set text(size: tablo-yazi)
  block(above: 0.9em, below: 1.1em, table(
    columns: (auto,) * (n - 1) + (1fr,),
    inset: tablo-bosluk,
    stroke: 0.5pt + renk.cizgi,
    fill: (x, y) => if y == 0 { renk.marka-acik },
    table.header(..ust),
    ..satirlar.flatten(),
  ))
})

// Numarasız ara başlık; içindekilere ve yer imlerine girmez.
#let ara(baslik) = heading(level: 3, numbering: none, outlined: false, bookmarked: false, baslik)

// Çapraz atıf: metinde "bkz. 4.10" olarak görünür.
#let bkz(etiket) = [bkz. #ref(etiket)]

#let bilgi(govde) = block(
  width: 100%,
  inset: (x: 10pt, y: 8pt),
  above: 1em,
  below: 1em,
  fill: renk.vurgu-acik,
  stroke: (left: 2pt + renk.vurgu),
  govde,
)

#let sss() = heading(level: 2, numbering: none)[Sık sorulanlar]

#let soru(s, c) = block(above: 0.9em, below: 0.9em, breakable: false)[
  #text(weight: "semibold", fill: renk.marka-koyu, s) \
  #c
]
