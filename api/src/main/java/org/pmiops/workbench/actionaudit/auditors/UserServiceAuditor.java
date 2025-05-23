package org.pmiops.workbench.actionaudit.auditors;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;

public interface UserServiceAuditor {
  void fireUpdateAccessTiersAction(
      DbUser targetUser,
      List<DbAccessTier> previousAccessTiers,
      List<DbAccessTier> newAccessTiers,
      Agent agent);

  void fireAdministrativeBypassTime(
      long userId,
      BypassTimeTargetProperty bypassTimeTargetProperty,
      Optional<Instant> previousBypassTime,
      Optional<Instant> newBypassTime);

  void fireAcknowledgeTermsOfService(DbUser targetUser, Integer termsOfServiceVersion);

  void fireSetInitialCreditsOverride(
      Long targetUserId, @Nullable Double previousDollarQuota, @Nullable Double newDollarQuota);
}
