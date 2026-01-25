package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.ROUND_NOT_CURRENT;
import static com.dariom.wds.api.common.ErrorCode.ROUND_NOT_ENDED;
import static com.dariom.wds.domain.RoundPlayerStatus.READY;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.domain.RoundStatus.PLAYING;
import static com.dariom.wds.service.round.validation.RoomAccessValidator.validateRoomStatus;
import static com.dariom.wds.websocket.model.EventType.PLAYER_STATUS_UPDATED;

import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.exception.RoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.room.RoomLockManager;
import com.dariom.wds.service.user.UserService;
import com.dariom.wds.websocket.model.PlayerStatusUpdatedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher eventPublisher;
  private final UserService userService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Optional<Round> getCurrentRound(String roomId, Integer currentRoundNumber) {
    if (currentRoundNumber == null) {
      return Optional.empty();
    }

    return roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, currentRoundNumber)
        .map(domainMapper::toRound);
  }

  @Transactional(readOnly = true)
  public Map<String, Round> getCurrentRoundsByRoomIds(List<String> roomIds) {
    if (roomIds.isEmpty()) {
      return Map.of();
    }

    var roundPerRoomId = new HashMap<String, Round>();
    var currentRounds = roundJpaRepository.findCurrentRoundsWithDetailsByRoomIds(roomIds);
    for (var roundEntity : currentRounds) {
      roundPerRoomId.put(roundEntity.getRoom().getId(), domainMapper.toRound(roundEntity));
    }

    return roundPerRoomId;
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

  public Room handleReady(String roomId, String playerId, Integer roundNumber) {
    return roomLockManager.withRoomLock(roomId, () -> {
      var transactionTemplate = new TransactionTemplate(transactionManager);
      return transactionTemplate.execute(
          status -> handleReadyInTransaction(roomId, playerId, roundNumber));
    });
  }

  private Room handleGuessInTransaction(String roomId, String playerId, String guess) {
    var roomEntity = findRoom(roomId);
    validateRoomStatus(playerId, roomId, roomEntity.getStatus(), roomEntity.getPlayerIds());

    var roundEntity = roundLifecycleService.ensureActiveRound(roomEntity);
    var statusUpdate =
        guessSubmissionService.applyGuess(roomId, playerId, guess, roomEntity, roundEntity);

    var shouldFinishRound = roundEntity.getRoundStatus() == PLAYING
        && roundLifecycleService.isRoundFinished(roomEntity, roundEntity);

    if (shouldFinishRound) {
      roundLifecycleService.finishRound(roundEntity, roomEntity);
    } else {
      statusUpdate.ifPresent(status -> publishPlayerStatusUpdated(roomId, status));
    }

    roomEntity.setLastUpdatedAt(Instant.now(clock));
    var saved = roomJpaRepository.save(roomEntity);
    var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

    return domainMapper.toRoom(roomEntity, domainMapper.toRound(roundEntity), displayNamePerPlayer);
  }

  private Room handleReadyInTransaction(String roomId, String playerId, Integer roundNumber) {
    var roomEntity = findRoom(roomId);
    validateRoomStatus(playerId, roomId, roomEntity.getStatus(), roomEntity.getPlayerIds());

    var currentRoundNumber = roomEntity.getCurrentRoundNumber();
    if (currentRoundNumber == null || !currentRoundNumber.equals(roundNumber)) {
      throw new RoundException(
          ROUND_NOT_CURRENT, "Round <%s> is not the current round".formatted(roundNumber)
      );
    }

    var roundEntity = roundJpaRepository
        .findWithDetailsByRoomIdAndRoundNumber(roomId, currentRoundNumber)
        .orElseThrow(() -> new RoundException(
            ROUND_NOT_CURRENT, "Round <%s> is not the current round".formatted(roundNumber)));

    if (roundEntity.getRoundStatus() != ENDED) {
      throw new RoundException(ROUND_NOT_ENDED, "Round is not ended");
    }

    if (roundEntity.getPlayerStatus(playerId) != READY) {
      roundEntity.setPlayerStatus(playerId, READY);
    }

    var allPlayersReady = roomEntity.getPlayerIds().stream()
        .allMatch(pid -> roundEntity.getPlayerStatus(pid) == READY);

    var currentRoundEntity = roundEntity;
    if (allPlayersReady) {
      currentRoundEntity = roundLifecycleService.startNewRoundEntity(roomEntity);
    } else {
      publishPlayerStatusUpdated(roomId, READY);
    }

    roomEntity.setLastUpdatedAt(Instant.now(clock));
    var saved = roomJpaRepository.save(roomEntity);
    var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

    return domainMapper.toRoom(roomEntity, domainMapper.toRound(currentRoundEntity),
        displayNamePerPlayer);
  }

  private RoomEntity findRoom(String roomId) {
    return roomJpaRepository.findWithPlayersById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));
  }

  private void publishPlayerStatusUpdated(String roomId, RoundPlayerStatus playerStatus) {
    eventPublisher.publishEvent(new RoomEventToPublish(roomId,
        new RoomEvent(PLAYER_STATUS_UPDATED, new PlayerStatusUpdatedPayload(playerStatus))));
  }

  private Map<String, String> getDisplayNamePerPlayer(RoomEntity room) {
    var playerIds = room.getPlayerIds();
    return userService.getDisplayNamePerPlayer(playerIds);
  }
}
