package org.pmiops.workbench.profile;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.institution.deprecated.InstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ProfileServiceTest {

  @MockBean private InstitutionDao mockInstitutionDao;
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserTermsOfServiceDao mockUserTermsOfServiceDao;

  @MockBean
  private VerifiedInstitutionalAffiliationMapper mockVerifiedInstitutionalAffiliationMapper;

  @Autowired ProfileService profileService;
  @Autowired UserDao userDao;

  DbInstitution dbInstitution;

  @TestConfiguration
  @MockBean({FreeTierBillingService.class, UserService.class})
  @Import({
    AddressMapperImpl.class,
    DemographicSurveyMapperImpl.class,
    InstitutionalAffiliationMapperImpl.class,
    PageVisitMapperImpl.class,
    ProfileMapperImpl.class,
    ProfileServiceImpl.class,
    VerifiedInstitutionalAffiliationMapperImpl.class,
    CommonMappers.class
  })
  static class Configuration {}

  @Before
  public void setUp() {
    dbInstitution = new DbInstitution();
    dbInstitution.setShortName("Broad");
    dbInstitution.setDisplayName("The Broad Institute");
  }

  @Test
  public void testGetProfile_empty() {
    assertThat(profileService.getProfile(userDao.save(new DbUser()))).isNotNull();
  }

  @Test
  public void testGetProfile_emptyDemographics() {
    // Regression coverage for RW-4219.
    DbUser user = new DbUser();
    user.setDemographicSurvey(new DbDemographicSurvey());
    user = userDao.save(user);
    assertThat(profileService.getProfile(user)).isNotNull();
  }

  @Test
  public void testReturnsLastAcknowledgedTermsOfService() {
    DbUserTermsOfService userTermsOfService = new DbUserTermsOfService();
    userTermsOfService.setTosVersion(1);
    userTermsOfService.setAgreementTime(new Timestamp(1));
    when(mockUserTermsOfServiceDao.findFirstByUserIdOrderByTosVersionDesc(1))
        .thenReturn(Optional.of(userTermsOfService));

    DbUser user = new DbUser();
    user.setUserId(1);
    Profile profile = profileService.getProfile(user);
    assertThat(profile.getLatestTermsOfServiceVersion()).isEqualTo(1);
    assertThat(profile.getLatestTermsOfServiceTime()).isEqualTo(1);
  }

  @Test
  public void validateInstitutionalAffiliation() {
    VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.ADMIN);

    Profile profile =
        new Profile()
            .verifiedInstitutionalAffiliation(affiliation)
            .contactEmail("kibitz@broadinstitute.org");

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(dbInstitution));

    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setInstitution(dbInstitution);
    dbVerifiedInstitutionalAffiliation.setInstitutionalRoleEnum(InstitutionalRole.ADMIN);
    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            affiliation, mockInstitutionService))
        .thenReturn(dbVerifiedInstitutionalAffiliation);

    when(mockInstitutionService.validateAffiliation(
            dbVerifiedInstitutionalAffiliation, "kibitz@broadinstitute.org"))
        .thenReturn(true);

    profileService.validateInstitutionalAffiliation(profile);

    ArgumentCaptor<DbVerifiedInstitutionalAffiliation> affiliationSpy =
        ArgumentCaptor.forClass(DbVerifiedInstitutionalAffiliation.class);
    ArgumentCaptor<String> unusedSpy = ArgumentCaptor.forClass(String.class);
    verify(mockInstitutionService)
        .validateAffiliation(affiliationSpy.capture(), unusedSpy.capture());

    assertThat(affiliationSpy.getValue().getInstitution().getShortName()).isEqualTo("Broad");
  }

  @Test
  public void validateInstitutionalAffiliation_other() {
    VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.OTHER)
            .institutionalRoleOtherText("Kibitzing");

    Profile profile =
        new Profile()
            .verifiedInstitutionalAffiliation(affiliation)
            .contactEmail("kibitz@broadinstitute.org");

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(dbInstitution));

    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setInstitution(dbInstitution);
    dbVerifiedInstitutionalAffiliation.setInstitutionalRoleEnum(InstitutionalRole.ADMIN);
    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            affiliation, mockInstitutionService))
        .thenReturn(dbVerifiedInstitutionalAffiliation);

    when(mockInstitutionService.validateAffiliation(
            dbVerifiedInstitutionalAffiliation, "kibitz@broadinstitute.org"))
        .thenReturn(true);
    ;

    profileService.validateInstitutionalAffiliation(profile);

    ArgumentCaptor<DbVerifiedInstitutionalAffiliation> affiliationSpy =
        ArgumentCaptor.forClass(DbVerifiedInstitutionalAffiliation.class);
    ArgumentCaptor<String> unusedSpy = ArgumentCaptor.forClass(String.class);
    verify(mockInstitutionService)
        .validateAffiliation(affiliationSpy.capture(), unusedSpy.capture());

    assertThat(affiliationSpy.getValue().getInstitution().getShortName()).isEqualTo("Broad");
  }

  @Test(expected = BadRequestException.class)
  public void validateInstitutionalAffiliation_noAffiliation() {
    profileService.validateInstitutionalAffiliation(new Profile());
  }

  @Test(expected = NotFoundException.class)
  public void validateInstitutionalAffiliation_noInstitution() {
    VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.OTHER)
            .institutionalRoleOtherText("Kibitzing");

    Profile profile = new Profile().verifiedInstitutionalAffiliation(affiliation);

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.empty());

    profileService.validateInstitutionalAffiliation(profile);
  }

  @Test(expected = BadRequestException.class)
  public void validateInstitutionalAffiliation_noRole() {
    VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute");

    Profile profile = new Profile().verifiedInstitutionalAffiliation(affiliation);

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(dbInstitution));

    profileService.validateInstitutionalAffiliation(profile);
  }

  @Test(expected = BadRequestException.class)
  public void validateInstitutionalAffiliation_noOtherText() {
    VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.OTHER);

    Profile profile = new Profile().verifiedInstitutionalAffiliation(affiliation);

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(dbInstitution));

    profileService.validateInstitutionalAffiliation(profile);
  }

  @Test(expected = BadRequestException.class)
  public void validateInstitutionalAffilation_badEmail() {
    VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.OTHER)
            .institutionalRoleOtherText("Kibitzing");

    Profile profile =
        new Profile()
            .verifiedInstitutionalAffiliation(affiliation)
            .contactEmail("kibitz@broadinstitute.org");

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(dbInstitution));

    when(mockInstitutionService.validateAffiliation(
            any(DbVerifiedInstitutionalAffiliation.class), anyString()))
        .thenReturn(false);

    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setInstitution(dbInstitution);

    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            affiliation, mockInstitutionService))
        .thenReturn(dbVerifiedInstitutionalAffiliation);

    profileService.validateInstitutionalAffiliation(profile);
  }
}
