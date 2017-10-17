package org.pmiops.workbench.api;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.Environment;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.RegistrationRequest;
import org.pmiops.workbench.model.UsernameTakenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController implements ProfileApiDelegate {

  private static final Logger log = Logger.getLogger(ProfileController.class.getName());
  private static final long MAX_BILLING_PROJECT_CREATION_ATTEMPTS = 5;

  private final ProfileService profileService;
  private final Provider<User> userProvider;
  private final UserDao userDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final DirectoryService directoryService;
  private final CloudStorageService cloudStorageService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Environment environment;

  @Autowired
  ProfileController(ProfileService profileService, Provider<User> userProvider, UserDao userDao,
        Clock clock, FireCloudService fireCloudService, DirectoryService directoryService,
        CloudStorageService cloudStorageService,
        Provider<WorkbenchConfig> workbenchConfigProvider, Environment environment) {
    this.profileService = profileService;
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.directoryService = directoryService;
    this.cloudStorageService = cloudStorageService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.environment = environment;
  }

  @Override
  public ResponseEntity<List<BillingProjectMembership>> getBillingProjects() {
    // TODO: retrieve billing projects
    return ResponseEntity.ok(new ArrayList<>());
  }

  private String createFirecloudUserAndBillingProject(User user) {
    try {
      fireCloudService.registerUser(user.getContactEmail(),
          user.getGivenName(), user.getFamilyName());
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error registering user: {0}".format(e.getResponseBody()), e);
      // We don't expect this to happen.
      // TODO: figure out what happens if you register after already registering
      throw new ServerErrorException("Error registering user", e);
    }
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    long suffix;
    if (environment.isDevelopment()) {
      // For local development, make one billing project per account based on a hash of the account
      // email, and reuse it across database resets. (Assume we won't have any collisions;
      // if we discover that somebody starts using our namespace, change it up.)
      suffix = user.getEmail().hashCode();
    } else {
      // In other environments, create a suffix based on the user ID from the database. We will
      // add a suffix if that billing project is already taken. (If the database is reset, we
      // should consider switching the prefix.)
      suffix = user.getUserId();
    }
    String billingProjectNamePrefix = workbenchConfig.firecloud.billingProjectPrefix + suffix;
    String billingProjectName = billingProjectNamePrefix;
    int numAttempts = 0;
    while (numAttempts < MAX_BILLING_PROJECT_CREATION_ATTEMPTS) {
      try {
        fireCloudService.createAllOfUsBillingProject(billingProjectName);
      } catch (ApiException e) {
        if (e.getCode() == HttpStatus.CONFLICT.value()) {
          if (environment.isDevelopment()) {
            // In local development, just re-use existing projects for the account. (We don't
            // want to create a new billing project every time the database is reset.)
            log.log(Level.WARNING, "Project with name {0} already exists; using it."
                .format(billingProjectName));
          } else {
            numAttempts++;
            // In cloud environments, keep trying billing project names until we find one
            // that hasn't been used before, or we hit MAX_BILLING_PROJECT_CREATION_ATTEMPTS.
            billingProjectName = billingProjectNamePrefix + "-" + numAttempts;
          }
        } else {
          log.log(Level.SEVERE, "Error creating billing project: {0}".format(e.getResponseBody()),
              e);
          throw new ServerErrorException("Error creating billing project", e);
        }
      }
    }
    if (numAttempts == MAX_BILLING_PROJECT_CREATION_ATTEMPTS) {
      throw new ServerErrorException("Encountered {0} billing project name collisions; giving up"
          .format(String.valueOf(MAX_BILLING_PROJECT_CREATION_ATTEMPTS)));
    }

    try {
      fireCloudService.addUserToBillingProject(user.getEmail(), billingProjectName);
    } catch (ApiException e) {
      // If we used an existing project above, it's possible this will fail. That should only
      // happen in local development... hopefully it won't. :)
      log.log(Level.SEVERE, "Error adding user to billing project: {0}".format(e.getResponseBody()),
          e);
      // TODO: figure out what happens if the user is already a member of this billing project.
      throw new ServerErrorException("Error adding user to billing project", e);
    }
    return billingProjectName;
  }

  private User initializeUserIfNeeded() {
    User user = userProvider.get();
    // On first sign-in, create a FC user, billing project, and set the first sign in time.
    if (user.getFirstSignInTime() == null) {
      if (user.getFreeTierBillingProjectName() == null) {
        String billingProjectName = createFirecloudUserAndBillingProject(user);
        user.setFreeTierBillingProjectName(billingProjectName);
      }
      user.setFirstSignInTime(new Timestamp(clock.instant().toEpochMilli()));
      userDao.save(user);
    }
    return user;
  }

  @Override
  public ResponseEntity<Profile> getMe() {
    User user = initializeUserIfNeeded();
    try {
      return ResponseEntity.ok(profileService.getProfile(user));
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
  public ResponseEntity<Profile> createAccount(CreateAccountRequest request) {
    if (request.getInvitationKey() == null
        || !request.getInvitationKey().equals(cloudStorageService.readInvitationKey())) {
      throw new BadRequestException(
          "Missing or incorrect invitationKey (this API is not yet publicly launched)");
    }
    com.google.api.services.admin.directory.model.User googleUser = directoryService.createUser(
        request.getGivenName(), request.getFamilyName(), request.getUsername(),
        request.getPassword()
    );

    // Create a user that has no data access or FC user associated.
    User user = new User();
    user.setEmail(googleUser.getPrimaryEmail());
    user.setContactEmail(request.getContactEmail());
    user.setFamilyName(request.getFamilyName());
    user.setGivenName(request.getGivenName());
    userDao.save(user);

    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(profileService.getProfile(user));
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error getting user profile: {0}".format(e.getResponseBody()), e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }

  @Override
  public ResponseEntity<Void> deleteAccount() {
    UserAuthentication userAuth =
        (UserAuthentication)SecurityContextHolder.getContext().getAuthentication();
    String email = userAuth.getPrincipal().getEmail();
    String[] parts = email.split("@");
    directoryService.deleteUser(parts[0]);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Profile> register(RegistrationRequest registrationRequest) {
    User user = initializeUserIfNeeded();
    if (user.getDataAccessLevel() != DataAccessLevel.UNREGISTERED) {
      throw new BadRequestException("User {0} is already registered".format(user.getEmail()));
    }
    // TODO: add user to authorization domain for registered access; add pet SA to
    // Google group for CDR access
    user.setDataAccessLevel(DataAccessLevel.REGISTERED);
    userDao.save(user);

    try {
      return ResponseEntity.ok(profileService.getProfile(user));
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error getting user profile: {0}".format(e.getResponseBody()), e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }
}
