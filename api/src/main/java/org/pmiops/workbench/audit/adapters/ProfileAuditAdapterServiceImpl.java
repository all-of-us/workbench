package org.pmiops.workbench.audit.adapters;

import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.audit.ActionAuditEvent;
import org.pmiops.workbench.audit.ActionAuditService;
import org.pmiops.workbench.audit.ActionType;
import org.pmiops.workbench.audit.AgentType;
import org.pmiops.workbench.audit.TargetType;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileAuditAdapterServiceImpl implements ProfileAuditAdapterService {

  private static final Logger logger =
      Logger.getLogger(ProfileAuditAdapterServiceImpl.class.getName());

  private final Provider<User> userProvider;
  private final ActionAuditService actionAuditService;
  private final Clock clock;

  @Autowired
  public ProfileAuditAdapterServiceImpl(
      Provider<User> userProvider, ActionAuditService actionAuditService, Clock clock) {
    this.userProvider = userProvider;
    this.actionAuditService = actionAuditService;
    this.clock = clock;
  }

  @Override
  public void fireCreateAction(Profile createdProfile) {}

  @Override
  public void fireUpdateAction(Profile previousProfile, Profile updatedProfile) {}

  // Each user is assumed to have only one profile, but we can't rely on
  // the userProvider if the user is deleted before the profile.
  @Override
  public void fireDeleteAction(long userId, String userEmail) {
    try {
      final ActionAuditEvent deleteProfileEvent =
          new ActionAuditEvent(
              clock.millis(),
              AgentType.USER,
              userId,
              userEmail,
              ActionAuditEvent.newActionId(),
              ActionType.DELETE,
              TargetType.PROFILE,
              null,
              userId,
              null,
              null);
      actionAuditService.send(deleteProfileEvent);
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void logAndSwallow(RuntimeException e) {
    logger.log(Level.WARNING, e, () -> "Exception encountered during audit.");
  }
}
