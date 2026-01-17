package com.dariom.wds.api.auth;

import com.dariom.wds.api.auth.dto.RefreshResponse;
import com.dariom.wds.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication and token lifecycle")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Refresh access token", description = "Rotates the refresh token (HttpOnly cookie) and returns a new short-lived access token (Bearer JWT). Requires a CSRF token.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token", content = @Content)
  })
  @PostMapping(value = "/refresh", produces = "application/json")
  public ResponseEntity<RefreshResponse> refresh(HttpServletRequest request,
      HttpServletResponse response) {
    var refreshed = authService.refreshToken(request, response);
    return ResponseEntity.ok(new RefreshResponse(refreshed.accessToken().token(),
        refreshed.accessToken().expiresInSeconds()));
  }

  @Operation(summary = "Logout", description = "Revokes the refresh token (if present) and clears the refresh token cookie.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Logged out", content = @Content)
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    authService.clearRefreshToken(request, response);
    return ResponseEntity.noContent().build();
  }

}
