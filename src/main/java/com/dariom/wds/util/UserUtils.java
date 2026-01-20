package com.dariom.wds.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class UserUtils {

  public static final String ANONYMOUS = "Anonymous";
  private static final int MAX_NAME_LENGTH = 32;

  private UserUtils() {

  }

  public static String normalizeFullName(String fullName) {
    if (isBlank(fullName)) {
      return ANONYMOUS;
    }

    var normalized = normalizeSpace(fullName);
    var firstName = substringBefore(normalized, " ");

    if (isBlank(firstName)) {
      return ANONYMOUS;
    }

    return left(firstName, MAX_NAME_LENGTH);
  }

}
