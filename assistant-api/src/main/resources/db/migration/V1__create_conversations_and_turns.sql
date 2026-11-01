CREATE TABLE conversations (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    channel    text        NOT NULL CHECK (channel IN ('PREVIEW')),
    created_at timestamptz NOT NULL
);

CREATE TABLE conversation_turns (
    id                     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id        bigint      NOT NULL REFERENCES conversations (id),
    turn_no                int         NOT NULL CHECK (turn_no >= 1),
    question               text        NOT NULL CHECK (char_length(question) BETWEEN 1 AND 1000),
    search_query           text        NOT NULL,
    generation_mode        text        NOT NULL CHECK (generation_mode IN ('RAG')),
    retrieval_strategy     text        NOT NULL CHECK (retrieval_strategy IN ('TOP_K', 'RERANK', 'RERANK_FALLBACK')),
    candidates             int         NOT NULL,
    top_k                  int         NOT NULL,
    chat_model             text        NOT NULL,
    answer                 text,
    not_found_answer       boolean     NOT NULL DEFAULT false,
    cited                  int[]       NOT NULL DEFAULT '{}',
    prompt_tokens          int,
    completion_tokens      int,
    retrieval_latency_ms   int,
    rerank_latency_ms      int,
    generation_latency_ms  int,
    time_to_first_token_ms int,
    latency_ms             int         NOT NULL,
    error_code             text CHECK (error_code IN ('RETRIEVAL_UNAVAILABLE', 'CHAT_MODEL_UNAVAILABLE', 'STREAM_CANCELLED', 'INTERNAL_ERROR')),
    created_at             timestamptz NOT NULL,
    completed_at           timestamptz NOT NULL,
    CONSTRAINT uq_conversation_turns_conversation_turn_no UNIQUE (conversation_id, turn_no)
);

CREATE TABLE turn_chunks (
    turn_id         bigint           NOT NULL REFERENCES conversation_turns (id) ON DELETE CASCADE,
    vector_rank     int              NOT NULL CHECK (vector_rank >= 1),
    chunk_id        bigint           NOT NULL,
    source_id       bigint           NOT NULL,
    version         text             NOT NULL,
    chunk_no        int              NOT NULL,
    page_number     int              NOT NULL,
    heading         text             NOT NULL,
    distance        double precision NOT NULL,
    rerank_score    double precision,
    prompt_position int CHECK (prompt_position >= 1),
    PRIMARY KEY (turn_id, vector_rank),
    CONSTRAINT uq_turn_chunks_turn_prompt_position UNIQUE (turn_id, prompt_position)
);
