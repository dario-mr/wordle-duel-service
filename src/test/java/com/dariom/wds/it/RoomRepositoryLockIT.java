package com.dariom.wds.it;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import jakarta.persistence.PessimisticLockException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.default_schema=public"
})
@AutoConfigureTestDatabase(replace = NONE)
@Import(RoomRepository.class)
class RoomRepositoryLockIT extends AbstractPostgresTest {

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RoomJpaRepository roomJpaRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void findWithPlayersByIdForUpdate_rowLocked_timesOutWithPessimisticLockingFailureException()
      throws Exception {
    // Arrange
    var roomId = "room-1";
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.setCurrentRoundNumber(null);
    room.addPlayer("p1");
    room.setPlayerScore("p1", 0);

    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> roomJpaRepository.saveAndFlush(room));

    var locked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();

    try {
      var holder = executor.submit(() -> new TransactionTemplate(transactionManager)
          .executeWithoutResult(status -> {
            roomRepository.findWithPlayersByIdForUpdate(roomId, Duration.ofSeconds(5));
            locked.countDown();
            await(release);
          }));

      assertThat(locked.await(5, SECONDS)).isTrue();

      // Act / Assert
      assertThatThrownBy(() -> new TransactionTemplate(transactionManager)
          .executeWithoutResult(status -> roomRepository.findWithPlayersByIdForUpdate(
              roomId,
              Duration.ofMillis(50)
          )))
          .isInstanceOfAny(PessimisticLockingFailureException.class,
              PessimisticLockException.class);

      release.countDown();
      holder.get(5, SECONDS);
    } finally {
      executor.shutdownNow();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(5, SECONDS)).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
