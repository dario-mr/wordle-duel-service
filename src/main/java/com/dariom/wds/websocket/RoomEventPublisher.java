package com.dariom.wds.websocket;

import com.dariom.wds.websocket.model.RoomEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public void publish(String roomId, RoomEvent event) {
    log.info("Publishing {} for room id {}", event, roomId);
    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
  }
}
