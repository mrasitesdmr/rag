package com.rasitbank.retrieval.ingestion.parsing;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rasitbank.retrieval.exception.UnprocessableContentException;
import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.chunking.TextChunker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.apache.pdfbox.text.PDFTextStripper;
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
    void givenGuide202610_whenParseAndChunk_thenChunkCountAndAverageTokensAsInM0Report() {
        // Act
        List<ChunkDraft> drafts = chunks();

        // Assert
        // 154 parça ve ortalama 283 token: TASK-024 M0 (gerçek yükleme) raporundaki değerler
        assertThat(drafts).hasSize(154);
        assertThat(Math.round(drafts.stream().mapToInt(ChunkDraft::tokenCount).average().orElseThrow()))
                .isEqualTo(283);
        assertThat(drafts.getFirst().pageNumber()).isEqualTo(5);
    }

    @Test
    void givenGuide202610_whenParse_thenEach105TypstQuestionIsAQuestionLineGroup() {
        // Act
        List<String> parsedQuestions = new ArrayList<>();
        for (ParsedSection section : document.sections()) {
            StringBuilder question = null;
            for (PageLine line : section.lines()) {
                if (line.isQuestion()) {
                    question = question == null ? new StringBuilder(line.text())
                            : question.append(' ').append(line.text());
                } else if (question != null) {
                    parsedQuestions.add(squash(question.toString()));
                    question = null;
                }
            }
            if (question != null) {
                parsedQuestions.add(squash(question.toString()));
            }
        }

        // Assert
        List<String> typstQuestions = typstPairs().stream().map(pair -> pair[0]).toList();
        assertThat(typstQuestions).hasSize(105);
        assertThat(document.questionCount()).isEqualTo(105);
        assertThat(parsedQuestions).containsExactlyInAnyOrderElementsOf(typstQuestions);
    }

    @Test
    void givenGuide202610_whenParseAndChunk_thenEveryAnswerSitsInTheChunkOfItsQuestionAndEachQuestionInOneChunk() {
        // Arrange
        // cevap metni bölümün gövdesinde de geçebilir (ör. 2.17); eşleme yalnız soru-cevap kısımlarının parçalarında aranır
        List<String> chunkTexts = chunks().stream().filter(draft -> draft.heading().endsWith(" · Sık sorulanlar"))
                .map(draft -> squash(draft.content())).toList();

        // Act + Assert
        for (String[] pair : typstPairs()) {
            String question = pair[0];
            String answerStart = pair[1].substring(0, 25);
            List<String> withQuestion = chunkTexts.stream().filter(text -> text.contains(question)).toList();
            assertThat(withQuestion).as(question).hasSize(1);
            // soru ile cevabın başı aynı parçada ve bitişik (iki soru aynı cevap başlangıcını paylaşabildiği için
            // eşleme "soru + cevap başı" birleşimi üzerinden yapılır)
            assertThat(withQuestion.getFirst()).as(question).contains(question + " " + answerStart);
        }
    }

    @Test
    void givenGuide202610_whenParseAndChunk_thenDovizKomisyonChunkHasNoOrphanLimitEmriAnswer() {
        // Act
        List<ChunkDraft> withQuestion = chunks().stream()
                .filter(draft -> draft.content().contains("Döviz alırken komisyon alıyor musunuz?")).toList();

        // Assert
        assertThat(withQuestion).hasSize(1);
        ChunkDraft chunk = withQuestion.getFirst();
        assertThat(chunk.content().lines().findFirst().orElseThrow())
                .isEqualTo("Döviz alırken komisyon alıyor musunuz?");
        if (chunk.content().contains("Raşit Mobil'den limit emri verebilirsiniz.")) {
            assertThat(chunk.content()).contains("Dolar belli bir kura gelince otomatik almak istiyorum.");
        }
    }

    @Test
    void givenGuide202610_whenParseAndChunk_thenEveryChunkFirstLineIsOnItsPage() throws IOException {
        // Arrange
        List<String> pageTexts = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(guideBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pageTexts.add(squash(stripper.getText(pdf)));
            }
        }

        // Act
        List<ChunkDraft> drafts = chunks();

        // Assert
        assertThat(drafts).allSatisfy(draft -> {
            String firstLine = squash(draft.content().lines().findFirst().orElseThrow());
            assertThat(pageTexts.get(draft.pageNumber() - 1)).as("chunk %d", draft.chunkNo()).contains(firstLine);
        });
        assertThat(drafts.getFirst().pageNumber()).isEqualTo(5);
    }

    private static List<ChunkDraft> chunks() {
        return new TextChunker(ingestionProperties()).chunk(document.sections());
    }

    private static String squash(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    private static List<String[]> typstPairs() {
        Pattern pair = Pattern.compile("^#soru\\[(.+?)\\]\\[(.+)\\]$");
        List<String[]> pairs = new ArrayList<>();
        try (Stream<Path> files = Files.list(Path.of("../docs/rehber/kaynak/bolumler"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".typ")).sorted().toList()) {
                for (String line : Files.readAllLines(file)) {
                    Matcher matcher = pair.matcher(line);
                    if (matcher.matches()) {
                        pairs.add(new String[] {squash(matcher.group(1)), squash(matcher.group(2))});
                    }
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return pairs;
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
