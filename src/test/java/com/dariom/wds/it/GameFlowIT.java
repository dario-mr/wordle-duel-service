package com.dariom.wds.it;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameFlowIT extends AbstractRedisTest {

  private static final String LANGUAGE = "IT";
  private static final String PLAYER_1_ID = "11111111-1111-1111-1111-111111111111";
  private static final String PLAYER_2_ID = "22222222-2222-2222-2222-222222222222";
  private static final String LOSING_GUESS = "FUOCO";

  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private IntegrationTestHelper itHelper;
  @Resource
  private RoundJpaRepository roundJpaRepository;

  @Test
  void playersProgressIndependentlyAndFiniteRoomClosesAfterBothFinish() throws Exception {
    var user1 = itHelper.createUser(PLAYER_1_ID, "player1@example.com", "John Smith");
    var user2 = itHelper.createUser(PLAYER_2_ID, "player2@example.com", "Bart Simpson");
    var player1Authentication = itHelper.userAuthentication(user1);
    var player2Authentication = itHelper.userAuthentication(user2);
    var roomId = createRoom(player1Authentication);

    itHelper.joinRoom(roomId, player2Authentication)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentRound.roundNumber").value(1))
        .andExpect(jsonPath("$.currentRound.guesses").isEmpty())
        .andExpect(jsonPath("$.currentRound.playerStatus").value("PLAYING"))
        .andExpect(jsonPath("$.currentRound.guessesByPlayerId").doesNotExist())
        .andExpect(jsonPath("$.currentRound.statusByPlayerId").doesNotExist());

    var round1Word = targetWord(1, roomId);
    var player1Round1 = itHelper.submitGuess(roomId, player1Authentication, round1Word)
        .andExpect(status().isOk());
    expectCompletedRound(player1Round1, "$.room", 1, "WON", 1, 0);
    expectCurrentRound(itHelper.startNextRound(roomId, player1Authentication)
        .andExpect(status().isOk()), "$", 2, "PLAYING", 1, 0);

    var player2BeforeGuess = itHelper.getRoom(roomId, player2Authentication)
        .andExpect(status().isOk());
    expectCurrentRound(player2BeforeGuess, "$", 1, "PLAYING", 1, 0);
    player2BeforeGuess.andExpect(jsonPath("$.currentRound.guesses").isEmpty());

    var player1Round2 = itHelper.submitGuess(roomId, player1Authentication, LOSING_GUESS)
        .andExpect(status().isOk());
    expectCompletedRound(player1Round2, "$.room", 2, "LOST", 1, 0);

    var round2Word = targetWord(2, roomId);
    player1Round2.andExpect(jsonPath("$.room.currentRound.solution").value(round2Word));
    expectCurrentRound(itHelper.startNextRound(roomId, player1Authentication)
        .andExpect(status().isOk()), "$", 3, "PLAYING", 1, 0);

    var player2Round2 = itHelper.submitGuess(roomId, player2Authentication, round1Word)
        .andExpect(status().isOk());
    expectCompletedRound(player2Round2, "$.room", 1, "WON", 1, 1);
    expectCurrentRound(itHelper.startNextRound(roomId, player2Authentication)
        .andExpect(status().isOk()), "$", 2, "PLAYING", 1, 1);

    expectCompletedRound(itHelper.submitGuess(roomId, player2Authentication, round2Word)
        .andExpect(status().isOk()), "$.room", 2, "WON", 1, 2);
    expectCurrentRound(itHelper.startNextRound(roomId, player2Authentication)
        .andExpect(status().isOk()), "$", 3, "PLAYING", 1, 2);

    for (var roundNumber = 3; roundNumber <= 5; roundNumber++) {
      var word = targetWord(roundNumber, roomId);
      itHelper.submitGuess(roomId, player1Authentication, word).andExpect(status().isOk());
      if (roundNumber < 5) {
        itHelper.startNextRound(roomId, player1Authentication).andExpect(status().isOk());
      }
    }

    var roomAfterPlayer1 = itHelper.getRoom(roomId, player1Authentication)
        .andExpect(status().isOk());
    roomAfterPlayer1.andExpectAll(
        jsonPath("$.status").value("IN_PROGRESS"),
        jsonPath("$.currentRound.roundNumber").value(5),
        jsonPath("$.currentRound.playerStatus").value("WON"),
        jsonPath("$.players[0].score").value(4),
        jsonPath("$.players[1].score").value(2));

    for (var roundNumber = 3; roundNumber <= 5; roundNumber++) {
      itHelper.submitGuess(roomId, player2Authentication, targetWord(roundNumber, roomId))
          .andExpect(status().isOk());
      if (roundNumber < 5) {
        itHelper.startNextRound(roomId, player2Authentication).andExpect(status().isOk());
      }
    }

    var roomAfterBothPlayers = itHelper.getRoom(roomId, player2Authentication)
        .andExpect(status().isOk());
    roomAfterBothPlayers.andExpectAll(
        jsonPath("$.status").value("CLOSED"),
        jsonPath("$.currentRound.roundNumber").value(5),
        jsonPath("$.currentRound.playerStatus").value("WON"),
        jsonPath("$.players[0].score").value(4),
        jsonPath("$.players[1].score").value(5));
  }

  private String createRoom(
      org.springframework.test.web.servlet.request.RequestPostProcessor authentication)
      throws Exception {
    var createRes = itHelper.createRoom(authentication, Map.of("language", LANGUAGE, "rounds", 5))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.currentRound").value(nullValue()))
        .andReturn();
    return objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();
  }

  private String targetWord(int roundNumber, String roomId) {
    return roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, roundNumber)
        .orElseThrow()
        .getTargetWord();
  }

  private static void expectCurrentRound(
      ResultActions result, String root, int roundNumber, String playerStatus,
      int player1Score, int player2Score) throws Exception {
    result.andExpectAll(
        jsonPath(root + ".currentRound.roundNumber").value(roundNumber),
        jsonPath(root + ".currentRound.playerStatus").value(playerStatus),
        jsonPath(root + ".currentRound.guesses", hasSize(0)),
        jsonPath(root + ".currentRound.guessesByPlayerId").doesNotExist(),
        jsonPath(root + ".currentRound.statusByPlayerId").doesNotExist(),
        jsonPath(root + ".players[0].score").value(player1Score),
        jsonPath(root + ".players[1].score").value(player2Score),
        jsonPath(root + ".currentRound.solution").doesNotExist());
  }

  private static void expectCompletedRound(
      ResultActions result, String root, int roundNumber, String playerStatus,
      int player1Score, int player2Score) throws Exception {
    result.andExpectAll(
        jsonPath(root + ".currentRound.roundNumber").value(roundNumber),
        jsonPath(root + ".currentRound.playerStatus").value(playerStatus),
        jsonPath(root + ".currentRound.guesses", hasSize(1)),
        jsonPath(root + ".currentRound.guessesByPlayerId").doesNotExist(),
        jsonPath(root + ".currentRound.statusByPlayerId").doesNotExist(),
        jsonPath(root + ".players[0].score").value(player1Score),
        jsonPath(root + ".players[1].score").value(player2Score));
  }
}
