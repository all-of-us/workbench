package org.pmiops.workbench.access;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.stereotype.Service;

@Service
public interface AccessTierService {
  List<DbAccessTier> getAccessTiersForUser(DbUser user);
}
