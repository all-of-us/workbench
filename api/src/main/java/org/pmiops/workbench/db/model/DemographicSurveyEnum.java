package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.SexualOrientation;

public class DemographicSurveyEnum {
  private static final BiMap<Race, Short> CLIENT_TO_STORAGE_RACE =
      ImmutableBiMap.<Race, Short>builder()
          .put(Race.AA, (short) 1)
          .put(Race.AIAN, (short) 2)
          .put(Race.ASIAN, (short) 3)
          .put(Race.NHOPI, (short) 4)
          .put(Race.WHITE, (short) 5)
          .put(Race.PREFER_NO_ANSWER, (short) 6)
          .put(Race.NONE, (short) 7)
          .build();

  private static final BiMap<SexualOrientation, Short> CLIENT_TO_STORAGE_SEXUAL_ORIENTATION =
      ImmutableBiMap.<SexualOrientation, Short>builder()
          .put(SexualOrientation.LESBIAN, (short) 1)
          .put(SexualOrientation.GAY, (short) 2)
          .put(SexualOrientation.STRAIGHT, (short) 3)
          .put(SexualOrientation.BISEXUAL, (short) 4)
          .put(SexualOrientation.PREFER_NO_ANSWER, (short) 5)
          .put(SexualOrientation.NONE_OF_THESE_DESCRIBE_ME, (short) 6)
          .build();

  private static final BiMap<SexAtBirth, Short> CLIENT_TO_STORAGE_SEX_AT_BIRTH =
      ImmutableBiMap.<SexAtBirth, Short>builder()
          .put(SexAtBirth.FEMALE, (short) 1)
          .put(SexAtBirth.MALE, (short) 2)
          .put(SexAtBirth.INTERSEX, (short) 3)
          .put(SexAtBirth.PREFER_NO_ANSWER, (short) 4)
          .put(SexAtBirth.NONE_OF_THESE_DESCRIBE_ME, (short) 5)
          .build();

  private static final BiMap<Ethnicity, Short> CLIENT_TO_STORAGE_ETHNICITY =
      ImmutableBiMap.<Ethnicity, Short>builder()
          .put(Ethnicity.HISPANIC, (short) 1)
          .put(Ethnicity.NOT_HISPANIC, (short) 2)
          .put(Ethnicity.PREFER_NO_ANSWER, (short) 3)
          .build();

  private static final BiMap<GenderIdentity, Short> CLIENT_TO_STORAGE_GENDER_IDENTITY =
      ImmutableBiMap.<GenderIdentity, Short>builder()
          .put(GenderIdentity.MAN, (short) 1)
          .put(GenderIdentity.WOMAN, (short) 2)
          .put(GenderIdentity.NON_BINARY, (short) 3)
          .put(GenderIdentity.TRANSGENDER, (short) 4)
          .put(GenderIdentity.NONE_DESCRIBE_ME, (short) 5)
          .put(GenderIdentity.PREFER_NO_ANSWER, (short) 6)
          .build();

  private static final BiMap<Education, Short> CLIENT_TO_STORAGE_EDUCATION =
      ImmutableBiMap.<Education, Short>builder()
          .put(Education.NO_EDUCATION, (short) 1)
          .put(Education.GRADES_1_12, (short) 2)
          .put(Education.COLLEGE_GRADUATE, (short) 3)
          .put(Education.UNDERGRADUATE, (short) 4)
          .put(Education.MASTER, (short) 5)
          .put(Education.DOCTORATE, (short) 6)
          .put(Education.PREFER_NO_ANSWER, (short) 7)
          .build();

  private static final BiMap<Disability, Short> CLIENT_TO_STORAGE_DISABILITY =
      ImmutableBiMap.<Disability, Short>builder()
          .put(Disability.TRUE, (short) 1)
          .put(Disability.FALSE, (short) 2)
          .put(Disability.PREFER_NO_ANSWER, (short) 3)
          .build();

  public static Race raceFromStorage(Short race) {
    return CLIENT_TO_STORAGE_RACE.inverse().get(race);
  }

  public static Short raceToStorage(Race race) {
    return CLIENT_TO_STORAGE_RACE.get(race);
  }

  public static SexualOrientation sexualOrientationFromStorage(Short sexualOrientation) {
    return CLIENT_TO_STORAGE_SEXUAL_ORIENTATION.inverse().get(sexualOrientation);
  }

  public static Short sexualOrientationToStorage(SexualOrientation sexualOrientation) {
    return CLIENT_TO_STORAGE_SEXUAL_ORIENTATION.get(sexualOrientation);
  }

  public static SexAtBirth sexAtBirthFromStorage(Short sexAtBirth) {
    return CLIENT_TO_STORAGE_SEX_AT_BIRTH.inverse().get(sexAtBirth);
  }

  public static Short sexAtBirthToStorage(SexAtBirth sexAtBirth) {
    return CLIENT_TO_STORAGE_SEX_AT_BIRTH.get(sexAtBirth);
  }

  public static Ethnicity ethnicityFromStorage(Short ethnicity) {
    return CLIENT_TO_STORAGE_ETHNICITY.inverse().get(ethnicity);
  }

  public static Short ethnicityToStorage(Ethnicity ethnicity) {
    return CLIENT_TO_STORAGE_ETHNICITY.get(ethnicity);
  }

  public static Short genderIdentityToStorage(GenderIdentity genderIdentity) {
    return CLIENT_TO_STORAGE_GENDER_IDENTITY.get(genderIdentity);
  }

  public static GenderIdentity genderIdentityFromStorage(Short genderIdentity) {
    return CLIENT_TO_STORAGE_GENDER_IDENTITY.inverse().get(genderIdentity);
  }

  public static Short educationToStorage(Education education) {
    return CLIENT_TO_STORAGE_EDUCATION.get(education);
  }

  public static Education educationFromStorage(Short education) {
    return CLIENT_TO_STORAGE_EDUCATION.inverse().get(education);
  }

  public static Short disabilityToStorage(Disability disability) {
    return CLIENT_TO_STORAGE_DISABILITY.get(disability);
  }

  public static Short disabilityToStorage(Boolean disability) {
    if (disability != null) {
      return disability ? (short) 1 : (short) 2;
    } else {
      return null;
    }
  }

  public static Disability disabilityFromStorage(Short disability) {
    return CLIENT_TO_STORAGE_DISABILITY.inverse().get(disability);
  }

  // named such for MapStruct, which will automatically pick up on a function with this signature.
  // can't be named disabilityFromStorage because it would have the same args as the other one.
  public static Boolean boolyDisabilityFromStorage(Short disability) {
    if (disability != null) {
      return disability == 1;
    } else {
      return null;
    }
  }
}
