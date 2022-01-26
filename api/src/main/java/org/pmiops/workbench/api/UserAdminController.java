package org.pmiops.workbench.api;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AuthDomainAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.AdminUserListResponse;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BatchSyncAccessRequest;
import org.pmiops.workbench.model.BatchSyncAccessResponse;
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
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final TaskQueueService taskQueueService;

  public UserAdminController(
      UserService userService,
      ProfileService profileService,
      ActionAuditQueryService actionAuditQueryService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      TaskQueueService taskQueueService) {
    this.userService = userService;
    this.profileService = profileService;
    this.actionAuditQueryService = actionAuditQueryService;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.taskQueueService = taskQueueService;
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> bypassAccessRequirement(
      Long userId, AccessBypassRequest request) {
    userService.updateBypassTime(userId, request);
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
      AccessBypassRequest request) {
    if (!workbenchConfigProvider.get().access.unsafeAllowSelfBypass) {
      throw new ForbiddenException("Self bypass is disallowed in this environment.");
    }
    long userId = userProvider.get().getUserId();
    userService.updateBypassTime(userId, request);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> updateAccountProperties(AccountPropertyUpdate request) {
    return ResponseEntity.ok(profileService.updateAccountProperties(request));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<BatchSyncAccessResponse> batchSyncAccess(BatchSyncAccessRequest request) {
    return ResponseEntity.ok(
        new BatchSyncAccessResponse()
            .cloudTaskNames(
                taskQueueService.groupAndPushSynchronizeAccessTasks(
                    userService.findUsersByUsernames(request.getUsernames()).stream()
                        .map(u -> u.getUserId())
                        .collect(Collectors.toList()))));
  }
}
