package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionTierRequirementDaoTest {

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
    registeredTier = accessTierDao.save(createRegisteredTier());
    controlledTier = accessTierDao.save(createControlledTier());
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
                .setMembershipRequirement(MembershipRequirement.DOMAINS));
    final DbInstitutionTierRequirement ctRequirement =
        institutionTierRequirementDao.save(
            new DbInstitutionTierRequirement()
                .setAccessTier(controlledTier)
                .setInstitution(testInst)
                .setMembershipRequirement(MembershipRequirement.ADDRESSES));

    assertThat(institutionTierRequirementDao.getByInstitution(testInst))
        .containsExactly(rtRequirement, ctRequirement);

    // Then delete by institution, verify empty after deletion
    institutionTierRequirementDao.deleteByInstitution(testInst);
    assertThat(institutionTierRequirementDao.getByInstitution(testInst)).isEmpty();
  }
}
