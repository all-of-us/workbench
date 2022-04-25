package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface DemographicSurveyMapper {
  @Mapping(target = "disability", source = "disabilityEnum")
  @Mapping(target = "education", source = "educationEnum")
  @Mapping(target = "ethnicity", source = "ethnicityEnum")
  @Mapping(target = "genderIdentityList", source = "genderIdentityEnumList")
  @Mapping(target = "race", source = "raceEnum")
  @Mapping(target = "sexAtBirth", source = "sexAtBirthEnum")
  DemographicSurvey dbDemographicSurveyToDemographicSurvey(DbDemographicSurvey dbDemographicSurvey);

  @Mapping(target = "disabilityEnum", source = "disability")
  @Mapping(target = "educationEnum", source = "education")
  @Mapping(target = "ethnicityEnum", source = "ethnicity")
  @Mapping(target = "genderIdentityEnumList", source = "genderIdentityList")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "raceEnum", source = "race")
  @Mapping(target = "sexAtBirthEnum", source = "sexAtBirth")
  @Mapping(target = "user", ignore = true) // set by UserService.createUser
  @Mapping(target = "year_of_birth", source = "yearOfBirth")
  DbDemographicSurvey demographicSurveyToDbDemographicSurvey(DemographicSurvey demographicSurvey);
}
