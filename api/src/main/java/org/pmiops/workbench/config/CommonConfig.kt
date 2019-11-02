package org.pmiops.workbench.config

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import java.security.SecureRandom
import java.time.Clock
import java.util.Random
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class CommonConfig {

    @Bean
    internal fun jsonFactory(): JsonFactory {
        return JacksonFactory()
    }

    @Bean
    internal fun clock(): Clock {
        return Clock.systemUTC()
    }

    @Bean
    internal fun random(): Random {
        return SecureRandom()
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    internal fun googleCredentialBuilder(): GoogleCredential.Builder {
        return GoogleCredential.Builder()
    }
}
