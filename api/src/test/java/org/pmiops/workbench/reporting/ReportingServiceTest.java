package org.pmiops.workbench.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.PdrResearcherDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
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
//  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
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
    assertThat(snapshot.getBigQueryPartitionKey()).isEqualTo(NOW_EPOCH_MILLI * 1000);
  }


  @Test
  public void testGetSnapshot_twoUsers() {
    addUsers();
    final ReportingSnapshot snapshot = reportingService.getSnapshot();
    assertThat(snapshot.getBigQueryPartitionKey()).isEqualTo(NOW_EPOCH_MILLI * 1000);
    assertThat(snapshot.getResearchers()).hasSize(2);
  }

  private void addUsers() {
    final DbUser user1 = createUser("Marge", false);
    final DbUser user2 = createUser("Homer", false);
  }

  private DbUser createUser(String givenName, boolean disabled) {
    DbUser user = new DbUser();
    user.setGivenName(GIVEN_NAME);
    user.setFamilyName(FAMILY_NAME);
    user.setUsername(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setOrganization(ORGANIZATION);
    user.setCurrentPosition(CURRENT_POSITION);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(disabled);
    return userDao.save(user);
  }
}
