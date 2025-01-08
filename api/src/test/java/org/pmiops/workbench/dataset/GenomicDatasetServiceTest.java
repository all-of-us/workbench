package org.pmiops.workbench.dataset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.gson.Gson;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.test.CohortDefinitions;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import({
  CohortMapperImpl.class,
  CohortService.class,
  CommonMappers.class,
  FakeClockConfiguration.class,
  GenomicDatasetServiceImpl.class,
})
public class GenomicDatasetServiceTest {

  private static final QueryJobConfiguration QUERY_JOB_CONFIGURATION_1 =
      QueryJobConfiguration.newBuilder(
              "SELECT person_id FROM `${projectId}.${dataSetId}.person` person")
          .addNamedParameter(
              "foo",
              QueryParameterValue.newBuilder()
                  .setType(StandardSQLTypeName.INT64)
                  .setValue(Long.toString(101L))
                  .build())
          .build();

  @Autowired private GenomicDatasetServiceImpl genomicDatasetService;

  @Autowired private CohortDao cohortDao;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;

  @MockBean private CohortQueryBuilder mockCohortQueryBuilder;
  @MockBean private BigQueryService mockBigQueryService;

  private DbCohort cohort;
  private DbWorkspace workspace;

  @BeforeEach
  public void setUp() {
    workspace = workspaceDao.save(new DbWorkspace());
    cohort = cohortDao.save(buildSimpleCohort(workspace));
    when(mockCohortQueryBuilder.buildParticipantIdQuery(any()))
        .thenReturn(QUERY_JOB_CONFIGURATION_1);
  }

  @Test
  public void testGetPersonIdsWithWholeGenome_cohorts() {
    mockPersonIdQuery();
    DbCohort cohort2 = cohortDao.save(buildSimpleCohort(workspace));

    DbDataset dataset = new DbDataset();
    dataset.setCohortIds(List.of(cohort.getCohortId(), cohort2.getCohortId()));
    genomicDatasetService.getPersonIdsWithWholeGenome(dataset);

    // Two participant criteria, one per cohort.
    verify(mockCohortQueryBuilder)
        .buildUnionedParticipantIdQuery(argThat(criteriaList -> criteriaList.size() == 2));
  }

  @Test
  public void testGetPersonIdsWithWholeGenome_allParticipants() {
    mockPersonIdQuery();

    DbDataset dataset = new DbDataset();
    dataset.setIncludesAllParticipants(true);
    genomicDatasetService.getPersonIdsWithWholeGenome(dataset);

    // Expect one participant criteria: "has WGS".
    // Note: this is dipping too much into implementation, but options are limited with the high
    // amount of mocking in this test.
    verify(mockCohortQueryBuilder)
        .buildUnionedParticipantIdQuery(argThat(criteriaList -> criteriaList.size() == 1));
  }

  private void mockPersonIdQuery() {
    final TableResult tableResultMock = mock(TableResult.class);

    final FieldList schema = FieldList.of(List.of(Field.of("person_id", LegacySQLTypeName.STRING)));

    doReturn(
            List.of(
                FieldValueList.of(List.of(FieldValue.of(Attribute.PRIMITIVE, "1")), schema),
                FieldValueList.of(List.of(FieldValue.of(Attribute.PRIMITIVE, "2")), schema)))
        .when(tableResultMock)
        .getValues();

    doReturn(tableResultMock).when(mockBigQueryService).executeQuery(any());
  }

  private DbCohort buildSimpleCohort(DbWorkspace workspace) {
    final CohortDefinition cohortDefinition = CohortDefinitions.males();
    final String cohortCriteria = new Gson().toJson(cohortDefinition);

    final DbCohort cohortDbModel = new DbCohort();
    cohortDbModel.setType("foo");
    cohortDbModel.setWorkspaceId(workspace.getWorkspaceId());
    cohortDbModel.setCriteria(cohortCriteria);
    cohortDbModel.setCreator(buildUser());
    return cohortDbModel;
  }

  private DbUser buildUser() {
    DbUser dbUser = new DbUser();
    dbUser.setFamilyName("Family Name");
    dbUser.setContactEmail("xyz@mock.com");
    return userDao.save(dbUser);
  }
}
