package com.dariom.wds.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class UserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserDetailsService userDetailsService;

  @Test
  void getUserDisplayName_userHasFullName_returnsDisplayName() {
    // Arrange
    var userId = "00000000-0000-0000-0000-000000000001";
    var userEntity = userEntity(userId, "John Smith");
    when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));

    // Act
    var displayName = userDetailsService.getUserDisplayName(userId);

    // Assert
    assertThat(displayName).isEqualTo("John");
    verify(userRepository).findById(userId);
  }

  @Test
  void getUserDisplayName_userHasBlankName_returnsAnonymous() {
    // Arrange
    var userId = "00000000-0000-0000-0000-000000000001";
    var userEntity = userEntity(userId, "");
    when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));

    // Act
    var displayName = userDetailsService.getUserDisplayName(userId);

    // Assert
    assertThat(displayName).isEqualTo("Anonymous");
    verify(userRepository).findById(userId);
  }

  @Test
  void getUserDisplayName_userDoesNotExist_returnsAnonymous() {
    // Arrange
    var userId = "00000000-0000-0000-0000-000000000001";
    when(userRepository.findById(any())).thenReturn(Optional.empty());

    // Act
    var displayName = userDetailsService.getUserDisplayName(userId);

    // Assert
    assertThat(displayName).isEqualTo("Anonymous");
    verify(userRepository).findById(userId);
  }

  private AppUserEntity userEntity(String userId, String fullName) {
    return new AppUserEntity(UUID.fromString(userId), "email", "googleSub", fullName);
  }
}