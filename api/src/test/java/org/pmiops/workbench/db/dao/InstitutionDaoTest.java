package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionDaoTest {

  @Autowired InstitutionDao institutionDao;

  private DbInstitution institutionWithoutEmailPatterns;
  private DbInstitution institutionWithEmailPatterns;
  private Set<DbInstitutionEmailAddress> emailAddresses;
  private Set<DbInstitutionEmailDomain> emailDomains;

  @Before
  public void setUp() {
    institutionWithoutEmailPatterns =
        institutionDao.save(
            new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute"));

    emailAddresses =
        Sets.newHashSet(new DbInstitutionEmailAddress().setEmailAddress("broad@example.com"));
    emailDomains = Sets.newHashSet(new DbInstitutionEmailDomain().setEmailDomain("broad.org"));

    institutionWithEmailPatterns =
        institutionDao.save(
            new DbInstitution()
                .setShortName("Broad1")
                .setDisplayName("The Broad Institute")
                .setEmailAddresses(emailAddresses)
                .setEmailDomains(emailDomains));
  }

  @Test
  public void test_save() {
    final DbInstitution toSaveWithEmailPatterns =
        new DbInstitution()
            .setShortName("Vanderbilt")
            .setDisplayName("Vanderbilt University")
            .setEmailDomains(emailDomains)
            .setEmailAddresses(emailAddresses);
    final DbInstitution savedWithEmailPatterns = institutionDao.save(toSaveWithEmailPatterns);
    assertThat(savedWithEmailPatterns).isEqualTo(toSaveWithEmailPatterns);
    assertThat(savedWithEmailPatterns.getEmailDomains()).isEqualTo(emailDomains);
    assertThat(savedWithEmailPatterns.getEmailAddresses()).isEqualTo(emailAddresses);

    final DbInstitution toSaveWithoutEmailPatterns =
        new DbInstitution()
            .setShortName("VUMC")
            .setDisplayName("Vanderbilt University Medical Center");
    final DbInstitution savedWithoutEmailPatterns = institutionDao.save(toSaveWithoutEmailPatterns);
    assertThat(savedWithoutEmailPatterns).isEqualTo(toSaveWithoutEmailPatterns);
    assertThat(savedWithoutEmailPatterns.getEmailDomains()).isEmpty();
    assertThat(savedWithoutEmailPatterns.getEmailAddresses()).isEmpty();
  }

  @Test
  public void test_delete() {
    institutionDao.delete(institutionWithoutEmailPatterns.getInstitutionId());
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithoutEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isNull();
    assertThat(institutionDao.findAll()).containsExactly(institutionWithEmailPatterns);
  }

  @Test
  public void test_findAll() {
    assertThat(institutionDao.findAll())
        .containsExactly(
            institutionWithoutEmailPatterns, institutionWithEmailPatterns);
  }

  @Test
  public void test_findOne() {
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithoutEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithoutEmailPatterns);
    assertThat(dbInstitution.getEmailDomains()).isEmpty();
    assertThat(dbInstitution.getEmailAddresses()).isEmpty();

    dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailDomains()).isEqualTo(emailDomains);
    assertThat(dbInstitution.getEmailAddresses()).isEqualTo(emailAddresses);
  }

  @Test
  public void test_findOneByShortName() {
    assertThat(institutionDao.findOneByShortName("Broad"))
        .hasValue(institutionWithoutEmailPatterns);
    assertThat(institutionDao.findOneByShortName("Broad1"))
        .hasValue(institutionWithEmailPatterns);
    assertThat(institutionDao.findOneByShortName("Verily")).isEmpty();
  }

  @Test
  public void test_updateAllNewEmailAddresses() {
    Set<DbInstitutionEmailAddress> newEmailAddresses =
        Sets.newHashSet(
            new DbInstitutionEmailAddress().setEmailAddress("broad1@example.com"),
            new DbInstitutionEmailAddress().setEmailAddress("broad2@example.com"));
    institutionWithEmailPatterns.setEmailAddresses(newEmailAddresses);
    institutionDao.save(institutionWithEmailPatterns);

    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailAddresses()).containsExactlyElementsIn(newEmailAddresses);
  }

  @Test
  public void test_updateWithExistingEmailAddresses() {
    Set<DbInstitutionEmailAddress> newEmailAddresses =
        Sets.newHashSet(
            new DbInstitutionEmailAddress().setEmailAddress("broad@example.com"),
            new DbInstitutionEmailAddress().setEmailAddress("broad2@example.com"));
    institutionWithEmailPatterns.setEmailAddresses(newEmailAddresses);
    institutionDao.save(institutionWithEmailPatterns);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailAddresses()).containsExactlyElementsIn(newEmailAddresses);
  }

  @Test
  public void test_updateRemoveAllEmailAddresses() {
    Set<DbInstitutionEmailAddress> newEmailAddresses = Sets.newHashSet();
    institutionWithEmailPatterns.setEmailAddresses(newEmailAddresses);
    institutionDao.save(institutionWithEmailPatterns);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailAddresses()).isEmpty();
  }

  @Test
  public void test_updateAllNewEmailDomains() {
    Set<DbInstitutionEmailDomain> newEmailDomains =
        Sets.newHashSet(
            new DbInstitutionEmailDomain().setEmailDomain("wpi.edu"),
            new DbInstitutionEmailDomain().setEmailDomain("mit.edu"));
    institutionWithEmailPatterns.setEmailDomains(newEmailDomains);
    institutionDao.save(institutionWithEmailPatterns);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailDomains()).containsExactlyElementsIn(newEmailDomains);
  }

  @Test
  public void test_updateWithExistingEmailDomains() {
    Set<DbInstitutionEmailDomain> newEmailDomains =
        Sets.newHashSet(
            new DbInstitutionEmailDomain().setEmailDomain("broad.org"),
            new DbInstitutionEmailDomain().setEmailDomain("mit.edu"));
    institutionWithEmailPatterns.setEmailDomains(newEmailDomains);
    institutionDao.save(institutionWithEmailPatterns);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailDomains()).containsExactlyElementsIn(newEmailDomains);
  }

  @Test
  public void test_updateRemoveAllEmailDomains() {
    Set<DbInstitutionEmailDomain> newEmailDomains = Sets.newHashSet();
    institutionWithEmailPatterns.setEmailDomains(newEmailDomains);
    institutionDao.save(institutionWithEmailPatterns);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailPatterns.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailPatterns);
    assertThat(dbInstitution.getEmailDomains()).isEmpty();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_idRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setDisplayName("so long");
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
  public void test_displayNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setShortName("VUMC");
    institutionDao.save(testInst);
  }
}
