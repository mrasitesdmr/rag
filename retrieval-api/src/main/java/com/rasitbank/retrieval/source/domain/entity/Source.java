package com.rasitbank.retrieval.source.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Çalışma alanındaki bir bilgi kaynağıdır (ör. ürün ve hizmet rehberi); bilgi bankasındaki aktif sürümünü gösterir.
 */
@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_id")
    @SequenceGenerator(name = "source_id", sequenceName = "sources_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private long workspaceId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceKind kind;

    @Column(name = "active_version_id")
    private @Nullable Long activeVersionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Source() {
    }

    private Source(long workspaceId, String name, SourceKind kind, Instant createdAt) {
        this.workspaceId = workspaceId;
        this.name = name;
        this.kind = kind;
        this.createdAt = createdAt;
    }

    public static Source create(long workspaceId, String name, SourceKind kind, Instant now) {
        return new Source(workspaceId, name, kind, now);
    }

    public void activate(long sourceVersionId) {
        this.activeVersionId = sourceVersionId;
    }

    public boolean hasActiveVersion() {
        return activeVersionId != null;
    }

    public Long getId() {
        return id;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public SourceKind getKind() {
        return kind;
    }

    public @Nullable Long getActiveVersionId() {
        return activeVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
