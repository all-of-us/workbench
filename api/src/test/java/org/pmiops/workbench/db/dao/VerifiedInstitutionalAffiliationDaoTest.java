package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class VerifiedInstitutionalAffiliationDaoTest {
  @Autowired VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired InstitutionDao institutionDao;
  @Autowired UserDao userDao;

  private DbUser testUser = new DbUser();
  private DbInstitution testInst =
      new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

  @Before
  public void setUp() {
    testUser = userDao.save(testUser);
    testInst = institutionDao.save(testInst);
  }

  @Test
  public void test_findAllByInstitution() {
    assertThat(verifiedInstitutionalAffiliationDao.findAll()).isEmpty();
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(testInst)).isEmpty();

    final DbVerifiedInstitutionalAffiliation testAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedInstitutionalAffiliationDao.findAll()).containsExactly(testAffiliation);
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(testInst))
        .containsExactly(testAffiliation);

    DbInstitution otherInst =
        new DbInstitution().setShortName("Other").setDisplayName("Some Other Institute");
    otherInst = institutionDao.save(otherInst);
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(otherInst)).isEmpty();
  }

  @Test
  public void test_findAllByInstitution_oneAffiliationPerUser() {
    final DbVerifiedInstitutionalAffiliation testAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedInstitutionalAffiliationDao.findAll()).containsExactly(testAffiliation);
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(testInst))
        .containsExactly(testAffiliation);

    final DbInstitution newInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    final DbVerifiedInstitutionalAffiliation replacementAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            testAffiliation
                .setUser(testUser)
                .setInstitution(newInst)
                .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedInstitutionalAffiliationDao.findAll())
        .containsExactly(replacementAffiliation);
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(newInst))
        .containsExactly(replacementAffiliation);
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(testInst)).isEmpty();
  }

  @Test
  public void test_findAllByInstitution_manyAffiliationsPerInst() {
    testUser.setUsername("Alice");
    testUser = userDao.save(testUser);

    final DbVerifiedInstitutionalAffiliation testAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    DbUser newUser = new DbUser();
    newUser.setUsername("Bob");
    newUser = userDao.save(newUser);

    final DbVerifiedInstitutionalAffiliation newAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(newUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.ADMIN)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedInstitutionalAffiliationDao.findAll())
        .containsExactly(testAffiliation, newAffiliation);
    assertThat(verifiedInstitutionalAffiliationDao.findAllByInstitution(testInst))
        .containsExactly(testAffiliation, newAffiliation);
  }

  @Test
  public void test_findFirstByUser() {
    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(userDao.save(new DbUser())))
        .isEmpty();

    final DbVerifiedInstitutionalAffiliation testAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(testUser))
        .hasValue(testAffiliation);
  }

  @Test
  public void test_findFirstByUser_oneAffiliationPerUser() {
    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(userDao.save(new DbUser())))
        .isEmpty();

    final DbVerifiedInstitutionalAffiliation testAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(testUser))
        .hasValue(testAffiliation);

    final DbInstitution newInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    final DbVerifiedInstitutionalAffiliation replacementAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            testAffiliation
                .setInstitution(newInst)
                .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(testUser))
        .hasValue(replacementAffiliation);
  }

  @Test
  public void test_findFirstByUser_manyAffiliationsPerInst() {
    testUser.setUsername("Alice");
    testUser = userDao.save(testUser);

    final DbVerifiedInstitutionalAffiliation testAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    DbUser newUser = new DbUser();
    newUser.setUsername("Bob");
    newUser = userDao.save(newUser);

    final DbVerifiedInstitutionalAffiliation newAffiliation =
        verifiedInstitutionalAffiliationDao.save(
            new DbVerifiedInstitutionalAffiliation()
                .setUser(newUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.ADMIN)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(testUser))
        .hasValue(testAffiliation);
    assertThat(
            verifiedInstitutionalAffiliationDao
                .findFirstByUser(testUser)
                .map(DbVerifiedInstitutionalAffiliation::getInstitution))
        .hasValue(testInst);

    assertThat(verifiedInstitutionalAffiliationDao.findFirstByUser(newUser))
        .hasValue(newAffiliation);
    assertThat(
            verifiedInstitutionalAffiliationDao
                .findFirstByUser(newUser)
                .map(DbVerifiedInstitutionalAffiliation::getInstitution))
        .hasValue(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_twoAffiliationsForUser() {
    verifiedInstitutionalAffiliationDao.save(
        new DbVerifiedInstitutionalAffiliation()
            .setUser(testUser)
            .setInstitution(testInst)
            .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    final DbInstitution newInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    verifiedInstitutionalAffiliationDao.save(
        new DbVerifiedInstitutionalAffiliation()
            .setUser(testUser)
            .setInstitution(newInst)
            .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
            .setInstitutionalRoleOtherText(
                "Arbitrary and does not actually require enum to be OTHER"));
  }
}
