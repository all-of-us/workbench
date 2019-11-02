package org.pmiops.workbench.testconfig

import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.CdrConfig
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class WorkbenchConfigConfig {

    @Bean
    fun workbenchConfig(): WorkbenchConfig {
        val workbenchConfig = WorkbenchConfig()
        workbenchConfig.cdr = CdrConfig()
        workbenchConfig.cdr.debugQueries = true
        workbenchConfig.firecloud = FireCloudConfig()
        workbenchConfig.firecloud.registeredDomainName = "all-of-us-registered-test"
        return workbenchConfig
    }
}
