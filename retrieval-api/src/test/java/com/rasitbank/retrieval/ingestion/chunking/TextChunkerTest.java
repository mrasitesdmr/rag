package com.rasitbank.retrieval.ingestion.chunking;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static com.rasitbank.retrieval.fixture.IngestionFixtures.line;
import static com.rasitbank.retrieval.fixture.IngestionFixtures.section;
import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.ingestion.parsing.PageLine;
import com.rasitbank.retrieval.ingestion.parsing.ParsedSection;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Parçalama kurallarını (1.600 hedef, 2.000 üst sınır, 200 örtüşme, kısım sınırı, satır bütünlüğü) doğrular.
 */
class TextChunkerTest {

    private final TextChunker chunker = new TextChunker(ingestionProperties());

    @Test
    void givenSectionOf40SentenceLines_whenChunk_thenEveryChunkAtMost2000AndMultiBlockChunksAtMost1600() {
        ParsedSection section = section("2.3 Ek kart", 7, 40, 100, '.');

        List<ChunkDraft> drafts = chunker.chunk(List.of(section));

        assertThat(drafts.size()).isGreaterThanOrEqualTo(3);
        assertThat(drafts).allSatisfy(draft -> {
            assertThat(draft.content().length()).isLessThanOrEqualTo(2000);
            assertThat(draft.content().length()).isLessThanOrEqualTo(1600);
        });
        // 4.039 karakterlik içerik 1.600 hedefiyle en az 3 parçaya iner; ilk parça hedefe yakındır
        assertThat(drafts.getFirst().content().length()).isBetween(1400, 1600);
    }

    @Test
    void givenConsecutiveChunksInOneSection_whenChunk_thenNextChunkStartsWithLastLinesOfPreviousUpTo200Chars() {
        List<ChunkDraft> drafts = chunker.chunk(List.of(section("2.3 Ek kart", 7, 40, 100, '.')));

        for (int index = 0; index + 1 < drafts.size(); index++) {
            List<String> previous = drafts.get(index).content().lines().toList();
            List<String> next = drafts.get(index + 1).content().lines().toList();
            int overlapLines = 0;
            while (overlapLines < next.size() && previous.contains(next.get(overlapLines))) {
                overlapLines++;
            }
            assertThat(overlapLines).isEqualTo(1);
            assertThat(next.getFirst()).isEqualTo(previous.getLast());
            assertThat(next.getFirst().length()).isLessThanOrEqualTo(200);
        }
    }

