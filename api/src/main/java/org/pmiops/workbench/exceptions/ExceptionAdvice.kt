package org.pmiops.workbench.exceptions

import java.util.logging.Level
import java.util.logging.Logger
import org.pmiops.workbench.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ExceptionAdvice {

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun messageNotReadableError(e: Exception): ResponseEntity<*> {
        log.log(Level.INFO, "failed to parse HTTP request message, returning 400", e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        WorkbenchException.errorResponse("failed to parse valid JSON request message")
                                .statusCode(HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(Exception::class)
    fun serverError(e: Exception): ResponseEntity<*> {
        val errorResponse = ErrorResponse()
        var statusCode: Int? = HttpStatus.INTERNAL_SERVER_ERROR.value()

        // if this error was thrown by another error, get the info from that exception
        var relevantError: Throwable = e
        if (e.cause != null) {
            relevantError = e.cause
        }

        // if exception class has an HTTP status associated with it, grab it
        if (relevantError.javaClass.getAnnotation(ResponseStatus::class.java) != null) {
            statusCode = relevantError.javaClass.getAnnotation(ResponseStatus::class.java).value().value()
        }
        if (relevantError is WorkbenchException) {
            // Only include Exception details on Workbench errors.
            errorResponse.setMessage(relevantError.message)
            errorResponse.setErrorClassName(relevantError.javaClass.name)
            val workbenchException = relevantError
            if (workbenchException.errorResponse != null && workbenchException.errorResponse.getErrorCode() != null) {
                errorResponse.setErrorCode(workbenchException.errorResponse.getErrorCode())
            }
        }

        // only log error if it's a server error
        if (statusCode >= 500) {
            log.log(Level.SEVERE, relevantError.javaClass.name, e)
        }

        errorResponse.setStatusCode(statusCode)
        return ResponseEntity.status(statusCode!!).body<Any>(errorResponse)
    }

    companion object {
        private val log = Logger.getLogger(ExceptionAdvice::class.java.name)
    }
}
