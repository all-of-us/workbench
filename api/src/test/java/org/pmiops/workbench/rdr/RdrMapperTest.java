package org.pmiops.workbench.rdr;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.DemographicSurveyV2;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.profile.DemographicSurveyMapperImpl;
import org.pmiops.workbench.rdr.model.RdrAccessTier;
import org.pmiops.workbench.rdr.model.RdrDegree;
import org.pmiops.workbench.rdr.model.RdrDemographicSurveyV2;
import org.pmiops.workbench.rdr.model.RdrEducation;
import org.pmiops.workbench.rdr.model.RdrEthnicity;
import org.pmiops.workbench.rdr.model.RdrGender;
import org.pmiops.workbench.rdr.model.RdrRace;
import org.pmiops.workbench.rdr.model.RdrResearcher;
import org.pmiops.workbench.rdr.model.RdrResearcherVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.rdr.model.RdrSexAtBirth;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspace.StatusEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.AccessToCareEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.AgeEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.DisabilityStatusEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.EducationLevelEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.GenderIdentityEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.GeographyEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.IncomeLevelEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.RaceEthnicityEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.SexAtBirthEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.SexualOrientationEnum;
import org.pmiops.workbench.rdr.model.RdrYesNoPreferNot;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class RdrMapperTest {

  private static final Timestamp TIME1 = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final Timestamp TIME2 = Timestamp.from(Instant.parse("2001-06-01T00:00:00.00Z"));

  @TestConfiguration
  @Import({
    DemographicSurveyMapperImpl.class,
    RdrMapperImpl.class,
  })
  static class Configuration {}

  private @Autowired RdrMapper rdrMapper;

  private static Stream<Arguments> accessTierCases() {
    return Stream.of(
        Arguments.of("controlled", RdrAccessTier.CONTROLLED),
        Arguments.of("registered", RdrAccessTier.REGISTERED),
        Arguments.of(null, RdrAccessTier.UNSET),
        Arguments.of("asdf", RdrAccessTier.UNSET));
  }

  @ParameterizedTest
  @MethodSource("accessTierCases")
  public void testMapRdrWorkspace_accessTiers(
      @Nullable String tierShortName, RdrAccessTier wantRdrTier) {
    DbAccessTier accessTier = new DbAccessTier();
    accessTier.setShortName(tierShortName);

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setAccessTier(accessTier);

    DbWorkspace from = new DbWorkspace();
    from.setCdrVersion(cdrVersion);

    assertThat(rdrMapper.toRdrWorkspace(from).getAccessTier()).isEqualTo(wantRdrTier);
  }

  @Test
  public void testMapRdrWorkspace() {
    assertThat(
            rdrMapper.toRdrWorkspace(
                TestMockFactory.createDbWorkspaceStub(
                    TestMockFactory.createWorkspace("ns", "name"), 1L)))
        .isEqualTo(
            new RdrWorkspace()
                .workspaceId(1)
                .name("name")
                .status(StatusEnum.ACTIVE)
                .diseaseFocusedResearch(true)
                .diseaseFocusedResearchName("cancer")
                .methodsDevelopment(true)
                .controlSet(true)
                .ancestry(true)
                .socialBehavioral(true)
                .populationHealth(true)
                .drugDevelopment(true)
                .commercialPurpose(true)
                .educational(true)
                .ethicalLegalSocialImplications(false)
                .otherPurpose(false)
                .intendToStudy("intended study")
                .findingsFromStudy("anticipated findings")
                .focusOnUnderrepresentedPopulations(false)
                .workspaceDemographic(
                    new RdrWorkspaceDemographic()
                        .raceEthnicity(ImmutableList.of(RaceEthnicityEnum.UNSET))
                        .age(ImmutableList.of(AgeEnum.UNSET))
                        .sexAtBirth(SexAtBirthEnum.UNSET)
                        .genderIdentity(GenderIdentityEnum.UNSET)
                        .sexualOrientation(SexualOrientationEnum.UNSET)
                        .geography(GeographyEnum.UNSET)
                        .disabilityStatus(DisabilityStatusEnum.UNSET)
                        .accessToCare(AccessToCareEnum.UNSET)
                        .educationLevel(EducationLevelEnum.UNSET)
                        .incomeLevel(IncomeLevelEnum.UNSET))
                .accessTier(RdrAccessTier.UNSET));
  }

  @Test
  public void testMapRdrWorkspace_otherPopulations() {
    RdrWorkspace got =
        rdrMapper.toRdrWorkspace(
            TestMockFactory.createDbWorkspaceStub(TestMockFactory.createWorkspace("ns", "name"), 1L)
                .setSpecificPopulationsEnum(ImmutableSet.of(SpecificPopulationEnum.OTHER))
                .setOtherPopulationDetails("reptilians"));
    assertThat(got.getWorkspaceDemographic().getOthers()).isEqualTo("reptilians");
  }

  @Test
  public void testMapRdrResearcher() {
    assertThat(
            rdrMapper.toRdrResearcher(
                new DbUser()
                    .setUserId(1)
                    .setCreationTime(TIME1)
                    .setLastModifiedTime(TIME2)
                    .setGivenName("falco")
                    .setFamilyName("lombardi")
                    .setAddress(
                        new DbAddress()
                            .setStreetAddress1("123 fake st")
                            .setStreetAddress2("apt 2")
                            .setCity("Mountain View")
                            .setState("CA")
                            .setZipCode("12345")
                            .setCountry("US"))
                    .setDemographicSurvey(
                        new DbDemographicSurvey()
                            .setEthnicityEnum(Ethnicity.PREFER_NO_ANSWER)
                            .setGenderIdentityEnumList(
                                ImmutableList.of(GenderIdentity.PREFER_NO_ANSWER))
                            .setRaceEnum(ImmutableList.of(Race.PREFER_NO_ANSWER))
                            .setSexAtBirthEnum(ImmutableList.of(SexAtBirth.PREFER_NO_ANSWER))
                            .setIdentifiesAsLgbtq(false)
                            .setLgbtqIdentity("n/a")
                            .setEducationEnum(Education.NO_EDUCATION)
                            .setDisabilityEnum(Disability.TRUE))
                    .setDegreesEnum(ImmutableList.of(Degree.MD))
                    .setContactEmail("contact@asdf.com"),
                ImmutableList.of(
                    new DbAccessTier().setShortName("registered"),
                    new DbAccessTier().setShortName("controlled")),
                new DbVerifiedInstitutionalAffiliation()
                    .setInstitution(
                        new DbInstitution().setShortName("Foo").setDisplayName("Foo Bar"))
                    .setInstitutionalRoleEnum(InstitutionalRole.FELLOW)))
        .isEqualTo(
            new RdrResearcher()
                .userId(1)
                .creationTime(rdrMapper.toModelOffsetTime(TIME1))
                .modifiedTime(rdrMapper.toModelOffsetTime(TIME2))
                .givenName("falco")
                .familyName("lombardi")
                .streetAddress1("123 fake st")
                .streetAddress2("apt 2")
                .city("Mountain View")
                .state("CA")
                .zipCode("12345")
                .country("US")
                .ethnicity(RdrEthnicity.PREFER_NOT_TO_ANSWER)
                .gender(ImmutableList.of(RdrGender.PREFER_NOT_TO_ANSWER))
                .race(ImmutableList.of(RdrRace.PREFER_NOT_TO_ANSWER))
                .sexAtBirth(ImmutableList.of(RdrSexAtBirth.PREFER_NOT_TO_ANSWER))
                .identifiesAsLgbtq(false)
                .lgbtqIdentity("n/a")
                .education(RdrEducation.NO_EDUCATION)
                .disability(RdrYesNoPreferNot.YES)
                .degrees(ImmutableList.of(RdrDegree.MD))
                .email("contact@asdf.com")
                .accessTierShortNames(
                    ImmutableList.of(RdrAccessTier.REGISTERED, RdrAccessTier.CONTROLLED))
                .verifiedInstitutionalAffiliation(
                    new RdrResearcherVerifiedInstitutionalAffiliation()
                        .institutionShortName("Foo")
                        .institutionDisplayName("Foo Bar")
                        .institutionalRole("FELLOW")));
  }

  @Test
  public void testMapRdrResearcher_otherRole() {
    RdrResearcher got =
        rdrMapper.toRdrResearcher(
            new DbUser(),
            ImmutableList.of(),
            new DbVerifiedInstitutionalAffiliation()
                .setInstitution(new DbInstitution().setShortName("Foo").setDisplayName("Foo Bar"))
                .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
                .setInstitutionalRoleOtherText("CEO"));
    assertThat(got.getVerifiedInstitutionalAffiliation().getInstitutionalRole()).isEqualTo("CEO");
  }

  @Test
  public void testMapRdrResearcher_noAccess() {
    RdrResearcher got = rdrMapper.toRdrResearcher(new DbUser(), ImmutableList.of(), null);
    assertThat(got.getAccessTierShortNames()).isEmpty();
  }

  @Test
  public void test_demoSurveyV2() {
    DemographicSurveyV2 original = TestMockFactory.createDemoSurveyV2AllCategories();
    RdrDemographicSurveyV2 rdrSurvey = rdrMapper.toDemographicSurveyV2(original);
    DemographicSurveyV2 roundTrip = rdrMapper.toModelDemographicSurveyV2(rdrSurvey);
    TestMockFactory.assertEqualDemographicSurveys(roundTrip, original);
  }
}
