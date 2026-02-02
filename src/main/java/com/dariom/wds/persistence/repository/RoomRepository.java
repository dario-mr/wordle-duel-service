package com.dariom.wds.persistence.repository;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RoomRepository {

  private final RoomJpaRepository roomJpaRepository;
  private final EntityManager entityManager;

  public RoomEntity findWithPlayersById(String id) {
    return roomJpaRepository.findWithPlayersById(id)
        .orElseThrow(() -> new RoomNotFoundException(id));
  }

  public List<RoomEntity> findWithPlayersByPlayerId(String playerId) {
    return roomJpaRepository.findWithPlayersByPlayerId(playerId);
  }

  public RoomEntity findWithPlayersByIdForUpdate(String id, Duration lockTimeout) {
    return findWithPlayersByIdForUpdateOptional(id, lockTimeout)
        .orElseThrow(() -> new RoomNotFoundException(id));
  }

  public RoomEntity save(RoomEntity room) {
    return roomJpaRepository.save(room);
  }

  public void delete(RoomEntity room) {
    roomJpaRepository.delete(room);
  }

  public long deleteInactive(Instant cutoff) {
    return roomJpaRepository.deleteInactive(cutoff);
  }

  private Optional<RoomEntity> findWithPlayersByIdForUpdateOptional(String id,
      Duration lockTimeout) {
    configureLockTimeout(lockTimeout);

    var query = entityManager
        .createQuery("select r from RoomEntity r where r.id = :id", RoomEntity.class)
        .setParameter("id", id)
        .setLockMode(PESSIMISTIC_WRITE);

    var room = query.getResultList().stream().findFirst();

    // Load players after the lock has been acquired to avoid Hibernate follow-on locking
    room.ifPresent(r -> r.getRoomPlayers().size());

    return room;
  }

  private void configureLockTimeout(Duration lockTimeout) {
    if (lockTimeout == null) {
      return;
    }

    var timeoutMs = lockTimeout.toMillis();
    if (timeoutMs <= 0) {
      return;
    }

    entityManager.unwrap(Session.class).doWork(connection -> {
      try (var statement = connection.createStatement()) {
        statement.execute("set local lock_timeout = %d".formatted(timeoutMs));
      } catch (SQLException ignored) {
        // Best-effort: some DBs (e.g. H2) don't support Postgres lock_timeout
        // todo remove once confirmed working
        log.warn("Failed to set local lock_timeout, ignoring it");
      }
    });
  }
}
