package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.WorkspaceTargetProperty;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

public class WorkspaceAuditorTest {

  private static final long WORKSPACE_1_DB_ID = 101L;
  private static final long WORKSPACE_2_DB_ID = 201L;
  private static final long REMOVED_USER_ID = 301L;
  private static final long ADDED_USER_ID = 401L;

  private Workspace workspace1;
  private DbWorkspace dbWorkspace1;

  @Autowired private WorkspaceAuditor workspaceAuditor;
  @MockBean private Provider<DbUser> mockUserProvider;
  @MockBean private ActionAuditService mockActionAuditService;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventCollectionCaptor;
  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  @TestConfiguration
  @Import({
    ActionAuditTestConfig.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    ConceptSetMapperImpl.class,
    CommonMappers.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceAuditorImpl.class,
    WorkspaceMapperImpl.class
  })
  @MockBean({UserDao.class, ConceptSetService.class, CohortService.class})
  static class Config {}

  @BeforeEach
  public void setUp() {
    final ResearchPurpose researchPurpose1 =
        new ResearchPurpose()
            .additionalNotes("I really like the cloud.")
            .approved(true)
            .ancestry(false)
            .anticipatedFindings("knowledge")
            .commercialPurpose(true)
            .controlSet(false)
            .diseaseFocusedResearch(true)
            .diseaseOfFocus("lack of focus")
            .drugDevelopment(true)
            .educational(false)
            .intendedStudy("stubbed toes")
            .scientificApproach("wild conjecture")
            .methodsDevelopment(true)
            .otherPopulationDetails("strictly Rhode Islanders")
            .otherPurpose(true)
            .otherPurposeDetails("deriving lottery numbers")
            .ethics(false)
            .populationDetails(Collections.emptyList())
            .populationHealth(true)
            .reasonForAllOfUs("the biggest data")
            .reviewRequested(false)
            .socialBehavioral(true)
            .timeRequested(1000L)
            .timeReviewed(10000L)
            .disseminateResearchFindingList(Collections.emptyList())
            .researchOutcomeList(Collections.emptyList())
            .needsReviewPrompt(false);
    final long now = System.currentTimeMillis();

    workspace1 =
        new Workspace()
            .id("fc-id-1")
            .etag("etag_1")
            .name("DbWorkspace 1")
            .namespace("aou-rw-local1-c4be869a")
            .cdrVersionId("1")
            .creator("user@fake-research-aou.org")
            .billingAccountName("big-bux")
            .billingAccountType(BillingAccountType.FREE_TIER)
            .googleBucketName("bucket o' science")
            .accessTierShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .researchPurpose(researchPurpose1)
            .billingStatus(BillingStatus.ACTIVE)
            .creationTime(now)
            .lastModifiedTime(now)
            .published(false);

    dbWorkspace1 = new DbWorkspace();
    dbWorkspace1.setWorkspaceId(WORKSPACE_1_DB_ID);
    dbWorkspace1.setLastAccessedTime(new Timestamp(now));
    dbWorkspace1.setLastModifiedTime(new Timestamp(now));
    dbWorkspace1.setCreationTime(new Timestamp(now));

    // By default, have the mock user provider return the test-config default user.
    doReturn(ActionAuditTestConfig.getUser()).when(mockUserProvider).get();
  }

  @Test
  public void testFiresCreateWorkspaceEvents() {
    workspaceAuditor.fireCreateAction(workspace1, WORKSPACE_1_DB_ID);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(WorkspaceTargetProperty.values().length);
    Optional<ActionAuditEvent> firstEvent = eventsSent.stream().findFirst();
    assertThat(firstEvent.isPresent()).isTrue();
    assertThat(firstEvent.map(ActionAuditEvent::getActionType).orElse(null))
        .isEqualTo(ActionType.CREATE);
    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getActionType)
                .collect(Collectors.toSet())
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testFiresDeleteWorkspaceEvent() {
    workspaceAuditor.fireDeleteAction(dbWorkspace1);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent eventSent = eventCaptor.getValue();
    assertThat(eventSent.getActionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.getTimestamp()).isEqualTo(ActionAuditTestConfig.INSTANT.toEpochMilli());
  }

