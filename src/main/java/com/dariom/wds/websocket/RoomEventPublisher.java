package com.dariom.wds.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public RoomEventPublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void publish(String roomId, RoomEvent event) {
    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
  }
}
