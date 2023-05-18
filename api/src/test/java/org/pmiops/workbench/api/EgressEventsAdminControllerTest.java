package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exfiltration.EgressLogService;
import org.pmiops.workbench.model.AuditEgressEventRequest;
import org.pmiops.workbench.model.AuditEgressEventResponse;
import org.pmiops.workbench.model.AuditEgressRuntimeLogEntry;
import org.pmiops.workbench.model.AuditEgressRuntimeLogGroup;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventStatus;
import org.pmiops.workbench.model.ListEgressEventsRequest;
import org.pmiops.workbench.model.ListEgressEventsResponse;
import org.pmiops.workbench.model.UpdateEgressEventRequest;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.PaginationToken;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.EgressEventMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
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
  @Autowired private BigQueryService mockBigQueryService;

  private DbUser user1;
  private DbUser user2;
  private DbUser user3;

  private DbWorkspace workspace1;
  private DbWorkspace workspace2;
  private DbWorkspace workspace3;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    EgressEventsAdminController.class,
    EgressEventMapperImpl.class,
    EgressLogService.class,
    FakeClockConfiguration.class,
    FakeJpaDateTimeConfiguration.class
  })
  @MockBean({
    BigQueryService.class,
    EgressEventAuditor.class,
  })
  static class Configuration {
    @Bean
    WorkbenchConfig config() {
      WorkbenchConfig c = new WorkbenchConfig();
      c.firecloud = new FireCloudConfig();
      c.firecloud.workspaceLogsProject = "fake-terra-logs";
      return c;
    }
  }

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
            new ListEgressEventsRequest().pageSize(1),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_1_WORKSPACE_1},
              {TestEgressEvent.USER_2_WORKSPACE_2},
              {TestEgressEvent.USER_3_WORKSPACE_1},
              {TestEgressEvent.USER_3_WORKSPACE_2},
              {TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(2),
            new TestEgressEvent[][] {
              {TestEgressEvent.USER_1_WORKSPACE_1, TestEgressEvent.USER_2_WORKSPACE_2},
              {TestEgressEvent.USER_3_WORKSPACE_1, TestEgressEvent.USER_3_WORKSPACE_2},
              {TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(3),
            new TestEgressEvent[][] {
              {
                TestEgressEvent.USER_1_WORKSPACE_1,
                TestEgressEvent.USER_2_WORKSPACE_2,
                TestEgressEvent.USER_3_WORKSPACE_1
              },
              {TestEgressEvent.USER_3_WORKSPACE_2, TestEgressEvent.NO_USER_NO_WORKSPACE}
            }),
        Arguments.of(
            new ListEgressEventsRequest().pageSize(4),
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
            new ListEgressEventsRequest().pageSize(5),
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
    int expectedTotalEvents = Arrays.stream(expectedPages).mapToInt(p -> p.length).sum();

    List<TestEgressEvent[]> gotPages = new ArrayList<>();
    do {
      ListEgressEventsResponse resp = controller.listEgressEvents(req).getBody();
      assertThat(resp.getTotalSize()).isEqualTo(expectedTotalEvents);

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

  @Test
  public void testListEgressEvents_notFoundUser() {
    assertThrows(
        NotFoundException.class,
        () ->
            controller.listEgressEvents(
                new ListEgressEventsRequest().sourceUserEmail("not-found@asdf.com")));
  }

  @Test
  public void testListEgressEvents_notFoundWorkspace() {
    assertThrows(
        NotFoundException.class,
        () ->
            controller.listEgressEvents(
                new ListEgressEventsRequest().sourceWorkspaceNamespace("not-found")));
  }

  @Test
  public void testListEgressEvents_badPageToken() {
    assertThrows(
        BadRequestException.class,
        () -> controller.listEgressEvents(new ListEgressEventsRequest().pageToken("malformed")));
  }

  @Test
  public void testListEgressEvents_offsetPageTokenOverscan() {
    assertThat(
            controller
                .listEgressEvents(
                    new ListEgressEventsRequest()
                        .pageToken(PaginationToken.of(13L, null, null, null).toBase64()))
                .getBody()
                .getEvents())
        .hasSize(0);
  }

  @Test
  public void testListEgressEvents_pageTokenWithChangedParameters() {
    saveNewEvent(user1, workspace1, timeMinusHours(1));
    saveNewEvent(user1, workspace1, timeMinusHours(2));

    String nextPageToken =
        controller
            .listEgressEvents(new ListEgressEventsRequest().pageSize(1))
            .getBody()
            .getNextPageToken();

    assertThrows(
        BadRequestException.class,
        () ->
            controller.listEgressEvents(
                new ListEgressEventsRequest().pageSize(10).pageToken(nextPageToken)));
  }

  @Test
  public void testUpdateEgressEvent_invalidId() {
    assertThrows(
        NotFoundException.class,
        () -> controller.updateEgressEvent("invalid", new UpdateEgressEventRequest()));
  }

  @Test
  public void testUpdateEgressEvent_notFoundEvent() {
    assertThrows(
        NotFoundException.class,
        () -> controller.updateEgressEvent("404", new UpdateEgressEventRequest()));
  }

  @Test
  public void testUpdateEgressEvent_eventPending() {
    String eventId = saveNewEventWithStatus(DbEgressEventStatus.PENDING);
    assertThrows(
        FailedPreconditionException.class,
        () ->
            controller.updateEgressEvent(
                eventId,
                new UpdateEgressEventRequest()
                    .egressEvent(
                        new EgressEvent().status(EgressEventStatus.VERIFIED_FALSE_POSITIVE))));
  }

  @Test
  public void testUpdateEgressEvent_badRequestNull() {
    String eventId = saveNewEventWithStatus(DbEgressEventStatus.REMEDIATED);
    assertThrows(
        BadRequestException.class,
        () ->
            controller.updateEgressEvent(
                eventId, new UpdateEgressEventRequest().egressEvent(new EgressEvent())));
  }

  @Test
  public void testUpdateEgressEvent_badRequestPending() {
    String eventId = saveNewEventWithStatus(DbEgressEventStatus.REMEDIATED);
    assertThrows(
        BadRequestException.class,
        () ->
            controller.updateEgressEvent(
                eventId,
                new UpdateEgressEventRequest()
                    .egressEvent(new EgressEvent().status(EgressEventStatus.PENDING))));
  }

  @Test
  public void testUpdateEgressEvent() {
    String eventId = saveNewEventWithStatus(DbEgressEventStatus.REMEDIATED);

    EgressEvent got =
        controller
            .updateEgressEvent(
                eventId,
                new UpdateEgressEventRequest()
                    .egressEvent(
                        new EgressEvent().status(EgressEventStatus.VERIFIED_FALSE_POSITIVE)))
            .getBody();

    assertThat(got.getStatus()).isEqualTo(EgressEventStatus.VERIFIED_FALSE_POSITIVE);

    // Call another endpoint to verify that the change was actually persisted
    List<EgressEvent> gotEvents =
        controller.listEgressEvents(new ListEgressEventsRequest()).getBody().getEvents();
    assertThat(gotEvents).hasSize(1);
    assertThat(gotEvents.get(0).getStatus()).isEqualTo(EgressEventStatus.VERIFIED_FALSE_POSITIVE);
  }

  @Test
  public void testAuditEgressEvent() throws Exception {
    String eventId = saveNewEvent(user1, workspace1, TIME0);

    List<List<AuditEgressRuntimeLogEntry>> expected =
        ImmutableList.of(
            ImmutableList.of(
                new AuditEgressRuntimeLogEntry().timestamp(TIME0.toString()).message("log1"),
                new AuditEgressRuntimeLogEntry()
                    .timestamp(TIME0.minus(Duration.ofMinutes(5)).toString())
                    .message("log2")),
            ImmutableList.of(),
            ImmutableList.of(
                new AuditEgressRuntimeLogEntry()
                    .timestamp(TIME0.minus(Duration.ofMinutes(2)).toString())
                    .message("log3")));

    Job mockJob = mock(Job.class);
    when(mockBigQueryService.startQuery(any())).thenReturn(mockJob);

    // Unfortunately we cannot use the variadic form of thenReturn with array inputs, as it is
    // a templatized method. Calling thenReturn in a loop is also disallowed by Junit.
    List<TableResult> bigQueryResults =
        expected.stream().map(this::logEntriesAsTableResult).collect(Collectors.toList());
    when(mockJob.getQueryResults(any()))
        .thenReturn(bigQueryResults.get(0))
        .thenReturn(bigQueryResults.get(1))
        .thenReturn(bigQueryResults.get(2));

    AuditEgressEventResponse got =
        controller.auditEgressEvent(eventId, new AuditEgressEventRequest()).getBody();

    assertThat(got.getEgressEvent().getEgressEventId()).isEqualTo(eventId);
    assertThat(got.getSumologicEvent()).isNotNull();

    List<List<AuditEgressRuntimeLogEntry>> gotEntries =
        got.getRuntimeLogGroups().stream()
            .map(AuditEgressRuntimeLogGroup::getEntries)
            .collect(Collectors.toList());
    assertThat(gotEntries).containsExactlyElementsIn(expected);
  }

  private Instant timeMinusHours(long h) {
    return TIME0.minus(Duration.ofHours(h));
  }

  private String saveNewEventWithStatus(DbEgressEventStatus status) {
    DbEgressEvent e = egressEventDao.save(newEvent().setStatus(status));
    return Long.toString(e.getEgressEventId());
  }

  private String saveNewEvent(DbUser user, DbWorkspace workspace, Instant created) {
    Instant originalTime = fakeClock.instant();
    fakeClock.setInstant(created);
    DbEgressEvent e = egressEventDao.save(newEvent().setUser(user).setWorkspace(workspace));

    fakeClock.setInstant(originalTime);
    return Long.toString(e.getEgressEventId());
  }

  private DbEgressEvent newEvent() {
    return new DbEgressEvent()
        .setStatus(DbEgressEventStatus.PENDING)
        .setEgressWindowSeconds(600L)
        .setSumologicEvent(
            "{\"egressWindowStart\": 123, \"timeWindowStart\": 123, \"timeWindowDuration\": 456}");
  }

  private DbUser saveNewUser(String username) {
    DbUser dbUser = new DbUser();
    dbUser.setUsername(username);
    return userDao.save(dbUser);
  }

  private DbWorkspace saveNewWorkspace(String workspaceNamespace) {
    return workspaceDao.save(new DbWorkspace().setWorkspaceNamespace(workspaceNamespace));
  }

  private TableResult logEntriesAsTableResult(List<AuditEgressRuntimeLogEntry> entries) {
    Field timestampField = Field.of("timestamp", LegacySQLTypeName.TIMESTAMP);
    Field messageField = Field.of("message", LegacySQLTypeName.STRING);
    Schema schema = Schema.of(timestampField, messageField);

    List<FieldValueList> tableRows =
        entries.stream()
            .map(
                e -> {
                  Instant ts = Instant.parse(e.getTimestamp());
                  return FieldValueList.of(
                      ImmutableList.of(
                          FieldValue.of(
                              Attribute.PRIMITIVE,
                              // This reverse engineers the complicated wire encoding used by
                              // BigQuery, per
                              // https://github.com/googleapis/java-bigquery/blob/13cc6e608fd501067f7c5dcd2f5b9a03c078b065/google-cloud-bigquery/src/main/java/com/google/cloud/bigquery/FieldValue.java#L184-L190
                              // The decimal contains microseconds, so we truncate before generating
                              // the decimal portion.
                              Double.toString(
                                  ((double) ts.getEpochSecond()) + ts.getNano() / 1000 / 1e6)),
                          FieldValue.of(Attribute.PRIMITIVE, e.getMessage())));
                })
            .collect(Collectors.toList());

    return new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));
  }
}
