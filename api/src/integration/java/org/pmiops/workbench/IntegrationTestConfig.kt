package org.pmiops.workbench

import org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.common.io.Resources
import com.google.gson.Gson
import java.io.IOException
import java.nio.charset.Charset
import org.pmiops.workbench.auth.Constants
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.config.CommonConfig
import org.pmiops.workbench.config.RetryConfig
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.firecloud.ApiClient
import org.pmiops.workbench.firecloud.FireCloudConfig
import org.pmiops.workbench.google.CloudStorageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy
import org.springframework.retry.backoff.Sleeper
import org.springframework.retry.backoff.ThreadWaitSleeper

@Configuration
@Import(RetryConfig::class, CommonConfig::class)
// Scan the google package, which we need for the CloudStorage bean.
@ComponentScan("org.pmiops.workbench.google")
// Scan the ServiceAccounts class, but exclude other classes in auth (since they
// bring in JPA-related beans, which include a whole bunch of other deps that are
// more complicated than we need for now).
//
// TODO(gjuggler): move ServiceAccounts out of the auth package, or move the more
// dependency-ridden classes (e.g. ProfileService) out instead.
@ComponentScan(basePackageClasses = [ServiceAccounts::class], useDefaultFilters = false, includeFilters = [ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = ServiceAccounts::class)])
class IntegrationTestConfig {

    @Lazy
    @Bean(name = [Constants.GSUITE_ADMIN_CREDS])
    internal fun gsuiteAdminCredentials(cloudStorageService: CloudStorageService): GoogleCredential {
        try {
            return cloudStorageService.gSuiteAdminCredentials
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @Lazy
    @Bean(name = [Constants.FIRECLOUD_ADMIN_CREDS])
    internal fun fireCloudCredentials(cloudStorageService: CloudStorageService): GoogleCredential {
        try {
            return cloudStorageService.fireCloudAdminCredentials
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @Lazy
    @Bean(name = [Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS])
    internal fun cloudResourceManagerCredentials(cloudStorageService: CloudStorageService): GoogleCredential {
        try {
            return cloudStorageService.cloudResourceManagerAdminCredentials
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @Lazy
    @Bean(name = [Constants.DEFAULT_SERVICE_ACCOUNT_CREDS])
    internal fun defaultServiceAccountCredentials(cloudStorageService: CloudStorageService): GoogleCredential {
        try {
            return cloudStorageService.defaultServiceAccountCredentials
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @Bean
    @Lazy
    @Throws(IOException::class)
    internal fun workbenchConfig(): WorkbenchConfig {
        val testConfig = Resources.toString(Resources.getResource("config_test.json"), Charset.defaultCharset())
        val workbenchConfig = Gson().fromJson(testConfig, WorkbenchConfig::class.java)
        workbenchConfig.firecloud.debugEndpoints = true
        return workbenchConfig
    }

    @Bean(name = [FireCloudConfig.END_USER_API_CLIENT])
    internal fun endUserApiClient(): ApiClient? {
        // Integration tests can't make calls using user credentials.
        return null
    }

    /**
     * Returns the Apache HTTP transport. Compare to CommonConfig which returns the App Engine HTTP
     * transport.
     *
     * @return
     */
    @Bean
    internal fun httpTransport(): HttpTransport {
        return ApacheHttpTransport()
    }

    @Bean
    fun sleeper(): Sleeper {
        return ThreadWaitSleeper()
    }

    @Bean
    fun backOffPolicy(sleeper: Sleeper): BackOffPolicy {
        // Defaults to 100ms initial interval, doubling each time, with some random multiplier.
        val policy = ExponentialRandomBackOffPolicy()
        // Set max interval of 20 seconds.
        policy.maxInterval = 20000
        policy.setSleeper(sleeper)
        return policy
    }
}
