package org.pmiops.workbench.api;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessSyncService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.AdminUserListResponse;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BatchSyncAccessRequest;
import org.pmiops.workbench.model.BatchSyncAccessResponse;
import org.pmiops.workbench.model.CreateEgressBypassWindowRequest;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.profile.ProfileService;
import org.pmiops.workbench.user.UserAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAdminController implements UserAdminApiDelegate {

  private final AccessModuleService accessModuleService;
  private final AccessSyncService accessSyncService;
  private final ActionAuditQueryService actionAuditQueryService;
  private final ProfileService profileService;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final TaskQueueService taskQueueService;
  private final UserService userService;
  private final UserAdminService userAdminService;

  public UserAdminController(
      AccessModuleService accessModuleService,
      AccessSyncService accessSyncService,
      ActionAuditQueryService actionAuditQueryService,
      ProfileService profileService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      TaskQueueService taskQueueService,
      UserService userService,
      UserAdminService userAdminService) {
    this.accessModuleService = accessModuleService;
    this.accessSyncService = accessSyncService;
    this.actionAuditQueryService = actionAuditQueryService;
    this.profileService = profileService;
    this.taskQueueService = taskQueueService;
    this.userProvider = userProvider;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userAdminService = userAdminService;
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
  public ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirements() {
    if (!workbenchConfigProvider.get().access.unsafeAllowSelfBypass) {
      throw new ForbiddenException("Self bypass is disallowed in this environment.");
    }
    final DbUser user = userProvider.get();
    accessModuleService.updateAllBypassTimes(user.getUserId());
    accessSyncService.updateUserAccessTiers(user, Agent.asUser(user));
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
                        .map(DbUser::getUserId)
                        .collect(Collectors.toList()))));
  }

  @Override
  @AuthorityRequired({Authority.SECURITY_ADMIN})
  public ResponseEntity<Void> createEgressBypassWindow(
      Long userId, CreateEgressBypassWindowRequest request) {
    userAdminService.createEgressBypassWindow(
        userId, Instant.ofEpochMilli(request.getStartTime()), request.getByPassDescription());

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<EgressBypassWindow> getEgressBypassWindow(Long userId) {
    return ResponseEntity.ok(userAdminService.getCurrentEgressBypassWindow(userId));
  }
}
