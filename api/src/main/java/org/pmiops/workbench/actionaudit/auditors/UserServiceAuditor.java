package org.pmiops.workbench.actionaudit.auditors;

import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;

public interface UserServiceAuditor {
  void fireUpdateDataAccessAction(
      DbUser targetUser,
      DataAccessLevel previousDataAccessLevel,
      DataAccessLevel newDataAccessLevel,
      Agent agent);

  void fireAdministrativeBypassTime(
      long userId,
      BypassTimeTargetProperty bypassTimeTargetProperty,
      Optional<Instant> previousBypassTime,
      Optional<Instant> newBypassTime);

  void fireAcknowledgeTermsOfService(DbUser targetUser, Integer termsOfServiceVersion);

  void fireFreeTierDollarQuotaAction(
      Long targetUserId, @Nullable Double previousDollarQuota, @Nullable Double newDollarQuota);
}
