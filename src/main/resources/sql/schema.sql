-- sql
CREATE TABLE comment
(
    comment_id        VARCHAR(100) PRIMARY KEY,               -- UUID 권장 (예: uuid_generate_v4() 결과를 문자열로 저장)
    article_id        VARCHAR(100) NOT NULL,                  -- 외부 Article 서비스 id (FK 미설정 권장)
    writer_id         VARCHAR(100) NOT NULL,                  -- 외부 User 서비스 id
    parent_comment_id VARCHAR(100),                           -- 부모 댓글 id (NULL이면 최상위)
    root_comment_id   VARCHAR(100),                           -- 스레드 루트 id (자기 자신 또는 최상위 id)
    depth             SMALLINT     NOT NULL DEFAULT 0,        -- 0: 루트, 1: 1뎁스, 2: 2뎁스
    contents          TEXT         NOT NULL,                  -- 댓글 내용
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,    -- soft-delete flag
    status            VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE', -- 예: ACTIVE, HIDDEN, BANNED, PENDING_REVIEW
    reply_count       INTEGER      NOT NULL DEFAULT 0,        -- 자식 댓글 수 (빠른 조회용)
    created_at        TIMESTAMP    NOT NULL,                  -- UTC 저장 권장
    updated_at        TIMESTAMP    NOT NULL,
    deleted_at        TIMESTAMP,
    CONSTRAINT chk_depth_range CHECK (depth BETWEEN 0 AND 2)
);

-- 인덱스: 조회 패턴에 맞춰 추가 (Postgres/MySQL 공통)
CREATE INDEX idx_comment_article_created ON comment (article_id, created_at);
CREATE INDEX idx_comment_parent ON comment (parent_comment_id);
CREATE INDEX idx_comment_root ON comment (root_comment_id);
CREATE INDEX idx_comment_writer ON comment (writer_id);
CREATE INDEX idx_comment_status ON comment (status);
