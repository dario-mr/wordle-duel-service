package com.dariom.wds.service.round;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.NoOpTransactionManager;
import com.dariom.wds.service.room.RoomLockManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

  @Mock
  private RoomJpaRepository roomJpaRepository;

  @Mock
  private RoundJpaRepository roundJpaRepository;

  @Mock
  private RoundLifecycleService roundLifecycleService;

  @Mock
  private GuessSubmissionService guessSubmissionService;

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
        guessSubmissionService
    );
  }

  @Test
  void getCurrentRound_currentRoundNumberIsNull_returnsEmpty() {
    // Act
    var result = service.getCurrentRound("room-1", null);

    // Assert
    assertThat(result).isEmpty();
    verify(roundJpaRepository, never()).findWithDetailsByRoomIdAndRoundNumber(anyString(),
        anyInt());
  }

  @Test
  void startNewRound_roomExists_returnsMappedRoundAndSavesRoom() {
    // Arrange
    var roomEntity = new RoomEntity();
    roomEntity.setId("room-1");

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));

    var roundEntity = new RoundEntity();
    roundEntity.setRoundNumber(1);
    roundEntity.setMaxAttempts(6);
    roundEntity.setFinished(false);

    when(roundLifecycleService.startNewRoundEntity(roomEntity)).thenReturn(roundEntity);

    // Act
    var result = service.startNewRound("room-1");

    // Assert
    assertThat(result).isEqualTo(domainMapper.toRound(roundEntity));
    verify(roomJpaRepository).findWithPlayersAndScoresById("room-1");
    verify(roomJpaRepository).save(roomEntity);
  }

  @Test
  void handleGuess_validInput_returnsMappedRoomAndPersistsRoom() {
    // Arrange
    var roomEntity = new RoomEntity();
    roomEntity.setId("room-1");
    roomEntity.setLanguage(IT);
    roomEntity.setStatus(IN_PROGRESS);
    roomEntity.addPlayer("p1");
    roomEntity.addPlayer("p2");
    roomEntity.setPlayerScore("p1", 0);
    roomEntity.setPlayerScore("p2", 0);

    var roundEntity = new RoundEntity();
    roundEntity.setRoundNumber(1);
    roundEntity.setMaxAttempts(6);
    roundEntity.setFinished(false);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundLifecycleService.ensureActiveRound(roomEntity)).thenReturn(roundEntity);
    when(roundLifecycleService.isRoundFinished(roomEntity, roundEntity)).thenReturn(false);

    // Act
    var result = service.handleGuess("room-1", "p1", "pizza");

    // Assert
    var expectedRound = domainMapper.toRound(roundEntity);
    var expectedRoom = domainMapper.toRoom(roomEntity, expectedRound);
    assertThat(result).isEqualTo(expectedRoom);

    verify(guessSubmissionService).applyGuess("room-1", "p1", "pizza", roomEntity, roundEntity);
    verify(roomJpaRepository).findWithPlayersAndScoresById("room-1");
    verify(roomJpaRepository).save(roomEntity);
  }

  @Test
  void handleGuess_roundBecomesFinished_callsFinishRound() {
    // Arrange
    var roomEntity = new RoomEntity();
    roomEntity.setId("room-1");
    roomEntity.setLanguage(IT);
    roomEntity.setStatus(IN_PROGRESS);
    roomEntity.addPlayer("p1");
    roomEntity.addPlayer("p2");
    roomEntity.setPlayerScore("p1", 0);
    roomEntity.setPlayerScore("p2", 0);

    var roundEntity = new RoundEntity();
    roundEntity.setRoundNumber(1);
    roundEntity.setMaxAttempts(6);
    roundEntity.setFinished(false);

    when(roomJpaRepository.findWithPlayersAndScoresById(anyString())).thenReturn(
        Optional.of(roomEntity));
    when(roundLifecycleService.ensureActiveRound(roomEntity)).thenReturn(roundEntity);
    when(roundLifecycleService.isRoundFinished(roomEntity, roundEntity)).thenReturn(true);

    // Act
    service.handleGuess("room-1", "p1", "pizza");

    // Assert
    verify(roundLifecycleService).finishRound(roundEntity, roomEntity);
  }

}
