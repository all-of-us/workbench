package org.pmiops.workbench.exceptions

import java.net.SocketTimeoutException
import org.junit.Test
import org.pmiops.workbench.notebooks.ApiException

class ExceptionUtilsTest {

    @Test(expected = GatewayTimeoutException::class)
    @Throws(Exception::class)
    fun convertNotebookException() {
        val cause = ApiException(SocketTimeoutException())
        ExceptionUtils.convertNotebookException(cause)
    }
}
