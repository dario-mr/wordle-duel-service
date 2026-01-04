package com.dariom.wds.it;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoomValidationIT {

  private static final String BASE_URL = "/api/v1/rooms";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createRoom_blankPlayerId_badRequest() throws Exception {
    var createReq = Map.of("playerId", "   ", "language", "IT");

    mockMvc.perform(post(BASE_URL)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PLAYER_ID"))
        .andExpect(jsonPath("$.message").value("playerId is required"));
  }

  @Test
  void createRoom_missingLanguage_badRequest() throws Exception {
    var createReq = new HashMap<String, Object>();
    createReq.put("playerId", "p1");

    mockMvc.perform(post(BASE_URL)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"))
        .andExpect(jsonPath("$.message").value("language is required"));
  }

  @Test
  void createRoom_invalidLanguage_badRequest() throws Exception {
    var createReq = Map.of("playerId", "p1", "language", "XX");

    mockMvc.perform(post(BASE_URL)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"))
        .andExpect(jsonPath("$.message").value("language is invalid"));
  }

  @Test
  void submitGuess_blankWord_badRequest() throws Exception {
    var createReq = Map.of("playerId", "p1", "word", "   ");

    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", 1)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_WORD"))
        .andExpect(jsonPath("$.message").value("word is required"));
  }
}
