package com.dariom.wds.it;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class GameFlowIT {

  private static final String LANGUAGE = "IT";
  private static final String PLAYER_1_ID = "11111111-1111-1111-1111-111111111111";
  private static final String PLAYER_2_ID = "22222222-2222-2222-2222-222222222222";
  private static final String WORD = "FUOCO";
  private static final int MAX_ATTEMPTS = 1;

  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private IntegrationTestHelper itHelper;

  @Test
  void roundFinishesWhenBothPlayersDone() throws Exception {
    // create users in DB
    var user1 = itHelper.createUser(PLAYER_1_ID, "player1@example.com", "John Smith");
    var user2 = itHelper.createUser(PLAYER_2_ID, "player2@example.com", "Bart Simpson");

    var player1Bearer = itHelper.bearer(user1);
    var player2Bearer = itHelper.bearer(user2);

    // player1 creates the room
    var roomId = createRoom(player1Bearer, PLAYER_1_ID);

    // player2 joins the room
    var joinRoomRes = itHelper.joinRoom(roomId, player2Bearer).andExpect(status().isOk());
    expectRoomInProgress(joinRoomRes, "$", roomId, 1, "PLAYING", "PLAYING", "PLAYING");
    joinRoomRes.andExpect(jsonPath("$.currentRound.solution").doesNotExist());
    joinRoomRes.andExpect(jsonPath("$.currentRound.guessesByPlayerId").value(anEmptyMap()));

    // ready not allowed while round is still playing
    itHelper.ready(roomId, player1Bearer, 1)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROUND_NOT_ENDED"))
        .andExpect(jsonPath("$.message", not(emptyOrNullString())));

    // player1 submits guess
    var player1GuessRes = itHelper.submitGuess(roomId, player1Bearer, WORD)
        .andExpect(status().isOk());
    expectRoomInProgress(player1GuessRes, "$.room", roomId, 1, "PLAYING", "LOST", "PLAYING");
    player1GuessRes.andExpect(
        jsonPath("$.room.currentRound.solution").value(not(emptyOrNullString())));
    player1GuessRes.andExpect(
        jsonPath("$.room.currentRound.solution").value(hasLength(WORD.length())));
    expectSingleGuess(player1GuessRes, "$.room", PLAYER_1_ID, WORD, 1);
    expectNoGuesses(player1GuessRes, "$.room", PLAYER_2_ID);

    // player2 still can't see solution
    var roomForP2AfterP1GuessRes = itHelper.getRoom(roomId, player2Bearer)
        .andExpect(status().isOk());
    expectRoomInProgress(roomForP2AfterP1GuessRes, "$", roomId, 1, "PLAYING", "LOST", "PLAYING");
    roomForP2AfterP1GuessRes.andExpect(jsonPath("$.currentRound.solution").doesNotExist());

    // player2 submits guess
    var player2GuessRes = itHelper.submitGuess(roomId, player2Bearer, WORD)
        .andExpect(status().isOk());
    expectRoomInProgress(player2GuessRes, "$.room", roomId, 1, "ENDED", "LOST", "LOST");
    player2GuessRes.andExpect(
        jsonPath("$.room.currentRound.solution").value(not(emptyOrNullString())));
    player2GuessRes.andExpect(
        jsonPath("$.room.currentRound.solution").value(hasLength(WORD.length())));
    expectSingleGuess(player2GuessRes, "$.room", PLAYER_1_ID, WORD, 1);
    expectSingleGuess(player2GuessRes, "$.room", PLAYER_2_ID, WORD, 1);

    // round is finished (both players lost)
    var roomRes = itHelper.getRoom(roomId, player1Bearer).andExpect(status().isOk());
    expectRoomInProgress(roomRes, "$", roomId, 1, "ENDED", "LOST", "LOST");
    roomRes.andExpect(jsonPath("$.currentRound.solution").value(not(emptyOrNullString())));
    roomRes.andExpect(jsonPath("$.currentRound.solution").value(hasLength(WORD.length())));
    expectGuessWordOnly(roomRes, "$", PLAYER_1_ID, WORD);
    expectGuessWordOnly(roomRes, "$", PLAYER_2_ID, WORD);

    // submitting another guess is illegal once round is finished
    itHelper.submitGuess(roomId, player1Bearer, WORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ROUND_FINISHED"))
        .andExpect(jsonPath("$.message", not(emptyOrNullString())));

    // room still has the same finished round
    var roomAfterIllegalGuessRes = itHelper.getRoom(roomId, player1Bearer)
        .andExpect(status().isOk());
    expectRoomInProgress(roomAfterIllegalGuessRes, "$", roomId, 1, "ENDED", "LOST", "LOST");
    roomAfterIllegalGuessRes.andExpect(
        jsonPath("$.currentRound.solution").value(not(emptyOrNullString())));

    // player1 ready (idempotent)
    var readyP1Res = itHelper.ready(roomId, player1Bearer, 1).andExpect(status().isOk());
    expectRoomInProgress(readyP1Res, "$", roomId, 1, "ENDED", "READY", "LOST");
    readyP1Res.andExpect(jsonPath("$.currentRound.solution").value(not(emptyOrNullString())));

    itHelper.ready(roomId, player1Bearer, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath(
            path("$", ".currentRound.statusByPlayerId['" + PLAYER_1_ID + "']")).value("READY"));

    // wrong round number rejected
    itHelper.ready(roomId, player2Bearer, 2)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROUND_NOT_CURRENT"))
        .andExpect(jsonPath("$.message", not(emptyOrNullString())));

    // player2 ready triggers next round start
    var readyP2Res = itHelper.ready(roomId, player2Bearer, 1).andExpect(status().isOk());
    expectRoomInProgress(readyP2Res, "$", roomId, 2, "PLAYING", "PLAYING", "PLAYING");
    readyP2Res.andExpect(jsonPath("$.currentRound.solution").doesNotExist());
    readyP2Res.andExpect(jsonPath("$.currentRound.guessesByPlayerId").value(anEmptyMap()));
  }

  private String createRoom(String bearer, String expectedPlayerId)
      throws Exception {
    var createReq = Map.of("language", LANGUAGE);

    var createRes = itHelper.createRoom(bearer, createReq)
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id", not(emptyOrNullString())))
        .andExpect(jsonPath("$.language").value(LANGUAGE))
        .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
        .andExpect(jsonPath("$.players[*].id", contains(expectedPlayerId)))
        .andExpect(jsonPath("$.players[0].score").value(0))
        .andExpect(jsonPath("$.players", hasSize(1)))
        .andExpect(jsonPath("$.currentRound").value(nullValue()))
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    return objectMapper.readTree(createdJson).get("id").asText();
  }

  private void expectRoomInProgress(
      ResultActions res,
      String root,
      String roomId,
      int roundNumber,
      String roundStatus,
      String p1Status,
      String p2Status) throws Exception {
    res.andExpectAll(
        jsonPath(path(root, ".id")).value(roomId),
        jsonPath(path(root, ".language")).value(LANGUAGE),
        jsonPath(path(root, ".status")).value("IN_PROGRESS"),
        jsonPath(path(root, ".players[*].id"), contains(PLAYER_1_ID, PLAYER_2_ID)),
        jsonPath(path(root, ".players[0].score")).value(0),
        jsonPath(path(root, ".players[1].score")).value(0),
        jsonPath(path(root, ".players[0].displayName")).value("John"),
        jsonPath(path(root, ".players[1].displayName")).value("Bart"),
        jsonPath(path(root, ".currentRound.roundNumber")).value(roundNumber),
        jsonPath(path(root, ".currentRound.maxAttempts")).value(MAX_ATTEMPTS),
        jsonPath(path(root, ".currentRound.roundStatus")).value(roundStatus),
        jsonPath(mapPath(root, ".currentRound.statusByPlayerId", PLAYER_1_ID)).value(p1Status),
        jsonPath(mapPath(root, ".currentRound.statusByPlayerId", PLAYER_2_ID)).value(p2Status));
  }

  private void expectSingleGuess(
      ResultActions res,
      String root,
      String playerId,
      String word,
      int attemptNumber) throws Exception {
    var guesses = mapPath(root, ".currentRound.guessesByPlayerId", playerId);

    res.andExpectAll(
        jsonPath(guesses, hasSize(1)),
        jsonPath(guesses + "[0].word").value(word),
        jsonPath(guesses + "[0].attemptNumber").value(attemptNumber),
        jsonPath(guesses + "[0].letters", hasSize(word.length())));

    expectGuessLetters(res, root, playerId, 0, word);
  }

  private void expectGuessWordOnly(ResultActions res, String root, String playerId, String word)
      throws Exception {
    var guesses = mapPath(root, ".currentRound.guessesByPlayerId", playerId);

    res.andExpectAll(
        jsonPath(guesses, hasSize(1)),
        jsonPath(guesses + "[0].word").value(word));
  }

  private void expectNoGuesses(ResultActions res, String root, String playerId) throws Exception {
    res.andExpect(
        jsonPath(mapPath(root, ".currentRound.guessesByPlayerId", playerId)).doesNotExist());
  }

  private void expectGuessLetters(
      ResultActions res,
      String root,
      String playerId,
      int guessIndex,
      String word) throws Exception {
    var guesses = mapPath(root, ".currentRound.guessesByPlayerId", playerId);

    for (var i = 0; i < word.length(); i++) {
      res.andExpect(jsonPath(guesses + "[" + guessIndex + "].letters[" + i + "].letter")
          .value(String.valueOf(word.charAt(i))));

      res.andExpect(jsonPath(guesses + "[" + guessIndex + "].letters[" + i + "].status")
          .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))));
    }
  }

  private static String mapPath(String root, String mapField, String key) {
    return root + mapField + "['" + key + "']";
  }

  private static String path(String root, String suffix) {
    return root + suffix;
  }

}
