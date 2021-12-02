package org.pmiops.workbench.api;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
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
  public ResponseEntity<WorkspaceAuditLogQueryResponse> getAuditLogEntries(
      String workspaceNamespace,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillisNullable) {
    return ResponseEntity.ok(
        workspaceAdminService.getAuditLogEntries(
            workspaceNamespace, limit, afterMillis, beforeMillisNullable));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<ReadOnlyNotebookResponse> adminReadOnlyNotebook(
      String workspaceNamespace, String notebookName, AccessReason accessReason) {
    final String notebookHtml =
        workspaceAdminService.getReadOnlyNotebook(workspaceNamespace, notebookName, accessReason);
    return ResponseEntity.ok(new ReadOnlyNotebookResponse().html(notebookHtml));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<List<FileDetail>> listFiles(String workspaceNamespace) {
    return ResponseEntity.ok(workspaceAdminService.listFiles(workspaceNamespace));
  }

  @Override
  @AuthorityRequired(Authority.SECURITY_ADMIN)
  public ResponseEntity<List<ListRuntimeResponse>> deleteRuntimesInWorkspace(
      String workspaceNamespace, ListRuntimeDeleteRequest runtimesToDelete) {
    return ResponseEntity.ok(
        workspaceAdminService.deleteRuntimesInWorkspace(workspaceNamespace, runtimesToDelete));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> setAdminLockedState(
      String workspaceNamespace, AdminLockingRequest lockingRequest) {
    if (lockingRequest.getRequestDateInMillis() == null
        || lockingRequest.getRequestDateInMillis() == 0
        || StringUtils.isBlank(lockingRequest.getRequestReason())) {
      throw new BadRequestException(
          String.format("Cannot have empty Request reason or Request Date"));
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
}
