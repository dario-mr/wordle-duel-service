package com.dariom.wds.service;

import static com.dariom.wds.util.UserUtils.normalizeFullName;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomPlayerEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

  public UserProfile toUserProfile(AppUserEntity user) {
    return new UserProfile(
        user.getId().toString(),
        user.getEmail(),
        user.getFullName(),
        normalizeFullName(user.getFullName()),
        user.getPictureUrl(),
        user.getCreatedOn()
    );
  }

  public Room toRoom(RoomEntity room, Round currentRound,
      Map<String, String> displayNamePerPlayer) {
    return new Room(
        room.getId(),
        room.getLanguage(),
        room.getStatus(),
        toPlayers(room.getRoomPlayers(), displayNamePerPlayer),
        currentRound
    );
  }

  public Round toRound(RoundEntity round) {
    if (round == null) {
      return null;
    }

    var guessesByPlayerId = round.getGuesses().stream()
        .sorted(comparingInt(GuessEntity::getAttemptNumber))
        .collect(groupingBy(
            GuessEntity::getPlayerId,
            mapping(this::toGuess, toList())
        ));

    return new Round(
        round.getRoundNumber(),
        round.getMaxAttempts(),
        guessesByPlayerId,
        Map.copyOf(round.getStatusByPlayerId()),
        round.getRoundStatus(),
        round.getTargetWord()
    );
  }

  private List<Player> toPlayers(Set<RoomPlayerEntity> roomPlayers,
      Map<String, String> displayNamePerPlayer) {
    if (roomPlayers == null) {
      return emptyList();
    }

    return roomPlayers.stream()
        .sorted(comparing(RoomPlayerEntity::getPlayerId))
        .map(p -> new Player(
            p.getPlayerId(),
            p.getScore(),
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
