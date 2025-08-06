package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.VwbWorkspaceListResponse;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbWorkspaceAdminController implements VwbWorkspaceAdminApiDelegate {
  private final VwbAdminQueryService vwbAdminQueryService;

  @Autowired
  public VwbWorkspaceAdminController(VwbAdminQueryService vwbAdminQueryService) {
    this.vwbAdminQueryService = vwbAdminQueryService;
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<VwbWorkspaceListResponse> getVwbWorkspaceByUsername(String username) {
    return ResponseEntity.ok(
        new VwbWorkspaceListResponse()
            .items(vwbAdminQueryService.queryVwbWorkspacesByCreator(username)));
  }
}
