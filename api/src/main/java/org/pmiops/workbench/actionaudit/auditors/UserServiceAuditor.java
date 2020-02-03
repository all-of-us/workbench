package org.pmiops.workbench.actionaudit.auditors;

import java.time.Instant;
import java.util.Optional;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;

public interface UserServiceAuditor {
  void fireUpdateDataAccessAction(
      DbUser targetUser, DataAccessLevel dataAccessLevel, DataAccessLevel previousDataAccessLevel);

  void fireAdministrativeBypassTime(
      long userId, BypassTimeTargetProperty bypassTimeTargetProperty, Optional<Instant> bypassTime);

  void fireAcknowledgeTermsOfService(DbUser targetUser, Integer termsOfServiceVersion);
}
