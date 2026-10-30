package com.rasitbank.retrieval.ingestion.chunking;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.ingestion.parsing.PageLine;
import com.rasitbank.retrieval.ingestion.parsing.ParsedSection;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Başlıklı kısımları satır sınırında, hedef boyda ve örtüşmeli parçalara böler; parça kısım sınırını geçmez.
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
            for (List<PageLine> block : sizedBlocks(section.lines())) {
                assembler.addBlock(section.heading(), block);
            }
            assembler.closeSection(section.heading());
        }
        return assembler.drafts;
    }

    private List<List<PageLine>> sizedBlocks(List<PageLine> lines) {
        List<List<PageLine>> sizedBlocks = new ArrayList<>();
        for (List<PageLine> block : blocks(splitLongLines(lines))) {
            if (length(block) <= maxChunkChars) {
                sizedBlocks.add(block);
            } else {
                sizedBlocks.addAll(splitLongBlock(block));
            }
        }
        return sizedBlocks;
    }

    private List<PageLine> splitLongLines(List<PageLine> lines) {
        List<PageLine> result = new ArrayList<>();
        for (PageLine line : lines) {
            String remaining = line.text();
            while (remaining.length() > maxChunkChars) {
                int cut = remaining.lastIndexOf(' ', maxChunkChars);
                if (cut <= 0) {
                    cut = maxChunkChars;
                }
                result.add(new PageLine(line.pageNumber(), remaining.substring(0, cut).strip()));
                remaining = remaining.substring(cut).strip();
            }
            if (!remaining.isEmpty()) {
                result.add(new PageLine(line.pageNumber(), remaining));
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

    private List<List<PageLine>> splitLongBlock(List<PageLine> block) {
        List<List<PageLine>> parts = new ArrayList<>();
        List<PageLine> part = new ArrayList<>();
        for (PageLine line : block) {
            if (!part.isEmpty() && length(part) + LINE_SEPARATOR.length() + line.text().length() > targetChunkChars) {
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

        void addBlock(String heading, List<PageLine> block) {
            if (newLineCount > 0 && length(currentLines) + LINE_SEPARATOR.length() + length(block) > targetChunkChars) {
                List<PageLine> overlap = overlap(currentLines);
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
            PageLine firstNewLine = currentLines.get(currentLines.size() - newLineCount);
            String content = join(currentLines);
            int inputLength = heading.length() + ChunkDraft.HEADING_SEPARATOR.length() + content.length();
            int tokenCount = Math.ceilDiv(inputLength, charsPerToken);
            drafts.add(new ChunkDraft(drafts.size(), firstNewLine.pageNumber(), heading, content, tokenCount));
            currentLines.clear();
            newLineCount = 0;
        }
    }
}
