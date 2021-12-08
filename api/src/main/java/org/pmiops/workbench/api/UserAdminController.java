package org.pmiops.workbench.api;

import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.AdminUserListResponse;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.profile.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAdminController implements UserAdminApiDelegate {

  private final UserService userService;
  private final ProfileService profileService;
  private final ActionAuditQueryService actionAuditQueryService;
  private final UserDao userDao;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public UserAdminController(
      UserService userService,
      ProfileService profileService,
      ActionAuditQueryService actionAuditQueryService,
      UserDao userDao,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.userService = userService;
    this.profileService = profileService;
    this.actionAuditQueryService = actionAuditQueryService;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> bypassAccessRequirement(
      Long userId, AccessBypassRequest bypassed) {
    userService.updateBypassTime(userId, bypassed);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<AdminUserListResponse> getAllUsers() {
    return ResponseEntity.ok(
        new AdminUserListResponse().users(profileService.getAdminTableUsers()));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<UserAuditLogQueryResponse> getAuditLogEntries(
      String usernameWithoutGsuiteDomain,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillis) {
    final String username =
        String.format(
            "%s@%s",
            usernameWithoutGsuiteDomain,
            workbenchConfigProvider.get().googleDirectoryService.gSuiteDomain);
    final long userDatabaseId = userService.getByUsernameOrThrow(username).getUserId();
    final Instant after = Instant.ofEpochMilli(afterMillis);
    final Instant before =
        Optional.ofNullable(beforeMillis).map(Instant::ofEpochMilli).orElse(Instant.now());
    return ResponseEntity.ok(
        actionAuditQueryService.queryEventsForUser(userDatabaseId, limit, after, before));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> getUserByUsername(String username) {
    DbUser user = userService.getByUsernameOrThrow(username);
    return ResponseEntity.ok(profileService.getProfile(user));
  }

  @Override
  public ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirement(
      AccessBypassRequest bypassed) {
    if (!workbenchConfigProvider.get().access.unsafeAllowSelfBypass) {
      throw new ForbiddenException("Self bypass is disallowed in this environment.");
    }
    long userId = userProvider.get().getUserId();
    userService.updateBypassTime(userId, bypassed);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> updateAccountProperties(AccountPropertyUpdate request) {
    return ResponseEntity.ok(profileService.updateAccountProperties(request));
  }
}
