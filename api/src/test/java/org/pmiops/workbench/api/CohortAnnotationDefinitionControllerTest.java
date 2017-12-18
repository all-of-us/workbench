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
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortAnnotationDefinition;

import static org.junit.Assert.*;
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
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No workspace matching workspaceNamespace: "
                    + namespace + ", workspaceId: " + name, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortAnnotationDefinition() throws Exception {
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

        CohortAnnotationDefinition request = new CohortAnnotationDefinition();
        request.setAnnotationType(AnnotationType.STRING);
        request.setName("testing");

        org.pmiops.workbench.db.model.CohortAnnotationDefinition dbCohortAnnotationDefinition =
                new org.pmiops.workbench.db.model.CohortAnnotationDefinition();
        dbCohortAnnotationDefinition.setAnnotationType(request.getAnnotationType());
        dbCohortAnnotationDefinition.setColumnName(request.getName());
        dbCohortAnnotationDefinition.setCohortId(cohortId);
        dbCohortAnnotationDefinition.setCohortAnnotationDefinitionId(annotationDefinitionId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition)).thenReturn(dbCohortAnnotationDefinition);

        CohortAnnotationDefinition expectedResponse = new CohortAnnotationDefinition();
        expectedResponse.setAnnotationType(AnnotationType.STRING);
        expectedResponse.setName("testing");
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
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No workspace matching workspaceNamespace: "
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
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort Annotation Definition exists for annotationDefinitionId: "
                    + annotationDefinitionId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findOne(annotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateCohortAnnotationDefinition() throws Exception {
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
        request.setName("new-name");

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
                .name(request.getName())
                .cohortAnnotationDefinitionId(annotationDefinitionId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(definition);
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
        verify(cohortAnnotationDefinitionDao, times(1)).save(definition);

        verifyNoMoreMockInteractions();
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortDao, cohortAnnotationDefinitionDao, workspaceService);
    }

}
