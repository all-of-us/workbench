package org.pmiops.workbench.exceptions;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.notebooks.ApiException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionUtilsTest {

  @Test
  public void convertNotebookException() throws Exception {
    ApiException cause = new ApiException(new SocketTimeoutException());
    assertThrows(GatewayTimeoutException.class, () -> ExceptionUtils.convertNotebookException(cause));
  }
}
