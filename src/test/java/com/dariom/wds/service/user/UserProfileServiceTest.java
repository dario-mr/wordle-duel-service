package com.dariom.wds.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.exception.UserNotFoundException;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserProfileService userProfileService;

  @Test
  void getUserProfile_userExists_returnsProfile() {
    // Arrange
    var userId = "00000000-0000-0000-0000-000000000001";
    var userEntity = new AppUserEntity(UUID.fromString(userId), "email", "googleSub", "John Smith",
        "https://example.com/pic.png");
    when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));

    // Act
    var profile = userProfileService.getUserProfile(userId);

    // Assert
    assertThat(profile.id()).isEqualTo(userId);
    assertThat(profile.displayName()).isEqualTo("John");
    assertThat(profile.pictureUrl()).isEqualTo("https://example.com/pic.png");
    verify(userRepository).findById(userId);
  }

  @Test
  void getUserProfile_userDoesNotExist_throwsException() {
    // Arrange
    var userId = "00000000-0000-0000-0000-000000000001";
    when(userRepository.findById(any())).thenReturn(Optional.empty());

    // Act
    var thrown = catchThrowable(() -> userProfileService.getUserProfile(userId));

    // Assert
    assertThat(thrown)
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("User <%s> not found".formatted(userId));
    verify(userRepository).findById(userId);
  }
}
