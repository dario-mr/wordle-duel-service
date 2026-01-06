package com.dariom.wds.exception;

import com.dariom.wds.domain.Language;

public class DictionaryEmptyException extends RuntimeException {

  public DictionaryEmptyException(Language language) {
    super("No answer words available for language: %s".formatted(language));
  }
}
