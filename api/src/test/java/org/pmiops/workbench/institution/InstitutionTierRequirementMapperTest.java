package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionTierRequirementMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionTierRequirementMapperTest extends SpringTest {
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
  private static final InstitutionTierRequirement RT_REQUIREMENT =
      new InstitutionTierRequirement()
          .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
          .eraRequired(true)
          .accessTierShortName(RT_ACCESS_TIER_SHORT_NAME);
  private static final InstitutionTierRequirement CT_REQUIREMENT =
      new InstitutionTierRequirement()
          .membershipRequirement(InstitutionMembershipRequirement.NO_ACCESS)
          .accessTierShortName(CT_ACCESS_TIER_SHORT_NAME);

  @Autowired InstitutionTierRequirementMapper mapper;

  @Test
  public void testModelToDbSuccess() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierRequirements(ImmutableList.of(RT_REQUIREMENT, CT_REQUIREMENT));

    // does not need to match the modelInst; it is simply attached to the
    // DbInstitutionTierRequirement
    final DbInstitution dbInst = new DbInstitution();

    final List<DbInstitutionTierRequirement> expectedResult =
        ImmutableList.of(
            new DbInstitutionTierRequirement()
                .setAccessTier(RT_ACCESS_TIER)
                .setInstitution(dbInst)
                .setMembershipRequirement(MembershipRequirement.DOMAINS)
                .setEraRequired(true),
            new DbInstitutionTierRequirement()
                .setAccessTier(CT_ACCESS_TIER)
                .setInstitution(dbInst)
                .setMembershipRequirement(MembershipRequirement.NO_ACCESS)
                .setEraRequired(false));

    assertThat(mapper.modelToDb(modelInst, dbInst, ACCESS_TIERS)).isEqualTo(expectedResult);
  }

  @Test
  public void testModelToDb_null() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierRequirements(null);

    assertThat(mapper.modelToDb(modelInst, new DbInstitution(), ACCESS_TIERS)).isEmpty();
  }

  @Test
  public void testModelToDbError_accessTierNotFound() {
    // Expect throws NotFoundException if the tier specified in Institution model not found in
    // all tiers passed to the mapper(in API, it happened when that tier not found in DB).
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierRequirements(ImmutableList.of(RT_REQUIREMENT, CT_REQUIREMENT));

    assertThrows(
        NotFoundException.class,
        () ->
            assertThat(
                mapper.modelToDb(
                    modelInst, new DbInstitution(), ImmutableList.of(CT_ACCESS_TIER))));
  }

  @Test
  public void testDbToModelSuccess() {
    // does not need to match the modelInst; it is simply attached to the
    // DbInstitutionTierRequirement
    final DbInstitution dbInst = new DbInstitution();
    final List<DbInstitutionTierRequirement> source =
        ImmutableList.of(
            new DbInstitutionTierRequirement()
                .setAccessTier(RT_ACCESS_TIER)
                .setInstitution(dbInst)
                .setMembershipRequirement(MembershipRequirement.DOMAINS)
                .setEraRequired(true),
            new DbInstitutionTierRequirement()
                .setAccessTier(CT_ACCESS_TIER)
                .setInstitution(dbInst)
                .setMembershipRequirement(MembershipRequirement.NO_ACCESS)
                .setEraRequired(false));

    // Expect CT_REQUIREMENT to be false instead of null.
    assertThat(mapper.dbToModel(source))
        .isEqualTo(ImmutableList.of(RT_REQUIREMENT, CT_REQUIREMENT.eraRequired(false)));
  }
}
