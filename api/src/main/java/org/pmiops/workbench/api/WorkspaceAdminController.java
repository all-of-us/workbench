package org.pmiops.workbench.api;

import jakarta.annotation.Nullable;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminRuntimeFields;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.PublishWorkspaceRequest;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceAdminController implements WorkspaceAdminApiDelegate {

  private final WorkspaceAdminService workspaceAdminService;

  @Autowired
  public WorkspaceAdminController(WorkspaceAdminService workspaceAdminService) {
    this.workspaceAdminService = workspaceAdminService;
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<CloudStorageTraffic> getCloudStorageTraffic(String workspaceNamespace) {
    return ResponseEntity.ok(workspaceAdminService.getCloudStorageTraffic(workspaceNamespace));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<WorkspaceAdminView> getWorkspaceAdminView(String workspaceNamespace) {
    return ResponseEntity.ok(workspaceAdminService.getWorkspaceAdminView(workspaceNamespace));
  }

  // use adminListRuntimesInWorkspaceV2
  @Deprecated(since = "October 2024", forRemoval = true)
  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<List<org.pmiops.workbench.model.ListRuntimeResponse>>
      adminListRuntimesInWorkspace(String workspaceNamespace) {
    return ResponseEntity.ok(workspaceAdminService.deprecatedListRuntimes(workspaceNamespace));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<List<AdminRuntimeFields>> adminListRuntimesInWorkspaceV2(
      String workspaceNamespace) {
    return ResponseEntity.ok(workspaceAdminService.listRuntimes(workspaceNamespace));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<List<UserAppEnvironment>> adminListUserAppsInWorkspace(
      String workspaceNamespace) {
    return ResponseEntity.ok(workspaceAdminService.listUserApps(workspaceNamespace));
  }

  /**
   * Get all audit log entries for this workspace
   *
   * @param workspaceNamespace Firecloud namespace for workspace
   * @param limit upper limit (inclusive) for this query
   * @param afterMillis lowest timestamp matched (inclusive)
   * @param beforeMillisNullable latest timestamp matched (exclusive). The half-open interval is
   *     convenient for pagination based on time intervals.
   */
  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<WorkspaceAuditLogQueryResponse> getWorkspaceAuditLogEntries(
      String workspaceNamespace,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillisNullable) {
    return ResponseEntity.ok(
        workspaceAdminService.getWorkspaceAuditLogEntries(
            workspaceNamespace, limit, afterMillis, beforeMillisNullable));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<ReadOnlyNotebookResponse> adminReadOnlyNotebook(
      String workspaceNamespace, String notebookNameWithFileExtension, AccessReason accessReason) {
    final String notebookHtml =
        workspaceAdminService.getReadOnlyNotebook(
            workspaceNamespace, notebookNameWithFileExtension, accessReason);
    return ResponseEntity.ok(new ReadOnlyNotebookResponse().html(notebookHtml));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<List<FileDetail>> listFiles(
      String workspaceNamespace, Boolean onlyAppFiles) {
    return ResponseEntity.ok(
        workspaceAdminService.listFiles(workspaceNamespace, Boolean.TRUE.equals(onlyAppFiles)));
  }

  // use adminDeleteRuntimesInWorkspace
  @Deprecated(since = "October 2024", forRemoval = true)
  @Override
  @AuthorityRequired(Authority.SECURITY_ADMIN)
  public ResponseEntity<List<org.pmiops.workbench.model.ListRuntimeResponse>>
      deleteRuntimesInWorkspace(
          String workspaceNamespace, ListRuntimeDeleteRequest runtimesToDelete) {
    return ResponseEntity.ok(
        workspaceAdminService.deprecatedDeleteRuntimes(workspaceNamespace, runtimesToDelete));
  }

  @Override
  @AuthorityRequired(Authority.SECURITY_ADMIN)
  public ResponseEntity<List<AdminRuntimeFields>> adminDeleteRuntimesInWorkspace(
      String workspaceNamespace, ListRuntimeDeleteRequest runtimesToDelete) {
    return ResponseEntity.ok(
        workspaceAdminService.deleteRuntimesInWorkspace(workspaceNamespace, runtimesToDelete));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> setAdminLockedState(
      String workspaceNamespace, AdminLockingRequest lockingRequest) {
    String lockingReason = lockingRequest.getRequestReason();
    if (lockingRequest.getRequestDateInMillis() == null
        || lockingRequest.getRequestDateInMillis() == 0
        || StringUtils.isBlank(lockingReason)) {
      throw new BadRequestException(
          String.format("Cannot have empty Request reason or Request Date"));
    }
    if (lockingReason.length() < 10 || lockingReason.length() > 4000) {
      throw new BadRequestException(
          "Locking Reason text length should be "
              + "at least 10 characters long and at most 4000 characters");
    }
    workspaceAdminService.setAdminLockedState(workspaceNamespace, lockingRequest);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> setAdminUnlockedState(String workspaceNamespace) {
    workspaceAdminService.setAdminUnlockedState(workspaceNamespace);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<EmptyResponse> publishWorkspaceViaDB(
      String workspaceNamespace, PublishWorkspaceRequest body) {
    workspaceAdminService.publishWorkspaceViaDB(workspaceNamespace, body);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<EmptyResponse> unpublishWorkspaceViaDB(String workspaceNamespace) {
    workspaceAdminService.unpublishWorkspaceViaDB(workspaceNamespace);
    return ResponseEntity.ok(new EmptyResponse());
  }
}
