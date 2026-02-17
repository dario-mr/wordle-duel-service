package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
class GuessSubmissionConcurrencyIT {

  private static final String LANGUAGE = "IT";
  private static final String PLAYER_1_ID = "11111111-1111-1111-1111-111111111111";
  private static final String PLAYER_2_ID = "22222222-2222-2222-2222-222222222222";
  private static final String WORD = "FUOCO";

  @Resource
  private IntegrationTestHelper itHelper;
  @Resource
  private ObjectMapper objectMapper;

  private String player1Bearer;
  private String player2Bearer;

  @BeforeEach
  void setUp() {
    var user1 = itHelper.createUser(PLAYER_1_ID, "player1@example.com", "John Smith");
    var user2 = itHelper.createUser(PLAYER_2_ID, "player2@example.com", "Bart Simpson");
    player1Bearer = itHelper.bearer(user1);
    player2Bearer = itHelper.bearer(user2);
  }

  @Test
  void submitGuess_concurrentByBothPlayers_bothSucceed() throws Exception {
    // Arrange
    var roomId = createRoomAndJoin();

    var latch = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    try {
      // Act — both players submit guesses simultaneously
      var future1 = CompletableFuture.supplyAsync(() -> {
        await(latch);
        return submitGuess(roomId, player1Bearer);
      }, executor);

      var future2 = CompletableFuture.supplyAsync(() -> {
        await(latch);
        return submitGuess(roomId, player2Bearer);
      }, executor);

      latch.countDown();

      var result1 = future1.get(10, SECONDS);
      var result2 = future2.get(10, SECONDS);

      // Assert — both should succeed
      assertThat(result1.getResponse().getStatus()).isEqualTo(200);
      assertThat(result2.getResponse().getStatus()).isEqualTo(200);

      // Verify final room state has both guesses
      var roomJson = getRoom(roomId);
      var guessesByPlayerId = roomJson.get("currentRound").get("guessesByPlayerId");
      assertThat(guessesByPlayerId.has(PLAYER_1_ID)).isTrue();
      assertThat(guessesByPlayerId.has(PLAYER_2_ID)).isTrue();
      assertThat(guessesByPlayerId.get(PLAYER_1_ID)).hasSize(1);
      assertThat(guessesByPlayerId.get(PLAYER_2_ID)).hasSize(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void handleReady_concurrentByBothPlayers_startsExactlyOneNewRound() throws Exception {
    // Arrange — complete a round (both players guess and lose with max-attempts=1)
    var roomId = createRoomAndJoin();
    itHelper.submitGuess(roomId, player1Bearer, WORD).andExpect(status().isOk());
    itHelper.submitGuess(roomId, player2Bearer, WORD).andExpect(status().isOk());

    var latch = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    try {
      // Act — both players call ready simultaneously
      var future1 = CompletableFuture.supplyAsync(() -> {
        await(latch);
        return ready(roomId, player1Bearer, 1);
      }, executor);

      var future2 = CompletableFuture.supplyAsync(() -> {
        await(latch);
        return ready(roomId, player2Bearer, 1);
      }, executor);

      latch.countDown();

      var result1 = future1.get(10, SECONDS);
      var result2 = future2.get(10, SECONDS);

      // Assert — both should succeed
      assertThat(result1.getResponse().getStatus()).isEqualTo(200);
      assertThat(result2.getResponse().getStatus()).isEqualTo(200);

      // Verify exactly one new round was started (round 2, not 3)
      var roomJson = getRoom(roomId);
      var currentRound = roomJson.get("currentRound");
      assertThat(currentRound.get("roundNumber").asInt()).isEqualTo(2);
      assertThat(currentRound.get("roundStatus").asText()).isEqualTo("PLAYING");
    } finally {
      executor.shutdownNow();
    }
  }

  private String createRoomAndJoin() throws Exception {
    var createRes = itHelper.createRoom(player1Bearer, Map.of("language", LANGUAGE))
        .andExpect(status().isCreated())
        .andReturn();

    var roomId = objectMapper.readTree(createRes.getResponse().getContentAsString())
        .get("id").asText();

    itHelper.joinRoom(roomId, player2Bearer).andExpect(status().isOk());
    return roomId;
  }

  private JsonNode getRoom(String roomId) throws Exception {
    var result = itHelper.getRoom(roomId, player1Bearer)
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private MvcResult submitGuess(String roomId, String bearer) {
    try {
      return itHelper.submitGuess(roomId, bearer, WORD)
          .andReturn();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private MvcResult ready(String roomId, String bearer, int roundNumber) {
    try {
      return itHelper.ready(roomId, bearer, roundNumber)
          .andReturn();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(10, SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
