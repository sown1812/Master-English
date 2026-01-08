# Firebase Realtime Database Schema

This app replaces the backend with Firebase Realtime Database.

## Public content

- `content/lessons/{lessonId}`
  - Lesson metadata (title, description, order, rewards).
- `content/words/{lessonId}/{wordId}`
  - Vocabulary for each lesson.
- `content/exercises/{lessonId}/{exerciseId}`
  - Exercises for each lesson.

## User data (per Firebase UID)

- `users/{uid}/profile`
  - User profile snapshot (XP, coins, streak, etc).
- `users/{uid}/progress/{lessonId}`
  - Lesson progress entries.
- `users/{uid}/achievements/{achievementType}`
  - Achievement progress and unlock state.
- `users/{uid}/gamestate/boosters/{boosterKey}` -> `true|false`
- `users/{uid}/gamestate/quests/{questKey}` -> `true|false`

## Notes

- Content is read-only for clients; seed it once with admin credentials.
- Debug builds can optionally seed from local data if rules allow it.
- User nodes are read/write by their own UID.
- The client merges by `updatedAt` for profile and progress.
