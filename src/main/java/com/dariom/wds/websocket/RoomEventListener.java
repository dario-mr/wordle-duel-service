package com.dariom.wds.websocket;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.dariom.wds.websocket.model.RoomEventToPublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link RoomEventToPublish} application events and relays them to Redis Pub/Sub.
 *
 * <p>Events are sent {@code AFTER_COMMIT} to ensure clients only receive notifications for state
 * that was actually persisted in the database.
 * <p>Publishing is best-effort: failures are logged and do not affect the already-committed
 * transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomEventListener {

  private final RedisRoomEventPublisher redisRoomEventPublisher;

  @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
  public void on(RoomEventToPublish roomEvent) {
    log.info("Publishing {} for room <{}>", roomEvent.event(), roomEvent.roomId());
    redisRoomEventPublisher.publish(roomEvent);
  }
}
