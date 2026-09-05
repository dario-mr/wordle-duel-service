package com.dariom.wds.service;

import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomPlayerEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

  public UserProfile toUserProfile(AppUserEntity user) {
    return new UserProfile(
        user.getId().toString(),
        user.getEmail(),
        user.getFullName(),
        user.getDisplayName(),
        user.getPictureUrl(),
        user.getCreatedOn()
    );
  }

  public Room toRoom(RoomEntity room, Round currentRound,
      Map<String, String> displayNamePerPlayer) {
    return new Room(
        room.getId(),
        room.getLanguage(),
        room.getConfiguredRounds(),
        room.getStatus(),
        toPlayers(room.getRoomPlayers(), room.getStatus(), displayNamePerPlayer),
        currentRound
    );
  }

  public Round toRound(RoundEntity round, String playerId) {
    if (round == null) {
      return null;
    }

    var guesses = round.getGuesses().stream()
        .filter(guess -> Objects.equals(guess.getPlayerId(), playerId))
        .sorted(comparingInt(GuessEntity::getAttemptNumber))
        .map(this::toGuess)
        .toList();

    return new Round(
        round.getRoundNumber(),
        round.getMaxAttempts(),
        guesses,
        round.getPlayerStatus(playerId),
        round.getRoundStatus(),
        round.getTargetWord()
    );
  }

  private List<Player> toPlayers(Set<RoomPlayerEntity> roomPlayers,
      RoomStatus roomStatus,
      Map<String, String> displayNamePerPlayer) {
    if (roomPlayers == null) {
      return emptyList();
    }

    return roomPlayers.stream()
        .sorted(comparing(RoomPlayerEntity::getPlayerId))
        .map(p -> new Player(
            p.getPlayerId(),
            p.getWins(),
            roomStatus == IN_PROGRESS
                ? p.getMatchScore() : null,
            displayNamePerPlayer == null ? null : displayNamePerPlayer.get(p.getPlayerId())
        ))
        .toList();
  }

  private Guess toGuess(GuessEntity guess) {
    var letters = guess.getLetters().stream()
        .map(l -> new LetterResult(l.getLetter(), l.getStatus()))
        .toList();

    return new Guess(guess.getWord(), letters, guess.getAttemptNumber());
  }
}
