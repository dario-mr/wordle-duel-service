package com.dariom.wds.websocket;

import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisRoomEventSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;

  public RedisRoomEventSubscriber(
      @Qualifier("redisEventObjectMapper") ObjectMapper objectMapper,
      SimpMessagingTemplate messagingTemplate) {
    this.objectMapper = objectMapper;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      var roomEvent = objectMapper.readValue(message.getBody(), RoomEventToPublish.class);
      var roomId = roomEvent.roomId();
      var event = roomEvent.event();

      log.info("Received {} for room <{}> from Redis", event, roomId);
      messagingTemplate.convertAndSend("/topic/rooms/%s".formatted(roomId), event);
    } catch (Exception e) {
      log.error("Failed to process room event from Redis", e);
    }
  }
}
