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
 * Bir kategorideki öneri sorusudur; başlangıç önerisi olup olmadığını ve kategorideki sırasını taşır.
 */
@Entity
@Table(name = "suggested_questions")
public class SuggestedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SuggestionCategory category;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "starter", nullable = false)
    private boolean starter;

    protected SuggestedQuestion() {
    }

    public SuggestedQuestion(SuggestionCategory category, int position, String text, boolean starter) {
        this.category = category;
        this.position = position;
        this.text = text;
        this.starter = starter;
    }

    public @Nullable Long getId() {
        return id;
    }

    public SuggestionCategory getCategory() {
        return category;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public boolean isStarter() {
        return starter;
    }
}
