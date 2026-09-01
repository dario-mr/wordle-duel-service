package com.dariom.wds.service.room;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomRounds.FIVE;
import static com.dariom.wds.domain.RoomStatus.CLOSED;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static com.dariom.wds.domain.RoundStatus.PLAYING;
import static com.dariom.wds.websocket.model.EventType.PLAYER_JOINED;
import static com.dariom.wds.websocket.model.EventType.ROOM_CREATED;
import static com.dariom.wds.websocket.model.EventType.REMATCH_STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.exception.RoomAccessDeniedException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.round.RoundService;
import com.dariom.wds.service.user.UserProfileService;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RematchStartedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  private final RoomLockProperties lockProperties = new RoomLockProperties(
      Duration.ofSeconds(3)
  );
  private final DomainMapper domainMapper = new DomainMapper();

  @Mock
  private RoomRepository roomRepository;
  @Mock
  private RoundService roundService;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private UserProfileService userProfileService;

  private RoomService roomService;

  @BeforeEach
  void setUp() {
    roomService = new RoomService(
        roomRepository,
        lockProperties,
        roundService,
        domainMapper,
        eventPublisher,
        userProfileService
    );
  }

  @Test
  void createRoom_validInput_returnsPersistedRoomAndPublishesRoomCreatedEvent() {
    // Arrange
    when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var room = roomService.createRoom(IT, FIVE, "p1");

    // Assert
    assertThat(room.id()).isNotBlank();
    assertThat(room.rounds()).isEqualTo(FIVE);
    assertThat(room.status()).isEqualTo(WAITING_FOR_PLAYERS);
    assertThat(room.players()).extracting(Player::id).containsExactly("p1");
    assertThat(room.players()).singleElement().satisfies(p -> assertThat(p.score()).isEqualTo(0));

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    var published = eventCaptor.getValue();
    assertThat(published.roomId()).isEqualTo(room.id());

    RoomEvent event = published.event();
    assertThat(event.type()).isEqualTo(ROOM_CREATED);
    assertThat(event.payload()).isInstanceOf(PlayerJoinedPayload.class);

    var payload = (PlayerJoinedPayload) event.payload();
    assertThat(payload.playerId()).isEqualTo("p1");
    assertThat(payload.players()).containsExactly("p1");
  }

  @Test
  void getRoom_roomNotFound_throwsRoomNotFoundException() {
    // Arrange
    when(roomRepository.findWithPlayersById(anyString())).thenThrow(
        new RoomNotFoundException("room-1"));

    // Act
    var thrown = catchThrowable(() -> roomService.getRoom("room-1", "player-1"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomNotFoundException.class)
        .hasMessageContaining("room-1");

    verify(roomRepository).findWithPlayersById("room-1");
    verifyNoInteractions(roundService, eventPublisher);
  }

  @Test
  void joinRoom_roomNotFound_throwsRoomNotFoundException() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any()))
        .thenThrow(new RoomNotFoundException("room-1"));

    // Act
    var thrown = catchThrowable(() -> roomService.joinRoom("room-1", "p2"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomNotFoundException.class)
        .hasMessageContaining("room-1");

    verify(roomRepository).findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout());
    verify(roomRepository, never()).save(any(RoomEntity.class));
    verifyNoInteractions(roundService, eventPublisher);
  }

  @Test
  void joinRoom_roomLocked_throwsRoomLockedException() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any()))
        .thenThrow(new PessimisticLockingFailureException("locked"));

    // Act
    var thrown = catchThrowable(() -> roomService.joinRoom("room-1", "p2"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomLockedException.class)
        .hasMessageContaining("room-1");
  }

  @Test
  void joinRoom_roomFull_throwsRoomFullException() {
    // Arrange
    var room = waitingRoom("room-1", "p1");
    room.addPlayer("p2");
    room.setPlayerScore("p2", 0);

    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(room);

    // Act
    var thrown = catchThrowable(() -> roomService.joinRoom("room-1", "p3"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomFullException.class)
        .hasMessageContaining("room-1");

    verify(roomRepository).findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout());
    verifyNoMoreInteractions(roomRepository);
    verifyNoInteractions(roundService, eventPublisher);
  }

  @Test
  void joinRoom_secondPlayerJoins_returnsInProgressRoom() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");

    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(entity);
    when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    when(roundService.getCurrentRound("room-1", "p2")).thenReturn(
        Optional.of(new Round(1, 6, List.of(), null, PLAYING, null)));

    // Act
    var room = roomService.joinRoom("room-1", "p2");

    // Assert
    assertThat(room.status()).isEqualTo(IN_PROGRESS);
    assertThat(room.players()).extracting(Player::id).containsExactly("p1", "p2");
    assertThat(room.players())
        .filteredOn(p -> p.id().equals("p2"))
        .singleElement()
        .satisfies(p -> assertThat(p.score()).isEqualTo(0));
    assertThat(room.currentRound()).isNotNull();

    verify(roundService).startNewRound("room-1");

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    var published = eventCaptor.getValue();
    assertThat(published.roomId()).isEqualTo("room-1");
    assertThat(published.event().type()).isEqualTo(PLAYER_JOINED);
    assertThat(published.event().payload())
        .isEqualTo(new PlayerJoinedPayload("p2", List.of("p1", "p2")));
  }

  @Test
  void joinRoom_playerAlreadyInRoom_returnsRoomWithoutResettingScoreOrStartingRound() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");
    entity.setPlayerScore("p1", 5);

    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(entity);
    when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(roundService.getCurrentRound(anyString(), any())).thenReturn(Optional.empty());

    // Act
    var room = roomService.joinRoom("room-1", "p1");

    // Assert
    assertThat(room.players()).extracting(Player::id).containsExactly("p1");
    assertThat(room.players()).singleElement().satisfies(p -> assertThat(p.score()).isEqualTo(5));

    verify(roundService, never()).startNewRound(anyString());
  }

  @Test
  void requestRematch_firstPlayerVotes_recordsVoteWithoutCreatingRoom() {
    // Arrange
    var source = closedRoom("room-1");
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(source);
    when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var result = roomService.requestRematch("room-1", "p1");

    // Assert
    assertThat(result).isEmpty();
    assertThat(rematchRequested(source, "p1")).isTrue();
    assertThat(rematchRequested(source, "p2")).isFalse();
    verify(roomRepository).save(source);
    verify(roundService, never()).startNewRound(anyString());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void requestRematch_secondPlayerVotes_createsAndPublishesOneRematch() {
    // Arrange
    var source = closedRoom("room-1");
    source.markRematchRequested("p1");
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(source);
    when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    // Act
    var result = roomService.requestRematch("room-1", "p2");

    // Assert
    var rematchRoomId = result.orElseThrow();
    assertThat(rematchRoomId).isNotEqualTo("room-1");
    assertThat(source.getRematchRoomId()).isEqualTo(rematchRoomId);
    assertThat(source.allPlayersRequestedRematch()).isTrue();

    var savedRooms = ArgumentCaptor.forClass(RoomEntity.class);
    verify(roomRepository, times(2)).save(savedRooms.capture());
    var rematchRoom = savedRooms.getAllValues().get(0);
    assertThat(rematchRoom.getId()).isEqualTo(rematchRoomId);
    assertThat(rematchRoom.getLanguage()).isEqualTo(IT);
    assertThat(rematchRoom.getConfiguredRounds()).isEqualTo(FIVE);
    assertThat(rematchRoom.getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(rematchRoom.getPlayerIds()).containsExactlyInAnyOrder("p1", "p2");
    assertThat(rematchRoom.getScoresByPlayerId())
        .containsExactlyInAnyOrderEntriesOf(Map.of("p1", 0, "p2", 0));
    assertThat(savedRooms.getAllValues().get(1)).isSameAs(source);

    verify(roundService).startNewRound(rematchRoomId);
    verify(eventPublisher).publishEvent(new RoomEventToPublish("room-1", new RoomEvent(
        REMATCH_STARTED,
        new RematchStartedPayload(rematchRoomId)
    )));
  }

  @Test
  void requestRematch_afterCompletion_returnsExistingRematchWithoutCreatingAnother() {
    // Arrange
    var source = closedRoom("room-1");
    source.setRematchRoomId("room-2");
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(source);

    // Act
    var result = roomService.requestRematch("room-1", "p1");

    // Assert
    assertThat(result).contains("room-2");
    verify(roomRepository, never()).save(any(RoomEntity.class));
    verifyNoInteractions(roundService, eventPublisher);
  }

  @Test
  void requestRematch_nonPlayer_throwsPlayerNotInRoomException() {
    // Arrange
    var source = closedRoom("room-1");
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(source);

    // Act
    var thrown = catchThrowable(() -> roomService.requestRematch("room-1", "p3"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(PlayerNotInRoomException.class)
        .hasMessage("Player <p3> is not in room <room-1>");
    verify(roomRepository, never()).save(any(RoomEntity.class));
    verifyNoInteractions(roundService, eventPublisher);
  }

  @Test
  void requestRematch_nonClosedRoom_throwsRoomNotReadyException() {
    // Arrange
    var source = waitingRoom("room-1", "p1");
    source.addPlayer("p2");
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(source);

    // Act
    var thrown = catchThrowable(() -> roomService.requestRematch("room-1", "p1"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomNotReadyException.class)
        .hasMessage("Room <room-1> is not ready: required status CLOSED, got WAITING_FOR_PLAYERS");
    verify(roomRepository, never()).save(any(RoomEntity.class));
    verifyNoInteractions(roundService, eventPublisher);
  }

  @Test
  void requestRematch_roomLocked_throwsRoomLockedException() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any()))
        .thenThrow(new PessimisticLockingFailureException("locked"));

    // Act
    var thrown = catchThrowable(() -> roomService.requestRematch("room-1", "p1"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomLockedException.class)
        .hasMessageContaining("room-1");
  }

  @Test
  void getRoom_currentRoundIsNull_returnsRoomWithoutCurrentRound() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");

    when(roomRepository.findWithPlayersById(anyString())).thenReturn(
        entity);
    when(roundService.getCurrentRound(anyString(), any())).thenReturn(Optional.empty());

    // Act
    var room = roomService.getRoom("room-1", "player-1");

    // Assert
    assertThat(room.status()).isEqualTo(WAITING_FOR_PLAYERS);
    assertThat(room.players()).extracting(Player::id).containsExactly("p1");
    assertThat(room.players()).singleElement().satisfies(p -> assertThat(p.score()).isEqualTo(0));
    assertThat(room.id()).isEqualTo("room-1");
    assertThat(room.language()).isEqualTo(IT);
    assertThat(room.currentRound()).isNull();

    verify(roundService).getCurrentRound("room-1", "player-1");
  }

  @Test
  void getRoom_currentRoundExists_returnsRoomWithCurrentRound() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");
    entity.setStatus(IN_PROGRESS);
    entity.findRoomPlayer("p1").orElseThrow().setCurrentRoundNumber(1);

    var currentRound = new Round(1, 6, List.of(), null, PLAYING, null);

    when(roomRepository.findWithPlayersById(anyString()))
        .thenReturn(entity);
    when(roundService.getCurrentRound(anyString(), any()))
        .thenReturn(Optional.of(currentRound));

    // Act
    var room = roomService.getRoom("room-1", "player-1");

    // Assert
    assertThat(room.status()).isEqualTo(IN_PROGRESS);
    assertThat(room.currentRound()).isEqualTo(currentRound);

    verify(roundService).getCurrentRound("room-1", "player-1");
  }

  @Test
  void getRoom_roomNotFull_allowsNonMemberInspection() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");

    when(roomRepository.findWithPlayersById(anyString()))
        .thenReturn(entity);
    when(roundService.getCurrentRound(anyString(), any())).thenReturn(Optional.empty());

    // Act
    var room = roomService.getRoom("room-1", "p2");

    // Assert
    assertThat(room.id()).isEqualTo("room-1");
    assertThat(room.players()).extracting(Player::id).containsExactly("p1");

    verify(roundService).getCurrentRound("room-1", "p2");
  }

  @Test
  void getRoom_roomFullAndRequestingPlayerNotInRoom_throwsRoomAccessDeniedException() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");
    entity.addPlayer("p2");
    entity.setPlayerScore("p2", 0);

    when(roomRepository.findWithPlayersById(anyString()))
        .thenReturn(entity);

    // Act
    var thrown = catchThrowable(() -> roomService.getRoom("room-1", "p3"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomAccessDeniedException.class)
        .hasMessage("Player <p3> cannot inspect room <room-1>");

    verify(roomRepository).findWithPlayersById("room-1");
    verifyNoInteractions(roundService, userProfileService, eventPublisher);
  }

  @Test
  void getRoom_roomFullAndRequestingPlayerInRoom_returnsRoom() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");
    entity.addPlayer("p2");
    entity.setPlayerScore("p2", 0);

    when(roomRepository.findWithPlayersById(anyString()))
        .thenReturn(entity);
    when(roundService.getCurrentRound(anyString(), any())).thenReturn(Optional.empty());

    // Act
    var room = roomService.getRoom("room-1", "p1");

    // Assert
    assertThat(room.id()).isEqualTo("room-1");
    assertThat(room.players()).extracting(Player::id).containsExactly("p1", "p2");

    verify(roundService).getCurrentRound("room-1", "p1");
  }

  @Test
  void listRoomsForPlayer_roomsExist_returnsRoomsWithCurrentRoundsWhenPresent() {
    // Arrange
    var waitingRoom = waitingRoom("room-1", "p1");

    var inProgressRoom = waitingRoom("room-2", "p1");
    inProgressRoom.setStatus(IN_PROGRESS);
    inProgressRoom.findRoomPlayer("p1").orElseThrow().setCurrentRoundNumber(1);

    var currentRound = new Round(1, 6, List.of(), null, PLAYING, null);

    when(roomRepository.findWithPlayersByPlayerId("p1"))
        .thenReturn(List.of(waitingRoom, inProgressRoom));
    when(roundService.getCurrentRoundsByRoomIds(List.of("room-1", "room-2"), "p1"))
        .thenReturn(Map.of("room-2", currentRound));
    when(userProfileService.getDisplayNamePerPlayer(any())).thenReturn(Map.of());

    // Act
    var rooms = roomService.listRoomsForPlayer("p1");

    // Assert
    assertThat(rooms).hasSize(2);
    assertThat(rooms)
        .extracting(Room::id)
        .containsExactly("room-1", "room-2");
    assertThat(rooms.get(0).currentRound()).isNull();
    assertThat(rooms.get(1).currentRound()).isEqualTo(currentRound);

    verify(roomRepository).findWithPlayersByPlayerId("p1");
    verify(roundService).getCurrentRoundsByRoomIds(List.of("room-1", "room-2"), "p1");
    verify(userProfileService, times(2)).getDisplayNamePerPlayer(Set.of("p1"));
    verifyNoMoreInteractions(roomRepository, roundService, userProfileService);
  }

  @Test
  void deleteInactiveRooms_cutoffProvided_deletesOldRooms() {
    // Arrange
    var cutoff = Instant.parse("2025-01-01T12:00:00Z");
    when(roomRepository.deleteInactive(cutoff)).thenReturn(3L);

    // Act
    var deleted = roomService.deleteInactiveRooms(cutoff);

    // Assert
    assertThat(deleted).isEqualTo(3L);
    verify(roomRepository).deleteInactive(cutoff);
  }

  @Test
  void deleteRoomById_roomExists_deletesRoom() {
    // Arrange
    var room = waitingRoom("room-1", "p1");
    when(roomRepository.findWithPlayersByIdForUpdate(eq("room-1"), any())).thenReturn(room);

    // Act
    roomService.deleteRoomById("room-1");

    // Assert
    verify(roomRepository).findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout());
    verify(roomRepository).delete(room);
  }

  @Test
  void deleteRoomById_roomMissing_throwsRoomNotFoundException() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any()))
        .thenThrow(new RoomNotFoundException("room-1"));

    // Act
    var thrown = catchThrowable(() -> roomService.deleteRoomById("room-1"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomNotFoundException.class)
        .hasMessage("Room <room-1> not found");
    verify(roomRepository).findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout());
    verify(roomRepository, never()).delete(any());
  }

  private static RoomEntity waitingRoom(String roomId, String playerId) {
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.addPlayer(playerId);
    room.setPlayerScore(playerId, 0);

    return room;
  }

  private static RoomEntity closedRoom(String roomId) {
    var room = waitingRoom(roomId, "p1");
    room.setConfiguredRounds(FIVE);
    room.addPlayer("p2");
    room.setStatus(CLOSED);
    room.setPlayerScore("p2", 3);
    room.setPlayerScore("p1", 7);
    return room;
  }

  private static boolean rematchRequested(RoomEntity room, String playerId) {
    return room.getRoomPlayers().stream()
        .filter(player -> player.getPlayerId().equals(playerId))
        .findFirst()
        .map(player -> player.isRematchRequested())
        .orElse(false);
  }

}
