package com.dariom.wds.persistence.repository;

import static com.dariom.wds.domain.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

  @Mock
  private AppUserJpaRepository appUserJpaRepository;
  @Mock
  private RoleJpaRepository roleJpaRepository;

  @InjectMocks
  private UserRepository userRepository;

  @Test
  void findOrCreate_existingByGoogleSubAndFullNameProvided_updatesFullNameAndAddsRole() {
    // Arrange
    var googleSub = "google-sub-1";
    var email = "user@test.com";
    var fullName = "New User";

    var existing = new AppUserEntity(UUID.randomUUID(), email, googleSub, "Old Name");
    var roleEntity = new RoleEntity(USER.getName());

    when(appUserJpaRepository.findByGoogleSub(anyString())).thenReturn(Optional.of(existing));
    when(roleJpaRepository.findById(anyString())).thenReturn(Optional.of(roleEntity));
    when(appUserJpaRepository.save(any(AppUserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    var result = userRepository.findOrCreate(googleSub, email, fullName);

    // Assert
    assertThat(result).isSameAs(existing);
    assertThat(result.getFullName()).isEqualTo(fullName);
    assertThat(result.getRoles())
        .extracting(RoleEntity::getName)
        .containsExactlyInAnyOrder(USER.getName());

    verify(appUserJpaRepository).findByGoogleSub(googleSub);
    verify(roleJpaRepository).findById(USER.getName());
    verify(appUserJpaRepository).save(existing);
    verifyNoMoreInteractions(appUserJpaRepository, roleJpaRepository);
  }

  @Test
  void findOrCreate_existingByEmail_linksGoogleSubAndAddsRole() {
    // Arrange
    var googleSub = "google-sub-1";
    var email = "user@test.com";
    var fullName = "New User";

    var existing = new AppUserEntity(UUID.randomUUID(), email, "google-sub-old", "Old Name");
    var roleEntity = new RoleEntity(USER.getName());

    when(appUserJpaRepository.findByGoogleSub(anyString())).thenReturn(Optional.empty());
    when(appUserJpaRepository.findByEmail(anyString())).thenReturn(Optional.of(existing));
    when(roleJpaRepository.findById(anyString())).thenReturn(Optional.of(roleEntity));
    when(appUserJpaRepository.save(any(AppUserEntity.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    // Act
    var result = userRepository.findOrCreate(googleSub, email, fullName);

    // Assert
    assertThat(result).isSameAs(existing);
    assertThat(result.getGoogleSub()).isEqualTo(googleSub);
    assertThat(result.getFullName()).isEqualTo(fullName);
    assertThat(result.getRoles())
        .extracting(RoleEntity::getName)
        .containsExactlyInAnyOrder(USER.getName());

    verify(appUserJpaRepository).findByGoogleSub(googleSub);
    verify(appUserJpaRepository).findByEmail(email);
    verify(roleJpaRepository).findById(USER.getName());
    verify(appUserJpaRepository).save(existing);
    verifyNoMoreInteractions(appUserJpaRepository, roleJpaRepository);
  }

  @Test
  void findOrCreate_newUser_createsUserWithRoleAndPersists() {
    // Arrange
    var googleSub = "google-sub-1";
    var email = "user@test.com";
    var fullName = "New User";

    var roleEntity = new RoleEntity(USER.getName());

    when(appUserJpaRepository.findByGoogleSub(anyString())).thenReturn(Optional.empty());
    when(appUserJpaRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(roleJpaRepository.findById(anyString())).thenReturn(Optional.of(roleEntity));
    when(appUserJpaRepository.save(any(AppUserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    var result = userRepository.findOrCreate(googleSub, email, fullName);

    // Assert
    assertThat(result.getId()).isNotNull();
    assertThat(result.getEmail()).isEqualTo(email);
    assertThat(result.getGoogleSub()).isEqualTo(googleSub);
    assertThat(result.getFullName()).isEqualTo(fullName);
    assertThat(result.getRoles())
        .extracting(RoleEntity::getName)
        .containsExactlyInAnyOrder(USER.getName());

    var userCaptor = ArgumentCaptor.forClass(AppUserEntity.class);
    verify(appUserJpaRepository).findByGoogleSub(googleSub);
    verify(appUserJpaRepository).findByEmail(email);
    verify(roleJpaRepository).findById(USER.getName());
    verify(appUserJpaRepository).save(userCaptor.capture());

    assertThat(userCaptor.getValue().getId()).isNotNull();
    assertThat(userCaptor.getValue().getEmail()).isEqualTo(email);
    assertThat(userCaptor.getValue().getGoogleSub()).isEqualTo(googleSub);
    assertThat(userCaptor.getValue().getFullName()).isEqualTo(fullName);

    verifyNoMoreInteractions(appUserJpaRepository, roleJpaRepository);
  }

  @Test
  void findOrCreate_missingRole_throws() {
    // Arrange
    var googleSub = "google-sub-1";
    var email = "user@test.com";
    var fullName = "New User";

    when(appUserJpaRepository.findByGoogleSub(anyString())).thenReturn(Optional.empty());
    when(appUserJpaRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(roleJpaRepository.findById(anyString())).thenReturn(Optional.empty());

    // Act
    var thrown = catchThrowable(() -> userRepository.findOrCreate(googleSub, email, fullName));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("RoleEntity not found: " + USER.getName());

    verify(appUserJpaRepository).findByGoogleSub(googleSub);
    verify(appUserJpaRepository).findByEmail(email);
    verify(roleJpaRepository).findById(USER.getName());
    verifyNoMoreInteractions(appUserJpaRepository, roleJpaRepository);
  }

  @Test
  void requireByEmail_unknownUser_throws() {
    // Arrange
    when(appUserJpaRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    // Act
    var thrown = catchThrowable(() -> userRepository.requireByEmail("missing@test.com"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown user: missing@test.com");

    verify(appUserJpaRepository).findByEmail("missing@test.com");
  }
}
