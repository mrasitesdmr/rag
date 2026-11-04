ALTER TABLE conversation_turns
    DROP CONSTRAINT ck_conversation_turns_answer_source,
    DROP CONSTRAINT ck_conversation_turns_none_reason;

ALTER TABLE conversation_turns
    ADD CONSTRAINT ck_conversation_turns_answer_source
        CHECK (answer_source IN ('MODEL', 'EMPTY_KNOWLEDGE_BASE', 'BELOW_RERANK_THRESHOLD', 'SMALL_TALK', 'NO_WORDS')),
    ADD CONSTRAINT ck_conversation_turns_none_reason
        CHECK (retrieval_strategy <> 'NONE' OR generation_mode = 'LLM_ONLY'
            OR answer_source IN ('SMALL_TALK', 'NO_WORDS')),
    ADD CONSTRAINT ck_conversation_turns_no_words
        CHECK (answer_source <> 'NO_WORDS'
            OR (generation_mode = 'RAG' AND retrieval_strategy = 'NONE' AND not_found_answer
                AND cardinality(cited) = 0 AND message_label IS NULL));
