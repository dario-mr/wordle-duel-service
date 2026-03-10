package com.dariom.wds.websocket;

import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisRoomEventPublisher {

  public static final String ROOM_EVENTS = "room-events";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public RedisRoomEventPublisher(
      StringRedisTemplate redisTemplate,
      @Qualifier("redisEventObjectMapper") ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public void publish(RoomEventToPublish roomEvent) {
    try {
      var json = objectMapper.writeValueAsString(roomEvent);
      redisTemplate.convertAndSend(ROOM_EVENTS, json);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize room event: roomId=<{}>, event={}", roomEvent.roomId(),
          roomEvent.event(), e);
    } catch (Exception e) {
      log.error("Failed to publish room event to Redis: roomId=<{}>, event={}", roomEvent.roomId(),
          roomEvent.event(), e);
    }
  }
}
