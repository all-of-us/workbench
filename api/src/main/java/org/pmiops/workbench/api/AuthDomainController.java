package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmptyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private final FireCloudService fireCloudService;
  private final UserService userService;
  private final UserDao userDao;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService,
      UserService userService,
      UserDao userDao) {
    this.fireCloudService = fireCloudService;
    this.userService = userService;
    this.userDao = userDao;
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
    User user = userDao.findUserByEmail(request.getEmail());
    Boolean previousDisabled = user.getDisabled();
    User updatedUser = userService.setDisabledStatus(user.getUserId(), request.getDisabled());
    userService.logAdminUserAction(
        user.getUserId(),
        "updated user disabled state",
        previousDisabled,
        request.getDisabled());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
