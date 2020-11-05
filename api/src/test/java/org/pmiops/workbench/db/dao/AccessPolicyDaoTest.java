package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth8;
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

  @Autowired
  private AccessPolicyDao accessPolicyDao;

  @Test
  public void testFindById() {
    assertThat(accessPolicyDao.count()).isEqualTo(0);

    final DbAccessPolicy policy = new DbAccessPolicy();
    policy.setDisplayName("Hand Stamp");

    final DbAccessPolicy saved = accessPolicyDao.save(policy);
    assertThat(saved.getAccessPolicyId()).isNotEqualTo(0);

    Truth8.assertThat(accessPolicyDao.findByAccessPolicyId(saved.getAccessPolicyId())).hasValue(saved);
  }

}
