package com.dariom.wds.service.auth;

import static com.dariom.wds.config.CacheConfig.DISPLAY_NAME_CACHE;
import static com.dariom.wds.config.CacheConfig.USER_PROFILE_CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@ExtendWith(MockitoExtension.class)
class OAuthUserServiceTest {

  private static final Instant TOKEN_ISSUED_AT = Instant.parse("2025-01-01T12:00:00Z");
  private static final Instant TOKEN_EXPIRES_AT = TOKEN_ISSUED_AT.plusSeconds(600);

  @Mock
  private UserRepository userRepository;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache userProfileCache;

  @Mock
  private Cache displayNameCache;

  @InjectMocks
  private OAuthUserService oAuthUserService;

  @Test
  void createOrUpdatePrincipal_validClaims_returnsPrincipalWithEmailNameAndRoleAuthorities() {
    // Arrange
    var email = "user@test.com";
    var sub = "google-sub-1";
    var fullName = "User Test";
    var pictureUrl = "picture.com/user.png";

    var oidcUser = oidcUser(email, sub, fullName, "email", pictureUrl);

    var user = new AppUserEntity(UUID.randomUUID(), email, sub, fullName, pictureUrl);
    user.addRole(new RoleEntity("USER"));
    user.addRole(new RoleEntity("ADMIN"));

    when(userRepository.findOrCreate(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(user);

    when(cacheManager.getCache(USER_PROFILE_CACHE)).thenReturn(userProfileCache);
    when(cacheManager.getCache(DISPLAY_NAME_CACHE)).thenReturn(displayNameCache);

    // Act
    var principal = oAuthUserService.createOrUpdatePrincipal(oidcUser);

    // Assert
    assertThat(principal.getName()).isEqualTo(email);
    assertThat(principal.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");

    verify(userRepository).findOrCreate(sub, email, fullName, pictureUrl);

    var appUserId = user.getId().toString();
    verify(cacheManager).getCache(USER_PROFILE_CACHE);
    verify(cacheManager).getCache(DISPLAY_NAME_CACHE);
    verify(userProfileCache).evict(appUserId);
    verify(displayNameCache).evict(appUserId);

    verifyNoMoreInteractions(userRepository);
  }

  @Test
  void createOrUpdatePrincipal_missingEmail_throws() {
    // Arrange
    var oidcUser = oidcUser(null, "google-sub-1", "User Test", "sub", "pictureUrl");

    // Act
    var thrown = catchThrowable(() -> oAuthUserService.createOrUpdatePrincipal(oidcUser));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Google user missing required claims (email/sub)");

    verifyNoInteractions(userRepository, cacheManager, userProfileCache, displayNameCache);
  }

  @Test
  void createOrUpdatePrincipal_missingSub_throws() {
    // Arrange
    var oidcUser = oidcUser("user@test.com", null, "User Test", "email", "pictureUrl");

    // Act
    var thrown = catchThrowable(() -> oAuthUserService.createOrUpdatePrincipal(oidcUser));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Google user missing required claims (email/sub)");

    verifyNoInteractions(userRepository, cacheManager, userProfileCache, displayNameCache);
  }

  private DefaultOidcUser oidcUser(
      String email, String sub, String fullName, String nameAttributeKey, String pictureUrl
  ) {
    var idTokenClaims = new HashMap<String, Object>();
    if (sub != null) {
      idTokenClaims.put("sub", sub);
    }
    if (email != null) {
      idTokenClaims.put("email", email);
    }
    if (fullName != null) {
      idTokenClaims.put("name", fullName);
    }
    if (fullName != null) {
      idTokenClaims.put("picture", pictureUrl);
    }

    var userInfoClaims = new HashMap<String, Object>();
    if (email != null) {
      userInfoClaims.put("email", email);
    } else if (sub != null) {
      userInfoClaims.put("sub", sub);
    } else if (fullName != null) {
      userInfoClaims.put("name", fullName);
    } else if (pictureUrl != null) {
      userInfoClaims.put("picture", pictureUrl);
    } else {
      userInfoClaims.put("name", "User Test");
    }

    var idToken = new OidcIdToken("id-token", TOKEN_ISSUED_AT, TOKEN_EXPIRES_AT,
        Map.copyOf(idTokenClaims));
    var userInfo = new OidcUserInfo(Map.copyOf(userInfoClaims));

    return new DefaultOidcUser(List.of(), idToken, userInfo, nameAttributeKey);
  }
}
