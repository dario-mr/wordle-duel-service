package com.dariom.wds.service.round;

import static com.dariom.wds.service.round.validation.RoomAccessValidator.validateRoomStatus;

import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.room.RoomLockManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RoundService {

  private final RoomLockManager roomLockManager;
  private final PlatformTransactionManager transactionManager;
  private final RoomJpaRepository roomJpaRepository;
  private final RoundJpaRepository roundJpaRepository;
  private final DomainMapper domainMapper;
  private final RoundLifecycleService roundLifecycleService;
  private final GuessSubmissionService guessSubmissionService;

  @Transactional(readOnly = true)
  public Optional<Round> getCurrentRound(String roomId, Integer currentRoundNumber) {
    if (currentRoundNumber == null) {
      return Optional.empty();
    }

    return roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, currentRoundNumber)
        .map(domainMapper::toRound);
  }

  @Transactional
  public Round startNewRound(String roomId) {
    var room = findRoom(roomId);
    var round = roundLifecycleService.startNewRoundEntity(room);
    roomJpaRepository.save(room);
    return domainMapper.toRound(round);
  }

  public Room handleGuess(String roomId, String playerId, String guess) {
    return roomLockManager.withRoomLock(roomId, () -> {
      var transactionTemplate = new TransactionTemplate(transactionManager);
      return transactionTemplate.execute(
          status -> handleGuessInTransaction(roomId, playerId, guess));
    });
  }

  private Room handleGuessInTransaction(String roomId, String playerId, String guess) {
    var roomEntity = findRoom(roomId);
    validateRoomStatus(playerId, domainMapper.toRoom(roomEntity, null));

    var roundEntity = roundLifecycleService.ensureActiveRound(roomEntity);
    guessSubmissionService.applyGuess(roomId, playerId, guess, roomEntity, roundEntity);

    if (!roundEntity.isFinished()
        && roundLifecycleService.isRoundFinished(roomEntity, roundEntity)) {
      roundLifecycleService.finishRound(roundEntity, roomEntity);
    }

    roomJpaRepository.save(roomEntity);
    return domainMapper.toRoom(roomEntity, domainMapper.toRound(roundEntity));
  }

  private RoomEntity findRoom(String roomId) {
    return roomJpaRepository.findWithPlayersAndScoresById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));
  }
}
