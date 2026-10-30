package com.rasitbank.retrieval.chunk.domain.entity;

import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bir kaynak sürümünden çıkarılan, başlığı, sayfası ve embedding vektörüyle saklanan metin parçasıdır.
 */
@Entity
@Table(name = "chunks")
public class Chunk {

    public static final int EMBEDDING_DIMENSIONS = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chunk_id")
    @SequenceGenerator(name = "chunk_id", sequenceName = "chunks_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private long workspaceId;

    @Column(name = "source_id", nullable = false)
    private long sourceId;

    @Column(name = "source_version_id", nullable = false)
    private long sourceVersionId;

    @Column(name = "chunk_no", nullable = false)
    private int chunkNo;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(nullable = false)
    private String heading;

    @Column(nullable = false)
    private String content;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSIONS)
    @Column(nullable = false)
    private float[] embedding;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Chunk() {
    }

    private Chunk(long workspaceId, long sourceId, long sourceVersionId, ChunkDraft draft, float[] embedding,
            String embeddingModel, Instant createdAt) {
        this.workspaceId = workspaceId;
        this.sourceId = sourceId;
        this.sourceVersionId = sourceVersionId;
        this.chunkNo = draft.chunkNo();
        this.pageNumber = draft.pageNumber();
        this.heading = draft.heading();
        this.content = draft.content();
        this.tokenCount = draft.tokenCount();
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
        this.createdAt = createdAt;
    }

    public static Chunk of(long workspaceId, long sourceId, long sourceVersionId, ChunkDraft draft, float[] embedding,
            String embeddingModel, Instant now) {
        return new Chunk(workspaceId, sourceId, sourceVersionId, draft, embedding, embeddingModel, now);
    }

    public Long getId() {
        return id;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public long getSourceId() {
        return sourceId;
    }

    public long getSourceVersionId() {
        return sourceVersionId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getHeading() {
        return heading;
    }

    public String getContent() {
        return content;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public float[] getEmbedding() {
        return embedding.clone();
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
