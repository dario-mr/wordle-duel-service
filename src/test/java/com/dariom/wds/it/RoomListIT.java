package com.dariom.wds.it;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class RoomListIT {

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
  void listRooms_returnsOnlyRoomsWherePlayerIsMember() throws Exception {
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

    // user1 creates a room
    var roomCreatedByP1 = createRoom(testUtil, player1Bearer, createReq);
    sleep(1);

    // user2 creates a room, user1 joins it
    var roomCreatedByP2 = createRoom(testUtil, player2Bearer, createReq);
    testUtil.joinRoom(roomCreatedByP2, player1Bearer).andExpect(status().isOk());

    // user3 creates a room (should not be visible to user1)
    createRoom(testUtil, player3Bearer, createReq);

    // Act / Assert
    mockMvc.perform(get("/api/v1/rooms")
            .header("Authorization", player1Bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].id", contains(roomCreatedByP2, roomCreatedByP1)));
  }

  private String createRoom(TestUtil testUtil, String bearer, Map<String, String> createReq)
      throws Exception {
    var createRes = testUtil.createRoom(bearer, createReq)
        .andExpect(status().isCreated())
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    return objectMapper.readTree(createdJson).get("id").asText();
  }
}
