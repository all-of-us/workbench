package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.disks.DiskAdminService;
import org.pmiops.workbench.environments.EnvironmentsAdminService;
import org.pmiops.workbench.model.Disk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskEnvironmentsController implements CloudTaskEnvironmentsApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskEnvironmentsController.class.getName());

  private final DiskAdminService diskAdminService;
  private final EnvironmentsAdminService environmentsAdminService;
  private final Provider<Stopwatch> stopwatchProvider;

  @Autowired
  public CloudTaskEnvironmentsController(
      DiskAdminService diskAdminService,
      EnvironmentsAdminService environmentsAdminService,
      Provider<Stopwatch> stopwatchProvider) {
    this.diskAdminService = diskAdminService;
    this.environmentsAdminService = environmentsAdminService;
    this.stopwatchProvider = stopwatchProvider;
  }

  @Override
  public ResponseEntity<Void> deleteUnsharedWorkspaceEnvironmentsBatch(
      List<String> workspaceNamespaces) {
    LOGGER.info(
        String.format(
            "Deleting unshared environments for %d workspaces.", workspaceNamespaces.size()));

    var stopwatch = stopwatchProvider.get().start();
    var failures = environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(workspaceNamespaces);
    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Attempted to delete unshared environments for %d workspaces (%d failures) in %s.",
            workspaceNamespaces.size(), failures, formatDurationPretty(elapsed)));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> checkPersistentDisksBatch(List<Disk> persistentDiskBatch) {
    LOGGER.info(String.format("Checking %s persistent disks.", persistentDiskBatch.size()));

    var stopwatch = stopwatchProvider.get().start();
    diskAdminService.checkPersistentDisks(persistentDiskBatch);
    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Completed checking %s persistent disks in %s.",
            persistentDiskBatch.size(), formatDurationPretty(elapsed)));

    return ResponseEntity.ok().build();
  }
}
