package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class,
  InstitutionUserInstructionsMapperImpl.class
})
public class InstitutionServiceTest {
  @Autowired private InstitutionService service;
  @Autowired private UserDao userDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final Institution testInst =
      new Institution()
          .displayName("this is a test")
          .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);

  // the mapper converts nulls to empty sets
  private final Institution roundTrippedTestInst =
      new Institution()
          .displayName(testInst.getDisplayName())
          .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
          .duaTypeEnum(DuaType.MASTER)
          .emailDomains(Collections.emptyList())
          .emailAddresses(Collections.emptyList())
          .userInstructions("");

  @Before
  public void setUp() {
    // will be retrieved as roundTrippedTestInst
    Institution createdInstitution = service.createInstitution(testInst);
    testInst.setShortName(createdInstitution.getShortName());
    roundTrippedTestInst.setShortName(createdInstitution.getShortName());
  }

  @Test
  public void test_createAnotherInstitution() {
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);

    Institution anotherInst =
        new Institution()
            .displayName("The Institution of testing")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList())
            .userInstructions("");
    Institution createdInstitution = service.createInstitution(anotherInst);
    // Short Name will have random integers attach to first three characters of display Name hence
    // checking for contains
    assertThat(createdInstitution.getShortName()).contains(anotherInst.getShortName());
    anotherInst.setShortName(createdInstitution.getShortName());
    assertThat(createdInstitution).isEqualTo(anotherInst);

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
    String shortName = service.createInstitution(testInst).getShortName();
    roundTrippedTestInst.setShortName(shortName);
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
            .displayName("The Institution of testing")
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList())
            .userInstructions("User Instruction for otherInst");
    service.createInstitution(otherInst);
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, otherInst);

    service.deleteInstitution(testInst.getShortName());
    assertThat(service.getInstitutions()).containsExactly(otherInst);
  }

  // Institution with entry in userInstruction table should populate the institution model
  // parameter userInstructions
  @Test
  public void test_getInstitutionsWithInstruction() {
    final String instructions = "Do some magic!";
    roundTrippedTestInst.setUserInstructions(instructions);
    service.updateInstitution(roundTrippedTestInst.getShortName(), roundTrippedTestInst);
    List<Institution> institutionList = service.getInstitutions();
    assertThat(institutionList.get(0).getUserInstructions()).contains(instructions);
  }

  @Test
  public void test_getInstitutionsWithoutInstruction() {
    List<Institution> institutionList = service.getInstitutions();
    assertThat(institutionList.get(0).getUserInstructions()).isEmpty();
  }

  @Test
  public void test_getInstitution() {
    roundTrippedTestInst.setShortName(testInst.getShortName());
    assertThat(service.getInstitution(testInst.getShortName())).hasValue(roundTrippedTestInst);
    assertThat(service.getInstitution("otherInst")).isEmpty();

    final Institution otherInst =
        new Institution()
            .displayName("The Institution of testing")
            .duaTypeEnum(DuaType.MASTER)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailAddresses(Collections.emptyList())
            .emailDomains(Collections.emptyList())
            .userInstructions("");
    Institution createdIn = service.createInstitution(otherInst);
    assertThat(service.getInstitution(createdIn.getShortName())).hasValue(otherInst);
  }

  @Test
  public void test_updateInstitution_displayName() {
    Institution newInst = roundTrippedTestInst.displayName("a different display name");
    newInst.setShortName(testInst.getShortName());
    assertThat(service.updateInstitution(testInst.getShortName(), newInst)).hasValue(newInst);
    assertThat(service.getInstitution(testInst.getShortName())).hasValue(newInst);
    assertThat(service.getInstitution(testInst.getShortName()).get().getDisplayName())
        .isNotEqualTo(testInst.getDisplayName());
  }

  @Test
  public void test_updateInstitution_emails() {
    final Institution instWithEmails =
        new Institution()
            .displayName("another test")
            .organizationTypeEnum(OrganizationType.INDUSTRY)
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
            .displayName("another test")
            .organizationTypeEnum(OrganizationType.INDUSTRY)
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
            .displayName("another test")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailDomains(ImmutableList.of("broad.org", "google.com"))
            .emailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com"));

    final Institution similarInst =
        new Institution()
            .displayName("The University of Elsewhere")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
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

  @Test(expected = BadRequestException.class)
  public void test_InstitutionNotFound() {
    assertThat(service.getInstitution("missing")).isEmpty();
    assertThat(service.updateInstitution("missing", new Institution())).isEmpty();
  }

  @Test
  public void test_emailValidation_address() {
    final Institution inst =
        service
            .createInstitution(
                new Institution()
                    .displayName("The Broad Institute")
                    .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                    .emailDomains(Lists.newArrayList("broad.org", "mit.edu"))
                    .emailAddresses(
                        Lists.newArrayList("external-researcher@sanger.uk", "science@aol.com")))
            .duaTypeEnum(DuaType.RESTRICTED);

    final DbUser user = createUser("external-researcher@sanger.uk");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_domain() {
    final Institution inst =
        service
            .createInstitution(
                new Institution()
                    .displayName("The Broad Institute")
                    .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                    .emailDomains(Lists.newArrayList("broad.org", "mit.edu"))
                    .emailAddresses(
                        Lists.newArrayList("external-researcher@sanger.uk", "science@aol.com")))
            .duaTypeEnum(DuaType.MASTER);

    final DbUser user = createUser("external-researcher@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_null() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));

    final DbUser user = userDao.save(new DbUser());
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_mismatch() {
    final Institution inst =
        service
            .createInstitution(
                new Institution()
                    .displayName("The Broad Institute")
                    .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION))
            .emailDomains(Lists.newArrayList("broad.org", "mit.edu"))
            .emailAddresses(Lists.newArrayList("email@domain.org"))
            .duaTypeEnum(DuaType.MASTER);

    final DbUser user = createUser("external-researcher@sanger.uk");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  public void test_emailValidation_malformed() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .displayName("The Broad Institute")
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org")));

    final DbUser user = createUser("user@hacker@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_restricted_mismatch() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.INDUSTRY)
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org"))
                .emailAddresses(Lists.newArrayList("testing@broad,org"))
                .duaTypeEnum(DuaType.RESTRICTED));

    final DbUser user = createUser("hack@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_nullDuaType() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org"))
                .emailAddresses(Lists.newArrayList("testing@broad,org")));

    final DbUser user = createUser("hack@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_nullDuaType_incorrectEmailDomain() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .emailDomains(Lists.newArrayList("broad.org", "lab.broad.org"))
                .emailAddresses(Lists.newArrayList("testing@broad,org")));

    final DbUser user = createUser("hack@broadinstitute.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  @Test
  public void getInstitutionUserInstructions_empty() {
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName())).isEmpty();
  }

  public void getInstitutionUserInstructions_instNotFound() {
    assertThat(service.getInstitutionUserInstructions("not found")).isEmpty();
  }

  @Test
  public void setInstitutionUserInstructions() {
    final String instructions = "Do some science";
    final InstitutionUserInstructions inst =
        new InstitutionUserInstructions()
            .institutionShortName(testInst.getShortName())
            .instructions(instructions);
    service.setInstitutionUserInstructions(inst);
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName()))
        .hasValue(instructions);
  }

  @Test
  public void setInstitutionUserInstructions_replace() {
    final String instructions1 = "Do some science";
    final InstitutionUserInstructions inst =
        new InstitutionUserInstructions()
            .institutionShortName(testInst.getShortName())
            .instructions(instructions1);
    service.setInstitutionUserInstructions(inst);
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName()))
        .hasValue(instructions1);

    final String instructions2 = "Do some science and then publish a paper";
    inst.instructions(instructions2);
    service.setInstitutionUserInstructions(inst);
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName()))
        .hasValue(instructions2);
  }

  @Test(expected = NotFoundException.class)
  public void setInstitutionUserInstructions_instNotFound() {
    final String instructions = "Do some science";
    final InstitutionUserInstructions inst =
        new InstitutionUserInstructions()
            .institutionShortName("not found")
            .instructions(instructions);
    service.setInstitutionUserInstructions(inst);
  }

  @Test
  public void deleteInstitutionUserInstructions() {
    final String instructions1 = "Do some science";
    final InstitutionUserInstructions inst =
        new InstitutionUserInstructions()
            .institutionShortName(testInst.getShortName())
            .instructions(instructions1);
    service.setInstitutionUserInstructions(inst);
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName()))
        .hasValue(instructions1);

    assertThat(service.deleteInstitutionUserInstructions(testInst.getShortName())).isTrue();
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName())).isEmpty();

    assertThat(service.deleteInstitutionUserInstructions(testInst.getShortName())).isFalse();
  }

  @Test
  public void deleteInstitutionUserInstructions_empty() {
    assertThat(service.deleteInstitutionUserInstructions(testInst.getShortName())).isFalse();
  }

  @Test(expected = NotFoundException.class)
  public void deleteInstitutionUserInstructions_instNotFound() {
    service.deleteInstitutionUserInstructions("not found");
  }

  @Test
  public void validate_OperationalUser() {
    DbInstitution institution = new DbInstitution();
    institution.setShortName("AouOps");
    assertThat(service.validateOperationalUser(institution)).isTrue();
  }

  @Test
  public void validate_NonOperationalUser() {
    DbInstitution institution = new DbInstitution();
    institution.setShortName("MockAouOps");
    assertThat(service.validateOperationalUser(institution)).isFalse();
  }

  @Test
  public void validate_OperationalUser_nullInstitution() {
    DbInstitution institution = null;
    assertThat(service.validateOperationalUser(institution)).isFalse();
  }

  @Test(expected = BadRequestException.class)
  public void testCreateInstitution_EmptyOrganizationEnum() {
    Institution mockInstitution =
        new Institution().displayName("Institution test").duaTypeEnum(DuaType.MASTER);
    service.createInstitution(mockInstitution);
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
    final DbInstitution inst = service.getDbInstitutionOrThrow(instName);
    final DbVerifiedInstitutionalAffiliation affiliation =
        new DbVerifiedInstitutionalAffiliation()
            .setUser(user)
            .setInstitution(inst)
            .setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    return verifiedInstitutionalAffiliationDao.save(affiliation);
  }
}
