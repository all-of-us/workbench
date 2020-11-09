package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessPolicy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface AccessPolicyDao extends CrudRepository<DbAccessPolicy, Long> {
  Optional<DbAccessPolicy> findByAccessPolicyId(long id);

  @Query(
      value =
          "SELECT "
              + "  ap.access_policy_id, "
              + "  ap.display_name "
              + "FROM access_policy ap "
              + "INNER JOIN access_policy_module apm ON ap.access_policy_id = apm.access_policy_id "
              + "INNER JOIN access_module am ON apm.access_module_id = am.access_module_id "
              + "WHERE am.access_module_id = :accessModuleId "
              + "ORDER BY ap.access_policy_id",
      nativeQuery = true)
  List<DbAccessPolicy> findAllByAccessModuleId(@Param("accessModuleId") long dbAccessModuleId);

  default List<DbAccessPolicy> findAllByAccessModule(DbAccessModule dbAccessModule) {
    return findAllByAccessModuleId(dbAccessModule.getAccessModuleId());
  }
}
