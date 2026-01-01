package com.dariom.wds.service;

import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_PLAYER_ID;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_CLOSED;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.websocket.PlayerJoinedPayload;
import com.dariom.wds.websocket.RoomEvent;
import com.dariom.wds.websocket.RoomEventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

  private final RoomJpaRepository roomJpaRepository;
  private final RoomLockManager roomLockManager;
  private final RoundLifecycleService roundLifecycleService;
  private final RoomEventPublisher eventPublisher;

  @Transactional
  public RoomEntity createRoom(Language language, String creatorPlayerId) {
    var normalizedPlayerId = normalizePlayerId(creatorPlayerId);

    var room = new RoomEntity();
    room.setId(UUID.randomUUID().toString());
    room.setLanguage(language == null ? Language.IT : language);
    room.setStatus(RoomStatus.WAITING_FOR_PLAYERS);
    room.getPlayerIds().add(normalizedPlayerId);
    room.getScoresByPlayerId().put(normalizedPlayerId, 0);
    room.setCurrentRoundNumber(null);

    var saved = roomJpaRepository.save(room);
    eventPublisher.publish(saved.getId(), new RoomEvent(
        "ROOM_CREATED",
        new PlayerJoinedPayload(normalizedPlayerId, saved.getPlayerIds().stream().sorted().toList())
    ));
    return saved;
  }

  @Transactional
  public RoomEntity joinRoom(String roomId, String playerId) {
    var normalizedPlayerId = normalizePlayerId(playerId);

    return roomLockManager.withRoomLock(roomId, () -> {
      var room = roomJpaRepository.findWithDetailsById(roomId)
          .orElseThrow(() -> new RoomNotFoundException(roomId));

      if (room.getStatus() == RoomStatus.CLOSED) {
        throw new InvalidGuessException(ROOM_CLOSED, "Room is closed: " + roomId);
      }

      if (!room.getPlayerIds().contains(normalizedPlayerId) && room.getPlayerIds().size() >= 2) {
        throw new RoomFullException(roomId);
      }

      room.getPlayerIds().add(normalizedPlayerId);
      room.getScoresByPlayerId().putIfAbsent(normalizedPlayerId, 0);

      if (room.getPlayerIds().size() == 2) {
        room.setStatus(RoomStatus.IN_PROGRESS);
        if (room.getCurrentRoundNumber() == null) {
          roundLifecycleService.startNewRound(room);
        }
      }

      var saved = roomJpaRepository.save(room);
      eventPublisher.publish(saved.getId(), new RoomEvent(
          "PLAYER_JOINED",
          new PlayerJoinedPayload(normalizedPlayerId,
              saved.getPlayerIds().stream().sorted().toList())
      ));
      return saved;
    });
  }

  @Transactional(readOnly = true)
  public RoomEntity getRoom(String roomId) {
    return roomJpaRepository.findWithDetailsById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));
  }

  private static String normalizePlayerId(String playerId) {
    if (playerId == null || playerId.trim().isEmpty()) {
      throw new InvalidGuessException(INVALID_PLAYER_ID, "playerId is required");
    }
    return playerId.trim();
  }
}
