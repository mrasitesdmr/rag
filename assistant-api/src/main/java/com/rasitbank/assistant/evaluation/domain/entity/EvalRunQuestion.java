package com.rasitbank.assistant.evaluation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/**
 * Çalıştırma başlarken kopyalanan bir golden sorudur: cevabın geçtiği parçalar, ölçülüp ölçülmediği ve sonucu
 * (doğru parçanın vector sırası, prompt'taki yeri, reranker'ın düşüp düşmediği).
 */
@Entity
@Table(name = "eval_run_questions")
public class EvalRunQuestion {

    @EmbeddedId
    private EvalRunQuestionId id;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "answer_text", nullable = false)
    private String answerText;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answer_chunk_ids", nullable = false)
    private long[] answerChunkIds;

    @Column(name = "is_measured", nullable = false)
    private boolean measured;

    @Column(name = "is_processed", nullable = false)
    private boolean processed;

    @Column(name = "vector_rank")
    private @Nullable Integer vectorRank;

    @Column(name = "prompt_position")
    private @Nullable Integer promptPosition;

    @Column(name = "is_rerank_fallback", nullable = false)
    private boolean rerankFallback;

    protected EvalRunQuestion() {
    }

    public EvalRunQuestion(long evalRunId, int position, String category, String text, int pageNumber,
            String answerText, long[] answerChunkIds) {
        this.id = new EvalRunQuestionId(evalRunId, position);
        this.category = category;
        this.text = text;
        this.pageNumber = pageNumber;
        this.answerText = answerText;
        this.answerChunkIds = answerChunkIds.clone();
        this.measured = answerChunkIds.length > 0;
    }

    public void recordResult(@Nullable Integer vectorRank, @Nullable Integer promptPosition, boolean rerankFallback) {
        if (!measured) {
            throw new IllegalStateException("An unmatched question is not measured: " + id.getPosition());
        }
        this.processed = true;
        this.vectorRank = vectorRank;
        this.promptPosition = promptPosition;
        this.rerankFallback = rerankFallback;
    }

    public long getEvalRunId() {
        return id.getEvalRunId();
    }

    public int getPosition() {
        return id.getPosition();
    }

    public String getCategory() {
        return category;
    }

    public String getText() {
        return text;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getAnswerText() {
        return answerText;
    }

    public Set<Long> getAnswerChunkIds() {
        return Arrays.stream(answerChunkIds).boxed().collect(Collectors.toUnmodifiableSet());
    }

    public boolean isMeasured() {
        return measured;
    }

    public boolean isProcessed() {
        return processed;
    }

    public @Nullable Integer getVectorRank() {
        return vectorRank;
    }

    public @Nullable Integer getPromptPosition() {
        return promptPosition;
    }

    public boolean isRerankFallback() {
        return rerankFallback;
    }
}
