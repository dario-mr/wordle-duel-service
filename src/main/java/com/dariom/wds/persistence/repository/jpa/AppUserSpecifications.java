package com.dariom.wds.persistence.repository.jpa;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.dariom.wds.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.domain.Specification;

public class AppUserSpecifications {

  private AppUserSpecifications() {
  }

  public static Specification<AppUserEntity> fullNameContains(String fullName) {
    if (isBlank(fullName)) {
      return null;
    }
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("fullName")), "%" + fullName.toLowerCase() + "%");
  }

  public static Specification<AppUserEntity> emailContains(String email) {
    if (isBlank(email)) {
      return null;
    }
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
  }

  public static Specification<AppUserEntity> displayNameContains(String displayName) {
    if (isBlank(displayName)) {
      return null;
    }
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("displayName")), "%" + displayName.toLowerCase() + "%");
  }

  public static Specification<AppUserEntity> playerSearch(String search) {
    if (isBlank(search)) {
      return null;
    }
    var pattern = "%" + search.strip().toLowerCase() + "%";
    return (root, query, cb) -> cb.or(
        cb.like(cb.lower(root.get("fullName")), pattern),
        cb.like(cb.lower(root.get("displayName")), pattern),
        cb.like(cb.lower(root.get("email")), pattern)
    );
  }
}
