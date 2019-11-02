package org.pmiops.workbench.mandrill

import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.mandrill.api.MandrillApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope

@Configuration
class MandrillConfig {

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun mandrillApiClient(workbenchConfig: WorkbenchConfig): ApiClient {
        return MandrillApiClientTracer()
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun mandrillApi(apiClient: ApiClient): MandrillApi {
        val api = MandrillApi()
        api.setApiClient(apiClient)
        return api
    }
}
