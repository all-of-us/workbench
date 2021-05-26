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
  @Mapping(target = "yearOfBirth", source = "year_of_birth")
  @Mapping(target = "genderIdentityList", source = "genderIdentityEnums")
  @Mapping(target = "race", source = "raceEnums")
  @Mapping(target = "sexAtBirth", source = "sexAtBirthEnums")
  DemographicSurvey dbDemographicSurveyToDemographicSurvey(DbDemographicSurvey dbDemographicSurvey);

  @Mapping(target = "disabilityEnum", source = "disability")
  @Mapping(target = "educationEnum", source = "education")
  @Mapping(target = "ethnicityEnum", source = "ethnicity")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true) // set by UserService.createUser
  @Mapping(target = "year_of_birth", source = "yearOfBirth")
  @Mapping(target = "genderIdentityEnums", source = "genderIdentityList")
  @Mapping(target = "raceEnums", source = "race")
  @Mapping(target = "sexAtBirthEnums", source = "sexAtBirth")
  DbDemographicSurvey demographicSurveyToDbDemographicSurvey(DemographicSurvey demographicSurvey);
}
