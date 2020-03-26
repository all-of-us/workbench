package org.pmiops.workbench.elasticsearch;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ElasticFiltersTest {

  private static final ImmutableList<String> NO_DATA_FILTERS = ImmutableList.of();
  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static DbCriteria icd9Criteria() {
    return DbCriteria.builder()
        .addDomainId(DomainType.CONDITION.toString())
        .addType(CriteriaType.ICD9CM.toString())
        .addAttribute(Boolean.FALSE)
        .addStandard(false)
        .addSynonyms("[CONDITION_rank1]")
        .build();
  }

  private static DbCriteria drugCriteria() {
    return DbCriteria.builder()
        .addDomainId(DomainType.DRUG.toString())
        .addType(CriteriaType.ATC.toString())
        .addAttribute(Boolean.FALSE)
        .addStandard(true)
        .build();
  }

  private static DbCriteria basicsCriteria() {
    return DbCriteria.builder()
        .addDomainId(DomainType.SURVEY.toString())
        .addType(CriteriaType.PPI.toString())
        .addSubtype(CriteriaSubType.SURVEY.toString())
        .addAttribute(Boolean.FALSE)
        .addStandard(false)
        .addSynonyms("+[SURVEY_rank1]")
        .build();
  }

  private SearchParameter leafParam2;

  @Before
  public void setUp() {
    // Generate a simple test criteria tree
    // 1
    // | - 2
    // | - 3
    DbCriteria icd9Parent = icd9Criteria();
    icd9Parent.setCode("001");
    icd9Parent.setConceptId("771");
    icd9Parent.setGroup(true);
    icd9Parent.setSelectable(false);
    icd9Parent.setParentId(0);
    saveCriteriaWithPath("", icd9Parent);

    DbCriteria icd9Child1 = icd9Criteria();
    icd9Child1.setCode("001.002");
    icd9Child1.setConceptId("772");
    icd9Child1.setGroup(false);
    icd9Child1.setSelectable(true);
    icd9Child1.setParentId(icd9Parent.getId());
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child1);

    DbCriteria icd9Child2 = icd9Criteria();
    icd9Child2.setCode("001.003");
    icd9Child2.setConceptId("773");
    icd9Child2.setGroup(true);
    icd9Child2.setSelectable(true);
    icd9Child2.setParentId(icd9Parent.getId());
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child2);

    // Singleton SNOMED code.
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addCode("005")
            .addConceptId("775")
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.SNOMED.toString())
            .addAttribute(Boolean.FALSE)
            .addGroup(false)
            .addSelectable(true)
            .addParentId(0)
            .addPath("")
            .build());

    // Four node PPI tree, survey, question, 1 answer.
    DbCriteria survey = basicsCriteria();
    survey.setCode("006");
    survey.setGroup(true);
    survey.setSelectable(true);
    survey.setParentId(0);
    survey.setConceptId("77");
    saveCriteriaWithPath("", survey);

    DbCriteria question = basicsCriteria();
    question.setCode("007");
    question.setConceptId("777");
    question.setGroup(true);
    question.setSelectable(true);
    question.setParentId(survey.getId());
    saveCriteriaWithPath(survey.getPath(), question);

    DbCriteria answer = basicsCriteria();
    answer.setCode("008");
    // Concept ID matches the question.
    answer.setConceptId("7771");
    answer.setGroup(false);
    answer.setSelectable(true);
    answer.setParentId(question.getId());
    saveCriteriaWithPath(question.getPath(), answer);

    // drug tree
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 21600009)");
    DbCriteria drugParent = drugCriteria();
    drugParent.setCode("A");
    drugParent.setConceptId("21600001");
    drugParent.setGroup(true);
    drugParent.setSelectable(false);
    drugParent.setParentId(0);
    saveCriteriaWithPath("", drugParent);

    DbCriteria drug1 = drugCriteria();
    drug1.setCode("A01");
    drug1.setConceptId("21600002");
    drug1.setGroup(true);
    drug1.setSelectable(true);
    drug1.setParentId(drugParent.getId());
    saveCriteriaWithPath(drugParent.getPath(), drug1);

    DbCriteria drug2 = drugCriteria();
    drug2.setCode("A01A");
    drug2.setConceptId("21600003");
    drug2.setGroup(true);
    drug2.setSelectable(true);
    drug2.setParentId(drug1.getId());
    saveCriteriaWithPath(drug1.getPath(), drug2);

    DbCriteria drug3 = drugCriteria();
    drug3.setCode("A01AA");
    drug3.setConceptId("21600004");
    drug3.setGroup(true);
    drug3.setSelectable(true);
    drug3.setParentId(drug2.getId());
    saveCriteriaWithPath(drug2.getPath(), drug3);

    DbCriteria drug4 = drugCriteria();
    drug4.setCode("9873");
    drug4.setConceptId("19069022");
    drug4.setGroup(false);
    drug4.setSelectable(true);
    drug4.setType(CriteriaType.RXNORM.toString());
    drug4.setParentId(drug3.getId());
    saveCriteriaWithPath(drug3.getPath(), drug4);

    leafParam2 =
        new SearchParameter()
            .conceptId(772L)
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .ancestorData(false)
            .standard(false)
            .group(false);
  }

  @After
  public void tearDown() {
    // jdbcTemplate is used to create/insert data into the criteria ancestor table. The codebase
    // currently doesn't have a need to implement a DAO for this table. The @DirtiesContext
    // annotation seems to only recognized Hibernate persisted entities and their related tables.
    // So the following tear down method needs to drop to table to keep from colliding with
    // other test classes that also use this approach to create the cb_criteria_ancestor table. Not
    // implementing this drop causes subsequent test suite runs to fail with a
    // BadSqlGrammarException: StatementCallback; bad SQL grammar [create table
    // cb_criteria_ancestor(ancestor_id integer, descendant_id integer)]; nested exception is
    // org.h2.jdbc.JdbcSQLException: Table "CB_CRITERIA_ANCESTOR" already exists
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  private static final QueryBuilder singleNestedQuery(
      List<String> dataFilters, QueryBuilder... inners) {
    return singleNestedQueryOccurrences(dataFilters, 1, inners);
  }

  private static final QueryBuilder singleNestedQueryOccurrences(
      List<String> dataFilters, int n, QueryBuilder... inners) {
    BoolQueryBuilder b = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      b.filter(in);
    }
    BoolQueryBuilder b2 =
        QueryBuilders.boolQuery()
            .filter(
                QueryBuilders.boolQuery()
                    .should(
                        QueryBuilders.functionScoreQuery(
                                QueryBuilders.nestedQuery(
                                    "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
                            .setMinScore(n)));
    BoolQueryBuilder b3 = QueryBuilders.boolQuery();
    for (String dataFilter : dataFilters) {
      b3.filter(QueryBuilders.termQuery(dataFilter, true));
    }
    return dataFilters.isEmpty() ? b2 : b2.filter(b3);
  }

  private static final QueryBuilder nonNestedQuery(QueryBuilder... inners) {
    BoolQueryBuilder innerBuilder = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      if (in.toString().contains("birth_datetime")
          || in.toString().contains("age_at_consent")
          || in.toString().contains("age_at_cdr")) {
        innerBuilder.should(in);
      } else {
        BoolQueryBuilder b = QueryBuilders.boolQuery().filter(in);
        innerBuilder.should(b);
      }
    }
    return QueryBuilders.boolQuery().filter(innerBuilder);
  }

  private static final QueryBuilder nonNestedMustNotQuery(QueryBuilder... inners) {
    BoolQueryBuilder innerBuilder = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      BoolQueryBuilder b = QueryBuilders.boolQuery().filter(in);
      innerBuilder.should(b);
    }
    return QueryBuilders.boolQuery().filter(innerBuilder).mustNot(innerBuilder);
  }

  @Test
  public void testLeafQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(leafParam2)))
                .addDataFiltersItem("has_ehr_data")
                .addDataFiltersItem("has_physical_measurement_data"));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                ImmutableList.of("has_ehr_data", "has_physical_measurement_data"),
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))));
  }

  @Test
  public void testParentConceptQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .conceptId(21600002L)
                                        .domain(DomainType.DRUG.toString())
                                        .type(CriteriaType.ATC.toString())
                                        .ancestorData(true)
                                        .standard(true)
                                        .group(true)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery(
                    "events.concept_id", ImmutableList.of("21600002", "21600009"))));
  }

  @Test
  public void testICD9Query() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .value("001")
                                        .conceptId(771L)
                                        .domain(DomainType.CONDITION.toString())
                                        .type(CriteriaType.ICD9CM.toString())
                                        .group(true)
                                        .ancestorData(false)
                                        .standard(false)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery(
                    "events.source_concept_id", ImmutableList.of("771", "772", "773"))));
  }

  @Test
  public void testICD9AndSnomedQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .value("001")
                                        .conceptId(771L)
                                        .domain(DomainType.CONDITION.toString())
                                        .type(CriteriaType.ICD9CM.toString())
                                        .group(false)
                                        .ancestorData(false)
                                        .standard(false))
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .conceptId(477L)
                                        .domain(DomainType.CONDITION.toString())
                                        .type(CriteriaType.SNOMED.toString())
                                        .group(false)
                                        .ancestorData(false)
                                        .standard(true)))));

    BoolQueryBuilder source = QueryBuilders.boolQuery();
    source.filter(QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("771")));
    BoolQueryBuilder standard = QueryBuilders.boolQuery();
    standard.filter(QueryBuilders.termsQuery("events.concept_id", ImmutableList.of("477")));
    QueryBuilder expected =
        QueryBuilders.boolQuery()
            .filter(
                QueryBuilders.boolQuery()
                    .should(
                        QueryBuilders.functionScoreQuery(
                                QueryBuilders.nestedQuery(
                                    "events",
                                    QueryBuilders.constantScoreQuery(source),
                                    ScoreMode.Total))
                            .setMinScore(1))
                    .should(
                        QueryBuilders.functionScoreQuery(
                                QueryBuilders.nestedQuery(
                                    "events",
                                    QueryBuilders.constantScoreQuery(standard),
                                    ScoreMode.Total))
                            .setMinScore(1)));
    assertThat(resp).isEqualTo(expected);
  }

  @Test
  public void testPPISurveyQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .domain(DomainType.SURVEY.toString())
                                        .type(CriteriaType.PPI.toString())
                                        .subtype(CriteriaSubType.SURVEY.toString())
                                        .ancestorData(false)
                                        .standard(false)
                                        .group(true)
                                        .conceptId(77L)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery(
                    "events.source_concept_id", ImmutableList.of("77", "7771", "777"))));
  }

  @Test
  public void testPPIQuestionQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .domain(DomainType.SURVEY.toString())
                                        .type(CriteriaType.PPI.toString())
                                        .subtype(CriteriaSubType.QUESTION.toString())
                                        .conceptId(777L)
                                        .ancestorData(false)
                                        .standard(false)
                                        .group(true)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery(
                    "events.source_concept_id", ImmutableList.of("7771", "777"))));
  }

  @Test
  public void testPPIAnswerQuery() {
    Attribute attr =
        new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).addOperandsItem("1");
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .domain(DomainType.SURVEY.toString())
                                        .type(CriteriaType.PPI.toString())
                                        .subtype(CriteriaSubType.ANSWER.toString())
                                        .conceptId(7771L)
                                        .group(false)
                                        .ancestorData(false)
                                        .standard(false)
                                        .addAttributesItem(attr)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("7771")),
                QueryBuilders.rangeQuery("events.value_as_number").gte(1.0F).lte(1.0F)));
  }

  @Test
  public void testPPIAnswerQueryCat() {
    Attribute attr = new Attribute().name(AttrName.CAT).operator(Operator.IN).addOperandsItem("1");
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .domain(DomainType.SURVEY.toString())
                                        .type(CriteriaType.PPI.toString())
                                        .subtype(CriteriaSubType.ANSWER.toString())
                                        .conceptId(777L)
                                        .group(false)
                                        .ancestorData(false)
                                        .standard(false)
                                        .addAttributesItem(attr)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("777")),
                QueryBuilders.termsQuery(
                    "events.value_as_source_concept_id", ImmutableList.of("1"))));
  }

  @Test
  public void testAgeAtEventModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.AGE_AT_EVENT)
                                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                                        .addOperandsItem("18")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                QueryBuilders.rangeQuery("events.age_at_start").gte(18)));
  }

  @Test
  public void testDateModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.EVENT_DATE)
                                        .operator(Operator.BETWEEN)
                                        .addOperandsItem("12/25/1988")
                                        .addOperandsItem("12/27/1988")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                QueryBuilders.rangeQuery("events.start_date").gte("12/25/1988").lte("12/27/1988")));
  }

  @Test
  public void testVisitsModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.ENCOUNTERS)
                                        .operator(Operator.IN)
                                        .addOperandsItem("123")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                QueryBuilders.termsQuery("events.visit_concept_id", ImmutableList.of("123"))));
  }

  @Test
  public void testNumOfOccurrencesModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.NUM_OF_OCCURRENCES)
                                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                                        .addOperandsItem("13")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQueryOccurrences(
                NO_DATA_FILTERS,
                13,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))));
  }

  @Test
  public void testHeightAnyQuery() {
    String conceptId = "903133";
    SearchParameter heightAnyParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .standard(false)
            .ancestorData(false)
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem().addSearchParametersItem(heightAnyParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testHeightEqualQuery() {
    Attribute attr =
        new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(ImmutableList.of("1"));
    Object left = Float.parseFloat(attr.getOperands().get(0));
    Object right = Float.parseFloat(attr.getOperands().get(0));
    String conceptId = "903133";
    SearchParameter heightParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .group(false)
            .ancestorData(false)
            .standard(false)
            .addAttributesItem(attr);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(heightParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.rangeQuery("events.value_as_number").gte(left).lte(right)));
  }

  @Test
  public void testWeightBetweenQuery() {
    Attribute attr =
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.BETWEEN)
            .operands(ImmutableList.of("1", "2"));
    Object left = Float.parseFloat(attr.getOperands().get(0));
    Object right = Float.parseFloat(attr.getOperands().get(1));
    String conceptId = "903121";
    SearchParameter weightParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .addAttributesItem(attr);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(weightParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.rangeQuery("events.value_as_number").gte(left).lte(right)));
  }

  @Test
  public void testGenderQuery() {
    String conceptId = "8507";
    SearchParameter genderParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .standard(true)
            .ancestorData(false)
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testGenderExcludeQuery() {
    String conceptId = "8507";
    SearchParameter genderParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addExcludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testGenderIncludeAndExcludeQuery() {
    String conceptId = "8507";
    SearchParameter genderParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam)))
                .addExcludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedMustNotQuery(
                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testRaceQuery() {
    String conceptId = "8515";
    SearchParameter raceParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.RACE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(raceParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("race_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testEthnicityQuery() {
    String conceptId = "38003563";
    SearchParameter ethParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.ETHNICITY.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(ethParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("ethnicity_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testDeceasedQuery() {
    SearchParameter deceasedParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.DECEASED.toString())
            .standard(true)
            .ancestorData(false)
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem().addSearchParametersItem(deceasedParam))));
    assertThat(resp).isEqualTo(nonNestedQuery(QueryBuilders.termQuery("is_deceased", true)));
  }

  @Test
  public void testPregnancyQuery() {
    String conceptId = "903120";
    String operand = "12345";
    Attribute attr =
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(ImmutableList.of(operand));
    SearchParameter pregParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .group(false)
            .ancestorData(false)
            .standard(false)
            .attributes(ImmutableList.of(attr));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(pregParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.termsQuery("events.value_as_concept_id", ImmutableList.of(operand))));
  }

  @Test
  public void testMeasurementCategoricalQuery() {
    String conceptId = "3015813";
    String operand1 = "12345";
    String operand2 = "12346";
    Attribute attr =
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(ImmutableList.of(operand1, operand2));
    SearchParameter measParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .attributes(ImmutableList.of(attr));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(measParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.termsQuery(
                    "events.value_as_concept_id", ImmutableList.of(operand1, operand2))));
  }

  @Test
  public void testVisitQuery() {
    String conceptId = "9202";
    SearchParameter visitParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .ancestorData(false)
            .standard(true)
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(visitParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                NO_DATA_FILTERS,
                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testAgeQuery() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Object left = now.minusYears(34).minusYears(1).toLocalDate();
    Object right = now.minusYears(20).toLocalDate();
    SearchParameter ageParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .addAttributesItem(
                new Attribute()
                    .name(AttrName.AGE)
                    .operator(Operator.BETWEEN)
                    .operands(ImmutableList.of("20", "34")));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(ageParam))));
    BoolQueryBuilder ageBuilder =
        QueryBuilders.boolQuery()
            .filter(QueryBuilders.termQuery("is_deceased", false))
            .filter(
                QueryBuilders.rangeQuery("birth_datetime")
                    .gte(left)
                    .lte(right)
                    .format("yyyy-MM-dd"));
    assertThat(resp).isEqualTo(nonNestedQuery(ageBuilder));
  }

  @Test
  public void testAgeAtConsentQuery() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Object left = 20;
    Object right = 34;
    SearchParameter ageAtConsentParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .addAttributesItem(
                new Attribute()
                    .name(AttrName.AGE_AT_CONSENT)
                    .operator(Operator.BETWEEN)
                    .operands(ImmutableList.of("20", "34")));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem().addSearchParametersItem(ageAtConsentParam))));
    BoolQueryBuilder ageBuilder =
        QueryBuilders.boolQuery()
            .filter(QueryBuilders.termQuery("is_deceased", false))
            .filter(QueryBuilders.rangeQuery("age_at_consent").gte(left).lte(right));
    assertThat(resp).isEqualTo(nonNestedQuery(ageBuilder));
  }

  @Test
  public void testAgeAndEthnicityQuery() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Object left = now.minusYears(34).minusYears(1).toLocalDate();
    Object right = now.minusYears(20).toLocalDate();
    SearchParameter ageParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .addAttributesItem(
                new Attribute()
                    .name(AttrName.AGE)
                    .operator(Operator.BETWEEN)
                    .operands(ImmutableList.of("20", "34")));
    String conceptId = "38003563";
    SearchParameter ethParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.ETHNICITY.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(Long.parseLong(conceptId));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .searchParameters(ImmutableList.of(ageParam, ethParam)))));

    BoolQueryBuilder ageBuilder =
        QueryBuilders.boolQuery()
            .filter(QueryBuilders.termQuery("is_deceased", false))
            .filter(
                QueryBuilders.rangeQuery("birth_datetime")
                    .gte(left)
                    .lte(right)
                    .format("yyyy-MM-dd"));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                ageBuilder,
                QueryBuilders.termsQuery("ethnicity_concept_id", ImmutableList.of(conceptId))));
  }

  private void saveCriteriaWithPath(String path, DbCriteria criteria) {
    cbCriteriaDao.save(criteria);
    String pathEnd = String.valueOf(criteria.getId());
    criteria.setPath(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    cbCriteriaDao.save(criteria);
  }
}
