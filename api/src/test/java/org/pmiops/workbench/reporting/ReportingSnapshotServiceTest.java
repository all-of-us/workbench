package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceService;
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
public class ReportingSnapshotServiceTest {
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";
  private static final long NOW_EPOCH_MILLI = 1594404482000L;
  private static final String USER_GIVEN_NAME_1 = "Marge";
  private static final boolean USER_DISABLED_1 = false;
  private static final long USER_ID_1 = 101L;

  @MockBean private Random mockRandom;
  @MockBean private UserService mockUserService;
  @MockBean private WorkspaceService mockWorkspaceService;

  @Autowired private ReportingSnapshotService reportingSnapshotService;

  private static final TestMockFactory TEST_MOCK_FACTORY = new TestMockFactory();

  @TestConfiguration
  @Import({
    CommonMappers.class,
    ReportingMapperImpl.class,
    ReportingSnapshotServiceImpl.class,
    ReportingUploadServiceImpl.class
  })
  @MockBean({BigQueryService.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(Instant.ofEpochMilli(NOW_EPOCH_MILLI));
    }
  }

  @Before
  public void setup() {
    // Return "random" numbers 100, 101, 102...
    doAnswer(
            new Answer<Long>() {
              private long lastValue = 100;

              public Long answer(InvocationOnMock invocation) {
                return lastValue++;
              }
            })
        .when(mockRandom)
        .nextLong();
  }

  public void mockWorkspaces() {
    final DbWorkspace dbWorkspace1 = stubDbWorkspace("aou-rw-123456", "A Tale of Two Cities", 101L);
    final DbWorkspace dbWorkspace2 = stubDbWorkspace("aou-rw-789", "Moby Dick", 202L);

    doReturn(ImmutableList.of(dbWorkspace1, dbWorkspace2))
        .when(mockWorkspaceService)
        .getAllActiveWorkspaces();
  }

  @Test
  public void testGetSnapshot_noEntries() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertThat(snapshot.getCaptureTimestamp()).isEqualTo(NOW_EPOCH_MILLI);
    assertThat(snapshot.getResearchers()).isEmpty();
    assertThat(snapshot.getWorkspaces()).isEmpty();
  }

  @Test
  public void testGetSnapshot_someEntries() {
    mockUsers();
    mockWorkspaces();

    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertThat((double) snapshot.getCaptureTimestamp()).isWithin(100.0).of(NOW_EPOCH_MILLI);
    assertThat(snapshot.getResearchers()).hasSize(2);
    final ReportingResearcher researcher1 = snapshot.getResearchers().get(0);
    assertThat(researcher1.getResearcherId()).isEqualTo(USER_ID_1);
    assertThat(researcher1.getIsDisabled()).isEqualTo(USER_DISABLED_1);
    assertThat(researcher1.getFirstName()).isEqualTo(USER_GIVEN_NAME_1);
    assertThat(researcher1.getUsername()).isEqualTo(PRIMARY_EMAIL);

    assertThat(snapshot.getWorkspaces()).hasSize(2);
    final ReportingWorkspace workspace1 = snapshot.getWorkspaces().get(0);
    assertThat(workspace1.getWorkspaceId()).isEqualTo(101L);
    assertThat(workspace1.getName()).isEqualTo("A Tale of Two Cities");
    assertThat(workspace1.getFakeSize()).isEqualTo(100L);
    assertThat(workspace1.getCreatorId()).isNull(); // not stubbed
  }

  private void mockUsers() {
    final List<DbUser> users =
        ImmutableList.of(
            createFakeUser(USER_GIVEN_NAME_1, USER_DISABLED_1, USER_ID_1),
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

  private DbWorkspace stubDbWorkspace(
      String workspaceNamespace, String workspaceName, long workspaceId) {
    final Workspace workspace1 =
        TEST_MOCK_FACTORY.createWorkspace(workspaceNamespace, workspaceName);
    return TestMockFactory.createDbWorkspaceStub(workspace1, workspaceId);
  }
}
