ALTER TABLE conversation_turns
    ADD COLUMN answer_source text NOT NULL DEFAULT 'MODEL',
    ADD COLUMN message_label text NULL,
    ADD COLUMN message_label_latency_ms integer NULL;

UPDATE conversation_turns t
SET answer_source = 'EMPTY_KNOWLEDGE_BASE'
WHERE t.generation_mode = 'RAG'
  AND t.error_code IS NULL
  AND t.not_found_answer
  AND NOT EXISTS (SELECT 1 FROM turn_chunks c WHERE c.turn_id = t.id);

ALTER TABLE conversation_turns
    DROP CONSTRAINT ck_conversation_turns_mode_strategy;

ALTER TABLE conversation_turns
    ADD CONSTRAINT ck_conversation_turns_answer_source
        CHECK (answer_source IN ('MODEL', 'EMPTY_KNOWLEDGE_BASE', 'BELOW_RERANK_THRESHOLD', 'SMALL_TALK')),
    ADD CONSTRAINT ck_conversation_turns_message_label
        CHECK (message_label IN ('GREETING', 'THANKS', 'FAREWELL', 'OTHER')),
    ADD CONSTRAINT ck_conversation_turns_message_label_latency
        CHECK (message_label_latency_ms >= 0),
    ADD CONSTRAINT ck_conversation_turns_label_latency_has_label
        CHECK (message_label_latency_ms IS NULL OR message_label IS NOT NULL),
    ADD CONSTRAINT ck_conversation_turns_llm_only_none
        CHECK (generation_mode <> 'LLM_ONLY' OR retrieval_strategy = 'NONE'),
    ADD CONSTRAINT ck_conversation_turns_none_reason
        CHECK (retrieval_strategy <> 'NONE' OR generation_mode = 'LLM_ONLY' OR answer_source = 'SMALL_TALK'),
    ADD CONSTRAINT ck_conversation_turns_small_talk
        CHECK (answer_source <> 'SMALL_TALK'
            OR (generation_mode = 'RAG' AND NOT not_found_answer AND cardinality(cited) = 0
                AND message_label IN ('GREETING', 'THANKS', 'FAREWELL'))),
    ADD CONSTRAINT ck_conversation_turns_below_threshold
        CHECK (answer_source <> 'BELOW_RERANK_THRESHOLD' OR (retrieval_strategy = 'RERANK' AND not_found_answer)),
    ADD CONSTRAINT ck_conversation_turns_llm_only_model
        CHECK (generation_mode <> 'LLM_ONLY' OR answer_source = 'MODEL');
