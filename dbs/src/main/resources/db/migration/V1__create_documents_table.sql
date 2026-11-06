-- названия и типы колонок должны соответствовать твоему DocumentEntity
CREATE TABLE documents
(
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    s3_key       VARCHAR(500) NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_documents_user_id
    ON documents (user_id);

CREATE INDEX idx_documents_created_at
    ON documents (created_at DESC);
