package com.dariom.wds.api.auth.error;

import static com.dariom.wds.api.common.ErrorCode.REFRESH_TOKEN_EMPTY;
import static com.dariom.wds.api.common.ErrorCode.REFRESH_TOKEN_INVALID;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.exception.InvalidRefreshTokenException;
import com.dariom.wds.exception.RefreshTokenEmptyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(1)
@RestControllerAdvice
public class AuthErrorHandler {

  @ExceptionHandler(RefreshTokenEmptyException.class)
  public ResponseEntity<ErrorResponse> handleRefreshTokenEmpty(RefreshTokenEmptyException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(UNAUTHORIZED)
        .body(new ErrorResponse(REFRESH_TOKEN_EMPTY, ex.getMessage()));
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(UNAUTHORIZED)
        .body(new ErrorResponse(REFRESH_TOKEN_INVALID, ex.getMessage()));
  }
}
