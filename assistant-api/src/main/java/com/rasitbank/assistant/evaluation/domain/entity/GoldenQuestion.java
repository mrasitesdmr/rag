package com.rasitbank.assistant.evaluation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Golden set'teki bir sorudur: kategorisi, metni, eşleştirme için normalize hâli, sayfası ve cevabın geçtiği cümle.
 */
@Entity
@Table(name = "golden_questions")
public class GoldenQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "golden_set_id", nullable = false)
    private long goldenSetId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "normalized_text", nullable = false)
    private String normalizedText;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "answer_text", nullable = false)
    private String answerText;

    protected GoldenQuestion() {
    }

    public GoldenQuestion(long goldenSetId, int position, String category, String text, String normalizedText,
            int pageNumber, String answerText) {
        this.goldenSetId = goldenSetId;
        this.position = position;
        this.category = category;
        this.text = text;
        this.normalizedText = normalizedText;
        this.pageNumber = pageNumber;
        this.answerText = answerText;
    }

    public @Nullable Long getId() {
        return id;
    }

    public long getGoldenSetId() {
        return goldenSetId;
    }

    public int getPosition() {
        return position;
    }

    public String getCategory() {
        return category;
    }

    public String getText() {
        return text;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getAnswerText() {
        return answerText;
    }
}
