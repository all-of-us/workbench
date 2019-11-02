package org.pmiops.workbench.config

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.oauth2.model.Userinfoplus
import java.io.IOException
import java.io.InputStream
import javax.servlet.ServletContext
import org.pmiops.workbench.auth.Constants
import org.pmiops.workbench.auth.UserAuthentication
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.interceptors.AuthInterceptor
import org.pmiops.workbench.interceptors.ClearCdrVersionContextInterceptor
import org.pmiops.workbench.interceptors.CorsInterceptor
import org.pmiops.workbench.interceptors.CronInterceptor
import org.pmiops.workbench.interceptors.SecurityHeadersInterceptor
import org.pmiops.workbench.interceptors.TracingInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = ["org.pmiops.workbench.interceptors", "org.pmiops.workbench.google"])
open class WebMvcConfig : WebMvcConfigurerAdapter() {

    @Autowired
    private val authInterceptor: AuthInterceptor? = null

    @Autowired
    private val corsInterceptor: CorsInterceptor? = null

    @Autowired
    private val clearCdrVersionInterceptor: ClearCdrVersionContextInterceptor? = null

    @Autowired
    private val cronInterceptor: CronInterceptor? = null

    @Autowired
    private val securityHeadersInterceptor: SecurityHeadersInterceptor? = null

    @Autowired
    private val tracingInterceptor: TracingInterceptor? = null

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun userAuthentication(): UserAuthentication {
        return SecurityContextHolder.getContext().authentication as UserAuthentication
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun userInfo(userAuthentication: UserAuthentication): Userinfoplus {
        return userAuthentication.principal
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun user(userAuthentication: UserAuthentication): User {
        return userAuthentication.user
    }

    @Bean
    open fun workbenchEnvironment(): WorkbenchEnvironment {
        return WorkbenchEnvironment()
    }

    /**
     * Service account credentials for Gsuite administration. These are derived from a key JSON file
     * copied from GCS deployed to /WEB-INF/gsuite-admin-sa.json during the build step. They can be
     * used to make API calls to directory service on behalf of AofU (as opposed to using end user
     * credentials.)
     *
     *
     * We may in future rotate key files in production, but will be sure to keep the ones currently
     * in use in cloud environments working when that happens.
     *
     *
     * TODO(gjuggler): should we start pulling this file from GCS instead?
     */
    @Lazy
    @Bean(name = [Constants.GSUITE_ADMIN_CREDS])
    fun gsuiteAdminCredential(): GoogleCredential {
        val context = requestServletContext
        val saFileAsStream = context.getResourceAsStream("/WEB-INF/gsuite-admin-sa.json")
        try {
            return GoogleCredential.fromStream(saFileAsStream)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Service account credentials for FireCloud administration. This Service Account has been enabled
     * for domain-wide delegation of authority.
     */
    @Lazy
    @Bean(name = [Constants.FIRECLOUD_ADMIN_CREDS])
    @Throws(IOException::class)
    fun firecloudAdminCredential(cloudStorageService: CloudStorageService): GoogleCredential {
        return cloudStorageService.fireCloudAdminCredentials
    }

    /**
     * Service account credentials for Cloud Resource Manager administration. This Service Account has
     * been enabled for domain-wide delegation of authority.
     */
    @Lazy
    @Bean(name = [Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS])
    @Throws(IOException::class)
    fun cloudResourceManagerAdminCredential(
            cloudStorageService: CloudStorageService): GoogleCredential {
        return cloudStorageService.cloudResourceManagerAdminCredentials
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(corsInterceptor!!)
        registry.addInterceptor(authInterceptor!!)
        registry.addInterceptor(tracingInterceptor!!)
        registry.addInterceptor(cronInterceptor!!)
        registry.addInterceptor(clearCdrVersionInterceptor!!)
        registry.addInterceptor(securityHeadersInterceptor!!)
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
    }

    companion object {

        internal val requestServletContext: ServletContext
            get() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes)
                    .request
                    .servletContext
    }
}
