package org.pmiops.workbench.exceptions;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.notebooks.ApiException;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionUtilsTest {

  @Test
void convertNotebookException() throws Exception {
    assertThrows(GatewayTimeoutException.class,()->{
    ApiException cause = new ApiException(new SocketTimeoutException());
    ExceptionUtils.convertNotebookException(cause);
  });
}
}
