package org.pmiops.workbench.app;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.pmiops.workbench.model.CloudPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AppsServiceFactory {

  private final Map<CloudPlatform, AppsService> appsServices;
  private final AppsService defaultAppsService;

  @Autowired
  public AppsServiceFactory(
      AppsService gcpAppsService, @Qualifier("awsAppsService") AppsService awsAppsService) {
    defaultAppsService = gcpAppsService;
    appsServices =
        ImmutableMap.of(
            CloudPlatform.GCP, gcpAppsService,
            CloudPlatform.AWS, awsAppsService);
  }

  public AppsService getAppsService(CloudPlatform cloudPlatform) {
    return appsServices.getOrDefault(cloudPlatform, defaultAppsService);
  }
}
