package com.dariom.wds.api.admin;

import com.dariom.wds.api.admin.dto.UserDto;
import com.dariom.wds.service.user.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@Tag(name = "Admin - Users", description = "Administrative user management")
public class AdminUserController {

  private final UserProfileService userProfileService;

  @Operation(summary = "List all users", description = "Returns a paginated list of all registered users.")
  @GetMapping
  public Page<UserDto> getAllUsers(
      @PageableDefault(size = 50, sort = "fullName") Pageable pageable
  ) {
    log.info("Admin list users pageable={}", pageable);
    return userProfileService.getAllUserProfiles(pageable)
        .map(u ->
            new UserDto(u.id(), u.email(), u.fullName(), u.displayName(), u.pictureUrl(), u.createdOn()));
  }
}
