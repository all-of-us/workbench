package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey.SatisfactionEnum;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyMapper;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(NewUserSatisfactionSurveyMapperImpl.class)
@SpringJUnitConfig
public class NewUserSatisfactionSurveyMapperTest {
  @Autowired private NewUserSatisfactionSurveyMapper mapper;

  @Test
  public void testToDbNewUserSatisfactionSurvey() {
    final DbUser user = new DbUser();
    final String additionalInfo = "I love the app";

    CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey =
        new CreateNewUserSatisfactionSurvey();
    createNewUserSatisfactionSurvey.setSatisfaction(SatisfactionEnum.SATISFIED);
    createNewUserSatisfactionSurvey.setAdditionalInfo(additionalInfo);
    DbNewUserSatisfactionSurvey mappedDbNewUserSatisfactionSurvey =
        mapper.toDbNewUserSatisfactionSurvey(createNewUserSatisfactionSurvey, user);

    assertThat(mappedDbNewUserSatisfactionSurvey.getSatisfaction().toString())
        .isEqualTo(createNewUserSatisfactionSurvey.getSatisfaction().toString());
    assertThat(mappedDbNewUserSatisfactionSurvey.getAdditionalInfo())
        .isEqualTo(createNewUserSatisfactionSurvey.getAdditionalInfo());
    assertThat(mappedDbNewUserSatisfactionSurvey.getUser()).isEqualTo(user);
  }
}
