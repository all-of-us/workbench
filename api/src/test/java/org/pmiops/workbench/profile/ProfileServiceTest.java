package org.pmiops.workbench.profile;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.institution.deprecated.InstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ProfileServiceTest {
  private static final FakeClock CLOCK = new FakeClock(Instant.parse("2000-01-01T00:00:00.00Z"));

  private static final DbInstitution BROAD_INSTITUTION =
      new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

  private static final VerifiedInstitutionalAffiliation BROAD_AFFILIATION =
      new VerifiedInstitutionalAffiliation()
          .institutionalRoleOtherText("")
          .institutionShortName("Broad")
          .institutionDisplayName("The Broad Institute")
          .institutionalRoleEnum(InstitutionalRole.ADMIN);

  private static Profile createValidProfile() {
    return new Profile()
        .username("jdoe123")
        .contactEmail("jdoe123@gmail.com")
        .address(
            new Address()
                .streetAddress1("1 Example Lane")
                .city("Boston")
                .state("MA")
                .country("USA")
                .zipCode("12345"))
        .givenName("Jane")
        .familyName("Doe")
        .professionalUrl("https://scholar.google.com/citations?user=asdf")
        .areaOfResearch("asdfasdfasdf")
        .verifiedInstitutionalAffiliation(BROAD_AFFILIATION);
  }

  private static final Profile VALID_PROFILE = createValidProfile();

  @MockBean private InstitutionDao mockInstitutionDao;
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserTermsOfServiceDao mockUserTermsOfServiceDao;

  @MockBean
  private VerifiedInstitutionalAffiliationMapper mockVerifiedInstitutionalAffiliationMapper;

  @MockBean private VerifiedInstitutionalAffiliationDao mockVerifiedInstitutionalAffiliationDao;

  @Autowired ProfileService profileService;
  @Autowired UserDao userDao;

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @MockBean({FreeTierBillingService.class, UserService.class})
  @Import({
    AddressMapperImpl.class,
    DemographicSurveyMapperImpl.class,
    InstitutionalAffiliationMapperImpl.class,
    PageVisitMapperImpl.class,
    ProfileMapperImpl.class,
    ProfileService.class,
    CommonMappers.class,
    CommonConfig.class,
  })
  @MockBean({ProfileAuditor.class})
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }

    @Bean
    public Clock getClock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
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
    Profile profile =
        new Profile()
            .verifiedInstitutionalAffiliation(BROAD_AFFILIATION)
            .contactEmail("kibitz@broadinstitute.org");

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));

    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setInstitution(BROAD_INSTITUTION);
    dbVerifiedInstitutionalAffiliation.setInstitutionalRoleEnum(InstitutionalRole.ADMIN);
    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            BROAD_AFFILIATION, mockInstitutionService))
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

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));

    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setInstitution(BROAD_INSTITUTION);
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

  @Test(expected = BadRequestException.class)
  public void validateInstitutionalAffiliation_noAffiliation() {
    profileService.validateInstitutionalAffiliation(new Profile());
  }

  @Test(expected = NotFoundException.class)
  public void validateInstitutionalAffiliation_noInstitution() {
    Profile profile = new Profile().verifiedInstitutionalAffiliation(BROAD_AFFILIATION);

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

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));

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

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));

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

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));

    when(mockInstitutionService.validateAffiliation(
            any(DbVerifiedInstitutionalAffiliation.class), anyString()))
        .thenReturn(false);

    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setInstitution(BROAD_INSTITUTION);

    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            affiliation, mockInstitutionService))
        .thenReturn(dbVerifiedInstitutionalAffiliation);

    profileService.validateInstitutionalAffiliation(profile);
  }

  @Test
  public void updateProfileForUser_affiliationChangeOnly() {
    // Regression test for RW-5139

    // Start with: a valid profile with a Broad affiliation but null Address.
    Profile previousProfile = createValidProfile().address(null);

    // Target state: new affiliation, still null address.
    VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Verily")
            .institutionDisplayName("Verily LLC")
            .institutionalRoleEnum(InstitutionalRole.ADMIN);
    Profile updatedProfile =
        createValidProfile().address(null).verifiedInstitutionalAffiliation(newAffiliation);

    DbUser user = new DbUser();
    user.setUserId(1);
    user.setGivenName("John");
    user.setFamilyName("Doe");

    DbInstitution verilyInstitution =
        new DbInstitution().setShortName("Verily").setDisplayName("Verily LLC");

    when(mockInstitutionDao.findOneByShortName("Verily"))
        .thenReturn(Optional.of(verilyInstitution));
    when(mockInstitutionService.validateAffiliation(any(), any())).thenReturn(true);
    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation().setInstitution(BROAD_INSTITUTION);
    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            newAffiliation, mockInstitutionService))
        .thenReturn(dbVerifiedInstitutionalAffiliation);

    profileService.updateProfileForUser(user, updatedProfile, previousProfile);
  }

  @Test
  public void validateProfile_noChangesOnEmptyProfile() {
    // This is a synthetic test case: we never expect to be updating an empty Profile object, but
    // technically this passes validation since no fields have changed.
    profileService.validateProfile(new Profile(), new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_emptyNewObject() {
    // The 'prevProfile' argument is null, so this is considered a new object and all validation
    // is executed. The empty Profile object is obviously rejected.
    profileService.validateNewProfile(new Profile());
  }

  @Test
  public void validateProfile_validNewObject() {
    // This positive test case should demonstrate applying all profile validation. VALID_PROFILE
    // must successfully pass all required validation steps in order to succeed.
    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));
    when(mockInstitutionService.validateAffiliation(any(), eq(VALID_PROFILE.getContactEmail())))
        .thenReturn(true);
    profileService.validateNewProfile(VALID_PROFILE);
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_usernameChanged() {
    // Username changes are disallowed.
    profileService.validateProfile(new Profile().username("new"), new Profile().username("old"));
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_usernameTooShort() {
    profileService.validateProfile(new Profile().username("ab"), new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_usernameTooLong() {
    profileService.validateProfile(
        new Profile().username(StringUtils.repeat("asdf", 30)), new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_contactEmailChanged() {
    // Contact email changes are disallowed.
    profileService.validateProfile(
        new Profile().contactEmail("new@gmail.com"), new Profile().contactEmail("old@gmail.com"));
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_givenNameTooShort() {
    Profile newProfile = new Profile().givenName("");
    profileService.validateProfile(newProfile, new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_FamilyNameTooLong() {
    Profile newProfile = new Profile().familyName(StringUtils.repeat("123", 30));
    profileService.validateProfile(newProfile, new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_addressRemoved() {
    // Ensures we validate the address when the field has been removed entirely.
    Profile oldProfile = new Profile().address(new Address().streetAddress1("asdf"));
    Profile newProfile = new Profile();
    profileService.validateProfile(newProfile, oldProfile);
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_addressRemoveZipCode() {
    // Ensures we validate the address when only a subfield has changed.
    Address oldAddress =
        new Address()
            .streetAddress1("asdf")
            .city("asdf")
            .state("asdf")
            .country("asdf")
            .zipCode("asdf");
    Address newAddress =
        new Address().streetAddress1("asdf").city("asdf").state("asdf").country("asdf");
    profileService.validateProfile(
        new Profile().address(newAddress), new Profile().address(oldAddress));
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_addressIncomplete() {
    Profile newProfile = new Profile().address(new Address().streetAddress1("asdf"));
    profileService.validateProfile(newProfile, new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_EmptyAreaOfResearch() {
    Profile newProfile = new Profile().areaOfResearch("");
    profileService.validateProfile(newProfile, new Profile());
  }

  @Test(expected = BadRequestException.class)
  public void validateProfile_noInstitutionMatch() {
    Profile newProfile =
        new Profile()
            .contactEmail("not-a-match@gmail.com")
            .verifiedInstitutionalAffiliation(VALID_PROFILE.getVerifiedInstitutionalAffiliation());

    when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.of(BROAD_INSTITUTION));
    when(mockInstitutionService.validateAffiliation(any(), any())).thenReturn(false);

    profileService.validateProfile(newProfile, new Profile());
  }
}
