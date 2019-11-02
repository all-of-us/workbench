package org.pmiops.workbench.moodle

import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.moodle.api.MoodleApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope

@Configuration
class MoodleConfig {

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun moodleApi(workbenchConfig: WorkbenchConfig): MoodleApi {
        val api = MoodleApi()
        val apiClient = MoodleApiClientTracer()
        apiClient.setBasePath("https://" + workbenchConfig.moodle.host + "/webservice/rest")
        api.setApiClient(apiClient)
        return api
    }
}