  @Test
  public void testFiresDuplicateEvent() {
    final Workspace workspace2 = clone(workspace1).id(String.valueOf(WORKSPACE_2_DB_ID));
    workspaceAuditor.fireDuplicateAction(
        dbWorkspace1.getWorkspaceId(), WORKSPACE_2_DB_ID, workspace2);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    final Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();

    // 1 workspace-from + N workspace-to attribute events
    final int expectedEvents = 1 + WorkspaceTargetProperty.values().length;
    assertThat(eventsSent).hasSize(expectedEvents);

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::getActionId).distinct().count())
        .isEqualTo(1);

    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getTargetType)
                .allMatch(t -> t.equals(TargetType.WORKSPACE)))
        .isTrue();

    ImmutableSet<ActionType> expectedActionTypes =
        ImmutableSet.of(ActionType.DUPLICATE_FROM, ActionType.DUPLICATE_TO);
    ImmutableSet<ActionType> actualActionTypes =
        eventsSent.stream()
            .map(ActionAuditEvent::getActionType)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(actualActionTypes).containsExactlyElementsIn(expectedActionTypes);
  }

  @Test
  public void testFiresCollaborateAction() {
    final ImmutableMap<Long, String> aclsByUserId =
        ImmutableMap.of(
            ActionAuditTestConfig.ADMINISTRATOR_USER_ID,
            WorkspaceAccessLevel.OWNER.toString(),
            REMOVED_USER_ID,
            WorkspaceAccessLevel.NO_ACCESS.toString(),
            ADDED_USER_ID,
            WorkspaceAccessLevel.READER.toString());
    workspaceAuditor.fireCollaborateAction(dbWorkspace1.getWorkspaceId(), aclsByUserId);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();

    // 1 workspace + N user COLLABORATE events
    final int expectedEvents = 1 + aclsByUserId.size();
    assertThat(eventsSent).hasSize(expectedEvents);

    Map<String, Long> countByTargetType =
        eventsSent.stream()
            .collect(
                Collectors.groupingBy(e -> e.getTargetType().toString(), Collectors.counting()));

    assertThat(countByTargetType.get(TargetType.WORKSPACE.toString())).isEqualTo(1);
    assertThat(countByTargetType.get(TargetType.USER.toString())).isEqualTo(3);

    Optional<String> targetPropertyMaybe =
        eventsSent.stream()
            .filter(e -> e.getTargetType() == TargetType.USER)
            .findFirst()
            .flatMap(e -> Optional.ofNullable(e.getTargetPropertyMaybe()));

    assertThat(targetPropertyMaybe.isPresent()).isTrue();
    assertThat(targetPropertyMaybe.get()).isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::getActionId).distinct().count())
        .isEqualTo(1);

    Optional<ActionAuditEvent> readerEventMaybe =
        eventsSent.stream()
            .filter(
                e ->
                    e.getTargetType() == TargetType.USER
                        && e.getTargetIdMaybe() != null
                        && e.getTargetIdMaybe().equals(ADDED_USER_ID))
            .findFirst();
    assertThat(readerEventMaybe.isPresent()).isTrue();
    assertThat(readerEventMaybe.get().getTargetPropertyMaybe()).isNotNull();
    assertThat(readerEventMaybe.get().getTargetPropertyMaybe())
        .isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());
    assertThat(readerEventMaybe.get().getNewValueMaybe())
        .isEqualTo(WorkspaceAccessLevel.READER.toString());
    assertThat(readerEventMaybe.get().getPreviousValueMaybe()).isNull();
  }

  @Test
  public void testCollaborateWithEmptyMapDoesNothing() {
    workspaceAuditor.fireCollaborateAction(WORKSPACE_1_DB_ID, Collections.emptyMap());
    verifyZeroInteractions(mockActionAuditService);
  }

  @Test
  public void testDoesNotThrowWhenUserProviderFails() {
    doReturn(null).when(mockUserProvider).get();
    workspaceAuditor.fireDeleteAction(dbWorkspace1);
  }

  @Test
  public void testFireEditAction_sendsNoEventsForSameWorkspace() {
    workspaceAuditor.fireEditAction(workspace1, workspace1, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    assertThat(eventCollectionCaptor.getValue()).isEmpty();
  }

  @Test
  public void testFireEditAction_sendsChangedProperties() {
    final ResearchPurpose editedResearchPurpose =
        clone(workspace1.getResearchPurpose())
            // changes
            .intendedStudy("height")
            .additionalNotes("tall folks are tall")
            .anticipatedFindings("something to earn tenure")
            .timeReviewed(workspace1.getResearchPurpose().getTimeReviewed() + 1000L)
            .controlSet(!workspace1.getResearchPurpose().getControlSet());
    final int rpChanges = 5;

    Workspace editedWorkspace =
        clone(workspace1)
            // changes included above
            .researchPurpose(editedResearchPurpose)
            // changes
            .name("a new name")
            .namespace("a new namespace")
            .creator("user10@fake-research-aou.org")
            .published(!workspace1.getPublished());
    final int wsChanges = 4;

    workspaceAuditor.fireEditAction(workspace1, editedWorkspace, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());

    Collection<ActionAuditEvent> events = eventCollectionCaptor.getValue();
    assertThat(events).hasSize(rpChanges + wsChanges);
  }

  private Workspace clone(Workspace in) {
    return new Workspace()
        .id(in.getId())
        .etag(in.getEtag())
        .name(in.getName())
        .namespace(in.getNamespace())
        .cdrVersionId(in.getCdrVersionId())
        .creator(in.getCreator())
        .billingAccountName(in.getBillingAccountName())
        .billingAccountType(in.getBillingAccountType())
        .googleBucketName(in.getGoogleBucketName())
        .accessTierShortName(in.getAccessTierShortName())
        .researchPurpose(in.getResearchPurpose())
        .billingStatus(in.getBillingStatus())
        .creationTime(in.getCreationTime())
        .lastModifiedTime(in.getLastModifiedTime())
        .published(in.getPublished());
  }

  private ResearchPurpose clone(ResearchPurpose in) {
    return new ResearchPurpose()
        .additionalNotes(in.getAdditionalNotes())
        .approved(in.getApproved())
        .ancestry(in.getAncestry())
        .anticipatedFindings(in.getAnticipatedFindings())
        .commercialPurpose(in.getCommercialPurpose())
        .controlSet(in.getControlSet())
        .diseaseFocusedResearch(in.getDiseaseFocusedResearch())
        .diseaseOfFocus(in.getDiseaseOfFocus())
        .drugDevelopment(in.getDrugDevelopment())
        .educational(in.getEducational())
        .intendedStudy(in.getIntendedStudy())
        .scientificApproach(in.getScientificApproach())
        .methodsDevelopment(in.getMethodsDevelopment())
        .otherPopulationDetails(in.getOtherPopulationDetails())
        .otherPurpose(in.getOtherPurpose())
        .otherPurposeDetails(in.getOtherPurposeDetails())
        .ethics(in.getEthics())
        .populationDetails(in.getPopulationDetails())
        .populationHealth(in.getPopulationHealth())
        .reasonForAllOfUs(in.getReasonForAllOfUs())
        .reviewRequested(in.getReviewRequested())
        .socialBehavioral(in.getSocialBehavioral())
        .timeRequested(in.getTimeRequested())
        .timeReviewed(in.getTimeReviewed())
        .disseminateResearchFindingList(in.getDisseminateResearchFindingList())
        .otherDisseminateResearchFindings(in.getOtherDisseminateResearchFindings())
        .researchOutcomeList(in.getResearchOutcomeList())
        .needsReviewPrompt(in.getNeedsReviewPrompt());
  }
}
