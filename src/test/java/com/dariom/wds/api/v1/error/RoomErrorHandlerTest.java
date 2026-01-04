package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.v1.error.ErrorCode.GENERIC_BAD_REQUEST;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_PLAYER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.api.v1.dto.CreateRoomRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class RoomErrorHandlerTest {

  private final RoomErrorHandler handler = new RoomErrorHandler();

  @Test
  void handleArgumentNotValid_fieldIsPlayerId_returnsInvalidPlayerId() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "playerId", "", false, null, null,
        "playerId is required"));

    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);
    var request = httpRequest("POST", "/api/v1/rooms");

    // Act
    var response = handler.handleArgumentNotValid(ex, request);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(INVALID_PLAYER_ID);
    assertThat(response.getBody().message()).isEqualTo("playerId is required");
  }

  @Test
  void handleArgumentNotValid_fieldIsLanguage_returnsInvalidLanguage() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "language", "", false, null, null,
        "language is invalid"));

    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

    // Act
    var response = handler.handleArgumentNotValid(ex, httpRequest("POST", "/api/v1/rooms"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(INVALID_LANGUAGE);
    assertThat(response.getBody().message()).isEqualTo("language is invalid");
  }

  @Test
  void handleArgumentNotValid_noFieldError_returnsGenericBadRequest() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

    // Act
    var response = handler.handleArgumentNotValid(ex, httpRequest("POST", "/api/v1/rooms"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(GENERIC_BAD_REQUEST);
  }

  @SuppressWarnings("unused")
  private void dummy(CreateRoomRequest request) {
  }

  private MethodParameter dummyParameter() throws NoSuchMethodException {
    Method method = RoomErrorHandlerTest.class.getDeclaredMethod("dummy", CreateRoomRequest.class);
    return new MethodParameter(method, 0);
  }

  private static HttpServletRequest httpRequest(String method, String uri) {
    var request = new MockHttpServletRequest(method, uri);
    request.setServerName("localhost");
    request.setServerPort(8080);
    return request;
  }
}
