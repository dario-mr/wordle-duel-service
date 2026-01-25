package com.dariom.wds.it;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import com.dariom.wds.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomAccessIT {

  private static final String LANGUAGE = "IT";
  private static final String PLAYER_1_ID = "11111111-1111-1111-1111-111111111111";
  private static final String PLAYER_2_ID = "22222222-2222-2222-2222-222222222222";
  private static final String PLAYER_3_ID = "33333333-3333-3333-3333-333333333333";

  @Resource
  private MockMvc mockMvc;
  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private JwtService jwtService;
  @Resource
  private AppUserJpaRepository appUserJpaRepository;
  @Resource
  private RoleJpaRepository roleJpaRepository;

  @Test
  void getRoom_roomFullAndRequestingPlayerNotInRoom_returnsForbidden() throws Exception {
    // Arrange
    var testUtil = new TestUtil(mockMvc, objectMapper, jwtService, appUserJpaRepository,
        roleJpaRepository);

    var user1 = testUtil.createUser(PLAYER_1_ID, "player1@example.com", "John Smith");
    var user2 = testUtil.createUser(PLAYER_2_ID, "player2@example.com", "Bart Simpson");
    var user3 = testUtil.createUser(PLAYER_3_ID, "player3@example.com", "Lisa Simpson");

    var player1Bearer = testUtil.bearer(user1);
    var player2Bearer = testUtil.bearer(user2);
    var player3Bearer = testUtil.bearer(user3);

    var createReq = Map.of("language", LANGUAGE);
    var createRes = testUtil.createRoom(player1Bearer, createReq)
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").value(not(emptyOrNullString())))
        .andExpect(jsonPath("$.language").value(LANGUAGE))
        .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
        .andExpect(jsonPath("$.players[0].id").value(PLAYER_1_ID))
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    var roomId = objectMapper.readTree(createdJson).get("id").asText();

    testUtil.joinRoom(roomId, player2Bearer).andExpect(status().isOk());

    // Act / Assert
    testUtil.getRoom(roomId, player3Bearer)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ROOM_ACCESS_DENIED"))
        .andExpect(jsonPath("$.message").value(not(emptyOrNullString())));
  }
}
