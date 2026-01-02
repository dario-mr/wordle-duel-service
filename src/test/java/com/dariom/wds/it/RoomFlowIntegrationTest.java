package com.dariom.wds.it;

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
class RoomFlowIntegrationTest {

  private static final String BASE_URL = "/api/v1/rooms";

  @Resource
  private MockMvc mockMvc;

  @Resource
  private ObjectMapper objectMapper;

  @Test
  void roundFinishesWhenBothPlayersDone() throws Exception {
    // player1 creates the room
    var createReq = Map.of("playerId", "p1", "language", "IT");
    var createRes = mockMvc.perform(post(BASE_URL)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    var roomId = objectMapper.readTree(createdJson).get("id").asText();

    // player2 joins the room
    mockMvc.perform(post(BASE_URL + "/{roomId}/join", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.currentRound.roundNumber").value(1));

    // player1 submits guess
    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p1", "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentRound.finished").value(false))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("PLAYING"));

    // player2 submits guess
    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p2", "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentRound.finished").value(true))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("LOST"))
        .andExpect(jsonPath("$.room.scores.p1").value(0))
        .andExpect(jsonPath("$.room.scores.p2").value(0));

    // round is finished
    mockMvc.perform(get(BASE_URL + "/{roomId}", roomId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentRound.finished").value(true));

    // submitting another guess starts a new round
    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p1", "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentRound.roundNumber").value(2));
  }
}
