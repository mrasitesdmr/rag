CREATE SEQUENCE chunks_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE chunks (
    id                bigint       PRIMARY KEY,
    workspace_id      bigint       NOT NULL REFERENCES workspaces (id),
    source_id         bigint       NOT NULL REFERENCES sources (id),
    source_version_id bigint       NOT NULL REFERENCES source_versions (id),
    chunk_no          int          NOT NULL CHECK (chunk_no >= 0),
    page_number       int          NOT NULL CHECK (page_number >= 1),
    heading           text         NOT NULL CHECK (char_length(heading) BETWEEN 1 AND 200),
    content           text         NOT NULL CHECK (char_length(content) BETWEEN 1 AND 2000),
    token_count       int          NOT NULL CHECK (token_count >= 1),
    embedding         vector(1024) NOT NULL,
    embedding_model   text         NOT NULL,
    created_at        timestamptz  NOT NULL,
    CONSTRAINT chunks_source_version_id_chunk_no_key UNIQUE (source_version_id, chunk_no)
);

ALTER SEQUENCE chunks_id_seq OWNED BY chunks.id;

CREATE INDEX chunks_embedding_hnsw_idx ON chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX chunks_source_id_idx ON chunks (source_id);
