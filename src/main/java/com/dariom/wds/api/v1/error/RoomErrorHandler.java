package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_NOT_IN_ROOM;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_FULL;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RoomErrorHandler {

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException ex) {
    return ResponseEntity.status(NOT_FOUND)
        .body(new ErrorResponse(ROOM_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(RoomFullException.class)
  public ResponseEntity<ErrorResponse> handleRoomFull(RoomFullException ex) {
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_FULL, ex.getMessage()));
  }

  @ExceptionHandler(PlayerNotInRoomException.class)
  public ResponseEntity<ErrorResponse> handlePlayerNotInRoom(PlayerNotInRoomException ex) {
    return ResponseEntity.status(FORBIDDEN)
        .body(new ErrorResponse(PLAYER_NOT_IN_ROOM, ex.getMessage()));
  }

  @ExceptionHandler(InvalidGuessException.class)
  public ResponseEntity<ErrorResponse> handleInvalidGuess(InvalidGuessException ex) {
    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }
}
