package com.dariom.wds.service.round;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomRounds.FIVE;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.MATCH_FINISHED;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.api.common.ErrorCode;
import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.EventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

@ExtendWith(MockitoExtension.class)
class RoundLifecycleServiceTest {

  private static final String ROOM_ID = "room-1";
  private static final String PLAYER_1 = "p1";
  private static final String PLAYER_2 = "p2";

  @Mock
  private DictionaryRepository dictionaryRepository;
  @Mock
  private RoundJpaRepository roundJpaRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private final WordleProperties properties = new WordleProperties(6, 5);
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"),
      ZoneOffset.UTC);

  private RoundLifecycleService service;

  @BeforeEach
  void setUp() {
    service = new RoundLifecycleService(dictionaryRepository, roundJpaRepository, properties,
        eventPublisher, clock);
  }

  @Test
  void startNewRoundEntity_twoPlayers_setsBothPointersAndCreatesSharedRound() {
    var room = room(IN_PROGRESS, null);
    when(dictionaryRepository.getAnswerWords(IT)).thenReturn(Set.of("PIZZA"));

    var round = service.startNewRoundEntity(room);

    assertThat(round.getRoundNumber()).isEqualTo(1);
    assertThat(round.getTargetWord()).isEqualTo("PIZZA");
    assertThat(round.getPlayerStatus(PLAYER_1)).isEqualTo(PLAYING);
    assertThat(round.getPlayerStatus(PLAYER_2)).isEqualTo(PLAYING);
    assertThat(room.findRoomPlayer(PLAYER_1).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(1);
    assertThat(room.findRoomPlayer(PLAYER_2).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(1);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void ensurePlayerRound_missingRound_createsRoundForRequesterOnly() {
    var room = room(IN_PROGRESS, 2);
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(ROOM_ID, 2))
        .thenReturn(Optional.empty());
    when(dictionaryRepository.getAnswerWords(IT)).thenReturn(Set.of("PIZZA"));

    var round = service.ensurePlayerRound(room, PLAYER_1);

    assertThat(round.getTargetWord()).isEqualTo("PIZZA");
    assertThat(round.getPlayerStatus(PLAYER_1)).isEqualTo(PLAYING);
    assertThat(round.getPlayerStatus(PLAYER_2)).isNull();
    assertThat(room.getRounds()).containsExactly(round);
  }

  @Test
  void ensurePlayerRound_finishedPlayer_throwsPlayerDone() {
    var room = room(IN_PROGRESS, null);

    var thrown = catchThrowable(() -> service.ensurePlayerRound(room, PLAYER_1));

    assertThat(thrown)
        .isInstanceOfSatisfying(InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PLAYER_DONE));
  }

  @Test
  void completePlayerRound_winnerKeepsCompletedRoundScoresAndPublishesScoreUpdate() {
    var room = room(IN_PROGRESS, 1);
    room.setConfiguredRounds(FIVE);
    var round = round(1, Map.of(PLAYER_1, WON, PLAYER_2, PLAYING));
    round.setMaxAttempts(6);
    round.setGuesses(java.util.List.of(guess(PLAYER_1, 1), guess(PLAYER_1, 2)));
    service.completePlayerRound(round, room, PLAYER_1, WON);

    assertThat(room.getMatchScoresByPlayerId().get(PLAYER_1)).isEqualTo(5);
    assertThat(room.findRoomPlayer(PLAYER_1).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(1);
    assertThat(room.findRoomPlayer(PLAYER_2).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(1);

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().event().type()).isEqualTo(SCORES_UPDATED);
  }

  @Test
  void completePlayerRound_strictFinalWinner_incrementsWinsAndKeepsMatchScores() {
    var room = room(IN_PROGRESS, 5);
    room.setConfiguredRounds(FIVE);
    var round = round(5, Map.of(PLAYER_1, WON, PLAYER_2, PLAYING));
    round.setMaxAttempts(6);
    round.setGuesses(java.util.List.of(
        guess(PLAYER_1, 1),
        guess(PLAYER_2, 1),
        guess(PLAYER_2, 2),
        guess(PLAYER_2, 3),
        guess(PLAYER_2, 4),
        guess(PLAYER_2, 5)
    ));

    service.completePlayerRound(round, room, PLAYER_1, WON);
    assertThat(room.getStatus()).isEqualTo(IN_PROGRESS);

    round.setPlayerStatus(PLAYER_2, WON);
    service.completePlayerRound(round, room, PLAYER_2, WON);
    assertThat(room.getStatus()).isEqualTo(MATCH_FINISHED);
    assertThat(round.getRoundStatus()).isEqualTo(ENDED);
    assertThat(room.getMatchScoresByPlayerId())
        .containsExactlyInAnyOrderEntriesOf(Map.of(PLAYER_1, 6, PLAYER_2, 2));
    assertThat(room.getRoomPlayers()).extracting(player -> player.getWins())
        .containsExactlyInAnyOrder(1, 0);

    var events = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(events.capture());
    assertThat(events.getAllValues()).extracting(event -> event.event().type())
        .containsExactly(SCORES_UPDATED, EventType.MATCH_FINISHED);
  }

  @Test
  void completePlayerRound_finalTie_keepsMatchScoresWithoutIncrementingWins() {
    var room = room(IN_PROGRESS, 5);
    room.setConfiguredRounds(FIVE);
    var round = round(5, Map.of(PLAYER_1, WON, PLAYER_2, PLAYING));
    round.setMaxAttempts(6);
    round.setGuesses(new java.util.ArrayList<>(java.util.List.of(
        guess(PLAYER_1, 1),
        guess(PLAYER_2, 1)
    )));

    service.completePlayerRound(round, room, PLAYER_1, WON);
    round.setPlayerStatus(PLAYER_2, WON);
    service.completePlayerRound(round, room, PLAYER_2, WON);

    assertThat(room.getStatus()).isEqualTo(MATCH_FINISHED);
    assertThat(room.getRoomPlayers()).allMatch(player -> player.getWins() == 0);
    assertThat(room.getMatchScoresByPlayerId())
        .containsExactlyInAnyOrderEntriesOf(Map.of(PLAYER_1, 6, PLAYER_2, 6));
  }

  @Test
  void advancePlayerRound_finishedPlayerMovesOnlyRequesterToNextRound() {
    var room = room(IN_PROGRESS, 1);
    room.setConfiguredRounds(FIVE);
    var completedRound = round(1, Map.of(PLAYER_1, WON, PLAYER_2, PLAYING));
    var nextRound = round(2, Map.of());
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(ROOM_ID, 1))
        .thenReturn(Optional.of(completedRound));
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(ROOM_ID, 2))
        .thenReturn(Optional.of(nextRound));

    var result = service.advancePlayerRound(room, PLAYER_1);

    assertThat(result).isEqualTo(nextRound);
    assertThat(room.findRoomPlayer(PLAYER_1).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(2);
    assertThat(room.findRoomPlayer(PLAYER_2).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(1);
    assertThat(nextRound.getPlayerStatus(PLAYER_1)).isEqualTo(PLAYING);
  }

  @Test
  void isRoundFinished_missingOpponentStatus_returnsFalse() {
    var room = room(IN_PROGRESS, 1);
    var round = round(1, Map.of(PLAYER_1, LOST));

    assertThat(service.isRoundFinished(room, round)).isFalse();
  }

  @Test
  void startNewRoundEntity_emptyDictionary_throwsWithoutPublishingEvent() {
    var room = room(IN_PROGRESS, null);
    when(dictionaryRepository.getAnswerWords(any())).thenReturn(Set.of());

    var thrown = catchThrowable(() -> service.startNewRoundEntity(room));

    assertThat(thrown).isInstanceOf(DictionaryEmptyException.class);
    assertThat(room.getRounds()).isEmpty();
    verifyNoInteractions(eventPublisher);
  }

  private static RoomEntity room(com.dariom.wds.domain.RoomStatus status,
      Integer currentRoundNumber) {
    var room = new RoomEntity();
    room.setId(ROOM_ID);
    room.setLanguage(IT);
    room.setStatus(status);
    room.addPlayer(PLAYER_1);
    room.addPlayer(PLAYER_2);
    room.findRoomPlayer(PLAYER_1).orElseThrow().setCurrentRoundNumber(currentRoundNumber);
    room.findRoomPlayer(PLAYER_2).orElseThrow().setCurrentRoundNumber(currentRoundNumber);
    return room;
  }

  private static RoundEntity round(int roundNumber,
      Map<String, RoundPlayerStatus> statuses) {
    var round = new RoundEntity();
    round.setRoundNumber(roundNumber);
    round.setRoundStatus(RoundStatus.PLAYING);
    statuses.forEach(round::setPlayerStatus);
    return round;
  }

  private static GuessEntity guess(String playerId, int attemptNumber) {
    var guess = new GuessEntity();
    guess.setPlayerId(playerId);
    guess.setAttemptNumber(attemptNumber);
    return guess;
  }
}
