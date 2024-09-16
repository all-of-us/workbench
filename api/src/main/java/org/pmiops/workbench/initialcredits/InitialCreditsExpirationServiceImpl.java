package org.pmiops.workbench.initialcredits;

import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration.NotificationStatus;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
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
  private final MailService mailService;
  private final Clock clock;
  private final WorkspaceDao workspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final LeonardoApiClient leonardoApiClient;

  @Autowired
  public InitialCreditsExpirationServiceImpl(
      UserDao userDao,
      MailService mailService,
      WorkspaceDao workspaceDao,
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      LeonardoApiClient leonardoApiClient) {
    this.userDao = userDao;
    this.mailService = mailService;
    this.clock = clock;
    this.workspaceDao = workspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.leonardoApiClient = leonardoApiClient;
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
        // TODO RW-13502 filter on institutional bypass as well, maybe something like
        // .filter(() -> institutionService.shouldBypassForCreditsExpiration(user))
        .map(DbUserInitialCreditsExpiration::getExpirationTime);
  }

  @Override
  public boolean haveCreditsExpired(DbUser user) {
    return getCreditsExpiration(user)
        .map(expirationTime -> !expirationTime.after(new Timestamp(clock.instant().toEpochMilli())))
        .orElse(false);
  }

  private void checkExpiration(DbUser user) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (null != userInitialCreditsExpiration
        && haveCreditsExpired(user)
        && userInitialCreditsExpiration
            .getNotificationStatus()
            .equals(NotificationStatus.NO_NOTIFICATION_SENT)) {
      logger.info(
          "Initial credits expired for user {}. Expiration time: {}",
          user.getUsername(),
          userInitialCreditsExpiration.getExpirationTime());
      boolean hasExhaustedWorkspace =
          workspaceDao.findAllByCreator(user).parallelStream()
              .anyMatch(DbWorkspace::isInitialCreditsExhausted);

      // Set initial credits expired for all workspaces regardless of whether
      // they have exhausted their credits or not.
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
      try {
        // If the user has already been notified about exhausting their initial credits,
        // we do not need to notify them about expiration as well.
        if (!hasExhaustedWorkspace) {
          mailService.alertUserInitialCreditsExpired(user);
          userInitialCreditsExpiration.setNotificationStatus(
              NotificationStatus.EXPIRATION_NOTIFICATION_SENT);
          userDao.save(user);
        }

      } catch (MessagingException e) {
        logger.error(
            String.format(
                "Failed to send initial credits expiration notification for user %s",
                user.getUserId()));
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
}
