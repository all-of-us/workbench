package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.FirecloudWorkspaceId;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
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

  public Workspace toApiWorkspace(DbWorkspace workspace, FirecloudWorkspace fcWorkspace) {
    ResearchPurpose researchPurpose = workspaceMapper.workspaceToResearchPurpose(workspace);

    Workspace result =
        new Workspace()
            .etag(Etags.fromVersion(workspace.getVersion()))
            .lastModifiedTime(workspace.getLastModifiedTime().getTime())
            .creationTime(workspace.getCreationTime().getTime())
            .dataAccessLevel(workspace.getDataAccessLevelEnum())
            .name(workspace.getName())
            .id(fcWorkspace.getName())
            .namespace(fcWorkspace.getNamespace())
            .researchPurpose(researchPurpose)
            .published(workspace.getPublished())
            .googleBucketName(fcWorkspace.getBucketName())
            .billingStatus(workspace.getBillingStatus())
            .billingAccountName(workspace.getBillingAccountName())
            .billingAccountType(workspace.getBillingAccountType());

    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout) {
      result.billingStatus(BillingStatus.ACTIVE);
    }

    if (fcWorkspace.getCreatedBy() != null) {
      result.setCreator(fcWorkspace.getCreatedBy());
    }

    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    return result;
  }

  public DbWorkspace toDbWorkspace(Workspace workspace) {
    DbWorkspace result = new DbWorkspace();

    if (workspace.getDataAccessLevel() != null) {
      result.setDataAccessLevelEnum(workspace.getDataAccessLevel());
    }

    result.setName(workspace.getName());

    if (workspace.getResearchPurpose() != null) {
      workspaceMapper.mergeResearchPurposeIntoWorkspace(result, workspace.getResearchPurpose());
      result.setReviewRequested(workspace.getResearchPurpose().getReviewRequested());
      if (workspace.getResearchPurpose().getTimeRequested() != null) {
        result.setTimeRequested(new Timestamp(workspace.getResearchPurpose().getTimeRequested()));
      }
      result.setApproved(workspace.getResearchPurpose().getApproved());
    }

    return result;
  }

  public UserRole toApiUserRole(DbUser user, FirecloudWorkspaceAccessEntry aclEntry) {
    UserRole result = new UserRole();
    result.setEmail(user.getUsername());
    result.setGivenName(user.getGivenName());
    result.setFamilyName(user.getFamilyName());
    result.setRole(WorkspaceAccessLevel.fromValue(aclEntry.getAccessLevel()));
    return result;
  }

  public RecentWorkspace buildRecentWorkspace(
      DbUserRecentWorkspace userRecentWorkspace,
      DbWorkspace dbWorkspace,
      WorkspaceAccessLevel accessLevel) {
    return new RecentWorkspace()
        .workspace(workspaceMapper.toApiWorkspace(dbWorkspace))
        .accessedTime(userRecentWorkspace.getLastAccessDate().toString())
        .accessLevel(accessLevel);
  }

  public List<RecentWorkspace> buildRecentWorkspaceList(
      List<DbUserRecentWorkspace> userRecentWorkspaces,
      Map<Long, DbWorkspace> dbWorkspacesByWorkspaceId,
      Map<Long, WorkspaceAccessLevel> workspaceAccessLevelsByWorkspaceId) {
    return userRecentWorkspaces.stream()
        .map(
            userRecentWorkspace ->
                buildRecentWorkspace(
                    userRecentWorkspace,
                    dbWorkspacesByWorkspaceId.get(userRecentWorkspace.getWorkspaceId()),
                    workspaceAccessLevelsByWorkspaceId.get(userRecentWorkspace.getWorkspaceId())))
        .collect(Collectors.toList());
  }
}
