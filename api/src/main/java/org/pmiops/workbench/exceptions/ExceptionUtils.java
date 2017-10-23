package org.pmiops.workbench.exceptions;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  public static RuntimeException convertGoogleIOException(IOException e) {
    if (isGoogleServiceUnavailableException(e)) {
      throw new ServerUnavailableException(e);
    }
    throw new ServerErrorException(e);
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
                "Service unavailable, attempt #{0}; retrying..."
                    .format(String.valueOf(numAttempts)), e);
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


  private ExceptionUtils() {}
}
