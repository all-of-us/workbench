package org.pmiops.workbench.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.VwbAodRequest;
import org.pmiops.workbench.model.VwbWorkspaceAdminView;
import org.pmiops.workbench.model.VwbWorkspaceAuditLog;
import org.pmiops.workbench.model.VwbWorkspaceListResponse;
import org.pmiops.workbench.model.VwbWorkspaceSearchParamType;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbWorkspaceAdminController implements VwbWorkspaceAdminApiDelegate {
  private static final Logger log = Logger.getLogger(VwbWorkspaceAdminController.class.getName());

  private final VwbAdminQueryService vwbAdminQueryService;
  private final VwbUserManagerClient vwbUserManagerClient;
  private final WsmClient wsmClient;

  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  @Autowired
  public VwbWorkspaceAdminController(
      VwbAdminQueryService vwbAdminQueryService,
      VwbUserManagerClient vwbUserManagerClient,
      WsmClient wsmClient) {
    this.vwbAdminQueryService = vwbAdminQueryService;
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.wsmClient = wsmClient;
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<VwbWorkspaceListResponse> getVwbWorkspacesBySearchParam(
      String paramType, String searchParam) {
    VwbWorkspaceSearchParamType searchParamType = validateSearchParamType(paramType);
    switch (searchParamType) {
      case CREATOR:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByCreator(searchParam)));
      case USER_FACING_ID:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByUserFacingId(searchParam)));
      case ID:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByWorkspaceId(searchParam)));
      case NAME:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByName(searchParam)));
      case SHARED:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByShareActivity(searchParam)));
      case GCP_PROJECT_ID:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspaceByGcpProjectId(searchParam)));
      default:
        throw new BadRequestException("Search Param Type " + paramType + " is not supported");
    }
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<VwbWorkspaceAdminView> getVwbWorkspaceAdminView(String userFacingId) {
    return ResponseEntity.ok(
        new VwbWorkspaceAdminView()
            .workspace(
                vwbAdminQueryService.queryVwbWorkspacesByUserFacingId(userFacingId).stream()
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new NotFoundException(
                                String.format(
                                    "Workspace User Facing Id %s was not found", userFacingId))))
            .collaborators(
                vwbAdminQueryService.queryVwbWorkspaceCollaboratorsByUserFacingId(userFacingId)));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<List<VwbWorkspaceAuditLog>> getVwbWorkspaceAuditLogs(String workspaceId) {
    return ResponseEntity.ok(vwbAdminQueryService.queryVwbWorkspaceActivity(workspaceId));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<Void> enableAccessOnDemandByUserFacingId(
      String userFacingId, VwbAodRequest request) {
    vwbUserManagerClient.workspaceAccessOnDemandByUserFacingId(userFacingId, request.getReason());
    return ResponseEntity.ok().build();
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<org.pmiops.workbench.model.InlineResponse200> getVwbWorkspaceResources(
      String workspaceId) {
    Object result = wsmClient.enumerateWorkspaceResources(workspaceId);

    // Extract resources from the result
    List<Object> resources = new ArrayList<>();
    if (result instanceof org.pmiops.workbench.wsmanager.model.ResourceList) {
      org.pmiops.workbench.wsmanager.model.ResourceList resourceList =
          (org.pmiops.workbench.wsmanager.model.ResourceList) result;
      if (resourceList.getResources() != null) {
        resources = new ArrayList<>(resourceList.getResources());
      }
    }

    return ResponseEntity.ok(
        new org.pmiops.workbench.model.InlineResponse200().resources(resources));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<Void> deleteVwbWorkspaceResource(
      String workspaceId, String resourceId, String resourceType) {
    wsmClient.deleteWorkspaceResource(workspaceId, resourceId, resourceType);
    return ResponseEntity.ok().build();
  }

  protected VwbWorkspaceSearchParamType validateSearchParamType(String param) {
    return Optional.ofNullable(param)
        .map(VwbWorkspaceSearchParamType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(BAD_REQUEST_MESSAGE, "search parameter type", param)));
  }
}
