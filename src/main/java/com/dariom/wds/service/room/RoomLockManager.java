package com.dariom.wds.service.room;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

  private static final int INITIAL_CACHE_CAPACITY = 128;

  private final Cache<String, RoomLockEntry> locksByRoomId = Caffeine.newBuilder()
      .initialCapacity(INITIAL_CACHE_CAPACITY)
      .build();

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
    return locksByRoomId.estimatedSize();
  }

  private RoomLockEntry acquireEntry(String roomId) {
    return locksByRoomId.asMap().compute(roomId, (ignored, existing) -> {
      var entry = existing != null ? existing : new RoomLockEntry();
      entry.users.incrementAndGet();
      return entry;
    });
  }

  private void releaseEntry(String roomId, RoomLockEntry entry) {
    locksByRoomId.asMap().computeIfPresent(roomId, (ignored, existing) -> {
      if (existing != entry) {
        return existing;
      }

      if (entry.users.decrementAndGet() == 0) {
        return null;
      }

      return existing;
    });
  }

  private static final class RoomLockEntry {

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger users = new AtomicInteger(0);

    private RoomLockEntry() {
    }
  }
}
