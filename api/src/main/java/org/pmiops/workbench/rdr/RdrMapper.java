package org.pmiops.workbench.rdr;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.EnumMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.db.model.DbAccessTier;
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
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.profile.DemographicSurveyMapper;
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
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.AgeEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.RaceEthnicityEnum;
import org.pmiops.workbench.rdr.model.RdrYesNoPreferNot;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class, uses = DemographicSurveyMapper.class)
public interface RdrMapper {
  ZoneOffset offset = OffsetDateTime.now().getOffset();

  @Mapping(source = "anticipatedFindings", target = "findingsFromStudy")
  @Mapping(source = "ethics", target = "ethicalLegalSocialImplications")
  // excludeFromPublicDirectory requires checking workspace creator institution information
  // This will be handle in ServiceImpl Class
  @Mapping(target = "excludeFromPublicDirectory", ignore = true)
  @Mapping(source = "intendedStudy", target = "intendToStudy")
  @Mapping(source = "diseaseOfFocus", target = "diseaseFocusedResearchName")
  @Mapping(source = "lastModifiedTime", target = "modifiedTime")
  @Mapping(source = "scientificApproach", target = "scientificApproaches")
  @Mapping(source = "specificPopulationsEnum", target = "focusOnUnderrepresentedPopulations")
  @Mapping(source = "specificPopulationsEnum", target = "workspaceDemographic")
  @Mapping(source = "workspaceActiveStatusEnum", target = "status")
  @Mapping(source = "cdrVersion.name", target = "cdrVersionName")
  @Mapping(source = "cdrVersion.accessTier.shortName", target = "accessTier")
  // Workspace User will be populated by a call to FireCloud
  // This will be handle in ServiceImpl Class
  @Mapping(target = "workspaceUsers", ignore = true)
  RdrWorkspace toRdrWorkspace(DbWorkspace dbWorkspace);

  @AfterMapping
  default void addOtherPopulationDetails(DbWorkspace source, @MappingTarget RdrWorkspace target) {
    if (source.getSpecificPopulationsEnum().contains(SpecificPopulationEnum.OTHER)) {
      target.getWorkspaceDemographic().others(source.getOtherPopulationDetails());
    }
  }

  @Mapping(source = "dbUser.lastModifiedTime", target = "modifiedTime")
  @Mapping(source = "dbUser.contactEmail", target = "email")
  @Mapping(source = "dbUser.degreesEnum", target = "degrees")
  @Mapping(source = "dbUser.address.streetAddress1", target = "streetAddress1")
  @Mapping(source = "dbUser.address.streetAddress2", target = "streetAddress2")
  @Mapping(source = "dbUser.address.city", target = "city")
  @Mapping(source = "dbUser.address.state", target = "state")
  @Mapping(source = "dbUser.address.country", target = "country")
  @Mapping(source = "dbUser.address.zipCode", target = "zipCode")
  @Mapping(source = "dbUser.demographicSurvey.disabilityEnum", target = "disability")
  @Mapping(source = "dbUser.demographicSurvey.educationEnum", target = "education")
  @Mapping(source = "dbUser.demographicSurvey.ethnicityEnum", target = "ethnicity")
  @Mapping(source = "dbUser.demographicSurvey.sexAtBirthEnum", target = "sexAtBirth")
  @Mapping(source = "dbUser.demographicSurvey.genderIdentityEnumList", target = "gender")
  @Mapping(source = "dbUser.demographicSurvey.raceEnum", target = "race")
  @Mapping(source = "dbUser.demographicSurvey.lgbtqIdentity", target = "lgbtqIdentity")
  @Mapping(source = "dbUser.demographicSurvey.identifiesAsLgbtq", target = "identifiesAsLgbtq")
  @Mapping(source = "accessTiers", target = "accessTierShortNames")
  @Mapping(source = "dbUser.demographicSurveyV2", target = "demographicSurveyV2")
  RdrResearcher toRdrResearcher(
      DbUser dbUser,
      List<DbAccessTier> accessTiers,
      @Nullable DbVerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation);

