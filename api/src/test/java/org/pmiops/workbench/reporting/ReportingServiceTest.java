package org.pmiops.workbench.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingServiceTest {
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";
  private static final long NOW_EPOCH_MILLI = 1594404482000L;
  @MockBean private UserService mockUserService;
  @Autowired private ReportingService reportingService;

  @TestConfiguration
  @Import({
      ReportingMapperImpl.class,
      ReportingServiceImpl.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(Instant.ofEpochMilli(NOW_EPOCH_MILLI));
    }
  }

  @Test
  public void testGetSnapshot_noEntries() {
    final ReportingSnapshot snapshot = reportingService.getSnapshot();
    assertThat(snapshot.getCaptureTimestamp())
        .isEqualTo(NOW_EPOCH_MILLI * 1000);
  }

  @Test
  public void testGetSnapshot_twoUsers() {
    mockUsers();
    final ReportingSnapshot snapshot = reportingService.getSnapshot();
    assertThat(snapshot.getCaptureTimestamp().getMillis())
        .isEqualTo(NOW_EPOCH_MILLI);
    assertThat(snapshot.getResearchers()).hasSize(2);
    assertThat(snapshot.getResearchers().get(0).getResearcherId()).isEqualTo(101);
    assertThat(snapshot.getResearchers().get(0).getIsDisabled()).isFalse();
  }

  private void mockUsers() {
    final List<DbUser> users = ImmutableList.of(
        createFakeUser("Marge", false, 101L),
        createFakeUser("Homer", false, 102L));
    doReturn(users).when(mockUserService).getAllUsers();
  }

  private DbUser createFakeUser(String givenName, boolean disabled, long userId) {
    DbUser user = new DbUser();
    user.setUserId(userId);
    user.setGivenName(givenName);
    user.setFamilyName(FAMILY_NAME);
    user.setUsername(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setOrganization(ORGANIZATION);
    user.setCurrentPosition(CURRENT_POSITION);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(disabled);
    return user;
  }
}
