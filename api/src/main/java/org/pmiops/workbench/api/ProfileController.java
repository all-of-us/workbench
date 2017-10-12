package org.pmiops.workbench.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.RegistrationRequest;
import org.pmiops.workbench.model.UsernameTakenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController implements ProfileApiDelegate {

  private static final Logger log = Logger.getLogger(ProfileController.class.getName());

  private final ProfileService profileService;
  private final Provider<User> userProvider;
  private final UserDao userDao;
  private final FireCloudService fireCloudService;
  private final DirectoryService directoryService;

  @Autowired
  ProfileController(ProfileService profileService, Provider<User> userProvider, UserDao userDao,
        FireCloudService fireCloudService, DirectoryService directoryService) {
    this.profileService = profileService;
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.fireCloudService = fireCloudService;
    this.directoryService = directoryService;
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
  public ResponseEntity<UsernameTakenResponse> isUsernameTaken(String username) {
    return ResponseEntity.ok(
        new UsernameTakenResponse().isTaken(directoryService.isUsernameTaken(username)));
  }

  @Override
  public ResponseEntity<Void> createAccount(CreateAccountRequest request) {
    directoryService.createUser(
        request.getGivenName(), request.getFamilyName(), request.getUsername(),
        request.getPassword()
    );
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteAccount() {
    UserAuthentication userAuth =
        (UserAuthentication)SecurityContextHolder.getContext().getAuthentication();
    String email = userAuth.getPrincipal().getEmail();
    String[] parts = email.split("@");
    directoryService.deleteUser(parts[0]);
    return null;
  }

  @Override
  public ResponseEntity<Profile> register(RegistrationRequest registrationRequest) {
    User user = userProvider.get();
    if (user.getDataAccessLevel() != DataAccessLevel.UNREGISTERED) {
      throw new BadRequestException("User {0} is already registered".format(user.getEmail()));
    }
    try {
      fireCloudService.registerUser(registrationRequest.getContactEmail(),
          registrationRequest.getGivenName(), registrationRequest.getFamilyName());
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error registering user: {0}".format(e.getResponseBody()), e);
      // We don't expect this to happen.
      // TODO: figure out what happens if you register after already registering
      throw new ServerErrorException("Error registering user", e);
    }
    String billingProjectName = "all-of-us-free-" + user.getUserId();
    try {
      fireCloudService.createAllOfUsBillingProject(billingProjectName);
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error creating billing project: {0}".format(e.getResponseBody()), e);
      // TODO: figure out what happens if this project already exists and is either owned by
      // AofU or not; consider generating a new name with a numeric suffix.
      throw new ServerErrorException("Error creating billing project", e);
    }
    try {
      fireCloudService.addUserToBillingProject(user.getEmail(), billingProjectName);
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error adding user to billing project: {0}".format(e.getResponseBody()),
          e);
      // TODO: figure out what happens if the user is already a member of this billing project.
      throw new ServerErrorException("Error adding user to billing project", e);
    }
    // TODO: consider whether we want to store this here, or just use Google membership checks
    user.setDataAccessLevel(DataAccessLevel.REGISTERED);
    user.setContactEmail(registrationRequest.getContactEmail());
    user.setFamilyName(registrationRequest.getFamilyName());
    user.setFullName(registrationRequest.getFullName());
    user.setGivenName(registrationRequest.getGivenName());
    user.setPhoneNumber(registrationRequest.getPhoneNumber());
    user.setFreeTierBillingProjectName(billingProjectName);

    userDao.save(user);

    try {
      return ResponseEntity.ok(profileService.getProfile(user));
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error getting user profile: {0}".format(e.getResponseBody()), e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }
}
