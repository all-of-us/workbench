package org.pmiops.workbench.api;

import org.pmiops.workbench.model.App;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListAppsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController implements AppApiDelegate {

  @Override
  public ResponseEntity<EmptyResponse> createApp(String workspaceNamespace, App app) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteApp(String workspaceNamespace, Boolean deleteDisk) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<App> getApp(String workspaceNamespace) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<EmptyResponse> updateApp(String workspaceNamespace, App app) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<ListAppsResponse> listApp() {
    throw new UnsupportedOperationException("API not supported.");
  }
}
