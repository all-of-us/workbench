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
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ModifyCohortAnnotationDefinitionRequest;

import java.util.ArrayList;

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

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(namespace,
                    name,
                    cohortId,
                    new CohortAnnotationDefinition());
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortAnnotationDefinition_BadWorkspace() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long badWorkspaceId = 0;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, badWorkspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(namespace,
                    name,
                    cohortId,
                    new CohortAnnotationDefinition());
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

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        CohortAnnotationDefinition request = createClientCohortAnnotationDefinition(
                annotationDefinitionId,
                cohortId,
                columnName,
                AnnotationType.STRING);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition existingDefinition =
                createDBCohortAnnotationDefinition(
                        cohortId,
                        annotationDefinitionId,
                        request.getAnnotationType(),
                        request.getColumnName());

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(existingDefinition);

        CohortAnnotationDefinition expectedResponse = createClientCohortAnnotationDefinition(
                annotationDefinitionId,
                cohortId,
                columnName,
                AnnotationType.STRING);

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

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        CohortAnnotationDefinition request = createClientCohortAnnotationDefinition(
                annotationDefinitionId,
                cohortId,
                columnName,
                AnnotationType.STRING);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition dbCohortAnnotationDefinition =
                createDBCohortAnnotationDefinition(
                        cohortId,
                        annotationDefinitionId,
                        request.getAnnotationType(),
                        request.getColumnName());

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition)).thenReturn(dbCohortAnnotationDefinition);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(null);

        CohortAnnotationDefinition expectedResponse = createClientCohortAnnotationDefinition(
                annotationDefinitionId,
                cohortId,
                columnName,
                AnnotationType.STRING);

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
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
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
        long workspaceId = 1;
        long badWorkspaceId = 0;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, badWorkspaceId);

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

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

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

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();
        request.setColumnName(columnName);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition definition =
                createDBCohortAnnotationDefinition(
                        cohortId,
                        annotationDefinitionId,
                        AnnotationType.STRING,
                        "name1");

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(definition);
        when(cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)).thenReturn(definition);

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

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        ModifyCohortAnnotationDefinitionRequest request = new ModifyCohortAnnotationDefinitionRequest();
        request.setColumnName(columnName);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition definition =
                createDBCohortAnnotationDefinition(
                        cohortId,
                        annotationDefinitionId,
                        AnnotationType.STRING,
                        "name1");

        CohortAnnotationDefinition expectedResponse = createClientCohortAnnotationDefinition(
                annotationDefinitionId,
                cohortId,
                request.getColumnName(),
                AnnotationType.STRING);

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

    @Test
    public void deleteCohortAnnotationDefinition_BadCohortId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long annotationDefinitionId = 1;

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId);
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void deleteCohortAnnotationDefinition_BadWorkspace() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long annotationDefinitionId = 1;
        long workspaceId = 1;
        long badWorkspaceId = 0;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, badWorkspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId);
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
    public void deleteCohortAnnotationDefinition_BadAnnotationDefinitionId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId);
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
    public void deleteCohortAnnotationDefinition() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;
        String columnName = "name";

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition cohortAnnotationDefinition =
                createDBCohortAnnotationDefinition(
                        cohortId,
                        annotationDefinitionId,
                        AnnotationType.STRING,
                        columnName);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(cohortAnnotationDefinition);
        doNothing().when(cohortAnnotationDefinitionDao).delete(annotationDefinitionId);

        EmptyResponse response = cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId).getBody();

        assertEquals(new EmptyResponse(), response);

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findOne(annotationDefinitionId);
        verify(cohortAnnotationDefinitionDao, times(1)).delete(annotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void getCohortAnnotationDefinition_NotFoundCohort() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long annotationDefinitionId = 1;

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId);
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void getCohortAnnotationDefinition_NotFoundWorkspace() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long annotationDefinitionId = 1;
        long workspaceId = 1;
        long badWorkspaceId = 0;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, badWorkspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId);
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
    public void getCohortAnnotationDefinition_NotFoundAnnotationDefinition() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                    namespace,
                    name,
                    cohortId,
                    annotationDefinitionId);
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
    public void getCohortAnnotationDefinition() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long annotationDefinitionId = 1;
        String columnName = "name";

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition cohortAnnotationDefinition =
                createDBCohortAnnotationDefinition(
                        cohortId,
                        annotationDefinitionId,
                        AnnotationType.STRING,
                        columnName);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findOne(annotationDefinitionId)).thenReturn(cohortAnnotationDefinition);

        CohortAnnotationDefinition responseDefinition =
                cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                        namespace,
                        name,
                        cohortId,
                        annotationDefinitionId).getBody();

        CohortAnnotationDefinition expectedResponse = createClientCohortAnnotationDefinition(
                annotationDefinitionId,
                cohortId,
                columnName,
                AnnotationType.STRING);
        assertEquals(expectedResponse, responseDefinition);

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findOne(annotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void getCohortAnnotationDefinitions_NotFoundCohort() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(
                    namespace,
                    name,
                    cohortId);
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void getCohortAnnotationDefinitions_NotFoundWorkspace() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;
        long badWorkspaceId = 0;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, badWorkspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(
                    namespace,
                    name,
                    cohortId);
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
    public void getCohortAnnotationDefinitions() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long workspaceId = 1;

        Cohort cohort = createCohort(workspaceId);

        Workspace workspace = createWorkspace(namespace, name, workspaceId);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(cohortAnnotationDefinitionDao.findByCohortId(cohortId)).thenReturn(new ArrayList<>());

        cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(
                namespace,
                name,
                cohortId);

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(cohortAnnotationDefinitionDao, times(1)).findByCohortId(cohortId);

        verifyNoMoreMockInteractions();
    }

    private CohortAnnotationDefinition createClientCohortAnnotationDefinition(long annotationDefinitionId,
                                                                              long cohortId,
                                                                              String columnName,
                                                                              AnnotationType annotationType) {
        CohortAnnotationDefinition request = new CohortAnnotationDefinition();
        request.setCohortAnnotationDefinitionId(annotationDefinitionId);
        request.setCohortId(cohortId);
        request.setColumnName(columnName);
        request.setAnnotationType(AnnotationType.STRING);
        return request;
    }

    private org.pmiops.workbench.db.model.CohortAnnotationDefinition createDBCohortAnnotationDefinition(long cohortId,
                                                                                                      long annotationDefinitionId,
                                                                                                      AnnotationType annotationType,
                                                                                                      String columnName) {
        return new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                .cohortId(cohortId)
                .cohortAnnotationDefinitionId(annotationDefinitionId)
                .annotationType(annotationType)
                .columnName(columnName);
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortDao, cohortAnnotationDefinitionDao, workspaceService);
    }

    private Cohort createCohort(long workspaceId) {
        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);
        return cohort;
    }

    private Workspace createWorkspace(String namespace, String name, long badWorkspaceId) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(badWorkspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);
        return workspace;
    }

}
