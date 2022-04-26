package org.pmiops.workbench.rdr;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.rdr.model.RdrDegree;
import org.pmiops.workbench.rdr.model.RdrDisability;
import org.pmiops.workbench.rdr.model.RdrEducation;
import org.pmiops.workbench.rdr.model.RdrEthnicity;
import org.pmiops.workbench.rdr.model.RdrGender;
import org.pmiops.workbench.rdr.model.RdrRace;
import org.pmiops.workbench.rdr.model.RdrSexAtBirth;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;

public class RdrExportEnums {

  private static final BiMap<Race, RdrRace> CLIENT_TO_RDR_RACE =
      ImmutableBiMap.<Race, RdrRace>builder()
          .put(Race.AA, RdrRace.AA)
          .put(Race.AIAN, RdrRace.AIAN)
          .put(Race.ASIAN, RdrRace.ASIAN)
          .put(Race.NHOPI, RdrRace.NHOPI)
          .put(Race.WHITE, RdrRace.WHITE)
          .put(Race.PREFER_NO_ANSWER, RdrRace.PREFER_NOT_TO_ANSWER)
          .put(Race.NONE, RdrRace.NONE)
          .build();

  private static final BiMap<Ethnicity, RdrEthnicity> CLIENT_TO_RDR_ETHNICITY =
      ImmutableBiMap.<Ethnicity, RdrEthnicity>builder()
          .put(Ethnicity.HISPANIC, RdrEthnicity.HISPANIC)
          .put(Ethnicity.NOT_HISPANIC, RdrEthnicity.NOT_HISPANIC)
          .put(Ethnicity.PREFER_NO_ANSWER, RdrEthnicity.PREFER_NOT_TO_ANSWER)
          .build();

  private static final BiMap<GenderIdentity, RdrGender> CLIENT_TO_RDR_GENDER =
      ImmutableBiMap.<GenderIdentity, RdrGender>builder()
          .put(GenderIdentity.MAN, RdrGender.MAN)
          .put(GenderIdentity.WOMAN, RdrGender.WOMAN)
          .put(GenderIdentity.NON_BINARY, RdrGender.NON_BINARY)
          .put(GenderIdentity.TRANSGENDER, RdrGender.TRANSGENDER)
          .put(GenderIdentity.NONE_DESCRIBE_ME, RdrGender.NONE_DESCRIBE_ME)
          .put(GenderIdentity.PREFER_NO_ANSWER, RdrGender.PREFER_NOT_TO_ANSWER)
          .build();

  private static final BiMap<Education, RdrEducation> CLIENT_TO_RDR_EDUCATION =
      ImmutableBiMap.<Education, RdrEducation>builder()
          .put(Education.NO_EDUCATION, RdrEducation.NO_EDUCATION)
          .put(Education.GRADES_1_12, RdrEducation.GRADES_1_12)
          .put(Education.COLLEGE_GRADUATE, RdrEducation.COLLEGE_GRADUATE)
          .put(Education.UNDERGRADUATE, RdrEducation.UNDERGRADUATE)
          .put(Education.MASTER, RdrEducation.MASTER)
          .put(Education.DOCTORATE, RdrEducation.DOCTORATE)
          .build();

  private static final BiMap<SexAtBirth, RdrSexAtBirth> CLIENT_TO_RDR_SEX_AT_BIRTH =
      ImmutableBiMap.<SexAtBirth, RdrSexAtBirth>builder()
          .put(SexAtBirth.MALE, RdrSexAtBirth.MALE)
          .put(SexAtBirth.FEMALE, RdrSexAtBirth.FEMALE)
          .put(SexAtBirth.INTERSEX, RdrSexAtBirth.INTERSEX)
          .put(SexAtBirth.NONE_OF_THESE_DESCRIBE_ME, RdrSexAtBirth.NONE_OF_THESE_DESCRIBE_ME)
          .put(SexAtBirth.PREFER_NO_ANSWER, RdrSexAtBirth.PREFER_NOT_TO_ANSWER)
          .build();

  private static final BiMap<Disability, RdrDisability> CLIENT_TO_RDR_DISABILITY =
      ImmutableBiMap.<Disability, RdrDisability>builder()
          .put(Disability.TRUE, RdrDisability.YES)
          .put(Disability.FALSE, RdrDisability.NO)
          .build();