  @Mapping(source = "institution.shortName", target = "institutionShortName")
  @Mapping(source = "institution.displayName", target = "institutionDisplayName")
  @Mapping(ignore = true, target = "institutionalRole")
  RdrResearcherVerifiedInstitutionalAffiliation toRdrAffiliation(
      DbVerifiedInstitutionalAffiliation v);

  default List<RdrAccessTier> toRdrAccessTiers(List<DbAccessTier> dbAccessTiers) {
    return dbAccessTiers.stream()
        .map(DbAccessTier::getShortName)
        .map(this::toModelAccessTier)
        // UNSET will be returned for any unknown tier. We don't want to send that as an element to
        // RDR, e.g. because we added a new tier and forgot to update the RDR enum.
        .filter(tier -> !RdrAccessTier.UNSET.equals(tier))
        .collect(Collectors.toList());
  }

  @AfterMapping
  default void addInstitutionalRole(
      DbVerifiedInstitutionalAffiliation source,
      @MappingTarget RdrResearcherVerifiedInstitutionalAffiliation target) {
    if (source == null || source.getInstitutionalRoleEnum() == null) {
      return;
    }
    final InstitutionalRole roleEnum = source.getInstitutionalRoleEnum();
    final String role =
        InstitutionalRole.OTHER.equals(roleEnum)
            ? source.getInstitutionalRoleOtherText()
            : roleEnum.toString();
    target.setInstitutionalRole(role);
  }

  default OffsetDateTime toModelOffsetTime(@Nullable Timestamp dbTime) {
    if (dbTime == null) {
      return null;
    }
    return dbTime.toLocalDateTime().atOffset(offset);
  }

  default RdrAccessTier toModelAccessTier(@Nullable String accessTier) {
    return Optional.ofNullable(accessTier)
        .map(t -> RdrAccessTier.fromValue(t.toUpperCase()))
        .orElse(RdrAccessTier.UNSET);
  }

  default RdrWorkspace.StatusEnum toModelStatus(WorkspaceActiveStatus workspaceActiveStatus) {
    return workspaceActiveStatus == WorkspaceActiveStatus.ACTIVE
        ? RdrWorkspace.StatusEnum.ACTIVE
        : RdrWorkspace.StatusEnum.INACTIVE;
  }

  @ValueMapping(source = "NONE", target = "UNSET")
  RdrDegree toRdrDegree(Degree d);

  @ValueMapping(source = "TRUE", target = "YES")
  @ValueMapping(source = "FALSE", target = "NO")
  @ValueMapping(source = "PREFER_NO_ANSWER", target = "PREFER_NOT_TO_ANSWER")
  RdrYesNoPreferNot toRdrDisability(Disability d);

  @ValueMapping(source = "PREFER_NO_ANSWER", target = "PREFER_NOT_TO_ANSWER")
  RdrEducation toRdrEducation(Education e);

  @ValueMapping(source = "PREFER_NO_ANSWER", target = "PREFER_NOT_TO_ANSWER")
  RdrEthnicity toRdrEthnicity(Ethnicity e);

  @ValueMapping(source = "PREFER_NO_ANSWER", target = "PREFER_NOT_TO_ANSWER")
  RdrSexAtBirth toRdrSexAtBirth(SexAtBirth s);

  @ValueMapping(source = "PREFER_NO_ANSWER", target = "PREFER_NOT_TO_ANSWER")
  RdrGender toRdrGender(GenderIdentity g);

  @ValueMapping(source = "PREFER_NO_ANSWER", target = "PREFER_NOT_TO_ANSWER")
  RdrRace toRdrRace(Race r);

  @ValueMapping(source = "RACE_MORE_THAN_ONE", target = "MULTI")
  // RACE_ASIAN -> ASIAN, etc
  @EnumMapping(
      nameTransformationStrategy = MappingConstants.STRIP_PREFIX_TRANSFORMATION,
      configuration = "RACE_")
  @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
  RaceEthnicityEnum toRdrRaceEthnicity(SpecificPopulationEnum specificPopulationEnum);

