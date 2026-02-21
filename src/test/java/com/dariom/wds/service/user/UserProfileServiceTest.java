package com.dariom.wds.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.exception.UserNotFoundException;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.repository.UserRepository;
import com.dariom.wds.service.DomainMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  @Mock
  private UserRepository userRepository;

  @Spy
  private DomainMapper domainMapper;

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
    assertThat(profile.email()).isEqualTo("email");
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

  @Test
  void getAllUserProfiles_usersExist_returnsPagedProfiles() {
    // Arrange
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var userEntity = new AppUserEntity(userId, "email", "googleSub", "John Smith",
        "https://example.com/pic.png");
    var pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(userEntity), pageable, 1));

    // Act
    var result = userProfileService.getAllUserProfiles(pageable);

    // Assert
    assertThat(result.getContent()).hasSize(1);
    var profile = result.getContent().getFirst();
    assertThat(profile.id()).isEqualTo(userId.toString());
    assertThat(profile.email()).isEqualTo("email");
    assertThat(profile.fullName()).isEqualTo("John Smith");
    assertThat(profile.displayName()).isEqualTo("John");
    assertThat(profile.pictureUrl()).isEqualTo("https://example.com/pic.png");
    verify(userRepository).findAll(pageable);
  }

  @Test
  void getAllUserProfiles_usersDoNotExist_returnsEmptyPage() {
    // Arrange
    var pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    // Act
    var result = userProfileService.getAllUserProfiles(pageable);

    // Assert
    assertThat(result.getContent()).hasSize(0);
    assertThat(result.getTotalElements()).isEqualTo(0);
    assertThat(result.getTotalPages()).isEqualTo(0);
    verify(userRepository).findAll(pageable);
  }
}
