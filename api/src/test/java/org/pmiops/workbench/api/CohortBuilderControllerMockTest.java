package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CohortBuilderControllerMockTest {

    @Mock
    private CriteriaDao mockCriteriaDao;

    @Mock
    private CdrVersionDao mockCdrVersionDao;

    @InjectMocks
    private CohortBuilderController controller;

    @Test
    public void getCriteriaByTypeAndParentIdIcd9() throws Exception {
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

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByCodeAsc("ICD9", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId(1L,"ICD9", 0L),
                new Criteria()
                        .id(1L)
                        .type("ICD9")
                        .code("001-139.99")
                        .name("Infectious and parasitic diseases")
                        .group(false)
                        .selectable(false)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByCodeAsc("ICD9", 0L);
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    @Test
    public void getCriteriaByTypeAndParentIdDemo() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("DEMO")
                        .name("Age")
                        .group(false)
                        .selectable(true)
                        .count("0")
                        .conceptId("12")
                        .subtype("AGE");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByCodeAsc("DEMO", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId(1L,"DEMO", 0L),
                new Criteria()
                        .id(1L)
                        .type("DEMO")
                        .name("Age")
                        .group(false)
                        .selectable(true)
                        .count(0L)
                        .conceptId(12L)
                        .subtype("AGE"));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByCodeAsc("DEMO", 0L);
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    @Test
    public void getCriteriaByTypeAndParentIdIcd10() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("ICD10")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByCodeAsc("ICD10", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId(1L,"ICD10", 0L),
                new Criteria()
                        .id(1L)
                        .type("ICD10")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByCodeAsc("ICD10", 0L);
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    @Test
    public void getCriteriaByTypeAndParentIdCPT() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("CPT")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByCodeAsc("CPT", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId(1L,"CPT", 0L),
                new Criteria()
                        .id(1L)
                        .type("CPT")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByCodeAsc("CPT", 0L);
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    @Test
    public void getCriteriaByTypeAndParentIdPhecodes() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("PHECODE")
                        .name("Intestinal infection")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");
        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByCodeAsc("PHECODE", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId(1l,"PHECODE", 0L),
                new Criteria()
                        .id(1L)
                        .type("PHECODE")
                        .name("Intestinal infection")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByCodeAsc("PHECODE", 0L);
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    @Test
    public void getCriteriaByTypeAndSubtypeDemographics() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("DEMO")
                        .subtype("RACE")
                        .name("African American")
                        .group(false)
                        .selectable(true)
                        .count("100")
                        .conceptId("0");
        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndSubtypeOrderByNameAsc("DEMO", "RACE"))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndSubtype(1l,"DEMO", "RACE"),
                new Criteria()
                        .id(1L)
                        .type("DEMO")
                        .subtype("RACE")
                        .name("African American")
                        .group(false)
                        .selectable(true)
                        .count(100L)
                        .conceptId(0L));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndSubtypeOrderByNameAsc("DEMO", "RACE");
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    @Test
    public void getCriteriaTreeQuickSearch() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("PHECODE")
                        .name("Intestinal infection")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndNameOrCode("PHECODE", "infect*"))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaTreeQuickSearch(1L,"PHECODE", "infect"),
                new Criteria()
                        .id(1L)
                        .type("PHECODE")
                        .name("Intestinal infection")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndNameOrCode("PHECODE", "infect*");
        verifyNoMoreInteractions(mockCriteriaDao, mockCdrVersionDao);
    }

    private void assertCriteria(ResponseEntity response, Criteria expectedCriteria) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CriteriaListResponse listResponse = (CriteriaListResponse) response.getBody();
        assertThat(listResponse.getItems().get(0)).isEqualTo(expectedCriteria);
    }
}
