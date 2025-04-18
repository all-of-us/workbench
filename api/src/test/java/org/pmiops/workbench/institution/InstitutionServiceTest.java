package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.institution.InstitutionServiceImpl.OPERATIONAL_USER_INSTITUTION_SHORT_NAME;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pmiops.workbench.FakeClockConfiguration;
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
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.UserTierEligibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  FakeClockConfiguration.class,
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class,
  InstitutionUserInstructionsMapperImpl.class,
  InstitutionTierConfigMapperImpl.class,
})
public class InstitutionServiceTest {

  @Autowired private InstitutionService service;
  @Autowired private UserDao userDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final Institution testInst =
      new Institution()
          .shortName("test")
          .displayName("this is a test")
          .organizationTypeEnum(OrganizationType.INDUSTRY)
          .bypassInitialCreditsExpiration(false);

  // the mapper converts null emails to empty lists
  private final Institution roundTrippedTestInst =
      new Institution()
          .shortName(testInst.getShortName())
          .displayName(testInst.getDisplayName())
          .organizationTypeEnum(testInst.getOrganizationTypeEnum())
          .bypassInitialCreditsExpiration(false)
          .tierConfigs(Collections.emptyList());

  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;

  private InstitutionTierConfig rtTierConfig;
  private InstitutionTierConfig ctTierConfig;

  @BeforeEach
  public void setUp() {
    // will be retrieved as roundTrippedTestInst
    service.createInstitution(testInst);
    registeredTier = accessTierDao.save(createRegisteredTier());
    controlledTier = accessTierDao.save(createControlledTier());
    rtTierConfig = new InstitutionTierConfig().accessTierShortName(registeredTier.getShortName());
    ctTierConfig = new InstitutionTierConfig().accessTierShortName(controlledTier.getShortName());
  }

