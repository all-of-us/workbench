package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.RequirementEnum;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionRequirementEnum;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionTierRequirementMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionTierRequirementMapperTest extends SpringTest {
  @Autowired InstitutionTierRequirementMapper mapper;
  private static final String RT_ACCESS_TIER_SHORT_NAME = "registered";
  private static final String CT_ACCESS_TIER_SHORT_NAME = "controlled";
  private static final DbAccessTier RT_ACCESS_TIER = new DbAccessTier().setShortName(RT_ACCESS_TIER_SHORT_NAME).setAccessTierId(111);
  private static final DbAccessTier CT_ACCESS_TIER = new DbAccessTier().setShortName(CT_ACCESS_TIER_SHORT_NAME).setAccessTierId(222);
  private static final List<DbAccessTier> ACCESS_TIERS = ImmutableList.of(
      new DbAccessTier().setShortName(RT_ACCESS_TIER_SHORT_NAME).setAccessTierId(111),
      new DbAccessTier().setShortName(CT_ACCESS_TIER_SHORT_NAME).setAccessTierId(222));
  private static final InstitutionTierRequirement RT_REQUIREMENT = new InstitutionTierRequirement().requirementEnum(
      InstitutionRequirementEnum.DOMAINS).eraRequired(true).accessTierShortName(RT_ACCESS_TIER_SHORT_NAME);
  private static final InstitutionTierRequirement CT_REQUIREMENT = new InstitutionTierRequirement().requirementEnum(
      InstitutionRequirementEnum.NO_ACCESS).accessTierShortName(CT_ACCESS_TIER_SHORT_NAME);

  @Test
  public void tesModelToDbSuccess() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierRequirements(ImmutableList.of(RT_REQUIREMENT, CT_REQUIREMENT));

    // does not need to match the modelInst; it is simply attached to the DbInstitutionTierRequirement
    final DbInstitution dbInst = new DbInstitution();

    final List<DbInstitutionTierRequirement> expectedResult = ImmutableList.of(
        new DbInstitutionTierRequirement().setAccessTier(RT_ACCESS_TIER).setInstitution(dbInst).setRequirementEnum(
            RequirementEnum.DOMAINS).setEraRequired(true),
        new DbInstitutionTierRequirement().setAccessTier(CT_ACCESS_TIER).setInstitution(dbInst).setRequirementEnum(
            RequirementEnum.NO_ACCESS).setEraRequired(false)
        );

    assertThat(mapper.modelToDb(modelInst, dbInst, ACCESS_TIERS)).isEqualTo(expectedResult);
  }

  @Test
  public void tesModelToDb_null() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierRequirements(null);

    assertThat(mapper.modelToDb(modelInst, new DbInstitution(), ACCESS_TIERS)).isEmpty();
  }

  @Test
  public void tesModelToDbError_accessTierNotFound() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierRequirements(ImmutableList.of(RT_REQUIREMENT, CT_REQUIREMENT));

    assertThrows(NotFoundException.class, () -> assertThat(mapper.modelToDb(modelInst, new DbInstitution(), ImmutableList.of(CT_ACCESS_TIER))));
  }

  @Test
  public void tesDbToModelSuccess() {
    // does not need to match the modelInst; it is simply attached to the DbInstitutionTierRequirement
    final DbInstitution dbInst = new DbInstitution();
    final List<DbInstitutionTierRequirement> source = ImmutableList.of(
        new DbInstitutionTierRequirement().setAccessTier(RT_ACCESS_TIER).setInstitution(dbInst).setRequirementEnum(
            RequirementEnum.DOMAINS).setEraRequired(true),
        new DbInstitutionTierRequirement().setAccessTier(CT_ACCESS_TIER).setInstitution(dbInst).setRequirementEnum(
            RequirementEnum.NO_ACCESS).setEraRequired(false)
    );

    assertThat(mapper.dbToModel(source)).isEqualTo(ImmutableList.of(RT_REQUIREMENT, CT_REQUIREMENT));
  }
}
