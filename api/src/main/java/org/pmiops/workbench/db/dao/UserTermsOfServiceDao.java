package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.springframework.data.repository.CrudRepository;

public interface UserTermsOfServiceDao extends CrudRepository<DbUserTermsOfService, Long> {
  Optional<DbUserTermsOfService> findFirstByUserIdOrderByTosVersionDesc(long userId);
}
