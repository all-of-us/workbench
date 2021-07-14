package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.RequirementEnum;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@Import({CommonConfig.class})
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionTierRequirementDaoTest extends SpringTest {

  @Autowired InstitutionDao institutionDao;
  @Autowired AccessTierDao accessTierDao;
  @Autowired InstitutionTierRequirementDao institutionTierRequirementDao;

  private DbInstitution testInst;
  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;

  @BeforeEach
  public void setUp() {
    testInst =
        institutionDao.save(
            new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute"));
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
  }

  @Test
  public void test_getByInstitution_empty() {
    assertThat(institutionTierRequirementDao.getByInstitution(testInst)).isEmpty();
    assertThat(institutionTierRequirementDao.count()).isEqualTo(0L);
  }

  @Test
  public void test_insertGetDeleteByInstitution() {
    final DbInstitutionTierRequirement rtRequirement =
        institutionTierRequirementDao.save(
            new DbInstitutionTierRequirement()
                .setAccessTier(registeredTier)
                .setInstitution(testInst)
                .setRequirementEnum(RequirementEnum.DOMAINS)
                .setEraRequired(true));
    final DbInstitutionTierRequirement ctRequirement =
        institutionTierRequirementDao.save(
            new DbInstitutionTierRequirement()
                .setAccessTier(controlledTier)
                .setInstitution(testInst)
                .setRequirementEnum(RequirementEnum.ADDRESSES)
                .setEraRequired(true));

    assertThat(institutionTierRequirementDao.getByInstitution(testInst))
        .containsExactly(rtRequirement, ctRequirement);

    // Then delete by institution, verify empty after deletion
    institutionTierRequirementDao.deleteByInstitution(testInst);
    assertThat(institutionTierRequirementDao.getByInstitution(testInst)).isEmpty();
  }
}
