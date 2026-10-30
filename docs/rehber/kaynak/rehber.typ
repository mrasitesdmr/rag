// Raşit Bankası Ürün ve Hizmet Rehberi: ana dosya.
// Derleme: ../uret.sh (sürüm --input surum=<etiket> ile verilir, bkz. surum.typ).
#import "ortak.typ": *

#let rehber-adi = "Raşit Bankası Ürün ve Hizmet Rehberi"

#set document(title: rehber-adi, author: "Raşit Bankası A.Ş.", date: S.tarih)
#set text(font: "Source Sans 3", size: 11pt, fill: renk.murekkep, lang: "tr", region: "TR", hyphenate: false, ligatures: false)
#set smartquote(enabled: false)
// Tireli sözcükler ve aralıklar ("e-posta", "09.00-17.00", "5.000-20.000 TL") satır sonunda bölünmez.
#show regex("[\p{L}\p{N}]+(?:[.,]\p{N}+)*(?:-[\p{L}\p{N}]+(?:[.,]\p{N}+)*)+"): it => box(it)
#set par(justify: false, leading: 0.7em, spacing: 1.1em)
#set list(indent: 0.4em, body-indent: 0.6em, spacing: 0.75em)
#set enum(indent: 0.4em, body-indent: 0.6em, spacing: 0.75em)
#set strong(delta: 200)

#set page(
  paper: "a4",
  margin: (left: 2.2cm, right: 2.2cm, top: 2.5cm, bottom: 2.3cm),
  header: context if here().page() > 1 {
    block(width: 100%, stroke: (bottom: 0.5pt + renk.cizgi), inset: (bottom: 5pt),
      text(size: 8.5pt, fill: renk.soluk)[#rehber-adi · Sürüm #S.etiket])
  },
  footer: context if here().page() > 1 {
    block(width: 100%, stroke: (top: 0.5pt + renk.cizgi), inset: (top: 5pt),
      text(size: 8.5pt, fill: renk.soluk)[Yürürlük: #S.yururluk · Sayfa #counter(page).display() / #counter(page).final().first()])
  },
)

#set heading(
  numbering: (..n) => if n.pos().len() <= 2 { numbering("1.1", ..n) },
  supplement: none,
)
#show heading: set text(fill: renk.marka-koyu)
#show heading.where(level: 1): it => {
  pagebreak(weak: true)
  block(above: 0pt, below: 1.2em, text(size: 20pt, weight: "bold", fill: renk.marka, it))
}
#show heading.where(level: 2): it => block(above: 1.5em, below: 0.75em, sticky: true, text(size: 13pt, weight: "bold", it))
#show heading.where(level: 3): it => block(above: 1.1em, below: 0.6em, sticky: true, text(size: 10.5pt, weight: "bold", it))

#show outline.entry.where(level: 1): set block(above: 0.9em)
#show outline.entry.where(level: 1): set text(weight: "bold")
#show outline.entry: it => link(it.element.location(), it.indented(
  it.prefix(),
  it.body() + box(width: 1fr, inset: (x: 6pt), line(length: 100%, stroke: 0.4pt + renk.cizgi)) + it.page(),
))

// Kapak (1. sayfa; numara yazmaz ama sayılır).
#page(margin: (x: 2.2cm, y: 2.5cm))[
  #block(width: 100%, inset: (y: 14pt), stroke: (bottom: 3pt + renk.marka))[
    #text(size: 13pt, weight: "bold", fill: renk.marka)[Raşit Bankası]
  ]
  #v(5cm)
  #text(size: 30pt, weight: "bold", fill: renk.marka-koyu)[#rehber-adi]
  #v(0.8cm)
  #text(size: 14pt)[Sürüm #S.etiket \ Yürürlük tarihi: #S.yururluk]
  #v(0.6cm)
  #text(size: 11pt, fill: renk.soluk)[Bireysel müşterilerimiz için kartlar, hesaplar, krediler, para transferi, dijital bankacılık, yatırım, şube ve ATM hizmetleri ile ücretler]
  #v(1fr)
  #block(width: 100%, inset: 12pt, fill: renk.vurgu-acik, stroke: (left: 3pt + renk.vurgu))[
    #text(size: 10.5pt)[Bu rehber bilgilendirme amaçlıdır. \ Raşit Bankası kurgusal bir bankadır; bu rehber bir eğitim demosu için hazırlanmıştır.]
  ]
]

// uret.sh toplam sayfa sayısını buradan okur (typst eval).
#context [#metadata(counter(page).final().first()) <sayfa-sayisi>]

#outline(depth: 2)

#include "bolumler/01-bu-rehber-hakkinda.typ"
#include "bolumler/02-kartlar.typ"
#include "bolumler/03-hesaplar.typ"
#include "bolumler/04-krediler.typ"
#include "bolumler/05-para-transferi.typ"
#include "bolumler/06-dijital-bankacilik.typ"
#include "bolumler/07-yatirim.typ"
#include "bolumler/08-sube-atm-musteri-hizmetleri.typ"
#include "bolumler/09-sikayet-kisisel-veriler.typ"
#include "bolumler/10-ucret-tarifesi.typ"
#include "bolumler/11-terimler-sozlugu.typ"
