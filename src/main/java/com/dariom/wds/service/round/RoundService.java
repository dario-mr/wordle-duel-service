package com.dariom.wds.service.round;

import static com.dariom.wds.service.round.validation.RoomAccessValidator.validateRoomStatus;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.user.UserProfileService;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoundService {

  private final RoomLockProperties lockProperties;
  private final RoomRepository roomRepository;
  private final RoundJpaRepository roundJpaRepository;
  private final DomainMapper domainMapper;
  private final RoundLifecycleService roundLifecycleService;
  private final GuessSubmissionService guessSubmissionService;
  private final UserProfileService userProfileService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Optional<Round> getCurrentRound(String roomId, String playerId) {
    return roundJpaRepository.findCurrentRoundWithDetailsByRoomIdAndPlayerId(roomId, playerId)
        .map(round -> domainMapper.toRound(round, playerId));
  }

  @Transactional(readOnly = true)
  public Map<String, Round> getCurrentRoundsByRoomIds(List<String> roomIds, String playerId) {
    if (roomIds.isEmpty()) {
      return Map.of();
    }

    var roundPerRoomId = new HashMap<String, Round>();
    var currentRounds = roundJpaRepository
        .findCurrentRoundsWithDetailsByRoomIdsAndPlayerId(roomIds, playerId);
    for (var round : currentRounds) {
      roundPerRoomId.put(round.getRoom().getId(), domainMapper.toRound(round, playerId));
    }

    return roundPerRoomId;
  }

  @Transactional
  public void startNewRound(String roomId) {
    var room = roomRepository.findWithPlayersById(roomId);
    roundLifecycleService.startNewRoundEntity(room);
    roomRepository.save(room);
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

  private Room handleGuessInTransaction(String roomId, String playerId, String guess) {
    var roomEntity = roomRepository.findWithPlayersByIdForUpdate(roomId,
        lockProperties.acquireTimeout());
    validateRoomStatus(playerId, roomId, roomEntity.getStatus(), roomEntity.getPlayerIds());

    var roundEntity = roundLifecycleService.ensurePlayerRound(roomEntity, playerId);
    var statusUpdate = guessSubmissionService.applyGuess(
        roomId, playerId, guess, roomEntity, roundEntity);
    if (statusUpdate.isPresent()) {
      roundLifecycleService.completePlayerRound(roundEntity, roomEntity, playerId,
          statusUpdate.get());
    }

    roomEntity.setLastUpdatedAt(Instant.now(clock));
    var saved = roomRepository.save(roomEntity);
    var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

    return domainMapper.toRoom(roomEntity,
        domainMapper.toRound(roundEntity, playerId), displayNamePerPlayer);
  }

  @Transactional
  public Room advanceToNextRound(String roomId, String playerId) {
    try {
      var roomEntity = roomRepository.findWithPlayersByIdForUpdate(roomId,
          lockProperties.acquireTimeout());
      validateRoomStatus(playerId, roomId, roomEntity.getStatus(), roomEntity.getPlayerIds());

      var nextRound = roundLifecycleService.advancePlayerRound(roomEntity, playerId);
      roomEntity.setLastUpdatedAt(Instant.now(clock));
      var saved = roomRepository.save(roomEntity);
      var displayNamePerPlayer = getDisplayNamePerPlayer(saved);

      return domainMapper.toRoom(roomEntity,
          domainMapper.toRound(nextRound, playerId), displayNamePerPlayer);
    } catch (PessimisticLockingFailureException | PessimisticLockException |
             LockTimeoutException e) {
      throw new RoomLockedException(roomId);
    }
  }

  private Map<String, String> getDisplayNamePerPlayer(RoomEntity room) {
    var playerIds = room.getPlayerIds();
    return userProfileService.getDisplayNamePerPlayer(playerIds);
  }
}
