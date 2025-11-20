-- Seed words for lesson 1 (Basics 1)
DO $$
DECLARE now_ms BIGINT := (extract(epoch from now())*1000)::bigint;
BEGIN
  IF EXISTS (SELECT 1 FROM lessons WHERE "order" = 1) THEN
    INSERT INTO words (word, translation, pronunciation, part_of_speech, example_sentence, example_translation, image_url, audio_url, lesson_id, difficulty, category, created_at, updated_at)
    SELECT w.word, w.translation, w.pronunciation, w.part_of_speech, w.example_sentence, w.example_translation, NULL, NULL, l.id, 1, 'basics', now_ms, now_ms
    FROM (
      VALUES
        ('hello','xin chào','həˈloʊ','interjection','Hello, how are you?','Xin chào, bạn khỏe không?'),
        ('goodbye','tạm biệt','ˌɡʊdˈbaɪ','interjection','Goodbye! See you later.','Tạm biệt! Hẹn gặp lại.'),
        ('please','làm ơn','pliːz','adverb','Please help me.','Làm ơn giúp tôi.'),
        ('thank you','cảm ơn','ˈθæŋk juː','expression','Thank you very much.','Cảm ơn bạn rất nhiều.'),
        ('yes','vâng','jɛs','adverb','Yes, I understand.','Vâng, tôi hiểu.')
    ) AS w(word, translation, pronunciation, part_of_speech, example_sentence, example_translation)
    CROSS JOIN (SELECT id FROM lessons WHERE "order" = 1) l
    ON CONFLICT DO NOTHING;
  END IF;
END$$;