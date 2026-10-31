package com.rasitbank.retrieval.ingestion.chunking;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.ingestion.parsing.PageLine;
import com.rasitbank.retrieval.ingestion.parsing.ParsedSection;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Başlıklı kısımları satır sınırında, hedef boyda ve örtüşmeli parçalara böler; parça kısım sınırını geçmez. Soru
 * satırı taşıyan kısımda her soru-cevap çifti bölünmeden ve örtüşmesiz yerleşir; uzun çiftin her parçası soruyla başlar.
 */
@Component
public class TextChunker {

    private static final String LINE_SEPARATOR = "\n";
    private static final String BULLET = "•";

    private final int targetChunkChars;
    private final int maxChunkChars;
    private final int overlapChars;
    private final int charsPerToken;

    public TextChunker(IngestionProperties properties) {
        this.targetChunkChars = properties.targetChunkChars();
        this.maxChunkChars = properties.maxChunkChars();
        this.overlapChars = properties.overlapChars();
        this.charsPerToken = properties.charsPerToken();
    }

    public List<ChunkDraft> chunk(List<ParsedSection> sections) {
        ChunkAssembler assembler = new ChunkAssembler();
        for (ParsedSection section : sections) {
            boolean hasQuestions = section.lines().stream().anyMatch(PageLine::isQuestion);
            assembler.startSection(!hasQuestions);
            List<List<PageLine>> blocks = hasQuestions ? questionAnswerBlocks(section.lines())
                    : sizedBlocks(section.lines());
            for (List<PageLine> block : blocks) {
                assembler.addBlock(section.heading(), block);
            }
            assembler.closeSection(section.heading());
        }
        return assembler.drafts;
    }

    private List<List<PageLine>> questionAnswerBlocks(List<PageLine> lines) {
        int firstQuestion = 0;
        while (!lines.get(firstQuestion).isQuestion()) {
            firstQuestion++;
        }
        List<List<PageLine>> blocks = new ArrayList<>(sizedBlocks(lines.subList(0, firstQuestion)));
        int pairStart = firstQuestion;
        while (pairStart < lines.size()) {
            int answerStart = pairStart;
            while (answerStart < lines.size() && lines.get(answerStart).isQuestion()) {
                answerStart++;
            }
            int pairEnd = answerStart;
            while (pairEnd < lines.size() && !lines.get(pairEnd).isQuestion()) {
                pairEnd++;
            }
            blocks.addAll(pairBlocks(lines.subList(pairStart, answerStart), lines.subList(answerStart, pairEnd)));
            pairStart = pairEnd;
        }
        return blocks;
    }

    private List<List<PageLine>> pairBlocks(List<PageLine> questionLines, List<PageLine> answerLines) {
        List<PageLine> pair = new ArrayList<>(questionLines);
        pair.addAll(answerLines);
        if (length(pair) <= maxChunkChars) {
            return List.of(pair);
        }
        int prefixLength = length(questionLines) + LINE_SEPARATOR.length();
        if (prefixLength >= targetChunkChars) {
            return splitLongBlock(splitLongLines(pair, maxChunkChars), targetChunkChars);
        }
        List<List<PageLine>> blocks = new ArrayList<>();
        List<PageLine> answerParts = splitLongLines(answerLines, maxChunkChars - prefixLength);
        for (List<PageLine> answerPart : splitLongBlock(answerParts, targetChunkChars - prefixLength)) {
            List<PageLine> block = new ArrayList<>(questionLines);
            block.addAll(answerPart);
            blocks.add(block);
        }
        return blocks;
    }

    private List<List<PageLine>> sizedBlocks(List<PageLine> lines) {
        List<List<PageLine>> sizedBlocks = new ArrayList<>();
        for (List<PageLine> block : blocks(splitLongLines(lines, maxChunkChars))) {
            if (length(block) <= maxChunkChars) {
                sizedBlocks.add(block);
            } else {
                sizedBlocks.addAll(splitLongBlock(block, targetChunkChars));
            }
        }
        return sizedBlocks;
    }

