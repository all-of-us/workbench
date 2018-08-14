package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.AuthDomainRequest;
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

  @AuthorityRequired({Authority.MANAGE_GROUP})
  @Override
  public ResponseEntity<EmptyResponse> createAuthDomain(String groupName) {
    fireCloudService.createGroup(groupName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.MANAGE_GROUP})
  public ResponseEntity<Void> removeUserFromAuthDomain(String groupName, AuthDomainRequest request) {
    User user = userDao.findUserByEmail(request.getEmail());
    DataAccessLevel previousAccess = StorageEnums.dataAccessLevelFromStorage(user.getDataAccessLevel());
    fireCloudService.removeUserFromGroup(request.getEmail(), groupName);
    user.setDataAccessLevel(StorageEnums.dataAccessLevelToStorage(DataAccessLevel.REVOKED));
    user.setDisabled(true);
    userDao.save(user);

    userService.logAdminUserAction(
        user.getUserId(),
        "user access to  " + groupName + " domain",
        previousAccess,
        DataAccessLevel.REVOKED);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @AuthorityRequired({Authority.MANAGE_GROUP})
  public ResponseEntity<Void> addUserToAuthDomain(String groupName, AuthDomainRequest request) {
    User user = userDao.findUserByEmail(request.getEmail());
    DataAccessLevel previousAccess = StorageEnums.dataAccessLevelFromStorage(user.getDataAccessLevel());
    fireCloudService.addUserToGroup(request.getEmail(), groupName);
    // TODO(blrubenstein): Parameterize this.
    user.setDataAccessLevel(StorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));
    user.setDisabled(false);
    userDao.save(user);

    userService.logAdminUserAction(
        user.getUserId(),
        "user access to  " + groupName + " domain",
        previousAccess,
        DataAccessLevel.REGISTERED);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
