package org.pmiops.workbench.workspaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.tanagra.model.Cohort;
import org.pmiops.workbench.tanagra.model.CohortList;
import org.pmiops.workbench.tanagra.model.FeatureSet;
import org.pmiops.workbench.tanagra.model.FeatureSetList;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescriptionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service(VwbWorkspaceServiceImpl.VWB_WORKSPACE_SERVICE)
public class VwbWorkspaceServiceImpl implements WorkspaceService {

  private static final Logger logger = LoggerFactory.getLogger(VwbWorkspaceServiceImpl.class);

  public static final String VWB_WORKSPACE_SERVICE = "vwbWorkspaceService";

  private final WsmClient wsmClient;

  private final WorkspaceMapper workspaceMapper;

  private final FirecloudMapper firecloudMapper;

  private final WorkspaceDao workspaceDao;

  private final InitialCreditsService expirationService;

  private final WorkspaceAuthService workspaceAuthService;

  public VwbWorkspaceServiceImpl(
      WsmClient wsmClient,
      WorkspaceMapper workspaceMapper,
      FirecloudMapper firecloudMapper,
      WorkspaceDao workspaceDao,
      InitialCreditsService expirationService,
      WorkspaceAuthService workspaceAuthService) {
    this.wsmClient = wsmClient;
    this.workspaceMapper = workspaceMapper;
    this.firecloudMapper = firecloudMapper;
    this.workspaceDao = workspaceDao;
    this.expirationService = expirationService;
    this.workspaceAuthService = workspaceAuthService;
  }

  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {

    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace);

    RawlsWorkspaceDetails fcWorkspace;
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();

    WorkspaceDescription workspaceDescription =
        wsmClient.getWorkspaceAsService(dbWorkspace.getWorkspaceNamespace());
    fcWorkspace = workspaceMapper.toWorkspaceDetails(workspaceDescription);

    workspaceResponse.setAccessLevel(
        firecloudMapper.fcToApiWorkspaceAccessLevel(
            firecloudMapper.fromIamRole(workspaceDescription.getHighestRole())));
    Workspace workspace =
        workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace, expirationService);
    workspaceResponse.setWorkspace(workspace);

    return workspaceResponse;
  }

  @Override
  public boolean notebookTransferComplete(String workspaceNamespace, String workspaceId) {
    return false;
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    return Collections.emptyList();
  }

  @Override
  public List<WorkspaceResponse> getFeaturedWorkspaces() {
    return null;
  }

  @Override
  public String getPublishedWorkspacesGroupEmail() {
    return null;
  }

  @Override
  public void deleteWorkspace(DbWorkspace dbWorkspace) {}

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {}

  @Override
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    return null;
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    return null;
  }

  @Override
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    return null;
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return null;
  }

  @Override
  public Map<String, DbWorkspace> getWorkspacesByGoogleProject(Set<String> keySet) {
    return null;
  }

  @Override
  public DbWorkspace lookupWorkspaceByNamespace(String workspaceNamespace) {
    return null;
  }

  @Override
  public void createTanagraStudy(String workspaceNamespace, String workspaceName) {}

  @Override
  public CohortList listTanagraCohorts(String workspaceNamespace, Integer offset, Integer limit) {
    return null;
  }

  @Override
  public FeatureSetList listTanagraFeatureSets(
      String workspaceNamespace, Integer offset, Integer limit) {
    return null;
  }

  @Override
  public void cloneTanagraCohort(
      Cohort cohort, String fromWorkspaceNamespace, String toWorkspaceNamespace) {}

  @Override
  public void cloneTanagraFeatureSet(
      FeatureSet featureSet, String fromWorkspaceNamespace, String toWorkspaceNamespace) {}

  @Override
  public void updateInitialCreditsExhaustion(DbUser user, boolean exhausted) {}

  @Override
  public void publishCommunityWorkspace(DbWorkspace workspace) {}

  @Override
  public List<DbUser> getWorkspaceOwnerList(DbWorkspace dbWorkspace) {
    return null;
  }

  //    @Override
  //    public RawlsWorkspaceDetails createWorkspace(Workspace workspace, DbCdrVersion cdrVersion) {
  //        String workspaceToClone = cdrVersion.getVwbTemplateID();
  //        WorkspaceDescription workspaceDescription =
  //                wsmClient.cloneWorkspaceAsService(workspaceToClone, workspace);
  //        // Need to wait until workspace has been created
  //        // before sharing it with the user
  //        String workspaceId = workspaceDescription.getId().toString();
  //        try {
  //            wsmClient.waitForWorkspaceCreation(workspaceId);
  //        } catch (InterruptedException | ApiException e) {
  //            // How do we recover here?
  //            throw new WorkbenchException(e);
  //        }
  //        wsmClient.shareWorkspaceAsService(workspaceId, workspace.getCreator(), IamRole.OWNER);
  //        return workspaceMapper.toWorkspaceDetails(workspaceDescription);
  //    }
  //
  //    @Override
  //    public RawlsWorkspaceDetails cloneWorkspace(
  //            String fromWorkspaceNamespace,
  //            String fromWorkspaceId,
  //            Workspace toWorkspace,
  //            DbAccessTier accessTier) {
  //        return null;
  //    }

  private boolean filterToNonPublished(WorkspaceResponse response) {
    return response.getAccessLevel() == WorkspaceAccessLevel.OWNER
        || response.getAccessLevel() == WorkspaceAccessLevel.WRITER
        || response.getWorkspace().getFeaturedCategory() == null;
  }

  private List<RawlsWorkspaceListResponse> workspaceResponseListFromWorkspaceDescriptionList(
      WorkspaceDescriptionList workspaceDescriptionList) {
    List<RawlsWorkspaceListResponse> responseList = new ArrayList<>();
    workspaceDescriptionList
        .getWorkspaces()
        .forEach(
            w -> {
              responseList.add(
                  new RawlsWorkspaceListResponse()
                      .accessLevel(firecloudMapper.fromIamRole(w.getHighestRole()))
                      .workspace(workspaceMapper.toWorkspaceDetails(w)));
            });
    return responseList;
  }
}
