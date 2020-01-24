package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class InstitutionDaoTest {
  @Autowired InstitutionDao institutionDao;

  @Test
  public void testDao() {
    final DbInstitution testInst = new DbInstitution("Broad", "The Broad Institute");
    institutionDao.save(testInst);
    assertThat(institutionDao.findOneByApiId("Broad")).isEqualTo(testInst);
    assertThat(institutionDao.findAll()).hasSize(1);

    // update existing entity, don't change size

    testInst.setApiId("Verily");
    institutionDao.save(testInst);
    assertThat(institutionDao.findAll()).hasSize(1);
    assertThat(institutionDao.findOneByApiId("Verily")).isEqualTo(testInst);

    testInst.setLongName("Yea, Verily");
    institutionDao.save(testInst);
    assertThat(institutionDao.findAll()).hasSize(1);
    assertThat(institutionDao.findOneByApiId("Verily")).isEqualTo(testInst);

    final DbInstitution otherInst = new DbInstitution("VUMC", "Vanderbilt");
    institutionDao.save(otherInst);
    assertThat(institutionDao.findAll()).hasSize(2);

    institutionDao.delete(institutionDao.findOneByApiId("Verily"));
    assertThat(institutionDao.findAll()).hasSize(1);

    assertThat(institutionDao.findOneByApiId("404 Institute Not Found")).isNull();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_idRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setLongName("so long");
    institutionDao.save(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_uniqueIdRequired() {
    final DbInstitution snowflake1 = new DbInstitution("unique?", "We are all individuals");
    institutionDao.save(snowflake1);

    final DbInstitution snowflake2 = new DbInstitution("unique?", "I'm not");
    institutionDao.save(snowflake2);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_longNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setApiId("VUMC");
    institutionDao.save(testInst);
  }
}
