package org.pmiops.workbench.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.pmiops.workbench.actionaudit.auditors.AuthDomainAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateDisabledStatusForUsersRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private final FireCloudService fireCloudService;
  private final UserService userService;
  private final UserDao userDao;
  private AuthDomainAuditor authDomainAuditAdapter;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService,
      UserService userService,
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
  public ResponseEntity<Void> updateDisabledStatusForUsers(
      UpdateDisabledStatusForUsersRequest request) {
    final List<DbUser> dbUsers = userDao.findUsersByUsernameIn(request.getUsernameList());
    final Map<Long, Boolean> oldDisabledStatusByUserId =
        dbUsers.stream().collect(Collectors.toMap(DbUser::getUserId, DbUser::getDisabled));
    final List<DbUser> updatedDbUsers =
        userService.setDisabledStatusForUsers(dbUsers, request.getDisabled());
    final Map<Long, Boolean> newDisabledStatusByUserId =
        updatedDbUsers.stream().collect(Collectors.toMap(DbUser::getUserId, DbUser::getDisabled));
    dbUsers.forEach(
        user -> {
          auditAdminActions(
              newDisabledStatusByUserId.get(user.getUserId()),
              oldDisabledStatusByUserId.get(user.getUserId()),
              user);
        });
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  private void auditAdminActions(
      Boolean newDisabled, Boolean previousDisabled, DbUser updatedTargetUser) {
    authDomainAuditAdapter.fireSetAccountDisabledStatus(
        updatedTargetUser.getUserId(), newDisabled, previousDisabled);
    userService.logAdminUserAction(
        updatedTargetUser.getUserId(),
        "updated user disabled state",
        previousDisabled,
        newDisabled);
  }
}
