package com.dariom.wds.service.room;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static com.dariom.wds.websocket.model.EventType.PLAYER_JOINED;
import static com.dariom.wds.websocket.model.EventType.ROOM_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.domain.Round;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.NoOpTransactionManager;
import com.dariom.wds.service.round.RoundService;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  private final RoomLockManager roomLockManager = new RoomLockManager();

  private final PlatformTransactionManager transactionManager = new NoOpTransactionManager();

  private final DomainMapper domainMapper = new DomainMapper();

  @Mock
  private RoomJpaRepository roomJpaRepository;

  @Mock
  private RoundService roundService;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  private RoomService roomService;

  @BeforeEach
  void setUp() {
    roomService = new RoomService(
        roomJpaRepository,
        roomLockManager,
        transactionManager,
        roundService,
        domainMapper,
        applicationEventPublisher
    );
  }

  @Test
  void createRoom_validInput_returnsPersistedRoomAndPublishesRoomCreatedEvent() {
    // Arrange
    when(roomJpaRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var room = roomService.createRoom(IT, "p1");

    // Assert
    assertThat(room.id()).isNotBlank();
    assertThat(room.status()).isEqualTo(WAITING_FOR_PLAYERS);
    assertThat(room.players()).containsExactly("p1");
    assertThat(room.scoresByPlayerId()).containsEntry("p1", 0);

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

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
  void joinRoom_secondPlayerJoins_returnsInProgressRoomAndPublishesPlayerJoinedEvent() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(entity));
    when(roomJpaRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    when(roundService.startNewRound("room-1")).thenReturn(
        new Round(1, 6, Map.of(), Map.of(), false));

    // Act
    var room = roomService.joinRoom("room-1", "p2");

    // Assert
    assertThat(room.status()).isEqualTo(IN_PROGRESS);
    assertThat(room.players()).containsExactly("p1", "p2");
    assertThat(room.scoresByPlayerId()).containsEntry("p2", 0);
    assertThat(room.currentRound()).isNotNull();

    verify(roundService).startNewRound("room-1");

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

    var published = eventCaptor.getValue();
    assertThat(published.roomId()).isEqualTo("room-1");
    assertThat(published.event().type()).isEqualTo(PLAYER_JOINED);
  }

  @Test
  void joinRoom_playerAlreadyInRoom_returnsRoomWithoutResettingScoreOrStartingRound() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");
    entity.setPlayerScore("p1", 5);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(entity));
    when(roomJpaRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(roundService.getCurrentRound(anyString(), any())).thenReturn(Optional.empty());

    // Act
    var room = roomService.joinRoom("room-1", "p1");

    // Assert
    assertThat(room.players()).containsExactly("p1");
    assertThat(room.scoresByPlayerId()).containsEntry("p1", 5);

    verify(roundService, never()).startNewRound(anyString());
  }

  @Test
  void getRoom_currentRoundIsNull_returnsRoomWithoutCurrentRound() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(entity));
    when(roundService.getCurrentRound(anyString(), any())).thenReturn(Optional.empty());

    // Act
    var room = roomService.getRoom("room-1");

    // Assert
    assertThat(room.status()).isEqualTo(WAITING_FOR_PLAYERS);
    assertThat(room.players()).containsExactly("p1");
    assertThat(room.scoresByPlayerId()).containsEntry("p1", 0);
    assertThat(room.id()).isEqualTo("room-1");
    assertThat(room.language()).isEqualTo(IT);
    assertThat(room.currentRound()).isNull();

    verify(roundService).getCurrentRound("room-1", null);
  }

  @Test
  void getRoom_currentRoundExists_returnsRoomWithCurrentRound() {
    // Arrange
    var entity = waitingRoom("room-1", "p1");
    entity.setStatus(IN_PROGRESS);
    entity.setCurrentRoundNumber(1);

    var currentRound = new Round(1, 6, Map.of(), Map.of(), false);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString()))
        .thenReturn(Optional.of(entity));
    when(roundService.getCurrentRound(anyString(), any()))
        .thenReturn(Optional.of(currentRound));

    // Act
    var room = roomService.getRoom("room-1");

    // Assert
    assertThat(room.status()).isEqualTo(IN_PROGRESS);
    assertThat(room.currentRound()).isEqualTo(currentRound);

    verify(roundService).getCurrentRound("room-1", 1);
  }

  private static RoomEntity waitingRoom(String roomId, String playerId) {
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.setCurrentRoundNumber(null);

    room.addPlayer(playerId);
    room.setPlayerScore(playerId, 0);

    return room;
  }

}
