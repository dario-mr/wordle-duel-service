package com.dariom.wds.it;

import static com.dariom.wds.domain.Role.ADMIN;
import static com.dariom.wds.domain.Role.USER;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dariom.wds.domain.Role;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import com.dariom.wds.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Lazy
@Component
@RequiredArgsConstructor
class TestUtil {

  private static final String BASE_URL = "/api/v1/rooms";

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final JwtService jwtService;
  private final AppUserJpaRepository appUserJpaRepository;
  private final RoleJpaRepository roleJpaRepository;

  String userBearer() {
    return bearer(USER);
  }

  String adminBearer() {
    return bearer(ADMIN);
  }

  String bearer(AppUserEntity user) {
    var tokenUser = new AppUserEntity(
        user.getId(), user.getEmail(), "google-sub", user.getFullName(), "pictureUrl");
    tokenUser.addRole(new RoleEntity(USER.name()));
    return "Bearer " + jwtService.createAccessToken(tokenUser).token();
  }

  AppUserEntity createUser(String userId, String email, String fullName) {
    var roleName = USER.name();
    var role = roleJpaRepository.findById(roleName)
        .orElseGet(() -> roleJpaRepository.save(new RoleEntity(roleName)));

    var user = new AppUserEntity(UUID.fromString(userId), email, "google-sub-" + userId, fullName,
        "pictureUrl");
    user.addRole(role);

    return appUserJpaRepository.save(user);
  }

  ResultActions createRoom(String bearer, Object body) throws Exception {
    return postJson(bearer, BASE_URL, body);
  }

  ResultActions joinRoom(String roomId, String bearer) throws Exception {
    return postJson(bearer, BASE_URL + "/{roomId}/join", Map.of(), roomId);
  }

  ResultActions submitGuess(String roomId, String bearer, String word) throws Exception {
    return postJson(bearer, BASE_URL + "/{roomId}/guess", Map.of("word", word), roomId);
  }

  ResultActions ready(String roomId, String bearer, int roundNumber) throws Exception {
    return postJson(bearer, BASE_URL + "/{roomId}/ready", Map.of("roundNumber", roundNumber),
        roomId);
  }

  ResultActions getRoom(String roomId, String bearer) throws Exception {
    return mockMvc.perform(get(BASE_URL + "/{roomId}", roomId)
        .header("Authorization", bearer));
  }

  ResultActions postJson(String bearer, String urlTemplate, Object body, Object... uriVars)
      throws Exception {
    return mockMvc.perform(post(urlTemplate, uriVars)
        .header("Authorization", bearer)
        .contentType(APPLICATION_JSON)
        .content(body instanceof String str ? str : objectMapper.writeValueAsString(body)));
  }

  private String bearer(Role role) {
    var user = new AppUserEntity(
        UUID.randomUUID(), "test@example.com", "google-sub", "Test User", "pictureUrl");
    user.addRole(new RoleEntity(role.name()));

    return "Bearer " + jwtService.createAccessToken(user).token();
  }
}
