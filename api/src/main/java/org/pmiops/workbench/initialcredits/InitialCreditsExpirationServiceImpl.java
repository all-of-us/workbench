package org.pmiops.workbench.initialcredits;

import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserInitialCreditsExpirationDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.InitialCreditExpirationNotificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InitialCreditsExpirationServiceImpl implements InitialCreditsExpirationService {

  private static final Logger log =
      Logger.getLogger(InitialCreditsExpirationServiceImpl.class.getName());
  private final UserDao userDao;
  private final UserInitialCreditsExpirationDao userInitialCreditsExpirationDao;
  private final MailService mailService;
  private final Clock clock;

  @Autowired
  public InitialCreditsExpirationServiceImpl(
      UserDao userDao,
      UserInitialCreditsExpirationDao userInitialCreditsExpirationDao,
      MailService mailService,
      Clock clock) {
    this.userDao = userDao;
    this.userInitialCreditsExpirationDao = userInitialCreditsExpirationDao;
    this.mailService = mailService;
    this.clock = clock;
  }

  @Override
  public void checkCreditsExpirationForUserIDs(List<Long> userIdsList) {
    if (userIdsList != null && !userIdsList.isEmpty()) {
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      List<DbUser> users = (List<DbUser>) userDao.findAllById(userIdsList);
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
            .equals(InitialCreditExpirationNotificationStatus.NO_NOTIFICATION_SENT)
        && !(userInitialCreditsExpiration.getExpirationTime().after(now))) {
      try {
        mailService.alertUserInitialCreditsExpired(user);
        userInitialCreditsExpiration.setNotificationStatus(
            InitialCreditExpirationNotificationStatus.EXPIRATION_NOTIFICATION_SENT);
        userInitialCreditsExpirationDao.save(userInitialCreditsExpiration);
      } catch (MessagingException e) {
        log.warning(
            String.format(
                "Failed to send initial credits expiration notification for user %s",
                user.getUserId()));
      }
    }
  }
}
