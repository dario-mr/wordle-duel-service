# Improvement ideas

- If horizontal scaling is ever implemented, replace `RoomLockManager` with a proper locking
  strategy (row-level lock, row versioning, distributed lock, ...)
- increase test coverage
- scheduler to delete old rooms (and all related records)