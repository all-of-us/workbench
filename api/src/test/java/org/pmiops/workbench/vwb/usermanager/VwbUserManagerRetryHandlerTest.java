package org.pmiops.workbench.vwb.usermanager;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.vwb.user.ApiException;
import org.springframework.retry.backoff.NoBackOffPolicy;

/**
 * Covers what the handler logs when User Manager rejects a call. Diagnosing these failures from our
 * own logs is the whole point, so the response code and an indication of an empty body have to be
 * there.
 */
public class VwbUserManagerRetryHandlerTest {
  private final List<LogRecord> logRecords = new ArrayList<>();
  private Logger logger;
  private Handler captureHandler;
  private VwbUserManagerRetryHandler retryHandler;

  @BeforeEach
  public void setUp() {
    // The message is emitted by the shared policy, not by the per-service handler.
    logger = Logger.getLogger(ResponseCodeRetryPolicy.class.getName());
    logger.setLevel(Level.ALL);
    captureHandler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            logRecords.add(record);
          }

          @Override
          public void flush() {
            // Records are kept in memory, so there is nothing to flush.
          }

          @Override
          public void close() {
            // Records are kept in memory, so there is nothing to close.
          }
        };
    logger.addHandler(captureHandler);
    retryHandler = new VwbUserManagerRetryHandler(new NoBackOffPolicy(), () -> null);
  }

  @AfterEach
  public void tearDown() {
    logger.removeHandler(captureHandler);
  }

  private String runAndCaptureLog(
      ApiException toThrow, Class<? extends WorkbenchException> thrown) {
    assertThrows(
        thrown,
        () ->
            retryHandler.run(
                context -> {
                  throw toThrow;
                }));
    // The handler emits more than one record per failure, so pick out the one describing the
    // User Manager response.
    return logRecords.stream()
        .map(LogRecord::getMessage)
        .filter(m -> m.contains("Exception calling User Manager API"))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "No User Manager response log record found in "
                        + logRecords.stream().map(LogRecord::getMessage).toList()));
  }

  @Test
  public void testLogsResponseCodeAndBody() {
    String message =
        runAndCaptureLog(
            new ApiException(400, "Bad Request", null, "{\"message\":\"Title is required\"}"),
            WorkbenchException.class);

    assertThat(message).contains("HTTP 400");
    assertThat(message).contains("Title is required");
  }

  @Test
  public void testLogsEmptyBodyResponse() {
    // Deleting an already-inactive notification is a bare 404 from User Manager. Logging only the
    // body printed a blank line, which is what made this undiagnosable from our logs.
    String message =
        runAndCaptureLog(new ApiException(404, "Not Found", null, ""), NotFoundException.class);

    assertThat(message).contains("HTTP 404");
    assertThat(message).contains("<empty body>");
  }

  @Test
  public void testLogsNullBodyResponse() {
    String message =
        runAndCaptureLog(new ApiException(404, "Not Found", null, null), NotFoundException.class);

    assertThat(message).contains("HTTP 404");
    assertThat(message).contains("<empty body>");
  }
}
