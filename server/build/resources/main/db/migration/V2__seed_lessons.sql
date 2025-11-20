-- Seed initial lessons
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM lessons) THEN
    INSERT INTO lessons (title, description, "order", total_words, total_exercises, difficulty, category, icon_url, xp_reward, coins_reward, is_unlocked, is_premium, created_at, updated_at)
    VALUES
      ('Basics 1', 'Greetings and simple phrases', 1, 20, 7, 'easy', 'basics', NULL, 50, 10, TRUE, FALSE, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
      ('Basics 2', 'Numbers and introductions', 2, 20, 8, 'easy', 'basics', NULL, 60, 12, FALSE, FALSE, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
      ('Food', 'Common food vocabulary', 3, 25, 9, 'medium', 'vocabulary', NULL, 80, 15, FALSE, FALSE, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint);
  END IF;
END$$;