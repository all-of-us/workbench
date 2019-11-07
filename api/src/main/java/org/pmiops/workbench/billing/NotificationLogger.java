package org.pmiops.workbench.billing;

import org.pmiops.workbench.db.model.DbUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationLogger implements NotificationService {

  private static final Logger logger = LoggerFactory.getLogger(BillingProjectBufferService.class);

  @Override
  public void alertUser(DbUser user, String msg) {
    logger.info("\nTO: " + user.getEmail() + " MSG: " + msg);
  }
}
