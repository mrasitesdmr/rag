package com.rasitbank.assistant.suggestion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Bir öneri sorusu cevaplandıktan sonra aynı konuşmada önerilen takip sorusudur.
 */
@Entity
@Table(name = "suggested_follow_ups")
public class SuggestedFollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private SuggestedQuestion question;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "text", nullable = false)
    private String text;

    protected SuggestedFollowUp() {
    }

    public SuggestedFollowUp(SuggestedQuestion question, int position, String text) {
        this.question = question;
        this.position = position;
        this.text = text;
    }

    public @Nullable Long getId() {
        return id;
    }

    public SuggestedQuestion getQuestion() {
        return question;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }
}
