package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailDomainDaoTest {

  @Autowired InstitutionDao institutionDao;
  @Autowired AccessTierDao accessTierDao;
  @Autowired InstitutionEmailDomainDao institutionEmailDomainDao;

  private DbInstitution testInst;
  private DbAccessTier registeredTier;

  @BeforeEach
  public void setUp() {
    testInst =
        institutionDao.save(
            new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute"));
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
  }

  @Test
  public void test_getByInstitution_empty() {
    assertThat(institutionEmailDomainDao.getByInstitution(testInst)).isEmpty();
    assertThat(institutionEmailDomainDao.count()).isEqualTo(0L);
  }

  @Test
  public void test_getByInstitution_multiple() {
    final DbInstitutionEmailDomain one =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.com")
                .setAccessTier(registeredTier)
                .setInstitution(testInst));
    final DbInstitutionEmailDomain two =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.net")
                .setAccessTier(registeredTier)
                .setInstitution(testInst));

    assertThat(institutionEmailDomainDao.getByInstitution(testInst)).containsExactly(one, two);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(2L);
  }

  @Test
  public void test_getByInstitution_multipleInsts() {
    final DbInstitutionEmailDomain one =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.com")
                .setAccessTier(registeredTier)
                .setInstitution(testInst));

    final DbInstitution otherInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    final DbInstitutionEmailDomain two =
        institutionEmailDomainDao.save(
            new DbInstitutionEmailDomain()
                .setEmailDomain("domain.net")
                .setAccessTier(registeredTier)
                .setInstitution(otherInst));

    assertThat(institutionEmailDomainDao.getByInstitution(testInst)).containsExactly(one);
    assertThat(institutionEmailDomainDao.getByInstitution(otherInst)).containsExactly(two);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(2L);
  }

  @Test
  public void test_deleteByInstitution_empty() {
    assertThat(institutionEmailDomainDao.deleteByInstitution(testInst)).isEqualTo(0L);
  }

  @Test
  public void test_deleteByInstitution_multiple() {
    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.com")
            .setAccessTier(registeredTier)
            .setInstitution(testInst));
    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.net")
            .setAccessTier(registeredTier)
            .setInstitution(testInst));

    assertThat(institutionEmailDomainDao.deleteByInstitution(testInst)).isEqualTo(2L);
    assertThat(institutionEmailDomainDao.deleteByInstitution(testInst)).isEqualTo(0L);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(0L);
  }

  @Test
  public void test_deleteByInstitution_multipleInsts() {
    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.com")
            .setAccessTier(registeredTier)
            .setInstitution(testInst));

    final DbInstitution otherInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    institutionEmailDomainDao.save(
        new DbInstitutionEmailDomain()
            .setEmailDomain("domain.net")
            .setAccessTier(registeredTier)
            .setInstitution(otherInst));

    assertThat(institutionEmailDomainDao.deleteByInstitution(testInst)).isEqualTo(1L);
    assertThat(institutionEmailDomainDao.deleteByInstitution(otherInst)).isEqualTo(1L);
    assertThat(institutionEmailDomainDao.deleteByInstitution(testInst)).isEqualTo(0L);
    assertThat(institutionEmailDomainDao.deleteByInstitution(otherInst)).isEqualTo(0L);
    assertThat(institutionEmailDomainDao.count()).isEqualTo(0L);
  }
}
