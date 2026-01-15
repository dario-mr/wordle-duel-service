package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.ROUND_NOT_CURRENT;
import static com.dariom.wds.api.common.ErrorCode.ROUND_NOT_ENDED;
import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.READY;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.domain.RoundStatus.PLAYING;
import static com.dariom.wds.websocket.model.EventType.PLAYER_STATUS_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.NoOpTransactionManager;
import com.dariom.wds.service.room.RoomLockManager;
import com.dariom.wds.websocket.model.PlayerStatusUpdatedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

  private static final String ROOM_ID = "room-1";
  private static final String PLAYER_1 = "p1";
  private static final String PLAYER_2 = "p2";
  private static final int MAX_ATTEMPTS = 6;

  @Mock
  private RoomJpaRepository roomJpaRepository;
  @Mock
  private RoundJpaRepository roundJpaRepository;
  @Mock
  private RoundLifecycleService roundLifecycleService;
  @Mock
  private GuessSubmissionService guessSubmissionService;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private final RoomLockManager roomLockManager = new RoomLockManager();
  private final PlatformTransactionManager transactionManager = new NoOpTransactionManager();
  private final DomainMapper domainMapper = new DomainMapper();

  private RoundService service;

  @BeforeEach
  void setUp() {
    service = new RoundService(
        roomLockManager,
        transactionManager,
        roomJpaRepository,
        roundJpaRepository,
        domainMapper,
        roundLifecycleService,
        guessSubmissionService,
        eventPublisher
    );
  }

  @Test
  void getCurrentRound_currentRoundNumberIsNull_returnsEmpty() {
    // Act
    var result = service.getCurrentRound(ROOM_ID, null);

    // Assert
    assertThat(result).isEmpty();
    verify(roundJpaRepository, never()).findWithDetailsByRoomIdAndRoundNumber(anyString(),
        anyInt());
  }

  @Test
  void startNewRound_roomExists_returnsMappedRoundAndSavesRoom() {
    // Arrange
    var roomEntity = room(ROOM_ID);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));

    var roundEntity = round(1, PLAYING);
    when(roundLifecycleService.startNewRoundEntity(roomEntity)).thenReturn(roundEntity);

    // Act
    var result = service.startNewRound(ROOM_ID);

    // Assert
    assertThat(result).isEqualTo(domainMapper.toRound(roundEntity));
    verify(roomJpaRepository).findWithPlayersAndScoresById(ROOM_ID);
    verify(roomJpaRepository).save(roomEntity);
  }

  @Test
  void handleGuess_validInput_returnsMappedRoomAndPersistsRoom() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, PLAYER_1, PLAYER_2);

    var roundEntity = round(1, PLAYING);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundLifecycleService.ensureActiveRound(roomEntity)).thenReturn(roundEntity);
    when(guessSubmissionService.applyGuess(ROOM_ID, PLAYER_1, "pizza", roomEntity, roundEntity))
        .thenReturn(Optional.empty());
    when(roundLifecycleService.isRoundFinished(roomEntity, roundEntity)).thenReturn(false);

    // Act
    var result = service.handleGuess(ROOM_ID, PLAYER_1, "pizza");

    // Assert
    var expectedRound = domainMapper.toRound(roundEntity);
    var expectedRoom = domainMapper.toRoom(roomEntity, expectedRound);
    assertThat(result).isEqualTo(expectedRoom);

    verify(guessSubmissionService).applyGuess(ROOM_ID, PLAYER_1, "pizza", roomEntity, roundEntity);
    verify(roomJpaRepository).findWithPlayersAndScoresById(ROOM_ID);
    verify(roomJpaRepository).save(roomEntity);
  }

  @Test
  void handleGuess_playerStatusUpdatedButRoundNotFinished_publishesPlayerStatusUpdated() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, PLAYER_1, PLAYER_2);

    var roundEntity = round(1, PLAYING);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundLifecycleService.ensureActiveRound(roomEntity)).thenReturn(roundEntity);
    when(guessSubmissionService.applyGuess(ROOM_ID, PLAYER_1, "pizza", roomEntity, roundEntity))
        .thenReturn(Optional.of(WON));
    when(roundLifecycleService.isRoundFinished(roomEntity, roundEntity)).thenReturn(false);

    // Act
    service.handleGuess(ROOM_ID, PLAYER_1, "pizza");

    // Assert
    verify(eventPublisher).publishEvent(new RoomEventToPublish(ROOM_ID,
        new RoomEvent(PLAYER_STATUS_UPDATED, new PlayerStatusUpdatedPayload(WON))));
    verify(roundLifecycleService, never()).finishRound(roundEntity, roomEntity);
  }

  @Test
  void handleGuess_roundBecomesFinished_callsFinishRoundAndSuppressesStatusEvent() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, PLAYER_1, PLAYER_2);

    var roundEntity = round(1, PLAYING);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundLifecycleService.ensureActiveRound(roomEntity)).thenReturn(roundEntity);
    when(guessSubmissionService.applyGuess(ROOM_ID, PLAYER_1, "pizza", roomEntity, roundEntity))
        .thenReturn(Optional.of(LOST));
    when(roundLifecycleService.isRoundFinished(roomEntity, roundEntity)).thenReturn(true);

    // Act
    service.handleGuess(ROOM_ID, PLAYER_1, "pizza");

    // Assert
    verify(roundLifecycleService).finishRound(roundEntity, roomEntity);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void handleReady_playerNotInRoom_throwsPlayerNotInRoom() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, 1, PLAYER_1);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString()))
        .thenReturn(Optional.of(roomEntity));

    // Act / Assert
    assertThatThrownBy(() -> service.handleReady(ROOM_ID, PLAYER_2, 1))
        .isInstanceOf(PlayerNotInRoomException.class)
        .hasMessage("Player <p2> is not in room <room-1>");

    verify(roomJpaRepository).findWithPlayersAndScoresById(ROOM_ID);
    verifyNoMoreInteractions(roomJpaRepository);
    verifyNoInteractions(roundJpaRepository);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void handleReady_roundNumberIsNotCurrent_throwsRoundNotCurrent() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, 2, PLAYER_1, PLAYER_2);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString()))
        .thenReturn(Optional.of(roomEntity));

    // Act / Assert
    assertThatThrownBy(() -> service.handleReady(ROOM_ID, PLAYER_1, 1))
        .isInstanceOf(RoundException.class)
        .hasMessage("Round <1> is not the current round")
        .satisfies(ex -> assertThat(((RoundException) ex).getCode()).isEqualTo(ROUND_NOT_CURRENT));

    verify(roomJpaRepository).findWithPlayersAndScoresById(ROOM_ID);
    verify(roundJpaRepository, never()).findWithDetailsByRoomIdAndRoundNumber(
        anyString(), anyInt());
    verify(roomJpaRepository, never()).save(roomEntity);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void handleReady_roundNotEnded_throwsRoundNotEnded() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, 1, PLAYER_1, PLAYER_2);
    var roundEntity = round(1, PLAYING, Map.of(PLAYER_1, WON));

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(
        Optional.of(roundEntity));

    // Act / Assert
    assertThatThrownBy(() -> service.handleReady(ROOM_ID, PLAYER_1, 1))
        .isInstanceOf(RoundException.class)
        .hasMessage("Round is not ended")
        .satisfies(ex -> assertThat(((RoundException) ex).getCode()).isEqualTo(ROUND_NOT_ENDED));

    verify(roomJpaRepository, never()).save(roomEntity);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void handleReady_notAllPlayersReady_setsReadyAndDoesNotStartNewRound() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, 1, PLAYER_1, PLAYER_2);
    var roundEntity = round(1, ENDED, Map.of(
        PLAYER_1, WON,
        PLAYER_2, LOST
    ));

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(
        Optional.of(roundEntity));

    // Act
    var result = service.handleReady(ROOM_ID, PLAYER_1, 1);

    // Assert
    var currentRound = result.currentRound();
    assertThat(currentRound.roundNumber()).isEqualTo(1);
    assertThat(currentRound.roundStatus()).isEqualTo(ENDED);
    assertThat(currentRound.statusByPlayerId().get(PLAYER_1)).isEqualTo(READY);
    assertThat(currentRound.statusByPlayerId().get(PLAYER_2)).isEqualTo(LOST);

    verify(eventPublisher).publishEvent(new RoomEventToPublish(ROOM_ID,
        new RoomEvent(PLAYER_STATUS_UPDATED, new PlayerStatusUpdatedPayload(READY))));
    verifyNoMoreInteractions(eventPublisher);
    verify(roundLifecycleService, never()).startNewRoundEntity(roomEntity);
    verify(roomJpaRepository).save(roomEntity);
  }

  @Test
  void handleReady_allPlayersReady_startsNewRound() {
    // Arrange
    var roomEntity = inProgressRoom(ROOM_ID, 1, PLAYER_1, PLAYER_2);
    var roundEntity = round(1, ENDED, Map.of(
        PLAYER_1, LOST,
        PLAYER_2, READY
    ));

    var newRoundEntity = round(2, PLAYING, Map.of(
        PLAYER_1, RoundPlayerStatus.PLAYING,
        PLAYER_2, RoundPlayerStatus.PLAYING
    ));

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(
        Optional.of(roundEntity));
    when(roundLifecycleService.startNewRoundEntity(roomEntity)).thenReturn(newRoundEntity);

    // Act
    var result = service.handleReady(ROOM_ID, PLAYER_1, 1);

    // Assert
    var currentRound = result.currentRound();
    assertThat(currentRound.roundNumber()).isEqualTo(2);
    assertThat(currentRound.roundStatus()).isEqualTo(PLAYING);
    assertThat(currentRound.statusByPlayerId().get(PLAYER_1)).isEqualTo(RoundPlayerStatus.PLAYING);
    assertThat(currentRound.statusByPlayerId().get(PLAYER_2)).isEqualTo(RoundPlayerStatus.PLAYING);

    verify(roundLifecycleService).startNewRoundEntity(roomEntity);
    verify(roomJpaRepository).save(roomEntity);
    verifyNoInteractions(eventPublisher);
  }

  private static RoomEntity room(String roomId) {
    var room = new RoomEntity();
    room.setId(roomId);
    return room;
  }

  private static RoomEntity inProgressRoom(String roomId, String... playerIds) {
    return inProgressRoom(roomId, null, playerIds);
  }

  private static RoomEntity inProgressRoom(String roomId, Integer currentRoundNumber,
      String... playerIds) {
    var room = room(roomId);
    room.setLanguage(IT);
    room.setStatus(IN_PROGRESS);

    for (var playerId : playerIds) {
      room.addPlayer(playerId);
    }

    if (currentRoundNumber != null) {
      room.setCurrentRoundNumber(currentRoundNumber);
    }

    return room;
  }

  private static RoundEntity round(int roundNumber, RoundStatus roundStatus) {
    return round(roundNumber, roundStatus, Map.of());
  }

  private static RoundEntity round(
      int roundNumber,
      RoundStatus roundStatus,
      Map<String, RoundPlayerStatus> statusByPlayerId
  ) {
    var round = new RoundEntity();
    round.setRoundNumber(roundNumber);
    round.setMaxAttempts(MAX_ATTEMPTS);
    round.setRoundStatus(roundStatus);

    if (statusByPlayerId != null) {
      statusByPlayerId.forEach(round::setPlayerStatus);
    }

    return round;
  }
}
