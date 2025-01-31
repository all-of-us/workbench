package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.initialcredits.InitialCreditsBatchUpdateService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskUserController implements CloudTaskUserApiDelegate {
  private static final Set<String> ALLOWED_PARENT_IDS =
      ImmutableSet.of(
          "400176686919", // test.firecloud.org
          "386193000800", // firecloud.org
          "394551486437", // pmi-ops.org
          // Terra Data Repo seemingly shares some data globally to all users. Our research users
          // therefore have access to some projects here.
          "815384374864", // Terra Data Repo (prod)
          "270278425081", // Terra Data Repo (dev)
          // TODO(calbach): Consider moving this into cdr_config_*.json
          "737976594150", // terra_dev_aou_test
          "272722258246", // terra_dev_aou_test_2
          "791559643292", // terra_perf_aou_perf
          "851853921816", // terra_perf_aou_perf_2
          "727685043294", // terra_prod_aou_staging
          "730698401092", // terra_prod_aou_staging_2
          "96867205717", // terra_prod_aou_stable
          "973382847971", // terra_prod_aou_stable_2
          "1030649683602", // terra_prod_aou_preprod
          "307813759063", // terra_prod_aou_preprod_2
          "300547813290", // terra_prod_aou_prod
          "762320479256"); // terra_prod_aou_prod_2
  private static final Logger log = Logger.getLogger(CloudTaskUserController.class.getName());

  private final AccessModuleService accessModuleService;
  private final CloudResourceManagerService cloudResourceManagerService;
  private final InitialCreditsBatchUpdateService freeTierBillingUpdateService;
  private final InitialCreditsService initialCreditsService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserService userService;

  CloudTaskUserController(
      AccessModuleService accessModuleService,
      CloudResourceManagerService cloudResourceManagerService,
      InitialCreditsBatchUpdateService freeTierBillingUpdateService,
      InitialCreditsService initialCreditsService,
      Provider<Stopwatch> stopwatchProvider,
      UserService userService) {
    this.accessModuleService = accessModuleService;
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.freeTierBillingUpdateService = freeTierBillingUpdateService;
    this.initialCreditsService = initialCreditsService;
    this.stopwatchProvider = stopwatchProvider;
    this.userService = userService;
  }

  @Override
  public ResponseEntity<Void> auditProjectAccessBatch(List<Long> userIds) {
    int errorCount = processDbUserBatch(userIds, "auditing project access", this::auditOneUser);

    if (errorCount > 0) {
      log.severe(String.format("encountered errors on %d/%d users", errorCount, userIds.size()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } else {
      log.info(String.format("successfully audited %d users", userIds.size()));
      return ResponseEntity.noContent().build();
    }
  }

  private int auditOneUser(DbUser user) {
    // TODO(RW-2062): Move to using the gcloud api for list all resources when it is
    // available.
    try {
      List<String> unauthorizedLogs =
          cloudResourceManagerService.getAllProjectsForUser(user).stream()
              .filter(
                  project ->
                      project.getParent() == null
                          || !(ALLOWED_PARENT_IDS.contains(getId(project.getParent()))))
              .map(
                  project ->
                      String.format(
                          "%s in %s %s",
                          project.getName(),
                          Optional.ofNullable(project.getParent())
                              .map(this::getType)
                              .orElse("[type unknown]"),
                          Optional.ofNullable(project.getParent())
                              .map(this::getId)
                              .orElse("[id unknown]")))
              .collect(Collectors.toList());
      if (!unauthorizedLogs.isEmpty()) {
        log.warning(
            "User "
                + user.getUsername()
                + " has access to projects: "
                + String.join(", ", unauthorizedLogs));
      }
    } catch (IOException e) {
      log.log(Level.SEVERE, "failed to audit project access for user " + user.getUsername(), e);
      return 1; // error
    }
    return 0; // no error
  }

  /**
   * Takes in batch of user Ids check whether users have incurred sufficient cost in their
   * workspaces to trigger alerts due to passing thresholds or exceeding limits
   *
   * @param userIds : Batch of user IDs from cloud task queue: freeTierBillingQueue
   * @return
   */
  @Override
  public ResponseEntity<Void> checkAndAlertFreeTierBillingUsageBatch(List<Long> userIds) {
    return processUserIdBatch(
        userIds,
        "alerting for initial credits usage",
        freeTierBillingUpdateService::checkAndAlertFreeTierBillingUsage);
  }

  @Override
  public ResponseEntity<Void> synchronizeUserAccessBatch(List<Long> userIds) {
    int errorCount = processDbUserBatch(userIds, "synchronizing access", this::syncOneUser);

    if (errorCount > 0) {
      log.severe(String.format("Encountered errors on %d/%d users", errorCount, userIds.size()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } else {
      log.info(String.format("Successfully synchronized %d users", userIds.size()));
      return ResponseEntity.noContent().build();
    }
  }

  private int syncOneUser(DbUser user) {
    try {
      // 2FA synchronization requires an outgoing call to gsuite. For this reason, we
      // optimize to only verify that users who have 2FA enabled, still have it
      // enabled.
      // Users who don't have 2FA will go through an active flow to enable it, and are
      // not
      // dependent on this offline check. Disabled users have no access anyways, so
      // don't
      // bother checking them either.
      if (!user.getDisabled()) {
        Optional<AccessModuleStatus> status =
            accessModuleService.getAccessModuleStatus(user, DbAccessModuleName.TWO_FACTOR_AUTH);
        if (status.isPresent() && status.get().getCompletionEpochMillis() != null) {
          user = userService.syncTwoFactorAuthStatus(user, Agent.asSystem());
        }
      }

      // Note: each module synchronization calls updateUserAccessTiers() which checks
      // the status
      // of *all* modules, so this serves as a general fallback as well (e.g. due to
      // partial
      // system failures or bugs), ensuring that the database and access tier groups
      // are
      // consistent with access module statuses.
      user = userService.syncDuccVersionStatus(user, Agent.asSystem());
    } catch (WorkbenchException e) {
      log.log(Level.SEVERE, "Failed to synchronize access for user " + user.getUsername(), e);
      return 1; // error
    }
    return 0; // no error
  }

  /**
   * Takes in batch of user IDs and check whether users have initial credits that have expired
   *
   * @param userIds : Batch of user IDs from cloud task queue: checkCreditsExpirationForUserIDsQueue
   */
  @Override
  public ResponseEntity<Void> checkCreditsExpirationForUserIDsBatch(List<Long> userIds) {
    return processUserIdBatch(
        userIds,
        "checking initial credits expiration",
        initialCreditsService::checkCreditsExpirationForUserIDs);
  }

  /**
   * Takes in batch of user IDs and check whether users' access has expired
   *
   * @param userIds : Batch of user IDs from cloud task queue: accessExpirationEmailQueue
   */
  @Override
  public ResponseEntity<Void> sendAccessExpirationEmailsBatch(List<Long> userIds) {
    processDbUserBatch(
        userIds,
        "checking access expiration",
        dbUsers -> {
          userService.maybeSendAccessTierExpirationEmails(dbUsers);
          return 0;
        });

    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<Void> processUserIdBatch(
      List<Long> userIds, String description, Consumer<List<Long>> fn) {
    processUserBatch(
        description,
        userIds.size(),
        () -> {
          fn.accept(userIds);
          return 0;
        });
    return ResponseEntity.noContent().build();
  }

  private int processDbUserBatch(List<Long> userIds, String description, ToIntFunction<DbUser> fn) {
    return processUserBatch(
        description,
        userIds.size(),
        () -> userIds.stream().map(userService::getByDatabaseIdOrThrow).mapToInt(fn).sum());
  }

  private int processUserBatch(String description, int size, IntSupplier batchFn) {
    log.info(String.format("%s for %d users...", StringUtils.capitalize(description), size));
    var stopwatch = stopwatchProvider.get().start();
    int errorCount = batchFn.getAsInt();
    var elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "Done %s for %d users in %s", description, size, formatDurationPretty(elapsed)));
    return errorCount;
  }

  // v1 cloudresourcemanager project.getParent() returned a ResourceId with type and id fields
  // v3 returns a string instead, in the format type/id

  private String getType(String typeAndId) {
    return typeAndId.split("/")[0];
  }

  private String getId(String typeAndId) {
    return typeAndId.split("/")[1];
  }
}
