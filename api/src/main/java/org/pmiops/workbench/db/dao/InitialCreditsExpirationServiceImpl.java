package org.pmiops.workbench.db.dao;

import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.InitialCreditExpirationNotificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InitialCreditsExpirationServiceImpl
    implements InitialCreditsExpirationService {

  private static final Logger log =
      Logger.getLogger(InitialCreditsExpirationServiceImpl.class.getName());
  private final UserDao userDao;
  private final MailService mailService;

  @Autowired
  public InitialCreditsExpirationServiceImpl(UserDao userDao, MailService mailService) {
    this.userDao = userDao;
    this.mailService = mailService;
  }

  @Override
  public void checkCreditsExpirationForUserIDs(List<Long> userIdsList) {
    if(userIdsList != null && !userIdsList.isEmpty()) {
      Timestamp now = new Timestamp(System.currentTimeMillis());
      List<DbUser> users = (List<DbUser>) userDao.findAllById(userIdsList);
      users.forEach(user -> checkCreditsExpirationForUser(user, now));
    }
  }

  private boolean isBypassed(DbUserInitialCreditsExpiration userInitialCreditsExpiration) {
    return userInitialCreditsExpiration.isBypassed();
  }

  private void checkCreditsExpirationForUser(DbUser user, Timestamp now) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (null != userInitialCreditsExpiration) {
      if (!isBypassed(userInitialCreditsExpiration)
          && !(userInitialCreditsExpiration.getExpirationTime().after(now))) {
        try {
          mailService.alertUserInitialCreditsExpired(user);
          userInitialCreditsExpiration.setNotificationStatus(
              InitialCreditExpirationNotificationStatus.EXPIRATION_NOTIFICATION_SENT);
        } catch (MessagingException e) {
          log.warning(
              String.format(
                  "Failed to send initial credits expiration notification for user %s",
                  user.getUserId()));
        }

      }
    }
  }
}
