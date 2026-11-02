CREATE TABLE golden_sets (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version      text        NOT NULL CHECK (char_length(version) BETWEEN 1 AND 40),
    content_hash text        NOT NULL,
    loaded_at    timestamptz NOT NULL
);

CREATE TABLE golden_questions (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    golden_set_id   bigint NOT NULL REFERENCES golden_sets (id) ON DELETE CASCADE,
    position        int    NOT NULL CHECK (position >= 1),
    category        text   NOT NULL CHECK (char_length(category) BETWEEN 1 AND 40),
    text            text   NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200),
    normalized_text text   NOT NULL,
    page_number     int    NOT NULL CHECK (page_number >= 1),
    answer_text     text   NOT NULL CHECK (char_length(answer_text) BETWEEN 20 AND 300),
    CONSTRAINT uq_golden_questions_set_position UNIQUE (golden_set_id, position),
    CONSTRAINT uq_golden_questions_set_normalized_text UNIQUE (golden_set_id, normalized_text)
);

CREATE INDEX ix_golden_questions_normalized_text ON golden_questions (normalized_text);

CREATE TABLE eval_runs (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status                text             NOT NULL CHECK (status IN ('RUNNING', 'DONE', 'FAILED')),
    golden_set_version    text             NOT NULL,
    retrieval_strategy    text             NOT NULL CHECK (retrieval_strategy IN ('TOP_K', 'RERANK')),
    candidates            int              NOT NULL CHECK (candidates >= 1),
    top_k                 int              NOT NULL CHECK (top_k >= 1),
    source_versions       text             NOT NULL,
    question_count        int              NOT NULL CHECK (question_count >= 1),
    measured_count        int              NOT NULL CHECK (measured_count >= 1),
    processed_count       int              NOT NULL DEFAULT 0 CHECK (processed_count >= 0),
    hit_count             int,
    recall_at_k           double precision,
    mrr                   double precision,
    rerank_fallback_count int              NOT NULL DEFAULT 0 CHECK (rerank_fallback_count >= 0),
    error_code            text CHECK (error_code IN ('RETRIEVAL_UNAVAILABLE', 'EVAL_RUN_INTERRUPTED', 'INTERNAL_ERROR')),
    started_at            timestamptz      NOT NULL,
    finished_at           timestamptz,
    duration_ms           bigint,
    CONSTRAINT ck_eval_runs_counts
        CHECK (measured_count <= question_count AND processed_count <= measured_count),
    CONSTRAINT ck_eval_runs_done_has_metrics
        CHECK ((status = 'DONE') = (hit_count IS NOT NULL AND recall_at_k IS NOT NULL AND mrr IS NOT NULL)),
    CONSTRAINT ck_eval_runs_failed_has_error
        CHECK ((status = 'FAILED') = (error_code IS NOT NULL)),
    CONSTRAINT ck_eval_runs_finished
        CHECK ((status = 'RUNNING') = (finished_at IS NULL AND duration_ms IS NULL))
);

CREATE UNIQUE INDEX ux_eval_runs_single_running ON eval_runs (status) WHERE status = 'RUNNING';

CREATE INDEX ix_eval_runs_started_at_id ON eval_runs (started_at DESC, id DESC);

CREATE TABLE eval_run_questions (
    eval_run_id        bigint   NOT NULL REFERENCES eval_runs (id) ON DELETE CASCADE,
    position           int      NOT NULL CHECK (position >= 1),
    category           text     NOT NULL,
    text               text     NOT NULL,
    page_number        int      NOT NULL,
    answer_text        text     NOT NULL,
    answer_chunk_ids   bigint[] NOT NULL,
    is_measured        boolean  NOT NULL,
    is_processed       boolean  NOT NULL DEFAULT false,
    vector_rank        int,
    prompt_position    int,
    is_rerank_fallback boolean  NOT NULL DEFAULT false,
    PRIMARY KEY (eval_run_id, position),
    CONSTRAINT ck_eval_run_questions_measured
        CHECK (is_measured = (cardinality(answer_chunk_ids) > 0)),
    CONSTRAINT ck_eval_run_questions_processed
        CHECK (is_processed OR (vector_rank IS NULL AND prompt_position IS NULL AND NOT is_rerank_fallback))
);
