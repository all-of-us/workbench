package org.pmiops.workbench.fileArtifacts;

import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class FileArtifactsRetryHandler extends RetryHandler<ApiException> {

  private static final Logger logger = Logger.getLogger(FileArtifactsRetryHandler.class.getName());

  private static class FileArtifactRetryPolicy extends ResponseCodeRetryPolicy {

    public FileArtifactRetryPolicy() {
      super("FileArtifacts API");
    }

    @Override
    protected int getResponseCode(Throwable lastException) {
      if (lastException instanceof ApiException) {
        return ((ApiException) lastException).getCode();
      }
      if (lastException instanceof SocketTimeoutException) {
        return HttpServletResponse.SC_GATEWAY_TIMEOUT;
      }
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    protected void logNoRetry(Throwable t, int responseCode) {
      if (t instanceof ApiException) {
        logger.log(
            getLogLevel(responseCode),
            String.format(
                "Exception calling FileArtifacts API with response: %s",
                ((ApiException) t).getResponseBody()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  @Autowired
  public FileArtifactsRetryHandler(BackOffPolicy backoffPolicy) {
    super(backoffPolicy, new FileArtifactRetryPolicy());
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return ExceptionUtils.convertFileArtifactException(exception);
  }
}
