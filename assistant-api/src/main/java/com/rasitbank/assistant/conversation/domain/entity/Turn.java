package com.rasitbank.assistant.conversation.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/**
 * Konuşmanın bir turudur (bir soru ve cevabı); cevabın neye dayandığını sonradan incelemek için arama sorgusu,
 * strateji, aday parçalar, alıntılar, token sayıları, süreler ve hata kodu bu kayıtta durur.
 */
@Entity
@Table(name = "conversation_turns")
public class Turn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "turn_no", nullable = false)
    private int turnNo;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "search_query", nullable = false)
    private String searchQuery;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_rewrite_status", nullable = false)
    private QueryRewriteStatus queryRewriteStatus;

    @Column(name = "query_rewrite_latency_ms")
    private @Nullable Integer queryRewriteLatencyMs;

    @Column(name = "history_turns", nullable = false)
    private int historyTurns;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false)
    private GenerationMode generationMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrieval_strategy", nullable = false)
    private RetrievalStrategy retrievalStrategy;

    @Column(name = "candidates", nullable = false)
    private int candidates;

    @Column(name = "top_k", nullable = false)
    private int topK;

    @Column(name = "chat_model", nullable = false)
    private String chatModel;

    @Column(name = "answer")
    private @Nullable String answer;

    @Column(name = "not_found_answer", nullable = false)
    private boolean notFoundAnswer;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cited", nullable = false)
    private int[] cited;

    @Column(name = "prompt_tokens")
    private @Nullable Integer promptTokens;

    @Column(name = "completion_tokens")
    private @Nullable Integer completionTokens;

    @Column(name = "retrieval_latency_ms")
    private @Nullable Integer retrievalLatencyMs;

    @Column(name = "rerank_latency_ms")
    private @Nullable Integer rerankLatencyMs;

    @Column(name = "generation_latency_ms")
    private @Nullable Integer generationLatencyMs;

    @Column(name = "time_to_first_token_ms")
    private @Nullable Integer timeToFirstTokenMs;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "error_code")
    private @Nullable String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @OneToMany(mappedBy = "turn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.vectorRank")
    private List<TurnChunk> chunks = new ArrayList<>();

    protected Turn() {
    }

    private Turn(Conversation conversation, int turnNo, TurnValues values) {
        this.conversation = conversation;
        this.turnNo = turnNo;
        this.question = values.question();
        this.searchQuery = values.searchQuery();
        this.queryRewriteStatus = values.queryRewriteStatus();
        this.queryRewriteLatencyMs = values.queryRewriteLatencyMs();
        this.historyTurns = values.historyTurns();
        this.generationMode = values.generationMode();
        this.retrievalStrategy = values.retrievalStrategy();
        this.candidates = values.candidates();
        this.topK = values.topK();
        this.chatModel = values.chatModel();
        this.answer = values.answer();
        this.notFoundAnswer = values.notFoundAnswer();
        this.cited = values.cited().stream().mapToInt(Integer::intValue).toArray();
        this.promptTokens = values.promptTokens();
        this.completionTokens = values.completionTokens();
        this.retrievalLatencyMs = values.retrievalLatencyMs();
        this.rerankLatencyMs = values.rerankLatencyMs();
        this.generationLatencyMs = values.generationLatencyMs();
        this.timeToFirstTokenMs = values.timeToFirstTokenMs();
        this.latencyMs = values.latencyMs();
        this.errorCode = values.errorCode();
        this.createdAt = values.createdAt();
        this.completedAt = values.completedAt();
    }

    public static Turn record(Conversation conversation, int turnNo, TurnValues values, List<TurnChunkValues> chunks) {
        Turn turn = new Turn(conversation, turnNo, values);
        chunks.forEach(chunk -> turn.chunks.add(new TurnChunk(turn, chunk)));
        return turn;
    }

    public @Nullable Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public String getQuestion() {
        return question;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public QueryRewriteStatus getQueryRewriteStatus() {
        return queryRewriteStatus;
    }

    public @Nullable Integer getQueryRewriteLatencyMs() {
        return queryRewriteLatencyMs;
    }

    public int getHistoryTurns() {
        return historyTurns;
    }

    public GenerationMode getGenerationMode() {
        return generationMode;
    }

    public RetrievalStrategy getRetrievalStrategy() {
        return retrievalStrategy;
    }

    public int getCandidates() {
        return candidates;
    }

    public int getTopK() {
        return topK;
    }

    public String getChatModel() {
        return chatModel;
    }

    public @Nullable String getAnswer() {
        return answer;
    }

    public boolean isNotFoundAnswer() {
        return notFoundAnswer;
    }

    public List<Integer> getCited() {
        return Arrays.stream(cited).boxed().toList();
    }

    public @Nullable Integer getPromptTokens() {
        return promptTokens;
    }

    public @Nullable Integer getCompletionTokens() {
        return completionTokens;
    }

    public @Nullable Integer getRetrievalLatencyMs() {
        return retrievalLatencyMs;
    }

    public @Nullable Integer getRerankLatencyMs() {
        return rerankLatencyMs;
    }

    public @Nullable Integer getGenerationLatencyMs() {
        return generationLatencyMs;
    }

    public @Nullable Integer getTimeToFirstTokenMs() {
        return timeToFirstTokenMs;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public @Nullable String getErrorCode() {
        return errorCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<TurnChunk> getChunks() {
        return List.copyOf(chunks);
    }
}
