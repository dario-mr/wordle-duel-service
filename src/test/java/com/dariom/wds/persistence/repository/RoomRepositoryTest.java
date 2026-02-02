package com.dariom.wds.persistence.repository;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomRepositoryTest {

  @Mock
  private RoomJpaRepository roomJpaRepository;
  @Mock
  private EntityManager entityManager;
  @Mock
  private Session session;
  @Mock
  private Connection connection;
  @Mock
  private Statement statement;
  @Mock
  private TypedQuery<RoomEntity> query;

  private RoomRepository repository;

  @BeforeEach
  void setUp() {
    repository = new RoomRepository(roomJpaRepository, entityManager);
  }

  @Test
  void findWithPlayersById_roomExists_delegatesToJpaRepository() {
    // Arrange
    var room = new RoomEntity();
    when(roomJpaRepository.findWithPlayersById("room-1")).thenReturn(Optional.of(room));

    // Act
    var result = repository.findWithPlayersById("room-1");

    // Assert
    assertThat(result).isSameAs(room);
    verify(roomJpaRepository).findWithPlayersById("room-1");
  }

  @Test
  void findWithPlayersById_roomMissing_throwsRoomNotFoundException() {
    // Arrange
    when(roomJpaRepository.findWithPlayersById(anyString())).thenReturn(Optional.empty());

    // Act / Assert
    assertThatThrownBy(() -> repository.findWithPlayersById("room-1"))
        .isInstanceOf(RoomNotFoundException.class)
        .hasMessageContaining("room-1");
  }

  @Test
  void save_roomProvided_delegatesToJpaRepository() {
    // Arrange
    var room = new RoomEntity();
    when(roomJpaRepository.save(room)).thenReturn(room);

    // Act
    var saved = repository.save(room);

    // Assert
    assertThat(saved).isSameAs(room);
    verify(roomJpaRepository).save(room);
  }

  @Test
  void findWithPlayersByIdForUpdate_roomExists_returnsFirstResult() throws Exception {
    // Arrange
    var room = new RoomEntity();

    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.execute(anyString())).thenThrow(new SQLException("not supported"));

    when(entityManager.createQuery(eq("select r from RoomEntity r where r.id = :id"),
        eq(RoomEntity.class)))
        .thenReturn(query);

    when(query.setParameter(eq("id"), any())).thenReturn(query);
    when(query.setLockMode(PESSIMISTIC_WRITE)).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(room));

    doAnswer(inv -> {
      inv.getArgument(0, Work.class).execute(connection);
      return null;
    }).when(session).doWork(any(Work.class));

    // Act
    var result = repository.findWithPlayersByIdForUpdate("room-1", Duration.ofMillis(50));

    // Assert
    assertThat(result).isSameAs(room);
    verify(entityManager).createQuery("select r from RoomEntity r where r.id = :id",
        RoomEntity.class);
    verify(query).setParameter("id", "room-1");
    verify(query).setLockMode(PESSIMISTIC_WRITE);
  }

  @Test
  void findWithPlayersByIdForUpdate_roomMissing_throwsRoomNotFoundException() throws Exception {
    // Arrange
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.execute(anyString())).thenThrow(new SQLException("not supported"));

    when(entityManager.createQuery(eq("select r from RoomEntity r where r.id = :id"),
        eq(RoomEntity.class)))
        .thenReturn(query);

    when(query.setParameter(eq("id"), any())).thenReturn(query);
    when(query.setLockMode(PESSIMISTIC_WRITE)).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of());

    doAnswer(inv -> {
      inv.getArgument(0, Work.class).execute(connection);
      return null;
    }).when(session).doWork(any(Work.class));

    // Act / Assert
    assertThatThrownBy(
        () -> repository.findWithPlayersByIdForUpdate("room-1", Duration.ofMillis(50)))
        .isInstanceOf(RoomNotFoundException.class)
        .hasMessageContaining("room-1");
  }
}
