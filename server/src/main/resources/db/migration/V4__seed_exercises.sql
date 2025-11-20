-- Seed exercises for lesson 1
DO $$
DECLARE now_ms BIGINT := (extract(epoch from now())*1000)::bigint;
BEGIN
  IF EXISTS (SELECT 1 FROM lessons WHERE "order" = 1) THEN
    INSERT INTO exercises (lesson_id, word_id, type, question, correct_answer, option_a, option_b, option_c, option_d, match_pairs, hint, explanation, "order", difficulty, created_at)
    SELECT l.id, NULL, 'multiple_choice', 'What is the translation of "hello"?', 'xin chào', 'xin chào', 'tạm biệt', 'vâng', 'cảm ơn', NULL, NULL, NULL, 1, 1, now_ms
    FROM (SELECT id FROM lessons WHERE "order" = 1) l;

    INSERT INTO exercises (lesson_id, word_id, type, question, correct_answer, option_a, option_b, option_c, option_d, match_pairs, hint, explanation, "order", difficulty, created_at)
    SELECT l.id, NULL, 'fill_in_blank', '_____ , how are you?', 'Hello', NULL, NULL, NULL, NULL, NULL, NULL, 'Capitalize the first letter.', 2, 1, now_ms
    FROM (SELECT id FROM lessons WHERE "order" = 1) l;
  END IF;
END$$;