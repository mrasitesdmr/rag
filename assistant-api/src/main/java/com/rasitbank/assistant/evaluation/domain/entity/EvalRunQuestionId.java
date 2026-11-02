package com.rasitbank.assistant.evaluation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Bir çalıştırmaya kopyalanan sorunun kimliğidir: çalıştırmanın kimliği ve sorunun golden set'teki sırası.
 */
@Embeddable
public class EvalRunQuestionId implements Serializable {

    @Column(name = "eval_run_id", nullable = false)
    private long evalRunId;

    @Column(name = "position", nullable = false)
    private int position;

    protected EvalRunQuestionId() {
    }

    public EvalRunQuestionId(long evalRunId, int position) {
        this.evalRunId = evalRunId;
        this.position = position;
    }

    public long getEvalRunId() {
        return evalRunId;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EvalRunQuestionId that)) {
            return false;
        }
        return evalRunId == that.evalRunId && position == that.position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(evalRunId, position);
    }
}
