package org.pmiops.workbench.notebooks

import java.net.SocketTimeoutException
import java.util.logging.Logger
import javax.servlet.http.HttpServletResponse
import org.pmiops.workbench.config.RetryConfig
import org.pmiops.workbench.exceptions.ExceptionUtils
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.utils.RetryHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.stereotype.Service

@Service
class NotebooksRetryHandler @Autowired
constructor(backoffPolicy: BackOffPolicy) : RetryHandler<ApiException>(backoffPolicy, NotebookRetryPolicy()) {

    private class NotebookRetryPolicy : RetryConfig.ResponseCodeRetryPolicy("Notebooks API") {

        override fun getResponseCode(lastException: Throwable): Int {
            if (lastException is ApiException) {
                return (lastException as ApiException).getCode()
            }
            return if (lastException is SocketTimeoutException) {
                HttpServletResponse.SC_GATEWAY_TIMEOUT
            } else HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }

        override fun logNoRetry(t: Throwable, responseCode: Int) {
            if (t is ApiException) {
                logger.log(
                        getLogLevel(responseCode),
                        String.format(
                                "Exception calling Notebooks API with response: %s",
                                (t as ApiException).getResponseBody()),
                        t)
            } else {
                super.logNoRetry(t, responseCode)
            }
        }
    }

    override fun convertException(exception: ApiException): WorkbenchException {
        return ExceptionUtils.convertNotebookException(exception)
    }

    companion object {

        private val logger = Logger.getLogger(NotebooksRetryHandler::class.java.name)
    }
}
