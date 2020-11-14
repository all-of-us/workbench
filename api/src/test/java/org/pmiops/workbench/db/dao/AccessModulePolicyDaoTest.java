package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.accessmodules.AccessModuleEvaluatorKey;
import org.pmiops.workbench.accessmodules.AccessModuleType;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModulePolicy;
import org.pmiops.workbench.db.model.DbAccessPolicy;
import org.pmiops.workbench.db.model.embedded.DbAccessPolicyModuleKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Basically a test of how ManyToMany join table entities work (assuming they need to
 * exist at all). https://www.baeldung.com/jpa-many-to-many
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class AccessModulePolicyDaoTest {
  private static final String MODULE_1_NAME = "Module 1";

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessModulePolicyDao accessModulePolicyDao;
  @Autowired private AccessPolicyDao accessPolicyDao;

  @Test
  public void testInsertion() {
    final DbAccessModule accessModule = createAccessModule();
    final DbAccessPolicy accessPolicy = createAccessPolicy();

    accessPolicy.setAccessModules(Collections.singleton(accessModule));

    final DbAccessPolicy updatedAccessPolicy = accessPolicyDao.save(accessPolicy);
    assertThat(accessPolicyDao.count()).isEqualTo(1);
    assertThat(updatedAccessPolicy.getAccessModules()).hasSize(1);
    assertThat(updatedAccessPolicy.getAccessModules().stream().findAny()
        .get().getDisplayName()).isEqualTo(MODULE_1_NAME);

    assertThat(accessModulePolicyDao.count()).isEqualTo(1);

    final List<DbAccessModulePolicy> persistedModules = accessModulePolicyDao.findDbAccessModulePoliciesByAccessPolicy(accessPolicy);
    assertThat(persistedModules).hasSize(1);
    assertThat(persistedModules.get(0).getAccessModule().getDisplayName()).isEqualTo(MODULE_1_NAME);
  }

  public DbAccessPolicy createAccessPolicy() {
    DbAccessPolicy accessPolicy = new DbAccessPolicy();
    accessPolicy.setDisplayName("Policy 1");
    accessPolicy = accessPolicyDao.save(accessPolicy);
    return accessPolicy;
  }

  public DbAccessModule createAccessModule() {
    DbAccessModule accessModule = new DbAccessModule();
    accessModule.setDisplayName(MODULE_1_NAME);
    accessModule.setAccessModuleEvaluatorKey(AccessModuleEvaluatorKey.DOCUSIGN);
    accessModule.setAccessModuleType(AccessModuleType.EARLY_ACCESS);
    accessModule = accessModuleDao.save(accessModule);
    assertThat(accessModuleDao.count()).isEqualTo(1);
    return accessModule;
  }
}
