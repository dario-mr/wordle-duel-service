package com.dariom.wds.service.auth;

import static org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;
import static org.springframework.security.oauth2.jwt.JwtEncoderParameters.from;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.domain.AccessToken;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final SecurityProperties securityProperties;
  private final JwtEncoder jwtEncoder;
  private final Clock clock;

  public AccessToken createAccessToken(AppUserEntity user) {
    var now = Instant.now(clock);
    var props = securityProperties.jwt();
    var expiresAt = now.plusSeconds(props.ttlSeconds());

    var roles = user.getRoles().stream()
        .map(RoleEntity::getName)
        .sorted()
        .toList();

    var claimsBuilder = JwtClaimsSet.builder()
        .issuer(props.issuer())
        .issuedAt(now)
        .expiresAt(expiresAt)
        .subject(user.getEmail())
        .claim("uid", user.getId().toString())
        .claim("roles", roles);
    var claims = claimsBuilder.build();

    var header = JwsHeader.with(HS256).build();
    var tokenValue = jwtEncoder.encode(from(header, claims)).getTokenValue();

    return new AccessToken(tokenValue, props.ttlSeconds());
  }

}
