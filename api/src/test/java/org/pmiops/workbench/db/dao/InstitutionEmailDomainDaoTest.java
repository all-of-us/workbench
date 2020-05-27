package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailDomainDaoTest {

  @Autowired InstitutionDao institutionDao;
  @Autowired InstitutionEmailDomainDao institutionEmailDomainDao;

  private DbInstitution testInst;

  @Before
  public void setUp() {
    testInst =
        institutionDao.save(
            new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute"));
  }

  @Test
  public void test_getByInstitution_empty() {
    assertThat(institutionEmailDomainDao.getByInstitutionId(testInst.getInstitutionId())).isEmpty();
    assertThat(institutionEmailDomainDao.count()).isEqualTo(0L);
  }

  @Test
  public void test_getByInstitution_multiple() {
    final DbInstitutionEmailDomain one =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.com")
                .setInstitutionId(testInst.getInstitutionId()));
    final DbInstitutionEmailDomain two =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.net")
                .setInstitutionId(testInst.getInstitutionId()));

    assertThat(institutionEmailDomainDao.getByInstitutionId(testInst.getInstitutionId()))
        .containsExactly(one, two);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(2L);
  }

  @Test
  public void test_getByInstitution_multipleInsts() {
    final DbInstitutionEmailDomain one =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.com")
                .setInstitutionId(testInst.getInstitutionId()));

    final DbInstitution otherInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    final DbInstitutionEmailDomain two =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.net")
                .setInstitutionId(otherInst.getInstitutionId()));

    assertThat(institutionEmailDomainDao.getByInstitutionId(testInst.getInstitutionId()))
        .containsExactly(one);
    assertThat(institutionEmailDomainDao.getByInstitutionId(otherInst.getInstitutionId()))
        .containsExactly(two);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(2L);
  }

  @Test
  public void test_deleteByInstitution_empty() {
    assertThat(institutionEmailDomainDao.deleteByInstitutionId(testInst.getInstitutionId()))
        .isEqualTo(0L);
  }

  @Test
  public void test_deleteByInstitution_multiple() {
    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.com")
            .setInstitutionId(testInst.getInstitutionId()));
    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.net")
            .setInstitutionId(testInst.getInstitutionId()));

    assertThat(institutionEmailDomainDao.deleteByInstitutionId(testInst.getInstitutionId()))
        .isEqualTo(2L);
    assertThat(institutionEmailDomainDao.deleteByInstitutionId(testInst.getInstitutionId()))
        .isEqualTo(0L);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(0L);
  }

  @Test
  public void test_deleteByInstitution_multipleInsts() {
    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.com")
            .setInstitutionId(testInst.getInstitutionId()));

    final DbInstitution otherInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.net")
            .setInstitutionId(otherInst.getInstitutionId()));

    assertThat(institutionEmailDomainDao.deleteByInstitutionId(testInst.getInstitutionId()))
        .isEqualTo(1L);
    assertThat(institutionEmailDomainDao.deleteByInstitutionId(otherInst.getInstitutionId()))
        .isEqualTo(1L);
    assertThat(institutionEmailDomainDao.deleteByInstitutionId(testInst.getInstitutionId()))
        .isEqualTo(0L);
    assertThat(institutionEmailDomainDao.deleteByInstitutionId(otherInst.getInstitutionId()))
        .isEqualTo(0L);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(0L);
  }
}
