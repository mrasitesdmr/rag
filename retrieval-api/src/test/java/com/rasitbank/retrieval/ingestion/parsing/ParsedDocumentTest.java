package com.rasitbank.retrieval.ingestion.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Soru sayısının art arda soru satırlarını tek soru sayarak ve kısım sınırında sıfırlanarak hesaplandığını doğrular.
 */
class ParsedDocumentTest {

    @Test
    void givenSectionsWithTwoSingleAndOneTwoLineQuestions_whenQuestionCount_thenThree() {
        ParsedSection first = new ParsedSection("2 Kartlar · Sık sorulanlar", List.of(
                new PageLine(5, "Birinci soru?", true), new PageLine(5, "Cevap."),
                new PageLine(5, "İkinci soru birinci satır", true), new PageLine(5, "ikinci satır?", true),
                new PageLine(6, "Cevap.")));
        ParsedSection second = new ParsedSection("3 Hesaplar · Sık sorulanlar", List.of(
                new PageLine(7, "Üçüncü soru?", true), new PageLine(7, "Cevap.")));
        ParsedSection plain = new ParsedSection("3.1 Hesap", List.of(new PageLine(7, "Düz satır.")));
        ParsedDocument document = new ParsedDocument(70, 11, List.of(first, plain, second), 0);

        assertThat(document.questionCount()).isEqualTo(3);
    }

    @Test
    void givenQuestionLineLastInOneSectionAndFirstInNext_whenQuestionCount_thenCountedSeparately() {
        ParsedSection first = new ParsedSection("2 Kartlar · Sık sorulanlar",
                List.of(new PageLine(5, "Soru bir?", true)));
        ParsedSection second = new ParsedSection("3 Hesaplar · Sık sorulanlar",
                List.of(new PageLine(7, "Soru iki?", true)));

        assertThat(new ParsedDocument(70, 11, List.of(first, second), 0).questionCount()).isEqualTo(2);
    }
}
