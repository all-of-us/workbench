package org.pmiops.workbench.opsgenie;

import com.ifountain.opsgenie.client.OpsGenieClient;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class OpsGenieConfig {

  static final String OPSGENIE_API_KEY_FILENAME = "opsgenie-key.txt";

  // This bean is prototype-scoped, so a Cloud Storage API call will be made every time a new
  // instance is injected. This is okay since we expect alert API requests to be rare; the benefit
  // is that API key changes will be reflected immediately rather than requiring a restart of all
  // App Engine tasks.
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AlertApi getAlertApi(CloudStorageService cloudStorageService) {
    AlertApi client = new OpsGenieClient().alertV2();
    client
        .getApiClient()
        .setApiKey(cloudStorageService.getCredentialsBucketString(OPSGENIE_API_KEY_FILENAME));
    return client;
  }
}
