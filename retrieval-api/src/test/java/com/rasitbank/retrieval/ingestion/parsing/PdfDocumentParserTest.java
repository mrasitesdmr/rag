package com.rasitbank.retrieval.ingestion.parsing;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static com.rasitbank.retrieval.fixture.PdfFixtures.PageSpec;
import static com.rasitbank.retrieval.fixture.PdfFixtures.corruptPdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.encryptedPdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.imageOnlyPdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rasitbank.retrieval.exception.UnprocessableContentException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * PDF okuma kurallarını (yer imli başlıklar, kapak, SSS, üst/alt bilgi, hata kodları) üretilmiş PDF'lerle doğrular.
 */
class PdfDocumentParserTest {

    private static final String HEADER = "Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm 2026.10";

    private final PdfDocumentParser parser = new PdfDocumentParser(new PageMarginFilter(ingestionProperties()));

    private static PageSpec page(int number, String bookmark, String... body) {
        String[] lines = new String[body.length + 2];
        lines[0] = HEADER;
        System.arraycopy(body, 0, lines, 1, body.length);
        lines[lines.length - 1] = "Yürürlük: 1 Ekim 2026 · Sayfa " + number + " / 4";
        return PageSpec.of(bookmark, lines);
    }

    private static byte[] guideLikePdf() {
        return pdf(true,
                page(1, null, "Kapak sayfası"),
                page(2, null, "İçindekiler", "2 Kartlar 3", "2.3 Ek kart 3"),
                page(3, "2 Kartlar|>2.3 Ek kart", "2 Kartlar", "2.3 Ek kart", "Ek kart gövde satırı bir.",
                        "1 Kasım 2026 tarihinden itibaren geçerlidir."),
                page(4, "5 Havale|>5.1 Havale|>5.2 EFT", "5 Havale", "5.1 Havale", "Havale gövde.",
                        "1. Kartınızı aktif edin", "5.2 EFT", "EFT gövde.", "Sık sorulanlar", "SSS satırı."));
    }

    private static List<String> lineTexts(ParsedDocument document) {
        return document.sections().stream().flatMap(section -> section.lines().stream()).map(PageLine::text).toList();
    }

    @Test
    void givenUnnumberedBookmarks_whenParse_thenSectionsFollowBookmarkTitles() {
        // Arrange
        byte[] bytes = pdf(true,
                PageSpec.of("Giriş", "Giriş", "Giriş metni."),
                PageSpec.of("Teslimat|>Gecikmeler", "Teslimat", "Teslimat metni.", "Gecikmeler", "Gecikme metni."));

        // Act
        ParsedDocument document = parser.parse(bytes);

        // Assert
        assertThat(document.sections()).extracting(ParsedSection::heading)
                .containsExactly("Giriş", "Teslimat", "Gecikmeler");
        assertThat(document.sections()).extracting(section -> section.lines().stream().map(PageLine::text).toList())
                .containsExactly(List.of("Giriş metni."), List.of("Teslimat metni."), List.of("Gecikme metni."));
        assertThat(document.sections().get(2).lines().getFirst().pageNumber()).isEqualTo(2);
        assertThat(document.chapterCount()).isEqualTo(2);
        assertThat(document.pageCount()).isEqualTo(2);
    }

    @Test
    void givenBookmarkTitleNotPrintedOnItsPage_whenParse_thenSectionStartsAtTopOfThatPage() {
        // Arrange
        byte[] bytes = pdf(true,
                PageSpec.of("Bölüm A", "Bölüm A", "a satırı"),
                PageSpec.of("Bölüm B", "ilk satır", "ikinci satır"));

        // Act
        ParsedDocument document = parser.parse(bytes);

        // Assert
        assertThat(document.sections()).extracting(ParsedSection::heading).containsExactly("Bölüm A", "Bölüm B");
        assertThat(document.sections().get(0).lines()).extracting(PageLine::text).containsExactly("a satırı");
        assertThat(document.sections().get(1).lines()).extracting(PageLine::text)
                .containsExactly("ilk satır", "ikinci satır");
    }

    @Test
    void givenCoverAndTocBeforeFirstBookmark_whenParse_thenThosePagesAreSkipped() {
        // Act
        ParsedDocument document = parser.parse(guideLikePdf());

        // Assert
        List<PageLine> lines = document.sections().stream().flatMap(section -> section.lines().stream()).toList();
        assertThat(lines).extracting(PageLine::text).doesNotContain("Kapak sayfası", "İçindekiler", "2.3 Ek kart 3");
        assertThat(lines.getFirst().pageNumber()).isEqualTo(3);
        assertThat(document.pageCount()).isEqualTo(4);
    }

    @Test
    void givenPagesWithHeaderAndFooter_whenParse_thenHeaderAndFooterLinesAreDroppedAndCounted() {
        // Act
        ParsedDocument document = parser.parse(guideLikePdf());

        // Assert
        assertThat(lineTexts(document)).noneMatch(text -> text.startsWith("Raşit Bankası Ürün")
                || text.startsWith("Yürürlük:"));
        assertThat(lineTexts(document)).contains("Ek kart gövde satırı bir.");
        assertThat(document.removedMarginLines()).isEqualTo(8);
    }

