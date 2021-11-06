package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.JpaFakeDateTimeConfiguration;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ListEgressEventsRequest;
import org.pmiops.workbench.model.ListEgressEventsResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.EgressEventMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class EgressEventsAdminControllerTest {

  private static final Instant TIME0 = Instant.parse("2020-06-11T01:30:00.02Z");

  private static final String USER1_EMAIL = "user1@asdf.com";
  private static final String USER2_EMAIL = "user2@asdf.com";
  private static final String USER3_EMAIL = "user3@asdf.com";

  private static final String WORKSPACE1_NS = "ns1";
  private static final String WORKSPACE2_NS = "ns2";
  private static final String WORKSPACE3_NS = "ns3";

  @Autowired private EgressEventsAdminController controller;

  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private EgressEventDao egressEventDao;
  @Autowired private FakeClock fakeClock;

  private DbUser user1;
  private DbUser user2;
  private DbUser user3;

  private DbWorkspace workspace1;
  private DbWorkspace workspace2;
  private DbWorkspace workspace3;

  @TestConfiguration
  @Import({
    EgressEventsAdminController.class,
    EgressEventMapperImpl.class,
    CommonMappers.class,
    FakeClockConfiguration.class,
    JpaFakeDateTimeConfiguration.class
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    user1 = saveNewUser("user1@asdf.com");
    user2 = saveNewUser("user2@asdf.com");
    user3 = saveNewUser("user3@asdf.com");

    workspace1 = saveNewWorkspace("ns1");
    workspace2 = saveNewWorkspace("ns2");
    workspace3 = saveNewWorkspace("ns3");
  }

  @AfterEach
  public void tearDown() {
    egressEventDao.deleteAll();
    workspaceDao.deleteAll();
    userDao.deleteAll();
  }

  private enum TestEgressEvent {
    USER_1_WORKSPACE_1,
    USER_2_WORKSPACE_2,
    USER_3_WORKSPACE_1,
    USER_3_WORKSPACE_2,
    NO_USER_NO_WORKSPACE
  }

  private static Stream<Arguments> listEgressEventsCases() {
    return Stream.of(
        Arguments.of(
            new ListEgressEventsRequest(),
            new TestEgressEvent[][] {
              {
                TestEgressEvent.USER_1_WORKSPACE_1,
                TestEgressEvent.USER_2_WORKSPACE_2,
                TestEgressEvent.USER_3_WORKSPACE_1,
                TestEgressEvent.USER_3_WORKSPACE_2,
                TestEgressEvent.NO_USER_NO_WORKSPACE
              }
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(BigDecimal.valueOf(1)),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_1_WORKSPACE_1},
              {TestEgressEvent.USER_2_WORKSPACE_2},
              {TestEgressEvent.USER_3_WORKSPACE_1},
              {TestEgressEvent.USER_3_WORKSPACE_2},
              {TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(BigDecimal.valueOf(2)),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_1_WORKSPACE_1, TestEgressEvent.USER_2_WORKSPACE_2},
              {TestEgressEvent.USER_3_WORKSPACE_1, TestEgressEvent.USER_3_WORKSPACE_2},
              {TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(BigDecimal.valueOf(3)),
            new TestEgressEvent[][] {
              {
                TestEgressEvent.USER_1_WORKSPACE_1,
                TestEgressEvent.USER_2_WORKSPACE_2,
                TestEgressEvent.USER_3_WORKSPACE_1
              },
              {TestEgressEvent.USER_3_WORKSPACE_2, TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(BigDecimal.valueOf(4)),
            new TestEgressEvent[][] {
              {
                TestEgressEvent.USER_1_WORKSPACE_1,
                TestEgressEvent.USER_2_WORKSPACE_2,
                TestEgressEvent.USER_3_WORKSPACE_1,
                TestEgressEvent.USER_3_WORKSPACE_2
              },
              {TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(BigDecimal.valueOf(5)),
            new TestEgressEvent[][] {
              {
                TestEgressEvent.USER_1_WORKSPACE_1,
                TestEgressEvent.USER_2_WORKSPACE_2,
                TestEgressEvent.USER_3_WORKSPACE_1,
                TestEgressEvent.USER_3_WORKSPACE_2,
                TestEgressEvent.NO_USER_NO_WORKSPACE
              }
            }),
        Arguments.of(
            new ListEgressEventsRequest().sourceUserEmail(USER1_EMAIL),
            new TestEgressEvent[][] {{TestEgressEvent.USER_1_WORKSPACE_1}}),
        Arguments.of(
            new ListEgressEventsRequest().sourceUserEmail(USER2_EMAIL),
            new TestEgressEvent[][] {{TestEgressEvent.USER_2_WORKSPACE_2}}),
        Arguments.of(
            new ListEgressEventsRequest().sourceUserEmail(USER3_EMAIL),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_3_WORKSPACE_1, TestEgressEvent.USER_3_WORKSPACE_2}
            }),
        Arguments.of(
            new ListEgressEventsRequest().sourceWorkspaceNamespace(WORKSPACE1_NS),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_1_WORKSPACE_1, TestEgressEvent.USER_3_WORKSPACE_1}
            }),
        Arguments.of(
            new ListEgressEventsRequest().sourceWorkspaceNamespace(WORKSPACE2_NS),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_2_WORKSPACE_2, TestEgressEvent.USER_3_WORKSPACE_2}
            }),
        Arguments.of(
            new ListEgressEventsRequest().sourceWorkspaceNamespace(WORKSPACE3_NS),
            new TestEgressEvent[][] {{}}),
        Arguments.of(
            new ListEgressEventsRequest()
                .sourceUserEmail(USER1_EMAIL)
                .sourceWorkspaceNamespace(WORKSPACE2_NS),
            new TestEgressEvent[][] {{}}),
        Arguments.of(
            new ListEgressEventsRequest()
                .sourceUserEmail(USER3_EMAIL)
                .sourceWorkspaceNamespace(WORKSPACE2_NS),
            new TestEgressEvent[][] {{TestEgressEvent.USER_3_WORKSPACE_2}}));
  }

  @ParameterizedTest
  @MethodSource("listEgressEventsCases")
  public void testListEgressEvents(
      ListEgressEventsRequest initialRequest, TestEgressEvent[][] expectedPages) {
    Map<String, TestEgressEvent> egressEventIds =
        ImmutableMap.<String, TestEgressEvent>builder()
            .put(
                saveNewEvent(user1, workspace1, timeMinusHours(1)),
                TestEgressEvent.USER_1_WORKSPACE_1)
            .put(
                saveNewEvent(user2, workspace2, timeMinusHours(2)),
                TestEgressEvent.USER_2_WORKSPACE_2)
            .put(
                saveNewEvent(user3, workspace1, timeMinusHours(3)),
                TestEgressEvent.USER_3_WORKSPACE_1)
            .put(
                saveNewEvent(user3, workspace2, timeMinusHours(4)),
                TestEgressEvent.USER_3_WORKSPACE_2)
            .put(saveNewEvent(null, null, timeMinusHours(5)), TestEgressEvent.NO_USER_NO_WORKSPACE)
            .build();

    ListEgressEventsRequest req =
        new ListEgressEventsRequest()
            .sourceUserEmail(initialRequest.getSourceUserEmail())
            .sourceWorkspaceNamespace(initialRequest.getSourceWorkspaceNamespace())
            .pageSize(initialRequest.getPageSize());

    List<TestEgressEvent[]> gotPages = new ArrayList<>();
    do {
      ListEgressEventsResponse resp = controller.listEgressEvents(req).getBody();
      gotPages.add(
          resp.getEvents().stream()
              .map(e -> egressEventIds.get(e.getEgressEventId()))
              .collect(Collectors.toList())
              .toArray(new TestEgressEvent[] {}));
      req.setPageToken(resp.getNextPageToken());
    } while (!Strings.isNullOrEmpty(req.getPageToken()));

    assertThat(gotPages).hasSize(expectedPages.length);
    for (int i = 0; i < gotPages.size(); i++) {
      assertThat(gotPages.get(i)).isEqualTo(expectedPages[i]);
    }
  }

  private Instant timeMinusHours(Integer h) {
    return TIME0.minus(Duration.ofHours(h));
  }

  private String saveNewEvent(DbUser user, DbWorkspace workspace, Instant created) {
    Instant originalTime = fakeClock.instant();
    fakeClock.setInstant(created);
    DbEgressEvent e =
        egressEventDao.save(
            new DbEgressEvent()
                .setUser(user)
                .setWorkspace(workspace)
                .setStatus(DbEgressEventStatus.PENDING));

    fakeClock.setInstant(originalTime);
    return Long.toString(e.getEgressEventId());
  }

  private DbUser saveNewUser(String username) {
    DbUser dbUser = new DbUser();
    dbUser.setUsername(username);
    return userDao.save(dbUser);
  }

  private DbWorkspace saveNewWorkspace(String workspaceNamespace) {
    return workspaceDao.save(new DbWorkspace().setWorkspaceNamespace(workspaceNamespace));
  }
}
