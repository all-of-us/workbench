package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.TierEmailAddresses;
import org.pmiops.workbench.model.TierEmailDomains;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class,
  InstitutionUserInstructionsMapperImpl.class,
  InstitutionEmailDomainMapperImpl.class,
  InstitutionEmailAddressMapperImpl.class,
  InstitutionTierRequirementMapperImpl.class,
})
public class InstitutionServiceTest extends SpringTest {

  @Autowired private InstitutionService service;
  @Autowired private UserDao userDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final Institution testInst =
      new Institution()
          .shortName("test")
          .displayName("this is a test")
          .organizationTypeEnum(OrganizationType.INDUSTRY);

  // the mapper converts null emails to empty lists
  private final Institution roundTrippedTestInst =
      new Institution()
          .shortName(testInst.getShortName())
          .displayName(testInst.getDisplayName())
          .duaTypeEnum(DuaType.MASTER)
          .tierEmailDomains(Collections.emptyList())
          .tierEmailAddresses(Collections.emptyList())
          .tierRequirements(Collections.emptyList())
          .organizationTypeEnum(testInst.getOrganizationTypeEnum());

  private DbAccessTier registeredTier;
  private InstitutionTierRequirement institutionTierRequirement;
  private TierEmailAddresses rtTierEmailAddress;
  private TierEmailDomains rtTierEmailDomains;

  @BeforeEach
  public void setUp() {
    // will be retrieved as roundTrippedTestInst
    service.createInstitution(testInst);
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    institutionTierRequirement =
        new InstitutionTierRequirement()
            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
            .eraRequired(false)
            .accessTierShortName(registeredTier.getShortName());
    rtTierEmailAddress.accessTierShortName(registeredTier.getShortName());
    rtTierEmailAddress.accessTierShortName(registeredTier.getShortName());
  }

