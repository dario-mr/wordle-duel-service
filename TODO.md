# TODO

- If horizontal scaling is ever implemented, replace `RoomLockManager` with a proper locking
  strategy (row-level lock, row versioning, distributed lock, ...)
- consider pagination for `RoomController#listRooms` if too many rooms per user (unlikely)
- Check bug: "you are not a player", after clicking join
- Redis for room lock and jobs
- make redis logs work in alloy