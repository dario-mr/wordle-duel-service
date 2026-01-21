package com.dariom.wds.api.v1;

import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.api.v1.dto.UserMeDto;
import com.dariom.wds.service.user.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile endpoints")
public class UserController {

  private final UserProfileService userProfileService;

  public UserController(UserProfileService userProfileService) {
    this.userProfileService = userProfileService;
  }

  @Operation(summary = "Get current user", description = "Returns the current authenticated user's profile.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User returned", content = @Content(schema = @Schema(implementation = UserMeDto.class))),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/me")
  public ResponseEntity<UserMeDto> me(@AuthenticationPrincipal Jwt jwt) {
    var appUserId = jwt.getClaimAsString("uid");
    var profile = userProfileService.getUserProfile(appUserId);
    return ResponseEntity.ok(new UserMeDto(
        profile.id(), profile.fullName(), profile.displayName(), profile.pictureUrl()
    ));
  }
}
