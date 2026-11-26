CREATE TABLE memories
(
    id UUID PRIMARY KEY,

    memory_type VARCHAR(40) NOT NULL,

    title VARCHAR(200) NOT NULL,

    content TEXT NOT NULL,

    source VARCHAR(100),

    importance INTEGER NOT NULL DEFAULT 3,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    access_count INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    last_accessed_at TIMESTAMPTZ,

    CONSTRAINT chk_memory_importance
        CHECK (importance BETWEEN 1 AND 5),

    CONSTRAINT chk_memory_access_count
        CHECK (access_count >= 0)
);

CREATE INDEX idx_memories_type
    ON memories(memory_type);

CREATE INDEX idx_memories_created_at
    ON memories(created_at DESC);

CREATE INDEX idx_memories_active
    ON memories(active);

CREATE INDEX idx_memories_importance
    ON memories(importance DESC);