package org.pmiops.workbench.actionaudit

import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ActionAuditSpringConfiguration {

    open val cloudLogging: Logging
        @Bean
        get() = LoggingOptions.getDefaultInstance().service
}
