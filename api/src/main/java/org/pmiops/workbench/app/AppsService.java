package org.pmiops.workbench.app;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.UserAppEnvironment;

public interface AppsService {

  void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace);

  void deleteApp(String appName, DbWorkspace dbWorkspace, Boolean deleteDisk);

  UserAppEnvironment getApp(String appName, DbWorkspace dbWorkspace);

  void updateApp(String appName, UserAppEnvironment app, DbWorkspace dbWorkspace);

  List<UserAppEnvironment> listAppsInWorkspace(DbWorkspace dbWorkspace);
}
