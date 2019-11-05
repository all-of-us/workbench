package org.pmiops.workbench.audit

import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.util.UUID
import javax.inject.Provider

@Configuration
open class ActionAuditSpringConfiguration {

    open val cloudLogging: Logging
        @Bean
        get() = LoggingOptions.getDefaultInstance().service

    open val actionId: String
        @Bean(name = ["ACTION_ID"])
        @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        get() {
            return UUID.randomUUID().toString()
        }
}
