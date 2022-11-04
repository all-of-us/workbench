package org.pmiops.workbench.profile;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ProfileAccessModules;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyService;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import({
  ProfileMapperImpl.class,
  AddressMapperImpl.class,
  CommonMappers.class,
  FakeClockConfiguration.class,
  DemographicSurveyMapperImpl.class,
  PageVisitMapperImpl.class
})
@SpringJUnitConfig
public class ProfileMapperTest {
  @Autowired private ProfileMapper mapper;
  @MockBean private NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;

  @Test
  public void testNewUserSatisfactionSurveyEligibility() {
    final DbUser user = new DbUser();
    when(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).thenReturn(true);
    final Profile profile =
        mapper.toModel(
            user,
            new VerifiedInstitutionalAffiliation(),
            new DbUserTermsOfService(),
            0.0,
            0.0,
            new ArrayList<>(),
            new ArrayList<>(),
            new ProfileAccessModules(),
            newUserSatisfactionSurveyService);

    assertThat(profile.getNewUserSatisfactionSurveyEligibility()).isTrue();
  }
}
