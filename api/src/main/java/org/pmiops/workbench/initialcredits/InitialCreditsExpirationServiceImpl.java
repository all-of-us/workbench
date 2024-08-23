package org.pmiops.workbench.initialcredits;

import java.sql.Timestamp;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.springframework.stereotype.Service;

@Service
public class InitialCreditsExpirationServiceImpl implements InitialCreditsExpirationService {

  @Override
  public Optional<Timestamp> getCreditsExpiration(DbUser user) {
    return Optional.ofNullable(user.getUserInitialCreditsExpiration())
        .filter(exp -> !exp.isBypassed()) // If the expiration is bypassed, return empty.
        // TODO RW-13502 filter on institutional bypass as well, maybe something like
        // .filter(() -> institutionService.shouldBypassForCreditsExpiration(user))
        .map(DbUserInitialCreditsExpiration::getExpirationTime);
  }
}
