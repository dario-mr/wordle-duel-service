package com.dariom.wds.service.round;

import static com.dariom.wds.api.v1.error.ErrorCode.ROUND_FINISHED;
import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.websocket.model.EventType.ROUND_STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

  @Mock
  private DictionaryRepository dictionaryRepository;

  @Mock
  private RoundJpaRepository roundJpaRepository;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  private final WordleProperties properties = new WordleProperties(6, 5);
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

  private RoundLifecycleService service;

  @BeforeEach
  void setUp() {
    service = new RoundLifecycleService(dictionaryRepository, roundJpaRepository, properties,
        applicationEventPublisher, clock);

  }

  @Test
  void startNewRoundEntity_notTwoPlayers_throwsRoomNotReadyException() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.addPlayer("p1");

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
    room.addPlayer("p2");
    room.addPlayer("p1");

    when(dictionaryRepository.getAnswerWords(any())).thenReturn(Set.of("PIZZA"));

    // Act
    var round = service.startNewRoundEntity(room);

    // Assert
    assertThat(room.getCurrentRoundNumber()).isEqualTo(1);

    assertThat(round.getRoundNumber()).isEqualTo(1);
    assertThat(round.getTargetWord()).isEqualTo("PIZZA");
    assertThat(round.getMaxAttempts()).isEqualTo(6);
    assertThat(round.getStartedAt()).isEqualTo(clock.instant());

    assertThat(round.getPlayerStatus("p1")).isEqualTo(PLAYING);
    assertThat(round.getPlayerStatus("p2")).isEqualTo(PLAYING);

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

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
  void ensureActiveRound_currentRoundNumberIsNull_startsNewRound() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.addPlayer("p1");
    room.addPlayer("p2");

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

}
