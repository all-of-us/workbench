package org.pmiops.workbench.initialcredits;

import java.sql.Timestamp;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbUser;

public interface InitialCreditsExpirationService {
  Optional<Timestamp> getCreditsExpiration(DbUser user);
}