    @Test
    void givenBodyLinesStartingWithNumbers_whenParse_thenOnlyBookmarkedLinesBecomeHeadings() {
        // Act
        ParsedDocument document = parser.parse(guideLikePdf());

        // Assert
        assertThat(document.sections()).extracting(ParsedSection::heading)
                .containsExactly("2.3 Ek kart", "5.1 Havale", "5.2 EFT", "5 Havale · Sık sorulanlar");
        assertThat(document.sections().get(0).lines()).extracting(PageLine::text)
                .containsExactly("Ek kart gövde satırı bir.", "1 Kasım 2026 tarihinden itibaren geçerlidir.");
        assertThat(document.sections().get(1).lines()).extracting(PageLine::text)
                .containsExactly("Havale gövde.", "1. Kartınızı aktif edin");
    }

    @Test
    void givenFaqLineAndChapterWithoutSubsections_whenParse_thenHeadingsAreChapterDotFaqAndChapterTitle() {
        // Arrange
        byte[] bytes = pdf(true,
                PageSpec.of("2 Kartlar|>2.1 Kart türleri", "2 Kartlar", "Bölüm giriş satırı.", "2.1 Kart türleri",
                        "Tür satırı.", "Sık sorulanlar", "Soru bir?"),
                PageSpec.of("11 Terimler sözlüğü", "11 Terimler sözlüğü", "Terim satırı."));

        // Act
        ParsedDocument document = parser.parse(bytes);

        // Assert
        assertThat(document.sections()).extracting(ParsedSection::heading).containsExactly("2 Kartlar",
                "2.1 Kart türleri", "2 Kartlar · Sık sorulanlar", "11 Terimler sözlüğü");
        assertThat(document.sections().get(0).lines()).extracting(PageLine::text)
                .containsExactly("Bölüm giriş satırı.");
        assertThat(document.sections().get(2).lines()).extracting(PageLine::text).containsExactly("Soru bir?");
        assertThat(document.chapterCount()).isEqualTo(2);
    }

    @Test
    void givenFaqLineBeforeAnyChapter_whenParse_thenItIsNotTreatedAsFaqHeading() {
        // Arrange
        byte[] bytes = pdf(true,
                PageSpec.of(null, "Sık sorulanlar", "Kapakta rastlanan satır."),
                PageSpec.of("1 Giriş", "1 Giriş", "Giriş satırı."));

        // Act
        ParsedDocument document = parser.parse(bytes);

        // Assert
        assertThat(document.sections()).extracting(ParsedSection::heading).containsExactly("1 Giriş");
        assertThat(document.sections().getFirst().lines()).extracting(PageLine::text).containsExactly("Giriş satırı.");
    }

    @Test
    void givenBookmarkTitleOf250Chars_whenParse_thenHeadingCutAtWhitespaceBefore200WithEllipsis() {
        // Arrange
        String title = "kelime ".repeat(35) + "kelim";
        byte[] bytes = pdf(true, PageSpec.of(title, "metin satırı"));

        // Act
        ParsedDocument document = parser.parse(bytes);

        // Assert
        String expected = String.join(" ", Collections.nCopies(28, "kelime")) + "…";
        assertThat(title).hasSize(250);
        assertThat(document.sections().getFirst().heading()).isEqualTo(expected).hasSize(196);
    }

    @Test
    void givenPdfWithOnlyImagePages_whenParse_thenThrowsPdfNoText() {
        // Act
        Throwable withBookmark = catchThrowable(() -> parser.parse(imageOnlyPdf(true)));
        Throwable withoutBookmark = catchThrowable(() -> parser.parse(imageOnlyPdf(false)));

        // Assert
        assertThat(withBookmark).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_TEXT");
        assertThat(withoutBookmark).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_TEXT");
    }

    @Test
    void givenPdfWithTextButNoBookmarks_whenParse_thenThrowsPdfNoBookmarks() {
        // Arrange
        byte[] bytes = pdf(false, PageSpec.of(null, "BÜYÜK BAŞLIK", "2 Kartlar", "gövde."));

        // Act
        Throwable thrown = catchThrowable(() -> parser.parse(bytes));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_BOOKMARKS");
    }

    @Test
    void givenOnlyBookmarkWithBlankTitle_whenParse_thenThrowsPdfNoBookmarks() {
        // Arrange
        byte[] bytes = pdf(true, PageSpec.of(" ", "gövde satırı."));

        // Act
        Throwable thrown = catchThrowable(() -> parser.parse(bytes));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_BOOKMARKS");
    }

    @Test
    void givenBookmarkWhoseOnlyLineIsItsOwnTitle_whenParse_thenThrowsPdfNoBookmarks() {
        // Arrange
        byte[] bytes = pdf(true, PageSpec.of("Sadece başlık", "Sadece başlık"));

        // Act
        Throwable thrown = catchThrowable(() -> parser.parse(bytes));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_BOOKMARKS");
    }

    @Test
    void givenBytesThatAreNotAPdf_whenParse_thenThrowsPdfUnreadable() {
        // Act
        Throwable plainText = catchThrowable(() -> parser.parse("bu bir pdf degil".getBytes()));
        Throwable corrupt = catchThrowable(() -> parser.parse(corruptPdf()));

        // Assert
        assertThat(plainText).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_UNREADABLE");
        assertThat(corrupt).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_UNREADABLE");
    }

    @Test
    void givenEncryptedPdf_whenParse_thenThrowsPdfUnreadable() {
        // Act
        Throwable thrown = catchThrowable(() -> parser.parse(encryptedPdf()));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_UNREADABLE");
    }
}
