package org.pmiops.workbench.app;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AppsServiceFactory {

  private final AppsService gcpAppsService;
  private final AppsService awsAppsService;

  public AppsServiceFactory(
      AppsService gcpAppsService, @Qualifier("awsAppsService") AppsService awsAppsService) {
    this.gcpAppsService = gcpAppsService;
    this.awsAppsService = awsAppsService;
  }

  public AppsService getAppsService(DbWorkspace dbWorkspace) {
    if (dbWorkspace.isAws()) return awsAppsService;
    else return gcpAppsService;
  }
}
