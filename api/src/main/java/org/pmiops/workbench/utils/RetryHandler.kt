package org.pmiops.workbench.utils

import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.exceptions.WorkbenchException
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryException
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.support.RetryTemplate

abstract class RetryHandler<E : Exception>(private val retryTemplate: RetryTemplate) {

    private fun retryTemplate(backOffPolicy: BackOffPolicy, retryPolicy: RetryPolicy): RetryTemplate {
        val retryTemplate = RetryTemplate()
        retryTemplate.setBackOffPolicy(backOffPolicy)
        retryTemplate.setRetryPolicy(retryPolicy)
        retryTemplate.setThrowLastExceptionOnExhausted(true)
        return retryTemplate
    }

    constructor(backOffPolicy: BackOffPolicy, retryPolicy: RetryPolicy) : this(retryTemplate(backOffPolicy, retryPolicy)) {}

    fun <T> run(retryCallback: RetryCallback<T, E>): T {
        try {
            return retryTemplate.execute(retryCallback)
        } catch (retryException: RetryException) {
            throw ServerErrorException(retryException.cause)
        } catch (exception: Exception) {
            throw convertException(exception as E)
        }

    }

    @Throws(E::class)
    fun <T> runAndThrowChecked(retryCallback: RetryCallback<T, E>): T {
        return retryTemplate.execute(retryCallback)
    }

    protected abstract fun convertException(exception: E): WorkbenchException
}
