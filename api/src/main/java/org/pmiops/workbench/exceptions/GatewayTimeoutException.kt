package org.pmiops.workbench.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
class GatewayTimeoutException : WorkbenchException {
    constructor() : super() {}

    constructor(message: String) : super(message) {}
}
