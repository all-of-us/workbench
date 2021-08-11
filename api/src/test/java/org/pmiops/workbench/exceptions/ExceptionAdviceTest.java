package org.pmiops.workbench.exceptions;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.ResponseEntity;

public class ExceptionAdviceTest {

  private ExceptionAdvice exceptionAdvice;

  @BeforeEach
  public void setUp() {
    exceptionAdvice = new ExceptionAdvice();
  }

  private static Stream<Arguments> serverErrorParameters() {
    return Stream.of(
        Arguments.of(new BadRequestException(), 400, null, null),
        Arguments.of(new ConflictException(), 409, null, null),
        Arguments.of(new FailedPreconditionException(), 412, null, null),
        Arguments.of(new TooManyRequestsException(), 429, null, null),
        Arguments.of(new ServerErrorException(), 500, null, null),
        Arguments.of(new NullPointerException(), 500, null, null),
        Arguments.of(new TooManyRequestsException(new Exception("inner")), 429, null, null),
        Arguments.of(new GatewayTimeoutException("foo"), 504, "foo", null),
        Arguments.of(new Exception("internal message not shown"), 500, null, null),
        Arguments.of(
            new ForbiddenException(
                WorkbenchException.errorResponse("disabled", ErrorCode.USER_DISABLED)),
            403,
            "disabled",
            ErrorCode.USER_DISABLED));
  }

  @ParameterizedTest
  @MethodSource("serverErrorParameters")
  public void serverError(
      Exception e,
      int wantStatusCode,
      @Nullable String wantMessage,
      @Nullable ErrorCode wantErrorCode) {
    ResponseEntity<?> resp = exceptionAdvice.serverError(e);
    assertThat(resp).isNotNull();
    assertThat(resp.getStatusCodeValue()).isEqualTo(wantStatusCode);

    ErrorResponse errResponse = (ErrorResponse) resp.getBody();
    assertThat(errResponse).isNotNull();
    assertThat(errResponse.getMessage()).isEqualTo(wantMessage);
    assertThat(errResponse.getErrorCode()).isEqualTo(wantErrorCode);
  }
}
