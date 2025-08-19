package org.pmiops.workbench.vwb.admin;

import java.util.List;
import org.pmiops.workbench.model.VwbWorkspace;

public interface VwbAdminQueryService {

  /**
   * Query VWB BQ for all workspaces a user has created
   *
   * @return A List of workspaces created by the user
   */
  List<VwbWorkspace> queryVwbWorkspacesByCreator(String email);

  List<VwbWorkspace> queryVwbWorkspacesById(String id);

  List<VwbWorkspace> queryVwbWorkspacesByName(String name);

  List<VwbWorkspace> queryVwbWorkspacesByShareActivity(String email);
}
