package org.pmiops.workbench.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.RegisteredDomainRequest;


@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private static final Logger log = Logger.getLogger(BugReportController.class.getName());
  private final FireCloudService fireCloudService;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService) {
    this.fireCloudService = fireCloudService;
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

  @Override
  public ResponseEntity<EmptyResponse> addToRegistered(RegisteredDomainRequest request) {
    try {
      fireCloudService.addUserToRegisteredGroup(request.getEmail());
    } catch (ApiException e) {
      if (e.getCode() == 403) {
        throw new ForbiddenException(e.getResponseBody());
      } else if (e.getCode() == 404) {
        throw new BadRequestException(e.getResponseBody());
      } else {
        throw new ServerErrorException(e.getResponseBody());
      }
    }
    return ResponseEntity.ok(new EmptyResponse());
  }
}
