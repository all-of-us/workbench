package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.Operator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class CohortBuilderControllerMockTest {

    @Mock
    private CriteriaDao mockCriteriaDao;

    @Mock
    private CdrVersionDao mockCdrVersionDao;

    @SuppressWarnings("unused")
    @Mock
    private CdrVersionService mockCdrVersionService;

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
                        .conceptId("0")
                        .predefinedAttributes("[]");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByIdAsc("ICD9", 0L))
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
                        .conceptId(0L)
                        .hasAttributes(false));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByIdAsc("ICD9", 0L);
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
                        .subtype("AGE")
                        .predefinedAttributes("[]");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByIdAsc("DEMO", 0L))
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
                        .subtype("AGE")
                        .hasAttributes(false));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByIdAsc("DEMO", 0L);
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
                        .conceptId("0")
                        .predefinedAttributes("[]");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByIdAsc("ICD10", 0L))
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
                        .conceptId(0L)
                        .hasAttributes(false));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByIdAsc("ICD10", 0L);
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
                        .conceptId("0")
                        .predefinedAttributes("[]");

        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByIdAsc("CPT", 0L))
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
                        .conceptId(0L)
                        .hasAttributes(false));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByIdAsc("CPT", 0L);
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
                        .conceptId("0")
                        .predefinedAttributes("[{'name':'Systolic','operator':'LESS_THAN_OR_EQUAL_TO','operands':['90'],'conceptId':'903118'},{'name':'Diastolic','operator':'LESS_THAN_OR_EQUAL_TO','operands':['60'],'conceptId':'903115'}]");
        when(mockCdrVersionDao.findOne(1L)).thenReturn(new CdrVersion());
        when(mockCriteriaDao
                .findCriteriaByTypeAndParentIdOrderByIdAsc("PHECODE", 0L))
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
                        .conceptId(0L)
                        .hasAttributes(false)
                        .predefinedAttributes(
                          Arrays.asList(
                          new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
                          new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L)
                          )
                        ));

        verify(mockCdrVersionDao).findOne(1L);
        verify(mockCriteriaDao).findCriteriaByTypeAndParentIdOrderByIdAsc("PHECODE", 0L);
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
                        .conceptId("0")
                        .predefinedAttributes("[]");
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
                        .conceptId(0L)
                        .hasAttributes(false));

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
                        .conceptId("0")
                        .predefinedAttributes("[]");

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
                        .conceptId(0L)
                        .hasAttributes(false));

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
