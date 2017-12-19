package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.ModifyCohortAnnotationDefinitionRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CohortAnnotationDefinitionControllerTest {

    @Mock
    CohortDao cohortDao;

    @Mock
    CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

    @Mock
    WorkspaceService workspaceService;

    @InjectMocks
    CohortAnnotationDefinitionController cohortAnnotationDefinitionController;

    @Test
    public void createCohortAnnotationDefinition_BadCohortId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;

        CohortAnnotationDefinition request = new CohortAnnotationDefinition();

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(namespace, name, cohortId, request);
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortAnnotationDefinition_BadWorkspace() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(1);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(0);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        CohortAnnotationDefinition request = new CohortAnnotationDefinition();

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(namespace, name, cohortId, request);
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No workspace matching workspaceNamespace: "
                    + namespace + ", workspaceId: " + name, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortAnnotationDefinition_NameConflict() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;
        final String columnName = "testing";

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        CohortAnnotationDefinition request = new CohortAnnotationDefinition();
        request.setAnnotationType(AnnotationType.STRING);
        request.setColumnName(columnName);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition dbCohortAnnotationDefinition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                        .annotationType(request.getAnnotationType())
                        .columnName(request.getColumnName())
                        .cohortId(cohortId)
                        .cohortAnnotationDefinitionId(annotationDefinitionId);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition existingDefinition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                        .annotationType(request.getAnnotationType())
                        .columnName(request.getColumnName())
                        .cohortId(cohortId)
                        .cohortAnnotationDefinitionId(annotationDefinitionId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(existingDefinition);

        CohortAnnotationDefinition expectedResponse = new CohortAnnotationDefinition();
        expectedResponse.setAnnotationType(AnnotationType.STRING);
        expectedResponse.setColumnName(columnName);
        expectedResponse.setCohortId(cohortId);
        expectedResponse.setCohortAnnotationDefinitionId(annotationDefinitionId);

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    request);
            fail("Should have thrown a ConflictException!");
        } catch (ConflictException e) {
            assertEquals("Conflict: Cohort Annotation Definition name exists for: " + columnName, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findByCohortIdAndColumnName(cohortId, columnName);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortAnnotationDefinition() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;
        final String columnName = "testing";

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        CohortAnnotationDefinition request = new CohortAnnotationDefinition();
        request.setAnnotationType(AnnotationType.STRING);
        request.setColumnName(columnName);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition dbCohortAnnotationDefinition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition();
        dbCohortAnnotationDefinition.setAnnotationType(request.getAnnotationType());
        dbCohortAnnotationDefinition.setColumnName(request.getColumnName());
        dbCohortAnnotationDefinition.setCohortId(cohortId);
        dbCohortAnnotationDefinition.setCohortAnnotationDefinitionId(annotationDefinitionId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition)).thenReturn(dbCohortAnnotationDefinition);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(null);

        CohortAnnotationDefinition expectedResponse = new CohortAnnotationDefinition();
        expectedResponse.setAnnotationType(AnnotationType.STRING);
        expectedResponse.setColumnName(columnName);
        expectedResponse.setCohortId(cohortId);
        expectedResponse.setCohortAnnotationDefinitionId(annotationDefinitionId);

        CohortAnnotationDefinition response =
                cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
                        namespace,
                        name,
                        cohortId,
                        request).getBody();
        assertEquals(expectedResponse, response);


        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).save(dbCohortAnnotationDefinition);
        verify(cohortAnnotationDefinitionDao, times(1)).findByCohortIdAndColumnName(cohortId, columnName);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateCohortAnnotationDefinition_BadCohortId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long annotationDefinitionId = 1;

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId,
                    request);
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateCohortAnnotationDefinition_BadWorkspace() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long annotationDefinitionId = 1;

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(1);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(0);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId,
                    request);
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No workspace matching workspaceNamespace: "
                    + namespace + ", workspaceId: " + name, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateCohortAnnotationDefinition_BadAnnotationDefinitionId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId,
                    request);
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: "
                    + annotationDefinitionId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findOne(annotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateCohortAnnotationDefinition_NameConflict() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;
        final String columnName = "new-name";

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();
        request.setColumnName(columnName);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition definition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                        .cohortAnnotationDefinitionId(annotationDefinitionId)
                        .annotationType(AnnotationType.STRING)
                        .cohortId(cohortId)
                        .columnName("name1");

        org.pmiops.workbench.db.model.CohortAnnotationDefinition existingDefinition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                        .cohortAnnotationDefinitionId(annotationDefinitionId)
                        .annotationType(AnnotationType.STRING)
                        .cohortId(cohortId)
                        .columnName("name1");

        CohortAnnotationDefinition expectedResponse =
                new CohortAnnotationDefinition()
                        .annotationType(AnnotationType.STRING)
                        .cohortId(cohortId)
                        .columnName(request.getColumnName())
                        .cohortAnnotationDefinitionId(annotationDefinitionId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(definition);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(existingDefinition);

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId,
                    request);
            fail("Should have thrown a ConflictException!");
        } catch (ConflictException e) {
            assertEquals("Conflict: Cohort Annotation Definition name exists for: "
                    + columnName, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findOne(annotationDefinitionId);
        verify(cohortAnnotationDefinitionDao, times(1)).findByCohortIdAndColumnName(cohortId, columnName);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateCohortAnnotationDefinition() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;
        final String columnName = "new-name";

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();
        request.setColumnName(columnName);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition definition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(annotationDefinitionId)
                .annotationType(AnnotationType.STRING)
                .cohortId(cohortId)
                .columnName("name1");

        CohortAnnotationDefinition expectedResponse =
                new CohortAnnotationDefinition()
                .annotationType(AnnotationType.STRING)
                .cohortId(cohortId)
                .columnName(request.getColumnName())
                .cohortAnnotationDefinitionId(annotationDefinitionId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(definition);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(null);
        when(cohortAnnotationDefinitionDao.save(definition)).thenReturn(definition);

        CohortAnnotationDefinition responseDefinition =
        cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId,
                    request).getBody();

        assertEquals(expectedResponse, responseDefinition);

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findOne(annotationDefinitionId);
        verify(cohortAnnotationDefinitionDao, times(1)).findByCohortIdAndColumnName(cohortId, columnName);
        verify(cohortAnnotationDefinitionDao, times(1)).save(definition);

        verifyNoMoreMockInteractions();
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortDao, cohortAnnotationDefinitionDao, workspaceService);
    }

}
