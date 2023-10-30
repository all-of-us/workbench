package org.pmiops.workbench.app;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("multicloudAppsService")
public class MulticloudAppsService implements AppsService {

  private final AppsServiceFactory appsServiceFactory;

  @Autowired
  public MulticloudAppsService(AppsServiceFactory appsServiceFactory) {
    this.appsServiceFactory = appsServiceFactory;
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace) {
    getAppsService(dbWorkspace).createApp(createAppRequest, dbWorkspace);
  }

  @Override
  public void deleteApp(String appName, DbWorkspace dbWorkspace, Boolean deleteDisk) {
    getAppsService(dbWorkspace).deleteApp(appName, dbWorkspace, deleteDisk);
  }

  @Override
  public UserAppEnvironment getApp(String appName, DbWorkspace dbWorkspace) {
    return getAppsService(dbWorkspace).getApp(appName, dbWorkspace);
  }

  @Override
  public void updateApp(String appName, UserAppEnvironment app, DbWorkspace dbWorkspace) {
    getAppsService(dbWorkspace).updateApp(appName, app, dbWorkspace);
  }

  @Override
  public List<UserAppEnvironment> listAppsInWorkspace(DbWorkspace dbWorkspace) {
    return getAppsService(dbWorkspace).listAppsInWorkspace(dbWorkspace);
  }

  private AppsService getAppsService(DbWorkspace dbWorkspace) {
    return appsServiceFactory.getAppsService(dbWorkspace.getCloudPlatform());
  }
}
