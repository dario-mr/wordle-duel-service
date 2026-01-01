package com.dariom.wds.wordle;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "wordle.max-attempts=1")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RoomFlowIntegrationTest {

  private static final String BASE_URL = "/api/v1/rooms";
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void roundFinishesWhenBothPlayersDone() throws Exception {
    var createReq = Map.of("playerId", "p1", "language", "IT");

    var createRes = mvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    var roomId = objectMapper.readTree(createdJson).get("id").asText();

    mvc.perform(post(BASE_URL + "/{roomId}/join", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.currentRound.roundNumber").value(1));

    mvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p1", "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentRound.finished").value(false))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("PLAYING"));

    mvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p2", "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentRound.finished").value(true))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p1").value("LOST"))
        .andExpect(jsonPath("$.room.currentRound.statusByPlayerId.p2").value("LOST"))
        .andExpect(jsonPath("$.room.scores.p1").value(0))
        .andExpect(jsonPath("$.room.scores.p2").value(0));

    mvc.perform(get(BASE_URL + "/{roomId}", roomId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentRound.finished").value(true));

    mvc.perform(post(BASE_URL + "/{roomId}/guess", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("playerId", "p1", "word", "FUOCO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentRound.roundNumber").value(2));
  }
}
