CREATE TABLE suggestion_categories (
    id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    position int  NOT NULL CHECK (position >= 0),
    name     text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 40),
    CONSTRAINT uq_suggestion_categories_position UNIQUE (position)
);

CREATE TABLE suggested_questions (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id bigint  NOT NULL REFERENCES suggestion_categories (id) ON DELETE CASCADE,
    position    int     NOT NULL CHECK (position >= 0),
    text        text    NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200),
    starter     boolean NOT NULL,
    CONSTRAINT uq_suggested_questions_category_position UNIQUE (category_id, position)
);

CREATE TABLE suggested_follow_ups (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id bigint NOT NULL REFERENCES suggested_questions (id) ON DELETE CASCADE,
    position    int    NOT NULL CHECK (position >= 0),
    text        text   NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200),
    CONSTRAINT uq_suggested_follow_ups_question_position UNIQUE (question_id, position)
);
