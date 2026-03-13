-- MediVoice PostgreSQL Schema Initialization
-- This runs on first database creation

CREATE TABLE IF NOT EXISTS symptom_sessions (
    id              VARCHAR(255) PRIMARY KEY,
    session_status  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at       TIMESTAMP,
    transcript      TEXT,
    triage_level    VARCHAR(10) DEFAULT 'LOW',
    triage_reason   VARCHAR(500),
    user_agent      VARCHAR(500),
    remote_address  VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_sessions_status ON symptom_sessions(session_status);
CREATE INDEX IF NOT EXISTS idx_sessions_created ON symptom_sessions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sessions_triage ON symptom_sessions(triage_level);
