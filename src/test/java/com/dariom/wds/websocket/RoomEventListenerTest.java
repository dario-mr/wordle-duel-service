package com.dariom.wds.websocket;

import static org.mockito.Mockito.verify;

import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomEventListenerTest {

  @Mock
  private RedisRoomEventPublisher redisRoomEventPublisher;

  @InjectMocks
  private RoomEventListener roomEventListener;

  @Test
  void on_validEvent_publishesToRedis() {
    // Arrange
    var event = new RoomEvent(
        EventType.SCORES_UPDATED,
        new ScoresUpdatedPayload(java.util.Map.of())
    );
    var toPublish = new RoomEventToPublish("room-1", event);

    // Act
    roomEventListener.on(toPublish);

    // Assert
    verify(redisRoomEventPublisher).publish(toPublish);
  }
}
