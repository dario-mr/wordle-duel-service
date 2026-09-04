package com.dariom.wds.service.room;

import static com.dariom.wds.websocket.model.EventType.ROOM_MESSAGE_SENT;

import com.dariom.wds.api.v1.dto.RoomMessageDto;
import com.dariom.wds.api.v1.dto.RoomMessagesDto;
import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.RoomMessagePreset;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoomMessageLimitReachedException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomMessageEntity;
import com.dariom.wds.persistence.entity.RoomPlayerEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.jpa.RoomMessageJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoomMessagePayload;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomMessageService {

  private static final int REQUIRED_PLAYER_COUNT = 2;
  private static final int MAX_CONSECUTIVE_MESSAGES = 3;

  private final RoomRepository roomRepository;
  private final RoomLockProperties lockProperties;
  private final RoomMessageJpaRepository roomMessageJpaRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public RoomMessagesDto listMessages(String roomId, String playerId) {
    var room = roomRepository.findWithPlayersById(roomId);
    requireChatReady(room, roomId, playerId);
    return toMessagesDto(roomMessageJpaRepository.findByRoomIdOrderByCreatedAtAscIdAsc(roomId),
        room.findRoomPlayer(playerId).orElseThrow());
  }

  @Transactional
  public RoomMessagesDto markMessagesRead(String roomId, String playerId) {
    try {
      var room = roomRepository.findWithPlayersByIdForUpdate(roomId, lockProperties.acquireTimeout());
      requireChatReady(room, roomId, playerId);
      var messages = roomMessageJpaRepository.findByRoomIdOrderByCreatedAtAscIdAsc(roomId);
      var player = room.findRoomPlayer(playerId).orElseThrow();
      if (!messages.isEmpty()) {
        player.setLastReadMessageId(messages.getLast().getId());
      }
      return toMessagesDto(messages, player);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  @Transactional
  public RoomMessageDto sendMessage(String roomId, String playerId, RoomMessagePreset preset) {
    try {
      return sendMessageInTransaction(roomId, playerId, preset);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  private RoomMessageDto sendMessageInTransaction(String roomId, String playerId,
      RoomMessagePreset preset) {
    var room = roomRepository.findWithPlayersByIdForUpdate(roomId, lockProperties.acquireTimeout());
    requireChatReady(room, roomId, playerId);

    var recentMessages = roomMessageJpaRepository
        .findTop3ByRoomIdOrderByCreatedAtDescIdDesc(roomId);
    if (recentMessages.size() == MAX_CONSECUTIVE_MESSAGES
        && recentMessages.stream()
        .allMatch(message -> playerId.equals(message.getSenderPlayerId()))) {
      throw new RoomMessageLimitReachedException(playerId, roomId);
    }

    var message = new RoomMessageEntity();
    message.setRoomId(roomId);
    message.setSenderPlayerId(playerId);
    message.setPreset(preset);
    message.setCreatedAt(Instant.now());
    var saved = roomMessageJpaRepository.save(message);
    var dto = toDto(saved);

    eventPublisher.publishEvent(new RoomEventToPublish(roomId, new RoomEvent(
        ROOM_MESSAGE_SENT,
        new RoomMessagePayload(dto.id(), dto.senderPlayerId(), dto.preset(), dto.createdAt())
    )));
    return dto;
  }

  private void requireChatReady(RoomEntity room, String roomId, String playerId) {
    if (!room.getPlayerIds().contains(playerId)) {
      throw new PlayerNotInRoomException(playerId, roomId);
    }
    if (room.getPlayerIds().size() < REQUIRED_PLAYER_COUNT) {
      throw new RoomNotReadyException(roomId, room.getPlayerIds().size());
    }
  }

  private static RoomMessageDto toDto(RoomMessageEntity message) {
    return new RoomMessageDto(
        message.getId(),
        message.getSenderPlayerId(),
        message.getPreset(),
        message.getCreatedAt()
    );
  }

  private static RoomMessagesDto toMessagesDto(List<RoomMessageEntity> messages,
      RoomPlayerEntity player) {
    var unreadCount = messages.stream()
        .filter(message -> !player.getPlayerId().equals(message.getSenderPlayerId()))
        .filter(message -> message.getId() > player.getLastReadMessageId())
        .count();
    return new RoomMessagesDto(messages.stream().map(RoomMessageService::toDto).toList(), unreadCount);
  }
}
