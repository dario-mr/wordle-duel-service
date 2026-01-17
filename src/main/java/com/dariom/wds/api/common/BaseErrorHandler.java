package com.dariom.wds.api.common;

import static com.dariom.wds.api.common.ErrorCode.UNKNOWN_ERROR;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class BaseErrorHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest req) {
    log.error("Unhandled error: {} {}", req.getMethod(), req.getRequestURI(), ex);
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(UNKNOWN_ERROR, "Unexpected error"));
  }
}
