CREATE TABLE IF NOT EXISTS user_boosters (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    booster_key TEXT NOT NULL,
    is_owned BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_boosters_user ON user_boosters(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_boosters_user_key ON user_boosters(user_id, booster_key);

CREATE TABLE IF NOT EXISTS user_quests (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    quest_key TEXT NOT NULL,
    is_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_quests_user ON user_quests(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_quests_user_key ON user_quests(user_id, quest_key);

CREATE TABLE IF NOT EXISTS daily_challenges (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    target INTEGER NOT NULL DEFAULT 5,
    updated_at BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_challenges_user ON daily_challenges(user_id);
