package com.rasitbank.assistant.suggestion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Ürün ekibinin öneri listesindeki bir kategoridir; "Kayıtlı sorular" penceresinde başlık olarak görünür.
 */
@Entity
@Table(name = "suggestion_categories")
public class SuggestionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "name", nullable = false)
    private String name;

    protected SuggestionCategory() {
    }

    public SuggestionCategory(int position, String name) {
        this.position = position;
        this.name = name;
    }

    public @Nullable Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }
}