  private static final BiMap<SpecificPopulationEnum, RdrWorkspaceDemographic.RaceEthnicityEnum>
      CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY =
          ImmutableBiMap
              .<SpecificPopulationEnum, RdrWorkspaceDemographic.RaceEthnicityEnum>builder()
              .put(SpecificPopulationEnum.RACE_AA, RdrWorkspaceDemographic.RaceEthnicityEnum.AA)
              .put(SpecificPopulationEnum.RACE_AIAN, RdrWorkspaceDemographic.RaceEthnicityEnum.AIAN)
              .put(
                  SpecificPopulationEnum.RACE_ASIAN,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.ASIAN)
              .put(SpecificPopulationEnum.RACE_NHPI, RdrWorkspaceDemographic.RaceEthnicityEnum.NHPI)
              .put(SpecificPopulationEnum.RACE_MENA, RdrWorkspaceDemographic.RaceEthnicityEnum.MENA)
              .put(
                  SpecificPopulationEnum.RACE_HISPANIC,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.HISPANIC)
              .put(
                  SpecificPopulationEnum.RACE_MORE_THAN_ONE,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.MULTI)
              .build();

  private static final BiMap<SpecificPopulationEnum, RdrWorkspaceDemographic.AgeEnum>
      CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE =
          ImmutableBiMap.<SpecificPopulationEnum, RdrWorkspaceDemographic.AgeEnum>builder()
              .put(SpecificPopulationEnum.AGE_CHILDREN, RdrWorkspaceDemographic.AgeEnum.AGE_0_11)
              .put(
                  SpecificPopulationEnum.AGE_ADOLESCENTS, RdrWorkspaceDemographic.AgeEnum.AGE_12_17)
              .put(SpecificPopulationEnum.AGE_OLDER, RdrWorkspaceDemographic.AgeEnum.AGE_65_74)
              .put(
                  SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75,
                  RdrWorkspaceDemographic.AgeEnum.AGE_75_AND_MORE)
              .build();

  private static final BiMap<Degree, RdrDegree> CLIENT_TO_RDR_DEGREE =
      ImmutableBiMap.<Degree, RdrDegree>builder()
          .put(Degree.PHD, RdrDegree.PHD)
          .put(Degree.MD, RdrDegree.MD)
          .put(Degree.JD, RdrDegree.JD)
          .put(Degree.EDD, RdrDegree.EDD)
          .put(Degree.MSN, RdrDegree.MSN)
          .put(Degree.MS, RdrDegree.MS)
          .put(Degree.MA, RdrDegree.MA)
          .put(Degree.MBA, RdrDegree.MBA)
          .put(Degree.ME, RdrDegree.ME)
          .put(Degree.MSW, RdrDegree.MSW)
          .put(Degree.MPH, RdrDegree.MPH)
          .put(Degree.BA, RdrDegree.BA)
          .put(Degree.BS, RdrDegree.BS)
          .put(Degree.BSN, RdrDegree.BSN)
          .put(Degree.NONE, RdrDegree.UNSET)
          .build();

  public static RdrRace raceToRdrRace(Race race) {
    if (race == null) return null;
    return CLIENT_TO_RDR_RACE.get(race);
  }

  public static RdrEthnicity ethnicityToRdrEthnicity(Ethnicity ethnicity) {
    if (ethnicity == null) return null;
    return CLIENT_TO_RDR_ETHNICITY.get(ethnicity);
  }

  public static RdrGender genderToRdrGender(GenderIdentity genderIdentity) {
    if (genderIdentity == null) return null;
    return CLIENT_TO_RDR_GENDER.get(genderIdentity);
  }

  public static RdrEducation educationToRdrEducation(Education education) {
    if (education == null) return null;
    return CLIENT_TO_RDR_EDUCATION.get(education);
  }

  public static RdrSexAtBirth sexAtBirthToRdrSexAtBirth(SexAtBirth sexAtBirth) {
    if (sexAtBirth == null) return null;
    return CLIENT_TO_RDR_SEX_AT_BIRTH.get(sexAtBirth);
  }

  public static RdrDisability disabilityToRdrDisability(Disability disability) {
    if (disability == null) return RdrDisability.PREFER_NOT_TO_ANSWER;
    return CLIENT_TO_RDR_DISABILITY.get(disability);
  }

  public static RdrWorkspaceDemographic.RaceEthnicityEnum specificPopulationToRaceEthnicity(
      SpecificPopulationEnum specificPopulationEnum) {
    if (CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY.containsKey(specificPopulationEnum)) {
      return CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY.get(specificPopulationEnum);
    } else {
      return null;
    }
  }

  public static RdrWorkspaceDemographic.AgeEnum specificPopulationToAge(
      SpecificPopulationEnum specificPopulationEnum) {
    if (CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE.containsKey(specificPopulationEnum)) {
      return CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE.get(specificPopulationEnum);
    } else {
      return null;
    }
  }

  public static RdrDegree degreeToRdrDegree(Degree degree) {
    if (degree == null) return null;
    return CLIENT_TO_RDR_DEGREE.get(degree);
  }
}
