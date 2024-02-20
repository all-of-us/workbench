package org.pmiops.workbench.audit

import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.util.UUID

@Configuration
open class ActionAuditSpringConfiguration {
    companion object {
        const val ACTION_ID_BEAN = "ACTION_ID"
    }

    open val cloudLogging: Logging
        @Bean
        get() = LoggingOptions.getDefaultInstance().service

    open val actionId: String
        @Bean(name = [ACTION_ID_BEAN])
        @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        get() {
            return UUID.randomUUID().toString()
        }
}
