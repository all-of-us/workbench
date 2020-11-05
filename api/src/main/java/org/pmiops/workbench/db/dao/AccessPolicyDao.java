package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessPolicy;
import org.springframework.data.repository.CrudRepository;

public interface AccessPolicyDao extends CrudRepository<DbAccessPolicy, Long> {
  Optional<DbAccessPolicy> findByAccessPolicyId(long id);
}
