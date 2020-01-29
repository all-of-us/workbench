package org.pmiops.workbench.auth;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ProfileServiceTest {

  @TestConfiguration
  @MockBean({FreeTierBillingService.class})
  @Import(ProfileService.class)
  static class Configuration {}

  @Autowired ProfileService profileService;

  @Test
  public void testGetProfile_empty() {
    assertThat(profileService.getProfile(new DbUser())).isNotNull();
  }

  @Test
  public void testGetProfile_emptyDemographics() {
    // Regression coverage for RW-4219.
    DbUser user = new DbUser();
    user.setDemographicSurvey(new DbDemographicSurvey());
    assertThat(profileService.getProfile(user)).isNotNull();
  }

  @Test
  public void testReturnsLastAcknowledgedTermsOfService() {
    // The Profile service is responsible for sorting the ToS signing events for a given user
    // and setting Profile fields based on the latest one.
    DbUserTermsOfService v1 = new DbUserTermsOfService();
    v1.setTosVersion(1);
    v1.setAgreementTime(new Timestamp(1));

    DbUserTermsOfService v2 = new DbUserTermsOfService();
    v2.setTosVersion(2);
    v2.setAgreementTime(new Timestamp(2));

    DbUser user = new DbUser();
    user.setTermsOfServiceRows(ImmutableList.of(v1, v2));
    Profile profile = profileService.getProfile(user);
    assertThat(profile.getLatestTermsOfServiceVersion()).isEqualTo(2);
    assertThat(profile.getLatestTermsOfServiceTime()).isEqualTo(2);
  }
}
