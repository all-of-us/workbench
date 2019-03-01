package org.pmiops.workbench.elasticsearch;

import static com.google.common.truth.Truth.assertThat;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.elasticsearch.ElasticFilters.ElasticFilterResponse;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ElasticFiltersTest {

  @Autowired
  private CriteriaDao criteriaDao;

  @Before
  public void setUp() {
    criteriaDao.save(new Criteria()
        .id(1)
        .code("001")
        .conceptId("1")
        .domainId("Condition")
        .group(true)
        .selectable(false)
        .parentId(0)
        .type(TreeType.ICD9.toString())
        .subtype(TreeSubType.CM.toString())
        .attribute(Boolean.FALSE)
        .path("001"));
    criteriaDao.save(new Criteria()
        .id(2)
        .code("001.002")
        .conceptId("2")
        .domainId("Condition")
        .group(false)
        .selectable(true)
        .parentId(1)
        .type(TreeType.ICD9.toString())
        .subtype(TreeSubType.CM.toString())
        .attribute(Boolean.FALSE)
        .path("1.2"));
  }

  @Test
  public void testFromCohortSearch() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
        .addIncludesItem(new SearchGroup()
        .addItemsItem(new SearchGroupItem()
        .addSearchParametersItem(new SearchParameter()
        .conceptId(2L)))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(QueryBuilders.boolQuery());
  }
}
