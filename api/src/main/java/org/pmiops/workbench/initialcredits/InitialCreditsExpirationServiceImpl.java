package org.pmiops.workbench.initialcredits;

import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
import org.pmiops.workbench.workspaces.WorkspaceUtils;
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
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      Iterable<DbUser> users = userDao.findAllById(userIdsList);
      users.forEach(user -> checkExpiration(user, now));
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

  private void checkExpiration(DbUser user, Timestamp now) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (null != userInitialCreditsExpiration
        && !userInitialCreditsExpiration.isBypassed()
        && userInitialCreditsExpiration
            .getNotificationStatus()
            .equals(NotificationStatus.NO_NOTIFICATION_SENT)
        && !(userInitialCreditsExpiration.getExpirationTime().after(now))) {
      logger.info(
          "Initial credits expired for user {}. Expiration time: {}",
          user.getUsername(),
          userInitialCreditsExpiration.getExpirationTime());
      Stream<DbWorkspace> expiringWorkspaces =
          workspaceDao.findAllByCreator(user).stream()
              .filter(
                  ws ->
                      WorkspaceUtils.isFreeTier(
                          ws.getBillingAccountName(), workbenchConfigProvider.get()))
              .filter(DbWorkspace::isActive)
              .filter(ws -> ws.getBillingStatus().equals(BillingStatus.ACTIVE));
      logger.info(
          "Setting billing status to invalid for all workspaces owned by user {}",
          user.getUsername());
      expireBillingStatusForWorkspaces(expiringWorkspaces);
      logger.info("Deleting apps and runtimes in workspaces owned by user {}", user.getUsername());
      deleteAppsAndRuntimesInWorkspaces(expiringWorkspaces);
      try {
        mailService.alertUserInitialCreditsExpired(user);
        userInitialCreditsExpiration.setNotificationStatus(
            NotificationStatus.EXPIRATION_NOTIFICATION_SENT);
        userDao.save(user);

      } catch (MessagingException e) {
        logger.error(
            String.format(
                "Failed to send initial credits expiration notification for user %s",
                user.getUserId()));
      }
    }
  }

  private void expireBillingStatusForWorkspaces(Stream<DbWorkspace> workspaces) {
    workspaces
        .map(DbWorkspace::getWorkspaceId)
        .forEach(id -> workspaceDao.updateBillingStatus(id, BillingStatus.INACTIVE));
  }

  private void deleteAppsAndRuntimesInWorkspaces(Stream<DbWorkspace> workspaces) {
    workspaces.forEach(
        dbWorkspace -> {
          String namespace = dbWorkspace.getWorkspaceNamespace();
          try {
            leonardoApiClient.deleteAllResources(dbWorkspace.getGoogleProject(), false);
            logger.info("Deleted apps and runtimes for workspace {}", namespace);
          } catch (WorkbenchException e) {
            logger.error("Failed to delete apps and runtimes for workspace {}", namespace, e);
          }
        });
  }
}
