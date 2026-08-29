package com.dariom.wds.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.api.v1.dto.UserMeDto;
import com.dariom.wds.config.security.AuthenticatedUser;
import com.dariom.wds.config.security.AuthenticatedUserResolver;
import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.service.user.UserProfileService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserProfileService userProfileService;
  @Mock
  private AuthenticatedUserResolver authenticatedUserResolver;
  @Mock
  private OidcUser oidcUser;

  @InjectMocks
  private UserController userController;

  @Test
  void me_validSessionPrincipal_returnsOkWithUser() {
    // Arrange
    when(authenticatedUserResolver.from(any(OidcUser.class)))
        .thenReturn(new AuthenticatedUser("user-1", "john@example.com", java.util.Set.of("USER")));
    when(userProfileService.getUserProfile(anyString()))
        .thenReturn(new UserProfile("user-1", "john@example.com", "John Smith", "John",
            "https://example.com/pic.png", Instant.parse("2025-06-01T10:00:00Z")));

    // Act
    var response = userController.me(oidcUser);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody())
        .isEqualTo(new UserMeDto("user-1", "John Smith", "John", "https://example.com/pic.png",
            java.util.Set.of("USER")));

    verify(userProfileService).getUserProfile("user-1");
  }

}
