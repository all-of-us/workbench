package org.pmiops.workbench.initialcredits;

import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration.NotificationStatus;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.workspaces.WorkspaceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InitialCreditsExpirationServiceImpl implements InitialCreditsExpirationService {

  private static final Logger log =
      Logger.getLogger(InitialCreditsExpirationServiceImpl.class.getName());
  private final UserDao userDao;
  private final MailService mailService;
  private final Clock clock;
  private final WorkspaceDao workspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public InitialCreditsExpirationServiceImpl(
      UserDao userDao,
      MailService mailService,
      WorkspaceDao workspaceDao,
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.userDao = userDao;
    this.mailService = mailService;
    this.clock = clock;
    this.workspaceDao = workspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void checkCreditsExpirationForUserIDs(List<Long> userIdsList) {
    if (userIdsList != null && !userIdsList.isEmpty()) {
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      Iterable<DbUser> users = userDao.findAllById(userIdsList);
      users.forEach(user -> checkCreditsExpirationForUser(user, now));
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

  private void checkCreditsExpirationForUser(DbUser user, Timestamp now) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (null != userInitialCreditsExpiration
        && !userInitialCreditsExpiration.isBypassed()
        && userInitialCreditsExpiration
            .getNotificationStatus()
            .equals(NotificationStatus.NO_NOTIFICATION_SENT)
        && !(userInitialCreditsExpiration.getExpirationTime().after(now))) {
      expireBillingStatusForUserWorkspaces(user);
      try {
        mailService.alertUserInitialCreditsExpired(user);
        userInitialCreditsExpiration.setNotificationStatus(
            NotificationStatus.EXPIRATION_NOTIFICATION_SENT);
        userDao.save(user);

      } catch (MessagingException e) {
        log.warning(
            String.format(
                "Failed to send initial credits expiration notification for user %s",
                user.getUserId()));
      }
    }
  }

  private void expireBillingStatusForUserWorkspaces(DbUser user) {
    workspaceDao.findAllByCreator(user).stream()
        .filter(
            ws ->
                WorkspaceUtils.isFreeTier(
                    ws.getBillingAccountName(), workbenchConfigProvider.get()))
        .map(DbWorkspace::getWorkspaceId)
        .forEach(id -> workspaceDao.updateBillingStatus(id, BillingStatus.EXPIRED));
  }
}
