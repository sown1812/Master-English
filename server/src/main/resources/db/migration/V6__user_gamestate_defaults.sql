-- Ensure defaults exist for new tables in case of migrations on existing DBs
ALTER TABLE user_boosters ALTER COLUMN updated_at SET DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000;
ALTER TABLE user_quests ALTER COLUMN updated_at SET DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000;
ALTER TABLE daily_challenges ALTER COLUMN updated_at SET DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000;
