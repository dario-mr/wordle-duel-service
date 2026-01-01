package com.dariom.wds.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoomEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public RoomEventPublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void publish(String roomId, RoomEvent event) {
    log.info("Publishing {} for room id {}", event, roomId);
    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
  }
}
