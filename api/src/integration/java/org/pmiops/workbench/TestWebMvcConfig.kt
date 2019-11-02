package org.pmiops.workbench

import org.pmiops.workbench.config.WebMvcConfig
import org.pmiops.workbench.config.WorkbenchEnvironment
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestWebMvcConfig : WebMvcConfig() {

    @Bean
    @Primary
    override fun workbenchEnvironment(): WorkbenchEnvironment {
        return WorkbenchEnvironment(true, "appId")
    }
}
