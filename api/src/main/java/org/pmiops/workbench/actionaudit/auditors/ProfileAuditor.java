package org.pmiops.workbench.actionaudit.auditors;

import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Profile;

/** Auditor service which handles collecting audit logs for profile-related actions. */
public interface ProfileAuditor {
  /** Fires an audit log event for creating a profile. */
  void fireCreateAction(Profile createdProfile);

  /** Fires an audit log event for updating a profile. */
  void fireUpdateAction(Profile previousProfile, Profile updatedProfile, Agent agent);

  /** Fires an audit log event for deleting a profile. */
  void fireDeleteAction(long userId, String userEmail);

  /** Fires an audit log event for a user login action. */
  void fireLoginAction(DbUser dbUser);
}
