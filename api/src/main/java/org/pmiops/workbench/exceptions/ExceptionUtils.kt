package org.pmiops.workbench.exceptions

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.servlet.http.HttpServletResponse
import org.pmiops.workbench.firecloud.ApiException
import org.springframework.http.HttpStatus

/** Utility methods related to exceptions.  */
object ExceptionUtils {

    fun isGoogleServiceUnavailableException(e: IOException): Boolean {
        // We assume that any 500 range error for Google is something we should retry.
        if (e is GoogleJsonResponseException) {
            val code = e.details.code
            return code >= 500 && code < 600
        }
        return false
    }

    fun isGoogleConflictException(e: IOException): Boolean {
        if (e is GoogleJsonResponseException) {
            val code = e.details.code
            return code == 409
        }
        return false
    }

    fun convertGoogleIOException(e: IOException): WorkbenchException {
        if (isGoogleServiceUnavailableException(e)) {
            throw ServerUnavailableException(e)
        } else if (isGoogleConflictException(e)) {
            throw ConflictException(e)
        }
        throw ServerErrorException(e)
    }

    fun isSocketTimeoutException(e: Throwable): Boolean {
        return e is SocketTimeoutException
    }

    fun convertFirecloudException(e: ApiException): WorkbenchException {
        if (isSocketTimeoutException(e.getCause())) {
            throw GatewayTimeoutException()
        }
        throw codeToException(e.getCode())
    }

    fun convertNotebookException(
            e: org.pmiops.workbench.notebooks.ApiException): WorkbenchException {
        if (isSocketTimeoutException(e.getCause())) {
            throw GatewayTimeoutException()
        }
        throw codeToException(e.getCode())
    }

    fun isServiceUnavailable(code: Int): Boolean {
        return code == HttpServletResponse.SC_SERVICE_UNAVAILABLE || code == HttpServletResponse.SC_BAD_GATEWAY
    }

    private fun codeToException(code: Int): RuntimeException {

        return if (code == HttpStatus.NOT_FOUND.value()) {
            NotFoundException()
        } else if (code == HttpServletResponse.SC_BAD_REQUEST) {
            BadRequestException()
        } else if (code == HttpServletResponse.SC_UNAUTHORIZED) {
            UnauthorizedException()
        } else if (code == HttpServletResponse.SC_FORBIDDEN) {
            ForbiddenException()
        } else if (isServiceUnavailable(code)) {
            ServerUnavailableException()
        } else if (code == HttpServletResponse.SC_GATEWAY_TIMEOUT) {
            GatewayTimeoutException()
        } else if (code == HttpServletResponse.SC_CONFLICT) {
            ConflictException()
        } else {
            ServerErrorException()
        }
    }
}
