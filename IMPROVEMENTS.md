# Improvement ideas

- If horizontal scaling is ever implemented, replace `RoomLockManager` with a proper locking
  strategy (row-level lock, row versioning, distributed lock, ...)
- increase test coverage
- scheduler to delete unused rooms (and all related records), e.g. based on `last_updated_at` or
  other inactivity indicator