package org.pmiops.workbench.audit

import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID
import javax.inject.Provider

@Configuration
open class ActionAuditSpringConfiguration {

    open val cloudLogging: Logging
        @Bean
        get() = LoggingOptions.getDefaultInstance().service

    open val actionIdProvider: Provider<String>
        @Bean(name = ["ACTION_ID_PROVIDER"])
        get() {
            return Provider { UUID.randomUUID().toString() }
        }
}
