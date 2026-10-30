CREATE SEQUENCE sources_id_seq START WITH 10001 INCREMENT BY 1;

CREATE TABLE sources (
    id                bigint      PRIMARY KEY DEFAULT nextval('sources_id_seq'),
    workspace_id      bigint      NOT NULL REFERENCES workspaces (id),
    name              text        NOT NULL CHECK (char_length(name) BETWEEN 2 AND 80),
    kind              text        NOT NULL CHECK (kind IN ('PDF')),
    active_version_id bigint      NULL,
    created_at        timestamptz NOT NULL,
    CONSTRAINT sources_workspace_id_name_key UNIQUE (workspace_id, name)
);

ALTER SEQUENCE sources_id_seq OWNED BY sources.id;

CREATE TABLE ingestion_jobs (
    id              bigint           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workspace_id    bigint           NOT NULL REFERENCES workspaces (id),
    source_id       bigint           NOT NULL REFERENCES sources (id),
    version         text             NOT NULL CHECK (char_length(version) BETWEEN 1 AND 40),
    file_name       text             NOT NULL CHECK (char_length(file_name) BETWEEN 1 AND 255),
    file_size_bytes bigint           NOT NULL CHECK (file_size_bytes > 0),
    stage           text             NOT NULL
        CHECK (stage IN ('QUEUED', 'PARSING', 'CHUNKING', 'EMBEDDING', 'WRITING', 'DONE', 'FAILED')),
    failed_stage    text             NULL
        CHECK (failed_stage IN ('QUEUED', 'PARSING', 'CHUNKING', 'EMBEDDING', 'WRITING')),
    progress        double precision NOT NULL CHECK (progress >= 0 AND progress <= 1),
    embedded_chunks int              NOT NULL DEFAULT 0,
    total_chunks    int              NULL,
    error_code      text             NULL
        CHECK (error_code IN ('PDF_UNREADABLE', 'PDF_NO_SECTIONS', 'EMBEDDING_UNAVAILABLE',
                              'EMBEDDING_DIMENSION_MISMATCH', 'EMBEDDING_MODEL_MISMATCH',
                              'INGESTION_INTERRUPTED', 'INTERNAL_ERROR')),
    created_at      timestamptz      NOT NULL,
    started_at      timestamptz      NULL,
    finished_at     timestamptz      NULL,
    CONSTRAINT ingestion_jobs_failed_has_error_check
        CHECK ((stage = 'FAILED') = (error_code IS NOT NULL AND failed_stage IS NOT NULL))
);

CREATE UNIQUE INDEX ingestion_jobs_one_active_per_source
    ON ingestion_jobs (source_id)
    WHERE stage NOT IN ('DONE', 'FAILED');

CREATE TABLE ingestion_job_files (
    ingestion_job_id bigint PRIMARY KEY REFERENCES ingestion_jobs (id),
    content          bytea  NOT NULL
);

CREATE TABLE source_versions (
    id               bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_id        bigint      NOT NULL REFERENCES sources (id),
    ingestion_job_id bigint      NOT NULL UNIQUE REFERENCES ingestion_jobs (id),
    version          text        NOT NULL,
    file_name        text        NOT NULL,
    file_size_bytes  bigint      NOT NULL,
    page_count       int         NOT NULL CHECK (page_count > 0),
    chapter_count    int         NOT NULL CHECK (chapter_count > 0),
    chunk_count      int         NOT NULL CHECK (chunk_count > 0),
    synced_at        timestamptz NOT NULL,
    created_at       timestamptz NOT NULL
);

ALTER TABLE sources
    ADD CONSTRAINT sources_active_version_id_fkey
    FOREIGN KEY (active_version_id) REFERENCES source_versions (id);
