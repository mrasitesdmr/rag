ALTER TABLE sources DROP CONSTRAINT sources_workspace_id_name_key;

CREATE UNIQUE INDEX sources_workspace_id_lower_name_key ON sources (workspace_id, lower(name));

ALTER TABLE ingestion_jobs DROP CONSTRAINT ingestion_jobs_error_code_check;

ALTER TABLE ingestion_jobs
    ADD CONSTRAINT ingestion_jobs_error_code_check
    CHECK (error_code IN ('PDF_UNREADABLE', 'PDF_NO_TEXT', 'PDF_NO_BOOKMARKS', 'EMBEDDING_UNAVAILABLE',
                          'EMBEDDING_DIMENSION_MISMATCH', 'EMBEDDING_MODEL_MISMATCH',
                          'INGESTION_INTERRUPTED', 'INTERNAL_ERROR'));
