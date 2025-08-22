package org.pmiops.workbench.api;

import java.util.Optional;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.VwbWorkspaceAdminView;
import org.pmiops.workbench.model.VwbWorkspaceListResponse;
import org.pmiops.workbench.model.VwbWorkspaceSearchParamType;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbWorkspaceAdminController implements VwbWorkspaceAdminApiDelegate {
  private final VwbAdminQueryService vwbAdminQueryService;

  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  @Autowired
  public VwbWorkspaceAdminController(VwbAdminQueryService vwbAdminQueryService) {
    this.vwbAdminQueryService = vwbAdminQueryService;
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
      case NAME:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByName(searchParam)));
      case SHARED:
        return ResponseEntity.ok(
            new VwbWorkspaceListResponse()
                .items(vwbAdminQueryService.queryVwbWorkspacesByShareActivity(searchParam)));
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

  protected VwbWorkspaceSearchParamType validateSearchParamType(String param) {
    return Optional.ofNullable(param)
        .map(VwbWorkspaceSearchParamType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(BAD_REQUEST_MESSAGE, "search parameter type", param)));
  }
}
