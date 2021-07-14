package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import({
  VerifiedInstitutionalAffiliationMapperImpl.class,
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class,
  InstitutionUserInstructionsMapperImpl.class,
  InstitutionEmailDomainMapperImpl.class,
  InstitutionEmailAddressMapperImpl.class,
  InstitutionTierRequirementMapperImpl.class,
})
@MockBean({
  AccessTierService.class,
})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class VerifiedInstitutionalAffiliationMapperTest extends SpringTest {
  @Autowired VerifiedInstitutionalAffiliationMapper mapper;

  @Autowired InstitutionService institutionService;
  @Autowired UserDao userDao;

  private Institution testInstitution;

  @BeforeEach
  public void setUp() {
    testInstitution =
        institutionService.createInstitution(
            new Institution()
                .shortName("Broad")
                .displayName("The Broad Institute")
                .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION));
  }

  @Test
  public void test_setDbInstitution() {
    final VerifiedInstitutionalAffiliation source =
        new VerifiedInstitutionalAffiliation().institutionShortName(testInstitution.getShortName());
    final DbVerifiedInstitutionalAffiliation target = new DbVerifiedInstitutionalAffiliation();

    mapper.setDbInstitution(target, source, institutionService);

    assertThat(target.getInstitution().getShortName()).isEqualTo(testInstitution.getShortName());
  }

  @Test
  public void test_setDbInstitution_missing() {
    final VerifiedInstitutionalAffiliation source =
        new VerifiedInstitutionalAffiliation().institutionShortName("not in DB");
    final DbVerifiedInstitutionalAffiliation target = new DbVerifiedInstitutionalAffiliation();

    assertThrows(
        NotFoundException.class, () -> mapper.setDbInstitution(target, source, institutionService));
  }

  @Test
  public void test_dbToModel() {
    final DbUser dbUser = userDao.save(new DbUser());
    final DbVerifiedInstitutionalAffiliation dbAffiliation =
        new DbVerifiedInstitutionalAffiliation()
            .setUser(dbUser)
            .setInstitution(
                institutionService.getDbInstitutionOrThrow(testInstitution.getShortName()))
            .setInstitutionalRoleEnum(InstitutionalRole.FELLOW)
            .setInstitutionalRoleOtherText("A fine fellow, specifically");

    final VerifiedInstitutionalAffiliation affiliation = mapper.dbToModel(dbAffiliation);

    assertThat(affiliation.getInstitutionShortName())
        .isEqualTo(dbAffiliation.getInstitution().getShortName());
    assertThat(affiliation.getInstitutionDisplayName())
        .isEqualTo(dbAffiliation.getInstitution().getDisplayName());
    assertThat(affiliation.getInstitutionalRoleEnum())
        .isEqualTo(dbAffiliation.getInstitutionalRoleEnum());
    assertThat(affiliation.getInstitutionalRoleOtherText())
        .isEqualTo(dbAffiliation.getInstitutionalRoleOtherText());

    // note: testInstitution needs to be saved in the DB for this call to work
    final DbVerifiedInstitutionalAffiliation roundTrip =
        mapper.modelToDbWithoutUser(affiliation, institutionService);

    assertThat(roundTrip.getInstitution()).isEqualTo(dbAffiliation.getInstitution());
    assertThat(roundTrip.getInstitutionalRoleEnum())
        .isEqualTo(dbAffiliation.getInstitutionalRoleEnum());
    assertThat(roundTrip.getInstitutionalRoleOtherText())
        .isEqualTo(dbAffiliation.getInstitutionalRoleOtherText());
  }

  @Test
  public void test_modelToDbWithoutUser() {
    // final DbUser dbUser = userDao.save(new DbUser());
    final VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(testInstitution.getShortName())
            .institutionDisplayName(testInstitution.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.FELLOW)
            .institutionalRoleOtherText("A fine fellow, specifically");

    // note: testInstitution needs to be saved in the DB for this call to work
    final DbVerifiedInstitutionalAffiliation dbAffiliation =
        mapper.modelToDbWithoutUser(affiliation, institutionService);

    assertThat(dbAffiliation.getInstitution().getShortName())
        .isEqualTo(affiliation.getInstitutionShortName());
    assertThat(dbAffiliation.getInstitution().getDisplayName())
        .isEqualTo(affiliation.getInstitutionDisplayName());
    assertThat(dbAffiliation.getInstitutionalRoleEnum())
        .isEqualTo(affiliation.getInstitutionalRoleEnum());
    assertThat(dbAffiliation.getInstitutionalRoleOtherText())
        .isEqualTo(affiliation.getInstitutionalRoleOtherText());

    final VerifiedInstitutionalAffiliation roundTrip = mapper.dbToModel(dbAffiliation);

    assertThat(roundTrip.getInstitutionShortName())
        .isEqualTo(affiliation.getInstitutionShortName());
    assertThat(roundTrip.getInstitutionDisplayName())
        .isEqualTo(affiliation.getInstitutionDisplayName());
    assertThat(roundTrip.getInstitutionalRoleEnum())
        .isEqualTo(affiliation.getInstitutionalRoleEnum());
    assertThat(roundTrip.getInstitutionalRoleOtherText())
        .isEqualTo(affiliation.getInstitutionalRoleOtherText());
  }
}
