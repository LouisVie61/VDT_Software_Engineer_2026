CREATE TABLE IF NOT EXISTS app_users (
    id VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS query_history (
    id UUID PRIMARY KEY,
    user_identity VARCHAR(100) NOT NULL REFERENCES app_users(id),
    session_id VARCHAR(120),
    nl_query TEXT NOT NULL,
    generated_dsl TEXT NOT NULL,
    summary TEXT,
    chart_type VARCHAR(40) NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    result_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE query_history ADD COLUMN IF NOT EXISTS session_id VARCHAR(120);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    user_identity VARCHAR(100) NOT NULL REFERENCES app_users(id),
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nl_query TEXT NOT NULL,
    generated_dsl TEXT,
    results_count INTEGER,
    execution_time_ms BIGINT,
    status VARCHAR(40) NOT NULL,
    llm_provider VARCHAR(40),
    error_message TEXT,
    session_id VARCHAR(120),
    predicted_intent VARCHAR(60),
    override_intent VARCHAR(60),
    selected_template VARCHAR(60),
    confidence_scores TEXT,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    diagnostic_classification VARCHAR(100)
);

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS session_id VARCHAR(120);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS predicted_intent VARCHAR(60);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS override_intent VARCHAR(60);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS selected_template VARCHAR(60);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS confidence_scores TEXT;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS cache_hit BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS diagnostic_classification VARCHAR(100);

INSERT INTO app_users (id, display_name, role_name)
VALUES ('soc-analyst-demo', 'SOC Analyst Demo', 'SOC_ANALYST')
ON CONFLICT (id) DO NOTHING;