  @ValueMapping(source = "AGE_CHILDREN", target = "AGE_0_11")
  @ValueMapping(source = "AGE_ADOLESCENTS", target = "AGE_12_17")
  @ValueMapping(source = "AGE_OLDER", target = "AGE_65_74")
  @ValueMapping(source = "AGE_OLDER_MORE_THAN_75", target = "AGE_75_AND_MORE")
  @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
  AgeEnum toRdrAge(SpecificPopulationEnum specificPopulationEnum);

  default boolean toModelFocusOnUnderrepresentedPopulation(
      Set<SpecificPopulationEnum> dbSpecificPopulationSet) {
    return dbSpecificPopulationSet != null && dbSpecificPopulationSet.size() > 0;
  }

  default RdrWorkspaceDemographic toModelWorkspaceDemographic(
      Set<SpecificPopulationEnum> dbPopulationEnumSet) {
    RdrWorkspaceDemographic rdrDemographic = new RdrWorkspaceDemographic();

    rdrDemographic.setAccessToCare(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.ACCESS_TO_CARE)
            ? RdrWorkspaceDemographic.AccessToCareEnum.NOT_EASILY_ACCESS_CARE
            : RdrWorkspaceDemographic.AccessToCareEnum.UNSET);

    rdrDemographic.setDisabilityStatus(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.DISABILITY_STATUS)
            ? RdrWorkspaceDemographic.DisabilityStatusEnum.DISABILITY
            : RdrWorkspaceDemographic.DisabilityStatusEnum.UNSET);

    rdrDemographic.setEducationLevel(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.EDUCATION_LEVEL)
            ? RdrWorkspaceDemographic.EducationLevelEnum.LESS_THAN_HIGH_SCHOOL
            : RdrWorkspaceDemographic.EducationLevelEnum.UNSET);

    rdrDemographic.setIncomeLevel(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.INCOME_LEVEL)
            ? RdrWorkspaceDemographic.IncomeLevelEnum.BELOW_FEDERAL_POVERTY_LEVEL_200_PERCENT
            : RdrWorkspaceDemographic.IncomeLevelEnum.UNSET);

    rdrDemographic.setGeography(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.GEOGRAPHY)
            ? RdrWorkspaceDemographic.GeographyEnum.RURAL
            : RdrWorkspaceDemographic.GeographyEnum.UNSET);

    rdrDemographic.setSexualOrientation(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.SEXUAL_ORIENTATION)
            ? RdrWorkspaceDemographic.SexualOrientationEnum.OTHER_THAN_STRAIGHT
            : RdrWorkspaceDemographic.SexualOrientationEnum.UNSET);

    rdrDemographic.setGenderIdentity(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.GENDER_IDENTITY)
            ? RdrWorkspaceDemographic.GenderIdentityEnum.OTHER_THAN_MAN_WOMAN
            : RdrWorkspaceDemographic.GenderIdentityEnum.UNSET);

    rdrDemographic.setSexAtBirth(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.SEX)
            ? RdrWorkspaceDemographic.SexAtBirthEnum.INTERSEX
            : RdrWorkspaceDemographic.SexAtBirthEnum.UNSET);

    rdrDemographic.setRaceEthnicity(
        dbPopulationEnumSet.stream()
            .map(this::toRdrRaceEthnicity)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

    if (rdrDemographic.getRaceEthnicity().isEmpty()) {
      rdrDemographic.setRaceEthnicity(
          Collections.singletonList(RdrWorkspaceDemographic.RaceEthnicityEnum.UNSET));
    }

    rdrDemographic.setAge(
        dbPopulationEnumSet.stream()
            .map(this::toRdrAge)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

    if (rdrDemographic.getAge().isEmpty()) {
      rdrDemographic.setAge(Collections.singletonList(RdrWorkspaceDemographic.AgeEnum.UNSET));
    }

    return rdrDemographic;
  }

  RdrDemographicSurveyV2 toDemographicSurveyV2(DemographicSurveyV2 demoSurveyV2);

  // for round trip testing only
  DemographicSurveyV2 toModelDemographicSurveyV2(RdrDemographicSurveyV2 demoSurveyV2);
}