  @Test
  public void test_createAnotherInstitution() {
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);

    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("An Institution for Testing")
            .tierEmailDomains(Collections.emptyList())
            .tierEmailAddresses(Collections.emptyList())
            .tierRequirements(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY);
    assertThat(service.createInstitution(anotherInst)).isEqualTo(anotherInst);

    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, anotherInst);
    Comparator<Institution> comparator = Comparator.comparing(Institution::getDisplayName);
    assertThat(service.getInstitutions()).isStrictlyOrdered(comparator);
  }

  @Test
  public void testCreateInstitution_withTierRequirement() {
    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("An Institution for Testing")
            .tierEmailDomains(Collections.emptyList())
            .tierEmailAddresses(Collections.emptyList())
            .tierRequirements(
                ImmutableList.of(
                    new InstitutionTierRequirement()
                        .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                        .eraRequired(false)
                        .accessTierShortName(registeredTier.getShortName())))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    assertThat(service.createInstitution(anotherInst)).isEqualTo(anotherInst);

    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, anotherInst);
    Comparator<Institution> comparator = Comparator.comparing(Institution::getDisplayName);
    assertThat(service.getInstitutions()).isStrictlyOrdered(comparator);
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

  @Test
  public void test_deleteInstitutionMissing() {
    assertThrows(
        NotFoundException.class,
        () -> {
          service.deleteInstitution("missing");
        });
  }

  @Test
  public void test_deleteInstitutionWithAffiliation() {
    assertThrows(
        ConflictException.class,
        () -> {
          createAffiliation(createUser("any email"), testInst.getShortName());
          service.deleteInstitution(testInst.getShortName());
        });
  }

  @Test
  public void test_getInstitutions() {
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .duaTypeEnum(DuaType.MASTER)
            .tierEmailDomains(Collections.emptyList())
            .tierEmailAddresses(Collections.emptyList())
            .tierRequirements(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY);
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
    final InstitutionUserInstructions inst =
        new InstitutionUserInstructions()
            .institutionShortName(roundTrippedTestInst.getShortName())
            .instructions(instructions);
    service.setInstitutionUserInstructions(inst);
    List<Institution> institutionList = service.getInstitutions();
    assertThat(institutionList.get(0).getUserInstructions()).contains(instructions);
  }

  @Test
  public void test_getInstitutionsWithoutInstruction() {
    List<Institution> institutionList = service.getInstitutions();
    assertThat(institutionList.get(0).getUserInstructions()).isNull();
  }

  @Test
  public void test_getInstitution() {
    assertThat(service.getInstitution(testInst.getShortName())).hasValue(roundTrippedTestInst);
    assertThat(service.getInstitution("otherInst")).isEmpty();

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .duaTypeEnum(DuaType.MASTER)
            .tierEmailAddresses(Collections.emptyList())
            .tierEmailDomains(Collections.emptyList())
            .tierRequirements(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY);
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
            .tierEmailDomains(ImmutableList.of(rtTierEmailDomains.emailDomains(ImmutableList.of("broad.org", "google.com"))))
            .tierEmailAddresses(ImmutableList.of(rtTierEmailAddress.emailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com"))))
            .organizationTypeEnum(OrganizationType.INDUSTRY);
    final Institution instWithEmailsRoundTrip = service.createInstitution(instWithEmails);
    assertThat(instWithEmailsRoundTrip).isEqualTo(instWithEmails);

    // keep one and change one of each

    final Institution instWithNewEmails =
        instWithEmails
            .tierEmailDomains(ImmutableList.of("broad.org", "verily.com"))
            .tierEmailAddresses(ImmutableList.of("joel@broad.org", "joel@verily.com"));
    final Institution instWithNewEmailsRoundTrip =
        service.updateInstitution(instWithEmails.getShortName(), instWithNewEmails).get();
    assertThat(instWithNewEmailsRoundTrip).isEqualTo(instWithNewEmails);

    // clear both
    final Institution instWithoutEmails =
        instWithEmails
            .tierEmailDomains(Collections.emptyList())
            .tierEmailAddresses(Collections.emptyList());
    final Institution instWithoutEmailsRoundTrip =
        service.updateInstitution(instWithEmails.getShortName(), instWithoutEmails).get();
    assertThat(instWithoutEmailsRoundTrip.getEmailDomains()).isEmpty();
    assertThat(instWithoutEmailsRoundTrip.getEmailAddresses()).isEmpty();
  }

  @Test
  public void test_updateInstitution_tierRequirement() {
    final Institution existingInst =
        new Institution()
            .shortName("test_updateInstitution_tierRequirement")
            .displayName("test_updateInstitution_tierRequirement")
            .tierEmailAddresses(Collections.emptyList())
            .tierEmailDomains(Collections.emptyList())
            .tierRequirements(ImmutableList.of(institutionTierRequirement))
            .organizationTypeEnum(OrganizationType.INDUSTRY);
    assertThat(service.createInstitution(existingInst)).isEqualTo(existingInst);

    final Institution instWithNewTierRequirement =
        existingInst.tierRequirements(
            ImmutableList.of(
                institutionTierRequirement.membershipRequirement(
                    InstitutionMembershipRequirement.NO_ACCESS)));
    assertThat(
            service
                .updateInstitution(existingInst.getShortName(), instWithNewTierRequirement)
                .get())
        .isEqualTo(instWithNewTierRequirement);

    // clear
    final Institution instWithoutTierRequirements =
        instWithNewTierRequirement.tierRequirements(Collections.emptyList());
    assertThat(
            service
                .updateInstitution(existingInst.getShortName(), instWithoutTierRequirements)
                .get()
                .getTierRequirements())
        .isEmpty();
  }

  // we uniquify Email Addresses and Domains in the DB per-institution
  @Test
  public void test_uniqueEmailPatterns() {
    final Institution instWithDupes =
        new Institution()
            .shortName("test2")
            .displayName("another test")
            .tierEmailDomains(ImmutableList.of("broad.org", "broad.org", "google.com"))
            .tierEmailAddresses(ImmutableList.of("joel@broad.org", "joel@broad.org", "joel@google.com"))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

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

  // Email Addresses and Domains can be claimed by multiple institutions
  @Test
  public void test_nonUniqueEmailPatterns() {
    final Institution instWithEmails =
        new Institution()
            .shortName("hasEmails")
            .displayName("another test")
            .tierEmailDomains(ImmutableList.of("broad.org", "google.com"))
            .tierEmailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com"))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    final Institution similarInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The University of Elsewhere")
            .tierEmailDomains(instWithEmails.getEmailDomains())
            .tierEmailAddresses(instWithEmails.getEmailAddresses())
            .organizationTypeEnum(OrganizationType.INDUSTRY);

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
    Institution updateInstitution =
        new Institution()
            .displayName("Try To Update")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    assertThat(service.updateInstitution("missing", updateInstitution)).isEmpty();
  }

  @Test
  public void test_nonUniqueIds() {
    assertThrows(
        ConflictException.class,
        () -> {
          service.createInstitution(
              new Institution()
                  .shortName("test")
                  .displayName("We are all individuals")
                  .organizationTypeEnum(OrganizationType.INDUSTRY));
          service
              .createInstitution(new Institution().shortName("test").displayName("I'm not"))
              .organizationTypeEnum(OrganizationType.EDUCATIONAL_INSTITUTION);
        });
  }

  @Test
  public void test_nonUniqueDisplayName() {
    assertThrows(
        ConflictException.class,
        () -> {
          service.createInstitution(
              new Institution()
                  .shortName("test")
                  .displayName("We are all individuals")
                  .organizationTypeEnum(OrganizationType.INDUSTRY));
          service
              .createInstitution(
                  new Institution().shortName("testing").displayName("We are all individuals"))
              .organizationTypeEnum(OrganizationType.EDUCATIONAL_INSTITUTION);
        });
  }

  @Test
  public void test_emailValidation_address() {
    final Institution inst =
        service
            .createInstitution(
                new Institution()
                    .shortName("Broad")
                    .displayName("The Broad Institute")
                    .tierEmailDomains(Lists.newArrayList("broad.org", "mit.edu"))
                    .tierEmailAddresses(
                        Lists.newArrayList("external-researcher@sanger.uk", "science@aol.com"))
                    .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION))
            .duaTypeEnum(DuaType.RESTRICTED);

    final DbUser user = createUser("external-researcher@sanger.uk");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_domain() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .tierEmailDomains(Lists.newArrayList("broad.org", "mit.edu"))
                .tierEmailAddresses(
                    Lists.newArrayList("external-researcher@sanger.uk", "science@aol.com"))
                .duaTypeEnum(DuaType.MASTER)
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));

    final DbUser user = createUser("external-researcher@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_null() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
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
                    .shortName("Broad")
                    .displayName("The Broad Institute")
                    .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION))
            .tierEmailDomains(Lists.newArrayList("broad.org", "mit.edu"))
            .tierEmailAddresses(Lists.newArrayList("email@domain.org"))
            .duaTypeEnum(DuaType.MASTER);

    final DbUser user = createUser("external-researcher@sanger.uk");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  public void test_emailValidation_malformed() {
    final Institution inst =
        service
            .createInstitution(
                new Institution()
                    .shortName("Broad")
                    .displayName("The Broad Institute")
                    .tierEmailDomains(Lists.newArrayList("broad.org", "lab.broad.org")))
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);

    final DbUser user = createUser("user@hacker@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_restricted_mismatch() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .tierEmailDomains(Lists.newArrayList("broad.org", "lab.broad.org"))
                .tierEmailAddresses(Lists.newArrayList("testing@broad.org"))
                .duaTypeEnum(DuaType.RESTRICTED)
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));

    final DbUser user = createUser("hack@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
  }

  @Test
  public void test_emailValidation_nullDuaType() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .tierEmailDomains(Lists.newArrayList("broad.org", "lab.broad.org"))
                .tierEmailAddresses(Lists.newArrayList("testing@broad,org"))
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));

    final DbUser user = createUser("hack@broad.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isTrue();
  }

  @Test
  public void test_emailValidation_nullDuaType_incorrectEmailDomain() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .tierEmailDomains(Lists.newArrayList("broad.org", "lab.broad.org"))
                .tierEmailAddresses(Lists.newArrayList("testing@broad,org"))
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));

    final DbUser user = createUser("hack@broadinstitute.org");
    assertThat(service.validateInstitutionalEmail(inst, user.getContactEmail())).isFalse();
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
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierEmailDomains(Lists.newArrayList("broad.org", "lab.broad.org")));

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

  @Test
  public void getInstitutionUserInstructions_empty() {
    assertThat(service.getInstitutionUserInstructions(testInst.getShortName())).isEmpty();
  }

  @Test
  public void getInstitutionUserInstructions_instNotFound() {
    assertThrows(
        NotFoundException.class,
        () -> {
          service.getInstitutionUserInstructions("not found");
        });
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

  @Test
  public void setInstitutionUserInstructions_instNotFound() {
    assertThrows(
        NotFoundException.class,
        () -> {
          final String instructions = "Do some science";
          final InstitutionUserInstructions inst =
              new InstitutionUserInstructions()
                  .institutionShortName("not found")
                  .instructions(instructions);
          service.setInstitutionUserInstructions(inst);
        });
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

  @Test
  public void deleteInstitutionUserInstructions_instNotFound() {
    assertThrows(
        NotFoundException.class,
        () -> {
          service.deleteInstitutionUserInstructions("not found");
        });
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

  @Test
  public void test_createInstitution_MissingOrganizationType() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Institution institution_NoOrgType =
              new Institution()
                  .displayName("No Organization")
                  .duaTypeEnum(DuaType.MASTER)
                  .tierEmailAddresses(Arrays.asList("testDomain.com"))
                  .tierRequirements(Collections.emptyList())
                  .userInstructions("Should throw exception");
          service.createInstitution(institution_NoOrgType);
        });
  }

  @Test
  public void test_createInstitution_AddDefaultDUA() {
    Institution institution_NoDUA =
        new Institution()
            .displayName("No Organization")
            .tierEmailAddresses(Collections.emptyList())
            .tierEmailDomains(Collections.emptyList())
            .tierRequirements(Collections.emptyList())
            .userInstructions("Should Add dua Type As Master")
            .organizationTypeEnum(OrganizationType.INDUSTRY);
    Institution createdInstitution = service.createInstitution(institution_NoDUA);
    Institution institutionWithDua = institution_NoDUA.duaTypeEnum(DuaType.MASTER);
    assertThat(createdInstitution).isEqualTo(institutionWithDua);
  }

  @Test
  public void test_createInstitution_IncorrectEmailAddressFormat() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Institution institution_EmailAddress =
              new Institution()
                  .displayName("No Organization")
                  .duaTypeEnum(DuaType.RESTRICTED)
                  .tierEmailAddresses(
                      Arrays.asList("CorrectEmailAddress@domain.com, incorrectEmail.com"))
                  .tierRequirements(Collections.emptyList())
                  .organizationTypeEnum(OrganizationType.INDUSTRY);
          service.createInstitution(institution_EmailAddress);
        });
  }

  @Test
  public void test_createInstitution_DisplayNameWithSpaces() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Institution institution_EmailAddress =
              new Institution()
                  .displayName("     ")
                  .duaTypeEnum(DuaType.RESTRICTED)
                  .tierEmailAddresses(
                      Arrays.asList("CorrectEmailAddress@domain.com, incorrectEmail.com"))
                  .tierRequirements(Collections.emptyList())
                  .organizationTypeEnum(OrganizationType.INDUSTRY);
          service.createInstitution(institution_EmailAddress);
        });
  }

  @Test
  public void test_createInstitution_OtherOrganizationType_noOtherText() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Institution institution_withOtherOrganizationType =
              new Institution()
                  .displayName("     ")
                  .duaTypeEnum(DuaType.RESTRICTED)
                  .tierEmailAddresses(
                      Arrays.asList("CorrectEmailAddress@domain.com, incorrectEmail.com"))
                  .tierRequirements(Collections.emptyList())
                  .organizationTypeEnum(OrganizationType.OTHER);
          service.createInstitution(institution_withOtherOrganizationType);
        });
  }

  @Test
  public void test_createInstitution_OtherOrganizationType() {
    Institution institution_withOtherOrganizationType =
        new Institution()
            .displayName("     ")
            .duaTypeEnum(DuaType.RESTRICTED)
            .tierEmailAddresses(Arrays.asList("CorrectEmailAddress@domain.com"))
            .tierEmailDomains(Collections.EMPTY_LIST)
            .tierRequirements(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.OTHER)
            .organizationTypeOtherText("Some text");
    assertThat(service.createInstitution(institution_withOtherOrganizationType))
        .isEqualTo(institution_withOtherOrganizationType);
  }

  @Test
  public void test_updateInstitution_RemoveUserInstructionFromExistingInstitution() {
    Institution institution_WithUserInstructions =
        new Institution()
            .displayName("No Organization")
            .duaTypeEnum(DuaType.RESTRICTED)
            .tierEmailAddresses(Arrays.asList("CorrectEmailAddress@domain.com"))
            .tierEmailDomains(Collections.EMPTY_LIST)
            .tierRequirements(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .userInstructions("Some user instructions");
    Institution createdInstitution = service.createInstitution(institution_WithUserInstructions);
    assertThat(createdInstitution.getUserInstructions()).isEqualTo("Some user instructions");

    Institution institutionNoUserInstruction =
        institution_WithUserInstructions.userInstructions("");

    Institution expectedUpdateInstitution = institution_WithUserInstructions.userInstructions(null);
    assertThat(
            service.updateInstitution(
                institution_WithUserInstructions.getShortName(), institutionNoUserInstruction))
        .hasValue(expectedUpdateInstitution);
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
