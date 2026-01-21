package com.dariom.wds.it;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

  @Autowired
  private JwtService jwtService;

  @Test
  void createRoom_missingLanguage_badRequest() throws Exception {
    var createReq = new HashMap<String, Object>();

    mockMvc.perform(post(BASE_URL)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"))
        .andExpect(jsonPath("$.message").value("language is required"));
  }

  @Test
  void createRoom_invalidLanguage_badRequest() throws Exception {
    var createReq = Map.of("language", "XX");

    mockMvc.perform(post(BASE_URL)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"))
        .andExpect(jsonPath("$.message").value("language is invalid"));
  }

  @Test
  void submitGuess_blankWord_badRequest() throws Exception {
    var createReq = Map.of("word", "   ");

    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", 1)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_WORD"))
        .andExpect(jsonPath("$.message").value("word is required"));
  }

  @Test
  void ready_missingRoundNumber_badRequest() throws Exception {
    var createReq = Map.of();

    mockMvc.perform(post(BASE_URL + "/{roomId}/ready", 1)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_ROUND_NUMBER"))
        .andExpect(jsonPath("$.message").value("roundNumber is required"));
  }

  @Test
  void ready_roundNumberIsZero_badRequest() throws Exception {
    var createReq = Map.of("roundNumber", "0");

    mockMvc.perform(post(BASE_URL + "/{roomId}/ready", 1)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_ROUND_NUMBER"))
        .andExpect(jsonPath("$.message").value("roundNumber must be greater than 1"));
  }

  private String bearer() {
    var user = new AppUserEntity(UUID.randomUUID(), "test@example.com", "google-sub", "Test User",
        "pictureUrl");
    user.addRole(new RoleEntity("USER"));

    return "Bearer " + jwtService.createAccessToken(user).token();
  }
}
