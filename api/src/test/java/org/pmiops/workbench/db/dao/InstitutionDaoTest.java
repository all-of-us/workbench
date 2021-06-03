package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbInstitution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@Import({CommonConfig.class})
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionDaoTest extends SpringTest {

  @Autowired InstitutionDao institutionDao;

  private DbInstitution testInst;

  @BeforeEach
  public void setUp() {
    testInst =
        institutionDao.save(
            new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute"));
  }

  @Test
  public void test_save() {
    final DbInstitution toSave =
        new DbInstitution()
            .setShortName("VUMC")
            .setDisplayName("Vanderbilt University Medical Center");
    final DbInstitution saved = institutionDao.save(toSave);
    assertThat(saved).isEqualTo(toSave);
  }

  @Test
  public void test_delete() {
    institutionDao.deleteById(testInst.getInstitutionId());
    DbInstitution dbInstitution = institutionDao.findById(testInst.getInstitutionId()).orElse(null);
    assertThat(dbInstitution).isNull();
    assertThat(institutionDao.findAll()).isEmpty();
  }

  @Test
  public void test_findAll() {
    assertThat(institutionDao.findAll()).containsExactly(testInst);
  }

  @Test
  public void test_findById() {
    DbInstitution dbInstitution = institutionDao.findById(testInst.getInstitutionId()).get();
    assertThat(dbInstitution).isEqualTo(testInst);
  }

  @Test
  public void test_findOneByShortName() {
    assertThat(institutionDao.findOneByShortName("Broad")).hasValue(testInst);
    assertThat(institutionDao.findOneByShortName("Verily")).isEmpty();
  }

  @Test
  public void test_findOneByDisplayName() {
    assertThat(institutionDao.findOneByDisplayName("The Broad Institute")).hasValue(testInst);
    assertThat(institutionDao.findOneByDisplayName("The National Institutes of Health")).isEmpty();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_shortNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setDisplayName("so long");
    institutionDao.save(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_displayNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setShortName("VUMC");
    institutionDao.save(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_uniqueShortNameRequired() {
    final DbInstitution snowflake1 =
        new DbInstitution().setShortName("unique?").setDisplayName("We are all individuals");
    institutionDao.save(snowflake1);

    final DbInstitution snowflake2 =
        new DbInstitution().setShortName("unique?").setDisplayName("I'm not");
    institutionDao.save(snowflake2);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_uniqueDisplayNameRequired() {
    final DbInstitution snowflake1 =
        new DbInstitution().setShortName("Inst1").setDisplayName("Not Unique");
    institutionDao.save(snowflake1);

    final DbInstitution snowflake2 =
        new DbInstitution().setShortName("Inst2").setDisplayName("Not Unique");
    institutionDao.save(snowflake2);
  }
}
