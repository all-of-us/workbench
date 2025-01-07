package org.pmiops.workbench.leonardo;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.UserAppEnvironment;

public class LeonardoStatusUtils {

  // Keep in sync with Leonardo's runtimeModels.scala

  private static final Set<LeonardoRuntimeStatus> STOPPABLE_LEO_RUNTIME_STATUSES =
      ImmutableSet.of(
          LeonardoRuntimeStatus.UNKNOWN,
          LeonardoRuntimeStatus.RUNNING,
          LeonardoRuntimeStatus.STARTING,
          LeonardoRuntimeStatus.UPDATING);

  private static final Set<LeonardoRuntimeStatus> DELETABLE_LEO_RUNTIME_STATUSES =
      ImmutableSet.of(
          LeonardoRuntimeStatus.UNKNOWN,
          LeonardoRuntimeStatus.RUNNING,
          LeonardoRuntimeStatus.STARTING,
          LeonardoRuntimeStatus.UPDATING,
          LeonardoRuntimeStatus.ERROR,
          LeonardoRuntimeStatus.STOPPING,
          LeonardoRuntimeStatus.STOPPED);

  // Keep in sync with Leonardo's kubernetesModels.scala

  private static final Set<AppStatus> DELETABLE_LEO_APP_STATUSES =
      Set.of(AppStatus.STATUS_UNSPECIFIED, AppStatus.RUNNING, AppStatus.ERROR);

  private static final Set<org.pmiops.workbench.model.AppStatus> DELETABLE_RWB_APP_STATUSES =
      Set.of(
          org.pmiops.workbench.model.AppStatus.STATUS_UNSPECIFIED,
          org.pmiops.workbench.model.AppStatus.RUNNING,
          org.pmiops.workbench.model.AppStatus.ERROR);

  // Keep in sync with Leonardo's diskmodels.scala

  private static final Set<DiskStatus> DELETABLE_LEO_DISK_STATUSES =
      Set.of(DiskStatus.FAILED, DiskStatus.READY);

  private static final Set<org.pmiops.workbench.model.DiskStatus> ACTIVE_RWB_DISK_STATUSES =
      ImmutableSet.of(
          org.pmiops.workbench.model.DiskStatus.READY,
          org.pmiops.workbench.model.DiskStatus.CREATING,
          org.pmiops.workbench.model.DiskStatus.RESTORING);

  public static boolean canStopRuntime(LeonardoListRuntimeResponse response) {
    return STOPPABLE_LEO_RUNTIME_STATUSES.contains(response.getStatus());
  }

  public static boolean canDeleteRuntime(LeonardoListRuntimeResponse response) {
    return DELETABLE_LEO_RUNTIME_STATUSES.contains(response.getStatus());
  }

  public static boolean canDeleteApp(ListAppResponse response) {
    return DELETABLE_LEO_APP_STATUSES.contains(response.getStatus());
  }

  public static boolean canDeleteApp(UserAppEnvironment app) {
    return DELETABLE_RWB_APP_STATUSES.contains(app.getStatus());
  }

  public static boolean isActiveDisk(Disk disk) {
    return ACTIVE_RWB_DISK_STATUSES.contains(disk.getStatus());
  }

  public static boolean canDeleteDisk(ListPersistentDiskResponse disk) {
    return DELETABLE_LEO_DISK_STATUSES.contains(disk.getStatus());
  }
}
