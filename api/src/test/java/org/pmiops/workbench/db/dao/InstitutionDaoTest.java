package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private DbInstitution institutionNoEmailAddressesOrDomains;
  private DbInstitution institutionWithEmailAddressesAndDomains;
  private Set<DbInstitutionEmailAddress> emailAddresses;
  private Set<DbInstitutionEmailDomain> emailDomains;

  @Before
  public void setUp() {
    institutionNoEmailAddressesOrDomains =
        institutionDao.save(
            new DbInstitution().shortName("Broad").displayName("The Broad Institute"));

    emailAddresses =
        Stream.of(new DbInstitutionEmailAddress().setEmailAddress("emailAddress"))
            .collect(Collectors.toCollection(HashSet::new));
    emailDomains =
        Stream.of(new DbInstitutionEmailDomain().setEmailDomain("emailDomain"))
            .collect(Collectors.toCollection(HashSet::new));
    institutionWithEmailAddressesAndDomains =
        institutionDao.save(
            new DbInstitution()
                .shortName("Broad1")
                .displayName("The Broad Institute")
                .emailAddresses(emailAddresses)
                .emailDomains(emailDomains));
  }

  @Test
  public void test_save() {
    // no need to call save since we have a setup method.
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionNoEmailAddressesOrDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionNoEmailAddressesOrDomains);
    assertThat(dbInstitution.getEmailDomains()).isEmpty();
    assertThat(dbInstitution.getEmailAddresses()).isEmpty();

    dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
    assertThat(dbInstitution.getEmailDomains()).isEqualTo(emailDomains);
    assertThat(dbInstitution.getEmailAddresses()).isEqualTo(emailAddresses);
  }

  @Test
  public void test_delete() {
    institutionDao.delete(institutionNoEmailAddressesOrDomains.getInstitutionId());
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionNoEmailAddressesOrDomains.getInstitutionId());
    assertThat(dbInstitution).isNull();
    assertThat(institutionDao.findAll()).containsExactly(institutionWithEmailAddressesAndDomains);
  }

  @Test
  public void test_findAll() {
    assertThat(institutionDao.findAll())
        .containsExactly(
            institutionNoEmailAddressesOrDomains, institutionWithEmailAddressesAndDomains);
  }

  @Test
  public void test_findOneByShortName() {
    assertThat(institutionDao.findOneByShortName("Broad"))
        .hasValue(institutionNoEmailAddressesOrDomains);
    assertThat(institutionDao.findOneByShortName("Broad1"))
        .hasValue(institutionWithEmailAddressesAndDomains);
    assertThat(institutionDao.findOneByShortName("Verily")).isEmpty();
  }

  @Test
  public void test_updateAllNewEmailAddresses() {
    Set<DbInstitutionEmailAddress> newEmailAddresses =
        Sets.newHashSet(
            new DbInstitutionEmailAddress().setEmailAddress("emailAddress1"),
            new DbInstitutionEmailAddress().setEmailAddress("emailAddress2"));
    institutionWithEmailAddressesAndDomains.setEmailAddresses(newEmailAddresses);
    institutionDao.save(institutionWithEmailAddressesAndDomains);

    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
    assertThat(dbInstitution.getEmailAddresses()).containsExactlyElementsIn(newEmailAddresses);
  }

  @Test
  public void test_updateWithExistingEmailAddresses() {
    Set<DbInstitutionEmailAddress> newEmailAddresses =
        Sets.newHashSet(
            new DbInstitutionEmailAddress().setEmailAddress("emailAddress"),
            new DbInstitutionEmailAddress().setEmailAddress("emailAddress2"));
    institutionWithEmailAddressesAndDomains.setEmailAddresses(newEmailAddresses);
    institutionDao.save(institutionWithEmailAddressesAndDomains);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
    assertThat(dbInstitution.getEmailAddresses()).containsExactlyElementsIn(newEmailAddresses);
  }

  @Test
  public void test_updateRemoveAllEmailAddresses() {
    Set<DbInstitutionEmailAddress> newEmailAddresses = Sets.newHashSet();
    institutionWithEmailAddressesAndDomains.setEmailAddresses(newEmailAddresses);
    institutionDao.save(institutionWithEmailAddressesAndDomains);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
    assertThat(dbInstitution.getEmailAddresses()).isEmpty();
  }

  @Test
  public void test_updateAllNewEmailDomains() {
    Set<DbInstitutionEmailDomain> newEmailDomains =
        Sets.newHashSet(
            new DbInstitutionEmailDomain().setEmailDomain("emailDomain1"),
            new DbInstitutionEmailDomain().setEmailDomain("emailDomain2"));
    institutionWithEmailAddressesAndDomains.setEmailDomains(newEmailDomains);
    institutionDao.save(institutionWithEmailAddressesAndDomains);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
    assertThat(dbInstitution.getEmailDomains()).containsExactlyElementsIn(newEmailDomains);
  }

  @Test
  public void test_updateWithExistingEmailDomains() {
    Set<DbInstitutionEmailDomain> newEmailDomains =
        Sets.newHashSet(
            new DbInstitutionEmailDomain().setEmailDomain("emailDomain"),
            new DbInstitutionEmailDomain().setEmailDomain("emailDomain2"));
    institutionWithEmailAddressesAndDomains.setEmailDomains(newEmailDomains);
    institutionDao.save(institutionWithEmailAddressesAndDomains);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
    assertThat(dbInstitution.getEmailDomains()).containsExactlyElementsIn(newEmailDomains);
  }

  @Test
  public void test_updateRemoveAllEmailDomains() {
    Set<DbInstitutionEmailDomain> newEmailDomains = Sets.newHashSet();
    institutionWithEmailAddressesAndDomains.setEmailDomains(newEmailDomains);
    institutionDao.save(institutionWithEmailAddressesAndDomains);
    DbInstitution dbInstitution =
        institutionDao.findOne(institutionWithEmailAddressesAndDomains.getInstitutionId());
    assertThat(dbInstitution).isEqualTo(institutionWithEmailAddressesAndDomains);
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
        new DbInstitution().shortName("unique?").displayName("We are all individuals");
    institutionDao.save(snowflake1);

    final DbInstitution snowflake2 =
        new DbInstitution().shortName("unique?").displayName("I'm not");
    institutionDao.save(snowflake2);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_displayNameRequired() {
    final DbInstitution testInst = new DbInstitution();
    testInst.setShortName("VUMC");
    institutionDao.save(testInst);
  }
}
