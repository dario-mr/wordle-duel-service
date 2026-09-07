package com.dariom.wds.service.room;

import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.MATCH_FINISHED;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.createdAtOn;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.firstPlayerFullNameSort;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.languageEquals;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.lastUpdatedAtOn;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.playerMatches;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.roomIdContains;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.roundsEquals;
import static com.dariom.wds.persistence.repository.jpa.RoomSpecifications.statusIn;
import static com.dariom.wds.service.room.RoomValidator.validateRoom;
import static com.dariom.wds.websocket.model.EventType.MATCH_RESTARTED;
import static com.dariom.wds.websocket.model.EventType.PLAYER_JOINED;
import static com.dariom.wds.websocket.model.EventType.ROOM_CREATED;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.dariom.wds.api.admin.dto.AdminPlayerDto;
import com.dariom.wds.api.admin.dto.AdminRoomDto;
import com.dariom.wds.api.admin.dto.AdminRoundDto;
import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomRounds;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomAccessDeniedException;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomPlayerEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.round.RoundService;
import com.dariom.wds.service.user.UserProfileService;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
  private final RoomMessageService roomMessageService;
  private final DomainMapper domainMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final UserProfileService userProfileService;
  private final RoundJpaRepository roundJpaRepository;

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

  @Transactional(readOnly = true)
  public Page<AdminRoomDto> listRoomsForAdmin(Pageable pageable, Set<RoomStatus> statuses,
      Language language, RoomRounds rounds, String roomId, String playerSearch,
      LocalDate createdAt, LocalDate lastUpdatedAt) {
    var matchingPlayerIds = userProfileService.findPlayerIdsBySearch(playerSearch);
    var spec = Specification.allOf(
        statusIn(statuses),
        languageEquals(language),
        roundsEquals(rounds),
        roomIdContains(roomId),
        playerMatches(playerSearch, matchingPlayerIds),
        createdAtOn(createdAt),
        lastUpdatedAtOn(lastUpdatedAt)
    );
    var playerSort = pageable.getSort().getOrderFor("players");
    if (playerSort != null) {
      spec = Specification.allOf(spec, firstPlayerFullNameSort(playerSort.getDirection()));
      pageable = withoutSort(pageable, "players");
    }
    var page = roomRepository.findAll(spec, pageable);
    var roomIds = page.getContent().stream().map(RoomEntity::getId).toList();
    if (roomIds.isEmpty()) {
      return page.map(room -> toAdminRoomDto(room, room, List.of(), Map.of()));
    }

    var roomWithPlayersById = roomRepository.findWithPlayersByIds(roomIds).stream()
        .collect(toMap(RoomEntity::getId, identity()));
    var roundsByRoomId = roundJpaRepository.findWithPlayerStatusesByRoomIds(roomIds).stream()
        .collect(groupingBy(round -> round.getRoom().getId()));
    var playerIds = roomWithPlayersById.values().stream()
        .flatMap(room -> room.getRoomPlayers().stream())
        .map(RoomPlayerEntity::getPlayerId)
        .collect(toSet());
    var displayNamePerPlayer = userProfileService.getDisplayNamePerPlayer(playerIds);

    return page.map(room -> toAdminRoomDto(
        room,
        roomWithPlayersById.get(room.getId()),
        roundsByRoomId.getOrDefault(room.getId(), List.of()),
        displayNamePerPlayer
    ));
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
    roomMessageService.clearMessages(roomId);
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

  private AdminRoomDto toAdminRoomDto(RoomEntity room, RoomEntity roomWithPlayers,
      List<RoundEntity> roundEntities,
      Map<String, String> displayNamePerPlayer) {
    var players = roomWithPlayers.getRoomPlayers().stream()
        .sorted(comparing(RoomPlayerEntity::getPlayerId))
        .map(player -> new AdminPlayerDto(
            player.getPlayerId(),
            player.getWins(),
            player.getMatchScore(),
            displayNamePerPlayer.get(player.getPlayerId()),
            player.getCurrentRoundNumber()
        ))
        .toList();
    var rounds = roundEntities.stream()
        .sorted(comparingInt(RoundEntity::getRoundNumber))
        .map(round -> new AdminRoundDto(
            round.getRoundNumber(),
            round.getTargetWord(),
            round.getRoundStatus(),
            Map.copyOf(round.getStatusByPlayerId())
        ))
        .toList();
    return new AdminRoomDto(
        room.getId(),
        room.getLanguage(),
        room.getConfiguredRounds(),
        room.getStatus(),
        players,
        rounds,
        room.getCreatedAt(),
        room.getLastUpdatedAt()
    );
  }

  private Pageable withoutSort(Pageable pageable, String property) {
    var orders = pageable.getSort().stream()
        .filter(order -> !order.getProperty().equals(property))
        .toList();
    var sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
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
