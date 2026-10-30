package com.rasitbank.retrieval.ingestion.parsing;

import static com.rasitbank.retrieval.ingestion.parsing.PdfFixtures.PageSpec;
import static com.rasitbank.retrieval.ingestion.parsing.PdfFixtures.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.fixture.IngestionFixtures;
import com.rasitbank.retrieval.exception.UnprocessableContentException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * PDF okuma kurallarını (üst/alt bilgi, kapak, yer imli başlıklar, SSS, hata kodları) üretilmiş PDF'lerle doğrular.
 */
class PdfDocumentParserTest {

    private static final String HEADER = "Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm 2026.10";

    private final PdfDocumentParser parser = new PdfDocumentParser(new IngestionProperties(1600, 2000, 200, 4, 96,
            IngestionFixtures.HEADER_PATTERN, IngestionFixtures.FOOTER_PATTERN,
            new IngestionProperties.Bootstrap(false, Path.of("x.pdf"), "ad", "1")));

    private static PageSpec page(int number, String bookmark, String... body) {
        String[] lines = new String[body.length + 2];
        lines[0] = HEADER;
        System.arraycopy(body, 0, lines, 1, body.length);
        lines[lines.length - 1] = "Yürürlük: 1 Ekim 2026 · Sayfa " + number + " / 4";
        return PageSpec.of(bookmark, lines);
    }

    private byte[] guideLikePdf() {
        return pdf(true,
                page(1, null, "Kapak sayfası"),
                page(2, null, "İçindekiler", "2 Kartlar 3", "2.3 Ek kart 3"),
                page(3, "2 Kartlar|2.3 Ek kart", "2 Kartlar", "2.3 Ek kart", "Ek kart gövde satırı bir.",
                        "1 Kasım 2026 tarihinden itibaren geçerlidir."),
                page(4, "5 Havale|5.1 Havale|5.2 EFT", "5 Havale", "5.1 Havale", "Havale gövde.", "1. Kartınızı aktif edin",
                        "5.2 EFT", "EFT gövde.", "Sık sorulanlar", "SSS satırı."));
    }

    @Test
    void givenPagesWithHeaderAndFooter_whenParse_thenHeaderAndFooterLinesAreDropped() {
        ParsedDocument document = parser.parse(guideLikePdf());

        List<String> texts = document.sections().stream().flatMap(section -> section.lines().stream())
                .map(PageLine::text).toList();
        assertThat(texts).noneMatch(text -> text.startsWith("Raşit Bankası Ürün") || text.startsWith("Yürürlük:"));
        assertThat(texts).contains("Ek kart gövde satırı bir.");
    }

    @Test
    void givenCoverAndTocBeforeFirstChapterBookmark_whenParse_thenThosePagesAreSkipped() {
        ParsedDocument document = parser.parse(guideLikePdf());

        List<PageLine> lines = document.sections().stream().flatMap(section -> section.lines().stream()).toList();
        assertThat(lines).extracting(PageLine::text).doesNotContain("Kapak sayfası", "İçindekiler", "2.3 Ek kart 3");
        assertThat(lines.getFirst().pageNumber()).isEqualTo(3);
        assertThat(document.pageCount()).isEqualTo(4);
    }

    @Test
    void givenBodyLinesStartingWithNumbers_whenParse_thenOnlyBookmarkedLinesBecomeHeadings() {
        ParsedDocument document = parser.parse(guideLikePdf());

        assertThat(document.sections()).extracting(ParsedSection::heading)
                .containsExactly("2.3 Ek kart", "5.1 Havale", "5.2 EFT", "5 Havale · Sık sorulanlar");
        ParsedSection ekKart = document.sections().getFirst();
        assertThat(ekKart.lines()).extracting(PageLine::text).containsExactly("Ek kart gövde satırı bir.",
                "1 Kasım 2026 tarihinden itibaren geçerlidir.");
        ParsedSection havale = document.sections().get(1);
        assertThat(havale.lines()).extracting(PageLine::text).containsExactly("Havale gövde.",
                "1. Kartınızı aktif edin");
    }

    @Test
    void givenFaqLineAndChapterWithoutSubsections_whenParse_thenHeadingsAreChapterDotFaqAndChapterTitle() {
        byte[] bytes = pdf(true,
                page(1, "2 Kartlar|2.1 Kart türleri", "2 Kartlar", "Bölüm giriş satırı.", "2.1 Kart türleri",
                        "Tür satırı.", "Sık sorulanlar", "Soru bir?"),
                page(2, "11 Terimler sözlüğü", "11 Terimler sözlüğü", "Terim satırı."));

        ParsedDocument document = parser.parse(bytes);

        assertThat(document.sections()).extracting(ParsedSection::heading).containsExactly("2 Kartlar",
                "2.1 Kart türleri", "2 Kartlar · Sık sorulanlar", "11 Terimler sözlüğü");
        assertThat(document.sections().getFirst().lines()).extracting(PageLine::text)
                .containsExactly("Bölüm giriş satırı.");
        assertThat(document.sections().get(2).lines()).extracting(PageLine::text).containsExactly("Soru bir?");
        assertThat(document.chapterCount()).isEqualTo(2);
    }

    @Test
    void givenBytesThatAreNotAPdf_whenParse_thenThrowsPdfUnreadable() {
        assertThatThrownBy(() -> parser.parse("bu bir pdf degil".getBytes()))
                .isInstanceOfSatisfying(UnprocessableContentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PDF_UNREADABLE"));
    }

    @Test
    void givenPdfWithoutBookmarks_whenParse_thenThrowsPdfNoSections() {
        byte[] bytes = pdf(false, page(1, null, "2 Kartlar", "gövde."));

        assertThatThrownBy(() -> parser.parse(bytes)).isInstanceOfSatisfying(UnprocessableContentException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("PDF_NO_SECTIONS"));
    }

    @Test
    void givenFaqLineBeforeAnyChapter_whenParse_thenItIsNotTreatedAsFaqHeading() {
        byte[] bytes = pdf(true,
                page(1, null, "Sık sorulanlar", "Kapakta rastlanan satır."),
                page(2, "1 Giriş", "1 Giriş", "Giriş satırı."));

        ParsedDocument document = parser.parse(bytes);

        assertThat(document.sections()).extracting(ParsedSection::heading).containsExactly("1 Giriş");
        assertThat(document.sections().getFirst().lines()).extracting(PageLine::text).containsExactly("Giriş satırı.");
    }
}
