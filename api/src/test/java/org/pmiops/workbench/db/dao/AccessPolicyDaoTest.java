package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth8;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.accessmodules.AccessModuleEvaluatorKey;
import org.pmiops.workbench.accessmodules.AccessModuleType;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class AccessPolicyDaoTest {

  @Autowired private AccessPolicyDao accessPolicyDao;
  @Autowired private AccessModuleDao accessModuleDao;

  @Before
  public void setup() {
    accessModuleDao.deleteAll();
    accessPolicyDao.deleteAll();
  }

  @Test
  public void testFindById() {
    assertThat(accessPolicyDao.count()).isEqualTo(0);

    final DbAccessPolicy policy = new DbAccessPolicy();
    policy.setDisplayName("Hand Stamp");

    final DbAccessPolicy saved = accessPolicyDao.save(policy);
    assertThat(saved.getAccessPolicyId()).isNotEqualTo(0);

    Truth8.assertThat(accessPolicyDao.findByAccessPolicyId(saved.getAccessPolicyId()))
        .hasValue(saved);
  }

  @Test
  public void testPolicyWithModules() {
    DbAccessModule module1 = new DbAccessModule();
    module1.setAccessModuleType(AccessModuleType.EXTERNAL_CREDENTIAL);
    module1.setDisplayName("Yellow Belt or Higher");
    module1.setAccessModuleEvaluatorKey(AccessModuleEvaluatorKey.MOODLE);
    module1 = accessModuleDao.save(module1);

    DbAccessModule module2 = new DbAccessModule();
    module2.setDisplayName("2FA Enabled");
    module2.setAccessModuleType(AccessModuleType.AUTHENTICATION);
    module2.setAccessModuleEvaluatorKey(AccessModuleEvaluatorKey.DOCUSIGN);
    module2 = accessModuleDao.save(module2);
    assertThat(accessModuleDao.count()).isEqualTo(2);

    final DbAccessPolicy policy = new DbAccessPolicy();
    policy.setDisplayName("Basic Access Policy");

    module1.setAccessPolicies(Collections.singleton(policy));
    module2.setAccessPolicies(Collections.singleton(policy));
    accessModuleDao.save(ImmutableSet.of(module1, module2));

    policy.setAccessModules(ImmutableSet.of(module1, module2));
    assertThat(policy.getAccessModules()).hasSize(2);


    final DbAccessPolicy updatedPolicy = accessPolicyDao.save(policy);
    assertThat(updatedPolicy.getAccessModules()).hasSize(2);

    module1 = accessModuleDao.findOne(module1.getAccessModuleId());
    assertThat(module1.getAccessPolicies()).containsExactly(policy);

    module2 = accessModuleDao.findOne(module2.getAccessModuleId());
    assertThat(module2.getAccessPolicies()).containsExactly(policy);
  }
}
