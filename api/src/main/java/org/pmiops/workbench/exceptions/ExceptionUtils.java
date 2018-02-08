package org.pmiops.workbench.exceptions;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.firecloud.ApiException;
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

  public static boolean isGoogleBadRequestException(IOException e) {
    if (e instanceof GoogleJsonResponseException) {
      int code = ((GoogleJsonResponseException) e).getDetails().getCode();
      return code == 400;
    }
    return false;
  }

  public static RuntimeException convertGoogleIOException(IOException e) {
    if (isGoogleServiceUnavailableException(e)) {
      throw new ServerUnavailableException(e);
    } else if (isGoogleConflictException(e)) {
      throw new ConflictException(e);
    } else if (isGoogleBadRequestException(e)) {
      throw new BadRequestException(e);
    }
    throw new ServerErrorException(e);
  }

  public static RuntimeException convertFirecloudException(ApiException e) {
    log.log(e.getCode() >= 500 ? Level.SEVERE : Level.INFO, "Exception calling FireCloud", e);
    if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
      throw new NotFoundException();
    } else if (e.getCode() == HttpServletResponse.SC_FORBIDDEN) {
      throw new ForbiddenException();
    } else if (e.getCode() == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
      throw new ServerUnavailableException();
    } else {
      throw new ServerErrorException();
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
    ErrorResponse response = new ErrorResponse();
    response.setMessage(message);
    return response;
  }

  private ExceptionUtils() {}
}
