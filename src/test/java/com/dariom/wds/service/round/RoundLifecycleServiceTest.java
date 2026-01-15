package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.ROUND_FINISHED;
import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.websocket.model.EventType.ROUND_STARTED;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundFinishedPayload;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RoundLifecycleServiceTest {

  private static final String PLAYER_1 = "p1";
  private static final String PLAYER_2 = "p2";

  @Mock
  private DictionaryRepository dictionaryRepository;
  @Mock
  private RoundJpaRepository roundJpaRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private final WordleProperties properties = new WordleProperties(6, 5);
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), UTC);

  private RoundLifecycleService service;

  @BeforeEach
  void setUp() {
    service = new RoundLifecycleService(dictionaryRepository, roundJpaRepository, properties,
        eventPublisher, clock);

  }

  @Test
  void startNewRoundEntity_notTwoPlayers_throwsRoomNotReadyException() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.addPlayer(PLAYER_1);

    // Act
    var thrown = catchThrowable(() -> service.startNewRoundEntity(room));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomNotReadyException.class)
        .hasMessageContaining("room-1")
        .hasMessageContaining("1");
  }

  @Test
  void startNewRoundEntity_twoPlayers_initializesRoundAndPublishesEvent() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.addPlayer(PLAYER_2);
    room.addPlayer(PLAYER_1);

    when(dictionaryRepository.getAnswerWords(any())).thenReturn(Set.of("PIZZA"));

    // Act
    var round = service.startNewRoundEntity(room);

    // Assert
    assertThat(room.getCurrentRoundNumber()).isEqualTo(1);

    assertThat(round.getRoundNumber()).isEqualTo(1);
    assertThat(round.getTargetWord()).isEqualTo("PIZZA");
    assertThat(round.getMaxAttempts()).isEqualTo(6);
    assertThat(round.getStartedAt()).isEqualTo(clock.instant());

    assertThat(round.getPlayerStatus(PLAYER_1)).isEqualTo(PLAYING);
    assertThat(round.getPlayerStatus(PLAYER_2)).isEqualTo(PLAYING);

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    var published = eventCaptor.getValue();
    assertThat(published.roomId()).isEqualTo("room-1");

    RoomEvent roomEvent = published.event();
    assertThat(roomEvent.type()).isEqualTo(ROUND_STARTED);
    assertThat(roomEvent.payload()).isInstanceOf(RoundStartedPayload.class);

    var payload = (RoundStartedPayload) roomEvent.payload();
    assertThat(payload.roundNumber()).isEqualTo(1);
    assertThat(payload.maxAttempts()).isEqualTo(6);
  }

  @Test
  void startNewRoundEntity_twoPlayers_dictionaryEmpty_throwsDictionaryEmptyException() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.addPlayer(PLAYER_1);
    room.addPlayer(PLAYER_2);

    when(dictionaryRepository.getAnswerWords(any())).thenReturn(Set.of());

    // Act
    var thrown = catchThrowable(() -> service.startNewRoundEntity(room));

    // Assert
    assertThat(thrown)
        .isInstanceOf(DictionaryEmptyException.class)
        .hasMessageContaining("IT");
    assertThat(room.getCurrentRoundNumber()).isNull();

    verify(dictionaryRepository).getAnswerWords(IT);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void ensureActiveRound_currentRoundNumberIsNull_startsNewRound() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.addPlayer(PLAYER_1);
    room.addPlayer(PLAYER_2);

    when(dictionaryRepository.getAnswerWords(any())).thenReturn(Set.of("PIZZA"));

    // Act
    var round = service.ensureActiveRound(room);

    // Assert
    assertThat(round).isNotNull();
    assertThat(round.getRoundNumber()).isEqualTo(1);
  }

  @Test
  void ensureActiveRound_existingRoundUnfinished_returnsExistingRound() {
    // Arrange
    var spied = spy(service);

    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.setCurrentRoundNumber(1);

    var existingRound = new RoundEntity();
    existingRound.setRoundStatus(RoundStatus.PLAYING);

    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(anyString(), anyInt()))
        .thenReturn(Optional.of(existingRound));

    // Act
    var actual = spied.ensureActiveRound(room);

    // Assert
    assertThat(actual).isSameAs(existingRound);
    verify(spied, never()).startNewRoundEntity(any(RoomEntity.class));
    verify(roundJpaRepository).findWithDetailsByRoomIdAndRoundNumber("room-1", 1);
  }

  @Test
  void ensureActiveRound_existingRoundFinished_throwsInvalidGuessException() {
    // Arrange
    var spied = spy(service);

    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.setCurrentRoundNumber(1);

    var finishedRound = new RoundEntity();
    finishedRound.setRoundStatus(ENDED);

    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(anyString(), anyInt()))
        .thenReturn(Optional.of(finishedRound));

    // Act
    var thrown = catchThrowable(() -> spied.ensureActiveRound(room));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(ROUND_FINISHED)
        );

    verify(spied, never()).startNewRoundEntity(any(RoomEntity.class));
    verify(roundJpaRepository).findWithDetailsByRoomIdAndRoundNumber("room-1", 1);
  }

  @Test
  void finishRound_playerWon_finishRoundAndIncrementPlayerScore() {
    // Arrange
    var round = new RoundEntity();
    round.setRoundStatus(RoundStatus.PLAYING);
    round.setRoundNumber(1);
    round.setMaxAttempts(6);
    round.setPlayerStatus(PLAYER_1, WON);
    round.setPlayerStatus(PLAYER_2, LOST);
    round.setGuesses(List.of(
        guess(PLAYER_1, 1),
        guess(PLAYER_1, 2)
    ));

    var room = new RoomEntity();
    room.setId("room-1");
    room.addPlayer(PLAYER_1);
    room.addPlayer(PLAYER_2);

    // Act
    service.finishRound(round, room);

    // Assert
    assertThat(round.getRoundStatus()).isEqualTo(ENDED);
    assertThat(round.getFinishedAt()).isNotNull();
    assertThat(room.getScoresByPlayerId().get(PLAYER_1)).isEqualTo(5);
    assertThat(room.getScoresByPlayerId().get(PLAYER_2)).isEqualTo(0);

    verify(eventPublisher).publishEvent(
        new RoomEventToPublish("room-1", new RoomEvent(
            EventType.ROUND_FINISHED,
            new RoundFinishedPayload(1)
        )));
  }

  @Test
  void isRoundFinished_playerIsPlaying_returnFalse() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.addPlayer(PLAYER_1);
    room.addPlayer(PLAYER_2);

    var round = new RoundEntity();
    round.setPlayerStatus(PLAYER_1, PLAYING);
    round.setPlayerStatus(PLAYER_2, LOST);

    // Act
    var finished = service.isRoundFinished(room, round);

    // Assert
    assertThat(finished).isFalse();
  }

  @Test
  void isRoundFinished_NoPlayerIsPlaying_returnTrue() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.addPlayer(PLAYER_1);
    room.addPlayer(PLAYER_2);

    var round = new RoundEntity();
    round.setPlayerStatus(PLAYER_1, WON);
    round.setPlayerStatus(PLAYER_2, WON);

    // Act
    var finished = service.isRoundFinished(room, round);

    // Assert
    assertThat(finished).isTrue();
  }

  private static GuessEntity guess(String playerId, int attemptNumber) {
    var guess = new GuessEntity();
    guess.setPlayerId(playerId);
    guess.setAttemptNumber(attemptNumber);

    return guess;
  }

}
