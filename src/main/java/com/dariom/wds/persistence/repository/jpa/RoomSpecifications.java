package com.dariom.wds.persistence.repository.jpa;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomRounds;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomPlayerEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public class RoomSpecifications {

  private RoomSpecifications() {
  }

  public static Specification<RoomEntity> statusIn(Collection<RoomStatus> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return null;
    }
    return (root, query, cb) -> root.get("status").in(statuses);
  }

  public static Specification<RoomEntity> languageEquals(Language language) {
    if (language == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("language"), language);
  }

  public static Specification<RoomEntity> roundsEquals(RoomRounds rounds) {
    if (rounds == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("configuredRounds"), rounds);
  }

  public static Specification<RoomEntity> roomIdContains(String roomId) {
    if (isBlank(roomId)) {
      return null;
    }
    return (root, query, cb) -> cb.like(root.get("id"), "%" + roomId.strip() + "%");
  }

  public static Specification<RoomEntity> createdAtOn(LocalDate date) {
    return instantOn("createdAt", date);
  }

  public static Specification<RoomEntity> lastUpdatedAtOn(LocalDate date) {
    return instantOn("lastUpdatedAt", date);
  }

  public static Specification<RoomEntity> playerMatches(String search, Set<String> matchingPlayerIds) {
    if (isBlank(search)) {
      return null;
    }
    var pattern = "%" + search.strip().toLowerCase() + "%";
    return (root, query, cb) -> {
      query.distinct(true);
      var playerId = root.join("roomPlayers").get("id").<String>get("playerId");
      var playerIdMatches = cb.like(cb.lower(playerId), pattern);
      if (matchingPlayerIds.isEmpty()) {
        return playerIdMatches;
      }
      return cb.or(playerIdMatches, cb.lower(playerId).in(matchingPlayerIds));
    };
  }

  public static Specification<RoomEntity> firstPlayerFullNameSort(Sort.Direction direction) {
    return (root, query, cb) -> {
      if (query.getResultType() == Long.class || query.getResultType() == long.class) {
        return null;
      }

      var playerNameQuery = query.subquery(String.class);
      var playerRoot = playerNameQuery.from(RoomPlayerEntity.class);
      var userRoot = playerNameQuery.from(AppUserEntity.class);
      var playerId = playerRoot.get("id").<String>get("playerId");
      var playerName = userRoot.get("fullName").<String>as(String.class);
      playerNameQuery.select(cb.least(cb.coalesce(cb.lower(playerName), cb.lower(playerId))));
      playerNameQuery.where(
          cb.equal(playerRoot.get("room").get("id"), root.get("id")),
          cb.equal(playerId, userRoot.get("id").as(String.class))
      );
      var playerNameOrder = direction.isAscending()
          ? cb.asc(playerNameQuery)
          : cb.desc(playerNameQuery);
      query.orderBy(playerNameOrder, cb.asc(root.get("id")));
      return null;
    };
  }

  private static Specification<RoomEntity> instantOn(String property, LocalDate date) {
    if (date == null) {
      return null;
    }

    var start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    var end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    return (root, query, cb) -> cb.and(
        cb.greaterThanOrEqualTo(root.<Instant>get(property), start),
        cb.lessThan(root.<Instant>get(property), end)
    );
  }
}
