package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
class GuessSubmissionConcurrencyIT extends AbstractRedisTest {

  private static final String LANGUAGE = "IT";
  private static final String PLAYER_1_ID = "11111111-1111-1111-1111-111111111111";
  private static final String PLAYER_2_ID = "22222222-2222-2222-2222-222222222222";
  private static final String WORD = "FUOCO";

  @Resource
  private IntegrationTestHelper itHelper;
  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private RoundJpaRepository roundJpaRepository;

  private RequestPostProcessor player1Authentication;
  private RequestPostProcessor player2Authentication;

  @BeforeEach
  void setUp() {
    var user1 = itHelper.createUser(PLAYER_1_ID, "player1@example.com", "John Smith");
    var user2 = itHelper.createUser(PLAYER_2_ID, "player2@example.com", "Bart Simpson");
    player1Authentication = itHelper.userAuthentication(user1);
    player2Authentication = itHelper.userAuthentication(user2);
  }

  @Test
  void submitGuess_concurrentByBothPlayers_completesOneRound() throws Exception {
    var roomId = createRoomAndJoin();
    var latch = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    try {
      var future1 = CompletableFuture.supplyAsync(() -> {
        await(latch);
        return submitGuess(roomId, player1Authentication);
      }, executor);
      var future2 = CompletableFuture.supplyAsync(() -> {
        await(latch);
        return submitGuess(roomId, player2Authentication);
      }, executor);
      latch.countDown();

      assertThat(future1.get(10, SECONDS).getResponse().getStatus()).isEqualTo(200);
      assertThat(future2.get(10, SECONDS).getResponse().getStatus()).isEqualTo(200);
    } finally {
      executor.shutdownNow();
    }

    var round1 = roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, 1)
        .orElseThrow();
    assertThat(roundJpaRepository.count()).isEqualTo(1);
    assertThat(round1.getPlayerStatus(PLAYER_1_ID)).isNotEqualTo(PLAYING);
    assertThat(round1.getPlayerStatus(PLAYER_2_ID)).isNotEqualTo(PLAYING);
  }

  private String createRoomAndJoin() throws Exception {
    var createRes = itHelper.createRoom(player1Authentication,
            Map.of("language", LANGUAGE, "rounds", 5))
        .andExpect(status().isCreated())
        .andReturn();
    var roomId = objectMapper.readTree(createRes.getResponse().getContentAsString())
        .get("id").asText();
    itHelper.joinRoom(roomId, player2Authentication).andExpect(status().isOk());
    return roomId;
  }

  private MvcResult submitGuess(String roomId, RequestPostProcessor authentication) {
    try {
      return itHelper.submitGuess(roomId, authentication, WORD).andReturn();
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