    private static List<PageLine> splitLongLines(List<PageLine> lines, int maxLineChars) {
        List<PageLine> result = new ArrayList<>();
        for (PageLine line : lines) {
            String remaining = line.text();
            while (remaining.length() > maxLineChars) {
                int cut = remaining.lastIndexOf(' ', maxLineChars);
                if (cut <= 0) {
                    cut = maxLineChars;
                }
                result.add(new PageLine(line.pageNumber(), remaining.substring(0, cut).strip(), line.isQuestion()));
                remaining = remaining.substring(cut).strip();
            }
            if (!remaining.isEmpty()) {
                result.add(new PageLine(line.pageNumber(), remaining, line.isQuestion()));
            }
        }
        return result;
    }

    private static List<List<PageLine>> blocks(List<PageLine> lines) {
        List<List<PageLine>> blocks = new ArrayList<>();
        List<PageLine> block = new ArrayList<>();
        for (PageLine line : lines) {
            if (line.text().startsWith(BULLET) && !block.isEmpty()) {
                blocks.add(block);
                block = new ArrayList<>();
            }
            block.add(line);
            if (endsSentence(line.text())) {
                blocks.add(block);
                block = new ArrayList<>();
            }
        }
        if (!block.isEmpty()) {
            blocks.add(block);
        }
        return blocks;
    }

    private static boolean endsSentence(String text) {
        char last = text.charAt(text.length() - 1);
        return last == '.' || last == '!' || last == '?' || last == ':';
    }

    private static List<List<PageLine>> splitLongBlock(List<PageLine> block, int partTargetChars) {
        List<List<PageLine>> parts = new ArrayList<>();
        List<PageLine> part = new ArrayList<>();
        for (PageLine line : block) {
            if (!part.isEmpty() && length(part) + LINE_SEPARATOR.length() + line.text().length() > partTargetChars) {
                parts.add(part);
                part = new ArrayList<>();
            }
            part.add(line);
        }
        if (!part.isEmpty()) {
            parts.add(part);
        }
        return parts;
    }

    private static int length(List<PageLine> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        int length = (lines.size() - 1) * LINE_SEPARATOR.length();
        for (PageLine line : lines) {
            length += line.text().length();
        }
        return length;
    }

    private static String join(List<PageLine> lines) {
        return String.join(LINE_SEPARATOR, lines.stream().map(PageLine::text).toList());
    }

    private final class ChunkAssembler {

        private final List<ChunkDraft> drafts = new ArrayList<>();
        private final List<PageLine> currentLines = new ArrayList<>();
        private int newLineCount;
        private boolean isOverlapEnabled = true;

        void startSection(boolean isOverlapEnabled) {
            this.isOverlapEnabled = isOverlapEnabled;
        }

        void addBlock(String heading, List<PageLine> block) {
            if (newLineCount > 0 && length(currentLines) + LINE_SEPARATOR.length() + length(block) > targetChunkChars) {
                List<PageLine> overlap = isOverlapEnabled ? overlap(currentLines) : List.of();
                emit(heading);
                currentLines.addAll(overlap);
                while (!currentLines.isEmpty()
                        && length(currentLines) + LINE_SEPARATOR.length() + length(block) > maxChunkChars) {
                    currentLines.removeFirst();
                }
            }
            currentLines.addAll(block);
            newLineCount += block.size();
        }

        void closeSection(String heading) {
            if (newLineCount > 0) {
                emit(heading);
            }
            currentLines.clear();
            newLineCount = 0;
        }

        private List<PageLine> overlap(List<PageLine> lines) {
            List<PageLine> overlap = new ArrayList<>();
            int overlapLength = -LINE_SEPARATOR.length();
            for (int index = lines.size() - 1; index >= 0; index--) {
                PageLine line = lines.get(index);
                int lengthWithLine = overlapLength + LINE_SEPARATOR.length() + line.text().length();
                if (lengthWithLine > overlapChars) {
                    break;
                }
                overlap.addFirst(line);
                overlapLength = lengthWithLine;
            }
            return overlap;
        }

        private void emit(String heading) {
            int pageNumber = currentLines.getFirst().pageNumber();
            String content = join(currentLines);
            int inputLength = heading.length() + ChunkDraft.HEADING_SEPARATOR.length() + content.length();
            int tokenCount = Math.ceilDiv(inputLength, charsPerToken);
            drafts.add(new ChunkDraft(drafts.size(), pageNumber, heading, content, tokenCount));
            currentLines.clear();
            newLineCount = 0;
        }
    }
}
