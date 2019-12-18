package org.pmiops.workbench.api;

import org.pmiops.workbench.actionaudit.auditors.AuthDomainAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private final FireCloudService fireCloudService;
  private final UserServiceImpl userService;
  private final UserDao userDao;
  private AuthDomainAuditor authDomainAuditAdapter;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService,
      UserServiceImpl userService,
      UserDao userDao,
      AuthDomainAuditor authDomainAuditAdapter) {
    this.fireCloudService = fireCloudService;
    this.userService = userService;
    this.userDao = userDao;
    this.authDomainAuditAdapter = authDomainAuditAdapter;
  }

  @AuthorityRequired({Authority.DEVELOPER})
  @Override
  public ResponseEntity<EmptyResponse> createAuthDomain(String groupName) {
    fireCloudService.createGroup(groupName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Void> updateUserDisabledStatus(UpdateUserDisabledRequest request) {
    final DbUser targetDbUser = userDao.findUserByEmail(request.getEmail());
    final Boolean previousDisabled = targetDbUser.getDisabled();
    final DbUser updatedTargetUser =
        userService.setDisabledStatus(targetDbUser.getUserId(), request.getDisabled());
    auditAdminActions(request, previousDisabled, updatedTargetUser);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  private void auditAdminActions(
      UpdateUserDisabledRequest request, Boolean previousDisabled, DbUser updatedTargetUser) {
    authDomainAuditAdapter.fireSetAccountDisabledStatus(
        updatedTargetUser.getUserId(), request.getDisabled(), previousDisabled);
    userService.logAdminUserAction(
        updatedTargetUser.getUserId(),
        "updated user disabled state",
        previousDisabled,
        request.getDisabled());
  }
}
