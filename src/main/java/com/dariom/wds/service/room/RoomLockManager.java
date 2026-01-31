package com.dariom.wds.service.room;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.exception.RoomLockedException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

/**
 * Provides per-room mutual exclusion by executing code under a lock scoped to a room id.
 *
 * <p>The lock instance is obtained from a {@link LockRegistry} using a room-scoped key, then
 * acquired with a configurable timeout. This class does not manage any lock caching/eviction; that
 * behavior is delegated to the configured {@code LockRegistry} implementation.
 */
@Component
@RequiredArgsConstructor
public class RoomLockManager {

  private final LockRegistry lockRegistry;
  private final RoomLockProperties properties;

  public void withRoomLock(String roomId, Runnable runnable) {
    withRoomLock(roomId, () -> {
      runnable.run();
      return true;
    });
  }

  public <T> T withRoomLock(String roomId, Supplier<T> supplier) {
    var lock = lockRegistry.obtain("room:" + roomId);
    var acquired = false;

    try {
      acquired = lock.tryLock(properties.acquireTimeout().toMillis(), MILLISECONDS);
      if (!acquired) {
        throw new RoomLockedException(roomId);
      }

      return supplier.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RoomLockedException(roomId);
    } finally {
      if (acquired) {
        lock.unlock();
      }
    }
  }
}
