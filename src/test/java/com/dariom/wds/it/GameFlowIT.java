package com.dariom.wds.it;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class GameFlowIT {

  private static final String BASE_URL = "/api/v1/rooms";
  private static final String LANGUAGE = "IT";
  private static final String PLAYER_ID_1 = "p1";
  private static final String PLAYER_ID_2 = "p2";
  private static final String WORD = "FUOCO";
  private static final int MAX_ATTEMPTS = 1;

  @Resource
  private MockMvc mockMvc;

  @Resource
  private ObjectMapper objectMapper;

  @Test
  void roundFinishesWhenBothPlayersDone() throws Exception {
    // player1 creates the room
    var roomId = createRoom(PLAYER_ID_1);

    // player2 joins the room
    var joinRoomRes = joinRoom(roomId, PLAYER_ID_2).andExpect(status().isOk());
    expectRoomInProgress(joinRoomRes, "$", roomId, 1, false, "PLAYING", "PLAYING");
    joinRoomRes.andExpect(jsonPath("$.currentRound.guessesByPlayerId").value(anEmptyMap()));

    // player1 submits guess
    var player1GuessRes = submitGuess(roomId, PLAYER_ID_1, WORD).andExpect(status().isOk());
    expectRoomInProgress(player1GuessRes, "$.room", roomId, 1, false, "LOST", "PLAYING");
    expectSingleGuess(player1GuessRes, "$.room", PLAYER_ID_1, WORD, 1);
    expectNoGuesses(player1GuessRes, "$.room", PLAYER_ID_2);

    // player2 submits guess
    var player2GuessRes = submitGuess(roomId, PLAYER_ID_2, WORD).andExpect(status().isOk());
    expectRoomInProgress(player2GuessRes, "$.room", roomId, 1, true, "LOST", "LOST");
    expectSingleGuess(player2GuessRes, "$.room", PLAYER_ID_1, WORD, 1);
    expectSingleGuess(player2GuessRes, "$.room", PLAYER_ID_2, WORD, 1);

    // round is finished (both players lost)
    var roomRes = getRoom(roomId).andExpect(status().isOk());
    expectRoomInProgress(roomRes, "$", roomId, 1, true, "LOST", "LOST");
    expectGuessWordOnly(roomRes, "$", PLAYER_ID_1, WORD);
    expectGuessWordOnly(roomRes, "$", PLAYER_ID_2, WORD);

    // submitting another guess starts a new round
    var nextRoundGuessRes = submitGuess(roomId, PLAYER_ID_1, WORD).andExpect(status().isOk());
    expectRoomInProgress(nextRoundGuessRes, "$.room", roomId, 2, false, "LOST", "PLAYING");
    expectSingleGuess(nextRoundGuessRes, "$.room", PLAYER_ID_1, WORD, 1);
    expectNoGuesses(nextRoundGuessRes, "$.room", PLAYER_ID_2);
  }

  private String createRoom(String playerId) throws Exception {
    var createReq = Map.of("playerId", playerId, "language", LANGUAGE);

    var createRes = postJson(BASE_URL, createReq)
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id", not(emptyOrNullString())))
        .andExpect(jsonPath("$.language").value(LANGUAGE))
        .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
        .andExpect(jsonPath("$.players", contains(playerId)))
        .andExpect(jsonPath("$.scores.p1").value(0))
        .andExpect(jsonPath("$.scores.p2").doesNotExist())
        .andExpect(jsonPath("$.currentRound").value(nullValue()))
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    return objectMapper.readTree(createdJson).get("id").asText();
  }

  private ResultActions joinRoom(String roomId, String playerId) throws Exception {
    return postJson(BASE_URL + "/{roomId}/join", Map.of("playerId", playerId), roomId);
  }

  private ResultActions submitGuess(String roomId, String playerId, String word) throws Exception {
    return postJson(BASE_URL + "/{roomId}/guess", Map.of("playerId", playerId, "word", word),
        roomId);
  }

  private ResultActions getRoom(String roomId) throws Exception {
    return mockMvc.perform(get(BASE_URL + "/{roomId}", roomId));
  }

  private ResultActions postJson(String urlTemplate, Object body, Object... uriVars)
      throws Exception {
    return mockMvc.perform(post(urlTemplate, uriVars)
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)));
  }

  private void expectRoomInProgress(
      ResultActions res,
      String root,
      String roomId,
      int roundNumber,
      boolean finished,
      String p1Status,
      String p2Status) throws Exception {
    res.andExpectAll(
        jsonPath(path(root, ".id")).value(roomId),
        jsonPath(path(root, ".language")).value(LANGUAGE),
        jsonPath(path(root, ".status")).value("IN_PROGRESS"),
        jsonPath(path(root, ".players"), contains(PLAYER_ID_1, PLAYER_ID_2)),
        jsonPath(path(root, ".scores.p1")).value(0),
        jsonPath(path(root, ".scores.p2")).value(0),
        jsonPath(path(root, ".currentRound.roundNumber")).value(roundNumber),
        jsonPath(path(root, ".currentRound.maxAttempts")).value(MAX_ATTEMPTS),
        jsonPath(path(root, ".currentRound.finished")).value(finished),
        jsonPath(path(root, ".currentRound.statusByPlayerId.p1")).value(p1Status),
        jsonPath(path(root, ".currentRound.statusByPlayerId.p2")).value(p2Status));
  }

  private void expectSingleGuess(
      ResultActions res,
      String root,
      String playerId,
      String word,
      int attemptNumber) throws Exception {
    res.andExpectAll(
        jsonPath(path(root, ".currentRound.guessesByPlayerId." + playerId), hasSize(1)),
        jsonPath(path(root, ".currentRound.guessesByPlayerId." + playerId + "[0].word")).value(
            word),
        jsonPath(
            path(root, ".currentRound.guessesByPlayerId." + playerId + "[0].attemptNumber")).value(
            attemptNumber),
        jsonPath(path(root, ".currentRound.guessesByPlayerId." + playerId + "[0].letters"),
            hasSize(word.length())));

    expectGuessLetters(res, root, playerId, 0, word);
  }

  private void expectGuessWordOnly(ResultActions res, String root, String playerId, String word)
      throws Exception {
    res.andExpectAll(
        jsonPath(path(root, ".currentRound.guessesByPlayerId." + playerId), hasSize(1)),
        jsonPath(path(root, ".currentRound.guessesByPlayerId." + playerId + "[0].word"))
            .value(word));
  }

  private void expectNoGuesses(ResultActions res, String root, String playerId) throws Exception {
    res.andExpect(
        jsonPath(path(root, ".currentRound.guessesByPlayerId." + playerId)).doesNotExist());
  }

  private void expectGuessLetters(
      ResultActions res,
      String root,
      String playerId,
      int guessIndex,
      String word) throws Exception {
    for (var i = 0; i < word.length(); i++) {
      res.andExpect(jsonPath(
          path(root, ".currentRound.guessesByPlayerId." + playerId + "[" + guessIndex
              + "].letters[" + i + "].letter")).value(String.valueOf(word.charAt(i))));

      res.andExpect(jsonPath(
          path(root, ".currentRound.guessesByPlayerId." + playerId + "[" + guessIndex
              + "].letters[" + i + "].status")).value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))));
    }
  }

  private static String path(String root, String suffix) {
    return root + suffix;
  }
}
