package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.springframework.stereotype.Service;

// We should migrate over to the Mapstruct supported WorkspaceMapper
@Service
public class ManualWorkspaceMapper {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceMapper workspaceMapper;

  public ManualWorkspaceMapper(
      Provider<WorkbenchConfig> workbenchConfigProvider, WorkspaceMapper workspaceMapper) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceMapper = workspaceMapper;
  }

  public static WorkspaceAccessLevel toApiWorkspaceAccessLevel(String firecloudAccessLevel) {
    if (firecloudAccessLevel.equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(firecloudAccessLevel);
    }
  }

  public RecentWorkspace buildRecentWorkspace(
      DbUserRecentWorkspace userRecentWorkspace,
      DbWorkspace dbWorkspace,
      WorkspaceAccessLevel accessLevel) {
    System.out.println(
        workspaceMapper.toApiRecentWorkspace(userRecentWorkspace, dbWorkspace, accessLevel).equals(
            new RecentWorkspace()
                .workspace(workspaceMapper.toApiWorkspace(dbWorkspace))
                .accessedTime(userRecentWorkspace.getLastAccessDate().toString())
                .accessLevel(accessLevel)
        ));

    return new RecentWorkspace()
        .workspace(workspaceMapper.toApiWorkspace(dbWorkspace))
        .accessedTime(userRecentWorkspace.getLastAccessDate().toString())
        .accessLevel(accessLevel);
  }

}
