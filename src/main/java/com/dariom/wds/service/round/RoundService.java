package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.ROUND_NOT_CURRENT;
import static com.dariom.wds.api.common.ErrorCode.ROUND_NOT_ENDED;
import static com.dariom.wds.domain.RoundPlayerStatus.READY;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.domain.RoundStatus.PLAYING;
import static com.dariom.wds.service.round.validation.RoomAccessValidator.validateRoomStatus;
import static com.dariom.wds.websocket.model.EventType.PLAYER_STATUS_UPDATED;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoundException;
import com.dariom.wds.metrics.HotPathMetrics;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.RoundRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.user.UserProfileService;
import com.dariom.wds.websocket.model.PlayerStatusUpdatedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoundService {

  private final RoomLockProperties lockProperties;
  private final RoomRepository roomRepository;
  private final RoundRepository roundRepository;
  private final DomainMapper domainMapper;
  private final RoundLifecycleService roundLifecycleService;
  private final GuessSubmissionService guessSubmissionService;
  private final ApplicationEventPublisher eventPublisher;
  private final UserProfileService userProfileService;
  private final Clock clock;
  private final HotPathMetrics hotPathMetrics;

  @Transactional(readOnly = true)
  public Optional<Round> getCurrentRound(String roomId, Integer currentRoundNumber) {
    return hotPathMetrics.record("round.get_current_round", "total", () -> {
      if (currentRoundNumber == null) {
        return Optional.empty();
      }

      return hotPathMetrics.record("round.get_current_round", "load_round", () ->
          roundRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, currentRoundNumber)
      ).map(roundEntity -> hotPathMetrics.record("round.get_current_round", "map_round",
          () -> domainMapper.toRound(roundEntity)));
    });
  }

  @Transactional(readOnly = true)
  public Map<String, Round> getCurrentRoundsByRoomIds(List<String> roomIds) {
    return hotPathMetrics.record("round.get_current_rounds", "total", () -> {
      if (roomIds.isEmpty()) {
        return Map.of();
      }

      var roundPerRoomId = new HashMap<String, Round>();
      var currentRounds = hotPathMetrics.record("round.get_current_rounds", "load_rounds",
          () -> roundRepository.findCurrentRoundsWithDetailsByRoomIds(roomIds));
      for (var roundEntity : currentRounds) {
        var round = hotPathMetrics.record("round.get_current_rounds", "map_round",
            () -> domainMapper.toRound(roundEntity));
        roundPerRoomId.put(roundEntity.getRoom().getId(), round);
      }

      return roundPerRoomId;
    });
  }

  @Transactional
  public Round startNewRound(String roomId) {
    var room = roomRepository.findWithPlayersById(roomId);
    var round = roundLifecycleService.startNewRoundEntity(room);
    roomRepository.save(room);
    return domainMapper.toRound(round);
  }

  @Transactional
  public Room handleGuess(String roomId, String playerId, String guess) {
    try {
      return handleGuessInTransaction(roomId, playerId, guess);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  @Transactional
  public Room handleReady(String roomId, String playerId, Integer roundNumber) {
    try {
      return handleReadyInTransaction(roomId, playerId, roundNumber);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  private Room handleGuessInTransaction(String roomId, String playerId, String guess) {
    var roomEntity = roomRepository.findWithPlayersByIdForUpdate(roomId,
        lockProperties.acquireTimeout());
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
    var saved = roomRepository.save(roomEntity);
    var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

    return domainMapper.toRoom(roomEntity, domainMapper.toRound(roundEntity), displayNamePerPlayer);
  }

  private Room handleReadyInTransaction(String roomId, String playerId, Integer roundNumber) {
    var roomEntity = roomRepository.findWithPlayersByIdForUpdate(roomId,
        lockProperties.acquireTimeout());
    validateRoomStatus(playerId, roomId, roomEntity.getStatus(), roomEntity.getPlayerIds());

    var currentRoundNumber = roomEntity.getCurrentRoundNumber();
    if (currentRoundNumber == null || !currentRoundNumber.equals(roundNumber)) {
      throw new RoundException(
          ROUND_NOT_CURRENT, "Round <%s> is not the current round".formatted(roundNumber)
      );
    }

    var roundEntity = hotPathMetrics.record("round.handle_ready", "load_round", () ->
            roundRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, currentRoundNumber)
        )
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
    var saved = roomRepository.save(roomEntity);
    var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

    return domainMapper.toRoom(roomEntity, domainMapper.toRound(currentRoundEntity),
        displayNamePerPlayer);
  }

  private void publishPlayerStatusUpdated(String roomId, RoundPlayerStatus playerStatus) {
    eventPublisher.publishEvent(new RoomEventToPublish(roomId,
        new RoomEvent(PLAYER_STATUS_UPDATED, new PlayerStatusUpdatedPayload(playerStatus))));
  }

  private Map<String, String> getDisplayNamePerPlayer(RoomEntity room) {
    var playerIds = room.getPlayerIds();
    return userProfileService.getDisplayNamePerPlayer(playerIds);
  }
}
