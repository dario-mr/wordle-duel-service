package com.dariom.wds.service.room;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Provides per-room mutual exclusion by executing code under a lock scoped to a room id. Locks are
 * created on demand and released from the cache when no longer in use.
 */
@Component
public class RoomLockManager {

  private final ConcurrentHashMap<String, RoomLockEntry> locksByRoomId = new ConcurrentHashMap<>();

  public <T> T withRoomLock(String roomId, Supplier<T> supplier) {
    var entry = acquireEntry(roomId);
    entry.lock.lock();
    try {
      return supplier.get();
    } finally {
      entry.lock.unlock();
      releaseEntry(roomId, entry);
    }
  }

  long registeredLockCount() {
    return locksByRoomId.size();
  }

  private RoomLockEntry acquireEntry(String roomId) {
    return locksByRoomId.compute(roomId, (ignored, existing) -> {
      var entry = existing != null ? existing : new RoomLockEntry();
      entry.users.incrementAndGet();
      return entry;
    });
  }

  private void releaseEntry(String roomId, RoomLockEntry entry) {
    locksByRoomId.computeIfPresent(roomId, (ignored, existing) -> {
      if (existing != entry) {
        return existing;
      }
      return entry.users.decrementAndGet() == 0 ? null : existing;
    });
  }

  private static final class RoomLockEntry {

    final ReentrantLock lock = new ReentrantLock();
    final AtomicInteger users = new AtomicInteger();
  }
}
