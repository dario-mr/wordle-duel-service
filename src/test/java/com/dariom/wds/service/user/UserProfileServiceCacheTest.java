package com.dariom.wds.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.CacheConfig;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.UserRepository;
import com.dariom.wds.service.DomainMapper;
import com.dariom.wds.service.auth.OAuthUserService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {
    CacheConfig.class,
    DomainMapper.class,
    UserProfileService.class,
    OAuthUserService.class,
    UserProfileServiceCacheTest.TestConfig.class
})
class UserProfileServiceCacheTest {

  private static final Instant TOKEN_ISSUED_AT = Instant.parse("2025-01-01T12:00:00Z");
  private static final Instant TOKEN_EXPIRES_AT = TOKEN_ISSUED_AT.plusSeconds(600);

  @Configuration
  static class TestConfig {

    @Bean
    UserRepository userRepository() {
      return Mockito.mock(UserRepository.class);
    }

    @Bean
    UserDetailsService userDetailsService() {
      return Mockito.mock(UserDetailsService.class);
    }
  }

  @Autowired
  private UserProfileService userProfileService;

  @Autowired
  private OAuthUserService oAuthUserService;

  @Autowired
  private UserRepository userRepository;

  @Test
  void getUserProfile_cachedAndEvictedOnOAuthLogin_hitsRepositoryAgainAfterEvict() {
    // Arrange
    var userId = "00000000-0000-0000-0000-000000000001";
    var userUuid = UUID.fromString(userId);

    var userEntity = new AppUserEntity(userUuid, "user@test.com", "google-sub-1", "John Smith",
        "https://example.com/pic.png");
    userEntity.addRole(new RoleEntity("USER"));

    when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

    var email = "user@test.com";
    var googleSub = "google-sub-1";
    var fullName = "User Test";
    var pictureUrl = "picture.com/user.png";

    var oidcUser = oidcUser(email, googleSub, fullName, "email", pictureUrl);
    when(userRepository.findOrCreate(googleSub, email, fullName, pictureUrl))
        .thenReturn(userEntity);

    // Act
    var profile1 = userProfileService.getUserProfile(userId);
    var profile2 = userProfileService.getUserProfile(userId);

    oAuthUserService.createOrUpdatePrincipal(oidcUser);

    var profile3 = userProfileService.getUserProfile(userId);

    // Assert
    assertThat(profile1).isEqualTo(profile2);
    assertThat(profile3).isEqualTo(profile1);

    verify(userRepository, times(2)).findById(userId);
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
