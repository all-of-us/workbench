package org.pmiops.workbench.profile;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
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
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.AdminTableUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DemographicSurveyV2;
import org.pmiops.workbench.model.EducationV2;
import org.pmiops.workbench.model.EthnicCategory;
import org.pmiops.workbench.model.GenderIdentityV2;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.SexAtBirthV2;
import org.pmiops.workbench.model.SexualOrientationV2;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.YesNoPreferNot;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

@DataJpaTest
public class ProfileServiceTest {
  private static final FakeClock CLOCK = new FakeClock(Instant.parse("2000-01-01T00:00:00.00Z"));

  private static final DbInstitution BROAD_INSTITUTION =
      new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

  private static final VerifiedInstitutionalAffiliation BROAD_AFFILIATION =
      new VerifiedInstitutionalAffiliation()
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
        .verifiedInstitutionalAffiliation(BROAD_AFFILIATION)
        .initialCreditsExpirationBypassed(false);
  }

  private static final Profile VALID_PROFILE = createValidProfile();

  @MockBean private InstitutionDao mockInstitutionDao;
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserService mockUserService;
  @MockBean private InitialCreditsService mockInitialCreditsService;
  @MockBean private UserTermsOfServiceDao mockUserTermsOfServiceDao;

  @MockBean
  private VerifiedInstitutionalAffiliationMapper mockVerifiedInstitutionalAffiliationMapper;

  @Autowired ProfileService profileService;
  // Use a SpyBean here, since we need to have a real UserDao available, but also mock out specific
  // method calls (see e.g. testGetAdminTableUsers* tests).
  @SpyBean UserDao userDao;

  // enables access to the logged in user
  private static DbUser loggedInUser;

  private static WorkbenchConfig providedWorkbenchConfig;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    AddressMapperImpl.class,
    CommonConfig.class,
    CommonMappers.class,
    DemographicSurveyMapperImpl.class,
    PageVisitMapperImpl.class,
    ProfileMapperImpl.class,
    ProfileService.class,
    VerifiedInstitutionalAffiliationMapperImpl.class
  })
  @MockBean({
    AccessModuleService.class,
    AccessTierService.class,
    InitialCreditsService.class,
    NewUserSatisfactionSurveyService.class,
    ProfileAuditor.class,
    VerifiedInstitutionalAffiliationDao.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DbUser getLoggedInUser() {
      return loggedInUser;
    }

    @Bean
    public Clock getClock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() {
    loggedInUser = new DbUser();
    loggedInUser.setUserId(1000);
    loggedInUser = userDao.save(loggedInUser);

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
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
    int latestVersion = 5; // arbitrary;
    providedWorkbenchConfig.termsOfService.minimumAcceptedAouVersion = latestVersion;

    DbUserTermsOfService userTermsOfService = new DbUserTermsOfService();
    userTermsOfService.setTosVersion(latestVersion);
    userTermsOfService.setAouAgreementTime(new Timestamp(100));
    when(mockUserTermsOfServiceDao.findFirstByUserIdOrderByTosVersionDesc(1))
        .thenReturn(Optional.of(userTermsOfService));

    DbUser user = new DbUser();
    user.setUserId(1);
    Profile profile = profileService.getProfile(user);
    assertThat(profile.getLatestTermsOfServiceVersion()).isEqualTo(latestVersion);
    assertThat(profile.getLatestTermsOfServiceTime()).isEqualTo(100);
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

    profileService.validateAffiliation(profile);

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

    profileService.validateAffiliation(profile);

    ArgumentCaptor<DbVerifiedInstitutionalAffiliation> affiliationSpy =
        ArgumentCaptor.forClass(DbVerifiedInstitutionalAffiliation.class);
    ArgumentCaptor<String> unusedSpy = ArgumentCaptor.forClass(String.class);
    verify(mockInstitutionService)
        .validateAffiliation(affiliationSpy.capture(), unusedSpy.capture());

    assertThat(affiliationSpy.getValue().getInstitution().getShortName()).isEqualTo("Broad");
  }

  @Test
  public void validateInstitutionalAffiliation_noAffiliation() {
    assertThrows(
        BadRequestException.class,
        () -> {
          profileService.validateAffiliation(new Profile());
        });
  }

  @Test
  public void validateInstitutionalAffiliation_noInstitution() {
    assertThrows(
        NotFoundException.class,
        () -> {
          Profile profile = new Profile().verifiedInstitutionalAffiliation(BROAD_AFFILIATION);
          when(mockInstitutionDao.findOneByShortName("Broad")).thenReturn(Optional.empty());
          profileService.validateAffiliation(profile);
        });
  }

  @Test
  public void validateInstitutionalAffiliation_noRole() {
    assertThrows(
        BadRequestException.class,
        () -> {
          VerifiedInstitutionalAffiliation affiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName("Broad")
                  .institutionDisplayName("The Broad Institute");
          Profile profile = new Profile().verifiedInstitutionalAffiliation(affiliation);
          when(mockInstitutionDao.findOneByShortName("Broad"))
              .thenReturn(Optional.of(BROAD_INSTITUTION));
          profileService.validateAffiliation(profile);
        });
  }

  @Test
  public void validateInstitutionalAffiliation_noOtherText() {
    assertThrows(
        BadRequestException.class,
        () -> {
          VerifiedInstitutionalAffiliation affiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName("Broad")
                  .institutionDisplayName("The Broad Institute")
                  .institutionalRoleEnum(InstitutionalRole.OTHER);
          Profile profile = new Profile().verifiedInstitutionalAffiliation(affiliation);
          when(mockInstitutionDao.findOneByShortName("Broad"))
              .thenReturn(Optional.of(BROAD_INSTITUTION));
          profileService.validateAffiliation(profile);
        });
  }

  @Test
  public void validateInstitutionalAffilation_badEmail() {
    assertThrows(
        BadRequestException.class,
        () -> {
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
          when(mockInstitutionDao.findOneByShortName("Broad"))
              .thenReturn(Optional.of(BROAD_INSTITUTION));
          when(mockInstitutionService.validateAffiliation(
                  any(DbVerifiedInstitutionalAffiliation.class), anyString()))
              .thenReturn(false);
          DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
              new DbVerifiedInstitutionalAffiliation();
          dbVerifiedInstitutionalAffiliation.setInstitution(BROAD_INSTITUTION);
          when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
                  affiliation, mockInstitutionService))
              .thenReturn(dbVerifiedInstitutionalAffiliation);
          profileService.validateAffiliation(profile);
        });
  }

  @Test
  public void updateProfile_affiliationChangeOnly_asAdmin() {
    // Regression test for RW-5139

    // grant admin authority to loggedInUser
    when(mockUserService.hasAuthority(loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
        .thenReturn(true);

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
    user.setUserId(10);
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

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(user);

    profileService.updateProfile(
        user, Agent.asAdmin(loggedInUser), updatedProfile, previousProfile);
  }

  @Test
  public void updateProfile_cant_change_contactEmail() {

    Profile previousProfile = createValidProfile().contactEmail("researcher@nih.gov");
    Profile updatedProfile = createValidProfile().contactEmail("other-researcher@nih.gov");
    assertThrows(
        BadRequestException.class,
        () ->
            profileService.updateProfile(
                loggedInUser, Agent.asUser(loggedInUser), updatedProfile, previousProfile));
  }

  @Test
  public void updateProfile_can_change_contactEmail_asAdmin() {

    // grant admin authority to loggedInUser
    when(mockUserService.hasAuthority(loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
        .thenReturn(true);

    DbInstitution verilyInstitution =
        new DbInstitution().setShortName("Verily").setDisplayName("Verily LLC");

    VerifiedInstitutionalAffiliation verilyAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(verilyInstitution.getShortName())
            .institutionDisplayName(verilyInstitution.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.ADMIN);

    when(mockInstitutionDao.findOneByShortName("Verily"))
        .thenReturn(Optional.of(verilyInstitution));
    when(mockInstitutionService.validateAffiliation(any(), any())).thenReturn(true);
    DbVerifiedInstitutionalAffiliation dbVerilyAffiliation =
        new DbVerifiedInstitutionalAffiliation().setInstitution(verilyInstitution);
    when(mockVerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            verilyAffiliation, mockInstitutionService))
        .thenReturn(dbVerilyAffiliation);

    Profile previousProfile =
        createValidProfile()
            .contactEmail("researcher@verily.com") // note: the mock in this test matches everything
            .verifiedInstitutionalAffiliation(verilyAffiliation);
    Profile updatedProfile =
        createValidProfile()
            .contactEmail(
                "other-researcher@verily.com") // note: the mock in this test matches everything
            .verifiedInstitutionalAffiliation(verilyAffiliation);

    DbUser targetUser = new DbUser();
    targetUser.setUserId(10);
    targetUser.setGivenName("John");
    targetUser.setFamilyName("Doe");

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asAdmin(loggedInUser), updatedProfile, previousProfile);

    assertThat(profileService.getProfile(targetUser).getContactEmail())
        .isEqualTo("other-researcher@verily.com");
  }

  @Test
  public void updateProfile_cannot_disable_asUser() {
    Profile previousProfile = createValidProfile().disabled(false);
    Profile updatedProfile = createValidProfile().disabled(true);

    DbUser targetUser = new DbUser();
    targetUser.setUserId(10);
    targetUser.setGivenName("John");
    targetUser.setFamilyName("Doe");

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    // pretend to be an Admin for the purpose of this call,
    // to demonstrate that this is NOT how Admin access is obtained
    final Agent sneakyUser = Agent.asAdmin(loggedInUser);
    assertThrows(
        BadRequestException.class,
        () ->
            profileService.updateProfile(targetUser, sneakyUser, updatedProfile, previousProfile));
  }

  @Test
  public void updateProfile_cannot_enable_asUser() {
    Profile previousProfile = createValidProfile().disabled(true);
    Profile updatedProfile = createValidProfile().disabled(false);

    DbUser targetUser = new DbUser();
    targetUser.setUserId(10);
    targetUser.setGivenName("John");
    targetUser.setFamilyName("Doe");

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    // pretend to be an Admin for the purpose of this call,
    // to demonstrate that this is NOT how Admin access is obtained
    final Agent sneakyUser = Agent.asAdmin(loggedInUser);
    assertThrows(
        BadRequestException.class,
        () ->
            profileService.updateProfile(targetUser, sneakyUser, updatedProfile, previousProfile));
  }

  @Test
  public void updateProfile_disable_user_asAdmin() {
    // grant admin authority to loggedInUser
    when(mockUserService.hasAuthority(loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
        .thenReturn(true);

    Profile previousProfile = createValidProfile().disabled(false);
    Profile updatedProfile = createValidProfile().disabled(true);

    DbUser targetUser = new DbUser();
    targetUser.setUserId(10);
    targetUser.setGivenName("John");
    targetUser.setFamilyName("Doe");

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asAdmin(loggedInUser), updatedProfile, previousProfile);

    assertThat(profileService.getProfile(targetUser).isDisabled()).isTrue();
  }

  @Test
  public void updateProfile_enable_user_asAdmin() {
    // grant admin authority to loggedInUser
    when(mockUserService.hasAuthority(loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
        .thenReturn(true);

    Profile previousProfile = createValidProfile().disabled(true);
    Profile updatedProfile = createValidProfile().disabled(false);

    DbUser targetUser = new DbUser();
    targetUser.setUserId(10);
    targetUser.setGivenName("John");
    targetUser.setFamilyName("Doe");

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asAdmin(loggedInUser), updatedProfile, previousProfile);

    assertThat(profileService.getProfile(targetUser).isDisabled()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void updateProfile_bypassInitialCredits_user_asAdmin(
      boolean enableInitialCreditsExpiration) {
    providedWorkbenchConfig.featureFlags.enableInitialCreditsExpiration =
        enableInitialCreditsExpiration;
    // grant admin authority to loggedInUser
    when(mockUserService.hasAuthority(loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
        .thenReturn(true);

    Profile previousProfile = createValidProfile().initialCreditsExpirationBypassed(false);
    Profile updatedProfile = createValidProfile().initialCreditsExpirationBypassed(true);

    DbUser targetUser = new DbUser();
    targetUser.setUserId(10);
    targetUser.setGivenName("John");
    targetUser.setFamilyName("Doe");

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asAdmin(loggedInUser), updatedProfile, previousProfile);

    verify(mockInitialCreditsService, times(enableInitialCreditsExpiration ? 1 : 0))
        .setInitialCreditsExpirationBypassed(targetUser, true);
  }

  @Test
  public void updateProfile_demo_survey_add_v2() {
    DemographicSurveyV2 v2Survey =
        new DemographicSurveyV2()
            .ethnicCategories(
                ImmutableList.of(
                    EthnicCategory.ASIAN, EthnicCategory.ASIAN_CHINESE, EthnicCategory.WHITE))
            .genderIdentities(ImmutableList.of(GenderIdentityV2.MAN, GenderIdentityV2.TRANS_MAN))
            .sexualOrientations(ImmutableList.of(SexualOrientationV2.QUEER))
            .sexAtBirth(SexAtBirthV2.PREFER_NOT_TO_ANSWER)
            .yearOfBirthPreferNot(true)
            .disabilityHearing(YesNoPreferNot.NO)
            .disabilitySeeing(YesNoPreferNot.YES)
            .education(EducationV2.DOCTORATE)
            .disadvantaged(YesNoPreferNot.PREFER_NOT_TO_ANSWER);

    Profile previousProfile = createValidProfile();
    Profile updatedProfile = createValidProfile().demographicSurveyV2(v2Survey);

    DbUser targetUser =
        userDao.save(new DbUser().setUserId(10).setGivenName("John").setFamilyName("Doe"));

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asUser(loggedInUser), updatedProfile, previousProfile);

    TestMockFactory.assertEqualDemographicSurveys(
        profileService.getProfile(targetUser).getDemographicSurveyV2(),
        updatedProfile.getDemographicSurveyV2());
  }

  @Test
  public void updateProfile_with_existing_demo_survey_v2() {
    DemographicSurveyV2 v2Survey =
        new DemographicSurveyV2()
            .ethnicCategories(
                ImmutableList.of(
                    EthnicCategory.ASIAN, EthnicCategory.ASIAN_CHINESE, EthnicCategory.WHITE))
            .genderIdentities(ImmutableList.of(GenderIdentityV2.MAN, GenderIdentityV2.TRANS_MAN))
            .sexualOrientations(ImmutableList.of(SexualOrientationV2.QUEER))
            .sexAtBirth(SexAtBirthV2.PREFER_NOT_TO_ANSWER)
            .yearOfBirthPreferNot(true)
            .disabilityHearing(YesNoPreferNot.NO)
            .disabilitySeeing(YesNoPreferNot.YES)
            .education(EducationV2.DOCTORATE)
            .disadvantaged(YesNoPreferNot.PREFER_NOT_TO_ANSWER);

    Profile previousProfile = createValidProfile().demographicSurveyV2(v2Survey);
    Profile updatedProfile =
        createValidProfile().demographicSurveyV2(v2Survey).areaOfResearch("Some research");

    DbUser targetUser =
        userDao.save(new DbUser().setUserId(10).setGivenName("John").setFamilyName("Doe"));

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asUser(loggedInUser), updatedProfile, previousProfile);

    TestMockFactory.assertEqualDemographicSurveys(
        profileService.getProfile(targetUser).getDemographicSurveyV2(),
        updatedProfile.getDemographicSurveyV2());

    assertThat(profileService.getProfile(targetUser).getAreaOfResearch())
        .isEqualTo("Some research");
  }

  @Test
  public void updateProfile_demo_survey_update_v2() {
    DemographicSurveyV2 v2Survey =
        new DemographicSurveyV2()
            .ethnicCategories(
                ImmutableList.of(
                    EthnicCategory.ASIAN, EthnicCategory.ASIAN_CHINESE, EthnicCategory.WHITE))
            .genderIdentities(ImmutableList.of(GenderIdentityV2.MAN, GenderIdentityV2.TRANS_MAN))
            .sexualOrientations(ImmutableList.of(SexualOrientationV2.QUEER))
            .sexAtBirth(SexAtBirthV2.PREFER_NOT_TO_ANSWER)
            .yearOfBirthPreferNot(true)
            .disabilityHearing(YesNoPreferNot.NO)
            .disabilitySeeing(YesNoPreferNot.YES)
            .education(EducationV2.DOCTORATE)
            .disadvantaged(YesNoPreferNot.PREFER_NOT_TO_ANSWER);

    DemographicSurveyV2 updatedV2Survey =
        new DemographicSurveyV2()
            .ethnicCategories(ImmutableList.of(EthnicCategory.BLACK))
            .genderIdentities(ImmutableList.of(GenderIdentityV2.PREFER_NOT_TO_ANSWER))
            .sexualOrientations(ImmutableList.of(SexualOrientationV2.PREFER_NOT_TO_ANSWER))
            .sexAtBirth(SexAtBirthV2.FEMALE)
            .yearOfBirth(1995)
            .disabilityHearing(YesNoPreferNot.NO)
            .disabilitySeeing(YesNoPreferNot.NO)
            .education(EducationV2.PREFER_NOT_TO_ANSWER)
            .disadvantaged(YesNoPreferNot.NO);

    Profile previousProfile = createValidProfile().demographicSurveyV2(v2Survey);
    Profile updatedProfile = createValidProfile().demographicSurveyV2(updatedV2Survey);

    DbUser targetUser =
        userDao.save(new DbUser().setUserId(10).setGivenName("John").setFamilyName("Doe"));

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asUser(loggedInUser), updatedProfile, previousProfile);

    TestMockFactory.assertEqualDemographicSurveys(
        profileService.getProfile(targetUser).getDemographicSurveyV2(),
        updatedProfile.getDemographicSurveyV2());
  }

  @Test
  public void updateProfile_demo_survey_add_v2_all_selections() {
    DemographicSurveyV2 v2Survey = TestMockFactory.createDemoSurveyV2AllCategories();

    Profile previousProfile = createValidProfile();
    Profile updatedProfile = createValidProfile().demographicSurveyV2(v2Survey);

    DbUser targetUser =
        userDao.save(new DbUser().setUserId(10).setGivenName("John").setFamilyName("Doe"));

    when(mockUserService.updateUserWithRetries(any(), any(), any())).thenReturn(targetUser);

    profileService.updateProfile(
        targetUser, Agent.asUser(loggedInUser), updatedProfile, previousProfile);

    TestMockFactory.assertEqualDemographicSurveys(
        profileService.getProfile(targetUser).getDemographicSurveyV2(),
        updatedProfile.getDemographicSurveyV2());
  }

  @Test
  public void validateProfile_noChangesOnEmptyProfile() {
    // This is a synthetic test case: we never expect to be updating an empty Profile object, but
    // technically this passes validation since no fields have changed.
    profileService.validateProfile(new Profile(), new Profile());
  }

  @Test
  public void validateProfile_emptyNewObject() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // The 'prevProfile' argument is null, so this is considered a new object and all
          // validation
          // is executed. The empty Profile object is obviously rejected.
          profileService.validateNewProfile(new Profile());
        });
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

  @Test
  public void validateProfile_usernameChanged_user() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // Username changes are disallowed for users
          profileService.validateProfile(
              new Profile().username("new"), new Profile().username("old"));
        });
  }

  @Test
  public void validateProfile_usernameChanged_admin() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // Username changes are still disallowed for admins
          when(mockUserService.hasAuthority(
                  loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
              .thenReturn(true);
          profileService.validateProfile(
              new Profile().username("new"), new Profile().username("old"));
        });
  }

  @Test
  public void validateProfile_usernameTooShort() {
    assertThrows(
        BadRequestException.class,
        () -> {
          profileService.validateProfile(new Profile().username("ab"), new Profile());
        });
  }

  @Test
  public void validateProfile_usernameTooLong() {
    assertThrows(
        BadRequestException.class,
        () -> {
          profileService.validateProfile(
              new Profile().username(StringUtils.repeat("asdf", 30)), new Profile());
        });
  }

  @Test
  public void validateProfile_contactEmailChanged_user() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // Contact email changes are disallowed for users.
          profileService.validateProfile(
              new Profile().contactEmail("new@gmail.com"),
              new Profile().contactEmail("old@gmail.com"));
        });
  }

  @Test
  public void validateProfile_contactEmailChanged_admin() {
    // Contact email changes are allowed for admins
    when(mockUserService.hasAuthority(loggedInUser.getUserId(), Authority.ACCESS_CONTROL_ADMIN))
        .thenReturn(true);

    profileService.validateProfile(
        new Profile().contactEmail("new@gmail.com"), new Profile().contactEmail("old@gmail.com"));
  }

  @Test
  public void validateProfile_givenNameTooShort() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Profile newProfile = new Profile().givenName("");
          profileService.validateProfile(newProfile, new Profile());
        });
  }

  @Test
  public void validateProfile_FamilyNameTooLong() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Profile newProfile = new Profile().familyName(StringUtils.repeat("123", 30));
          profileService.validateProfile(newProfile, new Profile());
        });
  }

  @Test
  public void validateProfile_addressRemoved() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // Ensures we validate the address when the field has been removed entirely.
          Profile oldProfile = new Profile().address(new Address().streetAddress1("asdf"));
          Profile newProfile = new Profile();
          profileService.validateProfile(newProfile, oldProfile);
        });
  }

  @Test
  public void validateProfile_addressRemoveZipCode() {
    assertThrows(
        BadRequestException.class,
        () -> {
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
        });
  }

  @Test
  public void validateProfile_addressIncomplete() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Profile newProfile = new Profile().address(new Address().streetAddress1("asdf"));
          profileService.validateProfile(newProfile, new Profile());
        });
  }

  @Test
  public void validateProfile_EmptyAreaOfResearch() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Profile newProfile = new Profile().areaOfResearch("");
          profileService.validateProfile(newProfile, new Profile());
        });
  }

  @Test
  public void validateProfile_noInstitutionMatch() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Profile newProfile =
              new Profile()
                  .contactEmail("not-a-match@gmail.com")
                  .verifiedInstitutionalAffiliation(
                      VALID_PROFILE.getVerifiedInstitutionalAffiliation());
          when(mockInstitutionDao.findOneByShortName("Broad"))
              .thenReturn(Optional.of(BROAD_INSTITUTION));
          when(mockInstitutionService.validateAffiliation(any(), any())).thenReturn(false);
          profileService.validateProfile(newProfile, new Profile());
        });
  }

  @Test
  public void testGetAdminTableUsers_noUsers() {
    doReturn(ImmutableList.of()).when(userDao).getAdminTableUsers();

    final List<AdminTableUser> profiles = profileService.getAdminTableUsers();
    assertThat(profiles).isEmpty();
  }

  @Test
  public void testGetAdminTableUsers_someUsers() {
    ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

    final UserDao.DbAdminTableUser user1 = factory.createProjection(UserDao.DbAdminTableUser.class);
    user1.setUserId(101L);
    user1.setContactEmail("jay@aou.biz");
    user1.setDisabled(false);
    user1.setInstitutionName("University 1");

    final UserDao.DbAdminTableUser user2 = factory.createProjection(UserDao.DbAdminTableUser.class);
    user2.setUserId(102L);
    user2.setContactEmail("fred@aou.biz");
    user2.setDisabled(true);
    user2.setInstitutionName("University 2");

    final UserDao.DbAdminTableUser user3 = factory.createProjection(UserDao.DbAdminTableUser.class);
    user3.setUserId(103L);
    user3.setContactEmail("betty@aou.biz");
    user3.setDisabled(true);
    user3.setInstitutionName("University 3");

    doReturn(ImmutableList.of(user1, user2, user3)).when(userDao).getAdminTableUsers();

    final List<AdminTableUser> adminTableUsers = profileService.getAdminTableUsers();
    assertThat(adminTableUsers).hasSize(3);

    assertThat(adminTableUsers.get(0).getInstitutionName()).isEqualTo("University 1");
    assertThat(adminTableUsers.get(0).getContactEmail()).isEqualTo("jay@aou.biz");
    assertThat(adminTableUsers.get(0).isDisabled()).isEqualTo(false);
    assertThat(adminTableUsers.get(1).getUserId()).isEqualTo(user2.getUserId());
    assertThat(adminTableUsers.get(2).getUserId()).isEqualTo(user3.getUserId());
  }
}
