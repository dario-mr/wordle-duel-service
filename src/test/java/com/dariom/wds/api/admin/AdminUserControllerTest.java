package com.dariom.wds.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.service.user.UserProfileService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

  @Mock
  private UserProfileService userProfileService;

  @InjectMocks
  private AdminUserController controller;

  @Test
  void getAllUsers_validRequest_returnsPagedDtos() {
    // Arrange
    var profile = new UserProfile("id-1", "john@example.com", "John Smith", "John",
        "https://example.com/pic.png", Instant.parse("2025-06-01T10:00:00Z"));
    var page = new PageImpl<>(List.of(profile));
    when(userProfileService.getAllUserProfiles(any(Pageable.class), isNull(), isNull())).thenReturn(
        page);

    // Act
    var pageable = PageRequest.of(0, 20, Sort.by("fullName"));
    var result = controller.getAllUsers(pageable, null, null);

    // Assert
    assertThat(result.getContent()).hasSize(1);
    var dto = result.getContent().getFirst();
    assertThat(dto.id()).isEqualTo("id-1");
    assertThat(dto.email()).isEqualTo("john@example.com");
    assertThat(dto.fullName()).isEqualTo("John Smith");
    assertThat(dto.displayName()).isEqualTo("John");
    assertThat(dto.pictureUrl()).isEqualTo("https://example.com/pic.png");
    assertThat(dto.createdOn()).isEqualTo(Instant.parse("2025-06-01T10:00:00Z"));

    var captor = ArgumentCaptor.forClass(Pageable.class);
    verify(userProfileService).getAllUserProfiles(captor.capture(), isNull(), isNull());
    var captured = captor.getValue();
    assertThat(captured.getPageNumber()).isZero();
    assertThat(captured.getPageSize()).isEqualTo(20);
    assertThat(captured.getSort().getOrderFor("fullName")).isNotNull();
  }

  @Test
  void getAllUsers_withFilters_forwardsFiltersToService() {
    // Arrange
    var profile = new UserProfile("id-1", "john@example.com", "John Smith", "John",
        "https://example.com/pic.png", Instant.parse("2025-06-01T10:00:00Z"));
    var page = new PageImpl<>(List.of(profile));
    when(userProfileService.getAllUserProfiles(any(Pageable.class), eq("John"), eq("example.com")))
        .thenReturn(page);

    // Act
    var pageable = PageRequest.of(1, 10, Sort.by("email"));
    var result = controller.getAllUsers(pageable, "John", "example.com");

    // Assert
    assertThat(result.getContent()).hasSize(1);
    verify(userProfileService).getAllUserProfiles(pageable, "John", "example.com");
  }
}
