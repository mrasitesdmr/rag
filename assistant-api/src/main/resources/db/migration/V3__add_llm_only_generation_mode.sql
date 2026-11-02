ALTER TABLE conversation_turns
    DROP CONSTRAINT conversation_turns_generation_mode_check,
    DROP CONSTRAINT conversation_turns_retrieval_strategy_check;

ALTER TABLE conversation_turns
    ADD CONSTRAINT ck_conversation_turns_generation_mode
        CHECK (generation_mode IN ('RAG', 'LLM_ONLY')),
    ADD CONSTRAINT ck_conversation_turns_retrieval_strategy
        CHECK (retrieval_strategy IN ('TOP_K', 'RERANK', 'RERANK_FALLBACK', 'NONE')),
    ALTER COLUMN search_query DROP NOT NULL,
    ALTER COLUMN candidates DROP NOT NULL,
    ALTER COLUMN top_k DROP NOT NULL;

ALTER TABLE conversation_turns
    ADD CONSTRAINT ck_conversation_turns_mode_strategy
        CHECK ((generation_mode = 'LLM_ONLY') = (retrieval_strategy = 'NONE')),
    ADD CONSTRAINT ck_conversation_turns_none_has_no_retrieval
        CHECK (retrieval_strategy <> 'NONE'
            OR (search_query IS NULL AND candidates IS NULL AND top_k IS NULL
                AND retrieval_latency_ms IS NULL AND rerank_latency_ms IS NULL)),
    ADD CONSTRAINT ck_conversation_turns_retrieval_has_values
        CHECK (retrieval_strategy = 'NONE'
            OR (search_query IS NOT NULL AND candidates IS NOT NULL AND top_k IS NOT NULL));
