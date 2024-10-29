package org.pmiops.workbench.initialcredits;

import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.utils.BillingUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InitialCreditsExpirationServiceImpl implements InitialCreditsExpirationService {

  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(InitialCreditsExpirationServiceImpl.class);
  private final UserDao userDao;
  private final Clock clock;
  private final WorkspaceDao workspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final LeonardoApiClient leonardoApiClient;
  private final InstitutionService institutionService;
  private final MailService mailService;

  @Autowired
  public InitialCreditsExpirationServiceImpl(
      UserDao userDao,
      WorkspaceDao workspaceDao,
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      LeonardoApiClient leonardoApiClient,
      InstitutionService institutionService, MailService mailService) {
    this.userDao = userDao;
    this.clock = clock;
    this.workspaceDao = workspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.leonardoApiClient = leonardoApiClient;
    this.institutionService = institutionService;
    this.mailService = mailService;
  }

  @Override
  public void checkCreditsExpirationForUserIDs(List<Long> userIdsList) {
    if (userIdsList != null && !userIdsList.isEmpty()) {
      Iterable<DbUser> users = userDao.findAllById(userIdsList);
      users.forEach(this::checkExpiration);
    }
  }

  @Override
  public Optional<Timestamp> getCreditsExpiration(DbUser user) {
    return Optional.ofNullable(user.getUserInitialCreditsExpiration())
        .filter(exp -> !exp.isBypassed()) // If the expiration is bypassed, return empty.
        .filter(exp -> !institutionService.shouldBypassForCreditsExpiration(user))
        .map(DbUserInitialCreditsExpiration::getExpirationTime);
  }

  @Override
  public boolean haveCreditsExpired(DbUser user) {
    return getCreditsExpiration(user)
        .map(expirationTime -> !expirationTime.after(new Timestamp(clock.instant().toEpochMilli())))
        .orElse(false);
  }

  // Returns true if the user's credits are expiring within the initialCreditsExpirationWarningDays.
  private boolean areCreditsExpiringSoon(DbUser user) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    long initialCreditsExpirationWarningDays =
        workbenchConfigProvider.get().billing.initialCreditsExpirationWarningDays;
    return getCreditsExpiration(user)
        .map(
            expirationTime ->
                now.after(
                    new Timestamp(
                        expirationTime.getTime()
                            - TimeUnit.DAYS.toMillis(initialCreditsExpirationWarningDays))))
        .orElse(false);
  }

  @Override
  public DbUserInitialCreditsExpiration createInitialCreditsExpiration(DbUser user) {
    long initialCreditsValidityPeriodDays =
        workbenchConfigProvider.get().billing.initialCreditsValidityPeriodDays;
    Timestamp now = clockNow();
    Timestamp expirationTime =
        new Timestamp(now.getTime() + TimeUnit.DAYS.toMillis(initialCreditsValidityPeriodDays));
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setCreditStartTime(now)
            .setExpirationTime(expirationTime)
            .setUser(user);
    user.setUserInitialCreditsExpiration(userInitialCreditsExpiration);
    return userInitialCreditsExpiration;
  }

  @Override
  public void setInitialCreditsExpirationBypassed(DbUser user, boolean isBypassed) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (userInitialCreditsExpiration == null) {
      userInitialCreditsExpiration = createInitialCreditsExpiration(user);
    }
    userInitialCreditsExpiration.setBypassed(isBypassed);
  }

  @Override
  public void extendInitialCreditsExpiration(DbUser user) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (userInitialCreditsExpiration == null) {
      throw new WorkbenchException("User does not have initial credits expiration set.");
    }
    if (userInitialCreditsExpiration.getExtensionCount() != 0) {
      throw new WorkbenchException(
          "User has already extended their initial credits expiration and cannot extend further.");
    }
    userInitialCreditsExpiration.setExpirationTime(
        new Timestamp(
            userInitialCreditsExpiration.getCreditStartTime().getTime()
                + TimeUnit.DAYS.toMillis(
                    workbenchConfigProvider.get().billing.initialCreditsExtensionPeriodDays)));
    userInitialCreditsExpiration.setExtensionCount(
        userInitialCreditsExpiration.getExtensionCount() + 1);
    userDao.save(user);
  }

  private void checkExpiration(DbUser user) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (null != userInitialCreditsExpiration
        && null == userInitialCreditsExpiration.getExpirationCleanupTime()) {
      if (haveCreditsExpired(user)) {
        logger.info(
            "Initial credits expired for user {}. Expiration time: {}",
            user.getUsername(),
            userInitialCreditsExpiration.getExpirationTime());

        workspaceDao.findAllByCreator(user).stream()
            .filter(
                ws ->
                    BillingUtils.isInitialCredits(
                        ws.getBillingAccountName(), workbenchConfigProvider.get()))
            .filter(DbWorkspace::isActive)
            .filter(ws -> !ws.isInitialCreditsExpired())
            .forEach(
                ws -> {
                  ws.setInitialCreditsExpired(true);
                  ws.setBillingStatus(BillingStatus.INACTIVE);
                  workspaceDao.save(ws);
                  deleteAppsAndRuntimesInWorkspace(ws);
                });

        userInitialCreditsExpiration.setExpirationCleanupTime(Timestamp.from(Instant.now()));
        userDao.save(user);
      } else if (areCreditsExpiringSoon(user)
          && null == userInitialCreditsExpiration.getApproachingExpirationNotificationTime()) {
        logger.info(
            "Initial credits expiring soon for user {}. Expiration time: {}",
            user.getUsername(),
            userInitialCreditsExpiration.getExpirationTime());
        try {
          mailService.alertUserInitialCreditsExpiring(user);
          userInitialCreditsExpiration.setApproachingExpirationNotificationTime(Timestamp.from(Instant.now()));
          userDao.save(user);
        } catch (MessagingException e) {
          logger.error(
              String.format(
                  "Failed to send initial credits expiration warning notification for user %s",
                  user.getUserId()));
        }
      }
    }
  }

  private void deleteAppsAndRuntimesInWorkspace(DbWorkspace workspace) {

    String namespace = workspace.getWorkspaceNamespace();
    try {
      leonardoApiClient.deleteAllResources(workspace.getGoogleProject(), false);
      logger.info("Deleted apps and runtimes for workspace {}", namespace);
    } catch (WorkbenchException e) {
      logger.error("Failed to delete apps and runtimes for workspace {}", namespace, e);
    }
  }

  private Timestamp clockNow() {
    return new Timestamp(clock.instant().toEpochMilli());
  }
}
