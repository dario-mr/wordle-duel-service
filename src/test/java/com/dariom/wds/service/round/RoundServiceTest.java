package com.dariom.wds.service.round;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.user.UserProfileService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

  private static final String ROOM_ID = "room-1";
  private static final String PLAYER_1 = "p1";
  private static final String PLAYER_2 = "p2";

  @Mock
  private RoomRepository roomRepository;
  @Mock
  private RoundJpaRepository roundJpaRepository;
  @Mock
  private RoundLifecycleService roundLifecycleService;
  @Mock
  private GuessSubmissionService guessSubmissionService;
  @Mock
  private UserProfileService userProfileService;

  private final RoomLockProperties lockProperties = new RoomLockProperties(Duration.ofSeconds(3));
  private final DomainMapper domainMapper = new DomainMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

  private RoundService service;

  @BeforeEach
  void setUp() {
    service = new RoundService(
        lockProperties,
        roomRepository,
        roundJpaRepository,
        domainMapper,
        roundLifecycleService,
        guessSubmissionService,
        userProfileService,
        clock
    );
  }

  @Test
  void getCurrentRound_playerPointerExists_returnsPlayerRound() {
    var roundEntity = round(1, RoundStatus.PLAYING,
        Map.of(PLAYER_1, PLAYING, PLAYER_2, PLAYING));
    when(roundJpaRepository.findCurrentRoundWithDetailsByRoomIdAndPlayerId(ROOM_ID, PLAYER_1))
        .thenReturn(Optional.of(roundEntity));

    var result = service.getCurrentRound(ROOM_ID, PLAYER_1);

    assertThat(result).contains(domainMapper.toRound(roundEntity, PLAYER_1));
    verify(roundJpaRepository).findCurrentRoundWithDetailsByRoomIdAndPlayerId(ROOM_ID, PLAYER_1);
  }

  @Test
  void getCurrentRoundsByRoomIds_emptyRoomIds_returnsEmptyWithoutQuery() {
    var result = service.getCurrentRoundsByRoomIds(List.of(), PLAYER_1);

    assertThat(result).isEmpty();
    verify(roundJpaRepository, never())
        .findCurrentRoundsWithDetailsByRoomIdsAndPlayerId(any(), anyString());
  }

  @Test
  void getCurrentRoundsByRoomIds_playerPointerExists_returnsRoundsByRoomId() {
    var room1 = inProgressRoom("room-1", 1, PLAYER_1, PLAYER_2);
    var room2 = inProgressRoom("room-2", 2, PLAYER_1, PLAYER_2);
    var round1 = round(1, RoundStatus.PLAYING, Map.of(PLAYER_1, PLAYING));
    var round2 = round(2, RoundStatus.ENDED, Map.of(PLAYER_1, WON));
    round1.setRoom(room1);
    round2.setRoom(room2);

    when(roundJpaRepository.findCurrentRoundsWithDetailsByRoomIdsAndPlayerId(
        List.of("room-1", "room-2"), PLAYER_1)).thenReturn(List.of(round1, round2));

    var result = service.getCurrentRoundsByRoomIds(List.of("room-1", "room-2"), PLAYER_1);

    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
        "room-1", domainMapper.toRound(round1, PLAYER_1),
        "room-2", domainMapper.toRound(round2, PLAYER_1)));
  }

  @Test
  void startNewRound_roomExists_createsAndSavesRound() {
    var roomEntity = inProgressRoom(ROOM_ID, null, PLAYER_1, PLAYER_2);
    when(roomRepository.findWithPlayersById(ROOM_ID)).thenReturn(roomEntity);

    service.startNewRound(ROOM_ID);

    verify(roundLifecycleService).startNewRoundEntity(roomEntity);
    verify(roomRepository).save(roomEntity);
  }

  @Test
  void handleGuess_roomLocked_throwsRoomLockedException() {
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any()))
        .thenThrow(new PessimisticLockingFailureException("locked"));

    assertThatThrownBy(() -> service.handleGuess(ROOM_ID, PLAYER_1, "pizza"))
        .isInstanceOf(RoomLockedException.class)
        .hasMessageContaining(ROOM_ID);
  }

  @Test
  void handleGuess_terminalResultReturnsCompletedRound() {
    var roomEntity = inProgressRoom(ROOM_ID, 1, PLAYER_1, PLAYER_2);
    var roundEntity = round(1, RoundStatus.PLAYING,
        Map.of(PLAYER_1, PLAYING, PLAYER_2, PLAYING));
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(roomEntity);
    when(guessSubmissionService.applyGuess(ROOM_ID, PLAYER_1, "pizza", roomEntity, roundEntity))
        .thenReturn(Optional.of(WON));
    when(roundLifecycleService.ensurePlayerRound(roomEntity, PLAYER_1)).thenReturn(roundEntity);
    when(roomRepository.save(roomEntity)).thenReturn(roomEntity);
    when(userProfileService.getDisplayNamePerPlayer(Set.of(PLAYER_1, PLAYER_2)))
        .thenReturn(Map.of());

    var result = service.handleGuess(ROOM_ID, PLAYER_1, "pizza");

    assertThat(result.currentRound()).isEqualTo(domainMapper.toRound(roundEntity, PLAYER_1));
    verify(roundLifecycleService).completePlayerRound(roundEntity, roomEntity, PLAYER_1, WON);
    assertThat(roomEntity.findRoomPlayer(PLAYER_1).orElseThrow().getCurrentRoundNumber())
        .isEqualTo(1);
  }

  @Test
  void advanceToNextRound_returnsOnlyRequestersNewRound() {
    var roomEntity = inProgressRoom(ROOM_ID, 1, PLAYER_1, PLAYER_2);
    var nextRound = round(2, RoundStatus.PLAYING, Map.of(PLAYER_1, PLAYING));
    when(roomRepository.findWithPlayersByIdForUpdate(anyString(), any())).thenReturn(roomEntity);
    when(roundLifecycleService.advancePlayerRound(roomEntity, PLAYER_1)).thenReturn(nextRound);
    when(roomRepository.save(roomEntity)).thenReturn(roomEntity);
    when(userProfileService.getDisplayNamePerPlayer(Set.of(PLAYER_1, PLAYER_2)))
        .thenReturn(Map.of());

    var result = service.advanceToNextRound(ROOM_ID, PLAYER_1);

    assertThat(result.currentRound()).isEqualTo(domainMapper.toRound(nextRound, PLAYER_1));
    verify(roundLifecycleService).advancePlayerRound(roomEntity, PLAYER_1);
  }

  private static RoomEntity inProgressRoom(
      String roomId, Integer currentRoundNumber, String... playerIds) {
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(IN_PROGRESS);
    for (var playerId : playerIds) {
      room.addPlayer(playerId);
      room.findRoomPlayer(playerId).orElseThrow().setCurrentRoundNumber(currentRoundNumber);
    }
    return room;
  }

  private static RoundEntity round(
      int roundNumber,
      RoundStatus roundStatus,
      Map<String, RoundPlayerStatus> statuses) {
    var round = new RoundEntity();
    round.setRoundNumber(roundNumber);
    round.setMaxAttempts(6);
    round.setRoundStatus(roundStatus);
    statuses.forEach(round::setPlayerStatus);
    return round;
  }
}
