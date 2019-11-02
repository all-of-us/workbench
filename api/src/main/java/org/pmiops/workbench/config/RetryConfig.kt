package org.pmiops.workbench.config

import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpServletResponse
import org.pmiops.workbench.exceptions.ExceptionUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.RetryContext
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy
import org.springframework.retry.backoff.Sleeper
import org.springframework.retry.backoff.ThreadWaitSleeper
import org.springframework.retry.policy.SimpleRetryPolicy

@Configuration
class RetryConfig {

    abstract class ResponseCodeRetryPolicy(private val serviceName: String) : SimpleRetryPolicy() {

        override fun canRetry(context: RetryContext): Boolean {
            // canRetry is (counter-intuitively) invoked before the first attempt;
            // in that scenario, getLastThrowable() returns null and we should proceed.
            if (context.lastThrowable == null) {
                return true
            }
            val lastException = context.lastThrowable
            val responseCode = getResponseCode(lastException)
            if (canRetry(responseCode)) {
                if (context.retryCount < maxAttempts) {
                    logRetry(context.retryCount, lastException)
                    return true
                } else {
                    logGivingUp(context.retryCount, lastException)
                    return false
                }
            } else {
                logNoRetry(lastException, responseCode)
                return false
            }
        }

        protected open fun canRetry(code: Int): Boolean {
            return ExceptionUtils.isServiceUnavailable(code)
        }

        protected fun logRetry(retryCount: Int, t: Throwable) {
            logger.log(
                    Level.WARNING,
                    String.format("%s unavailable, retrying after %d attempts", serviceName, retryCount),
                    t)
        }

        protected fun logGivingUp(retryCount: Int, t: Throwable) {
            logger.log(
                    Level.WARNING,
                    String.format("%s unavailable, giving up after %d attempts", serviceName, retryCount),
                    t)
        }

        protected fun getLogLevel(responseCode: Int): Level {
            when (responseCode) {
                HttpServletResponse.SC_NOT_FOUND -> return Level.INFO
                HttpServletResponse.SC_UNAUTHORIZED, HttpServletResponse.SC_FORBIDDEN, HttpServletResponse.SC_CONFLICT -> return Level.WARNING
                else -> return Level.SEVERE
            }
        }

        protected open fun logNoRetry(t: Throwable, responseCode: Int) {
            logger.log(getLogLevel(responseCode), String.format("Exception calling %s", serviceName), t)
        }

        protected abstract fun getResponseCode(lastException: Throwable): Int

        companion object {

            private val logger = Logger.getLogger(ResponseCodeRetryPolicy::class.java.name)
        }
    }

    @Bean
    fun sleeper(): Sleeper {
        return ThreadWaitSleeper()
    }

    @Bean
    fun backOffPolicy(sleeper: Sleeper): BackOffPolicy {
        // Defaults to 100ms initial interval, doubling each time, with some random multiplier.
        val policy = ExponentialRandomBackOffPolicy()
        // Set max interval of 20 seconds.
        policy.maxInterval = 20000
        policy.setSleeper(sleeper)
        return policy
    }
}
