package com.dariom.wds.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserDetailsService userDetailsService;

  @InjectMocks
  private UserService userService;

  @Test
  void getDisplayNamePerPlayer_returnsDisplayNames() {
    // Arrange
    var userId1 = "user-1";
    var userId2 = "user-2";
    when(userDetailsService.getUserDisplayName(userId1)).thenReturn("John");
    when(userDetailsService.getUserDisplayName(userId2)).thenReturn("Bart");

    // Act
    var displayNamePerPlayer = userService.getDisplayNamePerPlayer(Set.of(userId1, userId2));

    // Assert
    assertThat(displayNamePerPlayer).hasSize(2);
    assertThat(displayNamePerPlayer.get(userId1)).isEqualTo("John");
    assertThat(displayNamePerPlayer.get(userId2)).isEqualTo("Bart");

    verify(userDetailsService).getUserDisplayName(userId1);
    verify(userDetailsService).getUserDisplayName(userId2);
  }
}