    @Test
    void givenTableRowsWithoutSentenceEnd_whenChunk_thenEveryRowAppearsWholeInAChunk() {
        List<PageLine> rows = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            rows.add(new PageLine(12, "Raşit Gold " + index + " 1.450 TL Yıllık 150.000 TL harcama Harcamanın binde 5'i"
                    + " ek açıklama ".repeat(20) + index));
        }

        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("2.2 Kart ücretleri", rows)));

        assertThat(drafts.size()).isGreaterThan(1);
        List<String> allChunkLines = drafts.stream().flatMap(draft -> draft.content().lines()).toList();
        assertThat(allChunkLines).containsAll(rows.stream().map(PageLine::text).toList());
        List<String> rowTexts = rows.stream().map(PageLine::text).toList();
        assertThat(drafts).allSatisfy(draft -> assertThat(rowTexts).containsAll(draft.content().lines().toList()));
    }

    @Test
    void givenTwoSections_whenChunk_thenNoChunkSpansSectionsAndEachCarriesItsHeading() {
        ParsedSection first = new ParsedSection("2.3 Ek kart", List.of(new PageLine(7, "A1 birinci kısım."),
                new PageLine(7, "A2 birinci kısım.")));
        ParsedSection second = new ParsedSection("2.4 Yıllık üyelik ücreti, muafiyet ve iade",
                List.of(new PageLine(8, "B1 ikinci kısım."), new PageLine(8, "B2 ikinci kısım.")));

        List<ChunkDraft> drafts = chunker.chunk(List.of(first, second));

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).heading()).isEqualTo("2.3 Ek kart");
        assertThat(drafts.get(0).content()).isEqualTo("A1 birinci kısım.\nA2 birinci kısım.");
        assertThat(drafts.get(1).heading()).isEqualTo("2.4 Yıllık üyelik ücreti, muafiyet ve iade");
        assertThat(drafts.get(1).content()).isEqualTo("B1 ikinci kısım.\nB2 ikinci kısım.");
    }

    @Test
    void givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstNewLine() {
        List<PageLine> lines = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            lines.add(new PageLine(index < 15 ? 7 : 8, line(index, 100, '.')));
        }

        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("2.3 Ek kart", lines)));

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).pageNumber()).isEqualTo(7);
        assertThat(drafts.get(1).content().lines().findFirst().orElseThrow()).isEqualTo(lines.get(14).text());
        assertThat(drafts.get(1).pageNumber()).isEqualTo(8);
    }

    @Test
    void givenHeadingAndContent_whenChunk_thenTokenCountIsCeilOfEmbeddingInputOver4() {
        String content = "a".repeat(84) + ".";

        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("2.3 Ek kart",
                List.of(new PageLine(7, content)))));

        // 11 + 2 + 85 = 98 karakter -> ceil(98 / 4) = 25
        assertThat(drafts.getFirst().tokenCount()).isEqualTo(25);
    }

    @Test
    void givenLineOf4500CharsWithSpaces_whenChunk_thenSplitAtWhitespaceIntoPiecesAtMost2000() {
        String word = "kelime ";
        String longLine = word.repeat(4500 / word.length()).strip();

        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("1 Giriş",
                List.of(new PageLine(5, longLine)))));

        assertThat(drafts.size()).isGreaterThanOrEqualTo(3);
        assertThat(drafts).allSatisfy(draft -> assertThat(draft.content().length()).isLessThanOrEqualTo(2000));
        String joined = String.join(" ", drafts.stream().map(ChunkDraft::content).toList());
        assertThat(joined.replaceAll("\\s+", "")).startsWith("kelimekelime");
        assertThat(drafts).allSatisfy(draft -> assertThat(draft.content()).doesNotContain("kelimekelime")
                .doesNotStartWith("elime").doesNotEndWith("kelim"));
    }

    @Test
    void givenThreeSections_whenChunk_thenChunkNumbersRunFrom0WithoutGaps() {
        List<ParsedSection> sections = List.of(section("1 A", 5, 30, 100, '.'), section("2 B", 6, 5, 100, '.'),
                section("3 C", 7, 30, 100, '.'));

        List<ChunkDraft> drafts = chunker.chunk(sections);

        assertThat(drafts).extracting(ChunkDraft::chunkNo)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, drafts.size()).boxed().toList());
        assertThat(drafts.size()).isGreaterThan(3);
    }

    @Test
    void givenTwoSections_whenChunk_thenSecondSectionFirstChunkStartsWithoutOverlap() {
        List<ChunkDraft> drafts = chunker.chunk(List.of(section("1 A", 5, 30, 100, '.'),
                new ParsedSection("2 B", List.of(new PageLine(9, "yeni kısım ilk satır.")))));

        assertThat(drafts.getLast().heading()).isEqualTo("2 B");
        assertThat(drafts.getLast().content()).isEqualTo("yeni kısım ilk satır.");
    }

    private static List<PageLine> linesWithMarkerAt(int markerIndex, char markerEnd, String markerPrefix) {
        List<PageLine> lines = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            String text = line(index, 100, 'x');
            if (index == markerIndex) {
                text = markerPrefix + text.substring(markerPrefix.length(), 99) + markerEnd;
            }
            lines.add(new PageLine(5, text));
        }
        return lines;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(chars = {'.', '!', '?', ':'})
    void givenLineEndingWithSentenceMark_whenChunk_thenFirstChunkEndsExactlyAtThatLine(char mark) {
        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("1 A", linesWithMarkerAt(9, mark, ""))));

        assertThat(drafts.getFirst().content().lines().count()).isEqualTo(10);
    }

    @Test
    void givenLineEndingWithComma_whenChunk_thenItDoesNotCloseABlock() {
        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("1 A", linesWithMarkerAt(9, ',', ""))));

        assertThat(drafts.getFirst().content().lines().count()).isNotEqualTo(10);
    }

    @Test
    void givenBulletLine_whenChunk_thenBulletStartsANewBlockSoFirstChunkEndsBeforeIt() {
        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("1 A", linesWithMarkerAt(10, 'x', "• "))));

        assertThat(drafts.getFirst().content().lines().count()).isEqualTo(10);
        assertThat(drafts.get(1).content()).startsWith("L009");
    }

    @Test
    void givenTwoBlocksTotalling1600_whenChunk_thenOneChunkButAt1601TwoChunks() {
        String first = "a".repeat(798) + ".";
        List<ChunkDraft> exact = chunker.chunk(List.of(new ParsedSection("1 A",
                List.of(new PageLine(5, first), new PageLine(5, "b".repeat(799) + ".")))));
        List<ChunkDraft> over = chunker.chunk(List.of(new ParsedSection("1 A",
                List.of(new PageLine(5, first), new PageLine(5, "b".repeat(800) + ".")))));

        assertThat(exact).hasSize(1);
        assertThat(exact.getFirst().content().length()).isEqualTo(1600);
        assertThat(over).hasSize(2);
    }

    @Test
    void givenLineOf2000And2001Chars_whenChunk_thenOnlyThe2001LineIsSplit() {
        String at2000 = "w".repeat(9) + (" " + "w".repeat(9)).repeat(198) + " " + "w".repeat(10);
        String at2001 = at2000 + "w";
        assertThat(at2000).hasSize(2000);

        List<ChunkDraft> exact = chunker.chunk(List.of(new ParsedSection("1 A", List.of(new PageLine(5, at2000)))));
        List<ChunkDraft> over = chunker.chunk(List.of(new ParsedSection("1 A", List.of(new PageLine(5, at2001)))));

        assertThat(exact).hasSize(1);
        assertThat(exact.getFirst().content()).isEqualTo(at2000);
        assertThat(over.size()).isGreaterThanOrEqualTo(2);
        assertThat(over).allSatisfy(draft -> assertThat(draft.content().length()).isLessThanOrEqualTo(2000));
    }

    @Test
    void givenOverlapCandidatesTotalling200Chars_whenChunk_thenBothLinesAreCarriedOver() {
        List<PageLine> lines = List.of(new PageLine(5, "a".repeat(1397) + "."), new PageLine(5, "b".repeat(99) + "."),
                new PageLine(5, "c".repeat(98) + "."), new PageLine(5, "d".repeat(299) + "."));

        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("1 A", lines)));

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).content().length()).isEqualTo(1599);
        assertThat(drafts.get(1).content().lines().map(l -> l.charAt(0)).toList()).containsExactly('b', 'c', 'd');
    }

    @Test
    void givenOverlapPlusBlockAboveMax_whenChunk_thenOverlapIsTrimmedFromTheStartAndChunkStaysAtMost2000() {
        List<PageLine> lines = List.of(new PageLine(5, "a".repeat(1397) + "."), new PageLine(5, "b".repeat(99) + "."),
                new PageLine(5, "c".repeat(98) + "."), new PageLine(5, "d".repeat(1899) + "."));

        List<ChunkDraft> drafts = chunker.chunk(List.of(new ParsedSection("1 A", lines)));

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(1).content().length()).isEqualTo(2000);
        assertThat(drafts.get(1).content().lines().map(l -> l.charAt(0)).toList()).containsExactly('c', 'd');
    }
}
