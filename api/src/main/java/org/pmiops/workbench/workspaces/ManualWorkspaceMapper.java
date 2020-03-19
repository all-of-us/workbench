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

  public Workspace toApiWorkspace(DbWorkspace workspace) {
    ResearchPurpose researchPurpose = workspaceMapper.workspaceToResearchPurpose(workspace);
    FirecloudWorkspaceId workspaceId = workspace.getFirecloudWorkspaceId();

    Workspace result =
        new Workspace()
            .etag(Etags.fromVersion(workspace.getVersion()))
            .lastModifiedTime(workspace.getLastModifiedTime().getTime())
            .creationTime(workspace.getCreationTime().getTime())
            .dataAccessLevel(workspace.getDataAccessLevelEnum())
            .billingStatus(workspace.getBillingStatus())
            .name(workspace.getName())
            .id(workspaceId.getWorkspaceName())
            .namespace(workspaceId.getWorkspaceNamespace())
            .published(workspace.getPublished())
            .researchPurpose(researchPurpose);

    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout) {
      result.billingStatus(BillingStatus.ACTIVE);
    }

    if (workspace.getCreator() != null) {
      result.setCreator(workspace.getCreator().getUsername());
    }

    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    return result;
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

  /**
   * This probably doesn't belong in a mapper service but it makes the refactoring easier atm. Sets
   * user-editable research purpose detail fields.
   */
  public void setResearchPurposeDetails(DbWorkspace dbWorkspace, ResearchPurpose purpose) {
    dbWorkspace.setDiseaseFocusedResearch(purpose.getDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(purpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(purpose.getMethodsDevelopment());
    dbWorkspace.setControlSet(purpose.getControlSet());
    dbWorkspace.setAncestry(purpose.getAncestry());
    dbWorkspace.setCommercialPurpose(purpose.getCommercialPurpose());
    dbWorkspace.setSocialBehavioral(purpose.getSocialBehavioral());
    dbWorkspace.setPopulationHealth(purpose.getPopulationHealth());
    dbWorkspace.setEthics(purpose.getEthics());
    dbWorkspace.setEducational(purpose.getEducational());
    dbWorkspace.setDrugDevelopment(purpose.getDrugDevelopment());
    dbWorkspace.setOtherPurpose(purpose.getOtherPurpose());
    dbWorkspace.setOtherPurposeDetails(purpose.getOtherPurposeDetails());
    dbWorkspace.setAdditionalNotes(purpose.getAdditionalNotes());
    dbWorkspace.setReasonForAllOfUs(purpose.getReasonForAllOfUs());
    dbWorkspace.setIntendedStudy(purpose.getIntendedStudy());
    dbWorkspace.setScientificApproach(purpose.getScientificApproach());
    dbWorkspace.setAnticipatedFindings(purpose.getAnticipatedFindings());
    dbWorkspace.setOtherPopulationDetails(purpose.getOtherPopulationDetails());
    dbWorkspace.setDisseminateResearchEnumSet(
        Optional.ofNullable(purpose.getDisseminateResearchFindingList())
            .map(disseminateResearch -> disseminateResearch.stream().collect(Collectors.toSet()))
            .orElse(new HashSet<DisseminateResearchEnum>()));

    // TODO eric: also missing
    if (dbWorkspace.getDisseminateResearchEnumSet().contains(DisseminateResearchEnum.OTHER)) {
      dbWorkspace.setDisseminateResearchOther(purpose.getOtherDisseminateResearchFindings());
    }

    // TODO eric: also missing
    dbWorkspace.setResearchOutcomeEnumSet(
        Optional.ofNullable(purpose.getResearchOutcomeList())
            .map(researchOutcoming -> researchOutcoming.stream().collect(Collectors.toSet()))
            .orElse(new HashSet<ResearchOutcomeEnum>()));
  }

  public RecentWorkspace buildRecentWorkspace(
      DbUserRecentWorkspace userRecentWorkspace,
      DbWorkspace dbWorkspace,
      WorkspaceAccessLevel accessLevel) {
    return new RecentWorkspace()
        .workspace(toApiWorkspace(dbWorkspace))
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
