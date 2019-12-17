package org.pmiops.workbench.moodle;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class MoodleConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public MoodleApi moodleApi(WorkbenchConfig workbenchConfig) {
    MoodleApi api = new MoodleApi();
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("https://" + workbenchConfig.moodle.host + "/webservice/rest");
    api.setApiClient(apiClient);
    return api;
  }
}
