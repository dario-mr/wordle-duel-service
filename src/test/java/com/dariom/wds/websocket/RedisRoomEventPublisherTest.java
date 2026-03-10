package com.dariom.wds.websocket;

import static com.dariom.wds.websocket.RedisRoomEventPublisher.ROOM_EVENTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisRoomEventPublisherTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private RedisRoomEventPublisher publisher;

  @Test
  void publish_validEvent_sendsToRedisChannel() throws Exception {
    // Arrange
    var event = new RoomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6));
    var toPublish = new RoomEventToPublish("room-1", event);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":true}");

    // Act
    publisher.publish(toPublish);

    // Assert
    verify(redisTemplate).convertAndSend(ROOM_EVENTS, "{\"json\":true}");
  }

  @Test
  void publish_serializationFails_doesNotPropagateException() throws Exception {
    // Arrange
    var event = new RoomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6));
    var toPublish = new RoomEventToPublish("room-1", event);
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("serialize error") {
        });

    // Act
    publisher.publish(toPublish);

    // Assert
    verify(redisTemplate, never()).convertAndSend(any(), any(Object.class));
  }

  @Test
  void publish_redisFails_doesNotPropagateException() throws Exception {
    // Arrange
    var event = new RoomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6));
    var toPublish = new RoomEventToPublish("room-1", event);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":true}");
    doThrow(new RuntimeException("redis down"))
        .when(redisTemplate).convertAndSend(any(), any(Object.class));

    // Act
    publisher.publish(toPublish);
  }
}
