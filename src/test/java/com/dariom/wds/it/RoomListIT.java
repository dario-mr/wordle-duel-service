package com.dariom.wds.it;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class RoomListIT extends AbstractRedisTest {

  private static final String LANGUAGE = "IT";
  private static final String PLAYER_1_ID = "11111111-1111-1111-1111-111111111111";
  private static final String PLAYER_2_ID = "22222222-2222-2222-2222-222222222222";
  private static final String PLAYER_3_ID = "33333333-3333-3333-3333-333333333333";

  @Resource
  private MockMvc mockMvc;

  @Resource
  private ObjectMapper objectMapper;

  @Resource
  private IntegrationTestHelper itHelper;

  @Test
  void listRooms_returnsOnlyRoomsWherePlayerIsMember() throws Exception {
    // Arrange
    var user1 = itHelper.createUser(PLAYER_1_ID, "player1@example.com", "John Smith");
    var user2 = itHelper.createUser(PLAYER_2_ID, "player2@example.com", "Bart Simpson");
    var user3 = itHelper.createUser(PLAYER_3_ID, "player3@example.com", "Lisa Simpson");

    var player1Authentication = itHelper.userAuthentication(user1);
    var player2Authentication = itHelper.userAuthentication(user2);
    var player3Authentication = itHelper.userAuthentication(user3);

    var createReq = Map.<String, Object>of("language", LANGUAGE, "rounds", 5);

    // user1 creates a room
    var roomCreatedByP1 = createRoom(player1Authentication, createReq);
    sleep(1);

    // user2 creates a room, user1 joins it
    var roomCreatedByP2 = createRoom(player2Authentication, createReq);
    itHelper.joinRoom(roomCreatedByP2, player1Authentication).andExpect(status().isOk());

    // user3 creates a room (should not be visible to user1)
    createRoom(player3Authentication, createReq);

    // Act / Assert
    mockMvc.perform(get("/api/v1/rooms")
            .with(player1Authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].id", contains(roomCreatedByP2, roomCreatedByP1)))
        .andExpect(jsonPath("$[0].players[0].wins").value(0))
        .andExpect(jsonPath("$[0].players[1].wins").value(0))
        .andExpect(jsonPath("$[0].players[0].matchScore").value(0))
        .andExpect(jsonPath("$[0].players[1].matchScore").value(0));
  }

  private String createRoom(org.springframework.test.web.servlet.request.RequestPostProcessor authentication,
      Map<String, Object> createReq)
      throws Exception {
    var createRes = itHelper.createRoom(authentication, createReq)
        .andExpect(status().isCreated())
        .andReturn();

    var createdJson = createRes.getResponse().getContentAsString();
    return objectMapper.readTree(createdJson).get("id").asText();
  }
}
