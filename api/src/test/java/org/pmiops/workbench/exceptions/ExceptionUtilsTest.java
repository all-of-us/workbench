package org.pmiops.workbench.exceptions;

import java.net.SocketTimeoutException;
import org.junit.Test;
import org.pmiops.workbench.notebooks.ApiException;

public class ExceptionUtilsTest {

  @Test(expected = GatewayTimeoutException.class)
  public void convertNotebookException() throws Exception {
    ApiException cause = new ApiException(new SocketTimeoutException());
    ExceptionUtils.convertNotebookException(cause);
  }
}
