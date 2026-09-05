package com.dariom.wds.service.room;

import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.MATCH_FINISHED;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static com.dariom.wds.service.room.RoomValidator.validateRoom;
import static com.dariom.wds.websocket.model.EventType.MATCH_RESTARTED;
import static com.dariom.wds.websocket.model.EventType.PLAYER_JOINED;
import static com.dariom.wds.websocket.model.EventType.ROOM_CREATED;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomRounds;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomAccessDeniedException;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.round.RoundService;
import com.dariom.wds.service.user.UserProfileService;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

  private static final int MAX_PLAYERS = 2;
  private static final int INITIAL_SCORE = 0;

  private final RoomRepository roomRepository;
  private final RoomLockProperties lockProperties;
  private final RoundService roundService;
  private final DomainMapper domainMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final UserProfileService userProfileService;

  @Transactional
  public Room createRoom(Language language, RoomRounds rounds, String creatorPlayerId) {
    var room = new RoomEntity();
    room.setId(UUID.randomUUID().toString());
    room.setLanguage(language);
    room.setConfiguredRounds(rounds);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.addPlayer(creatorPlayerId);
    room.setPlayerMatchScore(creatorPlayerId, INITIAL_SCORE);

    var saved = roomRepository.save(room);
    var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

    publishRoomEvent(saved.getId(), new RoomEvent(
        ROOM_CREATED,
        new PlayerJoinedPayload(creatorPlayerId, saved.getSortedPlayerIds())
    ));

    return domainMapper.toRoom(saved, null, displayNamePerPlayer);
  }

  @Transactional
  public Room joinRoom(String roomId, String joiningPlayerId) {
    try {
      return joinRoomInTransaction(roomId, joiningPlayerId);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  @Transactional
  public boolean requestRematch(String roomId, String playerId) {
    try {
      return requestRematchInTransaction(roomId, playerId);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  @Transactional(readOnly = true)
  public Room getRoom(String roomId, String requestingPlayerId) {
    var room = roomRepository.findWithPlayersById(roomId);
    ensurePlayerCanInspectRoom(room, requestingPlayerId);

    var currentRound = roundService.getCurrentRound(room.getId(), requestingPlayerId)
        .orElse(null);
    var displayNamePerPlayer = getDisplayNamePerPlayer(room);
    return domainMapper.toRoom(room, currentRound, displayNamePerPlayer);
  }

  @Transactional(readOnly = true)
  public List<Room> listRoomsForPlayer(String playerId) {
    var rooms = roomRepository.findWithPlayersByPlayerId(playerId);
    var roomIds = rooms.stream().map(RoomEntity::getId).toList();
    var currentRoundPerRoomId = roundService.getCurrentRoundsByRoomIds(roomIds, playerId);

    return rooms.stream()
        .map(room -> {
          var currentRound = currentRoundPerRoomId.get(room.getId());
          var displayNamePerPlayer = getDisplayNamePerPlayer(room);
          return domainMapper.toRoom(room, currentRound, displayNamePerPlayer);
        })
        .toList();
  }

  @Transactional
  public long deleteInactiveRooms(Instant cutoff) {
    return roomRepository.deleteInactive(cutoff);
  }

  @Transactional
  public void deleteRoomById(String roomId) {
    try {
      var room = roomRepository.findWithPlayersByIdForUpdate(roomId,
          lockProperties.acquireTimeout());
      roomRepository.delete(room);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  private Room joinRoomInTransaction(String roomId, String joiningPlayerId) {
    var room = roomRepository.findWithPlayersByIdForUpdate(roomId, lockProperties.acquireTimeout());
    validateRoom(joiningPlayerId, domainMapper.toRoom(room, null, null), MAX_PLAYERS);
    var roomWasWaiting = room.getStatus() == WAITING_FOR_PLAYERS;
    var playerWasAlreadyInRoom = room.findRoomPlayer(joiningPlayerId).isPresent();

    addPlayerAndInitializeScore(room, joiningPlayerId);
    maybeStartRound(room);
    var savedRoom = roomRepository.save(room);
    if (roomWasWaiting && !playerWasAlreadyInRoom && savedRoom.getStatus() == IN_PROGRESS) {
      publishRoomEvent(savedRoom.getId(), new RoomEvent(
          PLAYER_JOINED,
          new PlayerJoinedPayload(joiningPlayerId, savedRoom.getSortedPlayerIds())
      ));
    }
    var currentRound = roundService.getCurrentRound(savedRoom.getId(), joiningPlayerId)
        .orElse(null);
    var displayNamePerPlayer = getDisplayNamePerPlayer(savedRoom);

    return domainMapper.toRoom(savedRoom, currentRound, displayNamePerPlayer);
  }

  private boolean requestRematchInTransaction(String roomId, String playerId) {
    var sourceRoom = roomRepository.findWithPlayersByIdForUpdate(roomId,
        lockProperties.acquireTimeout());

    if (!sourceRoom.getPlayerIds().contains(playerId)) {
      throw new PlayerNotInRoomException(playerId, roomId);
    }
    if (sourceRoom.getStatus() != MATCH_FINISHED) {
      throw new RoomNotReadyException(roomId, sourceRoom.getStatus(), MATCH_FINISHED);
    }

    sourceRoom.markRematchRequested(playerId);
    if (!sourceRoom.allPlayersRequestedRematch()) {
      roomRepository.save(sourceRoom);
      return false;
    }

    sourceRoom.resetForRematch();
    roomRepository.save(sourceRoom);
    roundService.startNewRound(roomId);
    publishRoomEvent(roomId, new RoomEvent(
        MATCH_RESTARTED,
        null
    ));

    return true;
  }

  private void addPlayerAndInitializeScore(RoomEntity room, String playerId) {
    room.addPlayer(playerId);
    // don't reset match score if player already in the room
    room.setPlayerMatchScoreIfNotSet(playerId, INITIAL_SCORE);
  }

  private void maybeStartRound(RoomEntity room) {
    if (room.getPlayerIds().size() != MAX_PLAYERS) {
      return;
    }

    room.setStatus(IN_PROGRESS);

    if (room.getRoomPlayers().stream().anyMatch(player -> player.getCurrentRoundNumber() != null)) {
      return;
    }

    roundService.startNewRound(room.getId());
  }

  private void publishRoomEvent(String roomId, RoomEvent roomEvent) {
    eventPublisher.publishEvent(new RoomEventToPublish(roomId, roomEvent));
  }

  private Map<String, String> getDisplayNamePerPlayer(RoomEntity room) {
    var playerIds = room.getPlayerIds();
    return userProfileService.getDisplayNamePerPlayer(playerIds);
  }

  private void ensurePlayerCanInspectRoom(RoomEntity room, String requestingPlayerId) {
    if (room.getRoomPlayers().size() < MAX_PLAYERS) {
      return;
    }

    var isPlayerInRoom = room.getRoomPlayers().stream()
        .anyMatch(player -> player.getPlayerId().equals(requestingPlayerId));
    if (!isPlayerInRoom) {
      throw new RoomAccessDeniedException(room.getId(), requestingPlayerId);
    }
  }
}
