package org.pmiops.workbench.api;

import java.sql.Timestamp;
import java.time.Clock;
import javax.inject.Provider;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
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

  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final UserDao userDao;
  private final Provider<User> userProvider;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService,
      Clock clock,
      UserDao userDao,
      Provider<User> userProvider) {
    this.fireCloudService = fireCloudService;
    this.clock = clock;
    this.userDao = userDao;
    this.userProvider = userProvider;
  }

  @AuthorityRequired({Authority.MANAGE_GROUP})
  @Override
  public ResponseEntity<EmptyResponse> createAuthDomain(String groupName) {
    try {
      fireCloudService.createGroup(groupName);
    } catch (ApiException e) {
      if (e.getCode() == 409) {
        throw new ConflictException(e.getResponseBody());
      } else {
        throw new ServerErrorException(e.getResponseBody());
      }
    }
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.MANAGE_GROUP})
  public ResponseEntity<Void> removeUserFromAuthDomain(String groupName, AuthDomainRequest request) {
    final Timestamp timestamp = new Timestamp(clock.instant().toEpochMilli());
    User user = userDao.findUserByEmail(request.getEmail());
    try {
      fireCloudService.removeUserFromGroup(request.getEmail(), groupName);
    } catch (ApiException e) {
      if (e.getCode() == 403) {
        throw new ForbiddenException(e.getResponseBody());
      } else if (e.getCode() == 404) {
        throw new NotFoundException(e.getResponseBody());
      } else {
        throw new ServerErrorException(e.getResponseBody());
      }
    }
    user.setDataAccessLevel(DataAccessLevel.REVOKED);
    user.setDisabled(true);
    user.setDisabledTime(timestamp);
    user.setDisablingAdminId(userProvider.get().getUserId());
    userDao.save(user);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @AuthorityRequired({Authority.MANAGE_GROUP})
  public ResponseEntity<Void> addUserToAuthDomain(String groupName, AuthDomainRequest request) {
    User user = userDao.findUserByEmail(request.getEmail());
    try {
      fireCloudService.addUserToGroup(request.getEmail(), groupName);
    } catch (ApiException e) {
      if (e.getCode() == 403) {
        throw new ForbiddenException(e.getResponseBody());
      } else if (e.getCode() == 404) {
        throw new NotFoundException(e.getResponseBody());
      } else {
        throw new ServerErrorException(e.getResponseBody());
      }
    }
    // TODO(blrubenstein): Parameterize this.
    user.setDataAccessLevel(DataAccessLevel.REGISTERED);
    user.setDisabled(false);
    userDao.save(user);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
