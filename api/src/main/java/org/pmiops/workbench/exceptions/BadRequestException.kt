package org.pmiops.workbench.exceptions

import org.pmiops.workbench.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException : WorkbenchException {
    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(errorResponse: ErrorResponse) : super(errorResponse) {}

    constructor(t: Throwable) : super(t) {}

    constructor(message: String, t: Throwable) : super(message, t) {}
}
