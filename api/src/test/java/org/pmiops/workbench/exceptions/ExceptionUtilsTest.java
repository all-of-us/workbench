package org.pmiops.workbench.exceptions;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionUtilsTest {

  @Test
  public void convertNotebookException() throws Exception {
    org.pmiops.workbench.notebooks.ApiException cause =
        new org.pmiops.workbench.notebooks.ApiException(new SocketTimeoutException());
    assertThrows(
        GatewayTimeoutException.class, () -> ExceptionUtils.convertNotebookException(cause));
  }

  @Test
  public void convertLeonardoConflictException() throws Exception {
    org.pmiops.workbench.leonardo.ApiException cause =
        new org.pmiops.workbench.leonardo.ApiException(new ConflictException());
    WorkbenchException leonardoException =
        assertThrows(ConflictException.class, () -> ExceptionUtils.convertLeonardoException(cause));
    Assertions.assertEquals(
        leonardoException.getMessage(),
        "Please wait a few minutes and try to create your environment again.");
  }
}
