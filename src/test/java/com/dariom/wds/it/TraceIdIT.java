package com.dariom.wds.it;

import static com.dariom.wds.config.TraceIdFilter.TRACE_ID_HEADER;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TraceIdIT {

  private static final String BASE_URL = "/api/v1/rooms";

  @Resource
  private MockMvc mockMvc;

  @Resource
  private ObjectMapper objectMapper;

  @Resource
  private JwtService jwtService;

  @Test
  void requestWithoutTraceIdHeader_generatesTraceIdInResponseHeader() throws Exception {
    // Arrange
    var requestBody = Map.of("language", "IT");

    // Act / Assert
    mockMvc.perform(post(BASE_URL)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestBody)))
        .andExpect(status().isCreated())
        .andExpect(header().string(TRACE_ID_HEADER, not(emptyOrNullString())));
  }

  @Test
  void requestWithTraceIdHeader_reusesTraceIdInResponseHeader() throws Exception {
    // Arrange
    var traceId = "trace-id-from-client";
    var requestBody = Map.of("language", "IT");

    // Act / Assert
    mockMvc.perform(post(BASE_URL)
            .header(TRACE_ID_HEADER, traceId)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestBody)))
        .andExpect(status().isCreated())
        .andExpect(header().string(TRACE_ID_HEADER, is(traceId)));
  }

  @Test
  void invalidRequest_stillIncludesTraceIdInResponseHeader() throws Exception {
    // Arrange
    var requestBody = Map.of();

    // Act / Assert
    mockMvc.perform(post(BASE_URL)
            .header("Authorization", bearer())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestBody)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(TRACE_ID_HEADER, not(emptyOrNullString())));
  }

  private String bearer() {
    var user = new AppUserEntity(UUID.randomUUID(), "test@example.com", "google-sub", "Test User",
        "pictureUrl");
    user.addRole(new RoleEntity("USER"));

    return "Bearer " + jwtService.createAccessToken(user).token();
  }
}
