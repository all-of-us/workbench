package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import({InstitutionTierConfigMapperImpl.class, FakeClockConfiguration.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionTierConfigMapperTest {
  private static final String RT_ACCESS_TIER_SHORT_NAME = "registered";
  private static final String CT_ACCESS_TIER_SHORT_NAME = "controlled";
  private static final DbAccessTier RT_ACCESS_TIER =
      new DbAccessTier().setShortName(RT_ACCESS_TIER_SHORT_NAME).setAccessTierId(111);
  private static final DbAccessTier CT_ACCESS_TIER =
      new DbAccessTier().setShortName(CT_ACCESS_TIER_SHORT_NAME).setAccessTierId(222);
  private static final List<DbAccessTier> ACCESS_TIERS =
      ImmutableList.of(
          new DbAccessTier().setShortName(RT_ACCESS_TIER_SHORT_NAME).setAccessTierId(111),
          new DbAccessTier().setShortName(CT_ACCESS_TIER_SHORT_NAME).setAccessTierId(222));

  @Autowired InstitutionTierConfigMapper mapper;

  @Test
  public void testModelToTierRequirementSuccess() {
    // does not need to match the modelInst; it is simply attached to the
    // DbInstitutionTierRequirement
    final DbInstitution dbInst = new DbInstitution();

    InstitutionTierConfig rtTierConfig =
        new InstitutionTierConfig()
            .accessTierShortName(RT_ACCESS_TIER_SHORT_NAME)
            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS);
    InstitutionTierConfig ctTierConfig =
        new InstitutionTierConfig()
            .accessTierShortName(CT_ACCESS_TIER_SHORT_NAME)
            .membershipRequirement(InstitutionMembershipRequirement.NO_ACCESS);

    final List<DbInstitutionTierRequirement> expectedResult =
        ImmutableList.of(
            new DbInstitutionTierRequirement()
                .setAccessTier(RT_ACCESS_TIER)
                .setInstitution(dbInst)
                .setMembershipRequirement(MembershipRequirement.DOMAINS),
            new DbInstitutionTierRequirement()
                .setAccessTier(CT_ACCESS_TIER)
                .setInstitution(dbInst)
                .setMembershipRequirement(MembershipRequirement.NO_ACCESS));

    assertThat(
            mapper.tierConfigsToDbTierRequirements(
                ImmutableList.of(rtTierConfig, ctTierConfig), dbInst, ACCESS_TIERS))
        .isEqualTo(expectedResult);
  }

  @Test
  public void testModelToTierRequirement_null() {
    assertThat(mapper.tierConfigsToDbTierRequirements(null, new DbInstitution(), ACCESS_TIERS))
        .isEmpty();
  }

  @Test
  public void testModelToTierRequirementError_accessTierNotFound() {
    // Expect throws NotFoundException if the tier specified in Institution model not found in
    // all tiers passed to the mapper(in API, it happened when that tier not found in DB).
    assertThrows(
        NotFoundException.class,
        () ->
            assertThat(
                mapper.tierConfigsToDbTierRequirements(
                    ImmutableList.of(
                        new InstitutionTierConfig()
                            .accessTierShortName(RT_ACCESS_TIER_SHORT_NAME)
                            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)),
                    new DbInstitution(),
                    ImmutableList.of(CT_ACCESS_TIER))));
  }

  @Test
  public void test_modelToDbEmailDomain() {
    // contains an out-of-order duplicate
    final List<String> rtRawDomains = Lists.newArrayList("nih.gov", "other-inst.org", "nih.gov");
    final List<String> ctRawDomains = Lists.newArrayList("ct.gov");
    final SortedSet<String> sortedRtDistinctDomains = new TreeSet<>(rtRawDomains);
    final SortedSet<String> sortedCtDistinctDomains = new TreeSet<>(ctRawDomains);

    InstitutionTierConfig rtTierConfig = new InstitutionTierConfig().emailDomains(rtRawDomains);
    InstitutionTierConfig ctTierConfig = new InstitutionTierConfig().emailDomains(ctRawDomains);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailDomain> rtDbDomains =
        rtTierConfig.getEmailDomains().stream()
            .map(domain -> mapper.emailDomainToDb(domain, dbInst, RT_ACCESS_TIER))
            .collect(Collectors.toSet());
    final Set<DbInstitutionEmailDomain> ctDbDomains =
        ctTierConfig.getEmailDomains().stream()
            .map(domain -> mapper.emailDomainToDb(domain, dbInst, CT_ACCESS_TIER))
            .collect(Collectors.toSet());

    assertThat(rtDbDomains).hasSize(sortedRtDistinctDomains.size());
    assertThat(ctDbDomains).hasSize(sortedCtDistinctDomains.size());

    for (final DbInstitutionEmailDomain dbDomain : rtDbDomains) {
      assertThat(sortedRtDistinctDomains).contains(dbDomain.getEmailDomain());
      assertThat(dbDomain.getInstitution()).isEqualTo(dbInst);
    }
    for (final DbInstitutionEmailDomain dbDomain : ctDbDomains) {
      assertThat(sortedCtDistinctDomains).contains(dbDomain.getEmailDomain());
      assertThat(dbDomain.getInstitution()).isEqualTo(dbInst);
    }
  }

  @Test
  public void test_modelToDbEmailAddress() {
    // contains an out-of-order duplicate
    final List<String> rtRawAddresses = Lists.newArrayList("nih.gov", "other-inst.org", "nih.gov");
    final List<String> ctRawAddresses = Lists.newArrayList("ct.gov");
    final SortedSet<String> sortedRtDistinctAddresses = new TreeSet<>(rtRawAddresses);
    final SortedSet<String> sortedCtDistinctAddresses = new TreeSet<>(ctRawAddresses);

    InstitutionTierConfig rtTierConfig = new InstitutionTierConfig().emailAddresses(rtRawAddresses);
    InstitutionTierConfig ctTierConfig = new InstitutionTierConfig().emailAddresses(ctRawAddresses);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailAddress> rtDbAddresses =
        rtTierConfig.getEmailAddresses().stream()
            .map(domain -> mapper.emailAddressToDb(domain, dbInst, RT_ACCESS_TIER))
            .collect(Collectors.toSet());
    final Set<DbInstitutionEmailAddress> ctDbAddresses =
        ctTierConfig.getEmailAddresses().stream()
            .map(domain -> mapper.emailAddressToDb(domain, dbInst, CT_ACCESS_TIER))
            .collect(Collectors.toSet());

    assertThat(rtDbAddresses).hasSize(sortedRtDistinctAddresses.size());
    assertThat(ctDbAddresses).hasSize(sortedCtDistinctAddresses.size());

    for (final DbInstitutionEmailAddress dbAddress : rtDbAddresses) {
      assertThat(sortedRtDistinctAddresses).contains(dbAddress.getEmailAddress());
      assertThat(dbAddress.getInstitution()).isEqualTo(dbInst);
    }
    for (final DbInstitutionEmailAddress dbAddress : ctDbAddresses) {
      assertThat(sortedCtDistinctAddresses).contains(dbAddress.getEmailAddress());
      assertThat(dbAddress.getInstitution()).isEqualTo(dbInst);
    }
  }

  @Test
  public void testDbToModelSuccess() {
    // does not need to match the modelInst; it is simply attached to the
    // DbInstitutionTierRequirement
    final DbInstitution dbInst = new DbInstitution();
    final List<String> emailAddresses = Lists.newArrayList("bar@other-inst.org", "foo@nih.gov");
    final DbInstitutionTierRequirement tierRequirement =
        new DbInstitutionTierRequirement()
            .setAccessTier(RT_ACCESS_TIER)
            .setInstitution(dbInst)
            .setMembershipRequirement(MembershipRequirement.DOMAINS);

    InstitutionTierConfig expectedTierConfig =
        new InstitutionTierConfig()
            .accessTierShortName(RT_ACCESS_TIER_SHORT_NAME)
            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
            .emailAddresses(emailAddresses);
    assertThat(mapper.dbToTierConfigModel(tierRequirement, new TreeSet<>(emailAddresses), null))
        .isEqualTo(expectedTierConfig);
  }
}