  @Test
  public void test_createAnotherInstitution() {
    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst);

    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("An Institution for Testing")
            .tierConfigs(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(true);
    assertThat(service.createInstitution(anotherInst)).isEqualTo(anotherInst);

    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, anotherInst);
    Comparator<Institution> comparator = Comparator.comparing(Institution::getDisplayName);
    assertThat(service.getInstitutions()).isInStrictOrder(comparator);
  }

  @Test
  public void testCreateInstitution_withTierRequirement() {
    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("An Institution for Testing")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName()))
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(true);

    assertThat(service.createInstitution(anotherInst)).isEqualTo(anotherInst);

    assertThat(service.getInstitutions()).containsExactly(roundTrippedTestInst, anotherInst);
    Comparator<Institution> comparator = Comparator.comparing(Institution::getDisplayName);
    assertThat(service.getInstitutions()).isInStrictOrder(comparator);
  }

  @Test
  public void testCreateInstitutionError_accessTierNotFound() {
    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("An Institution for Testing")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName("non exist tier"))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    assertThrows(NotFoundException.class, () -> service.createInstitution(anotherInst));
  }

  @Test
  public void testCreateInstitution_accessTierNotFound_noAccessRequirement() {
    final Institution anotherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("An Institution for Testing")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.NO_ACCESS)
                    .accessTierShortName("non exist tier"))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    assertThat(service.createInstitution(anotherInst).getTierConfigs()).isEmpty();
  }

  @Test
  public void testCreateInstitution_withPeriodInName() {
    final Institution anotherInst =
        new Institution()
            .displayName("a.b .c")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName()))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    assertThat(service.createInstitution(anotherInst).getShortName().startsWith("abc")).isTrue();
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
            .tierConfigs(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(false);
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
            .tierConfigs(Collections.emptyList())
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(true);
    service.createInstitution(otherInst);
    assertThat(service.getInstitution("otherInst")).hasValue(otherInst);
  }

  @Test
  public void test_updateInstitution_shortName() {
    final String oldShortName = testInst.getShortName();
    final String newShortName = "NewShortName";
    final Institution newInst = roundTrippedTestInst.shortName(newShortName);
    assertThrows(BadRequestException.class, () -> service.updateInstitution(oldShortName, newInst));
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
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName())
                    .emailDomains(ImmutableList.of("broad.org", "google.com"))
                    .emailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com")))
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(true);
    final Institution instWithEmailsRoundTrip = service.createInstitution(instWithEmails);
    assertThat(instWithEmailsRoundTrip).isEqualTo(instWithEmails);

    // keep one and change one of each

    final Institution instWithNewEmails =
        instWithEmails
            .tierConfigs(
                ImmutableList.of(
                    rtTierConfig
                        .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                        .accessTierShortName(registeredTier.getShortName())
                        .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                        .emailAddresses(ImmutableList.of("joel@broad.org", "joel@verily.com"))))
            .organizationTypeEnum(OrganizationType.INDUSTRY);
    final Institution instWithNewEmailsRoundTrip =
        service.updateInstitution(instWithEmails.getShortName(), instWithNewEmails).get();
    assertThat(instWithNewEmailsRoundTrip).isEqualTo(instWithNewEmails);

    // clear both
    final Institution instWithoutEmails =
        instWithEmails.tierConfigs(
            ImmutableList.of(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName())
                    .emailAddresses(ImmutableList.of())));
    final Institution instWithoutEmailsRoundTrip =
        service.updateInstitution(instWithEmails.getShortName(), instWithoutEmails).get();
    assertThat(instWithoutEmailsRoundTrip.getTierConfigs().get(0).getEmailAddresses()).isNull();
  }

  @Test
  public void test_updateInstitution_tierRequirement() {
    final Institution existingInst =
        new Institution()
            .shortName("test_updateInstitution_tierRequirement")
            .displayName("test_updateInstitution_tierRequirement")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName()))
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(true);
    assertThat(service.createInstitution(existingInst)).isEqualTo(existingInst);

    final Institution instWithNewTierRequirement =
        existingInst.tierConfigs(
            ImmutableList.of(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName())));
    assertThat(
            service
                .updateInstitution(existingInst.getShortName(), instWithNewTierRequirement)
                .get())
        .isEqualTo(instWithNewTierRequirement);

    // clear
    final Institution instWithoutTierRequirements = instWithNewTierRequirement.tierConfigs(null);
    assertThat(
            service
                .updateInstitution(existingInst.getShortName(), instWithoutTierRequirements)
                .get()
                .getTierConfigs())
        .isEmpty();
  }

  @Test
  public void test_updateInstitution_tierRequirement_changetoNoAccess() {
    final Institution existingInst =
        new Institution()
            .shortName("test_updateInstitution_tierRequirement")
            .displayName("test_updateInstitution_tierRequirement")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName()))
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .bypassInitialCreditsExpiration(true);
    assertThat(service.createInstitution(existingInst)).isEqualTo(existingInst);

    final Institution instWithNewTierRequirement =
        existingInst.tierConfigs(
            ImmutableList.of(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.NO_ACCESS)
                    .accessTierShortName(registeredTier.getShortName())));
    assertThat(
            service
                .updateInstitution(existingInst.getShortName(), instWithNewTierRequirement)
                .get()
                .getTierConfigs())
        .isEmpty();
  }

  // we uniquify Email Addresses and Domains in the DB per-institution
  @Test
  public void test_uniqueEmailPatterns() {
    final Institution instWithDupes =
        new Institution()
            .shortName("test2")
            .displayName("another test")
            .addTierConfigsItem(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                    .accessTierShortName(registeredTier.getShortName())
                    .emailDomains(ImmutableList.of("broad.org", "broad.org", "google.com"))
                    .emailAddresses(
                        ImmutableList.of("joel@broad.org", "joel@broad.org", "joel@google.com")))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    final Set<String> uniquifiedEmailDomains =
        Sets.newHashSet(instWithDupes.getTierConfigs().get(0).getEmailDomains());
    final Set<String> uniquifiedEmailAddresses =
        Sets.newHashSet(instWithDupes.getTierConfigs().get(0).getEmailAddresses());

    final Institution uniquifiedInst = service.createInstitution(instWithDupes);

    assertThat(instWithDupes.getTierConfigs().get(0).getEmailDomains().size())
        .isNotEqualTo(uniquifiedEmailDomains.size());
    assertThat(uniquifiedInst.getTierConfigs().get(0).getEmailDomains())
        .containsExactlyElementsIn(uniquifiedEmailDomains);

    assertThat(instWithDupes.getTierConfigs().get(0).getEmailAddresses().size())
        .isNotEqualTo(uniquifiedEmailAddresses.size());
    assertThat(uniquifiedInst.getTierConfigs().get(0).getEmailAddresses())
        .containsExactlyElementsIn(uniquifiedEmailAddresses);
  }

  // Email Addresses and Domains can be claimed by multiple institutions
  @Test
  public void test_nonUniqueEmailPatterns() {
    final Institution instWithEmails =
        new Institution()
            .shortName("hasEmails")
            .displayName("another test")
            .tierConfigs(
                ImmutableList.of(
                    rtTierConfig
                        .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                        .accessTierShortName(registeredTier.getShortName())
                        .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                        .emailAddresses(ImmutableList.of("joel@broad.org", "joel@verily.com"))))
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    final Institution similarInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The University of Elsewhere")
            .tierConfigs(instWithEmails.getTierConfigs())
            .organizationTypeEnum(OrganizationType.INDUSTRY);

    final Institution instWithEmailsViaDb = service.createInstitution(instWithEmails);
    final Institution similarInstViaDb = service.createInstitution(similarInst);

    assertThat(instWithEmailsViaDb.getShortName()).isNotEqualTo(similarInstViaDb.getShortName());
    assertThat(instWithEmailsViaDb.getDisplayName())
        .isNotEqualTo(similarInstViaDb.getDisplayName());
    assertThat(instWithEmailsViaDb.getTierConfigs().size()).isEqualTo(1);
    assertThat(instWithEmailsViaDb.getTierConfigs().get(0).getEmailDomains())
        .containsExactlyElementsIn(similarInstViaDb.getTierConfigs().get(0).getEmailDomains());
    assertThat(instWithEmailsViaDb.getTierConfigs().get(0).getEmailAddresses())
        .containsExactlyElementsIn(similarInstViaDb.getTierConfigs().get(0).getEmailAddresses());
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
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                            .emailAddresses(
                                ImmutableList.of(
                                    "external-researcher@sanger.uk", "science@aol.com")))));
    assertThat(
            service.validateInstitutionalEmail(
                inst, "external-researcher@sanger.uk", REGISTERED_TIER_SHORT_NAME))
        .isTrue();
    // Fail even when domain matches, because the requirement is ADDRESSES.
    assertThat(
            service.validateInstitutionalEmail(inst, "yy@verily.com", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
  }

  @Test
  public void test_emailValidation_address_ignoreCase() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                            .emailAddresses(
                                ImmutableList.of(
                                    "EXternal-rEsearcher@sanger.UK", "science@aol.com")))));
    assertThat(
            service.validateInstitutionalEmail(
                inst, "external-researcher@sanger.uk", REGISTERED_TIER_SHORT_NAME))
        .isTrue();
    // Fail even when domain matches, because the requirement is ADDRESSES.
    assertThat(
            service.validateInstitutionalEmail(inst, "yy@verily.com", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
  }

  @Test
  public void test_emailValidation_null_address() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                            .emailAddresses(
                                ImmutableList.of(
                                    "external-researcher@sanger.uk", "science@aol.com")))));
    assertThat(service.validateInstitutionalEmail(inst, null, REGISTERED_TIER_SHORT_NAME))
        .isFalse();
  }

  @Test
  public void test_emailValidation_domain() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                            .emailAddresses(
                                ImmutableList.of(
                                    "external-researcher@sanger.uk", "science@aol.com")))));

    assertThat(
            service.validateInstitutionalEmail(inst, "yy@verily.com", REGISTERED_TIER_SHORT_NAME))
        .isTrue();
    // malformed
    assertThat(
            service.validateInstitutionalEmail(
                inst, "yy@hacker@verily.org", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
    // Fail even when domain matches, because the requirement is DOMAINS.
    assertThat(
            service.validateInstitutionalEmail(
                inst, "external-researcher@sanger.uk", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
  }

  @Test
  public void test_emailValidation_no_access() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.NO_ACCESS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com"))
                            .emailAddresses(
                                ImmutableList.of(
                                    "external-researcher@sanger.uk", "science@aol.com")))));

    // fail even if address or domain matches
    assertThat(
            service.validateInstitutionalEmail(
                inst, "external-researcher@sanger.uk", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
    assertThat(
            service.validateInstitutionalEmail(inst, "yy@verily.com", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
  }

  @Test
  public void test_emailValidation_domain_ignoreCase() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("BROAD.org", "verily.COM"))
                            .emailAddresses(
                                ImmutableList.of(
                                    "external-researcher@sanger.uk", "science@aol.com")))));

    assertThat(
            service.validateInstitutionalEmail(inst, "yy@verily.com", REGISTERED_TIER_SHORT_NAME))
        .isTrue();
    // malformed
    assertThat(
            service.validateInstitutionalEmail(
                inst, "yy@hacker@verily.org", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
    // Fail even when domain matches, because the requirement is DOMAINS.
    assertThat(
            service.validateInstitutionalEmail(
                inst, "external-researcher@sanger.uk", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
  }

  @Test
  public void test_emailValidation_null_requirement() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));
    assertThat(
            service.validateInstitutionalEmail(
                inst, "external-researcher@sanger.uk", REGISTERED_TIER_SHORT_NAME))
        .isFalse();
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
    institution.setShortName(OPERATIONAL_USER_INSTITUTION_SHORT_NAME);
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
                  .tierConfigs(Collections.emptyList())
                  .userInstructions("Should throw exception");
          service.createInstitution(institution_NoOrgType);
        });
  }

  @Test
  public void test_createInstitution_IncorrectEmailAddressFormat() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Institution institution_EmailAddress =
              new Institution()
                  .displayName("No Organization")
                  .tierConfigs(
                      ImmutableList.of(
                          rtTierConfig
                              .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                              .accessTierShortName(registeredTier.getShortName())
                              .emailAddresses(
                                  ImmutableList.of(
                                      "CorrectEmailAddress@domain.com, incorrectEmail.com"))))
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
                  .tierConfigs(
                      ImmutableList.of(
                          rtTierConfig
                              .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                              .accessTierShortName(registeredTier.getShortName())))
                  .organizationTypeEnum(OrganizationType.OTHER);
          service.createInstitution(institution_withOtherOrganizationType);
        });
  }

  @Test
  public void test_createInstitution_OtherOrganizationType() {
    Institution institution_withOtherOrganizationType =
        new Institution()
            .displayName("     ")
            .tierConfigs(
                ImmutableList.of(
                    rtTierConfig
                        .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                        .accessTierShortName(registeredTier.getShortName())))
            .organizationTypeEnum(OrganizationType.OTHER)
            .organizationTypeOtherText("Some text")
            .bypassInitialCreditsExpiration(false);
    assertThat(service.createInstitution(institution_withOtherOrganizationType))
        .isEqualTo(institution_withOtherOrganizationType);
  }

  @Test
  public void test_updateInstitution_RemoveUserInstructionFromExistingInstitution() {
    Institution institutionWithUserInstructions =
        new Institution()
            .displayName("No Organization")
            .tierConfigs(
                List.of(
                    rtTierConfig
                        .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                        .accessTierShortName(registeredTier.getShortName())))
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .userInstructions("Some user instructions")
            .bypassInitialCreditsExpiration(true);
    Institution createdInstitution = service.createInstitution(institutionWithUserInstructions);
    assertThat(createdInstitution.getUserInstructions()).isEqualTo("Some user instructions");

    Institution institutionNoUserInstruction = institutionWithUserInstructions.userInstructions("");

    Institution expectedUpdateInstitution = institutionWithUserInstructions.userInstructions(null);
    assertThat(
            service.updateInstitution(
                institutionWithUserInstructions.getShortName(), institutionNoUserInstruction))
        .hasValue(expectedUpdateInstitution);
  }

  @Test
  public void testEligibleTiers_institutionNotFound() {
    final DbUser user = createUser("user@broad.org");
    assertThat(service.getUserTierEligibilities(user)).isEmpty();
  }

  @Test
  public void testEligibleTiers_registeredTier() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com")))));
    final DbUser user = createUser("user@broad.org");
    createAffiliation(user, inst.getShortName());
    assertThat(service.getUserTierEligibilities(user))
        .containsExactly(
            new UserTierEligibility()
                .accessTierShortName(registeredTier.getShortName())
                .eligible(true));
  }

  @Test
  public void testEligibleTiers_registeredTierAndControlled() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com")),
                        ctTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                            .accessTierShortName(controlledTier.getShortName())
                            .emailAddresses(ImmutableList.of("user@broad.org")))));
    final DbUser user = createUser("user@broad.org");
    createAffiliation(user, inst.getShortName());
    assertThat(service.getUserTierEligibilities(user))
        .containsExactly(
            new UserTierEligibility()
                .accessTierShortName(registeredTier.getShortName())
                .eligible(true),
            new UserTierEligibility()
                .accessTierShortName(controlledTier.getShortName())
                .eligible(true));
  }

  @Test
  public void testEligibleTiers_institutionHasRtAndCt_userEligibleForRt() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com")),
                        ctTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                            .accessTierShortName(controlledTier.getShortName())
                            .emailAddresses(ImmutableList.of("user2@broad.org")))));
    final DbUser user = createUser("user@broad.org");
    createAffiliation(user, inst.getShortName());
    assertThat(service.getUserTierEligibilities(user))
        .containsExactly(
            new UserTierEligibility()
                .accessTierShortName(registeredTier.getShortName())
                .eligible(true),
            new UserTierEligibility()
                .accessTierShortName(controlledTier.getShortName())
                .eligible(false));
  }

  @Test
  public void testEligibleTiers_institutionHasRtAndCt_institutionNotSignedForCt() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailDomains(ImmutableList.of("broad.org", "verily.com")),
                        ctTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.NO_ACCESS)
                            .accessTierShortName(registeredTier.getShortName()))));
    final DbUser user = createUser("user@broad.org");
    createAffiliation(user, inst.getShortName());
    assertThat(service.getUserTierEligibilities(user))
        .containsExactly(
            new UserTierEligibility()
                .accessTierShortName(registeredTier.getShortName())
                .eligible(true));
  }

  @Test
  public void testEligibleTiers_emailNotMatch() {
    final Institution inst =
        service.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                .tierConfigs(
                    ImmutableList.of(
                        rtTierConfig
                            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                            .accessTierShortName(registeredTier.getShortName())
                            .emailAddresses(ImmutableList.of("user@broad.org")))));
    final DbUser user = createUser("user2@broad.org");
    createAffiliation(user, inst.getShortName());
    assertThat(service.getUserTierEligibilities(user))
        .containsExactly(
            new UserTierEligibility()
                .accessTierShortName(registeredTier.getShortName())
                .eligible(false));
  }

  @Test
  public void testShouldBypassForCreditsExpiration_noInstitution() {
    final DbUser user = createUser("user@broad.org");
    assertThat(service.shouldBypassForCreditsExpiration(user)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testShouldBypassForCreditsExpiration_withInstitution(boolean bypass) {
    final DbUser user = createUser("user@broad.org");
    createAffiliation(user, testInst.getShortName(), bypass);
    assertThat(service.shouldBypassForCreditsExpiration(user)).isEqualTo(bypass);
  }

  private DbUser createUser(String contactEmail) {
    DbUser user = new DbUser();
    user.setContactEmail(contactEmail);
    user = userDao.save(user);
    return user;
  }

  private DbVerifiedInstitutionalAffiliation createAffiliation(
      final DbUser user, final String instName) {
    return createAffiliation(user, instName, false);
  }

  private DbVerifiedInstitutionalAffiliation createAffiliation(
      final DbUser user, final String instName, boolean bypassInitialCreditsExpiration) {
    final DbInstitution inst = service.getDbInstitutionOrThrow(instName);
    inst.setBypassInitialCreditsExpiration(bypassInitialCreditsExpiration);
    final DbVerifiedInstitutionalAffiliation affiliation =
        new DbVerifiedInstitutionalAffiliation()
            .setUser(user)
            .setInstitution(inst)
            .setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    return verifiedInstitutionalAffiliationDao.save(affiliation);
  }
}
