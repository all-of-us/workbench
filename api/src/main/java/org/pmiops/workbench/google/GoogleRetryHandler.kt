package org.pmiops.workbench.google

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException
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
class GoogleRetryHandler @Autowired
constructor(backOffPolicy: BackOffPolicy) : RetryHandler<IOException>(backOffPolicy, GoogleRetryPolicy()) {

    private class GoogleRetryPolicy : RetryConfig.ResponseCodeRetryPolicy("Google API") {

        override fun getResponseCode(lastException: Throwable): Int {
            if (lastException is GoogleJsonResponseException) {
                return lastException.statusCode
            }
            return if (lastException is SocketTimeoutException) {
                HttpServletResponse.SC_GATEWAY_TIMEOUT
            } else HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }

        override fun canRetry(code: Int): Boolean {
            // Google services are known to throw 500 errors sometimes when it would be appropriate
            // to retry. So we will retry in these cases, too.
            return super.canRetry(code) || code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }

        override fun logNoRetry(t: Throwable, responseCode: Int) {
            if (t is GoogleJsonResponseException) {
                logger.log(
                        getLogLevel(responseCode),
                        String.format(
                                "Exception calling Google API with response: %s",
                                t.details),
                        t)
            } else {
                super.logNoRetry(t, responseCode)
            }
        }
    }

    override fun convertException(exception: IOException): WorkbenchException {
        return ExceptionUtils.convertGoogleIOException(exception)
    }

    companion object {

        private val logger = Logger.getLogger(GoogleRetryHandler::class.java.name)
    }
}
