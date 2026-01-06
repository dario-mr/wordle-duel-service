package com.dariom.wds.websocket;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RoomEventListenerTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private RoomEventListener roomEventListener;

  @Test
  void on_validEvent_sendsToRoomTopic() {
    // Arrange
    var event = new RoomEvent(
        EventType.PLAYER_JOINED,
        new PlayerJoinedPayload("p1", List.of("p1"))
    );
    var toPublish = new RoomEventToPublish("room-1", event);

    // Act
    roomEventListener.on(toPublish);

    // Assert
    verify(messagingTemplate).convertAndSend("/topic/rooms/room-1", event);
  }

  @Test
  void on_messagingThrows_doesNotPropagateException() {
    // Arrange
    var event = new RoomEvent(
        EventType.PLAYER_JOINED,
        new PlayerJoinedPayload("p1", List.of("p1"))
    );
    var toPublish = new RoomEventToPublish("room-1", event);

    doThrow(new RuntimeException("boom"))
        .when(messagingTemplate)
        .convertAndSend("/topic/rooms/room-1", event);

    // Act
    roomEventListener.on(toPublish);
  }

}
