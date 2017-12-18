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
import org.pmiops.workbench.model.CohortAnnotationDefinition;

import static org.junit.Assert.*;
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
        long cdrVersionId = 1;

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
            assertEquals("Invalid Request: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);

        verifyNoMoreMockInteractions();
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortDao, cohortAnnotationDefinitionDao, workspaceService);
    }

}
