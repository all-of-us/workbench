package org.pmiops.workbench.db;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataAccessException;

public class DbRetryUtilsTest {
  private static final CannotSerializeTransactionException RETRY_EXCEPTION =
      new CannotSerializeTransactionException("test");

  @SuppressWarnings("unchecked")
  @Mock
  DbRetryUtils.DatabaseOperation<Boolean> mockDatabaseOperation =
      mock(DbRetryUtils.DatabaseOperation.class);

  @Test
  public void succeedAfterRetry() throws Exception {
    // Throw retryable exception 3 times then succeed.
    when(mockDatabaseOperation.execute())
        .thenThrow(RETRY_EXCEPTION)
        .thenThrow(RETRY_EXCEPTION)
        .thenThrow(RETRY_EXCEPTION)
        .thenReturn(true);
    assertThat(DbRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 10))
        .isTrue();
    verify(mockDatabaseOperation, times(4)).execute();
  }

  @Test
  public void exceedMaxRetry() throws Exception {
    when(mockDatabaseOperation.execute()).thenThrow(RETRY_EXCEPTION);
    DataAccessException finalException =
        assertThrows(
            DataAccessException.class,
            () -> DbRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 3));
    verify(mockDatabaseOperation, times(3)).execute();
    assertThat(finalException).isEqualTo(RETRY_EXCEPTION);
  }

  @Test
  public void nonRetryableExecute() throws Exception {
    when(mockDatabaseOperation.execute()).thenThrow(new RuntimeException("non-retryable"));
    RuntimeException finalException =
        assertThrows(
            RuntimeException.class,
            () -> DbRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 3));

    verify(mockDatabaseOperation, times(1)).execute();
    assertThat(finalException.getMessage()).isEqualTo("non-retryable");
  }

  @Test
  public void zeroMaxAttemptsInvalid() throws Exception {
    when(mockDatabaseOperation.execute()).thenReturn(true);
    assertThrows(
        IllegalArgumentException.class,
        () -> DbRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 0));
  }
}
