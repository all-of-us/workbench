package org.pmiops.workbench.elasticsearch;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.elasticsearch.ElasticFilters.ElasticFilterResponse;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
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

import java.util.Arrays;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ElasticFiltersTest {

  @Autowired
  private CriteriaDao criteriaDao;

  private static Criteria icd9Criteria() {
    return new Criteria()
        .type(TreeType.ICD9.toString())
        .subtype(TreeSubType.CM.toString())
        .attribute(Boolean.FALSE);
  }

  private static Criteria basicsCriteria() {
    return new Criteria()
        .type(TreeType.PPI.toString())
        .subtype(TreeSubType.BASICS.toString())
        .attribute(Boolean.FALSE);
  }
  private SearchParameter leafParam2;
  private SearchParameter ageParam;
  private Attribute ageAttr;

  @Before
  public void setUp() {
    // Generate a simple test criteria tree
    // 1
    // | - 2
    // | - 3 - 4
    criteriaDao.save(icd9Criteria()
        .id(1)
        .code("001")
        .conceptId("771")
        .group(true)
        .selectable(false)
        .parentId(0)
        .path(""));
    criteriaDao.save(icd9Criteria()
        .id(2)
        .code("001.002")
        .conceptId("772")
        .group(false)
        .selectable(true)
        .parentId(1)
        .path("1"));
    criteriaDao.save(icd9Criteria()
        .id(3)
        .code("001.003")
        .conceptId("773")
        .group(true)
        .selectable(true)
        .parentId(1)
        .path("1"));
    criteriaDao.save(icd9Criteria()
        .id(4)
        .code("001.003.004")
        .conceptId("774")
        .group(false)
        .selectable(true)
        .parentId(3)
        .path("1.3"));

    // Singleton SNOMED code.
    criteriaDao.save(new Criteria()
        .id(5)
        .code("005")
        .conceptId("775")
        .domainId("Condition")
        .type(TreeType.SNOMED.toString())
        .subtype(TreeSubType.CM.toString())
        .attribute(Boolean.FALSE)
        .group(false)
        .selectable(true)
        .parentId(0)
        .path(""));

    // Four node PPI tree, survey, question, 2 answers.
    criteriaDao.save(basicsCriteria()
        .id(6)
        .code("006")
        .group(true)
        .selectable(true)
        .parentId(0)
        .path(""));
    criteriaDao.save(basicsCriteria()
        .id(7)
        .code("007")
        .conceptId("777")
        .group(true)
        .selectable(true)
        .parentId(6)
        .path("6"));
    criteriaDao.save(basicsCriteria()
        .id(8)
        .code("008")
        // Concept ID matches the question.
        .conceptId("777")
        .group(false)
        .selectable(true)
        .parentId(7)
        .path("6.7"));
    criteriaDao.save(basicsCriteria()
        .id(9)
        .code("009")
        // Concept ID matches the question.
        .conceptId("777")
        .group(false)
        .selectable(true)
        .parentId(7)
        .path("6.7"));

    leafParam2 = new SearchParameter()
        .conceptId(772L)
        .type(TreeType.ICD9.toString())
        .subtype(TreeSubType.CM.toString())
        .group(false);

    ageAttr = new Attribute()
      .name(AttrName.AGE)
      .operator(Operator.EQUAL)
      .operands(Arrays.asList("38"));
    ageParam = new SearchParameter()
      .type(TreeType.DEMO.toString())
      .subtype(TreeSubType.AGE.toString())
      .group(false)
      .attributes(Arrays.asList(ageAttr));
  }

  private static final QueryBuilder singleNestedQuery(QueryBuilder... inners) {
    return singleNestedQueryOccurrences(1, inners);
  }

  private static final QueryBuilder singleNestedQueryOccurrences(int n, QueryBuilder... inners) {
    BoolQueryBuilder b = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      b.filter(in);
    }
    return QueryBuilders.boolQuery().filter(QueryBuilders.boolQuery().filter(
        QueryBuilders.boolQuery().should(
            QueryBuilders.functionScoreQuery(
                QueryBuilders.nestedQuery(
                    "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total)
            ).setMinScore(n)
        )
    ));
  }

  @Test
  public void testLeafQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(leafParam2))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))));
  }

  @Test
  public void testParentConceptQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(new SearchParameter()
                        .conceptId(773L)
                        .type(TreeType.ICD9.toString())
                        .subtype(TreeSubType.CM.toString())
                        .group(true)))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("774"))));
  }

  @Test
  public void testParentCodeQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(new SearchParameter()
                        .value("001")
                        .type(TreeType.ICD9.toString())
                        .subtype(TreeSubType.CM.toString())
                        .group(true)))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772", "774"))));
  }

  @Test
  public void testParentTreeQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(new SearchParameter()
                        .type(TreeType.ICD9.toString())
                        .subtype(TreeSubType.CM.toString())
                        .group(true)))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772", "774"))));
  }

  @Test
  public void testPPIQuestionQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(new SearchParameter()
                        .type(TreeType.PPI.toString())
                        .subtype(TreeSubType.BASICS.toString())
                        .conceptId(777L)
                        .group(true)))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("777"))));
  }

  @Test
  public void testAgeAtEventModifierQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(leafParam2)
                    .addModifiersItem(
                        new Modifier()
                            .name(ModifierType.AGE_AT_EVENT)
                            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                            .addOperandsItem("18"))
                )));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
        QueryBuilders.rangeQuery("events.age_at_start").gte(18)));
  }

  @Test
  public void testDateModifierQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(leafParam2)
                    .addModifiersItem(
                        new Modifier()
                            .name(ModifierType.EVENT_DATE)
                            .operator(Operator.BETWEEN)
                            .addOperandsItem("12/25/1988")
                            .addOperandsItem("12/27/1988"))
                )));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
        QueryBuilders.rangeQuery("events.start_date").gt("12/25/1988").lt("12/27/1988")));
  }

  @Test
  public void testVisitsModifierQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(leafParam2)
                    .addModifiersItem(
                        new Modifier()
                            .name(ModifierType.ENCOUNTERS)
                            .operator(Operator.IN)
                            .addOperandsItem("123"))
                )));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
        QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
        QueryBuilders.termsQuery("events.visit_concept_id", ImmutableList.of("123"))));
  }

  @Test
  public void testNumOfOccurrencesModifierQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
        ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
            .addIncludesItem(new SearchGroup()
                .addItemsItem(new SearchGroupItem()
                    .addSearchParametersItem(leafParam2)
                    .addModifiersItem(
                        new Modifier()
                            .name(ModifierType.NUM_OF_OCCURRENCES)
                            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                            .addOperandsItem("13"))
                )));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQueryOccurrences(
        13, QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))));
  }

  @Test
  public void testAgeQuery() {
    ElasticFilterResponse<QueryBuilder> resp =
      ElasticFilters.fromCohortSearch(criteriaDao, new SearchRequest()
        .addIncludesItem(new SearchGroup()
          .addItemsItem(new SearchGroupItem()
            .addSearchParametersItem(ageParam))));
    assertThat(resp.isApproximate()).isFalse();
    assertThat(resp.value()).isEqualTo(singleNestedQuery(
      QueryBuilders.rangeQuery("events.birth_datetime").from("1981-03-12").to("1981-03-12").format("yyyy-MM-dd")));
  }
}
