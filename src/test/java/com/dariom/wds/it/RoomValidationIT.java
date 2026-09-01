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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomValidationIT extends AbstractRedisTest {

  private static final String BASE_URL = "/api/v1/rooms";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private IntegrationTestHelper itHelper;

  @Test
  void createRoom_missingLanguage_badRequest() throws Exception {
    var createReq = new HashMap<String, Object>();
    createReq.put("rounds", 5);

    mockMvc.perform(post(BASE_URL)
            .with(itHelper.userAuthentication())
            .with(csrf())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"))
        .andExpect(jsonPath("$.message").value("language is required"));
  }

  @Test
  void createRoom_invalidLanguage_badRequest() throws Exception {
    var createReq = Map.of("language", "XX", "rounds", 5);

    mockMvc.perform(post(BASE_URL)
            .with(itHelper.userAuthentication())
            .with(csrf())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"))
        .andExpect(jsonPath("$.message").value("language is invalid"));
  }

  @Test
  void createRoom_invalidRounds_badRequest() throws Exception {
    var createReq = Map.of("language", "IT", "rounds", 7);

    mockMvc.perform(post(BASE_URL)
            .with(itHelper.userAuthentication())
            .with(csrf())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GENERIC_BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Invalid request"));
  }

  @Test
  void submitGuess_blankWord_badRequest() throws Exception {
    var createReq = Map.of("word", "   ");

    mockMvc.perform(post(BASE_URL + "/{roomId}/guess", 1)
            .with(itHelper.userAuthentication())
            .with(csrf())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_WORD"))
        .andExpect(jsonPath("$.message").value("word is required"));
  }

}
