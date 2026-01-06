package com.dariom.wds.dummy;

import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;

import java.util.Optional;

public class CoverageDummyGreeter {

  public String greet(String name) {
    var trimmed = requireNonNull(name, "name").trim();
    if (trimmed.isBlank()) {
      return "Hello, stranger";
    }

    return "Hello, " + trimmed.toUpperCase(ROOT);
  }

  public Optional<String> tryGreet(String name) {
    if (name == null) {
      return Optional.empty();
    }

    var trimmed = name.trim();
    if (trimmed.isBlank()) {
      return Optional.empty();
    }

    return Optional.of(greet(trimmed));
  }
}
