package com.rasitbank.retrieval.ingestion.parsing;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rasitbank.retrieval.exception.UnprocessableContentException;
import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.chunking.TextChunker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Gerçek rehber 2026.10 PDF'inin genel okuyucuyla başlık, sayfa ve parça kurallarına uygun okunduğunu doğrular.
 */
class PdfDocumentParserGuideTest {

    private static final Path GUIDE = Path.of("../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf");

    private static byte[] guideBytes;
    private static ParsedDocument document;

    private static PdfDocumentParser parser() {
        return new PdfDocumentParser(new PageMarginFilter(ingestionProperties()));
    }

    @BeforeAll
    static void parseGuide() throws IOException {
        guideBytes = Files.readAllBytes(GUIDE);
        document = parser().parse(guideBytes);
    }

    @Test
    void givenGuide202610_whenParse_thenSeventyPagesElevenChaptersAndEveryTocSubsectionIsASection() {
        // Act
        List<String> headings = document.sections().stream().map(ParsedSection::heading).toList();

        // Assert
        assertThat(document.pageCount()).isEqualTo(70);
        assertThat(document.chapterCount()).isEqualTo(11);
        assertThat(headings.stream().filter(heading -> heading.endsWith(" · Sık sorulanlar"))).hasSize(8);
        // elle sayılmış: TASK-004 rehberi 99 numaralı alt başlık (x.y) içerir
        assertThat(headings.stream().filter(heading -> heading.matches("^\\d+\\.\\d+ .+"))).hasSize(99);
        assertThat(headings).contains("10 Ücret tarifesi", "11 Terimler sözlüğü",
                "2.16 Kayıp ya da çalıntı bildirimi", "4.10 Erken ödeme ve erken kapama tazminatı",
                "2 Kartlar · Sık sorulanlar");
        assertThat(headings).doesNotHaveDuplicates();
    }

    @Test
    void givenGuide202610_whenParse_thenKnownSectionsStartOnTheirBookmarkPages() {
        assertThat(firstLine("2.16 Kayıp ya da çalıntı bildirimi").pageNumber()).isEqualTo(16);
        assertThat(firstLine("4.10 Erken ödeme ve erken kapama tazminatı").pageNumber()).isEqualTo(35);
    }

    @Test
    void givenGuide202610_whenParse_thenNoTocOrPageFooterLinesRemain() {
        // Arrange
        Pattern tocLine = Pattern.compile("^\\d+\\.\\d+ .+ \\d+$");

        // Act
        List<String> texts = document.sections().stream().flatMap(section -> section.lines().stream())
                .map(PageLine::text).toList();

        // Assert
        assertThat(texts).doesNotContain("İçindekiler");
        assertThat(texts).noneMatch(text -> text.matches(".*Sayfa \\d+ / 70.*"));
        assertThat(texts).noneMatch(text -> text.startsWith("Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm"));
        assertThat(texts).noneMatch(text -> {
            Matcher matcher = tocLine.matcher(text);
            return matcher.matches() && text.startsWith("2.16 ");
        });
        assertThat(document.removedMarginLines()).isPositive();
    }

    @Test
    void givenGuide202610_whenParseAndChunk_then154ChunksWithAverage285Tokens() {
        // Act
        List<ChunkDraft> drafts = new TextChunker(ingestionProperties()).chunk(document.sections());

        // Assert
        assertThat(drafts).hasSize(154);
        assertThat(Math.round(drafts.stream().mapToInt(ChunkDraft::tokenCount).average().orElseThrow()))
                .isEqualTo(285);
        assertThat(drafts.getFirst().pageNumber()).isEqualTo(5);
    }

    @Test
    void givenGuide202610_whenChunk_thenEveryChunkAtMost2000AndRasitGoldRowIsOneLineInAChunk() {
        // Act
        List<ChunkDraft> drafts = new TextChunker(ingestionProperties()).chunk(document.sections());

        // Assert
        assertThat(drafts).allSatisfy(draft -> assertThat(draft.content().length()).isLessThanOrEqualTo(2000));
        assertThat(drafts).anySatisfy(draft -> assertThat(draft.content().lines())
                .anyMatch(line -> line.startsWith("Raşit Gold") && line.contains("TL")));
    }

    @Test
    void givenGuideWithBookmarksRemoved_whenParse_thenThrowsPdfNoBookmarks() throws IOException {
        // Arrange
        byte[] withoutBookmarks;
        try (PDDocument pdf = Loader.loadPDF(guideBytes); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pdf.getDocumentCatalog().setDocumentOutline(null);
            pdf.save(out);
            withoutBookmarks = out.toByteArray();
        }

        // Act
        Throwable thrown = catchThrowable(() -> parser().parse(withoutBookmarks));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_BOOKMARKS");
    }

    private static PageLine firstLine(String heading) {
        return document.sections().stream().filter(section -> section.heading().equals(heading)).findFirst()
                .orElseThrow().lines().getFirst();
    }
}
