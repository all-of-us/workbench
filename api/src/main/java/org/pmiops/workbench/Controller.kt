package org.pmiops.workbench

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller {
    @RequestMapping("/")
    fun index(): String {
        return "AllOfUs Workbench API"
    }
}
