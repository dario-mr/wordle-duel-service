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

@SpringBootTest
@AutoConfigureMockMvc
class GameFlowIntegrationTest {

  private static final String BASE_URL = "/api/v1/rooms";
  private static final String PLAYER_ID_1 = "p1";
  private static final String PLAYER_ID_2 = "p2";

  @Resource
  private MockMvc mockMvc;

  @Resource
  private ObjectMapper objectMapper;

  @Test
  void roundFinishesWhenBothPlayersDone() throws Exception {
    // player1 creates the room
    var createReq = Map.of("playerId", PLAYER_ID_1, "language", "IT");
    var createRes = mockMvc.perform(post(BASE_URL)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id", not(emptyOrNullString())))
        .andExpect(jsonPath("$.language").value("IT"))
        .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
        .andExpect(jsonPath("$.players", contains(PLAYER_ID_1)))
        .andExpect(jsonPath("$.scores.p1").value(0))
        .andExpect(jsonPath("$.scores.p2").doesNotExist())
        .andExpect(jsonPath("$.currentRound").value(nullValue()))
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    var roomId = objectMapper.readTree(createdJson).get("id").asText();

    // player2 joins the room
    mockMvc.perform(post(BASE_URL + "/{roomId}/join", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", PLAYER_ID_2))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(roomId))
        .andExpect(jsonPath("$.language").value("IT"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.players", contains(PLAYER_ID_1, PLAYER_ID_2)))
        .andExpect(jsonPath("$.scores.p1").value(0))
        .andExpect(jsonPath("$.scores.p2").value(0))
        .andExpect(jsonPath("$.currentRound.roundNumber").value(1))
        .andExpect(jsonPath("$.currentRound.maxAttempts").value(1))
        .andExpect(jsonPath("$.currentRound.finished").value(false))
        .andExpect(jsonPath("$.currentRound.statusByPlayerId.p1").value("PLAYING"))
        .andExpect(jsonPath("$.currentRound.statusByPlayerId.p2").value("PLAYING"))
        .andExpect(jsonPath("$.currentRound.guessesByPlayerId").value(anEmptyMap()));

    // player1 submits guess
    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", PLAYER_ID_1, "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.id").value(roomId))
        .andExpect(jsonPath("$.room.language").value("IT"))
        .andExpect(jsonPath("$.room.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.room.players", contains(PLAYER_ID_1, PLAYER_ID_2)))
        .andExpect(jsonPath("$.room.scores.p1").value(0))
        .andExpect(jsonPath("$.room.scores.p2").value(0))
        .andExpect(jsonPath("$.room.currentRound.roundNumber").value(1))
        .andExpect(jsonPath("$.room.currentRound.maxAttempts").value(1))
        .andExpect(jsonPath("$.room.currentRound.finished").value(false))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("PLAYING"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1", hasSize(1)))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].word").value("FUOCO"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].attemptNumber").value(1))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters", hasSize(5)))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[0].letter").value("F"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[1].letter").value("U"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[2].letter").value("O"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[3].letter").value("C"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[4].letter").value("O"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[0].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[1].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[2].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[3].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters[4].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2").doesNotExist());

    // player2 submits guess
    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", PLAYER_ID_2, "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.id").value(roomId))
        .andExpect(jsonPath("$.room.language").value("IT"))
        .andExpect(jsonPath("$.room.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.room.players", contains(PLAYER_ID_1, PLAYER_ID_2)))
        .andExpect(jsonPath("$.room.scores.p1").value(0))
        .andExpect(jsonPath("$.room.scores.p2").value(0))
        .andExpect(jsonPath("$.room.currentRound.roundNumber").value(1))
        .andExpect(jsonPath("$.room.currentRound.maxAttempts").value(1))
        .andExpect(jsonPath("$.room.currentRound.finished").value(true))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1", hasSize(1)))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].word").value("FUOCO"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].attemptNumber").value(1))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2", hasSize(1)))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].word").value("FUOCO"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].attemptNumber").value(1))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters", hasSize(5)))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[0].letter").value("F"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[1].letter").value("U"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[2].letter").value("O"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[3].letter").value("C"))
        .andExpect(
            jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[4].letter").value("O"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[0].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[1].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[2].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[3].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2[0].letters[4].status")
            .value(is(oneOf("ABSENT", "PRESENT", "CORRECT"))));

    // round is finished
    mockMvc.perform(get(BASE_URL + "/{roomId}", roomId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(roomId))
        .andExpect(jsonPath("$.language").value("IT"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.players", contains(PLAYER_ID_1, PLAYER_ID_2)))
        .andExpect(jsonPath("$.scores.p1").value(0))
        .andExpect(jsonPath("$.scores.p2").value(0))
        .andExpect(jsonPath("$.currentRound.roundNumber").value(1))
        .andExpect(jsonPath("$.currentRound.maxAttempts").value(1))
        .andExpect(jsonPath("$.currentRound.finished").value(true))
        .andExpect(jsonPath("$.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.currentRound.statusByPlayerId.p2").value("LOST"))
        .andExpect(jsonPath("$.currentRound.guessesByPlayerId.p1", hasSize(1)))
        .andExpect(jsonPath("$.currentRound.guessesByPlayerId.p1[0].word").value("FUOCO"))
        .andExpect(jsonPath("$.currentRound.guessesByPlayerId.p2", hasSize(1)))
        .andExpect(jsonPath("$.currentRound.guessesByPlayerId.p2[0].word").value("FUOCO"));

    // submitting another guess starts a new round
    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", PLAYER_ID_1, "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.id").value(roomId))
        .andExpect(jsonPath("$.room.language").value("IT"))
        .andExpect(jsonPath("$.room.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.room.players", contains(PLAYER_ID_1, PLAYER_ID_2)))
        .andExpect(jsonPath("$.room.scores.p1").value(0))
        .andExpect(jsonPath("$.room.scores.p2").value(0))
        .andExpect(jsonPath("$.room.currentRound.roundNumber").value(2))
        .andExpect(jsonPath("$.room.currentRound.maxAttempts").value(1))
        .andExpect(jsonPath("$.room.currentRound.finished").value(false))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("PLAYING"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1", hasSize(1)))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].word").value("FUOCO"))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].attemptNumber").value(1))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p1[0].letters", hasSize(5)))
        .andExpect(jsonPath("$.room.currentRound.guessesByPlayerId.p2").doesNotExist());
  }
}
