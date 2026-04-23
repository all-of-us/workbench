package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.VwbDataCollectionListResponse;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbDataCollectionAdminController implements VwbDataCollectionAdminApiDelegate {

  private final VwbAdminQueryService vwbAdminQueryService;

  @Autowired
  public VwbDataCollectionAdminController(VwbAdminQueryService vwbAdminQueryService) {
    this.vwbAdminQueryService = vwbAdminQueryService;
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<VwbDataCollectionListResponse> listVwbDataCollections() {
    return ResponseEntity.ok(
        new VwbDataCollectionListResponse().items(vwbAdminQueryService.queryVwbDataCollections()));
  }
}
