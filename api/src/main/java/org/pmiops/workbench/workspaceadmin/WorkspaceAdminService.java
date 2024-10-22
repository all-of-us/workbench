package org.pmiops.workbench.workspaceadmin;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminRuntimeFields;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.PublishWorkspaceRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;

public interface WorkspaceAdminService {
  Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace);

  AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId);

  AdminWorkspaceCloudStorageCounts getAdminWorkspaceCloudStorageCounts(
      String workspaceNamespace, String workspaceTerraName);

  CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace);

  WorkspaceAdminView getWorkspaceAdminView(String workspaceNamespace);

  List<AdminRuntimeFields> listRuntimes(String workspaceNamespace);

  // use listRuntimes
  @Deprecated(since = "October 2024", forRemoval = true)
  List<org.pmiops.workbench.model.ListRuntimeResponse> deprecatedListRuntimes(
      String workspaceNamespace);

  List<UserAppEnvironment> listUserApps(String workspaceNamespace);

  WorkspaceAuditLogQueryResponse getWorkspaceAuditLogEntries(
      String workspaceNamespace,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillisNullable);

  String getReadOnlyNotebook(
      String workspaceNamespace, String notebookName, AccessReason accessReason);

  List<FileDetail> listFiles(String workspaceNamespace, boolean onlyAppFiles);

  List<AdminRuntimeFields> deleteRuntimes(String workspaceNamespace, ListRuntimeDeleteRequest req);

  // use deleteRuntimes
  @Deprecated(since = "October 2024", forRemoval = true)
  List<org.pmiops.workbench.model.ListRuntimeResponse> deprecatedDeleteRuntimes(
      String workspaceNamespace, ListRuntimeDeleteRequest req);

  void setAdminLockedState(String workspaceNamespace, AdminLockingRequest adminLockingRequest);

  void setAdminUnlockedState(String workspaceNamespace);

  void publishWorkspaceViaDB(
      String workspaceNamespace, PublishWorkspaceRequest publishWorkspaceRequest);

  void unpublishWorkspaceViaDB(String workspaceNamespace);
}
