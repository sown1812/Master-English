-- Initial schema for Master English
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    email TEXT NOT NULL,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    current_level INT NOT NULL DEFAULT 1,
    total_xp INT NOT NULL DEFAULT 0,
    coins INT NOT NULL DEFAULT 0,
    streak_days INT NOT NULL DEFAULT 0,
    last_study_date BIGINT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    words_learned INT NOT NULL DEFAULT 0,
    lessons_completed INT NOT NULL DEFAULT 0,
    exercises_completed INT NOT NULL DEFAULT 0,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    premium_expiry_date BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    last_synced_at BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lessons (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    "order" INT NOT NULL,
    total_words INT NOT NULL,
    total_exercises INT NOT NULL,
    difficulty TEXT NOT NULL,
    category TEXT NOT NULL,
    icon_url TEXT,
    xp_reward INT NOT NULL,
    coins_reward INT NOT NULL,
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS words (
    id SERIAL PRIMARY KEY,
    word TEXT NOT NULL,
    translation TEXT NOT NULL,
    pronunciation TEXT NOT NULL,
    part_of_speech TEXT NOT NULL,
    example_sentence TEXT NOT NULL,
    example_translation TEXT NOT NULL,
    image_url TEXT,
    audio_url TEXT,
    lesson_id INT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    difficulty INT NOT NULL,
    category TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS exercises (
    id SERIAL PRIMARY KEY,
    lesson_id INT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    word_id INT REFERENCES words(id) ON DELETE SET NULL,
    type TEXT NOT NULL,
    question TEXT NOT NULL,
    correct_answer TEXT NOT NULL,
    option_a TEXT,
    option_b TEXT,
    option_c TEXT,
    option_d TEXT,
    match_pairs TEXT,
    hint TEXT,
    explanation TEXT,
    "order" INT NOT NULL,
    difficulty INT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_progress (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    lesson_id INT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    word_id INT REFERENCES words(id) ON DELETE SET NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at BIGINT,
    score INT NOT NULL DEFAULT 0,
    accuracy REAL NOT NULL DEFAULT 0,
    time_spent BIGINT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    correct_answers INT NOT NULL DEFAULT 0,
    wrong_answers INT NOT NULL DEFAULT 0,
    xp_earned INT NOT NULL DEFAULT 0,
    coins_earned INT NOT NULL DEFAULT 0,
    last_review_date BIGINT,
    next_review_date BIGINT,
    review_count INT NOT NULL DEFAULT 0,
    ease_factor REAL NOT NULL DEFAULT 2.5,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS achievements (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    achievement_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at BIGINT,
    progress INT NOT NULL DEFAULT 0,
    target INT NOT NULL DEFAULT 1,
    xp_reward INT NOT NULL DEFAULT 0,
    coins_reward INT NOT NULL DEFAULT 0,
    badge_url TEXT,
    created_at BIGINT NOT NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_words_lesson ON words(lesson_id);
CREATE INDEX IF NOT EXISTS idx_exercises_lesson ON exercises(lesson_id);
CREATE INDEX IF NOT EXISTS idx_progress_user ON user_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_progress_lesson ON user_progress(lesson_id);
CREATE INDEX IF NOT EXISTS idx_achievements_user ON achievements(user_id);
