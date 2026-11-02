package com.rasitbank.assistant.evaluation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Ürün ekibinin yüklediği golden set'in sürümünü, içerik özetini ve yüklenme zamanını tutar; aynı anda tek set vardır.
 */
@Entity
@Table(name = "golden_sets")
public class GoldenSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "loaded_at", nullable = false)
    private Instant loadedAt;

    protected GoldenSet() {
    }

    public GoldenSet(String version, String contentHash, Instant loadedAt) {
        this.version = version;
        this.contentHash = contentHash;
        this.loadedAt = loadedAt;
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }
}
