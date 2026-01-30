package com.dariom.wds.it;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.domain.LetterStatus;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminRoomDeleteIT {

  @Resource
  private MockMvc mockMvc;

  @Resource
  private TestUtil testUtil;

  @Resource
  private RoomJpaRepository roomJpaRepository;

  @Resource
  private EntityManager entityManager;

  @Test
  void deleteRoomById_userRole_returnsForbidden() throws Exception {
    // Arrange
    var userBearer = testUtil.userBearer();

    // Act / Assert
    mockMvc.perform(delete("/admin/rooms/{roomId}", "room-1")
            .header("Authorization", userBearer))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteRoomById_roomMissing_returnsNotFound() throws Exception {
    // Arrange
    var adminBearer = testUtil.adminBearer();

    // Act / Assert
    mockMvc.perform(delete("/admin/rooms/{roomId}", "missing-room")
            .header("Authorization", adminBearer))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteRoomById_adminRole_deletesRoomAndChildren() throws Exception {
    // Arrange
    var adminBearer = testUtil.adminBearer();
    var roomId = "room-cascade";

    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.addPlayer("p1");
    room.setPlayerScore("p1", 0);

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(1);
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(6);
    round.setRoundStatus(RoundStatus.PLAYING);
    round.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);

    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId("p1");
    guess.setWord("PIZZA");
    guess.setAttemptNumber(1);
    guess.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
    guess.setLetters(List.of(new LetterResultEmbeddable('P', LetterStatus.CORRECT)));

    round.addGuess(guess);
    room.addRound(round);

    roomJpaRepository.save(room);
    entityManager.flush();
    entityManager.clear();

    assertThat(count("select count(*) from rooms where id = ?", roomId)).isEqualTo(1);
    assertThat(count("select count(*) from room_players where room_id = ?", roomId)).isEqualTo(1);
    assertThat(count("select count(*) from rounds where room_id = ?", roomId)).isEqualTo(1);
    assertThat(count("select count(*) from guesses")).isEqualTo(1);
    assertThat(count("select count(*) from round_player_status")).isEqualTo(1);
    assertThat(count("select count(*) from guess_letters")).isEqualTo(1);

    // Act
    mockMvc.perform(delete("/admin/rooms/{roomId}", roomId)
            .header("Authorization", adminBearer))
        .andExpect(status().isNoContent());
    entityManager.flush();
    entityManager.clear();

    // Assert
    assertThat(count("select count(*) from rooms where id = ?", roomId)).isEqualTo(0);
    assertThat(count("select count(*) from room_players where room_id = ?", roomId)).isEqualTo(0);
    assertThat(count("select count(*) from rounds where room_id = ?", roomId)).isEqualTo(0);
    assertThat(count("select count(*) from guesses")).isEqualTo(0);
    assertThat(count("select count(*) from round_player_status")).isEqualTo(0);
    assertThat(count("select count(*) from guess_letters")).isEqualTo(0);
  }

  private long count(String sql, Object... params) {
    var query = entityManager.createNativeQuery(sql);
    for (var i = 0; i < params.length; i++) {
      query.setParameter(i + 1, params[i]);
    }
    return ((Number) query.getSingleResult()).longValue();
  }
}
