package org.pmiops.workbench.survey;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface NewUserSatisfactionSurveyMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  DbNewUserSatisfactionSurvey toDbNewUserSatisfactionSurvey(
      CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey, DbUser user);
}
