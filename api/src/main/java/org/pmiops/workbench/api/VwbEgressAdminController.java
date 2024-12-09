package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exfiltration.EgressEventService;
import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbEgressAdminController implements VwbEgressAdminApiDelegate {
  private static final Logger log = Logger.getLogger(NotebooksController.class.getName());
  private final EgressEventService egressEventService;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final Provider<DbUser> userProvider;

  @Autowired
  public VwbEgressAdminController(
      EgressEventService egressEventService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider) {
    this.egressEventService = egressEventService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<Void> createVwbEgressEvent(VwbEgressEventRequest body) {
    if (!workbenchConfigProvider
        .get()
        .vwb
        .exfilManagerServiceAccount
        .equals(userProvider.get().getUsername())) {
      throw new ForbiddenException(
          String.format(
              "User %s is not authorized to create VwbEgressEvent",
              userProvider.get().getUsername()));
    }
    egressEventService.handleVwbEvent(body);
    return ResponseEntity.ok().build();
  }
}
