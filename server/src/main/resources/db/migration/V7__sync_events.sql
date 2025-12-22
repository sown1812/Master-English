CREATE TABLE IF NOT EXISTS sync_events (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    event_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    occurred_at BIGINT NOT NULL,
    processed_at BIGINT NOT NULL DEFAULT (extract(epoch from now())*1000)::bigint
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_events_user_event ON sync_events(user_id, event_id);
CREATE INDEX IF NOT EXISTS idx_sync_events_user_processed ON sync_events(user_id, processed_at DESC);

