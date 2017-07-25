package org.pmiops.workbench.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.model.Profile;
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
  public ResponseEntity<Profile> getMe() {
    try {
      return ResponseEntity.ok(profileService.getProfile());
    } catch (ApiException e) {
      log.log(Level.INFO, "Error calling FireCloud", e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }
}
