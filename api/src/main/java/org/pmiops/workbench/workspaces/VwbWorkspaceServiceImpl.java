package org.pmiops.workbench.workspaces;

import java.util.*;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
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
  public List<WorkspaceResponse> listWorkspaces() {
    return Collections.emptyList();
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    logger.warn("updateRecentWorkspaces not implemented in VWB");
    return null;
  }

  @Override
  public List<DbWorkspace> lookupWorkspacesByNamespace(Collection<String> workspaceNamespaces) {
    logger.warn("lookupWorkspacesByNamespace not implemented in VWB");
    return null;
  }
}
