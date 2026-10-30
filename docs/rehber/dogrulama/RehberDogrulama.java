// Rehber PDF'lerini Apache PDFBox ile okuyup TASK-004 ve TASK-020 kontrollerini (D1-D10, D12-D14) çalıştırır.
// Çalıştırma: ../dogrula.sh (PDFBox jar'ını indirir ve bu dosyayı java ile başlatır).

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class RehberDogrulama {

    static final Locale TR = Locale.forLanguageTag("tr");
    static final String REHBER_ADI = "Raşit Bankası Ürün ve Hizmet Rehberi";
    static final String DOSYA_KALIBI = "Rasit-Bankasi-Urun-Hizmet-Rehberi-%s.pdf";
    static final Map<String, String> YURURLUK = Map.of("2026.10", "1 Ekim 2026", "2026.11", "1 Kasım 2026");
    static final String ESKI = "2026.10";
    static final String YENI = "2026.11";
    static final Pattern TOC_SATIRI = Pattern.compile("^(\\d+(?:\\.\\d+)?) (.+) (\\d+)$");
    static final Pattern ATIF = Pattern.compile("bkz\\. (\\d+(?:\\.\\d+)?)");
    static final String TARIFE_BOLUMU = "10";
    // Aynı sayfada iki gövde satırı arasındaki dikey mesafe bundan büyükse yeni paragraf başlar
    // (satır aralığı yaklaşık 15 pt, paragraf aralığı yaklaşık 19 pt).
    static final float PARAGRAF_ARALIGI = 17f;
    // Madde işareti, numaralı adım ya da örnek her zaman yeni paragraf başlatır (sayfa başında da).
    static final Pattern PARAGRAF_BASI = Pattern.compile("• |\\d+\\. |Örnek:");
    static final Pattern CUMLE_SONU = Pattern.compile("(?<=[.!?])\\s+(?=[\\p{Lu}\"(])");
    static final Pattern UCRETSIZ_DEGER = Pattern.compile("ücretsiz|alınmaz|komisyon alınmaz");
    static final Pattern UCRETSIZ_SOZ = Pattern.compile("(?<!\\p{L})(?:ücretsiz|alınmaz|ücreti? almaz)");
    static final String SAYI = "\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?|\\d+(?:,\\d+)?";
    // D13 ve D14'ün karşılaştırdığı değerler: tutar, döviz, yüzde, binde, puan, süre ve saat. Adet ve sayı değer değildir.
    static final Pattern BELIRTEC = Pattern.compile(
            "(?<![\\p{L}\\d.,])(?:(?<ilk>" + SAYI + ")\\s?-\\s?)?(?<sayi>" + SAYI + ") (?<birim>tl|abd doları|euro|iş günü|iş gün|dakika|saniye|saat|gün|hafta|ay|yıl|puan)(?<ek>\\p{L}*)"
                    + "|%(?<yuzde>" + SAYI + ")"
                    + "|(?<binde>on binde|binde) (?<bsayi>" + SAYI + ")"
                    + "|(?<![\\p{L}\\d.,])(?<saat>(?:[01]\\d|2[0-3])\\.[0-5]\\d)(?!\\d)");
    static final Map<String, String> BIRIM_ADI = Map.ofEntries(
            Map.entry("tl", "TL"), Map.entry("abd doları", "ABD doları"), Map.entry("euro", "euro"), Map.entry("iş günü", "iş günü"),
            Map.entry("iş gün", "iş günü"), Map.entry("dakika", "dakika"), Map.entry("saniye", "saniye"), Map.entry("saat", "saat"),
            Map.entry("gün", "gün"), Map.entry("hafta", "hafta"), Map.entry("ay", "ay"), Map.entry("yıl", "yıl"), Map.entry("puan", "puan"));

    record Toc(String no, String baslik, int sayfa) {}

    // Bölüm içinde numaralı başlığın (ör. "2.7"), bölüm girişinin ("2") ya da sık sorulanların ("2 SSS") satırları.
    record Kisim(String no, String bolum, List<PdfSatiri> satirlar) {
        boolean sss() {
            return no.endsWith(" SSS");
        }

        String ad() {
            return sss() ? bolum + ". bölüm Sık sorulanlar" : no.equals(bolum) ? bolum + ". bölüm girişi" : no;
        }
    }

    // D13'ün karşılaştırma birimi: düzyazıda bir cümle ya da bir tablo satırı.
    record Birim(Kisim kisim, int sayfa, String metin, String kucuk) {}

    record Soru(String soru, String cevap, int sayfa) {}

    record Belirtec(String tur, String deger, String yazim, int bas) {
        String anahtar() {
            return tur + ":" + deger;
        }
    }

    // kontroller.txt [TARIFE] girdisi: tarife satırının etiketi ve satırın bölümlerde anıldığı yerleri bulan kalıplar.
    record Tarife(String etiket, boolean yalnizTarife, List<Pattern> kaliplar) {}

    record TarifeSatiri(Tarife giris, String deger, String kisim, int sayfa) {}

    // Yazı tipi bilgisiyle bir metin satırı (üst ve alt bilgi hariç). D13 ve D14 bunlardan bölüm yapısını kurar.
    record PdfSatiri(int sayfa, String metin, float boyut, String agirlik, float y, float x) {
        boolean tablo() {
            return boyut < 10f;
        }

        boolean govde() {
            return boyut >= 10.5f && boyut < 12f;
        }

        boolean duz() {
            return agirlik.equals("Regular");
        }
    }

    static class Rehber {
        final String surum;
        final List<String> sayfalar = new ArrayList<>();
        final List<String> yerImleri = new ArrayList<>();
        final Map<String, Integer> yerImiSayfasi = new LinkedHashMap<>();
        final List<PdfSatiri> satirlar = new ArrayList<>();
        int goruntuSayisi;

        Rehber(String surum) {
            this.surum = surum;
        }

        int sayfaSayisi() {
            return sayfalar.size();
        }

        String duz(int sayfa) {
            return bosluk(sayfalar.get(sayfa - 1));
        }

        String tumu() {
            return bosluk(String.join("\n", sayfalar));
        }
    }

    static int kalan;
    static int gecen;

    public static void main(String[] args) throws IOException {
        Path pdfKlasoru = Path.of(args.length > 0 ? args[0] : "pdf");
        Kontroller k = Kontroller.oku(Path.of("dogrulama/kontroller.txt"));
        List<Fark> farklar = Fark.oku(Path.of("degisiklik-ozeti-2026.11.md"));
        List<String> yasaklar = yasakKelimeler(Path.of("rehberde-olmayan-konular.md"));

        Rehber eski = yukle(pdfKlasoru, ESKI);
        Rehber yeni = yukle(pdfKlasoru, YENI);
        Map<String, Rehber> rehberler = new LinkedHashMap<>();
        rehberler.put(ESKI, eski);
        rehberler.put(YENI, yeni);

        Map<String, List<Toc>> toclar = new LinkedHashMap<>();
        for (Rehber r : rehberler.values()) {
            toclar.put(r.surum, icindekiler(r));
        }

        d1(eski, yeni, k);
        d2(rehberler);
        d3(rehberler, k);
        d4(rehberler, k);
        d5(rehberler);
        d6(rehberler, toclar);
        d7(rehberler, toclar);
        d8(eski, yeni, farklar);
        d9(eski, yeni, toclar, k);
        d10(rehberler, yasaklar);
        d12(rehberler, k);
        Map<String, List<Kisim>> kisimlar = new LinkedHashMap<>();
        for (Rehber r : rehberler.values()) {
            kisimlar.put(r.surum, kisimlar(r, bolumBaslangiclari(toclar.get(r.surum)).get("1")));
        }
        d13(rehberler, kisimlar, k);
        d14(rehberler, kisimlar, soruSayisi(Path.of("kaynak/bolumler")));

        System.out.println();
        System.out.println("Toplam: " + gecen + " GEÇTİ, " + kalan + " KALDI");
        System.exit(kalan == 0 ? 0 : 1);
    }

    // --- Kontroller ---

    static void d1(Rehber eski, Rehber yeni, Kontroller k) {
        int a = eski.sayfaSayisi();
        int b = yeni.sayfaSayisi();
        boolean ok = a == b && a >= k.enAzSayfa && a <= k.enCokSayfa;
        sonuc("D1", "Sayfa sayısı", ok, ESKI + "=" + a + ", " + YENI + "=" + b + ", onaylanan aralık " + k.enAzSayfa + "-" + k.enCokSayfa);
    }

    static void d2(Map<String, Rehber> rehberler) {
        List<String> hatalar = new ArrayList<>();
        StringBuilder ozet = new StringBuilder();
        for (Rehber r : rehberler.values()) {
            int enAz = Integer.MAX_VALUE;
            for (int i = 1; i <= r.sayfaSayisi(); i++) {
                int uzunluk = r.duz(i).length();
                int sinir = i == 1 ? 80 : 300;
                if (uzunluk < sinir) {
                    hatalar.add(r.surum + " s." + i + " " + uzunluk + " karakter (en az " + sinir + ")");
                }
                if (i > 1) {
                    enAz = Math.min(enAz, uzunluk);
                }
            }
            if (r.goruntuSayisi > 0) {
                hatalar.add(r.surum + " " + r.goruntuSayisi + " görüntü nesnesi");
            }
            ozet.append(r.surum).append(": kapak ").append(r.duz(1).length()).append(", en kısa iç sayfa ").append(enAz).append(" karakter, görüntü ").append(r.goruntuSayisi).append("; ");
        }
        sonuc("D2", "Metin seçilebilir", hatalar.isEmpty(), hatalar.isEmpty() ? ozet.toString() : String.join("; ", hatalar));
    }

    static void d3(Map<String, Rehber> rehberler, Kontroller k) {
        List<String> hatalar = new ArrayList<>();
        // Büyük Ğ aranmaz: Türkçede Ğ ile başlayan sözcük yoktur, büyük Ğ yalnızca tamamı büyük harfli metinde çıkar.
        String harfler = "çğıİöşüÇÖŞÜâ";
        Pattern bolunmus = Pattern.compile("[a-zçğıöşü]-\\n[a-zçğıöşü]");
        for (Rehber r : rehberler.values()) {
            String ham = String.join("\n", r.sayfalar);
            for (char c : harfler.toCharArray()) {
                if (ham.indexOf(c) < 0) {
                    hatalar.add(r.surum + " '" + c + "' yok");
                }
            }
            long bozuk = ham.chars().filter(c -> c == 0xFFFD || c == 0x0307 || (c >= 0xFB00 && c <= 0xFB06)).count();
            if (bozuk > 0) {
                hatalar.add(r.surum + " " + bozuk + " bozuk/bitişik karakter");
            }
            Matcher m = bolunmus.matcher(ham);
            int bolunen = 0;
            while (m.find()) {
                bolunen++;
            }
            if (bolunen > 0) {
                hatalar.add(r.surum + " " + bolunen + " satır sonunda bölünmüş kelime");
            }
            String tum = r.tumu();
            for (String cumle : k.cumleler) {
                if (!tum.contains(cumle)) {
                    hatalar.add(r.surum + " kontrol cümlesi yok: " + cumle);
                }
            }
        }
        sonuc("D3", "Türkçe karakterler", hatalar.isEmpty(),
                hatalar.isEmpty() ? harfler.length() + " harf (" + harfler + ") var, bozuk karakter 0, bölünmüş kelime 0, " + k.cumleler.size() + " kontrol cümlesi bulundu" : String.join("; ", hatalar));
    }

    static void d4(Map<String, Rehber> rehberler, Kontroller k) {
        List<String> hatalar = new ArrayList<>();
        int sayi = 0;
        for (Kontroller.Satir s : k.satirlar) {
            for (Rehber r : rehberler.values()) {
                if (!s.surum().equals("*") && !s.surum().equals(r.surum)) {
                    continue;
                }
                sayi++;
                boolean bulundu = false;
                for (String sayfa : r.sayfalar) {
                    for (String satir : sayfa.split("\n")) {
                        if (siraliIcerir(satir, s.hucreler())) {
                            bulundu = true;
                            break;
                        }
                    }
                    if (bulundu) {
                        break;
                    }
                }
                if (!bulundu) {
                    hatalar.add(r.surum + " tek satırda yok: " + String.join(" | ", s.hucreler()));
                }
            }
        }
        sonuc("D4", "Tablo satırları", hatalar.isEmpty(), hatalar.isEmpty() ? sayi + " satır kontrolü tek satırda ve sütun sırasıyla bulundu" : String.join("; ", hatalar));
    }

    static void d5(Map<String, Rehber> rehberler) {
        List<String> hatalar = new ArrayList<>();
        for (Rehber r : rehberler.values()) {
            String ust = REHBER_ADI + " · Sürüm " + r.surum;
            int t = r.sayfaSayisi();
            for (int i = 1; i <= t; i++) {
                List<String> satirlar = doluSatirlar(r.sayfalar.get(i - 1));
                String ilk = satirlar.get(0);
                String son = satirlar.get(satirlar.size() - 1);
                String alt = "Yürürlük: " + YURURLUK.get(r.surum) + " · Sayfa " + i + " / " + t;
                if (i == 1) {
                    if (ilk.startsWith(REHBER_ADI + " · Sürüm") || son.startsWith("Yürürlük:")) {
                        hatalar.add(r.surum + " kapakta üst/alt bilgi var");
                    }
                    continue;
                }
                if (!ilk.equals(ust)) {
                    hatalar.add(r.surum + " s." + i + " üst bilgi: '" + ilk + "'");
                }
                if (!son.equals(alt)) {
                    hatalar.add(r.surum + " s." + i + " alt bilgi: '" + son + "'");
                }
            }
        }
        sonuc("D5", "Üst ve alt bilgi", hatalar.isEmpty(), hatalar.isEmpty() ? "her iki PDF'te 2. sayfadan itibaren her sayfada doğru; kapakta yok" : String.join("; ", hatalar));
    }

    static void d6(Map<String, Rehber> rehberler, Map<String, List<Toc>> toclar) {
        List<String> hatalar = new ArrayList<>();
        StringBuilder ozet = new StringBuilder();
        for (Rehber r : rehberler.values()) {
            List<Toc> toc = toclar.get(r.surum);
            Map<String, Integer> numaraliYerImleri = new LinkedHashMap<>();
            for (String yerImi : r.yerImleri) {
                if (yerImi.matches("^\\d+(?:\\.\\d+)? .+")) {
                    numaraliYerImleri.put(yerImi, r.yerImiSayfasi.get(yerImi));
                }
            }
            Set<String> tocBasliklari = new LinkedHashSet<>();
            for (Toc e : toc) {
                String tam = e.no() + " " + e.baslik();
                tocBasliklari.add(tam);
                List<String> satirlar = doluSatirlar(r.sayfalar.get(e.sayfa() - 1));
                if (!satirlar.contains(tam)) {
                    hatalar.add(r.surum + " '" + tam + "' s." + e.sayfa() + " üzerinde başlık olarak yok");
                }
                if (!e.no().contains(".") && !satirlar.get(1).equals(tam)) {
                    hatalar.add(r.surum + " bölüm '" + tam + "' sayfanın ilk başlığı değil");
                }
                Integer yerImiSayfa = numaraliYerImleri.get(tam);
                if (yerImiSayfa == null) {
                    hatalar.add(r.surum + " yer imi yok: " + tam);
                } else if (yerImiSayfa != e.sayfa()) {
                    hatalar.add(r.surum + " yer imi sayfası " + yerImiSayfa + " ≠ içindekiler " + e.sayfa() + ": " + tam);
                }
            }
            for (String yerImi : numaraliYerImleri.keySet()) {
                if (!tocBasliklari.contains(yerImi)) {
                    hatalar.add(r.surum + " içindekilerde olmayan yer imi: " + yerImi);
                }
            }
            long bolum = toc.stream().filter(e -> !e.no().contains(".")).count();
            ozet.append(r.surum).append(": ").append(toc.size()).append(" numaralı başlık (").append(bolum).append(" bölüm); ");
        }
        sonuc("D6", "İçindekiler ve yer imleri", hatalar.isEmpty(), hatalar.isEmpty() ? ozet + "sayfa numaraları ve yer imleri eşleşiyor" : String.join("; ", hatalar));
    }

    static void d7(Map<String, Rehber> rehberler, Map<String, List<Toc>> toclar) {
        List<String> hatalar = new ArrayList<>();
        StringBuilder ozet = new StringBuilder();
        for (Rehber r : rehberler.values()) {
            Set<String> numaralar = new LinkedHashSet<>();
            toclar.get(r.surum).forEach(e -> numaralar.add(e.no()));
            Matcher m = ATIF.matcher(r.tumu());
            int sayi = 0;
            Set<String> farkli = new LinkedHashSet<>();
            while (m.find()) {
                sayi++;
                farkli.add(m.group(1));
                if (!numaralar.contains(m.group(1))) {
                    hatalar.add(r.surum + " olmayan başlığa atıf: bkz. " + m.group(1));
                }
            }
            if (sayi < 10) {
                hatalar.add(r.surum + " yalnızca " + sayi + " atıf");
            }
            ozet.append(r.surum).append(": ").append(sayi).append(" atıf, ").append(farkli.size()).append(" farklı başlık; ");
        }
        sonuc("D7", "Çapraz atıflar", hatalar.isEmpty(), hatalar.isEmpty() ? ozet + "hepsi var olan başlıkları gösteriyor" : String.join("; ", hatalar));
    }

    static void d8(Rehber eski, Rehber yeni, List<Fark> farklar) {
        List<String> hatalar = new ArrayList<>();
        if (farklar.isEmpty()) {
            hatalar.add("değişiklik özetinde Ayrıntı satırı okunamadı");
        }
        for (Fark f : farklar) {
            if (!f.eskiMetin().equals("—")) {
                if (f.eskiSayfa() < 1 || f.eskiSayfa() > eski.sayfaSayisi() || !eski.duz(f.eskiSayfa()).contains(f.eskiMetin())) {
                    hatalar.add(f.no() + " " + ESKI + " s." + f.eskiSayfa() + " üzerinde yok: " + f.eskiMetin());
                }
                if (yeni.tumu().contains(f.eskiMetin())) {
                    hatalar.add(f.no() + " eski metin " + YENI + " içinde hâlâ var: " + f.eskiMetin());
                }
            }
            if (!f.yeniMetin().equals("—")) {
                if (f.yeniSayfa() < 1 || f.yeniSayfa() > yeni.sayfaSayisi() || !yeni.duz(f.yeniSayfa()).contains(f.yeniMetin())) {
                    hatalar.add(f.no() + " " + YENI + " s." + f.yeniSayfa() + " üzerinde yok: " + f.yeniMetin());
                }
                if (eski.tumu().contains(f.yeniMetin())) {
                    hatalar.add(f.no() + " yeni metin " + ESKI + " içinde de var: " + f.yeniMetin());
                }
            }
        }
        sayimKontrol(hatalar, yeni, "1.450 TL", 0);
        sayimKontrol(hatalar, eski, "1.650 TL", 0);
        sayimKontrol(hatalar, eski, "Raşit Genç", 0);
        if (kucuk(yeni.tumu()).contains("şifre cihazı")) {
            hatalar.add(YENI + " içinde 'şifre cihazı' geçiyor");
        }
        long eskiGold = farklar.stream().filter(f -> f.eskiMetin().contains("1.450 TL")).count();
        long yeniGold = farklar.stream().filter(f -> f.yeniMetin().contains("1.650 TL")).count();
        sayimKontrol(hatalar, eski, "1.450 TL", (int) eskiGold);
        sayimKontrol(hatalar, yeni, "1.650 TL", (int) yeniGold);
        sonuc("D8", "Değişiklik özeti", hatalar.isEmpty(),
                hatalar.isEmpty() ? farklar.size() + " satır yazılan sayfada bulundu, eski değerler " + YENI + "'de yok; '1.450 TL' " + eskiGold + " kez, '1.650 TL' " + yeniGold + " kez (yalnızca Gold ücreti)" : String.join("; ", hatalar));
    }

    static void d9(Rehber eski, Rehber yeni, Map<String, List<Toc>> toclar, Kontroller k) {
        List<String> hatalar = new ArrayList<>();
        Map<String, Integer> eskiBas = bolumBaslangiclari(toclar.get(ESKI));
        Map<String, Integer> yeniBas = bolumBaslangiclari(toclar.get(YENI));
        if (!eskiBas.equals(yeniBas)) {
            hatalar.add("bölüm başlangıçları farklı: " + eskiBas + " / " + yeniBas);
        }
        List<String> bolumler = new ArrayList<>(eskiBas.keySet());
        int karsilastirilan = 0;
        for (String b : k.degismeyenBolumler) {
            int idx = bolumler.indexOf(b);
            if (idx < 0) {
                hatalar.add("bölüm " + b + " içindekilerde yok");
                continue;
            }
            int bas = eskiBas.get(b);
            int bit = idx + 1 < bolumler.size() ? eskiBas.get(bolumler.get(idx + 1)) - 1 : eski.sayfaSayisi();
            for (int s = bas; s <= bit; s++) {
                karsilastirilan++;
                if (!govde(eski.sayfalar.get(s - 1)).equals(govde(yeni.sayfalar.get(s - 1)))) {
                    hatalar.add("bölüm " + b + " s." + s + " iki sürümde farklı");
                }
            }
        }
        sonuc("D9", "Sayfa düzeni", hatalar.isEmpty(),
                hatalar.isEmpty() ? "bölüm başlangıçları aynı " + eskiBas + "; değişmeyen bölümler " + k.degismeyenBolumler + " (" + karsilastirilan + " sayfa) birebir aynı" : String.join("; ", hatalar));
    }

    static void d10(Map<String, Rehber> rehberler, List<String> yasaklar) {
        List<String> hatalar = new ArrayList<>();
        if (yasaklar.isEmpty()) {
            hatalar.add("rehberde-olmayan-konular.md içinde yasak kelime okunamadı");
        }
        for (Rehber r : rehberler.values()) {
            String metin = kucuk(r.tumu());
            for (String y : yasaklar) {
                if (kelimeVar(metin, kucuk(y))) {
                    hatalar.add(r.surum + " yasak kelime geçiyor: " + y);
                }
            }
        }
        sonuc("D10", "Rehberde olmayan konular", hatalar.isEmpty(), hatalar.isEmpty() ? yasaklar.size() + " yasak kelime iki PDF'te de geçmiyor" : String.join("; ", hatalar));
    }

    static void d12(Map<String, Rehber> rehberler, Kontroller k) {
        List<String> hatalar = new ArrayList<>();
        Pattern adres = Pattern.compile("(?i)[\\w.-]+@[\\w-]+(?:\\.[\\w-]+)+|www\\.[\\w-]+(?:\\.[\\w-]+)+|\\b[\\w-]+\\.(?:com|net|org|gov|io)(?:\\.tr)?\\b");
        Set<String> adresler = new LinkedHashSet<>();
        for (Rehber r : rehberler.values()) {
            String kapak = r.duz(1);
            for (String not : k.kapakNotlari) {
                if (!kapak.contains(not)) {
                    hatalar.add(r.surum + " kapakta yok: " + not);
                }
            }
            String metin = kucuk(r.tumu());
            for (String banka : k.gercekBankalar) {
                if (kelimeVar(metin, kucuk(banka))) {
                    hatalar.add(r.surum + " gerçek banka adı geçiyor: " + banka);
                }
            }
            Matcher m = adres.matcher(r.tumu());
            while (m.find()) {
                String a = m.group().replaceAll("[.,;:]+$", "");
                adresler.add(a);
                if (!a.toLowerCase(Locale.ROOT).endsWith("rasitbank.example")) {
                    hatalar.add(r.surum + " kurgusal olmayan adres: " + a);
                }
            }
        }
        sonuc("D12", "Kurgu sınırı", hatalar.isEmpty(),
                hatalar.isEmpty() ? "kapak notları var; " + k.gercekBankalar.size() + " gerçek banka adı geçmiyor; adresler " + adresler : String.join("; ", hatalar));
    }


    static void d13(Map<String, Rehber> rehberler, Map<String, List<Kisim>> kisimlar, Kontroller k) {
        List<String> hatalar = new ArrayList<>();
        StringBuilder ozet = new StringBuilder();
        Set<String> kullanilanEtiketler = new LinkedHashSet<>();
        for (Rehber r : rehberler.values()) {
            List<Kisim> ks = kisimlar.get(r.surum);
            List<Birim> birimler = birimler(ks);
            List<TarifeSatiri> tarife = new ArrayList<>();
            for (Kisim kisim : ks) {
                if (!kisim.bolum.equals(TARIFE_BOLUMU) || kisim.no.equals(TARIFE_BOLUMU)) {
                    continue;
                }
                for (PdfSatiri s : kisim.satirlar) {
                    if (!s.tablo() || !s.duz()) {
                        continue;
                    }
                    Tarife giris = k.tarifeGirisi(s.metin());
                    if (giris == null) {
                        hatalar.add(r.surum + " kontroller.txt [TARIFE] içinde girdisi olmayan tarife satırı: " + kisim.no + " s." + s.sayfa() + " '" + s.metin() + "'");
                        continue;
                    }
                    if (!kullanilanEtiketler.add(r.surum + "\u0000" + giris.etiket())) {
                        hatalar.add(r.surum + " tarife satırı iki kez geçiyor: " + giris.etiket());
                    }
                    tarife.add(new TarifeSatiri(giris, s.metin().substring(giris.etiket().length()).strip(), kisim.no, s.sayfa()));
                }
            }
            int dogrulanan = 0;
            int yer = 0;
            int yalniz = 0;
            for (TarifeSatiri t : tarife) {
                if (t.giris().yalnizTarife()) {
                    yalniz++;
                    continue;
                }
                List<Belirtec> tarifeDegerleri = belirtecler(kucuk(t.deger()));
                String tur = UCRETSIZ_DEGER.matcher(kucuk(t.deger())).matches() ? "ucretsiz" : tarifeDegerleri.isEmpty() ? "metin" : "tutar";
                int gecis = 0;
                for (Birim b : birimler) {
                    Set<String> celiskiler = new LinkedHashSet<>();
                    boolean ayni = false;
                    for (Pattern kalip : t.giris().kaliplar()) {
                        Matcher m = kalip.matcher(b.kucuk());
                        while (m.find()) {
                            String sonuc = karsilastir(tur, tarifeDegerleri, b.kucuk().substring(m.end()));
                            if (sonuc == null) {
                                continue;
                            }
                            if (sonuc.isEmpty()) {
                                ayni = true;
                            } else {
                                celiskiler.add(sonuc);
                            }
                        }
                    }
                    for (String c : celiskiler) {
                        hatalar.add(r.surum + " çelişki: '" + t.giris().etiket() + "' " + t.kisim() + " s." + t.sayfa() + " '" + t.deger() + "' ↔ "
                                + b.kisim().ad() + " s." + b.sayfa() + " '" + kisalt(b.metin()) + "' (" + c + ")");
                    }
                    if (ayni && celiskiler.isEmpty()) {
                        gecis++;
                    }
                }
                if (gecis == 0) {
                    hatalar.add(r.surum + " bölümlerde doğrulanan geçiş bulunamadı: '" + t.giris().etiket() + "' (" + t.kisim() + " s." + t.sayfa() + ")");
                } else {
                    dogrulanan++;
                    yer += gecis;
                }
            }
            ozet.append(r.surum).append(": ").append(tarife.size()).append(" tarife satırı, ").append(dogrulanan).append("'i bölümlerde ").append(yer)
                    .append(" yerde doğrulandı, ").append(yalniz).append("'i yalnız tarifede; ");
        }
        for (Tarife giris : k.tarife) {
            if (kullanilanEtiketler.stream().noneMatch(e -> e.endsWith("\u0000" + giris.etiket()))) {
                hatalar.add("kontroller.txt [TARIFE] girdisi iki sürümün tarifesinde de yok: " + giris.etiket());
            }
        }
        if (k.tarife.isEmpty()) {
            hatalar.add("kontroller.txt [TARIFE] bölümü boş");
        }
        sonuc("D13", "Tarife tutarlılığı", hatalar.isEmpty(), hatalar.isEmpty() ? ozet.toString() : String.join("; ", hatalar));
    }

    static void d14(Map<String, Rehber> rehberler, Map<String, List<Kisim>> kisimlar, int kaynaktakiSoru) {
        List<String> hatalar = new ArrayList<>();
        StringBuilder ozet = new StringBuilder();
        for (Rehber r : rehberler.values()) {
            List<Kisim> ks = kisimlar.get(r.surum);
            int soruSayisi = 0;
            int degerSayisi = 0;
            for (Kisim kisim : ks) {
                if (!kisim.sss()) {
                    continue;
                }
                for (Soru soru : sorular(kisim)) {
                    soruSayisi++;
                    String yer = r.surum + " " + kisim.ad() + " s." + soru.sayfa() + " '" + kisalt(soru.soru()) + "'";
                    if (soru.cevap().isEmpty()) {
                        hatalar.add(yer + ": cevap yok");
                        continue;
                    }
                    List<String> hedefler = new ArrayList<>();
                    Matcher m = ATIF.matcher(soru.cevap());
                    while (m.find()) {
                        hedefler.add(m.group(1));
                    }
                    if (hedefler.isEmpty()) {
                        hatalar.add(yer + ": cevapta 'bkz.' atfı yok");
                        continue;
                    }
                    List<Belirtec> hedefDegerleri = new ArrayList<>();
                    for (String h : hedefler) {
                        hedefDegerleri.addAll(belirtecler(atifsiz(kucuk(kisimMetni(ks, h)))));
                    }
                    Set<String> hedefAnahtarlari = new LinkedHashSet<>();
                    hedefDegerleri.forEach(b -> hedefAnahtarlari.add(b.anahtar()));
                    for (Belirtec b : belirtecler(atifsiz(kucuk(soru.cevap())))) {
                        degerSayisi++;
                        if (!hedefAnahtarlari.contains(b.anahtar())) {
                            Set<String> ayniTur = new LinkedHashSet<>();
                            hedefDegerleri.stream().filter(h -> h.tur().equals(b.tur())).forEach(h -> ayniTur.add(h.yazim()));
                            hatalar.add(yer + ": '" + b.yazim() + "' atıf yapılan başlıkta (bkz. " + String.join(", ", hedefler) + ") yok; oradaki aynı türden değerler: "
                                    + (ayniTur.isEmpty() ? "yok" : String.join(", ", ayniTur)));
                        }
                    }
                }
            }
            if (soruSayisi != kaynaktakiSoru) {
                hatalar.add(r.surum + " PDF'te " + soruSayisi + " soru bulundu, kaynakta " + kaynaktakiSoru + " soru var");
            }
            ozet.append(r.surum).append(": ").append(soruSayisi).append(" soru, ").append(degerSayisi).append(" değer; ");
        }
        sonuc("D14", "Sık sorulanlar ve atıflar", hatalar.isEmpty(),
                hatalar.isEmpty() ? ozet + "her değer atıf yapılan başlıkta yazılı" : String.join("; ", hatalar));
    }

    // --- Yardımcılar ---

    static Rehber yukle(Path klasor, String surum) throws IOException {
        File dosya = klasor.resolve(String.format(DOSYA_KALIBI, surum)).toFile();
        Rehber r = new Rehber(surum);
        try (PDDocument doc = Loader.loadPDF(dosya)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                r.sayfalar.add(stripper.getText(doc));
            }
            SatirToplayici toplayici = new SatirToplayici();
            for (int i = 2; i <= doc.getNumberOfPages(); i++) {
                List<PdfSatiri> sayfaSatirlari = toplayici.topla(doc, i);
                r.satirlar.addAll(sayfaSatirlari.subList(1, sayfaSatirlari.size() - 1));
            }
            for (PDPage sayfa : doc.getPages()) {
                for (COSName ad : sayfa.getResources().getXObjectNames()) {
                    if (sayfa.getResources().getXObject(ad) instanceof PDImageXObject) {
                        r.goruntuSayisi++;
                    }
                }
            }
            if (doc.getDocumentCatalog().getDocumentOutline() != null) {
                yerImleriniTopla(doc, doc.getDocumentCatalog().getDocumentOutline(), r);
            }
        }
        return r;
    }

    static List<Kisim> kisimlar(Rehber r, int ilkSayfa) {
        List<Kisim> kisimlar = new ArrayList<>();
        Pattern bolumBasligi = Pattern.compile("^(\\d+) .+");
        Pattern altBaslik = Pattern.compile("^(\\d+\\.\\d+) .+");
        Kisim simdiki = null;
        String bolum = null;
        for (PdfSatiri s : r.satirlar) {
            if (s.sayfa() < ilkSayfa) {
                continue;
            }
            Matcher b = bolumBasligi.matcher(s.metin());
            Matcher a = altBaslik.matcher(s.metin());
            if (s.boyut() >= 15f && s.agirlik().equals("Bold") && b.matches()) {
                bolum = b.group(1);
                simdiki = new Kisim(bolum, bolum, new ArrayList<>());
                kisimlar.add(simdiki);
            } else if (s.boyut() >= 12f && s.boyut() < 15f && s.agirlik().equals("Bold") && bolum != null && (a.matches() || s.metin().equals("Sık sorulanlar"))) {
                simdiki = new Kisim(a.matches() ? a.group(1) : bolum + " SSS", bolum, new ArrayList<>());
                kisimlar.add(simdiki);
            } else if (simdiki != null) {
                simdiki.satirlar().add(s);
            }
        }
        return kisimlar;
    }

    static List<Birim> birimler(List<Kisim> kisimlar) {
        List<Birim> birimler = new ArrayList<>();
        for (Kisim kisim : kisimlar) {
            if (Integer.parseInt(kisim.bolum()) >= Integer.parseInt(TARIFE_BOLUMU)) {
                continue;
            }
            StringBuilder paragraf = new StringBuilder();
            List<int[]> satirBaslari = new ArrayList<>();
            PdfSatiri onceki = null;
            for (PdfSatiri s : kisim.satirlar()) {
                boolean govde = s.govde() && s.duz();
                if (govde) {
                    boolean yeni = paragraf.isEmpty() || PARAGRAF_BASI.matcher(s.metin()).lookingAt() || onceki == null
                            || (onceki.sayfa() == s.sayfa() && s.y() - onceki.y() > PARAGRAF_ARALIGI);
                    if (yeni) {
                        paragrafBirimleri(kisim, paragraf, satirBaslari, birimler);
                    } else {
                        paragraf.append(' ');
                    }
                    satirBaslari.add(new int[] {paragraf.length(), s.sayfa()});
                    paragraf.append(s.metin());
                    onceki = s;
                    continue;
                }
                paragrafBirimleri(kisim, paragraf, satirBaslari, birimler);
                onceki = null;
                if (s.tablo() && s.duz()) {
                    birimler.add(birim(kisim, s.sayfa(), s.metin()));
                }
            }
            paragrafBirimleri(kisim, paragraf, satirBaslari, birimler);
        }
        return birimler;
    }

    static void paragrafBirimleri(Kisim kisim, StringBuilder paragraf, List<int[]> satirBaslari, List<Birim> birimler) {
        String metin = paragraf.toString();
        if (!metin.replaceFirst("^• ", "").startsWith("Örnek:")) {
            Matcher m = CUMLE_SONU.matcher(metin);
            int bas = 0;
            while (bas < metin.length()) {
                int bit = m.find() ? m.start() : metin.length();
                int sayfa = satirBaslari.get(0)[1];
                for (int[] sb : satirBaslari) {
                    if (sb[0] <= bas) {
                        sayfa = sb[1];
                    }
                }
                birimler.add(birim(kisim, sayfa, metin.substring(bas, bit)));
                bas = bit == metin.length() ? bit : m.end();
            }
        }
        paragraf.setLength(0);
        satirBaslari.clear();
    }

    static Birim birim(Kisim kisim, int sayfa, String metin) {
        return new Birim(kisim, sayfa, metin.strip(), atifsiz(kucuk(bosluk(metin))));
    }

    static List<Soru> sorular(Kisim kisim) {
        List<Soru> sorular = new ArrayList<>();
        StringBuilder soru = null;
        StringBuilder cevap = null;
        int sayfa = 0;
        for (PdfSatiri s : kisim.satirlar()) {
            if (!s.govde()) {
                continue;
            }
            if (!s.duz()) {
                if (soru == null || !cevap.isEmpty()) {
                    if (soru != null) {
                        sorular.add(new Soru(bosluk(soru.toString()), bosluk(cevap.toString()), sayfa));
                    }
                    soru = new StringBuilder();
                    cevap = new StringBuilder();
                    sayfa = s.sayfa();
                }
                soru.append(' ').append(s.metin());
            } else if (soru != null) {
                cevap.append(' ').append(s.metin());
            }
        }
        if (soru != null) {
            sorular.add(new Soru(bosluk(soru.toString()), bosluk(cevap.toString()), sayfa));
        }
        return sorular;
    }

    static String kisimMetni(List<Kisim> kisimlar, String no) {
        StringBuilder metin = new StringBuilder();
        for (Kisim k : kisimlar) {
            if (no.contains(".") ? k.no().equals(no) : k.bolum().equals(no) && !k.sss()) {
                k.satirlar().forEach(s -> metin.append(s.metin()).append('\n'));
            }
        }
        return bosluk(metin.toString());
    }

    static List<Belirtec> belirtecler(String kucukMetin) {
        List<Belirtec> sonuc = new ArrayList<>();
        Matcher m = BELIRTEC.matcher(kucukMetin);
        while (m.find()) {
            if (m.group("sayi") != null) {
                String birim = m.group("birim");
                String ek = m.group("ek");
                if (birim.equals("ay") && (ek.startsWith("r") || ek.startsWith("n"))) {
                    continue;
                }
                String ad = BIRIM_ADI.get(birim);
                if (m.group("ilk") != null) {
                    sonuc.add(new Belirtec(ad, sayi(m.group("ilk")), m.group("ilk") + " " + ad, m.start("ilk")));
                }
                sonuc.add(new Belirtec(ad, sayi(m.group("sayi")), m.group("sayi") + " " + ad, m.start("sayi")));
            } else if (m.group("yuzde") != null) {
                sonuc.add(new Belirtec("%", sayi(m.group("yuzde")), "%" + m.group("yuzde"), m.start()));
            } else if (m.group("binde") != null) {
                sonuc.add(new Belirtec(m.group("binde"), sayi(m.group("bsayi")), m.group("binde") + " " + m.group("bsayi"), m.start()));
            } else {
                sonuc.add(new Belirtec("saat (ss.dd)", m.group("saat"), m.group("saat"), m.start()));
            }
        }
        return sonuc;
    }

    static String sayi(String yazim) {
        return new BigDecimal(yazim.replace(".", "").replace(',', '.')).stripTrailingZeros().toPlainString();
    }

    // Sonuç: null = pencerede karşılaştırılacak değer yok; "" = değer tarifeyle aynı; diğer = çelişkinin açıklaması.
    static String karsilastir(String tur, List<Belirtec> tarifeDegerleri, String pencere) {
        List<Belirtec> degerler = belirtecler(pencere);
        Matcher u = UCRETSIZ_SOZ.matcher(pencere);
        int ucretsiz = u.find() ? u.start() : Integer.MAX_VALUE;
        int ilkDeger = degerler.isEmpty() ? Integer.MAX_VALUE : degerler.get(0).bas();
        switch (tur) {
            case "ucretsiz" -> {
                if (ilkDeger < ucretsiz) {
                    return "bölümde " + degerler.get(0).yazim() + ", tarifede ücretsiz";
                }
                return ucretsiz < Integer.MAX_VALUE ? "" : null;
            }
            case "metin" -> {
                return degerler.isEmpty() ? "" : "bölümde " + degerler.get(0).yazim() + ", tarifede değer yok";
            }
            default -> {
                if (ucretsiz < ilkDeger) {
                    return "bölümde ücretsiz, tarifede " + tarifeDegerleri.get(0).yazim();
                }
                Map<String, Belirtec> ilkler = new LinkedHashMap<>();
                degerler.forEach(d -> ilkler.putIfAbsent(d.tur(), d));
                Map<String, Belirtec> tarifeIlkleri = new LinkedHashMap<>();
                tarifeDegerleri.forEach(d -> tarifeIlkleri.putIfAbsent(d.tur(), d));
                int karsilastirilan = 0;
                for (Belirtec t : tarifeIlkleri.values()) {
                    Belirtec d = ilkler.get(t.tur());
                    if (d == null) {
                        continue;
                    }
                    karsilastirilan++;
                    if (!d.deger().equals(t.deger())) {
                        return "bölümde " + d.yazim() + ", tarifede " + t.yazim();
                    }
                }
                return karsilastirilan > 0 ? "" : null;
            }
        }
    }

    static String atifsiz(String kucukMetin) {
        return ATIF.matcher(kucukMetin).replaceAll("bkz.");
    }

    static String kisalt(String metin) {
        return metin.length() <= 100 ? metin : metin.substring(0, 99) + "…";
    }

    static int soruSayisi(Path klasor) throws IOException {
        int sayi = 0;
        try (var dosyalar = Files.list(klasor)) {
            for (Path p : dosyalar.filter(d -> d.toString().endsWith(".typ")).sorted().toList()) {
                String metin = Files.readString(p, StandardCharsets.UTF_8);
                for (int i = metin.indexOf("#soru["); i >= 0; i = metin.indexOf("#soru[", i + 1)) {
                    sayi++;
                }
            }
        }
        return sayi;
    }

    static String agirlik(String yaziTipi) {
        return yaziTipi.contains("Semibold") ? "Semibold" : yaziTipi.contains("Bold") ? "Bold" : "Regular";
    }

    // Her satırı ilk karakterinin yazı tipi boyutu, ağırlığı ve konumuyla toplar.
    static class SatirToplayici extends PDFTextStripper {
        private final List<PdfSatiri> satirlar = new ArrayList<>();
        private final StringBuilder metin = new StringBuilder();
        private TextPosition ilk;
        private int sayfa;

        List<PdfSatiri> topla(PDDocument doc, int sayfa) throws IOException {
            this.sayfa = sayfa;
            satirlar.clear();
            setStartPage(sayfa);
            setEndPage(sayfa);
            getText(doc);
            satiriBitir();
            return new ArrayList<>(satirlar);
        }

        @Override
        protected void writeString(String text, List<TextPosition> konumlar) {
            if (ilk == null && !konumlar.isEmpty()) {
                ilk = konumlar.get(0);
            }
            metin.append(text);
        }

        @Override
        protected void writeWordSeparator() {
            metin.append(' ');
        }

        @Override
        protected void writeLineSeparator() {
            satiriBitir();
        }

        private void satiriBitir() {
            String s = metin.toString().strip();
            if (!s.isEmpty() && ilk != null) {
                satirlar.add(new PdfSatiri(sayfa, s, ilk.getFontSizeInPt(), agirlik(ilk.getFont().getName()), ilk.getYDirAdj(), ilk.getXDirAdj()));
            }
            metin.setLength(0);
            ilk = null;
        }
    }

    static void yerImleriniTopla(PDDocument doc, PDOutlineNode dugum, Rehber r) throws IOException {
        for (PDOutlineItem oge : dugum.children()) {
            String baslik = oge.getTitle().strip();
            PDPage sayfa = oge.findDestinationPage(doc);
            r.yerImleri.add(baslik);
            r.yerImiSayfasi.put(baslik, sayfa == null ? -1 : doc.getPages().indexOf(sayfa) + 1);
            yerImleriniTopla(doc, oge, r);
        }
    }

    static List<Toc> icindekiler(Rehber r) {
        List<Toc> toc = new ArrayList<>();
        int bolumBir = -1;
        for (int i = 2; i <= r.sayfaSayisi(); i++) {
            List<String> satirlar = doluSatirlar(r.sayfalar.get(i - 1));
            if (bolumBir > 0 && i >= bolumBir) {
                break;
            }
            for (String satir : satirlar.subList(1, satirlar.size() - 1)) {
                Matcher m = TOC_SATIRI.matcher(satir);
                if (m.matches()) {
                    Toc e = new Toc(m.group(1), m.group(2), Integer.parseInt(m.group(3)));
                    if (e.no().equals("1")) {
                        bolumBir = e.sayfa();
                    }
                    toc.add(e);
                }
            }
        }
        return toc;
    }

    static Map<String, Integer> bolumBaslangiclari(List<Toc> toc) {
        Map<String, Integer> m = new LinkedHashMap<>();
        toc.stream().filter(e -> !e.no().contains(".")).forEach(e -> m.put(e.no(), e.sayfa()));
        return m;
    }

    static String govde(String sayfa) {
        List<String> satirlar = doluSatirlar(sayfa);
        return String.join("\n", satirlar.subList(1, satirlar.size() - 1));
    }

    static List<String> doluSatirlar(String sayfa) {
        List<String> satirlar = new ArrayList<>();
        for (String s : sayfa.split("\n")) {
            if (!s.isBlank()) {
                satirlar.add(s.strip());
            }
        }
        return satirlar;
    }

    static boolean siraliIcerir(String satir, List<String> hucreler) {
        int konum = 0;
        for (String h : hucreler) {
            int bulunan = satir.indexOf(h, konum);
            if (bulunan < 0) {
                return false;
            }
            konum = bulunan + h.length();
        }
        return true;
    }

    static void sayimKontrol(List<String> hatalar, Rehber r, String dize, int beklenen) {
        int sayi = 0;
        String metin = r.tumu();
        for (int i = metin.indexOf(dize); i >= 0; i = metin.indexOf(dize, i + 1)) {
            sayi++;
        }
        if (sayi != beklenen) {
            hatalar.add(r.surum + " '" + dize + "' " + sayi + " kez geçiyor, beklenen " + beklenen);
        }
    }

    static boolean kelimeVar(String metin, String kelime) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(kelime) + "(?![\\p{L}\\p{N}])").matcher(metin).find();
    }

    static String kucuk(String s) {
        return s.toLowerCase(TR);
    }

    static String bosluk(String s) {
        return s.replaceAll("\\s+", " ").strip();
    }

    static List<String> yasakKelimeler(Path dosya) throws IOException {
        List<String> kelimeler = new ArrayList<>();
        Pattern tirnak = Pattern.compile("`([^`]+)`");
        for (String satir : Files.readAllLines(dosya, StandardCharsets.UTF_8)) {
            if (satir.strip().startsWith("- **Yasak kelimeler:**")) {
                Matcher m = tirnak.matcher(satir);
                while (m.find()) {
                    kelimeler.add(m.group(1));
                }
            }
        }
        return kelimeler;
    }

    static void sonuc(String kod, String ad, boolean ok, String ayrinti) {
        if (ok) {
            gecen++;
        } else {
            kalan++;
        }
        System.out.println(kod + " " + (ok ? "GEÇTİ" : "KALDI") + " · " + ad + " · " + ayrinti);
    }

    record Fark(String no, String eskiMetin, String yeniMetin, int eskiSayfa, int yeniSayfa) {
        // "Ayrıntı" tablosu: | # | Tür | Konu | 2026.10 metni | 2026.11 metni | Bölüm | Sayfa (2026.10) | Sayfa (2026.11) | Yürürlük |
        static List<Fark> oku(Path dosya) throws IOException {
            List<Fark> farklar = new ArrayList<>();
            boolean ayrinti = false;
            for (String satir : Files.readAllLines(dosya, StandardCharsets.UTF_8)) {
                if (satir.startsWith("## ")) {
                    ayrinti = satir.strip().equals("## Ayrıntı");
                    continue;
                }
                if (!ayrinti || !satir.startsWith("|") || satir.startsWith("|---") || satir.startsWith("| #")) {
                    continue;
                }
                String[] h = satir.split("\\|", -1);
                String eski = metin(h[4]);
                String yeni = metin(h[5]);
                farklar.add(new Fark(h[1].strip(), eski, yeni, sayfa(h[7]), sayfa(h[8])));
            }
            return farklar;
        }

        static String metin(String hucre) {
            String s = hucre.strip();
            return s.startsWith("`") && s.endsWith("`") ? s.substring(1, s.length() - 1) : s;
        }

        static int sayfa(String hucre) {
            String s = hucre.strip();
            return s.equals("—") ? -1 : Integer.parseInt(s);
        }
    }

    static class Kontroller {
        final List<String> cumleler = new ArrayList<>();
        final List<Satir> satirlar = new ArrayList<>();
        final List<String> gercekBankalar = new ArrayList<>();
        final List<String> kapakNotlari = new ArrayList<>();
        final List<String> degismeyenBolumler = new ArrayList<>();
        final List<Tarife> tarife = new ArrayList<>();
        int enAzSayfa;
        int enCokSayfa;

        record Satir(String surum, List<String> hucreler) {}

        Tarife tarifeGirisi(String satir) {
            Tarife bulunan = null;
            for (Tarife t : tarife) {
                if (satir.startsWith(t.etiket() + " ") && (bulunan == null || t.etiket().length() > bulunan.etiket().length())) {
                    bulunan = t;
                }
            }
            return bulunan;
        }

        static Kontroller oku(Path dosya) throws IOException {
            Kontroller k = new Kontroller();
            String bolum = "";
            Map<String, List<String>> ham = new TreeMap<>();
            for (String satir : Files.readAllLines(dosya, StandardCharsets.UTF_8)) {
                String s = satir.strip();
                if (s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                if (s.startsWith("[") && s.endsWith("]")) {
                    bolum = s.substring(1, s.length() - 1);
                    continue;
                }
                ham.computeIfAbsent(bolum, x -> new ArrayList<>()).add(s);
            }
            k.cumleler.addAll(ham.getOrDefault("CUMLE", List.of()));
            k.gercekBankalar.addAll(ham.getOrDefault("GERCEK_BANKA", List.of()));
            k.kapakNotlari.addAll(ham.getOrDefault("KAPAK", List.of()));
            for (String s : ham.getOrDefault("SATIR", List.of())) {
                int iki = s.indexOf(": ");
                List<String> hucreler = new ArrayList<>();
                for (String h : s.substring(iki + 2).split(" \\| ")) {
                    hucreler.add(h.strip());
                }
                k.satirlar.add(new Satir(s.substring(0, iki).strip(), hucreler));
            }
            for (String s : ham.getOrDefault("DEGISMEYEN_BOLUMLER", List.of())) {
                for (String b : s.split(",")) {
                    k.degismeyenBolumler.add(b.strip());
                }
            }
            for (String s : ham.getOrDefault("TARIFE", List.of())) {
                int ok = s.indexOf(" => ");
                if (ok < 0) {
                    throw new IllegalArgumentException("kontroller.txt [TARIFE] satırında ' => ' yok: " + s);
                }
                String sag = s.substring(ok + 4).strip();
                List<Pattern> kaliplar = new ArrayList<>();
                if (!sag.equals("yalniz-tarife")) {
                    for (String kalip : sag.split(" \\|\\| ")) {
                        kaliplar.add(Pattern.compile(kalip.strip()));
                    }
                }
                k.tarife.add(new Tarife(s.substring(0, ok).strip(), sag.equals("yalniz-tarife"), kaliplar));
            }
            String[] aralik = ham.get("SAYFA_ARALIGI").get(0).split("-");
            k.enAzSayfa = Integer.parseInt(aralik[0].strip());
            k.enCokSayfa = Integer.parseInt(aralik[1].strip());
            return k;
        }
    }
}
