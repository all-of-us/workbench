package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionDaoTest {
  @Autowired InstitutionDao institutionDao;

  @Test
  public void testDao() {
    DbInstitution testInst = new DbInstitution("Broad", "The Broad Institute");
    testInst = institutionDao.save(testInst);
    assertThat(institutionDao.findOneByShortName("Broad")).hasValue(testInst);
    assertThat(institutionDao.findAll()).hasSize(1);

    // update existing entity, don't change size

    testInst.setShortName("Verily");
    testInst = institutionDao.save(testInst);
    assertThat(institutionDao.findAll()).hasSize(1);
    assertThat(institutionDao.findOneByShortName("Verily")).hasValue(testInst);

    testInst.setDisplayName("Yea, Verily");
    testInst = institutionDao.save(testInst);
    assertThat(institutionDao.findAll()).hasSize(1);
    assertThat(institutionDao.findOneByShortName("Verily")).hasValue(testInst);

    institutionDao.save(new DbInstitution("VUMC", "Vanderbilt"));
    assertThat(institutionDao.findAll()).hasSize(2);

    institutionDao.delete(institutionDao.findOneByShortName("Verily").get());
    assertThat(institutionDao.findAll()).hasSize(1);

    assertThat(institutionDao.findOneByShortName("404 Institute Not Found")).isEmpty();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_idRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setDisplayName("so long");
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
  public void test_displayNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setShortName("VUMC");
    institutionDao.save(testInst);
  }
}
