package org.pmiops.workbench.exceptions

import org.pmiops.workbench.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Returns a 503 to the client, indicating that the service temporarily can't handle the request and
 * the client should retry.
 *
 *
 * Use [ServerErrorException] instead when there's a bug / condition we haven't figured out
 * how to handle yet.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ServerUnavailableException : WorkbenchException {
    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(errorResponse: ErrorResponse) : super(errorResponse) {}

    constructor(t: Throwable) : super(t) {}

    constructor(message: String, t: Throwable) : super(message, t) {}
}
