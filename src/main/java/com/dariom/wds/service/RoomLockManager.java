package com.dariom.wds.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class RoomLockManager {

  private final ConcurrentHashMap<String, ReentrantLock> locksByRoomId = new ConcurrentHashMap<>();

  public <T> T withRoomLock(String roomId, Supplier<T> supplier) {
    var lock = locksByRoomId.computeIfAbsent(roomId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      return supplier.get();
    } finally {
      lock.unlock();
    }
  }

}
