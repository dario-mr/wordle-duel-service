package com.dariom.wds.service.round;

import static com.dariom.wds.service.round.validation.RoomAccessValidator.validateRoomStatus;

import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.service.DomainMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoundService {

  private final RoomJpaRepository roomJpaRepository;
  private final RoundJpaRepository roundJpaRepository;
  private final DomainMapper domainMapper;
  private final RoundLifecycleService roundLifecycleService;
  private final GuessSubmissionService guessSubmissionService;
  private final RoundFinisher roundFinisher;

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

  @Transactional
  public Room handleGuess(String roomId, String playerId, String guess) {
    var roomEntity = findRoom(roomId);
    validateRoomStatus(playerId, domainMapper.toRoom(roomEntity, null));

    var roundEntity = roundLifecycleService.ensureActiveRound(roomEntity);
    guessSubmissionService.applyGuess(roomId, playerId, guess, roomEntity, roundEntity);

    if (!roundEntity.isFinished() && roundFinisher.isRoundFinished(roomEntity, roundEntity)) {
      roundFinisher.finishRound(roundEntity, roomEntity);
    }

    roomJpaRepository.save(roomEntity);
    return domainMapper.toRoom(roomEntity, domainMapper.toRound(roundEntity));
  }

  private RoomEntity findRoom(String roomId) {
    return roomJpaRepository.findWithPlayersAndScoresById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));
  }
}
