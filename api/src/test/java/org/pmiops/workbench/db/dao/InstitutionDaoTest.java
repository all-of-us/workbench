package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
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

  private DbInstitution testInst = new DbInstitution("Broad", "The Broad Institute");

  @Before
  public void setUp() {
    testInst = institutionDao.save(testInst);
  }

  @Test
  public void test_save() {
    assertThat(institutionDao.findAll()).hasSize(1);
    institutionDao.save(new DbInstitution("VUMC", "Vanderbilt"));
    assertThat(institutionDao.findAll()).hasSize(2);
  }

  @Test
  public void test_delete() {
    assertThat(institutionDao.findAll()).hasSize(1);
    institutionDao.delete(institutionDao.findOneByShortName("Broad").get());
    assertThat(institutionDao.findAll()).hasSize(0);
  }

  @Test
  public void test_findAll() {
    assertThat(institutionDao.findAll()).containsExactlyElementsIn(ImmutableList.of(testInst));

    DbInstitution otherInst = new DbInstitution("VUMC", "Vanderbilt");
    otherInst = institutionDao.save(otherInst);
    assertThat(institutionDao.findAll())
        .containsExactlyElementsIn(ImmutableList.of(testInst, otherInst));

    institutionDao.delete(institutionDao.findOneByShortName("Broad").get());
    assertThat(institutionDao.findAll()).containsExactlyElementsIn(ImmutableList.of(otherInst));
  }

  @Test
  public void test_findOneByShortName() {
    assertThat(institutionDao.findOneByShortName("Broad")).hasValue(testInst);
    assertThat(institutionDao.findOneByShortName("Verily")).isEmpty();

    DbInstitution otherInst = new DbInstitution("Verily", "An Alphabet Company");
    otherInst = institutionDao.save(otherInst);
    assertThat(institutionDao.findOneByShortName("Verily")).hasValue(otherInst);
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
