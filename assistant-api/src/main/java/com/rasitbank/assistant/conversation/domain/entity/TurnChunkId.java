package com.rasitbank.assistant.conversation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Bir turun aday parçasının kimliğidir: turun kimliği ve parçanın vector sırası.
 */
@Embeddable
public class TurnChunkId implements Serializable {

    private @Nullable Long turnId;

    @Column(name = "vector_rank", nullable = false)
    private int vectorRank;

    protected TurnChunkId() {
    }

    TurnChunkId(int vectorRank) {
        this.vectorRank = vectorRank;
    }

    public @Nullable Long getTurnId() {
        return turnId;
    }

    public int getVectorRank() {
        return vectorRank;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TurnChunkId that)) {
            return false;
        }
        return vectorRank == that.vectorRank && Objects.equals(turnId, that.turnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(turnId, vectorRank);
    }
}
