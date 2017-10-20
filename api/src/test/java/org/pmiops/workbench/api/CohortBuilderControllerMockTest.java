package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class CohortBuilderControllerMockTest {

    @InjectMocks
    CohortBuilderController controller;

    @Mock
    CriteriaDao mockCriteriaDao;

    @Test
    public void getCriteriaByTypeAndParentId_Icd9() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("ICD9")
                        .code("001-139.99")
                        .name("Infectious and parasitic diseases")
                        .group(false)
                        .selectable(false)
                        .count("0")
                        .conceptId("0");

        when(mockCriteriaDao
                .findCriteriaByTypeLikeAndParentIdOrderBySortOrderAsc("ICD9", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("ICD9", 0L),
                new Criteria()
                        .id(1L)
                        .type("ICD9")
                        .code("001-139.99")
                        .name("Infectious and parasitic diseases")
                        .group(false)
                        .selectable(false)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCriteriaDao).findCriteriaByTypeLikeAndParentIdOrderBySortOrderAsc("ICD9", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    @Test
    public void getCriteriaByTypeAndParentId_demo() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("DEMO_AGE")
                        .name("Age")
                        .group(false)
                        .selectable(true)
                        .count("0")
                        .conceptId("12");

        when(mockCriteriaDao
                .findCriteriaByTypeLikeAndParentIdOrderBySortOrderAsc("DEMO", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("DEMO", 0L),
                new Criteria()
                        .id(1L)
                        .type("DEMO_AGE")
                        .name("Age")
                        .group(false)
                        .selectable(true)
                        .count(0L)
                        .conceptId(12L));

        verify(mockCriteriaDao).findCriteriaByTypeLikeAndParentIdOrderBySortOrderAsc("DEMO", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    private void assertCriteria(ResponseEntity response, Criteria expectedCriteria) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CriteriaListResponse listResponse = (CriteriaListResponse) response.getBody();
        assertThat(listResponse.getItems().get(0)).isEqualTo(expectedCriteria);
    }

}
