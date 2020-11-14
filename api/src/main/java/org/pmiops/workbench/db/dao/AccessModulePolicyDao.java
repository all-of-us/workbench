package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModulePolicy;
import org.pmiops.workbench.db.model.DbAccessPolicy;
import org.pmiops.workbench.db.model.embedded.DbAccessPolicyModuleKey;
import org.springframework.data.repository.CrudRepository;

public interface AccessModulePolicyDao extends CrudRepository<DbAccessModulePolicy, DbAccessPolicyModuleKey> {
  List<DbAccessModulePolicy> findDbAccessModulePoliciesByAccessModule(DbAccessModule accessModule);
  List<DbAccessModulePolicy> findDbAccessModulePoliciesByAccessPolicy(DbAccessPolicy accessPolicy);
}
