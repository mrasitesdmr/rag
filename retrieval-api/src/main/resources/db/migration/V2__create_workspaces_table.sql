CREATE TABLE workspaces (
    id                   bigint      PRIMARY KEY,
    name                 text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 80),
    embedding_model      text        NOT NULL,
    embedding_dimensions int         NOT NULL CHECK (embedding_dimensions > 0),
    created_at           timestamptz NOT NULL DEFAULT now()
);

INSERT INTO workspaces (id, name, embedding_model, embedding_dimensions)
VALUES (42, 'Raşit Bankası · Dijital Asistan', 'bge-m3', 1024);
