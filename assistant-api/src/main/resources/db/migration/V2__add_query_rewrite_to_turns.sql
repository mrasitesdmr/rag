ALTER TABLE conversation_turns ADD COLUMN query_rewrite_status text;

UPDATE conversation_turns SET query_rewrite_status = 'DISABLED';

ALTER TABLE conversation_turns
    ALTER COLUMN query_rewrite_status SET NOT NULL,
    ADD CONSTRAINT ck_conversation_turns_query_rewrite_status
        CHECK (query_rewrite_status IN ('DISABLED', 'NO_HISTORY', 'REWRITTEN', 'FAILED', 'EMPTY')),
    ADD COLUMN query_rewrite_latency_ms int,
    ADD COLUMN history_turns int NOT NULL DEFAULT 0 CHECK (history_turns >= 0);
