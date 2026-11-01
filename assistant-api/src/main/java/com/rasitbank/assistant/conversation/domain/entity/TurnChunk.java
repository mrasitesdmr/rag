package com.rasitbank.assistant.conversation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Bir turda retrieval-api'den gelen aday parçanın kaydıdır: vector sırası, mesafe, rerank puanı ve prompt'a
 * girdiyse prompt'taki yeri.
 */
@Entity
@Table(name = "turn_chunks")
public class TurnChunk {

    @EmbeddedId
    private TurnChunkId id;

    @MapsId("turnId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turn_id", nullable = false)
    private Turn turn;

    @Column(name = "chunk_id", nullable = false)
    private long chunkId;

    @Column(name = "source_id", nullable = false)
    private long sourceId;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "chunk_no", nullable = false)
    private int chunkNo;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "heading", nullable = false)
    private String heading;

    @Column(name = "distance", nullable = false)
    private double distance;

    @Column(name = "rerank_score")
    private @Nullable Double rerankScore;

    @Column(name = "prompt_position")
    private @Nullable Integer promptPosition;

    protected TurnChunk() {
    }

    TurnChunk(Turn turn, TurnChunkValues values) {
        this.id = new TurnChunkId(values.vectorRank());
        this.turn = turn;
        this.chunkId = values.chunkId();
        this.sourceId = values.sourceId();
        this.version = values.version();
        this.chunkNo = values.chunkNo();
        this.pageNumber = values.pageNumber();
        this.heading = values.heading();
        this.distance = values.distance();
        this.rerankScore = values.rerankScore();
        this.promptPosition = values.promptPosition();
    }

    public int getVectorRank() {
        return id.getVectorRank();
    }

    public long getChunkId() {
        return chunkId;
    }

    public long getSourceId() {
        return sourceId;
    }

    public String getVersion() {
        return version;
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

    public double getDistance() {
        return distance;
    }

    public @Nullable Double getRerankScore() {
        return rerankScore;
    }

    public @Nullable Integer getPromptPosition() {
        return promptPosition;
    }
}
