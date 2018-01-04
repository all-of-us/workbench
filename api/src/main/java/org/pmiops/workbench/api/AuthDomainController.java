package org.pmiops.workbench.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.RegisteredDomainRemoveRequest;
import org.springframework.http.HttpStatus;


@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private static final Logger log = Logger.getLogger(BugReportController.class.getName());
  private final FireCloudService fireCloudService;
  private final UserDao userDao;
  private final Provider<User> userProvider;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService,
      UserDao userDao,
      Provider<User> userProvider) {
    this.fireCloudService = fireCloudService;
    this.userDao = userDao;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<EmptyResponse> createRegisteredGroup() {
    try {
      fireCloudService.createRegisteredGroup();
    } catch (ApiException e) {
      if (e.getCode() == 409) {
        throw new ConflictException(e.getResponseBody());
      } else {
        throw new ServerErrorException(e.getResponseBody());
      }
    }
    return ResponseEntity.ok(new EmptyResponse());
  }

  @AuthorityRequired({Authority.REVOKE_REGISTERED_ACCESS})
  public ResponseEntity<Void> removeUserFromRegisteredGroup(
      RegisteredDomainRemoveRequest body) {
    User user = userProvider.get();

    try {
      fireCloudService.removeUserFromRegisteredGroup(body.getEmail());

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
    userDao.save(user);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
