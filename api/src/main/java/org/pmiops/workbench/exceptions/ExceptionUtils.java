package org.pmiops.workbench.exceptions;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.model.ErrorResponse;

/**
 * Utility methods related to exceptions.
 */
public class ExceptionUtils {

  private static final Logger log = Logger.getLogger(ExceptionUtils.class.getName());

  private static final int MAX_ATTEMPTS = 3;

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

  public static RuntimeException convertGoogleIOException(IOException e) {
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


  public static RuntimeException convertFirecloudException(ApiException e) {
    log.log(e.getCode() >= 500 ? Level.SEVERE : Level.INFO, "Exception calling FireCloud", e);
    if (isSocketTimeoutException(e.getCause())) {
      throw new GatewayTimeoutException();
    }
    throw codeToException(e.getCode());
  }

  public static RuntimeException convertNotebookException(
      org.pmiops.workbench.notebooks.ApiException e) {
    log.log(e.getCode() >= 500 ? Level.SEVERE : Level.INFO, "Exception calling notebooks API", e);
    if (isSocketTimeoutException(e)) {
      throw new GatewayTimeoutException();
    }
    throw codeToException(e.getCode());
  }

  private static RuntimeException codeToException(int code) {

    if (code == HttpServletResponse.SC_NOT_FOUND) {
      return new NotFoundException();
    } else if (code == HttpServletResponse.SC_FORBIDDEN) {
      return new ForbiddenException();
    } else if (code == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
      return new ServerUnavailableException();
    } else if (code == HttpServletResponse.SC_CONFLICT) {
      return new ConflictException();
    } else {
      return new ServerErrorException();
    }
  }

  public static <T> T executeWithRetries(AbstractGoogleClientRequest<T> request)
      throws IOException {
    int numAttempts = 0;
    // Retry on 503 exceptions.
    while (true) {
      try {
        return request.execute();
      } catch (IOException e) {
        numAttempts++;
        if (isGoogleServiceUnavailableException(e)) {
          if (numAttempts > 1 && numAttempts < MAX_ATTEMPTS) {
            log.log(Level.SEVERE,
                String.format("Service unavailable, attempt %s; retrying...", numAttempts), e);
            try {
              // Sleep with some backoff.
              Thread.sleep(2000 * numAttempts );
            } catch (InterruptedException e2) {
              throw e;
            }
            continue;
          }
        }
        throw e;
      }
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
