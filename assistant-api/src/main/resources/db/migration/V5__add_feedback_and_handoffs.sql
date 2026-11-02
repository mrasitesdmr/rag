ALTER TABLE conversation_turns
    ADD COLUMN helpful boolean,
    ADD COLUMN feedback_at timestamptz;

ALTER TABLE conversation_turns
    ADD CONSTRAINT ck_conversation_turns_feedback_pair
        CHECK ((helpful IS NULL) = (feedback_at IS NULL)),
    ADD CONSTRAINT ck_conversation_turns_feedback_completed
        CHECK (helpful IS NULL OR error_code IS NULL);

CREATE TABLE handoffs (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id bigint      NOT NULL REFERENCES conversations (id),
    turn_id         bigint      NOT NULL REFERENCES conversation_turns (id),
    reason          text        NOT NULL CHECK (reason IN ('NO_ANSWER')),
    queue           text        NOT NULL CHECK (char_length(queue) BETWEEN 1 AND 40),
    created_at      timestamptz NOT NULL,
    CONSTRAINT uq_handoffs_conversation UNIQUE (conversation_id)
);
