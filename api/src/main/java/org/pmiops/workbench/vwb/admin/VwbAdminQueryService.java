package org.pmiops.workbench.vwb.admin;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.model.PreprodWorkspace;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.VwbDataCollectionEntry;
import org.pmiops.workbench.model.VwbWorkspace;
import org.pmiops.workbench.model.VwbWorkspaceAuditLog;

public interface VwbAdminQueryService {

  /**
   * Query VWB BQ for all workspaces a user has created
   *
   * @return A List of workspaces created by the user
   */
  List<VwbWorkspace> queryVwbWorkspacesByCreator(String email);

  List<VwbWorkspace> queryVwbWorkspacesByUserFacingId(String id);

  List<VwbWorkspace> queryVwbWorkspacesByWorkspaceId(String workspaceId);

  List<VwbWorkspace> queryVwbWorkspacesByName(String name);

  List<VwbWorkspace> queryVwbWorkspacesByShareActivity(String email);

  List<VwbWorkspace> queryVwbWorkspaceByGcpProjectId(String id);

  List<UserRole> queryVwbWorkspaceCollaboratorsByUserFacingId(String id);

  List<VwbWorkspaceAuditLog> queryVwbWorkspaceActivity(String workspaceId);

  Set<String> queryPodIdsByUserEmail(String email);

  List<VwbDataCollectionEntry> queryVwbDataCollections();

  List<PreprodWorkspace> queryPreprodWorkspaceByNamespace(String workspaceNamespace);
}
