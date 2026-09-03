package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.common.ErrorCode.CHAT_MESSAGE_LIMIT_REACHED;
import static com.dariom.wds.api.common.ErrorCode.DICTIONARY_EMPTY;
import static com.dariom.wds.api.common.ErrorCode.GENERIC_BAD_REQUEST;
import static com.dariom.wds.api.common.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.common.ErrorCode.INVALID_ROUND_NUMBER;
import static com.dariom.wds.api.common.ErrorCode.INVALID_WORD;
import static com.dariom.wds.api.common.ErrorCode.PLAYER_NOT_IN_ROOM;
import static com.dariom.wds.api.common.ErrorCode.ROOM_ACCESS_DENIED;
import static com.dariom.wds.api.common.ErrorCode.ROOM_BUSY;
import static com.dariom.wds.api.common.ErrorCode.ROOM_CLOSED;
import static com.dariom.wds.api.common.ErrorCode.ROOM_FULL;
import static com.dariom.wds.api.common.ErrorCode.ROOM_NOT_FOUND;
import static com.dariom.wds.api.common.ErrorCode.ROOM_NOT_READY;
import static com.dariom.wds.api.common.ErrorCode.USER_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomAccessDeniedException;
import com.dariom.wds.exception.RoomClosedException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoomMessageLimitReachedException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(1)
@RestControllerAdvice
public class ApiErrorHandler {

  @ExceptionHandler(RoomAccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleRoomAccessDenied(RoomAccessDeniedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(FORBIDDEN)
        .body(new ErrorResponse(ROOM_ACCESS_DENIED));
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(NOT_FOUND)
        .body(new ErrorResponse(USER_NOT_FOUND));
  }

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(NOT_FOUND)
        .body(new ErrorResponse(ROOM_NOT_FOUND));
  }

  @ExceptionHandler(RoomFullException.class)
  public ResponseEntity<ErrorResponse> handleRoomFull(RoomFullException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_FULL));
  }

  @ExceptionHandler(RoomClosedException.class)
  public ResponseEntity<ErrorResponse> handleRoomClosed(RoomClosedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_CLOSED));
  }

  @ExceptionHandler(RoomNotReadyException.class)
  public ResponseEntity<ErrorResponse> handleRoomNotReady(RoomNotReadyException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_NOT_READY));
  }

  @ExceptionHandler(RoomLockedException.class)
  public ResponseEntity<ErrorResponse> handleRoomLocked(RoomLockedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_BUSY));
  }

  @ExceptionHandler(RoomMessageLimitReachedException.class)
  public ResponseEntity<ErrorResponse> handleRoomMessageLimitReached(
      RoomMessageLimitReachedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(CHAT_MESSAGE_LIMIT_REACHED));
  }

  @ExceptionHandler(PlayerNotInRoomException.class)
  public ResponseEntity<ErrorResponse> handlePlayerNotInRoom(PlayerNotInRoomException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(FORBIDDEN)
        .body(new ErrorResponse(PLAYER_NOT_IN_ROOM));
  }

  @ExceptionHandler(InvalidGuessException.class)
  public ResponseEntity<ErrorResponse> handleInvalidGuess(InvalidGuessException ex) {
    log.warn("Invalid guess: code={}, message={}", ex.getCode(), ex.getMessage());
    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(ex.getCode()));
  }

  @ExceptionHandler(DictionaryEmptyException.class)
  public ResponseEntity<ErrorResponse> handleDictionaryEmpty(DictionaryEmptyException ex) {
    log.error(ex.getMessage());
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(DICTIONARY_EMPTY));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleArgumentNotValid(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    var fieldError = ex.getBindingResult().getFieldError();
    if (fieldError == null) {
      log.warn("Request validation failed: no fieldError");
      return ResponseEntity.status(BAD_REQUEST)
          .body(new ErrorResponse(GENERIC_BAD_REQUEST));
    }

    var message = defaultIfBlank(fieldError.getDefaultMessage(), "Invalid request");
    log.warn("Request validation failed: endpoint={} {}, field={}, rejectedValue={}, message={}",
        request.getMethod(), request.getRequestURI(),
        fieldError.getField(), fieldError.getRejectedValue(), message);

    var errorCode = switch (fieldError.getField()) {
      case "word" -> INVALID_WORD;
      case "language" -> INVALID_LANGUAGE;
      case "roundNumber" -> INVALID_ROUND_NUMBER;
      default -> GENERIC_BAD_REQUEST;
    };

    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(errorCode));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(
      HttpMessageNotReadableException ex) {
    log.warn("Request body is not readable: {}", ex.getMostSpecificCause().getMessage());
    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(GENERIC_BAD_REQUEST));
  }

}
