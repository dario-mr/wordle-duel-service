package com.dariom.wds.service;

import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_CLOSED;
import static com.dariom.wds.domain.RoomStatus.CLOSED;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static com.dariom.wds.websocket.model.EventType.PLAYER_JOINED;
import static com.dariom.wds.websocket.model.EventType.ROOM_CREATED;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

  private final RoomJpaRepository roomJpaRepository;
  private final RoomLockManager roomLockManager;
  private final RoundLifecycleService roundLifecycleService;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public Room createRoom(Language language, String creatorPlayerId) {
    var room = new RoomEntity();
    room.setId(UUID.randomUUID().toString());
    room.setLanguage(language);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.addPlayer(creatorPlayerId);
    room.setPlayerScore(creatorPlayerId, 0);
    room.setCurrentRoundNumber(null);

    var saved = roomJpaRepository.save(room);

    applicationEventPublisher.publishEvent(new RoomEventToPublish(saved.getId(), new RoomEvent(
        ROOM_CREATED,
        new PlayerJoinedPayload(creatorPlayerId, saved.getSortedPlayerIds())
    )));

    return new Room(saved, null);
  }

  @Transactional
  public Room joinRoom(String roomId, String playerId) {
    return roomLockManager.withRoomLock(roomId, () -> {
      var room = roomJpaRepository.findWithPlayersAndScoresById(roomId)
          .orElseThrow(() -> new RoomNotFoundException(roomId));

      if (room.getStatus() == CLOSED) {
        throw new InvalidGuessException(ROOM_CLOSED, "Room <%s> is closed".formatted(roomId));
      }

      if (!room.getPlayerIds().contains(playerId) && room.getPlayerIds().size() >= 2) {
        throw new RoomFullException(roomId);
      }

      room.addPlayer(playerId);
      room.setPlayerScoreIfNotSet(playerId, 0); // don't reset score if player already in the room

      if (room.getPlayerIds().size() == 2) {
        room.setStatus(IN_PROGRESS);
        if (room.getCurrentRoundNumber() == null) {
          roundLifecycleService.startNewRound(room);
        }
      }

      var saved = roomJpaRepository.save(room);

      applicationEventPublisher.publishEvent(new RoomEventToPublish(saved.getId(), new RoomEvent(
          PLAYER_JOINED,
          new PlayerJoinedPayload(playerId, saved.getSortedPlayerIds())
      )));

      var currentRound = roundLifecycleService.getCurrentRoundOrNull(saved);
      return new Room(saved, currentRound);
    });
  }

  @Transactional(readOnly = true)
  public Room getRoom(String roomId) {
    var room = roomJpaRepository.findWithPlayersAndScoresById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));

    var currentRound = roundLifecycleService.getCurrentRoundOrNull(room);
    return new Room(room, currentRound);
  }

}
