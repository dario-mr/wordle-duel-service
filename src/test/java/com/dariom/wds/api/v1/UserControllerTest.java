package com.dariom.wds.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.api.v1.dto.UserMeDto;
import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.service.user.UserProfileService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserProfileService userProfileService;

  @InjectMocks
  private UserController userController;

  @Test
  void me_validJwt_returnsOkWithUser() {
    // Arrange
    when(userProfileService.getUserProfile(anyString()))
        .thenReturn(new UserProfile("user-1", "John Smith", "John", "https://example.com/pic.png",
            Instant.parse("2025-06-01T10:00:00Z")));

    // Act
    var response = userController.me(jwtWithUid("user-1"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody())
        .isEqualTo(new UserMeDto("user-1", "John Smith", "John", "https://example.com/pic.png"));

    verify(userProfileService).getUserProfile("user-1");
  }

  private static Jwt jwtWithUid(String uid) {
    var now = Instant.now();
    return new Jwt(
        "test-token",
        now,
        now.plusSeconds(3600),
        Map.of("alg", "none"),
        Map.of("uid", uid)
    );
  }
}
