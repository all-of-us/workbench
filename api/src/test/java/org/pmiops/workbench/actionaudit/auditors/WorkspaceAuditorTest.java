package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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
            .researchOutcomeList(Collections.emptyList());
    final long now = System.currentTimeMillis();

    User creator = new User();
    creator.setUserName("user@fake-research-aou.org");

    workspace1 =
        new Workspace()
            .etag("etag_1")
            .name("DbWorkspace 1")
            .terraName("dbworkspace1")
            .namespace("aou-rw-local1-c4be869a")
            .cdrVersionId("1")
            .creatorUser(creator)
            .billingAccountName("big-bux")
            .googleBucketName("bucket o' science")
            .accessTierShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .researchPurpose(researchPurpose1)
            .billingStatus(BillingStatus.ACTIVE)
            .creationTime(now)
            .lastModifiedTime(now);

    dbWorkspace1 = new DbWorkspace();
    dbWorkspace1.setWorkspaceId(WORKSPACE_1_DB_ID);
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
    assertThat(firstEvent.map(ActionAuditEvent::actionType).orElse(null))
        .isEqualTo(ActionType.CREATE);
    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::actionType)
                .collect(Collectors.toSet())
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testFiresDeleteWorkspaceEvent() {
    workspaceAuditor.fireDeleteAction(dbWorkspace1);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent eventSent = eventCaptor.getValue();
    assertThat(eventSent.actionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.timestamp()).isEqualTo(ActionAuditTestConfig.INSTANT.toEpochMilli());
  }

  @Test
  public void testFiresDuplicateEvent() {
    final Workspace workspace2 = clone(workspace1).terraName(String.valueOf(WORKSPACE_2_DB_ID));
    workspaceAuditor.fireDuplicateAction(
        dbWorkspace1.getWorkspaceId(), WORKSPACE_2_DB_ID, workspace2);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    final Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();

    // 1 workspace-from + N workspace-to attribute events
    final int expectedEvents = 1 + WorkspaceTargetProperty.values().length;
    assertThat(eventsSent).hasSize(expectedEvents);

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::actionId).distinct().count()).isEqualTo(1);

    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::targetType)
                .allMatch(t -> t.equals(TargetType.WORKSPACE)))
        .isTrue();

    ImmutableSet<ActionType> expectedActionTypes =
        ImmutableSet.of(ActionType.DUPLICATE_FROM, ActionType.DUPLICATE_TO);
    ImmutableSet<ActionType> actualActionTypes =
        eventsSent.stream()
            .map(ActionAuditEvent::actionType)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(actualActionTypes).containsExactlyElementsIn(expectedActionTypes);
  }

  @Test
  public void testFiresCollaborateAction() {
    final ImmutableMap<Long, String> aclByUserId =
        ImmutableMap.of(
            ActionAuditTestConfig.ADMINISTRATOR_USER_ID,
            WorkspaceAccessLevel.OWNER.toString(),
            REMOVED_USER_ID,
            WorkspaceAccessLevel.NO_ACCESS.toString(),
            ADDED_USER_ID,
            WorkspaceAccessLevel.READER.toString());
    workspaceAuditor.fireCollaborateAction(dbWorkspace1.getWorkspaceId(), aclByUserId);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();

    // 1 workspace + N user COLLABORATE events
    final int expectedEvents = 1 + aclByUserId.size();
    assertThat(eventsSent).hasSize(expectedEvents);

    Map<String, Long> countByTargetType =
        eventsSent.stream()
            .collect(Collectors.groupingBy(e -> e.targetType().toString(), Collectors.counting()));

    assertThat(countByTargetType.get(TargetType.WORKSPACE.toString())).isEqualTo(1);
    assertThat(countByTargetType.get(TargetType.USER.toString())).isEqualTo(3);

    Optional<String> targetPropertyMaybe =
        eventsSent.stream()
            .filter(e -> e.targetType() == TargetType.USER)
            .findFirst()
            .map(ActionAuditEvent::targetPropertyMaybe);

    assertThat(targetPropertyMaybe.isPresent()).isTrue();
    assertThat(targetPropertyMaybe.get()).isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::actionId).distinct().count()).isEqualTo(1);

    Optional<ActionAuditEvent> readerEventMaybe =
        eventsSent.stream()
            .filter(
                e ->
                    e.targetType() == TargetType.USER
                        && e.targetIdMaybe() != null
                        && e.targetIdMaybe().equals(ADDED_USER_ID))
            .findFirst();
    assertThat(readerEventMaybe.isPresent()).isTrue();
    assertThat(readerEventMaybe.get().targetPropertyMaybe()).isNotNull();
    assertThat(readerEventMaybe.get().targetPropertyMaybe())
        .isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());
    assertThat(readerEventMaybe.get().newValueMaybe())
        .isEqualTo(WorkspaceAccessLevel.READER.toString());
    assertThat(readerEventMaybe.get().previousValueMaybe()).isNull();
  }

  @Test
  public void testCollaborateWithEmptyMapDoesNothing() {
    workspaceAuditor.fireCollaborateAction(WORKSPACE_1_DB_ID, Collections.emptyMap());
    verifyNoInteractions(mockActionAuditService);
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
            .controlSet(!workspace1.getResearchPurpose().isControlSet());
    final int rpChanges = 5;
    User creator = new User();
    creator.setUserName("user10@fake-research-aou.org");

    Workspace editedWorkspace =
        clone(workspace1)
            // changes included above
            .researchPurpose(editedResearchPurpose)
            // changes
            .name("a new name")
            .namespace("a new namespace")
            .creatorUser(creator);
    final int wsChanges = 3;

    workspaceAuditor.fireEditAction(workspace1, editedWorkspace, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());

    Collection<ActionAuditEvent> events = eventCollectionCaptor.getValue();
    assertThat(events).hasSize(rpChanges + wsChanges);
  }

  private Workspace clone(Workspace in) {
    return new Workspace()
        .terraName(in.getTerraName())
        .etag(in.getEtag())
        .name(in.getName())
        .namespace(in.getNamespace())
        .cdrVersionId(in.getCdrVersionId())
        .creator(in.getCreator())
        .billingAccountName(in.getBillingAccountName())
        .googleBucketName(in.getGoogleBucketName())
        .accessTierShortName(in.getAccessTierShortName())
        .researchPurpose(in.getResearchPurpose())
        .billingStatus(in.getBillingStatus())
        .creationTime(in.getCreationTime())
        .lastModifiedTime(in.getLastModifiedTime());
  }

  private ResearchPurpose clone(ResearchPurpose in) {
    return new ResearchPurpose()
        .additionalNotes(in.getAdditionalNotes())
        .approved(in.isApproved())
        .ancestry(in.isAncestry())
        .anticipatedFindings(in.getAnticipatedFindings())
        .commercialPurpose(in.isCommercialPurpose())
        .controlSet(in.isControlSet())
        .diseaseFocusedResearch(in.isDiseaseFocusedResearch())
        .diseaseOfFocus(in.getDiseaseOfFocus())
        .drugDevelopment(in.isDrugDevelopment())
        .educational(in.isEducational())
        .intendedStudy(in.getIntendedStudy())
        .scientificApproach(in.getScientificApproach())
        .methodsDevelopment(in.isMethodsDevelopment())
        .otherPopulationDetails(in.getOtherPopulationDetails())
        .otherPurpose(in.isOtherPurpose())
        .otherPurposeDetails(in.getOtherPurposeDetails())
        .ethics(in.isEthics())
        .populationDetails(in.getPopulationDetails())
        .populationHealth(in.isPopulationHealth())
        .reasonForAllOfUs(in.getReasonForAllOfUs())
        .reviewRequested(in.isReviewRequested())
        .socialBehavioral(in.isSocialBehavioral())
        .timeRequested(in.getTimeRequested())
        .timeReviewed(in.getTimeReviewed())
        .disseminateResearchFindingList(in.getDisseminateResearchFindingList())
        .otherDisseminateResearchFindings(in.getOtherDisseminateResearchFindings())
        .researchOutcomeList(in.getResearchOutcomeList());
  }
}
