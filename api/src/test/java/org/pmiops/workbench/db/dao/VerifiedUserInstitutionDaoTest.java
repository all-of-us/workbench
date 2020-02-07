package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedUserInstitution;
import org.pmiops.workbench.model.InstitutionalRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class VerifiedUserInstitutionDaoTest {
  @Autowired VerifiedUserInstitutionDao verifiedUserInstitutionDao;
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
    assertThat(verifiedUserInstitutionDao.findAll()).isEmpty();
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(testInst)).isEmpty();

    final DbVerifiedUserInstitution testAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedUserInstitutionDao.findAll()).containsExactly(testAffiliation);
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(testInst))
        .containsExactly(testAffiliation);

    DbInstitution otherInst =
        new DbInstitution().setShortName("Other").setDisplayName("Some Other Institute");
    otherInst = institutionDao.save(otherInst);
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(otherInst)).isEmpty();
  }

  @Test
  public void test_findAllByInstitution_oneAffiliationPerUser() {
    final DbVerifiedUserInstitution testAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedUserInstitutionDao.findAll()).containsExactly(testAffiliation);
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(testInst))
        .containsExactly(testAffiliation);

    final DbInstitution newInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    final DbVerifiedUserInstitution replacementAffiliation =
        verifiedUserInstitutionDao.save(
            testAffiliation
                .setUser(testUser)
                .setInstitution(newInst)
                .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedUserInstitutionDao.findAll()).containsExactly(replacementAffiliation);
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(newInst))
        .containsExactly(replacementAffiliation);
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(testInst)).isEmpty();
  }

  @Test
  public void test_findAllByInstitution_manyAffiliationsPerInst() {
    testUser.setUsername("Alice");
    testUser = userDao.save(testUser);

    final DbVerifiedUserInstitution testAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    DbUser newUser = new DbUser();
    newUser.setUsername("Bob");
    newUser = userDao.save(newUser);

    final DbVerifiedUserInstitution newAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(newUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.ADMIN)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedUserInstitutionDao.findAll())
        .containsExactly(testAffiliation, newAffiliation);
    assertThat(verifiedUserInstitutionDao.findAllByInstitution(testInst))
        .containsExactly(testAffiliation, newAffiliation);
  }

  @Test
  public void test_findFirstByUser() {
    assertThat(verifiedUserInstitutionDao.findFirstByUser(userDao.save(new DbUser()))).isEmpty();

    final DbVerifiedUserInstitution testAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedUserInstitutionDao.findFirstByUser(testUser)).hasValue(testAffiliation);
  }

  @Test
  public void test_findFirstByUser_oneAffiliationPerUser() {
    assertThat(verifiedUserInstitutionDao.findFirstByUser(userDao.save(new DbUser()))).isEmpty();

    final DbVerifiedUserInstitution testAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    assertThat(verifiedUserInstitutionDao.findFirstByUser(testUser)).hasValue(testAffiliation);

    final DbInstitution newInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    final DbVerifiedUserInstitution replacementAffiliation =
        verifiedUserInstitutionDao.save(
            testAffiliation
                .setInstitution(newInst)
                .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedUserInstitutionDao.findFirstByUser(testUser))
        .hasValue(replacementAffiliation);
  }

  @Test
  public void test_findFirstByUser_manyAffiliationsPerInst() {
    testUser.setUsername("Alice");
    testUser = userDao.save(testUser);

    final DbVerifiedUserInstitution testAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(testUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    DbUser newUser = new DbUser();
    newUser.setUsername("Bob");
    newUser = userDao.save(newUser);

    final DbVerifiedUserInstitution newAffiliation =
        verifiedUserInstitutionDao.save(
            new DbVerifiedUserInstitution()
                .setUser(newUser)
                .setInstitution(testInst)
                .setInstitutionalRoleEnum(InstitutionalRole.ADMIN)
                .setInstitutionalRoleOtherText(
                    "Arbitrary and does not actually require enum to be OTHER"));

    assertThat(verifiedUserInstitutionDao.findFirstByUser(testUser)).hasValue(testAffiliation);
    assertThat(
            verifiedUserInstitutionDao
                .findFirstByUser(testUser)
                .map(DbVerifiedUserInstitution::getInstitution))
        .hasValue(testInst);

    assertThat(verifiedUserInstitutionDao.findFirstByUser(newUser)).hasValue(newAffiliation);
    assertThat(
            verifiedUserInstitutionDao
                .findFirstByUser(newUser)
                .map(DbVerifiedUserInstitution::getInstitution))
        .hasValue(testInst);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_twoAffiliationsForUser() {
    verifiedUserInstitutionDao.save(
        new DbVerifiedUserInstitution()
            .setUser(testUser)
            .setInstitution(testInst)
            .setInstitutionalRoleEnum(InstitutionalRole.STUDENT));

    final DbInstitution newInst =
        institutionDao.save(new DbInstitution().setShortName("VUMC").setDisplayName("Vanderbilt"));

    verifiedUserInstitutionDao.save(
        new DbVerifiedUserInstitution()
            .setUser(testUser)
            .setInstitution(newInst)
            .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
            .setInstitutionalRoleOtherText(
                "Arbitrary and does not actually require enum to be OTHER"));
  }
}
