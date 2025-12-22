CREATE UNIQUE INDEX IF NOT EXISTS idx_achievements_user_type
    ON achievements(user_id, achievement_type);

