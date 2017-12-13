package org.pmiops.workbench.cohorts;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.DemoQueryBuilder;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({ParticipantCounter.class, BigQueryService.class, CohortMaterializationService.class,
    DemoQueryBuilder.class, QueryBuilderFactory.class})
public class CohortMaterializationServiceTest extends BigQueryBaseTest {

  @Autowired
  private CohortMaterializationService cohortMaterializationService;

  private CdrVersion cdrVersion = new CdrVersion();

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
        "person");
  }

  @Test
  public void testMaterializeCohortOneMale() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cdrVersion,
        SearchRequests.males(),null, 1000, null);
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPaging() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cdrVersion,
        SearchRequests.allGenders(),null, 2, null);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNotNull();
    MaterializeCohortResponse response2 = cohortMaterializationService.materializeCohort(cdrVersion,
        SearchRequests.allGenders(),null, 2, response.getNextPageToken());
    assertPersonIds(response2, 102246L);
    assertThat(response2.getNextPageToken()).isNull();

    try {
      // Pagination token doesn't match, this should fail.
      cohortMaterializationService.materializeCohort(cdrVersion, SearchRequests.males(),
          null, 2, response.getNextPageToken());
      fail("Exception expected");
    } catch (BadRequestException e) {
      // expected
    }

    PaginationToken token = PaginationToken.fromBase64(response.getNextPageToken());
    PaginationToken invalidToken = new PaginationToken(-1L, token.getParameterHash());
    try {
      // Pagination token doesn't match, this should fail.
      cohortMaterializationService.materializeCohort(cdrVersion, SearchRequests.males(),
          null, 2, invalidToken.toBase64());
      fail("Exception expected");
    } catch (BadRequestException e) {
      // expected
    }
  }

  private void assertPersonIds(MaterializeCohortResponse response, long... personIds) {
    List<Object> expectedResults = new ArrayList<>();
    for (long personId : personIds) {
      expectedResults.add(ImmutableMap.of(CohortMaterializationService.PERSON_ID, personId));
    }
    assertThat(response.getResults()).isEqualTo(expectedResults);
  }
}
