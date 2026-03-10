package com.dariom.wds.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RedisRoomEventSubscriberTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private RedisRoomEventSubscriber subscriber;

  @Test
  void onMessage_validMessage_sendsToStompTopic() throws Exception {
    // Arrange
    var event = new RoomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6));
    var roomEvent = new RoomEventToPublish("room-1", event);
    var json = "{\"roomId\":\"room-1\"}".getBytes();
    var message = new DefaultMessage(RedisRoomEventPublisher.ROOM_EVENTS.getBytes(), json);
    when(objectMapper.readValue(json, RoomEventToPublish.class)).thenReturn(roomEvent);

    // Act
    subscriber.onMessage(message, null);

    // Assert
    verify(messagingTemplate).convertAndSend("/topic/rooms/room-1", event);
  }

  @Test
  void onMessage_deserializationFails_doesNotPropagateException() throws Exception {
    // Arrange
    var json = "invalid".getBytes();
    var message = new DefaultMessage(RedisRoomEventPublisher.ROOM_EVENTS.getBytes(), json);
    when(objectMapper.readValue(json, RoomEventToPublish.class))
        .thenThrow(new RuntimeException("parse error"));

    // Act
    subscriber.onMessage(message, null);

    // Assert
    verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
  }

  @Test
  void onMessage_stompSendFails_doesNotPropagateException() throws Exception {
    // Arrange
    var event = new RoomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6));
    var roomEvent = new RoomEventToPublish("room-1", event);
    var json = "{\"roomId\":\"room-1\"}".getBytes();
    var message = new DefaultMessage(RedisRoomEventPublisher.ROOM_EVENTS.getBytes(), json);
    when(objectMapper.readValue(json, RoomEventToPublish.class)).thenReturn(roomEvent);
    doThrow(new RuntimeException("stomp error"))
        .when(messagingTemplate).convertAndSend(eq("/topic/rooms/room-1"), any(Object.class));

    // Act
    subscriber.onMessage(message, null);
  }
}
