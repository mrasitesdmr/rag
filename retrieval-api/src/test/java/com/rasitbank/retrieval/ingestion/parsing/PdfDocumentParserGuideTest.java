package com.rasitbank.retrieval.ingestion.parsing;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.chunking.TextChunker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Gerçek rehber 2026.10 PDF'inin başlık, sayfa ve parça kurallarına uygun okunduğunu doğrular.
 */
class PdfDocumentParserGuideTest {

    private static final Path GUIDE = Path.of("../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf");

    private static ParsedDocument document;

    @BeforeAll
    static void parseGuide() throws IOException {
        document = new PdfDocumentParser(ingestionProperties()).parse(Files.readAllBytes(GUIDE));
    }

    @Test
    void givenGuide202610_whenParse_thenSeventyPagesElevenChaptersAndEveryTocSubsectionIsASection() {
        List<String> headings = document.sections().stream().map(ParsedSection::heading).toList();

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
        Pattern tocLine = Pattern.compile("^\\d+\\.\\d+ .+ \\d+$");
        List<String> texts = document.sections().stream().flatMap(section -> section.lines().stream())
                .map(PageLine::text).toList();

        assertThat(texts).doesNotContain("İçindekiler");
        assertThat(texts).noneMatch(text -> text.matches(".*Sayfa \\d+ / 70.*"));
        assertThat(texts).noneMatch(text -> text.startsWith("Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm"));
        assertThat(texts).noneMatch(text -> {
            Matcher matcher = tocLine.matcher(text);
            return matcher.matches() && text.startsWith("2.16 ");
        });
    }

    @Test
    void givenGuide202610_whenChunk_thenEveryChunkAtMost2000AndRaşitGoldRowIsOneLineInAChunk() {
        List<ChunkDraft> drafts = new TextChunker(ingestionProperties()).chunk(document.sections());

        assertThat(drafts).allSatisfy(draft -> assertThat(draft.content().length()).isLessThanOrEqualTo(2000));
        assertThat(drafts).anySatisfy(draft -> assertThat(draft.content().lines())
                .anyMatch(line -> line.startsWith("Raşit Gold") && line.contains("TL")));
    }

    private static PageLine firstLine(String heading) {
        return document.sections().stream().filter(section -> section.heading().equals(heading)).findFirst()
                .orElseThrow().lines().getFirst();
    }
}
