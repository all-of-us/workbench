package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.accessmodules.AccessModuleEvaluatorKey;
import org.pmiops.workbench.accessmodules.AccessModuleType;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class AccessModuleDaoTest {

  @Autowired private AccessModuleDao accessModuleDao;

  @Test
  public void testFindByAccessModuleId() {
    assertThat(accessModuleDao.count()).isEqualTo(0);

    final DbAccessModule module = new DbAccessModule();
    module.setDisplayName("Hand Stamp");
    module.setAccessModuleEvaluatorKey(AccessModuleEvaluatorKey.MOODLE);
    module.setAccessModuleType(AccessModuleType.EXTERNAL_CREDENTIAL);
    final DbAccessModule saved = accessModuleDao.save(module);
    assertThat(saved.getAccessModuleId()).isNotEqualTo(0);

    assertThat(accessModuleDao.findByAccessModuleId(saved.getAccessModuleId())).hasValue(saved);
  }
}
