package org.pmiops.workbench.exceptions;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;

/**
 * Utility methods related to exceptions.
 */
public class ExceptionUtils {

  private static final Logger log = Logger.getLogger(ExceptionUtils.class.getName());

  public static boolean isGoogleServiceUnavailableException(IOException e) {
    // We assume that any 500 range error for Google is something we should retry.
    if (e instanceof GoogleJsonResponseException) {
      int code = ((GoogleJsonResponseException) e).getDetails().getCode();
      return code >= 500 && code < 600;
    }
    return false;
  }

  public static boolean isGoogleConflictException(IOException e) {
    if (e instanceof GoogleJsonResponseException) {
      int code = ((GoogleJsonResponseException) e).getDetails().getCode();
      return code == 409;
    }
    return false;
  }

  public static WorkbenchException convertGoogleIOException(IOException e) {
    if (isGoogleServiceUnavailableException(e)) {
      throw new ServerUnavailableException(e);
    } else if (isGoogleConflictException(e)) {
      throw new ConflictException(e);
    }
    throw new ServerErrorException(e);
  }

  public static boolean isSocketTimeoutException(Throwable e) {
    return (e instanceof SocketTimeoutException);
  }


  public static WorkbenchException convertFirecloudException(ApiException e) {
    if (isSocketTimeoutException(e.getCause())) {
      throw new GatewayTimeoutException();
    }
    throw codeToException(e.getCode());
  }

  public static WorkbenchException convertNotebookException(
      org.pmiops.workbench.notebooks.ApiException e) {
    if (isSocketTimeoutException(e.getCause())) {
      throw new GatewayTimeoutException();
    }
    throw codeToException(e.getCode());
  }

  public static boolean isServiceUnavailable(int code) {
    return code == HttpServletResponse.SC_SERVICE_UNAVAILABLE
        || code == HttpServletResponse.SC_BAD_GATEWAY
        || code == HttpServletResponse.SC_GATEWAY_TIMEOUT;
  }

  private static RuntimeException codeToException(int code) {

    if (code == HttpStatus.NOT_FOUND.value()) {
      return new NotFoundException();
    } else if (code == HttpServletResponse.SC_UNAUTHORIZED) {
      return new UnauthorizedException();
    } else if (code == HttpServletResponse.SC_FORBIDDEN) {
      return new ForbiddenException();
    } else if (isServiceUnavailable(code)) {
      return new ServerUnavailableException();
    } else if (code == HttpServletResponse.SC_CONFLICT) {
      return new ConflictException();
    } else {
      return new ServerErrorException();
    }
  }

  public static ErrorResponse errorResponse(String message) {
    return errorResponse(null, message);
  }

  public static ErrorResponse errorResponse(ErrorCode code, String message) {
    ErrorResponse response = new ErrorResponse();
    response.setMessage(message);
    response.setErrorCode(code);
    return response;
  }

  private ExceptionUtils() {}
}
