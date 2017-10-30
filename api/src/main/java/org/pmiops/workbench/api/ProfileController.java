package org.pmiops.workbench.api;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.BillingProjectMembership.StatusEnum;
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

  private static final Function<org.pmiops.workbench.firecloud.model.BillingProjectMembership,
      BillingProjectMembership> TO_CLIENT_BILLING_PROJECT_MEMBERSHIP =
      new Function<org.pmiops.workbench.firecloud.model.BillingProjectMembership, BillingProjectMembership>() {
        @Override
        public BillingProjectMembership apply(
            org.pmiops.workbench.firecloud.model.BillingProjectMembership billingProjectMembership) {
          BillingProjectMembership result = new BillingProjectMembership();
          result.setMessage(billingProjectMembership.getMessage());
          result.setProjectName(billingProjectMembership.getProjectName());
          result.setRole(billingProjectMembership.getRole());
          result.setStatus(StatusEnum.fromValue(billingProjectMembership.getStatus().toString()));
          return result;
        }
      };

  private static final Logger log = Logger.getLogger(ProfileController.class.getName());
  private static final long MAX_BILLING_PROJECT_CREATION_ATTEMPTS = 5;

  private final ProfileService profileService;
  private final Provider<User> userProvider;
  private final Provider<Userinfoplus> userinfoplusProvider;
  private final UserDao userDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final DirectoryService directoryService;
  private final CloudStorageService cloudStorageService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkbenchEnvironment workbenchEnvironment;

  @Autowired
  ProfileController(ProfileService profileService, Provider<User> userProvider,
      Provider<Userinfoplus> userinfoplusProvider, UserDao userDao,
      Clock clock, FireCloudService fireCloudService, DirectoryService directoryService,
      CloudStorageService cloudStorageService, Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkbenchEnvironment workbenchEnvironment) {
    this.profileService = profileService;
    this.userProvider = userProvider;
    this.userinfoplusProvider = userinfoplusProvider;
    this.userDao = userDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.directoryService = directoryService;
    this.cloudStorageService = cloudStorageService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workbenchEnvironment = workbenchEnvironment;
  }

  @Override
  public ResponseEntity<List<BillingProjectMembership>> getBillingProjects() {
    try {
      List<org.pmiops.workbench.firecloud.model.BillingProjectMembership> memberships =
          fireCloudService.getBillingProjectMemberships();
      return ResponseEntity.ok(memberships.stream().map(TO_CLIENT_BILLING_PROJECT_MEMBERSHIP)
          .collect(Collectors.toList()));
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Error fetching billing project memberships: {0}"
          .format(e.getResponseBody()), e);
      throw new ServerErrorException("Error fetching billing project memberships", e);
    }
  }

  private String createFirecloudUserAndBillingProject(User user) {
    try {
      // If the user is already registered, their profile will get updated.
      fireCloudService.registerUser(user.getContactEmail(),
          user.getGivenName(), user.getFamilyName());
    } catch (ApiException e) {
      log.log(Level.SEVERE, String.format("Error registering user: %s", e.getResponseBody()), e);
      // We don't expect this to happen.
      throw new ServerErrorException("Error registering user", e);
    }
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    long suffix;
    if (workbenchEnvironment.isDevelopment()) {
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
        break;
      } catch (ApiException e) {
        if (e.getCode() == HttpStatus.CONFLICT.value()) {
          if (workbenchEnvironment.isDevelopment()) {
            // In local development, just re-use existing projects for the account. (We don't
            // want to create a new billing project every time the database is reset.)
            log.log(Level.WARNING, "Project with name {0} already exists; using it."
                .format(billingProjectName));
            break;
          } else {
            numAttempts++;
            // In cloud environments, keep trying billing project names until we find one
            // that hasn't been used before, or we hit MAX_BILLING_PROJECT_CREATION_ATTEMPTS.
            billingProjectName = billingProjectNamePrefix + "-" + numAttempts;
          }
        } else {
          log.log(
              Level.SEVERE,
              String.format("Error creating billing project: %s", e.getResponseBody()),
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
      // If the user is already a member of the billing project, this will have no effect.
      fireCloudService.addUserToBillingProject(user.getEmail(), billingProjectName);
    } catch (ApiException e) {

      if (e.getCode() == HttpStatus.FORBIDDEN.value()) {
        // AofU is not the owner of the billing project. This should only happen in local
        // environments (and hopefully never, given the prefix we're using.) If it happens,
        // we may need to pick a different prefix.
        log.log(Level.SEVERE, ("Unable to add user to billing project {0}: {0}; " +
            "consider changing billing project prefix").format(billingProjectName,
            e.getResponseBody()), e);
        throw new ServerErrorException("Unable to add user to billing project", e);
      } else {
        log.log(Level.SEVERE,
            String.format("Error adding user to billing project: %s", e.getResponseBody()),
            e);
        throw new ServerErrorException("Error adding user to billing project", e);
      }
    }
    return billingProjectName;
  }

  private User initializeUserIfNeeded() {
    User user = userProvider.get();
    if (user == null) {
      // For now, this will happen with any account that you sign in with (since we're not
      // creating Google accounts yet.) Create a user record for the account.

      // After we start requiring that user accounts are in our GSuite domain, this should only
      // happen if a Google account was created but we failed to write to our
      // database right after that.
      // TODO: remove this logic when we start using Gsuite account info, or start storing the
      // contact email in Google account info so we can use it here. (We don't want User records
      // without contact emails.)
      Userinfoplus userInfo = userinfoplusProvider.get();
      user = new User();
      user.setDataAccessLevel(DataAccessLevel.UNREGISTERED);
      user.setEmail(userInfo.getEmail());
      user.setGivenName(userInfo.getGivenName());
      user.setFamilyName(userInfo.getFamilyName());
      userDao.save(user);
    }

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
    // Record that the user signed in, and create the user's FireCloud user and free tier billing
    // project if they haven't been created already.
    // This means they can start using the NIH billing account in FireCloud (without access to
    // the CDR); we will probably need a job that deactivates accounts after some period of
    // not accepting the terms of use.

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
    try {
      return ResponseEntity.ok(
          new UsernameTakenResponse().isTaken(directoryService.isUsernameTaken(username)));
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
  }

  @Override
  public ResponseEntity<Profile> createAccount(CreateAccountRequest request) {
    if (request.getInvitationKey() == null
        || !request.getInvitationKey().equals(cloudStorageService.readInvitationKey())) {
      throw new BadRequestException(
          "Missing or incorrect invitationKey (this API is not yet publicly launched)");
    }
    com.google.api.services.admin.directory.model.User googleUser;
    try {
      googleUser = directoryService.createUser(request.getProfile().getGivenName(),
          request.getProfile().getFamilyName(), request.getProfile().getUsername(),
          request.getPassword());
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }

    // Create a user that has no data access or FC user associated.
    // We create this account before they sign in so we can keep track of which users we have
    // created Google accounts for. This can be used subsequently to delete orphaned accounts.

    // We store this information in our own database so that:
    // 1) we can support bring-your-own account in future (when we won't be using directory service)
    // 2) we can easily generate lists of researchers for the storefront, without joining to Google

    // It's possible for the profile information to become out of sync with the user's Google
    // profile, since it can be edited in our UI as well as the Google UI,  and we're fine with
    // that; the expectation is their profile in AofU will be managed in AofU, not in Google.

    User user = new User();
    user.setDataAccessLevel(DataAccessLevel.UNREGISTERED);
    user.setEmail(googleUser.getPrimaryEmail());
    user.setContactEmail(request.getProfile().getContactEmail());
    user.setFamilyName(request.getProfile().getFamilyName());
    user.setGivenName(request.getProfile().getGivenName());
    userDao.save(user);

    // TODO(dmohs): This should be 201 Created with no body, but the UI's swagger-generated code
    // doesn't allow this. Fix.
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> deleteAccount() {
    UserAuthentication userAuth =
        (UserAuthentication)SecurityContextHolder.getContext().getAuthentication();
    String email = userAuth.getPrincipal().getEmail();
    String[] parts = email.split("@");
    try {
      directoryService.deleteUser(parts[0]);
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }

    userDao.delete(userDao.findUserByEmail(email));

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Profile> register(RegistrationRequest registrationRequest) {
    User user = initializeUserIfNeeded();
    if (user.getDataAccessLevel() != DataAccessLevel.UNREGISTERED) {
      throw new BadRequestException(String.format(
          "User %s is already registered", user.getEmail()));
    }
    // TODO: add user to authorization domain for registered access; add pet SA to
    // Google group for CDR access
    user.setDataAccessLevel(DataAccessLevel.REGISTERED);
    userDao.save(user);

    try {
      return ResponseEntity.ok(profileService.getProfile(user));
    } catch (ApiException e) {
      log.log(
          Level.SEVERE, String.format("Error getting user profile: %s", e.getResponseBody()), e);
      return ResponseEntity.status(e.getCode()).build();
    }
  }
}
