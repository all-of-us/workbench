package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.springframework.data.repository.CrudRepository;

public interface UserAccessTierDao extends CrudRepository<DbUserAccessTier, Long> {
  Optional<DbUserAccessTier> getByUserAndAccessTier(DbUser user, DbAccessTier accessTier);

  List<DbUserAccessTier> getAllByUser(DbUser user);

  List<DbUserAccessTier> getAllByAccessTier(DbAccessTier accessTier);
}
