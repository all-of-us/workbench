package org.pmiops.workbench.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.RegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController implements ProfileApiDelegate {

  private static final Logger log = Logger.getLogger(ProfileController.class.getName());

  private final ProfileService profileService;

  @Autowired
  ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @Override
  public ResponseEntity<List<BillingProjectMembership>> getBillingProjects() {
    // TODO: retrieve billing projects
    return ResponseEntity.ok(new ArrayList<>());
  }

  @Override
  public ResponseEntity<Profile> getMe() {
    try {
      return ResponseEntity.ok(profileService.getProfile());
    } catch (ApiException e) {
      log.log(Level.INFO, "Error calling FireCloud", e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }

  @Override
  public ResponseEntity<Profile> register(RegistrationRequest registrationRequest) {
    try {
      // TODO: do registration, etc.
      return ResponseEntity.ok(profileService.getProfile());
    } catch (ApiException e) {
      log.log(Level.INFO, "Error calling FireCloud", e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }
}
