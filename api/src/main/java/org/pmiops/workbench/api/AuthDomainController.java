package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.model.AuthDomainCreatedResponse;
import org.pmiops.workbench.model.Authority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private final FireCloudService fireCloudService;

  @Autowired
  AuthDomainController(FireCloudService fireCloudService) {
    this.fireCloudService = fireCloudService;
  }

  @AuthorityRequired({Authority.DEVELOPER})
  @Override
  public ResponseEntity<AuthDomainCreatedResponse> createAuthDomain(String authDomainName) {
    final FirecloudManagedGroupWithMembers group = fireCloudService.createGroup(authDomainName);
    return ResponseEntity.ok(
        new AuthDomainCreatedResponse()
            .authDomainName(authDomainName)
            .groupEmail(group.getGroupEmail()));
  }
}
