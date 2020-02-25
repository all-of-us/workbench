package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionalRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class
})
public class InstitutionServiceTest {
  @Autowired private InstitutionService service;
  @Autowired private UserDao userDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final Institution testInst =
      new Institution().shortName("test").displayName("this is a test");

  // the mapper converts nulls to empty sets
  private final Institution roundTrippedTestInst =
      new Institution()
          .shortName(testInst.getShortName())
          .displayName(testInst.getDisplayName())
          .emailDomains(Collections.emptyList())
          .emailAddresses(Collections.emptyList());

  @Before
  public void setUp() {
    // will be retrieved as roundTrippedTestInst
    service.createInstitution(testInst);
  }

  @Test
  public void test_createAnotherInstitution() {
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);

    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList());
    assertThat(service.createInstitution(anotherInst)).isEqualTo(anotherInst);

    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, anotherInst);
  }

  @Test
  public void test_deleteInstitution() {
    service.deleteInstitution(testInst.getShortName());
    assertThat(service.getInstitutions()).isEmpty();
  }

  @Test
  public void test_deleteAndRecreateInstitution() {
    service.deleteInstitution(testInst.getShortName());
    assertThat(service.getInstitutions()).isEmpty();
    service.createInstitution(testInst);
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);
  }

  @Test(expected = NotFoundException.class)
  public void test_deleteInstitutionMissing() {
    service.deleteInstitution("missing");
  }

  @Test(expected = ConflictException.class)
  public void test_deleteInstitutionWithAffiliation() {
    createAffiliation(createUser("any email"), testInst.getShortName());
    service.deleteInstitution(testInst.getShortName());
  }

  @Test
  public void test_getInstitutions() {
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList());
    service.createInstitution(otherInst);
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, otherInst);

    service.deleteInstitution(testInst.getShortName());
    assertThat(service.getInstitutions()).containsExactly(otherInst);
  }

  @Test
  public void test_getInstitution() {
    assertThat(service.getInstitution(testInst.getShortName())).hasValue(roundTrippedTestInst);
    assertThat(service.getInstitution("otherInst")).isEmpty();

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailAddresses(Collections.emptyList())
            .emailDomains(Collections.emptyList());
    service.createInstitution(otherInst);
    assertThat(service.getInstitution("otherInst")).hasValue(otherInst);
  }

  @Test
  public void test_updateInstitution_shortName() {
    final String oldShortName = testInst.getShortName();
    final String newShortName = "NewShortName";
    final Institution newInst = roundTrippedTestInst.shortName(newShortName);
    assertThat(service.updateInstitution(oldShortName, newInst)).hasValue(newInst);

    assertThat(service.getInstitution(oldShortName)).isEmpty();
    assertThat(service.getInstitution(newShortName)).hasValue(newInst);
  }

  @Test
  public void test_updateInstitution_displayName() {
    final Institution newInst = roundTrippedTestInst.displayName("a different display name");

    assertThat(service.updateInstitution(testInst.getShortName(), newInst)).hasValue(newInst);
    assertThat(service.getInstitution(testInst.getShortName())).hasValue(newInst);
    assertThat(service.getInstitution(testInst.getShortName()).get().getDisplayName())
        .isNotEqualTo(testInst.getDisplayName());
  }

  @Test
  public void test_updateInstitution_emails() {
    final Institution instWithEmails =
        new Institution()
            .shortName("hasEmails")
            .displayName("another test")
            .emailDomains(ImmutableList.of("broad.org", "google.com"))
            .emailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com"));
    final Institution instWithEmailsRoundTrip = service.createInstitution(instWithEmails);
    assertEqualInstitutions(instWithEmailsRoundTrip, instWithEmails);

    // keep one and change one of each

    final Institution instWithNewEmails =
        instWithEmails
            .emailDomains(ImmutableList.of("broad.org", "verily.com"))
            .emailAddresses(ImmutableList.of("joel@broad.org", "joel@verily.com"));
    final Institution instWithNewEmailsRoundTrip =
        service.updateInstitution(instWithEmails.getShortName(), instWithNewEmails).get();
    assertEqualInstitutions(instWithNewEmailsRoundTrip, instWithNewEmails);

    // clear both
    final Institution instWithoutEmails =
        instWithEmails
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList());
    final Institution instWithoutEmailsRoundTrip =
        service.updateInstitution(instWithEmails.getShortName(), instWithoutEmails).get();
    assertThat(instWithoutEmailsRoundTrip.getEmailDomains()).isEmpty();
    assertThat(instWithoutEmailsRoundTrip.getEmailAddresses()).isEmpty();
  }

  // we uniquify Email Addresses and Domains in the DB per-institution
  @Test
  public void test_uniqueEmailPatterns() {
    final Institution instWithDupes =
        new Institution()
            .shortName("test2")
            .displayName("another test")
            .emailDomains(ImmutableList.of("broad.org", "broad.org", "google.com"))
            .emailAddresses(
                ImmutableList.of("joel@broad.org", "joel@broad.org", "joel@google.com"));

    final Set<String> uniquifiedEmailDomains = Sets.newHashSet(instWithDupes.getEmailDomains());
    final Set<String> uniquifiedEmailAddresses = Sets.newHashSet(instWithDupes.getEmailAddresses());

    final Institution uniquifiedInst = service.createInstitution(instWithDupes);

    assertThat(instWithDupes.getEmailDomains().size()).isNotEqualTo(uniquifiedEmailDomains.size());
    assertThat(uniquifiedInst.getEmailDomains()).containsExactlyElementsIn(uniquifiedEmailDomains);

    assertThat(instWithDupes.getEmailAddresses().size())
        .isNotEqualTo(uniquifiedEmailAddresses.size());
    assertThat(uniquifiedInst.getEmailAddresses())
        .containsExactlyElementsIn(uniquifiedEmailAddresses);
  }

  // we do not uniquify Email Addresses and Domains in the DB across institutions
  @Test
  public void test_nonUniqueEmailPatterns() {
    final Institution instWithEmails =
        new Institution()
            .shortName("hasEmails")
            .displayName("another test")
            .emailDomains(ImmutableList.of("broad.org", "google.com"))
            .emailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com"));

    final Institution similarInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The University of Elsewhere")
            .emailDomains(instWithEmails.getEmailDomains())
            .emailAddresses(instWithEmails.getEmailAddresses());

    final Institution instWithEmailsViaDb = service.createInstitution(instWithEmails);
    final Institution similarInstViaDb = service.createInstitution(similarInst);

    assertThat(instWithEmailsViaDb.getShortName()).isNotEqualTo(similarInstViaDb.getShortName());
    assertThat(instWithEmailsViaDb.getDisplayName())
        .isNotEqualTo(similarInstViaDb.getDisplayName());
    assertThat(instWithEmailsViaDb.getEmailDomains())
        .containsExactlyElementsIn(similarInstViaDb.getEmailDomains());
    assertThat(instWithEmailsViaDb.getEmailAddresses())
        .containsExactlyElementsIn(similarInstViaDb.getEmailAddresses());
  }

  @Test
  public void test_InstitutionNotFound() {
    assertThat(service.getInstitution("missing")).isEmpty();
    assertThat(service.updateInstitution("missing", new Institution())).isEmpty();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_nonUniqueIds() {
    service.createInstitution(
        new Institution().shortName("test").displayName("We are all individuals"));
    service.createInstitution(new Institution().shortName("test").displayName("I'm not"));
  }

  @Test
  public void test_emailValidation_domain() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org")));

    final DbUser user = createUser("user@broad.org");
    final DbVerifiedInstitutionalAffiliation affiliation =
        createAffiliation(user, inst.getShortName());

    assertThat(service.validateAffiliation(affiliation, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_address() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .emailDomains(Lists.newArrayList("broad.org", "mit.edu"))
                .emailAddresses(
                    Lists.newArrayList("external-researcher@sanger.uk", "science@aol.com")));

    final DbUser user = createUser("external-researcher@sanger.uk");
    final DbVerifiedInstitutionalAffiliation affiliation =
        createAffiliation(user, inst.getShortName());

    assertThat(service.validateAffiliation(affiliation, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_null() {
    final Institution inst =
        service.createInstitution(
            new Institution().shortName("Broad").displayName("The Broad Institute"));

    final DbUser user = userDao.save(new DbUser());
    final DbVerifiedInstitutionalAffiliation affiliation =
        createAffiliation(user, inst.getShortName());

    assertThat(service.validateAffiliation(affiliation, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_mismatch() {
    final Institution inst =
        service
            .createInstitution(
                new Institution().shortName("Broad").displayName("The Broad Institute"))
            .emailDomains(Lists.newArrayList("broad.org", "mit.edu"));

    final DbUser user = createUser("external-researcher@sanger.uk");
    final DbVerifiedInstitutionalAffiliation affiliation =
        createAffiliation(user, inst.getShortName());

    assertThat(service.validateAffiliation(affiliation, user.getContactEmail())).isFalse();
  }

  public void test_emailValidation_malformed() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org")));

    final DbUser user = createUser("user@hacker@broad.org");
    final DbVerifiedInstitutionalAffiliation affiliation =
        createAffiliation(user, inst.getShortName());

    assertThat(service.validateAffiliation(affiliation, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_changedShortName() {
    final String oldShortName = "Broad";
    final String newShortName = "TheBroad";

    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName(oldShortName)
                .displayName("The Broad Institute")
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org")));

    final DbUser user = createUser("user@broad.org");
    final DbVerifiedInstitutionalAffiliation affiliation =
        createAffiliation(user, inst.getShortName());

    assertThat(service.validateAffiliation(affiliation, user.getContactEmail())).isTrue();

    final Institution renamed = inst.shortName(newShortName);
    service.updateInstitution(oldShortName, renamed);

    final DbVerifiedInstitutionalAffiliation updatedAffiliation =
        verifiedInstitutionalAffiliationDao.findFirstByUser(user).get();

    assertThat(updatedAffiliation.getInstitution().getShortName()).isEqualTo(newShortName);
    assertThat(service.validateAffiliation(updatedAffiliation, user.getContactEmail())).isTrue();
  }

  // Institutions' email domains and addresses are Lists but have no inherent order,
  // so they can't be directly compared for equality
  private void assertEqualInstitutions(Institution actual, final Institution expected) {
    assertThat(actual.getShortName()).isEqualTo(expected.getShortName());
    assertThat(actual.getDisplayName()).isEqualTo(expected.getDisplayName());
    assertThat(actual.getEmailDomains()).containsExactlyElementsIn(expected.getEmailDomains());
    assertThat(actual.getEmailAddresses()).containsExactlyElementsIn(expected.getEmailAddresses());
  }

  private DbUser createUser(String contactEmail) {
    DbUser user = new DbUser();
    user.setContactEmail(contactEmail);
    user = userDao.save(user);
    return user;
  }

  private DbVerifiedInstitutionalAffiliation createAffiliation(
      final DbUser user, final String instName) {
    final DbInstitution inst = service.getDbInstitution(instName).get();
    final DbVerifiedInstitutionalAffiliation affiliation =
        new DbVerifiedInstitutionalAffiliation()
            .setUser(user)
            .setInstitution(inst)
            .setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    return verifiedInstitutionalAffiliationDao.save(affiliation);
  }
}
