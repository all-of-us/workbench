package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMapper {

  public WorkspaceAccessLevel toApiWorkspaceAccessLevel(String firecloudAccessLevel) {
    if (firecloudAccessLevel.equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(firecloudAccessLevel);
    }
  }

  public Workspace toApiWorkspace(org.pmiops.workbench.db.model.Workspace workspace) {
    ResearchPurpose researchPurpose = createResearchPurpose(workspace);
    Workspace result = constructListWorkspaceFromDb(workspace, researchPurpose);
    return result;
  }

  public Workspace toApiWorkspace(org.pmiops.workbench.db.model.Workspace workspace, org.pmiops.workbench.firecloud.model.Workspace fcWorkspace) {
    ResearchPurpose researchPurpose = createResearchPurpose(workspace);
    if (workspace.getContainsUnderservedPopulation()) {
      researchPurpose.setUnderservedPopulationDetails(
          new ArrayList<>(workspace.getUnderservedPopulationsEnum()));
    }
    Workspace result = constructListWorkspaceFromFCAndDb(workspace, fcWorkspace,
        researchPurpose);
    return result;
  }

  public org.pmiops.workbench.db.model.Workspace toDbWorkspace(Workspace workspace) {
    org.pmiops.workbench.db.model.Workspace result = new org.pmiops.workbench.db.model.Workspace();

    if (workspace.getDataAccessLevel() != null) {
      result.setDataAccessLevelEnum(workspace.getDataAccessLevel());
    }

    result.setDescription(workspace.getDescription());
    result.setName(workspace.getName());

    if (workspace.getResearchPurpose() != null) {
      setResearchPurposeDetails(result, workspace.getResearchPurpose());
      result.setReviewRequested(workspace.getResearchPurpose().getReviewRequested());
      if (workspace.getResearchPurpose().getTimeRequested() != null) {
        result.setTimeRequested(
            new Timestamp(workspace.getResearchPurpose().getTimeRequested()));
      }
      result.setApproved(workspace.getResearchPurpose().getApproved());
    }

    return result;
  }

  public UserRole toApiUserRole(WorkspaceUserRole workspaceUserRole) {
    UserRole result = new UserRole();
    result.setEmail(workspaceUserRole.getUser().getEmail());
    result.setGivenName(workspaceUserRole.getUser().getGivenName());
    result.setFamilyName(workspaceUserRole.getUser().getFamilyName());
    result.setRole(workspaceUserRole.getRoleEnum());
    return result;
  }

  /**
   * This probably doesn't belong in a mapper service but it makes the refactoring easier atm.
   * Sets user-editable research purpose detail fields.
   */
  public static void setResearchPurposeDetails(org.pmiops.workbench.db.model.Workspace dbWorkspace,
      ResearchPurpose purpose) {
    dbWorkspace.setDiseaseFocusedResearch(purpose.getDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(purpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(purpose.getMethodsDevelopment());
    dbWorkspace.setControlSet(purpose.getControlSet());
    dbWorkspace.setAggregateAnalysis(purpose.getAggregateAnalysis());
    dbWorkspace.setAncestry(purpose.getAncestry());
    dbWorkspace.setCommercialPurpose(purpose.getCommercialPurpose());
    dbWorkspace.setPopulation(purpose.getPopulation());
    dbWorkspace.setPopulationOfFocus(purpose.getPopulationOfFocus());
    dbWorkspace.setAdditionalNotes(purpose.getAdditionalNotes());
    dbWorkspace.setContainsUnderservedPopulation(purpose.getContainsUnderservedPopulation());
    if (purpose.getContainsUnderservedPopulation()) {
      dbWorkspace
          .setUnderservedPopulationsEnum(new HashSet<>(purpose.getUnderservedPopulationDetails()));
    }
  }

  // This does not populate the list of underserved research groups.
  private final Workspace constructListWorkspaceFromDb(
      org.pmiops.workbench.db.model.Workspace workspace,
      ResearchPurpose researchPurpose) {
    FirecloudWorkspaceId workspaceId = workspace.getFirecloudWorkspaceId();
    Workspace result = new Workspace()
        .etag(Etags.fromVersion(workspace.getVersion()))
        .lastModifiedTime(workspace.getLastModifiedTime().getTime())
        .creationTime(workspace.getCreationTime().getTime())
        .dataAccessLevel(workspace.getDataAccessLevelEnum())
        .name(workspace.getName())
        .id(workspaceId.getWorkspaceName())
        .namespace(workspaceId.getWorkspaceNamespace())
        .description(workspace.getDescription())
        .researchPurpose(researchPurpose);
    if (workspace.getCreator() != null) {
      result.setCreator(workspace.getCreator().getEmail());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    result.setUserRoles(workspace.getWorkspaceUserRoles().stream().map(this::toApiUserRole)
        .collect(Collectors.toList()));
    return result;
  }

  // This does not populate the list of underserved research groups.
  private final Workspace constructListWorkspaceFromFCAndDb(
      org.pmiops.workbench.db.model.Workspace workspace,
      org.pmiops.workbench.firecloud.model.Workspace fcWorkspace, ResearchPurpose researchPurpose) {
    Workspace result = new Workspace()
        .etag(Etags.fromVersion(workspace.getVersion()))
        .lastModifiedTime(workspace.getLastModifiedTime().getTime())
        .creationTime(workspace.getCreationTime().getTime())
        .dataAccessLevel(workspace.getDataAccessLevelEnum())
        .name(workspace.getName())
        .id(fcWorkspace.getName())
        .namespace(fcWorkspace.getNamespace())
        .description(workspace.getDescription())
        .researchPurpose(researchPurpose)
        .googleBucketName(fcWorkspace.getBucketName());
    if (fcWorkspace.getCreatedBy() != null) {
      result.setCreator(fcWorkspace.getCreatedBy());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    result.setUserRoles(workspace.getWorkspaceUserRoles().stream().map(this::toApiUserRole)
        .collect(Collectors.toList()));

    return result;
  }

  private final ResearchPurpose createResearchPurpose(
      org.pmiops.workbench.db.model.Workspace workspace) {
    ResearchPurpose researchPurpose = new ResearchPurpose()
        .diseaseFocusedResearch(workspace.getDiseaseFocusedResearch())
        .diseaseOfFocus(workspace.getDiseaseOfFocus())
        .methodsDevelopment(workspace.getMethodsDevelopment())
        .controlSet(workspace.getControlSet())
        .aggregateAnalysis(workspace.getAggregateAnalysis())
        .ancestry(workspace.getAncestry())
        .commercialPurpose(workspace.getCommercialPurpose())
        .population(workspace.getPopulation())
        .populationOfFocus(workspace.getPopulationOfFocus())
        .additionalNotes(workspace.getAdditionalNotes())
        .reviewRequested(workspace.getReviewRequested())
        .approved(workspace.getApproved())
        .containsUnderservedPopulation(workspace.getContainsUnderservedPopulation());
    if (workspace.getTimeRequested() != null) {
      researchPurpose.timeRequested(workspace.getTimeRequested().getTime());
    }
    return researchPurpose;
  }

}
