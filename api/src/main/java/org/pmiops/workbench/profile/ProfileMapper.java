package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.NonAcademicAffiliation;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface ProfileMapper {
  Profile dbUserToProfile(DbUser dbUser);

  DbUser profileToDbUser(Profile profile);

  static Authority authorityFromStorage(Short authority) {
    return DbStorageEnums.authorityFromStorage(authority);
  }

  static Short authorityToStorage(Authority authority) {
    return DbStorageEnums.authorityToStorage(authority);
  }

  static Degree degreeFromStorage(Short degree) {
    return DbStorageEnums.degreeFromStorage(degree);
  }

  static Short degreeToStorage(Degree degree) {
    return DbStorageEnums.degreeToStorage(degree);
  }

  static Education educationFromStorage(Short education) {
    return DbStorageEnums.educationFromStorage(education);
  }

  static Short educationToStorage(Education education) {
    return DbStorageEnums.educationToStorage(education);
  }

  static Ethnicity ethnicityFromStorage(Short ethnicity) {
    return DbStorageEnums.ethnicityFromStorage(ethnicity);
  }

  static Short ethnicityToStorage(Ethnicity ethnicity) {
    return DbStorageEnums.ethnicityToStorage(ethnicity);
  }

  static EmailVerificationStatus emailVerificationStatusFromStorage(Short emailVerificationStatus) {
    return DbStorageEnums.emailVerificationStatusFromStorage(emailVerificationStatus);
  }

  static Short emailVerificationStatusToStorage(EmailVerificationStatus emailVerificationStatus) {
    return DbStorageEnums.emailVerificationStatusToStorage(emailVerificationStatus);
  }

  static GenderIdentity genderIdentityFromStorage(Short genderIdentity) {
    return DbStorageEnums.genderIdentityFromStorage(genderIdentity);
  }

  static Short genderIdentityToStorage(GenderIdentity genderIdentity) {
    return DbStorageEnums.genderIdentityToStorage(genderIdentity);
  }

  static NonAcademicAffiliation nonAcademicAffiliationFromStorage(Short nonAcademicAffiliation) {
    return DbStorageEnums.nonAcademicAffiliationFromStorage(nonAcademicAffiliation);
  }

  static Short nonAcademicAffiliationToStorage(NonAcademicAffiliation nonAcademicAffiliation) {
    return DbStorageEnums.nonAcademicAffiliationToStorage(nonAcademicAffiliation);
  }

  static Race raceFromStorage(Short race) {
    return DbStorageEnums.raceFromStorage(race);
  }

  static Short raceToStorage(Race race) {
    return DbStorageEnums.raceToStorage(race);
  }

  static SexAtBirth sexAtBirthFromStorage(Short sexAtBirth) {
    return DbStorageEnums.sexAtBirthFromStorage(sexAtBirth);
  }

  static Short sexAtBirthToStorage(SexAtBirth sexAtBirth) {
    return DbStorageEnums.sexAtBirthToStorage(sexAtBirth);
  }
}
