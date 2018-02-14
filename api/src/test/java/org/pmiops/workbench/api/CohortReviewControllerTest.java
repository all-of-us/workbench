package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantCohortStatusesRequest;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.http.ResponseEntity;

import javax.inject.Provider;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CohortReviewControllerTest {

    private String namespace;
    private String name;
    private long cohortId;
    private long cdrVersionId;
    private long workspaceId;
    private long cohortReviewId;
    private long participantId;

    @Mock
    CohortReviewService cohortReviewService;

    @Mock
    BigQueryService bigQueryService;

    @Mock
    ParticipantCounter participantCounter;

    @Mock
    CodeDomainLookupService codeDomainLookupService;

    @Mock
    WorkspaceService workspaceService;

    @Mock
    Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

    @InjectMocks
    CohortReviewController reviewController;

    @Before
    public void onSetUp() {
        namespace = "aou-test";
        name = "test";
        cohortId = 1;
        cdrVersionId = 1;
        workspaceId = 1;
        cohortReviewId = 1;
        participantId = 1;
    }

    @Test
    public void createCohortReviewReviewAlreadyCreated() throws Exception {

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(1, cohortId, cohortReviewId, cdrVersionId, null));

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review already created for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewService, times(1)).findCohortReview(cohortId, cdrVersionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortReviewMoreThanTenThousand() throws Exception {

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(20000));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review size must be between 0 and 10000", e.getMessage());
        }

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortReviewNoCohortDefinitionFound() throws Exception {

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
            WorkspaceAccessLevel.WRITER)).thenReturn(createWorkspace(workspaceId, namespace, name));

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort definition matching cohortId: "
                         + cohortId,
                         e.getMessage());
        }

        verify(cohortReviewService, times(1)).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, times(1)).findCohort(cohortId);
        verify(cohortReviewService, times(1)).validateMatchingWorkspace(namespace, name, workspaceId, WorkspaceAccessLevel.WRITER);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortReview() throws Exception {

        String definition = "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":" +
                "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":" +
                "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";

        SearchRequest searchRequest = new Gson().fromJson(definition, SearchRequest.class);

        QueryResult queryResult = mock(QueryResult.class);
        Iterable testIterable = new Iterable() {
            @Override
            public Iterator iterator() {
                List<FieldValue> list = new ArrayList<>();
                list.add(null);
                return list.iterator();
            }
        };
        Map<String, Integer> rm = new HashMap<>();
        rm.put("person_id", 0);
        rm.put("birth_datetime", 1);
        rm.put("gender_concept_id", 2);
        rm.put("race_concept_id", 3);
        rm.put("ethnicity_concept_id", 4);

        when(workspaceService.enforceWorkspaceAccessLevel(namespace, name, WorkspaceAccessLevel.READER)).thenReturn(WorkspaceAccessLevel.OWNER);
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, definition));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
            WorkspaceAccessLevel.WRITER)).thenReturn(createWorkspace(workspaceId, namespace, name));
        doNothing().when(codeDomainLookupService).findCodesForEmptyDomains(searchRequest.getIncludes());
        doNothing().when(codeDomainLookupService).findCodesForEmptyDomains(searchRequest.getExcludes());
        when(participantCounter.buildParticipantIdQuery(searchRequest, 200, 0L)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(0L);
        when(bigQueryService.getString(null, 1)).thenReturn("1");
        when(bigQueryService.getLong(null, 2)).thenReturn(0L);
        when(bigQueryService.getLong(null, 3)).thenReturn(0L);
        when(bigQueryService.getLong(null, 4)).thenReturn(0L);
        when(genderRaceEthnicityConceptProvider.get()).thenReturn(new GenderRaceEthnicityConcept(createGenderRaceEthnicityConcept()));
        doNothing().when(cohortReviewService).saveFullCohortReview(createCohortReview(1, cohortId, cohortReviewId, cdrVersionId, ReviewStatus.CREATED),
                Arrays.asList(createParticipantCohortStatus(cohortReviewId, 0, CohortStatus.NOT_REVIEWED)));
        when(cohortReviewService.findAll(isA(Long.class), isA(List.class), isA(PageRequest.class))).thenReturn(Arrays.asList(createParticipantCohortStatus(cohortReviewId, 0, CohortStatus.INCLUDED)));

        reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));

        verify(cohortReviewService, times(1)).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, times(1)).findCohort(cohortId);
        verify(cohortReviewService, times(1)).validateMatchingWorkspace(namespace, name, workspaceId, WorkspaceAccessLevel.WRITER);
        verify(codeDomainLookupService, times(1)).findCodesForEmptyDomains(searchRequest.getIncludes());
        verify(codeDomainLookupService, times(1)).findCodesForEmptyDomains(searchRequest.getExcludes());
        verify(participantCounter, times(1)).buildParticipantIdQuery(searchRequest, 200, 0L);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(bigQueryService, times(1)).getString(null, 1);
        verify(bigQueryService, times(1)).getLong(null, 2);
        verify(bigQueryService, times(1)).getLong(null, 3);
        verify(bigQueryService, times(1)).getLong(null, 4);
        verify(queryResult, times(1)).iterateAll();
        verify(cohortReviewService, times(1)).saveFullCohortReview(isA(CohortReview.class), isA(List.class));
        verify(genderRaceEthnicityConceptProvider, times(1)).get();
        verify(cohortReviewService).findAll(isA(Long.class), isA(List.class), isA(PageRequest.class));
        verifyNoMoreMockInteractions();
    }

    @Test
    public void getParticipants() throws Exception {
        int page = 1;
        int pageSize = 22;

        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, pageSize, SortOrder.DESC, ParticipantCohortStatusColumns.STATUS);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, pageSize, SortOrder.DESC,ParticipantCohortStatusColumns.PARTICIPANTID);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null,null,null, ParticipantCohortStatusColumns.STATUS);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null,null, SortOrder.DESC,null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, pageSize,null,null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page,null,null,null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null,null,null);
    }

    @Test
    public void getParticipantDemographics() throws Exception {

        Map<String, Map<Long, String>> concepts = createGenderRaceEthnicityConcept();

        List<ConceptIdName> raceList = concepts.get(GenderRaceEthnicityType.RACE.name()).entrySet().stream()
                .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
                .collect(Collectors.toList());
        List<ConceptIdName> genderList = concepts.get(GenderRaceEthnicityType.GENDER.name()).entrySet().stream()
                .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
                .collect(Collectors.toList());
        List<ConceptIdName> ethnicityList = concepts.get(GenderRaceEthnicityType.ETHNICITY.name()).entrySet().stream()
                .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
                .collect(Collectors.toList());
        ParticipantDemographics expected = new ParticipantDemographics().raceList(raceList).genderList(genderList).ethnicityList(ethnicityList);

        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.READER)).thenReturn(new Workspace());
        when(genderRaceEthnicityConceptProvider.get()).thenReturn(new GenderRaceEthnicityConcept(concepts));

        ParticipantDemographics response = reviewController.getParticipantDemographics(namespace, name, cohortId, cdrVersionId).getBody();
        assertEquals(expected, response);

        verify(cohortReviewService).findCohort(cohortId);
        verify(cohortReviewService).validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.READER);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void createParticipantCohortAnnotation() throws Exception {
        assertCreateParticipantCohortAnnotation(createParticipantCohortAnnotation(Boolean.TRUE, null, null, null, null), AnnotationType.BOOLEAN);
        assertCreateParticipantCohortAnnotation(createParticipantCohortAnnotation(null, "test", null, null, null), AnnotationType.STRING);
        assertCreateParticipantCohortAnnotation(createParticipantCohortAnnotation(null, null, 1, null, null), AnnotationType.INTEGER);
        assertCreateParticipantCohortAnnotation(createParticipantCohortAnnotation(null, null, null, "1999-02-01", null), AnnotationType.DATE);
        assertCreateParticipantCohortAnnotation(createParticipantCohortAnnotation(null, null, null, null, "stest"), AnnotationType.ENUM);
    }

    @Test
    public void createParticipantCohortAnnotationInvalidCohortAnnotationDefinitionId() throws Exception {
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, new ParticipantCohortAnnotation());
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals("Invalid Request: Please provide a valid cohort annotation definition id.", e.getMessage());
        }
    }

    @Test
    public void createParticipantCohortAnnotationNotFoundCohortAnnotationDefinition() throws Exception {
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        when(cohortReviewService.findCohortAnnotationDefinition(1L)).thenReturn(null);
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, new ParticipantCohortAnnotation().cohortAnnotationDefinitionId(1L));
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals("Invalid Request: No cohort annotation definition found for id: " + 1, e.getMessage());
        }
    }

    @Test
    public void createParticipantCohortAnnotationNotFoundParticipantCohortStatus() throws Exception {
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(null);
        when(cohortReviewService.findCohortAnnotationDefinition(1L)).thenReturn(new CohortAnnotationDefinition());
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, new ParticipantCohortAnnotation().cohortAnnotationDefinitionId(1L));
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals("Invalid Request: No participant found for id: " + 1, e.getMessage());
        }
    }

    @Test
    public void assertParticipantCohortAnnotaitonParticipantAnnotationExists() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = new CohortAnnotationDefinition()
                .annotationType(AnnotationType.STRING);

        ParticipantCohortAnnotation request = new ParticipantCohortAnnotation().cohortAnnotationDefinitionId(1L).annotationValueString("12");
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        when(cohortReviewService.findCohortAnnotationDefinition(request.getCohortAnnotationDefinitionId())).thenReturn(cohortAnnotationDefinition);
        when(cohortReviewService.findParticipantCohortAnnotation(cohortReviewId, request.getCohortAnnotationDefinitionId(), participantId))
                .thenReturn(new org.pmiops.workbench.db.model.ParticipantCohortAnnotation());
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, request);
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals("Invalid Request: Cohort annotation definition exists for id: " +
                    request.getCohortAnnotationDefinitionId(), e.getMessage());
        }
    }

    @Test
    public void assertParticipantCohortAnnotaitonInvalidDateString() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = new CohortAnnotationDefinition()
                .annotationType(AnnotationType.DATE);

        ParticipantCohortAnnotation request = new ParticipantCohortAnnotation().cohortAnnotationDefinitionId(1L).annotationValueDate("12");
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        when(cohortReviewService.findCohortAnnotationDefinition(request.getCohortAnnotationDefinitionId())).thenReturn(cohortAnnotationDefinition);
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, request);
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals("Invalid Request: Please provide a valid DATE value (yyyy-MM-dd) for annotation defintion id: " +
                    request.getCohortAnnotationDefinitionId(), e.getMessage());
        }
    }

    @Test
    public void assertParticipantCohortAnnotaitonInvalidEnumValue() throws Exception {
        CohortAnnotationEnumValue cohortAnnotationEnumValue = new CohortAnnotationEnumValue().name("11");
        CohortAnnotationDefinition cohortAnnotationDefinition = new CohortAnnotationDefinition()
                .annotationType(AnnotationType.ENUM).enumValues(new TreeSet(Arrays.asList(cohortAnnotationEnumValue)));

        ParticipantCohortAnnotation request = new ParticipantCohortAnnotation().cohortAnnotationDefinitionId(1L).annotationValueEnum("12");
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        when(cohortReviewService.findCohortAnnotationDefinition(request.getCohortAnnotationDefinitionId())).thenReturn(cohortAnnotationDefinition);
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, request);
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals("Invalid Request: Please provide a valid ENUM value for annotation defintion id: " +
                    request.getCohortAnnotationDefinitionId(), e.getMessage());
        }
    }

    @Test
    public void createParticipantCohortAnnotationAll() throws Exception {
        assertParticipantCohortAnnotaitonValidateAnnotationTypes(AnnotationType.BOOLEAN);
        assertParticipantCohortAnnotaitonValidateAnnotationTypes(AnnotationType.STRING);
        assertParticipantCohortAnnotaitonValidateAnnotationTypes(AnnotationType.DATE);
        assertParticipantCohortAnnotaitonValidateAnnotationTypes(AnnotationType.INTEGER);
        assertParticipantCohortAnnotaitonValidateAnnotationTypes(AnnotationType.ENUM);
    }

    private void assertParticipantCohortAnnotaitonValidateAnnotationTypes(AnnotationType annotationType) {
        CohortAnnotationDefinition cohortAnnotationDefinition = new CohortAnnotationDefinition()
                .annotationType(annotationType);

        ParticipantCohortAnnotation request = new ParticipantCohortAnnotation().cohortAnnotationDefinitionId(1L);
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        when(cohortReviewService.findCohortAnnotationDefinition(request.getCohortAnnotationDefinitionId())).thenReturn(cohortAnnotationDefinition);
        try {
            reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, request);
            fail("Should have thrown a BadRequestException!");
        } catch (Exception e) {
            assertEquals(String.format("Invalid Request: Please provide a valid %s value for annotation defintion id: %s",
                    annotationType.name(), request.getCohortAnnotationDefinitionId()), e.getMessage());
        }
    }

    private ParticipantCohortAnnotation createParticipantCohortAnnotation(Boolean booleanValue, String stringValue, Integer integerValue, String dateValue, String enumValue) {
        return new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(1L)
                .annotationId(1L)
                .participantId(participantId)
                .cohortReviewId(cohortReviewId)
                .annotationValueBoolean(booleanValue)
                .annotationValueString(stringValue)
                .annotationValueInteger(integerValue)
                .annotationValueDate(dateValue)
                .annotationValueEnum(enumValue);
    }

    private void assertCreateParticipantCohortAnnotation(ParticipantCohortAnnotation request, AnnotationType annotationType) throws Exception {

        CohortAnnotationDefinition cohortAnnotationDefinition = new CohortAnnotationDefinition().annotationType(annotationType);
        if (request.getAnnotationValueEnum() != null) {
            CohortAnnotationEnumValue cohortAnnotationEnumValue = new CohortAnnotationEnumValue().name(request.getAnnotationValueEnum());
            cohortAnnotationDefinition.setEnumValues(new TreeSet(Arrays.asList(cohortAnnotationEnumValue)));
        }
        when(cohortReviewService.findCohort(cohortId)).thenReturn(createCohort(cohortId, workspaceId, null));
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(createCohortReview(0, cohortId, cohortReviewId, cdrVersionId, null));
        when(cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)).thenReturn(new ParticipantCohortStatus());
        when(cohortReviewService.findCohortAnnotationDefinition(request.getCohortAnnotationDefinitionId())).thenReturn(cohortAnnotationDefinition);
        when(cohortReviewService.findParticipantCohortAnnotation(cohortReviewId, request.getCohortAnnotationDefinitionId(), participantId)).thenReturn(null);
        when(cohortReviewService.saveParticipantCohortAnnotation(isA(org.pmiops.workbench.db.model.ParticipantCohortAnnotation.class)))
                .thenReturn(isA(org.pmiops.workbench.db.model.ParticipantCohortAnnotation.class));

        ParticipantCohortAnnotation response =
                reviewController.createParticipantCohortAnnotation(namespace, name, cohortId, cdrVersionId, participantId, request).getBody();

        assertEquals(request, response);

        verify(cohortReviewService, atLeastOnce()).findCohort(cohortId);
        verify(cohortReviewService, atLeastOnce()).validateMatchingWorkspace(namespace, name, workspaceId,
                WorkspaceAccessLevel.WRITER);
        verify(cohortReviewService, atLeastOnce()).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, atLeastOnce()).findParticipantCohortStatus(cohortReviewId, participantId);
        verify(cohortReviewService, atLeastOnce()).findCohortAnnotationDefinition(request.getCohortAnnotationDefinitionId());
        verify(cohortReviewService, atLeastOnce()).findParticipantCohortAnnotation(cohortReviewId, request.getCohortAnnotationDefinitionId(), participantId);
        verify(cohortReviewService, atLeastOnce()).saveParticipantCohortAnnotation(isA(org.pmiops.workbench.db.model.ParticipantCohortAnnotation.class));
        verifyNoMoreMockInteractions();
    }

    private Map<String, Map<Long, String>> createGenderRaceEthnicityConcept() {
        Map<Long, String> raceMap = new HashMap<>();
        raceMap.put(1L, "White");
        raceMap.put(2L, "African American");

        Map<Long, String> genderMap = new HashMap<>();
        genderMap.put(3L, "MALE");
        genderMap.put(4L, "FEMALE");

        Map<Long, String> ethnicityMap = new HashMap<>();
        ethnicityMap.put(5L, "Hispanic or Latino");
        ethnicityMap.put(6L, "Not Hispanic or Latino");

        Map<String, Map<Long, String>> concepts = new HashMap<>();
        concepts.put(GenderRaceEthnicityType.RACE.name(), raceMap);
        concepts.put(GenderRaceEthnicityType.GENDER.name(), genderMap);
        concepts.put(GenderRaceEthnicityType.ETHNICITY.name(), ethnicityMap);
        return concepts;
    }

    private ParticipantCohortStatus createParticipantCohortStatus(long cohortReviewId, long participantId, CohortStatus cohortStatus) {
        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey().cohortReviewId(cohortReviewId).participantId(participantId);
        final Date dob = new Date(System.currentTimeMillis());

        return new ParticipantCohortStatus()
                .participantKey(key)
                .status(cohortStatus)
                .birthDate(dob)
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L);
    }

    private CohortReview createCohortReview(long reviewSize, long cohortId, long cohortReviewId, long cdrVersionId, ReviewStatus reviewStatus) {
        return new CohortReview()
                .cohortId(cohortId)
                .reviewSize(reviewSize)
                .cohortReviewId(cohortReviewId)
                .cdrVersionId(cdrVersionId)
                .matchedParticipantCount(1000)
                .creationTime(new Timestamp(System.currentTimeMillis()))
                .reviewStatus(reviewStatus);
    }

    private Cohort createCohort(long cohortId, long workspaceId, String definition) {
        Cohort cohort = new Cohort();
        cohort.setCohortId(cohortId);
        cohort.setWorkspaceId(workspaceId);
        cohort.setCriteria(definition);
        return cohort;
    }

    private Workspace createWorkspace(long workspaceId, String namespace, String name) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);
        return workspace;
    }

    private void assertFindByCohortIdAndCdrVersionId(String namespace,
                                                     String name,
                                                     long cohortId,
                                                     long cdrVersionId,
                                                     Integer page,
                                                     Integer pageSize,
                                                     SortOrder sortOrder,
                                                     ParticipantCohortStatusColumns sortColumn) {
        Integer pageParam = page == null ? 0 : page;
        Integer pageSizeParam = pageSize == null ? 25 : pageSize;
        sortColumn = (sortColumn == null || sortColumn.name().equals(sortColumn.PARTICIPANTID)) ? ParticipantCohortStatusColumns.PARTICIPANTID : sortColumn;
        sortOrder = sortOrder == null ? SortOrder.ASC : sortOrder;

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey().cohortReviewId(cohortId).participantId(participantId);
        final Date dob = new Date(System.currentTimeMillis());
        ParticipantCohortStatus dbParticipant = new ParticipantCohortStatus()
                .participantKey(key)
                .status(CohortStatus.INCLUDED)
                .birthDate(dob)
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L);

        org.pmiops.workbench.model.ParticipantCohortStatus respParticipant =
                new org.pmiops.workbench.model.ParticipantCohortStatus()
                        .participantId(1L)
                        .status(CohortStatus.INCLUDED)
                        .birthDate(dob.getTime())
                        .ethnicityConceptId(1L)
                        .genderConceptId(1L)
                        .raceConceptId(1L);

        org.pmiops.workbench.model.CohortReview respCohortReview =
                new org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(1L)
                        .cohortId(cohortId)
                        .cdrVersionId(cdrVersionId)
                        .matchedParticipantCount(1000L)
                        .reviewedCount(0L)
                        .reviewSize(200L)
                        .page(pageParam)
                        .pageSize(pageSizeParam)
                        .sortOrder(sortOrder.toString())
                        .sortColumn(sortColumn.toString())
                .participantCohortStatuses(Arrays.asList(respParticipant));

        List<ParticipantCohortStatus> participants = new ArrayList<ParticipantCohortStatus>();
        participants.add(dbParticipant);

        CohortReview cohortReviewAfter = new CohortReview();
        cohortReviewAfter.setCohortReviewId(1L);
        cohortReviewAfter.setCohortId(cohortId);
        cohortReviewAfter.setCdrVersionId(cdrVersionId);
        cohortReviewAfter.setMatchedParticipantCount(1000);
        cohortReviewAfter.setReviewSize(200);
        cohortReviewAfter.setCreationTime(new Timestamp(System.currentTimeMillis()));

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(1);

        Map<String, Map<Long, String>> concepts = new HashMap<>();
        concepts.put(GenderRaceEthnicityType.RACE.name(), new HashMap<>());
        concepts.put(GenderRaceEthnicityType.GENDER.name(), new HashMap<>());
        concepts.put(GenderRaceEthnicityType.ETHNICITY.name(), new HashMap<>());
        GenderRaceEthnicityConcept greConcept = new GenderRaceEthnicityConcept(concepts);

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(cohortReviewAfter);
        when(cohortReviewService.findAll(key.getCohortReviewId(),
                Collections.<Filter>emptyList(),
                new PageRequest(pageParam, pageSizeParam, sortOrder, sortColumn))).thenReturn(participants);
        when(cohortReviewService.validateMatchingWorkspace(namespace, name, workspaceId,
            WorkspaceAccessLevel.READER)).thenReturn(new Workspace());
        when(cohortReviewService.findCohort(cohortId)).thenReturn(cohort);

        ParticipantCohortStatusesRequest request = new ParticipantCohortStatusesRequest()
                .page(page)
                .pageSize(pageSize)
                .sortColumn(sortColumn)
                .sortOrder(sortOrder);
        ResponseEntity<org.pmiops.workbench.model.CohortReview> response =
                reviewController.getParticipantCohortStatuses(
                        namespace, name, cohortId, cdrVersionId, request);

        org.pmiops.workbench.model.CohortReview actualCohortReview = response.getBody();
        respCohortReview.setCreationTime(actualCohortReview.getCreationTime());
        assertEquals(respCohortReview, response.getBody());
        verify(cohortReviewService, atLeast(1)).validateMatchingWorkspace(namespace, name, workspaceId, WorkspaceAccessLevel.READER);
        verify(cohortReviewService, atLeast(1)).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, times(1))
                .findAll(key.getCohortReviewId(),
                        Collections.<Filter>emptyList(),
                        new PageRequest(pageParam, pageSizeParam, sortOrder, sortColumn));
        verify(cohortReviewService, atLeast(1)).findCohort(cohortId);
        verifyNoMoreMockInteractions();
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortReviewService, bigQueryService, participantCounter, codeDomainLookupService, workspaceService);
    }

}
